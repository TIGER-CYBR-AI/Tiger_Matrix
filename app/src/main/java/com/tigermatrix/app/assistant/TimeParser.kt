package com.tigermatrix.app.assistant

import com.tigermatrix.app.tools.ExpressionEvaluator
import java.util.Calendar

data class ParsedTime(val title: String, val triggerAt: Long?)

/**
 * يفهم أوقات التذكير بالعربي: "بعد ساعة"، "بكرا الساعة 5 مساء"، "الساعة 8 ونص"، "بعد يومين"...
 * إذا ما لقى وقت واضح بيرجع triggerAt = null والنص الأصلي كما هو.
 */
object TimeParser {

    private enum class Period { AM, NOON, PM, NIGHT }

    private data class Relative(val minutes: Long, val days: Int, val end: Int)

    private val wordNumbers = mapOf(
        "واحده" to 1, "واحد" to 1, "وحده" to 1,
        "اثنين" to 2, "اتنين" to 2, "تنتين" to 2, "ثنتين" to 2,
        "ثلاثه" to 3, "تلاته" to 3, "ثلاث" to 3, "تلات" to 3,
        "اربعه" to 4, "اربع" to 4,
        "خمسه" to 5, "خمس" to 5,
        "سته" to 6, "ست" to 6,
        "سبعه" to 7, "سبع" to 7,
        "ثمانيه" to 8, "تمانيه" to 8, "ثمان" to 8, "تمان" to 8,
        "تسعه" to 9, "تسع" to 9,
        "عشره" to 10, "عشر" to 10,
        "احدعش" to 11, "حداشر" to 11,
        "اثنعش" to 12, "اتنعش" to 12, "تنعش" to 12
    )

    private val minuteUnits = setOf("دقيقه", "دقايق", "دقائق", "دقيقتين")
    private val hourUnits = setOf("ساعه", "ساعات", "ساعتين")
    private val dayUnits = setOf("يوم", "ايام", "يومين")
    private val weekUnits = setOf("اسبوع", "اسابيع", "اسبوعين")
    private val dualUnits = setOf("دقيقتين", "ساعتين", "يومين", "اسبوعين")

    private val todayWords = setOf("اليوم")
    private val tonightWords = setOf("الليله")
    private val tomorrowWords = setOf("بكرا", "بكره", "غدا", "غد", "باكر")

    private val fractionMinutes = mapOf("نص" to 30, "نصف" to 30, "ربع" to 15, "ثلث" to 20)

    private val connectors = setOf("في", "على", "ع", "عند", "ب", "لي", "ان", "انو", "اني", "الى", "ل", "و")

    private val clockRegex = Regex("^(\\d{1,2})[:.](\\d{2})$")

    private fun normTok(t: String): String =
        ArabicText.normalize(ArabicText.stripDiacritics(ExpressionEvaluator.normalizeDigits(t)))
            .trim(',', '،', '.', '!', '؟', '?', ';', '؛', ':')

    private fun numberOf(tok: String): Int? = tok.toIntOrNull() ?: wordNumbers[tok]

    private fun isIn(tok: String?, set: Set<String>): Boolean = tok != null && tok in set

    private fun periodOf(tok: String): Period? = when (tok) {
        "صباحا", "صباح", "الصبح", "صبحا", "فجرا", "الفجر" -> Period.AM
        "ظهرا", "الظهر" -> Period.NOON
        "عصرا", "العصر", "مساء", "مساءا", "المسا", "مسا" -> Period.PM
        "ليلا", "الليل", "بالليل" -> Period.NIGHT
        else -> null
    }

    private fun parseClock(tok: String): Pair<Int, Int>? {
        val m = clockRegex.matchEntire(tok)
        if (m != null) {
            val h = m.groupValues[1].toInt()
            val mm = m.groupValues[2].toInt()
            return if (h in 0..23 && mm in 0..59) Pair(h, mm) else null
        }
        val h = numberOf(tok) ?: return null
        return if (h in 0..23) Pair(h, 0) else null
    }

