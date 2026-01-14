package com.zeynbakers.order_management_system.order.ui


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.order.data.*
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("unused")
@Composable
fun OrderEditorSheet(
    date: LocalDate,
    orders: List<OrderEntity>,
    onSave: (OrderEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {

            Text(
                text = "Orders for $date",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(8.dp))

            if (orders.isEmpty()) {
                Text(
                    text = "No orders yet.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(orders) { order ->
                        OrderCard(order, onSave)
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "New Order",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

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
