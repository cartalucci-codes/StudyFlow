package com.studyflow.app.util

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.studyflow.app.R
import com.studyflow.app.StudyFlowApp
import com.studyflow.app.ui.home.HomeActivity

/** Builds and shows a StudyFlow notification (used for FCM pushes and the daily study reminder). */
object NotificationHelper {
    private const val TAG = "NotificationHelper"

    fun show(context: Context, title: String, body: String) {
        // Android 13+ needs the POST_NOTIFICATIONS runtime permission (requested on the Home screen).
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Notification permission not granted - not showing \"$title\"")
            return
        }

        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, StudyFlowApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_studyflow)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        Log.d(TAG, "Shown notification: $title")
    }
}
