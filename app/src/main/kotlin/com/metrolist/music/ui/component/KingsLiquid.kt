/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Liquid glass touch for the whole app (like the liquid bottom bar):
 *  - kingsLiquidPress: touch a song, card, setting or the search bar and it squishes like a
 *    water drop and bounces back.
 *  - Liquid glass (song lists, search, settings, 3-dot menus): touch and a clear glass appears
 *    under your finger. Move your finger up or down and the glass moves with it (the list scrolls
 *    as normal). Lift your finger and it melts away. Taps and long presses work as normal.
 */

package com.metrolist.music.ui.component

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.exp

// ---------------------------------------------------------------- liquid press

/**
 * Water-drop touch: squish, lean toward the finger, soft light under the finger, bounce back.
 * Watches touches only (never takes them), so clicks and scrolling are not changed.
 */
@Composable
fun Modifier.kingsLiquidPress(corner: Dp = 18.dp): Modifier {
    val press = remember { Animatable(0f) }
    var touch by remember { mutableStateOf(Offset.Unspecified) }
    val scope = rememberCoroutineScope()
    val light = MaterialTheme.colorScheme.onSurface
    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                touch = down.position
                scope.launch { press.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 650f)) }
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    // the list started scrolling (or something else took the touch): let go softly
                    if (change.isConsumed && change.positionChange() != Offset.Zero) break
                    touch = change.position
                }
                // low damping = it overshoots a little, like a drop settling
                scope.launch { press.animateTo(0f, spring(dampingRatio = 0.36f, stiffness = 360f)) }
            }
        }
        .graphicsLayer {
            val p = press.value
            val squish = 0.035f * p
            scaleX = 1f - squish * 0.8f
            scaleY = 1f - squish
            if (touch.isSpecified && size.width > 0f && size.height > 0f) {
                val pp = p.coerceIn(0f, 1f)
                translationX = ((touch.x / size.width) - 0.5f) * 8.dp.toPx() * pp
                translationY = ((touch.y / size.height) - 0.5f) * 4.dp.toPx() * pp
            }
        }
        .drawWithContent {
            drawContent()
            val p = press.value.coerceIn(0f, 1f)
            if (p > 0.01f && touch.isSpecified) {
                val r = corner.toPx()
                drawRoundRect(
                    color = light.copy(alpha = 0.05f * p),
                    cornerRadius = CornerRadius(r, r),
                )
                val radius = size.maxDimension * 0.55f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(light.copy(alpha = 0.14f * p), Color.Transparent),
                        center = touch,
                        radius = radius,
                    ),
                    radius = radius,
                    center = touch,
                )
            }
        }
}

// ---------------------------------------------------------------- liquid glass lens

/** Items of one group (settings group, menu) so the lens can snap onto them. */
@Stable
class KingsBubbleState {
    internal val items = mutableMapOf<Any, LayoutCoordinates>()
}

@Composable
fun rememberKingsBubble(): KingsBubbleState = remember { KingsBubbleState() }

/** Marks one item of a group (the lens snaps to its shape). */
@Suppress("UNUSED_PARAMETER")
fun Modifier.kingsBubbleItem(
    state: KingsBubbleState?,
    key: Any,
    enabled: Boolean = true,
    onSelect: () -> Unit = {},
): Modifier {
    if (state == null) return this
    return this.onGloballyPositioned { coords ->
        if (enabled) state.items[key] = coords else state.items.remove(key)
    }
}

/** Settings groups and 3-dot menus: liquid glass lens that snaps onto the item under the finger. */
@Composable
fun Modifier.kingsBubbleHost(state: KingsBubbleState, corner: Dp = 20.dp): Modifier =
    kingsLens(state, 64.dp, corner)

/** Song lists, playlists, search results: liquid glass lens under the finger (one song row tall). */
@Composable
fun Modifier.kingsLiquidFollow(rowHeight: Dp = 64.dp, corner: Dp = 20.dp): Modifier =
    kingsLens(null, rowHeight, corner)

/** Moving glass: current edges (drawn) and where they are going (targets). */
private class LensState {
    var l by mutableFloatStateOf(0f)
    var t by mutableFloatStateOf(0f)
    var r by mutableFloatStateOf(0f)
    var b by mutableFloatStateOf(0f)
    var tl = 0f
    var tt = 0f
    var tr = 0f
    var tb = 0f
    var running = false
}

