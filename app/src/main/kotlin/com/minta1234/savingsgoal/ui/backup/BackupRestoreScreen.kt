package com.minta1234.savingsgoal.ui.backup

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minta1234.savingsgoal.R
import com.minta1234.savingsgoal.data.BackupPayload
import com.minta1234.savingsgoal.data.SavingsRepository
import com.minta1234.savingsgoal.util.rememberFileExport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val repository = remember { SavingsRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportJson = rememberFileExport("application/json", "SavingGoalPlus_Backup.json", repository::exportToJson) {
        snackbarHostState.showSnackbar(it)
    }
    val exportCsv = rememberFileExport("text/csv", "SavingGoalPlus_Goals.csv", repository::exportToCsv) {
        snackbarHostState.showSnackbar(it)
    }

    var isProcessing by remember { mutableStateOf(false) }
    var pendingBackup by remember { mutableStateOf<BackupPayload?>(null) }

    fun handleSelectedUri(uri: Uri) {
        if (isProcessing) return
        isProcessing = true
        scope.launch {
            val backup = try {
                repository.previewBackup(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BackupRestoreScreen", "Could not read the selected backup", e)
                null
            } finally {
                isProcessing = false
            }
            if (backup == null) {
                snackbarHostState.showSnackbar(resources.getString(R.string.backup_invalid))
            } else {
                pendingBackup = backup
            }
        }
    }

    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::handleSelectedUri)
    }
    val fallbackPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::handleSelectedUri)
    }

    fun openPicker() {
        try {
            documentPicker.launch(arrayOf("*/*"))
        } catch (_: android.content.ActivityNotFoundException) {
            try {
                fallbackPicker.launch("*/*")
            } catch (_: android.content.ActivityNotFoundException) {
                scope.launch { snackbarHostState.showSnackbar(resources.getString(R.string.file_picker_unavailable)) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ui_backup_restore), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ui_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Export Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.ui_create_backup), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.ui_backup_your_entire_savings_data_including_all_active_goals_archived_goals_and_transaction_histories),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = exportJson,
                            enabled = !isProcessing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.backup_save_json))
                        }

                        OutlinedButton(
                            onClick = exportCsv,
                            enabled = !isProcessing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.ui_export_csv))
                        }
                    }
                }
            }

            // Restore Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.ui_restore_data), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.ui_restore_data_from_a_json_backup_file_or_from_an_existing_savinggoaldb_sqlite_file_from_the_original_app),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = ::openPicker, enabled = !isProcessing, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.backup_open_file))
                    }
                }
            }
        }
    }

    pendingBackup?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingBackup = null },
            title = { Text(stringResource(R.string.ui_confirm_database_restore)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.backup_preview, backup.goals.size, backup.payments.size))
                    Text(stringResource(R.string.ui_restoring_will_replace_all_current_data_with_the_selected_backup_are_you_sure_you_want_to_proceed))
                }
            },
            confirmButton = {
                Button(onClick = {
                    pendingBackup = null
                    isProcessing = true
                    scope.launch {
                        val message = try {
                            repository.restoreBackup(backup)
                            R.string.restore_success
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e("BackupRestoreScreen", "Restore failed", e)
                            R.string.restore_failed
                        } finally {
                            isProcessing = false
                        }
                        snackbarHostState.showSnackbar(resources.getString(message))
                    }
                }) { Text(stringResource(R.string.ui_restore_now)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingBackup = null }) { Text(stringResource(R.string.ui_cancel)) }
            }
        )
    }
}
