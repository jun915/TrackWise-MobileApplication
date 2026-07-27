package com.example.ui

import android.content.Intent
import android.widget.Toast
import com.example.utils.TrackWiseUtils
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HabitEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habitId: String,
    viewModel: TrackWiseViewModel,
    onBack: () -> Unit,
    onEditHabit: (HabitEntity) -> Unit
) {
    val context = LocalContext.current
    val habits by viewModel.allHabits.collectAsState()
    val habit = habits.find { it.id == habitId }

    if (habit == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFF9E59)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Habit not found", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Go Back") }
            }
        }
        return
    }

    val todayStr = remember { TrackWiseUtils.getTodayString() }
    val daysCompleted = remember(habit.daysCompletedJson) {
        TrackWiseUtils.deserializeStringList(habit.daysCompletedJson)
    }
    val isCompletedToday = daysCompleted.contains(todayStr)

    var showMenu by remember { mutableStateOf(false) }
    var isExpandedSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBgSelectorDialog by remember { mutableStateOf(false) }
    var habitLogInput by remember { mutableStateOf("") }

    // Dynamic background color based on category/habit/backgroundImage
    val themeBgColor = remember(habit.backgroundImage, habit.category) {
        when (habit.backgroundImage.lowercase()) {
            "fitness" -> Color(0xFFFF8A3D)
            "mindfulness" -> Color(0xFF4F46E5)
            "study" -> Color(0xFF6366F1)
            "finance" -> Color(0xFF0F766E)
            else -> {
                when (habit.category.lowercase()) {
                    "health", "fitness" -> Color(0xFF4ADE80)
                    "learning", "mindset" -> Color(0xFF38BDF8)
                    "life", "productivity" -> Color(0xFFA855F7)
                    else -> Color(0xFFFF9E59) // Coral/Orange standard
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Habit", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${habit.name}'? This operation cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteHabit(habit.id)
                    onBack()
                }) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBgSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showBgSelectorDialog = false },
            title = { Text("Choose Background Illustration", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Select a visual theme for this habit's detail page and widget cover.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    val options = listOf(
                        Triple("window", "Sunrise Window", Color(0xFF67E8F9)),
                        Triple("fitness", "Running Track", Color(0xFFFDBA74)),
                        Triple("mindfulness", "Serene Lotus", Color(0xFF818CF8)),
                        Triple("study", "Study Desk", Color(0xFFFCA5A5)),
                        Triple("finance", "Wealth Growth", Color(0xFF2DD4BF))
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        options.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { (id, label, color) ->
                                    val isSelected = habit.backgroundImage == id
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) color.copy(alpha = 0.2f) else Color(0xFFF8FAFC)
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) color else Color.LightGray.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(110.dp)
                                            .clickable {
                                                viewModel.updateHabit(habit.copy(backgroundImage = id))
                                                showBgSelectorDialog = false
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .padding(2.dp)
                                            ) {
                                                HabitBackgroundIllustration(
                                                    imageName = id,
                                                    isAchieved = true,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black,
                                                maxLines = 1,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                                if (pair.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBgSelectorDialog = false }) {
                    Text("Close", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeBgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("habit_detail_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isExpandedSheet) {
                    Text(
                        text = habit.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("habit_detail_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .width(160.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit", color = Color.Black, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.DarkGray) },
                            onClick = {
                                showMenu = false
                                onEditHabit(habit)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Change Background Image", color = Color.Black, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = Color.DarkGray) },
                            onClick = {
                                showMenu = false
                                showBgSelectorDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share", color = Color.Black, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color.DarkGray) },
                            onClick = {
                                showMenu = false
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "I am tracking '${habit.name}' on TrackWise! Current Streak: ${habit.streak} days.")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, null))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Archive", color = Color.Black, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, tint = Color.DarkGray) },
                            onClick = {
                                showMenu = false
                                Toast.makeText(context, "Habit archived", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }

            if (!isExpandedSheet) {
                // MAIN COVER / SLIDER VIEW (Screenshots 1 & 4)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // ARTWORK WINDOW ILLUSTRATION WITH "ACHIEVED" BADGE
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HabitBackgroundIllustration(
                            imageName = habit.backgroundImage,
                            isAchieved = isCompletedToday,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Achieved Badge Stamp on Top Right
                        if (isCompletedToday) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 10.dp, y = 10.dp)
                                    .rotate(15f)
                                    .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Achieved",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // HABIT TITLE & QUOTE
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = habit.name,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = habit.quote.ifBlank { "Get up and be amazing" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // SLIDER OR COMPLETED STATS CARD
                    if (isCompletedToday) {
                        // SCREENSHOT 4: Stats Card after Check-In
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    StatMetricItem(number = "${daysCompleted.size}", label = "Total check-ins")
                                    StatMetricItem(number = "${habit.streak.coerceAtLeast(1)}", label = "Best Streak")
                                    StatMetricItem(number = "${habit.streak}", label = "Streak")
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "Completed '${habit.name}' today! 🎉 Streak: ${habit.streak} days.")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, null))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeBgColor),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text("Share", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    } else {
                        // SCREENSHOT 1: Interactive Check Slider Track
                        HabitCheckInSlider(
                            onComplete = {
                                viewModel.toggleHabitToday(habit)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    }

                    // CHEVRON EXPAND ARROW
                    Box(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { isExpandedSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Expand Details",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            } else {
                // EXPANDED DETAILS SHEET (Screenshot 3)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        // COLLAPSE BUTTON CHEVRON
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { isExpandedSheet = false }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Collapse",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    // CARD 1: MONTH CALENDAR VIEW
                    item {
                        HabitMonthCalendarCard(daysCompleted = daysCompleted)
                    }

                    // CARD 2: CHECK-INS STATISTICS CARD
                    item {
                        HabitCheckInStatisticsCard(habit = habit, daysCompleted = daysCompleted)
                    }

                    // CARD 3: HABIT LOG ON MONTH
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Habit Log on ${SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No check-in thoughts to share this month yet",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = habitLogInput,
                                        onValueChange = { habitLogInput = it },
                                        placeholder = { Text("Write a check-in thought...", fontSize = 12.sp) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (habitLogInput.isNotBlank()) {
                                                Toast.makeText(context, "Log saved!", Toast.LENGTH_SHORT).show()
                                                habitLogInput = ""
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = themeBgColor)
                                    ) {
                                        Text("Save", fontSize = 12.sp)
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

@Composable
fun StatMetricItem(number: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = number,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}

// SLIDER COMPONENT (Screenshot 1 & 4)
@Composable
fun HabitCheckInSlider(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val maxOffsetPx = 540f // approx track width travel

    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "slider")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(Color.White.copy(alpha = 0.25f))
            .padding(6.dp)
            .clickable {
                // Click also toggles complete
                onComplete()
            }
    ) {
        // Track hint text
        Text(
            text = "Slide to check in  >",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )

        // White circular draggable handle button
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newOffset = (offsetX + delta).coerceIn(0f, maxOffsetPx)
                        offsetX = newOffset
                        if (offsetX >= maxOffsetPx - 20f) {
                            onComplete()
                            offsetX = 0f
                        }
                    },
                    onDragStopped = {
                        if (offsetX < maxOffsetPx / 2) {
                            offsetX = 0f
                        } else {
                            onComplete()
                            offsetX = 0f
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Check in",
                tint = Color(0xFFFF8A3D),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// GENERIC HABIT BACKGROUND ILLUSTRATION (Multiple Beautiful Themes drawn with Canvas)
@Composable
fun HabitBackgroundIllustration(imageName: String, isAchieved: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (imageName.lowercase()) {
            "fitness" -> {
                val archPath = Path().apply {
                    moveTo(w * 0.2f, h * 0.9f)
                    lineTo(w * 0.2f, h * 0.4f)
                    cubicTo(w * 0.2f, h * 0.1f, w * 0.8f, h * 0.1f, w * 0.8f, h * 0.4f)
                    lineTo(w * 0.8f, h * 0.9f)
                    close()
                }
                drawPath(path = archPath, color = Color(0xFFFDBA74)) // Warm orange sky
                
                // Track path
                val trackPath = Path().apply {
                    moveTo(w * 0.3f, h * 0.9f)
                    cubicTo(w * 0.35f, h * 0.65f, w * 0.45f, h * 0.5f, w * 0.5f, h * 0.5f)
                    cubicTo(w * 0.55f, h * 0.5f, w * 0.65f, h * 0.65f, w * 0.7f, h * 0.9f)
                    close()
                }
                drawPath(path = trackPath, color = Color(0xFFEF4444)) // Red running track
                
                // Lane line
                val laneLine = Path().apply {
                    moveTo(w * 0.5f, h * 0.9f)
                    lineTo(w * 0.5f, h * 0.5f)
                }
                drawPath(path = laneLine, color = Color.White, style = Stroke(width = 3.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)))

                // Sun
                drawCircle(color = Color(0xFFFEF08A), radius = w * 0.15f, center = Offset(w * 0.5f, h * 0.35f))

                // Frame
                drawPath(path = archPath, color = Color.White, style = Stroke(width = 8.dp.toPx()))

                // Finish flag if achieved
                if (isAchieved) {
                    val flagPole = Path().apply {
                        moveTo(w * 0.5f, h * 0.75f)
                        lineTo(w * 0.5f, h * 0.45f)
                    }
                    drawPath(path = flagPole, color = Color(0xFF475569), style = Stroke(width = 4.dp.toPx()))
                    val flag = Path().apply {
                        moveTo(w * 0.5f, h * 0.45f)
                        lineTo(w * 0.68f, h * 0.52f)
                        lineTo(w * 0.5f, h * 0.59f)
                        close()
                    }
                    drawPath(path = flag, color = Color(0xFF10B981))
                } else {
                    drawCircle(color = Color.White, radius = 8f, center = Offset(w * 0.35f, h * 0.25f))
                    drawCircle(color = Color.White, radius = 6f, center = Offset(w * 0.65f, h * 0.28f))
                }
            }
            "mindfulness" -> {
                val archPath = Path().apply {
                    moveTo(w * 0.2f, h * 0.9f)
                    lineTo(w * 0.2f, h * 0.4f)
                    cubicTo(w * 0.2f, h * 0.1f, w * 0.8f, h * 0.1f, w * 0.8f, h * 0.4f)
                    lineTo(w * 0.8f, h * 0.9f)
                    close()
                }
                drawPath(path = archPath, color = Color(0xFF4338CA)) // Deep Indigo

                // Moon
                drawCircle(color = Color(0xFFFEF08A), radius = w * 0.14f, center = Offset(w * 0.4f, h * 0.32f))
                drawCircle(color = Color(0xFF4338CA), radius = w * 0.14f, center = Offset(w * 0.46f, h * 0.30f)) // Mask for crescent

                // Calm Water Waves
                drawArc(color = Color(0xFF818CF8).copy(alpha = 0.4f), startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.25f, h * 0.72f), size = Size(w * 0.5f, h * 0.1f), style = Stroke(width = 3.dp.toPx()))
                drawArc(color = Color(0xFF818CF8).copy(alpha = 0.4f), startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.2f, h * 0.8f), size = Size(w * 0.6f, h * 0.1f), style = Stroke(width = 3.dp.toPx()))

                // Frame
                drawPath(path = archPath, color = Color.White, style = Stroke(width = 8.dp.toPx()))

                // Lotus flower/Moon reflection
                val lotusPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.82f)
                    cubicTo(w * 0.42f, h * 0.72f, w * 0.45f, h * 0.65f, w * 0.5f, h * 0.65f)
                    cubicTo(w * 0.55f, h * 0.65f, w * 0.58f, h * 0.72f, w * 0.5f, h * 0.82f)
                }
                drawPath(path = lotusPath, color = Color(0xFFF472B6)) // Central Petal

                val leftPetal = Path().apply {
                    moveTo(w * 0.5f, h * 0.82f)
                    cubicTo(w * 0.35f, h * 0.75f, w * 0.38f, h * 0.68f, w * 0.44f, h * 0.72f)
                    cubicTo(w * 0.48f, h * 0.76f, w * 0.49f, h * 0.8f, w * 0.5f, h * 0.82f)
                }
                drawPath(path = leftPetal, color = Color(0xFFEC4899))

                val rightPetal = Path().apply {
                    moveTo(w * 0.5f, h * 0.82f)
                    cubicTo(w * 0.65f, h * 0.75f, w * 0.62f, h * 0.68f, w * 0.56f, h * 0.72f)
                    cubicTo(w * 0.52f, h * 0.76f, w * 0.51f, h * 0.8f, w * 0.5f, h * 0.82f)
                }
                drawPath(path = rightPetal, color = Color(0xFFEC4899))

                if (isAchieved) {
                    drawCircle(color = Color.White, radius = 10f, center = Offset(w * 0.5f, h * 0.54f))
                    drawCircle(color = Color.White, radius = 6f, center = Offset(w * 0.32f, h * 0.58f))
                    drawCircle(color = Color.White, radius = 6f, center = Offset(w * 0.68f, h * 0.58f))
                    drawCircle(color = Color.White, radius = 4f, center = Offset(w * 0.44f, h * 0.50f))
                    drawCircle(color = Color.White, radius = 4f, center = Offset(w * 0.56f, h * 0.50f))
                }
            }
            "study" -> {
                val archPath = Path().apply {
                    moveTo(w * 0.2f, h * 0.9f)
                    lineTo(w * 0.2f, h * 0.4f)
                    cubicTo(w * 0.2f, h * 0.1f, w * 0.8f, h * 0.1f, w * 0.8f, h * 0.4f)
                    lineTo(w * 0.8f, h * 0.9f)
                    close()
                }
                drawPath(path = archPath, color = Color(0xFF818CF8)) // Cozy Light Violet

                // Lamp shine cone if achieved
                if (isAchieved) {
                    val shinePath = Path().apply {
                        moveTo(w * 0.5f, h * 0.32f)
                        lineTo(w * 0.28f, h * 0.82f)
                        lineTo(w * 0.72f, h * 0.82f)
                        close()
                    }
                    drawPath(path = shinePath, color = Color(0xFFFEF08A).copy(alpha = 0.5f))
                }

                // Desk Line
                drawLine(color = Color.White, start = Offset(w * 0.2f, h * 0.82f), end = Offset(w * 0.8f, h * 0.82f), strokeWidth = 5.dp.toPx())

                // Open Book
                val leftPage = Path().apply {
                    moveTo(w * 0.5f, h * 0.82f)
                    cubicTo(w * 0.45f, h * 0.74f, w * 0.36f, h * 0.74f, w * 0.32f, h * 0.77f)
                    lineTo(w * 0.32f, h * 0.82f)
                    cubicTo(w * 0.36f, h * 0.79f, w * 0.45f, h * 0.79f, w * 0.5f, h * 0.84f)
                    close()
                }
                drawPath(path = leftPage, color = Color.White)

                val rightPage = Path().apply {
                    moveTo(w * 0.5f, h * 0.82f)
                    cubicTo(w * 0.55f, h * 0.74f, w * 0.64f, h * 0.74f, w * 0.68f, h * 0.77f)
                    lineTo(w * 0.68f, h * 0.82f)
                    cubicTo(w * 0.64f, h * 0.79f, w * 0.55f, h * 0.79f, w * 0.5f, h * 0.84f)
                    close()
                }
                drawPath(path = rightPage, color = Color(0xFFF1F5F9))

                // Lamp
                val lampArm = Path().apply {
                    moveTo(w * 0.68f, h * 0.82f)
                    cubicTo(w * 0.74f, h * 0.55f, w * 0.70f, h * 0.35f, w * 0.54f, h * 0.32f)
                }
                drawPath(path = lampArm, color = Color(0xFF334155), style = Stroke(width = 4.dp.toPx()))
                drawCircle(color = Color(0xFFF43F5E), radius = 18f, center = Offset(w * 0.52f, h * 0.32f))

                // Frame
                drawPath(path = archPath, color = Color.White, style = Stroke(width = 8.dp.toPx()))

                if (isAchieved) {
                    drawCircle(color = Color(0xFFFACC15), radius = 8f, center = Offset(w * 0.38f, h * 0.62f))
                    drawCircle(color = Color(0xFFFACC15), radius = 6f, center = Offset(w * 0.62f, h * 0.60f))
                    drawCircle(color = Color(0xFFFACC15), radius = 4f, center = Offset(w * 0.48f, h * 0.54f))
                }
            }
            "finance" -> {
                val archPath = Path().apply {
                    moveTo(w * 0.2f, h * 0.9f)
                    lineTo(w * 0.2f, h * 0.4f)
                    cubicTo(w * 0.2f, h * 0.1f, w * 0.8f, h * 0.1f, w * 0.8f, h * 0.4f)
                    lineTo(w * 0.8f, h * 0.9f)
                    close()
                }
                drawPath(path = archPath, color = Color(0xFF14B8A6)) // Vibrant Teal

                // Pot
                val potPath = Path().apply {
                    moveTo(w * 0.4f, h * 0.86f)
                    lineTo(w * 0.44f, h * 0.74f)
                    lineTo(w * 0.56f, h * 0.74f)
                    lineTo(w * 0.60f, h * 0.86f)
                    close()
                }
                drawPath(path = potPath, color = Color(0xFFD97706)) // Terracotta Orange

                // Plant Stem
                val stemPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.74f)
                    cubicTo(w * 0.48f, h * 0.60f, w * 0.52f, h * 0.45f, w * 0.5f, h * 0.40f)
                }
                drawPath(path = stemPath, color = Color(0xFF047857), style = Stroke(width = 4.dp.toPx()))

                // Plant Leaves
                drawCircle(color = Color(0xFF10B981), radius = 22f, center = Offset(w * 0.44f, h * 0.65f))
                drawCircle(color = Color(0xFF10B981), radius = 20f, center = Offset(w * 0.58f, h * 0.58f))
                drawCircle(color = Color(0xFF10B981), radius = 18f, center = Offset(w * 0.45f, h * 0.48f))
                drawCircle(color = Color(0xFF34D399), radius = 14f, center = Offset(w * 0.5f, h * 0.38f))

                // Frame
                drawPath(path = archPath, color = Color.White, style = Stroke(width = 8.dp.toPx()))

                // Coins
                if (isAchieved) {
                    drawCircle(color = Color(0xFFFACC15), radius = 14f, center = Offset(w * 0.32f, h * 0.82f))
                    drawCircle(color = Color(0xFFFACC15), radius = 14f, center = Offset(w * 0.68f, h * 0.80f))
                    drawCircle(color = Color(0xFFFACC15), radius = 14f, center = Offset(w * 0.5f, h * 0.30f)) // Floating fruit coin!
                } else {
                    drawCircle(color = Color(0xFFFEF08A), radius = 10f, center = Offset(w * 0.34f, h * 0.84f))
                    drawCircle(color = Color(0xFFFEF08A), radius = 10f, center = Offset(w * 0.66f, h * 0.84f))
                }
            }
            else -> {
                val archPath = Path().apply {
                    moveTo(w * 0.2f, h * 0.9f)
                    lineTo(w * 0.2f, h * 0.4f)
                    cubicTo(w * 0.2f, h * 0.1f, w * 0.8f, h * 0.1f, w * 0.8f, h * 0.4f)
                    lineTo(w * 0.8f, h * 0.9f)
                    close()
                }

                // Blue sky inside window
                drawPath(path = archPath, color = Color(0xFF67E8F9))

                // Sun inside window
                drawCircle(
                    color = Color(0xFFFACC15),
                    radius = w * 0.22f,
                    center = Offset(w * 0.62f, h * 0.5f)
                )

                // Window Frame Lines
                drawPath(
                    path = archPath,
                    color = Color.White,
                    style = Stroke(width = 8.dp.toPx())
                )
                // Crossbars
                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.5f, h * 0.18f),
                    end = Offset(w * 0.5f, h * 0.9f),
                    strokeWidth = 6.dp.toPx()
                )
                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.2f, h * 0.35f),
                    end = Offset(w * 0.8f, h * 0.35f),
                    strokeWidth = 6.dp.toPx()
                )

                // Window Sill Ledge
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.15f, h * 0.88f),
                    size = Size(w * 0.7f, h * 0.08f),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Plant pot on sill
                val potPath = Path().apply {
                    moveTo(w * 0.28f, h * 0.88f)
                    lineTo(w * 0.32f, h * 0.78f)
                    lineTo(w * 0.42f, h * 0.78f)
                    lineTo(w * 0.45f, h * 0.88f)
                    close()
                }
                drawPath(path = potPath, color = Color(0xFFFB923C))

                // Plant Leaves
                drawCircle(color = Color(0xFF10B981), radius = 18f, center = Offset(w * 0.35f, h * 0.72f))
                drawCircle(color = Color(0xFF10B981), radius = 22f, center = Offset(w * 0.32f, h * 0.66f))
                drawCircle(color = Color(0xFF10B981), radius = 20f, center = Offset(w * 0.39f, h * 0.64f))

                // Bird on sill if achieved
                if (isAchieved) {
                    drawCircle(color = Color(0xFF334155), radius = 14f, center = Offset(w * 0.55f, h * 0.84f))
                    drawCircle(color = Color(0xFF334155), radius = 10f, center = Offset(w * 0.52f, h * 0.82f))
                }
            }
        }
    }
}

