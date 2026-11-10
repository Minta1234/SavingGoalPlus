package com.example.savingsgoal

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Main : NavKey

@Serializable
data class GoalDetail(val goalId: Long, val saveToday: Boolean = false) : NavKey

@Serializable
data object Archive : NavKey

@Serializable
data object Settings : NavKey

@Serializable
data object BackupRestore : NavKey
