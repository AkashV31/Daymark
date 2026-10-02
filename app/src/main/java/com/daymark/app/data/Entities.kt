package com.daymark.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Room entities deliberately store civil dates as epoch-day values and local wall-clock
 * times as minutes after midnight. Alarm instants are derived from these values and the
 * current ZoneId rather than persisted as ambiguous date strings.
 */
@Serializable
@Entity(tableName = "recurrence_rules", indices = [Index("frequency")])
data class RecurrenceRuleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val frequency: String,
    val interval: Int = 1,
    /** Monday is bit 0, Sunday is bit 6. Used for CUSTOM/WEEKLY rules. */
    val weekdaysMask: Int = 0,
    val endEpochDay: Long? = null,
    val occurrenceLimit: Int? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "goals",
    indices = [Index("targetEpochDay"), Index("status")]
)
data class GoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val startEpochDay: Long,
    val targetEpochDay: Long? = null,
    val progress: Int = 0,
    val status: String = "ACTIVE",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "projects",
    indices = [Index("targetEpochDay"), Index("goalId"), Index("status")],
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val targetEpochDay: Long? = null,
    val goalId: String? = null,
    val status: String = "ACTIVE",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "courses",
    indices = [Index("targetEpochDay"), Index("category")]
)
data class CourseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val platform: String = "",
    val instructor: String = "",
    val category: String = "",
    val startEpochDay: Long? = null,
    val targetEpochDay: Long? = null,
    val progress: Int = 0,
    val referenceUrl: String = "",
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "tasks",
    indices = [
        Index("dueEpochDay"), Index("status"), Index("priority"), Index("seriesId"),
        Index("goalId"), Index("projectId"), Index("courseId"), Index("recurrenceRuleId")
    ],
    foreignKeys = [
        ForeignKey(entity = RecurrenceRuleEntity::class, parentColumns = ["id"], childColumns = ["recurrenceRuleId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CourseEntity::class, parentColumns = ["id"], childColumns = ["courseId"], onDelete = ForeignKey.SET_NULL)
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val dueEpochDay: Long? = null,
    val dueMinuteOfDay: Int? = null,
    val estimatedDurationMinutes: Int? = null,
    /** LOW, NORMAL, HIGH, CRITICAL */
    val priority: String = "NORMAL",
    /** PLANNED, IN_PROGRESS, COMPLETED, SKIPPED, OVERDUE */
    val status: String = "PLANNED",
    val category: String = "",
    val courseId: String? = null,
    val projectId: String? = null,
    val goalId: String? = null,
    val recurrenceRuleId: String? = null,
    val seriesId: String? = null,
    val occurrenceIndex: Int = 0,
    val recurrenceGenerated: Boolean = false,
    val notes: String = "",
    val completedAtMillis: Long? = null,
    val archived: Boolean = false
)

@Serializable
@Entity(
    tableName = "subtasks",
    indices = [Index("taskId"), Index(value = ["taskId", "sortOrder"])],
    foreignKeys = [ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)]
)
data class SubTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val completedAtMillis: Long? = null
)

@Serializable
@Entity(
    tableName = "events",
    indices = [Index("startEpochDay"), Index("category"), Index("projectId"), Index("courseId")],
    foreignKeys = [
        ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CourseEntity::class, parentColumns = ["id"], childColumns = ["courseId"], onDelete = ForeignKey.SET_NULL)
    ]
)
data class EventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val startEpochDay: Long,
    val startMinuteOfDay: Int? = null,
    val endEpochDay: Long? = null,
    val endMinuteOfDay: Int? = null,
    val isAllDay: Boolean = false,
    val category: String = "",
    val projectId: String? = null,
    val courseId: String? = null,
    val recurrenceRuleId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val archived: Boolean = false
)

@Serializable
@Entity(
    tableName = "deadlines",
    indices = [Index("dueEpochDay"), Index("courseId"), Index("projectId"), Index("status")],
    foreignKeys = [
        ForeignKey(entity = CourseEntity::class, parentColumns = ["id"], childColumns = ["courseId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.SET_NULL)
    ]
)
data class DeadlineEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val dueEpochDay: Long,
    val dueMinuteOfDay: Int? = null,
    val category: String = "",
    val courseId: String? = null,
    val projectId: String? = null,
    val status: String = "OPEN",
    val completedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val archived: Boolean = false
)

@Serializable
@Entity(
    tableName = "course_modules",
    indices = [Index("courseId"), Index(value = ["courseId", "sortOrder"])],
    foreignKeys = [ForeignKey(entity = CourseEntity::class, parentColumns = ["id"], childColumns = ["courseId"], onDelete = ForeignKey.CASCADE)]
)
data class CourseModuleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val courseId: String,
    val title: String,
    val sortOrder: Int = 0,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val notes: String = ""
)

