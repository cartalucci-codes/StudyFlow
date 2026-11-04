package com.studyflow.app.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.messaging.FirebaseMessaging
import com.studyflow.app.R
import com.studyflow.app.data.local.AppDatabase
import com.studyflow.app.data.local.entity.StudySessionEntity
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.local.entity.TaskStatus
import com.studyflow.app.data.remote.DeviceTokenRequest
import com.studyflow.app.data.remote.RetrofitClient
import com.studyflow.app.data.repository.SyncScheduler
import com.studyflow.app.data.repository.TaskRepository
import com.studyflow.app.databinding.ActivityHomeBinding
import com.studyflow.app.ui.login.LoginActivity
import com.studyflow.app.ui.settings.SettingsActivity
import com.studyflow.app.ui.tasks.AddEditTaskActivity
import com.studyflow.app.ui.tasks.TaskAdapter
import com.studyflow.app.ui.tasks.TaskListActivity
import com.studyflow.app.ui.timer.StudyTimerActivity
import com.studyflow.app.util.BadgeType
import com.studyflow.app.util.DailyReminderScheduler
import com.studyflow.app.util.Gamification
import com.studyflow.app.util.NetworkLiveData
import com.studyflow.app.util.SessionManager
import com.studyflow.app.util.applySystemBarInsets
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * "My Day" dashboard (Planning & Design doc, Section 4.1): what is due today or overdue, the
 * XP / level / streak / badge progress, and the offline-sync status.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var session: SessionManager
    private lateinit var repository: TaskRepository
    private lateinit var adapter: TaskAdapter

    private var allTasks: List<TaskEntity> = emptyList()
    private var completedSessions: List<StudySessionEntity> = emptyList()
    private var pendingTasks = 0
    private var pendingSessions = 0
    private var online = true

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> Log.d(TAG, "Notification permission granted = $granted") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        session = SessionManager(this)
        val userId = session.userId
        if (!session.isLoggedIn || userId == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val db = AppDatabase.getInstance(this)
        repository = TaskRepository(db.taskDao(), db.studySessionDao(), RetrofitClient.api, session)

        adapter = TaskAdapter(
            onClick = { task ->
                startActivity(Intent(this, AddEditTaskActivity::class.java).putExtra(AddEditTaskActivity.EXTRA_TASK_ID, task.taskId))
            },
            onToggleDone = { task, done -> toggleDone(task, done) }
        )
        binding.recyclerToday.layoutManager = LinearLayoutManager(this)
        binding.recyclerToday.adapter = adapter

        binding.textGreeting.text = getString(R.string.greeting_format, session.displayName ?: "")
        binding.textAvatar.text = session.avatar

        // Everything on this screen comes from the local Room database, so it works offline.
        repository.observeTasks(userId).observe(this) { tasks ->
            allTasks = tasks
            render()
        }
        db.studySessionDao().observeCompletedSessions(userId).observe(this) { sessions ->
            completedSessions = sessions
            render()
        }
        db.taskDao().observePendingCount(userId).observe(this) { pendingTasks = it; render() }
        db.studySessionDao().observePendingCount(userId).observe(this) { pendingSessions = it; render() }
        NetworkLiveData(this).observe(this) { isOnline -> online = isOnline; render() }

        binding.buttonAddTask.setOnClickListener { startActivity(Intent(this, AddEditTaskActivity::class.java)) }
        binding.navCalendar.setOnClickListener { startActivity(Intent(this, TaskListActivity::class.java)) }
        binding.navTimer.setOnClickListener { startActivity(Intent(this, StudyTimerActivity::class.java)) }
        binding.navSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        SyncScheduler.enqueue(this) // push anything saved while offline, as soon as we're online
        DailyReminderScheduler.schedule(this, session)
        requestNotificationPermissionIfNeeded()
        registerDeviceTokenForPush()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            binding.textGreeting.text = getString(R.string.greeting_format, session.displayName ?: "")
            binding.textAvatar.text = session.avatar
        }
    }

    /** Re-draws the whole dashboard from the latest data. */
    private fun render() {
        if (!::adapter.isInitialized) return

        // --- Today & overdue tasks ---
        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        val today = allTasks.filter { it.status == TaskStatus.Open && it.dueDate <= endOfToday }
        adapter.submitList(today)
        binding.textEmpty.visibility = if (today.isEmpty()) View.VISIBLE else View.GONE
        binding.textTaskCount.text = resources.getQuantityString(R.plurals.tasks_due_today, today.size, today.size)

        // --- Gamification: XP, level, streak, badges (shared rules in util/Gamification.kt) ---
        val doneTasks = allTasks.count { it.status == TaskStatus.Done }
        val focusMinutes = completedSessions.sumOf { Gamification.xpForSession(it.durationMins) }
        val xp = Gamification.totalXp(doneTasks, focusMinutes)
        val level = Gamification.level(xp)
        val studyDays = completedSessions.map { Gamification.dayIndex(it.startTime) }.toSet()
        val streak = Gamification.currentStreak(studyDays, Gamification.dayIndex(System.currentTimeMillis()))
        val badges = Gamification.earnedBadges(completedSessions.size, Gamification.longestStreak(studyDays))

        binding.progressXp.max = Gamification.XP_PER_LEVEL
        binding.progressXp.setProgressCompat(Gamification.xpIntoLevel(xp), true)
        binding.textXp.text = getString(R.string.xp_format, xp)
        binding.textLevel.text = getString(R.string.level_format, level)
        binding.textXpToNext.text = getString(R.string.xp_to_next, Gamification.xpToNextLevel(xp), level + 1)
        binding.textStreak.text = "🔥 " + resources.getQuantityString(R.plurals.streak_days, streak, streak)
        binding.textBadges.text = if (badges.isEmpty()) getString(R.string.badges_none)
        else badges.joinToString("  ") { "🏅 " + getString(badgeLabel(it)) }

        // --- Offline / sync status ---
        binding.textOfflineBanner.visibility = if (online) View.GONE else View.VISIBLE
        val pending = pendingTasks + pendingSessions
        binding.textSyncStatus.text = when {
            pending > 0 -> "⏳ " + resources.getQuantityString(R.plurals.sync_pending, pending, pending)
            session.lastSyncMillis > 0 -> "☁ " + getString(R.string.status_synced)
            else -> "☁ " + getString(R.string.status_never_synced)
        }
    }

    private fun badgeLabel(type: BadgeType): Int = when (type) {
        BadgeType.FIRST_SESSION -> R.string.badge_first
        BadgeType.STREAK_3 -> R.string.badge_streak_3
        BadgeType.STREAK_7 -> R.string.badge_streak_7
        BadgeType.STREAK_30 -> R.string.badge_streak_30
    }

    private fun toggleDone(task: TaskEntity, done: Boolean) {
        lifecycleScope.launch {
            repository.setDone(task, done)
            SyncScheduler.enqueue(this@HomeActivity)
            if (done) {
                Toast.makeText(this@HomeActivity, getString(R.string.msg_task_done_xp, Gamification.XP_PER_TASK), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Android 13+ needs the notification permission granted at runtime before any notification shows. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Sends this phone's FCM token to the API so the server can push notifications to it. */
    private fun registerDeviceTokenForPush() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                lifecycleScope.launch {
                    try {
                        RetrofitClient.api.registerDeviceToken(session.authHeader(), DeviceTokenRequest(token))
                        Log.d(TAG, "Device token registered with the API")
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not register device token (will retry next launch): ${e.message}")
                    }
                }
            }
            .addOnFailureListener { Log.w(TAG, "Could not get an FCM token: ${it.message}") }
    }

    companion object {
        private const val TAG = "HomeActivity"
    }
}
