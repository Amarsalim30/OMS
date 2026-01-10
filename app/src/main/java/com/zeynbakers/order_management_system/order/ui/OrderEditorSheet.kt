package com.zeynbakers.order_management_system.order.ui


import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.order.data.*
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

@Composable
fun OrderEditorSheet(
    date: LocalDate,
    orders: List<OrderEntity>,
    onSave: (OrderEntity) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {

        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = "Orders for ${date}",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(8.dp))

            orders.forEach { order ->
                OrderCard(order, onSave)
            }

            Divider()

            Text(
                text = "New Order",
                style = MaterialTheme.typography.titleMedium
            )

            OrderEditor(
                order = OrderEntity(
                    orderDate = date,
                    notes = "",
                    totalAmount = BigDecimal.ZERO
                ),
                onSave = onSave
            )
        }
    }
}
