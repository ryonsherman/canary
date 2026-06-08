package com.canary

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class CanaryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            "Canary Reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Hourly reminders to tap your sticker"
            enableVibration(true)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "canary_reminder"
        const val REMINDER_NOTIFICATION_ID = 1
    }
}
