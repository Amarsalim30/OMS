package com.zeynbakers.order_management_system.order.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.zeynbakers.order_management_system.core.ui.VoiceCalculatorOverlay
import com.zeynbakers.order_management_system.customer.data.CustomerEntity

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
    onClear: () -> Unit,
    onClearOptional: () -> Unit,
    onCancel: () -> Unit,
    onNotesFocused: () -> Unit,
    onTotalFocused: ((String) -> Unit) -> Unit,
    voiceHasPermission: Boolean,
    onRequestVoicePermission: () -> Unit
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
    val phoneRequester = remember { FocusRequester() }
    val setTotalText by rememberUpdatedState<(String) -> Unit>({ onTotalTextChange(it) })
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val formScrollState = rememberScrollState()
    val hasAnyInput =
        notes.isNotBlank() ||
            totalText.isNotBlank() ||
            pickupTimeText.isNotBlank() ||
            customerName.isNotBlank() ||
            customerPhone.isNotBlank()
    val hasOptionalInput =
        pickupTimeText.isNotBlank() ||
            customerName.isNotBlank() ||
            customerPhone.isNotBlank()
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        nameRequester.requestFocus()
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
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.order_editor_customer_optional_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = onCustomerNameChange,
                        label = { Text(stringResource(R.string.order_editor_customer_name_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { phoneRequester.requestFocus() }),
                        trailingIcon = if (customerName.isNotEmpty()) {
                            {
                                IconButton(onClick = { onCustomerNameChange("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
                                }
                            }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nameRequester)
                    )

                    if (suggestions.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.order_editor_suggestions_label),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.height(4.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            suggestions.take(6).forEach { customer ->
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

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = onCustomerPhoneChange,
                        label = { Text(stringResource(R.string.order_editor_customer_phone_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { notesRequester.requestFocus() }),
                        trailingIcon = if (customerPhone.isNotEmpty()) {
                            {
                                IconButton(onClick = { onCustomerPhoneChange("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
                                }
                            }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(phoneRequester)
                    )

                    customerError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.order_editor_required_section_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = onNotesChange,
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
                    )

                    notesError?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(Modifier.height(8.dp))

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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { pickupRequester.requestFocus() },
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
                    )

                    totalError?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                    }

                    statusText?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pickupTimeText,
                        onValueChange = onPickupTimeChange,
                        label = { Text(stringResource(R.string.order_editor_pickup_time_optional_label)) },
                        placeholder = { Text(stringResource(R.string.order_editor_pickup_time_placeholder)) },
                        isError = isPickupTimeInvalid,
                        trailingIcon = if (pickupTimeText.isNotEmpty()) {
                            {
                                IconButton(onClick = { onPickupTimeChange("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
                                }
                            }
                        } else null,
                        supportingText = {
                            if (isPickupTimeInvalid) {
                                Text(stringResource(R.string.order_editor_pickup_time_hint))
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(pickupRequester)
                    )

                    Spacer(Modifier.height(12.dp))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    if (hasOptionalInput) {
                        TextButton(onClick = onClearOptional) {
                            Text(stringResource(R.string.order_editor_clear_optional))
                        }
                    }
                    TextButton(
                        onClick = {
                            if (hasAnyInput) {
                                showClearConfirm = true
                            } else {
                                onClear()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_clear)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_clear))
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onSave,
                        enabled = canSave
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
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

            VoiceCalculatorOverlay(
                hasPermission = voiceHasPermission,
                onRequestPermission = onRequestVoicePermission,
                lockToRightOnIdle = true,
                lockToTopOnIdle = true,
                peekWidthDp = 18.dp,
                allowDrag = true
            )
        }
    }
}
