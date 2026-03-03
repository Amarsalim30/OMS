@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    kotlinx.coroutines.FlowPreview::class
)

package com.zeynbakers.order_management_system.customer.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.accounting.data.CustomerAccountSummary
import com.zeynbakers.order_management_system.core.tutorial.TutorialCoachTargets
import com.zeynbakers.order_management_system.core.tutorial.tutorialCoachTarget
import com.zeynbakers.order_management_system.core.ui.LocalUiEventDispatcher
import com.zeynbakers.order_management_system.core.ui.showSnackbar
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
    onSyncContacts: () -> Unit = onAddCustomer,
    onPaymentHistory: (Long) -> Unit = {},
    onRecordPayment: (Long) -> Unit = {},
    onAddOrder: (Long) -> Unit = {},
    onEditCustomer: (Long) -> Unit = {},
    onArchiveCustomer: (Long) -> Unit = {},
    onDeleteCustomer: (Long) -> Unit = {},
    onRestoreCustomer: (Long) -> Unit = {},
    isSyncingContacts: Boolean = false,
    showBack: Boolean = true
) {
    CustomersScreenM3(
        query = query,
        summaries = summaries,
        onQueryChange = onQueryChange,
        onCustomerClick = onCustomerClick,
        onBack = onBack,
        onAddCustomer = onAddCustomer,
        onSyncContacts = onSyncContacts,
        onPaymentHistory = onPaymentHistory,
        onRecordPayment = onRecordPayment,
        onAddOrder = onAddOrder,
        onEditCustomer = onEditCustomer,
        onArchiveCustomer = onArchiveCustomer,
        onDeleteCustomer = onDeleteCustomer,
        onRestoreCustomer = onRestoreCustomer,
        isSyncingContacts = isSyncingContacts,
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
    onSyncContacts: () -> Unit,
    onPaymentHistory: (Long) -> Unit,
    onRecordPayment: (Long) -> Unit,
    onAddOrder: (Long) -> Unit,
    onEditCustomer: (Long) -> Unit,
    onArchiveCustomer: (Long) -> Unit,
    onDeleteCustomer: (Long) -> Unit,
    onRestoreCustomer: (Long) -> Unit,
    isSyncingContacts: Boolean,
    showBack: Boolean
) {
    var queryText by rememberSaveable { mutableStateOf(query) }
    var isSearchActive by rememberSaveable { mutableStateOf(query.isNotBlank()) }
    var selectedFilter by rememberSaveable { mutableStateOf(CustomerFilter.All) }
    var selectedSort by rememberSaveable { mutableStateOf(CustomerSort.BalanceDesc) }
    var hideZeroBalances by rememberSaveable { mutableStateOf(true) }
    var showInactive by rememberSaveable { mutableStateOf(false) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var longPressedCustomer by remember { mutableStateOf<CustomerAccountSummary?>(null) }
    var pendingArchiveCustomer by remember { mutableStateOf<CustomerAccountSummary?>(null) }
    val hasActiveFilters by remember(selectedFilter, hideZeroBalances, showInactive) {
        derivedStateOf {
            selectedFilter != CustomerFilter.All || !hideZeroBalances || showInactive
        }
    }
    val hasActiveControls by remember(hasActiveFilters, selectedSort) {
        derivedStateOf {
            hasActiveFilters || selectedSort != CustomerSort.BalanceDesc
        }
    }
    val hasAnyCustomers = summaries.isNotEmpty()
    val uiEvents = LocalUiEventDispatcher.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val customerArchivedMessage = stringResource(R.string.customer_archived)
    val customerDeletedMessage = stringResource(R.string.customer_deleted)
    val undoActionLabel = stringResource(R.string.action_undo)
    val resetFiltersAndSort: () -> Unit = {
        selectedFilter = CustomerFilter.All
        selectedSort = CustomerSort.BalanceDesc
        hideZeroBalances = true
        showInactive = false
    }

    LaunchedEffect(query) {
        if (queryText != query) {
            queryText = query
        }
        if (query.isNotBlank()) {
            isSearchActive = true
        }
    }

    LaunchedEffect(queryText) {
        snapshotFlow { queryText }
            .debounce(300)
            .distinctUntilChanged()
            .collect { onQueryChange(it) }
    }

    BackHandler(
        enabled =
            pendingArchiveCustomer != null ||
                longPressedCustomer != null ||
                showSortSheet ||
                showFilterSheet ||
                isSearchActive
    ) {
        when {
            pendingArchiveCustomer != null -> pendingArchiveCustomer = null
            longPressedCustomer != null -> longPressedCustomer = null
            showSortSheet -> showSortSheet = false
            showFilterSheet -> showFilterSheet = false
            isSearchActive -> {
                isSearchActive = false
                queryText = ""
            }
        }
    }
    val revealImportedCustomers: () -> Unit = {
        showInactive = true
        selectedFilter = CustomerFilter.All
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
                val hasActivity =
                    summary.hasOrders ||
                        !isZeroAmount(summary.billed) ||
                        !isZeroAmount(summary.paid)
                if (!showInactive && !hasActivity) return@filter false
                true
            }
            val filtered = base.filter { summary ->
                when (selectedFilter) {
                    CustomerFilter.All -> true
                    CustomerFilter.Owing -> summary.balance > BigDecimal.ZERO
                    CustomerFilter.Credit -> summary.balance < BigDecimal.ZERO
                    CustomerFilter.Settled -> isZeroAmount(summary.balance)
                }
            }.filter { summary ->
                if (selectedFilter != CustomerFilter.All || !hideZeroBalances) return@filter true
                val hasActivity =
                    summary.hasOrders ||
                        !isZeroAmount(summary.billed) ||
                        !isZeroAmount(summary.paid)
                !isZeroAmount(summary.balance) || hasActivity
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
        topBar = {
            if (isSearchActive) {
                CustomerSearchTopBar(
                    queryText = queryText,
                    onQueryTextChange = { queryText = it },
                    onBack = {
                        isSearchActive = false
                        queryText = ""
                    }
                )
            } else {
                CustomersTopBar(
                    onBack = onBack,
                    showBack = showBack,
                    onSearchClick = { isSearchActive = true }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(start = 12.dp, end = 12.dp, top = 12.dp)
        ) {
            CustomerListControlRow(
                selectedFilter = selectedFilter,
                selectedSort = selectedSort,
                hideZeroBalances = hideZeroBalances,
                showInactive = showInactive,
                onFilterClick = { showFilterSheet = true },
                onSortClick = { showSortSheet = true },
                onSyncClick = onSyncContacts,
                isSyncing = isSyncingContacts,
                showSyncAction = hasAnyCustomers,
                modifier = Modifier.tutorialCoachTarget(TutorialCoachTargets.CustomersControlRow)
            )

            if (hasActiveControls) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.customer_filters_active),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = resetFiltersAndSort) {
                        Text(text = stringResource(R.string.customer_reset_filters))
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (queryText.isBlank()) {
                            stringResource(R.string.customer_all_customers)
                        } else {
                            stringResource(R.string.customer_search_results)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.customer_result_count,
                            filteredCustomers.size,
                            filteredCustomers.size
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            if (filteredCustomers.isEmpty()) {
                val allCustomersWithoutOrders =
                    hasAnyCustomers &&
                        summaries.all {
                            !it.hasOrders &&
                                isZeroAmount(it.billed) &&
                                isZeroAmount(it.paid)
                        }
                val showImportedNoOrdersState =
                    queryText.isBlank() &&
                        !hasActiveFilters &&
                        !showInactive &&
                        allCustomersWithoutOrders
                EmptyCustomersState(
                    isSearching = queryText.isNotBlank(),
                    hasActiveFilters = hasActiveFilters,
                    hasAnyCustomers = hasAnyCustomers,
                    showImportedNoOrdersState = showImportedNoOrdersState,
                    onClearFilters = resetFiltersAndSort,
                    onAddCustomer = onAddCustomer,
                    onSyncContacts = onSyncContacts,
                    onRevealCustomers = revealImportedCustomers
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredCustomers, key = { it.customerId }) { customer ->
                        CustomerRowItem(
                            customer = customer,
                            onClick = { onCustomerClick(customer.customerId) },
                            onLongClick = { longPressedCustomer = customer },
                            onMenuClick = { longPressedCustomer = customer },
                            onRecordPayment = { onRecordPayment(customer.customerId) },
                            onAddOrder = { onAddOrder(customer.customerId) },
                            onMessage = {
                                if (customer.phone.isNotBlank()) {
                                    launchCustomerMessage(context, customer.phone)
                                }
                            },
                            hasPhone = customer.phone.isNotBlank()
                        )
                    }
                }
            }
        }
    }

    longPressedCustomer?.let { customer ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val hasPhone = customer.phone.isNotBlank()
        val canDelete =
            !customer.hasOrders &&
            isZeroAmount(customer.billed) &&
                isZeroAmount(customer.paid) &&
                isZeroAmount(customer.balance)
        ModalBottomSheet(
            onDismissRequest = { longPressedCustomer = null },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text(
                    text = customer.name.ifBlank { stringResource(R.string.customer_unknown) },
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(6.dp))
                ActionRow(label = stringResource(R.string.customer_action_record_payment), onClick = {
                    onRecordPayment(customer.customerId)
                    longPressedCustomer = null
                })
                ActionRow(label = stringResource(R.string.customer_action_new_order), onClick = {
                    onAddOrder(customer.customerId)
                    longPressedCustomer = null
                })
                ActionRow(
                    label = stringResource(R.string.customer_action_message),
                    enabled = hasPhone,
                    onClick = {
                        launchCustomerMessage(context, customer.phone)
                        longPressedCustomer = null
                    }
                )
                ActionRow(label = stringResource(R.string.customer_action_view_details), onClick = {
                    onCustomerClick(customer.customerId)
                    longPressedCustomer = null
                })
                ActionRow(label = stringResource(R.string.customer_action_payment_history), onClick = {
                    onPaymentHistory(customer.customerId)
                    longPressedCustomer = null
                })
                ActionRow(label = stringResource(R.string.customer_action_edit_customer), onClick = {
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
                ActionRow(
                    label =
                        stringResource(
                            if (canDelete) {
                                R.string.customer_action_delete
                            } else {
                                R.string.customer_action_archive
                            }
                        ),
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        pendingArchiveCustomer = customer
                        longPressedCustomer = null
                    }
                )
            }
        }
    }

    pendingArchiveCustomer?.let { customer ->
        val hasTransactions =
            customer.hasOrders ||
                !isZeroAmount(customer.billed) ||
                !isZeroAmount(customer.paid) ||
                !isZeroAmount(customer.balance)
        val titleRes =
            if (hasTransactions) {
                R.string.customer_archive_title
            } else {
                R.string.customer_delete_title
            }
        val messageRes =
            if (hasTransactions) {
                R.string.customer_archive_message
            } else {
                R.string.customer_delete_message
            }
        val confirmRes =
            if (hasTransactions) {
                R.string.customer_action_archive
            } else {
                R.string.customer_action_delete
            }
        AlertDialog(
            onDismissRequest = { pendingArchiveCustomer = null },
            title = { Text(stringResource(titleRes)) },
            text = { Text(stringResource(messageRes)) },
            confirmButton = {
                TextButton(onClick = {
                    if (hasTransactions) {
                        onArchiveCustomer(customer.customerId)
                        pendingArchiveCustomer = null
                        scope.launch {
                            val result =
                                uiEvents.showSnackbar(
                                    message = customerArchivedMessage,
                                    actionLabel = undoActionLabel
                                )
                            if (result == SnackbarResult.ActionPerformed) {
                                onRestoreCustomer(customer.customerId)
                            }
                        }
                    } else {
                        onDeleteCustomer(customer.customerId)
                        pendingArchiveCustomer = null
                        scope.launch {
                            uiEvents.showSnackbar(
                                message = customerDeletedMessage
                            )
                        }
                    }
                }) { Text(stringResource(confirmRes)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingArchiveCustomer = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showFilterSheet) {
        CustomerFilterSheet(
            selectedFilter = selectedFilter,
            hideZeroBalances = hideZeroBalances,
            showInactive = showInactive,
            onSelect = { selectedFilter = it },
            onToggleHideZero = { hideZeroBalances = !hideZeroBalances },
            onToggleShowInactive = { showInactive = !showInactive },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showSortSheet) {
        CustomerSortSheet(
            selectedSort = selectedSort,
            onSelect = { selectedSort = it },
            onDismiss = { showSortSheet = false }
        )
    }
}

private fun isZeroAmount(value: BigDecimal): Boolean = value.compareTo(BigDecimal.ZERO) == 0

internal enum class CustomerFilter {
    All,
    Owing,
    Credit,
    Settled
}

internal enum class CustomerSort {
    BalanceDesc,
    BalanceAsc,
    NameAsc
}
