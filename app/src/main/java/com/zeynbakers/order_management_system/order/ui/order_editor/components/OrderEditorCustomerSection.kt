package com.zeynbakers.order_management_system.order.ui.order_editor.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import kotlinx.coroutines.launch

@Composable
internal fun OrderEditorCustomerSection(
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    customerPhone: String,
    onCustomerPhoneChange: (String) -> Unit,
    customerConfirmed: Boolean,
    onCustomerConfirmedChange: (Boolean) -> Unit,
    suggestions: List<CustomerEntity>,
    onSuggestionSelected: (CustomerEntity) -> Unit,
    onCreateCustomerFromQuery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.order_editor_customer_section_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (customerConfirmed) {
            val badgeLabel =
                customerName.trim().ifBlank {
                    customerPhone.trim().ifBlank { stringResource(R.string.day_no_customer) }
                }
            InputChip(
                selected = true,
                onClick = {},
                colors =
                    InputChipDefaults.inputChipColors(
                        selectedContainerColor = Color(0xFF1E88E5),
                        selectedLabelColor = Color.White,
                        selectedTrailingIconColor = Color.White
                    ),
                label = {
                    Text(
                        text = badgeLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            onCustomerNameChange("")
                            onCustomerPhoneChange("")
                            onCustomerConfirmedChange(false)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_clear),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            val customerFocusBringIntoView = remember { BringIntoViewRequester() }
            val customerScope = rememberCoroutineScope()
            OrderEditorOutlinedField(
                value = customerName,
                onValueChange = onCustomerNameChange,
                placeholder = stringResource(R.string.order_editor_customer_name_optional),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.action_search)
                    )
                },
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(customerFocusBringIntoView)
                        .onFocusChanged {
                            if (it.isFocused) {
                                customerScope.launch { customerFocusBringIntoView.bringIntoView() }
                            }
                        }
            )

            if (customerName.isNotBlank()) {
                CustomerSuggestionDropdown(
                    query = customerName,
                    suggestions = suggestions,
                    onCreateCustomer = {
                        onCreateCustomerFromQuery(customerName.trim())
                        onCustomerConfirmedChange(true)
                    },
                    onSuggestionSelected = { customer ->
                        onSuggestionSelected(customer)
                        onCustomerConfirmedChange(true)
                    }
                )
            }
        }
    }
}

@Composable
private fun CustomerSuggestionDropdown(
    query: String,
    suggestions: List<CustomerEntity>,
    onCreateCustomer: () -> Unit,
    onSuggestionSelected: (CustomerEntity) -> Unit
) {
    val trimmedQuery = query.trim()
    val showCreateRow =
        trimmedQuery.isNotBlank() &&
                suggestions.none { it.name.equals(trimmedQuery, ignoreCase = true) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = OrderEditorFieldShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (showCreateRow) {
                Surface(
                    onClick = onCreateCustomer,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .sizeIn(minHeight = 48.dp)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text =
                                stringResource(
                                    R.string.order_editor_create_customer,
                                    trimmedQuery
                                ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (suggestions.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            suggestions.take(5).forEachIndexed { index, customer ->
                Surface(
                    onClick = { onSuggestionSelected(customer) },
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .sizeIn(minHeight = 48.dp)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (customer.phone.isNotBlank()) {
                            Text(
                                text = customer.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (index < suggestions.take(5).lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
