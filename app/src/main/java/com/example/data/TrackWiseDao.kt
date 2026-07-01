package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackWiseDao {

    // --- Users ---
    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // --- Tasks ---
    @Query("SELECT * FROM tasks WHERE userId = :userId")
    fun getTasksForUserFlow(userId: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    // --- Habits ---
    @Query("SELECT * FROM habits WHERE userId = :userId")
    fun getHabitsForUserFlow(userId: String): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: String)

    // --- Birthdays ---
    @Query("SELECT * FROM birthdays WHERE userId = :userId")
    fun getBirthdaysForUserFlow(userId: String): Flow<List<BirthdayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBirthday(birthday: BirthdayEntity)

    @Query("DELETE FROM birthdays WHERE id = :birthdayId")
    suspend fun deleteBirthdayById(birthdayId: String)

    // --- Wishlist ---
    @Query("SELECT * FROM wishlist WHERE userId = :userId")
    fun getWishlistForUserFlow(userId: String): Flow<List<WishItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishItem(item: WishItemEntity)

    @Query("DELETE FROM wishlist WHERE id = :itemId")
    suspend fun deleteWishItemById(itemId: String)

    // --- Streak History ---
    @Query("SELECT * FROM streak_history WHERE userId = :userId ORDER BY date DESC LIMIT 7")
    fun getStreakHistoryForUserFlow(userId: String): Flow<List<StreakHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreakHistory(history: StreakHistoryEntity)

    @Query("DELETE FROM streak_history WHERE id = :id")
    suspend fun deleteStreakHistoryById(id: String)

    // --- Weight Entries ---
    @Query("SELECT * FROM weight_entries WHERE userId = :userId ORDER BY date DESC, time DESC")
    fun getWeightEntriesForUserFlow(userId: String): Flow<List<WeightEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightEntry(entry: WeightEntryEntity)

    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun deleteWeightEntryById(id: String)

    // --- Vital Readings ---
    @Query("SELECT * FROM vital_readings WHERE userId = :userId ORDER BY date DESC, time DESC")
    fun getVitalReadingsForUserFlow(userId: String): Flow<List<VitalReadingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVitalReading(reading: VitalReadingEntity)

    @Query("DELETE FROM vital_readings WHERE id = :id")
    suspend fun deleteVitalReadingById(id: String)

    // --- Water Logs ---
    @Query("SELECT * FROM water_logs WHERE userId = :userId ORDER BY date DESC")
    fun getWaterLogsForUserFlow(userId: String): Flow<List<WaterLogEntity>>

    @Query("SELECT * FROM water_logs WHERE userId = :userId AND date = :date")
    suspend fun getWaterLogForDate(userId: String, date: String): WaterLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(waterLog: WaterLogEntity)

    // --- Exercise Logs ---
    @Query("SELECT * FROM exercise_logs WHERE userId = :userId ORDER BY date DESC, time DESC")
    fun getExerciseLogsForUserFlow(userId: String): Flow<List<ExerciseLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseLog(log: ExerciseLogEntity)

    @Query("DELETE FROM exercise_logs WHERE id = :id")
    suspend fun deleteExerciseLogById(id: String)

    // --- Health Issue Logs ---
    @Query("SELECT * FROM health_issue_logs WHERE userId = :userId ORDER BY date DESC, time DESC")
    fun getHealthIssueLogsForUserFlow(userId: String): Flow<List<HealthIssueLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthIssueLog(log: HealthIssueLogEntity)

    @Query("DELETE FROM health_issue_logs WHERE id = :id")
    suspend fun deleteHealthIssueLogById(id: String)

    // --- Friend Connections ---
    @Query("SELECT * FROM friend_connections WHERE userId = :userId")
    fun getFriendsForUserFlow(userId: String): Flow<List<FriendConnectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendConnectionEntity)

    @Query("DELETE FROM friend_connections WHERE id = :id")
    suspend fun deleteFriendById(id: String)

    // --- Alarms ---
    @Query("SELECT * FROM alarms WHERE userId = :userId ORDER BY hour ASC, minute ASC")
    fun getAlarmsForUserFlow(userId: String): Flow<List<AlarmEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: String)

    // --- Sleep Logs ---
    @Query("SELECT * FROM sleep_logs WHERE userId = :userId ORDER BY date DESC")
    fun getSleepLogsForUserFlow(userId: String): Flow<List<SleepLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepLog(log: SleepLogEntity)

    @Query("DELETE FROM sleep_logs WHERE id = :id")
    suspend fun deleteSleepLogById(id: String)

    // --- User Profiles ---
    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    suspend fun getUserProfile(userId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    fun getUserProfileFlow(userId: String): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)
}
