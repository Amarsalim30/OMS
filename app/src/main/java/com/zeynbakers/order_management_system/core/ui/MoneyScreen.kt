package com.zeynbakers.order_management_system.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.zeynbakers.order_management_system.accounting.ui.LedgerViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentApplySummary
import com.zeynbakers.order_management_system.accounting.ui.ManualPaymentScreen
import com.zeynbakers.order_management_system.accounting.ui.MpesaImportScreen
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeViewModel
import com.zeynbakers.order_management_system.accounting.ui.CustomerStatementsScreen
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel

enum class MoneyTab {
    Mpesa,
    Manual,
    Ledger
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    selectedTab: MoneyTab,
    onTabChange: (MoneyTab) -> Unit,
    paymentIntakeViewModel: PaymentIntakeViewModel,
    customerViewModel: CustomerAccountsViewModel,
    ledgerViewModel: LedgerViewModel,
    initialText: String?,
    manualCustomerId: Long?,
    statementCustomerId: Long?,
    onManualContextConsumed: () -> Unit,
    onStatementContextConsumed: () -> Unit,
    onManualSaved: () -> Unit,
    onApplied: (PaymentApplySummary) -> Unit,
    onAppliedInPlace: () -> Unit,
    onOpenReceiptHistory: (Long) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column(modifier = androidx.compose.ui.Modifier.statusBarsPadding()) {
                TabRow(selectedTabIndex = selectedTab.ordinal) {
                    Tab(
                        selected = selectedTab == MoneyTab.Mpesa,
                        onClick = { onTabChange(MoneyTab.Mpesa) },
                        text = { Text("M-PESA") }
                    )
                    Tab(
                        selected = selectedTab == MoneyTab.Manual,
                        onClick = { onTabChange(MoneyTab.Manual) },
                        text = { Text("Manual") }
                    )
                    Tab(
                        selected = selectedTab == MoneyTab.Ledger,
                        onClick = { onTabChange(MoneyTab.Ledger) },
                        text = { Text("Statements") }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            MoneyTab.Mpesa -> {
                MpesaImportScreen(
                    viewModel = paymentIntakeViewModel,
                    initialText = initialText,
                    onClose = {},
                    onApplied = onApplied,
                    onAppliedInPlace = onAppliedInPlace,
                    onOpenReceiptHistory = onOpenReceiptHistory,
                    showTopBar = false,
                    externalPadding = padding
                )
            }
            MoneyTab.Manual -> {
                ManualPaymentScreen(
                    customerViewModel = customerViewModel,
                    initialCustomerId = manualCustomerId,
                    onContextConsumed = onManualContextConsumed,
                    onPaymentRecorded = onManualSaved,
                    showTopBar = false,
                    externalPadding = padding
                )
            }
            MoneyTab.Ledger -> {
                CustomerStatementsScreen(
                    customerViewModel = customerViewModel,
                    ledgerViewModel = ledgerViewModel,
                    initialCustomerId = statementCustomerId,
                    onContextConsumed = onStatementContextConsumed,
                    externalPadding = padding
                )
            }
        }
    }
}
