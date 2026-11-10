package com.minta1234.savingsgoal.ui.archive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minta1234.savingsgoal.data.Goal
import com.minta1234.savingsgoal.data.SavingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ArchiveUiState(
    val archivedGoals: List<Goal> = emptyList(),
    val defaultCurrency: String = "$",
    val isLoading: Boolean = true
)

class ArchiveViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SavingsRepository(application)

    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.dataRevision.collectLatest {
                loadData()
            }
        }
    }

    private suspend fun loadData() {
        val archived = repository.getArchivedGoals()
        val defaultCurrency = repository.getDefaultCurrency()
        _uiState.value = ArchiveUiState(
            archivedGoals = archived,
            defaultCurrency = defaultCurrency,
            isLoading = false
        )
    }

    fun unarchiveGoal(id: Long) {
        viewModelScope.launch {
            repository.setGoalFolder(id, "MAIN")
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            repository.deleteGoal(id)
        }
    }
}

