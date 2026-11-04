package com.studyflow.app.ui.timer

import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyflow.app.R
import com.studyflow.app.data.local.AppDatabase
import com.studyflow.app.data.local.entity.StudySessionEntity
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.remote.RetrofitClient
import com.studyflow.app.data.repository.SyncScheduler
import com.studyflow.app.data.repository.TaskRepository
import com.studyflow.app.databinding.ActivityStudyTimerBinding
import com.studyflow.app.util.Gamification
import com.studyflow.app.util.SessionManager
import com.studyflow.app.util.applySystemBarInsets
import kotlinx.coroutines.launch

/**
 * Pomodoro-style focus timer (Planning & Design doc, requirement #9). A session can be linked to one
 * of the student's open tasks. Finished sessions are saved locally, synced to the API, and earn XP
 * and streak badges (requirement #8). The 1-minute option exists so the feature can be demoed quickly.
 */
class StudyTimerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudyTimerBinding
    private lateinit var repository: TaskRepository
    private lateinit var session: SessionManager

    private var openTasks: List<TaskEntity> = emptyList()
    private val durationOptions = listOf(1, 15, 25, 50)

    private var countDownTimer: CountDownTimer? = null
    private var totalMillis = 25 * 60_000L
    private var remainingMillis = totalMillis
    private var startedAtWallClock = 0L
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudyTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        session = SessionManager(this)
        val db = AppDatabase.getInstance(this)
        repository = TaskRepository(db.taskDao(), db.studySessionDao(), RetrofitClient.api, session)

        // Duration choices
        binding.spinnerDuration.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            durationOptions.map { getString(R.string.label_minutes, it) }
        )
        binding.spinnerDuration.setSelection(durationOptions.indexOf(25))

        // "Link to a task" choices: free session + the student's open tasks
        lifecycleScope.launch {
            val userId = session.userId ?: return@launch
            openTasks = repository.getOpenTasks(userId)
            val names = listOf(getString(R.string.timer_free_session)) + openTasks.map { it.title }
            binding.spinnerTask.adapter = ArrayAdapter(this@StudyTimerActivity, android.R.layout.simple_spinner_dropdown_item, names)
        }

        updateDisplay()
        binding.buttonStartPause.setOnClickListener { if (running) pauseTimer() else startTimer() }
        binding.buttonEndSession.setOnClickListener { endSession(completed = false) }

        // Going back mid-session asks first, so a session isn't thrown away by accident.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (startedAtWallClock == 0L) {
                    finish()
                } else {
                    MaterialAlertDialogBuilder(this@StudyTimerActivity)
                        .setTitle(R.string.dialog_leave_session_title)
                        .setMessage(R.string.dialog_leave_session_message)
                        .setNegativeButton(R.string.action_keep_going, null)
                        .setPositiveButton(R.string.action_end_session) { _, _ -> endSession(completed = false) }
                        .show()
                }
            }
        })
    }

    private fun startTimer() {
        if (startedAtWallClock == 0L) {
            // First start: lock in the chosen duration
            totalMillis = durationOptions[binding.spinnerDuration.selectedItemPosition] * 60_000L
            remainingMillis = totalMillis
            startedAtWallClock = System.currentTimeMillis()
        }
        running = true
        binding.spinnerDuration.isEnabled = false
        binding.spinnerTask.isEnabled = false
        binding.buttonStartPause.setText(R.string.action_pause)

        val endAt = SystemClock.elapsedRealtime() + remainingMillis
        countDownTimer = object : CountDownTimer(remainingMillis, 250) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                updateDisplay()
            }

            override fun onFinish() {
                remainingMillis = 0
                updateDisplay()
                endSession(completed = true)
            }
        }.start()
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        running = false
        binding.buttonStartPause.setText(R.string.action_resume)
    }

    /** Saves the session (if it lasted long enough to count) and closes the screen. */
    private fun endSession(completed: Boolean) {
        countDownTimer?.cancel()
        running = false
        val userId = session.userId
        val elapsedMillis = totalMillis - remainingMillis
        val minutes = if (completed) (totalMillis / 60_000L).toInt() else (elapsedMillis / 60_000L).toInt()

        // Ending before a full minute has passed isn't worth recording.
        if (userId == null || startedAtWallClock == 0L || minutes < 1) {
            finish()
            return
        }

        val linkedTask = binding.spinnerTask.selectedItemPosition.let { if (it > 0) openTasks.getOrNull(it - 1) else null }
        lifecycleScope.launch {
            repository.saveStudySession(
                StudySessionEntity(
                    userId = userId,
                    taskId = linkedTask?.taskId,
                    startTime = startedAtWallClock,
                    durationMins = minutes,
                    completed = completed
                )
            )
            SyncScheduler.enqueue(this@StudyTimerActivity)
            Log.i(TAG, "Study session saved: $minutes min, completed=$completed, task=${linkedTask?.taskId}")
            val message = if (completed) getString(R.string.msg_session_logged, Gamification.xpForSession(minutes))
            else getString(R.string.msg_session_ended_early)
            Toast.makeText(this@StudyTimerActivity, message, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun updateDisplay() {
        val totalSeconds = (remainingMillis + 999) / 1000
        binding.textTimer.text = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
        binding.progressTimer.max = 1000
        binding.progressTimer.progress =
            if (totalMillis > 0) (1000 - remainingMillis * 1000 / totalMillis).toInt() else 0
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "StudyTimer"
    }
}
