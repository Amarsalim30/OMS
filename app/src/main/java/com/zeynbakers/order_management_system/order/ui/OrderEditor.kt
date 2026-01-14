package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal

@Composable
fun OrderEditor(order: OrderEntity, onSave: (OrderEntity) -> Unit) {
    var notes by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }

    Column {
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
                    if (it.isNotBlank()) onSave(order.copy(totalAmount = BigDecimal(it)))
                },
                label = { Text("Total") },
                modifier = Modifier.fillMaxWidth()
        )
    }
}
