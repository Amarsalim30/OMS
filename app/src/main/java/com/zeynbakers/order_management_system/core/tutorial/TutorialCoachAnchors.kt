package com.zeynbakers.order_management_system.core.tutorial

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

@Stable
class TutorialCoachAnchorRegistry {
    private val bounds = mutableStateMapOf<String, Rect>()

    fun update(anchorId: String, rect: Rect) {
        bounds[anchorId] = rect
    }

    fun remove(anchorId: String) {
        bounds.remove(anchorId)
    }

    fun boundsFor(anchorId: String): Rect? = bounds[anchorId]
}

val LocalTutorialCoachAnchorRegistry = staticCompositionLocalOf<TutorialCoachAnchorRegistry?> { null }

fun Modifier.tutorialCoachTarget(anchorId: String): Modifier =
    composed {
        val registry = LocalTutorialCoachAnchorRegistry.current
        DisposableEffect(anchorId, registry) {
            onDispose {
                registry?.remove(anchorId)
            }
        }
        onGloballyPositioned { coordinates ->
            registry?.update(anchorId, coordinates.boundsInRoot())
        }
    }
