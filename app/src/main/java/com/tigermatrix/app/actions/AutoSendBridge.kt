package com.tigermatrix.app.actions

/**
 * جسر بين المساعد وخدمة إمكانية الوصول.
 * الخدمة ما بتضغط "إرسال" إلا إذا المساعد "سلّح" الجسر قبل أقل من 30 ثانية لنفس التطبيق.
 * (بدون هالتسليح الخدمة ما بتعمل شي نهائياً.)
 */
object AutoSendBridge {
    private const val ARM_WINDOW_MS = 30_000L
    const val MIN_INTERVAL_MS = 10_000L

    @Volatile private var armedPackage: String? = null
    @Volatile private var expiresAt: Long = 0L
    @Volatile private var lastSentAt: Long = 0L

    fun arm(pkg: String) {
        armedPackage = pkg
        expiresAt = System.currentTimeMillis() + ARM_WINDOW_MS
    }

    fun disarm() {
        armedPackage = null
        expiresAt = 0L
    }

    fun isArmedFor(pkg: String): Boolean =
        armedPackage == pkg && System.currentTimeMillis() < expiresAt

    /** حماية من الإرسال المتكرر السريع (بتقلل خطر حظر واتساب). */
    fun canArmNow(): Boolean = System.currentTimeMillis() - lastSentAt >= MIN_INTERVAL_MS

    fun markSent() {
        lastSentAt = System.currentTimeMillis()
        disarm()
    }
}
