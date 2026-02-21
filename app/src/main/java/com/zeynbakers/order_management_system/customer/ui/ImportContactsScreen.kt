package com.zeynbakers.order_management_system.customer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ImportContactsScreen(
    contacts: List<ImportContact>,
    selectedPhones: Set<String>,
    isLoading: Boolean,
    hasPermission: Boolean,
    isPermissionPermanentlyDenied: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetryLoad: () -> Unit,
    onToggleSelect: (String) -> Unit,
    onToggleSelectAll: (List<String>) -> Unit,
    onImport: () -> Unit
) {
    val selectedCount = selectedPhones.size
    var query by rememberSaveable { mutableStateOf("") }
    val filteredContacts =
        contacts.filter { contact ->
            val lowered = query.trim().lowercase()
            if (lowered.isBlank()) return@filter true
            contact.name.lowercase().contains(lowered) || contact.phone.contains(lowered)
        }
    val visiblePhones = filteredContacts.map { it.phone }
    val allVisibleSelected =
        visiblePhones.isNotEmpty() && visiblePhones.all { selectedPhones.contains(it) }
    val isFiltered = query.isNotBlank() && filteredContacts.size != contacts.size
    val selectedLabel =
        pluralStringResource(
            id = R.plurals.import_contacts_selected_count,
            count = selectedCount,
            selectedCount
        )

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.import_contacts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (hasPermission && errorMessage == null && !isLoading) {
                Surface(tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onImport,
                            enabled = selectedCount > 0
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.import_contacts_action_with_count,
                                    selectedCount
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(start = 12.dp, end = 12.dp, top = 12.dp)
        ) {
            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (!hasPermission) {
                PermissionRequiredCard(
                    isPermanentlyDenied = isPermissionPermanentlyDenied,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings
                )
                return@Column
            }

            if (errorMessage != null) {
                LoadErrorCard(
                    message = errorMessage,
                    onRetry = onRetryLoad
                )
                return@Column
            }

            Text(
                text = stringResource(R.string.import_contacts_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.import_contacts_search_label)) },
                placeholder = { Text(stringResource(R.string.import_contacts_search_placeholder)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            if (contacts.isEmpty()) {
                Text(
                    text = stringResource(R.string.import_contacts_empty_device),
                    style = MaterialTheme.typography.bodyMedium
                )
                return@Column
            }

            SelectAllRow(
                label =
                    if (isFiltered) {
                        stringResource(R.string.import_contacts_select_all_visible)
                    } else {
                        stringResource(R.string.import_contacts_select_all)
                    },
                selected = allVisibleSelected,
                onToggle = { onToggleSelectAll(visiblePhones) }
            )

            Spacer(Modifier.height(8.dp))

            if (filteredContacts.isEmpty()) {
                Text(
                    text = stringResource(R.string.import_contacts_no_matches, query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredContacts, key = { it.phone }) { contact ->
                    ContactRow(
                        contact = contact,
                        selected = selectedPhones.contains(contact.phone),
                        onToggle = { onToggleSelect(contact.phone) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRequiredCard(
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.import_contacts_permission_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.import_contacts_permission_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.import_contacts_grant_permission))
            }
            if (isPermanentlyDenied) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.permission_primer_open_settings))
                }
            }
        }
    }
}

@Composable
private fun LoadErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.import_contacts_error_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
private fun SelectAllRow(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundCheckToggle(
                selected = selected,
                onToggle = onToggle,
                contentDescription = stringResource(R.string.import_contacts_select_all)
            )
            Spacer(Modifier.size(8.dp))
            Text(text = label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ContactRow(
    contact: ImportContact,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundCheckToggle(
                selected = selected,
                onToggle = onToggle,
                contentDescription =
                    stringResource(R.string.import_contacts_select_contact, contact.name)
            )
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = contact.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RoundCheckToggle(
    selected: Boolean,
    onToggle: () -> Unit,
    contentDescription: String
) {
    IconToggleButton(
        checked = selected,
        onCheckedChange = { onToggle() }
    ) {
        val icon =
            if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

