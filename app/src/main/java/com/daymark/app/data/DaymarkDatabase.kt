package com.daymark.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE archived = 0 ORDER BY CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END, dueEpochDay IS NULL, dueEpochDay, dueMinuteOfDay, createdAtMillis DESC")
    fun observeActive(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE archived = 0 ORDER BY createdAtMillis DESC")
    fun observeActiveByRecent(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdAtMillis")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun get(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE seriesId = :seriesId AND archived = 0 ORDER BY occurrenceIndex, dueEpochDay")
    suspend fun getSeries(seriesId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE archived = 0 AND (lower(title) LIKE '%' || lower(:query) || '%' OR lower(description) LIKE '%' || lower(:query) || '%' OR lower(category) LIKE '%' || lower(:query) || '%') ORDER BY dueEpochDay IS NULL, dueEpochDay LIMIT 100")
    suspend fun search(query: String): List<TaskEntity>

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(task: TaskEntity): Long

    @Query("UPDATE tasks SET archived = 1, updatedAtMillis = :now WHERE id = :id")
    suspend fun archive(id: String, now: Long)
}

@Dao
interface SubTaskDao {
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder")
    fun observeForTask(taskId: String): Flow<List<SubTaskEntity>>

    @Query("SELECT * FROM subtasks")
    suspend fun getAll(): List<SubTaskEntity>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder")
    suspend fun getForTask(taskId: String): List<SubTaskEntity>

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: String)

    @Upsert
    suspend fun upsert(item: SubTaskEntity)

    @Upsert
    suspend fun upsertAll(items: List<SubTaskEntity>)
}

@Dao
interface RecurrenceRuleDao {
    @Query("SELECT * FROM recurrence_rules WHERE id = :id LIMIT 1")
    suspend fun get(id: String): RecurrenceRuleEntity?

    @Query("SELECT * FROM recurrence_rules")
    fun observeAll(): Flow<List<RecurrenceRuleEntity>>

    @Query("SELECT * FROM recurrence_rules")
    suspend fun getAll(): List<RecurrenceRuleEntity>

    @Upsert
    suspend fun upsert(rule: RecurrenceRuleEntity)

