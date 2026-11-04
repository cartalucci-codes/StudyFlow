package com.studyflow.app.data.repository

import android.util.Log
import com.studyflow.app.data.local.dao.UserDao
import com.studyflow.app.data.local.entity.UserEntity
import com.studyflow.app.data.remote.ApiService
import com.studyflow.app.data.remote.AuthResponse
import com.studyflow.app.data.remote.GoogleAuthRequest
import com.studyflow.app.data.remote.LoginRequest
import com.studyflow.app.data.remote.RegisterRequest
import com.studyflow.app.data.remote.UserDto
import com.studyflow.app.util.SessionManager
import java.io.IOException

enum class AuthError { INVALID_CREDENTIALS, EMAIL_TAKEN, INVALID_INPUT, NETWORK, SERVER, GOOGLE }

sealed class AuthResult {
    data class Success(val user: UserDto) : AuthResult()
    data class Error(val kind: AuthError) : AuthResult()
}

/** Register / log in / Google SSO against the StudyFlow API, then remember the session on the phone. */
class AuthRepository(
    private val api: ApiService,
    private val userDao: UserDao,
    private val session: SessionManager
) {

    suspend fun register(name: String, email: String, password: String): AuthResult =
        call("register") { api.register(RegisterRequest(name.trim(), email.trim(), password)) }

    suspend fun login(email: String, password: String): AuthResult =
        call("login") { api.login(LoginRequest(email.trim(), password)) }

    suspend fun loginWithGoogle(idToken: String): AuthResult =
        call("google") { api.loginWithGoogle(GoogleAuthRequest(idToken)) }

    private suspend fun call(
        label: String,
        request: suspend () -> retrofit2.Response<AuthResponse>
    ): AuthResult = try {
        val response = request()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            persistSession(body)
            Log.i(TAG, "$label succeeded")
            AuthResult.Success(body.user)
        } else {
            Log.w(TAG, "$label failed: HTTP ${response.code()}")
            AuthResult.Error(errorForCode(response.code(), label == "google"))
        }
    } catch (e: IOException) {
        Log.w(TAG, "$label failed - network problem: ${e.message}")
        AuthResult.Error(AuthError.NETWORK)
    } catch (e: Exception) {
        Log.e(TAG, "$label failed unexpectedly", e)
        AuthResult.Error(AuthError.SERVER)
    }

    private suspend fun persistSession(auth: AuthResponse) {
        session.token = auth.token
        session.userId = auth.user.userId
        session.displayName = auth.user.name
        userDao.upsert(
            UserEntity(
                userId = auth.user.userId,
                name = auth.user.name,
                email = auth.user.email,
                language = auth.user.language ?: "en",
                themePref = auth.user.themePref ?: "light",
                xpPoints = auth.user.xpPoints
            )
        )
    }

    companion object {
        private const val TAG = "AuthRepository"

        /** Maps an HTTP status code to what the student should be told. Pure, so it is unit tested. */
        fun errorForCode(code: Int, google: Boolean = false): AuthError = when {
            google && code in 400..499 -> AuthError.GOOGLE
            code == 401 -> AuthError.INVALID_CREDENTIALS
            code == 409 -> AuthError.EMAIL_TAKEN
            code in 400..499 -> AuthError.INVALID_INPUT
            else -> AuthError.SERVER
        }
    }
}
