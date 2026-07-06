package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.widget.RemoteViews
import com.example.R
import com.example.data.TrackWiseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackWiseWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        const val ACTION_TAB_CLICK = "com.example.widget.ACTION_TAB_CLICK"
        const val EXTRA_CATEGORY = "extra_category"

        private fun getPendingIntent(context: Context, category: String, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, TrackWiseWidgetProvider::class.java).apply {
                action = ACTION_TAB_CLICK
                putExtra(EXTRA_CATEGORY, category)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(
                context,
                category.hashCode() + appWidgetId,
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
        if (intent.action == ACTION_TAB_CLICK) {
            val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "finance"
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

            // Save selected category in preferences
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                prefs.edit().putString("selected_category_$appWidgetId", category).apply()
                // Update specific widget
                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateWidget(context, appWidgetManager, appWidgetId)
            } else {
                // Fallback for global update
                prefs.edit().putString("selected_category", category).apply()
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TrackWiseWidgetProvider::class.java))
                for (id in appWidgetIds) {
                    updateWidget(context, appWidgetManager, id)
                }
            }
        } else if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: appWidgetManager.getAppWidgetIds(ComponentName(context, TrackWiseWidgetProvider::class.java))
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

                // Fetch current user or default from session preferences
                val sessionPrefs = context.getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
                val savedUserId = sessionPrefs.getString("saved_user_id", null)
                val userId = savedUserId ?: dao.getAllUsers().firstOrNull()?.id ?: "guest"

                // Get stored selection for this widget
                val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                val selectedCategory = prefs.getString("selected_category_$appWidgetId", null)
                    ?: prefs.getString("selected_category", "finance") ?: "finance"

                // Fetch session theme preferences from trackwise_session SharedPrefs
                val themeMode = sessionPrefs.getString("saved_theme_mode", "light") ?: "light"
                val themeAccent = sessionPrefs.getString("saved_theme_accent", "Default Violet") ?: "Default Violet"

                val isDark = themeMode == "dark"

                // Color configuration matching the selected theme mode
                val widgetBgRes = if (isDark) R.drawable.widget_background_dark else R.drawable.widget_background_light
                val textPrimaryColor = if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
                val textSecondaryColor = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
                val dividerColor = if (isDark) Color.parseColor("#334155") else Color.parseColor("#E2E8F0")

                // Accent color configuration matching themeAccent
                val accentColorHex = when (themeAccent) {
                    "Ocean Blue" -> "#0EA5E9"
                    "Forest Green" -> "#10B981"
                    "Sunset Orange" -> "#F97316"
                    "Crimson Red" -> "#F43F5E"
                    else -> "#7C3AED" // Default Violet
                }
                val accentColor = Color.parseColor(accentColorHex)

                val views = RemoteViews(context.packageName, R.layout.trackwise_widget_layout)

                // Apply overall widget theming colors dynamically
                views.setInt(R.id.widget_root, "setBackgroundResource", widgetBgRes)
                views.setTextColor(R.id.widget_app_title, textPrimaryColor)
                views.setInt(R.id.widget_divider, "setBackgroundColor", dividerColor)

                // Setup interactive pending intents for buttons
                views.setOnClickPendingIntent(R.id.btn_tab_finance, getPendingIntent(context, "finance", appWidgetId))
                views.setOnClickPendingIntent(R.id.btn_tab_health, getPendingIntent(context, "health", appWidgetId))
                views.setOnClickPendingIntent(R.id.btn_tab_tasks, getPendingIntent(context, "tasks", appWidgetId))

                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                // Configure RemoteViews states based on selection
                when (selectedCategory) {
                    "finance" -> {
                        views.setInt(R.id.btn_tab_finance, "setBackgroundResource", R.drawable.widget_tab_active)
                        views.setTextColor(R.id.btn_tab_finance, Color.WHITE)

                        views.setInt(R.id.btn_tab_health, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_health, textSecondaryColor)

                        views.setInt(R.id.btn_tab_tasks, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_tasks, textSecondaryColor)

                        // Get finance stats
                        val logs = dao.getFinanceLogsForUser(userId)
                        val income = logs.filter { it.type == "income" }.sumOf { it.amount }
                        val expense = logs.filter { it.type == "expense" }.sumOf { it.amount }
                        val savings = logs.filter { it.type == "savings" }.sumOf { it.amount }
                        val balance = income - (expense + savings)

                        val spentToday = logs.filter { it.date == todayStr && it.type == "expense" }.sumOf { it.amount }

                        val profile = dao.getUserProfile(userId)
                        val dailyTarget = profile?.financeDailyTarget ?: 1000.0
                        val leftToSpend = dailyTarget - spentToday

                        views.setTextViewText(R.id.txt_analytics_title, "Finance Insights 💰")
                        views.setTextColor(R.id.txt_analytics_title, accentColor)

                        views.setTextViewText(R.id.txt_metric1_label, "Available Balance:")
                        views.setTextColor(R.id.txt_metric1_label, textSecondaryColor)
                        views.setTextViewText(R.id.txt_metric1_value, "₹${String.format("%.1f", balance)}")
                        views.setTextColor(R.id.txt_metric1_value, textPrimaryColor)

                        views.setTextViewText(R.id.txt_metric2_label, "Limit Buffer:")
                        views.setTextColor(R.id.txt_metric2_label, textSecondaryColor)
                        val budgetStatusText = if (leftToSpend >= 0) {
                            "₹${String.format("%.1f", leftToSpend)} left"
                        } else {
                            "₹${String.format("%.1f", -leftToSpend)} over!"
                        }
                        views.setTextViewText(R.id.txt_metric2_value, budgetStatusText)
                        views.setTextColor(R.id.txt_metric2_value, if (leftToSpend < 0) Color.parseColor("#EF4444") else textPrimaryColor)

                        // Progress indicator for finance spending
                        val progress = if (dailyTarget > 0) {
                            ((spentToday / dailyTarget) * 100).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            views.setColorStateList(R.id.widget_progress_bar, "setProgressTintList", ColorStateList.valueOf(accentColor))
                        }
                    }
                    "health" -> {
                        views.setInt(R.id.btn_tab_finance, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_finance, textSecondaryColor)

                        views.setInt(R.id.btn_tab_health, "setBackgroundResource", R.drawable.widget_tab_active)
                        views.setTextColor(R.id.btn_tab_health, Color.WHITE)

                        views.setInt(R.id.btn_tab_tasks, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_tasks, textSecondaryColor)

                        // Get health stats
                        val waterLogs = dao.getWaterLogsForUser(userId)
                        val todayWaterEntry = waterLogs.find { it.date == todayStr }
                        val todayGlasses = todayWaterEntry?.glasses ?: 0
                        val targetGlasses = todayWaterEntry?.goal ?: 8

                        val exerciseLogs = dao.getExerciseLogsForUser(userId)
                        val todayExercise = exerciseLogs.filter { it.date == todayStr }.sumOf { it.durationMinutes }

                        views.setTextViewText(R.id.txt_analytics_title, "Health Insights 💧")
                        views.setTextColor(R.id.txt_analytics_title, accentColor)

                        views.setTextViewText(R.id.txt_metric1_label, "Water Drunk Today:")
                        views.setTextColor(R.id.txt_metric1_label, textSecondaryColor)
                        views.setTextViewText(R.id.txt_metric1_value, "$todayGlasses / $targetGlasses Glasses")
                        views.setTextColor(R.id.txt_metric1_value, textPrimaryColor)

                        views.setTextViewText(R.id.txt_metric2_label, "Active Exercise:")
                        views.setTextColor(R.id.txt_metric2_label, textSecondaryColor)
                        val excText = if (todayExercise > 0) "$todayExercise mins logged" else "None logged yet"
                        views.setTextViewText(R.id.txt_metric2_value, excText)
                        views.setTextColor(R.id.txt_metric2_value, if (todayExercise > 0) Color.parseColor("#10B981") else textPrimaryColor)

                        // Progress based on water target glasses
                        val progress = if (targetGlasses > 0) {
                            ((todayGlasses.toDouble() / targetGlasses) * 100).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            views.setColorStateList(R.id.widget_progress_bar, "setProgressTintList", ColorStateList.valueOf(accentColor))
                        }
                    }
                    "tasks" -> {
                        views.setInt(R.id.btn_tab_finance, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_finance, textSecondaryColor)

                        views.setInt(R.id.btn_tab_health, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_health, textSecondaryColor)

                        views.setInt(R.id.btn_tab_tasks, "setBackgroundResource", R.drawable.widget_tab_active)
                        views.setTextColor(R.id.btn_tab_tasks, Color.WHITE)

                        // Get task stats
                        val tasks = dao.getTasksForUser(userId)
                        val todayTasks = tasks.filter { com.example.utils.TrackWiseUtils.shouldShowTaskOnDate(it, todayStr) }
                        val completedToday = todayTasks.count { it.completed }
                        val totalToday = todayTasks.size
                        val pendingToday = totalToday - completedToday

                        views.setTextViewText(R.id.txt_analytics_title, "Task Insights ✅")
                        views.setTextColor(R.id.txt_analytics_title, accentColor)

                        views.setTextViewText(R.id.txt_metric1_label, "Today's Progress:")
                        views.setTextColor(R.id.txt_metric1_label, textSecondaryColor)
                        views.setTextViewText(R.id.txt_metric1_value, "$completedToday / $totalToday Tasks done")
                        views.setTextColor(R.id.txt_metric1_value, textPrimaryColor)

                        views.setTextViewText(R.id.txt_metric2_label, "Action Item:")
                        views.setTextColor(R.id.txt_metric2_label, textSecondaryColor)
                        val taskStatusText = if (pendingToday > 0) "$pendingToday tasks remaining" else "All tasks completed! 🎉"
                        views.setTextViewText(R.id.txt_metric2_value, taskStatusText)
                        views.setTextColor(R.id.txt_metric2_value, if (pendingToday > 0) Color.parseColor("#F59E0B") else Color.parseColor("#10B981"))

                        // Progress based on completed tasks percentage today
                        val progress = if (totalToday > 0) {
                            ((completedToday.toDouble() / totalToday) * 100).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            views.setColorStateList(R.id.widget_progress_bar, "setProgressTintList", ColorStateList.valueOf(accentColor))
                        }
                    }
                }

                // Query and render the live beautiful Daily Progress Pie Chart on the right
                val allTasks = dao.getTasksForUser(userId)
                val todayTasks = allTasks.filter { com.example.utils.TrackWiseUtils.shouldShowTaskOnDate(it, todayStr) }
                val completedTasksToday = todayTasks.count { it.completed }
                val totalTasksToday = todayTasks.size

                val habits = dao.getHabitsForUser(userId)
                val filteredHabits = habits.filter { com.example.utils.TrackWiseUtils.shouldShowHabitOnDate(it, todayStr) }
                val completedHabitsToday = filteredHabits.count { it.daysCompletedJson.contains(todayStr) }
                val totalHabitsToday = filteredHabits.size

                val totalCompleted = completedTasksToday + completedHabitsToday
                val totalScheduled = totalTasksToday + totalHabitsToday

                val pieChartBitmap = drawPieChart(totalCompleted, totalScheduled, accentColor, isDark)
                views.setImageViewBitmap(R.id.img_pie_chart, pieChartBitmap)

                // Update footer with timestamp
                val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
                views.setTextViewText(R.id.widget_footer_text, "Last updated: $timeStr")
                views.setTextColor(R.id.widget_footer_text, textSecondaryColor)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun drawPieChart(completed: Int, total: Int, accentColor: Int, isDark: Boolean): Bitmap {
        val size = 144 // 144x144 pixels canvas
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw transparent background
        val bgPaint = Paint().apply {
            color = Color.TRANSPARENT
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        val strokeW = 14f
        val rectF = RectF(strokeW, strokeW, size.toFloat() - strokeW, size.toFloat() - strokeW)

        // 1. Draw track circle (subdued grey)
        val trackPaint = Paint().apply {
            color = if (isDark) Color.parseColor("#334155") else Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(rectF, 0f, 360f, false, trackPaint)

        // 2. Draw progress arc (active theme accent color)
        val percentage = if (total > 0) completed.toFloat() / total else 0f
        if (percentage > 0f) {
            val progressPaint = Paint().apply {
                color = accentColor
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawArc(rectF, -90f, percentage * 360f, false, progressPaint)
        }

        // 3. Draw text showing percentage inside the pie/ring
        val textPaint = Paint().apply {
            color = if (isDark) Color.WHITE else Color.parseColor("#0F172A")
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val percentageText = if (total > 0) "${(percentage * 100).toInt()}%" else "0%"
        val textY = (size / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(percentageText, (size / 2).toFloat(), textY, textPaint)

        return bitmap
    }
}
