package com.daymark.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Rebuilds alarms after process loss, reboot, app update, clock or timezone changes. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? com.daymark.app.DaymarkApplication
                runCatching {
                    app?.repository?.rollForwardRecurringTasks()
                    ReminderScheduler(context).rescheduleAll()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
