package com.studyflow.app.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TaskEntityTest {

    @Test
    fun `new task defaults to Open, Pending sync, not deleted`() {
        val task = TaskEntity(userId = "user-1", title = "Finish the build", dueDate = System.currentTimeMillis())
        assertEquals(TaskStatus.Open, task.status)
        assertEquals(SyncStatus.PENDING, task.syncStatus)
        assertEquals(Priority.Med, task.priority)
        assertFalse(task.deleted)
    }

    @Test
    fun `each task gets a unique id generated on the phone`() {
        val one = TaskEntity(userId = "u", title = "A", dueDate = 0L)
        val two = TaskEntity(userId = "u", title = "B", dueDate = 0L)
        assertNotEquals(one.taskId, two.taskId)
    }
}
