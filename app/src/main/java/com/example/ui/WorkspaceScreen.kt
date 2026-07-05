package com.example.ui

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

import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource

@Composable
fun WorkspaceScreen(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    val activeSubTab by viewModel.workspaceSubTab.collectAsState()
    val subTabs = listOf("Tasks", "Habit Runways", "Wishlist", "Occasions", "Timer & Stopwatch", "Grocery List")
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
        // --- Header Section ---
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Workspace",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Manage tasks, habit runways, wishlist, and birthdays in one place.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // --- Workspace Sub-Tabs Dropdown Menu ---
        item {
            var workspaceDropdownExpanded by remember { mutableStateOf(false) }
            val currentLabel = subTabs.getOrElse(activeSubTab) { "Select Section" }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BrandViolet.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { workspaceDropdownExpanded = !workspaceDropdownExpanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (activeSubTab) {
                                    0 -> Icons.Default.Assignment
                                    1 -> Icons.Default.Repeat
                                    2 -> Icons.Default.Star
                                    3 -> Icons.Default.Cake
                                    4 -> Icons.Default.Timer
                                    else -> Icons.Default.ShoppingCart
                                },
                                contentDescription = null,
                                tint = BrandViolet,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = currentLabel.uppercase(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Icon(
                            imageVector = if (workspaceDropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Dropdown Indicator",
                            tint = BrandViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = workspaceDropdownExpanded,
                    onDismissRequest = { workspaceDropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    subTabs.forEachIndexed { index, label ->
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Default.Assignment
                                        1 -> Icons.Default.Repeat
                                        2 -> Icons.Default.Star
                                        3 -> Icons.Default.Cake
                                        4 -> Icons.Default.Timer
                                        else -> Icons.Default.ShoppingCart
                                    },
                                    contentDescription = null,
                                    tint = if (activeSubTab == index) BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (activeSubTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (activeSubTab == index) BrandViolet else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                viewModel.setWorkspaceSubTab(index)
                                workspaceDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

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
    val tasks by viewModel.allTasks.collectAsState()
    
    var showForm by remember { mutableStateOf(false) }
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var project by remember { mutableStateOf("Work") }
    var priority by remember { mutableStateOf("medium") }
    var deadline by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }
    var reminderTime by remember { mutableStateOf("") }

    var repeatType by remember { mutableStateOf("none") }
    var customRepeatValue by remember { mutableStateOf("1") }
    var customRepeatUnit by remember { mutableStateOf("days") }
    var customRepeatDaysOfWeek by remember { mutableStateOf(emptySet<String>()) }

    val projects = listOf("Personal", "Work")
    val priorities = listOf("low", "medium", "high")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Toggle add task card
        Button(
            onClick = { showForm = !showForm },
            colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(if (showForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null, tint = Color.White)
            Text(if (showForm) "Close Form" else "Add New Task", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }

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
                        onValueChange = { title = it },
                        label = { Text("Task Title *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
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

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CompactTextField(
                            value = deadline,
                            onValueChange = { deadline = it },
                            label = "Deadline (YYYY-MM-DD)",
                            placeholder = "2026-06-30",
                            modifier = Modifier.weight(1f)
                        )

                        CompactTextField(
                            value = reminderTime,
                            onValueChange = { reminderTime = it },
                            label = "Reminder (HH:MM)",
                            placeholder = "e.g. 08:30",
                            modifier = Modifier.weight(1f)
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
                        themeColor = BrandViolet
                    )

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                viewModel.addTask(
                                    title = title,
                                    description = description,
                                    project = project,
                                    priority = priority,
                                    points = 0,
                                    deadline = deadline,
                                    reminderTime = if (reminderTime.isBlank()) null else reminderTime,
                                    repeatType = repeatType,
                                    customRepeatValue = customRepeatValue.toIntOrNull() ?: 1,
                                    customRepeatUnit = customRepeatUnit,
                                    customRepeatDaysOfWeek = if (repeatType == "custom" && customRepeatUnit == "weeks") customRepeatDaysOfWeek.joinToString(",") else null
                                )
                                // Reset
                                title = ""
                                description = ""
                                repeatType = "none"
                                customRepeatValue = "1"
                                customRepeatUnit = "days"
                                customRepeatDaysOfWeek = emptySet()
                                showForm = false
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
    var showSubtaskInput by remember { mutableStateOf(false) }
    var newSubtaskTitle by remember { mutableStateOf("") }
    val subtasks = TrackWiseUtils.deserializeSubTasks(task.subtasksJson)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
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

                Column(modifier = Modifier.weight(1f)) {
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
                            Text(
                                text = sub.title,
                                fontSize = 12.sp,
                                textDecoration = if (sub.completed) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (sub.completed) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.deleteSubTask(task, sub.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = BrandRose, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    if (showSubtaskInput) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
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
                                    viewModel.addSubTask(task, newSubtaskTitle)
                                    newSubtaskTitle = ""
                                    showSubtaskInput = false
                                }
                            }) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = BrandGreen)
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
    val habits by viewModel.allHabits.collectAsState()
    var showForm by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Wellness") }

    var isMultipleTimes by remember { mutableStateOf(false) }
    var multipleTimesTargetInput by remember { mutableStateOf("1") }
    var isTimeBound by remember { mutableStateOf(false) }
    var timeBoundDurationInput by remember { mutableStateOf("") }

    var repeatType by remember { mutableStateOf("none") }
    var customRepeatValue by remember { mutableStateOf("1") }
    var customRepeatUnit by remember { mutableStateOf("days") }
    var customRepeatDaysOfWeek by remember { mutableStateOf(emptySet<String>()) }

    val categories = listOf("Wellness", "Fitness", "Learning", "Productivity")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Toggle add habit card
        Button(
            onClick = { showForm = !showForm },
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
                        onValueChange = { name = it },
                        label = { Text("Habit Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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
                            onValueChange = { multipleTimesTargetInput = it },
                            label = { Text("Target Count Per Day") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (isTimeBound) {
                        OutlinedTextField(
                            value = timeBoundDurationInput,
                            onValueChange = { timeBoundDurationInput = it },
                            label = { Text("Duration / Time Constraint") },
                            placeholder = { Text("e.g. 30 mins, or 18:00") },
                            singleLine = true,
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
                        themeColor = BrandOrange
                    )

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
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
                                    customRepeatDaysOfWeek = if (repeatType == "custom" && customRepeatUnit == "weeks") customRepeatDaysOfWeek.joinToString(",") else null
                                )
                                name = ""
                                isMultipleTimes = false
                                multipleTimesTargetInput = "1"
                                isTimeBound = false
                                timeBoundDurationInput = ""
                                repeatType = "none"
                                customRepeatValue = "1"
                                customRepeatUnit = "days"
                                customRepeatDaysOfWeek = emptySet()
                                showForm = false
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

        if (habits.isEmpty()) {
            Text("No habit runways launched yet. Create one above!")
        } else {
            habits.forEach { habit ->
                HabitCard(habit = habit, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HabitCard(habit: HabitEntity, viewModel: TrackWiseViewModel) {
    val completedDays = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson)
    val today = TrackWiseUtils.getTodayString()
    val completedCountToday = completedDays.count { it == today }
    val isCompletedToday = if (habit.isMultipleTimesPerDay) {
        completedCountToday >= habit.multipleTimesTarget
    } else {
        completedCountToday >= 1
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
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
    val items by viewModel.allWishlist.collectAsState()
    var showForm by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("medium") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Toggle add wishlist item form
        Button(
            onClick = { showForm = !showForm },
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
                        onValueChange = { title = it },
                        label = { Text("Item Title *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CompactTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = "Price (₹)",
                            placeholder = "e.g. 1500",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        CompactTextField(
                            value = link,
                            onValueChange = { link = it },
                            label = "Product URL Link",
                            placeholder = "https://...",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Priority Selection
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("low", "medium", "high").forEach { prio ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (priority == prio) BrandPink else MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { priority = prio }
                            ) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(prio.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp), color = if (priority == prio) Color.White else MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                viewModel.addWishItem(
                                    title = title,
                                    price = price.toDoubleOrNull() ?: 0.0,
                                    link = if (link.isBlank()) null else link,
                                    priority = priority
                                )
                                title = ""
                                price = ""
                                link = ""
                                showForm = false
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

                        Column(modifier = Modifier.weight(1f)) {
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

@Composable
fun BirthdaySection(viewModel: TrackWiseViewModel) {
    val birthdays by viewModel.allBirthdays.collectAsState()
    var showForm by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var giftIdea by remember { mutableStateOf("") }
    var isYearSelected by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Birthday") }
    var selectedRelationship by remember { mutableStateOf("Others") }
    var listFilter by remember { mutableStateOf("All") }
    var relationFilter by remember { mutableStateOf("All") }
    var showError by remember { mutableStateOf(false) }
    var editingBirthday by remember { mutableStateOf<com.example.data.BirthdayEntity?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Toggle add birthday form & Seed custom list row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showForm = !showForm },
                colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.1f)
            ) {
                Icon(if (showForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null, tint = Color.White)
                Text(if (showForm) "Close Form" else "Add Occasion", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp), maxLines = 1)
            }

            OutlinedButton(
                onClick = { viewModel.loadCustomUserBirthdays() },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BrandPink),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPink),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = BrandPink)
                Text("Seed Custom List", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp), maxLines = 1)
            }
        }

        if (showForm) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ADD OCCASION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandCyan)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name of Person * (e.g. Syed)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Format toggle
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
                            listOf(false to "DD/MM (No Year)", true to "DD/MM/YYYY (With Year)").forEach { (hasYearOption, labelText) ->
                                val isSel = isYearSelected == hasYearOption
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) BrandCyan else Color.Transparent)
                                        .clickable { 
                                            isYearSelected = hasYearOption
                                            showError = false
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = labelText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CompactTextField(
                            value = dateText,
                            onValueChange = { 
                                dateText = it
                                showError = false
                            },
                            label = if (isYearSelected) "Date (DD/MM/YYYY) *" else "Date (DD/MM) *",
                            placeholder = if (isYearSelected) "15/10/1995" else "15/10",
                            modifier = Modifier.weight(1f)
                        )

                        CompactTextField(
                            value = giftIdea,
                            onValueChange = { giftIdea = it },
                            label = "Gift Idea / Note",
                            placeholder = "e.g. Watch",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Occasion Type Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "OCCASION TYPE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Birthday", "Marriage Anniversary", "Death Anniversary").forEach { cat ->
                                val isSel = selectedCategory == cat
                                val catColor = when (cat) {
                                    "Birthday" -> BrandCyan
                                    "Marriage Anniversary" -> BrandPink
                                    "Death Anniversary" -> BrandViolet
                                    else -> BrandAmber
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) catColor.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(1.dp, if (isSel) catColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSel) catColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Relationship Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "RELATIONSHIP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Friend", "Family", "Relative", "Others").forEach { rel ->
                                val isSel = selectedRelationship == rel
                                val relColor = when (rel) {
                                    "Friend" -> BrandCyan
                                    "Family" -> BrandPink
                                    "Relative" -> BrandViolet
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) relColor.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(1.dp, if (isSel) relColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable { selectedRelationship = rel }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = rel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSel) relColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (showError) {
                        Text(
                            text = if (isYearSelected) "Please enter a valid date in DD/MM/YYYY format (e.g. 15/10/1995)" 
                                   else "Please enter a valid date in DD/MM format (e.g. 15/10)",
                            color = BrandRose,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val parsed = parseInputDate(dateText, isYearSelected)
                            if (name.isNotBlank() && parsed != null) {
                                viewModel.addBirthday(
                                    name = name.trim(),
                                    date = parsed,
                                    giftIdea = if (giftIdea.isBlank()) null else giftIdea.trim(),
                                    category = "$selectedCategory|$selectedRelationship"
                                )
                                name = ""
                                dateText = ""
                                giftIdea = ""
                                selectedRelationship = "Others"
                                showError = false
                                showForm = false
                            } else {
                                showError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Occasion", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Segregated Filter list
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Type:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf("All", "Birthday", "Marriage Anniversary", "Death Anniversary").forEach { filter ->
                    val isSel = listFilter == filter
                    val catColor = when (filter) {
                        "All" -> BrandCyan
                        "Birthday" -> BrandCyan
                        "Marriage Anniversary" -> BrandPink
                        "Death Anniversary" -> BrandViolet
                        else -> BrandAmber
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSel) catColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { listFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Relation:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf("All", "Friends", "Family", "Relatives", "Others").forEach { filter ->
                    val isSel = relationFilter == filter
                    val catColor = when (filter) {
                        "All" -> BrandCyan
                        "Friends" -> BrandCyan
                        "Family" -> BrandPink
                        "Relatives" -> BrandViolet
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSel) catColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { relationFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (birthdays.isEmpty()) {
                Text("No occasions registered. Store dates to track countdowns!")
            } else {
                val filteredBirthdays = birthdays.filter { bday ->
                    val bdayType = bday.category.split("|")[0]
                    val matchesType = if (listFilter == "All") {
                        true
                    } else if (listFilter == "Birthday") {
                        !bdayType.equals("Marriage Anniversary", ignoreCase = true) && !bdayType.equals("Death Anniversary", ignoreCase = true)
                    } else {
                        bdayType.equals(listFilter, ignoreCase = true)
                    }

                    val bdayRelation = bday.category.split("|").getOrNull(1) ?: "Others"
                    val matchesRelation = if (relationFilter == "All") {
                        true
                    } else {
                        val relationKey = when (relationFilter) {
                            "Friends" -> "Friend"
                            "Relatives" -> "Relative"
                            else -> relationFilter // "Family", "Others"
                        }
                        bdayRelation.equals(relationKey, ignoreCase = true)
                    }

                    matchesType && matchesRelation
                }
                val sortedBirthdays = filteredBirthdays.sortedBy { daysUntilBirthday(it.date) }

                if (sortedBirthdays.isEmpty()) {
                    Text(
                        text = "No occasions in current filter criteria.",
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                } else {
                    sortedBirthdays.forEach { bday ->
                        val bdayType = bday.category.split("|")[0]
                        val daysLeft = daysUntilBirthday(bday.date)
                        val age = calculateAge(bday.date)
                        val catColor = when (bdayType) {
                            "Marriage Anniversary" -> BrandPink
                            "Death Anniversary" -> BrandViolet
                            else -> BrandCyan
                        }
                        val occasionIcon = when (bdayType) {
                            "Marriage Anniversary" -> Icons.Default.Favorite
                            "Death Anniversary" -> Icons.Default.LocalFlorist
                            else -> Icons.Default.Cake
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (daysLeft == 0) 2.dp else 1.dp,
                                    color = if (daysLeft == 0) BrandAmber else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(occasionIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(24.dp))
                                
                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = bday.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Text(
                                        text = buildString {
                                            append("Date: ${formatBirthdayDate(bday.date)}")
                                            if (age != null) {
                                                if (bdayType == "Death Anniversary") {
                                                    append(" (Years passed: $age)")
                                                } else if (bdayType == "Marriage Anniversary") {
                                                    append(" (Years: $age)")
                                                } else {
                                                    append(" (Age: $age)")
                                                }
                                            }
                                            if (bday.giftIdea != null) {
                                                append(" · Note: ${bday.giftIdea}")
                                            }
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (daysLeft == 0) BrandAmber.copy(alpha = 0.15f)
                                            else if (daysLeft <= 7) BrandRose.copy(alpha = 0.1f)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = when (daysLeft) {
                                            0 -> {
                                                if (bdayType == "Death Anniversary") "TODAY 🕯️"
                                                else if (bdayType == "Marriage Anniversary") "TODAY! 💖"
                                                else "TODAY! 🎂"
                                            }
                                            1 -> "Tomorrow"
                                            else -> "In $daysLeft days"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (daysLeft == 0) BrandAmber
                                        else if (daysLeft <= 7) BrandRose
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(onClick = { editingBirthday = bday }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Occasion", tint = BrandCyan)
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(onClick = { viewModel.deleteBirthday(bday.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = BrandRose)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingBirthday != null) {
        val bday = editingBirthday!!
        var editName by remember(bday) { mutableStateOf(bday.name) }
        val parts = remember(bday) { bday.date.split("-") }
        var editIsYearSelected by remember(bday) { mutableStateOf(parts.size == 3) }
        var editDateText by remember(bday) {
            mutableStateOf(
                if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}"
                else if (parts.size == 2) "${parts[1]}/${parts[0]}"
                else bday.date
            )
        }
        var editGiftIdea by remember(bday) { mutableStateOf(bday.giftIdea ?: "") }
        val bdayCategoryType = remember(bday) { bday.category.split("|")[0] }
        val bdayCategoryRelation = remember(bday) { bday.category.split("|").getOrNull(1) ?: "Others" }
        var editCategoryType by remember(bday) { mutableStateOf(bdayCategoryType) }
        var editCategoryRelation by remember(bday) { mutableStateOf(bdayCategoryRelation) }
        var editShowError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingBirthday = null },
            title = {
                Text(
                    text = "EDIT OCCASION",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandCyan
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CHOOSE DATE FORMAT",
                            fontSize = 10.sp,
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
                            listOf(false to "DD/MM (No Year)", true to "DD/MM/YYYY (With Year)").forEach { (hasYearOption, labelText) ->
                                val isSel = editIsYearSelected == hasYearOption
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) BrandCyan else Color.Transparent)
                                        .clickable { 
                                            editIsYearSelected = hasYearOption
                                            editShowError = false
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

                    OutlinedTextField(
                        value = editDateText,
                        onValueChange = { 
                            editDateText = it
                            editShowError = false
                        },
                        label = { Text(if (editIsYearSelected) "Date (DD/MM/YYYY) *" else "Date (DD/MM) *") },
                        placeholder = { Text(if (editIsYearSelected) "15/10/1995" else "15/10") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editGiftIdea,
                        onValueChange = { editGiftIdea = it },
                        label = { Text("Gift Idea / Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "OCCASION TYPE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Birthday", "Marriage Anniversary", "Death Anniversary").forEach { cat ->
                                val isSel = editCategoryType == cat
                                val catColor = when (cat) {
                                    "Birthday" -> BrandCyan
                                    "Marriage Anniversary" -> BrandPink
                                    "Death Anniversary" -> BrandViolet
                                    else -> BrandAmber
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) catColor.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(1.dp, if (isSel) catColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable { editCategoryType = cat }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSel) catColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "RELATIONSHIP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Friend", "Family", "Relative", "Others").forEach { rel ->
                                val isSel = editCategoryRelation == rel
                                val relColor = when (rel) {
                                    "Friend" -> BrandCyan
                                    "Family" -> BrandPink
                                    "Relative" -> BrandViolet
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) relColor.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(1.dp, if (isSel) relColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable { editCategoryRelation = rel }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = rel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSel) relColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (editShowError) {
                        Text(
                            text = if (editIsYearSelected) "Please enter a valid date in DD/MM/YYYY format" 
                                   else "Please enter a valid date in DD/MM format",
                            color = BrandRose,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = parseInputDate(editDateText, editIsYearSelected)
                        if (editName.isNotBlank() && parsed != null) {
                            viewModel.updateBirthday(
                                bday.copy(
                                    name = editName.trim(),
                                    date = parsed,
                                    giftIdea = if (editGiftIdea.isBlank()) null else editGiftIdea.trim(),
                                    category = "$editCategoryType|$editCategoryRelation"
                                )
                            )
                            editingBirthday = null
                        } else {
                            editShowError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBirthday = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
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
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
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
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
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
    }
}

@Composable
fun GrocerySection(viewModel: TrackWiseViewModel) {
    val groceryItems by viewModel.allGroceryItems.collectAsState()

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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
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
                        onValueChange = { name = it },
                        label = "Item Name *",
                        placeholder = "e.g., Organic Bananas 🍌"
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
                            .clickable { enablePricing = !enablePricing }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = enablePricing,
                            onCheckedChange = { enablePricing = it },
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
                                    onValueChange = { priceInput = it },
                                    label = "Price ($) *",
                                    placeholder = "e.g., 2.50",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )

                                CompactTextField(
                                    value = numericQuantityInput,
                                    onValueChange = { numericQuantityInput = it },
                                    label = "Quantity Value",
                                    placeholder = "e.g., 3, 1.5 (Defaults to 1)",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            if (name.isNotBlank()) {
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
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
                        modifier = Modifier.fillMaxWidth()
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

                                Column(modifier = Modifier.weight(1f)) {
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
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
    themeColor: Color = BrandViolet
) {
    var repeatDropdownExpanded by remember { mutableStateOf(false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    val repeatOptions = listOf(
        "none" to "No Repeat",
        "daily" to "Daily",
        "weekdays" to "Weekdays (Mon-Fri)",
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
                modifier = Modifier.fillMaxWidth()
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
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
                                onDismissRequest = { unitDropdownExpanded = false }
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
    }
}

