package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.order.printing.PairedBluetoothPrinter

@Composable
internal fun BluetoothPrinterPickerDialog(
    printers: List<PairedBluetoothPrinter>,
    onDismiss: () -> Unit,
    onPrinterSelected: (PairedBluetoothPrinter) -> Unit,
    onChangePrinter: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.order_print_select_printer)) },
        text = {
            if (printers.isEmpty()) {
                Text(stringResource(R.string.order_print_no_paired_devices))
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                ) {
                    printers.forEach { printer ->
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onPrinterSelected(printer) }
                                    .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = printer.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = printer.macAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (onChangePrinter != null && printers.isNotEmpty()) {
                TextButton(onClick = onChangePrinter) {
                    Text(stringResource(R.string.order_print_change_printer))
                }
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
