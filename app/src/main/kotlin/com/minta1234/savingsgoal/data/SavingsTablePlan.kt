package com.minta1234.savingsgoal.data

import java.math.BigDecimal
import java.math.RoundingMode

/** Amounts use satang to avoid floating-point rounding in the final installment. */
data class SavingsTablePlan(val daily: Long, val target: Long) {
    init {
        require(daily > 0 && target > 0)
        require(target <= MAX_AMOUNT && daily <= MAX_AMOUNT)
        require((target - 1) / daily + 1 <= MAX_DAYS)
    }

    val dayCount: Int get() = ((target - 1) / daily + 1).toInt()
    fun amount(day: Int): Long {
        require(day in 1..dayCount)
        return minOf(daily, target - (day - 1).toLong() * daily)
    }
    fun cumulative(day: Int): Long {
        require(day in 1..dayCount)
        return minOf(target, day.toLong() * daily)
    }
    fun saved(completed: Set<Int>): Long = completed.filter { it in 1..dayCount }.sumOf(::amount)

    fun progressFraction(saved: Long): Double = saved.coerceIn(0, target).toDouble() / target
    fun progressLabel(saved: Long): String {
        // Integer hundredths of a percent, rounded half up; amounts are bounded by MAX_AMOUNT.
        val hundredths = (saved.coerceIn(0, target) * 10_000 + target / 2) / target
        return "${format(hundredths)}%"
    }

    /** UTF-8 BOM makes Thai column headings readable in Excel. Values are numeric cells. */
    fun csv(completed: Set<Int>, headings: List<String>): String = buildString {
        append('\uFEFF')
        append(headings.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" })
        append("\r\n")
        for (day in 1..dayCount) {
            append("$day,${format(amount(day))},${format(cumulative(day))},${format(target - cumulative(day))},${if (day in completed) 1 else 0}\r\n")
        }
    }

    companion object {
        const val MAX_DAYS = 10_000
        const val MAX_AMOUNT = 99_999_999_999L
        fun parseAmount(text: String): Long? = try {
            if (!text.trim().matches(Regex("[0-9]+(\\.[0-9]{1,2})?"))) null
            else BigDecimal(text.trim()).movePointRight(2).longValueExact().takeIf { it in 1..MAX_AMOUNT }
        } catch (_: ArithmeticException) { null } catch (_: NumberFormatException) { null }
        fun format(satang: Long): String = BigDecimal.valueOf(satang, 2).setScale(2, RoundingMode.UNNECESSARY).toPlainString()
    }
}
