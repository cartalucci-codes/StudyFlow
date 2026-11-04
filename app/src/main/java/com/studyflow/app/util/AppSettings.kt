package com.studyflow.app.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Applies the student's language and theme choices app-wide.
 *
 * Language uses AppCompat's per-app locale API, so the whole UI (every string in res/values,
 * res/values-af and res/values-zu) switches immediately. If the student has never chosen a
 * language, nothing is forced and Android picks the closest match to the phone's language - that is
 * how "detect the device locale on first launch" works.
 */
object AppSettings {
    val SUPPORTED_LANGUAGES = listOf("en", "zu", "af")

    fun apply(session: SessionManager) {
        session.language?.let { code ->
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
        }
        AppCompatDelegate.setDefaultNightMode(
            when (session.theme) {
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    /** The language currently showing: the saved choice, else the phone's language if we support it. */
    fun effectiveLanguage(session: SessionManager): String =
        session.language ?: Locale.getDefault().language.takeIf { it in SUPPORTED_LANGUAGES } ?: "en"
}
