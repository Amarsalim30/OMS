package com.zeynbakers.order_management_system.core.helper.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.helper.data.HelperNoteType
import com.zeynbakers.order_management_system.core.helper.data.HelperNoteWithCustomer
import com.zeynbakers.order_management_system.core.ui.LocalUiEventDispatcher
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import com.zeynbakers.order_management_system.core.ui.components.AppEmptyState
import com.zeynbakers.order_management_system.core.ui.showSnackbar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesHistoryScreen(
    viewModel: NotesHistoryViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val uiEvents = LocalUiEventDispatcher.current
    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val todayHeader = stringResource(R.string.notes_history_day_today)
    val yesterdayHeader = stringResource(R.string.notes_history_day_yesterday)
    var editTarget by remember { mutableStateOf<HelperNoteWithCustomer?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notes_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.action_search)
                        )
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = stringResource(R.string.notes_history_filter)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (showSearch) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    label = { Text(stringResource(R.string.notes_history_search_label)) },
                    placeholder = { Text(stringResource(R.string.notes_history_search_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (state.items.isEmpty()) {
                AppEmptyState(
                    title = stringResource(R.string.notes_history_empty_title),
                    body = stringResource(R.string.notes_history_empty_body)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var previousHeader: String? = null
                    state.items.forEach { row ->
                        val header = dayBucketLabel(row.note.createdAt, todayHeader, yesterdayHeader)
                        if (header != previousHeader) {
                            item(key = "header_${row.note.id}") {
                                Text(
                                    text = header,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                            }
                            previousHeader = header
                        }
                        item(key = row.note.id) {
                            NoteHistoryRow(
                                row = row,
                                onCopy = { text ->
                                    clipboard.setText(AnnotatedString(text))
                                    scope.launch { uiEvents.showSnackbar(context.getString(R.string.notes_history_copied)) }
                                },
                                onShare = { text ->
                                    val shareIntent =
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, text)
                                        }
                                    context.startActivity(
                                        Intent.createChooser(
                                            shareIntent,
                                            context.getString(R.string.notes_history_share_title)
                                        )
                                    )
                                },
                                onTogglePin = { noteId, pinned ->
                                    viewModel.togglePin(noteId, pinned)
                                },
                                onDelete = { noteId ->
                                    viewModel.delete(noteId)
                                    scope.launch { uiEvents.showSnackbar(context.getString(R.string.notes_history_deleted)) }
                                },
                                onEdit = { row ->
                                    editTarget = row
                                },
                                onCall = { phoneDigits ->
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneDigits"))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        NotesFilterSheet(
            filter = state.filter,
            onDismiss = { showFilterSheet = false },
            onSelectAllTypes = viewModel::setAllTypes,
            onToggleType = viewModel::toggleType,
            onTimeRangeChange = viewModel::setTimeRange,
            onCustomFromChange = viewModel::setCustomFrom,
            onCustomToChange = viewModel::setCustomTo,
            onPinnedFirstChange = viewModel::setPinnedFirst,
            onHasPhoneChange = viewModel::setHasPhone,
            onHasAmountChange = viewModel::setHasAmount,
            onClear = viewModel::clearFilters
        )
    }

    editTarget?.let { target ->
        NoteEditDialog(
            row = target,
            onDismiss = { editTarget = null },
            onSave = { noteId, updatedText ->
                viewModel.edit(noteId, updatedText)
                editTarget = null
                scope.launch { uiEvents.showSnackbar(context.getString(R.string.notes_history_updated)) }
            }
        )
    }
}

@Composable
private fun NoteHistoryRow(
    row: HelperNoteWithCustomer,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onTogglePin: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onEdit: (HelperNoteWithCustomer) -> Unit,
    onCall: (String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val note = row.note
    val primaryText = note.displayText.ifBlank { note.rawTranscript }
    val amountLabel =
        when {
            !note.calculatorResult.isNullOrBlank() ->
                note.calculatorResult
            !note.detectedAmountNormalized.isNullOrBlank() ->
                note.detectedAmountNormalized
            else -> null
        }
    val phoneLabel = note.detectedPhone ?: note.detectedPhoneDigits
    val metadata = buildString {
        if (!amountLabel.isNullOrBlank()) {
            append(amountLabel)
        }
        if (!phoneLabel.isNullOrBlank()) {
            if (isNotBlank()) append(" | ")
            append(phoneLabel)
        }
        if (!row.customerName.isNullOrBlank()) {
            if (isNotBlank()) append(" | ")
            append(row.customerName)
        }
    }
    val shareText =
        when {
            note.type == HelperNoteType.CALCULATOR ->
                "${note.calculatorExpression ?: note.displayText} = ${note.calculatorResult.orEmpty()}"
            else -> note.displayText
        }

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = noteTypeIcon(note.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (metadata.isNotBlank()) {
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatHistoryTimestamp(note.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (note.pinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.notes_history_pinned_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.notes_history_more_actions)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.notes_history_action_copy)) },
                    onClick = {
                        menuExpanded = false
                        if (note.type == HelperNoteType.CALCULATOR && !note.calculatorResult.isNullOrBlank()) {
                            onCopy(note.calculatorResult)
                        } else {
                            onCopy(note.displayText)
                        }
                    },
                    leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) }
                )
                if (note.type == HelperNoteType.CALCULATOR && !note.calculatorExpression.isNullOrBlank()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.notes_history_action_copy_expression)) },
                        onClick = {
                            menuExpanded = false
                            onCopy(note.calculatorExpression)
                        },
                        leadingIcon = { Icon(Icons.Filled.Calculate, contentDescription = null) }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.notes_history_action_edit)) },
                    onClick = {
                        menuExpanded = false
                        onEdit(row)
                    },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.notes_history_action_share)) },
                    onClick = {
                        menuExpanded = false
                        onShare(shareText)
                    },
                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (note.pinned) {
                                stringResource(R.string.notes_history_action_unpin)
                            } else {
                                stringResource(R.string.notes_history_action_pin)
                            }
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onTogglePin(note.id, !note.pinned)
                    },
                    leadingIcon = { Icon(Icons.Filled.PushPin, contentDescription = null) }
                )
                if (!note.detectedPhoneDigits.isNullOrBlank()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.notes_history_action_call)) },
                        onClick = {
                            menuExpanded = false
                            onCall(note.detectedPhoneDigits)
                        },
                        leadingIcon = { Icon(Icons.Filled.Call, contentDescription = null) }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.notes_history_action_delete)) },
                    onClick = {
                        menuExpanded = false
                        onDelete(note.id)
                    },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
