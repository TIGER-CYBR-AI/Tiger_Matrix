package com.tigermatrix.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.tigermatrix.app.MainActivity
import com.tigermatrix.app.R

/** ويدجت الشاشة الرئيسية: إضافة سريعة لمهمة أو ملاحظة، أو فتح المساعد. */
class QuickAddWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_quick_add)
                views.setOnClickPendingIntent(R.id.btn_task, pending(context, MainActivity.ACTION_ADD_TASK, 1))
                views.setOnClickPendingIntent(R.id.btn_note, pending(context, MainActivity.ACTION_ADD_NOTE, 2))
                views.setOnClickPendingIntent(R.id.btn_assistant, pending(context, MainActivity.ACTION_ASSISTANT, 3))
                appWidgetManager.updateAppWidget(id, views)
            } catch (e: Exception) {
                // تجاهل
            }
        }
    }

    private fun pending(context: Context, action: String, code: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction("tm.widget.$action")
            .putExtra(MainActivity.EXTRA_ACTION, action)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
