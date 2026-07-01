package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        TaskEntity::class,
        HabitEntity::class,
        BirthdayEntity::class,
        WishItemEntity::class,
        StreakHistoryEntity::class,
        WeightEntryEntity::class,
        VitalReadingEntity::class,
        WaterLogEntity::class,
        ExerciseLogEntity::class,
        HealthIssueLogEntity::class,
        FriendConnectionEntity::class,
        AlarmEntity::class,
        SleepLogEntity::class,
        UserProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TrackWiseDatabase : RoomDatabase() {
    abstract fun trackWiseDao(): TrackWiseDao

    companion object {
        @Volatile
        private var INSTANCE: TrackWiseDatabase? = null

        fun getDatabase(context: Context): TrackWiseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackWiseDatabase::class.java,
                    "trackwise_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
