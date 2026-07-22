package com.example.ui

import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BirthdayEntity
import com.example.data.HabitEntity
import com.example.data.TaskEntity
import com.example.data.WishItemEntity
import com.example.ui.theme.*
import com.example.utils.TrackWiseUtils
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource

@Composable
fun WorkspaceScreen(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    val activeSubTab by viewModel.workspaceSubTab.collectAsState()
    val subTabs = listOf("Tasks", "Habit Runways", "Wishlist", "Countdown", "Timer & Stopwatch", "Grocery List")
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




        // --- Sub-Tab Content Rendering ---
        when (activeSubTab) {
            0 -> { // Tasks Sub-Tab
                item { TaskSection(viewModel = viewModel) }
            }
            1 -> { // Habit Runways Sub-Tab
                item { HabitSection(viewModel = viewModel) }
            }
            2 -> { // Wishlist Sub-Tab
                item { WishlistSection(viewModel = viewModel) }
            }
            3 -> { // Birthdays Sub-Tab
                item { BirthdaySection(viewModel = viewModel) }
            }
            4 -> { // Alarms & Clocks Sub-Tab
                item { AlarmTimerSection(viewModel = viewModel) }
            }
            5 -> { // Grocery List Sub-Tab
                item { GrocerySection(viewModel = viewModel) }
            }
        }
    }
}