@Composable
private fun Modifier.kingsLens(registry: KingsBubbleState?, rowHeight: Dp, corner: Dp): Modifier {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val accent = MaterialTheme.colorScheme.primary
    val ls = remember { LensState() }
    val shown = remember { Animatable(0f) }
    var host by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val density = LocalDensity.current
    val rowHeightPx = with(density) { rowHeight.toPx() }
    val insetPx = with(density) { 8.dp.toPx() }

    fun itemAt(pos: Offset): Rect? {
        val h = host ?: return null
        if (registry == null || !h.isAttached) return null
        for (coords in registry.items.values) {
            if (!coords.isAttached) continue
            val rect = runCatching { h.localBoundingBoxOf(coords, clipBounds = false) }.getOrNull() ?: continue
            if (rect.contains(pos)) return rect
        }
        return null
    }

    /** Where the glass should be for a finger position: on the item under it, or one row tall at the finger. */
    fun targetFor(pos: Offset, width: Float): Rect {
        itemAt(pos)?.let { return it }
        val half = rowHeightPx / 2f
        return Rect(insetPx, pos.y - half, width - insetPx, pos.y + half)
    }

    fun setTarget(r: Rect) {
        ls.tl = r.left
        ls.tt = r.top
        ls.tr = r.right
        ls.tb = r.bottom
    }

    /** One smooth loop per touch (every screen frame): the edge in front moves fast, the edge behind follows = liquid. */
    fun startLoop() {
        if (ls.running) return
        ls.running = true
        scope.launch {
            var last = 0L
            while (ls.running) {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0.016f else ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    last = now
                    fun step(cur: Float, target: Float, fast: Boolean) =
                        cur + (target - cur) * (1f - exp(-(if (fast) 30f else 12f) * dt))
                    val goingDown = ls.tb > ls.b
                    ls.t = step(ls.t, ls.tt, fast = !goingDown)
                    ls.b = step(ls.b, ls.tb, fast = goingDown)
                    ls.l = step(ls.l, ls.tl, fast = true)
                    ls.r = step(ls.r, ls.tr, fast = true)
                }
            }
        }
    }

    return this
        .onGloballyPositioned { host = it }
        .pointerInput(registry) {
            awaitEachGesture {
                // Only watches the finger: taps, long presses and scrolling work as normal.
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val width = size.width.toFloat()
                val first = targetFor(down.position, width)
                // the glass grows out of the finger
                val c0 = first.center
                ls.l = c0.x - 24f; ls.r = c0.x + 24f; ls.t = c0.y - 24f; ls.b = c0.y + 24f
                setTarget(first)
                startLoop()
                scope.launch { shown.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 650f)) }
                var lastItem = itemAt(down.position)
                while (true) {
                    val e = awaitPointerEvent(PointerEventPass.Final)
                    val c = e.changes.firstOrNull { it.id == down.id } ?: break
                    if (!c.pressed) break
                    // finger up = glass up, finger down = glass down (the list scrolls with it)
                    setTarget(targetFor(c.position, width))
                    val item = itemAt(c.position)
                    if (item != null && item != lastItem) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastItem = item
                }
                scope.launch {
                    shown.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 420f))
                    if (shown.value < 0.02f) ls.running = false
                }
            }
        }
        .drawWithContent {
            drawContent()
            val s = shown.value.coerceIn(0f, 1.2f)
            if (s < 0.01f) return@drawWithContent
            val a = s.coerceAtMost(1f)
            val grow = 3.dp.toPx() * a
            val rect = Rect(ls.l - grow, ls.t - grow, ls.r + grow, ls.b + grow)
            if (rect.width <= 1f || rect.height <= 1f) return@drawWithContent
            val r = (corner.toPx() + grow).coerceAtMost(rect.height / 2f)
            val cr = CornerRadius(r, r)
            // clear glass body (no copy of the text inside, so nothing shows twice)
            drawRoundRect(color = Color.White.copy(alpha = 0.07f * a), topLeft = rect.topLeft, size = rect.size, cornerRadius = cr)
            // light from the top
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.16f * a), Color.Transparent),
                    startY = rect.top,
                    endY = rect.top + rect.height * 0.55f,
                ),
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = cr,
            )
            // rainbow edges like real glass
            val stroke = Stroke(width = 1.5.dp.toPx())
            translate(left = -1.dp.toPx()) {
                drawRoundRect(Color(0xFFFF4D6D).copy(alpha = 0.30f * a), rect.topLeft, rect.size, cr, style = stroke)
            }
            translate(left = 1.dp.toPx()) {
                drawRoundRect(Color(0xFF3DD6FF).copy(alpha = 0.30f * a), rect.topLeft, rect.size, cr, style = stroke)
            }
            // bright glass rim
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.80f * a), accent.copy(alpha = 0.30f * a), Color.White.copy(alpha = 0.40f * a)),
                    startY = rect.top,
                    endY = rect.bottom,
                ),
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = cr,
                style = stroke,
            )
        }
}
