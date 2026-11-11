package com.minta1234.savingsgoal.ui.main

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minta1234.savingsgoal.data.Goal
import com.minta1234.savingsgoal.data.GoalSortOrder
import com.minta1234.savingsgoal.data.SavingsRepository
import com.minta1234.savingsgoal.data.TotalSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class MainUiState(
    val goals: List<Goal> = emptyList(),
    val summary: TotalSummary = TotalSummary(),
    val defaultCurrency: String = "$",
    val showTotalGoalsCard: Boolean = true,
    val sortOrder: GoalSortOrder = GoalSortOrder.HIGHEST_TO_LOWEST,
    val isLoading: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SavingsRepository(application)
    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var rawGoals: List<Goal> = emptyList()

    init {
        viewModelScope.launch {
            repository.dataRevision.collectLatest {
                loadData()
            }
        }
    }

    private fun getSavedSortOrder(): GoalSortOrder {
        val savedName = prefs.getString("goals_sort_order", GoalSortOrder.HIGHEST_TO_LOWEST.name)
        return try {
            GoalSortOrder.valueOf(savedName ?: GoalSortOrder.HIGHEST_TO_LOWEST.name)
        } catch (_: Exception) {
            GoalSortOrder.HIGHEST_TO_LOWEST
        }
    }

    private fun sortGoals(goals: List<Goal>, order: GoalSortOrder): List<Goal> {
        return when (order) {
            GoalSortOrder.HIGHEST_TO_LOWEST -> goals.sortedWith(
                compareByDescending<Goal> { it.totalSaved }.thenByDescending { it.amount }
            )
            GoalSortOrder.LOWEST_TO_HIGHEST -> goals.sortedWith(
                compareBy<Goal> { it.totalSaved }.thenBy { it.amount }
            )
            GoalSortOrder.NEWEST_FIRST -> goals.sortedByDescending { it.id }
            GoalSortOrder.OLDEST_FIRST -> goals.sortedBy { it.id }
        }
    }

    fun setSortOrder(order: GoalSortOrder) {
        prefs.edit().putString("goals_sort_order", order.name).apply()
        _uiState.value = _uiState.value.copy(
            sortOrder = order,
            goals = sortGoals(rawGoals, order)
        )
    }

    private suspend fun loadData() {
        try {
            rawGoals = repository.getActiveGoals()
            val summary = repository.summarizeGoals(rawGoals)
            val defaultCurrency = repository.getDefaultCurrency()
            val showTotal = repository.getShowTotalGoalsCard()
            val currentSort = getSavedSortOrder()

            _uiState.value = MainUiState(
                goals = sortGoals(rawGoals, currentSort),
                summary = summary,
                defaultCurrency = defaultCurrency,
                showTotalGoalsCard = showTotal,
                sortOrder = currentSort,
                isLoading = false
            )
        } catch (_: Exception) {
            _uiState.value = MainUiState(
                goals = emptyList(),
                summary = TotalSummary(),
                defaultCurrency = "$",
                showTotalGoalsCard = true,
                sortOrder = GoalSortOrder.HIGHEST_TO_LOWEST,
                isLoading = false
            )
        }
    }

    fun addGoal(
        name: String,
        amount: Double,
        date: String?,
        note: String,
        currency: String?,
        bitmap: Bitmap?
    ) {
        viewModelScope.launch {
            repository.insertGoal(name, amount, date, note, currency, bitmap)
        }
    }

    fun updateGoal(
        id: Long,
        name: String,
        amount: Double,
        date: String?,
        note: String,
        currency: String?,
        bitmap: Bitmap?,
        removeImage: Boolean
    ) {
        viewModelScope.launch {
            repository.updateGoal(id, name, amount, date, note, currency, bitmap, removeImage)
        }
    }

    fun archiveGoal(id: Long) {
        viewModelScope.launch {
            repository.setGoalFolder(id, "ARCHIVE")
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            repository.deleteGoal(id)
        }
    }
}
