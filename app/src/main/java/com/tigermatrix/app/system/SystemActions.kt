package com.tigermatrix.app.system

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.AlarmClock
import com.tigermatrix.app.assistant.ArabicText
import com.tigermatrix.app.assistant.TimeFormat
import java.util.Calendar

/** أوامر النظام: فتح تطبيقات، منبه، فلاش. كل دالة ترجع نص الرد وما بترمي استثناءات. */
object SystemActions {

    private val aliases: Map<String, List<String>> = mapOf(
        "واتساب" to listOf("whatsapp"),
        "واتس" to listOf("whatsapp"),
        "واتساب بزنس" to listOf("w4b"),
        "يوتيوب" to listOf("youtube"),
        "تلغرام" to listOf("telegram"),
        "تيليجرام" to listOf("telegram"),
        "تلجرام" to listOf("telegram"),
        "فيسبوك" to listOf("facebook"),
        "فيس" to listOf("facebook"),
        "انستغرام" to listOf("instagram"),
        "انستجرام" to listOf("instagram"),
        "انستا" to listOf("instagram"),
        "كاميرا" to listOf("camera"),
        "الاعدادات" to listOf("settings"),
        "اعدادات" to listOf("settings"),
        "الساعه" to listOf("clock"),
        "الحاسبه" to listOf("calculator"),
        "حاسبه" to listOf("calculator"),
        "الخرائط" to listOf("maps"),
        "خرائط" to listOf("maps"),
        "كروم" to listOf("chrome"),
        "تيك توك" to listOf("tiktok"),
        "سناب" to listOf("snapchat"),
        "جيميل" to listOf("gmail"),
        "الصور" to listOf("photos", "gallery"),
        "المعرض" to listOf("gallery", "photos"),
        "الهاتف" to listOf("dialer", "phone"),
        "الرسائل" to listOf("messaging", "messages", "mms"),
        "الملفات" to listOf("files"),
        "ماسنجر" to listOf("messenger"),
        "تويتر" to listOf("twitter"),
        "المتجر" to listOf("vending"),
        "بلاي ستور" to listOf("vending")
    ).mapKeys { ArabicText.normalize(it.key) }

    fun openApp(context: Context, query: String): String {
        try {
            val q = ArabicText.normalize(ArabicText.stripDiacritics(query)).trim()
            if (q.length < 2) return "أي تطبيق بدك أفتح؟"

            val terms = ArrayList<String>()
            terms.add(q)
            aliases[q]?.let { terms.addAll(it) }
            if (q.startsWith("ال")) {
                val s = q.removePrefix("ال")
                terms.add(s)
                aliases[s]?.let { terms.addAll(it) }
            }

            val pm = context.packageManager
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val apps: List<ResolveInfo> = pm.queryIntentActivities(launcher, 0)

            var best: ResolveInfo? = null
            var bestScore = 0
            var bestLabel = ""
            for (ri in apps) {
                val label = ri.loadLabel(pm).toString()
                val labelNorm = ArabicText.normalize(label)
                val pkg = ri.activityInfo.packageName.lowercase()
                var score = 0
                for (t in terms) {
                    val s = when {
                        labelNorm == t -> 100
                        labelNorm.startsWith(t) -> 80
                        labelNorm.contains(t) -> 60
                        pkg.contains(t) -> 40
                        else -> 0
                    }
                    if (s > score) score = s
                }
                if (score > bestScore) {
                    bestScore = score
                    best = ri
                    bestLabel = label
                }
            }

            val target = best ?: return "ما لقيت تطبيق باسم «$query»."
            val intent = pm.getLaunchIntentForPackage(target.activityInfo.packageName)
                ?: return "ما قدرت أفتح «$bestLabel»."
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return "فتحت $bestLabel."
        } catch (e: Exception) {
            return "ما قدرت أفتح التطبيق."
        }
    }

    fun setAlarm(context: Context, hour: Int, minute: Int, label: String): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, hour)
                .putExtra(AlarmClock.EXTRA_MINUTES, minute)
                .putExtra(AlarmClock.EXTRA_MESSAGE, label)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }
            "ضبطت منبه الساعة ${TimeFormat.clock(cal.timeInMillis)} بتطبيق الساعة."
        } catch (e: Exception) {
            "ما قدرت أضبط المنبه. تأكد من وجود تطبيق ساعة على جهازك."
        }
    }

    fun setFlashlight(context: Context, on: Boolean): String {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            var torchId: String? = null
            for (id in cm.cameraIdList) {
                val ch = cm.getCameraCharacteristics(id)
                val hasFlash = ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val back = ch.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                if (hasFlash && back) {
                    torchId = id
                    break
                }
            }
            val id = torchId ?: return "ما لقيت فلاش بجهازك."
            cm.setTorchMode(id, on)
            if (on) "شغّلت الفلاش." else "طفّيت الفلاش."
        } catch (e: Exception) {
            "ما قدرت أتحكم بالفلاش على جهازك."
        }
    }
}
