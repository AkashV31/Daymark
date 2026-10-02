package com.daymark.app.domain

import com.daymark.app.data.DeadlineEntity
import com.daymark.app.data.EventEntity
import com.daymark.app.data.ReminderEntity
import com.daymark.app.data.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Converts civil dates and local times into an alarm instant using timezone/DST rules. */
object ReminderPlanner {
    fun taskTrigger(
        task: TaskEntity,
        reminder: ReminderEntity,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Instant? {
        val day = task.dueEpochDay ?: return null
        val time = task.dueMinuteOfDay?.let(::minuteToTime) ?: LocalTime.of(9, 0)
        return trigger(day, time, reminder.offsetMinutes, zoneId)
    }

    fun eventTrigger(
        event: EventEntity,
        reminder: ReminderEntity,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Instant? {
        val time = if (event.isAllDay) LocalTime.of(9, 0)
        else event.startMinuteOfDay?.let(::minuteToTime) ?: LocalTime.of(9, 0)
        return trigger(event.startEpochDay, time, reminder.offsetMinutes, zoneId)
    }

    fun deadlineTrigger(
        deadline: DeadlineEntity,
        reminder: ReminderEntity,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Instant? {
        val time = deadline.dueMinuteOfDay?.let(::minuteToTime) ?: LocalTime.of(23, 59)
        return trigger(deadline.dueEpochDay, time, reminder.offsetMinutes, zoneId)
    }

    fun atLocalTime(epochDay: Long, minuteOfDay: Int?, defaultTime: LocalTime, zoneId: ZoneId): ZonedDateTime {
        val date = LocalDate.ofEpochDay(epochDay)
        val time = minuteOfDay?.let(::minuteToTime) ?: defaultTime
        // ZonedDateTime applies the platform tzdb rule for gaps/overlaps (gap -> next valid time).
        return date.atTime(time).atZone(zoneId)
    }

    fun minuteToTime(minuteOfDay: Int): LocalTime {
        val safeMinute = minuteOfDay.coerceIn(0, 23 * 60 + 59)
        return LocalTime.of(safeMinute / 60, safeMinute % 60)
    }

    private fun trigger(epochDay: Long, dueTime: LocalTime, offsetMinutes: Int, zoneId: ZoneId): Instant {
        return LocalDate.ofEpochDay(epochDay)
            .atTime(dueTime)
            .atZone(zoneId)
            .minusMinutes(offsetMinutes.coerceAtLeast(0).toLong())
            .toInstant()
    }
}
