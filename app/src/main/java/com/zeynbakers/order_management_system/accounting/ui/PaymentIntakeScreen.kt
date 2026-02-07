@file:Suppress("DEPRECATION")

package com.zeynbakers.order_management_system.accounting.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.components.AppCard
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
    var rawExpanded by rememberSaveable { mutableStateOf(true) }
    var didAutoCollapse by rememberSaveable { mutableStateOf(false) }
    var intakeFilter by rememberSaveable { mutableStateOf(IntakeFilter.All) }
    var filterMenuOpen by remember { mutableStateOf(false) }
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

    val totalDetected = transactions.size
    val duplicateCount = transactions.count { it.duplicateState != DuplicateState.NONE }
    val needsMatchCount =
        transactions.count { it.duplicateState == DuplicateState.NONE && !it.canApply() }
    val readyItems =
        transactions.filter { it.duplicateState == DuplicateState.NONE && it.canApply() }
    val readyCount = readyItems.size
    val readyAmount =
        readyItems.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
    val rawIsLarge = rawText.lineSequence().count() > 3 || rawText.length > 220
    val rawPreviewLine =
        rawText.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: rawText.trim()
    val allCount = transactions.size
    val needsCount =
        transactions.count { it.duplicateState == DuplicateState.NONE && !it.canApply() }
    val duplicatesCount = transactions.count { it.duplicateState != DuplicateState.NONE }
    val selectedCount = transactions.count { it.selected }
    val selectedReadyItems =
        transactions.filter { it.selected && it.duplicateState == DuplicateState.NONE && it.canApply() }
    val selectedReadyCount = selectedReadyItems.size
    val selectedReadyAmount =
        selectedReadyItems.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
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
            messageParts +=
                existingDuplicatesTemplate.format(summary.existingDuplicates)
        }
        if (summary.intakeDuplicates > 0) {
            messageParts +=
                intakeDuplicatesTemplate.format(summary.intakeDuplicates)
        }
        if (summary.skippedNoCustomer > 0) {
            messageParts +=
                missingCustomerTemplate.format(summary.skippedNoCustomer)
        }
        val message =
            messageParts.joinToString(separator = ", ")
        uiEvents.showSnackbar(message)
        summary
    }

    LaunchedEffect(rawText) {
        if (rawText.isBlank()) {
            rawExpanded = true
            didAutoCollapse = false
        } else if (rawIsLarge && !didAutoCollapse) {
            rawExpanded = false
            didAutoCollapse = true
        }
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
                    onApplySelected = {
                        scope.launch {
                            val summary = applyAndNotify()
                            onApplied(summary)
                        }
                    },
                    onApplyAllReady = {
                        scope.launch {
                            viewModel.selectReadyOnly()
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
                start = 12.dp + externalPadding.calculateStartPadding(layoutDirection),
                end = 12.dp + externalPadding.calculateEndPadding(layoutDirection),
                top = 15.dp + externalPadding.calculateTopPadding(),
                bottom = 6.dp + externalPadding.calculateBottomPadding()
            )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (rawExpanded || rawText.isBlank() || !rawIsLarge) {
                        val interactionSource = remember { MutableInteractionSource() }
                        BasicTextField(
                            value = rawText,
                            onValueChange = { viewModel.setRawText(it) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            interactionSource = interactionSource,
                            singleLine = false,
                            minLines = 2,
                            maxLines = 3,
                            decorationBox = { innerTextField ->
                                OutlinedTextFieldDefaults.DecorationBox(
                                    value = rawText,
                                    visualTransformation = VisualTransformation.None,
                                    innerTextField = innerTextField,
                                    placeholder = { Text(stringResource(R.string.money_paste_messages_placeholder)) },
                                    label = { Text(stringResource(R.string.money_mpesa_messages_label)) },
                                    trailingIcon = {
                                        if (rawIsLarge) {
                                            IconButton(onClick = { rawExpanded = false }) {
                                                Icon(
                                                    imageVector = Icons.Filled.ExpandLess,
                                                    contentDescription = stringResource(R.string.action_collapse)
                                                )
                                            }
                                        }
                                    },
                                    supportingText =
                                        if (rawText.isBlank()) {
                                            { Text(stringResource(R.string.money_separate_messages)) }
                                        } else {
                                            null
                                        },
                                    singleLine = false,
                                    enabled = true,
                                    isError = false,
                                    interactionSource = interactionSource,
                                    colors =
                                        OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                        ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = rawPreviewLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { rawExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.ExpandMore,
                                        contentDescription = stringResource(R.string.action_expand)
                                    )
                                }
                            }
                        }
                    }

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

            if (transactions.isNotEmpty()) {
                stickyHeader {
                    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val filterLabel =
                                when (intakeFilter) {
                                    IntakeFilter.All -> stringResource(R.string.money_filter_all_count, allCount)
                                    IntakeFilter.Needs -> stringResource(R.string.money_filter_needs_count, needsCount)
                                    IntakeFilter.Duplicates -> stringResource(R.string.money_filter_duplicates_count, duplicatesCount)
                                    IntakeFilter.Selected -> stringResource(R.string.money_filter_selected_count, selectedCount)
                                }
                            Box {
                                TextButton(onClick = { filterMenuOpen = true }) {
                                    Text(filterLabel)
                                }
                                DropdownMenu(
                                    expanded = filterMenuOpen,
                                    onDismissRequest = { filterMenuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.money_filter_all_count, allCount)) },
                                        onClick = {
                                            intakeFilter = IntakeFilter.All
                                            filterMenuOpen = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.money_filter_needs_count, needsCount)) },
                                        onClick = {
                                            intakeFilter = IntakeFilter.Needs
                                            filterMenuOpen = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.money_filter_duplicates_count, duplicatesCount)) },
                                        onClick = {
                                            intakeFilter = IntakeFilter.Duplicates
                                            filterMenuOpen = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.money_filter_selected_count, selectedCount)) },
                                        onClick = {
                                            intakeFilter = IntakeFilter.Selected
                                            filterMenuOpen = false
                                        }
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.money_selected_count, selectedCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.money_no_payments_detected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (filteredTransactions.isEmpty()) {
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

private enum class IntakeFilter(val labelRes: Int) {
    All(R.string.money_filter_all_short),
    Needs(R.string.money_filter_needs_short),
    Duplicates(R.string.money_filter_duplicates_short),
    Selected(R.string.money_filter_selected_short)
}
