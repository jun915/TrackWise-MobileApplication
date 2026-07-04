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

// Data representation for standard archived items
data class ArchivedItem(
    val id: String,
    val title: String,
    val category: String, // "Tasks", "Groceries", "Exercise", "Wishlist", "Health Issues"
    val dateCompleted: String, // YYYY-MM-DD or standard display date
    val details: String = ""
)

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

    // Process completed categories
    val completedTasks = remember(allTasks) {
        allTasks.filter { it.completed }.map {
            ArchivedItem(
                id = it.id,
                title = it.title,
                category = "Tasks",
                dateCompleted = it.deadline,
                details = it.priority.uppercase() + " Priority"
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
                details = "Qty: ${it.quantity}"
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
                details = "${it.durationMinutes} minutes logged"
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
                details = "₹${String.format("%,.2f", it.price)}"
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
                details = "Severity: ${it.severity.uppercase()}"
            )
        }
    }

    // Combine all completed items
    val allArchivedItems = remember(
        completedTasks,
        completedGroceries,
        completedExercises,
        completedWishlist,
        resolvedHealthIssues
    ) {
        val list = mutableListOf<ArchivedItem>()
        list.addAll(completedTasks)
        list.addAll(completedGroceries)
        list.addAll(completedExercises)
        list.addAll(completedWishlist)
        list.addAll(resolvedHealthIssues)
        // Sort items by date descending (safely falls back to title if dates are same)
        list.sortedWith(compareByDescending<ArchivedItem> { it.dateCompleted }.thenBy { it.title })
    }

    // Filter and search state
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val filteredItems = remember(allArchivedItems, searchQuery, selectedCategoryFilter) {
        allArchivedItems.filter { item ->
            val matchesCategory = selectedCategoryFilter == "All" || item.category == selectedCategoryFilter
            val matchesSearch = item.title.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
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
                    ArchivedItemRow(item = item)
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

@Composable
fun ArchivedItemRow(
    item: ArchivedItem,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (item.category) {
        "Tasks" -> BrandViolet
        "Groceries" -> BrandGreen
        "Exercise" -> BrandPink
        "Wishlist" -> BrandAmber
        "Health Issues" -> BrandRose
        else -> BrandViolet
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Checkmark Circle Indicator
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

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
