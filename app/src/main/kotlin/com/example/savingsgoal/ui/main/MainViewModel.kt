package com.example.savingsgoal.ui.main

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingsgoal.data.Goal
import com.example.savingsgoal.data.SavingsRepository
import com.example.savingsgoal.data.TotalSummary
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
    val isLoading: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SavingsRepository(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.dataRevision.collectLatest {
                loadData()
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            val goals = repository.getActiveGoals()
            val summary = repository.getTotalSummary()
            val defaultCurrency = repository.getDefaultCurrency()
            val showTotal = repository.getShowTotalGoalsCard()
            _uiState.value = MainUiState(
                goals = goals,
                summary = summary,
                defaultCurrency = defaultCurrency,
                showTotalGoalsCard = showTotal,
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

