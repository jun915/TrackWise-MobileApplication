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
        if (action == Intent.ACTION_BOOT_COMPLETED) {
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
                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingSnooze)
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
            try {
                checkAndTriggerNotifications(context)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
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
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        val iconId = context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)
        val smallIcon = if (iconId != 0) iconId else android.R.drawable.ic_dialog_info

        val notifiedPrefs = context.getSharedPreferences("notified_reminders", Context.MODE_PRIVATE)

        // 1. Tasks Check
        val tasks = dao.getTasksForUser(userId)
        tasks.forEach { task ->
            if (task.remindMe && !task.completed) {
                val rDate = task.reminderDate?.take(10) ?: task.deadline.take(10)
                val rTime = task.reminderTime?.trim()
                if (rDate == todayStr && rTime != null && rTime <= currentTimeStr) {
                    val key = "task-${task.id}-$rDate-$rTime"
                    if (!notifiedPrefs.getBoolean(key, false)) {
                        notifiedPrefs.edit().putBoolean(key, true).apply()
                        showNotification(
                            context,
                            notificationManager,
                            "Task Reminder: ${task.title}",
                            "Deadline: ${task.deadline}. Don't forget to complete it!",
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

        // 2. Habits Check
        val habits = dao.getHabitsForUser(userId)
        habits.forEach { habit ->
            if (habit.remindMe) {
                val rDate = habit.reminderDate?.take(10) ?: todayStr
                val rTime = habit.reminderTime?.trim()
                if (rDate == todayStr && rTime != null && rTime <= currentTimeStr) {
                    val key = "habit-${habit.id}-$rDate-$rTime"
                    if (!notifiedPrefs.getBoolean(key, false)) {
                        notifiedPrefs.edit().putBoolean(key, true).apply()
                        showNotification(
                            context,
                            notificationManager,
                            "Habit: ${habit.name}",
                            "It's time for your habit: ${habit.category}!",
                            "workspace",
                            1, // Habit tab
                            key.hashCode(),
                            smallIcon,
                            channelId
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
                val rTime = item.reminderTime?.trim()
                if (rDate == todayStr && rTime != null && rTime <= currentTimeStr) {
                    val key = "wish-${item.id}-$rDate-$rTime"
                    if (!notifiedPrefs.getBoolean(key, false)) {
                        notifiedPrefs.edit().putBoolean(key, true).apply()
                        showNotification(
                            context,
                            notificationManager,
                            "Wishlist Reminder: ${item.title}",
                            "Check out your item: ${item.title} (₹${item.price})",
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
                val rTime = bday.reminderTime?.trim()
                if (rDate == todayStr && rTime != null && rTime <= currentTimeStr) {
                    val key = "bday-${bday.id}-$rDate-$rTime"
                    if (!notifiedPrefs.getBoolean(key, false)) {
                        notifiedPrefs.edit().putBoolean(key, true).apply()
                        showNotification(
                            context,
                            notificationManager,
                            "Occasion Reminder: ${bday.name}",
                            "Event: ${bday.name} is scheduled for today!",
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
            val rTime12 = tablet.timeOfDay.trim() // e.g. "08:00 AM" or "09:00 PM"
            val rTime24 = convertTo24Hour(rTime12) ?: return@forEach
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

    private fun convertTo24Hour(time12: String): String? {
        return try {
            val sdf12 = SimpleDateFormat("hh:mm a", Locale.US)
            val sdf24 = SimpleDateFormat("HH:mm", Locale.US)
            val date = sdf12.parse(time12) ?: return null
            sdf24.format(date)
        } catch (e: Exception) {
            null
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
        tabletId: String? = null
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

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (taskId != null || tabletId != null) {
            builder.addAction(0, "Complete", completePendingIntent)
        }
        builder.addAction(0, "Snooze (5 min)", snoozePendingIntent)
        builder.addAction(0, "Dismiss", dismissPendingIntent)

        val notification = builder.build()

        notificationManager.notify(notificationId, notification)
    }

    companion object {
        fun scheduleBackgroundReminderAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, 999, intent, flags)

            val interval = 60000L // 1 minute
            val triggerAt = System.currentTimeMillis() + 5000L // start in 5s

            try {
                alarmManager.setRepeating(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    interval,
                    pendingIntent
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
