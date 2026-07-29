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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.CompositingStrategy

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
    onNavigateToSocial: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dbProfile by viewModel.userProfile.collectAsState()
    val sessionUser by viewModel.sessionUser.collectAsState()
    val profileImageUri by viewModel.profileImageUri.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()

    var showDetailedFormDialog by remember { mutableStateOf(false) }
    val allUsers by viewModel.allUsers.collectAsState()
    var showAddAccountDialog by remember { mutableStateOf(false) }

    val totalCompleted = remember(allTasks, allHabits) {
        val completedTasksCount = allTasks.count { it.completed }
        val habitCompletionsCount = allHabits.sumOf { habit ->
            com.example.utils.TrackWiseUtils.deserializeStringList(habit.daysCompletedJson).size
        }
        completedTasksCount + habitCompletionsCount
    }

    val userLevel = remember(totalCompleted) {
        when {
            totalCompleted >= 50 -> "Grandmaster (Lvl ${1 + totalCompleted / 10})"
            totalCompleted >= 30 -> "Elite (Lvl ${1 + totalCompleted / 8})"
            totalCompleted >= 15 -> "Expert (Lvl ${1 + totalCompleted / 6})"
            totalCompleted >= 5 -> "Novice (Lvl ${1 + totalCompleted / 4})"
            else -> "Beginner (Lvl 1)"
        }
    }

    val totalBadgesCollected = remember(allHabits) {
        allHabits.flatMap { habit ->
            com.example.utils.TrackWiseUtils.deserializeIntList(habit.badgesEarnedJson)
        }.toSet().size
    }

    // --- State Holders for Personal Information ---
    var isPersonalInfoExpanded by remember { mutableStateOf(true) }
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

    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    var selectedRawUriForCrop by remember { mutableStateOf<String?>(null) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedRawUriForCrop = uri.toString()
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val displayName = if (firstName.isNotBlank()) "$firstName $lastName" else sessionUser?.fullName ?: "Guest User"
                    val nameLetter = displayName.trim().take(1).uppercase()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(86.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .align(Alignment.Center)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(themeColor, MaterialTheme.colorScheme.tertiary)
                                        )
                                    )
                                    .border(3.dp, themeColor.copy(alpha = 0.3f), CircleShape)
                                    .clickable {
                                        showPhotoOptionsDialog = true
                                    },
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
                                        fontSize = 32.sp
                                    )
                                }
                            }

                            // Camera icon overlay indicator for click-to-edit
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .clickable {
                                        showPhotoOptionsDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Edit photo",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = displayName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (emailAddress.isNotBlank()) emailAddress else sessionUser?.email ?: "No email registered",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )

                            // Tag Row: level tag & total badges collected tag
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                // Level Tag
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = userLevel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Badges Tag
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))
                                        .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "$totalBadgesCollected Badges",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    )

                    // Detailed User Information Link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showDetailedFormDialog = true
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Detailed User Information",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Open Details",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (showPhotoOptionsDialog) {
                        AlertDialog(
                            onDismissRequest = { showPhotoOptionsDialog = false },
                            title = { Text("Profile Photo", fontWeight = FontWeight.Bold) },
                            text = { Text("Choose an action for your profile photo.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showPhotoOptionsDialog = false
                                        imagePickerLauncher.launch("image/*")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Choose Photo")
                                }
                            },
                            dismissButton = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (profileImageUri != null) {
                                        TextButton(
                                            onClick = {
                                                showPhotoOptionsDialog = false
                                                viewModel.setProfileImageUri(null)
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Remove")
                                        }
                                    }
                                    TextButton(onClick = { showPhotoOptionsDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                    }

                    selectedRawUriForCrop?.let { rawUri ->
                        ImageCropDialog(
                            sourceUri = rawUri,
                            onDismiss = { selectedRawUriForCrop = null },
                            onCropSuccess = { croppedUri ->
                                viewModel.setProfileImageUri(croppedUri)
                                selectedRawUriForCrop = null
                            }
                        )
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
                                    .size(125.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        onNavigateToSocial?.invoke()
                                    },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = badge.medal, fontSize = 28.sp)
                                    Text(
                                        text = badge.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                                        lineHeight = 10.sp,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Multi-Account Management Section ---
        item {
            var showManageAccountsDialog by remember { mutableStateOf(false) }

            ProfileSectionCard(
                title = "MANAGE ACCOUNTS",
                icon = Icons.Default.Settings
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // "+ Add Account" row
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddAccountDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Account",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Add Account",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // "Manage Account" row
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showManageAccountsDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Manage Account",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Manage Account",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Dialog to select which account should be active
                    if (showManageAccountsDialog) {
                        AlertDialog(
                            onDismissRequest = { showManageAccountsDialog = false },
                            title = {
                                Text(
                                    text = "Select Active Account",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            },
                            text = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    allUsers.forEach { user ->
                                        val isActive = user.id == sessionUser?.id
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isActive) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    width = if (isActive) 1.5.dp else 1.dp,
                                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    if (!isActive) {
                                                        viewModel.switchAccount(user.id)
                                                    }
                                                    showManageAccountsDialog = false
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Initials avatar
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = user.fullName.take(1).uppercase(),
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp
                                                        )
                                                    }

                                                    Column {
                                                        Text(
                                                            text = user.fullName,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = user.email,
                                                            fontSize = 11.sp,
                                                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                        )
                                                    }
                                                }

                                                if (isActive) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Active",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showManageAccountsDialog = false }) {
                                    Text("CLOSE")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddAccountDialog) {
        var addName by remember { mutableStateOf("") }
        var addEmail by remember { mutableStateOf("") }
        var addPassword by remember { mutableStateOf("") }
        var addErrorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = {
                Text(
                    text = "Add New Account",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "Create another user profile on this device. This profile will have its own independent sandbox database.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = addName,
                        onValueChange = { addName = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )

                    OutlinedTextField(
                        value = addEmail,
                        onValueChange = { addEmail = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )

                    OutlinedTextField(
                        value = addPassword,
                        onValueChange = { addPassword = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                    )

                    if (addErrorMsg != null) {
                        Text(
                            text = addErrorMsg!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (addName.isBlank() || addEmail.isBlank() || addPassword.isBlank()) {
                            addErrorMsg = "All fields are required."
                        } else if (!addEmail.contains("@")) {
                            addErrorMsg = "Please enter a valid email address."
                        } else {
                            viewModel.signUp(addEmail, addPassword, addName)
                            showAddAccountDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    Text("CREATE ACCOUNT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showDetailedFormDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showDetailedFormDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Custom Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showDetailedFormDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Detailed User Information",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Scrollable content inside the Dialog
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // --- 1. Basic Personal Information ---
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                    .clickable { isPersonalInfoExpanded = !isPersonalInfoExpanded },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
                                        Text("1. BASIC PERSONAL INFORMATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColor)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(
                                            imageVector = if (isPersonalInfoExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Toggle Section",
                                            tint = themeColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    if (isPersonalInfoExpanded) {
                                        Spacer(modifier = Modifier.height(16.dp))
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
                                        showDetailedFormDialog = false
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

@Composable
fun ImageCropDialog(
    sourceUri: String,
    onDismiss: () -> Unit,
    onCropSuccess: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val viewportSizeDp = 240.dp
    val viewportSizePx = with(density) { viewportSizeDp.toPx() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Crop Profile Photo",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Drag to reposition, use slider to zoom.\nThe photo inside the circle is what will be saved.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                // Viewport Container
                Box(
                    modifier = Modifier
                        .size(viewportSizeDp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = sourceUri,
                        contentDescription = "Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )

                    // Overlay Mask with transparent circle cutout
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val radius = canvasWidth.coerceAtMost(canvasHeight) / 2f * 0.88f

                        // Draw dark overlay
                        drawRect(
                            color = Color.Black.copy(alpha = 0.65f)
                        )

                        // Clear circle cutout
                        drawCircle(
                            color = Color.Transparent,
                            radius = radius,
                            center = androidx.compose.ui.geometry.Offset(canvasWidth / 2f, canvasHeight / 2f),
                            blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                        )

                        // Outline stroke
                        drawCircle(
                            color = Color.White.copy(alpha = 0.8f),
                            radius = radius,
                            center = androidx.compose.ui.geometry.Offset(canvasWidth / 2f, canvasHeight / 2f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // Precision Zoom Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 1f..4f,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val croppedUri = performCropAndSave(
                        context = context,
                        sourceUriStr = sourceUri,
                        userScale = scale,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        viewportSizePx = viewportSizePx
                    )
                    if (croppedUri != null) {
                        onCropSuccess(croppedUri)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Crop & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    )
}

private fun performCropAndSave(
    context: android.content.Context,
    sourceUriStr: String,
    userScale: Float,
    offsetX: Float,
    offsetY: Float,
    viewportSizePx: Float
): String? {
    try {
        val uri = android.net.Uri.parse(sourceUriStr)
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream) ?: return null
        inputStream.close()

        val wOrig = originalBitmap.width.toFloat()
        val hOrig = originalBitmap.height.toFloat()

        val baseScale = Math.min(viewportSizePx / wOrig, viewportSizePx / hOrig)
        val totalScale = baseScale * userScale

        val wFinal = wOrig * totalScale
        val hFinal = hOrig * totalScale

        val xFinal = (viewportSizePx - wFinal) / 2f + offsetX
        val yFinal = (viewportSizePx - hFinal) / 2f + offsetY

        val leftScaled = -xFinal
        val topScaled = -yFinal

        var cropX = (leftScaled / totalScale).toInt()
        var cropY = (topScaled / totalScale).toInt()
        var cropW = (viewportSizePx / totalScale).toInt()
        var cropH = (viewportSizePx / totalScale).toInt()

        cropX = cropX.coerceIn(0, (wOrig - 1).toInt())
        cropY = cropY.coerceIn(0, (hOrig - 1).toInt())
        cropW = cropW.coerceAtMost((wOrig - cropX).toInt())
        cropH = cropH.coerceAtMost((hOrig - cropY).toInt())

        if (cropW <= 0 || cropH <= 0) return null

        val croppedBitmap = android.graphics.Bitmap.createBitmap(originalBitmap, cropX, cropY, cropW, cropH)

        val cacheFile = java.io.File(context.cacheDir, "cropped_profile_${System.currentTimeMillis()}.jpg")
        val outStream = java.io.FileOutputStream(cacheFile)
        croppedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outStream)
        outStream.flush()
        outStream.close()

        originalBitmap.recycle()
        if (croppedBitmap != originalBitmap) {
            croppedBitmap.recycle()
        }

        return android.net.Uri.fromFile(cacheFile).toString()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}
