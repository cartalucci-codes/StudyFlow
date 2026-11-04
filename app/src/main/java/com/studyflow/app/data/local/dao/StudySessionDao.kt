package com.studyflow.app.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyflow.app.data.local.entity.StudySessionEntity

@Dao
interface StudySessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: StudySessionEntity)

    @Update
    suspend fun update(session: StudySessionEntity)

    /** Completed focus sessions - the source for XP, streaks and badges. */
    @Query("SELECT * FROM study_sessions WHERE userId = :userId AND completed = 1")
    fun observeCompletedSessions(userId: String): LiveData<List<StudySessionEntity>>

    @Query("SELECT COUNT(*) FROM study_sessions WHERE userId = :userId AND syncStatus = 'PENDING'")
    fun observePendingCount(userId: String): LiveData<Int>

    @Query("SELECT * FROM study_sessions WHERE userId = :userId AND syncStatus = 'PENDING'")
    suspend fun getPending(userId: String): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions WHERE sessionId = :id LIMIT 1")
    suspend fun getById(id: String): StudySessionEntity?
}
