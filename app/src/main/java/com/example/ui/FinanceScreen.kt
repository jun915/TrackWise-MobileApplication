package com.example.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FinanceLogEntity
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
    val focusManager = LocalFocusManager.current

    // Date State
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val selectedDateStr = TrackWiseUtils.formatDate(selectedCalendar.time, "yyyy-MM-dd")

    // Filtered Logs
    val dayLogs = remember(financeLogs, selectedDateStr) {
        financeLogs.filter { it.date == selectedDateStr }
    }

    val totalIncome = remember(dayLogs) {
        dayLogs.filter { it.type == "income" }.sumOf { it.amount }
    }
    val totalExpense = remember(dayLogs) {
        dayLogs.filter { it.type == "expense" }.sumOf { it.amount }
    }
    val totalSavings = remember(dayLogs) {
        dayLogs.filter { it.type == "savings" }.sumOf { it.amount }
    }

    // Equation Check: income = expense + savings
    val isBalanced = remember(totalIncome, totalExpense, totalSavings) {
        kotlin.math.abs(totalIncome - (totalExpense + totalSavings)) < 0.01
    }
    val discrepancy = remember(totalIncome, totalExpense, totalSavings) {
        totalIncome - (totalExpense + totalSavings)
    }

    // Input States
    var selectedTab by remember { mutableStateOf("expense") } // "income", "expense", "savings"
    var transactionDate by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }

    // Income Inputs
    var incomeAmount by remember { mutableStateOf("") }
    var incomeSource by remember { mutableStateOf("Salary") }
    val incomeSources = listOf("Salary", "Business", "Freelance", "Investment", "Pocket Money", "Others")
    var incomeNotes by remember { mutableStateOf("") }
    var showIncomeSourceDropdown by remember { mutableStateOf(false) }

    // Expense Inputs
    var expenseAmount by remember { mutableStateOf("") }
    var selectedExpenseCategory by remember { mutableStateOf("Housing and Utilities (Fixed Essentials)") }
    var selectedExpenseTitle by remember { mutableStateOf("Rent or EMI") }
    var expenseNotes by remember { mutableStateOf("") }
    var showExpenseCategoryDropdown by remember { mutableStateOf(false) }
    var showExpenseTitleDropdown by remember { mutableStateOf(false) }

    // Expense categories mapping
    val expenseCategories = listOf(
        "Housing and Utilities (Fixed Essentials)",
        "Groceries and Daily Essentials (Variable Living)",
        "Education and Childcare (High-Priority Fixed)",
        "Transport and Commute (Daily Variable)",
        "Healthcare and Insurance (Financial Security)",
        "Lifestyle, Entertainment, and Discretionary (Flex Spends)",
        "Others"
    )

    val expenseCategoryTitles = mapOf(
        "Housing and Utilities (Fixed Essentials)" to listOf("Rent or EMI", "Society Maintenance", "Electricity Bill", "Water & LPG", "Internet & Mobile", "Others"),
        "Groceries and Daily Essentials (Variable Living)" to listOf("Kirana & Staples", "Fresh Produce", "Household Supplies", "Domestic Help", "Others"),
        "Education and Childcare (High-Priority Fixed)" to listOf("School / College Fees", "Coaching & Hobbies", "Books & Stationery", "Others"),
        "Transport and Commute (Daily Variable)" to listOf("Vehicle Fuel", "Public Commute", "Vehicle Maintenance", "Others"),
        "Healthcare and Insurance (Financial Security)" to listOf("Routine Medicines", "Doctor Consultations", "Insurance Premiums", "Others"),
        "Lifestyle, Entertainment, and Discretionary (Flex Spends)" to listOf("Dining & Delivery", "Shopping & Apparel", "Salons & Wellness", "Family Events", "Others"),
        "Others" to listOf("General Expense", "Miscellaneous")
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

    LazyColumn(
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
                    text = "Track daily income, expenses, and savings while ensuring a balanced budget equation.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // --- Date Picker Control ---
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newCal = selectedCalendar.clone() as Calendar
                        newCal.add(Calendar.DAY_OF_YEAR, -1)
                        selectedCalendar = newCal
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = BrandViolet)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = TrackWiseUtils.formatDate(selectedCalendar.time, "EEEE, MMMM d, yyyy"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (selectedDateStr == TrackWiseUtils.getTodayString()) {
                            Text(
                                text = "Today",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandViolet
                            )
                        }
                    }

                    IconButton(onClick = {
                        val newCal = selectedCalendar.clone() as Calendar
                        newCal.add(Calendar.DAY_OF_YEAR, 1)
                        selectedCalendar = newCal
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = BrandViolet)
                    }
                }
            }
        }



        // --- Add Entry Panel ---
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Add Daily Transaction",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Add Entry Sub-Tabs
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
                        ).forEach { (tabId, label, tabColor) ->
                            val isSelected = selectedTab == tabId
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) tabColor.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { selectedTab = tabId }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) tabColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Contextual Inputs based on Sub-Tabs
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
                                        viewModel.addFinanceLog(
                                            type = "expense",
                                            category = selectedExpenseCategory,
                                            title = selectedExpenseTitle,
                                            amount = amt,
                                            notes = expenseNotes.trim().ifEmpty { null },
                                            date = transactionDate
                                        )
                                        expenseAmount = ""
                                        expenseNotes = ""
                                        focusManager.clearFocus()
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
                                leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = BrandGreen) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Savings Type Dropdown Selection
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedSavingsCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Savings Instrument / Asset") },
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
                                            title = selectedSavingsCategory,
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
                                Text("Add Savings Entry", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        "income" -> {
                            // Amount
                            OutlinedTextField(
                                value = incomeAmount,
                                onValueChange = { incomeAmount = it },
                                label = { Text("Income Amount (₹)") },
                                leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null, tint = BrandViolet) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Income Source Dropdown
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
                                    incomeSources.forEach { source ->
                                        DropdownMenuItem(
                                            text = { Text(source, fontSize = 13.sp) },
                                            onClick = {
                                                incomeSource = source
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
                                            category = "Income Flow",
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
                                Text("Add Income Stream", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // --- Daily Logs History ---
        item {
            Text(
                text = "Transaction History (${dayLogs.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (dayLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MoneyOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No transactions logged on this day.",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(dayLogs, key = { it.id }) { log ->
                val typeColor = when (log.type) {
                    "income" -> BrandViolet
                    "savings" -> BrandGreen
                    else -> BrandRose
                }

                val icon = when (log.type) {
                    "income" -> Icons.Default.TrendingUp
                    "savings" -> Icons.Default.AccountBalanceWallet
                    else -> Icons.Default.LocalMall
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left category / icon status
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(typeColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = typeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Text details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = log.category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            if (!log.notes.isNullOrBlank()) {
                                Text(
                                    text = log.notes,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // Right amount & action
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${if (log.type == "income") "+" else "-"} ₹${String.format("%.2f", log.amount)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = typeColor
                            )

                            IconButton(
                                onClick = { viewModel.deleteFinanceLog(log.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete entry",
                                    tint = BrandRose,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
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
