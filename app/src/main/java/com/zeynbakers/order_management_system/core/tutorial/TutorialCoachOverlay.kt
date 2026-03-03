package com.zeynbakers.order_management_system.core.tutorial

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
internal fun TutorialCoachOverlay(
    targetBounds: Rect?,
    progressText: String,
    title: String,
    body: String,
    backLabel: String,
    skipLabel: String,
    nextLabel: String,
    finishLabel: String,
    primaryActionLabel: String?,
    showBack: Boolean,
    isLastStep: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onPrimaryAction: (() -> Unit)?
) {
    val pulseTransition = rememberInfiniteTransition(label = "tutorialCoachPulse")
    val pulseProgress by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1500), repeatMode = RepeatMode.Restart),
        label = "tutorialCoachPulseProgress"
    )
    var cardBounds by remember { mutableStateOf<Rect?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenHeightPx = remember(maxHeight, density) { with(density) { maxHeight.toPx() } }
        val placeCardTop = remember(targetBounds, screenHeightPx) {
            targetBounds?.center?.y?.let { centerY -> centerY > (screenHeightPx * 0.56f) } ?: false
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            ) {
                drawRect(color = Color.Black.copy(alpha = 0.64f))

                targetBounds?.let { target ->
                    val padding = 12.dp.toPx()
                    val left = (target.left - padding).coerceAtLeast(0f)
                    val top = (target.top - padding).coerceAtLeast(0f)
                    val width = (target.width + (padding * 2)).coerceAtMost(size.width - left)
                    val height = (target.height + (padding * 2)).coerceAtMost(size.height - top)
                    val corner = 18.dp.toPx()

                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(corner, corner),
                        blendMode = BlendMode.Clear
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.92f),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(corner, corner),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    val pulseInset = 14.dp.toPx() * pulseProgress
                    val pulseAlpha = 0.5f * (1f - pulseProgress)
                    drawRoundRect(
                        color = Color.White.copy(alpha = pulseAlpha),
                        topLeft = Offset((left - pulseInset).coerceAtLeast(0f), (top - pulseInset).coerceAtLeast(0f)),
                        size = Size(
                            (width + (pulseInset * 2)).coerceAtMost(size.width - (left - pulseInset).coerceAtLeast(0f)),
                            (height + (pulseInset * 2)).coerceAtMost(size.height - (top - pulseInset).coerceAtLeast(0f))
                        ),
                        cornerRadius = CornerRadius(corner + pulseInset, corner + pulseInset),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                if (targetBounds != null && cardBounds != null) {
                    val card = cardBounds!!
                    val start =
                        if (card.center.y <= targetBounds.center.y) {
                            Offset(card.center.x, card.bottom)
                        } else {
                            Offset(card.center.x, card.top)
                        }
                    val end =
                        if (card.center.y <= targetBounds.center.y) {
                            Offset(targetBounds.center.x, targetBounds.top)
                        } else {
                            Offset(targetBounds.center.x, targetBounds.bottom)
                        }
                    drawLine(
                        color = Color.White.copy(alpha = 0.9f),
                        start = start,
                        end = end,
                        strokeWidth = 2.dp.toPx()
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.95f),
                        radius = 4.dp.toPx(),
                        center = end
                    )
                }
            }

            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 16.dp)
                        .align(if (placeCardTop) Alignment.TopCenter else Alignment.BottomCenter)
                        .onGloballyPositioned { coordinates ->
                            cardBounds = coordinates.boundsInRoot()
                        },
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (onPrimaryAction != null && primaryActionLabel != null) {
                        Button(
                            onClick = onPrimaryAction,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(primaryActionLabel)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showBack) {
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(backLabel)
                            }
                        }
                        OutlinedButton(
                            onClick = onSkip,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(skipLabel)
                        }
                        Button(
                            onClick = onNext,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isLastStep) finishLabel else nextLabel)
                        }
                    }
                }
            }
        }
    }
}

