package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        scope.launch {
            try {
                val db = TrackWiseDatabase.getDatabase(context)
                val dao = db.trackWiseDao()

                // Fetch current user or default
                val users = dao.getAllUsers()
                val userId = users.firstOrNull()?.id ?: "guest"

                // Get stored selection for this widget
                val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                val selectedCategory = prefs.getString("selected_category_$appWidgetId", null)
                    ?: prefs.getString("selected_category", "finance") ?: "finance"

                val views = RemoteViews(context.packageName, R.layout.trackwise_widget_layout)

                // Setup interactive pending intents for buttons
                views.setOnClickPendingIntent(R.id.btn_tab_finance, getPendingIntent(context, "finance", appWidgetId))
                views.setOnClickPendingIntent(R.id.btn_tab_health, getPendingIntent(context, "health", appWidgetId))
                views.setOnClickPendingIntent(R.id.btn_tab_tasks, getPendingIntent(context, "tasks", appWidgetId))

                // Configure RemoteViews states based on selection
                when (selectedCategory) {
                    "finance" -> {
                        views.setInt(R.id.btn_tab_finance, "setBackgroundResource", R.drawable.widget_tab_active)
                        views.setTextColor(R.id.btn_tab_finance, Color.WHITE)

                        views.setInt(R.id.btn_tab_health, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_health, Color.parseColor("#CBD5E1"))

                        views.setInt(R.id.btn_tab_tasks, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_tasks, Color.parseColor("#CBD5E1"))

                        // Get finance stats
                        val logs = dao.getFinanceLogsForUser(userId)
                        val income = logs.filter { it.type == "income" }.sumOf { it.amount }
                        val expense = logs.filter { it.type == "expense" }.sumOf { it.amount }
                        val savings = logs.filter { it.type == "savings" }.sumOf { it.amount }
                        val balance = income - (expense + savings)

                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        val spentToday = logs.filter { it.date == todayStr && it.type == "expense" }.sumOf { it.amount }

                        val profile = dao.getUserProfile(userId)
                        val dailyTarget = profile?.financeDailyTarget ?: 1000.0

                        views.setTextViewText(R.id.txt_analytics_title, "Finance Summary 💰")
                        views.setTextViewText(R.id.txt_metric1_label, "Cumulative Balance:")
                        views.setTextViewText(R.id.txt_metric1_value, "₹${String.format("%.2f", balance)}")
                        views.setTextViewText(R.id.txt_metric2_label, "Spent Today:")
                        views.setTextViewText(R.id.txt_metric2_value, "₹${String.format("%.2f", spentToday)}")

                        // Progress indicator for finance spending
                        val progress = if (dailyTarget > 0) {
                            ((spentToday / dailyTarget) * 100).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)
                    }
                    "health" -> {
                        views.setInt(R.id.btn_tab_finance, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_finance, Color.parseColor("#CBD5E1"))

                        views.setInt(R.id.btn_tab_health, "setBackgroundResource", R.drawable.widget_tab_active)
                        views.setTextColor(R.id.btn_tab_health, Color.WHITE)

                        views.setInt(R.id.btn_tab_tasks, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_tasks, Color.parseColor("#CBD5E1"))

                        // Get health stats
                        val waterLogs = dao.getWaterLogsForUser(userId)
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        val todayWaterEntry = waterLogs.find { it.date == todayStr }
                        val todayGlasses = todayWaterEntry?.glasses ?: 0
                        val targetGlasses = todayWaterEntry?.goal ?: 8

                        val exerciseLogs = dao.getExerciseLogsForUser(userId)
                        val todayExercise = exerciseLogs.filter { it.date == todayStr }.sumOf { it.durationMinutes }

                        views.setTextViewText(R.id.txt_analytics_title, "Health Tracker 💧")
                        views.setTextViewText(R.id.txt_metric1_label, "Water Hydration:")
                        views.setTextViewText(R.id.txt_metric1_value, "$todayGlasses of $targetGlasses glasses")
                        views.setTextViewText(R.id.txt_metric2_label, "Exercise Duration:")
                        views.setTextViewText(R.id.txt_metric2_value, "$todayExercise mins")

                        // Progress based on water target glasses
                        val progress = if (targetGlasses > 0) {
                            ((todayGlasses.toDouble() / targetGlasses) * 100).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)
                    }
                    "tasks" -> {
                        views.setInt(R.id.btn_tab_finance, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_finance, Color.parseColor("#CBD5E1"))

                        views.setInt(R.id.btn_tab_health, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_health, Color.parseColor("#CBD5E1"))

                        views.setInt(R.id.btn_tab_tasks, "setBackgroundResource", R.drawable.widget_tab_active)
                        views.setTextColor(R.id.btn_tab_tasks, Color.WHITE)

                        // Get task stats
                        val tasks = dao.getTasksForUser(userId)
                        val completedCount = tasks.count { it.completed }
                        val totalCount = tasks.size

                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        val todayTasks = tasks.filter { it.deadline == todayStr }
                        val completedToday = todayTasks.count { it.completed }
                        val totalToday = todayTasks.size

                        views.setTextViewText(R.id.txt_analytics_title, "Task Deadlines ✅")
                        views.setTextViewText(R.id.txt_metric1_label, "All-time Completion:")
                        views.setTextViewText(R.id.txt_metric1_value, "$completedCount of $totalCount done")
                        views.setTextViewText(R.id.txt_metric2_label, "Scheduled Today:")
                        views.setTextViewText(R.id.txt_metric2_value, "$completedToday of $totalToday done")

                        // Progress based on completed tasks percentage
                        val progress = if (totalCount > 0) {
                            ((completedCount.toDouble() / totalCount) * 100).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)
                    }
                }

                // Update footer with timestamp
                val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
                views.setTextViewText(R.id.widget_footer_text, "Last updated: $timeStr")

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
