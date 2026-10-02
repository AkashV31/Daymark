package com.daymark.app

import android.app.Application
import com.daymark.app.data.DaymarkDatabase
import com.daymark.app.data.DaymarkRepository
import com.daymark.app.notifications.NotificationChannels
import com.daymark.app.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DaymarkApplication : Application() {
    val database: DaymarkDatabase by lazy { DaymarkDatabase.getInstance(this) }
    val repository: DaymarkRepository by lazy { DaymarkRepository(database) }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureBaseChannel(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { repository.initializeDefaults() }
            runCatching { ReminderScheduler(this@DaymarkApplication).rescheduleAll() }
        }
    }
}
