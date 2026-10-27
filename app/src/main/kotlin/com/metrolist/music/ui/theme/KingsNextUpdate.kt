/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Next-update extras:
 *  - Now-playing glow: soft glow in the song cover's colors (mini player + player)
 *  - Heart burst: little hearts pop when you like a song
 *  - Glass search bar look
 *  - Lyrics in the Kings Play theme colors
 * All effects are still (no endless animations), so they cost no battery when idle.
 */

package com.metrolist.music.ui.theme

import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------- Now-playing glow

private val coverColorCache = LruCache<String, Int>(64)

/** The main bright color of a song cover (null while loading or without a cover). */
@Composable
fun rememberCoverGlowColor(url: String?): Color? {
    val context = LocalContext.current
    var color by remember(url) { mutableStateOf(url?.let { coverColorCache.get(it) }?.let { Color(it) }) }
    LaunchedEffect(url) {
        if (url == null || color != null) return@LaunchedEffect
        val argb = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(96, 96)
                    .allowHardware(false)
                    .build()
                val bitmap = context.imageLoader.execute(request).image?.toBitmap() ?: return@runCatching null
                val palette = Palette.from(bitmap).maximumColorCount(12).generate()
                val swatch = palette.vibrantSwatch
                    ?: palette.lightVibrantSwatch
                    ?: palette.dominantSwatch
                swatch?.rgb
            }.getOrNull()
        }
        if (argb != null) {
            coverColorCache.put(url, argb)
            color = Color(argb)
        }
    }
    return color
}

/** Makes very dark cover colors bright enough to glow. */
private fun Color.glowable(): Color =
    if (luminance() < 0.12f) lerp(this, Color.White, 0.35f) else this

/**
 * Soft glow around a rounded box (mini player).
 * Put it BEFORE the glass/clip modifiers so the glow can spill outside.
 */
@Composable
fun Modifier.kingsCoverGlow(color: Color?, corner: Dp, strength: Float = 1f): Modifier {
    val target = color?.glowable() ?: Color.Transparent
    val animated by animateColorAsState(target, tween(700), label = "kpCoverGlow")
    return this.drawBehind {
        if (animated.alpha < 0.01f) return@drawBehind
        val r = corner.toPx()
        for (i in 1..6) {
            val grow = i * 3.dp.toPx()
            drawRoundRect(
                color = animated.copy(alpha = 0.075f * strength * (7 - i) / 6f),
                topLeft = Offset(-grow, -grow + 2.dp.toPx()),
                size = Size(size.width + grow * 2, size.height + grow * 2),
                cornerRadius = CornerRadius(r + grow, r + grow),
            )
        }
    }
}

