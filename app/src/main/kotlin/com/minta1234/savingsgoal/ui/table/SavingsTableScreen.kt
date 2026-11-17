package com.minta1234.savingsgoal.ui.table

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.minta1234.savingsgoal.R
import com.minta1234.savingsgoal.data.SavingsTableEntry
import com.minta1234.savingsgoal.data.SavingsTablePlan
import com.minta1234.savingsgoal.ui.components.CircularGoalGauge
import com.minta1234.savingsgoal.ui.components.FunctionMenu
import com.minta1234.savingsgoal.ui.theme.ProgressAmber
import com.minta1234.savingsgoal.ui.theme.ProgressCyan
import com.minta1234.savingsgoal.ui.theme.ProgressEmerald
import com.minta1234.savingsgoal.util.rememberFileExport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsTableScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("savings_table", Context.MODE_PRIVATE) }
    val minimumCellWidth = (96f * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp
    val legacyName = stringResource(R.string.table_title)
    val store = remember { SavingsTableStore(prefs) }
    var tables by remember { mutableStateOf(store.load(legacyName)) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = tables.firstOrNull { it.id == selectedId }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var daily by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    var pending by remember { mutableStateOf<SavingsTableEntry?>(null) }
    var deleting by remember { mutableStateOf<SavingsTableEntry?>(null) }
    var message by remember { mutableStateOf("") }
    fun edit(entry: SavingsTableEntry?) {
        editingId = entry?.id
        name = entry?.name.orEmpty()
        daily = entry?.plan?.let { SavingsTablePlan.format(it.daily) }.orEmpty()
        target = entry?.plan?.let { SavingsTablePlan.format(it.target) }.orEmpty()
        error = false
        showEditor = true
    }
    fun persist(next: List<SavingsTableEntry>) {
        store.save(next)
        tables = next
    }
    fun save(entry: SavingsTableEntry) {
        persist(if (tables.any { it.id == entry.id }) tables.map { if (it.id == entry.id) entry else it } else tables + entry)
        if (editingId == null) selectedId = entry.id
        showEditor = false
    }
    BackHandler(enabled = selected != null && !showEditor) { selectedId = null; message = "" }
    val headings = listOf(R.string.table_day, R.string.table_daily, R.string.table_cumulative, R.string.table_remaining, R.string.table_done)
        .map { stringResource(it) }
    val export = rememberFileExport("text/csv", "savings-table-${selected?.id ?: "new"}.csv", {
        requireNotNull(selected).let { it.plan.csv(it.completed, headings) }
    }, { message = it })

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(selected?.name ?: stringResource(R.string.table_title), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                if (selected != null) IconButton(onClick = { selectedId = null; message = "" }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.table_all))
                } else FunctionMenu(onHome = onBack, onTable = {})
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )
    }, floatingActionButton = {
        if (selected == null) FloatingActionButton(onClick = { edit(null) }, containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.table_new))
        }
    }) { padding ->
        // Keying the list gives each detail page a fresh scroll position.
        key(selectedId) {
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = minimumCellWidth), modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (selected == null) {
                    if (tables.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(Modifier.fillMaxWidth().padding(vertical = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                Spacer(Modifier.height(16.dp))
                                Text(stringResource(R.string.table_empty), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.table_empty_hint), Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            val saved = tables.sumOf { it.plan.saved(it.completed) }
                            val total = tables.sumOf { it.plan.target }
                            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(R.string.table_overview), style = MaterialTheme.typography.titleMedium)
                                    Text("฿${SavingsTablePlan.format(saved)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.table_total_target, SavingsTablePlan.format(total)))
                                    Text(stringResource(R.string.table_count, tables.size), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                        items(tables, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { entry ->
                            SavingsTableCard(entry, onOpen = { selectedId = entry.id; message = "" }, onEdit = { edit(entry) }, onDelete = { deleting = entry })
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(72.dp)) }
                } else {
                    val active = selected.plan
                    val completed = selected.completed
                    val saved = active.saved(completed)
                    val percent = (saved * 100 / active.target).toInt()
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularGoalGauge(progressPercent = percent, subText = stringResource(R.string.table_saved_label),
                                    progressFraction = active.progressFraction(saved).toFloat(), progressLabel = active.progressLabel(saved))
                                Text("฿${SavingsTablePlan.format(saved)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.table_total_target, SavingsTablePlan.format(active.target)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(stringResource(R.string.table_remaining_value, SavingsTablePlan.format(active.target - saved)))
                                Text(stringResource(R.string.table_schedule, SavingsTablePlan.format(active.daily), active.dayCount), style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { edit(selected) }) { Text(stringResource(R.string.ui_edit)) }
                                    Button(onClick = export) { Text(stringResource(R.string.table_export)) }
                                }
                                TextButton(onClick = { deleting = selected }) { Text(stringResource(R.string.table_delete), color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.table_independent), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (message.isNotEmpty()) Text(message)
                            Text(stringResource(R.string.table_daily_log), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.table_grid_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items((1..active.dayCount).toList(), key = { it }) { day ->
                        val checked = day in completed
                        Surface(
                            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.toggleable(
                                value = checked, role = Role.Checkbox,
                                onValueChange = { savedDay ->
                                    tables.firstOrNull { it.id == selectedId }?.let { entry ->
                                        val updated = entry.withDay(day, savedDay)
                                        persist(tables.map { if (it.id == updated.id) updated else it })
                                    }
                                }
                            ),
                            shape = RoundedCornerShape(10.dp),
                            color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(Modifier.heightIn(min = 100.dp).padding(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${stringResource(R.string.table_day)} $day", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                    Icon(if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null, modifier = Modifier.size(18.dp),
                                        tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("฿${SavingsTablePlan.format(active.amount(day))}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showEditor) AlertDialog(onDismissRequest = { showEditor = false },
        title = { Text(stringResource(if (editingId == null) R.string.table_new else R.string.table_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.table_description))
                OutlinedTextField(value = name, onValueChange = { name = it; error = false }, label = { Text(stringResource(R.string.table_name)) }, singleLine = true)
                OutlinedTextField(value = daily, onValueChange = { daily = it; error = false }, label = { Text(stringResource(R.string.table_daily)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(value = target, onValueChange = { target = it; error = false }, label = { Text(stringResource(R.string.table_target)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                if (error) Text(stringResource(R.string.table_invalid), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = {
            val d = SavingsTablePlan.parseAmount(daily)
            val t = SavingsTablePlan.parseAmount(target)
            val next = if (d != null && t != null) runCatching { SavingsTablePlan(d, t) }.getOrNull() else null
            error = next == null || name.isBlank()
            if (!error && next != null) {
                val existing = tables.firstOrNull { it.id == editingId }
                val entry = existing?.withDetails(name, next) ?: SavingsTableEntry(name = name.trim(), plan = next)
                if (existing != null && next != existing.plan && existing.completed.isNotEmpty()) pending = entry
                else save(entry)
            }
        }) { Text(stringResource(R.string.table_save)) } },
        dismissButton = { TextButton(onClick = { showEditor = false }) { Text(stringResource(R.string.ui_cancel)) } })
    if (pending != null) AlertDialog(onDismissRequest = { pending = null },
        title = { Text(stringResource(R.string.table_replace)) }, text = { Text(stringResource(R.string.table_replace_warning)) },
        confirmButton = { TextButton(onClick = { pending?.let { save(it) }; pending = null }) { Text(stringResource(R.string.table_save)) } },
        dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.ui_cancel)) } })
    if (deleting != null) AlertDialog(onDismissRequest = { deleting = null },
        title = { Text(stringResource(R.string.table_delete)) }, text = { Text(stringResource(R.string.table_delete_warning, deleting?.name.orEmpty())) },
        confirmButton = { TextButton(onClick = {
            persist(tables.filterNot { it.id == deleting?.id })
            if (selectedId == deleting?.id) selectedId = null
            deleting = null
        }) { Text(stringResource(R.string.table_delete)) } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.ui_cancel)) } })
}

@Composable
private fun SavingsTableCard(entry: SavingsTableEntry, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val saved = entry.plan.saved(entry.completed)
    val percent = (saved * 100 / entry.plan.target).toInt()
    val color = when { percent <= 30 -> ProgressAmber; percent <= 60 -> ProgressCyan; else -> ProgressEmerald }
    val progress by animateFloatAsState(targetValue = entry.plan.progressFraction(saved).toFloat(), label = "table_progress")
    var menu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Text(entry.name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(entry.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.table_schedule, SavingsTablePlan.format(entry.plan.daily), entry.plan.dayCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.ui_options)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.ui_edit)) }, onClick = { menu = false; onEdit() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.table_delete), color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onDelete() })
                    }
                }
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = color, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("฿${SavingsTablePlan.format(saved)} / ฿${SavingsTablePlan.format(entry.plan.target)}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
                Text(entry.plan.progressLabel(saved), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}
