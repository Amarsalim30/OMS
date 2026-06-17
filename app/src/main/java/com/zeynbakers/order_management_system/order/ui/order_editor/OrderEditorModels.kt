package com.zeynbakers.order_management_system.order.ui.order_editor

data class OrderEditorTutorialHint(
    val stepText: String,
    val title: String,
    val body: String,
    val continueLabel: String,
    val skipLabel: String,
    val showContinue: Boolean,
    val onContinue: () -> Unit,
    val onSkip: () -> Unit
)
