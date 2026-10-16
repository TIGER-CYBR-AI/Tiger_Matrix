package com.tigermatrix.app.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.tigermatrix.app.tools.TextTools

@Composable
fun TextTool() {
    var text by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val stats = TextTools.stats(text)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("النص") },
            minLines = 6,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "الحروف: ${stats.chars}  •  الكلمات: ${stats.words}  •  الأسطر: ${stats.lines}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { text = TextTools.removeDiacritics(text) }) { Text("حذف التشكيل") }
            OutlinedButton(onClick = { text = TextTools.cleanSpaces(text) }) { Text("تنظيف المسافات") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(text)) }) { Text("نسخ") }
            OutlinedButton(onClick = { text = "" }) { Text("مسح") }
        }
    }
}
