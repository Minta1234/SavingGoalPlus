package com.minta1234.savingsgoal.ui.detail

import androidx.compose.ui.res.stringResource
import com.minta1234.savingsgoal.R
import android.app.Application
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minta1234.savingsgoal.data.Payment
import com.minta1234.savingsgoal.data.SavingsCalculations
import com.minta1234.savingsgoal.data.SortOrder
import com.minta1234.savingsgoal.ui.components.CircularGoalGauge
import com.minta1234.savingsgoal.ui.components.ConfettiCelebrationDialog
import com.minta1234.savingsgoal.ui.components.GoalEditDialog
import com.minta1234.savingsgoal.ui.components.PaymentAddEditDialog
import com.minta1234.savingsgoal.ui.components.SingleSummaryCardView
import com.minta1234.savingsgoal.ui.components.localizedDeadlineText
import com.minta1234.savingsgoal.ui.theme.DepositGreen
import com.minta1234.savingsgoal.ui.theme.ProgressEmerald
import com.minta1234.savingsgoal.ui.theme.WithdrawRed
import java.io.File
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: Long,
    onBack: () -> Unit,
    saveToday: Boolean = false
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val viewModel: GoalDetailViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return GoalDetailViewModel(app, goalId) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val goal = uiState.goal

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddPaymentDialog by rememberSaveable { mutableStateOf(saveToday) }
    var todayOnly by rememberSaveable { mutableStateOf(saveToday) }
    var editingPayment by remember { mutableStateOf<Payment?>(null) }
    var deletingPayment by remember { mutableStateOf<Payment?>(null) }
    var showEditGoalDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showCelebrationDialog by remember { mutableStateOf(false) }

    val currency = if (goal?.currency.isNullOrBlank() || goal.currency == "null") uiState.defaultCurrency else goal.currency

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = goal?.name ?: stringResource(R.string.ui_goal_details),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ui_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showEditGoalDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.ui_edit_goal))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    todayOnly = false
                    showAddPaymentDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.ui_add_payment))
            }
        }
    ) { innerPadding ->
        if (goal == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.ui_loading_goal))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Secondary Tab Row: Overview vs Transactions
                SecondaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text(stringResource(R.string.ui_overview), fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text(stringResource(R.string.transactions_count, uiState.payments.size), fontWeight = FontWeight.SemiBold) }
                    )
                }

                if (selectedTabIndex == 0) {
                    // TAB 0: OVERVIEW
                    OverviewTabContent(
                        goal = goal,
                        currency = currency,
                        ratePlan = uiState.ratePlan,
                        toSaveOption = uiState.toSaveOption,
                        onSetTargetDate = { showDatePickerDialog = true },
                        onCelebrate = { showCelebrationDialog = true }
                    )
                } else {
                    // TAB 1: TRANSACTIONS LIST
                    TransactionsTabContent(
                        payments = uiState.payments,
                        currency = currency,
                        sortOrder = uiState.sortOrder,
                        onSortChange = { viewModel.setSortOrder(it) },
                        onEdit = { editingPayment = it },
                        onDelete = { deletingPayment = it }
                    )
                }
            }
        }
    }

    // Add Payment Dialog
    if (showAddPaymentDialog && goal != null) {
        PaymentAddEditDialog(
            currencySymbol = currency,
            todayDepositGoal = goal.name.takeIf { todayOnly },
            onDismiss = { showAddPaymentDialog = false },
            onSave = { amount, date, isSaving, note ->
                val prevSaved = goal.totalSaved
                val target = goal.amount
                val newSaved = if (isSaving) prevSaved + amount else prevSaved - amount
                viewModel.addPayment(amount, date, isSaving, note)
                showAddPaymentDialog = false
                if (target > 0 && newSaved >= target && prevSaved < target) {
                    showCelebrationDialog = true
                }
            }
        )
    }

    // Confetti Celebration Dialog
    if (showCelebrationDialog && goal != null) {
        ConfettiCelebrationDialog(
            goal = goal,
            currencySymbol = currency,
            onDismiss = { showCelebrationDialog = false }
        )
    }

    // Edit Payment Dialog
    if (editingPayment != null) {
        PaymentAddEditDialog(
            currencySymbol = currency,
            initialPayment = editingPayment,
            onDismiss = { editingPayment = null },
            onSave = { amount, date, isSaving, note ->
                editingPayment?.let { p ->
                    viewModel.updatePayment(p.id, amount, date, isSaving, note)
                }
                editingPayment = null
            }
        )
    }

    // Delete Payment Confirmation
    if (deletingPayment != null) {
        AlertDialog(
            onDismissRequest = { deletingPayment = null },
            title = { Text(stringResource(R.string.ui_delete_transaction)) },
            text = { Text(stringResource(R.string.delete_transaction_warning, SavingsCalculations.formatCurrency(deletingPayment?.amount ?: 0.0, currency))) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingPayment?.let { viewModel.deletePayment(it.id) }
                        deletingPayment = null
                    }
                ) {
                    Text(stringResource(R.string.ui_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPayment = null }) {
                    Text(stringResource(R.string.ui_cancel))
                }
            }
        )
    }

    // Edit Goal Dialog
    if (showEditGoalDialog && goal != null) {
        GoalEditDialog(
            initialGoal = goal,
            defaultCurrency = uiState.defaultCurrency,
            onDismiss = { showEditGoalDialog = false },
            onSave = { name, amount, date, note, cur, bitmap, removeImg ->
                viewModel.updateGoal(name, amount, date, note, cur, bitmap, removeImg)
                showEditGoalDialog = false
            }
        )
    }

    // Date Picker for Target Date
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val localDate = Instant.ofEpochMilli(selectedMillis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        viewModel.updateTargetDate(SavingsCalculations.formatDateForStorage(localDate))
                    }
                    showDatePickerDialog = false
                }) {
                    Text(stringResource(R.string.ui_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(stringResource(R.string.ui_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun OverviewTabContent(
    goal: com.minta1234.savingsgoal.data.Goal,
    currency: String,
    ratePlan: com.minta1234.savingsgoal.data.SavingsRatePlan?,
    toSaveOption: Int,
    onSetTargetDate: () -> Unit,
    onCelebrate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Goal Summary Card (K-Plus Card Like in Home)
        SingleSummaryCardView(
            title = goal.name,
            savedAmount = goal.totalSaved,
            targetAmount = goal.amount,
            remainingAmount = goal.remainingAmount,
            progressFraction = goal.progressFraction,
            statusBadge = "${goal.progressPercent}%",
            statusLabel = stringResource(R.string.ui_progress),
            currencySymbol = currency,
            imagePath = goal.imagePath,
            onClick = null,
            modifier = Modifier.fillMaxWidth()
        )

        // Circular Gauge
        CircularGoalGauge(
            progressPercent = goal.progressPercent,
            subText = stringResource(if (goal.isReached) R.string.ui_goal_achieved else R.string.ui_saved)
        )

        // Celebrate Banner Button if goal is completed
        if (goal.isReached) {
            Button(
                onClick = onCelebrate,
                colors = ButtonDefaults.buttonColors(containerColor = ProgressEmerald),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.ui_goal_completed_celebrate), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        // 3 Metrics Breakdown Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = stringResource(R.string.ui_target),
                amount = SavingsCalculations.formatCurrency(goal.amount, currency),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = stringResource(R.string.ui_saved),
                amount = SavingsCalculations.formatCurrency(goal.totalSaved, currency),
                color = ProgressEmerald,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = if (goal.excessAmount > 0.0) stringResource(R.string.ui_exceeding) else stringResource(R.string.ui_remaining),
                amount = SavingsCalculations.formatCurrency(
                    if (goal.excessAmount > 0.0) goal.excessAmount else goal.remainingAmount,
                    currency
                ),
                color = if (goal.excessAmount > 0.0) ProgressEmerald else MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        // Target Date Button & Countdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedButton(
                    onClick = onSetTargetDate,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (!goal.date.isNullOrBlank()) stringResource(R.string.target_date_short, SavingsCalculations.formatDateForDisplay(goal.date)) else stringResource(R.string.ui_add_target_date)
                    )
                }

                if (ratePlan != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = ratePlan.localizedDeadlineText(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ratePlan.isExpired && !ratePlan.isReached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Savings Projection Engine (Kirakira)
        if (ratePlan != null && !ratePlan.isExpired && !ratePlan.isReached && goal.remainingAmount > 0.0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ui_recommended_savings_plan),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ProjectionItem(
                            label = stringResource(R.string.ui_daily),
                            value = SavingsCalculations.formatCurrency(ratePlan.dailyAmount, currency)
                        )
                        ProjectionItem(
                            label = if (toSaveOption == 2) stringResource(R.string.ui_bi_weekly) else stringResource(R.string.ui_weekly),
                            value = SavingsCalculations.formatCurrency(
                                if (toSaveOption == 2) ratePlan.biWeeklyAmount else ratePlan.weeklyAmount,
                                currency
                            )
                        )
                        ProjectionItem(
                            label = stringResource(R.string.ui_monthly),
                            value = SavingsCalculations.formatCurrency(ratePlan.monthlyAmount, currency)
                        )
                    }
                }
            }
        }

        // Notes section (if any)
        if (goal.note.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = goal.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
private fun MetricCard(
    title: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProjectionItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TransactionsTabContent(
    payments: List<Payment>,
    currency: String,
    sortOrder: SortOrder,
    onSortChange: (SortOrder) -> Unit,
    onEdit: (Payment) -> Unit,
    onDelete: (Payment) -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sort Row Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.records_count, payments.size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Box {
                FilterChip(
                    selected = true,
                    onClick = { sortMenuExpanded = true },
                    label = {
                        Text(
                            when (sortOrder) {
                                SortOrder.NEWEST_FIRST -> stringResource(R.string.ui_newest_first)
                                SortOrder.OLDEST_FIRST -> stringResource(R.string.ui_oldest_first)
                                SortOrder.HIGHEST_AMOUNT -> stringResource(R.string.ui_highest_amount)
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.ui_newest_first)) },
                        onClick = {
                            sortMenuExpanded = false
                            onSortChange(SortOrder.NEWEST_FIRST)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.ui_oldest_first)) },
                        onClick = {
                            sortMenuExpanded = false
                            onSortChange(SortOrder.OLDEST_FIRST)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.ui_highest_amount)) },
                        onClick = {
                            sortMenuExpanded = false
                            onSortChange(SortOrder.HIGHEST_AMOUNT)
                        }
                    )
                }
            }
        }

        if (payments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.ui_no_transactions_yet),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.ui_tap_the_button_to_add_your_first_deposit_or_withdrawal),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(payments, key = { it.id }) { payment ->
                    PaymentItemRow(
                        payment = payment,
                        currency = currency,
                        onEdit = { onEdit(payment) },
                        onDelete = { onDelete(payment) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}

@Composable
private fun PaymentItemRow(
    payment: Payment,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Icon (+ / -)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (payment.isSaving) DepositGreen.copy(alpha = 0.15f) else WithdrawRed.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (payment.isSaving) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = if (payment.isSaving) stringResource(R.string.ui_deposit_alt) else stringResource(R.string.ui_withdrawal),
                    tint = if (payment.isSaving) DepositGreen else WithdrawRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (payment.isSaving) "+ ${SavingsCalculations.formatCurrency(payment.amount, currency)}" else "- ${SavingsCalculations.formatCurrency(payment.amount, currency)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (payment.isSaving) DepositGreen else WithdrawRed
                    )

                    Text(
                        text = SavingsCalculations.formatDateForDisplay(payment.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (payment.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = payment.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.ui_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.ui_edit)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.ui_delete), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
