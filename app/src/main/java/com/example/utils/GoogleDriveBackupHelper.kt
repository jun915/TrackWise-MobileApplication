package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object PKCE {
    fun generateVerifier(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun generateChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256")
        val hashed = digest.digest(bytes)
        return Base64.encodeToString(hashed, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}

class GoogleDriveBackupHelper(private val context: Context) {
    private val client = OkHttpClient()
    private val prefs = context.getSharedPreferences("google_drive_backup_prefs", Context.MODE_PRIVATE)

    companion object {
        const val CLIENT_ID = "378736944526-0tdao5v0tipaebjr33jvvhuiec20kmic.apps.googleusercontent.com"
        const val REDIRECT_URI = "com.aistudio.trackwise.pksqmx:/oauth2redirect"
        const val AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        const val DRIVE_FOLDER_NAME = "TrackWise Backups"
        const val TAG = "GDriveBackupHelper"
    }

    fun isConnected(): Boolean {
        return !prefs.getString("refresh_token", null).isNullOrBlank()
    }

    fun getConnectedAccountEmail(): String? {
        return prefs.getString("connected_email", null)
    }

    fun disconnect() {
        prefs.edit().clear().apply()
    }

    fun startAuthFlow() {
        try {
            val verifier = PKCE.generateVerifier()
            val challenge = PKCE.generateChallenge(verifier)
            
            prefs.edit()
                .putString("pkce_verifier", verifier)
                .apply()

            val authUri = Uri.parse(AUTH_URL)
                .buildUpon()
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", "https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/drive https://www.googleapis.com/auth/userinfo.email")
                .appendQueryParameter("code_challenge", challenge)
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("access_type", "offline")
                .appendQueryParameter("prompt", "consent")
                .build()

            val intent = Intent(Intent.ACTION_VIEW, authUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting auth flow", e)
        }
    }

    suspend fun handleRedirectUri(uri: Uri): Boolean = suspendCancellableCoroutine { continuation ->
        val code = uri.getQueryParameter("code")
        val verifier = prefs.getString("pkce_verifier", null)

        if (code == null || verifier == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        val formBody = FormBody.Builder()
            .add("client_id", CLIENT_ID)
            .add("redirect_uri", REDIRECT_URI)
            .add("code", code)
            .add("code_verifier", verifier)
            .add("grant_type", "authorization_code")
            .build()

        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to swap code for token", e)
                continuation.resume(false)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        val json = JSONObject(body)
                        val accessToken = json.getString("access_token")
                        val refreshToken = json.optString("refresh_token")
                        
                        val edit = prefs.edit()
                            .putString("access_token", accessToken)
                            .putLong("token_expiry", System.currentTimeMillis() + (json.getLong("expires_in") * 1000))
                        
                        if (!refreshToken.isNullOrBlank()) {
                            edit.putString("refresh_token", refreshToken)
                        }
                        edit.apply()

                        // Fetch user email
                        fetchUserEmailAndComplete(accessToken) { email ->
                            if (email != null) {
                                prefs.edit().putString("connected_email", email).apply()
                            }
                            continuation.resume(true)
                        }
                    } else {
                        Log.e(TAG, "Error response from token endpoint: $body")
                        continuation.resume(false)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during token parse", e)
                    continuation.resume(false)
                }
            }
        })
    }

    private fun fetchUserEmailAndComplete(accessToken: String, callback: (String?) -> Unit) {
        val request = Request.Builder()
            .url("https://www.googleapis.com/oauth2/v2/userinfo")
            .header("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        val json = JSONObject(body)
                        callback(json.optString("email", null))
                    } else {
                        callback(null)
                    }
                } catch (e: Exception) {
                    callback(null)
                }
            }
        })
    }

    private suspend fun getValidAccessToken(): String? {
        val expiry = prefs.getLong("token_expiry", 0L)
        val accessToken = prefs.getString("access_token", null)
        val refreshToken = prefs.getString("refresh_token", null)

        if (refreshToken.isNullOrBlank()) return null

        // Token is still valid (with 5 min buffer)
        if (accessToken != null && expiry > System.currentTimeMillis() + 300_000) {
            return accessToken
        }

        return suspendCancellableCoroutine { continuation ->
            val formBody = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("refresh_token", refreshToken)
                .add("grant_type", "refresh_token")
                .build()

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(formBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Failed to refresh token", e)
                    continuation.resume(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val body = response.body?.string()
                        if (response.isSuccessful && body != null) {
                            val json = JSONObject(body)
                            val newAccessToken = json.getString("access_token")
                            
                            prefs.edit()
                                .putString("access_token", newAccessToken)
                                .putLong("token_expiry", System.currentTimeMillis() + (json.getLong("expires_in") * 1000))
                                .apply()
                            
                            continuation.resume(newAccessToken)
                        } else {
                            Log.e(TAG, "Token refresh error response: $body")
                            continuation.resume(null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception refreshing token", e)
                        continuation.resume(null)
                    }
                }
            })
        }
    }

    suspend fun performGoogleDriveBackup(backupContent: String): Pair<Boolean, String> {
        val accessToken = getValidAccessToken() ?: return Pair(false, "Not connected to Google Drive or session expired.")
        
        try {
            // Step 1: Find or Create separate folder "TrackWise Backups"
            val folderId = findOrCreateBackupFolder(accessToken) ?: return Pair(false, "Failed to create backup directory on Google Drive.")

            // Step 2: Get previous backup details
            val previousBackup = getLatestBackupFileInfo(accessToken, folderId)
            val currentSize = backupContent.toByteArray(Charsets.UTF_8).size

            if (previousBackup != null) {
                // "Make sure backup only backs up when the size of previous backup is less than current backup"
                if (currentSize <= previousBackup.sizeBytes) {
                    Log.i(TAG, "Backup skipped: Previous backup size (${previousBackup.sizeBytes} bytes) is >= current size ($currentSize bytes).")
                    return Pair(false, "Skipped: Previous backup size (${previousBackup.sizeBytes} B) is larger or equal to current size ($currentSize B).")
                }
            }

            // Step 3: Upload with Date and Time appended to name
            val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val timestamp = sdf.format(Date())
            val filename = "trackwise_backup_$timestamp.json"

            val success = uploadBackupFile(accessToken, folderId, filename, backupContent)
            return if (success) {
                val statusMsg = "Success: Backed up $filename ($currentSize B)"
                prefs.edit().putString("last_drive_backup_status", statusMsg).apply()
                Pair(true, statusMsg)
            } else {
                Pair(false, "Failed to upload file to Google Drive.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing Drive backup", e)
            return Pair(false, "Backup error: ${e.message}")
        }
    }

    private suspend fun findOrCreateBackupFolder(accessToken: String): String? = suspendCancellableCoroutine { continuation ->
        // Search query
        val query = "name = '$DRIVE_FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl()
            .newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("fields", "files(id)")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        val files = JSONObject(body).getJSONArray("files")
                        if (files.length() > 0) {
                            continuation.resume(files.getJSONObject(0).getString("id"))
                            return
                        }
                        
                        // Create folder
                        createFolder(accessToken) { newFolderId ->
                            continuation.resume(newFolderId)
                        }
                    } else {
                        continuation.resume(null)
                    }
                } catch (e: Exception) {
                    continuation.resume(null)
                }
            }
        })
    }

    private fun createFolder(accessToken: String, callback: (String?) -> Unit) {
        val payload = JSONObject().apply {
            put("name", DRIVE_FOLDER_NAME)
            put("mimeType", "application/vnd.google-apps.folder")
        }
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = payload.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files")
            .header("Authorization", "Bearer $accessToken")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        callback(JSONObject(body).getString("id"))
                    } else {
                        callback(null)
                    }
                } catch (e: Exception) {
                    callback(null)
                }
            }
        })
    }

    data class BackupFileInfo(val id: String, val name: String, val sizeBytes: Long)

    private suspend fun getLatestBackupFileInfo(accessToken: String, folderId: String): BackupFileInfo? = suspendCancellableCoroutine { continuation ->
        val query = "'$folderId' in parents and name contains 'trackwise_backup_' and trashed = false"
        val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl()
            .newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("orderBy", "createdTime desc")
            .addQueryParameter("fields", "files(id,name,size)")
            .addQueryParameter("pageSize", "1")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        val files = JSONObject(body).getJSONArray("files")
                        if (files.length() > 0) {
                            val first = files.getJSONObject(0)
                            // Note: Google Drive API size field is a string representable as a long
                            val size = if (first.has("size")) first.getString("size").toLong() else 0L
                            continuation.resume(BackupFileInfo(
                                first.getString("id"),
                                first.getString("name"),
                                size
                            ))
                        } else {
                            continuation.resume(null)
                        }
                    } else {
                        continuation.resume(null)
                    }
                } catch (e: Exception) {
                    continuation.resume(null)
                }
            }
        })
    }

    private suspend fun uploadBackupFile(accessToken: String, folderId: String, filename: String, content: String): Boolean = suspendCancellableCoroutine { continuation ->
        val metadata = JSONObject().apply {
            put("name", filename)
            put("parents", JSONArray().put(folderId))
        }

        val boundary = "---GDriveBackupBoundary---"
        val mediaType = "multipart/related; boundary=$boundary".toMediaTypeOrNull()

        val bodyBuilder = java.lang.StringBuilder()
        bodyBuilder.append("--$boundary\r\n")
        bodyBuilder.append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
        bodyBuilder.append(metadata.toString())
        bodyBuilder.append("\r\n--$boundary\r\n")
        bodyBuilder.append("Content-Type: application/json\r\n\r\n")
        bodyBuilder.append(content)
        bodyBuilder.append("\r\n--$boundary--\r\n")

        val requestBody = bodyBuilder.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            .header("Authorization", "Bearer $accessToken")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(false)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response.isSuccessful)
            }
        })
    }

    fun getLastBackupStatus(): String? {
        return prefs.getString("last_drive_backup_status", "No backup uploaded yet.")
    }
}
