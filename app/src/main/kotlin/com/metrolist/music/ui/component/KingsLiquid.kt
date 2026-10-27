/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Liquid glass touch for the whole app (like the liquid bottom bar):
 *  - kingsLiquidPress: touch a song, card, setting or the search bar and it squishes like a
 *    water drop and bounces back.
 *  - Liquid glass lens (song lists, search, settings, 3-dot menus):
 *      press and hold for a moment, then move your finger: a clear glass lens appears and
 *      follows your finger, magnifying what is under it, stretching like liquid.
 *      Lift your finger: the lens drops on that item and it opens (song plays, setting opens).
 *    A quick tap, a normal scroll and a long press still work exactly as before.
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
import androidx.compose.runtime.mutableStateOf
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
import kotlin.math.abs

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

/** Taps the screen at a point (in window coordinates), after the current touch is finished. */
private var kingsIgnoreUntil = 0L

private fun tapAt(view: View, windowPos: Offset) {
    view.post {
        val root = view.rootView
        val t = SystemClock.uptimeMillis()
        kingsIgnoreUntil = t + 300
        val down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, windowPos.x, windowPos.y, 0)
        val up = MotionEvent.obtain(t, t + 30, MotionEvent.ACTION_UP, windowPos.x, windowPos.y, 0)
        down.source = InputDevice.SOURCE_TOUCHSCREEN
        up.source = InputDevice.SOURCE_TOUCHSCREEN
        root.dispatchTouchEvent(down)
        root.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
    }
}

