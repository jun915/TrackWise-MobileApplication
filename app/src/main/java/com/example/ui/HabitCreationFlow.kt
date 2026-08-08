package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import com.example.utils.TrackWiseUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    "star", "fire", "fitness", "run", "water", "book", "food", "sun", "moon", "smile",
    "journal", "tidy", "phone", "plant", "vitamin", "sleep", "sports", "music", "coffee", "camera",
    "home", "car", "bike", "pool", "movie", "gaming", "nature", "beach", "flag", "brush",
    "lightbulb", "compass", "map", "timer", "alarm", "shield", "sparkles", "diamond", "event", "heart",
    "trophy", "bell", "flight", "pets", "work", "school", "medical", "shopping", "money", "laptop",
    "spa", "check", "lock", "cake", "balloon", "medal", "fastfood", "email", "group", "person",
    "place", "search", "like", "verified", "target", "brain", "nosugar", "gratitude"
).distinct()

val MOTIVATIONAL_QUOTES = listOf(
    "Believe in yourself and all that you are.",
    "Small daily improvements lead to stunning results.",
    "Consistency is the key to mastery.",
    "Your future self will thank you for taking action today.",
    "Action is the foundational key to all success.",
    "Every day is a fresh start.",
    "Progress over perfection.",
    "Now or never.",
    "Try a little harder to be a little better.",
    "Dream big, start small, act now.",
    "Success is the sum of small efforts repeated daily.",
    "Do something today that your future self will thank you for.",
    "You don't have to be great to start, but you have to start to be great.",
    "Focus on being productive instead of busy.",
    "Discipline is choosing between what you want now and what you want most.",
    "The secret of getting ahead is getting started.",
    "Small steps in the right direction can turn out to be the biggest step of your life.",
    "Energy flows where attention goes.",
    "Be stronger than your excuses.",
    "Great things never come from comfort zones.",
    "Don't count the days, make the days count.",
    "Your only limit is you.",
    "Stay positive, work hard, make it happen.",
    "Doubt kills more dreams than failure ever will.",
    "Turn your obstacles into opportunities.",
    "Believe you can and you're halfway there.",
    "Excellence is not an act, but a habit.",
    "What you do today can improve all your tomorrows.",
    "Hard work beats talent when talent doesn't work hard.",
    "The best way to predict the future is to create it.",
    "Do what you can, with what you have, where you are.",
    "A journey of a thousand miles begins with a single step.",
    "Fall seven times, stand up eight.",
    "Success isn't overnight. It's when every day you get a little better.",
    "Make each day your masterpiece.",
    "Push yourself, because no one else is going to do it for you.",
    "You are capable of amazing things.",
    "Wake up with determination, go to bed with satisfaction.",
    "It always seems impossible until it's done.",
    "Difficult roads often lead to beautiful destinations.",
    "Be so good they can'ignore you.",
    "Work hard in silence, let your success be your noise.",
    "Your mind is a powerful thing. Fill it with positive thoughts.",
    "Little by little, one travels far.",
    "Don't watch the clock; do what it does. Keep going.",
    "You do not find the happy life. You make it.",
    "Build your own dreams, or someone else will hire you to build theirs.",
    "Start where you are. Use what you have. Do what you can.",
    "Courage is resistance to fear, mastery of fear, not absence of fear.",
    "Aim for the moon. If you miss, you may hit a star.",
    "Success is walking from failure to failure with no loss of enthusiasm.",
    "The only way to do great work is to love what you do.",
    "Opportunities don't happen, you create them.",
    "Never give up on a dream just because of the time it will take to accomplish it.",
    "Everything you've ever wanted is on the other side of fear.",
    "Success starts with self-discipline.",
    "Clear your mind of can't.",
    "Believe in the power of yet.",
    "Habits shape your future; cultivate good ones daily.",
    "Focus on the journey, not the outcome.",
    "Be the change that you wish to see in the world.",
    "With self-discipline almost anything is possible.",
    "Mindset is everything.",
    "Keep your eyes on the stars, and your feet on the ground.",
    "The difference between ordinary and extraordinary is that little extra.",
    "Success belongs to those who prepare for it today.",
    "Stay hungry, stay foolish.",
    "Mastering yourself is true power.",
    "One day or day one. You decide.",
    "Don't wait for opportunity. Create it.",
    "Greatness is a series of small decisions made consistently.",
    "Self-care and self-discipline go hand in hand.",
    "Transform your habits, transform your life.",
    "Rise above the storm and you will find the sunshine.",
    "Consistency turns motion into progress.",
    "You are the author of your own story.",
    "Choose progress over comfort.",
    "Your habits determine your quality of life.",
    "Focus on what you can control.",
    "Small wins pave the way to monumental triumphs.",
    "Stay committed to your decisions, but flexible in your approach.",
    "Strength does not come from winning. Your struggles develop your strengths.",
    "Be relentless in the pursuit of what sets your soul on fire.",
    "Every accomplishment starts with the decision to try.",
    "Don't let yesterday take up too much of today.",
    "Show up for yourself every single day.",
    "Your potential is endless.",
    "Quality is not an act, it is a habit.",
    "Be patient with yourself. Nothing in nature blooms all year.",
    "Action cures anxiety and fear.",
    "Small daily habits multiply over time.",
    "Keep moving forward, no matter how slow.",
    "You are capable of more than you know.",
    "Focus, commit, and achieve.",
    "Continuous improvement is better than delayed perfection.",
    "The habit of persistence is the habit of victory.",
    "Success is habit in action.",
    "Nurture your goals with daily effort.",
    "You are one habit away from a breakthrough."
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
        val habitToEdit by viewModel.habitToEdit.collectAsState()

        var currentStep by remember(habitToEdit) { mutableIntStateOf(if (habitToEdit != null) 1 else 0) } // 0: Gallery, 1: Step 1 (Name/Icon/Quote), 2: Step 2 (Frequency/Settings)

        // Form state
        var galleryCategory by remember { mutableStateOf("Suggested") }
        var habitName by remember(habitToEdit) { mutableStateOf(habitToEdit?.name ?: "") }
        var selectedIcon by remember(habitToEdit) { mutableStateOf(habitToEdit?.icon ?: "😊") }
        var currentQuote by remember(habitToEdit) { mutableStateOf(habitToEdit?.quote ?: "") }
        var habitCategory by remember(habitToEdit) { mutableStateOf(habitToEdit?.category ?: "Suggested") }
        var showQuotesPickerDialog by remember { mutableStateOf(false) }
        var quotesSearchQuery by remember { mutableStateOf("") }

        // Step 2 state
        var selectedFrequencyOption by remember(habitToEdit) { 
            mutableStateOf(
                if (habitToEdit != null) {
                    when (habitToEdit!!.repeatType.lowercase()) {
                        "daily" -> "DAILY"
                        "weekdays" -> "WEEKDAYS"
                        "weekly" -> "WEEKLY"
                        "monthly" -> "MONTHLY"
                        "yearly" -> "YEARLY"
                        "custom" -> "CUSTOM"
                        else -> "DAILY"
                    }
                } else "DAILY"
            ) 
        }

        var customRepeatValueState by remember(habitToEdit) { 
            mutableIntStateOf(
                if (habitToEdit != null && habitToEdit!!.repeatType.equals("custom", ignoreCase = true)) {
                    habitToEdit!!.customRepeatValue.coerceAtLeast(1)
                } else 1
            ) 
        }

        var customRepeatUnitState by remember(habitToEdit) { 
            mutableStateOf(
                if (habitToEdit != null && habitToEdit!!.repeatType.equals("custom", ignoreCase = true) && habitToEdit!!.customRepeatUnit.isNotBlank()) {
                    habitToEdit!!.customRepeatUnit.lowercase()
                } else "days"
            ) 
        }

        var customSelectedDaysOfWeek by remember(habitToEdit) { 
            mutableStateOf(
                if (habitToEdit != null && !habitToEdit?.customRepeatDaysOfWeek.isNullOrBlank()) {
                    habitToEdit!!.customRepeatDaysOfWeek!!.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                } else setOf("Tue")
            ) 
        }

        var showCustomRepeatDialog by remember { mutableStateOf(false) }

        var goalType by remember(habitToEdit) { 
            mutableStateOf(
                if (habitToEdit != null && habitToEdit!!.isMultipleTimesPerDay) "Reach a certain amount" else "Achieve it all"
            ) 
        } // "Achieve it all", "Reach a certain amount"
        var goalAmount by remember(habitToEdit) { mutableIntStateOf(habitToEdit?.multipleTimesTarget ?: 1) }
        var startDate by remember(habitToEdit) { mutableStateOf(habitToEdit?.startDate ?: TrackWiseUtils.getTodayString()) }
        var goalDays by remember(habitToEdit) { mutableStateOf(habitToEdit?.endDate ?: "Forever") }

        var selectedFolders by remember(habitToEdit) { 
            mutableStateOf(
                if (!habitToEdit?.section.isNullOrBlank()) {
                    habitToEdit!!.section.split(",").toSet()
                } else setOf("Inbox")
            ) 
        }
        val tasks by viewModel.allTasks.collectAsState()
        val customFolders by viewModel.customFolders.collectAsState()
        val deletedFolders by viewModel.deletedFolders.collectAsState()
        val allFolders = remember(tasks, customFolders, deletedFolders) {
            val defaults = listOf("Inbox", "Work", "Personal", "Shopping", "Learning", "Wish List", "Fitness", "Welcome")
            val dynamic = tasks.map { it.project }.filter { it.isNotBlank() }
            (defaults + customFolders + dynamic)
                .distinct()
                .filter { folder -> !deletedFolders.any { it.equals(folder, ignoreCase = true) } }
        }

        var customTagInput by remember { mutableStateOf("") }

        var reminderTimes by remember(habitToEdit) { 
            mutableStateOf(
                if (habitToEdit != null && habitToEdit!!.remindMe && !habitToEdit!!.reminderTime.isNullOrBlank()) {
                    mutableListOf(habitToEdit!!.reminderTime!!)
                } else mutableListOf<String>()
            ) 
        }
        var autoPopupLog by remember { mutableStateOf(false) }

        // Dialogs inside step 2
        var showGoalDialog by remember { mutableStateOf(false) }
        var showDatePickerDialog by remember { mutableStateOf(false) }
        var showGoalDaysDialog by remember { mutableStateOf(false) }
        var showAddSectionDialog by remember { mutableStateOf(false) }
        var showAddReminderDialog by remember { mutableStateOf(false) }
        var editingReminderTime by remember { mutableStateOf<String?>(null) }

        var nameError by remember { mutableStateOf(false) }

        val primaryColor = MaterialTheme.colorScheme.primary
        val backgroundColor = MaterialTheme.colorScheme.background
        val surfaceColor = MaterialTheme.colorScheme.surface
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface
        val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
        val textColor = MaterialTheme.colorScheme.onBackground

        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.changedToDown() }) {
                                focusManager.clearFocus()
                            }
                        }
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                TopAppBar(
                    title = {
                        Text(
                            text = if (currentStep == 0) "Gallery" else if (habitToEdit != null) "Edit Habit" else "New Habit",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = textColor
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (currentStep == 0 || (currentStep == 1 && habitToEdit != null)) {
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
                                                    currentQuote = template.quote.ifBlank { MOTIVATIONAL_QUOTES.random() }
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
                                                    HabitIconView(icon = template.icon, tint = template.iconColor, size = 24.dp)
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
                                                    HabitIconView(icon = selectedIcon, tint = primaryColor, size = 26.dp)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            FlatColorIconPicker(
                                                selectedIcon = selectedIcon,
                                                onIconSelected = { selectedIcon = it },
                                                accentColor = primaryColor,
                                                modifier = Modifier.fillMaxWidth()
                                            )
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
                                                    text = "Motivational Quote 💡",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    color = textColor
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            currentQuote = MOTIVATIONAL_QUOTES.random()
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Refresh,
                                                            contentDescription = "Random Quote",
                                                            tint = primaryColor,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            OutlinedTextField(
                                                value = currentQuote,
                                                onValueChange = { currentQuote = it },
                                                placeholder = { Text("Select or type a motivational quote...", color = onSurfaceVariantColor, fontSize = 13.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                textStyle = TextStyle(fontSize = 13.sp, color = textColor),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = primaryColor,
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    focusedContainerColor = backgroundColor,
                                                    unfocusedContainerColor = backgroundColor
                                                )
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))

                                            /* Quotes ribbon removed */
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
                                    val parsedStart = remember(startDate) {
                                        try {
                                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            val date = sdf.parse(startDate)
                                            Calendar.getInstance().apply { if (date != null) time = date }
                                        } catch (e: Exception) {
                                            Calendar.getInstance()
                                        }
                                    }

                                    val dayOfWeekName = remember(parsedStart) {
                                        SimpleDateFormat("EEEE", Locale.getDefault()).format(parsedStart.time)
                                    }

                                    val dayOfMonthFormatted = remember(parsedStart) {
                                        val day = parsedStart.get(Calendar.DAY_OF_MONTH)
                                        val suffix = when {
                                            day in 11..13 -> "th"
                                            day % 10 == 1 -> "st"
                                            day % 10 == 2 -> "nd"
                                            day % 10 == 3 -> "rd"
                                            else -> "th"
                                        }
                                        "$day$suffix"
                                    }

                                    val monthAndDayFormatted = remember(parsedStart) {
                                        SimpleDateFormat("MMM d", Locale.getDefault()).format(parsedStart.time)
                                    }

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

                                            val customSub = if (selectedFrequencyOption == "CUSTOM") {
                                                when (customRepeatUnitState) {
                                                    "days" -> "Every $customRepeatValueState day${if (customRepeatValueState > 1) "s" else ""}"
                                                    "weeks" -> "Every $customRepeatValueState week${if (customRepeatValueState > 1) "s" else ""}" + (if (customSelectedDaysOfWeek.isNotEmpty()) " on ${customSelectedDaysOfWeek.joinToString(", ")}" else "")
                                                    "months" -> "Every $customRepeatValueState month${if (customRepeatValueState > 1) "s" else ""}"
                                                    "years" -> "Every $customRepeatValueState year${if (customRepeatValueState > 1) "s" else ""}"
                                                    else -> "Custom repeat schedule"
                                                }
                                            } else "Custom repeat schedule..."

                                            val options = listOf(
                                                "DAILY" to ("Daily" to "Every day"),
                                                "WEEKDAYS" to ("Weekdays" to "Saturday and Sunday off"),
                                                "WEEKLY" to ("Weekly" to "Same day of every week ($dayOfWeekName)"),
                                                "MONTHLY" to ("Monthly" to "Same date of every month ($dayOfMonthFormatted)"),
                                                "YEARLY" to ("Yearly" to "Same date of every year ($monthAndDayFormatted)"),
                                                "CUSTOM" to ("Custom" to customSub)
                                            )

                                            options.forEach { (optionKey, titleSubPair) ->
                                                val (optionTitle, optionSubtitle) = titleSubPair
                                                val isSelected = selectedFrequencyOption == optionKey

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            if (optionKey == "CUSTOM") {
                                                                selectedFrequencyOption = "CUSTOM"
                                                                showCustomRepeatDialog = true
                                                            } else {
                                                                selectedFrequencyOption = optionKey
                                                            }
                                                        }
                                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                                ) {
                                                    RadioButton(
                                                        selected = isSelected,
                                                        onClick = {
                                                            if (optionKey == "CUSTOM") {
                                                                selectedFrequencyOption = "CUSTOM"
                                                                showCustomRepeatDialog = true
                                                            } else {
                                                                selectedFrequencyOption = optionKey
                                                            }
                                                        },
                                                        colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = optionTitle,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = textColor
                                                        )
                                                        Text(
                                                            text = optionSubtitle,
                                                            fontSize = 12.sp,
                                                            color = onSurfaceVariantColor
                                                        )
                                                    }
                                                    if (optionKey == "CUSTOM" && isSelected) {
                                                        IconButton(
                                                            onClick = { showCustomRepeatDialog = true },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Edit,
                                                                contentDescription = "Edit Custom Frequency",
                                                                tint = primaryColor,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
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
                                                            onClick = {
                                                                editingReminderTime = time
                                                                showAddReminderDialog = true
                                                            },
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
                                    val repeatTypeVal = when (selectedFrequencyOption) {
                                        "DAILY" -> "daily"
                                        "WEEKDAYS" -> "weekdays"
                                        "WEEKLY" -> "weekly"
                                        "MONTHLY" -> "monthly"
                                        "YEARLY" -> "yearly"
                                        "CUSTOM" -> "custom"
                                        else -> "daily"
                                    }

                                    val customDays = when (selectedFrequencyOption) {
                                        "WEEKDAYS" -> "Mon,Tue,Wed,Thu,Fri"
                                        "CUSTOM" -> {
                                            if (customRepeatUnitState == "weeks" && customSelectedDaysOfWeek.isNotEmpty()) {
                                                customSelectedDaysOfWeek.joinToString(",")
                                            } else {
                                                null
                                            }
                                        }
                                        else -> null
                                    }

                                    val customVal = when (selectedFrequencyOption) {
                                        "CUSTOM" -> customRepeatValueState
                                        else -> 1
                                    }

                                    val customUnit = when (selectedFrequencyOption) {
                                        "CUSTOM" -> customRepeatUnitState
                                        "WEEKLY" -> "weeks"
                                        "MONTHLY" -> "months"
                                        "YEARLY" -> "years"
                                        else -> "days"
                                    }

                                    if (habitToEdit != null) {
                                        val updated = habitToEdit!!.copy(
                                            name = habitName,
                                            category = habitCategory,
                                            isMultipleTimesPerDay = (goalType == "Reach a certain amount"),
                                            multipleTimesTarget = if (goalType == "Reach a certain amount") goalAmount else 1,
                                            repeatType = repeatTypeVal,
                                            customRepeatValue = customVal,
                                            customRepeatUnit = customUnit,
                                            customRepeatDaysOfWeek = customDays,
                                            startDate = startDate,
                                            remindMe = reminderTimes.isNotEmpty(),
                                            reminderTime = reminderTimes.firstOrNull(),
                                            icon = selectedIcon,
                                            quote = currentQuote,
                                            goalType = goalType,
                                            goalDays = goalDays,
                                            section = if (selectedFolders.isEmpty()) "Inbox" else {
                                                val validFolders = selectedFolders.filter { it.isNotBlank() }
                                                val cleanedFolders = if (validFolders.size > 1) validFolders.filter { !it.equals("Others", ignoreCase = true) } else validFolders
                                                if (cleanedFolders.isEmpty()) "Inbox" else cleanedFolders.joinToString(",")
                                            }
                                        )
                                        viewModel.updateHabit(updated)
                                    } else {
                                        viewModel.addHabit(
                                            name = habitName,
                                            category = habitCategory,
                                            isMultipleTimesPerDay = (goalType == "Reach a certain amount"),
                                            multipleTimesTarget = if (goalType == "Reach a certain amount") goalAmount else 1,
                                            repeatType = repeatTypeVal,
                                            customRepeatValue = customVal,
                                            customRepeatUnit = customUnit,
                                            customRepeatDaysOfWeek = customDays,
                                            startDate = startDate,
                                            remindMe = reminderTimes.isNotEmpty(),
                                            reminderTime = reminderTimes.firstOrNull(),
                                            icon = selectedIcon,
                                            quote = currentQuote,
                                            goalType = goalType,
                                            goalDays = goalDays,
                                            section = if (selectedFolders.isEmpty()) "Inbox" else {
                                                val validFolders = selectedFolders.filter { it.isNotBlank() }
                                                val cleanedFolders = if (validFolders.size > 1) validFolders.filter { !it.equals("Others", ignoreCase = true) } else validFolders
                                                if (cleanedFolders.isEmpty()) "Inbox" else cleanedFolders.joinToString(",")
                                            },
                                            autoPopup = false
                                        )
                                    }

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
                                else -> if (habitToEdit != null) "Save Changes" else "Save"
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

            val monthCal = remember(selYear, selMonth) {
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, selYear)
                    set(Calendar.MONTH, selMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
            }
            val firstDayOfWeek = remember(monthCal) {
                monthCal.get(Calendar.DAY_OF_WEEK) - 1 // 0-based Sun=0..Sat=6
            }
            val daysInMonth = remember(monthCal) {
                monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            }

            AlertDialog(
                onDismissRequest = { showDatePickerDialog = false },
                title = { Text("Select Start Date", fontWeight = FontWeight.Bold) },
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
                                .height(240.dp)
                        ) {
                            items(listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")) { h ->
                                Text(h, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                            }
                            // Leading blank cells for first day of week alignment
                            items(firstDayOfWeek) {
                                Box(modifier = Modifier.size(32.dp))
                            }
                            items(daysInMonth) { dayIdx ->
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
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
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
                            val finalDay = selDay.coerceAtMost(daysInMonth)
                            val monthStr = (selMonth + 1).toString().padStart(2, '0')
                            val dayStr = finalDay.toString().padStart(2, '0')
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

        if (showQuotesPickerDialog) {
            AlertDialog(
                onDismissRequest = { showQuotesPickerDialog = false },
                title = {
                    Text(
                        text = "100 Motivational Quotes 💡",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = primaryColor
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                    ) {
                        OutlinedTextField(
                            value = quotesSearchQuery,
                            onValueChange = { quotesSearchQuery = it },
                            placeholder = { Text("Search quotes...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = primaryColor) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp)
                        )

                        val filteredQuotes = remember(quotesSearchQuery) {
                            if (quotesSearchQuery.isBlank()) MOTIVATIONAL_QUOTES
                            else MOTIVATIONAL_QUOTES.filter { it.contains(quotesSearchQuery, ignoreCase = true) }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(filteredQuotes) { quote ->
                                val isSelected = currentQuote == quote
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) primaryColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = if (isSelected) BorderStroke(1.dp, primaryColor) else null,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentQuote = quote
                                            showQuotesPickerDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FormatQuote,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = quote,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQuotesPickerDialog = false }) {
                        Text("Close", color = primaryColor, fontWeight = FontWeight.Bold)
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

        // Add/Edit Reminder Dialog using Native Clock
        if (showAddReminderDialog) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val calendar = java.util.Calendar.getInstance()
            val isDarkTheme = MaterialTheme.colorScheme.background.let { (it.red + it.green + it.blue) / 3f < 0.5f }
            val themeId = if (isDarkTheme) android.R.style.Theme_DeviceDefault_Dialog_Alert else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert
            
            var initialHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            var initialMinute = calendar.get(java.util.Calendar.MINUTE)
            if (editingReminderTime != null) {
                val parsed24 = com.example.receiver.ReminderReceiver.parseTo24HourTime(editingReminderTime)
                if (parsed24 != null && parsed24.contains(":")) {
                    val parts = parsed24.split(":")
                    initialHour = parts[0].toIntOrNull() ?: initialHour
                    initialMinute = parts[1].toIntOrNull() ?: initialMinute
                }
            }

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
                        if (editingReminderTime != null) {
                            val targetOld = editingReminderTime
                            reminderTimes = reminderTimes.toMutableList().apply {
                                val idx = indexOf(targetOld)
                                if (idx >= 0) {
                                    set(idx, formattedTime)
                                } else if (!contains(formattedTime)) {
                                    add(formattedTime)
                                }
                            }
                            editingReminderTime = null
                        } else {
                            reminderTimes = reminderTimes.toMutableList().apply { if (!contains(formattedTime)) add(formattedTime) }
                        }
                        showAddReminderDialog = false
                    },
                    initialHour,
                    initialMinute,
                    false // Use 12h clock
                )
                tpd.setOnCancelListener {
                    editingReminderTime = null
                    showAddReminderDialog = false
                }
                tpd.show()
                onDispose {
                    tpd.dismiss()
                }
            }
        }

        // Custom Repeat Dialog ("Repeat every ...")
        if (showCustomRepeatDialog) {
            var tempValueStr by remember { mutableStateOf(customRepeatValueState.toString()) }
            var tempUnit by remember { mutableStateOf(customRepeatUnitState) }
            var tempDays by remember { mutableStateOf(customSelectedDaysOfWeek) }
            var unitDropdownExpanded by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showCustomRepeatDialog = false },
                title = {
                    Text(
                        text = "Repeat every ...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textColor
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Underlined Text Field for Number Input
                            BasicTextField(
                                value = tempValueStr,
                                onValueChange = { newValue ->
                                    if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                                        tempValueStr = newValue
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    textAlign = TextAlign.Center
                                ),
                                singleLine = true,
                                modifier = Modifier.width(44.dp)
                            ) { innerTextField ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    innerTextField()
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .background(primaryColor)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Dropdown for Unit selection ("days", "weeks", "months", "years")
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.clickable { unitDropdownExpanded = true }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = tempUnit,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = textColor
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = "Select unit",
                                            tint = onSurfaceVariantColor
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = unitDropdownExpanded,
                                    onDismissRequest = { unitDropdownExpanded = false }
                                ) {
                                    listOf("days", "weeks", "months", "years").forEach { unitOpt ->
                                        DropdownMenuItem(
                                            text = { Text(unitOpt, fontSize = 15.sp) },
                                            onClick = {
                                                tempUnit = unitOpt
                                                unitDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // If "weeks" is selected: show circular day buttons for SUN, MON, TUE, WED, THU, FRI, SAT
                        if (tempUnit.lowercase() == "weeks") {
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val daysList = listOf(
                                    "Sun" to "SUN",
                                    "Mon" to "MON",
                                    "Tue" to "TUE",
                                    "Wed" to "WED",
                                    "Thu" to "THU",
                                    "Fri" to "FRI",
                                    "Sat" to "SAT"
                                )
                                daysList.forEach { (dayKey, dayLabel) ->
                                    val isSelected = tempDays.contains(dayKey)
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) primaryColor else Color.Transparent)
                                            .border(
                                                width = if (isSelected) 0.dp else 1.dp,
                                                color = if (isSelected) Color.Transparent else Color.LightGray,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                val newSet = tempDays.toMutableSet()
                                                if (isSelected) newSet.remove(dayKey)
                                                else newSet.add(dayKey)
                                                tempDays = newSet
                                            }
                                    ) {
                                        Text(
                                            text = dayLabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else onSurfaceVariantColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val parsed = tempValueStr.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            customRepeatValueState = parsed
                            customRepeatUnitState = tempUnit.lowercase()
                            customSelectedDaysOfWeek = tempDays
                            selectedFrequencyOption = "CUSTOM"
                            showCustomRepeatDialog = false
                        }
                    ) {
                        Text("DONE", fontWeight = FontWeight.Bold, color = primaryColor)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCustomRepeatDialog = false }
                    ) {
                        Text("CANCEL", fontWeight = FontWeight.Bold, color = onSurfaceVariantColor)
                    }
                }
            )
        }
    }
}
