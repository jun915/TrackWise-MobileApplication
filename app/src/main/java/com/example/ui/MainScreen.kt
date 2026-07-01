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
import com.example.data.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    var leftDrawerOpen by remember { mutableStateOf(false) }

    val currentTheme = viewModel.themeMode.collectAsState().value
    val isSystemInDark = androidx.compose.foundation.isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current

    // Background selection based on dark/light/system
    val bgGradient = if (currentTheme == "dark" || (currentTheme == "system" && isSystemInDark)) {
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
                    },
                    leftDrawerOpen = leftDrawerOpen,
                    onMenuClick = { leftDrawerOpen = !leftDrawerOpen }
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
                    "analytics" -> AnalyticsScreen(viewModel = viewModel)
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

        // --- Semi-Transparent Backdrop Scrim when Drawer is Open ---
        if (leftDrawerOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { leftDrawerOpen = false }
            )
        }

        // --- Sliding Left Drawer Pane (Drawer Left Pane overlay) ---
        AnimatedVisibility(
            visible = leftDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.85f)
                .align(Alignment.CenterStart)
        ) {
            LeftDrawerPane(
                viewModel = viewModel,
                activeTab = activeTab,
                onNavigate = { activeTab = it },
                onClose = { leftDrawerOpen = false }
            )
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
    onTabSelected: (String) -> Unit,
    leftDrawerOpen: Boolean,
    onMenuClick: () -> Unit
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
            label = "Menu",
            icon = Icons.Default.Menu,
            isActive = leftDrawerOpen,
            isActiveColor = BrandViolet,
            onClick = onMenuClick
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

@Composable
fun LeftDrawerPane(
    viewModel: TrackWiseViewModel,
    activeTab: String,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit
) {
    val currentTheme by viewModel.themeMode.collectAsState()
    val taskSound by viewModel.taskSound.collectAsState()
    val alarmSound by viewModel.alarmSound.collectAsState()
    val themeAccent by viewModel.appThemeSelection.collectAsState()
    val detailedProfile by viewModel.userProfile.collectAsState()
    val friendConnections by viewModel.friendConnections.collectAsState()

    var settingsExpanded by remember { mutableStateOf(true) }
    var profileFormExpanded by remember { mutableStateOf(false) }
    var friendEmailInput by remember { mutableStateOf("") }
    val authError by viewModel.authError.collectAsState()

    // --- Profile Form Local State Binding ---
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Prefer not to say") }
    var maritalStatus by remember { mutableStateOf("Single") }
    var nationality by remember { mutableStateOf("") }
    var nationalId by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }

    var resAddress by remember { mutableStateOf("") }
    var permAddress by remember { mutableStateOf("") }
    var mobileNum by remember { mutableStateOf("") }
    var altPhone by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }

    var emergName by remember { mutableStateOf("") }
    var emergRelation by remember { mutableStateOf("") }
    var emergMobile by remember { mutableStateOf("") }
    var emergAltPhone by remember { mutableStateOf("") }
    var emergEmail by remember { mutableStateOf("") }

    // Sync database state with form
    LaunchedEffect(detailedProfile) {
        detailedProfile?.let {
            firstName = it.firstName
            middleName = it.middleName
            lastName = it.lastName
            dob = it.dob
            gender = it.gender
            maritalStatus = it.maritalStatus
            nationality = it.nationality
            nationalId = it.nationalId
            bloodGroup = it.bloodGroup
            resAddress = it.residentialStreet
            permAddress = it.permanentStreet
            mobileNum = it.mobileNumber
            altPhone = it.alternatePhone
            emailAddress = it.emailAddress
            emergName = it.emergencyName
            emergRelation = it.emergencyRelationship
            emergMobile = it.emergencyPhone
            emergAltPhone = it.alternateEmergencyPhone
            emergEmail = ""
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // --- Drawer Header ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { settingsExpanded = !settingsExpanded }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings Expanded",
                            tint = BrandViolet,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "TrackWise Left Pane",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Left Menu",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Analytics Center Navigation Link ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (activeTab == "analytics") BrandViolet.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigate("analytics")
                                onClose()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Assessment, contentDescription = null, tint = BrandViolet)
                                Text(
                                    text = "ANALYTICS CENTER",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (activeTab == "analytics") BrandViolet else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = if (activeTab == "analytics") BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                if (settingsExpanded) {
                    // --- Theme Modes (Light, Dark, System) ---
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("THEME MODE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("light", "dark", "system").forEach { mode ->
                                    val isSelected = currentTheme == mode
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) BrandViolet else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.setThemeMode(mode) }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = mode.uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- Theme Selection (Different Theme of App) ---
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("THEME DESIGN ACCENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Default Violet", "Ocean Blue", "Forest Green", "Sunset Orange", "Crimson Red").forEach { accent ->
                                    val isSelected = themeAccent == accent
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) BrandViolet else MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.dp, if (isSelected) BrandViolet else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.setAppThemeSelection(accent) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = accent.split(" ").last(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- Sound Selection for Task Completion ---
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("TASK COMPLETION SOUND", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Chime", "Ding", "Bell", "None").forEach { snd ->
                                    val isSelected = taskSound == snd
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) BrandViolet else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { viewModel.setTaskSound(snd) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = snd,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- Sound Selection for Alarm ---
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("ALARM ALERT SOUND", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Morning Birds", "Digital Beep", "Loud Siren", "Classic Bell").forEach { snd ->
                                    val isSelected = alarmSound == snd
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) BrandViolet else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { viewModel.setAlarmSound(snd) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = snd.split(" ").last(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- Sync, Import, Export Row ---
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("DATA & SYNC MANAGEMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.syncDeviceState() },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.2f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text("Sync States", fontSize = 11.sp, color = Color.White)
                                }

                                Button(
                                    onClick = { viewModel.exportData() },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text("Export", fontSize = 11.sp, color = Color.White)
                                }

                                Button(
                                    onClick = { viewModel.importData() },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandAmber),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text("Import", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    // --- Friends & Achievements (Interactive Section) ---
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("FRIENDS & ACHIEVEMENTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Add friend input row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = friendEmailInput,
                                        onValueChange = { friendEmailInput = it },
                                        placeholder = { Text("friend@trackwise.com", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BrandViolet,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            if (friendEmailInput.isNotBlank()) {
                                                viewModel.addFriend(friendEmailInput)
                                                friendEmailInput = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Add", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                                if (authError != null) {
                                    Text(authError!!, color = BrandRose, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                                // Real friend connections list
                                if (friendConnections.isEmpty()) {
                                    Text(
                                        text = "Social circle empty. Add friends to share progress!",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                } else {
                                    friendConnections.forEach { friend ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(friend.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text("Since ${friend.addedAt}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                            }
                                            IconButton(
                                                onClick = { viewModel.removeFriend(friend.friendUserId) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = null,
                                                    tint = BrandRose,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- My Profile Complete Form Expansion Header ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (profileFormExpanded) BrandViolet.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { profileFormExpanded = !profileFormExpanded }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = BrandViolet)
                                Text(
                                    text = "MY DETAILED PROFILE FORM",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = if (profileFormExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }
                }

                if (profileFormExpanded) {
                    // --- 1. Basic Personal Information ---
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "1. BASIC PERSONAL INFORMATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandViolet
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CompactTextField(
                                    value = firstName,
                                    onValueChange = { firstName = it },
                                    label = "First Name *",
                                    placeholder = "First",
                                    modifier = Modifier.weight(1f)
                                )
                                CompactTextField(
                                    value = middleName,
                                    onValueChange = { middleName = it },
                                    label = "Middle Name",
                                    placeholder = "Middle",
                                    modifier = Modifier.weight(1f)
                                )
                                CompactTextField(
                                    value = lastName,
                                    onValueChange = { lastName = it },
                                    label = "Last Name *",
                                    placeholder = "Last",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CompactTextField(
                                    value = dob,
                                    onValueChange = { dob = it },
                                    label = "Date of Birth *",
                                    placeholder = "DD/MM/YYYY",
                                    modifier = Modifier.weight(1.2f)
                                )
                                CompactTextField(
                                    value = bloodGroup,
                                    onValueChange = { bloodGroup = it },
                                    label = "Blood Group *",
                                    placeholder = "O+",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CompactTextField(
                                    value = nationality,
                                    onValueChange = { nationality = it },
                                    label = "Nationality *",
                                    placeholder = "Indian",
                                    modifier = Modifier.weight(1f)
                                )
                                CompactTextField(
                                    value = nationalId,
                                    onValueChange = { nationalId = it },
                                    label = "ID / Aadhaar / Passport *",
                                    placeholder = "National ID Num",
                                    modifier = Modifier.weight(1.2f)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Gender / Sex", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("Male", "Female", "Other", "Prefer not to say").forEach { gen ->
                                        val isSel = gender == gen
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSel) BrandViolet else MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { gender = gen }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(gen, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Marital Status", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("Single", "Married", "Divorced", "Widowed").forEach { status ->
                                        val isSel = maritalStatus == status
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSel) BrandViolet else MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { maritalStatus = status }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- 2. Contact & Address Details ---
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Text(
                                text = "2. CONTACT & ADDRESS DETAILS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandViolet
                            )

                            CompactTextField(
                                value = resAddress,
                                onValueChange = { resAddress = it },
                                label = "Residential Address *",
                                placeholder = "Street, City, State, ZIP, Country",
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = permAddress == resAddress && resAddress.isNotBlank(),
                                    onCheckedChange = { if (it) permAddress = resAddress }
                                )
                                Text("Permanent Address is same as Residential Address", fontSize = 10.sp)
                            }

                            CompactTextField(
                                value = permAddress,
                                onValueChange = { permAddress = it },
                                label = "Permanent Address *",
                                placeholder = "Street, City, State, ZIP, Country",
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CompactTextField(
                                    value = mobileNum,
                                    onValueChange = { mobileNum = it },
                                    label = "Mobile Number *",
                                    placeholder = "+91 XXXXX XXXXX",
                                    modifier = Modifier.weight(1f)
                                )
                                CompactTextField(
                                    value = altPhone,
                                    onValueChange = { altPhone = it },
                                    label = "Alternate Phone",
                                    placeholder = "+91 XXXXX XXXXX",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            CompactTextField(
                                value = emailAddress,
                                onValueChange = { emailAddress = it },
                                label = "Email Address *",
                                placeholder = "example@domain.com",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // --- 3. Emergency Contact Details ---
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Text(
                                text = "3. EMERGENCY CONTACT DETAILS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandViolet
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CompactTextField(
                                    value = emergName,
                                    onValueChange = { emergName = it },
                                    label = "Emergency Contact Name *",
                                    placeholder = "Full Name",
                                    modifier = Modifier.weight(1.2f)
                                )
                                CompactTextField(
                                    value = emergRelation,
                                    onValueChange = { emergRelation = it },
                                    label = "Relationship *",
                                    placeholder = "Parent / Spouse / Sibling",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CompactTextField(
                                    value = emergMobile,
                                    onValueChange = { emergMobile = it },
                                    label = "Emergency Mobile *",
                                    placeholder = "+91 XXXXX XXXXX",
                                    modifier = Modifier.weight(1f)
                                )
                                CompactTextField(
                                    value = emergAltPhone,
                                    onValueChange = { emergAltPhone = it },
                                    label = "Alternate Phone",
                                    placeholder = "+91 XXXXX XXXXX",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            CompactTextField(
                                value = emergEmail,
                                onValueChange = { emergEmail = it },
                                label = "Emergency Email Address *",
                                placeholder = "emergency@domain.com",
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    val user = viewModel.sessionUser.value
                                    if (user != null) {
                                        val profile = UserProfileEntity(
                                            userId = user.id,
                                            firstName = firstName,
                                            middleName = middleName,
                                            lastName = lastName,
                                            dob = dob,
                                            gender = gender,
                                            maritalStatus = maritalStatus,
                                            nationality = nationality,
                                            nationalId = nationalId,
                                            bloodGroup = bloodGroup,
                                            residentialStreet = resAddress,
                                            permanentStreet = permAddress,
                                            mobileNumber = mobileNum,
                                            alternatePhone = altPhone,
                                            emailAddress = emailAddress,
                                            emergencyName = emergName,
                                            emergencyRelationship = emergRelation,
                                            emergencyPhone = emergMobile,
                                            alternateEmergencyPhone = emergAltPhone
                                        )
                                        viewModel.saveDetailedProfile(profile)
                                        profileFormExpanded = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Detailed Profile Data", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // --- Footer Controls (Logout & Delete Account) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Logout", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.deleteAccount() },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRose),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Account", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
