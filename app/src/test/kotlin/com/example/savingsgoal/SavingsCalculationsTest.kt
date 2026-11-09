package com.example.savingsgoal

import com.example.savingsgoal.data.Goal
import com.example.savingsgoal.data.SavingsCalculations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SavingsCalculationsTest {

    @Test
    fun testGoalProgressCalculations() {
        val goal = Goal(
            name = "Vacation",
            amount = 1000.0,
            totalSaved = 400.0
        )

        assertEquals(0.4f, goal.progressFraction, 0.001f)
        assertEquals(40, goal.progressPercent)
        assertEquals(600.0, goal.remainingAmount, 0.001)
        assertEquals(0.0, goal.excessAmount, 0.001)
        assertFalse(goal.isReached)
    }

    @Test
    fun testGoalReachedCalculations() {
        val goal = Goal(
            name = "Emergency Fund",
            amount = 500.0,
            totalSaved = 750.0
        )

        assertEquals(1.0f, goal.progressFraction, 0.001f)
        assertEquals(100, goal.progressPercent)
        assertEquals(0.0, goal.remainingAmount, 0.001)
        assertEquals(250.0, goal.excessAmount, 0.001)
        assertTrue(goal.isReached)
    }

    @Test
    fun testRatePlanCalculation() {
        val today = LocalDate.now()
        val futureDate = today.plusDays(100).toString()

        val plan = SavingsCalculations.calculateRatePlan(
            targetDateStr = futureDate,
            remainingAmount = 1000.0,
            isReached = false
        )

        assertNotNull(plan)
        assertFalse(plan!!.isExpired)
        assertEquals(10.0, plan.dailyAmount, 0.01)
        assertTrue(plan.weeklyAmount > 0.0)
        assertTrue(plan.monthlyAmount > 0.0)
    }

    @Test
    fun testCurrencyFormatting() {
        val formatted = SavingsCalculations.formatCurrency(1234.5, "$")
        assertEquals("$ 1,234.50", formatted)
    }

    @Test
    fun testNumberParsing() {
        assertEquals(1500.75, SavingsCalculations.parseAmount("1,500.75"), 0.001)
        assertEquals(250.0, SavingsCalculations.parseAmount("250"), 0.001)
        assertEquals(0.0, SavingsCalculations.parseAmount(""), 0.001)
    }
}

