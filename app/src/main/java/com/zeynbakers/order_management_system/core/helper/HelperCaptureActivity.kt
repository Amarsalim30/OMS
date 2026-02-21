package com.zeynbakers.order_management_system.core.helper

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.db.DatabaseProvider
import com.zeynbakers.order_management_system.core.helper.data.HelperNoteClassifier
import com.zeynbakers.order_management_system.core.util.VoiceMathParseResult
import com.zeynbakers.order_management_system.core.util.parseVoiceMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HelperCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = HelperCaptureMode.fromWireValue(intent.getStringExtra(EXTRA_MODE))
        setContent {
            CaptureScreen(
                mode = mode,
                onClose = { finish() }
            )
        }
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
    }
}

private enum class CaptureStage {
    AwaitingPermission,
    Listening,
    Result,
    Error
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureScreen(
    mode: HelperCaptureMode,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val database = remember { DatabaseProvider.getDatabase(context.applicationContext) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var stage by remember {
        mutableStateOf(
            if (hasMicPermission) CaptureStage.Listening else CaptureStage.AwaitingPermission
        )
    }
    var transcript by remember { mutableStateOf("") }
    var calcResult by remember { mutableStateOf<VoiceMathParseResult?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var partialText by remember { mutableStateOf("") }
    var notePreview by remember { mutableStateOf<com.zeynbakers.order_management_system.core.helper.data.HelperNoteDetection?>(null) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasMicPermission = granted
            stage = if (granted) CaptureStage.Listening else CaptureStage.AwaitingPermission
        }

    val recognizer = remember {
        runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
    }

    fun resetAndListen() {
        if (!hasMicPermission) {
            stage = CaptureStage.AwaitingPermission
            return
        }
        if (recognizer == null || !SpeechRecognizer.isRecognitionAvailable(context)) {
            errorText = context.getString(R.string.helper_capture_error_speech_unavailable)
            stage = CaptureStage.Error
            return
        }
        transcript = ""
        partialText = ""
        calcResult = null
        notePreview = null
        errorText = null
        stage = CaptureStage.Listening
        val recognizeIntent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
        runCatching {
            recognizer.cancel()
            recognizer.startListening(recognizeIntent)
        }.onFailure {
            errorText = context.getString(R.string.helper_capture_error_start_failed)
            stage = CaptureStage.Error
        }
    }

    fun processTranscript(text: String) {
        val cleaned = text.trim()
        transcript = cleaned
        if (cleaned.isBlank()) {
            errorText = context.getString(R.string.helper_capture_error_no_input)
            stage = CaptureStage.Error
            return
        }
        when (mode) {
            HelperCaptureMode.VoiceCalculator -> {
                val parsed = parseVoiceMath(cleaned)
                if (parsed == null) {
                    errorText = context.getString(R.string.helper_capture_error_parse_failed)
                    stage = CaptureStage.Error
                } else {
                    calcResult = parsed
                    stage = CaptureStage.Result
                }
            }
            HelperCaptureMode.VoiceNote -> {
                notePreview = HelperNoteClassifier.classifyVoiceTranscript(cleaned)
                stage = CaptureStage.Result
            }
        }
    }

    DisposableEffect(recognizer) {
        if (recognizer != null) {
            recognizer.setRecognitionListener(
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit

                    override fun onPartialResults(partialResults: Bundle?) {
                        val text =
                            partialResults
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                .orEmpty()
                        if (text.isNotBlank()) {
                            partialText = text
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val finalText =
                            results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                .orEmpty()
                        processTranscript(if (finalText.isBlank()) partialText else finalText)
                    }

                    override fun onError(error: Int) {
                        if (partialText.isNotBlank()) {
                            processTranscript(partialText)
                        } else {
                            errorText = context.getString(R.string.helper_capture_error_retry)
                            stage = CaptureStage.Error
                        }
                    }
                }
            )
        }
        onDispose {
            runCatching {
                recognizer?.cancel()
                recognizer?.destroy()
            }
        }
    }

    LaunchedEffect(mode, hasMicPermission) {
        if (!hasMicPermission) {
            stage = CaptureStage.AwaitingPermission
        } else {
            resetAndListen()
        }
    }

    val title =
        when (mode) {
            HelperCaptureMode.VoiceNote -> stringResource(R.string.helper_action_voice_note)
            HelperCaptureMode.VoiceCalculator -> stringResource(R.string.helper_action_voice_calculator)
        }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(title) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (stage) {
                CaptureStage.AwaitingPermission -> {
                    Text(
                        text = stringResource(R.string.helper_capture_mic_required),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                            Text(stringResource(R.string.permission_primer_continue))
                        }
                        OutlinedButton(onClick = onClose) {
                            Text(stringResource(R.string.permission_primer_not_now))
                        }
                    }
                }
                CaptureStage.Listening -> {
                    Text(
                        text = stringResource(R.string.helper_capture_listening),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = partialText.ifBlank { stringResource(R.string.helper_capture_speak_hint) },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    OutlinedButton(onClick = onClose) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
                CaptureStage.Result -> {
                    if (mode == HelperCaptureMode.VoiceCalculator) {
                        val result = calcResult
                        Text(
                            text = transcript,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = result?.value?.stripTrailingZeros()?.toPlainString().orEmpty(),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val value = result?.value?.stripTrailingZeros()?.toPlainString().orEmpty()
                                    clipboard.setText(AnnotatedString(value))
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.notes_history_copied),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                enabled = result != null
                            ) {
                                Text(stringResource(R.string.helper_capture_copy_result))
                            }
                            Button(
                                onClick = {
                                    val parsed = result ?: return@Button
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            database.helperNoteDao().insert(
                                                HelperNoteClassifier.buildCalculatorNote(
                                                    expression = transcript,
                                                    result = parsed.value
                                                )
                                            )
                                        }
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.helper_capture_saved),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onClose()
                                    }
                                },
                                enabled = result != null
                            ) {
                                Text(stringResource(R.string.helper_capture_save_note))
                            }
                        }
                    } else {
                        Text(
                            text = transcript,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        notePreview?.let { detection ->
                            if (!detection.detectedAmountRaw.isNullOrBlank()) {
                                Text(
                                    text = stringResource(
                                        R.string.helper_capture_detected_amount,
                                        detection.detectedAmountRaw
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (!detection.detectedPhone.isNullOrBlank()) {
                                Text(
                                    text = stringResource(
                                        R.string.helper_capture_detected_phone,
                                        detection.detectedPhone
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            database.helperNoteDao().insert(
                                                HelperNoteClassifier.buildVoiceNote(transcript = transcript)
                                            )
                                        }
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.helper_capture_saved),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onClose()
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.action_save))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { resetAndListen() }) {
                            Text(stringResource(R.string.helper_capture_try_again))
                        }
                        OutlinedButton(onClick = onClose) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
                CaptureStage.Error -> {
                    Text(
                        text = errorText.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { resetAndListen() }) {
                            Text(stringResource(R.string.helper_capture_try_again))
                        }
                        OutlinedButton(onClick = onClose) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
            }
        }
    }
}
