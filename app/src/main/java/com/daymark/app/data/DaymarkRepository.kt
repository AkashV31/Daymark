package com.daymark.app.data

import androidx.room.withTransaction
import com.daymark.app.domain.RecurrenceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class SearchResult(
    val id: String,
    val type: String,
    val title: String,
    val subtitle: String = ""
)

class DaymarkRepository(private val database: DaymarkDatabase) {
    val tasks: Flow<List<TaskEntity>> = database.taskDao().observeActive()
    val events: Flow<List<EventEntity>> = database.eventDao().observeActive()
    val deadlines: Flow<List<DeadlineEntity>> = database.deadlineDao().observeActive()
    val notes: Flow<List<NoteEntity>> = database.noteDao().observeActive()
    val courses: Flow<List<CourseEntity>> = database.courseDao().observeAll()
    val projects: Flow<List<ProjectEntity>> = database.projectDao().observeActive()
    val goals: Flow<List<GoalEntity>> = database.goalDao().observeActive()
    val completions: Flow<List<TaskCompletionEntity>> = database.taskCompletionDao().observeAll()
    val goalActivities: Flow<List<GoalActivityEntity>> = database.goalActivityDao().observeAll()
    val preferences: Flow<UserPreferencesEntity> = database.preferenceDao().observe()
        .map { it ?: UserPreferencesEntity() }
    val dashboard: Flow<List<DashboardConfigurationEntity>> = database.dashboardDao().observeAll()
    val sounds: Flow<List<NotificationSoundEntity>> = database.soundDao().observeAll()

    suspend fun initializeDefaults() {
        database.withTransaction {
            if (database.preferenceDao().get() == null) {
                database.preferenceDao().upsert(UserPreferencesEntity())
            }
            if (database.dashboardDao().getAll().isEmpty()) {
                database.dashboardDao().upsertAll(DashboardWidgets.defaults)
            }
        }
    }

