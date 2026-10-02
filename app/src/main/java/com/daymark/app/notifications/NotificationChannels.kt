package com.daymark.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.daymark.app.R

 data class BuiltInSound(val id: String, val label: String, val resourceId: Int?)

object NotificationSoundCatalog {
    val builtIns = listOf(
        BuiltInSound("default", "Default", null),
        BuiltInSound("soft_bell", "Soft Bell", R.raw.sound_soft_bell),
        BuiltInSound("gentle_chime", "Gentle Chime", R.raw.sound_gentle_chime),
        BuiltInSound("focus", "Focus", R.raw.sound_focus),
        BuiltInSound("digital", "Digital", R.raw.sound_digital),
        BuiltInSound("urgent", "Urgent", R.raw.sound_urgent)
    )

    fun labelFor(id: String): String = builtIns.firstOrNull { it.id == id }?.label ?: "Imported sound"
}

object NotificationChannels {
    private const val GROUP_ID = "daymark_reminders"
    const val TEST_NOTIFICATION_ID = 740_001

    fun ensureBaseChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannelGroup(
            android.app.NotificationChannelGroup(GROUP_ID, context.getString(R.string.notification_group_name))
        )
        // Clean up stale pre-v2 and dead test channels
        manager.notificationChannels?.forEach { ch ->
            if (ch.id.startsWith("daymark_reminder_") && !ch.id.startsWith("daymark_reminder_v2_")) {
                manager.deleteNotificationChannel(ch.id)
            }
            if (ch.id == "daymark_test") {
                manager.deleteNotificationChannel(ch.id)
            }
        }
        ensureChannel(context, "default", loud = true)
        ensureChannel(context, "default", loud = false)
    }

    fun channelIdFor(soundId: String, loud: Boolean = true): String {
        val tier = if (loud) "alarm" else "notif"
        return "daymark_reminder_v2_${tier}_${soundId.replace(Regex("[^A-Za-z0-9_-]"), "_")}"
    }

    /** A new channel is created for each sound choice and loudness mode because Android treats channel settings as user-owned. */
    fun ensureChannel(context: Context, soundId: String, importedUri: Uri? = null, loud: Boolean = true): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return channelIdFor(soundId, loud)
        val channelId = channelIdFor(soundId, loud)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return channelId
        if (manager.getNotificationChannel(channelId) != null) return channelId

        val sound = importedUri ?: NotificationSoundCatalog.builtIns
            .firstOrNull { it.id == soundId }
            ?.resourceId
            ?.let { Uri.parse("android.resource://${context.packageName}/$it") }
            ?: RingtoneManager.getDefaultUri(if (loud) RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION)

        val attributes = AudioAttributes.Builder()
            .setUsage(if (loud) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build()
        val channel = NotificationChannel(
            channelId,
            "Daymark reminders · ${NotificationSoundCatalog.labelFor(soundId)}${if (loud) " (Loud)" else ""}",
            if (loud) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            group = GROUP_ID
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            setSound(sound, attributes)
        }
        manager.createNotificationChannel(channel)
        return channelId
    }

    fun postTest(context: Context, soundId: String, importedUri: Uri? = null, loud: Boolean = true): Boolean {
        ensureBaseChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val channelId = ensureChannel(context, soundId, importedUri, loud)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_daymark)
            .setContentTitle(context.getString(R.string.test_notification_title))
            .setContentText(context.getString(R.string.test_notification_body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setShowWhen(true)
            .build()
        return runCatching {
            manager.notify(TEST_NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)
    }
}
