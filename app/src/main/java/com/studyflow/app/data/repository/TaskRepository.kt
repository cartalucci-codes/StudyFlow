package com.studyflow.app.data.repository

import androidx.lifecycle.LiveData
import com.studyflow.app.data.local.dao.TaskDao
import com.studyflow.app.data.local.entity.Priority
import com.studyflow.app.data.local.entity.SyncStatus
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.local.entity.TaskStatus
import com.studyflow.app.data.remote.ApiService
import com.studyflow.app.data.remote.TaskDto
import com.studyflow.app.util.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Offline-first task storage: every write lands in Room straight away (status = PENDING)
 * so the UI never blocks on the network. syncPendingTasks() is called by the WorkManager
 * job (see SyncWorker) once connectivity is available, pushing pending rows to the API.
 */
class TaskRepository(
    private val taskDao: TaskDao,
    private val api: ApiService,
    private val session: SessionManager
) {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun observeTasks(userId: String): LiveData<List<TaskEntity>> = taskDao.observeTasksForUser(userId)

    suspend fun addTask(
        userId: String,
        title: String,
        description: String?,
        subject: String?,
        dueDate: Long,
        priority: Priority,
        reminderTime: Long?
    ) {
        val task = TaskEntity(
            userId = userId,
            title = title,
            description = description,
            subject = subject,
            dueDate = dueDate,
            priority = priority,
            reminderTime = reminderTime,
            syncStatus = SyncStatus.PENDING
        )
        taskDao.upsert(task) // saved offline instantly
    }

    suspend fun markDone(task: TaskEntity) {
        taskDao.update(task.copy(status = TaskStatus.Done, syncStatus = SyncStatus.PENDING))
    }

    /** Pushes every locally PENDING task to the API. Call from a background/WorkManager job. */
    suspend fun syncPendingTasks(): Result<Int> = try {
        val pending = taskDao.getTasksBySyncStatus(SyncStatus.PENDING)
        var synced = 0
        for (task in pending) {
            val dto = TaskDto(
                taskId = null,
                title = task.title,
                description = task.description,
                subject = task.subject,
                dueDate = isoFormat.format(Date(task.dueDate)),
                priority = task.priority.name,
                status = task.status.name,
                reminderTime = task.reminderTime?.let { isoFormat.format(Date(it)) }
            )
            val response = api.createTask(session.authHeader(), dto)
            if (response.isSuccessful) {
                taskDao.update(task.copy(syncStatus = SyncStatus.SYNCED))
                synced++
            }
        }
        Result.success(synced)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