    private fun parseRelative(norm: List<String>, start: Int): Relative? {
        val first = norm[start]
        if (first in tomorrowWords) return Relative(0, 2, start)

        var amount = 1
        var fraction: Double? = null
        val unitIdx: Int
        val next = norm.getOrNull(start + 1)
        if ((first == "نص" || first == "نصف") && isIn(next, hourUnits)) {
            fraction = 0.5
            unitIdx = start + 1
        } else if (first == "ربع" && isIn(next, hourUnits)) {
            fraction = 0.25
            unitIdx = start + 1
        } else if (numberOf(first) != null) {
            amount = numberOf(first) ?: 1
            unitIdx = start + 1
        } else {
            unitIdx = start
        }

        val unit = norm.getOrNull(unitIdx) ?: return null
        if (unit in dualUnits && unitIdx == start) amount = 2

        val minutes: Long
        val days: Int
        if (unit in minuteUnits) {
            minutes = amount.toLong()
            days = 0
        } else if (unit in hourUnits) {
            minutes = ((fraction ?: amount.toDouble()) * 60).toLong()
            days = 0
        } else if (unit in dayUnits) {
            minutes = 0
            days = amount
        } else if (unit in weekUnits) {
            minutes = 0
            days = amount * 7
        } else {
            return null
        }
        return Relative(minutes, days, unitIdx)
    }

    fun parse(text: String, now: Long = System.currentTimeMillis()): ParsedTime {
        val original = text.trim()
        if (original.isEmpty()) return ParsedTime(original, null)

        val tokens = original.split(Regex("\\s+"))
        val norm = tokens.map { normTok(it) }
        val used = BooleanArray(tokens.size)

        var relativeMinutes: Long? = null
        var dayOffset: Int? = null
        var hour: Int? = null
        var minute = 0
        var minus = 0
        var period: Period? = null
        var tonight = false

        // 1) "بعد الظهر"
        for (a in 0 until norm.size - 1) {
            if (!used[a] && !used[a + 1] && norm[a] == "بعد" && norm[a + 1] == "الظهر") {
                period = Period.PM
                used[a] = true
                used[a + 1] = true
            }
        }

        // 2) "بعد" + مدة
        var b = 0
        while (b < norm.size) {
            if (!used[b] && norm[b] == "بعد" && b + 1 < norm.size && !used[b + 1]) {
                val rel = parseRelative(norm, b + 1)
                if (rel != null) {
                    if (rel.days > 0) dayOffset = rel.days else relativeMinutes = rel.minutes
                    for (k in b..rel.end) used[k] = true
                    break
                }
            }
            b++
        }

        // 3) كلمات اليوم
        for (k in norm.indices) {
            if (used[k]) continue
            val t = norm[k]
            if (t in tomorrowWords && dayOffset == null) {
                dayOffset = 1
                used[k] = true
            } else if (t in todayWords && dayOffset == null) {
                dayOffset = 0
                used[k] = true
            } else if (t in tonightWords) {
                dayOffset = 0
                tonight = true
                used[k] = true
            }
        }

        // 4) "الساعة" + رقم (+ ونص / وربع / الا ربع)
        for (k in norm.indices) {
            if (used[k] || norm[k] != "الساعه") continue
            val n = k + 1
            if (n >= norm.size || used[n]) continue
            val hm = parseClock(norm[n]) ?: continue
            hour = hm.first
            minute = hm.second
            used[k] = true
            used[n] = true
            var j = n + 1
            while (j < norm.size && !used[j]) {
                val t = norm[j]
                val glued = if (t.startsWith("و")) fractionMinutes[t.drop(1)] else null
                if (glued != null) {
                    minute = glued
                    used[j] = true
                    j++
                } else if (t == "و" && j + 1 < norm.size && fractionMinutes.containsKey(norm[j + 1])) {
                    minute = fractionMinutes[norm[j + 1]] ?: 0
                    used[j] = true
                    used[j + 1] = true
                    j += 2
                } else if (t == "الا" && j + 1 < norm.size && (norm[j + 1] == "ربع" || norm[j + 1] == "ثلث")) {
                    minus = if (norm[j + 1] == "ربع") 15 else 20
                    used[j] = true
                    used[j + 1] = true
                    j += 2
                } else {
                    break
                }
            }
            break
        }

        // 5) "8 مساء" بدون كلمة الساعة
        if (hour == null) {
            for (k in 0 until norm.size - 1) {
                if (used[k]) continue
                if (periodOf(norm[k + 1]) == null) continue
                val hm = parseClock(norm[k]) ?: continue
                hour = hm.first
                minute = hm.second
                used[k] = true
                break
            }
        }

        // 6) كلمات الفترة
        for (k in norm.indices) {
            if (used[k]) continue
            val p = periodOf(norm[k]) ?: continue
            if (period == null) period = p
            used[k] = true
        }
        if (period == null && tonight) period = Period.NIGHT

        val rel = relativeMinutes
        val trigger: Long? = if (rel != null && hour == null && dayOffset == null) {
            now + rel * 60_000L
        } else {
            computeClock(now, dayOffset, hour, minute, minus, period)
        }
        if (trigger == null || trigger <= now) return ParsedTime(original, null)

        // العنوان = الكلمات اللي ما استهلكناها
        val titleTokens = tokens.filterIndexed { idx, _ -> !used[idx] }.toMutableList()
        while (titleTokens.isNotEmpty() && normTok(titleTokens.first()) in connectors) {
            titleTokens.removeAt(0)
        }
        while (titleTokens.isNotEmpty() && normTok(titleTokens.last()) in connectors) {
            titleTokens.removeAt(titleTokens.lastIndex)
        }
        val title = titleTokens.joinToString(" ").trim().ifBlank { "تذكير" }
        return ParsedTime(title, trigger)
    }

