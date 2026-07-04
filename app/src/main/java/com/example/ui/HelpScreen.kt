package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun HelpScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go back",
                            tint = BrandViolet
                        )
                    }
                    Column {
                        Text(
                            text = "TRACKWISE USER MANUAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandViolet,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "How TrackWise Works",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✨ Welcome to Your Personal Engine",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandViolet
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "TrackWise is a fully unified environment combining tasks, daily habits, deep health logs, hydration targets, multi-account net worth sheets, and balanced budgets. Below is a guide on how everything syncs automatically to guide you to peak performance.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Overview segments
            item { HelpHeader("CORE SYSTEM FUNCTIONALITIES") }

            item {
                HelpTopicCard(
                    icon = Icons.Default.Assessment,
                    title = "Analytics & Live Dashboards",
                    color = BrandViolet,
                    description = "Analyze trends across finance, habits, and health in real time. Standard charts summarize your categorical allocations, monthly savings targets, water intake trends, and sleep durations cleanly."
                )
            }

            item {
                HelpTopicCard(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "Balanced Budgeting Equation",
                    color = BrandCyan,
                    description = "When adding items in Finance, they are strictly kept balanced under: Income = Expenses + Savings. Expenses subtract from your selected Net Worth asset sources, and any logged Savings are directly added into your Net Worth sheets to grow your wealth."
                )
            }

            item {
                HelpTopicCard(
                    icon = Icons.Default.TrendingUp,
                    title = "Net Worth & Accounts",
                    color = BrandGreen,
                    description = "Track what you own versus what you owe. Under Assets (Cash, Bank Accounts, Mutual Funds) and Liabilities/Loans, keep a real-time net balance of your total assets minus debts. This adapts instantly when savings or expenses are updated."
                )
            }

            item {
                HelpTopicCard(
                    icon = Icons.Default.Favorite,
                    title = "Health, Hydration & Sleep",
                    color = BrandRose,
                    description = "Input physical measurements to calculate your BMI and daily calorie goals. Record exercise logs, monitor sleep cycles, view doctor recommendation tips, and click '+' or '-' directly on the hydration meters to complete daily water goals."
                )
            }

            item {
                HelpTopicCard(
                    icon = Icons.Default.CheckCircle,
                    title = "Daily Habits & Tasks",
                    color = BrandAmber,
                    description = "Build streaks by ticking daily habits. Streaks auto-progress toward predefined milestones (e.g., 3-day Spark, 30-day Calendar Crusher). Manage daily tasks by sorting priority levels, setting sub-tasks, and mapping deadline triggers."
                )
            }

            item {
                HelpTopicCard(
                    icon = Icons.Default.Archive,
                    title = "The Completed Items Archive",
                    color = BrandIndigo,
                    description = "When you complete tasks, finish exercises, resolve medical symptoms, purchase wishlist items, or purchase groceries, they are logged safely into the Archive screen. You can review all historically completed records grouped by category."
                )
            }

            item { HelpHeader("APP SECURE CUSTOMIZATION") }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "💡 Quick Controls Tip",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Use the left settings drawer to switch theme accents (Violet, Blue, Green, Orange, Red) and set custom sound indicators for task completions. All changes are saved locally instantly.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HelpHeader(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
fun HelpTopicCard(
    icon: ImageVector,
    title: String,
    color: Color,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    lineHeight = 17.sp
                )
            }
        }
    }
}
