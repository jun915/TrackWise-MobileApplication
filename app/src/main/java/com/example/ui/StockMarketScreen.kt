package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StockTradeEntity
import com.example.ui.theme.*
import com.example.utils.TrackWiseUtils
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockMarketScreen(
    viewModel: TrackWiseViewModel,
    onBack: () -> Unit = {}
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Calculator, 1: Log Trade, 2: Logs
    val decimalFormat = remember { DecimalFormat("#,##0.00") }

    // Hoisted Calculator State to prevent clearing when switching tabs
    var currentPriceStr by rememberSaveable { mutableStateOf("") }
    var amountAvailableStr by rememberSaveable { mutableStateOf("10000") }
    var targetSellingPercentStr by rememberSaveable { mutableStateOf("3.0") }
    var stopLossPercentStr by rememberSaveable { mutableStateOf("1.5") }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = (paddingValues.calculateTopPadding() - 16.dp).coerceAtLeast(0.dp),
                    bottom = paddingValues.calculateBottomPadding(),
                    start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                    end = paddingValues.calculateEndPadding(LayoutDirection.Ltr)
                )
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main Top Tabs matching the Rest of the App's Segmented Style
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Calculator", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = null) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Log Trade", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.AddBusiness, contentDescription = null) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("Logs", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> StockCalculatorTab(
                        decimalFormat = decimalFormat,
                        currentPriceStr = currentPriceStr,
                        onCurrentPriceChange = { currentPriceStr = it },
                        amountAvailableStr = amountAvailableStr,
                        onAmountAvailableChange = { amountAvailableStr = it },
                        targetSellingPercentStr = targetSellingPercentStr,
                        onTargetSellingPercentChange = { targetSellingPercentStr = it },
                        stopLossPercentStr = stopLossPercentStr,
                        onStopLossPercentChange = { stopLossPercentStr = it }
                    )
                    1 -> LogTradeTab(viewModel) { activeTab = 2 }
                    2 -> TradeLogsTab(viewModel, decimalFormat)
                }
            }
        }
    }
}

