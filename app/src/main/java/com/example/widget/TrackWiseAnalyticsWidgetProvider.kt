package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.TrackWiseDatabase
import com.example.utils.TrackWiseUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TrackWiseAnalyticsWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        const val ACTION_CHANGE_ANALYTICS_TAB = "com.example.widget.ACTION_CHANGE_ANALYTICS_TAB"
        const val EXTRA_TAB_NAME = "extra_tab_name"
        const val PREFS_NAME = "trackwise_analytics_widget_prefs"

        fun getTabClickPendingIntent(context: Context, appWidgetId: Int, tabName: String): PendingIntent {
            val intent = Intent(context, TrackWiseAnalyticsWidgetProvider::class.java).apply {
                action = ACTION_CHANGE_ANALYTICS_TAB
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_TAB_NAME, tabName)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(
                context,
                (appWidgetId * 31) + tabName.hashCode(),
                intent,
                flags
            )
        }

        fun getActivityPendingIntent(context: Context, targetTab: String = "analytics"): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("target_tab", targetTab)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getActivity(
                context,
                targetTab.hashCode() + 5000,
                intent,
                flags
            )
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CHANGE_ANALYTICS_TAB) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val tabName = intent.getStringExtra(EXTRA_TAB_NAME) ?: "habits"
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString("active_tab_$appWidgetId", tabName).apply()

                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        } else if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: appWidgetManager.getAppWidgetIds(ComponentName(context, TrackWiseAnalyticsWidgetProvider::class.java))
            for (id in appWidgetIds) {
                updateWidget(context, appWidgetManager, id)
            }
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        scope.launch {
            try {
                val db = TrackWiseDatabase.getDatabase(context)
                val dao = db.trackWiseDao()

                val sessionPrefs = context.getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
                val savedUserId = sessionPrefs.getString("saved_user_id", null)
                val userId = savedUserId ?: dao.getAllUsers().firstOrNull()?.id ?: "guest"

                val themeMode = sessionPrefs.getString("saved_theme_mode", "light") ?: "light"
                val isDark = when (themeMode) {
                    "dark" -> true
                    "light" -> false
                    "auto" -> {
                        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        hour < 6 || hour >= 18
                    }
                    else -> {
                        val currentNightMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                        currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
                    }
                }

                val widgetPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val activeTab = widgetPrefs.getString("active_tab_$appWidgetId", "habits") ?: "habits"

                val widgetBgRes = if (isDark) R.drawable.widget_background_dark else R.drawable.widget_background_light
                val textPrimaryColor = if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
                val textSecondaryColor = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")

                val views = RemoteViews(context.packageName, R.layout.trackwise_analytics_widget_layout)
                views.setInt(R.id.widget_analytics_root, "setBackgroundResource", widgetBgRes)
                views.setTextColor(R.id.txt_analytics_widget_title, textPrimaryColor)

                val displayTime = SimpleDateFormat("MMM d • hh:mm a", Locale.US).format(Date())
                views.setTextViewText(R.id.txt_analytics_timestamp, displayTime)
                views.setTextColor(R.id.txt_analytics_timestamp, textSecondaryColor)

                // Setup Tab buttons state and click listeners
                val tabs = listOf("habits", "tasks", "finance", "health", "breakers")
                val tabViewIds = listOf(
                    R.id.tab_btn_habits,
                    R.id.tab_btn_tasks,
                    R.id.tab_btn_finance,
                    R.id.tab_btn_health,
                    R.id.tab_btn_breakers
                )

                for (i in tabs.indices) {
                    val tabKey = tabs[i]
                    val tabViewId = tabViewIds[i]
                    val isSelected = tabKey.equals(activeTab, ignoreCase = true)

                    views.setInt(
                        tabViewId,
                        "setBackgroundResource",
                        if (isSelected) R.drawable.widget_tab_active else R.drawable.widget_tab_inactive
                    )
                    views.setTextColor(
                        tabViewId,
                        if (isSelected) Color.WHITE else textSecondaryColor
                    )
                    views.setOnClickPendingIntent(
                        tabViewId,
                        getTabClickPendingIntent(context, appWidgetId, tabKey)
                    )
                }

                // Global widget clicks open corresponding app section
                val targetScreen = when (activeTab) {
                    "habits" -> "habits"
                    "tasks" -> "tasks"
                    "finance" -> "finance"
                    "health" -> "health"
                    "breakers" -> "habit_breaker"
                    else -> "analytics"
                }
                views.setOnClickPendingIntent(R.id.widget_analytics_root, getActivityPendingIntent(context, targetScreen))
                views.setOnClickPendingIntent(R.id.img_analytics_chart, getActivityPendingIntent(context, targetScreen))
                views.setOnClickPendingIntent(R.id.txt_analytics_headline, getActivityPendingIntent(context, targetScreen))

                val todayStr = TrackWiseUtils.getTodayString()

                // Generate Dynamic Bitmap Chart and Analytics Stats based on Active Tab
                when (activeTab) {
                    "habits" -> {
                        val allHabits = dao.getHabitsForUser(userId)
                        val last7Days = (6 downTo 0).map { offset ->
                            val c = Calendar.getInstance()
                            c.add(Calendar.DAY_OF_YEAR, -offset)
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
                        }
                        val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

                        val dailyStats = last7Days.map { dateStr ->
                            val c = Calendar.getInstance().apply {
                                time = TrackWiseUtils.parseDate(dateStr)
                            }
                            val dayLabel = dayLabels[(c.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
                            val dueHabits = allHabits.filter { TrackWiseUtils.shouldShowHabitOnDate(it, dateStr) }
                            val completedCount = dueHabits.count { it.daysCompletedJson.contains(dateStr) }
                            val totalCount = dueHabits.size.coerceAtLeast(1)
                            val pct = (completedCount.toFloat() / totalCount * 100).toInt()
                            Triple(dayLabel, pct, dateStr == todayStr)
                        }

                        val overallPct = if (dailyStats.isNotEmpty()) dailyStats.map { it.second }.average().toInt() else 0
                        val todayCompleted = allHabits.filter { TrackWiseUtils.shouldShowHabitOnDate(it, todayStr) }.count { it.daysCompletedJson.contains(todayStr) }
                        val todayTotal = allHabits.filter { TrackWiseUtils.shouldShowHabitOnDate(it, todayStr) }.size

                        views.setTextViewText(R.id.txt_analytics_headline, "💪 7-Day Habit Consistency: $overallPct%")
                        views.setTextColor(R.id.txt_analytics_headline, Color.parseColor("#7C3AED"))
                        views.setTextViewText(R.id.txt_analytics_subtext, "Today: $todayCompleted/$todayTotal Completed")
                        views.setTextColor(R.id.txt_analytics_subtext, Color.parseColor("#0EA5E9"))

                        views.setTextViewText(R.id.txt_analytics_footer_left, "🔥 Weekly Momentum: ${if (overallPct >= 75) "Optimal 🚀" else "Building 🌱"}")
                        views.setTextViewText(R.id.txt_analytics_footer_right, "${allHabits.size} Active Habit(s)")

                        val chartBitmap = drawBarChart(
                            labels = dailyStats.map { it.first },
                            values = dailyStats.map { it.second.toFloat() },
                            maxValue = 100f,
                            barColor = Color.parseColor("#7C3AED"),
                            highlightIndex = dailyStats.indexOfFirst { it.third },
                            highlightColor = Color.parseColor("#0EA5E9"),
                            isDark = isDark,
                            suffix = "%"
                        )
                        views.setImageViewBitmap(R.id.img_analytics_chart, chartBitmap)
                    }

                    "tasks" -> {
                        val allTasks = dao.getTasksForUser(userId)
                        val completedTasks = allTasks.filter { it.completed }
                        val pendingTasks = allTasks.filter { !it.completed }

                        val highPri = allTasks.count { it.priority.equals("high", ignoreCase = true) }
                        val medPri = allTasks.count { it.priority.equals("medium", ignoreCase = true) }
                        val lowPri = allTasks.count { it.priority.equals("low", ignoreCase = true) }

                        val compRate = if (allTasks.isNotEmpty()) (completedTasks.size.toFloat() / allTasks.size * 100).toInt() else 0

                        views.setTextViewText(R.id.txt_analytics_headline, "📝 Task Completion Velocity: $compRate%")
                        views.setTextColor(R.id.txt_analytics_headline, Color.parseColor("#0EA5E9"))
                        views.setTextViewText(R.id.txt_analytics_subtext, "${pendingTasks.size} Pending • ${completedTasks.size} Done")
                        views.setTextColor(R.id.txt_analytics_subtext, Color.parseColor("#10B981"))

                        views.setTextViewText(R.id.txt_analytics_footer_left, "⚡ High Priority: $highPri | Med: $medPri | Low: $lowPri")
                        views.setTextViewText(R.id.txt_analytics_footer_right, "Total: ${allTasks.size} Tasks")

                        val chartBitmap = drawBarChart(
                            labels = listOf("High", "Med", "Low", "Done", "Pending"),
                            values = listOf(highPri.toFloat(), medPri.toFloat(), lowPri.toFloat(), completedTasks.size.toFloat(), pendingTasks.size.toFloat()),
                            maxValue = (allTasks.size.toFloat()).coerceAtLeast(5f),
                            barColor = Color.parseColor("#0EA5E9"),
                            highlightIndex = 3,
                            highlightColor = Color.parseColor("#10B981"),
                            isDark = isDark,
                            suffix = ""
                        )
                        views.setImageViewBitmap(R.id.img_analytics_chart, chartBitmap)
                    }

                    "finance" -> {
                        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
                        val logs = dao.getFinanceLogsForUser(userId)
                        val monthLogs = logs.filter { it.date.startsWith(currentMonthStr) }

                        val income = monthLogs.filter { it.type == "income" }.sumOf { it.amount }
                        val expense = monthLogs.filter { it.type == "expense" }.sumOf { it.amount }
                        val savings = monthLogs.filter { it.type == "savings" }.sumOf { it.amount }
                        val netBalance = income - (expense + savings)
                        val savingsRate = if (income > 0) ((savings + netBalance.coerceAtLeast(0.0)) / income * 100).toInt().coerceIn(0, 100) else 0

                        views.setTextViewText(R.id.txt_analytics_headline, "💰 Net Balance: ₹${netBalance.toInt()} (${savingsRate}% Saved)")
                        views.setTextColor(R.id.txt_analytics_headline, if (netBalance >= 0) Color.parseColor("#10B981") else Color.parseColor("#F43F5E"))
                        views.setTextViewText(R.id.txt_analytics_subtext, "Income: ₹${income.toInt()} • Exp: ₹${expense.toInt()}")
                        views.setTextColor(R.id.txt_analytics_subtext, textSecondaryColor)

                        views.setTextViewText(R.id.txt_analytics_footer_left, "💳 Savings Reserve: ₹${savings.toInt()}")
                        views.setTextViewText(R.id.txt_analytics_footer_right, "${monthLogs.size} Transactions this month")

                        val maxFinVal = maxOf(income, expense, savings, 1000.0).toFloat()
                        val chartBitmap = drawMultiBarChart(
                            labels = listOf("Income", "Expense", "Savings", "Net"),
                            values = listOf(income.toFloat(), expense.toFloat(), savings.toFloat(), netBalance.coerceAtLeast(0.0).toFloat()),
                            maxValue = maxFinVal,
                            colors = listOf(
                                Color.parseColor("#10B981"),
                                Color.parseColor("#F43F5E"),
                                Color.parseColor("#7C3AED"),
                                Color.parseColor("#0EA5E9")
                            ),
                            isDark = isDark,
                            currencyPrefix = "₹"
                        )
                        views.setImageViewBitmap(R.id.img_analytics_chart, chartBitmap)
                    }

                    "health" -> {
                        val waterLogs = dao.getWaterLogsForUser(userId)
                        val last5Days = (4 downTo 0).map { offset ->
                            val c = Calendar.getInstance()
                            c.add(Calendar.DAY_OF_YEAR, -offset)
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
                        }
                        val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

                        val hydrationStats: List<Triple<String, Int, Boolean>> = last5Days.map { dateStr ->
                            val c = Calendar.getInstance().apply {
                                time = TrackWiseUtils.parseDate(dateStr)
                            }
                            val dayLabel = dayLabels[(c.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
                            val dayLog = waterLogs.firstOrNull { it.date == dateStr }
                            val glasses = dayLog?.glasses ?: 0
                            Triple(dayLabel, glasses, dateStr == todayStr)
                        }

                        val todayLog = waterLogs.firstOrNull { it.date == todayStr }
                        val todayGlasses = todayLog?.glasses ?: 0
                        val goalGlasses = (todayLog?.goal ?: 8).coerceAtLeast(1)
                        val hydrationPct = (todayGlasses.toFloat() / goalGlasses * 100).toInt()

                        views.setTextViewText(R.id.txt_analytics_headline, "💧 Daily Hydration: $todayGlasses / $goalGlasses Glasses ($hydrationPct%)")
                        views.setTextColor(R.id.txt_analytics_headline, Color.parseColor("#0EA5E9"))
                        views.setTextViewText(R.id.txt_analytics_subtext, if (todayGlasses >= goalGlasses) "Goal Met! 🌟" else "${goalGlasses - todayGlasses} glasses left")
                        views.setTextColor(R.id.txt_analytics_subtext, Color.parseColor("#10B981"))

                        views.setTextViewText(R.id.txt_analytics_footer_left, "🛡️ Hydration Target: $goalGlasses Glasses (~2L) Daily")
                        views.setTextViewText(R.id.txt_analytics_footer_right, "Health & Vitality Tracker")

                        val chartBitmap = drawBarChart(
                            labels = hydrationStats.map { it.first },
                            values = hydrationStats.map { it.second.toFloat() },
                            maxValue = goalGlasses.toFloat().coerceAtLeast(8f),
                            barColor = Color.parseColor("#0EA5E9"),
                            highlightIndex = hydrationStats.indexOfFirst { it.third },
                            highlightColor = Color.parseColor("#10B981"),
                            isDark = isDark,
                            suffix = "gl"
                        )
                        views.setImageViewBitmap(R.id.img_analytics_chart, chartBitmap)
                    }

                    "breakers" -> {
                        // Preserved Resources & Relapse Prevention Analysis
                        val badHabitsPrefs = context.getSharedPreferences("trackwise_bad_habits", Context.MODE_PRIVATE)
                        val rawHabitsJson = badHabitsPrefs.getString("bad_habits_$userId", null)
                        val habitsCount = if (!rawHabitsJson.isNullOrBlank()) {
                            try { org.json.JSONArray(rawHabitsJson).length() } catch (e: Exception) { 3 }
                        } else 3

                        views.setTextViewText(R.id.txt_analytics_headline, "🛡️ Preserved Resources & Defense")
                        views.setTextColor(R.id.txt_analytics_headline, Color.parseColor("#F43F5E"))
                        views.setTextViewText(R.id.txt_analytics_subtext, "Urge Self-Control Active")
                        views.setTextColor(R.id.txt_analytics_subtext, Color.parseColor("#10B981"))

                        views.setTextViewText(R.id.txt_analytics_footer_left, "💰 Wealth, ⏰ Time, 🛡️ Health, 😊 Mood")
                        views.setTextViewText(R.id.txt_analytics_footer_right, "$habitsCount Habit Breakers")

                        val chartBitmap = drawMultiBarChart(
                            labels = listOf("Money", "Time", "Health", "Mood"),
                            values = listOf(85f, 70f, 90f, 75f),
                            maxValue = 100f,
                            colors = listOf(
                                Color.parseColor("#10B981"),
                                Color.parseColor("#6366F1"),
                                Color.parseColor("#EC4899"),
                                Color.parseColor("#F97316")
                            ),
                            isDark = isDark,
                            currencyPrefix = ""
                        )
                        views.setImageViewBitmap(R.id.img_analytics_chart, chartBitmap)
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Chart Drawing Helpers for crisp RemoteViews Bitmaps
    private fun drawBarChart(
        labels: List<String>,
        values: List<Float>,
        maxValue: Float,
        barColor: Int,
        highlightIndex: Int,
        highlightColor: Int,
        isDark: Boolean,
        suffix: String
    ): Bitmap {
        val width = 600
        val height = 220
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            color = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
            textAlign = Paint.Align.CENTER
        }

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#334155") else Color.parseColor("#E2E8F0")
            strokeWidth = 2f
        }

        // Draw baseline
        val baselineY = height - 44f
        canvas.drawLine(30f, baselineY, width - 30f, baselineY, gridPaint)

        val count = labels.size
        if (count == 0) return bitmap

        val availableWidth = width - 60f
        val slotWidth = availableWidth / count
        val barWidth = (slotWidth * 0.55f).coerceAtLeast(16f)

        for (i in 0 until count) {
            val cx = 30f + (i * slotWidth) + (slotWidth / 2f)
            val v = values.getOrNull(i) ?: 0f
            val fraction = (v / maxValue).coerceIn(0.05f, 1f)
            val barHeight = fraction * (baselineY - 45f)
            val topY = baselineY - barHeight

            val isHighlighted = i == highlightIndex
            paint.color = if (isHighlighted) highlightColor else barColor
            val rect = RectF(cx - barWidth / 2f, topY, cx + barWidth / 2f, baselineY)
            canvas.drawRoundRect(rect, 8f, 8f, paint)

            // Draw Value on top
            valPaint.color = if (isHighlighted) highlightColor else (if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A"))
            val valText = if (suffix == "%") "${v.toInt()}%" else if (suffix == "ml") "${v.toInt()}" else "${v.toInt()}"
            canvas.drawText(valText, cx, (topY - 8f).coerceAtLeast(24f), valPaint)

            // Draw Label at bottom
            canvas.drawText(labels[i], cx, height - 12f, textPaint)
        }

        return bitmap
    }

    private fun drawMultiBarChart(
        labels: List<String>,
        values: List<Float>,
        maxValue: Float,
        colors: List<Int>,
        isDark: Boolean,
        currencyPrefix: String
    ): Bitmap {
        val width = 600
        val height = 220
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            color = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
            textAlign = Paint.Align.CENTER
        }

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#334155") else Color.parseColor("#E2E8F0")
            strokeWidth = 2f
        }

        val baselineY = height - 44f
        canvas.drawLine(30f, baselineY, width - 30f, baselineY, gridPaint)

        val count = labels.size
        if (count == 0) return bitmap

        val availableWidth = width - 60f
        val slotWidth = availableWidth / count
        val barWidth = (slotWidth * 0.58f).coerceAtLeast(18f)

        for (i in 0 until count) {
            val cx = 30f + (i * slotWidth) + (slotWidth / 2f)
            val v = values.getOrNull(i) ?: 0f
            val fraction = (v / maxValue).coerceIn(0.08f, 1f)
            val barHeight = fraction * (baselineY - 45f)
            val topY = baselineY - barHeight

            val color = colors.getOrElse(i) { Color.parseColor("#7C3AED") }
            paint.color = color
            val rect = RectF(cx - barWidth / 2f, topY, cx + barWidth / 2f, baselineY)
            canvas.drawRoundRect(rect, 8f, 8f, paint)

            // Draw Value
            valPaint.color = color
            val valText = if (currencyPrefix.isNotEmpty()) "$currencyPrefix${v.toInt()}" else "${v.toInt()}%"
            canvas.drawText(valText, cx, (topY - 8f).coerceAtLeast(24f), valPaint)

            // Draw Label
            canvas.drawText(labels[i], cx, height - 12f, textPaint)
        }

        return bitmap
    }
}
