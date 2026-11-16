package com.minta1234.savingsgoal.data

import org.junit.Assert.*
import org.junit.Test

class SavingsTableEntryTest {
    @Test fun identicalPlansHaveIndependentIdentityAndProgress() {
        val plan = SavingsTablePlan(3000, 10000)
        val first = SavingsTableEntry(name = "Travel", plan = plan)
        val second = SavingsTableEntry(name = "Emergency", plan = plan)
        assertNotEquals(first.id, second.id)
        val updated = first.withDay(1, true).withDay(4, true)
        assertEquals(4000L, updated.plan.saved(updated.completed))
        assertTrue(second.completed.isEmpty())
        assertEquals(setOf(4), updated.withDay(1, false).completed)
        assertEquals(listOf(second), listOf(updated, second).filterNot { it.id == first.id })
    }

    @Test fun renamingKeepsIdentityAndSavedDays() {
        val entry = SavingsTableEntry(name = "Old", plan = SavingsTablePlan(100, 500), completed = setOf(1, 3))
        val renamed = entry.withDetails("  New name  ", entry.plan)
        assertEquals(entry.id, renamed.id)
        assertEquals("New name", renamed.name)
        assertEquals(entry.completed, renamed.completed)
    }

    @Test fun replacingPlanClearsOnlyItsOwnChecks() {
        val first = SavingsTableEntry(name = "First", plan = SavingsTablePlan(100, 500), completed = setOf(1))
        val other = SavingsTableEntry(name = "Other", plan = first.plan, completed = setOf(2))
        val replacement = first.withDetails("First", SavingsTablePlan(200, 800))
        val updated = listOf(first, other).map { if (it.id == replacement.id) replacement else it }
        assertTrue(updated[0].completed.isEmpty())
        assertEquals(setOf(2), updated[1].completed)
        assertEquals(500L, updated[1].plan.target)
    }
}
