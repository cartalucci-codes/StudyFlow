package com.studyflow.app.data.repository

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studyflow.app.data.local.AppDatabase
import com.studyflow.app.data.remote.RetrofitClient
import com.studyflow.app.util.SessionManager

/**
 * Background job that pushes any locally PENDING tasks up to the API once
 * connectivity is available. Scheduled from HomeActivity with a network constraint.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val session = SessionManager(applicationContext)
        if (!session.isLoggedIn) return Result.success()

        val repo = TaskRepository(db.taskDao(), RetrofitClient.api, session)
        val result = repo.syncPendingTasks()
        return if (result.isSuccess) Result.success() else Result.retry()
    }
}
