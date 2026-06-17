package com.zeynbakers.order_management_system.order.ui.order_editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.ui.common.OrderItemDraft
import com.zeynbakers.order_management_system.order.ui.order_editor.components.AddProductBottomSheet
import com.zeynbakers.order_management_system.order.ui.order_editor.components.OrderCartSummary
import com.zeynbakers.order_management_system.order.ui.order_editor.components.OrderEditorCustomerSection
import com.zeynbakers.order_management_system.product.data.ProductEntity
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

private enum class EditingRow {
    NONE,
    NOTES,
    TOTAL
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun OrderEditorSheet(
    title: String,
    cartItems: List<OrderItemDraft>,
    onCartItemsChange: (List<OrderItemDraft>) -> Unit,
    notesError: String?,
    totalText: String,
    onTotalTextChange: (String) -> Unit,
    isTotalInvalid: Boolean,
    totalSupportingText: String?,
    totalError: String?,
    statusText: String?,
    pickupTimeText: String,
    onPickupTimeChange: (String) -> Unit,
    isPickupTimeInvalid: Boolean,
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    customerPhone: String,
    onCustomerPhoneChange: (String) -> Unit,
    suggestions: List<CustomerEntity>,
    onSuggestionSelected: (CustomerEntity) -> Unit,
    customerConfirmed: Boolean,
    onCustomerConfirmedChange: (Boolean) -> Unit,
    onCreateCustomerFromQuery: (String) -> Unit,
    productMatches: List<ProductEntity>,
    onProductQueryChange: (String) -> Unit,
    onEnsureProduct: suspend (String, BigDecimal, String) -> ProductEntity,
    customerError: String?,
    canSave: Boolean,
    onSave: () -> Unit,
    focusNotesInitially: Boolean,
    onClear: () -> Unit,
    onCancel: () -> Unit,
    onNotesFocused: () -> Unit,
    onTotalFocused: ((String) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    customerFieldModifier: Modifier = Modifier,
    notesFieldModifier: Modifier = Modifier,
    totalFieldModifier: Modifier = Modifier,
    saveButtonModifier: Modifier = Modifier,
    tutorialHint: OrderEditorTutorialHint? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val imeVisibleState by rememberUpdatedState(imeVisible)
    val keyboardControllerState by rememberUpdatedState(keyboardController)
    val focusManagerState by rememberUpdatedState(focusManager)
    val notesRequester = remember { FocusRequester() }
    val totalRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val setTotalText by rememberUpdatedState<(String) -> Unit>({ onTotalTextChange(it) })
    val formScrollState = rememberScrollState()

    val cartTotal =
        remember(cartItems) { cartItems.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.lineTotal) } }
    var showAddProductSheet by remember { mutableStateOf(false) }

    val hasAnyInput =
        cartItems.isNotEmpty() ||
                pickupTimeText.isNotBlank() ||
                customerName.isNotBlank() ||
                customerPhone.isNotBlank() ||
                customerConfirmed

