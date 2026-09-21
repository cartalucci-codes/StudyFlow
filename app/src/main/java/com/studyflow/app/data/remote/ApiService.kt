package com.studyflow.app.data.remote

import retrofit2.Response
import retrofit2.http.*

data class RegisterRequest(val name: String, val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class GoogleAuthRequest(val idToken: String)
data class UserDto(
    val userId: String,
    val name: String,
    val email: String,
    val language: String,
    val themePref: String,
    val xpPoints: Int
)
data class AuthResponse(val token: String, val user: UserDto)

data class TaskDto(
    val taskId: String?,
    val title: String,
    val description: String?,
    val subject: String?,
    val dueDate: String, // ISO-8601
    val priority: String,
    val status: String? = "Open",
    val reminderTime: String?
)

data class StudySessionDto(
    val sessionId: String?,
    val taskId: String?,
    val startTime: String,
    val durationMins: Int,
    val completed: Boolean
)

data class UpdateSettingsRequest(val name: String?, val language: String?, val themePref: String?)
data class DeviceTokenRequest(val fcmToken: String)

/**
 * Retrofit definition of the StudyFlow REST API.
 * Matches the endpoint table in the Part 1 Planning and Design document, Section 5.
 */
interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("api/auth/google")
    suspend fun loginWithGoogle(@Body body: GoogleAuthRequest): Response<AuthResponse>

    @GET("api/tasks")
    suspend fun getTasks(@Header("Authorization") bearer: String): Response<List<TaskDto>>

    @POST("api/tasks")
    suspend fun createTask(@Header("Authorization") bearer: String, @Body body: TaskDto): Response<TaskDto>

    @PUT("api/tasks/{id}")
    suspend fun updateTask(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
        @Body body: TaskDto
    ): Response<TaskDto>

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Header("Authorization") bearer: String, @Path("id") id: String): Response<Unit>

    @GET("api/studysessions")
    suspend fun getStudySessions(@Header("Authorization") bearer: String): Response<List<StudySessionDto>>

    @POST("api/studysessions")
    suspend fun createStudySession(
        @Header("Authorization") bearer: String,
        @Body body: StudySessionDto
    ): Response<StudySessionDto>

    @GET("api/users/me")
    suspend fun getMe(@Header("Authorization") bearer: String): Response<UserDto>

    @PUT("api/users/me")
    suspend fun updateMe(
        @Header("Authorization") bearer: String,
        @Body body: UpdateSettingsRequest
    ): Response<UserDto>

    @POST("api/notifications/register")
    suspend fun registerDeviceToken(
        @Header("Authorization") bearer: String,
        @Body body: DeviceTokenRequest
    ): Response<Unit>

    @POST("api/notifications/send-test")
    suspend fun sendTestNotification(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<Unit>
}
