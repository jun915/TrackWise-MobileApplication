package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: TrackWiseViewModel,
    onBack: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Profile Form 👤", "Theme & Sounds 🎨", "Backup & Sync 💾")

    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // --- Custom App Bar Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to Dashboard",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings & Configuration",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // --- M3 TabRow ---
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = BrandViolet,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    modifier = Modifier.testTag("settings_tab_$index")
                ) {
                    Box(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTabIndex == index) BrandViolet else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Screen Content Based on Tab ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTabIndex) {
                0 -> {
                    // Reuses the complete, fully functional ProfileScreen Composable directly!
                    ProfileScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                1 -> {
                    PreferencesTab(viewModel = viewModel)
                }
                2 -> {
                    BackupAndSyncTab(
                        viewModel = viewModel,
                        onImportClick = onImportClick,
                        onClearDataClick = { showClearDataConfirm = true },
                        onDeleteAccountClick = { showDeleteAccountConfirm = true }
                    )
                }
            }
        }
    }

    // --- Dialog Confirmations ---
    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text("Clear All Data", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear all your local logs, tracking history, and health statistics? This action cannot be undone, but your user account will remain active.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDataConfirm = false
                        viewModel.clearAllData()
                    }
                ) {
                    Text("Clear All", color = BrandRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteAccountConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirm = false },
            title = { Text("Delete User Account", fontWeight = FontWeight.Bold) },
            text = { Text("Are you absolutely sure you want to delete your entire user profile and all associated data permanently? This action is completely irreversible!") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountConfirm = false
                        viewModel.deleteAccount()
                    },
                    modifier = Modifier.testTag("delete_account_confirm")
                ) {
                    Text("Delete Permanently", color = BrandRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PreferencesTab(viewModel: TrackWiseViewModel) {
    val currentTheme by viewModel.themeMode.collectAsState()
    val themeAccent by viewModel.appThemeSelection.collectAsState()
    val taskSound by viewModel.taskSound.collectAsState()

    var themeModeExpanded by remember { mutableStateOf(false) }
    var themeAccentExpanded by remember { mutableStateOf(false) }
    var taskSoundExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "THEME & VISUAL PREFERENCES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandViolet
                    )

                    // 1. Theme Mode
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Theme Mode", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Box {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { themeModeExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val currentThemeLabel = when (currentTheme) {
                                        "light" -> "Light Mode ☀️"
                                        "dark" -> "Dark Mode 🌙"
                                        "auto" -> "Auto (Day/Night) 🌅"
                                        else -> "System Default ⚙️"
                                    }
                                    Text(text = currentThemeLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand Theme Mode")
                                }
                            }
                            DropdownMenu(
                                expanded = themeModeExpanded,
                                onDismissRequest = { themeModeExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                listOf("light" to "Light Mode ☀️", "dark" to "Dark Mode 🌙", "auto" to "Auto (Day/Night) 🌅", "system" to "System Default ⚙️").forEach { (mode, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label, fontSize = 13.sp) },
                                        onClick = {
                                            viewModel.setThemeMode(mode)
                                            themeModeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Design Accent
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Design Accent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Box {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { themeAccentExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val accentEmoji = when (themeAccent) {
                                        "Default Violet" -> "💜"
                                        "Ocean Blue" -> "💙"
                                        "Forest Green" -> "💚"
                                        "Sunset Orange" -> "🧡"
                                        "Crimson Red" -> "❤️"
                                        else -> "🎨"
                                    }
                                    Text(text = "$themeAccent $accentEmoji", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand Accent")
                                }
                            }
                            DropdownMenu(
                                expanded = themeAccentExpanded,
                                onDismissRequest = { themeAccentExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                listOf("Default Violet" to "💜", "Ocean Blue" to "💙", "Forest Green" to "💚", "Sunset Orange" to "🧡", "Crimson Red" to "❤️").forEach { (accent, emoji) ->
                                    DropdownMenuItem(
                                        text = { Text("$accent $emoji", fontSize = 13.sp) },
                                        onClick = {
                                            viewModel.setAppThemeSelection(accent)
                                            themeAccentExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "SOUND EFFECTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandViolet
                    )

                    // Task Completion Sound Dropdown
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Task Completion Trigger Sound", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Box {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { taskSoundExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🎵 $taskSound", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand Task Sound")
                                }
                            }
                            DropdownMenu(
                                expanded = taskSoundExpanded,
                                onDismissRequest = { taskSoundExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                listOf("Chime", "Ding", "Bell", "None").forEach { snd ->
                                    DropdownMenuItem(
                                        text = { Text(snd, fontSize = 13.sp) },
                                        onClick = {
                                            viewModel.setTaskSound(snd)
                                            taskSoundExpanded = false
                                        }
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

@Composable
fun BackupAndSyncTab(
    viewModel: TrackWiseViewModel,
    onImportClick: () -> Unit,
    onClearDataClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    val autoBackupFreq by viewModel.autoBackupFrequency.collectAsState()
    val lastBackupTime by viewModel.lastAutoBackupTime.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Sync, Import, Export ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "DATA & SYNC MANAGEMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandViolet
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.syncDeviceState() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("Sync States", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.exportData() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("Export", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onImportClick,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAmber),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("Import", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- Auto Local Backup ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "AUTO LOCAL BACKUP FREQUENCY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandViolet
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("none" to "None", "hourly" to "Hourly", "daily" to "Daily", "weekly" to "Weekly").forEach { (key, label) ->
                            val isSelected = autoBackupFreq == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) BrandViolet.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) BrandViolet else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.updateAutoBackupFrequency(key) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    if (autoBackupFreq != "none") {
                        val formattedBackupText = if (lastBackupTime > 0L) {
                            val formatted = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()).format(Date(lastBackupTime))
                            "Last auto-backup successfully saved: $formatted"
                        } else {
                            "Auto-backup enabled. Silently saving based on frequency selection."
                        }
                        Text(
                            text = formattedBackupText,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // --- Danger Zone ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrandRose.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "SAFETY & DANGER ZONE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandRose
                    )

                    Button(
                        onClick = onClearDataClick,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRose.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BrandRose, RoundedCornerShape(8.dp))
                            .testTag("clear_data_button"),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("Clear All Data (Start Fresh)", fontSize = 12.sp, color = BrandRose, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDeleteAccountClick,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRose),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_button"),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Account Permanently", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
