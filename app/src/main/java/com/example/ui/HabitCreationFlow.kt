package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.utils.TrackWiseUtils
import java.util.Calendar

data class HabitTemplate(
    val name: String,
    val subtitle: String,
    val icon: String,
    val category: String,
    val quote: String = "Believe in yourself.",
    val iconBgColor: Color = Color(0xFFE0F2FE),
    val iconColor: Color = Color(0xFF0284C7)
)

val GALLERY_TEMPLATES = mapOf(
    "Suggested" to listOf(
        HabitTemplate("Daily Check-in", "Try a little harder to be a little better", "😊", "Suggested", "Try a little harder to be a little better", Color(0xFFDCFCE7), Color(0xFF16A34A)),
        HabitTemplate("Drink water", "Stay moisturized", "🥛", "Suggested", "Stay hydrated, stay energized.", Color(0xFFE0F2FE), Color(0xFF0284C7)),
        HabitTemplate("Eat breakfast", "Life begins after breakfast", "🍞", "Suggested", "A good day starts with a nourishing meal.", Color(0xFFFCE7F3), Color(0xFFDB2777)),
        HabitTemplate("Eat fruits", "Stay healthier, stay happier", "🍌", "Suggested", "Fuel your body with nature's candy.", Color(0xFFFEF9C3), Color(0xFFCA8A04)),
        HabitTemplate("Early to rise", "Get up and be amazing", "☀️", "Suggested", "Win the morning, win the day.", Color(0xFFFEF3C7), Color(0xFFD97706)),
        HabitTemplate("Early to bed", "Dream lofty dream", "🌙", "Suggested", "Rest well to thrive tomorrow.", Color(0xFFE0E7FF), Color(0xFF4F46E5)),
        HabitTemplate("Learn new words", "Small number, big result", "📖", "Suggested", "Expand your mind one word at a time.", Color(0xFFE0F2FE), Color(0xFF0284C7)),
        HabitTemplate("Read", "A chapter a day will light your way", "📘", "Suggested", "Books are uniquely portable magic.", Color(0xFFDBEAFE), Color(0xFF2563EB))
    ),
    "Life" to listOf(
        HabitTemplate("Journaling", "Reflect on your day", "📓", "Life", "Reflection leads to wisdom.", Color(0xFFF1F5F9), Color(0xFF475569)),
        HabitTemplate("Tidy up room", "Clean space, clear mind", "🧹", "Life", "Order in your environment brings peace.", Color(0xFFFEF3C7), Color(0xFFD97706)),
        HabitTemplate("Call parents", "Stay connected with family", "📞", "Life", "Love and family come first.", Color(0xFFDCFCE7), Color(0xFF16A34A)),
        HabitTemplate("Plant care", "Nurture your green friends", "🪴", "Life", "Patience and care bring growth.", Color(0xFFDCFCE7), Color(0xFF15803D))
    ),
    "Health" to listOf(
        HabitTemplate("Drink water", "Stay moisturized", "🥛", "Health", "Stay hydrated, stay energized.", Color(0xFFE0F2FE), Color(0xFF0284C7)),
        HabitTemplate("Eat fruits", "Stay healthier, stay happier", "🍌", "Health", "Fuel your body with nature's candy.", Color(0xFFFEF9C3), Color(0xFFCA8A04)),
        HabitTemplate("Take vitamins", "Keep your body strong", "💊", "Health", "Invest in your health daily.", Color(0xFFFCE7F3), Color(0xFFDB2777)),
        HabitTemplate("Sleep 8 hours", "Rest and restore", "😴", "Health", "Sleep is the foundation of energy.", Color(0xFFE0E7FF), Color(0xFF4F46E5)),
        HabitTemplate("No sugar", "Cut out processed sweets", "🚫", "Health", "Pure energy without the crash.", Color(0xFFFEE2E2), Color(0xFFDC2626))
    ),
    "Sports" to listOf(
        HabitTemplate("Workout 30 mins", "Build your strength", "🏋️", "Sports", "No pain, no gain. Stay strong!", Color(0xFFFEE2E2), Color(0xFFDC2626)),
        HabitTemplate("10k steps", "Keep moving forward", "🏃", "Sports", "Every step brings you closer to your goal.", Color(0xFFFEF3C7), Color(0xFFD97706)),
        HabitTemplate("Stretching", "Stay flexible and relaxed", "🧘", "Sports", "Flexibility prevents injury and relaxes the mind.", Color(0xFFE0F2FE), Color(0xFF0284C7)),
        HabitTemplate("Cycling", "Enjoy the ride", "🚴", "Sports", "Life is like riding a bicycle. Keep moving.", Color(0xFFDCFCE7), Color(0xFF16A34A))
    ),
    "Mindset" to listOf(
        HabitTemplate("Daily Check-in", "Try a little harder to be a little better", "😊", "Mindset", "Try a little harder to be a little better", Color(0xFFDCFCE7), Color(0xFF16A34A)),
        HabitTemplate("Learn new words", "Small number, big result", "📖", "Mindset", "Expand your mind one word at a time.", Color(0xFFE0F2FE), Color(0xFF0284C7)),
        HabitTemplate("Read", "A chapter a day will light your way", "📘", "Mindset", "Books are uniquely portable magic.", Color(0xFFDBEAFE), Color(0xFF2563EB)),
        HabitTemplate("Gratitude journal", "Count your blessings", "🙏", "Mindset", "Gratitude turns what we have into enough.", Color(0xFFFCE7F3), Color(0xFFDB2777)),
        HabitTemplate("Limit screen time", "Be present in the moment", "📵", "Mindset", "Disconnect to reconnect with life.", Color(0xFFFEE2E2), Color(0xFFDC2626))
    )
)

