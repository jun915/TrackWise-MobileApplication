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
        return dao.getUserByEmail(email.lowercase().trim())
    }

    suspend fun findUserById(userId: String): UserEntity? {
        return dao.getUserById(userId)
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
        val user = findUserByEmail(email) ?: throw Exception("No account found with this email.")
        val hash = SecurityUtils.hashPassword(passwordRaw)
        if (user.passwordHash != hash) {
            throw Exception("Incorrect password.")
        }
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

    // --- Seed Demo Data (Part 15) ---
    private suspend fun seedDemoData(userId: String) {
        val today = TrackWiseUtils.getTodayString()
        
        // Default Projects
        val projects = listOf("Tasks", "Wish List", "Work", "Personal", "Health", "Learning")
        
        // 25 Sample tasks
        val sampleTasks = listOf(
            TaskEntity("seed-1", userId, "Read Yaseen 📘", "Read daily after morning prayer", "Personal", "high", today, false, 5, "[]", "06:00"),
            TaskEntity("seed-2", userId, "Exercise 🏋", "30 minutes cardio or weight training", "Health", "medium", today, false, 3, "[]", "07:00"),
            TaskEntity("seed-3", userId, "Pay ₹50 to Ammi 💸", "Daily household milk/bread expenses", "Personal", "low", today, false, 2, "[]", "10:00"),
            TaskEntity("seed-4", userId, "Mutual Fund SIP Auto-Pay check", "Verify monthly deduction from bank", "Work", "high", today, false, 5, "[]", "11:00"),
            TaskEntity("seed-5", userId, "Wifi Bill payment 📶", "Clear broadband outstanding", "Tasks", "high", today, false, 3, "[]", "18:00"),
            TaskEntity("seed-6", userId, "Hijama Therapy Appointment 🩸", "Wet cupping session", "Health", "medium", today, false, 3, "[]", "16:00"),
            TaskEntity("seed-7", userId, "Weekly Groceries list 🛒", "Prepare checklist for supermarket", "Personal", "low", today, false, 2, "[]"),
            TaskEntity("seed-8", userId, "Complete Jetpack Compose course 💻", "Study adaptive sizing", "Learning", "medium", today, false, 3, "[]"),
            TaskEntity("seed-9", userId, "Read book on Islamic History 📚", "10 pages daily", "Learning", "low", today, false, 2, "[]"),
            TaskEntity("seed-10", userId, "Submit progress report to Manager", "Include quarterly key metrics", "Work", "high", today, false, 5, "[]"),
            TaskEntity("seed-11", userId, "Update resume with Compose skill", "Add latest design project details", "Work", "medium", today, false, 3, "[]"),
            TaskEntity("seed-12", userId, "Hydrate 3L of water 💧", "Track glasses in Health tab", "Health", "high", today, false, 5, "[]"),
            TaskEntity("seed-13", userId, "Call Nani 📞", "Check on her health and medicines", "Personal", "high", today, false, 3, "[]"),
            TaskEntity("seed-14", userId, "Dentist checkup 🦷", "Annual routine clean up", "Health", "medium", today, false, 3, "[]"),
            TaskEntity("seed-15", userId, "Fix kitchen sink leak 🪠", "Replace the washer seal", "Tasks", "low", today, false, 2, "[]"),
            TaskEntity("seed-16", userId, "Review monthly expenses chart", "Ensure budget limits are maintained", "Tasks", "medium", today, false, 3, "[]"),
            TaskEntity("seed-17", userId, "Dhuhr Prayer 🕌", "Perform in congregation", "Personal", "high", today, false, 5, "[]", "13:30"),
            TaskEntity("seed-18", userId, "Asr Prayer 🕌", "Perform on time", "Personal", "high", today, false, 5, "[]", "17:15"),
            TaskEntity("seed-19", userId, "Maghrib Prayer 🕌", "Evening reflection", "Personal", "high", today, false, 5, "[]", "19:25"),
            TaskEntity("seed-20", userId, "Isha Prayer 🕌", "Final daily congregation", "Personal", "high", today, false, 5, "[]", "21:00"),
            TaskEntity("seed-21", userId, "Prepare dinner recipe 🍲", "Biryani special weekend cookout", "Personal", "low", today, false, 2, "[]"),
            TaskEntity("seed-22", userId, "Write diary entry ✍️", "Reflection on goals and mindfulness", "Personal", "low", today, false, 2, "[]"),
            TaskEntity("seed-23", userId, "Study Room database performance", "Optimize indices and migration paths", "Learning", "medium", today, false, 3, "[]"),
            TaskEntity("seed-24", userId, "Set up notification channels", "Test local alarms and interval timers", "Work", "medium", today, false, 3, "[]"),
            TaskEntity("seed-25", userId, "Check sibling's exam dates", "Coordinate support sessions", "Personal", "low", today, false, 2, "[]")
        )

        sampleTasks.forEach { dao.insertTask(it) }

        // 11 Sample Birthdays
        val sampleBirthdays = listOf(
            BirthdayEntity("b-1", userId, "Aarav Sharma's Birthday", "05-15", "Premium leather wallet", "Friend"),
            BirthdayEntity("b-2", userId, "Ammi Jaan's Birthday", "07-12", "Traditional Kashmiri shawl", "Family"),
            BirthdayEntity("b-3", userId, "Zoya Khan's Birthday", "09-24", "Wireless noise cancelling earbuds", "Friend"),
            BirthdayEntity("b-4", userId, "Rahul Patel's Birthday", "01-05", "Fitness smartwatch", "Relative"),
            BirthdayEntity("b-5", userId, "Priya Nair's Birthday", "11-30", "Scented candle gift set", "Friend"),
            BirthdayEntity("b-6", userId, "Kabir Mehta's Birthday", "03-18", "Python programming masterclass subscription", "Friend"),
            BirthdayEntity("b-7", userId, "Ananya Sen's Birthday", "12-05", "Ceramic handmade tea mug", "Friend"),
            BirthdayEntity("b-8", userId, "Fatima Bi's Birthday", "02-14", "Orthopedic posture cushion", "Relative"),
            BirthdayEntity("b-9", userId, "Rohan Das's Birthday", "06-10", "Mechanical gaming keyboard", "Friend"),
            BirthdayEntity("b-10", userId, "Siddharth's Birthday", "08-22", "Stainless steel water bottle", "Others"),
            BirthdayEntity("b-11", userId, "Abbu's Birthday", "10-14", "Digital BP monitoring machine", "Family")
        )

        sampleBirthdays.forEach { dao.insertBirthday(it) }

        // Sample Habits
        val sampleHabits = listOf(
            HabitEntity("h-1", userId, "Morning Jog 🏃‍♂️", "Fitness", "daily", "[]", 0, 0, "[]", today),
            HabitEntity("h-2", userId, "Read Quran 📖", "Wellness", "daily", "[]", 0, 0, "[]", today),
            HabitEntity("h-3", userId, "Hydration Checklist 💧", "Wellness", "daily", "[]", 0, 0, "[]", today),
            HabitEntity("h-4", userId, "Learn Kotlin 🧠", "Learning", "daily", "[]", 0, 0, "[]", today)
        )

        sampleHabits.forEach { dao.insertHabit(it) }

        // Sample Wishlist items
        val sampleWish = listOf(
            WishItemEntity("w-1", userId, "Apple iPad Air M2", 59900.0, "https://apple.com", "high", false),
            WishItemEntity("w-2", userId, "Ergonomic Office Chair", 12500.0, null, "medium", false),
            WishItemEntity("w-3", userId, "Noise Cancelling Headphones", 18000.0, null, "medium", false),
            WishItemEntity("w-4", userId, "Leather Duffle Bag", 4500.0, null, "low", false)
        )

        sampleWish.forEach { dao.insertWishItem(it) }

        // Sample Alarms
        val sampleAlarms = listOf(
            AlarmEntity("a-1", userId, "Fajr Prayer 🕌", 5, 0, true, "[\"Mon\",\"Tue\",\"Wed\",\"Thu\",\"Fri\",\"Sat\",\"Sun\"]"),
            AlarmEntity("a-2", userId, "Night Wind Down 🌙", 22, 30, true, "[\"Mon\",\"Tue\",\"Wed\",\"Thu\",\"Fri\",\"Sat\",\"Sun\"]")
        )
        sampleAlarms.forEach { dao.insertAlarm(it) }

        // Seed sample grocery items
        val sampleGroceries = listOf(
            GroceryItemEntity("g-1", userId, "Organic Bananas 🍌", "1 dozen", false, "Produce"),
            GroceryItemEntity("g-2", userId, "Fresh Whole Milk 🥛", "2 Litres", false, "Dairy"),
            GroceryItemEntity("g-3", userId, "Brown Bread 🍞", "1 loaf", true, "Bakery"),
            GroceryItemEntity("g-4", userId, "Oatmeal Oats 🥣", "1 kg", false, "Pantry"),
            GroceryItemEntity("g-5", userId, "Green Tea Bags 🍵", "1 pack", false, "Pantry")
        )
        sampleGroceries.forEach { dao.insertGroceryItem(it) }

        // Initialize today's water log
        dao.insertWaterLog(
            WaterLogEntity(
                id = "${userId}_$today",
                userId = userId,
                date = today,
                glasses = 6,
                goal = 8
            )
        )

        // Seed rich logs for Analytics (Water, Sleep, Exercise, Vitals)
        val datesList = listOf(
            today,
            "2026-06-30",
            "2026-06-29",
            "2026-06-28",
            "2026-06-27",
            "2026-06-26",
            "2026-06-25",
            "2026-06-24"
        )

        // Water Logs (various amounts)
        val waterAmounts = listOf(6, 4, 8, 3, 7, 5, 9, 2)
        datesList.forEachIndexed { idx, d ->
            dao.insertWaterLog(
                WaterLogEntity("${userId}_$d", userId, d, waterAmounts.getOrElse(idx) { 6 }, 8)
            )
        }

        // Sleep Logs (Start Time, End Time, Hours Slept)
        val sleepRecords = listOf(
            SleepLogEntity("sl-1", userId, today, 7.5, "23:00", "06:30", "Felt refreshed"),
            SleepLogEntity("sl-2", userId, "2026-06-30", 6.0, "00:00", "06:00", "Slightly tired"),
            SleepLogEntity("sl-3", userId, "2026-06-29", 8.5, "22:00", "06:30", "Deep restful sleep"),
            SleepLogEntity("sl-4", userId, "2026-06-28", 5.0, "01:00", "06:00", "Interrupted sleep"),
            SleepLogEntity("sl-5", userId, "2026-06-27", 9.0, "21:30", "06:30", "Excellent weekend sleep"),
            SleepLogEntity("sl-6", userId, "2026-06-26", 7.0, "23:30", "06:30", "Standard weekday"),
            SleepLogEntity("sl-7", userId, "2026-06-25", 4.5, "02:00", "06:30", "Late coding session"),
            SleepLogEntity("sl-8", userId, "2026-06-24", 8.0, "22:30", "06:30", "Very peacefull sleep")
        )
        sleepRecords.forEach { dao.insertSleepLog(it) }

        // Exercise Logs (varying intensities/durations for intensity split)
        val exerciseRecords = listOf(
            ExerciseLogEntity("ex-1", userId, today, "07:30", "HIIT Cardio", 45, true, "Very high intensity"),
            ExerciseLogEntity("ex-2", userId, "2026-06-30", "18:30", "Weight Lifting", 60, true, "Moderate intensity"),
            ExerciseLogEntity("ex-3", userId, "2026-06-29", "08:00", "Slow Yoga", 30, true, "Very low intensity"),
            ExerciseLogEntity("ex-4", userId, "2026-06-28", "07:00", "Outdoor Run", 50, true, "High intensity"),
            ExerciseLogEntity("ex-5", userId, "2026-06-27", "17:00", "Brisk Walk", 20, true, "Low intensity"),
            ExerciseLogEntity("ex-6", userId, "2026-06-26", "08:00", "Swimming", 40, true, "High intensity"),
            ExerciseLogEntity("ex-7", userId, "2026-06-25", "19:00", "Stretching", 15, true, "Very low intensity"),
            ExerciseLogEntity("ex-8", userId, "2026-06-24", "07:15", "Cycling", 55, true, "Moderate-High intensity")
        )
        exerciseRecords.forEach { dao.insertExerciseLog(it) }

        // Vital Readings (Blood Sugar and Blood Pressure with varying readings)
        val vitals = listOf(
            VitalReadingEntity("v-1", userId, "blood_pressure", today, "08:00", "120/80", "resting", "Perfect"),
            VitalReadingEntity("v-2", userId, "blood_pressure", "2026-06-30", "08:00", "130/85", "resting", "Slightly elevated"),
            VitalReadingEntity("v-3", userId, "blood_pressure", "2026-06-29", "09:00", "118/78", "resting", "Excellent"),
            VitalReadingEntity("v-4", userId, "blood_pressure", "2026-06-28", "08:30", "125/82", "resting", "Standard"),
            VitalReadingEntity("v-5", userId, "blood_pressure", "2026-06-27", "08:00", "122/80", "resting", "Good"),
            VitalReadingEntity("v-6", userId, "blood_pressure", "2026-06-26", "08:00", "140/90", "resting", "High stress morning"),
            VitalReadingEntity("v-7", userId, "blood_pressure", "2026-06-25", "08:15", "115/75", "resting", "Excellent"),
            VitalReadingEntity("v-8", userId, "blood_pressure", "2026-06-24", "08:00", "128/84", "resting", "Mild elevation"),

            VitalReadingEntity("v-9", userId, "blood_sugar", today, "07:30", "95", "fasting", "Perfect"),
            VitalReadingEntity("v-10", userId, "blood_sugar", "2026-06-30", "07:30", "110", "fasting", "Post-dessert hangover"),
            VitalReadingEntity("v-11", userId, "blood_sugar", "2026-06-29", "07:30", "88", "fasting", "Great"),
            VitalReadingEntity("v-12", userId, "blood_sugar", "2026-06-28", "07:30", "102", "fasting", "Standard"),
            VitalReadingEntity("v-13", userId, "blood_sugar", "2026-06-27", "07:30", "97", "fasting", "Good"),
            VitalReadingEntity("v-14", userId, "blood_sugar", "2026-06-26", "07:30", "125", "fasting", "High carbs dinner"),
            VitalReadingEntity("v-15", userId, "blood_sugar", "2026-06-25", "07:30", "90", "fasting", "Perfect"),
            VitalReadingEntity("v-16", userId, "blood_sugar", "2026-06-24", "07:30", "99", "fasting", "Optimal")
        )
        vitals.forEach { dao.insertVitalReading(it) }

        // Seed a very complete default User Profile
        val defaultProfile = UserProfileEntity(
            userId = userId,
            firstName = "Syed",
            middleName = "Junaid",
            lastName = "Shah",
            dob = "15/08/1998",
            gender = "Male",
            maritalStatus = "Single",
            nationality = "Indian",
            nationalId = "9876-5432-1012",
            bloodGroup = "O+",
            residentialStreet = "12 Block C, Shalimar Bagh",
            residentialCity = "New Delhi",
            residentialState = "Delhi",
            residentialZip = "110088",
            residentialCountry = "India",
            permanentStreet = "12 Block C, Shalimar Bagh",
            permanentCity = "New Delhi",
            permanentState = "Delhi",
            permanentZip = "110088",
            permanentCountry = "India",
            permanentIsSame = true,
            mobileNumber = "9159159150",
            alternatePhone = "9876543210",
            emailAddress = "syedjunaid915@gmail.com",
            emergencyName = "Syed Ahmad Shah",
            emergencyRelationship = "Father",
            emergencyPhone = "9900112233",
            alternateEmergencyPhone = "9887766554",
            height = "178 cm",
            weight = "72 kg",
            primaryDoctor = "Dr. Sameer Kaul",
            medicalConditions = "None / Active athlete",
            currentMedications = "Multivitamins daily",
            allergies = "None / Seasonal dust pollen",
            dietaryRestrictions = "Halal, high-protein diet preferred",
            vitalsHeight = "178",
            vitalsWeight = "72",
            vitalsBloodPressure = "120/80",
            vitalsHeartRate = "68",
            vitalsBloodGroup = "O+"
        )
        dao.insertUserProfile(defaultProfile)
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

    suspend fun clearUserData(userId: String) {
        dao.clearUserData(userId)
    }
}
