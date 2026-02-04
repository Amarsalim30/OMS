@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    kotlinx.coroutines.FlowPreview::class
)

package com.zeynbakers.order_management_system.customer.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.accounting.data.CustomerAccountSummary
import com.zeynbakers.order_management_system.core.util.formatKes
import java.math.BigDecimal
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun CustomerListScreen(
    query: String,
    summaries: List<CustomerAccountSummary>,
    onQueryChange: (String) -> Unit,
    onCustomerClick: (Long) -> Unit,
    onBack: () -> Unit,
    onAddCustomer: () -> Unit = {},
    onPaymentHistory: (Long) -> Unit = {},
    onRecordPayment: (Long) -> Unit = {},
    onAddOrder: (Long) -> Unit = {},
    onEditCustomer: (Long) -> Unit = {},
    onArchiveCustomer: (Long) -> Unit = {},
    onRestoreCustomer: (Long) -> Unit = {},
    onDeleteCustomer: (Long) -> Unit = {},
    showBack: Boolean = true
) {
    CustomersScreenM3(
        query = query,
        summaries = summaries,
        onQueryChange = onQueryChange,
        onCustomerClick = onCustomerClick,
        onBack = onBack,
        onAddCustomer = onAddCustomer,
        onPaymentHistory = onPaymentHistory,
        onRecordPayment = onRecordPayment,
        onAddOrder = onAddOrder,
        onEditCustomer = onEditCustomer,
        onArchiveCustomer = onArchiveCustomer,
        onRestoreCustomer = onRestoreCustomer,
        onDeleteCustomer = onDeleteCustomer,
        showBack = showBack
    )
}

