package com.tigermatrix.app.assistant

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeFormat {
    private val arabic: Locale = Locale.forLanguageTag("ar")

    fun clock(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        val h = c.get(Calendar.HOUR_OF_DAY)
        val m = c.get(Calendar.MINUTE)
        val h12 = if (h % 12 == 0) 12 else h % 12
        val suffix = if (h < 12) "صباحاً" else "مساءً"
        return "$h12:${m.toString().padStart(2, '0')} $suffix"
    }

    private fun startOfDay(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun daysBetween(from: Long, to: Long): Int =
        Math.round((startOfDay(to) - startOfDay(from)) / 86_400_000.0).toInt()

    fun weekday(millis: Long): String = SimpleDateFormat("EEEE", arabic).format(Date(millis))

    fun dateFull(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return "${weekday(millis)} ${c.get(Calendar.DAY_OF_MONTH)}/${c.get(Calendar.MONTH) + 1}/${c.get(Calendar.YEAR)}"
    }

    fun describe(millis: Long, now: Long = System.currentTimeMillis()): String {
        val dayText = when (daysBetween(now, millis)) {
            0 -> "اليوم"
            1 -> "بكرا"
            2 -> "بعد بكرا"
            -1 -> "مبارح"
            else -> {
                val c = Calendar.getInstance().apply { timeInMillis = millis }
                "${weekday(millis)} ${c.get(Calendar.DAY_OF_MONTH)}/${c.get(Calendar.MONTH) + 1}"
            }
        }
        return "$dayText الساعة ${clock(millis)}"
    }
}
