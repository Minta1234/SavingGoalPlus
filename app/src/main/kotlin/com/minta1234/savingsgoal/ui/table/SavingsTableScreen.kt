package com.minta1234.savingsgoal.ui.table

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.minta1234.savingsgoal.R
import com.minta1234.savingsgoal.data.SavingsTablePlan
import com.minta1234.savingsgoal.ui.components.FunctionMenu
import com.minta1234.savingsgoal.util.rememberFileExport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsTableScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("savings_table", Context.MODE_PRIVATE) }
    var plan by remember {
        mutableStateOf(runCatching {
            SavingsTablePlan(prefs.getLong("daily", 0), prefs.getLong("target", 0))
        }.getOrNull())
    }
    var completed by remember {
        mutableStateOf(prefs.getStringSet("completed", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet())
    }
    var daily by rememberSaveable { mutableStateOf(plan?.let { SavingsTablePlan.format(it.daily) } ?: "") }
    var target by rememberSaveable { mutableStateOf(plan?.let { SavingsTablePlan.format(it.target) } ?: "") }
    var error by rememberSaveable { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<SavingsTablePlan?>(null) }
    var message by remember { mutableStateOf("") }
    fun save(newPlan: SavingsTablePlan, days: Set<Int>) {
        prefs.edit().putLong("daily", newPlan.daily).putLong("target", newPlan.target)
            .putStringSet("completed", days.map { it.toString() }.toSet()).apply()
        plan = newPlan
        completed = days
    }
    val headings = listOf(R.string.table_day, R.string.table_daily, R.string.table_cumulative, R.string.table_remaining, R.string.table_done)
        .map { stringResource(it) }
    val exportPlan = plan
    val exportCompleted = completed
    val export = rememberFileExport("text/csv", "savings-table.csv", {
        requireNotNull(exportPlan).csv(exportCompleted, headings)
    }, { message = it })

    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.table_title)) }, navigationIcon = {
            FunctionMenu(onHome = onBack, onTable = {})
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(stringResource(R.string.table_description))
                OutlinedTextField(value = daily, onValueChange = { daily = it; error = false }, label = { Text(stringResource(R.string.table_daily)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = target, onValueChange = { target = it; error = false }, label = { Text(stringResource(R.string.table_target)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (error) Text(stringResource(R.string.table_invalid), color = MaterialTheme.colorScheme.error)
                Button(onClick = {
                    val d = SavingsTablePlan.parseAmount(daily)
                    val t = SavingsTablePlan.parseAmount(target)
                    val next = if (d != null && t != null) runCatching { SavingsTablePlan(d, t) }.getOrNull() else null
                    error = next == null
                    if (next != null && next != plan) {
                        if (completed.isNotEmpty()) { pending = next; confirm = true }
                        else save(next, emptySet())
                    }
                }) { Text(stringResource(R.string.table_create)) }
            }
            plan?.let { active ->
                item {
                    Text(stringResource(R.string.table_summary, active.dayCount, SavingsTablePlan.format(active.saved(completed)), SavingsTablePlan.format(active.target)))
                    Text(stringResource(R.string.table_independent), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = export) { Text(stringResource(R.string.table_export)) }
                    if (message.isNotEmpty()) Text(message)
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.table_day), Modifier.weight(0.6f))
                        Text(stringResource(R.string.table_daily), Modifier.weight(1f))
                        Text(stringResource(R.string.table_cumulative), Modifier.weight(1f))
                        Text(stringResource(R.string.table_done), Modifier.width(48.dp))
                    }
                    HorizontalDivider()
                }
                items((1..active.dayCount).toList(), key = { it }) { day ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(day.toString(), Modifier.weight(0.6f))
                        Text(SavingsTablePlan.format(active.amount(day)), Modifier.weight(1f))
                        Text(SavingsTablePlan.format(active.cumulative(day)), Modifier.weight(1f))
                        Checkbox(checked = day in completed, onCheckedChange = { checked ->
                            save(active, if (checked) completed + day else completed - day)
                        })
                    }
                    HorizontalDivider()
                }
            }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false; pending = null },
        title = { Text(stringResource(R.string.table_replace)) }, text = { Text(stringResource(R.string.table_replace_warning)) },
        confirmButton = { TextButton(onClick = { pending?.let { save(it, emptySet()) }; confirm = false; pending = null }) { Text(stringResource(R.string.table_create)) } },
        dismissButton = { TextButton(onClick = { confirm = false; pending = null }) { Text(stringResource(R.string.ui_cancel)) } })
}
