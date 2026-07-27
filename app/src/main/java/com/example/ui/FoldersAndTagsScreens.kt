package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TaskEntity
import com.example.data.HabitEntity
import com.example.utils.TrackWiseUtils
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFoldersScreen(
    viewModel: TrackWiseViewModel,
    onBack: () -> Unit,
    onNavigateToWorkspaceWithFolder: (String) -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    val habits by viewModel.allHabits.collectAsState()
    val customFolders by viewModel.customFolders.collectAsState()
    
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    
    // Dedicated folder detail view state (opens separately with separate tabs for tasks & habits)
    var activeDetailFolder by remember { mutableStateOf<String?>(null) }
    var selectedHabitForDetail by remember { mutableStateOf<HabitEntity?>(null) }
    
    // Track which folder is expanding its "Assign Tasks & Habits" list
    var expandedFolderAssign by remember { mutableStateOf<String?>(null) }
    
    val allFolders = remember(tasks, habits, customFolders) {
        val defaults = listOf("Inbox", "Work", "Personal", "Shopping", "Learning", "Wish List", "Fitness", "Welcome")
        val dynamicTasks = tasks.map { it.project }.filter { it.isNotBlank() }
        val dynamicHabits = habits.flatMap { it.section.split(",") }.map { it.trim() }.filter { it.isNotBlank() }
        (defaults + customFolders + dynamicTasks + dynamicHabits).distinct()
    }

    if (selectedHabitForDetail != null) {
        HabitDetailScreen(
            habitId = selectedHabitForDetail!!.id,
            viewModel = viewModel,
            onBack = { selectedHabitForDetail = null },
            onEditHabit = { habit ->
                selectedHabitForDetail = null
            }
        )
        return
    }

    if (activeDetailFolder != null) {
        FolderDetailView(
            folder = activeDetailFolder!!,
            viewModel = viewModel,
            onBack = { activeDetailFolder = null },
            onHabitClick = { habit ->
                selectedHabitForDetail = habit
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Task & Habit Folders",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Button(
                        onClick = { showAddFolderDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandCyan.copy(alpha = 0.12f),
                            contentColor = BrandCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "Add Folder",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Folder", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                items(allFolders) { folder ->
                    val folderTasksCount = remember(tasks, folder) {
                        tasks.count { it.project.equals(folder, ignoreCase = true) }
                    }
                    val folderHabitsCount = remember(habits, folder) {
                        habits.count { it.section.split(",").map { it.trim().lowercase() }.contains(folder.lowercase()) }
                    }
                    val isExpanded = expandedFolderAssign == folder

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isExpanded) BrandViolet.copy(alpha = 0.3f) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Folder row header - opens separately in dedicated FolderDetailView
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeDetailFolder = folder }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(BrandViolet.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = BrandViolet,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = folder,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "$folderTasksCount tasks · $folderHabitsCount habits",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Assign tasks button
                                    Button(
                                        onClick = {
                                            expandedFolderAssign = if (isExpanded) null else folder
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isExpanded) BrandViolet else BrandViolet.copy(alpha = 0.12f),
                                            contentColor = if (isExpanded) Color.White else BrandViolet
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.Check else Icons.Default.Assignment,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Assign", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Open",
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                    )
                                }
                            }

                            // Expanded Assign Tasks & Habits List
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                                        .padding(12.dp)
                                ) {
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 8.dp))
                                    
                                    // TASKS SECTION
                                    Text(
                                        text = "Tasks in folder '$folder':",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandViolet,
                                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                                    )

                                    if (tasks.isEmpty()) {
                                        Text(
                                            text = "No tasks available to assign. Add tasks first!",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                                        )
                                    } else {
                                        tasks.forEach { task ->
                                            val isAssigned = task.project.equals(folder, ignoreCase = true)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        val targetProj = if (isAssigned) "Inbox" else folder
                                                        viewModel.updateTask(task.copy(project = targetProj))
                                                    }
                                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Task,
                                                        contentDescription = null,
                                                        tint = if (isAssigned) BrandViolet else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = task.title,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isAssigned) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isAssigned) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                                    )
                                                }

                                                Checkbox(
                                                    checked = isAssigned,
                                                    onCheckedChange = { checked ->
                                                        val targetProj = if (checked == true) folder else "Inbox"
                                                        viewModel.updateTask(task.copy(project = targetProj))
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = BrandViolet)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 8.dp))

                                    // HABITS SECTION
                                    Text(
                                        text = "Habits in folder '$folder':",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandOrange,
                                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                                    )

                                    if (habits.isEmpty()) {
                                        Text(
                                            text = "No habits available to assign. Add habits first!",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                                        )
                                    } else {
                                        habits.forEach { habit ->
                                            val isAssigned = habit.section.split(",").map { it.trim().lowercase() }.contains(folder.lowercase())
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        val currentSections = habit.section.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                                                        if (isAssigned) {
                                                            currentSections.removeAll { it.equals(folder, ignoreCase = true) }
                                                        } else {
                                                            if (!currentSections.any { it.equals(folder, ignoreCase = true) }) {
                                                                currentSections.add(folder)
                                                            }
                                                        }
                                                        viewModel.updateHabit(habit.copy(section = currentSections.joinToString(",")))
                                                    }
                                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Favorite,
                                                        contentDescription = null,
                                                        tint = if (isAssigned) BrandOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = habit.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isAssigned) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isAssigned) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                                    )
                                                }

                                                Checkbox(
                                                    checked = isAssigned,
                                                    onCheckedChange = { checked ->
                                                        val currentSections = habit.section.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                                                        if (checked == true) {
                                                            if (!currentSections.any { it.equals(folder, ignoreCase = true) }) {
                                                                currentSections.add(folder)
                                                            }
                                                        } else {
                                                            currentSections.removeAll { it.equals(folder, ignoreCase = true) }
                                                        }
                                                        viewModel.updateHabit(habit.copy(section = currentSections.joinToString(",")))
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = BrandOrange)
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

        // Add Folder Dialog
        if (showAddFolderDialog) {
            AlertDialog(
                onDismissRequest = { showAddFolderDialog = false },
                title = {
                    Text(
                        text = "Create New Folder",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                text = {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        placeholder = { Text("Folder Name", fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFolderName.isNotBlank()) {
                                viewModel.addCustomFolder(newFolderName)
                                newFolderName = ""
                                showAddFolderDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                    ) {
                        Text("Add", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddFolderDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashtagsScreen(
    viewModel: TrackWiseViewModel,
    onBack: () -> Unit,
    onNavigateToWorkspaceWithTag: (String) -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    val habits by viewModel.allHabits.collectAsState()
    val customTags by viewModel.customTags.collectAsState()
    
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    
    // Dedicated details state
    var activeDetailTag by remember { mutableStateOf<String?>(null) }
    var selectedHabitForDetail by remember { mutableStateOf<HabitEntity?>(null) }
    
    // Track which tag is expanding its "Assign Tasks" list
    var expandedTagAssign by remember { mutableStateOf<String?>(null) }
    
    // Extract dynamic tags from tasks (checking description, title, or notes)
    val allTags = remember(tasks, habits, customTags) {
        val extracted = mutableSetOf<String>()
        tasks.forEach { task ->
            val textToSearch = "${task.title} ${task.description} ${task.notes}"
            val words = textToSearch.split(" ", "\n", ",", ";")
            words.forEach { word ->
                if (word.startsWith("#") && word.length > 1) {
                    extracted.add(word.removePrefix("#"))
                }
            }
        }
        habits.forEach { habit ->
            val textToSearch = "${habit.name} ${habit.notes} ${habit.category}"
            val words = textToSearch.split(" ", "\n", ",", ";")
            words.forEach { word ->
                if (word.startsWith("#") && word.length > 1) {
                    extracted.add(word.removePrefix("#"))
                }
            }
        }
        (customTags + extracted).distinct()
    }

    if (selectedHabitForDetail != null) {
        HabitDetailScreen(
            habitId = selectedHabitForDetail!!.id,
            viewModel = viewModel,
            onBack = { selectedHabitForDetail = null },
            onEditHabit = { selectedHabitForDetail = null }
        )
        return
    }

    if (activeDetailTag != null) {
        HashtagDetailView(
            tag = activeDetailTag!!,
            viewModel = viewModel,
            onBack = { activeDetailTag = null },
            onHabitClick = { habit ->
                selectedHabitForDetail = habit
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hashtags (#)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Button(
                        onClick = { showAddTagDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandOrange.copy(alpha = 0.12f),
                            contentColor = BrandOrange
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = "Add Tag",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Tag", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                items(allTags) { tag ->
                    val tagWithHash = "#$tag"
                    val tagTasksCount = remember(tasks, tagWithHash) {
                        tasks.count { task ->
                            val textToSearch = "${task.title} ${task.description} ${task.notes}"
                            textToSearch.contains(tagWithHash, ignoreCase = true)
                        }
                    }
                    val tagHabitsCount = remember(habits, tagWithHash) {
                        habits.count { habit ->
                            val textToSearch = "${habit.name} ${habit.notes} ${habit.category}"
                            textToSearch.contains(tagWithHash, ignoreCase = true)
                        }
                    }
                    val isExpanded = expandedTagAssign == tag

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isExpanded) BrandOrange.copy(alpha = 0.3f) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Tag row header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeDetailTag = tag }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(BrandOrange.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalOffer,
                                            contentDescription = null,
                                            tint = BrandOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = tagWithHash,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "$tagTasksCount tasks · $tagHabitsCount habits",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Assign tasks button
                                    Button(
                                        onClick = {
                                            expandedTagAssign = if (isExpanded) null else tag
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isExpanded) BrandOrange else BrandOrange.copy(alpha = 0.12f),
                                            contentColor = if (isExpanded) Color.White else BrandOrange
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.Check else Icons.Default.Assignment,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Assign", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Open",
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                    )
                                }
                            }

                            // Expanded Assign Tasks & Habits List
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                                        .padding(12.dp)
                                ) {
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 8.dp))
                                    Text(
                                        text = "Toggle hashtag '$tagWithHash' for tasks:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandOrange,
                                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                                    )

                                    if (tasks.isEmpty()) {
                                        Text(
                                            text = "No tasks available to tag. Add tasks first!",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    } else {
                                        tasks.forEach { task ->
                                            val isAssigned = "${task.title} ${task.description} ${task.notes}".contains(tagWithHash, ignoreCase = true)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        toggleTagForTask(viewModel, task, tagWithHash, !isAssigned)
                                                    }
                                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Task,
                                                        contentDescription = null,
                                                        tint = if (isAssigned) BrandOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = task.title,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isAssigned) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isAssigned) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                                    )
                                                }

                                                Checkbox(
                                                    checked = isAssigned,
                                                    onCheckedChange = { checked ->
                                                        toggleTagForTask(viewModel, task, tagWithHash, checked == true)
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = BrandOrange)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 8.dp))
                                    Text(
                                        text = "Toggle hashtag '$tagWithHash' for habits:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandOrange,
                                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                                    )

                                    if (habits.isEmpty()) {
                                        Text(
                                            text = "No habits available to tag. Add habits first!",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    } else {
                                        habits.forEach { habit ->
                                            val isAssigned = "${habit.name} ${habit.notes} ${habit.category}".contains(tagWithHash, ignoreCase = true)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        toggleTagForHabit(viewModel, habit, tagWithHash, !isAssigned)
                                                    }
                                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(text = habit.icon.ifBlank { "⭐" }, fontSize = 16.sp)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = habit.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isAssigned) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isAssigned) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                                    )
                                                }

                                                Checkbox(
                                                    checked = isAssigned,
                                                    onCheckedChange = { checked ->
                                                        toggleTagForHabit(viewModel, habit, tagWithHash, checked == true)
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = BrandOrange)
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

        // Add Tag Dialog
        if (showAddTagDialog) {
            AlertDialog(
                onDismissRequest = { showAddTagDialog = false },
                title = {
                    Text(
                        text = "Create New Hashtag",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                text = {
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        placeholder = { Text("tagname", fontSize = 13.sp) },
                        prefix = { Text("#") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTagName.isNotBlank()) {
                                viewModel.addCustomTag(newTagName)
                                newTagName = ""
                                showAddTagDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Text("Add", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTagDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
            )
        }
    }
}

// Helper to append/remove hashtag safely from the description field
private fun toggleTagForTask(viewModel: TrackWiseViewModel, task: TaskEntity, tagWithHash: String, enable: Boolean) {
    val currentDesc = task.description
    val newDesc = if (enable) {
        if (!currentDesc.contains(tagWithHash, ignoreCase = true)) {
            if (currentDesc.isBlank()) tagWithHash else "$currentDesc $tagWithHash"
        } else {
            currentDesc
        }
    } else {
        // Remove tag safely using exact match word boundary or regex
        currentDesc.replace(Regex("(?i)\\s*${Regex.escape(tagWithHash)}"), "").trim()
    }
    viewModel.updateTask(task.copy(description = newDesc))
}

private fun toggleTagForHabit(viewModel: TrackWiseViewModel, habit: HabitEntity, tagWithHash: String, enable: Boolean) {
    val currentNotes = habit.notes
    val newNotes = if (enable) {
        if (!currentNotes.contains(tagWithHash, ignoreCase = true)) {
            if (currentNotes.isBlank()) tagWithHash else "$currentNotes $tagWithHash"
        } else {
            currentNotes
        }
    } else {
        currentNotes.replace(Regex("(?i)\\s*${Regex.escape(tagWithHash)}"), "").trim()
    }
    viewModel.updateHabit(habit.copy(notes = newNotes))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashtagDetailView(
    tag: String,
    viewModel: TrackWiseViewModel,
    onBack: () -> Unit,
    onHabitClick: (com.example.data.HabitEntity) -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    val habits by viewModel.allHabits.collectAsState()

    val tagWithHash = "#$tag"

    val tagTasks = remember(tasks, tag) {
        tasks.filter { task ->
            "${task.title} ${task.description} ${task.notes}".contains(tagWithHash, ignoreCase = true)
        }
    }
    val tagHabits = remember(habits, tag) {
        habits.filter { habit ->
            "${habit.name} ${habit.notes} ${habit.category}".contains(tagWithHash, ignoreCase = true)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Tasks, 1: Habits

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = tagWithHash,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${tagTasks.size} tasks · ${tagHabits.size} habits",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // SEPARATE TABS FOR TASKS AND HABITS
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BrandViolet
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Tasks (${tagTasks.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Habits (${tagHabits.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTab == 0) {
                // TASKS TAB CONTENT
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (tagTasks.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Task, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No tasks tagged with $tagWithHash yet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    Text("Assign tasks to this tag using the Assign button!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                                }
                            }
                        }
                    } else {
                        items(tagTasks) { task ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = task.completed,
                                            onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                                            colors = CheckboxDefaults.colors(checkedColor = BrandViolet)
                                        )
                                        Column {
                                            Text(
                                                text = task.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (task.completed) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground
                                            )
                                            if (task.priority.isNotBlank()) {
                                                Text(
                                                    text = "Priority: ${task.priority.uppercase()}",
                                                    fontSize = 11.sp,
                                                    color = BrandOrange,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BrandRose, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // HABITS TAB CONTENT
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (tagHabits.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No habits tagged with $tagWithHash yet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    Text("Assign habits to this tag using the Assign button!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                                }
                            }
                        }
                    } else {
                        items(tagHabits) { habit ->
                            val todayStr = TrackWiseUtils.getTodayString()
                            val daysCompleted = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson)
                            val isCompletedToday = daysCompleted.contains(todayStr)

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                    .clickable { onHabitClick(habit) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (isCompletedToday) BrandOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { viewModel.toggleHabitToday(habit) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isCompletedToday) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = BrandOrange)
                                            } else {
                                                Text(text = habit.icon.ifBlank { "⭐" }, fontSize = 18.sp)
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = habit.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = "Streak: ${habit.streak}d · ${habit.category}",
                                                fontSize = 12.sp,
                                                color = BrandOrange,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Open detail",
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailView(
    folder: String,
    viewModel: TrackWiseViewModel,
    onBack: () -> Unit,
    onHabitClick: (com.example.data.HabitEntity) -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    val habits by viewModel.allHabits.collectAsState()

    val folderTasks = remember(tasks, folder) {
        tasks.filter { it.project.equals(folder, ignoreCase = true) }
    }
    val folderHabits = remember(habits, folder) {
        habits.filter { habit ->
            habit.section.split(",").map { it.trim().lowercase() }.contains(folder.lowercase())
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Tasks, 1: Habits
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = folder,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${folderTasks.size} tasks · ${folderHabits.size} habits",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // SEPARATE TABS FOR TASKS AND HABITS
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BrandViolet
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Tasks (${folderTasks.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Habits (${folderHabits.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTab == 0) {
                // TASKS TAB CONTENT
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Button(
                            onClick = { showAddTaskDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Task to $folder", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (folderTasks.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Task, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No tasks in this folder yet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    Text("Tap the button above to create one!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                                }
                            }
                        }
                    } else {
                        items(folderTasks) { task ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = task.completed,
                                            onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                                            colors = CheckboxDefaults.colors(checkedColor = BrandViolet)
                                        )
                                        Column {
                                            Text(
                                                text = task.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (task.completed) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground
                                            )
                                            if (task.priority.isNotBlank()) {
                                                Text(
                                                    text = "Priority: ${task.priority.uppercase()}",
                                                    fontSize = 11.sp,
                                                    color = BrandOrange,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BrandRose, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // HABITS TAB CONTENT
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (folderHabits.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No habits in this folder yet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    Text("Assign habits to this folder using the Assign button!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                                }
                            }
                        }
                    } else {
                        items(folderHabits) { habit ->
                            val todayStr = TrackWiseUtils.getTodayString()
                            val daysCompleted = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson)
                            val isCompletedToday = daysCompleted.contains(todayStr)

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                    .clickable { onHabitClick(habit) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (isCompletedToday) BrandOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { viewModel.toggleHabitToday(habit) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isCompletedToday) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = BrandOrange)
                                            } else {
                                                Text(text = habit.icon.ifBlank { "⭐" }, fontSize = 18.sp)
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = habit.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = "Streak: ${habit.streak}d · ${habit.category}",
                                                fontSize = 12.sp,
                                                color = BrandOrange,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Open detail",
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Add Task to $folder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("Task Title *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            viewModel.addTask(
                                title = newTaskTitle,
                                description = "",
                                project = folder,
                                priority = "medium",
                                points = 30,
                                deadline = TrackWiseUtils.getTodayString(),
                                reminderTime = null
                            )
                            newTaskTitle = ""
                            showAddTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                ) {
                    Text("Add Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
