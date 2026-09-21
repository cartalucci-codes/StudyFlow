package com.studyflow.app.util

import at.favre.lib.crypto.bcrypt.BCrypt

/**
 * Password hashing helper. The API is the source of truth for authentication
 * (it hashes with bcrypt server-side before the password ever touches the database),
 * but the app also uses this for local, offline-first checks - e.g. so a returning
 * user can unlock a cached session without a network call, without ever storing
 * their password in plain text on the device.
 */
object PasswordHasher {

    private const val COST_FACTOR = 12

    fun hash(plainTextPassword: String): String =
        BCrypt.withDefaults().hashToString(COST_FACTOR, plainTextPassword.toCharArray())

    fun verify(plainTextPassword: String, hash: String): Boolean =
        BCrypt.verifyer().verify(plainTextPassword.toCharArray(), hash).verified
}
