package com.zeynbakers.order_management_system.core.helper

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.zeynbakers.order_management_system.MainActivity
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.db.DatabaseProvider
import com.zeynbakers.order_management_system.core.helper.data.HelperNoteClassifier
import com.zeynbakers.order_management_system.core.helper.data.HelperNoteDetection
import com.zeynbakers.order_management_system.core.notifications.NotificationChannels
import com.zeynbakers.order_management_system.core.util.VoiceMathParseResult
import com.zeynbakers.order_management_system.core.util.parseVoiceMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

class HelperOverlayService : Service() {
    private val bubbleSizeDp = 52
    private val captureCardWidthDp = 248
    private val bubbleDefaultXdp = 20
    private val bubbleDefaultYdp = 120
    private val edgeInsetDp = 8
    private val peekVisibleDp = 10 //24
    private val panelOffsetDp = 58

    private val notificationId = 4102
    private val requestCodeOpenApp = 9001
    private val requestCodeVoiceNote = 9002
    private val requestCodeVoiceCalculator = 9003
    private val requestCodeReveal = 9004
    private val requestCodeStop = 9005

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var panelView: View? = null
    private var captureView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var captureParams: WindowManager.LayoutParams? = null
    private var bubbleBackground: GradientDrawable? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val helperPreferences by lazy { HelperPreferences(applicationContext) }
    private val database by lazy { DatabaseProvider.getDatabase(applicationContext) }
    private var settingsState = HelperSettingsState()
    private var activeTheme = HelperOverlayThemes.resolve(settingsState.themePreset)
    private var autoPeekJob: Job? = null
    private var bubbleDockLeft = false
    private var bubblePeeked = false

