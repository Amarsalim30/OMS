@file:Suppress("DEPRECATION")

package com.zeynbakers.order_management_system.accounting.ui

import android.widget.Toast
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.core.util.formatKes
import java.math.BigDecimal
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val rawText by viewModel.rawText.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    var rawExpanded by rememberSaveable { mutableStateOf(true) }
    var didAutoCollapse by rememberSaveable { mutableStateOf(false) }
    var intakeFilter by rememberSaveable { mutableStateOf(IntakeFilter.All) }
    var filterMenuOpen by remember { mutableStateOf(false) }
    var activeKey by rememberSaveable { mutableStateOf<String?>(null) }

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

    val pasteFromClipboard = {
        val clip = clipboardManager.getText()?.text?.trim().orEmpty()
        if (clip.isBlank()) {
            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
        } else if (rawText.isBlank()) {
            viewModel.setRawText(clip)
        } else {
            viewModel.appendRawText(clip)
        }
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
                    title = { Text("M-PESA Import") },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    readyCount = readyCount,
                    readyAmount = readyAmount,
                    onApply = {
                        scope.launch {
                            viewModel.setAllSelected(true)
                            val summary = viewModel.applySelected()
                            val message =
                                buildString {
                                    append("Applied ${summary.applied} payments")
                                    if (summary.existingDuplicates > 0) {
                                        append(", ${summary.existingDuplicates} already recorded")
                                    }
                                    if (summary.intakeDuplicates > 0) {
                                        append(", ${summary.intakeDuplicates} duplicates in import")
                                    }
                                    if (summary.skippedNoCustomer > 0) {
                                        append(", ${summary.skippedNoCustomer} missing customer")
                                    }
                                }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
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
                                    placeholder = { Text("Paste one or more messages") },
                                    label = { Text("M-PESA messages") },
                                    trailingIcon = {
                                        if (rawIsLarge) {
                                            IconButton(onClick = { rawExpanded = false }) {
                                                Icon(
                                                    imageVector = Icons.Filled.ExpandLess,
                                                    contentDescription = "Collapse"
                                                )
                                            }
                                        }
                                    },
                                    supportingText =
                                        if (rawText.isBlank()) {
                                            { Text("Separate each message with a blank line.") }
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
                                        contentDescription = "Expand"
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(onClick = pasteFromClipboard) { Text("Paste") }
                        TextButton(
                            onClick = { viewModel.setRawText("") },
                            enabled = rawText.isNotBlank()
                        ) { Text("Clear") }
                        TextButton(
                            onClick = { viewModel.selectReadyOnly() },
                            enabled = transactions.isNotEmpty()
                        ) { Text("Select ready") }
                        TextButton(
                            onClick = { viewModel.setAllSelected(false) },
                            enabled = selectedCount > 0
                        ) { Text("Clear selection") }
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
                                    IntakeFilter.All -> "All $allCount"
                                    IntakeFilter.Needs -> "Needs $needsCount"
                                    IntakeFilter.Duplicates -> "Duplicates $duplicatesCount"
                                    IntakeFilter.Selected -> "Selected $selectedCount"
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
                                        text = { Text("All $allCount") },
                                        onClick = {
                                            intakeFilter = IntakeFilter.All
                                            filterMenuOpen = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Needs $needsCount") },
                                        onClick = {
                                            intakeFilter = IntakeFilter.Needs
                                            filterMenuOpen = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Duplicates $duplicatesCount") },
                                        onClick = {
                                            intakeFilter = IntakeFilter.Duplicates
                                            filterMenuOpen = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Selected $selectedCount") },
                                        onClick = {
                                            intakeFilter = IntakeFilter.Selected
                                            filterMenuOpen = false
                                        }
                                    )
                                }
                            }
                            Text(
                                text = "Selected $selectedCount",
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
                        text = "No payments detected yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (filteredTransactions.isEmpty()) {
                item {
                    Text(
                        text = "No payments for this filter.",
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
                    Toast.makeText(context, "Receipt not found", Toast.LENGTH_SHORT).show()
                }
            },
            onMoveExisting = {
                scope.launch {
                    val result = viewModel.reallocateExistingReceipt(activeItem.key)
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { activeKey = null }
        )
    }
}

private enum class IntakeFilter(val label: String) {
    All("All"),
    Needs("Needs"),
    Duplicates("Duplicates"),
    Selected("Selected")
}

private enum class StepState {
    Idle,
    Active,
    Complete
}

@Composable
private fun IntakeStepRow(
    hasSource: Boolean,
    hasAssignments: Boolean,
    hasSelected: Boolean
) {
    val sourceState = if (hasSource) StepState.Complete else StepState.Active
    val assignState =
        when {
            !hasSource -> StepState.Idle
            hasAssignments -> StepState.Complete
            else -> StepState.Active
        }
    val postState =
        when {
            !hasAssignments -> StepState.Idle
            hasSelected -> StepState.Complete
            else -> StepState.Active
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StepChip(step = "1", label = "Source", state = sourceState, modifier = Modifier.weight(1f))
        StepChip(step = "2", label = "Assign", state = assignState, modifier = Modifier.weight(1f))
        StepChip(step = "3", label = "Post", state = postState, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StepChip(
    step: String,
    label: String,
    state: StepState,
    modifier: Modifier = Modifier
) {
    val (container, content) =
        when (state) {
            StepState.Complete -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
            StepState.Active -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
            StepState.Idle -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = content.copy(alpha = 0.12f),
                contentColor = content,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun IntakeSummaryRow(
    totalDetected: Int,
    readyCount: Int,
    needsMatchCount: Int,
    duplicateCount: Int
) {
    val hasAny = totalDetected > 0 || readyCount > 0 || needsMatchCount > 0 || duplicateCount > 0
    if (!hasAny) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SummaryPill(
            label = "Detected",
            value = totalDetected,
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SummaryPill(
            label = "Ready",
            value = readyCount,
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer
        )
        SummaryPill(
            label = "Needs match",
            value = needsMatchCount,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer
        )
        SummaryPill(
            label = "Duplicates",
            value = duplicateCount,
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntakeSummaryCard(
    totalDetected: Int,
    readyCount: Int,
    needsMatchCount: Int,
    duplicateCount: Int,
    selectedCount: Int,
    selectedAmount: BigDecimal,
    onSelectMatched: () -> Unit,
    onClearSelection: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Summary", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryPill(
                    label = "Detected",
                    value = totalDetected,
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    content = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SummaryPill(
                    label = "Ready",
                    value = readyCount,
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer
                )
                SummaryPill(
                    label = "Needs match",
                    value = needsMatchCount,
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer
                )
                SummaryPill(
                    label = "Duplicates",
                    value = duplicateCount,
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selected $selectedCount - Total ${formatKes(selectedAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    TextButton(onClick = onSelectMatched) { Text("Select matched") }
                    TextButton(onClick = onClearSelection) { Text("Clear") }
                }
            }
        }
    }
}

@Composable
private fun SummaryPill(
    label: String,
    value: Int,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color
) {
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ApplyReadyBar(
    readyCount: Int,
    readyAmount: BigDecimal,
    onApply: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total ${formatKes(readyAmount)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onApply,
                enabled = readyCount > 0,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Apply")
            }
        }
    }
}




