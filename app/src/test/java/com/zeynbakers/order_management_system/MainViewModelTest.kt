package com.zeynbakers.order_management_system

import androidx.lifecycle.SavedStateHandle
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.core.ui.UiEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    @Test
    fun `navigation state changes update flow`() = runTest {
        val handle = SavedStateHandle()
        val viewModel = MainViewModel(handle)

        viewModel.onSelectedTopLevelRouteChange(AppRoutes.Customers)
        assertEquals(AppRoutes.Customers, viewModel.selectedTopLevelRoute.value)
    }

    @Test
    fun `tutorial lifecycle updates state correctly`() = runTest {
        val handle = SavedStateHandle()
        val viewModel = MainViewModel(handle)

        viewModel.onStartTutorial(2)
        assertEquals(true, viewModel.tutorialActive.value)
        assertEquals(2, viewModel.tutorialStepIndex.value)

        viewModel.onDismissTutorial()
        assertEquals(false, viewModel.tutorialActive.value)
        assertEquals(0, viewModel.tutorialStepIndex.value)
    }

    @Test
    fun `shared text is preserved and consumed`() = runTest {
        val handle = SavedStateHandle()
        val viewModel = MainViewModel(handle)

        // Mocking extractSharedText is hard with static methods, 
        // so we test the state handle logic directly if possible,
        // or just verify the direct handle usage.

        handle["pending_shared_text"] = "Shared Message"
        assertEquals("Shared Message", viewModel.pendingSharedPaymentText.value)

        viewModel.consumePendingSharedText()
        assertNull(viewModel.pendingSharedPaymentText.value)
    }

    @Test
    fun `ui events are emitted`() = runTest {
        val handle = SavedStateHandle()
        val viewModel = MainViewModel(handle)

        viewModel.showSnackbar("Test Message")

        val event = viewModel.uiEvents.first()
        assertEquals("Test Message", (event as UiEvent.Snackbar).message)
    }
}
