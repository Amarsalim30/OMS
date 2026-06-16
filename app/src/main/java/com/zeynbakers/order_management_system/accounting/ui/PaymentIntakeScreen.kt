package com.zeynbakers.order_management_system.accounting.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.LocalUiEventDispatcher
import com.zeynbakers.order_management_system.core.ui.showSnackbar
import com.zeynbakers.order_management_system.core.util.formatKes
import java.math.BigDecimal
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun MpesaImportScreen(
    viewModel: PaymentIntakeViewModel,
    initialText: String?,
    onClose: () -> Unit,
    onApplied: (PaymentApplySummary) -> Unit,
    onAppliedInPlace: () -> Unit,
    onOpenReceiptHistory: (Long) -> Unit,
    showTopBar: Boolean = true,
    externalPadding: PaddingValues = PaddingValues(0.dp)
) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val uiEvents = LocalUiEventDispatcher.current
    val rawText by viewModel.rawText.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    var intakeFilter by rememberSaveable { mutableStateOf(IntakeFilter.All) }
    var activeKey by rememberSaveable { mutableStateOf<String?>(null) }
    val clipboardEmptyMessage = stringResource(R.string.money_clipboard_empty)
    val receiptNotFoundMessage = stringResource(R.string.money_receipt_not_found)
    val appliedPaymentsTemplate = stringResource(R.string.money_applied_payments)
    val existingDuplicatesTemplate = stringResource(R.string.money_applied_existing_duplicates)
    val intakeDuplicatesTemplate = stringResource(R.string.money_applied_intake_duplicates)
    val missingCustomerTemplate = stringResource(R.string.money_applied_missing_customer)

    LaunchedEffect(initialText) {
        val text = initialText?.trim().orEmpty()
        if (text.isNotBlank()) {
            viewModel.setRawText(text)
        }
    }

    val intakeStats = remember(transactions) { transactions.toIntakeStats() }
    val totalDetected = intakeStats.totalDetected
    val duplicateCount = intakeStats.duplicatesCount
    val needsMatchCount = intakeStats.needsCount
    val readyCount = intakeStats.readyCount
    val readyAmount = intakeStats.readyAmount
    val selectedCount = intakeStats.selectedCount
    val selectedReadyCount = intakeStats.selectedReadyCount
    val selectedReadyAmount = intakeStats.selectedReadyAmount
    val allCount = intakeStats.totalDetected
    val needsCount = intakeStats.needsCount
    val duplicatesCount = intakeStats.duplicatesCount

    val filteredTransactions =
        remember(transactions, intakeFilter) {
            when (intakeFilter) {
                IntakeFilter.All -> transactions
                IntakeFilter.Needs ->
                    transactions.filter { it.duplicateState == DuplicateState.NONE && !it.canApply() }
                IntakeFilter.Duplicates -> transactions.filter { it.duplicateState != DuplicateState.NONE }
                IntakeFilter.Selected -> transactions.filter { it.selected }
            }
        }

    val pasteFromClipboard: () -> Unit = {
        val clip = clipboardManager.getText()?.text?.trim().orEmpty()
        if (clip.isBlank()) {
            scope.launch { uiEvents.showSnackbar(clipboardEmptyMessage) }
            Unit
        } else if (rawText.isBlank()) {
            viewModel.setRawText(clip)
        } else {
            viewModel.appendRawText(clip)
        }
    }

    val applyAndNotify: suspend () -> PaymentApplySummary = {
        val summary = viewModel.applySelected()
        val messageParts =
            mutableListOf(
                appliedPaymentsTemplate.format(summary.applied)
            )
        if (summary.existingDuplicates > 0) {
            messageParts += existingDuplicatesTemplate.format(summary.existingDuplicates)
        }
        if (summary.intakeDuplicates > 0) {
            messageParts += intakeDuplicatesTemplate.format(summary.intakeDuplicates)
        }
        if (summary.skippedNoCustomer > 0) {
            messageParts += missingCustomerTemplate.format(summary.skippedNoCustomer)
        }
        val message = messageParts.joinToString(separator = ", ")
        uiEvents.showSnackbar(message)
        summary
    }

    LaunchedEffect(transactions, activeKey) {
        val currentKey = activeKey
        if (currentKey != null && transactions.none { it.key == currentKey }) {
            activeKey = null
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(stringResource(R.string.money_collect_title)) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (readyCount > 0) {
                ApplyReadyBar(
                    selectedReadyCount = selectedReadyCount,
                    selectedReadyAmount = selectedReadyAmount,
                    readyCount = readyCount,
                    readyAmount = readyAmount,
                    onApplyReady = {
                        scope.launch {
                            viewModel.selectReadyOnly()
                            val summary = applyAndNotify()
                            onApplied(summary)
                        }
                    },
                    onApplySelected = {
                        scope.launch {
                            val summary = applyAndNotify()
                            onApplied(summary)
                        }
                    }
                )
            }
        }
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        val contentPadding =
            PaddingValues(
                start = 16.dp + externalPadding.calculateStartPadding(layoutDirection),
                end = 16.dp + externalPadding.calculateEndPadding(layoutDirection),
                top = 16.dp + externalPadding.calculateTopPadding(),   // fixed: 15->16dp
                bottom = 8.dp + externalPadding.calculateBottomPadding()
            )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)   // fixed: 4->8dp
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Paste text field - replaced deprecated BasicTextField
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { viewModel.setRawText(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.money_mpesa_messages_label)) },
                        placeholder = { Text(stringResource(R.string.money_paste_messages_placeholder)) },
                        supportingText = if (rawText.isBlank()) {
                            { Text(stringResource(R.string.money_separate_messages)) }
                        } else null,
                        minLines = 2,
                        maxLines = 4,
                        singleLine = false
                    )

                    // Action buttons row
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        TextButton(onClick = pasteFromClipboard) { Text(stringResource(R.string.action_paste)) }
                        TextButton(
                            onClick = { viewModel.setRawText("") },
                            enabled = rawText.isNotBlank()
                        ) { Text(stringResource(R.string.action_clear)) }
                        TextButton(
                            onClick = { viewModel.selectReadyOnly() },
                            enabled = transactions.isNotEmpty()
                        ) { Text(stringResource(R.string.money_select_ready)) }
                        TextButton(
                            onClick = { viewModel.setAllSelected(false) },
                            enabled = selectedCount > 0
                        ) { Text(stringResource(R.string.money_clear_selection)) }
                    }
                }
            }

            // Empty state when no text has been pasted
            if (rawText.isBlank()) {
                item {
                    MpesaEmptyState(onPaste = pasteFromClipboard)
                }
            }

            // Filter chip row — replaces dropdown menu
            if (transactions.isNotEmpty()) {
                stickyHeader {
                    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = intakeFilter == IntakeFilter.All,
                                onClick = { intakeFilter = IntakeFilter.All },
                                label = { Text(stringResource(R.string.money_filter_all_count, allCount)) },
                                modifier = Modifier.sizeIn(minHeight = 48.dp)
                            )
                            FilterChip(
                                selected = intakeFilter == IntakeFilter.Needs,
                                onClick = { intakeFilter = IntakeFilter.Needs },
                                label = { Text(stringResource(R.string.money_filter_needs_count, needsCount)) },
                                modifier = Modifier.sizeIn(minHeight = 48.dp)
                            )
                            FilterChip(
                                selected = intakeFilter == IntakeFilter.Duplicates,
                                onClick = { intakeFilter = IntakeFilter.Duplicates },
                                label = { Text(stringResource(R.string.money_filter_duplicates_count, duplicatesCount)) },
                                modifier = Modifier.sizeIn(minHeight = 48.dp)
                            )
                            FilterChip(
                                selected = intakeFilter == IntakeFilter.Selected,
                                onClick = { intakeFilter = IntakeFilter.Selected },
                                label = { Text(stringResource(R.string.money_filter_selected_count, selectedCount)) },
                                modifier = Modifier.sizeIn(minHeight = 48.dp)
                            )
                        }
                    }
                }
            }

            if (transactions.isNotEmpty() && filteredTransactions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.money_no_payments_for_filter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(filteredTransactions, key = { it.key }) { item ->
                    MpesaTransactionRow(
                        item = item,
                        onToggleSelected = { selected -> viewModel.setSelected(item.key, selected) },
                        onOpenDetails = { activeKey = item.key }
                    )
                }
            }
        }
    }

    val activeItem = transactions.firstOrNull { it.key == activeKey }
    if (activeItem != null) {
        MpesaAllocationSheet(
            item = activeItem,
            searchCustomers = { query -> viewModel.searchCustomers(query) },
            onSelectCustomer = { customerId ->
                viewModel.selectCustomer(activeItem.key, customerId)
            },
            onSelectOrder = { orderId ->
                viewModel.selectOrder(activeItem.key, orderId)
            },
            onSelectAllocationMode = { mode ->
                viewModel.selectAllocationMode(activeItem.key, mode)
            },
            onViewExisting = {
                val target = activeItem.existingReceiptId
                if (target != null) {
                    onOpenReceiptHistory(target)
                } else {
                    scope.launch { uiEvents.showSnackbar(receiptNotFoundMessage) }
                }
            },
            onMoveExisting = {
                scope.launch {
                    val result = viewModel.reallocateExistingReceipt(activeItem.key)
                    uiEvents.showSnackbar(result.message)
                }
            },
            onDismiss = { activeKey = null }
        )
    }
}

