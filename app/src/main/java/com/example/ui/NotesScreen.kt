package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.NoteEntity
import com.example.data.NotebookEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

val NOTE_CARD_COLORS = listOf(
    Color(0xFFFFF59D) to "#FFF59D", // Light Yellow
    Color(0xFFFFCC80) to "#FFCC80", // Light Orange
    Color(0xFFC8E6C9) to "#C8E6C9", // Light Green
    Color(0xFFB3E5FC) to "#B3E5FC", // Light Blue
    Color(0xFFE1BEE7) to "#E1BEE7", // Light Purple
    Color(0xFFFFAB91) to "#FFAB91", // Light Peach
    Color(0xFFFFFFFF) to "#FFFFFF"  // White
)

val COVER_PRESETS = listOf("preset_1", "preset_2", "preset_3", "preset_4", "preset_5", "preset_6")

@Composable
fun NotesScreen(
    viewModel: TrackWiseViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        viewModel.ensureDefaultNotebookSeeded()
    }

    val notebooks by viewModel.allNotebooks.collectAsState()
    val selectedNotebook by viewModel.selectedNotebook.collectAsState()
    val activeNoteToEdit by viewModel.activeNoteToEdit.collectAsState()

    if (activeNoteToEdit != null) {
        // Rich Text Note Editor View
        NoteEditorScreen(
            note = activeNoteToEdit!!,
            onSave = { updatedNote ->
                viewModel.updateNote(updatedNote)
                viewModel.closeNoteEditor()
            },
            onDelete = {
                viewModel.deleteNote(activeNoteToEdit!!.id)
                viewModel.closeNoteEditor()
            },
            onBack = { viewModel.closeNoteEditor() }
        )
    } else if (selectedNotebook != null) {
        // Notebook Detail / Notes List View
        NotebookDetailScreen(
            notebook = selectedNotebook!!,
            viewModel = viewModel,
            onBack = { viewModel.selectNotebook(null) }
        )
    } else {
        // Notebooks Grid View
        NotebooksGridScreen(
            notebooks = notebooks,
            viewModel = viewModel,
            onNotebookClick = { viewModel.selectNotebook(it) }
        )
    }
}

