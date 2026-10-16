package com.tigermatrix.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tigermatrix.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** يستقبل: وقت التذكير، زر "تم"، زر "غفوة". كل شي ملفوف بـ try/catch حتى ما ينهار. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId < 0) return
        val app = context.applicationContext
        val action = intent.action
        val expected = intent.getLongExtra(EXTRA_TRIGGER_AT, -1L)
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(app).taskDao()
                val task = dao.getById(taskId)
                when (action) {
                    ACTION_FIRE -> {
                        // نتأكد إنو المهمة لسا موجودة وما انتهت وموعدها نفسه (حماية من منبهات قديمة)
                        if (task != null && !task.done && task.dueAt != null &&
                            (expected < 0 || task.dueAt == expected)
                        ) {
                            Notifier.showReminder(app, task)
                        }
                    }
                    ACTION_DONE -> {
                        if (task != null) dao.update(task.copy(done = true))
                        ReminderScheduler.cancel(app, taskId)
                        Notifier.cancel(app, taskId)
                    }
                    ACTION_SNOOZE -> {
                        if (task != null) {
                            val t = System.currentTimeMillis() + 10 * 60_000L
                            dao.update(task.copy(dueAt = t, done = false))
                            ReminderScheduler.schedule(app, task.id, t)
                        }
                        Notifier.cancel(app, taskId)
                    }
                }
            } catch (e: Exception) {
                // تجاهل
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.tigermatrix.app.REMINDER_FIRE"
        const val ACTION_DONE = "com.tigermatrix.app.REMINDER_DONE"
        const val ACTION_SNOOZE = "com.tigermatrix.app.REMINDER_SNOOZE"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TRIGGER_AT = "trigger_at"
    }
}
