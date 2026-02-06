package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal

@Composable
fun OrderCard(order: OrderEntity, onSave: (OrderEntity) -> Unit) {
    var notes by remember(order.id, order.notes) { mutableStateOf(order.notes) }
    var total by remember(order.id, order.totalAmount) { mutableStateOf(order.totalAmount.toPlainString()) }
    val focusManager = LocalFocusManager.current

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        notes = it
                        onSave(order.copy(notes = it))
                    },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                    value = total,
                    onValueChange = {
                        total = it
                    },
                    label = { Text("Total") },
                    isError = total.isNotBlank() && total.toBigDecimalOrNull() == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                total.toBigDecimalOrNull()?.let { parsed ->
                                    onSave(order.copy(totalAmount = parsed))
                                }
                                focusManager.clearFocus()
                            }
                        ),
                    modifier =
                        Modifier.fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    total.toBigDecimalOrNull()?.let { parsed ->
                                        onSave(order.copy(totalAmount = parsed))
                                    }
                                }
                            }
            )
        }
    }
}
