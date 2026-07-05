package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.utils.TrackWiseUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinanceScreen(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    val financeLogs by viewModel.allFinanceLogs.collectAsState()
    val netWorthItems by viewModel.allNetWorthItems.collectAsState()
    val focusManager = LocalFocusManager.current

    // Date State
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val selectedDateStr = TrackWiseUtils.formatDate(selectedCalendar.time, "yyyy-MM-dd")

    // Auto-populate defaults for Net Worth on first launch
    LaunchedEffect(Unit) {
        viewModel.populateDefaultNetWorthItemsIfEmpty()
    }

    val currentMonthStr = remember(selectedDateStr) {
        if (selectedDateStr.length >= 7) selectedDateStr.substring(0, 7) else "2026-07"
    }

    // Filtered Logs - Monthly Entries Log
    val monthLogs = remember(financeLogs, currentMonthStr) {
        financeLogs.filter { it.date.startsWith(currentMonthStr) }.sortedByDescending { it.date }
    }

    val totalIncome = remember(monthLogs) {
        monthLogs.filter { it.type == "income" }.sumOf { it.amount }
    }
    val totalExpense = remember(monthLogs) {
        monthLogs.filter {
            it.type == "expense" && (it.spendSource == null || it.spendSource == "Cash / Current Income" || it.spendSource == "Cash / Cash / Current Income" || it.spendSource == "")
        }.sumOf { it.amount }
    }
    val totalSavings = remember(monthLogs) {
        monthLogs.filter { it.type == "savings" }.sumOf { it.amount }
    }

    // Equation Check: income = expense + savings
    val isBalanced = remember(totalIncome, totalExpense, totalSavings) {
        kotlin.math.abs(totalIncome - (totalExpense + totalSavings)) < 0.01
    }
    val discrepancy = remember(totalIncome, totalExpense, totalSavings) {
        totalIncome - (totalExpense + totalSavings)
    }

    // Overall Balance and Daily Target States
    val overallIncome = remember(financeLogs) { financeLogs.filter { it.type == "income" }.sumOf { it.amount } }
    val overallExpense = remember(financeLogs) {
        financeLogs.filter {
            it.type == "expense" && (it.spendSource == null || it.spendSource == "Cash / Current Income" || it.spendSource == "Cash / Cash / Current Income" || it.spendSource == "")
        }.sumOf { it.amount }
    }
    val overallSavings = remember(financeLogs) { financeLogs.filter { it.type == "savings" }.sumOf { it.amount } }
    val overallBalance = overallIncome - (overallExpense + overallSavings)

    val userProfile by viewModel.userProfile.collectAsState()
    val monthlyTarget = userProfile?.financeDailyTarget ?: 30000.0

    val monthlyExpenses = remember(financeLogs, currentMonthStr) {
        financeLogs.filter {
            it.date.startsWith(currentMonthStr) && it.type == "expense" && (it.spendSource == null || it.spendSource == "Cash / Current Income" || it.spendSource == "Cash / Cash / Current Income" || it.spendSource == "")
        }.sumOf { it.amount }
    }
    val remainingBalance = remember(monthlyTarget, monthlyExpenses) {
        monthlyTarget - monthlyExpenses
    }

    // Deficit & Daily target States
    var showDeficitWarningDialog by remember { mutableStateOf(false) }
    var deficitAmountNeeded by remember { mutableStateOf(0.0) }
    var pendingExpenseAmount by remember { mutableStateOf(0.0) }
    var pendingExpenseCategory by remember { mutableStateOf("") }
    var pendingExpenseTitle by remember { mutableStateOf("") }
    var pendingExpenseNotes by remember { mutableStateOf("") }
    var pendingTransactionDate by remember { mutableStateOf("") }
    var pendingSpendSource by remember { mutableStateOf<String?>(null) }

    // Navigation and Input States
    var currentMainTab by remember { mutableStateOf("Monthly Budget") } // "Monthly Budget" or "Net Worth"
    var selectedTab by remember { mutableStateOf("expense") } // "income", "expense", "savings"
    var transactionDate by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }

    val filteredMonthLogs = remember(monthLogs, selectedTab) {
        monthLogs.filter { it.type == selectedTab }
    }

    // Income Inputs
    var incomeAmount by remember { mutableStateOf("") }
    var incomeSource by remember { mutableStateOf("Salary") }
    val incomeSources = listOf("Salary", "Business", "Freelance", "Investment", "Pocket Money", "Others")
    var incomeNotes by remember { mutableStateOf("") }
    var showIncomeSourceDropdown by remember { mutableStateOf(false) }

    // Expense Inputs
    var expenseAmount by remember { mutableStateOf("") }
    var selectedExpenseCategory by remember { mutableStateOf("Housing and Utilities") }
    var selectedExpenseTitle by remember { mutableStateOf("Rent or EMI") }
    var expenseNotes by remember { mutableStateOf("") }
    var showExpenseCategoryDropdown by remember { mutableStateOf(false) }
    var showExpenseTitleDropdown by remember { mutableStateOf(false) }

    // Spend Source Selector
    var selectedSpendSource by remember { mutableStateOf("Cash / Current Income") }
    var showSpendSourceDropdown by remember { mutableStateOf(false) }
    val spendSourceOptions = remember(netWorthItems) {
        listOf("Cash / Current Income") + netWorthItems.filter { it.type == "asset" }.map { it.name }
    }

    // Expense categories mapping
    val expenseCategories = listOf(
        "Housing and Utilities",
        "Groceries and Daily Essentials",
        "Education and Childcare",
        "Transport and Commute",
        "Healthcare and Insurance",
        "Lifestyle, Entertainment, and Discretionary",
        "Others"
    )

    val expenseCategoryTitles = mapOf(
        "Housing and Utilities" to listOf("Rent or EMI", "Society Maintenance", "Electricity Bill", "Water & LPG", "Internet & Mobile", "Others"),
        "Groceries and Daily Essentials" to listOf("Kirana & Staples", "Fresh Produce", "Household Supplies", "Domestic Help", "Others"),
        "Education and Childcare" to listOf("School / College Fees", "Coaching & Hobbies", "Books & Stationery", "Others"),
        "Transport and Commute" to listOf("Vehicle Fuel", "Public Commute", "Vehicle Maintenance", "Others"),
        "Healthcare and Insurance" to listOf("Routine Medicines", "Doctor Consultations", "Insurance Premiums", "Others"),
        "Lifestyle, Entertainment, and Discretionary" to listOf("Dining & Delivery", "Shopping & Apparel", "Salons & Wellness", "Family Events", "Others"),
        "Others" to listOf("General Expense", "Miscellaneous", "Others")
    )

    // Savings Inputs
    var savingsAmount by remember { mutableStateOf("") }
    var selectedSavingsCategory by remember { mutableStateOf("Simple Savings in Account") }
    var savingsNotes by remember { mutableStateOf("") }
    var showSavingsCategoryDropdown by remember { mutableStateOf(false) }

    val savingsCategories = listOf(
        "PPF",
        "FD",
        "RD",
        "NPS",
        "Mutual Funds",
        "EPF",
        "Stocks",
        "Simple Savings in Account",
        "Others"
    )

    // Sync selected title when category changes
    LaunchedEffect(selectedExpenseCategory) {
        val titles = expenseCategoryTitles[selectedExpenseCategory] ?: listOf("Others")
        if (selectedExpenseTitle !in titles) {
            selectedExpenseTitle = titles.first()
        }
    }

    val financeListState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(financeListState.isScrollInProgress) {
        if (financeListState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    LazyColumn(
        state = financeListState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Section ---
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Finance Tracker",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Track monthly budget, expenses, and savings while ensuring a balanced budget equation.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // --- Main Section Selector TabRow ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "Monthly Budget" to Icons.Default.AccountBalance,
                    "Net Worth" to Icons.Default.PieChart
                ).forEach { (tabName, tabIcon) ->
                    val isSelected = currentMainTab == tabName
                    val tabColor = if (tabName == "Monthly Budget") BrandViolet else BrandGreen
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) tabColor.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { currentMainTab = tabName }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = tabIcon,
                                contentDescription = null,
                                tint = if (isSelected) tabColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tabName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) tabColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        if (currentMainTab == "Monthly Budget") {
            // --- Monthly Budget Target Panel ---
            item {
                var showEditTargetDialog by remember { mutableStateOf(false) }
                val monthlyIncome = remember(financeLogs, currentMonthStr) {
                    financeLogs.filter { it.date.startsWith(currentMonthStr) && it.type == "income" }.sumOf { it.amount }
                }
                val monthlySavings = remember(financeLogs, currentMonthStr) {
                    financeLogs.filter { it.date.startsWith(currentMonthStr) && it.type == "savings" }.sumOf { it.amount }
                }
                val monthlyRemainingBalance = monthlyIncome - (monthlyExpenses + monthlySavings)

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditTargetDialog = true }
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(BrandViolet.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = "Monthly Limit: ₹${String.format("%.0f", monthlyTarget)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            IconButton(
                                onClick = { showEditTargetDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Monthly Target Limit", tint = BrandRose, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Budget Bar
                        val progress = if (monthlyTarget > 0) (monthlyExpenses / monthlyTarget).toFloat().coerceIn(0f, 1f) else 0f
                        val barColor = if (monthlyExpenses > monthlyTarget) BrandRose else BrandGreen
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Budget Spent: ${String.format("%.0f", progress * 100)}%",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Remaining Budget: ₹${String.format("%.1f", remainingBalance)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingBalance >= 0) BrandGreen else BrandRose
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )

                        // 4 metrics grid: Monthly Income, Monthly Expenses, Monthly Savings, Remaining Balance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TOTAL INCOME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("₹${String.format("%.1f", monthlyIncome)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TOTAL EXPENSES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("₹${String.format("%.1f", monthlyExpenses)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandRose)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TOTAL SAVINGS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("₹${String.format("%.1f", monthlySavings)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandCyan)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("NET BALANCE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${String.format("%.1f", monthlyRemainingBalance)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (monthlyRemainingBalance >= 0) BrandGreen else BrandRose
                                )
                            }
                        }
                    }
                }

                if (showEditTargetDialog) {
                    var inputLimit by remember { mutableStateOf(monthlyTarget.toInt().toString()) }
                    AlertDialog(
                        onDismissRequest = { showEditTargetDialog = false },
                        title = { Text("Update Monthly Limit") },
                        text = {
                            OutlinedTextField(
                                value = inputLimit,
                                onValueChange = { inputLimit = it },
                                label = { Text("Monthly Budget Target (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val lim = inputLimit.toDoubleOrNull()
                                    if (lim != null && lim > 0) {
                                        viewModel.updateFinanceDailyTarget(lim)
                                    }
                                    showEditTargetDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandRose)
                            ) {
                                Text("Save", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditTargetDialog = false }) {
                                Text("Cancel", color = Color.Gray)
                            }
                        }
                    )
                }
            }



            // --- Add Daily Transaction Panel ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title
                        Text(
                            text = "ADD TRANSACTION FOR TODAY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            letterSpacing = 1.sp
                        )

                        // Sub-Tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                Triple("expense", "Expense", BrandRose),
                                Triple("savings", "Savings", BrandGreen),
                                Triple("income", "Income", BrandViolet)
                            ).forEach { (tabId, label, tabTint) ->
                                val isTabSel = selectedTab == tabId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isTabSel) tabTint.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { selectedTab = tabId }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isTabSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isTabSel) tabTint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        when (selectedTab) {
                            "expense" -> {
                                // Amount
                                OutlinedTextField(
                                    value = expenseAmount,
                                    onValueChange = { expenseAmount = it },
                                    label = { Text("Expense Amount (₹)") },
                                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = BrandRose) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Category Dropdown Selection
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedExpenseCategory,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Expense Category") },
                                        trailingIcon = {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandRose)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { showExpenseCategoryDropdown = !showExpenseCategoryDropdown }
                                    )
                                    DropdownMenu(
                                        expanded = showExpenseCategoryDropdown,
                                        onDismissRequest = { showExpenseCategoryDropdown = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        expenseCategories.forEach { category ->
                                            DropdownMenuItem(
                                                text = { Text(category, fontSize = 13.sp) },
                                                onClick = {
                                                    selectedExpenseCategory = category
                                                    showExpenseCategoryDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Dynamic Title Dropdown Selection
                                val availableTitles = expenseCategoryTitles[selectedExpenseCategory] ?: listOf("Others")
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedExpenseTitle,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Item Title / Subcategory") },
                                        trailingIcon = {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandRose)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { showExpenseTitleDropdown = !showExpenseTitleDropdown }
                                    )
                                    DropdownMenu(
                                        expanded = showExpenseTitleDropdown,
                                        onDismissRequest = { showExpenseTitleDropdown = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        availableTitles.forEach { title ->
                                            DropdownMenuItem(
                                                text = { Text(title, fontSize = 13.sp) },
                                                onClick = {
                                                    selectedExpenseTitle = title
                                                    showExpenseTitleDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Spend Source Selector Dropdown
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedSpendSource,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Spend Source (Deduct Asset)") },
                                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = BrandRose) },
                                        trailingIcon = {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandRose)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { showSpendSourceDropdown = !showSpendSourceDropdown }
                                    )
                                    DropdownMenu(
                                        expanded = showSpendSourceDropdown,
                                        onDismissRequest = { showSpendSourceDropdown = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        spendSourceOptions.forEach { source ->
                                            DropdownMenuItem(
                                                text = { Text(source, fontSize = 13.sp) },
                                                onClick = {
                                                    selectedSpendSource = source
                                                    showSpendSourceDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Notes
                                OutlinedTextField(
                                    value = expenseNotes,
                                    onValueChange = { expenseNotes = it },
                                    label = { Text("Notes (Optional)") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Date Picker
                                TransactionDatePicker(
                                    dateStr = transactionDate,
                                    tintColor = BrandRose,
                                    onDateSelected = { transactionDate = it }
                                )

                                Button(
                                    onClick = {
                                        val amt = expenseAmount.toDoubleOrNull()
                                        if (amt != null && amt > 0) {
                                            val sourceArg = if (selectedSpendSource == "Cash / Current Income") null else selectedSpendSource
                                            if (overallBalance - amt < 0) {
                                                deficitAmountNeeded = amt - overallBalance
                                                pendingExpenseAmount = amt
                                                pendingExpenseCategory = selectedExpenseCategory
                                                pendingExpenseTitle = selectedExpenseTitle
                                                pendingExpenseNotes = expenseNotes.trim().ifEmpty { "" }
                                                pendingTransactionDate = transactionDate
                                                pendingSpendSource = sourceArg
                                                showDeficitWarningDialog = true
                                            } else {
                                                if (transactionDate.startsWith(currentMonthStr) && monthlyExpenses + amt > monthlyTarget) {
                                                    viewModel.addNotification(
                                                        title = "⚠️ Monthly Spending Limit Breached!",
                                                        message = "You spent ₹${String.format("%.1f", monthlyExpenses + amt)} this month, exceeding your monthly target limit of ₹${monthlyTarget}."
                                                    )
                                                }

                                                viewModel.addFinanceLog(
                                                    type = "expense",
                                                    category = selectedExpenseCategory,
                                                    title = selectedExpenseTitle,
                                                    amount = amt,
                                                    notes = expenseNotes.trim().ifEmpty { null },
                                                    date = transactionDate,
                                                    spendSource = sourceArg
                                                )
                                                expenseAmount = ""
                                                expenseNotes = ""
                                                focusManager.clearFocus()
                                            }
                                        }
                                    },
                                    enabled = expenseAmount.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandRose),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Add Expense", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            "savings" -> {
                                // Amount
                                OutlinedTextField(
                                    value = savingsAmount,
                                    onValueChange = { savingsAmount = it },
                                    label = { Text("Savings Amount (₹)") },
                                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = BrandGreen) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Category Dropdown Selection
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedSavingsCategory,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Savings Destination / Instrument") },
                                        trailingIcon = {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandGreen)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { showSavingsCategoryDropdown = !showSavingsCategoryDropdown }
                                    )
                                    DropdownMenu(
                                        expanded = showSavingsCategoryDropdown,
                                        onDismissRequest = { showSavingsCategoryDropdown = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        savingsCategories.forEach { category ->
                                            DropdownMenuItem(
                                                text = { Text(category, fontSize = 13.sp) },
                                                onClick = {
                                                    selectedSavingsCategory = category
                                                    showSavingsCategoryDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Notes
                                OutlinedTextField(
                                    value = savingsNotes,
                                    onValueChange = { savingsNotes = it },
                                    label = { Text("Notes (Optional)") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Date Picker
                                TransactionDatePicker(
                                    dateStr = transactionDate,
                                    tintColor = BrandGreen,
                                    onDateSelected = { transactionDate = it }
                                )

                                Button(
                                    onClick = {
                                        val amt = savingsAmount.toDoubleOrNull()
                                        if (amt != null && amt > 0) {
                                            viewModel.addFinanceLog(
                                                type = "savings",
                                                category = selectedSavingsCategory,
                                                title = "Monthly Savings Plan",
                                                amount = amt,
                                                notes = savingsNotes.trim().ifEmpty { null },
                                                date = transactionDate
                                            )
                                            savingsAmount = ""
                                            savingsNotes = ""
                                            focusManager.clearFocus()
                                        }
                                    },
                                    enabled = savingsAmount.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Add Savings", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            "income" -> {
                                // Amount
                                OutlinedTextField(
                                    value = incomeAmount,
                                    onValueChange = { incomeAmount = it },
                                    label = { Text("Income Amount (₹)") },
                                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = BrandViolet) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Source Dropdown Selection
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = incomeSource,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Income Source") },
                                        trailingIcon = {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandViolet)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { showIncomeSourceDropdown = !showIncomeSourceDropdown }
                                    )
                                    DropdownMenu(
                                        expanded = showIncomeSourceDropdown,
                                        onDismissRequest = { showIncomeSourceDropdown = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        incomeSources.forEach { src ->
                                            DropdownMenuItem(
                                                text = { Text(src, fontSize = 13.sp) },
                                                onClick = {
                                                    incomeSource = src
                                                    showIncomeSourceDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Notes
                                OutlinedTextField(
                                    value = incomeNotes,
                                    onValueChange = { incomeNotes = it },
                                    label = { Text("Notes (Optional)") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Date Picker
                                TransactionDatePicker(
                                    dateStr = transactionDate,
                                    tintColor = BrandViolet,
                                    onDateSelected = { transactionDate = it }
                                )

                                Button(
                                    onClick = {
                                        val amt = incomeAmount.toDoubleOrNull()
                                        if (amt != null && amt > 0) {
                                            viewModel.addFinanceLog(
                                                type = "income",
                                                category = "Income",
                                                title = incomeSource,
                                                amount = amt,
                                                notes = incomeNotes.trim().ifEmpty { null },
                                                date = transactionDate
                                            )
                                            incomeAmount = ""
                                            incomeNotes = ""
                                            focusManager.clearFocus()
                                        }
                                    },
                                    enabled = incomeAmount.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Add Income", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // --- Monthly Logs History ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MONTHLY ENTRIES LOGS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            if (filteredMonthLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No transactions recorded for this category this month",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                items(filteredMonthLogs, key = { it.id }) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val itemTint = when (log.type) {
                                    "income" -> BrandViolet
                                    "expense" -> BrandRose
                                    else -> BrandGreen
                                }
                                val icon = when (log.type) {
                                    "income" -> Icons.Default.TrendingUp
                                    "expense" -> Icons.Default.TrendingDown
                                    else -> Icons.Default.Savings
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(itemTint.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = itemTint, modifier = Modifier.size(18.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val formattedDate = try {
                                            val date = com.example.utils.TrackWiseUtils.parseDate(log.date, "yyyy-MM-dd")
                                            com.example.utils.TrackWiseUtils.formatDate(date, "dd MMM")
                                        } catch (e: Exception) {
                                            log.date
                                        }
                                        Text(
                                            text = "($formattedDate)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "•",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                        )
                                        Text(
                                            text = log.category,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                        )
                                        if (!log.spendSource.isNullOrBlank()) {
                                            Text(
                                                text = "• Src: ${log.spendSource}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandRose
                                            )
                                        }
                                    }
                                    if (!log.notes.isNullOrBlank()) {
                                        Text(
                                            text = log.notes,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                val logSign = if (log.type == "income") "+" else "-"
                                val logTint = when (log.type) {
                                    "income" -> BrandViolet
                                    "expense" -> BrandRose
                                    else -> BrandGreen
                                }
                                Text(
                                    text = "$logSign ₹${String.format("%.1f", log.amount)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = logTint
                                )

                                IconButton(onClick = { viewModel.deleteFinanceLog(log.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Log", tint = BrandRose, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- Net Worth Section ---
            item {
                val totalAssets = netWorthItems.filter { it.type == "asset" }.sumOf { it.amount }
                val totalLoans = netWorthItems.filter { it.type == "loan" }.sumOf { it.amount }
                val totalLiabilities = netWorthItems.filter { it.type == "liability" }.sumOf { it.amount }
                val netWorth = totalAssets - (totalLoans + totalLiabilities)

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "NET WORTH OVERVIEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandGreen,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "₹${String.format("%,.2f", netWorth)}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (netWorth >= 0) BrandGreen else BrandRose
                                )
                                Text(
                                    text = "Total Wealth - Debts & Liabilities",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(BrandGreen.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Assets block
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Assets",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "₹${String.format("%,.0f", totalAssets)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreen
                                )
                            }

                            // Debts block
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Debts",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "₹${String.format("%,.0f", totalLoans + totalLiabilities)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandRose
                                )
                            }
                        }
                    }
                }
            }

            // Quick Info Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandViolet.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(18.dp))
                        Text(
                            text = "💡 Monthly savings are added to Assets. Expenses from Spend Sources are deducted.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = BrandViolet
                        )
                    }
                }
            }

            // Add Form Button / Expanding Card
            item {
                var showAddForm by remember { mutableStateOf(false) }
                var itemName by remember { mutableStateOf("") }
                var itemAmount by remember { mutableStateOf("") }
                var itemType by remember { mutableStateOf("asset") }
                var showSavingsDropdownInAddNw by remember { mutableStateOf(false) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ADD NEW ACCOUNT/ASSET/DEBT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandViolet
                            )
                            IconButton(onClick = { showAddForm = !showAddForm }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = BrandViolet
                                )
                            }
                        }

                        if (showAddForm) {
                            // Dropdown with savings options
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = if (itemName in savingsCategories) itemName else "Custom Name",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Savings Option (Select to Auto-Fill Name)") },
                                    trailingIcon = {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandViolet)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { showSavingsDropdownInAddNw = !showSavingsDropdownInAddNw }
                                )
                                DropdownMenu(
                                    expanded = showSavingsDropdownInAddNw,
                                    onDismissRequest = { showSavingsDropdownInAddNw = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Custom/None (Type name below)", fontSize = 13.sp) },
                                        onClick = {
                                            showSavingsDropdownInAddNw = false
                                        }
                                    )
                                    savingsCategories.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category, fontSize = 13.sp) },
                                            onClick = {
                                                itemName = category
                                                itemType = "asset" // Set to asset automatically
                                                showSavingsDropdownInAddNw = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = itemName,
                                onValueChange = { itemName = it },
                                label = { Text("Name (e.g. SBI Account, Car Loan)") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = itemAmount,
                                onValueChange = { itemAmount = it },
                                label = { Text("Initial Balance (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Type selection Row
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Asset" to "asset", "Loan" to "loan", "Liability" to "liability").forEach { (label, value) ->
                                    val isSel = itemType == value
                                    val selColor = when(value) {
                                        "asset" -> BrandGreen
                                        "loan" -> BrandViolet
                                        else -> BrandRose
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) selColor.copy(alpha = 0.15f) else Color.Transparent)
                                            .border(1.dp, if (isSel) selColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .clickable { itemType = value }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) selColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val amt = itemAmount.toDoubleOrNull() ?: 0.0
                                    if (itemName.isNotBlank()) {
                                        viewModel.addNetWorthItem(itemName, itemType, amt)
                                        itemName = ""
                                        itemAmount = ""
                                        showAddForm = false
                                    }
                                },
                                enabled = itemName.isNotBlank() && itemAmount.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Add to Net Worth", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Assets Section
            item {
                Text(
                    text = "Assets (Positive Value) 🏦",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandGreen,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val assetItems = netWorthItems.filter { it.type == "asset" }
            if (assetItems.isEmpty()) {
                item {
                    Text(
                        text = "No assets added. Savings will create assets here.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            } else {
                items(assetItems, key = { it.id }) { item ->
                    NetWorthItemRow(item, BrandGreen, viewModel)
                }
            }

            // Loans Section
            item {
                Text(
                    text = "Loans (Mortgages / Debts) 🏠",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandViolet,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val loanItems = netWorthItems.filter { it.type == "loan" }
            if (loanItems.isEmpty()) {
                item {
                    Text(
                        text = "No loans logged.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            } else {
                items(loanItems, key = { it.id }) { item ->
                    NetWorthItemRow(item, BrandViolet, viewModel)
                }
            }

            // Liabilities Section
            item {
                Text(
                    text = "Liabilities (Credit Cards / Dues) 💳",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandRose,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val liabilityItems = netWorthItems.filter { it.type == "liability" }
            if (liabilityItems.isEmpty()) {
                item {
                    Text(
                        text = "No other liabilities logged.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            } else {
                items(liabilityItems, key = { it.id }) { item ->
                    NetWorthItemRow(item, BrandRose, viewModel)
                }
            }
        }
    }

    // --- Warning Dialog for deficit ---
    if (showDeficitWarningDialog) {
        AlertDialog(
            onDismissRequest = { showDeficitWarningDialog = false },
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = BrandRose)
                    Text("Deficit Detected!")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your cumulative expenses and savings will total ₹${String.format("%.2f", overallExpense + overallSavings + pendingExpenseAmount)}, which exceeds your cumulative income of ₹${String.format("%.2f", overallIncome)}.",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "You need an additional ₹${String.format("%.2f", deficitAmountNeeded)} of balance. You must either add more income or borrow credit.",
                        color = BrandRose,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Quick Borrow Credit
                        viewModel.addFinanceLog(
                            type = "income",
                            category = "Others",
                            title = "Pocket Money",
                            amount = deficitAmountNeeded,
                            notes = "Borrowed Credit to cover deficit",
                            date = pendingTransactionDate
                        )
                        viewModel.addFinanceLog(
                            type = "expense",
                            category = pendingExpenseCategory,
                            title = pendingExpenseTitle,
                            amount = pendingExpenseAmount,
                            notes = pendingExpenseNotes.ifEmpty { null },
                            date = pendingTransactionDate,
                            spendSource = pendingSpendSource
                        )

                        viewModel.addNotification(
                            title = "💰 Borrowed Credit Applied",
                            message = "Automatically recorded ₹${String.format("%.2f", deficitAmountNeeded)} as Borrowed Credit to cover your expense."
                        )

                        // Clear inputs
                        expenseAmount = ""
                        expenseNotes = ""
                        focusManager.clearFocus()
                        showDeficitWarningDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                ) {
                    Text("Borrow Credit (₹${String.format("%.1f", deficitAmountNeeded)})", color = Color.White)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = {
                        selectedTab = "income"
                        incomeAmount = deficitAmountNeeded.toString()
                        showDeficitWarningDialog = false
                    }) {
                        Text("Add Income", color = BrandViolet)
                    }
                    TextButton(onClick = { showDeficitWarningDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        )
    }
}

@Composable
fun NetWorthItemRow(
    item: com.example.data.NetWorthItemEntity,
    color: Color,
    viewModel: TrackWiseViewModel
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = item.type.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "₹${String.format("%,.2f", item.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = color
                )

                IconButton(
                    onClick = { viewModel.deleteNetWorthItem(item.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Item",
                        tint = BrandRose,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionDatePicker(
    dateStr: String,
    tintColor: Color,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val parsedDate = remember(dateStr) { TrackWiseUtils.parseDate(dateStr) }
    val calendar = remember(parsedDate) {
        Calendar.getInstance().apply { time = parsedDate }
    }

    val datePickerDialog = remember(calendar) {
        android.app.DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                }
                onDateSelected(TrackWiseUtils.formatDate(selectedCal.time, "yyyy-MM-dd"))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = dateStr,
            onValueChange = {},
            readOnly = true,
            label = { Text("Transaction Date") },
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = tintColor) },
            trailingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit Date", tint = tintColor, modifier = Modifier.size(16.dp)) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { datePickerDialog.show() }
        )
    }
}
