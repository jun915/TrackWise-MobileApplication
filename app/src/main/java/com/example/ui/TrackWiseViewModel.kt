package com.example.ui

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.repository.TrackWiseRepository
import com.example.utils.TrackWiseUtils
import com.example.utils.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class TrackWiseViewModel(
    application: Application,
    private val repository: TrackWiseRepository
) : AndroidViewModel(application) {

    // --- Session State ---
    private val _sessionUser = MutableStateFlow<UserEntity?>(null)
    val sessionUser: StateFlow<UserEntity?> = _sessionUser.asStateFlow()

    val isLoggedIn: Flow<Boolean> = _sessionUser.map { it != null }

    // --- Temporary Error/Success States ---
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // --- Notification State ---
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _notificationNavigateTab = MutableStateFlow<String?>(null)
    val notificationNavigateTab: StateFlow<String?> = _notificationNavigateTab.asStateFlow()

    data class UndoActionState(
        val type: String, // "task" or "habit"
        val id: String,
        val title: String,
        val isCompleted: Boolean,
        val originalTask: TaskEntity? = null,
        val originalHabit: HabitEntity? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _undoActionState = MutableStateFlow<UndoActionState?>(null)
    val undoActionState: StateFlow<UndoActionState?> = _undoActionState.asStateFlow()

    fun clearUndoAction() {
        _undoActionState.value = null
    }

    fun undoLastAction() {
        val state = _undoActionState.value ?: return
        _undoActionState.value = null
        viewModelScope.launch(Dispatchers.IO) {
            if (state.type == "task" && state.originalTask != null) {
                repository.insertTask(state.originalTask)
                triggerFakeSync()
            } else if (state.type == "habit" && state.originalHabit != null) {
                repository.insertHabit(state.originalHabit)
                triggerFakeSync()
            }
        }
    }

    fun setNotificationNavigateTab(tab: String?) {
        _notificationNavigateTab.value = tab
    }

    fun addNotification(
        title: String,
        message: String,
        showSystem: Boolean = false,
        taskId: String? = null,
        tabletId: String? = null,
        canSnooze: Boolean = false
    ) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timeStr = sdf.format(Date())
        val newNotification = AppNotification(
            id = "notif-${System.currentTimeMillis()}",
            title = title,
            message = message,
            timestamp = timeStr
        )
        _notifications.value = listOf(newNotification) + _notifications.value
        triggerNotificationVibration()
        if (showSystem) {
            showSystemNotification(title, message, taskId, tabletId, canSnooze)
        }
    }

    private fun showSystemNotification(
        title: String,
        message: String,
        taskId: String? = null,
        tabletId: String? = null,
        canSnooze: Boolean = false
    ) {
        try {
            val context = getApplication<Application>().applicationContext
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channelId = "trackwise_notifications"
                val channelName = "TrackWise Notifications"
                val importance = android.app.NotificationManager.IMPORTANCE_HIGH
                val channel = android.app.NotificationChannel(channelId, channelName, importance).apply {
                    description = "System notifications for TrackWise app events"
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            val iconId = context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)
            val smallIcon = if (iconId != 0) iconId else android.R.drawable.ic_dialog_info

            val targetTab = when {
                title.contains("Budget", ignoreCase = true) || 
                title.contains("Finance", ignoreCase = true) ||
                title.contains("Transaction", ignoreCase = true) ||
                title.contains("Spent", ignoreCase = true) ||
                title.contains("Savings", ignoreCase = true) ||
                title.contains("Income", ignoreCase = true) -> "finance"
                
                title.contains("Alarm", ignoreCase = true) ||
                title.contains("Timer", ignoreCase = true) -> "dashboard"
                
                title.contains("Habit", ignoreCase = true) ||
                title.contains("Daily", ignoreCase = true) -> "dashboard"
                
                title.contains("Health", ignoreCase = true) ||
                title.contains("Weight", ignoreCase = true) ||
                title.contains("Water", ignoreCase = true) ||
                title.contains("Steps", ignoreCase = true) ||
                title.contains("Calorie", ignoreCase = true) -> "health"
                
                title.contains("Imported", ignoreCase = true) ||
                title.contains("Exported", ignoreCase = true) ||
                title.contains("Profile", ignoreCase = true) ||
                title.contains("Session", ignoreCase = true) ||
                title.contains("Backup", ignoreCase = true) -> "profile"
                
                title.contains("Help", ignoreCase = true) ||
                title.contains("Guide", ignoreCase = true) -> "help"
                
                else -> "dashboard"
            }

            val intent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("target_tab", targetTab)
            }
            val pendingIntentFlags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                pendingIntentFlags
            )

            val notificationId = System.currentTimeMillis().toInt()

            val actionFlags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }

            // Snooze action
            val snoozeIntent = android.content.Intent(context, com.example.receiver.ReminderReceiver::class.java).apply {
                action = "com.example.action.NOTIFICATION_SNOOZE"
                putExtra("notification_id", notificationId)
                putExtra("title", title)
                putExtra("message", message)
            }
            val snoozePendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                notificationId + 3000,
                snoozeIntent,
                actionFlags
            )

            // Dismiss action
            val dismissIntent = android.content.Intent(context, com.example.receiver.ReminderReceiver::class.java).apply {
                action = "com.example.action.NOTIFICATION_DISMISS"
                putExtra("notification_id", notificationId)
            }
            val dismissPendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                notificationId + 4000,
                dismissIntent,
                actionFlags
            )

            val builder = androidx.core.app.NotificationCompat.Builder(context, "trackwise_notifications")
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setVibrate(longArrayOf(0, 250, 100, 250))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            // Complete action
            if (taskId != null || tabletId != null) {
                val completeIntent = android.content.Intent(context, com.example.receiver.ReminderReceiver::class.java).apply {
                    action = "com.example.action.NOTIFICATION_COMPLETE"
                    putExtra("notification_id", notificationId)
                    putExtra("task_id", taskId)
                    putExtra("tablet_id", tabletId)
                }
                val completePendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    notificationId + 2000,
                    completeIntent,
                    actionFlags
                )
                builder.addAction(0, "Complete", completePendingIntent)
            }

            if (canSnooze) {
                builder.addAction(0, "Snooze (5 min)", snoozePendingIntent)
            }
            builder.addAction(0, "Dismiss", dismissPendingIntent)
                
            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    fun updateAppWidget() {
        try {
            val context = getApplication<Application>().applicationContext
            val intent = android.content.Intent(context, com.example.widget.TrackWiseWidgetProvider::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, com.example.widget.TrackWiseWidgetProvider::class.java)
            )
            intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    // --- UI App Preferences ---
    private val _showEarlyToRiseSleepDialog = MutableStateFlow(false)
    val showEarlyToRiseSleepDialog: StateFlow<Boolean> = _showEarlyToRiseSleepDialog.asStateFlow()

    fun setShowEarlyToRiseSleepDialog(show: Boolean) {
        _showEarlyToRiseSleepDialog.value = show
    }

    private val _showSportExerciseDialog = MutableStateFlow(false)
    val showSportExerciseDialog: StateFlow<Boolean> = _showSportExerciseDialog.asStateFlow()

    private val _sportHabitCompletedName = MutableStateFlow("")
    val sportHabitCompletedName: StateFlow<String> = _sportHabitCompletedName.asStateFlow()

    fun setShowSportExerciseDialog(show: Boolean) {
        _showSportExerciseDialog.value = show
    }

    private val _pinnedFinanceLogIds = MutableStateFlow<List<String>>(emptyList())
    val pinnedFinanceLogIds: StateFlow<List<String>> = _pinnedFinanceLogIds.asStateFlow()

    fun togglePinFinanceLog(id: String) {
        _pinnedFinanceLogIds.value = if (_pinnedFinanceLogIds.value.contains(id)) {
            _pinnedFinanceLogIds.value - id
        } else {
            _pinnedFinanceLogIds.value + id
        }
    }

    private val _themeMode = MutableStateFlow("light") // "light", "dark", or "system"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _taskSound = MutableStateFlow("Chime Gentle")
    val taskSound: StateFlow<String> = _taskSound.asStateFlow()

    private val _alarmSound = MutableStateFlow("Reflection")
    val alarmSound: StateFlow<String> = _alarmSound.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _vibrateOnTaskCompletion = MutableStateFlow(true)
    val vibrateOnTaskCompletion: StateFlow<Boolean> = _vibrateOnTaskCompletion.asStateFlow()

    private val _vibrateOnSwipe = MutableStateFlow(true)
    val vibrateOnSwipe: StateFlow<Boolean> = _vibrateOnSwipe.asStateFlow()

    private val _vibrateOnNotification = MutableStateFlow(true)
    val vibrateOnNotification: StateFlow<Boolean> = _vibrateOnNotification.asStateFlow()

    private val _appThemeSelection = MutableStateFlow("Default Violet")
    val appThemeSelection: StateFlow<String> = _appThemeSelection.asStateFlow()

    private val _appBgType = MutableStateFlow("image") // "none", "color", "gradient", "image"
    val appBgType: StateFlow<String> = _appBgType.asStateFlow()

    private val _appBgColor = MutableStateFlow("Lavender & Amethyst")
    val appBgColor: StateFlow<String> = _appBgColor.asStateFlow()

    private val _appBgGradient = MutableStateFlow("Sunset Glow")
    val appBgGradient: StateFlow<String> = _appBgGradient.asStateFlow()

    private val _appBgImage = MutableStateFlow("https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=800")
    val appBgImage: StateFlow<String> = _appBgImage.asStateFlow()

    private val _appBgCustomUri = MutableStateFlow("")
    val appBgCustomUri: StateFlow<String> = _appBgCustomUri.asStateFlow()

    private val _profileImageUri = MutableStateFlow<String?>(null)
    val profileImageUri: StateFlow<String?> = _profileImageUri.asStateFlow()

    private val _appFontSize = MutableStateFlow("Medium")
    val appFontSize: StateFlow<String> = _appFontSize.asStateFlow()

    private val _appFontStyle = MutableStateFlow("Default")
    val appFontStyle: StateFlow<String> = _appFontStyle.asStateFlow()

    private val _settingsPanelOpen = MutableStateFlow(false)
    val settingsPanelOpen: StateFlow<Boolean> = _settingsPanelOpen.asStateFlow()

    private val _selectedTaskFolder = MutableStateFlow<String?>(null)
    val selectedTaskFolder: StateFlow<String?> = _selectedTaskFolder.asStateFlow()

    private val _selectedTaskTag = MutableStateFlow<String?>(null)
    val selectedTaskTag: StateFlow<String?> = _selectedTaskTag.asStateFlow()

    private val _customFolders = MutableStateFlow<List<String>>(emptyList())
    val customFolders: StateFlow<List<String>> = _customFolders.asStateFlow()

    private val _customTags = MutableStateFlow<List<String>>(emptyList())
    val customTags: StateFlow<List<String>> = _customTags.asStateFlow()

    private val _deletedFolders = MutableStateFlow<List<String>>(emptyList())
    val deletedFolders: StateFlow<List<String>> = _deletedFolders.asStateFlow()

    private val _deletedTags = MutableStateFlow<List<String>>(emptyList())
    val deletedTags: StateFlow<List<String>> = _deletedTags.asStateFlow()

    data class BadHabitSpec(
        val id: String,
        val name: String,
        val avoidType: String = "Habit",
        val reminderTime: String = "",
        val tags: List<String> = emptyList(),
        val priority: String = "Medium",
        val isRecurring: Boolean = true,
        val eventDate: String = "",
        val costType: String = "Money",
        val costValue: String = "",
        val iconName: String = "Block",
        val avoidCount: Int = 0,
        val logs: List<String> = emptyList() // Timestamps of occurrences
    ) {
        val avoidedCount: Int get() = avoidCount
        val slippedCount: Int get() = logs.size
    }

    private val _badHabits = MutableStateFlow<List<BadHabitSpec>>(emptyList())
    val badHabits: StateFlow<List<BadHabitSpec>> = _badHabits
        .map { list ->
            list.sortedWith(compareBy<BadHabitSpec> { 
                it.reminderTime.ifBlank { "99:99" } 
            }.thenBy { it.name })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _habitBreakerViewState = MutableStateFlow("list") // "list", "gallery", "create"
    val habitBreakerViewState: StateFlow<String> = _habitBreakerViewState.asStateFlow()

    fun setHabitBreakerViewState(view: String) {
        _habitBreakerViewState.value = view
    }

    fun loadBadHabits() {
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
        val jsonV2 = prefs.getString("bad_habits_json_v2", "") ?: ""
        if (jsonV2.isNotBlank()) {
            try {
                val array = org.json.JSONArray(jsonV2)
                val list = mutableListOf<BadHabitSpec>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val bhId = obj.getString("id")
                    val bhName = obj.getString("name")
                    val avoidType = obj.optString("avoidType", "Habit")
                    val reminderTime = obj.optString("reminderTime", "")
                    val tagsList = mutableListOf<String>()
                    if (obj.has("tags")) {
                        val tArr = obj.getJSONArray("tags")
                        for (j in 0 until tArr.length()) tagsList.add(tArr.getString(j))
                    }
                    val priority = obj.optString("priority", "Medium")
                    val isRecurring = obj.optBoolean("isRecurring", true)
                    val eventDate = obj.optString("eventDate", "")
                    val costType = obj.optString("costType", "Money")
                    val costValue = obj.optString("costValue", "")
                    val iconName = obj.optString("iconName", "Block")
                    val avoidCount = obj.optInt("avoidCount", 0)
                    val logsList = mutableListOf<String>()
                    if (obj.has("logs")) {
                        val lArr = obj.getJSONArray("logs")
                        for (j in 0 until lArr.length()) logsList.add(lArr.getString(j))
                    }
                    list.add(
                        BadHabitSpec(
                            id = bhId,
                            name = bhName,
                            avoidType = avoidType,
                            reminderTime = reminderTime,
                            tags = tagsList,
                            priority = priority,
                            isRecurring = isRecurring,
                            eventDate = eventDate,
                            costType = costType,
                            costValue = costValue,
                            iconName = iconName,
                            avoidCount = avoidCount,
                            logs = logsList
                        )
                    )
                }
                _badHabits.value = list
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback to legacy string or default list
        val listStr = prefs.getString("bad_habits_list", "") ?: ""
        if (listStr.isBlank()) {
            val defaultList = listOf(
                BadHabitSpec("lying", "Lying to others", avoidType = "Habit", tags = listOf("Social"), priority = "High"),
                BadHabitSpec("procrastination", "Procrastinating", avoidType = "Habit", tags = listOf("Productivity"), priority = "Medium"),
                BadHabitSpec("nail_biting", "Nail biting", avoidType = "Habit", tags = listOf("Health"), priority = "Low")
            )
            saveBadHabitsList(defaultList)
            _badHabits.value = defaultList
        } else {
            val list = listStr.split(",").mapNotNull { item ->
                val parts = item.split("|")
                if (parts.size >= 2) {
                    val id = parts[0]
                    val name = parts[1]
                    val logsStr = prefs.getString("bad_habit_logs_$id", "") ?: ""
                    val logs = if (logsStr.isNotBlank()) logsStr.split(";") else emptyList()
                    BadHabitSpec(id = id, name = name, logs = logs)
                } else null
            }
            _badHabits.value = list
            saveBadHabitsList(list)
        }
    }

    private fun saveBadHabitsList(list: List<BadHabitSpec>) {
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
        val array = org.json.JSONArray()
        list.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("avoidType", item.avoidType)
            obj.put("reminderTime", item.reminderTime)
            val tagsArr = org.json.JSONArray()
            item.tags.forEach { t -> tagsArr.put(t) }
            obj.put("tags", tagsArr)
            obj.put("priority", item.priority)
            obj.put("isRecurring", item.isRecurring)
            obj.put("eventDate", item.eventDate)
            obj.put("costType", item.costType)
            obj.put("costValue", item.costValue)
            obj.put("iconName", item.iconName)
            obj.put("avoidCount", item.avoidCount)
            val logsArr = org.json.JSONArray()
            item.logs.forEach { l -> logsArr.put(l) }
            obj.put("logs", logsArr)
            array.put(obj)
        }
        prefs.edit().putString("bad_habits_json_v2", array.toString()).apply()
    }

    fun addBadHabit(
        name: String,
        avoidType: String = "Habit",
        reminderTime: String = "",
        tags: List<String> = emptyList(),
        priority: String = "Medium",
        isRecurring: Boolean = true,
        eventDate: String = "",
        costType: String = "Money",
        costValue: String = "",
        iconName: String = "Block"
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val id = "bad_habit_" + System.currentTimeMillis()
        val current = _badHabits.value.toMutableList()
        val newItem = BadHabitSpec(
            id = id,
            name = cleanName,
            avoidType = avoidType,
            reminderTime = reminderTime,
            tags = tags,
            priority = priority,
            isRecurring = isRecurring,
            eventDate = eventDate,
            costType = costType,
            costValue = costValue,
            iconName = iconName,
            logs = emptyList()
        )
        current.add(newItem)
        _badHabits.value = current
        saveBadHabitsList(current)
        addNotification("Item Added to Avoid List 🚫", "Now tracking '$cleanName'. Stay strong!")
    }

    fun updateBadHabit(
        id: String,
        name: String,
        avoidType: String = "Habit",
        reminderTime: String = "",
        tags: List<String> = emptyList(),
        priority: String = "Medium",
        isRecurring: Boolean = true,
        eventDate: String = "",
        costType: String = "Money",
        costValue: String = "",
        iconName: String = "Block"
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val current = _badHabits.value.map { item ->
            if (item.id == id) {
                item.copy(
                    name = cleanName,
                    avoidType = avoidType,
                    reminderTime = reminderTime,
                    tags = tags,
                    priority = priority,
                    isRecurring = isRecurring,
                    eventDate = eventDate,
                    costType = costType,
                    costValue = costValue,
                    iconName = iconName
                )
            } else item
        }
        _badHabits.value = current
        saveBadHabitsList(current)
        addNotification("Item Updated ✏️", "Updated '$cleanName' successfully.")
    }

    fun logBadHabitAvoidance(id: String) {
        var habitName = "Bad Habit"
        val current = _badHabits.value.map { item ->
            if (item.id == id) {
                habitName = item.name
                item.copy(avoidCount = item.avoidCount + 1)
            } else item
        }
        _badHabits.value = current
        saveBadHabitsList(current)
        addNotification("Successfully Avoided! ✨", "Maintained clean record for '$habitName'. Keep it up!")
    }

    fun logBadHabitOccurrence(id: String) {
        var habitName = "Bad Habit"
        val current = _badHabits.value.map { item ->
            if (item.id == id) {
                habitName = item.name
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                item.copy(logs = item.logs + timestamp)
            } else item
        }
        _badHabits.value = current
        saveBadHabitsList(current)
        addNotification("Slip-Up Logged ⚠️", "Logged occurrence for '$habitName'. Take a deep breath and regain focus.")
    }

    fun undoBadHabitAvoidance(id: String) {
        var habitName = "Bad Habit"
        val current = _badHabits.value.map { item ->
            if (item.id == id && item.avoidCount > 0) {
                habitName = item.name
                item.copy(avoidCount = item.avoidCount - 1)
            } else item
        }
        _badHabits.value = current
        saveBadHabitsList(current)
        addNotification("Undo Completed 🔄", "Reverted avoidance count for '$habitName'.")
    }

    fun undoBadHabitOccurrence(id: String) {
        var habitName = "Bad Habit"
        val current = _badHabits.value.map { item ->
            if (item.id == id && item.logs.isNotEmpty()) {
                habitName = item.name
                item.copy(logs = item.logs.dropLast(1))
            } else item
        }
        _badHabits.value = current
        saveBadHabitsList(current)
        addNotification("Undo Completed 🔄", "Removed last slip-up log for '$habitName'.")
    }

    fun removeBadHabit(id: String) {
        val current = _badHabits.value.filter { it.id != id }
        _badHabits.value = current
        saveBadHabitsList(current)
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
        prefs.edit().remove("bad_habit_logs_$id").apply()
    }

    fun updateBadHabitIcon(id: String, newIconName: String) {
        val current = _badHabits.value.map { item ->
            if (item.id == id) {
                item.copy(iconName = newIconName)
            } else item
        }
        _badHabits.value = current
        saveBadHabitsList(current)
    }

    private fun getSharedPrefs(): android.content.SharedPreferences {
        return getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
    }

    private val _bottomBarTabIds = MutableStateFlow<List<String>>(loadBottomBarTabs())
    val bottomBarTabIds: StateFlow<List<String>> = _bottomBarTabIds.asStateFlow()

    private fun loadBottomBarTabs(): List<String> {
        val prefs = getSharedPrefs()
        val saved = prefs.getString("bottom_bar_tab_ids_v1", null)
        if (!saved.isNullOrBlank()) {
            val list = saved.split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (list.isNotEmpty()) {
                val taken = list.take(4)
                if (taken.size in 2..4) return taken
            }
        }
        return listOf("dashboard", "tasks", "habits", "countdown")
    }

    fun setBottomBarTabs(tabs: List<String>) {
        val limited = tabs.take(4)
        if (limited.size in 2..4) {
            _bottomBarTabIds.value = limited
            getSharedPrefs().edit().putString("bottom_bar_tab_ids_v1", limited.joinToString(",")).apply()
        }
    }

    fun addTabToBottomBar(tabId: String) {
        val current = _bottomBarTabIds.value.toMutableList()
        if (!current.contains(tabId) && current.size < 4) {
            current.add(tabId)
            setBottomBarTabs(current)
        }
    }

    fun removeTabFromBottomBar(tabId: String) {
        val current = _bottomBarTabIds.value.toMutableList()
        if (current.contains(tabId) && current.size > 2) {
            current.remove(tabId)
            setBottomBarTabs(current)
        }
    }

    fun moveTabInBottomBar(fromIndex: Int, toIndex: Int) {
        val current = _bottomBarTabIds.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            setBottomBarTabs(current)
        }
    }

    fun deleteFolderPermanently(folder: String) {
        val trimmed = folder.trim()
        if (!_deletedFolders.value.contains(trimmed)) {
            val newList = _deletedFolders.value + trimmed
            _deletedFolders.value = newList
            val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("deleted_folders_list", newList.joinToString(",")).apply()
        }
        deleteCustomFolder(trimmed)
    }

    fun deleteTagPermanently(tag: String) {
        val cleanTag = tag.trim().removePrefix("#")
        if (!_deletedTags.value.contains(cleanTag)) {
            val newList = _deletedTags.value + cleanTag
            _deletedTags.value = newList
            val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("deleted_tags_list", newList.joinToString(",")).apply()
        }
        deleteCustomTag(cleanTag)
    }

    fun addCustomFolder(folder: String) {
        val trimmed = folder.trim()
        if (trimmed.isNotEmpty() && !_customFolders.value.contains(trimmed)) {
            val newList = _customFolders.value + trimmed
            _customFolders.value = newList
            val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("custom_folders_list", newList.joinToString(",")).apply()
        }
    }

    fun addCustomTag(tag: String) {
        val trimmed = tag.trim().removePrefix("#")
        if (trimmed.isNotEmpty() && !_customTags.value.contains(trimmed)) {
            val newList = _customTags.value + trimmed
            _customTags.value = newList
            val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("custom_tags_list", newList.joinToString(",")).apply()
        }
    }

    fun deleteCustomFolder(folder: String) {
        val trimmed = folder.trim()
        val newList = _customFolders.value.filter { !it.equals(trimmed, ignoreCase = true) }
        _customFolders.value = newList
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("custom_folders_list", newList.joinToString(",")).apply()

        if (selectedTaskFolder.value.equals(trimmed, ignoreCase = true)) {
            _selectedTaskFolder.value = null
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            allTasks.value.filter { it.project.equals(trimmed, ignoreCase = true) }.forEach { task ->
                updateTask(task.copy(project = "Inbox"))
            }
            allHabits.value.forEach { habit ->
                val sections = habit.section.split(",").map { it.trim() }.filter { !it.equals(trimmed, ignoreCase = true) }
                val newSec = if (sections.isEmpty()) "Inbox" else sections.joinToString(",")
                if (newSec != habit.section) {
                    updateHabit(habit.copy(section = newSec))
                }
            }
        }
    }

    fun deleteCustomTag(tag: String) {
        val cleanTag = tag.trim().removePrefix("#")
        val newList = _customTags.value.filter { !it.equals(cleanTag, ignoreCase = true) }
        _customTags.value = newList
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("custom_tags_list", newList.joinToString(",")).apply()

        if (selectedTaskTag.value.equals(cleanTag, ignoreCase = true)) {
            _selectedTaskTag.value = null
        }

        val tagWithHash = "#$cleanTag"
        viewModelScope.launch(Dispatchers.IO) {
            allTasks.value.forEach { task ->
                if (task.title.contains(tagWithHash, ignoreCase = true) ||
                    task.description.contains(tagWithHash, ignoreCase = true) ||
                    task.notes.contains(tagWithHash, ignoreCase = true)) {
                    val newTitle = task.title.replace(tagWithHash, "", ignoreCase = true).trim()
                    val newDesc = task.description.replace(tagWithHash, "", ignoreCase = true).trim()
                    val newNotes = task.notes.replace(tagWithHash, "", ignoreCase = true).trim()
                    updateTask(task.copy(title = if (newTitle.isEmpty()) "Task" else newTitle, description = newDesc, notes = newNotes))
                }
            }
            allHabits.value.forEach { habit ->
                if (habit.name.contains(tagWithHash, ignoreCase = true) ||
                    habit.notes.contains(tagWithHash, ignoreCase = true) ||
                    habit.category.contains(tagWithHash, ignoreCase = true)) {
                    val newName = habit.name.replace(tagWithHash, "", ignoreCase = true).trim()
                    val newNotes = habit.notes.replace(tagWithHash, "", ignoreCase = true).trim()
                    val newCat = habit.category.replace(tagWithHash, "", ignoreCase = true).trim()
                    updateHabit(habit.copy(name = if (newName.isEmpty()) "Habit" else newName, notes = newNotes, category = newCat))
                }
            }
        }
    }

    private val _calendarOverlay = MutableStateFlow("islamic") // "islamic", "none", "hindu"
    val calendarOverlay: StateFlow<String> = _calendarOverlay.asStateFlow()

    private val _autoBackupFrequency = MutableStateFlow("none") // "none", "hourly", "daily", "weekly"
    val autoBackupFrequency: StateFlow<String> = _autoBackupFrequency.asStateFlow()

    private val _lastAutoBackupTime = MutableStateFlow(0L)
    val lastAutoBackupTime: StateFlow<Long> = _lastAutoBackupTime.asStateFlow()

    // --- Sync UI indicator state ---
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow("Auto-saved offline data")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    // --- Habit Detail & Edit State ---
    private val _activeDetailHabit = MutableStateFlow<HabitEntity?>(null)
    val activeDetailHabit: StateFlow<HabitEntity?> = _activeDetailHabit.asStateFlow()

    private val _habitToEdit = MutableStateFlow<HabitEntity?>(null)
    val habitToEdit: StateFlow<HabitEntity?> = _habitToEdit.asStateFlow()

    fun setActiveDetailHabit(habit: HabitEntity?) {
        _activeDetailHabit.value = habit
    }

    fun setHabitToEdit(habit: HabitEntity?) {
        _habitToEdit.value = habit
        if (habit != null) {
            setWorkspaceSubTab(1) // Switch to Habit Sub-tab
            _showHabitCreationSheet.value = true // Open the Habit creation dialog as editing!
        }
    }

    // --- Dynamic Data Streams ---
    val allTasks: StateFlow<List<TaskEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getTasksFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHabits: StateFlow<List<HabitEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getHabitsFlow(user.id).map { habits ->
                    habits.sortedWith(compareBy<HabitEntity> { 
                        val time = it.reminderTime ?: ""
                        if (time.isBlank()) "99:99" else time
                    }.thenBy { it.name })
                }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHabitsInSystem: StateFlow<List<HabitEntity>> = repository.getAllHabitsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBirthdays: StateFlow<List<BirthdayEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getBirthdaysFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWishlist: StateFlow<List<WishItemEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getWishlistFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Workspace Navigation & Grocery Check List State ---
    private val _workspaceSubTab = MutableStateFlow(0)
    val workspaceSubTab: StateFlow<Int> = _workspaceSubTab.asStateFlow()

    fun setWorkspaceSubTab(tabIndex: Int) {
        _workspaceSubTab.value = tabIndex
    }

    private val _triggerAddOccasion = MutableStateFlow<String?>(null)
    val triggerAddOccasion: StateFlow<String?> = _triggerAddOccasion.asStateFlow()

    fun triggerAddOccasion(category: String) {
        _triggerAddOccasion.value = category
    }

    fun clearTriggerAddOccasion() {
        _triggerAddOccasion.value = null
    }

    private val _healthSubTab = MutableStateFlow(0)
    val healthSubTab: StateFlow<Int> = _healthSubTab.asStateFlow()

    private val _showHealthOptionsOverlay = MutableStateFlow(false)
    val showHealthOptionsOverlay: StateFlow<Boolean> = _showHealthOptionsOverlay.asStateFlow()

    fun setShowHealthOptionsOverlay(show: Boolean) {
        _showHealthOptionsOverlay.value = show
    }

    fun setHealthSubTab(tabIndex: Int) {
        _healthSubTab.value = tabIndex
    }

    private val _socialSubTab = MutableStateFlow("friends")
    val socialSubTab: StateFlow<String> = _socialSubTab.asStateFlow()

    fun setSocialSubTab(tab: String) {
        _socialSubTab.value = tab
    }

    val allGroceryItems: StateFlow<List<GroceryItemEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getGroceryItemsFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val streakHistory: StateFlow<List<StreakHistoryEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getStreakHistoryFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weightEntries: StateFlow<List<WeightEntryEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getWeightEntriesFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vitalReadings: StateFlow<List<VitalReadingEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getVitalsFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waterLogs: StateFlow<List<WaterLogEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getWaterLogsFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exerciseLogs: StateFlow<List<ExerciseLogEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getExerciseLogsFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val healthIssueLogs: StateFlow<List<HealthIssueLogEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getHealthIssueLogsFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tabletReminders: StateFlow<List<TabletReminderEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getTabletRemindersFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val periodCycles: StateFlow<List<PeriodCycleEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getPeriodCyclesFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFinanceLogs: StateFlow<List<FinanceLogEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getFinanceLogsFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNetWorthItems: StateFlow<List<NetWorthItemEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getNetWorthItemsFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendConnections: StateFlow<List<FriendConnectionEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getFriendsFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlarms: StateFlow<List<AlarmEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getAlarmsFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sleepLogs: StateFlow<List<SleepLogEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getSleepLogsFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfileEntity?> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getUserProfileFlow(user.id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Notebooks & Notes State ---
    val allNotebooks: StateFlow<List<NotebookEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getNotebooksFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedNotebook = MutableStateFlow<NotebookEntity?>(null)
    val selectedNotebook: StateFlow<NotebookEntity?> = _selectedNotebook.asStateFlow()

    fun selectNotebook(notebook: NotebookEntity?) {
        _selectedNotebook.value = notebook
    }

    val notesForSelectedNotebook: StateFlow<List<NoteEntity>> = combine(
        _sessionUser,
        _selectedNotebook
    ) { user, notebook ->
        Pair(user, notebook)
    }.flatMapLatest { (user, notebook) ->
        if (user != null && notebook != null) {
            repository.getNotesForNotebookFlow(notebook.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeNoteToEdit = MutableStateFlow<NoteEntity?>(null)
    val activeNoteToEdit: StateFlow<NoteEntity?> = _activeNoteToEdit.asStateFlow()

    private val _notesViewMode = MutableStateFlow("grid") // "grid" or "list"
    val notesViewMode: StateFlow<String> = _notesViewMode.asStateFlow()

    fun setNotesViewMode(mode: String) {
        _notesViewMode.value = mode
    }

    fun openNoteToEdit(note: NoteEntity?) {
        _activeNoteToEdit.value = note
    }

    fun closeNoteEditor() {
        _activeNoteToEdit.value = null
    }

    private val _showCreateNotebookDialog = MutableStateFlow(false)
    val showCreateNotebookDialog: StateFlow<Boolean> = _showCreateNotebookDialog.asStateFlow()

    fun setShowCreateNotebookDialog(show: Boolean) {
        _showCreateNotebookDialog.value = show
    }

    private val _isNotesSpeedDialOpen = MutableStateFlow(false)
    val isNotesSpeedDialOpen: StateFlow<Boolean> = _isNotesSpeedDialOpen.asStateFlow()

    fun setNotesSpeedDialOpen(open: Boolean) {
        _isNotesSpeedDialOpen.value = open
    }

    fun toggleNotesSpeedDial() {
        _isNotesSpeedDialOpen.value = !_isNotesSpeedDialOpen.value
    }

    private val _isNotebookSearchActive = MutableStateFlow(false)
    val isNotebookSearchActive: StateFlow<Boolean> = _isNotebookSearchActive.asStateFlow()

    fun setNotebookSearchActive(active: Boolean) {
        _isNotebookSearchActive.value = active
    }

    fun toggleNotebookSearchActive() {
        _isNotebookSearchActive.value = !_isNotebookSearchActive.value
    }

    private val _isNoteSearchActive = MutableStateFlow(false)
    val isNoteSearchActive: StateFlow<Boolean> = _isNoteSearchActive.asStateFlow()

    fun setNoteSearchActive(active: Boolean) {
        _isNoteSearchActive.value = active
    }

    fun toggleNoteSearchActive() {
        _isNoteSearchActive.value = !_isNoteSearchActive.value
    }

    fun createAndOpenNewNote() {
        val currentNotebook = _selectedNotebook.value ?: return
        openNoteToEdit(
            NoteEntity(
                id = UUID.randomUUID().toString(),
                notebookId = currentNotebook.id,
                userId = currentNotebook.userId,
                title = "",
                content = "",
                cardColor = "#FFF59D",
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            )
        )
    }

    fun onMainFabClickInNotes() {
        toggleNotesSpeedDial()
    }

    fun createNotebook(title: String, coverPreset: String = "preset_1", coverColor: String = "#FF9800", customCoverUri: String? = null) {
        val user = _sessionUser.value ?: return
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val notebook = NotebookEntity(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            title = if (title.isBlank()) "Untitled" else title,
            coverPreset = coverPreset,
            coverColor = coverColor,
            customCoverUri = customCoverUri,
            createdAt = now,
            updatedAt = now
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertNotebook(notebook)
            selectNotebook(notebook)
            triggerFakeSync()
        }
    }

    fun updateNotebook(notebook: NotebookEntity) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertNotebook(notebook.copy(updatedAt = now))
            if (_selectedNotebook.value?.id == notebook.id) {
                _selectedNotebook.value = notebook
            }
            triggerFakeSync()
        }
    }

    fun deleteNotebook(notebookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNotebook(notebookId)
            if (_selectedNotebook.value?.id == notebookId) {
                _selectedNotebook.value = null
            }
            triggerFakeSync()
        }
    }

    fun createNote(notebookId: String, title: String, content: String, cardColor: String = "#FFF59D") {
        val user = _sessionUser.value ?: return
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val note = NoteEntity(
            id = UUID.randomUUID().toString(),
            notebookId = notebookId,
            userId = user.id,
            title = if (title.isBlank()) "Untitled" else title,
            content = content,
            cardColor = cardColor,
            createdAt = now,
            updatedAt = now
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertNote(note)
            triggerFakeSync()
        }
    }

    fun updateNote(note: NoteEntity) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertNote(note.copy(updatedAt = now))
            triggerFakeSync()
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNote(noteId)
            if (_activeNoteToEdit.value?.id == noteId) {
                _activeNoteToEdit.value = null
            }
            triggerFakeSync()
        }
    }

    fun ensureDefaultNotebookSeeded() {
        val user = _sessionUser.value ?: return
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_notebook_seed_${user.id}", Context.MODE_PRIVATE)
        if (prefs.getBoolean("seeded", false)) return
        if (allNotebooks.value.isEmpty()) {
            prefs.edit().putBoolean("seeded", true).apply()
            viewModelScope.launch(Dispatchers.IO) {
                val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                val nb1 = NotebookEntity(
                    id = "nb-default-1",
                    userId = user.id,
                    title = "My Notebook",
                    coverPreset = "preset_1",
                    coverColor = "#FF9800",
                    createdAt = now,
                    updatedAt = now
                )
                val nb2 = NotebookEntity(
                    id = "nb-default-2",
                    userId = user.id,
                    title = "Notes",
                    coverPreset = "preset_2",
                    coverColor = "#E91E63",
                    createdAt = now,
                    updatedAt = now
                )
                repository.insertNotebook(nb1)
                repository.insertNotebook(nb2)

                // Add initial sample notes into "My Notebook" matching Screenshot 4
                val note1 = NoteEntity(
                    id = "note-default-1",
                    notebookId = nb1.id,
                    userId = user.id,
                    title = "Uujnbbb",
                    content = "This is your first rich text note! Tap to edit and add more content.",
                    cardColor = "#FFF59D", // yellow
                    createdAt = now,
                    updatedAt = now
                )
                val note2 = NoteEntity(
                    id = "note-default-2",
                    notebookId = nb1.id,
                    userId = user.id,
                    title = "Yjjjjj",
                    content = "Quick thoughts and ideas saved in your notebook.",
                    cardColor = "#FFCC80", // orange
                    createdAt = now,
                    updatedAt = now
                )
                repository.insertNote(note1)
                repository.insertNote(note2)
            }
        }
    }

    // --- Computed Score Stats ---
    private val corePointsFlow: Flow<Int> = combine(
        allTasks,
        allHabits,
        allWishlist,
        allGroceryItems,
        allBirthdays
    ) { tasks, habits, wishlist, groceries, birthdays ->
        val todayStr = TrackWiseUtils.getTodayString()
        if (todayStr < TrackWiseUtils.APP_LAUNCH_DATE) return@combine 0
        
        // 1. Dynamic Task Points (High: 50 pts, Medium: 30 pts, Low: 15 pts + early/late proportional bonus)
        val taskPoints = tasks.filter { TrackWiseUtils.shouldShowTaskOnDate(it, todayStr) && it.completed }.sumOf { 
            val base = when (it.priority.lowercase()) {
                "high" -> 50
                "medium" -> 30
                else -> 15
            }
            val daysDiff = try {
                val dDate = TrackWiseUtils.parseDate(it.deadline)
                val tDate = TrackWiseUtils.parseDate(todayStr)
                val diffMs = dDate.time - tDate.time
                (diffMs / (1000 * 60 * 60 * 24)).toInt()
            } catch (e: Exception) {
                0
            }
            if (daysDiff >= 0) {
                // Completed on time or early! Reward them based on how soon they did it
                val earlyBonus = (daysDiff * 5).coerceAtMost(30)
                base + 10 + earlyBonus
            } else {
                // Completed late. Scale reward down depending on how late they were
                val penaltyFactor = 1f / (1f + (-daysDiff) * 0.15f)
                val scaledPoints = (base * 0.5f) + (base * 0.5f * penaltyFactor)
                scaledPoints.toInt()
            }
        }
        
        // 2. Dynamic Habit Points: 10 pts per completion count, plus streak/early proportional bonuses
        val habitPoints = habits.sumOf { habit ->
            val days = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson)
            val completionsToday = days.count { it == todayStr }
            if (completionsToday > 0) {
                val base = if (habit.isMultipleTimesPerDay) {
                    val compBase = completionsToday * 10
                    val compBonus = if (completionsToday >= habit.multipleTimesTarget) 15 else 0
                    compBase + compBonus
                } else {
                    20
                }
                // Consistency/Schedule streak bonus: reward them more if they kept the consistency schedule up soon
                val streakBonus = (habit.streak * 2).coerceAtMost(20)
                // Early-Bird/Scheduling Reward: completed early in the day (before noon)
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val earlyBirdBonus = if (hour < 12) 10 else 0
                
                base + streakBonus + earlyBirdBonus
            } else {
                0
            }
        }

        // 3. Dynamic Wishlist Points: 100 points + soon purchase proportional rewards
        val wishPoints = wishlist.filter { it.purchased }.sumOf { item ->
            val base = 100
            if (!item.reminderDate.isNullOrBlank()) {
                val daysDiff = try {
                    val rDate = TrackWiseUtils.parseDate(item.reminderDate)
                    val tDate = TrackWiseUtils.parseDate(todayStr)
                    val diffMs = rDate.time - tDate.time
                    (diffMs / (1000 * 60 * 60 * 24)).toInt()
                } catch (e: Exception) {
                    0
                }
                if (daysDiff >= 0) {
                    // Purchased on/before the reminder!
                    val soonBonus = (daysDiff * 3).coerceAtMost(40)
                    base + 20 + soonBonus
                } else {
                    // Purchased late relative to reminder. Scale the remaining 50 XP
                    val penaltyFactor = 1f / (1f + (-daysDiff) * 0.1f)
                    (50 + (50 * penaltyFactor)).toInt()
                }
            } else {
                base
            }
        }

        // 4. Dynamic Grocery Points: 10 points for each completed grocery item
        val groceryPoints = groceries.filter { it.completed }.size * 10

        // 5. Dynamic Birthday Points: 150 points if celebrating a birthday today
        val todayMMDD = todayStr.substring(5) // from YYYY-MM-DD to MM-DD
        val birthdayPoints = birthdays.filter { 
            it.date.endsWith(todayMMDD) 
        }.size * 150

        taskPoints + habitPoints + wishPoints + groceryPoints + birthdayPoints
    }

    val todayScore: StateFlow<Int> = combine(
        corePointsFlow,
        allFinanceLogs,
        sleepLogs
    ) { corePoints, finance, sleep ->
        val todayStr = TrackWiseUtils.getTodayString()
        if (todayStr < TrackWiseUtils.APP_LAUNCH_DATE) return@combine 0

        // 6. Dynamic Sleep Logging Points: 40 points if sleep is logged for today
        val sleepPoints = sleep.filter { it.date == todayStr }.size * 40

        // 7. Dynamic Finance Logging Points: 25 points for each transaction logged today
        val financePoints = finance.filter { it.date == todayStr }.size * 25
        
        val total = corePoints + sleepPoints + financePoints
        
        // Auto-save this updated score to Streak History in database background
        _sessionUser.value?.let { user ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateStreakHistory(user.id, todayStr, total)
            }
        }
        
        total
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- Authentication Actions ---
    fun login(email: String, passwordRaw: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _authError.value = null
            try {
                val user = repository.login(email, passwordRaw)
                _sessionUser.value = user
                _themeMode.value = "light" // Default theme
                
                // Persist session
                val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
                prefs.edit().putString("saved_user_id", user.id).putLong("last_active_timestamp", System.currentTimeMillis()).apply()
            } catch (e: Exception) {
                _authError.value = e.message ?: "Authentication failed."
            }
        }
    }

    fun signUp(email: String, passwordRaw: String, fullName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _authError.value = null
            try {
                val user = repository.signUp(email, passwordRaw, fullName)
                _sessionUser.value = user
                
                // Persist session
                val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
                prefs.edit().putString("saved_user_id", user.id).putLong("last_active_timestamp", System.currentTimeMillis()).apply()
                
                addNotification("Account Created", "Welcome to TrackWise, ${user.fullName}!")
            } catch (e: Exception) {
                _authError.value = e.message ?: "Account creation failed."
            }
        }
    }

    val allUsers: StateFlow<List<com.example.data.UserEntity>> = repository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun switchAccount(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.findUserById(userId)
            if (user != null) {
                _sessionUser.value = user
                val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
                prefs.edit().putString("saved_user_id", user.id).putLong("last_active_timestamp", System.currentTimeMillis()).apply()
                addNotification("Account Switched", "Switched to account: ${user.fullName}")
            }
        }
    }

    fun logout() {
        val user = _sessionUser.value
        _sessionUser.value = null
        _settingsPanelOpen.value = false
        
        // Clear persisted session
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        prefs.edit().remove("saved_user_id").apply()
        
        if (user != null) {
            addNotification("User Logged Out", "Logged out from account ${user.fullName}.")
        }
    }

    fun resetPassword(email: String, newPasswordRaw: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _authError.value = null
            _successMessage.value = null
            try {
                val user = repository.findUserByEmail(email)
                if (user == null) {
                    _authError.value = "Email address not found."
                } else {
                    val hashed = SecurityUtils.hashPassword(newPasswordRaw)
                    val updated = user.copy(passwordHash = hashed)
                    repository.updateUserProfile(updated)
                    _successMessage.value = "Password reset successfully! Please log in."
                    addNotification("Security Alert", "Password was reset for $email.", showSystem = true)
                }
            } catch (e: Exception) {
                _authError.value = e.message ?: "Failed to reset password."
            }
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun dismissSuccessMessage() {
        _successMessage.value = null
    }

    fun showSuccessMessage(message: String) {
        _successMessage.value = message
    }

    fun setAppFontSize(size: String) {
        _appFontSize.value = size
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        prefs.edit().putString("app_font_size", size).apply()
    }

    fun setAppFontStyle(style: String) {
        _appFontStyle.value = style
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        prefs.edit().putString("app_font_style", style).apply()
    }

    fun changePassword(currentPasswordRaw: String, newPasswordRaw: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = _sessionUser.value
        if (user == null) {
            onError("No user logged in.")
            return
        }
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val isSuccess = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    val hashedCurrent = SecurityUtils.hashPassword(currentPasswordRaw)
                    if (user.passwordHash != hashedCurrent) {
                        false
                    } else {
                        val newHashed = SecurityUtils.hashPassword(newPasswordRaw)
                        val updated = user.copy(passwordHash = newHashed)
                        repository.updateUserProfile(updated)
                        _sessionUser.value = updated
                        true
                    }
                }
                if (isSuccess) {
                    _successMessage.value = "Password changed successfully!"
                    addNotification("Security Update", "Your password has been changed successfully.", showSystem = true)
                    onSuccess()
                } else {
                    onError("Current password is incorrect.")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to change password.")
            }
        }
    }

    // --- Preferences Actions ---
    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        prefs.edit().putString("saved_theme_mode", mode).apply()
        updateAppWidget()
    }

    fun setProfileImageUri(uri: String?) {
        _profileImageUri.value = uri
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        prefs.edit().putString("profile_image_uri", uri).apply()
    }

    fun setCalendarOverlay(overlay: String) {
        _calendarOverlay.value = overlay
    }

    fun setSettingsPanelOpen(isOpen: Boolean) {
        _settingsPanelOpen.value = isOpen
    }

    fun setSelectedTaskFolder(folder: String?) {
        _selectedTaskFolder.value = folder
        _selectedTaskTag.value = null
    }

    fun setSelectedTaskTag(tag: String?) {
        _selectedTaskTag.value = tag
        _selectedTaskFolder.value = null
    }

    // --- User Profile Edit ---
    fun saveProfile(
        fullName: String,
        dob: String?,
        gender: String?,
        heightCm: Double?,
        weightKg: Double?,
        phone: String?,
        addressLine1: String?,
        addressLine2: String?,
        city: String?,
        state: String?,
        zipCode: String?,
        bloodType: String?,
        waterGoal: Int,
        conditions: String
    ) {
        val currentUser = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedUser = currentUser.copy(
                fullName = fullName,
                dob = dob,
                gender = gender,
                heightCm = heightCm,
                weightKg = weightKg,
                phone = phone,
                addressLine1 = addressLine1,
                addressLine2 = addressLine2,
                city = city,
                state = state,
                zipCode = zipCode,
                bloodType = bloodType,
                waterGoalGlasses = waterGoal,
                enabledConditions = conditions
            )
            repository.updateUserProfile(updatedUser)
            _sessionUser.value = updatedUser
            _successMessage.value = "Profile saved successfully."
            triggerFakeSync()
        }
    }

    fun completeOnboarding(
        firstName: String,
        lastName: String,
        phone: String,
        gender: String,
        religion: String,
        dob: String
    ) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val fullNameStr = "$firstName $lastName".trim()
            val updatedUser = user.copy(
                fullName = fullNameStr,
                dob = dob,
                gender = gender,
                phone = phone,
                religion = religion
            )
            repository.updateUserProfile(updatedUser)
            _sessionUser.value = updatedUser

            // Detailed form update (UserProfileEntity)
            val existingProfile = repository.getUserProfile(user.id) ?: UserProfileEntity(userId = user.id)
            val updatedProfile = existingProfile.copy(
                firstName = firstName,
                lastName = lastName,
                dob = dob,
                gender = gender,
                mobileNumber = phone,
                emailAddress = user.email,
                religion = religion
            )
            repository.insertUserProfile(updatedProfile)

            _successMessage.value = "Onboarding completed successfully!"
            triggerFakeSync()
        }
    }

    // --- Tasks Actions ---
    // Task edit sheet integration
    private val _taskToEdit = MutableStateFlow<TaskEntity?>(null)
    val taskToEdit: StateFlow<TaskEntity?> = _taskToEdit.asStateFlow()

    private val _showCustomTaskSheet = MutableStateFlow(false)
    val showCustomTaskSheet: StateFlow<Boolean> = _showCustomTaskSheet.asStateFlow()

    private val _showHabitCreationSheet = MutableStateFlow(false)
    val showHabitCreationSheet: StateFlow<Boolean> = _showHabitCreationSheet.asStateFlow()

    private val _showAddFinanceSheet = MutableStateFlow(false)
    val showAddFinanceSheet: StateFlow<Boolean> = _showAddFinanceSheet.asStateFlow()

    private val _showNetWorthAddSheet = MutableStateFlow(false)
    val showNetWorthAddSheet: StateFlow<Boolean> = _showNetWorthAddSheet.asStateFlow()

    private val _netWorthPresetType = MutableStateFlow("asset")
    val netWorthPresetType: StateFlow<String> = _netWorthPresetType.asStateFlow()

    fun openNetWorthAddSheet(type: String = "asset") {
        _netWorthPresetType.value = type
        _showNetWorthAddSheet.value = true
    }

    fun closeNetWorthAddSheet() {
        _showNetWorthAddSheet.value = false
    }

    fun openAddFinanceSheet() {
        _showAddFinanceSheet.value = true
    }

    fun closeAddFinanceSheet() {
        _showAddFinanceSheet.value = false
    }

    fun openHabitCreationSheet() {
        _habitToEdit.value = null
        _showHabitCreationSheet.value = true
    }

    fun closeHabitCreationSheet() {
        _showHabitCreationSheet.value = false
        _habitToEdit.value = null
    }

    fun openAddTaskSheet() {
        _taskToEdit.value = null
        _showCustomTaskSheet.value = true
    }

    fun openEditTaskSheet(task: TaskEntity) {
        _taskToEdit.value = task
        _showCustomTaskSheet.value = true
    }

    fun closeCustomTaskSheet() {
        _showCustomTaskSheet.value = false
        _taskToEdit.value = null
    }

    fun addTask(
        title: String,
        description: String,
        project: String,
        priority: String,
        points: Int,
        deadline: String,
        reminderTime: String?,
        repeatType: String = "none",
        customRepeatValue: Int = 1,
        customRepeatUnit: String = "days",
        customRepeatDaysOfWeek: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        notes: String = "",
        dueTime: String? = null,
        reminderDate: String? = null,
        subtasksJson: String = "[]"
    ) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val taskPoints = when (priority.lowercase()) {
                "high" -> 50
                "medium" -> 30
                else -> 15
            }
            val task = TaskEntity(
                id = "task-${System.currentTimeMillis()}",
                userId = user.id,
                title = title,
                description = description,
                project = project,
                priority = priority,
                deadline = deadline,
                completed = false,
                points = taskPoints,
                subtasksJson = subtasksJson,
                reminderTime = reminderTime,
                repeatType = repeatType,
                customRepeatValue = customRepeatValue,
                customRepeatUnit = customRepeatUnit,
                customRepeatDaysOfWeek = customRepeatDaysOfWeek,
                startDate = startDate,
                endDate = endDate,
                notes = notes,
                dueTime = dueTime,
                remindMe = reminderTime != null,
                reminderDate = reminderDate ?: (if (reminderTime != null) deadline else null)
            )
            repository.insertTask(task)
            triggerFakeSync()
        }
    }

    private fun autoDismissNotification(itemId: String, itemTitle: String? = null) {
        try {
            val context = getApplication<Application>().applicationContext
            val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val activeNotifs = notifManager.activeNotifications
                for (sbn in activeNotifs) {
                    val extras = sbn.notification.extras
                    val notifTaskId = extras.getString("task_id")
                    val notifHabitId = extras.getString("habit_id")
                    val notifTabletId = extras.getString("tablet_id")
                    val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
                    val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
                    
                    val matchesId = (notifTaskId == itemId || notifHabitId == itemId || notifTabletId == itemId)
                    val matchesTitle = itemTitle != null && itemTitle.isNotBlank() && (
                        title.contains(itemTitle, ignoreCase = true) || text.contains(itemTitle, ignoreCase = true)
                    )
                    
                    if (matchesId || matchesTitle) {
                        if (sbn.tag != null) {
                            notifManager.cancel(sbn.tag, sbn.id)
                        } else {
                            notifManager.cancel(sbn.id)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = task.copy(completed = !task.completed)
            repository.insertTask(updated)
            if (updated.completed) {
                playTaskCompletionSound()
                autoDismissNotification(task.id, task.title)
            }
            val newState = UndoActionState(
                type = "task",
                id = task.id,
                title = task.title,
                isCompleted = updated.completed,
                originalTask = task
            )
            _undoActionState.value = newState
            viewModelScope.launch {
                delay(5000)
                if (_undoActionState.value?.timestamp == newState.timestamp) {
                    _undoActionState.value = null
                }
            }
            triggerFakeSync()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTask(taskId)
            triggerFakeSync()
        }
    }

    fun addSubTask(task: TaskEntity, subTitle: String, dueDate: String? = null, dueTime: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSubTasks = TrackWiseUtils.deserializeSubTasks(task.subtasksJson).toMutableList()
            val newSub = SubTask(
                id = "sub-${System.currentTimeMillis()}",
                title = subTitle,
                completed = false,
                dueDate = dueDate,
                dueTime = dueTime
            )
            currentSubTasks.add(newSub)
            val updatedTask = task.copy(subtasksJson = TrackWiseUtils.serializeSubTasks(currentSubTasks))
            repository.insertTask(updatedTask)
            triggerFakeSync()
        }
    }

    fun toggleSubTask(task: TaskEntity, subId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            var playedSound = false
            val currentSubTasks = TrackWiseUtils.deserializeSubTasks(task.subtasksJson).map {
                if (it.id == subId) {
                    val nextVal = !it.completed
                    if (nextVal) playedSound = true
                    it.copy(completed = nextVal)
                } else it
            }
            val updatedTask = task.copy(subtasksJson = TrackWiseUtils.serializeSubTasks(currentSubTasks))
            repository.insertTask(updatedTask)
            if (playedSound) {
                playTaskCompletionSound()
            }
            triggerFakeSync()
        }
    }

    fun deleteSubTask(task: TaskEntity, subId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSubTasks = TrackWiseUtils.deserializeSubTasks(task.subtasksJson).filter { it.id != subId }
            val updatedTask = task.copy(subtasksJson = TrackWiseUtils.serializeSubTasks(currentSubTasks))
            repository.insertTask(updatedTask)
            triggerFakeSync()
        }
    }

    // --- Habits Actions ---
    fun addHabit(
        name: String,
        category: String,
        isMultipleTimesPerDay: Boolean = false,
        multipleTimesTarget: Int = 1,
        isTimeBound: Boolean = false,
        timeBoundDuration: String? = null,
        repeatType: String = "none",
        customRepeatValue: Int = 1,
        customRepeatUnit: String = "days",
        customRepeatDaysOfWeek: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        remindMe: Boolean = false,
        reminderDate: String? = null,
        reminderTime: String? = null,
        dueTime: String? = null,
        icon: String = "😊",
        quote: String = "",
        goalType: String = "Achieve it all",
        goalDays: String = "Forever",
        section: String = "Others",
        autoPopup: Boolean = false
    ) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val today = TrackWiseUtils.getTodayString()
            val finalStart = startDate ?: today
            val bgOptions = listOf("window", "fitness", "mindfulness", "study", "finance", "nature", "creativity", "rest", "nutrition")
            val selectedBg = when (category.lowercase()) {
                "fitness", "sports", "exercise" -> "fitness"
                "health", "nutrition", "diet" -> "nutrition"
                "mindfulness", "meditation" -> "mindfulness"
                "study", "learning", "education" -> "study"
                "finance", "money" -> "finance"
                "art", "creativity" -> "creativity"
                "sleep", "rest" -> "rest"
                "nature" -> "nature"
                else -> bgOptions.random()
            }
            val cleanSection = if (section.contains(",")) {
                val parts = section.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val filtered = parts.filter { !it.equals("Others", ignoreCase = true) }
                if (filtered.isNotEmpty()) filtered.joinToString(",") else section
            } else section

            val habit = HabitEntity(
                id = "habit-${System.currentTimeMillis()}",
                userId = user.id,
                name = name,
                category = category,
                createdAt = finalStart,
                isMultipleTimesPerDay = isMultipleTimesPerDay,
                multipleTimesTarget = multipleTimesTarget,
                isTimeBound = isTimeBound,
                timeBoundDuration = timeBoundDuration,
                repeatType = repeatType,
                customRepeatValue = customRepeatValue,
                customRepeatUnit = customRepeatUnit,
                customRepeatDaysOfWeek = customRepeatDaysOfWeek,
                startDate = finalStart,
                endDate = endDate,
                remindMe = remindMe,
                reminderDate = reminderDate,
                reminderTime = reminderTime,
                dueTime = dueTime,
                icon = icon,
                quote = quote,
                goalType = goalType,
                goalDays = goalDays,
                section = cleanSection,
                autoPopup = autoPopup,
                backgroundImage = selectedBg
            )
            repository.insertHabit(habit)
            if (remindMe && !reminderTime.isNullOrBlank()) {
                try {
                    val context = getApplication<android.app.Application>().applicationContext
                    val notifiedPrefs = context.getSharedPreferences("notified_reminders", android.content.Context.MODE_PRIVATE)
                    val todayStr = TrackWiseUtils.getTodayString()
                    val rDate = reminderDate?.take(10) ?: todayStr
                    val rTime24 = com.example.receiver.ReminderReceiver.parseTo24HourTime(reminderTime)
                    val currentTimeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())
                    if (rDate == todayStr && rTime24 != null && rTime24 <= currentTimeStr) {
                        val key = "habit-${habit.id}-$rDate-$rTime24"
                        notifiedPrefs.edit().putBoolean(key, true).apply()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            triggerFakeSync()
        }
    }

    fun toggleHabitToday(habit: HabitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val todayStr = TrackWiseUtils.getTodayString()
            if (todayStr < TrackWiseUtils.APP_LAUNCH_DATE) return@launch
            
            val days = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson).toMutableList()
            if (days.contains(todayStr)) {
                days.remove(todayStr)
            } else {
                days.add(todayStr)
                playTaskCompletionSound()
                autoDismissNotification(habit.id, habit.name)
                if (habit.name.equals("Early to rise", ignoreCase = true)) {
                    _showEarlyToRiseSleepDialog.value = true
                }
                val cat = habit.category.lowercase()
                val name = habit.name.lowercase()
                val sec = habit.section.lowercase()
                if (cat.contains("sport") || cat.contains("fitness") || cat.contains("workout") || cat.contains("exercise") ||
                    sec.contains("sport") || sec.contains("fitness") ||
                    name.contains("workout") || name.contains("step") || name.contains("stretch") || name.contains("cycl") ||
                    name.contains("run") || name.contains("gym") || name.contains("sport") || name.contains("exercise")) {
                    _sportHabitCompletedName.value = habit.name
                    _showSportExerciseDialog.value = true
                }
            }
            
            // Recalculate streak
            val sortedDays = days.sortedDescending()
            var currentStreak = 0
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            
            // Strictly check daily streaks from today backward
            if (days.contains(todayStr)) {
                currentStreak = 1
                var checkDate = Calendar.getInstance()
                checkDate.time = sdf.parse(todayStr) ?: Date()
                
                while (true) {
                    checkDate.add(Calendar.DAY_OF_YEAR, -1)
                    val prevStr = sdf.format(checkDate.time)
                    if (prevStr < TrackWiseUtils.APP_LAUNCH_DATE) break
                    if (days.contains(prevStr)) {
                        currentStreak++
                    } else {
                        break
                    }
                }
            }
            
            val maxStreak = max(habit.maxStreak, currentStreak)
            
            // Check & sync milestone badges
            val currentBadges = TrackWiseUtils.deserializeIntList(habit.badgesEarnedJson).toMutableList()
            val milestones = listOf(1, 3, 5, 7, 14, 21, 30, 45, 60, 90, 100, 365)
            milestones.forEach { milestone ->
                if (maxStreak >= milestone && !currentBadges.contains(milestone)) {
                    currentBadges.add(milestone)
                }
            }
            
            val updated = habit.copy(
                daysCompletedJson = TrackWiseUtils.serializeStringList(days),
                streak = currentStreak,
                maxStreak = maxStreak,
                badgesEarnedJson = TrackWiseUtils.serializeIntList(currentBadges)
            )
            
            repository.insertHabit(updated)
            val newState = UndoActionState(
                type = "habit",
                id = habit.id,
                title = habit.name,
                isCompleted = days.contains(todayStr),
                originalHabit = habit
            )
            _undoActionState.value = newState
            viewModelScope.launch {
                delay(5000)
                if (_undoActionState.value?.timestamp == newState.timestamp) {
                    _undoActionState.value = null
                }
            }
            triggerFakeSync()
        }
    }

    fun incrementHabitToday(habit: HabitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val todayStr = TrackWiseUtils.getTodayString()
            if (todayStr < TrackWiseUtils.APP_LAUNCH_DATE) return@launch
            
            val days = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson).toMutableList()
            days.add(todayStr)
            if (habit.name.equals("Early to rise", ignoreCase = true)) {
                _showEarlyToRiseSleepDialog.value = true
            }
            val cat = habit.category.lowercase()
            val name = habit.name.lowercase()
            val sec = habit.section.lowercase()
            if (cat.contains("sport") || cat.contains("fitness") || cat.contains("workout") || cat.contains("exercise") ||
                sec.contains("sport") || sec.contains("fitness") ||
                name.contains("workout") || name.contains("step") || name.contains("stretch") || name.contains("cycl") ||
                name.contains("run") || name.contains("gym") || name.contains("sport") || name.contains("exercise")) {
                _sportHabitCompletedName.value = habit.name
                _showSportExerciseDialog.value = true
            }
            
            // Recalculate streak
            var currentStreak = 0
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            if (days.contains(todayStr)) {
                currentStreak = 1
                var checkDate = Calendar.getInstance()
                checkDate.time = sdf.parse(todayStr) ?: Date()
                
                while (true) {
                    checkDate.add(Calendar.DAY_OF_YEAR, -1)
                    val prevStr = sdf.format(checkDate.time)
                    if (prevStr < TrackWiseUtils.APP_LAUNCH_DATE) break
                    if (days.contains(prevStr)) {
                        currentStreak++
                    } else {
                        break
                    }
                }
            }
            
            val maxStreak = max(habit.maxStreak, currentStreak)
            val currentBadges = TrackWiseUtils.deserializeIntList(habit.badgesEarnedJson).toMutableList()
            val milestones = listOf(1, 3, 5, 7, 14, 21, 30, 45, 60, 90, 100, 365)
            milestones.forEach { milestone ->
                if (maxStreak >= milestone && !currentBadges.contains(milestone)) {
                    currentBadges.add(milestone)
                }
            }
            
            val updated = habit.copy(
                daysCompletedJson = TrackWiseUtils.serializeStringList(days),
                streak = currentStreak,
                maxStreak = maxStreak,
                badgesEarnedJson = TrackWiseUtils.serializeIntList(currentBadges)
            )
            repository.insertHabit(updated)
            triggerFakeSync()
        }
    }

    fun decrementHabitToday(habit: HabitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val todayStr = TrackWiseUtils.getTodayString()
            if (todayStr < TrackWiseUtils.APP_LAUNCH_DATE) return@launch
            
            val days = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson).toMutableList()
            if (days.contains(todayStr)) {
                days.remove(todayStr)
            }
            
            // Recalculate streak
            var currentStreak = 0
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            if (days.contains(todayStr)) {
                currentStreak = 1
                var checkDate = Calendar.getInstance()
                checkDate.time = sdf.parse(todayStr) ?: Date()
                
                while (true) {
                    checkDate.add(Calendar.DAY_OF_YEAR, -1)
                    val prevStr = sdf.format(checkDate.time)
                    if (prevStr < TrackWiseUtils.APP_LAUNCH_DATE) break
                    if (days.contains(prevStr)) {
                        currentStreak++
                    } else {
                        break
                    }
                }
            }
            
            val maxStreak = max(habit.maxStreak, currentStreak)
            val currentBadges = TrackWiseUtils.deserializeIntList(habit.badgesEarnedJson).toMutableList()
            val milestones = listOf(1, 3, 5, 7, 14, 21, 30, 45, 60, 90, 100, 365)
            milestones.forEach { milestone ->
                if (maxStreak >= milestone && !currentBadges.contains(milestone)) {
                    currentBadges.add(milestone)
                }
            }
            
            val updated = habit.copy(
                daysCompletedJson = TrackWiseUtils.serializeStringList(days),
                streak = currentStreak,
                maxStreak = maxStreak,
                badgesEarnedJson = TrackWiseUtils.serializeIntList(currentBadges)
            )
            repository.insertHabit(updated)
            triggerFakeSync()
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHabit(habitId)
            triggerFakeSync()
        }
    }

    // --- Birthdays Actions ---
    private fun formatOccasionName(name: String, category: String): String {
        val baseCategory = category.split("|")[0]
        var cleaned = name.trim()
        if (baseCategory.equals("Countdown", ignoreCase = true) || baseCategory.equals("Holiday", ignoreCase = true)) {
            val suffix = if (baseCategory.equals("Countdown", ignoreCase = true)) "Countdown" else "Holiday"
            if (cleaned.endsWith(suffix, ignoreCase = true)) {
                return cleaned
            }
            return "$cleaned $suffix"
        }
        val suffixes = listOf(
            "'s Birthday", "' Birthday", " Birthday",
            "'s Marriage Anniversary", "' Marriage Anniversary", " Marriage Anniversary",
            "'s Death Anniversary", "' Death Anniversary", " Death Anniversary"
        )
        for (suffix in suffixes) {
            if (cleaned.endsWith(suffix, ignoreCase = true)) {
                cleaned = cleaned.substring(0, cleaned.length - suffix.length).trim()
            }
        }
        if (cleaned.endsWith("'s", ignoreCase = true)) {
            cleaned = cleaned.substring(0, cleaned.length - 2).trim()
        } else if (cleaned.endsWith("'", ignoreCase = true)) {
            cleaned = cleaned.substring(0, cleaned.length - 1).trim()
        }
        
        val suffix = when (baseCategory) {
            "Marriage Anniversary" -> "Marriage Anniversary"
            "Death Anniversary" -> "Death Anniversary"
            else -> "Birthday"
        }
        return if (cleaned.endsWith("s", ignoreCase = true)) {
            "$cleaned' $suffix"
        } else {
            "$cleaned's $suffix"
        }
    }

    fun addBirthday(
        name: String,
        date: String,
        giftIdea: String?,
        category: String = "Others",
        remindMe: Boolean = false,
        reminderDate: String? = null,
        reminderTime: String? = null,
        customBgImage: String? = null,
        customTextColor: String? = null,
        customFontStyle: String? = null,
        reminderOptions: String? = null,
        repeatPattern: String? = null,
        countingMode: String? = null
    ) {
        val user = _sessionUser.value ?: return
        val finalName = formatOccasionName(name, category)
        viewModelScope.launch(Dispatchers.IO) {
            val birthday = BirthdayEntity(
                id = "birthday-${System.currentTimeMillis()}",
                userId = user.id,
                name = finalName,
                date = date,
                giftIdea = giftIdea,
                category = category,
                remindMe = remindMe,
                reminderDate = reminderDate,
                reminderTime = reminderTime,
                isPinned = false,
                customBgImage = customBgImage,
                customTextColor = customTextColor,
                customFontStyle = customFontStyle,
                reminderOptions = reminderOptions,
                repeatPattern = repeatPattern,
                countingMode = countingMode
            )
            repository.insertBirthday(birthday)
            triggerFakeSync()
        }
    }

    fun deleteBirthday(birthdayId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBirthday(birthdayId)
            triggerFakeSync()
        }
    }

    fun updateBirthday(birthday: BirthdayEntity) {
        val formattedName = formatOccasionName(birthday.name, birthday.category)
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertBirthday(birthday.copy(name = formattedName))
            triggerFakeSync()
        }
    }

    fun loadCustomUserBirthdays() {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.clearBirthdaysForUser(user.id)
                val list = listOf(
                    Triple("Imran", "1988-07-07", "Family"),
                    Triple("Ashraf", "1993-07-14", "Family"),
                    Triple("Ayesha", "1983-07-31", "Family"),
                    Triple("Irfan", "1977-08-07", "Family"),
                    Triple("Rizwan", "1977-08-07", "Family"),
                    Triple("Homay", "1997-08-08", "Relative"),
                    Triple("Kashee", "1981-08-14", "Family"),
                    Triple("Sajjad", "08-17", "Relative"),
                    Triple("Athaul", "09-02", "Family"),
                    Triple("Asif Al", "1994-09-12", "Family"),
                    Triple("Saquib", "09-17", "Family"),
                    Triple("Zubair", "2009-09-18", "Family"),
                    Triple("Zeba", "2012-09-23", "Family"),
                    Triple("Triveni", "1997-10-08", "Relative"),
                    Triple("Ilyas", "2004-10-13", "Family"),
                    Triple("Shaba", "2001-10-14", "Family"),
                    Triple("Fazil", "1995-10-15", "Family"),
                    Triple("Vajee", "2008-10-15", "Family"),
                    Triple("Nasee", "2002-10-22", "Family"),
                    Triple("Roush", "2005-10-28", "Family"),
                    Triple("Thilak", "1997-11-01", "Relative"),
                    Triple("Sufiya", "11-27", "Family"),
                    Triple("Shabe", "1969-12-04", "Family"),
                    Triple("Masta", "1970-01-01", "Family"),
                    Triple("Faree", "1979-01-01", "Family"),
                    Triple("Moha", "1968-01-01", "Family"),
                    Triple("Triven", "01-04", "Relative"),
                    Triple("Saqui", "2013-01-04", "Family"),
                    Triple("Tahir", "01-21", "Family"),
                    Triple("Ziya", "1971-02-05", "Family"),
                    Triple("Shaik", "02-19", "Relative"),
                    Triple("Fahad", "02-23", "Relative"),
                    Triple("Rayan", "03-04", "Family"),
                    Triple("Janve", "03-20", "Relative"),
                    Triple("Janve", "1951-04-05", "Family"),
                    Triple("Fathi", "04-19", "Family"),
                    Triple("Akram", "1998-04-21", "Family"),
                    Triple("Sabe'e", "1998-05-04", "Family"),
                    Triple("Rahm", "1967-05-23", "Family"),
                    Triple("Shake", "1998-05-23", "Family"),
                    Triple("Parija", "1996-05-29", "Relative"),
                    Triple("Noor", "1975-06-02", "Family"),
                    Triple("Jurai", "1997-07-02", "Family"),
                    Triple("Nazee", "2004-07-02", "Family")
                )
                
                list.forEachIndexed { index, (name, date, rel) ->
                    val finalName = formatOccasionName(name, "Birthday|$rel")
                    val entity = BirthdayEntity(
                        id = "birthday-custom-$index-${System.currentTimeMillis()}",
                        userId = user.id,
                        name = finalName,
                        date = date,
                        giftIdea = null,
                        category = "Birthday|$rel"
                    )
                    repository.insertBirthday(entity)
                }
                
                viewModelScope.launch(Dispatchers.Main) {
                    _successMessage.value = "All existing birthdays removed & your 44 custom birthdays loaded!"
                }
                triggerFakeSync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Wishlist Actions ---
    fun addWishItem(
        title: String,
        price: Double,
        link: String?,
        priority: String,
        remindMe: Boolean = false,
        reminderDate: String? = null,
        reminderTime: String? = null
    ) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val item = WishItemEntity(
                id = "wish-${System.currentTimeMillis()}",
                userId = user.id,
                title = title,
                price = price,
                link = link,
                priority = priority,
                purchased = false,
                remindMe = remindMe,
                reminderDate = reminderDate,
                reminderTime = reminderTime
            )
            repository.insertWishItem(item)
            triggerFakeSync()
        }
    }

    fun toggleWishPurchased(item: WishItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = item.copy(purchased = !item.purchased)
            repository.insertWishItem(updated)
            if (updated.purchased) {
                playTaskCompletionSound()
                autoDismissNotification(item.id, item.title)
            }
            triggerFakeSync()
        }
    }

    fun deleteWishItem(itemId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWishItem(itemId)
            triggerFakeSync()
        }
    }

    // --- Grocery List Actions ---
    fun addGroceryItem(
        name: String,
        quantity: String,
        category: String,
        price: Double? = null,
        priceUnit: String? = null,
        numericQuantity: Double? = null
    ) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val item = GroceryItemEntity(
                id = "grocery-${System.currentTimeMillis()}-${(1000..9999).random()}",
                userId = user.id,
                name = name,
                quantity = quantity,
                completed = false,
                category = category,
                price = price,
                priceUnit = priceUnit,
                numericQuantity = numericQuantity
            )
            repository.insertGroceryItem(item)
            triggerFakeSync()
        }
    }

    fun toggleGroceryItem(item: GroceryItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = item.copy(completed = !item.completed)
            repository.insertGroceryItem(updated)
            if (updated.completed) {
                playTaskCompletionSound()
            }
            triggerFakeSync()
        }
    }

    fun deleteGroceryItem(itemId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGroceryItem(itemId)
            triggerFakeSync()
        }
    }

    fun clearCompletedGroceries() {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearCompletedGroceryItems(user.id)
            triggerFakeSync()
        }
    }

    // --- Tablet Reminder Actions ---
    fun addTabletReminder(tabletName: String, dosage: String, timeOfDay: String, scheduleType: String, notes: String?) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val reminder = TabletReminderEntity(
                id = "tablet-${System.currentTimeMillis()}-${(1000..9999).random()}",
                userId = user.id,
                tabletName = tabletName,
                dosage = dosage,
                timeOfDay = timeOfDay,
                scheduleType = scheduleType,
                notes = notes
            )
            repository.insertTabletReminder(reminder)
            triggerFakeSync()
        }
    }

    fun toggleTabletTaken(reminder: TabletReminderEntity, dateStr: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Deserialize JSON to list
            val list = try {
                val array = org.json.JSONArray(reminder.completedDatesJson)
                val res = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    res.add(array.getString(i))
                }
                res
            } catch (e: Exception) {
                mutableListOf()
            }

            if (list.contains(dateStr)) {
                list.remove(dateStr)
            } else {
                list.add(dateStr)
                playTaskCompletionSound()
                autoDismissNotification(reminder.id, reminder.tabletName)
            }

            // Serialize back
            val jsonArray = org.json.JSONArray()
            list.forEach { jsonArray.put(it) }

            val updated = reminder.copy(completedDatesJson = jsonArray.toString())
            repository.insertTabletReminder(updated)
            triggerFakeSync()
        }
    }

    fun deleteTabletReminder(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTabletReminder(id)
            triggerFakeSync()
        }
    }

    // --- Period Cycle Actions ---
    fun addPeriodCycle(startDate: String, durationDays: Int, cycleLengthDays: Int, symptoms: String, notes: String?) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val isDuplicate = periodCycles.value.any { it.startDate == startDate }
            if (isDuplicate) {
                _authError.value = "Only one period cycle can be logged for this start date!"
                return@launch
            }
            val cycle = PeriodCycleEntity(
                id = "period-${System.currentTimeMillis()}-${(1000..9999).random()}",
                userId = user.id,
                startDate = startDate,
                durationDays = durationDays,
                cycleLengthDays = cycleLengthDays,
                symptoms = symptoms,
                notes = notes
            )
            repository.insertPeriodCycle(cycle)
            triggerFakeSync()
        }
    }

    fun updatePeriodCycle(oldId: String, startDate: String, durationDays: Int, cycleLengthDays: Int, symptoms: String, notes: String?) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePeriodCycle(oldId)
            val cycle = PeriodCycleEntity(
                id = "period-${System.currentTimeMillis()}-${(1000..9999).random()}",
                userId = user.id,
                startDate = startDate,
                durationDays = durationDays,
                cycleLengthDays = cycleLengthDays,
                symptoms = symptoms,
                notes = notes
            )
            repository.insertPeriodCycle(cycle)
            triggerFakeSync()
        }
    }

    fun deletePeriodCycle(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePeriodCycle(id)
            triggerFakeSync()
        }
    }

    // --- Finance Actions ---
    fun addFinanceLog(
        type: String,
        category: String,
        title: String,
        amount: Double,
        notes: String?,
        date: String = TrackWiseUtils.getTodayString(),
        spendSource: String? = null
    ) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val log = FinanceLogEntity(
                id = "finance-${System.currentTimeMillis()}-${(1000..9999).random()}",
                userId = user.id,
                date = date,
                type = type,
                category = category,
                title = title,
                amount = amount,
                notes = notes,
                spendSource = spendSource
            )
            repository.insertFinanceLog(log)

            // If it's a savings log, it automatically goes to Net Worth assets!
            if (type == "savings") {
                val assetName = category // e.g. "Mutual Funds", "Simple Savings in Account"
                val existing = repository.getNetWorthItemByName(user.id, assetName)
                if (existing != null) {
                    repository.insertNetWorthItem(existing.copy(amount = existing.amount + amount))
                } else {
                    repository.insertNetWorthItem(
                        NetWorthItemEntity(
                            id = "nw-${System.currentTimeMillis()}-${(1000..9999).random()}",
                            userId = user.id,
                            name = assetName,
                            type = "asset",
                            amount = amount
                        )
                    )
                }
            }

            // If it's an expense log, deduct from that asset if it's not Cash/Current Income!
            if (type == "expense") {
                val source = spendSource ?: "Cash / Current Income"
                if (source != "Cash / Cash / Current Income" && source != "Cash / Current Income" && source.isNotBlank()) {
                    val existing = repository.getNetWorthItemByName(user.id, source)
                    if (existing != null) {
                        repository.insertNetWorthItem(existing.copy(amount = existing.amount - amount))
                    } else {
                        repository.insertNetWorthItem(
                            NetWorthItemEntity(
                                id = "nw-${System.currentTimeMillis()}-${(1000..9999).random()}",
                                userId = user.id,
                                name = source,
                                type = "asset",
                                amount = -amount
                            )
                        )
                    }
                }
            }

            triggerFakeSync()
        }
    }

    fun deleteFinanceLog(id: String) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val log = repository.getFinanceLogById(id)
            if (log != null) {
                // Undo the net worth effect!
                if (log.type == "savings") {
                    val assetName = log.category
                    val existing = repository.getNetWorthItemByName(user.id, assetName)
                    if (existing != null) {
                        repository.insertNetWorthItem(existing.copy(amount = maxOf(0.0, existing.amount - log.amount)))
                    }
                } else if (log.type == "expense") {
                    val source = log.spendSource ?: "Cash / Current Income"
                    if (source != "Cash / Cash / Current Income" && source != "Cash / Current Income" && source.isNotBlank()) {
                        val existing = repository.getNetWorthItemByName(user.id, source)
                        if (existing != null) {
                            repository.insertNetWorthItem(existing.copy(amount = existing.amount + log.amount))
                        }
                    }
                }
                repository.deleteFinanceLog(id)
            }
            triggerFakeSync()
        }
    }

    fun updateFinanceDailyTarget(target: Double) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val existingProfile = repository.getUserProfile(user.id) ?: UserProfileEntity(user.id)
            val updated = existingProfile.copy(financeDailyTarget = target)
            repository.insertUserProfile(updated)
            triggerFakeSync()
        }
    }

    // --- Net Worth Specific Actions ---
    fun addNetWorthItem(name: String, type: String, amount: Double) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedName = name.trim()
            val existing = repository.getNetWorthItemByName(user.id, trimmedName)
            if (existing != null) {
                val updated = existing.copy(amount = existing.amount + amount)
                repository.insertNetWorthItem(updated)
            } else {
                val item = NetWorthItemEntity(
                    id = "nw-${System.currentTimeMillis()}-${(1000..9999).random()}",
                    userId = user.id,
                    name = trimmedName,
                    type = type.lowercase(),
                    amount = amount
                )
                repository.insertNetWorthItem(item)
            }
            triggerFakeSync()
        }
    }

    fun deleteNetWorthItem(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNetWorthItem(id)
            triggerFakeSync()
        }
    }

    fun updateNetWorthItem(item: NetWorthItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertNetWorthItem(item)
            triggerFakeSync()
        }
    }

    private val _pinnedNetWorthIds = MutableStateFlow<List<String>>(emptyList())
    val pinnedNetWorthIds: StateFlow<List<String>> = _pinnedNetWorthIds.asStateFlow()

    fun togglePinNetWorthItem(id: String) {
        _pinnedNetWorthIds.value = if (_pinnedNetWorthIds.value.contains(id)) _pinnedNetWorthIds.value - id else _pinnedNetWorthIds.value + id
    }

    private val _pinnedGroceryIds = MutableStateFlow<List<String>>(emptyList())
    val pinnedGroceryIds: StateFlow<List<String>> = _pinnedGroceryIds.asStateFlow()

    fun togglePinGroceryItem(id: String) {
        _pinnedGroceryIds.value = if (_pinnedGroceryIds.value.contains(id)) _pinnedGroceryIds.value - id else _pinnedGroceryIds.value + id
    }
    fun togglePinGrocery(id: String) = togglePinGroceryItem(id)

    private val _pinnedWishlistIds = MutableStateFlow<List<String>>(emptyList())
    val pinnedWishlistIds: StateFlow<List<String>> = _pinnedWishlistIds.asStateFlow()

    fun togglePinWishlistItem(id: String) {
        _pinnedWishlistIds.value = if (_pinnedWishlistIds.value.contains(id)) _pinnedWishlistIds.value - id else _pinnedWishlistIds.value + id
    }
    fun togglePinWishlist(id: String) = togglePinWishlistItem(id)

    private val _pinnedHabitBreakerIds = MutableStateFlow<List<String>>(emptyList())
    val pinnedHabitBreakerIds: StateFlow<List<String>> = _pinnedHabitBreakerIds.asStateFlow()

    fun togglePinHabitBreaker(id: String) {
        _pinnedHabitBreakerIds.value = if (_pinnedHabitBreakerIds.value.contains(id)) _pinnedHabitBreakerIds.value - id else _pinnedHabitBreakerIds.value + id
    }

    private val _pinnedBirthdayIds = MutableStateFlow<List<String>>(emptyList())
    val pinnedBirthdayIds: StateFlow<List<String>> = _pinnedBirthdayIds.asStateFlow()

    fun togglePinBirthday(id: String) {
        _pinnedBirthdayIds.value = if (_pinnedBirthdayIds.value.contains(id)) {
            _pinnedBirthdayIds.value - id
        } else {
            _pinnedBirthdayIds.value + id
        }
        val current = allBirthdays.value.find { it.id == id }
        if (current != null) {
            updateBirthday(current.copy(isPinned = !current.isPinned))
        }
    }

    private val _pinnedTaskIds = MutableStateFlow<List<String>>(emptyList())
    val pinnedTaskIds: StateFlow<List<String>> = _pinnedTaskIds.asStateFlow()
    fun togglePinTask(id: String) {
        _pinnedTaskIds.value = if (_pinnedTaskIds.value.contains(id)) _pinnedTaskIds.value - id else _pinnedTaskIds.value + id
    }

    private val _pinnedHabitIds = MutableStateFlow<List<String>>(emptyList())
    val pinnedHabitIds: StateFlow<List<String>> = _pinnedHabitIds.asStateFlow()
    fun togglePinHabit(id: String) {
        _pinnedHabitIds.value = if (_pinnedHabitIds.value.contains(id)) _pinnedHabitIds.value - id else _pinnedHabitIds.value + id
    }

    private val _pinnedOccasionIds = MutableStateFlow<List<String>>(emptyList())
    val pinnedOccasionIds: StateFlow<List<String>> = _pinnedOccasionIds.asStateFlow()
    fun togglePinOccasion(id: String) = togglePinBirthday(id)

    private val _pinnedHealthLogIds = MutableStateFlow<List<String>>(emptyList())
    val pinnedHealthLogIds: StateFlow<List<String>> = _pinnedHealthLogIds.asStateFlow()
    fun togglePinHealthLog(id: String) {
        _pinnedHealthLogIds.value = if (_pinnedHealthLogIds.value.contains(id)) _pinnedHealthLogIds.value - id else _pinnedHealthLogIds.value + id
    }

    private val _dismissedHabitIdsToday = MutableStateFlow<Set<String>>(emptySet())
    val dismissedHabitIdsToday: StateFlow<Set<String>> = _dismissedHabitIdsToday.asStateFlow()

    private val _dismissedHabitKeys = MutableStateFlow<Set<String>>(emptySet())
    val dismissedHabitKeys: StateFlow<Set<String>> = _dismissedHabitKeys.asStateFlow()

    fun getHabitPeriodKey(frequency: String): String {
        val cal = java.util.Calendar.getInstance()
        val year = cal.get(java.util.Calendar.YEAR)
        return when (frequency.lowercase(java.util.Locale.ROOT)) {
            "weekly" -> "$year-W${cal.get(java.util.Calendar.WEEK_OF_YEAR)}"
            "monthly" -> "$year-M${cal.get(java.util.Calendar.MONTH)}"
            "yearly" -> "$year"
            else -> "$year-D${cal.get(java.util.Calendar.DAY_OF_YEAR)}"
        }
    }

    fun dismissHabitForToday(habitId: String) {
        dismissHabitForCurrentPeriod(habitId, "daily")
    }

    fun dismissHabitForCurrentPeriod(habitId: String, frequency: String) {
        val periodKey = getHabitPeriodKey(frequency)
        _dismissedHabitKeys.value = _dismissedHabitKeys.value + "${habitId}_${periodKey}"
        _dismissedHabitIdsToday.value = _dismissedHabitIdsToday.value + habitId
    }

    fun isHabitDismissedForCurrentPeriod(habitId: String, frequency: String): Boolean {
        val periodKey = getHabitPeriodKey(frequency)
        return _dismissedHabitKeys.value.contains("${habitId}_${periodKey}")
    }

    private val _dismissedTaskIdsToday = MutableStateFlow<Set<String>>(emptySet())
    val dismissedTaskIdsToday: StateFlow<Set<String>> = _dismissedTaskIdsToday.asStateFlow()
    fun dismissTaskForToday(taskId: String) {
        _dismissedTaskIdsToday.value = _dismissedTaskIdsToday.value + taskId
    }

    fun populateDefaultNetWorthItemsIfEmpty() {
        val user = _sessionUser.value ?: return
        val prefs = getSharedPrefs()
        if (prefs.getBoolean("net_worth_defaults_populated", false)) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val existingList = repository.getNetWorthItems(user.id)
            if (existingList.isEmpty()) {
                val defaults = listOf(
                    NetWorthItemEntity("nw-def-cash", user.id, "Cash / Current Income", "asset", 10000.0),
                    NetWorthItemEntity("nw-def-ssa", user.id, "Simple Savings in Account", "asset", 50000.0),
                    NetWorthItemEntity("nw-def-mf", user.id, "Mutual Funds", "asset", 25000.0),
                    NetWorthItemEntity("nw-def-stocks", user.id, "Stocks", "asset", 15000.0),
                    NetWorthItemEntity("nw-def-ppf", user.id, "PPF", "asset", 0.0),
                    NetWorthItemEntity("nw-def-fd", user.id, "FD", "asset", 0.0),
                    NetWorthItemEntity("nw-def-rd", user.id, "RD", "asset", 0.0),
                    NetWorthItemEntity("nw-def-nps", user.id, "NPS", "asset", 0.0),
                    NetWorthItemEntity("nw-def-epf", user.id, "EPF", "asset", 0.0),
                    NetWorthItemEntity("nw-def-others", user.id, "Others", "asset", 0.0),
                    NetWorthItemEntity("nw-def-loan", user.id, "Home Loan", "loan", 100000.0),
                    NetWorthItemEntity("nw-def-cc", user.id, "Credit Card Outstanding", "liability", 5000.0)
                )
                defaults.forEach { repository.insertNetWorthItem(it) }
                prefs.edit().putBoolean("net_worth_defaults_populated", true).apply()
                triggerFakeSync()
            }
        }
    }

    // --- Water Log Actions ---
    fun adjustWaterLog(glassesDelta: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = _sessionUser.value ?: allUsers.value.firstOrNull()
            val userId = user?.id ?: "user-default-syed"
            val today = TrackWiseUtils.getTodayString()
            val currentLog = repository.getWaterLogForDate(userId, today)
            
            if (currentLog == null) {
                val initGlasses = max(0, glassesDelta)
                repository.insertWaterLog(
                    WaterLogEntity(
                        id = "${userId}_$today",
                        userId = userId,
                        date = today,
                        glasses = initGlasses,
                        goal = user?.waterGoalGlasses ?: 8
                    )
                )
            } else {
                val newGlasses = max(0, currentLog.glasses + glassesDelta)
                repository.insertWaterLog(currentLog.copy(glasses = newGlasses))
            }
            triggerFakeSync()
        }
    }

    // --- Weight Log Actions ---
    fun logWeight(weight: Double, notes: String?, date: String? = null) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val finalDate = date ?: TrackWiseUtils.getTodayString()
            
            // Limit to one entry per day
            val isDuplicate = weightEntries.value.any { it.date == finalDate }
            if (isDuplicate) {
                _authError.value = "Only one weight entry is allowed per day!"
                return@launch
            }

            val time = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            val entry = WeightEntryEntity(
                id = "weight-${System.currentTimeMillis()}",
                userId = user.id,
                date = finalDate,
                time = time,
                weightKg = weight,
                notes = notes
            )
            repository.insertWeightEntry(entry)
            triggerFakeSync()
        }
    }

    fun deleteWeightEntry(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWeightEntry(id)
            triggerFakeSync()
        }
    }

    // --- Vital Log Actions ---
    fun logVital(type: String, value: String, context: String?, notes: String?, date: String? = null, time: String? = null) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val finalDate = date ?: TrackWiseUtils.getTodayString()
            val finalTime = time ?: SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            val entry = VitalReadingEntity(
                id = "vital-${System.currentTimeMillis()}",
                userId = user.id,
                type = type,
                date = finalDate,
                time = finalTime,
                value = value,
                context = context,
                notes = notes
            )
            repository.insertVitalReading(entry)
            triggerFakeSync()
        }
    }

    fun deleteVitalReading(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteVitalReading(id)
            triggerFakeSync()
        }
    }

    // --- Exercise Log Actions ---
    fun logExercise(type: String, duration: Int, completed: Boolean, notes: String?, date: String? = null) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val finalDate = date ?: TrackWiseUtils.getTodayString()
            val time = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            val entry = ExerciseLogEntity(
                id = "exercise-${System.currentTimeMillis()}",
                userId = user.id,
                date = finalDate,
                time = time,
                exerciseType = type,
                durationMinutes = duration,
                completed = completed,
                notes = notes
            )
            repository.insertExerciseLog(entry)
            triggerFakeSync()
        }
    }

    fun deleteExerciseLog(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExerciseLog(id)
            triggerFakeSync()
        }
    }

    // --- Health Issue Log Actions ---
    fun logHealthIssue(issueId: String, issueName: String, severity: String, notes: String?, date: String? = null) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val finalDate = date ?: TrackWiseUtils.getTodayString()
            val time = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            val entry = HealthIssueLogEntity(
                id = "issue-${System.currentTimeMillis()}",
                userId = user.id,
                date = finalDate,
                time = time,
                issueId = issueId,
                issueName = issueName,
                severity = severity,
                notes = notes,
                resolved = false
            )
            repository.insertHealthIssueLog(entry)
            triggerFakeSync()
        }
    }

    fun toggleHealthIssueResolved(issue: HealthIssueLogEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = issue.copy(resolved = !issue.resolved)
            repository.insertHealthIssueLog(updated)
            triggerFakeSync()
        }
    }

    fun deleteHealthIssueLog(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHealthIssueLog(id)
            triggerFakeSync()
        }
    }

    // --- Friend Actions ---
    fun addFriend(email: String) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _authError.value = null
            try {
                repository.addFriend(user.id, email)
                _successMessage.value = "Friend added successfully."
                triggerFakeSync()
            } catch (e: Exception) {
                _authError.value = e.message ?: "Failed to add friend."
            }
        }
    }

    fun removeFriend(friendUserId: String) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFriend(user.id, friendUserId)
            triggerFakeSync()
        }
    }

    fun getAllUsersFlow(): Flow<List<UserEntity>> = repository.getAllUsersFlow()

    // --- Alarm Actions ---
    fun addAlarm(hour: Int, minute: Int, label: String, repeatDays: List<String>) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val alarmId = "alarm-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(4)}"
            val alarm = AlarmEntity(
                id = alarmId,
                userId = user.id,
                label = label.ifBlank { "Alarm" },
                hour = hour,
                minute = minute,
                isEnabled = true,
                repeatDaysJson = TrackWiseUtils.serializeStringList(repeatDays)
            )
            repository.insertAlarm(alarm)
            triggerFakeSync()
        }
    }

    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled, snoozeCount = 0)
            repository.insertAlarm(updated)
            triggerFakeSync()
        }
    }

    fun deleteAlarm(alarmId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAlarm(alarmId)
            triggerFakeSync()
        }
    }

    fun snoozeAlarm(alarm: AlarmEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Add 5 minutes to snooze
            var newMin = alarm.minute + 5
            var newHour = alarm.hour
            if (newMin >= 60) {
                newMin -= 60
                newHour = (newHour + 1) % 24
            }
            val updated = alarm.copy(
                hour = newHour,
                minute = newMin,
                snoozeCount = alarm.snoozeCount + 1
            )
            repository.insertAlarm(updated)
            triggerFakeSync()
        }
    }

    fun dismissAlarm(alarm: AlarmEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Turn off alarm or keep enabled but reset snooze count
            val updated = alarm.copy(snoozeCount = 0)
            repository.insertAlarm(updated)
            triggerFakeSync()
        }
    }

    // --- Fake Sync Utility ---
    fun triggerFakeSync() {
        _isSyncing.value = false
        _syncMessage.value = "Auto-saved offline data"
    }

    // --- Sleep Tracker Actions ---
    fun addSleepLog(hoursSlept: Double, startTime: String, endTime: String, notes: String? = null, date: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = _sessionUser.value
            val userId = user?.id ?: "user-default-syed"
            val finalDate = date ?: TrackWiseUtils.getTodayString()
            
            val existing = sleepLogs.value.firstOrNull { it.date == finalDate }
            if (existing != null) {
                val updatedLog = existing.copy(
                    hoursSlept = hoursSlept,
                    startTime = startTime,
                    endTime = endTime,
                    notes = notes
                )
                repository.insertSleepLog(updatedLog)
                _successMessage.value = "Sleep entry updated for $finalDate."
            } else {
                val log = SleepLogEntity(
                    id = "sleep-${System.currentTimeMillis()}",
                    userId = userId,
                    date = finalDate,
                    hoursSlept = hoursSlept,
                    startTime = startTime,
                    endTime = endTime,
                    notes = notes
                )
                repository.insertSleepLog(log)
                _successMessage.value = "Sleep logged successfully."
            }
            triggerFakeSync()
        }
    }

    fun deleteSleepLog(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSleepLog(id)
            triggerFakeSync()
        }
    }

    // --- User Profile Actions ---
    fun saveDetailedProfile(profile: UserProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertUserProfile(profile)
            _sessionUser.value?.let { user ->
                val updatedUser = user.copy(
                    religion = profile.religion,
                    gender = profile.gender,
                    dob = profile.dob,
                    phone = profile.mobileNumber,
                    fullName = "${profile.firstName} ${profile.lastName}".trim()
                )
                repository.updateUserProfile(updatedUser)
                _sessionUser.value = updatedUser
            }
            _successMessage.value = "Detailed profile saved successfully."
            triggerFakeSync()
        }
    }

    // --- Sound & Vibration Selection Actions ---
    private var isAlarmPlaying = false

    fun triggerLightVibration() {
        try {
            val context = getApplication<Application>().applicationContext
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }
            if (vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK))
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(20L, 35))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(20L)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerVibrationPattern(pattern: LongArray) {
        try {
            val context = getApplication<Application>().applicationContext
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }
            if (vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerTaskCompletionVibration() {
        if (_vibrationEnabled.value && _vibrateOnTaskCompletion.value) {
            triggerVibrationPattern(longArrayOf(0, 30, 40, 50))
        }
    }

    fun triggerSwipeVibration() {
        if (_vibrationEnabled.value && _vibrateOnSwipe.value) {
            triggerLightVibration()
        }
    }

    fun triggerNotificationVibration() {
        if (_vibrationEnabled.value && _vibrateOnNotification.value) {
            triggerVibrationPattern(longArrayOf(0, 50, 40, 60))
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
        if (enabled) triggerLightVibration()
    }

    fun setVibrateOnTaskCompletion(enabled: Boolean) {
        _vibrateOnTaskCompletion.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("vibrate_on_task_completion", enabled).apply()
        if (enabled) triggerTaskCompletionVibration()
    }

    fun setVibrateOnSwipe(enabled: Boolean) {
        _vibrateOnSwipe.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("vibrate_on_swipe", enabled).apply()
        if (enabled) triggerSwipeVibration()
    }

    fun setVibrateOnNotification(enabled: Boolean) {
        _vibrateOnNotification.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("vibrate_on_notification", enabled).apply()
        if (enabled) triggerNotificationVibration()
    }

    fun playTaskCompletionSound() {
        triggerTaskCompletionVibration()
        val sound = _taskSound.value
        if (sound == "None") return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                when (sound) {
                    "Chime Gentle", "Chime" -> {
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        delay(120)
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
                    }
                    "Victory Bell" -> {
                        tg.startTone(ToneGenerator.TONE_DTMF_1, 90)
                        delay(90)
                        tg.startTone(ToneGenerator.TONE_DTMF_5, 90)
                        delay(90)
                        tg.startTone(ToneGenerator.TONE_DTMF_9, 220)
                    }
                    "Success Pop" -> {
                        tg.startTone(ToneGenerator.TONE_CDMA_PIP, 50)
                        delay(60)
                        tg.startTone(ToneGenerator.TONE_DTMF_8, 120)
                    }
                    "Digital Sparkle" -> {
                        val sparkle = listOf(ToneGenerator.TONE_DTMF_2, ToneGenerator.TONE_DTMF_4, ToneGenerator.TONE_DTMF_6, ToneGenerator.TONE_DTMF_8)
                        sparkle.forEach { note ->
                            tg.startTone(note, 60)
                            delay(70)
                        }
                    }
                    "Marimba Ring" -> {
                        val marimba = listOf(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE, ToneGenerator.TONE_CDMA_PIP, ToneGenerator.TONE_DTMF_5)
                        marimba.forEach { note ->
                            tg.startTone(note, 80)
                            delay(90)
                        }
                    }
                    "Zen Bowl" -> {
                        tg.startTone(ToneGenerator.TONE_SUP_RINGTONE, 300)
                    }
                    "Level Up" -> {
                        val levelNotes = listOf(ToneGenerator.TONE_DTMF_1, ToneGenerator.TONE_DTMF_3, ToneGenerator.TONE_DTMF_5, ToneGenerator.TONE_DTMF_8, ToneGenerator.TONE_DTMF_0)
                        levelNotes.forEach { note ->
                            tg.startTone(note, 60)
                            delay(70)
                        }
                    }
                    "Crystal Harp" -> {
                        val harp = listOf(ToneGenerator.TONE_DTMF_A, ToneGenerator.TONE_DTMF_B, ToneGenerator.TONE_DTMF_C, ToneGenerator.TONE_DTMF_D)
                        harp.forEach { note ->
                            tg.startTone(note, 80)
                            delay(100)
                        }
                    }
                    "Subtle Click" -> {
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 40)
                    }
                    "Acoustic Fanfare" -> {
                        tg.startTone(ToneGenerator.TONE_DTMF_3, 100)
                        delay(90)
                        tg.startTone(ToneGenerator.TONE_DTMF_6, 100)
                        delay(90)
                        tg.startTone(ToneGenerator.TONE_DTMF_9, 240)
                    }
                    else -> {
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                    }
                }
                delay(500)
                tg.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playAlarmSound() {
        val sound = _alarmSound.value
        isAlarmPlaying = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                while (isAlarmPlaying) {
                    val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                    when (sound) {
                        "Reflection" -> {
                            // Beautiful cascading high-pitch meditative notes
                            val reflectionNotes = listOf(
                                ToneGenerator.TONE_DTMF_1, ToneGenerator.TONE_DTMF_4, ToneGenerator.TONE_DTMF_7,
                                ToneGenerator.TONE_DTMF_5, ToneGenerator.TONE_DTMF_8, ToneGenerator.TONE_DTMF_3
                            )
                            for (note in reflectionNotes) {
                                if (!isAlarmPlaying) break
                                tg.startTone(note, 120)
                                delay(160)
                            }
                            delay(1000)
                        }
                        "Marimba" -> {
                            // Bright woodblock marimba rhythmic melody
                            val marimbaNotes = listOf(
                                ToneGenerator.TONE_CDMA_PIP, ToneGenerator.TONE_CDMA_PIP,
                                ToneGenerator.TONE_DTMF_5, ToneGenerator.TONE_CDMA_PIP,
                                ToneGenerator.TONE_DTMF_7, ToneGenerator.TONE_CDMA_PIP
                            )
                            for (note in marimbaNotes) {
                                if (!isAlarmPlaying) break
                                tg.startTone(note, 80)
                                delay(120)
                            }
                            delay(600)
                        }
                        "Over the Horizon" -> {
                            // Samsung-like classic melodic sequence
                            val horizonNotes = listOf(
                                ToneGenerator.TONE_DTMF_3, ToneGenerator.TONE_DTMF_4, 
                                ToneGenerator.TONE_DTMF_6, ToneGenerator.TONE_DTMF_8,
                                ToneGenerator.TONE_DTMF_9
                            )
                            for (note in horizonNotes) {
                                if (!isAlarmPlaying) break
                                tg.startTone(note, 180)
                                delay(220)
                            }
                            delay(1000)
                        }
                        "The Big Adventure" -> {
                            // Royal Westminster and chime hybrid dynamic pattern
                            val adventureNotes = listOf(
                                ToneGenerator.TONE_DTMF_1, ToneGenerator.TONE_DTMF_3, ToneGenerator.TONE_DTMF_5,
                                ToneGenerator.TONE_DTMF_2, ToneGenerator.TONE_DTMF_4, ToneGenerator.TONE_DTMF_6,
                                ToneGenerator.TONE_DTMF_5, ToneGenerator.TONE_DTMF_8
                            )
                            for (note in adventureNotes) {
                                if (!isAlarmPlaying) break
                                tg.startTone(note, 150)
                                delay(200)
                            }
                            delay(800)
                        }
                        else -> {
                            tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
                            delay(1000)
                        }
                    }
                    tg.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopAlarmSound() {
        isAlarmPlaying = false
    }

    fun setTaskSound(sound: String) {
        _taskSound.value = sound
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("task_sound", sound).apply()
        playTaskCompletionSound()
    }

    fun setAlarmSound(sound: String) {
        _alarmSound.value = sound
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("alarm_sound", sound).apply()
        // Play quick alarm preview
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
                when (sound) {
                    "Reflection" -> {
                        tg.startTone(ToneGenerator.TONE_DTMF_1, 100)
                        delay(120)
                        tg.startTone(ToneGenerator.TONE_DTMF_4, 100)
                    }
                    "Marimba" -> {
                        tg.startTone(ToneGenerator.TONE_CDMA_PIP, 80)
                        delay(100)
                        tg.startTone(ToneGenerator.TONE_DTMF_5, 80)
                    }
                    "Over the Horizon" -> {
                        tg.startTone(ToneGenerator.TONE_DTMF_3, 120)
                        delay(140)
                        tg.startTone(ToneGenerator.TONE_DTMF_6, 120)
                    }
                    "The Big Adventure" -> {
                        tg.startTone(ToneGenerator.TONE_DTMF_1, 100)
                        delay(120)
                        tg.startTone(ToneGenerator.TONE_DTMF_5, 100)
                    }
                }
                delay(200)
                tg.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Different Theme Accent Action ---
    fun setAppThemeSelection(themeName: String) {
        _appThemeSelection.value = themeName
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        prefs.edit().putString("saved_theme_accent", themeName).apply()
        updateAppWidget()
    }

    fun setAppBgType(type: String) {
        _appBgType.value = type
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        prefs.edit().putString("app_bg_type", type).apply()
    }

    fun setAppBgColor(colorName: String) {
        _appBgColor.value = colorName
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        prefs.edit().putString("app_bg_color", colorName).apply()
    }

    fun setAppBgGradient(gradientName: String) {
        _appBgGradient.value = gradientName
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        prefs.edit().putString("app_bg_gradient", gradientName).apply()
    }

    fun setAppBgImage(imageUrl: String) {
        _appBgImage.value = imageUrl
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        prefs.edit().putString("app_bg_image", imageUrl).apply()
    }

    fun setAppBgCustomUri(uriStr: String) {
        _appBgCustomUri.value = uriStr
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        prefs.edit().putString("app_bg_custom_uri", uriStr).apply()
    }

    // --- Account Management ---
    fun deleteAccount() {
        _sessionUser.value = null
        _settingsPanelOpen.value = false
        
        // Clear persisted session
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        prefs.edit().remove("saved_user_id").apply()
        
        _successMessage.value = "Account deleted successfully."
    }

    fun clearAllData() {
        val user = _sessionUser.value ?: return
        _settingsPanelOpen.value = false
        
        // Reset in-memory state
        _customFolders.value = emptyList()
        _customTags.value = emptyList()
        _deletedFolders.value = emptyList()
        _selectedTaskFolder.value = null
        _selectedTaskTag.value = null
        _badHabits.value = emptyList()
        saveBadHabitsList(emptyList())
        _notifications.value = emptyList()

        // Clear preferences
        val prefs = getSharedPrefs()
        prefs.edit()
            .remove("custom_folders_list")
            .remove("custom_tags_list")
            .remove("deleted_folders_set")
            .remove("bad_habits_list_v1")
            .remove("bottom_bar_tab_ids_v1")
            .putBoolean("net_worth_defaults_populated", true)
            .apply()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.clearUserData(user.id)
                viewModelScope.launch(Dispatchers.Main) {
                    _successMessage.value = "All data, net worth logs, finance logs, habit runways, and detailed records cleared! Account remains active."
                }
                updateAppWidget()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTask(task)
            triggerFakeSync()
        }
    }

    fun updateHabit(habit: HabitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertHabit(habit)
            if (habit.remindMe && !habit.reminderTime.isNullOrBlank()) {
                try {
                    val context = getApplication<android.app.Application>().applicationContext
                    val notifiedPrefs = context.getSharedPreferences("notified_reminders", android.content.Context.MODE_PRIVATE)
                    val todayStr = TrackWiseUtils.getTodayString()
                    val rDate = habit.reminderDate?.take(10) ?: todayStr
                    val rTime24 = com.example.receiver.ReminderReceiver.parseTo24HourTime(habit.reminderTime)
                    val currentTimeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())
                    if (rDate == todayStr && rTime24 != null && rTime24 <= currentTimeStr) {
                        val key = "habit-${habit.id}-$rDate-$rTime24"
                        notifiedPrefs.edit().putBoolean(key, true).apply()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            triggerFakeSync()
        }
    }

    fun updateWishItem(item: WishItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertWishItem(item)
            triggerFakeSync()
        }
    }

    fun updateGroceryItem(item: GroceryItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertGroceryItem(item)
            triggerFakeSync()
        }
    }

    fun updateTabletReminder(reminder: TabletReminderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTabletReminder(reminder)
            triggerFakeSync()
        }
    }

    fun updateWeightEntry(entry: WeightEntryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertWeightEntry(entry)
            triggerFakeSync()
        }
    }

    fun updateVitalReading(reading: VitalReadingEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertVitalReading(reading)
            triggerFakeSync()
        }
    }

    fun updateExerciseLog(log: ExerciseLogEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertExerciseLog(log)
            triggerFakeSync()
        }
    }

    fun updateHealthIssueLog(log: HealthIssueLogEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertHealthIssueLog(log)
            triggerFakeSync()
        }
    }

    fun updateSleepLog(log: SleepLogEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSleepLog(log)
            triggerFakeSync()
        }
    }

    fun updateFinanceLog(log: FinanceLogEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertFinanceLog(log)
            triggerFakeSync()
        }
    }

    // --- Alarm Update Action ---
    fun updateAlarm(id: String, hour: Int, minute: Int, label: String, repeatDays: List<String>) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val alarm = AlarmEntity(
                id = id,
                userId = user.id,
                label = label.ifBlank { "Alarm" },
                hour = hour,
                minute = minute,
                isEnabled = true,
                repeatDaysJson = TrackWiseUtils.serializeStringList(repeatDays)
            )
            repository.insertAlarm(alarm)
            triggerFakeSync()
            addNotification("Alarm Edited", "Alarm '${alarm.label}' was successfully updated.")
        }
    }

    private fun encodeUriToBase64IfNeeded(uriStr: String?): String? {
        if (uriStr.isNullOrBlank()) return null
        if (uriStr.startsWith("data:image/") || uriStr.startsWith("http://") || uriStr.startsWith("https://") || uriStr.startsWith("preset_")) {
            return uriStr
        }
        return try {
            val context = getApplication<Application>()
            val uri = android.net.Uri.parse(uriStr)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: if (java.io.File(uriStr).exists()) java.io.FileInputStream(java.io.File(uriStr)) else null
            inputStream?.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.isNotEmpty() && bytes.size < 15 * 1024 * 1024) {
                    "data:image/png;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                } else uriStr
            } ?: uriStr
        } catch (e: Exception) {
            uriStr
        }
    }

    private fun decodeBase64ToImageFileIfNeeded(dataStr: String?): String? {
        if (dataStr.isNullOrBlank()) return null
        if (dataStr.startsWith("data:image/")) {
            return try {
                val context = getApplication<Application>()
                val commaIdx = dataStr.indexOf(",")
                if (commaIdx != -1) {
                    val base64Str = dataStr.substring(commaIdx + 1)
                    val bytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
                    val dir = java.io.File(context.filesDir, "backed_up_images")
                    if (!dir.exists()) dir.mkdirs()
                    val file = java.io.File(dir, "img_${java.util.UUID.randomUUID()}.png")
                    file.writeBytes(bytes)
                    android.net.Uri.fromFile(file).toString()
                } else dataStr
            } catch (e: Exception) {
                dataStr
            }
        }
        return dataStr
    }

    // --- Settings Panels Actions ---
    suspend fun generateBackupJsonString(user: UserEntity): String {
        val rootJson = org.json.JSONObject()
        rootJson.put("version", 1)
        
        // App Preferences & Customizations
        val prefsJson = org.json.JSONObject()
        prefsJson.put("saved_theme_mode", _themeMode.value)
        prefsJson.put("saved_theme_accent", _appThemeSelection.value)
        prefsJson.put("app_font_size", _appFontSize.value)
        prefsJson.put("app_font_style", _appFontStyle.value)
        prefsJson.put("app_bg_type", _appBgType.value)
        prefsJson.put("app_bg_color", _appBgColor.value)
        prefsJson.put("app_bg_gradient", _appBgGradient.value)
        prefsJson.put("app_bg_image", _appBgImage.value)
        prefsJson.put("app_bg_custom_uri", encodeUriToBase64IfNeeded(_appBgCustomUri.value) ?: "")
        prefsJson.put("profile_image_uri", encodeUriToBase64IfNeeded(_profileImageUri.value) ?: "")
        prefsJson.put("auto_backup_frequency", _autoBackupFrequency.value)
        prefsJson.put("custom_folders_list", _customFolders.value.joinToString(","))
        prefsJson.put("custom_tags_list", _customTags.value.joinToString(","))
        prefsJson.put("bottom_bar_tab_ids", _bottomBarTabIds.value.joinToString(","))
        prefsJson.put("deleted_folders_set", _deletedFolders.value.joinToString(","))
        prefsJson.put("task_sound", _taskSound.value)
        prefsJson.put("alarm_sound", _alarmSound.value)
        val sessionPrefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
        prefsJson.put("bad_habits_json_v2", sessionPrefs.getString("bad_habits_json_v2", "") ?: "")
        prefsJson.put("pinned_finance_log_ids", _pinnedFinanceLogIds.value.joinToString(","))
        rootJson.put("appPreferences", prefsJson)

        // User info
        val userJson = org.json.JSONObject()
        userJson.put("id", user.id)
        userJson.put("email", user.email)
        userJson.put("fullName", user.fullName)
        userJson.put("dob", user.dob ?: "")
        userJson.put("gender", user.gender ?: "")
        userJson.put("heightCm", user.heightCm ?: 0.0)
        userJson.put("weightKg", user.weightKg ?: 0.0)
        userJson.put("phone", user.phone ?: "")
        userJson.put("addressLine1", user.addressLine1 ?: "")
        userJson.put("addressLine2", user.addressLine2 ?: "")
        userJson.put("city", user.city ?: "")
        userJson.put("state", user.state ?: "")
        userJson.put("zipCode", user.zipCode ?: "")
        userJson.put("bloodType", user.bloodType ?: "")
        userJson.put("waterGoalGlasses", user.waterGoalGlasses)
        userJson.put("enabledConditions", user.enabledConditions)
        userJson.put("religion", user.religion ?: "")
        rootJson.put("user", userJson)
        
        // Tasks
        val tasksArray = org.json.JSONArray()
        repository.getTasksFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("description", item.description)
            obj.put("completed", item.completed)
            obj.put("deadline", item.deadline)
            obj.put("priority", item.priority)
            obj.put("project", item.project)
            obj.put("points", item.points)
            obj.put("subtasksJson", item.subtasksJson)
            obj.put("reminderTime", item.reminderTime ?: "")
            obj.put("repeatType", item.repeatType)
            obj.put("customRepeatValue", item.customRepeatValue)
            obj.put("customRepeatUnit", item.customRepeatUnit)
            obj.put("customRepeatDaysOfWeek", item.customRepeatDaysOfWeek ?: "")
            obj.put("startDate", item.startDate ?: "")
            obj.put("endDate", item.endDate ?: "")
            obj.put("notes", item.notes)
            obj.put("remindMe", item.remindMe)
            obj.put("reminderDate", item.reminderDate ?: "")
            obj.put("dueTime", item.dueTime ?: "")
            tasksArray.put(obj)
        }
        rootJson.put("tasks", tasksArray)
        
        // Habits
        val habitsArray = org.json.JSONArray()
        repository.getHabitsFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("daysCompletedJson", item.daysCompletedJson)
            obj.put("frequency", item.frequency)
            obj.put("category", item.category)
            obj.put("streak", item.streak)
            obj.put("maxStreak", item.maxStreak)
            obj.put("badgesEarnedJson", item.badgesEarnedJson)
            obj.put("createdAt", item.createdAt)
            obj.put("isMultipleTimesPerDay", item.isMultipleTimesPerDay)
            obj.put("multipleTimesTarget", item.multipleTimesTarget)
            obj.put("isTimeBound", item.isTimeBound)
            obj.put("timeBoundDuration", item.timeBoundDuration ?: "")
            obj.put("repeatType", item.repeatType)
            obj.put("customRepeatValue", item.customRepeatValue)
            obj.put("customRepeatUnit", item.customRepeatUnit)
            obj.put("customRepeatDaysOfWeek", item.customRepeatDaysOfWeek ?: "")
            obj.put("startDate", item.startDate ?: "")
            obj.put("endDate", item.endDate ?: "")
            obj.put("notes", item.notes)
            obj.put("remindMe", item.remindMe)
            obj.put("reminderDate", item.reminderDate ?: "")
            obj.put("reminderTime", item.reminderTime ?: "")
            obj.put("dueTime", item.dueTime ?: "")
            obj.put("icon", encodeUriToBase64IfNeeded(item.icon) ?: "😊")
            obj.put("quote", item.quote)
            obj.put("goalType", item.goalType)
            obj.put("goalDays", item.goalDays)
            obj.put("section", item.section)
            obj.put("autoPopup", item.autoPopup)
            obj.put("backgroundImage", encodeUriToBase64IfNeeded(item.backgroundImage) ?: "window")
            habitsArray.put(obj)
        }
        rootJson.put("habits", habitsArray)

        // Alarms
        val alarmsArray = org.json.JSONArray()
        repository.getAlarmsFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("label", item.label)
            obj.put("hour", item.hour)
            obj.put("minute", item.minute)
            obj.put("isEnabled", item.isEnabled)
            obj.put("repeatDaysJson", item.repeatDaysJson)
            obj.put("snoozeCount", item.snoozeCount)
            alarmsArray.put(obj)
        }
        rootJson.put("alarms", alarmsArray)

        // Water Logs
        val waterArray = org.json.JSONArray()
        repository.getWaterLogsFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("date", item.date)
            obj.put("glasses", item.glasses)
            obj.put("goal", item.goal)
            obj.put("remindersEnabled", item.remindersEnabled)
            obj.put("reminderIntervalMinutes", item.reminderIntervalMinutes)
            waterArray.put(obj)
        }
        rootJson.put("waterLogs", waterArray)

        // Vital Readings
        val vitalsArray = org.json.JSONArray()
        repository.getVitalsFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("type", item.type)
            obj.put("date", item.date)
            obj.put("time", item.time ?: "")
            obj.put("value", item.value)
            obj.put("context", item.context ?: "")
            obj.put("notes", item.notes ?: "")
            vitalsArray.put(obj)
        }
        rootJson.put("vitalReadings", vitalsArray)

        // Weight Entries
        val weightArray = org.json.JSONArray()
        repository.getWeightEntriesFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("date", item.date)
            obj.put("time", item.time ?: "")
            obj.put("weightKg", item.weightKg)
            obj.put("notes", item.notes ?: "")
            weightArray.put(obj)
        }
        rootJson.put("weightEntries", weightArray)

        // Sleep Logs
        val sleepArray = org.json.JSONArray()
        repository.getSleepLogsFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("date", item.date)
            obj.put("hoursSlept", item.hoursSlept)
            obj.put("startTime", item.startTime)
            obj.put("endTime", item.endTime)
            obj.put("notes", item.notes ?: "")
            sleepArray.put(obj)
        }
        rootJson.put("sleepLogs", sleepArray)

        // Profile
        repository.getUserProfileFlow(user.id).first()?.let { prof ->
            val obj = org.json.JSONObject()
            obj.put("userId", prof.userId)
            obj.put("firstName", prof.firstName)
            obj.put("middleName", prof.middleName)
            obj.put("lastName", prof.lastName)
            obj.put("dob", prof.dob)
            obj.put("gender", prof.gender)
            obj.put("maritalStatus", prof.maritalStatus)
            obj.put("nationality", prof.nationality)
            obj.put("nationalId", prof.nationalId)
            obj.put("bloodGroup", prof.bloodGroup)
            obj.put("residentialStreet", prof.residentialStreet)
            obj.put("residentialCity", prof.residentialCity)
            obj.put("residentialState", prof.residentialState)
            obj.put("residentialZip", prof.residentialZip)
            obj.put("residentialCountry", prof.residentialCountry)
            obj.put("permanentStreet", prof.permanentStreet)
            obj.put("permanentCity", prof.permanentCity)
            obj.put("permanentState", prof.permanentState)
            obj.put("permanentZip", prof.permanentZip)
            obj.put("permanentCountry", prof.permanentCountry)
            obj.put("permanentIsSame", prof.permanentIsSame)
            obj.put("mobileNumber", prof.mobileNumber)
            obj.put("alternatePhone", prof.alternatePhone)
            obj.put("emailAddress", prof.emailAddress)
            obj.put("emergencyName", prof.emergencyName)
            obj.put("emergencyRelationship", prof.emergencyRelationship)
            obj.put("emergencyPhone", prof.emergencyPhone)
            obj.put("alternateEmergencyPhone", prof.alternateEmergencyPhone)
            obj.put("height", prof.height)
            obj.put("weight", prof.weight)
            obj.put("primaryDoctor", prof.primaryDoctor)
            obj.put("medicalConditions", prof.medicalConditions)
            obj.put("currentMedications", prof.currentMedications)
            obj.put("allergies", prof.allergies)
            obj.put("dietaryRestrictions", prof.dietaryRestrictions)
            obj.put("vitalsHeight", prof.vitalsHeight)
            obj.put("vitalsWeight", prof.vitalsWeight)
            obj.put("vitalsBloodPressure", prof.vitalsBloodPressure)
            obj.put("vitalsHeartRate", prof.vitalsHeartRate)
            obj.put("vitalsBloodGroup", prof.vitalsBloodGroup)
            obj.put("religion", prof.religion)
            obj.put("financeDailyTarget", prof.financeDailyTarget)
            rootJson.put("profile", obj)
        }

        // Birthdays / Occasions
        val birthdaysArray = org.json.JSONArray()
        repository.getBirthdaysFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("date", item.date)
            obj.put("giftIdea", item.giftIdea ?: "")
            obj.put("category", item.category)
            obj.put("remindMe", item.remindMe)
            obj.put("reminderDate", item.reminderDate ?: "")
            obj.put("reminderTime", item.reminderTime ?: "")
            obj.put("isPinned", item.isPinned)
            obj.put("customBgImage", encodeUriToBase64IfNeeded(item.customBgImage) ?: "")
            obj.put("customTextColor", item.customTextColor ?: "")
            obj.put("customFontStyle", item.customFontStyle ?: "")
            obj.put("reminderOptions", item.reminderOptions ?: "")
            obj.put("repeatPattern", item.repeatPattern ?: "")
            obj.put("countingMode", item.countingMode ?: "")
            birthdaysArray.put(obj)
        }
        rootJson.put("birthdays", birthdaysArray)

        // Notebooks
        val notebooksArray = org.json.JSONArray()
        repository.getNotebooksFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("coverPreset", item.coverPreset)
            obj.put("coverColor", item.coverColor)
            obj.put("customCoverUri", encodeUriToBase64IfNeeded(item.customCoverUri) ?: "")
            obj.put("createdAt", item.createdAt)
            obj.put("updatedAt", item.updatedAt)
            notebooksArray.put(obj)
        }
        rootJson.put("notebooks", notebooksArray)

        // Notes
        val notesArray = org.json.JSONArray()
        repository.getAllNotesFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("notebookId", item.notebookId)
            obj.put("title", item.title)
            obj.put("content", item.content)
            obj.put("cardColor", item.cardColor)
            obj.put("createdAt", item.createdAt)
            obj.put("updatedAt", item.updatedAt)
            obj.put("reminderDate", item.reminderDate ?: "")
            obj.put("reminderTime", item.reminderTime ?: "")
            obj.put("isPinned", item.isPinned)
            notesArray.put(obj)
        }
        rootJson.put("notes", notesArray)

        // Wishlist
        val wishlistArray = org.json.JSONArray()
        repository.getWishlistFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("price", item.price)
            obj.put("link", item.link ?: "")
            obj.put("priority", item.priority)
            obj.put("purchased", item.purchased)
            obj.put("remindMe", item.remindMe)
            obj.put("reminderDate", item.reminderDate ?: "")
            obj.put("reminderTime", item.reminderTime ?: "")
            wishlistArray.put(obj)
        }
        rootJson.put("wishlist", wishlistArray)

        // Grocery Items
        val groceryArray = org.json.JSONArray()
        repository.getGroceryItemsFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("quantity", item.quantity)
            obj.put("completed", item.completed)
            obj.put("category", item.category)
            obj.put("price", item.price ?: 0.0)
            obj.put("priceUnit", item.priceUnit ?: "")
            obj.put("numericQuantity", item.numericQuantity ?: 0.0)
            groceryArray.put(obj)
        }
        rootJson.put("groceryItems", groceryArray)

        // Streak History
        val streakHistoryArray = org.json.JSONArray()
        repository.getStreakHistoryFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("date", item.date)
            obj.put("score", item.score)
            streakHistoryArray.put(obj)
        }
        rootJson.put("streakHistory", streakHistoryArray)

        // Exercise Logs
        val exerciseArray = org.json.JSONArray()
        repository.getExerciseLogsFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("date", item.date)
            obj.put("time", item.time ?: "")
            obj.put("exerciseType", item.exerciseType)
            obj.put("durationMinutes", item.durationMinutes)
            obj.put("completed", item.completed)
            obj.put("notes", item.notes ?: "")
            exerciseArray.put(obj)
        }
        rootJson.put("exerciseLogs", exerciseArray)

        // Health Issue Logs
        val healthIssueArray = org.json.JSONArray()
        repository.getHealthIssueLogsFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("date", item.date)
            obj.put("time", item.time ?: "")
            obj.put("issueId", item.issueId)
            obj.put("issueName", item.issueName)
            obj.put("severity", item.severity)
            obj.put("notes", item.notes ?: "")
            obj.put("resolved", item.resolved)
            healthIssueArray.put(obj)
        }
        rootJson.put("healthIssueLogs", healthIssueArray)

        // Tablet Reminders
        val tabletArray = org.json.JSONArray()
        repository.getTabletRemindersFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("tabletName", item.tabletName)
            obj.put("dosage", item.dosage)
            obj.put("timeOfDay", item.timeOfDay)
            obj.put("scheduleType", item.scheduleType)
            obj.put("completedDatesJson", item.completedDatesJson)
            obj.put("notes", item.notes ?: "")
            tabletArray.put(obj)
        }
        rootJson.put("tabletReminders", tabletArray)

        // Period Cycles
        val periodArray = org.json.JSONArray()
        repository.getPeriodCyclesFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("startDate", item.startDate)
            obj.put("durationDays", item.durationDays)
            obj.put("cycleLengthDays", item.cycleLengthDays)
            obj.put("symptoms", item.symptoms)
            obj.put("notes", item.notes ?: "")
            periodArray.put(obj)
        }
        rootJson.put("periodCycles", periodArray)

        // Finance Logs
        val financeArray = org.json.JSONArray()
        repository.getFinanceLogsFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("date", item.date)
            obj.put("type", item.type)
            obj.put("category", item.category)
            obj.put("title", item.title)
            obj.put("amount", item.amount)
            obj.put("notes", item.notes ?: "")
            obj.put("spendSource", item.spendSource ?: "")
            financeArray.put(obj)
        }
        rootJson.put("financeLogs", financeArray)

        // Net Worth Items
        val netWorthArray = org.json.JSONArray()
        repository.getNetWorthItemsFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("type", item.type)
            obj.put("amount", item.amount)
            netWorthArray.put(obj)
        }
        rootJson.put("netWorthItems", netWorthArray)

        // Friends
        val friendsArray = org.json.JSONArray()
        repository.getFriendsFlow(user.id).first().forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("friendUserId", item.friendUserId)
            obj.put("displayName", item.displayName)
            obj.put("addedAt", item.addedAt)
            friendsArray.put(obj)
        }
        rootJson.put("friends", friendsArray)

        // Bad Habits
        val badHabitsArray = org.json.JSONArray()
        _badHabits.value.forEach { bh ->
            val obj = org.json.JSONObject()
            obj.put("id", bh.id)
            obj.put("name", bh.name)
            obj.put("avoidType", bh.avoidType)
            obj.put("reminderTime", bh.reminderTime)
            val tagsArr = org.json.JSONArray()
            bh.tags.forEach { t -> tagsArr.put(t) }
            obj.put("tags", tagsArr)
            obj.put("priority", bh.priority)
            obj.put("isRecurring", bh.isRecurring)
            obj.put("eventDate", bh.eventDate)
            obj.put("costType", bh.costType)
            obj.put("costValue", bh.costValue)
            val logsArr = org.json.JSONArray()
            bh.logs.forEach { l -> logsArr.put(l) }
            obj.put("logs", logsArr)
            badHabitsArray.put(obj)
        }
        rootJson.put("badHabits", badHabitsArray)

        return rootJson.toString(2)
    }

    fun exportData() {
        val user = _sessionUser.value
        if (user == null) {
            _authError.value = "You must be logged in to export data."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isSyncing.value = true
                _syncMessage.value = "Exporting data..."
                
                val jsonStr = generateBackupJsonString(user)
                
                // Write to cache file
                val file = java.io.File(getApplication<Application>().cacheDir, "trackwise_backup.json")
                file.writeText(jsonStr)
                
                // Copy to Clipboard as fallback
                val clipboard = getApplication<Application>().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("TrackWise Backup JSON", jsonStr)
                clipboard.setPrimaryClip(clip)

                // Save to downloads directory as well for easy access
                try {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                        val pubFile = java.io.File(downloadsDir, "trackwise_backup.json")
                        pubFile.writeText(jsonStr)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                _isSyncing.value = false
                _successMessage.value = "Backup JSON saved to Downloads and copied to Clipboard!"
                addNotification("Data Exported", "Successfully created trackwise_backup.json and copied to clipboard.")
                
                // Trigger Share Intent dynamically so they can easily save or copy it
                try {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        getApplication(),
                        "${getApplication<Application>().packageName}.fileprovider",
                        file
                    )
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = android.content.Intent.createChooser(intent, "Export TrackWise Backup")
                    chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    getApplication<Application>().startActivity(chooser)
                } catch (e: Exception) {
                    // Fallback handled beautifully
                    e.printStackTrace()
                }
                
            } catch (e: Exception) {
                _isSyncing.value = false
                _authError.value = "Export failed: ${e.message}"
            }
        }
    }

    fun updateAutoBackupFrequency(freq: String) {
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        prefs.edit().putString("auto_backup_frequency", freq).apply()
        _autoBackupFrequency.value = freq
        
        if (freq != "none") {
            checkAndPerformAutoBackup()
        }
    }

    fun checkAndPerformAutoBackup() {
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        val freq = prefs.getString("auto_backup_frequency", "none") ?: "none"
        if (freq == "none") return

        val lastBackup = prefs.getLong("last_auto_backup_time", 0L)
        val now = System.currentTimeMillis()
        val elapsedMs = now - lastBackup

        val thresholdMs = when (freq) {
            "hourly" -> 60 * 60 * 1000L
            "daily" -> 24 * 60 * 60 * 1000L
            "weekly" -> 7 * 24 * 60 * 60 * 1000L
            else -> Long.MAX_VALUE
        }

        if (elapsedMs >= thresholdMs) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val user = _sessionUser.value ?: return@launch
                    val jsonStr = generateBackupJsonString(user)

                    // Write to cache file
                    val cacheFile = java.io.File(getApplication<Application>().cacheDir, "trackwise_backup.json")
                    cacheFile.writeText(jsonStr)

                    // Save to Downloads folder as auto-backup
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                        val pubFile = java.io.File(downloadsDir, "trackwise_auto_backup.json")
                        pubFile.writeText(jsonStr)
                    }

                    prefs.edit().putLong("last_auto_backup_time", now).apply()
                    _lastAutoBackupTime.value = now
                    
                    addNotification("Auto Backup Created", "Successfully completed automated data export to local storage.")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun importData(jsonContent: String) {
        val user = _sessionUser.value
        if (user == null) {
            _authError.value = "You must be logged in to import data."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isSyncing.value = true
                _syncMessage.value = "Importing backup..."
                
                val rootJson = org.json.JSONObject(jsonContent)
                
                // Clear old database data first to avoid residual data merging
                repository.clearUserData(user.id)
                
                // Restore App Preferences
                if (rootJson.has("appPreferences")) {
                    val prefsObj = rootJson.getJSONObject("appPreferences")
                    if (prefsObj.has("saved_theme_mode")) setThemeMode(prefsObj.getString("saved_theme_mode"))
                    if (prefsObj.has("saved_theme_accent")) setAppThemeSelection(prefsObj.getString("saved_theme_accent"))
                    if (prefsObj.has("app_font_size")) setAppFontSize(prefsObj.getString("app_font_size"))
                    if (prefsObj.has("app_font_style")) setAppFontStyle(prefsObj.getString("app_font_style"))
                    if (prefsObj.has("app_bg_type")) setAppBgType(prefsObj.getString("app_bg_type"))
                    if (prefsObj.has("app_bg_color")) setAppBgColor(prefsObj.getString("app_bg_color"))
                    if (prefsObj.has("app_bg_gradient")) setAppBgGradient(prefsObj.getString("app_bg_gradient"))
                    if (prefsObj.has("app_bg_image")) setAppBgImage(prefsObj.getString("app_bg_image"))
                    if (prefsObj.has("app_bg_custom_uri")) setAppBgCustomUri(decodeBase64ToImageFileIfNeeded(prefsObj.getString("app_bg_custom_uri")) ?: "")
                    if (prefsObj.has("profile_image_uri")) setProfileImageUri(decodeBase64ToImageFileIfNeeded(prefsObj.optString("profile_image_uri"))?.ifBlank { null })
                    if (prefsObj.has("auto_backup_frequency")) updateAutoBackupFrequency(prefsObj.getString("auto_backup_frequency"))
                    if (prefsObj.has("custom_folders_list")) {
                        val folders = prefsObj.getString("custom_folders_list").split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        folders.forEach { addCustomFolder(it) }
                    }
                    if (prefsObj.has("custom_tags_list")) {
                        val tags = prefsObj.getString("custom_tags_list").split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        tags.forEach { addCustomTag(it) }
                    }
                    if (prefsObj.has("task_sound")) setTaskSound(prefsObj.getString("task_sound"))
                    if (prefsObj.has("alarm_sound")) setAlarmSound(prefsObj.getString("alarm_sound"))
                    if (prefsObj.has("bad_habits_json_v2")) {
                        val bh = prefsObj.getString("bad_habits_json_v2")
                        if (!bh.isNullOrBlank()) {
                            val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
                            prefs.edit().putString("bad_habits_json_v2", bh).apply()
                            loadBadHabits()
                        }
                    }
                    if (prefsObj.has("pinned_finance_log_ids")) {
                        val pinned = prefsObj.getString("pinned_finance_log_ids")
                        val list = pinned.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        _pinnedFinanceLogIds.value = list
                        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putString("pinned_finance_log_ids", pinned).apply()
                    }
                }

                // Restore User Info if detailed
                if (rootJson.has("user")) {
                    val uObj = rootJson.getJSONObject("user")
                    val updatedUser = user.copy(
                        dob = if (uObj.has("dob") && uObj.getString("dob").isNotBlank()) uObj.getString("dob") else user.dob,
                        gender = if (uObj.has("gender") && uObj.getString("gender").isNotBlank()) uObj.getString("gender") else user.gender,
                        heightCm = if (uObj.has("heightCm") && !uObj.isNull("heightCm")) uObj.getDouble("heightCm") else user.heightCm,
                        weightKg = if (uObj.has("weightKg") && !uObj.isNull("weightKg")) uObj.getDouble("weightKg") else user.weightKg,
                        phone = if (uObj.has("phone") && uObj.getString("phone").isNotBlank()) uObj.getString("phone") else user.phone,
                        addressLine1 = if (uObj.has("addressLine1") && uObj.getString("addressLine1").isNotBlank()) uObj.getString("addressLine1") else user.addressLine1,
                        addressLine2 = if (uObj.has("addressLine2") && uObj.getString("addressLine2").isNotBlank()) uObj.getString("addressLine2") else user.addressLine2,
                        city = if (uObj.has("city") && uObj.getString("city").isNotBlank()) uObj.getString("city") else user.city,
                        state = if (uObj.has("state") && uObj.getString("state").isNotBlank()) uObj.getString("state") else user.state,
                        zipCode = if (uObj.has("zipCode") && uObj.getString("zipCode").isNotBlank()) uObj.getString("zipCode") else user.zipCode,
                        bloodType = if (uObj.has("bloodType") && uObj.getString("bloodType").isNotBlank()) uObj.getString("bloodType") else user.bloodType,
                        waterGoalGlasses = uObj.optInt("waterGoalGlasses", user.waterGoalGlasses),
                        enabledConditions = uObj.optString("enabledConditions", user.enabledConditions),
                        religion = if (uObj.has("religion") && uObj.getString("religion").isNotBlank()) uObj.getString("religion") else user.religion
                    )
                    repository.updateUserProfile(updatedUser)
                    _sessionUser.value = updatedUser
                }

                // Restore Tasks
                if (rootJson.has("tasks")) {
                    val array = rootJson.getJSONArray("tasks")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = TaskEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            title = obj.getString("title"),
                            description = obj.optString("description", ""),
                            project = obj.optString("project", "Inbox"),
                            priority = obj.optString("priority", "medium"),
                            deadline = obj.optString("deadline", TrackWiseUtils.getTodayString()),
                            completed = obj.optBoolean("completed", false),
                            points = obj.optInt("points", 10),
                            subtasksJson = obj.optString("subtasksJson", "[]"),
                            reminderTime = if (obj.has("reminderTime") && obj.getString("reminderTime").isNotBlank()) obj.getString("reminderTime") else null,
                            repeatType = obj.optString("repeatType", "none"),
                            customRepeatValue = obj.optInt("customRepeatValue", 1),
                            customRepeatUnit = obj.optString("customRepeatUnit", "days"),
                            customRepeatDaysOfWeek = if (obj.has("customRepeatDaysOfWeek") && obj.getString("customRepeatDaysOfWeek").isNotBlank()) obj.getString("customRepeatDaysOfWeek") else null,
                            startDate = if (obj.has("startDate") && obj.getString("startDate").isNotBlank()) obj.getString("startDate") else null,
                            endDate = if (obj.has("endDate") && obj.getString("endDate").isNotBlank()) obj.getString("endDate") else null,
                            notes = obj.optString("notes", ""),
                            remindMe = obj.optBoolean("remindMe", false),
                            reminderDate = if (obj.has("reminderDate") && obj.getString("reminderDate").isNotBlank()) obj.getString("reminderDate") else null,
                            dueTime = if (obj.has("dueTime") && obj.getString("dueTime").isNotBlank()) obj.getString("dueTime") else null
                        )
                        repository.insertTask(entity)
                    }
                }
                
                // Restore Habits
                if (rootJson.has("habits")) {
                    val array = rootJson.getJSONArray("habits")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = HabitEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            name = obj.getString("name"),
                            category = obj.optString("category", "Wellness"),
                            frequency = obj.optString("frequency", "daily"),
                            daysCompletedJson = obj.optString("daysCompletedJson", "[]"),
                            streak = obj.optInt("streak", 0),
                            maxStreak = obj.optInt("maxStreak", 0),
                            badgesEarnedJson = obj.optString("badgesEarnedJson", "[]"),
                            createdAt = obj.optString("createdAt", TrackWiseUtils.getTodayString()),
                            isMultipleTimesPerDay = obj.optBoolean("isMultipleTimesPerDay", false),
                            multipleTimesTarget = obj.optInt("multipleTimesTarget", 1),
                            isTimeBound = obj.optBoolean("isTimeBound", false),
                            timeBoundDuration = if (obj.has("timeBoundDuration") && obj.getString("timeBoundDuration").isNotBlank()) obj.getString("timeBoundDuration") else null,
                            repeatType = obj.optString("repeatType", "none"),
                            customRepeatValue = obj.optInt("customRepeatValue", 1),
                            customRepeatUnit = obj.optString("customRepeatUnit", "days"),
                            customRepeatDaysOfWeek = if (obj.has("customRepeatDaysOfWeek") && obj.getString("customRepeatDaysOfWeek").isNotBlank()) obj.getString("customRepeatDaysOfWeek") else null,
                            startDate = if (obj.has("startDate") && obj.getString("startDate").isNotBlank()) obj.getString("startDate") else null,
                            endDate = if (obj.has("endDate") && obj.getString("endDate").isNotBlank()) obj.getString("endDate") else null,
                            notes = obj.optString("notes", ""),
                            remindMe = obj.optBoolean("remindMe", false),
                            reminderDate = if (obj.has("reminderDate") && obj.getString("reminderDate").isNotBlank()) obj.getString("reminderDate") else null,
                            reminderTime = if (obj.has("reminderTime") && obj.getString("reminderTime").isNotBlank()) obj.getString("reminderTime") else null,
                            dueTime = if (obj.has("dueTime") && obj.getString("dueTime").isNotBlank()) obj.getString("dueTime") else null,
                            icon = decodeBase64ToImageFileIfNeeded(obj.optString("icon", "😊")) ?: "😊",
                            quote = obj.optString("quote", ""),
                            goalType = obj.optString("goalType", "Achieve it all"),
                            goalDays = obj.optString("goalDays", "Forever"),
                            section = obj.optString("section", "Others"),
                            autoPopup = obj.optBoolean("autoPopup", false),
                            backgroundImage = decodeBase64ToImageFileIfNeeded(obj.optString("backgroundImage", "window")) ?: "window"
                        )
                        repository.insertHabit(entity)
                    }
                }

                // Restore Alarms
                if (rootJson.has("alarms")) {
                    val array = rootJson.getJSONArray("alarms")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = AlarmEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            label = obj.getString("label"),
                            hour = obj.getInt("hour"),
                            minute = obj.getInt("minute"),
                            isEnabled = obj.getBoolean("isEnabled"),
                            repeatDaysJson = obj.optString("repeatDaysJson", "[]"),
                            snoozeCount = obj.optInt("snoozeCount", 0)
                        )
                        repository.insertAlarm(entity)
                    }
                }

                // Restore Water Logs
                if (rootJson.has("waterLogs")) {
                    val array = rootJson.getJSONArray("waterLogs")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = WaterLogEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            date = obj.getString("date"),
                            glasses = obj.getInt("glasses"),
                            goal = obj.getInt("goal"),
                            remindersEnabled = obj.optBoolean("remindersEnabled", false),
                            reminderIntervalMinutes = obj.optInt("reminderIntervalMinutes", 60)
                        )
                        repository.insertWaterLog(entity)
                    }
                }

                // Restore Vital Readings
                if (rootJson.has("vitalReadings")) {
                    val array = rootJson.getJSONArray("vitalReadings")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = VitalReadingEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            type = obj.getString("type"),
                            date = obj.getString("date"),
                            time = if (obj.has("time") && obj.getString("time").isNotEmpty()) obj.getString("time") else null,
                            value = obj.getString("value"),
                            context = if (obj.has("context") && obj.getString("context").isNotEmpty()) obj.getString("context") else null,
                            notes = if (obj.has("notes") && obj.getString("notes").isNotEmpty()) obj.getString("notes") else null
                        )
                        repository.insertVitalReading(entity)
                    }
                }

                // Restore Weight Entries
                if (rootJson.has("weightEntries")) {
                    val array = rootJson.getJSONArray("weightEntries")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = WeightEntryEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            date = obj.getString("date"),
                            time = if (obj.has("time") && obj.getString("time").isNotEmpty()) obj.getString("time") else null,
                            weightKg = obj.getDouble("weightKg"),
                            notes = if (obj.has("notes") && obj.getString("notes").isNotEmpty()) obj.getString("notes") else null
                        )
                        repository.insertWeightEntry(entity)
                    }
                }

                // Restore Sleep Logs
                if (rootJson.has("sleepLogs")) {
                    val array = rootJson.getJSONArray("sleepLogs")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = SleepLogEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            date = obj.getString("date"),
                            hoursSlept = obj.getDouble("hoursSlept"),
                            startTime = obj.getString("startTime"),
                            endTime = obj.getString("endTime"),
                            notes = if (obj.has("notes") && obj.getString("notes").isNotEmpty()) obj.getString("notes") else null
                        )
                        repository.insertSleepLog(entity)
                    }
                }

                // Restore Profile
                if (rootJson.has("profile")) {
                    val obj = rootJson.getJSONObject("profile")
                    val entity = UserProfileEntity(
                        userId = user.id,
                        firstName = obj.optString("firstName", ""),
                        middleName = obj.optString("middleName", ""),
                        lastName = obj.optString("lastName", ""),
                        dob = obj.optString("dob", ""),
                        gender = obj.optString("gender", "Prefer not to say"),
                        maritalStatus = obj.optString("maritalStatus", "Single"),
                        nationality = obj.optString("nationality", ""),
                        nationalId = obj.optString("nationalId", ""),
                        bloodGroup = obj.optString("bloodGroup", "O+"),
                        residentialStreet = obj.optString("residentialStreet", ""),
                        residentialCity = obj.optString("residentialCity", ""),
                        residentialState = obj.optString("residentialState", ""),
                        residentialZip = obj.optString("residentialZip", ""),
                        residentialCountry = obj.optString("residentialCountry", ""),
                        permanentStreet = obj.optString("permanentStreet", ""),
                        permanentCity = obj.optString("permanentCity", ""),
                        permanentState = obj.optString("permanentState", ""),
                        permanentZip = obj.optString("permanentZip", ""),
                        permanentCountry = obj.optString("permanentCountry", ""),
                        permanentIsSame = obj.optBoolean("permanentIsSame", true),
                        mobileNumber = obj.optString("mobileNumber", ""),
                        alternatePhone = obj.optString("alternatePhone", ""),
                        emailAddress = obj.optString("emailAddress", ""),
                        emergencyName = obj.optString("emergencyName", ""),
                        emergencyRelationship = obj.optString("emergencyRelationship", ""),
                        emergencyPhone = obj.optString("emergencyPhone", ""),
                        alternateEmergencyPhone = obj.optString("alternateEmergencyPhone", ""),
                        height = obj.optString("height", ""),
                        weight = obj.optString("weight", ""),
                        primaryDoctor = obj.optString("primaryDoctor", ""),
                        medicalConditions = obj.optString("medicalConditions", ""),
                        currentMedications = obj.optString("currentMedications", ""),
                        allergies = obj.optString("allergies", ""),
                        dietaryRestrictions = obj.optString("dietaryRestrictions", ""),
                        vitalsHeight = obj.optString("vitalsHeight", ""),
                        vitalsWeight = obj.optString("vitalsWeight", ""),
                        vitalsBloodPressure = obj.optString("vitalsBloodPressure", ""),
                        vitalsHeartRate = obj.optString("vitalsHeartRate", ""),
                        vitalsBloodGroup = obj.optString("vitalsBloodGroup", ""),
                        religion = obj.optString("religion", "Others"),
                        financeDailyTarget = obj.optDouble("financeDailyTarget", 30000.0)
                    )
                    repository.insertUserProfile(entity)
                }

                // Restore Birthdays / Occasions
                if (rootJson.has("birthdays")) {
                    val array = rootJson.getJSONArray("birthdays")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = BirthdayEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            name = obj.getString("name"),
                            date = obj.getString("date"),
                            giftIdea = if (obj.has("giftIdea") && obj.getString("giftIdea").isNotBlank()) obj.getString("giftIdea") else null,
                            category = obj.optString("category", "Others"),
                            remindMe = obj.optBoolean("remindMe", false),
                            reminderDate = if (obj.has("reminderDate") && obj.getString("reminderDate").isNotBlank()) obj.getString("reminderDate") else null,
                            reminderTime = if (obj.has("reminderTime") && obj.getString("reminderTime").isNotBlank()) obj.getString("reminderTime") else null,
                            isPinned = obj.optBoolean("isPinned", false),
                            customBgImage = decodeBase64ToImageFileIfNeeded(if (obj.has("customBgImage") && obj.getString("customBgImage").isNotBlank()) obj.getString("customBgImage") else null),
                            customTextColor = if (obj.has("customTextColor") && obj.getString("customTextColor").isNotBlank()) obj.getString("customTextColor") else null,
                            customFontStyle = if (obj.has("customFontStyle") && obj.getString("customFontStyle").isNotBlank()) obj.getString("customFontStyle") else null,
                            reminderOptions = if (obj.has("reminderOptions") && obj.getString("reminderOptions").isNotBlank()) obj.getString("reminderOptions") else null,
                            repeatPattern = if (obj.has("repeatPattern") && obj.getString("repeatPattern").isNotBlank()) obj.getString("repeatPattern") else null,
                            countingMode = if (obj.has("countingMode") && obj.getString("countingMode").isNotBlank()) obj.getString("countingMode") else null
                        )
                        repository.insertBirthday(entity)
                    }
                }

                // Restore Notebooks
                if (rootJson.has("notebooks")) {
                    val array = rootJson.getJSONArray("notebooks")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = NotebookEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            title = obj.getString("title"),
                            coverPreset = obj.optString("coverPreset", "preset_1"),
                            coverColor = obj.optString("coverColor", "#FF9800"),
                            customCoverUri = decodeBase64ToImageFileIfNeeded(if (obj.has("customCoverUri") && obj.getString("customCoverUri").isNotBlank()) obj.getString("customCoverUri") else null),
                            createdAt = obj.optString("createdAt", TrackWiseUtils.getTodayString()),
                            updatedAt = obj.optString("updatedAt", TrackWiseUtils.getTodayString())
                        )
                        repository.insertNotebook(entity)
                    }
                }

                // Restore Notes
                if (rootJson.has("notes")) {
                    val array = rootJson.getJSONArray("notes")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = NoteEntity(
                            id = obj.getString("id"),
                            notebookId = obj.getString("notebookId"),
                            userId = user.id,
                            title = obj.getString("title"),
                            content = obj.optString("content", ""),
                            cardColor = obj.optString("cardColor", "#FFF59D"),
                            createdAt = obj.optString("createdAt", TrackWiseUtils.getTodayString()),
                            updatedAt = obj.optString("updatedAt", TrackWiseUtils.getTodayString()),
                            reminderDate = if (obj.has("reminderDate") && obj.getString("reminderDate").isNotBlank()) obj.getString("reminderDate") else null,
                            reminderTime = if (obj.has("reminderTime") && obj.getString("reminderTime").isNotBlank()) obj.getString("reminderTime") else null,
                            isPinned = obj.optBoolean("isPinned", false)
                        )
                        repository.insertNote(entity)
                    }
                }

                // Restore Wishlist
                if (rootJson.has("wishlist")) {
                    val array = rootJson.getJSONArray("wishlist")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = WishItemEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            title = obj.getString("title"),
                            price = obj.optDouble("price", 0.0),
                            link = if (obj.has("link") && obj.getString("link").isNotBlank()) obj.getString("link") else null,
                            priority = obj.optString("priority", "medium"),
                            purchased = obj.optBoolean("purchased", false),
                            remindMe = obj.optBoolean("remindMe", false),
                            reminderDate = if (obj.has("reminderDate") && obj.getString("reminderDate").isNotBlank()) obj.getString("reminderDate") else null,
                            reminderTime = if (obj.has("reminderTime") && obj.getString("reminderTime").isNotBlank()) obj.getString("reminderTime") else null
                        )
                        repository.insertWishItem(entity)
                    }
                }

                // Restore Grocery Items
                if (rootJson.has("groceryItems")) {
                    val array = rootJson.getJSONArray("groceryItems")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = GroceryItemEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            name = obj.getString("name"),
                            quantity = obj.optString("quantity", "1"),
                            completed = obj.optBoolean("completed", false),
                            category = obj.optString("category", "Other"),
                            price = if (obj.has("price") && !obj.isNull("price")) obj.getDouble("price") else null,
                            priceUnit = if (obj.has("priceUnit") && !obj.isNull("priceUnit")) obj.getString("priceUnit") else null,
                            numericQuantity = if (obj.has("numericQuantity") && !obj.isNull("numericQuantity")) obj.getDouble("numericQuantity") else null
                        )
                        repository.insertGroceryItem(entity)
                    }
                }

                // Restore Streak History
                if (rootJson.has("streakHistory")) {
                    val array = rootJson.getJSONArray("streakHistory")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = StreakHistoryEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            date = obj.getString("date"),
                            score = obj.getInt("score")
                        )
                        repository.insertStreakHistory(entity)
                    }
                }

                // Restore Exercise Logs
                if (rootJson.has("exerciseLogs")) {
                    val array = rootJson.getJSONArray("exerciseLogs")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = ExerciseLogEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            date = obj.getString("date"),
                            time = if (obj.has("time") && obj.getString("time").isNotEmpty()) obj.getString("time") else null,
                            exerciseType = obj.getString("exerciseType"),
                            durationMinutes = obj.optInt("durationMinutes", 0),
                            completed = obj.optBoolean("completed", false),
                            notes = if (obj.has("notes") && obj.getString("notes").isNotEmpty()) obj.getString("notes") else null
                        )
                        repository.insertExerciseLog(entity)
                    }
                }

                // Restore Health Issue Logs
                if (rootJson.has("healthIssueLogs")) {
                    val array = rootJson.getJSONArray("healthIssueLogs")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = HealthIssueLogEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            date = obj.getString("date"),
                            time = if (obj.has("time") && obj.getString("time").isNotEmpty()) obj.getString("time") else null,
                            issueId = obj.getString("issueId"),
                            issueName = obj.getString("issueName"),
                            severity = obj.optString("severity", "mild"),
                            notes = if (obj.has("notes") && obj.getString("notes").isNotEmpty()) obj.getString("notes") else null,
                            resolved = obj.optBoolean("resolved", false)
                        )
                        repository.insertHealthIssueLog(entity)
                    }
                }

                // Restore Tablet Reminders
                if (rootJson.has("tabletReminders")) {
                    val array = rootJson.getJSONArray("tabletReminders")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = TabletReminderEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            tabletName = obj.getString("tabletName"),
                            dosage = obj.getString("dosage"),
                            timeOfDay = obj.getString("timeOfDay"),
                            scheduleType = obj.getString("scheduleType"),
                            completedDatesJson = obj.optString("completedDatesJson", "[]"),
                            notes = if (obj.has("notes") && obj.getString("notes").isNotEmpty()) obj.getString("notes") else null
                        )
                        repository.insertTabletReminder(entity)
                    }
                }

                // Restore Period Cycles
                if (rootJson.has("periodCycles")) {
                    val array = rootJson.getJSONArray("periodCycles")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = PeriodCycleEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            startDate = obj.getString("startDate"),
                            durationDays = obj.optInt("durationDays", 5),
                            cycleLengthDays = obj.optInt("cycleLengthDays", 28),
                            symptoms = obj.optString("symptoms", ""),
                            notes = if (obj.has("notes") && obj.getString("notes").isNotEmpty()) obj.getString("notes") else null
                        )
                        repository.insertPeriodCycle(entity)
                    }
                }

                // Restore Finance Logs
                if (rootJson.has("financeLogs")) {
                    val array = rootJson.getJSONArray("financeLogs")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = FinanceLogEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            date = obj.getString("date"),
                            type = obj.getString("type"),
                            category = obj.getString("category"),
                            title = obj.getString("title"),
                            amount = obj.getDouble("amount"),
                            notes = if (obj.has("notes") && obj.getString("notes").isNotEmpty()) obj.getString("notes") else null,
                            spendSource = if (obj.has("spendSource") && obj.getString("spendSource").isNotEmpty()) obj.getString("spendSource") else null
                        )
                        repository.insertFinanceLog(entity)
                    }
                }

                // Restore Net Worth Items
                if (rootJson.has("netWorthItems")) {
                    val array = rootJson.getJSONArray("netWorthItems")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = NetWorthItemEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            name = obj.getString("name"),
                            type = obj.getString("type"),
                            amount = obj.getDouble("amount")
                        )
                        repository.insertNetWorthItem(entity)
                    }
                }

                // Restore Friends
                if (rootJson.has("friends")) {
                    val array = rootJson.getJSONArray("friends")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = FriendConnectionEntity(
                            id = obj.getString("id"),
                            userId = user.id,
                            friendUserId = obj.getString("friendUserId"),
                            displayName = obj.getString("displayName"),
                            addedAt = obj.getString("addedAt")
                        )
                        repository.insertFriend(entity)
                    }
                }

                // Restore Bad Habits
                if (rootJson.has("badHabits")) {
                    val array = rootJson.getJSONArray("badHabits")
                    val restoredBadHabits = mutableListOf<BadHabitSpec>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val bhId = obj.getString("id")
                        val bhName = obj.getString("name")
                        val avoidType = obj.optString("avoidType", "Habit")
                        val reminderTime = obj.optString("reminderTime", "")
                        val tagsList = mutableListOf<String>()
                        if (obj.has("tags")) {
                            val tArr = obj.getJSONArray("tags")
                            for (j in 0 until tArr.length()) tagsList.add(tArr.getString(j))
                        }
                        val priority = obj.optString("priority", "Medium")
                        val isRecurring = obj.optBoolean("isRecurring", true)
                        val eventDate = obj.optString("eventDate", "")
                        val costType = obj.optString("costType", "Money")
                        val costValue = obj.optString("costValue", "")
                        val bhLogs = mutableListOf<String>()
                        if (obj.has("logs")) {
                            val logsArr = obj.getJSONArray("logs")
                            for (j in 0 until logsArr.length()) {
                                bhLogs.add(logsArr.getString(j))
                            }
                        }
                        restoredBadHabits.add(
                            BadHabitSpec(
                                id = bhId,
                                name = bhName,
                                avoidType = avoidType,
                                reminderTime = reminderTime,
                                tags = tagsList,
                                priority = priority,
                                isRecurring = isRecurring,
                                eventDate = eventDate,
                                costType = costType,
                                costValue = costValue,
                                logs = bhLogs
                            )
                        )
                    }
                    _badHabits.value = restoredBadHabits
                    saveBadHabitsList(restoredBadHabits)
                }

                getSharedPrefs().edit().putBoolean("net_worth_defaults_populated", true).apply()

                _isSyncing.value = false
                _successMessage.value = "Backup successfully restored! All data has been filled."
                addNotification("Data Imported", "All your offline categories and states have been restored successfully.")
                triggerFakeSync()
            } catch (e: Exception) {
                _isSyncing.value = false
                _authError.value = "Import failed: ${e.message}"
            }
        }
    }

    fun importData() {
        // Fallback import when file picker is unavailable or for automatic default back up files
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = java.io.File(getApplication<Application>().cacheDir, "trackwise_backup.json")
                if (file.exists()) {
                    val content = file.readText()
                    importData(content)
                } else {
                    // Try Download folder
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val pubFile = java.io.File(downloadsDir, "trackwise_backup.json")
                    if (pubFile.exists()) {
                        importData(pubFile.readText())
                    } else {
                        _authError.value = "No backup file found in Cache or Downloads."
                    }
                }
            } catch (e: Exception) {
                _authError.value = "Local import failed: ${e.message}"
            }
        }
    }

    fun importDataFromUri(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val jsonContent = inputStream?.bufferedReader()?.use { it.readText() }
                if (jsonContent != null) {
                    importData(jsonContent)
                } else {
                    _authError.value = "Could not read empty or invalid backup file."
                }
            } catch (e: Exception) {
                _authError.value = "Failed to load backup: ${e.message}"
            }
        }
    }

    fun syncDeviceState() {
        viewModelScope.launch(Dispatchers.Main) {
            _isSyncing.value = true
            _syncMessage.value = "Refreshing..."
            kotlinx.coroutines.delay(1000)
            _isSyncing.value = false
            _syncMessage.value = "Auto-saved offline data"
        }
    }

    init {
        // Persistent Session & Monthly Re-login Check (on the 28th)
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        val savedUserId = prefs.getString("saved_user_id", null)
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        
        // Restore theme preferences
        val savedThemeMode = prefs.getString("saved_theme_mode", "light") ?: "light"
        val savedThemeAccent = prefs.getString("saved_theme_accent", "Default Violet") ?: "Default Violet"
        _themeMode.value = if (savedThemeMode == "auto") "system" else savedThemeMode
        _appThemeSelection.value = savedThemeAccent
        _appFontSize.value = prefs.getString("app_font_size", "Medium") ?: "Medium"
        _appFontStyle.value = prefs.getString("app_font_style", "Default") ?: "Default"

        // Restore background preferences
        _appBgType.value = prefs.getString("app_bg_type", "gradient") ?: "gradient"
        _appBgColor.value = prefs.getString("app_bg_color", "Lavender & Amethyst") ?: "Lavender & Amethyst"
        _appBgGradient.value = prefs.getString("app_bg_gradient", "Sunset Glow") ?: "Sunset Glow"
        _appBgImage.value = prefs.getString("app_bg_image", "https://images.unsplash.com/photo-1557683316-973673baf926?w=800") ?: "https://images.unsplash.com/photo-1557683316-973673baf926?w=800"
        _appBgCustomUri.value = prefs.getString("app_bg_custom_uri", "") ?: ""
        _profileImageUri.value = prefs.getString("profile_image_uri", null)

        // Restore auto-backup preferences
        val savedBackupFreq = prefs.getString("auto_backup_frequency", "none") ?: "none"
        val savedLastBackupTime = prefs.getLong("last_auto_backup_time", 0L)
        _autoBackupFrequency.value = savedBackupFreq
        _lastAutoBackupTime.value = savedLastBackupTime

        // Restore custom folders and tags
        val customFoldersStr = prefs.getString("custom_folders_list", "") ?: ""
        if (customFoldersStr.isNotBlank()) {
            _customFolders.value = customFoldersStr.split(",")
        }
        val customTagsStr = prefs.getString("custom_tags_list", "") ?: ""
        if (customTagsStr.isNotBlank()) {
            _customTags.value = customTagsStr.split(",")
        }
        val deletedFoldersStr = prefs.getString("deleted_folders_list", "") ?: ""
        if (deletedFoldersStr.isNotBlank()) {
            _deletedFolders.value = deletedFoldersStr.split(",")
        }
        val deletedTagsStr = prefs.getString("deleted_tags_list", "") ?: ""
        if (deletedTagsStr.isNotBlank()) {
            _deletedTags.value = deletedTagsStr.split(",")
        }
        loadBadHabits()
        _taskSound.value = prefs.getString("task_sound", "Chime Gentle") ?: "Chime Gentle"
        _alarmSound.value = prefs.getString("alarm_sound", "Reflection") ?: "Reflection"
        _vibrationEnabled.value = prefs.getBoolean("vibration_enabled", true)
        _vibrateOnTaskCompletion.value = prefs.getBoolean("vibrate_on_task_completion", true)
        _vibrateOnSwipe.value = prefs.getBoolean("vibrate_on_swipe", true)
        _vibrateOnNotification.value = prefs.getBoolean("vibrate_on_notification", true)
        
        if (savedUserId != null) {
            val lastActiveTime = prefs.getLong("last_active_timestamp", System.currentTimeMillis())
            val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000L
            if (lastActiveTime > 0L && (System.currentTimeMillis() - lastActiveTime) > thirtyDaysMs) {
                prefs.edit().remove("saved_user_id").apply()
                _authError.value = "Session expired due to 30 days of inactivity. Please log in again."
            } else {
                prefs.edit().putLong("last_active_timestamp", System.currentTimeMillis()).apply()
                viewModelScope.launch(Dispatchers.IO) {
                    val user = repository.findUserById(savedUserId)
                    if (user != null) {
                        _sessionUser.value = user
                        delay(2000)
                        checkAndPerformAutoBackup()
                    }
                }
            }
        }

        // Start background auto-backup checking loop
        viewModelScope.launch(Dispatchers.IO) {
            delay(15000) // 15s initial warm-up delay
            while (true) {
                if (_sessionUser.value != null) {
                    checkAndPerformAutoBackup()
                }
                delay(60000) // check every minute
            }
        }

        // Triggered reminders tracking set
        val triggeredReminders = mutableSetOf<String>()

        // Background reminder checking loop
        viewModelScope.launch(Dispatchers.IO) {
            delay(20000) // Initial warm-up delay
            while (true) {
                try {
                    val todayStr = TrackWiseUtils.getTodayString().take(10)
                    val currentTimeStr = SimpleDateFormat("HH:mm", Locale.US).format(Date())
                    
                    // Check Tasks
                    allTasks.value.forEach { task ->
                        if (task.remindMe && !task.completed && !task.notes.contains("[ARCHIVED]")) {
                            val rDate = task.reminderDate?.take(10) ?: task.deadline.take(10)
                            val rTime = task.reminderTime?.trim()
                            if (rDate == todayStr && rTime == currentTimeStr) {
                                val key = "task-${task.id}-$rDate-$rTime"
                                if (!triggeredReminders.contains(key)) {
                                    triggeredReminders.add(key)
                                    addNotification(
                                        title = "Task Reminder: ${task.title}",
                                        message = "Project: ${task.project} • Priority: ${task.priority.uppercase()}${if (task.description.isNotBlank()) " • " + task.description else ""}",
                                        showSystem = true,
                                        taskId = task.id,
                                        canSnooze = true
                                    )
                                }
                            }
                        }
                    }

                    // Check Habits
                    allHabits.value.forEach { habit ->
                        if (habit.remindMe) {
                            val rDate = habit.reminderDate?.take(10) ?: todayStr
                            val rTime = habit.reminderTime?.trim()
                            if (rDate == todayStr && rTime == currentTimeStr) {
                                val key = "habit-${habit.id}-$rDate-$rTime"
                                if (!triggeredReminders.contains(key)) {
                                    triggeredReminders.add(key)
                                    addNotification(
                                        title = "Habit: ${habit.name}",
                                        message = "Category: ${habit.category} • Streak: ${habit.streak} days${if (habit.quote.isNotBlank()) " • \"" + habit.quote + "\"" else if (habit.notes.isNotBlank()) " • " + habit.notes else ""}",
                                        showSystem = true,
                                        canSnooze = true
                                    )
                                }
                            }
                        }
                    }

                    // Check Wishlist
                    allWishlist.value.forEach { item ->
                        if (item.remindMe && !item.purchased) {
                            val rDate = item.reminderDate?.take(10)
                            val rTime = item.reminderTime?.trim()
                            if (rDate == todayStr && rTime == currentTimeStr) {
                                val key = "wish-${item.id}-$rDate-$rTime"
                                if (!triggeredReminders.contains(key)) {
                                    triggeredReminders.add(key)
                                    addNotification(
                                        title = "Wishlist Reminder: ${item.title}",
                                        message = "Price: ₹${item.price} • Priority: ${item.priority.uppercase()}${if (!item.link.isNullOrBlank()) " • Link available" else ""}",
                                        showSystem = true,
                                        canSnooze = true
                                    )
                                }
                            }
                        }
                    }

                    // Check Occasions
                    allBirthdays.value.forEach { bday ->
                        if (bday.remindMe) {
                            val rDate = bday.reminderDate?.take(10) ?: bday.date.take(10)
                            val rTime = bday.reminderTime?.trim()
                            if (rDate == todayStr && rTime == currentTimeStr) {
                                val key = "bday-${bday.id}-$rDate-$rTime"
                                if (!triggeredReminders.contains(key)) {
                                    triggeredReminders.add(key)
                                    addNotification(
                                        title = "Occasion Reminder: ${bday.name}",
                                        message = "Category: ${bday.category} • Date: ${bday.date}${if (!bday.giftIdea.isNullOrBlank()) " • Gift Idea: " + bday.giftIdea else ""}",
                                        showSystem = true,
                                        canSnooze = true
                                    )
                                }
                            }
                        }
                    }

                    // Check Notes
                    val activeUid = _sessionUser.value?.id ?: allUsers.value.firstOrNull()?.id
                    if (activeUid != null) {
                        val notesList = repository.getAllNotesFlow(activeUid).first()
                        notesList.forEach { note ->
                            val rDate = note.reminderDate?.take(10)
                            val rTime = note.reminderTime?.trim()
                            if (!rDate.isNullOrBlank() && rDate == todayStr && rTime == currentTimeStr) {
                                val key = "note-${note.id}-$rDate-$rTime"
                                if (!triggeredReminders.contains(key)) {
                                    triggeredReminders.add(key)
                                    addNotification(
                                        title = "Note Reminder: ${note.title}",
                                        message = if (note.content.isNotBlank()) note.content.take(100) else "You have a note reminder set for today.",
                                        showSystem = true,
                                        canSnooze = true
                                    )
                                }
                            }
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(30000) // check every 30 seconds
            }
        }

        // Auto-update home screen widget whenever key data states change
        viewModelScope.launch {
            combine(allTasks, allHabits, allFinanceLogs, waterLogs, sessionUser) { _, _, _, _, _ -> Unit }
                .collect {
                    updateAppWidget()
                }
        }
    }
}

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false
)
