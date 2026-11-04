package com.studyflow.app.data.repository

import com.studyflow.app.data.local.entity.Priority
import com.studyflow.app.data.local.entity.StudySessionEntity
import com.studyflow.app.data.local.entity.SyncStatus
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.local.entity.TaskStatus
import com.studyflow.app.data.remote.StudySessionDto
import com.studyflow.app.data.remote.TaskDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TaskMapperTest {

    @Test
    fun `iso dates round-trip without losing a millisecond`() {
        val now = 1_790_000_123_456L
        assertEquals(now, TaskMapper.parseIso(TaskMapper.formatIso(now)))
    }

    @Test
    fun `parses dates with and without milliseconds`() {
        assertNotNull(TaskMapper.parseIso("2026-10-12T18:00:00.000Z"))
        assertEquals(
            TaskMapper.parseIso("2026-10-12T18:00:00.000Z"),
            TaskMapper.parseIso("2026-10-12T18:00:00Z")
        )
    }

    @Test
    fun `unreadable dates return null instead of crashing`() {
        assertNull(TaskMapper.parseIso("not a date"))
        assertNull(TaskMapper.parseIso(""))
        assertNull(TaskMapper.parseIso(null))
    }

    @Test
    fun `task survives a trip to the server and back`() {
        val task = TaskEntity(
            userId = "u1", title = "Physics test", subject = "Physics", description = "Chapter 4",
            dueDate = 1_790_000_000_000L, priority = Priority.High, status = TaskStatus.Done,
            reminderTime = 1_789_996_400_000L
        )
        val back = TaskMapper.toEntity(TaskMapper.toDto(task), "u1")!!
        assertEquals(task.taskId, back.taskId)
        assertEquals(task.title, back.title)
        assertEquals(task.dueDate, back.dueDate)
        assertEquals(Priority.High, back.priority)
        assertEquals(TaskStatus.Done, back.status)
        assertEquals(task.reminderTime, back.reminderTime)
        assertEquals(SyncStatus.SYNCED, back.syncStatus) // anything that came from the server is synced
    }

    @Test
    fun `unusable server tasks are skipped`() {
        val noId = TaskDto(null, "x", null, null, "2026-10-12T18:00:00.000Z", "Med", "Open", null)
        val badDate = TaskDto("id", "x", null, null, "garbage", "Med", "Open", null)
        assertNull(TaskMapper.toEntity(noId, "u1"))
        assertNull(TaskMapper.toEntity(badDate, "u1"))
    }

    @Test
    fun `unknown priority or status from the server falls back to safe defaults`() {
        val dto = TaskDto("id", "x", null, null, "2026-10-12T18:00:00.000Z", "Urgent", "Weird", null)
        val entity = TaskMapper.toEntity(dto, "u1")!!
        assertEquals(Priority.Med, entity.priority)
        assertEquals(TaskStatus.Open, entity.status)
    }

    @Test
    fun `study session round-trips`() {
        val session = StudySessionEntity(userId = "u1", taskId = "t1", startTime = 1_790_000_000_000L, durationMins = 25, completed = true)
        val dto: StudySessionDto = TaskMapper.toDto(session)
        val back = TaskMapper.toEntity(dto, "u1")!!
        assertEquals(session.sessionId, back.sessionId)
        assertEquals(25, back.durationMins)
        assertEquals(true, back.completed)
        assertEquals("t1", back.taskId)
    }
}
