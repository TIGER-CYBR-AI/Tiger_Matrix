package com.tigermatrix.app.actions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.tigermatrix.app.tools.ExpressionEvaluator

/** اتصال، SMS، وفتح محادثة واتساب. كل دالة ترجع true/false وما بترمي استثناءات. */
object PhoneActions {
    const val PKG_WHATSAPP = "com.whatsapp"
    const val PKG_BUSINESS = "com.whatsapp.w4b"

    fun looksLikeNumber(s: String): Boolean {
        val t = ExpressionEvaluator.normalizeDigits(s).trim()
        return t.count { it.isDigit() } >= 6 && t.all { it.isDigit() || it in "+-() " }
    }

    private fun cleanNumber(s: String): String = ExpressionEvaluator.normalizeDigits(s).trim()

    /** يحوّل الرقم لصيغة دولية بدون + (المطلوبة لروابط واتساب). */
    fun normalizeForWhatsApp(number: String, countryCode: String): String {
        val raw = cleanNumber(number)
        val digits = raw.filter { it.isDigit() }
        return when {
            raw.startsWith("+") -> digits
            digits.startsWith("00") -> digits.drop(2)
            digits.startsWith("0") -> countryCode + digits.drop(1)
            else -> digits
        }
    }

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun canCallDirect(context: Context): Boolean = granted(context, Manifest.permission.CALL_PHONE)

    fun canSendSms(context: Context): Boolean = granted(context, Manifest.permission.SEND_SMS)

    fun call(context: Context, number: String, direct: Boolean): Boolean {
        return try {
            val uri = Uri.parse("tel:" + Uri.encode(cleanNumber(number)))
            val action = if (direct) Intent.ACTION_CALL else Intent.ACTION_DIAL
            context.startActivity(Intent(action, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun sendSms(context: Context, number: String, text: String): Boolean {
        return try {
            val manager: SmsManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val sm = manager ?: return false
            val dest = cleanNumber(number)
            val parts = sm.divideMessage(text)
            if (parts.size > 1) {
                sm.sendMultipartTextMessage(dest, null, parts, null, null)
            } else {
                sm.sendTextMessage(dest, null, text, null, null)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun composeSms(context: Context, number: String, text: String): Boolean {
        return try {
            val uri = Uri.parse("smsto:" + Uri.encode(cleanNumber(number)))
            val intent = Intent(Intent.ACTION_SENDTO, uri)
                .putExtra("sms_body", text)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isInstalled(context: Context, pkg: String): Boolean =
        try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: Exception) {
            false
        }

    /** pref: 0 تلقائي، 1 واتساب، 2 واتساب بزنس */
    fun pickWhatsApp(context: Context, pref: Int): String? {
        val normal = isInstalled(context, PKG_WHATSAPP)
        val business = isInstalled(context, PKG_BUSINESS)
        return when (pref) {
            1 -> if (normal) PKG_WHATSAPP else null
            2 -> if (business) PKG_BUSINESS else null
            else -> if (normal) PKG_WHATSAPP else if (business) PKG_BUSINESS else null
        }
    }

    fun openWhatsAppChat(context: Context, pkg: String, waNumber: String, text: String): Boolean {
        return try {
            val uri = Uri.parse("https://wa.me/$waNumber?text=" + Uri.encode(text))
            val intent = Intent(Intent.ACTION_VIEW, uri)
                .setPackage(pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
