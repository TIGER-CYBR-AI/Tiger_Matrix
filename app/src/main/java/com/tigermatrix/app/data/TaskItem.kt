package com.tigermatrix.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** priority: 0 = منخفضة، 1 = متوسطة، 2 = عالية */
@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val done: Boolean = false,
    val priority: Int = 1,
    val dueAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
