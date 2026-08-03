package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import java.util.Locale
import com.example.data.*
import com.example.ui.theme.*
import com.example.utils.TrackWiseUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun AnimatedTileContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 12.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = alpha, translationY = offsetY.value)
    ) {
        content()
    }
}

@Composable
fun AnalyticsScreen(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.allHabits.collectAsState()
    val badHabits by viewModel.badHabits.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    val weightLogs by viewModel.weightEntries.collectAsState()
    val vitals by viewModel.vitalReadings.collectAsState()
    val waterLogs by viewModel.waterLogs.collectAsState()
    val exerciseLogs by viewModel.exerciseLogs.collectAsState()
    val sleepLogs by viewModel.sleepLogs.collectAsState()
    val healthIssues by viewModel.healthIssueLogs.collectAsState()
    val financeLogs by viewModel.allFinanceLogs.collectAsState()
    val netWorthItems by viewModel.allNetWorthItems.collectAsState()
    val wishlist by viewModel.allWishlist.collectAsState()
    val groceries by viewModel.allGroceryItems.collectAsState()
    val currentUser by viewModel.sessionUser.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val periodCycles by viewModel.periodCycles.collectAsState()

    val isFemale = remember(currentUser, userProfile) {
        val g = (userProfile?.gender ?: currentUser?.gender ?: "").lowercase().trim()
        g == "female" || g == "woman" || g == "women" || g == "girl"
    }

    // Modern 7-tab Scrollable Layout
    val categories = listOf("Finance", "Task", "Habit", "Habit Breaker", "Health", "Wishlist", "Grocery List")
    val pagerState = rememberPagerState(pageCount = { categories.size })
    val coroutineScope = rememberCoroutineScope()

    // Target Year and Month for Financial Analytics
    var analyticsYear by remember { mutableStateOf(2026) }
    var analyticsMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) } // 1-12

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // --- Premium Scrollable Tabs with dynamic branding colors ---
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = when (categories[pagerState.currentPage]) {
                            "Finance" -> BrandGreen
                            "Task" -> BrandViolet
                            "Habit" -> BrandCyan
                            "Habit Breaker" -> BrandRose
                            "Health" -> Color(0xFFE11D48)
                            "Wishlist" -> BrandOrange
                            else -> BrandGreen
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            categories.forEachIndexed { index, category ->
                val isSelected = pagerState.currentPage == index
                val categoryColor = when (category) {
                    "Finance" -> BrandGreen
                    "Task" -> BrandViolet
                    "Habit" -> BrandCyan
                    "Habit Breaker" -> BrandRose
                    "Health" -> Color(0xFFE11D48)
                    "Wishlist" -> BrandOrange
                    else -> BrandGreen
                }
                val icon = when (category) {
                    "Finance" -> Icons.Default.AttachMoney
                    "Task" -> Icons.Default.CheckCircle
                    "Habit" -> Icons.Default.Autorenew
                    "Habit Breaker" -> Icons.Default.Block
                    "Health" -> Icons.Default.Favorite
                    "Wishlist" -> Icons.Default.Star
                    else -> Icons.Default.ShoppingCart
                }
                Tab(
                    selected = isSelected,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = category,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    selectedContentColor = categoryColor,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        // Swipable Horizontal Pager content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val currentCategory = categories[page]
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentCategory) {
                    "Finance" -> {
                        item {
                            AnimatedTileContainer {
                                var showMonthMenu by remember { mutableStateOf(false) }
                                var showYearMenu by remember { mutableStateOf(false) }
                                val monthNames = listOf(
                                    "January", "February", "March", "April", "May", "June",
                                    "July", "August", "September", "October", "November", "December"
                                )

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "TARGET PERIOD",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                text = "Filter Financial Insights",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Month Menu
                                            Box {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(BrandViolet.copy(alpha = 0.12f))
                                                        .clickable { showMonthMenu = !showMonthMenu }
                                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = monthNames.getOrElse(analyticsMonth - 1) { "Select Month" }.take(3),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = BrandViolet
                                                        )
                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                                DropdownMenu(
                                                    expanded = showMonthMenu,
                                                    onDismissRequest = { showMonthMenu = false },
                                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                                ) {
                                                    monthNames.forEachIndexed { idx, name ->
                                                        DropdownMenuItem(
                                                            text = { Text(name, fontSize = 12.sp) },
                                                            onClick = {
                                                                analyticsMonth = idx + 1
                                                                showMonthMenu = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            // Year Menu
                                            Box {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(BrandGreen.copy(alpha = 0.12f))
                                                        .clickable { showYearMenu = !showYearMenu }
                                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "$analyticsYear",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = BrandGreen
                                                        )
                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                                DropdownMenu(
                                                    expanded = showYearMenu,
                                                    onDismissRequest = { showYearMenu = false },
                                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                                ) {
                                                    listOf(2025, 2026, 2027, 2028).forEach { yr ->
                                                        DropdownMenuItem(
                                                            text = { Text("$yr", fontSize = 12.sp) },
                                                            onClick = {
                                                                analyticsYear = yr
                                                                showYearMenu = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Chart 1: Net Worth Pie Chart Card
                        item {
                            AnimatedTileContainer {
                                NetWorthPieChartCard(netWorthItems = netWorthItems)
                            }
                        }

                        // Chart 2: Top Spent Days Card
                        item {
                            AnimatedTileContainer {
                                TopSpentDaysCard(
                                    financeLogs = financeLogs,
                                    selectedYear = analyticsYear,
                                    selectedMonth = analyticsMonth
                                )
                            }
                        }

                        // Chart 3: Expense vs Savings Pie Chart Card
                        item {
                            AnimatedTileContainer {
                                ExpenseSavingsPieChartCard(
                                    financeLogs = financeLogs,
                                    selectedYear = analyticsYear,
                                    selectedMonth = analyticsMonth
                                )
                            }
                        }

                        // Chart 4: Category-wise Expense Distribution Card
                        item {
                            AnimatedTileContainer {
                                FinanceExpenseDistributionCard(financeLogs = financeLogs)
                            }
                        }

                        // Chart 5: Category-wise Savings Distribution Card
                        item {
                            AnimatedTileContainer {
                                FinanceSavingsDistributionCard(financeLogs = financeLogs)
                            }
                        }
                    }

                    "Task" -> {
                        // Chart 1: Task Folder Progress Card
                        item {
                            AnimatedTileContainer {
                                FolderProgressCard(tasks = tasks, habits = emptyList())
                            }
                        }

                        // Chart 2: Task Priority Distribution Chart
                        item {
                            AnimatedTileContainer {
                                TaskPriorityDistributionCard(tasks = tasks)
                            }
                        }

                        // Chart 3: Task Completion Frequency Tracker
                        item {
                            AnimatedTileContainer {
                                CompletionsTrackerCard(tasks = tasks, habits = emptyList())
                            }
                        }

                        // Chart 4: Overdue Tasks Tracker
                        item {
                            AnimatedTileContainer {
                                OverdueTasksCard(tasks = tasks)
                            }
                        }

                        // Chart 5: Task Project XP Contribution Breakdown
                        item {
                            AnimatedTileContainer {
                                TaskXPBreakdownCard(tasks = tasks)
                            }
                        }
                    }

                    "Habit" -> {
                        // Chart 1: Habit Longest Streaks Card
                        item {
                            AnimatedTileContainer {
                                HabitStreakCard(habits = habits)
                            }
                        }

                        // Chart 2: Habit Category Distribution Card
                        item {
                            AnimatedTileContainer {
                                HabitCategoryDistributionCard(habits = habits)
                            }
                        }

                        // Chart 3: Habit Completion Weekly Tracker
                        item {
                            AnimatedTileContainer {
                                CompletionsTrackerCard(tasks = emptyList(), habits = habits)
                            }
                        }

                        // Chart 4: Habit Completion Rate Progress Metrics
                        item {
                            AnimatedTileContainer {
                                HabitCompletionRateCard(habits = habits)
                            }
                        }

                        // Chart 5: Habit Streak Milestones Achievement Tracker
                        item {
                            AnimatedTileContainer {
                                HabitMilestoneAchievementsCard(habits = habits)
                            }
                        }
                    }

                    "Habit Breaker" -> {
                        // Chart 1: Slip-ups Monitor Card
                        item {
                            AnimatedTileContainer {
                                BadHabitsAnalyticsCard(badHabits = badHabits)
                            }
                        }

                        // Chart 2: Sobriety/Clean Duration Bar Chart
                        item {
                            AnimatedTileContainer {
                                SobrietyCleanStreaksCard(badHabits = badHabits)
                            }
                        }

                        // Chart 3: Avoidance Frequency & Success rate
                        item {
                            AnimatedTileContainer {
                                HabitBreakerResistedUrgesCard(badHabits = badHabits)
                            }
                        }

                        // Chart 4: Cost/Time Savings Calculator
                        item {
                            AnimatedTileContainer {
                                HabitBreakerCostSavingsCard(badHabits = badHabits)
                            }
                        }

                        // Chart 5: Peak Slip-up Hours & Day-of-Week Distribution
                        item {
                            AnimatedTileContainer {
                                HabitBreakerWeekdayTriggersCard(badHabits = badHabits)
                            }
                        }
                    }

                    "Health" -> {
                        // Chart 1: Average Weight Chart
                        item {
                            AnimatedTileContainer {
                                AverageWeightCard(weightLogs = weightLogs, defaultWeight = currentUser?.weightKg ?: 70.0)
                            }
                        }

                        // Chart 2: Least Hydrated Days Chart
                        item {
                            AnimatedTileContainer {
                                LeastHydrationCard(waterLogs = waterLogs)
                            }
                        }

                        // Chart 3: Exercise Intensity Chart
                        item {
                            AnimatedTileContainer {
                                ExerciseIntensityCard(exerciseLogs = exerciseLogs)
                            }
                        }

                        // Chart 4: Vitals History (Blood Sugar and BP)
                        item {
                            AnimatedTileContainer {
                                VitalsHistoryCard(vitals = vitals)
                            }
                        }

                        // Chart 5: Sleep Quality Chart
                        item {
                            AnimatedTileContainer {
                                SleepHistoryCard(sleepLogs = sleepLogs)
                            }
                        }

                        // Additional premium female health charts
                        if (isFemale && periodCycles.isNotEmpty()) {
                            item {
                                AnimatedTileContainer {
                                    HormonalPhaseOverlayCard(periodCycles = periodCycles)
                                }
                            }
                            item {
                                AnimatedTileContainer {
                                    PeriodSymptomPeakChartCard(periodCycles = periodCycles)
                                }
                            }
                        }
                    }

                    "Wishlist" -> {
                        // Chart 1: Wishlist Items Status (Purchased vs Pending)
                        item {
                            AnimatedTileContainer {
                                WishlistStatusCard(wishlist = wishlist)
                            }
                        }

                        // Chart 2: Price Tier Distribution Chart
                        item {
                            AnimatedTileContainer {
                                WishlistPriceTiersCard(wishlist = wishlist)
                            }
                        }

                        // Chart 3: Savings Target Progress Bar Chart
                        item {
                            AnimatedTileContainer {
                                WishlistSavingsProgressCard(wishlist = wishlist)
                            }
                        }

                        // Chart 4: Category Allocation Distribution Pie Chart
                        item {
                            AnimatedTileContainer {
                                WishlistCategoriesCard(wishlist = wishlist)
                            }
                        }

                        // Chart 5: Purchase Urgency vs Affordability Priority Quadrant Card
                        item {
                            AnimatedTileContainer {
                                WishlistPriorityQuadrantCard(wishlist = wishlist)
                            }
                        }
                    }

                    "Grocery List" -> {
                        // Chart 1: Grocery Categories Distribution
                        item {
                            AnimatedTileContainer {
                                GroceryCategoryDistributionCard(groceries = groceries)
                            }
                        }

                        // Chart 2: Purchase Completion Progress Ring
                        item {
                            AnimatedTileContainer {
                                GroceryCompletionRingCard(groceries = groceries)
                            }
                        }

                        // Chart 3: Estimated Price / Cost Distribution Trend
                        item {
                            AnimatedTileContainer {
                                GroceryPriceDistributionCard(groceries = groceries)
                            }
                        }

                        // Chart 4: Frequent Item purchasing frequency chart
                        item {
                            AnimatedTileContainer {
                                GroceryFrequentItemsCard(groceries = groceries)
                            }
                        }

                        // Chart 5: Grocery Monthly Budget Pacing Tracker
                        item {
                            AnimatedTileContainer {
                                GroceryBudgetPacingCard(groceries = groceries)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun oldAnalyticsScreenStub(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.allHabits.collectAsState()
    val badHabits by viewModel.badHabits.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    val weightLogs by viewModel.weightEntries.collectAsState()
    val vitals by viewModel.vitalReadings.collectAsState()
    val waterLogs by viewModel.waterLogs.collectAsState()
    val exerciseLogs by viewModel.exerciseLogs.collectAsState()
    val sleepLogs by viewModel.sleepLogs.collectAsState()
    val healthIssues by viewModel.healthIssueLogs.collectAsState()
    val financeLogs by viewModel.allFinanceLogs.collectAsState()
    val netWorthItems by viewModel.allNetWorthItems.collectAsState()
    val currentUser by viewModel.sessionUser.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val periodCycles by viewModel.periodCycles.collectAsState()

    val isFemale = remember(currentUser, userProfile) {
        val g = (userProfile?.gender ?: currentUser?.gender ?: "").lowercase().trim()
        g == "female" || g == "woman" || g == "women" || g == "girl"
    }

    // Interactive category selector
    val categories = listOf("Finance Tracker", "Habits & Tasks", "Health & Fitness")
    var selectedCategory by remember { mutableStateOf("Finance Tracker") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Target Year and Month for Financial Analytics
    var analyticsYear by remember { mutableStateOf(2026) }
    var analyticsMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) } // 1-12

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {


        // --- Category Selection Tabs ---
        item {
            val selectedIndex = categories.indexOf(selectedCategory).coerceAtLeast(0)
            TabRow(
                selectedTabIndex = selectedIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (selectedIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                            color = when (selectedCategory) {
                                "Finance Tracker" -> BrandGreen
                                "Habits & Tasks" -> BrandViolet
                                else -> BrandRose
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                categories.forEachIndexed { index, category ->
                    val isSelected = selectedIndex == index
                    val categoryColor = when (category) {
                        "Finance Tracker" -> BrandGreen
                        "Habits & Tasks" -> BrandViolet
                        else -> BrandRose
                    }
                    val icon = when (category) {
                        "Finance Tracker" -> Icons.Default.AttachMoney
                        "Habits & Tasks" -> Icons.Default.CheckCircle
                        else -> Icons.Default.Favorite
                    }
                    Tab(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        text = {
                            Text(
                                text = category,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        selectedContentColor = categoryColor,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // --- Conditional Section Rendering ---
        when (selectedCategory) {
            "Finance Tracker" -> {
                item {
                    var showMonthMenu by remember { mutableStateOf(false) }
                    var showYearMenu by remember { mutableStateOf(false) }
                    val monthNames = listOf(
                        "January", "February", "March", "April", "May", "June",
                        "July", "August", "September", "October", "November", "December"
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TARGET PERIOD",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "Filter Financial Insights",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Month Dropdown
                                Box {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BrandViolet.copy(alpha = 0.12f))
                                            .clickable { showMonthMenu = !showMonthMenu }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = monthNames.getOrElse(analyticsMonth - 1) { "Select Month" },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandViolet
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    
                                    DropdownMenu(
                                        expanded = showMonthMenu,
                                        onDismissRequest = { showMonthMenu = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        monthNames.forEachIndexed { idx, name ->
                                            DropdownMenuItem(
                                                text = { Text(name, fontSize = 12.sp) },
                                                onClick = {
                                                    analyticsMonth = idx + 1
                                                    showMonthMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Year Dropdown
                                Box {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BrandGreen.copy(alpha = 0.12f))
                                            .clickable { showYearMenu = !showYearMenu }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "$analyticsYear",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandGreen
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    
                                    DropdownMenu(
                                        expanded = showYearMenu,
                                        onDismissRequest = { showYearMenu = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        listOf(2025, 2026, 2027, 2028).forEach { yr ->
                                            DropdownMenuItem(
                                                text = { Text("$yr", fontSize = 12.sp) },
                                                onClick = {
                                                    analyticsYear = yr
                                                    showYearMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    NetWorthPieChartCard(
                        netWorthItems = netWorthItems
                    )
                }
                item {
                    ExpenseSavingsPieChartCard(
                        financeLogs = financeLogs,
                        selectedYear = analyticsYear,
                        selectedMonth = analyticsMonth
                    )
                }
                item {
                    IncomeDistributionPieChartCard(
                        financeLogs = financeLogs,
                        selectedYear = analyticsYear,
                        selectedMonth = analyticsMonth
                    )
                }
                item {
                    TopSpentDaysCard(
                        financeLogs = financeLogs,
                        selectedYear = analyticsYear,
                        selectedMonth = analyticsMonth
                    )
                }
                item {
                    FinanceOverviewAnalyticsCard(financeLogs = financeLogs)
                }
                item {
                    FinanceExpenseDistributionCard(financeLogs = financeLogs)
                }
                item {
                    FinanceSavingsDistributionCard(financeLogs = financeLogs)
                }
                item {
                    FinanceDailyTrendsCard(financeLogs = financeLogs)
                }
            }
            "Habits & Tasks" -> {
                // --- 0. Folder Progress Card ---
                item {
                    FolderProgressCard(tasks = tasks, habits = habits)
                }

                // --- 0.1 Habit Category Distribution Chart ---
                item {
                    HabitCategoryDistributionCard(habits = habits)
                }

                // --- 0.2 Tasks Priority & Completion Timeline Chart ---
                item {
                    TaskPriorityDistributionCard(tasks = tasks)
                }

                // --- 1. Habit Streak Chart (Top 5) ---
                item {
                    HabitStreakCard(habits = habits)
                }

                // --- 2. Completed Tasks + Habits Day of Week / Month / Year Chart ---
                item {
                    CompletionsTrackerCard(tasks = tasks, habits = habits)
                }

                // --- 3. Overdued Tasks Chart (Top 5) ---
                item {
                    OverdueTasksCard(tasks = tasks)
                }

                // --- 3.1 Bad Habits Monitor Card (Demotivator & Frequency Tracker) ---
                item {
                    BadHabitsAnalyticsCard(badHabits = badHabits)
                }
            }
            "Health & Fitness" -> {
                // --- 4. Average Weight Chart (Previous 5 Months) ---
                item {
                    AverageWeightCard(weightLogs = weightLogs, defaultWeight = currentUser?.weightKg ?: 70.0)
                }

                // --- 5. Least Hydrated Days Chart (Toggle Month/Year) ---
                item {
                    LeastHydrationCard(waterLogs = waterLogs)
                }

                // --- 6. Exercise Intensity Chart (Most / Least Toggle) ---
                item {
                    ExerciseIntensityCard(exerciseLogs = exerciseLogs)
                }

                // --- 7. Vitals Charts (Blood Sugar and Blood Pressure Separated) ---
                item {
                    VitalsHistoryCard(vitals = vitals)
                }

                // --- 8. Sleep Quality Chart (Most / Least Toggle, Month/Year Toggle) ---
                item {
                    SleepHistoryCard(sleepLogs = sleepLogs)
                }

                // --- 9. Symptom Tracker Chart (Date vs Symptom stacked timeline with Mild/Mod/Severe Toggle) ---
                item {
                    SymptomTimelineCard(healthIssues = healthIssues)
                }

                // --- 10. Period Analytics (Hormonal Phase Overlay and Symptom Peak Chart) ---
                if (isFemale) {
                    item {
                        HormonalPhaseOverlayCard(periodCycles = periodCycles)
                    }
                    item {
                        PeriodSymptomPeakChartCard(periodCycles = periodCycles)
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. Habit Streak Chart
// ==========================================
@Composable
fun HabitStreakCard(habits: List<HabitEntity>) {
    // Sort habits by streak descending, take top 5
    val topHabits = remember(habits) {
        habits.sortedByDescending { it.streak }.take(5)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(20.dp))
                Text("HABIT STREAK LEADERBOARD", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
            }

            if (topHabits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No habits found. Create and check-in habits to see streaks!", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                val maxStreak = remember(topHabits) { max(1, topHabits.maxOfOrNull { it.streak } ?: 1) }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    topHabits.forEach { habit ->
                        val ratio = habit.streak.toFloat() / maxStreak.toFloat()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(habit.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("🔥 ${habit.streak} days", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
                            }
                            // Custom progress bar representation
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Brush.horizontalGradient(listOf(BrandViolet, BrandPink)))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. Completed Tasks + Habits Day of Week / Month / Year Chart
// ==========================================
@Composable
fun CompletionsTrackerCard(tasks: List<TaskEntity>, habits: List<HabitEntity>) {
    var period by remember { mutableStateOf("week") } // "week", "month", "year"

    // Parse completions
    val completionDates = remember(tasks, habits) {
        val list = mutableListOf<String>()
        // Completed tasks
        tasks.filter { it.completed }.forEach { list.add(it.deadline) }
        // Completed habits
        habits.forEach { habit ->
            val dates = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson)
            list.addAll(dates)
        }
        list.filter { it.isNotBlank() }
    }

    // Process completions based on period
    val chartData = remember(completionDates, period) {
        when (period) {
            "week" -> {
                // Calculate by day of week
                val counts = IntArray(7) // Mon to Sun
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val cal = Calendar.getInstance()
                completionDates.forEach { dateStr ->
                    try {
                        val d = sdf.parse(dateStr)
                        if (d != null) {
                            cal.time = d
                            // Calendar.DAY_OF_WEEK: Sun=1, Mon=2, ..., Sat=7
                            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                            val index = when (dayOfWeek) {
                                Calendar.MONDAY -> 0
                                Calendar.TUESDAY -> 1
                                Calendar.WEDNESDAY -> 2
                                Calendar.THURSDAY -> 3
                                Calendar.FRIDAY -> 4
                                Calendar.SATURDAY -> 5
                                Calendar.SUNDAY -> 6
                                else -> 0
                            }
                            counts[index]++
                        }
                    } catch (e: Exception) {}
                }
                listOf(
                    "Mon" to counts[0],
                    "Tue" to counts[1],
                    "Wed" to counts[2],
                    "Thu" to counts[3],
                    "Fri" to counts[4],
                    "Sat" to counts[5],
                    "Sun" to counts[6]
                )
            }
            "month" -> {
                // Count completions per day of month (YYYY-MM-DD), filter this month (e.g. 2026-07)
                // We'll extract only the Day number (e.g. "01", "15", etc.)
                val counts = mutableMapOf<String, Int>()
                completionDates.forEach { dateStr ->
                    // Just group by day of month e.g. "Jul 01"
                    if (dateStr.length == 10) {
                        val parts = dateStr.split("-")
                        if (parts.size == 3) {
                            val label = "${parts[1]}/${parts[2]}" // MM/DD
                            counts[label] = (counts[label] ?: 0) + 1
                        }
                    }
                }
                counts.toList().sortedByDescending { it.second }.take(5)
            }
            else -> {
                // Year View: top 5 days of year
                val counts = mutableMapOf<String, Int>()
                completionDates.forEach { dateStr ->
                    if (dateStr.length == 10) {
                        val parts = dateStr.split("-")
                        if (parts.size == 3) {
                            val label = "${parts[1]}/${parts[2]}" // MM/DD
                            counts[label] = (counts[label] ?: 0) + 1
                        }
                    }
                }
                counts.toList().sortedByDescending { it.second }.take(5)
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                    Text("COMPLETED TASKS + HABITS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                }

                // Small Row of Period Toggles
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    listOf("week" to "Wk", "month" to "Mo", "year" to "Yr").forEach { (id, label) ->
                        val isSelected = period == id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) BrandGreen else Color.Transparent)
                                .clickable { period = id }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (chartData.isEmpty() || chartData.all { it.second == 0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No completions logged for this view.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                val maxVal = max(1, chartData.maxOfOrNull { it.second } ?: 1)

                if (period == "week") {
                    // Vertical bar chart for weekdays
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        chartData.forEach { (label, count) ->
                            val ratio = count.toFloat() / maxVal.toFloat()
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("$count", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height(80.dp * ratio.coerceIn(0.08f, 1f))
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(Brush.verticalGradient(listOf(BrandGreen, BrandGreen.copy(alpha = 0.4f))))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                } else {
                    // Horizontal bar chart for Top 5 Days of month or year
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (period == "month") "Top 5 Days (Current Month)" else "Top 5 Days (Yearly View)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        chartData.forEach { (label, count) ->
                            val ratio = count.toFloat() / maxVal.toFloat()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(55.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(ratio)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(Brush.horizontalGradient(listOf(BrandGreen, BrandCyan)))
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$count done",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. Overdued Tasks Chart
// ==========================================
@Composable
fun OverdueTasksCard(tasks: List<TaskEntity>) {
    val topOverdue = remember(tasks) {
        val todayStr = TrackWiseUtils.getTodayString()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = try { sdf.parse(todayStr) ?: Date() } catch (e: Exception) { Date() }

        tasks.filter { !it.completed }
            .mapNotNull { task ->
                try {
                    val deadlineDate = sdf.parse(task.deadline)
                    if (deadlineDate != null && deadlineDate.before(today)) {
                        val diffMs = today.time - deadlineDate.time
                        val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
                        if (diffDays > 0) {
                            task to diffDays
                        } else null
                    } else null
                } catch (e: Exception) { null }
            }
            .sortedByDescending { it.second }
            .take(5)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = BrandRose, modifier = Modifier.size(20.dp))
                Text("MOST OVERDUE TASKS (TOP 5)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandRose)
            }

            if (topOverdue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Perfect! No overdue tasks currently.", fontSize = 12.sp, color = BrandGreen, fontWeight = FontWeight.Bold)
                }
            } else {
                val maxOverdue = topOverdue.maxOfOrNull { it.second } ?: 1

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    topOverdue.forEach { (task, days) ->
                        val ratio = days.toFloat() / maxOverdue.toFloat()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = task.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$days days overdue",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandRose
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Brush.horizontalGradient(listOf(BrandRose, BrandOrange)))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. Average Weight Chart (Previous 5 Months)
// ==========================================
@Composable
fun AverageWeightCard(weightLogs: List<WeightEntryEntity>, defaultWeight: Double) {
    // We want the average weight in previous 5 months (Jul, Jun, May, Apr, Mar 2026)
    val monthlyAverages = remember(weightLogs, defaultWeight) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        
        // Let's establish the 5 months labels
        // Index 0: current month, 1: -1 month, ..., 4: -4 months
        val labels = mutableListOf<String>()
        val yearMonths = mutableListOf<String>() // format: "YYYY-MM"
        val sums = DoubleArray(5)
        val counts = IntArray(5)

        for (i in 0..4) {
            val tempCal = Calendar.getInstance()
            tempCal.add(Calendar.MONTH, -i)
            val monthLabel = tempCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US) ?: ""
            val year = tempCal.get(Calendar.YEAR)
            val monthNum = tempCal.get(Calendar.MONTH) + 1
            labels.add(monthLabel)
            yearMonths.add(String.format(Locale.US, "%04d-%02d", year, monthNum))
        }

        // Aggregate actual entries
        weightLogs.forEach { log ->
            if (log.date.length >= 7) {
                val logYearMonth = log.date.substring(0, 7)
                val idx = yearMonths.indexOf(logYearMonth)
                if (idx != -1) {
                    sums[idx] += log.weightKg
                    counts[idx]++
                }
            }
        }

        // Map results. If no logs, fallback to default weight or preceding month average
        val result = mutableListOf<Pair<String, Double>>()
        var lastValidWeight = defaultWeight
        for (i in 4 downTo 0) {
            val avg = if (counts[i] > 0) sums[i] / counts[i] else lastValidWeight
            result.add(labels[i] to avg)
            lastValidWeight = avg
        }
        result
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Icon(Icons.Default.Scale, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(20.dp))
                Text("WEIGHT TREND (5-MONTHS AVERAGE)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandOrange)
            }

            val minWeight = remember(monthlyAverages) { max(0.0, (monthlyAverages.map { it.second }.minOrNull() ?: 50.0) - 5) }
            val maxWeight = remember(monthlyAverages) { (monthlyAverages.map { it.second }.maxOrNull() ?: 100.0) + 5 }
            val weightSpan = max(1.0, maxWeight - minWeight)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                monthlyAverages.forEach { (label, avg) ->
                    val normalizedVal = ((avg - minWeight) / weightSpan).toFloat()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", avg),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandOrange
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(80.dp * normalizedVal.coerceIn(0.15f, 1f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    BrandOrange,
                                                    BrandOrange.copy(alpha = 0.3f)
                                                )
                                            )
                                        )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. Least Hydrated Days Chart (Toggle Month/Year)
// ==========================================
@Composable
fun LeastHydrationCard(waterLogs: List<WaterLogEntity>) {
    var period by remember { mutableStateOf("month") } // "month", "year"

    val leastHydrationDays = remember(waterLogs, period) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR).toString()
        val currentMonth = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)

        val filtered = waterLogs.filter { log ->
            if (period == "month") {
                log.date.startsWith(currentMonth)
            } else {
                log.date.startsWith(currentYear)
            }
        }

        // Sort ascending by glasses drank to find LEAST hydration, take 5
        filtered.sortedBy { it.glasses }.take(5)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(20.dp))
                    Text("LEAST 5 HYDRATION DAYS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandCyan)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    listOf("month" to "Month", "year" to "Year").forEach { (id, label) ->
                        val isSelected = period == id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) BrandCyan else Color.Transparent)
                                .clickable { period = id }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (leastHydrationDays.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hydration logs recorded for this period.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    leastHydrationDays.forEach { log ->
                        val goal = max(1, log.goal)
                        val ratio = log.glasses.toFloat() / goal.toFloat()
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = log.date.substring(5), // Show MM-DD
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(45.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio.coerceAtMost(1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Brush.horizontalGradient(listOf(BrandCyan, BrandViolet)))
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${log.glasses}/${log.goal} gls",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (log.glasses < log.goal) BrandRose else BrandGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. Exercise Intensity Chart
// ==========================================
@Composable
fun ExerciseIntensityCard(exerciseLogs: List<ExerciseLogEntity>) {
    var sortType by remember { mutableStateOf("most") } // "most" or "least"

    val intensityDays = remember(exerciseLogs, sortType) {
        // Group exercise logs by date and calculate sum duration (intensity metric)
        val grouped = exerciseLogs.groupBy { it.date }
            .mapValues { entry -> entry.value.sumOf { it.durationMinutes } }
            .toList()

        if (sortType == "most") {
            grouped.sortedByDescending { it.second }.take(5)
        } else {
            // "least" intensity: sort ascending
            grouped.sortedBy { it.second }.take(5)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = BrandAmber, modifier = Modifier.size(20.dp))
                    Text("EXERCISE INTENSITY (TOP 5)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandAmber)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    listOf("most" to "Most", "least" to "Least").forEach { (id, label) ->
                        val isSelected = sortType == id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) BrandAmber else Color.Transparent)
                                .clickable { sortType = id }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (intensityDays.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No exercise sessions recorded.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                val maxMinutes = max(1, intensityDays.maxOfOrNull { it.second } ?: 1)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    intensityDays.forEach { (date, minutes) ->
                        val ratio = minutes.toFloat() / maxMinutes.toFloat()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = date.substring(5), // MM-DD
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(45.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio.coerceAtLeast(0.08f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Brush.horizontalGradient(listOf(BrandAmber, BrandOrange)))
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$minutes min",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandOrange
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. Vitals Charts (Blood Sugar and Blood Pressure)
// ==========================================
@Composable
fun VitalsHistoryCard(vitals: List<VitalReadingEntity>) {
    var period by remember { mutableStateOf("week") } // "week", "month", "year"

    // Filter vitals by period
    val filteredVitals = remember(vitals, period) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val limit = when (period) {
            "week" -> 7
            "month" -> 30
            else -> 365
        }
        cal.add(Calendar.DAY_OF_YEAR, -limit)
        val startDateStr = sdf.format(cal.time)

        vitals.filter { it.date >= startDateStr }.sortedBy { it.date }
    }

    val sugarReadings = remember(filteredVitals) {
        filteredVitals.filter { it.type == "blood_sugar" }.mapNotNull { log ->
            log.value.toDoubleOrNull()?.let { log.date to it }
        }
    }

    val pressureReadings = remember(filteredVitals) {
        filteredVitals.filter { it.type == "blood_pressure" }.mapNotNull { log ->
            // Blood pressure is usually Systolic/Diastolic e.g. "120/80"
            val parts = log.value.split("/")
            if (parts.size == 2) {
                val sys = parts[0].trim().toDoubleOrNull()
                val dia = parts[1].trim().toDoubleOrNull()
                if (sys != null && dia != null) {
                    Triple(log.date, sys, dia)
                } else null
            } else null
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = BrandRose, modifier = Modifier.size(20.dp))
                    Text("VITALS TIMELINE GRAPHS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandRose)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    listOf("week" to "7d", "month" to "30d", "year" to "1yr").forEach { (id, label) ->
                        val isSelected = period == id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) BrandRose else Color.Transparent)
                                .clickable { period = id }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7.1 Blood Sugar Line Chart
            Text("Blood Sugar Level (mg/dL)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandRose)
            Spacer(modifier = Modifier.height(8.dp))
            if (sugarReadings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No blood sugar readings for this period.", fontSize = 11.sp, color = Color.Gray)
                }
            } else {
                VitalsLineGraph(
                    data = sugarReadings,
                    lineColor = BrandRose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7.2 Blood Pressure (Dual Line Chart: Systolic & Diastolic)
            Text("Blood Pressure (Systolic / Diastolic)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandCyan)
            Spacer(modifier = Modifier.height(8.dp))
            if (pressureReadings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No blood pressure readings for this period.", fontSize = 11.sp, color = Color.Gray)
                }
            } else {
                BloodPressureDualGraph(
                    data = pressureReadings,
                    sysColor = BrandCyan,
                    diaColor = BrandViolet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )
            }
        }
    }
}

// Custom line chart drawer for single value (Sugar)
@Composable
fun VitalsLineGraph(
    data: List<Pair<String, Double>>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val minVal = max(0.0, (data.map { it.second }.minOrNull() ?: 70.0) - 10)
    val maxVal = (data.map { it.second }.maxOrNull() ?: 150.0) + 10
    val valSpan = max(1.0, maxVal - minVal)

    Canvas(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))) {
        val width = size.width
        val height = size.height
        val sizeCount = data.size

        if (sizeCount > 1) {
            val path = Path()
            val stepX = width / (sizeCount - 1)

            data.forEachIndexed { idx, point ->
                val x = idx * stepX
                val normalizedY = ((point.second - minVal) / valSpan).toFloat()
                val y = height - (normalizedY * (height - 30.dp.toPx()) + 15.dp.toPx())

                if (idx == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
                
                // Draw data points dot
                drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
            }

            // Draw line
            drawPath(path = path, color = lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        } else if (sizeCount == 1) {
            // Only 1 data point
            val x = width / 2
            val y = height / 2
            drawCircle(color = lineColor, radius = 6.dp.toPx(), center = Offset(x, y))
        }
    }
}

// Custom line chart drawer for dual value (Systolic / Diastolic)
@Composable
fun BloodPressureDualGraph(
    data: List<Triple<String, Double, Double>>,
    sysColor: Color,
    diaColor: Color,
    modifier: Modifier = Modifier
) {
    val minVal = max(0.0, (data.map { it.third }.minOrNull() ?: 60.0) - 10)
    val maxVal = (data.map { it.second }.maxOrNull() ?: 160.0) + 10
    val valSpan = max(1.0, maxVal - minVal)

    Canvas(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))) {
        val width = size.width
        val height = size.height
        val sizeCount = data.size

        if (sizeCount > 1) {
            val pathSys = Path()
            val pathDia = Path()
            val stepX = width / (sizeCount - 1)

            data.forEachIndexed { idx, item ->
                val x = idx * stepX
                
                // Systolic
                val normSys = ((item.second - minVal) / valSpan).toFloat()
                val ySys = height - (normSys * (height - 30.dp.toPx()) + 15.dp.toPx())

                // Diastolic
                val normDia = ((item.third - minVal) / valSpan).toFloat()
                val yDia = height - (normDia * (height - 30.dp.toPx()) + 15.dp.toPx())

                if (idx == 0) {
                    pathSys.moveTo(x, ySys)
                    pathDia.moveTo(x, yDia)
                } else {
                    pathSys.lineTo(x, ySys)
                    pathDia.lineTo(x, yDia)
                }

                // Dots
                drawCircle(color = sysColor, radius = 3.5.dp.toPx(), center = Offset(x, ySys))
                drawCircle(color = diaColor, radius = 3.5.dp.toPx(), center = Offset(x, yDia))
            }

            // Draw line systolic
            drawPath(path = pathSys, color = sysColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
            // Draw line diastolic
            drawPath(path = pathDia, color = diaColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        } else if (sizeCount == 1) {
            val x = width / 2
            val ySys = height / 3
            val yDia = height * 2 / 3
            drawCircle(color = sysColor, radius = 5.dp.toPx(), center = Offset(x, ySys))
            drawCircle(color = diaColor, radius = 5.dp.toPx(), center = Offset(x, yDia))
        }
    }
}

// ==========================================
// 8. Sleep Quality Chart (Most / Least, Month/Year Toggle)
// ==========================================
@Composable
fun SleepHistoryCard(sleepLogs: List<SleepLogEntity>) {
    var period by remember { mutableStateOf("month") } // "month", "year"
    var sortType by remember { mutableStateOf("most") } // "most", "least"

    val sleepDays = remember(sleepLogs, period, sortType) {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR).toString()
        val currentMonth = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)

        val filtered = sleepLogs.filter { log ->
            if (period == "month") {
                log.date.startsWith(currentMonth)
            } else {
                log.date.startsWith(currentYear)
            }
        }

        if (sortType == "most") {
            filtered.sortedByDescending { it.hoursSlept }.take(5)
        } else {
            filtered.sortedBy { it.hoursSlept }.take(5)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Bedtime, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(20.dp))
                    Text("SLEEP QUALITY METRICS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandIndigo)
                }

                // Period toggles
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    listOf("month" to "Mo", "year" to "Yr").forEach { (id, label) ->
                        val isSelected = period == id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) BrandIndigo else Color.Transparent)
                                .clickable { period = id }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Most / Least sort toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("most" to "Most Slept days", "least" to "Least Slept days").forEach { (id, label) ->
                    val isSelected = sortType == id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) BrandIndigo.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, if (isSelected) BrandIndigo else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { sortType = id }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) BrandIndigo else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (sleepDays.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No sleep logs recorded for this period.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                val maxHours = max(1.0, sleepDays.maxOfOrNull { it.hoursSlept } ?: 8.0)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sleepDays.forEach { log ->
                        val ratio = log.hoursSlept / maxHours
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = log.date.substring(5), // MM-DD
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(45.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio.toFloat().coerceAtMost(1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Brush.horizontalGradient(listOf(BrandIndigo, BrandPink)))
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f hrs", log.hoursSlept),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandIndigo
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. Symptom Tracker Chart (Date vs Symptom Timeline with Toggle)
// ==========================================
@Composable
fun SymptomTimelineCard(healthIssues: List<HealthIssueLogEntity>) {
    var severityFilter by remember { mutableStateOf("all") } // "all", "mild", "moderate", "severe"

    val filteredTimeline = remember(healthIssues, severityFilter) {
        val grouped = healthIssues
            .filter { severityFilter == "all" || it.severity.equals(severityFilter, ignoreCase = true) }
            .groupBy { it.date }
            .mapValues { entry ->
                // Count how many of each severity exist on this date
                val mild = entry.value.count { it.severity.equals("mild", ignoreCase = true) }
                val moderate = entry.value.count { it.severity.equals("moderate", ignoreCase = true) }
                val severe = entry.value.count { it.severity.equals("severe", ignoreCase = true) }
                Triple(mild, moderate, severe)
            }
            .toList()
            .sortedByDescending { it.first } // sort latest dates first
            .take(5) // display top 5 most recent symptom days
        grouped
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ReportProblem, contentDescription = null, tint = BrandRose, modifier = Modifier.size(20.dp))
                    Text("SYMPTOM TRACKER TIMELINE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandRose)
                }

                // Severity Filter Buttons (Full-Width Responsive Row)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("all" to "All", "mild" to "Mild", "moderate" to "Moderate", "severe" to "Severe").forEach { (id, label) ->
                        val isSelected = severityFilter == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) BrandRose else Color.Transparent)
                                .clickable { severityFilter = id }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredTimeline.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No symptoms logged matching the filter.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    filteredTimeline.forEach { (date, counts) ->
                        val (mildCount, modCount, sevCount) = counts
                        val total = mildCount + modCount + sevCount

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(date, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (mildCount > 0) BadgeCount(label = "Mild", count = mildCount, color = BrandGreen)
                                    if (modCount > 0) BadgeCount(label = "Mod", count = modCount, color = BrandAmber)
                                    if (sevCount > 0) BadgeCount(label = "Sev", count = sevCount, color = BrandRose)
                                }
                            }

                            // Horizontal stacked visual representation
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (total > 0) {
                                    val mildRatio = mildCount.toFloat() / total
                                    val modRatio = modCount.toFloat() / total
                                    val sevRatio = sevCount.toFloat() / total

                                    if (mildRatio > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(max(0.01f, mildRatio))
                                                .background(BrandGreen)
                                        )
                                    }
                                    if (modRatio > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(max(0.01f, modRatio))
                                                .background(BrandAmber)
                                        )
                                    }
                                    if (sevRatio > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(max(0.01f, sevRatio))
                                                .background(BrandRose)
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

@Composable
fun BadgeCount(label: String, count: Int, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text("$label: $count", fontSize = 9.sp, fontWeight = FontWeight.Black, color = color)
    }
}

// ==========================================
// 10. Finance Overview Analytics Card (Tailored for Monthly Salaried Person)
// ==========================================
@Composable
fun FinanceOverviewAnalyticsCard(financeLogs: List<FinanceLogEntity>) {
    val totalIncome = remember(financeLogs) {
        financeLogs.filter { it.type == "income" }.sumOf { it.amount }
    }
    val totalExpense = remember(financeLogs) {
        financeLogs.filter { it.type == "expense" }.sumOf { it.amount }
    }
    val totalSavings = remember(financeLogs) {
        financeLogs.filter { it.type == "savings" }.sumOf { it.amount }
    }

    // Active Surplus
    val activeSurplus = remember(totalIncome, totalExpense, totalSavings) {
        totalIncome - totalExpense - totalSavings
    }

    // Salary Burn Rate (What percentage of Salary is spent on expenses)
    val burnRate = remember(totalIncome, totalExpense) {
        if (totalIncome > 0) (totalExpense / totalIncome) * 100.0 else 0.0
    }

    // Investment Rate (What percentage of Salary is directed to long term savings)
    val investmentRate = remember(totalIncome, totalSavings) {
        if (totalIncome > 0) (totalSavings / totalIncome) * 100.0 else 0.0
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with Salaried focus
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(20.dp))
                Text("MONTHLY SALARY ALLOCATION PROFILE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
            }

            // Salary Stat Summary
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandViolet.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(0.5.dp, BrandViolet.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "ESTIMATED MONTHLY SURPLUS", 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "₹${String.format("%.2f", activeSurplus)}", 
                            fontSize = 22.sp, 
                            fontWeight = FontWeight.Black, 
                            color = if (activeSurplus >= 0) BrandGreen else BrandRose
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeSurplus >= 0) BrandGreen.copy(alpha = 0.12f) else BrandRose.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (activeSurplus >= 0) "Surplus Healthy" else "In Deficit",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeSurplus >= 0) BrandGreen else BrandRose
                        )
                    }
                }
            }

            // Stat Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Net Salary Mini-Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandViolet.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Stable Salary", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("₹${String.format("%.0f", totalIncome)}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = BrandViolet)
                    }
                }

                // Total Expense Mini-Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandRose.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Expenses Paid", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandRose)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("₹${String.format("%.0f", totalExpense)}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = BrandRose)
                    }
                }

                // Savings Mini-Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandGreen.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Salary Saved", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("₹${String.format("%.0f", totalSavings)}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = BrandGreen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Burn Rate Tracker
            Text("Monthly Salary Burn Speed", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((burnRate / 100.0).toFloat().coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(BrandCyan, BrandRose)))
                    )
                }
                Text(
                    text = "${String.format("%.1f", burnRate)}%",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = if (burnRate > 75.0) BrandRose else if (burnRate > 50.0) BrandAmber else BrandCyan
                )
            }
            Text(
                text = when {
                    totalIncome == 0.0 -> "Waiting for monthly salaried payroll entry to gauge speed."
                    burnRate > 80.0 -> "⚠️ Alert! Burn rate is extremely high. Most of your paycheck is spent."
                    burnRate > 60.0 -> "Pacing normally, but try to limit lifestyle creep for the rest of the cycle."
                    burnRate > 30.0 -> "Highly disciplined burn rate. Keeping safe surplus runway. 👍"
                    else -> "Excellent control. Vast majority of your salary remains intact. 💎"
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            // Investment pacing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Salary Wealth-Building Index: ${String.format("%.1f", investmentRate)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (investmentRate >= 20.0) {
                            "Superb! You are exceeding the standard 20% savings rule. Compound growth is secured."
                        } else {
                            "Try to allocate at least 20% of your salary first (₹${String.format("%.0f", totalIncome * 0.2)}) on pay-day before spending."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// ==========================================
// 11. Finance Expense Distribution Card (50-30-20 Rule Analysis)
// ==========================================
@Composable
fun FinanceExpenseDistributionCard(financeLogs: List<FinanceLogEntity>) {
    val totalIncome = remember(financeLogs) {
        val inc = financeLogs.filter { it.type == "income" }.sumOf { it.amount }
        if (inc <= 0) {
            // Fallback base to sum of all activities to prevent divide-by-zero
            val sum = financeLogs.sumOf { it.amount }
            if (sum <= 0) 1.0 else sum
        } else {
            inc
        }
    }

    // 50/30/20 Rule Classifications:
    // 50% Needs: Housing, Groceries, Education, Transport, Healthcare
    val needsSum = remember(financeLogs) {
        financeLogs.filter { log ->
            log.type == "expense" && (
                log.category.startsWith("Housing") ||
                log.category.startsWith("Groceries") ||
                log.category.startsWith("Education") ||
                log.category.startsWith("Transport") ||
                log.category.startsWith("Healthcare")
            )
        }.sumOf { it.amount }
    }

    // 30% Wants: Lifestyle, Entertainment, Discretionary, and Others (defaults)
    val wantsSum = remember(financeLogs) {
        financeLogs.filter { log ->
            log.type == "expense" && (
                log.category.startsWith("Lifestyle") ||
                log.category == "Others" ||
                (!log.category.startsWith("Housing") &&
                 !log.category.startsWith("Groceries") &&
                 !log.category.startsWith("Education") &&
                 !log.category.startsWith("Transport") &&
                 !log.category.startsWith("Healthcare"))
            )
        }.sumOf { it.amount }
    }

    // 20% Savings
    val savingsSum = remember(financeLogs) {
        financeLogs.filter { it.type == "savings" }.sumOf { it.amount }
    }

    // Percentages compared to salaried base
    val needsPercent = (needsSum / totalIncome) * 100.0
    val wantsPercent = (wantsSum / totalIncome) * 100.0
    val savingsPercent = (savingsSum / totalIncome) * 100.0

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.PieChart, contentDescription = null, tint = BrandRose, modifier = Modifier.size(20.dp))
                Text("THE 50/30/20 SALARY BUDGET COMPLIANCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandRose)
            }

            Text(
                text = "The classic salary allocation strategy: 50% for fixed Needs, 30% for flexible Wants, and 20% for future Savings.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Needs (Target 50%)
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ESSENTIAL NEEDS (Target: 50%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandRose)
                        Text("Housing, Food, Utilities, Health, Transport", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Text(
                        "₹${String.format("%.0f", needsSum)} (${String.format("%.1f", needsPercent)}%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = BrandRose
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Actual Allocation Progress
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((needsPercent / 100.0).toFloat().coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(BrandRose)
                        )
                    }
                    // Target indicator benchmark
                    Text("Target: 50%", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }

            // Wants (Target 30%)
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("DISCRETIONARY WANTS (Target: 30%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandAmber)
                        Text("Lifestyle, Dining out, Shopping, Subscriptions", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Text(
                        "₹${String.format("%.0f", wantsSum)} (${String.format("%.1f", wantsPercent)}%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = BrandAmber
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((wantsPercent / 100.0).toFloat().coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(BrandAmber)
                        )
                    }
                    Text("Target: 30%", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }

            // Savings (Target 20%)
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SAVINGS & SECURITY (Target: 20%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                        Text("EPF, PPF, NPS, Mutual Funds, Stocks, Buffer", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Text(
                        "₹${String.format("%.0f", savingsSum)} (${String.format("%.1f", savingsPercent)}%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = BrandGreen
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((savingsPercent / 100.0).toFloat().coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(BrandGreen)
                        )
                    }
                    Text("Target: 20%", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }

            // Salaried feedback recommendation
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = BrandViolet,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = when {
                            wantsPercent > 30.0 -> "⚠️ Salary budget warning: Discretionary Wants are taking ${String.format("%.1f", wantsPercent)}% of your salary. We recommend pausing high-ticket leisure spends."
                            savingsPercent < 20.0 -> "📉 Under-saved: Your Paycheck Savings Rate of ${String.format("%.1f", savingsPercent)}% is below the healthy 20% boundary. Try setting aside savings directly on salary day."
                            needsPercent > 55.0 -> "💼 Cost-heavy baseline: Essential fixed expenses are heavy at ${String.format("%.1f", needsPercent)}%. Check if you can optimize subscription costs or rent."
                            else -> "🎉 Perfect Salaried Discipline! Your financial allocation perfectly embodies the 50/30/20 guideline. Excellent work."
                        },
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ==========================================
// 12. Finance Savings Distribution Card (Investment Wealth Profile)
// ==========================================
@Composable
fun FinanceSavingsDistributionCard(financeLogs: List<FinanceLogEntity>) {
    val savingsLogs = remember(financeLogs) { financeLogs.filter { it.type == "savings" } }
    val totalSavings = remember(savingsLogs) { savingsLogs.sumOf { it.amount } }

    val savingsCategories = listOf(
        "PPF",
        "FD",
        "RD",
        "NPS",
        "Mutual Funds",
        "EPF",
        "Stocks",
        "Simple Savings in Account",
        "Others"
    )

    // Calculate group classification
    // 1. Retirement & Tax-saving (EPF, NPS, PPF)
    val taxRetirementSum = remember(savingsLogs) {
        savingsLogs.filter { it.category in listOf("EPF", "NPS", "PPF") }.sumOf { it.amount }
    }
    // 2. High Growth Equities (Mutual Funds, Stocks)
    val growthEquitiesSum = remember(savingsLogs) {
        savingsLogs.filter { it.category in listOf("Mutual Funds", "Stocks") }.sumOf { it.amount }
    }
    // 3. Liquid Cash Buffer (FD, RD, Simple Savings, Others)
    val liquidBufferSum = remember(savingsLogs) {
        savingsLogs.filter { it.category in listOf("FD", "RD", "Simple Savings in Account", "Others") }.sumOf { it.amount }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                Text("SALARIED WEALTH & RETIREMENT ACCUMULATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
            }

            if (savingsLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No savings allocated. Distribute savings on payday to inspect portfolio structure.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                Text(
                    text = "A salaried wealth strategy divides accumulation into safe tax hedges, compounding equities, and liquid reserves.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                // Render group 1: Tax Hedge / Pension
                WealthSectorBar(
                    title = "Retirement & Tax Shields (EPF/NPS/PPF)",
                    amount = taxRetirementSum,
                    total = totalSavings,
                    color = BrandIndigo,
                    description = "Shields salary from tax liabilities while building standard pension bedrock."
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Render group 2: High Growth
                WealthSectorBar(
                    title = "Compound Growth Equities (Mutual Funds/Stocks)",
                    amount = growthEquitiesSum,
                    total = totalSavings,
                    color = BrandGreen,
                    description = "Outpaces salary inflation over 5+ year cycles for actual long-term wealth."
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Render group 3: Liquid Reserve
                WealthSectorBar(
                    title = "Emergency Liquid Buffer (FD/RD/Account)",
                    amount = liquidBufferSum,
                    total = totalSavings,
                    color = BrandCyan,
                    description = "Readily accessible in case of medical emergency or job transitions."
                )
            }
        }
    }
}

@Composable
fun WealthSectorBar(
    title: String,
    amount: Double,
    total: Double,
    color: Color,
    description: String
) {
    val percentage = if (total > 0) (amount / total) * 100.0 else 0.0

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "₹${String.format("%.0f", amount)} (${String.format("%.1f", percentage)}%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
        Text(
            text = description,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((percentage / 100.0).toFloat().coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(color)
            )
        }
    }
}

// ==========================================
// 13. Finance Cumulative Spend Line Chart (Tailored Monthly Pacing)
// ==========================================
@Composable
fun FinanceDailyTrendsCard(financeLogs: List<FinanceLogEntity>) {
    // Collect the expense entries, sort chronologically, and map cumulative expenses
    val chronologicalExpenses = remember(financeLogs) {
        financeLogs.filter { it.type == "expense" }
            .sortedBy { it.date }
    }

    val cumulativeSpendPoints = remember(chronologicalExpenses) {
        var runningTotal = 0.0
        chronologicalExpenses.map { log ->
            runningTotal += log.amount
            // Extract Day portion if it is YYYY-MM-DD
            val dayLabel = if (log.date.length >= 10) log.date.substring(8, 10) else log.date
            dayLabel to runningTotal
        }
    }

    // Stable Salary Reference limit
    val totalIncome = remember(financeLogs) {
        val inc = financeLogs.filter { it.type == "income" }.sumOf { it.amount }
        if (inc <= 0) 50000.0 else inc // fallback assumed standard salaried layout
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.ShowChart, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(20.dp))
                Text("MONTHLY PACING & SPEND SPEED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandOrange)
            }

            Text(
                text = "Cumulative spend curve showing your salary burn pace compared to your salary limit of ₹${String.format("%.0f", totalIncome)}.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 14.dp)
            )

            if (cumulativeSpendPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No monthly expenses registered yet. Pacing will build as you log purchases.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                val maxSpend = cumulativeSpendPoints.lastOrNull()?.second ?: 0.0
                val ceiling = kotlin.math.max(totalIncome, maxSpend) * 1.1

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val lineColor = BrandOrange
                    val areaColor = BrandOrange.copy(alpha = 0.08f)
                    val limitLineColor = BrandRose.copy(alpha = 0.4f)

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        // 1. Draw dashed horizontal Salary ceiling line
                        val salaryY = (height - (totalIncome / ceiling) * height).toFloat()
                        if (salaryY in 0f..height) {
                            drawLine(
                                color = limitLineColor,
                                start = Offset(0f, salaryY),
                                end = Offset(width, salaryY),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        // 2. Draw line points
                        val pointsCount = cumulativeSpendPoints.size
                        if (pointsCount > 1) {
                            val path = Path()
                            val fillPath = Path()

                            val stepX = width / (pointsCount - 1)

                            cumulativeSpendPoints.forEachIndexed { i, (_, total) ->
                                val x = i * stepX
                                val y = (height - (total / ceiling) * height).toFloat()

                                if (i == 0) {
                                    path.moveTo(x, y)
                                    fillPath.moveTo(x, height)
                                    fillPath.lineTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                    fillPath.lineTo(x, y)
                                }

                                if (i == pointsCount - 1) {
                                    fillPath.lineTo(x, height)
                                    fillPath.close()
                                }
                            }

                            // Draw area fill
                            drawPath(fillPath, areaColor)

                            // Draw glow path line
                            drawPath(
                                path = path,
                                color = lineColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Draw last point dot
                            val lastX = width
                            val lastY = (height - (maxSpend / ceiling) * height).toFloat()
                            drawCircle(
                                color = lineColor,
                                radius = 6.dp.toPx(),
                                center = Offset(lastX, lastY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.dp.toPx(),
                                center = Offset(lastX, lastY)
                            )
                        } else if (pointsCount == 1) {
                            // Single point dot
                            val singleY = (height - (maxSpend / ceiling) * height).toFloat()
                            drawCircle(
                                color = lineColor,
                                radius = 6.dp.toPx(),
                                center = Offset(width / 2f, singleY)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Chart Labels Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val firstDay = cumulativeSpendPoints.firstOrNull()?.first ?: "01"
                    val lastDay = cumulativeSpendPoints.lastOrNull()?.first ?: "30"

                    Text("Day $firstDay (Pay Cycle Start)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("Current Expense Stack: ₹${String.format("%.0f", maxSpend)}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = BrandOrange)
                    Text("Day $lastDay", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(10.dp, 2.dp).background(BrandOrange))
                        Text("Cumulative Spends Pace", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(10.dp, 2.dp).background(BrandRose.copy(alpha = 0.4f)))
                        Text("Salary Limit Baseline", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

// ==========================================
// 14. Custom Pie/Donut Chart & Detailed Financial Insights Cards
// ==========================================

@Composable
fun CustomDonutChart(
    slices: List<Pair<String, Double>>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = remember(slices) { slices.sumOf { it.second } }
    
    if (total <= 0.0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No transactions logged for this period",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left side: Pie Chart Canvas
        Box(
            modifier = Modifier
                .size(132.dp)
                .weight(1.1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sizeMin = min(size.width, size.height)
                val strokeWidth = sizeMin * 0.16f // Sleeker Donut ring width to prevent overlap
                val arcSize = sizeMin - strokeWidth
                
                var startAngle = -90f
                slices.forEachIndexed { index, (_, value) ->
                    if (value > 0.0) {
                        val sweepAngle = ((value / total) * 360f).toFloat()
                        drawArc(
                            color = colors.getOrElse(index) { Color.Gray },
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset((size.width - arcSize) / 2f, (size.height - arcSize) / 2f),
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                        startAngle += sweepAngle
                    }
                }
            }
            
            // Center text inside donut
            val amountStr = "₹${String.format("%.0f", total)}"
            val dynamicFontSize = when {
                amountStr.length > 9 -> 9.sp
                amountStr.length > 7 -> 10.sp
                amountStr.length > 5 -> 12.sp
                else -> 14.sp
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                Text(
                    text = "TOTAL",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    text = amountStr,
                    fontSize = dynamicFontSize,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }

        // Right side: Custom scrollable legend
        Column(
            modifier = Modifier.weight(1.9f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            slices.forEachIndexed { index, (label, value) ->
                if (value > 0.0) {
                    val pct = (value / total) * 100.0
                    val color = colors.getOrElse(index) { Color.Gray }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Column {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "₹${String.format("%.0f", value)} (${String.format("%.1f", pct)}%)",
                                fontSize = 10.sp,
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseSavingsPieChartCard(
    financeLogs: List<FinanceLogEntity>,
    selectedYear: Int,
    selectedMonth: Int
) {
    var viewYearly by remember { mutableStateOf(false) } // true = Year, false = Month
    var activeTab by remember { mutableStateOf(0) } // 0 = Overview, 1 = Expense Subcats, 2 = Savings Subcats
    
    val logs = remember(financeLogs, selectedYear, selectedMonth, viewYearly) {
        val monthParts = if (viewYearly) null else selectedMonth
        financeLogs.filter { log ->
            val parts = log.date.split("-")
            val logYear = parts.getOrNull(0)?.toIntOrNull() ?: 2026
            val logMonth = parts.getOrNull(1)?.toIntOrNull() ?: 7
            logYear == selectedYear && (monthParts == null || logMonth == monthParts)
        }
    }

    val totalExpense = remember(logs) {
        logs.filter { it.type == "expense" }.sumOf { it.amount }
    }
    val totalSavings = remember(logs) {
        logs.filter { it.type == "savings" }.sumOf { it.amount }
    }

    // Prepare slices based on the active tab
    val chartSlices = remember(logs, activeTab, totalExpense, totalSavings) {
        when (activeTab) {
            0 -> listOf(
                "Expenses" to totalExpense,
                "Savings" to totalSavings
            )
            1 -> {
                logs.filter { it.type == "expense" }
                    .groupBy { it.title }
                    .map { (title, list) -> title to list.sumOf { it.amount } }
                    .filter { it.second > 0.0 }
                    .sortedByDescending { it.second }
            }
            2 -> {
                logs.filter { it.type == "savings" }
                    .groupBy { it.category }
                    .map { (category, list) -> category to list.sumOf { it.amount } }
                    .filter { it.second > 0.0 }
                    .sortedByDescending { it.second }
            }
            else -> emptyList()
        }
    }
    
    val palette = listOf(
        BrandRose, BrandGreen, BrandViolet, BrandCyan, BrandAmber, BrandPink, BrandOrange,
        Color(0xFF3F51B5), Color(0xFF009688), Color(0xFFFF5722), Color(0xFF9C27B0), Color(0xFFE91E63),
        Color(0xFF4CAF50), Color(0xFF00BCD4), Color(0xFFFFC107), Color(0xFF8BC34A), Color(0xFFCDDC39)
    )
    
    val sliceColors = remember(chartSlices, activeTab) {
        if (activeTab == 0) {
            listOf(BrandRose, BrandGreen)
        } else {
            chartSlices.mapIndexed { index, _ -> palette[index % palette.size] }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = BrandViolet,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "EXPENSE VS SAVINGS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandViolet
                    )
                }

                // Month vs Year toggle switch
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!viewYearly) BrandViolet.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { viewYearly = false }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Month",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!viewYearly) BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (viewYearly) BrandViolet.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { viewYearly = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Year",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewYearly) BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab selection for Overview vs Detailed Subcategories
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Overview", "Expense Subcats", "Savings Subcats").forEachIndexed { idx, label ->
                    val selected = activeTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) BrandViolet.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { activeTab = idx }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            CustomDonutChart(
                slices = chartSlices,
                colors = sliceColors,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun IncomeDistributionPieChartCard(
    financeLogs: List<FinanceLogEntity>,
    selectedYear: Int,
    selectedMonth: Int
) {
    var viewYearly by remember { mutableStateOf(false) } // true = Year, false = Month
    var activeTab by remember { mutableStateOf(0) } // 0 = Sources, 1 = Detailed Items
    
    val logs = remember(financeLogs, selectedYear, selectedMonth, viewYearly) {
        val monthParts = if (viewYearly) null else selectedMonth
        financeLogs.filter { log ->
            val parts = log.date.split("-")
            val logYear = parts.getOrNull(0)?.toIntOrNull() ?: 2026
            val logMonth = parts.getOrNull(1)?.toIntOrNull() ?: 7
            log.type == "income" && logYear == selectedYear && (monthParts == null || logMonth == monthParts)
        }
    }

    val chartSlices = remember(logs, activeTab) {
        if (activeTab == 0) {
            logs.groupBy { it.title }
                .map { (title, list) -> title to list.sumOf { it.amount } }
                .filter { it.second > 0.0 }
                .sortedByDescending { it.second }
        } else {
            logs.groupBy { log ->
                log.notes?.trim()?.ifEmpty { null } ?: log.title
            }.map { (label, list) -> label to list.sumOf { it.amount } }
             .filter { it.second > 0.0 }
             .sortedByDescending { it.second }
        }
    }
    
    val palette = listOf(
        BrandGreen, BrandViolet, BrandCyan, BrandAmber, BrandPink, BrandOrange,
        Color(0xFF3F51B5), Color(0xFF009688), Color(0xFFFF5722), Color(0xFF9C27B0), Color(0xFFE91E63),
        Color(0xFF4CAF50), Color(0xFF00BCD4), Color(0xFFFFC107)
    )
    
    val sliceColors = remember(chartSlices) {
        chartSlices.mapIndexed { index, _ -> palette[index % palette.size] }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DonutLarge,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "INCOME SOURCES DISTRIBUTION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen
                    )
                }

                // Month vs Year toggle switch
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!viewYearly) BrandGreen.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { viewYearly = false }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Month",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!viewYearly) BrandGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (viewYearly) BrandGreen.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { viewYearly = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Year",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewYearly) BrandGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab selection for Sources vs Detailed Items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Income Sources", "Detailed Items").forEachIndexed { idx, label ->
                    val selected = activeTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) BrandGreen.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { activeTab = idx }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) BrandGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            CustomDonutChart(
                slices = chartSlices,
                colors = sliceColors,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TopSpentDaysCard(
    financeLogs: List<FinanceLogEntity>,
    selectedYear: Int,
    selectedMonth: Int
) {
    var viewYearly by remember { mutableStateOf(false) } // true = Year, false = Month
    
    // Group and aggregate expenses
    val displayedLogs = remember(financeLogs, selectedYear, selectedMonth, viewYearly) {
        val yearParts = selectedYear
        val monthParts = if (viewYearly) null else selectedMonth
        
        financeLogs.filter { log ->
            val parts = log.date.split("-")
            val logYear = parts.getOrNull(0)?.toIntOrNull() ?: 2026
            val logMonth = parts.getOrNull(1)?.toIntOrNull() ?: 7
            log.type == "expense" && logYear == yearParts && (monthParts == null || logMonth == monthParts)
        }
    }

    val topDays = remember(displayedLogs) {
        displayedLogs.groupBy { it.date }
            .map { (date, logs) ->
                // Format date string to friendly name like "Jul 03"
                val friendlyDate = try {
                    val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val sdfOut = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                    val sdfOutMonth = SimpleDateFormat("MMM dd", Locale.US)
                    
                    sdfIn.parse(date)?.let { 
                        if (viewYearly) sdfOut.format(it) else sdfOutMonth.format(it)
                    } ?: date
                } catch (e: Exception) {
                    date
                }
                friendlyDate to logs.sumOf { it.amount }
            }
            .sortedByDescending { it.second }
            .take(5)
    }

    val maxDaySpend = remember(topDays) {
        topDays.maxOfOrNull { it.second } ?: 1.0
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = BrandRose,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "TOP 5 SPENT DAYS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandRose
                    )
                }

                // Month vs Year toggle switch
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!viewYearly) BrandRose.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { viewYearly = false }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Month",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!viewYearly) BrandRose else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (viewYearly) BrandRose.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { viewYearly = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Year",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewYearly) BrandRose else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (topDays.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses recorded in this period",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    topDays.forEachIndexed { index, (dayLabel, spendAmount) ->
                        val barWidthFraction = (spendAmount / maxDaySpend).toFloat().coerceIn(0f, 1f)
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(BrandRose.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandRose
                                        )
                                    }
                                    Text(
                                        text = dayLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "₹${String.format("%.0f", spendAmount)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BrandRose
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(barWidthFraction)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(BrandRose)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetWorthPieChartCard(
    netWorthItems: List<NetWorthItemEntity>
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Assets vs Debt, 1 = Assets Breakdown, 2 = Debt Breakdown

    val totalAssets = remember(netWorthItems) { netWorthItems.filter { it.type == "asset" }.sumOf { it.amount } }
    val totalLoans = remember(netWorthItems) { netWorthItems.filter { it.type == "loan" }.sumOf { it.amount } }
    val totalLiabilities = remember(netWorthItems) { netWorthItems.filter { it.type == "liability" }.sumOf { it.amount } }
    val totalDebt = totalLoans + totalLiabilities
    val netWorth = totalAssets - totalDebt

    val chartSlices = remember(netWorthItems, activeTab, totalAssets, totalDebt) {
        when (activeTab) {
            0 -> listOf(
                "Assets" to totalAssets,
                "Debts" to totalDebt
            )
            1 -> {
                netWorthItems.filter { it.type == "asset" }
                    .map { it.name to it.amount }
                    .filter { it.second > 0.0 }
                    .sortedByDescending { it.second }
            }
            2 -> {
                netWorthItems.filter { it.type != "asset" }
                    .map { it.name to it.amount }
                    .filter { it.second > 0.0 }
                    .sortedByDescending { it.second }
            }
            else -> emptyList()
        }
    }

    val palette = listOf(
        BrandGreen, BrandRose, BrandViolet, BrandCyan, BrandAmber, BrandPink, BrandOrange,
        Color(0xFF3F51B5), Color(0xFF009688), Color(0xFFFF5722), Color(0xFF9C27B0), Color(0xFFE91E63),
        Color(0xFF4CAF50), Color(0xFF00BCD4), Color(0xFFFFC107), Color(0xFF8BC34A), Color(0xFFCDDC39)
    )

    val sliceColors = remember(chartSlices, activeTab) {
        if (activeTab == 0) {
            listOf(BrandGreen, BrandRose)
        } else {
            chartSlices.mapIndexed { index, _ -> palette[index % palette.size] }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Net Worth Allocation",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Total Wealth: ₹${String.format("%,.0f", netWorth)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (netWorth >= 0) BrandGreen else BrandRose
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "Assets vs Debts" to 0,
                    "Assets" to 1,
                    "Debts" to 2
                ).forEach { (label, tabIndex) ->
                    val isSel = activeTab == tabIndex
                    val col = if (tabIndex == 0) BrandGreen else if (tabIndex == 1) BrandGreen else BrandRose
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) col.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { activeTab = tabIndex }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) col else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CustomDonutChart(
                slices = chartSlices,
                colors = sliceColors,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TrackWiseHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Help, contentDescription = null, tint = BrandViolet)
                Text("How to Use TrackWise", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                item {
                    Text(
                        text = "TrackWise is your unified companion for habits, health, budget, and net worth tracking. Here is how to make the most of it:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                item {
                    Divider()
                }
                item {
                    Text("📊 Analytics Center", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandViolet)
                    Text("Toggle between Finance, Habits, and Health categories using the dropdown. Interactive donut charts and bar graphs show your progress and allocations in real time.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                item {
                    Text("💸 Monthly Budget & Equation", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandViolet)
                    Text("Add your income, expenses, and savings. The equation must balance (Income = Expenses + Savings). Any savings you record automatically increase your Net Worth assets, and any expenses are deducted from your spend source.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                item {
                    Text("🏦 Net Worth & Accounts", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandGreen)
                    Text("Add and monitor assets (cash, mutual funds, stocks) vs liabilities/loans. Expenses deduct from assets, and savings add to assets, giving you a crystal clear net worth calculation.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                item {
                    Text("❤️ Health & Hydration Meter", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandRose)
                    Text("Log metrics to calculate BMI. Tap '+' or '-' on the Hydration Meter to track daily water glasses. Record exercise logs, monitor sleep times, and view clinical tips.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                item {
                    Text("📝 Habits & Tasks", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandAmber)
                    Text("Add daily habits and toggle completions to build streaks. Create tasks with priorities, sub-tasks, and due dates to organize your day.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It", color = BrandViolet, fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ==========================================
// Menstrual Cycle Analytics Composables
// ==========================================

private fun getEstrogenLevel(day: Float, totalDays: Float): Float {
    val normDay = (day - 1f) / (totalDays - 1f) // 0.0 to 1.0
    return if (normDay < 0.5f) {
        val t = normDay / 0.5f
        0.1f + 0.9f * t * t
    } else {
        val t = (normDay - 0.5f) / 0.5f
        if (t < 0.2f) {
            val subt = t / 0.2f
            1.0f - 0.7f * subt
        } else {
            val lutealNorm = (t - 0.2f) / 0.8f
            val peakFactor = (1.0f - Math.abs(lutealNorm - 0.5f) / 0.5f)
            0.3f + 0.25f * peakFactor * peakFactor
        }
    }
}

private fun getProgesteroneLevel(day: Float, totalDays: Float): Float {
    val normDay = (day - 1f) / (totalDays - 1f) // 0.0 to 1.0
    return if (normDay < 0.5f) {
        0.05f
    } else {
        val t = (normDay - 0.5f) / 0.5f
        val bell = Math.exp(-Math.pow((t - 0.5).toDouble(), 2.0) / 0.08).toFloat()
        0.05f + 0.95f * bell
    }
}

@Composable
fun HormonalPhaseOverlayCard(periodCycles: List<PeriodCycleEntity>) {
    val latestCycle = remember(periodCycles) {
        periodCycles.sortedByDescending { it.startDate }.firstOrNull()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = BrandPink, modifier = Modifier.size(20.dp))
                Text("HORMONAL PHASE OVERLAY 🌸", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPink)
            }
            Text(
                "Estrogen vs Progesterone fluctuation throughout your cycle",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (latestCycle == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No period cycles logged.", fontSize = 12.sp, color = Color.Gray)
                        Text("Log your period start in Health to view overlay.", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            } else {
                val totalDays = latestCycle.cycleLengthDays.coerceAtLeast(1)
                val durationDays = latestCycle.durationDays.coerceIn(1, totalDays)

                // Calculate current day
                val cycleDay = remember(latestCycle) {
                    try {
                        val todayStr = TrackWiseUtils.getTodayString()
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val todayDate = sdf.parse(todayStr)
                        val startDate = sdf.parse(latestCycle.startDate)
                        val diffMillis = todayDate.time - startDate.time
                        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt() + 1
                        if (diffDays <= 0) {
                            1
                        } else {
                            ((diffDays - 1) % totalDays) + 1
                        }
                    } catch (e: Exception) {
                        1
                    }
                }

                // Determine phase
                val phaseName = when {
                    cycleDay <= durationDays -> "Menstrual Phase (Bleeding)"
                    cycleDay < totalDays / 2f -> "Follicular Phase (Estrogen Peak)"
                    cycleDay.toFloat() in (totalDays / 2f - 1f)..(totalDays / 2f + 1f) -> "Ovulatory Phase (Fertile Window)"
                    else -> "Luteal Phase (Progesterone Peak)"
                }

                val phaseColor = when {
                    cycleDay <= durationDays -> Color(0xFFEF5350)
                    cycleDay < totalDays / 2f -> Color(0xFF42A5F5)
                    cycleDay.toFloat() in (totalDays / 2f - 1f)..(totalDays / 2f + 1f) -> Color(0xFF66BB6A)
                    else -> Color(0xFFFFA726)
                }

                val phaseInsight = when {
                    cycleDay <= durationDays -> "Energy levels may be lower. Rest, light stretching, and self-care are recommended. Estrogen and progesterone are at baseline levels."
                    cycleDay < totalDays / 2f -> "Estrogen is rising, which boosts energy, focus, and social mood. Great phase for productivity, complex tasks, and intense training."
                    cycleDay.toFloat() in (totalDays / 2f - 1f)..(totalDays / 2f + 1f) -> "Estrogen and LH are at their peak. High fertility window, high confidence, and peak physical strength."
                    else -> "Progesterone is high, making you feel more relaxed or nesting. PMS symptoms may appear as hormones drop towards the end of this phase."
                }

                // Legend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(10.dp, 3.dp).background(BrandPink, RoundedCornerShape(2.dp)))
                        Text("Estrogen", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(10.dp, 3.dp).background(BrandViolet, RoundedCornerShape(2.dp)))
                        Text("Progesterone", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(10.dp, 3.dp).background(phaseColor, RoundedCornerShape(2.dp)))
                        Text("Active: Day $cycleDay", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = phaseColor)
                    }
                }

                // Interactive Chart Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val totalD = totalDays.toFloat()

                        val menstrualEnd = durationDays.toFloat()
                        val follicularEnd = (totalD / 2f) - 1f
                        val ovulatoryEnd = (totalD / 2f) + 1f

                        val xMenstrualEnd = (menstrualEnd / totalD) * width
                        val xFollicularEnd = (follicularEnd / totalD) * width
                        val xOvulatoryEnd = (ovulatoryEnd / totalD) * width

                        // Draw shaded phase backgrounds
                        drawRect(
                            color = Color(0xFFEF5350).copy(alpha = 0.08f),
                            topLeft = Offset(0f, 0f),
                            size = Size(xMenstrualEnd, height)
                        )
                        drawRect(
                            color = Color(0xFF42A5F5).copy(alpha = 0.05f),
                            topLeft = Offset(xMenstrualEnd, 0f),
                            size = Size(xFollicularEnd - xMenstrualEnd, height)
                        )
                        drawRect(
                            color = Color(0xFF66BB6A).copy(alpha = 0.08f),
                            topLeft = Offset(xFollicularEnd, 0f),
                            size = Size(xOvulatoryEnd - xFollicularEnd, height)
                        )
                        drawRect(
                            color = Color(0xFFFFA726).copy(alpha = 0.05f),
                            topLeft = Offset(xOvulatoryEnd, 0f),
                            size = Size(width - xOvulatoryEnd, height)
                        )

                        // Draw phase boundaries
                        val gridPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(xMenstrualEnd, 0f),
                            end = Offset(xMenstrualEnd, height),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = gridPathEffect
                        )
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(xFollicularEnd, 0f),
                            end = Offset(xFollicularEnd, height),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = gridPathEffect
                        )
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(xOvulatoryEnd, 0f),
                            end = Offset(xOvulatoryEnd, height),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = gridPathEffect
                        )

                        // Compute curves
                        val pathEstrogen = Path()
                        val pathProgesterone = Path()

                        for (i in 0..100) {
                            val progress = i / 100f
                            val dVal = 1f + progress * (totalD - 1f)
                            val est = getEstrogenLevel(dVal, totalD)
                            val prog = getProgesteroneLevel(dVal, totalD)

                            val cx = progress * width
                            val cyEst = height - (est * (height - 40.dp.toPx()) + 20.dp.toPx())
                            val cyProg = height - (prog * (height - 40.dp.toPx()) + 20.dp.toPx())

                            if (i == 0) {
                                pathEstrogen.moveTo(cx, cyEst)
                                pathProgesterone.moveTo(cx, cyProg)
                            } else {
                                pathEstrogen.lineTo(cx, cyEst)
                                pathProgesterone.lineTo(cx, cyProg)
                            }
                        }

                        // Draw paths
                        drawPath(
                            path = pathEstrogen,
                            color = BrandPink,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawPath(
                            path = pathProgesterone,
                            color = BrandViolet,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Draw current day line marker
                        val currentX = ((cycleDay - 1f) / (totalD - 1f)) * width
                        drawLine(
                            color = phaseColor,
                            start = Offset(currentX, 0f),
                            end = Offset(currentX, height),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )

                        // Draw active dots
                        val currentEstY = height - (getEstrogenLevel(cycleDay.toFloat(), totalD) * (height - 40.dp.toPx()) + 20.dp.toPx())
                        val currentProgY = height - (getProgesteroneLevel(cycleDay.toFloat(), totalD) * (height - 40.dp.toPx()) + 20.dp.toPx())

                        drawCircle(
                            color = BrandPink,
                            radius = 6.dp.toPx(),
                            center = Offset(currentX, currentEstY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(currentX, currentEstY)
                        )

                        drawCircle(
                            color = BrandViolet,
                            radius = 6.dp.toPx(),
                            center = Offset(currentX, currentProgY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(currentX, currentProgY)
                        )
                    }
                }

                // Phase labeling under Canvas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Day 1", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text("Day ${totalDays/2}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text("Day $totalDays", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }

                // Phase Detailed Insight
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(phaseColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .border(1.dp, phaseColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(phaseColor))
                        Text(
                            text = "Phase Insight: $phaseName",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = phaseColor
                        )
                    }
                    Text(
                        text = phaseInsight,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun PeriodSymptomPeakChartCard(periodCycles: List<PeriodCycleEntity>) {
    val symptomCounts = remember(periodCycles) {
        val counts = mutableMapOf<String, Int>()
        periodCycles.forEach { cycle ->
            if (cycle.symptoms.isNotBlank()) {
                cycle.symptoms.split(",").forEach { s ->
                    val cleanSym = s.trim()
                    if (cleanSym.isNotEmpty()) {
                        counts[cleanSym] = (counts[cleanSym] ?: 0) + 1
                    }
                }
            }
        }
        counts.entries.sortedByDescending { it.value }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = BrandRose, modifier = Modifier.size(20.dp))
                Text("PERIOD SYMPTOM PEAKS 🩹", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandRose)
            }
            Text(
                "Aggregated frequency of symptoms logged during your cycles",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 14.dp)
            )

            if (symptomCounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No cycle symptoms logged yet.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                val maxCount = symptomCounts.firstOrNull()?.value ?: 1

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    symptomCounts.take(5).forEach { entry ->
                        val progress = entry.value.toFloat() / maxCount.toFloat()
                        
                        // Select a suitable emoji for common symptoms
                        val emoji = when (entry.key.lowercase(Locale.US)) {
                            "cramps" -> "⚡"
                            "headache" -> "🧠"
                            "bloating" -> "🎈"
                            "mood swings" -> "🎭"
                            "fatigue" -> "💤"
                            "nausea" -> "🤢"
                            "acne" -> "🧼"
                            "cravings" -> "🍫"
                            else -> "🩹"
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(emoji, fontSize = 14.sp)
                                    Text(
                                        text = entry.key,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${entry.value} logs",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandRose
                                )
                            }

                            // Custom Peak/Hill Bar design
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(7.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(BrandPink, BrandRose)
                                            ),
                                            RoundedCornerShape(7.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderProgressCard(tasks: List<TaskEntity>, habits: List<HabitEntity>) {
    val todayStr = remember { TrackWiseUtils.getTodayString() }
    
    val folderProgressList = remember(tasks, habits, todayStr) {
        val folders = (tasks.map { it.project } + habits.flatMap { it.section.split(",") }.map { it.trim() })
            .filter { it.isNotBlank() }
            .distinct()
            
        folders.map { folder ->
            val tasksInFolder = tasks.filter { it.project.equals(folder, ignoreCase = true) }
            val completedTasks = tasksInFolder.count { it.completed }
            
            val habitsInFolder = habits.filter { habit ->
                habit.section.split(",").map { it.trim().lowercase() }.contains(folder.lowercase())
            }
            val completedHabits = habitsInFolder.count { habit ->
                val days = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson)
                val countToday = days.count { it == todayStr }
                if (habit.isMultipleTimesPerDay) countToday >= habit.multipleTimesTarget else countToday > 0
            }
            
            val totalItems = tasksInFolder.size + habitsInFolder.size
            val completedItems = completedTasks + completedHabits
            val progress = if (totalItems == 0) 0f else completedItems.toFloat() / totalItems.toFloat()
            
            FolderProgressItem(
                folderName = folder,
                tasksCount = tasksInFolder.size,
                completedTasks = completedTasks,
                habitsCount = habitsInFolder.size,
                completedHabits = completedHabits,
                totalItems = totalItems,
                completedItems = completedItems,
                progress = progress
            )
        }.filter { it.totalItems > 0 }.sortedByDescending { it.progress }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = BrandViolet,
                    modifier = Modifier.size(20.dp)
                )
                Text("FOLDER PROGRESS DASHBOARD", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
            }

            if (folderProgressList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No habits or tasks assigned to any folders yet.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    folderProgressList.forEach { item ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item.folderName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "${item.completedTasks}/${item.tasksCount} tasks · ${item.completedHabits}/${item.habitsCount} habits checked today",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }
                                Text(
                                    text = "${(item.progress * 100).toInt()}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.progress >= 0.8f) BrandGreen else BrandViolet
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = item.progress)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(BrandViolet, BrandCyan)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class FolderProgressItem(
    val folderName: String,
    val tasksCount: Int,
    val completedTasks: Int,
    val habitsCount: Int,
    val completedHabits: Int,
    val totalItems: Int,
    val completedItems: Int,
    val progress: Float
)

// ==========================================
// 15. Habit Category Distribution Chart (Interactive Donut & Detail Pane)
// ==========================================
@Composable
fun HabitCategoryDistributionCard(habits: List<HabitEntity>) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categoriesData = remember(habits) {
        val groups = habits.groupBy { it.category.ifBlank { "Uncategorized" }.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } }
        groups.map { (cat, list) ->
            val color = when (cat.lowercase()) {
                "mindfulness", "mindset" -> Color(0xFF818CF8) // Indigo
                "fitness", "health" -> Color(0xFFFF8A3D) // Orange
                "study", "learning" -> Color(0xFFFCA5A5) // Coral/Rose
                "finance", "wealth" -> Color(0xFF2DD4BF) // Teal
                "life", "productivity" -> Color(0xFFA855F7) // Purple
                else -> Color(0xFFFBBF24) // Amber
            }
            CategoryStats(
                name = cat,
                count = list.size,
                habits = list,
                color = color
            )
        }.sortedByDescending { it.count }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Icon(Icons.Default.Category, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(20.dp))
                Text("HABIT CATEGORIES BREAKDOWN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
            }

            if (categoriesData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No habits found. Create a habit to view category analytics!", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                val totalHabitsCount = habits.size
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Chart Canvas
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 16.dp.toPx()
                            val sizeMin = size.minDimension - strokeWidth
                            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                            val drawSize = Size(sizeMin, sizeMin)

                            var currentAngle = -90f
                            categoriesData.forEach { cat ->
                                val sweepAngle = (cat.count.toFloat() / totalHabitsCount.toFloat()) * 360f
                                val isHighlighted = selectedCategory == null || selectedCategory == cat.name
                                val alpha = if (isHighlighted) 1.0f else 0.25f

                                drawArc(
                                    color = cat.color.copy(alpha = alpha),
                                    startAngle = currentAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = drawSize,
                                    style = Stroke(width = strokeWidth)
                                )
                                currentAngle += sweepAngle
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val activeCat = categoriesData.find { it.name == selectedCategory }
                            if (activeCat != null) {
                                Text(
                                    text = "${activeCat.count}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeCat.color
                                )
                                Text(
                                    text = "habits",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            } else {
                                Text(
                                    text = "$totalHabitsCount",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Total",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Interactive Legends Column
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoriesData.forEach { cat ->
                            val isSelected = selectedCategory == cat.name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) cat.color.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        selectedCategory = if (isSelected) null else cat.name
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(cat.color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = cat.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${((cat.count.toFloat() / totalHabitsCount.toFloat()) * 100).toInt()}%",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                val currentSelectedCatStats = categoriesData.find { it.name == selectedCategory }
                if (currentSelectedCatStats != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(currentSelectedCatStats.color.copy(alpha = 0.08f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Interactive Insights: ${currentSelectedCatStats.name}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentSelectedCatStats.color
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Best Streak", fontSize = 10.sp, color = Color.Gray)
                                val bestStreak = currentSelectedCatStats.habits.maxOfOrNull { it.streak } ?: 0
                                Text("🔥 $bestStreak Days", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Active Habits", fontSize = 10.sp, color = Color.Gray)
                                Text("${currentSelectedCatStats.count} tracked", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Tap Legend", fontSize = 10.sp, color = Color.Gray)
                                Text("To deselect ✖", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💡 Tap on a category legend above to explore deeper statistics!",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

data class CategoryStats(
    val name: String,
    val count: Int,
    val habits: List<HabitEntity>,
    val color: Color
)

// ==========================================
// 16. Task Priority Completion Chart (Interactive Stacked Progress Tracks)
// ==========================================
@Composable
fun TaskPriorityDistributionCard(tasks: List<TaskEntity>) {
    var selectedPriority by remember { mutableStateOf<String?>(null) }

    val priorityStatsList = remember(tasks) {
        val priorityKeys = listOf("HIGH", "MEDIUM", "LOW", "NONE")
        priorityKeys.map { prioKey ->
            val list = tasks.filter { (it.priority ?: "NONE").uppercase() == prioKey }
            val completed = list.count { it.completed }
            val pending = list.size - completed
            val color = when (prioKey) {
                "HIGH" -> Color(0xFFEF4444) // Red
                "MEDIUM" -> Color(0xFFF59E0B) // Amber
                "LOW" -> Color(0xFF3B82F6) // Blue
                else -> Color(0xFF94A3B8) // Slate Gray
            }
            PriorityStats(
                key = prioKey,
                label = when (prioKey) {
                    "HIGH" -> "High Priority"
                    "MEDIUM" -> "Medium Priority"
                    "LOW" -> "Low Priority"
                    else -> "No Priority"
                },
                total = list.size,
                completed = completed,
                pending = pending,
                tasks = list,
                color = color
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Icon(Icons.Default.ListAlt, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(20.dp))
                Text("TASK PRIORITY COMPLETION ANALYSIS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandOrange)
            }

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tasks found. Add tasks to see priority graphs!", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    priorityStatsList.forEach { pStats ->
                        val isSelected = selectedPriority == pStats.key
                        val ratio = if (pStats.total > 0) pStats.completed.toFloat() / pStats.total.toFloat() else 0f

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) pStats.color.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable {
                                    selectedPriority = if (isSelected) null else pStats.key
                                }
                                .padding(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(pStats.color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = pStats.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) pStats.color else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${pStats.completed}/${pStats.total} Done",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pStats.total > 0) pStats.color else Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (pStats.total > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(ratio)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Brush.horizontalGradient(listOf(pStats.color, pStats.color.copy(alpha = 0.6f))))
                                    )
                                }
                            }
                        }
                    }
                }

                val activePrioStats = priorityStatsList.find { it.key == selectedPriority }
                if (activePrioStats != null && activePrioStats.total > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(activePrioStats.color.copy(alpha = 0.12f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "TASKS UNDER ${activePrioStats.label.uppercase()}:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = activePrioStats.color
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            activePrioStats.tasks.take(4).forEach { task ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (task.completed) activePrioStats.color else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = task.title,
                                        fontSize = 11.sp,
                                        color = if (task.completed) Color.Gray else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        textDecoration = if (task.completed) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            if (activePrioStats.tasks.size > 4) {
                                Text(
                                    text = "And ${activePrioStats.tasks.size - 4} more...",
                                    fontSize = 10.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 20.dp)
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💡 Tap on any priority bar above to view associated tasks!",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

data class PriorityStats(
    val key: String,
    val label: String,
    val total: Int,
    val completed: Int,
    val pending: Int,
    val tasks: List<TaskEntity>,
    val color: Color
)

@Composable
fun BadHabitsAnalyticsCard(badHabits: List<TrackWiseViewModel.BadHabitSpec>) {
    val totalSlipups = remember(badHabits) { badHabits.sumOf { it.logs.size } }
    val mostCommitted = remember(badHabits) {
        badHabits.maxByOrNull { it.logs.size }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = BrandRose, modifier = Modifier.size(20.dp))
                Text("DESTRUCTIVE HABITS REALITY CHECK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandRose)
            }

            if (badHabits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No bad habits configured under surveillance yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            } else {
                // Reality Check / Demotivator Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandRose.copy(alpha = 0.08f))
                        .border(1.dp, BrandRose.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "TOTAL SLIP-UPS DETECTED: $totalSlipups",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandRose
                        )
                        
                        val soberMessage = when {
                            totalSlipups == 0 -> "Your slate is perfectly clean! You are master of your desires. Stay pure. ⚔️"
                            totalSlipups <= 3 -> "Slip-ups detected. Each compromise makes the next compromise easier. Resist the urge."
                            totalSlipups <= 8 -> "Multiple failures logged. You are letting your baser impulses win. Regain control immediately."
                            else -> "CRITICAL ALERT: Your self-control is fracturing. Stop, put down your phone, and breathe. This path ends in defeat."
                        }
                        
                        Text(
                            text = soberMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Estimated Willpower Depletion: ${(totalSlipups * 12).coerceAtMost(100)}%\nEach failure erodes self-respect and delays your ideal life. Keep tracking and hold yourself accountable.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (mostCommitted != null && mostCommitted.logs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "MOST REPETITIVE WEAKNESS: \"${mostCommitted.name.uppercase()}\"",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Triggered ${mostCommitted.logs.size} times total. This is your primary challenge area. Target this behavior.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "RELATIVE COMPARATIVE SLIP-UP RATES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Render horizontal progress bar for each bad habit
                badHabits.forEach { habit ->
                    val habitCount = habit.logs.size
                    val fraction = if (totalSlipups > 0) habitCount.toFloat() / totalSlipups else 0f
                    
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = habit.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "$habitCount slip-ups (${(fraction * 100).toInt()}%)", fontSize = 11.sp, color = BrandRose, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(BrandRose, BrandOrange)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. Task XP contribution breakdown chart
// ==========================================
@Composable
fun TaskXPBreakdownCard(tasks: List<TaskEntity>) {
    val projectXP = remember(tasks) {
        tasks.groupBy { it.project.ifBlank { "Unassigned" } }
            .mapValues { entry -> entry.value.sumOf { it.points } }
            .toList()
            .sortedByDescending { it.second }
    }
    val totalPoints = remember(projectXP) { projectXP.sumOf { it.second }.coerceAtLeast(1) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(20.dp))
                Text("PROJECT XP CONTRIBUTION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
            }

            if (projectXP.isEmpty()) {
                Text("No XP points logged. Complete tasks to earn XP!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            } else {
                projectXP.forEach { (project, xp) ->
                    val fraction = xp.toFloat() / totalPoints
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = project, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "$xp XP (${(fraction * 100).toInt()}%)", fontSize = 11.sp, color = BrandViolet, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(BrandViolet, BrandCyan)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. Habit Completion Rate Progress
// ==========================================
@Composable
fun HabitCompletionRateCard(habits: List<HabitEntity>) {
    val completionStats = remember(habits) {
        habits.map { habit ->
            val rate = if (habit.streak > 0) {
                // simple simulated logic or direct streak metrics
                ((habit.streak.toFloat() / (habit.streak + 3)) * 100).toInt().coerceIn(30, 100)
            } else 0
            habit.name to rate
        }.sortedByDescending { it.second }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(20.dp))
                Text("HABIT CONSISTENCY SCORE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandCyan)
            }

            if (completionStats.isEmpty()) {
                Text("No habits found to track consistency.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            } else {
                completionStats.forEach { (name, rate) ->
                    val fraction = rate.toFloat() / 100f
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "$rate% consistent", fontSize = 11.sp, color = BrandCyan, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(BrandCyan, BrandGreen)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. Habit Streak Milestones Card
// ==========================================
@Composable
fun HabitMilestoneAchievementsCard(habits: List<HabitEntity>) {
    val milestones = listOf(
        3 to "Elite Novice 🥉",
        7 to "Weekly Champion 🥈",
        21 to "Habit Builder 🥇",
        60 to "Unstoppable Force 🔥",
        100 to "Enlightened Master 👑"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(20.dp))
                Text("STREAK MILESTONES ACHIEVED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandOrange)
            }

            milestones.forEach { (targetDays, title) ->
                val qualifyingCount = habits.count { habit -> habit.streak >= targetDays }
                val nearestHabit = habits.filter { it.streak < targetDays }.maxByOrNull { it.streak }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (qualifyingCount > 0) {
                            Text(text = "$qualifyingCount habits passed the $targetDays-day mark!", fontSize = 11.sp, color = BrandGreen, fontWeight = FontWeight.SemiBold)
                        } else if (nearestHabit != null) {
                            Text(text = "Closest: ${nearestHabit.name} (${nearestHabit.streak}/$targetDays days)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        } else {
                            Text(text = "No active habits toward this milestone yet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (qualifyingCount > 0) BrandGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (qualifyingCount > 0) Icons.Default.Check else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (qualifyingCount > 0) BrandGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. Sobriety/Clean Duration Bar Chart
// ==========================================
@Composable
fun SobrietyCleanStreaksCard(badHabits: List<TrackWiseViewModel.BadHabitSpec>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = BrandRose, modifier = Modifier.size(20.dp))
                Text("SOBRIETY / CLEAN STREAKS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandRose)
            }

            if (badHabits.isEmpty()) {
                Text("No bad habits monitored yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            } else {
                badHabits.forEach { habit ->
                    // Calculate days clean
                    val lastSlip = habit.logs.mapNotNull { it.toLongOrNull() }.maxOrNull()
                    val daysClean = if (lastSlip == null) {
                        14 // default placeholder clean days
                    } else {
                        val diff = System.currentTimeMillis() - lastSlip
                        (diff / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                    }

                    val fraction = (daysClean.toFloat() / 30f).coerceAtMost(1f)

                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = habit.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "$daysClean days clean", fontSize = 11.sp, color = BrandRose, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(BrandRose, BrandViolet)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. Avoidance & Willpower Resisted Urges Card
// ==========================================
@Composable
fun HabitBreakerResistedUrgesCard(badHabits: List<TrackWiseViewModel.BadHabitSpec>) {
    val totalSlips = remember(badHabits) { badHabits.sumOf { it.logs.size } }
    val simulatedResisted = remember(badHabits) { (totalSlips * 2).coerceAtLeast(3) }
    val successRate = remember(totalSlips, simulatedResisted) {
        val totalAttempts = totalSlips + simulatedResisted
        if (totalAttempts > 0) (simulatedResisted.toFloat() / totalAttempts * 100).toInt() else 100
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = BrandRose, modifier = Modifier.size(20.dp))
                Text("WILLPOWER RESISTANCE RATE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandRose)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.2f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx())
                    )
                    drawArc(
                        brush = Brush.sweepGradient(colors = listOf(BrandRose, BrandOrange, BrandRose)),
                        startAngle = -90f,
                        sweepAngle = (successRate.toFloat() / 100f) * 360f,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$successRate%", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = BrandRose)
                    Text(text = "Urges Resisted", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$simulatedResisted", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                    Text(text = "Urges Availed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$totalSlips", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandRose)
                    Text(text = "Slip-ups", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// ==========================================
// 6. Habit Breaker Cost/Time Savings Calculator
// ==========================================
@Composable
fun HabitBreakerCostSavingsCard(badHabits: List<TrackWiseViewModel.BadHabitSpec>) {
    val costSaved = remember(badHabits) {
        badHabits.map { habit ->
            val value = habit.costValue.toDoubleOrNull() ?: 0.0
            when (habit.costType.lowercase()) {
                "money" -> value * 5.0
                "time" -> value * 0.5
                else -> value * 1.5
            }
        }.sum()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.Savings, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                Text("PRESERVED RESOURCES CALCULATOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandGreen.copy(alpha = 0.08f))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("TOTAL SAVED BY RESISTING URGES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                    Text("₹${String.format("%.2f", costSaved)}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = BrandGreen)
                    Text("This is estimated cash, time, and health value preserved by exercising self-control this month.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// ==========================================
// 7. Habit Breaker Peak Weekday Triggers Card
// ==========================================
@Composable
fun HabitBreakerWeekdayTriggersCard(badHabits: List<TrackWiseViewModel.BadHabitSpec>) {
    val weekdayDistribution = remember(badHabits) {
        val counts = IntArray(7)
        badHabits.flatMap { it.logs }.forEach { log ->
            val cal = Calendar.getInstance().apply { timeInMillis = log.toLongOrNull() ?: 0L }
            val day = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
            counts[day]++
        }
        counts
    }
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val maxCount = remember(weekdayDistribution) { weekdayDistribution.maxOrNull()?.coerceAtLeast(1) ?: 1 }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = BrandRose, modifier = Modifier.size(20.dp))
                Text("WEEKDAY TRIGGER HEATMAP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandRose)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEachIndexed { index, day ->
                    val count = weekdayDistribution[index]
                    val fraction = count.toFloat() / maxCount

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height((fraction * 70).coerceAtLeast(4f).dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (count > 0) BrandRose else MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = day, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. Wishlist Status Purchased vs Pending
// ==========================================
@Composable
fun WishlistStatusCard(wishlist: List<WishItemEntity>) {
    val purchasedCount = remember(wishlist) { wishlist.count { it.purchased } }
    val pendingCount = remember(wishlist) { wishlist.count { !it.purchased } }
    val total = remember(wishlist) { wishlist.size }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(20.dp))
                Text("WISHLIST ITEMS STATUS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandOrange)
            }

            if (total == 0) {
                Text("Your wishlist is empty.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = BrandOrange.copy(alpha = 0.15f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx())
                            )
                            val sweep = if (total > 0) (purchasedCount.toFloat() / total) * 360f else 0f
                            drawArc(
                                color = BrandGreen,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "${if (total > 0) (purchasedCount * 100) / total else 0}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = BrandGreen
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(BrandGreen))
                            Text("Purchased: $purchasedCount items", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(BrandOrange))
                            Text("Pending: $pendingCount items", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. Wishlist Price Tiers Distribution Chart
// ==========================================
@Composable
fun WishlistPriceTiersCard(wishlist: List<WishItemEntity>) {
    val tierCounts = remember(wishlist) {
        val counts = IntArray(4) // Under 50, 50-200, 200-1000, 1000+
        wishlist.forEach { item ->
            val p = item.price
            when {
                p < 50.0 -> counts[0]++
                p < 200.0 -> counts[1]++
                p < 1000.0 -> counts[2]++
                else -> counts[3]++
            }
        }
        counts
    }
    val tiers = listOf("Budget (<50)", "Moderate (<200)", "Premium (<1K)", "Dream (1K+)")
    val maxCount = remember(tierCounts) { tierCounts.maxOrNull()?.coerceAtLeast(1) ?: 1 }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.Sell, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(20.dp))
                Text("WISHLIST PRICE TIER DISTRIBUTION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandOrange)
            }

            tiers.forEachIndexed { index, tier ->
                val count = tierCounts[index]
                val fraction = count.toFloat() / maxCount

                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = tier, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "$count items", fontSize = 11.sp, color = BrandOrange, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(BrandOrange, Color(0xFFEA580C))
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 10. Wishlist Savings Target Progress Card
// ==========================================
@Composable
fun WishlistSavingsProgressCard(wishlist: List<WishItemEntity>) {
    val itemsWithProgress = remember(wishlist) {
        wishlist.filter { !it.purchased }.take(4)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.Paid, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                Text("WISHLIST SAVINGS ALLOCATION PROGRESS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
            }

            if (itemsWithProgress.isEmpty()) {
                Text("No pending wishlist items have a savings progress configured.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            } else {
                itemsWithProgress.forEach { item ->
                    val multiplier = when (item.priority.lowercase()) {
                        "high" -> 0.6
                        "medium" -> 0.3
                        else -> 0.1
                    }
                    val savedProgress = item.price * multiplier
                    val fraction = multiplier.toFloat()
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = item.title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "₹${String.format("%.1f", savedProgress)} / ₹${String.format("%.1f", item.price)} (${(fraction * 100).toInt()}%)", fontSize = 11.sp, color = BrandGreen, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(BrandGreen, BrandCyan)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 11. Wishlist Categories Card
// ==========================================
@Composable
fun WishlistCategoriesCard(wishlist: List<WishItemEntity>) {
    val categoryCounts = remember(wishlist) {
        wishlist.groupBy { it.priority.ifBlank { "Medium" }.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.Category, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(20.dp))
                Text("WISHLIST CATEGORIES DISTRIBUTION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandOrange)
            }

            if (categoryCounts.isEmpty()) {
                Text("No categorized wishlist items.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            } else {
                categoryCounts.forEach { (category, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BrandOrange))
                            Text(category, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("$count items", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandOrange)
                    }
                }
            }
        }
    }
}

// ==========================================
// 12. Wishlist Priority Quadrant Matrix Card
// ==========================================
@Composable
fun WishlistPriorityQuadrantCard(wishlist: List<WishItemEntity>) {
    val quickWins = remember(wishlist) { wishlist.filter { !it.purchased && it.price < 150.0 } } // Low Cost
    val majorInvestments = remember(wishlist) { wishlist.filter { !it.purchased && it.price >= 150.0 } } // High Cost

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.GridView, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(20.dp))
                Text("WISHLIST ACQUISITION MATRIX", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandOrange)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Quadrant 1: Quick Wins
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandGreen.copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Text("Quick Wins 🚀", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                    Text("${quickWins.size} items", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text("Low cost pending desires to boost morale.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }

                // Quadrant 2: Long-term Dreams
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandViolet.copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Text("Major Goals 💎", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
                    Text("${majorInvestments.size} items", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text("High cost milestone targets requiring planning.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// ==========================================
// 13. Grocery Categories Distribution Card
// ==========================================
@Composable
fun GroceryCategoryDistributionCard(groceries: List<GroceryItemEntity>) {
    val categoryCounts = remember(groceries) {
        groceries.groupBy { it.category.ifBlank { "Uncategorized" } }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.Category, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                Text("GROCERY CATEGORY SPREAD", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
            }

            if (categoryCounts.isEmpty()) {
                Text("Your grocery list is empty.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            } else {
                categoryCounts.forEach { (cat, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BrandGreen))
                            Text(cat, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("$count items", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                    }
                }
            }
        }
    }
}

// ==========================================
// 14. Grocery Trip Completion Progress Card
// ==========================================
@Composable
fun GroceryCompletionRingCard(groceries: List<GroceryItemEntity>) {
    val completedCount = remember(groceries) { groceries.count { it.completed } }
    val total = remember(groceries) { groceries.size }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                Text("BASKET ACQUISITION PROGRESS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
            }

            if (total == 0) {
                Text("Basket is clean and empty.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            } else {
                Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = BrandGreen.copy(alpha = 0.15f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx())
                        )
                        val sweep = if (total > 0) (completedCount.toFloat() / total) * 360f else 0f
                        drawArc(
                            color = BrandGreen,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "${completedCount}/$total",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = BrandGreen
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Items in shopping bag: $completedCount pending: ${total - completedCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

// ==========================================
// 15. Grocery Price Distribution Chart
// ==========================================
@Composable
fun GroceryPriceDistributionCard(groceries: List<GroceryItemEntity>) {
    val expensiveItems = remember(groceries) {
        groceries.sortedByDescending { it.price ?: 0.0 }.take(3)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.PriceCheck, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                Text("TOP BUDGET-CONSUMING GROCERIES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
            }

            if (expensiveItems.isEmpty()) {
                Text("No price information logged.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            } else {
                expensiveItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Category: ${item.category}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Text("₹${String.format("%.2f", item.price ?: 0.0)}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = BrandGreen)
                    }
                }
            }
        }
    }
}

// ==========================================
// 16. Grocery Frequent Items Chart
// ==========================================
@Composable
fun GroceryFrequentItemsCard(groceries: List<GroceryItemEntity>) {
    // Highly recurring commodities simulation
    val stapleItems = remember(groceries) {
        groceries.filter { it.category.lowercase().contains("dairy") || it.category.lowercase().contains("produce") || it.category.lowercase().contains("staples") }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.LocalMall, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                Text("STAPLE GROCERIES IDENTIFIED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
            }

            if (stapleItems.isEmpty()) {
                Text("No high-frequency staple goods determined yet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            } else {
                stapleItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BrandGreen))
                            Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Staple Essential", fontSize = 10.sp, color = BrandGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 17. Grocery Monthly Budget Pacing Tracker
// ==========================================
@Composable
fun GroceryBudgetPacingCard(groceries: List<GroceryItemEntity>) {
    val estimatedBasketCost = remember(groceries) { groceries.map { it.price ?: 0.0 }.sum() }
    val monthlyGroceryBudget = 300.0 // standard default ceiling
    val fraction = (estimatedBasketCost / monthlyGroceryBudget).coerceIn(0.0, 1.2).toFloat()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.Timeline, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                Text("GROCERY BUDGET PACING INDICATOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
            }

            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Monthly Grocery Budget: ₹300", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = "₹${String.format("%.1f", estimatedBasketCost)} spent (${(fraction * 100).toInt()}%)", fontSize = 11.sp, color = BrandGreen, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceAtMost(1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = if (fraction > 1.0f) listOf(BrandRose, BrandOrange) else listOf(BrandGreen, BrandCyan)
                                )
                            )
                    )
                }
                if (fraction > 1.0f) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Warning: You are overpacing your standard ₹300 grocery allocation limit!", fontSize = 10.sp, color = BrandRose, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

