package com.daymark.app.domain.assistant

import com.daymark.app.data.CourseEntity
import com.daymark.app.data.DeadlineEntity
import com.daymark.app.data.EventEntity
import com.daymark.app.data.GoalActivityEntity
import com.daymark.app.data.GoalEntity
import com.daymark.app.data.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * On-device assistant provider interface.
 * Any implementation must run entirely offline and on-device (e.g. SLM / Hermes / "digi paws").
 */
interface AssistantProvider {
    val isAvailable: Boolean
    suspend fun suggest(context: AssistantContext): List<AssistantSuggestion>
}

object NoopAssistantProvider : AssistantProvider {
    override val isAvailable: Boolean = false
    override suspend fun suggest(context: AssistantContext): List<AssistantSuggestion> = emptyList()
}

object AssistantContextBuilder {
    fun build(
        tasks: List<TaskEntity>,
        events: List<EventEntity>,
        deadlines: List<DeadlineEntity>,
        goals: List<GoalEntity>,
        goalActivities: List<GoalActivityEntity>,
        courses: List<CourseEntity>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): AssistantContext {
        val todayEpoch = today.toEpochDay()
        val openTasks = tasks.filter { !it.archived && it.status != "COMPLETED" && it.status != "SKIPPED" }
        val todayTasks = openTasks.filter { it.dueEpochDay == todayEpoch }
        val completedToday = tasks.filter {
            it.status == "COMPLETED" && it.completedAtMillis?.let { stamp ->
                Instant.ofEpochMilli(stamp).atZone(zone).toLocalDate() == today
            } == true
        }
        val overdueTasks = openTasks.filter { it.dueEpochDay != null && it.dueEpochDay < todayEpoch }
        val todaysEvents = events.filter { !it.archived && it.startEpochDay == todayEpoch }
        val upcomingDeadlines = deadlines.filter { !it.archived && it.status != "COMPLETED" }
            .sortedBy { it.dueEpochDay }
            .take(5)
        val activeGoals = goals.filter { it.status == "ACTIVE" }
        val inProgressCourses = courses.filter { it.progress in 1..99 }

        // Compute free time slots between 09:00 and 18:00
        val busyIntervals = mutableListOf<Pair<Int, Int>>()
        todaysEvents.forEach { event ->
            val start = event.startMinuteOfDay ?: 0
            val end = event.endMinuteOfDay ?: (start + 60)
            busyIntervals.add(start to end)
        }
        todayTasks.forEach { task ->
            task.dueMinuteOfDay?.let { start ->
                val duration = task.estimatedDurationMinutes ?: 30
                busyIntervals.add(start to (start + duration))
            }
        }
        busyIntervals.sortBy { it.first }

        val workStart = 9 * 60 // 09:00
        val workEnd = 18 * 60  // 18:00
        val freeSlots = mutableListOf<TimeSlot>()
        var currentPointer = workStart

        for ((bStart, bEnd) in busyIntervals) {
            if (bStart > currentPointer && bStart - currentPointer >= 30) {
                freeSlots.add(TimeSlot(currentPointer, minOf(bStart, workEnd)))
            }
            currentPointer = maxOf(currentPointer, bEnd)
            if (currentPointer >= workEnd) break
        }
        if (currentPointer < workEnd && workEnd - currentPointer >= 30) {
            freeSlots.add(TimeSlot(currentPointer, workEnd))
        }

        // Streak calculation
        val distinctActiveDays = goalActivities.map { it.activityEpochDay }.toSet()
        var streak = 0
        var dayCursor = todayEpoch
        while (distinctActiveDays.contains(dayCursor)) {
            streak++
            dayCursor--
        }

        return AssistantContext(
            todayEpochDay = todayEpoch,
            todaysTasks = todayTasks,
            completedTodayCount = completedToday.size,
            openTodayCount = todayTasks.size,
            overdueCount = overdueTasks.size,
            upcomingDeadlines = upcomingDeadlines,
            todaysEvents = todaysEvents,
            activeGoals = activeGoals,
            inProgressCourses = inProgressCourses,
            freeTimeSlots = freeSlots,
            streakDays = streak
        )
    }
}