    val quickAmountAdds = remember { listOf(100, 500, 1000) }
    val quickAmountFormatter = remember { NumberFormat.getIntegerInstance() }
    val quickPickupTimes = remember { listOf("09:00", "12:00", "15:00", "18:00") }
    val quickChipBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
    val quickChipColors =
        FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            selectedContainerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedLabelColor = MaterialTheme.colorScheme.onSurface
        )
    val normalizedTotal = totalText.toBigDecimalOrNull()
    val totalSummaryFormatter = remember {
        NumberFormat.getNumberInstance().apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    val currencyPrefix = stringResource(R.string.order_editor_currency_prefix)

    val (initialHour, initialMinute) = remember(pickupTimeText) { parseTimeForPicker(pickupTimeText) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var editingRow by rememberSaveable { mutableStateOf(EditingRow.NONE) }

    LaunchedEffect(focusNotesInitially) {
        if (focusNotesInitially) {
            editingRow = EditingRow.NOTES
            notesRequester.requestFocus()
        }
    }

    val handleBackPress: () -> Unit = {
        when {
            imeVisibleState -> {
                keyboardControllerState?.hide()
                focusManagerState.clearFocus(force = true)
            }

            else -> onCancel()
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler(onBack = handleBackPress)
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 6.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { if (hasAnyInput) showClearConfirm = true },
                        enabled = hasAnyInput
                    ) {
                        Text(stringResource(R.string.action_clear))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .verticalScroll(formScrollState)
                ) {
                    tutorialHint?.let {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                            OrderEditorTutorialPanel(it)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    OrderEditorCustomerSection(
                        customerName = customerName,
                        onCustomerNameChange = onCustomerNameChange,
                        customerPhone = customerPhone,
                        onCustomerPhoneChange = onCustomerPhoneChange,
                        customerConfirmed = customerConfirmed,
                        onCustomerConfirmedChange = onCustomerConfirmedChange,
                        suggestions = suggestions,
                        onSuggestionSelected = onSuggestionSelected,
                        onCreateCustomerFromQuery = onCreateCustomerFromQuery,
                        modifier = customerFieldModifier
                    )

                    OrderCartSummary(
                        cartItems = cartItems,
                        onCartItemsChange = onCartItemsChange,
                        onAddProductClick = { showAddProductSheet = true },
                        modifier = notesFieldModifier
                    )

                    notesError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    customerError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 52.dp, end = 16.dp, bottom = 8.dp)
                        )
                    }

                    statusText?.takeIf { it.isNotBlank() }?.let {
                        OutlinedCard(
                            modifier = Modifier
                                .padding(start = 52.dp, end = 16.dp, bottom = 12.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    totalError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    ValueRow(
                        icon = Icons.Filled.Schedule,
                        value = formatDisplayPickupTime(pickupTimeText),
                        placeholder = stringResource(R.string.order_editor_pick_time),
                        onClick = {
                            showTimePicker = true
                            editingRow = EditingRow.NONE
                            focusManager.clearFocus(force = true)
                        }
                    )

                    if (isPickupTimeInvalid) {
                        Text(
                            text = stringResource(R.string.order_editor_pickup_time_hint),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 52.dp, end = 16.dp, bottom = 8.dp)
                        )
                    }

                    FlowRow(
                        modifier = Modifier.padding(start = 52.dp, end = 16.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickPickupTimes.forEach { value ->
                            val isSelected = isSamePickupTime(pickupTimeText, value)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onPickupTimeChange(value)
                                    editingRow = EditingRow.NONE
                                    focusManager.clearFocus(force = true)
                                },
                                label = { Text(formatDisplayPickupTime(value)) },
                                border = quickChipBorder,
                                colors = quickChipColors
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.order_editor_footer_total,
                            formatKes(cartTotal)
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = onSave,
                            enabled = canSave,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(),
                            modifier =
                                saveButtonModifier
                                    .fillMaxWidth(0.55f)
                                    .sizeIn(minHeight = 50.dp)
                        ) {
                            Text(stringResource(R.string.order_editor_save_order))
                        }
                    }
                }
            }

            AddProductBottomSheet(
                visible = showAddProductSheet,
                cartItems = cartItems,
                onCartItemsChange = onCartItemsChange,
                productMatches = productMatches,
                onProductQueryChange = onProductQueryChange,
                onEnsureProduct = onEnsureProduct,
                onDismiss = { showAddProductSheet = false }
            )

            if (showTimePicker) {
                val timePickerState = rememberTimePickerState(
                    initialHour = initialHour,
                    initialMinute = initialMinute,
                    is24Hour = false
                )
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    title = { Text(stringResource(R.string.order_editor_pick_time)) },
                    text = { TimePicker(state = timePickerState) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onPickupTimeChange(
                                    formatPickerTime(
                                        hour = timePickerState.hour,
                                        minute = timePickerState.minute
                                    )
                                )
                                showTimePicker = false
                            }
                        ) {
                            Text(stringResource(R.string.action_save))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }

            if (showClearConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearConfirm = false },
                    title = { Text(stringResource(R.string.order_editor_clear_form_title)) },
                    text = { Text(stringResource(R.string.order_editor_clear_form_message)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                onClear()
                                showClearConfirm = false
                            }
                        ) {
                            Text(stringResource(R.string.order_editor_clear_form_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirm = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ValueRow(
    icon: ImageVector?,
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    textMaxLines: Int = 1,
    modifier: Modifier = Modifier
) {
    val hasValue = value.isNotBlank()
    val isMultiline = textMaxLines > 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .clickable(onClick = onClick)
            .then(modifier)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = if (isMultiline) Alignment.Top else Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = if (hasValue) value else placeholder,
            style = MaterialTheme.typography.bodyLarge,
            color = if (hasValue) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            minLines = 1,
            maxLines = textMaxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        // Keep a fixed trailing slot so switching to inline edit does not shrink text width.
        Box(
            modifier =
                if (isMultiline) {
                    Modifier.defaultMinSize(minWidth = 48.dp)
                } else {
                    Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                }
        )
    }
}

@Composable
private fun InlineEditorRow(
    icon: ImageVector?,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
    onFocused: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    leadingText: String? = null,
    textMaxLines: Int = 1,
    readOnly: Boolean = false,
    onRowClick: (() -> Unit)? = null
) {
    val isMultiline = textMaxLines > 1
    val textStyle: TextStyle =
        MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    val cursorBrush = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary
        )
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .then(
                if (readOnly && onRowClick != null) {
                    Modifier.clickable(onClick = onRowClick)
                } else {
                    Modifier
                }
            )
            .then(modifier)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = if (isMultiline) Alignment.Top else Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
        }

        leadingText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = textMaxLines == 1,
            minLines = 1,
            maxLines = textMaxLines,
            readOnly = readOnly,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            cursorBrush = cursorBrush,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (!readOnly && it.isFocused) {
                        onFocused?.invoke()
                    }
                },
            decorationBox = { innerTextField ->
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                innerTextField()
            }
        )

        Box(
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
            contentAlignment = if (isMultiline) Alignment.TopCenter else Alignment.Center
        ) {
            if (!readOnly && onClear != null && value.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_clear)
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderEditorTutorialPanel(hint: OrderEditorTutorialHint) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = hint.stepText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = hint.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = hint.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (hint.showContinue) {
                Button(
                    onClick = hint.onContinue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(hint.continueLabel)
                }
            }
            TextButton(
                onClick = hint.onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(hint.skipLabel)
            }
        }
    }
}

