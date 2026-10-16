package com.tigermatrix.app.data

/**
 * مستوى صلاحية كل إجراء حساس (اتصال، SMS، واتساب):
 * ASK  = اسألني/جهّز الإجراء وانتظر تأكيدي (الافتراضي)
 * AUTO = نفّذ بعد مهلة إلغاء 10 ثواني
 * OFF  = مطفي
 */
enum class ActionMode { ASK, AUTO, OFF }

data class ActionSettings(
    val callMode: ActionMode = ActionMode.ASK,
    val smsMode: ActionMode = ActionMode.ASK,
    val whatsappMode: ActionMode = ActionMode.ASK,
    /** 0 = تلقائي، 1 = واتساب، 2 = واتساب بزنس */
    val whatsappApp: Int = 0,
    /** الخطوة 2: السماح بالإرسال التلقائي داخل كل تطبيق */
    val allowWhatsApp: Boolean = false,
    val allowBusiness: Boolean = false,
    /** رمز الدولة لتحويل الأرقام المحلية (سوريا 963) */
    val countryCode: String = "963"
)
