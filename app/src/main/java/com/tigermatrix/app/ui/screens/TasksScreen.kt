@file:OptIn(ExperimentalMaterial3Api::class)

package com.tigermatrix.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tigermatrix.app.assistant.TimeFormat
import com.tigermatrix.app.data.TaskItem
import com.tigermatrix.app.ui.common.EmptyState
import com.tigermatrix.app.ui.common.ScreenHeader
import com.tigermatrix.app.viewmodel.TasksViewModel
import java.util.Calendar

private val priorityLabels = listOf("منخفضة", "متوسطة", "عالية")
private val priorityColors = listOf(Color(0xFF2E7D32), Color(0xFFF9A825), Color(0xFFC62828))
private val reminderPresets = listOf("بدون تذكير", "بعد ساعة", "بعد 3 ساعات", "بكرا 9 صباحاً", "بكرا 6 مساءً")

private fun tomorrowAt(hour: Int, now: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun presetTime(index: Int): Long? {
    val now = System.currentTimeMillis()
    return when (index) {
        1 -> now + 60 * 60_000L
        2 -> now + 3 * 60 * 60_000L
        3 -> tomorrowAt(9, now)
        4 -> tomorrowAt(18, now)
        else -> null
    }
}

@Composable
fun TasksScreen(
    openCreate: Boolean = false,
    onCreateHandled: () -> Unit = {},
    vm: TasksViewModel = viewModel()
) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }

    LaunchedEffect(openCreate) {
        if (openCreate) {
            adding = true
            onCreateHandled()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("المهام") {
                if (tasks.any { it.done }) {
                    TextButton(onClick = { vm.clearCompleted() }) { Text("مسح المنجز") }
                }
            }
            if (tasks.isEmpty()) {
                EmptyState("ما في مهام بعد. اضغط + لإضافة أول مهمة.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks, key = { it.id }) { t ->
                        TaskRow(t, onToggle = { vm.toggle(t) }, onDelete = { vm.delete(t) })
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { adding = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة")
        }
    }

    if (adding) {
        AddTaskDialog(
            onAdd = { title, priority, dueAt ->
                vm.add(title, priority, dueAt)
                adding = false
            },
            onDismiss = { adding = false }
        )
    }
}

@Composable
private fun TaskRow(task: TaskItem, onToggle: () -> Unit, onDelete: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.done,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                }
            )
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                val p = task.priority.coerceIn(0, 2)
                Text(
                    "أولوية ${priorityLabels[p]}",
                    style = MaterialTheme.typography.labelSmall,
                    color = priorityColors[p]
                )
                val due = task.dueAt
                if (due != null) {
                    val overdue = !task.done && due < System.currentTimeMillis()
                    Text(
                        (if (overdue) "⚠ فات: " else "⏰ ") + TimeFormat.describe(due),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف")
            }
        }
    }
}

@Composable
private fun AddTaskDialog(onAdd: (String, Int, Long?) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(1) }
    var reminder by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مهمة جديدة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("نص المهمة") },
                    singleLine = true
                )
                Text("الأولوية", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    priorityLabels.forEachIndexed { i, label ->
                        FilterChip(
                            selected = priority == i,
                            onClick = { priority = i },
                            label = { Text(label) }
                        )
                    }
                }
                Text("التذكير", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reminderPresets.forEachIndexed { i, label ->
                        FilterChip(
                            selected = reminder == i,
                            onClick = { reminder = i },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(title, priority, presetTime(reminder)) },
                enabled = title.isNotBlank()
            ) { Text("إضافة") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
