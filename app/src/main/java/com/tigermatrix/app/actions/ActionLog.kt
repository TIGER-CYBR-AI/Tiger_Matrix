package com.tigermatrix.app.actions

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** سجل بسيط بالإجراءات اللي نفّذها المساعد (بدون نص الرسائل، للخصوصية). */
object ActionLog {
    private const val FILE_NAME = "actions_log.txt"
    private const val MAX_LINES = 100

    @Synchronized
    fun append(context: Context, text: String) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            val old = if (file.exists()) file.readLines() else emptyList()
            file.writeText((old + "$stamp  $text").takeLast(MAX_LINES).joinToString("\n"))
        } catch (e: Exception) {
            // تجاهل
        }
    }

    @Synchronized
    fun read(context: Context): List<String> {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) emptyList() else file.readLines().filter { it.isNotBlank() }.reversed()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun clear(context: Context) {
        try {
            File(context.filesDir, FILE_NAME).delete()
        } catch (e: Exception) {
            // تجاهل
        }
    }
}
