package com.minta1234.savingsgoal.data

import org.junit.Assert.*
import org.junit.Test

class SavingsTablePlanTest {
    @Test fun finalDayExactlyMeetsTarget() {
        val plan = SavingsTablePlan(3000, 10000)
        assertEquals(4, plan.dayCount)
        assertEquals(1000L, plan.amount(4))
        assertEquals(10000L, plan.cumulative(4))
        assertEquals(4000L, plan.saved(setOf(1, 4)))
        assertEquals(plan.target, (1..plan.dayCount).sumOf(plan::amount))
    }
    @Test fun supportsSatangAndDailyAboveTarget() {
        val plan = SavingsTablePlan(10, 31)
        assertEquals(1L, plan.amount(4))
        assertEquals(1, SavingsTablePlan(5000, 1).dayCount)
        assertEquals(1L, SavingsTablePlan(5000, 1).amount(1))
        assertEquals(12345L, SavingsTablePlan.parseAmount("123.45"))
        assertEquals("0.01", SavingsTablePlan.format(1))
    }
    @Test fun rejectsInvalidOrExcessiveInput() {
        listOf("", "0", "-1", "NaN", "Infinity", "1.001", "1e3", "999999999999999999999999", "1000000000").forEach {
            assertNull(it, SavingsTablePlan.parseAmount(it))
        }
        listOf(0L to 100L, 1L to 10001L, 1L to SavingsTablePlan.MAX_AMOUNT).forEach { (daily, target) ->
            assertTrue(runCatching { SavingsTablePlan(daily, target) }.isFailure)
        }
        assertEquals(10000, SavingsTablePlan(1, 10000).dayCount)
    }
    @Test fun exportsExcelFriendlyThaiAndNumericCells() {
        val plan = SavingsTablePlan(30, 50)
        val csv = plan.csv(setOf(2), listOf("วันที่", "ออม", "สะสม", "เหลือ", "สำเร็จ"))
        assertTrue(csv.startsWith("\uFEFF\"วันที่\""))
        assertTrue(csv.endsWith("1,0.30,0.30,0.20,0\r\n2,0.20,0.50,0.00,1\r\n"))
    }
    @Test fun progressShowsHundredthsAndCapsAtOneHundred() {
        val plan = SavingsTablePlan(4000, 6000000)
        assertEquals("0.07%", plan.progressLabel(4000))
        assertEquals(40.0 / 60000, plan.progressFraction(4000), 0.00000001)
        assertEquals("0.00%", plan.progressLabel(0))
        assertEquals("0.00%", plan.progressLabel(-1))
        assertEquals("100.00%", plan.progressLabel(plan.target))
        assertEquals("100.00%", plan.progressLabel(Long.MAX_VALUE))
        assertEquals("0.13%", SavingsTablePlan(1, 800).progressLabel(1))
        assertEquals("100.00%", SavingsTablePlan(SavingsTablePlan.MAX_AMOUNT, SavingsTablePlan.MAX_AMOUNT).progressLabel(SavingsTablePlan.MAX_AMOUNT))
    }
}
