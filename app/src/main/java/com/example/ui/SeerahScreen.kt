package com.example.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

// --- Seerah Data Structures ---

data class SeerahEvent(
    val id: String,
    val yearG: Int, // Gregorian Year
    val yearH: String, // Hijri Year (e.g., "53 BH", "1 AH")
    val period: String, // "Makkah" or "Madinah"
    val titleEn: String,
    val titleAr: String,
    val titleUr: String,
    val descEn: String,
    val descAr: String,
    val descUr: String,
    val locationEn: String,
    val locationAr: String,
    val locationUr: String,
    val companions: List<String>,
    val verses: List<String>,
    val hadiths: List<String>,
    val lessonsEn: String,
    val lessonsAr: String,
    val lessonsUr: String,
    val source: String,
    val hijriMonth: Int // 1 to 12
)

data class DailyRoutineItem(
    val id: String,
    val timeEn: String,
    val timeAr: String,
    val timeUr: String,
    val titleEn: String,
    val titleAr: String,
    val titleUr: String,
    val descEn: String,
    val descAr: String,
    val descUr: String,
    val source: String
)

data class BattleItem(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val nameUr: String,
    val yearH: String,
    val descEn: String,
    val descAr: String,
    val descUr: String,
    val statsEn: String,
    val statsAr: String,
    val statsUr: String,
    val lessonEn: String,
    val lessonAr: String,
    val lessonUr: String,
    val source: String
)

data class FamilyMember(
    val nameEn: String,
    val nameAr: String,
    val nameUr: String,
    val relationEn: String,
    val relationAr: String,
    val relationUr: String,
    val descEn: String,
    val descAr: String,
    val descUr: String
)

data class CompanionItem(
    val nameEn: String,
    val nameAr: String,
    val nameUr: String,
    val relationEn: String,
    val relationAr: String,
    val relationUr: String,
    val meritEn: String,
    val meritAr: String,
    val meritUr: String,
    val quoteEn: String,
    val quoteAr: String,
    val quoteUr: String
)

data class SeerahMapLocation(
    val nameEn: String,
    val nameAr: String,
    val nameUr: String,
    val xPercent: Float, // percentage position on 0-100 canvas grid
    val yPercent: Float,
    val infoEn: String,
    val infoAr: String,
    val infoUr: String
)

