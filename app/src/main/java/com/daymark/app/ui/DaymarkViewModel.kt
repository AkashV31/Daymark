package com.daymark.app.ui

import android.app.Application
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.daymark.app.data.BackupManager
import com.daymark.app.data.DashboardConfigurationEntity
import com.daymark.app.data.DaymarkRepository
import com.daymark.app.data.DeadlineEntity
import com.daymark.app.data.EventEntity
import com.daymark.app.data.GoalEntity
import com.daymark.app.data.NoteEntity
import com.daymark.app.data.ProjectEntity
import com.daymark.app.data.SearchResult
import com.daymark.app.data.TaskEntity
import com.daymark.app.data.SubTaskEntity
import com.daymark.app.data.CourseModuleEntity
import com.daymark.app.data.MilestoneEntity
import com.daymark.app.data.UserPreferencesEntity
import com.daymark.app.data.CourseEntity
import com.daymark.app.data.NotificationSoundEntity
import com.daymark.app.data.RecurringDeleteScope
import com.daymark.app.notifications.NotificationChannels
import com.daymark.app.notifications.NotificationSoundImporter
import com.daymark.app.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

data class UserMessage(
    val text: String,
    val actionLabel: String? = null,
    val action: (suspend () -> Unit)? = null
)

class DaymarkViewModel(
    private val app: Application,
    private val repository: DaymarkRepository
) : AndroidViewModel(app) {
    val tasks = repository.tasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val events = repository.events.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val deadlines = repository.deadlines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val notes = repository.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val courses = repository.courses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val projects = repository.projects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val goals = repository.goals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val completions = repository.completions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val goalActivities = repository.goalActivities.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val preferences = repository.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferencesEntity())
    val dashboard = repository.dashboard.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sounds = repository.sounds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recurrenceRules = repository.observeRecurrenceRules().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<UserMessage>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()
    private val scheduler by lazy { ReminderScheduler(app) }
    private val errorHandler = CoroutineExceptionHandler { _, error ->
        _messages.tryEmit(UserMessage(error.message?.takeIf { it.isNotBlank() } ?: "Daymark could not complete that action."))
    }

    private suspend fun emitMessage(text: String, actionLabel: String? = null, action: (suspend () -> Unit)? = null) {
        _messages.emit(UserMessage(text, actionLabel, action))
    }

    fun saveTask(
        task: TaskEntity,
        frequency: String,
        interval: Int,
        weekdaysMask: Int,
        endEpochDay: Long?,
        occurrenceLimit: Int?,
        reminderOffsets: Set<Int>,
        editScope: String = "THIS_OCCURRENCE"
    ) = launchSafe {
        repository.saveTask(task, frequency, interval, weekdaysMask, endEpochDay, occurrenceLimit, reminderOffsets, editScope)
        val summary = scheduler.rescheduleAll()
        if (reminderOffsets.isNotEmpty() && !NotificationManagerCompat.from(app).areNotificationsEnabled()) {
            emitMessage("Saved, but notifications are off. Enable them in Settings to receive this reminder.")
        } else if (summary.inexactFallbacks > 0) {
            emitMessage("Saved. Android may deliver reminders less precisely until exact alarms are allowed.")
        }
    }

    fun saveTaskWithSubtasks(
        task: TaskEntity,
        frequency: String,
        interval: Int,
        weekdaysMask: Int,
        endEpochDay: Long?,
        occurrenceLimit: Int?,
        reminderOffsets: Set<Int>,
        editScope: String = "THIS_OCCURRENCE",
        subtasks: List<SubTaskEntity>
    ) = launchSafe {
        repository.saveTaskWithSubtasks(task, frequency, interval, weekdaysMask, endEpochDay, occurrenceLimit, reminderOffsets, editScope, subtasks)
        val summary = scheduler.rescheduleAll()
        if (reminderOffsets.isNotEmpty() && !NotificationManagerCompat.from(app).areNotificationsEnabled()) {
            emitMessage("Saved, but notifications are off. Enable them in Settings to receive this reminder.")
        } else if (summary.inexactFallbacks > 0) {
            emitMessage("Saved. Android may deliver reminders less precisely until exact alarms are allowed.")
        }
    }

    fun completeTask(taskId: String) = launchSafe {
        repository.completeTask(taskId)
        scheduler.cancelPosted(DaymarkRepository.OWNER_TASK, taskId)
        scheduler.rescheduleAll()
    }

    fun uncompleteTask(taskId: String) = launchSafe {
        repository.uncompleteTask(taskId)
        scheduler.rescheduleAll()
    }

    fun archiveTask(taskId: String) = launchSafe {
        repository.archiveTask(taskId)
        scheduler.cancelPosted(DaymarkRepository.OWNER_TASK, taskId)
        scheduler.rescheduleAll()
        emitMessage("Task archived.")
    }

    fun deleteTask(taskId: String) = launchSafe {
        val snapshot = repository.deleteTask(taskId) ?: return@launchSafe
        scheduler.cancelPosted(DaymarkRepository.OWNER_TASK, taskId)
        scheduler.rescheduleAll()
        emitMessage(
            text = "\"${snapshot.task.title}\" deleted",
            actionLabel = "Undo",
            action = {
                repository.restoreTask(snapshot)
                scheduler.rescheduleAll()
            }
        )
    }

    fun deleteRecurringTask(taskId: String, scope: RecurringDeleteScope) = launchSafe {
        val snapshot = repository.deleteRecurringTask(taskId, scope) ?: return@launchSafe
        scheduler.cancelPosted(DaymarkRepository.OWNER_TASK, taskId)
        scheduler.rescheduleAll()
        emitMessage(
            text = "\"${snapshot.task.title}\" deleted",
            actionLabel = "Undo",
            action = {
                repository.restoreTask(snapshot)
                scheduler.rescheduleAll()
            }
        )
    }

    fun saveEvent(event: EventEntity, reminders: Set<Int>) = launchSafe {
        repository.saveEvent(event, reminders)
        scheduler.rescheduleAll()
        if (reminders.isNotEmpty() && !NotificationManagerCompat.from(app).areNotificationsEnabled()) {
            emitMessage("Saved, but notifications are off. Enable them in Settings to receive this reminder.")
        }
    }

    fun deleteEvent(id: String) = launchSafe {
        val snapshot = repository.deleteEvent(id) ?: return@launchSafe
        scheduler.cancelPosted(DaymarkRepository.OWNER_EVENT, id)
        scheduler.rescheduleAll()
        emitMessage(
            text = "\"${snapshot.event.title}\" deleted",
            actionLabel = "Undo",
            action = {
                repository.restoreEvent(snapshot)
                scheduler.rescheduleAll()
            }
        )
    }

    fun saveDeadline(deadline: DeadlineEntity, reminders: Set<Int>) = launchSafe {
        repository.saveDeadline(deadline, reminders)
        scheduler.rescheduleAll()
        if (reminders.isNotEmpty() && !NotificationManagerCompat.from(app).areNotificationsEnabled()) {
            emitMessage("Saved, but notifications are off. Enable them in Settings to receive this reminder.")
        }
    }

    fun completeDeadline(id: String) = launchSafe {
        repository.completeDeadline(id)
        scheduler.cancelPosted(DaymarkRepository.OWNER_DEADLINE, id)
        scheduler.rescheduleAll()
    }

    fun deleteDeadline(id: String) = launchSafe {
        val snapshot = repository.deleteDeadline(id) ?: return@launchSafe
        scheduler.cancelPosted(DaymarkRepository.OWNER_DEADLINE, id)
        scheduler.rescheduleAll()
        emitMessage(
            text = "\"${snapshot.deadline.title}\" deleted",
            actionLabel = "Undo",
            action = {
                repository.restoreDeadline(snapshot)
                scheduler.rescheduleAll()
            }
        )
    }

    fun saveNote(note: NoteEntity) = launchSafe { repository.saveNote(note) }

    fun deleteNote(id: String) = launchSafe {
        val snapshot = repository.deleteNote(id) ?: return@launchSafe
        emitMessage(
            text = "\"${snapshot.note.title.ifBlank { "Note" }}\" deleted",
            actionLabel = "Undo",
            action = { repository.restoreNote(snapshot) }
        )
    }

    fun saveGoal(goal: GoalEntity) = launchSafe { repository.saveGoal(goal) }

    fun deleteGoal(id: String) = launchSafe {
        val snapshot = repository.deleteGoal(id) ?: return@launchSafe
        emitMessage(
            text = "\"${snapshot.goal.title}\" deleted",
            actionLabel = "Undo",
            action = { repository.restoreGoal(snapshot) }
        )
    }

    fun saveProject(project: ProjectEntity) = launchSafe { repository.saveProject(project) }

    fun saveProjectWithMilestones(project: ProjectEntity, milestones: List<MilestoneEntity>) = launchSafe {
        repository.saveProjectWithMilestones(project, milestones)
    }

    fun deleteProject(id: String) = launchSafe {
        val snapshot = repository.deleteProject(id) ?: return@launchSafe
        emitMessage(
            text = "\"${snapshot.project.title}\" deleted",
            actionLabel = "Undo",
            action = { repository.restoreProject(snapshot) }
        )
    }

    fun saveCourse(course: CourseEntity) = launchSafe { repository.saveCourse(course) }

    fun saveCourseWithModules(course: CourseEntity, modules: List<CourseModuleEntity>) = launchSafe {
        repository.saveCourseWithModules(course, modules)
    }

    fun deleteCourse(id: String) = launchSafe {
        val snapshot = repository.deleteCourse(id) ?: return@launchSafe
        emitMessage(
            text = "\"${snapshot.course.title}\" deleted",
            actionLabel = "Undo",
            action = { repository.restoreCourse(snapshot) }
        )
    }

    suspend fun getSubtasks(taskId: String): List<SubTaskEntity> = repository.getSubtasks(taskId)
    fun observeSubtasks(taskId: String) = repository.observeSubtasks(taskId)
    fun saveSubtask(subtask: SubTaskEntity) = launchSafe { repository.saveSubtask(subtask) }
    fun toggleSubtask(id: String, completed: Boolean) = launchSafe { repository.toggleSubtask(id, completed) }
    fun deleteSubtask(id: String) = launchSafe { repository.deleteSubtask(id) }
    fun saveSubtasksForTask(taskId: String, subtasks: List<SubTaskEntity>) = launchSafe { repository.saveSubtasksForTask(taskId, subtasks) }

    fun observeCourseModules(courseId: String) = repository.observeCourseModules(courseId)
    suspend fun getCourseModules(courseId: String): List<CourseModuleEntity> = repository.getCourseModules(courseId)
    fun saveCourseModule(module: CourseModuleEntity) = launchSafe { repository.saveCourseModule(module) }
    fun saveCourseModulesForCourse(courseId: String, modules: List<CourseModuleEntity>) = launchSafe { repository.saveCourseModulesForCourse(courseId, modules) }
    fun toggleCourseModule(id: String, completed: Boolean) = launchSafe { repository.toggleCourseModule(id, completed) }
    fun deleteCourseModule(id: String) = launchSafe { repository.deleteCourseModule(id) }

    fun observeMilestonesForGoal(goalId: String) = repository.observeMilestonesForGoal(goalId)
    fun observeMilestonesForProject(projectId: String) = repository.observeMilestonesForProject(projectId)
    suspend fun getMilestonesForProject(projectId: String): List<MilestoneEntity> = repository.getMilestonesForProject(projectId)
    suspend fun getMilestonesForGoal(goalId: String): List<MilestoneEntity> = repository.getMilestonesForGoal(goalId)
    fun saveMilestone(milestone: MilestoneEntity) = launchSafe { repository.saveMilestone(milestone) }
    fun saveMilestonesForProject(projectId: String, milestones: List<MilestoneEntity>) = launchSafe { repository.saveMilestonesForProject(projectId, milestones) }
    fun saveMilestonesForGoal(goalId: String, milestones: List<MilestoneEntity>) = launchSafe { repository.saveMilestonesForGoal(goalId, milestones) }
    fun toggleMilestone(id: String, completed: Boolean) = launchSafe { repository.toggleMilestone(id, completed) }
    fun deleteMilestone(id: String) = launchSafe { repository.deleteMilestone(id) }

    fun savePreferences(value: UserPreferencesEntity) = launchSafe { repository.savePreference(value) }

    fun setTheme(themeKey: String) = launchSafe {
        repository.savePreference(preferences.value.copy(themeKey = themeKey))
    }

    fun setAccent(accentKey: String) = launchSafe {
        repository.savePreference(preferences.value.copy(accentKey = accentKey))
    }

    fun setUse24HourClock(enabled: Boolean) = launchSafe {
        repository.savePreference(preferences.value.copy(use24HourClock = enabled))
    }

    fun setLoudReminders(enabled: Boolean) = launchSafe {
        repository.savePreference(preferences.value.copy(loudReminders = enabled))
        val soundId = preferences.value.notificationSoundId
        val sound = repository.getSound(soundId)
        val imported = sound?.takeIf { !it.isBuiltIn }?.contentUri?.let { Uri.parse(it) }
        NotificationChannels.ensureChannel(app, soundId, imported, loud = enabled)
        scheduler.rescheduleAll()
    }

    fun updateDashboard(item: DashboardConfigurationEntity) = launchSafe {
        repository.saveDashboardItem(item)
    }

    fun moveDashboardWidget(widgetKey: String, delta: Int) = launchSafe {
        val current = dashboard.value.ifEmpty { com.daymark.app.data.DashboardWidgets.defaults }
            .sortedBy { it.position }
            .toMutableList()
        val index = current.indexOfFirst { it.widgetKey == widgetKey }
        val target = (index + delta).coerceIn(0, current.lastIndex)
        if (index >= 0 && target in current.indices && target != index) {
            val moving = current[index]
            val other = current[target]
            current[index] = other.copy(position = index)
            current[target] = moving.copy(position = target)
            current.forEach { repository.saveDashboardItem(it) }
        }
    }

    fun selectSound(soundId: String) = launchSafe {
        val selected = repository.getSound(soundId)
        val imported = selected?.takeIf { !it.isBuiltIn }?.contentUri?.let { Uri.parse(it) }
        NotificationChannels.ensureChannel(app, soundId, imported)
        repository.savePreference(preferences.value.copy(notificationSoundId = soundId))
        emitMessage("Reminder sound updated. Android keeps channel sound settings separately.")
    }

    fun importSound(uri: Uri) = launchSafe {
        val sound = NotificationSoundImporter.copyIntoDeviceMedia(app, uri)
        repository.saveSound(sound)
        NotificationChannels.ensureChannel(app, sound.soundId, Uri.parse(sound.contentUri))
        repository.savePreference(preferences.value.copy(notificationSoundId = sound.soundId))
        emitMessage("${sound.name} is ready for Daymark reminders.")
    }

    fun sendTestNotification() = launchSafe {
        val soundId = preferences.value.notificationSoundId
        val imported = repository.getSound(soundId)?.takeIf { !it.isBuiltIn }?.contentUri?.let { Uri.parse(it) }
        val sent = NotificationChannels.postTest(app, soundId, imported)
        emitMessage(if (sent) "Test notification sent." else "Allow Daymark notifications to test this sound.")
    }

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        repository.search(query)
    }

    suspend fun reminderOffsets(ownerType: String, ownerId: String): Set<Int> = withContext(Dispatchers.IO) {
        repository.reminderOffsets(ownerType, ownerId)
    }

    suspend fun recurrenceRule(id: String): com.daymark.app.data.RecurrenceRuleEntity? = withContext(Dispatchers.IO) {
        repository.recurrenceRule(id)
    }

    fun exportBackup(uri: Uri) = launchSafe {
        BackupManager(repository.database(), app.contentResolver).export(uri)
        emitMessage("Backup exported successfully.")
    }

    fun restoreBackup(uri: Uri) = launchSafe {
        BackupManager(repository.database(), app.contentResolver).restore(uri)
        scheduler.rescheduleAll()
        emitMessage("Backup restored. Your local data is ready.")
    }

    fun resetApplicationData() = launchSafe {
        withContext(Dispatchers.IO) { repository.resetAllData() }
        scheduler.rescheduleAll()
        emitMessage("Daymark data was reset.")
    }

    fun refreshReminders() = launchSafe { scheduler.rescheduleAll() }

    private fun launchSafe(block: suspend () -> Unit) {
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            try {
                block()
            } catch (error: Exception) {
                emitMessage(error.message?.takeIf { it.isNotBlank() } ?: "Daymark could not complete that action.")
            }
        }
    }

    class Factory(
        private val app: Application,
        private val repository: DaymarkRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (!modelClass.isAssignableFrom(DaymarkViewModel::class.java)) {
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
            return DaymarkViewModel(app, repository) as T
        }
    }
}
