/**
 * Kings Play - iPhone-style liquid glass for song lists (playlists, song search) and Settings groups.
 * Frosted body, bright top edge, soft rim. The glass still follows your finger like liquid.
 */

package com.metrolist.music.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Counts the song lists that currently show the iPhone glass (rows inside them do not squish). */
object KingsIosLiquid {
    var listHosts by mutableIntStateOf(0)
}

/** Song-row touch: the old squish, except inside a list that has the iPhone glass. */
@Composable
fun Modifier.kingsRowPress(corner: Dp = 8.dp): Modifier =
    if (KingsIosLiquid.listHosts > 0) this else kingsLiquidPress(corner)

internal fun DrawScope.drawKingsIosGlass(rect: Rect, cr: CornerRadius, a: Float, ink: Color) {
    // frosted body
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(ink.copy(alpha = 0.16f * a), ink.copy(alpha = 0.08f * a)),
            startY = rect.top,
            endY = rect.bottom,
        ),
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = cr,
    )
    // light from the top
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.26f * a), Color.Transparent),
            startY = rect.top,
            endY = rect.top + rect.height * 0.5f,
        ),
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = cr,
    )
    // glass rim: bright at the top, soft in the middle, a little light at the bottom
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.70f * a),
                Color.White.copy(alpha = 0.10f * a),
                Color.White.copy(alpha = 0.38f * a),
            ),
            startY = rect.top,
            endY = rect.bottom,
        ),
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = cr,
        style = Stroke(width = 1.dp.toPx()),
    )
}