package com.studyflow.app.ui.settings

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.messaging.FirebaseMessaging
import com.studyflow.app.BuildConfig
import com.studyflow.app.R
import com.studyflow.app.data.local.AppDatabase
import com.studyflow.app.data.remote.DeviceTokenRequest
import com.studyflow.app.data.remote.RetrofitClient
import com.studyflow.app.data.remote.UpdateSettingsRequest
import com.studyflow.app.data.repository.SyncScheduler
import com.studyflow.app.databinding.ActivitySettingsBinding
import com.studyflow.app.ui.login.LoginActivity
import com.studyflow.app.util.AppSettings
import com.studyflow.app.util.DailyReminderScheduler
import com.studyflow.app.util.SessionManager
import com.studyflow.app.util.Validators
import com.studyflow.app.util.applySystemBarInsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/**
 * Settings (Planning & Design doc, requirement #3): display name + avatar, interface language
 * (English / isiZulu / Afrikaans), light/dark theme, notification preferences, offline data & sync
 * status, and log out / delete account.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var session: SessionManager

    // Language names are shown in their own language so students can always find theirs.
    private val languages = listOf("en" to "English", "zu" to "isiZulu", "af" to "Afrikaans")
    private val avatars = listOf("🎓", "📚", "🦉", "🚀", "🌱", "⭐", "🧠", "🎧")

    private var reminderHour = 18
    private var reminderMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        session = SessionManager(this)
        val userId = session.userId ?: run { finish(); return }

        // --- Show the current settings ---
        binding.inputDisplayName.setText(session.displayName)
        binding.spinnerAvatar.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, avatars)
        binding.spinnerAvatar.setSelection(avatars.indexOf(session.avatar).coerceAtLeast(0))

        binding.spinnerLanguage.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages.map { it.second })
        binding.spinnerLanguage.setSelection(languages.indexOfFirst { it.first == AppSettings.effectiveLanguage(session) }.coerceAtLeast(0))

        val darkNow = when (session.theme) {
            "dark" -> true
            "light" -> false
            else -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        binding.switchDarkTheme.isChecked = darkNow
        binding.switchDeadlines.isChecked = session.notifyDeadlines
        binding.switchDaily.isChecked = session.notifyDaily
        reminderHour = session.dailyHour
        reminderMinute = session.dailyMinute
        updateReminderButton()
        binding.textVersion.text = getString(R.string.label_version, BuildConfig.VERSION_NAME)

        // --- Sync status ---
        val db = AppDatabase.getInstance(this)
        var pendingTasks = 0
        var pendingSessions = 0
        fun renderSync() {
            val pending = pendingTasks + pendingSessions
            val last = session.lastSyncMillis
            val lastText = if (last > 0) getString(
                R.string.status_last_sync,
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(last))
            ) else getString(R.string.status_never_synced)
            binding.textSyncDetails.text =
                (if (pending > 0) resources.getQuantityString(R.plurals.sync_pending, pending, pending) else getString(R.string.status_synced)) +
                    "\n" + lastText
        }
        db.taskDao().observePendingCount(userId).observe(this) { pendingTasks = it; renderSync() }
        db.studySessionDao().observePendingCount(userId).observe(this) { pendingSessions = it; renderSync() }

        // --- Actions ---
        binding.buttonReminderTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                reminderHour = hour
                reminderMinute = minute
                updateReminderButton()
            }, reminderHour, reminderMinute, android.text.format.DateFormat.is24HourFormat(this)).show()
        }
        binding.buttonSyncNow.setOnClickListener {
            SyncScheduler.enqueue(this)
            Toast.makeText(this, R.string.msg_sync_started, Toast.LENGTH_SHORT).show()
        }
        binding.buttonTestNotification.setOnClickListener { sendTestNotification() }
        binding.buttonSaveSettings.setOnClickListener { saveSettings() }
        binding.buttonLogout.setOnClickListener { logout() }
        binding.buttonDeleteAccount.setOnClickListener { confirmDeleteAccount() }
    }

    private fun updateReminderButton() {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, reminderHour); set(Calendar.MINUTE, reminderMinute) }
        binding.buttonReminderTime.text = getString(R.string.label_daily_time, DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time))
    }

    private fun saveSettings() {
        val name = binding.inputDisplayName.text.toString().trim()
        binding.layoutDisplayName.error = if (Validators.isValidName(name)) null else getString(R.string.error_required)
        if (binding.layoutDisplayName.error != null) return

        val language = languages[binding.spinnerLanguage.selectedItemPosition].first
        val theme = if (binding.switchDarkTheme.isChecked) "dark" else "light"

        session.displayName = name
        session.avatar = avatars[binding.spinnerAvatar.selectedItemPosition]
        session.notifyDeadlines = binding.switchDeadlines.isChecked
        session.notifyDaily = binding.switchDaily.isChecked
        session.dailyHour = reminderHour
        session.dailyMinute = reminderMinute
        DailyReminderScheduler.schedule(this, session)

        // Tell the server too (best effort). This runs in its own scope because changing the language
        // below recreates this screen, which would cancel a request tied to the screen's lifecycle.
        val auth = session.authHeader()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                RetrofitClient.api.updateMe(auth, UpdateSettingsRequest(name, language, theme))
            } catch (e: Exception) {
                Log.w(TAG, "Settings saved on the phone; server update skipped: ${e.message}")
            }
        }

        Toast.makeText(this, R.string.msg_settings_saved, Toast.LENGTH_SHORT).show()
        session.language = language
        session.theme = theme
        AppSettings.apply(session) // switches the language/theme immediately (the screen re-creates itself)
    }

    private fun sendTestNotification() {
        lifecycleScope.launch {
            val message = try {
                val response = RetrofitClient.api.sendTestNotification(session.authHeader(), emptyMap())
                when {
                    response.isSuccessful -> R.string.msg_test_notification_sent
                    response.code() == 503 -> R.string.msg_push_not_configured
                    response.code() == 404 -> R.string.msg_no_device_registered
                    else -> R.string.msg_server_error
                }
            } catch (e: Exception) {
                Log.w(TAG, "Test notification failed: ${e.message}")
                R.string.msg_network_error
            }
            Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun logout() {
        stopPushForThisAccount()
        session.clear()
        goToLogin()
    }

    private fun confirmDeleteAccount() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_account_title)
            .setMessage(R.string.dialog_delete_account_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                val auth = session.authHeader()
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.api.deleteAccount(auth)
                        if (response.isSuccessful) {
                            Toast.makeText(this@SettingsActivity, R.string.msg_account_deleted, Toast.LENGTH_LONG).show()
                            session.clear()
                            goToLogin()
                        } else {
                            Toast.makeText(this@SettingsActivity, R.string.msg_server_error, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Account deletion failed: ${e.message}")
                        Toast.makeText(this@SettingsActivity, R.string.msg_network_error, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }

    /** Stops the server pushing this account's notifications to this phone after logging out. */
    private fun stopPushForThisAccount() {
        val auth = session.authHeader()
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    RetrofitClient.api.unregisterDeviceToken(auth, DeviceTokenRequest(token))
                } catch (e: Exception) {
                    Log.w(TAG, "Could not unregister device token: ${e.message}")
                }
            }
        }
        WorkManager.getInstance(this).cancelUniqueWork("studyflow_daily_reminder")
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    companion object {
        private const val TAG = "SettingsActivity"
    }
}
