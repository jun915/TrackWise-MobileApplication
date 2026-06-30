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
            BirthdayEntity("b-1", userId, "Aarav Sharma", "05-15", "Premium leather wallet"),
            BirthdayEntity("b-2", userId, "Ammi Jaan", "07-12", "Traditional Kashmiri shawl"),
            BirthdayEntity("b-3", userId, "Zoya Khan", "09-24", "Wireless noise cancelling earbuds"),
            BirthdayEntity("b-4", userId, "Rahul Patel", "01-05", "Fitness smartwatch"),
            BirthdayEntity("b-5", userId, "Priya Nair", "11-30", "Scented candle gift set"),
            BirthdayEntity("b-6", userId, "Kabir Mehta", "03-18", "Python programming masterclass subscription"),
            BirthdayEntity("b-7", userId, "Ananya Sen", "12-05", "Ceramic handmade tea mug"),
            BirthdayEntity("b-8", userId, "Fatima Bi", "02-14", "Orthopedic posture cushion"),
            BirthdayEntity("b-9", userId, "Rohan Das", "06-10", "Mechanical gaming keyboard"),
            BirthdayEntity("b-10", userId, "Siddharth", "08-22", "Stainless steel water bottle"),
            BirthdayEntity("b-11", userId, "Abbu", "10-14", "Digital BP monitoring machine")
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

        // Initialize today's water log
        dao.insertWaterLog(
            WaterLogEntity(
                id = "${userId}_$today",
                userId = userId,
                date = today,
                glasses = 0,
                goal = 8
            )
        )
    }
}
