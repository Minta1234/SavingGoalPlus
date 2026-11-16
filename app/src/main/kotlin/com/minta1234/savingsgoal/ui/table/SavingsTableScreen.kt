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
import com.minta1234.savingsgoal.data.SavingsTableEntry
import com.minta1234.savingsgoal.data.SavingsTablePlan
import com.minta1234.savingsgoal.ui.components.FunctionMenu
import com.minta1234.savingsgoal.util.rememberFileExport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsTableScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("savings_table", Context.MODE_PRIVATE) }
    val legacyName = stringResource(R.string.table_title)
    val store = remember { SavingsTableStore(prefs) }
    var tables by remember { mutableStateOf(store.load(legacyName)) }
    var selectedId by rememberSaveable { mutableStateOf(tables.firstOrNull()?.id) }
    val selected = tables.firstOrNull { it.id == selectedId }
    val plan = selected?.plan
    val completed = selected?.completed.orEmpty()
    var name by rememberSaveable { mutableStateOf(selected?.name ?: "") }
    var daily by rememberSaveable { mutableStateOf(plan?.let { SavingsTablePlan.format(it.daily) } ?: "") }
    var target by rememberSaveable { mutableStateOf(plan?.let { SavingsTablePlan.format(it.target) } ?: "") }
    var error by rememberSaveable { mutableStateOf(false) }
    var pending by remember { mutableStateOf<SavingsTableEntry?>(null) }
    var deleting by remember { mutableStateOf<SavingsTableEntry?>(null) }
    var message by remember { mutableStateOf("") }
    var chooser by remember { mutableStateOf(false) }
    fun select(table: SavingsTableEntry?) {
        selectedId = table?.id
        name = table?.name.orEmpty()
        daily = table?.plan?.let { SavingsTablePlan.format(it.daily) }.orEmpty()
        target = table?.plan?.let { SavingsTablePlan.format(it.target) }.orEmpty()
        error = false
        message = ""
    }
    fun persist(next: List<SavingsTableEntry>) {
        store.save(next)
        tables = next
    }
    fun save(entry: SavingsTableEntry) {
        persist(if (tables.any { it.id == entry.id }) tables.map { if (it.id == entry.id) entry else it } else tables + entry)
        select(entry)
    }
    val headings = listOf(R.string.table_day, R.string.table_daily, R.string.table_cumulative, R.string.table_remaining, R.string.table_done)
        .map { stringResource(it) }
    val exportPlan = plan
    val exportCompleted = completed
    val export = rememberFileExport("text/csv", "savings-table-${selected?.id ?: "new"}.csv", {
        requireNotNull(exportPlan).csv(exportCompleted, headings)
    }, { message = it })

    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.table_title)) }, navigationIcon = {
            FunctionMenu(onHome = onBack, onTable = {})
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                if (tables.isNotEmpty()) {
                    Box {
                        TextButton(onClick = { chooser = true }) {
                            Text(selected?.name ?: stringResource(R.string.table_select))
                        }
                        DropdownMenu(expanded = chooser, onDismissRequest = { chooser = false }) {
                            tables.forEach { table ->
                                DropdownMenuItem(text = { Text(table.name) }, onClick = { select(table); chooser = false })
                            }
                        }
                    }
                }
                TextButton(onClick = { select(null) }) { Text(stringResource(R.string.table_new)) }
                Text(stringResource(R.string.table_description))
                OutlinedTextField(value = name, onValueChange = { name = it; error = false }, label = { Text(stringResource(R.string.table_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = daily, onValueChange = { daily = it; error = false }, label = { Text(stringResource(R.string.table_daily)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = target, onValueChange = { target = it; error = false }, label = { Text(stringResource(R.string.table_target)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (error) Text(stringResource(R.string.table_invalid), color = MaterialTheme.colorScheme.error)
                Button(onClick = {
                    val d = SavingsTablePlan.parseAmount(daily)
                    val t = SavingsTablePlan.parseAmount(target)
                    val next = if (d != null && t != null) runCatching { SavingsTablePlan(d, t) }.getOrNull() else null
                    error = next == null || name.isBlank()
                    if (!error && next != null) {
                        val entry = selected?.withDetails(name, next) ?: SavingsTableEntry(name = name.trim(), plan = next)
                        if (selected != null && next != plan && completed.isNotEmpty()) pending = entry
                        else save(entry)
                    }
                }) { Text(stringResource(if (selected == null) R.string.table_create else R.string.table_save)) }
                if (selected != null) {
                    TextButton(onClick = { deleting = selected }) { Text(stringResource(R.string.table_delete), color = MaterialTheme.colorScheme.error) }
                }
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
                            tables.firstOrNull { it.id == selectedId }?.let { entry ->
                                val updated = entry.withDay(day, checked)
                                persist(tables.map { if (it.id == updated.id) updated else it })
                            }
                        })
                    }
                    HorizontalDivider()
                }
            }
        }
    }
    if (pending != null) AlertDialog(onDismissRequest = { pending = null },
        title = { Text(stringResource(R.string.table_replace)) }, text = { Text(stringResource(R.string.table_replace_warning)) },
        confirmButton = { TextButton(onClick = { pending?.let { save(it) }; pending = null }) { Text(stringResource(R.string.table_save)) } },
        dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.ui_cancel)) } })
    if (deleting != null) AlertDialog(onDismissRequest = { deleting = null },
        title = { Text(stringResource(R.string.table_delete)) }, text = { Text(stringResource(R.string.table_delete_warning, deleting?.name.orEmpty())) },
        confirmButton = { TextButton(onClick = {
            val remaining = tables.filterNot { it.id == deleting?.id }
            persist(remaining)
            select(remaining.firstOrNull())
            deleting = null
        }) { Text(stringResource(R.string.table_delete)) } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.ui_cancel)) } })
}
