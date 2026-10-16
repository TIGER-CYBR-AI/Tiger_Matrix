package com.tigermatrix.app.assistant

import java.util.Calendar

/**
 * محلل أوامر بالقواعد (بدون إنترنت). القوائم تُكتب بصيغة طبيعية وتُطبَّع تلقائياً (ة→ه، أ→ا، ى→ي).
 * ترتيب الفحص مهم: الأوامر الأخص أول، والأعم آخر.
 */
object CommandParser {

    private fun prepare(list: List<String>) =
        list.map { ArabicText.normalize(it) }.sortedByDescending { it.length }

    private val helpWords = prepare(listOf("مساعدة", "help", "اوامر", "الاوامر", "شو بتعرف", "شو تقدر"))
    private val undoWords = prepare(listOf("تراجع", "ارجع", "الغاء اخر امر", "الغي اخر امر"))
    private val timeNowWords = prepare(listOf("كم الساعة", "شو الساعة", "الساعة كم", "الساعة الان"))
    private val dateWords = prepare(listOf("شو التاريخ", "التاريخ", "شو اليوم", "اي يوم اليوم", "كم التاريخ"))
    private val listReminderWords = prepare(listOf("اعرض التذكيرات", "عرض التذكيرات", "شو تذكيراتي", "تذكيراتي", "التذكيرات"))
    private val listTaskWords = prepare(listOf("اعرض المهام", "عرض المهام", "وريني المهام", "شو مهامي", "مهامي", "المهام"))
    private val listNoteWords = prepare(listOf("اعرض الملاحظات", "عرض الملاحظات", "وريني الملاحظات", "ملاحظاتي", "الملاحظات", "اخر الملاحظات"))

    private val flashOnWords = prepare(listOf("شغل الفلاش", "افتح الفلاش", "اشعل الفلاش", "شغل الكشاف", "افتح الكشاف", "شغل الضو", "شغل الضوء"))
    private val flashOffWords = prepare(listOf("طفي الفلاش", "اطفي الفلاش", "سكر الفلاش", "طفي الكشاف", "اطفي الكشاف", "سكر الكشاف", "طفي الضو"))

    private val deleteNotePrefixes = prepare(listOf("احذف ملاحظة", "امسح ملاحظة", "شيل ملاحظة", "الغي ملاحظة"))
    private val deleteTaskPrefixes = prepare(listOf("احذف مهمة", "امسح مهمة", "شيل مهمة", "الغي مهمة"))
    private val completePrefixes = prepare(listOf("خلصت مهمة", "انهيت مهمة", "انجزت مهمة", "تمت مهمة", "خلصت", "انجزت", "انهيت", "خلص"))

    private val calcPrefixes = prepare(listOf("احسب", "كم يساوي", "كم ناتج"))
    private val searchPrefixes = prepare(listOf("ابحث عن", "دور على", "فتش عن", "ابحث"))
    private val alarmPrefixes = prepare(listOf("اضبط منبه", "اضبطلي منبه", "ضبط منبه", "منبه", "نبهني", "صحيني", "صحني"))

    private val smsPrefixes = prepare(
        listOf("ابعت رسالة نصية", "ارسل رسالة نصية", "ابعت sms", "ارسل sms", "ابعت مسج", "ارسل مسج")
    )
    private val waPrefixes = prepare(
        listOf(
            "ابعت واتساب", "ابعت واتس", "ارسل واتساب", "ارسل واتس",
            "ابعت رسالة واتساب", "ابعت رسالة واتس", "ارسل رسالة واتساب",
            "ابعت رسالة على الواتس", "ابعت رسالة عالواتس",
            "ابعت رسالة", "ارسل رسالة", "راسل"
        )
    )
    private val callPrefixes = prepare(
        listOf("اتصل ب", "اتصل على", "اتصل", "كلم", "اعمل مكالمة ل", "اعمل اتصال ب", "رن على")
    )

    private val openPrefixes = prepare(listOf("افتح تطبيق", "شغل تطبيق", "افتح", "شغل", "روح على"))
    private val reminderPrefixes = prepare(listOf("ذكرني"))
    private val notePrefixes = prepare(
        listOf(
            "اضف ملاحظة", "ضيف ملاحظة", "اضف لي ملاحظة", "ضيف لي ملاحظة", "ضيفلي ملاحظة",
            "اضافة ملاحظة", "سجل ملاحظة", "سجل لي ملاحظة", "ملاحظة جديدة", "ملاحظة", "دون", "سجل"
        )
    )
    private val taskPrefixes = prepare(
        listOf(
            "اضف مهمة", "ضيف مهمة", "اضف لي مهمة", "ضيف لي مهمة", "ضيفلي مهمة",
            "اضافة مهمة", "مهمة جديدة", "مهمة", "ضيف", "اضف"
        )
    )

