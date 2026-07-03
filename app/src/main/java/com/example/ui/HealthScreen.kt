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
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.utils.TrackWiseUtils
import kotlin.math.pow

import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource

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
    val isWoman = userProfile?.gender?.lowercase()?.let { it.contains("female") || it.contains("women") } == true

    val tabs = remember(isWoman) {
        val list = mutableListOf("Metrics Log", "Exercise", "Symptom Log", "Sleep", "Tablets")
        if (isWoman) {
            list.add("Period Tracker")
        }
        list
    }

    var activeSubTab by remember { mutableStateOf(0) }
    LaunchedEffect(tabs) {
        if (activeSubTab >= tabs.size) {
            activeSubTab = 0
        }
    }

    val focusManager = LocalFocusManager.current

    // Dynamic BMI
    val height = currentUser?.heightCm ?: 0.0
    val weight = currentUser?.weightKg ?: 0.0
    val bmi = if (height > 50 && weight > 20) {
        weight / (height / 100.0).pow(2)
    } else 0.0

    LazyColumn(
        modifier = modifier
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
        // --- Header ---
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Health Hub",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Track water, body metrics, exercises, and symptoms safely.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // --- Rule-Based Clinical Health Analysis Tips (Section 10.1 & Part 16) ---
        item {
            val tips = evaluateHealthTips(bmi, waterLogs, vitalReadings, exerciseLogs, healthIssueLogs)
            if (tips.isNotEmpty()) {
                HealthTipsPanel(tips = tips)
            }
        }

        // --- BMI Tracker & Water Logs (First row of visual analytics) ---
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BMICard(bmi = bmi, weight = weight, height = height, modifier = Modifier.weight(1f))
                WaterTrackerCard(viewModel = viewModel, logs = waterLogs, modifier = Modifier.weight(1.2f))
            }
        }

        // --- Tabs Selection Dropdown ---
        item {
            var dropdownExpanded by remember { mutableStateOf(false) }
            val activeTabLabel = tabs.getOrNull(activeSubTab) ?: "Metrics Log"
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dropdownExpanded = true }
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val icon = when (activeTabLabel) {
                                "Metrics Log" -> Icons.Default.Favorite
                                "Exercise" -> Icons.Default.DirectionsRun
                                "Symptom Log" -> Icons.Default.Info
                                "Sleep" -> Icons.Default.NightsStay
                                "Tablets" -> Icons.Default.CheckCircle
                                "Period Tracker" -> Icons.Default.Face
                                else -> Icons.Default.Star
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = BrandViolet,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = activeTabLabel.uppercase(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandViolet
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Select View",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = BrandViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    tabs.forEachIndexed { index, label ->
                        val selected = activeSubTab == index
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) BrandViolet else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                activeSubTab = index
                                dropdownExpanded = false
                            },
                            leadingIcon = {
                                val itemIcon = when (label) {
                                    "Metrics Log" -> Icons.Default.Favorite
                                    "Exercise" -> Icons.Default.DirectionsRun
                                    "Symptom Log" -> Icons.Default.Info
                                    "Sleep" -> Icons.Default.NightsStay
                                    "Tablets" -> Icons.Default.CheckCircle
                                    "Period Tracker" -> Icons.Default.Face
                                    else -> Icons.Default.Star
                                }
                                Icon(
                                    imageVector = itemIcon,
                                    contentDescription = null,
                                    tint = if (selected) BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
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
            "Tablets" -> {
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
            Text("BODY MASS INDEX", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
            
            Text(
                text = if (bmi > 0) "%.1f".format(bmi) else "—",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = categoryColor,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = category,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = categoryColor
            )

            Text(
                text = "${weight.toInt()} kg · ${height.toInt()} cm",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
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
            Text("HYDRATION METER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandCyan)

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

            Text("Glasses today", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
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
    var weightInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

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
                    onValueChange = { weightInput = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1.5f)
                )
            }

            Button(
                onClick = {
                    val w = weightInput.toDoubleOrNull()
                    if (w != null) {
                        viewModel.logWeight(w, if (notesInput.isBlank()) null else notesInput)
                        weightInput = ""
                        notesInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPink),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Weight Entry", color = Color.White)
            }

            // Histoy List
            if (entries.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                entries.take(3).forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${entry.date} · ${entry.weightKg} kg", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.deleteWeightEntry(entry.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = BrandRose, modifier = Modifier.size(14.dp))
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
    var vitalType by remember { mutableStateOf("blood_sugar") } // "blood_sugar", "blood_pressure"
    var valueInput by remember { mutableStateOf("") }
    var contextInput by remember { mutableStateOf("fasting") } // "fasting", "post_meal", "random"

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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = valueInput,
                    onValueChange = { valueInput = it },
                    label = { Text(if (vitalType == "blood_sugar") "Value (mg/dL)" else "BP (e.g. 120/80)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                if (vitalType == "blood_sugar") {
                    OutlinedTextField(
                        value = contextInput,
                        onValueChange = { contextInput = it },
                        label = { Text("Context (fasting, post_meal)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Button(
                onClick = {
                    if (valueInput.isNotBlank()) {
                        viewModel.logVital(
                            type = vitalType,
                            value = valueInput,
                            context = if (vitalType == "blood_sugar") contextInput else "resting",
                            notes = null
                        )
                        valueInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Vital Log", color = Color.White)
            }

            // Readings history
            val filteredReadings = readings.filter { it.type == vitalType }
            if (filteredReadings.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                filteredReadings.take(3).forEach { read ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${read.date} · ${read.value} (${read.context?.uppercase()})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.deleteVitalReading(read.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = BrandRose, modifier = Modifier.size(14.dp))
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
    var typeInput by remember { mutableStateOf("Walking") }
    var durationInput by remember { mutableStateOf("") }

    val exerciseTypes = listOf("Walking", "Running", "Gym/Weights", "Yoga", "Cycling")

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
                OutlinedTextField(
                    value = typeInput,
                    onValueChange = { typeInput = it },
                    label = { Text("Exercise Type") },
                    singleLine = true,
                    modifier = Modifier.weight(1.5f)
                )

                OutlinedTextField(
                    value = durationInput,
                    onValueChange = { durationInput = it },
                    label = { Text("Duration (mins)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    val d = durationInput.toIntOrNull() ?: 0
                    if (typeInput.isNotBlank()) {
                        viewModel.logExercise(typeInput, d, true, null)
                        durationInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log Activity Session", color = Color.White)
            }

            if (logs.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                logs.take(3).forEach { log ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${log.date} · ${log.exerciseType} (${log.durationMinutes} mins)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.deleteExerciseLog(log.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = BrandRose, modifier = Modifier.size(14.dp))
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
    var issueName by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("mild") } // "mild", "moderate", "severe"

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

            Button(
                onClick = {
                    if (issueName.isNotBlank()) {
                        viewModel.logHealthIssue(
                            issueId = "issue-${System.currentTimeMillis()}",
                            issueName = issueName,
                            severity = severity,
                            notes = null
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

            if (logs.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                logs.take(3).forEach { log ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${log.date} · ${log.issueName} (${log.severity.uppercase()})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.deleteHealthIssueLog(log.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = BrandRose, modifier = Modifier.size(14.dp))
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
    var sleepStart by remember { mutableStateOf("22:30") }
    var sleepEnd by remember { mutableStateOf("06:30") }
    var notes by remember { mutableStateOf("") }

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
                CompactTextField(
                    value = sleepStart,
                    onValueChange = { sleepStart = it },
                    label = "Sleep Start (HH:MM)",
                    placeholder = "22:30",
                    modifier = Modifier.weight(1f)
                )

                CompactTextField(
                    value = sleepEnd,
                    onValueChange = { sleepEnd = it },
                    label = "Wake Up (HH:MM)",
                    placeholder = "06:30",
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
                    viewModel.addSleepLog(
                        hoursSlept = calculatedHours,
                        startTime = sleepStart,
                        endTime = sleepEnd,
                        notes = notes.ifBlank { "Logged sleep" }
                    )
                    notes = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log Sleep", color = Color.White, fontWeight = FontWeight.Bold)
            }

            if (sleepLogs.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Text(
                    text = "SLEEP LOG HISTORY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )

                sleepLogs.forEach { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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

                        IconButton(onClick = { viewModel.deleteSleepLog(log.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete sleep entry", tint = BrandRose, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

fun calculateHoursDifference(start: String, end: String): Double {
    try {
        val partsStart = start.split(":")
        val partsEnd = end.split(":")
        if (partsStart.size == 2 && partsEnd.size == 2) {
            val hStart = partsStart[0].toIntOrNull() ?: 22
            val mStart = partsStart[1].toIntOrNull() ?: 30
            val hEnd = partsEnd[0].toIntOrNull() ?: 6
            val mEnd = partsEnd[1].toIntOrNull() ?: 30

            var diffMin = (hEnd * 60 + mEnd) - (hStart * 60 + mStart)
            if (diffMin < 0) {
                diffMin += 24 * 60 // spanned midnight
            }
            return diffMin / 60.0
        }
    } catch (e: Exception) {}
    return 8.0
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
    val tabletReminders by viewModel.tabletReminders.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var tabletName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var timeOfDay by remember { mutableStateOf("08:00 AM") }
    var scheduleType by remember { mutableStateOf("Daily") }
    var notes by remember { mutableStateOf("") }

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
                    Text("Tablet Taker Tracker 💊", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Add and track your medication times & analytics.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
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
                    Text(if (showForm) "Close" else "New Tablet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        CompactTextField(
                            value = timeOfDay,
                            onValueChange = { timeOfDay = it },
                            label = "Time *",
                            placeholder = "e.g., 08:00 AM, Night",
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

            // List & Weekly Analytics
            if (tabletReminders.isEmpty()) {
                Text(
                    text = "No medications logged. Add one above to start tracking!",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    tabletReminders.forEach { reminder ->
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
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Row 1: Tablet Name, Dosage, Time, Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    onClick = { viewModel.deleteTabletReminder(reminder.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete medication", tint = BrandRose, modifier = Modifier.size(16.dp))
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

                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(dayName, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(if (wasTaken) BrandGreen else MaterialTheme.colorScheme.surface)
                                                    .border(
                                                        1.dp,
                                                        if (wasTaken) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (wasTaken) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
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
                    Text("Secure client-side cycle prediction & health analytics.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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
                                viewModel.addPeriodCycle(
                                    startDate = startDate.trim(),
                                    durationDays = dur,
                                    cycleLengthDays = cycl,
                                    symptoms = selectedSymptoms.joinToString(","),
                                    notes = if (notes.isBlank()) null else notes.trim()
                                )
                                startDate = today
                                durationDays = "5"
                                cycleLengthDays = "28"
                                selectedSymptoms.clear()
                                notes = ""
                                showForm = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPink),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Log Cycle", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Period Start: ${cycle.startDate}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                            IconButton(onClick = { viewModel.deletePeriodCycle(cycle.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete cycle", tint = BrandRose, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
