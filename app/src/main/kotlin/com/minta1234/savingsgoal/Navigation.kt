package com.minta1234.savingsgoal

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.minta1234.savingsgoal.ui.archive.ArchiveScreen
import com.minta1234.savingsgoal.ui.backup.BackupRestoreScreen
import com.minta1234.savingsgoal.ui.detail.GoalDetailScreen
import com.minta1234.savingsgoal.ui.main.MainScreen
import com.minta1234.savingsgoal.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Main)

    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<Main> {
                    MainScreen(
                        onGoalClick = { goalId -> backStack.add(GoalDetail(goalId)) },
                        onSaveToday = { goalId -> backStack.add(GoalDetail(goalId, saveToday = true)) },
                        onNavigateArchive = { backStack.add(Archive) },
                        onNavigateSettings = { backStack.add(Settings) }
                    )
                }

                entry<GoalDetail> { key ->
                    GoalDetailScreen(
                        goalId = key.goalId,
                        saveToday = key.saveToday,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                entry<Archive> {
                    ArchiveScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onGoalClick = { goalId -> backStack.add(GoalDetail(goalId)) }
                    )
                }

                entry<Settings> {
                    SettingsScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onNavigateBackup = { backStack.add(BackupRestore) }
                    )
                }

                entry<BackupRestore> {
                    BackupRestoreScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
            }
    )
}
