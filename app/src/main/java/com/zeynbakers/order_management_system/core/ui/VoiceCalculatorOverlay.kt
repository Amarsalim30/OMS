package com.zeynbakers.order_management_system.core.ui

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
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
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.core.util.parseVoiceMath
import java.math.BigDecimal
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoiceCalculatorOverlay(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onApplyAmount: (BigDecimal) -> Unit,
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
    var mode by remember { mutableStateOf(VoiceCalcMode.Idle) }
    var transcript by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<BigDecimal?>(null) }
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

    val speechRecognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(context)
    }

    if (!isVisible || isSuppressed) {
        return
    }

    fun vcLog(message: String) {
        Log.e("VoiceCalc", message)
    }

    fun startListening() {
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
        result = null
        lastPartial = ""
        mode = VoiceCalcMode.Listening
        try {
            speechRecognizer.cancel()
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
            speechRecognizer.startListening(recognizerIntent)
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
                    speechRecognizer.stopListening()
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
                        speechRecognizer.cancel()
                    } catch (t: Throwable) {
                        vcLog("cancel failed: ${t.javaClass.simpleName}: ${t.message}")
                    }
                    errorText = "Didn't catch that"
                    mode = VoiceCalcMode.Error
                }
            }
    }

    DisposableEffect(speechRecognizer) {
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
                        val parsed = parseVoiceMath(fallbackText)
                        if (parsed == null) {
                            vcLog("parse failed (error fallback)")
                            errorText = "Didn't catch that"
                            mode = VoiceCalcMode.Error
                        } else {
                            result = parsed.value
                            mode = VoiceCalcMode.Result
                            vcLog("parse ok (error fallback): ${parsed.value}")
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
                    val parsed = parseVoiceMath(finalText)
                    if (parsed == null) {
                        vcLog("parse failed")
                        errorText = "Didn't catch that"
                        mode = VoiceCalcMode.Error
                    } else {
                        result = parsed.value
                        mode = VoiceCalcMode.Result
                        vcLog("parse ok: ${parsed.value}")
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
        speechRecognizer.setRecognitionListener(listener)
        onDispose {
            listeningJob?.cancel()
            resultTimeoutJob?.cancel()
            autoHideJob?.cancel()
            speechRecognizer.destroy()
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
    val panelWidth = if (mode == VoiceCalcMode.Idle) minimizedSize else 300.dp
    val panelHeight = if (mode == VoiceCalcMode.Idle) minimizedSize else 180.dp

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
                                if (mode == VoiceCalcMode.Idle && !isRevealed) {
                                    isRevealed = true
                                } else {
                                    if (hasPermission) {
                                        startListening()
                                    } else {
                                        pendingStart = true
                                        onRequestPermission()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Voice calculator",
                                tint = Color.White
                            )
                        }
                    }
                }
                VoiceCalcMode.Listening -> {
                    VoicePanel(
                        title = "Listening...",
                        transcript = transcript,
                        result = result,
                        onClose = {
                            speechRecognizer.cancel()
                            mode = VoiceCalcMode.Idle
                        }
                    )
                }
                VoiceCalcMode.Result -> {
                    VoiceResultPanel(
                        transcript = transcript,
                        result = result,
                        onApply = {
                            result?.let { onApplyAmount(it) }
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
private fun VoicePanel(
    title: String,
    transcript: String,
    result: BigDecimal?,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.padding(12.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (transcript.isBlank()) "Say an amount..." else transcript,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = result?.let { formatKes(it) } ?: "--",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onClose) {
            Text("Close")
        }
    }
}

@Composable
private fun VoiceResultPanel(
    transcript: String,
    result: BigDecimal?,
    onApply: () -> Unit,
    onAgain: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.padding(12.dp)) {
        Text(text = "Result", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            text = transcript,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = result?.let { formatKes(it) } ?: "--",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onApply, enabled = result != null) { Text("Use") }
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

private enum class VoiceCalcMode {
    Idle,
    Listening,
    Result,
    Error
}
