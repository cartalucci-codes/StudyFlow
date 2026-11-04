package com.studyflow.app.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Small wrapper around SharedPreferences: the logged-in session (JWT + user) and the settings the
 * Settings screen edits. Logging out clears the login but keeps language/theme, which are
 * preferences of the phone rather than of one account.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("studyflow_session", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = putOrRemove(KEY_TOKEN, value)

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = putOrRemove(KEY_USER_ID, value)

    var displayName: String?
        get() = prefs.getString(KEY_NAME, null)
        set(value) = putOrRemove(KEY_NAME, value)

    /** "en", "zu" or "af". null = the student hasn't chosen yet, so follow the phone's language. */
    var language: String?
        get() = prefs.getString(KEY_LANGUAGE, null)
        set(value) = putOrRemove(KEY_LANGUAGE, value)

    /** "light" or "dark". null = follow the phone's theme. */
    var theme: String?
        get() = prefs.getString(KEY_THEME, null)
        set(value) = putOrRemove(KEY_THEME, value)

    var avatar: String
        get() = prefs.getString(KEY_AVATAR, DEFAULT_AVATAR) ?: DEFAULT_AVATAR
        set(value) = prefs.edit().putString(KEY_AVATAR, value).apply()

    var notifyDeadlines: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_DEADLINES, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_DEADLINES, value).apply()

    var notifyDaily: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_DAILY, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_DAILY, value).apply()

    var dailyHour: Int
        get() = prefs.getInt(KEY_DAILY_HOUR, 18)
        set(value) = prefs.edit().putInt(KEY_DAILY_HOUR, value).apply()

    var dailyMinute: Int
        get() = prefs.getInt(KEY_DAILY_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_DAILY_MINUTE, value).apply()

    var lastSyncMillis: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    val isLoggedIn: Boolean
        get() = token != null && userId != null

    fun authHeader(): String = "Bearer ${token ?: ""}"

    /** Log out: forget the account but keep phone-level preferences (language, theme). */
    fun clear() {
        prefs.edit()
            .remove(KEY_TOKEN).remove(KEY_USER_ID).remove(KEY_NAME).remove(KEY_LAST_SYNC)
            .apply()
    }

    private fun putOrRemove(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }

    companion object {
        const val DEFAULT_AVATAR = "🎓"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "display_name"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME = "theme"
        private const val KEY_AVATAR = "avatar"
        private const val KEY_NOTIFY_DEADLINES = "notify_deadlines"
        private const val KEY_NOTIFY_DAILY = "notify_daily"
        private const val KEY_DAILY_HOUR = "daily_hour"
        private const val KEY_DAILY_MINUTE = "daily_minute"
        private const val KEY_LAST_SYNC = "last_sync"
    }
}
