package com.tigermatrix.app.voice

/** كل رسائل الصوت بمكان واحد لسهولة التعديل. */
object VoiceMessages {
    const val NOT_AVAILABLE = "خدمة التعرف على الصوت غير متوفرة على جهازك. تأكد من وجود تطبيق Google وخدمات الكلام."
    const val MIC_DENIED = "بدون إذن الميكروفون ما بقدر أسمعك. فعّله من إعدادات التطبيق بالجهاز."
    const val NO_SPEECH = "ما سمعتك منيح، جرب مرة تانية."
    const val NETWORK = "التعرف الصوتي ما اشتغل. غالباً بدو إنترنت، أو حزمة عربية منزّلة أوفلاين."
    const val LANGUAGE = "اللغة العربية غير متوفرة للتعرف الصوتي على جهازك."
    const val BUSY = "خدمة الصوت مشغولة، جرب بعد ثواني."
    const val AUDIO = "في مشكلة بالميكروفون."
    const val GENERIC = "صار خطأ بالتعرف الصوتي، جرب مرة تانية."
    const val TTS_FAILED = "محرك النطق ما اشتغل على جهازك."
    const val NO_ARABIC_VOICE = "صوت عربي للنطق غير منزّل. نزّله من إعدادات الجهاز (تحويل النص إلى كلام) ثم جرب."
}
