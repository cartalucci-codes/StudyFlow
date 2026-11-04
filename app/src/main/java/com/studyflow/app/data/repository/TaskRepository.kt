package com.studyflow.app.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.studyflow.app.data.local.dao.StudySessionDao
import com.studyflow.app.data.local.dao.TaskDao
import com.studyflow.app.data.local.entity.StudySessionEntity
import com.studyflow.app.data.local.entity.SyncStatus
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.local.entity.TaskStatus
import com.studyflow.app.data.remote.ApiService
import com.studyflow.app.util.SessionManager

class AuthExpiredException : Exception("The login token is no longer valid")

data class SyncReport(val pushed: Int, val pulled: Int, val failed: Int)

/**
 * Offline-first data access for tasks and study sessions.
 *
 * Every change is written to Room immediately and flagged PENDING, so the app never waits on the
 * network. [syncAll] (run by SyncWorker whenever a connection is available) then:
 *   1. pushes every PENDING task / delete / study session to the REST API, and
 *   2. pulls down anything new from the server (e.g. changes made on another device).
 */
class TaskRepository(
    private val taskDao: TaskDao,
    private val sessionDao: StudySessionDao,
    private val api: ApiService,
    private val session: SessionManager
) {

    fun observeTasks(userId: String): LiveData<List<TaskEntity>> = taskDao.observeTasksForUser(userId)

    suspend fun getTask(id: String): TaskEntity? = taskDao.getById(id)

    suspend fun getOpenTasks(userId: String): List<TaskEntity> = taskDao.getOpenTasks(userId)

    /** Create or edit a task. Saved on the phone straight away; synced later. */
    suspend fun saveTask(task: TaskEntity) {
        taskDao.upsert(task.copy(syncStatus = SyncStatus.PENDING))
        Log.d(TAG, "Saved task ${task.taskId} locally (pending sync)")
    }

    suspend fun setDone(task: TaskEntity, done: Boolean) {
        saveTask(task.copy(status = if (done) TaskStatus.Done else TaskStatus.Open))
    }

    /** Soft delete: hidden immediately, and the server is told on the next sync. */
    suspend fun deleteTask(task: TaskEntity) {
        taskDao.upsert(task.copy(deleted = true, syncStatus = SyncStatus.PENDING))
        Log.d(TAG, "Marked task ${task.taskId} deleted locally (pending sync)")
    }

    suspend fun saveStudySession(studySession: StudySessionEntity) {
        sessionDao.upsert(studySession.copy(syncStatus = SyncStatus.PENDING))
        Log.d(TAG, "Saved study session ${studySession.sessionId} locally (pending sync)")
    }

    /** @throws java.io.IOException when offline (the caller retries later) and [AuthExpiredException] on HTTP 401. */
    suspend fun syncAll(): SyncReport {
        val userId = session.userId ?: return SyncReport(0, 0, 0)
        val auth = session.authHeader()
        var pushed = 0
        var failed = 0

        // 1. Push pending tasks (and tombstoned deletes)
        for (task in taskDao.getPending(userId)) {
            if (task.deleted) {
                val response = api.deleteTask(auth, task.taskId)
                when {
                    response.code() == 401 -> throw AuthExpiredException()
                    response.isSuccessful || response.code() == 404 -> {
                        taskDao.deleteById(task.taskId)
                        pushed++
                    }
                    else -> failed++
                }
            } else {
                val response = api.createTask(auth, TaskMapper.toDto(task))
                when {
                    response.code() == 401 -> throw AuthExpiredException()
                    response.isSuccessful -> {
                        taskDao.update(task.copy(syncStatus = SyncStatus.SYNCED))
                        pushed++
                    }
                    else -> {
                        Log.w(TAG, "Server rejected task ${task.taskId}: HTTP ${response.code()}")
                        failed++
                    }
                }
            }
        }

        // 2. Push pending study sessions
        for (studySession in sessionDao.getPending(userId)) {
            val response = api.createStudySession(auth, TaskMapper.toDto(studySession))
            when {
                response.code() == 401 -> throw AuthExpiredException()
                response.isSuccessful -> {
                    sessionDao.update(studySession.copy(syncStatus = SyncStatus.SYNCED))
                    pushed++
                }
                else -> {
                    Log.w(TAG, "Server rejected session ${studySession.sessionId}: HTTP ${response.code()}")
                    failed++
                }
            }
        }

        // 3. Pull: tasks and sessions from the server. Local changes that haven't been pushed win.
        var pulled = 0
        val tasksResponse = api.getTasks(auth)
        if (tasksResponse.code() == 401) throw AuthExpiredException()
        if (tasksResponse.isSuccessful) {
            for (dto in tasksResponse.body().orEmpty()) {
                val remote = TaskMapper.toEntity(dto, userId) ?: continue
                val local = taskDao.getById(remote.taskId)
                if (local == null || local.syncStatus == SyncStatus.SYNCED) {
                    if (local != remote) {
                        taskDao.upsert(remote)
                        pulled++
                    }
                }
            }
        }
        val sessionsResponse = api.getStudySessions(auth)
        if (sessionsResponse.isSuccessful) {
            for (dto in sessionsResponse.body().orEmpty()) {
                val remote = TaskMapper.toEntity(dto, userId) ?: continue
                if (sessionDao.getById(remote.sessionId) == null) {
                    sessionDao.upsert(remote)
                    pulled++
                }
            }
        }

        Log.i(TAG, "Sync finished: pushed=$pushed pulled=$pulled failed=$failed")
        return SyncReport(pushed, pulled, failed)
    }

    companion object {
        private const val TAG = "TaskRepository"
    }
}
