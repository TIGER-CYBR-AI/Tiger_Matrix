package com.tigermatrix.app.ui.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tigermatrix.app.reminders.Notifier

fun openNotificationSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        context.startActivity(intent)
    } catch (e: Exception) {
        openAppDetails(context)
    }
}

fun openAppDetails(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        context.startActivity(intent)
    } catch (e: Exception) {
        // تجاهل
    }
}

fun openBatterySettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    } catch (e: Exception) {
        openAppDetails(context)
    }
}

/** تفعيل الإشعارات + إعدادات البطارية + إشعار تجريبي. مستخدمة بالترحيب وبالإعدادات. */
@Composable
fun ReminderSetup(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tick = rememberResumeCounter()
    var bump by remember { mutableIntStateOf(0) }
    val enabled = remember(tick, bump) { Notifier.canNotify(context) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        bump++
        if (!granted) openNotificationSettings(context)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (enabled) "الإشعارات: مفعّلة ✓" else "الإشعارات: غير مفعّلة",
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        if (!enabled) {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 33) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openNotificationSettings(context)
                }
            }) { Text("تفعيل الإشعارات") }
        }
        OutlinedButton(onClick = { openBatterySettings(context) }) { Text("استثناء من توفير البطارية") }
        Text(
            "على موبايلات Xiaomi/Poco: فعّل «التشغيل التلقائي» للتطبيق، واختر «بدون قيود» بإعدادات البطارية، وإلا ممكن النظام يوقف التذكيرات.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = { Notifier.showTest(context) }) { Text("إرسال إشعار تجريبي") }
    }
}
