/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * iPhone-style "liquid glass" using the Backdrop library by Kyant (Apache-2.0).
 * The glass shows a blurred, bent copy of whatever is behind it.
 * Blur needs Android 12+, the bending (lens) needs Android 13+.
 * Older phones get a frosted see-through look instead.
 */

package com.metrolist.music.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.metrolist.music.utils.rememberPreference

/** Saved setting: liquid glass on/off (on by default). */
val LiquidGlassKey = booleanPreferencesKey("kingsLiquidGlass")

/** The screen content that glass parts can "see through" to. Null = not available. */
val LocalKingsBackdrop: ProvidableCompositionLocal<LayerBackdrop?> = compositionLocalOf { null }

/** Creates the backdrop that records the app screens (used in MainActivity). */
@Composable
fun rememberKingsBackdrop(): LayerBackdrop {
    val background = MaterialTheme.colorScheme.background
    val onDraw: ContentDrawScope.() -> Unit = remember(background) {
        {
            drawRect(background)
            drawContent()
        }
    }
    return rememberLayerBackdrop(onDraw = onDraw)
}

/** True when liquid glass should be drawn right now. */
@Composable
fun rememberLiquidGlassEnabled(): Boolean {
    val enabled = rememberPreference(LiquidGlassKey, true).value
    return enabled && LocalKingsBackdrop.current != null
}

/**
 * Turns any rounded box into liquid glass.
 * [tint] gives the glass a soft color (for example the Kings Play style color).
 */
fun Modifier.kingsGlass(
    backdrop: LayerBackdrop,
    shape: CornerBasedShape,
    surface: Color,
    tint: Color,
    dark: Boolean,
    refraction: Boolean = true,
): Modifier {
    val hasBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    // Without real blur (older phones) the glass is made more solid so text stays readable
    val surfaceAlpha = when {
        !hasBlur -> 0.88f
        dark -> 0.30f
        else -> 0.45f
    }
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(6f.dp.toPx())
            if (refraction) {
                lens(
                    refractionHeight = 18f.dp.toPx(),
                    refractionAmount = 34f.dp.toPx(),
                    chromaticAberration = false,
                )
            }
        },
        onDrawSurface = {
            drawRect(surface.copy(alpha = surfaceAlpha))
            drawRect(tint, blendMode = BlendMode.Hue, alpha = 0.18f)
            drawRect(tint.copy(alpha = 0.06f))
        },
    )
}
