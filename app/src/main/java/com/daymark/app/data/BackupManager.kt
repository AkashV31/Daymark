package com.daymark.app.data

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.Instant

@Serializable
data class DaymarkBackupFile(
    val format: String = FORMAT,
    val version: Int = CURRENT_VERSION,
    val createdAtUtc: String = Instant.now().toString(),
    val tasks: List<TaskEntity> = emptyList(),
    val subtasks: List<SubTaskEntity> = emptyList(),
    val recurrenceRules: List<RecurrenceRuleEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val deadlines: List<DeadlineEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val taskTags: List<TaskTagCrossRef> = emptyList(),
    val noteTags: List<NoteTagCrossRef> = emptyList(),
    val courses: List<CourseEntity> = emptyList(),
    val courseModules: List<CourseModuleEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val milestones: List<MilestoneEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val goalActivities: List<GoalActivityEntity> = emptyList(),
    val taskCompletions: List<TaskCompletionEntity> = emptyList(),
    val preferences: UserPreferencesEntity? = null,
    val dashboard: List<DashboardConfigurationEntity> = emptyList(),
    val notificationSounds: List<NotificationSoundEntity> = emptyList(),
    val notificationChannels: List<NotificationChannelConfigurationEntity> = emptyList(),
    val backupMetadata: List<BackupMetadataEntity> = emptyList()
) {
    companion object {
        const val FORMAT = "DAYMARK_BACKUP"
        const val CURRENT_VERSION = 1
    }
}

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Versioned JSON backup. Parsing and validation complete before any database write begins. */
class BackupManager(private val database: DaymarkDatabase, private val resolver: ContentResolver) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = true
    }

    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            val backup = snapshot()
            val output = resolver.openOutputStream(uri, "wt")
                ?: throw BackupException("The selected destination could not be opened.")
            output.bufferedWriter(Charsets.UTF_8).use { it.write(json.encodeToString(backup)) }
        } catch (error: BackupException) {
            throw error
        } catch (error: Exception) {
            throw BackupException("Daymark could not export this backup.", error)
        }
    }

    suspend fun restore(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            val input = resolver.openInputStream(uri)
                ?: throw BackupException("The selected backup could not be opened.")
            val text = input.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_BACKUP_BYTES) throw BackupException("This backup is larger than Daymark can safely import.")
                    output.write(buffer, 0, count)
                }
                output.toByteArray().toString(Charsets.UTF_8)
            }
            val backup = try {
                json.decodeFromString<DaymarkBackupFile>(text)
            } catch (error: Exception) {
                throw BackupException("This file is not a valid Daymark backup.", error)
            }
            validate(backup)
            database.withTransaction {
                // Insert parents before dependent rows so Room foreign keys remain enforced.
                database.recurrenceRuleDao().upsertAll(backup.recurrenceRules)
                database.goalDao().upsertAll(backup.goals)
                database.projectDao().upsertAll(backup.projects)
                database.courseDao().upsertAll(backup.courses)
                database.taskDao().upsertAll(backup.tasks)
                database.subTaskDao().upsertAll(backup.subtasks)
                database.eventDao().upsertAll(backup.events)
                database.deadlineDao().upsertAll(backup.deadlines)
                database.courseModuleDao().upsertAll(backup.courseModules)
                database.milestoneDao().upsertAll(backup.milestones)
                database.reminderDao().upsertAll(backup.reminders)
                database.noteDao().upsertAll(backup.notes)
                database.tagDao().upsertAll(backup.tags)
                database.tagDao().upsertCrossRefs(backup.taskTags)
                database.tagDao().upsertNoteCrossRefs(backup.noteTags)
                database.taskCompletionDao().upsertAll(backup.taskCompletions)
                database.goalActivityDao().upsertAll(backup.goalActivities)
                backup.preferences?.let { database.preferenceDao().upsert(it) }
                database.dashboardDao().upsertAll(backup.dashboard)
                database.soundDao().upsertAll(backup.notificationSounds)
                database.channelConfigurationDao().upsertAll(backup.notificationChannels)
                database.backupMetadataDao().upsertAll(backup.backupMetadata)
            }
        } catch (error: BackupException) {
            throw error
        } catch (error: Exception) {
            throw BackupException("The backup could not be restored. No partial changes were kept.", error)
        }
    }

    private suspend fun snapshot(): DaymarkBackupFile = database.withTransaction {
        DaymarkBackupFile(
            tasks = database.taskDao().getAll(),
            subtasks = database.subTaskDao().getAll(),
            recurrenceRules = database.recurrenceRuleDao().getAll(),
            events = database.eventDao().getAll(),
            deadlines = database.deadlineDao().getAll(),
            reminders = database.reminderDao().getAll(),
            notes = database.noteDao().getAll(),
            tags = database.tagDao().getAll(),
            taskTags = database.tagDao().getTaskCrossRefs(),
            noteTags = database.tagDao().getNoteCrossRefs(),
            courses = database.courseDao().getAll(),
            courseModules = database.courseModuleDao().getAll(),
            projects = database.projectDao().getAll(),
            milestones = database.milestoneDao().getAll(),
            goals = database.goalDao().getAll(),
            goalActivities = database.goalActivityDao().getAll(),
            taskCompletions = database.taskCompletionDao().getAll(),
            preferences = database.preferenceDao().get(),
            dashboard = database.dashboardDao().getAll(),
            notificationSounds = database.soundDao().getAll(),
            notificationChannels = database.channelConfigurationDao().getAll(),
            backupMetadata = database.backupMetadataDao().getAll()
        )
    }

    private fun validate(backup: DaymarkBackupFile) {
        if (backup.format != DaymarkBackupFile.FORMAT) throw BackupException("This is not a Daymark backup.")
        if (backup.version > DaymarkBackupFile.CURRENT_VERSION) {
            throw BackupException("This backup was created by a newer version of Daymark. Update the app before importing it.")
        }
        if (backup.version < 1) throw BackupException("This backup version is not supported.")
        if (backup.tasks.any { it.id.isBlank() || it.title.isBlank() || (it.dueMinuteOfDay != null && it.dueMinuteOfDay !in 0..1439) }) {
            throw BackupException("The backup contains an invalid task record.")
        }
        if (backup.events.any { it.id.isBlank() || it.title.isBlank() || (it.startMinuteOfDay != null && it.startMinuteOfDay !in 0..1439) }) {
            throw BackupException("The backup contains an invalid event record.")
        }
        if (backup.deadlines.any { it.id.isBlank() || it.title.isBlank() || (it.dueMinuteOfDay != null && it.dueMinuteOfDay !in 0..1439) }) {
            throw BackupException("The backup contains an invalid deadline record.")
        }
        if (backup.goals.any { it.id.isBlank() || it.title.isBlank() || it.progress !in 0..100 }) {
            throw BackupException("The backup contains an invalid goal record.")
        }
        if (backup.courses.any { it.id.isBlank() || it.title.isBlank() || it.progress !in 0..100 }) {
            throw BackupException("The backup contains an invalid course record.")
        }
        if (backup.reminders.any { it.id.isBlank() || it.ownerId.isBlank() || it.offsetMinutes < 0 }) {
            throw BackupException("The backup contains an invalid reminder record.")
        }
        if (backup.tasks.map { it.id }.toSet().size != backup.tasks.size) {
            throw BackupException("The backup contains duplicate task identifiers.")
        }
    }

    companion object {
        private const val MAX_BACKUP_BYTES = 50 * 1024 * 1024
    }
}