private fun parseTimeForPicker(value: String): Pair<Int, Int> {
    return parseTimePartsOrNull(value) ?: defaultPickerTime()
}

private fun formatPickerTime(hour: Int, minute: Int): String {
    return String.format(Locale.US, "%02d:%02d", hour, minute)
}

private fun parseTimePartsOrNull(value: String): Pair<Int, Int>? {
    val raw = value.trim()
    if (raw.isBlank()) return null

    val normalized = raw.replace('.', ':')
    if (':' in normalized) {
        val parts = normalized.split(':')
        if (parts.size == 2) {
            val hour = parts[0].toIntOrNull()
            val minute = parts[1].toIntOrNull()
            if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                return hour to minute
            }
        }
    }

    if (raw.all { it.isDigit() } && raw.length in 3..4) {
        val digits = raw.padStart(4, '0')
        val hour = digits.substring(0, 2).toIntOrNull()
        val minute = digits.substring(2, 4).toIntOrNull()
        if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
            return hour to minute
        }
    }
    return null
}

private fun formatDisplayPickupTime(value: String): String {
    val parts = parseTimePartsOrNull(value) ?: return value
    val hour24 = parts.first
    val minute = parts.second
    val amPm = if (hour24 < 12) "AM" else "PM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm)
}

private fun isSamePickupTime(left: String, right: String): Boolean {
    return parseTimePartsOrNull(left) == parseTimePartsOrNull(right)
}

private fun defaultPickerTime(): Pair<Int, Int> {
    val calendar = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 2) }
    val minute = calendar.get(Calendar.MINUTE)
    val roundedMinute = ((minute + 14) / 15) * 15
    if (roundedMinute >= 60) {
        calendar.add(Calendar.HOUR_OF_DAY, 1)
    }
    val finalHour = calendar.get(Calendar.HOUR_OF_DAY)
    val finalMinute = if (roundedMinute >= 60) 0 else roundedMinute
    return finalHour to finalMinute
}
