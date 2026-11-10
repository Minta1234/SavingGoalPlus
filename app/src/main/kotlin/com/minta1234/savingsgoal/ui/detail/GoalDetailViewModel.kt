package com.minta1234.savingsgoal.ui.detail

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minta1234.savingsgoal.data.Goal
import com.minta1234.savingsgoal.data.Payment
import com.minta1234.savingsgoal.data.SavingsCalculations
import com.minta1234.savingsgoal.data.SavingsRatePlan
import com.minta1234.savingsgoal.data.SavingsRepository
import com.minta1234.savingsgoal.data.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class GoalDetailUiState(
    val goal: Goal? = null,
    val payments: List<Payment> = emptyList(),
    val ratePlan: SavingsRatePlan? = null,
    val defaultCurrency: String = "$",
    val toSaveOption: Int = 1, // 1 = Weekly, 2 = Bi-Weekly
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val isLoading: Boolean = true
)

class GoalDetailViewModel(
    application: Application,
    private val goalId: Long
) : AndroidViewModel(application) {

    private val repository = SavingsRepository(application)

    private val selectedSort = MutableStateFlow(SortOrder.NEWEST_FIRST)
    private val _uiState = MutableStateFlow(GoalDetailUiState())
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.dataRevision.combine(selectedSort) { _, sort -> sort }
                .collectLatest { sort -> loadData(sort) }
        }
    }

    private suspend fun loadData(currentSort: SortOrder) {
        val goal = repository.getGoalById(goalId)
        val payments = repository.getPaymentsForGoal(goalId, currentSort)
        val defaultCurrency = repository.getDefaultCurrency()
        val toSaveOption = repository.getToSaveOption()

        val ratePlan = if (goal != null) {
            SavingsCalculations.calculateRatePlan(goal.date, goal.remainingAmount, goal.isReached)
        } else null

        _uiState.value = GoalDetailUiState(
            goal = goal,
            payments = payments,
            ratePlan = ratePlan,
            defaultCurrency = defaultCurrency,
            toSaveOption = toSaveOption,
            sortOrder = currentSort,
            isLoading = false
        )
    }

    fun setSortOrder(sortOrder: SortOrder) {
        selectedSort.value = sortOrder
    }

    fun addPayment(amount: Double, date: String, isSaving: Boolean, note: String) {
        viewModelScope.launch {
            repository.insertPayment(goalId, amount, date, isSaving, note)
        }
    }

    fun updatePayment(id: Long, amount: Double, date: String, isSaving: Boolean, note: String) {
        viewModelScope.launch {
            repository.updatePayment(id, amount, date, isSaving, note)
        }
    }

    fun deletePayment(id: Long) {
        viewModelScope.launch {
            repository.deletePayment(id)
        }
    }

    fun updateGoal(
        name: String,
        amount: Double,
        date: String?,
        note: String,
        currency: String?,
        bitmap: Bitmap?,
        removeImage: Boolean
    ) {
        viewModelScope.launch {
            repository.updateGoal(goalId, name, amount, date, note, currency, bitmap, removeImage)
        }
    }

    fun updateTargetDate(newDate: String?) {
        val currentGoal = _uiState.value.goal ?: return
        viewModelScope.launch {
            repository.updateGoal(
                id = currentGoal.id,
                name = currentGoal.name,
                amount = currentGoal.amount,
                date = newDate,
                note = currentGoal.note,
                currency = currentGoal.currency
            )
        }
    }
}

