package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val subTabs = listOf("Tasks", "Habit Runways", "Wishlist", "Birthdays", "Alarms & Clocks", "Grocery List")
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

        // --- Workspace Sub-Tabs Row ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .horizontalScroll(rememberScrollState())
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                subTabs.forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activeSubTab == index) BrandViolet else Color.Transparent)
                            .clickable { viewModel.setWorkspaceSubTab(index) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeSubTab == index) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
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
    var points by remember { mutableStateOf("3") }
    var deadline by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }
    var reminderTime by remember { mutableStateOf("") }

    val projects = listOf("Tasks", "Wish List", "Work", "Personal", "Health", "Learning")
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

                    // Project dropdown replacement (Simple Row selection)
                    Column {
                        Text("Project Workspace", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            projects.take(3).forEach { proj ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (project == proj) BrandViolet else MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { project = proj }
                                ) {
                                    Text(proj, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp), color = if (project == proj) Color.White else MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                        // Priority selection
                        Column(modifier = Modifier.weight(1f)) {
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

                        CompactTextField(
                            value = points,
                            onValueChange = { points = it },
                            label = "Pts",
                            placeholder = "3",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(72.dp)
                        )
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

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                viewModel.addTask(
                                    title = title,
                                    description = description,
                                    project = project,
                                    priority = priority,
                                    points = points.toIntOrNull() ?: 3,
                                    deadline = deadline,
                                    reminderTime = if (reminderTime.isBlank()) null else reminderTime
                                )
                                // Reset
                                title = ""
                                description = ""
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

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Wellness") }

    val categories = listOf("Wellness", "Fitness", "Learning", "Productivity")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addHabit(name, category)
                            name = ""
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
    val isCompletedToday = completedDays.contains(today)

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

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = habit.category,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
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

    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("medium") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
@Composable
fun BirthdaySection(viewModel: TrackWiseViewModel) {
    val birthdays by viewModel.allBirthdays.collectAsState()

    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var giftIdea by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ADD FRIEND'S BIRTHDAY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandCyan)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Friend's Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = "Birthday Date *",
                        placeholder = "MM-DD or YYYY-MM-DD",
                        modifier = Modifier.weight(1f)
                    )

                    CompactTextField(
                        value = giftIdea,
                        onValueChange = { giftIdea = it },
                        label = "Gift Idea",
                        placeholder = "e.g. Perfume",
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        if (name.isNotBlank() && date.isNotBlank()) {
                            viewModel.addBirthday(name, date, if (giftIdea.isBlank()) null else giftIdea)
                            name = ""
                            date = ""
                            giftIdea = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Birthday", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (birthdays.isEmpty()) {
            Text("No birthdays registered. Store dates to track countdowns!")
        } else {
            birthdays.forEach { bday ->
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
                        Icon(Icons.Default.Cake, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(24.dp))
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bday.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Date: ${bday.date} ${if (bday.giftIdea != null) "· Gift: ${bday.giftIdea}" else ""}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }

                        IconButton(onClick = { viewModel.deleteBirthday(bday.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = BrandRose)
                        }
                    }
                }
            }
        }
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
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)) } } else null,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandViolet,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
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

    val categories = listOf("Produce", "Dairy", "Bakery", "Pantry", "Meat & Seafood", "Beverages", "Other")

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
                text = "Grocery Check List 🛒",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (groceryItems.any { it.completed }) {
                    TextButton(
                        onClick = { viewModel.clearCompletedGroceries() },
                        colors = ButtonDefaults.textButtonColors(contentColor = BrandRose)
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Bought", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { showForm = !showForm },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showForm) MaterialTheme.colorScheme.surfaceVariant else BrandViolet,
                        contentColor = if (showForm) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = if (showForm) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showForm) "Close" else "Add Item", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                .clickable { expanded = !expanded }
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
                                viewModel.addGroceryItem(
                                    name = name.trim(),
                                    quantity = if (quantity.isBlank()) "1" else quantity.trim(),
                                    category = category
                                )
                                // Reset form fields
                                name = ""
                                quantity = ""
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
    }
}

