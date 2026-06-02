package com.henryliu.cbtreframe.shared

import android.content.Context
import android.util.Log

class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {
    override suspend fun requestPermission(): Boolean {
        Log.d("ReminderScheduler", "requestPermission called on Android")
        return true
    }

    override suspend fun scheduleDailyReminder(hour: Int, minute: Int) {
        Log.d("ReminderScheduler", "scheduleDailyReminder called on Android for $hour:$minute")
    }

    override fun cancelDailyReminder() {
        Log.d("ReminderScheduler", "cancelDailyReminder called on Android")
    }
}
