package com.example.utils

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
            sb.append("{\"id\":\"${subTask.id}\",\"title\":\"$escapedTitle\",\"completed\":${subTask.completed}}")
            if (index < subtasks.size - 1) sb.append(",")
        }
        sb.append("]")
        return sb.toString()
    }

    fun deserializeSubTasks(json: String): List<SubTask> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<SubTask>()
        try {
            // Simple manual parse for safety
            val regex = """\{"id":"([^"]*)","title":"([^"]*)","completed":(true|false)\}""".toRegex()
            val matches = regex.findAll(json)
            for (match in matches) {
                val id = match.groupValues[1]
                val title = match.groupValues[2].replace("\\\"", "\"")
                val completed = match.groupValues[3].toBoolean()
                list.add(SubTask(id, title, completed))
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

    fun isBeforeLaunch(dateStr: String): Boolean {
        return dateStr < APP_LAUNCH_DATE
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
        val l = jd - 1948440 + 10632
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

    data class AllahName(val dayNum: Int, val en: String, val ar: String, val ur: String, val meaning: String)
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

    private val ALLAH_NAMES_LIST = listOf(
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
}
