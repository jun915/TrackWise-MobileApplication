package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.ui.theme.*
import com.example.utils.TrackWiseUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.sessionUser.collectAsState()
    val todayScore by viewModel.todayScore.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()
    val allWishlist by viewModel.allWishlist.collectAsState()

    val todayStr = TrackWiseUtils.getTodayString()
    val isPreLaunch = TrackWiseUtils.isBeforeLaunch(todayStr)

    val name = currentUser?.fullName?.split(" ")?.firstOrNull() ?: "there"

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // --- Pre-Launch Banner (Part 1.5 & Part 6.1) ---
        if (isPreLaunch) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandCyan.copy(alpha = 0.15f)),
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

        // --- Welcome Hero Section ---
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "$greeting, $name!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Your analytics, today's focus, and priority items at a glance.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // --- 3 Stat Tiles (Grid) ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Points Stat
                StatTile(
                    label = "POINTS",
                    value = "$todayScore",
                    color = BrandViolet,
                    icon = Icons.Default.Stars,
                    modifier = Modifier.weight(1f)
                )
                // Tasks Stat
                val totalTasksCompleted = allTasks.count { it.completed }
                StatTile(
                    label = "TASKS",
                    value = "$totalTasksCompleted",
                    color = BrandPink,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                // Streak Stat
                val maxHabitStreak = allHabits.maxOfOrNull { it.streak } ?: 0
                StatTile(
                    label = "STREAK",
                    value = "${maxHabitStreak}d",
                    color = BrandCyan,
                    icon = Icons.Default.LocalFireDepartment,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- Progress Widget (Section 8.2) ---
        item {
            ProgressWidget(viewModel = viewModel)
        }

        // --- Today's Items Widget (Section 8.3) ---
        item {
            TodayItemsWidget(
                tasks = allTasks.filter { it.deadline == todayStr },
                onToggleTask = { viewModel.toggleTaskCompletion(it) }
            )
        }

        // --- Priority Items Widget (Section 8.4) ---
        item {
            PriorityItemsWidget(
                tasks = allTasks,
                wishlist = allWishlist,
                onToggleTask = { viewModel.toggleTaskCompletion(it) }
            )
        }

        // --- Daily Habits Widget (Section 8.5) ---
        item {
            DailyHabitsWidget(
                habits = allHabits,
                onToggleHabit = { viewModel.toggleHabitToday(it) }
            )
        }

        // --- Habit Badge Collection (Section 8.6) ---
        if (allHabits.isNotEmpty()) {
            item {
                HabitBadgeCollection(habits = allHabits)
            }
        }
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = modifier.border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ProgressWidget(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Streaks", "Health")

    val streakHistory by viewModel.streakHistory.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()
    val weightEntries by viewModel.weightEntries.collectAsState()
    val waterLogs by viewModel.waterLogs.collectAsState()

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ANALYTICS & PROGRESS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrandViolet,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Sub-tabs row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabs.forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTab == index) BrandViolet else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == index) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Sub-tab Content
            when (selectedTab) {
                0 -> { // Overview Tab
                    if (streakHistory.isEmpty()) {
                        EmptyProgressPlaceholder("Complete tasks and habits to populate historical analytics charts.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Daily Scores (Last 7 Days)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            // Clean visual list representing history bars since complex external charts can fail
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
                                        // Score fill bar
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
                                        text = "${history.score} pts",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPink
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> { // Streaks Tab
                    if (allHabits.isEmpty()) {
                        EmptyProgressPlaceholder("Create habits in the Workspace tab to view streak trajectories.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            allHabits.take(4).forEach { habit ->
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = habit.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.LocalFireDepartment,
                                                contentDescription = null,
                                                tint = BrandOrange,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "${habit.streak}d streak",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandOrange,
                                                modifier = Modifier.padding(start = 2.dp)
                                            )
                                        }
                                    }
                                    // Progress bar to next milestone
                                    val milestones = listOf(1, 3, 5, 7, 14, 21, 30, 45, 60, 90, 100, 365)
                                    val nextMilestone = milestones.firstOrNull { it > habit.streak } ?: 365
                                    val progressFraction = (habit.streak.toFloat() / nextMilestone).coerceIn(0f, 1f)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(progressFraction)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(BrandOrange)
                                        )
                                    }
                                    Text(
                                        text = "Next milestone: $nextMilestone days",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> { // Health Tab
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Water log progress
                        val todayWater = waterLogs.find { it.date == TrackWiseUtils.getTodayString() }
                        val waterGlasses = todayWater?.glasses ?: 0
                        val waterGoal = todayWater?.goal ?: 8
                        val waterFraction = (waterGlasses.toFloat() / waterGoal).coerceIn(0f, 1f)

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Water Intake Today 💧",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$waterGlasses/$waterGoal glasses",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandCyan
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(waterFraction)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(BrandCyan)
                                )
                            }
                        }

                        // Latest Weight log
                        val latestWeight = weightEntries.firstOrNull()
                        if (latestWeight != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Latest Weight Log ⚖️",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${latestWeight.weightKg} kg (${latestWeight.date})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPink
                                )
                            }
                        } else {
                            Text(
                                text = "No weight logs recorded yet.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = BrandCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "TODAY'S FOCUS ITEMS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandCyan,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (tasks.isEmpty()) {
                Text(
                    text = "All caught up for today! Add tasks with today's deadline in the Workspace tab.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasks.sortedBy { it.completed }.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .clickable { onToggleTask(task) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Completion Spark/Check Simulation (Section 14)
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
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = BrandGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (task.completed) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${task.project} · ${task.priority.uppercase()} · ${task.points} pts",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }

                            if (task.reminderTime != null) {
                                Icon(
                                    Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = BrandViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = task.reminderTime,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = BrandViolet,
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

@Composable
fun PriorityItemsWidget(
    tasks: List<TaskEntity>,
    wishlist: List<WishItemEntity>,
    onToggleTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = TrackWiseUtils.getTodayString()
    val overdueTasks = tasks.filter { !it.completed && it.deadline < today }
    val urgentTasks = tasks.filter { !it.completed && (it.priority == "high" || it.deadline <= today) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    Icons.Default.PriorityHigh,
                    contentDescription = null,
                    tint = BrandRose,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "PRIORITY & OVERDUE ITEMS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandRose,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Overdue Banner
            if (overdueTasks.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandRose.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, BrandRose, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = BrandRose,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${overdueTasks.size} overdue task(s) detect! Complete them immediately.",
                            color = BrandRose,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            if (urgentTasks.isEmpty() && wishlist.none { it.priority == "high" && !it.purchased }) {
                Text(
                    text = "No urgent tasks or high-priority wishlist items recorded.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    urgentTasks.take(5).forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .clickable { onToggleTask(task) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = BrandAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row {
                                    Text(
                                        text = "${task.deadline} · ${task.project}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                    if (task.deadline < today) {
                                        Text(
                                            text = " (overdue)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandRose
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Top wishlist high-priority items
                    wishlist.filter { it.priority == "high" && !it.purchased }.take(3).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = BrandPink,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Wishlist aspirational · ₹${item.price}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
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
fun DailyHabitsWidget(
    habits: List<HabitEntity>,
    onToggleHabit: (HabitEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = TrackWiseUtils.getTodayString()
    val completedToday = habits.count {
        TrackWiseUtils.deserializeStringList(it.daysCompletedJson).contains(today)
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = BrandOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "DAILY HABIT RUNWAYS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandOrange.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$completedToday/${habits.size} done",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (habits.isEmpty()) {
                Text(
                    text = "Configure Habit Runways in the Workspace tab to launch daily streak multipliers.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    habits.sortedBy {
                        TrackWiseUtils.deserializeStringList(it.daysCompletedJson).contains(today)
                    }.forEach { habit ->
                        val isDone = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson).contains(today)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDone) BrandOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    1.dp,
                                    if (isDone) BrandOrange.copy(alpha = 0.3f) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onToggleHabit(habit) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .border(
                                        2.dp,
                                        if (isDone) BrandOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                        CircleShape
                                    )
                                    .background(
                                        if (isDone) BrandOrange.copy(alpha = 0.2f) else Color.Transparent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = BrandOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = habit.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = habit.category,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = BrandOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${habit.streak}d",
                                    fontSize = 12.sp,
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

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "HABIT BADGES (${earnedMilestones.size}/12 earned)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrandPink,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(badges) { badge ->
                    val isEarned = earnedMilestones.contains(badge.days)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEarned) BrandPink.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(130.dp)
                            .border(
                                1.dp,
                                if (isEarned) BrandPink.copy(alpha = 0.4f) else Color.Transparent,
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = badge.medal,
                                fontSize = 28.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
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

data class BadgeSpec(
    val days: Int,
    val name: String,
    val medal: String,
    val tier: String,
    val description: String
)
