package com.tigermatrix.app.actions

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object AccessibilityStatus {
    /** هل المستخدم فعّل خدمة الإرسال التلقائي من إعدادات النظام؟ */
    fun isServiceEnabled(context: Context): Boolean {
        val enabled: String? = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        } catch (e: Exception) {
            null
        }
        if (enabled.isNullOrEmpty()) return false
        val component = ComponentName(context, WhatsAppAutoSendService::class.java)
        val full = component.flattenToString()
        val short = component.flattenToShortString()
        return enabled.split(':').any { it.equals(full, true) || it.equals(short, true) }
    }
}
