@file:OptIn(ExperimentalFoundationApi::class)

package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.utils.TrackWiseUtils
import kotlin.math.pow

import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun HealthLogItemOptionsDialog(
    title: String,
    isPinned: Boolean = false,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit = {},
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
                onDismiss()
            },
            title = { Text("Confirm Delete", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this entry ($title)? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                    onDismiss()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDismiss()
                }) { Text("Cancel") }
            }
        )
    }

    DropdownMenu(
        expanded = !showDeleteConfirm,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Edit Details") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = {
                onDismiss()
                onEdit()
            }
        )
        DropdownMenuItem(
            text = { Text(if (isPinned) "Unpin from Top" else "Pin to Top") },
            leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, tint = BrandAmber) },
            onClick = {
                onTogglePin()
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Delete Entry", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = {
                showDeleteConfirm = true
            }
        )
    }
}

@Composable
fun HealthScreen(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.sessionUser.collectAsState()
    val waterLogs by viewModel.waterLogs.collectAsState()
    val weightEntries by viewModel.weightEntries.collectAsState()
    val vitalReadings by viewModel.vitalReadings.collectAsState()
    val exerciseLogs by viewModel.exerciseLogs.collectAsState()
    val healthIssueLogs by viewModel.healthIssueLogs.collectAsState()
    val sleepLogs by viewModel.sleepLogs.collectAsState()

    val userProfile by viewModel.userProfile.collectAsState()
    val isWoman = remember(currentUser, userProfile) {
        val g = (userProfile?.gender ?: currentUser?.gender ?: "").lowercase().trim()
        g == "female" || g == "woman" || g == "women" || g == "girl"
    }

    val tabs = remember(isWoman) {
        val list = mutableListOf("Metrics Log", "Exercise", "Symptom Log", "Sleep", "Medicine Taker")
        if (isWoman) {
            list.add("Period Tracker")
        }
        list
    }

    val activeSubTab by viewModel.healthSubTab.collectAsState()
    LaunchedEffect(tabs) {
        if (activeSubTab >= tabs.size) {
            viewModel.setHealthSubTab(0)
        }
    }

    val focusManager = LocalFocusManager.current
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    // Dynamic BMI - pick up latest logged weight in weightEntries, fallback to profile weight
    val rawHeight = userProfile?.height ?: ""
    val heightFromProfile = rawHeight.replace("cm", "", ignoreCase = true).trim().toDoubleOrNull()
    val height = heightFromProfile ?: currentUser?.heightCm ?: 0.0

    val latestWeightEntry = weightEntries.sortedByDescending { it.date }.firstOrNull()
    val rawWeight = userProfile?.weight ?: ""
    val weightFromProfile = rawWeight.replace("kg", "", ignoreCase = true).trim().toDoubleOrNull()
    val weight = latestWeightEntry?.weightKg ?: weightFromProfile ?: currentUser?.weightKg ?: 0.0
    val bmi = if (height > 50 && weight > 20) {
        weight / (height / 100.0).pow(2)
    } else 0.0

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
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


        // --- Rule-Based Clinical Health Analysis Tips (Section 10.1 & Part 16) ---
        item {
            val tips = evaluateHealthTips(bmi, waterLogs, vitalReadings, exerciseLogs, healthIssueLogs)
            if (tips.isNotEmpty()) {
                HealthTipsPanel(tips = tips)
            }
        }

        // --- BMI Tracker & Water Logs (First row of visual analytics) ---
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BMICard(bmi = bmi, weight = weight, height = height, modifier = Modifier.weight(1f).fillMaxHeight())
                    WaterTrackerCard(viewModel = viewModel, logs = waterLogs, modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }

        val activeTabLabel = tabs.getOrNull(activeSubTab) ?: "Metrics Log"

        // --- Sub-Tab Contents ---
        when (activeTabLabel) {
            "Metrics Log" -> {
                item { WeightLogSection(viewModel = viewModel, entries = weightEntries) }
                item { VitalsLogSection(viewModel = viewModel, readings = vitalReadings) }
            }
            "Exercise" -> {
                item { ExerciseLogSection(viewModel = viewModel, logs = exerciseLogs) }
            }
            "Symptom Log" -> {
                item { SymptomLogSection(viewModel = viewModel, logs = healthIssueLogs) }
            }
            "Sleep" -> {
                item { SleepLogSection(viewModel = viewModel, sleepLogs = sleepLogs) }
            }
            "Medicine Taker" -> {
                item { TabletTrackerSection(viewModel = viewModel) }
            }
            "Period Tracker" -> {
                if (isWoman) {
                    item { PeriodTrackerSection(viewModel = viewModel) }
                }
            }
        }
    }

    }
}

// --- BMI Calculation Card (Section 10.2) ---
@Composable
fun BMICard(bmi: Double, weight: Double, height: Double, modifier: Modifier = Modifier) {
    val category = when {
        bmi <= 0.0 -> "N/A"
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Normal"
        bmi < 30.0 -> "Overweight"
        else -> "Obese"
    }

    val categoryColor = when (category) {
        "Normal" -> BrandGreen
        "Underweight", "Overweight" -> BrandAmber
        "Obese" -> BrandRose
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "BODY MASS INDEX", 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold, 
                color = BrandViolet,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = if (bmi > 0) "%.1f".format(bmi) else "—",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = categoryColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
            )

            Text(
                text = category,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = categoryColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "${weight.toInt()} kg · ${height.toInt()} cm",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
            )
        }
    }
}

