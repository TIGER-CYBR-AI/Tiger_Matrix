package com.tigermatrix.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tigermatrix.app.MainActivity
import com.tigermatrix.app.R
import com.tigermatrix.app.data.TaskItem

object Notifier {
    const val CHANNEL_ID = "reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(CHANNEL_ID, "التذكيرات", NotificationManager.IMPORTANCE_HIGH)
                ch.description = "تذكيرات المهام"
                ch.enableVibration(true)
                nm.createNotificationChannel(ch)
            }
        }
    }

    fun canNotify(context: Context): Boolean =
        try {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (e: Exception) {
            false
        }

    fun showReminder(context: Context, task: TaskItem) {
        post(context, task.id.toInt(), "تذكير", task.title, task.id)
    }

    fun showTest(context: Context) {
        post(context, 987654, "إشعار تجريبي", "إذا وصلك هالإشعار، التذكيرات رح تشتغل.", null)
    }

    fun cancel(context: Context, taskId: Long) {
        try {
            NotificationManagerCompat.from(context).cancel(taskId.toInt())
        } catch (e: Exception) {
            // تجاهل
        }
    }

    private fun post(context: Context, notifId: Int, title: String, text: String, taskId: Long?) {
        try {
            ensureChannel(context)
            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(openApp)

            if (taskId != null) {
                builder.addAction(0, "تم", actionIntent(context, ReminderReceiver.ACTION_DONE, taskId))
                builder.addAction(0, "غفوة 10 دقائق", actionIntent(context, ReminderReceiver.ACTION_SNOOZE, taskId))
            }
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        } catch (e: Exception) {
            // تجاهل (غالباً إذن الإشعارات غير ممنوح)
        }
    }

    private fun actionIntent(context: Context, action: String, taskId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
