package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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

    var activeSubTab by remember { mutableStateOf(0) }
    val tabs = listOf("Metrics Log", "Exercise", "Symptom Log", "Sleep")
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

        // --- Tabs Selection ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabs.forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (activeSubTab == index) BrandViolet else Color.Transparent)
                            .clickable { activeSubTab = index }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeSubTab == index) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // --- Sub-Tab Contents ---
        when (activeSubTab) {
            0 -> { // Body Metrics Logs
                item { WeightLogSection(viewModel = viewModel, entries = weightEntries) }
                item { VitalsLogSection(viewModel = viewModel, readings = vitalReadings) }
            }
            1 -> { // Exercises
                item { ExerciseLogSection(viewModel = viewModel, logs = exerciseLogs) }
            }
            2 -> { // Symptom Logs
                item { SymptomLogSection(viewModel = viewModel, logs = healthIssueLogs) }
            }
            3 -> { // Sleep Tracker
                item { SleepLogSection(viewModel = viewModel, sleepLogs = sleepLogs) }
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
