package com.zeynbakers.order_management_system.core.ui

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import com.zeynbakers.order_management_system.core.ui.VoiceNotesMode
import com.zeynbakers.order_management_system.core.ui.VoicePreparedResult
import com.zeynbakers.order_management_system.core.ui.VoiceTarget
import com.zeynbakers.order_management_system.core.ui.LocalVoiceInputRouter
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoiceCalculatorOverlay(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    isVisible: Boolean = true,
    isSuppressed: Boolean = false,
    lockToRightOnIdle: Boolean = false,
    lockToTopOnIdle: Boolean = false,
    peekWidthDp: Dp = 24.dp,
    allowDrag: Boolean = true,
    autoHideIdleMs: Long = 2000L,
    defaultIdleYDp: Dp = 96.dp
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val voiceRouter = LocalVoiceInputRouter.current
    var mode by remember { mutableStateOf(VoiceCalcMode.Idle) }
    var transcript by remember { mutableStateOf("") }
    var prepared by remember { mutableStateOf<VoicePreparedResult?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var pendingStart by remember { mutableStateOf(false) }
    var hasUserMoved by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(IntOffset(0, 0)) }
    var listeningJob by remember { mutableStateOf<Job?>(null) }
    var resultTimeoutJob by remember { mutableStateOf<Job?>(null) }
    var lastPartial by remember { mutableStateOf("") }
    var isRevealed by remember { mutableStateOf(false) }
    var lastDockedRight by remember { mutableStateOf(true) }
    var autoHideJob by remember { mutableStateOf<Job?>(null) }

    val speechRecognizer: SpeechRecognizer? =
        remember {
            runCatching { SpeechRecognizer.createSpeechRecognizer(context) }
                .onFailure { t ->
                    Log.e("VoiceCalc", "createSpeechRecognizer failed: ${t.javaClass.simpleName}: ${t.message}", t)
                }
                .getOrNull()
        }

    if (!isVisible || isSuppressed) {
        return
    }

    fun vcLog(message: String) {
        Log.e("VoiceCalc", message)
    }

    fun startListening() {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            errorText = "Speech recognizer unavailable"
            mode = VoiceCalcMode.Error
            vcLog("speechRecognizer is null")
            return
        }
        isRevealed = false
        vcLog("startListening: hasPermission=$hasPermission mode=$mode")
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            errorText = "Speech recognition unavailable"
            mode = VoiceCalcMode.Error
            vcLog("recognition unavailable")
            return
        }
        pendingStart = false
        errorText = null
        transcript = ""
        prepared = null
        lastPartial = ""
        mode = VoiceCalcMode.Listening
        try {
            recognizer.cancel()
            vcLog("cancel ok")
        } catch (t: Throwable) {
            vcLog("cancel failed: ${t.javaClass.simpleName}: ${t.message}")
        }
        val recognizerIntent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1800)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800)
            }
        try {
            recognizer.startListening(recognizerIntent)
            vcLog("startListening ok")
        } catch (t: Throwable) {
            vcLog("startListening failed: ${t.javaClass.simpleName}: ${t.message}")
            errorText = "Voice failed to start"
            mode = VoiceCalcMode.Error
            return
        }
        listeningJob?.cancel()
        listeningJob =
            coroutineScope.launch {
                delay(12000)
                vcLog("listening timeout -> stopListening()")
                try {
                    recognizer.stopListening()
                } catch (t: Throwable) {
                    vcLog("stopListening failed: ${t.javaClass.simpleName}: ${t.message}")
                }
            }
        resultTimeoutJob?.cancel()
        resultTimeoutJob =
            coroutineScope.launch {
                delay(12000)
                if (mode == VoiceCalcMode.Listening) {
                    vcLog("result timeout -> cancel()")
                    try {
                        recognizer.cancel()
                    } catch (t: Throwable) {
                        vcLog("cancel failed: ${t.javaClass.simpleName}: ${t.message}")
                    }
                    errorText = "Didn't catch that"
                    mode = VoiceCalcMode.Error
                }
            }
    }

    DisposableEffect(speechRecognizer) {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            onDispose {
                listeningJob?.cancel()
                resultTimeoutJob?.cancel()
                autoHideJob?.cancel()
            }
        } else {
            val listener =
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        mode = VoiceCalcMode.Listening
                        vcLog("onReadyForSpeech")
                    }

                    override fun onBeginningOfSpeech() {
                        vcLog("onBeginningOfSpeech")
                    }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        vcLog("onEndOfSpeech")
                    }

                    override fun onError(error: Int) {
                        vcLog("onError=$error")
                        listeningJob?.cancel()
                        resultTimeoutJob?.cancel()
                        val fallbackText = lastPartial
                        if (fallbackText.isNotBlank()) {
                            transcript = fallbackText
                            val preparedResult = voiceRouter.prepare(fallbackText)
                            if (!preparedResult.canApply) {
                                vcLog("parse failed (error fallback)")
                                errorText = preparedResult.errorMessage ?: "Didn't catch that"
                                prepared = null
                                mode = VoiceCalcMode.Error
                            } else {
                                prepared = preparedResult
                                mode = VoiceCalcMode.Result
                                vcLog("parse ok (error fallback)")
                            }
                        } else {
                            errorText = "Didn't catch that"
                            mode = VoiceCalcMode.Error
                        }
                    }

                    override fun onResults(resultsBundle: Bundle?) {
                        vcLog("onResults")
                        listeningJob?.cancel()
                        resultTimeoutJob?.cancel()
                        val spokenText =
                            resultsBundle
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                .orEmpty()
                        val finalText = if (spokenText.isBlank()) lastPartial else spokenText
                        transcript = finalText
                        vcLog("final transcript: $finalText")
                        val preparedResult = voiceRouter.prepare(finalText)
                        if (!preparedResult.canApply) {
                            vcLog("parse failed")
                            errorText = preparedResult.errorMessage ?: "Didn't catch that"
                            prepared = null
                            mode = VoiceCalcMode.Error
                        } else {
                            prepared = preparedResult
                            mode = VoiceCalcMode.Result
                            vcLog("parse ok")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val spokenText =
                            partialResults
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                .orEmpty()
                        transcript = spokenText
                        if (spokenText.isNotBlank()) {
                            lastPartial = spokenText
                            vcLog("partial transcript: $spokenText")
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                }
            recognizer.setRecognitionListener(listener)
            onDispose {
                listeningJob?.cancel()
                resultTimeoutJob?.cancel()
                autoHideJob?.cancel()
                runCatching { recognizer.destroy() }
                    .onFailure { t ->
                        vcLog("destroy failed: ${t.javaClass.simpleName}: ${t.message}")
                    }
            }
        }
    }

    LaunchedEffect(hasPermission) {
        if (pendingStart && hasPermission) {
            startListening()
        }
    }

    LaunchedEffect(mode) {
        if (mode != VoiceCalcMode.Idle) {
            isRevealed = false
        }
    }

    LaunchedEffect(Unit) {
        isRevealed = false
    }

    LaunchedEffect(mode, isRevealed, autoHideIdleMs) {
        autoHideJob?.cancel()
        if (mode == VoiceCalcMode.Idle && isRevealed) {
            autoHideJob =
                coroutineScope.launch {
                    delay(autoHideIdleMs)
                    if (mode == VoiceCalcMode.Idle) {
                        isRevealed = false
                    }
                }
        }
    }

    val minimizedSize = 48.dp
    val isIdleExpanded = mode == VoiceCalcMode.Idle && isRevealed
    val panelWidth =
        when {
            isIdleExpanded -> 280.dp
            mode == VoiceCalcMode.Idle -> minimizedSize
            else -> 300.dp
        }
    val panelHeight =
        when {
            isIdleExpanded -> 208.dp
            mode == VoiceCalcMode.Idle -> minimizedSize
            else -> 180.dp
        }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val panelWidthPx = with(density) { panelWidth.toPx() }
        val panelHeightPx = with(density) { panelHeight.toPx() }
        val topInset = WindowInsets.systemBars.getTop(density).toFloat()
        val marginPx = with(density) { 12.dp.toPx() }
        val peekPx = with(density) { peekWidthDp.toPx() }
        val defaultIdleYPx = with(density) { defaultIdleYDp.toPx() }

        fun clampOffset(offset: IntOffset, allowPeekRight: Boolean, allowPeekLeft: Boolean): IntOffset {
            val minX =
                if (allowPeekLeft) {
                    -(panelWidthPx - peekPx)
                } else {
                    marginPx
                }
            val maxX =
                if (allowPeekRight) {
                    maxWidthPx - peekPx
                } else {
                    maxWidthPx - panelWidthPx - marginPx
                }
            val minY = topInset + marginPx
            val maxY = maxHeightPx - panelHeightPx - marginPx
            val clampedX = offset.x.toFloat().coerceIn(minX, maxX).toInt()
            val clampedY = offset.y.toFloat().coerceIn(minY, maxY).toInt()
            return IntOffset(clampedX, clampedY)
        }

        LaunchedEffect(mode, hasUserMoved, maxWidth, maxHeight, isRevealed) {
            if (mode == VoiceCalcMode.Idle && (!allowDrag || lockToRightOnIdle)) {
                val targetX =
                    if (lockToRightOnIdle) {
                        if (isRevealed) {
                            (maxWidthPx - panelWidthPx - marginPx).toInt()
                        } else {
                            (maxWidthPx - peekPx).toInt()
                        }
                    } else {
                        marginPx.toInt()
                    }
                val targetY =
                    if (lockToTopOnIdle) {
                        (topInset + marginPx).toInt()
                    } else {
                        dragOffset.y
                    }
                dragOffset =
                    clampOffset(
                        IntOffset(targetX, targetY),
                        allowPeekRight = lockToRightOnIdle && !isRevealed,
                        allowPeekLeft = false
                    )
            } else if (mode == VoiceCalcMode.Idle && allowDrag) {
                val targetX =
                    if (lastDockedRight) {
                        if (isRevealed) {
                            (maxWidthPx - panelWidthPx - marginPx).toInt()
                        } else {
                            (maxWidthPx - peekPx).toInt()
                        }
                    } else {
                        if (isRevealed) {
                            marginPx.toInt()
                        } else {
                            (-(panelWidthPx - peekPx)).toInt()
                        }
                    }
                val targetY =
                    if (!hasUserMoved) {
                        (topInset + defaultIdleYPx).toInt()
                    } else {
                        dragOffset.y
                    }
                dragOffset =
                    clampOffset(
                        IntOffset(targetX, targetY),
                        allowPeekRight = !isRevealed && lastDockedRight,
                        allowPeekLeft = !isRevealed && !lastDockedRight
                    )
            } else {
                dragOffset = clampOffset(dragOffset, allowPeekRight = false, allowPeekLeft = false)
            }
        }

        val modifier =
            if (allowDrag) {
                Modifier
                    .offset { dragOffset }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { hasUserMoved = true },
                            onDragEnd = {
                                lastDockedRight = dragOffset.x + panelWidthPx / 2 > maxWidthPx / 2
                                isRevealed = false
                                val targetX =
                                    if (lastDockedRight) {
                                        maxWidthPx - panelWidthPx - marginPx
                                    } else {
                                        marginPx
                                    }
                                dragOffset =
                                    clampOffset(
                                        IntOffset(targetX.toInt(), dragOffset.y),
                                        allowPeekRight = false,
                                        allowPeekLeft = false
                                    )
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            val newOffset =
                                IntOffset(
                                    x = dragOffset.x + dragAmount.x.toInt(),
                                    y = dragOffset.y + dragAmount.y.toInt()
                                )
                            dragOffset = clampOffset(newOffset, allowPeekRight = false, allowPeekLeft = false)
                        }
                    }
            } else {
                Modifier.offset { dragOffset }
            }

        Surface(
            tonalElevation = 4.dp,
            shape = MaterialTheme.shapes.large,
            modifier = modifier.animateContentSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            when (mode) {
                VoiceCalcMode.Idle -> {
                    if (isRevealed) {
                        VoiceIdlePanel(
                            target = voiceRouter.target,
                            hasNotesTarget = voiceRouter.hasNotesTarget,
                            followsFocus = voiceRouter.followsFocus,
                            notesMode = voiceRouter.notesMode,
                            onTargetSelect = { voiceRouter.setManualTarget(it) },
                            onEnableAuto = { voiceRouter.enableFollowFocus() },
                            onNotesModeChange = { voiceRouter.updateNotesMode(it) },
                            onStart = {
                                if (hasPermission) {
                                    startListening()
                                } else {
                                    pendingStart = true
                                    onRequestPermission()
                                }
                            },
                            onClose = { isRevealed = false }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(minimizedSize)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    shape = MaterialTheme.shapes.large
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(
                                onClick = {
                                    isRevealed = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = "Voice input",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
                VoiceCalcMode.Listening -> {
                    VoicePanel(
                        title = "Listening...",
                        transcript = transcript,
                        target = voiceRouter.target,
                        onClose = {
                            runCatching { speechRecognizer?.cancel() }
                                .onFailure { t ->
                                    vcLog("cancel failed: ${t.javaClass.simpleName}: ${t.message}")
                                }
                            mode = VoiceCalcMode.Idle
                        }
                    )
                }
                VoiceCalcMode.Result -> {
                    VoiceResultPanel(
                        prepared = prepared,
                        onApply = {
                            val appliedMessage = prepared?.let { voiceRouter.apply(it) }
                            if (!appliedMessage.isNullOrBlank()) {
                                Toast
                                    .makeText(context, appliedMessage, Toast.LENGTH_SHORT)
                                    .show()
                            }
                            mode = VoiceCalcMode.Idle
                        },
                        onAgain = { startListening() },
                        onClose = { mode = VoiceCalcMode.Idle }
                    )
                }
                VoiceCalcMode.Error -> {
                    VoiceErrorPanel(
                        errorText = errorText ?: "Didn't catch that",
                        onAgain = { startListening() },
                        onClose = { mode = VoiceCalcMode.Idle }
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceIdlePanel(
    target: VoiceTarget,
    hasNotesTarget: Boolean,
    followsFocus: Boolean,
    notesMode: VoiceNotesMode,
    onTargetSelect: (VoiceTarget) -> Unit,
    onEnableAuto: () -> Unit,
    onNotesModeChange: (VoiceNotesMode) -> Unit,
    onStart: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Voice input", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (followsFocus) "Auto" else "Manual",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!followsFocus) {
                Spacer(Modifier.width(6.dp))
                TextButton(onClick = onEnableAuto) { Text("Follow focus") }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (hasNotesTarget) {
            Text(
                text = "Target",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = target == VoiceTarget.Notes,
                    onClick = { onTargetSelect(VoiceTarget.Notes) },
                    label = { Text("Notes") }
                )
                FilterChip(
                    selected = target == VoiceTarget.Total,
                    onClick = { onTargetSelect(VoiceTarget.Total) },
                    label = { Text("Total") }
                )
            }
        } else {
            Text(
                text = "Target: Total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (hasNotesTarget && target == VoiceTarget.Notes) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Notes mode",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = notesMode == VoiceNotesMode.Append,
                    onClick = { onNotesModeChange(VoiceNotesMode.Append) },
                    label = { Text("Append") }
                )
                FilterChip(
                    selected = notesMode == VoiceNotesMode.Replace,
                    onClick = { onNotesModeChange(VoiceNotesMode.Replace) },
                    label = { Text("Replace") }
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStart) { Text("Start") }
            TextButton(onClick = onClose) { Text("Close") }
        }
    }
}

@Composable
private fun VoicePanel(
    title: String,
    transcript: String,
    target: VoiceTarget,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Target: ${targetLabel(target)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(6.dp))
        val hint =
            if (target == VoiceTarget.Notes) {
                "Say notes..."
            } else {
                "Say an amount..."
            }
        Text(
            text = if (transcript.isBlank()) hint else transcript,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onClose) {
            Text("Close")
        }
    }
}

@Composable
private fun VoiceResultPanel(
    prepared: VoicePreparedResult?,
    onApply: () -> Unit,
    onAgain: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.padding(12.dp)) {
        val target = prepared?.target ?: VoiceTarget.Total
        Text(text = "Result (${targetLabel(target)})", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            text = prepared?.transcript.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = prepared?.previewLabel.orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = prepared?.previewValue ?: "--",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onApply, enabled = prepared?.canApply == true) { Text("Use") }
            TextButton(onClick = onAgain) { Text("Again") }
            TextButton(onClick = onClose) { Text("Close") }
        }
    }
}

@Composable
private fun VoiceErrorPanel(
    errorText: String,
    onAgain: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.MicOff,
                contentDescription = "Mic error",
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.size(6.dp))
            Text(text = "Error", style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(6.dp))
        Text(text = errorText, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAgain) { Text("Again") }
            TextButton(onClick = onClose) { Text("Close") }
        }
    }
}

private fun targetLabel(target: VoiceTarget): String {
    return if (target == VoiceTarget.Notes) "Notes" else "Total"
}

private enum class VoiceCalcMode {
    Idle,
    Listening,
    Result,
    Error
}
