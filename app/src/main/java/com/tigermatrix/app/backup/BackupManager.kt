package com.tigermatrix.app.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.tigermatrix.app.data.AppDatabase
import com.tigermatrix.app.data.Note
import com.tigermatrix.app.data.TaskItem
import com.tigermatrix.app.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** نسخ احتياطي واستعادة بملف JSON (بدون أي إذن، عبر منتقي الملفات تبع النظام). */
object BackupManager {
    private const val VERSION = 1

    suspend fun exportToUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val json = buildJson(context)
            val stream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext "ما قدرت أكتب الملف."
            stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            "تم حفظ النسخة الاحتياطية."
        } catch (e: Exception) {
            "فشل حفظ النسخة الاحتياطية."
        }
    }

    suspend fun importFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return@withContext "ما قدرت أقرأ الملف."
            val text = stream.use { it.readBytes().toString(Charsets.UTF_8) }
            val root = JSONObject(text)
            if (root.optString("app") != "TigerMatrix") {
                return@withContext "هاد الملف مو نسخة احتياطية من Tiger Matrix."
            }
            val notes = parseNotes(root.optJSONArray("notes"))
            val tasks = parseTasks(root.optJSONArray("tasks"))

            val db = AppDatabase.get(context)
            db.taskDao().getAll().forEach { ReminderScheduler.cancel(context, it.id) }
            db.withTransaction {
                db.noteDao().deleteAll()
                db.taskDao().deleteAll()
                db.noteDao().insertAll(notes)
                db.taskDao().insertAll(tasks)
            }
            ReminderScheduler.rescheduleAll(context)
            "تمت الاستعادة: ${notes.size} ملاحظة و${tasks.size} مهمة."
        } catch (e: Exception) {
            "الملف تالف أو غير صالح."
        }
    }

    private suspend fun buildJson(context: Context): String {
        val db = AppDatabase.get(context)
        val notes = db.noteDao().getAll()
        val tasks = db.taskDao().getAll()
        val root = JSONObject()
        root.put("app", "TigerMatrix")
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val notesArr = JSONArray()
        for (n in notes) {
            val o = JSONObject()
            o.put("title", n.title)
            o.put("content", n.content)
            o.put("pinned", n.pinned)
            o.put("createdAt", n.createdAt)
            o.put("updatedAt", n.updatedAt)
            notesArr.put(o)
        }
        root.put("notes", notesArr)

        val tasksArr = JSONArray()
        for (t in tasks) {
            val o = JSONObject()
            o.put("title", t.title)
            o.put("done", t.done)
            o.put("priority", t.priority)
            o.put("createdAt", t.createdAt)
            val due = t.dueAt
            if (due != null) o.put("dueAt", due)
            tasksArr.put(o)
        }
        root.put("tasks", tasksArr)
        return root.toString(2)
    }

    private fun parseNotes(arr: JSONArray?): List<Note> {
        val out = ArrayList<Note>()
        if (arr == null) return out
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val title = o.optString("title", "")
            val content = o.optString("content", "")
            if (title.isBlank() && content.isBlank()) continue
            val now = System.currentTimeMillis()
            out.add(
                Note(
                    title = title,
                    content = content,
                    pinned = o.optBoolean("pinned", false),
                    createdAt = o.optLong("createdAt", now),
                    updatedAt = o.optLong("updatedAt", now)
                )
            )
        }
        return out
    }

    private fun parseTasks(arr: JSONArray?): List<TaskItem> {
        val out = ArrayList<TaskItem>()
        if (arr == null) return out
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val title = o.optString("title", "")
            if (title.isBlank()) continue
            val due: Long? = if (o.has("dueAt") && !o.isNull("dueAt")) o.optLong("dueAt") else null
            out.add(
                TaskItem(
                    title = title,
                    done = o.optBoolean("done", false),
                    priority = o.optInt("priority", 1).coerceIn(0, 2),
                    dueAt = due,
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
        return out
    }
}
