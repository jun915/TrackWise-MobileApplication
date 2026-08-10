package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.example.R
import com.example.MainActivity
import com.example.data.TrackWiseDatabase
import com.example.receiver.ReminderReceiver
import com.example.utils.TrackWiseUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TrackWiseWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private fun getActivityPendingIntent(context: Context, targetTab: String = "workspace"): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("target_tab", targetTab)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getActivity(context, targetTab.hashCode(), intent, flags)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
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

                // Fetch current active user or guest
                val sessionPrefs = context.getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
                val savedUserId = sessionPrefs.getString("saved_user_id", null)
                val userId = savedUserId ?: dao.getAllUsers().firstOrNull()?.id ?: "guest"

                // Theme Mode Setup
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

                val widgetBgRes = if (isDark) R.drawable.widget_background_dark else R.drawable.widget_background_light
                val textPrimaryColor = if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
                val textSecondaryColor = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
                val dividerColor = if (isDark) Color.parseColor("#334155") else Color.parseColor("#E2E8F0")

                val views = RemoteViews(context.packageName, R.layout.trackwise_widget_layout)

                // Apply overall widget background and dividers
                views.setInt(R.id.widget_root, "setBackgroundResource", widgetBgRes)
                views.setTextColor(R.id.widget_app_title, textPrimaryColor)
                views.setTextViewText(R.id.widget_app_title, "TrackWise")
                views.setInt(R.id.widget_divider, "setBackgroundColor", dividerColor)

                // Set Click Intents for each section to launch corresponding app tab
                views.setOnClickPendingIntent(R.id.widget_root, getActivityPendingIntent(context, "dashboard"))
                views.setOnClickPendingIntent(R.id.widget_app_title, getActivityPendingIntent(context, "dashboard"))
                views.setOnClickPendingIntent(R.id.widget_finance_balance, getActivityPendingIntent(context, "finance"))
                views.setOnClickPendingIntent(R.id.layout_events_container, getActivityPendingIntent(context, "occasions"))
                views.setOnClickPendingIntent(R.id.txt_events_header, getActivityPendingIntent(context, "occasions"))
                views.setOnClickPendingIntent(R.id.txt_event_1, getActivityPendingIntent(context, "occasions"))
                views.setOnClickPendingIntent(R.id.txt_event_2, getActivityPendingIntent(context, "occasions"))
                views.setOnClickPendingIntent(R.id.layout_habits_container, getActivityPendingIntent(context, "habits"))
                views.setOnClickPendingIntent(R.id.txt_habits_header, getActivityPendingIntent(context, "habits"))
                views.setOnClickPendingIntent(R.id.txt_habit_1, getActivityPendingIntent(context, "habits"))
                views.setOnClickPendingIntent(R.id.txt_habit_2, getActivityPendingIntent(context, "habits"))
                views.setOnClickPendingIntent(R.id.layout_tasks_container, getActivityPendingIntent(context, "tasks"))
                views.setOnClickPendingIntent(R.id.txt_tasks_header, getActivityPendingIntent(context, "tasks"))
                views.setOnClickPendingIntent(R.id.txt_task_1, getActivityPendingIntent(context, "tasks"))
                views.setOnClickPendingIntent(R.id.txt_task_2, getActivityPendingIntent(context, "tasks"))
                views.setOnClickPendingIntent(R.id.txt_water_stat, getActivityPendingIntent(context, "health"))
                views.setOnClickPendingIntent(R.id.txt_slipped_stat, getActivityPendingIntent(context, "habit_breaker"))
                views.setOnClickPendingIntent(R.id.widget_header_time_date, getActivityPendingIntent(context, "calendar"))
                views.setOnClickPendingIntent(R.id.widget_header_urdu_allah, getActivityPendingIntent(context, "hijri_calendar"))

                // --- 1. Top Right: Small Date & Time + Urdu Date + Today's Allah Name ---
                val now = Date()
                val calendar = Calendar.getInstance()
                val displayDateStr = SimpleDateFormat("EEE, MMM d • hh:mm a", Locale.US).format(now)
                views.setTextViewText(R.id.widget_header_time_date, displayDateStr)
                views.setTextColor(R.id.widget_header_time_date, textSecondaryColor)

                val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
                val allahNameObj = TrackWiseUtils.ALLAH_NAMES[(dayOfYear) % 99]
                val allahNameStr = "ﷲ ${allahNameObj.transliteration} (${allahNameObj.arabic})"

                val hijriDateStr = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val hijrah = java.time.chrono.HijrahDate.now()
                        val day = hijrah.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
                        val monthIdx = hijrah.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
                        val year = hijrah.get(java.time.temporal.ChronoField.YEAR)
                        val hijriMonths = arrayOf("", "Muharram", "Safar", "Rabi' I", "Rabi' II", "Jumada I", "Jumada II", "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah")
                        "$day ${hijriMonths.getOrElse(monthIdx) { "Safar" }} $year AH"
                    } else {
                        "23 Safar 1448 AH"
                    }
                } catch (e: Exception) {
                    "23 Safar 1448 AH"
                }

                views.setTextViewText(R.id.widget_header_urdu_allah, "$hijriDateStr • $allahNameStr")

                // --- 2. Net Balance from Monthly Finance Summary ---
                val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(now)
                val logs = dao.getFinanceLogsForUser(userId)
                val monthLogs = logs.filter { it.date.startsWith(currentMonthStr) }
                val income = monthLogs.filter { it.type == "income" }.sumOf { it.amount }
                val expense = monthLogs.filter { it.type == "expense" }.sumOf { it.amount }
                val savings = monthLogs.filter { it.type == "savings" }.sumOf { it.amount }
                val netBalance = income - (expense + savings)

                views.setTextViewText(R.id.widget_finance_balance, "💰 Net Balance: ₹${netBalance.toInt()}")

                // --- 3. Max 2 Countdown Events Today (GONE if no events today) ---
                val todayStr = TrackWiseUtils.getTodayString().take(10)
                val birthdays = dao.getBirthdaysForUserFlow(userId).first()
                val todayBirthdays = birthdays.filter { it.date.endsWith(todayStr.substring(5)) }.map { "🎉 ${it.name}'s Birthday" }
                val todayFestivals = TrackWiseUtils.getIndianFestivalsForDate(todayStr).map { "🎆 $it" }
                val todayEvents = (todayBirthdays + todayFestivals)

                if (todayEvents.isEmpty()) {
                    views.setViewVisibility(R.id.layout_events_container, View.GONE)
                } else {
                    views.setViewVisibility(R.id.layout_events_container, View.VISIBLE)
                    views.setTextViewText(R.id.txt_events_header, "🎉 Today's Countdown Events")
                    views.setTextColor(R.id.txt_events_header, textPrimaryColor)

                    views.setTextViewText(R.id.txt_event_1, "• " + todayEvents[0])
                    views.setTextColor(R.id.txt_event_1, textSecondaryColor)

                    if (todayEvents.size >= 2) {
                        views.setViewVisibility(R.id.txt_event_2, View.VISIBLE)
                        views.setTextViewText(R.id.txt_event_2, "• " + todayEvents[1])
                        views.setTextColor(R.id.txt_event_2, textSecondaryColor)
                    } else {
                        views.setViewVisibility(R.id.txt_event_2, View.GONE)
                    }
                }

                // --- 4. Max 2 Habits for Today / Current Hour ---
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentHourStr = String.format(Locale.US, "%02d", currentHour)
                val allHabits = dao.getHabitsForUser(userId).filter { TrackWiseUtils.shouldShowHabitOnDate(it, todayStr) }
                
                val currentHourHabits = allHabits.filter { habit ->
                    val t24 = ReminderReceiver.parseTo24HourTime(habit.reminderTime)
                    t24 != null && t24.take(2) == currentHourStr
                }.ifEmpty { 
                    allHabits.sortedBy { ReminderReceiver.parseTo24HourTime(it.reminderTime) ?: "23:59" } 
                }.take(2)

                views.setTextViewText(R.id.txt_habits_header, "💪 Habits (${currentHourHabits.size} for today)")
                views.setTextColor(R.id.txt_habits_header, textPrimaryColor)

                if (currentHourHabits.isNotEmpty()) {
                    val habit1 = currentHourHabits[0]
                    val isDone1 = habit1.daysCompletedJson.contains(todayStr)
                    val timeDisp1 = if (!habit1.reminderTime.isNullOrBlank()) " @ ${habit1.reminderTime}" else ""
                    views.setTextViewText(R.id.txt_habit_1, "• ${habit1.name}$timeDisp1 (${if (isDone1) "Done" else "Pending"})")
                    views.setTextColor(R.id.txt_habit_1, textSecondaryColor)

                    if (currentHourHabits.size >= 2) {
                        val habit2 = currentHourHabits[1]
                        val isDone2 = habit2.daysCompletedJson.contains(todayStr)
                        val timeDisp2 = if (!habit2.reminderTime.isNullOrBlank()) " @ ${habit2.reminderTime}" else ""
                        views.setViewVisibility(R.id.txt_habit_2, View.VISIBLE)
                        views.setTextViewText(R.id.txt_habit_2, "• ${habit2.name}$timeDisp2 (${if (isDone2) "Done" else "Pending"})")
                        views.setTextColor(R.id.txt_habit_2, textSecondaryColor)
                    } else {
                        views.setViewVisibility(R.id.txt_habit_2, View.GONE)
                    }
                } else {
                    views.setTextViewText(R.id.txt_habit_1, "• No habits scheduled for today")
                    views.setTextColor(R.id.txt_habit_1, textSecondaryColor)
                    views.setViewVisibility(R.id.txt_habit_2, View.GONE)
                }

                // --- 5. Max 2 Tasks for Today / Current Hour ---
                val allTasks = dao.getTasksForUser(userId).filter { TrackWiseUtils.shouldShowTaskOnDate(it, todayStr) }
                val currentHourTasks = allTasks.filter { task ->
                    val t24 = ReminderReceiver.parseTo24HourTime(task.reminderTime ?: task.dueTime)
                    t24 != null && t24.take(2) == currentHourStr
                }.ifEmpty { 
                    allTasks.filter { !it.completed }.sortedBy { ReminderReceiver.parseTo24HourTime(it.reminderTime ?: it.dueTime) ?: "23:59" } 
                }.take(2)

                views.setTextViewText(R.id.txt_tasks_header, "📝 Tasks (${currentHourTasks.size} pending)")
                views.setTextColor(R.id.txt_tasks_header, textPrimaryColor)

                if (currentHourTasks.isNotEmpty()) {
                    val task1 = currentHourTasks[0]
                    val timeDisp1 = if (!task1.reminderTime.isNullOrBlank()) " @ ${task1.reminderTime}" else if (!task1.dueTime.isNullOrBlank()) " @ ${task1.dueTime}" else ""
                    views.setTextViewText(R.id.txt_task_1, "• ${task1.title}$timeDisp1 (${if (task1.completed) "Done" else "Pending"})")
                    views.setTextColor(R.id.txt_task_1, textSecondaryColor)

                    if (currentHourTasks.size >= 2) {
                        val task2 = currentHourTasks[1]
                        val timeDisp2 = if (!task2.reminderTime.isNullOrBlank()) " @ ${task2.reminderTime}" else if (!task2.dueTime.isNullOrBlank()) " @ ${task2.dueTime}" else ""
                        views.setViewVisibility(R.id.txt_task_2, View.VISIBLE)
                        views.setTextViewText(R.id.txt_task_2, "• ${task2.title}$timeDisp2 (${if (task2.completed) "Done" else "Pending"})")
                        views.setTextColor(R.id.txt_task_2, textSecondaryColor)
                    } else {
                        views.setViewVisibility(R.id.txt_task_2, View.GONE)
                    }
                } else {
                    views.setTextViewText(R.id.txt_task_1, "• No task reminders this hour")
                    views.setTextColor(R.id.txt_task_1, textSecondaryColor)
                    views.setViewVisibility(R.id.txt_task_2, View.GONE)
                }

                // --- 6. Water Intake Today (Icon + Number) ---
                val waterLogs = dao.getWaterLogsForUser(userId)
                val todayWater = waterLogs.find { it.date == todayStr }
                val glasses = todayWater?.glasses ?: 0
                val goal = todayWater?.goal ?: 8
                views.setTextViewText(R.id.txt_water_stat, "💧 $glasses / $goal glasses")

                // --- 7. Most Slipped Up Habit ---
                val badHabitsJson = sessionPrefs.getString("bad_habits_json_v2", null)
                var mostSlippedName = "None"
                var maxSlips = 0

                if (!badHabitsJson.isNullOrBlank()) {
                    try {
                        val arr = org.json.JSONArray(badHabitsJson)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val name = obj.optString("name", "")
                            val logsArr = obj.optJSONArray("logs")
                            val slips = logsArr?.length() ?: 0
                            if (slips > maxSlips) {
                                maxSlips = slips
                                mostSlippedName = name
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (maxSlips > 0) {
                    views.setTextViewText(R.id.txt_slipped_stat, "⚠️ Most Slipped: $mostSlippedName (${maxSlips}x)")
                } else {
                    views.setTextViewText(R.id.txt_slipped_stat, "⚠️ Most Slipped: None")
                }

                // --- 8. Footer Timestamp ---
                val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(now)
                views.setTextViewText(R.id.widget_footer_text, "Last updated: $timeStr")
                views.setTextColor(R.id.widget_footer_text, textSecondaryColor)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
