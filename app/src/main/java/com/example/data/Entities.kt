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
    val enabledConditions: String = "",
    val religion: String? = null
)

data class SubTask(
    val id: String,
    val title: String,
    val completed: Boolean,
    val dueDate: String? = null, // YYYY-MM-DD
    val dueTime: String? = null  // HH:MM
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
    val reminderTime: String? = null, // HH:MM
    val repeatType: String = "none", // "none", "daily", "weekdays", "weekly", "monthly", "yearly", "custom"
    val customRepeatValue: Int = 1,
    val customRepeatUnit: String = "days", // "days", "weeks", "months", "years"
    val customRepeatDaysOfWeek: String? = null, // Comma-separated list like "Sun,Mon..."
    val startDate: String? = null, // YYYY-MM-DD
    val endDate: String? = null, // YYYY-MM-DD (null/blank means "Until I stop")
    val notes: String = "",
    val remindMe: Boolean = false,
    val reminderDate: String? = null,
    val dueTime: String? = null // Optional due time
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
    val createdAt: String, // YYYY-MM-DD
    val isMultipleTimesPerDay: Boolean = false,
    val multipleTimesTarget: Int = 1,
    val isTimeBound: Boolean = false,
    val timeBoundDuration: String? = null, // e.g. "30 mins" or "08:00 - 09:00"
    val repeatType: String = "none", // "none", "daily", "weekdays", "weekly", "monthly", "yearly", "custom"
    val customRepeatValue: Int = 1,
    val customRepeatUnit: String = "days", // "days", "weeks", "months", "years"
    val customRepeatDaysOfWeek: String? = null, // Comma-separated list like "Sun,Mon..."
    val startDate: String? = null, // YYYY-MM-DD
    val endDate: String? = null, // YYYY-MM-DD (null/blank means "Until I stop")
    val notes: String = "",
    val remindMe: Boolean = false,
    val reminderDate: String? = null,
    val reminderTime: String? = null,
    val dueTime: String? = null, // Optional due time
    val icon: String = "😊",
    val quote: String = "",
    val goalType: String = "Achieve it all",
    val goalDays: String = "Forever",
    val section: String = "Others",
    val autoPopup: Boolean = false,
    val backgroundImage: String = "window"
)

@Entity(tableName = "birthdays")
data class BirthdayEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val date: String, // YYYY-MM-DD or MM-DD
    val giftIdea: String? = null,
    val category: String = "Others",
    val remindMe: Boolean = false,
    val reminderDate: String? = null,
    val reminderTime: String? = null,
    val isPinned: Boolean = false,
    val customBgImage: String? = null,
    val customTextColor: String? = null,
    val customFontStyle: String? = null,
    val reminderOptions: String? = null,
    val repeatPattern: String? = null,
    val countingMode: String? = null
)

@Entity(tableName = "wishlist")
data class WishItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val price: Double,
    val link: String? = null,
    val priority: String, // "low", "medium", "high"
    val purchased: Boolean,
    val remindMe: Boolean = false,
    val reminderDate: String? = null,
    val reminderTime: String? = null
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

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val label: String,
    val hour: Int, // 0-23
    val minute: Int, // 0-59
    val isEnabled: Boolean,
    val repeatDaysJson: String = "[]", // list of days: e.g. ["Mon", "Wed"]
    val snoozeCount: Int = 0
)

@Entity(tableName = "sleep_logs")
data class SleepLogEntity(
    @PrimaryKey val id: String, // e.g. "userId_date"
    val userId: String,
    val date: String, // YYYY-MM-DD
    val hoursSlept: Double,
    val startTime: String, // HH:MM
    val endTime: String, // HH:MM
    val notes: String? = null
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val dob: String = "",
    val gender: String = "Prefer not to say",
    val maritalStatus: String = "Single",
    val nationality: String = "",
    val nationalId: String = "",
    val bloodGroup: String = "O+",
    val residentialStreet: String = "",
    val residentialCity: String = "",
    val residentialState: String = "",
    val residentialZip: String = "",
    val residentialCountry: String = "",
    val permanentStreet: String = "",
    val permanentCity: String = "",
    val permanentState: String = "",
    val permanentZip: String = "",
    val permanentCountry: String = "",
    val permanentIsSame: Boolean = true,
    val mobileNumber: String = "",
    val alternatePhone: String = "",
    val emailAddress: String = "",
    val emergencyName: String = "",
    val emergencyRelationship: String = "",
    val emergencyPhone: String = "",
    val alternateEmergencyPhone: String = "",
    val height: String = "",
    val weight: String = "",
    val primaryDoctor: String = "",
    val medicalConditions: String = "",
    val currentMedications: String = "",
    val allergies: String = "",
    val dietaryRestrictions: String = "",
    val vitalsHeight: String = "",
    val vitalsWeight: String = "",
    val vitalsBloodPressure: String = "",
    val vitalsHeartRate: String = "",
    val vitalsBloodGroup: String = "",
    val religion: String = "",
    val financeDailyTarget: Double = 0.0
)

@Entity(tableName = "grocery_items")
data class GroceryItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val quantity: String = "1",
    val completed: Boolean = false,
    val category: String = "Other",
    val price: Double? = null,
    val priceUnit: String? = null, // "single item", "single kg", "single gram", "single set"
    val numericQuantity: Double? = null
)

@Entity(tableName = "tablet_reminders")
data class TabletReminderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val tabletName: String,
    val dosage: String,         // e.g. "1 pill", "5ml"
    val timeOfDay: String,      // e.g. "08:00 AM", "09:00 PM"
    val scheduleType: String,   // e.g. "Daily", "Weekly", "As Needed"
    val completedDatesJson: String = "[]", // List of dates "YYYY-MM-DD" when it was taken
    val notes: String? = null
)

@Entity(tableName = "period_cycles")
data class PeriodCycleEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val startDate: String, // YYYY-MM-DD (start of period bleeding)
    val durationDays: Int = 5, // Bleeding duration
    val cycleLengthDays: Int = 28, // Length of entire cycle
    val symptoms: String = "", // Comma-separated list of symptoms
    val notes: String? = null
)

@Entity(tableName = "finance_logs")
data class FinanceLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val date: String, // YYYY-MM-DD
    val type: String, // "income", "expense", "savings"
    val category: String, // e.g. "Housing and Utilities (Fixed Essentials)", "PPF"
    val title: String, // e.g. "Rent or EMI", "Electricity Bill"
    val amount: Double,
    val notes: String? = null,
    val spendSource: String? = null
)

@Entity(tableName = "net_worth_items")
data class NetWorthItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String, // e.g. "Simple Savings in Account", "Mutual Funds", "Home Loan"
    val type: String, // "asset", "loan", "liability"
    val amount: Double
)




