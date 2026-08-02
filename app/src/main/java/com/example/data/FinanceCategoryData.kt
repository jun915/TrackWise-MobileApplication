package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.vector.ImageVector

data class FinanceCategoryItem(
    val id: String,
    val name: String,
    val type: String, // "expense", "income", "savings"
    val iconKey: String,
    val colorHex: Long = 0xFFF59E0B
)

object FinanceCategoryDefaults {

    val expenseCategories: SnapshotStateList<FinanceCategoryItem> = mutableStateListOf(
        FinanceCategoryItem("exp_1", "Shopping", "expense", "shopping", 0xFFF59E0B),
        FinanceCategoryItem("exp_2", "Food", "expense", "food", 0xFF10B981),
        FinanceCategoryItem("exp_3", "Phone", "expense", "phone", 0xFFEC4899),
        FinanceCategoryItem("exp_4", "Entertainment", "expense", "entertainment", 0xFF8B5CF6),
        FinanceCategoryItem("exp_5", "Education", "expense", "education", 0xFF3B82F6),
        FinanceCategoryItem("exp_6", "Beauty", "expense", "beauty", 0xFFEF4444),
        FinanceCategoryItem("exp_7", "Sports", "expense", "sports", 0xFF10B981),
        FinanceCategoryItem("exp_8", "Social", "expense", "social", 0xFF06B6D4),
        FinanceCategoryItem("exp_9", "Transportation", "expense", "transportation", 0xFFF59E0B),
        FinanceCategoryItem("exp_10", "Clothing", "expense", "clothing", 0xFF8B5CF6),
        FinanceCategoryItem("exp_11", "Car", "expense", "car", 0xFF3B82F6),
        FinanceCategoryItem("exp_12", "Alcohol", "expense", "alcohol", 0xFFEC4899),
        FinanceCategoryItem("exp_13", "Cigarettes", "expense", "cigarettes", 0xFF6B7280),
        FinanceCategoryItem("exp_14", "Electronics", "expense", "electronics", 0xFF06B6D4),
        FinanceCategoryItem("exp_15", "Travel", "expense", "travel", 0xFFF59E0B),
        FinanceCategoryItem("exp_16", "Health", "expense", "health", 0xFFEF4444),
        FinanceCategoryItem("exp_17", "Pets", "expense", "pets", 0xFF10B981),
        FinanceCategoryItem("exp_18", "Repairs", "expense", "repairs", 0xFF8B5CF6),
        FinanceCategoryItem("exp_19", "Housing", "expense", "housing", 0xFF3B82F6),
        FinanceCategoryItem("exp_20", "Home", "expense", "home", 0xFF06B6D4),
        FinanceCategoryItem("exp_21", "Gifts", "expense", "gifts", 0xFFEC4899),
        FinanceCategoryItem("exp_22", "Donations", "expense", "donations", 0xFF10B981),
        FinanceCategoryItem("exp_23", "Lottery", "expense", "lottery", 0xFFF59E0B),
        FinanceCategoryItem("exp_24", "Snacks", "expense", "snacks", 0xFFEF4444),
        FinanceCategoryItem("exp_25", "Kids", "expense", "kids", 0xFF8B5CF6),
        FinanceCategoryItem("exp_26", "Vegetables", "expense", "vegetables", 0xFF10B981),
        FinanceCategoryItem("exp_27", "Fruits", "expense", "fruits", 0xFFF59E0B)
    )

    val incomeCategories: SnapshotStateList<FinanceCategoryItem> = mutableStateListOf(
        FinanceCategoryItem("inc_1", "Salary", "income", "salary", 0xFFF59E0B),
        FinanceCategoryItem("inc_2", "Investments", "income", "investments", 0xFF10B981),
        FinanceCategoryItem("inc_3", "Part-Time", "income", "part_time", 0xFF3B82F6),
        FinanceCategoryItem("inc_4", "Bonus", "income", "bonus", 0xFF8B5CF6),
        FinanceCategoryItem("inc_5", "Others", "income", "others", 0xFF06B6D4)
    )

