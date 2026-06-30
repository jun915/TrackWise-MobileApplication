package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

@Composable
fun WorkspaceScreen(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Tasks, 1 = Habits, 2 = Wishlist, 3 = Birthdays
    val subTabs = listOf("Tasks", "Habit Runways", "Wishlist", "Birthdays")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
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
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                subTabs.forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (activeSubTab == index) BrandViolet else Color.Transparent)
                            .clickable { activeSubTab = index }
                            .padding(vertical = 12.dp),
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

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Priority selection
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Priority", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                priorities.forEach { prio ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (priority == prio) BrandViolet else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { priority = prio }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(prio.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (priority == prio) Color.White else MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = points,
                            onValueChange = { points = it },
                            label = { Text("Pts") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(60.dp)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = deadline,
                            onValueChange = { deadline = it },
                            label = { Text("Deadline (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = reminderTime,
                            onValueChange = { reminderTime = it },
                            placeholder = { Text("e.g. 08:30") },
                            label = { Text("Reminder (HH:MM)") },
                            singleLine = true,
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
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        label = { Text("Product URL Link") },
                        singleLine = true,
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
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        placeholder = { Text("MM-DD or YYYY-MM-DD") },
                        label = { Text("Birthday Date *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = giftIdea,
                        onValueChange = { giftIdea = it },
                        label = { Text("Gift Idea") },
                        singleLine = true,
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
