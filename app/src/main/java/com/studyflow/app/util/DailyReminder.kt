package com.studyflow.app.util

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.studyflow.app.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * The daily study reminder from the Part 1 spec: a notification at a time the student chooses in
 * Settings. It is scheduled locally with WorkManager (so it works offline) and re-schedules itself
 * for the next day each time it fires.
 */
object DailyReminderScheduler {
    private const val UNIQUE_NAME = "studyflow_daily_reminder"
    private const val TAG = "DailyReminder"

    fun schedule(context: Context, session: SessionManager) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (!session.isLoggedIn || !session.notifyDaily) {
            workManager.cancelUniqueWork(UNIQUE_NAME)
            return
        }
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, session.dailyHour)
            set(Calendar.MINUTE, session.dailyMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = next.timeInMillis - now.timeInMillis
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
        Log.d(TAG, "Daily reminder scheduled in ${delay / 60000} minutes")
    }
}

class DailyReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val session = SessionManager(applicationContext)
        if (session.isLoggedIn && session.notifyDaily) {
            NotificationHelper.show(
                applicationContext,
                applicationContext.getString(R.string.notif_daily_title),
                applicationContext.getString(R.string.notif_daily_body)
            )
            DailyReminderScheduler.schedule(applicationContext, session) // set up tomorrow's reminder
        }
        return Result.success()
    }
}
