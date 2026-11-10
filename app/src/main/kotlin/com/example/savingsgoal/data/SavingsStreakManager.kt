package com.example.savingsgoal.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class StreakDay(val label: String, val description: String, val saved: Boolean, val isToday: Boolean)

data class StreakInfo(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastSavedDate: String? = null,
    val totalDaysSaved: Int = 0,
    val isStreakActiveToday: Boolean = false,
    val recentDays: List<StreakDay> = emptyList()
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
    private const val KEY_RECENT_DATES = "STREAK_RECENT_DATES"

    private val _streakState = MutableStateFlow(StreakInfo())
    val streakState: StateFlow<StreakInfo> = _streakState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    @Synchronized
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_CURRENT_STREAK, 0)
        val best = prefs.getInt(KEY_BEST_STREAK, 0)
        val lastDate = prefs.getString(KEY_LAST_DATE, null)
        val total = prefs.getInt(KEY_TOTAL_DAYS, 0)

        val today = getTodayDateString()
        val yesterday = getYesterdayDateString()

        val dates = recentDates(prefs, lastDate, current)
        if (!prefs.contains(KEY_RECENT_DATES)) {
            prefs.edit().putStringSet(KEY_RECENT_DATES, dates).apply()
        }

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
            isStreakActiveToday = (lastDate == today),
            recentDays = recentDays(dates)
        )
    }

    @Synchronized
    fun recordDepositStreak(context: Context): StreakInfo {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getTodayDateString()
        val yesterday = getYesterdayDateString()

        val lastDate = prefs.getString(KEY_LAST_DATE, null)
        var current = prefs.getInt(KEY_CURRENT_STREAK, 0)
        var best = prefs.getInt(KEY_BEST_STREAK, 0)
        var total = prefs.getInt(KEY_TOTAL_DAYS, 0)
        val dates = recentDates(prefs, lastDate, current) + today

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
            .putStringSet(KEY_RECENT_DATES, dates)
            .apply()

        val newInfo = StreakInfo(
            currentStreak = current,
            bestStreak = best,
            lastSavedDate = today,
            totalDaysSaved = total,
            isStreakActiveToday = true,
            recentDays = recentDays(dates)
        )
        _streakState.value = newInfo
        return newInfo
    }

    private fun recentDates(prefs: SharedPreferences, lastDate: String?, current: Int): Set<String> {
        // Older installs only know the dates belonging to their last consecutive streak.
        val dates = prefs.getStringSet(KEY_RECENT_DATES, null) ?: run {
            val lastSaved = try {
                lastDate?.let(dateFormat::parse)
            } catch (_: ParseException) {
                null
            }
            if (lastSaved == null) emptySet() else {
                (0 until current.coerceIn(1, 7)).map { dateFormat.format(dayOffset(lastSaved, -it)) }.toSet()
            }
        }
        val now = Date()
        val visibleDates = (-6..0).map { dateFormat.format(dayOffset(now, it)) }.toSet()
        return dates.intersect(visibleDates)
    }

    private fun recentDays(dates: Set<String>): List<StreakDay> {
        val now = Date()
        val label = SimpleDateFormat("EEE", Locale.getDefault())
        val description = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        return (-6..0).map { offset ->
            val day = dayOffset(now, offset)
            StreakDay(label.format(day), description.format(day), dateFormat.format(day) in dates, offset == 0)
        }
    }

    private fun dayOffset(date: Date, offset: Int): Date = Calendar.getInstance().apply {
        time = date
        add(Calendar.DAY_OF_YEAR, offset)
    }.time

    private fun getTodayDateString(): String = dateFormat.format(Date())

    private fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(cal.time)
    }
}

