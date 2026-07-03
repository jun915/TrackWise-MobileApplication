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

    fun addNotification(title: String, message: String) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
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
            
            val builder = androidx.core.app.NotificationCompat.Builder(context, "trackwise_notifications")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setVibrate(longArrayOf(0, 250, 100, 250))
                .setAutoCancel(true)
                
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    init {
        // Persistent Session & Monthly Re-login Check (on the 28th)
        val prefs = application.getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        val savedUserId = prefs.getString("saved_user_id", null)
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        
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
                        addNotification("Welcome Back!", "Automated secure login successful.")
                    }
                }
            }
        }
    }

    // --- UI App Preferences ---
    private val _themeMode = MutableStateFlow("light") // "light", "dark", or "system"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _taskSound = MutableStateFlow("Chime")
    val taskSound: StateFlow<String> = _taskSound.asStateFlow()

    private val _alarmSound = MutableStateFlow("Morning Birds")
    val alarmSound: StateFlow<String> = _alarmSound.asStateFlow()

    private val _appThemeSelection = MutableStateFlow("Default Violet")
    val appThemeSelection: StateFlow<String> = _appThemeSelection.asStateFlow()

    private val _settingsPanelOpen = MutableStateFlow(false)
    val settingsPanelOpen: StateFlow<Boolean> = _settingsPanelOpen.asStateFlow()

    private val _calendarOverlay = MutableStateFlow("none") // "none", "islamic", "hindu"
    val calendarOverlay: StateFlow<String> = _calendarOverlay.asStateFlow()

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
    val todayScore: StateFlow<Int> = combine(allTasks, allHabits) { tasks, habits ->
        val todayStr = TrackWiseUtils.getTodayString()
        if (todayStr < TrackWiseUtils.APP_LAUNCH_DATE) return@combine 0
        
        val taskPoints = tasks.filter { it.deadline == todayStr && it.completed }.sumOf { it.points }
        val habitPoints = habits.filter {
            val days = TrackWiseUtils.deserializeStringList(it.daysCompletedJson)
            days.contains(todayStr)
        }.size * 2
        
        val total = taskPoints + habitPoints
        
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
                
                addNotification("User Logged In", "Successfully logged in as ${user.fullName}.")
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

    // --- Tasks Actions ---
    fun addTask(title: String, description: String, project: String, priority: String, points: Int, deadline: String, reminderTime: String?) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val task = TaskEntity(
                id = "task-${System.currentTimeMillis()}",
                userId = user.id,
                title = title,
                description = description,
                project = project,
                priority = priority,
                deadline = deadline,
                completed = false,
                points = points,
                reminderTime = reminderTime
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

    fun addSubTask(task: TaskEntity, subTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSubTasks = TrackWiseUtils.deserializeSubTasks(task.subtasksJson).toMutableList()
            val newSub = SubTask(
                id = "sub-${System.currentTimeMillis()}",
                title = subTitle,
                completed = false
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
    fun addHabit(name: String, category: String) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val today = TrackWiseUtils.getTodayString()
            val habit = HabitEntity(
                id = "habit-${System.currentTimeMillis()}",
                userId = user.id,
                name = name,
                category = category,
                createdAt = today
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

    fun deleteHabit(habitId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHabit(habitId)
            triggerFakeSync()
        }
    }

    // --- Birthdays Actions ---
    fun addBirthday(name: String, date: String, giftIdea: String?) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val birthday = BirthdayEntity(
                id = "birthday-${System.currentTimeMillis()}",
                userId = user.id,
                name = name,
                date = date,
                giftIdea = giftIdea
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

    // --- Wishlist Actions ---
    fun addWishItem(title: String, price: Double, link: String?, priority: String) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val item = WishItemEntity(
                id = "wish-${System.currentTimeMillis()}",
                userId = user.id,
                title = title,
                price = price,
                link = link,
                priority = priority,
                purchased = false
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
    fun addGroceryItem(name: String, quantity: String, category: String) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val item = GroceryItemEntity(
                id = "grocery-${System.currentTimeMillis()}-${(1000..9999).random()}",
                userId = user.id,
                name = name,
                quantity = quantity,
                completed = false,
                category = category
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
    fun addFinanceLog(type: String, category: String, title: String, amount: Double, notes: String?, date: String = TrackWiseUtils.getTodayString()) {
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
                notes = notes
            )
            repository.insertFinanceLog(log)
            triggerFakeSync()
        }
    }

    fun deleteFinanceLog(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFinanceLog(id)
            triggerFakeSync()
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
    fun logWeight(weight: Double, notes: String?) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val today = TrackWiseUtils.getTodayString()
            val time = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            val entry = WeightEntryEntity(
                id = "weight-${System.currentTimeMillis()}",
                userId = user.id,
                date = today,
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
    fun logVital(type: String, value: String, context: String?, notes: String?) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val today = TrackWiseUtils.getTodayString()
            val time = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            val entry = VitalReadingEntity(
                id = "vital-${System.currentTimeMillis()}",
                userId = user.id,
                type = type,
                date = today,
                time = time,
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
    fun logExercise(type: String, duration: Int, completed: Boolean, notes: String?) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val today = TrackWiseUtils.getTodayString()
            val time = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            val entry = ExerciseLogEntity(
                id = "exercise-${System.currentTimeMillis()}",
                userId = user.id,
                date = today,
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
    fun logHealthIssue(issueId: String, issueName: String, severity: String, notes: String?) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val today = TrackWiseUtils.getTodayString()
            val time = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            val entry = HealthIssueLogEntity(
                id = "issue-${System.currentTimeMillis()}",
                userId = user.id,
                date = today,
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
    fun addSleepLog(hoursSlept: Double, startTime: String, endTime: String, notes: String? = null) {
        val user = _sessionUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val date = TrackWiseUtils.getTodayString()
            val log = SleepLogEntity(
                id = "sleep-${System.currentTimeMillis()}",
                userId = user.id,
                date = date,
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
                        "Morning Birds" -> {
                            // Beautiful cascading high-pitch birdsong
                            val birdsong = listOf(
                                ToneGenerator.TONE_DTMF_1, ToneGenerator.TONE_DTMF_4, ToneGenerator.TONE_DTMF_7,
                                ToneGenerator.TONE_DTMF_2, ToneGenerator.TONE_DTMF_5, ToneGenerator.TONE_DTMF_8,
                                ToneGenerator.TONE_DTMF_3, ToneGenerator.TONE_DTMF_6, ToneGenerator.TONE_DTMF_9
                            )
                            for (note in birdsong) {
                                if (!isAlarmPlaying) break
                                tg.startTone(note, 60)
                                delay(90)
                            }
                            delay(800)
                        }
                        "Digital Beep" -> {
                            // Rhythmic Casio watch melody
                            val casioNotes = listOf(
                                ToneGenerator.TONE_CDMA_PIP, ToneGenerator.TONE_CDMA_PIP,
                                ToneGenerator.TONE_CDMA_PIP, ToneGenerator.TONE_CDMA_PIP,
                                ToneGenerator.TONE_CDMA_PIP
                            )
                            for (note in casioNotes) {
                                if (!isAlarmPlaying) break
                                tg.startTone(note, 100)
                                delay(180)
                            }
                            delay(600)
                        }
                        "Loud Siren" -> {
                            // Alternating emergency siren waves
                            for (i in 1..4) {
                                if (!isAlarmPlaying) break
                                tg.startTone(ToneGenerator.TONE_SUP_DIAL, 250)
                                delay(280)
                                tg.startTone(ToneGenerator.TONE_SUP_ERROR, 250)
                                delay(280)
                            }
                            delay(300)
                        }
                        "Classic Bell" -> {
                            // Royal Westminster Quarters clock chime melody (8 notes)
                            val westminster = listOf(
                                ToneGenerator.TONE_DTMF_3, ToneGenerator.TONE_DTMF_1, ToneGenerator.TONE_DTMF_2, ToneGenerator.TONE_DTMF_5,
                                ToneGenerator.TONE_DTMF_5, ToneGenerator.TONE_DTMF_2, ToneGenerator.TONE_DTMF_3, ToneGenerator.TONE_DTMF_1
                            )
                            for (note in westminster) {
                                if (!isAlarmPlaying) break
                                tg.startTone(note, 250)
                                delay(350)
                            }
                            delay(1200)
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
                    "Morning Birds" -> {
                        tg.startTone(ToneGenerator.TONE_DTMF_4, 80)
                        delay(100)
                        tg.startTone(ToneGenerator.TONE_DTMF_7, 80)
                    }
                    "Digital Beep" -> {
                        tg.startTone(ToneGenerator.TONE_CDMA_PIP, 100)
                        delay(120)
                        tg.startTone(ToneGenerator.TONE_CDMA_PIP, 100)
                    }
                    "Loud Siren" -> {
                        tg.startTone(ToneGenerator.TONE_SUP_DIAL, 200)
                        delay(220)
                        tg.startTone(ToneGenerator.TONE_SUP_ERROR, 200)
                    }
                    "Classic Bell" -> {
                        tg.startTone(ToneGenerator.TONE_DTMF_3, 200)
                        delay(220)
                        tg.startTone(ToneGenerator.TONE_DTMF_1, 200)
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
                    obj.put("permanentStreet", prof.permanentStreet)
                    obj.put("mobileNumber", prof.mobileNumber)
                    obj.put("alternatePhone", prof.alternatePhone)
                    obj.put("emailAddress", prof.emailAddress)
                    obj.put("emergencyName", prof.emergencyName)
                    obj.put("emergencyRelationship", prof.emergencyRelationship)
                    obj.put("emergencyPhone", prof.emergencyPhone)
                    obj.put("alternateEmergencyPhone", prof.alternateEmergencyPhone)
                    rootJson.put("profile", obj)
                }

                val jsonStr = rootJson.toString(2)
                
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
                            reminderTime = if (obj.has("reminderTime") && obj.getString("reminderTime").isNotEmpty()) obj.getString("reminderTime") else null
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
}

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false
)
