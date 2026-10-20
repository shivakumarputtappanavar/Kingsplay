/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Liquid glass switch (iPhone style) used everywhere in Settings.
 * Press: the knob grows into a clear glass drop. Slide or tap: it moves like liquid,
 * then turns back into a solid knob. Works like the normal Switch (same options).
 * When "Liquid glass" is off in Settings, the normal Material switch is shown.
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.theme.LiquidGlassKey
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TrackWidth = 52.dp
private val TrackHeight = 32.dp
private val KnobRestWidth = 30.dp
private val KnobRestHeight = 26.dp
private val KnobLiftWidth = 40.dp
private val KnobLiftHeight = 34.dp
private val TrackPadding = 3.dp

@Composable
fun KingsLiquidSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val liquid = rememberPreference(LiquidGlassKey, true).value
    if (!liquid) {
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            thumbContent = thumbContent,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
        )
        return
    }

    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val currentChecked by rememberUpdatedState(checked)
    val currentOnChange by rememberUpdatedState(onCheckedChange)

    val position = remember { Animatable(if (checked) 1f else 0f) }
    var dragging by remember { mutableStateOf(false) }
    val lift by animateFloatAsState(
        targetValue = if (pressed || dragging) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "kpSwitchLift",
    )

    LaunchedEffect(checked) {
        if (!dragging) {
            position.animateTo(if (checked) 1f else 0f, spring(dampingRatio = 0.62f, stiffness = 420f))
        }
    }

    val travelPx = with(density) { (TrackWidth - TrackPadding * 2 - KnobRestWidth).toPx() }
    val p = position.value
    val trackColor = lerp(
        if (enabled) colors.uncheckedTrackColor else colors.disabledUncheckedTrackColor,
        if (enabled) colors.checkedTrackColor else colors.disabledCheckedTrackColor,
        p.coerceIn(0f, 1f),
    )

    val interactive = if (onCheckedChange != null) {
        Modifier
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interaction,
                indication = null,
                onValueChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(it)
                },
            )
            .pointerInput(enabled, travelPx) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true },
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        scope.launch { position.snapTo((position.value + dx / travelPx).coerceIn(0f, 1f)) }
                    },
                    onDragEnd = {
                        val wanted = position.value > 0.5f
                        dragging = false
                        if (wanted != currentChecked) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentOnChange?.invoke(wanted)
                        }
                        scope.launch {
                            position.animateTo(if (wanted) 1f else 0f, spring(dampingRatio = 0.62f, stiffness = 420f))
                            // If the setting did not change, slide back
                            delay(150)
                            if (currentChecked != wanted) {
                                position.animateTo(if (currentChecked) 1f else 0f, spring(dampingRatio = 0.62f, stiffness = 420f))
                            }
                        }
                    },
                    onDragCancel = {
                        dragging = false
                        scope.launch {
                            position.animateTo(if (currentChecked) 1f else 0f, spring(dampingRatio = 0.62f, stiffness = 420f))
                        }
                    },
                )
            }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(TrackWidth, TrackHeight)
            .then(interactive)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Track
        Canvas(modifier = Modifier.size(TrackWidth, TrackHeight)) {
            drawRoundRect(color = trackColor, cornerRadius = CornerRadius(size.height / 2f))
        }

        // Knob: solid white at rest, clear glass drop while pressed / sliding
        val l = lift.coerceIn(0f, 1.2f)
        val knobW = KnobRestWidth + (KnobLiftWidth - KnobRestWidth) * l
        val knobH = KnobRestHeight + (KnobLiftHeight - KnobRestHeight) * l
        val centerX = with(density) { (TrackPadding + KnobRestWidth / 2).toPx() } + p * travelPx
        val trackHpx = with(density) { TrackHeight.toPx() }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (centerX - knobW.toPx() / 2f).roundToInt(),
                        ((trackHpx - knobH.toPx()) / 2f).roundToInt(),
                    )
                }
                .size(knobW, knobH),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(knobW, knobH)) {
                val r = CornerRadius(size.height / 2f)
                val glass = l.coerceIn(0f, 1f)
                // soft shadow (only when solid)
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.18f * (1f - glass)),
                    topLeft = Offset(0f, 1.dp.toPx()),
                    size = size,
                    cornerRadius = r,
                )
                // body: white -> clear glass
                drawRoundRect(color = Color.White.copy(alpha = 1f - 0.82f * glass), cornerRadius = r)
                if (glass > 0.01f) {
                    // top shine
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.45f * glass), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.6f,
                        ),
                        cornerRadius = r,
                    )
                    // rainbow glass edge
                    drawRoundRect(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFF7FE7FF), Color(0xFFFF8AD8), Color(0xFFFFE27A),
                                Color(0xFF8CFFB0), Color(0xFF7FE7FF),
                            ).map { it.copy(alpha = 0.55f * glass) },
                            center = Offset(size.width / 2f, size.height / 2f),
                        ),
                        cornerRadius = r,
                        style = Stroke(width = 1.6.dp.toPx()),
                    )
                    // bright rim
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.7f * glass),
                        topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                        size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                        cornerRadius = CornerRadius(size.height / 2f - 1.dp.toPx()),
                        style = Stroke(width = 0.8.dp.toPx()),
                    )
                }
            }
            if (thumbContent != null) {
                Box(modifier = Modifier.graphicsLayer { alpha = 1f - l.coerceIn(0f, 1f) }) {
                    CompositionLocalProvider(
                        LocalContentColor provides if (checked) colors.checkedTrackColor else Color(0xFF5F5C66)
                    ) {
                        thumbContent()
                    }
                }
            }
        }
    }
}
