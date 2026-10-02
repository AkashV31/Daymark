package com.daymark.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.daymark.app.data.DaymarkDatabase
import com.daymark.app.data.DaymarkRepository
import com.daymark.app.data.ReminderEntity
import com.daymark.app.domain.ReminderPlanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/** AlarmManager adapter. Room remains the source of truth; alarms are rebuilt from it. */
class ReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val database = DaymarkDatabase.getInstance(appContext)
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class ScheduleSummary(
        val scheduled: Int,
        val exact: Int,
        val inexactFallbacks: Int,
        val skippedPast: Int
    )

    private data class PlannedAlarm(
        val key: String,
        val ownerType: String,
        val ownerId: String,
        val reminderId: String,
        val title: String,
        val triggerAtMillis: Long
    )

    suspend fun rescheduleAll(): ScheduleSummary = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val taskMap = database.taskDao().getAll()
            .filter { !it.archived && it.status != DaymarkRepository.STATUS_COMPLETED && it.status != "SKIPPED" }
            .associateBy { it.id }
        val eventMap = database.eventDao().getActive().associateBy { it.id }
        val deadlineMap = database.deadlineDao().getActive()
            .filter { !it.archived && it.status != "COMPLETED" }
            .associateBy { it.id }
        val pref = database.preferenceDao().get()
        val reminders = database.reminderDao().getEnabled()
        val now = System.currentTimeMillis()
        val planned = ArrayList<PlannedAlarm>(reminders.size)
        var skippedPast = 0

        reminders.forEach { reminder ->
            val isRecentSnooze = reminder.snoozedUntilMillis?.let { now - it <= LATE_SNOOZE_GRACE_MILLIS } == true
            if (reminder.lastDeliveredAtMillis != null && reminder.snoozedUntilMillis == null) return@forEach
            if (reminder.lastDeliveredAtMillis != null && !isRecentSnooze) return@forEach

            val titleAndDue = when (reminder.ownerType) {
                DaymarkRepository.OWNER_TASK -> taskMap[reminder.ownerId]?.let { task ->
                    task.title to ReminderPlanner.taskTrigger(task, reminder, zone)
                }
                DaymarkRepository.OWNER_EVENT -> eventMap[reminder.ownerId]?.let { event ->
                    event.title to ReminderPlanner.eventTrigger(event, reminder, zone)
                }
                DaymarkRepository.OWNER_DEADLINE -> deadlineMap[reminder.ownerId]?.let { deadline ->
                    deadline.title to ReminderPlanner.deadlineTrigger(deadline, reminder, zone)
                }
                else -> null
            } ?: return@forEach

            val dueInstant = titleAndDue.second ?: return@forEach
            val snoozeAt = reminder.snoozedUntilMillis
            val triggerAt = when {
                snoozeAt != null && snoozeAt > now -> snoozeAt
                snoozeAt != null && isRecentSnooze -> now + 1_000L
                snoozeAt != null -> { skippedPast++; return@forEach }
                dueInstant.toEpochMilli() > now -> dueInstant.toEpochMilli()
                reminder.lastDeliveredAtMillis == null && now - dueInstant.toEpochMilli() <= LATE_REMINDER_GRACE_MILLIS -> now + 1_000L
                else -> { skippedPast++; return@forEach }
            }
            val key = alarmKey(reminder.ownerType, reminder.ownerId, reminder.id)
            planned += PlannedAlarm(
                key = key,
                ownerType = reminder.ownerType,
                ownerId = reminder.ownerId,
                reminderId = reminder.id,
                title = titleAndDue.first,
                triggerAtMillis = triggerAt
            )
        }

        val oldKeys = prefs.getStringSet(KEY_SCHEDULED_ALARMS, emptySet()).orEmpty().toSet()
        val currentKeys = planned.mapTo(linkedSetOf()) { it.key }
        (oldKeys - currentKeys).forEach(::cancelAlarm)

        var exactCount = 0
        var inexactCount = 0
        planned.forEach { item ->
            val pending = pendingIntent(item, PendingIntent.FLAG_UPDATE_CURRENT)
            val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
            if (exactAllowed) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.triggerAtMillis, pending)
                    exactCount++
                } catch (_: SecurityException) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.triggerAtMillis, pending)
                    inexactCount++
                }
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.triggerAtMillis, pending)
                inexactCount++
            }
        }
        prefs.edit().putStringSet(KEY_SCHEDULED_ALARMS, currentKeys).apply()
        ScheduleSummary(planned.size, exactCount, inexactCount, skippedPast)
    }

    suspend fun cancelOwner(ownerType: String, ownerId: String) {
        // Reconciliation also cancels old identities after reminder removal/editing.
        rescheduleAll()
    }

    private fun alarmKey(ownerType: String, ownerId: String, reminderId: String) =
        "$ownerType:$ownerId:$reminderId"

    private fun pendingIntent(item: PlannedAlarm, flags: Int): PendingIntent {
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            action = "com.daymark.app.REMINDER:${item.key}"
            putExtra(ReminderReceiver.EXTRA_ALARM_KEY, item.key)
            putExtra(ReminderReceiver.EXTRA_OWNER_TYPE, item.ownerType)
            putExtra(ReminderReceiver.EXTRA_OWNER_ID, item.ownerId)
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, item.reminderId)
            putExtra(ReminderReceiver.EXTRA_TITLE, item.title)
        }
        return PendingIntent.getBroadcast(
            appContext,
            item.key.hashCode(),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelAlarm(key: String) {
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            action = "com.daymark.app.REMINDER:$key"
        }
        val pending = PendingIntent.getBroadcast(
            appContext,
            key.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }

    companion object {
        private const val PREFS_NAME = "daymark_alarm_reconciliation"
        private const val KEY_SCHEDULED_ALARMS = "scheduled_alarm_ids"
        private const val LATE_REMINDER_GRACE_MILLIS = 2 * 60 * 60 * 1000L
        private const val LATE_SNOOZE_GRACE_MILLIS = 12 * 60 * 60 * 1000L
    }
}
