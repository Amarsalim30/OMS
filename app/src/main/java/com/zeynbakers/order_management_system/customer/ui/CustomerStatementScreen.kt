package com.zeynbakers.order_management_system.customer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.components.AppEmptyState
import com.zeynbakers.order_management_system.core.util.formatDateTime
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerStatementScreen(
    customer: CustomerEntity?,
    balance: BigDecimal,
    financeSummary: CustomerFinanceSummary?,
    rows: List<CustomerStatementRowUi>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onAddOrder: () -> Unit,
    onRecordPayment: () -> Unit,
    onMarkBadDebt: (BigDecimal, String) -> Unit
) {
    val netBalance = financeSummary?.netBalance ?: balance
    val availableCredit = (financeSummary?.availableCredit ?: BigDecimal.ZERO).max(BigDecimal.ZERO)
    var showBadDebtDialog by remember { mutableStateOf(false) }
    var badDebtAmount by remember(netBalance) { mutableStateOf(netBalance.max(BigDecimal.ZERO).stripTrailingZeros().toPlainString()) }
    var badDebtNote by remember { mutableStateOf("") }
    var activePaymentDetails by remember { mutableStateOf<CustomerPaymentAllocationDetailsUi?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.customer_statement_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                StatementHeaderCard(
                    customerName = customer?.name.orEmpty(),
                    balance = netBalance,
                    availableCredit = availableCredit,
                    onAddOrder = onAddOrder,
                    onRecordPayment = onRecordPayment,
                    onMarkBadDebt = {
                        badDebtAmount = netBalance.max(BigDecimal.ZERO).stripTrailingZeros().toPlainString()
                        badDebtNote = ""
                        showBadDebtDialog = true
                    }
                )
            }

            if (rows.isEmpty()) {
                item {
                    AppEmptyState(
                        title = stringResource(R.string.customer_statement_empty_title),
                        body = stringResource(R.string.customer_statement_empty_body)
                    )
                }
            } else {
                items(rows, key = { it.key }) { row ->
                    StatementTimelineRow(
                        row = row,
                        onClick = {
                            if (row.type == CustomerStatementRowType.PAYMENT) {
                                activePaymentDetails = row.paymentDetails
                            }
                        }
                    )
                }
            }

            item {
                FinalBalanceCard(
                    finalBalance = netBalance,
                    availableCredit = availableCredit
                )
            }
        }
    }

    if (showBadDebtDialog) {
        AlertDialog(
            onDismissRequest = { showBadDebtDialog = false },
            title = { Text(stringResource(R.string.customer_statement_bad_debt_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.customer_statement_bad_debt_help))
                    OutlinedTextField(
                        value = badDebtAmount,
                        onValueChange = { badDebtAmount = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.customer_statement_bad_debt_amount)) }
                    )
                    OutlinedTextField(
                        value = badDebtNote,
                        onValueChange = { badDebtNote = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.customer_statement_bad_debt_note)) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = badDebtAmount.toBigDecimalOrNullSafe()
                        if (amount > BigDecimal.ZERO) {
                            onMarkBadDebt(amount, badDebtNote.trim())
                            badDebtAmount = ""
                            badDebtNote = ""
                            showBadDebtDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.customer_statement_bad_debt_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBadDebtDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    activePaymentDetails?.let { details ->
        ModalBottomSheet(onDismissRequest = { activePaymentDetails = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.customer_statement_payment_details_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = details.methodLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (details.allocations.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.customer_statement_applied_to),
                        style = MaterialTheme.typography.labelLarge
                    )
                    details.allocations.forEach { allocation ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = allocation.orderLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = formatKes(allocation.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Text(
                    text =
                        stringResource(
                            R.string.customer_statement_unallocated_credit,
                            formatKes(details.unallocatedCredit.max(BigDecimal.ZERO))
                        ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.padding(bottom = 12.dp))
            }
        }
    }
}

@Composable
private fun StatementHeaderCard(
    customerName: String,
    balance: BigDecimal,
    availableCredit: BigDecimal,
    onAddOrder: () -> Unit,
    onRecordPayment: () -> Unit,
    onMarkBadDebt: () -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = customerName.ifBlank { stringResource(R.string.money_customer) },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = balanceLabel(balance),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (availableCredit > BigDecimal.ZERO) {
                Text(
                    text =
                        stringResource(
                            R.string.customer_statement_available_credit,
                            formatKes(availableCredit)
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddOrder, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.customer_statement_action_add_order))
                }
                Button(onClick = onRecordPayment, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.customer_statement_action_add_payment))
                }
            }
            if (balance > BigDecimal.ZERO) {
                TextButton(onClick = onMarkBadDebt) {
                    Text(stringResource(R.string.customer_statement_action_mark_bad_debt))
                }
            }
        }
    }
}

@Composable
private fun StatementTimelineRow(
    row: CustomerStatementRowUi,
    onClick: () -> Unit
) {
    val amountColor =
        when (row.type) {
            CustomerStatementRowType.ORDER,
            CustomerStatementRowType.ADJUSTMENT -> MaterialTheme.colorScheme.error
            CustomerStatementRowType.PAYMENT -> MaterialTheme.colorScheme.primary
            CustomerStatementRowType.BAD_DEBT -> MaterialTheme.colorScheme.secondary
        }
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = row.type == CustomerStatementRowType.PAYMENT) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val detail =
                    row.subtitle?.takeIf { it.isNotBlank() }?.let {
                        "${formatDateTime(row.timestamp)} - $it"
                    } ?: formatDateTime(row.timestamp)
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatKes(row.amount),
                    style = MaterialTheme.typography.titleSmall,
                    color = amountColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                )
                Text(
                    text = stringResource(R.string.customer_statement_balance_after, balanceLabel(row.runningBalance)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun FinalBalanceCard(
    finalBalance: BigDecimal,
    availableCredit: BigDecimal
) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.customer_statement_final_balance, balanceLabel(finalBalance)),
                style = MaterialTheme.typography.titleMedium
            )
            if (availableCredit > BigDecimal.ZERO) {
                Text(
                    text =
                        stringResource(
                            R.string.customer_statement_available_credit,
                            formatKes(availableCredit)
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun balanceLabel(balance: BigDecimal): String {
    return when {
        balance > BigDecimal.ZERO ->
            stringResource(R.string.customer_statement_balance_owes, formatKes(balance))
        balance < BigDecimal.ZERO ->
            stringResource(R.string.customer_statement_balance_credit, formatKes(balance.abs()))
        else -> stringResource(R.string.customer_statement_balance_zero)
    }
}

private fun String.toBigDecimalOrNullSafe(): BigDecimal {
    val clean = trim().replace(",", "")
    return runCatching { clean.toBigDecimal() }.getOrNull() ?: BigDecimal.ZERO
}
