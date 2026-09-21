package com.studyflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studyflow.app.data.local.entity.StudySessionEntity
import com.studyflow.app.data.local.entity.SyncStatus

@Dao
interface StudySessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: StudySessionEntity)

    @Query("SELECT * FROM study_sessions WHERE userId = :userId ORDER BY startTime DESC")
    suspend fun getSessionsForUser(userId: String): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions WHERE syncStatus = :status")
    suspend fun getSessionsBySyncStatus(status: SyncStatus = SyncStatus.PENDING): List<StudySessionEntity>
}
