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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(Unit) {
        notesRequester.requestFocus()
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
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notes (required)") },
                    placeholder = { Text("Customer details, delivery time, etc.") },
                    minLines = 2,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { totalRequester.requestFocus() }),
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

                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = totalText,
                    onValueChange = onTotalTextChange,
                    label = { Text("Total amount") },
                    placeholder = { Text("KSh 0.00") },
                    leadingIcon = { Text("KSh") },
                    isError = isTotalInvalid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { pickupRequester.requestFocus() }),
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
                    androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = pickupTimeText,
                    onValueChange = onPickupTimeChange,
                    label = { Text("Pickup time (optional)") },
                    placeholder = { Text("09:00") },
                    isError = isPickupTimeInvalid,
                    supportingText = {
                        if (isPickupTimeInvalid) {
                            Text("Use HH:MM (e.g., 09:30 or 930).")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { nameRequester.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(pickupRequester)
                )

                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                Text(text = "Customer (optional)", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = customerName,
                    onValueChange = onCustomerNameChange,
                    label = { Text("Customer name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { phoneRequester.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameRequester)
                )

                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = onCustomerPhoneChange,
                    label = { Text("Customer phone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
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
                        .focusRequester(phoneRequester)
                )

                if (suggestions.isNotEmpty()) {
                    androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
                    Text(text = "Suggestions", style = MaterialTheme.typography.labelLarge)
                    Column(
                        modifier = Modifier
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        suggestions.forEach { customer ->
                            TextButton(onClick = { onSuggestionSelected(customer) }) {
                                Text("${customer.name} - ${customer.phone}")
                            }
                        }
                    }
                }

                customerError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        TextButton(onClick = onCancel) {
                            Text("Cancel")
                        }
                        TextButton(onClick = onClear) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = null)
                            androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
                            Text("Clear")
                        }
                    }
                    Button(
                        onClick = onSave,
                        enabled = canSave,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text("Save")
                    }
                }

                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
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

