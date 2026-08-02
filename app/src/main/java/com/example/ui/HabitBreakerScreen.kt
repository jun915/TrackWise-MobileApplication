package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class HabitBreakerView {
    LIST,
    GALLERY,
    CREATE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitBreakerScreen(
    viewModel: TrackWiseViewModel,
    onBack: () -> Unit = {}
) {
    val badHabits by viewModel.badHabits.collectAsState()
    var currentView by remember { mutableStateOf(HabitBreakerView.LIST) }
    var filterType by remember { mutableStateOf("All") }

    var prefilledName by remember { mutableStateOf("") }
    var prefilledType by remember { mutableStateOf("Habit") }
    var prefilledTag by remember { mutableStateOf("Health") }
    var prefilledPriority by remember { mutableStateOf("Medium") }

    when (currentView) {
        HabitBreakerView.LIST -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Avoid List",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = "Break habits, avoid triggers & stay clean",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("habit_breaker_back")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { currentView = HabitBreakerView.GALLERY },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(CircleShape)
                                    .background(BrandRose.copy(alpha = 0.15f))
                                    .testTag("habit_breaker_add_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Item to Avoid",
                                    tint = BrandRose
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { currentView = HabitBreakerView.GALLERY },
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                        text = { Text("Add Item to Avoid") },
                        containerColor = BrandRose,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("habit_breaker_fab")
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Category Filter Bar
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf("All", "Habit", "Person", "Event", "Place", "Trigger")
                        items(filters) { filter ->
                            val isSelected = filterType == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { filterType = filter },
                                label = { Text(filter) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandRose.copy(alpha = 0.2f),
                                    selectedLabelColor = BrandRose
                                ),
                                modifier = Modifier.testTag("filter_chip_$filter")
                            )
                        }
                    }

                    // Guidance Banner
                    Surface(
                        color = BrandRose.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Swipe, contentDescription = null, tint = BrandRose, modifier = Modifier.size(20.dp))
                            Text(
                                text = "👉 Swipe Right: Avoided (Keep Streak) | 👈 Swipe Left: Slipped (Reset Timer)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandRose
                            )
                        }
                    }

                    val filteredList = remember(badHabits, filterType) {
                        if (filterType == "All") badHabits
                        else badHabits.filter { it.avoidType.equals(filterType, ignoreCase = true) }
                    }

                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(BrandRose.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = null,
                                            tint = BrandRose,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    Text(
                                        text = if (filterType == "All") "No Items to Avoid Yet" else "No $filterType items found",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = "Add bad habits, triggers, people, or events you want to avoid to build discipline and self-control.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )

                                    Button(
                                        onClick = { currentView = HabitBreakerView.GALLERY },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandRose),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("empty_add_avoid_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Browse Gallery & Add")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                        ) {
                            items(filteredList, key = { it.id }) { item ->
                                SwipeableAvoidCard(
                                    item = item,
                                    onLogAvoidance = { viewModel.logBadHabitAvoidance(item.id) },
                                    onLogSlipUp = { viewModel.logBadHabitOccurrence(item.id) },
                                    onDelete = { viewModel.removeBadHabit(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        HabitBreakerView.GALLERY -> {
            FullPageGallery(
                onBack = { currentView = HabitBreakerView.LIST },
                onCreateCustomClick = { initialName, initialType ->
                    prefilledName = initialName
                    prefilledType = initialType
                    prefilledTag = "Health"
                    prefilledPriority = "Medium"
                    currentView = HabitBreakerView.CREATE
                },
                onAddTemplateDirectly = { templateName, type, tag, priority ->
                    viewModel.addBadHabit(
                        name = templateName,
                        avoidType = type,
                        tags = listOf(tag),
                        priority = priority
                    )
                    currentView = HabitBreakerView.LIST
                }
            )
        }

        HabitBreakerView.CREATE -> {
            FullPageAddAvoidItem(
                initialName = prefilledName,
                initialType = prefilledType,
                initialTag = prefilledTag,
                initialPriority = prefilledPriority,
                onBack = { currentView = HabitBreakerView.GALLERY },
                onSave = { name, avoidType, reminderTime, tags, priority, isRecurring, eventDate, costType, costValue, iconName ->
                    viewModel.addBadHabit(
                        name = name,
                        avoidType = avoidType,
                        reminderTime = reminderTime,
                        tags = tags,
                        priority = priority,
                        isRecurring = isRecurring,
                        eventDate = eventDate,
                        costType = costType,
                        costValue = costValue,
                        iconName = iconName
                    )
                    currentView = HabitBreakerView.LIST
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableAvoidCard(
    item: TrackWiseViewModel.BadHabitSpec,
    onLogAvoidance: () -> Unit,
    onLogSlipUp: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onLogAvoidance()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onLogSlipUp()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> BrandGreen
                SwipeToDismissBoxValue.EndToStart -> BrandRose
                else -> Color.Transparent
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.CheckCircle
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Replay
                else -> Icons.Default.Block
            }
            val text = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> "✨ Avoided! Streak Continues"
                SwipeToDismissBoxValue.EndToStart -> "⚠️ Slipped! Timer Resets"
                else -> ""
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (direction == SwipeToDismissBoxValue.StartToEnd) {
                        Icon(imageVector = icon, contentDescription = null, tint = Color.White)
                        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Icon(imageVector = icon, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    ) {
        AvoidItemCard(
            item = item,
            onLogAvoidance = onLogAvoidance,
            onLogSlipUp = onLogSlipUp,
            onDelete = onDelete
        )
    }
}

@Composable
fun AvoidItemCard(
    item: TrackWiseViewModel.BadHabitSpec,
    onLogAvoidance: () -> Unit,
    onLogSlipUp: () -> Unit,
    onDelete: () -> Unit
) {
    val cleanTimerText = remember(item.logs, item.id) {
        calculateCleanTimeText(item.logs, item.id)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("avoid_card_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val icon = getHabitBreakerIcon(item.iconName, item.avoidType)
                    val typeColor = when (item.avoidType.lowercase()) {
                        "person" -> BrandIndigo
                        "event" -> BrandOrange
                        "place" -> BrandGreen
                        "trigger" -> BrandViolet
                        else -> BrandRose
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(typeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.avoidType,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = typeColor
                            )
                            Text(text = "•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(
                                color = BrandGreen.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = BrandGreen,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = cleanTimerText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreen
                                    )
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Tags & Priority Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val prioColor = when (item.priority.lowercase()) {
                    "high" -> BrandRose
                    "low" -> BrandGreen
                    else -> BrandOrange
                }
                Surface(
                    color = prioColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${item.priority} Priority",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = prioColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                item.tags.forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (item.reminderTime.isNotBlank()) {
                    Surface(
                        color = BrandViolet.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = BrandViolet,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = item.reminderTime,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandViolet
                            )
                        }
                    }
                }
            }

            if (item.costValue.isNotBlank()) {
                val costPrefix = when (item.costType.lowercase()) {
                    "money" -> "💰"
                    "mood" -> "😊"
                    "health" -> "🛡️"
                    "time" -> "⏱️"
                    else -> "⚠️"
                }
                Text(
                    text = "$costPrefix Cost/Impact: ${item.costValue}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Action Buttons Row (Avoided vs Slipped)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val slipUpCount = item.logs.size
                Column {
                    Text(
                        text = if (slipUpCount == 0) "Pristine Record ✨" else "$slipUpCount Relapse${if (slipUpCount > 1) "s" else ""}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (slipUpCount == 0) BrandGreen else BrandOrange
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onLogAvoidance,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen.copy(alpha = 0.15f), contentColor = BrandGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Avoided! ✨", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onLogSlipUp,
                        border = BorderStroke(1.dp, BrandRose.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRose),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("log_slip_up_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Slipped ⚠️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPageGallery(
    onBack: () -> Unit,
    onCreateCustomClick: (initialName: String, initialType: String) -> Unit,
    onAddTemplateDirectly: (name: String, type: String, tag: String, priority: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val categories = listOf("Suggested", "Life", "Health", "Productivity", "Social", "Mindset")
    val pagerState = rememberPagerState(pageCount = { categories.size })

    val galleryData = mapOf(
        "Suggested" to listOf(
            BadHabitTemplate("Lying to others", "Habit", "Social", "High"),
            BadHabitTemplate("Procrastinating on urgent tasks", "Habit", "Productivity", "High"),
            BadHabitTemplate("Nail biting", "Habit", "Health", "Low"),
            BadHabitTemplate("Late night smartphone scrolling", "Habit", "Health", "Medium"),
            BadHabitTemplate("Impulse online shopping", "Habit", "Life", "Medium")
        ),
        "Life" to listOf(
            BadHabitTemplate("Consuming junk food late at night", "Habit", "Life", "Medium"),
            BadHabitTemplate("Skipping morning exercise", "Habit", "Life", "Low"),
            BadHabitTemplate("Disorganized workspace & mess", "Habit", "Life", "Low"),
            BadHabitTemplate("Toxic family argument triggers", "Event", "Life", "High"),
            BadHabitTemplate("Excessive caffeine consumption", "Habit", "Life", "Medium")
        ),
        "Health" to listOf(
            BadHabitTemplate("Smoking cigarettes / Vaping", "Habit", "Health", "High"),
            BadHabitTemplate("Excessive sugar intake", "Habit", "Health", "High"),
            BadHabitTemplate("Sleeping less than 7 hours", "Habit", "Health", "High"),
            BadHabitTemplate("Sitting continuously without breaks", "Habit", "Health", "Medium")
        ),
        "Productivity" to listOf(
            BadHabitTemplate("Multi-tasking on important tasks", "Habit", "Productivity", "Medium"),
            BadHabitTemplate("Checking email every 5 minutes", "Habit", "Productivity", "Medium"),
            BadHabitTemplate("Endless social media scrolling", "Habit", "Productivity", "High"),
            BadHabitTemplate("Hitting snooze on morning alarm", "Habit", "Productivity", "High")
        ),
        "Social" to listOf(
            BadHabitTemplate("Gossiping or backbiting", "Habit", "Social", "High"),
            BadHabitTemplate("Interrupting people while speaking", "Habit", "Social", "Medium"),
            BadHabitTemplate("Toxic friend gatherings", "Person", "Social", "High"),
            BadHabitTemplate("Over-promising and under-delivering", "Habit", "Social", "High")
        ),
        "Mindset" to listOf(
            BadHabitTemplate("Comparing myself to others", "Habit", "Mindset", "High"),
            BadHabitTemplate("Seeking external validation", "Habit", "Mindset", "Medium"),
            BadHabitTemplate("Overthinking decisions", "Habit", "Mindset", "Medium"),
            BadHabitTemplate("Self-critical inner monologue", "Habit", "Mindset", "High")
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gallery of Bad Habits", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Swipe left or right to explore trigger categories", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("gallery_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Create Custom Habit Breaker Button
            Button(
                onClick = { onCreateCustomClick("", "Habit") },
                colors = ButtonDefaults.buttonColors(containerColor = BrandRose),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .testTag("create_new_habit_breaker_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create a Custom Bad Habit Breaker", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Slidable Scrollable Tab Row
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 20.dp,
                divider = {},
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = BrandRose
                        )
                    }
                }
            ) {
                categories.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        },
                        selectedContentColor = BrandRose,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("gallery_tab_$title")
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Slidable Horizontal Pager across tabs
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                val category = categories[pageIndex]
                val items = galleryData[category] ?: emptyList()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items) { template ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAddTemplateDirectly(template.name, template.type, template.tag, template.priority)
                                }
                                .testTag("template_item_${template.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = BrandRose,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = template.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${template.type} • #${template.tag} • ${template.priority} Priority",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        onAddTemplateDirectly(template.name, template.type, template.tag, template.priority)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandRose.copy(alpha = 0.15f), contentColor = BrandRose),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("+ Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
fun FullPageAddAvoidItem(
    initialName: String = "",
    initialType: String = "Habit",
    initialTag: String = "Health",
    initialPriority: String = "Medium",
    onBack: () -> Unit,
    onSave: (
        name: String,
        avoidType: String,
        reminderTime: String,
        tags: List<String>,
        priority: String,
        isRecurring: Boolean,
        eventDate: String,
        costType: String,
        costValue: String,
        iconName: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var avoidType by remember { mutableStateOf(initialType) }
    var reminderTime by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf(initialTag) }
    var priority by remember { mutableStateOf(initialPriority) }
    var isRecurring by remember { mutableStateOf(true) }
    var eventDate by remember { mutableStateOf("") }
    var costType by remember { mutableStateOf("Money") }
    var costValue by remember { mutableStateOf("") }
    var iconName by remember { mutableStateOf("Block") }

    val avoidTypes = listOf("Habit", "Person", "Event", "Place", "Trigger")
    val tagsList = listOf("Health", "Productivity", "Social", "Digital", "Mindset", "Finance", "Relationship", "Fitness")
    val costTypes = listOf("Money", "Mood", "Health", "Time", "Focus")
    val iconsList = listOf(
        "Block" to "🚫 Block",
        "SmokeFree" to "🚭 Smoke Free",
        "NoFood" to "🍱 Food",
        "PhonelinkOff" to "📱 Phone",
        "Warning" to "⚠️ Warning",
        "PersonOff" to "👤 Person",
        "EventBusy" to "📅 Event",
        "Place" to "📍 Place",
        "LocalBar" to "🍸 Drink",
        "SentimentVeryDissatisfied" to "😔 Mood",
        "HourglassDisabled" to "⏳ Time",
        "Bedtime" to "🌙 Sleep",
        "AttachMoney" to "💰 Money",
        "Shield" to "🛡️ Shield",
        "AutoAwesome" to "✨ Sparkle"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Avoid Item", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("add_avoid_back")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("What do you want to avoid?") },
                placeholder = { Text("e.g. Lying, Smoking, Toxic Gatherings") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("avoid_item_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandRose,
                    focusedLabelColor = BrandRose
                )
            )

            // Slidable Avoid Type Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Avoid Type 🏷️", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(avoidTypes) { type ->
                        val isSelected = avoidType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { avoidType = type },
                            label = { Text(type) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandRose,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Slidable Icon Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Select Icon 🎨", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(iconsList) { (key, label) ->
                        val isSelected = iconName == key
                        Surface(
                            color = if (isSelected) BrandRose else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.clickable { iconName = key }
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Slidable Tags Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Category Tag 📌", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tagsList) { tag ->
                        val isSelected = selectedTag == tag
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTag = tag },
                            label = { Text("#$tag") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandViolet,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Priority Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Priority Level ⚡", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("High", "Medium", "Low").forEach { prio ->
                        val isSelected = priority == prio
                        val prioColor = when (prio) {
                            "High" -> BrandRose
                            "Low" -> BrandGreen
                            else -> BrandOrange
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) prioColor else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { priority = prio }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = prio,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Slidable Cost Type Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Impact / Cost Category 💔", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(costTypes) { cost ->
                        val isSelected = costType == cost
                        FilterChip(
                            selected = isSelected,
                            onClick = { costType = cost },
                            label = { Text(cost) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            OutlinedTextField(
                value = costValue,
                onValueChange = { costValue = it },
                label = { Text("Impact Description / Cost") },
                placeholder = { Text("e.g. $50 wasted, Loss of peace of mind, Ruined evening") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandRose,
                    focusedLabelColor = BrandRose
                )
            )

            TimePickerField(
                timeStr = reminderTime,
                label = "Reminder Alarm Time",
                onTimeSelected = { reminderTime = it },
                modifier = Modifier.fillMaxWidth(),
                tintColor = BrandRose
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name,
                            avoidType,
                            reminderTime,
                            listOf(selectedTag),
                            priority,
                            isRecurring,
                            eventDate,
                            costType,
                            costValue,
                            iconName
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandRose),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_avoid_item_btn")
            ) {
                Text("Save Avoid Item", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

data class BadHabitTemplate(
    val name: String,
    val type: String = "Habit",
    val tag: String = "Productivity",
    val priority: String = "Medium"
)

fun getHabitBreakerIcon(iconName: String, avoidType: String): ImageVector {
    return when (iconName.lowercase()) {
        "smokefree", "smoke" -> Icons.Default.SmokeFree
        "nofood", "food" -> Icons.Default.NoFood
        "phonelinkoff", "phone" -> Icons.Default.PhonelinkOff
        "warning" -> Icons.Default.Warning
        "personoff", "person" -> Icons.Default.PersonOff
        "eventbusy", "event" -> Icons.Default.EventBusy
        "place" -> Icons.Default.Place
        "localbar", "bar", "drink" -> Icons.Default.LocalBar
        "sentimentverydissatisfied", "mood" -> Icons.Default.SentimentVeryDissatisfied
        "hourglassdisabled", "time" -> Icons.Default.HourglassDisabled
        "bedtime", "sleep" -> Icons.Default.Bedtime
        "attachmoney", "money" -> Icons.Default.AttachMoney
        "shield" -> Icons.Default.Shield
        "autoawesome", "sparkle" -> Icons.Default.AutoAwesome
        "block" -> Icons.Default.Block
        else -> when (avoidType.lowercase()) {
            "person" -> Icons.Default.PersonOff
            "event" -> Icons.Default.EventBusy
            "place" -> Icons.Default.Place
            "trigger" -> Icons.Default.FlashOn
            else -> Icons.Default.Block
        }
    }
}

fun calculateCleanTimeText(logs: List<String>, id: String): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    val now = Date().time

    val lastSlipTimestamp = if (logs.isNotEmpty()) {
        val lastLog = logs.last()
        try {
            sdf.parse(lastLog)?.time ?: now
        } catch (e: Exception) {
            now
        }
    } else {
        val idTime = id.removePrefix("bad_habit_").toLongOrNull()
        idTime ?: (now - 86400000L)
    }

    val diffMs = maxOf(0L, now - lastSlipTimestamp)
    val seconds = diffMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    val remHours = hours % 24
    val remMins = minutes % 60

    return if (days > 0) {
        "${days}d ${remHours}h clean"
    } else if (hours > 0) {
        "${hours}h ${remMins}m clean"
    } else {
        "${remMins}m clean"
    }
}
