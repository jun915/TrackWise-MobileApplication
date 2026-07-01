package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.utils.TrackWiseUtils
import kotlinx.coroutines.launch

import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource

@Composable
fun MainScreen(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf("dashboard") }
    val showSettings by viewModel.settingsPanelOpen.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val currentTheme = viewModel.themeMode.collectAsState().value
    val focusManager = LocalFocusManager.current

    // Background selection based on dark/light
    val bgGradient = if (currentTheme == "dark") {
        Brush.verticalGradient(listOf(DarkBgStart, DarkBgMid, DarkBgEnd))
    } else {
        Brush.verticalGradient(listOf(LightBgStart, LightBgMid, LightBgEnd))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Scaffold(
            topBar = {
                HeaderToolbar(
                    viewModel = viewModel,
                    activeTab = activeTab,
                    onNavigateToDashboard = { activeTab = "dashboard" }
                )
            },
            bottomBar = {
                BottomNavigationBar(
                    activeTab = activeTab,
                    onTabSelected = {
                        activeTab = it
                        viewModel.setSettingsPanelOpen(false) // Auto-close settings on tab swap
                    }
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                    }
            ) {
                // Main Content Switching
                when (activeTab) {
                    "dashboard" -> DashboardScreen(viewModel = viewModel)
                    "workspace" -> WorkspaceScreen(viewModel = viewModel)
                    "health" -> HealthScreen(viewModel = viewModel)
                    "calendar" -> CalendarScreen(viewModel = viewModel)
                    "friends" -> FriendsScreen(viewModel = viewModel)
                }

                // In-App Toast alerts (Section 13.4)
                successMessage?.let { msg ->
                    ToastAlert(
                        title = "Success notification",
                        message = msg,
                        onDismiss = { viewModel.dismissSuccessMessage() },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    )
                }

                if (isSyncing) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandGreen.copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    ) {
                        Text(
                            text = syncMessage,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // --- Animated Dropdown Settings Panel (Section 7.3) ---
                AnimatedVisibility(
                    visible = showSettings,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsPanel(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun HeaderToolbar(
    viewModel: TrackWiseViewModel,
    activeTab: String,
    onNavigateToDashboard: () -> Unit
) {
    val currentUser by viewModel.sessionUser.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val showSettings by viewModel.settingsPanelOpen.collectAsState()
    val currentTheme by viewModel.themeMode.collectAsState()

    // Sync Rotate Animation
    val rotation by animateFloatAsState(
        targetValue = if (isSyncing) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Side: Brand Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onNavigateToDashboard() }
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(BrandViolet, BrandPink))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "TrackWise",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = BrandViolet
            )
        }

        // Right Side Toolbar actions pill container
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Theme Toggle
            IconButton(onClick = {
                viewModel.setThemeMode(if (currentTheme == "dark") "light" else "dark")
            }) {
                Icon(
                    imageVector = if (currentTheme == "dark") Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme",
                    tint = BrandAmber,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Sync Indicator Action (Fake Sync Trigger 1.2s)
            IconButton(onClick = { viewModel.triggerFakeSync() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync Now",
                    tint = BrandCyan,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(if (isSyncing) rotation else 0f)
                )
            }

            // Settings Sheet Toggle
            IconButton(onClick = { viewModel.setSettingsPanelOpen(!showSettings) }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (showSettings) BrandViolet else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Log Out Trigger
            IconButton(onClick = { viewModel.logout() }) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Log Out",
                    tint = BrandRose,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface) // Solid themed background
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomTabItem(
            label = "Dashboard",
            icon = Icons.Default.Dashboard,
            isActive = activeTab == "dashboard",
            onClick = { onTabSelected("dashboard") }
        )
        BottomTabItem(
            label = "Workspace",
            icon = Icons.Default.Work,
            isActive = activeTab == "workspace",
            onClick = { onTabSelected("workspace") }
        )
        BottomTabItem(
            label = "Health",
            icon = Icons.Default.Favorite,
            isActive = activeTab == "health",
            isActiveColor = BrandGreen,
            onClick = { onTabSelected("health") }
        )
        BottomTabItem(
            label = "Calendar",
            icon = Icons.Default.CalendarToday,
            isActive = activeTab == "calendar",
            isActiveColor = BrandCyan,
            onClick = { onTabSelected("calendar") }
        )
        BottomTabItem(
            label = "Friends",
            icon = Icons.Default.People,
            isActive = activeTab == "friends",
            onClick = { onTabSelected("friends") }
        )
    }
}

@Composable
fun BottomTabItem(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    isActiveColor: Color = BrandViolet,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) isActiveColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// --- Settings Profile panel slide-down panel ---
@Composable
fun SettingsPanel(viewModel: TrackWiseViewModel) {
    val currentUser by viewModel.sessionUser.collectAsState()

    var nameInput by remember { mutableStateOf(currentUser?.fullName ?: "") }
    var heightInput by remember { mutableStateOf(currentUser?.heightCm?.toString() ?: "") }
    var weightInput by remember { mutableStateOf(currentUser?.weightKg?.toString() ?: "") }
    var phoneInput by remember { mutableStateOf(currentUser?.phone ?: "") }
    var cityInput by remember { mutableStateOf(currentUser?.city ?: "") }
    var stateInput by remember { mutableStateOf(currentUser?.state ?: "Delhi") }
    var zipInput by remember { mutableStateOf(currentUser?.zipCode ?: "") }
    var waterGoalInput by remember { mutableStateOf(currentUser?.waterGoalGlasses?.toString() ?: "8") }

    // Boolean stats share trigger
    var shareStats by remember { mutableStateOf(currentUser?.enabledConditions?.contains("share_stats") ?: true) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("PROFILE & APP CONFIGURATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandViolet)

            CompactTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = "Display / Full Name",
                placeholder = "Your Name",
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactTextField(
                    value = heightInput,
                    onValueChange = { heightInput = it },
                    label = "Height (cm)",
                    placeholder = "175",
                    modifier = Modifier.weight(1f)
                )

                CompactTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = "Weight (kg)",
                    placeholder = "70",
                    modifier = Modifier.weight(1f)
                )

                CompactTextField(
                    value = waterGoalInput,
                    onValueChange = { waterGoalInput = it },
                    label = "Water goal",
                    placeholder = "8",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = "Phone (+91)",
                    placeholder = "9876543210",
                    modifier = Modifier.weight(1.5f)
                )

                CompactTextField(
                    value = zipInput,
                    onValueChange = { zipInput = it },
                    label = "PIN Code",
                    placeholder = "110001",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactTextField(
                    value = cityInput,
                    onValueChange = { cityInput = it },
                    label = "City",
                    placeholder = "New Delhi",
                    modifier = Modifier.weight(1f)
                )

                CompactTextField(
                    value = stateInput,
                    onValueChange = { stateInput = it },
                    label = "State (India)",
                    placeholder = "Delhi",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { shareStats = !shareStats }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = shareStats,
                    onCheckedChange = { shareStats = it },
                    colors = CheckboxDefaults.colors(checkedColor = BrandViolet)
                )
                Text(
                    text = "Share my analytics/stats with social circle friends",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Button(
                onClick = {
                    viewModel.saveProfile(
                        fullName = nameInput,
                        dob = currentUser?.dob,
                        gender = currentUser?.gender,
                        heightCm = heightInput.toDoubleOrNull(),
                        weightKg = weightInput.toDoubleOrNull(),
                        phone = phoneInput,
                        addressLine1 = currentUser?.addressLine1,
                        addressLine2 = currentUser?.addressLine2,
                        city = cityInput,
                        state = stateInput,
                        zipCode = zipInput,
                        bloodType = currentUser?.bloodType,
                        waterGoal = waterGoalInput.toIntOrNull() ?: 8,
                        conditions = if (shareStats) "share_stats" else ""
                    )
                    viewModel.setSettingsPanelOpen(false)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Profile Changes", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ToastAlert(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth(0.9f)
            .border(1.dp, BrandGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onDismiss() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = BrandGreen)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                Text(message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
