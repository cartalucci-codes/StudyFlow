package com.studyflow.app.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TaskEntityTest {

    @Test
    fun `new task defaults to Open status and Pending sync`() {
        val task = TaskEntity(
            userId = "user-1",
            title = "Finish Part 2 prototype",
            dueDate = System.currentTimeMillis()
        )
        assertEquals(TaskStatus.Open, task.status)
        assertEquals(SyncStatus.PENDING, task.syncStatus)
        assertEquals(Priority.Med, task.priority)
    }

    @Test
    fun `each task gets a unique generated id`() {
        val taskOne = TaskEntity(userId = "u", title = "A", dueDate = 0L)
        val taskTwo = TaskEntity(userId = "u", title = "B", dueDate = 0L)
        assertNotNull(taskOne.taskId)
        assert(taskOne.taskId != taskTwo.taskId)
    }
}
