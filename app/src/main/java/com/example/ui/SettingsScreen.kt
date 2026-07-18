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

@Composable
fun SettingsScreen(
    viewModel: TrackWiseViewModel,
    onBack: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }
    var isThemesAndSoundsExpanded by remember { mutableStateOf(false) }
    var isImportAndExportExpanded by remember { mutableStateOf(false) }

    val themeColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Themes and Sounds Link Section ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .clickable { isThemesAndSoundsExpanded = !isThemesAndSoundsExpanded }
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
                                    text = "Themes and sounds",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Fonts, font styles, themes, and sound triggers",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isThemesAndSoundsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    AnimatedVisibility(visible = isThemesAndSoundsExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            PreferencesSection(viewModel = viewModel)
                        }
                    }
                }
            }
        }

        // --- 2. Import and Export Link Section ---
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
                                Icon(Icons.Default.ImportExport, contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "Import and export",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Database backups, restore, and change password",
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
                                onImportClick = onImportClick,
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
fun PreferencesSection(viewModel: TrackWiseViewModel) {
    val currentTheme by viewModel.themeMode.collectAsState()
    val bgType by viewModel.appBgType.collectAsState()
    val bgColorName by viewModel.appBgColor.collectAsState()
    val bgGradientName by viewModel.appBgGradient.collectAsState()
    val bgImageUrl by viewModel.appBgImage.collectAsState()
    val bgCustomUri by viewModel.appBgCustomUri.collectAsState()
    val taskSound by viewModel.taskSound.collectAsState()

    var themeModeExpanded by remember { mutableStateOf(false) }
    var taskSoundExpanded by remember { mutableStateOf(false) }
    val fontSize by viewModel.appFontSize.collectAsState()
    val fontStyle by viewModel.appFontStyle.collectAsState()
    var fontSizeExpanded by remember { mutableStateOf(false) }
    var fontStyleExpanded by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
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
        // --- Theme & Background Options Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
                    Text(
                        text = "THEME & VISUAL PREFERENCES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                }

                // 1. Theme Mode Select
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
                                Text(text = currentThemeLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand Theme Mode")
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
                                Text(text = "$fontSize Size Option", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand Font Size")
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
                                Text(text = "$fontStyle Style Option", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand Font Style")
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

                // 2. Background Customization Selectors
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
                                            val swatchColor = if (isSystemInDarkTheme()) colorOpt.darkColor else colorOpt.lightColor
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
                                                        tint = if (isSystemInDarkTheme()) Color.White else Color.Black,
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
                                            val colors = if (isSystemInDarkTheme()) gradOpt.darkColors else gradOpt.lightColors
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
                                                        tint = if (isSystemInDarkTheme()) Color.White else Color.Black,
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

        // --- Sound Effects Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
                    Text(
                        text = "SOUND EFFECTS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                }

                // Task Completion Sound Dropdown
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Task Completion Trigger Sound", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Box {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
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

@Composable
fun BackupAndSyncSection(
    viewModel: TrackWiseViewModel,
    onImportClick: () -> Unit,
    onClearDataClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    val autoBackupFreq by viewModel.autoBackupFrequency.collectAsState()
    val lastBackupTime by viewModel.lastAutoBackupTime.collectAsState()

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
        // --- Data & Sync Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
                    Text(
                        text = "DATA & SYNC MANAGEMENT",
                        fontSize = 12.sp,
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
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync States", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.exportData() },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onImportClick,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandAmber),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isChangePasswordOpen = !isChangePasswordOpen }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
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
                            tint = MaterialTheme.colorScheme.primary, 
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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

        // --- Auto Local Backup Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Update, contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
                    Text(
                        text = "AUTO LOCAL BACKUP FREQUENCY",
                        fontSize = 12.sp,
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
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
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
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // --- Danger Zone Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BrandRose.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Dangerous, contentDescription = null, tint = BrandRose, modifier = Modifier.size(20.dp))
                    Text(
                        text = "SAFETY & DANGER ZONE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandRose
                    )
                }

                Button(
                    onClick = onClearDataClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRose.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrandRose, RoundedCornerShape(10.dp))
                        .testTag("clear_data_button"),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = BrandRose, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All Data (Start Fresh)", fontSize = 12.sp, color = BrandRose, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDeleteAccountClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRose),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_account_button"),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Account Permanently", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
