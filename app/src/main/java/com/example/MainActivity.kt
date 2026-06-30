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
            MyApplicationTheme(darkTheme = themeMode == "dark") {
                val isLoggedIn by viewModel.isLoggedIn.collectAsState(initial = false)

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
