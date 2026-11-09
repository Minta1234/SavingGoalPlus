package com.example.savingsgoal.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SavingsReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences(
            "com.rmystudio.savingsgoal.PREFERENCE_FILE_KEY",
            Context.MODE_PRIVATE
        )
        val isEnabled = prefs.getBoolean("REMINDER_ENABLED", false)
        val hour = prefs.getInt("REMINDER_HOUR", 20)
        val minute = prefs.getInt("REMINDER_MINUTE", 0)

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule after reboot
            if (isEnabled) {
                SavingsNotificationManager.scheduleDailyReminder(context, hour, minute)
            }
            return
        }

        // Show reminder notification
        SavingsNotificationManager.showReminderNotification(
            context,
            title = "Time to save for your goals! 💰",
            message = "Take a moment today to record your savings or review your progress."
        )

        // Reschedule for tomorrow
        if (isEnabled) {
            SavingsNotificationManager.scheduleDailyReminder(context, hour, minute)
        }
    }
}

