/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Animated "thought" shown instead of the "Home" title (default: ONE WORLD. ONE BEAT).
 * Words rise in one by one, then a soft color shine keeps flowing through the text.
 * Users can change the text in Settings.
 */

package com.metrolist.music.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import com.metrolist.music.utils.rememberPreference

val KingsThoughtKey = stringPreferencesKey("kingsHomeThought")
const val DefaultKingsThought = "ONE WORLD. ONE BEAT"
const val KingsThoughtMaxLength = 40

/** The user's thought, or the default when empty. */
@Composable
fun rememberKingsThought(): String {
    val text by rememberPreference(KingsThoughtKey, DefaultKingsThought)
    return text.trim().ifBlank { DefaultKingsThought }
}

@Composable
fun KingsHomeThought(modifier: Modifier = Modifier) {
    val text = rememberKingsThought()
    val colors = MaterialTheme.colorScheme
    val words = remember(text) { text.split(Regex("\\s+")) }

    // Words appear one by one every time Home opens or the text changes
    val appear = remember(text) { Animatable(0f) }
    LaunchedEffect(text) {
        appear.snapTo(0f)
        appear.animateTo(1f, tween(durationMillis = 450 + words.size * 160, easing = LinearEasing))
    }

    // Soft shine that keeps flowing through the letters
    val shine by rememberInfiniteTransition(label = "kpThought").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
        label = "kpThoughtShine",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2500),
    ) {
        words.forEachIndexed { i, word ->
            val total = words.size.coerceAtLeast(1)
            val from = i / (total + 1.5f)
            val to = from + 2.5f / (total + 1.5f)
            val k = FastOutSlowInEasing.transform(((appear.value - from) / (to - from)).coerceIn(0f, 1f))
            val x = -400f + shine * 800f
            Text(
                text = word,
                maxLines = 1,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(colors.onSurface, colors.primary, colors.tertiary, colors.onSurface),
                        start = Offset(x, 0f),
                        end = Offset(x + 400f, 40f),
                        tileMode = TileMode.Mirror,
                    ),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                ),
                modifier = Modifier.graphicsLayer {
                    alpha = k
                    translationY = (1f - k) * 14.dp.toPx()
                    val s = 1.15f - 0.15f * k
                    scaleX = s
                    scaleY = s
                },
            )
            if (i != words.lastIndex) Spacer(Modifier.width(7.dp))
        }
    }
}
