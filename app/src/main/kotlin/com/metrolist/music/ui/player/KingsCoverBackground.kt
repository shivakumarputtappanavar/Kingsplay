/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Kings Play player styles:
 *  - NORMAL      : Metrolist's normal player
 *  - ALBUM_COVER : the song's cover fills the whole player screen
 *  - VINYL       : cover sleeve with a spinning record sliding out
 *  - TURNTABLE   : spinning record on a turntable with a moving tonearm
 */

package com.metrolist.music.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import coil3.compose.AsyncImage
import com.metrolist.music.ui.utils.resize

// ---------- Setting ----------
val KingsPlayerStyleKey = stringPreferencesKey("kingsPlayerStyle")

enum class KingsPlayerStyle(val label: String) {
    NORMAL("Normal"),
    ALBUM_COVER("Album cover"),
    VINYL("Vinyl"),
    TURNTABLE("Turntable"),
}

// ---------- 1. Album cover background ----------
@Composable
fun KingsCoverBackground(
    thumbnailUrl: String?,
    alpha: Float,
) {
    AnimatedContent(
        targetState = thumbnailUrl,
        transitionSpec = { fadeIn(tween(700)).togetherWith(fadeOut(tween(700))) },
        label = "kingsCoverBackground",
    ) { url ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
        ) {
            if (url != null) {
                AsyncImage(
                    model = url.resize(1200, 1200),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Dark at the top (status bar), clear in the middle (photo), dark at the bottom (controls)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.00f to Color.Black.copy(alpha = 0.45f),
                            0.18f to Color.Black.copy(alpha = 0.05f),
                            0.45f to Color.Transparent,
                            0.62f to Color.Black.copy(alpha = 0.55f),
                            0.80f to Color.Black.copy(alpha = 0.85f),
                            1.00f to Color.Black.copy(alpha = 0.95f),
                        )
                    )
            )
        }
    }
}

// ---------- Shared: a spinning record ----------
/** Keeps turning while [isPlaying]; stops smoothly where it is when paused. */
@Composable
private fun rememberSpinAngle(isPlaying: Boolean, degreesPerSecond: Float = 40f): () -> Float {
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            var last = withFrameNanos { it }
            while (true) {
                withFrameNanos { now ->
                    angle = (angle + (now - last) / 1_000_000_000f * degreesPerSecond) % 360f
                    last = now
                }
            }
        }
    }
    return { angle }
}

/** A black vinyl record with grooves, a shine, and the song cover as its center label. */
@Composable
private fun SpinningRecord(
    thumbnailUrl: String?,
    angle: () -> Float,
    size: Dp,
    modifier: Modifier = Modifier,
    labelFraction: Float = 0.38f,
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(18.dp, CircleShape, clip = false),
        contentAlignment = Alignment.Center,
    ) {
        // Turning part: disc + grooves + label
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = angle() },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = this.size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF2A2A2E), Color(0xFF111114), Color(0xFF050506)),
                        center = center,
                        radius = r,
                    ),
                    radius = r,
                )
                // Grooves
                var gr = r * 0.97f
                while (gr > r * (labelFraction / 2f + 0.04f)) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.045f),
                        radius = gr,
                        style = Stroke(width = 1f),
                    )
                    gr -= r * 0.028f
                }
                drawCircle(color = Color.Black, radius = r, style = Stroke(width = r * 0.02f))
            }
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl.resize(400, 400),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size * labelFraction)
                        .clip(CircleShape),
                )
            }
            // Center hole
            Box(
                modifier = Modifier
                    .size(size * 0.035f)
                    .clip(CircleShape)
                    .background(Color(0xFF0B0B0D))
            )
        }
        // Still part: light shine on the record (does not turn, looks realistic)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            rotate(degrees = -35f) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        0.00f to Color.Transparent,
                        0.10f to Color.White.copy(alpha = 0.10f),
                        0.20f to Color.Transparent,
                        0.50f to Color.Transparent,
                        0.60f to Color.White.copy(alpha = 0.08f),
                        0.70f to Color.Transparent,
                        1.00f to Color.Transparent,
                        center = center,
                    ),
                    radius = r,
                )
            }
        }
    }
}

