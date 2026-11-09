package com.example.savingsgoal.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.savingsgoal.data.Goal
import com.example.savingsgoal.ui.components.GoalCard
import com.example.savingsgoal.ui.components.GoalEditDialog
import com.example.savingsgoal.ui.components.TotalSummaryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onGoalClick: (Long) -> Unit,
    onNavigateArchive: () -> Unit,
    onNavigateSettings: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<Goal?>(null) }
    var deletingGoal by remember { mutableStateOf<Goal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Savings Goal",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateArchive) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Archived Goals"
                        )
                    }
                    IconButton(onClick = onNavigateSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        }
    ) { innerPadding ->
        if (uiState.goals.isEmpty() && !uiState.isLoading) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Active Savings Goals",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the + button below to create your first goal and start tracking your savings!",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Total Summary Card
                if (uiState.showTotalGoalsCard) {
                    item {
                        TotalSummaryCard(
                            summary = uiState.summary,
                            currencySymbol = uiState.defaultCurrency
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                items(uiState.goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        defaultCurrency = uiState.defaultCurrency,
                        onClick = { onGoalClick(goal.id) },
                        onEdit = { editingGoal = goal },
                        onToggleArchive = { viewModel.archiveGoal(goal.id) },
                        onDelete = { deletingGoal = goal }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp)) // padding for FAB
                }
            }
        }
    }

    // Add Goal Dialog
    if (showAddDialog) {
        GoalEditDialog(
            defaultCurrency = uiState.defaultCurrency,
            onDismiss = { showAddDialog = false },
            onSave = { name, amount, date, note, currency, bitmap, _ ->
                viewModel.addGoal(name, amount, date, note, currency, bitmap)
                showAddDialog = false
            }
        )
    }

    // Edit Goal Dialog
    if (editingGoal != null) {
        GoalEditDialog(
            initialGoal = editingGoal,
            defaultCurrency = uiState.defaultCurrency,
            onDismiss = { editingGoal = null },
            onSave = { name, amount, date, note, currency, bitmap, removeImage ->
                editingGoal?.let { g ->
                    viewModel.updateGoal(g.id, name, amount, date, note, currency, bitmap, removeImage)
                }
                editingGoal = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (deletingGoal != null) {
        AlertDialog(
            onDismissRequest = { deletingGoal = null },
            title = { Text("Delete Goal?") },
            text = {
                Text("Are you sure you want to delete '${deletingGoal?.name}'? All recorded transactions and history will be permanently deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingGoal?.let { viewModel.deleteGoal(it.id) }
                        deletingGoal = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingGoal = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