    private val mathOnly = Regex("^[0-9٠-٩+\\-*/().×÷\\s٫]+$")

    fun parse(input: String, now: Long = System.currentTimeMillis()): Command {
        val clean = ArabicText.stripDiacritics(input).trim()
        val m = ArabicText.normalize(clean)
        if (m.isEmpty()) return Command.Unknown

        if (matchesAny(m, helpWords)) return Command.Help
        if (matchesAny(m, undoWords)) return Command.Undo
        if (matchesAny(m, timeNowWords)) return Command.TimeNow
        if (matchesAny(m, dateWords)) return Command.DateToday
        if (matchesAny(m, listReminderWords)) return Command.ListReminders
        if (matchesAny(m, listTaskWords)) return Command.ListTasks
        if (matchesAny(m, listNoteWords)) return Command.ListNotes

        if (matchesAny(m, flashOnWords)) return Command.Flashlight(true)
        if (matchesAny(m, flashOffWords)) return Command.Flashlight(false)

        stripPrefix(m, clean, deleteNotePrefixes)?.let { return Command.DeleteNote(it) }
        stripPrefix(m, clean, deleteTaskPrefixes)?.let { return Command.DeleteTask(it) }
        stripPrefix(m, clean, completePrefixes)?.let { return Command.CompleteTask(it) }

        stripPrefix(m, clean, calcPrefixes)?.let { return Command.Calc(it) }
        stripPrefix(m, clean, searchPrefixes)?.let { return Command.Search(it) }

        stripPrefix(m, clean, alarmPrefixes)?.let { rest ->
            val p = TimeParser.parse(rest, now)
            val t = p.triggerAt ?: return Command.Reply(Responses.ALARM_NEEDS_TIME)
            val cal = Calendar.getInstance().apply { timeInMillis = t }
            val label = if (p.title == "تذكير") "" else p.title
            return Command.SetAlarm(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), label)
        }

        stripPrefix(m, clean, smsPrefixes)?.let { return Command.SendSms(it) }
        stripPrefix(m, clean, waPrefixes)?.let { return Command.SendWhatsApp(it) }
        stripPrefix(m, clean, callPrefixes)?.let {
            return if (it.isBlank()) Command.Reply(Responses.CALL_MISSING) else Command.Call(it)
        }

        stripPrefix(m, clean, openPrefixes)?.let {
            return if (it.isBlank()) Command.Reply(Responses.OPEN_MISSING) else Command.OpenApp(it)
        }

        stripPrefix(m, clean, reminderPrefixes)?.let { rest ->
            if (rest.isBlank()) return Command.Reply(Responses.REMINDER_MISSING)
            val p = TimeParser.parse(rest, now)
            val t = p.triggerAt
            return if (t != null) Command.AddReminder(p.title, t) else Command.AddTask(rest, needTimeHint = true)
        }

        stripPrefix(m, clean, notePrefixes)?.let { return Command.AddNote(it) }

        stripPrefix(m, clean, taskPrefixes)?.let { rest ->
            if (rest.isBlank()) return Command.AddTask("")
            val p = TimeParser.parse(rest, now)
            val t = p.triggerAt
            return if (t != null) Command.AddReminder(p.title, t) else Command.AddTask(rest)
        }

        if (mathOnly.matches(m) && m.any { it.isDigit() } && m.any { it in "+-*/×÷" }) {
            return Command.Calc(clean)
        }
        return Command.Unknown
    }

    private fun matchesAny(m: String, words: List<String>): Boolean = words.any { w ->
        m == w || (m.startsWith(w) && !m[w.length].isLetter())
    }

    /** إذا النص يبدأ بإحدى البادئات، يرجع الباقي (من النص الأصلي)، وإلا null. */
    private fun stripPrefix(m: String, clean: String, prefixes: List<String>): String? {
        for (p in prefixes) {
            if (m.startsWith(p)) {
                val rest = m.substring(p.length)
                if (rest.isEmpty() || !rest[0].isLetter()) {
                    return clean.substring(p.length).trim().trimStart(':', '،', ',', '-').trim()
                }
            }
        }
        return null
    }
}
