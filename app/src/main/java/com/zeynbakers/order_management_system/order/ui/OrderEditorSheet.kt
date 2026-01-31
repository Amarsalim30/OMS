package com.zeynbakers.order_management_system.order.ui


import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.zeynbakers.order_management_system.order.data.*
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

private const val TAG_SHEET_BACK = "SheetBack"

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
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val imeVisibleState by rememberUpdatedState(imeVisible)
    val keyboardControllerState by rememberUpdatedState(keyboardController)
    val focusManagerState by rememberUpdatedState(focusManager)

    val dismissSheet = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onDismiss()
    }
    val handleBackPress: () -> Unit = {
        val sheetOpen = true
        if (Log.isLoggable(TAG_SHEET_BACK, Log.DEBUG)) {
            Log.d(TAG_SHEET_BACK, "back pressed imeVisible=$imeVisibleState sheetOpen=$sheetOpen")
        }
        if (imeVisibleState) {
            keyboardControllerState?.hide()
            focusManagerState.clearFocus(force = true)
        } else {
            onDismiss()
        }
    }
    val handleDismissRequest: () -> Unit = {
        if (Log.isLoggable(TAG_SHEET_BACK, Log.DEBUG)) {
            Log.d(TAG_SHEET_BACK, "dismiss request imeVisible=$imeVisible sheetOpen=true")
        }
        dismissSheet()
    }
    ModalBottomSheet(
        onDismissRequest = handleDismissRequest,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
    ) {
        BackHandler(onBack = handleBackPress)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .imePadding()
                .navigationBarsPadding()
                .padding(12.dp)
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

