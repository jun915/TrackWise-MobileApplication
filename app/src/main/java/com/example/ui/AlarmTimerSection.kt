package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AlarmEntity
import com.example.ui.theme.*
import com.example.utils.TrackWiseUtils
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun AlarmTimerSection(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    var selectedUtilityTab by remember { mutableStateOf(0) } // 0 = Timer, 1 = Stopwatch
    val utilityTabs = listOf("Timer ⏱️", "Stopwatch ⏳")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Inner Segmented Controls ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            utilityTabs.forEachIndexed { index, title ->
                val isSelected = selectedUtilityTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) BrandViolet else Color.Transparent)
                        .clickable { selectedUtilityTab = index }
                        .padding(vertical = 10.dp)
                        .testTag("utility_tab_$index"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // --- Active Subscreen Content ---
        AnimatedContent(
            targetState = selectedUtilityTab,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "utility_animation"
        ) { tabIndex ->
            when (tabIndex) {
                0 -> TimerSubSection()
                1 -> StopwatchSubSection()
            }
        }
    }
}

// ==========================================
// 1. ALARMS SUBSECTION
// ==========================================
@Composable
fun AlarmsSubSection(viewModel: TrackWiseViewModel) {
    val alarms by viewModel.allAlarms.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmEntity?>(null) }

    // Check if any enabled alarm is currently ringing
    var activeRingingAlarm by remember { mutableStateOf<AlarmEntity?>(null) }
    
    // Realtime background check for simulated alarm trigger
    LaunchedEffect(alarms) {
        while (true) {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val currentDayNum = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
            val currentDayStr = when (currentDayNum) {
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"
                Calendar.SUNDAY -> "Sun"
                else -> ""
            }

            val ringing = alarms.firstOrNull { alarm ->
                alarm.isEnabled && 
                alarm.hour == currentHour && 
                alarm.minute == currentMinute &&
                (alarm.repeatDaysJson == "[]" || alarm.repeatDaysJson.contains(currentDayStr))
            }
            if (ringing != null && activeRingingAlarm == null) {
                activeRingingAlarm = ringing
                viewModel.playAlarmSound()
            }
            delay(5000) // Check every 5 seconds
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header card with custom action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Alarms",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_alarm_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Alarm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (alarms.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessAlarms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Alarms Set",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Create an alarm to stay on schedule.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            alarms.forEach { alarm ->
                AlarmItemCard(
                    alarm = alarm,
                    onToggle = { viewModel.toggleAlarm(alarm) },
                    onDelete = { viewModel.deleteAlarm(alarm.id) },
                    onClick = { editingAlarm = alarm }
                )
            }
        }

        // Active Ringing Overlay/Dialog
        activeRingingAlarm?.let { alarm ->
            Dialog(onDismissRequest = { 
                viewModel.stopAlarmSound()
                activeRingingAlarm = null 
            }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, BrandViolet, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(BrandViolet.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AlarmOn,
                                contentDescription = null,
                                tint = BrandViolet,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        val amPmBanner = if (alarm.hour >= 12) "PM" else "AM"
                        val displayHourBanner = when {
                            alarm.hour == 0 -> 12
                            alarm.hour > 12 -> alarm.hour - 12
                            else -> alarm.hour
                        }
                        Text(
                            text = String.format("%d:%02d %s", displayHourBanner, alarm.minute, amPmBanner),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = alarm.label,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.snoozeAlarm(alarm)
                                    viewModel.stopAlarmSound()
                                    activeRingingAlarm = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, BrandPink)
                            ) {
                                Text("Snooze (+5m)", color = BrandPink, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    viewModel.dismissAlarm(alarm)
                                    viewModel.stopAlarmSound()
                                    activeRingingAlarm = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Dismiss", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Create Alarm Dialog
        if (showAddDialog) {
            AddAlarmDialog(
                onDismiss = { showAddDialog = false },
                onSave = { hour, minute, label, repeatDays ->
                    viewModel.addAlarm(hour, minute, label, repeatDays)
                    showAddDialog = false
                }
            )
        }

        // Edit Alarm Dialog
        if (editingAlarm != null) {
            AddAlarmDialog(
                existingAlarm = editingAlarm,
                onDismiss = { editingAlarm = null },
                onSave = { hour, minute, label, repeatDays ->
                    viewModel.updateAlarm(editingAlarm!!.id, hour, minute, label, repeatDays)
                    editingAlarm = null
                }
            )
        }
    }
}

@Composable
fun AlarmItemCard(
    alarm: AlarmEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val displayTime = String.format("%02d:%02d", alarm.hour, alarm.minute)
    val amPmStr = if (alarm.hour >= 12) "PM" else "AM"
    val displayHour12 = when {
        alarm.hour == 0 -> 12
        alarm.hour > 12 -> alarm.hour - 12
        else -> alarm.hour
    }
    val formattedTime12 = String.format("%d:%02d %s", displayHour12, alarm.minute, amPmStr)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = if (alarm.isEnabled) BrandViolet.copy(alpha = 0.3f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = alarm.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (alarm.isEnabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formattedTime12,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = if (alarm.isEnabled) BrandViolet else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${displayTime})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BrandViolet,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_alarm_${alarm.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete alarm",
                            tint = BrandRose,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Days of week indicators
            val activeDays = remember(alarm.repeatDaysJson) {
                TrackWiseUtils.deserializeStringList(alarm.repeatDaysJson)
            }
            val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allDays.forEach { day ->
                    val isDayActive = activeDays.contains(day)
                    Box(
                        modifier = Modifier
                            .size(width = 38.dp, height = 24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isDayActive && alarm.isEnabled) BrandViolet.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDayActive && alarm.isEnabled) BrandViolet.copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.take(3),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDayActive && alarm.isEnabled) BrandViolet else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddAlarmDialog(
    existingAlarm: AlarmEntity? = null,
    onDismiss: () -> Unit,
    onSave: (hour: Int, minute: Int, label: String, repeatDays: List<String>) -> Unit
) {
    var hour by remember { mutableStateOf(existingAlarm?.hour ?: 8) }
    var minute by remember { mutableStateOf(existingAlarm?.minute ?: 0) }
    var label by remember { mutableStateOf(existingAlarm?.label ?: "") }
    
    var amPm by remember { mutableStateOf(if (hour >= 12) "PM" else "AM") }
    var displayHour by remember { 
        val h = hour % 12
        mutableStateOf(if (h == 0) "12" else h.toString()) 
    }
    var displayMinute by remember { mutableStateOf(String.format("%02d", minute)) }

    var showErrors by remember { mutableStateOf(false) }
    val displayHourInt = displayHour.toIntOrNull()
    val displayMinuteInt = displayMinute.toIntOrNull()
    val hourError = if (displayHour.isBlank()) "Required" else if (displayHourInt == null || displayHourInt !in 1..12) "Use 1-12" else null
    val minuteError = if (displayMinute.isBlank()) "Required" else if (displayMinuteInt == null || displayMinuteInt !in 0..59) "Use 0-59" else null

    val convertTo24Hour = { h12: Int, amPmStr: String ->
        var h = h12 % 12
        if (amPmStr == "PM") {
            h += 12
        }
        h
    }

    val selectedDays = remember {
        val daysList = if (existingAlarm != null) {
            TrackWiseUtils.deserializeStringList(existingAlarm.repeatDaysJson)
        } else {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri")
        }
        mutableStateListOf<String>().apply { addAll(daysList) }
    }
    val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (existingAlarm != null) "Edit Alarm" else "Add Custom Alarm",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Beautiful Hour & Minute Input Boxes with AM/PM selector
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour Box
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hour", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 4.dp))
                        OutlinedTextField(
                            value = displayHour,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }
                                if (filtered.length <= 2) {
                                    displayHour = filtered
                                    showErrors = false
                                    val h = filtered.toIntOrNull() ?: 12
                                    if (h in 1..12) {
                                        hour = convertTo24Hour(h, amPm)
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = showErrors && hourError != null,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                            modifier = Modifier.size(width = 72.dp, height = 56.dp).testTag("alarm_hour_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandViolet,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                errorBorderColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }

                    Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp, top = 16.dp, end = 12.dp, bottom = 0.dp))

                    // Minute Box
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minute", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 4.dp))
                        OutlinedTextField(
                            value = displayMinute,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }
                                if (filtered.length <= 2) {
                                    displayMinute = filtered
                                    showErrors = false
                                    val m = filtered.toIntOrNull() ?: 0
                                    if (m in 0..59) {
                                        minute = m
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = showErrors && minuteError != null,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                            modifier = Modifier.size(width = 72.dp, height = 56.dp).testTag("alarm_minute_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandViolet,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                errorBorderColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // AM/PM Selection
                    Column(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("AM", "PM").forEach { opt ->
                            val isSelected = amPm == opt
                            Box(
                                modifier = Modifier
                                    .size(width = 54.dp, height = 28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) BrandViolet else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        amPm = opt
                                        val h = displayHour.toIntOrNull() ?: 12
                                        hour = convertTo24Hour(h, opt)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Label field
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Alarm Label (e.g., Morning Meditation)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandViolet,
                        focusedLabelColor = BrandViolet
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("alarm_label_input")
                )

                // Day Selection Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Repeat Days",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        allDays.forEach { day ->
                            val isSelected = selectedDays.contains(day)
                            Box(
                                modifier = Modifier
                                    .size(width = 38.dp, height = 30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) BrandViolet else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable {
                                        if (isSelected) selectedDays.remove(day) else selectedDays.add(day)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.take(1),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                if (showErrors && (hourError != null || minuteError != null)) {
                    Text(
                        text = "Please correct errors: " + (hourError ?: "") + " " + (minuteError ?: ""),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                    Button(
                        onClick = { 
                            if (hourError == null && minuteError == null) {
                                val finalHour = displayHourInt ?: 12
                                val finalMinute = displayMinuteInt ?: 0
                                onSave(convertTo24Hour(finalHour, amPm), finalMinute, label, selectedDays.toList()) 
                            } else {
                                showErrors = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("save_alarm")
                    ) {
                        Text("Save Alarm", color = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. TIMER SUBSECTION
// ==========================================
@Composable
fun TimerSubSection() {
    var hours by remember { mutableStateOf(0) }
    var minutes by remember { mutableStateOf(10) }
    var seconds by remember { mutableStateOf(0) }

    var totalDurationSeconds by remember { mutableStateOf(0L) }
    var remainingSeconds by remember { mutableStateOf(0L) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Start timer process
    LaunchedEffect(isTimerRunning, remainingSeconds) {
        if (isTimerRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
            if (remainingSeconds == 0L) {
                isTimerRunning = false
            }
        } else {
            isTimerRunning = false
        }
    }

    val progress = if (totalDurationSeconds > 0) {
        remainingSeconds.toFloat() / totalDurationSeconds.toFloat()
    } else {
        0f
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Modern Focus Timer",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier.size(190.dp),
            contentAlignment = Alignment.Center
        ) {
            // Circle Progress Ring
            Canvas(modifier = Modifier.size(190.dp)) {
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.15f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = BrandCyan,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Big Clock Readout
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isTimerRunning || remainingSeconds > 0) {
                    val dispHours = remainingSeconds / 3600
                    val dispMins = (remainingSeconds % 3600) / 60
                    val dispSecs = remainingSeconds % 60
                    Text(
                        text = if (dispHours > 0) {
                            String.format("%02d:%02d:%02d", dispHours, dispMins, dispSecs)
                        } else {
                            String.format("%02d:%02d", dispMins, dispSecs)
                        },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "REMAINING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                } else {
                    Icon(
                        Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = BrandCyan,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "READY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandCyan
                    )
                }
            }
        }

        if (!isTimerRunning && remainingSeconds == 0L) {
            // Input pickers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Hours Picker
                TimerUnitSelector(
                    label = "HR",
                    value = hours,
                    onIncrement = { if (hours < 99) hours++ },
                    onDecrement = { if (hours > 0) hours-- },
                    modifier = Modifier.weight(1f)
                )

                // Minutes Picker
                TimerUnitSelector(
                    label = "MIN",
                    value = minutes,
                    onIncrement = { if (minutes < 59) minutes++ else minutes = 0 },
                    onDecrement = { if (minutes > 0) minutes-- else minutes = 59 },
                    modifier = Modifier.weight(1f)
                )

                // Seconds Picker
                TimerUnitSelector(
                    label = "SEC",
                    value = seconds,
                    onIncrement = { if (seconds < 59) seconds++ else seconds = 0 },
                    onDecrement = { if (seconds > 0) seconds-- else seconds = 59 },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Running State pulse indicator
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandCyan.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(BrandCyan)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isTimerRunning) "Focus Loop Active" else "Timer Paused",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandCyan
                )
            }
        }

        // Control Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isTimerRunning || remainingSeconds > 0) {
                // Stop/Pause & Reset controls
                OutlinedButton(
                    onClick = {
                        isTimerRunning = false
                        remainingSeconds = 0L
                        totalDurationSeconds = 0L
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, BrandRose)
                ) {
                    Text("Reset", color = BrandRose, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { isTimerRunning = !isTimerRunning },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTimerRunning) BrandAmber else BrandCyan
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isTimerRunning) "Pause" else "Resume",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                // Start button
                val totalSecs = (hours * 3600) + (minutes * 60) + seconds
                Button(
                    onClick = {
                        if (totalSecs > 0) {
                            totalDurationSeconds = totalSecs.toLong()
                            remainingSeconds = totalSecs.toLong()
                            isTimerRunning = true
                        }
                    },
                    enabled = totalSecs > 0,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Timer", fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun TimerUnitSelector(
    label: String,
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = String.format("%02d", value),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(onClick = onIncrement, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}


// ==========================================
// 3. STOPWATCH SUBSECTION
// ==========================================
@Composable
fun StopwatchSubSection() {
    var timeElapsedMs by remember { mutableStateOf(0L) }
    var isStopwatchRunning by remember { mutableStateOf(false) }
    val laps = remember { mutableStateListOf<Long>() }

    // Coroutine updates the stopwatch with milliseconds precision
    LaunchedEffect(isStopwatchRunning) {
        if (isStopwatchRunning) {
            var lastTime = System.currentTimeMillis()
            while (isStopwatchRunning) {
                delay(10) // 10ms ticks for high precision centiseconds
                val now = System.currentTimeMillis()
                timeElapsedMs += (now - lastTime)
                lastTime = now
            }
        }
    }

    // Centiseconds extraction
    val minutes = (timeElapsedMs / 60000) % 60
    val seconds = (timeElapsedMs / 1000) % 60
    val centiseconds = (timeElapsedMs / 10) % 100

    val stopwatchStr = String.format("%02d:%02d.%02d", minutes, seconds, centiseconds)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "High Precision Stopwatch",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        // Stopwatch Visual Board
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stopwatchStr,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = BrandViolet
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "MINUTES : SECONDS . CENTISECONDS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (timeElapsedMs > 0) {
                // Clear / Reset
                OutlinedButton(
                    onClick = {
                        isStopwatchRunning = false
                        timeElapsedMs = 0L
                        laps.clear()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, BrandRose)
                ) {
                    Text("Reset", color = BrandRose, fontWeight = FontWeight.Bold)
                }

                // Lap record
                if (isStopwatchRunning) {
                    Button(
                        onClick = { laps.add(timeElapsedMs) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Lap 🚩", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Start / Pause
            Button(
                onClick = { isStopwatchRunning = !isStopwatchRunning },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isStopwatchRunning) BrandAmber else BrandViolet
                ),
                modifier = Modifier.weight(if (timeElapsedMs > 0) 1f else 2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isStopwatchRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isStopwatchRunning) "Pause" else "Start",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Lap Times list
        if (laps.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Lap Splits",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        laps.asReversed().forEachIndexed { rawIndex, lapTime ->
                            val index = laps.size - rawIndex
                            val lapMin = (lapTime / 60000) % 60
                            val lapSec = (lapTime / 1000) % 60
                            val lapCen = (lapTime / 10) % 100
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Lap #$index",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = String.format("%02d:%02d.%02d", lapMin, lapSec, lapCen),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            if (index > 1) {
                                Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }
        }
    }
}
