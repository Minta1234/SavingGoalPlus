package com.minta1234.savingsgoal.data

import java.util.UUID

/** A separately named plan with its own saved-day history. */
data class SavingsTableEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val plan: SavingsTablePlan,
    val completed: Set<Int> = emptySet()
) {
    fun withDetails(name: String, plan: SavingsTablePlan): SavingsTableEntry = copy(
        name = name.trim(), plan = plan,
        completed = if (plan == this.plan) completed else emptySet()
    )
    fun withDay(day: Int, saved: Boolean): SavingsTableEntry {
        require(day in 1..plan.dayCount)
        return copy(completed = if (saved) completed + day else completed - day)
    }
}
