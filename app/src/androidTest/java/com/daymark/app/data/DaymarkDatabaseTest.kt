package com.daymark.app.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DaymarkDatabaseTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DaymarkDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

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

    @Test fun completingTwoOccurrencesOfSameSeriesOnSameDaySucceeds() = runBlocking {
        val repository = DaymarkRepository(database)
        val today = LocalDate.now()
        val task1 = TaskEntity(
            title = "Daily Review",
            dueEpochDay = today.toEpochDay(),
            dueMinuteOfDay = 540
        )
        repository.saveTask(
            candidate = task1,
            recurrenceFrequency = "DAILY",
            recurrenceInterval = 1,
            recurrenceOccurrenceLimit = 5
        )

        repository.completeTask(task1.id)
        val series = database.taskDao().getSeries(task1.id)
        val task2 = series.single { it.recurrenceGenerated }
        assertEquals(today.plusDays(1).toEpochDay(), task2.dueEpochDay)

        // Completing tomorrow's occurrence today should not collide on seriesId + occurrenceDateEpochDay
        repository.completeTask(task2.id)

        val completions = database.taskCompletionDao().getAll()
        val seriesCompletions = completions.filter { it.seriesId == (task1.seriesId ?: task1.id) }
        assertEquals(2, seriesCompletions.size)
        assertTrue(seriesCompletions.any { it.occurrenceDateEpochDay == today.toEpochDay() })
        assertTrue(seriesCompletions.any { it.occurrenceDateEpochDay == today.plusDays(1).toEpochDay() })
    }

    @Test fun deleteTaskRemovesRemindersAndRestoreBringsThemBack() = runBlocking {
        val repository = DaymarkRepository(database)
        val task = TaskEntity(title = "Buy groceries", dueEpochDay = LocalDate.now().toEpochDay(), dueMinuteOfDay = 600)
        val subtask = SubTaskEntity(taskId = task.id, title = "Apples")
        repository.saveTaskWithSubtasks(task, subtasks = listOf(subtask), reminderOffsets = setOf(0, 15))

        val initialReminders = database.reminderDao().forOwner(DaymarkRepository.OWNER_TASK, task.id)
        assertEquals(2, initialReminders.size)
        assertEquals(1, database.subTaskDao().getForTask(task.id).size)

        val snapshot = repository.deleteTask(task.id)
        assertNotNull(snapshot)
        assertNull(database.taskDao().get(task.id))
        assertTrue(database.reminderDao().forOwner(DaymarkRepository.OWNER_TASK, task.id).isEmpty())
        assertTrue(database.subTaskDao().getForTask(task.id).isEmpty())

        repository.restoreTask(snapshot!!)
        assertNotNull(database.taskDao().get(task.id))
        val restoredReminders = database.reminderDao().forOwner(DaymarkRepository.OWNER_TASK, task.id)
        assertEquals(2, restoredReminders.size)
        assertEquals(1, database.subTaskDao().getForTask(task.id).size)
    }

    @Test fun saveTaskWithSubtasksIsAtomic() = runBlocking {
        val repository = DaymarkRepository(database)
        val task = TaskEntity(title = "Trip Prep", dueEpochDay = LocalDate.now().toEpochDay())
        val subtasks = listOf(
            SubTaskEntity(taskId = task.id, title = "Passport", sortOrder = 0),
            SubTaskEntity(taskId = task.id, title = "Tickets", sortOrder = 1)
        )
        repository.saveTaskWithSubtasks(task, subtasks = subtasks, reminderOffsets = setOf(30))

        val savedTask = database.taskDao().get(task.id)
        assertNotNull(savedTask)
        val savedSubs = database.subTaskDao().getForTask(task.id)
        assertEquals(2, savedSubs.size)
        assertEquals(listOf("Passport", "Tickets"), savedSubs.map { it.title })
    }

    @Test fun migrate1To2AddsAccentKey() {
        val dbName = "migration-test"
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO user_preferences (id, displayName, themeKey, use24HourClock, notificationSoundKey, soundVolume, vibrationEnabled, firstDayOfWeek) VALUES ('singleton', 'Alex', 'SYSTEM', 0, 'DEFAULT', 1.0, 1, 1)")
            close()
        }

        val dbV2 = helper.runMigrationsAndValidate(dbName, 2, true, DaymarkDatabase.MIGRATION_1_2)
        val cursor = dbV2.query("SELECT accentKey, loudReminders FROM user_preferences WHERE id = 'singleton'")
        cursor.moveToFirst()
        val accentKey = cursor.getString(0)
        val loudReminders = cursor.getInt(1)
        assertEquals("LIGHT", accentKey)
        assertEquals(1, loudReminders)
        cursor.close()
        dbV2.close()
    }
}
