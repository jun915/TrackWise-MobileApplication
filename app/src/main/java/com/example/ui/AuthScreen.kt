package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import com.example.ui.theme.*

@Composable
fun AuthScreen(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    var authMode by remember { mutableStateOf("login") } // "login", "signup", "forgot"
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    
    var showPassword by remember { mutableStateOf(false) }
    
    val authError by viewModel.authError.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    val currentTheme = viewModel.themeMode.collectAsState().value
    val themeAccent by viewModel.appThemeSelection.collectAsState()
    val isSystemInDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = currentTheme == "dark" || (currentTheme == "system" && isSystemInDark)
    val focusManager = LocalFocusManager.current

    val gradientColors = com.example.ui.theme.getThemeGradientColors(themeAccent, isDark)
    val bgGradient = Brush.verticalGradient(gradientColors)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Frosted Glass Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Branding Title with vibrant multi-color neon gradient
            Text(
                text = "TrackWise",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(BrandViolet, BrandPink, BrandCyan)
                    )
                ),
                modifier = Modifier.testTag("app_title")
            )
            
            Text(
                text = "Track habits, tasks & health — see progress clearly",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Form Title
            Text(
                text = when (authMode) {
                    "signup" -> "Create Account"
                    "forgot" -> "Reset Password"
                    else -> "Welcome Back"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (authError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandRose.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, BrandRose, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = authError!!,
                        color = BrandRose,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (successMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandGreen.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, BrandGreen, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = successMessage!!,
                        color = BrandGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (authMode == "signup") {
                // Name Field
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BrandViolet) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandViolet,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fullname_input")
                        .padding(bottom = 12.dp)
                )
            }

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BrandViolet) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandViolet,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("email_input")
                    .padding(bottom = 12.dp)
            )

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (authMode == "forgot") "New Password" else "Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BrandViolet) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = BrandViolet
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandViolet,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("password_input")
                    .padding(bottom = if (authMode == "login") 4.dp else 12.dp)
            )

            if (authMode == "login") {
                // Forgot Password Link on the right side
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "Forgot Password?",
                        color = BrandViolet,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                authMode = "forgot"
                                viewModel.clearAuthError()
                            }
                            .testTag("forgot_password_link")
                    )
                }
            }

            if (authMode == "signup" || authMode == "forgot") {
                // Confirm Password Field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(if (authMode == "forgot") "Confirm New Password" else "Confirm Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BrandViolet) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandViolet,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_password_input")
                        .padding(bottom = 24.dp)
                )
            }

            // Primary Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(BrandViolet, BrandPink)))
                    .clickable {
                        viewModel.dismissSuccessMessage()
                        when (authMode) {
                            "signup" -> {
                                if (fullName.isBlank()) {
                                    viewModel.signUp(email, password, fullName) // will trigger error
                                    return@clickable
                                }
                                if (password != confirmPassword) {
                                    return@clickable
                                }
                                viewModel.signUp(email, password, fullName)
                            }
                            "forgot" -> {
                                if (email.isBlank() || password.isBlank()) return@clickable
                                if (password != confirmPassword) return@clickable
                                viewModel.resetPassword(email, password)
                            }
                            else -> {
                                viewModel.login(email, password)
                            }
                        }
                    }
                    .testTag("auth_submit_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (authMode) {
                        "signup" -> "Sign Up"
                        "forgot" -> "Reset Password"
                        else -> "Log In"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Toggle Text
            TextButton(
                onClick = {
                    viewModel.clearAuthError()
                    viewModel.dismissSuccessMessage()
                    authMode = if (authMode == "login") "signup" else "login"
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = when (authMode) {
                        "signup" -> "Already have an account? Log In"
                        "forgot" -> "Back to Log In"
                        else -> "Don't have an account? Sign Up"
                    },
                    color = BrandViolet,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your data is stored locally on this device, isolated per account.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}
