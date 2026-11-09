package com.example.savingsgoal

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.savingsgoal.ui.archive.ArchiveScreen
import com.example.savingsgoal.ui.backup.BackupRestoreScreen
import com.example.savingsgoal.ui.detail.GoalDetailScreen
import com.example.savingsgoal.ui.main.MainScreen
import com.example.savingsgoal.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Main)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<Main> {
                    MainScreen(
                        onGoalClick = { goalId -> backStack.add(GoalDetail(goalId)) },
                        onNavigateArchive = { backStack.add(Archive) },
                        onNavigateSettings = { backStack.add(Settings) }
                    )
                }

                entry<GoalDetail> { key ->
                    GoalDetailScreen(
                        goalId = key.goalId,
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
