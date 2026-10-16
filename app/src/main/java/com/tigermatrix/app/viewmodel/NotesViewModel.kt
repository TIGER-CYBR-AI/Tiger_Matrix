package com.tigermatrix.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tigermatrix.app.data.AppDatabase
import com.tigermatrix.app.data.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).noteDao()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val notes: StateFlow<List<Note>> = combine(dao.observeAll(), _query) { list, q ->
        if (q.isBlank()) list
        else list.filter { it.title.contains(q, true) || it.content.contains(q, true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) {
        _query.value = q
    }

    fun add(title: String, content: String) {
        val t = title.trim()
        val c = content.trim()
        viewModelScope.launch {
            dao.insert(Note(title = t.ifBlank { c.lines().first().take(40) }, content = c))
        }
    }

    fun update(note: Note) {
        viewModelScope.launch { dao.update(note) }
    }

    fun delete(note: Note) {
        viewModelScope.launch { dao.delete(note) }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { dao.update(note.copy(pinned = !note.pinned)) }
    }
}
