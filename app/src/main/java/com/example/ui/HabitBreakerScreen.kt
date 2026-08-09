package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
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

data class UndoSwipeData(
    val habitId: String,
    val habitName: String,
    val isAvoidance: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

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
    val viewState by viewModel.habitBreakerViewState.collectAsState()
    
    val currentView = when (viewState) {
        "gallery" -> HabitBreakerView.GALLERY
        "create" -> HabitBreakerView.CREATE
        else -> HabitBreakerView.LIST
    }

    var prefilledName by remember { mutableStateOf("") }
    var prefilledType by remember { mutableStateOf("Habit") }
    var prefilledTag by remember { mutableStateOf("Health") }
    var prefilledPriority by remember { mutableStateOf("Medium") }
    var prefilledReminderTime by remember { mutableStateOf("") }
    var prefilledIsRecurring by remember { mutableStateOf(true) }
    var prefilledEventDate by remember { mutableStateOf("") }
    var prefilledCostType by remember { mutableStateOf("Time") }
    var prefilledCostValue by remember { mutableStateOf("6.0") }
    var prefilledIconName by remember { mutableStateOf("Block") }
    var editingItemId by remember { mutableStateOf<String?>(null) }

    var selectedItemForOptions by remember { mutableStateOf<TrackWiseViewModel.BadHabitSpec?>(null) }
    var selectedItemForLogUpdate by remember { mutableStateOf<TrackWiseViewModel.BadHabitSpec?>(null) }
    var selectedItemForIconPicker by remember { mutableStateOf<TrackWiseViewModel.BadHabitSpec?>(null) }
    var undoSwipeData by remember { mutableStateOf<UndoSwipeData?>(null) }

    LaunchedEffect(undoSwipeData) {
        if (undoSwipeData != null) {
            kotlinx.coroutines.delay(5000)
            undoSwipeData = null
        }
    }

    if (currentView != HabitBreakerView.LIST) {
        androidx.activity.compose.BackHandler(enabled = true) {
            if (currentView == HabitBreakerView.CREATE) {
                editingItemId = null
                viewModel.setHabitBreakerViewState("gallery")
            } else if (currentView == HabitBreakerView.GALLERY) {
                viewModel.setHabitBreakerViewState("list")
            }
        }
    }

    val pinnedHabitBreakerIds by viewModel.pinnedHabitBreakerIds.collectAsState()

    when (currentView) {
        HabitBreakerView.LIST -> {
            var tick by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(1000)
                    tick++
                }
            }
            val sortedBadHabits = remember(badHabits, pinnedHabitBreakerIds, tick) {
                badHabits.sortedWith(
                    compareBy<TrackWiseViewModel.BadHabitSpec> { !pinnedHabitBreakerIds.contains(it.id) }
                        .thenBy { if (pinnedHabitBreakerIds.contains(it.id)) pinnedHabitBreakerIds.indexOf(it.id) else Int.MAX_VALUE }
                        .thenBy { calculateCleanTimerMs(it.logs, it.id) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (badHabits.isEmpty()) {
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
                                    text = "No Items to Avoid Yet",
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
                                    onClick = { viewModel.setHabitBreakerViewState("gallery") },
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
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
                    ) {
                        items(sortedBadHabits, key = { it.id }) { item ->
                            SwipeableAvoidCard(
                                item = item,
                                isPinned = pinnedHabitBreakerIds.contains(item.id),
                                onTogglePin = { viewModel.togglePinHabitBreaker(item.id) },
                                onLogAvoidance = {
                                    viewModel.logBadHabitAvoidance(item.id)
                                    viewModel.triggerSwipeVibration()
                                    undoSwipeData = UndoSwipeData(item.id, item.name, isAvoidance = true)
                                },
                                onLogSlipUp = {
                                    viewModel.logBadHabitOccurrence(item.id)
                                    viewModel.triggerSwipeVibration()
                                    undoSwipeData = UndoSwipeData(item.id, item.name, isAvoidance = false)
                                },
                                onDelete = { viewModel.removeBadHabit(item.id) },
                                onCardClick = { selectedItemForOptions = item },
                                tick = tick
                            )
                        }
                    }
                }

                // Floating 5-Second Undo Popup (Compact Pill)
                AnimatedVisibility(
                    visible = undoSwipeData != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    if (undoSwipeData != null) {
                        val state = undoSwipeData!!
                        Surface(
                            onClick = {
                                if (state.isAvoidance) {
                                    viewModel.undoBadHabitAvoidance(state.habitId)
                                } else {
                                    viewModel.undoBadHabitOccurrence(state.habitId)
                                }
                                viewModel.triggerSwipeVibration()
                                undoSwipeData = null
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Undo,
                                    contentDescription = "Undo",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "UNDO",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Sheets & Dialogs for Tile Interactions
            if (selectedItemForOptions != null) {
                val item = selectedItemForOptions!!
                val cleanTimerText = calculateCleanTimeText(item.logs, item.id)
                HabitBreakerOptionsBottomSheet(
                    item = item,
                    cleanTimerText = cleanTimerText,
                    onDismiss = { selectedItemForOptions = null },
                    onLogNow = { selectedItemForLogUpdate = item },
                    onEdit = {
                        editingItemId = item.id
                        prefilledName = item.name
                        prefilledType = item.avoidType
                        prefilledTag = item.tags.firstOrNull() ?: "Health"
                        prefilledPriority = item.priority
                        prefilledReminderTime = item.reminderTime
                        prefilledIsRecurring = item.isRecurring
                        prefilledEventDate = item.eventDate
                        prefilledCostType = item.costType
                        prefilledCostValue = item.costValue
                        prefilledIconName = item.iconName
                        viewModel.setHabitBreakerViewState("create")
                    },
                    onChangeIcon = { selectedItemForIconPicker = item },
                    onRemove = { viewModel.removeBadHabit(item.id) }
                )
            }

            if (selectedItemForLogUpdate != null) {
                val item = selectedItemForLogUpdate!!
                LogTodayUpdateBottomSheet(
                    item = item,
                    onDismiss = { selectedItemForLogUpdate = null },
                    onAvoided = { viewModel.logBadHabitAvoidance(item.id) },
                    onSlipped = { viewModel.logBadHabitOccurrence(item.id) }
                )
            }

            if (selectedItemForIconPicker != null) {
                val item = selectedItemForIconPicker!!
                IconPickerBottomSheet(
                    item = item,
                    onDismiss = { selectedItemForIconPicker = null },
                    onSelectIcon = { newIcon ->
                        viewModel.updateBadHabitIcon(item.id, newIcon)
                    }
                )
            }
        }

        HabitBreakerView.GALLERY -> {
            FullPageGallery(
                onBack = { viewModel.setHabitBreakerViewState("list") },
                onCreateCustomClick = { initialName, initialType ->
                    prefilledName = initialName
                    prefilledType = initialType
                    prefilledTag = "Health"
                    prefilledPriority = "Medium"
                    viewModel.setHabitBreakerViewState("create")
                },
                onAddTemplateDirectly = { templateName, type, tag, priority ->
                    viewModel.addBadHabit(
                        name = templateName,
                        avoidType = type,
                        tags = listOf(tag),
                        priority = priority
                    )
                    viewModel.setHabitBreakerViewState("list")
                }
            )
        }

        HabitBreakerView.CREATE -> {
            FullPageAddAvoidItem(
                initialName = prefilledName,
                initialType = prefilledType,
                initialTag = prefilledTag,
                initialPriority = prefilledPriority,
                initialReminderTime = prefilledReminderTime,
                initialIsRecurring = prefilledIsRecurring,
                initialEventDate = prefilledEventDate,
                initialCostType = prefilledCostType,
                initialCostValue = prefilledCostValue,
                initialIconName = prefilledIconName,
                onBack = {
                    editingItemId = null
                    viewModel.setHabitBreakerViewState("gallery")
                },
                onSave = { name, avoidType, reminderTime, tags, priority, isRecurring, eventDate, costType, costValue, iconName ->
                    if (editingItemId != null) {
                        viewModel.updateBadHabit(
                            id = editingItemId!!,
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
                    } else {
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
                    }
                    editingItemId = null
                    viewModel.setHabitBreakerViewState("list")
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SwipeableAvoidCard(
    item: TrackWiseViewModel.BadHabitSpec,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {},
    onLogAvoidance: () -> Unit,
    onLogSlipUp: () -> Unit,
    onDelete: () -> Unit,
    onCardClick: () -> Unit,
    tick: Int
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.7f },
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
                    .clip(RoundedCornerShape(12.dp))
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
            isPinned = isPinned,
            onTogglePin = onTogglePin,
            onLogAvoidance = onLogAvoidance,
            onLogSlipUp = onLogSlipUp,
            onDelete = onDelete,
            onCardClick = onCardClick,
            tick = tick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AvoidItemCard(
    item: TrackWiseViewModel.BadHabitSpec,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {},
    onLogAvoidance: () -> Unit,
    onLogSlipUp: () -> Unit,
    onDelete: () -> Unit,
    onCardClick: () -> Unit,
    tick: Int
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLongPressMenu by remember { mutableStateOf(false) }
    val cleanTimerText = remember(item.logs, item.id, tick) {
        calculateCleanTimeText(item.logs, item.id)
    }

    if (showLongPressMenu) {
        AlertDialog(
            onDismissRequest = { showLongPressMenu = false },
            title = { Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onTogglePin()
                            showLongPressMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null, tint = BrandAmber)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isPinned) "Unpin from Top" else "Pin to Top", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            showLongPressMenu = false
                            showDeleteConfirm = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Habit Breaker", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLongPressMenu = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Habit Breaker?") },
            text = { Text("Are you sure you want to permanently delete '${item.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = BrandRose)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = { showLongPressMenu = true },
                onClick = onCardClick
            )
            .testTag("avoid_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val typeColor = when (item.avoidType.lowercase()) {
                "person" -> BrandIndigo
                "event" -> BrandOrange
                "place" -> BrandGreen
                "trigger" -> BrandViolet
                else -> BrandRose
            }

            // Left Icon Box
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                HabitBreakerIconView(
                    icon = item.iconName,
                    avoidType = item.avoidType,
                    tint = typeColor,
                    size = 18.dp
                )
            }

            // Middle Column: Title + Row under title (Flag, Hashtag, Cost Score)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = BrandAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Row beneath title (starts below title, not icon)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Priority Flag (Red = High, Orange = Medium, Blue = Low)
                    val flagTint = when (item.priority.lowercase()) {
                        "high" -> Color(0xFFEF4444) // Red flag
                        "low" -> Color(0xFF3B82F6)  // Blue flag
                        else -> Color(0xFFFF9800)   // Orange flag
                    }
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "${item.priority} priority flag",
                        tint = flagTint,
                        modifier = Modifier.size(15.dp)
                    )

                    // Hashtag / Tags
                    item.tags.forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Cost Score beside Hashtag: reduced in multiples of entered cost Value
                    val enteredCost = item.costValue.toDoubleOrNull() ?: 0.0
                    val totalCostImpact = enteredCost * item.logs.size
                    val formattedImpact = if (totalCostImpact % 1.0 == 0.0) {
                        totalCostImpact.toLong().toString()
                    } else {
                        String.format(java.util.Locale.US, "%.1f", totalCostImpact)
                    }

                    val (costEmoji, costLabel) = when (item.costType.lowercase()) {
                        "money" -> Pair("💰", "")
                        "mood" -> Pair("😊", "Mood")
                        "health" -> Pair("🛡️", "Health")
                        "time" -> Pair("⏰", "Time")
                        else -> Pair("💔", item.costType)
                    }

                    val displayCostText = if (costLabel.isEmpty()) {
                        "$costEmoji -$formattedImpact"
                    } else {
                        "$costEmoji $costLabel -$formattedImpact"
                    }

                    Surface(
                        color = BrandRose.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = displayCostText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandRose,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Right side: Clean timer & Avoided/Slipped stats
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = cleanTimerText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = BrandGreen
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Avoided count",
                            tint = BrandAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${item.avoidCount}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Slipped count",
                            tint = BrandRose,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${item.logs.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

fun getGalleryTemplateIcon(templateName: String): ImageVector {
    val lower = templateName.lowercase()
    return when {
        lower.contains("lying") || lower.contains("gossip") || lower.contains("argument") -> Icons.Default.RecordVoiceOver
        lower.contains("procrastinat") || lower.contains("snooze") || lower.contains("alarm") -> Icons.Default.HourglassDisabled
        lower.contains("nail") || lower.contains("junk food") || lower.contains("sugar") -> Icons.Default.NoFood
        lower.contains("phone") || lower.contains("social media") || lower.contains("email") || lower.contains("scroll") -> Icons.Default.PhonelinkOff
        lower.contains("shopping") || lower.contains("buy") -> Icons.Default.ShoppingCart
        lower.contains("exercise") || lower.contains("sitting") -> Icons.Default.FitnessCenter
        lower.contains("workspace") || lower.contains("mess") -> Icons.Default.CleaningServices
        lower.contains("caffeine") || lower.contains("coffee") -> Icons.Default.LocalCafe
        lower.contains("smok") || lower.contains("vap") -> Icons.Default.SmokeFree
        lower.contains("sleep") -> Icons.Default.Bedtime
        lower.contains("interrupt") -> Icons.Default.Forum
        lower.contains("toxic friend") -> Icons.Default.People
        lower.contains("compare") || lower.contains("validation") || lower.contains("overthink") || lower.contains("inner monologue") -> Icons.Default.Psychology
        else -> Icons.Default.Block
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
                        Text("Tap any tile to instantly add to your list", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        val templateIcon = getGalleryTemplateIcon(template.name)
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
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
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
                                            .background(BrandRose.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = templateIcon,
                                            contentDescription = null,
                                            tint = BrandRose,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
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
    initialReminderTime: String = "",
    initialIsRecurring: Boolean = true,
    initialEventDate: String = "",
    initialCostType: String = "Time",
    initialCostValue: String = "6.0",
    initialIconName: String = "Block",
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
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var avoidType by remember { mutableStateOf(initialType) }
    var reminderTime by remember { mutableStateOf(initialReminderTime) }
    var selectedTag by remember { mutableStateOf(initialTag) }
    var priority by remember { mutableStateOf(initialPriority) }
    var isRecurring by remember { mutableStateOf(initialIsRecurring) }
    var eventDate by remember { mutableStateOf(initialEventDate) }
    var costType by remember { mutableStateOf(initialCostType) }
    var costValue by remember { mutableStateOf(initialCostValue) }
    var iconName by remember { mutableStateOf(initialIconName) }
    var isAdvancedExpanded by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }

    val avoidTypes = listOf("Habit", "Person", "Event", "Place", "Trigger")
    val tagsList = listOf("Health", "Productivity", "Social", "Digital", "Mindset", "Finance", "Relationship", "Fitness")
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

    if (showDatePicker) {
        val cal = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            context,
            { _, y, m, d ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(y, m, d)
                val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                eventDate = sdf.format(selectedCal.time)
                showDatePicker = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.setOnDismissListener { showDatePicker = false }
        DisposableEffect(Unit) {
            datePickerDialog.show()
            onDispose {
                if (datePickerDialog.isShowing) {
                    datePickerDialog.dismiss()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Avoid Item", fontWeight = FontWeight.Bold) },
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
                FlatColorIconPicker(
                    selectedIcon = iconName,
                    onIconSelected = { iconName = it },
                    accentColor = BrandRose,
                    searchQuery = name,
                    modifier = Modifier.fillMaxWidth()
                )
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

            // Is Recurring Switch (Screenshot 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Is this a recurring habit?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // Event Date Row (only visible if isRecurring is false)
            if (!isRecurring) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Event Date:",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.testTag("select_event_date_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = BrandViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (eventDate.isBlank()) "Select Date" else eventDate,
                            color = BrandViolet,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Advanced Options Collapsible Header (Screenshot 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isAdvancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Advanced options",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isAdvancedExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Cost Type selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Cost Type:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val costOptions = listOf(
                            "Money" to Pair("Money", Icons.Default.AttachMoney),
                            "Mood" to Pair("Mood", Icons.Default.SentimentDissatisfied),
                            "Health" to Pair("Health", Icons.Default.Shield),
                            "Time" to Pair("Time", Icons.Default.Timer)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(costOptions) { (key, pair) ->
                                val (label, icon) = pair
                                val isSelected = costType.equals(key, ignoreCase = true)
                                val activeBg = if (isSelected) Color(0xFF00A3FF) else MaterialTheme.colorScheme.surface
                                val activeFg = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

                                Surface(
                                    onClick = { costType = key },
                                    shape = RoundedCornerShape(12.dp),
                                    color = activeBg,
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF00A3FF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier.height(42.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else BrandViolet,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = label,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = activeFg
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Subfield based on Cost Type
                    val fieldLabel = when (costType.lowercase()) {
                        "time" -> "Hours lost/relapse"
                        "mood" -> "Mood lost"
                        "money" -> "Amount lost/relapse ($)"
                        "health" -> "Health score impact"
                        else -> "Cost Value"
                    }
                    val fieldIcon = when (costType.lowercase()) {
                        "time" -> Icons.Default.Timer
                        "mood" -> Icons.Default.SentimentDissatisfied
                        "money" -> Icons.Default.AttachMoney
                        "health" -> Icons.Default.Shield
                        else -> Icons.Default.Warning
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = fieldLabel,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = costValue,
                            onValueChange = { costValue = it },
                            leadingIcon = {
                                Icon(
                                    imageVector = fieldIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            placeholder = { Text("6.0") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (costType.equals("mood", ignoreCase = true)) {
                            Text(
                                text = "Higher = more mood lost.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            TimePickerField(
                timeStr = reminderTime,
                label = "Reminder Alarm Time",
                onTimeSelected = { reminderTime = it },
                modifier = Modifier.fillMaxWidth(),
                tintColor = BrandRose
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cancel and Save Pills (Screenshot 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cancel_avoid_item_btn")
                ) {
                    Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

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
                                costValue.ifBlank { "6.0" },
                                iconName
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_avoid_item_btn")
                ) {
                    Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
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

@Composable
fun HabitBreakerIconView(
    icon: String,
    avoidType: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: androidx.compose.ui.unit.Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    val cleanKey = icon.trim()
    val isEmoji = cleanKey.any { it.code > 127 || it.isSurrogate() }
    if (isEmoji) {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cleanKey,
                fontSize = (size.value * 0.85f).sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        val vector = getHabitBreakerIcon(cleanKey, avoidType)
        Icon(
            imageVector = vector,
            contentDescription = null,
            tint = tint,
            modifier = modifier.size(size)
        )
    }
}

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

fun calculateCleanTimerMs(logs: List<String>, id: String): Long {
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
    return maxOf(0L, now - lastSlipTimestamp)
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
    val totalSeconds = diffMs / 1000

    // Limit to 5 years (5 * 365 days * 24 * 3600 seconds)
    val maxSeconds = 5L * 365 * 24 * 3600
    if (totalSeconds >= maxSeconds) {
        return "5y"
    }

    val secondsInYear = 365L * 24 * 3600
    val secondsInDay = 24L * 3600
    val secondsInHour = 3600L
    val secondsInMinute = 60L

    val years = totalSeconds / secondsInYear
    var remSeconds = totalSeconds % secondsInYear

    val days = remSeconds / secondsInDay
    remSeconds %= secondsInDay

    val hours = remSeconds / secondsInHour
    remSeconds %= secondsInHour

    val minutes = remSeconds / secondsInMinute
    val seconds = remSeconds % secondsInMinute

    return when {
        years > 0 -> {
            "${years}y ${days}d ${hours}h ${minutes}m"
        }
        days > 0 -> {
            "${days}d ${hours}h ${minutes}m"
        }
        hours > 0 -> {
            "${hours}h ${minutes}m"
        }
        minutes > 0 -> {
            "${minutes}m"
        }
        else -> {
            "${seconds}s"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitBreakerOptionsBottomSheet(
    item: TrackWiseViewModel.BadHabitSpec,
    cleanTimerText: String,
    onDismiss: () -> Unit,
    onLogNow: () -> Unit,
    onEdit: () -> Unit,
    onChangeIcon: () -> Unit,
    onRemove: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Habit Breaker?") },
            text = { Text("Are you sure you want to permanently delete '${item.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDismiss()
                        onRemove()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = BrandRose)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val typeColor = when (item.avoidType.lowercase()) {
                        "person" -> BrandIndigo
                        "event" -> BrandOrange
                        "place" -> BrandGreen
                        "trigger" -> BrandViolet
                        else -> BrandRose
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(typeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        HabitBreakerIconView(
                            icon = item.iconName,
                            avoidType = item.avoidType,
                            tint = typeColor,
                            size = 24.dp
                        )
                    }

                    Column {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Streak $cleanTimerText",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    OptionRowItem(
                        icon = Icons.Default.CheckCircle,
                        iconTint = BrandGreen,
                        title = "Log Now",
                        onClick = {
                            onDismiss()
                            onLogNow()
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    OptionRowItem(
                        icon = Icons.Default.Edit,
                        iconTint = MaterialTheme.colorScheme.onSurface,
                        title = "Edit",
                        onClick = {
                            onDismiss()
                            onEdit()
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    OptionRowItem(
                        icon = Icons.Default.Palette,
                        iconTint = MaterialTheme.colorScheme.onSurface,
                        title = "Icon",
                        onClick = {
                            onDismiss()
                            onChangeIcon()
                        }
                    )
                }
            }

            TextButton(
                onClick = {
                    showDeleteConfirm = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = BrandRose,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Remove",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandRose
                )
            }
        }
    }
}

@Composable
fun OptionRowItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (badge != null) {
            Surface(
                color = Color(0xFFFF9800),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogTodayUpdateBottomSheet(
    item: TrackWiseViewModel.BadHabitSpec,
    onDismiss: () -> Unit,
    onAvoided: () -> Unit,
    onSlipped: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Log today's update",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = item.name,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    onAvoided()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE8F8EE),
                    contentColor = BrandGreen
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Avoided",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    onSlipped()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFDE8EB),
                    contentColor = BrandRose
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = null,
                    tint = BrandRose,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Slipped",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISupportBottomSheet(
    item: TrackWiseViewModel.BadHabitSpec,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(BrandViolet.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(32.dp))
            }
            Text("AI Support for ${item.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💡 Resisting Urges:", fontWeight = FontWeight.Bold, color = BrandViolet)
                    Text("• Take 3 deep diaphragmatic breaths (4s in, 4s hold, 6s out).")
                    Text("• Change your immediate environment or stand up for 2 minutes.")
                    Text("• Remind yourself of the impact: cost category '${item.costType}' with impact '${item.costValue}'.")
                }
            }
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = BrandViolet), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Got It!")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphBottomSheet(
    item: TrackWiseViewModel.BadHabitSpec,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Streak History & Analytics", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(item.name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${item.avoidedCount}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                    Text("Times Avoided", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${item.slippedCount}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BrandRose)
                    Text("Relapses", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReflectionsBottomSheet(
    item: TrackWiseViewModel.BadHabitSpec,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Journal Reflection", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("How do you feel avoiding '${item.name}' today?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Write your thoughts...") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = BrandGreen), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Save Reflection")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestonesBottomSheet(
    item: TrackWiseViewModel.BadHabitSpec,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("🎯 Milestones for ${item.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            val milestones = listOf("1 Day Clean" to true, "3 Days Clean" to true, "1 Week Clean" to false, "1 Month Clean" to false, "1 Year Clean" to false)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                milestones.forEach { (label, achieved) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, fontWeight = FontWeight.Medium)
                        Text(if (achieved) "✅ Achieved" else "🔒 Locked", color = if (achieved) BrandGreen else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreakGameBottomSheet(
    item: TrackWiseViewModel.BadHabitSpec,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("🎮 Select Distraction Game", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Break the urge with a quick 30-second focus game.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            listOf("🧘 Deep Breathing Loop", "🧩 Memory Pattern Tap", "⚡ Speed Reflex Challenge").forEach { gameName ->
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(gameName, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerBottomSheet(
    item: TrackWiseViewModel.BadHabitSpec,
    onDismiss: () -> Unit,
    onSelectIcon: (String) -> Unit
) {
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("🎨 Choose Icon for ${item.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            FlatColorIconPicker(
                selectedIcon = item.iconName,
                onIconSelected = { key ->
                    onSelectIcon(key)
                    onDismiss()
                },
                accentColor = BrandRose,
                searchQuery = item.name,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
