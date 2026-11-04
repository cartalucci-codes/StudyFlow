package com.studyflow.app.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyflow.app.data.local.entity.TaskEntity

@Dao
interface TaskDao {

    /** Live list for the UI. Soft-deleted tasks are hidden. */
    @Query("SELECT * FROM tasks WHERE userId = :userId AND deleted = 0 ORDER BY dueDate ASC")
    fun observeTasksForUser(userId: String): LiveData<List<TaskEntity>>

    /** How many tasks are still waiting to be pushed to the server (drives the sync status text). */
    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND syncStatus = 'PENDING'")
    fun observePendingCount(userId: String): LiveData<Int>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND syncStatus = 'PENDING'")
    suspend fun getPending(userId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND status = 'Open' AND deleted = 0 ORDER BY dueDate ASC")
    suspend fun getOpenTasks(userId: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE taskId = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM tasks WHERE taskId = :id LIMIT 1")
    suspend fun getById(id: String): TaskEntity?
}
