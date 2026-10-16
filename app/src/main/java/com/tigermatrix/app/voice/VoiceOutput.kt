package com.tigermatrix.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * نطق الردود عبر TextToSpeech تبع أندرويد.
 * إذا الصوت العربي غير منزّل، بنبلّغ المستخدم مرة وحدة وما بننطق شي (بدون انهيار).
 */
class VoiceOutput(
    context: Context,
    private val onProblem: (String) -> Unit
) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status -> onInit(status) }
        } catch (e: Exception) {
            tts = null
            onProblem(VoiceMessages.TTS_FAILED)
        }
    }

    private fun onInit(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            onProblem(VoiceMessages.TTS_FAILED)
            return
        }
        try {
            val result = engine.setLanguage(Locale.forLanguageTag("ar"))
            ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ready) onProblem(VoiceMessages.NO_ARABIC_VOICE)
        } catch (e: Exception) {
            ready = false
            onProblem(VoiceMessages.TTS_FAILED)
        }
    }

    fun speak(text: String) {
        if (!ready) return
        val clean = prepare(text)
        if (clean.isBlank()) return
        try {
            tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "tm_reply")
        } catch (e: Exception) {
            // تجاهل
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            // تجاهل
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            // تجاهل
        }
        tts = null
        ready = false
    }

    /** يحوّل رموز الردود لكلام مفهوم قبل النطق. */
    private fun prepare(text: String): String = text
        .replace("•", "")
        .replace("✓", "")
        .replace("=", " يساوي ")
        .replace("*", " ضرب ")
        .replace("×", " ضرب ")
        .replace("+", " زائد ")
        .replace("÷", " قسمة ")
        .replace("/", " قسمة ")
        .replace("\n", ". ")
        .trim()
}
