package com.example.savingsgoal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.savingsgoal.data.Payment
import com.example.savingsgoal.data.SavingsCalculations
import com.example.savingsgoal.ui.theme.DepositGreen
import com.example.savingsgoal.ui.theme.WithdrawRed
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentAddEditDialog(
    currencySymbol: String,
    initialPayment: Payment? = null,
    onDismiss: () -> Unit,
    onSave: (amount: Double, date: String, isSaving: Boolean, note: String) -> Unit
) {
    var isSaving by remember { mutableStateOf(initialPayment?.isSaving ?: true) }
    var amountText by remember { mutableStateOf(if (initialPayment != null) SavingsCalculations.formatNumber(initialPayment.amount) else "") }
    var dateStr by remember { mutableStateOf(initialPayment?.date ?: LocalDate.now().toString()) }
    var note by remember { mutableStateOf(initialPayment?.note ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialPayment == null) "Record Transaction" else "Edit Transaction")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Deposit vs Withdraw Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = isSaving,
                        onClick = { isSaving = true },
                        label = { Text("Deposit (+)") },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null, tint = if (isSaving) DepositGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DepositGreen.copy(alpha = 0.15f),
                            selectedLabelColor = DepositGreen
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = !isSaving,
                        onClick = { isSaving = false },
                        label = { Text("Withdraw (-)") },
                        leadingIcon = {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = if (!isSaving) WithdrawRed else MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WithdrawRed.copy(alpha = 0.15f),
                            selectedLabelColor = WithdrawRed
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Text(
                            text = currencySymbol,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSaving) DepositGreen else WithdrawRed
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Date Picker Button
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Date: ${SavingsCalculations.formatDateForDisplay(dateStr)}")
                }

                // Note Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Description / Note (Optional)") },
                    placeholder = { Text("e.g. Birthday gift, monthly allowance") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = SavingsCalculations.parseAmount(amountText)
                    onSave(amount, dateStr, isSaving, note.trim())
                },
                enabled = SavingsCalculations.parseAmount(amountText) > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSaving) DepositGreen else WithdrawRed
                )
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

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
                        dateStr = SavingsCalculations.formatDateForStorage(localDate)
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

