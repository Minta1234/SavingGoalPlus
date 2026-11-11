package com.minta1234.savingsgoal.ui.components

import androidx.compose.ui.res.stringResource
import com.minta1234.savingsgoal.R
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
import com.minta1234.savingsgoal.data.Payment
import com.minta1234.savingsgoal.data.SavingsCalculations
import com.minta1234.savingsgoal.ui.theme.DepositGreen
import com.minta1234.savingsgoal.ui.theme.WithdrawRed
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentAddEditDialog(
    currencySymbol: String,
    initialPayment: Payment? = null,
    todayDepositGoal: String? = null,
    onDismiss: () -> Unit,
    onSave: (amount: Double, date: String, isSaving: Boolean, note: String) -> Unit
) {
    var isSaving by remember { mutableStateOf(initialPayment?.isSaving ?: true) }
    var amountText by remember { mutableStateOf(if (initialPayment != null) SavingsCalculations.formatNumber(initialPayment.amount) else "") }
    var dateStr by remember { mutableStateOf(initialPayment?.date ?: LocalDate.now().toString()) }
    var note by remember { mutableStateOf(initialPayment?.note ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    val amount = SavingsCalculations.parseAmount(amountText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (todayDepositGoal != null) stringResource(R.string.ui_save_today) else if (initialPayment == null) stringResource(R.string.ui_record_transaction) else stringResource(R.string.ui_edit_transaction))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (todayDepositGoal != null) {
                    Text(stringResource(R.string.deposit_into_goal, todayDepositGoal))
                } else {
                    // Deposit vs Withdraw Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = isSaving,
                            onClick = { isSaving = true },
                            label = { Text(stringResource(R.string.ui_deposit)) },
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
                            label = { Text(stringResource(R.string.ui_withdraw)) },
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
                }

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.ui_amount)) },
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

                if (todayDepositGoal == null) {
                    // Date Picker Button
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.transaction_date_value, SavingsCalculations.formatDateForDisplay(dateStr)))
                    }
                }

                // Note Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.ui_description_note_optional)) },
                    placeholder = { Text(stringResource(R.string.ui_e_g_birthday_gift_monthly_allowance)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paymentDate = if (todayDepositGoal != null) LocalDate.now().toString() else dateStr
                    onSave(amount, paymentDate, isSaving, note.trim())
                },
                enabled = amount.isFinite() && amount > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSaving) DepositGreen else WithdrawRed
                )
            ) {
                Text(stringResource(R.string.ui_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ui_cancel))
            }
        }
    )

    if (showDatePicker) {
        val initialMillis = remember(dateStr) {
            SavingsCalculations.parseDate(dateStr)?.atStartOfDay(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli()
                ?: System.currentTimeMillis()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
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
                    Text(stringResource(R.string.ui_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.ui_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

