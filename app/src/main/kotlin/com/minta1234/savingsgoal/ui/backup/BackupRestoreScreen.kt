package com.minta1234.savingsgoal.ui.backup

import android.net.Uri
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
import androidx.compose.material.icons.filled.Storage
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
import com.minta1234.savingsgoal.data.SavingsGoalDbHelper
import com.minta1234.savingsgoal.data.SavingsRepository
import com.minta1234.savingsgoal.util.rememberFileExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

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
    var confirmRestoreDialog by remember { mutableStateOf(false) }
    var pendingRestoreFile by remember { mutableStateOf<File?>(null) }

    fun handleSelectedUri(uri: Uri) {
        scope.launch(Dispatchers.IO) {
            try {
                isProcessing = true
                val cacheFile = File(context.cacheDir, "pending_restore_upload.bin")
                context.contentResolver.openInputStream(uri)?.use { inStream ->
                    FileOutputStream(cacheFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                pendingRestoreFile = cacheFile
                isProcessing = false
                confirmRestoreDialog = true
            } catch (e: Exception) {
                isProcessing = false
                android.util.Log.e("BackupRestoreScreen", "Failed to cache selected file", e)
                snackbarHostState.showSnackbar(resources.getString(R.string.restore_failed))
            }
        }
    }

    // OpenDocument contract opens document / file manager (Files, Drive, Downloads, SD card)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedUri(uri)
        }
    }

    // Fallback GetContent launcher
    val getContentFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedUri(uri)
        }
    }

    fun openPicker(mimeTypes: Array<String>) {
        try {
            documentPickerLauncher.launch(mimeTypes)
        } catch (_: Exception) {
            try {
                getContentFallbackLauncher.launch("*/*")
            } catch (_: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar(resources.getString(R.string.file_picker_unavailable))
                }
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.ui_export_json))
                        }

                        OutlinedButton(
                            onClick = exportCsv,
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { openPicker(arrayOf("application/json", "text/*", "*/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.ui_restore_json))
                        }

                        OutlinedButton(
                            onClick = { openPicker(arrayOf("application/octet-stream", "application/x-sqlite3", "*/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.ui_restore_db))
                        }
                    }
                }
            }
        }
    }

    // Confirmation Alert
    if (confirmRestoreDialog) {
        AlertDialog(
            onDismissRequest = { confirmRestoreDialog = false },
            title = { Text(stringResource(R.string.ui_confirm_database_restore)) },
            text = { Text(stringResource(R.string.ui_restoring_will_replace_all_current_data_with_the_selected_backup_are_you_sure_you_want_to_proceed)) },
            confirmButton = {
                Button(
                    onClick = {
                        val file = pendingRestoreFile
                        confirmRestoreDialog = false

                        if (file != null && file.exists()) {
                            isProcessing = true
                            scope.launch {
                                var success = false
                                withContext(Dispatchers.IO) {
                                    try {
                                        val isSqlite = isSqliteDatabase(file)
                                        if (isSqlite) {
                                            success = SavingsGoalDbHelper.getInstance(context).importDatabaseFile(context, file)
                                            if (success) {
                                                repository.notifyChanged()
                                            }
                                        } else {
                                            val content = file.readText(Charsets.UTF_8)
                                            success = repository.importFromJson(content)
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("BackupRestoreScreen", "Restore failed", e)
                                        success = false
                                    } finally {
                                        file.delete()
                                    }
                                }
                                isProcessing = false
                                if (success) {
                                    snackbarHostState.showSnackbar(resources.getString(R.string.restore_success))
                                } else {
                                    snackbarHostState.showSnackbar(resources.getString(R.string.restore_failed))
                                }
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.ui_restore_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestoreDialog = false }) {
                    Text(stringResource(R.string.ui_cancel))
                }
            }
        )
    }
}

private fun isSqliteDatabase(file: File): Boolean {
    return try {
        if (!file.exists() || file.length() < 16) return false
        val header = ByteArray(16)
        file.inputStream().use { it.read(header) }
        header.contentEquals("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII))
    } catch (_: Exception) {
        false
    }
}