val ICON_OPTIONS = listOf(
    "😊", "🥛", "🍞", "🍚", "🍌", "🥕", "🍦", "🚫", "🌙", "🏃",
    "🧘", "🤸", "🦵", "🚴", "🏊", "📖", "✏️", "📓", "💵", "📋",
    "📞", "👍", "📷", "👁️", "🦷", "🚿", "🧹", "⭐", "📹", "📺",
    "🎵", "🚶", "🐕", "🐈", "🎬", "📄", "🚭", "💖", "☀️", "💊",
    "💡", "🚀", "🔥", "🎯", "🎨", "💻", "🧠", "🌱", "🧘‍♀️", "💪",
    "🍎", "🥗", "🍵", "🧺", "⏰", "📅", "📝", "✍️", "🧩", "🎳",
    "🎮", "⚽", "🏀", "👟", "👚", "🧴", "💤", "🛌", "🧗", "🛹",
    "🎸", "🎻", "🎤", "🎧", "🏋️", "🚶‍♂️", "🚴‍♂️", "🧘‍♂️", "🛌", "🙏"
)

val MOTIVATIONAL_QUOTES = listOf(
    "Believe in yourself.",
    "Now or never.",
    "Try a little harder to be a little better.",
    "Small daily improvements lead to stunning results.",
    "Consistency is the key to mastery.",
    "Dream lofty dream.",
    "Action is the foundational key to all success.",
    "Your future self will thank you for taking action today.",
    "Every day is a fresh start.",
    "Progress over perfection."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitCreationFlowDialog(
    viewModel: TrackWiseViewModel,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        var currentStep by remember { mutableIntStateOf(0) } // 0: Gallery, 1: Step 1 (Name/Icon/Quote), 2: Step 2 (Frequency/Settings)

        // Form state
        var galleryCategory by remember { mutableStateOf("Suggested") }
        var habitName by remember { mutableStateOf("") }
        var selectedIcon by remember { mutableStateOf("😊") }
        var currentQuote by remember { mutableStateOf("") }
        var habitCategory by remember { mutableStateOf("Suggested") }

        // Step 2 state
        var frequencyType by remember { mutableStateOf("DAILY") } // DAILY, WEEKLY, INTERVAL
        var selectedDaysOfWeek by remember { mutableStateOf(setOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")) }
        var daysPerWeek by remember { mutableIntStateOf(2) }
        var intervalDays by remember { mutableIntStateOf(2) }

        var goalType by remember { mutableStateOf("Achieve it all") } // "Achieve it all", "Reach a certain amount"
        var goalAmount by remember { mutableIntStateOf(1) }
        var startDate by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }
        var goalDays by remember { mutableStateOf("Forever") }

        var selectedFolders by remember { mutableStateOf(setOf("Inbox")) }
        val tasks by viewModel.allTasks.collectAsState()
        val customFolders by viewModel.customFolders.collectAsState()
        val allFolders = remember(tasks, customFolders) {
            val defaults = listOf("Inbox", "Work", "Personal", "Shopping", "Learning", "Wish List", "Fitness", "Welcome")
            val dynamic = tasks.map { it.project }.filter { it.isNotBlank() }
            (defaults + customFolders + dynamic).distinct()
        }

        var customTagInput by remember { mutableStateOf("") }

        var reminderTimes by remember { mutableStateOf(mutableListOf<String>()) }
        var autoPopupLog by remember { mutableStateOf(false) }

        // Dialogs inside step 2
        var showGoalDialog by remember { mutableStateOf(false) }
        var showDatePickerDialog by remember { mutableStateOf(false) }
        var showGoalDaysDialog by remember { mutableStateOf(false) }
        var showAddSectionDialog by remember { mutableStateOf(false) }
        var showAddReminderDialog by remember { mutableStateOf(false) }

        var nameError by remember { mutableStateOf(false) }

        val primaryColor = MaterialTheme.colorScheme.primary
        val backgroundColor = MaterialTheme.colorScheme.background
        val surfaceColor = MaterialTheme.colorScheme.surface
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface
        val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
        val textColor = MaterialTheme.colorScheme.onBackground

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                TopAppBar(
                    title = {
                        Text(
                            text = if (currentStep == 0) "Gallery" else "New Habit",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = textColor
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (currentStep == 0) {
                                    onDismiss()
                                } else {
                                    currentStep -= 1
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentStep) {
                        0 -> {
                            // SCREENSHOT 1: GALLERY
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Category Chips
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(listOf("Suggested", "Life", "Health", "Sports", "Mindset")) { cat ->
                                        val isSelected = galleryCategory == cat
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.clickable { galleryCategory = cat }
                                        ) {
                                            Text(
                                                text = cat,
                                                color = if (isSelected) Color.White else onSurfaceVariantColor,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 14.sp,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }

                                // Habit Templates List
                                val templates = GALLERY_TEMPLATES[galleryCategory] ?: emptyList()
                                LazyColumn(
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(templates) { template ->
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    habitName = template.name
                                                    selectedIcon = template.icon
                                                    currentQuote = template.quote
                                                    habitCategory = template.category
                                                    currentStep = 1
                                                }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(CircleShape)
                                                        .background(template.iconBgColor)
                                                ) {
                                                    Text(text = template.icon, fontSize = 24.sp)
                                                }

                                                Spacer(modifier = Modifier.width(14.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = template.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Text(
                                                        text = template.subtitle,
                                                        fontSize = 13.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        habitName = template.name
                                                        selectedIcon = template.icon
                                                        currentQuote = template.quote
                                                        habitCategory = template.category
                                                        currentStep = 1
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Add template",
                                                        tint = Color(0xFF94A3B8)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // SCREENSHOT 2 & 3: NEW HABIT STEP 1 (Name, Icon, Quote)
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Card 1: Name
                                item {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Name",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = textColor
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            OutlinedTextField(
                                                value = habitName,
                                                onValueChange = {
                                                    habitName = it
                                                    if (it.isNotBlank()) nameError = false
                                                },
                                                placeholder = { Text("Daily Check-in", color = onSurfaceVariantColor) },
                                                trailingIcon = {
                                                    if (habitName.isNotEmpty()) {
                                                        IconButton(onClick = { habitName = "" }) {
                                                            Icon(
                                                                imageVector = Icons.Default.Cancel,
                                                                contentDescription = "Clear",
                                                                tint = onSurfaceVariantColor
                                                            )
                                                        }
                                                    }
                                                },
                                                isError = nameError,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = primaryColor,
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    focusedContainerColor = backgroundColor,
                                                    unfocusedContainerColor = backgroundColor
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            if (nameError) {
                                                Text(
                                                    text = "Habit name is required",
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Card 2: Icon Selection
                                item {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Icon",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = textColor
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Top active preview circles
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .size(52.dp)
                                                        .border(2.dp, primaryColor.copy(alpha = 0.5f), CircleShape)
                                                        .padding(3.dp)
                                                        .clip(CircleShape)
                                                        .background(primaryColor.copy(alpha = 0.15f))
                                                ) {
                                                    Text(text = selectedIcon, fontSize = 26.sp)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Slider / LazyRow Grid of Icons (4 rows of columns)
                                            val iconRows = 4
                                            val chunkedIcons = remember { ICON_OPTIONS.chunked(iconRows) }
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(240.dp)
                                            ) {
                                                items(chunkedIcons) { columnIcons ->
                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        columnIcons.forEach { iconStr ->
                                                            val isSelected = selectedIcon == iconStr
                                                            Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier = Modifier
                                                                    .size(45.dp)
                                                                    .clip(CircleShape)
                                                                    .background(
                                                                        if (isSelected) primaryColor.copy(alpha = 0.15f)
                                                                        else MaterialTheme.colorScheme.surfaceVariant
                                                                    )
                                                                    .border(
                                                                        width = if (isSelected) 2.dp else 0.dp,
                                                                        color = if (isSelected) primaryColor else Color.Transparent,
                                                                        shape = CircleShape
                                                                    )
                                                                    .clickable { selectedIcon = iconStr }
                                                            ) {
                                                                Text(text = iconStr, fontSize = 22.sp)
                                                                if (isSelected) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Check,
                                                                        contentDescription = null,
                                                                        tint = primaryColor,
                                                                        modifier = Modifier
                                                                            .size(14.dp)
                                                                            .align(Alignment.BottomEnd)
                                                                            .offset(x = 2.dp, y = 2.dp)
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

                                // Card 3: Quote
                                item {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "Quote",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    color = textColor
                                                )
                                                IconButton(
                                                    onClick = {
                                                        currentQuote = MOTIVATIONAL_QUOTES.random()
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = "Refresh Quote",
                                                        tint = primaryColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(backgroundColor)
                                                    .padding(14.dp)
                                            ) {
                                                Text(
                                                    text = currentQuote.ifBlank { "Choose or enter a tag below..." },
                                                    fontSize = 14.sp,
                                                    color = onSurfaceColor,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // SCREENSHOT 4, 5, 6, 7, 8, 9: NEW HABIT STEP 2 (Frequency, Goals, Folders, Reminder, #tags)
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Card 1: Frequency
                                item {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Frequency",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = textColor
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Frequency Tabs (DAILY, WEEKLY, INTERVAL)
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                listOf("DAILY", "WEEKLY", "INTERVAL").forEach { type ->
                                                    val isSelected = frequencyType == type
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier = Modifier.clickable { frequencyType = type }
                                                    ) {
                                                        Text(
                                                            text = type,
                                                            fontSize = 13.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) primaryColor else onSurfaceVariantColor
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        if (isSelected) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(28.dp)
                                                                    .height(3.dp)
                                                                    .clip(RoundedCornerShape(2.dp))
                                                                    .background(primaryColor)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Content based on Frequency Tab
                                            when (frequencyType) {
                                                "DAILY" -> {
                                                    // Day Selector S M T W T F S with unique keys to fix the duplicate day selection bug
                                                    Row(
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        val daysList = listOf(
                                                            "Sun" to "S",
                                                            "Mon" to "M",
                                                            "Tue" to "T",
                                                            "Wed" to "W",
                                                            "Thu" to "T",
                                                            "Fri" to "F",
                                                            "Sat" to "S"
                                                        )
                                                        daysList.forEach { (dayId, dayLabel) ->
                                                            val isSelected = selectedDaysOfWeek.contains(dayId)
                                                            Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier = Modifier
                                                                    .size(40.dp)
                                                                    .clip(CircleShape)
                                                                    .background(
                                                                        if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                                                                    )
                                                                    .clickable {
                                                                        val newSet = selectedDaysOfWeek.toMutableSet()
                                                                        if (isSelected) newSet.remove(dayId)
                                                                        else newSet.add(dayId)
                                                                        selectedDaysOfWeek = newSet
                                                                    }
                                                            ) {
                                                                Text(
                                                                    text = dayLabel,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (isSelected) Color.White else onSurfaceColor
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                "WEEKLY" -> {
                                                    // X days per week selector
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center,
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                                    ) {
                                                        IconButton(
                                                            onClick = { if (daysPerWeek > 1) daysPerWeek-- }
                                                        ) {
                                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", tint = primaryColor)
                                                        }
                                                        Text(
                                                            text = "$daysPerWeek",
                                                            fontSize = 28.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = textColor,
                                                            modifier = Modifier.padding(horizontal = 20.dp)
                                                        )
                                                        IconButton(
                                                            onClick = { if (daysPerWeek < 7) daysPerWeek++ }
                                                        ) {
                                                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase", tint = primaryColor)
                                                        }
                                                        Text(
                                                            text = "days per week",
                                                            fontSize = 15.sp,
                                                            color = onSurfaceVariantColor,
                                                            modifier = Modifier.padding(start = 12.dp)
                                                        )
                                                    }
                                                }
                                                "INTERVAL" -> {
                                                    // Every X days selector
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center,
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                                    ) {
                                                        Text(text = "Every", fontSize = 15.sp, color = onSurfaceVariantColor, modifier = Modifier.padding(end = 12.dp))
                                                        IconButton(
                                                            onClick = { if (intervalDays > 1) intervalDays-- }
                                                        ) {
                                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", tint = primaryColor)
                                                        }
                                                        Text(
                                                            text = "$intervalDays",
                                                            fontSize = 28.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = textColor,
                                                            modifier = Modifier.padding(horizontal = 20.dp)
                                                        )
                                                        IconButton(
                                                            onClick = { if (intervalDays < 365) intervalDays++ }
                                                        ) {
                                                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase", tint = primaryColor)
                                                        }
                                                        Text(text = "days", fontSize = 15.sp, color = onSurfaceVariantColor, modifier = Modifier.padding(start = 12.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Card 2: Goal, Start Date, Goal Days
                                item {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                            // Goal Row
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { showGoalDialog = true }
                                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                            ) {
                                                Text("Goal", fontSize = 15.sp, color = textColor, fontWeight = FontWeight.Medium)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = if (goalType == "Achieve it all") "Achieve it all" else "$goalAmount time(s)",
                                                        fontSize = 14.sp,
                                                        color = onSurfaceVariantColor
                                                    )
                                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = onSurfaceVariantColor)
                                                }
                                            }

                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                                            // Start Date Row
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { showDatePickerDialog = true }
                                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                            ) {
                                                Text("Start Date", fontSize = 15.sp, color = textColor, fontWeight = FontWeight.Medium)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = startDate,
                                                        fontSize = 14.sp,
                                                        color = onSurfaceVariantColor
                                                    )
                                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = onSurfaceVariantColor)
                                                }
                                            }

                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                                            // Goal Days Row
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { showGoalDaysDialog = true }
                                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Goal Days", fontSize = 15.sp, color = textColor, fontWeight = FontWeight.Medium)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = goalDays,
                                                        fontSize = 14.sp,
                                                        color = onSurfaceVariantColor
                                                    )
                                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = onSurfaceVariantColor)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Card 3: Folders (Renamed from Sections and tied to real customFolders list)
                                item {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "Folders",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    color = textColor
                                                )
                                                IconButton(
                                                    onClick = { showAddSectionDialog = true },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Add Folder", tint = onSurfaceVariantColor)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                items(allFolders) { folder ->
                                                    val isSelected = selectedFolders.contains(folder)
                                                    Surface(
                                                        shape = RoundedCornerShape(20.dp),
                                                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant,
                                                        modifier = Modifier.clickable {
                                                            val updated = selectedFolders.toMutableSet()
                                                            if (isSelected) updated.remove(folder) else updated.add(folder)
                                                            selectedFolders = updated
                                                        }
                                                    ) {
                                                        Text(
                                                            text = folder,
                                                            color = if (isSelected) Color.White else onSurfaceColor,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            fontSize = 13.sp,
                                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Card 4: Reminder
                                item {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Reminder",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = textColor
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            if (reminderTimes.isNotEmpty()) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                ) {
                                                    reminderTimes.forEach { time ->
                                                        AssistChip(
                                                            onClick = {},
                                                            label = { Text(time) },
                                                            trailingIcon = {
                                                                IconButton(
                                                                    onClick = {
                                                                        reminderTimes = reminderTimes.toMutableList().apply { remove(time) }
                                                                    },
                                                                    modifier = Modifier.size(18.dp)
                                                                ) {
                                                                    Icon(Icons.Default.Close, contentDescription = "Remove")
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            TextButton(
                                                onClick = { showAddReminderDialog = true },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }

                                // Card 5: Select a #tag (Replaces auto pop-up switch with highly interactive tagging)
                                item {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Select #tag",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = textColor
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))

                                            OutlinedTextField(
                                                value = customTagInput,
                                                onValueChange = { input ->
                                                    customTagInput = if (input.startsWith("#") || input.isEmpty()) input else "#$input"
                                                    currentQuote = customTagInput
                                                },
                                                placeholder = { Text("Type custom tag (e.g. #focus)", color = onSurfaceVariantColor, fontSize = 13.sp) },
                                                singleLine = true,
                                                textStyle = TextStyle(fontSize = 13.sp, color = textColor),
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = primaryColor,
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    focusedContainerColor = backgroundColor,
                                                    unfocusedContainerColor = backgroundColor
                                                )
                                            )

                                            Spacer(modifier = Modifier.height(12.dp))

                                            val presetTags = listOf("#Health", "#Fitness", "#Routine", "#Mindset", "#Work", "#Personal", "#Study")
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                items(presetTags) { tag ->
                                                    val isSelected = currentQuote == tag
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant,
                                                        modifier = Modifier.clickable {
                                                            if (isSelected) {
                                                                currentQuote = ""
                                                                customTagInput = ""
                                                            } else {
                                                                currentQuote = tag
                                                                customTagInput = tag
                                                            }
                                                        }
                                                    ) {
                                                        Text(
                                                            text = tag,
                                                            color = if (isSelected) Color.White else onSurfaceColor,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            fontSize = 12.sp,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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

                // Bottom Action Button Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceColor)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            when (currentStep) {
                                0 -> {
                                    // Create a new habit
                                    habitName = ""
                                    selectedIcon = "😊"
                                    currentQuote = ""
                                    currentStep = 1
                                }
                                1 -> {
                                    // Next button
                                    if (habitName.isBlank()) {
                                        nameError = true
                                    } else {
                                        nameError = false
                                        currentStep = 2
                                    }
                                }
                                2 -> {
                                    // Save button
                                    val repeatTypeVal = when (frequencyType) {
                                        "DAILY" -> "daily"
                                        "WEEKLY" -> "weekly"
                                        "INTERVAL" -> "custom"
                                        else -> "daily"
                                    }
                                    val customDays = if (frequencyType == "DAILY") selectedDaysOfWeek.joinToString(",") else null

                                    viewModel.addHabit(
                                        name = habitName,
                                        category = habitCategory,
                                        isMultipleTimesPerDay = (goalType == "Reach a certain amount"),
                                        multipleTimesTarget = if (goalType == "Reach a certain amount") goalAmount else 1,
                                        repeatType = repeatTypeVal,
                                        customRepeatValue = if (frequencyType == "INTERVAL") intervalDays else daysPerWeek,
                                        customRepeatUnit = if (frequencyType == "INTERVAL") "days" else "weeks",
                                        customRepeatDaysOfWeek = customDays,
                                        startDate = startDate,
                                        remindMe = reminderTimes.isNotEmpty(),
                                        reminderTime = reminderTimes.firstOrNull(),
                                        icon = selectedIcon,
                                        quote = currentQuote,
                                        goalType = goalType,
                                        goalDays = goalDays,
                                        section = if (selectedFolders.isEmpty()) "Inbox" else selectedFolders.joinToString(","),
                                        autoPopup = false
                                    )

                                    onDismiss()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("habit_primary_action_button")
                    ) {
                        Text(
                            text = when (currentStep) {
                                0 -> "Create a new habit"
                                1 -> "Next"
                                else -> "Save"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // SCREENSHOT 7: Goal Dialog
        if (showGoalDialog) {
            AlertDialog(
                onDismissRequest = { showGoalDialog = false },
                title = { Text("Goal", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { goalType = "Achieve it all" }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = (goalType == "Achieve it all"),
                                onClick = { goalType = "Achieve it all" },
                                colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Achieve it all", fontSize = 15.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { goalType = "Reach a certain amount" }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = (goalType == "Reach a certain amount"),
                                onClick = { goalType = "Reach a certain amount" },
                                colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reach a certain amount", fontSize = 15.sp)
                        }

                        if (goalType == "Reach a certain amount") {
                            OutlinedTextField(
                                value = goalAmount.toString(),
                                onValueChange = { goalAmount = it.toIntOrNull() ?: 1 },
                                label = { Text("Target count") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showGoalDialog = false }) {
                        Text("OK", color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGoalDialog = false }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                }
            )
        }

        // SCREENSHOT 8: Date Picker Dialog
        if (showDatePickerDialog) {
            val calendar = Calendar.getInstance()
            var selYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
            var selMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
            var selDay by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

            AlertDialog(
                onDismissRequest = { showDatePickerDialog = false },
                title = { Text("Date", fontWeight = FontWeight.Bold) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Month navigator
                        val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { if (selMonth > 0) selMonth-- else { selMonth = 11; selYear-- } }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month")
                            }
                            Text("${monthNames[selMonth]} $selYear", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            IconButton(onClick = { if (selMonth < 11) selMonth++ else { selMonth = 0; selYear++ } }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Grid of days
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            items(listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")) { h ->
                                Text(h, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            }
                            items(31) { dayIdx ->
                                val dayNum = dayIdx + 1
                                val isSel = (selDay == dayNum)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isSel) primaryColor else Color.Transparent)
                                        .clickable { selDay = dayNum }
                                ) {
                                    Text(
                                        text = "$dayNum",
                                        fontSize = 13.sp,
                                        color = if (isSel) Color.White else textColor
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val monthStr = (selMonth + 1).toString().padStart(2, '0')
                            val dayStr = selDay.toString().padStart(2, '0')
                            startDate = "$selYear-$monthStr-$dayStr"
                            showDatePickerDialog = false
                        }
                    ) {
                        Text("Confirm", color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerDialog = false }) {
                        Text("Cancel", color = onSurfaceVariantColor)
                    }
                }
            )
        }

        var customGoalDaysInput by remember {
            val digits = goalDays.filter { it.isDigit() }
            mutableStateOf(if (goalDays.startsWith("Custom") && digits.isNotBlank()) digits else "")
        }

        // SCREENSHOT 9: Goal Days Dialog
        if (showGoalDaysDialog) {
            AlertDialog(
                onDismissRequest = { showGoalDaysDialog = false },
                title = { Text("Goal Days", fontWeight = FontWeight.Bold, color = textColor) },
                text = {
                    val options = listOf("Forever", "7 days", "21 days", "30 days", "100 days", "365 days", "Custom")
                    LazyColumn {
                        items(options) { opt ->
                            val isSelected = if (opt == "Custom") {
                                goalDays.startsWith("Custom")
                            } else {
                                goalDays == opt
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (opt == "Custom") {
                                            goalDays = if (customGoalDaysInput.isNotBlank()) "Custom ($customGoalDaysInput Days)" else "Custom"
                                        } else {
                                            goalDays = opt
                                        }
                                    }
                                    .padding(vertical = 6.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        if (opt == "Custom") {
                                            goalDays = if (customGoalDaysInput.isNotBlank()) "Custom ($customGoalDaysInput Days)" else "Custom"
                                        } else {
                                            goalDays = opt
                                        }
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(opt, fontSize = 15.sp, color = textColor)
                                
                                if (opt == "Custom" && isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = customGoalDaysInput,
                                        onValueChange = { input ->
                                            val filtered = input.filter { it.isDigit() }
                                            if (filtered.length <= 3) {
                                                val intVal = filtered.toIntOrNull()
                                                if (filtered.isEmpty() || (intVal != null && intVal in 1..999)) {
                                                    customGoalDaysInput = filtered
                                                    goalDays = if (filtered.isNotBlank()) "Custom ($filtered Days)" else "Custom"
                                                }
                                            }
                                        },
                                        placeholder = { Text("1-999", fontSize = 12.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = textColor,
                                            unfocusedTextColor = textColor,
                                            focusedBorderColor = primaryColor,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .width(90.dp)
                                            .height(52.dp)
                                            .padding(vertical = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Days", fontSize = 14.sp, color = textColor)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showGoalDaysDialog = false }) {
                        Text("OK", color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGoalDaysDialog = false }) {
                        Text("Cancel", color = onSurfaceVariantColor)
                    }
                }
            )
        }

        // Add Folder Dialog
        if (showAddSectionDialog) {
            var newFolderName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddSectionDialog = false },
                title = { Text("Add Folder", fontWeight = FontWeight.Bold, color = textColor) },
                text = {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder Name", color = onSurfaceVariantColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newFolderName.isNotBlank()) {
                                viewModel.addCustomFolder(newFolderName.trim())
                                selectedFolders = selectedFolders + newFolderName.trim()
                            }
                            showAddSectionDialog = false
                        }
                    ) {
                        Text("Add", color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSectionDialog = false }) {
                        Text("Cancel", color = onSurfaceVariantColor)
                    }
                }
            )
        }

        // Add Reminder Dialog using Native Clock
        if (showAddReminderDialog) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val calendar = java.util.Calendar.getInstance()
            val isDarkTheme = MaterialTheme.colorScheme.background.let { (it.red + it.green + it.blue) / 3f < 0.5f }
            val themeId = if (isDarkTheme) android.R.style.Theme_DeviceDefault_Dialog_Alert else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert
            
            androidx.compose.runtime.DisposableEffect(Unit) {
                val tpd = android.app.TimePickerDialog(
                    context,
                    themeId,
                    { _, h, m ->
                        val formattedTime = String.format(java.util.Locale.US, "%02d:%02d %s", 
                            if (h == 0 || h == 12) 12 else h % 12, 
                            m, 
                            if (h < 12) "AM" else "PM"
                        )
                        reminderTimes = reminderTimes.toMutableList().apply { if (!contains(formattedTime)) add(formattedTime) }
                        showAddReminderDialog = false
                    },
                    calendar.get(java.util.Calendar.HOUR_OF_DAY),
                    calendar.get(java.util.Calendar.MINUTE),
                    false // Use 12h clock
                )
                tpd.setOnCancelListener {
                    showAddReminderDialog = false
                }
                tpd.show()
                onDispose {
                    tpd.dismiss()
                }
            }
        }
    }
}
