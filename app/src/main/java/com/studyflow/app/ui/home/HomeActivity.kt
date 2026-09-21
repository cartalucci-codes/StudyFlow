package com.studyflow.app.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessaging
import com.studyflow.app.data.local.AppDatabase
import com.studyflow.app.data.remote.DeviceTokenRequest
import com.studyflow.app.data.remote.RetrofitClient
import com.studyflow.app.data.repository.SyncWorker
import com.studyflow.app.databinding.ActivityHomeBinding
import com.studyflow.app.ui.settings.SettingsActivity
import com.studyflow.app.ui.tasks.AddEditTaskActivity
import com.studyflow.app.ui.tasks.TaskListActivity
import com.studyflow.app.ui.timer.StudyTimerActivity
import com.studyflow.app.util.SessionManager
import kotlinx.coroutines.launch

/**
 * "My Day" dashboard: today's due tasks, XP progress, and bottom navigation
 * to Calendar/Tasks, the Study Timer and Settings (see Planning & Design doc, Section 4.1).
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var session: SessionManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op either way; notifications just won't show if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        binding.textGreeting.text = getString(com.studyflow.app.R.string.greeting_format, session.displayName ?: "")

        observeTasks()
        scheduleBackgroundSync()
        requestNotificationPermissionIfNeeded()
        registerDeviceTokenForPush()

        binding.buttonAddTask.setOnClickListener {
            startActivity(Intent(this, AddEditTaskActivity::class.java))
        }
        binding.navCalendar.setOnClickListener {
            startActivity(Intent(this, TaskListActivity::class.java))
        }
        binding.navTimer.setOnClickListener {
            startActivity(Intent(this, StudyTimerActivity::class.java))
        }
        binding.navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun observeTasks() {
        val userId = session.userId ?: return
        val dao = AppDatabase.getInstance(this).taskDao()
        dao.observeTasksForUser(userId).observe(this, Observer { tasks ->
            val dueToday = tasks.filter { it.status.name == "Open" }
            binding.textTaskCount.text = resources.getQuantityString(
                com.studyflow.app.R.plurals.tasks_due_today, dueToday.size, dueToday.size
            )
        })
    }

    /** Kicks off a one-off, network-constrained WorkManager job to push pending offline changes. */
    private fun scheduleBackgroundSync() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(constraints).build()
        WorkManager.getInstance(this).enqueue(request)
    }

    /** Android 13+ requires runtime permission before any notification (including FCM) can show. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /** Fetches this device's current FCM token and sends it to the API so the server can push to it. */
    private fun registerDeviceTokenForPush() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            lifecycleScope.launch {
                try {
                    RetrofitClient.api.registerDeviceToken(session.authHeader(), DeviceTokenRequest(token))
                } catch (_: Exception) {
                    // Fine to ignore here - onNewToken in StudyFlowMessagingService will retry
                    // on the next token refresh, and this isn't a blocking feature for login.
                }
            }
        }
    }
}
