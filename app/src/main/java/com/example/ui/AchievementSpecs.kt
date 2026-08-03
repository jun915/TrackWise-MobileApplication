package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.*
import com.example.ui.theme.*
import com.example.utils.TrackWiseUtils
import java.util.Locale

data class BadgeSpec(
    val days: Int,
    val name: String,
    val medal: String,
    val tier: String,
    val desc: String
) {
    val description: String get() = desc
}

val ALL_STANDARD_HABIT_BADGES = listOf(
    BadgeSpec(1, "The Spark", "🥉", "The Launchpad", "Ignited the first habit"),
    BadgeSpec(3, "Three's Company", "🥉", "The Launchpad", "Overcame day-two slump"),
    BadgeSpec(5, "Workweek Warrior", "🥉", "The Launchpad", "Five consecutive days"),
    BadgeSpec(7, "Weekly Wonder", "🥉", "The Launchpad", "Completed full week"),
    BadgeSpec(14, "Fortnight Force", "🥈", "The Builder", "Two weeks dedication"),
    BadgeSpec(21, "Habit Former", "🥈", "The Builder", "Avg days to lock routine"),
    BadgeSpec(30, "Calendar Crusher", "🥈", "The Builder", "One full month"),
    BadgeSpec(45, "Halfway Hero", "🥈", "The Builder", "Momentum past 1 month"),
    BadgeSpec(60, "Iron Will", "🥇", "The Master", "Two months unbroken"),
    BadgeSpec(90, "Seasoned Pro", "🥇", "The Master", "Seasonal commitment"),
    BadgeSpec(100, "Centurion", "🥇", "The Master", "Triple-digit milestone"),
    BadgeSpec(365, "Immortal", "🥇", "The Master", "One full year")
)

data class AchievementItemSpec(
    val title: String,
    val desc: String,
    val progress: Int,
    val target: Int,
    val icon: ImageVector,
    val iconColor: Color,
    val tier: String = "EPIC",
    val isBadge: Boolean = false
)

