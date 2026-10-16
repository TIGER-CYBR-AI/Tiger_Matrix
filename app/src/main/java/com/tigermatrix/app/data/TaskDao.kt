package com.tigermatrix.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY done ASC, priority DESC, createdAt DESC")
    fun observeAll(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskItem>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskItem?

    @Query("SELECT * FROM tasks WHERE done = 0 ORDER BY priority DESC, createdAt DESC")
    suspend fun getPending(): List<TaskItem>

    /** المهام المعلقة اللي إلها موعد تذكير */
    @Query("SELECT * FROM tasks WHERE done = 0 AND dueAt IS NOT NULL ORDER BY dueAt ASC")
    suspend fun getReminders(): List<TaskItem>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :q || '%' ORDER BY done ASC, createdAt DESC LIMIT 5")
    suspend fun search(q: String): List<TaskItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskItem>)

    @Update
    suspend fun update(task: TaskItem)

    @Delete
    suspend fun delete(task: TaskItem)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tasks WHERE done = 1")
    suspend fun deleteCompleted()

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
