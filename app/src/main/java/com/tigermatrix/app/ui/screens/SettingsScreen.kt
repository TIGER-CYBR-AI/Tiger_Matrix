package com.tigermatrix.app.ui.screens

import android.net.Uri
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tigermatrix.app.backup.BackupManager
import com.tigermatrix.app.ui.common.ReminderSetup
import com.tigermatrix.app.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val themeOptions = listOf("حسب النظام", "فاتح", "داكن")

@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit, onOpenPermissions: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mode by vm.themeMode.collectAsStateWithLifecycle()
    val speak by vm.speakReplies.collectAsStateWithLifecycle()

    var message by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch { message = BackupManager.exportToUri(context, uri) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingImport = uri
    }

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
                "الإعدادات",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        SectionTitle("المظهر")
        themeOptions.forEachIndexed { i, label ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.setThemeMode(i) }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = mode == i, onClick = null)
                Text(label, modifier = Modifier.padding(start = 12.dp))
            }
        }

        SectionTitle("المساعد")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { vm.setSpeakReplies(!speak) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("نطق ردود المساعد بصوت عالي", modifier = Modifier.weight(1f))
            Switch(checked = speak, onCheckedChange = null)
        }
        OutlinedButton(
            onClick = onOpenPermissions,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) { Text("مركز الصلاحيات (اتصال، رسائل، واتساب)") }

        SectionTitle("التذكيرات")
        ReminderSetup(modifier = Modifier.padding(horizontal = 16.dp))

        SectionTitle("النسخ الاحتياطي")
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    exportLauncher.launch("tiger-matrix-backup-$stamp.json")
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("حفظ نسخة احتياطية") }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("استعادة من نسخة احتياطية") }
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
        }

        Text(
            "Tiger Matrix • الإصدار 1.0.0\nTiger Technology",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }

    val importUri = pendingImport
    if (importUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("استعادة النسخة الاحتياطية") },
            text = { Text("الاستعادة بتستبدل كل ملاحظاتك ومهامك الحالية بمحتوى الملف. متأكد؟") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch { message = BackupManager.importFromUri(context, importUri) }
                }) { Text("استعادة") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}