// ==========================================
// 1. NOTEBOOKS GRID SCREEN
// ==========================================
@Composable
fun NotebooksGridScreen(
    notebooks: List<NotebookEntity>,
    viewModel: TrackWiseViewModel,
    onNotebookClick: (NotebookEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val isSearchActive by viewModel.isNotebookSearchActive.collectAsState()
    val showCreateDialogFromVm by viewModel.showCreateNotebookDialog.collectAsState()
    var localShowCreateDialog by remember { mutableStateOf(false) }
    var notebookToEdit by remember { mutableStateOf<NotebookEntity?>(null) }
    var notebookMenuTarget by remember { mutableStateOf<NotebookEntity?>(null) }

    val showDialog = showCreateDialogFromVm || localShowCreateDialog || notebookToEdit != null

    val filteredNotebooks = remember(notebooks, searchQuery) {
        if (searchQuery.isBlank()) notebooks
        else notebooks.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar header if active
            AnimatedVisibility(visible = isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search notebooks...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.setNotebookSearchActive(false)
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            if (filteredNotebooks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Book,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No notebooks found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the + button to create a new notebook",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNotebooks, key = { it.id }) { notebook ->
                        Box {
                            NotebookCoverItem(
                                notebook = notebook,
                                onClick = { onNotebookClick(notebook) },
                                onLongClick = { notebookMenuTarget = notebook }
                            )
                            DropdownMenu(
                                expanded = notebookMenuTarget?.id == notebook.id,
                                onDismissRequest = { notebookMenuTarget = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Open") },
                                    onClick = {
                                        notebookMenuTarget = null
                                        onNotebookClick(notebook)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit Title & Cover") },
                                    onClick = {
                                        notebookToEdit = notebook
                                        notebookMenuTarget = null
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Notebook", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        val id = notebook.id
                                        notebookMenuTarget = null
                                        viewModel.deleteNotebook(id)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create / Edit Notebook Dialog
    if (showDialog) {
        NotebookDialog(
            initialNotebook = notebookToEdit,
            onDismiss = {
                localShowCreateDialog = false
                viewModel.setShowCreateNotebookDialog(false)
                notebookToEdit = null
            },
            onConfirm = { title, preset, customUri ->
                if (notebookToEdit != null) {
                    viewModel.updateNotebook(notebookToEdit!!.copy(title = title, coverPreset = preset, customCoverUri = customUri))
                } else {
                    viewModel.createNotebook(title, preset, customCoverUri = customUri)
                }
                localShowCreateDialog = false
                viewModel.setShowCreateNotebookDialog(false)
                notebookToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotebookCoverItem(
    notebook: NotebookEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(140.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Book Cover Card
        Card(
            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 12.dp, bottomEnd = 12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .aspectRatio(0.72f)
                .fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!notebook.customCoverUri.isNullOrBlank()) {
                    AsyncImage(
                        model = notebook.customCoverUri,
                        contentDescription = notebook.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AbstractCanvasCover(preset = notebook.coverPreset)
                }

                // Notebook Spine effect on left
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(10.dp)
                        .background(Color.Black.copy(alpha = 0.25f))
                        .align(Alignment.CenterStart)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = notebook.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// Render Abstract Geometric Painted Canvas Art
@Composable
fun AbstractCanvasCover(preset: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (preset) {
            "preset_2" -> {
                drawRect(Color(0xFFE53935))
                val path1 = Path().apply {
                    moveTo(0f, h * 0.3f)
                    lineTo(w, 0f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path1, Color(0xFFFB8C00))
                val path2 = Path().apply {
                    moveTo(0f, h * 0.75f)
                    lineTo(w, h * 0.4f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path2, Color(0xFF3949AB))
            }
            "preset_3" -> {
                drawRect(Color(0xFF1A237E))
                val path1 = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w * 0.8f, 0f)
                    lineTo(0f, h * 0.6f)
                    close()
                }
                drawPath(path1, Color(0xFF558B2F))
                val path2 = Path().apply {
                    moveTo(w * 0.3f, h)
                    lineTo(w, h * 0.25f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path2, Color(0xFFEF6C00))
            }
            "preset_4" -> {
                drawRect(Color(0xFFB71C1C))
                val path1 = Path().apply {
                    moveTo(0f, h * 0.4f)
                    lineTo(w, h * 0.1f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path1, Color(0xFF0D47A1))
                val path2 = Path().apply {
                    moveTo(w * 0.4f, h)
                    lineTo(w, h * 0.6f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path2, Color(0xFFFBC02D))
            }
            "preset_5" -> {
                drawRect(Color(0xFF1B5E20))
                val path1 = Path().apply {
                    moveTo(0f, h * 0.25f)
                    lineTo(w, h * 0.6f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path1, Color(0xFFF57F17))
                val path2 = Path().apply {
                    moveTo(0f, h * 0.7f)
                    lineTo(w * 0.8f, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path2, Color(0xFFD84315))
            }
            "preset_6" -> {
                drawRect(Color(0xFF4A148C))
                val path1 = Path().apply {
                    moveTo(w * 0.2f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h * 0.7f)
                    close()
                }
                drawPath(path1, Color(0xFF8E24AA))
                val path2 = Path().apply {
                    moveTo(0f, h * 0.5f)
                    lineTo(w * 0.7f, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path2, Color(0xFF00695C))
            }
            else -> {
                // preset_1 default teal/indigo gradient block
                drawRect(Color(0xFF00897B))
                val path1 = Path().apply {
                    moveTo(0f, h * 0.4f)
                    lineTo(w, h * 0.2f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path1, Color(0xFF1E88E5))
                val path2 = Path().apply {
                    moveTo(0f, h * 0.65f)
                    lineTo(w, h * 0.85f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path2, Color(0xFFD81B60))
            }
        }
    }
}

@Composable
fun NotebookDialog(
    initialNotebook: NotebookEntity?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?) -> Unit
) {
    var title by remember { mutableStateOf(initialNotebook?.title ?: "") }
    var selectedPreset by remember { mutableStateOf(initialNotebook?.coverPreset ?: "preset_1") }
    var selectedCustomUri by remember { mutableStateOf<String?>(initialNotebook?.customCoverUri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedCustomUri = uri.toString()
            selectedPreset = "custom"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialNotebook != null) "Edit Notebook" else "New Notebook",
                fontWeight = FontWeight.Bold,
                color = BrandViolet
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notebook Title") },
                    placeholder = { Text("e.g. My Notebook") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Choose Cover Style", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    // Custom Local Image Tile
                    Box(
                        modifier = Modifier
                            .size(42.dp, 58.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (!selectedCustomUri.isNullOrBlank() || selectedPreset == "custom") 3.dp else 1.dp,
                                color = if (!selectedCustomUri.isNullOrBlank() || selectedPreset == "custom") BrandViolet else Color.Gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { photoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!selectedCustomUri.isNullOrBlank()) {
                            AsyncImage(
                                model = selectedCustomUri,
                                contentDescription = "Local Image Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Upload Local Image",
                                    tint = BrandViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("Upload", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandViolet)
                            }
                        }
                    }

                    // Built-in Cover Presets
                    COVER_PRESETS.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .size(42.dp, 58.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(
                                    width = if (selectedPreset == preset && selectedCustomUri == null) 3.dp else 1.dp,
                                    color = if (selectedPreset == preset && selectedCustomUri == null) BrandViolet else Color.Gray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    selectedPreset = preset
                                    selectedCustomUri = null
                                }
                        ) {
                            AbstractCanvasCover(preset = preset)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, selectedPreset, selectedCustomUri) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ==========================================
// 2. NOTEBOOK DETAIL / NOTES LIST SCREEN
// ==========================================
@Composable
fun NotebookDetailScreen(
    notebook: NotebookEntity,
    viewModel: TrackWiseViewModel,
    onBack: () -> Unit
) {
    val notes by viewModel.notesForSelectedNotebook.collectAsState()
    val viewMode by viewModel.notesViewMode.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val isSearchActive by viewModel.isNoteSearchActive.collectAsState()
    var noteMenuTarget by remember { mutableStateOf<NoteEntity?>(null) }

    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) notes
        else notes.filter {
            it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar header if active
            AnimatedVisibility(visible = isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search notes in ${notebook.title}...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.setNoteSearchActive(false)
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.NoteAlt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No notes in ${notebook.title}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the + button to create your first note!",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (viewMode == "grid") {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            Box {
                                NoteStickyCard(
                                    note = note,
                                    onClick = { viewModel.openNoteToEdit(note) },
                                    onLongClick = { noteMenuTarget = note }
                                )
                                DropdownMenu(
                                    expanded = noteMenuTarget?.id == note.id,
                                    onDismissRequest = { noteMenuTarget = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit Note") },
                                        onClick = {
                                            noteMenuTarget = null
                                            viewModel.openNoteToEdit(note)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (note.isPinned) "Unpin Note" else "Pin Note") },
                                        onClick = {
                                            noteMenuTarget = null
                                            viewModel.updateNote(note.copy(isPinned = !note.isPinned))
                                        },
                                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Note", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            val id = note.id
                                            noteMenuTarget = null
                                            viewModel.deleteNote(id)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            Box {
                                NoteStickyCard(
                                    note = note,
                                    onClick = { viewModel.openNoteToEdit(note) },
                                    onLongClick = { noteMenuTarget = note }
                                )
                                DropdownMenu(
                                    expanded = noteMenuTarget?.id == note.id,
                                    onDismissRequest = { noteMenuTarget = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit Note") },
                                        onClick = {
                                            noteMenuTarget = null
                                            viewModel.openNoteToEdit(note)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (note.isPinned) "Unpin Note" else "Pin Note") },
                                        onClick = {
                                            noteMenuTarget = null
                                            viewModel.updateNote(note.copy(isPinned = !note.isPinned))
                                        },
                                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Note", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            val id = note.id
                                            noteMenuTarget = null
                                            viewModel.deleteNote(id)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteStickyCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val cardColor = remember(note.cardColor) {
        try {
            Color(android.graphics.Color.parseColor(note.cardColor))
        } catch (e: Exception) {
            Color(0xFFFFF59D)
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (note.title.isBlank()) "Untitled" else note.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (note.isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black.copy(alpha = 0.6f)
                    )
                }
            }

            if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    fontSize = 13.sp,
                    color = Color.Black.copy(alpha = 0.75f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.updatedAt.take(10),
                    fontSize = 11.sp,
                    color = Color.Black.copy(alpha = 0.5f)
                )
                if (note.reminderDate != null) {
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = "Reminder",
                        modifier = Modifier.size(14.dp),
                        tint = Color.Black.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. RICH TEXT NOTE EDITOR SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: NoteEntity,
    onSave: (NoteEntity) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var contentValue by remember { mutableStateOf(TextFieldValue(note.content)) }
    var selectedColorHex by remember { mutableStateOf(note.cardColor) }
    var isPinned by remember { mutableStateOf(note.isPinned) }
    var reminderDate by remember { mutableStateOf(note.reminderDate) }
    var reminderTime by remember { mutableStateOf(note.reminderTime) }

    fun buildCurrentNote(): NoteEntity {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        return note.copy(
            title = title,
            content = contentValue.text,
            cardColor = selectedColorHex,
            isPinned = isPinned,
            reminderDate = reminderDate,
            reminderTime = reminderTime,
            updatedAt = now
        )
    }

    androidx.activity.compose.BackHandler {
        onSave(buildCurrentNote())
        onBack()
    }

    var showReminderPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Undo & Redo History Stack
    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }

    fun updateContent(newValue: TextFieldValue) {
        if (newValue.text != contentValue.text) {
            undoStack.add(contentValue)
            redoStack.clear()
            if (undoStack.size > 50) undoStack.removeAt(0)
        }
        contentValue = newValue
    }

    fun handleUndo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(contentValue)
            contentValue = prev
        }
    }

    fun handleRedo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(contentValue)
            contentValue = next
        }
    }

    fun insertTextAtSelection(prefix: String, suffix: String = "") {
        val text = contentValue.text
        val selStart = contentValue.selection.start.coerceIn(0, text.length)
        val selEnd = contentValue.selection.end.coerceIn(0, text.length)
        val minSel = minOf(selStart, selEnd)
        val maxSel = maxOf(selStart, selEnd)
        val selectedStr = text.substring(minSel, maxSel)
        val newText = text.substring(0, minSel) + prefix + selectedStr + suffix + text.substring(maxSel)
        val newCursorPos = minSel + prefix.length + selectedStr.length
        updateContent(TextFieldValue(text = newText, selection = TextRange(newCursorPos)))
    }

    fun insertLinePrefix(prefix: String) {
        val text = contentValue.text
        val cursor = contentValue.selection.start.coerceIn(0, text.length)
        val lineStart = if (cursor > 0) text.lastIndexOf('\n', cursor - 1) + 1 else 0
        val safeLineStart = lineStart.coerceIn(0, text.length)
        val newText = text.substring(0, safeLineStart) + prefix + text.substring(safeLineStart)
        val newCursor = cursor + prefix.length
        updateContent(TextFieldValue(text = newText, selection = TextRange(newCursor.coerceIn(0, newText.length))))
    }

    val cardBgColor = remember(selectedColorHex) {
        try {
            Color(android.graphics.Color.parseColor(selectedColorHex))
        } catch (e: Exception) {
            Color(0xFFFFF59D)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = cardBgColor,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                            onSave(
                                note.copy(
                                    title = title,
                                    content = contentValue.text,
                                    cardColor = selectedColorHex,
                                    isPinned = isPinned,
                                    reminderDate = reminderDate,
                                    reminderTime = reminderTime,
                                    updatedAt = now
                                )
                            )
                        }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Save and Back", tint = Color.Black)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Undo
                        IconButton(
                            onClick = { handleUndo() },
                            enabled = undoStack.isNotEmpty()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Default.Undo,
                                contentDescription = "Undo",
                                tint = if (undoStack.isNotEmpty()) Color.Black else Color.Gray.copy(alpha = 0.4f)
                            )
                        }
                        // Redo
                        IconButton(
                            onClick = { handleRedo() },
                            enabled = redoStack.isNotEmpty()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Default.Redo,
                                contentDescription = "Redo",
                                tint = if (redoStack.isNotEmpty()) Color.Black else Color.Gray.copy(alpha = 0.4f)
                            )
                        }
                        // Alarm / Reminder
                        IconButton(onClick = { showReminderPicker = true }) {
                            Icon(
                                Icons.Default.Alarm,
                                contentDescription = "Set Reminder",
                                tint = if (reminderDate != null) BrandViolet else Color.Black
                            )
                        }
                        // Color palette
                        IconButton(onClick = { showColorPicker = true }) {
                            Icon(Icons.Default.Palette, contentDescription = "Card Color", tint = Color.Black)
                        }
                        // More options
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Black)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isPinned) "Unpin Note" else "Pin Note") },
                                    onClick = {
                                        isPinned = !isPinned
                                        showMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Note", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        onDelete()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Horizontal Slider Toolbar sitting directly above keyboard when open
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.imePadding()
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Text Format (Bold / Styling)
                        IconButton(onClick = { insertTextAtSelection("*", "*") }) {
                            Text("A", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        // 2. Bullet list
                        IconButton(onClick = { insertLinePrefix("• ") }) {
                            Icon(Icons.Default.FormatListNumbered, contentDescription = "Bullet List")
                        }
                        // 3. Numbered list
                        IconButton(onClick = { insertLinePrefix("1. ") }) {
                            Icon(Icons.AutoMirrored.Default.FormatListBulleted, contentDescription = "Numbered List")
                        }
                        // 4. Checklist
                        IconButton(onClick = { insertLinePrefix("[ ] ") }) {
                            Icon(Icons.Default.CheckBox, contentDescription = "Checklist")
                        }
                        // 5. Heading
                        IconButton(onClick = { insertLinePrefix("# ") }) {
                            Icon(Icons.Default.Title, contentDescription = "Heading")
                        }
                        // 6. Quote
                        IconButton(onClick = { insertLinePrefix("> ") }) {
                            Icon(Icons.Default.FormatQuote, contentDescription = "Quote")
                        }
                        // 7. Code Block
                        IconButton(onClick = { insertTextAtSelection("\n```\n", "\n```\n") }) {
                            Icon(Icons.Default.Code, contentDescription = "Code Block")
                        }
                        // 8. Indent
                        IconButton(onClick = { insertLinePrefix("    ") }) {
                            Icon(Icons.AutoMirrored.Default.FormatIndentIncrease, contentDescription = "Indent")
                        }
                        // 9. Outdent
                        IconButton(onClick = { insertLinePrefix("") }) {
                            Icon(Icons.AutoMirrored.Default.FormatIndentDecrease, contentDescription = "Outdent")
                        }
                        // 10. Table
                        IconButton(onClick = {
                            insertTextAtSelection("\n| Col 1 | Col 2 |\n|---|---|\n| Item 1 | Item 2 |\n")
                        }) {
                            Icon(Icons.Default.GridOn, contentDescription = "Insert Table")
                        }
                        // 11. Link
                        IconButton(onClick = { insertTextAtSelection("[Link Title](https://", ")") }) {
                            Icon(Icons.Default.Link, contentDescription = "Insert Link")
                        }
                        // 12. Date Stamp
                        IconButton(onClick = {
                            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            insertTextAtSelection(" $today ")
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Insert Date")
                        }
                        // 13. Horizontal Rule
                        IconButton(onClick = { insertTextAtSelection("\n---\n") }) {
                            Icon(Icons.Default.HorizontalRule, contentDescription = "Horizontal Rule")
                        }
                        // 14. Note Color
                        IconButton(onClick = { showColorPicker = true }) {
                            Icon(Icons.Default.Palette, contentDescription = "Note Color")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(cardBgColor)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Note Title Field
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.9f)
                ),
                cursorBrush = SolidColor(BrandViolet),
                decorationBox = { innerTextField ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                "Title",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black.copy(alpha = 0.35f)
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Rich Content Text Field
            BasicTextField(
                value = contentValue,
                onValueChange = { updateContent(it) },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = Color.Black.copy(alpha = 0.85f),
                    lineHeight = 22.sp
                ),
                cursorBrush = SolidColor(BrandViolet),
                decorationBox = { innerTextField ->
                    Box {
                        if (contentValue.text.isEmpty()) {
                            Text(
                                "Start typing your note...",
                                fontSize = 16.sp,
                                color = Color.Black.copy(alpha = 0.35f)
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 350.dp)
            )
        }
    }

    // Color Picker Dialog
    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Choose Note Color", fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NOTE_CARD_COLORS.forEach { (color, hex) ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColorHex == hex) 3.dp else 1.dp,
                                    color = if (selectedColorHex == hex) BrandViolet else Color.Gray.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedColorHex = hex
                                    showColorPicker = false
                                }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Reminder Picker Dialog
    if (showReminderPicker) {
        var selectedDate by remember { mutableStateOf(reminderDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
        var selectedTime by remember { mutableStateOf(reminderTime ?: "09:00 AM") }

        AlertDialog(
            onDismissRequest = { showReminderPicker = false },
            title = { Text("Set Note Reminder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { selectedDate = it },
                        label = { Text("Reminder Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = selectedTime,
                        onValueChange = { selectedTime = it },
                        label = { Text("Reminder Time (e.g. 09:00 AM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        reminderDate = selectedDate
                        reminderTime = selectedTime
                        showReminderPicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                ) {
                    Text("Save Reminder")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        reminderDate = null
                        reminderTime = null
                        showReminderPicker = false
                    }
                ) {
                    Text("Clear")
                }
            }
        )
    }
}