    val savingsCategories: SnapshotStateList<FinanceCategoryItem> = mutableStateListOf(
        FinanceCategoryItem("sav_1", "Emergency Fund", "savings", "emergency", 0xFF3B82F6),
        FinanceCategoryItem("sav_2", "Fixed Deposit", "savings", "fd", 0xFF10B981),
        FinanceCategoryItem("sav_3", "Retirement", "savings", "retirement", 0xFF8B5CF6),
        FinanceCategoryItem("sav_4", "Mutual Funds", "savings", "mutual_funds", 0xFFF59E0B),
        FinanceCategoryItem("sav_5", "Gold", "savings", "gold", 0xFFEAB308),
        FinanceCategoryItem("sav_6", "Real Estate", "savings", "real_estate", 0xFF06B6D4),
        FinanceCategoryItem("sav_7", "Savings Account", "savings", "savings_account", 0xFF10B981),
        FinanceCategoryItem("sav_8", "Piggy Bank", "savings", "piggy_bank", 0xFFEC4899),
        FinanceCategoryItem("sav_9", "Stocks", "savings", "stocks", 0xFF3B82F6),
        FinanceCategoryItem("sav_10", "Crypto", "savings", "crypto", 0xFF8B5CF6),
        FinanceCategoryItem("sav_11", "Others", "savings", "others", 0xFF6B7280)
    )

    fun moveCategoryUp(list: SnapshotStateList<FinanceCategoryItem>, index: Int) {
        if (index > 0 && index < list.size) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
        }
    }

    fun moveCategoryDown(list: SnapshotStateList<FinanceCategoryItem>, index: Int) {
        if (index >= 0 && index < list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
        }
    }

    fun getCategoryIcon(key: String): ImageVector {
        val allCategories = expenseCategories + incomeCategories + savingsCategories
        val resolvedKey = allCategories.find { it.name.equals(key, ignoreCase = true) }?.iconKey ?: key
        return when (resolvedKey.lowercase()) {
            "shopping" -> Icons.Default.ShoppingCart
            "food" -> Icons.Default.Restaurant
            "phone" -> Icons.Default.Smartphone
            "entertainment" -> Icons.Default.SportsEsports
            "education" -> Icons.Default.School
            "beauty" -> Icons.Default.ContentCut
            "sports" -> Icons.Default.DirectionsRun
            "social" -> Icons.Default.People
            "transportation" -> Icons.Default.DirectionsBus
            "clothing" -> Icons.Default.Checkroom
            "car" -> Icons.Default.DirectionsCar
            "alcohol" -> Icons.Default.LocalBar
            "cigarettes" -> Icons.Default.SmokingRooms
            "electronics" -> Icons.Default.Computer
            "travel" -> Icons.Default.Flight
            "health" -> Icons.Default.Favorite
            "pets" -> Icons.Default.Pets
            "repairs" -> Icons.Default.Build
            "housing" -> Icons.Default.HomeWork
            "home" -> Icons.Default.Home
            "gifts" -> Icons.Default.CardGiftcard
            "donations" -> Icons.Default.VolunteerActivism
            "lottery" -> Icons.Default.Casino
            "snacks" -> Icons.Default.Fastfood
            "kids" -> Icons.Default.ChildCare
            "vegetables" -> Icons.Default.Agriculture
            "fruits" -> Icons.Default.Eco
            "salary" -> Icons.Default.Work
            "investments" -> Icons.Default.TrendingUp
            "part_time" -> Icons.Default.PanTool
            "bonus" -> Icons.Default.EmojiEvents
            "emergency" -> Icons.Default.Shield
            "fd" -> Icons.Default.AccountBalance
            "retirement" -> Icons.Default.BeachAccess
            "mutual_funds" -> Icons.Default.ShowChart
            "gold" -> Icons.Default.MonetizationOn
            "real_estate" -> Icons.Default.LocationCity
            "savings_account" -> Icons.Default.AccountBalanceWallet
            "piggy_bank" -> Icons.Default.Savings
            "stocks" -> Icons.Default.SsidChart
            "crypto" -> Icons.Default.CurrencyBitcoin
            "settings" -> Icons.Default.Add
            else -> Icons.Default.Category
        }
    }
}
