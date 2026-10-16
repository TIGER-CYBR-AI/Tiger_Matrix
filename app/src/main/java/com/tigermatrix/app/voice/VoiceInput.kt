package com.tigermatrix.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * تعرف صوتي عبر SpeechRecognizer تبع أندرويد.
 * لازم تُستدعى كل الدوال من الخيط الرئيسي (Compose يعمل هيك تلقائياً).
 * كل استدعاء للنظام ملفوف بـ try/catch حتى ما ينهار التطبيق إذا خدمة الصوت غير موجودة أو تعطلت.
 */
class VoiceInput(
    private val context: Context,
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onListening()
        fun onPartial(text: String)
        fun onResult(text: String)
        fun onError(message: String)
    }

    private var recognizer: SpeechRecognizer? = null
    private var active = false

    fun isAvailable(): Boolean = try {
        SpeechRecognizer.isRecognitionAvailable(context)
    } catch (e: Exception) {
        false
    }

    fun start() {
        cancel()
        if (!isAvailable()) {
            callbacks.onError(VoiceMessages.NOT_AVAILABLE)
            return
        }
        try {
            val r = SpeechRecognizer.createSpeechRecognizer(context)
            r.setRecognitionListener(listener)
            recognizer = r
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE_TAG)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            active = true
            callbacks.onListening()
            r.startListening(intent)
        } catch (e: Exception) {
            release()
            callbacks.onError(VoiceMessages.GENERIC)
        }
    }

    /** إلغاء صامت (المستخدم ضغط الزر مرة تانية أو غادر الشاشة). */
    fun cancel() {
        release()
    }

    fun destroy() {
        release()
    }

    private fun release() {
        active = false
        val r = recognizer
        recognizer = null
        try {
            r?.cancel()
        } catch (e: Exception) {
            // تجاهل
        }
        try {
            r?.destroy()
        } catch (e: Exception) {
            // تجاهل
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            if (!active) return
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) callbacks.onPartial(text)
        }

        override fun onResults(results: Bundle?) {
            if (!active) return
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
            release()
            if (text.isNullOrBlank()) callbacks.onError(VoiceMessages.NO_SPEECH)
            else callbacks.onResult(text)
        }

        override fun onError(error: Int) {
            // ERROR_CLIENT غالباً ينتج عن إلغاء منا، وما بدنا نعرضه كخطأ
            if (!active) return
            release()
            callbacks.onError(messageFor(error))
        }
    }

    private fun messageFor(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceMessages.NO_SPEECH
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER -> VoiceMessages.NETWORK
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceMessages.MIC_DENIED
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> VoiceMessages.BUSY
        SpeechRecognizer.ERROR_AUDIO -> VoiceMessages.AUDIO
        12, 13 -> VoiceMessages.LANGUAGE // اللغة غير مدعومة/غير متوفرة (أندرويد 13+)
        else -> VoiceMessages.GENERIC
    }

    companion object {
        /** لغة التعرف. إذا ما اشتغلت جرب "ar" أو "ar-EG". */
        const val LANGUAGE_TAG = "ar-SA"
    }
}
