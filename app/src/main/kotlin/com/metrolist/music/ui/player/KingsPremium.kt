/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * "Premium player": calm white/glass buttons, thin slider, clean song titles,
 * smooth title transitions and a light tap feel on play/pause.
 */

package com.metrolist.music.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey

/** Saved setting: premium player look (on by default). */
val KingsPremiumPlayerKey = booleanPreferencesKey("kingsPremiumPlayer")

private val TITLE_JUNK = listOf(
    // (Official Video), [Official Music Video], (Official Audio), (Lyric Video), (Lyrics), (Visualizer) ...
    Regex(
        """\s*[\(\[]\s*(official\s*)?(music\s*|lyric(al)?\s*|audio\s*|hd\s*|4k\s*)?(video|audio|lyrics?|visuali[sz]er|mv|full\s*song|full\s*video)(\s*song)?\s*[\)\]]""",
        RegexOption.IGNORE_CASE,
    ),
    // (Official Video Song), (Official) ...
    Regex("""\s*[\(\[]\s*official[^\)\]]*[\)\]]""", RegexOption.IGNORE_CASE),
    // | 7clouds Hindi, | T-Series ... (anything after a pipe)
    Regex("""\s*\|.*$"""),
    // - Lyrics / - Official Video at the end
    Regex("""\s*[-–]\s*(official\s*)?(lyrics?|lyric\s*video|audio|video)\s*$""", RegexOption.IGNORE_CASE),
)

/** "Kal Ho Naa Ho - (Lyrics) | 7clouds Hindi" -> "Kal Ho Naa Ho" */
fun cleanSongTitle(title: String): String {
    var t = title
    TITLE_JUNK.forEach { t = t.replace(it, "") }
    t = t.trim().trimEnd('-', '–', '|', ':').trim()
    return t.ifBlank { title }
}

// ============================================================================
// Kings Play: impressive play/pause button
// ============================================================================

/**
 * Play icon that smoothly morphs into the pause icon (and back).
 * The triangle is split into two halves that turn into the two pause bars.
 */
@Composable
fun KingsPlayPauseIcon(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "kpPlayPauseMorph",
    )
    val path = remember { Path() }

    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val ox = (size.width - 24f * s) / 2f
        val oy = (size.height - 24f * s) / 2f
        fun lx(a: Float, b: Float) = ox + (a + (b - a) * progress) * s
        fun ly(a: Float, b: Float) = oy + (a + (b - a) * progress) * s

        path.reset()
        // Left half: play-left -> pause-left bar
        path.moveTo(lx(8f, 6f), ly(5f, 5f))
        path.lineTo(lx(13.5f, 10f), ly(8.5f, 5f))
        path.lineTo(lx(13.5f, 10f), ly(15.5f, 19f))
        path.lineTo(lx(8f, 6f), ly(19f, 19f))
        path.close()
        // Right half: play-tip -> pause-right bar
        path.moveTo(lx(13.5f, 14f), ly(8.5f, 5f))
        path.lineTo(lx(19f, 18f), ly(12f, 5f))
        path.lineTo(lx(19f, 18f), ly(12f, 19f))
        path.lineTo(lx(13.5f, 14f), ly(15.5f, 19f))
        path.close()

        drawPath(path = path, color = color)
    }
}

/**
 * Bounce on press, a ring that bursts out on every play/pause,
 * and a soft breathing glow while music is playing.
 */
@Composable
fun Modifier.kingsPlayButtonFx(
    isPlaying: Boolean,
    pressed: Boolean,
    glowColor: Color,
): Modifier {
    // Squeeze when pressed, spring back with a little bounce
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "kpPlayScale",
    )

    // Ring burst on every change
    val ring = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(isPlaying) {
        if (first) {
            first = false
        } else {
            ring.snapTo(0f)
            ring.animateTo(1f, tween(durationMillis = 650, easing = FastOutSlowInEasing))
        }
    }

    // Soft glow while playing
    val glowStrength by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(500),
        label = "kpGlowOn",
    )
    // Steady glow (a never-ending animation would redraw the screen all the time)
    val breath = 0.8f

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawBehind {
            val radius = size.height / 2f
            // Soft glow: a few growing, fading rounded rects (works on every Android version)
            val g = glowStrength * breath
            if (g > 0.01f) {
                for (i in 1..5) {
                    val grow = i * 3.dp.toPx()
                    drawRoundRect(
                        color = glowColor.copy(alpha = 0.10f * g * (6 - i) / 5f),
                        topLeft = Offset(-grow, -grow),
                        size = Size(size.width + grow * 2, size.height + grow * 2),
                        cornerRadius = CornerRadius(radius + grow, radius + grow),
                    )
                }
            }
            // Ring burst
            val p = ring.value
            if (p < 1f) {
                val grow = p * 14.dp.toPx()
                drawRoundRect(
                    color = glowColor.copy(alpha = 0.7f * (1f - p)),
                    topLeft = Offset(-grow, -grow),
                    size = Size(size.width + grow * 2, size.height + grow * 2),
                    cornerRadius = CornerRadius(radius + grow, radius + grow),
                    style = Stroke(width = (2.5f * (1f - p) + 0.5f).dp.toPx()),
                )
            }
        }
}