@Composable
private fun Modifier.kingsLens(registry: KingsBubbleState?, rowHeight: Dp, corner: Dp): Modifier {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val accent = MaterialTheme.colorScheme.primary
    val left = remember { Animatable(0f) }
    val top = remember { Animatable(0f) }
    val right = remember { Animatable(0f) }
    val bottom = remember { Animatable(0f) }
    val shown = remember { Animatable(0f) } // 0 hidden, 1 lens
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

    /** Where the lens should be for a finger position. */
    fun targetFor(pos: Offset, width: Float): Rect {
        itemAt(pos)?.let { return it }
        val half = rowHeightPx / 2f
        val inset = insetPx
        return Rect(inset, pos.y - half, width - inset, pos.y + half)
    }

    fun moveTo(r: Rect, snap: Boolean) {
        if (snap) {
            scope.launch { left.snapTo(r.left) }
            scope.launch { top.snapTo(r.top) }
            scope.launch { right.snapTo(r.right) }
            scope.launch { bottom.snapTo(r.bottom) }
            return
        }
        // the edge in front moves fast, the edge behind follows slowly = liquid stretch
        val fast = spring<Float>(dampingRatio = 0.72f, stiffness = 1500f)
        val slow = spring<Float>(dampingRatio = 0.62f, stiffness = 420f)
        val down = r.center.y > (top.value + bottom.value) / 2f
        val rightward = r.center.x > (left.value + right.value) / 2f
        scope.launch { top.animateTo(r.top, if (down) slow else fast) }
        scope.launch { bottom.animateTo(r.bottom, if (down) fast else slow) }
        scope.launch { left.animateTo(r.left, if (rightward) slow else fast) }
        scope.launch { right.animateTo(r.right, if (rightward) fast else slow) }
    }

    return this
        .onGloballyPositioned { host = it }
        .pointerInput(registry) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (SystemClock.uptimeMillis() < kingsIgnoreUntil) return@awaitEachGesture // our own tap
                val slop = viewConfiguration.touchSlop
                val longPress = viewConfiguration.longPressTimeoutMillis

                // 1) quick moves = normal scrolling, quick lift = normal tap
                val early = withTimeoutOrNull(200L) {
                    var outcome = ""
                    while (outcome.isEmpty()) {
                        val e = awaitPointerEvent(PointerEventPass.Final)
                        val c = e.changes.firstOrNull { it.id == down.id }
                        if (c == null || !c.pressed) outcome = "up"
                        else if (c.isConsumed || (c.position - down.position).getDistance() > slop) outcome = "moved"
                    }
                    outcome
                }
                if (early != null) return@awaitEachGesture

                // 2) held still a moment: now moving the finger brings the lens
                //    (holding still longer = normal long press)
                var pos = down.position
                val armed = withTimeoutOrNull((longPress - 230L).coerceAtLeast(60L)) {
                    var outcome = ""
                    while (outcome.isEmpty()) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        val c = e.changes.firstOrNull { it.id == down.id }
                        if (c == null || !c.pressed) {
                            outcome = "up"
                        } else if ((c.position - down.position).getDistance() > slop / 2f) {
                            e.changes.forEach { it.consume() } // no scrolling, no long press
                            pos = c.position
                            outcome = "lens"
                        }
                    }
                    outcome
                }
                if (armed != "lens") return@awaitEachGesture

                // 3) lens mode
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val width = size.width.toFloat()
                val start = targetFor(down.position, width)
                moveTo(Rect(start.center, 0f).inflate(rowHeight.toPx() / 3f), snap = true) // grows from the finger
                moveTo(targetFor(pos, width), snap = false)
                scope.launch { shown.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 520f)) }
                var lastItem = itemAt(pos)
                while (true) {
                    val e = awaitPointerEvent(PointerEventPass.Initial)
                    val c = e.changes.firstOrNull { it.id == down.id } ?: break
                    e.changes.forEach { it.consume() }
                    if (!c.pressed) break
                    pos = c.position
                    val item = itemAt(pos)
                    if (item != null && item != lastItem) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    lastItem = item
                    moveTo(targetFor(pos, width), snap = false)
                }

                // 4) lift: the lens drops onto the item and opens it
                // tap the middle of the row (not the 3-dot button at the end)
                val target = itemAt(pos)?.center ?: Offset(size.width * 0.4f, pos.y)
                scope.launch { shown.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 380f)) }
                val h = host
                if (h != null && h.isAttached && pos.y >= 0f && pos.y <= size.height) {
                    tapAt(view, h.localToWindow(target))
                }
            }
        }
        .drawWithContent {
            drawContent()
            val s = shown.value
            if (s < 0.01f) return@drawWithContent
            val a = s.coerceIn(0f, 1f)
            // the lens grows slightly bigger than the row, like a drop of glass on top
            val grow = 4.dp.toPx() * s
            val rect = Rect(left.value - grow, top.value - grow, right.value + grow, bottom.value + grow)
            if (rect.width <= 1f || rect.height <= 1f) return@drawWithContent
            val r = (corner.toPx() + grow).coerceAtMost(rect.height / 2f)
            val shape = Path().apply { addRoundRect(RoundRect(rect, CornerRadius(r, r))) }

            // soft shadow under the lens
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.18f * a),
                topLeft = Offset(rect.left, rect.top + 3.dp.toPx()),
                size = rect.size,
                cornerRadius = CornerRadius(r, r),
            )
            // magnified content inside the lens
            clipPath(shape) {
                val zoom = 1f + 0.10f * a
                scale(zoom, zoom, pivot = rect.center) {
                    this@drawWithContent.drawContent()
                }
                // clear glass tint + light from the top
                drawRect(color = Color.White.copy(alpha = 0.05f * a), topLeft = rect.topLeft, size = rect.size)
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.20f * a), Color.Transparent),
                        startY = rect.top,
                        endY = rect.top + rect.height * 0.5f,
                    ),
                    topLeft = rect.topLeft,
                    size = rect.size,
                )
            }
            // rainbow edges like real glass (red on one side, blue on the other)
            val stroke = Stroke(width = 1.6.dp.toPx())
            translate(left = -1.2.dp.toPx()) {
                drawRoundRect(Color(0xFFFF4D6D).copy(alpha = 0.35f * a), rect.topLeft, rect.size, CornerRadius(r, r), style = stroke)
            }
            translate(left = 1.2.dp.toPx()) {
                drawRoundRect(Color(0xFF3DD6FF).copy(alpha = 0.35f * a), rect.topLeft, rect.size, CornerRadius(r, r), style = stroke)
            }
            // bright glass rim
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.85f * a), accent.copy(alpha = 0.35f * a), Color.White.copy(alpha = 0.45f * a)),
                    startY = rect.top,
                    endY = rect.bottom,
                ),
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = CornerRadius(r, r),
                style = stroke,
            )
        }
}
