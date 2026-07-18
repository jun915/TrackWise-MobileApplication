package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.UserProfileEntity
import com.example.ui.theme.*
import com.example.utils.TrackWiseUtils

data class ProfileBadgeSpec(
    val days: Int,
    val name: String,
    val medal: String,
    val tier: String,
    val desc: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: TrackWiseViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dbProfile by viewModel.userProfile.collectAsState()
    val sessionUser by viewModel.sessionUser.collectAsState()
    val profileImageUri by viewModel.profileImageUri.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()

    // --- State Holders for Personal Information ---
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Prefer not to say") }
    var maritalStatus by remember { mutableStateOf("Single") }
    var religion by remember { mutableStateOf("Islam") }
    var nationality by remember { mutableStateOf("") }
    var nationalId by remember { mutableStateOf("") }

    // --- State Holders for Contact & Address ---
    var residentialStreet by remember { mutableStateOf("") }
    var residentialCity by remember { mutableStateOf("") }
    var residentialState by remember { mutableStateOf("") }
    var residentialZip by remember { mutableStateOf("") }
    var residentialCountry by remember { mutableStateOf("") }

    var permanentIsSame by remember { mutableStateOf(true) }
    var permanentStreet by remember { mutableStateOf("") }
    var permanentCity by remember { mutableStateOf("") }
    var permanentState by remember { mutableStateOf("") }
    var permanentZip by remember { mutableStateOf("") }
    var permanentCountry by remember { mutableStateOf("") }

    var mobileNumber by remember { mutableStateOf("") }
    var alternatePhone by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }

    // --- State Holders for Emergency Contact ---
    var emergencyName by remember { mutableStateOf("") }
    var emergencyRelationship by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var alternateEmergencyPhone by remember { mutableStateOf("") }

    // --- State Holders for Health, Medical & Biometrics (Consolidated) ---
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("O+") }
    var primaryDoctor by remember { mutableStateOf("") }
    var medicalConditions by remember { mutableStateOf("") }
    var currentMedications by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var dietaryRestrictions by remember { mutableStateOf("") }

    // --- State Holders for Clinical Vitals ---
    var vitalsBloodPressure by remember { mutableStateOf("") }
    var vitalsHeartRate by remember { mutableStateOf("") }

    var showErrors by remember { mutableStateOf(false) }

    val firstNameError = if (firstName.isBlank()) "First Name is required" else null
    val emailError = if (emailAddress.isBlank()) {
        "Email Address is required"
    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
        "Please enter a valid email address"
    } else null

    val mobileError = if (mobileNumber.isBlank()) {
        "Mobile Number is required"
    } else if (mobileNumber.filter { it.isDigit() || it == '+' }.length < 7) {
        "Please enter a valid phone number"
    } else null

    // --- Load existing data from DB ---
    LaunchedEffect(dbProfile) {
        dbProfile?.let { prof ->
            firstName = prof.firstName
            middleName = prof.middleName
            lastName = prof.lastName
            dob = prof.dob
            gender = if (prof.gender.isBlank()) "Prefer not to say" else prof.gender
            maritalStatus = if (prof.maritalStatus.isBlank()) "Single" else prof.maritalStatus
            nationality = prof.nationality
            nationalId = prof.nationalId
            religion = if (prof.religion.isNotBlank()) prof.religion else (sessionUser?.religion ?: "Islam")
            
            bloodGroup = if (prof.bloodGroup.isNotBlank()) {
                prof.bloodGroup
            } else if (prof.vitalsBloodGroup.isNotBlank()) {
                prof.vitalsBloodGroup
            } else {
                "O+"
            }

            residentialStreet = prof.residentialStreet
            residentialCity = prof.residentialCity
            residentialState = prof.residentialState
            residentialZip = prof.residentialZip
            residentialCountry = prof.residentialCountry

            permanentIsSame = prof.permanentIsSame
            permanentStreet = prof.permanentStreet
            permanentCity = prof.permanentCity
            permanentState = prof.permanentState
            permanentZip = prof.permanentZip
            permanentCountry = prof.permanentCountry

            mobileNumber = prof.mobileNumber
            alternatePhone = prof.alternatePhone
            emailAddress = if (prof.emailAddress.isBlank()) sessionUser?.email ?: "" else prof.emailAddress

            emergencyName = prof.emergencyName
            emergencyRelationship = prof.emergencyRelationship
            emergencyPhone = prof.emergencyPhone
            alternateEmergencyPhone = prof.alternateEmergencyPhone

            height = if (prof.height.isNotBlank()) prof.height else prof.vitalsHeight
            weight = if (prof.weight.isNotBlank()) prof.weight else prof.vitalsWeight
            primaryDoctor = prof.primaryDoctor
            medicalConditions = prof.medicalConditions
            currentMedications = prof.currentMedications
            allergies = prof.allergies
            dietaryRestrictions = prof.dietaryRestrictions

            vitalsBloodPressure = prof.vitalsBloodPressure
            vitalsHeartRate = prof.vitalsHeartRate
        } ?: run {
            emailAddress = sessionUser?.email ?: ""
            religion = sessionUser?.religion ?: "Islam"
        }
    }

    // --- Auto Sync Addresses ---
    LaunchedEffect(permanentIsSame, residentialStreet, residentialCity, residentialState, residentialZip, residentialCountry) {
        if (permanentIsSame) {
            permanentStreet = residentialStreet
            permanentCity = residentialCity
            permanentState = residentialState
            permanentZip = residentialZip
            permanentCountry = residentialCountry
        }
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setProfileImageUri(uri.toString())
        }
    }

    val themeColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Custom App Bar Header ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "My Profile & Milestones",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Your digital profile, biometrics & earned rewards",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // --- Profile Image & Hero Card ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val displayName = if (firstName.isNotBlank()) "$firstName $lastName" else sessionUser?.fullName ?: "Guest User"
                    val nameLetter = displayName.trim().take(1).uppercase()

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(themeColor, MaterialTheme.colorScheme.tertiary)
                                )
                            )
                            .border(3.dp, themeColor.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileImageUri != null) {
                            AsyncImage(
                                model = profileImageUri,
                                contentDescription = "Profile Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = nameLetter,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 38.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (emailAddress.isNotBlank()) emailAddress else sessionUser?.email ?: "No email registered",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                        ) {
                            Icon(
                                imageVector = if (profileImageUri == null) Icons.Default.Upload else Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (profileImageUri == null) "Upload Photo" else "Edit Photo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (profileImageUri != null) {
                            OutlinedButton(
                                onClick = { viewModel.setProfileImageUri(null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- Earned Badges Slider Showcase ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(Icons.Default.Stars, contentDescription = null, tint = themeColor, modifier = Modifier.size(22.dp))
                        Text(
                            text = "EARNED MILITARY & HABIT BADGES 🏆",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColor
                        )
                    }

                    val standardBadges = listOf(
                        ProfileBadgeSpec(1, "The Spark", "🥉", "The Launchpad", "Ignited the habit"),
                        ProfileBadgeSpec(3, "Three's Company", "🥉", "The Launchpad", "Overcame day-two slump"),
                        ProfileBadgeSpec(5, "Workweek Warrior", "🥉", "The Launchpad", "Five consecutive days"),
                        ProfileBadgeSpec(7, "Weekly Wonder", "🥉", "The Launchpad", "Completed full week"),
                        ProfileBadgeSpec(14, "Fortnight Force", "🥈", "The Builder", "Two weeks dedication"),
                        ProfileBadgeSpec(21, "Habit Former", "🥈", "The Builder", "Avg days to lock routine"),
                        ProfileBadgeSpec(30, "Calendar Crusher", "🥈", "The Builder", "One full month"),
                        ProfileBadgeSpec(45, "Halfway Hero", "🥈", "The Builder", "Momentum past 1 month"),
                        ProfileBadgeSpec(60, "Iron Will", "🥇", "The Master", "Two months unbroken"),
                        ProfileBadgeSpec(90, "Seasoned Pro", "🥇", "The Master", "Seasonal commitment"),
                        ProfileBadgeSpec(100, "Centurion", "🥇", "The Master", "Triple-digit milestone"),
                        ProfileBadgeSpec(365, "Immortal", "🥇", "The Master", "One full year")
                    )

                    val earnedBadgeDays = remember(allHabits) {
                        allHabits.flatMap { habit ->
                            TrackWiseUtils.deserializeIntList(habit.badgesEarnedJson)
                        }.toSet()
                    }

                    val userEarnedBadges = remember(earnedBadgeDays) {
                        val earned = standardBadges.filter { earnedBadgeDays.contains(it.days) }
                        if (earned.isEmpty()) {
                            // Show initial introductory milestone achievements by default
                            listOf(
                                ProfileBadgeSpec(1, "The Spark", "🥉", "The Launchpad", "Ignited the first habit"),
                                ProfileBadgeSpec(3, "Three's Company", "🥉", "The Launchpad", "Overcame day-two slump"),
                                ProfileBadgeSpec(5, "Workweek Warrior", "🥉", "The Launchpad", "Five consecutive days")
                            )
                        } else {
                            earned
                        }
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(userEarnedBadges) { badge ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .width(110.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = badge.medal, fontSize = 28.sp)
                                    Text(
                                        text = badge.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = badge.tier,
                                        fontSize = 8.sp,
                                        color = themeColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = badge.desc,
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        lineHeight = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 1. Basic Personal Information ---
        item {
            ProfileSectionCard(
                title = "1. BASIC PERSONAL INFORMATION",
                icon = Icons.Default.Person
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { 
                                firstName = it
                                if (it.isNotBlank()) showErrors = false
                            },
                            label = { Text("First Name *") },
                            isError = showErrors && firstNameError != null,
                            supportingText = {
                                if (showErrors && firstNameError != null) {
                                    Text(firstNameError, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                        )
                        OutlinedTextField(
                            value = middleName,
                            onValueChange = { middleName = it },
                            label = { Text("Middle Name") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                        )
                    }
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )
                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("Date of Birth (DD/MM/YYYY)") },
                        placeholder = { Text("e.g. 15/08/1998") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )

                    // Gender Selector
                    Text("Gender / Sex", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColor)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Male", "Female", "Other", "Prefer not to say").forEach { option ->
                            val isSel = gender == option
                            ChoiceChip(text = option, isSelected = isSel, onClick = { gender = option })
                        }
                    }

                    // Marital Status Selector
                    Text("Marital Status", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColor)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Single", "Married", "Divorced", "Widowed").forEach { option ->
                            val isSel = maritalStatus == option
                            ChoiceChip(text = option, isSelected = isSel, onClick = { maritalStatus = option })
                        }
                    }

                    // Religion Selector
                    Text("Religion", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColor)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Islam", "Hindu", "Christian", "Sikh", "Others").forEach { option ->
                            val isSel = religion == option
                            ChoiceChip(text = option, isSelected = isSel, onClick = { religion = option })
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = nationality,
                            onValueChange = { nationality = it },
                            label = { Text("Nationality") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                        )
                        OutlinedTextField(
                            value = nationalId,
                            onValueChange = { nationalId = it },
                            label = { Text("National ID / Passport") },
                            modifier = Modifier.weight(1.2f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                        )
                    }
                }
            }
        }

        // --- 2. Contact & Address Records ---
        item {
            ProfileSectionCard(
                title = "2. CONTACT & ADDRESS INFORMATION",
                icon = Icons.Default.Home
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Primary Contacts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColor)
                    OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = {
                            mobileNumber = it
                            if (it.isNotBlank()) showErrors = false
                        },
                        label = { Text("Mobile Phone Number *") },
                        isError = showErrors && mobileError != null,
                        supportingText = {
                            if (showErrors && mobileError != null) {
                                Text(mobileError, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )
                    OutlinedTextField(
                        value = alternatePhone,
                        onValueChange = { alternatePhone = it },
                        label = { Text("Alternate Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )
                    OutlinedTextField(
                        value = emailAddress,
                        onValueChange = {
                            emailAddress = it
                            if (it.isNotBlank()) showErrors = false
                        },
                        label = { Text("Email Address *") },
                        isError = showErrors && emailError != null,
                        supportingText = {
                            if (showErrors && emailError != null) {
                                Text(emailError, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )

                    Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Text("Residential / Current Address", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColor)
                    OutlinedTextField(
                        value = residentialStreet,
                        onValueChange = { residentialStreet = it },
                        label = { Text("Street Address") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = residentialCity,
                            onValueChange = { residentialCity = it },
                            label = { Text("City") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                        )
                        OutlinedTextField(
                            value = residentialState,
                            onValueChange = { residentialState = it },
                            label = { Text("State / Province") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = residentialZip,
                            onValueChange = { residentialZip = it },
                            label = { Text("ZIP / Postal Code") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                        )
                        OutlinedTextField(
                            value = residentialCountry,
                            onValueChange = { residentialCountry = it },
                            label = { Text("Country") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { permanentIsSame = !permanentIsSame }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = permanentIsSame,
                            onCheckedChange = { permanentIsSame = it },
                            colors = CheckboxDefaults.colors(checkedColor = themeColor)
                        )
                        Text(
                            text = "Permanent Address is same as Residential address",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (!permanentIsSame) {
                        Text("Permanent Address", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColor)
                        OutlinedTextField(
                            value = permanentStreet,
                            onValueChange = { permanentStreet = it },
                            label = { Text("Street Address") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = permanentCity,
                                onValueChange = { permanentCity = it },
                                label = { Text("City") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                            )
                            OutlinedTextField(
                                value = permanentState,
                                onValueChange = { permanentState = it },
                                label = { Text("State / Province") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = permanentZip,
                                onValueChange = { permanentZip = it },
                                label = { Text("ZIP / Postal Code") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                            )
                            OutlinedTextField(
                                value = permanentCountry,
                                onValueChange = { permanentCountry = it },
                                label = { Text("Country") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                            )
                        }
                    }
                }
            }
        }

        // --- 3. Emergency Contacts ---
        item {
            ProfileSectionCard(
                title = "3. EMERGENCY CONTACT INFORMATION",
                icon = Icons.Default.Phone
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = emergencyName,
                        onValueChange = { emergencyName = it },
                        label = { Text("Primary Emergency Contact Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )
                    OutlinedTextField(
                        value = emergencyRelationship,
                        onValueChange = { emergencyRelationship = it },
                        label = { Text("Relationship to User") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )
                    OutlinedTextField(
                        value = emergencyPhone,
                        onValueChange = { emergencyPhone = it },
                        label = { Text("Emergency Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )
                    OutlinedTextField(
                        value = alternateEmergencyPhone,
                        onValueChange = { alternateEmergencyPhone = it },
                        label = { Text("Alternate Emergency Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )
                }
            }
        }

        // --- 4. Health, Medical & Biometrics (Consolidated) ---
        item {
            ProfileSectionCard(
                title = "4. HEALTH, MEDICAL & BIOMETRIC DETAILS",
                icon = Icons.Default.Favorite
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it },
                            label = { Text("Height (e.g., cm)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                        )
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("Weight (e.g., kg)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                        )
                    }

                    OutlinedTextField(
                        value = primaryDoctor,
                        onValueChange = { primaryDoctor = it },
                        label = { Text("Primary Doctor Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )
                    OutlinedTextField(
                        value = medicalConditions,
                        onValueChange = { medicalConditions = it },
                        label = { Text("Known Medical Conditions / Chronic Illnesses") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )
                    OutlinedTextField(
                        value = currentMedications,
                        onValueChange = { currentMedications = it },
                        label = { Text("Current Active Medications") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )
                    OutlinedTextField(
                        value = allergies,
                        onValueChange = { allergies = it },
                        label = { Text("Allergies (Food, Drug, Environment)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )
                    OutlinedTextField(
                        value = dietaryRestrictions,
                        onValueChange = { dietaryRestrictions = it },
                        label = { Text("Dietary Restrictions / Preferences") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )
                }
            }
        }

        // --- 5. Clinical Vitals ---
        item {
            ProfileSectionCard(
                title = "5. CLINICAL VITALS RECORDS",
                icon = Icons.Default.Info
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = vitalsBloodPressure,
                            onValueChange = { vitalsBloodPressure = it },
                            label = { Text("Blood Pressure") },
                            placeholder = { Text("e.g. 120/80") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                        )
                        OutlinedTextField(
                            value = vitalsHeartRate,
                            onValueChange = { vitalsHeartRate = it },
                            label = { Text("Resting Heart Rate (BPM)") },
                            placeholder = { Text("e.g. 72") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                        )
                    }

                    // Consolidated Blood Group Selector
                    Text("Verified Blood Group", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColor)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-").forEach { option ->
                            val isSel = bloodGroup == option
                            ChoiceChip(text = option, isSelected = isSel, onClick = { bloodGroup = option })
                        }
                    }
                }
            }
        }

        // --- Save Button ---
        item {
            if (showErrors && (firstNameError != null || emailError != null || mobileError != null)) {
                Text(
                    text = "Please fix required fields first (marked with *)",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = {
                    if (firstNameError == null && emailError == null && mobileError == null) {
                        val userId = sessionUser?.id ?: "unknown-user"
                        val newProfile = UserProfileEntity(
                            userId = userId,
                            firstName = firstName,
                            middleName = middleName,
                            lastName = lastName,
                            dob = dob,
                            gender = gender,
                            maritalStatus = maritalStatus,
                            nationality = nationality,
                            nationalId = nationalId,
                            bloodGroup = bloodGroup,
                            residentialStreet = residentialStreet,
                            residentialCity = residentialCity,
                            residentialState = residentialState,
                            residentialZip = residentialZip,
                            residentialCountry = residentialCountry,
                            permanentStreet = permanentStreet,
                            permanentCity = permanentCity,
                            permanentState = permanentState,
                            permanentZip = permanentZip,
                            permanentCountry = permanentCountry,
                            permanentIsSame = permanentIsSame,
                            mobileNumber = mobileNumber,
                            alternatePhone = alternatePhone,
                            emailAddress = emailAddress,
                            emergencyName = emergencyName,
                            emergencyRelationship = emergencyRelationship,
                            emergencyPhone = emergencyPhone,
                            alternateEmergencyPhone = alternateEmergencyPhone,
                            height = height,
                            weight = weight,
                            primaryDoctor = primaryDoctor,
                            medicalConditions = medicalConditions,
                            currentMedications = currentMedications,
                            allergies = allergies,
                            dietaryRestrictions = dietaryRestrictions,
                            vitalsHeight = height,
                            vitalsWeight = weight,
                            vitalsBloodPressure = vitalsBloodPressure,
                            vitalsHeartRate = vitalsHeartRate,
                            vitalsBloodGroup = bloodGroup,
                            religion = religion
                        )
                        viewModel.saveDetailedProfile(newProfile)
                        viewModel.showSuccessMessage("Detailed Profile saved successfully!")
                        showErrors = false
                    } else {
                        showErrors = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SAVE DETAILED PROFILE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun ProfileSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    val themeColor = MaterialTheme.colorScheme.primary

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(icon, contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColor)
            }
            content()
        }
    }
}

@Composable
fun ChoiceChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val themeColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) themeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .border(
                width = 1.dp,
                color = if (isSelected) themeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}
