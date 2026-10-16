package com.tigermatrix.app.assistant

/** أدوات تنظيف النص العربي. normalize يحافظ على طول النص حتى نقدر نقص الأجزاء من النص الأصلي. */
object ArabicText {
    private val diacritics = Regex("[\\u064B-\\u065F\\u0670\\u0640]")

    fun stripDiacritics(s: String): String = s.replace(diacritics, "")

    fun normalize(s: String): String = s.lowercase()
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ة', 'ه')
        .replace('ى', 'ي')
}
