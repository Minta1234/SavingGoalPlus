package com.minta1234.savingsgoal.notification

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

        // Show reminder notification with streak encouragement
        com.minta1234.savingsgoal.data.SavingsStreakManager.init(context)
        val streakInfo = com.minta1234.savingsgoal.data.SavingsStreakManager.streakState.value
        val (title, message) = if (streakInfo.currentStreak > 0) {
            Pair(
                "🔥 Keep your ${streakInfo.currentStreak}-day streak alive!",
                "Don't let your ${streakInfo.levelTitle} streak break! Record your deposit today."
            )
        } else {
            Pair(
                "Time to save for your goals! 💰",
                "Take a moment today to record your savings or review your progress."
            )
        }

        SavingsNotificationManager.showReminderNotification(
            context,
            title = title,
            message = message
        )

        // Reschedule for tomorrow
        if (isEnabled) {
            SavingsNotificationManager.scheduleDailyReminder(context, hour, minute)
        }
    }
}

