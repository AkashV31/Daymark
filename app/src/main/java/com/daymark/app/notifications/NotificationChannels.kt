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
    private const val TEST_CHANNEL_ID = "daymark_test"

    fun ensureBaseChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannelGroup(
            android.app.NotificationChannelGroup(GROUP_ID, context.getString(R.string.notification_group_name))
        )
        ensureChannel(context, "default")
        val testChannel = NotificationChannel(
            TEST_CHANNEL_ID,
            context.getString(R.string.notification_test_channel),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_test_channel_description)
            group = GROUP_ID
        }
        manager.createNotificationChannel(testChannel)
    }

    fun channelIdFor(soundId: String): String = "daymark_reminder_${soundId.replace(Regex("[^A-Za-z0-9_-]"), "_")}"

    /** A new channel is created for each sound choice because Android treats channel sound as user-owned. */
    fun ensureChannel(context: Context, soundId: String, importedUri: Uri? = null): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return channelIdFor(soundId)
        val channelId = channelIdFor(soundId)
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(channelId) != null) return channelId

        val sound = importedUri ?: NotificationSoundCatalog.builtIns
            .firstOrNull { it.id == soundId }
            ?.resourceId
            ?.let { Uri.parse("android.resource://${context.packageName}/$it") }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(
            channelId,
            "Daymark reminders · ${NotificationSoundCatalog.labelFor(soundId)}",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            group = GROUP_ID
            enableVibration(true)
            setSound(sound, attributes)
        }
        manager.createNotificationChannel(channel)
        return channelId
    }

    fun postTest(context: Context, soundId: String, importedUri: Uri? = null): Boolean {
        ensureBaseChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val channelId = ensureChannel(context, soundId, importedUri)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_daymark)
            .setContentTitle(context.getString(R.string.test_notification_title))
            .setContentText(context.getString(R.string.test_notification_body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        return runCatching {
            manager.notify(TEST_NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)
    }

    fun testChannelId(): String = TEST_CHANNEL_ID
}
