package com.studyflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class SyncStatus { PENDING, SYNCED, CONFLICT }
enum class Priority { Low, Med, High }
enum class TaskStatus { Open, Done }

/**
 * Local Room cache of a task. Everything is written here first (offline-first),
 * then a WorkManager job pushes PENDING rows to the StudyFlow REST API.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val taskId: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val description: String? = null,
    val subject: String? = null,
    val dueDate: Long, // epoch millis
    val priority: Priority = Priority.Med,
    val status: TaskStatus = TaskStatus.Open,
    val reminderTime: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