    private var captureMode: HelperCaptureMode? = null
    private var captureStage: CaptureStage = CaptureStage.Idle
    private var transcript: String = ""
    private var partialText: String = ""
    private var calcResult: VoiceMathParseResult? = null
    private var notePreview: HelperNoteDetection? = null
    private var errorText: String? = null
    private var recognizer: SpeechRecognizer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (!startAsForeground()) {
            stopSelf()
            return
        }
        refreshOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            HelperOverlayController.ACTION_STOP -> {
                serviceScope.launch(Dispatchers.IO) {
                    helperPreferences.setEnabled(false)
                }
                stopSelf()
                return START_NOT_STICKY
            }
            HelperOverlayController.ACTION_CAPTURE_NOTE -> {
                handleCaptureAction(HelperCaptureMode.VoiceNote)
                return START_STICKY
            }
            HelperOverlayController.ACTION_CAPTURE_CALCULATOR -> {
                handleCaptureAction(HelperCaptureMode.VoiceCalculator)
                return START_STICKY
            }
            HelperOverlayController.ACTION_REVEAL -> {
                handleRevealAction()
                return START_STICKY
            }
            HelperOverlayController.ACTION_START,
            HelperOverlayController.ACTION_REFRESH,
            null -> {
                if (!startAsForeground()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                refreshOverlay()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        cancelAutoPeek()
        removeCapture()
        removePanel()
        removeBubble()
        destroyRecognizer()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startAsForeground(): Boolean {
        val notification = buildNotification()
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    notificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(notificationId, notification)
            }
        }.isSuccess
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent =
            PendingIntent.getActivity(
                this,
                requestCodeOpenApp,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        val notePendingIntent =
            capturePendingIntent(HelperOverlayController.ACTION_CAPTURE_NOTE, requestCodeVoiceNote)
        val calcPendingIntent =
            capturePendingIntent(HelperOverlayController.ACTION_CAPTURE_CALCULATOR, requestCodeVoiceCalculator)
        val revealPendingIntent =
            capturePendingIntent(HelperOverlayController.ACTION_REVEAL, requestCodeReveal)
        val stopPendingIntent =
            PendingIntent.getService(
                this,
                requestCodeStop,
                Intent(this, HelperOverlayService::class.java).setAction(HelperOverlayController.ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val bodyRes =
            if (HelperPermissions.hasOverlayPermission(this) && !settingsState.fallbackOnly) {
                R.string.helper_notification_body
            } else {
                R.string.helper_notification_body_fallback
            }

        return NotificationCompat.Builder(this, NotificationChannels.HELPER_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.helper_notification_title))
            .setContentText(getString(bodyRes))
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                R.drawable.ic_notification,
                getString(R.string.helper_action_voice_note),
                notePendingIntent
            )
            .addAction(
                R.drawable.ic_notification,
                getString(R.string.helper_action_voice_calculator),
                calcPendingIntent
            )
            .addAction(
                R.drawable.ic_notification,
                getString(R.string.helper_action_show),
                revealPendingIntent
            )
            .addAction(
                R.drawable.ic_notification,
                getString(R.string.helper_action_turn_off),
                stopPendingIntent
            )
            .build()
    }

    private fun capturePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, HelperOverlayService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun refreshOverlay() {
        serviceScope.launch {
            settingsState = withContext(Dispatchers.IO) { helperPreferences.readState() }
            activeTheme = HelperOverlayThemes.resolve(settingsState.themePreset)
            val shouldShowOverlay =
                !settingsState.fallbackOnly && HelperPermissions.hasOverlayPermission(this@HelperOverlayService)
            if (!shouldShowOverlay) {
                cancelAutoPeek()
                removeCapture()
                removePanel()
                removeBubble()
                return@launch
            }
            ensureBubble()
            applyBubblePositionFromSettings()
            applyThemeToVisibleViews()
            scheduleAutoPeek()
        }
    }

    private fun handleRevealAction() {
        serviceScope.launch {
            settingsState = withContext(Dispatchers.IO) { helperPreferences.readState() }
            activeTheme = HelperOverlayThemes.resolve(settingsState.themePreset)
            val canOverlay =
                !settingsState.fallbackOnly && HelperPermissions.hasOverlayPermission(this@HelperOverlayService)
            if (!canOverlay) return@launch
            ensureBubble()
            revealBubble(savePosition = false)
        }
    }

    private fun ensureBubble() {
        if (bubbleView != null) return
        val manager = windowManager ?: return
        val size = bubbleSizePx()
        val params =
            WindowManager.LayoutParams(
                size,
                size,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = resolveInitialBubbleX()
                y = resolveInitialBubbleY()
            }
        bubbleDockLeft =
            if (settingsState.hasSavedBubblePosition) {
                settingsState.dockLeft
            } else {
                params.x + (size / 2) < (screenWidthPx() / 2)
            }
        bubblePeeked = false
        val icon =
            ImageView(this).apply {
                setImageResource(android.R.drawable.ic_btn_speak_now)
                setColorFilter(ContextCompat.getColor(this@HelperOverlayService, android.R.color.white))
            }
        val backgroundDrawable =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(activeTheme.bubbleColorFor(HelperBubbleVisualState.Idle))
            }
        val bubble =
            FrameLayout(this).apply {
                background = backgroundDrawable
                contentDescription = getString(R.string.helper_notification_title)
                isClickable = true
                isFocusable = true
                addView(
                    icon,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    )
                )
                setOnClickListener {
                    when {
                        captureView != null -> removeCapture()
                        panelView == null -> showPanel()
                        else -> removePanel()
                    }
                }
                setOnTouchListener(BubbleTouchListener(params))
            }
        runCatching {
            manager.addView(bubble, params)
            bubbleView = bubble
            bubbleParams = params
            bubbleBackground = backgroundDrawable
            applyBubbleVisualState()
        }
    }

    private fun removeBubble() {
        val manager = windowManager ?: return
        val bubble = bubbleView ?: return
        runCatching { manager.removeView(bubble) }
        bubbleView = null
        bubbleParams = null
        bubbleBackground = null
        bubblePeeked = false
    }

    private fun showPanel() {
        if (panelView != null) return
        cancelAutoPeek()
        revealBubble(savePosition = false, schedulePeek = false)
        removeCapture()
        val manager = windowManager ?: return
        val bubbleLayout = bubbleParams ?: return
        val layout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val pad = dpToPx(8)
                setPadding(pad, pad, pad, pad)
                background = createCardBackground()
                addView(
                    actionButton(
                        text = getString(R.string.helper_action_voice_note),
                        onClick = { handleCaptureAction(HelperCaptureMode.VoiceNote) }
                    )
                )
                addView(
                    actionButton(
                        text = getString(R.string.helper_action_voice_calculator),
                        onClick = { handleCaptureAction(HelperCaptureMode.VoiceCalculator) }
                    )
                )
            }
        val params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = bubbleLayout.x
                y = bubbleLayout.y + dpToPx(panelOffsetDp)
            }
        runCatching {
            manager.addView(layout, params)
            panelView = layout
            panelParams = params
        }
    }

    private fun removePanel() {
        val manager = windowManager ?: return
        val panel = panelView ?: return
        runCatching { manager.removeView(panel) }
        panelView = null
        panelParams = null
        scheduleAutoPeek()
    }

    private fun actionButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setAllCaps(false)
            minimumHeight = dpToPx(48)
            stylePrimaryButton(this)
            setOnClickListener {
                onClick()
                removePanel()
            }
        }
    }

    private fun openCapture(mode: HelperCaptureMode) {
        val intent =
            Intent(this, HelperCaptureActivity::class.java).apply {
                putExtra(HelperCaptureActivity.EXTRA_MODE, mode.wireValue)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        startActivity(intent)
    }

    private fun handleCaptureAction(mode: HelperCaptureMode) {
        serviceScope.launch {
            settingsState = withContext(Dispatchers.IO) { helperPreferences.readState() }
            activeTheme = HelperOverlayThemes.resolve(settingsState.themePreset)
            if (!settingsState.fallbackOnly && HelperPermissions.hasOverlayPermission(this@HelperOverlayService)) {
                ensureBubble()
                revealBubble(savePosition = false, schedulePeek = false)
                removePanel()
            }
            // Keep microphone capture in a user-visible Activity to satisfy while-in-use rules.
            removeCapture()
            openCapture(mode)
        }
    }

    private fun showCapture(mode: HelperCaptureMode) {
        cancelAutoPeek()
        captureMode = mode
        captureStage = CaptureStage.Idle
        transcript = ""
        partialText = ""
        calcResult = null
        notePreview = null
        errorText = null
        removePanel()
        ensureCaptureView()
        updateFloatingPanelPositions()
        renderCapture()
        applyBubbleVisualState()
    }

    private fun ensureCaptureView() {
        if (captureView != null) return
        val manager = windowManager ?: return
        val card =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val padding = dpToPx(10)
                setPadding(padding, padding, padding, padding)
                background = createCardBackground()
            }
        val params =
            WindowManager.LayoutParams(
                dpToPx(captureCardWidthDp),
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = clampBubbleXVisible(dpToPx(bubbleDefaultXdp))
                y = clampBubbleY(dpToPx(bubbleDefaultYdp + 64))
            }
        runCatching {
            manager.addView(card, params)
            captureView = card
            captureParams = params
        }
    }

    private fun removeCapture() {
        runCatching { recognizer?.cancel() }
        val manager = windowManager
        val panel = captureView
        if (manager != null && panel != null) {
            runCatching { manager.removeView(panel) }
        }
        captureView = null
        captureParams = null
        captureMode = null
        captureStage = CaptureStage.Idle
        transcript = ""
        partialText = ""
        calcResult = null
        notePreview = null
        errorText = null
        applyBubbleVisualState()
        scheduleAutoPeek()
    }

    private fun renderCapture() {
        val container = captureView as? LinearLayout ?: return
        val mode = captureMode ?: return
        container.removeAllViews()
        container.background = createCardBackground()
        container.addView(captureHeader(mode))
        when (captureStage) {
            CaptureStage.Idle -> {
                container.addView(bodyText(getString(R.string.helper_capture_speak_hint)))
            }
            CaptureStage.NeedsPermission -> {
                container.addView(bodyText(getString(R.string.helper_capture_mic_required)))
                container.addView(
                    actionRow(
                        primaryText = getString(R.string.permission_primer_open_settings),
                        primaryClick = { openAppSettings() },
                        secondaryText = getString(R.string.action_cancel),
                        secondaryClick = { removeCapture() }
                    )
                )
            }
            CaptureStage.Listening -> {
                container.addView(
                    bodyText(
                        getString(R.string.helper_capture_listening),
                        isImportant = true
                    )
                )
                val content =
                    partialText.ifBlank {
                        getString(R.string.helper_capture_speak_hint)
                    }
                container.addView(bodyText(content))
                container.addView(
                    actionRow(
                        primaryText = getString(R.string.action_cancel),
                        primaryClick = { removeCapture() }
                    )
                )
            }
            CaptureStage.Result -> {
                if (mode == HelperCaptureMode.VoiceCalculator) {
                    container.addView(bodyText(transcript))
                    container.addView(
                        bodyText(
                            text = calcResult?.value?.stripTrailingZeros()?.toPlainString().orEmpty(),
                            isImportant = true,
                            textSizeSp = 26f
                        )
                    )
                    container.addView(
                        actionRow(
                            primaryText = getString(R.string.helper_capture_copy_result),
                            primaryClick = { copyResultToClipboard() },
                            secondaryText = getString(R.string.helper_capture_save_note),
                            secondaryClick = { saveCaptureAndClose() }
                        )
                    )
                } else {
                    container.addView(bodyText(transcript))
                    notePreview?.detectedAmountRaw?.let { amount ->
                        container.addView(
                            bodyText(getString(R.string.helper_capture_detected_amount, amount))
                        )
                    }
                    notePreview?.detectedPhone?.let { phone ->
                        container.addView(
                            bodyText(getString(R.string.helper_capture_detected_phone, phone))
                        )
                    }
                    container.addView(
                        actionRow(
                            primaryText = getString(R.string.action_save),
                            primaryClick = { saveCaptureAndClose() }
                        )
                    )
                }
                container.addView(
                    actionRow(
                        primaryText = getString(R.string.helper_capture_try_again),
                        primaryClick = { startListening() },
                        secondaryText = getString(R.string.action_cancel),
                        secondaryClick = { removeCapture() }
                    )
                )
            }
            CaptureStage.Error -> {
                container.addView(
                    bodyText(
                        text = errorText.orEmpty(),
                        textColor = activeTheme.bubbleErrorColor
                    )
                )
                container.addView(
                    actionRow(
                        primaryText = getString(R.string.helper_capture_try_again),
                        primaryClick = { startListening() },
                        secondaryText = getString(R.string.action_cancel),
                        secondaryClick = { removeCapture() }
                    )
                )
            }
        }
    }

    private fun captureHeader(mode: HelperCaptureMode): View {
        val titleRes =
            if (mode == HelperCaptureMode.VoiceCalculator) {
                R.string.helper_action_voice_calculator
            } else {
                R.string.helper_action_voice_note
            }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                TextView(this@HelperOverlayService).apply {
                    text = getString(titleRes)
                    setTypeface(typeface, Typeface.BOLD)
                    textSize = 17f
                    setTextColor(activeTheme.titleColor)
                    layoutParams =
                        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
            )
            addView(
                ImageButton(this@HelperOverlayService).apply {
                    setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    background = null
                    contentDescription = getString(R.string.action_cancel)
                    setColorFilter(activeTheme.titleColor)
                    setOnClickListener { removeCapture() }
                }
            )
        }
    }

    private fun bodyText(
        text: String,
        isImportant: Boolean = false,
        textColor: Int = activeTheme.bodyColor,
        textSizeSp: Float = if (isImportant) 19f else 14f
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = textSizeSp
            setTextColor(textColor)
            if (isImportant) {
                setTypeface(typeface, Typeface.BOLD)
            }
            setPadding(0, dpToPx(4), 0, dpToPx(4))
        }
    }

    private fun actionRow(
        primaryText: String,
        primaryClick: () -> Unit,
        secondaryText: String? = null,
        secondaryClick: (() -> Unit)? = null
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dpToPx(4), 0, 0)
            addView(
                Button(this@HelperOverlayService).apply {
                    text = primaryText
                    setAllCaps(false)
                    minimumHeight = dpToPx(48)
                    stylePrimaryButton(this)
                    setOnClickListener { primaryClick() }
                    layoutParams =
                        LinearLayout.LayoutParams(
                            if (secondaryText == null) LinearLayout.LayoutParams.WRAP_CONTENT else 0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            if (secondaryText == null) 0f else 1f
                        )
                }
            )
            if (secondaryText != null && secondaryClick != null) {
                addView(
                Button(this@HelperOverlayService).apply {
                    text = secondaryText
                    setAllCaps(false)
                    minimumHeight = dpToPx(48)
                    styleSecondaryButton(this)
                    setOnClickListener { secondaryClick() }
                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            ).apply {
                                marginStart = dpToPx(8)
                            }
                    }
                )
            }
        }
    }

    private fun stylePrimaryButton(button: Button) {
        button.backgroundTintList = ColorStateList.valueOf(activeTheme.primaryButtonColor)
        button.setTextColor(activeTheme.primaryButtonTextColor)
    }

    private fun styleSecondaryButton(button: Button) {
        button.backgroundTintList = ColorStateList.valueOf(activeTheme.secondaryButtonColor)
        button.setTextColor(activeTheme.secondaryButtonTextColor)
    }

    private fun createCardBackground(): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dpToPx(16).toFloat()
            setColor(activeTheme.cardBackgroundColor)
            setStroke(dpToPx(1), activeTheme.cardBorderColor)
        }
    }

    private fun startListening() {
        cancelAutoPeek()
        if (!HelperPermissions.hasMicrophonePermission(this)) {
            captureStage = CaptureStage.NeedsPermission
            renderCapture()
            applyBubbleVisualState()
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showCaptureError(getString(R.string.helper_capture_error_speech_unavailable))
            return
        }
        val speechRecognizer = ensureRecognizer() ?: run {
            showCaptureError(getString(R.string.helper_capture_error_speech_unavailable))
            return
        }
        transcript = ""
        partialText = ""
        calcResult = null
        notePreview = null
        errorText = null
        captureStage = CaptureStage.Listening
        renderCapture()
        applyBubbleVisualState()
        val recognizeIntent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
        runCatching {
            speechRecognizer.cancel()
            speechRecognizer.startListening(recognizeIntent)
        }.onFailure {
            showCaptureError(getString(R.string.helper_capture_error_start_failed))
        }
    }

    private fun ensureRecognizer(): SpeechRecognizer? {
        recognizer?.let { return it }
        val created = runCatching { SpeechRecognizer.createSpeechRecognizer(this) }.getOrNull() ?: return null
        created.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onPartialResults(partialResults: Bundle?) {
                    if (captureStage != CaptureStage.Listening) return
                    val text =
                        partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                    if (text.isNotBlank()) {
                        partialText = text
                        renderCapture()
                    }
                }

                override fun onResults(results: Bundle?) {
                    if (captureStage != CaptureStage.Listening) return
                    val finalText =
                        results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                    processTranscript(if (finalText.isBlank()) partialText else finalText)
                }

                override fun onError(error: Int) {
                    if (captureStage != CaptureStage.Listening) return
                    if (partialText.isNotBlank()) {
                        processTranscript(partialText)
                        return
                    }
                    if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        captureStage = CaptureStage.NeedsPermission
                        renderCapture()
                        applyBubbleVisualState()
                    } else {
                        showCaptureError(getString(R.string.helper_capture_error_retry))
                    }
                }
            }
        )
        recognizer = created
        return created
    }

    private fun processTranscript(text: String) {
        val cleaned = text.trim()
        transcript = cleaned
        if (cleaned.isBlank()) {
            showCaptureError(getString(R.string.helper_capture_error_no_input))
            return
        }
        when (captureMode) {
            HelperCaptureMode.VoiceCalculator -> {
                val parsed = parseVoiceMath(cleaned)
                if (parsed == null) {
                    showCaptureError(getString(R.string.helper_capture_error_parse_failed))
                } else {
                    calcResult = parsed
                    captureStage = CaptureStage.Result
                    renderCapture()
                    applyBubbleVisualState()
                }
            }
            HelperCaptureMode.VoiceNote -> {
                notePreview = HelperNoteClassifier.classifyVoiceTranscript(cleaned)
                captureStage = CaptureStage.Result
                renderCapture()
                applyBubbleVisualState()
            }
            null -> Unit
        }
    }

    private fun showCaptureError(text: String) {
        errorText = text
        captureStage = CaptureStage.Error
        renderCapture()
        applyBubbleVisualState()
    }

    private fun saveCaptureAndClose() {
        val mode = captureMode ?: return
        val transcriptText = transcript.trim()
        if (transcriptText.isBlank()) {
            showCaptureError(getString(R.string.helper_capture_error_no_input))
            return
        }
        serviceScope.launch {
            withContext(Dispatchers.IO) {
                when (mode) {
                    HelperCaptureMode.VoiceNote -> {
                        database.helperNoteDao().insert(
                            HelperNoteClassifier.buildVoiceNote(transcript = transcriptText)
                        )
                    }
                    HelperCaptureMode.VoiceCalculator -> {
                        val result = calcResult ?: return@withContext
                        database.helperNoteDao().insert(
                            HelperNoteClassifier.buildCalculatorNote(
                                expression = transcriptText,
                                result = result.value
                            )
                        )
                    }
                }
            }
            Toast.makeText(
                this@HelperOverlayService,
                getString(R.string.helper_capture_saved),
                Toast.LENGTH_SHORT
            ).show()
            removeCapture()
        }
    }

    private fun copyResultToClipboard() {
        val resultText = calcResult?.value?.stripTrailingZeros()?.toPlainString().orEmpty()
        if (resultText.isBlank()) return
        val clipboard =
            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("helper_result", resultText))
        Toast.makeText(
            this,
            getString(R.string.notes_history_copied),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun openAppSettings() {
        val intent =
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:$packageName".toUri()
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun destroyRecognizer() {
        runCatching {
            recognizer?.cancel()
            recognizer?.destroy()
        }
        recognizer = null
    }

    private fun applyThemeToVisibleViews() {
        applyBubbleVisualState()
        (panelView as? LinearLayout)?.let { panel ->
            panel.background = createCardBackground()
            repeat(panel.childCount) { index ->
                (panel.getChildAt(index) as? Button)?.let { button ->
                    stylePrimaryButton(button)
                }
            }
        }
        if (captureView != null) {
            renderCapture()
        }
    }

    private fun applyBubbleVisualState() {
        val state =
            when (captureStage) {
                CaptureStage.Listening -> HelperBubbleVisualState.Listening
                CaptureStage.Result -> HelperBubbleVisualState.Result
                CaptureStage.Error -> HelperBubbleVisualState.Error
                CaptureStage.Idle,
                CaptureStage.NeedsPermission -> HelperBubbleVisualState.Idle
            }
        bubbleBackground?.setColor(activeTheme.bubbleColorFor(state))
        bubbleView?.alpha = if (bubblePeeked) settingsState.idlePeekAlphaPercent / 100f else 1f
    }

    private fun applyBubblePositionFromSettings() {
        val manager = windowManager ?: return
        val bubble = bubbleView ?: return
        val params = bubbleParams ?: return
        if (settingsState.hasSavedBubblePosition) {
            params.x = clampBubbleXVisible(settingsState.bubbleX)
            params.y = clampBubbleY(settingsState.bubbleY)
            bubbleDockLeft = settingsState.dockLeft
        } else {
            params.x = clampBubbleXVisible(dpToPx(bubbleDefaultXdp))
            params.y = clampBubbleY(dpToPx(bubbleDefaultYdp))
            bubbleDockLeft = params.x + (bubbleSizePx() / 2) < (screenWidthPx() / 2)
        }
        bubblePeeked = false
        bubble.alpha = 1f
        runCatching { manager.updateViewLayout(bubble, params) }
        updateFloatingPanelPositions()
    }

    private fun updateFloatingPanelPositions() {
        val manager = windowManager ?: return
        val bubble = bubbleParams ?: return
        val panel = panelView
        val panelLayoutParams = panelParams
        if (panel != null && panelLayoutParams != null) {
            panelLayoutParams.x = clampPanelX(bubble.x)
            panelLayoutParams.y = clampPanelY(bubble.y + dpToPx(panelOffsetDp))
            runCatching { manager.updateViewLayout(panel, panelLayoutParams) }
        }
        val capture = captureView
        val captureLayoutParams = captureParams
        if (capture != null && captureLayoutParams != null) {
            captureLayoutParams.x = clampPanelX(bubble.x)
            captureLayoutParams.y = clampPanelY(bubble.y + dpToPx(panelOffsetDp))
            runCatching { manager.updateViewLayout(capture, captureLayoutParams) }
        }
    }

    private fun scheduleAutoPeek() {
        cancelAutoPeek()
        if (!settingsState.smartHideEnabled) return
        if (bubbleView == null) return
        if (panelView != null || captureView != null) return
        if (captureStage == CaptureStage.Listening || captureStage == CaptureStage.Result) return
        autoPeekJob =
            serviceScope.launch {
                delay(settingsState.idlePeekSeconds.coerceIn(2, 12) * 1000L)
                peekBubble()
            }
    }

    private fun cancelAutoPeek() {
        autoPeekJob?.cancel()
        autoPeekJob = null
    }

    private fun peekBubble() {
        if (!settingsState.smartHideEnabled) return
        val manager = windowManager ?: return
        val bubble = bubbleView ?: return
        val params = bubbleParams ?: return
        if (panelView != null || captureView != null) return
        val peekVisiblePx = dpToPx(peekVisibleDp)
        params.x =
            if (bubbleDockLeft) {
                -bubbleSizePx() + peekVisiblePx
            } else {
                screenWidthPx() - peekVisiblePx
            }
        params.y = clampBubbleY(params.y)
        bubblePeeked = true
        bubble.alpha = settingsState.idlePeekAlphaPercent / 100f
        runCatching { manager.updateViewLayout(bubble, params) }
    }

    private fun revealBubble(
        savePosition: Boolean,
        schedulePeek: Boolean = true
    ) {
        val manager = windowManager ?: return
        val bubble = bubbleView ?: return
        val params = bubbleParams ?: return
        val edge = dpToPx(edgeInsetDp)
        params.x =
            if (bubbleDockLeft) {
                edge
            } else {
                max(edge, screenWidthPx() - bubbleSizePx() - edge)
            }
        params.y = clampBubbleY(params.y)
        bubblePeeked = false
        bubble.alpha = 1f
        runCatching { manager.updateViewLayout(bubble, params) }
        updateFloatingPanelPositions()
        if (savePosition) {
            persistBubblePosition()
        }
        if (schedulePeek) {
            scheduleAutoPeek()
        }
    }

    private fun snapBubbleToEdge(savePosition: Boolean) {
        val params = bubbleParams ?: return
        bubbleDockLeft = params.x + (bubbleSizePx() / 2) < (screenWidthPx() / 2)
        revealBubble(savePosition = savePosition)
    }

    private fun persistBubblePosition() {
        val params = bubbleParams ?: return
        serviceScope.launch(Dispatchers.IO) {
            helperPreferences.saveBubblePosition(params.x, params.y, bubbleDockLeft)
        }
    }

    private fun bubbleSizePx(): Int = dpToPx(bubbleSizeDp)

    private fun screenWidthPx(): Int = resources.displayMetrics.widthPixels

    private fun screenHeightPx(): Int = resources.displayMetrics.heightPixels

    private fun resolveInitialBubbleX(): Int {
        return if (settingsState.hasSavedBubblePosition) {
            clampBubbleXVisible(settingsState.bubbleX)
        } else {
            clampBubbleXVisible(dpToPx(bubbleDefaultXdp))
        }
    }

    private fun resolveInitialBubbleY(): Int {
        return if (settingsState.hasSavedBubblePosition) {
            clampBubbleY(settingsState.bubbleY)
        } else {
            clampBubbleY(dpToPx(bubbleDefaultYdp))
        }
    }

    private fun clampBubbleY(y: Int): Int {
        val top = dpToPx(40)
        val bottom = max(top, screenHeightPx() - bubbleSizePx() - dpToPx(100))
        return y.coerceIn(top, bottom)
    }

    private fun clampBubbleXVisible(x: Int): Int {
        val edge = dpToPx(edgeInsetDp)
        val maxX = max(edge, screenWidthPx() - bubbleSizePx() - edge)
        return x.coerceIn(edge, maxX)
    }

    private fun clampPanelX(x: Int): Int {
        val inset = dpToPx(edgeInsetDp)
        val maxX = max(inset, screenWidthPx() - dpToPx(captureCardWidthDp) - inset)
        return x.coerceIn(inset, maxX)
    }

    private fun clampPanelY(y: Int): Int {
        val inset = dpToPx(24)
        val maxY = max(inset, screenHeightPx() - dpToPx(220))
        return y.coerceIn(inset, maxY)
    }

    private fun overlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private inner class BubbleTouchListener(
        private val params: WindowManager.LayoutParams
    ) : View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var touchStartX = 0f
        private var touchStartY = 0f
        private var downAt = 0L

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val manager = windowManager ?: return false
            return when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    cancelAutoPeek()
                    if (bubblePeeked) {
                        revealBubble(savePosition = false, schedulePeek = false)
                    }
                    startX = params.x
                    startY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    downAt = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - touchStartX).toInt()
                    val deltaY = (event.rawY - touchStartY).toInt()
                    params.x = clampBubbleXVisible(startX + deltaX)
                    params.y = clampBubbleY(startY + deltaY)
                    bubblePeeked = false
                    runCatching { manager.updateViewLayout(v, params) }
                    updateFloatingPanelPositions()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val duration = System.currentTimeMillis() - downAt
                    val deltaX = abs(event.rawX - touchStartX)
                    val deltaY = abs(event.rawY - touchStartY)
                    val moved = deltaX >= dpToPx(8) || deltaY >= dpToPx(8)
                    if (moved) {
                        snapBubbleToEdge(savePosition = true)
                    } else if (duration < 300) {
                        v.performClick()
                    }
                    scheduleAutoPeek()
                    true
                }
                else -> false
            }
        }
    }

    private enum class CaptureStage {
        Idle,
        NeedsPermission,
        Listening,
        Result,
        Error
    }
}