    private fun hourCandidates(h: Int, period: Period?, hasDay: Boolean): List<Int> {
        if (h > 12) return listOf(h)
        if (h == 0) return listOf(0)
        return when (period) {
            Period.AM -> listOf(if (h == 12) 0 else h)
            Period.NOON -> listOf(
                if (h == 12) 12 else if (h in 1..5) h + 12 else h
            )
            Period.PM -> listOf(if (h == 12) 12 else h + 12)
            Period.NIGHT -> listOf(
                if (h == 12) 0 else if (h in 6..11) h + 12 else h
            )
            null -> {
                val am = h % 12
                val pm = h % 12 + 12
                if (hasDay) {
                    listOf(if (h == 12) 12 else if (h in 1..6) pm else am)
                } else {
                    listOf(am, pm)
                }
            }
        }
    }

    private fun timeOn(now: Long, dayOffset: Int, h: Int, m: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun computeClock(
        now: Long,
        dayOffset: Int?,
        hour: Int?,
        minute: Int,
        minus: Int,
        period: Period?
    ): Long? {
        val candidates: List<Pair<Int, Int>>
        if (hour == null) {
            if (dayOffset == null) return null
            if (dayOffset == 0 && period == null) return null
            val h = when (period) {
                Period.AM -> 9
                Period.NOON -> 12
                Period.PM -> 17
                Period.NIGHT -> 21
                null -> 9
            }
            candidates = listOf(Pair(h, 0))
        } else {
            candidates = hourCandidates(hour, period, dayOffset != null).map { h ->
                val total = ((h * 60 + minute - minus) % 1440 + 1440) % 1440
                Pair(total / 60, total % 60)
            }
        }

        val day = dayOffset ?: 0
        val times = candidates.map { timeOn(now, day, it.first, it.second) }
        val future = times.filter { it > now }.minOrNull()
        if (future != null) return future

        if (dayOffset == null) {
            // كل الاحتمالات فاتت اليوم: بكرا
            val idx = if (candidates.size == 1) 0 else if (hour != null && hour in 7..11) 0 else 1
            val c = candidates[idx]
            return timeOn(now, 1, c.first, c.second)
        }
        return null
    }
}
