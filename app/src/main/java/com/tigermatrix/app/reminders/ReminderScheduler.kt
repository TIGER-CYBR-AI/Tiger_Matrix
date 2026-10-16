package com.tigermatrix.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.tigermatrix.app.MainActivity
import com.tigermatrix.app.data.AppDatabase

/**
 * جدولة التذكيرات عبر setAlarmClock: دقيقة بالوقت وما بتحتاج إذن "المنبهات الدقيقة".
 * (الثمن: أيقونة منبه صغيرة بشريط الحالة طول ما في تذكير معلّق.)
 */
object ReminderScheduler {

    private fun alarmManager(context: Context): AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private fun firePending(context: Context, taskId: Long, triggerAt: Long, flags: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_FIRE)
            .putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            .putExtra(ReminderReceiver.EXTRA_TRIGGER_AT, triggerAt)
        return PendingIntent.getBroadcast(context, taskId.toInt(), intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    fun schedule(context: Context, taskId: Long, triggerAt: Long) {
        val app = context.applicationContext
        try {
            Notifier.ensureChannel(app)
            val am = alarmManager(app) ?: return
            val pi = firePending(app, taskId, triggerAt, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
            val showIntent = PendingIntent.getActivity(
                app,
                0,
                Intent(app, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), pi)
        } catch (e: Exception) {
            // تجاهل
        }
    }

    fun cancel(context: Context, taskId: Long) {
        val app = context.applicationContext
        try {
            val am = alarmManager(app) ?: return
            val pi = firePending(app, taskId, 0L, PendingIntent.FLAG_NO_CREATE)
            if (pi != null) {
                am.cancel(pi)
                pi.cancel()
            }
        } catch (e: Exception) {
            // تجاهل
        }
    }

    suspend fun rescheduleAll(context: Context) {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        val tasks = AppDatabase.get(app).taskDao().getReminders()
        for (t in tasks) {
            val due = t.dueAt
            if (due != null && due > now) schedule(app, t.id, due)
        }
    }
}
