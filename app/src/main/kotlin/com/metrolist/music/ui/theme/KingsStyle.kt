/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Kings Play styles (ready-made color themes) and the optional liquid effect.
 */

package com.metrolist.music.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import com.metrolist.music.ui.component.KingsLiquidSwitch as Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.metrolist.music.ui.player.KingsPlayerStyle
import com.metrolist.music.ui.player.KingsPlayerStyleKey
import com.metrolist.music.ui.player.KingsPremiumPlayerKey
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// ---------- Saved settings ----------
val KingsStyleKey = stringPreferencesKey("kingsStyle")
val LiquidEffectKey = booleanPreferencesKey("kingsLiquidEffect")
val LiquidStrengthKey = floatPreferencesKey("kingsLiquidStrength")
val LiquidSpeedKey = floatPreferencesKey("kingsLiquidSpeed")

const val DefaultLiquidStrength = 0.5f
const val DefaultLiquidSpeed = 1f

// ---------- Kings Play styles ----------
enum class KingsStyle(
    val label: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
) {
    // Uses Metrolist's own color palette / album-art colors instead of a Kings style
    CLASSIC("Classic", Color(0xFFED5564), Color(0xFF8E24AA), Color(0xFF1E88E5), Color(0xFF121212)),
    NEON("Kings Neon", Color(0xFF9B5CFF), Color(0xFF39E7FF), Color(0xFFFF4FCB), Color(0xFF080914)),
    OCEAN("Ocean Night", Color(0xFF168CFF), Color(0xFF39E7FF), Color(0xFF7C9CFF), Color(0xFF060C18)),
    SUNSET("Sunset Gold", Color(0xFFFFB547), Color(0xFFFF4FCB), Color(0xFFFF7A45), Color(0xFF120A0A)),
    EMERALD("Emerald Glow", Color(0xFF31D69B), Color(0xFF39E7FF), Color(0xFFB6F36A), Color(0xFF06110E)),
    ROSE("Rose Neon", Color(0xFFFF4F8B), Color(0xFFC86BFF), Color(0xFFFFB3C7), Color(0xFF12060C)),
    MIDNIGHT("Midnight", Color(0xFFE6E6F0), Color(0xFF9A9AB2), Color(0xFF6E7BFF), Color(0xFF000000)),
}

val DefaultKingsStyle = KingsStyle.NEON

private fun onColorFor(c: Color): Color =
    if (c.luminance() > 0.45f) Color(0xFF111111) else Color.White

/** Full dark color scheme for a Kings Play style. */
fun kingsDarkScheme(style: KingsStyle): ColorScheme {
    val bg = style.background
    val p = style.primary
    fun tone(lift: Float) = lerp(lerp(bg, Color.White, lift), p, 0.05f)

    return darkColorScheme(
        primary = p,
        onPrimary = onColorFor(p),
        primaryContainer = lerp(bg, p, 0.35f),
        onPrimaryContainer = lerp(p, Color.White, 0.75f),

        secondary = style.secondary,
        onSecondary = onColorFor(style.secondary),
        secondaryContainer = lerp(bg, style.secondary, 0.30f),
        onSecondaryContainer = lerp(style.secondary, Color.White, 0.75f),

        tertiary = style.tertiary,
        onTertiary = onColorFor(style.tertiary),
        tertiaryContainer = lerp(bg, style.tertiary, 0.30f),
        onTertiaryContainer = lerp(style.tertiary, Color.White, 0.75f),

        background = bg,
        onBackground = Color(0xFFECEAF6),
        surface = bg,
        onSurface = Color(0xFFECEAF6),
        surfaceVariant = tone(0.16f),
        onSurfaceVariant = Color(0xFFB9B8CF),
        surfaceTint = p,
        surfaceDim = bg,
        surfaceBright = tone(0.20f),

        surfaceContainerLowest = lerp(bg, Color.Black, 0.3f),
        surfaceContainerLow = tone(0.04f),
        surfaceContainer = tone(0.07f),
        surfaceContainerHigh = tone(0.11f),
        surfaceContainerHighest = tone(0.15f),

        outline = tone(0.28f),
        outlineVariant = tone(0.18f),
    )
}

// ---------- Liquid effect ----------
/**
 * Slowly moving, glowing color blobs drawn over the app.
 * It only draws; it never blocks touches.
 */
