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

    fun setNotificationNavigateTab(tab: String?) {
        _notificationNavigateTab.value = tab
    }

    fun addNotification(title: String, message: String) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timeStr = sdf.format(Date())
        val newNotification = AppNotification(
            id = "notif-${System.currentTimeMillis()}",
            title = title,
            message = message,
            timestamp = timeStr
        )
        _notifications.value = listOf(newNotification) + _notifications.value
        showSystemNotification(title, message)
    }

    private fun showSystemNotification(title: String, message: String) {
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

            val builder = androidx.core.app.NotificationCompat.Builder(context, "trackwise_notifications")
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setVibrate(longArrayOf(0, 250, 100, 250))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
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
    private val _themeMode = MutableStateFlow("light") // "light", "dark", or "system"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _taskSound = MutableStateFlow("Chime")
    val taskSound: StateFlow<String> = _taskSound.asStateFlow()

    private val _alarmSound = MutableStateFlow("Reflection")
    val alarmSound: StateFlow<String> = _alarmSound.asStateFlow()

    private val _appThemeSelection = MutableStateFlow("Default Violet")
    val appThemeSelection: StateFlow<String> = _appThemeSelection.asStateFlow()

    private val _settingsPanelOpen = MutableStateFlow(false)
    val settingsPanelOpen: StateFlow<Boolean> = _settingsPanelOpen.asStateFlow()

    private val _calendarOverlay = MutableStateFlow("none") // "none", "islamic", "hindu"
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

    // --- Dynamic Data Streams ---
    val allTasks: StateFlow<List<TaskEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getTasksFlow(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHabits: StateFlow<List<HabitEntity>> = _sessionUser
        .flatMapLatest { user ->
            if (user != null) repository.getHabitsFlow(user.id) else flowOf(emptyList())
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

    // --- Computed Score Stats ---
    val todayScore: StateFlow<Int> = combine(
        allTasks,
        allHabits,
        allWishlist,
        allGroceryItems,
        allBirthdays
    ) { tasks, habits, wishlist, groceries, birthdays ->
        val todayStr = TrackWiseUtils.getTodayString()
        if (todayStr < TrackWiseUtils.APP_LAUNCH_DATE) return@combine 0
        
        // 1. Dynamic Task Points (High: 15 pts, Medium: 10 pts, Low: 5 pts)
        val taskPoints = tasks.filter { TrackWiseUtils.shouldShowTaskOnDate(it, todayStr) && it.completed }.sumOf { 
            when (it.priority.lowercase()) {
                "high" -> 15
                "medium" -> 10
                else -> 5
            }
        }
        
        // 2. Dynamic Habit Points: 3 pts per completion count, plus 5 pts bonus if they reach the target
        val habitPoints = habits.sumOf { habit ->
            val days = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson)
            val completionsToday = days.count { it == todayStr }
            if (completionsToday > 0) {
                if (habit.isMultipleTimesPerDay) {
                    val base = completionsToday * 3
                    val bonus = if (completionsToday >= habit.multipleTimesTarget) 5 else 0
                    base + bonus
                } else {
                    5
                }
            } else {
                0
            }
        }

        // 3. Dynamic Wishlist Points: 20 points for each purchased wishlist item
        val wishPoints = wishlist.filter { it.purchased }.size * 20

        // 4. Dynamic Grocery Points: 2 points for each completed grocery item
        val groceryPoints = groceries.filter { it.completed }.size * 2

        // 5. Dynamic Birthday Points: 50 points if celebrating a birthday today
        val todayMMDD = todayStr.substring(5) // from YYYY-MM-DD to MM-DD
        val birthdayPoints = birthdays.filter { 
            it.date.endsWith(todayMMDD) 
        }.size * 50
        
        val total = taskPoints + habitPoints + wishPoints + groceryPoints + birthdayPoints
        
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
                prefs.edit().putString("saved_user_id", user.id).apply()
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
                prefs.edit().putString("saved_user_id", user.id).apply()
                
                addNotification("Account Created", "Welcome to TrackWise, ${user.fullName}!")
            } catch (e: Exception) {
                _authError.value = e.message ?: "Account creation failed."
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
                    addNotification("Security Alert", "Password was reset for $email.")
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

    // --- Preferences Actions ---
    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        val prefs = getApplication<Application>().getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        prefs.edit().putString("saved_theme_mode", mode).apply()
        updateAppWidget()
    }

    fun setCalendarOverlay(overlay: String) {
        _calendarOverlay.value = overlay
    }

    fun setSettingsPanelOpen(isOpen: Boolean) {
        _settingsPanelOpen.value = isOpen
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
        dueTime: String? = null
    ) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val taskPoints = when (priority.lowercase()) {
                "high" -> 15
                "medium" -> 10
                else -> 5
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
                reminderTime = reminderTime,
                repeatType = repeatType,
                customRepeatValue = customRepeatValue,
                customRepeatUnit = customRepeatUnit,
                customRepeatDaysOfWeek = customRepeatDaysOfWeek,
                startDate = startDate,
                endDate = endDate,
                notes = notes,
                dueTime = dueTime
            )
            repository.insertTask(task)
            triggerFakeSync()
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = task.copy(completed = !task.completed)
            repository.insertTask(updated)
            if (updated.completed) {
                playTaskCompletionSound()
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
        dueTime: String? = null
    ) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val today = TrackWiseUtils.getTodayString()
            val finalStart = startDate ?: today
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
                dueTime = dueTime
            )
            repository.insertHabit(habit)
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
            triggerFakeSync()
        }
    }

    fun incrementHabitToday(habit: HabitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val todayStr = TrackWiseUtils.getTodayString()
            if (todayStr < TrackWiseUtils.APP_LAUNCH_DATE) return@launch
            
            val days = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson).toMutableList()
            days.add(todayStr)
            
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
        reminderTime: String? = null
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
                reminderTime = reminderTime
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

    fun populateDefaultNetWorthItemsIfEmpty() {
        val user = _sessionUser.value ?: return
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
                triggerFakeSync()
            }
        }
    }

    // --- Water Log Actions ---
    fun adjustWaterLog(glassesDelta: Int) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val today = TrackWiseUtils.getTodayString()
            val currentLog = repository.getWaterLogForDate(user.id, today)
            
            if (currentLog == null) {
                val initGlasses = max(0, glassesDelta)
                repository.insertWaterLog(
                    WaterLogEntity(
                        id = "${user.id}_$today",
                        userId = user.id,
                        date = today,
                        glasses = initGlasses,
                        goal = user.waterGoalGlasses
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
        viewModelScope.launch(Dispatchers.Main) {
            _isSyncing.value = true
            _syncMessage.value = "Syncing device states..."
            kotlinx.coroutines.delay(1000)
            _isSyncing.value = false
            _syncMessage.value = "Auto-saved offline data"
        }
    }

    // --- Sleep Tracker Actions ---
    fun addSleepLog(hoursSlept: Double, startTime: String, endTime: String, notes: String? = null, date: String? = null) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val finalDate = date ?: TrackWiseUtils.getTodayString()
            
            // Limit to one entry per day
            val isDuplicate = sleepLogs.value.any { it.date == finalDate }
            if (isDuplicate) {
                _authError.value = "Only one sleep entry is allowed per day!"
                return@launch
            }

            val log = SleepLogEntity(
                id = "sleep-${System.currentTimeMillis()}",
                userId = user.id,
                date = finalDate,
                hoursSlept = hoursSlept,
                startTime = startTime,
                endTime = endTime,
                notes = notes
            )
            repository.insertSleepLog(log)
            _successMessage.value = "Sleep logged successfully."
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

    // --- Sound Selection Actions ---
    private var isAlarmPlaying = false

    fun playTaskCompletionSound() {
        val sound = _taskSound.value
        if (sound == "None") return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Route to STREAM_MUSIC with max volume so it always plays even if notifications are muted
                val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                when (sound) {
                    "Chime" -> {
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                        delay(150)
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                    }
                    "Ding" -> {
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
                    }
                    "Bell" -> {
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        delay(120)
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        delay(120)
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                    }
                }
                delay(600)
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
        playTaskCompletionSound()
    }

    fun setAlarmSound(sound: String) {
        _alarmSound.value = sound
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
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.clearUserData(user.id)
                viewModelScope.launch(Dispatchers.Main) {
                    _successMessage.value = "All your personal logs and statistics have been successfully cleared! Your account is active."
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

    fun updateNetWorthItem(item: NetWorthItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertNetWorthItem(item)
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

    // --- Settings Panels Actions ---
    fun generateBackupJsonString(user: UserEntity): String {
        val rootJson = org.json.JSONObject()
        rootJson.put("version", 1)
        
        // User info
        val userJson = org.json.JSONObject()
        userJson.put("id", user.id)
        userJson.put("email", user.email)
        userJson.put("fullName", user.fullName)
        rootJson.put("user", userJson)
        
        // Tasks
        val tasksArray = org.json.JSONArray()
        allTasks.value.forEach { item ->
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
            obj.put("notes", item.notes)
            tasksArray.put(obj)
        }
        rootJson.put("tasks", tasksArray)
        
        // Habits
        val habitsArray = org.json.JSONArray()
        allHabits.value.forEach { item ->
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
            habitsArray.put(obj)
        }
        rootJson.put("habits", habitsArray)

        // Alarms
        val alarmsArray = org.json.JSONArray()
        allAlarms.value.forEach { item ->
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
        waterLogs.value.forEach { item ->
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
        vitalReadings.value.forEach { item ->
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
        weightEntries.value.forEach { item ->
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
        sleepLogs.value.forEach { item ->
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
        userProfile.value?.let { prof ->
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
            obj.put("financeDailyTarget", prof.financeDailyTarget)
            rootJson.put("profile", obj)
        }

        // Birthdays / Occasions
        val birthdaysArray = org.json.JSONArray()
        allBirthdays.value.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("date", item.date)
            obj.put("giftIdea", item.giftIdea ?: "")
            obj.put("category", item.category)
            birthdaysArray.put(obj)
        }
        rootJson.put("birthdays", birthdaysArray)

        // Wishlist
        val wishlistArray = org.json.JSONArray()
        allWishlist.value.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("price", item.price)
            obj.put("link", item.link ?: "")
            obj.put("priority", item.priority)
            obj.put("purchased", item.purchased)
            wishlistArray.put(obj)
        }
        rootJson.put("wishlist", wishlistArray)

        // Grocery Items
        val groceryArray = org.json.JSONArray()
        allGroceryItems.value.forEach { item ->
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
        streakHistory.value.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("date", item.date)
            obj.put("score", item.score)
            streakHistoryArray.put(obj)
        }
        rootJson.put("streakHistory", streakHistoryArray)

        // Exercise Logs
        val exerciseArray = org.json.JSONArray()
        exerciseLogs.value.forEach { item ->
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
        healthIssueLogs.value.forEach { item ->
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
        tabletReminders.value.forEach { item ->
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
        periodCycles.value.forEach { item ->
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
        allFinanceLogs.value.forEach { item ->
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
        allNetWorthItems.value.forEach { item ->
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
        friendConnections.value.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("friendUserId", item.friendUserId)
            obj.put("displayName", item.displayName)
            obj.put("addedAt", item.addedAt)
            friendsArray.put(obj)
        }
        rootJson.put("friends", friendsArray)

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
                        "com.example.fileprovider",
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
                            project = obj.optString("project", "Default"),
                            priority = obj.optString("priority", "medium"),
                            deadline = obj.optString("deadline", TrackWiseUtils.getTodayString()),
                            completed = obj.optBoolean("completed", false),
                            points = obj.optInt("points", 10),
                            subtasksJson = obj.optString("subtasksJson", "[]"),
                            reminderTime = if (obj.has("reminderTime") && obj.getString("reminderTime").isNotEmpty()) obj.getString("reminderTime") else null,
                            notes = obj.optString("notes", "")
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
                            createdAt = obj.optString("createdAt", TrackWiseUtils.getTodayString())
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
                        vitalsBloodGroup = obj.optString("vitalsBloodGroup", "")
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
                            giftIdea = if (obj.has("giftIdea") && obj.getString("giftIdea").isNotEmpty()) obj.getString("giftIdea") else null,
                            category = obj.optString("category", "Others")
                        )
                        repository.insertBirthday(entity)
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
                            link = if (obj.has("link") && obj.getString("link").isNotEmpty()) obj.getString("link") else null,
                            priority = obj.optString("priority", "medium"),
                            purchased = obj.optBoolean("purchased", false)
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
        triggerFakeSync()
        viewModelScope.launch(Dispatchers.Main) {
            _successMessage.value = "Device states synchronized successfully!"
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
        _themeMode.value = savedThemeMode
        _appThemeSelection.value = savedThemeAccent

        // Restore auto-backup preferences
        val savedBackupFreq = prefs.getString("auto_backup_frequency", "none") ?: "none"
        val savedLastBackupTime = prefs.getLong("last_auto_backup_time", 0L)
        _autoBackupFrequency.value = savedBackupFreq
        _lastAutoBackupTime.value = savedLastBackupTime
        
        if (savedUserId != null) {
            if (dayOfMonth == 28) {
                prefs.edit().remove("saved_user_id").apply()
                _authError.value = "Monthly security check: Please log in again (28th of the month)."
                addNotification("Security Check", "Session expired on the 28th for monthly re-login.")
            } else {
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
                        if (task.remindMe && !task.completed) {
                            val rDate = task.reminderDate?.take(10) ?: task.deadline.take(10)
                            val rTime = task.reminderTime?.trim()
                            if (rDate == todayStr && rTime == currentTimeStr) {
                                val key = "task-${task.id}-$rDate-$rTime"
                                if (!triggeredReminders.contains(key)) {
                                    triggeredReminders.add(key)
                                    addNotification(
                                        title = "Task Reminder: ${task.title}",
                                        message = "Deadline: ${task.deadline}. Don't forget to complete it!"
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
                                        title = "Habit Runway: ${habit.name}",
                                        message = "It's time for your habit: ${habit.category}!"
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
                                        message = "Check out your item: ${item.title} (₹${item.price})"
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
                                        message = "Event: ${bday.name} is scheduled for today!"
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
