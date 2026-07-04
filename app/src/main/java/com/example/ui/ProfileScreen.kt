package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfileEntity
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    val dbProfile by viewModel.userProfile.collectAsState()
    val sessionUser by viewModel.sessionUser.collectAsState()

    // --- State Holders for Personal Information ---
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Prefer not to say") }
    var maritalStatus by remember { mutableStateOf("Single") }
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
            
            // Consolidate blood group from either field
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

            // Consolidate height & weight from either field
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Block ---
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBox,
                        contentDescription = "Profile",
                        tint = BrandViolet,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Detailed Profile",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "A complete repository of your personal, contact, and biometrics records.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
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
                            onValueChange = { firstName = it },
                            label = { Text("First Name") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                        )
                        OutlinedTextField(
                            value = middleName,
                            onValueChange = { middleName = it },
                            label = { Text("Middle Name") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                        )
                    }
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                    )
                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("Date of Birth (DD/MM/YYYY)") },
                        placeholder = { Text("e.g. 15/08/1998") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                    )

                    // Gender Selector
                    Text("Gender / Sex", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
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
                    Text("Marital Status", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = nationality,
                            onValueChange = { nationality = it },
                            label = { Text("Nationality") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                        )
                        OutlinedTextField(
                            value = nationalId,
                            onValueChange = { nationalId = it },
                            label = { Text("National ID / Passport") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                        )
                    }
                }
            }
        }

        // --- 2. Contact & Address Details ---
        item {
            ProfileSectionCard(
                title = "2. CONTACT & ADDRESS DETAILS",
                icon = Icons.Default.Home
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Residential Address", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
                    OutlinedTextField(
                        value = residentialStreet,
                        onValueChange = { residentialStreet = it },
                        label = { Text("Street Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = residentialCity,
                            onValueChange = { residentialCity = it },
                            label = { Text("City") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                        )
                        OutlinedTextField(
                            value = residentialState,
                            onValueChange = { residentialState = it },
                            label = { Text("State") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = residentialZip,
                            onValueChange = { residentialZip = it },
                            label = { Text("ZIP / Pin Code") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                        )
                        OutlinedTextField(
                            value = residentialCountry,
                            onValueChange = { residentialCountry = it },
                            label = { Text("Country") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                        )
                    }

                    // Toggle: Same Address
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { permanentIsSame = !permanentIsSame }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Permanent address is same as residential", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = permanentIsSame,
                            onCheckedChange = { permanentIsSame = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrandViolet, checkedTrackColor = BrandViolet.copy(alpha = 0.4f))
                        )
                    }

                    AnimatedVisibility(visible = !permanentIsSame) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Permanent Address", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
                            OutlinedTextField(
                                value = permanentStreet,
                                onValueChange = { permanentStreet = it },
                                label = { Text("Street Address") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = permanentCity,
                                    onValueChange = { permanentCity = it },
                                    label = { Text("City") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                                )
                                OutlinedTextField(
                                    value = permanentState,
                                    onValueChange = { permanentState = it },
                                    label = { Text("State") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = permanentZip,
                                    onValueChange = { permanentZip = it },
                                    label = { Text("ZIP / Pin Code") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                                )
                                OutlinedTextField(
                                    value = permanentCountry,
                                    onValueChange = { permanentCountry = it },
                                    label = { Text("Country") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                                )
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Text("Contact Details", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
                    OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = { mobileNumber = it },
                        label = { Text("Mobile Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                    )
                    OutlinedTextField(
                        value = alternatePhone,
                        onValueChange = { alternatePhone = it },
                        label = { Text("Alternate Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                    )
                    OutlinedTextField(
                        value = emailAddress,
                        onValueChange = { emailAddress = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                    )
                }
            }
        }

        // --- 3. Emergency Contact ---
        item {
            ProfileSectionCard(
                title = "3. EMERGENCY CONTACT",
                icon = Icons.Default.ContactPhone
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = emergencyName,
                        onValueChange = { emergencyName = it },
                        label = { Text("Emergency Contact Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                    )
                    OutlinedTextField(
                        value = emergencyRelationship,
                        onValueChange = { emergencyRelationship = it },
                        label = { Text("Relationship (e.g. Father)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                    )
                    OutlinedTextField(
                        value = emergencyPhone,
                        onValueChange = { emergencyPhone = it },
                        label = { Text("Emergency Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                    )
                    OutlinedTextField(
                        value = alternateEmergencyPhone,
                        onValueChange = { alternateEmergencyPhone = it },
                        label = { Text("Alternate Emergency Phone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                    )
                }
            }
        }

        // --- 4. Health & Medical Information ---
        item {
            ProfileSectionCard(
                title = "4. HEALTH & MEDICAL INFORMATION",
                icon = Icons.Default.Healing
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = primaryDoctor,
                        onValueChange = { primaryDoctor = it },
                        label = { Text("Primary Doctor / Physician Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                    )
                    OutlinedTextField(
                        value = medicalConditions,
                        onValueChange = { medicalConditions = it },
                        label = { Text("Medical Conditions / Chronic Illnesses") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                    )
                    OutlinedTextField(
                        value = currentMedications,
                        onValueChange = { currentMedications = it },
                        label = { Text("Current Medications") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                    )
                    OutlinedTextField(
                        value = allergies,
                        onValueChange = { allergies = it },
                        label = { Text("Allergies (Medication / Food / Environmental)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                    )
                    OutlinedTextField(
                        value = dietaryRestrictions,
                        onValueChange = { dietaryRestrictions = it },
                        label = { Text("Dietary Restrictions / Preferences") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                    )
                }
            }
        }

        // --- 5. Biometrics & Clinical Vitals (Consolidated) ---
        item {
            ProfileSectionCard(
                title = "5. BIOMETRICS & CLINICAL VITALS",
                icon = Icons.Default.Favorite
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it },
                            label = { Text("Height (e.g. 178 cm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                        )
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("Weight (e.g. 72 kg)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = vitalsBloodPressure,
                            onValueChange = { vitalsBloodPressure = it },
                            label = { Text("Blood Pressure") },
                            placeholder = { Text("e.g. 120/80") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                        )
                        OutlinedTextField(
                            value = vitalsHeartRate,
                            onValueChange = { vitalsHeartRate = it },
                            label = { Text("Resting Heart Rate (BPM)") },
                            placeholder = { Text("e.g. 72") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandViolet, focusedLabelColor = BrandViolet)
                        )
                    }

                    // Consolidated Blood Group Selector
                    Text("Verified Blood Group", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
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
            Button(
                onClick = {
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
                        vitalsBloodGroup = bloodGroup
                    )
                    viewModel.saveDetailedProfile(newProfile)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
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
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                Icon(icon, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(20.dp))
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
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
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) BrandViolet else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .border(
                width = 1.dp,
                color = if (isSelected) BrandViolet else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
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
