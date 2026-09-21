package com.studyflow.app.data.repository

import com.studyflow.app.data.local.dao.UserDao
import com.studyflow.app.data.local.entity.UserEntity
import com.studyflow.app.data.remote.ApiService
import com.studyflow.app.data.remote.GoogleAuthRequest
import com.studyflow.app.data.remote.LoginRequest
import com.studyflow.app.data.remote.RegisterRequest
import com.studyflow.app.data.remote.UserDto
import com.studyflow.app.util.SessionManager

sealed class AuthResult {
    data class Success(val user: UserDto) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    private val api: ApiService,
    private val userDao: UserDao,
    private val session: SessionManager
) {

    suspend fun register(name: String, email: String, password: String): AuthResult = try {
        val response = api.register(RegisterRequest(name, email, password))
        if (response.isSuccessful && response.body() != null) {
            persistSession(response.body()!!)
            AuthResult.Success(response.body()!!.user)
        } else {
            AuthResult.Error(response.errorBody()?.string() ?: "Registration failed")
        }
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Network error during registration")
    }

    suspend fun login(email: String, password: String): AuthResult = try {
        val response = api.login(LoginRequest(email, password))
        if (response.isSuccessful && response.body() != null) {
            persistSession(response.body()!!)
            AuthResult.Success(response.body()!!.user)
        } else {
            AuthResult.Error("Invalid email or password")
        }
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Network error during login")
    }

    suspend fun loginWithGoogle(idToken: String): AuthResult = try {
        val response = api.loginWithGoogle(GoogleAuthRequest(idToken))
        if (response.isSuccessful && response.body() != null) {
            persistSession(response.body()!!)
            AuthResult.Success(response.body()!!.user)
        } else {
            AuthResult.Error("Google sign-in failed")
        }
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Network error during Google sign-in")
    }

    private suspend fun persistSession(auth: com.studyflow.app.data.remote.AuthResponse) {
        session.token = auth.token
        session.userId = auth.user.userId
        session.displayName = auth.user.name
        session.language = auth.user.language
        session.themePref = auth.user.themePref
        userDao.upsert(
            UserEntity(
                userId = auth.user.userId,
                name = auth.user.name,
                email = auth.user.email,
                language = auth.user.language,
                themePref = auth.user.themePref,
                xpPoints = auth.user.xpPoints
            )
        )
    }

    fun logout() = session.clear()
}
