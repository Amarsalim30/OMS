@file:OptIn(
        androidx.compose.material3.ExperimentalMaterial3Api::class,
        androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.zeynbakers.order_management_system.customer.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.accounting.data.CustomerAccountSummary
import com.zeynbakers.order_management_system.core.util.formatKes
import java.math.BigDecimal

@Composable
internal fun CustomersTopBar(onBack: () -> Unit, showBack: Boolean, onSearchClick: () -> Unit) {
    CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                            text = stringResource(R.string.customers_title),
                            style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                            text = stringResource(R.string.customers_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                if (showBack) {
                    IconButton(onClick = onBack) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.action_search)
                    )
                }
            }
    )
}

@Composable
internal fun CustomerSearchTopBar(
    queryText: String,
    onQueryTextChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(64.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        TextField(
            value = queryText,
            onValueChange = onQueryTextChange,
            placeholder = {
                Text(stringResource(R.string.customer_name_or_phone))
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            leadingIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            },
            trailingIcon = {
                if (queryText.isNotBlank()) {
                    IconButton(onClick = { onQueryTextChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_clear)
                        )
                    }
                }
            }
        )
    }
}

@Composable
internal fun CustomerListControlRow(
    selectedFilter: CustomerFilter,
    selectedSort: CustomerSort,
    hideZeroBalances: Boolean,
    showInactive: Boolean,
    onFilterClick: () -> Unit,
    onSortClick: () -> Unit,
    onSyncClick: () -> Unit,
    showSyncAction: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlIconButton(
                icon = Icons.Filled.Tune,
                contentDescription = stringResource(R.string.action_more_filters),
                active = selectedFilter != CustomerFilter.All || !hideZeroBalances || showInactive,
                onClick = onFilterClick
            )
            ControlIconButton(
                icon = Icons.AutoMirrored.Filled.Sort,
                contentDescription = stringResource(R.string.action_sort),
                active = selectedSort != CustomerSort.BalanceDesc,
                onClick = onSortClick
            )
        }
        if (showSyncAction) {
            TextButton(
                onClick = onSyncClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.customer_sync_contacts),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
internal fun CustomerFilterSheet(
    selectedFilter: CustomerFilter,
    hideZeroBalances: Boolean,
    showInactive: Boolean,
    onSelect: (CustomerFilter) -> Unit,
    onToggleHideZero: () -> Unit,
    onToggleShowInactive: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.customer_filter_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            CustomerFilter.values().forEach { filter ->
                ListItem(
                    headlineContent = { Text(getCustomerFilterLabel(filter)) },
                    leadingContent = {
                        if (filter == selectedFilter) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        } else {
                            Spacer(Modifier.size(24.dp))
                        }
                    },
                    modifier = Modifier.clickable {
                        onSelect(filter)
                        onDismiss()
                    }
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.customer_hide_zero_balances)) },
                leadingContent = {
                    if (hideZeroBalances) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                    } else {
                        Spacer(Modifier.size(24.dp))
                    }
                },
                modifier = Modifier.clickable {
                    onToggleHideZero()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.customer_show_inactive_no_orders)) },
                leadingContent = {
                    if (showInactive) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                    } else {
                        Spacer(Modifier.size(24.dp))
                    }
                },
                modifier = Modifier.clickable {
                    onToggleShowInactive()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun ControlIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (active) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (active) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
internal fun CustomerSortSheet(
    selectedSort: CustomerSort,
    onSelect: (CustomerSort) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.customer_sort_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            CustomerSort.values().forEach { sort ->
                ListItem(
                    headlineContent = { Text(getCustomerSortLabel(sort)) },
                    leadingContent = {
                        if (sort == selectedSort) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        } else {
                            Spacer(Modifier.size(24.dp))
                        }
                    },
                    modifier = Modifier.clickable {
                        onSelect(sort)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun getCustomerFilterLabel(filter: CustomerFilter): String {
    return when (filter) {
        CustomerFilter.All -> stringResource(R.string.customer_filter_all)
        CustomerFilter.Owing -> stringResource(R.string.customer_filter_owes)
        CustomerFilter.Credit -> stringResource(R.string.customer_filter_credit)
        CustomerFilter.Settled -> stringResource(R.string.customer_filter_settled)
    }
}

@Composable
private fun getCustomerSortLabel(sort: CustomerSort): String {
    return when (sort) {
        CustomerSort.BalanceDesc -> stringResource(R.string.customer_sort_balance_desc_short)
        CustomerSort.BalanceAsc -> stringResource(R.string.customer_sort_balance_asc_short)
        CustomerSort.NameAsc -> stringResource(R.string.customer_sort_name_asc_short)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CustomerRowItem(
        customer: CustomerAccountSummary,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
        onMenuClick: () -> Unit,
        onRecordPayment: () -> Unit,
        onAddOrder: () -> Unit,
        onMessage: () -> Unit,
        hasPhone: Boolean
) {
    val name = customer.name.ifBlank { stringResource(R.string.customer_unknown) }
    val initials =
            name.trim()
                    .split(" ")
                    .take(2)
                    .mapNotNull { it.firstOrNull() }
                    .joinToString("")
                    .uppercase()
                    .ifBlank { "?" }
    val phone = customer.phone.trim()

    Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
            modifier =
                    Modifier.fillMaxWidth()
                            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact avatar
            Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                            text = initials,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Main content - name and phone in compact vertical stack
            Column(modifier = Modifier.weight(1f)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    // Balance chip on the right
                    CompactBalanceChip(balance = customer.balance)
                }

                Spacer(Modifier.height(2.dp))

                // Secondary info row: phone + billed/paid summary
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    if (phone.isNotBlank()) {
                        Text(
                                text = phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                            text =
                                    stringResource(
                                            R.string.customer_billed_paid,
                                            formatKes(customer.billed),
                                            formatKes(customer.paid)
                                    ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            // Smaller menu icon
            IconButton(onClick = onMenuClick, modifier = Modifier.size(32.dp)) {
                Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.customer_actions),
                        modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactBalanceChip(balance: BigDecimal) {
    val (label, containerColor, contentColor) =
            when {
                balance > BigDecimal.ZERO ->
                        Triple(
                                formatKes(balance),
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                balance < BigDecimal.ZERO ->
                        Triple(
                                formatKes(balance.abs()),
                                MaterialTheme.colorScheme.tertiaryContainer,
                                MaterialTheme.colorScheme.onTertiaryContainer
                        )
                else ->
                        Triple(
                                "Settled",
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
            }
    Surface(color = containerColor, shape = MaterialTheme.shapes.small) {
        Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
internal fun EmptyCustomersState(
        isSearching: Boolean,
        hasActiveFilters: Boolean,
        hasAnyCustomers: Boolean,
        showImportedNoOrdersState: Boolean,
        onClearFilters: () -> Unit,
        onAddCustomer: () -> Unit,
        onSyncContacts: () -> Unit,
        onRevealCustomers: () -> Unit
) {
    val titleText =
            when {
                isSearching -> stringResource(R.string.customer_no_customers_found)
                hasActiveFilters -> stringResource(R.string.customer_no_customers_for_filters)
                showImportedNoOrdersState -> stringResource(R.string.customer_no_orders_for_imported_title)
                hasAnyCustomers -> stringResource(R.string.customer_no_customers_for_filters)
                else -> stringResource(R.string.customer_no_customers_yet)
            }
    val bodyText =
            when {
                isSearching -> stringResource(R.string.customer_try_different_search)
                hasActiveFilters -> stringResource(R.string.customer_adjust_or_clear_filters)
                showImportedNoOrdersState -> stringResource(R.string.customer_no_orders_for_imported_body)
                hasAnyCustomers -> stringResource(R.string.customer_adjust_or_clear_filters)
                else -> stringResource(R.string.customer_import_contacts_first)
            }

    Column(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = stringResource(R.string.customer_no_customers_icon),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(6.dp))
        Text(
                text = bodyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        if (hasActiveFilters) {
            Button(onClick = onClearFilters) {
                Text(stringResource(R.string.customer_reset_filters))
            }
        } else if (showImportedNoOrdersState) {
            Button(onClick = onRevealCustomers) {
                Text(stringResource(R.string.customer_show_imported_contacts))
            }
        } else if (!hasAnyCustomers) {
            Button(onClick = onSyncContacts) {
                Text(stringResource(R.string.customer_import_contacts_action))
            }
        } else {
            Button(onClick = onAddCustomer) { Text(stringResource(R.string.customer_add)) }
        }
    }
}

@Composable
internal fun ActionRow(
        label: String,
        onClick: () -> Unit,
        enabled: Boolean = true,
        textColor: androidx.compose.ui.graphics.Color? = null
) {
    val resolvedColor = textColor ?: MaterialTheme.colorScheme.onSurface
    val displayColor = if (enabled) resolvedColor else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
            modifier =
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = enabled) {
                        onClick()
                    },
            color = MaterialTheme.colorScheme.surface
    ) {
        Row(
                modifier = Modifier.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
        ) { Text(text = label, style = MaterialTheme.typography.bodyLarge, color = displayColor) }
    }
}
