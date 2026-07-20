package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.utils.TrackWiseUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAddTaskBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAddTask: (title: String, desc: String, project: String, priority: String, deadline: String, reminderTime: String?, repeatType: String) -> Unit
) {
    if (!visible) return

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var project by remember { mutableStateOf("Inbox") }
    var priority by remember { mutableStateOf("none") } // "none", "low", "medium", "high"
    
    val todayStr = remember { TrackWiseUtils.getTodayString() }
    var deadline by remember { mutableStateOf(todayStr) }
    var reminderTime by remember { mutableStateOf<String?>(null) }
    var repeatType by remember { mutableStateOf("none") }

    var showDatePicker by remember { mutableStateOf(false) }
    var priorityMenuExpanded by remember { mutableStateOf(false) }
    var projectMenuExpanded by remember { mutableStateOf(false) }
    var tagMenuExpanded by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Sheet Content Container
        Card(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Consume clicks to avoid closing
                )
                .navigationBarsPadding()
                .imePadding()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                        .align(Alignment.CenterHorizontally)
                )

                // 1. Title Input
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(BrandCyan),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("task_title_input"),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (title.isEmpty()) {
                                Text(
                                    text = "What would you like to do?",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // 2. Description Input
                BasicTextField(
                    value = description,
                    onValueChange = { description = it },
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    ),
                    cursorBrush = SolidColor(BrandCyan),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_description_input"),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (description.isEmpty()) {
                                Text(
                                    text = "Description",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 3. Action Buttons & Icons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // A. Calendar / Date Pill
                        val dateText = remember(deadline, todayStr) {
                            if (deadline == todayStr) "Today"
                            else {
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                try {
                                    val target = sdf.parse(deadline)
                                    val current = sdf.parse(todayStr)
                                    if (target != null && current != null) {
                                        val diff = (target.time - current.time) / (1000 * 60 * 60 * 24)
                                        if (diff == 1L) "Tomorrow"
                                        else {
                                            val outFormat = SimpleDateFormat("MMM d", Locale.US)
                                            val formattedDate = outFormat.format(target)
                                            if (diff > 1) "$formattedDate, ${diff}d left"
                                            else formattedDate
                                        }
                                    } else deadline
                                } catch (e: Exception) {
                                    deadline
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BrandCyan.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, BrandCyan.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .clickable { showDatePicker = true }
                                .testTag("task_date_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Select Date",
                                    tint = BrandCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = dateText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandCyan
                                )
                            }
                        }

                        // B. Priority Flag Icon
                        Box {
                            val flagColor = when (priority) {
                                "high" -> BrandRose
                                "medium" -> BrandOrange
                                "low" -> BrandCyan
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                            IconButton(
                                onClick = { priorityMenuExpanded = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (priority != "none") flagColor.copy(alpha = 0.12f) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = if (priority != "none") 1.dp else 0.dp,
                                        color = if (priority != "none") flagColor.copy(alpha = 0.25f) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .testTag("priority_flag_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = "Priority",
                                    tint = flagColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = priorityMenuExpanded,
                                onDismissRequest = { priorityMenuExpanded = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            ) {
                                listOf(
                                    Triple("high", "High Priority", BrandRose),
                                    Triple("medium", "Medium Priority", BrandOrange),
                                    Triple("low", "Low Priority", BrandCyan),
                                    Triple("none", "No Priority", MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                ).forEach { (key, label, color) ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.Flag, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                                                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            }
                                        },
                                        onClick = {
                                            priority = key
                                            priorityMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // C. Tag Label Icon
                        Box {
                            IconButton(
                                onClick = { tagMenuExpanded = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Label,
                                    contentDescription = "Tags",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = tagMenuExpanded,
                                onDismissRequest = { tagMenuExpanded = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            ) {
                                listOf("#daily routine", "#DailyRoutine", "#work", "#fitness", "#learning").forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t, fontSize = 13.sp) },
                                        onClick = {
                                            if (!title.contains(t)) {
                                                title = if (title.isBlank()) t else "$title $t"
                                            }
                                            tagMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // D. List Selection Icon
                        Box {
                            val projectIcon = when (project.lowercase()) {
                                "work" -> Icons.Default.BusinessCenter
                                "personal" -> Icons.Default.Person
                                "shopping" -> Icons.Default.ShoppingCart
                                "learning" -> Icons.Default.School
                                "wish list" -> Icons.Default.Star
                                "fitness" -> Icons.Default.DirectionsRun
                                else -> Icons.Default.Inbox
                            }
                            IconButton(
                                onClick = { projectMenuExpanded = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(BrandViolet.copy(alpha = 0.12f), CircleShape)
                                    .border(1.dp, BrandViolet.copy(alpha = 0.2f), CircleShape)
                                    .testTag("project_select_button")
                            ) {
                                Icon(
                                    imageVector = projectIcon,
                                    contentDescription = "Project / List",
                                    tint = BrandViolet,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = projectMenuExpanded,
                                onDismissRequest = { projectMenuExpanded = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            ) {
                                listOf("Inbox", "Work", "Personal", "Shopping", "Learning", "Wish List", "Fitness", "Welcome").forEach { proj ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(proj, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                if (project == proj) {
                                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = BrandCyan, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        },
                                        onClick = {
                                            project = proj
                                            projectMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // E. Ellipses (More) Icon
                        IconButton(
                            onClick = { /* Additional advanced settings could go here */ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // F. Send/Add Button
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onAddTask(title, description, project, priority, deadline, reminderTime, repeatType)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("task_submit_button"),
                        enabled = title.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Save Task",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Sub-sheet for custom Date Picker
        CustomDatePickerSheet(
            visible = showDatePicker,
            currentDate = deadline,
            currentTime = reminderTime,
            currentReminder = if (reminderTime != null) "On the day" else "None",
            currentRepeat = repeatType,
            onDismiss = { showDatePicker = false },
            onConfirm = { date, time, reminder, repeat ->
                deadline = date
                reminderTime = time
                repeatType = repeat
                showDatePicker = false
            }
        )
    }
}

@Composable
fun CustomDatePickerSheet(
    visible: Boolean,
    currentDate: String,
    currentTime: String?,
    currentReminder: String,
    currentRepeat: String,
    onDismiss: () -> Unit,
    onConfirm: (date: String, time: String?, reminder: String, repeat: String) -> Unit
) {
    if (!visible) return

    var activeTab by remember { mutableStateOf("date") } // "date" or "duration"
    var selectedDateStr by remember { mutableStateOf(currentDate) }
    var selectedTimeStr by remember { mutableStateOf(currentTime) }
    var selectedReminder by remember { mutableStateOf(currentReminder) }
    var selectedRepeat by remember { mutableStateOf(currentRepeat) }

    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf(false) }

    val calendar = remember(selectedDateStr) {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            val d = sdf.parse(selectedDateStr)
            if (d != null) cal.time = d
        } catch (e: Exception) {}
        cal
    }

    // Month Navigation State
    var monthOffset by remember { mutableStateOf(0) }
    val displayCalendar = remember(monthOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        cal
    }
    val monthName = remember(displayCalendar) {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(displayCalendar.time)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    // Tabs
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { activeTab = "date" }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                "Date",
                                fontSize = 16.sp,
                                fontWeight = if (activeTab == "date") FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == "date") BrandCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (activeTab == "date") {
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(2.dp)
                                        .background(BrandCyan)
                                        .padding(top = 2.dp)
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { activeTab = "duration" }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                "Duration",
                                fontSize = 16.sp,
                                fontWeight = if (activeTab == "duration") FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == "duration") BrandCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (activeTab == "duration") {
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(2.dp)
                                        .background(BrandCyan)
                                        .padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            onConfirm(selectedDateStr, selectedTimeStr, selectedReminder, selectedRepeat)
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Confirm", tint = BrandCyan)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // Scrollable content inside Dialog
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick Action pills row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick pills helper
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val today = Calendar.getInstance()
                        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                        val nextMonday = Calendar.getInstance().apply {
                            while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                        }

                        listOf(
                            Triple("Today", today, Icons.Default.CalendarToday),
                            Triple("Tomorrow", tomorrow, Icons.Default.WbSunny),
                            Triple("Next Mon", nextMonday, Icons.Default.Event),
                            Triple("Tom Morning", tomorrow, Icons.Default.LightMode)
                        ).forEach { (label, cal, icon) ->
                            val isSelected = selectedDateStr == sdf.format(cal.time) &&
                                    (label != "Tom Morning" || selectedTimeStr == "09:00")

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) BrandCyan else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSelected) BrandCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedDateStr = sdf.format(cal.time)
                                        if (label == "Tom Morning") {
                                            selectedTimeStr = "09:00"
                                            selectedReminder = "On the day"
                                        }
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Monthly Calendar Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = monthName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { monthOffset-- },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                            }
                            IconButton(
                                onClick = { monthOffset++ },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                            }
                        }
                    }

                    // Calendar Grid (Compose Native simple grid to fit 100%)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Weekday headers
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Generate Calendar days grid padding
                        val firstDayCal = displayCalendar.clone() as Calendar
                        firstDayCal.set(Calendar.DAY_OF_MONTH, 1)
                        val startDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun, 1=Mon...
                        val maxDays = displayCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

                        val prevMonthCal = displayCalendar.clone() as Calendar
                        prevMonthCal.add(Calendar.MONTH, -1)
                        val maxPrevDays = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

                        val daysList = remember(monthOffset) {
                            val list = mutableListOf<Triple<Int, Boolean, String>>() // day, isCurrentMonth, fullDateString
                            
                            val sdfFull = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            
                            // Prev Month padding
                            for (i in startDayOfWeek - 1 downTo 0) {
                                val d = maxPrevDays - i
                                val tempCal = prevMonthCal.clone() as Calendar
                                tempCal.set(Calendar.DAY_OF_MONTH, d)
                                list.add(Triple(d, false, sdfFull.format(tempCal.time)))
                            }
                            // Current Month
                            for (d in 1..maxDays) {
                                val tempCal = displayCalendar.clone() as Calendar
                                tempCal.set(Calendar.DAY_OF_MONTH, d)
                                list.add(Triple(d, true, sdfFull.format(tempCal.time)))
                            }
                            // Next Month padding
                            val remaining = 42 - list.size
                            for (d in 1..remaining) {
                                val tempCal = displayCalendar.clone() as Calendar
                                tempCal.add(Calendar.MONTH, 1)
                                tempCal.set(Calendar.DAY_OF_MONTH, d)
                                list.add(Triple(d, false, sdfFull.format(tempCal.time)))
                            }
                            list
                        }

                        // Chunk into weeks
                        daysList.chunked(7).forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                week.forEach { (dayNum, isCurrent, fullDateStr) ->
                                    val isSelected = selectedDateStr == fullDateStr
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1.1f)
                                            .clip(CircleShape)
                                            .background(if (isSelected) BrandCyan else Color.Transparent)
                                            .clickable {
                                                selectedDateStr = fullDateStr
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = dayNum.toString(),
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White
                                                        else if (isCurrent) MaterialTheme.colorScheme.onSurface
                                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Detail options row listings: Time, Reminder, Repeat
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            // 1. Time Selection Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTimePickerDialog = true }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(20.dp))
                                    Text("Time", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedTimeStr ?: "None",
                                        fontSize = 13.sp,
                                        color = if (selectedTimeStr != null) BrandCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))

                            // 2. Reminder Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showReminderDialog = true }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(20.dp))
                                    Text("Reminder", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedReminder,
                                        fontSize = 13.sp,
                                        color = if (selectedReminder != "None") BrandCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))

                            // 3. Repeat Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showRepeatDialog = true }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Repeat, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(20.dp))
                                    Text("Repeat", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedRepeat.replaceFirstChar { it.uppercase() },
                                        fontSize = 13.sp,
                                        color = if (selectedRepeat != "none") BrandCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Time Picker Modal Dialog
    if (showTimePickerDialog) {
        val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
        val currentMin = remember { Calendar.getInstance().get(Calendar.MINUTE) }
        
        var hourVal by remember { mutableStateOf(currentHour) }
        var minVal by remember { mutableStateOf(currentMin) }

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text("Select Time", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Simple custom spinner layout for time selection
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { hourVal = (hourVal + 1) % 24 }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up")
                        }
                        Text(String.format("%02d", hourVal), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BrandCyan)
                        IconButton(onClick = { hourVal = if (hourVal == 0) 23 else hourVal - 1 }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down")
                        }
                    }
                    Text(" : ", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BrandCyan, modifier = Modifier.padding(horizontal = 12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { minVal = (minVal + 5) % 60 }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up")
                        }
                        Text(String.format("%02d", minVal), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BrandCyan)
                        IconButton(onClick = { minVal = if (minVal < 5) 55 else minVal - 5 }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTimeStr = String.format("%02d:%02d", hourVal, minVal)
                        showTimePickerDialog = false
                    }
                ) {
                    Text("OK", color = BrandCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedTimeStr = null
                        showTimePickerDialog = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // Reminder Option Selector Dialog
    if (showReminderDialog) {
        val reminderOptions = listOf("None", "On the day", "1 day early", "2 days early", "3 days early", "1 week early")
        var localReminderSelection by remember { mutableStateOf(selectedReminder) }
        var constantReminderEnabled by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Reminder", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reminderOptions.forEach { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { localReminderSelection = opt }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(opt, fontSize = 14.sp)
                            if (localReminderSelection == opt) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = BrandCyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(vertical = 6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Constant Reminder 👑", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = constantReminderEnabled,
                            onCheckedChange = { constantReminderEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrandCyan, checkedTrackColor = BrandCyan.copy(alpha = 0.3f))
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedReminder = localReminderSelection
                        showReminderDialog = false
                    }
                ) {
                    Text("OK", color = BrandCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Repeat Selection Dialog
    if (showRepeatDialog) {
        val repeatOptions = listOf(
            "none" to "None",
            "daily" to "Daily",
            "weekly" to "Weekly (Mon)",
            "monthly" to "Monthly",
            "yearly" to "Yearly",
            "weekdays" to "Every Weekday (Mon-Fri)"
        )
        var localRepeatSelection by remember { mutableStateOf(selectedRepeat) }

        AlertDialog(
            onDismissRequest = { showRepeatDialog = false },
            title = { Text("Repeat", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeatOptions.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { localRepeatSelection = key }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontSize = 14.sp)
                            if (localRepeatSelection == key) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = BrandCyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedRepeat = localRepeatSelection
                        showRepeatDialog = false
                    }
                ) {
                    Text("OK", color = BrandCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRepeatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