@Serializable
@Entity(
    tableName = "milestones",
    indices = [Index("projectId"), Index("goalId"), Index("targetEpochDay")],
    foreignKeys = [
        ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class MilestoneEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val projectId: String? = null,
    val goalId: String? = null,
    val targetEpochDay: Long? = null,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val sortOrder: Int = 0
)

@Serializable
@Entity(
    tableName = "reminders",
    indices = [Index(value = ["ownerType", "ownerId"]), Index("enabled")]
)
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    /** TASK, EVENT, or DEADLINE. Polymorphic owner IDs are validated by the repository. */
    val ownerType: String,
    val ownerId: String,
    /** Minutes before the owner's scheduled local date/time. Zero means at the due time. */
    val offsetMinutes: Int = 0,
    val enabled: Boolean = true,
    val soundId: String = "",
    val zoneId: String = "",
    val snoozedUntilMillis: Long? = null,
    val lastDeliveredAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "notes",
    indices = [Index("updatedAtMillis"), Index("isPinned"), Index("linkedCourseId"), Index("linkedProjectId"), Index("linkedGoalId")],
    foreignKeys = [
        ForeignKey(entity = CourseEntity::class, parentColumns = ["id"], childColumns = ["linkedCourseId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["linkedProjectId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["linkedGoalId"], onDelete = ForeignKey.SET_NULL)
    ]
)
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val linkedCourseId: String? = null,
    val linkedProjectId: String? = null,
    val linkedGoalId: String? = null,
    val archived: Boolean = false
)

@Serializable
@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorKey: String = "lavender"
)

@Serializable
@Entity(
    tableName = "task_tag_cross_ref",
    primaryKeys = ["taskId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("tagId")]
)
data class TaskTagCrossRef(val taskId: String, val tagId: String)

@Serializable
@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("tagId")]
)
data class NoteTagCrossRef(val noteId: String, val tagId: String)

@Serializable
@Entity(
    tableName = "task_completions",
    indices = [Index("seriesId"), Index("completedAtMillis"), Index(value = ["seriesId", "occurrenceDateEpochDay"], unique = true)],
    foreignKeys = [ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)]
)
data class TaskCompletionEntity(
    @PrimaryKey val taskId: String,
    val seriesId: String? = null,
    val occurrenceDateEpochDay: Long,
    val completedAtMillis: Long
)

@Serializable
@Entity(
    tableName = "goal_activities",
    indices = [Index("goalId"), Index("activityEpochDay"), Index(value = ["goalId", "activityEpochDay"], unique = true)],
    foreignKeys = [
        ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.SET_NULL)
    ]
)
data class GoalActivityEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val goalId: String,
    val taskId: String? = null,
    val activityEpochDay: Long,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val meaningfulActivityCount: Int = 1
)

@Serializable
@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String = "",
    val themeKey: String = "LIGHT",
    val notificationSoundId: String = "default",
    val use24HourClock: Boolean = false,
    val weekStartsOn: Int = 1,
    val notificationsExplained: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "dashboard_configuration", indices = [Index("position")])
data class DashboardConfigurationEntity(
    @PrimaryKey val widgetKey: String,
    val isVisible: Boolean = true,
    val position: Int = 0,
    val configurationJson: String = "{}"
)

@Serializable
@Entity(tableName = "notification_sounds")
data class NotificationSoundEntity(
    @PrimaryKey val soundId: String,
    val name: String,
    val contentUri: String,
    val isBuiltIn: Boolean = false,
    val importedAtMillis: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "notification_channel_configuration")
data class NotificationChannelConfigurationEntity(
    @PrimaryKey val channelId: String,
    val soundId: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "backup_metadata", indices = [Index("createdAtMillis")])
data class BackupMetadataEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val schemaVersion: Int,
    val createdAtMillis: Long,
    val note: String = ""
)

object DashboardWidgets {
    const val TODAY_PROGRESS = "TODAY_PROGRESS"
    const val NEXT_UP = "NEXT_UP"
    const val TODAY_TASKS = "TODAY_TASKS"
    const val DEADLINES = "DEADLINES"
    const val GOALS = "GOALS"
    const val COURSES = "COURSES"
    const val RECENT_NOTES = "RECENT_NOTES"
    const val INSIGHTS = "INSIGHTS"

    val defaults = listOf(
        DashboardConfigurationEntity(TODAY_PROGRESS, true, 0),
        DashboardConfigurationEntity(NEXT_UP, true, 1),
        DashboardConfigurationEntity(TODAY_TASKS, true, 2),
        DashboardConfigurationEntity(DEADLINES, true, 3),
        DashboardConfigurationEntity(GOALS, true, 4),
        DashboardConfigurationEntity(COURSES, false, 5),
        DashboardConfigurationEntity(RECENT_NOTES, false, 6),
        DashboardConfigurationEntity(INSIGHTS, false, 7)
    )
}
