package com.daymark.app.domain

import com.daymark.app.data.ReminderEntity
import com.daymark.app.data.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ReminderPlannerTest {
    @Test fun offsetIsAppliedToLocalScheduledTime() {
        val zone = ZoneId.of("Asia/Kolkata")
        val task = TaskEntity(
            title = "Submit assignment",
            dueEpochDay = LocalDate.of(2026, 10, 8).toEpochDay(),
            dueMinuteOfDay = 19 * 60
        )
        val reminder = ReminderEntity(ownerType = "TASK", ownerId = task.id, offsetMinutes = 30)
        val actual = ReminderPlanner.taskTrigger(task, reminder, zone)
        val expected = LocalDate.of(2026, 10, 8).atTime(18, 30).atZone(zone).toInstant()
        assertEquals(expected, actual)
    }

    @Test fun daylightSavingGapUsesFirstValidWallClockTimeAfterGap() {
        val zone = ZoneId.of("America/New_York")
        val task = TaskEntity(
            title = "DST check",
            dueEpochDay = LocalDate.of(2026, 3, 8).toEpochDay(),
            dueMinuteOfDay = 2 * 60 + 30
        )
        val reminder = ReminderEntity(ownerType = "TASK", ownerId = task.id, offsetMinutes = 0)
        val trigger = ReminderPlanner.taskTrigger(task, reminder, zone)!!
        assertEquals(LocalTime.of(3, 30), trigger.atZone(zone).toLocalTime())
    }

    @Test fun deviceTimezoneChangeRetainsTheLocalDateAndTime() {
        val day = LocalDate.of(2026, 10, 8).toEpochDay()
        val task = TaskEntity(title = "Travel", dueEpochDay = day, dueMinuteOfDay = 7 * 60)
        val reminder = ReminderEntity(ownerType = "TASK", ownerId = task.id, offsetMinutes = 0)
        val kolkata = ReminderPlanner.taskTrigger(task, reminder, ZoneId.of("Asia/Kolkata"))!!
        val london = ReminderPlanner.taskTrigger(task, reminder, ZoneId.of("Europe/London"))!!
        assertEquals(LocalDate.of(2026, 10, 8), kolkata.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate())
        assertEquals(LocalTime.of(7, 0), london.atZone(ZoneId.of("Europe/London")).toLocalTime())
        assertEquals(4 * 60 * 60L + 30 * 60L, java.time.Duration.between(kolkata, london).seconds)
    }

    @Test fun deadlineWithoutTimeDefaultsToLocalEndOfDay() {
        val deadline = com.daymark.app.data.DeadlineEntity(
            title = "Payment",
            dueEpochDay = LocalDate.of(2026, 10, 2).toEpochDay()
        )
        val instant = ReminderPlanner.deadlineTrigger(
            deadline,
            ReminderEntity(ownerType = "DEADLINE", ownerId = deadline.id),
            ZoneId.of("Asia/Kolkata")
        )!!
        assertEquals(LocalTime.of(23, 59), instant.atZone(ZoneId.of("Asia/Kolkata")).toLocalTime())
    }
}
