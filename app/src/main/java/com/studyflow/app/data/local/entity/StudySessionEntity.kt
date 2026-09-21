package com.studyflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey val sessionId: String = UUID.randomUUID().toString(),
    val userId: String,
    val taskId: String? = null,
    val startTime: Long,
    val durationMins: Int,
    val completed: Boolean,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
