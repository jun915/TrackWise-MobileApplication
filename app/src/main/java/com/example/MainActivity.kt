package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.TrackWiseDatabase
import com.example.repository.TrackWiseRepository
import com.example.ui.AuthScreen
import com.example.ui.MainScreen
import com.example.ui.TrackWiseViewModel
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: TrackWiseViewModel

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentExtras(intent)
        handleOAuthRedirect(intent)
    }

    private fun handleIntentExtras(intent: android.content.Intent) {
        val targetTab = intent.getStringExtra("target_tab")
        val targetSubTab = intent.getIntExtra("target_sub_tab", -1)
        if (targetTab != null && ::viewModel.isInitialized) {
            viewModel.setNotificationNavigateTab(targetTab)
            if (targetSubTab != -1) {
                if (targetTab == "workspace") {
                    viewModel.setWorkspaceSubTab(targetSubTab)
                } else if (targetTab == "health") {
                    viewModel.setHealthSubTab(targetSubTab)
                }
            }
        }
    }

    private fun handleOAuthRedirect(intent: android.content.Intent) {
        val data = intent.data
        if (data != null && data.scheme == "com.aistudio.trackwise.pksqmx") {
            if (::viewModel.isInitialized) {
                viewModel.handleGoogleDriveRedirect(data)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Local Room persistence instantiation (Section 5.12 & Repository pattern)
        val database = TrackWiseDatabase.getDatabase(applicationContext)
        val repository = TrackWiseRepository(database.trackWiseDao())
        viewModel = TrackWiseViewModel(application, repository)

        handleIntentExtras(intent)
        handleOAuthRedirect(intent)

        // Schedule persistent background reminder receiver
        com.example.receiver.ReminderReceiver.scheduleBackgroundReminderAlarm(applicationContext)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val themeAccent by viewModel.appThemeSelection.collectAsState()
            val appFontSize by viewModel.appFontSize.collectAsState()
            val appFontStyle by viewModel.appFontStyle.collectAsState()
            val appBgType by viewModel.appBgType.collectAsState()
            val appBgColor by viewModel.appBgColor.collectAsState()
            val appBgGradient by viewModel.appBgGradient.collectAsState()
            val appBgImage by viewModel.appBgImage.collectAsState()

            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            
            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }

            // Runtime request launcher for Post Notifications permission in Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // Permission handled silently without showing 'Notifications Configured' toast or alert
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    
                    if (!hasPermission) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            LaunchedEffect(isDark) {
                if (isDark) {
                    enableEdgeToEdge(
                        statusBarStyle = androidx.activity.SystemBarStyle.dark(
                            android.graphics.Color.TRANSPARENT
                        ),
                        navigationBarStyle = androidx.activity.SystemBarStyle.dark(
                            android.graphics.Color.TRANSPARENT
                        )
                    )
                } else {
                    enableEdgeToEdge(
                        statusBarStyle = androidx.activity.SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        ),
                        navigationBarStyle = androidx.activity.SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    )
                }
            }

            MyApplicationTheme(
                darkTheme = isDark,
                themeAccent = themeAccent,
                fontSize = appFontSize,
                fontStyle = appFontStyle,
                bgType = appBgType,
                bgColorName = appBgColor,
                bgGradientName = appBgGradient,
                bgImageName = appBgImage
            ) {
                val isLoggedIn by viewModel.isLoggedIn.collectAsState(initial = false)
                val focusManager = LocalFocusManager.current
                val keyboardController = LocalSoftwareKeyboardController.current

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            )
                        }
                ) {
                    if (isLoggedIn) {
                        // Authenticated shell (Section 7.1 bottom nav)
                        MainScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Auth gate (Section 3.1)
                        AuthScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
