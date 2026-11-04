package com.studyflow.app.data.repository

import com.studyflow.app.data.local.entity.Priority
import com.studyflow.app.data.local.entity.StudySessionEntity
import com.studyflow.app.data.local.entity.SyncStatus
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.local.entity.TaskStatus
import com.studyflow.app.data.remote.StudySessionDto
import com.studyflow.app.data.remote.TaskDto
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Converts between the Room entities (epoch millis) and the API's JSON objects (ISO-8601 UTC strings). */
object TaskMapper {

    private val PATTERNS = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'")

    private fun formatter(pattern: String) = SimpleDateFormat(pattern, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }

    fun formatIso(millis: Long): String = formatter(PATTERNS[0]).format(Date(millis))

    fun parseIso(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        for (pattern in PATTERNS) {
            try {
                return formatter(pattern).parse(value)?.time
            } catch (_: ParseException) {
                // try the next pattern
            }
        }
        return null
    }

    fun toDto(task: TaskEntity) = TaskDto(
        taskId = task.taskId,
        title = task.title,
        description = task.description,
        subject = task.subject,
        dueDate = formatIso(task.dueDate),
        priority = task.priority.name,
        status = task.status.name,
        reminderTime = task.reminderTime?.let { formatIso(it) }
    )

    /** Returns null if the server sent something unusable (missing id or unreadable due date). */
    fun toEntity(dto: TaskDto, userId: String): TaskEntity? {
        val id = dto.taskId ?: return null
        val due = parseIso(dto.dueDate) ?: return null
        return TaskEntity(
            taskId = id,
            userId = userId,
            title = dto.title,
            description = dto.description,
            subject = dto.subject,
            dueDate = due,
            priority = Priority.values().firstOrNull { it.name == dto.priority } ?: Priority.Med,
            status = TaskStatus.values().firstOrNull { it.name == dto.status } ?: TaskStatus.Open,
            reminderTime = parseIso(dto.reminderTime),
            syncStatus = SyncStatus.SYNCED
        )
    }

    fun toDto(session: StudySessionEntity) = StudySessionDto(
        sessionId = session.sessionId,
        taskId = session.taskId,
        startTime = formatIso(session.startTime),
        durationMins = session.durationMins,
        completed = session.completed
    )

    fun toEntity(dto: StudySessionDto, userId: String): StudySessionEntity? {
        val id = dto.sessionId ?: return null
        val start = parseIso(dto.startTime) ?: return null
        return StudySessionEntity(
            sessionId = id,
            userId = userId,
            taskId = dto.taskId,
            startTime = start,
            durationMins = dto.durationMins,
            completed = dto.completed,
            syncStatus = SyncStatus.SYNCED
        )
    }
}