// --- Water Intake Incrementer Card (Section 10.3) ---
@Composable
fun WaterTrackerCard(viewModel: TrackWiseViewModel, logs: List<WaterLogEntity>, modifier: Modifier = Modifier) {
    val todayLog = logs.find { it.date == TrackWiseUtils.getTodayString() }
    val glasses = todayLog?.glasses ?: 0
    val goal = todayLog?.goal ?: 8

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "HYDRATION METER", 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold, 
                color = BrandCyan,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.adjustWaterLog(-1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrement", tint = BrandCyan)
                }

                Text(
                    text = "$glasses/$goal",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = { viewModel.adjustWaterLog(1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increment", tint = BrandCyan)
                }
            }

            Text(
                text = "Glasses today", 
                fontSize = 11.sp, 
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// --- Clinical Health Tips Panel (Section 10.1 & Part 16) ---
@Composable
fun HealthTipsPanel(tips: List<HealthTip>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Healing, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(18.dp))
                Text(
                    text = "CLINICAL INSIGHTS & HEALTH TIPS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tips.forEach { tip ->
                    val borderAccentColor = when (tip.level) {
                        "alert" -> BrandRose
                        "caution" -> BrandAmber
                        "good" -> BrandGreen
                        else -> BrandCyan
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(borderAccentColor.copy(alpha = 0.08f))
                            .border(1.dp, borderAccentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = when (tip.level) {
                                "alert" -> Icons.Default.Warning
                                "caution" -> Icons.Default.Info
                                "good" -> Icons.Default.CheckCircle
                                else -> Icons.Default.Lightbulb
                            },
                            contentDescription = null,
                            tint = borderAccentColor,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(text = tip.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = borderAccentColor)
                            Text(text = tip.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
                            if (tip.suggestion != null) {
                                Text(
                                    text = "💡 Advice: ${tip.suggestion}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandViolet,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Weight Logger (Section 10.4) ---
@Composable
fun WeightLogSection(viewModel: TrackWiseViewModel, entries: List<WeightEntryEntity>) {
    val focusManager = LocalFocusManager.current
    var weightInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }

    var showErrors by remember { mutableStateOf(false) }

    var editingWeightEntry by remember { mutableStateOf<WeightEntryEntity?>(null) }
    if (editingWeightEntry != null) {
        val entry = editingWeightEntry!!
        var editWeight by remember(entry) { mutableStateOf(entry.weightKg.toString()) }
        var editNotes by remember(entry) { mutableStateOf(entry.notes ?: "") }
        var editDate by remember(entry) { mutableStateOf(entry.date) }

        val scrollState = rememberScrollState()
        LaunchedEffect(scrollState.isScrollInProgress) {
            if (scrollState.isScrollInProgress) {
                focusManager.clearFocus()
            }
        }

        AlertDialog(
            onDismissRequest = { editingWeightEntry = null },
            title = { Text("Edit Weight Entry ⚖️", fontWeight = FontWeight.Bold, color = BrandPink) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editWeight,
                        onValueChange = { editWeight = it },
                        label = { Text("Weight (kg) *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val w = editWeight.toDoubleOrNull()
                        if (w != null && w > 0) {
                            val updatedEntry = entry.copy(
                                weightKg = w,
                                notes = editNotes.ifBlank { null },
                                date = editDate
                            )
                            viewModel.updateWeightEntry(updatedEntry)
                            editingWeightEntry = null
                        }
                    }
                ) {
                    Text("Save Changes", color = BrandPink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingWeightEntry = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }
    val weightError = if (weightInput.isBlank()) {
        "Weight is required"
    } else {
        val w = weightInput.toDoubleOrNull()
        if (w == null) "Must be a valid number"
        else if (w <= 0.0) "Weight must be greater than 0 kg"
        else null
    }

    val context = LocalContext.current
    val parsedDate = remember(selectedDate) { TrackWiseUtils.parseDate(selectedDate) }
    val calendar = remember(parsedDate) { Calendar.getInstance().apply { time = parsedDate } }
    val isDarkTheme = MaterialTheme.colorScheme.background.let { (it.red + it.green + it.blue) / 3f < 0.5f }
    val themeId = if (isDarkTheme) android.R.style.Theme_DeviceDefault_Dialog_Alert else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert

    val datePickerDialog = remember(calendar, themeId) {
        android.app.DatePickerDialog(
            context,
            themeId,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                }
                selectedDate = TrackWiseUtils.formatDate(selectedCal.time, "yyyy-MM-dd")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("LOG WEIGHT (KG)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPink)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { 
                        weightInput = it 
                        showErrors = false
                    },
                    label = { Text("Weight (kg) *") },
                    isError = showErrors && weightError != null,
                    supportingText = {
                        if (showErrors && weightError != null) {
                            Text(weightError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1.8f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        label = { Text("Log Date", fontSize = 10.sp, maxLines = 1) },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BrandPink, modifier = Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { datePickerDialog.show() }
                    )
                }
            }

            Button(
                onClick = {
                    if (weightError == null) {
                        val w = weightInput.toDoubleOrNull()
                        if (w != null) {
                            viewModel.logWeight(w, if (notesInput.isBlank()) null else notesInput, selectedDate)
                            weightInput = ""
                            notesInput = ""
                            showErrors = false
                        }
                    } else {
                        showErrors = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPink),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Weight Entry", color = Color.White)
            }

            val pinnedHealthLogIds by viewModel.pinnedHealthLogIds.collectAsState()
            var optionsEntry by remember { mutableStateOf<WeightEntryEntity?>(null) }

            // Histoy List
            if (entries.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                entries.take(5).forEach { entry ->
                    val isPinned = pinnedHealthLogIds.contains(entry.id)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { editingWeightEntry = entry },
                                    onLongClick = { optionsEntry = entry }
                                )
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (isPinned) {
                                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = BrandAmber, modifier = Modifier.size(12.dp))
                                }
                                Text("${entry.date} · ${entry.weightKg} kg", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { optionsEntry = entry }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }

                        if (optionsEntry?.id == entry.id) {
                            HealthLogItemOptionsDialog(
                                title = "${entry.date} · ${entry.weightKg} kg",
                                isPinned = isPinned,
                                onEdit = { editingWeightEntry = entry },
                                onTogglePin = { viewModel.togglePinHealthLog(entry.id) },
                                onDelete = { viewModel.deleteWeightEntry(entry.id) },
                                onDismiss = { optionsEntry = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Vitals Logger (Blood Sugar & Blood Pressure) (Section 10.5 & 10.6) ---
@Composable
fun VitalsLogSection(viewModel: TrackWiseViewModel, readings: List<VitalReadingEntity>) {
    val focusManager = LocalFocusManager.current
    var vitalType by remember { mutableStateOf("blood_sugar") } // "blood_sugar", "blood_pressure"
    
    // Single sugar input box separated by /
    var sugarInput by remember { mutableStateOf("") }
    
    // BP input
    var bpInput by remember { mutableStateOf("") }
    
    // Condition/Meal context dropdown
    var contextInput by remember { mutableStateOf("fasting") } // "fasting", "post_meal", "random"
    var contextExpanded by remember { mutableStateOf(false) }

    var showVitalsErrors by remember { mutableStateOf(false) }

    var editingVitalReading by remember { mutableStateOf<VitalReadingEntity?>(null) }
    if (editingVitalReading != null) {
        val read = editingVitalReading!!
        var editValue by remember(read) { mutableStateOf(read.value) }
        var editContext by remember(read) { mutableStateOf(read.context ?: "fasting") }
        var editContextExpanded by remember { mutableStateOf(false) }

        val scrollState = rememberScrollState()
        LaunchedEffect(scrollState.isScrollInProgress) {
            if (scrollState.isScrollInProgress) {
                focusManager.clearFocus()
            }
        }

        AlertDialog(
            onDismissRequest = { editingVitalReading = null },
            title = { Text(if (read.type == "blood_sugar") "Edit Blood Sugar Entry 🩸" else "Edit Blood Pressure Entry 🫀", fontWeight = FontWeight.Bold, color = BrandCyan) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        label = { Text(if (read.type == "blood_sugar") "Value (mg/dL)" else "Value (Systolic/Diastolic)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (read.type == "blood_sugar") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (editContext == "fasting") "Fasting" else if (editContext == "post_meal") "Post Meal" else "Random",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Condition / Meal State") },
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Toggle")
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandCyan,
                                    focusedLabelColor = BrandCyan
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { editContextExpanded = !editContextExpanded }
                            )
                            DropdownMenu(
                                expanded = editContextExpanded,
                                onDismissRequest = { editContextExpanded = false },
                                modifier = Modifier
                                    .widthIn(min = 180.dp, max = 280.dp)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, BrandCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Fasting") },
                                    onClick = {
                                        editContext = "fasting"
                                        editContextExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Post Meal") },
                                    onClick = {
                                        editContext = "post_meal"
                                        editContextExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Random") },
                                    onClick = {
                                        editContext = "random"
                                        editContextExpanded = false
                                    }
                                )
                            }
                        }
                    } else {
                        var editContextBpExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (editContext == "resting") "Resting" else if (editContext == "post_exercise") "Post Exercise" else "Random",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Context") },
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Toggle")
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandCyan,
                                    focusedLabelColor = BrandCyan
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { editContextBpExpanded = !editContextBpExpanded }
                            )
                            DropdownMenu(
                                expanded = editContextBpExpanded,
                                onDismissRequest = { editContextBpExpanded = false },
                                modifier = Modifier
                                    .widthIn(min = 180.dp, max = 280.dp)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, BrandCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Resting") },
                                    onClick = {
                                        editContext = "resting"
                                        editContextBpExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Post Exercise") },
                                    onClick = {
                                        editContext = "post_exercise"
                                        editContextBpExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Random") },
                                    onClick = {
                                        editContext = "random"
                                        editContextBpExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editValue.isNotBlank()) {
                            val updatedReading = read.copy(
                                value = editValue,
                                context = editContext.ifBlank { null }
                            )
                            viewModel.updateVitalReading(updatedReading)
                            editingVitalReading = null
                        }
                    }
                ) {
                    Text("Save Changes", color = BrandCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingVitalReading = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    val sugarError = if (sugarInput.isBlank()) {
        "Blood sugar is required"
    } else {
        val s = sugarInput.toDoubleOrNull()
        if (s == null) {
            "Must be a valid positive number"
        } else if (s <= 0) {
            "Must be greater than 0 mg/dL"
        } else {
            null
        }
    }

    val bpError = if (bpInput.isBlank()) {
        "Blood pressure is required"
    } else {
        val parts = bpInput.split("/")
        if (parts.size != 2) {
            "Must be in Systolic/Diastolic format (e.g. 120/80)"
        } else {
            val sys = parts[0].trim().toIntOrNull()
            val dia = parts[1].trim().toIntOrNull()
            if (sys == null || dia == null) {
                "Must be valid numbers"
            } else if (sys <= 0 || dia <= 0) {
                "Pressure values must be positive"
            } else {
                null
            }
        }
    }

    // Date & Time pickers
    var selectedDate by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }
    var selectedTime by remember { mutableStateOf(SimpleDateFormat("hh:mm a", Locale.US).format(Date())) }

    val context = LocalContext.current
    
    // DatePickerDialog setup
    val parsedDate = remember(selectedDate) { TrackWiseUtils.parseDate(selectedDate) }
    val calendar = remember(parsedDate) { Calendar.getInstance().apply { time = parsedDate } }
    val isDarkTheme = MaterialTheme.colorScheme.background.let { (it.red + it.green + it.blue) / 3f < 0.5f }
    val themeId = if (isDarkTheme) android.R.style.Theme_DeviceDefault_Dialog_Alert else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert

    val datePickerDialog = remember(calendar, themeId) {
        android.app.DatePickerDialog(
            context,
            themeId,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                }
                selectedDate = TrackWiseUtils.formatDate(selectedCal.time, "yyyy-MM-dd")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // TimePickerDialog setup
    val timePickerDialog = remember(context, selectedTime, themeId) {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("hh:mm a", Locale.US)
            cal.time = sdf.parse(selectedTime) ?: Date()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        android.app.TimePickerDialog(
            context,
            themeId,
            { _, hour, minute ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                selectedTime = SimpleDateFormat("hh:mm a", Locale.US).format(newCal.time)
            },
            h,
            m,
            false
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("LOG BODY VITALS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandCyan)

            // Selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (vitalType == "blood_sugar") BrandCyan else Color.Transparent)
                        .clickable { vitalType = "blood_sugar" }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Blood Sugar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (vitalType == "blood_sugar") Color.White else MaterialTheme.colorScheme.onBackground)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (vitalType == "blood_pressure") BrandCyan else Color.Transparent)
                        .clickable { vitalType = "blood_pressure" }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Blood Pressure", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (vitalType == "blood_pressure") Color.White else MaterialTheme.colorScheme.onBackground)
                }
            }

            // Single input box for Sugar (mg/dL) OR standard BP box
            if (vitalType == "blood_sugar") {
                OutlinedTextField(
                    value = sugarInput,
                    onValueChange = { 
                        sugarInput = it 
                        showVitalsErrors = false
                    },
                    label = { Text("Blood Sugar (mg/dL) *") },
                    placeholder = { Text("e.g. 120") },
                    isError = showVitalsErrors && sugarError != null,
                    supportingText = {
                        if (showVitalsErrors && sugarError != null) {
                            Text(sugarError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Dropdown for fasting, post_meal, random
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (contextInput == "fasting") "Fasting" else if (contextInput == "post_meal") "Post Meal" else "Random",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Condition / Meal State") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Toggle")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandRose,
                            focusedLabelColor = BrandRose
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Transparent overlay to ensure the entire container is clickable and opens the dropdown
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { contextExpanded = !contextExpanded }
                    )
                    DropdownMenu(
                        expanded = contextExpanded,
                        onDismissRequest = { contextExpanded = false },
                        modifier = Modifier
                            .widthIn(min = 180.dp, max = 280.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, BrandRose.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Fasting") },
                            onClick = {
                                contextInput = "fasting"
                                contextExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Post Meal") },
                            onClick = {
                                contextInput = "post_meal"
                                contextExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Random") },
                            onClick = {
                                contextInput = "random"
                                contextExpanded = false
                            }
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = bpInput,
                    onValueChange = { 
                        bpInput = it 
                        showVitalsErrors = false
                    },
                    label = { Text("BP (Systolic/Diastolic) *") },
                    placeholder = { Text("e.g. 120/80") },
                    isError = showVitalsErrors && bpError != null,
                    supportingText = {
                        if (showVitalsErrors && bpError != null) {
                            Text(bpError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Date and Time selectors (equal sizes)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        label = { Text("Log Date", fontSize = 10.sp, maxLines = 1) },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { datePickerDialog.show() }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedTime,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Log Time") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { timePickerDialog.show() }
                    )
                }
            }

            Button(
                onClick = {
                    val hasError = if (vitalType == "blood_sugar") sugarError != null else bpError != null
                    if (!hasError) {
                        val finalValue = if (vitalType == "blood_sugar") {
                            sugarInput
                        } else {
                            bpInput
                        }
                        if (finalValue.isNotBlank()) {
                            viewModel.logVital(
                                type = vitalType,
                                value = finalValue,
                                context = if (vitalType == "blood_sugar") contextInput else "resting",
                                notes = null,
                                date = selectedDate,
                                time = selectedTime
                            )
                            sugarInput = ""
                            bpInput = ""
                            showVitalsErrors = false
                        }
                    } else {
                        showVitalsErrors = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Vital Log", color = Color.White)
            }

            val pinnedHealthLogIds by viewModel.pinnedHealthLogIds.collectAsState()
            var optionsReading by remember { mutableStateOf<VitalReadingEntity?>(null) }

            // Readings history
            val filteredReadings = readings.filter { it.type == vitalType }
            if (filteredReadings.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                filteredReadings.take(5).forEach { read ->
                    val isPinned = pinnedHealthLogIds.contains(read.id)
                    val displayValue = if (read.type == "blood_sugar") "${read.value} mg/dL" else read.value
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { editingVitalReading = read },
                                    onLongClick = { optionsReading = read }
                                )
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (isPinned) {
                                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = BrandAmber, modifier = Modifier.size(12.dp))
                                }
                                Text("${read.date} ${read.time} · $displayValue (${read.context?.uppercase()})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { optionsReading = read }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }

                        if (optionsReading?.id == read.id) {
                            val displayVal = if (read.type == "blood_sugar") "${read.value} mg/dL" else read.value
                            HealthLogItemOptionsDialog(
                                title = "${read.date} ${read.time} · $displayVal",
                                isPinned = isPinned,
                                onEdit = { editingVitalReading = read },
                                onTogglePin = { viewModel.togglePinHealthLog(read.id) },
                                onDelete = { viewModel.deleteVitalReading(read.id) },
                                onDismiss = { optionsReading = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Exercise Log (Section 10.7) ---
@Composable
fun ExerciseLogSection(viewModel: TrackWiseViewModel, logs: List<ExerciseLogEntity>) {
    val focusManager = LocalFocusManager.current
    var selectedType by remember { mutableStateOf("Walking") }
    var customTypeInput by remember { mutableStateOf("") }
    var durationInput by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var showExerciseErrors by remember { mutableStateOf(false) }

    var editingExerciseLog by remember { mutableStateOf<ExerciseLogEntity?>(null) }
    if (editingExerciseLog != null) {
        val log = editingExerciseLog!!
        var editType by remember(log) { mutableStateOf(log.exerciseType) }
        var editDuration by remember(log) { mutableStateOf(log.durationMinutes.toString()) }
        var editNotes by remember(log) { mutableStateOf(log.notes ?: "") }
        var editTypeExpanded by remember { mutableStateOf(false) }
        val exerciseOptions = listOf("Walking", "Running", "Gym/Weights", "Yoga", "Cycling", "Others")

        val scrollState = rememberScrollState()
        LaunchedEffect(scrollState.isScrollInProgress) {
            if (scrollState.isScrollInProgress) {
                focusManager.clearFocus()
            }
        }

        AlertDialog(
            onDismissRequest = { editingExerciseLog = null },
            title = { Text("Edit Exercise Log 🏃‍♂️", fontWeight = FontWeight.Bold, color = BrandViolet) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editType,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Activity Type *") },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Toggle")
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { editTypeExpanded = !editTypeExpanded }
                        )
                        DropdownMenu(
                            expanded = editTypeExpanded,
                            onDismissRequest = { editTypeExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            exerciseOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        editType = opt
                                        editTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = editDuration,
                        onValueChange = { editDuration = it },
                        label = { Text("Duration (minutes) *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val dur = editDuration.toIntOrNull()
                        if (editType.isNotBlank() && dur != null && dur > 0) {
                            val updatedLog = log.copy(
                                exerciseType = editType,
                                durationMinutes = dur,
                                notes = editNotes.ifBlank { null }
                            )
                            viewModel.updateExerciseLog(updatedLog)
                            editingExerciseLog = null
                        }
                    }
                ) {
                    Text("Save Changes", color = BrandViolet, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingExerciseLog = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }
    val durationError = if (durationInput.isBlank()) {
        "Duration is required"
    } else if (durationInput.toIntOrNull() == null || durationInput.toInt() <= 0) {
        "Enter a valid positive number"
    } else null

    val customTypeError = if (selectedType == "Others" && customTypeInput.isBlank()) {
        "Exercise name is required"
    } else null

    val exerciseOptions = listOf("Walking", "Running", "Gym/Weights", "Yoga", "Cycling", "Others")

    val context = LocalContext.current
    val parsedDate = remember(selectedDate) { TrackWiseUtils.parseDate(selectedDate) }
    val calendar = remember(parsedDate) { Calendar.getInstance().apply { time = parsedDate } }
    val isDarkTheme = MaterialTheme.colorScheme.background.let { (it.red + it.green + it.blue) / 3f < 0.5f }
    val themeId = if (isDarkTheme) android.R.style.Theme_DeviceDefault_Dialog_Alert else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert

    val datePickerDialog = remember(calendar, themeId) {
        android.app.DatePickerDialog(
            context,
            themeId,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                }
                selectedDate = TrackWiseUtils.formatDate(selectedCal.time, "yyyy-MM-dd")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("LOG EXERCISE ACTIVITY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandViolet)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Exercise selector dropdown (weight 1.8f)
                Box(modifier = Modifier.weight(1.8f)) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Exercise") },
                        trailingIcon = {
                            IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Toggle")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable { dropdownExpanded = !dropdownExpanded }
                    )
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.45f).background(MaterialTheme.colorScheme.surface)
                    ) {
                        exerciseOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    selectedType = opt
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Duration input box (weight 0.8f)
                OutlinedTextField(
                    value = durationInput,
                    onValueChange = { 
                        durationInput = it 
                        showExerciseErrors = false
                    },
                    label = { Text("Duration (mins) *") },
                    isError = showExerciseErrors && durationError != null,
                    supportingText = {
                        if (showExerciseErrors && durationError != null) {
                            Text(durationError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.8f)
                )
            }

            // If "Others" is selected, show an additional field to enter custom exercise name
            if (selectedType == "Others") {
                OutlinedTextField(
                    value = customTypeInput,
                    onValueChange = { 
                        customTypeInput = it 
                        showExerciseErrors = false
                    },
                    label = { Text("Enter Custom Exercise Name *") },
                    isError = showExerciseErrors && customTypeError != null,
                    supportingText = {
                        if (showExerciseErrors && customTypeError != null) {
                            Text(customTypeError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Date picker box
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        label = { Text("Log Date", fontSize = 10.sp, maxLines = 1) },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { datePickerDialog.show() }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Button(
                onClick = {
                    val hasError = durationError != null || (selectedType == "Others" && customTypeError != null)
                    if (!hasError) {
                        val d = durationInput.toIntOrNull() ?: 0
                        val finalType = if (selectedType == "Others") customTypeInput else selectedType
                        if (finalType.isNotBlank()) {
                            viewModel.logExercise(finalType, d, true, null, selectedDate)
                            durationInput = ""
                            customTypeInput = ""
                            showExerciseErrors = false
                        }
                    } else {
                        showExerciseErrors = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log Activity Session", color = Color.White)
            }

            val pinnedHealthLogIds by viewModel.pinnedHealthLogIds.collectAsState()
            var optionsLog by remember { mutableStateOf<ExerciseLogEntity?>(null) }

            if (logs.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                logs.take(5).forEach { log ->
                    val isPinned = pinnedHealthLogIds.contains(log.id)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { editingExerciseLog = log },
                                    onLongClick = { optionsLog = log }
                                )
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (isPinned) {
                                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = BrandAmber, modifier = Modifier.size(12.dp))
                                }
                                Text("${log.date} · ${log.exerciseType} (${log.durationMinutes} mins)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { optionsLog = log }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }

                        if (optionsLog?.id == log.id) {
                            HealthLogItemOptionsDialog(
                                title = "${log.date} · ${log.exerciseType} (${log.durationMinutes} mins)",
                                isPinned = isPinned,
                                onEdit = { editingExerciseLog = log },
                                onTogglePin = { viewModel.togglePinHealthLog(log.id) },
                                onDelete = { viewModel.deleteExerciseLog(log.id) },
                                onDismiss = { optionsLog = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Health Issue Logs (Symptoms) (Section 10.8) ---
@Composable
fun SymptomLogSection(viewModel: TrackWiseViewModel, logs: List<HealthIssueLogEntity>) {
    val focusManager = LocalFocusManager.current
    var issueName by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("mild") } // "mild", "moderate", "severe"
    var selectedDate by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }

    var editingHealthIssueLog by remember { mutableStateOf<HealthIssueLogEntity?>(null) }
    if (editingHealthIssueLog != null) {
        val log = editingHealthIssueLog!!
        var editName by remember(log) { mutableStateOf(log.issueName) }
        var editSeverity by remember(log) { mutableStateOf(log.severity) }
        var editNotes by remember(log) { mutableStateOf(log.notes ?: "") }

        val scrollState = rememberScrollState()
        LaunchedEffect(scrollState.isScrollInProgress) {
            if (scrollState.isScrollInProgress) {
                focusManager.clearFocus()
            }
        }

        AlertDialog(
            onDismissRequest = { editingHealthIssueLog = null },
            title = { Text("Edit Symptom Log 🩹", fontWeight = FontWeight.Bold, color = BrandRose) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Symptom/Issue Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Column {
                        Text("Severity", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("mild", "moderate", "severe").forEach { s ->
                                val selected = editSeverity == s
                                Button(
                                    onClick = { editSeverity = s },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) BrandRose else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(s.replaceFirstChar { it.uppercase() }, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editName.isNotBlank()) {
                            val updatedLog = log.copy(
                                issueName = editName,
                                severity = editSeverity,
                                notes = editNotes.ifBlank { null }
                            )
                            viewModel.updateHealthIssueLog(updatedLog)
                            editingHealthIssueLog = null
                        }
                    }
                ) {
                    Text("Save Changes", color = BrandRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingHealthIssueLog = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    val context = LocalContext.current
    val parsedDate = remember(selectedDate) { TrackWiseUtils.parseDate(selectedDate) }
    val calendar = remember(parsedDate) { Calendar.getInstance().apply { time = parsedDate } }
    val isDarkTheme = MaterialTheme.colorScheme.background.let { (it.red + it.green + it.blue) / 3f < 0.5f }
    val themeId = if (isDarkTheme) android.R.style.Theme_DeviceDefault_Dialog_Alert else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert

    val datePickerDialog = remember(calendar, themeId) {
        android.app.DatePickerDialog(
            context,
            themeId,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                }
                selectedDate = TrackWiseUtils.formatDate(selectedCal.time, "yyyy-MM-dd")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("LOG SYMPTOM / DISCOMFORT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandRose)

            OutlinedTextField(
                value = issueName,
                onValueChange = { issueName = it },
                label = { Text("Symptom / Discomfort Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Severity Selector
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("mild", "moderate", "severe").forEach { sev ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (severity == sev) BrandRose else MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { severity = sev }
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(sev.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp), color = if (severity == sev) Color.White else MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }

            // Date picker box
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        label = { Text("Log Date", fontSize = 10.sp, maxLines = 1) },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BrandRose, modifier = Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { datePickerDialog.show() }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Button(
                onClick = {
                    if (issueName.isNotBlank()) {
                        viewModel.logHealthIssue(
                            issueId = "issue-${System.currentTimeMillis()}",
                            issueName = issueName,
                            severity = severity,
                            notes = null,
                            date = selectedDate
                        )
                        issueName = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandRose),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log Symptom", color = Color.White)
            }

            val pinnedHealthLogIds by viewModel.pinnedHealthLogIds.collectAsState()
            var optionsLog by remember { mutableStateOf<HealthIssueLogEntity?>(null) }

            if (logs.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                logs.take(5).forEach { log ->
                    val isPinned = pinnedHealthLogIds.contains(log.id)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { editingHealthIssueLog = log },
                                    onLongClick = { optionsLog = log }
                                )
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (isPinned) {
                                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = BrandAmber, modifier = Modifier.size(12.dp))
                                }
                                Text("${log.date} · ${log.issueName} (${log.severity.uppercase()})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { optionsLog = log }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }

                        if (optionsLog?.id == log.id) {
                            HealthLogItemOptionsDialog(
                                title = "${log.date} · ${log.issueName} (${log.severity.uppercase()})",
                                isPinned = isPinned,
                                onEdit = { editingHealthIssueLog = log },
                                onTogglePin = { viewModel.togglePinHealthLog(log.id) },
                                onDelete = { viewModel.deleteHealthIssueLog(log.id) },
                                onDismiss = { optionsLog = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SleepLogSection(
    viewModel: TrackWiseViewModel,
    sleepLogs: List<SleepLogEntity>
) {
    val focusManager = LocalFocusManager.current
    fun isValidSleepTime(time: String): Boolean {
        if (time.isBlank()) return false
        val (hour, min) = parseTimeHourMinute(time)
        return hour in 0..23 && min in 0..59
    }

    var sleepStart by remember { mutableStateOf("22:30") }
    var sleepEnd by remember { mutableStateOf("06:30") }
    var notes by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }

    var showSleepErrors by remember { mutableStateOf(false) }

    var editingSleepLog by remember { mutableStateOf<SleepLogEntity?>(null) }
    if (editingSleepLog != null) {
        val log = editingSleepLog!!
        var editStart by remember(log) { mutableStateOf(log.startTime) }
        var editEnd by remember(log) { mutableStateOf(log.endTime) }
        var editNotes by remember(log) { mutableStateOf(log.notes ?: "") }

        val scrollState = rememberScrollState()
        LaunchedEffect(scrollState.isScrollInProgress) {
            if (scrollState.isScrollInProgress) {
                focusManager.clearFocus()
            }
        }

        AlertDialog(
            onDismissRequest = { editingSleepLog = null },
            title = { Text("Edit Sleep Log 🛌", fontWeight = FontWeight.Bold, color = BrandViolet) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HealthTimePickerField(
                        timeStr = editStart,
                        onTimeSelected = { editStart = it },
                        label = "Sleep Start",
                        tintColor = BrandViolet,
                        modifier = Modifier.fillMaxWidth()
                    )
                    HealthTimePickerField(
                        timeStr = editEnd,
                        onTimeSelected = { editEnd = it },
                        label = "Sleep End",
                        tintColor = BrandViolet,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isValidSleepTime(editStart) && isValidSleepTime(editEnd)) {
                            val hours = calculateHoursDifference(editStart, editEnd)
                            val updatedLog = log.copy(
                                startTime = editStart,
                                endTime = editEnd,
                                hoursSlept = hours,
                                notes = editNotes.ifBlank { null }
                            )
                            viewModel.updateSleepLog(updatedLog)
                            editingSleepLog = null
                        }
                    }
                ) {
                    Text("Save Changes", color = BrandViolet, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSleepLog = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    val startError = if (!isValidSleepTime(sleepStart)) "Use HH:MM format (00:00 to 23:59)" else null
    val endError = if (!isValidSleepTime(sleepEnd)) "Use HH:MM format (00:00 to 23:59)" else null

    val context = LocalContext.current
    val parsedDate = remember(selectedDate) { TrackWiseUtils.parseDate(selectedDate) }
    val calendar = remember(parsedDate) { Calendar.getInstance().apply { time = parsedDate } }
    val isDarkTheme = MaterialTheme.colorScheme.background.let { (it.red + it.green + it.blue) / 3f < 0.5f }
    val themeId = if (isDarkTheme) android.R.style.Theme_DeviceDefault_Dialog_Alert else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert

    val datePickerDialog = remember(calendar, themeId) {
        android.app.DatePickerDialog(
            context,
            themeId,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                }
                selectedDate = TrackWiseUtils.formatDate(selectedCal.time, "yyyy-MM-dd")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val calculatedHours = calculateHoursDifference(sleepStart, sleepEnd)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.NightsStay, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(20.dp))
                    Text(
                        text = "LOG NEW SLEEP ENTRY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandViolet
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandViolet.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "%.1f Hours".format(calculatedHours),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandViolet
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        label = { Text("Sleep Date", fontSize = 10.sp, maxLines = 1) },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { datePickerDialog.show() }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HealthTimePickerField(
                    timeStr = sleepStart,
                    onTimeSelected = { 
                        sleepStart = it 
                        showSleepErrors = false
                    },
                    label = "Sleep Start *",
                    tintColor = BrandViolet,
                    modifier = Modifier.weight(1f)
                )

                HealthTimePickerField(
                    timeStr = sleepEnd,
                    onTimeSelected = { 
                        sleepEnd = it 
                        showSleepErrors = false
                    },
                    label = "Wake Up *",
                    tintColor = BrandViolet,
                    modifier = Modifier.weight(1f)
                )
            }

            CompactTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Sleep Quality Notes",
                placeholder = "e.g. Felt relaxed, deep sleep",
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val hasError = startError != null || endError != null
                    if (!hasError) {
                        viewModel.addSleepLog(
                            hoursSlept = calculatedHours,
                            startTime = sleepStart,
                            endTime = sleepEnd,
                            notes = notes.ifBlank { "Logged sleep" },
                            date = selectedDate
                        )
                        notes = ""
                        showSleepErrors = false
                    } else {
                        showSleepErrors = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log Sleep", color = Color.White, fontWeight = FontWeight.Bold)
            }

            val pinnedHealthLogIds by viewModel.pinnedHealthLogIds.collectAsState()
            var optionsLog by remember { mutableStateOf<SleepLogEntity?>(null) }

            if (sleepLogs.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Text(
                    text = "SLEEP LOG HISTORY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )

                sleepLogs.forEach { log ->
                    val isPinned = pinnedHealthLogIds.contains(log.id)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .combinedClickable(
                                    onClick = { editingSleepLog = log },
                                    onLongClick = { optionsLog = log }
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isPinned) {
                                        Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = BrandAmber, modifier = Modifier.size(12.dp))
                                    }
                                    Text(
                                        text = log.date,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(BrandViolet.copy(alpha = 0.1f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${log.hoursSlept} hrs",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandViolet
                                        )
                                    }
                                }
                                Text(
                                    text = "Times: ${log.startTime} to ${log.endTime} · ${log.notes ?: ""}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }

                            IconButton(onClick = { optionsLog = log }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }

                        if (optionsLog?.id == log.id) {
                            HealthLogItemOptionsDialog(
                                title = "${log.date} · ${log.hoursSlept} hrs",
                                isPinned = isPinned,
                                onEdit = { editingSleepLog = log },
                                onTogglePin = { viewModel.togglePinHealthLog(log.id) },
                                onDelete = { viewModel.deleteSleepLog(log.id) },
                                onDismiss = { optionsLog = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun calculateHoursDifference(start: String, end: String): Double {
    try {
        val (hStart, mStart) = parseTimeHourMinute(start)
        val (hEnd, mEnd) = parseTimeHourMinute(end)

        var diffMin = (hEnd * 60 + mEnd) - (hStart * 60 + mStart)
        if (diffMin <= 0) {
            diffMin += 24 * 60 // spanned midnight or same time
        }
        return diffMin / 60.0
    } catch (e: Exception) {
        return 8.0
    }
}

// --- HealthTips Evaluations (Part 16 Clinical Rules) ---
fun evaluateHealthTips(
    bmi: Double,
    waterLogs: List<WaterLogEntity>,
    vitals: List<VitalReadingEntity>,
    exercises: List<ExerciseLogEntity>,
    issues: List<HealthIssueLogEntity>
): List<HealthTip> {
    val tips = mutableListOf<HealthTip>()

    // BMI Tip
    if (bmi > 0.0) {
        when {
            bmi < 18.5 -> tips.add(
                HealthTip("Underweight BMI alert", "Your BMI is under 18.5. Lean proteins and complex carbohydrates can support energy levels.", "alert", "Consult a certified nutritionist.")
            )
            bmi < 25.0 -> tips.add(
                HealthTip("Normal BMI status", "Excellent! Your weight and height metrics are perfectly proportional.", "good", "Maintain this balanced lifestyle.")
            )
            bmi < 30.0 -> tips.add(
                HealthTip("Overweight BMI caution", "Your BMI is in the overweight zone (25 - 29.9). Core exercises can support cardiac wellness.", "caution", "Increase active steps daily.")
            )
            else -> tips.add(
                HealthTip("Obesity indicator alert", "Your BMI is over 30. High severity status detected.", "alert", "Consult with a health professional.")
            )
        }
    }

    // Water Log Tip
    val todayWater = waterLogs.find { it.date == TrackWiseUtils.getTodayString() }
    if (todayWater != null) {
        val glasses = todayWater.glasses
        val goal = todayWater.goal
        if (glasses < goal / 2) {
            tips.add(
                HealthTip("Inadequate water intake", "Your hydration is under 50% of your daily goal.", "caution", "Drink a glass of water now!")
            )
        } else if (glasses >= goal) {
            tips.add(
                HealthTip("Perfect Hydration achieved!", "You met your daily target of $goal glasses.", "good")
            )
        }
    }

    // Blood Sugar Tips
    val sugarVitals = vitals.filter { it.type == "blood_sugar" }
    val latestSugar = sugarVitals.firstOrNull()
    if (latestSugar != null) {
        val valInt = latestSugar.value.toIntOrNull() ?: 100
        val isFasting = latestSugar.context == "fasting"
        if (isFasting) {
            when {
                valInt < 70 -> tips.add(HealthTip("Low Fasting Blood Sugar", "Fasting blood sugar of $valInt is low. Risk of hypoglycemia.", "alert", "Consume fast-acting carbs."))
                valInt in 70..99 -> tips.add(HealthTip("Normal Fasting Sugar", "Perfect! Your fasting blood sugar level of $valInt is normal.", "good"))
                valInt in 100..125 -> tips.add(HealthTip("Prediabetes threshold caution", "Fasting blood sugar is elevated ($valInt mg/dL).", "caution", "Monitor meal times and reduce processed sugar."))
                else -> tips.add(HealthTip("Diabetes threshold alert", "Fasting blood sugar of $valInt is high.", "alert", "Consult your physician."))
            }
        }
    }

    // Symptoms Tips
    val unresolvedIssues = issues.filter { !it.resolved }
    if (unresolvedIssues.isNotEmpty()) {
        tips.add(
            HealthTip("Active symptoms registered", "You have ${unresolvedIssues.size} unresolved health issues logged.", "caution", "Track symptoms hourly and get plenty of rest.")
        )
    }

    return tips
}

data class HealthTip(
    val title: String,
    val message: String,
    val level: String, // "good", "caution", "alert", "info"
    val suggestion: String? = null
)

// --- Tablet Tracker Section ---
@Composable
fun TabletTrackerSection(viewModel: TrackWiseViewModel) {
    val focusManager = LocalFocusManager.current
    val tabletReminders by viewModel.tabletReminders.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var tabletName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var timeOfDay by remember { mutableStateOf("08:00 AM") }
    var scheduleType by remember { mutableStateOf("Daily") }
    var notes by remember { mutableStateOf("") }

    var editingTabletReminder by remember { mutableStateOf<TabletReminderEntity?>(null) }
    if (editingTabletReminder != null) {
        val rem = editingTabletReminder!!
        var editName by remember(rem) { mutableStateOf(rem.tabletName) }
        var editDosage by remember(rem) { mutableStateOf(rem.dosage) }
        var editTime by remember(rem) { mutableStateOf(rem.timeOfDay) }
        var editNotes by remember(rem) { mutableStateOf(rem.notes ?: "") }

        val scrollState = rememberScrollState()
        LaunchedEffect(scrollState.isScrollInProgress) {
            if (scrollState.isScrollInProgress) {
                focusManager.clearFocus()
            }
        }

        AlertDialog(
            onDismissRequest = { editingTabletReminder = null },
            title = { Text("Edit Medication Reminder 💊", fontWeight = FontWeight.Bold, color = BrandViolet) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Medication Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDosage,
                        onValueChange = { editDosage = it },
                        label = { Text("Dosage (e.g., 1 pill) *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    HealthTimePickerField(
                        timeStr = editTime,
                        label = "Time of Day *",
                        onTimeSelected = { editTime = it },
                        tintColor = BrandViolet,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editName.isNotBlank() && editDosage.isNotBlank()) {
                            val updatedReminder = rem.copy(
                                tabletName = editName,
                                dosage = editDosage,
                                timeOfDay = editTime,
                                notes = editNotes.ifBlank { null }
                            )
                            viewModel.updateTabletReminder(updatedReminder)
                            editingTabletReminder = null
                        }
                    }
                ) {
                    Text("Save Changes", color = BrandViolet, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTabletReminder = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    val today = TrackWiseUtils.getTodayString()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Medicine Taker Tracker 💊", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Track your medications.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showForm = !showForm },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showForm) MaterialTheme.colorScheme.surfaceVariant else BrandViolet,
                        contentColor = if (showForm) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = if (showForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showForm) "Close" else "New Medicine", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Form
            if (showForm) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("Add Medication", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandViolet)

                    CompactTextField(
                        value = tabletName,
                        onValueChange = { tabletName = it },
                        label = "Medication Name *",
                        placeholder = "e.g., Vitamin D3, Paracetamol"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactTextField(
                            value = dosage,
                            onValueChange = { dosage = it },
                            label = "Dosage *",
                            placeholder = "e.g., 1 pill, 5ml",
                            modifier = Modifier.weight(1f)
                        )
                        HealthTimePickerField(
                            timeStr = timeOfDay,
                            label = "Time *",
                            onTimeSelected = { timeOfDay = it },
                            tintColor = BrandViolet,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Schedule type selector
                    Column {
                        Text("Frequency", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf("Daily", "Weekly", "As Needed").forEach { freq ->
                                val isSelected = scheduleType == freq
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) BrandViolet else MaterialTheme.colorScheme.surface)
                                        .clickable { scheduleType = freq }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(freq, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    CompactTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Notes (Optional)",
                        placeholder = "e.g., Take with food"
                    )

                    Button(
                        onClick = {
                            if (tabletName.isNotBlank() && dosage.isNotBlank()) {
                                viewModel.addTabletReminder(
                                    tabletName = tabletName.trim(),
                                    dosage = dosage.trim(),
                                    timeOfDay = timeOfDay.trim(),
                                    scheduleType = scheduleType,
                                    notes = if (notes.isBlank()) null else notes.trim()
                                )
                                tabletName = ""
                                dosage = ""
                                notes = ""
                                showForm = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Medication", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            val pinnedHealthLogIds by viewModel.pinnedHealthLogIds.collectAsState()
            var optionsReminder by remember { mutableStateOf<TabletReminderEntity?>(null) }

            // List & Weekly Analytics
            if (tabletReminders.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "No medications logged. Add one above to start tracking!",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    tabletReminders.forEachIndexed { index, reminder ->
                        val isPinned = pinnedHealthLogIds.contains(reminder.id)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            StaggeredItem(index = index) {
                                // Deserialization helper for taken dates
                            val takenDatesList = try {
                                val array = org.json.JSONArray(reminder.completedDatesJson)
                                val list = mutableListOf<String>()
                                for (i in 0 until array.length()) {
                                    list.add(array.getString(i))
                                }
                                list
                            } catch (e: Exception) {
                                emptyList()
                            }

                            val isTakenToday = takenDatesList.contains(today)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = { editingTabletReminder = reminder },
                                        onLongClick = { optionsReminder = reminder }
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Row 1: Tablet Name, Dosage, Time, Options
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (isPinned) {
                                                Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = BrandAmber, modifier = Modifier.size(12.dp))
                                            }
                                            Text(reminder.tabletName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Box(
                                                modifier = Modifier
                                                    .background(BrandViolet.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(reminder.scheduleType, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
                                            }
                                        }
                                        Text(
                                            text = "${reminder.dosage} · Scheduled: ${reminder.timeOfDay}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        if (!reminder.notes.isNullOrBlank()) {
                                            Text(
                                                text = "Note: ${reminder.notes}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { optionsReminder = reminder },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Taken Today Logging Action Button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isTakenToday) BrandGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            if (isTakenToday) BrandGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.toggleTabletTaken(reminder, today) }
                                        .padding(vertical = 10.dp, horizontal = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isTakenToday) Icons.Default.CheckCircle else Icons.Default.Circle,
                                            contentDescription = null,
                                            tint = if (isTakenToday) BrandGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isTakenToday) "Taken Today (Click to Undo)" else "Log as Taken Today",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isTakenToday) BrandGreen else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // Row 3: 7-Day Analytics Visual Tracker
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("7-Day Analytics History", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        
                                        // Calculate completion rate
                                        val last7Days = (0..6).map { offset ->
                                            val cal = Calendar.getInstance()
                                            cal.add(Calendar.DAY_OF_YEAR, -offset)
                                            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                                            takenDatesList.contains(dateStr)
                                        }
                                        val takenCount = last7Days.count { it }
                                        val ratePercent = (takenCount / 7.0 * 100).toInt()
                                        
                                        Text("Compliance Rate: $ratePercent%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (ratePercent >= 75) BrandGreen else BrandAmber)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // List 7 days from oldest (6 days ago) to newest (today)
                                        (0..6).reversed().forEach { offset ->
                                            val cal = Calendar.getInstance()
                                            cal.add(Calendar.DAY_OF_YEAR, -offset)
                                            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                                            val dayName = SimpleDateFormat("E", Locale.getDefault()).format(cal.time).take(1) // M, T, W...
                                            val wasTaken = takenDatesList.contains(dateStr)

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.clickable { viewModel.toggleTabletTaken(reminder, dateStr) }
                                            ) {
                                                Text(dayName, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(if (wasTaken) BrandGreen else MaterialTheme.colorScheme.surfaceVariant)
                                                        .border(
                                                            1.dp,
                                                            if (wasTaken) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                            CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (wasTaken) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                    } else {
                                                        Text(dayName, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (optionsReminder?.id == reminder.id) {
                                HealthLogItemOptionsDialog(
                                    title = "${reminder.tabletName} (${reminder.dosage})",
                                    isPinned = isPinned,
                                    onEdit = { editingTabletReminder = reminder },
                                    onTogglePin = { viewModel.togglePinHealthLog(reminder.id) },
                                    onDelete = { viewModel.deleteTabletReminder(reminder.id) },
                                    onDismiss = { optionsReminder = null }
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

// --- Period Tracker Section ---
@Composable
fun PeriodTrackerSection(viewModel: TrackWiseViewModel) {
    val periodCycles by viewModel.periodCycles.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }
    var durationDays by remember { mutableStateOf("5") }
    var cycleLengthDays by remember { mutableStateOf("28") }
    var notes by remember { mutableStateOf("") }
    var editingCycleId by remember { mutableStateOf<String?>(null) }

    val selectedSymptoms = remember { mutableStateListOf<String>() }

    val today = TrackWiseUtils.getTodayString()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Period Cycle Tracker 🌸", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Secure client predictions.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Button(
                    onClick = { showForm = !showForm },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showForm) MaterialTheme.colorScheme.surfaceVariant else BrandPink,
                        contentColor = if (showForm) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = if (showForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showForm) "Close" else "Log Period", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Predicting Next Period / Fertility window
            if (periodCycles.isNotEmpty()) {
                val latestCycle = periodCycles.first() // Sorted DESC in DB
                
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val startCal = Calendar.getInstance()
                try {
                    val parsedDate = sdf.parse(latestCycle.startDate)
                    if (parsedDate != null) {
                        startCal.time = parsedDate
                    }
                } catch (e: Exception) {}

                // Next Period Calculation
                val nextPeriodCal = startCal.clone() as Calendar
                nextPeriodCal.add(Calendar.DAY_OF_YEAR, latestCycle.cycleLengthDays)
                val nextPeriodStr = sdf.format(nextPeriodCal.time)

                // Days count till next period
                val todayCal = Calendar.getInstance()
                val diffMs = nextPeriodCal.timeInMillis - todayCal.timeInMillis
                val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()

                // Ovulation predicted day (14 days before next period)
                val ovulationCal = nextPeriodCal.clone() as Calendar
                ovulationCal.add(Calendar.DAY_OF_YEAR, -14)
                val ovulationStr = sdf.format(ovulationCal.time)

                // Fertile window (5 days before ovulation + ovulation day)
                val fertileStartCal = ovulationCal.clone() as Calendar
                fertileStartCal.add(Calendar.DAY_OF_YEAR, -5)
                val isTodayFertile = !todayCal.before(fertileStartCal) && !todayCal.after(ovulationCal)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // prediction card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandPink.copy(alpha = 0.08f)),
                        modifier = Modifier.weight(1.1f).border(1.dp, BrandPink.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("NEXT PERIOD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandPink)
                            Spacer(modifier = Modifier.height(4.dp))
                            val statusText = when {
                                diffDays > 0 -> "In $diffDays days"
                                diffDays == 0 -> "Expected Today"
                                else -> "Overdue by ${-diffDays} days"
                            }
                            Text(statusText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BrandPink)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(nextPeriodStr, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }

                    // Fertile window card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isTodayFertile) BrandViolet.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f).border(1.dp, if (isTodayFertile) BrandViolet.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FERTILITY STATUS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isTodayFertile) BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isTodayFertile) "High (Fertile)" else "Low Fertility",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isTodayFertile) BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Ovulation: $ovulationStr", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            // Form
            if (showForm) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("Add Period Record", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPink)

                    CompactTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = "Start Date * (YYYY-MM-DD)",
                        placeholder = "e.g., 2026-07-01"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactTextField(
                            value = durationDays,
                            onValueChange = { durationDays = it },
                            label = "Bleeding Duration (Days) *",
                            placeholder = "e.g., 5",
                            modifier = Modifier.weight(1f)
                        )
                        CompactTextField(
                            value = cycleLengthDays,
                            onValueChange = { cycleLengthDays = it },
                            label = "Cycle Length (Days) *",
                            placeholder = "e.g., 28",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Symptoms checkchips (safe static rows instead of FlowRow)
                    Column {
                        Text("Logged Symptoms", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Cramps", "Headache", "Bloating").forEach { sym ->
                                    val isSelected = selectedSymptoms.contains(sym)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) BrandPink else MaterialTheme.colorScheme.surface)
                                            .border(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            .clickable {
                                                if (isSelected) selectedSymptoms.remove(sym) else selectedSymptoms.add(sym)
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(sym, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Fatigue", "Mood Swings", "Nausea").forEach { sym ->
                                    val isSelected = selectedSymptoms.contains(sym)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) BrandPink else MaterialTheme.colorScheme.surface)
                                            .border(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            .clickable {
                                                if (isSelected) selectedSymptoms.remove(sym) else selectedSymptoms.add(sym)
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(sym, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }

                    CompactTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Notes / Cycle Intensity",
                        placeholder = "e.g., Medium flow, mild cramping"
                    )

                    Button(
                        onClick = {
                            val dur = durationDays.toIntOrNull() ?: 5
                            val cycl = cycleLengthDays.toIntOrNull() ?: 28
                            if (startDate.isNotBlank()) {
                                val sym = selectedSymptoms.joinToString(",")
                                val nt = if (notes.isBlank()) null else notes.trim()
                                if (editingCycleId != null) {
                                    viewModel.updatePeriodCycle(
                                        oldId = editingCycleId!!,
                                        startDate = startDate.trim(),
                                        durationDays = dur,
                                        cycleLengthDays = cycl,
                                        symptoms = sym,
                                        notes = nt
                                    )
                                } else {
                                    viewModel.addPeriodCycle(
                                        startDate = startDate.trim(),
                                        durationDays = dur,
                                        cycleLengthDays = cycl,
                                        symptoms = sym,
                                        notes = nt
                                    )
                                }
                                startDate = today
                                durationDays = "5"
                                cycleLengthDays = "28"
                                selectedSymptoms.clear()
                                notes = ""
                                editingCycleId = null
                                showForm = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPink),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (editingCycleId != null) "Update Cycle Entry" else "Log Cycle", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Analytics Section: Symptom counts
            if (periodCycles.isNotEmpty()) {
                val symptomCounts = mutableMapOf<String, Int>()
                periodCycles.forEach { cycle ->
                    if (cycle.symptoms.isNotBlank()) {
                        cycle.symptoms.split(",").forEach { s ->
                            val cleanSym = s.trim()
                            if (cleanSym.isNotEmpty()) {
                                symptomCounts[cleanSym] = (symptomCounts[cleanSym] ?: 0) + 1
                            }
                        }
                    }
                }

                if (symptomCounts.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text("Symptom Analytics Breakdown", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        
                        symptomCounts.entries.sortedByDescending { it.value }.take(3).forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BrandPink))
                                    Text(entry.key, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Text("Logged ${entry.value} times", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPink)
                            }
                        }
                    }
                }
            }

            // Cycle history list
            val pinnedHealthLogIds by viewModel.pinnedHealthLogIds.collectAsState()
            var optionsCycle by remember { mutableStateOf<PeriodCycleEntity?>(null) }

            if (periodCycles.isEmpty()) {
                Text(
                    text = "No period cycles logged. Log your first period start above!",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Logged Cycle History", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    periodCycles.forEach { cycle ->
                        val isPinned = pinnedHealthLogIds.contains(cycle.id)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .combinedClickable(
                                        onClick = {
                                            editingCycleId = cycle.id
                                            startDate = cycle.startDate
                                            durationDays = cycle.durationDays.toString()
                                            cycleLengthDays = cycle.cycleLengthDays.toString()
                                            notes = cycle.notes ?: ""
                                            selectedSymptoms.clear()
                                            if (cycle.symptoms.isNotBlank()) {
                                                selectedSymptoms.addAll(cycle.symptoms.split(","))
                                            }
                                            showForm = true
                                        },
                                        onLongClick = { optionsCycle = cycle }
                                    )
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (isPinned) {
                                            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = BrandAmber, modifier = Modifier.size(12.dp))
                                        }
                                        Text("Period Start: ${cycle.startDate}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Text(
                                        text = "Bleeding: ${cycle.durationDays} days · Cycle: ${cycle.cycleLengthDays} days",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    if (cycle.symptoms.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            cycle.symptoms.split(",").forEach { sym ->
                                                Box(
                                                    modifier = Modifier
                                                        .background(BrandPink.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Text(sym, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = BrandPink)
                                                }
                                            }
                                        }
                                    }
                                    if (!cycle.notes.isNullOrBlank()) {
                                        Text(
                                            text = cycle.notes,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = { optionsCycle = cycle }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                            if (optionsCycle?.id == cycle.id) {
                                HealthLogItemOptionsDialog(
                                    title = "Period Start: ${cycle.startDate}",
                                    isPinned = isPinned,
                                    onEdit = {
                                        editingCycleId = cycle.id
                                        startDate = cycle.startDate
                                        durationDays = cycle.durationDays.toString()
                                        cycleLengthDays = cycle.cycleLengthDays.toString()
                                        notes = cycle.notes ?: ""
                                        selectedSymptoms.clear()
                                        if (cycle.symptoms.isNotBlank()) {
                                            selectedSymptoms.addAll(cycle.symptoms.split(","))
                                        }
                                        showForm = true
                                    },
                                    onTogglePin = { viewModel.togglePinHealthLog(cycle.id) },
                                    onDelete = { viewModel.deletePeriodCycle(cycle.id) },
                                    onDismiss = { optionsCycle = null }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseTimeHourMinute(timeStr: String?): Pair<Int, Int> {
    if (timeStr.isNullOrBlank()) return Pair(8, 0)
    return try {
        val uppercase = timeStr.trim().uppercase(Locale.US)
        val isPm = uppercase.contains("PM")
        val isAm = uppercase.contains("AM")
        val clean = uppercase.replace("AM", "").replace("PM", "").trim()
        val parts = clean.split(":")
        var h = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        if (isPm && h < 12) h += 12
        if (isAm && h == 12) h = 0
        Pair(h, m)
    } catch (e: Exception) {
        Pair(8, 0)
    }
}

@Composable
fun HealthTimePickerField(
    timeStr: String, // HH:MM or 12h
    label: String,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    tintColor: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    val (hour, minute) = remember(timeStr) { parseTimeHourMinute(timeStr) }

    val isDarkTheme = MaterialTheme.colorScheme.background.let { (it.red + it.green + it.blue) / 3f < 0.5f }
    val themeId = if (isDarkTheme) android.R.style.Theme_DeviceDefault_Dialog_Alert else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert

    val timePickerDialog = remember(hour, minute, themeId) {
        android.app.TimePickerDialog(
            context,
            themeId,
            { _, selectedHour, selectedMinute ->
                val h12 = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                val amPm = if (selectedHour >= 12) "PM" else "AM"
                val formattedTime = String.format(Locale.US, "%02d:%02d %s", h12, selectedMinute, amPm)
                onTimeSelected(formattedTime)
            },
            hour,
            minute,
            false // 12 hour view
        )
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = timeStr,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = tintColor, modifier = Modifier.size(16.dp)) },
            trailingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit Time", tint = tintColor, modifier = Modifier.size(16.dp)) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { timePickerDialog.show() }
        )
    }
}

