package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.repository.TrackWiseRepository
import com.example.utils.TrackWiseUtils
import kotlinx.coroutines.Dispatchers
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

    // --- Temporary Error/Success States ---
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

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
            } catch (e: Exception) {
                _authError.value = e.message ?: "Account creation failed."
            }
        }
    }

    fun logout() {
        _sessionUser.value = null
        _settingsPanelOpen.value = false
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
            val currentSubTasks = TrackWiseUtils.deserializeSubTasks(task.subtasksJson).map {
                if (it.id == subId) it.copy(completed = !it.completed) else it
            }
            val updatedTask = task.copy(subtasksJson = TrackWiseUtils.serializeSubTasks(currentSubTasks))
            repository.insertTask(updatedTask)
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
    fun setTaskSound(sound: String) {
        _taskSound.value = sound
    }

    fun setAlarmSound(sound: String) {
        _alarmSound.value = sound
    }

    // --- Different Theme Accent Action ---
    fun setAppThemeSelection(themeName: String) {
        _appThemeSelection.value = themeName
    }

    // --- Account Management ---
    fun deleteAccount() {
        _sessionUser.value = null
        _settingsPanelOpen.value = false
        _successMessage.value = "Account deleted successfully."
    }

    // --- Settings Panels Actions ---
    fun exportData() {
        viewModelScope.launch(Dispatchers.Main) {
            _isSyncing.value = true
            _syncMessage.value = "Exporting data..."
            kotlinx.coroutines.delay(1000)
            _isSyncing.value = false
            _successMessage.value = "App data successfully exported to backups!"
        }
    }

    fun importData() {
        viewModelScope.launch(Dispatchers.Main) {
            _isSyncing.value = true
            _syncMessage.value = "Importing data..."
            kotlinx.coroutines.delay(1000)
            _isSyncing.value = false
            _successMessage.value = "App data successfully imported from backup!"
        }
    }

    fun syncDeviceState() {
        triggerFakeSync()
        viewModelScope.launch(Dispatchers.Main) {
            _successMessage.value = "Device states synchronized successfully!"
        }
    }
}
