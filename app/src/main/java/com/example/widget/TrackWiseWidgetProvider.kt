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
import com.example.MainActivity
import com.example.data.TrackWiseDatabase
import com.example.utils.TrackWiseUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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

        private fun getTabPendingIntent(context: Context, category: String, appWidgetId: Int): PendingIntent {
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

        private fun getActivityPendingIntent(context: Context, targetTab: String, targetSubTab: Int = 0): PendingIntent {
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
            val requestCode = (targetTab + targetSubTab).hashCode()
            return PendingIntent.getActivity(context, requestCode, intent, flags)
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
            val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "planner"
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                prefs.edit().putString("selected_category_$appWidgetId", category).apply()
                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateWidget(context, appWidgetManager, appWidgetId)
            } else {
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

                // Get stored selection for this widget (planner, wellbeing, wealth)
                val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                val selectedCategory = prefs.getString("selected_category_$appWidgetId", null)
                    ?: prefs.getString("selected_category", "planner") ?: "planner"

                // Fetch theme mode & accent
                val themeMode = sessionPrefs.getString("saved_theme_mode", "light") ?: "light"
                val themeAccent = sessionPrefs.getString("saved_theme_accent", "Default Violet") ?: "Default Violet"
                val isDark = when (themeMode) {
                    "dark" -> true
                    "light" -> false
                    "auto" -> {
                        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
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

                // Header Date (Dynamic)
                val todayStr = TrackWiseUtils.getTodayString().take(10)
                val displayDateStr = SimpleDateFormat("EEEE, MMM d", Locale.US).format(Date())
                views.setTextViewText(R.id.widget_header_date, displayDateStr)
                views.setTextColor(R.id.widget_header_date, textSecondaryColor)

                // Tab button click pending intents
                views.setOnClickPendingIntent(R.id.btn_tab_1, getTabPendingIntent(context, "planner", appWidgetId))
                views.setOnClickPendingIntent(R.id.btn_tab_2, getTabPendingIntent(context, "wellbeing", appWidgetId))
                views.setOnClickPendingIntent(R.id.btn_tab_3, getTabPendingIntent(context, "wealth", appWidgetId))

                when (selectedCategory) {
                    "planner" -> {
                        views.setInt(R.id.btn_tab_1, "setBackgroundResource", R.drawable.widget_tab_active)
                        views.setTextColor(R.id.btn_tab_1, Color.WHITE)

                        views.setInt(R.id.btn_tab_2, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_2, textSecondaryColor)

                        views.setInt(R.id.btn_tab_3, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_3, textSecondaryColor)

                        // 1. Top 1 Task
                        val allTasks = dao.getTasksForUser(userId)
                        val todayTasks = allTasks.filter { TrackWiseUtils.shouldShowTaskOnDate(it, todayStr) }
                        val topTask = todayTasks.firstOrNull { !it.completed } ?: todayTasks.firstOrNull()

                        if (topTask != null) {
                            views.setTextViewText(R.id.txt_item1_label, "📝 " + topTask.title)
                            views.setTextViewText(R.id.txt_item1_value, if (topTask.completed) "Completed" else "Pending")
                        } else {
                            views.setTextViewText(R.id.txt_item1_label, "📝 No tasks today")
                            views.setTextViewText(R.id.txt_item1_value, "")
                        }
                        views.setTextColor(R.id.txt_item1_label, textPrimaryColor)
                        views.setTextColor(R.id.txt_item1_value, textSecondaryColor)
                        views.setOnClickPendingIntent(R.id.layout_item1, getActivityPendingIntent(context, "workspace", 0))

                        // 2. Top 1 Habit
                        val allHabits = dao.getHabitsForUser(userId)
                        val filteredHabits = allHabits.filter { TrackWiseUtils.shouldShowHabitOnDate(it, todayStr) }
                        val topHabit = filteredHabits.firstOrNull { !it.daysCompletedJson.contains(todayStr) } ?: filteredHabits.firstOrNull()

                        if (topHabit != null) {
                            val isCompleted = topHabit.daysCompletedJson.contains(todayStr)
                            views.setTextViewText(R.id.txt_item2_label, "💪 " + topHabit.name)
                            views.setTextViewText(R.id.txt_item2_value, if (isCompleted) "Completed" else "Pending")
                        } else {
                            views.setTextViewText(R.id.txt_item2_label, "💪 No habits today")
                            views.setTextViewText(R.id.txt_item2_value, "")
                        }
                        views.setTextColor(R.id.txt_item2_label, textPrimaryColor)
                        views.setTextColor(R.id.txt_item2_value, textSecondaryColor)
                        views.setOnClickPendingIntent(R.id.layout_item2, getActivityPendingIntent(context, "workspace", 1))

                        // 3. Today's Occasions
                        val birthdays = dao.getBirthdaysForUserFlow(userId).first()
                        val todayOccasion = birthdays.firstOrNull { it.date.endsWith(todayStr.substring(5)) }
                        val todayFestivals = TrackWiseUtils.getIndianFestivalsForDate(todayStr)

                        if (todayOccasion != null) {
                            views.setTextViewText(R.id.txt_item3_label, "🎉 Occasion: " + todayOccasion.name)
                            views.setTextViewText(R.id.txt_item3_value, "Today")
                        } else if (todayFestivals.isNotEmpty()) {
                            views.setTextViewText(R.id.txt_item3_label, "🎉 " + todayFestivals.first())
                            views.setTextViewText(R.id.txt_item3_value, "Today")
                        } else {
                            views.setTextViewText(R.id.txt_item3_label, "🎉 No occasions today")
                            views.setTextViewText(R.id.txt_item3_value, "")
                        }
                        views.setTextColor(R.id.txt_item3_label, textPrimaryColor)
                        views.setTextColor(R.id.txt_item3_value, textSecondaryColor)
                        views.setOnClickPendingIntent(R.id.layout_item3, getActivityPendingIntent(context, "workspace", 3))

                        // Progress ring: Tasks + Habits completed
                        val completedToday = todayTasks.count { it.completed } + filteredHabits.count { it.daysCompletedJson.contains(todayStr) }
                        val totalScheduled = todayTasks.size + filteredHabits.size
                        val bitmap = drawPieChart(completedToday, totalScheduled, accentColor, isDark)
                        views.setImageViewBitmap(R.id.img_pie_chart, bitmap)
                        views.setOnClickPendingIntent(R.id.layout_right_pane, getActivityPendingIntent(context, "workspace", 0))
                    }
                    "wellbeing" -> {
                        views.setInt(R.id.btn_tab_1, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_1, textSecondaryColor)

                        views.setInt(R.id.btn_tab_2, "setBackgroundResource", R.drawable.widget_tab_active)
                        views.setTextColor(R.id.btn_tab_2, Color.WHITE)

                        views.setInt(R.id.btn_tab_3, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_3, textSecondaryColor)

                        // 1. Water intake
                        val waterLogs = dao.getWaterLogsForUser(userId)
                        val waterEntry = waterLogs.find { it.date == todayStr }
                        val glasses = waterEntry?.glasses ?: 0
                        val goal = waterEntry?.goal ?: 8

                        views.setTextViewText(R.id.txt_item1_label, "💧 Water Intake:")
                        views.setTextViewText(R.id.txt_item1_value, "$glasses / $goal glasses")
                        views.setTextColor(R.id.txt_item1_label, textPrimaryColor)
                        views.setTextColor(R.id.txt_item1_value, textSecondaryColor)
                        views.setOnClickPendingIntent(R.id.layout_item1, getActivityPendingIntent(context, "health", 0))

                        // 2. BMI
                        val userProfile = dao.getUserProfile(userId)
                        val height = userProfile?.height?.replace("cm", "", ignoreCase = true)?.trim()?.toDoubleOrNull() ?: 170.0
                        val weightEntries = dao.getWeightEntriesForUserFlow(userId).first()
                        val latestWeight = weightEntries.sortedByDescending { it.date }.firstOrNull()?.weightKg 
                            ?: userProfile?.weight?.replace("kg", "", ignoreCase = true)?.trim()?.toDoubleOrNull() 
                            ?: 65.0
                        
                        val bmi = latestWeight / (height / 100.0 * height / 100.0)
                        val bmiCategory = when {
                            bmi < 18.5 -> "Underweight"
                            bmi < 25.0 -> "Normal"
                            bmi < 30.0 -> "Overweight"
                            else -> "Obese"
                        }

                        views.setTextViewText(R.id.txt_item2_label, "⚖️ Current BMI:")
                        views.setTextViewText(R.id.txt_item2_value, String.format(Locale.US, "%.1f (%s)", bmi, bmiCategory))
                        views.setTextColor(R.id.txt_item2_label, textPrimaryColor)
                        views.setTextColor(R.id.txt_item2_value, textSecondaryColor)
                        views.setOnClickPendingIntent(R.id.layout_item2, getActivityPendingIntent(context, "health", 0))

                        // 3. Top 2 Tablet taker logs
                        val tablets = dao.getTabletRemindersForUserFlow(userId).first()
                        if (tablets.isNotEmpty()) {
                            val activeTablets = tablets.take(2)
                            if (activeTablets.size == 2) {
                                views.setTextViewText(R.id.txt_item3_label, "💊 " + activeTablets[0].tabletName + " & " + activeTablets[1].tabletName)
                                views.setTextViewText(R.id.txt_item3_value, activeTablets[0].timeOfDay + " / " + activeTablets[1].timeOfDay)
                            } else {
                                views.setTextViewText(R.id.txt_item3_label, "💊 " + activeTablets[0].tabletName)
                                views.setTextViewText(R.id.txt_item3_value, activeTablets[0].timeOfDay)
                            }
                        } else {
                            views.setTextViewText(R.id.txt_item3_label, "💊 No tablets scheduled")
                            views.setTextViewText(R.id.txt_item3_value, "")
                        }
                        views.setTextColor(R.id.txt_item3_label, textPrimaryColor)
                        views.setTextColor(R.id.txt_item3_value, textSecondaryColor)
                        views.setOnClickPendingIntent(R.id.layout_item3, getActivityPendingIntent(context, "health", 4))

                        // Water Progress Ring
                        val waterBitmap = drawPieChart(glasses, goal, Color.parseColor("#0ea5e9"), isDark)
                        views.setImageViewBitmap(R.id.img_pie_chart, waterBitmap)
                        views.setOnClickPendingIntent(R.id.layout_right_pane, getActivityPendingIntent(context, "health", 0))
                    }
                    "wealth" -> {
                        views.setInt(R.id.btn_tab_1, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_1, textSecondaryColor)

                        views.setInt(R.id.btn_tab_2, "setBackgroundResource", R.drawable.widget_tab_inactive)
                        views.setTextColor(R.id.btn_tab_2, textSecondaryColor)

                        views.setInt(R.id.btn_tab_3, "setBackgroundResource", R.drawable.widget_tab_active)
                        views.setTextColor(R.id.btn_tab_3, Color.WHITE)

                        // 1. Total Expenses
                        val logs = dao.getFinanceLogsForUser(userId)
                        val income = logs.filter { it.type == "income" }.sumOf { it.amount }
                        val expense = logs.filter { it.type == "expense" }.sumOf { it.amount }
                        val savings = logs.filter { it.type == "savings" }.sumOf { it.amount }
                        val balance = income - (expense + savings)

                        val spentToday = logs.filter { it.date == todayStr && it.type == "expense" }.sumOf { it.amount }

                        views.setTextViewText(R.id.txt_item1_label, "💸 Total Expenses Today:")
                        views.setTextViewText(R.id.txt_item1_value, "₹${spentToday.toInt()}")
                        views.setTextColor(R.id.txt_item1_label, textPrimaryColor)
                        views.setTextColor(R.id.txt_item1_value, textSecondaryColor)
                        views.setOnClickPendingIntent(R.id.layout_item1, getActivityPendingIntent(context, "finance", 0))

                        // 2. Net Balance
                        views.setTextViewText(R.id.txt_item2_label, "🏦 Net Balance:")
                        views.setTextViewText(R.id.txt_item2_value, "₹${balance.toInt()}")
                        views.setTextColor(R.id.txt_item2_label, textPrimaryColor)
                        views.setTextColor(R.id.txt_item2_value, textSecondaryColor)
                        views.setOnClickPendingIntent(R.id.layout_item2, getActivityPendingIntent(context, "finance", 0))

                        // 3. Subcategories clickable label
                        views.setTextViewText(R.id.txt_item3_label, "📊 Expenses Breakdown:")
                        views.setTextViewText(R.id.txt_item3_value, "View details")
                        views.setTextColor(R.id.txt_item3_label, textPrimaryColor)
                        views.setTextColor(R.id.txt_item3_value, textSecondaryColor)
                        views.setOnClickPendingIntent(R.id.layout_item3, getActivityPendingIntent(context, "finance", 0))

                        // Donut Chart of subcategories
                        val subcatMap = logs.filter { it.type == "expense" }
                            .groupBy { it.category }
                            .mapValues { entry -> entry.value.sumOf { it.amount } }

                        val donutBitmap = drawSubcatsDonutChart(subcatMap, isDark, accentColor)
                        views.setImageViewBitmap(R.id.img_pie_chart, donutBitmap)
                        views.setOnClickPendingIntent(R.id.layout_right_pane, getActivityPendingIntent(context, "finance", 0))
                    }
                }

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

        val bgPaint = Paint().apply {
            color = Color.TRANSPARENT
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        val strokeW = 12f
        val rectF = RectF(strokeW, strokeW, size.toFloat() - strokeW, size.toFloat() - strokeW)

        val trackPaint = Paint().apply {
            color = if (isDark) Color.parseColor("#334155") else Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(rectF, 0f, 360f, false, trackPaint)

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

        val textPaint = Paint().apply {
            color = if (isDark) Color.WHITE else Color.parseColor("#0F172A")
            textSize = 26f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val percentageText = if (total > 0) "${(percentage * 100).toInt()}%" else "0%"
        val textY = (size / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(percentageText, (size / 2).toFloat(), textY, textPaint)

        return bitmap
    }

    private fun drawSubcatsDonutChart(
        subcatExpenses: Map<String, Double>,
        isDark: Boolean,
        accentColor: Int
    ): Bitmap {
        val size = 144
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply { color = Color.TRANSPARENT; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        val strokeW = 12f
        val rectF = RectF(strokeW, strokeW, size.toFloat() - strokeW, size.toFloat() - strokeW)

        val total = subcatExpenses.values.sum()
        if (total <= 0.0) {
            val trackPaint = Paint().apply {
                color = if (isDark) Color.parseColor("#334155") else Color.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                isAntiAlias = true
            }
            canvas.drawArc(rectF, 0f, 360f, false, trackPaint)

            val textPaint = Paint().apply {
                color = if (isDark) Color.WHITE else Color.parseColor("#0F172A")
                textSize = 24f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("₹0", (size / 2).toFloat(), (size / 2) + 8f, textPaint)
            return bitmap
        }

        val sliceColors = intArrayOf(
            accentColor,
            Color.parseColor("#10B981"), // BrandGreen
            Color.parseColor("#F97316"), // BrandOrange
            Color.parseColor("#0EA5E9"), // BrandBlue
            Color.parseColor("#EC4899")  // Pink
        )

        var startAngle = -90f
        var colorIdx = 0

        for ((_, amount) in subcatExpenses) {
            val sweepAngle = ((amount / total) * 360f).toFloat()
            val sliceColor = sliceColors[colorIdx % sliceColors.size]
            colorIdx++

            val progressPaint = Paint().apply {
                color = sliceColor
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                isAntiAlias = true
                strokeCap = Paint.Cap.BUTT
            }
            canvas.drawArc(rectF, startAngle, sweepAngle, false, progressPaint)
            startAngle += sweepAngle
        }

        val textPaint = Paint().apply {
            color = if (isDark) Color.WHITE else Color.parseColor("#0F172A")
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val totalStr = if (total >= 1000) {
            "₹${String.format(Locale.US, "%.1fk", total / 1000.0)}"
        } else {
            "₹${total.toInt()}"
        }
        val textY = (size / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(totalStr, (size / 2).toFloat(), textY, textPaint)

        return bitmap
    }
}
