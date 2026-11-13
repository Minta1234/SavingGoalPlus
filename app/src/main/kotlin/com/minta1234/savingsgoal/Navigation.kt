package com.minta1234.savingsgoal

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModelFactory = AndroidViewModelFactory.getInstance(application)
    val backStack = rememberNavBackStack(Main)

    val safeOnBack: () -> Unit = {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        } else {
            context.findActivity()?.finish()
        }
    }

    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        onBack = safeOnBack,
        entryProvider =
            entryProvider {
                entry<Main> {
                    MainScreen(
                        viewModel = viewModel(factory = viewModelFactory),
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
                        onBack = safeOnBack
                    )
                }

                entry<Archive> {
                    ArchiveScreen(
                        viewModel = viewModel(factory = viewModelFactory),
                        onBack = safeOnBack,
                        onGoalClick = { goalId -> backStack.add(GoalDetail(goalId)) }
                    )
                }

                entry<Settings> {
                    SettingsScreen(
                        viewModel = viewModel(factory = viewModelFactory),
                        onBack = safeOnBack,
                        onNavigateBackup = { backStack.add(BackupRestore) }
                    )
                }

                entry<BackupRestore> {
                    BackupRestoreScreen(
                        onBack = safeOnBack
                    )
                }
            }
    )
}
