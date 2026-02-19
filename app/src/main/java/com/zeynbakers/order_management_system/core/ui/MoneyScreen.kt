package com.zeynbakers.order_management_system.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.accounting.ui.PaymentApplySummary
import com.zeynbakers.order_management_system.accounting.ui.ManualPaymentScreen
import com.zeynbakers.order_management_system.accounting.ui.MpesaImportScreen
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeViewModel
import com.zeynbakers.order_management_system.core.ui.components.AppScreenHeaderCard
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel

enum class MoneyTab {
    Collect,
    Record
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    selectedTab: MoneyTab,
    onTabChange: (MoneyTab) -> Unit,
    paymentIntakeViewModel: PaymentIntakeViewModel,
    customerViewModel: CustomerAccountsViewModel,
    initialText: String?,
    manualCustomerId: Long?,
    onManualContextConsumed: () -> Unit,
    onManualSaved: () -> Unit,
    onApplied: (PaymentApplySummary) -> Unit,
    onAppliedInPlace: () -> Unit,
    onOpenReceiptHistory: (Long) -> Unit
) {
    val tabSubtitle =
        when (selectedTab) {
            MoneyTab.Collect -> stringResource(R.string.money_owner_subtitle_collect)
            MoneyTab.Record -> stringResource(R.string.money_owner_subtitle_record)
        }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                AppScreenHeaderCard(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    title = stringResource(R.string.money_owner_title),
                    subtitle = tabSubtitle,
                    leadingIcon = Icons.Filled.AccountBalanceWallet,
                    highlight = stringResource(R.string.money_owner_highlight)
                )
                Surface(tonalElevation = 1.dp) {
                    TabRow(selectedTabIndex = selectedTab.ordinal) {
                        Tab(
                            selected = selectedTab == MoneyTab.Collect,
                            onClick = { onTabChange(MoneyTab.Collect) },
                            text = { Text(stringResource(R.string.money_tab_collect)) }
                        )
                        Tab(
                            selected = selectedTab == MoneyTab.Record,
                            onClick = { onTabChange(MoneyTab.Record) },
                            text = { Text(stringResource(R.string.money_tab_record)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            MoneyTab.Collect -> {
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
            MoneyTab.Record -> {
                ManualPaymentScreen(
                    customerViewModel = customerViewModel,
                    initialCustomerId = manualCustomerId,
                    onContextConsumed = onManualContextConsumed,
                    onPaymentRecorded = onManualSaved,
                    showTopBar = false,
                    externalPadding = padding
                )
            }
        }
    }
}
