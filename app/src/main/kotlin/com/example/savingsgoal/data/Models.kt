package com.example.savingsgoal.data

import kotlinx.serialization.Serializable

@Serializable
data class Goal(
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val date: String? = null, // YYYY-MM-DD or null
    val folder: String = "MAIN", // "MAIN" or "ARCHIVE"
    val position: Int = 0,
    val note: String = "",
    val currency: String? = null,
    val imagePath: String? = null,
    val totalSaved: Double = 0.0,
    val paymentCount: Int = 0
) {
    val remainingAmount: Double
        get() = (amount - totalSaved).coerceAtLeast(0.0)

    val excessAmount: Double
        get() = (totalSaved - amount).coerceAtLeast(0.0)

    val isReached: Boolean
        get() = amount > 0 && totalSaved >= amount

    val progressFraction: Float
        get() = if (amount <= 0.0) 0f else (totalSaved / amount).toFloat().coerceIn(0f, 1f)

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()
}

@Serializable
data class Payment(
    val id: Long = 0,
    val mainId: Long,
    val date: String, // YYYY-MM-DD
    val amount: Double,
    val isSaving: Boolean = true, // true = deposit/saving, false = withdrawal
    val note: String = ""
)

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    HIGHEST_AMOUNT
}

enum class GoalSortOrder {
    HIGHEST_TO_LOWEST,
    LOWEST_TO_HIGHEST,
    NEWEST_FIRST,
    OLDEST_FIRST
}

data class SavingsRatePlan(
    val daysToGo: Long,
    val monthsToGo: Long,
    val yearsToGo: Long,
    val formattedTimeToGo: String,
    val isExpired: Boolean,
    val isReached: Boolean,
    val dailyAmount: Double,
    val weeklyAmount: Double,
    val biWeeklyAmount: Double,
    val monthlyAmount: Double
)

data class TotalSummary(
    val totalGoals: Int = 0,
    val totalTarget: Double = 0.0,
    val totalSaved: Double = 0.0,
    val totalRemaining: Double = 0.0
)

@Serializable
data class BackupPayload(
    val version: Int = 2,
    val exportDate: String,
    val goals: List<Goal>,
    val payments: List<Payment>
)