// ==================== 1. TASKS SECTION ====================
@Composable
fun TaskSection(viewModel: TrackWiseViewModel) {
    val focusManager = LocalFocusManager.current
    val tasks by viewModel.allTasks.collectAsState()
    
    var showForm by remember { mutableStateOf(false) }
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var project by remember { mutableStateOf("Work") }
    var priority by remember { mutableStateOf("medium") }
    var deadline by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }
    var reminderTime by remember { mutableStateOf("08:00") }
    var remindMe by remember { mutableStateOf(false) }
    var reminderDate by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }
    var dueTime by remember { mutableStateOf("") }
 
    var showErrors by remember { mutableStateOf(false) }
    val titleError = if (title.isBlank()) "Task Title is required" else null
    val deadlineError = if (deadline.isNotBlank() && !deadline.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) "Use YYYY-MM-DD format" else null
    val reminderError = if (reminderTime.isNotBlank() && !reminderTime.matches(Regex("\\d{2}:\\d{2}"))) "Use HH:MM format (24h)" else null

    val projects = listOf("Personal", "Work")
    val priorities = listOf("low", "medium", "high")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showForm) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("CREATE NEW TASK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandViolet)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { 
                            title = it 
                            if (it.isNotBlank()) showErrors = false
                        },
                        label = { Text("Task Title *") },
                        singleLine = true,
                        isError = showErrors && titleError != null,
                        supportingText = {
                            if (showErrors && titleError != null) {
                                Text(titleError, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (e.g. key details, urls, logs)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Project Workspace (Simple Row selection, centered text)
                    Column {
                        Text("Project Workspace", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            projects.forEach { proj ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (project == proj) BrandViolet else MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { project = proj }
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = proj,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            color = if (project == proj) Color.White else MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                        // Priority selection
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Priority", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                priorities.forEach { prio ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (priority == prio) BrandViolet else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { priority = prio }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(prio.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (priority == prio) Color.White else MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DatePickerField(
                            dateStr = deadline,
                            label = "Deadline Date",
                            onDateSelected = {
                                deadline = it
                                showErrors = false
                            },
                            tintColor = BrandViolet,
                            modifier = Modifier.weight(1f)
                        )
                        TimePickerField(
                            timeStr = dueTime,
                            label = "Due Time (Optional)",
                            onTimeSelected = { dueTime = it },
                            modifier = Modifier.weight(1f),
                            tintColor = BrandViolet
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = remindMe,
                            onCheckedChange = { remindMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = BrandViolet)
                        )
                        Text("Remind Me", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }

                    if (remindMe) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DatePickerField(
                                dateStr = reminderDate,
                                label = "Reminder Date",
                                onDateSelected = { reminderDate = it },
                                modifier = Modifier.weight(1f),
                                tintColor = BrandViolet
                            )
                            TimePickerField(
                                timeStr = reminderTime,
                                label = "Reminder Time",
                                onTimeSelected = { reminderTime = it },
                                modifier = Modifier.weight(1f),
                                tintColor = BrandViolet
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (titleError == null && deadlineError == null) {
                                viewModel.addTask(
                                    title = title,
                                    description = description,
                                    project = project,
                                    priority = priority,
                                    points = 0,
                                    deadline = deadline,
                                    reminderTime = if (remindMe) reminderTime else null,
                                    notes = notes,
                                    dueTime = if (dueTime.isBlank()) null else dueTime
                                )
                                // Add notification/log reminder setting if checked
                                if (remindMe) {
                                    viewModel.addNotification(
                                        title = "Reminder Configured",
                                        message = "You will be reminded for \"$title\" on $reminderDate at $reminderTime."
                                    )
                                }
                                // Reset
                                title = ""
                                description = ""
                                notes = ""
                                dueTime = ""
                                remindMe = false
                                reminderDate = TrackWiseUtils.getTodayString()
                                reminderTime = "08:00"
                                showForm = false
                                showErrors = false
                            } else {
                                showErrors = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Task", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Tasks Grid/List
        if (tasks.isEmpty()) {
            Text("No tasks added yet. Fill out the form above to get started!")
        } else {
            tasks.forEach { task ->
                TaskCard(task = task, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TaskCard(task: TaskEntity, viewModel: TrackWiseViewModel) {
    val focusManager = LocalFocusManager.current
    var showSubtaskInput by remember { mutableStateOf(false) }
    var newSubtaskTitle by remember { mutableStateOf("") }
    var newSubtaskDueDate by remember { mutableStateOf("") }
    var newSubtaskDueTime by remember { mutableStateOf("") }
    val subtasks = TrackWiseUtils.deserializeSubTasks(task.subtasksJson)

    var showEditDialog by remember { mutableStateOf(false) }
    if (showEditDialog) {
        var editTitle by remember { mutableStateOf(task.title) }
        var editDesc by remember { mutableStateOf(task.description) }
        var editNotes by remember { mutableStateOf(task.notes) }
        var editProject by remember { mutableStateOf(task.project) }
        var editPriority by remember { mutableStateOf(task.priority) }
        var editDeadline by remember { mutableStateOf(task.deadline) }
        var editReminder by remember { mutableStateOf(task.reminderTime ?: "08:00") }
        var editRemindMe by remember { mutableStateOf(task.remindMe) }
        var editReminderDate by remember { mutableStateOf(task.reminderDate ?: task.deadline) }
        var editDueTime by remember { mutableStateOf(task.dueTime ?: "") }

        val scrollState = rememberScrollState()
        LaunchedEffect(scrollState.isScrollInProgress) {
            if (scrollState.isScrollInProgress) {
                focusManager.clearFocus()
            }
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Task Runway 📝", fontWeight = FontWeight.Bold, color = BrandViolet) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Task Title *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editProject,
                        onValueChange = { editProject = it },
                        label = { Text("Project / Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Column {
                        Text("Priority Level", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("low", "medium", "high").forEach { p ->
                                val selected = editPriority == p
                                Button(
                                    onClick = { editPriority = p },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) BrandViolet else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = p.replaceFirstChar { it.uppercase() },
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DatePickerField(
                            dateStr = editDeadline,
                            label = "Deadline Date",
                            onDateSelected = { editDeadline = it },
                            tintColor = BrandViolet,
                            modifier = Modifier.weight(1f)
                        )
                        TimePickerField(
                            timeStr = editDueTime,
                            label = "Due Time (Optional)",
                            onTimeSelected = { editDueTime = it },
                            modifier = Modifier.weight(1f),
                            tintColor = BrandViolet
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = editRemindMe,
                            onCheckedChange = { editRemindMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = BrandViolet)
                        )
                        Text("Remind Me", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }

                    if (editRemindMe) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DatePickerField(
                                dateStr = editReminderDate,
                                label = "Reminder Date",
                                onDateSelected = { editReminderDate = it },
                                modifier = Modifier.weight(1f),
                                tintColor = BrandViolet
                            )
                            TimePickerField(
                                timeStr = editReminder,
                                label = "Reminder Time",
                                onTimeSelected = { editReminder = it },
                                modifier = Modifier.weight(1f),
                                tintColor = BrandViolet
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            val updatedTask = task.copy(
                                title = editTitle,
                                description = editDesc,
                                notes = editNotes,
                                project = editProject,
                                priority = editPriority,
                                deadline = editDeadline,
                                dueTime = if (editDueTime.isBlank()) null else editDueTime,
                                reminderTime = if (editRemindMe) editReminder else null,
                                remindMe = editRemindMe,
                                reminderDate = if (editRemindMe) editReminderDate else null
                            )
                            viewModel.updateTask(updatedTask)
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Save Changes", color = BrandViolet, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .clickable { showEditDialog = true }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Task Checkbox
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
                        )
                        .clickable { viewModel.toggleTaskCompletion(task) },
                    contentAlignment = Alignment.Center
                ) {
                    if (task.completed) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.completed) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground
                    )
                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    if (task.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📝 ${task.notes}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = BrandViolet
                        )
                    }
                }

                // Delete Button
                IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BrandRose)
                }
            }

            // Tag Pills row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandViolet.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(task.project, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
                }

                val priorityColor = when (task.priority) {
                    "high" -> BrandRose
                    "medium" -> BrandViolet
                    else -> BrandCyan
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(priorityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(task.priority.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = priorityColor)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("📅 ${task.deadline}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                if (task.reminderTime != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandOrange.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("⏰ ${task.reminderTime}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandOrange)
                    }
                }

                if (task.repeatType != "none") {
                    val repeatLabel = when (task.repeatType) {
                        "daily" -> "Daily"
                        "weekdays" -> "Weekdays"
                        "weekly" -> "Weekly"
                        "monthly" -> "Monthly"
                        "yearly" -> "Yearly"
                        "custom" -> {
                            val unitStr = if (task.customRepeatValue == 1) {
                                task.customRepeatUnit.removeSuffix("s")
                            } else {
                                task.customRepeatUnit
                            }
                            var base = "Every ${task.customRepeatValue} $unitStr"
                            if (task.customRepeatUnit == "weeks" && !task.customRepeatDaysOfWeek.isNullOrBlank()) {
                                base += " on ${task.customRepeatDaysOfWeek}"
                            }
                            base
                        }
                        else -> task.repeatType
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("🔁 $repeatLabel", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                    }
                }
            }

            // Subtasks section
            if (subtasks.isNotEmpty() || showSubtaskInput) {
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Text("Subtasks", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandViolet)

                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subtasks.forEach { sub ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .border(
                                        1.5.dp,
                                        if (sub.completed) BrandGreen else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                        CircleShape
                                    )
                                    .background(
                                        if (sub.completed) BrandGreen.copy(alpha = 0.2f) else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { viewModel.toggleSubTask(task, sub.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (sub.completed) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(12.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sub.title,
                                    fontSize = 12.sp,
                                    textDecoration = if (sub.completed) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (sub.completed) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground
                                )
                                if (!sub.dueDate.isNullOrBlank() || !sub.dueTime.isNullOrBlank()) {
                                    val datePart = if (!sub.dueDate.isNullOrBlank()) "📅 ${sub.dueDate}" else ""
                                    val timePart = if (!sub.dueTime.isNullOrBlank()) "⏰ ${sub.dueTime}" else ""
                                    val spacer = if (datePart.isNotEmpty() && timePart.isNotEmpty()) " " else ""
                                    Text(
                                        text = "$datePart$spacer$timePart",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandViolet.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.deleteSubTask(task, sub.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = BrandRose, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    if (showSubtaskInput) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newSubtaskTitle,
                                    onValueChange = { newSubtaskTitle = it },
                                    placeholder = { Text("Enter subtask title") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    if (newSubtaskTitle.isNotBlank()) {
                                        viewModel.addSubTask(
                                            task = task,
                                            subTitle = newSubtaskTitle,
                                            dueDate = if (newSubtaskDueDate.isBlank()) null else newSubtaskDueDate,
                                            dueTime = if (newSubtaskDueTime.isBlank()) null else newSubtaskDueTime
                                        )
                                        newSubtaskTitle = ""
                                        newSubtaskDueDate = ""
                                        newSubtaskDueTime = ""
                                        showSubtaskInput = false
                                    }
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = BrandGreen)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DatePickerField(
                                    dateStr = newSubtaskDueDate,
                                    label = "Due Date (Opt)",
                                    onDateSelected = { newSubtaskDueDate = it },
                                    modifier = Modifier.weight(1f),
                                    tintColor = BrandViolet
                                )
                                TimePickerField(
                                    timeStr = newSubtaskDueTime,
                                    label = "Due Time (Opt)",
                                    onTimeSelected = { newSubtaskDueTime = it },
                                    modifier = Modifier.weight(1f),
                                    tintColor = BrandViolet
                                )
                            }
                        }
                    }
                }
            }

            // Inline subtask adder trigger
            if (!showSubtaskInput) {
                TextButton(
                    onClick = { showSubtaskInput = true },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandViolet)
                    Text("Add Subtask", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandViolet, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

// ==================== 2. HABITS SECTION ====================
@Composable
fun HabitSection(viewModel: TrackWiseViewModel) {
    val focusManager = LocalFocusManager.current
    val habits by viewModel.allHabits.collectAsState()
    var showForm by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Wellness") }

    var isMultipleTimes by remember { mutableStateOf(false) }
    var multipleTimesTargetInput by remember { mutableStateOf("1") }
    var isTimeBound by remember { mutableStateOf(false) }
    var timeBoundDurationInput by remember { mutableStateOf("") }

    var repeatType by remember { mutableStateOf("daily") }
    var customRepeatValue by remember { mutableStateOf("1") }
    var customRepeatUnit by remember { mutableStateOf("days") }
    var customRepeatDaysOfWeek by remember { mutableStateOf(emptySet<String>()) }
    var habitStartDate by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }
    var habitEndDate by remember { mutableStateOf("") }
    var habitUntilIStop by remember { mutableStateOf(true) }
    var remindMe by remember { mutableStateOf(false) }
    var reminderDate by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }
    var reminderTime by remember { mutableStateOf("08:00") }
    var dueTime by remember { mutableStateOf("") }
 
    var showErrors by remember { mutableStateOf(false) }
    val nameError = if (name.isBlank()) "Habit Name is required" else null
    val targetError = if (isMultipleTimes && (multipleTimesTargetInput.toIntOrNull() ?: 0) <= 0) "Target count must be at least 1" else null
    val durationError = if (isTimeBound && timeBoundDurationInput.isBlank()) "Duration is required for time-bound habits" else null
    val habitStartDateError = if (repeatType != "none" && habitStartDate.isNotBlank() && !habitStartDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) "Use YYYY-MM-DD format" else null
    val habitEndDateError = if (repeatType != "none" && !habitUntilIStop && habitEndDate.isNotBlank() && !habitEndDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) "Use YYYY-MM-DD format" else null

    val categories = listOf("Wellness", "Fitness", "Learning", "Productivity")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Toggle add habit card
        Button(
            onClick = { 
                showForm = !showForm
                if (showForm) showErrors = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(if (showForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null, tint = Color.White)
            Text(if (showForm) "Close Form" else "Add New Habit", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }

        if (showForm) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("LAUNCH HABIT RUNWAY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandOrange)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            if (it.isNotBlank()) showErrors = false
                        },
                        label = { Text("Habit Name *") },
                        singleLine = true,
                        isError = showErrors && nameError != null,
                        supportingText = {
                            if (showErrors && nameError != null) {
                                Text(nameError, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    TimePickerField(
                        timeStr = dueTime,
                        label = "Due Time",
                        onTimeSelected = { dueTime = it },
                        modifier = Modifier.fillMaxWidth(),
                        tintColor = BrandOrange
                    )

                    Column {
                        Text("Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.forEach { cat ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (category == cat) BrandOrange else MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { category = cat }
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp), color = if (category == cat) Color.White else MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                            }
                        }
                    }

                    Text("Habit Type Constraint ⚙️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandOrange)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMultipleTimes) BrandOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { 
                                    isMultipleTimes = !isMultipleTimes
                                    if (isMultipleTimes) isTimeBound = false
                                },
                            border = BorderStroke(1.dp, if (isMultipleTimes) BrandOrange else Color.Transparent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Multiple Times", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isMultipleTimes) BrandOrange else MaterialTheme.colorScheme.onSurface)
                                Text("Per Day", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isTimeBound) BrandOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { 
                                    isTimeBound = !isTimeBound
                                    if (isTimeBound) isMultipleTimes = false
                                },
                            border = BorderStroke(1.dp, if (isTimeBound) BrandOrange else Color.Transparent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Time Bound", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isTimeBound) BrandOrange else MaterialTheme.colorScheme.onSurface)
                                Text("e.g. 30 mins", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (isMultipleTimes) {
                        OutlinedTextField(
                            value = multipleTimesTargetInput,
                            onValueChange = { 
                                multipleTimesTargetInput = it 
                                showErrors = false
                            },
                            label = { Text("Target Count Per Day") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = showErrors && targetError != null,
                            supportingText = {
                                if (showErrors && targetError != null) {
                                    Text(targetError, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (isTimeBound) {
                        OutlinedTextField(
                            value = timeBoundDurationInput,
                            onValueChange = { 
                                timeBoundDurationInput = it 
                                showErrors = false
                            },
                            label = { Text("Duration / Time Constraint") },
                            placeholder = { Text("e.g. 30 mins, or 18:00") },
                            singleLine = true,
                            isError = showErrors && durationError != null,
                            supportingText = {
                                if (showErrors && durationError != null) {
                                    Text(durationError, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    RecurrenceSelector(
                        repeatType = repeatType,
                        onRepeatTypeChange = { repeatType = it },
                        customRepeatValue = customRepeatValue,
                        onCustomRepeatValueChange = { customRepeatValue = it },
                        customRepeatUnit = customRepeatUnit,
                        onCustomRepeatUnitChange = { customRepeatUnit = it },
                        customRepeatDaysOfWeek = customRepeatDaysOfWeek,
                        onCustomRepeatDaysOfWeekChange = { customRepeatDaysOfWeek = it },
                        startDate = habitStartDate,
                        onStartDateChange = { habitStartDate = it; showErrors = false },
                        endDate = habitEndDate,
                        onEndDateChange = { habitEndDate = it; showErrors = false },
                        untilIStop = habitUntilIStop,
                        onUntilIStopChange = { habitUntilIStop = it; showErrors = false },
                        themeColor = BrandOrange,
                        startDateError = if (showErrors) habitStartDateError else null,
                        endDateError = if (showErrors) habitEndDateError else null
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = remindMe,
                            onCheckedChange = { remindMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = BrandOrange)
                        )
                        Text("Remind Me", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }

                    if (remindMe) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DatePickerField(
                                dateStr = reminderDate,
                                label = "Reminder Date",
                                onDateSelected = { reminderDate = it },
                                modifier = Modifier.weight(1f),
                                tintColor = BrandOrange
                            )
                            TimePickerField(
                                timeStr = reminderTime,
                                label = "Reminder Time",
                                onTimeSelected = { reminderTime = it },
                                modifier = Modifier.weight(1f),
                                tintColor = BrandOrange
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (nameError == null && targetError == null && durationError == null && habitStartDateError == null && habitEndDateError == null) {
                                viewModel.addHabit(
                                    name = name,
                                    category = category,
                                    isMultipleTimesPerDay = isMultipleTimes,
                                    multipleTimesTarget = if (isMultipleTimes) (multipleTimesTargetInput.toIntOrNull() ?: 1) else 1,
                                    isTimeBound = isTimeBound,
                                    timeBoundDuration = if (isTimeBound) timeBoundDurationInput else null,
                                    repeatType = repeatType,
                                    customRepeatValue = customRepeatValue.toIntOrNull() ?: 1,
                                    customRepeatUnit = customRepeatUnit,
                                    customRepeatDaysOfWeek = if (repeatType == "custom" && customRepeatUnit == "weeks") customRepeatDaysOfWeek.joinToString(",") else null,
                                    startDate = if (repeatType == "none") null else habitStartDate,
                                    endDate = if (repeatType == "none" || habitUntilIStop) null else habitEndDate,
                                    remindMe = remindMe,
                                    reminderDate = if (remindMe) reminderDate else null,
                                    reminderTime = if (remindMe) reminderTime else null,
                                    dueTime = if (dueTime.isBlank()) null else dueTime
                                )
                                // Reset
                                name = ""
                                dueTime = ""
                                isMultipleTimes = false
                                multipleTimesTargetInput = "1"
                                isTimeBound = false
                                timeBoundDurationInput = ""
                                repeatType = "none"
                                customRepeatValue = "1"
                                customRepeatUnit = "days"
                                customRepeatDaysOfWeek = emptySet()
                                habitStartDate = TrackWiseUtils.getTodayString()
                                habitEndDate = ""
                                habitUntilIStop = true
                                remindMe = false
                                reminderDate = TrackWiseUtils.getTodayString()
                                reminderTime = "08:00"
                                showForm = false
                                showErrors = false
                            } else {
                                showErrors = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Runway", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Display all habits in Workspace so they are always visible, manageable, editable, and deletable
        if (habits.isEmpty()) {
            Text("No habit runways configured yet. Create one above!")
        } else {
            habits.forEach { habit ->
                HabitCard(habit = habit, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HabitCard(habit: HabitEntity, viewModel: TrackWiseViewModel) {
    val focusManager = LocalFocusManager.current
    val completedDays = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson)
    val today = TrackWiseUtils.getTodayString()
    val completedCountToday = completedDays.count { it == today }
    val isCompletedToday = if (habit.isMultipleTimesPerDay) {
        completedCountToday >= habit.multipleTimesTarget
    } else {
        completedCountToday >= 1
    }

    var showEditDialog by remember { mutableStateOf(false) }
    if (showEditDialog) {
        var editName by remember { mutableStateOf(habit.name) }
        var editCategory by remember { mutableStateOf(habit.category) }
        var editNotes by remember { mutableStateOf(habit.notes) }
        var editMultipleTimes by remember { mutableStateOf(habit.isMultipleTimesPerDay) }
        var editMultipleTimesTarget by remember { mutableStateOf(habit.multipleTimesTarget.toString()) }
        var editTimeBound by remember { mutableStateOf(habit.isTimeBound) }
        var editTimeBoundDuration by remember { mutableStateOf(habit.timeBoundDuration ?: "") }
        var editRemindMe by remember { mutableStateOf(habit.remindMe) }
        var editReminderDate by remember { mutableStateOf(habit.reminderDate ?: TrackWiseUtils.getTodayString()) }
        var editReminderTime by remember { mutableStateOf(habit.reminderTime ?: "08:00") }
        var editDueTime by remember { mutableStateOf(habit.dueTime ?: "") }

        var editRepeatType by remember { mutableStateOf(habit.repeatType) }
        var editCustomRepeatValue by remember { mutableStateOf(habit.customRepeatValue.toString()) }
        var editCustomRepeatUnit by remember { mutableStateOf(habit.customRepeatUnit) }
        var editCustomRepeatDaysOfWeek by remember {
            mutableStateOf(
                if (habit.customRepeatDaysOfWeek.isNullOrBlank()) emptySet<String>()
                else habit.customRepeatDaysOfWeek.split(",").toSet()
            )
        }
        var editHabitStartDate by remember { mutableStateOf(habit.startDate ?: TrackWiseUtils.getTodayString()) }
        var editHabitEndDate by remember { mutableStateOf(habit.endDate ?: "") }
        var editHabitUntilIStop by remember { mutableStateOf(habit.endDate.isNullOrBlank()) }

        val scrollState = rememberScrollState()
        LaunchedEffect(scrollState.isScrollInProgress) {
            if (scrollState.isScrollInProgress) {
                focusManager.clearFocus()
            }
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Habit Runway ⚙️", fontWeight = FontWeight.Bold, color = BrandOrange) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                        }
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Habit Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    TimePickerField(
                        timeStr = editDueTime,
                        label = "Due Time",
                        onTimeSelected = { editDueTime = it },
                        modifier = Modifier.fillMaxWidth(),
                        tintColor = BrandOrange
                    )

                    Column {
                        Text("Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Wellness", "Fitness", "Learning", "Productivity").forEach { cat ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (editCategory == cat) BrandOrange else MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { editCategory = cat }
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp), color = if (editCategory == cat) Color.White else MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (editMultipleTimes) BrandOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { 
                                    editMultipleTimes = !editMultipleTimes
                                    if (editMultipleTimes) editTimeBound = false
                                },
                            border = BorderStroke(1.dp, if (editMultipleTimes) BrandOrange else Color.Transparent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Multiple Times", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (editMultipleTimes) BrandOrange else MaterialTheme.colorScheme.onSurface)
                                Text("Per Day", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (editTimeBound) BrandOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { 
                                    editTimeBound = !editTimeBound
                                    if (editTimeBound) editMultipleTimes = false
                                },
                            border = BorderStroke(1.dp, if (editTimeBound) BrandOrange else Color.Transparent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Time Bound", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (editTimeBound) BrandOrange else MaterialTheme.colorScheme.onSurface)
                                Text("e.g. 30 mins", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (editMultipleTimes) {
                        OutlinedTextField(
                            value = editMultipleTimesTarget,
                            onValueChange = { editMultipleTimesTarget = it },
                            label = { Text("Daily Target Times") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (editTimeBound) {
                        OutlinedTextField(
                            value = editTimeBoundDuration,
                            onValueChange = { editTimeBoundDuration = it },
                            label = { Text("Duration (e.g. 30 mins)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    RecurrenceSelector(
                        repeatType = editRepeatType,
                        onRepeatTypeChange = { editRepeatType = it },
                        customRepeatValue = editCustomRepeatValue,
                        onCustomRepeatValueChange = { editCustomRepeatValue = it },
                        customRepeatUnit = editCustomRepeatUnit,
                        onCustomRepeatUnitChange = { editCustomRepeatUnit = it },
                        customRepeatDaysOfWeek = editCustomRepeatDaysOfWeek,
                        onCustomRepeatDaysOfWeekChange = { editCustomRepeatDaysOfWeek = it },
                        startDate = editHabitStartDate,
                        onStartDateChange = { editHabitStartDate = it },
                        endDate = editHabitEndDate,
                        onEndDateChange = { editHabitEndDate = it },
                        untilIStop = editHabitUntilIStop,
                        onUntilIStopChange = { editHabitUntilIStop = it },
                        themeColor = BrandOrange,
                        startDateError = null,
                        endDateError = null
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = editRemindMe,
                            onCheckedChange = { editRemindMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = BrandOrange)
                        )
                        Text("Remind Me", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }

                    if (editRemindMe) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DatePickerField(
                                dateStr = editReminderDate,
                                label = "Reminder Date",
                                onDateSelected = { editReminderDate = it },
                                modifier = Modifier.weight(1f),
                                tintColor = BrandOrange
                            )
                            TimePickerField(
                                timeStr = editReminderTime,
                                label = "Reminder Time",
                                onTimeSelected = { editReminderTime = it },
                                modifier = Modifier.weight(1f),
                                tintColor = BrandOrange
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editName.isNotBlank()) {
                            val updatedHabit = habit.copy(
                                name = editName,
                                category = editCategory,
                                notes = editNotes,
                                isMultipleTimesPerDay = editMultipleTimes,
                                multipleTimesTarget = editMultipleTimesTarget.toIntOrNull() ?: 1,
                                isTimeBound = editTimeBound,
                                timeBoundDuration = if (editTimeBound) editTimeBoundDuration.ifBlank { null } else null,
                                repeatType = editRepeatType,
                                customRepeatValue = editCustomRepeatValue.toIntOrNull() ?: 1,
                                customRepeatUnit = editCustomRepeatUnit,
                                customRepeatDaysOfWeek = if (editCustomRepeatDaysOfWeek.isEmpty()) null else editCustomRepeatDaysOfWeek.joinToString(","),
                                startDate = editHabitStartDate,
                                endDate = if (editHabitUntilIStop) null else editHabitEndDate.ifBlank { null },
                                remindMe = editRemindMe,
                                reminderDate = if (editRemindMe) editReminderDate else null,
                                reminderTime = if (editRemindMe) editReminderTime else null,
                                dueTime = if (editDueTime.isBlank()) null else editDueTime
                            )
                            viewModel.updateHabit(updatedHabit)
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Save Changes", color = BrandOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .clickable { showEditDialog = true }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (habit.isMultipleTimesPerDay) {
                    IconButton(
                        onClick = { viewModel.decrementHabitToday(habit) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = BrandOrange, modifier = Modifier.size(18.dp))
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(
                                2.dp,
                                if (isCompletedToday) BrandOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                CircleShape
                            )
                            .background(
                                if (isCompletedToday) BrandOrange.copy(alpha = 0.2f) else Color.Transparent,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompletedToday) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(16.dp))
                        } else {
                            Text(text = "$completedCountToday", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandOrange)
                        }
                    }

                    IconButton(
                        onClick = { viewModel.incrementHabitToday(habit) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = BrandOrange, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(
                                2.dp,
                                if (isCompletedToday) BrandOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                CircleShape
                            )
                            .background(
                                if (isCompletedToday) BrandOrange.copy(alpha = 0.2f) else Color.Transparent,
                                CircleShape
                            )
                            .clickable { viewModel.toggleHabitToday(habit) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompletedToday) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = habit.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isCompletedToday) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isCompletedToday) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = habit.category,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        if (habit.isTimeBound && !habit.timeBoundDuration.isNullOrBlank()) {
                            Text(
                                text = "• ⏰ ${habit.timeBoundDuration}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandOrange
                            )
                        }
                        if (habit.isMultipleTimesPerDay) {
                            Text(
                                text = "• 🎯 Target: ${habit.multipleTimesTarget}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandOrange
                            )
                        }
                        if (habit.repeatType != "none") {
                            val repeatLabel = when (habit.repeatType) {
                                "daily" -> "Daily"
                                "weekdays" -> "Weekdays"
                                "weekly" -> "Weekly"
                                "monthly" -> "Monthly"
                                "yearly" -> "Yearly"
                                "custom" -> {
                                    val unitStr = if (habit.customRepeatValue == 1) {
                                        habit.customRepeatUnit.removeSuffix("s")
                                    } else {
                                        habit.customRepeatUnit
                                    }
                                    var base = "Every ${habit.customRepeatValue} $unitStr"
                                    if (habit.customRepeatUnit == "weeks" && !habit.customRepeatDaysOfWeek.isNullOrBlank()) {
                                        base += " on ${habit.customRepeatDaysOfWeek}"
                                    }
                                    base
                                }
                                else -> habit.repeatType
                            }
                            Text(
                                text = "• 🔁 $repeatLabel",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandGreen
                            )
                        }
                    }
                }

                // Streaks & Badges indicators
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${habit.streak}d",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { viewModel.deleteHabit(habit.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BrandRose)
                }
            }

            // --- 7-Day Completion Grid (Section 9.3) ---
            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Text("7-Day Runway Track", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandOrange)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Generate last 7 days representing circles
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val dayLabelSdf = SimpleDateFormat("EEE", Locale.US)
                val cal = Calendar.getInstance()
                
                // Fetch the list of days backwards
                val last7Days = (0..6).map { i ->
                    val c = Calendar.getInstance()
                    c.add(Calendar.DAY_OF_YEAR, -i)
                    c.time
                }.reversed()

                last7Days.forEach { date ->
                    val dateStr = sdf.format(date)
                    val label = dayLabelSdf.format(date).take(1) // Single character representation: M, T, W
                    val hasCompleted = completedDays.contains(dateStr)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasCompleted) BrandOrange else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (hasCompleted) Color.Transparent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                                    CircleShape
                                )
                                .clickable {
                                    // A direct specific date toggle can be stimulated safely using standard API
                                    // Normally toggleHabitToday operates on current date, but we can do custom trigger
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasCompleted) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 3. WISHLIST SECTION ====================
@Composable
fun WishlistSection(viewModel: TrackWiseViewModel) {
    val focusManager = LocalFocusManager.current
    val items by viewModel.allWishlist.collectAsState()
    var showForm by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("medium") }
    var remindMe by remember { mutableStateOf(false) }
    var reminderDate by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }
    var reminderTime by remember { mutableStateOf("08:00") }

    var showErrors by remember { mutableStateOf(false) }
    val titleError = if (title.isBlank()) "Item Title is required" else null
    val priceError = if (price.isNotBlank() && (price.toDoubleOrNull() ?: -1.0) < 0.0) "Enter a valid positive price" else null
    val linkError = if (link.isNotBlank() && !link.startsWith("http://") && !link.startsWith("https://")) "Must start with http:// or https://" else null

    var editingWishItem by remember { mutableStateOf<com.example.data.WishItemEntity?>(null) }

    if (editingWishItem != null) {
        val wish = editingWishItem!!
        var editTitle by remember(wish) { mutableStateOf(wish.title) }
        var editPrice by remember(wish) { mutableStateOf(wish.price.toString()) }
        var editLink by remember(wish) { mutableStateOf(wish.link ?: "") }
        var editPriority by remember(wish) { mutableStateOf(wish.priority) }
        var editRemindMe by remember(wish) { mutableStateOf(wish.remindMe) }
        var editReminderDate by remember(wish) { mutableStateOf(wish.reminderDate ?: TrackWiseUtils.getTodayString()) }
        var editReminderTime by remember(wish) { mutableStateOf(wish.reminderTime ?: "08:00") }

        val scrollState = rememberScrollState()
        LaunchedEffect(scrollState.isScrollInProgress) {
            if (scrollState.isScrollInProgress) {
                focusManager.clearFocus()
            }
        }

        AlertDialog(
            onDismissRequest = { editingWishItem = null },
            title = { Text("Edit Wishlist Item 🎁", fontWeight = FontWeight.Bold, color = BrandPink) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                        }
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPrice,
                        onValueChange = { editPrice = it },
                        label = { Text("Price (INR)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editLink,
                        onValueChange = { editLink = it },
                        label = { Text("Link") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Priority Level", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("low", "medium", "high").forEach { p ->
                                val selected = editPriority == p
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) BrandPink else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { editPriority = p }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = p.replaceFirstChar { it.uppercase() },
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = editRemindMe,
                            onCheckedChange = { editRemindMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = BrandPink)
                        )
                        Text("Remind Me", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }

                    if (editRemindMe) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DatePickerField(
                                dateStr = editReminderDate,
                                label = "Reminder Date",
                                onDateSelected = { editReminderDate = it },
                                modifier = Modifier.weight(1f),
                                tintColor = BrandPink
                            )
                            TimePickerField(
                                timeStr = editReminderTime,
                                label = "Reminder Time",
                                onTimeSelected = { editReminderTime = it },
                                modifier = Modifier.weight(1f),
                                tintColor = BrandPink
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            val updatedItem = wish.copy(
                                title = editTitle,
                                price = editPrice.toDoubleOrNull() ?: 0.0,
                                link = editLink.ifBlank { null },
                                priority = editPriority,
                                remindMe = editRemindMe,
                                reminderDate = if (editRemindMe) editReminderDate else null,
                                reminderTime = if (editRemindMe) editReminderTime else null
                            )
                            viewModel.updateWishItem(updatedItem)
                            editingWishItem = null
                        }
                    }
                ) {
                    Text("Save Changes", color = BrandPink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingWishItem = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Toggle add wishlist item form
        Button(
            onClick = { 
                showForm = !showForm
                if (showForm) showErrors = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = BrandPink),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(if (showForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null, tint = Color.White)
            Text(if (showForm) "Close Form" else "Add New Item", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }

        if (showForm) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ADD WISHLIST ASPIRATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPink)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { 
                            title = it 
                            if (it.isNotBlank()) showErrors = false
                        },
                        label = { Text("Item Title *") },
                        singleLine = true,
                        isError = showErrors && titleError != null,
                        supportingText = {
                            if (showErrors && titleError != null) {
                                Text(titleError, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CompactTextField(
                            value = price,
                            onValueChange = { 
                                price = it 
                                showErrors = false
                            },
                            label = "Price (₹)",
                            placeholder = "e.g. 1500",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = showErrors && priceError != null,
                            errorText = priceError,
                            modifier = Modifier.weight(1f)
                        )

                        CompactTextField(
                            value = link,
                            onValueChange = { 
                                link = it 
                                showErrors = false
                            },
                            label = "Product URL Link",
                            placeholder = "https://...",
                            isError = showErrors && linkError != null,
                            errorText = linkError,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Priority Selection
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("low", "medium", "high").forEach { prio ->
                            val selected = priority == prio
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) BrandPink else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { priority = prio }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = prio.replaceFirstChar { it.uppercase() },
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = remindMe,
                            onCheckedChange = { remindMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = BrandPink)
                        )
                        Text("Remind Me", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }

                    if (remindMe) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DatePickerField(
                                dateStr = reminderDate,
                                label = "Reminder Date",
                                onDateSelected = { reminderDate = it },
                                modifier = Modifier.weight(1f),
                                tintColor = BrandPink
                            )
                            TimePickerField(
                                timeStr = reminderTime,
                                label = "Reminder Time",
                                onTimeSelected = { reminderTime = it },
                                modifier = Modifier.weight(1f),
                                tintColor = BrandPink
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (titleError == null && priceError == null && linkError == null) {
                                viewModel.addWishItem(
                                    title = title,
                                    price = price.toDoubleOrNull() ?: 0.0,
                                    link = if (link.isBlank()) null else link,
                                    priority = priority,
                                    remindMe = remindMe,
                                    reminderDate = if (remindMe) reminderDate else null,
                                    reminderTime = if (remindMe) reminderTime else null
                                )
                                title = ""
                                price = ""
                                link = ""
                                remindMe = false
                                reminderDate = TrackWiseUtils.getTodayString()
                                reminderTime = "08:00"
                                showForm = false
                                showErrors = false
                            } else {
                                showErrors = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPink),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add to Wishlist", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (items.isEmpty()) {
            Text("No wishlist items added yet. Record your dream items!")
        } else {
            items.forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .clickable { editingWishItem = item }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(
                                    2.dp,
                                    if (item.purchased) BrandGreen else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    CircleShape
                                )
                                .background(
                                    if (item.purchased) BrandGreen.copy(alpha = 0.2f) else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { viewModel.toggleWishPurchased(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.purchased) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = item.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (item.purchased) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (item.purchased) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "₹${item.price} · ${item.priority.uppercase()} Priority",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }

                        IconButton(onClick = { viewModel.deleteWishItem(item.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = BrandRose)
                        }
                    }
                }
            }
        }
    }
}

// ==================== 4. BIRTHDAYS SECTION ====================
private fun daysUntilBirthday(storedDate: String): Int {
    val parts = storedDate.split("-")
    val (month, day) = if (parts.size == 3) {
        Pair(parts[1].toIntOrNull() ?: 1, parts[2].toIntOrNull() ?: 1)
    } else if (parts.size == 2) {
        Pair(parts[0].toIntOrNull() ?: 1, parts[1].toIntOrNull() ?: 1)
    } else {
        return 999
    }

    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)

    val bdayThisYear = Calendar.getInstance()
    bdayThisYear.set(Calendar.YEAR, today.get(Calendar.YEAR))
    bdayThisYear.set(Calendar.MONTH, month - 1)
    bdayThisYear.set(Calendar.DAY_OF_MONTH, day)
    bdayThisYear.set(Calendar.HOUR_OF_DAY, 0)
    bdayThisYear.set(Calendar.MINUTE, 0)
    bdayThisYear.set(Calendar.SECOND, 0)
    bdayThisYear.set(Calendar.MILLISECOND, 0)

    if (bdayThisYear.timeInMillis == today.timeInMillis) {
        return 0
    }

    if (bdayThisYear.before(today)) {
        val bdayNextYear = Calendar.getInstance()
        bdayNextYear.set(Calendar.YEAR, today.get(Calendar.YEAR) + 1)
        bdayNextYear.set(Calendar.MONTH, month - 1)
        bdayNextYear.set(Calendar.DAY_OF_MONTH, day)
        bdayNextYear.set(Calendar.HOUR_OF_DAY, 0)
        bdayNextYear.set(Calendar.MINUTE, 0)
        bdayNextYear.set(Calendar.SECOND, 0)
        bdayNextYear.set(Calendar.MILLISECOND, 0)
        
        val diffMs = bdayNextYear.timeInMillis - today.timeInMillis
        return (diffMs / (1000 * 60 * 60 * 24)).toInt()
    } else {
        val diffMs = bdayThisYear.timeInMillis - today.timeInMillis
        return (diffMs / (1000 * 60 * 60 * 24)).toInt()
    }
}

private fun calculateAge(birthDateStr: String): Int? {
    val parts = birthDateStr.split("-")
    if (parts.size != 3) return null
    val birthYear = parts[0].toIntOrNull() ?: return null
    val birthMonth = parts[1].toIntOrNull() ?: return null
    val birthDay = parts[2].toIntOrNull() ?: return null

    val today = Calendar.getInstance()
    val currentYear = today.get(Calendar.YEAR)
    val currentMonth = today.get(Calendar.MONTH) + 1
    val currentDay = today.get(Calendar.DAY_OF_MONTH)

    var age = currentYear - birthYear
    if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
        age--
    }
    return if (age >= 0) age else 0
}

private fun formatBirthdayDate(storedDate: String): String {
    val parts = storedDate.split("-")
    if (parts.size == 3) {
        val year = parts[0]
        val month = parts[1].toIntOrNull() ?: 1
        val day = parts[2].toIntOrNull() ?: 1
        val monthName = getShortMonthName(month)
        return "$day $monthName $year"
    } else if (parts.size == 2) {
        val month = parts[0].toIntOrNull() ?: 1
        val day = parts[1].toIntOrNull() ?: 1
        val monthName = getShortMonthName(month)
        return "$day $monthName"
    }
    return storedDate
}

private fun getShortMonthName(month: Int): String {
    return when (month) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> ""
    }
}

private fun parseInputDate(input: String, hasYear: Boolean): String? {
    val cleaned = input.trim().replace('/', '-').replace('.', '-')
    val parts = cleaned.split("-")
    if (hasYear) {
        if (parts.size == 3) {
            val day = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull() ?: return null
            val year = parts[2].toIntOrNull() ?: return null
            if (day in 1..31 && month in 1..12 && year > 1900 && year < 2100) {
                val formattedDay = day.toString().padStart(2, '0')
                val formattedMonth = month.toString().padStart(2, '0')
                return "$year-$formattedMonth-$formattedDay"
            }
        }
    } else {
        if (parts.size == 2) {
            val day = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull() ?: return null
            if (day in 1..31 && month in 1..12) {
                val formattedDay = day.toString().padStart(2, '0')
                val formattedMonth = month.toString().padStart(2, '0')
                return "$formattedMonth-$formattedDay"
            }
        }
    }
    return null
}

private fun parseFontStyle(fontName: String?): androidx.compose.ui.text.font.FontFamily {
    return when (fontName) {
        "Sans Serif" -> androidx.compose.ui.text.font.FontFamily.SansSerif
        "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
        "Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
        "Cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
        else -> androidx.compose.ui.text.font.FontFamily.Default
    }
}

private fun calculateOccasionDays(bday: com.example.data.BirthdayEntity): Int {
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
            if (eventDate.before(today) || eventDate == today) {
                val diffMs = today.timeInMillis - eventDate.timeInMillis
                return (diffMs / (1000 * 60 * 60 * 24)).toInt()
            }
            return 0
        } else if (bday.countingMode == "Count Down") {
            if (eventDate.after(today)) {
                val diffMs = eventDate.timeInMillis - today.timeInMillis
                return (diffMs / (1000 * 60 * 60 * 24)).toInt()
            }
            return 0
        }
    }
    // Default to the yearly occurrence countdown logic
    return daysUntilBirthday(bday.date)
}

@Composable
private fun DialogTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun getDynamicRepeatLabel(choice: String, dateStr: String?): String {
    val cleanChoice = when {
        choice.startsWith("Weekly") -> "Weekly"
        choice.startsWith("Monthly") -> "Monthly"
        choice.startsWith("Yearly") -> "Yearly"
        else -> choice
    }
    val cal = Calendar.getInstance()
    if (!dateStr.isNullOrBlank()) {
        val parts = dateStr.split("-")
        try {
            if (parts.size == 3) {
                val y = parts[0].toInt()
                val m = parts[1].toInt() - 1
                val d = parts[2].toInt()
                cal.set(y, m, d)
            } else if (parts.size == 2) {
                val m = parts[0].toInt() - 1
                val d = parts[1].toInt()
                cal.set(Calendar.getInstance().get(Calendar.YEAR), m, d)
            }
        } catch (e: Exception) {
            // use current system date
        }
    }

    return when (cleanChoice) {
        "Weekly" -> {
            val dayOfWeek = SimpleDateFormat("EEE", Locale.US).format(cal.time)
            "Weekly($dayOfWeek)"
        }
        "Monthly" -> {
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            "Monthly(The $dayOfMonth day)"
        }
        "Yearly" -> {
            val monthAndDay = SimpleDateFormat("MMM dd", Locale.US).format(cal.time)
            "Yearly (on $monthAndDay)"
        }
        else -> cleanChoice
    }
}

@Composable
fun BirthdaySection(viewModel: TrackWiseViewModel) {
    val focusManager = LocalFocusManager.current
    val birthdays by viewModel.allBirthdays.collectAsState()
    val context = LocalContext.current

    var showAddOccasionDialog by remember { mutableStateOf(false) }
    var addOccasionType by remember { mutableStateOf("Birthday") }
    var editingBirthday by remember { mutableStateOf<com.example.data.BirthdayEntity?>(null) }
    var detailedBirthday by remember { mutableStateOf<com.example.data.BirthdayEntity?>(null) }
    var showGlobalFontDialog by remember { mutableStateOf(false) }
    var globalFontStyle by remember { mutableStateOf("Default") }

    val triggeredCategory by viewModel.triggerAddOccasion.collectAsState()
    LaunchedEffect(triggeredCategory) {
        triggeredCategory?.let { category ->
            addOccasionType = category
            showAddOccasionDialog = true
            viewModel.clearTriggerAddOccasion()
        }
    }

    val allPresets = remember {
        com.example.ui.theme.BackgroundPresets.textures +
        com.example.ui.theme.BackgroundPresets.abstractImages +
        com.example.ui.theme.BackgroundPresets.cityscapes +
        com.example.ui.theme.BackgroundPresets.landscapes
    }

    if (showAddOccasionDialog || editingBirthday != null) {
        val bday = editingBirthday
        val isEdit = bday != null

        var nameText by remember(showAddOccasionDialog, bday) {
            mutableStateOf(bday?.name ?: "")
        }
        var dateText by remember(showAddOccasionDialog, bday) {
            mutableStateOf(bday?.date)
        }
        var isYearSelected by remember(showAddOccasionDialog, bday) {
            mutableStateOf(bday?.date?.split("-")?.size == 3)
        }
        var categoryType by remember(showAddOccasionDialog, bday) {
            mutableStateOf(bday?.category?.split("|")?.getOrNull(0) ?: addOccasionType)
        }
        var categoryRelation by remember(showAddOccasionDialog, bday) {
            mutableStateOf(bday?.category?.split("|")?.getOrNull(1) ?: "Others")
        }
        var selectedReminders by remember(showAddOccasionDialog, bday) {
            mutableStateOf(
                bday?.reminderOptions?.split(",")?.toSet()
                    ?: if (bday?.remindMe == true) setOf("On the day (9AM)") else setOf("None")
            )
        }
        var customReminderDateText by remember(showAddOccasionDialog, bday) {
            mutableStateOf(bday?.reminderDate)
        }
        var repeatOption by remember(showAddOccasionDialog, bday) {
            mutableStateOf(bday?.repeatPattern ?: "None")
        }

        // Dynamically update repeat option when dateText changes
        LaunchedEffect(dateText) {
            if (!dateText.isNullOrBlank()) {
                if (repeatOption.startsWith("Weekly")) {
                    repeatOption = getDynamicRepeatLabel("Weekly", dateText)
                } else if (repeatOption.startsWith("Monthly")) {
                    repeatOption = getDynamicRepeatLabel("Monthly", dateText)
                } else if (repeatOption.startsWith("Yearly")) {
                    repeatOption = getDynamicRepeatLabel("Yearly", dateText)
                }
            }
        }

        var countingMode by remember(showAddOccasionDialog, bday) {
            mutableStateOf(bday?.countingMode ?: "Count Down")
        }
        var customBgImage by remember(showAddOccasionDialog, bday) {
            mutableStateOf(bday?.customBgImage)
        }
        var customFontStyle by remember(showAddOccasionDialog, bday) {
            mutableStateOf(bday?.customFontStyle ?: "Default")
        }
        var noteText by remember(showAddOccasionDialog, bday) {
            mutableStateOf(bday?.giftIdea ?: "")
        }
        var showDialogErrors by remember { mutableStateOf(false) }

        // --- Popups/Dialogs inside showAddOccasionDialog ---

        // 1. Reminder intervals multi-select popup
        var showRemindersPopup by remember { mutableStateOf(false) }
        if (showRemindersPopup) {
            val reminderChoices = listOf(
                "None",
                "On the day (9AM)",
                "1 day early (9AM)",
                "2 days early (9AM)",
                "3 days early (9AM)",
                "1 week early",
                "Custom"
            )
            AlertDialog(
                onDismissRequest = { showRemindersPopup = false },
                title = { Text("Select Reminder Intervals", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        reminderChoices.forEach { choice ->
                            val isChecked = if (choice == "Custom") {
                                selectedReminders.any { it.startsWith("Custom") }
                            } else {
                                selectedReminders.contains(choice)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newSet = selectedReminders.toMutableSet()
                                        if (choice == "None") {
                                            newSet.clear()
                                            newSet.add("None")
                                            customReminderDateText = null
                                        } else {
                                            newSet.remove("None")
                                            if (isChecked) {
                                                if (choice == "Custom") {
                                                    val customItem = newSet.find { it.startsWith("Custom") }
                                                    if (customItem != null) newSet.remove(customItem)
                                                    customReminderDateText = null
                                                } else {
                                                    newSet.remove(choice)
                                                }
                                            } else {
                                                if (choice == "Custom") {
                                                    val calendar = Calendar.getInstance()
                                                    val datePickerDialog = android.app.DatePickerDialog(
                                                        context,
                                                        { _, year, month, dayOfMonth ->
                                                            val monthStr = String.format("%02d", month + 1)
                                                            val dayStr = String.format("%02d", dayOfMonth)
                                                            val pickedDate = "$year-$monthStr-$dayStr"
                                                            customReminderDateText = pickedDate
                                                            val oldCustom = newSet.find { it.startsWith("Custom") }
                                                            if (oldCustom != null) newSet.remove(oldCustom)
                                                            newSet.add("Custom ($pickedDate)")
                                                            selectedReminders = newSet
                                                        },
                                                        calendar.get(Calendar.YEAR),
                                                        calendar.get(Calendar.MONTH),
                                                        calendar.get(Calendar.DAY_OF_MONTH)
                                                    )
                                                    datePickerDialog.show()
                                                } else {
                                                    newSet.add(choice)
                                                }
                                            }
                                            if (newSet.isEmpty()) {
                                                newSet.add("None")
                                            }
                                        }
                                        selectedReminders = newSet
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        val newSet = selectedReminders.toMutableSet()
                                        if (choice == "None") {
                                            newSet.clear()
                                            newSet.add("None")
                                            customReminderDateText = null
                                        } else {
                                            newSet.remove("None")
                                            if (checked == true) {
                                                if (choice == "Custom") {
                                                    val calendar = Calendar.getInstance()
                                                    val datePickerDialog = android.app.DatePickerDialog(
                                                        context,
                                                        { _, year, month, dayOfMonth ->
                                                            val monthStr = String.format("%02d", month + 1)
                                                            val dayStr = String.format("%02d", dayOfMonth)
                                                            val pickedDate = "$year-$monthStr-$dayStr"
                                                            customReminderDateText = pickedDate
                                                            val oldCustom = newSet.find { it.startsWith("Custom") }
                                                            if (oldCustom != null) newSet.remove(oldCustom)
                                                            newSet.add("Custom ($pickedDate)")
                                                            selectedReminders = newSet
                                                        },
                                                        calendar.get(Calendar.YEAR),
                                                        calendar.get(Calendar.MONTH),
                                                        calendar.get(Calendar.DAY_OF_MONTH)
                                                    )
                                                    datePickerDialog.show()
                                                } else {
                                                    newSet.add(choice)
                                                }
                                            } else {
                                                if (choice == "Custom") {
                                                    val customItem = newSet.find { it.startsWith("Custom") }
                                                    if (customItem != null) newSet.remove(customItem)
                                                    customReminderDateText = null
                                                } else {
                                                    newSet.remove(choice)
                                                }
                                            }
                                            if (newSet.isEmpty()) {
                                                newSet.add("None")
                                            }
                                        }
                                        selectedReminders = newSet
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val labelText = if (choice == "Custom" && customReminderDateText != null) {
                                    "$customReminderDateText"
                                } else {
                                    choice
                                }
                                Text(labelText, fontSize = 14.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showRemindersPopup = false }, colors = ButtonDefaults.buttonColors(containerColor = BrandCyan)) {
                        Text("OK", color = Color.White)
                    }
                }
            )
        }

        // 2. Repeat Setting popup
        var showRepeatPopup by remember { mutableStateOf(false) }
        if (showRepeatPopup) {
            val repeatChoices = listOf(
                "None",
                "Daily",
                "Weekly",
                "Monthly",
                "Yearly",
                "Custom"
            )
            AlertDialog(
                onDismissRequest = { showRepeatPopup = false },
                title = { Text("Select Repeat Pattern", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeatChoices.forEach { choice ->
                            val dynamicLabel = getDynamicRepeatLabel(choice, dateText)
                            val isSelected = repeatOption == dynamicLabel || (choice == "None" && repeatOption == "None") || (choice == "Daily" && repeatOption == "Daily") || (choice == "Custom" && repeatOption == "Custom")
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        repeatOption = dynamicLabel
                                        showRepeatPopup = false
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        repeatOption = dynamicLabel
                                        showRepeatPopup = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dynamicLabel, fontSize = 14.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRepeatPopup = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 3. Occasion Type popup
        var showTypePopup by remember { mutableStateOf(false) }
        if (showTypePopup) {
            val typeChoices = listOf("Birthday", "Marriage Anniversary", "Death Anniversary", "Countdown", "Holiday")
            AlertDialog(
                onDismissRequest = { showTypePopup = false },
                title = { Text("Select Occasion Type", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        typeChoices.forEach { choice ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        categoryType = choice
                                        showTypePopup = false
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = categoryType == choice,
                                    onClick = {
                                        categoryType = choice
                                        showTypePopup = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(choice, fontSize = 14.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTypePopup = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 4. Counting Mode popup
        var showCountingModePopup by remember { mutableStateOf(false) }
        if (showCountingModePopup) {
            AlertDialog(
                onDismissRequest = { showCountingModePopup = false },
                title = { Text("Select Counting Mode", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Choose to count down to the event, or count up since the event occurred:")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    countingMode = "Count Down"
                                    showCountingModePopup = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (countingMode == "Count Down") BrandCyan else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text("Count Down", color = if (countingMode == "Count Down") Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                            Button(
                                onClick = {
                                    countingMode = "Count Up"
                                    showCountingModePopup = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (countingMode == "Count Up") BrandCyan else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text("Count Up", color = if (countingMode == "Count Up") Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCountingModePopup = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // 5. Background Image preset row popup
        var showBgImagePopup by remember { mutableStateOf(false) }
        if (showBgImagePopup) {
            AlertDialog(
                onDismissRequest = { showBgImagePopup = false },
                title = { Text("Select Background Image Preset", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("Swipe horizontally to select a high-quality preset background style:", fontSize = 13.sp)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            width = if (customBgImage.isNullOrEmpty()) 2.dp else 1.dp,
                                            color = if (customBgImage.isNullOrEmpty()) BrandCyan else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            customBgImage = null
                                            showBgImagePopup = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("None", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            items(allPresets) { imageUrl ->
                                val isSelected = customBgImage == imageUrl
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) BrandCyan else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            customBgImage = imageUrl
                                            showBgImagePopup = false
                                        }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showBgImagePopup = false }, colors = ButtonDefaults.buttonColors(containerColor = BrandCyan)) {
                        Text("Done", color = Color.White)
                    }
                }
            )
        }

        // 6. Font Style popup
        var showFontStylePopup by remember { mutableStateOf(false) }
        if (showFontStylePopup) {
            val fontChoices = listOf("Default", "Sans Serif", "Serif", "Monospace", "Cursive")
            AlertDialog(
                onDismissRequest = { showFontStylePopup = false },
                title = { Text("Select Font Style", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        fontChoices.forEach { font ->
                            val isSelected = customFontStyle == font
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        customFontStyle = font
                                        showFontStylePopup = false
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        customFontStyle = font
                                        showFontStylePopup = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = font,
                                    fontFamily = parseFontStyle(font),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFontStylePopup = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        AlertDialog(
            onDismissRequest = {
                showAddOccasionDialog = false
                editingBirthday = null
            },
            title = {
                Text(
                    text = if (isEdit) "EDIT OCCASION" else "ADD OCCASION",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandCyan
                )
            },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("Name of Occasion / Person *") },
                        singleLine = true,
                        isError = showDialogErrors && nameText.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note / Gift Idea") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CHOOSE DATE FORMAT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(false to "MM-DD", true to "YYYY-MM-DD").forEach { (hasYearOption, labelText) ->
                                val isSel = isYearSelected == hasYearOption
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) BrandCyan else Color.Transparent)
                                        .clickable {
                                            isYearSelected = hasYearOption
                                            dateText = null
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = labelText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("EVENT DATE *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    val calendar = Calendar.getInstance()
                                    val datePickerDialog = android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val monthStr = String.format("%02d", month + 1)
                                            val dayStr = String.format("%02d", dayOfMonth)
                                            dateText = if (isYearSelected) "$year-$monthStr-$dayStr" else "$monthStr-$dayStr"
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    )
                                    if (categoryType == "Birthday" || categoryType == "Marriage Anniversary" || categoryType == "Death Anniversary") {
                                        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
                                    }
                                    datePickerDialog.show()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = BrandCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (dateText.isNullOrEmpty()) "Select Date *" else formatBirthdayDate(dateText!!),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dateText.isNullOrEmpty() && showDialogErrors) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("RELATIONSHIP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Friend", "Family", "Relative", "Others").forEach { rel ->
                                val isSel = categoryRelation == rel
                                val color = when (rel) {
                                    "Friend" -> BrandCyan
                                    "Family" -> BrandPink
                                    "Relative" -> BrandViolet
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) color.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(1.dp, if (isSel) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable { categoryRelation = rel }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSel) color else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ADDITIONAL DETAILS & CUSTOMIZATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    // 1. Reminder Settings Tile
                    DialogTile(
                        icon = Icons.Default.Notifications,
                        title = "REMINDER INTERVALS",
                        value = if (selectedReminders.isEmpty() || selectedReminders == setOf("None")) "None" else selectedReminders.map {
                            if (it.startsWith("Custom")) "$customReminderDateText" else it
                        }.joinToString(", "),
                        onClick = { showRemindersPopup = true }
                    )

                    // 2. Repeat Pattern Tile
                    DialogTile(
                        icon = Icons.Default.Repeat,
                        title = "REPEAT PATTERN",
                        value = repeatOption,
                        onClick = { showRepeatPopup = true }
                    )

                    // 3. Select Type Tile
                    DialogTile(
                        icon = Icons.Default.Category,
                        title = "OCCASION TYPE",
                        value = categoryType,
                        onClick = { showTypePopup = true }
                    )

                    // 4. Counting Mode Tile
                    DialogTile(
                        icon = Icons.Default.HourglassTop,
                        title = "COUNTING MODE",
                        value = countingMode,
                        onClick = { showCountingModePopup = true }
                    )

                    // 5. Select Background Image Tile
                    DialogTile(
                        icon = Icons.Default.Image,
                        title = "BACKGROUND PRESET",
                        value = if (customBgImage.isNullOrEmpty()) "None" else "Selected Preset",
                        onClick = { showBgImagePopup = true }
                    )

                    // 6. Select Font Style Tile
                    DialogTile(
                        icon = Icons.Default.TextFields,
                        title = "FONT STYLE",
                        value = customFontStyle,
                        onClick = { showFontStylePopup = true }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameText.isNotBlank() && dateText != null) {
                            if (isEdit) {
                                viewModel.updateBirthday(
                                    bday!!.copy(
                                        name = nameText.trim(),
                                        date = dateText!!,
                                        giftIdea = if (noteText.isBlank()) null else noteText.trim(),
                                        category = "$categoryType|$categoryRelation",
                                        remindMe = !selectedReminders.contains("None") && selectedReminders.isNotEmpty(),
                                        reminderOptions = selectedReminders.joinToString(","),
                                        reminderDate = customReminderDateText,
                                        repeatPattern = repeatOption,
                                        countingMode = countingMode,
                                        customBgImage = customBgImage,
                                        customFontStyle = customFontStyle
                                    )
                                )
                            } else {
                                viewModel.addBirthday(
                                    name = nameText.trim(),
                                    date = dateText!!,
                                    giftIdea = if (noteText.isBlank()) null else noteText.trim(),
                                    category = "$categoryType|$categoryRelation",
                                    remindMe = !selectedReminders.contains("None") && selectedReminders.isNotEmpty(),
                                    reminderDate = customReminderDateText,
                                    reminderTime = "09:00",
                                    customBgImage = customBgImage,
                                    customTextColor = null,
                                    customFontStyle = customFontStyle,
                                    reminderOptions = selectedReminders.joinToString(","),
                                    repeatPattern = repeatOption,
                                    countingMode = countingMode
                                )
                            }
                            showAddOccasionDialog = false
                            editingBirthday = null
                        } else {
                            showDialogErrors = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isEdit) "Save" else "Add", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddOccasionDialog = false
                        editingBirthday = null
                    }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            if (birthdays.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No occasions registered. Store dates to track countdowns!",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                val sortedBirthdays = birthdays.sortedWith(
                    compareByDescending<com.example.data.BirthdayEntity> { it.isPinned }
                        .thenBy { calculateOccasionDays(it) }
                )

                sortedBirthdays.forEach { bday ->
                    val bdayType = bday.category.split("|")[0]
                    val daysLeft = calculateOccasionDays(bday)
                    val age = calculateAge(bday.date)
                    val catColor = when (bdayType) {
                        "Marriage Anniversary" -> MaterialTheme.colorScheme.tertiary
                        "Death Anniversary" -> MaterialTheme.colorScheme.primary
                        "Countdown" -> MaterialTheme.colorScheme.secondary
                        "Holiday" -> BrandAmber
                        else -> MaterialTheme.colorScheme.secondary
                    }
                    val occasionIcon = when (bdayType) {
                        "Marriage Anniversary" -> Icons.Default.Favorite
                        "Death Anniversary" -> Icons.Default.LocalFlorist
                        "Countdown" -> Icons.Default.HourglassEmpty
                        "Holiday" -> Icons.Default.Star
                        else -> Icons.Default.Cake
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (daysLeft == 0) 2.dp else if (bday.isPinned) 1.5.dp else 1.dp,
                                color = if (daysLeft == 0) BrandAmber else if (bday.isPinned) BrandAmber.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { detailedBirthday = bday }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(occasionIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(24.dp))
                            
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (bday.isPinned) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "Pinned",
                                            tint = BrandAmber,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = bday.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            val displayDaysText = when {
                                daysLeft == 0 -> {
                                    if (bdayType == "Death Anniversary") "TODAY 🕯"
                                    else if (bdayType == "Marriage Anniversary") "TODAY 💖"
                                    else if (bdayType == "Holiday") "TODAY 🎄"
                                    else if (bdayType == "Countdown") "TODAY 🚀"
                                    else "TODAY 🎂"
                                }
                                daysLeft == 1 -> "Tomorrow"
                                bday.countingMode == "Count Up" -> "$daysLeft Days ago"
                                bday.countingMode == "Count Down" -> "$daysLeft Days left"
                                else -> "In $daysLeft days"
                            }
                            val boxBg = when {
                                daysLeft == 0 -> BrandAmber.copy(alpha = 0.12f)
                                daysLeft <= 7 -> BrandRose.copy(alpha = 0.12f)
                                else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            }
                            val boxText = when {
                                daysLeft == 0 -> BrandAmber
                                daysLeft <= 7 -> BrandRose
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(boxBg)
                                    .border(
                                        width = 1.dp,
                                        color = boxText.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (daysLeft == 0) {
                                        val emoji = when (bdayType) {
                                            "Death Anniversary" -> "🕯"
                                            "Marriage Anniversary" -> "💖"
                                            "Holiday" -> "🎄"
                                            "Countdown" -> "🚀"
                                            else -> "🎂"
                                        }
                                        Text(
                                            text = emoji,
                                            fontSize = 20.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "TODAY",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = boxText,
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        Text(
                                            text = "$daysLeft",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = boxText,
                                            textAlign = TextAlign.Center
                                        )
                                        val label = when {
                                            daysLeft == 1 -> "TOMORROW"
                                            bday.countingMode == "Count Up" -> "DAYS AGO"
                                            else -> "DAYS LEFT"
                                        }
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = boxText.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center
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

    if (detailedBirthday != null) {
        val bday = detailedBirthday!!
        val activeBday = birthdays.find { it.id == bday.id } ?: bday
        val bdayType = activeBday.category.split("|")[0]
        val daysLeft = daysUntilBirthday(activeBday.date)
        val formattedDate = formatBirthdayDate(activeBday.date)
        val age = calculateAge(activeBday.date)

        // States for Editing Note / Appearance
        var showNoteDialog by remember { mutableStateOf(false) }
        var currentNoteText by remember(activeBday) { mutableStateOf(activeBday.giftIdea ?: "") }
        var showAppearanceDialog by remember { mutableStateOf(false) }
        
        // UI Theme color
        val themeColor = MaterialTheme.colorScheme.primary

        // Custom background image and text color
        val textColor = if (!activeBday.customBgImage.isNullOrEmpty()) Color.White else MaterialTheme.colorScheme.onBackground
        val adaptiveTextColor = textColor

        val presetImages = remember {
            com.example.ui.theme.BackgroundPresets.textures +
            com.example.ui.theme.BackgroundPresets.abstractImages +
            com.example.ui.theme.BackgroundPresets.cityscapes +
            com.example.ui.theme.BackgroundPresets.landscapes
        }

        val colorOptions = listOf(
            Pair("White", "#FFFFFF"),
            Pair("Black", "#000000"),
            Pair("Gold", "#FFD700"),
            Pair("Sky Blue", "#0EA5E9"),
            Pair("Mint Green", "#10B981"),
            Pair("Coral Red", "#EF4444"),
            Pair("Amethyst", "#7C3AED"),
            Pair("Soft Rose", "#EC4899"),
            Pair("Vivid Amber", "#F59E0B")
        )

        Dialog(
            onDismissRequest = { detailedBirthday = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background image
                    if (!activeBday.customBgImage.isNullOrEmpty()) {
                        AsyncImage(
                            model = activeBday.customBgImage,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Scrim overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { detailedBirthday = null }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = adaptiveTextColor
                                )
                            }

                            // 3 Dots Options Menu
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = adaptiveTextColor
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            editingBirthday = activeBday
                                            detailedBirthday = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = BrandRose) },
                                        onClick = {
                                            showMenu = false
                                            viewModel.deleteBirthday(activeBday.id)
                                            detailedBirthday = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (activeBday.isPinned) "Unpin from Top" else "Pin to Top") },
                                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, tint = BrandAmber) },
                                        onClick = {
                                            showMenu = false
                                            val updated = activeBday.copy(isPinned = !activeBday.isPinned)
                                            viewModel.updateBirthday(updated)
                                        }
                                    )
                                }
                            }
                        }

                        // Center Content (Name, Big Days, Date)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val activeFont = parseFontStyle(activeBday.customFontStyle)
                            val adaptiveTextColor = if (!activeBday.customBgImage.isNullOrEmpty()) textColor else MaterialTheme.colorScheme.onBackground

                            // Occasion Name
                            Text(
                                text = activeBday.name,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = activeFont,
                                color = adaptiveTextColor,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Big Days Number
                            val daysLabel = if (daysLeft == 0) {
                                if (bdayType == "Death Anniversary") "TODAY 🕯️"
                                else if (bdayType == "Marriage Anniversary") "TODAY! 💖"
                                else if (bdayType == "Holiday") "TODAY! 🎄"
                                else "TODAY! 🎂"
                            } else if (daysLeft == 1 && activeBday.countingMode != "Count Up") {
                                "Tomorrow"
                            } else if (activeBday.countingMode == "Count Up") {
                                "$daysLeft Days ago"
                            } else {
                                "In $daysLeft Days"
                            }
                            Text(
                                text = daysLabel,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = activeFont,
                                color = if (!activeBday.customBgImage.isNullOrEmpty()) textColor else themeColor,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Date
                            Text(
                                text = buildString {
                                    append("Date: $formattedDate")
                                    if (age != null) {
                                        if (bdayType == "Death Anniversary") {
                                            append(" · Years passed: $age")
                                        } else if (bdayType == "Marriage Anniversary") {
                                            append(" · Years: $age")
                                        } else {
                                            append(" · Age: $age")
                                        }
                                    }
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = activeFont,
                                color = if (!activeBday.customBgImage.isNullOrEmpty()) textColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )

                            // Note if present
                            if (!activeBday.giftIdea.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (!activeBday.customBgImage.isNullOrEmpty()) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Note,
                                            contentDescription = null,
                                            tint = if (!activeBday.customBgImage.isNullOrEmpty()) textColor else themeColor
                                        )
                                        Text(
                                            text = activeBday.giftIdea ?: "",
                                            fontSize = 14.sp,
                                            fontFamily = activeFont,
                                            color = if (!activeBday.customBgImage.isNullOrEmpty()) textColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Bar with Note, Font Style, and Appearance icon
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val iconColor = if (!activeBday.customBgImage.isNullOrEmpty()) textColor else MaterialTheme.colorScheme.onSurface
                            val buttonBg = if (!activeBday.customBgImage.isNullOrEmpty()) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant

                            // Note Icon Button
                            IconButton(
                                onClick = { showNoteDialog = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = buttonBg,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NoteAdd,
                                    contentDescription = "Add Note",
                                    tint = iconColor
                                )
                            }

                            // Font Style Selector Button
                            var showFontDialog by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { showFontDialog = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = buttonBg,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = "Select Font Style",
                                    tint = iconColor
                                )
                            }

                            if (showFontDialog) {
                                AlertDialog(
                                    onDismissRequest = { showFontDialog = false },
                                    title = { Text("Choose Font Style") },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf("Default", "Sans Serif", "Serif", "Monospace", "Cursive").forEach { font ->
                                                val isSelected = activeBday.customFontStyle == font
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            viewModel.updateBirthday(activeBday.copy(customFontStyle = font))
                                                            showFontDialog = false
                                                        }
                                                        .padding(vertical = 8.dp)
                                                ) {
                                                    RadioButton(
                                                        selected = isSelected,
                                                        onClick = {
                                                            viewModel.updateBirthday(activeBday.copy(customFontStyle = font))
                                                            showFontDialog = false
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = font,
                                                        fontFamily = parseFontStyle(font),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showFontDialog = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }

                            // Appearance Icon Button
                            IconButton(
                                onClick = { showAppearanceDialog = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = buttonBg,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Appearance Settings",
                                    tint = iconColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // dialog to edit note
        if (showNoteDialog) {
            AlertDialog(
                onDismissRequest = { showNoteDialog = false },
                title = { Text("Occasion Note") },
                text = {
                    OutlinedTextField(
                        value = currentNoteText,
                        onValueChange = { currentNoteText = it },
                        label = { Text("Add note or gift idea") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showNoteDialog = false
                            val updated = activeBday.copy(giftIdea = currentNoteText)
                            viewModel.updateBirthday(updated)
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNoteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Dialog/BottomSheet to edit appearance
        if (showAppearanceDialog) {
            AlertDialog(
                onDismissRequest = { showAppearanceDialog = false },
                title = { Text("Customize Appearance", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Background Image", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        
                        // Preset backgrounds horizontal list
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                // None/Default Background
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            width = if (activeBday.customBgImage.isNullOrEmpty()) 2.dp else 1.dp,
                                            color = if (activeBday.customBgImage.isNullOrEmpty()) themeColor else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            val updated = activeBday.copy(customBgImage = null)
                                            viewModel.updateBirthday(updated)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("None", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            items(presetImages) { imageUrl ->
                                val isSelected = activeBday.customBgImage == imageUrl
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) themeColor else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            val updated = activeBday.copy(customBgImage = imageUrl)
                                            viewModel.updateBirthday(updated)
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Font Style", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val fonts = listOf("Default", "Sans Serif", "Serif", "Monospace", "Cursive")
                            items(fonts) { font ->
                                val isSelected = activeBday.customFontStyle == font
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) themeColor.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(1.dp, if (isSelected) themeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            val updated = activeBday.copy(customFontStyle = font)
                                            viewModel.updateBirthday(updated)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = font,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = parseFontStyle(font),
                                        color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showAppearanceDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                    ) {
                        Text("Done", color = Color.White)
                    }
                }
            )
        }
    }
}

private fun parseHexColor(hex: String?, default: Color): Color {
    if (hex.isNullOrEmpty()) return default
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        default
    }
}

@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp)
                ),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                maxLines = if (singleLine) 1 else 3
                            )
                        }
                        innerTextField()
                    }
                }
            }
        )
        if (isError && !errorText.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = errorText,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun GrocerySection(viewModel: TrackWiseViewModel) {
    val focusManager = LocalFocusManager.current
    val groceryItems by viewModel.allGroceryItems.collectAsState()

    var editingGroceryItem by remember { mutableStateOf<com.example.data.GroceryItemEntity?>(null) }
    if (editingGroceryItem != null) {
        val gItem = editingGroceryItem!!
        var editName by remember(gItem) { mutableStateOf(gItem.name) }
        var editQuantity by remember(gItem) { mutableStateOf(gItem.quantity) }
        var editCategory by remember(gItem) { mutableStateOf(gItem.category) }
        var editPriceInput by remember(gItem) { mutableStateOf(gItem.price?.toString() ?: "") }
        var editNumericQuantity by remember(gItem) { mutableStateOf(gItem.numericQuantity?.toString() ?: "") }

        val scrollState = rememberScrollState()
        LaunchedEffect(scrollState.isScrollInProgress) {
            if (scrollState.isScrollInProgress) {
                focusManager.clearFocus()
            }
        }

        AlertDialog(
            onDismissRequest = { editingGroceryItem = null },
            title = { Text("Edit Grocery Item 🛒", fontWeight = FontWeight.Bold, color = BrandViolet) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Item Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editQuantity,
                        onValueChange = { editQuantity = it },
                        label = { Text("Quantity Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPriceInput,
                        onValueChange = { editPriceInput = it },
                        label = { Text("Price (INR)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNumericQuantity,
                        onValueChange = { editNumericQuantity = it },
                        label = { Text("Numeric Quantity") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editName.isNotBlank()) {
                            val updatedItem = gItem.copy(
                                name = editName,
                                quantity = editQuantity,
                                category = editCategory,
                                price = editPriceInput.toDoubleOrNull(),
                                numericQuantity = editNumericQuantity.toDoubleOrNull()
                            )
                            viewModel.updateGroceryItem(updatedItem)
                            editingGroceryItem = null
                        }
                    }
                ) {
                    Text("Save Changes", color = BrandViolet, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingGroceryItem = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    var showForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Produce") }
    var filterState by remember { mutableStateOf("All") } // "All", "Pending", "Completed"

    // Pricing Options
    var enablePricing by remember { mutableStateOf(false) }
    var priceInput by remember { mutableStateOf("") }
    var priceUnit by remember { mutableStateOf("single item") }
    var numericQuantityInput by remember { mutableStateOf("") }

    var showErrors by remember { mutableStateOf(false) }
    val nameError = if (name.isBlank()) "Item Name is required" else null
    val priceError = if (enablePricing && (priceInput.toDoubleOrNull() ?: -1.0) <= 0.0) "Enter a valid positive price" else null
    val qtyError = if (enablePricing && numericQuantityInput.isNotBlank() && (numericQuantityInput.toDoubleOrNull() ?: -1.0) <= 0.0) "Enter a valid positive quantity" else null

    val categories = listOf("Produce", "Dairy", "Bakery", "Pantry", "Meat & Seafood", "Beverages", "Other")

    // Calculations
    val totalCost = groceryItems.filter { it.price != null }.sumOf {
        val p = it.price ?: 0.0
        val q = it.numericQuantity ?: 1.0
        p * q
    }

    val completedCost = groceryItems.filter { it.completed && it.price != null }.sumOf {
        val p = it.price ?: 0.0
        val q = it.numericQuantity ?: 1.0
        p * q
    }

    val pendingCost = groceryItems.filter { !it.completed && it.price != null }.sumOf {
        val p = it.price ?: 0.0
        val q = it.numericQuantity ?: 1.0
        p * q
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Actions Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Grocery List 🛒",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (groceryItems.any { it.completed }) {
                    TextButton(
                        onClick = { viewModel.clearCompletedGroceries() },
                        colors = ButtonDefaults.textButtonColors(contentColor = BrandRose),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("clear bought", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { showForm = !showForm },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showForm) MaterialTheme.colorScheme.surfaceVariant else BrandViolet,
                        contentColor = if (showForm) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp).height(34.dp)
                ) {
                    Icon(
                        imageVector = if (showForm) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showForm) "Close" else "Add Item", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- Add Item Form ---
        if (showForm) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add New Grocery Item",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    CompactTextField(
                        value = name,
                        onValueChange = { 
                            name = it 
                            if (it.isNotBlank()) showErrors = false
                        },
                        label = "Item Name *",
                        placeholder = "e.g., Organic Bananas 🍌",
                        isError = showErrors && nameError != null,
                        errorText = nameError
                    )

                    CompactTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = "Quantity / Notes",
                        placeholder = "e.g., 1 dozen, 2 litres, 500g"
                    )

                    // Pricing Toggle Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                enablePricing = !enablePricing 
                                showErrors = false
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = enablePricing,
                            onCheckedChange = { 
                                enablePricing = it 
                                showErrors = false
                            },
                            colors = CheckboxDefaults.colors(checkedColor = BrandViolet)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Price & Cost Tracking (Optional)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    AnimatedVisibility(visible = enablePricing) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CompactTextField(
                                    value = priceInput,
                                    onValueChange = { 
                                        priceInput = it 
                                        showErrors = false
                                    },
                                    label = "Price ($) *",
                                    placeholder = "e.g., 2.50",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = showErrors && priceError != null,
                                    errorText = priceError,
                                    modifier = Modifier.weight(1f)
                                )

                                CompactTextField(
                                    value = numericQuantityInput,
                                    onValueChange = { 
                                        numericQuantityInput = it 
                                        showErrors = false
                                    },
                                    label = "Quantity Value",
                                    placeholder = "e.g., 3, 1.5 (Defaults to 1)",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = showErrors && qtyError != null,
                                    errorText = qtyError,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Select Price Unit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("single item", "single kg", "single gram", "single set").forEach { unit ->
                                        val isSel = priceUnit == unit
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) BrandViolet.copy(alpha = 0.15f) else Color.Transparent)
                                                .border(
                                                    1.dp,
                                                    if (isSel) BrandViolet else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { priceUnit = unit }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when(unit) {
                                                    "single item" -> "Item"
                                                    "single kg" -> "kg"
                                                    "single gram" -> "gram"
                                                    "single set" -> "Set"
                                                    else -> unit
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) BrandViolet else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Category Selection
                    Column {
                        Text(
                            text = "Category",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Dropdown Indicator",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { expanded = !expanded }
                            )
                            
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(MaterialTheme.colorScheme.surface)
                             ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat, fontSize = 13.sp) },
                                        onClick = {
                                            category = cat
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (nameError == null && priceError == null && qtyError == null) {
                                val parsedPrice = if (enablePricing) priceInput.toDoubleOrNull() else null
                                val parsedNumericQty = if (enablePricing) numericQuantityInput.toDoubleOrNull() ?: 1.0 else null
                                viewModel.addGroceryItem(
                                    name = name.trim(),
                                    quantity = if (quantity.isBlank()) "1" else quantity.trim(),
                                    category = category,
                                    price = parsedPrice,
                                    priceUnit = if (enablePricing) priceUnit else null,
                                    numericQuantity = parsedNumericQty
                                )
                                // Reset form fields
                                name = ""
                                quantity = ""
                                priceInput = ""
                                numericQuantityInput = ""
                                enablePricing = false
                                showForm = false
                                showErrors = false
                            } else {
                                showErrors = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("Add to Checklist", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // --- Filter Chips ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("All", "Pending", "Bought").forEach { tab ->
                val isSelected = filterState == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) BrandViolet.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) BrandViolet else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { filterState = tab }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tab,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) BrandViolet else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // --- Items List ---
        val filteredList = when (filterState) {
            "Pending" -> groceryItems.filter { !it.completed }
            "Bought" -> groceryItems.filter { it.completed }
            else -> groceryItems
        }

        if (filteredList.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No items found",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = if (filterState == "All") "Your grocery checklist is empty. Tap 'Add Item' to start planning your shopping!" else "No items in this category.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredList.forEach { item ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.completed) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (item.completed) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingGroceryItem = item }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Checkbox Circle Button
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable { viewModel.toggleGroceryItem(item) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (item.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Toggle completion",
                                        tint = if (item.completed) BrandGreen else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = item.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textDecoration = if (item.completed) TextDecoration.LineThrough else null,
                                        color = if (item.completed) {
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                        } else {
                                            MaterialTheme.colorScheme.onBackground
                                        }
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        // Quantity tag
                                        Text(
                                            text = "Qty: ${item.quantity}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                        )

                                        // Pricing info tag
                                        if (item.price != null && item.price > 0) {
                                            val qtyVal = item.numericQuantity ?: 1.0
                                            val subTotal = item.price * qtyVal
                                            val displayUnit = when (item.priceUnit) {
                                                "single item" -> "item"
                                                "single kg" -> "kg"
                                                "single gram" -> "g"
                                                "single set" -> "set"
                                                else -> item.priceUnit ?: "item"
                                            }
                                            Text(
                                                text = String.format("· $%.2f/%s", item.price, displayUnit),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = BrandViolet
                                            )
                                            Text(
                                                text = String.format("(Total: $%.2f)", subTotal),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                            )
                                        }

                                        // Category bubble
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = item.category,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Delete button
                            IconButton(
                                onClick = { viewModel.deleteGroceryItem(item.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete item",
                                    tint = BrandRose,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Pricing Cost Summary Card ---
        if (totalCost > 0.0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Cost Summary & Total 💰",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Estimated Cost:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format("$%.2f", totalCost), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bought Items Cost:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format("$%.2f", completedCost), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Remaining to Buy:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format("$%.2f", pendingCost), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandRose)
                    }
                }
            }
        }
    }
}

@Composable
fun RecurrenceSelector(
    repeatType: String,
    onRepeatTypeChange: (String) -> Unit,
    customRepeatValue: String,
    onCustomRepeatValueChange: (String) -> Unit,
    customRepeatUnit: String,
    onCustomRepeatUnitChange: (String) -> Unit,
    customRepeatDaysOfWeek: Set<String>,
    onCustomRepeatDaysOfWeekChange: (Set<String>) -> Unit,
    startDate: String,
    onStartDateChange: (String) -> Unit,
    endDate: String,
    onEndDateChange: (String) -> Unit,
    untilIStop: Boolean,
    onUntilIStopChange: (Boolean) -> Unit,
    themeColor: Color = BrandViolet,
    startDateError: String? = null,
    endDateError: String? = null,
    showDateRange: Boolean = true
) {
    var repeatDropdownExpanded by remember { mutableStateOf(false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    val repeatOptions = listOf(
        "none" to "No Repeat",
        "daily" to "Daily",
        "weekly" to "Weekly",
        "monthly" to "Monthly",
        "yearly" to "Yearly",
        "custom" to "Custom..."
    )

    val currentLabel = repeatOptions.find { it.first == repeatType }?.second ?: "No Repeat"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Repeat Options 🔁",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = themeColor
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { repeatDropdownExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColor),
                border = BorderStroke(1.dp, themeColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(currentLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Icon(
                        imageVector = if (repeatDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = repeatDropdownExpanded,
                onDismissRequest = { repeatDropdownExpanded = false },
                modifier = Modifier
                    .widthIn(min = 200.dp, max = 300.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, themeColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                repeatOptions.forEach { (optionType, label) ->
                    DropdownMenuItem(
                        text = { Text(label, fontSize = 13.sp) },
                        onClick = {
                            onRepeatTypeChange(optionType)
                            repeatDropdownExpanded = false
                        }
                    )
                }
            }
        }

        if (repeatType == "custom") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Custom Recurrence Details", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColor)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customRepeatValue,
                            onValueChange = { onCustomRepeatValueChange(it) },
                            label = { Text("Repeat every", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Box(modifier = Modifier.weight(1.2f)) {
                            OutlinedButton(
                                onClick = { unitDropdownExpanded = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(customRepeatUnit.replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = unitDropdownExpanded,
                                onDismissRequest = { unitDropdownExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                listOf("days", "weeks", "months", "years").forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit.replaceFirstChar { it.uppercase() }, fontSize = 13.sp) },
                                        onClick = {
                                            onCustomRepeatUnitChange(unit)
                                            unitDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (customRepeatUnit == "weeks") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Select Days of Week:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val daysOfWeekList = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                daysOfWeekList.forEach { day ->
                                    val isSelected = customRepeatDaysOfWeek.contains(day)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) themeColor else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable {
                                                val nextSet = customRepeatDaysOfWeek.toMutableSet()
                                                if (isSelected) {
                                                    nextSet.remove(day)
                                                } else {
                                                    nextSet.add(day)
                                                }
                                                onCustomRepeatDaysOfWeekChange(nextSet)
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (repeatType != "none" && showDateRange) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Active Range (Start & End Dates) 📅",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = themeColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Start Date Input
                CompactTextField(
                    value = startDate,
                    onValueChange = { onStartDateChange(it) },
                    label = "Start Date (YYYY-MM-DD)",
                    placeholder = "e.g. 2026-07-05",
                    isError = startDateError != null,
                    errorText = startDateError,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // End Date Choice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeColor.copy(alpha = 0.08f))
                    .clickable { 
                        onUntilIStopChange(!untilIStop)
                        if (!untilIStop) {
                            onEndDateChange("")
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Until I Stop (Endless Repeat)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Checkbox(
                    checked = untilIStop,
                    onCheckedChange = { 
                        onUntilIStopChange(it)
                        if (it) {
                            onEndDateChange("")
                        }
                    },
                    colors = CheckboxDefaults.colors(checkedColor = themeColor)
                )
            }

            if (!untilIStop) {
                Spacer(modifier = Modifier.height(8.dp))
                CompactTextField(
                    value = endDate,
                    onValueChange = { onEndDateChange(it) },
                    label = "End Date (YYYY-MM-DD)",
                    placeholder = "e.g. 2026-08-30",
                    isError = endDateError != null,
                    errorText = endDateError,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun DatePickerField(
    dateStr: String,
    label: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    tintColor: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    val parsedDate = remember(dateStr) {
        try {
            if (dateStr.length == 5) { // MM-DD or DD/MM
                val parts = dateStr.split("/", "-")
                val cal = Calendar.getInstance()
                cal.set(Calendar.MONTH, (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1)
                cal.set(Calendar.DAY_OF_MONTH, parts.getOrNull(0)?.toIntOrNull() ?: 1)
                cal.time
            } else {
                TrackWiseUtils.parseDate(dateStr)
            }
        } catch (e: Exception) {
            Date()
        }
    }
    val calendar = remember(parsedDate) {
        Calendar.getInstance().apply { time = parsedDate }
    }

    val datePickerDialog = remember(calendar) {
        android.app.DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                }
                onDateSelected(TrackWiseUtils.formatDate(selectedCal.time, "yyyy-MM-dd"))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = dateStr,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
            label = { Text(label, fontSize = 10.sp, maxLines = 1) },
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = tintColor, modifier = Modifier.size(18.dp)) },
            trailingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit Date", tint = tintColor, modifier = Modifier.size(14.dp)) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { datePickerDialog.show() }
        )
    }
}

@Composable
fun TimePickerField(
    timeStr: String?, // HH:MM or null/blank
    label: String,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    tintColor: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    val displayValue = if (timeStr.isNullOrBlank()) "Not Set" else timeStr
    val parts = remember(timeStr) { (timeStr ?: "12:00").split(":") }
    val hour = remember(parts) { parts.getOrNull(0)?.toIntOrNull() ?: 12 }
    val minute = remember(parts) { parts.getOrNull(1)?.toIntOrNull() ?: 0 }

    val timePickerDialog = remember(hour, minute) {
        android.app.TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(formattedTime)
            },
            hour,
            minute,
            true // 24 hour view
        )
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
            label = { Text(label, fontSize = 10.sp, maxLines = 1) },
            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = tintColor, modifier = Modifier.size(18.dp)) },
            trailingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit Time", tint = tintColor, modifier = Modifier.size(14.dp)) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { timePickerDialog.show() }
        )
    }
}