/** Big soft glow behind the album cover in the player. Fill the cover area with it. */
@Composable
fun KingsCoverGlowLayer(color: Color?, modifier: Modifier = Modifier) {
    val target = color?.glowable() ?: Color.Transparent
    val animated by animateColorAsState(target, tween(900), label = "kpPlayerGlow")
    Box(
        modifier = modifier.drawBehind {
            if (animated.alpha < 0.01f) return@drawBehind
            val radius = size.minDimension * 0.62f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animated.copy(alpha = 0.55f),
                        animated.copy(alpha = 0.22f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
    )
}

// ---------------------------------------------------------------- Heart burst

private fun DrawScope.drawHeart(center: Offset, size: Float, color: Color) {
    val s = size / 2f
    val path = Path().apply {
        moveTo(center.x, center.y + s * 0.85f)
        cubicTo(center.x - s * 1.25f, center.y + s * 0.05f, center.x - s * 0.85f, center.y - s * 1.0f, center.x, center.y - s * 0.35f)
        cubicTo(center.x + s * 0.85f, center.y - s * 1.0f, center.x + s * 1.25f, center.y + s * 0.05f, center.x, center.y + s * 0.85f)
        close()
    }
    drawPath(path, color)
}

private val HeartColors = listOf(
    Color(0xFFFF4D6D), Color(0xFFFF8FA3), Color(0xFFFFB3C1), Color(0xFFFF6B9A), Color(0xFFFFD166),
)

/**
 * Hearts pop out of the button when [liked] turns on, and the button does a little bounce.
 * Nothing happens on first show or when un-liking.
 */
@Composable
fun Modifier.kingsHeartBurst(liked: Boolean): Modifier {
    val progress = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    var lastLiked by remember { mutableStateOf(liked) }
    LaunchedEffect(liked) {
        if (first) {
            first = false
            lastLiked = liked
            return@LaunchedEffect
        }
        val turnedOn = liked && !lastLiked
        lastLiked = liked
        if (turnedOn) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
        }
    }
    val p = progress.value
    return this
        .graphicsLayer {
            // bounce: grows to 1.25x then settles
            val bounce = if (p < 1f) 1f + 0.25f * sin(p * PI.toFloat()) * (1f - p) * 2f else 1f
            scaleX = bounce
            scaleY = bounce
        }
        .drawWithContent {
            drawContent()
            if (p >= 1f) return@drawWithContent
            val count = 9
            val maxDist = size.minDimension * 1.25f
            for (i in 0 until count) {
                val angle = (-PI / 2 + (i - (count - 1) / 2.0) * (PI / (count + 1)) * 1.6).toFloat()
                val wobble = sin((p * 6f + i) * 1.3f) * 4.dp.toPx()
                val dist = maxDist * FastOutSlowInEasing.transform(p) * (0.75f + 0.25f * ((i * 37) % 5) / 4f)
                val c = Offset(
                    center.x + cos(angle) * dist + wobble,
                    center.y + sin(angle) * dist - p * 10.dp.toPx(),
                )
                val heartSize = (6f + (i % 3) * 2.5f).dp.toPx() * (1f - 0.35f * p)
                val alpha = (1f - p).coerceIn(0f, 1f)
                rotate(degrees = (i - count / 2) * 9f * p, pivot = c) {
                    drawHeart(c, heartSize, HeartColors[i % HeartColors.size].copy(alpha = alpha))
                }
            }
            // soft ring
            val ringR = size.minDimension * (0.55f + 0.6f * p)
            drawCircle(
                color = Color(0xFFFF4D6D).copy(alpha = 0.35f * (1f - p)),
                radius = ringR,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = (3f * (1f - p) + 0.5f).dp.toPx()),
            )
        }
}

// ---------------------------------------------------------------- Glass search bar

/**
 * Frosted glass capsule for the search box (with a shine on top and a glowing edge).
 * The search screen is inside the recorded app area, so it can't bend what is behind it
 * like the bottom bar — it uses a frosted look instead. Off when "Liquid glass" is off.
 */
@Composable
fun Modifier.kingsSearchCapsule(): Modifier {
    val liquid = rememberPreference(LiquidGlassKey, true).value
    if (!liquid) return this
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val shape = RoundedCornerShape(26.dp)
    val fill = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.6f)
    return this
        .padding(end = 8.dp)
        .height(48.dp)
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    fill.copy(alpha = fill.alpha + if (dark) 0.06f else 0.2f),
                    fill,
                )
            )
        )
        .background(colors.primary.copy(alpha = 0.06f))
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = if (dark) 0.35f else 0.8f),
                    colors.primary.copy(alpha = 0.35f),
                    Color.White.copy(alpha = if (dark) 0.12f else 0.4f),
                )
            ),
            shape = shape,
        )
        .padding(start = 16.dp)
}

// ---------------------------------------------------------------- Lyrics colors

/**
 * Lyrics color: with a Kings Play style, sung words light up in a soft theme color
 * (still bright enough to read on the blurred cover). Classic style keeps the original color.
 */
@Composable
fun kingsLyricsAccent(original: Color): Color {
    val style by rememberEnumPreference(KingsStyleKey, DefaultKingsStyle)
    if (style == KingsStyle.CLASSIC) return original
    val primary = MaterialTheme.colorScheme.primary
    return if (original == Color.White) lerp(Color.White, primary, 0.4f) else primary
}
