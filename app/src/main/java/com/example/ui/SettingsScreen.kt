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
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import com.example.data.*
import com.example.utils.TrackWiseUtils

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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val tasks by viewModel.allTasks.collectAsState()
    val habits by viewModel.allHabits.collectAsState()
    val badHabits by viewModel.badHabits.collectAsState()
    val financeLogs by viewModel.allFinanceLogs.collectAsState()
    
    val sleepLogs by viewModel.sleepLogs.collectAsState()
    val waterLogs by viewModel.waterLogs.collectAsState()
    val exerciseLogs by viewModel.exerciseLogs.collectAsState()
    val weightEntries by viewModel.weightEntries.collectAsState()
    val vitalReadings by viewModel.vitalReadings.collectAsState()
    val healthIssueLogs by viewModel.healthIssueLogs.collectAsState()
    val tabletReminders by viewModel.tabletReminders.collectAsState()

    val notes by viewModel.allNotes.collectAsState()
    val notebooks by viewModel.allNotebooks.collectAsState()
    val birthdays by viewModel.allBirthdays.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    // Helper function to check if a string represents a real date/time format
    fun isRealDate(ts: String?): Boolean {
        if (ts.isNullOrBlank()) return false
        val cleaned = ts.trim().uppercase()
        if (cleaned == "N/A" || cleaned == "-" || cleaned == "PENDING" || cleaned == "PENDING COMPLETION" || cleaned == "NOT COMPLETED" || cleaned == "ACTIVE GOAL (NO COMPLETIONS YET)" || cleaned == "ACTIVE DEFENSE (NO SLIPS)") {
            return false
        }
        return cleaned.any { it.isDigit() }
    }

    // Log Entry Comparator prioritizing latest real date above, and empty/no-date elements at the very last
    val PlainLogEntryComparator = Comparator<PlainLogEntry> { a, b ->
        val aHasDate = isRealDate(a.sortTimestamp)
        val bHasDate = isRealDate(b.sortTimestamp)
        when {
            aHasDate && bHasDate -> b.sortTimestamp.compareTo(a.sortTimestamp) // descending
            aHasDate && !bHasDate -> -1
            !aHasDate && bHasDate -> 1
            else -> a.activityName.compareTo(b.activityName)
        }
    }

    // 1. Task entries
    val taskItems = remember(tasks) {
        tasks.map { task ->
            val dueStr = if (task.deadline.isNotBlank()) {
                "${task.deadline}${if (!task.dueTime.isNullOrBlank()) " ${task.dueTime}" else if (!task.reminderTime.isNullOrBlank()) " ${task.reminderTime}" else ""}".trim()
            } else "N/A"
            val completedTime = if (task.completed) {
                task.completedAt ?: "${task.deadline} 00:00:00"
            } else {
                "Pending Completion"
            }
            val sortTime = if (task.completed) {
                task.completedAt ?: "${task.deadline} 00:00:00"
            } else {
                "" // Blank so it will be sorted at the very end
            }
            val detailsStr = buildString {
                append("Status: ${if (task.completed) "COMPLETED" else "PENDING"}")
                if (task.completed && !task.completedAt.isNullOrBlank()) {
                    append(" | Completed At: ${task.completedAt}")
                }
                append(" | Priority: ${task.priority.uppercase()}")
                append(" | Points: ${task.points}")
                append(" | Project/Folder: ${task.project.ifBlank { "General" }}")
                append(" | Deadline: ${task.deadline.ifBlank { "N/A" }}")
                if (!task.dueTime.isNullOrBlank()) append(" | Due Time: ${task.dueTime}")
                if (!task.startDate.isNullOrBlank()) append(" | Start Date: ${task.startDate}")
                if (!task.endDate.isNullOrBlank()) append(" | End Date: ${task.endDate}")
                append(" | Recurrence: ${task.repeatType}")
                if (task.repeatType == "custom") {
                    append(" (Every ${task.customRepeatValue} ${task.customRepeatUnit}")
                    if (!task.customRepeatDaysOfWeek.isNullOrBlank()) append(" on ${task.customRepeatDaysOfWeek}")
                    append(")")
                }
                append(" | Remind Me: ${if (task.remindMe) "YES" else "NO"}")
                if (!task.reminderDate.isNullOrBlank()) append(" (Date: ${task.reminderDate})")
                if (!task.reminderTime.isNullOrBlank()) append(" (Time: ${task.reminderTime})")
                if (task.description.isNotBlank()) append(" | Description: ${task.description}")
                if (task.notes.isNotBlank()) append(" | Notes: ${task.notes}")
                val sub = TrackWiseUtils.deserializeSubTasks(task.subtasksJson)
                if (sub.isNotEmpty()) {
                    append(" | Subtasks: ")
                    sub.forEachIndexed { i, s ->
                        append("${i + 1}) ${s.title} [${if (s.completed) "Done" else "Pending"}]")
                        if (i < sub.lastIndex) append(", ")
                    }
                }
            }
            PlainLogEntry(
                dateTime = completedTime,
                activityName = task.title,
                type = "Task",
                dueDateTime = dueStr,
                folderName = task.project.ifBlank { "General" },
                streak = "-",
                habitBreakerCostType = "-",
                details = detailsStr,
                sortTimestamp = sortTime
            )
        }
    }

    // 2. Habit entries (Individual completions as actions, plus active habit definitions)
    val habitItems = remember(habits) {
        val list = mutableListOf<PlainLogEntry>()
        habits.forEach { habit ->
            val completedDays = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson)
            val dueStr = if (!habit.reminderTime.isNullOrBlank()) "Daily ${habit.reminderTime}" else habit.frequency
            val habitDetails = buildString {
                append("Category: ${habit.category.ifBlank { "Wellness" }}")
                append(" | Frequency: ${habit.frequency}")
                append(" | Streak: ${habit.streak} Days (Max: ${habit.maxStreak}d)")
                append(" | Goal Type: ${habit.goalType}")
                append(" | Goal Days: ${habit.goalDays}")
                append(" | Created At: ${habit.createdAt}")
                if (!habit.startDate.isNullOrBlank()) append(" | Start Date: ${habit.startDate}")
                if (!habit.endDate.isNullOrBlank()) append(" | End Date: ${habit.endDate}")
                append(" | Time Bound: ${if (habit.isTimeBound) "YES (${habit.timeBoundDuration})" else "NO"}")
                append(" | Recurrence: ${habit.repeatType}")
                if (habit.repeatType == "custom") {
                    append(" (Every ${habit.customRepeatValue} ${habit.customRepeatUnit}")
                    if (!habit.customRepeatDaysOfWeek.isNullOrBlank()) append(" on ${habit.customRepeatDaysOfWeek}")
                    append(")")
                }
                append(" | Remind Me: ${if (habit.remindMe) "YES" else "NO"}")
                if (!habit.reminderDate.isNullOrBlank()) append(" (Date: ${habit.reminderDate})")
                if (!habit.reminderTime.isNullOrBlank()) append(" (Time: ${habit.reminderTime})")
                if (!habit.dueTime.isNullOrBlank()) append(" (Due: ${habit.dueTime})")
                if (habit.notes.isNotBlank()) append(" | Notes: ${habit.notes}")
                if (habit.quote.isNotBlank()) append(" | Quote: ${habit.quote}")
                append(" | Section: ${habit.section}")
                append(" | Auto Popup: ${if (habit.autoPopup) "YES" else "NO"}")
                if (completedDays.isNotEmpty()) {
                    append(" | Completed Days: ${completedDays.joinToString(", ")}")
                }
                val badges = TrackWiseUtils.deserializeIntList(habit.badgesEarnedJson)
                if (badges.isNotEmpty()) {
                    append(" | Badges Earned (Days): ${badges.joinToString(", ")}")
                }
            }
            if (completedDays.isNotEmpty()) {
                completedDays.forEach { dateStr ->
                    list.add(
                        PlainLogEntry(
                            dateTime = "$dateStr${if (!habit.reminderTime.isNullOrBlank()) " ${habit.reminderTime}" else ""}".trim(),
                            activityName = "${habit.name} (Completed)",
                            type = "Habit: Completion",
                            dueDateTime = dueStr,
                            folderName = habit.category.ifBlank { "Wellness" },
                            streak = "${habit.streak} Days (Max: ${habit.maxStreak}d)",
                            habitBreakerCostType = "-",
                            details = habitDetails,
                            sortTimestamp = dateStr
                        )
                    )
                }
            } else {
                list.add(
                    PlainLogEntry(
                        dateTime = "Active Goal (No completions yet)",
                        activityName = habit.name,
                        type = "Habit: Active Goal",
                        dueDateTime = dueStr,
                        folderName = habit.category.ifBlank { "Wellness" },
                        streak = "${habit.streak} Days (Max: ${habit.maxStreak}d)",
                        habitBreakerCostType = "-",
                        details = habitDetails,
                        sortTimestamp = "" // no completion date, keep it at the last
                    )
                )
            }
        }
        list
    }

    // 3. Habit Breaker entries (Each slip occurrence & surveillance entry)
    val breakerItems = remember(badHabits) {
        val list = mutableListOf<PlainLogEntry>()
        badHabits.forEach { breaker ->
            val costStr = "${breaker.costType}${if (breaker.costValue.isNotBlank()) " (${breaker.costValue})" else ""}"
            val surveillanceDue = if (breaker.reminderTime.isNotBlank()) "Surveillance: ${breaker.reminderTime}" else "Continuous"
            val breakerDetails = buildString {
                append("Avoid Type: ${breaker.avoidType.ifBlank { "Habit Breaker" }}")
                append(" | Priority: ${breaker.priority.uppercase()}")
                append(" | Cost: $costStr")
                append(" | Total Slips: ${breaker.logs.size}")
                append(" | Avoid Count: ${breaker.avoidCount}")
                append(" | Recurring: ${if (breaker.isRecurring) "YES" else "NO"}")
                if (breaker.eventDate.isNotBlank()) append(" | Event/Start Date: ${breaker.eventDate}")
                if (breaker.reminderTime.isNotBlank()) append(" | Reminder/Surveillance Time: ${breaker.reminderTime}")
                if (breaker.tags.isNotEmpty()) append(" | Tags: ${breaker.tags.joinToString(", ")}")
                if (breaker.logs.isNotEmpty()) {
                    append(" | Slip Timestamps: ${breaker.logs.joinToString(", ")}")
                }
            }
            if (breaker.logs.isNotEmpty()) {
                breaker.logs.forEach { timestamp ->
                    list.add(
                        PlainLogEntry(
                            dateTime = timestamp,
                            activityName = "${breaker.name} (Slip-Up)",
                            type = "Habit Breaker: Slip-Up",
                            dueDateTime = surveillanceDue,
                            folderName = breaker.avoidType.ifBlank { "Habit Breaker" },
                            streak = "Clean: 0 Days",
                            habitBreakerCostType = costStr,
                            details = breakerDetails,
                            sortTimestamp = timestamp
                        )
                    )
                }
            }
            list.add(
                PlainLogEntry(
                    dateTime = "Active Defense (No Slips)",
                    activityName = breaker.name,
                    type = "Habit Breaker: Defense",
                    dueDateTime = surveillanceDue,
                    folderName = breaker.avoidType.ifBlank { "Habit Breaker" },
                    streak = "Avoided: ${breaker.avoidCount} times",
                    habitBreakerCostType = costStr,
                    details = breakerDetails,
                    sortTimestamp = "" // no slips, keep it at the last
                )
            )
        }
        list
    }

    // 4. Finance entries
    val financeItems = remember(financeLogs) {
        financeLogs.map { log ->
            val amountFormatted = String.format(Locale.getDefault(), "%.2f", log.amount)
            val financeDetails = buildString {
                append("Type: ${log.type.uppercase()}")
                append(" | Category: ${log.category}")
                append(" | Amount: ₹$amountFormatted")
                if (!log.spendSource.isNullOrBlank()) append(" | Source/Account: ${log.spendSource}")
                if (!log.notes.isNullOrBlank()) append(" | Notes: ${log.notes}")
                append(" | Date: ${log.date}")
            }
            PlainLogEntry(
                dateTime = log.date,
                activityName = log.title.ifBlank { log.category },
                type = "Finance: ${log.type.uppercase()}",
                dueDateTime = "N/A",
                folderName = log.category,
                streak = "-",
                habitBreakerCostType = "-",
                details = financeDetails,
                sortTimestamp = log.date
            )
        }
    }

    // 5. Health entries
    val healthItems = remember(sleepLogs, waterLogs, exerciseLogs, weightEntries, vitalReadings, healthIssueLogs, tabletReminders) {
        val list = mutableListOf<PlainLogEntry>()
        sleepLogs.forEach {
            list.add(
                PlainLogEntry(
                    dateTime = "${it.date} ${it.startTime}",
                    activityName = "Sleep Session",
                    type = "Health: Sleep",
                    dueDateTime = "Wake: ${it.endTime}",
                    folderName = "Sleep & Recovery",
                    streak = "-",
                    habitBreakerCostType = "-",
                    details = "Duration: ${it.hoursSlept} hrs (${it.startTime} - ${it.endTime})${if (!it.notes.isNullOrBlank()) " | Notes: ${it.notes}" else ""}",
                    sortTimestamp = it.date
                )
            )
        }
        waterLogs.forEach {
            val pct = (it.glasses.toFloat() / it.goal.coerceAtLeast(1) * 100).toInt()
            list.add(
                PlainLogEntry(
                    dateTime = it.date,
                    activityName = "Water Hydration",
                    type = "Health: Water",
                    dueDateTime = "Goal: ${it.goal} glasses",
                    folderName = "Hydration",
                    streak = "-",
                    habitBreakerCostType = "-",
                    details = "Drank: ${it.glasses} / ${it.goal} glasses ($pct%)",
                    sortTimestamp = it.date
                )
            )
        }
        exerciseLogs.forEach {
            list.add(
                PlainLogEntry(
                    dateTime = "${it.date}${if (!it.time.isNullOrBlank()) " ${it.time}" else ""}".trim(),
                    activityName = it.exerciseType,
                    type = "Health: Exercise",
                    dueDateTime = "N/A",
                    folderName = "Fitness",
                    streak = "-",
                    habitBreakerCostType = "-",
                    details = "Duration: ${it.durationMinutes} mins | Completed: ${if (it.completed) "YES" else "NO"}${if (!it.notes.isNullOrBlank()) " | Notes: ${it.notes}" else ""}",
                    sortTimestamp = it.date
                )
            )
        }
        weightEntries.forEach {
            list.add(
                PlainLogEntry(
                    dateTime = "${it.date}${if (!it.time.isNullOrBlank()) " ${it.time}" else ""}".trim(),
                    activityName = "Weight Measurement",
                    type = "Health: Weight",
                    dueDateTime = "N/A",
                    folderName = "Body Metrics",
                    streak = "-",
                    habitBreakerCostType = "-",
                    details = "Weight: ${it.weightKg} kg${if (!it.notes.isNullOrBlank()) " | Notes: ${it.notes}" else ""}",
                    sortTimestamp = it.date
                )
            )
        }
        vitalReadings.forEach {
            list.add(
                PlainLogEntry(
                    dateTime = "${it.date}${if (!it.time.isNullOrBlank()) " ${it.time}" else ""}".trim(),
                    activityName = "Vital: ${it.type.replace("_", " ").uppercase()}",
                    type = "Health: Vital",
                    dueDateTime = "N/A",
                    folderName = "Clinical Readings",
                    streak = "-",
                    habitBreakerCostType = "-",
                    details = "Reading: ${it.value}${if (!it.context.isNullOrBlank()) " (${it.context})" else ""}${if (!it.notes.isNullOrBlank()) " | Notes: ${it.notes}" else ""}",
                    sortTimestamp = it.date
                )
            )
        }
        healthIssueLogs.forEach {
            list.add(
                PlainLogEntry(
                    dateTime = "${it.date}${if (!it.time.isNullOrBlank()) " ${it.time}" else ""}".trim(),
                    activityName = it.issueName,
                    type = "Health: Issue",
                    dueDateTime = "N/A",
                    folderName = "Symptom Journal",
                    streak = "-",
                    habitBreakerCostType = "-",
                    details = "Severity: ${it.severity.uppercase()} | Resolved: ${if (it.resolved) "YES" else "NO"}${if (!it.notes.isNullOrBlank()) " | Notes: ${it.notes}" else ""}",
                    sortTimestamp = it.date
                )
            )
        }
        tabletReminders.forEach {
            list.add(
                PlainLogEntry(
                    dateTime = "Daily ${it.timeOfDay}",
                    activityName = it.tabletName,
                    type = "Health: Medication",
                    dueDateTime = it.timeOfDay,
                    folderName = "Medication Tracker",
                    streak = "-",
                    habitBreakerCostType = "-",
                    details = "Dosage: ${it.dosage} | Schedule: ${it.scheduleType}${if (!it.notes.isNullOrBlank()) " | Notes: ${it.notes}" else ""}",
                    sortTimestamp = "2026-08-15"
                )
            )
        }
        list
    }

    // 6. Notes entries
    val noteItems = remember(notes, notebooks) {
        val notebookTitleMap = notebooks.associate { it.id to it.title }
        notes.map { note ->
            val dueStr = if (!note.reminderDate.isNullOrBlank()) {
                "${note.reminderDate}${if (!note.reminderTime.isNullOrBlank()) " ${note.reminderTime}" else ""}".trim()
            } else "N/A"
            val folder = notebookTitleMap[note.notebookId] ?: "My Notebook"
            PlainLogEntry(
                dateTime = note.updatedAt.ifBlank { note.createdAt },
                activityName = note.title.ifBlank { "Untitled Note" },
                type = "Note",
                dueDateTime = dueStr,
                folderName = folder,
                streak = "-",
                habitBreakerCostType = "-",
                details = "Pinned: ${if (note.isPinned) "YES" else "NO"} | Created: ${note.createdAt} | Length: ${note.content.length} chars",
                sortTimestamp = note.updatedAt.ifBlank { note.createdAt }
            )
        }
    }

    // 7. Occasion entries
    val occasionItems = remember(birthdays) {
        birthdays.map { occ ->
            val dueStr = "${occ.date}${if (!occ.reminderTime.isNullOrBlank()) " ${occ.reminderTime}" else ""}".trim()
            PlainLogEntry(
                dateTime = occ.date,
                activityName = occ.name,
                type = "Occasion / Milestone",
                dueDateTime = dueStr,
                folderName = occ.category.ifBlank { "Occasions" },
                streak = "-",
                habitBreakerCostType = "-",
                details = "Event Date: ${occ.date}${if (!occ.giftIdea.isNullOrBlank()) " | Gift/Plan: ${occ.giftIdea}" else ""}",
                sortTimestamp = occ.date
            )
        }
    }

    // All combined master logs list
    val allCombinedItems = remember(taskItems, habitItems, breakerItems, financeItems, healthItems, noteItems, occasionItems) {
        (taskItems + habitItems + breakerItems + financeItems + healthItems + noteItems + occasionItems)
            .sortedByDescending { it.sortTimestamp }
    }

    val activeList = when (selectedTabIndex) {
        0 -> allCombinedItems
        1 -> taskItems.sortedByDescending { it.sortTimestamp }
        2 -> habitItems.sortedByDescending { it.sortTimestamp }
        3 -> breakerItems.sortedByDescending { it.sortTimestamp }
        4 -> financeItems.sortedByDescending { it.sortTimestamp }
        5 -> healthItems.sortedByDescending { it.sortTimestamp }
        6 -> noteItems.sortedByDescending { it.sortTimestamp }
        7 -> occasionItems.sortedByDescending { it.sortTimestamp }
        else -> allCombinedItems
    }

    val filteredList = remember(activeList, searchQuery) {
        if (searchQuery.isBlank()) activeList
        else activeList.filter { entry ->
            entry.activityName.contains(searchQuery, ignoreCase = true) ||
            entry.type.contains(searchQuery, ignoreCase = true) ||
            entry.dateTime.contains(searchQuery, ignoreCase = true) ||
            entry.dueDateTime.contains(searchQuery, ignoreCase = true) ||
            entry.folderName.contains(searchQuery, ignoreCase = true) ||
            entry.streak.contains(searchQuery, ignoreCase = true) ||
            entry.habitBreakerCostType.contains(searchQuery, ignoreCase = true) ||
            entry.details.contains(searchQuery, ignoreCase = true)
        }
    }

    val tabs = listOf(
        "All (${allCombinedItems.size})",
        "Tasks (${taskItems.size})",
        "Habits (${habitItems.size})",
        "Breakers (${breakerItems.size})",
        "Finance (${financeItems.size})",
        "Health (${healthItems.size})",
        "Notes (${noteItems.size})",
        "Occasions (${occasionItems.size})"
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
                // Top App Bar / Page Header (Plain & High Contrast)
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
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Plain Text Ledger • ${filteredList.size} Entries",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Copy All Plain Text Button
                                IconButton(
                                    onClick = {
                                        val ledgerText = generatePlainTextLedger(filteredList)
                                        clipboardManager.setText(AnnotatedString(ledgerText))
                                        Toast.makeText(context, "Copied ${filteredList.size} plain text log entries to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Plain Text Ledger",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
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
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Search Bar expansion
                        AnimatedVisibility(visible = isSearchExpanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Filter logs by activity, type, date, folder, cost type...", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Plain Tab Navigation (Clean, High-Contrast)
                        ScrollableTabRow(
                            selectedTabIndex = selectedTabIndex,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
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
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Page Content List in Plain Text Format
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (filteredList.isEmpty()) {
                        HistoricalEmptyState(
                            icon = Icons.Default.History,
                            title = if (searchQuery.isBlank()) "No Action Records" else "No matching records found",
                            subtitle = if (searchQuery.isBlank()) "All activities, completions, and measurements will be chronicled here in plain text." else "Try adjusting your search query."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(filteredList.size) { index ->
                                val entry = filteredList[index]
                                PlainTextLogRow(
                                    rowNumber = index + 1,
                                    entry = entry
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Plain text log row representing a single action entry with all requested metadata fields:
 * - Row Number
 * - Date and Time
 * - Activity Name
 * - Type
 * - Due Time and Date
 * - Folder Name
 * - Streak
 * - Habit Breaker Cost Type
 * - Details
 * Rendered in clean, neutral plain text without colorful badges or pills.
 */
@Composable
fun PlainTextLogRow(
    rowNumber: Int,
    entry: PlainLogEntry,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Row Number and Date/Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Row #$rowNumber",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Date & Time: ${entry.dateTime}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Activity Name
            Text(
                text = "Activity: ${entry.activityName}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Type & Due Date & Time
            Text(
                text = "Type: ${entry.type}  |  Due: ${entry.dueDateTime}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Folder Name & Streak
            Text(
                text = "Folder: ${entry.folderName}  |  Streak: ${entry.streak}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Habit Breaker Cost Type
            Text(
                text = "Habit Breaker Cost Type: ${entry.habitBreakerCostType}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Details / Notes / Status
            Text(
                text = "Details: ${entry.details}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Generates an exportable plain-text table ledger of all records.
 */
fun generatePlainTextLedger(entries: List<PlainLogEntry>): String {
    val sb = StringBuilder()
    sb.appendLine("================================================================================")
    sb.appendLine("                     TRACKWISE HISTORICAL LOG LEDGER                           ")
    sb.appendLine("================================================================================")
    sb.appendLine("Total Action Entries: ${entries.size}")
    sb.appendLine("--------------------------------------------------------------------------------")
    entries.forEachIndexed { index, entry ->
        val rowNum = index + 1
        sb.appendLine("[ROW #$rowNum]")
        sb.appendLine("• Date & Time            : ${entry.dateTime}")
        sb.appendLine("• Activity Name          : ${entry.activityName}")
        sb.appendLine("• Type                   : ${entry.type}")
        sb.appendLine("• Due Time & Date        : ${entry.dueDateTime}")
        sb.appendLine("• Folder Name            : ${entry.folderName}")
        sb.appendLine("• Streak                 : ${entry.streak}")
        sb.appendLine("• Habit Breaker Cost Type: ${entry.habitBreakerCostType}")
        sb.appendLine("• Details                : ${entry.details}")
        sb.appendLine("--------------------------------------------------------------------------------")
    }
    return sb.toString()
}

data class PlainLogEntry(
    val rowNumber: Int = 0,
    val dateTime: String,
    val activityName: String,
    val type: String,
    val dueDateTime: String,
    val folderName: String,
    val streak: String,
    val habitBreakerCostType: String,
    val details: String,
    val sortTimestamp: String
)

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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
