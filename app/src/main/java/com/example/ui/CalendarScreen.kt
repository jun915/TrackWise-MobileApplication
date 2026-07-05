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

    val todayDateStr = TrackWiseUtils.formatDate(Calendar.getInstance().time, "yyyy-MM-dd")
    var selectedDateStr by remember { mutableStateOf(todayDateStr) }

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

        // --- View Toggles (Day / Month) ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Day", "Month").forEach { viewName ->
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
                    onClick = {
                        currentDate = Calendar.getInstance()
                        selectedDateStr = todayDateStr
                    },
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
                        selectedDateStr = TrackWiseUtils.formatDate(newCal.time, "yyyy-MM-dd")
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
                        selectedDateStr = TrackWiseUtils.formatDate(newCal.time, "yyyy-MM-dd")
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
                                val label = when(mode) {
                                    "islamic" -> "URDU / HIJRI"
                                    "hindu" -> "HINDU"
                                    else -> "NONE"
                                }
                                Row(
                                    modifier = Modifier
                                        .weight(1.5f)
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
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
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
                        .padding(bottom = 12.dp)
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
                        .padding(bottom = 12.dp)
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
                    selectedDateStr = selectedDateStr,
                    onDayClick = { dayStr ->
                        selectedDateStr = dayStr
                        val pDate = TrackWiseUtils.parseDate(dayStr)
                        val cal = Calendar.getInstance()
                        cal.time = pDate
                        currentDate = cal
                    },
                    tasks = tasks,
                    birthdays = birthdays,
                    overlayMode = overlay
                )
            }
        }

        // --- Unified Selected Date Details Card ---
        if (activeView == "month") {
            item {
                val parsedSelected = TrackWiseUtils.parseDate(selectedDateStr)
                val formattedHeader = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(parsedSelected)
                val selectedFestivals = TrackWiseUtils.getIndianFestivalsForDate(selectedDateStr)
                val selectedBirthdays = birthdays.filter { it.date.endsWith(selectedDateStr.substring(5)) }
                val selectedTasks = tasks.filter { TrackWiseUtils.shouldShowTaskOnDate(it, selectedDateStr) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header Date
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formattedHeader,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 12.dp))

                        // 1. Urdu / Hijri details
                        if (overlay == "islamic") {
                            val hijriInfo = TrackWiseUtils.getHijriInfo(selectedDateStr)
                            val urduDateStr = "${TrackWiseUtils.toUrduNumerals(hijriInfo.day)} ${hijriInfo.monthNameUr} ${TrackWiseUtils.toUrduNumerals(hijriInfo.year)} ہجری"
                            val enHijriDate = "${hijriInfo.day} ${hijriInfo.monthNameEn} ${hijriInfo.year} AH"
                            val allahName = TrackWiseUtils.getAllahNameForDate(selectedDateStr)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BrandCyan.copy(alpha = 0.08f))
                                    .border(1.dp, BrandCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Urdu / Hijri Date",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandCyan
                                    )
                                    Text(
                                        text = "اردو اسلامی تاریخ",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandCyan
                                    )
                                }

                                Text(
                                    text = urduDateStr,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                )

                                Text(
                                    text = "Hijri Equivalent: $enHijriDate",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Divine Name of the Day (Day ${allahName.dayNum})",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandCyan.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = "${allahName.ar} / ${allahName.ur}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Meaning: ${allahName.en} — ${allahName.meaning}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // 2. Hindu details
                        if (overlay == "hindu") {
                            val hinduInfo = TrackWiseUtils.getHinduCalendarInfo(selectedDateStr)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BrandPink.copy(alpha = 0.08f))
                                    .border(1.dp, BrandPink.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Hindu Lunar Calendar (Vikram Samvat ${hinduInfo.vsYear})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPink
                                )

                                Text(
                                    text = "Month: ${hinduInfo.vsMonth} · Paksha: ${hinduInfo.paksha}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Tithi: ${hinduInfo.tithi}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BrandPink
                                    )

                                    if (hinduInfo.isPurnima) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(BrandAmber.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("🌕 Purnima (Full Moon)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandAmber)
                                        }
                                    } else if (hinduInfo.isAmavasya) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(BrandRose.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("🌑 Amavasya (New Moon)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandRose)
                                        }
                                    } else if (hinduInfo.tithi == "Ekadashi") {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(BrandViolet.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("🕉️ Ekadashi (Auspicious Fasting)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
                                        }
                                    }
                                }

                                Text(
                                    text = when {
                                        hinduInfo.isPurnima -> "A highly auspicious day in Hindu culture, perfect for spiritual reflection, fasting (Satyanarayan Vrat), and gratitude."
                                        hinduInfo.isAmavasya -> "A day of new beginnings, honoring ancestors (Shradh), introspection, and quiet meditation."
                                        hinduInfo.tithi == "Ekadashi" -> "The eleventh lunar day dedicated to Lord Vishnu. Observers practice complete or partial fasting to cleanse body and mind."
                                        else -> "The current lunar tithi represents the '${hinduInfo.tithi}' stage of the moon's phase in the ${hinduInfo.paksha} paksha."
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // 3. Indian Festivals / Holidays
                        if (selectedFestivals.isNotEmpty()) {
                            Text(
                                text = "FESTIVALS & HOLIDAYS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandRose,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            selectedFestivals.forEach { festival ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BrandRose.copy(alpha = 0.08f))
                                        .border(1.dp, BrandRose.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Celebration, contentDescription = null, tint = BrandRose, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = festival,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandRose
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // 4. Tasks Deadlines
                        if (selectedTasks.isNotEmpty()) {
                            Text(
                                text = "DEADLINES & TASKS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandViolet,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            selectedTasks.forEach { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BrandViolet.copy(alpha = 0.08f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.Circle,
                                        contentDescription = null,
                                        tint = if (task.completed) BrandViolet else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = task.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (task.completed) {
                                        Text("Completed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // 5. Birthdays
                        if (selectedBirthdays.isNotEmpty()) {
                            Text(
                                text = "BIRTHDAYS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPink,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            selectedBirthdays.forEach { bday ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BrandPink.copy(alpha = 0.08f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val icon = when (bday.category) {
                                        "Marriage Anniversary" -> Icons.Default.Favorite
                                        "Death Anniversary" -> Icons.Default.LocalFlorist
                                        else -> Icons.Default.Cake
                                    }
                                    val color = when (bday.category) {
                                        "Marriage Anniversary" -> BrandPink
                                        "Death Anniversary" -> BrandViolet
                                        else -> BrandCyan
                                    }
                                    val label = bday.name + if (bday.category == "Death Anniversary") " 🕯️" else if (bday.category == "Marriage Anniversary") " 💍" else " 🎂"
                                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Empty State for Day events
                        if (selectedFestivals.isEmpty() && selectedTasks.isEmpty() && selectedBirthdays.isEmpty() && overlay == "none") {
                            Text(
                                text = "No personal events or Indian festivals scheduled for this day.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
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
    val todayTasks = tasks.filter { TrackWiseUtils.shouldShowTaskOnDate(it, dayStr) }
    val todayBirthdays = birthdays.filter { it.date.endsWith(dayStr.substring(5)) }
    val festivals = TrackWiseUtils.getIndianFestivalsForDate(dayStr)

    // Separate tasks: all-day (no reminder time) vs timed (has reminder time)
    val allDayTasks = todayTasks.filter { it.reminderTime.isNullOrBlank() }
    val timedTasks = todayTasks.filter { !it.reminderTime.isNullOrBlank() }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Teams Header Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = SimpleDateFormat("EEEE, d MMMM", Locale.US).format(currentDate.time),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Teams Daily Schedule • ${todayTasks.size} tasks",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandViolet.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Teams Style",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandViolet
                    )
                }
            }

            // --- All Day section (Festivals, Birthdays, and All-Day tasks) ---
            if (festivals.isNotEmpty() || todayBirthdays.isNotEmpty() || allDayTasks.isNotEmpty()) {
                Text(
                    text = "ALL-DAY EVENTS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    festivals.forEach { fest ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandRose.copy(alpha = 0.15f))
                                .border(1.dp, BrandRose.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Celebration, contentDescription = null, tint = BrandRose, modifier = Modifier.size(16.dp))
                            Text(text = "Festival: $fest", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandRose)
                        }
                    }

                    todayBirthdays.forEach { bday ->
                        val color = when (bday.category) {
                            "Marriage Anniversary" -> BrandPink
                            "Death Anniversary" -> BrandViolet
                            else -> BrandCyan
                        }
                        val icon = when (bday.category) {
                            "Marriage Anniversary" -> Icons.Default.Favorite
                            "Death Anniversary" -> Icons.Default.LocalFlorist
                            else -> Icons.Default.Cake
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(color.copy(alpha = 0.15f))
                                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                            val bdayTitle = bday.name
                            Text(text = bdayTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                        }
                    }

                    allDayTasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandViolet.copy(alpha = 0.1f))
                                .border(1.dp, BrandViolet.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (task.completed) BrandGreen else BrandViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = task.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (task.completed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BrandViolet.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Task", fontSize = 9.sp, color = BrandViolet, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- 24-Hour Schedule Timeline ---
            Text(
                text = "TIMELINE (24 HRS)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                for (h in 0..23) {
                    val displayHour = when {
                        h == 0 -> "12 AM"
                        h < 12 -> "$h AM"
                        h == 12 -> "12 PM"
                        else -> "${h - 12} PM"
                    }

                    // Find tasks for this hour slot
                    val hourTasks = timedTasks.filter { task ->
                        val time = task.reminderTime
                        if (time != null) {
                            val parts = time.split(":")
                            if (parts.size >= 2) {
                                val hour = parts[0].toIntOrNull()
                                hour == h
                            } else false
                        } else false
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min), // dynamic height fits schedule content
                        verticalAlignment = Alignment.Top
                    ) {
                        // Hour Label Lane
                        Box(
                            modifier = Modifier
                                .width(55.dp)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Text(
                                text = displayHour,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        // Vertical Timeline Line and Content Lane
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Timeline Axis
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                // Vertical line spanning full height of this row
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                                )
                                // Circle dot representing the hour anchor
                                Box(
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (hourTasks.isNotEmpty()) BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
                                )
                            }

                            // Scheduled tasks for this hour
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp)
                            ) {
                                if (hourTasks.isEmpty()) {
                                    // Teams style empty schedule placeholder
                                    Spacer(modifier = Modifier.height(28.dp))
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        hourTasks.forEach { task ->
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (task.completed) {
                                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                                    } else {
                                                        BrandViolet.copy(alpha = 0.12f)
                                                    }
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(
                                                        1.dp,
                                                        if (task.completed) Color.Transparent else BrandViolet.copy(alpha = 0.3f),
                                                        RoundedCornerShape(10.dp)
                                                    )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                            contentDescription = null,
                                                            tint = if (task.completed) BrandGreen else BrandViolet,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Column {
                                                            Text(
                                                                text = task.title,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (task.completed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "Time: ${task.reminderTime}",
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
            val dayTasks = tasks.filter { TrackWiseUtils.shouldShowTaskOnDate(it, dayStr) }
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
                                    Text("OCCASION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = BrandPink)
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
                                val prefix = when (bday.category) {
                                    "Marriage Anniversary" -> "💖 "
                                    "Death Anniversary" -> "🕯️ "
                                    else -> "🎂 "
                                }
                                val color = when (bday.category) {
                                    "Marriage Anniversary" -> BrandPink
                                    "Death Anniversary" -> BrandViolet
                                    else -> BrandCyan
                                }
                                Text(
                                    text = "$prefix${bday.name}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color
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
    selectedDateStr: String,
    onDayClick: (String) -> Unit,
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

                            val hasTasks = tasks.any { TrackWiseUtils.shouldShowTaskOnDate(it, dayStr) }
                            val hasBirthdays = birthdays.any { it.date.endsWith(dayStr.substring(5)) }
                            val festivals = TrackWiseUtils.getIndianFestivalsForDate(dayStr)

                            val isToday = dayStr == todayDateStr
                            val isSelected = dayStr == selectedDateStr

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) BrandViolet.copy(alpha = 0.3f)
                                        else if (isToday) BrandViolet.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else if (isToday) 1.5.dp else 1.dp,
                                        color = if (isSelected) BrandViolet else if (isToday) BrandViolet.copy(alpha = 0.5f) else if (festivals.isNotEmpty()) BrandRose.copy(alpha = 0.4f) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onDayClick(dayStr)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (overlayMode == "islamic") {
                                    Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                                        // Gregorian Day Number (Top Left)
                                        Text(
                                            text = "$dayNumber",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isToday) BrandViolet else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            modifier = Modifier.align(Alignment.TopStart)
                                        )

                                        // Urdu Hijri Day (Center/Bottom Right)
                                        val hijriInfo = TrackWiseUtils.getHijriInfo(dayStr)
                                        Text(
                                            text = TrackWiseUtils.toUrduNumerals(hijriInfo.day),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandCyan,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                } else if (overlayMode == "hindu") {
                                    Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                                        // Gregorian Day Number (Top Left)
                                        Text(
                                            text = "$dayNumber",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isToday) BrandViolet else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            modifier = Modifier.align(Alignment.TopStart)
                                        )

                                        // Hindu Tithi Info
                                        val hinduInfo = TrackWiseUtils.getHinduCalendarInfo(dayStr)
                                        val displayStr = if (hinduInfo.isPurnima) "🌕"
                                            else if (hinduInfo.isAmavasya) "🌑"
                                            else if (hinduInfo.tithi == "Ekadashi") "🕉️"
                                            else hinduInfo.tithi.take(4)

                                        Text(
                                            text = displayStr,
                                            fontSize = if (hinduInfo.isPurnima || hinduInfo.isAmavasya || hinduInfo.tithi == "Ekadashi") 12.sp else 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hinduInfo.isPurnima || hinduInfo.isAmavasya) BrandAmber else BrandPink,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                } else {
                                    // Standard Mode (Gregorian centered + dots)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = "$dayNumber",
                                            fontSize = 11.sp,
                                            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (isToday) BrandViolet else if (festivals.isNotEmpty()) BrandRose else MaterialTheme.colorScheme.onBackground
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            if (hasTasks) {
                                                Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(BrandViolet))
                                            }
                                            if (hasBirthdays) {
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(BrandPink))
                                            }
                                            if (festivals.isNotEmpty()) {
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(BrandRose))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}
