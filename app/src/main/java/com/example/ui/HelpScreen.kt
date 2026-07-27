package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedChapter by remember { mutableIntStateOf(-1) }

    val chapters = listOf(
        ChapterSpec(
            title = "1. Unified System Architecture",
            subtitle = "Database sync, MVVM flows, and the unified dashboard engine",
            icon = Icons.Default.Layers,
            color = BrandViolet,
            content = """
                Welcome to TrackWise, a fully unified personal intelligence environment designed to integrate every critical domain of your daily life.
                
                ■ Core Architecture & Data Engine:
                The application is built on a highly optimized Model-View-ViewModel (MVVM) design pattern. All data is persisted locally in an encrypted SQLite database managed by the Android Room Persistence Library. 
                Rather than using static data or lazy polls, TrackWise leverages Kotlin Coroutines and asynchronous StateFlow streams. When any record changes—be it an expense, a completed habit, a symptom log, or a task checklist—the repository emits a new state. This immediately flows to the ViewModel, triggering lightweight recompositions across all active widgets.
                
                ■ Real-time Dashboard Sync:
                The Main Dashboard acts as your central command deck. Every widget on this screen pulls directly from the unified database:
                - Daily Habits Widget: Streams today's habit compliance.
                - Budget Summary: Reflects immediate balances, today's spending, and net asset valuation.
                - Hydration & Wellness: Synchronizes current liquid volumes and wellness targets.
                - Tasks List: Displays urgent task deadlines.
                
                By avoiding fragmented databases or local state mirrors, the app guarantees that a habit marked complete on the Workspace tab is immediately registered in Analytics, the Calendar, and the Dashboard without a single refresh.
            """.trimIndent()
        ),
        ChapterSpec(
            title = "2. Daily Habits Engine",
            subtitle = "Completion JSON, habit streaks, custom repeat intervals, and badges",
            icon = Icons.Default.Repeat,
            color = BrandPink,
            content = """
                Building consistent daily habits is the foundation of peak personal performance. The TrackWise Habits Engine is meticulously engineered to track, evaluate, and reward consistency.
                
                ■ Persistence & Serialization:
                Habit records are saved with detailed metadata. The historical records of completions are stored inside the database as a serialized JSON array of ISO date strings (e.g., '["2026-07-25", "2026-07-26"]'). When you click a checkmark on a habit tile, the app deserializes this array, adds or removes today's date, updates the database, and recalculates all compliance metrics.
                
                ■ The Streak Calculation Algorithm:
                Streaks are computed dynamically. The algorithm analyzes the frequency settings of the habit:
                1. Daily: Scans consecutive days backward from today. If yesterday was completed, the streak continues. If today is completed, the count increments. If neither is completed, the streak resets.
                2. Weekdays: Excludes weekends from the consecutive-day scanner, ensuring your professional routines aren't penalized over weekends.
                3. Custom: Supports specific custom repeat intervals (e.g., every 3 days) and correctly aligns calculations with the custom period.
                
                ■ Milestone Badges & Gamification:
                To keep you motivated, compliance thresholds unlock milestone badges:
                - 3-day Spark: Encourages early-stage commitment.
                - 7-day Pioneer: Marks the completion of a full weekly cycle.
                - 15-day Builder: Awarded when a habit begins to wire into your neural pathways.
                - 30-day Calendar Crusher: Represents true behavioral automation.
                
                ■ Custom Repeats:
                You can configure custom recurrence intervals from 1 to 999 Days, Weeks, Months, or Years. When "custom" is selected in the creation sheet, a text field dynamically appears allowing precise interval configuration.
            """.trimIndent()
        ),
        ChapterSpec(
            title = "3. Unified Task Management",
            subtitle = "Checklists, priority matrix, folders, and hashtag structures",
            icon = Icons.Default.Assignment,
            color = BrandViolet,
            content = """
                The Task Management Suite empowers you to capture, organize, and execute professional and personal objectives seamlessly.
                
                ■ The Priority Matrix:
                Every task is classified into one of three priority levels:
                - HIGH (Red): Crucial, time-sensitive targets.
                - MEDIUM (Orange): Important items with flexible timelines.
                - LOW (Blue): Minor tasks or long-term backlogs.
                
                ■ Folders & Project Organization:
                Group tasks into dedicated Folders (e.g., Work, Personal, Fitness, Learning). The folder view acts as a separate organizational dashboard where you can drill down into folder-specific tasks and habits or easily reassign items in bulk.
                
                ■ Hashtag Tagging Engine:
                For dynamic, cross-functional organization, TrackWise parses plain text hashtags. If you append '#projectX' to a task title or description, or a habit note, the system automatically extracts the tag. 
                Opening the 'Hashtags' screen reveals a standalone page with separate tabs for Habits and Tasks. You can click on any tag card to open its independent detail screen, allowing you to view and toggle tagged habits and tasks side-by-side or expand them to quickly assign other items.
                
                ■ Checklist Subtasks:
                For complex actions, you can append nested checklists. The parent task progress bar automatically calculates completion percentages:
                Progress = (Completed Subtasks / Total Subtasks) * 100%
            """.trimIndent()
        ),
        ChapterSpec(
            title = "4. Balanced Budgeting & Finance",
            subtitle = "Income equation, net worth sheets, asset/liability tracking",
            icon = Icons.Default.AttachMoney,
            color = BrandOrange,
            content = """
                Take absolute control of your financial destiny with our structured multi-account ledger.
                
                ■ The Fundamental Budgeting Equation:
                All transactions conform to the balanced financial model:
                Income = Expenses + Savings
                
                - Income: Money flowing in increases your total asset balances.
                - Expenses: Funds leaving are deducted from the specific account you select (e.g., Credit Card, Cash, Checking).
                - Savings: Logged savings are direct allocations toward your future net worth, automatically transferred to checking, investment, or dedicated savings accounts.
                
                ■ Assets & Liabilities (Net Worth Ledger):
                TrackWise organizes your financial profile into accounts:
                - Assets: Cash, Bank Accounts, Savings, Mutual Funds, and Crypto Ledgers.
                - Liabilities: Credit Cards, Student Loans, Mortgages, and Personal Debts.
                
                Your overall Net Worth is updated instantly:
                Net Worth = Σ(Assets) - Σ(Liabilities)
                
                ■ Automatic Transaction Sync:
                When you add a transaction, the engine records the category, payment source, and date. It immediately reconciles the balances. For example, logging a $50 fuel expense from your 'Credit Card' account automatically increases that liability by $50, decreasing your net worth, while updating monthly spending charts.
            """.trimIndent()
        ),
        ChapterSpec(
            title = "5. Health & Wellness",
            subtitle = "BMI calculations, hydration logs, sleep tracking, and medical logs",
            icon = Icons.Default.Favorite,
            color = BrandGreen,
            content = """
                Achieving high performance requires physical wellness. TrackWise incorporates a complete health tracking station.
                
                ■ The Body Mass Index (BMI) Engine:
                When you enter your Height (cm) and Weight (kg), the app calculates BMI:
                BMI = Weight (kg) / (Height (m))^2
                
                The system categorizes your score:
                - Underweight: < 18.5
                - Normal Weight: 18.5 - 24.9
                - Overweight: 25.0 - 29.9
                - Obese: >= 30.0
                It also estimates your BMR (Basal Metabolic Rate) to recommend personalized daily calorie intakes.
                
                ■ Hydration Meter:
                Click '+' or '-' on the hydration card to record daily water intake. The widget progress ring fills up toward your custom target (e.g., 2500 ml). It resets every midnight and logs historical water targets.
                
                ■ Sleep Cycle Analysis:
                Log sleep onset and wake-up times to track total sleep duration. The system computes sleep efficiency based on deep sleep segments and tracks weekly averages to ensure adequate recovery.
                
                ■ Exercise Logs:
                Record cardio, strength, or flexibility exercises, mapping duration and calories burned.
                
                ■ Medical & Symptom Logging:
                Log physical symptoms, severity levels (Mild, Moderate, Severe), medication schedules, and doctor consultations. Once a symptom resolves, click 'Mark Resolved' to file it into the secure completed archives.
            """.trimIndent()
        ),
        ChapterSpec(
            title = "6. Time-Bound Utilities",
            subtitle = "Countdown timers, high-precision stopwatches, and alarms",
            icon = Icons.Default.Timer,
            color = BrandIndigo,
            content = """
                Time is your most valuable asset. TrackWise equips you with robust time-management utilities.
                
                ■ The Countdown Timer:
                Configure countdown targets for work intervals, study, or meditation.
                - Offers preset durations (5m, 15m, 25m Pomodoro, 1h).
                - Displays an elegant, smooth circular countdown progress ring.
                - Features a ticking sound option and trigger alerts.
                - Operates safely in the background using Android coroutine-backed timers.
                
                ■ High-Precision Stopwatch:
                Perfect for timing sprints, workouts, or reading blocks.
                - Records split-laps with millisecond accuracy.
                - Displays lap lists scrolling dynamically.
                
                ■ Alarm Clock Suite:
                Set Wake-Up alarms, daily reminders, or medicine trackers.
                - Configure custom snooze intervals (5m, 10m, 15m).
                - Bind recurring days (e.g., Mon, Wed, Fri).
                - Add custom labels.
            """.trimIndent()
        ),
        ChapterSpec(
            title = "7. Shopping, Grocery & Wishlist",
            subtitle = "Smart grocery lists, wishlist priorities, and financial purchasing",
            icon = Icons.Default.ShoppingCart,
            color = BrandPink,
            content = """
                Manage your consumer habits responsibly and organize your household needs.
                
                ■ Interactive Grocery Lists:
                Add items with target quantities and categories (e.g., Dairy, Produce, Pantry).
                - Check off items as you shop.
                - Tapping 'Clear Completed' sweeps purchased items directly into your historical logs.
                
                ■ The Intentional Wishlist:
                Avoid impulsive spending! Add desires to your Wishlist, recording price, priority, and links.
                - Set custom savings goals specifically for big-ticket wishlist items.
                - Track progress toward purchasing.
                - When you finally click 'Purchase', the app prompts you to select a payment account, auto-creates a financial transaction under the Expense category 'Shopping', deducts the balance from your Net Worth ledger, and files the item safely in the Archive.
            """.trimIndent()
        ),
        ChapterSpec(
            title = "8. Social & Occasions",
            subtitle = "Friends profiles, birthday registries, anniversaries, and custom style cards",
            icon = Icons.Default.People,
            color = BrandCyan,
            content = """
                Strengthen your relationships and never miss important life events.
                
                ■ Friends & Contacts Directory:
                Keep a warm log of your loved ones:
                - Save birthdates, contact info, and special preferences.
                - Keep gift ideas listed under each friend to refer back to when birthdays approach.
                
                ■ Occasion & Milestone Registry:
                Track birthdays, marriage anniversaries, work milestones, or national events.
                - Customize cards with decorative background images.
                - Choose custom text colors.
                - Enable automated reminders so you receive notifications days in advance.
                - Integrated directly into the main calendar view to display upcoming celebrations.
            """.trimIndent()
        ),
        ChapterSpec(
            title = "9. The Multi-Faith Calendar",
            subtitle = "Traditional Gregorian, Islamic Hijri, and Hindu calendars",
            icon = Icons.Default.CalendarMonth,
            color = BrandViolet,
            content = """
                TrackWise features a multi-system calendar allowing you to stay connected to your religious and cultural events.
                
                ■ Gregorian Calendar:
                Provides monthly and daily grid schedules displaying all tasks, habits, and occasions.
                
                ■ Islamic Hijri Calendar:
                For Muslim users, enabling this overlay maps Hijri dates (e.g., 1447 AH) alongside Gregorian dates.
                - Highlights major events: Ramadan, Eid al-Fitr, Eid al-Adha, Laylat al-Qadr, Ashura, and Islamic New Year.
                - Dynamically computes moon-phase offsets.
                
                ■ Hindu Luni-Solar Calendar:
                For Hindu users, enabling this overlay displays tithi cycles and festival markers.
                - Highlights major festivals: Diwali, Holi, Dussehra, Janmashtami, Raksha Bandhan, and Maha Shivratri.
                
                ■ Seamless Integration:
                Enable overlays via settings or directly in the top bar of the Calendar tab. The multi-faith schedule overlays automatically without cluttering your layout.
            """.trimIndent()
        ),
        ChapterSpec(
            title = "10. Analytics & Completed Archive",
            subtitle = "Spending breakdowns, completion rates, and historical logs",
            icon = Icons.Default.Assessment,
            color = BrandOrange,
            content = """
                Make data-driven decisions to optimize your routines and finance.
                
                ■ Categorical Spending Analysis:
                The Finance Analytics dashboard presents full charts breaking down expenses by category (e.g., Food, Travel, Medical, Shopping). It calculates saving ratios and compares active spending against monthly targets.
                
                ■ Habit Consistency Analytics:
                Track completion ratios over 7-day, 30-day, and 12-month periods. Bar charts show which habits are fully automated and which need attention.
                
                ■ Completed Items Archive:
                Your historical record of achievements:
                - Completed Tasks.
                - Resolved Medical Symptoms.
                - Purchased Grocery & Wishlist Items.
                - Past Workouts & Sleep Logs.
                
                This archive ensures your active lists remain light and fast, while keeping your historical data secure for auditing and reviews.
            """.trimIndent()
        )
    )

    if (selectedChapter != -1) {
        val chapter = chapters[selectedChapter]
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "CHAPTER ${selectedChapter + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = chapter.color,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = chapter.title.substringAfter(". "),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedChapter = -1 }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to index", tint = chapter.color)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(chapter.color.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = chapter.icon,
                                            contentDescription = null,
                                            tint = chapter.color,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = chapter.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = chapter.subtitle,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                                Text(
                                    text = chapter.content,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
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
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go back",
                            tint = BrandViolet
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                                text = "✨ Interactive Reference Guide",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandViolet
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Welcome to the official 10-Chapter TrackWise Comprehensive Guidebook. This manual covers every mathematical equation, database architecture flow, routine streaks, and balanced budgeting module in deep detail. Select a chapter below to open the complete reference guide.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "GUIDEBOOK CHAPTERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            itemsIndexed(chapters) { idx, chapter ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .clickable { selectedChapter = idx }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(chapter.color.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = chapter.icon,
                                contentDescription = null,
                                tint = chapter.color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = chapter.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = chapter.subtitle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Read chapter",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

data class ChapterSpec(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val content: String
)
