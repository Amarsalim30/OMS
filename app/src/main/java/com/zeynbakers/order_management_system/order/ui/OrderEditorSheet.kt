package com.zeynbakers.order_management_system.order.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun OrderEditorSheet(
    title: String,
    notes: String,
    onNotesChange: (String) -> Unit,
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
    customerError: String?,
    canSave: Boolean,
    onSave: () -> Unit,
    focusNotesInitially: Boolean,
    onClear: () -> Unit,
    onCancel: () -> Unit,
    onNotesFocused: () -> Unit,
    onTotalFocused: ((String) -> Unit) -> Unit,
    customerFieldModifier: Modifier = Modifier,
    notesFieldModifier: Modifier = Modifier,
    totalFieldModifier: Modifier = Modifier,
    saveButtonModifier: Modifier = Modifier
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
    val pickupRequester = remember { FocusRequester() }
    val nameRequester = remember { FocusRequester() }
    val setTotalText by rememberUpdatedState<(String) -> Unit>({ onTotalTextChange(it) })
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val formScrollState = rememberScrollState()
    val hasAnyInput =
        notes.isNotBlank() ||
            totalText.isNotBlank() ||
            pickupTimeText.isNotBlank() ||
            customerName.isNotBlank() ||
            customerPhone.isNotBlank()
    val quickAmountAdds = remember { listOf(100, 500, 1000) }
    val quickAmountFormatter = remember { NumberFormat.getIntegerInstance() }
    val quickPickupTimes = remember { listOf("09:00", "12:00", "15:00", "18:00") }
    val normalizedTotal = totalText.toBigDecimalOrNull()
    val totalSummaryFormatter = remember {
        NumberFormat.getNumberInstance().apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    val formattedTotalSummary = normalizedTotal?.let { totalSummaryFormatter.format(it) }
    val (initialHour, initialMinute) = remember(pickupTimeText) { parseTimeForPicker(pickupTimeText) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(focusNotesInitially) {
        if (focusNotesInitially) {
            notesRequester.requestFocus()
        } else {
            nameRequester.requestFocus()
        }
    }

    val dismissSheet = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onCancel()
    }

    val handleBackPress: () -> Unit = {
        if (imeVisibleState) {
            keyboardControllerState?.hide()
            focusManagerState.clearFocus(force = true)
        } else {
            onCancel()
        }
    }

    ModalBottomSheet(
        onDismissRequest = dismissSheet,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
    ) {
        BackHandler(onBack = handleBackPress)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = dismissSheet) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_cancel)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .verticalScroll(formScrollState)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EditorSectionCard(
                        title = stringResource(R.string.order_editor_customer_schedule_section)
                    ) {
                        val hasSelectedCustomer = customerName.isNotBlank() && customerPhone.isNotBlank()
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = {
                                onCustomerNameChange(it)
                                if (customerPhone.isNotBlank()) {
                                    onCustomerPhoneChange("")
                                }
                            },
                            label = { Text(stringResource(R.string.order_editor_customer_search_label)) },
                            placeholder = { Text(stringResource(R.string.order_editor_customer_search_placeholder)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { pickupRequester.requestFocus() }),
                            trailingIcon = if (customerName.isNotEmpty()) {
                                {
                                    IconButton(
                                        onClick = {
                                            onCustomerNameChange("")
                                            onCustomerPhoneChange("")
                                        }
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
                                    }
                                }
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(nameRequester)
                                .then(customerFieldModifier)
                        )

                        if (hasSelectedCustomer) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = customerName.trim(),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = customerPhone.trim(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            onCustomerPhoneChange("")
                                            nameRequester.requestFocus()
                                        }
                                    ) {
                                        Text(stringResource(R.string.action_change))
                                    }
                                }
                            }
                        }

                        if (suggestions.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.order_editor_suggestions_label),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                suggestions.take(4).forEach { customer ->
                                    Surface(
                                        onClick = { onSuggestionSelected(customer) },
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.surfaceContainerLow
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = customer.name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (customer.phone.isNotBlank()) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = customer.phone,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        customerError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }

                        OutlinedTextField(
                            value = formatDisplayPickupTime(pickupTimeText),
                            onValueChange = onPickupTimeChange,
                            label = { Text(stringResource(R.string.order_editor_pickup_short_label)) },
                            placeholder = { Text("--:--") },
                            readOnly = true,
                            isError = isPickupTimeInvalid,
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (pickupTimeText.isNotEmpty()) {
                                        IconButton(onClick = { onPickupTimeChange("") }) {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = stringResource(R.string.action_clear)
                                            )
                                        }
                                    }
                                    IconButton(onClick = { showTimePicker = true }) {
                                        Icon(
                                            imageVector = Icons.Filled.AccessTime,
                                            contentDescription = stringResource(R.string.order_editor_pick_time)
                                        )
                                    }
                                }
                            },
                            supportingText = {
                                if (isPickupTimeInvalid) {
                                    Text(stringResource(R.string.order_editor_pickup_time_hint))
                                } else if (pickupTimeText.isBlank()) {
                                    Text(stringResource(R.string.order_editor_pick_time))
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.None),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(pickupRequester)
                                .onFocusChanged { state ->
                                    if (state.isFocused) {
                                        showTimePicker = true
                                        focusManager.clearFocus(force = true)
                                    }
                                }
                        )

                        Text(
                            text = stringResource(R.string.order_editor_quick_pickup_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickPickupTimes.forEach { value ->
                                val isSelected = isSamePickupTime(pickupTimeText, value)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onPickupTimeChange(value) },
                                    label = { Text(formatDisplayPickupTime(value)) }
                                )
                            }
                        }
                    }

                    EditorSectionCard(
                        title = stringResource(R.string.order_editor_required_section_label)
                    ) {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { onNotesChange(sanitizeOrderNotesInput(it)) },
                            label = { Text(stringResource(R.string.order_editor_notes_required_label)) },
                            placeholder = { Text(stringResource(R.string.order_editor_notes_placeholder)) },
                            minLines = 2,
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { totalRequester.requestFocus() }),
                            trailingIcon = if (notes.isNotEmpty()) {
                                {
                                    IconButton(onClick = { onNotesChange("") }) {
                                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
                                    }
                                }
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(notesRequester)
                                .onFocusChanged { state ->
                                    if (state.isFocused) {
                                        onNotesFocused()
                                    }
                                }
                                .then(notesFieldModifier)
                        )
                        notesError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }

                        OutlinedTextField(
                            value = totalText,
                            onValueChange = onTotalTextChange,
                            label = { Text(stringResource(R.string.order_editor_total_amount_label)) },
                            placeholder = { Text(stringResource(R.string.order_editor_total_placeholder)) },
                            leadingIcon = { Text(stringResource(R.string.order_editor_currency_prefix)) },
                            trailingIcon = if (totalText.isNotEmpty()) {
                                {
                                    IconButton(onClick = { onTotalTextChange("") }) {
                                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
                                    }
                                }
                            } else null,
                            isError = isTotalInvalid,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (canSave) {
                                        onSave()
                                    }
                                }
                            ),
                            supportingText = {
                                totalSupportingText?.let { Text(it) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(totalRequester)
                                .onFocusChanged { state ->
                                    if (state.isFocused) {
                                        onTotalFocused(setTotalText)
                                    }
                                }
                                .then(totalFieldModifier)
                        )

                        totalError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }

                        statusText?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = stringResource(R.string.order_editor_quick_amounts_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickAmountAdds.forEach { amount ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        val updated = (normalizedTotal ?: BigDecimal.ZERO)
                                            .add(amount.toBigDecimal())
                                            .stripTrailingZeros()
                                            .toPlainString()
                                        onTotalTextChange(updated)
                                    },
                                    label = {
                                        Text(
                                            stringResource(
                                                R.string.order_editor_quick_amount_chip,
                                                quickAmountFormatter.format(amount)
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        formattedTotalSummary?.let {
                            Text(
                                text = stringResource(R.string.order_editor_footer_total, it),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                if (hasAnyInput) {
                                    showClearConfirm = true
                                } else {
                                    onClear()
                                }
                            },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.action_clear)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_clear))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onSave,
                        enabled = canSave,
                        modifier = saveButtonModifier
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
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
                                notesRequester.requestFocus()
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

private fun parseTimeForPicker(value: String): Pair<Int, Int> {
    return parseTimePartsOrNull(value) ?: defaultPickerTime()
}

private fun formatPickerTime(hour: Int, minute: Int): String {
    return String.format("%02d:%02d", hour, minute)
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
    return String.format("%d:%02d %s", hour12, minute, amPm)
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

@Composable
private fun EditorSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}