// MONTH CALENDAR CARD (Screenshot 3)
@Composable
fun HabitMonthCalendarCard(daysCompleted: List<String>) {
    var calendarMonthOffset by remember { mutableIntStateOf(0) }
    val calendar = remember(calendarMonthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, calendarMonthOffset)
        }
    }

    val monthName = remember(calendar) {
        SimpleDateFormat("MMMM YYYY", Locale.getDefault()).format(calendar.time)
    }

    val daysInMonth = remember(calendar) {
        calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val firstDayOfWeek = remember(calendar) {
        val temp = calendar.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, 1)
        temp.get(Calendar.DAY_OF_WEEK) - 1 // 0-based Sun..Sat
    }

    val currentYearMonthPrefix = remember(calendar) {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Month Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { calendarMonthOffset-- },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Color.Black)
                }

                Text(
                    text = monthName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )

                IconButton(
                    onClick = { calendarMonthOffset++ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day of Week Headers
            val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid
            val totalGridCells = firstDayOfWeek + daysInMonth
            val rows = (totalGridCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (c in 0 until 7) {
                            val cellIndex = r * 7 + c
                            val dayNum = cellIndex - firstDayOfWeek + 1
                            if (dayNum in 1..daysInMonth) {
                                val dayStr = String.format(Locale.US, "%s-%02d", currentYearMonthPrefix, dayNum)
                                val isCheckedIn = daysCompleted.contains(dayStr)

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCheckedIn) Color(0xFF38BDF8) else Color(0xFFF1F5F9)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNum",
                                        fontSize = 13.sp,
                                        fontWeight = if (isCheckedIn) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCheckedIn) Color.White else Color.Black
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// CHECK-INS STATISTICS CARD (Screenshot 3)
@Composable
fun HabitCheckInStatisticsCard(habit: HabitEntity, daysCompleted: List<String>) {
    val totalCheckIns = daysCompleted.size
    val currentMonthPrefix = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }
    val monthlyCheckIns = daysCompleted.count { it.startsWith(currentMonthPrefix) }
    val daysInCurrentMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthlyRate = if (daysInCurrentMonth > 0) ((monthlyCheckIns.toFloat() / daysInCurrentMonth) * 100).toInt() else 0

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Check-ins Statistics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    text = "More >",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBoxItem(
                        icon = Icons.Default.CheckCircle,
                        iconTint = Color(0xFF10B981),
                        label = "Monthly check-ins",
                        value = "$monthlyCheckIns Days",
                        modifier = Modifier.weight(1f)
                    )
                    StatBoxItem(
                        icon = Icons.Default.EventAvailable,
                        iconTint = Color(0xFF0284C7),
                        label = "Total check-ins",
                        value = "$totalCheckIns Days",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBoxItem(
                        icon = Icons.Default.PieChart,
                        iconTint = Color(0xFFF59E0B),
                        label = "Monthly check-in rate",
                        value = "$monthlyRate %",
                        modifier = Modifier.weight(1f)
                    )
                    StatBoxItem(
                        icon = Icons.Default.ShowChart,
                        iconTint = Color(0xFF6366F1),
                        label = "Best Streak",
                        value = "${habit.streak} Days",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatBoxItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}
