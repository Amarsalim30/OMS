package com.zeynbakers.order_management_system.accounting.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.LocalUiEventDispatcher
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import com.zeynbakers.order_management_system.core.ui.components.AppEmptyState
import com.zeynbakers.order_management_system.core.ui.components.AppFilterOption
import com.zeynbakers.order_management_system.core.ui.components.AppFilterRow
import com.zeynbakers.order_management_system.core.ui.components.AppScreenHeaderCard
import com.zeynbakers.order_management_system.core.ui.components.AppSpacing
import com.zeynbakers.order_management_system.core.ui.showSnackbar
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MpesaImportScreen(
    viewModel: PaymentIntakeViewModel,
    initialText: String?,
    onClose: () -> Unit,
    onApplied: (PaymentApplySummary) -> Unit,
    onAppliedInPlace: () -> Unit,
    onOpenReceiptHistory: (Long) -> Unit,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
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
    val readyCount = intakeStats.readyCount
    val readyAmount = intakeStats.readyAmount
    val selectedReadyCount = intakeStats.selectedReadyCount
    val selectedReadyAmount = intakeStats.selectedReadyAmount

    val filteredTransactions = remember(transactions, intakeFilter) {
        when (intakeFilter) {
            IntakeFilter.All -> transactions
            IntakeFilter.Needs -> transactions.filter { it.duplicateState == DuplicateState.NONE && !it.canApply() }
            IntakeFilter.Duplicates -> transactions.filter { it.duplicateState != DuplicateState.NONE }
            IntakeFilter.Selected -> transactions.filter { it.selected }
        }
    }

    val pasteFromClipboard: () -> Unit = {
        val clip = clipboard.getText()?.text?.trim().orEmpty()
        if (clip.isBlank()) {
            scope.launch { uiEvents.showSnackbar(clipboardEmptyMessage) }
        } else if (rawText.isBlank()) {
            viewModel.setRawText(clip)
        } else {
            viewModel.appendRawText(clip)
        }
    }

    val applyAndNotify: suspend () -> PaymentApplySummary = {
        val summary = viewModel.applySelected()
        val messageParts = mutableListOf(appliedPaymentsTemplate.format(summary.applied))
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

    Scaffold(
        modifier = modifier,
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            item { Spacer(modifier = Modifier.height(AppSpacing.xSmall)) }

            // Header Section
            item {
                AppScreenHeaderCard(
                    title = stringResource(R.string.money_collect_title),
                    subtitle = stringResource(R.string.share_payment_trust_body),
                    highlight = if (transactions.isNotEmpty()) stringResource(R.string.money_detected) + " " + transactions.size else null
                )
            }

            // Workflow Progress
            item {
                IntakeStepRow(
                    hasSource = rawText.isNotBlank(),
                    hasAssignments = transactions.isNotEmpty() && transactions.any { it.canApply() },
                    hasSelected = transactions.any { it.selected }
                )
            }

            // Input Section
            item {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                        OutlinedTextField(
                            value = rawText,
                            onValueChange = { viewModel.setRawText(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.money_mpesa_messages_label)) },
                            placeholder = { Text(stringResource(R.string.money_paste_messages_placeholder)) },
                            minLines = 3,
                            maxLines = 5,
                            shape = MaterialTheme.shapes.medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.setRawText("") },
                                enabled = rawText.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.action_clear))
                            }

                            Button(
                                onClick = pasteFromClipboard,
                                modifier = Modifier.weight(1.2f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(
                                    Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.action_paste))
                            }
                        }
                    }
                }
            }

            // Filters Section
            if (transactions.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = stringResource(R.string.money_search_results),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = AppSpacing.xSmall, start = 4.dp)
                        )

                        AppFilterRow(
                            options = listOf(
                                AppFilterOption(
                                    IntakeFilter.All.name,
                                    stringResource(
                                        R.string.money_filter_all_count,
                                        transactions.size
                                    )
                                ),
                                AppFilterOption(
                                    IntakeFilter.Needs.name,
                                    stringResource(
                                        R.string.money_filter_needs_count,
                                        intakeStats.needsCount
                                    )
                                ),
                                AppFilterOption(
                                    IntakeFilter.Duplicates.name,
                                    stringResource(
                                        R.string.money_filter_duplicates_count,
                                        intakeStats.duplicatesCount
                                    )
                                ),
                                AppFilterOption(
                                    IntakeFilter.Selected.name,
                                    stringResource(
                                        R.string.money_filter_selected_count,
                                        intakeStats.selectedCount
                                    )
                                )
                            ),
                            selectedKey = intakeFilter.name,
                            onSelect = { key -> intakeFilter = IntakeFilter.valueOf(key) }
                        )
                    }
                }
            }

            // Empty State or Transactions List
            if (rawText.isBlank()) {
                item {
                    AppEmptyState(
                        title = stringResource(R.string.money_empty_state_title),
                        body = listOf(
                            stringResource(R.string.money_empty_state_step1),
                            stringResource(R.string.money_empty_state_step2),
                            stringResource(R.string.money_empty_state_step3)
                        ).joinToString("\n"),
                        actionLabel = stringResource(R.string.action_paste),
                        onAction = pasteFromClipboard
                    )
                }
            } else if (filteredTransactions.isEmpty() && transactions.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.money_no_payments_for_filter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = AppSpacing.medium)
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

            item { Spacer(modifier = Modifier.height(AppSpacing.medium)) }
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
                    uiEvents.showSnackbar(context.getString(result.messageRes))
                }
            },
            onDismiss = { activeKey = null }
        )
    }
}

private enum class IntakeFilter {
    All,
    Needs,
    Duplicates,
    Selected
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
