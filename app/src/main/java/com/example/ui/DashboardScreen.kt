package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HabitEntity
import com.example.data.TaskEntity
import com.example.data.WishItemEntity
import com.example.data.WaterLogEntity
import com.example.ui.theme.*
import com.example.utils.TrackWiseUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

private fun isTaskDueTimeEnded(task: com.example.data.TaskEntity, todayStr: String): Boolean {
    if (task.completed) return false
    if (task.deadline < todayStr) {
        return true
    }
    if (task.deadline > todayStr) return false
    val dTime = if (!task.dueTime.isNullOrBlank()) task.dueTime else task.reminderTime
    if (!dTime.isNullOrBlank()) {
        try {
            val nowTimeStr = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            return nowTimeStr > dTime
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return false
}

@Composable
fun DashboardScreen(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit = {}
) {
    val currentUser by viewModel.sessionUser.collectAsState()
    val todayScore by viewModel.todayScore.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()
    val allWishlist by viewModel.allWishlist.collectAsState()
    val badHabits by viewModel.badHabits.collectAsState()
    val allFinanceLogs by viewModel.allFinanceLogs.collectAsState()
    val allBirthdays by viewModel.allBirthdays.collectAsState()
    val friendConnections by viewModel.friendConnections.collectAsState()
    val allAlarms by viewModel.allAlarms.collectAsState()
    val waterLogs by viewModel.waterLogs.collectAsState()
    val streakHistory by viewModel.streakHistory.collectAsState()

    val reminderWishlist = remember(allWishlist) {
        allWishlist.filter { !it.purchased && (it.remindMe || !it.reminderDate.isNullOrEmpty()) }
    }

    var activePostponeTask by remember { mutableStateOf<TaskEntity?>(null) }

    val todayStr = TrackWiseUtils.getTodayString()
    val isPreLaunch = TrackWiseUtils.isBeforeLaunch(todayStr)

    val todayFocusItems = remember(allTasks, todayStr) {
        allTasks.filter { 
            !it.notes.contains("[ARCHIVED]") &&
            !it.completed &&
            TrackWiseUtils.shouldShowTaskOnDate(it, todayStr)
        }
        .sortedWith(compareBy<TaskEntity> { !it.notes.contains("[PINNED]") }
            .thenBy { it.reminderTime == null }
            .thenBy { it.reminderTime ?: "" }
            .thenBy { it.title }
        )
    }

    val priorityAndOverdueItems = remember(allTasks, todayStr) {
        allTasks.filter {
            !it.notes.contains("[ARCHIVED]") &&
            !it.completed &&
            (it.deadline < todayStr || it.priority == "high")
        }
        .sortedWith(compareBy<TaskEntity> { !it.notes.contains("[PINNED]") }
            .thenBy { it.deadline }
            .thenByDescending { it.priority == "high" }
            .thenBy { it.title }
        )
    }

    val combinedPriorityAndTodayItems = remember(todayFocusItems, priorityAndOverdueItems) {
        (todayFocusItems + priorityAndOverdueItems).distinctBy { it.id }
    }

    val name = currentUser?.fullName?.split(" ")?.firstOrNull() ?: "there"

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // --- Pre-Launch Banner (Part 1.5 & Part 6.1) ---
        if (isPreLaunch) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .border(1.dp, BrandCyan, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "TrackWise Launches 2026-07-01 🚀",
                            color = BrandCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Streaks, daily scores, and habit tracking begin on 2026-07-01. Your tasks are ready — complete them starting July 1st.",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
        // --- Welcome Header Section ---
        item {
            StaggeredItem(index = 0) {
                var currentTimeState by remember { mutableStateOf(SimpleDateFormat("EEEE, dd MMMM yyyy  •  hh:mm a", Locale.US).format(Date())) }
                LaunchedEffect(Unit) {
                    while (true) {
                        currentTimeState = SimpleDateFormat("EEEE, dd MMMM yyyy  •  hh:mm a", Locale.US).format(Date())
                        kotlinx.coroutines.delay(10000)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "$greeting, $name!",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Current Date & Time",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = currentTimeState,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Text(
                        text = "Your analytics, today's focus, and priority runways at a glance.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // --- Level & XP Mastery Progress Card ---
        item {
            StaggeredItem(index = 2) {
                val streakHistory by viewModel.streakHistory.collectAsState()
                val totalXP = remember(streakHistory, todayScore) {
                    streakHistory.sumOf { it.score } + todayScore
                }
                val userLevel = (totalXP / 1000) + 1
                val currentLevelXP = totalXP % 1000
                val levelProgressFraction = (currentLevelXP / 1000f).coerceIn(0f, 1f)
                val levelTitle = when {
                    userLevel <= 1 -> "Novice Focused"
                    userLevel <= 3 -> "Rising Explorer"
                    userLevel <= 5 -> "Habit Architect"
                    userLevel <= 8 -> "Productivity Strategist"
                    userLevel <= 12 -> "Master Motivator"
                    else -> "Grandmaster Legend"
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            BrandViolet.copy(alpha = 0.5f),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(BrandViolet, BrandPink)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$userLevel",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }

                            Column {
                                Text(
                                    text = "LEVEL $userLevel",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandViolet,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = levelTitle,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Level Progress: $currentLevelXP / 1000 XP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${(levelProgressFraction * 100).roundToInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandViolet
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { levelProgressFraction },
                        color = BrandViolet,
                        trackColor = BrandViolet.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }

        // --- 4 Stat Tiles Grid (XP Gained, Level, Streak, Tasks) ---
        item {
            StaggeredItem(index = 3) {
                val streakHistory by viewModel.streakHistory.collectAsState()
                val totalXP = remember(streakHistory, todayScore) {
                    streakHistory.sumOf { it.score } + todayScore
                }
                val userLevel = (totalXP / 1000) + 1
                val totalTasksCompleted = allTasks.count { it.completed }
            val maxHabitStreak = allHabits.maxOfOrNull { it.streak } ?: 0

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // XP Gained Stat
                    StatTile(
                        label = "XP GAINED",
                        value = "+$todayScore",
                        color = BrandViolet,
                        icon = Icons.Default.Bolt,
                        modifier = Modifier.weight(1f)
                    )
                    // Level Stat
                    StatTile(
                        label = "LEVEL",
                        value = "Lvl $userLevel",
                        color = BrandOrange,
                        icon = Icons.Default.MilitaryTech,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Streak Stat
                    StatTile(
                        label = "STREAK",
                        value = "${maxHabitStreak}d",
                        color = BrandCyan,
                        icon = Icons.Default.LocalFireDepartment,
                        modifier = Modifier.weight(1f)
                    )
                    // Tasks Stat
                    StatTile(
                        label = "TASKS",
                        value = "$totalTasksCompleted",
                        color = BrandPink,
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

        // --- Daily Scores Overview Widget ---
        item {
            StaggeredItem(index = 4) {
                DailyScoresOverviewWidget(viewModel = viewModel)
            }
        }

        // --- Monthly Finance Summary Widget ---
        item {
            StaggeredItem(index = 5) {
                DashboardFinanceSummaryWidget(
                    financeLogs = allFinanceLogs,
                    onNavigate = onNavigate
                )
            }
        }

        // --- Occasions Countdown Widget (<= 3 Days Left) ---
        item {
            StaggeredItem(index = 6) {
                DashboardOccasionsCountdownWidget(
                    birthdays = allBirthdays,
                    onNavigate = { target ->
                        if (target == "countdown") {
                            viewModel.setWorkspaceSubTab(3)
                            onNavigate("workspace")
                        } else {
                            onNavigate(target)
                        }
                    }
                )
            }
        }

        // --- Habit Streaks Widget ---
        item {
            StaggeredItem(index = 7) {
                HabitStreaksWidget(allHabits = allHabits)
            }
        }

        // --- Water Intake Widget (Hydration Monitor) ---
        item {
            StaggeredItem(index = 8) {
                val waterLogs by viewModel.waterLogs.collectAsState()
                WaterIntakeWidget(viewModel = viewModel, waterLogs = waterLogs)
            }
        }

        // --- Daily Habits Widget (Daily Habit Runways - BELOW Hydration) ---
        item {
            StaggeredItem(index = 9) {
                DailyHabitsWidget(
                    habits = allHabits,
                    onToggleHabit = { viewModel.toggleHabitToday(it) },
                    onHabitClick = { viewModel.setActiveDetailHabit(it) },
                    onAddHabit = { viewModel.openHabitCreationSheet() }
                )
            }
        }

        // --- Habit Breaker Chart Widget (Habit Breaker Insights - BELOW Hydration) ---
        item {
            StaggeredItem(index = 10) {
                DashboardHabitBreakerChartWidget(
                    badHabits = badHabits,
                    onNavigate = onNavigate
                )
            }
        }

        // --- Priority & Overdue Runway (includes Today's Focus) ---
        item {
            StaggeredItem(index = 12) {
                PriorityItemsWidget(
                    tasks = combinedPriorityAndTodayItems,
                    onToggleTask = { viewModel.toggleTaskCompletion(it) },
                    onDeleteTask = { viewModel.deleteTask(it.id) },
                    onArchiveTask = { viewModel.updateTask(it.copy(notes = it.notes + "[ARCHIVED]")) },
                    onPinTask = { viewModel.updateTask(it.copy(notes = if (it.notes.contains("[PINNED]")) it.notes.replace("[PINNED]", "") else it.notes + "[PINNED]")) },
                    onPostponeTask = { activePostponeTask = it }
                )
            }
        }

        // --- Wishlist Reminders Widget ---
        if (reminderWishlist.isNotEmpty()) {
            item {
                StaggeredItem(index = 13) {
                    DashboardWishlistRemindersWidget(
                        wishlistItems = reminderWishlist,
                        onTogglePurchased = { viewModel.toggleWishPurchased(it) },
                        onNavigate = onNavigate
                    )
                }
            }
        }

        // --- Badges & Achievements Mastery Hall Board ---
        item {
            StaggeredItem(index = 14) {
                DashboardBadgesAndAchievementsHallWidget(
                    allHabits = allHabits,
                    allTasks = allTasks,
                    allFinanceLogs = allFinanceLogs,
                    allWishlist = allWishlist,
                    allBirthdays = allBirthdays,
                    badHabits = badHabits,
                    socialCircleSize = friendConnections.size,
                    hasActiveAlarm = allAlarms.any { it.isEnabled },
                    alarmsCount = allAlarms.size,
                    waterLogs = waterLogs,
                    streakHistory = streakHistory,
                    userLevel = (streakHistory.sumOf { it.score } + todayScore) / 1000 + 1,
                    todayScore = todayScore,
                    onNavigate = onNavigate
                )
            }
        }
    }

    activePostponeTask?.let { task ->
        PostponeTaskDialog(
            task = task,
            onDismiss = { activePostponeTask = null },
            onReschedule = { newDate, newTime ->
                val updatedTask = task.copy(deadline = newDate, reminderTime = newTime, completed = false)
                viewModel.updateTask(updatedTask)
                viewModel.addNotification("Task Postponed", "Rescheduled \"${task.title}\" to $newDate")
            },
            onSkipReoccurrence = {
                val cal = Calendar.getInstance()
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val tomorrow = sdf.format(cal.time)
                val updatedTask = task.copy(deadline = tomorrow, startDate = tomorrow, completed = false)
                viewModel.updateTask(updatedTask)
                viewModel.addNotification("Reoccurrence Skipped", "Skipped next session of \"${task.title}\"")
            }
        )
    }


}

@Composable
fun StatTile(
    label: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.border(
            1.dp,
            color.copy(alpha = 0.5f),
            RoundedCornerShape(20.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                letterSpacing = 1.2.sp
            )
        }
    }
}

@Composable
fun DailyScoresOverviewWidget(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    val streakHistory by viewModel.streakHistory.collectAsState()
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "DAILY XP OVERVIEW",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrandViolet,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (streakHistory.isEmpty()) {
                EmptyProgressPlaceholder("Complete tasks and habits to populate historical analytics charts.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Daily XP Gained (Last 7 Days)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    streakHistory.take(7).reversed().forEach { history ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = history.date.substring(5), // MM-DD
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(48.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                val fillWidthFraction = (history.score / 30f).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fillWidthFraction)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(BrandViolet, BrandPink)
                                            )
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${history.score} XP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPink
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getHabitCompletionPercentage(habit: com.example.data.HabitEntity): Int {
    val completedDays = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson)
    if (completedDays.isEmpty()) return 0
    val today = Date()
    val cal = Calendar.getInstance()
    var totalDaysCount = 0
    var completedCount = 0
    for (i in 0 until 30) {
        cal.time = today
        cal.add(Calendar.DAY_OF_YEAR, -i)
        val dateStr = TrackWiseUtils.formatDate(cal.time)
        val sDate = (habit.startDate ?: habit.createdAt).take(10)
        if (dateStr >= sDate) {
            totalDaysCount++
            if (completedDays.contains(dateStr)) {
                completedCount++
            }
        }
    }
    if (totalDaysCount == 0) return 0
    return ((completedCount.toFloat() / totalDaysCount.toFloat()) * 100).toInt().coerceIn(0, 100)
}

private fun getHabitColor(index: Int, category: String): Color {
    val categoryLower = category.lowercase()
    return when {
        categoryLower.contains("exercise") || categoryLower.contains("fitness") || categoryLower.contains("run") -> Color(0xFF1976D2)
        categoryLower.contains("read") || categoryLower.contains("learn") || categoryLower.contains("book") -> Color(0xFFFF8F00)
        categoryLower.contains("water") || categoryLower.contains("hydration") || categoryLower.contains("drink") -> Color(0xFF2E7D32)
        categoryLower.contains("meditation") || categoryLower.contains("wellness") || categoryLower.contains("mind") -> Color(0xFFD32F2F)
        categoryLower.contains("health") || categoryLower.contains("diet") || categoryLower.contains("eat") -> Color(0xFF8E24AA)
        else -> {
            val colors = listOf(
                Color(0xFF1976D2),
                Color(0xFFFF8F00),
                Color(0xFF2E7D32),
                Color(0xFFD32F2F),
                Color(0xFF8E24AA),
                Color(0xFF8D6E63)
            )
            colors[index % colors.size]
        }
    }
}

@Composable
fun SegmentedDonutChart(
    percentage: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val numSegments = 20
            val strokeWidth = 6.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val radius = diameter / 2f
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            
            val gapAngle = 3.5f
            val totalGapAngle = numSegments * gapAngle
            val availableAngle = 360f - totalGapAngle
            val segmentAngle = availableAngle / numSegments
            
            for (i in 0 until numSegments) {
                val startAngle = -90f + i * (segmentAngle + gapAngle) + gapAngle / 2f
                val isFilled = (i.toFloat() / numSegments.toFloat() * 100) < percentage
                val segmentColor = if (isFilled) color else Color(0xFFE0E0E0).copy(alpha = 0.5f)
                
                drawArc(
                    color = segmentColor,
                    startAngle = startAngle,
                    sweepAngle = segmentAngle,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(diameter, diameter),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Butt)
                )
            }
        }
        
        Text(
            text = "$percentage",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun HabitStreaksWidget(
    allHabits: List<HabitEntity>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "HABIT STREAK TRAJECTORIES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrandOrange,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (allHabits.isEmpty()) {
                EmptyProgressPlaceholder("Create habits in the Workspace tab to view streak trajectories.")
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allHabits) { habit ->
                        val percentage = getHabitCompletionPercentage(habit)
                        val color = getHabitColor(allHabits.indexOf(habit), habit.category)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .width(115.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                SegmentedDonutChart(
                                    percentage = percentage,
                                    color = color,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = habit.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = habit.category,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = BrandOrange,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "${habit.streak}d",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandOrange,
                                        modifier = Modifier.padding(start = 2.dp)
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

@Composable
fun WaterIntakeWidget(
    viewModel: TrackWiseViewModel,
    waterLogs: List<WaterLogEntity>,
    modifier: Modifier = Modifier
) {
    val todayStr = TrackWiseUtils.getTodayString()
    val todayWater = waterLogs.firstOrNull { it.date == todayStr }
    val waterGlasses = todayWater?.glasses ?: 0
    val waterGoal = todayWater?.goal ?: 8
    val waterFraction = (waterGlasses.toFloat() / waterGoal).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DAILY HYDRATION MONITOR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandCyan,
                    letterSpacing = 1.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.adjustWaterLog(-1) },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrement water", tint = BrandCyan, modifier = Modifier.size(14.dp))
                    }
                    IconButton(
                        onClick = { viewModel.adjustWaterLog(1) },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increment water", tint = BrandCyan, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Water Intake Today 💧",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$waterGlasses/$waterGoal glasses",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = BrandCyan
                    )
                }
                
                // Beautiful interactive drops
                val displayGoal = if (waterGoal in 1..10) waterGoal else 8
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    for (i in 1..displayGoal) {
                        val isFilled = i <= waterGlasses
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isFilled) BrandCyan.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isFilled) BrandCyan.copy(alpha = 0.35f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    val diff = i - waterGlasses
                                    viewModel.adjustWaterLog(diff)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "Glass $i",
                                tint = if (isFilled) BrandCyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(waterFraction)
                            .clip(RoundedCornerShape(3.dp))
                            .background(BrandCyan)
                    )
                }
            }
        }
    }
}



@Composable
fun EmptyProgressPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun TodayItemsWidget(
    tasks: List<TaskEntity>,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onArchiveTask: (TaskEntity) -> Unit,
    onPinTask: (TaskEntity) -> Unit,
    onPostponeTask: (TaskEntity) -> Unit,
    onAddTask: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(BrandCyan.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = BrandCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "TODAY'S FOCUS RUNWAY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandCyan,
                    modifier = Modifier.padding(start = 10.dp),
                    letterSpacing = 1.sp
                )
            }

            if (tasks.isEmpty()) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "All caught up for today! Add tasks with today's deadline in the Workspace tab.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    tasks.sortedBy { it.completed }.forEach { task ->
                        SwipeableTaskItem(
                            task = task,
                            onToggleTask = { onToggleTask(task) },
                            onDeleteTask = { onDeleteTask(task) },
                            onArchiveTask = { onArchiveTask(task) },
                            onPinTask = { onPinTask(task) },
                            onPostponeTask = { onPostponeTask(task) }
                        ) {
                            val isDark = MaterialTheme.colorScheme.onBackground.red > 0.5f
                            val gradientBrush = if (task.completed) {
                                Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                            } else {
                                when (task.priority.lowercase()) {
                                    "high" -> if (isDark) {
                                        Brush.linearGradient(colors = listOf(Color(0xFF4C0519), Color(0xFF881337)))
                                    } else {
                                        Brush.linearGradient(colors = listOf(Color(0xFFFFF1F2), Color(0xFFFFD1D3)))
                                    }
                                    "medium" -> if (isDark) {
                                        Brush.linearGradient(colors = listOf(Color(0xFF431407), Color(0xFF7C2D12)))
                                    } else {
                                        Brush.linearGradient(colors = listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5)))
                                    }
                                    "low" -> if (isDark) {
                                        Brush.linearGradient(colors = listOf(Color(0xFF172554), Color(0xFF1E3A8A)))
                                    } else {
                                        Brush.linearGradient(colors = listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE)))
                                    }
                                    else -> if (isDark) {
                                        Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
                                    } else {
                                        Brush.linearGradient(colors = listOf(Color.White, Color(0xFFF9FAFB)))
                                    }
                                }
                            }
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(gradientBrush, RoundedCornerShape(16.dp))
                                    .border(
                                        1.dp,
                                        if (task.completed) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onToggleTask(task) }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Header Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = task.title,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                                                color = if (task.completed) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onBackground,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${task.project} · ${task.priority.uppercase()}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // XP capsule tag (Amanah style)
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (task.completed) BrandGreen.copy(alpha = 0.1f)
                                                                else BrandCyan.copy(alpha = 0.12f)
                                            ),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "+${task.points} XP",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (task.completed) BrandGreen else BrandCyan,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Subtle separator line inside card
                                    Spacer(
                                        modifier = Modifier
                                            .padding(vertical = 10.dp)
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                    )

                                    // Footer Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left: Calendar indicator / reminder
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = if (task.reminderTime != null) "Today at ${task.reminderTime}" else "Today",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                                modifier = Modifier.padding(start = 4.dp)
                                            )
                                        }

                                        // Right: Complete Toggle Circle button
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .border(
                                                    2.dp,
                                                    if (task.completed) BrandGreen else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                                    CircleShape
                                                )
                                                .background(
                                                    if (task.completed) BrandGreen.copy(alpha = 0.2f) else Color.Transparent,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (task.completed) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = BrandGreen,
                                                    modifier = Modifier.size(14.dp)
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
    }
}

@Composable
fun PriorityItemsWidget(
    tasks: List<TaskEntity>,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onArchiveTask: (TaskEntity) -> Unit,
    onPinTask: (TaskEntity) -> Unit,
    onPostponeTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = TrackWiseUtils.getTodayString()
    val overdueTasks = tasks.filter { !it.completed && it.deadline < today }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(BrandRose.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PriorityHigh,
                        contentDescription = null,
                        tint = BrandRose,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "PRIORITY & OVERDUE RUNWAY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandRose,
                    modifier = Modifier.padding(start = 10.dp),
                    letterSpacing = 1.sp
                )
            }

            // Overdue Banner
            if (overdueTasks.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .border(1.dp, BrandRose.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = BrandRose,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${overdueTasks.size} overdue task(s) on your runway!",
                            color = BrandRose,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            if (tasks.isEmpty()) {
                Text(
                    text = "No urgent tasks or overdue items recorded.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    tasks.forEach { task ->
                        val isOverdue = task.deadline < today && !task.completed
                        SwipeableTaskItem(
                            task = task,
                            onToggleTask = { onToggleTask(task) },
                            onDeleteTask = { onDeleteTask(task) },
                            onArchiveTask = { onArchiveTask(task) },
                            onPinTask = { onPinTask(task) },
                            onPostponeTask = { onPostponeTask(task) }
                        ) {
                            val isDark = MaterialTheme.colorScheme.onBackground.red > 0.5f
                            val gradientBrush = if (task.completed) {
                                Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                            } else {
                                when (task.priority.lowercase()) {
                                    "high" -> if (isDark) {
                                        Brush.linearGradient(colors = listOf(Color(0xFF4C0519), Color(0xFF881337)))
                                    } else {
                                        Brush.linearGradient(colors = listOf(Color(0xFFFFF1F2), Color(0xFFFFD1D3)))
                                    }
                                    "medium" -> if (isDark) {
                                        Brush.linearGradient(colors = listOf(Color(0xFF431407), Color(0xFF7C2D12)))
                                    } else {
                                        Brush.linearGradient(colors = listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5)))
                                    }
                                    "low" -> if (isDark) {
                                        Brush.linearGradient(colors = listOf(Color(0xFF172554), Color(0xFF1E3A8A)))
                                    } else {
                                        Brush.linearGradient(colors = listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE)))
                                    }
                                    else -> if (isDark) {
                                        Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
                                    } else {
                                        Brush.linearGradient(colors = listOf(Color.White, Color(0xFFF9FAFB)))
                                    }
                                }
                            }
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(gradientBrush, RoundedCornerShape(16.dp))
                                    .border(
                                        1.dp,
                                        if (isOverdue) BrandRose.copy(alpha = 0.2f)
                                        else if (task.completed) Color.Transparent
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onToggleTask(task) }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Header Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = task.title,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                                                    color = if (task.completed) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onBackground,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (isOverdue) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "OVERDUE",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = BrandRose,
                                                        modifier = Modifier
                                                            .background(BrandRose.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${task.project} · ${task.priority.uppercase()}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // XP capsule tag
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (task.completed) BrandGreen.copy(alpha = 0.1f)
                                                                else BrandRose.copy(alpha = 0.12f)
                                            ),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "+${task.points} XP",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (task.completed) BrandGreen else BrandRose,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    if (task.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "📝 ${task.notes}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = BrandViolet.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Subtle separator line inside card
                                    Spacer(
                                        modifier = Modifier
                                            .padding(vertical = 10.dp)
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                    )

                                    // Footer Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left: Calendar indicator / reminder
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Due: ${task.deadline}",
                                                fontSize = 11.sp,
                                                color = if (isOverdue) BrandRose else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                                fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(start = 4.dp)
                                            )
                                        }

                                        // Right: Complete Toggle Circle button (colored BrandRose)
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .border(
                                                    2.dp,
                                                    if (task.completed) BrandRose else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                                    CircleShape
                                                )
                                                .background(
                                                    if (task.completed) BrandRose.copy(alpha = 0.2f) else Color.Transparent,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (task.completed) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = BrandRose,
                                                    modifier = Modifier.size(14.dp)
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
    }
}

@Composable
fun DailyHabitsWidget(
    habits: List<HabitEntity>,
    onToggleHabit: (HabitEntity) -> Unit,
    onHabitClick: (HabitEntity) -> Unit,
    onAddHabit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val today = TrackWiseUtils.getTodayString()
    val filteredHabits = remember(habits, today) {
        habits.filter { TrackWiseUtils.shouldShowHabitOnDate(it, today) }
    }
    val completedToday = filteredHabits.count {
        TrackWiseUtils.deserializeStringList(it.daysCompletedJson).contains(today)
    }
    val renderedHabits = remember(filteredHabits, today) {
        filteredHabits.filter {
            !TrackWiseUtils.deserializeStringList(it.daysCompletedJson).contains(today)
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(BrandOrange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = BrandOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "DAILY HABIT RUNWAYS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandOrange,
                        modifier = Modifier.padding(start = 10.dp),
                        letterSpacing = 1.sp
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$completedToday/${filteredHabits.size} COMPLETED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = BrandOrange,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (renderedHabits.isEmpty()) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (habits.isEmpty()) "Configure Habits in the Workspace tab to launch daily streak multipliers."
                               else if (completedToday > 0 && completedToday == filteredHabits.size) "All of today's habits completed! Keep up the great work! 🎉"
                               else "No active habits scheduled for today.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    renderedHabits.forEach { habit ->
                        val isDone = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson).contains(today)
                        SwipeableHabitCard(
                            habit = habit,
                            onToggleCompleted = { completed ->
                                val todayStr = TrackWiseUtils.getTodayString()
                                val days = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson)
                                val isCurrentlyCompleted = days.contains(todayStr)
                                if (isCurrentlyCompleted != completed) {
                                    onToggleHabit(habit)
                                }
                            }
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDone) BrandOrange.copy(alpha = 0.05f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isDone) BrandOrange.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onHabitClick(habit) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left: Habit icon / checkmark
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable { onToggleHabit(habit) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Done",
                                                tint = BrandOrange,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        } else {
                                            HabitIconView(
                                                icon = habit.icon,
                                                tint = BrandOrange,
                                                size = 22.dp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    // Middle: Text details
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = habit.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                            color = if (isDone) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = habit.category,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    // Right: Beautiful streak flame badge
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(
                                                if (habit.streak > 0) BrandOrange.copy(alpha = 0.12f)
                                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = if (habit.streak > 0) BrandOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "${habit.streak}d",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (habit.streak > 0) BrandOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                            modifier = Modifier.padding(start = 2.dp)
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BadgeDetailDialog(
    badge: BadgeSpec,
    isEarned: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPink),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Awesome!", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "medal_bounce")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                Text(
                    text = badge.medal,
                    fontSize = 44.sp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                )
                Column {
                    Text(text = badge.name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = badge.tier, fontSize = 11.sp, color = BrandPink, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isEarned) BrandPink.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                        .border(
                            1.dp,
                            if (isEarned) BrandPink.copy(alpha = 0.3f) else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isEarned) Icons.Default.EmojiEvents else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isEarned) BrandPink else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isEarned) "UNLOCKED!" else "LOCKED",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isEarned) BrandPink else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = badge.description,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Achieved by maintaining a streak of ${badge.days} days in any habit.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                if (isEarned) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨ You earned this prestigious badge! Keep up the incredible daily focus and dominate your streaks! ✨",
                            fontSize = 12.sp,
                            color = BrandPink,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "💡 Consistency is the key! Perform your habits daily to unlock this beautiful milestone.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun HabitBadgeCollection(
    habits: List<HabitEntity>,
    modifier: Modifier = Modifier
) {
    // Collect all unique milestone badges earned across all habits
    val earnedMilestones = habits.flatMap {
        TrackWiseUtils.deserializeIntList(it.badgesEarnedJson)
    }.distinct()

    val badges = listOf(
        BadgeSpec(1, "The Spark", "🥉", "The Launchpad", "Ignited the habit"),
        BadgeSpec(3, "Three's Company", "🥉", "The Launchpad", "Overcame day-two slump"),
        BadgeSpec(5, "Workweek Warrior", "🥉", "The Launchpad", "Five consecutive days"),
        BadgeSpec(7, "Weekly Wonder", "🥉", "The Launchpad", "Completed full week"),
        BadgeSpec(14, "Fortnight Force", "🥈", "The Builder", "Two weeks dedication"),
        BadgeSpec(21, "Habit Former", "🥈", "The Builder", "Avg days to lock routine"),
        BadgeSpec(30, "Calendar Crusher", "🥈", "The Builder", "One full month"),
        BadgeSpec(45, "Halfway Hero", "🥈", "The Builder", "Momentum past 1 month"),
        BadgeSpec(60, "Iron Will", "🥇", "The Master", "Two months unbroken"),
        BadgeSpec(90, "Seasoned Pro", "🥇", "The Master", "Seasonal commitment"),
        BadgeSpec(100, "Centurion", "🥇", "The Master", "Triple-digit milestone"),
        BadgeSpec(365, "Immortal", "🥇", "The Master", "One full year")
    )

    var selectedBadge by remember { mutableStateOf<BadgeSpec?>(null) }
    var selectedBadgeEarned by remember { mutableStateOf(false) }

    if (selectedBadge != null) {
        BadgeDetailDialog(
            badge = selectedBadge!!,
            isEarned = selectedBadgeEarned,
            onDismiss = { selectedBadge = null }
        )
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HABIT BADGES (${earnedMilestones.size}/12 earned)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPink
                )
                Text(
                    text = "Tap to inspect",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(badges) { badge ->
                    val isEarned = earnedMilestones.contains(badge.days)
                    
                    // Core Infinite animations for glowing/pulsing earned badges
                    val infiniteTransition = rememberInfiniteTransition(label = "badge_glow")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.97f,
                        targetValue = 1.03f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )
                    val sparkleAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "sparkle"
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEarned) BrandPink.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(130.dp)
                            .graphicsLayer {
                                if (isEarned) {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                }
                            }
                            .clickable {
                                selectedBadgeEarned = isEarned
                                selectedBadge = badge
                            }
                            .border(
                                width = 1.dp,
                                color = if (isEarned) BrandPink.copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.TopEnd,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                            ) {
                                if (isEarned) {
                                    Text(
                                        text = "✨",
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .graphicsLayer { alpha = sparkleAlpha }
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                        modifier = Modifier.size(10.dp).align(Alignment.TopEnd)
                                    )
                                }
                                
                                Text(
                                    text = badge.medal,
                                    fontSize = 28.sp,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            
                            Text(
                                text = badge.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEarned) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${badge.days} days",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPink
                            )
                            Text(
                                text = badge.description,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                maxLines = 2,
                                minLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}



private fun getTomorrowDate(): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, 1)
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(cal.time)
}

private fun getNextMondayDate(): String {
    val cal = Calendar.getInstance()
    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(cal.time)
}

private fun showDatePicker(context: android.content.Context, onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val isDarkTheme = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
    val themeId = if (isDarkTheme) android.R.style.Theme_DeviceDefault_Dialog_Alert else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert
    val dpd = android.app.DatePickerDialog(
        context,
        themeId,
        { _, year, month, dayOfMonth ->
            val formattedMonth = String.format("%02d", month + 1)
            val formattedDay = String.format("%02d", dayOfMonth)
            val dateStr = "$year-$formattedMonth-$formattedDay"
            onDateSelected(dateStr)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    dpd.show()
}

private fun showTimePicker(context: android.content.Context, onTimeSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val isDarkTheme = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
    val themeId = if (isDarkTheme) android.R.style.Theme_DeviceDefault_Dialog_Alert else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert
    val tpd = android.app.TimePickerDialog(
        context,
        themeId,
        { _, hourOfDay, minute ->
            val formattedHour = String.format("%02d", hourOfDay)
            val formattedMinute = String.format("%02d", minute)
            onTimeSelected("$formattedHour:$formattedMinute")
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )
    tpd.show()
}

@Composable
fun SwipeableTaskItem(
    task: com.example.data.TaskEntity,
    onToggleTask: () -> Unit,
    onDeleteTask: () -> Unit,
    onArchiveTask: () -> Unit,
    onPinTask: () -> Unit,
    onPostponeTask: () -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val animOffset = remember { androidx.compose.animation.core.Animatable(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
    ) {
        // --- BACKGROUND ACTIONS ---
        // Right Swipe Actions (Mark Done, Pin) - aligned to LEFT (revealed when dragging right, i.e. positive offset)
        val rightAlpha = if (animOffset.value > 0f) {
            (animOffset.value / (15f * density)).coerceIn(0f, 1f)
        } else {
            0f
        }
        if (animOffset.value > 1f) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .graphicsLayer { alpha = rightAlpha },
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch { animOffset.animateTo(0f) }
                        onToggleTask()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BrandGreen.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Mark Done",
                        tint = BrandGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch { animOffset.animateTo(0f) }
                        onPinTask()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BrandAmber.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin to Top",
                        tint = BrandAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Left Swipe Actions (Archive, Delete, Postpone) - aligned to RIGHT (revealed when dragging left, i.e. negative offset)
        val leftAlpha = 1f
        if (animOffset.value < -1f) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .graphicsLayer { alpha = leftAlpha },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch { animOffset.animateTo(0f) }
                        onArchiveTask()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Archive",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch { animOffset.animateTo(0f) }
                        onDeleteTask()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch { animOffset.animateTo(0f) }
                        onPostponeTask()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BrandOrange.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Postpone",
                        tint = BrandOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- FOREGROUND CARD ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { androidx.compose.ui.unit.IntOffset(animOffset.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                animOffset.snapTo((animOffset.value + dragAmount).coerceIn(-280f * density, 280f * density))
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                val offsetPx = animOffset.value
                                val thresholdFullRight = 200f * density
                                val thresholdFullLeft = -200f * density
                                val thresholdRevealRight = 50f * density
                                val thresholdRevealLeft = -50f * density
                                val snapRevealRight = 112f * density
                                val snapRevealLeft = -164f * density

                                if (offsetPx > thresholdFullRight) {
                                    animOffset.animateTo(300f * density)
                                    onToggleTask()
                                    animOffset.animateTo(0f)
                                } else if (offsetPx < thresholdFullLeft) {
                                    animOffset.animateTo(-300f * density)
                                    onPostponeTask()
                                    animOffset.animateTo(0f)
                                } else if (offsetPx > thresholdRevealRight) {
                                    animOffset.animateTo(snapRevealRight)
                                } else if (offsetPx < thresholdRevealLeft) {
                                    animOffset.animateTo(snapRevealLeft)
                                } else {
                                    animOffset.animateTo(0f)
                                }
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
fun PostponeTaskDialog(
    task: com.example.data.TaskEntity,
    onDismiss: () -> Unit,
    onReschedule: (newDate: String, newTime: String?) -> Unit,
    onSkipReoccurrence: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Postpone Workitem", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Reschedule \"${task.title}\" to:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Today
                Button(
                    onClick = {
                        onReschedule(TrackWiseUtils.getTodayString(), task.reminderTime)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Today", fontWeight = FontWeight.SemiBold)
                }

                // Tomorrow
                Button(
                    onClick = {
                        val tomorrow = getTomorrowDate()
                        onReschedule(tomorrow, task.reminderTime)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tomorrow", fontWeight = FontWeight.SemiBold)
                }

                // Next Monday
                Button(
                    onClick = {
                        val nextMonday = getNextMondayDate()
                        onReschedule(nextMonday, task.reminderTime)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Icon(Icons.Default.NextWeek, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Next Monday", fontWeight = FontWeight.SemiBold)
                }

                // Pick Date
                Button(
                    onClick = {
                        showDatePicker(context) { dateStr ->
                            showTimePicker(context) { timeStr ->
                                onReschedule(dateStr, timeStr)
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick Date & Time", fontWeight = FontWeight.SemiBold)
                }

                // Skip Reoccurrence
                if (task.repeatType != "none") {
                    Button(
                        onClick = {
                            onSkipReoccurrence()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Skip Reoccurrence", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Clear", color = MaterialTheme.colorScheme.error)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun DashboardHabitBreakerChartWidget(
    badHabits: List<TrackWiseViewModel.BadHabitSpec>,
    onNavigate: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BrandRose.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = BrandRose,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "HABIT BREAKER INSIGHTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandRose,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Slip-ups & sobriety progress",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = { onNavigate("habit_breaker") },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Details", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandRose)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (badHabits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "No bad habits added yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Add habits to track slip-ups and clean streaks",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Let's compute weekly slip-up statistics
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val dayFormat = java.text.SimpleDateFormat("E", java.util.Locale.US)
                val last7Days = remember(badHabits) {
                    (0..6).map { i ->
                        val cal = java.util.Calendar.getInstance()
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
                        val dateStr = sdf.format(cal.time)
                        val dayLabel = dayFormat.format(cal.time).substring(0, 1)
                        dateStr to dayLabel
                    }.reversed()
                }

                val dailyCounts = remember(badHabits, last7Days) {
                    last7Days.map { (dateStr, _) ->
                        badHabits.sumOf { habit ->
                            habit.logs.count { it.startsWith(dateStr) }
                        }
                    }
                }

                val maxCount = remember(dailyCounts) { (dailyCounts.maxOrNull() ?: 1).coerceAtLeast(1) }

                // Slip-ups Trend Bar Chart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    last7Days.forEachIndexed { idx, (dateStr, label) ->
                        val count = dailyCounts[idx]
                        val barHeightFactor = count.toFloat() / maxCount.toFloat()

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (count > 0) {
                                Text(
                                    text = count.toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandRose
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.7f)
                                    .width(16.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (count > 0) BrandRose.copy(alpha = 0.85f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                    )
                                    .fillMaxHeight(barHeightFactor.coerceAtLeast(0.08f))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown list
                Text(
                    text = "ACTIVE BAD HABITS & SOBRIETY STATUS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    badHabits.take(3).forEach { habit ->
                        val lastSlipDateStr = habit.logs.map { it.take(10) }.maxOrNull()
                        val cleanDays = remember(lastSlipDateStr) {
                            if (lastSlipDateStr == null) {
                                // No slip-ups ever
                                "Clean"
                            } else {
                                try {
                                    val lastDate = sdf.parse(lastSlipDateStr)
                                    val todayDate = sdf.parse(sdf.format(java.util.Date()))
                                    val diff = todayDate.time - lastDate.time
                                    val days = (diff / (1000 * 60 * 60 * 24)).toInt()
                                    if (days <= 0) "Slipped Today" else "$days d clean"
                                } catch (e: Exception) {
                                    "Active"
                                }
                            }
                        }

                        val isClean = cleanDays != "Slipped Today"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = habit.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isClean) BrandGreen else BrandRose)
                                    )
                                    Text(
                                        text = cleanDays,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isClean) BrandGreen else BrandRose
                                    )
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = BrandRose.copy(alpha = 0.08f)
                                )
                            ) {
                                Text(
                                    text = "${habit.logs.size} slip-up${if (habit.logs.size == 1) "" else "s"}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandRose,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (badHabits.size > 3) {
                        Text(
                            text = "+ ${badHabits.size - 3} more bad habit trackers...",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = BrandRose,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable { onNavigate("habit_breaker") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardFinanceSummaryWidget(
    financeLogs: List<com.example.data.FinanceLogEntity>,
    onNavigate: (String) -> Unit
) {
    val currentMonthKey = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
    val currentMonthName = remember { SimpleDateFormat("MMMM yyyy", Locale.US).format(Date()) }

    val monthLogs = remember(financeLogs, currentMonthKey) {
        financeLogs.filter { it.date.startsWith(currentMonthKey) }
    }

    val totalIncome = remember(monthLogs) {
        monthLogs.filter { it.type == "income" }.sumOf { it.amount }
    }

    val totalExpense = remember(monthLogs) {
        monthLogs.filter { it.type == "expense" }.sumOf { it.amount }
    }

    val totalSavings = remember(monthLogs) {
        monthLogs.filter { it.type == "savings" }.sumOf { it.amount }
    }

    val netBalance = totalIncome - totalExpense - totalSavings

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("finance") }
            .border(
                1.dp,
                BrandGreen.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BrandGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "MONTHLY FINANCE SUMMARY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandGreen,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = currentMonthName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Finance",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Income tile
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Income",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹${String.format("%,.0f", totalIncome)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                // Expense tile
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Expense",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEF4444)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹${String.format("%,.0f", totalExpense)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }

                // Net Balance tile
                val balanceColor = if (netBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = balanceColor.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Net Balance",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = balanceColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹${String.format("%,.0f", netBalance)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = balanceColor
                        )
                    }
                }
            }
        }
    }
}

private fun calculateDashboardOccasionDays(bday: com.example.data.BirthdayEntity): Int {
    val dateStr = bday.date
    val parts = dateStr.split("-")
    if (parts.size == 3) {
        val year = parts[0].toIntOrNull() ?: 2000
        val month = parts[1].toIntOrNull() ?: 1
        val day = parts[2].toIntOrNull() ?: 1

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val eventDate = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (bday.countingMode == "Count Up") {
            return 999
        }

        if (eventDate.timeInMillis == today.timeInMillis) {
            return 0
        } else if (eventDate.after(today)) {
            val diffMs = eventDate.timeInMillis - today.timeInMillis
            return Math.round(diffMs.toDouble() / (1000 * 60 * 60 * 24)).toInt()
        } else {
            val bdayThisYear = Calendar.getInstance().apply {
                set(Calendar.YEAR, today.get(Calendar.YEAR))
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (bdayThisYear.timeInMillis == today.timeInMillis) {
                return 0
            } else if (bdayThisYear.after(today)) {
                val diffMs = bdayThisYear.timeInMillis - today.timeInMillis
                return Math.round(diffMs.toDouble() / (1000 * 60 * 60 * 24)).toInt()
            } else {
                val bdayNextYear = Calendar.getInstance().apply {
                    set(Calendar.YEAR, today.get(Calendar.YEAR) + 1)
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val diffMs = bdayNextYear.timeInMillis - today.timeInMillis
                return Math.round(diffMs.toDouble() / (1000 * 60 * 60 * 24)).toInt()
            }
        }
    }
    return 999
}

@Composable
fun DashboardOccasionsCountdownWidget(
    birthdays: List<com.example.data.BirthdayEntity>,
    onNavigate: (String) -> Unit
) {
    val countdownItems = remember(birthdays) {
        birthdays.map { it to calculateDashboardOccasionDays(it) }
            .filter { it.second in 0..3 }
            .sortedBy { it.second }
    }

    if (countdownItems.isEmpty()) return

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                BrandOrange.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BrandOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = BrandOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "UPCOMING OCCASION COUNTDOWNS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandOrange,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "3 Days or Fewer Remaining",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Surface(
                    onClick = { onNavigate("countdown") },
                    shape = RoundedCornerShape(8.dp),
                    color = BrandOrange.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "View All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                countdownItems.forEach { (bday, daysLeft) ->
                    val badgeColor = when (daysLeft) {
                        0 -> Color(0xFFEF4444)
                        1 -> Color(0xFFF59E0B)
                        else -> BrandCyan
                    }
                    val badgeText = when (daysLeft) {
                        0 -> "TODAY! 🎉"
                        1 -> "1 DAY LEFT ⏰"
                        else -> "$daysLeft DAYS LEFT ⏳"
                    }

                    Surface(
                        onClick = { onNavigate("countdown") },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bday.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = bday.category.split("|").firstOrNull() ?: "Occasion",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = badgeColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
fun DashboardWishlistRemindersWidget(
    wishlistItems: List<com.example.data.WishItemEntity>,
    onTogglePurchased: (com.example.data.WishItemEntity) -> Unit,
    onNavigate: (String) -> Unit
) {
    if (wishlistItems.isEmpty()) return

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                BrandPink.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BrandPink.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = BrandPink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "WISHLIST REMINDERS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandPink,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${wishlistItems.size} Scheduled Items",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Surface(
                    onClick = { onNavigate("workspace") },
                    shape = RoundedCornerShape(8.dp),
                    color = BrandPink.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "View All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPink,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                wishlistItems.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "₹${String.format("%,.0f", item.price)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPink
                                    )
                                    val priorityColor = when (item.priority.lowercase()) {
                                        "high" -> Color(0xFFEF4444)
                                        "medium" -> Color(0xFFF59E0B)
                                        else -> BrandCyan
                                    }
                                    Text(
                                        text = "• ${item.priority.uppercase()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = priorityColor
                                    )
                                    if (!item.reminderDate.isNullOrBlank()) {
                                        Text(
                                            text = "• ⏰ ${item.reminderDate} ${item.reminderTime ?: ""}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { onTogglePurchased(item) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(BrandPink.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Mark Purchased",
                                    tint = BrandPink,
                                    modifier = Modifier.size(18.dp)
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
fun DashboardBadgesAndAchievementsHallWidget(
    allHabits: List<com.example.data.HabitEntity>,
    allTasks: List<com.example.data.TaskEntity>,
    allFinanceLogs: List<com.example.data.FinanceLogEntity>,
    allWishlist: List<com.example.data.WishItemEntity>,
    allBirthdays: List<com.example.data.BirthdayEntity>,
    badHabits: List<TrackWiseViewModel.BadHabitSpec>,
    socialCircleSize: Int,
    hasActiveAlarm: Boolean,
    alarmsCount: Int,
    waterLogs: List<com.example.data.WaterLogEntity>,
    streakHistory: List<com.example.data.StreakHistoryEntity>,
    userLevel: Int,
    todayScore: Int,
    onNavigate: (String) -> Unit
) {
    val totalXP = remember(streakHistory, todayScore) {
        streakHistory.sumOf { it.score } + todayScore
    }
    val netWorth = remember(allFinanceLogs) {
        allFinanceLogs.filter { it.type == "income" || it.type == "savings" }.sumOf { it.amount } - allFinanceLogs.filter { it.type == "expense" }.sumOf { it.amount }
    }

    val systemAchievements = remember(allTasks, allHabits, allFinanceLogs, allWishlist, allBirthdays, badHabits, socialCircleSize, hasActiveAlarm, alarmsCount, waterLogs, streakHistory, userLevel, totalXP, netWorth) {
        getAllSystemAchievements(
            allTasks = allTasks,
            allHabits = allHabits,
            allFinanceLogs = allFinanceLogs,
            allWishlist = allWishlist,
            allBirthdays = allBirthdays,
            badHabits = badHabits,
            socialCircleSize = socialCircleSize,
            hasActiveAlarm = hasActiveAlarm,
            alarmsCount = alarmsCount,
            waterLogs = waterLogs,
            streakHistory = streakHistory,
            userLevel = userLevel,
            totalXP = totalXP,
            netWorth = netWorth
        )
    }

    val earnedBadgeDays = remember(allHabits) {
        allHabits.flatMap { TrackWiseUtils.deserializeIntList(it.badgesEarnedJson) }.distinct()
    }

    val habitBadgesData = remember(earnedBadgeDays) {
        ALL_STANDARD_HABIT_BADGES.map { spec ->
            val maxStreak = allHabits.maxOfOrNull { it.maxStreak } ?: 0
            AchievementItemSpec(
                title = spec.name,
                desc = spec.desc,
                progress = maxStreak,
                target = spec.days,
                icon = Icons.Default.MilitaryTech,
                iconColor = BrandAmber,
                tier = spec.tier.uppercase(),
                isBadge = true
            )
        }
    }

    val allBoardItems = remember(habitBadgesData, systemAchievements) {
        habitBadgesData + systemAchievements
    }

    val totalUnlockedCount = remember(allBoardItems) {
        allBoardItems.count { it.progress >= it.target }
    }

    var selectedFilter by remember { mutableStateOf("All") }
    var selectedInspectItem by remember { mutableStateOf<AchievementItemSpec?>(null) }

    val filteredItems = remember(allBoardItems, selectedFilter) {
        when (selectedFilter) {
            "Badges" -> allBoardItems.filter { it.isBadge }
            "Achievements" -> allBoardItems.filter { !it.isBadge }
            "Unlocked" -> allBoardItems.filter { it.progress >= it.target }
            else -> allBoardItems
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                BrandViolet.copy(alpha = 0.5f),
                RoundedCornerShape(24.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(BrandViolet.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = BrandViolet,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "BADGES & ACHIEVEMENTS MASTERY HALL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandViolet,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$totalUnlockedCount / ${allBoardItems.size} Unlocked",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("All", "Badges", "Achievements", "Unlocked").forEach { filter ->
                    val isSel = selectedFilter == filter
                    Surface(
                        onClick = { selectedFilter = filter },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) BrandViolet else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3 Horizontal Sliding Rows (sliding at once)
            val chunkedCols = filteredItems.chunked(3)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(chunkedCols) { colItems ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        colItems.forEach { item ->
                            val isUnlocked = item.progress >= item.target
                            MasteryItemGridCard(
                                item = item,
                                isUnlocked = isUnlocked,
                                onClick = { selectedInspectItem = item },
                                modifier = Modifier.width(105.dp)
                            )
                        }
                        if (colItems.size < 3) {
                            repeat(3 - colItems.size) {
                                Spacer(
                                    modifier = Modifier
                                        .width(105.dp)
                                        .aspectRatio(0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedInspectItem?.let { item ->
        val isUnlocked = item.progress >= item.target
        AlertDialog(
            onDismissRequest = { selectedInspectItem = null },
            confirmButton = {
                TextButton(onClick = { selectedInspectItem = null }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            icon = {
                Icon(
                    imageVector = if (isUnlocked) item.icon else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isUnlocked) item.iconColor else Color.Gray,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.desc,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isUnlocked) BrandViolet.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isUnlocked) "UNLOCKED • ${item.tier}" else "LOCKED • Progress: ${item.progress}/${item.target}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isUnlocked) BrandViolet else Color.Gray,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun MasteryItemGridCard(
    item: AchievementItemSpec,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tierColor = when (item.tier) {
        "MYTHIC" -> Color(0xFFFF3B30)
        "LEGENDARY" -> Color(0xFFFFD700)
        "EPIC" -> Color(0xFFAF52DE)
        "RARE" -> Color(0xFF007AFF)
        else -> Color(0xFF34C759)
    }

    val cardBg = if (isUnlocked) {
        tierColor.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    }

    val borderStroke = if (isUnlocked) {
        BorderStroke(1.5.dp, tierColor)
    } else {
        BorderStroke(1.dp, Color.Gray.copy(alpha = 0.25f))
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        border = borderStroke,
        modifier = modifier.aspectRatio(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isUnlocked) {
                    Text(
                        text = item.tier.take(3),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = tierColor
                    )
                    Text(
                        text = "★",
                        fontSize = 10.sp,
                        color = Color(0xFFFFD700)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${item.progress}/${item.target}",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) tierColor.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = if (isUnlocked) tierColor else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = item.title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
