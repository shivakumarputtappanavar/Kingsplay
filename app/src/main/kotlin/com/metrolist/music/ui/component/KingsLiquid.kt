/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Liquid touch for the whole app:
 *  - kingsLiquidPress: touch a song, card, setting or the search bar and it squishes like a
 *    water drop, leans toward your finger with a soft light under it, then bounces back.
 *    It only watches touches, so taps, long presses and scrolling work exactly as before.
 *  - kingsLiquidFollow (song lists, search results): a glass drop under your finger that flows
 *    with it while you move or scroll.
 *  - Liquid bubble (3-dot menus and settings): it follows your finger; press and hold, then slide.
 *    A glass bubble flows from item to item (stretching as it moves). Let go to open that item.
 */

package com.metrolist.music.ui.component

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

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

// ---------------------------------------------------------------- liquid bubble

/** Shared state of one group of items (one menu group or one settings group). */
@Stable
class KingsBubbleState {
    internal val items = mutableMapOf<Any, Pair<LayoutCoordinates, () -> Unit>>()
    internal var host: LayoutCoordinates? = null
    internal val left = Animatable(0f)
    internal val top = Animatable(0f)
    internal val right = Animatable(0f)
    internal val bottom = Animatable(0f)
    internal val shown = Animatable(0f)
    internal var currentKey: Any? by mutableStateOf(null)
}

@Composable
fun rememberKingsBubble(): KingsBubbleState = remember { KingsBubbleState() }

/** Marks one item of a group. [onSelect] runs when you let go on it in bubble mode. */
fun Modifier.kingsBubbleItem(
    state: KingsBubbleState?,
    key: Any,
    enabled: Boolean = true,
    onSelect: () -> Unit,
): Modifier {
    if (state == null) return this
    return this.onGloballyPositioned { coords ->
        if (enabled) state.items[key] = coords to onSelect else state.items.remove(key)
    }
}

/**
 * Put on the box that holds the items. Press and hold (about a quarter second) on an item,
 * then slide: the glass bubble follows. Let go to open the item under your finger.
 */
@Composable
fun Modifier.kingsBubbleHost(state: KingsBubbleState, corner: Dp = 20.dp): Modifier {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val glass = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary

    fun itemAt(pos: Offset): Pair<Any, Rect>? {
        val host = state.host ?: return null
        if (!host.isAttached) return null
        for ((key, value) in state.items) {
            val coords = value.first
            if (!coords.isAttached) continue
            val rect = runCatching { host.localBoundingBoxOf(coords, clipBounds = false) }.getOrNull() ?: continue
            if (rect.contains(pos)) return key to rect
        }
        return null
    }

    fun flowTo(rect: Rect, snap: Boolean) {
        val fast = spring<Float>(dampingRatio = 0.62f, stiffness = 950f)
        val slow = spring<Float>(dampingRatio = 0.55f, stiffness = 300f)
        if (snap) {
            scope.launch { state.left.snapTo(rect.left) }
            scope.launch { state.top.snapTo(rect.top) }
            scope.launch { state.right.snapTo(rect.right) }
            scope.launch { state.bottom.snapTo(rect.bottom) }
            return
        }
        // the edge in front moves fast and the edge behind follows slowly = liquid stretch
        val goingDown = rect.center.y > (state.top.value + state.bottom.value) / 2f
        val goingRight = rect.center.x > (state.left.value + state.right.value) / 2f
        val specTop: AnimationSpec<Float> = if (goingDown) slow else fast
        val specBottom: AnimationSpec<Float> = if (goingDown) fast else slow
        val specLeft: AnimationSpec<Float> = if (goingRight) slow else fast
        val specRight: AnimationSpec<Float> = if (goingRight) fast else slow
        scope.launch { state.top.animateTo(rect.top, specTop) }
        scope.launch { state.bottom.animateTo(rect.bottom, specBottom) }
        scope.launch { state.left.animateTo(rect.left, specLeft) }
        scope.launch { state.right.animateTo(rect.right, specRight) }
    }

    return this
        .onGloballyPositioned { state.host = it }
        .pointerInput(state) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

                // 1) touch: the bubble appears on the item under the finger and moves with the finger
                //    (taps and scrolling work as normal; the bubble just follows)
                fun follow(pos: Offset) {
                    val hit = itemAt(pos) ?: return
                    if (hit.first != state.currentKey) {
                        val wasShown = state.currentKey != null
                        state.currentKey = hit.first
                        flowTo(hit.second, snap = !wasShown)
                    } else {
                        flowTo(hit.second, snap = false) // stays glued to the item while the page scrolls
                    }
                }
                state.currentKey = null
                follow(down.position)
                if (state.currentKey != null) {
                    scope.launch { state.shown.animateTo(0.6f, spring(dampingRatio = 0.6f, stiffness = 600f)) }
                }

                // wait: if the finger stays still for a moment, the bubble "grabs" (step 2)
                val result = withTimeoutOrNull(260L) {
                    var outcome = ""
                    while (outcome.isEmpty()) {
                        val e = awaitPointerEvent(PointerEventPass.Final)
                        val c = e.changes.firstOrNull { it.id == down.id }
                        if (c == null || !c.pressed) {
                            outcome = "up"
                        } else {
                            follow(c.position)
                            if (c.isConsumed || (c.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                outcome = "moved"
                            }
                        }
                    }
                    outcome
                }
                if (result != null) {
                    // the finger moved (scrolling) or was lifted: keep following until it is lifted
                    if (result == "moved") {
                        while (true) {
                            val e = awaitPointerEvent(PointerEventPass.Final)
                            val c = e.changes.firstOrNull { it.id == down.id } ?: break
                            if (!c.pressed) break
                            follow(c.position)
                        }
                    }
                    scope.launch { state.shown.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 420f)) }
                    state.currentKey = null
                    return@awaitEachGesture
                }
                val first = itemAt(down.position) ?: run {
                    scope.launch { state.shown.animateTo(0f) }
                    state.currentKey = null
                    return@awaitEachGesture
                }

                // 2) bubble mode: slide to choose, let go to open
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                state.currentKey = first.first
                flowTo(first.second, snap = false)
                scope.launch { state.shown.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f)) }
                var lastPos = down.position
                while (true) {
                    val e = awaitPointerEvent(PointerEventPass.Initial)
                    val c = e.changes.firstOrNull { it.id == down.id } ?: break
                    e.changes.forEach { it.consume() } // the list does not scroll, the item does not click by itself
                    if (!c.pressed) break
                    lastPos = c.position
                    val hit = itemAt(lastPos)
                    if (hit != null && hit.first != state.currentKey) {
                        state.currentKey = hit.first
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        flowTo(hit.second, snap = false)
                    }
                }
                // 3) let go: open the item under the finger
                val chosen = itemAt(lastPos)
                scope.launch { state.shown.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 420f)) }
                state.currentKey = null
                if (chosen != null) state.items[chosen.first]?.second?.invoke()
            }
        }
        .drawWithContent {
            drawContent()
            val s = state.shown.value
            if (s < 0.01f) return@drawWithContent
            val grow = 4.dp.toPx() * s
            val topLeft = Offset(state.left.value - grow, state.top.value - grow)
            val bubbleSize = Size(
                (state.right.value - state.left.value + grow * 2).coerceAtLeast(0f),
                (state.bottom.value - state.top.value + grow * 2).coerceAtLeast(0f),
            )
            val r = corner.toPx() + grow
            val a = s.coerceIn(0f, 1f)
            // glass body
            drawRoundRect(
                color = glass.copy(alpha = 0.10f * a),
                topLeft = topLeft,
                size = bubbleSize,
                cornerRadius = CornerRadius(r, r),
            )
            // light from the top, like a drop of glass
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.16f * a), Color.Transparent),
                    startY = topLeft.y,
                    endY = topLeft.y + bubbleSize.height * 0.6f,
                ),
                topLeft = topLeft,
                size = bubbleSize,
                cornerRadius = CornerRadius(r, r),
            )
            // glowing rim
            drawRoundRect(
                color = accent.copy(alpha = 0.55f * a),
                topLeft = topLeft,
                size = bubbleSize,
                cornerRadius = CornerRadius(r, r),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
}

