package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@Composable
fun SettingsScreen(
    viewModel: TrackWiseViewModel,
    onBack: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }
    var isAppearanceExpanded by remember { mutableStateOf(false) }
    var isSoundsExpanded by remember { mutableStateOf(false) }
    var isImportAndExportExpanded by remember { mutableStateOf(false) }
    var isAccountExpanded by remember { mutableStateOf(false) }

    val themeColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Appearance Link Section ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .clickable { isAppearanceExpanded = !isAppearanceExpanded }
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(themeColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "Appearance",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Fonts, font styles, themes, and background presets",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isAppearanceExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    AnimatedVisibility(visible = isAppearanceExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            AppearanceSection(viewModel = viewModel)
                        }
                    }
                }
            }
        }

        // --- 2. Sounds Link Section ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .clickable { isSoundsExpanded = !isSoundsExpanded }
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(themeColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "Sounds",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Sound trigger alerts and audio completion preferences",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isSoundsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    AnimatedVisibility(visible = isSoundsExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            SoundsSection(viewModel = viewModel)
                        }
                    }
                }
            }
        }

        // --- 3. Backup and Sync Link Section ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .clickable { isImportAndExportExpanded = !isImportAndExportExpanded }
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(themeColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "Backup and sync",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Export database states, import records, and backup frequency",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isImportAndExportExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    AnimatedVisibility(visible = isImportAndExportExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            BackupAndSyncSection(
                                viewModel = viewModel,
                                onImportClick = onImportClick
                            )
                        }
                    }
                }
            }
        }

        // --- 4. Account Section (Change Password, Safety, Danger Zone) ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .clickable { isAccountExpanded = !isAccountExpanded }
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(themeColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "Account",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Change account password, system security, and safety controls",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isAccountExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    AnimatedVisibility(visible = isAccountExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            AccountSection(
                                viewModel = viewModel,
                                onClearDataClick = { showClearDataConfirm = true },
                                onDeleteAccountClick = { showDeleteAccountConfirm = true }
                            )
                        }
                    }
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
fun AppearanceSection(viewModel: TrackWiseViewModel) {
    val currentTheme by viewModel.themeMode.collectAsState()
    val bgType by viewModel.appBgType.collectAsState()
    val bgColorName by viewModel.appBgColor.collectAsState()
    val bgGradientName by viewModel.appBgGradient.collectAsState()
    val bgImageUrl by viewModel.appBgImage.collectAsState()
    val bgCustomUri by viewModel.appBgCustomUri.collectAsState()

    var themeModeExpanded by remember { mutableStateOf(false) }
    val fontSize by viewModel.appFontSize.collectAsState()
    val fontStyle by viewModel.appFontStyle.collectAsState()
    var fontSizeExpanded by remember { mutableStateOf(false) }
    var fontStyleExpanded by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setAppBgCustomUri(uri.toString())
            viewModel.setAppBgImage("custom")
            viewModel.setAppBgType("image")
        }
    }

    val themeColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Theme Mode Select
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Theme Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Box {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { themeModeExpanded = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentThemeLabel = when (currentTheme) {
                            "light" -> "Light Mode ☀️"
                            "dark" -> "Dark Mode 🌙"
                            else -> "System Default ⚙️"
                        }
                        Text(
                            text = currentThemeLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand Theme Mode",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                DropdownMenu(
                    expanded = themeModeExpanded,
                    onDismissRequest = { themeModeExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    listOf("light" to "Light Mode ☀️", "dark" to "Dark Mode 🌙", "system" to "System Default ⚙️").forEach { (mode, label) ->
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

        // Font Size Selector
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Font Size", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Box {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { fontSizeExpanded = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$fontSize Size Option",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand Font Size",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                DropdownMenu(
                    expanded = fontSizeExpanded,
                    onDismissRequest = { fontSizeExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    listOf("Small", "Medium", "Large").forEach { sizeOpt ->
                        DropdownMenuItem(
                            text = { Text(sizeOpt, fontSize = 13.sp) },
                            onClick = {
                                viewModel.setAppFontSize(sizeOpt)
                                fontSizeExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Font Style Selector
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Font Style", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Box {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { fontStyleExpanded = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$fontStyle Style Option",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand Font Style",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                DropdownMenu(
                    expanded = fontStyleExpanded,
                    onDismissRequest = { fontStyleExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    listOf("Default", "Sans Serif", "Serif", "Monospace", "Cursive").forEach { styleOpt ->
                        DropdownMenuItem(
                            text = { Text(styleOpt, fontSize = 13.sp) },
                            onClick = {
                                viewModel.setAppFontStyle(styleOpt)
                                fontStyleExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Background Customization Selectors
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "APP BACKGROUND SYSTEM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = themeColor
            )

            // Mode Segmented Buttons
            val bgOptions = listOf(
                "none" to "None ✖️",
                "color" to "Color 🎨",
                "gradient" to "Gradient 🌈",
                "image" to "Image 🖼️",
                "custom" to "Custom 📷"
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                bgOptions.forEach { (type, label) ->
                    val isActive = (type == "custom" && bgImageUrl == "custom" && bgType == "image") || 
                                   (type == "image" && bgImageUrl != "custom" && bgType == "image") ||
                                   (type == bgType && type != "image")
                                   
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) themeColor else Color.Transparent,
                            contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (type == "custom") {
                                    if (bgCustomUri.isEmpty()) {
                                        imagePickerLauncher.launch("image/*")
                                    } else {
                                        viewModel.setAppBgImage("custom")
                                        viewModel.setAppBgType("image")
                                    }
                                } else if (type == "image") {
                                    viewModel.setAppBgImage(BackgroundPresets.textures.first())
                                    viewModel.setAppBgType("image")
                                } else {
                                    viewModel.setAppBgType(type)
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label, 
                                fontSize = 9.sp, 
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Options Content depending on selection
            val isDark = MaterialTheme.colorScheme.onBackground.red > 0.5f
            when {
                bgType == "color" -> {
                    Text("Choose Background Color", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BackgroundPresets.colors.chunked(4).forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chunk.forEach { colorOpt ->
                                    val isSelected = bgType == "color" && bgColorName == colorOpt.name
                                    val swatchColor = if (isDark) colorOpt.darkColor else colorOpt.lightColor
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(swatchColor)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) themeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                viewModel.setAppBgColor(colorOpt.name)
                                                viewModel.setAppBgType("color")
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = if (isDark) Color.White else Color.Black,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                if (chunk.size < 4) {
                                    repeat(4 - chunk.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                bgType == "gradient" -> {
                    Text("Choose Background Gradient", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BackgroundPresets.gradients.chunked(4).forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chunk.forEach { gradOpt ->
                                    val isSelected = bgType == "gradient" && bgGradientName == gradOpt.name
                                    val colors = if (isDark) gradOpt.darkColors else gradOpt.lightColors
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Brush.horizontalGradient(colors))
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) themeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                viewModel.setAppBgGradient(gradOpt.name)
                                                viewModel.setAppBgType("gradient")
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = if (isDark) Color.White else Color.Black,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                if (chunk.size < 4) {
                                    repeat(4 - chunk.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                bgType == "image" && bgImageUrl != "custom" -> {
                    var selectedCategory by remember { mutableStateOf("Textures & Materials") }
                    val categories = listOf("Textures & Materials", "Abstract", "Photography & Cityscapes", "Nature & Landscapes")
                    
                    ScrollableTabRow(
                        selectedTabIndex = categories.indexOf(selectedCategory),
                        containerColor = Color.Transparent,
                        edgePadding = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categories.forEach { cat ->
                            Tab(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedCategory == cat) themeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }

                    val activeList = when (selectedCategory) {
                        "Textures & Materials" -> BackgroundPresets.textures
                        "Abstract" -> BackgroundPresets.abstractImages
                        "Photography & Cityscapes" -> BackgroundPresets.cityscapes
                        else -> BackgroundPresets.landscapes
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        activeList.chunked(3).forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chunk.forEach { imgUrl ->
                                    val isSelected = bgType == "image" && bgImageUrl == imgUrl
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(80.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) themeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                viewModel.setAppBgImage(imgUrl)
                                                viewModel.setAppBgType("image")
                                            }
                                    ) {
                                        AsyncImage(
                                            model = imgUrl,
                                            contentDescription = "Background",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.3f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (chunk.size < 3) {
                                    repeat(3 - chunk.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                bgType == "image" && bgImageUrl == "custom" -> {
                    Text("Your Custom Background Image", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bgCustomUri.isNotEmpty()) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = bgCustomUri,
                                        contentDescription = "Custom Image Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = "No image selected",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        "No custom background selected yet.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (bgCustomUri.isEmpty()) "Select Image" else "Choose Different Image", fontSize = 12.sp)
                    }
                }
                else -> {
                    Text(
                        text = "Plain theme background is active. No custom overlays, colors, gradients, or images will be drawn.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun SoundsSection(viewModel: TrackWiseViewModel) {
    val taskSound by viewModel.taskSound.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val vibrateOnTaskCompletion by viewModel.vibrateOnTaskCompletion.collectAsState()
    val vibrateOnSwipe by viewModel.vibrateOnSwipe.collectAsState()
    val vibrateOnNotification by viewModel.vibrateOnNotification.collectAsState()

    var taskSoundExpanded by remember { mutableStateOf(false) }
    val themeColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Sound Trigger Settings ---
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "TASK & HABIT COMPLETION SOUND",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = themeColor
            )
            Box {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { taskSoundExpanded = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
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
                    listOf(
                        "Chime Gentle",
                        "Victory Bell",
                        "Success Pop",
                        "Digital Sparkle",
                        "Marimba Ring",
                        "Zen Bowl",
                        "Level Up",
                        "Crystal Harp",
                        "Subtle Click",
                        "Acoustic Fanfare",
                        "None"
                    ).forEach { snd ->
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

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        // --- 2. Vibration & Haptics Section ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "VIBRATION & HAPTIC FEEDBACK",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = themeColor
            )

            // Master Vibration Toggle Switch
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Vibration Haptics",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tactile vibration feedback across user interactions",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { viewModel.setVibrationEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            // Sub-options when Vibration is ON
            AnimatedVisibility(visible = vibrationEnabled) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Option A: Task or habit completion vibration
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Task or habit completion vibration",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Vibrate when checking off tasks or habits",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = vibrateOnTaskCompletion,
                                onCheckedChange = { viewModel.setVibrateOnTaskCompletion(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }

                    // Option B: Slide left and slide right vibration
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CompareArrows,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Slide left and slide right vibration",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Vibrate on swipe gestures (e.g., Habit Breaker)",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = vibrateOnSwipe,
                                onCheckedChange = { viewModel.setVibrateOnSwipe(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                    checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }

                    // Option C: Mobile notification vibration
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Mobile notification vibration",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Vibrate on in-app alerts and notifications",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = vibrateOnNotification,
                                onCheckedChange = { viewModel.setVibrateOnNotification(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                                    checkedTrackColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackupAndSyncSection(
    viewModel: TrackWiseViewModel,
    onImportClick: () -> Unit
) {
    val autoBackupFreq by viewModel.autoBackupFrequency.collectAsState()
    val lastBackupTime by viewModel.lastAutoBackupTime.collectAsState()
    val themeColor = MaterialTheme.colorScheme.primary
    var showLogExplorer by remember { mutableStateOf(false) }

    if (showLogExplorer) {
        AllLogsExplorerDialog(
            viewModel = viewModel,
            onDismiss = { showLogExplorer = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Data & Sync Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = themeColor, modifier = Modifier.size(18.dp))
                    Text(
                        text = "DATA & SYNC MANAGEMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.syncDeviceState() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync States", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.exportData() },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onImportClick,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandAmber),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showLogExplorer = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "View All Logged Records",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColor,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        )
                    }
                }
            }
        }

        // --- Auto Local Backup Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Update, contentDescription = null, tint = themeColor, modifier = Modifier.size(18.dp))
                    Text(
                        text = "AUTO LOCAL BACKUP FREQUENCY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                }

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
                                    color = if (isSelected) themeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) themeColor else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.updateAutoBackupFrequency(key) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
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
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AccountSection(
    viewModel: TrackWiseViewModel,
    onClearDataClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    var isChangePasswordOpen by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var changePasswordError by remember { mutableStateOf<String?>(null) }
    var changePasswordSuccess by remember { mutableStateOf<String?>(null) }

    val themeColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Password Change Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isChangePasswordOpen = !isChangePasswordOpen }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock, 
                            contentDescription = null, 
                            tint = themeColor, 
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Change Account Password",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        if (isChangePasswordOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isChangePasswordOpen) "Collapse Change Password" else "Expand Change Password",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = isChangePasswordOpen) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = currentPassword,
                            onValueChange = { currentPassword = it },
                            label = { Text("Current Password", fontSize = 11.sp) },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password", fontSize = 11.sp) },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = confirmNewPassword,
                            onValueChange = { confirmNewPassword = it },
                            label = { Text("Confirm New Password", fontSize = 11.sp) },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        changePasswordError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        changePasswordSuccess?.let {
                            Text(it, color = BrandGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                changePasswordError = null
                                changePasswordSuccess = null
                                if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
                                    changePasswordError = "All fields are required."
                                    return@Button
                                }
                                if (newPassword != confirmNewPassword) {
                                    changePasswordError = "New passwords do not match."
                                    return@Button
                                }
                                if (newPassword.length < 6) {
                                    changePasswordError = "Password must be at least 6 characters."
                                    return@Button
                                }
                                viewModel.changePassword(
                                    currentPasswordRaw = currentPassword,
                                    newPasswordRaw = newPassword,
                                    onSuccess = {
                                        changePasswordSuccess = "Password changed successfully!"
                                        currentPassword = ""
                                        newPassword = ""
                                        confirmNewPassword = ""
                                    },
                                    onError = { err ->
                                        changePasswordError = err
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text("Update Password", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // --- Safety Controls ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = themeColor, modifier = Modifier.size(18.dp))
                    Text(
                        text = "SAFETY CONTROLS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                }

                Text(
                    text = "Clear local statistics, task logs, and habit records without deleting your user profile credentials.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Button(
                    onClick = onClearDataClick,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, themeColor, RoundedCornerShape(10.dp))
                        .testTag("clear_data_button"),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = themeColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All Data (Start Fresh)", fontSize = 11.sp, color = themeColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- Danger Zone ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BrandRose.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Dangerous, contentDescription = null, tint = BrandRose, modifier = Modifier.size(18.dp))
                    Text(
                        text = "DANGER ZONE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandRose
                    )
                }

                Text(
                    text = "Permanently delete your profile and all databases. This is completely irreversible.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Button(
                    onClick = onDeleteAccountClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRose),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_account_button"),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Account Permanently", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AllLogsExplorerDialog(
    viewModel: TrackWiseViewModel,
    onDismiss: () -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    val habits by viewModel.allHabits.collectAsState()
    val financeLogs by viewModel.allFinanceLogs.collectAsState()
    
    val sleepLogs by viewModel.sleepLogs.collectAsState()
    val waterLogs by viewModel.waterLogs.collectAsState()
    val exerciseLogs by viewModel.exerciseLogs.collectAsState()
    val weightEntries by viewModel.weightEntries.collectAsState()
    val vitalReadings by viewModel.vitalReadings.collectAsState()
    val healthIssueLogs by viewModel.healthIssueLogs.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val combinedHealth = remember(sleepLogs, waterLogs, exerciseLogs, weightEntries, vitalReadings, healthIssueLogs) {
        val list = ArrayList<HealthLogItem>()
        sleepLogs.forEach { list.add(HealthLogItem(it.date, "Sleep Logged", "Duration: ${it.hoursSlept} hours (${it.startTime} - ${it.endTime})", "sleep", BrandCyan)) }
        waterLogs.forEach { list.add(HealthLogItem(it.date, "Water Logged", "Drank ${it.glasses} glasses of ${it.goal} target", "water", BrandCyan)) }
        exerciseLogs.forEach { list.add(HealthLogItem(it.date, "Exercise Completed", "${it.exerciseType} for ${it.durationMinutes} mins", "exercise", BrandGreen)) }
        weightEntries.forEach { list.add(HealthLogItem(it.date, "Weight Recorded", "Logged: ${it.weightKg} kg", "weight", BrandRose)) }
        vitalReadings.forEach { list.add(HealthLogItem(it.date, "Vital: ${it.type.replace("_", " ").uppercase()}", "Value: ${it.value} (${it.context ?: "general"})", "vital", BrandAmber)) }
        healthIssueLogs.forEach { list.add(HealthLogItem(it.date, "Health Issue Reported", "${it.issueName} (${it.severity.uppercase()})", "issue", BrandRose)) }
        list.sortedByDescending { it.date }
    }

    val filteredTasks = remember(tasks, searchQuery) {
        if (searchQuery.isBlank()) tasks
        else tasks.filter { it.title.contains(searchQuery, ignoreCase = true) || it.project.contains(searchQuery, ignoreCase = true) || it.priority.contains(searchQuery, ignoreCase = true) }
    }

    val filteredHabits = remember(habits, searchQuery) {
        if (searchQuery.isBlank()) habits
        else habits.filter { it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    val filteredFinance = remember(financeLogs, searchQuery) {
        if (searchQuery.isBlank()) financeLogs
        else financeLogs.filter { it.title.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) || (it.notes ?: "").contains(searchQuery, ignoreCase = true) }
    }

    val filteredHealth = remember(combinedHealth, searchQuery) {
        if (searchQuery.isBlank()) combinedHealth
        else combinedHealth.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) || it.date.contains(searchQuery, ignoreCase = true) }
    }

    val tabs = listOf(
        "Tasks (${filteredTasks.size})",
        "Habits (${filteredHabits.size})",
        "Finance (${filteredFinance.size})",
        "Health (${filteredHealth.size})"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top App Bar / Page Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Historical Log Explorer",
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Unified activity & completion records",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    isSearchExpanded = !isSearchExpanded
                                    if (!isSearchExpanded) searchQuery = ""
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = if (isSearchExpanded) "Close Search" else "Search Logs",
                                    tint = if (isSearchExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Search Bar expansion
                        AnimatedVisibility(visible = isSearchExpanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Filter logs in this tab...", fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tab Navigation
                        ScrollableTabRow(
                            selectedTabIndex = selectedTabIndex,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary,
                            edgePadding = 0.dp,
                            divider = {}
                        ) {
                            tabs.forEachIndexed { index, title ->
                                val isSelected = selectedTabIndex == index
                                Tab(
                                    selected = isSelected,
                                    onClick = { selectedTabIndex = index },
                                    text = {
                                        Text(
                                            title,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Page Content List
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    when (selectedTabIndex) {
                        0 -> {
                            if (filteredTasks.isEmpty()) {
                                HistoricalEmptyState(
                                    icon = Icons.Default.TaskAlt,
                                    title = if (searchQuery.isBlank()) "No Task Records" else "No matching tasks found",
                                    subtitle = if (searchQuery.isBlank()) "Tasks you create and complete will appear here as historic logs." else "Try adjusting your search query."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 24.dp)
                                ) {
                                    items(filteredTasks.size) { i ->
                                        val task = filteredTasks[i]
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(
                                                        task.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                        ) {
                                                            Text(
                                                                task.project.ifBlank { "General" },
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = when (task.priority.lowercase()) {
                                                                "high" -> BrandRose.copy(alpha = 0.15f)
                                                                "medium" -> BrandAmber.copy(alpha = 0.15f)
                                                                else -> BrandGreen.copy(alpha = 0.15f)
                                                            }
                                                        ) {
                                                            Text(
                                                                "${task.priority.uppercase()} Priority",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = when (task.priority.lowercase()) {
                                                                    "high" -> BrandRose
                                                                    "medium" -> BrandAmber
                                                                    else -> BrandGreen
                                                                },
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    if (task.deadline.isNotBlank()) {
                                                        Text("Due: ${task.deadline}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                val statusText = if (task.completed) "Done" else "Pending"
                                                val statusColor = if (task.completed) BrandGreen else BrandAmber
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = statusColor.copy(alpha = 0.15f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                                            contentDescription = null,
                                                            tint = statusColor,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(statusText, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = statusColor)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            if (filteredHabits.isEmpty()) {
                                HistoricalEmptyState(
                                    icon = Icons.Default.Whatshot,
                                    title = if (searchQuery.isBlank()) "No Habit Records" else "No matching habits found",
                                    subtitle = if (searchQuery.isBlank()) "Track habits daily to build streaks and maintain logs." else "Try adjusting your search query."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 24.dp)
                                ) {
                                    items(filteredHabits.size) { i ->
                                        val habit = filteredHabits[i]
                                        val days = com.example.utils.TrackWiseUtils.deserializeStringList(habit.daysCompletedJson)
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(habit.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = BrandRose.copy(alpha = 0.15f)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(Icons.Default.Whatshot, contentDescription = null, tint = BrandRose, modifier = Modifier.size(14.dp))
                                                            Text("${habit.streak} d streak", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BrandRose)
                                                        }
                                                    }
                                                }
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                    ) {
                                                        Text(habit.category, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                    Text("• ${habit.frequency}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                }
                                                if (days.isNotEmpty()) {
                                                    Text("Completions (${days.size}): ${days.takeLast(10).joinToString(", ")}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 2)
                                                } else {
                                                    Text("No completions logged yet.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (filteredFinance.isEmpty()) {
                                HistoricalEmptyState(
                                    icon = Icons.Default.AttachMoney,
                                    title = if (searchQuery.isBlank()) "No Financial Records" else "No matching transactions found",
                                    subtitle = if (searchQuery.isBlank()) "Transactions and expense logs will be presented here in chronological order." else "Try adjusting your search query."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 24.dp)
                                ) {
                                    items(filteredFinance.size) { i ->
                                        val log = filteredFinance[i]
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(log.title.ifBlank { log.category }, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                        ) {
                                                            Text(log.category, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                        }
                                                        Text("• ${log.date}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                    }
                                                    if (!log.notes.isNullOrBlank()) {
                                                        Text("Note: ${log.notes}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                val prefix = when (log.type) {
                                                    "income" -> "+"
                                                    "savings" -> "🐖"
                                                    else -> "-"
                                                }
                                                val amtColor = when (log.type) {
                                                    "income" -> BrandGreen
                                                    "savings" -> BrandCyan
                                                    else -> BrandRose
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = amtColor.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        "$prefix${String.format(Locale.getDefault(), "%.2f", log.amount)}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = amtColor,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            if (filteredHealth.isEmpty()) {
                                HistoricalEmptyState(
                                    icon = Icons.Default.Favorite,
                                    title = if (searchQuery.isBlank()) "No Health Records" else "No matching health entries found",
                                    subtitle = if (searchQuery.isBlank()) "Sleep, water, workouts, vitals, and health issues will be chronicled here." else "Try adjusting your search query."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 24.dp)
                                ) {
                                    items(filteredHealth.size) { i ->
                                        val item = filteredHealth[i]
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    Text(item.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = item.themeColor.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        item.date,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = item.themeColor,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
            }
        }
    }
}

@Composable
fun HistoricalEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

data class HealthLogItem(
    val date: String,
    val title: String,
    val description: String,
    val type: String,
    val themeColor: Color
)
