package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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

    var showImportOptionDialog by remember { mutableStateOf(false) }
    var pastedJsonText by remember { mutableStateOf("") }
    var showPasteInputArea by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importDataFromUri(uri)
        }
    }

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
                    onNavigateToDashboard = { activeTab = "dashboard" },
                    onNavigateToSubTab = { tab, subTab ->
                        activeTab = tab
                        viewModel.setWorkspaceSubTab(subTab)
                    }
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
                    "finance" -> FinanceScreen(viewModel = viewModel)
                    "analytics" -> AnalyticsScreen(viewModel = viewModel)
                    "profile" -> ProfileScreen(viewModel = viewModel)
                    "social" -> SocialScreen(viewModel = viewModel)
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
                onClose = { leftDrawerOpen = false },
                onImportClick = { showImportOptionDialog = true }
            )
        }
    }

    // Backup Restore Option Dialog
    if (showImportOptionDialog) {
        AlertDialog(
            onDismissRequest = { 
                showImportOptionDialog = false 
                showPasteInputArea = false
                pastedJsonText = ""
            },
            title = {
                Text("Restore Backup Data", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Choose a method to import your TrackWise backup data (.json):",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )

                    if (!showPasteInputArea) {
                        Button(
                            onClick = {
                                showImportOptionDialog = false
                                filePickerLauncher.launch("application/json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse JSON File", color = Color.White)
                        }

                        Button(
                            onClick = {
                                showPasteInputArea = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPink),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Paste JSON Text", color = Color.White)
                        }

                        OutlinedButton(
                            onClick = {
                                showImportOptionDialog = false
                                viewModel.importData() // Local cached backup search
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(Icons.Default.Cached, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import Last Auto-Backup", color = MaterialTheme.colorScheme.onBackground)
                        }
                    } else {
                        OutlinedTextField(
                            value = pastedJsonText,
                            onValueChange = { pastedJsonText = it },
                            label = { Text("Paste JSON backup string here") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandViolet,
                                focusedLabelColor = BrandViolet
                            )
                        )
                    }
                }
            },
            confirmButton = {
                if (showPasteInputArea) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { showPasteInputArea = false }
                        ) {
                            Text("Back", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }
                        Button(
                            onClick = {
                                if (pastedJsonText.isNotBlank()) {
                                    viewModel.importData(pastedJsonText)
                                }
                                showImportOptionDialog = false
                                showPasteInputArea = false
                                pastedJsonText = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Import data", color = Color.White)
                        }
                    }
                } else {
                    TextButton(
                        onClick = { showImportOptionDialog = false }
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
            }
        )
    }
}

@Composable
fun HeaderToolbar(
    viewModel: TrackWiseViewModel,
    activeTab: String,
    onNavigateToDashboard: () -> Unit,
    onNavigateToSubTab: (String, Int) -> Unit
) {
    val currentUser by viewModel.sessionUser.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showAddChoiceDialog by remember { mutableStateOf(false) }

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

        // Right Side: Quick navigation (+) icon beside Notification Bell Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quick Add '+' Button
            IconButton(
                onClick = { showAddChoiceDialog = true },
                modifier = Modifier.testTag("quick_add_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Add",
                    tint = BrandViolet,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Notification Bell Icon
            Box {
                BadgedBox(
                    badge = {
                        if (notifications.isNotEmpty()) {
                            Badge(
                                containerColor = BrandPink,
                                contentColor = Color.White
                            ) {
                                Text(notifications.size.toString())
                            }
                        }
                    }
                ) {
                    IconButton(
                        onClick = { showNotificationsDialog = true },
                        modifier = Modifier.testTag("notification_bell_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = BrandViolet,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }

    // Quick Add Navigation Choice Dialog
    if (showAddChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showAddChoiceDialog = false },
            title = {
                Text(
                    text = "Launch / Quick Navigation",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Select a workspace section to open directly:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val choices = listOf(
                        Triple("Task Checklist", "Manage daily to-dos & milestones", Icons.Default.Assignment),
                        Triple("Habit Runways", "Track daily routines & streaks", Icons.Default.Repeat),
                        Triple("Wishlist Items", "Plan personal purchases & products", Icons.Default.Star),
                        Triple("Birthdays Log", "Keep track of friends & special days", Icons.Default.Cake),
                        Triple("Alarms & Clocks", "Manage waking hours & reminders", Icons.Default.Alarm),
                        Triple("Grocery Check List", "Surgical shopping checklist & qty", Icons.Default.ShoppingCart)
                    )

                    choices.forEachIndexed { index, choice ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onNavigateToSubTab("workspace", index)
                                    showAddChoiceDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(BrandViolet.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = choice.third,
                                        contentDescription = null,
                                        tint = BrandViolet,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = choice.first,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = choice.second,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAddChoiceDialog = false }
                ) {
                    Text("Close", color = BrandViolet, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Notifications Dialog
    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (notifications.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearNotifications() }
                        ) {
                            Text("Clear All", color = BrandRose, fontSize = 13.sp)
                        }
                    }
                }
            },
            text = {
                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "All caught up!",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notifications) { item ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = BrandViolet
                                        )
                                        Text(
                                            text = item.timestamp,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.message,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showNotificationsDialog = false }
                ) {
                    Text("Close", color = BrandViolet)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
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
            label = "Finance",
            icon = Icons.Default.AttachMoney,
            isActive = activeTab == "finance",
            isActiveColor = BrandOrange,
            onClick = { onTabSelected("finance") }
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
    onClose: () -> Unit,
    onImportClick: () -> Unit
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
                // --- Finance Tracker Navigation Link ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (activeTab == "finance") BrandViolet.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigate("finance")
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
                                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = BrandViolet)
                                Text(
                                    text = "FINANCE TRACKER",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (activeTab == "finance") BrandViolet else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = if (activeTab == "finance") BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

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

                // --- Detailed Profile Navigation Link ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (activeTab == "profile") BrandViolet.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigate("profile")
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
                                Icon(Icons.Default.AccountBox, contentDescription = null, tint = BrandViolet)
                                Text(
                                    text = "DETAILED PROFILE FORM",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (activeTab == "profile") BrandViolet else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = if (activeTab == "profile") BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
                                    onClick = { onImportClick() },
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

                    // --- Friends & Achievements Navigation Link ---
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (activeTab == "social") BrandViolet.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onNavigate("social")
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
                                    Icon(Icons.Default.Group, contentDescription = null, tint = BrandViolet)
                                    Text(
                                        text = "FRIENDS & ACHIEVEMENTS",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (activeTab == "social") BrandViolet else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = if (activeTab == "social") BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
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
