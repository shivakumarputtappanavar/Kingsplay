/**
 * Kings Play - helpers for the liquid glass lens.
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

/** Counts the song lists that currently show the glass lens (rows inside them do not squish). */
object KingsIosLiquid {
    var listHosts by mutableIntStateOf(0)
}

/** Song-row touch: the old squish, except inside a list that has the glass lens. */
@Composable
fun Modifier.kingsRowPress(corner: Dp = 8.dp): Modifier =
    if (KingsIosLiquid.listHosts > 0) this else kingsLiquidPress(corner)

/** Glass on top of the lens: faint frosted tint, light from the top, bright rim. */
internal fun DrawScope.drawKingsGlassBody(rect: Rect, cr: CornerRadius, a: Float, ink: Color) {
    drawRoundRect(
        color = ink.copy(alpha = 0.06f * a),
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = cr,
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.30f * a), Color.Transparent),
            startY = rect.top,
            endY = rect.top + rect.height * 0.45f,
        ),
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = cr,
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.85f * a),
                Color.White.copy(alpha = 0.12f * a),
                Color.White.copy(alpha = 0.45f * a),
            ),
            startY = rect.top,
            endY = rect.bottom,
        ),
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = cr,
        style = Stroke(width = 1.2.dp.toPx()),
    )
}
