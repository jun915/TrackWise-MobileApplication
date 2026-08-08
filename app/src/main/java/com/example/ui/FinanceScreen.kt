package com.example.ui

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.utils.TrackWiseUtils
import java.text.SimpleDateFormat
import java.util.*

// -----------------------------------------------------------------------------
// RECURRING TRANSACTION MODEL
// -----------------------------------------------------------------------------
data class RecurringTransaction(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String = "Expense",
    val category: String = "Shopping",
    val frequency: String = "Monthly",
    val startDate: String = TrackWiseUtils.getTodayString(),
    val amount: Double = 0.0,
    val numberOfTransactions: String = "Unlimited",
    val note: String? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FinanceScreen(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier,
    isSpeedDialOpen: Boolean = false,
    onSpeedDialOpenChange: (Boolean) -> Unit = {},
    currentViewMode: String = "home",
    onViewModeChange: (String) -> Unit = {},
    isSearchActive: Boolean = false,
    onSearchActiveChange: (Boolean) -> Unit = {},
    presetTabForAddSheet: String = "expense",
    onPresetTabChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val financeLogs by viewModel.allFinanceLogs.collectAsState()
    val isShowAddSheet by viewModel.showAddFinanceSheet.collectAsState()

    // Screen-level state
    var selectedMonthYear by remember { mutableStateOf(Calendar.getInstance()) }
    var showCategorySettings by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMonthYearPicker by remember { mutableStateOf(false) }

    // Selected Date Detail for Calendar View (Matching 2nd Screenshot)
    var selectedDateDetail by remember { mutableStateOf<String?>(null) }

    val pinnedFinanceLogIds by viewModel.pinnedFinanceLogIds.collectAsState()

    // Transaction editing, deleting & details
    var editingFinanceLog by remember { mutableStateOf<FinanceLogEntity?>(null) }
    var deletingFinanceLog by remember { mutableStateOf<FinanceLogEntity?>(null) }
    var selectedDetailFinanceLog by remember { mutableStateOf<FinanceLogEntity?>(null) }

    // Recurring Transactions Screen & Add Sheet
    var showRecurringTransactionsScreen by remember { mutableStateOf(false) }
    var showAddRecurringTransactionSheet by remember { mutableStateOf(false) }
    var recurringTransactionsList by remember { mutableStateOf(listOf<RecurringTransaction>()) }

    // Month String formatting (e.g., "2026-08", "Aug 2026")
    val currentMonthKey = remember(selectedMonthYear.time) {
        TrackWiseUtils.formatDate(selectedMonthYear.time, "yyyy-MM")
    }
    val currentMonthDisplay = remember(selectedMonthYear.time) {
        TrackWiseUtils.formatDate(selectedMonthYear.time, "MMM yyyy")
    }

    // Filter logs for selected month
    val monthLogs = remember(financeLogs, currentMonthKey) {
        financeLogs.filter { it.date.startsWith(currentMonthKey) }.sortedByDescending { it.date }
    }

    // Calculation Totals
    val totalExpense = remember(monthLogs) {
        monthLogs.filter { it.type == "expense" }.sumOf { it.amount }
    }
    val totalIncome = remember(monthLogs) {
        monthLogs.filter { it.type == "income" }.sumOf { it.amount }
    }
    val totalSavings = remember(monthLogs) {
        monthLogs.filter { it.type == "savings" }.sumOf { it.amount }
    }
    val totalBalance = remember(totalIncome, totalExpense, totalSavings) {
        totalIncome - totalExpense - totalSavings
    }

    // Colors matching overall app theme
    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (isSpeedDialOpen) 16.dp else 0.dp)
        ) {

            // Top Money Tracker Header Bar
            if (currentViewMode == "net_worth") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net Worth Portfolio",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = { onViewModeChange("home") }) {
                            Icon(Icons.Default.Home, contentDescription = "Home", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } else {
                MoneyTrackerHeader(
                    currentMonthDisplay = currentMonthDisplay,
                    totalExpense = totalExpense,
                    totalIncome = totalIncome,
                    totalBalance = totalBalance,
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onPrevMonth = {
                        val newCal = selectedMonthYear.clone() as Calendar
                        newCal.add(Calendar.MONTH, -1)
                        selectedMonthYear = newCal
                    },
                    onNextMonth = {
                        val newCal = selectedMonthYear.clone() as Calendar
                        newCal.add(Calendar.MONTH, 1)
                        selectedMonthYear = newCal
                    },
                    onMonthClick = {
                        showMonthYearPicker = true
                    }
                )
            }

            // View Content (Home / Reports / Calendar / DateDetail)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (selectedDateDetail != null) {
                    // Date Detail View (Screenshot 2)
                    DateDetailView(
                        dateStr = selectedDateDetail!!,
                        allLogs = monthLogs,
                        onBack = { selectedDateDetail = null },
                        onSelectLog = { selectedDetailFinanceLog = it },
                        onAddClick = {
                            onPresetTabChange("expense")
                            viewModel.openAddFinanceSheet()
                        }
                    )
                } else {
                    when (currentViewMode) {
                        "home" -> MoneyTrackerHomeView(
                            monthLogs = if (searchQuery.isBlank()) monthLogs else monthLogs.filter {
                                it.category.contains(searchQuery, ignoreCase = true) ||
                                        it.title.contains(searchQuery, ignoreCase = true) ||
                                        (it.notes ?: "").contains(searchQuery, ignoreCase = true)
                            },
                            pinnedFinanceLogIds = pinnedFinanceLogIds,
                            onSelectLog = { selectedDetailFinanceLog = it },
                            onTogglePinLog = { id -> viewModel.togglePinFinanceLog(id) },
                            onEditLog = { editingFinanceLog = it },
                            onDeleteLog = { deletingFinanceLog = it }
                        )
                        "reports" -> MoneyTrackerReportsView(
                            financeLogs = financeLogs,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            totalBalance = totalBalance
                        )
                        "calendar" -> MoneyTrackerCalendarView(
                            monthLogs = monthLogs,
                            selectedMonthYear = selectedMonthYear,
                            onSelectDate = { dateStr ->
                                selectedDateDetail = dateStr
                            }
                        )
                        "net_worth" -> NetWorthManagerView(
                            viewModel = viewModel,
                            onAddNetWorthClick = {
                                onPresetTabChange("net_worth")
                                viewModel.openAddFinanceSheet()
                            }
                        )
                    }
                }
            }
        }

        if (showMonthYearPicker) {
            MonthYearPickerDialog(
                currentCalendar = selectedMonthYear,
                onDismiss = { showMonthYearPicker = false },
                onMonthYearSelected = {
                    selectedMonthYear = it
                }
            )
        }

        // Add Transaction Sheet (Triggered by FAB Options)
        if (isShowAddSheet) {
            if (presetTabForAddSheet == "net_worth") {
                NetWorthAddSheet(
                    onDismiss = { viewModel.closeAddFinanceSheet() },
                    onSave = { name, type, amount ->
                        viewModel.addNetWorthItem(name, type, amount)
                        viewModel.closeAddFinanceSheet()
                    }
                )
            } else {
                FinanceAddSheet(
                    initialTab = presetTabForAddSheet,
                    onDismiss = { viewModel.closeAddFinanceSheet() },
                    onOpenSettings = {
                        viewModel.closeAddFinanceSheet()
                        showCategorySettings = true
                    },
                    onOpenRecurring = {
                        showRecurringTransactionsScreen = true
                    },
                    onSaveLog = { type, category, amount, notes, date ->
                        viewModel.addFinanceLog(
                            type = type,
                            category = category,
                            title = category,
                            amount = amount,
                            notes = notes,
                            date = date
                        )
                        viewModel.closeAddFinanceSheet()
                    },
                    onSaveNetWorth = { name, type, amount ->
                        viewModel.addNetWorthItem(name, type, amount)
                        viewModel.closeAddFinanceSheet()
                    }
                )
            }
        }

        // Transaction Detail Dialog
        if (selectedDetailFinanceLog != null) {
            TransactionDetailDialog(
                log = selectedDetailFinanceLog!!,
                onDismiss = { selectedDetailFinanceLog = null },
                onEdit = {
                    editingFinanceLog = selectedDetailFinanceLog
                    selectedDetailFinanceLog = null
                },
                onDelete = {
                    deletingFinanceLog = selectedDetailFinanceLog
                    selectedDetailFinanceLog = null
                }
            )
        }

        // Recurring Transactions Fullscreen / Dialog
        if (showRecurringTransactionsScreen) {
            RecurringTransactionsScreen(
                recurringList = recurringTransactionsList,
                onBack = { showRecurringTransactionsScreen = false },
                onAddNew = { showAddRecurringTransactionSheet = true },
                onDelete = { id ->
                    recurringTransactionsList = recurringTransactionsList.filter { it.id != id }
                }
            )
        }

        // Add Recurring Transaction Form Sheet
        if (showAddRecurringTransactionSheet) {
            AddRecurringTransactionSheet(
                onDismiss = { showAddRecurringTransactionSheet = false },
                onSave = { item ->
                    recurringTransactionsList = recurringTransactionsList + item
                    viewModel.addFinanceLog(
                        type = item.type.lowercase(),
                        category = item.category,
                        title = item.name,
                        amount = item.amount,
                        notes = "Recurring (${item.frequency}): ${item.note ?: ""}".trim(),
                        date = item.startDate
                    )
                    showAddRecurringTransactionSheet = false
                }
            )
        }

        // Category Settings Sheet (Triggered by + Settings icon)
        if (showCategorySettings) {
            FinanceCategorySettingsSheet(
                onDismiss = { showCategorySettings = false }
            )
        }

        // Edit Dialog
        if (editingFinanceLog != null) {
            val log = editingFinanceLog!!
            var editAmount by remember(log) { mutableStateOf(log.amount.toString()) }
            var editNotes by remember(log) { mutableStateOf(log.notes ?: "") }
            var editCategory by remember(log) { mutableStateOf(log.category) }

            AlertDialog(
                onDismissRequest = { editingFinanceLog = null },
                title = { Text("Edit Entry", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = editCategory,
                            onValueChange = { editCategory = it },
                            label = { Text("Category") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editAmount,
                            onValueChange = { editAmount = it },
                            label = { Text("Amount") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editNotes,
                            onValueChange = { editNotes = it },
                            label = { Text("Note") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val newAmt = editAmount.toDoubleOrNull() ?: log.amount
                        viewModel.updateFinanceLog(
                            log.copy(
                                category = editCategory,
                                title = editCategory,
                                amount = newAmt,
                                notes = editNotes.ifBlank { null }
                            )
                        )
                        editingFinanceLog = null
                    }) {
                        Text("Save", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingFinanceLog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete Dialog
        if (deletingFinanceLog != null) {
            val log = deletingFinanceLog!!
            AlertDialog(
                onDismissRequest = { deletingFinanceLog = null },
                title = { Text("Delete Entry", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete ${log.category} (₹${log.amount})?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteFinanceLog(log.id)
                        deletingFinanceLog = null
                    }) {
                        Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingFinanceLog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SpeedDialOptionItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 4.dp
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(badgeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// MONEY TRACKER HEADER
// -----------------------------------------------------------------------------
@Composable
fun MoneyTrackerHeader(
    currentMonthDisplay: String,
    totalExpense: Double,
    totalIncome: Double,
    totalBalance: Double,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            // Search Bar
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search category, title, note...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    }
                )
            }

            // Summary Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Month Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevMonth, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clickable(onClick = onMonthClick)
                    ) {
                        Text(currentMonthDisplay, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select Month",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onNextMonth, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Expense Total
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Expenses", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "₹${String.format("%,.0f", totalExpense)}",
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Income Total
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Income", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "₹${String.format("%,.0f", totalIncome)}",
                        color = Color(0xFF10B981),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Balance
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text("Balance", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "₹${String.format("%,.0f", totalBalance)}",
                        color = if (totalBalance >= 0) MaterialTheme.colorScheme.onSurface else Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// MONTH YEAR PICKER DIALOG
// -----------------------------------------------------------------------------
@Composable
fun MonthYearPickerDialog(
    currentCalendar: Calendar,
    onDismiss: () -> Unit,
    onMonthYearSelected: (Calendar) -> Unit
) {
    var selectedYear by remember { mutableStateOf(currentCalendar.get(Calendar.YEAR)) }
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val currentMonthIdx = currentCalendar.get(Calendar.MONTH)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Month & Year", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Year")
                    }
                    Text(
                        text = selectedYear.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Year")
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val chunkedMonths = months.chunked(3)
                chunkedMonths.forEachIndexed { rowIndex, rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMonths.forEachIndexed { colIndex, monthLabel ->
                            val monthIdx = rowIndex * 3 + colIndex
                            val isSelected = selectedYear == currentCalendar.get(Calendar.YEAR) && monthIdx == currentMonthIdx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) Color(0xFFF59E0B)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        val newCal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, selectedYear)
                                            set(Calendar.MONTH, monthIdx)
                                            set(Calendar.DAY_OF_MONTH, 1)
                                        }
                                        onMonthYearSelected(newCal)
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = monthLabel,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// -----------------------------------------------------------------------------
// MONEY TRACKER HOME VIEW
// -----------------------------------------------------------------------------
@Composable
fun MoneyTrackerHomeView(
    monthLogs: List<FinanceLogEntity>,
    pinnedFinanceLogIds: List<String> = emptyList(),
    onSelectLog: (FinanceLogEntity) -> Unit,
    onTogglePinLog: (String) -> Unit = {},
    onEditLog: (FinanceLogEntity) -> Unit = {},
    onDeleteLog: (FinanceLogEntity) -> Unit = {}
) {
    var longPressLog by remember { mutableStateOf<FinanceLogEntity?>(null) }

    if (longPressLog != null) {
        val log = longPressLog!!
        val isPinned = pinnedFinanceLogIds.contains(log.id)
        AlertDialog(
            onDismissRequest = { longPressLog = null },
            title = { Text(log.category, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            longPressLog = null
                            onEditLog(log)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Edit Transaction", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            onTogglePinLog(log.id)
                            longPressLog = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isPinned) "Unpin from Top" else "Pin to Top", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            longPressLog = null
                            onDeleteLog(log)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Transaction", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { longPressLog = null }) { Text("Cancel") }
            }
        )
    }

    if (monthLogs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No transactions recorded for this month.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    } else {
        val groupedLogs = remember(monthLogs) {
            monthLogs.groupBy { it.date }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val list = groupedLogs.toList()
            itemsIndexed(list) { index, (dateStr, logs) ->
                val sortedLogs = remember(logs, pinnedFinanceLogIds) {
                    logs.sortedWith(
                        compareBy<FinanceLogEntity> { !pinnedFinanceLogIds.contains(it.id) }
                            .thenBy { if (pinnedFinanceLogIds.contains(it.id)) pinnedFinanceLogIds.indexOf(it.id) else Int.MAX_VALUE }
                    )
                }
                StaggeredItem(index = index) {
                    val dateExpense = logs.filter { it.type == "expense" }.sumOf { it.amount }
                    val dateIncome = logs.filter { it.type == "income" }.sumOf { it.amount }

                    Column {
                        // Date header row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateStr,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (dateExpense > 0) {
                                    Text(
                                        "Exp: -₹${String.format("%,.0f", dateExpense)}",
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp
                                    )
                                }
                                if (dateIncome > 0) {
                                    Text(
                                        "Inc: +₹${String.format("%,.0f", dateIncome)}",
                                        color = Color(0xFF10B981),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // List of items
                        sortedLogs.forEach { log ->
                            FinanceItemRow(
                                log = log,
                                isPinned = pinnedFinanceLogIds.contains(log.id),
                                onClick = { onSelectLog(log) },
                                onLongClick = { longPressLog = log }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FinanceItemRow(
    log: FinanceLogEntity,
    isPinned: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = if (isPinned) BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFFF59E0B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        FinanceCategoryDefaults.getCategoryIcon(log.category),
                        contentDescription = log.category,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            log.category,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    if (!log.notes.isNullOrBlank()) {
                        Text(
                            log.notes,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val isExpense = log.type == "expense"
                val sign = if (isExpense) "-" else "+"
                val amountColor = if (isExpense) Color(0xFFEF4444) else Color(0xFF10B981)

                Text(
                    "$sign₹${String.format("%,.0f", log.amount)}",
                    color = amountColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TRANSACTION DETAIL DIALOG
// -----------------------------------------------------------------------------
@Composable
fun TransactionDetailDialog(
    log: FinanceLogEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFF59E0B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        FinanceCategoryDefaults.getCategoryIcon(log.category),
                        contentDescription = log.category,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        log.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        log.type.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (log.type == "expense") Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Amount", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    val isExpense = log.type == "expense"
                    val sign = if (isExpense) "-" else "+"
                    val amountColor = if (isExpense) Color(0xFFEF4444) else Color(0xFF10B981)
                    Text(
                        "$sign₹${String.format("%,.0f", log.amount)}",
                        color = amountColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                // Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Date", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text(log.date, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }

                // Note
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Note", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text(
                        log.notes ?: "None",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

                if (!log.spendSource.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Source", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Text(
                            log.spendSource,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }

                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}



// -----------------------------------------------------------------------------
// MONEY TRACKER REPORTS VIEW
// -----------------------------------------------------------------------------
@Composable
fun MoneyTrackerReportsView(
    financeLogs: List<FinanceLogEntity>,
    totalIncome: Double,
    totalExpense: Double,
    totalBalance: Double
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Monthly Summary Report", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReportRow("Net Income", "₹${String.format("%,.0f", totalIncome)}", Color(0xFF10B981))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ReportRow("Total Expenses", "₹${String.format("%,.0f", totalExpense)}", Color(0xFFEF4444))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ReportRow("Net Balance", "₹${String.format("%,.0f", totalBalance)}", Color(0xFFF59E0B))
                }
            }
        }

        item {
            Text("Overall Statistics", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReportRow("Total Recorded Logs", "${financeLogs.size}", MaterialTheme.colorScheme.onSurface)
                    ReportRow(
                        "Avg Expense / Transaction",
                        "₹${String.format("%,.0f", if (financeLogs.none { it.type == "expense" }) 0.0 else financeLogs.filter { it.type == "expense" }.map { it.amount }.average())}",
                        MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ReportRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

// -----------------------------------------------------------------------------
// MONEY TRACKER CALENDAR VIEW (Screenshot 1 & 2)
// -----------------------------------------------------------------------------
@Composable
fun MoneyTrackerCalendarView(
    monthLogs: List<FinanceLogEntity>,
    selectedMonthYear: Calendar,
    onSelectDate: (String) -> Unit
) {
    // Map of Date -> Pair(Total Income, Total Expense)
    val dailySummaries = remember(monthLogs) {
        monthLogs.groupBy { it.date }.mapValues { entry ->
            val income = entry.value.filter { it.type == "income" }.sumOf { it.amount }
            val expense = entry.value.filter { it.type == "expense" }.sumOf { it.amount }
            Pair(income, expense)
        }
    }

    val daysInMonth = remember(selectedMonthYear) {
        val cal = selectedMonthYear.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday
        Pair(maxDays, startDayOfWeek)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Days of Week Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(
                    day,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid of date boxes
        val totalGridCells = 42 // 6 rows of 7
        val (maxDays, startDayOfWeek) = daysInMonth

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(totalGridCells) { index ->
                val dayNumber = index - (startDayOfWeek - 1) + 1
                if (dayNumber in 1..maxDays) {
                    val cal = selectedMonthYear.clone() as Calendar
                    cal.set(Calendar.DAY_OF_MONTH, dayNumber)
                    val dateStr = TrackWiseUtils.formatDate(cal.time, "yyyy-MM-dd")
                    val daySummary = dailySummaries[dateStr]

                    Box(
                        modifier = Modifier
                            .aspectRatio(0.85f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .clickable { onSelectDate(dateStr) }
                            .padding(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "$dayNumber",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (daySummary != null) {
                                Column {
                                    if (daySummary.first > 0) {
                                        Text(
                                            String.format("%.0f", daySummary.first),
                                            color = Color(0xFF10B981),
                                            fontSize = 8.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (daySummary.second > 0) {
                                        Text(
                                            String.format("%.0f", daySummary.second),
                                            color = Color(0xFFEF4444),
                                            fontSize = 8.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.aspectRatio(0.85f))
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// DATE DETAIL VIEW (Screenshot 2)
// -----------------------------------------------------------------------------
@Composable
fun DateDetailView(
    dateStr: String,
    allLogs: List<FinanceLogEntity>,
    onBack: () -> Unit,
    onSelectLog: (FinanceLogEntity) -> Unit,
    onAddClick: () -> Unit
) {
    val dayLogs = remember(allLogs, dateStr) {
        allLogs.filter { it.date == dateStr }
    }

    val dayExpense = remember(dayLogs) {
        dayLogs.filter { it.type == "expense" }.sumOf { it.amount }
    }
    val dayIncome = remember(dayLogs) {
        dayLogs.filter { it.type == "income" }.sumOf { it.amount }
    }

    val formattedHeaderDate = remember(dateStr) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = sdf.parse(dateStr)
            val sdfOut = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            sdfOut.format(d ?: Date())
        } catch (e: Exception) {
            dateStr
        }
    }

    val formattedSubhead = remember(dateStr) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = sdf.parse(dateStr)
            val sdfOut = SimpleDateFormat("MMM d EEEE", Locale.getDefault())
            sdfOut.format(d ?: Date())
        } catch (e: Exception) {
            dateStr
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(formattedHeaderDate, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // Subheader bar with date summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(formattedSubhead, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Expenses: ₹${String.format("%.0f", dayExpense)}",
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Income: ₹${String.format("%.0f", dayIncome)}",
                    color = Color(0xFF10B981),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (dayLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions logged for this date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(dayLogs) { log ->
                    FinanceItemRow(
                        log = log,
                        onClick = { onSelectLog(log) }
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// ADD NET WORTH SHEET
// -----------------------------------------------------------------------------
@Composable
fun NetWorthAddSheet(
    initialType: String = "asset",
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, amount: Double) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var type by remember(initialType) { mutableStateOf(initialType) } // "asset", "liability", "loan"
    var amountStr by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val categoryOptions = when (type) {
        "asset" -> listOf(
            "Emergency Fund", "Fixed Deposit", "FD", "Retirement", "Mutual Funds",
            "Gold", "Silver", "EPF", "NPS", "Stocks", "PPF",
            "Savings bank account", "Savings Account", "Property", "Real Estate",
            "RD", "LIC", "Piggy Bank", "Crypto", "Others"
        )
        "liability" -> listOf(
            "Credit Card", "Personal Debt", "Overdraft", "Tax Due",
            "Outstanding Bill", "Other Liability", "Others"
        )
        else -> listOf(
            "Home Loan", "Personal Loan", "Car/Auto Loan", "Education Loan",
            "Business Loan", "Gold Loan", "Other Loan", "Others"
        )
    }

    var selectedCategory by remember(type) { mutableStateOf(categoryOptions.first()) }
    var customName by remember { mutableStateOf("") }

    val finalName = if (selectedCategory == "Others") customName else selectedCategory

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                },
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header: Cancel | Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Cancel",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        modifier = Modifier.clickable(onClick = onDismiss)
                    )
                    Text(
                        "Add Asset or Liability",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(48.dp)) // balance layout
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Selector for Asset, Liability, Loan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("asset" to "Asset", "liability" to "Liability", "loan" to "Loan").forEach { (typeKey, label) ->
                        val isSelected = type == typeKey
                        val color = if (isSelected) {
                            when (typeKey) {
                                "asset" -> Color(0xFF10B981)
                                "liability" -> Color(0xFFEF4444)
                                else -> Color(0xFFF59E0B)
                            }
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                        val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(color, RoundedCornerShape(22.dp))
                                .clickable { type = typeKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = when (type) {
                            "asset" -> "Asset Category"
                            "liability" -> "Liability Category"
                            else -> "Loan Category"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface
                            ),
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Category")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { dropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier
                                .widthIn(max = 260.dp)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            categoryOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        selectedCategory = option
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedCategory == "Others") {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Custom Name") },
                            placeholder = { Text("Enter custom category or item name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }

                // Amount Input
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                // Save Button
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        if (finalName.isNotBlank() && amt > 0) {
                            onSave(finalName, type, amt)
                        } else {
                            Toast.makeText(context, "Please enter a valid name and amount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Save Asset / Liability", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// -----------------------------------------------------------------------------
// ADD TRANSACTION SHEET (Screenshots 1, 2, 3, 4)
// -----------------------------------------------------------------------------
@Composable
fun FinanceAddSheet(
    initialTab: String = "expense",
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRecurring: () -> Unit = {},
    onSaveLog: (type: String, category: String, amount: Double, notes: String?, date: String) -> Unit,
    onSaveNetWorth: (name: String, type: String, amount: Double) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var activeTab by remember { mutableStateOf(initialTab) } // "expense", "income", "savings", "net_worth"
    var selectedCategoryItem by remember { mutableStateOf<FinanceCategoryItem?>(null) }

    // Net Worth Form States
    var netWorthType by remember { mutableStateOf("asset") } // "asset", "liability", "loan"
    var netWorthName by remember { mutableStateOf("") }

    // Keypad & Math State
    var mathExpression by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }
    var dateButtonLabel by remember { mutableStateOf("Today") }

    val currentCategories = when (activeTab) {
        "expense" -> FinanceCategoryDefaults.expenseCategories
        "income" -> FinanceCategoryDefaults.incomeCategories
        else -> FinanceCategoryDefaults.savingsCategories
    }

    // DatePicker Dialog
    val calendar = remember { Calendar.getInstance() }
    val datePickerDialog = remember(context) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                selectedDate = TrackWiseUtils.formatDate(cal.time, "yyyy-MM-dd")
                dateButtonLabel = if (selectedDate == TrackWiseUtils.getTodayString()) {
                    "Today"
                } else {
                    TrackWiseUtils.formatDate(cal.time, "MMM dd")
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                },
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Top Header: Cancel | Add | Recurring Icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Cancel",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        modifier = Modifier.clickable(onClick = onDismiss)
                    )
                    Text("Add", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onOpenRecurring) {
                        Icon(Icons.Default.Repeat, contentDescription = "Recurring Transactions", tint = Color(0xFFF59E0B))
                    }
                }

                // Tabs: Expense | Income | Savings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tabs = listOf(
                        "expense" to "Expense",
                        "income" to "Income",
                        "savings" to "Savings"
                    )
                    tabs.forEach { (tabKey, label) ->
                        val isSelected = activeTab == tabKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFFF59E0B) else Color.Transparent)
                                .clickable {
                                    activeTab = tabKey
                                    selectedCategoryItem = null
                                    mathExpression = ""
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Categories Grid (4 Columns)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentCategories) { item ->
                        val isSelected = selectedCategoryItem?.id == item.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                selectedCategoryItem = item
                                mathExpression = ""
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(
                                        if (isSelected) Color(0xFFF59E0B) else MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    FinanceCategoryDefaults.getCategoryIcon(item.iconKey),
                                    contentDescription = item.name,
                                    tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                item.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Last Item: + Settings
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable(onClick = onOpenSettings)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Settings",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Calculator Keypad Overlay (Screenshots 3 & 4) when Category is selected
                if (selectedCategoryItem != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {

                            // Expression Display Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (mathExpression.isEmpty()) "0" else mathExpression,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Note Input Line with Send/Enter button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (noteText.isEmpty()) {
                                        Text("Note : Enter a note...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                    }
                                    BasicTextField(
                                        value = noteText,
                                        onValueChange = { noteText = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val calculatedVal = evaluateMathExpression(mathExpression)
                                        if (calculatedVal > 0) {
                                            onSaveLog(
                                                activeTab,
                                                selectedCategoryItem!!.name,
                                                calculatedVal,
                                                noteText.ifBlank { null },
                                                selectedDate
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Save Transaction",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            // Keypad Buttons Grid (4x5 perfectly aligned)
                            val keypadRows = listOf(
                                listOf("7", "8", "9", "+"),
                                listOf("4", "5", "6", "-"),
                                listOf("1", "2", "3", "×"),
                                listOf(".", "0", "⌫", "÷"),
                                listOf("DATE", "=")
                            )

                            keypadRows.forEach { row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    row.forEach { btn ->
                                        val weight = if (row.size == 2) 2f else 1f
                                        Surface(
                                            color = when (btn) {
                                                "=" -> Color(0xFFF59E0B)
                                                "DATE" -> MaterialTheme.colorScheme.surface
                                                "+", "-", "×", "÷" -> MaterialTheme.colorScheme.surface
                                                else -> MaterialTheme.colorScheme.surface
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .weight(weight)
                                                .height(48.dp)
                                                .clickable {
                                                    when (btn) {
                                                        "DATE" -> {
                                                            try {
                                                                val parts = selectedDate.split("-")
                                                                if (parts.size == 3) {
                                                                    datePickerDialog.updateDate(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                                                                }
                                                            } catch (e: Exception) {}
                                                            datePickerDialog.show()
                                                        }
                                                        "⌫" -> if (mathExpression.isNotEmpty()) mathExpression = mathExpression.dropLast(1)
                                                        "=" -> {
                                                            val calculatedVal = evaluateMathExpression(mathExpression)
                                                            mathExpression = if (calculatedVal > 0) {
                                                                java.lang.Math.round(calculatedVal).toString()
                                                            } else {
                                                                ""
                                                            }
                                                        }
                                                        else -> mathExpression += btn
                                                    }
                                                },
                                            contentColor = if (btn == "=") Color.Black else MaterialTheme.colorScheme.onSurface
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (btn == "DATE") {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                                        Text(dateButtonLabel, fontSize = 12.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                                    }
                                                } else {
                                                    Text(
                                                        btn,
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// CATEGORY SETTINGS SHEET (With Drag / Up-Down Reordering)
// -----------------------------------------------------------------------------
@Composable
fun FinanceCategorySettingsSheet(onDismiss: () -> Unit) {
    var activeTab by remember { mutableStateOf("expense") } // "expense", "income", "savings"

    val currentCategories = when (activeTab) {
        "expense" -> FinanceCategoryDefaults.expenseCategories
        "income" -> FinanceCategoryDefaults.incomeCategories
        else -> FinanceCategoryDefaults.savingsCategories
    }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Category Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.SwapVert, contentDescription = "Reorder")
            }

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("expense" to "Expense", "income" to "Income", "savings" to "Savings").forEach { (tabKey, label) ->
                    val isSelected = activeTab == tabKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color(0xFFF59E0B) else Color.Transparent)
                            .clickable { activeTab = tabKey }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Items List with Up/Down Reorder Actions
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                itemsIndexed(currentCategories, key = { _, cat -> cat.id }) { index, cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFF59E0B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    FinanceCategoryDefaults.getCategoryIcon(cat.iconKey),
                                    contentDescription = cat.name,
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(cat.name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Up Arrow
                            IconButton(
                                onClick = { FinanceCategoryDefaults.moveCategoryUp(currentCategories, index) },
                                enabled = index > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = "Move Up",
                                    tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Down Arrow
                            IconButton(
                                onClick = { FinanceCategoryDefaults.moveCategoryDown(currentCategories, index) },
                                enabled = index < currentCategories.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = "Move Down",
                                    tint = if (index < currentCategories.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { currentCategories.remove(cat) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                }
            }

            // Bottom Add Category Button
            Button(
                onClick = { showAddCategoryDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add category", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // Add Category Dialog
        if (showAddCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = { Text("Add New Category", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newCategoryName.isNotBlank()) {
                            val newCat = FinanceCategoryItem(
                                id = "custom_${System.currentTimeMillis()}",
                                name = newCategoryName,
                                type = activeTab,
                                iconKey = "others"
                            )
                            currentCategories.add(newCat)
                            newCategoryName = ""
                            showAddCategoryDialog = false
                        }
                    }) {
                        Text("Add", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCategoryDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// Simple Expression Evaluator
fun evaluateMathExpression(expr: String): Double {
    val clean = expr.replace(",", "").replace(" ", "").replace("×", "*").replace("÷", "/")
    if (clean.isBlank()) return 0.0
    return try {
        val addParts = clean.split("+")
        var sum = 0.0
        for (addPart in addParts) {
            val subParts = addPart.split("-")
            var subVal = evalMulDiv(subParts[0])
            for (i in 1 until subParts.size) {
                subVal -= evalMulDiv(subParts[i])
            }
            sum += subVal
        }
        sum
    } catch (e: Exception) {
        clean.toDoubleOrNull() ?: 0.0
    }
}

private fun evalMulDiv(expr: String): Double {
    if (expr.isBlank()) return 0.0
    val mulParts = expr.split("*")
    var product = 1.0
    for (mulPart in mulParts) {
        val divParts = mulPart.split("/")
        var divVal = divParts[0].toDoubleOrNull() ?: 0.0
        for (i in 1 until divParts.size) {
            val divisor = divParts[i].toDoubleOrNull() ?: 1.0
            if (divisor != 0.0) divVal /= divisor
        }
        product *= divVal
    }
    return product
}

// -----------------------------------------------------------------------------
// RECURRING TRANSACTIONS SCREENS & SHEETS
// -----------------------------------------------------------------------------
@Composable
fun RecurringTransactionsScreen(
    recurringList: List<RecurringTransaction>,
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    onDelete: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Recurring Transactions",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Body
            if (recurringList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No records",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recurringList) { item ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(Color(0xFFF59E0B), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            FinanceCategoryDefaults.getCategoryIcon(item.category),
                                            contentDescription = item.category,
                                            tint = Color.Black,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            item.name.ifBlank { item.category },
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            "${item.type} • ${item.frequency} • Starts ${item.startDate}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isExpense = item.type.equals("expense", ignoreCase = true)
                                    val sign = if (isExpense) "-" else "+"
                                    val amountColor = if (isExpense) Color(0xFFEF4444) else Color(0xFF10B981)

                                    Text(
                                        "$sign₹${String.format("%,.0f", item.amount)}",
                                        color = amountColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )

                                    IconButton(
                                        onClick = { onDelete(item.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Add Button
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onAddNew,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddRecurringTransactionSheet(
    onDismiss: () -> Unit,
    onSave: (RecurringTransaction) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Expense") }
    var selectedCategory by remember { mutableStateOf("Shopping") }
    var selectedFrequency by remember { mutableStateOf("Monthly") }
    var startDate by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }
    var amountText by remember { mutableStateOf("") }
    var numberOfTransactions by remember { mutableStateOf("Unlimited") }
    var note by remember { mutableStateOf("") }

    var showTypePicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showFrequencyPicker by remember { mutableStateOf(false) }
    var showNumPicker by remember { mutableStateOf(false) }

    val calendar = remember { Calendar.getInstance() }
    val datePickerDialog = remember(context) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                startDate = TrackWiseUtils.formatDate(cal.time, "yyyy-MM-dd")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                },
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Top Header: Cancel | Add | Checkmark
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Cancel",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        modifier = Modifier.clickable(onClick = onDismiss)
                    )
                    Text("Add", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        val item = RecurringTransaction(
                            name = name.ifBlank { selectedCategory },
                            type = selectedType,
                            category = selectedCategory,
                            frequency = selectedFrequency,
                            startDate = startDate,
                            amount = amt,
                            numberOfTransactions = numberOfTransactions,
                            note = note.ifBlank { null }
                        )
                        onSave(item)
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color(0xFFF59E0B))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Transaction name
                    FormLabelWithYellowBar("Transaction name")
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Please enter the transaction name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 2. Type
                    FormLabelWithYellowBar("Type")
                    FormSelectorBox(
                        value = selectedType,
                        onClick = { showTypePicker = true }
                    )

                    // 3. Category
                    FormLabelWithYellowBar("Category")
                    FormSelectorBox(
                        value = selectedCategory,
                        icon = FinanceCategoryDefaults.getCategoryIcon(selectedCategory),
                        onClick = { showCategoryPicker = true }
                    )

                    // 4. Transaction frequency
                    FormLabelWithYellowBar("Transaction frequency")
                    FormSelectorBox(
                        value = selectedFrequency,
                        onClick = { showFrequencyPicker = true }
                    )

                    // 5. Transaction start date
                    FormLabelWithYellowBar("Transaction start date")
                    FormSelectorBox(
                        value = startDate,
                        onClick = { datePickerDialog.show() }
                    )
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            "Transaction date: $startDate...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Data is added automatically only on the transaction date you set. If you have allowed notification permission, you will receive reminder notifications as well.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }

                    // 6. Amount ( Every time )
                    FormLabelWithYellowBar("Amount ( Every time )")
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        placeholder = { Text("0.00") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // 7. Number of transactions
                    FormLabelWithYellowBar("Number of transactions")
                    FormSelectorBox(
                        value = numberOfTransactions,
                        onClick = { showNumPicker = true }
                    )

                    // 8. Note
                    FormLabelWithYellowBar("Note")
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

        // Pickers
        if (showTypePicker) {
            OptionSelectionBottomSheet(
                title = "Select Type",
                options = listOf("Income", "Expense", "Savings"),
                selectedOption = selectedType,
                onDismiss = { showTypePicker = false },
                onSelect = { type ->
                    selectedType = type
                    selectedCategory = when (type.lowercase()) {
                        "income" -> "Salary"
                        "savings" -> "Emergency Fund"
                        else -> "Shopping"
                    }
                    showTypePicker = false
                }
            )
        }

        if (showCategoryPicker) {
            CategoryGridSelectionDialog(
                selectedType = selectedType,
                selectedCategory = selectedCategory,
                onDismiss = { showCategoryPicker = false },
                onSelect = { selectedCategory = it; showCategoryPicker = false }
            )
        }

        if (showFrequencyPicker) {
            OptionSelectionBottomSheet(
                title = "Select Frequency",
                options = listOf(
                    "Daily", "Weekly", "Every two weeks", "Monthly",
                    "Every two months", "Every three months",
                    "Every four months", "Every six months", "Every year"
                ),
                selectedOption = selectedFrequency,
                onDismiss = { showFrequencyPicker = false },
                onSelect = { selectedFrequency = it; showFrequencyPicker = false }
            )
        }

        if (showNumPicker) {
            OptionSelectionBottomSheet(
                title = "Number of Transactions",
                options = listOf("Unlimited", "3", "6", "12", "18", "24"),
                selectedOption = numberOfTransactions,
                onDismiss = { showNumPicker = false },
                onSelect = { numberOfTransactions = it; showNumPicker = false }
            )
        }
    }
}
}

@Composable
fun FormLabelWithYellowBar(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(14.dp)
                .background(Color(0xFFF59E0B), RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FormSelectorBox(
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = value,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = value,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OptionSelectionBottomSheet(
    title: String,
    options: List<String>,
    selectedOption: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEach { opt ->
                    val isSel = opt == selectedOption
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(opt) }
                            .background(
                                if (isSel) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                    ) {
                        Text(
                            text = opt,
                            color = if (isSel) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CategoryGridSelectionDialog(
    selectedType: String,
    selectedCategory: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val categories = when (selectedType.lowercase()) {
        "income" -> FinanceCategoryDefaults.incomeCategories.map { it.name }
        "savings" -> FinanceCategoryDefaults.savingsCategories.map { it.name }
        else -> FinanceCategoryDefaults.expenseCategories.map { it.name }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { cat ->
                    val isSel = cat == selectedCategory
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onSelect(cat) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    if (isSel) Color(0xFFF59E0B) else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                FinanceCategoryDefaults.getCategoryIcon(cat),
                                contentDescription = cat,
                                tint = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            cat,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// -----------------------------------------------------------------------------
// NET WORTH MANAGER VIEW
// -----------------------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NetWorthManagerView(
    viewModel: TrackWiseViewModel,
    onAddNetWorthClick: () -> Unit
) {
    val netWorthItems by viewModel.allNetWorthItems.collectAsState()
    val allFinanceLogs by viewModel.allFinanceLogs.collectAsState()
    val pinnedNetWorthIds by viewModel.pinnedNetWorthIds.collectAsState()

    var editingNetWorthItem by remember { mutableStateOf<NetWorthItemEntity?>(null) }
    var deletingNetWorthItem by remember { mutableStateOf<NetWorthItemEntity?>(null) }
    
    val totalSavingsFromLogs = remember(allFinanceLogs) { allFinanceLogs.filter { it.type == "savings" }.sumOf { it.amount } }
    val totalAssets = remember(netWorthItems, totalSavingsFromLogs) { netWorthItems.filter { it.type == "asset" }.sumOf { it.amount } + totalSavingsFromLogs }
    val totalLoans = remember(netWorthItems) { netWorthItems.filter { it.type == "loan" }.sumOf { it.amount } }
    val totalLiabilities = remember(netWorthItems) { netWorthItems.filter { it.type == "liability" }.sumOf { it.amount } }
    val totalDebt = totalLoans + totalLiabilities
    val netWorth = totalAssets - totalDebt

    // Edit Item Dialog
    if (editingNetWorthItem != null) {
        val item = editingNetWorthItem!!
        var editName by remember { mutableStateOf(item.name) }
        var editType by remember { mutableStateOf(item.type) }
        var editAmountStr by remember { mutableStateOf(item.amount.toString()) }

        AlertDialog(
            onDismissRequest = { editingNetWorthItem = null },
            title = { Text("Edit Net Worth Item", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("asset" to "Asset", "liability" to "Liability", "loan" to "Loan").forEach { (typeKey, label) ->
                            val isSel = editType == typeKey
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { editType = typeKey }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = editAmountStr,
                        onValueChange = { editAmountStr = it },
                        label = { Text("Amount (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = editAmountStr.toDoubleOrNull() ?: item.amount
                        if (editName.isNotBlank() && amt >= 0) {
                            viewModel.updateNetWorthItem(item.copy(name = editName, type = editType, amount = amt))
                            editingNetWorthItem = null
                        }
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNetWorthItem = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Confirmation Dialog
    if (deletingNetWorthItem != null) {
        val item = deletingNetWorthItem!!
        AlertDialog(
            onDismissRequest = { deletingNetWorthItem = null },
            title = { Text("Delete Net Worth Item", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${item.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNetWorthItem(item.id)
                        deletingNetWorthItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingNetWorthItem = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Net Worth Card
        item {
            StaggeredItem(index = 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "YOUR ESTIMATED NET WORTH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${String.format("%,.0f", netWorth)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (netWorth >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Assets", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${String.format("%,.0f", totalAssets)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                            Divider(
                                modifier = Modifier
                                    .height(32.dp)
                                    .width(1.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Liabilities", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${String.format("%,.0f", totalDebt)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }
        }

        if (netWorthItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No assets or liabilities found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Text(
                            "Tap the button above to add assets, loans, or liabilities.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        } else {
            // Assets List
            val assetsList = netWorthItems.filter { it.type == "asset" }.sortedWith(
                compareBy<NetWorthItemEntity> { !pinnedNetWorthIds.contains(it.id) }
                    .thenBy { if (pinnedNetWorthIds.contains(it.id)) pinnedNetWorthIds.indexOf(it.id) else Int.MAX_VALUE }
            )
            if (assetsList.isNotEmpty()) {
                item {
                    Text(
                        "ASSETS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(vertical = 4.dp),
                        letterSpacing = 1.sp
                    )
                }
                itemsIndexed(assetsList) { idx, item ->
                    StaggeredItem(index = 2 + idx) {
                        NetWorthItemRow(
                            item = item,
                            isPinned = pinnedNetWorthIds.contains(item.id),
                            onTogglePin = { viewModel.togglePinNetWorthItem(item.id) },
                            onEdit = { editingNetWorthItem = item },
                            onDelete = { deletingNetWorthItem = item }
                        )
                    }
                }
            }

            // Liabilities/Loans List
            val debtsList = netWorthItems.filter { it.type != "asset" }.sortedWith(
                compareBy<NetWorthItemEntity> { !pinnedNetWorthIds.contains(it.id) }
                    .thenBy { if (pinnedNetWorthIds.contains(it.id)) pinnedNetWorthIds.indexOf(it.id) else Int.MAX_VALUE }
            )
            if (debtsList.isNotEmpty()) {
                item {
                    Text(
                        "LIABILITIES & LOANS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.padding(vertical = 4.dp),
                        letterSpacing = 1.sp
                    )
                }
                itemsIndexed(debtsList) { idx, item ->
                    StaggeredItem(index = 2 + assetsList.size + idx) {
                        NetWorthItemRow(
                            item = item,
                            isPinned = pinnedNetWorthIds.contains(item.id),
                            onTogglePin = { viewModel.togglePinNetWorthItem(item.id) },
                            onEdit = { editingNetWorthItem = item },
                            onDelete = { deletingNetWorthItem = item }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NetWorthItemRow(
    item: NetWorthItemEntity,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onEdit,
                onLongClick = { showMenu = true }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val isAsset = item.type == "asset"
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isAsset) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAsset) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (isAsset) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isPinned) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = BrandAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                item.name,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            item.type.uppercase(),
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Text(
                    "₹${String.format("%,.0f", item.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (item.type == "asset") Color(0xFF10B981) else Color(0xFFEF4444)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit Details") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isPinned) "Unpin from Top" else "Pin to Top") },
                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, tint = BrandAmber) },
                    onClick = {
                        showMenu = false
                        onTogglePin()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
fun NetWorthScreen(viewModel: TrackWiseViewModel) {
    val showAddSheet by viewModel.showNetWorthAddSheet.collectAsState()
    val presetType by viewModel.netWorthPresetType.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        NetWorthManagerView(
            viewModel = viewModel,
            onAddNetWorthClick = {}
        )

        if (showAddSheet) {
            NetWorthAddSheet(
                initialType = presetType,
                onDismiss = { viewModel.closeNetWorthAddSheet() },
                onSave = { name, type, amount ->
                    viewModel.addNetWorthItem(name, type, amount)
                    viewModel.closeNetWorthAddSheet()
                }
            )
        }
    }
}
