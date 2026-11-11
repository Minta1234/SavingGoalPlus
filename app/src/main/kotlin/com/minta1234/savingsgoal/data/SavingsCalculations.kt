package com.minta1234.savingsgoal.data

import java.text.NumberFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale

object SavingsCalculations {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

    fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank() || dateStr == "null" || dateStr == "NULL") return null
        return try {
            if (dateStr.contains("-")) {
                try {
                    LocalDate.parse(dateStr)
                } catch (_: DateTimeParseException) {
                    LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                }
            } else if (dateStr.contains("/")) {
                LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            } else {
                LocalDate.parse(dateStr, displayDateFormatter)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun formatDateForDisplay(dateStr: String?): String {
        val parsed = parseDate(dateStr) ?: return ""
        return parsed.format(displayDateFormatter)
    }

    fun formatDateForStorage(date: LocalDate): String {
        return date.format(dateFormatter)
    }

    fun calculateRatePlan(
        targetDateStr: String?,
        remainingAmount: Double,
        isReached: Boolean
    ): SavingsRatePlan? {
        val targetDate = parseDate(targetDateStr) ?: return null
        val today = LocalDate.now()

        val isExpired = targetDate.isBefore(today)
        val period = Period.between(today, targetDate)
        val years = period.years.coerceAtLeast(0)
        val months = period.months.coerceAtLeast(0)
        val days = period.days.coerceAtLeast(0)

        val timeParts = mutableListOf<String>()
        if (years > 0) timeParts.add("$years Year" + if (years > 1) "s" else "")
        if (months > 0) timeParts.add("$months Month" + if (months > 1) "s" else "")
        if (days > 0) timeParts.add("$days Day" + if (days > 1) "s" else "")
        val formattedTimeToGo = if (timeParts.isEmpty()) "Today" else "${timeParts.joinToString(" ")} To Go"

        if (isExpired || isReached || remainingAmount <= 0.0) {
            return SavingsRatePlan(
                daysToGo = 0,
                monthsToGo = 0,
                yearsToGo = 0,
                formattedTimeToGo = if (isReached) "Goal Reached!" else "Target Date Passed",
                isExpired = isExpired,
                isReached = isReached,
                dailyAmount = 0.0,
                weeklyAmount = 0.0,
                biWeeklyAmount = 0.0,
                monthlyAmount = 0.0
            )
        }

        val totalDays = ChronoUnit.DAYS.between(today, targetDate).coerceAtLeast(1)
        val totalWeeks = ChronoUnit.WEEKS.between(today, targetDate).coerceAtLeast(1)
        val biWeeks = (totalWeeks / 2).coerceAtLeast(1)
        var totalMonths = ChronoUnit.MONTHS.between(today, targetDate)
        if (totalMonths <= 0) {
            totalMonths = 1
        } else if (today.dayOfMonth != targetDate.dayOfMonth) {
            totalMonths++
        }

        val daily = remainingAmount / totalDays
        val weekly = remainingAmount / totalWeeks
        val biWeekly = remainingAmount / biWeeks
        val monthly = remainingAmount / totalMonths

        return SavingsRatePlan(
            daysToGo = totalDays,
            monthsToGo = totalMonths,
            yearsToGo = years.toLong(),
            formattedTimeToGo = formattedTimeToGo,
            isExpired = false,
            isReached = false,
            dailyAmount = daily,
            weeklyAmount = weekly,
            biWeeklyAmount = biWeekly,
            monthlyAmount = monthly
        )
    }

    fun formatCurrency(amount: Double, currencySymbol: String): String {
        val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val symbol = if (currencySymbol.isBlank() || currencySymbol == "null") "$" else currencySymbol
        return "$symbol ${numberFormat.format(amount)}"
    }

    fun formatNumber(amount: Double): String {
        val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            isGroupingUsed = false
        }
        return numberFormat.format(amount)
    }

    fun parseAmount(input: String): Double {
        if (input.isBlank() || input == ".") return 0.0
        val sanitized = input.replace(",", "").trim()
        return sanitized.toDoubleOrNull() ?: 0.0
    }
}

