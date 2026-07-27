package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE habits ADD COLUMN icon TEXT NOT NULL DEFAULT '😊'")
        db.execSQL("ALTER TABLE habits ADD COLUMN quote TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE habits ADD COLUMN goalType TEXT NOT NULL DEFAULT 'Achieve it all'")
        db.execSQL("ALTER TABLE habits ADD COLUMN goalDays TEXT NOT NULL DEFAULT 'Forever'")
        db.execSQL("ALTER TABLE habits ADD COLUMN section TEXT NOT NULL DEFAULT 'Others'")
        db.execSQL("ALTER TABLE habits ADD COLUMN autoPopup INTEGER NOT NULL DEFAULT 0")
    }
}

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
        UserProfileEntity::class,
        GroceryItemEntity::class,
        TabletReminderEntity::class,
        PeriodCycleEntity::class,
        FinanceLogEntity::class,
        NetWorthItemEntity::class
    ],
    version = 16,
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
                .addMigrations(MIGRATION_15_16)
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
