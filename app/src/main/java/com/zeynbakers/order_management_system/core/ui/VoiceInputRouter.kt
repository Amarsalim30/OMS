package com.zeynbakers.order_management_system.core.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.zeynbakers.order_management_system.core.util.VoiceNotesParseResult
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.core.util.parseVoiceMath
import com.zeynbakers.order_management_system.core.util.parseVoiceNotes
import java.math.BigDecimal

enum class VoiceTarget {
    Notes,
    Total
}

enum class VoiceNotesMode {
    Append,
    Replace
}

data class VoicePreparedResult(
    val target: VoiceTarget,
    val transcript: String,
    val previewLabel: String,
    val previewValue: String,
    val amount: BigDecimal? = null,
    val notesText: String? = null,
    val errorMessage: String? = null
) {
    val canApply: Boolean
        get() = errorMessage == null
}

class VoiceInputRouter(
    private val onApplyTotal: (BigDecimal) -> Unit
) {
    private data class NotesHandler(
        val getNotes: () -> String,
        val setNotes: (String) -> Unit
    )

    private var notesHandler: NotesHandler? by mutableStateOf(null)

    var target: VoiceTarget by mutableStateOf(VoiceTarget.Total)
        private set

    var notesMode: VoiceNotesMode by mutableStateOf(VoiceNotesMode.Append)
        private set

    var followsFocus: Boolean by mutableStateOf(true)
        private set

    val hasNotesTarget: Boolean
        get() = notesHandler != null

    fun registerNotesTarget(getNotes: () -> String, setNotes: (String) -> Unit) {
        notesHandler = NotesHandler(getNotes = getNotes, setNotes = setNotes)
        ensureTargetAvailable()
    }

    fun clearNotesTarget() {
        notesHandler = null
        ensureTargetAvailable()
    }

    fun onFocusTarget(target: VoiceTarget) {
        if (followsFocus) {
            this.target = target
        }
    }

    fun setManualTarget(target: VoiceTarget) {
        this.target = target
        followsFocus = false
    }

    fun enableFollowFocus() {
        followsFocus = true
    }

    fun updateNotesMode(mode: VoiceNotesMode) {
        notesMode = mode
    }

    fun prepare(transcript: String): VoicePreparedResult {
        val trimmed = transcript.trim()
        if (trimmed.isBlank()) {
            return VoicePreparedResult(
                target = target,
                transcript = transcript,
                previewLabel = "",
                previewValue = "",
                errorMessage = "Didn't catch that"
            )
        }

        return when (target) {
            VoiceTarget.Total -> prepareTotal(trimmed)
            VoiceTarget.Notes -> prepareNotes(trimmed)
        }
    }

    fun apply(prepared: VoicePreparedResult): String? {
        if (!prepared.canApply) return null
        return when (prepared.target) {
            VoiceTarget.Total -> {
                val amount = prepared.amount ?: return null
                onApplyTotal(amount)
                "Applied to Total"
            }
            VoiceTarget.Notes -> {
                val handler = notesHandler ?: return null
                val notesText = prepared.notesText ?: return null
                val existing = handler.getNotes()
                val updated =
                    if (notesMode == VoiceNotesMode.Append) {
                        appendNotes(existing, notesText)
                    } else {
                        notesText
                    }
                handler.setNotes(updated)
                "Applied to Notes"
            }
        }
    }

    private fun prepareTotal(trimmed: String): VoicePreparedResult {
        val parsed = parseVoiceMath(trimmed)
        return if (parsed == null) {
            VoicePreparedResult(
                target = VoiceTarget.Total,
                transcript = trimmed,
                previewLabel = "Total",
                previewValue = "--",
                errorMessage = "Couldn't parse"
            )
        } else {
            VoicePreparedResult(
                target = VoiceTarget.Total,
                transcript = trimmed,
                previewLabel = "Total",
                previewValue = formatKes(parsed.value),
                amount = parsed.value
            )
        }
    }

    private fun prepareNotes(trimmed: String): VoicePreparedResult {
        val handler = notesHandler
        if (handler == null) {
            return VoicePreparedResult(
                target = VoiceTarget.Notes,
                transcript = trimmed,
                previewLabel = "Notes",
                previewValue = "--",
                errorMessage = "Notes field unavailable"
            )
        }
        val parsed: VoiceNotesParseResult? = parseVoiceNotes(trimmed)
        val notesText =
            if (parsed != null && parsed.isStructured) {
                parsed.formatted
            } else {
                trimmed
            }
        return VoicePreparedResult(
            target = VoiceTarget.Notes,
            transcript = trimmed,
            previewLabel = "Notes",
            previewValue = notesText,
            notesText = notesText
        )
    }

    private fun appendNotes(existing: String, addition: String): String {
        val base = existing.trim()
        val extra = addition.trim()
        if (extra.isBlank()) return base
        if (base.isBlank()) return extra
        val separator =
            if (base.endsWith(",") || base.endsWith("\n")) {
                " "
            } else {
                ", "
            }
        return base + separator + extra
    }

    private fun ensureTargetAvailable() {
        if (target == VoiceTarget.Notes && notesHandler == null) {
            target = VoiceTarget.Total
            followsFocus = true
        }
    }
}

val LocalVoiceInputRouter = staticCompositionLocalOf {
    // Isolated screen tests and previews should degrade gracefully without a voice provider.
    VoiceInputRouter(onApplyTotal = {})
}