@Composable
private fun CustomersScreenM3(
    query: String,
    summaries: List<CustomerAccountSummary>,
    onQueryChange: (String) -> Unit,
    onCustomerClick: (Long) -> Unit,
    onBack: () -> Unit,
    onAddCustomer: () -> Unit,
    onPaymentHistory: (Long) -> Unit,
    onRecordPayment: (Long) -> Unit,
    onAddOrder: (Long) -> Unit,
    onEditCustomer: (Long) -> Unit,
    onArchiveCustomer: (Long) -> Unit,
    onRestoreCustomer: (Long) -> Unit,
    onDeleteCustomer: (Long) -> Unit,
    showBack: Boolean
) {
    var queryText by rememberSaveable { mutableStateOf(query) }
    var selectedFilter by rememberSaveable { mutableStateOf(CustomerFilter.All) }
    var selectedSort by rememberSaveable { mutableStateOf(CustomerSort.BalanceDesc) }
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var hideZeroBalances by rememberSaveable { mutableStateOf(true) }
    var showInactive by rememberSaveable { mutableStateOf(false) }
    var longPressedCustomer by remember { mutableStateOf<CustomerAccountSummary?>(null) }
    var pendingArchiveCustomer by remember { mutableStateOf<CustomerAccountSummary?>(null) }
    var pendingDeleteCustomer by remember { mutableStateOf<CustomerAccountSummary?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(query) {
        if (queryText != query) {
            queryText = query
        }
    }

    LaunchedEffect(queryText) {
        snapshotFlow { queryText }
            .debounce(300)
            .distinctUntilChanged()
            .collect { onQueryChange(it) }
    }

    val filteredCustomers by remember(
        summaries,
        selectedFilter,
        selectedSort,
        hideZeroBalances,
        showInactive
    ) {
        derivedStateOf {
            val base = summaries.filter { summary ->
                val hasTransactions =
                    summary.billed != BigDecimal.ZERO || summary.paid != BigDecimal.ZERO
                if (!showInactive && !hasTransactions) return@filter false
                true
            }
            val filtered = base.filter { summary ->
                when (selectedFilter) {
                    CustomerFilter.All -> true
                    CustomerFilter.Owing -> summary.balance > BigDecimal.ZERO
                    CustomerFilter.Credit -> summary.balance < BigDecimal.ZERO
                    CustomerFilter.Settled -> summary.balance == BigDecimal.ZERO
                }
            }.filter { summary ->
                if (selectedFilter != CustomerFilter.All || !hideZeroBalances) return@filter true
                val hasTransactions =
                    summary.billed != BigDecimal.ZERO || summary.paid != BigDecimal.ZERO
                if (!hasTransactions) return@filter true
                summary.balance != BigDecimal.ZERO
            }
            when (selectedSort) {
                CustomerSort.BalanceDesc -> filtered.sortedByDescending { it.balance }
                CustomerSort.BalanceAsc -> filtered.sortedBy { it.balance }
                CustomerSort.NameAsc -> filtered.sortedBy { it.name.lowercase() }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CustomersTopBar(
                onBack = onBack,
                showBack = showBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(start = 12.dp, end = 12.dp, top = 12.dp)
        ) {
            SearchField(
                queryText = queryText,
                onQueryTextChange = { queryText = it }
            )

            Spacer(Modifier.height(6.dp))

            FilterSortBar(
                selectedFilter = selectedFilter,
                onFilterChange = { selectedFilter = it },
                selectedSort = selectedSort,
                onSortMenuToggle = { isSortMenuOpen = !isSortMenuOpen }
            )

            SortMenu(
                expanded = isSortMenuOpen,
                selectedSort = selectedSort,
                onDismiss = { isSortMenuOpen = false },
                onSelect = {
                    selectedSort = it
                    isSortMenuOpen = false
                }
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = hideZeroBalances,
                    onClick = { hideZeroBalances = !hideZeroBalances },
                    label = { Text("Hide zero balances") }
                )
                FilterChip(
                    selected = showInactive,
                    onClick = { showInactive = !showInactive },
                    label = { Text("Show inactive (no orders)") }
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (queryText.isBlank()) "All customers" else "Search results",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "${filteredCustomers.size} results",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(6.dp))

            if (filteredCustomers.isEmpty()) {
                EmptyCustomersState(
                    isSearching = queryText.isNotBlank(),
                    onAddCustomer = onAddCustomer
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 0.dp)
                ) {
                    items(filteredCustomers, key = { it.customerId }) { customer ->
                        CustomerRowItem(
                            customer = customer,
                            onClick = { onCustomerClick(customer.customerId) },
                            onLongClick = { longPressedCustomer = customer },
                            onMenuClick = { longPressedCustomer = customer }
                        )
                    }
                }
            }
        }
    }

    longPressedCustomer?.let { customer ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val hasTransactions =
            customer.billed != BigDecimal.ZERO || customer.paid != BigDecimal.ZERO
        val hasPhone = customer.phone.isNotBlank()
        ModalBottomSheet(
            onDismissRequest = { longPressedCustomer = null },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text(
                    text = customer.name.ifBlank { "Unknown Customer" },
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(6.dp))
                ActionRow(label = "Record payment", onClick = {
                    onRecordPayment(customer.customerId)
                    longPressedCustomer = null
                })
                ActionRow(label = "New order", onClick = {
                    onAddOrder(customer.customerId)
                    longPressedCustomer = null
                })
                ActionRow(
                    label = "Message",
                    enabled = hasPhone,
                    onClick = {
                        launchSms(context, customer.phone)
                        longPressedCustomer = null
                    }
                )
                ActionRow(label = "View details", onClick = {
                    onCustomerClick(customer.customerId)
                    longPressedCustomer = null
                })
                ActionRow(label = "Payment history", onClick = {
                    onPaymentHistory(customer.customerId)
                    longPressedCustomer = null
                })
                ActionRow(label = "Edit customer", onClick = {
                    onEditCustomer(customer.customerId)
                    longPressedCustomer = null
                })
                Spacer(Modifier.height(6.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {}
                val destructiveLabel = if (hasTransactions) "Archive" else "Delete"
                ActionRow(
                    label = destructiveLabel,
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        if (hasTransactions) {
                            pendingArchiveCustomer = customer
                        } else {
                            pendingDeleteCustomer = customer
                        }
                        longPressedCustomer = null
                    }
                )
            }
        }
    }

    pendingArchiveCustomer?.let { customer ->
        AlertDialog(
            onDismissRequest = { pendingArchiveCustomer = null },
            title = { Text("Archive customer?") },
            text = { Text("Archived customers are hidden from lists, but their history stays.") },
            confirmButton = {
                TextButton(onClick = {
                    onArchiveCustomer(customer.customerId)
                    pendingArchiveCustomer = null
                    scope.launch {
                        val result =
                            snackbarHostState.showSnackbar(
                                message = "Customer archived",
                                actionLabel = "Undo"
                            )
                        if (result == SnackbarResult.ActionPerformed) {
                            onRestoreCustomer(customer.customerId)
                        }
                    }
                }) { Text("Archive") }
            },
            dismissButton = {
                TextButton(onClick = { pendingArchiveCustomer = null }) { Text("Cancel") }
            }
        )
    }

    pendingDeleteCustomer?.let { customer ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCustomer = null },
            title = { Text("Delete customer?") },
            text = { Text("This will remove the customer permanently.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCustomer(customer.customerId)
                    pendingDeleteCustomer = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCustomer = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CustomersTopBar(
    onBack: () -> Unit,
    showBack: Boolean
) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Customers", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Find customers and collect payments",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            if (showBack) {
                TextButton(onClick = onBack) { Text("Back") }
            }
        }
    )
}

@Composable
private fun SearchField(
    queryText: String,
    onQueryTextChange: (String) -> Unit
) {
    OutlinedTextField(
        value = queryText,
        onValueChange = onQueryTextChange,
        label = { Text("Search customers") },
        placeholder = { Text("Name or phone") },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "Search") },
        trailingIcon = {
            if (queryText.isNotBlank()) {
                IconButton(onClick = { onQueryTextChange("") }) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FilterSortBar(
    selectedFilter: CustomerFilter,
    onFilterChange: (CustomerFilter) -> Unit,
    selectedSort: CustomerSort,
    onSortMenuToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == CustomerFilter.All,
                onClick = { onFilterChange(CustomerFilter.All) },
                label = { Text("All") }
            )
            FilterChip(
                selected = selectedFilter == CustomerFilter.Owing,
                onClick = { onFilterChange(CustomerFilter.Owing) },
                label = { Text("Owes") }
            )
            FilterChip(
                selected = selectedFilter == CustomerFilter.Credit,
                onClick = { onFilterChange(CustomerFilter.Credit) },
                label = { Text("Credit") }
            )
            FilterChip(
                selected = selectedFilter == CustomerFilter.Settled,
                onClick = { onFilterChange(CustomerFilter.Settled) },
                label = { Text("Settled") }
            )
        }

        IconButton(onClick = onSortMenuToggle) {
            Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
        }
    }
}

@Composable
private fun SortMenu(
    expanded: Boolean,
    selectedSort: CustomerSort,
    onDismiss: () -> Unit,
    onSelect: (CustomerSort) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Balance: High to low") },
            onClick = { onSelect(CustomerSort.BalanceDesc) },
            trailingIcon = {
                if (selectedSort == CustomerSort.BalanceDesc) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = "Selected")
                }
            }
        )
        DropdownMenuItem(
            text = { Text("Balance: Low to high") },
            onClick = { onSelect(CustomerSort.BalanceAsc) },
            trailingIcon = {
                if (selectedSort == CustomerSort.BalanceAsc) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = "Selected")
                }
            }
        )
        DropdownMenuItem(
            text = { Text("Name: A to Z") },
            onClick = { onSelect(CustomerSort.NameAsc) },
            trailingIcon = {
                if (selectedSort == CustomerSort.NameAsc) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = "Selected")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomerRowItem(
    customer: CustomerAccountSummary,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val name = customer.name.ifBlank { "Unknown Customer" }
    val initials = name.trim().split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("")
        .ifBlank { "?" }
    val phone = customer.phone.trim()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = initials, style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (phone.isNotBlank()) {
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Billed ${formatKes(customer.billed)} - Paid ${formatKes(customer.paid)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(6.dp))

            BalanceChip(balance = customer.balance)
            IconButton(onClick = onMenuClick) {
                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Customer actions")
            }
        }
    }
}