    suspend fun saveTask(
        candidate: TaskEntity,
        recurrenceFrequency: String = RecurrenceEngine.NONE,
        recurrenceInterval: Int = 1,
        weekdaysMask: Int = 0,
        recurrenceEndEpochDay: Long? = null,
        recurrenceOccurrenceLimit: Int? = null,
        reminderOffsets: Set<Int> = emptySet(),
        editScope: String = "THIS_OCCURRENCE"
    ) {
        require(candidate.title.isNotBlank()) { "A task needs a title." }
        val normalizedTitle = candidate.title.trim()
        val now = System.currentTimeMillis()
        val existing = database.taskDao().get(candidate.id)
        val frequency = recurrenceFrequency.uppercase()
        require(frequency == RecurrenceEngine.NONE || candidate.dueEpochDay != null) {
            "Choose a due date before adding a recurring task."
        }
        val ruleId = if (frequency == RecurrenceEngine.NONE) null
        else candidate.recurrenceRuleId ?: UUID.randomUUID().toString()
        val rule = ruleId?.let {
            RecurrenceRuleEntity(
                id = it,
                frequency = frequency,
                interval = recurrenceInterval.coerceAtLeast(1),
                weekdaysMask = weekdaysMask,
                endEpochDay = recurrenceEndEpochDay,
                occurrenceLimit = recurrenceOccurrenceLimit,
                createdAtMillis = now
            )
        }
        val task = candidate.copy(
            title = normalizedTitle,
            id = existing?.id ?: candidate.id,
            createdAtMillis = existing?.createdAtMillis ?: candidate.createdAtMillis,
            updatedAtMillis = now,
            recurrenceRuleId = ruleId,
            seriesId = if (ruleId == null) null else (existing?.seriesId ?: candidate.seriesId ?: candidate.id),
            occurrenceIndex = existing?.occurrenceIndex ?: candidate.occurrenceIndex
        )

        database.withTransaction {
            rule?.let { database.recurrenceRuleDao().upsert(it) }
            database.taskDao().upsert(task)
            database.reminderDao().deleteForOwner(OWNER_TASK, task.id)
            reminderOffsets.distinct().filter { it >= 0 }.sortedDescending().forEach { offset ->
                database.reminderDao().upsert(
                    ReminderEntity(
                        id = reminderId(OWNER_TASK, task.id, offset),
                        ownerType = OWNER_TASK,
                        ownerId = task.id,
                        offsetMinutes = offset,
                        enabled = true,
                        zoneId = ZoneId.systemDefault().id,
                        createdAtMillis = now
                    )
                )
            }

            if (existing?.seriesId != null && editScope != "THIS_OCCURRENCE") {
                val seriesId = existing.seriesId
                val shiftDays = if (existing.dueEpochDay != null && task.dueEpochDay != null) {
                    task.dueEpochDay - existing.dueEpochDay
                } else 0L
                database.taskDao().getSeries(seriesId).forEach { occurrence ->
                    val isInScope = editScope == "ENTIRE_SERIES" || occurrence.occurrenceIndex >= existing.occurrenceIndex
                    if (!isInScope || occurrence.id == task.id) return@forEach
                    val shiftedDay = occurrence.dueEpochDay?.plus(shiftDays)
                    database.taskDao().upsert(
                        occurrence.copy(
                            title = task.title,
                            description = task.description,
                            dueEpochDay = shiftedDay,
                            dueMinuteOfDay = task.dueMinuteOfDay,
                            estimatedDurationMinutes = task.estimatedDurationMinutes,
                            priority = task.priority,
                            category = task.category,
                            courseId = task.courseId,
                            projectId = task.projectId,
                            goalId = task.goalId,
                            recurrenceRuleId = ruleId,
                            seriesId = if (ruleId == null) null else seriesId,
                            notes = task.notes,
                            updatedAtMillis = now
                        )
                    )
                    database.reminderDao().deleteForOwner(OWNER_TASK, occurrence.id)
                    reminderOffsets.distinct().filter { it >= 0 }.forEach { offset ->
                        database.reminderDao().upsert(
                            ReminderEntity(
                                id = reminderId(OWNER_TASK, occurrence.id, offset),
                                ownerType = OWNER_TASK,
                                ownerId = occurrence.id,
                                offsetMinutes = offset,
                                zoneId = ZoneId.systemDefault().id,
                                createdAtMillis = now
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun saveEvent(event: EventEntity, reminderOffsets: Set<Int> = emptySet()) {
        require(event.title.isNotBlank()) { "An event needs a title." }
        val now = System.currentTimeMillis()
        val existing = database.eventDao().get(event.id)
        val saved = event.copy(
            title = event.title.trim(),
            createdAtMillis = existing?.createdAtMillis ?: event.createdAtMillis,
            updatedAtMillis = now
        )
        database.withTransaction {
            database.eventDao().upsert(saved)
            database.reminderDao().deleteForOwner(OWNER_EVENT, saved.id)
            reminderOffsets.distinct().filter { it >= 0 }.sortedDescending().forEach { offset ->
                database.reminderDao().upsert(
                    ReminderEntity(
                        id = reminderId(OWNER_EVENT, saved.id, offset),
                        ownerType = OWNER_EVENT,
                        ownerId = saved.id,
                        offsetMinutes = offset,
                        zoneId = ZoneId.systemDefault().id,
                        createdAtMillis = now
                    )
                )
            }
        }
    }

    suspend fun saveDeadline(deadline: DeadlineEntity, reminderOffsets: Set<Int> = emptySet()) {
        require(deadline.title.isNotBlank()) { "A deadline needs a title." }
        val now = System.currentTimeMillis()
        val existing = database.deadlineDao().get(deadline.id)
        val saved = deadline.copy(
            title = deadline.title.trim(),
            createdAtMillis = existing?.createdAtMillis ?: deadline.createdAtMillis,
            updatedAtMillis = now
        )
        database.withTransaction {
            database.deadlineDao().upsert(saved)
            database.reminderDao().deleteForOwner(OWNER_DEADLINE, saved.id)
            reminderOffsets.distinct().filter { it >= 0 }.sortedDescending().forEach { offset ->
                database.reminderDao().upsert(
                    ReminderEntity(
                        id = reminderId(OWNER_DEADLINE, saved.id, offset),
                        ownerType = OWNER_DEADLINE,
                        ownerId = saved.id,
                        offsetMinutes = offset,
                        zoneId = ZoneId.systemDefault().id,
                        createdAtMillis = now
                    )
                )
            }
        }
    }

    suspend fun saveTaskWithSubtasks(
        candidate: TaskEntity,
        recurrenceFrequency: String = RecurrenceEngine.NONE,
        recurrenceInterval: Int = 1,
        recurrenceWeekdaysMask: Int = 0,
        recurrenceEndEpochDay: Long? = null,
        recurrenceOccurrenceLimit: Int? = null,
        reminderOffsets: Set<Int> = emptySet(),
        editScope: String = "THIS_OCCURRENCE",
        subtasks: List<SubTaskEntity> = emptyList()
    ) {
        database.withTransaction {
            saveTask(
                candidate = candidate,
                recurrenceFrequency = recurrenceFrequency,
                recurrenceInterval = recurrenceInterval,
                weekdaysMask = recurrenceWeekdaysMask,
                recurrenceEndEpochDay = recurrenceEndEpochDay,
                recurrenceOccurrenceLimit = recurrenceOccurrenceLimit,
                reminderOffsets = reminderOffsets,
                editScope = editScope
            )
            database.subTaskDao().deleteForTask(candidate.id)
            if (subtasks.isNotEmpty()) {
                database.subTaskDao().upsertAll(subtasks.map { it.copy(taskId = candidate.id) })
            }
        }
    }

    suspend fun saveProjectWithMilestones(project: ProjectEntity, milestones: List<MilestoneEntity>) {
        require(project.title.isNotBlank()) { "A project needs a title." }
        database.withTransaction {
            saveProject(project)
            database.milestoneDao().deleteForProject(project.id)
            if (milestones.isNotEmpty()) {
                database.milestoneDao().upsertAll(milestones.map { it.copy(projectId = project.id) })
            }
        }
    }

    suspend fun saveCourseWithModules(course: CourseEntity, modules: List<CourseModuleEntity>) {
        require(course.title.isNotBlank()) { "A course needs a title." }
        database.withTransaction {
            saveCourse(course)
            database.courseModuleDao().deleteForCourse(course.id)
            if (modules.isNotEmpty()) {
                database.courseModuleDao().upsertAll(modules.map { it.copy(courseId = course.id) })
            }
        }
    }

    suspend fun saveNote(note: NoteEntity) {
        require(note.title.isNotBlank() || note.content.isNotBlank()) { "Add a title or some note text." }
        val now = System.currentTimeMillis()
        val existing = database.noteDao().get(note.id)
        database.noteDao().upsert(
            note.copy(
                title = note.title.trim(),
                createdAtMillis = existing?.createdAtMillis ?: note.createdAtMillis,
                updatedAtMillis = now
            )
        )
    }

    suspend fun saveGoal(goal: GoalEntity) {
        require(goal.title.isNotBlank()) { "A goal needs a title." }
        val old = database.goalDao().get(goal.id)
        database.goalDao().upsert(
            goal.copy(
                title = goal.title.trim(),
                progress = goal.progress.coerceIn(0, 100),
                createdAtMillis = old?.createdAtMillis ?: goal.createdAtMillis,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveProject(project: ProjectEntity) {
        require(project.title.isNotBlank()) { "A project needs a title." }
        val old = database.projectDao().get(project.id)
        database.projectDao().upsert(
            project.copy(
                title = project.title.trim(),
                createdAtMillis = old?.createdAtMillis ?: project.createdAtMillis,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveCourse(course: CourseEntity) {
        require(course.title.isNotBlank()) { "A course needs a title." }
        val old = database.courseDao().get(course.id)
        database.courseDao().upsert(
            course.copy(
                title = course.title.trim(),
                progress = course.progress.coerceIn(0, 100),
                createdAtMillis = old?.createdAtMillis ?: course.createdAtMillis,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun savePreference(preferences: UserPreferencesEntity) {
        database.preferenceDao().upsert(preferences.copy(updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun saveDashboardItem(item: DashboardConfigurationEntity) {
        database.withTransaction {
            if (database.dashboardDao().getAll().isEmpty()) {
                database.dashboardDao().upsertAll(DashboardWidgets.defaults)
            }
            database.dashboardDao().upsert(item)
        }
    }

    suspend fun saveSound(sound: NotificationSoundEntity) {
        database.soundDao().upsert(sound)
    }

    suspend fun getSound(soundId: String): NotificationSoundEntity? = database.soundDao().get(soundId)

    suspend fun reminderOffsets(ownerType: String, ownerId: String): Set<Int> =
        database.reminderDao().forOwner(ownerType, ownerId).filter { it.enabled }.map { it.offsetMinutes }.toSet()

    fun observeRecurrenceRules(): Flow<List<RecurrenceRuleEntity>> = database.recurrenceRuleDao().observeAll()
    suspend fun recurrenceRule(id: String): RecurrenceRuleEntity? = database.recurrenceRuleDao().get(id)

    suspend fun completeTask(taskId: String, completedAtMillis: Long = System.currentTimeMillis()) {
        database.withTransaction {
            val task = database.taskDao().get(taskId) ?: return@withTransaction
            if (task.status == STATUS_COMPLETED || task.archived) return@withTransaction
            val today = LocalDate.now().toEpochDay()
            val occurrenceDay = task.dueEpochDay ?: today
            database.taskDao().upsert(
                task.copy(status = STATUS_COMPLETED, completedAtMillis = completedAtMillis, updatedAtMillis = completedAtMillis)
            )
            database.taskCompletionDao().upsert(
                TaskCompletionEntity(
                    taskId = task.id,
                    seriesId = task.seriesId,
                    occurrenceDateEpochDay = occurrenceDay,
                    completedAtMillis = completedAtMillis
                )
            )
            task.goalId?.let { goalId ->
                database.goalActivityDao().upsert(
                    GoalActivityEntity(
                        id = "$goalId:$occurrenceDay",
                        goalId = goalId,
                        taskId = task.id,
                        activityEpochDay = occurrenceDay,
                        createdAtMillis = completedAtMillis
                    )
                )
            }

            val ruleId = task.recurrenceRuleId
            val dueDay = task.dueEpochDay
            if (ruleId != null && dueDay != null) {
                val rule = database.recurrenceRuleDao().get(ruleId)
                val seriesId = task.seriesId ?: task.id
                val nextDate = rule?.let {
                    RecurrenceEngine.nextOccurrence(
                        current = LocalDate.ofEpochDay(dueDay),
                        rule = it,
                        seriesStart = database.taskDao().getSeries(seriesId)
                            .minOfOrNull { occurrence -> LocalDate.ofEpochDay(occurrence.dueEpochDay ?: dueDay) }
                            ?: LocalDate.ofEpochDay(dueDay),
                        currentOccurrenceIndex = task.occurrenceIndex
                    )
                }
                if (nextDate != null) {
                    val nextId = UUID.nameUUIDFromBytes("$seriesId:${nextDate.toEpochDay()}".toByteArray()).toString()
                    val next = task.copy(
                        id = nextId,
                        dueEpochDay = nextDate.toEpochDay(),
                        status = STATUS_PLANNED,
                        completedAtMillis = null,
                        createdAtMillis = completedAtMillis,
                        updatedAtMillis = completedAtMillis,
                        occurrenceIndex = task.occurrenceIndex + 1,
                        recurrenceGenerated = true
                    )
                    val inserted = database.taskDao().insertIfMissing(next)
                    if (inserted != -1L) {
                        database.reminderDao().forOwner(OWNER_TASK, task.id).forEach { reminder ->
                            database.reminderDao().upsert(
                                reminder.copy(
                                    id = reminderId(OWNER_TASK, nextId, reminder.offsetMinutes),
                                    ownerId = nextId,
                                    createdAtMillis = completedAtMillis
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun uncompleteTask(taskId: String) {
        database.withTransaction {
            val task = database.taskDao().get(taskId) ?: return@withTransaction
            if (task.status != STATUS_COMPLETED || task.archived) return@withTransaction
            database.taskDao().upsert(task.copy(status = STATUS_PLANNED, completedAtMillis = null, updatedAtMillis = System.currentTimeMillis()))
            database.taskCompletionDao().deleteForTask(taskId)
            val seriesId = task.seriesId
            if (seriesId != null) {
                val next = database.taskDao().getSeries(seriesId).firstOrNull {
                    it.occurrenceIndex == task.occurrenceIndex + 1 &&
                        it.recurrenceGenerated &&
                        it.status == STATUS_PLANNED &&
                        it.updatedAtMillis == it.createdAtMillis
                }
                if (next != null) {
                    database.taskDao().delete(next.id)
                    database.reminderDao().deleteForOwner(OWNER_TASK, next.id)
                }
            }
        }
    }

    suspend fun rollForwardRecurringTasks(nowEpochDay: Long = LocalDate.now().toEpochDay()) {
        database.withTransaction {
            val tasks = database.taskDao().getAll()
                .filter { !it.archived && it.status == STATUS_PLANNED && it.recurrenceRuleId != null && it.dueEpochDay != null }
            for (task in tasks) {
                val dueDay = task.dueEpochDay ?: continue
                if (dueDay < nowEpochDay) {
                    val ruleId = task.recurrenceRuleId ?: continue
                    val rule = database.recurrenceRuleDao().get(ruleId) ?: continue
                    val seriesId = task.seriesId ?: task.id
                    val seriesStart = database.taskDao().getSeries(seriesId)
                        .minOfOrNull { occurrence -> LocalDate.ofEpochDay(occurrence.dueEpochDay ?: dueDay) }
                        ?: LocalDate.ofEpochDay(dueDay)

                    var current = LocalDate.ofEpochDay(dueDay)
                    var index = task.occurrenceIndex
                    var nextDate = RecurrenceEngine.nextOccurrence(current, rule, seriesStart, index)
                    while (nextDate != null && nextDate.toEpochDay() < nowEpochDay) {
                        current = nextDate
                        index++
                        nextDate = RecurrenceEngine.nextOccurrence(current, rule, seriesStart, index)
                    }
                    if (nextDate != null) {
                        val nextId = UUID.nameUUIDFromBytes("$seriesId:${nextDate.toEpochDay()}".toByteArray()).toString()
                        val now = System.currentTimeMillis()
                        val next = task.copy(
                            id = nextId,
                            dueEpochDay = nextDate.toEpochDay(),
                            status = STATUS_PLANNED,
                            completedAtMillis = null,
                            createdAtMillis = now,
                            updatedAtMillis = now,
                            occurrenceIndex = index + 1,
                            recurrenceGenerated = true
                        )
                        val inserted = database.taskDao().insertIfMissing(next)
                        if (inserted != -1L) {
                            database.reminderDao().forOwner(OWNER_TASK, task.id).forEach { reminder ->
                                database.reminderDao().upsert(
                                    reminder.copy(
                                        id = reminderId(OWNER_TASK, nextId, reminder.offsetMinutes),
                                        ownerType = OWNER_TASK,
                                        ownerId = nextId,
                                        zoneId = ZoneId.systemDefault().id,
                                        createdAtMillis = now
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun completeDeadline(deadlineId: String) {
        val deadline = database.deadlineDao().get(deadlineId) ?: return
        val now = System.currentTimeMillis()
        database.deadlineDao().upsert(deadline.copy(status = "COMPLETED", completedAtMillis = now, updatedAtMillis = now))
    }

    suspend fun archiveTask(taskId: String) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.taskDao().archive(taskId, now)
            database.reminderDao().deleteForOwner(OWNER_TASK, taskId)
        }
    }

    suspend fun deleteTask(taskId: String): TaskSnapshot? = database.withTransaction {
        val task = database.taskDao().get(taskId) ?: return@withTransaction null
        val subtasks = database.subTaskDao().getForTask(taskId)
        val reminders = database.reminderDao().forOwner(OWNER_TASK, taskId).filter { it.enabled }.map { it.offsetMinutes }.toSet()
        val completions = database.taskCompletionDao().getAll().filter { it.taskId == taskId }
        database.taskDao().delete(taskId)
        database.reminderDao().deleteForOwner(OWNER_TASK, taskId)
        TaskSnapshot(task, subtasks, reminders, completions)
    }

    suspend fun deleteRecurringTask(taskId: String, scope: RecurringDeleteScope): TaskSnapshot? = database.withTransaction {
        val task = database.taskDao().get(taskId) ?: return@withTransaction null
        val subtasks = database.subTaskDao().getForTask(taskId)
        val reminders = database.reminderDao().forOwner(OWNER_TASK, taskId).filter { it.enabled }.map { it.offsetMinutes }.toSet()
        val completions = database.taskCompletionDao().getAll().filter { it.taskId == taskId }
        val seriesId = task.seriesId ?: task.id

        when (scope) {
            RecurringDeleteScope.THIS_OCCURRENCE -> {
                database.taskDao().delete(taskId)
                database.reminderDao().deleteForOwner(OWNER_TASK, taskId)
            }
            RecurringDeleteScope.THIS_AND_FUTURE -> {
                val fromDay = task.dueEpochDay ?: LocalDate.now().toEpochDay()
                val series = database.taskDao().getSeries(seriesId).filter { (it.dueEpochDay ?: 0L) >= fromDay }
                series.forEach { occ ->
                    database.reminderDao().deleteForOwner(OWNER_TASK, occ.id)
                }
                database.taskDao().deleteSeriesFrom(seriesId, fromDay)
            }
            RecurringDeleteScope.ENTIRE_SERIES -> {
                val series = database.taskDao().getSeries(seriesId)
                series.forEach { occ ->
                    database.reminderDao().deleteForOwner(OWNER_TASK, occ.id)
                }
                database.taskDao().deleteSeries(seriesId)
            }
        }
        TaskSnapshot(task, subtasks, reminders, completions)
    }

    suspend fun restoreTask(snapshot: TaskSnapshot) = database.withTransaction {
        database.taskDao().upsert(snapshot.task)
        if (snapshot.subtasks.isNotEmpty()) {
            database.subTaskDao().upsertAll(snapshot.subtasks)
        }
        if (snapshot.completions.isNotEmpty()) {
            database.taskCompletionDao().upsertAll(snapshot.completions)
        }
        snapshot.reminderOffsets.forEach { offset ->
            database.reminderDao().upsert(
                ReminderEntity(
                    id = reminderId(OWNER_TASK, snapshot.task.id, offset),
                    ownerType = OWNER_TASK,
                    ownerId = snapshot.task.id,
                    offsetMinutes = offset,
                    zoneId = ZoneId.systemDefault().id
                )
            )
        }
    }

    suspend fun deleteEvent(eventId: String): EventSnapshot? = database.withTransaction {
        val event = database.eventDao().get(eventId) ?: return@withTransaction null
        val reminders = database.reminderDao().forOwner(OWNER_EVENT, eventId).filter { it.enabled }.map { it.offsetMinutes }.toSet()
        database.eventDao().delete(eventId)
        database.reminderDao().deleteForOwner(OWNER_EVENT, eventId)
        EventSnapshot(event, reminders)
    }

    suspend fun restoreEvent(snapshot: EventSnapshot) = database.withTransaction {
        database.eventDao().upsert(snapshot.event)
        snapshot.reminderOffsets.forEach { offset ->
            database.reminderDao().upsert(
                ReminderEntity(
                    id = reminderId(OWNER_EVENT, snapshot.event.id, offset),
                    ownerType = OWNER_EVENT,
                    ownerId = snapshot.event.id,
                    offsetMinutes = offset,
                    zoneId = ZoneId.systemDefault().id
                )
            )
        }
    }

    suspend fun deleteDeadline(deadlineId: String): DeadlineSnapshot? = database.withTransaction {
        val deadline = database.deadlineDao().get(deadlineId) ?: return@withTransaction null
        val reminders = database.reminderDao().forOwner(OWNER_DEADLINE, deadlineId).filter { it.enabled }.map { it.offsetMinutes }.toSet()
        database.deadlineDao().delete(deadlineId)
        database.reminderDao().deleteForOwner(OWNER_DEADLINE, deadlineId)
        DeadlineSnapshot(deadline, reminders)
    }

    suspend fun restoreDeadline(snapshot: DeadlineSnapshot) = database.withTransaction {
        database.deadlineDao().upsert(snapshot.deadline)
        snapshot.reminderOffsets.forEach { offset ->
            database.reminderDao().upsert(
                ReminderEntity(
                    id = reminderId(OWNER_DEADLINE, snapshot.deadline.id, offset),
                    ownerType = OWNER_DEADLINE,
                    ownerId = snapshot.deadline.id,
                    offsetMinutes = offset,
                    zoneId = ZoneId.systemDefault().id
                )
            )
        }
    }

    suspend fun deleteNote(noteId: String): NoteSnapshot? = database.withTransaction {
        val note = database.noteDao().get(noteId) ?: return@withTransaction null
        database.noteDao().delete(noteId)
        NoteSnapshot(note)
    }

    suspend fun restoreNote(snapshot: NoteSnapshot) = database.withTransaction {
        database.noteDao().upsert(snapshot.note)
    }

    suspend fun deleteGoal(goalId: String): GoalSnapshot? = database.withTransaction {
        val goal = database.goalDao().get(goalId) ?: return@withTransaction null
        val milestones = database.milestoneDao().getForGoal(goalId)
        val activities = database.goalActivityDao().getAll().filter { it.goalId == goalId }
        database.goalDao().delete(goalId)
        GoalSnapshot(goal, milestones, activities)
    }

    suspend fun restoreGoal(snapshot: GoalSnapshot) = database.withTransaction {
        database.goalDao().upsert(snapshot.goal)
        if (snapshot.milestones.isNotEmpty()) {
            database.milestoneDao().upsertAll(snapshot.milestones)
        }
        if (snapshot.activities.isNotEmpty()) {
            database.goalActivityDao().upsertAll(snapshot.activities)
        }
    }

    suspend fun deleteProject(projectId: String): ProjectSnapshot? = database.withTransaction {
        val project = database.projectDao().get(projectId) ?: return@withTransaction null
        val milestones = database.milestoneDao().getForProject(projectId)
        database.projectDao().delete(projectId)
        ProjectSnapshot(project, milestones)
    }

    suspend fun restoreProject(snapshot: ProjectSnapshot) = database.withTransaction {
        database.projectDao().upsert(snapshot.project)
        if (snapshot.milestones.isNotEmpty()) {
            database.milestoneDao().upsertAll(snapshot.milestones)
        }
    }

    suspend fun deleteCourse(courseId: String): CourseSnapshot? = database.withTransaction {
        val course = database.courseDao().get(courseId) ?: return@withTransaction null
        val modules = database.courseModuleDao().getForCourse(courseId)
        database.courseDao().delete(courseId)
        CourseSnapshot(course, modules)
    }

    suspend fun restoreCourse(snapshot: CourseSnapshot) = database.withTransaction {
        database.courseDao().upsert(snapshot.course)
        if (snapshot.modules.isNotEmpty()) {
            database.courseModuleDao().upsertAll(snapshot.modules)
        }
    }

    fun observeSubtasks(taskId: String): Flow<List<SubTaskEntity>> = database.subTaskDao().observeForTask(taskId)
    suspend fun getSubtasks(taskId: String): List<SubTaskEntity> = database.subTaskDao().getForTask(taskId)
    suspend fun saveSubtask(subtask: SubTaskEntity) = database.subTaskDao().upsert(subtask)
    suspend fun toggleSubtask(id: String, completed: Boolean) {
        val item = database.subTaskDao().get(id) ?: return
        database.subTaskDao().upsert(
            item.copy(
                isCompleted = completed,
                completedAtMillis = if (completed) System.currentTimeMillis() else null
            )
        )
    }
    suspend fun deleteSubtask(id: String) = database.subTaskDao().delete(id)
    suspend fun saveSubtasksForTask(taskId: String, subtasks: List<SubTaskEntity>) {
        database.withTransaction {
            database.subTaskDao().deleteForTask(taskId)
            database.subTaskDao().upsertAll(subtasks)
        }
    }

    fun observeCourseModules(courseId: String): Flow<List<CourseModuleEntity>> = database.courseModuleDao().observeForCourse(courseId)
    suspend fun getCourseModules(courseId: String): List<CourseModuleEntity> = database.courseModuleDao().getForCourse(courseId)
    suspend fun saveCourseModule(module: CourseModuleEntity) = database.courseModuleDao().upsert(module)
    suspend fun saveCourseModulesForCourse(courseId: String, modules: List<CourseModuleEntity>) {
        database.withTransaction {
            database.courseModuleDao().deleteForCourse(courseId)
            database.courseModuleDao().upsertAll(modules)
        }
    }
    suspend fun toggleCourseModule(id: String, completed: Boolean) {
        val item = database.courseModuleDao().get(id) ?: return
        database.courseModuleDao().upsert(
            item.copy(
                isCompleted = completed,
                completedAtMillis = if (completed) System.currentTimeMillis() else null
            )
        )
    }
    suspend fun deleteCourseModule(id: String) = database.courseModuleDao().delete(id)

    fun observeMilestonesForGoal(goalId: String): Flow<List<MilestoneEntity>> = database.milestoneDao().observeFor(goalId = goalId, projectId = null)
    fun observeMilestonesForProject(projectId: String): Flow<List<MilestoneEntity>> = database.milestoneDao().observeFor(goalId = null, projectId = projectId)
    suspend fun getMilestonesForProject(projectId: String): List<MilestoneEntity> = database.milestoneDao().getForProject(projectId)
    suspend fun getMilestonesForGoal(goalId: String): List<MilestoneEntity> = database.milestoneDao().getForGoal(goalId)
    suspend fun saveMilestone(milestone: MilestoneEntity) = database.milestoneDao().upsert(milestone)
    suspend fun saveMilestonesForProject(projectId: String, milestones: List<MilestoneEntity>) {
        database.withTransaction {
            database.milestoneDao().deleteForProject(projectId)
            database.milestoneDao().upsertAll(milestones)
        }
    }
    suspend fun saveMilestonesForGoal(goalId: String, milestones: List<MilestoneEntity>) {
        database.withTransaction {
            database.milestoneDao().deleteForGoal(goalId)
            database.milestoneDao().upsertAll(milestones)
        }
    }
    suspend fun toggleMilestone(id: String, completed: Boolean) {
        val item = database.milestoneDao().get(id) ?: return
        database.milestoneDao().upsert(
            item.copy(
                isCompleted = completed,
                completedAtMillis = if (completed) System.currentTimeMillis() else null
            )
        )
    }
    suspend fun deleteMilestone(id: String) = database.milestoneDao().delete(id)

    suspend fun search(query: String): List<SearchResult> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        val tasks = database.taskDao().search(q).map { SearchResult(it.id, "TASK", it.title, it.category.ifBlank { "Task" }) }
        val deadlines = database.deadlineDao().search(q).map { SearchResult(it.id, "DEADLINE", it.title, "Deadline") }
        val events = database.eventDao().search(q).map { SearchResult(it.id, "EVENT", it.title, "Schedule") }
        val notes = database.noteDao().search(q).map { SearchResult(it.id, "NOTE", it.title.ifBlank { it.content.take(45) }, "Note") }
        val courses = database.courseDao().search(q).map { SearchResult(it.id, "COURSE", it.title, it.platform.ifBlank { "Learning" }) }
        val projects = database.projectDao().search(q).map { SearchResult(it.id, "PROJECT", it.title, "Project") }
        val goals = database.goalDao().search(q).map { SearchResult(it.id, "GOAL", it.title, "Goal") }
        return tasks + deadlines + events + notes + courses + projects + goals
    }

    suspend fun resetAllData() {
        database.clearAllTables()
        initializeDefaults()
    }

    fun database(): DaymarkDatabase = database

    companion object {
        const val OWNER_TASK = "TASK"
        const val OWNER_EVENT = "EVENT"
        const val OWNER_DEADLINE = "DEADLINE"
        const val STATUS_PLANNED = "PLANNED"
        const val STATUS_COMPLETED = "COMPLETED"

        fun reminderId(ownerType: String, ownerId: String, offsetMinutes: Int): String =
            UUID.nameUUIDFromBytes("$ownerType:$ownerId:$offsetMinutes".toByteArray()).toString()
    }
}

enum class RecurringDeleteScope {
    THIS_OCCURRENCE,
    THIS_AND_FUTURE,
    ENTIRE_SERIES
}

data class TaskSnapshot(
    val task: TaskEntity,
    val subtasks: List<SubTaskEntity> = emptyList(),
    val reminderOffsets: Set<Int> = emptySet(),
    val completions: List<TaskCompletionEntity> = emptyList()
)

data class EventSnapshot(
    val event: EventEntity,
    val reminderOffsets: Set<Int> = emptySet()
)

data class DeadlineSnapshot(
    val deadline: DeadlineEntity,
    val reminderOffsets: Set<Int> = emptySet()
)

data class NoteSnapshot(
    val note: NoteEntity
)

data class GoalSnapshot(
    val goal: GoalEntity,
    val milestones: List<MilestoneEntity> = emptyList(),
    val activities: List<GoalActivityEntity> = emptyList()
)

data class ProjectSnapshot(
    val project: ProjectEntity,
    val milestones: List<MilestoneEntity> = emptyList()
)

data class CourseSnapshot(
    val course: CourseEntity,
    val modules: List<CourseModuleEntity> = emptyList()
)
