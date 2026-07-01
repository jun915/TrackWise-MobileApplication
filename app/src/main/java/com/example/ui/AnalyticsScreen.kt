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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.utils.TrackWiseUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun AnalyticsScreen(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.allHabits.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    val weightLogs by viewModel.weightEntries.collectAsState()
    val vitals by viewModel.vitalReadings.collectAsState()
    val waterLogs by viewModel.waterLogs.collectAsState()
    val exerciseLogs by viewModel.exerciseLogs.collectAsState()
    val sleepLogs by viewModel.sleepLogs.collectAsState()
    val healthIssues by viewModel.healthIssueLogs.collectAsState()
    val currentUser by viewModel.sessionUser.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- Dashboard Header ---
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = "Analytics",
                        tint = BrandViolet,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Analytics Center",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Interactive rich tracking visualization and health metrics.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
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
                                        .fillMaxHeight(ratio.coerceIn(0.08f, 1f))
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
                                .fillMaxHeight(normalizedVal.coerceIn(0.15f, 1f))
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ReportProblem, contentDescription = null, tint = BrandRose, modifier = Modifier.size(20.dp))
                    Text("SYMPTOM TRACKER TIMELINE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandRose)
                }

                // Severity Filter Buttons
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    listOf("all" to "All", "mild" to "Mild", "moderate" to "Mod", "severe" to "Sev").forEach { (id, label) ->
                        val isSelected = severityFilter == id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) BrandRose else Color.Transparent)
                                .clickable { severityFilter = id }
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
