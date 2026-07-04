package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.TrackWiseDatabase
import com.example.repository.TrackWiseRepository
import com.example.ui.AuthScreen
import com.example.ui.MainScreen
import com.example.ui.TrackWiseViewModel
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Local Room persistence instantiation (Section 5.12 & Repository pattern)
        val database = TrackWiseDatabase.getDatabase(applicationContext)
        val repository = TrackWiseRepository(database.trackWiseDao())
        val viewModel = TrackWiseViewModel(application, repository)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val themeAccent by viewModel.appThemeSelection.collectAsState()

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
                if (isGranted) {
                    viewModel.addNotification("Notifications Configured", "You will now receive alerts for completed items, goals, and alarms.")
                }
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

            MyApplicationTheme(darkTheme = isDark, themeAccent = themeAccent) {
                val isLoggedIn by viewModel.isLoggedIn.collectAsState(initial = false)
                val focusManager = LocalFocusManager.current

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
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
