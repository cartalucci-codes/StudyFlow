package com.studyflow.app.ui.timer

import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.studyflow.app.data.local.AppDatabase
import com.studyflow.app.data.local.entity.StudySessionEntity
import com.studyflow.app.databinding.ActivityStudyTimerBinding
import com.studyflow.app.util.SessionManager
import kotlinx.coroutines.launch

/**
 * Pomodoro-style focus timer. Logs a StudySession (completed or abandoned) when the
 * session ends, which feeds the gamification XP system - see Planning & Design doc
 * requirement #9.
 */
class StudyTimerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudyTimerBinding
    private lateinit var session: SessionManager
    private var countDownTimer: CountDownTimer? = null
    private var sessionStartTime: Long = 0L
    private val totalMillis = 25 * 60 * 1000L // 25-minute Pomodoro
    private var millisRemaining = totalMillis
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudyTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        updateTimerText()

        binding.buttonStartPause.setOnClickListener {
            if (isRunning) pauseTimer() else startTimer()
        }
        binding.buttonEndSession.setOnClickListener { endSession(completed = false) }
    }

    private fun startTimer() {
        if (sessionStartTime == 0L) sessionStartTime = System.currentTimeMillis()
        isRunning = true
        binding.buttonStartPause.text = getString(com.studyflow.app.R.string.action_pause)

        countDownTimer = object : CountDownTimer(millisRemaining, 1000) {
            override fun onTick(remaining: Long) {
                millisRemaining = remaining
                updateTimerText()
            }
            override fun onFinish() {
                millisRemaining = 0
                updateTimerText()
                endSession(completed = true)
            }
        }.start()
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isRunning = false
        binding.buttonStartPause.text = getString(com.studyflow.app.R.string.action_resume)
    }

    private fun endSession(completed: Boolean) {
        countDownTimer?.cancel()
        val userId = session.userId ?: return
        val durationMins = ((totalMillis - millisRemaining) / 60000).toInt().coerceAtLeast(1)

        lifecycleScope.launch {
            AppDatabase.getInstance(this@StudyTimerActivity).studySessionDao().upsert(
                StudySessionEntity(
                    userId = userId,
                    taskId = null,
                    startTime = if (sessionStartTime != 0L) sessionStartTime else System.currentTimeMillis(),
                    durationMins = durationMins,
                    completed = completed
                )
            )
            finish()
        }
    }

    private fun updateTimerText() {
        val minutes = millisRemaining / 60000
        val seconds = (millisRemaining % 60000) / 1000
        binding.textTimer.text = String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
