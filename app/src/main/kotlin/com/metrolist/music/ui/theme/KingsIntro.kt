/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Animated intro shown when the app opens:
 * chosen logo pops in with a glowing ring, "KINGS PLAY" appears letter by letter,
 * then the tagline — all in the colors of the chosen Kings Play style. Tap to skip.
 */

package com.metrolist.music.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.utils.KingsAppIcon
import com.metrolist.music.utils.KingsIntroState
import kotlinx.coroutines.launch

/** True inside a Kings theme, so only the outermost theme shows the intro. */
val LocalInsideKingsTheme = staticCompositionLocalOf { false }

/** Shows the intro when MainActivity asked for it (fresh app open). */
@Composable
fun KingsIntroHost() {
    val icon = KingsIntroState.icon ?: return
    KingsIntroScreen(icon = icon, onDone = { KingsIntroState.icon = null })
}

private fun segment(p: Float, from: Float, to: Float): Float =
    FastOutSlowInEasing.transform(((p - from) / (to - from)).coerceIn(0f, 1f))

@Composable
fun KingsIntroScreen(icon: KingsAppIcon, onDone: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val progress = remember { Animatable(0f) }
    val exit = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(icon) {
        progress.snapTo(0f)
        exit.snapTo(1f)
        progress.animateTo(1f, tween(durationMillis = 2400, easing = LinearEasing))
        exit.animateTo(0f, tween(durationMillis = 450))
        onDone()
    }

    val p = progress.value
    val name = "KINGS PLAY"
    val nameBrush = Brush.linearGradient(listOf(colors.primary, colors.secondary, colors.tertiary))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = exit.value
                val s = 1f + (1f - exit.value) * 0.08f
                scaleX = s
                scaleY = s
            }
            .background(colors.background)
            // Tap anywhere to skip; also stops taps reaching the app underneath
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                scope.launch {
                    exit.animateTo(0f, tween(250))
                    onDone()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Soft color glows in the style's colors
        Canvas(modifier = Modifier.fillMaxSize()) {
            val glow = segment(p, 0f, 0.5f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colors.primary.copy(alpha = 0.35f * glow), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.25f),
                    radius = size.maxDimension * 0.55f,
                ),
                radius = size.maxDimension,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colors.tertiary.copy(alpha = 0.28f * glow), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.8f),
                    radius = size.maxDimension * 0.5f,
                ),
                radius = size.maxDimension,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Logo with a spinning gradient ring
            val logoIn = segment(p, 0.02f, 0.30f)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(190.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(colors.primary.copy(alpha = 0.45f * logoIn), Color.Transparent),
                            radius = r,
                        ),
                        radius = r,
                    )
                    rotate(degrees = p * 540f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(Color.Transparent, colors.primary, colors.secondary, colors.tertiary, Color.Transparent)
                            ),
                            startAngle = 0f,
                            sweepAngle = 300f * logoIn,
                            useCenter = false,
                            topLeft = Offset(size.width * 0.06f, size.height * 0.06f),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.88f, size.height * 0.88f),
                            style = Stroke(width = 3.dp.toPx()),
                        )
                    }
                }
                Image(
                    painter = painterResource(icon.preview),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(132.dp)
                        .graphicsLayer {
                            val s = 0.6f + 0.4f * logoIn
                            scaleX = s
                            scaleY = s
                            alpha = logoIn
                            rotationZ = (1f - logoIn) * -12f
                        }
                        .clip(RoundedCornerShape(34.dp)),
                )
            }

            Spacer(Modifier.height(26.dp))

            // "KINGS PLAY" letter by letter
            Row(horizontalArrangement = Arrangement.Center) {
                name.forEachIndexed { i, ch ->
                    val start = 0.30f + i * 0.035f
                    val k = segment(p, start, start + 0.14f)
                    Text(
                        text = ch.toString(),
                        style = TextStyle(
                            brush = nameBrush,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp,
                        ),
                        modifier = Modifier.graphicsLayer {
                            alpha = k
                            translationY = (1f - k) * 22.dp.toPx()
                            val s = 1.3f - 0.3f * k
                            scaleX = s
                            scaleY = s
                        },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "YOUR SOUND · YOUR WORLD",
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.graphicsLayer {
                    val k = segment(p, 0.68f, 0.86f)
                    alpha = k
                    translationY = (1f - k) * 10.dp.toPx()
                },
            )
        }
    }
}
