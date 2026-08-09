package com.example.repository

import com.example.data.*
import com.example.utils.SecurityUtils
import com.example.utils.TrackWiseUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class TrackWiseRepository(private val dao: TrackWiseDao) {

    // --- Authentication ---
    fun getAllUsersFlow(): Flow<List<UserEntity>> = dao.getAllUsersFlow()

    suspend fun findUserByEmail(email: String): UserEntity? {
        val cleanEmail = email.lowercase().trim()
        val found = dao.getUserByEmail(cleanEmail)
        
        val isSyedUser = cleanEmail == "syed@syed.com" || cleanEmail == "syed"
        val isJuUser = cleanEmail == "ju" || cleanEmail == "ju@gmail.com"
        
        if (found == null && (isSyedUser || isJuUser)) {
            val userId = if (isSyedUser) "user-default-syed" else "user-default-ju"
            val targetEmail = if (isSyedUser) "syed@syed.com" else "ju@gmail.com"
            val passwordHash = SecurityUtils.hashPassword("Syed@123")
            val newUser = UserEntity(
                id = userId,
                email = targetEmail,
                passwordHash = passwordHash,
                fullName = if (isSyedUser) "Syed Junaid" else "Junaid Ju",
                dob = "15/08/2000",
                gender = "male",
                waterGoalGlasses = 8,
                religion = "islam",
                phone = "9159159150"
            )
            dao.insertUser(newUser)
            seedDemoData(userId)
            
            val profile = dao.getUserProfile(userId)
            if (profile != null) {
                dao.insertUserProfile(
                    profile.copy(
                        firstName = if (isSyedUser) "Syed" else "Junaid",
                        lastName = if (isSyedUser) "Junaid" else "Ju",
                        dob = "15/08/2000",
                        gender = "Male",
                        religion = "Islam"
                    )
                )
            } else {
                dao.insertUserProfile(
                    UserProfileEntity(
                        userId = userId,
                        firstName = if (isSyedUser) "Syed" else "Junaid",
                        lastName = if (isSyedUser) "Junaid" else "Ju",
                        dob = "15/08/2000",
                        gender = "Male",
                        religion = "Islam"
                    )
                )
            }
            return newUser
        }
        
        if (found == null && cleanEmail == "syed") {
            return dao.getUserByEmail("syed@syed.com")
        }
        if (found == null && cleanEmail == "ju") {
            return dao.getUserByEmail("ju@gmail.com")
        }
        
        return found
    }

    suspend fun findUserById(userId: String): UserEntity? {
        val found = dao.getUserById(userId)
        if (found == null && (userId == "user-default-ju" || userId == "user-default-syed")) {
            if (userId == "user-default-syed") {
                findUserByEmail("syed@syed.com")
            } else {
                findUserByEmail("ju@gmail.com")
            }
            return dao.getUserById(userId)
        }
        return found
    }

    suspend fun signUp(email: String, passwordRaw: String, fullName: String): UserEntity {
        val existing = findUserByEmail(email)
        if (existing != null) {
            throw Exception("An account with this email already exists.")
        }
        val userId = "user-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(6)}"
        val passwordHash = SecurityUtils.hashPassword(passwordRaw)
        val newUser = UserEntity(
            id = userId,
            email = email.lowercase().trim(),
            passwordHash = passwordHash,
            fullName = fullName,
            waterGoalGlasses = 8
        )
        dao.insertUser(newUser)
        
        // Seed initial data for this user
        seedDemoData(userId)
        
        return newUser
    }

    suspend fun login(email: String, passwordRaw: String): UserEntity {
        val cleanEmail = email.lowercase().trim()
        var user = findUserByEmail(cleanEmail)
        
        if (user == null && (cleanEmail == "syed" || cleanEmail == "syed@syed.com")) {
            user = findUserByEmail("syed@syed.com")
        }
        if (user == null && (cleanEmail == "ju" || cleanEmail == "ju@gmail.com")) {
            user = findUserByEmail("ju@gmail.com")
        }
        
        if (user == null) {
            throw Exception("No account found with this email.")
        }
        
        val hash = SecurityUtils.hashPassword(passwordRaw)
        
        // Special case for demo / test user accounts: Syed / Ju
        if (user.id == "user-default-syed" || user.id == "user-default-ju" || user.email == "syed@syed.com" || user.email == "ju@gmail.com") {
            val isDemoPasswordValid = passwordRaw == "Syed@123" || passwordRaw == "Syed" || passwordRaw == "syed" || passwordRaw == "1234567890" || user.passwordHash == hash
            if (isDemoPasswordValid) {
                if (user.passwordHash != hash) {
                    val updatedUser = user.copy(passwordHash = hash)
                    dao.insertUser(updatedUser)
                    seedDemoData(updatedUser.id)
                    return updatedUser
                }
                seedDemoData(user.id)
                return user
            } else {
                throw Exception("Incorrect password.")
            }
        } else {
            if (user.passwordHash != hash) {
                throw Exception("Incorrect password.")
            }
        }
        
        seedDemoData(user.id)
        return user
    }

    suspend fun updateUserProfile(user: UserEntity) {
        dao.insertUser(user)
        
        // Also update water log goals for today to keep them in sync
        val today = TrackWiseUtils.getTodayString()
        val existingWater = dao.getWaterLogForDate(user.id, today)
        if (existingWater == null) {
            dao.insertWaterLog(
                WaterLogEntity(
                    id = "${user.id}_$today",
                    userId = user.id,
                    date = today,
                    glasses = 0,
                    goal = user.waterGoalGlasses
                )
            )
        } else {
            dao.insertWaterLog(existingWater.copy(goal = user.waterGoalGlasses))
        }
    }

    // --- Tasks ---
    fun getTasksFlow(userId: String): Flow<List<TaskEntity>> = dao.getTasksForUserFlow(userId)

    suspend fun insertTask(task: TaskEntity) {
        dao.insertTask(task)
    }

    suspend fun deleteTask(taskId: String) {
        dao.deleteTaskById(taskId)
    }

    // --- Habits ---
    fun getHabitsFlow(userId: String): Flow<List<HabitEntity>> = dao.getHabitsForUserFlow(userId)

    fun getAllHabitsFlow(): Flow<List<HabitEntity>> = dao.getAllHabitsFlow()

    suspend fun insertHabit(habit: HabitEntity) {
        dao.insertHabit(habit)
    }

    suspend fun deleteHabit(habitId: String) {
        dao.deleteHabitById(habitId)
    }

    // --- Birthdays ---
    fun getBirthdaysFlow(userId: String): Flow<List<BirthdayEntity>> = dao.getBirthdaysForUserFlow(userId)

    suspend fun insertBirthday(birthday: BirthdayEntity) {
        dao.insertBirthday(birthday)
    }

    suspend fun deleteBirthday(birthdayId: String) {
        dao.deleteBirthdayById(birthdayId)
    }

    suspend fun clearBirthdaysForUser(userId: String) {
        dao.clearBirthdaysForUser(userId)
    }

    // --- Wishlist ---
    fun getWishlistFlow(userId: String): Flow<List<WishItemEntity>> = dao.getWishlistForUserFlow(userId)

    suspend fun insertWishItem(item: WishItemEntity) {
        dao.insertWishItem(item)
    }

    suspend fun deleteWishItem(itemId: String) {
        dao.deleteWishItemById(itemId)
    }

    // --- Streak History ---
    fun getStreakHistoryFlow(userId: String): Flow<List<StreakHistoryEntity>> = dao.getStreakHistoryForUserFlow(userId)

    suspend fun updateStreakHistory(userId: String, date: String, score: Int) {
        if (date < TrackWiseUtils.APP_LAUNCH_DATE) return
        val id = "${userId}_$date"
        dao.insertStreakHistory(
            StreakHistoryEntity(
                id = id,
                userId = userId,
                date = date,
                score = score
            )
        )
    }

    suspend fun insertStreakHistory(history: StreakHistoryEntity) {
        dao.insertStreakHistory(history)
    }

    // --- Weight Entries ---
    fun getWeightEntriesFlow(userId: String): Flow<List<WeightEntryEntity>> = dao.getWeightEntriesForUserFlow(userId)

    suspend fun insertWeightEntry(entry: WeightEntryEntity) {
        dao.insertWeightEntry(entry)
        // Sync weight to profile to keep things up to date
        val user = dao.getUserById(entry.userId)
        if (user != null) {
            dao.insertUser(user.copy(weightKg = entry.weightKg))
        }
    }

    suspend fun deleteWeightEntry(id: String) {
        dao.deleteWeightEntryById(id)
    }

    // --- Vitals ---
    fun getVitalsFlow(userId: String): Flow<List<VitalReadingEntity>> = dao.getVitalReadingsForUserFlow(userId)

    suspend fun insertVitalReading(reading: VitalReadingEntity) {
        dao.insertVitalReading(reading)
    }

    suspend fun deleteVitalReading(id: String) {
        dao.deleteVitalReadingById(id)
    }

    // --- Water ---
    fun getWaterLogsFlow(userId: String): Flow<List<WaterLogEntity>> = dao.getWaterLogsForUserFlow(userId)

    suspend fun getWaterLogForDate(userId: String, date: String): WaterLogEntity? {
        return dao.getWaterLogForDate(userId, date)
    }

    suspend fun insertWaterLog(waterLog: WaterLogEntity) {
        dao.insertWaterLog(waterLog)
    }

    // --- Exercises ---
    fun getExerciseLogsFlow(userId: String): Flow<List<ExerciseLogEntity>> = dao.getExerciseLogsForUserFlow(userId)

    suspend fun insertExerciseLog(log: ExerciseLogEntity) {
        dao.insertExerciseLog(log)
    }

    suspend fun deleteExerciseLog(id: String) {
        dao.deleteExerciseLogById(id)
    }

    // --- Health Issues ---
    fun getHealthIssueLogsFlow(userId: String): Flow<List<HealthIssueLogEntity>> = dao.getHealthIssueLogsForUserFlow(userId)

    suspend fun insertHealthIssueLog(log: HealthIssueLogEntity) {
        dao.insertHealthIssueLog(log)
    }

    suspend fun deleteHealthIssueLog(id: String) {
        dao.deleteHealthIssueLogById(id)
    }

    // --- Friends ---
    fun getFriendsFlow(userId: String): Flow<List<FriendConnectionEntity>> = dao.getFriendsForUserFlow(userId)

    suspend fun addFriend(userId: String, friendEmail: String): FriendConnectionEntity {
        val normalizedEmail = friendEmail.lowercase().trim()
        val friendUser = findUserByEmail(normalizedEmail) ?: throw Exception("No TrackWise account found with that email. They must sign up on this device first.")
        if (friendUser.id == userId) {
            throw Exception("You cannot add yourself as a friend.")
        }
        
        // Add bidirectional friend
        val addedAt = TrackWiseUtils.getTodayString()
        
        val friendship1 = FriendConnectionEntity(
            id = "${userId}_${friendUser.id}",
            userId = userId,
            friendUserId = friendUser.id,
            displayName = friendUser.fullName.split(" ").firstOrNull() ?: friendUser.fullName,
            addedAt = addedAt
        )
        val friendship2 = FriendConnectionEntity(
            id = "${friendUser.id}_$userId",
            userId = friendUser.id,
            friendUserId = userId,
            displayName = (dao.getUserById(userId)?.fullName ?: "User").split(" ").firstOrNull() ?: "User",
            addedAt = addedAt
        )
        
        dao.insertFriend(friendship1)
        dao.insertFriend(friendship2)
        
        return friendship1
    }

    suspend fun deleteFriend(userId: String, friendUserId: String) {
        dao.deleteFriendById("${userId}_$friendUserId")
        dao.deleteFriendById("${friendUserId}_$userId")
    }

    suspend fun insertFriend(friend: FriendConnectionEntity) {
        dao.insertFriend(friend)
    }

    // --- Seed Demo Data ---
    private suspend fun seedDemoData(userId: String) {
        val today = TrackWiseUtils.getTodayString()
        
        // 1. Tasks
        val existingTasks = dao.getTasksForUser(userId)
        if (existingTasks.isEmpty()) {
            dao.insertTask(
                TaskEntity(
                    id = "task_demo_1",
                    userId = userId,
                    title = "Complete Quarterly Planning & Goals",
                    description = "Review progress and set goals for the next quarter.",
                    project = "Work",
                    priority = "high",
                    deadline = today,
                    completed = false,
                    points = 20,
                    reminderTime = "09:00",
                    remindMe = true,
                    notes = "[PINNED]"
                )
            )
            dao.insertTask(
                TaskEntity(
                    id = "task_demo_2",
                    userId = userId,
                    title = "Morning Workout & Yoga Flow",
                    description = "30 mins cardio and core exercises.",
                    project = "Health",
                    priority = "medium",
                    deadline = today,
                    completed = false,
                    points = 15,
                    reminderTime = "07:30",
                    notes = ""
                )
            )
            dao.insertTask(
                TaskEntity(
                    id = "task_demo_3",
                    userId = userId,
                    title = "Read 20 pages of Book",
                    description = "Focus on personal development reading.",
                    project = "Learning",
                    priority = "low",
                    deadline = today,
                    completed = false,
                    points = 10,
                    notes = ""
                )
            )
        }

        // 2. Habits
        val existingHabits = dao.getHabitsForUser(userId)
        if (existingHabits.isEmpty()) {
            dao.insertHabit(
                HabitEntity(
                    id = "habit_demo_1",
                    userId = userId,
                    name = "Hydration: Drink 8 Glasses",
                    category = "Wellness",
                    frequency = "daily",
                    createdAt = today,
                    isMultipleTimesPerDay = true,
                    multipleTimesTarget = 8,
                    reminderTime = "08:00 AM",
                    icon = "💧",
                    quote = "Stay hydrated, stay energized!",
                    notes = "[PINNED]"
                )
            )
            dao.insertHabit(
                HabitEntity(
                    id = "habit_demo_2",
                    userId = userId,
                    name = "Daily Mindfulness Meditation",
                    category = "Mindfulness",
                    frequency = "daily",
                    createdAt = today,
                    reminderTime = "10:00 PM",
                    icon = "🧘",
                    quote = "Peace comes from within."
                )
            )
        }

        // 3. Finance Logs
        val existingFinance = dao.getFinanceLogsForUser(userId)
        if (existingFinance.isEmpty()) {
            dao.insertFinanceLog(
                FinanceLogEntity(
                    id = "fin_demo_1",
                    userId = userId,
                    date = today,
                    type = "income",
                    category = "Salary",
                    title = "Monthly Salary Deposit",
                    amount = 4500.0,
                    notes = "Monthly payroll income"
                )
            )
            dao.insertFinanceLog(
                FinanceLogEntity(
                    id = "fin_demo_2",
                    userId = userId,
                    date = today,
                    type = "expense",
                    category = "Groceries & Supermarket",
                    title = "Organic Grocery Shopping",
                    amount = 135.50,
                    notes = "Weekly grocery run"
                )
            )
            dao.insertFinanceLog(
                FinanceLogEntity(
                    id = "fin_demo_3",
                    userId = userId,
                    date = today,
                    type = "expense",
                    category = "Dining Out & Food Delivery",
                    title = "Lunch with Team",
                    amount = 32.0,
                    notes = "Coffee and lunch"
                )
            )
        }

        // 4. Net Worth Items
        val existingNetWorth = dao.getNetWorthItems(userId)
        if (existingNetWorth.isEmpty()) {
            dao.insertNetWorthItem(
                NetWorthItemEntity(
                    id = "nw_demo_1",
                    userId = userId,
                    name = "Bank Savings Account",
                    type = "asset",
                    amount = 6500.0
                )
            )
            dao.insertNetWorthItem(
                NetWorthItemEntity(
                    id = "nw_demo_2",
                    userId = userId,
                    name = "Mutual Funds Portfolio",
                    type = "asset",
                    amount = 15000.0
                )
            )
        }

        // 5. Grocery Items
        val existingGroceries = dao.getGroceryItemsForUserFlow(userId).firstOrNull() ?: emptyList()
        if (existingGroceries.isEmpty()) {
            dao.insertGroceryItem(
                GroceryItemEntity(
                    id = "groc_demo_1",
                    userId = userId,
                    name = "Almond Milk",
                    quantity = "2 cartons",
                    category = "Dairy",
                    price = 4.50
                )
            )
            dao.insertGroceryItem(
                GroceryItemEntity(
                    id = "groc_demo_2",
                    userId = userId,
                    name = "Fresh Apples",
                    quantity = "1 kg",
                    category = "Fruits",
                    price = 3.20
                )
            )
            dao.insertGroceryItem(
                GroceryItemEntity(
                    id = "groc_demo_3",
                    userId = userId,
                    name = "Whole Wheat Bread",
                    quantity = "1 loaf",
                    category = "Bakery",
                    price = 2.80
                )
            )
        }

        // 6. Wishlist Items
        val existingWishlist = dao.getWishlistForUserFlow(userId).firstOrNull() ?: emptyList()
        if (existingWishlist.isEmpty()) {
            dao.insertWishItem(
                WishItemEntity(
                    id = "wish_demo_1",
                    userId = userId,
                    title = "Wireless Noise-Canceling Headphones",
                    price = 199.99,
                    priority = "high",
                    purchased = false
                )
            )
        }

        // 7. Birthdays / Occasions
        val existingBirthdays = dao.getBirthdaysForUserFlow(userId).firstOrNull() ?: emptyList()
        if (existingBirthdays.isEmpty()) {
            dao.insertBirthday(
                BirthdayEntity(
                    id = "bday_demo_1",
                    userId = userId,
                    name = "Mom's Birthday",
                    date = "2026-08-25",
                    category = "Birthday|Family",
                    giftIdea = "Custom photo album & flower bouquet",
                    remindMe = true
                )
            )
        }
    }

    // --- Alarms ---
    fun getAlarmsFlow(userId: String): Flow<List<AlarmEntity>> = dao.getAlarmsForUserFlow(userId)

    suspend fun insertAlarm(alarm: AlarmEntity) {
        dao.insertAlarm(alarm)
    }

    suspend fun deleteAlarm(alarmId: String) {
        dao.deleteAlarmById(alarmId)
    }

    // --- Sleep Logs ---
    fun getSleepLogsFlow(userId: String): Flow<List<SleepLogEntity>> = dao.getSleepLogsForUserFlow(userId)

    suspend fun insertSleepLog(log: SleepLogEntity) {
        dao.insertSleepLog(log)
    }

    suspend fun deleteSleepLog(id: String) {
        dao.deleteSleepLogById(id)
    }

    // --- User Profile ---
    fun getUserProfileFlow(userId: String): Flow<UserProfileEntity?> = dao.getUserProfileFlow(userId)

    suspend fun getUserProfile(userId: String): UserProfileEntity? = dao.getUserProfile(userId)

    suspend fun insertUserProfile(profile: UserProfileEntity) {
        dao.insertUserProfile(profile)
    }

    // --- Grocery Items ---
    fun getGroceryItemsFlow(userId: String): Flow<List<GroceryItemEntity>> = dao.getGroceryItemsForUserFlow(userId)

    suspend fun insertGroceryItem(item: GroceryItemEntity) {
        dao.insertGroceryItem(item)
    }

    suspend fun deleteGroceryItem(id: String) {
        dao.deleteGroceryItemById(id)
    }

    suspend fun clearCompletedGroceryItems(userId: String) {
        dao.clearCompletedGroceryItems(userId)
    }

    // --- Tablet Reminders ---
    fun getTabletRemindersFlow(userId: String): Flow<List<TabletReminderEntity>> = dao.getTabletRemindersForUserFlow(userId)

    suspend fun insertTabletReminder(reminder: TabletReminderEntity) {
        dao.insertTabletReminder(reminder)
    }

    suspend fun deleteTabletReminder(id: String) {
        dao.deleteTabletReminderById(id)
    }

    // --- Period Cycles ---
    fun getPeriodCyclesFlow(userId: String): Flow<List<PeriodCycleEntity>> = dao.getPeriodCyclesForUserFlow(userId)

    suspend fun insertPeriodCycle(cycle: PeriodCycleEntity) {
        dao.insertPeriodCycle(cycle)
    }

    suspend fun deletePeriodCycle(id: String) {
        dao.deletePeriodCycleById(id)
    }

    // --- Finance Logs ---
    fun getFinanceLogsFlow(userId: String): Flow<List<FinanceLogEntity>> = dao.getFinanceLogsForUserFlow(userId)

    suspend fun getFinanceLogs(userId: String): List<FinanceLogEntity> {
        return dao.getFinanceLogsForUser(userId)
    }

    suspend fun insertFinanceLog(log: FinanceLogEntity) {
        dao.insertFinanceLog(log)
    }

    suspend fun getFinanceLogById(id: String): FinanceLogEntity? {
        return dao.getFinanceLogById(id)
    }

    suspend fun deleteFinanceLog(id: String) {
        dao.deleteFinanceLogById(id)
    }

    // --- Net Worth Items ---
    fun getNetWorthItemsFlow(userId: String): Flow<List<NetWorthItemEntity>> = dao.getNetWorthItemsFlow(userId)

    suspend fun getNetWorthItems(userId: String): List<NetWorthItemEntity> = dao.getNetWorthItems(userId)

    suspend fun insertNetWorthItem(item: NetWorthItemEntity) {
        dao.insertNetWorthItem(item)
    }

    suspend fun deleteNetWorthItem(id: String) {
        dao.deleteNetWorthItemById(id)
    }

    suspend fun updateNetWorthItemAmount(userId: String, name: String, delta: Double) {
        dao.updateNetWorthItemAmount(userId, name, delta)
    }

    suspend fun getNetWorthItemByName(userId: String, name: String): NetWorthItemEntity? {
        return dao.getNetWorthItemByName(userId, name)
    }

    // --- Notebooks & Notes ---
    fun getNotebooksFlow(userId: String): Flow<List<NotebookEntity>> = dao.getNotebooksForUserFlow(userId)

    suspend fun insertNotebook(notebook: NotebookEntity) {
        dao.insertNotebook(notebook)
    }

    suspend fun deleteNotebook(notebookId: String) {
        dao.deleteNotesByNotebookId(notebookId)
        dao.deleteNotebookById(notebookId)
    }

    fun getNotesForNotebookFlow(notebookId: String): Flow<List<NoteEntity>> = dao.getNotesForNotebookFlow(notebookId)

    fun getAllNotesFlow(userId: String): Flow<List<NoteEntity>> = dao.getAllNotesForUserFlow(userId)

    suspend fun insertNote(note: NoteEntity) {
        dao.insertNote(note)
    }

    suspend fun deleteNote(noteId: String) {
        dao.deleteNoteById(noteId)
    }

    suspend fun clearUserData(userId: String) {
        dao.clearUserData(userId)
    }
}
