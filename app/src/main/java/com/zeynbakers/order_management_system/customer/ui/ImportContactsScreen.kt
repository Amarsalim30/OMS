package com.zeynbakers.order_management_system.customer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ImportContactsScreen(
    contacts: List<ImportContact>,
    selectedPhones: Set<String>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onToggleSelect: (String) -> Unit,
    onToggleSelectAll: () -> Unit,
    onImport: () -> Unit
) {
    val selectedCount = selectedPhones.size
    val allSelected = contacts.isNotEmpty() && selectedCount == contacts.size
    var query by rememberSaveable { mutableStateOf("") }
    val filteredContacts =
        contacts.filter { contact ->
            val lowered = query.trim().lowercase()
            if (lowered.isBlank()) return@filter true
            contact.name.lowercase().contains(lowered) || contact.phone.contains(lowered)
        }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Import contacts") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    TextButton(
                        onClick = onImport,
                        enabled = selectedCount > 0
                    ) {
                        Text(text = "Import ($selectedCount)")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
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

            Text(
                text = "Import customers from your phonebook. We skip empty numbers automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search contacts") },
                placeholder = { Text("Name or phone") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            if (contacts.isEmpty()) {
                Text(
                    text = "No contacts found on this device.",
                    style = MaterialTheme.typography.bodyMedium
                )
                return@Column
            }

            SelectAllRow(
                selected = allSelected,
                onToggle = onToggleSelectAll
            )

            Spacer(Modifier.height(8.dp))

            if (filteredContacts.isEmpty()) {
                Text(
                    text = "No matches for \"$query\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
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
private fun SelectAllRow(
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
                contentDescription = "Select all"
            )
            Spacer(Modifier.size(8.dp))
            Text(text = "Select all", style = MaterialTheme.typography.titleMedium)
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
                contentDescription = "Select ${contact.name}"
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
