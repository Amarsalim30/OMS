package com.zeynbakers.order_management_system.core.helper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.core.helper.data.HelperNoteType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesHistoryViewModel(private val database: AppDatabase) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(NotesFilterState())

    val uiState: StateFlow<NotesHistoryUiState> =
        combine(
            database.helperNoteDao().observeActive(),
            query,
            filter
        ) { notes, queryText, currentFilter ->
            NotesHistoryUiState(
                items = filterHelperNotes(notes, queryText, currentFilter),
                query = queryText,
                filter = currentFilter
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotesHistoryUiState()
        )

    fun setQuery(value: String) {
        query.value = value
    }

    fun toggleType(type: HelperNoteType) {
        val current = filter.value.types
        val updated =
            if (current.contains(type)) {
                current - type
            } else {
                current + type
            }
        filter.value = filter.value.copy(types = updated)
    }

    fun setAllTypes() {
        filter.value = filter.value.copy(types = emptySet())
    }

    fun setTimeRange(range: NotesTimeRange) {
        filter.value = filter.value.copy(timeRange = range)
    }

    fun setCustomFrom(value: String) {
        filter.value = filter.value.copy(customFrom = value)
    }

    fun setCustomTo(value: String) {
        filter.value = filter.value.copy(customTo = value)
    }

    fun setPinnedFirst(enabled: Boolean) {
        filter.value = filter.value.copy(pinnedFirst = enabled)
    }

    fun setHasPhone(enabled: Boolean) {
        filter.value = filter.value.copy(hasPhone = enabled)
    }

    fun setHasAmount(enabled: Boolean) {
        filter.value = filter.value.copy(hasAmount = enabled)
    }

    fun clearFilters() {
        filter.value = NotesFilterState()
    }

    fun togglePin(noteId: Long, pinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            database.helperNoteDao().setPinned(
                id = noteId,
                pinned = pinned,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun delete(noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            database.helperNoteDao().softDelete(
                id = noteId,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun edit(noteId: Long, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = database.helperNoteDao().getById(noteId) ?: return@launch
            val updated = buildEditedHelperNote(existing, text) ?: return@launch
            database.helperNoteDao().update(updated)
        }
    }
}
