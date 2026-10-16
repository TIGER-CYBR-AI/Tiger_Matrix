@file:OptIn(ExperimentalMaterial3Api::class)

package com.tigermatrix.app.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tigermatrix.app.data.Note
import com.tigermatrix.app.ui.common.EmptyState
import com.tigermatrix.app.ui.common.ScreenHeader
import com.tigermatrix.app.viewmodel.NotesViewModel

@Composable
fun NotesScreen(
    openCreate: Boolean = false,
    onCreateHandled: () -> Unit = {},
    vm: NotesViewModel = viewModel()
) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Note?>(null) }
    var creating by remember { mutableStateOf(false) }

    LaunchedEffect(openCreate) {
        if (openCreate) {
            creating = true
            onCreateHandled()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("الملاحظات")
            OutlinedTextField(
                value = query,
                onValueChange = { vm.setQuery(it) },
                placeholder = { Text("بحث...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            if (notes.isEmpty()) {
                EmptyState(if (query.isBlank()) "ما في ملاحظات بعد. اضغط + لإضافة أول ملاحظة." else "ما في نتائج.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notes, key = { it.id }) { n ->
                        NoteCard(n, onClick = { editing = n }, onPin = { vm.togglePin(n) })
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة")
        }
    }

    if (creating) {
        NoteEditorDialog(
            note = null,
            onSave = { t, c ->
                vm.add(t, c)
                creating = false
            },
            onDelete = null,
            onDismiss = { creating = false }
        )
    }

    editing?.let { n ->
        NoteEditorDialog(
            note = n,
            onSave = { t, c ->
                vm.update(n.copy(title = t.trim().ifBlank { c.lines().first().take(40) }, content = c.trim(), updatedAt = System.currentTimeMillis()))
                editing = null
            },
            onDelete = {
                vm.delete(n)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit, onPin: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(note.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (note.content.isNotBlank()) {
                    Text(
                        note.content,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onPin) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "تثبيت",
                    tint = if (note.pinned) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }
        }
    }
}

@Composable
private fun NoteEditorDialog(
    note: Note?,
    onSave: (String, String) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note == null) "ملاحظة جديدة" else "تعديل الملاحظة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("العنوان") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("المحتوى") },
                    minLines = 4,
                    maxLines = 8
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, content) },
                enabled = title.isNotBlank() || content.isNotBlank()
            ) { Text("حفظ") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("حذف", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("إلغاء") }
            }
        }
    )
}
