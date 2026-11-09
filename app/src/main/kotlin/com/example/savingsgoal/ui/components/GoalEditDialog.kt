package com.example.savingsgoal.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.savingsgoal.data.Goal
import com.example.savingsgoal.data.SavingsCalculations
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditDialog(
    initialGoal: Goal? = null,
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Double, date: String?, note: String, currency: String?, bitmap: Bitmap?, removeImage: Boolean) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialGoal?.name ?: "") }
    var amountText by remember { mutableStateOf(if (initialGoal != null) SavingsCalculations.formatNumber(initialGoal.amount) else "") }
    var targetDate by remember { mutableStateOf(initialGoal?.date) }
    var note by remember { mutableStateOf(initialGoal?.note ?: "") }
    var customCurrency by remember { mutableStateOf(if (initialGoal?.currency != "null") initialGoal?.currency ?: "" else "") }

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var removeImage by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val initialImageBitmap = remember(initialGoal?.imagePath) {
        initialGoal?.imagePath?.let { path ->
            val file = File(path)
            if (file.exists()) BitmapFactory.decodeFile(path) else null
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    selectedBitmap = BitmapFactory.decodeStream(stream)
                    removeImage = false
                }
            } catch (_: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialGoal == null) "New Savings Goal" else "Edit Goal")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Image Picker / Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayBitmap = if (removeImage) null else (selectedBitmap ?: initialImageBitmap)

                    if (displayBitmap != null) {
                        Image(
                            bitmap = displayBitmap.asImageBitmap(),
                            contentDescription = "Goal Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(onClick = {
                            selectedBitmap = null
                            removeImage = true
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove photo", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = "Add Photo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text(if (displayBitmap != null) "Change Photo" else "Select Photo")
                    }
                }

                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name") },
                    placeholder = { Text("e.g. Dream Vacation, New Car") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Target Amount") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Text(
                            text = if (customCurrency.isNotBlank()) customCurrency else defaultCurrency,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Target Date Selector
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (!targetDate.isNullOrBlank()) "Target Date: ${SavingsCalculations.formatDateForDisplay(targetDate)}" else "Set Target Date (Optional)"
                    )
                    if (!targetDate.isNullOrBlank()) {
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { targetDate = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Date")
                        }
                    }
                }

                // Custom Currency (Optional)
                OutlinedTextField(
                    value = customCurrency,
                    onValueChange = { customCurrency = it },
                    label = { Text("Custom Currency Symbol (Optional)") },
                    placeholder = { Text("Default: $defaultCurrency") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Notes Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes / Memo (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = SavingsCalculations.parseAmount(amountText)
                    val finalCurrency = customCurrency.trim().ifBlank { null }
                    onSave(
                        name.trim(),
                        parsedAmount,
                        targetDate,
                        note.trim(),
                        finalCurrency,
                        selectedBitmap,
                        removeImage
                    )
                },
                enabled = name.isNotBlank() && SavingsCalculations.parseAmount(amountText) > 0.0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val localDate = Instant.ofEpochMilli(selectedMillis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        targetDate = SavingsCalculations.formatDateForStorage(localDate)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

