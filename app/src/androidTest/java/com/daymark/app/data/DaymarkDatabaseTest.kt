package com.daymark.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DaymarkDatabaseTest {
    private val database: DaymarkDatabase by lazy {
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            DaymarkDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After fun closeDb() { if (database.isOpen) database.close() }

    @Test fun taskCreateEditCompleteAndUncompleteArePersisted() = runBlocking {
        val repository = DaymarkRepository(database)
        val original = TaskEntity(title = "Read chapter", dueEpochDay = LocalDate.now().toEpochDay(), dueMinuteOfDay = 600)
        repository.saveTask(original, reminderOffsets = setOf(0, 15))
        assertEquals("Read chapter", database.taskDao().get(original.id)?.title)
        assertEquals(setOf(0, 15), repository.reminderOffsets(DaymarkRepository.OWNER_TASK, original.id))

        repository.saveTask(original.copy(title = "Read chapter four"), reminderOffsets = setOf(15))
        assertEquals("Read chapter four", database.taskDao().get(original.id)?.title)
        assertEquals(setOf(15), repository.reminderOffsets(DaymarkRepository.OWNER_TASK, original.id))

        repository.completeTask(original.id)
        assertEquals(DaymarkRepository.STATUS_COMPLETED, database.taskDao().get(original.id)?.status)
        assertNotNull(database.taskCompletionDao().getAll().firstOrNull { it.taskId == original.id })

        repository.uncompleteTask(original.id)
        assertEquals(DaymarkRepository.STATUS_PLANNED, database.taskDao().get(original.id)?.status)
        assertNull(database.taskCompletionDao().getAll().firstOrNull { it.taskId == original.id })
    }

    @Test fun recurringTaskCreatesOneDeterministicNextOccurrence() = runBlocking {
        val repository = DaymarkRepository(database)
        val start = LocalDate.of(2026, 10, 2)
        val task = TaskEntity(title = "Practice", dueEpochDay = start.toEpochDay(), dueMinuteOfDay = 540)
        repository.saveTask(
            candidate = task,
            recurrenceFrequency = "DAILY",
            recurrenceInterval = 1,
            recurrenceOccurrenceLimit = 3,
            reminderOffsets = setOf(0, 30)
        )

        repository.completeTask(task.id)
        val series = database.taskDao().getSeries(task.id)
        assertEquals(2, series.size)
        val next = series.single { it.recurrenceGenerated }
        assertEquals(start.plusDays(1).toEpochDay(), next.dueEpochDay)
        assertEquals(setOf(0, 30), repository.reminderOffsets(DaymarkRepository.OWNER_TASK, next.id))

        repository.completeTask(task.id)
        assertEquals(2, database.taskDao().getSeries(task.id).size)
    }

    @Test fun remindersUseStableIdentityPerOwnerAndOffset() {
        val taskId = "task-1"
        val atTime = DaymarkRepository.reminderId(DaymarkRepository.OWNER_TASK, taskId, 0)
        assertEquals(atTime, DaymarkRepository.reminderId(DaymarkRepository.OWNER_TASK, taskId, 0))
        assertEquals(false, atTime == DaymarkRepository.reminderId(DaymarkRepository.OWNER_TASK, taskId, 15))
        assertEquals(false, atTime == DaymarkRepository.reminderId(DaymarkRepository.OWNER_EVENT, taskId, 0))
    }
}