/**
 * Shown when the paste field is blank. Guides new users through the M-Pesa
 * message import flow with three numbered steps and a prominent Paste button.
 */
@Composable
private fun MpesaEmptyState(onPaste: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.money_empty_state_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.money_empty_state_step1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.money_empty_state_step2),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.money_empty_state_step3),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(onClick = onPaste) {
            Icon(
                imageVector = Icons.Filled.ContentPaste,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.action_paste))
        }
    }
}

private enum class IntakeFilter(val labelRes: Int) {
    All(R.string.money_filter_all_short),
    Needs(R.string.money_filter_needs_short),
    Duplicates(R.string.money_filter_duplicates_short),
    Selected(R.string.money_filter_selected_short)
}

private data class IntakeStats(
    val totalDetected: Int,
    val duplicatesCount: Int,
    val needsCount: Int,
    val readyCount: Int,
    val readyAmount: BigDecimal,
    val selectedCount: Int,
    val selectedReadyCount: Int,
    val selectedReadyAmount: BigDecimal
)

private fun List<MpesaTransactionUi>.toIntakeStats(): IntakeStats {
    var duplicatesCount = 0
    var needsCount = 0
    var readyCount = 0
    var readyAmount = BigDecimal.ZERO
    var selectedCount = 0
    var selectedReadyCount = 0
    var selectedReadyAmount = BigDecimal.ZERO

    forEach { item ->
        val isDuplicate = item.duplicateState != DuplicateState.NONE
        val canApply = item.canApply()
        if (isDuplicate) {
            duplicatesCount += 1
        } else if (!canApply) {
            needsCount += 1
        } else {
            readyCount += 1
            readyAmount += item.amount
        }

        if (item.selected) {
            selectedCount += 1
            if (!isDuplicate && canApply) {
                selectedReadyCount += 1
                selectedReadyAmount += item.amount
            }
        }
    }

    return IntakeStats(
        totalDetected = size,
        duplicatesCount = duplicatesCount,
        needsCount = needsCount,
        readyCount = readyCount,
        readyAmount = readyAmount,
        selectedCount = selectedCount,
        selectedReadyCount = selectedReadyCount,
        selectedReadyAmount = selectedReadyAmount
    )
}

