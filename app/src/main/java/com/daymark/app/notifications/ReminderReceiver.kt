package com.daymark.app.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.daymark.app.DaymarkApplication
import com.daymark.app.MainActivity
import com.daymark.app.R
import com.daymark.app.data.DaymarkRepository
import com.daymark.app.data.ReminderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ownerType = intent.getStringExtra(EXTRA_OWNER_TYPE) ?: return
        val ownerId = intent.getStringExtra(EXTRA_OWNER_ID) ?: return
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val key = intent.getStringExtra(EXTRA_ALARM_KEY) ?: "$ownerType:$ownerId:$reminderId"
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as DaymarkApplication
                val database = app.database
                val reminder = database.reminderDao().get(reminderId) ?: return@launch
                if (!reminder.enabled || reminder.ownerType != ownerType || reminder.ownerId != ownerId) return@launch

                val content = when (ownerType) {
                    DaymarkRepository.OWNER_TASK -> database.taskDao().get(ownerId)?.takeIf {
                        !it.archived && it.status != DaymarkRepository.STATUS_COMPLETED && it.status != "SKIPPED"
                    }?.let { Triple(it.title, it.description, true) }
                    DaymarkRepository.OWNER_EVENT -> database.eventDao().get(ownerId)?.takeIf { !it.archived }
                        ?.let { Triple(it.title, it.description, false) }
                    DaymarkRepository.OWNER_DEADLINE -> database.deadlineDao().get(ownerId)?.takeIf {
                        !it.archived && it.status != "COMPLETED"
                    }?.let { Triple(it.title, it.description, true) }
                    else -> null
                } ?: return@launch

                val isSnoozeDelivery = reminder.snoozedUntilMillis != null
                if (reminder.lastDeliveredAtMillis != null && !isSnoozeDelivery) return@launch
                val manager = NotificationManagerCompat.from(context)
                if (!manager.areNotificationsEnabled()) return@launch

                val preferences = database.preferenceDao().get()
                val soundId = reminder.soundId.ifBlank { preferences?.notificationSoundId ?: "default" }
                val sound = database.soundDao().get(soundId)
                val channelId = NotificationChannels.ensureChannel(
                    context,
                    soundId,
                    sound?.takeIf { !it.isBuiltIn }?.contentUri?.let { android.net.Uri.parse(it) }
                )
                val notificationId = key.hashCode()
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(EXTRA_OWNER_TYPE, ownerType)
                    putExtra(EXTRA_OWNER_ID, ownerId)
                    putExtra(EXTRA_REMINDER_ID, reminderId)
                }
                val openPending = PendingIntent.getActivity(
                    context,
                    notificationId,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val actionIntent = { action: String ->
                    Intent(context, NotificationActionReceiver::class.java).apply {
                        this.action = action
                        putExtra(EXTRA_OWNER_TYPE, ownerType)
                        putExtra(EXTRA_OWNER_ID, ownerId)
                        putExtra(EXTRA_REMINDER_ID, reminderId)
                        putExtra(EXTRA_ALARM_KEY, key)
                    }
                }
                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_stat_daymark)
                    .setContentTitle(content.first)
                    .setContentText(content.second.ifBlank {
                        when (ownerType) {
                            DaymarkRepository.OWNER_TASK -> "A task you planned is ready."
                            DaymarkRepository.OWNER_EVENT -> "An item on your schedule is coming up."
                            else -> "A deadline is coming up."
                        }
                    })
                    .setContentIntent(openPending)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .addAction(
                        R.drawable.ic_stat_daymark,
                        "Snooze 10m",
                        PendingIntent.getBroadcast(
                            context,
                            (key + ":snooze").hashCode(),
                            actionIntent(NotificationActionReceiver.ACTION_SNOOZE),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    .apply {
                        if (content.third) {
                            addAction(
                                R.drawable.ic_stat_daymark,
                                if (ownerType == DaymarkRepository.OWNER_DEADLINE) "Complete" else "Done",
                                PendingIntent.getBroadcast(
                                    context,
                                    (key + ":done").hashCode(),
                                    actionIntent(NotificationActionReceiver.ACTION_DONE),
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                            )
                        }
                    }
                    .build()

                try {
                    manager.notify(notificationId, notification)
                    database.reminderDao().upsert(
                        reminder.copy(
                            snoozedUntilMillis = null,
                            lastDeliveredAtMillis = System.currentTimeMillis()
                        )
                    )
                } catch (_: SecurityException) {
                    // Permission can be revoked after the alarm fires. Keep it undelivered so
                    // a later permission grant/reconcile can retry within the late-reminder window.
                }
            } catch (_: Exception) {
                // A damaged/stale reminder must not crash the application process or lose other data.
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_OWNER_TYPE = "daymark.owner_type"
        const val EXTRA_OWNER_ID = "daymark.owner_id"
        const val EXTRA_REMINDER_ID = "daymark.reminder_id"
        const val EXTRA_ALARM_KEY = "daymark.alarm_key"
        const val EXTRA_TITLE = "daymark.title"
    }
}
