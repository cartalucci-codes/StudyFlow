package com.studyflow.app.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.studyflow.app.data.local.entity.SyncStatus
import com.studyflow.app.data.local.entity.TaskEntity

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY dueDate ASC")
    fun observeTasksForUser(userId: String): LiveData<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE syncStatus = :status")
    suspend fun getTasksBySyncStatus(status: SyncStatus = SyncStatus.PENDING): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE taskId = :id LIMIT 1")
    suspend fun getById(id: String): TaskEntity?
}
