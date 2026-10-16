@file:OptIn(ExperimentalMaterial3Api::class)

package com.tigermatrix.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tigermatrix.app.ui.common.ScreenHeader
import com.tigermatrix.app.viewmodel.AssistantViewModel
import com.tigermatrix.app.viewmodel.ChatMessage
import com.tigermatrix.app.viewmodel.SettingsViewModel
import com.tigermatrix.app.voice.VoiceInput
import com.tigermatrix.app.voice.VoiceMessages
import com.tigermatrix.app.voice.VoiceOutput
import com.tigermatrix.app.voice.VoicePermissions

private val suggestions = listOf("مهامي", "تذكيراتي", "ملاحظاتي", "تراجع", "مساعدة")

@Composable
fun AssistantScreen(
    settingsVm: SettingsViewModel,
    vm: AssistantViewModel = viewModel()
) {
    val context = LocalContext.current
    val messages by vm.messages.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val permReq by vm.permissionRequest.collectAsStateWithLifecycle()
    val speakReplies by settingsVm.speakReplies.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    val voiceOutput = remember { VoiceOutput(context) { msg -> notice = msg } }
    val voiceInput = remember {
        VoiceInput(context, object : VoiceInput.Callbacks {
            override fun onListening() {
                listening = true
                notice = null
            }

            override fun onPartial(text: String) {
                input = text
            }

            override fun onResult(text: String) {
                listening = false
                input = ""
                vm.send(text)
            }

            override fun onError(message: String) {
                listening = false
                input = ""
                notice = message
            }
        })
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceInput.destroy()
            voiceOutput.shutdown()
        }
    }

    // إذن الميكروفون
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceOutput.stop()
            voiceInput.start()
        } else {
            notice = VoiceMessages.MIC_DENIED
        }
    }

    // أذونات يطلبها المساعد أثناء تنفيذ الأوامر (جهات اتصال، اتصال، SMS)
    val commandPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> vm.onPermissionResult(granted) }

    LaunchedEffect(permReq?.id) {
        val req = permReq
        if (req != null) commandPermissionLauncher.launch(req.permission)
    }

    fun toggleMic() {
        if (listening) {
            voiceInput.cancel()
            listening = false
            input = ""
        } else if (VoicePermissions.hasMic(context)) {
            voiceOutput.stop()
            notice = null
            voiceInput.start()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // نطق آخر رد من المساعد (بدون نطق رسالة الترحيب الأولى)
    LaunchedEffect(messages.size) {
        val last = messages.lastOrNull()
        if (speakReplies && messages.size > 1 && last != null && !last.fromUser) {
            voiceOutput.speak(last.text)
        }
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    LaunchedEffect(speakReplies) {
        if (!speakReplies) voiceOutput.stop()
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("المساعد")
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { m -> Bubble(m) }
        }

        // بطاقة الإجراء المعلّق: تأكيد أو عدّاد تنازلي مع إلغاء
        pending?.let { p ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(p.title, fontWeight = FontWeight.Bold)
                    if (p.detail.isNotBlank()) {
                        Text(p.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (p.secondsLeft != null) {
                        Text("بنفّذ خلال ${p.secondsLeft} ثانية", color = MaterialTheme.colorScheme.primary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.confirmPending() }) {
                            Text(if (p.secondsLeft != null) "نفّذ هلق" else "تأكيد")
                        }
                        OutlinedButton(onClick = { vm.cancelPending() }) { Text("إلغاء") }
                    }
                }
            }
        }

        if (listening) {
            Text(
                "عم سمعك... احكي الأمر",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        notice?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestions) { s ->
                AssistChip(onClick = { vm.send(s) }, label = { Text(s) })
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("اكتب أمر أو اضغط الميكروفون...") },
                maxLines = 3
            )
            IconButton(onClick = { toggleMic() }) {
                Text(if (listening) "⏹" else "🎤", fontSize = 22.sp)
            }
            IconButton(onClick = {
                if (listening) {
                    voiceInput.cancel()
                    listening = false
                }
                vm.send(input)
                input = ""
            }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال")
            }
        }
    }
}

@Composable
private fun Bubble(m: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (m.fromUser) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            color = if (m.fromUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(m.text, modifier = Modifier.padding(12.dp))
        }
    }
}
