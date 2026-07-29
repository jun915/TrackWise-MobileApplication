package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BirthdayEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Brand accent colors for the new design
val PinkAccent = Color(0xFFFF3B5C)
val LightBgTint = Color(0xFFFFF2F4)
val DarkBgTint = Color(0xFF1E1B29)
val CardLightBg = Color(0xFFFFFFFF)
val CardDarkBg = Color(0xFF272436)
val FieldLightBg = Color(0xFFF6ECEE)
val FieldDarkBg = Color(0xFF322E45)

@Composable
fun ModernAddOccasionDialog(
    onDismissRequest: () -> Unit,
    onSaveOccasion: (BirthdayEntity) -> Unit,
    initialCategory: String = "Birthday",
    editingBirthday: BirthdayEntity? = null
) {
    val isEdit = editingBirthday != null

    // State step: 1 = Form Step, 2 = Appearance Selection Step
    var currentStep by remember { mutableStateOf(1) }

    // Form fields state
    var nameText by remember { mutableStateOf(editingBirthday?.name ?: "") }
    
    // Category / Type
    var selectedType by remember {
        mutableStateOf(
            editingBirthday?.category?.split("|")?.getOrNull(0)
                ?: if (initialCategory.isNotBlank()) initialCategory else "Birthday"
        )
    }
    var selectedRelation by remember {
        mutableStateOf(editingBirthday?.category?.split("|")?.getOrNull(1) ?: "Others")
    }

    // Date state
    val calendar = remember { Calendar.getInstance() }
    var yearVal by remember {
        mutableStateOf(
            editingBirthday?.date?.split("-")?.let {
                if (it.size == 3) it[0].toIntOrNull() ?: calendar.get(Calendar.YEAR)
                else calendar.get(Calendar.YEAR)
            } ?: calendar.get(Calendar.YEAR)
        )
    }
    var monthVal by remember {
        mutableStateOf(
            editingBirthday?.date?.split("-")?.let {
                if (it.size == 3) (it[1].toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1
                else if (it.size == 2) (it[0].toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1
                else calendar.get(Calendar.MONTH)
            } ?: calendar.get(Calendar.MONTH)
        )
    }
    var dayVal by remember {
        mutableStateOf(
            editingBirthday?.date?.split("-")?.let {
                if (it.size == 3) it[2].toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)
                else if (it.size == 2) it[1].toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)
                else calendar.get(Calendar.DAY_OF_MONTH)
            } ?: calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    var ignoreYear by remember {
        mutableStateOf(editingBirthday?.date?.split("-")?.size == 2)
    }
    var hasSelectedDate by remember { mutableStateOf(editingBirthday?.date != null) }

    // Reminders state
    var selectedReminders by remember {
        mutableStateOf(
            editingBirthday?.reminderOptions?.split(",")?.filter { it.isNotBlank() }?.toSet()
                ?: if (editingBirthday?.remindMe == true) setOf("On the day (09:00)", "3 days early (09:00)")
                else setOf("On the day (09:00)", "3 days early (09:00)")
        )
    }
    var isConstantReminder by remember { mutableStateOf(false) }

    // Repeat state
    var repeatOption by remember { mutableStateOf(editingBirthday?.repeatPattern ?: "Every Year") }

    // Show Age / Smart List state
    var showAge by remember { mutableStateOf(true) }
    var smartListOption by remember { mutableStateOf("On the day") }

    // Customization / Appearance State
    var selectedIcon by remember {
        mutableStateOf(
            when (selectedType.lowercase()) {
                "birthday" -> "cake"
                "anniversary", "marriage anniversary" -> "favorite"
                "countdown" -> "hourglass"
                "holiday" -> "star"
                else -> "cake"
            }
        )
    }
    var selectedColorHex by remember { mutableStateOf(editingBirthday?.customTextColor ?: "#FF3B5C") }
    var selectedBgPreset by remember { mutableStateOf(editingBirthday?.customBgImage ?: "solid_pink") }
    var selectedFontStyle by remember { mutableStateOf(editingBirthday?.customFontStyle ?: "Default") }
    var giftNotesText by remember { mutableStateOf(editingBirthday?.giftIdea ?: "") }

    // Dialog state handlers
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showReminderPickerDialog by remember { mutableStateOf(false) }
    var showRepeatPickerDialog by remember { mutableStateOf(false) }
    var showCustomRepeatDialog by remember { mutableStateOf(false) }
    var showTypePickerDialog by remember { mutableStateOf(false) }
    var showSmartListPickerDialog by remember { mutableStateOf(false) }
    var showIconPickerDialog by remember { mutableStateOf(false) }
    var showSmartListInfoDialog by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    val formattedDateString = remember(yearVal, monthVal, dayVal, ignoreYear, hasSelectedDate) {
        if (!hasSelectedDate) "None"
        else {
            val mStr = String.format(Locale.getDefault(), "%02d", monthVal + 1)
            val dStr = String.format(Locale.getDefault(), "%02d", dayVal)
            val cal = Calendar.getInstance().apply { set(if (ignoreYear) 2024 else yearVal, monthVal, dayVal) }
            val monthName = SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
            if (ignoreYear) "$monthName $dStr" else "$monthName $dStr, $yearVal"
        }
    }

    val dateFormattedForDb = remember(yearVal, monthVal, dayVal, ignoreYear) {
        val mStr = String.format(Locale.getDefault(), "%02d", monthVal + 1)
        val dStr = String.format(Locale.getDefault(), "%02d", dayVal)
        if (ignoreYear) "$mStr-$dStr" else "$yearVal-$mStr-$dStr"
    }

    val isDark = MaterialTheme.colorScheme.background.let { (it.red + it.green + it.blue) / 3f < 0.5f }
    val bgColor = if (isDark) DarkBgTint else LightBgTint
    val cardBg = if (isDark) CardDarkBg else CardLightBg
    val fieldBg = if (isDark) FieldDarkBg else FieldLightBg
    val textColor = MaterialTheme.colorScheme.onBackground

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp, vertical = 0.dp),
            color = bgColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentStep == 2) currentStep = 1 else onDismissRequest()
                        }
                    ) {
                        Icon(
                            imageVector = if (currentStep == 2) Icons.Default.ArrowBack else Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textColor
                        )
                    }
                    Text(
                        text = if (currentStep == 1) (if (isEdit) "Edit" else "Add") else "Appearance",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { step ->
                    if (step == 1) {
                        // STEP 1: Main Add Form
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(4.dp))

                            // Top Avatar / Icon Badge with Pencil Edit
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF86E3CE))
                                    .clickable { showIconPickerDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getVectorForIconKey(selectedIcon),
                                    contentDescription = "Occasion Icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0E0E0))
                                        .border(1.5.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Icon",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            // Name Input Box
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = fieldBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = nameText,
                                    onValueChange = {
                                        nameText = it
                                        if (it.isNotBlank()) nameError = false
                                    },
                                    placeholder = {
                                        Text("Name", color = textColor.copy(alpha = 0.4f), fontSize = 16.sp)
                                    },
                                    singleLine = true,
                                    isError = nameError,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        errorBorderColor = MaterialTheme.colorScheme.error,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                        .testTag("occasion_name_input")
                                )
                            }

                            // Card Group 1: Date, Reminder, Repeat
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = cardBg,
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Row 1: Date
                                    FormOptionRow(
                                        label = "Date",
                                        valueText = formattedDateString,
                                        onClick = { showDatePickerDialog = true }
                                    )

                                    HorizontalDivider(color = textColor.copy(alpha = 0.08f), thickness = 0.8.dp)

                                    // Row 2: Reminder
                                    val reminderSummary = remember(selectedReminders) {
                                        if (selectedReminders.isEmpty() || selectedReminders.contains("None")) "None"
                                        else selectedReminders.joinToString(", ") {
                                            it.replace(" (09:00)", "").replace(" (9AM)", "")
                                        }
                                    }
                                    FormOptionRow(
                                        label = "Reminder",
                                        valueText = reminderSummary,
                                        showClearButton = reminderSummary != "None",
                                        onClear = { selectedReminders = setOf("None") },
                                        onClick = { showReminderPickerDialog = true }
                                    )

                                    HorizontalDivider(color = textColor.copy(alpha = 0.08f), thickness = 0.8.dp)

                                    // Row 3: Repeat
                                    FormOptionRow(
                                        label = "Repeat",
                                        valueText = repeatOption,
                                        showClearButton = repeatOption != "None",
                                        onClear = { repeatOption = "None" },
                                        onClick = { showRepeatPickerDialog = true }
                                    )
                                }
                            }

                            // Card Group 2: Type, Show Age, Show in Smart List
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = cardBg,
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Row 1: Type
                                    FormOptionRow(
                                        label = "Type",
                                        valueText = selectedType,
                                        onClick = { showTypePickerDialog = true }
                                    )

                                    HorizontalDivider(color = textColor.copy(alpha = 0.08f), thickness = 0.8.dp)

                                    // Row 2: Show Age (Switch)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Show Age",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = textColor
                                        )
                                        Switch(
                                            checked = showAge,
                                            onCheckedChange = { showAge = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = PinkAccent
                                            )
                                        )
                                    }

                                    HorizontalDivider(color = textColor.copy(alpha = 0.08f), thickness = 0.8.dp)

                                    // Row 3: Show in Smart List
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showSmartListPickerDialog = true }
                                            .padding(horizontal = 18.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Show in Smart List",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = textColor
                                            )
                                            Icon(
                                                imageVector = Icons.Default.HelpOutline,
                                                contentDescription = "Help",
                                                tint = textColor.copy(alpha = 0.4f),
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clickable { showSmartListInfoDialog = true }
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = smartListOption,
                                                fontSize = 14.sp,
                                                color = textColor.copy(alpha = 0.6f)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = textColor.copy(alpha = 0.4f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Bottom "Next" Button
                            Button(
                                onClick = {
                                    if (nameText.isBlank()) {
                                        nameError = true
                                    } else {
                                        if (!hasSelectedDate) {
                                            hasSelectedDate = true
                                        }
                                        currentStep = 2
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PinkAccent),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("occasion_next_button")
                            ) {
                                Text("Next", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    } else {
                        // STEP 2: Appearance Selection Screen
                        AppearanceSelectionScreen(
                            name = nameText.ifBlank { "Occasion" },
                            dateStr = formattedDateString,
                            categoryType = selectedType,
                            selectedIcon = selectedIcon,
                            onIconSelected = { selectedIcon = it },
                            selectedColorHex = selectedColorHex,
                            onColorSelected = { selectedColorHex = it },
                            selectedBgPreset = selectedBgPreset,
                            onBgPresetSelected = { selectedBgPreset = it },
                            selectedFontStyle = selectedFontStyle,
                            onFontStyleSelected = { selectedFontStyle = it },
                            selectedRelation = selectedRelation,
                            onRelationSelected = { selectedRelation = it },
                            giftNotesText = giftNotesText,
                            onNotesChanged = { giftNotesText = it },
                            onBackClick = { currentStep = 1 },
                            onSaveClick = {
                                val finalEntity = BirthdayEntity(
                                    id = editingBirthday?.id ?: "birthday-${System.currentTimeMillis()}",
                                    userId = editingBirthday?.userId ?: "local",
                                    name = nameText.trim(),
                                    date = dateFormattedForDb,
                                    giftIdea = giftNotesText.ifBlank { null },
                                    category = "$selectedType|$selectedRelation",
                                    remindMe = !selectedReminders.contains("None") && selectedReminders.isNotEmpty(),
                                    reminderDate = null,
                                    reminderTime = "09:00",
                                    isPinned = editingBirthday?.isPinned ?: false,
                                    customBgImage = selectedBgPreset,
                                    customTextColor = selectedColorHex,
                                    customFontStyle = selectedFontStyle,
                                    reminderOptions = selectedReminders.joinToString(","),
                                    repeatPattern = repeatOption,
                                    countingMode = if (selectedType.lowercase() == "countdown") "Count Down" else "Count Up"
                                )
                                onSaveOccasion(finalEntity)
                                onDismissRequest()
                            }
                        )
                    }
                }
            }
        }
    }

    // --- SUB-DIALOGS & POPUPS ---

    // 1. DATE PICKER DIALOG (Screenshots 2 & 3)
    if (showDatePickerDialog) {
        Dialog(onDismissRequest = { showDatePickerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Date",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    // Wheel Pickers Container
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Month Wheel
                        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        WheelPickerColumn(
                            items = months,
                            selectedIndex = monthVal,
                            onSelectedIndexChanged = { monthVal = it },
                            modifier = Modifier.weight(1f)
                        )

                        // Day Wheel
                        val days = (1..31).map { it.toString() }
                        WheelPickerColumn(
                            items = days,
                            selectedIndex = (dayVal - 1).coerceIn(0, 30),
                            onSelectedIndexChanged = { dayVal = it + 1 },
                            modifier = Modifier.weight(1f)
                        )

                        // Year Wheel (Hidden if ignoreYear is True)
                        if (!ignoreYear) {
                            val currentYr = calendar.get(Calendar.YEAR)
                            val years = (1950..2035).map { it.toString() }
                            val initialYearIdx = (yearVal - 1950).coerceIn(0, years.size - 1)
                            WheelPickerColumn(
                                items = years,
                                selectedIndex = initialYearIdx,
                                onSelectedIndexChanged = { yearVal = 1950 + it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Ignore Year Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Ignore Year",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )
                        Switch(
                            checked = ignoreYear,
                            onCheckedChange = { ignoreYear = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PinkAccent
                            )
                        )
                    }

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showDatePickerDialog = false }) {
                            Text("Cancel", color = PinkAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                hasSelectedDate = true
                                showDatePickerDialog = false
                            }
                        ) {
                            Text("OK", color = PinkAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 2. REMINDER PICKER DIALOG (Screenshot 4)
    if (showReminderPickerDialog) {
        Dialog(onDismissRequest = { showReminderPickerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Reminder",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    val reminderOptions = listOf(
                        "None",
                        "On the day (09:00)",
                        "1 day early (09:00)",
                        "2 days early (09:00)",
                        "3 days early (09:00)",
                        "1 week early (09:00)",
                        "Custom"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        reminderOptions.forEach { opt ->
                            val isSelected = if (opt == "None") {
                                selectedReminders.contains("None") || selectedReminders.isEmpty()
                            } else {
                                selectedReminders.contains(opt)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val newSet = selectedReminders.toMutableSet()
                                        if (opt == "None") {
                                            newSet.clear()
                                            newSet.add("None")
                                        } else {
                                            newSet.remove("None")
                                            if (isSelected) {
                                                newSet.remove(opt)
                                            } else {
                                                newSet.add(opt)
                                            }
                                            if (newSet.isEmpty()) newSet.add("None")
                                        }
                                        selectedReminders = newSet
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = opt,
                                    fontSize = 15.sp,
                                    color = if (isSelected) PinkAccent else textColor
                                )
                                if (isSelected && opt != "None") {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = PinkAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else if (opt == "Custom") {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "More",
                                        tint = textColor.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = textColor.copy(alpha = 0.1f))

                    // Constant Reminder Switch Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Constant Reminder",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                            Text("👑", fontSize = 14.sp)
                        }
                        Switch(
                            checked = isConstantReminder,
                            onCheckedChange = { isConstantReminder = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PinkAccent
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showReminderPickerDialog = false }) {
                            Text("Cancel", color = PinkAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { showReminderPickerDialog = false }) {
                            Text("OK", color = PinkAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 3. REPEAT PICKER DIALOG (Screenshot 5)
    if (showRepeatPickerDialog) {
        Dialog(onDismissRequest = { showRepeatPickerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Repeat",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    val repeatChoices = listOf(
                        "None",
                        "Daily",
                        "Weekly (Wed)",
                        "Monthly (The 29 day)",
                        "Yearly (on Jul 29)",
                        "Custom"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeatChoices.forEach { choice ->
                            val isSel = repeatOption == choice || (choice.startsWith("Yearly") && repeatOption.contains("Year"))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (choice == "Custom") {
                                            showRepeatPickerDialog = false
                                            showCustomRepeatDialog = true
                                        } else {
                                            repeatOption = choice
                                            showRepeatPickerDialog = false
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = choice,
                                    fontSize = 15.sp,
                                    color = if (isSel) PinkAccent else textColor
                                )
                                if (isSel && choice != "Custom") {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = PinkAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showRepeatPickerDialog = false }) {
                            Text("Cancel", color = PinkAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 4. CUSTOM REPEAT DIALOG (Screenshots 6 & 7)
    if (showCustomRepeatDialog) {
        CustomRepeatDialog(
            isDark = isDark,
            onDismiss = { showCustomRepeatDialog = false },
            onConfirm = { customPattern ->
                repeatOption = customPattern
                showCustomRepeatDialog = false
            }
        )
    }

    // 5. TYPE PICKER DIALOG (Screenshot 8)
    if (showTypePickerDialog) {
        Dialog(onDismissRequest = { showTypePickerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Type",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    val typesList = listOf("Countdown", "Anniversary", "Birthday", "Holiday")

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        typesList.forEach { typeItem ->
                            val isSel = selectedType.equals(typeItem, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedType = typeItem
                                        selectedIcon = when (typeItem.lowercase()) {
                                            "birthday" -> "cake"
                                            "anniversary" -> "favorite"
                                            "countdown" -> "hourglass"
                                            "holiday" -> "star"
                                            else -> "cake"
                                        }
                                        showTypePickerDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = typeItem,
                                    fontSize = 15.sp,
                                    color = if (isSel) PinkAccent else textColor
                                )
                                if (isSel) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = PinkAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTypePickerDialog = false }) {
                            Text("Cancel", color = PinkAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 6. SMART LIST PICKER DIALOG
    if (showSmartListPickerDialog) {
        Dialog(onDismissRequest = { showSmartListPickerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Show in Smart List",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    val options = listOf("On the day", "1 day early", "3 days early", "1 week early", "Always", "Never")

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        options.forEach { opt ->
                            val isSel = smartListOption == opt
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        smartListOption = opt
                                        showSmartListPickerDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = opt,
                                    fontSize = 15.sp,
                                    color = if (isSel) PinkAccent else textColor
                                )
                                if (isSel) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = PinkAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSmartListPickerDialog = false }) {
                            Text("Cancel", color = PinkAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 7. SMART LIST INFO DIALOG
    if (showSmartListInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSmartListInfoDialog = false },
            title = { Text("Smart List", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Smart List automatically highlights upcoming occasions on your main dashboard so you never miss an important date or anniversary.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showSmartListInfoDialog = false }) {
                    Text("Got it", color = PinkAccent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 8. ICON PICKER DIALOG
    if (showIconPickerDialog) {
        Dialog(onDismissRequest = { showIconPickerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Choose Icon",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    val iconKeys = listOf(
                        "cake", "favorite", "hourglass", "star", "gift", "party",
                        "balloon", "trophy", "bell", "flower", "flight", "music",
                        "coffee", "fire", "medal", "pets"
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(iconKeys) { key ->
                            val isSel = selectedIcon == key
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) PinkAccent else fieldBg)
                                    .clickable {
                                        selectedIcon = key
                                        showIconPickerDialog = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getVectorForIconKey(key),
                                    contentDescription = key,
                                    tint = if (isSel) Color.White else textColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper Row component for Form Options
@Composable
private fun FormOptionRow(
    label: String,
    valueText: String,
    showClearButton: Boolean = false,
    onClear: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = valueText,
                fontSize = 14.sp,
                color = textColor.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (showClearButton && onClear != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = textColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Custom Wheel Picker Column using LazyColumn
@Composable
fun WheelPickerColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.size - 1))
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex
            if (centerIndex in items.indices && centerIndex != selectedIndex) {
                onSelectedIndexChanged(centerIndex)
            }
        }
    }

    Box(
        modifier = modifier.height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Selection Highlight bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(10.dp)
        ) {}

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = 70.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { idx, item ->
                val isSelected = idx == listState.firstVisibleItemIndex
                Text(
                    text = item,
                    fontSize = if (isSelected) 18.sp else 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) PinkAccent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .wrapContentHeight()
                        .scale(if (isSelected) 1.1f else 0.9f)
                        .clickable { onSelectedIndexChanged(idx) }
                )
            }
        }
    }
}

// Custom Repeat Dialog Composable (Screenshots 6 & 7)
@Composable
private fun CustomRepeatDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var freqNum by remember { mutableStateOf(1) }
    var freqUnit by remember { mutableStateOf("Year") } // Year, Month, Week, Day
    var activeTab by remember { mutableStateOf(0) } // 0 = Each, 1 = On the ...

    // "Each" Tab selection (day of month)
    var selectedCalendarDay by remember { mutableStateOf(29) }

    // "On the ..." Tab selection (Month, Ordinal, DayOfWeek)
    var selectedMonthIdx by remember { mutableStateOf(6) } // July
    var selectedOccurrenceIdx by remember { mutableStateOf(4) } // Fifth
    var selectedDayOfWeekIdx by remember { mutableStateOf(2) } // Wednesday

    val monthsList = listOf("May", "June", "July", "August", "September", "October", "November", "December", "January", "February", "March", "April")
    val ordinalList = listOf("First", "Second", "Third", "Fourth", "Fifth", "Last")
    val daysOfWeekList = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    val summaryText = remember(freqNum, freqUnit, activeTab, selectedCalendarDay, selectedMonthIdx, selectedOccurrenceIdx, selectedDayOfWeekIdx) {
        if (activeTab == 0) {
            "Yearly on July $selectedCalendarDay"
        } else {
            val monthStr = monthsList.getOrNull(selectedMonthIdx) ?: "July"
            val ordStr = ordinalList.getOrNull(selectedOccurrenceIdx) ?: "Fifth"
            val dayStr = daysOfWeekList.getOrNull(selectedDayOfWeekIdx) ?: "Wednesday"
            "Yearly on the $ordStr $dayStr of $monthStr"
        }
    }

    val cardBg = if (isDark) CardDarkBg else CardLightBg
    val textColor = MaterialTheme.colorScheme.onBackground

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp),
            color = if (isDark) DarkBgTint else LightBgTint
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Custom", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text("👑", fontSize = 16.sp)
                    }
                    IconButton(onClick = { onConfirm(summaryText) }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = PinkAccent)
                    }
                }

                // Frequency Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Frequency",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Every", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor)

                            // Frequency Number Wheel
                            WheelPickerColumn(
                                items = (1..30).map { it.toString() },
                                selectedIndex = freqNum - 1,
                                onSelectedIndexChanged = { freqNum = it + 1 },
                                modifier = Modifier.weight(1f)
                            )

                            // Unit Wheel
                            val units = listOf("Year", "Month", "Week", "Day")
                            WheelPickerColumn(
                                items = units,
                                selectedIndex = units.indexOf(freqUnit).coerceAtLeast(0),
                                onSelectedIndexChanged = { freqUnit = units[it] },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Text(
                    text = summaryText,
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Tabs: "Each" vs "On the ..."
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.clickable { activeTab = 0 },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Each",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeTab == 0) PinkAccent else textColor.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (activeTab == 0) {
                                    Box(
                                        modifier = Modifier
                                            .width(36.dp)
                                            .height(3.dp)
                                            .background(PinkAccent, CircleShape)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.clickable { activeTab = 1 },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "On the ...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeTab == 1) PinkAccent else textColor.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (activeTab == 1) {
                                    Box(
                                        modifier = Modifier
                                            .width(50.dp)
                                            .height(3.dp)
                                            .background(PinkAccent, CircleShape)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (activeTab == 0) {
                            // "Each" Tab Content: Month Calendar Grid (Screenshot 6)
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("July", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    Row {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = textColor.copy(alpha = 0.4f))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = textColor.copy(alpha = 0.4f))
                                    }
                                }

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(7),
                                    modifier = Modifier.height(240.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(31) { dayIdx ->
                                        val day = dayIdx + 1
                                        val isSel = selectedCalendarDay == day
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (isSel) PinkAccent else Color.Transparent)
                                                .clickable { selectedCalendarDay = day },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$day",
                                                fontSize = 14.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) Color.White else textColor
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // "On the ..." Tab Content: 3 Wheel Pickers (Screenshot 7)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                WheelPickerColumn(
                                    items = monthsList,
                                    selectedIndex = selectedMonthIdx,
                                    onSelectedIndexChanged = { selectedMonthIdx = it },
                                    modifier = Modifier.weight(1f)
                                )
                                WheelPickerColumn(
                                    items = ordinalList,
                                    selectedIndex = selectedOccurrenceIdx,
                                    onSelectedIndexChanged = { selectedOccurrenceIdx = it },
                                    modifier = Modifier.weight(1f)
                                )
                                WheelPickerColumn(
                                    items = daysOfWeekList,
                                    selectedIndex = selectedDayOfWeekIdx,
                                    onSelectedIndexChanged = { selectedDayOfWeekIdx = it },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// STEP 2: APPEARANCE SELECTION SCREEN
@Composable
private fun AppearanceSelectionScreen(
    name: String,
    dateStr: String,
    categoryType: String,
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    selectedBgPreset: String,
    onBgPresetSelected: (String) -> Unit,
    selectedFontStyle: String,
    onFontStyleSelected: (String) -> Unit,
    selectedRelation: String,
    onRelationSelected: (String) -> Unit,
    giftNotesText: String,
    onNotesChanged: (String) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.let { (it.red + it.green + it.blue) / 3f < 0.5f }
    val cardBg = if (isDark) CardDarkBg else CardLightBg
    val textColor = MaterialTheme.colorScheme.onBackground
    val parsedColor = remember(selectedColorHex) {
        try { Color(android.graphics.Color.parseColor(selectedColorHex)) }
        catch (e: Exception) { PinkAccent }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Live Card Preview
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(getBrushForPreset(selectedBgPreset, parsedColor))
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(parsedColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getVectorForIconKey(selectedIcon),
                            contentDescription = null,
                            tint = parsedColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$categoryType • $dateStr",
                            fontSize = 13.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = parsedColor,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = selectedRelation,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Section 1: Icon Chooser
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Icon", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)

                val icons = listOf(
                    "cake", "favorite", "hourglass", "star", "gift", "party",
                    "balloon", "trophy", "bell", "flower", "flight", "music"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    icons.take(6).forEach { key ->
                        val isSel = selectedIcon == key
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSel) parsedColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .clickable { onIconSelected(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getVectorForIconKey(key),
                                contentDescription = key,
                                tint = if (isSel) Color.White else textColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Accent Color Palette
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Accent Color", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)

                val hexColors = listOf(
                    "#FF3B5C", "#FFB300", "#00BCD4", "#7C3AED",
                    "#10B981", "#2196F3", "#FF5722", "#E91E63"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    hexColors.forEach { hex ->
                        val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { PinkAccent }
                        val isSel = selectedColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isSel) 3.dp else 0.dp,
                                    color = if (isSel) textColor else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onColorSelected(hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSel) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Relationship / Tag
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Relationship Tag", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)

                val relations = listOf("Friend", "Family", "Relative", "Work", "Lover", "Others")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    relations.take(4).forEach { rel ->
                        val isSel = selectedRelation.equals(rel, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) parsedColor.copy(alpha = 0.15f) else Color.Transparent)
                                .border(1.dp, if (isSel) parsedColor else textColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .clickable { onRelationSelected(rel) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(rel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isSel) parsedColor else textColor)
                        }
                    }
                }
            }
        }

        // Section 4: Notes & Gift Idea Field
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Notes / Gift Ideas", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                OutlinedTextField(
                    value = giftNotesText,
                    onValueChange = onNotesChanged,
                    placeholder = { Text("Add gift ideas, party location, or notes...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action Buttons: Back & Save
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBackClick,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Text("Back", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onSaveClick,
                colors = ButtonDefaults.buttonColors(containerColor = PinkAccent),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(2f)
                    .height(52.dp)
                    .testTag("occasion_save_button")
            ) {
                Text("Save Occasion", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Helpers
fun getVectorForIconKey(key: String): ImageVector {
    return when (key.lowercase()) {
        "cake" -> Icons.Default.Cake
        "favorite", "heart" -> Icons.Default.Favorite
        "hourglass" -> Icons.Default.HourglassEmpty
        "star" -> Icons.Default.Star
        "gift" -> Icons.Default.Redeem
        "party" -> Icons.Default.Celebration
        "balloon" -> Icons.Default.Brightness7
        "trophy" -> Icons.Default.EmojiEvents
        "bell" -> Icons.Default.Notifications
        "flower" -> Icons.Default.LocalFlorist
        "flight", "plane" -> Icons.Default.Flight
        "music" -> Icons.Default.MusicNote
        "coffee" -> Icons.Default.LocalCafe
        "fire" -> Icons.Default.Whatshot
        "medal" -> Icons.Default.MilitaryTech
        "pets" -> Icons.Default.Pets
        else -> Icons.Default.Cake
    }
}

fun getBrushForPreset(presetKey: String, accentColor: Color): Brush {
    return when (presetKey) {
        "solid_pink" -> Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.12f), accentColor.copy(alpha = 0.04f)))
        "sunset" -> Brush.horizontalGradient(listOf(Color(0xFFFF7E5F).copy(alpha = 0.2f), Color(0xFFFEB47B).copy(alpha = 0.2f)))
        "ocean" -> Brush.horizontalGradient(listOf(Color(0xFF2193B0).copy(alpha = 0.2f), Color(0xFF6DD5ED).copy(alpha = 0.2f)))
        "velvet" -> Brush.verticalGradient(listOf(Color(0xFF8E2DE2).copy(alpha = 0.2f), Color(0xFF4A00E0).copy(alpha = 0.2f)))
        else -> Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.12f), accentColor.copy(alpha = 0.04f)))
    }
}
