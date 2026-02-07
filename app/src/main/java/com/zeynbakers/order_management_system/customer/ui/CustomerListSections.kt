@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zeynbakers.order_management_system.customer.ui

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.accounting.data.CustomerAccountSummary
import com.zeynbakers.order_management_system.core.ui.components.AppFilterOption
import com.zeynbakers.order_management_system.core.ui.components.AppFilterRow
import com.zeynbakers.order_management_system.core.util.formatKes
import java.math.BigDecimal

@Composable
internal fun CustomersTopBar(
    onBack: () -> Unit,
    showBack: Boolean
) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.customers_title), style = MaterialTheme.typography.titleLarge)
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
        }
    )
}

@Composable
internal fun SearchField(
    queryText: String,
    onQueryTextChange: (String) -> Unit
) {
    OutlinedTextField(
        value = queryText,
        onValueChange = onQueryTextChange,
        label = { Text(stringResource(R.string.customer_search_customers)) },
        placeholder = { Text(stringResource(R.string.customer_name_or_phone)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.action_search)
            )
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
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun FilterSortBar(
    selectedFilter: CustomerFilter,
    onFilterChange: (CustomerFilter) -> Unit,
    onSortMenuToggle: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppFilterRow(
            options = customerFilterOptions(),
            selectedKey = selectedFilter.name,
            onSelect = { onFilterChange(CustomerFilter.valueOf(it)) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSortMenuToggle) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = stringResource(R.string.action_sort)
                )
            }
        }
    }
}

@Composable
private fun customerFilterOptions(): List<AppFilterOption> {
    return listOf(
        AppFilterOption(CustomerFilter.All.name, stringResource(R.string.customer_filter_all)),
        AppFilterOption(CustomerFilter.Owing.name, stringResource(R.string.customer_filter_owes)),
        AppFilterOption(CustomerFilter.Credit.name, stringResource(R.string.customer_filter_credit)),
        AppFilterOption(CustomerFilter.Settled.name, stringResource(R.string.customer_filter_settled))
    )
}

@Composable
internal fun SortMenu(
    expanded: Boolean,
    selectedSort: CustomerSort,
    onDismiss: () -> Unit,
    onSelect: (CustomerSort) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.customer_sort_balance_high_to_low)) },
            onClick = { onSelect(CustomerSort.BalanceDesc) },
            trailingIcon = {
                if (selectedSort == CustomerSort.BalanceDesc) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.customer_sort_selected)
                    )
                }
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.customer_sort_balance_low_to_high)) },
            onClick = { onSelect(CustomerSort.BalanceAsc) },
            trailingIcon = {
                if (selectedSort == CustomerSort.BalanceAsc) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.customer_sort_selected)
                    )
                }
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.customer_sort_name_a_to_z)) },
            onClick = { onSelect(CustomerSort.NameAsc) },
            trailingIcon = {
                if (selectedSort == CustomerSort.NameAsc) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.customer_sort_selected)
                    )
                }
            }
        )
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
        name.trim().split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("").ifBlank { "?" }
    val phone = customer.phone.trim()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier =
            Modifier.fillMaxWidth()
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
                    text =
                        stringResource(
                            R.string.customer_billed_paid,
                            formatKes(customer.billed),
                            formatKes(customer.paid)
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onRecordPayment,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(stringResource(R.string.customer_action_pay_short))
                    }
                    TextButton(
                        onClick = onAddOrder,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(stringResource(R.string.customer_action_order_short))
                    }
                    TextButton(
                        onClick = onMessage,
                        enabled = hasPhone,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(stringResource(R.string.customer_action_message))
                    }
                }
            }

            Spacer(Modifier.width(6.dp))

            BalanceChip(balance = customer.balance)
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.customer_actions)
                )
            }
        }
    }
}

@Composable
private fun BalanceChip(balance: BigDecimal) {
    val (label, color) =
        when {
            balance > BigDecimal.ZERO ->
                stringResource(R.string.customer_filter_owes) to MaterialTheme.colorScheme.errorContainer
            balance < BigDecimal.ZERO ->
                stringResource(R.string.customer_filter_credit) to MaterialTheme.colorScheme.tertiaryContainer
            else ->
                stringResource(R.string.customer_filter_settled) to MaterialTheme.colorScheme.secondaryContainer
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
internal fun EmptyCustomersState(
    isSearching: Boolean,
    onAddCustomer: () -> Unit
) {
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
            text =
                if (isSearching) {
                    stringResource(R.string.customer_no_customers_found)
                } else {
                    stringResource(R.string.customer_no_customers_yet)
                },
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text =
                if (isSearching) {
                    stringResource(R.string.customer_try_different_search)
                } else {
                    stringResource(R.string.customer_add_first)
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Button(onClick = onAddCustomer) {
            Text(stringResource(R.string.customer_add))
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
            Modifier.fillMaxWidth()
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

internal fun launchSms(context: android.content.Context, phone: String) {
    if (phone.isBlank()) return
    val intent = Intent(Intent.ACTION_VIEW, "sms:$phone".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
