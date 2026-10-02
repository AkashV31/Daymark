package com.daymark.app.domain.assistant

import com.daymark.app.data.CourseEntity
import com.daymark.app.data.DeadlineEntity
import com.daymark.app.data.EventEntity
import com.daymark.app.data.GoalEntity
import com.daymark.app.data.TaskEntity

/**
 * An immutable, computed snapshot of current personal-organization state.
 * This is the stable input consumed by on-device assistants and SLM models.
 * Contract: Any model processing this context must execute strictly on-device
 * without sending data across network boundaries.
 */
data class AssistantContext(
    val todayEpochDay: Long,
    val todaysTasks: List<TaskEntity>,
    val completedTodayCount: Int,
    val openTodayCount: Int,
    val overdueCount: Int,
    val upcomingDeadlines: List<DeadlineEntity>,
    val todaysEvents: List<EventEntity>,
    val activeGoals: List<GoalEntity>,
    val inProgressCourses: List<CourseEntity>,
    val freeTimeSlots: List<TimeSlot> = emptyList(),
    val streakDays: Int = 0
)

data class TimeSlot(
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int
)

data class AssistantSuggestion(
    val id: String,
    val title: String,
    val message: String,
    val priority: SuggestionPriority = SuggestionPriority.NORMAL,
    val actionType: String? = null,
    val targetRecordId: String? = null
)

enum class SuggestionPriority {
    LOW, NORMAL, HIGH
}
