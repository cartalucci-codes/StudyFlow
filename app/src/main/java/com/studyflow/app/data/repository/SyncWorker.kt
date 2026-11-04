package com.studyflow.app.data.repository

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.studyflow.app.data.local.AppDatabase
import com.studyflow.app.data.remote.RetrofitClient
import com.studyflow.app.util.SessionManager
import java.util.concurrent.TimeUnit

/**
 * Background job that pushes everything saved while offline to the API (and pulls new data down).
 * WorkManager only runs it when the phone has a network connection, so it fires automatically
 * the moment the student reconnects - even if the app has been closed in the meantime.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val session = SessionManager(applicationContext)
        if (!session.isLoggedIn) return Result.success()

        val db = AppDatabase.getInstance(applicationContext)
        val repository = TaskRepository(db.taskDao(), db.studySessionDao(), RetrofitClient.api, session)

        return try {
            val report = repository.syncAll()
            session.lastSyncMillis = System.currentTimeMillis()
            if (report.failed > 0 && runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
        } catch (e: AuthExpiredException) {
            Log.w(TAG, "Sync stopped - login expired, the student must log in again")
            Result.failure()
        } catch (e: Exception) {
            Log.w(TAG, "Sync failed (attempt $runAttemptCount): ${e.message}")
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val MAX_ATTEMPTS = 6
    }
}

/** Queues a sync that runs as soon as the phone is online. Call after every local change. */
object SyncScheduler {
    private const val UNIQUE_NAME = "studyflow_sync"

    fun enqueue(context: Context) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}
