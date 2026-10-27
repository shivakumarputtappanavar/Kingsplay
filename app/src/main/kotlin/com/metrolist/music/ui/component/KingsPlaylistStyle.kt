/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * New look for playlist pages:
 *  - big rounded cover with a glow in the cover's colour
 *  - colour wash at the top of the page that fades while you scroll
 *  - header slides slower than the list and fades out (parallax)
 *  - songs slide in softly while scrolling, the playing song is highlighted
 *  - Like · Play · Shuffle buttons
 */

package com.metrolist.music.ui.component

import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.utils.resize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------- cover colour

private val kpColorCache = LruCache<String, Int>(64)

/** Main bright colour of a cover picture (null while loading). */
@Composable
fun rememberKingsPlaylistColor(url: String?): Color? {
    val context = LocalContext.current
    var color by remember(url) { mutableStateOf(url?.let { kpColorCache.get(it) }?.let { Color(it) }) }
    LaunchedEffect(url) {
        if (url == null || color != null) return@LaunchedEffect
        val argb = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url.resize(96, 96))
                    .size(96, 96)
                    .allowHardware(false)
                    .build()
                val bitmap = context.imageLoader.execute(request).image?.toBitmap() ?: return@runCatching null
                val palette = Palette.from(bitmap).maximumColorCount(12).generate()
                (palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.dominantSwatch)?.rgb
            }.getOrNull()
        }
        if (argb != null) {
            kpColorCache.put(url, argb)
            color = Color(argb)
        }
    }
    return color
}

private fun Color.brightEnough(): Color =
    if (luminance() < 0.12f) lerp(this, Color.White, 0.35f) else this

// ---------------------------------------------------------------- page background

/** Colour wash at the top of the page (cover colour). Fades out while you scroll down. */
@Composable
fun KingsPlaylistBackdrop(url: String?, state: LazyListState) {
    val target = rememberKingsPlaylistColor(url)?.brightEnough() ?: Color.Transparent
    val color by animateColorAsState(target, tween(800), label = "kpBackdrop")
    Box(
        Modifier
            .fillMaxWidth()
            .height(560.dp)
            .graphicsLayer {
                alpha = if (state.firstVisibleItemIndex == 0) {
                    (1f - state.firstVisibleItemScrollOffset / 1100f).coerceIn(0f, 1f)
                } else {
                    0f
                }
            }
            .background(
                Brush.verticalGradient(
                    0f to color.copy(alpha = color.alpha * 0.55f),
                    0.55f to color.copy(alpha = color.alpha * 0.18f),
                    1f to Color.Transparent,
                )
            )
    )
}

// ---------------------------------------------------------------- header

/** Big rounded cover with a soft glow in its own colour and a glass shine. */
@Composable
fun KingsPlaylistCover(url: String?, size: Dp = 260.dp) {
    val glow = rememberKingsPlaylistColor(url)?.brightEnough() ?: Color.Transparent
    val animated by animateColorAsState(glow, tween(800), label = "kpCoverGlow")
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier
            .size(size)
            .drawBehind {
                if (animated.alpha < 0.01f) return@drawBehind
                val r = 30.dp.toPx()
                for (i in 1..7) {
                    val grow = i * 4.dp.toPx()
                    drawRoundRect(
                        color = animated.copy(alpha = 0.09f * (8 - i) / 7f),
                        topLeft = Offset(-grow, -grow + 6.dp.toPx()),
                        size = Size(this.size.width + grow * 2, this.size.height + grow * 2),
                        cornerRadius = CornerRadius(r + grow, r + grow),
                    )
                }
            }
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape),
    ) {
        AsyncImage(
            model = url?.resize(1080, 1080),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        0f to Color.White.copy(alpha = 0.18f),
                        0.35f to Color.Transparent,
                        1f to Color.Transparent,
                    )
                )
        )
    }
}

/** "50 songs 3:26:17" in a small glass pill. */
@Composable
fun KingsMetaPill(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
    }
}

/** Soft glow around a rounded cover (used on your own / downloaded / liked playlist pages). */
fun Modifier.kingsCoverRing(color: Color, corner: Dp = 28.dp): Modifier = drawBehind {
    val r = corner.toPx()
    for (i in 1..7) {
        val grow = i * 4.dp.toPx()
        drawRoundRect(
            color = color.copy(alpha = 0.08f * (8 - i) / 7f),
            topLeft = Offset(-grow, -grow + 6.dp.toPx()),
            size = Size(size.width + grow * 2, size.height + grow * 2),
            cornerRadius = CornerRadius(r + grow, r + grow),
        )
    }
}

/** Soft rings of glow around the big Play button. */
fun Modifier.kingsPlayGlow(color: Color): Modifier = drawBehind {
    for (i in 1..5) {
        val grow = i * 4.dp.toPx()
        drawCircle(
            color = color.copy(alpha = 0.10f * (6 - i) / 5f),
            radius = size.minDimension / 2f + grow,
            center = center,
        )
    }
}

// ---------------------------------------------------------------- shuffle

/** Shuffle the playlist: uses YouTube's own shuffle (whole playlist), or shuffles the loaded songs. */
fun kingsShufflePlay(
    playerConnection: PlayerConnection,
    shuffleEndpoint: WatchEndpoint?,
    title: String,
    songs: List<SongItem>,
) {
    if (shuffleEndpoint != null) {
        playerConnection.playQueue(YouTubeQueue(shuffleEndpoint))
    } else if (songs.isNotEmpty()) {
        playerConnection.playQueue(
            ListQueue(title = title, items = songs.shuffled().map { it.toMediaMetadata().toMediaItem() })
        )
    }
}

// ---------------------------------------------------------------- scrolling style

/** Header moves slower than the list, shrinks a little and fades out while you scroll (parallax). */
fun Modifier.kingsHeaderParallax(state: LazyListState): Modifier = graphicsLayer {
    if (state.firstVisibleItemIndex == 0) {
        val offset = state.firstVisibleItemScrollOffset.toFloat()
        val progress = (offset / size.height.coerceAtLeast(1f)).coerceIn(0f, 1f)
        translationY = offset * 0.45f
        alpha = (1f - progress * 1.15f).coerceIn(0f, 1f)
        val scale = 1f - progress * 0.08f
        scaleX = scale
        scaleY = scale
    } else {
        alpha = 0f
    }
}

/** Songs slide up and fade in softly when they come onto the screen. */
@Composable
fun Modifier.kingsRowAppear(): Modifier {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(320, easing = FastOutSlowInEasing)) }
    return this.graphicsLayer {
        val v = anim.value
        alpha = v
        translationY = (1f - v) * 26.dp.toPx()
        val scale = 0.96f + 0.04f * v
        scaleX = scale
        scaleY = scale
    }
}

/** Rounded song row; the playing song gets a soft highlight. */
@Composable
fun Modifier.kingsSongRow(active: Boolean): Modifier {
    val target = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent
    val bg by animateColorAsState(target, tween(400), label = "kpRow")
    return this
        .padding(horizontal = 8.dp, vertical = 2.dp)
        .clip(RoundedCornerShape(18.dp))
        .background(bg)
}
