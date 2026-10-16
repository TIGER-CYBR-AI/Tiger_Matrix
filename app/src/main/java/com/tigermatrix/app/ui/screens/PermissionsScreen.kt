package com.tigermatrix.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tigermatrix.app.actions.AccessibilityStatus
import com.tigermatrix.app.actions.ActionLog
import com.tigermatrix.app.data.ActionMode
import androidx.compose.foundation.text.KeyboardOptions
import com.tigermatrix.app.ui.common.openAppDetails
import com.tigermatrix.app.ui.common.rememberResumeCounter
import com.tigermatrix.app.viewmodel.SettingsViewModel

@Composable
fun PermissionsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val tick = rememberResumeCounter()
    val cfg by vm.actionSettings.collectAsStateWithLifecycle()
    var logBump by remember { mutableIntStateOf(0) }
    val serviceEnabled = remember(tick) { AccessibilityStatus.isServiceEnabled(context) }
    val log = remember(tick, logBump) { ActionLog.read(context).take(20) }
    var countryCode by remember { mutableStateOf(cfg.countryCode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
            }
            Text(
                "مركز الصلاحيات",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Hint("المساعد ما بينفّذ شي لحاله أبداً: كل إجراء بيحصل فقط بعد أمر منك. هون بتختار كيف يتصرف بعد ما تعطيه الأمر.")

        // ---------------- الاتصال
        Section("الاتصال بالاسم")
        ModeSelector(
            selected = cfg.callMode,
            askLabel = "اسألني كل مرة (بجهّز المكالمة وبضغط تأكيد)",
            autoLabel = "نفّذ تلقائياً بعد 10 ثواني (مع زر إلغاء)",
            offLabel = "مطفي",
            onSelect = { vm.setCallMode(it) }
        )

        // ---------------- SMS
        Section("رسائل SMS")
        ModeSelector(
            selected = cfg.smsMode,
            askLabel = "اسألني كل مرة (بقرأ الرسالة وبضغط تأكيد)",
            autoLabel = "أرسل تلقائياً بعد 10 ثواني (مع زر إلغاء)",
            offLabel = "مطفي",
            onSelect = { vm.setSmsMode(it) }
        )

        // ---------------- واتساب
        Section("رسائل واتساب")
        ModeSelector(
            selected = cfg.whatsappMode,
            askLabel = "افتح المحادثة والرسالة جاهزة (أنا بضغط إرسال)",
            autoLabel = "إرسال تلقائي (بيحتاج خدمة إمكانية الوصول، الخطوات تحت)",
            offLabel = "مطفي",
            onSelect = { vm.setWhatsappMode(it) }
        )
        Text(
            "أي تطبيق واتساب؟",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        ModeRow(cfg.whatsappApp == 0, "تلقائي") { vm.setWhatsappApp(0) }
        ModeRow(cfg.whatsappApp == 1, "واتساب") { vm.setWhatsappApp(1) }
        ModeRow(cfg.whatsappApp == 2, "واتساب بزنس") { vm.setWhatsappApp(2) }
        OutlinedTextField(
            value = countryCode,
            onValueChange = {
                val d = it.filter { c -> c.isDigit() }.take(4)
                countryCode = d
                vm.setCountryCode(d)
            },
            label = { Text("رمز الدولة للأرقام المحلية (سوريا 963)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // ---------------- الأذونات
        Divider(modifier = Modifier.padding(vertical = 12.dp))
        Section("أذونات النظام")
        PermissionRow("جهات الاتصال (للبحث بالاسم)", Manifest.permission.READ_CONTACTS)
        PermissionRow("الاتصال المباشر", Manifest.permission.CALL_PHONE)
        PermissionRow("إرسال SMS مباشرة", Manifest.permission.SEND_SMS)
        PermissionRow("الميكروفون", Manifest.permission.RECORD_AUDIO)
        TextButton(
            onClick = { openAppDetails(context) },
            modifier = Modifier.padding(horizontal = 8.dp)
        ) { Text("فتح إعدادات التطبيق بالنظام") }

        // ---------------- الإرسال التلقائي لواتساب
        Divider(modifier = Modifier.padding(vertical = 12.dp))
        Section("الإرسال التلقائي لواتساب (اختياري)")
        Hint(
            "بدون هالخطوات المساعد بيجهّز الرسالة بس وأنت بتضغط إرسال. " +
                "الخدمة محصورة بواتساب فقط، وما بتتحرك إلا بعد أمر منك، وبتضغط زر الإرسال وبس."
        )

        Text(
            "الخطوة 1: تفعيل الخدمة من النظام",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Text(
            if (serviceEnabled) "✓ الخدمة مفعّلة" else "الخدمة غير مفعّلة",
            color = if (serviceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Hint(
            "1) افتح «معلومات التطبيق»، اضغط ⋮ (أعلى اليمين) ثم «السماح بالإعدادات المقيّدة». " +
                "(أندرويد 13+ وبس إذا التطبيق منزّل من خارج المتجر).\n" +
                "2) افتح «إمكانية الوصول» ← «التطبيقات المثبّتة» ← Tiger Matrix ← فعّلها."
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { openAppDetails(context) }) { Text("معلومات التطبيق") }
            Button(onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (e: Exception) {
                    openAppDetails(context)
                }
            }) { Text("إمكانية الوصول") }
        }

        Text(
            "الخطوة 2: ربط الخدمة بالتطبيقات",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
        )
        SwitchRow("السماح بالإرسال التلقائي داخل واتساب", cfg.allowWhatsApp) { vm.setAllowWhatsApp(it) }
        SwitchRow("السماح بالإرسال التلقائي داخل واتساب بزنس", cfg.allowBusiness) { vm.setAllowBusiness(it) }
        Hint("حماية: بين كل رسالتين تلقائيتين لازم يمر 10 ثواني على الأقل. الإرسال الكثيف بسرعة ممكن يعرّض رقمك لحظر واتساب.")

        // ---------------- السجل
        Divider(modifier = Modifier.padding(vertical = 12.dp))
        Section("سجل الإجراءات")
        if (log.isEmpty()) {
            Text(
                "ما في إجراءات بعد.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            log.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
            }
            TextButton(
                onClick = {
                    ActionLog.clear(context)
                    logBump++
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) { Text("مسح السجل") }
        }
        Text("", modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun ModeSelector(
    selected: ActionMode,
    askLabel: String,
    autoLabel: String,
    offLabel: String,
    onSelect: (ActionMode) -> Unit
) {
    Column {
        ModeRow(selected == ActionMode.ASK, askLabel) { onSelect(ActionMode.ASK) }
        ModeRow(selected == ActionMode.AUTO, autoLabel) { onSelect(ActionMode.AUTO) }
        ModeRow(selected == ActionMode.OFF, offLabel) { onSelect(ActionMode.OFF) }
    }
}

@Composable
private fun ModeRow(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun PermissionRow(label: String, permission: String) {
    val context = LocalContext.current
    val tick = rememberResumeCounter()
    var bump by remember { mutableIntStateOf(0) }
    val granted = remember(tick, bump) {
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { bump++ }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        if (granted) {
            Text("✓ ممنوح", color = MaterialTheme.colorScheme.primary)
        } else {
            OutlinedButton(onClick = { launcher.launch(permission) }) { Text("منح") }
        }
    }
}
