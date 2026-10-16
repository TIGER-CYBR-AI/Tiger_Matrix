package com.tigermatrix.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tigermatrix.app.data.AppDatabase
import com.tigermatrix.app.data.TaskItem
import com.tigermatrix.app.reminders.Notifier
import com.tigermatrix.app.reminders.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasksViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Application = app
    private val dao = AppDatabase.get(app).taskDao()

    val tasks: StateFlow<List<TaskItem>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(title: String, priority: Int, dueAt: Long? = null) {
        val t = title.trim()
        if (t.isEmpty()) return
        viewModelScope.launch {
            val id = dao.insert(TaskItem(title = t, priority = priority, dueAt = dueAt))
            if (dueAt != null) ReminderScheduler.schedule(ctx, id, dueAt)
        }
    }

    fun toggle(task: TaskItem) {
        viewModelScope.launch {
            val updated = task.copy(done = !task.done)
            dao.update(updated)
            if (updated.done) {
                ReminderScheduler.cancel(ctx, task.id)
                Notifier.cancel(ctx, task.id)
            } else {
                val due = updated.dueAt
                if (due != null && due > System.currentTimeMillis()) {
                    ReminderScheduler.schedule(ctx, task.id, due)
                }
            }
        }
    }

    fun delete(task: TaskItem) {
        viewModelScope.launch {
            dao.delete(task)
            ReminderScheduler.cancel(ctx, task.id)
            Notifier.cancel(ctx, task.id)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch { dao.deleteCompleted() }
    }
}
