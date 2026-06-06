package com.henryliu.cbtreframe.shared

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceiver : BroadcastReceiver(), KoinComponent {
    private val settingsManager: SettingsManager by inject()
    private val scheduler: ReminderScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            CoroutineScope(Dispatchers.IO).launch {
                val isEnabled = settingsManager.getDailyReminderEnabled()
                if (isEnabled) {
                    val hour = settingsManager.getReminderHour()
                    val minute = settingsManager.getReminderMinute()
                    scheduler.scheduleDailyReminder(hour, minute)
                }
            }
        }
    }
}