@Composable
fun LiquidOverlay(
    modifier: Modifier,
    colors: List<Color>,
    strength: Float,
    speed: Float,
    dark: Boolean,
) {
    // Slow drift: 30 updates a second is plenty and saves a lot of battery
    val durationSec = 28f / speed.coerceIn(0.25f, 3f)
    val clock = rememberKingsSlowClock(fps = 30)
    run {
        val alpha = (if (dark) 0.10f else 0.06f) + (if (dark) 0.22f else 0.12f) * strength.coerceIn(0f, 1f)
        val blend = if (dark) BlendMode.Screen else BlendMode.SrcOver

        Canvas(modifier = modifier) {
            val a = ((clock.value / durationSec) % 1f) * 2f * PI.toFloat()
            val w = size.width
            val h = size.height
            val base = max(w, h) * 0.50f

            // Each blob moves on its own looping path and gently "breathes"
            val blobs = listOf(
                Triple(Offset(w * (0.20f + 0.18f * sin(a)), h * (0.18f + 0.10f * cos(a))), 1.00f, 0f),
                Triple(Offset(w * (0.82f + 0.14f * cos(a * 2f)), h * (0.35f + 0.16f * sin(a))), 0.85f, 1.7f),
                Triple(Offset(w * (0.50f + 0.25f * sin(a * 2f + 1f)), h * (0.72f + 0.12f * cos(a + 2f))), 0.95f, 3.1f),
                Triple(Offset(w * (0.15f + 0.12f * cos(a + 4f)), h * (0.88f + 0.08f * sin(a * 2f))), 0.70f, 4.4f),
            )

            blobs.forEachIndexed { i, (center, scale, phase) ->
                val color = colors[i % colors.size]
                val radius = base * scale * (0.85f + 0.15f * sin(a * 2f + phase))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                    blendMode = blend,
                )
            }
        }
    }
}

// ---------- Settings card (shown on the Theme screen) ----------
@Composable
fun KingsStyleControls(modifier: Modifier = Modifier) {
    val (style, onStyleChange) = rememberEnumPreference(KingsStyleKey, DefaultKingsStyle)
    val (glassOn, onGlassOnChange) = rememberPreference(LiquidGlassKey, true)
    val (playerStyle, onPlayerStyleChange) = rememberEnumPreference(KingsPlayerStyleKey, KingsPlayerStyle.NORMAL)
    val (premiumOn, onPremiumOnChange) = rememberPreference(KingsPremiumPlayerKey, true)
    val (liquidOn, onLiquidOnChange) = rememberPreference(LiquidEffectKey, false)
    val (strength, onStrengthChange) = rememberPreference(LiquidStrengthKey, DefaultLiquidStrength)
    val (speed, onSpeedChange) = rememberPreference(LiquidSpeedKey, DefaultLiquidSpeed)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Kings Play style",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(KingsStyle.entries) { item ->
                    StyleChip(
                        style = item,
                        selected = item == style,
                        onClick = { onStyleChange(item) }
                    )
                }
            }

            Text(
                text = if (style == KingsStyle.CLASSIC) {
                    "Classic uses the color palette below (or album-art colors)."
                } else {
                    "Best in dark mode. Pick Classic to use the color palette below."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Liquid glass",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "iPhone-style see-through glass bottom bar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = glassOn, onCheckedChange = onGlassOnChange)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Premium player",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Clean white buttons, thin slider, clean song titles, tap feel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = premiumOn, onCheckedChange = onPremiumOnChange)
            }

            // Player style: Normal / Album cover / Vinyl / Turntable
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Player style",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "How the song looks on the full player screen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                KingsPlayerStyle.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { option ->
                            PlayerStyleOption(
                                text = option.label,
                                selected = option == playerStyle,
                                onClick = { onPlayerStyleChange(option) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Liquid glow",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Moving glow of colors across the app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = liquidOn, onCheckedChange = onLiquidOnChange)
            }

            if (liquidOn) {
                LabeledSlider(
                    label = "Strength",
                    leftHint = "Soft",
                    rightHint = "Bold",
                    value = strength,
                    range = 0f..1f,
                    onValueChange = onStrengthChange
                )
                LabeledSlider(
                    label = "Speed",
                    leftHint = "Slow",
                    rightHint = "Fast",
                    value = speed,
                    range = 0.4f..2.5f,
                    onValueChange = onSpeedChange
                )
                Text(
                    text = "Tip: the liquid glow uses a little more battery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StyleChip(
    style: KingsStyle,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                )
                .padding(5.dp)
                .background(
                    brush = Brush.linearGradient(
                        listOf(style.primary, style.secondary, style.tertiary)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (style == KingsStyle.CLASSIC) {
                Text("Aa", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        Text(
            text = style.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    leftHint: String,
    rightHint: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    // Move smoothly while dragging, save only when the finger is lifted
    var local by remember(value) { mutableFloatStateOf(value.coerceIn(range.start, range.endInclusive)) }
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onValueChange(local) },
            valueRange = range
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(leftHint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(modifier = Modifier.weight(1f))
            Text(rightHint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlayerStyleOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.primary.copy(alpha = 0.22f) else colors.surfaceContainerHighest)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.primary else colors.outlineVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) colors.primary else colors.onSurface
        )
    }
}
