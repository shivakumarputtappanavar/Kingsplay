/**
 * Metrolist Project (C) 2026
 * Kings Play changes: Kings Play styles + optional liquid effect
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference

val DefaultThemeColor = Color(0xFFED5564)

@Composable
fun MetrolistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    // Kings Play settings (changed on Settings > Appearance > Theme)
    val kingsStyle by rememberEnumPreference(KingsStyleKey, DefaultKingsStyle)
    val liquidOn by rememberPreference(LiquidEffectKey, false)
    val liquidStrength by rememberPreference(LiquidStrengthKey, DefaultLiquidStrength)
    val liquidSpeed by rememberPreference(LiquidSpeedKey, DefaultLiquidSpeed)

    val baseColorScheme = when {
        // A Kings Play style is selected
        kingsStyle != KingsStyle.CLASSIC && darkTheme -> {
            remember(kingsStyle) { kingsDarkScheme(kingsStyle) }
        }
        kingsStyle != KingsStyle.CLASSIC -> {
            rememberDynamicColorScheme(
                seedColor = kingsStyle.primary,
                isDark = false,
                specVersion = ColorSpec.SpecVersion.SPEC_2025,
                style = PaletteStyle.TonalSpot
            )
        }
        // Classic: Metrolist's original behaviour
        themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            rememberDynamicColorScheme(
                seedColor = themeColor,
                isDark = darkTheme,
                specVersion = ColorSpec.SpecVersion.SPEC_2025,
                style = PaletteStyle.TonalSpot
            )
        }
    }

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        Box {
            content()
            if (liquidOn) {
                LiquidOverlay(
                    modifier = Modifier.matchParentSize(),
                    colors = listOf(
                        colorScheme.primary,
                        colorScheme.secondary,
                        colorScheme.tertiary,
                        colorScheme.primary,
                    ),
                    strength = liquidStrength,
                    speed = liquidSpeed,
                    dark = darkTheme,
                )
            }
        }
    }
}

fun Bitmap.extractThemeColor(): Color = Color(
    Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .rankedColors(1, DefaultThemeColor.toArgb())
        .first()
)

internal fun Palette.rankedColors(
    desiredColorCount: Int,
    fallbackColor: Int,
): List<Int> = Score.score(
    swatches.associate { it.rgb to it.population },
    desiredColorCount,
    fallbackColor,
    true,
)

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
