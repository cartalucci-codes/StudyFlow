package com.studyflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class SyncStatus { PENDING, SYNCED, CONFLICT }
enum class Priority { Low, Med, High }
enum class TaskStatus { Open, Done }

/**
 * Local Room copy of a task. Everything the student does is written here FIRST (offline-first),
 * flagged PENDING, and a WorkManager job pushes it to the REST API as soon as there is a connection.
 *
 * The id is generated on the phone so the server can treat repeated pushes as the same task.
 * [deleted] is a tombstone: a task deleted while offline stays in Room (hidden from the UI)
 * until the delete has been sent to the server.
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
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val deleted: Boolean = false
)
