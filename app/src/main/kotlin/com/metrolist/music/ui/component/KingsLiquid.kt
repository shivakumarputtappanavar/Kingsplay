/**
 * Kings Play - based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * v18: the glass that followed your finger is removed everywhere.
 *  - kingsLiquidPress: touch a row and it lights up softly in place (it does not move or follow the finger).
 *  - kingsBubbleHost / kingsBubbleHostIos / kingsLiquidFollow / kingsBubbleItem: kept only so every screen
 *    still compiles. They do nothing now.
 * The liquid bottom bar bubble is a separate piece of code and is not touched.
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- still press highlight

/**
 * Soft light on the whole row while it is pressed. No squish, no movement, nothing follows the finger.
 * Watches touches only (never takes them), so clicks, long presses and scrolling are not changed.
 */
@Composable
fun Modifier.kingsLiquidPress(corner: Dp = 18.dp): Modifier {
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val light = MaterialTheme.colorScheme.onSurface
    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                scope.launch { press.animateTo(1f, spring(stiffness = 900f)) }
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    // the list started scrolling: let go
                    if (change.isConsumed && change.positionChange() != Offset.Zero) break
                }
                scope.launch { press.animateTo(0f, spring(stiffness = 500f)) }
            }
        }
        .drawWithContent {
            drawContent()
            val p = press.value.coerceIn(0f, 1f)
            if (p > 0.01f) {
                val r = corner.toPx()
                drawRoundRect(
                    color = light.copy(alpha = 0.10f * p),
                    cornerRadius = CornerRadius(r, r),
                )
            }
        }
}

// ---------------------------------------------------------------- old glass lens: switched off

/** Kept so other files still compile. */
@Stable
class KingsBubbleState {
    internal val items = mutableMapOf<Any, LayoutCoordinates>()
}

@Composable
fun rememberKingsBubble(): KingsBubbleState = remember { KingsBubbleState() }

/** Does nothing now. */
@Suppress("UNUSED_PARAMETER")
fun Modifier.kingsBubbleItem(
    state: KingsBubbleState?,
    key: Any,
    enabled: Boolean = true,
    onSelect: () -> Unit = {},
): Modifier = this

/** Does nothing now. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun Modifier.kingsBubbleHost(state: KingsBubbleState, corner: Dp = 20.dp): Modifier = this

/** Does nothing now. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun Modifier.kingsBubbleHostIos(state: KingsBubbleState, corner: Dp = 20.dp): Modifier = this

/** Does nothing now. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun Modifier.kingsLiquidFollow(rowHeight: Dp = 64.dp, corner: Dp = 20.dp): Modifier = this