@Composable
private fun BalanceChip(balance: BigDecimal) {
    val (label, color) =
        when {
            balance > BigDecimal.ZERO -> "Owes" to MaterialTheme.colorScheme.errorContainer
            balance < BigDecimal.ZERO -> "Credit" to MaterialTheme.colorScheme.tertiaryContainer
            else -> "Settled" to MaterialTheme.colorScheme.secondaryContainer
        }
    Surface(
        color = color,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.heightIn(min = 32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = formatKes(balance.abs()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EmptyCustomersState(
    isSearching: Boolean,
    onAddCustomer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = "No customers",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (isSearching) "No customers found" else "No customers yet",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (isSearching) "Try a different search." else "Add your first customer to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Button(onClick = onAddCustomer) {
            Text("Add customer")
        }
    }
}

@Composable
private fun ActionRow(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    textColor: androidx.compose.ui.graphics.Color? = null
) {
    val resolvedColor = textColor ?: MaterialTheme.colorScheme.onSurface
    val displayColor =
        if (enabled) resolvedColor else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled) { onClick() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = displayColor
            )
        }
    }
}

private enum class CustomerFilter {
    All,
    Owing,
    Credit,
    Settled
}

private enum class CustomerSort {
    BalanceDesc,
    BalanceAsc,
    NameAsc
}

private fun launchSms(context: android.content.Context, phone: String) {
    if (phone.isBlank()) return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phone"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}




