package com.studyflow.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.studyflow.app.util.AppSettings
import com.studyflow.app.util.DailyReminderScheduler
import com.studyflow.app.util.SessionManager

class StudyFlowApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val session = SessionManager(this)
        AppSettings.apply(session)                    // saved language + theme
        DailyReminderScheduler.schedule(this, session) // keep the daily reminder alive
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = getString(R.string.channel_description) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "studyflow_reminders"
    }
}
