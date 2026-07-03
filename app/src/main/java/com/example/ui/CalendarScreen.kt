package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BirthdayEntity
import com.example.data.TaskEntity
import com.example.ui.theme.*
import com.example.utils.TrackWiseUtils
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource

@Composable
fun CalendarScreen(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allTasks.collectAsState()
    val birthdays by viewModel.allBirthdays.collectAsState()
    val overlay by viewModel.calendarOverlay.collectAsState()

    var activeView by remember { mutableStateOf("month") } // "day", "week", "month"
    var currentDate by remember { mutableStateOf(Calendar.getInstance()) }

    val todayStr = TrackWiseUtils.formatDate(currentDate.time, "yyyy-MM-dd")
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
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header ---
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Calendar",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "View upcoming deadlines, birthdays, and Indian festivals.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // --- View Toggles (Day / Week / Month) ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Day", "Week", "Month").forEach { viewName ->
                    val mode = viewName.lowercase()
                    val isSelected = activeView == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BrandViolet else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { activeView = mode }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = viewName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // --- Controls: Navigation & View Swapping ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Today trigger
                Button(
                    onClick = { currentDate = Calendar.getInstance() },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Today", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        val newCal = currentDate.clone() as Calendar
                        when (activeView) {
                            "day" -> newCal.add(Calendar.DAY_OF_YEAR, -1)
                            "week" -> newCal.add(Calendar.WEEK_OF_YEAR, -1)
                            "month" -> newCal.add(Calendar.MONTH, -1)
                        }
                        currentDate = newCal
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                    }

                    // Header date string
                    val headerText = when (activeView) {
                        "day" -> SimpleDateFormat("MMM d, yyyy", Locale.US).format(currentDate.time)
                        "week" -> {
                            val first = currentDate.clone() as Calendar
                            first.set(Calendar.DAY_OF_WEEK, first.firstDayOfWeek)
                            val last = first.clone() as Calendar
                            last.add(Calendar.DAY_OF_YEAR, 6)
                            val fStr = SimpleDateFormat("MMM d", Locale.US).format(first.time)
                            val lStr = SimpleDateFormat("MMM d", Locale.US).format(last.time)
                            "$fStr - $lStr"
                        }
                        else -> SimpleDateFormat("MMMM yyyy", Locale.US).format(currentDate.time)
                    }

                    Text(
                        text = headerText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(onClick = {
                        val newCal = currentDate.clone() as Calendar
                        when (activeView) {
                            "day" -> newCal.add(Calendar.DAY_OF_YEAR, 1)
                            "week" -> newCal.add(Calendar.WEEK_OF_YEAR, 1)
                            "month" -> newCal.add(Calendar.MONTH, 1)
                        }
                        currentDate = newCal
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        // --- Calendar Overlay Selector (Only relevant in Month mode) ---
        if (activeView == "month") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CALENDAR OVERLAYS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandCyan,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("none", "islamic", "hindu").forEach { mode ->
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (overlay == mode) BrandCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, if (overlay == mode) BrandCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setCalendarOverlay(mode) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (overlay == mode) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (overlay == mode) BrandCyan else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = mode.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (overlay == mode) BrandCyan else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Islamic Sidebar Info panel ---
        if (overlay == "islamic" && activeView == "month") {
            item {
                val allahName = TrackWiseUtils.getAllahNameForDate(todayStr)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrandCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "آج کا نام — Day ${allahName.dayNum}/100",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandCyan,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${allahName.ar} / ${allahName.ur}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                        Text(
                            text = "Divine Attribute: ${allahName.en} — ${allahName.meaning}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Hijri Date: ${TrackWiseUtils.getHijriDate(todayStr)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandCyan,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // --- Hindu Sidebar Info panel ---
        if (overlay == "hindu" && activeView == "month") {
            item {
                val hinduInfo = TrackWiseUtils.getHinduCalendarInfo(todayStr)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrandPink.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Hindu Lunar Calendar VS ${hinduInfo.vsYear}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPink
                        )
                        Text(
                            text = "Month: ${hinduInfo.vsMonth} · Paksha: ${hinduInfo.paksha}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tithi: ${hinduInfo.tithi}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPink,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            if (hinduInfo.isPurnima) {
                                Text("🌕 Purnima (Full Moon)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandAmber)
                            }
                            if (hinduInfo.isAmavasya) {
                                Text("🌑 Amavasya (New Moon)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandRose)
                            }
                        }
                    }
                }
            }
        }

        // --- Render Specific View (Day / Week / Month Grid) ---
        item {
            when (activeView) {
                "day" -> CalendarDayView(
                    currentDate = currentDate,
                    tasks = tasks,
                    birthdays = birthdays
                )
                "week" -> CalendarWeekView(
                    currentDate = currentDate,
                    tasks = tasks,
                    birthdays = birthdays
                )
                else -> CalendarGrid(
                    currentDate = currentDate,
                    tasks = tasks,
                    birthdays = birthdays,
                    overlayMode = overlay
                )
            }
        }
    }
}

@Composable
fun CalendarDayView(
    currentDate: Calendar,
    tasks: List<TaskEntity>,
    birthdays: List<BirthdayEntity>
) {
    val dayStr = TrackWiseUtils.formatDate(currentDate.time, "yyyy-MM-dd")
    val todayTasks = tasks.filter { it.deadline == dayStr }
    val todayBirthdays = birthdays.filter { it.date.endsWith(dayStr.substring(5)) }
    val festivals = TrackWiseUtils.getIndianFestivalsForDate(dayStr)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(currentDate.time),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BrandViolet
            )

            if (festivals.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandRose.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Celebration, contentDescription = null, tint = BrandRose)
                        Column {
                            Text("Indian Festivals Today", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandRose)
                            Text(festivals.joinToString(", "), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (todayBirthdays.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandPink.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Cake, contentDescription = null, tint = BrandPink)
                        Column {
                            Text("Birthdays Today", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPink)
                            Text(todayBirthdays.joinToString(", ") { it.name }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Text(
                text = "TODAY'S SCHEDULE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            if (todayTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(32.dp))
                        Text("No tasks scheduled for today!", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    todayTasks.forEach { task ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (task.completed) BrandGreen else BrandViolet,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = task.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (task.completed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (task.reminderTime != null) {
                                            Text(
                                                text = "Reminder: ${task.reminderTime}",
                                                fontSize = 11.sp,
                                                color = BrandViolet
                                            )
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (task.priority.lowercase()) {
                                                "high" -> BrandRose.copy(alpha = 0.15f)
                                                "medium" -> BrandAmber.copy(alpha = 0.15f)
                                                else -> BrandCyan.copy(alpha = 0.15f)
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = task.priority.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (task.priority.lowercase()) {
                                            "high" -> BrandRose
                                            "medium" -> BrandAmber
                                            else -> BrandCyan
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
}

@Composable
fun CalendarWeekView(
    currentDate: Calendar,
    tasks: List<TaskEntity>,
    birthdays: List<BirthdayEntity>
) {
    val tempCal = currentDate.clone() as Calendar
    // Set to Sunday of the active week
    tempCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in 0..6) {
            val dayCal = tempCal.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_YEAR, i)
            val dayStr = TrackWiseUtils.formatDate(dayCal.time, "yyyy-MM-dd")
            val dayTasks = tasks.filter { it.deadline == dayStr }
            val dayBirthdays = birthdays.filter { it.date.endsWith(dayStr.substring(5)) }
            val festivals = TrackWiseUtils.getIndianFestivalsForDate(dayStr)
            
            val isToday = dayStr == TrackWiseUtils.formatDate(Calendar.getInstance().time, "yyyy-MM-dd")

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isToday) BrandViolet.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isToday) 2.dp else 1.dp,
                        color = if (isToday) BrandViolet else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isToday) BrandViolet else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = SimpleDateFormat("d", Locale.US).format(dayCal.time),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text(
                                    text = SimpleDateFormat("EEEE", Locale.US).format(dayCal.time),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isToday) BrandViolet else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = SimpleDateFormat("MMMM yyyy", Locale.US).format(dayCal.time),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Badges for Day View
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (festivals.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(BrandRose.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("FESTIVAL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = BrandRose)
                                }
                            }
                            if (dayBirthdays.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(BrandPink.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("BIRTHDAY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = BrandPink)
                                }
                            }
                        }
                    }

                    // Tasks or placeholder
                    if (dayTasks.isNotEmpty() || dayBirthdays.isNotEmpty() || festivals.isNotEmpty()) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            festivals.forEach { festival ->
                                Text(
                                    text = "🎉 Indian Festival: $festival",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandRose
                                )
                            }
                            dayBirthdays.forEach { bday ->
                                Text(
                                    text = "🎂 Birthday: ${bday.name}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPink
                                )
                            }
                            dayTasks.take(3).forEach { task ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (task.completed) BrandGreen else BrandViolet,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = task.title,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        color = if (task.completed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (dayTasks.size > 3) {
                                Text(
                                    text = "+ ${dayTasks.size - 3} more tasks",
                                    fontSize = 11.sp,
                                    color = BrandViolet,
                                    modifier = Modifier.padding(start = 20.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No events scheduled",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.padding(top = 6.dp, start = 40.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(
    currentDate: Calendar,
    tasks: List<TaskEntity>,
    birthdays: List<BirthdayEntity>,
    overlayMode: String,
    modifier: Modifier = Modifier
) {
    val tempCal = currentDate.clone() as Calendar
    tempCal.set(Calendar.DAY_OF_MONTH, 1)

    val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")

    // Get today's actual date string to highlight
    val todayCal = Calendar.getInstance()
    val todayDateStr = TrackWiseUtils.formatDate(todayCal.time, "yyyy-MM-dd")

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Weekday labels
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                weekDays.forEach { day ->
                    Text(
                        text = day,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandViolet,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calculate grid rows
            val totalCells = daysInMonth + (firstDayOfWeek - 1)
            val numRows = (totalCells + 6) / 7

            for (row in 0 until numRows) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 1..7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - (firstDayOfWeek - 1)

                        if (dayNumber in 1..daysInMonth) {
                            val temp = currentDate.clone() as Calendar
                            temp.set(Calendar.DAY_OF_MONTH, dayNumber)
                            val dayStr = TrackWiseUtils.formatDate(temp.time, "yyyy-MM-dd")

                            val hasTasks = tasks.any { it.deadline == dayStr }
                            val hasBirthdays = birthdays.any { it.date.endsWith(dayStr.substring(5)) }
                            val festivals = TrackWiseUtils.getIndianFestivalsForDate(dayStr)

                            val isToday = dayStr == todayDateStr

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isToday) BrandViolet.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        width = if (isToday) 2.dp else 1.dp,
                                        color = if (isToday) BrandViolet else if (festivals.isNotEmpty()) BrandRose.copy(alpha = 0.5f) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$dayNumber",
                                        fontSize = 11.sp,
                                        fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (isToday) BrandViolet else if (festivals.isNotEmpty()) BrandRose else MaterialTheme.colorScheme.onBackground
                                    )

                                    // Dynamic overlay indicators
                                    if (overlayMode == "islamic") {
                                        val allahName = TrackWiseUtils.getAllahNameForDate(dayStr)
                                        Text(
                                            text = allahName.ar,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = BrandCyan,
                                            maxLines = 1
                                        )
                                    } else if (overlayMode == "hindu") {
                                        val hinduInfo = TrackWiseUtils.getHinduCalendarInfo(dayStr)
                                        Text(
                                            text = hinduInfo.tithi.take(4),
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = BrandPink,
                                            maxLines = 1
                                        )
                                    } else {
                                        // Standard simple dot indicator
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            if (hasTasks) {
                                                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(BrandViolet))
                                            }
                                            if (hasBirthdays) {
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(BrandPink))
                                            }
                                            if (festivals.isNotEmpty()) {
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(BrandRose))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Empty cells representing offset/out of month cells
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}
