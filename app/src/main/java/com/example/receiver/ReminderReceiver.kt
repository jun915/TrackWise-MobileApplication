package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.TrackWiseDatabase
import com.example.utils.TrackWiseUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            scheduleBackgroundReminderAlarm(context)
            return
        }

        val notificationId = intent.getIntExtra("notification_id", -1)
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (action == "com.example.action.NOTIFICATION_DISMISS") {
            if (notificationId != -1) {
                notifManager.cancel(notificationId)
            }
            return
        }

        if (action == "com.example.action.NOTIFICATION_SNOOZE") {
            if (notificationId != -1) {
                notifManager.cancel(notificationId)
            }
            val title = intent.getStringExtra("title") ?: "Snoozed Reminder"
            val message = intent.getStringExtra("message") ?: "Snooze elapsed!"
            val taskId = intent.getStringExtra("task_id")
            
            // Update task reminderTime and reminderDate in database
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = TrackWiseDatabase.getDatabase(context)
                    val dao = database.trackWiseDao()
                    
                    val sharedPrefs = context.getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
                    val userId = sharedPrefs.getString("saved_user_id", null) ?: run {
                        val users = dao.getAllUsers()
                        users.firstOrNull()?.id
                    }
                    
                    if (userId != null && taskId != null) {
                        val task = dao.getTasksForUser(userId).firstOrNull { it.id == taskId }
                        if (task != null) {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.MINUTE, 5)
                            val newTimeStr = SimpleDateFormat("HH:mm", Locale.US).format(cal.time)
                            val newDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                            
                            val updatedTask = task.copy(
                                reminderDate = newDateStr,
                                reminderTime = newTimeStr,
                                remindMe = true
                            )
                            dao.insertTask(updatedTask)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
                setAction("com.example.action.TRIGGER_SNOOZE")
                putExtra("title", title)
                putExtra("message", message)
                putExtra("notification_id", notificationId)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_ONE_SHOT
            }
            val pendingSnooze = PendingIntent.getBroadcast(context, notificationId + 100000, snoozeIntent, flags)
            
            // 5 minutes from now (300,000 ms)
            val triggerTime = System.currentTimeMillis() + 300_000L
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingSnooze)
                    } catch (se: SecurityException) {
                        alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingSnooze)
                    }
                } else {
                    alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingSnooze)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        if (action == "com.example.action.TRIGGER_SNOOZE") {
            val title = intent.getStringExtra("title") ?: "Snoozed Reminder"
            val message = intent.getStringExtra("message") ?: "Snooze elapsed!"
            val originalId = intent.getIntExtra("notification_id", System.currentTimeMillis().toInt())

            val channelId = "trackwise_notifications"
            val iconId = context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)
            val smallIcon = if (iconId != 0) iconId else android.R.drawable.ic_dialog_info

            showNotification(
                context,
                notifManager,
                title,
                "$message (Snoozed)",
                "dashboard",
                0,
                originalId,
                smallIcon,
                channelId
            )
            return
        }

        if (action == "com.example.action.NOTIFICATION_COMPLETE") {
            if (notificationId != -1) {
                notifManager.cancel(notificationId)
            }
            val taskId = intent.getStringExtra("task_id")
            val tabletId = intent.getStringExtra("tablet_id")
            val habitId = intent.getStringExtra("habit_id")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = TrackWiseDatabase.getDatabase(context)
                    val dao = database.trackWiseDao()
                    
                    val sharedPrefs = context.getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
                    val userId = sharedPrefs.getString("saved_user_id", null) ?: run {
                        val users = dao.getAllUsers()
                        users.firstOrNull()?.id
                    }
                    
                    if (userId != null) {
                        if (taskId != null) {
                            val task = dao.getTasksForUser(userId).firstOrNull { it.id == taskId }
                            if (task != null) {
                                dao.insertTask(task.copy(completed = true))
                            }
                        } else if (tabletId != null) {
                            val tablet = dao.getTabletRemindersForUserFlow(userId).first().firstOrNull { it.id == tabletId }
                            if (tablet != null) {
                                val todayStr = TrackWiseUtils.getTodayString().take(10)
                                val completedDates = try {
                                    val array = org.json.JSONArray(tablet.completedDatesJson)
                                    val list = mutableListOf<String>()
                                    for (i in 0 until array.length()) {
                                        list.add(array.getString(i))
                                    }
                                    list
                                } catch (e: Exception) {
                                    mutableListOf<String>()
                                }
                                if (!completedDates.contains(todayStr)) {
                                    completedDates.add(todayStr)
                                    val updatedJson = org.json.JSONArray(completedDates).toString()
                                    dao.insertTabletReminder(tablet.copy(completedDatesJson = updatedJson))
                                }
                            }
                        } else if (habitId != null) {
                            val habit = dao.getHabitsForUser(userId).firstOrNull { it.id == habitId }
                            if (habit != null) {
                                val todayStr = TrackWiseUtils.getTodayString()
                                val days = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson).toMutableList()
                                if (!days.contains(todayStr)) {
                                    days.add(todayStr)
                                    
                                    // Recalculate streak
                                    var currentStreak = 1
                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                    val checkDate = java.util.Calendar.getInstance()
                                    checkDate.time = sdf.parse(todayStr) ?: java.util.Date()
                                    
                                    while (true) {
                                        checkDate.add(java.util.Calendar.DAY_OF_YEAR, -1)
                                        val prevStr = sdf.format(checkDate.time)
                                        if (prevStr < TrackWiseUtils.APP_LAUNCH_DATE) break
                                        if (days.contains(prevStr)) {
                                            currentStreak++
                                        } else {
                                            break
                                        }
                                    }
                                    
                                    val maxStreak = java.lang.Math.max(habit.maxStreak, currentStreak)
                                    
                                    // Badges
                                    val currentBadges = TrackWiseUtils.deserializeIntList(habit.badgesEarnedJson).toMutableList()
                                    val milestones = listOf(1, 3, 5, 7, 14, 21, 30, 45, 60, 90, 100, 365)
                                    milestones.forEach { milestone ->
                                        if (maxStreak >= milestone && !currentBadges.contains(milestone)) {
                                            currentBadges.add(milestone)
                                        }
                                    }
                                    
                                    val updated = habit.copy(
                                        daysCompletedJson = TrackWiseUtils.serializeStringList(days),
                                        streak = currentStreak,
                                        maxStreak = maxStreak,
                                        badgesEarnedJson = TrackWiseUtils.serializeIntList(currentBadges)
                                    )
                                    dao.insertHabit(updated)
                                }
                            }
                        }
                    }
                    
                    // Show confirmation notification
                    val iconId = context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)
                    val smallIcon = if (iconId != 0) iconId else android.R.drawable.ic_dialog_info
                    val completedNotification = NotificationCompat.Builder(context, "trackwise_notifications")
                        .setSmallIcon(smallIcon)
                        .setContentTitle("Goal Met! 🎉")
                        .setContentText("Activity completed and successfully logged.")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build()
                    notifManager.notify(System.currentTimeMillis().toInt(), completedNotification)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            val wakeLock = pm?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "TrackWise:ReminderWakeLock")
            wakeLock?.acquire(10000L)
            try {
                triggerWidgetUpdate(context)
                checkAndTriggerNotifications(context)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (wakeLock?.isHeld == true) {
                    try { wakeLock.release() } catch (_: Exception) {}
                }
                // Reschedule for next wakeup (works even in Doze mode / when app is closed)
                scheduleBackgroundReminderAlarm(context)
                pendingResult.finish()
            }
        }
    }

    private fun triggerWidgetUpdate(context: Context) {
        try {
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
            
            // Update Summary Widget
            val summaryIntent = Intent(context, com.example.widget.TrackWiseWidgetProvider::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val summaryIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, com.example.widget.TrackWiseWidgetProvider::class.java)
            )
            if (summaryIds != null && summaryIds.isNotEmpty()) {
                summaryIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, summaryIds)
                context.sendBroadcast(summaryIntent)
            }

            // Update Analytics Widget
            val analyticsIntent = Intent(context, com.example.widget.TrackWiseAnalyticsWidgetProvider::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val analyticsIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, com.example.widget.TrackWiseAnalyticsWidgetProvider::class.java)
            )
            if (analyticsIds != null && analyticsIds.isNotEmpty()) {
                analyticsIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, analyticsIds)
                context.sendBroadcast(analyticsIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun checkAndTriggerNotifications(context: Context) {
        val database = TrackWiseDatabase.getDatabase(context)
        val dao = database.trackWiseDao()

        // Get user session
        val sharedPrefs = context.getSharedPreferences("trackwise_session", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getString("saved_user_id", null) ?: run {
            // Fallback: use first user found
            val users = dao.getAllUsers()
            users.firstOrNull()?.id
        } ?: return

        val todayStr = TrackWiseUtils.getTodayString().take(10)
        val currentTimeStr = SimpleDateFormat("HH:mm", Locale.US).format(Date())

        // Setup channel
        val channelId = "trackwise_notifications"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    "TrackWise Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "System notifications for TrackWise app events"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 100, 50, 100)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        val iconId = context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)
        val smallIcon = if (iconId != 0) iconId else android.R.drawable.ic_dialog_info

        val notifiedPrefs = context.getSharedPreferences("notified_reminders", Context.MODE_PRIVATE)

        // 1. Tasks Check (Multi-Reminder Option Support)
        val tasks = dao.getTasksForUser(userId)
        tasks.forEach { task ->
            if (task.remindMe && !task.completed) {
                val triggers = calculateTaskReminderTriggers(task)
                triggers.forEach { trigger ->
                    if (trigger.triggerDate == todayStr && trigger.triggerTime24 <= currentTimeStr) {
                        val key = "task-${task.id}-${trigger.optionLabel}-${trigger.triggerDate}-${trigger.triggerTime24}"
                        if (!notifiedPrefs.getBoolean(key, false)) {
                            notifiedPrefs.edit().putBoolean(key, true).apply()
                            showNotification(
                                context,
                                notificationManager,
                                "Task Reminder (${trigger.optionLabel}): ${task.title}",
                                "Project: ${task.project} • Priority: ${task.priority.uppercase()}${if (task.description.isNotBlank()) " • " + task.description else ""}",
                                "workspace",
                                0, // Tasks tab
                                key.hashCode(),
                                smallIcon,
                                channelId,
                                taskId = task.id
                            )
                        }
                    }
                }
            }
        }

        // 2. Habits Check
        val habits = dao.getHabitsForUser(userId)
        habits.forEach { habit ->
            if (habit.remindMe) {
                val isDueToday = TrackWiseUtils.shouldShowHabitOnDate(habit, todayStr)
                val completedDays = TrackWiseUtils.deserializeStringList(habit.daysCompletedJson)
                val isCompletedToday = completedDays.contains(todayStr)
                val rTime24 = parseTo24HourTime(habit.reminderTime)
                if (isDueToday && !isCompletedToday && rTime24 != null && rTime24 <= currentTimeStr) {
                    val key = "habit-${habit.id}-$todayStr-$rTime24"
                    if (!notifiedPrefs.getBoolean(key, false)) {
                        notifiedPrefs.edit().putBoolean(key, true).apply()
                        showNotification(
                            context,
                            notificationManager,
                            "Habit: ${habit.name}",
                            "Category: ${habit.category} • Streak: ${habit.streak} days${if (habit.quote.isNotBlank()) " • \"" + habit.quote + "\"" else if (habit.notes.isNotBlank()) " • " + habit.notes else ""}",
                            "workspace",
                            1, // Habit tab
                            key.hashCode(),
                            smallIcon,
                            channelId,
                            habitId = habit.id
                        )
                    }
                }
            }
        }

        // 3. Wishlist Check
        val wishlist = dao.getWishlistForUserFlow(userId).first()
        wishlist.forEach { item ->
            if (item.remindMe && !item.purchased) {
                val rDate = item.reminderDate?.take(10)
                val rTime24 = parseTo24HourTime(item.reminderTime)
                if (rDate == todayStr && rTime24 != null && rTime24 <= currentTimeStr) {
                    val key = "wish-${item.id}-$rDate-$rTime24"
                    if (!notifiedPrefs.getBoolean(key, false)) {
                        notifiedPrefs.edit().putBoolean(key, true).apply()
                        showNotification(
                            context,
                            notificationManager,
                            "Wishlist Reminder: ${item.title}",
                            "Price: ₹${item.price} • Priority: ${item.priority.uppercase()}${if (!item.link.isNullOrBlank()) " • Link available" else ""}",
                            "workspace",
                            2, // Wishlist tab
                            key.hashCode(),
                            smallIcon,
                            channelId
                        )
                    }
                }
            }
        }

        // 4. Occasions Check
        val birthdays = dao.getBirthdaysForUserFlow(userId).first()
        birthdays.forEach { bday ->
            if (bday.remindMe) {
                val rDate = bday.reminderDate?.take(10) ?: bday.date.take(10)
                val rTime24 = parseTo24HourTime(bday.reminderTime)
                if (rDate == todayStr && rTime24 != null && rTime24 <= currentTimeStr) {
                    val key = "bday-${bday.id}-$rDate-$rTime24"
                    if (!notifiedPrefs.getBoolean(key, false)) {
                        notifiedPrefs.edit().putBoolean(key, true).apply()
                        showNotification(
                            context,
                            notificationManager,
                            "Occasion Reminder: ${bday.name}",
                            "Category: ${bday.category} • Date: ${bday.date}${if (!bday.giftIdea.isNullOrBlank()) " • Gift Idea: " + bday.giftIdea else ""}",
                            "workspace",
                            3, // Occasions tab
                            key.hashCode(),
                            smallIcon,
                            channelId
                        )
                    }
                }
            }
        }

        // 5. Tablet Reminders Check
        val tabletReminders = dao.getTabletRemindersForUserFlow(userId).first()
        tabletReminders.forEach { tablet ->
            val rTime24 = parseTo24HourTime(tablet.timeOfDay) ?: return@forEach
            if (rTime24 <= currentTimeStr) {
                val completedDates = try {
                    val array = org.json.JSONArray(tablet.completedDatesJson)
                    val list = mutableListOf<String>()
                    for (i in 0 until array.length()) {
                        list.add(array.getString(i))
                    }
                    list
                } catch (e: Exception) {
                    emptyList()
                }

                if (!completedDates.contains(todayStr)) {
                    val key = "tablet-${tablet.id}-$todayStr-$rTime24"
                    if (!notifiedPrefs.getBoolean(key, false)) {
                        notifiedPrefs.edit().putBoolean(key, true).apply()
                        showNotification(
                            context,
                            notificationManager,
                            "Tablet Reminder: ${tablet.tabletName}",
                            "It's time to take ${tablet.tabletName} (${tablet.dosage}). Notes: ${tablet.notes ?: "None"}",
                            "health",
                            4, // Tablets tab in HealthScreen
                            key.hashCode(),
                            smallIcon,
                            channelId,
                            tabletId = tablet.id
                        )
                    }
                }
            }
        }
    }

    private fun showNotification(
        context: Context,
        notificationManager: NotificationManager,
        title: String,
        message: String,
        targetTab: String,
        targetSubTab: Int,
        notificationId: Int,
        smallIcon: Int,
        channelId: String,
        taskId: String? = null,
        tabletId: String? = null,
        habitId: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_tab", targetTab)
            putExtra("target_sub_tab", targetSubTab)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, notificationId, intent, flags)

        val actionFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Complete action
        val completeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.action.NOTIFICATION_COMPLETE"
            putExtra("notification_id", notificationId)
            putExtra("task_id", taskId)
            putExtra("tablet_id", tabletId)
            putExtra("habit_id", habitId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2000,
            completeIntent,
            actionFlags
        )

        // Snooze action
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.action.NOTIFICATION_SNOOZE"
            putExtra("notification_id", notificationId)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("task_id", taskId)
            putExtra("tablet_id", tabletId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 3000,
            snoozeIntent,
            actionFlags
        )

        // Dismiss action
        val dismissIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.action.NOTIFICATION_DISMISS"
            putExtra("notification_id", notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 4000,
            dismissIntent,
            actionFlags
        )

        val notifExtras = android.os.Bundle().apply {
            if (taskId != null) putString("task_id", taskId)
            if (tabletId != null) putString("tablet_id", tabletId)
            if (habitId != null) putString("habit_id", habitId)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(true)
            .addExtras(notifExtras)
            .setVibrate(longArrayOf(0, 100, 50, 100))
            .setContentIntent(pendingIntent)

        if (taskId != null || tabletId != null || habitId != null) {
            builder.addAction(0, "Complete", completePendingIntent)
        }
        if (habitId == null) {
            builder.addAction(0, "Snooze (5 min)", snoozePendingIntent)
        }
        builder.addAction(0, "Dismiss", dismissPendingIntent)

        val notification = builder.build()

        notificationManager.notify(notificationId, notification)

        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun parseTo24HourTime(timeStr: String?): String? {
            if (timeStr.isNullOrBlank()) return null
            val trimmed = timeStr.trim()
            if (trimmed.matches(Regex("^\\d{1,2}:\\d{2}$"))) {
                val parts = trimmed.split(":")
                val h = parts[0].toIntOrNull() ?: return null
                val m = parts[1].toIntOrNull() ?: return null
                return String.format(Locale.US, "%02d:%02d", h, m)
            }
            return convertTo24Hour(trimmed)
        }

        private fun convertTo24Hour(time12: String): String? {
            val upperTime12 = time12.uppercase(Locale.US)
            return try {
                val sdf12 = SimpleDateFormat("hh:mm a", Locale.US)
                val sdf24 = SimpleDateFormat("HH:mm", Locale.US)
                val date = sdf12.parse(upperTime12) ?: return null
                sdf24.format(date)
            } catch (e: Exception) {
                try {
                    val sdf12NoSpace = SimpleDateFormat("hh:mma", Locale.US)
                    val sdf24 = SimpleDateFormat("HH:mm", Locale.US)
                    val date = sdf12NoSpace.parse(upperTime12) ?: return null
                    sdf24.format(date)
                } catch (ex: Exception) {
                    null
                }
            }
        }

        fun scheduleBackgroundReminderAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, 999, intent, flags)

            val triggerAt = System.currentTimeMillis() + 60000L // Next check in 1 minute

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pendingIntent
                        )
                    } catch (e: Exception) {
                        alarmManager.setAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.set(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

data class ReminderTrigger(
    val triggerDate: String,
    val triggerTime24: String,
    val optionLabel: String
)

fun calculateTaskReminderTriggers(task: com.example.data.TaskEntity): List<ReminderTrigger> {
    val triggers = mutableListOf<ReminderTrigger>()
    val deadline = task.deadline.take(10)
    val rTimeStr = task.reminderTime ?: "09:00"
    
    val sdfFull = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
    
    val baseTime24 = ReminderReceiver.parseTo24HourTime(rTimeStr) ?: "09:00"
    val baseDateTime = try {
        sdfFull.parse("$deadline $baseTime24")
    } catch (e: Exception) {
        null
    }
    
    val rawOptionsStr = task.reminderDate ?: "On the day"
    val optionsList = rawOptionsStr.split(",").map { it.trim() }.filter { it.isNotBlank() && it != "None" }
    
    val optionsToProcess = if (optionsList.isEmpty()) listOf("On the day") else optionsList
    
    optionsToProcess.forEach { opt ->
        if (baseDateTime == null) {
            triggers.add(ReminderTrigger(deadline, baseTime24, opt))
            return@forEach
        }
        val cal = Calendar.getInstance()
        cal.time = baseDateTime
        
        when {
            opt.contains("On the day") || opt.contains("At time of event") -> {
                // No offset
            }
            opt.contains("5 mins") -> cal.add(Calendar.MINUTE, -5)
            opt.contains("15 mins") -> cal.add(Calendar.MINUTE, -15)
            opt.contains("30 mins") -> cal.add(Calendar.MINUTE, -30)
            opt.contains("1 hour") -> cal.add(Calendar.MINUTE, -60)
            opt.contains("2 hours") -> cal.add(Calendar.MINUTE, -120)
            opt.contains("1 day") -> cal.add(Calendar.DAY_OF_YEAR, -1)
            opt.contains("2 days") -> cal.add(Calendar.DAY_OF_YEAR, -2)
            opt.contains("3 days") -> cal.add(Calendar.DAY_OF_YEAR, -3)
            opt.contains("1 week") -> cal.add(Calendar.DAY_OF_YEAR, -7)
            opt.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                triggers.add(ReminderTrigger(opt, baseTime24, "Custom"))
                return@forEach
            }
        }
        val tDate = sdfDate.format(cal.time)
        val tTime = sdfTime.format(cal.time)
        triggers.add(ReminderTrigger(tDate, tTime, opt))
    }
    
    return triggers
}