    @Upsert
    suspend fun upsertAll(rules: List<RecurrenceRuleEntity>)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE archived = 0 ORDER BY startEpochDay, startMinuteOfDay")
    fun observeActive(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE archived = 0 ORDER BY startEpochDay, startMinuteOfDay")
    suspend fun getActive(): List<EventEntity>

    @Query("SELECT * FROM events")
    suspend fun getAll(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun get(id: String): EventEntity?

    @Query("SELECT * FROM events WHERE archived = 0 AND (lower(title) LIKE '%' || lower(:query) || '%' OR lower(description) LIKE '%' || lower(:query) || '%' OR lower(category) LIKE '%' || lower(:query) || '%') ORDER BY startEpochDay LIMIT 100")
    suspend fun search(query: String): List<EventEntity>

    @Upsert
    suspend fun upsert(item: EventEntity)

    @Upsert
    suspend fun upsertAll(items: List<EventEntity>)
}

@Dao
interface DeadlineDao {
    @Query("SELECT * FROM deadlines WHERE archived = 0 ORDER BY dueEpochDay, dueMinuteOfDay")
    fun observeActive(): Flow<List<DeadlineEntity>>

    @Query("SELECT * FROM deadlines WHERE archived = 0 ORDER BY dueEpochDay, dueMinuteOfDay")
    suspend fun getActive(): List<DeadlineEntity>

    @Query("SELECT * FROM deadlines")
    suspend fun getAll(): List<DeadlineEntity>

    @Query("SELECT * FROM deadlines WHERE id = :id LIMIT 1")
    suspend fun get(id: String): DeadlineEntity?

    @Query("SELECT * FROM deadlines WHERE archived = 0 AND status != 'COMPLETED' AND (lower(title) LIKE '%' || lower(:query) || '%' OR lower(description) LIKE '%' || lower(:query) || '%' OR lower(category) LIKE '%' || lower(:query) || '%') ORDER BY dueEpochDay LIMIT 100")
    suspend fun search(query: String): List<DeadlineEntity>

    @Upsert
    suspend fun upsert(item: DeadlineEntity)

    @Upsert
    suspend fun upsertAll(items: List<DeadlineEntity>)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY offsetMinutes DESC")
    suspend fun forOwner(ownerType: String, ownerId: String): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun get(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE enabled = 1")
    suspend fun getEnabled(): List<ReminderEntity>

    @Query("SELECT * FROM reminders")
    suspend fun getAll(): List<ReminderEntity>

    @Query("DELETE FROM reminders WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun deleteForOwner(ownerType: String, ownerId: String)

    @Upsert
    suspend fun upsert(item: ReminderEntity)

    @Upsert
    suspend fun upsertAll(items: List<ReminderEntity>)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE archived = 0 ORDER BY isPinned DESC, updatedAtMillis DESC")
    fun observeActive(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    suspend fun getAll(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun get(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE archived = 0 AND (lower(title) LIKE '%' || lower(:query) || '%' OR lower(content) LIKE '%' || lower(:query) || '%') ORDER BY isPinned DESC, updatedAtMillis DESC LIMIT 100")
    suspend fun search(query: String): List<NoteEntity>

    @Upsert
    suspend fun upsert(item: NoteEntity)

    @Upsert
    suspend fun upsertAll(items: List<NoteEntity>)
}

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY updatedAtMillis DESC")
    fun observeAll(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses")
    suspend fun getAll(): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
    suspend fun get(id: String): CourseEntity?

    @Query("SELECT * FROM courses WHERE lower(title) LIKE '%' || lower(:query) || '%' OR lower(platform) LIKE '%' || lower(:query) || '%' OR lower(category) LIKE '%' || lower(:query) || '%' ORDER BY updatedAtMillis DESC LIMIT 100")
    suspend fun search(query: String): List<CourseEntity>

    @Upsert
    suspend fun upsert(item: CourseEntity)

    @Upsert
    suspend fun upsertAll(items: List<CourseEntity>)
}

@Dao
interface CourseModuleDao {
    @Query("SELECT * FROM course_modules WHERE courseId = :courseId ORDER BY sortOrder")
    fun observeForCourse(courseId: String): Flow<List<CourseModuleEntity>>

    @Query("SELECT * FROM course_modules WHERE courseId = :courseId ORDER BY sortOrder")
    suspend fun getForCourse(courseId: String): List<CourseModuleEntity>

    @Query("SELECT * FROM course_modules")
    suspend fun getAll(): List<CourseModuleEntity>

    @Query("DELETE FROM course_modules WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM course_modules WHERE courseId = :courseId")
    suspend fun deleteForCourse(courseId: String)

    @Upsert
    suspend fun upsert(item: CourseModuleEntity)

    @Upsert
    suspend fun upsertAll(items: List<CourseModuleEntity>)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE status != 'ARCHIVED' ORDER BY targetEpochDay IS NULL, targetEpochDay, updatedAtMillis DESC")
    fun observeActive(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects")
    suspend fun getAll(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE lower(title) LIKE '%' || lower(:query) || '%' OR lower(description) LIKE '%' || lower(:query) || '%' ORDER BY updatedAtMillis DESC LIMIT 100")
    suspend fun search(query: String): List<ProjectEntity>

    @Upsert
    suspend fun upsert(item: ProjectEntity)

    @Upsert
    suspend fun upsertAll(items: List<ProjectEntity>)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE status != 'ARCHIVED' ORDER BY targetEpochDay IS NULL, targetEpochDay, updatedAtMillis DESC")
    fun observeActive(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals")
    suspend fun getAll(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE lower(title) LIKE '%' || lower(:query) || '%' OR lower(description) LIKE '%' || lower(:query) || '%' ORDER BY updatedAtMillis DESC LIMIT 100")
    suspend fun search(query: String): List<GoalEntity>

    @Upsert
    suspend fun upsert(item: GoalEntity)

    @Upsert
    suspend fun upsertAll(items: List<GoalEntity>)
}

@Dao
interface MilestoneDao {
    @Query("SELECT * FROM milestones")
    suspend fun getAll(): List<MilestoneEntity>

    @Query("SELECT * FROM milestones WHERE projectId = :projectId ORDER BY sortOrder")
    suspend fun getForProject(projectId: String): List<MilestoneEntity>

    @Query("SELECT * FROM milestones WHERE goalId = :goalId ORDER BY sortOrder")
    suspend fun getForGoal(goalId: String): List<MilestoneEntity>

    @Query("SELECT * FROM milestones WHERE goalId = :goalId OR projectId = :projectId ORDER BY sortOrder")
    fun observeFor(goalId: String?, projectId: String?): Flow<List<MilestoneEntity>>

    @Query("DELETE FROM milestones WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM milestones WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: String)

    @Query("DELETE FROM milestones WHERE goalId = :goalId")
    suspend fun deleteForGoal(goalId: String)

    @Upsert
    suspend fun upsert(item: MilestoneEntity)

    @Upsert
    suspend fun upsertAll(items: List<MilestoneEntity>)
}

@Dao
interface TaskCompletionDao {
    @Query("SELECT * FROM task_completions ORDER BY completedAtMillis DESC")
    fun observeAll(): Flow<List<TaskCompletionEntity>>

    @Query("SELECT * FROM task_completions")
    suspend fun getAll(): List<TaskCompletionEntity>

    @Query("DELETE FROM task_completions WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: String)

    @Upsert
    suspend fun upsert(item: TaskCompletionEntity)

    @Upsert
    suspend fun upsertAll(items: List<TaskCompletionEntity>)
}

@Dao
interface GoalActivityDao {
    @Query("SELECT * FROM goal_activities ORDER BY activityEpochDay DESC")
    fun observeAll(): Flow<List<GoalActivityEntity>>

    @Query("SELECT * FROM goal_activities")
    suspend fun getAll(): List<GoalActivityEntity>

    @Upsert
    suspend fun upsert(item: GoalActivityEntity)

    @Upsert
    suspend fun upsertAll(items: List<GoalActivityEntity>)
}

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    fun observe(): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    suspend fun get(): UserPreferencesEntity?

    @Upsert
    suspend fun upsert(item: UserPreferencesEntity)
}

@Dao
interface DashboardDao {
    @Query("SELECT * FROM dashboard_configuration ORDER BY position")
    fun observeAll(): Flow<List<DashboardConfigurationEntity>>

    @Query("SELECT * FROM dashboard_configuration ORDER BY position")
    suspend fun getAll(): List<DashboardConfigurationEntity>

    @Upsert
    suspend fun upsert(item: DashboardConfigurationEntity)

    @Upsert
    suspend fun upsertAll(items: List<DashboardConfigurationEntity>)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name")
    suspend fun getAll(): List<TagEntity>

    @Upsert
    suspend fun upsertAll(items: List<TagEntity>)

    @Upsert
    suspend fun upsertCrossRefs(items: List<TaskTagCrossRef>)

    @Upsert
    suspend fun upsertNoteCrossRefs(items: List<NoteTagCrossRef>)

    @Query("SELECT * FROM task_tag_cross_ref")
    suspend fun getTaskCrossRefs(): List<TaskTagCrossRef>

    @Query("SELECT * FROM note_tag_cross_ref")
    suspend fun getNoteCrossRefs(): List<NoteTagCrossRef>
}

@Dao
interface SoundDao {
    @Query("SELECT * FROM notification_sounds ORDER BY importedAtMillis DESC")
    fun observeAll(): Flow<List<NotificationSoundEntity>>

    @Query("SELECT * FROM notification_sounds WHERE soundId = :id LIMIT 1")
    suspend fun get(id: String): NotificationSoundEntity?

    @Query("SELECT * FROM notification_sounds")
    suspend fun getAll(): List<NotificationSoundEntity>

    @Upsert
    suspend fun upsert(item: NotificationSoundEntity)

    @Upsert
    suspend fun upsertAll(items: List<NotificationSoundEntity>)
}

@Dao
interface ChannelConfigurationDao {
    @Query("SELECT * FROM notification_channel_configuration")
    suspend fun getAll(): List<NotificationChannelConfigurationEntity>

    @Upsert
    suspend fun upsertAll(items: List<NotificationChannelConfigurationEntity>)
}

@Dao
interface BackupMetadataDao {
    @Query("SELECT * FROM backup_metadata ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<BackupMetadataEntity>

    @Upsert
    suspend fun upsert(item: BackupMetadataEntity)

    @Upsert
    suspend fun upsertAll(items: List<BackupMetadataEntity>)
}

@Database(
    entities = [
        RecurrenceRuleEntity::class, GoalEntity::class, ProjectEntity::class, CourseEntity::class,
        TaskEntity::class, SubTaskEntity::class, EventEntity::class, DeadlineEntity::class,
        CourseModuleEntity::class, MilestoneEntity::class, ReminderEntity::class, NoteEntity::class,
        TagEntity::class, TaskTagCrossRef::class, NoteTagCrossRef::class, TaskCompletionEntity::class,
        GoalActivityEntity::class, UserPreferencesEntity::class, DashboardConfigurationEntity::class,
        NotificationSoundEntity::class, NotificationChannelConfigurationEntity::class,
        BackupMetadataEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class DaymarkDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun subTaskDao(): SubTaskDao
    abstract fun recurrenceRuleDao(): RecurrenceRuleDao
    abstract fun eventDao(): EventDao
    abstract fun deadlineDao(): DeadlineDao
    abstract fun reminderDao(): ReminderDao
    abstract fun noteDao(): NoteDao
    abstract fun courseDao(): CourseDao
    abstract fun courseModuleDao(): CourseModuleDao
    abstract fun projectDao(): ProjectDao
    abstract fun goalDao(): GoalDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun taskCompletionDao(): TaskCompletionDao
    abstract fun goalActivityDao(): GoalActivityDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun dashboardDao(): DashboardDao
    abstract fun tagDao(): TagDao
    abstract fun soundDao(): SoundDao
    abstract fun channelConfigurationDao(): ChannelConfigurationDao
    abstract fun backupMetadataDao(): BackupMetadataDao

    companion object {
        @Volatile private var instance: DaymarkDatabase? = null

        fun getInstance(context: Context): DaymarkDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                DaymarkDatabase::class.java,
                "daymark-local.db"
            ).build().also { instance = it }
        }
    }
}
