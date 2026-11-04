package com.studyflow.app.data.remote

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.studyflow.app.R
import com.studyflow.app.util.NotificationHelper
import com.studyflow.app.util.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives push notifications from Firebase Cloud Messaging: a task due within the hour, a missed
 * deadline, or a test notification. The API sends "data" messages, so this method runs whether the
 * app is open or closed, and the notification is built here - which lets it respect the student's
 * notification settings.
 */
class StudyFlowMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received")
        val session = SessionManager(applicationContext)
        if (!session.isLoggedIn) return // the token is registered after the next login instead
        scope.launch {
            try {
                RetrofitClient.api.registerDeviceToken(session.authHeader(), DeviceTokenRequest(token))
            } catch (e: Exception) {
                Log.w(TAG, "Could not register refreshed token: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val session = SessionManager(applicationContext)
        if (!session.isLoggedIn || !session.notifyDeadlines) {
            Log.d(TAG, "Push ignored (logged out or deadline alerts switched off in Settings)")
            return
        }
        val title = message.data["title"] ?: message.notification?.title ?: getString(R.string.app_name)
        val body = message.data["body"] ?: message.notification?.body ?: return
        Log.d(TAG, "Push received: $title")
        NotificationHelper.show(applicationContext, title, body)
    }

    companion object {
        private const val TAG = "StudyFlowFCM"
    }
}
