package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.onFocusChanged
import com.zeynbakers.order_management_system.core.ui.LocalAmountFieldRegistry
import com.zeynbakers.order_management_system.core.ui.LocalVoiceInputRouter
import com.zeynbakers.order_management_system.core.ui.VoiceTarget
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OrderEditor(order: OrderEntity, onSave: (OrderEntity) -> Unit) {
    var notes by rememberSaveable { mutableStateOf(order.notes) }
    var totalText by rememberSaveable {
        mutableStateOf(
            if (order.totalAmount == BigDecimal.ZERO) "" else order.totalAmount.toPlainString()
        )
    }
    val amountRegistry = LocalAmountFieldRegistry.current
    val voiceRouter = LocalVoiceInputRouter.current

    LaunchedEffect(order) {
        notes = order.notes
        totalText = if (order.totalAmount == BigDecimal.ZERO) "" else order.totalAmount.toPlainString()
    }

    val trimmedTotal = totalText.trim()
    val totalAmount = trimmedTotal.toBigDecimalOrNull()
    val isTotalInvalid = trimmedTotal.isNotEmpty() && totalAmount == null
    val canSave = totalAmount != null
    val formatter = remember {
        NumberFormat.getNumberInstance(Locale.forLanguageTag("en-KE")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    val formattedTotal = totalAmount?.let { formatter.format(it) }

    val notesState by rememberUpdatedState(notes)
    val setNotes by rememberUpdatedState<(String) -> Unit>({ notes = it })
    DisposableEffect(Unit) {
        voiceRouter.registerNotesTarget(getNotes = { notesState }, setNotes = setNotes)
        onDispose { voiceRouter.clearNotesTarget() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
                value = notes,
                onValueChange = {
                    notes = it
                },
                label = { Text("Notes") },
                placeholder = { Text("Customer details, delivery time, etc.") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            voiceRouter.onFocusTarget(VoiceTarget.Notes)
                        }
                    }
        )

        val setTotalText by rememberUpdatedState<(String) -> Unit>({ totalText = it })
        OutlinedTextField(
                value = totalText,
                onValueChange = {
                    val filtered = it.filter { ch -> ch.isDigit() || ch == '.' }
                    if (filtered.count { ch -> ch == '.' } <= 1) {
                        totalText = filtered
                    }
                },
                label = { Text("Total amount") },
                placeholder = { Text("KSh 0.00") },
                leadingIcon = { Text("KSh") },
                isError = isTotalInvalid,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val finalAmount = totalAmount?.setScale(2, RoundingMode.HALF_UP)
                        if (finalAmount != null) {
                            onSave(order.copy(notes = notes.trim(), totalAmount = finalAmount))
                            notes = ""
                            totalText = ""
                        }
                    }
                ),
                supportingText = {
                    when {
                        isTotalInvalid -> Text("Enter a valid amount.")
                        formattedTotal != null -> Text("Total: KSh $formattedTotal")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            amountRegistry.update(setTotalText)
                            voiceRouter.onFocusTarget(VoiceTarget.Total)
                        }
                    }
        )

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val finalAmount = totalAmount?.setScale(2, RoundingMode.HALF_UP) ?: return@Button
                    onSave(order.copy(notes = notes.trim(), totalAmount = finalAmount))
                    notes = ""
                    totalText = ""
                },
                enabled = canSave,
                modifier = Modifier.weight(1f)
            ) {
                Text("Add order")
            }

            TextButton(
                onClick = {
                    notes = ""
                    totalText = ""
                }
            ) {
                Text("Clear")
            }
        }
    }
}
