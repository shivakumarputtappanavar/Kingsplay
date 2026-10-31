/**
 * Kings Play - dark glass look for 3-dot menus
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** True only inside the glass 3-dot menu sheet. Everywhere else the old look stays. */
val LocalKingsGlass = compositionLocalOf { false }

val KingsGlassText = Color(0xFFF5F5F7)
val KingsGlassTextDim = Color(0xA6FFFFFF)
val KingsGlassDivider = Color(0x1AFFFFFF)
val KingsGlassEdgeTop = Color(0x47FFFFFF)
val KingsGlassEdgeBottom = Color(0x12FFFFFF)

/** Frosted glass card: soft light gradient fill and a bright thin edge. */
fun Modifier.kingsGlassSurface(shape: Shape): Modifier =
    this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(Color(0x26FFFFFF), Color(0x12FFFFFF))))
        .border(
            width = 0.8.dp,
            brush = Brush.verticalGradient(listOf(KingsGlassEdgeTop, KingsGlassEdgeBottom)),
            shape = shape,
        )
