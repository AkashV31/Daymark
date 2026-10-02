package com.daymark.app.notifications

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.daymark.app.data.NotificationSoundEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/** Copies a SAF-selected audio file into app-created device media so the notification system can read it. */
object NotificationSoundImporter {
    suspend fun copyIntoDeviceMedia(context: Context, source: Uri): NotificationSoundEntity = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(source)?.takeIf { it.startsWith("audio/") }
            ?: throw IllegalArgumentException("Choose an audio file.")
        val sourceName = resolver.query(source, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            ?: "Daymark sound"
        val extension = sourceName.substringAfterLast('.', "audio").take(8).lowercase()
            .filter { it.isLetterOrDigit() }.ifBlank { "audio" }
        val displayName = "daymark_${UUID.randomUUID()}.$extension"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_NOTIFICATIONS}/Daymark")
            put(MediaStore.Audio.Media.IS_NOTIFICATION, 1)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val destination = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Daymark could not create a local sound file.")
        try {
            val input = resolver.openInputStream(source) ?: throw IllegalStateException("The selected audio file could not be read.")
            val output = resolver.openOutputStream(destination, "w") ?: throw IllegalStateException("Daymark could not copy the audio file.")
            input.use { src -> output.use { dst -> src.copyTo(dst) } }
            resolver.update(destination, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (error: Exception) {
            runCatching { resolver.delete(destination, null, null) }
            throw error
        }
        NotificationSoundEntity(
            soundId = "custom:${UUID.randomUUID()}",
            name = sourceName.substringBeforeLast('.').take(40).ifBlank { "Imported sound" },
            contentUri = destination.toString(),
            isBuiltIn = false
        )
    }
}
