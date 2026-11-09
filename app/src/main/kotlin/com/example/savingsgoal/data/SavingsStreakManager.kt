package com.example.savingsgoal.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class StreakInfo(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastSavedDate: String? = null,
    val totalDaysSaved: Int = 0,
    val isStreakActiveToday: Boolean = false
) {
    val levelTitle: String
        get() = when {
            currentStreak >= 100 -> "👑 Savings Titan"
            currentStreak >= 30 -> "💎 Month Master"
            currentStreak >= 14 -> "🏆 Fortnight Legend"
            currentStreak >= 7 -> "⚡ Week Champion"
            currentStreak >= 3 -> "🔥 Warming Up"
            currentStreak >= 1 -> "🌱 Habit Spark"
            else -> "✨ Start Your Streak"
        }

    val motivationalText: String
        get() = when {
            isStreakActiveToday -> "Great job! You kept your streak burning today! 🔥"
            currentStreak > 0 -> "Deposit today to keep your $currentStreak-day streak alive! 🔥"
            else -> "Record your first deposit to ignite your savings streak!"
        }
}

object SavingsStreakManager {
    private const val PREFS_NAME = "com.rmystudio.savingsgoal.PREFERENCE_FILE_KEY"
    private const val KEY_CURRENT_STREAK = "STREAK_CURRENT"
    private const val KEY_BEST_STREAK = "STREAK_BEST"
    private const val KEY_LAST_DATE = "STREAK_LAST_DATE"
    private const val KEY_TOTAL_DAYS = "STREAK_TOTAL_DAYS"

    private val _streakState = MutableStateFlow(StreakInfo())
    val streakState: StateFlow<StreakInfo> = _streakState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_CURRENT_STREAK, 0)
        val best = prefs.getInt(KEY_BEST_STREAK, 0)
        val lastDate = prefs.getString(KEY_LAST_DATE, null)
        val total = prefs.getInt(KEY_TOTAL_DAYS, 0)

        val today = getTodayDateString()
        val yesterday = getYesterdayDateString()

        // Check if streak broke because yesterday was missed
        val validatedStreak = when {
            lastDate == null -> 0
            lastDate == today -> current
            lastDate == yesterday -> current
            else -> 0 // Missed a day! Streak resets
        }

        if (validatedStreak != current) {
            prefs.edit().putInt(KEY_CURRENT_STREAK, validatedStreak).apply()
        }

        _streakState.value = StreakInfo(
            currentStreak = validatedStreak,
            bestStreak = maxOf(best, validatedStreak),
            lastSavedDate = lastDate,
            totalDaysSaved = total,
            isStreakActiveToday = (lastDate == today)
        )
    }

    fun recordDepositStreak(context: Context): StreakInfo {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getTodayDateString()
        val yesterday = getYesterdayDateString()

        val lastDate = prefs.getString(KEY_LAST_DATE, null)
        var current = prefs.getInt(KEY_CURRENT_STREAK, 0)
        var best = prefs.getInt(KEY_BEST_STREAK, 0)
        var total = prefs.getInt(KEY_TOTAL_DAYS, 0)

        when {
            lastDate == today -> {
                // Already recorded today, streak maintained
            }
            lastDate == yesterday -> {
                // Saved yesterday, streak increments!
                current += 1
                total += 1
            }
            else -> {
                // First time or streak broken, start at 1
                current = 1
                total += 1
            }
        }

        if (current > best) {
            best = current
        }

        prefs.edit()
            .putInt(KEY_CURRENT_STREAK, current)
            .putInt(KEY_BEST_STREAK, best)
            .putString(KEY_LAST_DATE, today)
            .putInt(KEY_TOTAL_DAYS, total)
            .apply()

        val newInfo = StreakInfo(
            currentStreak = current,
            bestStreak = best,
            lastSavedDate = today,
            totalDaysSaved = total,
            isStreakActiveToday = true
        )
        _streakState.value = newInfo
        return newInfo
    }

    private fun getTodayDateString(): String = dateFormat.format(Date())

    private fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(cal.time)
    }
}