// ---------- 2. Vinyl: sleeve + record sliding out ----------
@Composable
fun KingsVinylArt(
    thumbnailUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val angle = rememberSpinAngle(isPlaying)
    // Record slides further out of the sleeve while playing
    val slideOut by animateFloatAsState(
        targetValue = if (isPlaying) 0.48f else 0.22f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 120f),
        label = "vinylSlide",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        // Sleeve size: leave room on the right for the record
        val sleeve = minOf(maxWidth / 1.5f, maxHeight)
        Box(modifier = Modifier.size(width = maxWidth, height = sleeve)) {
            SpinningRecord(
                thumbnailUrl = thumbnailUrl,
                angle = angle,
                size = sleeve * 0.96f,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = sleeve * slideOut + sleeve * 0.02f),
            )
            // Sleeve (album cover) on top
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(sleeve)
                    .shadow(20.dp, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1B1B22)),
            ) {
                AnimatedContent(
                    targetState = thumbnailUrl,
                    transitionSpec = { fadeIn(tween(500)).togetherWith(fadeOut(tween(500))) },
                    label = "vinylSleeve",
                ) { url ->
                    if (url != null) {
                        AsyncImage(
                            model = url.resize(900, 900),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                // Light edge, like printed card
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.12f), Color.Transparent, Color.Black.copy(alpha = 0.18f))
                            )
                        )
                )
            }
        }
    }
}

// ---------- 3. Turntable ----------
@Composable
fun KingsTurntableArt(
    thumbnailUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val angle = rememberSpinAngle(isPlaying, degreesPerSecond = 120f)
    // Tonearm moves onto the record while playing
    val armAngle by animateFloatAsState(
        targetValue = if (isPlaying) 24f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 60f),
        label = "tonearm",
    )

    Box(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .aspectRatio(1f)
            .shadow(24.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF3A3A42), Color(0xFF1C1C22), Color(0xFF101014)))
            ),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val side = maxWidth
            // Platter (slightly bigger than the record)
            Box(
                modifier = Modifier
                    .size(side * 0.84f)
                    .offset(x = side * 0.04f, y = side * 0.08f)
                    .clip(CircleShape)
                    .background(Color(0xFF0C0C0F)),
            )
            SpinningRecord(
                thumbnailUrl = thumbnailUrl,
                angle = angle,
                size = side * 0.80f,
                labelFraction = 0.36f,
                modifier = Modifier.offset(x = side * 0.06f, y = side * 0.10f),
            )
            // Tonearm, pivot at the top-right corner
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pivot = Offset(size.width * 0.88f, size.height * 0.12f)
                rotate(degrees = armAngle, pivot = pivot) {
                    val elbow = Offset(size.width * 0.86f, size.height * 0.62f)
                    val head = Offset(size.width * 0.72f, size.height * 0.80f)
                    drawLine(Color(0xFFD9D9E0), pivot, elbow, strokeWidth = size.width * 0.018f, cap = StrokeCap.Round)
                    drawLine(Color(0xFFD9D9E0), elbow, head, strokeWidth = size.width * 0.018f, cap = StrokeCap.Round)
                    // Head shell
                    drawLine(Color(0xFF8E8E96), head, Offset(head.x - size.width * 0.04f, head.y + size.width * 0.05f), strokeWidth = size.width * 0.05f, cap = StrokeCap.Round)
                }
                // Pivot base (does not move)
                drawCircle(Color(0xFF55555E), radius = size.width * 0.06f, center = pivot)
                drawCircle(Color(0xFFBDBDC6), radius = size.width * 0.028f, center = pivot)
            }
        }
    }
}
