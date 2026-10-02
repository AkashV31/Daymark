package com.daymark.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.daymark.app.DaymarkApplication
import com.daymark.app.data.DaymarkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val ownerType = intent.getStringExtra(ReminderReceiver.EXTRA_OWNER_TYPE) ?: return
        val ownerId = intent.getStringExtra(ReminderReceiver.EXTRA_OWNER_ID) ?: return
        val reminderId = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_ID) ?: return
        val key = intent.getStringExtra(ReminderReceiver.EXTRA_ALARM_KEY) ?: "$ownerType:$ownerId:$reminderId"
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as DaymarkApplication
                val database = app.database
                when (action) {
                    ACTION_SNOOZE -> {
                        val reminder = database.reminderDao().get(reminderId)
                        if (reminder != null && reminder.enabled) {
                            database.reminderDao().upsert(
                                reminder.copy(snoozedUntilMillis = System.currentTimeMillis() + SNOOZE_MILLIS)
                            )
                        }
                    }
                    ACTION_DONE -> when (ownerType) {
                        DaymarkRepository.OWNER_TASK -> app.repository.completeTask(ownerId)
                        DaymarkRepository.OWNER_DEADLINE -> app.repository.completeDeadline(ownerId)
                    }
                    ACTION_DISMISS -> Unit
                }
                NotificationManagerCompat.from(context).cancel(key.hashCode())
                ReminderScheduler(context).rescheduleAll()
            } catch (_: Exception) {
                // Receiver work is best-effort; the database remains the source of truth.
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.daymark.app.action.DONE"
        const val ACTION_SNOOZE = "com.daymark.app.action.SNOOZE"
        const val ACTION_DISMISS = "com.daymark.app.action.DISMISS"
        private const val SNOOZE_MILLIS = 10 * 60 * 1000L
    }
}
