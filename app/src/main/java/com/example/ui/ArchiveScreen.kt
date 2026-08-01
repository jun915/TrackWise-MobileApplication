package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.example.data.*
import com.example.utils.TrackWiseUtils
import java.util.Calendar

// Data representation for standard archived items
data class ArchivedItem(
    val id: String,
    val title: String,
    val category: String, // "Tasks", "Groceries", "Exercise", "Wishlist", "Health Issues"
    val dateCompleted: String, // YYYY-MM-DD or standard display date
    val details: String = "",
    val originalEntity: Any? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchiveScreen(
    viewModel: TrackWiseViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect States from the ViewModel
    val allTasks by viewModel.allTasks.collectAsState()
    val allWishlist by viewModel.allWishlist.collectAsState()
    val allGroceryItems by viewModel.allGroceryItems.collectAsState()
    val exerciseLogs by viewModel.exerciseLogs.collectAsState()
    val healthIssueLogs by viewModel.healthIssueLogs.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()
    val allBirthdays by viewModel.allBirthdays.collectAsState()

    var pinnedIds by remember { mutableStateOf(setOf<String>()) }
    var selectedItemIds by remember { mutableStateOf(setOf<String>()) }
    var show3DotsMenu by remember { mutableStateOf(false) }
    val isSelectionMode = selectedItemIds.isNotEmpty()

    // Process completed categories
    val completedTasks = remember(allTasks) {
        allTasks.filter { it.completed }.map {
            ArchivedItem(
                id = it.id,
                title = it.title,
                category = "Tasks",
                dateCompleted = it.deadline,
                details = it.priority.uppercase() + " Priority",
                originalEntity = it
            )
        }
    }

    val completedGroceries = remember(allGroceryItems) {
        allGroceryItems.filter { it.completed }.map {
            ArchivedItem(
                id = it.id,
                title = it.name,
                category = "Groceries",
                dateCompleted = "Today", // standard mock/recent date or generic label
                details = "Qty: ${it.quantity}",
                originalEntity = it
            )
        }
    }

    val completedExercises = remember(exerciseLogs) {
        exerciseLogs.filter { it.completed }.map {
            ArchivedItem(
                id = it.id,
                title = it.exerciseType,
                category = "Exercise",
                dateCompleted = it.date,
                details = "${it.durationMinutes} minutes logged",
                originalEntity = it
            )
        }
    }

    val completedWishlist = remember(allWishlist) {
        allWishlist.filter { it.purchased }.map {
            ArchivedItem(
                id = it.id,
                title = it.title,
                category = "Wishlist",
                dateCompleted = "Purchased",
                details = "₹${String.format("%,.2f", it.price)}",
                originalEntity = it
            )
        }
    }

    val resolvedHealthIssues = remember(healthIssueLogs) {
        healthIssueLogs.filter { it.resolved }.map {
            ArchivedItem(
                id = it.id,
                title = it.issueName,
                category = "Health Issues",
                dateCompleted = it.date,
                details = "Severity: ${it.severity.uppercase()}",
                originalEntity = it
            )
        }
    }

    val completedHabits = remember(allHabits) {
        allHabits.filter { habit ->
            val daysCompleted = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson)
            daysCompleted.isNotEmpty() || habit.streak > 0
        }.map {
            ArchivedItem(
                id = it.id,
                title = it.name,
                category = "Habits",
                dateCompleted = "Streak: ${it.streak} days",
                details = "${it.category} habit",
                originalEntity = it
            )
        }
    }

    val completedCountdowns = remember(allBirthdays) {
        allBirthdays.filter { birthday ->
            birthday.category.contains("Countdown", ignoreCase = true) || TrackWiseUtils.getDaysUntil(birthday.date) <= 0
        }.map {
            ArchivedItem(
                id = it.id,
                title = it.name,
                category = "Countdowns",
                dateCompleted = it.date,
                details = it.category,
                originalEntity = it
            )
        }
    }

    // Combine all completed items
    val allArchivedItems = remember(
        completedTasks,
        completedGroceries,
        completedExercises,
        completedWishlist,
        resolvedHealthIssues,
        completedHabits,
        completedCountdowns
    ) {
        val list = mutableListOf<ArchivedItem>()
        list.addAll(completedTasks)
        list.addAll(completedGroceries)
        list.addAll(completedExercises)
        list.addAll(completedWishlist)
        list.addAll(resolvedHealthIssues)
        list.addAll(completedHabits)
        list.addAll(completedCountdowns)
        // Sort items by date descending (safely falls back to title if dates are same)
        list.sortedWith(compareByDescending<ArchivedItem> { it.dateCompleted }.thenBy { it.title })
    }

    // Filter and search state
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val filteredItems = remember(allArchivedItems, searchQuery, selectedCategoryFilter, pinnedIds) {
        allArchivedItems
            .filter { item ->
                val matchesCategory = selectedCategoryFilter == "All" || item.category == selectedCategoryFilter
                val matchesSearch = item.title.contains(searchQuery, ignoreCase = true) ||
                        item.category.contains(searchQuery, ignoreCase = true)
                matchesCategory && matchesSearch
            }
            .sortedWith(compareByDescending<ArchivedItem> { pinnedIds.contains(it.id) }.thenByDescending { it.dateCompleted })
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandViolet.copy(alpha = 0.12f))
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { selectedItemIds = emptySet() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel selection",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${selectedItemIds.size} Selected",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Select All / Deselect All Toggle Button
                            val allFilteredIds = remember(filteredItems) { filteredItems.map { it.id }.toSet() }
                            val isAllSelected = selectedItemIds.containsAll(allFilteredIds) && allFilteredIds.isNotEmpty()

                            TextButton(
                                onClick = {
                                    selectedItemIds = if (isAllSelected) emptySet() else allFilteredIds
                                }
                            ) {
                                Icon(
                                    imageVector = if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                    contentDescription = if (isAllSelected) "Deselect All" else "Select All",
                                    tint = BrandViolet,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAllSelected) "Deselect All" else "Select All",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandViolet
                                )
                            }

                            // 3-Dots Menu
                            Box {
                                IconButton(onClick = { show3DotsMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More options",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                DropdownMenu(
                                    expanded = show3DotsMenu,
                                    onDismissRequest = { show3DotsMenu = false }
                                ) {
                                    val isSingleSelected = selectedItemIds.size == 1
                                    val singleId = if (isSingleSelected) selectedItemIds.first() else null
                                    val isSinglePinned = singleId != null && pinnedIds.contains(singleId)

                                    if (isSingleSelected) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (isSinglePinned) "Unpin from Top" else "Pin to Top",
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.PushPin,
                                                    contentDescription = null,
                                                    tint = BrandAmber
                                                )
                                            },
                                            onClick = {
                                                singleId?.let { id ->
                                                    pinnedIds = if (isSinglePinned) pinnedIds - id else pinnedIds + id
                                                }
                                                selectedItemIds = emptySet()
                                                show3DotsMenu = false
                                            }
                                        )
                                    }

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Mark Incomplete (${selectedItemIds.size})",
                                                fontWeight = FontWeight.SemiBold,
                                                color = BrandViolet
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Undo,
                                                contentDescription = null,
                                                tint = BrandViolet
                                            )
                                        },
                                        onClick = {
                                            val targets = allArchivedItems.filter { selectedItemIds.contains(it.id) }
                                            targets.forEach { item ->
                                                when (item.category) {
                                                    "Tasks" -> (item.originalEntity as? TaskEntity)?.let { viewModel.updateTask(it.copy(completed = false)) }
                                                    "Groceries" -> (item.originalEntity as? GroceryItemEntity)?.let { viewModel.updateGroceryItem(it.copy(completed = false)) }
                                                    "Wishlist" -> (item.originalEntity as? WishItemEntity)?.let { viewModel.updateWishItem(it.copy(purchased = false)) }
                                                    "Exercise" -> (item.originalEntity as? ExerciseLogEntity)?.let { viewModel.updateExerciseLog(it.copy(completed = false)) }
                                                    "Health Issues" -> (item.originalEntity as? HealthIssueLogEntity)?.let { viewModel.updateHealthIssueLog(it.copy(resolved = false)) }
                                                    "Habits" -> (item.originalEntity as? HabitEntity)?.let { viewModel.updateHabit(it.copy(daysCompletedJson = "[]", streak = 0)) }
                                                    "Countdowns" -> (item.originalEntity as? BirthdayEntity)?.let { birthday ->
                                                        val parts = birthday.date.split("-")
                                                        val nextDate = if (parts.size == 3) {
                                                            val year = parts[0].toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                                                            "${year + 1}-${parts[1]}-${parts[2]}"
                                                        } else {
                                                            val nextYear = Calendar.getInstance().get(Calendar.YEAR) + 1
                                                            "$nextYear-${birthday.date}"
                                                        }
                                                        viewModel.updateBirthday(birthday.copy(date = nextDate))
                                                    }
                                                }
                                            }
                                            selectedItemIds = emptySet()
                                            show3DotsMenu = false
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Delete Permanently (${selectedItemIds.size})",
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            val targets = allArchivedItems.filter { selectedItemIds.contains(it.id) }
                                            targets.forEach { item ->
                                                when (item.category) {
                                                    "Tasks" -> viewModel.deleteTask(item.id)
                                                    "Groceries" -> viewModel.deleteGroceryItem(item.id)
                                                    "Wishlist" -> viewModel.deleteWishItem(item.id)
                                                    "Exercise" -> viewModel.deleteExerciseLog(item.id)
                                                    "Health Issues" -> viewModel.deleteHealthIssueLog(item.id)
                                                    "Habits" -> viewModel.deleteHabit(item.id)
                                                    "Countdowns" -> viewModel.deleteBirthday(item.id)
                                                }
                                            }
                                            pinnedIds = pinnedIds - selectedItemIds
                                            selectedItemIds = emptySet()
                                            show3DotsMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Go back",
                                tint = BrandViolet
                            )
                        }
                        Column {
                            Text(
                                text = "HISTORY LOGS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandViolet,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Completed Archive",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Category Stats Cards Title
            item {
                Text(
                    text = "TOTAL ARCHIVED ITEMS BY CATEGORY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Grid / Row of totals by category
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        ArchiveCategoryCard(
                            name = "Tasks",
                            count = completedTasks.size,
                            color = BrandViolet,
                            icon = Icons.Default.Task,
                            isSelected = selectedCategoryFilter == "Tasks",
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == "Tasks") "All" else "Tasks"
                            }
                        )
                    }
                    item {
                        ArchiveCategoryCard(
                            name = "Groceries",
                            count = completedGroceries.size,
                            color = BrandGreen,
                            icon = Icons.Default.ShoppingCart,
                            isSelected = selectedCategoryFilter == "Groceries",
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == "Groceries") "All" else "Groceries"
                            }
                        )
                    }
                    item {
                        ArchiveCategoryCard(
                            name = "Exercise",
                            count = completedExercises.size,
                            color = BrandPink,
                            icon = Icons.Default.DirectionsRun,
                            isSelected = selectedCategoryFilter == "Exercise",
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == "Exercise") "All" else "Exercise"
                            }
                        )
                    }
                    item {
                        ArchiveCategoryCard(
                            name = "Wishlist",
                            count = completedWishlist.size,
                            color = BrandAmber,
                            icon = Icons.Default.CardMembership,
                            isSelected = selectedCategoryFilter == "Wishlist",
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == "Wishlist") "All" else "Wishlist"
                            }
                        )
                    }
                    item {
                        ArchiveCategoryCard(
                            name = "Health Issues",
                            count = resolvedHealthIssues.size,
                            color = BrandRose,
                            icon = Icons.Default.Healing,
                            isSelected = selectedCategoryFilter == "Health Issues",
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == "Health Issues") "All" else "Health Issues"
                            }
                        )
                    }
                    item {
                        ArchiveCategoryCard(
                            name = "Habits",
                            count = completedHabits.size,
                            color = BrandViolet,
                            icon = Icons.Default.Autorenew,
                            isSelected = selectedCategoryFilter == "Habits",
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == "Habits") "All" else "Habits"
                            }
                        )
                    }
                    item {
                        ArchiveCategoryCard(
                            name = "Countdowns",
                            count = completedCountdowns.size,
                            color = BrandPink,
                            icon = Icons.Default.Timer,
                            isSelected = selectedCategoryFilter == "Countdowns",
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == "Countdowns") "All" else "Countdowns"
                            }
                        )
                    }
                }
            }

            // Search Bar & Filter Headers
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search completed items...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        } else null,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = BrandViolet,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Filter status banner
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ARCHIVE LIST (${filteredItems.size} items)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    if (selectedCategoryFilter != "All" || searchQuery.isNotEmpty()) {
                        Text(
                            text = "Clear Filters",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandViolet,
                            modifier = Modifier.clickable {
                                selectedCategoryFilter = "All"
                                searchQuery = ""
                            }
                        )
                    }
                }
            }

            // List of items
            if (filteredItems.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No items match your filters",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Any finished tasks, bought groceries, completed physical exercises, acquired wishlist goals, or resolved symptoms will populate automatically.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                items(filteredItems) { item ->
                    val isSelected = selectedItemIds.contains(item.id)
                    ArchivedItemRow(
                        item = item,
                        isPinned = pinnedIds.contains(item.id),
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                selectedItemIds = if (isSelected) selectedItemIds - item.id else selectedItemIds + item.id
                            }
                        },
                        onLongClick = {
                            selectedItemIds = if (isSelected) selectedItemIds - item.id else selectedItemIds + item.id
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ArchiveCategoryCard(
    name: String,
    count: Int,
    color: Color,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .width(130.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "$count",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = color
                )
            }
            Column {
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "archived",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchivedItemRow(
    item: ArchivedItem,
    isPinned: Boolean = false,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categoryColor = when (item.category) {
        "Tasks" -> BrandViolet
        "Groceries" -> BrandGreen
        "Exercise" -> BrandPink
        "Wishlist" -> BrandAmber
        "Health Issues" -> BrandRose
        "Habits" -> BrandViolet
        "Countdowns" -> BrandPink
        else -> BrandViolet
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> BrandViolet.copy(alpha = 0.15f)
                isPinned -> BrandAmber.copy(alpha = 0.08f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected || isPinned) 1.5.dp else 1.dp,
                color = when {
                    isSelected -> BrandViolet
                    isPinned -> BrandAmber.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Checkbox in selection mode or checkmark indicator
            if (isSelectionMode || isSelected) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = BrandViolet,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(categoryColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed icon",
                        tint = categoryColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = BrandAmber,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(categoryColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor
                        )
                    }
                    if (item.details.isNotEmpty()) {
                        Text(
                            text = "• ${item.details}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Date of Completion
            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = item.dateCompleted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
