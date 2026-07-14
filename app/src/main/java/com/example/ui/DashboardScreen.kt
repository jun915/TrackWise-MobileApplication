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

import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource

private fun isTaskDueTimeEnded(task: com.example.data.TaskEntity, todayStr: String): Boolean {
    if (task.deadline < todayStr) return true
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
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.sessionUser.collectAsState()
    val todayScore by viewModel.todayScore.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()
    val allWishlist by viewModel.allWishlist.collectAsState()

    val todayStr = TrackWiseUtils.getTodayString()
    val isPreLaunch = TrackWiseUtils.isBeforeLaunch(todayStr)

    val todayFocusItems = remember(allTasks, todayStr) {
        allTasks.filter { 
            TrackWiseUtils.shouldShowTaskOnDate(it, todayStr) && 
            (!it.completed || !isTaskDueTimeEnded(it, todayStr))
        }
        .sortedWith(compareBy<TaskEntity> { it.completed }
            .thenBy { it.reminderTime == null }
            .thenBy { it.reminderTime ?: "" }
            .thenBy { it.title }
        )
        .take(5)
    }

    val priorityAndOverdueItems = remember(allTasks, todayStr) {
        allTasks.filter {
            if (it.completed) {
                (it.priority == "high" || it.deadline == todayStr) && !isTaskDueTimeEnded(it, todayStr)
            } else {
                it.deadline < todayStr || it.priority == "high"
            }
        }
        .sortedWith(compareBy<TaskEntity> { it.completed }
            .thenBy { it.deadline }
            .thenByDescending { it.priority == "high" }
            .thenBy { it.title }
        )
        .take(5)
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

        // --- Badge Level Mastery Widget ---
        item {
            val earnedCount = remember(allHabits) {
                allHabits.flatMap {
                    TrackWiseUtils.deserializeIntList(it.badgesEarnedJson)
                }.distinct().size
            }
            
            val (rankName, medal, nextMilestone) = when {
                earnedCount <= 1 -> Triple("Bronze Explorer", "🥉", "Earn 2 badges for Silver")
                earnedCount <= 4 -> Triple("Silver Achiever", "🥈", "Earn 5 badges for Gold")
                earnedCount <= 8 -> Triple("Gold Champion", "🥇", "Earn 9 badges for Platinum")
                earnedCount <= 11 -> Triple("Platinum Legend", "👑", "Earn 12 badges for Immortal")
                else -> Triple("Immortal Master", "🔮", "You've unlocked all milestones!")
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BrandPink.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        BrandPink.copy(alpha = 0.25f),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Animating Medal icon
                    val infiniteTransition = rememberInfiniteTransition(label = "mastery_medal")
                    val bounceScale by infiniteTransition.animateFloat(
                        initialValue = 0.92f,
                        targetValue = 1.08f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bounce"
                    )

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(BrandPink.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = medal,
                            fontSize = 32.sp,
                            modifier = Modifier.graphicsLayer {
                                scaleX = bounceScale
                                scaleY = bounceScale
                            }
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "BADGE MASTERY LEVEL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandPink,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = rankName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "$earnedCount/12 Badges",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPink
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { earnedCount.toFloat() / 12f },
                            color = BrandPink,
                            trackColor = BrandPink.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "🎯 Goal: $nextMilestone",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
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

        // --- Daily Scores Overview Widget ---
        item {
            DailyScoresOverviewWidget(viewModel = viewModel)
        }

        // --- Habit Streaks Widget (1st section of streaks) ---
        item {
            HabitStreaksWidget(allHabits = allHabits)
        }

        // --- Water Intake Widget (1st section of health) ---
        item {
            val waterLogs by viewModel.waterLogs.collectAsState()
            WaterIntakeWidget(viewModel = viewModel, waterLogs = waterLogs)
        }

        // --- Today's Items Widget (Section 8.3) ---
        item {
            TodayItemsWidget(
                tasks = todayFocusItems,
                onToggleTask = { viewModel.toggleTaskCompletion(it) }
            )
        }

        // --- Priority Items Widget (Section 8.4) ---
        item {
            PriorityItemsWidget(
                tasks = priorityAndOverdueItems,
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
                text = "DAILY SCORES OVERVIEW",
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
                        text = "Daily Scores (Last 7 Days)",
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .width(115.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
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
                    text = "DAILY HYDRATION MONITOR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandCyan
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val todayStr = TrackWiseUtils.getTodayString()
                    val todayWater = waterLogs.firstOrNull { it.date == todayStr }
                    val currentGlasses = todayWater?.glasses ?: 0

                    IconButton(
                        onClick = { viewModel.adjustWaterLog(-1) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrement water", tint = BrandCyan, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { viewModel.adjustWaterLog(1) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increment water", tint = BrandCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }

            val todayWater = waterLogs.firstOrNull { it.date == TrackWiseUtils.getTodayString() }
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
    onToggleTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = TrackWiseUtils.getTodayString()
    val overdueTasks = tasks.filter { !it.completed && it.deadline < today }

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
                            text = "${overdueTasks.size} overdue task(s) detected! Complete them immediately.",
                            color = BrandRose,
                            fontSize = 12.sp,
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
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasks.forEach { task ->
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
                                tint = if (task.priority == "high") BrandAmber else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
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
                                        text = "${task.deadline} · ${task.project} · ${task.priority.uppercase()}",
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
                                if (task.notes.isNotBlank()) {
                                    Text(
                                        text = "📝 ${task.notes}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = BrandViolet,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
fun DailyHabitsWidget(
    habits: List<HabitEntity>,
    onToggleHabit: (HabitEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = TrackWiseUtils.getTodayString()
    val filteredHabits = remember(habits, today) {
        habits.filter { TrackWiseUtils.shouldShowHabitOnDate(it, today) }
    }
    val completedToday = filteredHabits.count {
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
                        text = "$completedToday/${filteredHabits.size} done",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredHabits.isEmpty()) {
                Text(
                    text = if (habits.isEmpty()) "Configure Habit Runways in the Workspace tab to launch daily streak multipliers." else "No active habit runways scheduled for today.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredHabits.sortedBy {
                        TrackWiseUtils.deserializeStringList(it.daysCompletedJson).contains(today)
                    }.take(5).forEach { habit ->
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
                                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (isDone) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground
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

data class BadgeSpec(
    val days: Int,
    val name: String,
    val medal: String,
    val tier: String,
    val description: String
)