@Composable
fun StockCalculatorTab(
    decimalFormat: DecimalFormat,
    currentPriceStr: String,
    onCurrentPriceChange: (String) -> Unit,
    amountAvailableStr: String,
    onAmountAvailableChange: (String) -> Unit,
    targetSellingPercentStr: String,
    onTargetSellingPercentChange: (String) -> Unit,
    stopLossPercentStr: String,
    onStopLossPercentChange: (String) -> Unit
) {
    val currentPrice = currentPriceStr.toDoubleOrNull() ?: 0.0
    val amountAvailable = amountAvailableStr.toDoubleOrNull() ?: 0.0
    val targetPercent = targetSellingPercentStr.toDoubleOrNull() ?: 3.0
    val stopLossPercent = stopLossPercentStr.toDoubleOrNull() ?: 1.5

    // Calculation Logic
    val stocksCanBuy = if (currentPrice > 0) floor(amountAvailable / currentPrice).toInt() else 0
    val totalBuyingPrice = stocksCanBuy * currentPrice
    val stopLossValue = currentPrice * (stopLossPercent / 100.0)
    val totalLossRisk = stocksCanBuy * stopLossValue
    val sellingPricePerStock = currentPrice * (1.0 + (targetPercent / 100.0))
    val totalSellingPrice = stocksCanBuy * sellingPricePerStock
    val totalProfitExpected = totalSellingPrice - totalBuyingPrice

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form Fields Container Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Calculator Inputs 📊",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = currentPriceStr,
                    onValueChange = onCurrentPriceChange,
                    label = { Text("Current Stock Price (₹)") },
                    placeholder = { Text("e.g. 150.0") },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountAvailableStr,
                    onValueChange = onAmountAvailableChange,
                    label = { Text("Amount Available (₹)") },
                    placeholder = { Text("e.g. 10000.0") },
                    leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = targetSellingPercentStr,
                        onValueChange = onTargetSellingPercentChange,
                        label = { Text("Target Selling (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = stopLossPercentStr,
                        onValueChange = onStopLossPercentChange,
                        label = { Text("Stop Loss (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }

        // Calculations Results Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Projections & Metrics ⚡",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                // Stocks to Buy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stocks You Can Buy",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "$stocksCanBuy",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))

                // Buying Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Buying Price (Per Stock)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "₹${decimalFormat.format(currentPrice)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Buying Price",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "₹${decimalFormat.format(totalBuyingPrice)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))

                // Selling Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Selling Price (Per Stock)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "₹${decimalFormat.format(sellingPricePerStock)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Expected Profit",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "+₹${decimalFormat.format(totalProfitExpected)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = BrandGreen
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))

                // Risk Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Stop Loss Per Stock",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "₹${decimalFormat.format(currentPrice - stopLossValue)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Max Loss Risk",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "-₹${decimalFormat.format(totalLossRisk)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandRose
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogTradeTab(
    viewModel: TrackWiseViewModel,
    onSaveSuccess: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var stockName by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("") }
    var isProfitMode by remember { mutableStateOf(true) } // true: Profit, false: Loss
    var profitOrLossAmountStr by remember { mutableStateOf("") }
    var taxAmountStr by remember { mutableStateOf("") }
    var tradeDate by remember { mutableStateOf(TrackWiseUtils.getTodayString()) }

    var isDropdownExpanded by remember { mutableStateOf(false) }

    val indianStocks = remember {
        listOf(
            "RELIANCE" to "Reliance Industries Ltd.",
            "TCS" to "Tata Consultancy Services Ltd.",
            "HDFCBANK" to "HDFC Bank Ltd.",
            "INFY" to "Infosys Ltd.",
            "ICICIBANK" to "ICICI Bank Ltd.",
            "HINDUNILVR" to "Hindustan Unilever Ltd.",
            "ITC" to "ITC Ltd.",
            "SBIN" to "State Bank of India",
            "BHARTIARTL" to "Bharti Airtel Ltd.",
            "L&T" to "Larsen & Toubro Ltd.",
            "BAJFINANCE" to "Bajaj Finance Ltd.",
            "KOTAKBANK" to "Kotak Mahindra Bank Ltd.",
            "HCLTECH" to "HCL Technologies Ltd.",
            "AXISBANK" to "Axis Bank Ltd.",
            "ASIANPAINT" to "Asian Paints Ltd.",
            "MARUTI" to "Maruti Suzuki India Ltd.",
            "SUNPHARMA" to "Sun Pharmaceutical Industries Ltd.",
            "TITAN" to "Titan Company Ltd.",
            "ULTRACEMCO" to "UltraTech Cement Ltd.",
            "WIPRO" to "Wipro Ltd.",
            "NTPC" to "NTPC Ltd.",
            "ONGC" to "Oil & Natural Gas Corporation Ltd.",
            "ADANIENT" to "Adani Enterprises Ltd.",
            "ADANIPORTS" to "Adani Ports & SEZ Ltd.",
            "JSWSTEEL" to "JSW Steel Ltd.",
            "POWERGRID" to "Power Grid Corporation of India Ltd.",
            "TATASTEEL" to "Tata Steel Ltd.",
            "TATAMOTORS" to "Tata Motors Ltd.",
            "M&M" to "Mahindra & Mahindra Ltd.",
            "INDUSINDBK" to "IndusInd Bank Ltd.",
            "BAJAJFINSV" to "Bajaj Finserv Ltd.",
            "NESTLEIND" to "Nestle India Ltd.",
            "GRASIM" to "Grasim Industries Ltd.",
            "HINDALCO" to "Hindalco Industries Ltd.",
            "TECHM" to "Tech Mahindra Ltd.",
            "CIPLA" to "Cipla Ltd.",
            "EICHERMOT" to "Eicher Motors Ltd.",
            "BRITANNIA" to "Britannia Industries Ltd.",
            "BPCL" to "Bharat Petroleum Corporation Ltd.",
            "COALINDIA" to "Coal India Ltd.",
            "APOLLOHOSP" to "Apollo Hospitals Enterprise Ltd.",
            "DIVISLAB" to "Divi's Laboratories Ltd.",
            "HEROMOTOCO" to "Hero MotoCorp Ltd.",
            "BAJAJ-AUTO" to "Bajaj Auto Ltd.",
            "DRREDDY" to "Dr. Reddy's Laboratories Ltd.",
            "SBILIFE" to "SBI Life Insurance Company Ltd.",
            "HDFCLIFE" to "HDFC Life Insurance Company Ltd.",
            "TATAPOWER" to "Tata Power Co. Ltd.",
            "IRFC" to "Indian Railway Finance Corp",
            "IREDA" to "Indian Renewable Energy Development Agency",
            "JIOFIN" to "Jio Financial Services",
            "ZOMATO" to "Zomato Ltd.",
            "PAYTM" to "One97 Communications Ltd. (Paytm)",
            "HAL" to "Hindustan Aeronautics Ltd.",
            "BEL" to "Bharat Electronics Ltd.",
            "NHPC" to "NHPC Ltd.",
            "SJVN" to "SJVN Ltd.",
            "IRCTC" to "Indian Railway Catering & Tourism Corp",
            "PFC" to "Power Finance Corporation",
            "REC" to "REC Ltd.",
            "MRF" to "MRF Ltd.",
            "TATACOMM" to "Tata Communications Ltd.",
            "TATACONSUM" to "Tata Consumer Products Ltd.",
            "RVNL" to "Rail Vikas Nigam Ltd.",
            "IRCON" to "Ircon International Ltd.",
            "BHEL" to "Bharat Heavy Electricals Ltd.",
            "IOC" to "Indian Oil Corporation Ltd.",
            "SAIL" to "Steel Authority of India Ltd.",
            "GAIL" to "GAIL (India) Ltd.",
            "YESBANK" to "Yes Bank Ltd.",
            "BANKBARODA" to "Bank of Baroda",
            "BANKINDIA" to "Bank of India",
            "CANBK" to "Canara Bank",
            "UNIONBANK" to "Union Bank of India",
            "PNB" to "Punjab National Bank",
            "IDFCFIRSTB" to "IDFC First Bank Ltd.",
            "FEDERALBNK" to "The Federal Bank Ltd.",
            "BANDHANBNK" to "Bandhan Bank Ltd.",
            "IDBI" to "IDBI Bank Ltd.",
            "INDUSTOWER" to "Indus Towers Ltd.",
            "CHOLAFIN" to "Cholamandalam Investment & Finance",
            "SHRIRAMFIN" to "Shriram Finance Ltd.",
            "MUTHOOTFIN" to "Muthoot Finance Ltd.",
            "LICHSGFIN" to "LIC Housing Finance Ltd.",
            "LICI" to "Life Insurance Corporation of India",
            "HINDCOPPER" to "Hindustan Copper Ltd.",
            "NMDC" to "NMDC Ltd.",
            "NATIONALUM" to "National Aluminium Co. Ltd.",
            "JINDALSTEL" to "Jindal Steel & Power Ltd.",
            "TATACHEM" to "Tata Chemicals Ltd.",
            "PIDILITIND" to "Pidilite Industries Ltd.",
            "AMBUJACEM" to "Ambuja Cements Ltd.",
            "ACC" to "ACC Ltd.",
            "SHREECEM" to "Shree Cement Ltd.",
            "RAMCOCEM" to "The Ramco Cements Ltd.",
            "INDIGO" to "InterGlobe Aviation Ltd. (IndiGo)",
            "DLF" to "DLF Ltd.",
            "GODREJPROP" to "Godrej Properties Ltd.",
            "OBEROIRLTY" to "Oberoi Realty Ltd.",
            "MACROTECH" to "Macrotech Developers Ltd. (Lodha)",
            "CONCOR" to "Container Corporation of India Ltd.",
            "GMRINFRA" to "GMR Airports Infrastructure Ltd.",
            "TRENT" to "Trent Ltd.",
            "NYKAA" to "FSN E-Commerce Ventures (Nykaa)",
            "DMART" to "Avenue Supermarts Ltd. (DMart)",
            "BIOCON" to "Biocon Ltd.",
            "LUPIN" to "Lupin Ltd.",
            "AUROPHARMA" to "Aurobindo Pharma Ltd.",
            "TORNTPHARM" to "Torrent Pharmaceuticals Ltd.",
            "ALKEM" to "Alkem Laboratories Ltd.",
            "GLAND" to "Gland Pharma Ltd.",
            "SYNGENE" to "Syngene International Ltd.",
            "ABFRL" to "Aditya Birla Fashion and Retail Ltd.",
            "PAGEIND" to "Page Industries Ltd.",
            "BATAINDIA" to "Bata India Ltd.",
            "RELAXO" to "Relaxo Footwears Ltd.",
            "COFORGE" to "Coforge Ltd.",
            "PERSISTENT" to "Persistent Systems Ltd.",
            "LTTS" to "L&T Technology Services Ltd.",
            "LTIM" to "LTIMindtree Ltd.",
            "MPHASIS" to "Mphasis Ltd.",
            "KPITTECH" to "KPIT Technologies Ltd.",
            "TATAELXSI" to "Tata Elxsi Ltd.",
            "OFSS" to "Oracle Financial Services Software Ltd.",
            "PRESTIGE" to "Prestige Estates Projects Ltd.",
            "ASHOKLEY" to "Ashok Leyland Ltd.",
            "ESCORTS" to "Escorts Kubota Ltd.",
            "BALKRISIND" to "Balkrishna Industries Ltd.",
            "TIINDIA" to "Tube Investments of India Ltd.",
            "CEAT" to "CEAT Ltd.",
            "APOLLOTYRE" to "Apollo Tyres Ltd.",
            "EXIDEIND" to "Exide Industries Ltd.",
            "AMARAJABAT" to "Amara Raja Energy & Mobility Ltd.",
            "POLYCAB" to "Polycab India Ltd.",
            "KEI" to "KEI Industries Ltd.",
            "HAVELLS" to "Havells India Ltd.",
            "VOLTAS" to "Voltas Ltd.",
            "BLUESTARCO" to "Blue Star Ltd.",
            "CROMPTON" to "Crompton Greaves Consumer Electricals Ltd.",
            "DIXON" to "Dixon Technologies (India) Ltd.",
            "BHARATFORG" to "Bharat Forge Ltd.",
            "CUMMINSIND" to "Cummins India Ltd.",
            "THERMAX" to "Thermax Ltd.",
            "ASTRAL" to "Astral Ltd.",
            "SUPREMEIND" to "Supreme Industries Ltd.",
            "FINCABLES" to "Finolex Cables Ltd.",
            "DEEPAKNTR" to "Deepak Nitrite Ltd.",
            "SRF" to "SRF Ltd.",
            "COROMANDEL" to "Coromandel International Ltd.",
            "CHAMBLFERT" to "Chambal Fertilisers & Chemicals Ltd.",
            "UPL" to "UPL Ltd.",
            "MAXHEALTH" to "Max Healthcare Institute Ltd.",
            "LALPATHLAB" to "Dr. Lal PathLabs Ltd.",
            "METROPOLIS" to "Metropolis Healthcare Ltd.",
            "FORTIS" to "Fortis Healthcare Ltd.",
            "NH" to "Narayana Hrudayalaya Ltd.",
            "ASTERDM" to "Aster DM Healthcare Ltd.",
            "JKPAPER" to "JK Paper Ltd.",
            "WESTLIFE" to "Westlife Foodworld Ltd.",
            "DEVYANI" to "Devyani International Ltd.",
            "JUBILANT" to "Jubilant FoodWorks Ltd.",
            "RADICO" to "Radico Khaitan Ltd.",
            "MCDOWELL-N" to "United Spirits Ltd.",
            "VBL" to "Varun Beverages Ltd.",
            "MARICO" to "Marico Ltd.",
            "COLPAL" to "Colgate-Palmolive (India) Ltd.",
            "GODREJCP" to "Godrej Consumer Products Ltd.",
            "DABUR" to "Dabur India Ltd.",
            "BALRAMCHIN" to "Balrampur Chini Mills Ltd.",
            "TRIDENT" to "Trident Ltd.",
            "WELSPUNLIV" to "Welspun Living Ltd.",
            "RAYMOND" to "Raymond Ltd.",
            "CARBORUNIV" to "Carborundum Universal Ltd.",
            "GRINDWELL" to "Grindwell Norton Ltd.",
            "SHYAMMETL" to "Shyam Metalics and Energy Ltd.",
            "HINDZINC" to "Hindustan Zinc Ltd."
        )
    }

    val filteredStocks = remember(stockName) {
        if (stockName.isBlank()) {
            indianStocks.take(8)
        } else {
            indianStocks.filter { (ticker, fullname) ->
                ticker.contains(stockName, ignoreCase = true) ||
                fullname.contains(stockName, ignoreCase = true)
            }.take(10)
        }
    }

    val quantity = quantityStr.toIntOrNull() ?: 0
    val rawAmount = profitOrLossAmountStr.toDoubleOrNull() ?: 0.0
    val taxAmount = taxAmountStr.toDoubleOrNull() ?: 0.0

    val calculatedNetProfit = if (isProfitMode) {
        rawAmount - taxAmount
    } else {
        -rawAmount - taxAmount
    }

    var showDatePicker by remember { mutableStateOf(false) }
    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(tradeDate)
            if (parsed != null) {
                calendar.time = parsed
            }
        } catch (e: Exception) {}

        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                tradeDate = TrackWiseUtils.formatDate(cal.time, "yyyy-MM-dd")
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { showDatePicker = false }
            show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "New Trade Log 📝",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Stock Name Selector with Dropdown / Autocomplete
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = stockName,
                        onValueChange = {
                            stockName = it
                            isDropdownExpanded = true
                        },
                        label = { Text("Stock Name") },
                        placeholder = { Text("e.g. RELIANCE, TCS") },
                        leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }) {
                                Icon(
                                    imageVector = if (isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Toggle Dropdown"
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .heightIn(max = 250.dp),
                        properties = androidx.compose.ui.window.PopupProperties(focusable = false)
                    ) {
                        if (filteredStocks.isNotEmpty()) {
                            filteredStocks.forEach { (ticker, fullname) ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(text = ticker, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(text = fullname, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        stockName = ticker
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (stockName.isNotBlank()) "Use Custom Stock: \"$stockName\"" else "Enter Other Custom Stock...",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            onClick = {
                                isDropdownExpanded = false
                            }
                        )
                    }
                }

                // Date Picker Field
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = tradeDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Trade Date") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantity") },
                    placeholder = { Text("e.g. 10") },
                    leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Profit or Loss Switch Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isProfitMode) BrandGreen else Color.Transparent)
                            .clickable { isProfitMode = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Profit 🟢",
                            fontWeight = FontWeight.Bold,
                            color = if (isProfitMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isProfitMode) BrandRose else Color.Transparent)
                            .clickable { isProfitMode = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loss 🔴",
                            fontWeight = FontWeight.Bold,
                            color = if (!isProfitMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = profitOrLossAmountStr,
                    onValueChange = { profitOrLossAmountStr = it },
                    label = { Text(if (isProfitMode) "Profit Amount (₹)" else "Loss Amount (₹)") },
                    placeholder = { Text("e.g. 50.0") },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = taxAmountStr,
                    onValueChange = { taxAmountStr = it },
                    label = { Text("Tax / Brokerage Amount (₹)") },
                    placeholder = { Text("e.g. 2.50") },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // Calculated Live Net Profit Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Net Profit / Loss",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (calculatedNetProfit >= 0) {
                            "+₹${calculatedNetProfit}"
                        } else {
                            "-₹${kotlin.math.abs(calculatedNetProfit)}"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (calculatedNetProfit >= 0) BrandGreen else BrandRose
                    )
                }

                // Submit/Save button
                Button(
                    onClick = {
                        val finalProfit = if (isProfitMode) rawAmount else 0.0
                        val finalLoss = if (!isProfitMode) rawAmount else 0.0
                        
                        val trade = StockTradeEntity(
                            id = UUID.randomUUID().toString(),
                            userId = viewModel.sessionUser.value?.id ?: "guest",
                            stockName = stockName.ifBlank { "Unknown Stock" },
                            quantity = quantity,
                            profit = finalProfit,
                            loss = finalLoss,
                            taxAmount = taxAmount,
                            netProfit = calculatedNetProfit,
                            date = tradeDate
                        )
                        viewModel.insertStockTrade(trade)
                        onSaveSuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (calculatedNetProfit >= 0) BrandGreen else BrandRose
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Save Trade Log",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun TradeLogsTab(viewModel: TrackWiseViewModel, decimalFormat: DecimalFormat) {
    val stockTrades by viewModel.allStockTrades.collectAsState()
    var logsSubTab by remember { mutableStateOf(0) } // 0: Day, 1: Month, 2: Year

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Day / Month / Year Sub-tab selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(4.dp)
        ) {
            listOf("Day", "Month", "Year").forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (logsSubTab == index) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { logsSubTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (logsSubTab == index) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (stockTrades.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No saved trades yet 📈",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Use the 'Log Trade' tab to add your logs",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            // Group and Aggregate trades based on Sub-tab
            when (logsSubTab) {
                0 -> DailyLogsGroup(stockTrades, viewModel, decimalFormat)
                1 -> MonthlyLogsGroup(stockTrades, decimalFormat)
                2 -> YearlyLogsGroup(stockTrades, decimalFormat)
            }
        }
    }
}

@Composable
fun DailyLogsGroup(
    trades: List<StockTradeEntity>,
    viewModel: TrackWiseViewModel,
    decimalFormat: DecimalFormat
) {
    val grouped = remember(trades) {
        trades.groupBy { it.date }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        grouped.forEach { (date, dailyTrades) ->
            item {
                val netProfit = dailyTrades.sumOf { it.netProfit }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Date Header & Daily Aggregate
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = date,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Net: " + if (netProfit >= 0) "+₹${decimalFormat.format(netProfit)}" else "-₹${decimalFormat.format(kotlin.math.abs(netProfit))}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (netProfit >= 0) BrandGreen else BrandRose
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        // Table Headers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Text(text = "Stock", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(text = "Qty", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                            Text(text = "Profit/Loss", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End)
                            Text(text = "Tax", modifier = Modifier.weight(0.9f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End)
                            Text(text = "", modifier = Modifier.width(30.dp)) // for action
                        }

                        // Daily Individual Items Rows
                        dailyTrades.forEach { trade ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = trade.stockName,
                                    modifier = Modifier.weight(1.5f),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${trade.quantity}",
                                    modifier = Modifier.weight(0.8f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                val displayValue = if (trade.profit > 0) trade.profit else -trade.loss
                                Text(
                                    text = if (displayValue >= 0) "+₹${decimalFormat.format(displayValue)}" else "-₹${decimalFormat.format(kotlin.math.abs(displayValue))}",
                                    modifier = Modifier.weight(1.5f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (displayValue >= 0) BrandGreen else BrandRose,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = "₹${decimalFormat.format(trade.taxAmount)}",
                                    modifier = Modifier.weight(0.9f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.End
                                )
                                IconButton(
                                    onClick = { viewModel.deleteStockTrade(trade.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
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
}

@Composable
fun MonthlyLogsGroup(
    trades: List<StockTradeEntity>,
    decimalFormat: DecimalFormat
) {
    val grouped = remember(trades) {
        trades.groupBy {
            if (it.date.length >= 7) it.date.substring(0, 7) else "Unknown" // YYYY-MM
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        grouped.forEach { (yearMonth, monthlyTrades) ->
            item {
                val totalProfit = monthlyTrades.sumOf { it.profit }
                val totalLoss = monthlyTrades.sumOf { it.loss }
                val totalTax = monthlyTrades.sumOf { it.taxAmount }
                val netProfit = monthlyTrades.sumOf { it.netProfit }

                val formattedLabel = remember(yearMonth) {
                    try {
                        val sdfIn = SimpleDateFormat("yyyy-MM", Locale.US)
                        val sdfOut = SimpleDateFormat("MMMM yyyy", Locale.US)
                        sdfOut.format(sdfIn.parse(yearMonth) ?: Date())
                    } catch (e: Exception) {
                        yearMonth
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = formattedLabel,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Total Profits logged", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text(text = "₹${decimalFormat.format(totalProfit)}", color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text(text = "Total Losses logged", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text(text = "₹${decimalFormat.format(totalLoss)}", color = BrandRose, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Taxes & Fees Paid", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text(text = "₹${decimalFormat.format(totalTax)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (netProfit >= 0) BrandGreen.copy(alpha = 0.1f) else BrandRose.copy(alpha = 0.1f))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Net Month Balance", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = if (netProfit >= 0) "+₹${decimalFormat.format(netProfit)}" else "-₹${decimalFormat.format(kotlin.math.abs(netProfit))}",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = if (netProfit >= 0) BrandGreen else BrandRose
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YearlyLogsGroup(
    trades: List<StockTradeEntity>,
    decimalFormat: DecimalFormat
) {
    val grouped = remember(trades) {
        trades.groupBy {
            if (it.date.length >= 4) it.date.substring(0, 4) else "Unknown" // YYYY
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        grouped.forEach { (year, yearlyTrades) ->
            item {
                val totalProfit = yearlyTrades.sumOf { it.profit }
                val totalLoss = yearlyTrades.sumOf { it.loss }
                val totalTax = yearlyTrades.sumOf { it.taxAmount }
                val netProfit = yearlyTrades.sumOf { it.netProfit }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Year $year",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Yearly Profits", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text(text = "₹${decimalFormat.format(totalProfit)}", color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Column {
                                Text(text = "Yearly Losses", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text(text = "₹${decimalFormat.format(totalLoss)}", color = BrandRose, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Yearly Taxes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text(text = "₹${decimalFormat.format(totalTax)}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (netProfit >= 0) BrandGreen.copy(alpha = 0.12f) else BrandRose.copy(alpha = 0.12f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Net Year Performance", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = if (netProfit >= 0) "+₹${decimalFormat.format(netProfit)}" else "-₹${decimalFormat.format(kotlin.math.abs(netProfit))}",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = if (netProfit >= 0) BrandGreen else BrandRose
                            )
                        }
                    }
                }
            }
        }
    }
}
