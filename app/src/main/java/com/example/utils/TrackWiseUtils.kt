package com.example.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import com.example.data.SubTask
import com.example.data.TaskEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor

object TrackWiseUtils {

    const val APP_LAUNCH_DATE = "2026-07-01"

    // --- JSON Converters (Simulating Moshi or simple serialization manually/reliably for speed) ---
    // Since SubTask is simple, we can serialize it with a simple custom parser to avoid reflection/KSP bugs
    fun serializeSubTasks(subtasks: List<SubTask>): String {
        val sb = StringBuilder()
        sb.append("[")
        subtasks.forEachIndexed { index, subTask ->
            val escapedTitle = subTask.title.replace("\"", "\\\"")
            val escapedDueDate = subTask.dueDate?.replace("\"", "\\\"") ?: ""
            val escapedDueTime = subTask.dueTime?.replace("\"", "\\\"") ?: ""
            sb.append("{\"id\":\"${subTask.id}\",\"title\":\"$escapedTitle\",\"completed\":${subTask.completed},\"dueDate\":\"$escapedDueDate\",\"dueTime\":\"$escapedDueTime\"}")
            if (index < subtasks.size - 1) sb.append(",")
        }
        sb.append("]")
        return sb.toString()
    }

    fun deserializeSubTasks(json: String): List<SubTask> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<SubTask>()
        try {
            val objRegex = """\{([^}]+)\}""".toRegex()
            val keyValRegex = """\"([^\"]+)\"\s*:\s*(\"([^\"]*)\"|(true|false))""".toRegex()
            
            val objects = objRegex.findAll(json)
            for (obj in objects) {
                val content = obj.groupValues[1]
                var id = ""
                var title = ""
                var completed = false
                var dueDate: String? = null
                var dueTime: String? = null
                
                val pairs = keyValRegex.findAll(content)
                for (pair in pairs) {
                    val key = pair.groupValues[1]
                    val value = if (pair.groupValues[3].isNotEmpty()) pair.groupValues[3] else pair.groupValues[4]
                    when (key) {
                        "id" -> id = value
                        "title" -> title = value.replace("\\\"", "\"")
                        "completed" -> completed = value.toBoolean()
                        "dueDate" -> if (value.isNotBlank()) dueDate = value
                        "dueTime" -> if (value.isNotBlank()) dueTime = value
                    }
                }
                list.add(SubTask(id, title, completed, dueDate, dueTime))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializeStringList(list: List<String>): String {
        return list.joinToString(",")
    }

    fun deserializeStringList(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return json.split(",")
    }

    fun serializeIntList(list: List<Int>): String {
        return list.joinToString(",")
    }

    fun deserializeIntList(json: String): List<Int> {
        if (json.isBlank()) return emptyList()
        return json.split(",").mapNotNull { it.toIntOrNull() }
    }

    // --- Date Helpers ---
    fun formatDate(date: Date, format: String = "yyyy-MM-dd"): String {
        val sdf = SimpleDateFormat(format, Locale.US)
        return sdf.format(date)
    }

    fun parseDate(dateStr: String, format: String = "yyyy-MM-dd"): Date {
        val sdf = SimpleDateFormat(format, Locale.US)
        return try {
            sdf.parse(dateStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    fun getTodayString(): String {
        return formatDate(Date())
    }

    fun getPinTimestamp(notes: String): Long {
        if (!notes.contains("[PINNED]")) return Long.MAX_VALUE
        val match = Regex("\\[PINNED:(\\d+)\\]").find(notes)
        return match?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }

    fun parseHourFromTimeString(time: String?): Int? {
        if (time.isNullOrBlank()) return null
        val trimmed = time.trim()
        val isPm = trimmed.contains("PM", ignoreCase = true)
        val isAm = trimmed.contains("AM", ignoreCase = true)
        
        val cleanStr = trimmed.replace("AM", "", ignoreCase = true)
                              .replace("PM", "", ignoreCase = true)
                              .trim()
        val parts = cleanStr.split(":")
        if (parts.isNotEmpty()) {
            val rawHour = parts[0].trim().toIntOrNull() ?: return null
            return when {
                isPm -> if (rawHour < 12) rawHour + 12 else rawHour
                isAm -> if (rawHour == 12) 0 else rawHour
                else -> rawHour
            }
        }
        return null
    }

    fun getDaysUntil(storedDate: String): Int {
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

    fun isBeforeLaunch(dateStr: String): Boolean {
        return dateStr < APP_LAUNCH_DATE
    }

    fun getDaysLeftText(deadline: String): String {
        if (deadline.isBlank()) return ""
        val todayStr = getTodayString()
        if (deadline == todayStr) return "Due today"
        val dDate = parseDate(deadline)
        val tDate = parseDate(todayStr)
        val diffMs = dDate.time - tDate.time
        val diffDays = java.lang.Math.round(diffMs.toDouble() / (1000.0 * 60 * 60 * 24)).toInt()
        return when {
            diffDays == 0 -> "Due today"
            diffDays == 1 -> "1 day left"
            diffDays > 1 -> "$diffDays days left"
            diffDays == -1 -> "Overdue by 1 day"
            else -> "Overdue by ${kotlin.math.abs(diffDays)} days"
        }
    }

    // --- Islamic Calendar (Hijri Date and 99 Names) ---
    // Epoch: 2025-12-21 is Day 100 ("Allah"), so 2025-12-22 is Day 1
    fun getAllahNameForDate(dateStr: String): AllahName {
        val epochStr = "2025-12-21"
        val epoch = parseDate(epochStr).time
        val current = parseDate(dateStr).time
        val diffMs = current - epoch
        val diffDays = java.lang.Math.round(diffMs.toDouble() / (1000.0 * 60 * 60 * 24)).toInt()
        
        // Cycle is 100 days
        var cycleIndex = diffDays % 100
        if (cycleIndex < 0) {
            cycleIndex += 100
        }
        
        val nameNum = if (cycleIndex == 0) 100 else cycleIndex
        return ALLAH_NAMES_LIST.find { it.dayNum == nameNum } ?: ALLAH_NAMES_LIST[0]
    }

    // Tabular Islamic Calendar Approximation
    fun getHijriDate(dateStr: String): String {
        val info = getHijriInfo(dateStr)
        return "${info.day} ${info.monthNameEn} ${info.year}"
    }

    fun getHijriInfo(dateStr: String): HijriInfo {
        val date = parseDate(dateStr)
        val cal = Calendar.getInstance()
        cal.time = date
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        // Julian Day Number (JDN)
        val a = floor((14 - month) / 12.0).toInt()
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        val jdn = day + floor((153 * m + 2) / 5.0).toInt() + 365 * y + floor(y / 4.0).toInt() - floor(y / 100.0).toInt() + floor(y / 400.0).toInt() - 32045

        // Estimate Hijri date from Julian Day
        val jd = jdn.toDouble()
        val l = jd - 1948440 + 10633 // Adjusted by +1 to match India's moon sighting calendar (e.g. 21 Safar on 2026-08-05)
        val n = floor((l - 1) / 10631.0).toInt()
        val l2 = l - 10631 * n + 354
        val j = (floor((10985 - l2) / 5316.0) * floor((50 * l2) / 17719.0) + floor(l2 / 5670.0) * floor((43 * l2) / 15238.0)).toInt()
        val l3 = l2 - floor((30 - j) / 15.0).toInt() * floor((17719 * j) / 50.0).toInt() - floor(j / 16.0).toInt() * floor((15238 * j) / 43.0).toInt() + 29
        
        val hMonth = floor((24 * l3) / 709.0).toInt()
        val hDay = (l3 - floor((709 * hMonth) / 24.0)).toInt()
        val hYear = 30 * n + j - 30

        val monthNameEn = HIJRI_MONTHS.getOrElse(hMonth - 1) { "Muharram" }
        val monthNameUr = HIJRI_MONTHS_URDU.getOrElse(hMonth - 1) { monthNameEn }
        return HijriInfo(hDay, hMonth, hYear, monthNameEn, monthNameUr)
    }

    fun toUrduNumerals(number: Int): String {
        return number.toString().map { char ->
            when (char) {
                '0' -> '۰'
                '1' -> '۱'
                '2' -> '۲'
                '3' -> '۳'
                '4' -> '۴'
                '5' -> '۵'
                '6' -> '۶'
                '7' -> '۷'
                '8' -> '۸'
                '9' -> '۹'
                else -> char
            }
        }.joinToString("")
    }

    // --- Hindu Calendar Approximation (Vikram Samvat and Tithi) ---
    // Vikram Samvat is approximately Gregorian Year + 57
    // Lunar phase (tithi) approximation based on astronomical epoch
    fun getHinduCalendarInfo(dateStr: String): HinduCalendarInfo {
        val date = parseDate(dateStr)
        val cal = Calendar.getInstance()
        cal.time = date
        val gYear = cal.get(Calendar.YEAR)
        val vsYear = gYear + 57

        // Days since a known New Moon (e.g., New Moon on 2025-12-20)
        val epochStr = "2025-12-20"
        val epoch = parseDate(epochStr).time
        val current = parseDate(dateStr).time
        val diffMs = current - epoch
        val diffDays = diffMs / (1000.0 * 60 * 60 * 24)

        // Synodic Month = 29.53059 days
        val moonAge = (diffDays % 29.53059)
        val tithiIndex = floor((moonAge / 29.53059) * 30.0).toInt() + 1
        
        val paksha = if (tithiIndex <= 15) "Shukla" else "Krishna"
        
        // Map tithiIndex to 1-15 scale
        val localTithiNum = if (tithiIndex <= 15) tithiIndex else tithiIndex - 15
        val tithiName = when (localTithiNum) {
            1 -> "Pratipada"
            2 -> "Dwitiya"
            3 -> "Tritiya"
            4 -> "Chaturthi"
            5 -> "Panchami"
            6 -> "Shashthi"
            7 -> "Saptami"
            8 -> "Ashtami"
            9 -> "Navami"
            10 -> "Dashami"
            11 -> "Ekadashi"
            12 -> "Dwadashi"
            13 -> "Trayodashi"
            14 -> "Chaturdashi"
            15 -> if (paksha == "Shukla") "Purnima" else "Amavasya"
            else -> "Pratipada"
        }

        // Approximate Hindu Lunar month based on current solar month
        val gMonth = cal.get(Calendar.MONTH) // 0-indexed
        val vsMonth = HINDU_MONTHS[gMonth]

        return HinduCalendarInfo(
            vsYear = vsYear,
            vsMonth = vsMonth,
            tithi = tithiName,
            paksha = paksha,
            isAmavasya = tithiName == "Amavasya",
            isPurnima = tithiName == "Purnima"
        )
    }

    // --- Indian Festivals List (2025 - 2028 Embedded) ---
    fun getIndianFestivalsForDate(dateStr: String): List<String> {
        val fixedFestivals = FIXED_INDIAN_FESTIVALS[dateStr.substring(5)] // MM-DD
        val list = mutableListOf<String>()
        if (fixedFestivals != null) {
            list.add(fixedFestivals)
        }
        val movableFestival = MOVABLE_FESTIVALS[dateStr]
        if (movableFestival != null) {
            list.add(movableFestival)
        }
        return list
    }

    // --- Embedded Constants ---

    data class AllahName(
        val dayNum: Int,
        val en: String,
        val ar: String,
        val ur: String,
        val meaning: String
    ) {
        val id: Int get() = dayNum
        val transliteration: String get() = en
        val arabic: String get() = ar
        val urdu: String get() = ur
    }
    data class HinduCalendarInfo(val vsYear: Int, val vsMonth: String, val tithi: String, val paksha: String, val isAmavasya: Boolean, val isPurnima: Boolean)
    data class HijriInfo(val day: Int, val month: Int, val year: Int, val monthNameEn: String, val monthNameUr: String)

    private val HIJRI_MONTHS = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qadah", "Dhu al-Hijjah"
    )

    private val HIJRI_MONTHS_URDU = listOf(
        "محرم", "صفر", "ربیع الاول", "ربیع الثانی",
        "جمادی الاول", "جمادی الثانی", "رجب", "شعبان",
        "رمضان", "شوال", "ذی القعدہ", "ذی الحجہ"
    )

    private val HINDU_MONTHS = listOf(
        "Pausha", "Magha", "Phalguna", "Chaitra", "Vaishakha", "Jyeshtha",
        "Ashadha", "Shravana", "Bhadrapada", "Ashvina", "Kartika", "Margashirsha"
    )

    private val FIXED_INDIAN_FESTIVALS = mapOf(
        "01-01" to "New Year's Day 🎉",
        "01-14" to "Makar Sankranti / Pongal 🌾",
        "01-15" to "Indian Army Day 🪖",
        "01-26" to "Republic Day 🇮🇳",
        "03-08" to "International Women's Day ♀️",
        "04-14" to "Ambedkar Jayanti ✍️ / Baisakhi 🌾",
        "05-01" to "Labour Day 🛠️",
        "08-15" to "Independence Day 🇮🇳",
        "09-05" to "Teachers' Day 🎓",
        "10-02" to "Gandhi Jayanti 🕊️",
        "10-31" to "National Unity Day 🤝",
        "11-14" to "Children's Day 🧸",
        "12-25" to "Christmas 🎄"
    )

    // Movable festivals mapped for 2025/2026/2027/2028
    private val MOVABLE_FESTIVALS = mapOf(
        // 2025
        "2025-01-13" to "Lohri 🔥",
        "2025-01-14" to "Makar Sankranti / Pongal 🌾",
        "2025-02-26" to "Maha Shivratri 🔱",
        "2025-03-14" to "Holi 🎨",
        "2025-03-30" to "Gudi Padwa / Ugadi 🍃",
        "2025-03-31" to "Eid ul-Fitr 🌙",
        "2025-04-06" to "Rama Navami 🏹",
        "2025-04-10" to "Mahavir Jayanti 🌸",
        "2025-04-18" to "Good Friday ✝️",
        "2025-05-12" to "Buddha Purnima ☸️",
        "2025-06-07" to "Eid ul-Adha 🐐",
        "2025-07-06" to "Muharram (Ashura) 🕌",
        "2025-08-09" to "Raksha Bandhan 🎗️",
        "2025-08-16" to "Janmashtami 🪈",
        "2025-08-27" to "Ganesh Chaturthi 🐘",
        "2025-09-05" to "Milad-un-Nabi 💚",
        "2025-10-02" to "Dussehra / Vijayadashami 🏹",
        "2025-10-20" to "Diwali / Deepavali 🪔",
        "2025-10-22" to "Bhai Dooj 🤝",
        "2025-10-27" to "Chhath Puja ☀️",
        "2025-11-05" to "Guru Nanak Jayanti 🪯",

        // 2026
        "2026-01-13" to "Lohri 🔥",
        "2026-01-14" to "Makar Sankranti / Pongal 🌾",
        "2026-02-15" to "Maha Shivratri 🔱",
        "2026-03-03" to "Holi 🎨",
        "2026-03-19" to "Gudi Padwa / Ugadi 🍃",
        "2026-03-20" to "Eid ul-Fitr 🌙",
        "2026-03-27" to "Rama Navami 🏹",
        "2026-04-02" to "Mahavir Jayanti 🌸",
        "2026-04-03" to "Good Friday ✝️",
        "2026-05-02" to "Buddha Purnima ☸️",
        "2026-05-27" to "Eid ul-Adha 🐐",
        "2026-07-26" to "Muharram (Ashura) 🕌",
        "2026-08-28" to "Raksha Bandhan 🎗️",
        "2026-09-04" to "Janmashtami 🪈",
        "2026-09-15" to "Ganesh Chaturthi 🐘",
        "2026-09-25" to "Milad-un-Nabi 💚",
        "2026-10-11" to "Dussehra / Vijayadashami 🏹",
        "2026-10-20" to "Diwali / Deepavali 🪔",
        "2026-10-22" to "Bhai Dooj 🤝",
        "2026-11-15" to "Chhath Puja ☀️",
        "2026-11-24" to "Guru Nanak Jayanti 🪯",
        
        // 2027
        "2027-01-13" to "Lohri 🔥",
        "2027-01-14" to "Makar Sankranti / Pongal 🌾",
        "2027-03-06" to "Maha Shivratri 🔱",
        "2027-03-10" to "Eid ul-Fitr 🌙",
        "2027-03-22" to "Holi 🎨",
        "2027-04-07" to "Gudi Padwa / Ugadi 🍃",
        "2027-04-15" to "Rama Navami 🏹",
        "2027-04-20" to "Mahavir Jayanti 🌸",
        "2027-04-23" to "Good Friday ✝️",
        "2027-05-16" to "Eid ul-Adha 🐐",
        "2027-05-20" to "Buddha Purnima ☸️",
        "2027-07-16" to "Muharram (Ashura) 🕌",
        "2027-08-17" to "Raksha Bandhan 🎗️",
        "2027-08-25" to "Janmashtami 🪈",
        "2027-09-04" to "Ganesh Chaturthi 🐘",
        "2027-09-14" to "Milad-un-Nabi 💚",
        "2027-10-09" to "Dussehra / Vijayadashami 🏹",
        "2027-11-08" to "Diwali / Deepavali 🪔",
        "2027-11-10" to "Bhai Dooj 🤝",
        "2027-11-14" to "Guru Nanak Jayanti 🪯",
        "2027-12-04" to "Chhath Puja ☀️",

        // 2028
        "2028-01-13" to "Lohri 🔥",
        "2028-01-14" to "Makar Sankranti / Pongal 🌾",
        "2028-02-23" to "Maha Shivratri 🔱",
        "2028-03-11" to "Holi 🎨",
        "2028-03-26" to "Gudi Padwa / Ugadi 🍃",
        "2028-03-28" to "Eid ul-Fitr 🌙",
        "2028-04-03" to "Rama Navami 🏹",
        "2028-04-08" to "Mahavir Jayanti 🌸",
        "2028-04-14" to "Good Friday ✝️",
        "2028-05-08" to "Buddha Purnima ☸️",
        "2028-06-04" to "Eid ul-Adha 🐐",
        "2028-07-04" to "Muharram (Ashura) 🕌",
        "2028-08-05" to "Raksha Bandhan 🎗️",
        "2028-08-13" to "Janmashtami 🪈",
        "2028-08-24" to "Ganesh Chaturthi 🐘",
        "2028-09-02" to "Milad-un-Nabi 💚",
        "2028-10-22" to "Dussehra / Vijayadashami 🏹",
        "2028-11-07" to "Diwali / Deepavali 🪔",
        "2028-11-09" to "Bhai Dooj 🤝",
        "2028-11-18" to "Chhath Puja ☀️",
        "2028-12-02" to "Guru Nanak Jayanti 🪯"
    )

    val ALLAH_NAMES_LIST = listOf(
        AllahName(1, "Ar-Rahman", "الرحمن", "الرحمن", "The Beneficent"),
        AllahName(2, "Ar-Rahim", "الرحيم", "الرحيم", "The Merciful"),
        AllahName(3, "Al-Malik", "الملك", "الملك", "The Eternal Lord"),
        AllahName(4, "Al-Quddus", "القدوس", "القدوس", "The Most Sacred"),
        AllahName(5, "As-Salam", "السلام", "السلام", "The Embodiment of Peace"),
        AllahName(6, "Al-Mu'min", "المؤمن", "المؤمن", "The Infuser of Faith"),
        AllahName(7, "Al-Muhaymin", "المهيمن", "المهيمن", "The Preserver of Safety"),
        AllahName(8, "Al-Aziz", "العزيز", "العزيز", "The Mighty One"),
        AllahName(9, "Al-Jabbar", "الجبار", "الجبار", "The Compeller"),
        AllahName(10, "Al-Mutakabbir", "المتكبر", "المتكبر", "The Supreme"),
        AllahName(11, "Al-Khaliq", "الخالق", "الخالق", "The Creator"),
        AllahName(12, "Al-Bari", "البارئ", "البارئ", "The Maker of Order"),
        AllahName(13, "Al-Musawwir", "المصور", "المصور", "The Shaper of Beauty"),
        AllahName(14, "Al-Ghaffar", "الغفار", "الغفار", "The Forgiver"),
        AllahName(15, "Al-Qahhar", "القهار", "القهار", "The Subduer"),
        AllahName(16, "Al-Wahhab", "الوهاب", "الوهاب", "The Giver of All"),
        AllahName(17, "Ar-Razzaq", "الرزاق", "الرزاق", "The Provider"),
        AllahName(18, "Al-Fattah", "الفتاح", "الفتاح", "The Opener of Gates"),
        AllahName(19, "Al-Alim", "العليم", "العليم", "The All-Knowing"),
        AllahName(20, "Al-Qabid", "القابض", "القابض", "The Restrainer"),
        AllahName(21, "Al-Basit", "الباسط", "الباسط", "The Expander"),
        AllahName(22, "Al-Khafid", "الخافض", "الخافض", "The Abaser"),
        AllahName(23, "Ar-Rafi", "الرافع", "الرافع", "The Exalter"),
        AllahName(24, "Al-Mu'izz", "المعز", "المعز", "The Giver of Honor"),
        AllahName(25, "Al-Mudhill", "المذل", "المذل", "The Giver of Dishonor"),
        AllahName(26, "As-Sami", "السميع", "السميع", "The All-Hearing"),
        AllahName(27, "Al-Basir", "البصير", "البصير", "The All-Seeing"),
        AllahName(28, "Al-Hakam", "الحكم", "الحكم", "The Judge"),
        AllahName(29, "Al-Adl", "العدل", "العدل", "The Utterly Just"),
        AllahName(30, "Al-Latif", "اللطيف", "اللطيف", "The Subtly Kind"),
        AllahName(31, "Al-Khabir", "الخبير", "الخبير", "The All-Aware"),
        AllahName(32, "Al-Halim", "الحليم", "الحليم", "The Forbearing"),
        AllahName(33, "Al-Azim", "العظيم", "العظيم", "The Magnificent"),
        AllahName(34, "Al-Ghafur", "الغفور", "الغفور", "The Great Forgiver"),
        AllahName(35, "Ash-Shakur", "الشكور", "الشكور", "The Grateful"),
        AllahName(36, "Al-Ali", "العلي", "العلي", "The Sublime"),
        AllahName(37, "Al-Kabir", "الكبير", "الكبير", "The Infinite"),
        AllahName(38, "Al-Hafiz", "الحفيظ", "الحفيظ", "The Preserver"),
        AllahName(39, "Al-Muqit", "المقيت", "المقيت", "The Nourisher"),
        AllahName(40, "Al-Hasib", "الحسيب", "الحسيب", "The Bringer of Judgment"),
        AllahName(41, "Al-Jalil", "الجليل", "الجليل", "The Majestic"),
        AllahName(42, "Al-Karim", "الكريم", "الكريم", "The Generous"),
        AllahName(43, "Ar-Raqib", "الرقيب", "الرقيب", "The Watchful"),
        AllahName(44, "Al-Mujib", "المجيب", "المجيب", "The Responsive"),
        AllahName(45, "Al-Wasi", "الواسع", "الواسع", "The Boundless"),
        AllahName(46, "Al-Hakim", "الحكيم", "الحكيم", "The Wise"),
        AllahName(47, "Al-Wadud", "الودود", "الودود", "The Loving"),
        AllahName(48, "Al-Majid", "المجيد", "المجيد", "The All-Glorious"),
        AllahName(49, "Al-Ba'ith", "الباعث", "الباعث", "The Resurrector"),
        AllahName(50, "Ash-Shahid", "الشهيد", "الشهيد", "The Witness"),
        AllahName(51, "Al-Haqq", "الحق", "الحق", "The Absolute Truth"),
        AllahName(52, "Al-Wakil", "الوكيل", "الوكيل", "The Trustee"),
        AllahName(53, "Al-Qawi", "القوي", "القوي", "The Strong"),
        AllahName(54, "Al-Matin", "المتين", "المتين", "The Firm"),
        AllahName(55, "Al-Wali", "الولي", "الولي", "The Protecting Friend"),
        AllahName(56, "Al-Hamid", "الحميد", "الحميد", "The Praiseworthy"),
        AllahName(57, "Al-Muhsi", "المحصي", "المحصي", "The Appraiser of All"),
        AllahName(58, "Al-Mubdi", "المبدئ", "المبدئ", "The Originator"),
        AllahName(59, "Al-Mu'id", "المعيد", "المعيد", "The Restorer"),
        AllahName(60, "Al-Muhyi", "المحيي", "المحيي", "The Giver of Life"),
        AllahName(61, "Al-Mumit", "المميت", "المميت", "The Creator of Death"),
        AllahName(62, "Al-Hayy", "الحي", "الحي", "The Ever-Living"),
        AllahName(63, "Al-Qayyum", "القيوم", "القيوم", "The Self-Subsisting"),
        AllahName(64, "Al-Wajid", "الواجد", "الواجد", "The Finder"),
        AllahName(65, "Al-Majid", "الماجد", "الماجد", "The Noble"),
        AllahName(66, "Al-Wahid", "الواحد", "الواحد", "The Unique"),
        AllahName(67, "Al-Ahad", "الأحد", "الأحد", "The One"),
        AllahName(68, "As-Samad", "الصمد", "الصمد", "The Eternal Provider"),
        AllahName(69, "Al-Qadir", "القادر", "القادر", "The Omnipotent"),
        AllahName(70, "Al-Muqtadir", "المقتدر", "المقتدر", "The Determiner"),
        AllahName(71, "Al-Muqaddim", "المقدم", "المقدم", "The Expediter"),
        AllahName(72, "Al-Mu'akhkhir", "المؤخر", "المؤخر", "The Delayer"),
        AllahName(73, "Al-Awwal", "الأول", "الأول", "The First"),
        AllahName(74, "Al-Akhir", "الآخر", "الآخر", "The Last"),
        AllahName(75, "Az-Zahir", "الظاهر", "الظاهر", "The Manifest"),
        AllahName(76, "Al-Batin", "الباطن", "الباطن", "The Hidden"),
        AllahName(77, "Al-Wali", "الوالي", "الوالي", "The Patron"),
        AllahName(78, "Al-Muta'ali", "المتعالي", "المتعالي", "The Self-Exalted"),
        AllahName(79, "Al-Barr", "البر", "البر", "The Source of Goodness"),
        AllahName(80, "At-Tawwab", "التواب", "التواب", "The Acceptor of Repentance"),
        AllahName(81, "Al-Muntaqim", "المنتقم", "المنتقم", "The Avenger"),
        AllahName(82, "Al-Afu", "العفو", "العفو", "The Pardoner"),
        AllahName(83, "Ar-Ra'uf", "الرؤوف", "الرؤوف", "The Compassionate"),
        AllahName(84, "Malik-ul-Mulk", "مالك الملك", "مالك الملك", "Owner of All Sovereignty"),
        AllahName(85, "Dhul-Jalali wal-Ikram", "ذو الجلال والإكرام", "ذو الجلال والإكرام", "Lord of Majesty and Bounty"),
        AllahName(86, "Al-Muqsit", "المقسط", "المقسط", "The Equitable"),
        AllahName(87, "Al-Jami", "الجامع", "الجامع", "The Gatherer"),
        AllahName(88, "Al-Ghani", "الغني", "الغني", "The Self-Sufficient"),
        AllahName(89, "Al-Mughni", "المغني", "المغني", "The Enricher"),
        AllahName(90, "Al-Mani", "المانع", "المانع", "The Preventer"),
        AllahName(91, "Ad-Darr", "الضار", "الضار", "The Distresser"),
        AllahName(92, "An-Nafi", "النافع", "النافع", "The Benefactor"),
        AllahName(93, "An-Nur", "النور", "النور", "The Light"),
        AllahName(94, "Al-Hadi", "الهادي", "الهادي", "The Guide"),
        AllahName(95, "Al-Badi", "البديع", "البديع", "The Incomparable Originator"),
        AllahName(96, "Al-Baqi", "الباقي", "الباقي", "The Everlasting"),
        AllahName(97, "Al-Warith", "الوارث", "الوارث", "The Supreme Inheritor"),
        AllahName(98, "Ar-Rashid", "الرشيد", "الرشيد", "The Guide to Right Path"),
        AllahName(99, "As-Sabur", "الصبور", "الصبور", "The Patient One"),
        AllahName(100, "Allah", "الله", "اللہ", "The Supreme Name")
    )

    val ALLAH_NAMES get() = ALLAH_NAMES_LIST

    fun shouldShowHabitOnDate(habit: com.example.data.HabitEntity, dateStr: String): Boolean {
        val rawSDate = if (!habit.startDate.isNullOrBlank()) habit.startDate else habit.createdAt
        val sDate = rawSDate.take(10)
        val eDate = habit.endDate?.take(10)

        if (dateStr < sDate) {
            return false
        }
        if (!eDate.isNullOrBlank() && dateStr > eDate) {
            return false
        }

        val date = parseDate(dateStr)
        val baseDateStr = if (!habit.reminderDate.isNullOrBlank() && habit.repeatType.lowercase() in listOf("weekly", "monthly", "yearly", "custom")) {
            habit.reminderDate.take(10)
        } else {
            sDate
        }
        val created = parseDate(baseDateStr)

        val cal = Calendar.getInstance().apply { time = date }
        val calCreated = Calendar.getInstance().apply { time = created }

        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val dayOfWeekStr = when (dayOfWeek) {
            Calendar.SUNDAY -> "Sun"
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> ""
        }

        // Check if habit has specific days of week selected (e.g. "Mon,Wed,Fri" or "Friday")
        val customDaysList = habit.customRepeatDaysOfWeek?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        if (customDaysList.isNotEmpty()) {
            val matches = customDaysList.any { day ->
                day.equals(dayOfWeekStr, ignoreCase = true) ||
                dayOfWeekStr.equals(day.take(3), ignoreCase = true) ||
                day.startsWith(dayOfWeekStr, ignoreCase = true)
            }
            if (!matches) {
                return false
            }
        }

        return when (habit.repeatType.lowercase()) {
            "none" -> {
                dateStr == sDate
            }
            "daily" -> {
                true
            }
            "weekdays" -> {
                dayOfWeek in listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
            }
            "weekly" -> {
                if (customDaysList.isNotEmpty()) {
                    true
                } else {
                    cal.get(Calendar.DAY_OF_WEEK) == calCreated.get(Calendar.DAY_OF_WEEK)
                }
            }
            "monthly" -> {
                val targetDay = minOf(calCreated.get(Calendar.DAY_OF_MONTH), cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.get(Calendar.DAY_OF_MONTH) == targetDay
            }
            "yearly" -> {
                val targetDay = minOf(calCreated.get(Calendar.DAY_OF_MONTH), cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.get(Calendar.DAY_OF_MONTH) == targetDay && cal.get(Calendar.MONTH) == calCreated.get(Calendar.MONTH)
            }
            "custom" -> {
                val value = habit.customRepeatValue.coerceAtLeast(1)
                val cal1 = Calendar.getInstance().apply {
                    time = created
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val cal2 = Calendar.getInstance().apply {
                    time = date
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val diffMs = cal2.timeInMillis - cal1.timeInMillis
                val diffDays = java.lang.Math.round(diffMs.toDouble() / (1000.0 * 60 * 60 * 24)).toInt()

                when (habit.customRepeatUnit.lowercase()) {
                    "days" -> {
                        diffDays >= 0 && (diffDays % value == 0)
                    }
                    "weeks" -> {
                        val diffWeeks = diffDays / 7
                        val isCorrectWeek = diffWeeks >= 0 && (diffWeeks % value == 0)
                        if (customDaysList.isEmpty()) {
                            isCorrectWeek && cal.get(Calendar.DAY_OF_WEEK) == calCreated.get(Calendar.DAY_OF_WEEK)
                        } else {
                            isCorrectWeek && customDaysList.contains(dayOfWeekStr)
                        }
                    }
                    "months" -> {
                        val yearDiff = cal.get(Calendar.YEAR) - calCreated.get(Calendar.YEAR)
                        val monthDiff = cal.get(Calendar.MONTH) - calCreated.get(Calendar.MONTH) + (yearDiff * 12)
                        val isCorrectMonth = monthDiff >= 0 && (monthDiff % value == 0)
                        val targetDay = minOf(calCreated.get(Calendar.DAY_OF_MONTH), cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                        isCorrectMonth && cal.get(Calendar.DAY_OF_MONTH) == targetDay
                    }
                    "years" -> {
                        val yearDiff = cal.get(Calendar.YEAR) - calCreated.get(Calendar.YEAR)
                        val isCorrectYear = yearDiff >= 0 && (yearDiff % value == 0)
                        val targetDay = minOf(calCreated.get(Calendar.DAY_OF_MONTH), cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                        isCorrectYear && cal.get(Calendar.DAY_OF_MONTH) == targetDay &&
                                cal.get(Calendar.MONTH) == calCreated.get(Calendar.MONTH)
                    }
                    else -> true
                }
            }
            else -> true
        }
    }

    fun shouldShowTaskOnDate(task: com.example.data.TaskEntity, dateStr: String): Boolean {
        val sDate = if (!task.startDate.isNullOrBlank()) task.startDate else task.deadline
        if (dateStr < sDate) {
            return false
        }
        if (!task.endDate.isNullOrBlank() && dateStr > task.endDate) {
            return false
        }

        val date = parseDate(dateStr)
        val created = parseDate(sDate)

        val cal = Calendar.getInstance().apply { time = date }
        val calCreated = Calendar.getInstance().apply { time = created }

        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val dayOfWeekStr = when (dayOfWeek) {
            Calendar.SUNDAY -> "Sun"
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> ""
        }

        // Check if task has specific days of week selected (e.g. "Mon,Wed,Fri")
        val customDaysList = task.customRepeatDaysOfWeek?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        if (customDaysList.isNotEmpty() && !customDaysList.contains(dayOfWeekStr)) {
            return false
        }

        return when (task.repeatType.lowercase()) {
            "none" -> {
                dateStr == sDate
            }
            "daily" -> {
                if (customDaysList.isNotEmpty()) {
                    customDaysList.contains(dayOfWeekStr)
                } else {
                    true
                }
            }
            "weekdays" -> {
                dayOfWeek in listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
            }
            "weekly" -> {
                if (customDaysList.isNotEmpty()) {
                    customDaysList.contains(dayOfWeekStr)
                } else {
                    cal.get(Calendar.DAY_OF_WEEK) == calCreated.get(Calendar.DAY_OF_WEEK)
                }
            }
            "monthly" -> {
                val targetDay = minOf(calCreated.get(Calendar.DAY_OF_MONTH), cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.get(Calendar.DAY_OF_MONTH) == targetDay
            }
            "yearly" -> {
                val targetDay = minOf(calCreated.get(Calendar.DAY_OF_MONTH), cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.get(Calendar.DAY_OF_MONTH) == targetDay && cal.get(Calendar.MONTH) == calCreated.get(Calendar.MONTH)
            }
            "custom" -> {
                val value = task.customRepeatValue.coerceAtLeast(1)
                val cal1 = Calendar.getInstance().apply {
                    time = created
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val cal2 = Calendar.getInstance().apply {
                    time = date
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val diffMs = cal2.timeInMillis - cal1.timeInMillis
                val diffDays = java.lang.Math.round(diffMs.toDouble() / (1000.0 * 60 * 60 * 24)).toInt()

                when (task.customRepeatUnit.lowercase()) {
                    "days" -> {
                        diffDays >= 0 && (diffDays % value == 0)
                    }
                    "weeks" -> {
                        val diffWeeks = diffDays / 7
                        val isCorrectWeek = diffWeeks >= 0 && (diffWeeks % value == 0)
                        if (customDaysList.isEmpty()) {
                            isCorrectWeek && cal.get(Calendar.DAY_OF_WEEK) == calCreated.get(Calendar.DAY_OF_WEEK)
                        } else {
                            isCorrectWeek && customDaysList.contains(dayOfWeekStr)
                        }
                    }
                    "months" -> {
                        val yearDiff = cal.get(Calendar.YEAR) - calCreated.get(Calendar.YEAR)
                        val monthDiff = cal.get(Calendar.MONTH) - calCreated.get(Calendar.MONTH) + (yearDiff * 12)
                        val isCorrectMonth = monthDiff >= 0 && (monthDiff % value == 0)
                        val targetDay = minOf(calCreated.get(Calendar.DAY_OF_MONTH), cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                        isCorrectMonth && cal.get(Calendar.DAY_OF_MONTH) == targetDay
                    }
                    "years" -> {
                        val yearDiff = cal.get(Calendar.YEAR) - calCreated.get(Calendar.YEAR)
                        val isCorrectYear = yearDiff >= 0 && (yearDiff % value == 0)
                        val targetDay = minOf(calCreated.get(Calendar.DAY_OF_MONTH), cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                        isCorrectYear && cal.get(Calendar.DAY_OF_MONTH) == targetDay &&
                                cal.get(Calendar.MONTH) == calCreated.get(Calendar.MONTH)
                    }
                    else -> true
                }
            }
            else -> true
        }
    }
}

data class FlatColorIconSpec(val emoji: String, val name: String)

val FLAT_COLOR_ICONS = listOf(
    // 1. Productivity & Mindset (40)
    FlatColorIconSpec("🎯", "target goal focus hit aim success bow"),
    FlatColorIconSpec("📅", "calendar date schedule event appointment"),
    FlatColorIconSpec("📝", "notebook notes paper write memo journal lesson homework exam study"),
    FlatColorIconSpec("🧠", "brain mind psychology mental smart intelligence idea think thought"),
    FlatColorIconSpec("📚", "books read study learn knowledge library exam"),
    FlatColorIconSpec("💡", "lightbulb idea brainstorm inspiration light creative"),
    FlatColorIconSpec("⏰", "alarm clock time early wake morning schedule"),
    FlatColorIconSpec("⏳", "hourglass timer waiting countdown duration progress"),
    FlatColorIconSpec("🔒", "lock closed secure guard protect safety privacy"),
    FlatColorIconSpec("🔑", "key lock unlock secure door open password solution"),
    FlatColorIconSpec("💼", "briefcase work job business executive office career profession"),
    FlatColorIconSpec("🎓", "graduation cap student degree academic school college teach learn"),
    FlatColorIconSpec("💻", "laptop computer tech developer coder screen workspace office internet"),
    FlatColorIconSpec("📈", "chart graph grow increase positive profit statistics analyze scale"),
    FlatColorIconSpec("🎨", "palette paint art brush drawing design creative hobby color"),
    FlatColorIconSpec("✍️", "writing pencil pen draw draft letter text sign author"),
    FlatColorIconSpec("🎒", "backpack school bag travel hike student camp pack"),
    FlatColorIconSpec("📣", "megaphone announcement speaker loud broadcast promote alert"),
    FlatColorIconSpec("📌", "pushpin pin location tag coordinate document attach mark note"),
    FlatColorIconSpec("🏷️", "tag label retail price coupon code custom category"),
    FlatColorIconSpec("📂", "folder open document file data store collection project archive"),
    FlatColorIconSpec("📊", "bar chart analysis stats report project business presentation"),
    FlatColorIconSpec("🔋", "battery full power energy charge level source"),
    FlatColorIconSpec("🔌", "electric plug power connect charge cable wire appliance tech"),
    FlatColorIconSpec("⚙️", "gear cog settings engine mechanism customize wheel process"),
    FlatColorIconSpec("🛠️", "tools hammer wrench repair fix build craft construct engineering"),
    FlatColorIconSpec("🗣️", "speaking talking voice head communication chatter presentation feedback"),
    FlatColorIconSpec("💭", "thought bubble dream mind think reflect wish imagine ideal"),
    FlatColorIconSpec("🧘", "yoga meditation posture peace mindfulness wellness relax zen body stretch"),
    FlatColorIconSpec("🩺", "stethoscope medical doctor health check exam heartbeat clinic hospital"),
    FlatColorIconSpec("🔬", "microscope science research lab test biology blood find"),
    FlatColorIconSpec("🧬", "dna helix science gene therapy biology heritage vaccine research"),
    FlatColorIconSpec("🚀", "rocket ship launch space flying speed future startup project progress"),
    FlatColorIconSpec("🛸", "ufo alien spacecraft mystery foreign discovery space science"),
    FlatColorIconSpec("🌍", "earth world planet globe green nature geography international travels"),
    FlatColorIconSpec("🗺️", "map world travel plan navigation route adventure search"),
    FlatColorIconSpec("💎", "diamond gem wealth luxury precious accessory crystal spark"),
    FlatColorIconSpec("🏆", "trophy gold champion win first place prize award honor"),
    FlatColorIconSpec("🎖️", "military medal honor badge service soldier patriotic guard"),
    FlatColorIconSpec("🥇", "gold medal first champion winner prize award"),

    // 2. Health & Fitness (40)
    FlatColorIconSpec("🏃", "running runner sprint speed athletics race fast cardio exercise physical"),
    FlatColorIconSpec("🚶", "walking steps walk pedestrian stroll outdoors pathway movement"),
    FlatColorIconSpec("🚴", "bicycling biker transport cycle cycling outdoor cardio sport ride"),
    FlatColorIconSpec("🏊", "swimming pool beach swimmer dive wet float water cardio"),
    FlatColorIconSpec("🏋️", "weightlifting weight gym workout fitness muscle lift bodybuilder strength"),
    FlatColorIconSpec("🧗", "climbing rock adventure mountain hiking extreme sports"),
    FlatColorIconSpec("🤸", "cartwheel gymnastics acrobatics dynamic balance sport pose stretch"),
    FlatColorIconSpec("🏌️", "golf club swing green hole tournament ball leisure"),
    FlatColorIconSpec("⚽", "soccer football stadium ball team sport goal match"),
    FlatColorIconSpec("🏀", "basketball court hoops ball orange team sport slam"),
    FlatColorIconSpec("🏈", "american football stadium team sport yard pass defense"),
    FlatColorIconSpec("⚾", "baseball stadium bat ball team glove sport league"),
    FlatColorIconSpec("🎾", "tennis racket ball court set match net green sport"),
    FlatColorIconSpec("🏐", "volleyball beach net team block serve sport sand"),
    FlatColorIconSpec("🏉", "rugby team stadium tackle pass kick field sport"),
    FlatColorIconSpec("🎱", "billiards 8ball pool game stick green table pocket target"),
    FlatColorIconSpec("🥋", "martial arts gi karate judo taekwondo white fight belt sport"),
    FlatColorIconSpec("🥊", "boxing glove red punch fighter ring knock match power"),
    FlatColorIconSpec("🏹", "archery bow arrow target focus aim shoot sport outdoor"),
    FlatColorIconSpec("🎣", "fishing rod catch fish lake river bait outdoor nature"),
    FlatColorIconSpec("🛹", "skateboard board roll slide street skatepark extreme sport"),
    FlatColorIconSpec("🛶", "rowing canoe boat paddle water river lake travel kayak"),
    FlatColorIconSpec("🏄", "surfboard beach summer wave ocean water sport board"),
    FlatColorIconSpec("🏋️‍♂️", "man weightlifting gym fitness workout muscle strength power heavy lift"),
    FlatColorIconSpec("🏃‍♂️", "man running speed race sprint cardio athletics workout"),
    FlatColorIconSpec("🚴‍♂️", "man cycling road bike ride fitness outdoor sport speed"),
    FlatColorIconSpec("🤸‍♂️", "man gymnast acrobat flip balance sport stretch flexibility"),
    FlatColorIconSpec("🧘‍♂️", "man meditating lotus peace calm mindfulness zen yoga breath"),
    FlatColorIconSpec("🏌️‍♂️", "man playing golf club green fairway tournament leisure swing"),
    FlatColorIconSpec("🏊‍♂️", "man swimming pool ocean beach water sport cardio swim"),
    FlatColorIconSpec("🚣", "rowboat boat water river lake team sport paddle stroke"),
    FlatColorIconSpec("🧗‍♂️", "man climbing mountain rock scale adventure sports hike peak"),
    FlatColorIconSpec("🏋️‍♀️", "woman weightlifting gym fitness workout muscle strength power lift"),
    FlatColorIconSpec("🏃‍♀️", "woman running speed race sprint cardio athletics workout"),
    FlatColorIconSpec("🚴‍♀️", "woman cycling road bike ride fitness outdoor sport speed"),
    FlatColorIconSpec("🏊‍♀️", "woman swimming pool ocean beach water sport cardio swim"),
    FlatColorIconSpec("🧗‍♀️", "woman climbing mountain rock scale adventure sports hike peak"),
    FlatColorIconSpec("👟", "sneaker shoe run walk sport fitness footwear casual steps"),
    FlatColorIconSpec("🥾", "hiking boot outdoor trek scale climb forest footprint shoe waterproof"),
    FlatColorIconSpec("🎽", "running singlet shirt athletics marathon race vest clothing"),

    // 3. Food & Drink (40)
    FlatColorIconSpec("🍎", "apple red fruit sweet crisp fresh vitamin organic healthy snack"),
    FlatColorIconSpec("🍌", "banana yellow fruit sweet potassium energy sport nutrition snack peel"),
    FlatColorIconSpec("🍊", "orange mandarin fruit citrus sweet juice fresh vitamin c orange peel"),
    FlatColorIconSpec("🍇", "grapes fruit purple sweet vine winery berry fruit bowl bunch"),
    FlatColorIconSpec("🍓", "strawberry red berry fruit sweet summer fresh organic dessert"),
    FlatColorIconSpec("🍒", "cherries red fruit sweet summer berry double stem fruit"),
    FlatColorIconSpec("🍑", "peach pink orange fruit sweet juicy summer velvet fresh soft"),
    FlatColorIconSpec("🍍", "pineapple tropical yellow fruit sweet summer citrus crown juice"),
    FlatColorIconSpec("🥑", "avocado green fruit superfood healthy fat diet keto spread seed"),
    FlatColorIconSpec("🥦", "broccoli green vegetable veggie organic healthy fiber diet crown stem"),
    FlatColorIconSpec("🥬", "leafy green salad lettuce cabbage veggie vegetable fresh raw health"),
    FlatColorIconSpec("🥕", "carrot orange veggie vegetable fresh crunchy vitamin a rabbit eyes"),
    FlatColorIconSpec("🌽", "corn cob yellow sweet veggie vegetable kernel grain farm steam"),
    FlatColorIconSpec("🌶️", "hot pepper red chili spicy seasoning heat hot veggie vegetable flavor"),
    FlatColorIconSpec("🥔", "potato root vegetable brown veggie starch carb baked chip potato"),
    FlatColorIconSpec("🥜", "peanuts nut shell salty snack snack protein vegan spread"),
    FlatColorIconSpec("🍞", "bread slice bakery toast carb grain bakery flour loaf"),
    FlatColorIconSpec("🥐", "croissant french bakery pastry butter crescent breakfast flour"),
    FlatColorIconSpec("🧀", "cheese yellow dairy calcium block slice holes protein dairy"),
    FlatColorIconSpec("🍗", "chicken leg poultry drumstick meat protein dinner roasted fried"),
    FlatColorIconSpec("🥩", "steak beef cutlet meat raw protein grill dinner chef gourmet"),
    FlatColorIconSpec("🍔", "hamburger fastfood cheat meal bun beef cheese grease grill junk breaker"),
    FlatColorIconSpec("🍟", "french fries potato chips fastfood salty snack cheat meal grease junk breaker"),
    FlatColorIconSpec("🍕", "pizza slice fastfood cheat meal cheese pepperoni sauce junk breaker"),
    FlatColorIconSpec("🌭", "hotdog fastfood bun mustard sausage street food grill snack junk breaker"),
    FlatColorIconSpec("🌮", "taco mexican tortilla beef salsa spice lettuce wrap shell crunch"),
    FlatColorIconSpec("🍣", "sushi rolls raw fish rice japanese seaweed seafood elegant chopstick"),
    FlatColorIconSpec("🥗", "green salad bowl veggie vegetable organic healthy diet vitamin weight"),
    FlatColorIconSpec("🍿", "popcorn snack bucket movie theatre salt butter corn grain cheat breaker"),
    FlatColorIconSpec("🍪", "cookie sweet chocolate chip bakery bake treat sugar oven"),
    FlatColorIconSpec("🍩", "donut sugar sweet glaze bakery breakfast treat cheat breaker ring"),
    FlatColorIconSpec("🍫", "chocolate bar sweet cocoa dark milk sugar candy treat slab breaker"),
    FlatColorIconSpec("🍬", "candy sweet wrapper sugar treat color kids tooth sugar breaker"),
    FlatColorIconSpec("☕", "coffee mug cup caffeine morning hot tea warm steam drink energy"),
    FlatColorIconSpec("🍵", "green tea matcha cup warm drink organic relaxation ceremony oriental"),
    FlatColorIconSpec("🥛", "milk glass dairy beverage drink calcium white breakfast protein"),
    FlatColorIconSpec("🥤", "soda cup straw fastfood beverage cool sweet sweet drink cold breaker"),
    FlatColorIconSpec("🍺", "beer mug foam alcohol pub bar party celebrate toast wheat drink breaker"),
    FlatColorIconSpec("🍷", "wine glass red alcohol drink bar dinner elegant vineyard breaker"),
    FlatColorIconSpec("🍹", "cocktail tropical juice summer bar celebrate beach holiday drink orange"),

    // 4. Activities & Daily Habits (40)
    FlatColorIconSpec("😴", "sleeping face tired exhausted snooze rest bedtime dream head relaxed"),
    FlatColorIconSpec("🛌", "sleep bed rest snooze bedtime hotel night sleep relaxed"),
    FlatColorIconSpec("🚿", "shower water wash wet clean bath hygiene bathroom splash"),
    FlatColorIconSpec("🧼", "soap bubble wash clean hand hygiene skin shower bath slip"),
    FlatColorIconSpec("🪥", "toothbrush dental teeth brush paste clean morning hygiene white"),
    FlatColorIconSpec("🧹", "broom sweep clean chore tidy house dust floor brush dirt"),
    FlatColorIconSpec("🧺", "laundry basket wash clothes chore house sorting clean folded"),
    FlatColorIconSpec("🛒", "shopping cart grocery purchase buy market checkout spend store"),
    FlatColorIconSpec("🛍️", "shopping bag retail buy purchase boutique gift paper"),
    FlatColorIconSpec("🪞", "mirror reflection glass beauty cosmetic vanity grooming wall"),
    FlatColorIconSpec("💈", "hairbrush comb hair style salon groom mirror design head"),
    FlatColorIconSpec("💅", "nail polish manicure cosmetic hand finger beauty pink salon paint"),
    FlatColorIconSpec("💈", "haircut salon scissors comb style professional groom fashion"),
    FlatColorIconSpec("🧖", "spa stone wellness flower massage relaxation skin beauty therapy"),
    FlatColorIconSpec("💆", "massage relaxation body therapy spa health back head muscle"),
    FlatColorIconSpec("📻", "radio music podcast news antique tune speaker analog dial"),
    FlatColorIconSpec("📺", "television screen movie series watch broadcast monitor flat show"),
    FlatColorIconSpec("📱", "tablet computer screen touch pad work play display monitor"),
    FlatColorIconSpec("🔌", "power plug electric energy battery source adapter charger device"),
    FlatColorIconSpec("📷", "camera photo photograph shutter memory travel lens picture"),
    FlatColorIconSpec("📹", "video camera record movie tape clip filming news youtube director"),
    FlatColorIconSpec("🎙️", "microphone podcast speak record voice audio lecture host audio"),
    FlatColorIconSpec("🎧", "headphones music audio song focus noise cancel listen head ear"),
    FlatColorIconSpec("🎮", "gaming controller play console buttons play joystick video game arcade"),
    FlatColorIconSpec("🕹️", "joystick gaming console controller button play video game arcade retro"),
    FlatColorIconSpec("🃏", "playing cards deck poker blackjack gamble play fun game suit"),
    FlatColorIconSpec("🎲", "dice play boardgame random cube luck numbers dots risk play"),
    FlatColorIconSpec("🎯", "dart target focus aim strategy board game hit win central"),
    FlatColorIconSpec("🎭", "theater drama actor play mask tragedy comedy show performance screen"),
    FlatColorIconSpec("🎤", "sing song concert karaoke voice musical lyric performance artist"),
    FlatColorIconSpec("🎸", "guitar acoustic music rock instrument song band chord string"),
    FlatColorIconSpec("🎹", "piano keyboard musical synthesizer key note classical instrument melody"),
    FlatColorIconSpec("🎻", "violin musical string classic elegant bow orchestra instrument symphony"),
    FlatColorIconSpec("🎷", "saxophone jazz music instrument wind brass soul blues concert melody"),
    FlatColorIconSpec("🎺", "trumpet brass music instrument concert marching bugle fanfare band"),
    FlatColorIconSpec("🥁", "drum stick rhythm snare cymbal percussion beat band instrument music"),
    FlatColorIconSpec("⭐", "star gold yellow space rating award favorite priority outstanding sparkling"),
    FlatColorIconSpec("✨", "sparkles shiny clean new magic fairy glow star blink bright"),
    FlatColorIconSpec("🔔", "bell notification sound alarm ring alert church music metal gold"),
    FlatColorIconSpec("🌐", "globe earth world planet ocean travel continent geography mapping"),

    // 5. Breaker / Avoid / Negative Habits (40)
    FlatColorIconSpec("🚭", "no smoking tobacco cigarette breaker quit smoke free stop warning air health limit"),
    FlatColorIconSpec("📵", "no phone mobile device focus restrict screen time limit digital detox offline breaker"),
    FlatColorIconSpec("🚫", "forbidden restricted prohibited warning block cancel stop access limit boundary breaker"),
    FlatColorIconSpec("🚯", "no littering trash clean environment recycling keep tidy waste street path"),
    FlatColorIconSpec("🔞", "under eighteen age limit warning restrict content adult safe barrier"),
    FlatColorIconSpec("🛑", "stop sign traffic road cross danger warning red boundary hazard alert"),
    FlatColorIconSpec("⚠️", "caution warning danger yellow triangle hazard risk security notice guard alert"),
    FlatColorIconSpec("🥃", "whiskey glass alcohol drink liquor bar beverage shot dark heavy breaker"),
    FlatColorIconSpec("🚬", "cigarette smoking tobacco ash lighter quit smoke breaker nicotine poison habit"),
    FlatColorIconSpec("🛋️", "sofa couch lazy sedentary relax potato rest living room house sit breaker"),
    FlatColorIconSpec("🎰", "slot machine casino gambling bet risk luck lose cash coin spin breaker"),
    FlatColorIconSpec("💸", "money wings lose money fly spend budget expense costly debt waste bank breaker"),
    FlatColorIconSpec("🤬", "angry swearing curse swearing text red steam mouth anger shout limit breaker"),
    FlatColorIconSpec("💩", "poop bad waste toilet junk trash useless dump failed bad quality breaker"),
    FlatColorIconSpec("🤡", "clown silly joke mimic fool waste time distraction lazy circus breaker"),
    FlatColorIconSpec("😈", "devil evil angry purple bad mood negative demon mischief trouble breaker"),
    FlatColorIconSpec("👹", "ogre red demon mask scary japanese monster myth panic threat breaker"),
    FlatColorIconSpec("💀", "skull head bone death poison toxic threat warning skeleton danger breaker"),
    FlatColorIconSpec("🥱", "yawn tired sleep lazy bored weary morning fatigue overwork sleep debt"),
    FlatColorIconSpec("💤", "zzz sleep snoring dream rest bedtime quiet bedroom sleeping head off"),
    FlatColorIconSpec("💔", "broken heart emotional hurt break relation sad grief sorrow repair loss"),
    FlatColorIconSpec("🩹", "bandage wound plaster skin injury cut recover heal aid medical clinic first"),
    FlatColorIconSpec("🧼", "soap clean bubble hygiene wash disinfect clean hand pure"),
    FlatColorIconSpec("🪥", "toothbrush teeth clean fresh hygiene brush paste morning dental cavity protection"),
    FlatColorIconSpec("🧹", "broom sweep dust clean floor task chore clean house"),
    FlatColorIconSpec("🧺", "laundry basket dirty wash clothes wash routine organization dry clean"),
    FlatColorIconSpec("🧴", "lotion skin cosmetic care dry cream body sun protect block bottle"),
    FlatColorIconSpec("💄", "lipstick cosmetic makeup face vanity red beauty lip draw styling brush"),
    FlatColorIconSpec("💅", "nail polish fingers cosmetic polish beauty lacquer hands beauty salon colored"),
    FlatColorIconSpec("💆", "massage face head spa relaxation wellness stress peace hand pressure scalp"),
    FlatColorIconSpec("🧖", "sauna steam bath towel wellness relaxation water heat skin clean sweat"),
    FlatColorIconSpec("🚶‍♂️", "man walking outdoor road travel stride steps cardio health pace"),
    FlatColorIconSpec("🚶‍♀️", "woman walking outdoor road travel stride steps cardio health pace"),
    FlatColorIconSpec("🕺", "man dancing party music enjoy movement speed dance studio rhythm latin style"),
    FlatColorIconSpec("💃", "woman dancing red dress party latin salsa studio joy move music rhythm"),
    FlatColorIconSpec("🩺", "stethoscope heart rate check medical doctor visit pulse sound health clinic"),
    FlatColorIconSpec("🧬", "dna helix biology medicine vaccine science research human generic strand mapping"),
    FlatColorIconSpec("💊", "pill tablet medicine capsule health dose nurse doctor pharmacist drug pharmacy"),
    FlatColorIconSpec("🌡️", "thermometer fever temperature cold flu warm climate hot degrees weather"),
    FlatColorIconSpec("🩹", "adhesive bandage medical recovery cure injury care doctor clinic protect"),

    // 6. Places, Travel & Events (40)
    FlatColorIconSpec("🏠", "house building residence address home safety base roof realestate door"),
    FlatColorIconSpec("🏡", "house garden yard family estate villa trees home green suburban"),
    FlatColorIconSpec("🏢", "office workplace company corporate business city tower commercial industry workplace"),
    FlatColorIconSpec("🏫", "school class university college academy learn student teachers library building"),
    FlatColorIconSpec("🏥", "hospital emergency ward clinic cross medical nurse doctors healthcare bed building"),
    FlatColorIconSpec("🏦", "bank money safe store wealth exchange transaction deposit loan building check"),
    FlatColorIconSpec("🏨", "hotel vacation stay travel accommodation room lobby service suite luggage bed"),
    FlatColorIconSpec("🏪", "convenience store open market buy street milk snacks retail shop building"),
    FlatColorIconSpec("⛪", "church christian chapel cross prayer priest bible sunday holy building"),
    FlatColorIconSpec("🕌", "mosque crescent minaret prayer muslim islam holy ramadan masjid building worship"),
    FlatColorIconSpec("🕋", "kaaba black stone mecca saudi pilgrimage islam muslim religion holy center"),
    FlatColorIconSpec("⛺", "tent camp shelter outdoor camp forest wilderness hike campfire simple dome"),
    FlatColorIconSpec("🏕️", "campsite mountain trees forest tent outdoor night nature holiday adventure pack"),
    FlatColorIconSpec("🏖️", "beach parasol sand sun sea ocean vacation summer rest towel island wave"),
    FlatColorIconSpec("🏝️", "island palm desert ocean sea summer vacation travel paradise beach isolated"),
    FlatColorIconSpec("🏜️", "desert sand dunes yellow sun camel heat dry cactus pyramid adventure"),
    FlatColorIconSpec("🏔️", "mountain snow peak scale summit cold season winter alpine ski high steep"),
    FlatColorIconSpec("⛰️", "mountain landscape peaks hills hike climb scale country ridge outline"),
    FlatColorIconSpec("🌋", "volcano erupt lava ash smoke mountain nature disaster crater magmatic heat"),
    FlatColorIconSpec("🎡", "ferris wheel amusement park ride fair carnival fun height tickets circle swing"),
    FlatColorIconSpec("🎢", "roller coaster speed track thrill heights loop amusement park train gravity fun"),
    FlatColorIconSpec("🚂", "steam locomotive train track transport travel engine rail vintage steel trip"),
    FlatColorIconSpec("🚌", "bus passenger public transport route station transit commute travel highway vehicle"),
    FlatColorIconSpec("🚗", "car transport auto driving road vehicle travel street family sedan wheel"),
    FlatColorIconSpec("🚕", "taxi cab city ride yellow fare transport driver call passenger vehicle"),
    FlatColorIconSpec("🏍️", "motorcycle motor racer ride sport highway engine custom speed chopper bike"),
    FlatColorIconSpec("🚲", "bicycle transport ride cycle fitness cardio race spoke pedal clean city air"),
    FlatColorIconSpec("🛴", "scooter pushkick kick ride child urban transport lane wheel handle simple"),
    FlatColorIconSpec("🚨", "police siren light emergency red blue flash alert warning response vehicle"),
    FlatColorIconSpec("✈️", "airplane flight travel vacation sky clouds wing cabin pilot airport travel ticket"),
    FlatColorIconSpec("🛳️", "passenger ship cruise liner vessel ocean sea vacation tourist travel yacht port"),
    FlatColorIconSpec("⚓", "anchor ship sea harbor navy marine symbol ocean deep safety heavy metal"),
    FlatColorIconSpec("🏰", "castle fortress tower stone medieval royal king queen ancient building tourism"),
    FlatColorIconSpec("🗼", "tokyo tower landmark red metal beacon tourist travel monuments steel building"),
    FlatColorIconSpec("🗽", "statue of liberty newyork travel landmark liberty torch copper green beacon"),
    FlatColorIconSpec("⛲", "fountain park city plaza water jet architecture garden spray splash stone"),
    FlatColorIconSpec("🎪", "circus tent stripes red white show carnival performance magic tickets kids"),
    FlatColorIconSpec("🌌", "milkyway space stars galaxy universe night sky dark astrology science dust"),
    FlatColorIconSpec("🌙", "crescent moon night sky sleep dream twilight space crescent islam star"),
    FlatColorIconSpec("☀️", "sun yellow rays day heat shine dry weather climate summer light brightness"),

    // 7. Nature & Symbols (60)
    FlatColorIconSpec("⭐", "star gold yellow rating outstanding priority favorite space sparkle rank award"),
    FlatColorIconSpec("🌟", "glowing star yellow space light night shine gold priority beautiful magical"),
    FlatColorIconSpec("⚡", "high voltage spark strike lightning electricity power flash danger storm energy yellow"),
    FlatColorIconSpec("🌈", "rainbow color rain weather red orange yellow green blue indigo violet sky summer light"),
    FlatColorIconSpec("🌲", "evergreen tree pine forest wood wood logs nature alpine mountain green foliage"),
    FlatColorIconSpec("🌳", "deciduous tree green trunk leaves branch foliage shade forest countryside nature wood"),
    FlatColorIconSpec("🌴", "palm tree island tropical beach summer sunny ocean warm wind breeze leaf"),
    FlatColorIconSpec("🌵", "cactus desert dry prick green plant spike water heat mexican hot spikes"),
    FlatColorIconSpec("🌾", "sheaf of rice crop farm harvest field wheat yellow gold food grain crop"),
    FlatColorIconSpec("🌿", "herb leaves organic health cooking seasoning tea aroma green salad flora sprout"),
    FlatColorIconSpec("🍀", "four leaf clover luck stpatrick green irish field leaf magic grass charm"),
    FlatColorIconSpec("🍁", "maple leaf red orange fall autumn season nature foliage canada forest leaf"),
    FlatColorIconSpec("🍂", "fallen leaves brown wind drop seasonal dry autumn fall forest ground nature"),
    FlatColorIconSpec("🌸", "cherry blossom flower pink sakura petals spring bloom floral soft garden sweet"),
    FlatColorIconSpec("🌹", "rose flower red leaves love romantic couple garden valentine stem bloom thorn"),
    FlatColorIconSpec("🌺", "hibiscus flower tropical red pink island garden beach summer sweet flora botanical"),
    FlatColorIconSpec("🌻", "sunflower yellow large seed summer garden sun oil seeds tall bright botanical"),
    FlatColorIconSpec("🌼", "blossom yellow daisy spring flower petals garden meadow bright sweet flora"),
    FlatColorIconSpec("🌷", "tulip flower pink red spring holland garden botanical bloom soft sweet flora"),
    FlatColorIconSpec("🍄", "mushroom toadstool fungus forest ground organic toxic chef cook kitchen gourmet"),
    FlatColorIconSpec("🍃", "leaf fluttering green breeze blow spring fall season fresh air pure botanical"),
    FlatColorIconSpec("🍇", "grapes cluster fruit purple sweet wine dessert healthy vitamin vine fruit"),
    FlatColorIconSpec("🍉", "watermelon slice summer green red sweet seed juicy water hydrate slice fruit"),
    FlatColorIconSpec("🍊", "citrus fruit orange mandarin sweet juice vitamin peel food fresh grocery snack"),
    FlatColorIconSpec("🍋", "lemon yellow sour citrus vitamin tea fresh kitchen juice flavor cook acid"),
    FlatColorIconSpec("🍌", "banana sweet fruit energy yellow potassium snack peel sport grocery diet"),
    FlatColorIconSpec("🍍", "pineapple sweet crown tropical island summer juice yellow fruit acid scale"),
    FlatColorIconSpec("🍎", "apple red sweet fruit vitamin fiber healthy grocery snack seed crisp tree"),
    FlatColorIconSpec("🍏", "green apple sour fruit vitamin diet crispy healthy snack grocery seed cook"),
    FlatColorIconSpec("🍒", "cherries twin stem red sweet berry fruit summer fresh organic dessert baked"),
    FlatColorIconSpec("🍓", "strawberry red berry sweet seed dessert summer fresh healthy snack fruit botanical"),
    FlatColorIconSpec("🥑", "avocado green fat healthy diet toast keto superfood spread organic salad seed"),
    FlatColorIconSpec("🥕", "carrot orange vegetable roots beta rabbit eyes healthy veggie salad kitchen"),
    FlatColorIconSpec("🌽", "corn grain yellow sweet crop farm steam cob yellow grain sweet crop"),
    FlatColorIconSpec("🌶️", "chili pepper red hot spicy flavor seasoning heat spice organic kitchen veggie"),
    FlatColorIconSpec("🥔", "potato starch brown root carb vegetable baked frenchfry chip skin veggie"),
    FlatColorIconSpec("🍞", "bread loaf slice bakery flour grain breakfast baked toast sandwich carbohydrate"),
    FlatColorIconSpec("🧀", "cheese dairy block holes yellow slice calcium protein dairy cow farm swiss"),
    FlatColorIconSpec("🍗", "chicken drumstick meat protein poultry bone roasted fried dinner fastfood cook"),
    FlatColorIconSpec("🥩", "meat steak raw beef protein grill bbq chef kitchen cook gourmet slice"),
    FlatColorIconSpec("🍔", "burger sandwich hamburger fastfood cheat beef cheese bread sesame seed bun breaker"),
    FlatColorIconSpec("🍟", "frenchfries potato chips fastfood salty potato salt fat grease fryer snack breaker"),
    FlatColorIconSpec("🍕", "pizza slice cheese sauce pepperoni baked fastfood grease cheat meal italian breaker"),
    FlatColorIconSpec("🍿", "popcorn theater movie bag salty butter pop corn snack grain white bucket breaker"),
    FlatColorIconSpec("🥛", "milk glass calcium white drink beverage dairy shake breakfast protein child cow"),
    FlatColorIconSpec("☕", "coffee mug cup hot caffeine espresso morning steam tea beverage warm break"),
    FlatColorIconSpec("🍵", "tea green cup ceramic warm match relaxation organic asian leaf hot drink"),
    FlatColorIconSpec("🍺", "beer mug pub alcohol foam golden drink cheers party wheat brew glass breaker"),
    FlatColorIconSpec("🍷", "wine red grapes glass luxury dinner bar alcohol vineyard dine drink elegant breaker"),
    FlatColorIconSpec("🍹", "tropical cocktail drink beach summer party bar orange straw summer vacation glass"),
    FlatColorIconSpec("🥂", "champagne cheers toast glasses celebrate party wedding holiday anniversary sparkling gold"),
    FlatColorIconSpec("🧸", "teddybear soft child toy cute bear brown play sleep companion gift"),
    FlatColorIconSpec("🧹", "broom handle cleaning task chore tidy clean dust trash floor sweep bristle"),
    FlatColorIconSpec("🧼", "soap bar bubble wash foam clean hygiene hand skin lather pure bath"),
    FlatColorIconSpec("🪥", "toothbrush dental teeth brush paste clean morning hygiene white soft bristle"),
    FlatColorIconSpec("🧺", "laundry basket wash clothes chore house dry clean folded sorting organization"),
    FlatColorIconSpec("🔑", "keys keychain lock unlock home door security safe metal access find lost"),
    FlatColorIconSpec("🔒", "lock secure safety closed protection warning guard metal key code password"),
    FlatColorIconSpec("🔓", "unlock free open safety access key entry accessible permission secure custom"),
    FlatColorIconSpec("🛡️", "shield safety armor guard defend warrior protection security blue knight safe")
)

fun Modifier.bounceClick(
    minScale: Float = 0.95f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) minScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounceScale"
    )

    this
        .scale(scale)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                }
            )
        }
}

@Composable
fun CompletionBurstWrapper(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dotColor: Color = MaterialTheme.colorScheme.primary,
    dotCount: Int = 6,
    initialRadiusDp: Dp = 10.dp,
    burstRadiusMaxDp: Dp = 26.dp,
    dotRadiusDp: Dp = 3.dp,
    content: @Composable () -> Unit
) {
    var triggerCount by remember { mutableStateOf(0) }
    val progress = remember { Animatable(0f) }
    val buttonScale = remember { Animatable(1f) }

    LaunchedEffect(triggerCount) {
        if (triggerCount > 0) {
            launch {
                buttonScale.snapTo(0.82f)
                buttonScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                progress.snapTo(0f)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
                )
            }
        }
    }

    Box(
        modifier = modifier
            .scale(buttonScale.value)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                triggerCount++
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        content()

        if (progress.value > 0f && progress.value < 1f) {
            val p = progress.value
            Canvas(modifier = Modifier.matchParentSize()) {
                val center = center
                val startDist = initialRadiusDp.toPx()
                val maxDist = burstRadiusMaxDp.toPx()
                val currentDist = startDist + (maxDist - startDist) * p
                val currentDotRadius = dotRadiusDp.toPx() * (1f - p * 0.4f)
                val alpha = (1f - p).coerceIn(0f, 1f)

                for (i in 0 until dotCount) {
                    val angleRad = Math.toRadians(i * (360.0 / dotCount))
                    val x = center.x + currentDist * cos(angleRad).toFloat()
                    val y = center.y + currentDist * sin(angleRad).toFloat()

                    drawCircle(
                        color = dotColor.copy(alpha = alpha),
                        radius = currentDotRadius,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}