fun getAllSystemAchievements(
    allTasks: List<TaskEntity>,
    allHabits: List<HabitEntity>,
    allFinanceLogs: List<FinanceLogEntity>,
    allWishlist: List<WishItemEntity>,
    allBirthdays: List<BirthdayEntity>,
    badHabits: List<TrackWiseViewModel.BadHabitSpec>,
    socialCircleSize: Int,
    hasActiveAlarm: Boolean,
    alarmsCount: Int,
    waterLogs: List<WaterLogEntity>,
    streakHistory: List<StreakHistoryEntity>,
    userLevel: Int,
    totalXP: Int,
    bookmarkedSeerahCount: Int = 0,
    weightEntriesCount: Int = 0,
    netWorth: Double = 0.0
): List<AchievementItemSpec> {
    val completedTasksCount = allTasks.count { it.completed }
    val highPriorityCompletedCount = allTasks.count { it.completed && it.priority.lowercase(Locale.ROOT) == "high" }
    val maxHabitStreak = allHabits.maxOfOrNull { it.maxStreak } ?: 0
    val totalWaterGlasses = waterLogs.sumOf { it.glasses }
    val totalIncomeAmount = allFinanceLogs.filter { it.type == "income" }.sumOf { it.amount }
    val totalExpenseCount = allFinanceLogs.count { it.type == "expense" }
    val totalSavingsAmount = allFinanceLogs.filter { it.type == "savings" }.sumOf { it.amount }
    val purchasedWishlistCount = allWishlist.count { it.purchased }
    val highValWishlistPurchased = allWishlist.count { it.purchased && it.price >= 10000 }
    val totalBadHabitLogs = badHabits.sumOf { it.logs.size }
    val earnedHabitBadgesCount = allHabits.flatMap {
        TrackWiseUtils.deserializeIntList(it.badgesEarnedJson)
    }.distinct().size

    return listOf(
        // --- 1. Habiteers & Streaks ---
        AchievementItemSpec("First Blood: Streak Champion", "Develop consistent routines by checking off habits.", if (maxHabitStreak > 0) 1 else 0, 1, Icons.Default.LocalFireDepartment, BrandRose, "RARE"),
        AchievementItemSpec("Recruit: Habit Novice", "Create 3 habits to structure your life.", allHabits.size, 3, Icons.Default.MilitaryTech, BrandViolet, "COMMON"),
        AchievementItemSpec("Commando: Habit Enthusiast", "Build consistency with 5 active habits.", allHabits.size, 5, Icons.Default.Shield, BrandAmber, "EPIC"),
        AchievementItemSpec("Apex Predator: Habit Master", "Reach peak self-discipline with 10 active habits.", allHabits.size, 10, Icons.Default.WorkspacePremium, BrandCyan, "LEGENDARY"),
        AchievementItemSpec("Overachiever: 5-Day Streak", "Reach a 5-day maximum streak on any habit.", maxHabitStreak, 5, Icons.Default.FlashOn, BrandViolet, "EPIC"),
        AchievementItemSpec("Berserker: 15-Day Unbroken", "Reach a 15-day streak to lock in your routine.", maxHabitStreak, 15, Icons.Default.Whatshot, BrandRose, "MYTHIC"),
        AchievementItemSpec("Winner Winner Chicken Dinner: 30-Day Streak", "Maintain a 30-day streak — legendary performance!", maxHabitStreak, 30, Icons.Default.EmojiEvents, BrandAmber, "MYTHIC"),

        // --- 2. Tasks & Execution ---
        AchievementItemSpec("Gunslinger: Task Specialist", "Mark at least 3 tasks as completed.", completedTasksCount, 3, Icons.Default.GpsFixed, BrandGreen, "COMMON"),
        AchievementItemSpec("Deadeye: Task Journeyman", "Complete 10 total tasks successfully.", completedTasksCount, 10, Icons.Default.GpsFixed, BrandPink, "RARE"),
        AchievementItemSpec("Terminator: Task Master", "Complete 25 distinct tasks.", completedTasksCount, 25, Icons.Default.PrecisionManufacturing, BrandViolet, "EPIC"),
        AchievementItemSpec("Conqueror: Task Overlord", "Execute 50 tasks to dominate your workflow.", completedTasksCount, 50, Icons.Default.MilitaryTech, BrandCyan, "LEGENDARY"),
        AchievementItemSpec("Tactical Master: Super Planner", "Draft 10 tasks to organize your future.", allTasks.size, 10, Icons.Default.AutoAwesome, BrandRose, "EPIC"),
        AchievementItemSpec("Warlord: Architect of Life", "Build a massive plan of 30 total recorded tasks.", allTasks.size, 30, Icons.Default.Shield, BrandAmber, "MYTHIC"),
        AchievementItemSpec("Sharpshooter: High Priority Hero", "Complete 1 critical high-priority task.", highPriorityCompletedCount, 1, Icons.Default.ElectricBolt, BrandRose, "RARE"),
        AchievementItemSpec("Rampage: Elite Executor", "Complete 5 critical high-priority tasks.", highPriorityCompletedCount, 5, Icons.Default.Whatshot, BrandViolet, "EPIC"),
        AchievementItemSpec("Dominator: Apex Achiever", "Conquer 15 high-priority tasks under pressure.", highPriorityCompletedCount, 15, Icons.Default.Diamond, BrandAmber, "MYTHIC"),

        // --- 3. Hydration & Health ---
        AchievementItemSpec("Hydration Initiate", "Drink your first glass of water of the journey.", totalWaterGlasses, 1, Icons.Default.Info, BrandCyan, "COMMON"),
        AchievementItemSpec("Hydration Hero", "Drink 8 glasses of water in total.", totalWaterGlasses, 8, Icons.Default.Info, BrandCyan, "RARE"),
        AchievementItemSpec("Waterfall", "Log 40 total glasses of water.", totalWaterGlasses, 40, Icons.Default.Info, BrandViolet, "EPIC"),
        AchievementItemSpec("Aquarius", "Achieve prime hydration with 100 total logged glasses.", totalWaterGlasses, 100, Icons.Default.Info, BrandRose, "LEGENDARY"),

        // --- 4. Utilities & Alarms ---
        AchievementItemSpec("Early Bird Alarm", "Enable at least one active alarm.", if (hasActiveAlarm) 1 else 0, 1, Icons.Default.AccessTime, BrandAmber, "COMMON"),
        AchievementItemSpec("Snooze Defier", "Set up 3 custom timers/alarms.", alarmsCount, 3, Icons.Default.AccessTime, BrandViolet, "RARE"),

        // --- 5. Break Bad Habits ---
        AchievementItemSpec("Honest Tracker", "Keep at least 2 custom bad habits under surveillance.", badHabits.size, 2, Icons.Default.Verified, BrandGreen, "COMMON"),
        AchievementItemSpec("Self-Awareness Mirror", "Log your first slip-up honestly.", totalBadHabitLogs, 1, Icons.Default.LockOpen, BrandCyan, "RARE"),
        AchievementItemSpec("Truth Seeker", "Log a slip-up for 'Lying' honestly to break the cycle.", badHabits.filter { it.name.lowercase(Locale.ROOT).contains("lie") || it.name.lowercase(Locale.ROOT).contains("lying") }.sumOf { it.logs.size }, 1, Icons.Default.History, BrandAmber, "EPIC"),
        AchievementItemSpec("Breaking Chains", "Build deep self-discipline by logging 5 honest checks.", totalBadHabitLogs, 5, Icons.Default.LinkOff, BrandRose, "LEGENDARY"),

        // ==========================================
        // --- 30 NEW HARD ACHIEVEMENTS ---
        // ==========================================
        AchievementItemSpec("Task Centurion", "Complete 100 tasks overall with unwavering focus.", completedTasksCount, 100, Icons.Default.CheckCircle, BrandGreen, "LEGENDARY"),
        AchievementItemSpec("Task Titan", "Complete 250 tasks overall across your workflow.", completedTasksCount, 250, Icons.Default.MilitaryTech, BrandCyan, "LEGENDARY"),
        AchievementItemSpec("Task Overlord Supreme", "Execute 500 total tasks — unmatched productivity!", completedTasksCount, 500, Icons.Default.WorkspacePremium, BrandRose, "MYTHIC"),
        AchievementItemSpec("Grandmaster of Focus", "Maintain a 30-day task completion streak.", streakHistory.size, 30, Icons.Default.Psychology, BrandViolet, "MYTHIC"),
        AchievementItemSpec("High Priority Conqueror", "Complete 30 critical high-priority tasks.", highPriorityCompletedCount, 30, Icons.Default.Bolt, BrandRose, "MYTHIC"),
        AchievementItemSpec("Financial Titan", "Accumulate ₹1,00,000 in total recorded income.", totalIncomeAmount.toInt(), 100000, Icons.Default.AttachMoney, BrandGreen, "MYTHIC"),
        AchievementItemSpec("Fort Knox Savings", "Save ₹50,000 in dedicated savings logs.", totalSavingsAmount.toInt(), 50000, Icons.Default.AccountBalanceWallet, BrandOrange, "LEGENDARY"),
        AchievementItemSpec("Savings Millionaire", "Accumulate ₹1,00,000 in total savings.", totalSavingsAmount.toInt(), 100000, Icons.Default.AccountBalance, BrandAmber, "MYTHIC"),
        AchievementItemSpec("Frugal Master", "Record 50 individual expense transactions.", totalExpenseCount, 50, Icons.Default.ReceiptLong, BrandViolet, "EPIC"),
        AchievementItemSpec("Net Worth Pioneer", "Reach a total Net Worth of ₹2,00,000.", netWorth.toInt().coerceAtLeast(0), 200000, Icons.Default.AccountBalanceWallet, BrandPink, "MYTHIC"),
        AchievementItemSpec("Iron Will Titan", "Reach an unbroken 100-day habit streak.", maxHabitStreak, 100, Icons.Default.Whatshot, BrandRose, "MYTHIC"),
        AchievementItemSpec("Unstoppable Olympian", "Maintain a 365-day streak on any habit.", maxHabitStreak, 365, Icons.Default.EmojiEvents, BrandAmber, "MYTHIC"),
        AchievementItemSpec("Habit Architect Elite", "Create and manage 10 active habits simultaneously.", allHabits.size, 10, Icons.Default.Build, BrandCyan, "LEGENDARY"),
        AchievementItemSpec("Habit Dynasty", "Earn 6 distinct habit milestone badges.", earnedHabitBadgesCount, 6, Icons.Default.Star, BrandViolet, "LEGENDARY"),
        AchievementItemSpec("Master Architect Hall", "Earn all 12 habit milestone badges.", earnedHabitBadgesCount, 12, Icons.Default.MilitaryTech, BrandAmber, "MYTHIC"),
        AchievementItemSpec("Hydration Overlord", "Log 500 total glasses of water.", totalWaterGlasses, 500, Icons.Default.InvertColors, BrandCyan, "LEGENDARY"),
        AchievementItemSpec("Oceanic Hydration", "Log 1,000 total glasses of water.", totalWaterGlasses, 1000, Icons.Default.Opacity, BrandIndigo, "MYTHIC"),
        AchievementItemSpec("Century Streak History", "Log 100 total days of streak history.", streakHistory.size, 100, Icons.Default.DateRange, BrandViolet, "LEGENDARY"),
        AchievementItemSpec("Yearlong Streak Legend", "Accumulate 365 days of streak history.", streakHistory.size, 365, Icons.Default.DateRange, BrandPink, "MYTHIC"),
        AchievementItemSpec("Millennium XP", "Earn 5,000 total XP points.", totalXP, 5000, Icons.Default.AutoAwesome, BrandRose, "LEGENDARY"),
        AchievementItemSpec("Master Motivated Level", "Reach Level 10 in your mastery journey.", userLevel, 10, Icons.Default.ArrowUpward, BrandCyan, "EPIC"),
        AchievementItemSpec("Legendary Sovereign Level", "Reach Level 25 in your mastery journey.", userLevel, 25, Icons.Default.EmojiEvents, BrandAmber, "MYTHIC"),
        AchievementItemSpec("Sobriety Sovereign", "Maintain 30 clean slip-free days on a bad habit.", 0, 30, Icons.Default.Shield, BrandGreen, "LEGENDARY"),
        AchievementItemSpec("Bad Habit Breaker Supreme", "Log 20 honest self-awareness check-ins.", totalBadHabitLogs, 20, Icons.Default.RemoveCircle, BrandRose, "MYTHIC"),
        AchievementItemSpec("Wishlist Collector", "Purchase 10 items from your wishlist.", purchasedWishlistCount, 10, Icons.Default.ShoppingBag, BrandPink, "LEGENDARY"),
        AchievementItemSpec("Dream Realizer", "Purchase a high-priority wishlist item worth over ₹10,000.", highValWishlistPurchased, 1, Icons.Default.CardGiftcard, BrandOrange, "EPIC"),
        AchievementItemSpec("Social Network Mogul", "Connect with 10 friends in your circle.", socialCircleSize, 10, Icons.Default.Group, BrandAmber, "LEGENDARY"),
        AchievementItemSpec("Master Sentinel Alarmist", "Configure 5 active alarms.", alarmsCount, 5, Icons.Default.Alarm, BrandViolet, "EPIC"),
        AchievementItemSpec("Seerah Master Scholar", "Read and bookmark 10 Seerah historical events.", bookmarkedSeerahCount, 10, Icons.Default.MenuBook, BrandCyan, "LEGENDARY"),
        AchievementItemSpec("Health Guardian Titan", "Log 30 weight monitoring entries.", weightEntriesCount, 30, Icons.Default.Favorite, BrandRose, "EPIC")
    )
}