// ---------------------------------------------------------------- liquid follow (song lists)

/**
 * For song lists (playlists, search results): touch the list and a glass drop appears under your
 * finger, as tall as a song row. Move your finger and the drop flows with it, stretching like
 * liquid. Let go and it melts away. It only watches touches: taps, long presses and scrolling
 * work exactly as before.
 */
@Composable
fun Modifier.kingsLiquidFollow(rowHeight: Dp = 64.dp, corner: Dp = 18.dp): Modifier {
    val top = remember { Animatable(0f) }
    val bottom = remember { Animatable(0f) }
    val shown = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val glass = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val half = rowHeight.toPx() / 2f
                scope.launch { top.snapTo(down.position.y - half) }
                scope.launch { bottom.snapTo(down.position.y + half) }
                scope.launch { shown.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 600f)) }
                var lastY = down.position.y
                while (true) {
                    val e = awaitPointerEvent(PointerEventPass.Final)
                    val c = e.changes.firstOrNull { it.id == down.id } ?: break
                    if (!c.pressed) break
                    val y = c.position.y
                    val goingDown = y > lastY
                    lastY = y
                    // the edge in front moves fast, the edge behind follows slowly = liquid stretch
                    val fast = spring<Float>(dampingRatio = 0.7f, stiffness = 1400f)
                    val slow = spring<Float>(dampingRatio = 0.6f, stiffness = 380f)
                    scope.launch { top.animateTo(y - half, if (goingDown) slow else fast) }
                    scope.launch { bottom.animateTo(y + half, if (goingDown) fast else slow) }
                }
                scope.launch { shown.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 380f)) }
            }
        }
        .drawWithContent {
            drawContent()
            val s = shown.value.coerceIn(0f, 1f)
            if (s < 0.01f) return@drawWithContent
            val inset = 8.dp.toPx() + (1f - s) * 10.dp.toPx()
            val topLeft = Offset(inset, top.value)
            val bubbleSize = Size((size.width - inset * 2).coerceAtLeast(0f), (bottom.value - top.value).coerceAtLeast(0f))
            val r = corner.toPx()
            drawRoundRect(
                color = glass.copy(alpha = 0.08f * s),
                topLeft = topLeft,
                size = bubbleSize,
                cornerRadius = CornerRadius(r, r),
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.14f * s), Color.Transparent),
                    startY = topLeft.y,
                    endY = topLeft.y + bubbleSize.height * 0.6f,
                ),
                topLeft = topLeft,
                size = bubbleSize,
                cornerRadius = CornerRadius(r, r),
            )
            drawRoundRect(
                color = accent.copy(alpha = 0.45f * s),
                topLeft = topLeft,
                size = bubbleSize,
                cornerRadius = CornerRadius(r, r),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
}
