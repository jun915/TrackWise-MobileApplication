package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val passwordHash: String,
    val fullName: String,
    val dob: String? = null,
    val gender: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val phone: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zipCode: String? = null,
    val bloodType: String? = null,
    val waterGoalGlasses: Int = 8,
    // Comma-separated list of enabled condition IDs (e.g. "diabetes,hypertension")
    val enabledConditions: String = ""
)

data class SubTask(
    val id: String,
    val title: String,
    val completed: Boolean
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String = "",
    val project: String,
    val priority: String, // "low", "medium", "high"
    val deadline: String, // YYYY-MM-DD
    val completed: Boolean,
    val points: Int,
    val subtasksJson: String = "[]", // List<SubTask> serialized
    val reminderTime: String? = null // HH:MM
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val category: String, // "Wellness", "Fitness", "Learning", etc.
    val frequency: String = "daily", // "daily", "weekly"
    val daysCompletedJson: String = "[]", // List<String> YYYY-MM-DD
    val streak: Int = 0,
    val maxStreak: Int = 0,
    val badgesEarnedJson: String = "[]", // List<Int> representing milestone day counts
    val createdAt: String // YYYY-MM-DD
)

@Entity(tableName = "birthdays")
data class BirthdayEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val date: String, // YYYY-MM-DD or MM-DD
    val giftIdea: String? = null
)

@Entity(tableName = "wishlist")
data class WishItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val price: Double,
    val link: String? = null,
    val priority: String, // "low", "medium", "high"
    val purchased: Boolean
)

@Entity(tableName = "streak_history")
data class StreakHistoryEntity(
    @PrimaryKey val id: String, // userId_date
    val userId: String,
    val date: String, // YYYY-MM-DD
    val score: Int
)

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val date: String, // YYYY-MM-DD
    val time: String? = null, // HH:MM
    val weightKg: Double,
    val notes: String? = null
)

@Entity(tableName = "vital_readings")
data class VitalReadingEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String, // "blood_sugar", "blood_pressure"
    val date: String, // YYYY-MM-DD
    val time: String? = null, // HH:MM
    val value: String, // e.g. "95" or "120/80"
    val context: String? = null, // "fasting", "post_meal", "random", "resting"
    val notes: String? = null
)

@Entity(tableName = "water_logs")
data class WaterLogEntity(
    @PrimaryKey val id: String, // userId_date
    val userId: String,
    val date: String, // YYYY-MM-DD
    val glasses: Int,
    val goal: Int,
    val remindersEnabled: Boolean = false,
    val reminderIntervalMinutes: Int = 60
)

@Entity(tableName = "exercise_logs")
data class ExerciseLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val date: String, // YYYY-MM-DD
    val time: String? = null, // HH:MM
    val exerciseType: String,
    val durationMinutes: Int = 0,
    val completed: Boolean,
    val notes: String? = null
)

@Entity(tableName = "health_issue_logs")
data class HealthIssueLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val date: String, // YYYY-MM-DD
    val time: String? = null, // HH:MM
    val issueId: String,
    val issueName: String,
    val severity: String, // "mild", "moderate", "severe"
    val notes: String? = null,
    val resolved: Boolean
)

@Entity(tableName = "friend_connections")
data class FriendConnectionEntity(
    @PrimaryKey val id: String, // userId_friendUserId
    val userId: String,
    val friendUserId: String,
    val displayName: String,
    val addedAt: String
)
