@file:OptIn(ExperimentalMaterial3Api::class)

package com.tigermatrix.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tigermatrix.app.assistant.TimeFormat
import com.tigermatrix.app.ui.common.ScreenHeader
import com.tigermatrix.app.viewmodel.NotesViewModel
import com.tigermatrix.app.viewmodel.TasksViewModel

@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    notesVm: NotesViewModel = viewModel(),
    tasksVm: TasksViewModel = viewModel()
) {
    val notes by notesVm.notes.collectAsStateWithLifecycle()
    val tasks by tasksVm.tasks.collectAsStateWithLifecycle()
    val pending = tasks.filter { !it.done }
    val reminders = tasks.filter { !it.done && it.dueAt != null }.sortedBy { it.dueAt }.take(3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader("Tiger Matrix") {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "الإعدادات")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("مهام معلقة", pending.size.toString(), Modifier.weight(1f))
            StatCard("الملاحظات", notes.size.toString(), Modifier.weight(1f))
        }

        Section("تذكيرات قادمة") {
            if (reminders.isEmpty()) {
                Text("ما في تذكيرات", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                reminders.forEach {
                    Text(
                        "• ${it.title} — ${TimeFormat.describe(it.dueAt ?: 0L)}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Section("أهم المهام") {
            if (pending.isEmpty()) {
                Text("ما في مهام معلقة", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                pending.take(3).forEach { Text("• ${it.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }

        Section("آخر الملاحظات") {
            if (notes.isEmpty()) {
                Text("ما في ملاحظات بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                notes.take(3).forEach { Text("• ${it.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        content()
    }
}
