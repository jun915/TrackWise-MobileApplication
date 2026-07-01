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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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

    val sdfMonthHeader = SimpleDateFormat("MM000 yyyy", Locale.US) // Will format manually for simplicity
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

        // --- Controls: Navigation & View Swapping (Section 11.2) ---
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
                        if (activeView == "month") newCal.add(Calendar.MONTH, -1) else newCal.add(Calendar.WEEK_OF_YEAR, -1)
                        currentDate = newCal
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                    }

                    // Simple header date string
                    val headerMonth = SimpleDateFormat("MMMM yyyy", Locale.US).format(currentDate.time)
                    Text(
                        text = headerMonth,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(onClick = {
                        val newCal = currentDate.clone() as Calendar
                        if (activeView == "month") newCal.add(Calendar.MONTH, 1) else newCal.add(Calendar.WEEK_OF_YEAR, 1)
                        currentDate = newCal
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        // --- Calendar Overlay Selector (Section 11.6) ---
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

        // --- Islamic Sidebar Info panel (Section 11.6 Option A) ---
        if (overlay == "islamic") {
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

        // --- Hindu Sidebar Info panel (Section 11.6 Option B) ---
        if (overlay == "hindu") {
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

        // --- Traditional Month Calendar Grid (Section 11.1 & 11.7) ---
        item {
            CalendarGrid(
                currentDate = currentDate,
                tasks = tasks,
                birthdays = birthdays,
                overlayMode = overlay
            )
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
    
    // Find first day of month (1-indexed, e.g. Sunday=1, Monday=2)
    val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
    
    val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")

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

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .border(
                                        1.dp,
                                        if (festivals.isNotEmpty()) BrandRose.copy(alpha = 0.5f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$dayNumber",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (festivals.isNotEmpty()) BrandRose else MaterialTheme.colorScheme.onBackground
                                    )

                                    // Dynamic overlay indicators
                                    if (overlayMode == "islamic") {
                                        val allahName = TrackWiseUtils.getAllahNameForDate(dayStr)
                                        Text(
                                            text = allahName.ar, // Full Arabic name (preserves correct spelling and word-shaping)
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
                                        // Standard simple dot indicator (Section 11.3)
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
