package com.studyflow.app.util

/** Input validation shared by the Register, Login and Add Task screens (pure Kotlin, unit tested). */
object Validators {
    private val EMAIL = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean = email.trim().length <= 150 && EMAIL.matches(email.trim())

    /** At least 8 characters, containing a letter and a number. */
    fun isStrongPassword(password: String): Boolean =
        password.length >= 8 && password.any { it.isLetter() } && password.any { it.isDigit() }

    fun isValidTitle(title: String): Boolean = title.trim().isNotEmpty() && title.trim().length <= 150

    fun isValidName(name: String): Boolean = name.trim().isNotEmpty() && name.trim().length <= 100
}
