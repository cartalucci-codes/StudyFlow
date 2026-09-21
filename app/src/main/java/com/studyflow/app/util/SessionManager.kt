package com.studyflow.app.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Small wrapper around SharedPreferences for the logged-in session:
 * JWT token, current userId, and the settings the Settings screen edits
 * (language, theme). Kept separate from Room, which holds task/session data.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("studyflow_session", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var displayName: String?
        get() = prefs.getString(KEY_NAME, null)
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var themePref: String
        get() = prefs.getString(KEY_THEME, "light") ?: "light"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    val isLoggedIn: Boolean
        get() = token != null

    fun authHeader(): String = "Bearer ${token ?: ""}"

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "display_name"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME = "theme"
    }
}
