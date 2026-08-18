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
        scheduleNextUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE || intent.action == "com.example.widget.ACTION_WIDGET_AUTOREFRESH") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: appWidgetManager.getAppWidgetIds(ComponentName(context, TrackWiseWidgetProvider::class.java))
            for (id in appWidgetIds) {
                updateWidget(context, appWidgetManager, id)
            }
            scheduleNextUpdate(context)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleNextUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelNextUpdate(context)
    }

    private fun scheduleNextUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, TrackWiseWidgetProvider::class.java).apply {
            action = "com.example.widget.ACTION_WIDGET_AUTOREFRESH"
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 202608, intent, flags)

        // 15 minutes from now (900_000 ms)
        val triggerTime = System.currentTimeMillis() + 900_000L

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC, triggerTime, pendingIntent)
            } else {
                alarmManager.set(android.app.AlarmManager.RTC, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelNextUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, TrackWiseWidgetProvider::class.java).apply {
            action = "com.example.widget.ACTION_WIDGET_AUTOREFRESH"
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 202608, intent, flags)
        try {
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
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
                val todayStr = TrackWiseUtils.getTodayString()
                val displayDateStr = SimpleDateFormat("EEE, MMM d • hh:mm a", Locale.US).format(now)
                views.setTextViewText(R.id.widget_header_time_date, displayDateStr)
                views.setTextColor(R.id.widget_header_time_date, textSecondaryColor)

                val allahNameObj = TrackWiseUtils.getAllahNameForDate(todayStr)
                val allahNameStr = "ﷲ ${allahNameObj.transliteration} (${allahNameObj.arabic})"

                val hijriInfo = TrackWiseUtils.getHijriInfo(todayStr)
                val hijriDateStr = "${hijriInfo.day} ${hijriInfo.monthNameUr} ${hijriInfo.year} AH"

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

                // --- 3. Occasions & Countdowns Section (Always Visible with real-time countdown) ---
                val birthdays = dao.getBirthdaysForUser(userId).ifEmpty { dao.getAllBirthdays() }
                val upcomingOccasions = birthdays.map { bday ->
                    val daysLeft = calculateOccasionDays(bday)
                    val prefix = when {
                        bday.category.contains("Birthday", ignoreCase = true) -> "🎂"
                        bday.category.contains("Anniversary", ignoreCase = true) -> "💍"
                        bday.category.contains("Holiday", ignoreCase = true) -> "🎆"
                        else -> "🎯"
                    }
                    val badgeStr = when (daysLeft) {
                        0 -> "TODAY! 🎉"
                        1 -> "Tomorrow ⏰"
                        in 2..4 -> "In $daysLeft days ⏳"
                        else -> "In $daysLeft days 📅"
                    }
                    Triple(bday, daysLeft, "$prefix ${bday.name} • $badgeStr")
                }.filter { it.second in 0..998 }
                .sortedBy { it.second }

                val todayFestivals = TrackWiseUtils.getIndianFestivalsForDate(todayStr).map { "🎆 $it • TODAY!" }
                val allDisplayEvents = mutableListOf<String>()
                allDisplayEvents.addAll(todayFestivals)
                upcomingOccasions.forEach { allDisplayEvents.add(it.third) }

                views.setViewVisibility(R.id.layout_events_container, View.VISIBLE)
                views.setTextViewText(R.id.txt_events_header, "🎉 Occasions & Countdowns (${allDisplayEvents.size})")
                views.setTextColor(R.id.txt_events_header, textPrimaryColor)

                if (allDisplayEvents.isNotEmpty()) {
                    views.setTextViewText(R.id.txt_event_1, "• " + allDisplayEvents[0])
                    views.setTextColor(R.id.txt_event_1, textSecondaryColor)

                    if (allDisplayEvents.size >= 2) {
                        views.setViewVisibility(R.id.txt_event_2, View.VISIBLE)
                        views.setTextViewText(R.id.txt_event_2, "• " + allDisplayEvents[1])
                        views.setTextColor(R.id.txt_event_2, textSecondaryColor)
                    } else {
                        views.setViewVisibility(R.id.txt_event_2, View.GONE)
                    }
                } else {
                    views.setTextViewText(R.id.txt_event_1, "• No upcoming occasions (Tap to add)")
                    views.setTextColor(R.id.txt_event_1, textSecondaryColor)
                    views.setViewVisibility(R.id.txt_event_2, View.GONE)
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

    private fun daysUntilBirthday(storedDate: String): Int {
        val parts = storedDate.split("-")
        val (month, day) = if (parts.size == 3) {
            Pair(parts[1].toIntOrNull() ?: 1, parts[2].toIntOrNull() ?: 1)
        } else if (parts.size == 2) {
            Pair(parts[0].toIntOrNull() ?: 1, parts[1].toIntOrNull() ?: 1)
        } else {
            return 999
        }

        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val bdayThisYear = Calendar.getInstance()
        bdayThisYear.set(Calendar.YEAR, today.get(Calendar.YEAR))
        bdayThisYear.set(Calendar.MONTH, month - 1)
        bdayThisYear.set(Calendar.DAY_OF_MONTH, day)
        bdayThisYear.set(Calendar.HOUR_OF_DAY, 0)
        bdayThisYear.set(Calendar.MINUTE, 0)
        bdayThisYear.set(Calendar.SECOND, 0)
        bdayThisYear.set(Calendar.MILLISECOND, 0)

        if (bdayThisYear.timeInMillis == today.timeInMillis) {
            return 0
        }

        if (bdayThisYear.before(today)) {
            val bdayNextYear = Calendar.getInstance()
            bdayNextYear.set(Calendar.YEAR, today.get(Calendar.YEAR) + 1)
            bdayNextYear.set(Calendar.MONTH, month - 1)
            bdayNextYear.set(Calendar.DAY_OF_MONTH, day)
            bdayNextYear.set(Calendar.HOUR_OF_DAY, 0)
            bdayNextYear.set(Calendar.MINUTE, 0)
            bdayNextYear.set(Calendar.SECOND, 0)
            bdayNextYear.set(Calendar.MILLISECOND, 0)
            
            val diffMs = bdayNextYear.timeInMillis - today.timeInMillis
            return (diffMs / (1000 * 60 * 60 * 24)).toInt()
        } else {
            val diffMs = bdayThisYear.timeInMillis - today.timeInMillis
            return (diffMs / (1000 * 60 * 60 * 24)).toInt()
        }
    }

    private fun calculateOccasionDays(bday: com.example.data.BirthdayEntity): Int {
        val dateStr = bday.date
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val year = parts[0].toIntOrNull() ?: 2000
            val month = parts[1].toIntOrNull() ?: 1
            val day = parts[2].toIntOrNull() ?: 1

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val eventDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (bday.countingMode == "Count Up") {
                return 999
            }

            // Single-target countdown or holiday event
            if (bday.category.contains("Countdown", ignoreCase = true) || bday.category.contains("Holiday", ignoreCase = true)) {
                if (eventDate.timeInMillis == today.timeInMillis) {
                    return 0
                } else if (eventDate.after(today)) {
                    val diffMs = eventDate.timeInMillis - today.timeInMillis
                    return Math.round(diffMs.toDouble() / (1000 * 60 * 60 * 24)).toInt()
                } else {
                    return 999
                }
            }

            // Recurring events like Birthdays and Anniversaries
            val bdayThisYear = Calendar.getInstance().apply {
                set(Calendar.YEAR, today.get(Calendar.YEAR))
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (bdayThisYear.timeInMillis == today.timeInMillis) {
                return 0
            } else if (bdayThisYear.after(today)) {
                val diffMs = bdayThisYear.timeInMillis - today.timeInMillis
                return Math.round(diffMs.toDouble() / (1000 * 60 * 60 * 24)).toInt()
            } else {
                val bdayNextYear = Calendar.getInstance().apply {
                    set(Calendar.YEAR, today.get(Calendar.YEAR) + 1)
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val diffMs = bdayNextYear.timeInMillis - today.timeInMillis
                return Math.round(diffMs.toDouble() / (1000 * 60 * 60 * 24)).toInt()
            }
        }
        return daysUntilBirthday(bday.date)
    }
}
