package com.example.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.ui.theme.*
import com.example.utils.TrackWiseUtils
import com.example.data.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.launch
import java.util.Calendar

import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun MainScreen(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    var navigationHistory by remember { mutableStateOf(listOf("dashboard")) }
    val activeTab = navigationHistory.lastOrNull() ?: "dashboard"

    fun navigateTo(tab: String) {
        if (activeTab != tab) {
            val currentList = navigationHistory.toMutableList()
            if (tab == "dashboard") {
                navigationHistory = listOf("dashboard")
            } else {
                val existingIdx = currentList.indexOf(tab)
                if (existingIdx != -1) {
                    navigationHistory = currentList.subList(0, existingIdx + 1)
                } else {
                    currentList.add(tab)
                    navigationHistory = currentList
                }
            }
        }
    }

    LaunchedEffect(activeTab) {
        if (activeTab == "dashboard") {
            viewModel.closeCustomTaskSheet()
        }
    }

    fun navigateBack() {
        if (navigationHistory.size > 1) {
            navigationHistory = navigationHistory.dropLast(1)
        }
    }

    val notificationNavTab by viewModel.notificationNavigateTab.collectAsState()
    LaunchedEffect(notificationNavTab) {
        notificationNavTab?.let {
            navigateTo(it)
            viewModel.setNotificationNavigateTab(null)
        }
    }

    val habitToEdit by viewModel.habitToEdit.collectAsState()
    LaunchedEffect(habitToEdit) {
        if (habitToEdit != null) {
            navigateTo("workspace")
            viewModel.setWorkspaceSubTab(1)
        }
    }
    val showSettings by viewModel.settingsPanelOpen.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val currentUser by viewModel.sessionUser.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    var pullOffset by remember { mutableStateOf(0f) }
    val animatedPullOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isRefreshing || isSyncing) 60f else pullOffset,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "pull_offset_anim"
    )

    val nestedScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                return if (available.y < 0 && pullOffset > 0f) {
                    val consumed = pullOffset.coerceAtMost(-available.y)
                    pullOffset -= consumed
                    androidx.compose.ui.geometry.Offset(0f, -consumed)
                } else {
                    androidx.compose.ui.geometry.Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                return if (available.y > 0 && consumed.y == 0f && !isRefreshing) {
                    viewModel.dismissSuccessMessage()
                    pullOffset = (pullOffset + available.y * 0.45f).coerceAtMost(140f)
                    androidx.compose.ui.geometry.Offset(0f, available.y)
                } else {
                    if (consumed.y > 0f) {
                        pullOffset = 0f
                    }
                    androidx.compose.ui.geometry.Offset.Zero
                }
            }

            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (pullOffset > 100f && !isRefreshing) {
                    isRefreshing = true
                    viewModel.dismissSuccessMessage()
                    viewModel.syncDeviceState()
                }
                pullOffset = 0f
                return androidx.compose.ui.unit.Velocity.Zero
            }

            override suspend fun onPostFling(
                consumed: androidx.compose.ui.unit.Velocity,
                available: androidx.compose.ui.unit.Velocity
            ): androidx.compose.ui.unit.Velocity {
                if (pullOffset > 100f && !isRefreshing) {
                    isRefreshing = true
                    viewModel.dismissSuccessMessage()
                    viewModel.syncDeviceState()
                }
                pullOffset = 0f
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

    LaunchedEffect(isSyncing) {
        if (!isSyncing) {
            isRefreshing = false
            pullOffset = 0f
            viewModel.dismissSuccessMessage()
        }
    }
    var leftDrawerOpen by remember { mutableStateOf(false) }
    var showGlobalNotificationsDialog by remember { mutableStateOf(false) }
    var showGlobalSearchDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showAddChoiceDialog by remember { mutableStateOf(false) }
    var showOccasionSpeedDial by remember { mutableStateOf(false) }
    var showMainSpeedDial by remember { mutableStateOf(false) }

    val showCustomTaskSheet by viewModel.showCustomTaskSheet.collectAsState()
    val showHabitCreationSheet by viewModel.showHabitCreationSheet.collectAsState()
    val activeDetailHabit by viewModel.activeDetailHabit.collectAsState()
    val taskToEdit by viewModel.taskToEdit.collectAsState()

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

    val currentTheme by viewModel.themeMode.collectAsState()
    val themeAccent by viewModel.appThemeSelection.collectAsState()
    val isSystemInDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    val isDark = when (currentTheme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDark
    }
    val focusManager = LocalFocusManager.current

    val isAnyPopupOpen = activeDetailHabit != null || showCustomTaskSheet || showHabitCreationSheet || showAddChoiceDialog || leftDrawerOpen || showMoreMenu || showSettings || showMainSpeedDial || showOccasionSpeedDial

    BackHandler(enabled = isAnyPopupOpen || navigationHistory.size > 1) {
        if (activeDetailHabit != null) {
            viewModel.setActiveDetailHabit(null)
        } else if (showCustomTaskSheet) {
            viewModel.closeCustomTaskSheet()
        } else if (showHabitCreationSheet) {
            viewModel.closeHabitCreationSheet()
        } else if (showAddChoiceDialog) {
            showAddChoiceDialog = false
        } else if (showMainSpeedDial) {
            showMainSpeedDial = false
        } else if (showOccasionSpeedDial) {
            showOccasionSpeedDial = false
        } else if (leftDrawerOpen) {
            leftDrawerOpen = false
        } else if (showMoreMenu) {
            showMoreMenu = false
        } else if (showSettings) {
            viewModel.setSettingsPanelOpen(false)
        } else {
            navigateBack()
        }
    }

    com.example.ui.theme.AppBackground(
        viewModel = viewModel,
        isDark = isDark,
        modifier = modifier
    ) {
        val activeSubTab by viewModel.workspaceSubTab.collectAsState()
        val isMoreMenuActive = showMoreMenu || 
                activeTab in listOf("health", "finance", "calendar") ||
                (activeTab == "workspace" && activeSubTab in listOf(2, 4, 5))

        Scaffold(
            topBar = {
                if (activeDetailHabit == null) {
                    HeaderToolbar(
                        viewModel = viewModel,
                        activeTab = activeTab,
                        onMenuClick = { leftDrawerOpen = !leftDrawerOpen },
                        onNavigateToDashboard = {
                            viewModel.setActiveDetailHabit(null)
                            navigateTo("dashboard")
                        },
                        onNavigateToSubTab = { tab, subTab ->
                            viewModel.setActiveDetailHabit(null)
                            navigateTo(tab)
                            viewModel.setWorkspaceSubTab(subTab)
                        }
                    )
                }
            },
            bottomBar = {
                if (activeDetailHabit == null) {
                    BottomNavigationBar(
                        activeTab = activeTab,
                        viewModel = viewModel,
                        onTabSelected = {
                            viewModel.setActiveDetailHabit(null)
                            navigateTo(it)
                            showMoreMenu = false
                            showMainSpeedDial = false
                            showOccasionSpeedDial = false
                            viewModel.setSettingsPanelOpen(false) // Auto-close settings on tab swap
                        },
                        onSubTabSelected = { tab, subTab ->
                            viewModel.setActiveDetailHabit(null)
                            navigateTo(tab)
                            viewModel.setWorkspaceSubTab(subTab)
                            showMoreMenu = false
                            showMainSpeedDial = false
                            showOccasionSpeedDial = false
                            viewModel.setSettingsPanelOpen(false)
                        },
                        onMoreMenuClick = {
                            val next = !showMoreMenu
                            showMoreMenu = next
                            if (next) {
                                showMainSpeedDial = false
                                showOccasionSpeedDial = false
                            }
                        },
                        isMoreMenuActive = isMoreMenuActive
                    )
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .nestedScroll(nestedScrollConnection)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                    }
            ) {
                // Main Content Switching
                when (activeTab) {
                    "dashboard" -> DashboardScreen(viewModel = viewModel, onNavigate = { navigateTo(it) })
                    "workspace" -> WorkspaceScreen(viewModel = viewModel)
                    "folders" -> TaskFoldersScreen(
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onNavigateToWorkspaceWithFolder = { folder ->
                            viewModel.setSelectedTaskFolder(folder)
                            navigateTo("workspace")
                            viewModel.setWorkspaceSubTab(0)
                        }
                    )
                    "tags" -> HashtagsScreen(
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onNavigateToWorkspaceWithTag = { tag ->
                            viewModel.setSelectedTaskTag(tag)
                            navigateTo("workspace")
                            viewModel.setWorkspaceSubTab(0)
                        }
                    )
                    "health" -> HealthScreen(viewModel = viewModel)
                    "calendar" -> CalendarScreen(viewModel = viewModel, onNavigateToSeerah = { navigateTo("seerah") })
                    "finance" -> FinanceScreen(viewModel = viewModel)
                    "analytics" -> AnalyticsScreen(viewModel = viewModel)
                    "profile" -> ProfileScreen(
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onNavigateToSocial = { navigateTo("social") }
                    )
                    "settings" -> SettingsScreen(viewModel = viewModel, onBack = { navigateBack() }, onImportClick = { showImportOptionDialog = true })
                    "social" -> SocialScreen(viewModel = viewModel)
                    "help" -> HelpScreen(onBack = { navigateBack() })
                    "archive" -> ArchiveScreen(viewModel = viewModel, onBack = { navigateBack() })
                    "seerah" -> SeerahScreen(viewModel = viewModel, onBack = { navigateBack() })
                }

                // Global Pull to Refresh circle indicator overlay
                AnimatedVisibility(
                    visible = animatedPullOffset > 10f || isRefreshing || isSyncing,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .offset(y = (animatedPullOffset * 0.40f).coerceAtMost(36f).dp)
                ) {
                    Card(
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isRefreshing || isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Pull to Refresh",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate((animatedPullOffset * 2).coerceIn(0f, 360f))
                                )
                            }
                        }
                    }
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

                // --- Animated Dropdown Settings Panel (Section 7.3) ---
                AnimatedVisibility(
                    visible = showSettings,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsPanel(viewModel = viewModel)
                }

                // Global Habit Detail Screen Overlay
                val activeDetailHabit by viewModel.activeDetailHabit.collectAsState()
                activeDetailHabit?.let { habit ->
                    HabitDetailScreen(
                        habitId = habit.id,
                        viewModel = viewModel,
                        onBack = { viewModel.setActiveDetailHabit(null) },
                        onEditHabit = { habitToEdit ->
                            viewModel.setActiveDetailHabit(null)
                            viewModel.setHabitToEdit(habitToEdit)
                        }
                    )
                }
            }
        }

        // --- Full Screen Backdrop Scrim when Speed Dial or Add Popup is active ---
        if (showMainSpeedDial || showOccasionSpeedDial || showAddChoiceDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showMainSpeedDial = false
                        showOccasionSpeedDial = false
                        showAddChoiceDialog = false
                    }
            )
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

        // --- Semi-Transparent Backdrop Scrim when More Menu is Open ---
        if (showMoreMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { showMoreMenu = false }
            )
        }

        // --- Sliding More Menu Slider Pane ---
        AnimatedVisibility(
            visible = showMoreMenu,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 84.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .testTag("more_menu_slider")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MORE TRACKERS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandViolet,
                            letterSpacing = 1.2.sp
                        )
                        IconButton(
                            onClick = { showMoreMenu = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Menu",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    val moreItems = listOf(
                        MoreMenuItemSpec("Finance", Icons.Default.AttachMoney, BrandOrange, "finance", -1),
                        MoreMenuItemSpec("Wishlist", Icons.Default.Star, BrandPink, "workspace", 2),
                        MoreMenuItemSpec("Timer & Stopwatch", Icons.Default.Timer, BrandIndigo, "workspace", 4),
                        MoreMenuItemSpec("Grocery List", Icons.Default.ShoppingCart, BrandGreen, "workspace", 5)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (row in 0 until 3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (col in 0 until 2) {
                                    val itemIdx = row * 2 + col
                                    if (itemIdx < moreItems.size) {
                                        val item = moreItems[itemIdx]
                                        val activeSubTabVal = viewModel.workspaceSubTab.collectAsState().value
                                        val isActive = if (item.subTab == -1) {
                                            activeTab == item.tab
                                        } else {
                                            activeTab == item.tab && activeSubTabVal == item.subTab
                                        }

                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isActive) item.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                            border = if (isActive) BorderStroke(1.dp, item.color.copy(alpha = 0.4f)) else null,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    if (item.subTab == -1) {
                                                        navigateTo(item.tab)
                                                    } else {
                                                        navigateTo(item.tab)
                                                        viewModel.setWorkspaceSubTab(item.subTab)
                                                    }
                                                    showMoreMenu = false
                                                }
                                                .testTag("more_menu_item_${item.label.lowercase().replace(" ", "_").replace("& ", "")}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(item.color.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = item.icon,
                                                        contentDescription = null,
                                                        tint = item.color,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Text(
                                                    text = item.label,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isActive) item.color else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                onNavigate = { navigateTo(it) },
                onClose = { leftDrawerOpen = false },
                onImportClick = { showImportOptionDialog = true },
                onNotificationClick = { showGlobalNotificationsDialog = true },
                onSearchClick = { showGlobalSearchDialog = true }
            )
        }

        if (showGlobalNotificationsDialog) {
            NotificationsDialog(
                viewModel = viewModel,
                onDismiss = { showGlobalNotificationsDialog = false }
            )
        }

        if (showGlobalSearchDialog) {
            GlobalSearchDialog(
                viewModel = viewModel,
                onDismiss = { showGlobalSearchDialog = false },
                onNavigate = { navigateTo(it) }
            )
        }

        // --- Onboarding Popup / Overlay ---
        val user = currentUser
        val needsOnboarding = user != null && (
            user.dob.isNullOrBlank() ||
            user.gender.isNullOrBlank() ||
            user.phone.isNullOrBlank() ||
            user.religion.isNullOrBlank()
        )

        if (needsOnboarding) {
            OnboardingOverlay(
                viewModel = viewModel,
                currentUser = user!!
            )
        }

        // --- Floating Action Button & Speed Dials Rendered at Root level above the Scrims ---
        val activeDetailHabit by viewModel.activeDetailHabit.collectAsState()
        if (activeDetailHabit == null && !needsOnboarding) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 16.dp, bottom = 100.dp)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.BottomEnd
            ) {
                if (activeTab == "workspace" && activeSubTab == 3) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (showOccasionSpeedDial) {
                            val speedDialOptions = listOf(
                                Triple("countdown", "Countdown", Icons.Default.HourglassEmpty),
                                Triple("marriage anniversary", "Marriage Anniversary", Icons.Default.Favorite),
                                Triple("death anniversary", "Death Anniversary", Icons.Default.LocalFlorist),
                                Triple("birthday", "Birthday", Icons.Default.Cake),
                                Triple("holiday", "Holiday", Icons.Default.Star)
                            )

                            speedDialOptions.forEach { (key, label, icon) ->
                                val color = when (key) {
                                    "countdown" -> MaterialTheme.colorScheme.secondary
                                    "marriage anniversary" -> MaterialTheme.colorScheme.tertiary
                                    "death anniversary" -> MaterialTheme.colorScheme.primary
                                    "birthday" -> BrandAmber
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.clickable {
                                        viewModel.triggerAddOccasion(label)
                                        showOccasionSpeedDial = false
                                    }
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 4.dp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    FloatingActionButton(
                                        onClick = {
                                            viewModel.triggerAddOccasion(label)
                                            showOccasionSpeedDial = false
                                        },
                                        containerColor = color,
                                        contentColor = Color.White,
                                        modifier = Modifier.size(44.dp),
                                        shape = CircleShape
                                    ) {
                                        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        FloatingActionButton(
                            onClick = {
                                showMoreMenu = false
                                showOccasionSpeedDial = !showOccasionSpeedDial
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("occasion_speed_dial_button")
                        ) {
                            Icon(
                                imageVector = if (showOccasionSpeedDial) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Add Occasion",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else if (activeTab == "workspace" && activeSubTab == 0) {
                    FloatingActionButton(
                        onClick = {
                            showMoreMenu = false
                            viewModel.openAddTaskSheet()
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .offset(y = 8.dp)
                            .testTag("floating_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Quick Add",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (activeTab == "workspace" && activeSubTab == 1) {
                    FloatingActionButton(
                        onClick = {
                            showMoreMenu = false
                            viewModel.openHabitCreationSheet()
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .offset(y = 8.dp)
                            .testTag("floating_add_habit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Habit",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (showMainSpeedDial) {
                            val mainSpeedDialOptions = listOf(
                                Triple("analytics", "Analytics Center", Icons.Default.BarChart),
                                Triple("social", "Friends & Achievements", Icons.Default.EmojiEvents),
                                Triple("settings", "Settings", Icons.Default.Settings),
                                Triple("archive", "Completed & Archived Items", Icons.Default.Archive)
                            )

                            mainSpeedDialOptions.forEach { (key, label, icon) ->
                                val color = when (key) {
                                    "analytics" -> BrandCyan
                                    "social" -> BrandOrange
                                    "settings" -> BrandViolet
                                    "archive" -> BrandPink
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.clickable {
                                        showMoreMenu = false
                                        showMainSpeedDial = false
                                        navigateTo(key)
                                    }
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 4.dp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    FloatingActionButton(
                                        onClick = {
                                            showMoreMenu = false
                                            showMainSpeedDial = false
                                            navigateTo(key)
                                        },
                                        containerColor = color,
                                        contentColor = Color.White,
                                        modifier = Modifier.size(44.dp),
                                        shape = CircleShape
                                    ) {
                                        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        FloatingActionButton(
                            onClick = {
                                showMoreMenu = false
                                showMainSpeedDial = !showMainSpeedDial
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(56.dp)
                                .offset(y = 8.dp)
                                .testTag("floating_add_button")
                        ) {
                            Icon(
                                imageVector = if (showMainSpeedDial) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Quick Add",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
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

    // Quick Add Navigation Choice Dialog in MainScreen
    if (showAddChoiceDialog) {
        var searchQuery by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddChoiceDialog = false },
            title = {
                Text(
                    text = "Quick Navigation & Tools",
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
                    // Search at the top!
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search actions & tasks...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(16.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(12.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    )

                    val choices = listOf(
                        Triple("Analytics Center", "View stats and progress reports", Icons.Default.BarChart),
                        Triple("Friends & Achievements", "Connect with friends & track medals", Icons.Default.EmojiEvents),
                        Triple("Settings", "Configure theme, fonts, & preferences", Icons.Default.Settings),
                        Triple("Completed & Archived Items", "View past achievements & archived tasks", Icons.Default.Archive)
                    )

                    val filteredChoices = choices.filter {
                        it.first.contains(searchQuery, ignoreCase = true) ||
                        it.second.contains(searchQuery, ignoreCase = true)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredChoices.forEach { choice ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showAddChoiceDialog = false
                                        when (choice.first) {
                                            "Analytics Center" -> navigateTo("analytics")
                                            "Friends & Achievements" -> navigateTo("social")
                                            "Settings" -> navigateTo("settings")
                                            "Completed & Archived Items" -> navigateTo("archive")
                                        }
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

    val tasksForSuggestions by viewModel.allTasks.collectAsState()
    val customFoldersForSuggestions by viewModel.customFolders.collectAsState()
    val customTagsForSuggestions by viewModel.customTags.collectAsState()

    val initialProjectsList = remember(tasksForSuggestions, customFoldersForSuggestions) {
        val defaults = listOf("Inbox", "Work", "Personal", "Shopping", "Learning", "Wish List", "Fitness", "Welcome")
        val dynamic = tasksForSuggestions.map { it.project }.filter { it.isNotBlank() }
        (defaults + customFoldersForSuggestions + dynamic).distinct()
    }

    val initialTagsList = remember(tasksForSuggestions, customTagsForSuggestions) {
        val defaults = listOf("daily routine", "work", "fitness", "learning")
        val extracted = mutableSetOf<String>()
        tasksForSuggestions.forEach { t ->
            val textToSearch = "${t.title} ${t.description} ${t.notes}"
            val words = textToSearch.split(" ", "\n", ",", ";")
            words.forEach { word ->
                if (word.startsWith("#") && word.length > 1) {
                    extracted.add(word.removePrefix("#"))
                }
            }
        }
        (defaults + customTagsForSuggestions + extracted).map { if (it.startsWith("#")) it else "#$it" }.distinct()
    }

    CustomAddTaskBottomSheet(
        visible = showCustomTaskSheet,
        onDismiss = { viewModel.closeCustomTaskSheet() },
        onAddTask = { titleVal, descVal, projVal, priorityVal, deadlineVal, reminderTimeVal, repeatTypeVal, reminderDateVal, notesVal, subtasksJsonVal ->
            if (projVal.isNotBlank()) {
                viewModel.addCustomFolder(projVal)
            }
            val wordsInNotes = notesVal.split(" ", "\n")
            wordsInNotes.forEach { word ->
                if (word.startsWith("#") && word.length > 1) {
                    viewModel.addCustomTag(word.removePrefix("#"))
                }
            }
            if (taskToEdit != null) {
                val updatedTask = taskToEdit!!.copy(
                    title = titleVal,
                    description = descVal,
                    project = projVal,
                    priority = priorityVal,
                    deadline = deadlineVal,
                    reminderTime = reminderTimeVal,
                    repeatType = repeatTypeVal,
                    reminderDate = reminderDateVal,
                    notes = notesVal,
                    subtasksJson = subtasksJsonVal
                )
                viewModel.updateTask(updatedTask)
            } else {
                viewModel.addTask(
                    title = titleVal,
                    description = descVal,
                    project = projVal,
                    priority = priorityVal,
                    points = 0,
                    deadline = deadlineVal,
                    reminderTime = reminderTimeVal,
                    repeatType = repeatTypeVal,
                    reminderDate = reminderDateVal,
                    notes = notesVal,
                    subtasksJson = subtasksJsonVal
                )
            }
            viewModel.closeCustomTaskSheet()
        },
        taskToEdit = taskToEdit,
        onDeleteTask = { taskId ->
            viewModel.deleteTask(taskId)
            viewModel.closeCustomTaskSheet()
        },
        initialProjects = initialProjectsList,
        initialTags = initialTagsList
    )

    if (showHabitCreationSheet) {
        HabitCreationFlowDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeHabitCreationSheet() }
        )
    }
}

private data class MoreMenuItemSpec(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val tab: String,
    val subTab: Int
)

@Composable
fun HeaderToolbar(
    viewModel: TrackWiseViewModel,
    activeTab: String,
    onMenuClick: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToSubTab: (String, Int) -> Unit
) {
    val activeSubTab by viewModel.workspaceSubTab.collectAsState()
    val currentTitle = when (activeTab) {
        "dashboard" -> "Dashboard"
        "workspace" -> {
            when (activeSubTab) {
                0 -> "Task Checklist"
                1 -> "Habit"
                2 -> "Wishlist"
                3 -> "Countdown"
                4 -> "Timer & Stopwatch"
                5 -> "Grocery Checklist"
                else -> "Workspace"
            }
        }
        "health" -> "Health"
        "calendar" -> "Calendar"
        "finance" -> "Finance"
        "analytics" -> "Analytics"
        "settings", "profile" -> "Settings"
        "folders" -> "Task & Habit Folders"
        "tags" -> "Hashtags (#)"
        "social" -> "Friends"
        "help" -> "How It Works"
        "archive" -> "Archive"
        "seerah" -> "Seerah"
        else -> "TrackWise"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Side: Hamburger Menu & Dynamic Title
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("top_hamburger_menu")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Drawer",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currentTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Right Side: Health & Calendar actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
                    onNavigateToSubTab("health", -1)
                },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("top_health_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Open Health",
                    tint = BrandGreen,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = {
                    onNavigateToSubTab("calendar", -1)
                },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("top_calendar_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Open Calendar",
                    tint = BrandViolet,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun NotificationsDialog(
    viewModel: TrackWiseViewModel,
    onDismiss: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val areNotificationsEnabled = remember {
        androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (notifications.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearNotifications() }
                        ) {
                            Text("Clear All", color = BrandRose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!areNotificationsEnabled) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = BrandRose.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = BrandRose,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notifications Blocked",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = BrandRose
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Real phone push alerts require permissions. Tap here to open App Settings, choose Notifications, and turn them on.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Close", color = BrandViolet)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun BottomNavigationBar(
    activeTab: String,
    viewModel: TrackWiseViewModel,
    onTabSelected: (String) -> Unit,
    onSubTabSelected: (String, Int) -> Unit,
    onMoreMenuClick: () -> Unit,
    isMoreMenuActive: Boolean
) {
    val barBgColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    val activeSubTab by viewModel.workspaceSubTab.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(barBgColor)
            .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomTabItem(
            label = "Dashboard",
            icon = Icons.Default.Dashboard,
            isActive = activeTab == "dashboard",
            onClick = { onTabSelected("dashboard") }
        )
        BottomTabItem(
            label = "Tasks",
            icon = Icons.Default.Assignment,
            isActive = activeTab == "workspace" && activeSubTab == 0,
            isActiveColor = BrandViolet,
            onClick = { onSubTabSelected("workspace", 0) }
        )
        BottomTabItem(
            label = "Habits",
            icon = Icons.Default.Repeat,
            isActive = activeTab == "workspace" && activeSubTab == 1,
            isActiveColor = BrandPink,
            onClick = { onSubTabSelected("workspace", 1) }
        )
        BottomTabItem(
            label = "Countdown",
            icon = Icons.Default.HourglassEmpty,
            isActive = activeTab == "workspace" && activeSubTab == 3,
            isActiveColor = BrandOrange,
            onClick = { onSubTabSelected("workspace", 3) }
        )
        BottomTabItem(
            label = "More",
            icon = Icons.Default.MoreHoriz,
            isActive = isMoreMenuActive,
            isActiveColor = BrandViolet,
            onClick = onMoreMenuClick
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
    val backgroundAlpha = if (isActive) 0.15f else 0.0f
    val backgroundColor = isActiveColor.copy(alpha = backgroundAlpha)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) isActiveColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
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

    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PROFILE & APP CONFIGURATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
            }

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
    androidx.compose.runtime.LaunchedEffect(message) {
        kotlinx.coroutines.delay(5000)
        onDismiss()
    }
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
    onImportClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var profileExpanded by remember { mutableStateOf(false) }
    val currentUser by viewModel.sessionUser.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    val selectedFolder by viewModel.selectedTaskFolder.collectAsState()
    val selectedTag by viewModel.selectedTaskTag.collectAsState()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount < -15f) {
                        onClose()
                    }
                }
            }
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
            // --- Drawer Header: Custom Modern Header with name, image, search, bell, settings ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { 
                            onNavigate("profile")
                            onClose()
                        }
                        .padding(4.dp)
                ) {
                    val user = currentUser
                    val nameLetter = user?.fullName?.trim()?.take(1)?.uppercase() ?: "U"
                    val displayName = user?.fullName ?: "Guest User"
                    val profileImageUri by viewModel.profileImageUri.collectAsState()

                    // Circular Avatar placeholder or custom profile image
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileImageUri != null) {
                            coil.compose.AsyncImage(
                                model = profileImageUri,
                                contentDescription = "Profile Image",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = nameLetter,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search icon
                    IconButton(
                        onClick = {
                            onSearchClick()
                            onClose()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Notification Bell Icon with Badge
                    val notifications by viewModel.notifications.collectAsState()
                    BadgedBox(
                        badge = {
                            if (notifications.isNotEmpty()) {
                                Badge(
                                    containerColor = BrandPink,
                                    contentColor = Color.White,
                                    modifier = Modifier.offset(x = (-2).dp, y = (2).dp)
                                ) {
                                    Text(notifications.size.toString(), fontSize = 8.sp)
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = onNotificationClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Settings icon
                    IconButton(
                        onClick = {
                            onNavigate("settings")
                            onClose()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }



            // Expanded Section: Detailed Form Link, Profile Image Actions, and Earned Badges Slider
            AnimatedVisibility(
                visible = profileExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Detailed Profile Form Link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .clickable {
                                onNavigate("settings")
                                onClose()
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = BrandViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Detailed Profile Form 📋",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Update full name, phone, medical & address details",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // 2. Profile Image Actions: Upload, Delete, Edit
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "PROFILE IMAGE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandViolet.copy(alpha = 0.8f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val profileImageUri by viewModel.profileImageUri.collectAsState()

                            // Upload / Edit Image Action
                            Button(
                                onClick = {
                                    onNavigate("profile")
                                    onClose()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (profileImageUri == null) Icons.Default.Upload else Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (profileImageUri == null) "Upload Image" else "Edit Image",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Delete Image Action
                            if (profileImageUri != null) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.setProfileImageUri(null)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Delete Image",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // 3. Earned Badges Icons Slider
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "EARNED BADGES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandViolet.copy(alpha = 0.8f)
                        )

                        val allHabits by viewModel.allHabits.collectAsState()
                        val standardBadges = listOf(
                            com.example.ui.BadgeSpec(1, "The Spark", "🥉", "The Launchpad", "Ignited the habit"),
                            com.example.ui.BadgeSpec(3, "Three's Company", "🥉", "The Launchpad", "Overcame day-two slump"),
                            com.example.ui.BadgeSpec(5, "Workweek Warrior", "🥉", "The Launchpad", "Five consecutive days"),
                            com.example.ui.BadgeSpec(7, "Weekly Wonder", "🥉", "The Launchpad", "Completed full week"),
                            com.example.ui.BadgeSpec(14, "Fortnight Force", "🥈", "The Builder", "Two weeks dedication"),
                            com.example.ui.BadgeSpec(21, "Habit Former", "🥈", "The Builder", "Avg days to lock routine"),
                            com.example.ui.BadgeSpec(30, "Calendar Crusher", "🥈", "The Builder", "One full month"),
                            com.example.ui.BadgeSpec(45, "Halfway Hero", "🥈", "The Builder", "Momentum past 1 month"),
                            com.example.ui.BadgeSpec(60, "Iron Will", "🥇", "The Master", "Two months unbroken"),
                            com.example.ui.BadgeSpec(90, "Seasoned Pro", "🥇", "The Master", "Seasonal commitment"),
                            com.example.ui.BadgeSpec(100, "Centurion", "🥇", "The Master", "Triple-digit milestone"),
                            com.example.ui.BadgeSpec(365, "Immortal", "🥇", "The Master", "One full year")
                        )
                        val earnedBadgeDays = remember(allHabits) {
                            allHabits.flatMap { habit ->
                                TrackWiseUtils.deserializeIntList(habit.badgesEarnedJson)
                            }.toSet()
                        }
                        val userEarnedBadges = remember(earnedBadgeDays) {
                            val earned = standardBadges.filter { earnedBadgeDays.contains(it.days) }
                            if (earned.isEmpty()) {
                                listOf(
                                    com.example.ui.BadgeSpec(1, "The Spark", "🥉", "The Launchpad", "Ignited the first habit"),
                                    com.example.ui.BadgeSpec(3, "Three's Company", "🥉", "The Launchpad", "Overcame day-two slump"),
                                    com.example.ui.BadgeSpec(5, "Workweek Warrior", "🥉", "The Launchpad", "Five consecutive days")
                                )
                            } else {
                                earned
                            }
                        }

                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(userEarnedBadges) { badge ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .width(85.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(text = badge.medal, fontSize = 22.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = badge.name,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = badge.tier,
                                            fontSize = 7.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                // --- Friends & Achievements Navigation Link (Moved to top) ---
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
                                Icon(Icons.Default.BarChart, contentDescription = null, tint = BrandViolet)
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

                // --- Completed Items Archive Navigation Link ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (activeTab == "archive") BrandViolet.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigate("archive")
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
                                Icon(Icons.Default.Archive, contentDescription = null, tint = BrandViolet)
                                Text(
                                    text = "COMPLETED ARCHIVE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (activeTab == "archive") BrandViolet else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = if (activeTab == "archive") BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // --- Task Folders Navigation Link ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (activeTab == "folders") BrandViolet.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigate("folders")
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
                                Icon(Icons.Default.Folder, contentDescription = null, tint = BrandViolet)
                                Text(
                                    text = "TASK & HABIT FOLDERS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (activeTab == "folders") BrandViolet else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = if (activeTab == "folders") BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // --- Hashtags Navigation Link ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (activeTab == "tags") BrandViolet.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigate("tags")
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
                                Icon(Icons.Default.LocalOffer, contentDescription = null, tint = BrandViolet)
                                Text(
                                    text = "HASHTAGS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (activeTab == "tags") BrandViolet else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = if (activeTab == "tags") BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // --- Footer Controls (Logout & How It Works) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showLogoutConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Logout", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = {
                        onNavigate("help")
                        onClose()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "How This App Works",
                        tint = BrandViolet,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Logout Account", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out of your account on this device?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        viewModel.logout()
                    }
                ) {
                    Text("Logout", color = BrandViolet, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingOverlay(
    viewModel: TrackWiseViewModel,
    currentUser: UserEntity
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val initialFirst = remember(currentUser.fullName) {
        val parts = currentUser.fullName.split(" ")
        parts.firstOrNull() ?: ""
    }
    val initialLast = remember(currentUser.fullName) {
        val parts = currentUser.fullName.split(" ")
        if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
    }

    var firstName by remember { mutableStateOf(initialFirst) }
    var lastName by remember { mutableStateOf(initialLast) }
    val email = currentUser.email
    var phone by remember { mutableStateOf(currentUser.phone ?: "") }
    var gender by remember { mutableStateOf(currentUser.gender ?: "Male") }
    var religion by remember { mutableStateOf(if (currentUser.religion.isNullOrBlank()) "Others" else currentUser.religion) }
    var dob by remember { mutableStateOf(currentUser.dob ?: "") }

    var showError by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()
    val isDarkTheme = MaterialTheme.colorScheme.background.let { (it.red + it.green + it.blue) / 3f < 0.5f }
    val themeId = if (isDarkTheme) android.R.style.Theme_DeviceDefault_Dialog_Alert else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert

    val datePickerDialog = remember(calendar, themeId) {
        android.app.DatePickerDialog(
            context,
            themeId,
            { _, year, month, dayOfMonth ->
                val monthStr = String.format("%02d", month + 1)
                val dayStr = String.format("%02d", dayOfMonth)
                dob = "$year-$monthStr-$dayStr"
            },
            calendar.get(Calendar.YEAR) - 20,
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    var genderDropdownExpanded by remember { mutableStateOf(false) }
    var religionDropdownExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
                .border(1.dp, BrandViolet.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            val scrollState = rememberScrollState()
            LaunchedEffect(scrollState.isScrollInProgress) {
                if (scrollState.isScrollInProgress) {
                    focusManager.clearFocus()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Welcome to TrackWise! 🌟",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandViolet,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Let's personalize your experience. Please fill out your details.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (showError && validationError.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                text = validationError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BrandViolet) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BrandViolet) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {},
                    label = { Text("Email (Signed up with)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    readOnly = true,
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number *") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BrandViolet) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                ) {
                    OutlinedTextField(
                        value = dob,
                        onValueChange = {},
                        label = { Text("Date of Birth (YYYY-MM-DD) *") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BrandViolet) },
                        readOnly = true,
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() }
                    )
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = gender,
                        onValueChange = {},
                        label = { Text("Gender *") },
                        leadingIcon = { Icon(Icons.Default.Face, contentDescription = null, tint = BrandViolet) },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandViolet)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandViolet,
                            focusedLabelColor = BrandViolet
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { genderDropdownExpanded = !genderDropdownExpanded }
                    )
                    DropdownMenu(
                        expanded = genderDropdownExpanded,
                        onDismissRequest = { genderDropdownExpanded = false },
                        modifier = Modifier
                            .widthIn(min = 180.dp, max = 280.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, BrandViolet.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        listOf("Male", "Female", "Prefer not to say").forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g) },
                                onClick = {
                                    gender = g
                                    genderDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = religion,
                        onValueChange = {},
                        label = { Text("Religion *") },
                        leadingIcon = { Icon(Icons.Default.Book, contentDescription = null, tint = BrandViolet) },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandViolet)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandViolet,
                            focusedLabelColor = BrandViolet
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { religionDropdownExpanded = !religionDropdownExpanded }
                    )
                    DropdownMenu(
                        expanded = religionDropdownExpanded,
                        onDismissRequest = { religionDropdownExpanded = false },
                        modifier = Modifier
                            .widthIn(min = 180.dp, max = 280.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, BrandViolet.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        listOf("Islam", "Hindu", "Christian", "Sikh", "Others").forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    religion = r
                                    religionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (firstName.isBlank() || lastName.isBlank()) {
                            validationError = "First and Last Name are required."
                            showError = true
                        } else if (phone.isBlank() || phone.length < 10) {
                            validationError = "Please enter a valid phone number (at least 10 digits)."
                            showError = true
                        } else if (dob.isBlank()) {
                            validationError = "Please select your Date of Birth."
                            showError = true
                        } else {
                            viewModel.completeOnboarding(
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                phone = phone.trim(),
                                gender = gender,
                                religion = religion,
                                dob = dob
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("COMPLETE PROFILE SETUP", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class SearchResult(
    val title: String,
    val subtitle: String,
    val type: String, // "Task", "Habit", "Wishlist", "Occasion", "Alarm", "Grocery", "Finance", "Friend", "Medicine", "Health Issue"
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: androidx.compose.ui.graphics.Color,
    val onClick: () -> Unit
)

@Composable
fun GlobalSearchDialog(
    viewModel: TrackWiseViewModel,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val tasks by viewModel.allTasks.collectAsState()
    val habits by viewModel.allHabits.collectAsState()
    val birthdays by viewModel.allBirthdays.collectAsState()
    val wishlist by viewModel.allWishlist.collectAsState()
    val groceryItems by viewModel.allGroceryItems.collectAsState()
    val financeLogs by viewModel.allFinanceLogs.collectAsState()
    val alarms by viewModel.allAlarms.collectAsState()
    val friends by viewModel.friendConnections.collectAsState()
    val tabletReminders by viewModel.tabletReminders.collectAsState()
    val healthIssues by viewModel.healthIssueLogs.collectAsState()

    val filteredResults = remember(
        searchQuery, tasks, habits, birthdays, wishlist, groceryItems, financeLogs, alarms, friends, tabletReminders, healthIssues
    ) {
        if (searchQuery.trim().isEmpty()) {
            emptyList<SearchResult>()
        } else {
            val query = searchQuery.trim().lowercase()
            val results = mutableListOf<SearchResult>()

            // 1. Tasks
            tasks.filter {
                it.title.lowercase().contains(query) ||
                it.description.lowercase().contains(query) ||
                it.project.lowercase().contains(query) ||
                it.notes.lowercase().contains(query)
            }.forEach { task ->
                results.add(
                    SearchResult(
                        title = task.title,
                        subtitle = "Project: ${task.project} • Priority: ${task.priority.uppercase()}",
                        type = "Task",
                        icon = Icons.Default.Assignment,
                        color = BrandViolet,
                        onClick = {
                            viewModel.setWorkspaceSubTab(0)
                            onNavigate("workspace")
                            onDismiss()
                        }
                    )
                )
            }

            // 2. Habits
            habits.filter {
                it.name.lowercase().contains(query) ||
                it.category.lowercase().contains(query) ||
                it.notes.lowercase().contains(query)
            }.forEach { habit ->
                results.add(
                    SearchResult(
                        title = habit.name,
                        subtitle = "Category: ${habit.category} • Streak: ${habit.streak} days",
                        type = "Habit",
                        icon = Icons.Default.Repeat,
                        color = BrandPink,
                        onClick = {
                            viewModel.setWorkspaceSubTab(1)
                            onNavigate("workspace")
                            onDismiss()
                        }
                    )
                )
            }

            // 3. Wishlist
            wishlist.filter {
                it.title.lowercase().contains(query) ||
                it.priority.lowercase().contains(query) ||
                (it.link?.lowercase()?.contains(query) == true)
            }.forEach { item ->
                results.add(
                    SearchResult(
                        title = item.title,
                        subtitle = "Price: $${item.price} • Priority: ${item.priority.uppercase()}",
                        type = "Wishlist Item",
                        icon = Icons.Default.Star,
                        color = BrandCyan,
                        onClick = {
                            viewModel.setWorkspaceSubTab(2)
                            onNavigate("workspace")
                            onDismiss()
                        }
                    )
                )
            }

            // 4. Occasions / Birthdays
            birthdays.filter {
                it.name.lowercase().contains(query) ||
                it.category.lowercase().contains(query) ||
                (it.giftIdea?.lowercase()?.contains(query) == true)
            }.forEach { birthday ->
                results.add(
                    SearchResult(
                        title = birthday.name,
                        subtitle = "Date: ${birthday.date} • ${birthday.category}",
                        type = "Occasion",
                        icon = Icons.Default.HourglassEmpty,
                        color = BrandOrange,
                        onClick = {
                            viewModel.setWorkspaceSubTab(3)
                            onNavigate("workspace")
                            onDismiss()
                        }
                    )
                )
            }

            // 5. Alarms / Timers
            alarms.filter {
                it.label.lowercase().contains(query)
            }.forEach { alarm ->
                val timeStr = String.format("%02d:%02d", alarm.hour, alarm.minute)
                results.add(
                    SearchResult(
                        title = alarm.label.ifBlank { "Alarm" },
                        subtitle = "Scheduled: $timeStr • Enabled: ${alarm.isEnabled}",
                        type = "Timer / Alarm",
                        icon = Icons.Default.Alarm,
                        color = BrandIndigo,
                        onClick = {
                            viewModel.setWorkspaceSubTab(4)
                            onNavigate("workspace")
                            onDismiss()
                        }
                    )
                )
            }

            // 6. Grocery List
            groceryItems.filter {
                it.name.lowercase().contains(query) ||
                it.category.lowercase().contains(query)
            }.forEach { grocery ->
                results.add(
                    SearchResult(
                        title = grocery.name,
                        subtitle = "Category: ${grocery.category} • Quantity: ${grocery.quantity}",
                        type = "Grocery Item",
                        icon = Icons.Default.ShoppingCart,
                        color = BrandGreen,
                        onClick = {
                            viewModel.setWorkspaceSubTab(5)
                            onNavigate("workspace")
                            onDismiss()
                        }
                    )
                )
            }

            // 7. Finance Logs
            financeLogs.filter {
                it.title.lowercase().contains(query) ||
                it.category.lowercase().contains(query)
            }.forEach { log ->
                val amtPrefix = if (log.type == "expense") "-" else "+"
                results.add(
                    SearchResult(
                        title = log.title,
                        subtitle = "Category: ${log.category} • Amount: $amtPrefix$${log.amount}",
                        type = "Finance Log",
                        icon = Icons.Default.AttachMoney,
                        color = BrandViolet,
                        onClick = {
                            onNavigate("finance")
                            onDismiss()
                        }
                    )
                )
            }

            // 8. Friends
            friends.filter {
                it.displayName.lowercase().contains(query)
            }.forEach { friend ->
                results.add(
                    SearchResult(
                        title = friend.displayName,
                        subtitle = "Friend Connection since ${friend.addedAt}",
                        type = "Friend",
                        icon = Icons.Default.Person,
                        color = BrandPink,
                        onClick = {
                            viewModel.setSocialSubTab("friends")
                            onNavigate("social")
                            onDismiss()
                        }
                    )
                )
            }

            // 9. Medicines / Tablet Reminders
            tabletReminders.filter {
                it.tabletName.lowercase().contains(query) ||
                (it.notes?.lowercase()?.contains(query) == true)
            }.forEach { tab ->
                results.add(
                    SearchResult(
                        title = tab.tabletName,
                        subtitle = "Dosage: ${tab.dosage} • Time: ${tab.timeOfDay}",
                        type = "Medicine",
                        icon = Icons.Default.Favorite,
                        color = BrandViolet,
                        onClick = {
                            viewModel.setHealthSubTab(1)
                            onNavigate("health")
                            onDismiss()
                        }
                    )
                )
            }

            // 10. Health Issues
            healthIssues.filter {
                it.issueName.lowercase().contains(query) ||
                (it.notes?.lowercase()?.contains(query) == true)
            }.forEach { issue ->
                results.add(
                    SearchResult(
                        title = issue.issueName,
                        subtitle = "Severity: ${issue.severity.uppercase()} • Date: ${issue.date}",
                        type = "Health Issue",
                        icon = Icons.Default.Info,
                        color = BrandRose,
                        onClick = {
                            viewModel.setHealthSubTab(0)
                            onNavigate("health")
                            onDismiss()
                        }
                    )
                )
            }

            results
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                // Top Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search icon",
                                tint = BrandViolet
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandViolet,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Close Button
                    TextButton(onClick = onDismiss) {
                        Text(
                            "Cancel",
                            color = BrandViolet,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Results list
                if (searchQuery.trim().isEmpty()) {
                    // Empty/Idle State
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = BrandViolet.copy(alpha = 0.25f),
                                modifier = Modifier.size(72.dp)
                            )
                            Text(
                                text = "Search literally anything",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Type to search tasks, habits, wishlist items, grocery list, alarms, medicine, health issues, expenses, friends, etc.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else if (filteredResults.isEmpty()) {
                    // No results state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "No results found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "We couldn't find anything matching \"$searchQuery\"",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Results list
                    Text(
                        text = "FOUND ${filteredResults.size} MATCHING ITEMS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandViolet,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredResults) { result ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { result.onClick() }
                                    .border(
                                        width = 1.dp,
                                        color = result.color.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Custom Circular Icon with background tint
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(result.color.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = result.icon,
                                            contentDescription = null,
                                            tint = result.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = result.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = result.subtitle,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }

                                    // Type Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(result.color.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = result.type.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = result.color,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