private fun NoteEditDialog(
    row: HelperNoteWithCustomer,
    onDismiss: () -> Unit,
    onSave: (Long, String) -> Unit
) {
    var text by remember(row.note.id) {
        mutableStateOf(row.note.displayText.ifBlank { row.note.rawTranscript })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notes_history_edit_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.notes_history_edit_label)) },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(row.note.id, text) },
                enabled = text.trim().isNotEmpty()
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NotesFilterSheet(
    filter: NotesFilterState,
    onDismiss: () -> Unit,
    onSelectAllTypes: () -> Unit,
    onToggleType: (HelperNoteType) -> Unit,
    onTimeRangeChange: (NotesTimeRange) -> Unit,
    onCustomFromChange: (String) -> Unit,
    onCustomToChange: (String) -> Unit,
    onPinnedFirstChange: (Boolean) -> Unit,
    onHasPhoneChange: (Boolean) -> Unit,
    onHasAmountChange: (Boolean) -> Unit,
    onClear: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.notes_history_filter),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.notes_history_filter_type),
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter.types.isEmpty(),
                    onClick = onSelectAllTypes,
                    label = { Text(stringResource(R.string.notes_history_filter_type_all)) }
                )
                HelperNoteType.entries.forEach { type ->
                    FilterChip(
                        selected = filter.types.contains(type),
                        onClick = { onToggleType(type) },
                        label = { Text(noteTypeLabel(type)) }
                    )
                }
            }

            Text(
                text = stringResource(R.string.notes_history_filter_time),
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeChip(filter.timeRange, NotesTimeRange.ALL, R.string.notes_history_time_all, onTimeRangeChange)
                TimeChip(filter.timeRange, NotesTimeRange.TODAY, R.string.action_today, onTimeRangeChange)
                TimeChip(filter.timeRange, NotesTimeRange.LAST_7_DAYS, R.string.notes_history_time_7_days, onTimeRangeChange)
                TimeChip(filter.timeRange, NotesTimeRange.LAST_30_DAYS, R.string.notes_history_time_30_days, onTimeRangeChange)
                TimeChip(filter.timeRange, NotesTimeRange.CUSTOM, R.string.notes_history_time_custom, onTimeRangeChange)
            }
            if (filter.timeRange == NotesTimeRange.CUSTOM) {
                OutlinedTextField(
                    value = filter.customFrom,
                    onValueChange = onCustomFromChange,
                    label = { Text(stringResource(R.string.notes_history_custom_from)) },
                    placeholder = { Text(stringResource(R.string.notes_history_date_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = filter.customTo,
                    onValueChange = onCustomToChange,
                    label = { Text(stringResource(R.string.notes_history_custom_to)) },
                    placeholder = { Text(stringResource(R.string.notes_history_date_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FilterSwitchRow(
                title = stringResource(R.string.notes_history_filter_pinned_first),
                checked = filter.pinnedFirst,
                onCheckedChange = onPinnedFirstChange
            )
            FilterSwitchRow(
                title = stringResource(R.string.notes_history_filter_has_phone),
                checked = filter.hasPhone,
                onCheckedChange = onHasPhoneChange
            )
            FilterSwitchRow(
                title = stringResource(R.string.notes_history_filter_has_amount),
                checked = filter.hasAmount,
                onCheckedChange = onHasAmountChange
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.action_clear))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        }
    }
}

@Composable
private fun FilterSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun TimeChip(
    selectedRange: NotesTimeRange,
    range: NotesTimeRange,
    labelRes: Int,
    onTimeRangeChange: (NotesTimeRange) -> Unit
) {
    FilterChip(
        selected = selectedRange == range,
        onClick = { onTimeRangeChange(range) },
        label = { Text(stringResource(labelRes)) }
    )
}

@Composable
private fun noteTypeLabel(type: HelperNoteType): String {
    return when (type) {
        HelperNoteType.VOICE -> stringResource(R.string.notes_history_type_voice)
        HelperNoteType.TEXT -> stringResource(R.string.notes_history_type_text)
        HelperNoteType.MONEY -> stringResource(R.string.notes_history_type_money)
        HelperNoteType.PHONE -> stringResource(R.string.notes_history_type_phone)
        HelperNoteType.CALCULATOR -> stringResource(R.string.notes_history_type_calculator)
    }
}

private fun noteTypeIcon(type: HelperNoteType) =
    when (type) {
        HelperNoteType.VOICE -> Icons.Filled.Mic
        HelperNoteType.TEXT -> Icons.AutoMirrored.Filled.Notes
        HelperNoteType.MONEY -> Icons.Filled.Payments
        HelperNoteType.PHONE -> Icons.Filled.Phone
        HelperNoteType.CALCULATOR -> Icons.Filled.Calculate
}

private fun dayBucketLabel(
    epochMillis: Long,
    todayLabel: String,
    yesterdayLabel: String
): String {
    val zone = ZoneId.systemDefault()
    val noteDate = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when {
        noteDate == today -> todayLabel
        noteDate == today.minusDays(1) -> yesterdayLabel
        else -> noteDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    }
}

private fun formatHistoryTimestamp(epochMillis: Long): String {
    val zone = ZoneId.systemDefault()
    return Instant.ofEpochMilli(epochMillis)
        .atZone(zone)
        .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm"))
}