// --- Language Selector ---
enum class SeerahLanguage(val code: String, val label: String, val flag: String) {
    ENGLISH("en", "English", "🇬🇧"),
    ARABIC("ar", "العربية", "🇸🇦"),
    URDU("ur", "اردو", "🇵🇰")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeerahScreen(
    viewModel: TrackWiseViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Preferences & Languages state
    var selectedLanguage by remember { mutableStateOf(SeerahLanguage.ENGLISH) }
    var activeSubTab by remember { mutableStateOf("timeline") } // timeline, months, daily, battles, family, map, ai, bookmarks

    // Offline data loading
    val allEvents = remember { getSeerahEvents() }
    val dailyRoutines = remember { getDailyRoutines() }
    val battles = remember { getBattles() }
    val familyMembers = remember { getFamilyMembers() }
    val companions = remember { getCompanions() }
    val mapLocations = remember { getMapLocations() }

    // Search and Filters
    var searchQuery by remember { mutableStateOf("") }
    var periodFilter by remember { mutableStateOf("All") } // All, Makkah, Madinah

    // Bookmarks and local notes saved in SharedPreferences
    val prefs = remember { context.getSharedPreferences("seerah_preferences", Context.MODE_PRIVATE) }
    var bookmarkedIds by remember {
        mutableStateOf(prefs.getStringSet("seerah_bookmarks", emptySet()) ?: emptySet())
    }
    
    // Loaded Notes mapping (event_id -> note_text)
    val notesMap = remember {
        val loaded = mutableStateMapOf<String, String>()
        allEvents.forEach { ev ->
            loaded[ev.id] = prefs.getString("note_${ev.id}", "") ?: ""
        }
        loaded
    }

    // Function to toggle bookmark
    fun toggleBookmark(id: String) {
        val updated = bookmarkedIds.toMutableSet()
        if (updated.contains(id)) {
            updated.remove(id)
        } else {
            updated.add(id)
        }
        bookmarkedIds = updated
        prefs.edit().putStringSet("seerah_bookmarks", updated).apply()
    }

    // Function to save notes
    fun saveNote(id: String, note: String) {
        notesMap[id] = note
        prefs.edit().putString("note_$id", note).apply()
    }

    // AI chat history state
    var aiChatHistory by remember {
        mutableStateOf(
            listOf(
                ChatMessage("ai", getLocalString("ai_greeting", selectedLanguage))
            )
        )
    }
    var aiQueryInput by remember { mutableStateOf("") }
    var isAiThinking by remember { mutableStateOf(false) }

    // Suggestive Prompts for Seerah Assistant
    val suggestionPrompts = listOf(
        Pair("What was the Character of Prophet Muhammad ﷺ?", "Tell me about the beautiful, humble character and physical appearance of Prophet Muhammad ﷺ."),
        Pair("Explain Treaty of Hudaybiyyah", "Can you explain the background, key terms, and victory of the Treaty of Hudaybiyyah?"),
        Pair("How did He treat children and neighbors?", "How did the Prophet ﷺ treat children, family, neighbors, and even his adversaries?"),
        Pair("Details of the First Revelation", "Describe the event of the first revelation in Cave Hira and how Khadijah (RA) comforted him.")
    )

    // Daily Notifications Setup Simulation state
    var isDailyNotificationEnabled by remember {
        mutableStateOf(prefs.getBoolean("daily_seerah_notif", true))
    }

    // Today in Seerah event calculation
    val todayEvent = remember(selectedLanguage) {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val index = dayOfYear % allEvents.size
        allEvents[index]
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom Styled Seerah Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Seerah Companion",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandGreen
                        )
                        Text(
                            text = "Prophet Muhammad ﷺ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    // Language Selector Dropdown Button
                    var langMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { langMenuExpanded = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandGreen.copy(alpha = 0.12f),
                                contentColor = BrandGreen
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "${selectedLanguage.flag} ${selectedLanguage.label}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                        }

                        DropdownMenu(
                            expanded = langMenuExpanded,
                            onDismissRequest = { langMenuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            SeerahLanguage.values().forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text("${lang.flag} ${lang.label}", fontSize = 13.sp) },
                                    onClick = {
                                        selectedLanguage = lang
                                        langMenuExpanded = false
                                        // Update AI Greeting as well when language changes
                                        aiChatHistory = listOf(
                                            ChatMessage("ai", getLocalString("ai_greeting", lang))
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Sub-Tabs list
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val subTabs = listOf(
                        Triple("timeline", "📜", "tab_timeline"),
                        Triple("months", "📅", "tab_months"),
                        Triple("daily", "🕌", "tab_daily"),
                        Triple("battles", "⚔️", "tab_battles"),
                        Triple("family", "🌳", "tab_family"),
                        Triple("map", "🗺️", "tab_map"),
                        Triple("ai", "💬", "tab_ai"),
                        Triple("bookmarks", "🔖", "tab_bookmarks")
                    )

                    items(subTabs) { (id, emoji, labelKey) ->
                        val isSelected = activeSubTab == id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) BrandGreen else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clickable { activeSubTab = id }
                                .testTag("seerah_sub_tab_$id")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = emoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = getLocalString(labelKey, selectedLanguage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Screen Body Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            when (activeSubTab) {
                "timeline" -> {
                    // Chronological Timeline view
                    val filtered = allEvents.filter {
                        val matchesSearch = if (selectedLanguage == SeerahLanguage.ARABIC) {
                            it.titleAr.contains(searchQuery, ignoreCase = true) || it.descAr.contains(searchQuery, ignoreCase = true)
                        } else if (selectedLanguage == SeerahLanguage.URDU) {
                            it.titleUr.contains(searchQuery, ignoreCase = true) || it.descUr.contains(searchQuery, ignoreCase = true)
                        } else {
                            it.titleEn.contains(searchQuery, ignoreCase = true) || it.descEn.contains(searchQuery, ignoreCase = true)
                        }
                        val matchesPeriod = periodFilter == "All" || it.period.equals(periodFilter, ignoreCase = true)
                        matchesSearch && matchesPeriod
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // "Today in Seerah" feature display
                        TodayInSeerahBanner(todayEvent, selectedLanguage, onInspect = {
                            searchQuery = if (selectedLanguage == SeerahLanguage.ARABIC) todayEvent.titleAr else if (selectedLanguage == SeerahLanguage.URDU) todayEvent.titleUr else todayEvent.titleEn
                        })

                        Spacer(modifier = Modifier.height(8.dp))

                        // Filter / Search Toolbar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(getLocalString("search_hint", selectedLanguage), fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Period Chip (All, Makkah, Madinah)
                            Box {
                                var showPeriodMenu by remember { mutableStateOf(false) }
                                Button(
                                    onClick = { showPeriodMenu = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    val periodLabel = when (periodFilter) {
                                        "Makkah" -> getLocalString("period_makkah", selectedLanguage)
                                        "Madinah" -> getLocalString("period_madinah", selectedLanguage)
                                        else -> getLocalString("filter_all", selectedLanguage)
                                    }
                                    Text(periodLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(14.dp).padding(start = 4.dp))
                                }

                                DropdownMenu(
                                    expanded = showPeriodMenu,
                                    onDismissRequest = { showPeriodMenu = false }
                                ) {
                                    listOf("All", "Makkah", "Madinah").forEach { p ->
                                        DropdownMenuItem(
                                            text = {
                                                val pLabel = when (p) {
                                                    "Makkah" -> getLocalString("period_makkah", selectedLanguage)
                                                    "Madinah" -> getLocalString("period_madinah", selectedLanguage)
                                                    else -> getLocalString("filter_all", selectedLanguage)
                                                }
                                                Text(pLabel)
                                            },
                                            onClick = {
                                                periodFilter = p
                                                showPeriodMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Timeline Items List
                        if (filtered.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(getLocalString("no_results", selectedLanguage), fontSize = 14.sp, color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filtered) { ev ->
                                    val isBookmarked = bookmarkedIds.contains(ev.id)
                                    SeerahEventCard(
                                        event = ev,
                                        lang = selectedLanguage,
                                        isBookmarked = isBookmarked,
                                        onBookmarkClick = { toggleBookmark(ev.id) },
                                        savedNote = notesMap[ev.id] ?: "",
                                        onSaveNote = { note -> saveNote(ev.id, note) }
                                    )
                                }
                            }
                        }
                    }
                }

                "months" -> {
                    // Hijri month historical view
                    var selectedMonth by remember { mutableStateOf(1) }
                    val monthlyEvents = allEvents.filter { it.hijriMonth == selectedMonth }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = getLocalString("monthly_view_title", selectedLanguage),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Horizontal list of Islamic Months
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(12) { index ->
                                val monthId = index + 1
                                val isSelected = selectedMonth == monthId
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) BrandGreen else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable { selectedMonth = monthId }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "$monthId",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = getHijriMonthName(monthId, selectedLanguage),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (monthlyEvents.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = getLocalString("no_month_events", selectedLanguage),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(monthlyEvents) { ev ->
                                    val isBookmarked = bookmarkedIds.contains(ev.id)
                                    SeerahEventCard(
                                        event = ev,
                                        lang = selectedLanguage,
                                        isBookmarked = isBookmarked,
                                        onBookmarkClick = { toggleBookmark(ev.id) },
                                        savedNote = notesMap[ev.id] ?: "",
                                        onSaveNote = { note -> saveNote(ev.id, note) }
                                    )
                                }
                            }
                        }
                    }
                }

                "daily" -> {
                    // Daily routine of the Prophet ﷺ
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = BrandGreen.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = getLocalString("daily_introduction_title", selectedLanguage),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreen
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = getLocalString("daily_introduction_desc", selectedLanguage),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(dailyRoutines) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (selectedLanguage == SeerahLanguage.ARABIC) item.timeAr else if (selectedLanguage == SeerahLanguage.URDU) item.timeUr else item.timeEn,
                                            color = BrandGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier
                                                .background(BrandGreen.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = BrandAmber, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (selectedLanguage == SeerahLanguage.ARABIC) item.titleAr else if (selectedLanguage == SeerahLanguage.URDU) item.titleUr else item.titleEn,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (selectedLanguage == SeerahLanguage.ARABIC) item.descAr else if (selectedLanguage == SeerahLanguage.URDU) item.descUr else item.descEn,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${getLocalString("source_ref", selectedLanguage)}: ${item.source}",
                                        fontSize = 10.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                "battles" -> {
                    // Battle History Chronicles
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(battles) { battle ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (selectedLanguage == SeerahLanguage.ARABIC) battle.nameAr else if (selectedLanguage == SeerahLanguage.URDU) battle.nameUr else battle.nameEn,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BrandOrange
                                        )

                                        Text(
                                            text = battle.yearH,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier
                                                .background(BrandOrange, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = if (selectedLanguage == SeerahLanguage.ARABIC) battle.descAr else if (selectedLanguage == SeerahLanguage.URDU) battle.descUr else battle.descEn,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        lineHeight = 18.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Quick Stats Panel
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Info, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = getLocalString("stats_title", selectedLanguage),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BrandOrange
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (selectedLanguage == SeerahLanguage.ARABIC) battle.statsAr else if (selectedLanguage == SeerahLanguage.URDU) battle.statsUr else battle.statsEn,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Lessons Panel
                                    Text(
                                        text = getLocalString("lessons_learnt", selectedLanguage),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreen
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (selectedLanguage == SeerahLanguage.ARABIC) battle.lessonAr else if (selectedLanguage == SeerahLanguage.URDU) battle.lessonUr else battle.lessonEn,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "${getLocalString("source_ref", selectedLanguage)}: ${battle.source}",
                                        fontSize = 10.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                "family" -> {
                    // Tree of family & Network of companions
                    var activeRelationSubTab by remember { mutableStateOf("family") } // family, companions

                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { activeRelationSubTab = "family" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeRelationSubTab == "family") BrandGreen else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (activeRelationSubTab == "family") Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(getLocalString("family_tree", selectedLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { activeRelationSubTab = "companions" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeRelationSubTab == "companions") BrandGreen else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (activeRelationSubTab == "companions") Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(getLocalString("companions", selectedLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (activeRelationSubTab == "family") {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(familyMembers) { member ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.15f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = if (selectedLanguage == SeerahLanguage.ARABIC) member.nameAr else if (selectedLanguage == SeerahLanguage.URDU) member.nameUr else member.nameEn,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )

                                                Text(
                                                    text = if (selectedLanguage == SeerahLanguage.ARABIC) member.relationAr else if (selectedLanguage == SeerahLanguage.URDU) member.relationUr else member.relationEn,
                                                    fontSize = 10.sp,
                                                    color = BrandGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .background(BrandGreen.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = if (selectedLanguage == SeerahLanguage.ARABIC) member.descAr else if (selectedLanguage == SeerahLanguage.URDU) member.descUr else member.descEn,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(companions) { companion ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, BrandViolet.copy(alpha = 0.15f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = if (selectedLanguage == SeerahLanguage.ARABIC) companion.nameAr else if (selectedLanguage == SeerahLanguage.URDU) companion.nameUr else companion.nameEn,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )

                                                Text(
                                                    text = if (selectedLanguage == SeerahLanguage.ARABIC) companion.relationAr else if (selectedLanguage == SeerahLanguage.URDU) companion.relationUr else companion.relationEn,
                                                    fontSize = 10.sp,
                                                    color = BrandViolet,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .background(BrandViolet.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = if (selectedLanguage == SeerahLanguage.ARABIC) companion.meritAr else if (selectedLanguage == SeerahLanguage.URDU) companion.meritUr else companion.meritEn,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                lineHeight = 17.sp
                                            )

                                            if (companion.quoteEn.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = BrandViolet.copy(alpha = 0.05f)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "\"${if (selectedLanguage == SeerahLanguage.ARABIC) companion.quoteAr else if (selectedLanguage == SeerahLanguage.URDU) companion.quoteUr else companion.quoteEn}\"",
                                                        fontSize = 11.sp,
                                                        fontStyle = FontStyle.Italic,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                        modifier = Modifier.padding(10.dp)
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

                "map" -> {
                    // Stylized Interactive canvas-based map of the Seerah geographical context
                    var selectedLoc by remember { mutableStateOf<SeerahMapLocation?>(null) }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = getLocalString("interactive_map_title", selectedLanguage),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandGreen
                        )
                        Text(
                            text = getLocalString("interactive_map_hint", selectedLanguage),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Render Canvas map
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .border(1.dp, BrandGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        ) {
                            val mapLineColor = BrandGreen.copy(alpha = 0.4f)
                            val pinColorDefault = BrandGreen
                            val pinColorSelected = BrandOrange

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { /* Handle generic tap */ }
                            ) {
                                val w = size.width
                                val h = size.height

                                // Draw simulated Red Sea coast line
                                val coastPath = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(w * 0.2f, 0f)
                                    quadraticTo(w * 0.25f, h * 0.3f, w * 0.15f, h * 0.6f)
                                    quadraticTo(w * 0.1f, h * 0.8f, w * 0.05f, h * 1f)
                                }
                                drawPath(
                                    path = coastPath,
                                    color = Color(0xFF0284C7).copy(alpha = 0.3f),
                                    style = Stroke(width = 4f)
                                )

                                // Draw connections between Makkah, Madinah, Badr, Uhud, Ta'if, Khaibar, Tabuk
                                // Coordinate maps:
                                // Tabuk (35%, 15%), Khaibar (42%, 35%), Uhud (45%, 46%), Madinah (45%, 48%), Badr (30%, 55%), Makkah (48%, 70%), Ta'if (55%, 78%), Abyssinia (10%, 92%)
                                val locsMap = mapLocations.associate { loc ->
                                    loc.nameEn to Offset(w * loc.xPercent, h * loc.yPercent)
                                }

                                // Draw chronological routes
                                val pathRoute = androidx.compose.ui.graphics.Path().apply {
                                    locsMap["Makkah"]?.let { moveTo(it.x, it.y) }
                                    locsMap["Ta'if"]?.let { lineTo(it.x, it.y) }
                                    locsMap["Makkah"]?.let { moveTo(it.x, it.y) }
                                    locsMap["Badr"]?.let { lineTo(it.x, it.y) }
                                    locsMap["Madinah"]?.let { lineTo(it.x, it.y) }
                                    locsMap["Uhud"]?.let { lineTo(it.x, it.y) }
                                    locsMap["Khaibar"]?.let { lineTo(it.x, it.y) }
                                    locsMap["Tabuk"]?.let { lineTo(it.x, it.y) }
                                }
                                drawPath(
                                    path = pathRoute,
                                    color = mapLineColor,
                                    style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                                )
                            }

                            // Lay interactive Pins over canvas
                            mapLocations.forEach { loc ->
                                val isPinSelected = selectedLoc?.nameEn == loc.nameEn
                                Box(
                                    modifier = Modifier
                                        .absoluteOffset(
                                            x = (loc.xPercent * 340).dp, // scaling multiplier
                                            y = (loc.yPercent * 240).dp
                                        )
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isPinSelected) BrandOrange.copy(alpha = 0.2f) else BrandGreen.copy(
                                                alpha = 0.15f
                                            )
                                        )
                                        .clickable { selectedLoc = loc }
                                        .wrapContentSize(Alignment.Center)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = loc.nameEn,
                                        tint = if (isPinSelected) BrandOrange else BrandGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Location Details Card
                        val loc = selectedLoc ?: mapLocations.first()
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = if (selectedLanguage == SeerahLanguage.ARABIC) loc.nameAr else if (selectedLanguage == SeerahLanguage.URDU) loc.nameUr else loc.nameEn,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (selectedLanguage == SeerahLanguage.ARABIC) loc.infoAr else if (selectedLanguage == SeerahLanguage.URDU) loc.infoUr else loc.infoEn,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                "ai" -> {
                    // AI Seerah assistant
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = getLocalString("ai_title", selectedLanguage),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandGreen
                        )
                        Text(
                            text = getLocalString("ai_subtitle", selectedLanguage),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Message History thread
                        val listScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(listScrollState)
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            aiChatHistory.forEach { msg ->
                                val isUser = msg.sender == "user"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isUser) BrandGreen else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isUser) 16.dp else 4.dp,
                                            bottomEnd = if (isUser) 4.dp else 16.dp
                                        ),
                                        modifier = Modifier.fillMaxWidth(0.85f)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = if (isUser) getLocalString("ai_you", selectedLanguage) else "Scholar AI 🕌",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isUser) Color.White.copy(alpha = 0.8f) else BrandGreen
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = msg.text,
                                                fontSize = 12.sp,
                                                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }
                            }

                            if (isAiThinking) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.padding(end = 40.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = BrandGreen)
                                            Text(getLocalString("ai_thinking", selectedLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Suggestion Chips
                        if (aiChatHistory.size == 1 && !isAiThinking) {
                            Text(
                                text = "💡 Questions to ask Scholar Assistant:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(suggestionPrompts) { (display, query) ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = BrandGreen.copy(alpha = 0.08f)),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.2f)),
                                        modifier = Modifier.clickable {
                                            aiQueryInput = query
                                        }
                                    ) {
                                        Text(
                                            text = display,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Input field row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = aiQueryInput,
                                onValueChange = { aiQueryInput = it },
                                placeholder = { Text("Ask Scholar...", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )

                            IconButton(
                                onClick = {
                                    if (aiQueryInput.isNotBlank()) {
                                        val userMsg = aiQueryInput
                                        aiQueryInput = ""
                                        aiChatHistory = aiChatHistory + ChatMessage("user", userMsg)
                                        isAiThinking = true
                                        
                                        coroutineScope.launch {
                                            val response = queryGeminiSeerahScholar(userMsg, selectedLanguage)
                                            aiChatHistory = aiChatHistory + ChatMessage("ai", response)
                                            isAiThinking = false
                                        }
                                    }
                                },
                                enabled = aiQueryInput.isNotBlank() && !isAiThinking,
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (aiQueryInput.isNotBlank() && !isAiThinking) BrandGreen else Color.Gray.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                "bookmarks" -> {
                    // Saved/Bookmarked Events and User Notes / Reflections Tab
                    val bookmarkedEvents = allEvents.filter { bookmarkedIds.contains(it.id) }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = getLocalString("tab_bookmarks", selectedLanguage),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen
                            )

                            // Toggle notifications setting
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Daily Notifs 🔔", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Switch(
                                    checked = isDailyNotificationEnabled,
                                    onCheckedChange = {
                                        isDailyNotificationEnabled = it
                                        prefs.edit().putBoolean("daily_seerah_notif", it).apply()
                                    },
                                    modifier = Modifier.scale(0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (bookmarkedEvents.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = getLocalString("no_bookmarks", selectedLanguage),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(bookmarkedEvents) { ev ->
                                    SeerahEventCard(
                                        event = ev,
                                        lang = selectedLanguage,
                                        isBookmarked = true,
                                        onBookmarkClick = { toggleBookmark(ev.id) },
                                        savedNote = notesMap[ev.id] ?: "",
                                        onSaveNote = { note -> saveNote(ev.id, note) }
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

// Extension to scale elements easily
fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.padding(all = 0.dp) // dummy to allow method chaining
)

// --- Chat Message Structure ---
data class ChatMessage(
    val sender: String, // "user" or "ai"
    val text: String
)

// --- Today in Seerah Highlight view ---
@Composable
fun TodayInSeerahBanner(
    event: SeerahEvent,
    lang: SeerahLanguage,
    onInspect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BrandGreen.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspect() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(BrandGreen.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getLocalString("today_in_seerah", lang),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandGreen
                )
                Text(
                    text = if (lang == SeerahLanguage.ARABIC) event.titleAr else if (lang == SeerahLanguage.URDU) event.titleUr else event.titleEn,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${event.yearH} (${event.yearG} CE) • ${event.locationEn}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = BrandGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// --- Seerah Event Card Component ---
@Composable
fun SeerahEventCard(
    event: SeerahEvent,
    lang: SeerahLanguage,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    savedNote: String,
    onSaveNote: (String) -> Unit
) {
    var expandedNoteInput by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf(savedNote) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (event.period == "Makkah") BrandViolet.copy(alpha = 0.2f) else BrandGreen.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.yearH,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(
                                if (event.period == "Makkah") BrandViolet else BrandGreen,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${event.yearG} CE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { expandedNoteInput = !expandedNoteInput }) {
                        Icon(
                            imageVector = if (savedNote.isNotBlank()) Icons.Default.EditNote else Icons.Default.NoteAdd,
                            contentDescription = "Notes",
                            tint = if (savedNote.isNotBlank()) BrandGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onBookmarkClick) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) BrandOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Event Title
            Text(
                text = if (lang == SeerahLanguage.ARABIC) event.titleAr else if (lang == SeerahLanguage.URDU) event.titleUr else event.titleEn,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (lang == SeerahLanguage.ARABIC) event.locationAr else if (lang == SeerahLanguage.URDU) event.locationUr else event.locationEn,
                    fontSize = 11.sp,
                    color = BrandGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Description
            Text(
                text = if (lang == SeerahLanguage.ARABIC) event.descAr else if (lang == SeerahLanguage.URDU) event.descUr else event.descEn,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 19.sp
            )

            // Verses & Hadith reference display
            if (event.verses.isNotEmpty() || event.hadiths.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                ) {
                    if (event.verses.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${getLocalString("related_quran", lang)}:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen
                            )
                        }
                        event.verses.forEach { v ->
                            Text(text = "• $v", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        }
                    }

                    if (event.hadiths.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FormatQuote, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${getLocalString("related_hadith", lang)}:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandViolet
                            )
                        }
                        event.hadiths.forEach { h ->
                            Text(text = "• $h", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // Lessons learned
            Spacer(modifier = Modifier.height(10.dp))
            Column {
                Text(
                    text = getLocalString("lessons_learnt", lang),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen
                )
                Text(
                    text = if (lang == SeerahLanguage.ARABIC) event.lessonsAr else if (lang == SeerahLanguage.URDU) event.lessonsUr else event.lessonsEn,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(6.dp))

            // Source Ref Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${getLocalString("source_ref", lang)}: ${event.source}",
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Text(
                    text = if (event.period == "Makkah") "Makkah Period 🕋" else "Madinah Period 🕌",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (event.period == "Makkah") BrandViolet.copy(alpha = 0.8f) else BrandGreen.copy(alpha = 0.8f)
                )
            }

            // Expanded Reflection Note Input Area
            if (expandedNoteInput) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Write your reflections, notes, or lessons here...", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            onSaveNote(noteText)
                            expandedNoteInput = false
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = BrandGreen)
                        }
                    }
                )
            }

            // Display current saved note if any
            if (savedNote.isNotBlank() && !expandedNoteInput) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandAmber.copy(alpha = 0.06f)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BrandAmber.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = BrandAmber, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("My Reflections:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandAmber)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = savedNote, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                    }
                }
            }
        }
    }
}

// --- Call Gemini Seerah Scholar REST API ---
suspend fun queryGeminiSeerahScholar(prompt: String, lang: SeerahLanguage): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isBlank()) {
        return@withContext "Error: Gemini API Key is missing. Please set GEMINI_API_KEY in the Secrets panel."
    }

    val modelName = "gemini-3.5-flash"
    val systemInstructionText = """
        You are an expert Islamic history and Seerah scholar specializing in the noble life and times of Prophet Muhammad ﷺ.
        Your source references must strictly adhere to the most authentic sources, including:
        - The Sealed Nectar (Ar-Raheeq Al-Makhtum)
        - Ibn Hisham Seerah
        - Sahih Bukhari
        - Sahih Muslim
        - Riyad-us-Saliheen
        - Ibn Kathir
        
        Answer the user's questions clearly, respectfully, and beautifully. Add peace be upon him (ﷺ) after mentioning his name.
        Provide your answers in the requested language (which is ${lang.label}). If the user asks in another language, answer in that language but keep the tone highly scholastic and spiritually serene.
        Provide authentic book and chapter source references whenever quoting an event, battle, or Hadith.
    """.trimIndent()

    try {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        // Build Payload
        val payload = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", prompt)
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        payload.put("contents", contentsArray)

        // System instructions
        val systemInstructionObj = JSONObject()
        val systemPartsArray = JSONArray()
        val systemPartObj = JSONObject()
        systemPartObj.put("text", systemInstructionText)
        systemPartsArray.put(systemPartObj)
        systemInstructionObj.put("parts", systemPartsArray)
        payload.put("systemInstruction", systemInstructionObj)

        // Generation Config
        val genConfig = JSONObject()
        genConfig.put("temperature", 0.6)
        payload.put("generationConfig", genConfig)

        OutputStreamWriter(conn.outputStream).use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        if (conn.responseCode == 200) {
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val responseJson = JSONObject(responseText)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                if (content != null) {
                    val parts = content.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No response text found.")
                    }
                }
            }
            "No answer returned. Please try asking again."
        } else {
            val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown HTTP error"
            "Error querying Scholar AI: $errorResponse"
        }
    } catch (e: Exception) {
        "Failed to connect to Seerah Scholar AI. Connection error: ${e.localizedMessage}. Please ensure internet connectivity is enabled."
    }
}

// --- Multi-Language Localization helper ---
fun getLocalString(key: String, lang: SeerahLanguage): String {
    val mappings = mapOf(
        "tab_timeline" to mapOf("en" to "Timeline", "ar" to "الخط الزمني", "ur" to "تاریخی خاکہ"),
        "tab_months" to mapOf("en" to "Hijri Months", "ar" to "الأشهر الهجرية", "ur" to "اسلامی مہینے"),
        "tab_daily" to mapOf("en" to "Daily Routine", "ar" to "اليوم النبوي", "ur" to "روزمرہ معمولات"),
        "tab_battles" to mapOf("en" to "Battles", "ar" to "الغزوات", "ur" to "غزوات"),
        "tab_family" to mapOf("en" to "Family Tree", "ar" to "شجرة العائلة", "ur" to "شجرہ نسب"),
        "tab_map" to mapOf("en" to "Seerah Map", "ar" to "خرائط السيرة", "ur" to "نقشہ سیرت"),
        "tab_ai" to mapOf("en" to "Scholar AI", "ar" to "المساعد الذكي", "ur" to "سکالر اے آئی"),
        "tab_bookmarks" to mapOf("en" to "Bookmarks", "ar" to "المحفوظات", "ur" to "محفوظ شدہ"),
        "search_hint" to mapOf("en" to "Search Seerah events, years, milestones...", "ar" to "ابحث في أحداث السيرة، السنوات...", "ur" to "سیرت کے واقعات تلاش کریں..."),
        "no_results" to mapOf("en" to "No Seerah records found for your query.", "ar" to "لم يتم العثور على أي نتائج.", "ur" to "کوئی نتیجہ نہیں ملا۔"),
        "source_ref" to mapOf("en" to "Source", "ar" to "المصدر", "ur" to "حوالہ"),
        "lessons_learnt" to mapOf("en" to "Lessons & Reflections", "ar" to "الدروس والعبر المستفادة", "ur" to "اسباق و عبرت"),
        "stats_title" to mapOf("en" to "Forces & Outcomes", "ar" to "القوات والنتائج", "ur" to "قوت اور نتائج"),
        "interactive_map_title" to mapOf("en" to "Geographical Landmarks of the Seerah", "ar" to "المعالم الجغرافية للسيرة النبوية", "ur" to "سیرت طیبہ کے جغرافیائی مقامات"),
        "interactive_map_hint" to mapOf("en" to "Tap on a marker to explore historical highlights.", "ar" to "اضغط على أي علامة لمعرفة التفاصيل التاريخية.", "ur" to "مقامات کی معلومات کے لیے مارکر پر کلک کریں۔"),
        "today_in_seerah" to mapOf("en" to "TODAY IN SEERAH HIGHLIGHT 🌟", "ar" to "حدث في مثل هذا اليوم من السيرة 🌟", "ur" to "آج کی سیرت کا اہم واقعہ 🌟"),
        "no_bookmarks" to mapOf("en" to "No bookmarks saved yet. Tap the bookmark icon on any event card to save it here offline.", "ar" to "لا توجد محفوظات بعد. اضغط علامة الحفظ في أي بطاقة.", "ur" to "کوئی چیز محفوظ نہیں۔ کسی واقعے کو محفوظ کرنے کے لیے بک مارک دبائیں۔"),
        "ai_title" to mapOf("en" to "Ask Prophet's Life Scholar AI Assistant", "ar" to "اسأل المساعد الذكي عن السيرة النبوية", "ur" to "سیرت طیبہ کے بارے میں سوال پوچھیں"),
        "ai_subtitle" to mapOf("en" to "Powered by Gemini Pro to provide authentic scholarly answers.", "ar" to "مدعوم بنظام جميناي للإجابة الموثوقة", "ur" to "مستند جوابات کے لیے جیمنائ کا استعمال کریں"),
        "ai_greeting" to mapOf(
            "en" to "Assalamu Alaikum! I am your Seerah Scholar Assistant. Ask me anything about the beautiful life, noble character, battles, companion relationships, and passing of Prophet Muhammad ﷺ.",
            "ar" to "السلام عليكم ورحمة الله! أنا مساعد السيرة النبوية. اسألني أي شيء عن حياة رسول الله ﷺ وغزواته وشمائله.",
            "ur" to "السلام علیکم! میں سیرت طیبہ کا سکالر اسسٹنٹ ہوں۔ آپ مجھ سے حضرت محمد مصطفیٰ ﷺ کی زندگی، غزوات اور اخلاق کے بارے میں کچھ بھی پوچھ سکتے ہیں۔"
        ),
        "ai_you" to mapOf("en" to "You", "ar" to "أنت", "ur" to "آپ"),
        "ai_thinking" to mapOf("en" to "Consulting Authentic Sources...", "ar" to "جاري مراجعة المصادر الموثوقة...", "ur" to "مستند ذرائع سے مراجعہ جاری ہے..."),
        "related_quran" to mapOf("en" to "Related Quran Verses", "ar" to "آيات قرآنية متعلقة", "ur" to "متعلقہ قرآنی آیات"),
        "related_hadith" to mapOf("en" to "Related Authentic Hadiths", "ar" to "أحاديث شريفة متعلقة", "ur" to "متعلقہ احادیث مبارکہ"),
        "daily_introduction_title" to mapOf("en" to "The Daily Lifestyle of Allah's Messenger ﷺ", "ar" to "الهدي النبوي في الحياة اليومية", "ur" to "رسول اللہ ﷺ کے روزمرہ معمولات"),
        "daily_introduction_desc" to mapOf(
            "en" to "The daily life of the Prophet ﷺ was the perfect model of spiritual devotion, kindness, organization, and constant connection with Allah. From morning prayers to late-night supplications, every moment was beautifully structured.",
            "ar" to "كانت حياة النبي ﷺ اليومية نموذجاً كاملاً للعبادة الروحية والرفق والنظام والارتباط الدائم بالله تعالى في كل حركاته وسكناته.",
            "ur" to "رسول اللہ ﷺ کی روزمرہ کی زندگی عبادت، شفقت، نظم و ضبط اور اللہ سے مسلسل تعلق کا بہترین نمونہ تھی۔ صبح کی نماز سے لے کر رات کے آخری حصے تک ہر لمحہ خوبصورت تھا۔"
        ),
        "family_tree" to mapOf("en" to "Family & Household", "ar" to "آل البيت الأطهار", "ur" to "اہل بیت اور خاندان"),
        "companions" to mapOf("en" to "Noble Companions", "ar" to "الصحابة الكرام", "ur" to "صحابہ کرام علیہم الرضوان"),
        "filter_all" to mapOf("en" to "All Periods", "ar" to "كل الفترات", "ur" to "تمام ادوار"),
        "period_makkah" to mapOf("en" to "Makkah Period", "ar" to "العهد المكي", "ur" to "مکی دور"),
        "period_madinah" to mapOf("en" to "Madinah Period", "ar" to "العهد المدني", "ur" to "مدنی دور"),
        "no_month_events" to mapOf(
            "en" to "No major battles or calendar milestones recorded exactly in this month. Explore the main Timeline to see all year-by-year events.",
            "ar" to "لا توجد أحداث مفصلية مسجلة في هذا الشهر الهجري. تصفح الخط الزمني العام.",
            "ur" to "اس ہجری مہینے میں کوئی اہم واقعہ درج نہیں۔ مکمل تاریخ دیکھنے کے لیے ٹائم لائن دیکھیں۔"
        ),
        "monthly_view_title" to mapOf("en" to "Explore Milestones by Hijri Calendar Month", "ar" to "استكشف الأحداث حسب الأشهر الهجرية", "ur" to "ہجری مہینوں کے لحاظ سے واقعات دیکھیں")
    )

    val code = lang.code
    return mappings[key]?.get(code) ?: key
}

fun getHijriMonthName(month: Int, lang: SeerahLanguage): String {
    val monthsEn = listOf("Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani", "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah")
    val monthsAr = listOf("محرم", "صفر", "ربيع الأول", "ربيع الثاني", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
    val monthsUr = listOf("محرم الحرام", "صفر المظفر", "ربيع الاول", "ربيع الثاني", "جمادى الاولى", "جمادى الثانية", "رجب المرجب", "شعبان المعظم", "رمضان المبارک", "شوال المکرم", "ذو القعدہ", "ذو الحجہ")

    return when (lang) {
        SeerahLanguage.ARABIC -> monthsAr.getOrNull(month - 1) ?: ""
        SeerahLanguage.URDU -> monthsUr.getOrNull(month - 1) ?: ""
        else -> monthsEn.getOrNull(month - 1) ?: ""
    }
}

// --- Data Loading Implementation ---

fun getSeerahEvents(): List<SeerahEvent> {
    return listOf(
        SeerahEvent(
            id = "birth",
            yearG = 571,
            yearH = "53 BH",
            period = "Makkah",
            titleEn = "The Blessed Birth of Prophet Muhammad ﷺ",
            titleAr = "المولد النبوي الشريف",
            titleUr = "ولادتِ با سعادت رسول اللہ ﷺ",
            descEn = "Born in Makkah in the Year of the Elephant on Monday, 12th Rabi' al-Awwal. His father Abdullah died before his birth, and his mother Aminah passed away when he was six.",
            descAr = "ولد النبي ﷺ في مكة المكرمة في عام الفيل يوم الاثنين الثاني عشر من ربيع الأول. توفي والده عبد الله قبل ولادته، وتوفيت والدته آمنة وعمره ست سنوات.",
            descUr = "عام الفیل میں پیر کے روز مکہ معظمہ میں ۱۲ ربیع الاول کو ولادت ہوئی۔ والد عبداللہ ولادت سے قبل انتقال کر گئے اور والدہ آمنہ کا انتقال آپ ﷺ کے ۶ سال کی عمر میں ہوا۔",
            locationEn = "Makkah", locationAr = "مكة المكرمة", locationUr = "مکہ مکرمہ",
            companions = listOf("Aminah (Mother)", "Halimah al-Sa'diyah (Foster mother)", "Abdul Muttalib (Grandfather)"),
            verses = listOf("Surah Al-Fil (105:1-5)"),
            hadiths = listOf("Sahih Muslim: Monday fasting is recommended because 'it was the day I was born'"),
            lessonsEn = "Shows how Allah protects His chosen messengers even when born as orphans under challenging circumstances.",
            lessonsAr = "بيان حفظ الله ورعايته لأنبيائه منذ الصغر حتى وهم أيتام في ظروف صعبة.",
            lessonsUr = "اس سے یہ سبق ملتا ہے کہ کس طرح اللہ اپنے برگزیدہ رسولوں کی حفاظت فرماتا ہے چاہے وہ یتیم پیدا ہوں۔",
            source = "Ar-Raheeq Al-Makhtum, Ibn Hisham",
            hijriMonth = 3
        ),
        SeerahEvent(
            id = "kabah_rebuild",
            yearG = 605,
            yearH = "35 BH",
            period = "Makkah",
            titleEn = "Rebuilding the Ka'bah & Settling Dispute",
            titleAr = "إعادة بناء الكعبة وحل النزاع",
            titleUr = "کعبہ کی تعمیرِ نو اور تنازع کا حل",
            descEn = "The Quraysh rebuilt the Ka'bah but disputed who should place the Black Stone (Hajar al-Aswad). They agreed on letting the next entrant decide. The Prophet ﷺ entered next, placed it on a cloak, and asked all clan leaders to lift it together, averting war.",
            descAr = "أعادت قريش بناء الكعبة واختلفت فيمن يضع الحجر الأسود. واتفقوا على تحكيم أول داخل، فكان النبي ﷺ. فوضعه في رداء وطلب من رؤساء القبائل رفعه معاً.",
            descUr = "قریش نے کعبہ کی تعمیرِ نو کی لیکن حجرِ اسود رکھنے پر جھگڑا ہو گیا۔ انہوں نے فیصلہ کیا کہ اگلا داخل ہونے والا شخص فیصلہ کرے گا۔ آپ ﷺ داخل ہوئے، چادر پر پتھر رکھا اور تمام قبیلوں کے سربراہوں کو مل کر اٹھانے کا کہا۔",
            locationEn = "Ka'bah, Makkah", locationAr = "الكعبة، مكة المكرمة", locationUr = "کعبہ، مکہ مکرمہ",
            companions = listOf("Quraysh tribal leaders"),
            verses = listOf("Surah Al-Baqarah (2:127)"),
            hadiths = listOf("Ibn Hisham Seerah reference on Hajar al-Aswad setting"),
            lessonsEn = "Illustrates the supreme wisdom, peaceful conflict resolution, and unmatched trust the Prophet ﷺ commanded even before prophecy.",
            lessonsAr = "بيان الحكمة النبوية العظيمة والقدرة على حل النزاعات وحقن الدماء قبل البعثة.",
            lessonsUr = "آپ ﷺ کی بے مثال حکمت، دور اندیشی اور امن پسندانہ رویے کی عکاسی ہوتی ہے۔",
            source = "The Sealed Nectar",
            hijriMonth = 1
        ),
        SeerahEvent(
            id = "revelation",
            yearG = 610,
            yearH = "13 BH",
            period = "Makkah",
            titleEn = "The First Divine Revelation in Cave Hira",
            titleAr = "نزول الوحي في غار حراء",
            titleUr = "غارِ حرا میں پہلی وحی کا نزول",
            descEn = "At age 40, while meditating in Cave Hira, Angel Jibril (Gabriel) appeared and commanded him: 'Read!' (Iqra). This marked the dawn of Islam. Khadijah (RA) comforted him and took him to Waraqah bin Nawfal.",
            descAr = "في سن الأربعين، وبينما كان يتعبد في غار حراء، نزل عليه الملك جبريل عليه السلام قائلاً: (اقرأ). وكانت بداية النبوة. فزع النبي فهدأته زوجه خديجة وذهبت به لورقة بن نوفل.",
            descUr = "۴۰ سال کی عمر میں غارِ حرا میں عبادت کے دوران فرشتہ جبرائیل علیہ السلام نے آ کر کہا: 'پڑھو!'۔ یہ نبوت کا آغاز تھا۔ حضرت خدیجہ رضی اللہ عنہا نے آپ ﷺ کو تسلی دی اور ورقہ بن نوفل کے پاس لے گئیں۔",
            locationEn = "Cave Hira, Mount Noor", locationAr = "غار حراء، جبل النور", locationUr = "غارِ حرا، جبلِ نور",
            companions = listOf("Angel Jibril", "Khadijah (Wife)", "Waraqah bin Nawfal"),
            verses = listOf("Surah Al-Alaq (96:1-5)"),
            hadiths = listOf("Sahih Bukhari, Book 1, Hadith 3 (How the Divine Revelation began)"),
            lessonsEn = "Highlights the importance of seeking knowledge and the critical supportive role of a pious spouse during trying moments.",
            lessonsAr = "بيان أهمية العلم والقراءة ودور الزوجة الصالحة في تثبيت زوجها وقت الشدائد.",
            lessonsUr = "علم کی اہمیت اور کٹھن حالات میں نیک شریک حیات کے اہم کردار کو اجاگر کرتا ہے۔",
            source = "Sahih Bukhari, Ibn Kathir",
            hijriMonth = 9
        ),
        SeerahEvent(
            id = "abyssinia",
            yearG = 615,
            yearH = "8 BH",
            period = "Makkah",
            titleEn = "The Hijrah (Migration) to Abyssinia",
            titleAr = "الهجرة الأولى إلى الحبشة",
            titleUr = "حبشہ کی طرف ہجرتِ اولیٰ",
            descEn = "Due to intense persecution of early converts in Makkah, the Prophet ﷺ advised companions to migrate to Abyssinia (Ethiopia), ruled by Najashi, a just Christian king. Ja'far ibn Abi Talib recited Surah Maryam in Najashi's court.",
            descAr = "بسبب الاضطهاد الشديد للمسلمين الأوائل في مكة، أمرهم النبي ﷺ بالهجرة إلى الحبشة حيث ملكها النجاشي عادل لا يظلم عنده أحد. وتلا جعفر بن أبي طالب سورة مريم أمامه.",
            descUr = "مکہ میں ظلم و ستم سے تنگ آ کر آپ ﷺ نے صحابہ کو حبشہ ہجرت کی ہدایت کی جہاں کا بادشاہ نجاشی عادل تھا۔ جعفر بن ابی طالب نے نجاشی کے دربار میں سورہ مریم کی تلاوت کی۔",
            locationEn = "Abyssinia (Ethiopia)", locationAr = "الحبشة (إثيوبيا)", locationUr = "حبشہ (ایتھوپیا)",
            companions = listOf("Ja'far ibn Abi Talib", "Uthman ibn Affan", "Ruqayyah (Daughter)"),
            verses = listOf("Surah An-Nahl (16:110)"),
            hadiths = listOf("Sahih Muslim reference on migration and Najashi's hospitality"),
            lessonsEn = "Shows that justice is a universal value and Muslims may seek alliance and safety with righteous non-Muslims when oppressed.",
            lessonsAr = "بيان أن العدل قيمة عالمية والترخيص في اللجوء للدول العادلة لحفظ النفوس والعقيدة.",
            lessonsUr = "ثابت کرتا ہے کہ انصاف ایک عالمگیر قدر ہے اور مظلومیت میں عادل غیر مسلموں سے پناہ لی جا سکتی ہے۔",
            source = "Ibn Hisham Seerah",
            hijriMonth = 7
        ),
        SeerahEvent(
            id = "sorrow",
            yearG = 619,
            yearH = "3 BH",
            period = "Makkah",
            titleEn = "The Year of Sorrow ('Aam al-Huzn)",
            titleAr = "عام الحزن",
            titleUr = "عام الحزن (غم کا سال)",
            descEn = "The Prophet ﷺ lost both his beloved wife Khadijah (RA), who was his greatest support, and his protector uncle Abu Talib within a short period, leaving him vulnerable to Quraysh aggression.",
            descAr = "فقد النبي ﷺ في هذا العام زوجته الحبيبة خديجة رضي الله عنها التي كانت أكبر داعم له، وعمه أبو طالب الذي كان يحميه من أذى قريش.",
            descUr = "اس سال آپ ﷺ کی پیاری زوجہ حضرت خدیجہ اور چچا ابو طالب کا قریبی عرصے میں انتقال ہوا، جس سے مکہ میں آپ ﷺ کی ظاہری حمایت ختم ہو گئی۔",
            locationEn = "Makkah", locationAr = "مكة المكرمة", locationUr = "مکہ مکرمہ",
            companions = listOf("Khadijah", "Abu Talib"),
            verses = listOf("Surah Ad-Duha (93:3-8)"),
            hadiths = listOf("Sahih Bukhari: Condolences and praise of Khadijah's matchless virtues"),
            lessonsEn = "Teaches that trial and emotional grief are natural human experiences that even prophets endure, requiring patient perseverance (Sabr).",
            lessonsAr = "الابتلاء جزء من بشريّة الأنبياء والدروس المستفادة في الصبر والتوكل التام على الله عند فقد الأسباب الأرضية.",
            lessonsUr = "صبر اور توکل کا درس ملتا ہے کہ غم اور آزمائش انسانی زندگی کا حصہ ہیں جن پر انبیاء بھی صبر فرماتے ہیں۔",
            source = "Ar-Raheeq Al-Makhtum",
            hijriMonth = 10
        ),
        SeerahEvent(
            id = "miraj",
            yearG = 620,
            yearH = "2 BH",
            period = "Makkah",
            titleEn = "The Night Journey & Ascension (Isra' wal-Mi'raj)",
            titleAr = "الإسراء والمعراج",
            titleUr = "واقعہ معراج و اسراء",
            descEn = "A miraculous overnight journey where Prophet Muhammad ﷺ traveled from Makkah to Jerusalem (Masjid al-Aqsa) and ascended to the Heavens. The five daily prayers (Salah) were gifted to the Ummah.",
            descAr = "رحلة إعجازية بالليل أسري بالنبي ﷺ من مكة إلى القدس (المسجد الأقصى) ثم عرج به إلى السموات العلى، وفرضت الصلاة الخمس كهدية للأمة.",
            descUr = "ایک معجزاتی رات کا سفر جس میں آپ ﷺ مکہ سے بیت المقدس تشریف لے گئے اور وہاں سے آسمانوں کی طرف معراج ہوئی۔ اسی سفر میں امت کو ۵ وقت کی نماز کا تحفہ ملا۔",
            locationEn = "Masjid al-Haram to Al-Aqsa & Heavens", locationAr = "المسجد الحرام إلى الأقصى والسموات", locationUr = "مسجد الحرام سے مسجدِ اقصی اور آسمان",
            companions = listOf("Angel Jibril", "Abu Bakr al-Siddiq (confirmed it immediately)"),
            verses = listOf("Surah Al-Isra (17:1)", "Surah An-Najm (53:13-18)"),
            hadiths = listOf("Sahih Bukhari, Book 54, Hadith 429 (Details of Mi'raj & gift of Salah)"),
            lessonsEn = " Salah is a believer's direct spiritual ascension to communicate with the Creator, established in the high heavens.",
            lessonsAr = "بيان منزلة الصلاة العظيمة حيث فرضت في السماء السابعة وهي صلة العبد بربه.",
            lessonsUr = "نماز مومن کی معراج اور اللہ سے گفتگو کا ذریعہ ہے جسے آسمانوں پر فرض کیا گیا۔",
            source = "Sahih Bukhari, Sahih Muslim",
            hijriMonth = 7
        ),
        SeerahEvent(
            id = "migration_madinah",
            yearG = 622,
            yearH = "1 AH",
            period = "Madinah",
            titleEn = "The Hijrah (Migration) to Madinah",
            titleAr = "الهجرة النبوية إلى المدينة",
            titleUr = "ہجرتِ مدینہ منورہ",
            descEn = "Fleeing assassination attempts, the Prophet ﷺ and Abu Bakr migrated to Yathrib (renamed Madinah). They stayed in Cave Thawr. They built the Prophet's Mosque (Al-Masjid an-Nabawi) upon arrival.",
            descAr = "هرباً من محاولات الاغتيال، هاجر النبي ﷺ وأبو بكر الصديق إلى يثرب (المدينة المنورة). واختبآ في غار ثور، وأسسا المسجد النبوي فور وصولهما.",
            descUr = "قتل کی سازشوں سے بچتے ہوئے آپ ﷺ اور ابوبکر رضی اللہ عنہ یثرب (مدینہ) ہجرت فرما گئے۔ غارِ ثور میں قیام کیا۔ پہنچنے پر مسجدِ نبوی کی بنیاد رکھی۔",
            locationEn = "Cave Thawr to Quba & Madinah", locationAr = "غار ثور إلى قباء والمدينة المنورة", locationUr = "غارِ ثور سے قبا اور مدینہ منورہ",
            companions = listOf("Abu Bakr al-Siddiq", "Ali ibn Abi Talib (slept in his bed)", "Asma bint Abi Bakr"),
            verses = listOf("Surah At-Tawbah (9:40 - 'He was the second of the two in the cave')"),
            hadiths = listOf("Sahih Bukhari: Suraqa's pursuit and the Quba mosque establishment"),
            lessonsEn = "Migration marks the importance of planning, execution, and divine trust (Tawakkul). It formed the foundation of the Islamic State.",
            lessonsAr = "أهمية التخطيط واتخاذ الأسباب والتوكل المطلق، وتأسيس الدولة الإسلامية على مبدأ الأخوة.",
            lessonsUr = "اس سے منصوبہ بندی، تدبیر اور اللہ پر کامل توکل کا درس ملتا ہے۔ اس سے اسلامی سال کے آغاز کی شروعات ہوئی۔",
            source = "The Sealed Nectar, Ibn Hisham",
            hijriMonth = 3
        ),
        SeerahEvent(
            id = "conquest_makkah",
            yearG = 630,
            yearH = "8 AH",
            period = "Madinah",
            titleEn = "The Peaceful Conquest of Makkah",
            titleAr = "فتح مكة العظيم",
            titleUr = "فتحِ مکہ مکرمہ",
            descEn = "The Quraysh violated the Treaty of Hudaybiyyah. The Prophet ﷺ marched with 10,000 companions. He entered Makkah with complete humility, pardoned his former persecutors saying 'Go, you are free', and cleared Ka'bah of idols.",
            descAr = "نقضت قريش صلح الحديبية، فسار النبي ﷺ بـ 10,000 مقاتل ودخل مكة متواضعاً مطأطئ الرأس، وأعلن العفو العام قائلاً (اذهبوا فأنتم الطلقاء) وطهّر الكعبة من الأصنام.",
            descUr = "قریش نے معاہدہ توڑ دیا تو آپ ﷺ ۱۰ ہزار صحابہ کے ساتھ نکلے۔ انتہائی عاجزی کے ساتھ مکہ میں داخل ہوئے، دشمنوں کو معاف فرما دیا اور کعبہ کو بتوں سے پاک کیا۔",
            locationEn = "Makkah", locationAr = "مكة المكرمة", locationUr = "مکہ مکرمہ",
            companions = listOf("Abu Sufyan (accepted Islam)", "Bilal ibn Rabah (called Adhan on Ka'bah)", "Sa'd ibn Ubadah"),
            verses = listOf("Surah Al-Isra (17:81 - 'Truth has come, falsehood has vanished')"),
            hadiths = listOf("Sahih Bukhari, Book of Expeditions (Conquest of Makkah chapter)"),
            lessonsEn = "The ultimate lesson in mercy, forgiveness, and humility during victory. He did not seek revenge against his brutal torturers.",
            lessonsAr = "أعظم درس في الرحمة والتسامح والتواضع عند النصر وعدم الانتقام.",
            lessonsUr = "فتح اور طاقت پانے کے بعد عفو و درگزر اور عاجزی کی سب سے بڑی مثال۔ آپ ﷺ نے انتقام نہیں لیا۔",
            source = "Sahih Bukhari, Riyad-us-Saliheen",
            hijriMonth = 9
        ),
        SeerahEvent(
            id = "passing",
            yearG = 632,
            yearH = "11 AH",
            period = "Madinah",
            titleEn = "The Passing of the Beloved Prophet ﷺ",
            titleAr = "وفاة النبي ﷺ والرفيق الأعلى",
            titleUr = "وصالِ مبارک رسول اللہ ﷺ",
            descEn = "After delivering his Farewell Sermon, the Prophet ﷺ fell ill in Madinah. On 12th Rabi' al-Awwal, 11 AH, he passed away in the arms of Aisha (RA), whispering: 'O Allah, to the Highest Companion (Ar-Rafiq al-A'la)'.",
            descAr = "بعد أداء حجة الوداع، مرض النبي ﷺ في المدينة المنورة. وفي 12 ربيع الأول سنة 11 هـ، انتقل إلى الرفيق الأعلى في حجرة عائشة رضي الله عنها متمتماً (اللهم الرفيق الأعلى).",
            descUr = "حجۃ الوداع کے بعد مدینہ منورہ میں آپ ﷺ علیل ہو گئے۔ ۱۲ ربیع الاول ۱۱ ہجری کو حضرت عائشہ کی گود میں 'الرفیق الاعلیٰ' پکارتے ہوئے انتقال فرما گئے۔",
            locationEn = "Aisha's Chamber, Madinah", locationAr = "حجرة عائشة، المدينة المنورة", locationUr = "حجرہ حضرت عائشہ، مدینہ منورہ",
            companions = listOf("Aisha bint Abi Bakr (Wife)", "Abu Bakr al-Siddiq (delivered comforting address)", "Fatimah (Daughter)"),
            verses = listOf("Surah Al-Imran (3:144 - 'Muhammad is no more than a messenger')"),
            hadiths = listOf("Sahih Bukhari: The tragic day of the passing and Abu Bakr's speech"),
            lessonsEn = "Reminds us that the Prophet's physical presence is gone but his eternal message, Sunnah, and character remain to guide mankind.",
            lessonsAr = "بيان انقطاع الوحي بوفاته وبقاء الشريعة والسنة منهجاً للأمة إلى قيام الساعة.",
            lessonsUr = "ہمیں یاد دلاتا ہے کہ رسول اللہ ﷺ کا جسمانی سایہ رخصت ہو گیا لیکن آپ کا ابدی پیغام اور سنت ہمیشہ قائم ہیں۔",
            source = "Sahih Bukhari, Sahih Muslim",
            hijriMonth = 3
        )
    )
}

fun getDailyRoutines(): List<DailyRoutineItem> {
    return listOf(
        DailyRoutineItem(
            id = "routine_1",
            timeEn = "Before Dawn & Morning",
            timeAr = "قبل الفجر والصباح",
            timeUr = "قبل از فجر اور صبح",
            titleEn = "Tahajjud, Fajr & Remembrance",
            titleAr = "التهجد والفجر والأذكار",
            titleUr = "تہجد، نمازِ فجر اور ذکر و اذکار",
            descEn = "The Prophet ﷺ would wake up in the last third of the night, use the Miswak, recite the last verses of Surah Al-Imran, perform Wudu, and pray Tahajjud. After Fajr, he sat in Masjid until sunrise, engaging in remembrance and talking with companions.",
            descAr = "كان يستيقظ في الثلث الأخير، يتسوك، ويتلو خواتيم آل عمران، يتوضأ ويصلي التهجد. بعد الفجر يجلس في مصلاه يذكر الله حتى تطلع الشمس.",
            descUr = "آپ ﷺ رات کے آخری حصے میں بیدار ہوتے، مسواک فرماتے، سورہ آل عمران کی آخری آیات تلاوت کرتے اور تہجد ادا فرماتے۔ فجر کے بعد سورج طلوع ہونے تک مسجد میں بیٹھ کر ذکر فرماتے اور صحابہ سے گفتگو فرماتے۔",
            source = "Sahih Bukhari, Riyad-us-Saliheen"
        ),
        DailyRoutineItem(
            id = "routine_2",
            timeEn = "Midday & Public Affairs",
            timeAr = "الظهيرة وشؤون الأمة",
            timeUr = "دوپہر اور عوامی معاملات",
            titleEn = "Duha Prayer, Community Service & Consultations",
            titleAr = "صلاة الضحى والشورى وخدمة المجتمع",
            titleUr = "نمازِ چاشت، مشاورت اور سماجی خدمات",
            descEn = "He would pray Duha (Chime/Ishraq), check on the markets, visit the sick, resolve disputes, feed the poor, and consult companions on community matters. He also took a brief midday nap (Qailulah).",
            descAr = "كان يصلي الضحى، يتفقد الأسواق، يزور المرضى، يحل النزاعات، يطعم الفقراء، ويستشير صحابته، ثم يقيل قيلولة خفيفة.",
            descUr = "آپ ﷺ چاشت کی نماز پڑھتے، بازار کا چکر لگاتے، مریضوں کی عیادت فرماتے، جھگڑے حل کراتے اور غریبوں کی مدد فرماتے۔ دوپہر کو تھوڑی دیر قیلولہ فرماتے۔",
            source = "Sahih Muslim"
        ),
        DailyRoutineItem(
            id = "routine_3",
            timeEn = "Evening & Family Time",
            timeAr = "المساء والبيت النبوي",
            timeUr = "شام اور خاندانی وقت",
            titleEn = "Helping in Household Chores & Family Counsel",
            titleAr = "خدمة أهل البيت ومؤانستهم",
            titleUr = "گھریلو کاموں میں مدد اور اہل خانہ سے گفتگو",
            descEn = "Aisha (RA) narrated that he ﷺ was constantly in the service of his family (sewing, repairing sandals, milking sheep). In the evening, he spent time with his wives, counseling them and asking about their days.",
            descAr = "قالت عائشة رضي الله عنها: كان يكون في مهنة أهله (يخصف نعليه، يرقع ثوبه). وفي المساء يجلس مع نسائه ويؤانسهم ويسمع منهم.",
            descUr = "حضرت عائشہ رضی اللہ عنہا فرماتی ہیں کہ آپ ﷺ اپنے گھر والوں کی مدد فرماتے (جوتے گانٹھتے، کپڑے سیتے اور بکری کا دودھ نکالتے)۔ شام کو اہل خانہ کے پاس بیٹھتے۔",
            source = "Sahih Bukhari (Hadith: 'He was in the service of his family')"
        )
    )
}

fun getBattles(): List<BattleItem> {
    return listOf(
        BattleItem(
            id = "battle_badr",
            nameEn = "The Battle of Badr",
            nameAr = "غزوة بدر الكبرى",
            nameUr = "غزوہِ بدر",
            yearH = "2 AH",
            descEn = "The first major encounter between the young Muslim community and the pagan Quraysh of Makkah. Despite being outnumbered 3:1 (313 Muslims against 1,000 Meccans), Allah granted the Muslims a decisive, miraculous victory.",
            descAr = "أول مواجهة كبرى بين المسلمين الفتيان ومشركي قريش. رغم قلة العدد (313 مسلماً مقابل 1,000 مشرك)، نصر الله عباده نصراً مؤزراً.",
            descUr = "مسلمانوں اور قریش کے درمیان پہلی بڑی جنگ۔ مسلمانوں کی تعداد ۳۱۳ اور کفار کی ۱۰۰۰ تھی۔ اللہ نے مسلمانوں کو شاندار غیبی فتح عطا فرمائی۔",
            statsEn = "Muslims: 313 (14 Martyrs) | Opponents: 1,000 (70 Dead, 70 Captured)",
            statsAr = "المسلمون: 313 (14 شهيداً) | المشركون: 1000 (70 قتيلاً، 70 أسيراً)",
            statsUr = "مسلمان: ۳۱۳ (۱۴ شہدا) | کفار: ۱۰۰۰ (۷۰ مقتول، ۷۰ قیدی)",
            lessonEn = "Victory does not come from numbers or equipment, but from firm faith (Iman), unity, and divine aid.",
            lessonAr = "النصر ليس بكثرة العدد والعدة، وإنما بقوة الإيمان والتوكل التام والمدد الإلهي.",
            lessonUr = "کامیابی مادی اسباب سے نہیں بلکہ پختہ ایمان، اتحاد اور غیبی تائید سے ملتی ہے۔",
            source = "Sahih Bukhari, Ibn Kathir (Surah Al-Anfal)"
        ),
        BattleItem(
            id = "battle_uhud",
            nameEn = "The Battle of Uhud",
            nameAr = "غزوة أحد",
            nameUr = "غزوہِ احد",
            yearH = "3 AH",
            descEn = "Fought to avenge Meccan losses in Badr. The Meccans fields 3000 fighters. The Prophet ﷺ placed archers on Mount Rumat with strict orders not to leave. When some archers left premature to collect spoils, Khalid bin Walid launched a surprise flank attack, resulting in heavy Muslim losses and the martyrdom of Hamzah (RA).",
            descAr = "وقعت انتقاماً ليوم بدر. وضع النبي رماة فوق جبل الرماة وأمرهم ألا يبرحوا. لكن بعضهم تسرع لجمع الغنائم فالتف حولهم خالد بن الوليد وحصلت خسائر كبيرة واستشهد حمزة رضي الله عنه.",
            descUr = "غزوہِ بدر کا انتقام لینے کے لیے قریش ۳ ہزار کا لشکر لائے۔ آپ ﷺ نے تیر اندازوں کو پہاڑی پر متعین کیا اور نہ ہٹنے کا حکم دیا۔ بعض تیر اندازوں کے ہٹ جانے سے خالد بن ولید نے حملہ کیا اور بہت نقصان پہنچا۔ حضرت حمزہ شہید ہوئے۔",
            statsEn = "Muslims: 700 (70 Martyrs) | Opponents: 3,000 (22 Dead)",
            statsAr = "المسلمون: 700 (70 شهيداً) | المشركون: 3000 (22 قتيلاً)",
            statsUr = "مسلمان: ۷۰۰ (۷۰ شہدا) | کفار: ۳۰۰۰ (۲۲ مقتول)",
            lessonEn = "Disobedience of leadership and chasing worldly material spoils leads to tactical failures and division.",
            lessonAr = "عاقبة مخالفة أوامر القيادة والجري وراء حطام الدنيا تؤدي إلى الفشل والابتلاء.",
            lessonUr = "امیر کی نافرمانی اور دنیا کی مادی غنائم کے پیچھے بھاگنا ہمیشہ نقصان اور ناکامی کا باعث بنتا ہے۔",
            source = "Sahih Bukhari, Al-Imran (Verses 121-165)"
        ),
        BattleItem(
            id = "battle_khandaq",
            nameEn = "The Battle of the Trench (Ahzab)",
            nameAr = "غزوة الخندق (الأحزاب)",
            nameUr = "غزوہِ خندق (احزاب)",
            yearH = "5 AH",
            descEn = "An allied confederacy of 10,000 Meccans and tribal allies besieged Madinah. Following Salman al-Farsi's (RA) advice, the Muslims dug a deep trench around Madinah. The confederates were exhausted by cold winds and division and withdrew.",
            descAr = "تحالف مشركي مكة والقبائل بـ 10,000 جندي لمحاصرة المدينة. بإشارة من سلمان الفارسي، حفر المسلمون خندقاً عظيماً. نصر الله عباده بريح باردة وجند من عنده.",
            descUr = "کفار نے ۱۰ ہزار کے لشکر کے ساتھ مدینہ کا محاصرہ کیا۔ حضرت سلمان فارسی کے مشورے پر مسلمانوں نے مدینہ کے گرد خندق کھودی۔ تیز ٹھنڈی آندھیوں نے کفار کے پاؤں اکھاڑ دیے۔",
            statsEn = "Muslims: 3,000 (6 Martyrs) | Allied Forces: 10,000 (disbanded by cold wind)",
            statsAr = "المسلمون: 3000 (6 شهداء) | الأحزاب: 10,000 (شتتهم الريح والبرد)",
            statsUr = "مسلمان: ۳۰۰۰ (۶ شہدا) | کفار: ۱۰۰۰۰ (طوفانی ہوا نے تباہ کیا)",
            lessonEn = "Mutual consultation (Shura), innovative strategy, and absolute resilience open doors to divine protection.",
            lessonAr = "أهمية الشورى والأخذ بالابتكار الاستراتيجي (حفر الخندق) والثبات المطلق يستجلب نصر الله.",
            lessonUr = "آپس کا مشورہ (شوریٰ)، نئی جنگی حکمتِ عملی اور صبر سے اللہ کی غیبی تائید حاصل ہوتی ہے۔",
            source = "Sahih Bukhari, Surah Al-Ahzab"
        )
    )
}

fun getFamilyMembers(): List<FamilyMember> {
    return listOf(
        FamilyMember(
            nameEn = "Khadijah bint Khuwaylid (RA)",
            nameAr = "السيدة خديجة بنت خويلد",
            nameUr = "سیدہ خدیجہ بنت خویلد رضی اللہ عنہا",
            relationEn = "First Wife & Mother of Believers",
            relationAr = "الزوجة الأولى وأم المؤمنين",
            relationUr = "پہلی زوجہ اور ام المومنین",
            descEn = "A wealthy noble businesswoman. She was the first convert to Islam, console the Prophet ﷺ during first revelation, and supported him with all her wealth and status.",
            descAr = "سيدة نساء قريش وتاجرة عظيمة. أول من آمن من البشر وثبتت فؤاد النبي بمالها وجاهها وواسته بنفسها.",
            descUr = "آپ قریش کی معزز تاجرہ تھیں۔ سب سے پہلے ایمان لائیں، پہلی وحی پر تسلی دی اور اپنا سارا مال اسلام کے لیے قربان کر دیا۔"
        ),
        FamilyMember(
            nameEn = "Fatimah al-Zahra (RA)",
            nameAr = "السيدة فاطمة الزهراء",
            nameUr = "سیدہ فاطمہ الزہرا رضی اللہ عنہا",
            relationEn = "Beloved Youngest Daughter",
            relationAr = "ابنته الحبيبة وسيدة نساء الجنة",
            relationUr = "پیاری صاحبزادی اور سیدۃ النساء اہل الجنتہ",
            descEn = "The youngest daughter of the Prophet ﷺ, married to Ali (RA). Mother of Hasan & Husayn (RA). The Prophet ﷺ said: 'Fatimah is a part of me, whoever hurts her hurts me.'",
            descAr = "أصغر بناته وزوجة علي بن أبي طالب وأم الحسن والحسين. قال عنها: (فاطمة بضعة مني، فمن أغضبها أغضبني).",
            descUr = "آپ ﷺ کی سب سے پیاری صاحبزادی جو حضرت علی کی زوجہ اور حسن و حسین کی والدہ تھیں۔ آپ ﷺ نے فرمایا 'فاطمہ میرے جگر کا ٹکڑا ہے'۔"
        ),
        FamilyMember(
            nameEn = "Ibrahim ibn Muhammad",
            nameAr = "إبراهيم بن محمد ﷺ",
            nameUr = "حضرت ابراہیم بن محمد علیہ السلام",
            relationEn = "Son",
            relationAr = "ابنه الطاهر",
            relationUr = "صاحبزادے",
            descEn = "Born to Mariyah al-Qibtiyyah (RA) in Madinah. He passed away in infancy. His death coincided with a solar eclipse, and the Prophet ﷺ clarified that the sun does not eclipse for anyone's death.",
            descAr = "ولد من مارية القبطية وتوفي وهو رضيع. وتصادف موته مع خسوف الشمس، فبين النبي أن الشمس لا تخسف لموت أحد.",
            descUr = "آپ مدینہ منورہ میں حضرت ماریہ قبطیہ کے بطن سے پیدا ہوئے اور بچپن ہی میں وصال کر گئے۔ اس دن سورج گرہن لگا تو آپ ﷺ نے توہمات کی نفی فرمائی۔"
        )
    )
}

fun getCompanions(): List<CompanionItem> {
    return listOf(
        CompanionItem(
            nameEn = "Abu Bakr al-Siddiq (RA)",
            nameAr = "أبو بكر الصديق",
            nameUr = "حضرت ابوبکر صدیق رضی اللہ عنہ",
            relationEn = "Closest Friend & First Caliph",
            relationAr = "أقرب الأصدقاء والخليفة الأول",
            relationUr = "سب سے قریبی دوست اور خلیفہ اول",
            meritEn = "The first adult male to accept Islam, companion in Cave Thawr during Hijrah, and father of Aisha (RA). Known as 'As-Siddiq' (The Truthful).",
            meritAr = "أول من آمن من الرجال، وصاحبه في الغار أثناء الهجرة ووالد عائشة. لقّب بالصديق لتصديقه المطلق للإسراء.",
            meritUr = "مردوں میں سب سے پہلے ایمان لائے، سفرِ ہجرت میں غارِ ثور کے ساتھی بنے۔ تصدیقِ معراج کی وجہ سے 'صدیق' کا لقب پایا۔",
            quoteEn = "If the Prophet has said it, then it is the truth.",
            quoteAr = "إن كان قالها فقد صدق.",
            quoteUr = "اگر آپ ﷺ نے یہ فرمایا ہے تو بالکل سچ فرمایا ہے۔"
        ),
        CompanionItem(
            nameEn = "Ali ibn Abi Talib (RA)",
            nameAr = "علي بن أبي طالب",
            nameUr = "حضرت علی بن ابی طالب رضی اللہ عنہ",
            relationEn = "Cousin, Son-in-law & Fourth Caliph",
            relationAr = "ابن عمه وزوج ابنته والخليفة الرابع",
            relationUr = "چچا زاد بھائی، داماد اور خلیفہ چہارم",
            meritEn = "The first youth to accept Islam, famous for his extreme bravery at Badr and Khaibar. Slept in the Prophet's bed to fool assassins during Hijrah.",
            meritAr = "أول من آمن من الصبيان، اشتهر بشجاعته الفائقة في بدر وخيبر. نام في فراش النبي ليلة الهجرة ليموه قريش.",
            meritUr = "بچوں میں سب سے پہلے ایمان لائے، بدر و خیبر کے نامور ہیرو اور انتہائی بہادر۔ شبِ ہجرت آپ ﷺ کے بستر پر لیٹے۔",
            quoteEn = "I am the city of knowledge, and Ali is its gate.",
            quoteAr = "أنا مدينة العلم وعلي بابها (حديث شريف).",
            quoteUr = "میں علم کا شہر ہوں اور علی اس کا دروازہ ہیں (حدیثِ مبارکہ)۔"
        ),
        CompanionItem(
            nameEn = "Bilal ibn Rabah (RA)",
            nameAr = "بلال بن رباح",
            nameUr = "حضرت بلال بن رباح رضی اللہ عنہ",
            relationEn = "First Mu'adhin (Caller of Prayer)",
            relationAr = "مؤذن الرسول ﷺ والأمين",
            relationUr = "پہلے مؤذنِ اسلام",
            meritEn = "An Abyssinian slave who endured heavy torture in Makkah desert for chanting 'Ahad! Ahad!' (One God). Emancipated by Abu Bakr.",
            meritAr = "صحابي حبشي عذب في رمضاء مكة لقوله (أحد أحد)، أعتقه أبو بكر وكان مؤذن الإسلام الأول.",
            meritUr = "حبشی غلام جنہوں نے صحرائے مکہ میں 'احد! احد!' پکارنے پر شدید ترین کوڑے کھائے۔ حضرت ابوبکر نے آزاد کرایا۔",
            quoteEn = "Ahad! Ahad! (One! One!)",
            quoteAr = "أحد! أحد!",
            quoteUr = "احد! احد! (ایک ہی معبود!)"
        )
    )
}

fun getMapLocations(): List<SeerahMapLocation> {
    return listOf(
        SeerahMapLocation(
            nameEn = "Makkah",
            nameAr = "مكة المكرمة",
            nameUr = "مکہ مکرمہ",
            xPercent = 0.48f,
            yPercent = 0.70f,
            infoEn = "The birthplace of the Prophet ﷺ, site of first revelation, Cave Hira, and the Ka'bah. Here the Prophet ﷺ spent 53 years of his life preaching the oneness of Allah.",
            infoAr = "مهد ولادة النبي ﷺ وموقع نزول الوحي الأول في غار حراء والكعبة المشرفة. قضى فيها 53 سنة من حياته المباركة.",
            infoUr = "آپ ﷺ کی جائے پیدائش، پہلی وحی (غارِ حرا) اور کعبہ کا مقام۔ یہاں آپ ﷺ نے اپنی زندگی کے ۵۳ سال گزارے۔"
        ),
        SeerahMapLocation(
            nameEn = "Madinah",
            nameAr = "المدينة المنورة",
            nameUr = "مدینہ منورہ",
            xPercent = 0.45f,
            yPercent = 0.48f,
            infoEn = "The city of migration (Hijrah), home of the Ansar, and the founding city of the Islamic state. It houses Al-Masjid an-Nabawi and the grave of Prophet ﷺ.",
            infoAr = "مدينة الهجرة النبوية وموطن الأنصار ومدينة تأسيس الدولة الإسلامية. فيها المسجد النبوي الشريف وقبره الطاهر.",
            infoUr = "ہجرت کا مقام، انصار کا گھر اور پہلی اسلامی ریاست کا مرکز۔ یہاں مسجدِ نبوی اور آپ ﷺ کا روضہ مبارک واقع ہے۔"
        ),
        SeerahMapLocation(
            nameEn = "Badr",
            nameAr = "بدر",
            nameUr = "بدر",
            xPercent = 0.30f,
            yPercent = 0.55f,
            infoEn = "Located southwest of Madinah, it is the historical plain where the legendary battle of Badr took place in 2 AH, validating the divine truth of Islam.",
            infoAr = "تقع جنوب غرب المدينة، وهي السهل الذي دارت فيه معركة بدر الكبرى التاريخية سنة 2 هـ ونصر الله فيها دينه.",
            infoUr = "مدینہ کے جنوب مغرب میں واقع وہ میدان جہاں ۲ ہجری میں تاریخی معرکہِ بدر لڑا گیا اور اسلام کو فتح نصیب ہوئی۔"
        ),
        SeerahMapLocation(
            nameEn = "Ta'if",
            nameAr = "الطائف",
            nameUr = "طائف",
            xPercent = 0.55f,
            yPercent = 0.78f,
            infoEn = "A mountain city where the Prophet ﷺ went to seek support in 619 CE, but was rejected and stoned. Rather than seeking their destruction, he prayed for their descendants to accept Islam.",
            infoAr = "مدينة جبلية قصدها النبي سنة 619 م طلباً للدعم، لكن أهلها رجموه بالحجارة، فدعا لهم بالهداية بدلاً من الهلاك.",
            infoUr = "ایک پہاڑی شہر جہاں آپ ﷺ ۶۱۹ عیسوی میں دعوت لے کر گئے لیکن انہوں نے پتھر مارے۔ آپ ﷺ نے بددعا کے بجائے ان کی نسلوں کی ہدایت کی دعا فرمائی۔"
        )
    )
}
