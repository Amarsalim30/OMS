package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.accounting.domain.ReceiptAllocation
import com.zeynbakers.order_management_system.core.ui.AmountFieldRegistry
import com.zeynbakers.order_management_system.core.ui.VoiceCalcAccess
import com.zeynbakers.order_management_system.core.ui.VoiceInputRouter
import com.zeynbakers.order_management_system.core.ui.VoiceTarget
import com.zeynbakers.order_management_system.core.util.formatDateTime
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.core.util.formatOrderLabelWithId
import com.zeynbakers.order_management_system.core.util.normalizePickupTime
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@Composable
internal fun DayOrderEditorDialog(
    isEditorOpen: Boolean,
    editingOrderId: Long?,
    orderPaidAmounts: Map<Long, BigDecimal>,
    totalText: String,
    notes: String,
    pickupTimeText: String,
    customerName: String,
    customerPhone: String,
    suggestions: List<CustomerEntity>,
    notesError: String?,
    totalError: String?,
    customerError: String?,
    formatter: NumberFormat,
    amountRegistry: AmountFieldRegistry,
    voiceCalcAccess: VoiceCalcAccess,
    voiceRouter: VoiceInputRouter,
    onSaveOrder: (String, BigDecimal, String, String, String?, Long?) -> Unit,
    onDraftChange: (OrderDraft?) -> Unit,
    onSetNotes: (String) -> Unit,
    onSetTotalText: (String) -> Unit,
    onSetCustomerName: (String) -> Unit,
    onSetCustomerPhone: (String) -> Unit,
    onSetPickupTimeText: (String) -> Unit,
    onSetEditingOrderId: (Long?) -> Unit,
    onSetSuggestions: (List<CustomerEntity>) -> Unit,
    onSetNotesError: (String?) -> Unit,
    onSetTotalError: (String?) -> Unit,
    onSetCustomerError: (String?) -> Unit,
    onSetEditorOpen: (Boolean) -> Unit
) {
    if (!isEditorOpen) return

    val paidAmount = editingOrderId?.let { orderPaidAmounts[it] } ?: BigDecimal.ZERO
    val trimmedTotal = totalText.trim()
    val parsedTotal = trimmedTotal.toBigDecimalOrNull()
    val currentTotal = parsedTotal ?: BigDecimal.ZERO
    val statusText =
        if (paidAmount >= currentTotal && currentTotal > BigDecimal.ZERO) {
            stringResource(R.string.day_status_paid)
        } else {
            stringResource(R.string.day_status_unpaid)
        }
    val remaining = if (currentTotal > paidAmount) currentTotal.subtract(paidAmount) else BigDecimal.ZERO
    val formattedTotal = parsedTotal?.let { formatter.format(it) }
    val isTotalInvalid = trimmedTotal.isNotEmpty() && parsedTotal == null
    val normalizedPickupTime =
        if (pickupTimeText.isBlank()) null else normalizePickupTime(pickupTimeText)
    val isPickupTimeInvalid = pickupTimeText.isNotBlank() && normalizedPickupTime == null
    val hasCustomerMismatch = customerName.isNotBlank() && customerPhone.isBlank()
    val notesRequiredText = stringResource(R.string.day_editor_notes_required)
    val validTotalRequiredText = stringResource(R.string.day_editor_valid_total_required)
    val phoneRequiredText = stringResource(R.string.day_editor_phone_required_for_customer)
    val canSave =
        notes.trim().isNotEmpty() &&
            parsedTotal != null &&
            parsedTotal > BigDecimal.ZERO &&
            !hasCustomerMismatch &&
            !isPickupTimeInvalid

    val notesState by rememberUpdatedState(notes)
    val setNotes by rememberUpdatedState<(String) -> Unit>(onSetNotes)
    DisposableEffect(Unit) {
        voiceRouter.registerNotesTarget(getNotes = { notesState }, setNotes = setNotes)
        onDispose { voiceRouter.clearNotesTarget() }
    }

    fun submitOrder() {
        val trimmedNotes = notes.trim()
        val finalTotal = trimmedTotal.toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP)

        when {
            trimmedNotes.isEmpty() -> {
                onSetNotesError(notesRequiredText)
                onSetTotalError(null)
                onSetCustomerError(null)
            }
            finalTotal == null || finalTotal <= BigDecimal.ZERO -> {
                onSetNotesError(null)
                onSetTotalError(validTotalRequiredText)
                onSetCustomerError(null)
            }
            hasCustomerMismatch -> {
                onSetNotesError(null)
                onSetTotalError(null)
                onSetCustomerError(phoneRequiredText)
            }
            isPickupTimeInvalid -> {
                onSetNotesError(null)
                onSetTotalError(null)
                onSetCustomerError(null)
            }
            else -> {
                onSaveOrder(
                    trimmedNotes,
                    finalTotal,
                    customerName.trim(),
                    customerPhone.trim(),
                    normalizedPickupTime,
                    editingOrderId
                )
                onSetNotes("")
                onSetTotalText("")
                onSetCustomerName("")
                onSetCustomerPhone("")
                onSetPickupTimeText("")
                onSetEditingOrderId(null)
                onSetNotesError(null)
                onSetTotalError(null)
                onSetCustomerError(null)
                onDraftChange(null)
                onSetEditorOpen(false)
            }
        }
    }

    OrderEditorSheet(
        title = if (editingOrderId == null) {
            stringResource(R.string.day_new_order)
        } else {
            stringResource(R.string.day_edit_order)
        },
        notes = notes,
        onNotesChange = {
            onSetNotes(it)
            onSetNotesError(null)
        },
        notesError = notesError,
        totalText = totalText,
        onTotalTextChange = {
            onSetTotalText(sanitizeAmountInput(it))
            onSetTotalError(null)
        },
        isTotalInvalid = isTotalInvalid,
        totalSupportingText = when {
            isTotalInvalid -> stringResource(R.string.day_editor_enter_valid_amount)
            formattedTotal != null -> stringResource(R.string.day_editor_total_preview, formattedTotal)
            else -> null
        },
        totalError = totalError,
        statusText =
            stringResource(
                R.string.day_editor_status_line,
                statusText,
                formatKes(paidAmount),
                formatKes(remaining)
            ),
        pickupTimeText = pickupTimeText,
        onPickupTimeChange = { onSetPickupTimeText(sanitizePickupTimeInput(it)) },
        isPickupTimeInvalid = isPickupTimeInvalid,
        customerName = customerName,
        onCustomerNameChange = {
            onSetCustomerName(it)
            onSetCustomerError(null)
        },
        customerPhone = customerPhone,
        onCustomerPhoneChange = {
            onSetCustomerPhone(it)
            onSetCustomerError(null)
        },
        suggestions = suggestions,
        onSuggestionSelected = { customer ->
            onSetCustomerName(customer.name)
            onSetCustomerPhone(customer.phone)
            onSetSuggestions(emptyList())
        },
        customerError = customerError,
        canSave = canSave,
        onSave = { submitOrder() },
        onClear = {
            onSetNotes("")
            onSetTotalText("")
            onSetCustomerName("")
            onSetCustomerPhone("")
            onSetPickupTimeText("")
            onSetEditingOrderId(null)
            onSetNotesError(null)
            onSetTotalError(null)
            onSetCustomerError(null)
            onDraftChange(null)
        },
        onCancel = { onSetEditorOpen(false) },
        onNotesFocused = { voiceRouter.onFocusTarget(VoiceTarget.Notes) },
        onTotalFocused = { setter ->
            amountRegistry.update(setter)
            voiceRouter.onFocusTarget(VoiceTarget.Total)
        },
        voiceHasPermission = voiceCalcAccess.hasPermission,
        onRequestVoicePermission = voiceCalcAccess.onRequestPermission
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DayDeleteOrderDialog(
    pendingDeleteOrder: OrderEntity?,
    customerNames: Map<Long, String>,
    date: LocalDate,
    deleteAllocations: List<OrderPaymentAllocationUi>,
    deleteSelection: Set<Long>,
    deleteAction: OrderPaymentAction,
    deleteMoveTarget: DeleteMoveTarget,
    deleteMoveOrderOptions: List<OrderMoveOption>,
    deleteSelectedOrderId: Long?,
    deleteMoveFullReceipts: Boolean,
    onSetPendingDeleteOrder: (OrderEntity?) -> Unit,
    onSetDeleteSelection: (Set<Long>) -> Unit,
    onSetDeleteAction: (OrderPaymentAction) -> Unit,
    onSetDeleteMoveTarget: (DeleteMoveTarget) -> Unit,
    onSetDeleteSelectedOrderId: (Long?) -> Unit,
    onSetDeleteMoveFullReceipts: (Boolean) -> Unit,
    onDeleteOrder: (Long) -> Unit,
    onDeleteOrderWithPayments: suspend (
        Long,
        LocalDate,
        List<Long>,
        OrderPaymentAction,
        ReceiptAllocation?,
        Boolean
    ) -> Boolean
) {
    val order = pendingDeleteOrder ?: return
    val scope = rememberCoroutineScope()
    val hasAllocations = deleteAllocations.isNotEmpty()
    val selectedAllocations =
        deleteAllocations.filter { allocation -> deleteSelection.contains(allocation.allocationId) }
    val targetAllocation =
        if (deleteAction == OrderPaymentAction.MOVE) {
            when (deleteMoveTarget) {
                DeleteMoveTarget.ORDER ->
                    deleteSelectedOrderId?.let { ReceiptAllocation.Order(it) }
                DeleteMoveTarget.OLDEST_ORDERS ->
                    order.customerId?.let { ReceiptAllocation.OldestOrders(it) }
                DeleteMoveTarget.CUSTOMER_CREDIT ->
                    order.customerId?.let { ReceiptAllocation.CustomerCredit(it) }
            }
        } else {
            null
        }
    val canConfirm =
        if (!hasAllocations) {
            true
        } else {
            when (deleteAction) {
                OrderPaymentAction.MOVE ->
                    selectedAllocations.isNotEmpty() && targetAllocation != null
                OrderPaymentAction.VOID -> selectedAllocations.isNotEmpty()
            }
        }

    AlertDialog(
        onDismissRequest = { onSetPendingDeleteOrder(null) },
        title = { Text(stringResource(R.string.day_delete_order_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val label =
                    formatOrderLabelWithId(
                        orderId = order.id,
                        date = order.orderDate,
                        customerName = order.customerId?.let { customerNames[it] },
                        notes = order.notes,
                        totalAmount = order.totalAmount
                    )
                Text(stringResource(R.string.day_delete_order_message, label))

                if (!hasAllocations) {
                    Text(
                        text = stringResource(R.string.day_delete_no_payments_linked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.day_delete_select_payments),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                onSetDeleteSelection(deleteAllocations.map { it.allocationId }.toSet())
                            }
                        ) { Text(stringResource(R.string.day_select_all)) }
                        TextButton(onClick = { onSetDeleteSelection(emptySet()) }) { Text(stringResource(R.string.action_clear)) }
                    }

                    Column(
                        modifier = Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        deleteAllocations.forEach { allocation ->
                            val selected = deleteSelection.contains(allocation.allocationId)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = { checked ->
                                        onSetDeleteSelection(
                                            if (checked) {
                                                deleteSelection + allocation.allocationId
                                            } else {
                                                deleteSelection - allocation.allocationId
                                            }
                                        )
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    val codeLabel =
                                        allocation.transactionCode?.let { "${allocation.method.name} $it" }
                                            ?: allocation.method.name
                                    Text(
                                        text = "${formatKes(allocation.amount)} - $codeLabel",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = formatDateTime(allocation.receivedAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val sender =
                                        allocation.senderName?.takeIf { it.isNotBlank() }
                                            ?: allocation.senderPhone?.takeIf { it.isNotBlank() }
                                    if (sender != null) {
                                        Text(
                                            text = stringResource(R.string.day_sender_from, sender),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = deleteAction == OrderPaymentAction.MOVE,
                            onClick = { onSetDeleteAction(OrderPaymentAction.MOVE) },
                            label = { Text(stringResource(R.string.day_move_payments)) }
                        )
                        FilterChip(
                            selected = deleteAction == OrderPaymentAction.VOID,
                            onClick = { onSetDeleteAction(OrderPaymentAction.VOID) },
                            label = { Text(stringResource(R.string.day_delete_payments)) }
                        )
                    }

                    if (deleteAction == OrderPaymentAction.MOVE) {
                        if (order.customerId == null) {
                            Text(
                                text = stringResource(R.string.day_delete_no_customer_target),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = deleteMoveTarget == DeleteMoveTarget.ORDER,
                                onClick = { onSetDeleteMoveTarget(DeleteMoveTarget.ORDER) },
                                label = { Text(stringResource(R.string.day_target_order)) }
                            )
                            if (order.customerId != null) {
                                FilterChip(
                                    selected = deleteMoveTarget == DeleteMoveTarget.OLDEST_ORDERS,
                                    onClick = { onSetDeleteMoveTarget(DeleteMoveTarget.OLDEST_ORDERS) },
                                    label = { Text(stringResource(R.string.day_target_oldest_orders)) }
                                )
                                FilterChip(
                                    selected = deleteMoveTarget == DeleteMoveTarget.CUSTOMER_CREDIT,
                                    onClick = { onSetDeleteMoveTarget(DeleteMoveTarget.CUSTOMER_CREDIT) },
                                    label = { Text(stringResource(R.string.day_target_customer_credit)) }
                                )
                            }
                        }

                        if (deleteMoveTarget == DeleteMoveTarget.ORDER) {
                            if (deleteMoveOrderOptions.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.day_no_other_orders_available),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    deleteMoveOrderOptions.forEach { option ->
                                        FilterChip(
                                            selected = deleteSelectedOrderId == option.orderId,
                                            onClick = { onSetDeleteSelectedOrderId(option.orderId) },
                                            label = { Text(option.label) }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = deleteMoveFullReceipts,
                                onCheckedChange = { onSetDeleteMoveFullReceipts(it) }
                            )
                            Text(
                                text = stringResource(R.string.day_move_full_receipts),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val allocationIds = selectedAllocations.map { it.allocationId }
                    scope.launch {
                        if (!hasAllocations) {
                            onDeleteOrder(order.id)
                        } else {
                            onDeleteOrderWithPayments(
                                order.id,
                                date,
                                allocationIds,
                                deleteAction,
                                targetAllocation,
                                deleteMoveFullReceipts
                            )
                        }
                        onSetPendingDeleteOrder(null)
                    }
                },
                enabled = canConfirm
            ) {
                Text(stringResource(R.string.day_delete_order))
            }
        },
        dismissButton = {
            TextButton(onClick = { onSetPendingDeleteOrder(null) }) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
