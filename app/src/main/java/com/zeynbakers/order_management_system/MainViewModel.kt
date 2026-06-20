package com.zeynbakers.order_management_system

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.core.navigation.extractSharedText
import com.zeynbakers.order_management_system.core.ui.UiEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _selectedTopLevelRoute = MutableStateFlow(AppRoutes.Calendar)
    val selectedTopLevelRoute = _selectedTopLevelRoute.asStateFlow()

    private val _showMoreSheet = MutableStateFlow(false)
    val showMoreSheet = _showMoreSheet.asStateFlow()

    private val _tutorialActive = MutableStateFlow(false)
    val tutorialActive = _tutorialActive.asStateFlow()

    private val _tutorialStepIndex = MutableStateFlow(0)
    val tutorialStepIndex = _tutorialStepIndex.asStateFlow()

    // Shared text preserved across process death
    val pendingSharedPaymentText: StateFlow<String?> =
        savedStateHandle.getStateFlow(KEY_PENDING_TEXT, null)

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    fun onSelectedTopLevelRouteChange(route: String) {
        _selectedTopLevelRoute.value = route
    }

    fun onShowMoreSheetChange(show: Boolean) {
        _showMoreSheet.value = show
    }

    fun onStartTutorial(stepIndex: Int) {
        _tutorialStepIndex.value = stepIndex
        _tutorialActive.value = true
        _showMoreSheet.value = false
    }

    fun onTutorialStepChange(stepIndex: Int) {
        _tutorialStepIndex.value = stepIndex
    }

    fun onDismissTutorial() {
        _tutorialActive.value = false
        _tutorialStepIndex.value = 0
        _showMoreSheet.value = false
    }

    fun onIntentReceived(intent: Intent?) {
        if (intent == null) return
        extractSharedText(intent)?.let { sharedText ->
            savedStateHandle[KEY_PENDING_TEXT] = sharedText
        }
    }

    fun consumePendingSharedText() {
        savedStateHandle[KEY_PENDING_TEXT] = null
    }

    fun showSnackbar(message: String) {
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.Snackbar(message))
        }
    }

    companion object {
        private const val KEY_PENDING_TEXT = "pending_shared_text"
    }
}
