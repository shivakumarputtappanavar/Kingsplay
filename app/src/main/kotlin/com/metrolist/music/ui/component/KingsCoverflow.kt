/**
 * Kings Play - 3D cover carousel panel for the Home screen (like the picture):
 * tilted glass cards.
 * Clean dark panel (no photo behind the cards). Drag it any time; fast flicks slide several cards.
 * A soft tick plays while you slide.
 * Recents: follows the song that is playing (stops on it, moves when the song changes).
 * Other panels: move by themselves every 3 seconds; they pause while you touch them.
 */

package com.metrolist.music.ui.component

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.queues.ListQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/** How much the side cards turn. If the tilt looks the wrong way round, change 38f to -38f. */
private const val KG_TILT = 38f

/** Panels that move by themselves do it this often (the Recents panel follows the playing song instead). */
private const val KG_AUTO_MS = 3000L

/** Soft tick while sliding. Set to false to turn the sound off. */
private const val KG_SOUND = true

/** One card of a carousel panel. */
class KgCard(
    val key: String,
    val thumbnail: String?,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

/** A very short soft click, made in code (no sound file needed). */
private object KgTick {
    private val track: AudioTrack? by lazy { runCatching { build() }.getOrNull() }

    private fun build(): AudioTrack {
        val rate = 22050
        val count = (rate * 0.04f).toInt()
        val data = ShortArray(count) { i ->
            val t = i / rate.toFloat()
            (sin(2.0 * PI * 1500.0 * t) * exp(-t * 120.0) * 9000.0).toInt().toShort()
        }
        val audio = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(count * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        audio.write(data, 0, count)
        return audio
    }

    fun play() {
        track?.let {
            runCatching {
                it.stop()
                it.reloadStaticData()
                it.play()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KingsCarouselPanel(
    cards: List<KgCard>,
    modifier: Modifier = Modifier,
    title: String? = null,
    playingKey: String? = null,
    autoMove: Boolean = true,
) {
    val n = cards.size
    if (n < 1) return

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val infinite = n >= 3
    val virtualCount = if (infinite) 100_000 else n
    val startPage = if (infinite) virtualCount / 2 - (virtualCount / 2) % n else 0
    val pagerState = rememberPagerState(initialPage = startPage) { virtualCount }
    val dragged by pagerState.interactionSource.collectIsDraggedAsState()
    val follow = playingKey != null && cards.any { it.key == playingKey }

    // remember when the finger last touched this panel (sound only for slides made by the user)
    var lastTouch by remember { mutableLongStateOf(0L) }
    LaunchedEffect(dragged) {
        if (dragged) {
            lastTouch = Long.MAX_VALUE
        } else if (lastTouch == Long.MAX_VALUE) {
            lastTouch = System.currentTimeMillis()
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.drop(1).collect {
            val byUser = lastTouch == Long.MAX_VALUE || System.currentTimeMillis() - lastTouch < 1500
            if (KG_SOUND && byUser) launch(Dispatchers.Default) { KgTick.play() }
        }
    }

    // by itself every 5 seconds (not for the panel that follows the playing song)
    LaunchedEffect(dragged, follow, autoMove) {
        if (autoMove && infinite && !follow && !dragged) {
            while (true) {
                delay(KG_AUTO_MS)
                pagerState.animateScrollToPage(
                    pagerState.currentPage + 1,
                    animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    // follow the song that is playing; if the user slides away, come back after 5 seconds
    var lastHandled by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(dragged, playingKey, follow) {
        if (!follow || dragged) return@LaunchedEffect
        val idx = cards.indexOfFirst { it.key == playingKey }
        if (idx < 0) return@LaunchedEffect
        val songChanged = playingKey != lastHandled
        if (!songChanged) delay(KG_AUTO_MS)
        lastHandled = playingKey
        val cur = pagerState.currentPage
        val delta = if (infinite) {
            val diff = ((idx - cur % n) + n) % n
            if (diff > n / 2) diff - n else diff
        } else {
            idx - cur
        }
        if (delta != 0) {
            if (songChanged && KG_SOUND) scope.launch(Dispatchers.Default) { KgTick.play() }
            pagerState.animateScrollToPage(
                cur + delta,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            )
        }
    }

    val cardWidth = 210.dp
    val sidePadding = ((LocalConfiguration.current.screenWidthDp.dp - 20.dp - cardWidth) / 2).coerceAtLeast(0.dp)
    val cardShape = RoundedCornerShape(22.dp)
    val panelShape = RoundedCornerShape(30.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .clip(panelShape)
                .background(Color(0xFF14121F)),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 20.dp),
                contentPadding = PaddingValues(horizontal = sidePadding),
                pageSize = PageSize.Fixed(cardWidth),
                beyondViewportPageCount = 1,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    pagerSnapDistance = PagerSnapDistance.atMost(8),
                    snapAnimationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 2200f),
                ),
            ) { page ->
                val card = cards[page % n]

                Column(
                    modifier = Modifier
                        .width(cardWidth)
                        .graphicsLayer {
                            // read while drawing, so sliding does not recompose the cards
                            val offset = (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction
                            val shown = abs(offset).coerceAtMost(3f)
                            cameraDistance = 14f * density
                            rotationY = (-offset * KG_TILT).coerceIn(-55f, 55f)
                            val s = 1f - shown * 0.08f
                            scaleX = s
                            scaleY = s
                            translationX = -offset * 52.dp.toPx()
                            alpha = (1f - shown * 0.2f).coerceIn(0f, 1f)
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                            shape = cardShape
                            clip = true
                        }
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.35f), cardShape)
                        .clickable {
                            if (pagerState.currentPage == page) {
                                card.onClick()
                            } else {
                                scope.launch { pagerState.animateScrollToPage(page) }
                            }
                        }
                        .padding(8.dp),
                ) {
                    val request = remember(card.thumbnail) {
                        ImageRequest.Builder(context)
                            .data(card.thumbnail)
                            .size(420, 420)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = card.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Song version (kept so older Home code still compiles). */
@Composable
fun KingsCoverflow(songs: List<Song>, modifier: Modifier = Modifier) {
    KingsSongsPanel(songs = songs, title = null, playingKey = null, modifier = modifier)
}

@Composable
private fun KingsSongsPanel(
    songs: List<Song>,
    title: String?,
    playingKey: String?,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current
    val cards = remember(songs) {
        songs.mapIndexed { index, song ->
            KgCard(
                key = song.id,
                thumbnail = song.song.thumbnailUrl,
                title = song.song.title,
                subtitle = song.artists.joinToString(", ") { it.name },
                onClick = {
                    // same call the Recents section uses
                    playerConnection?.playQueue(
                        ListQueue(
                            title = title ?: "Songs",
                            items = songs.map { it.toMediaItem() },
                            startIndex = index,
                        )
                    )
                },
            )
        }
    }
    KingsCarouselPanel(cards = cards, modifier = modifier, title = title, playingKey = playingKey)
}

/**
 * Recents panel for Home: songs from your history (the same ones the old Recents row showed).
 * It stops on the song that is playing and moves to the next card when the song changes.
 */
@Composable
fun KingsCoverflowHome(modifier: Modifier = Modifier) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val today = remember { LocalDate.now() }
    val thisMonday = remember { today.with(DayOfWeek.MONDAY) }
    val events by remember {
        database.historyEvents(
            tomorrowStart = today.plusDays(1).atStartOfDay(),
            todayStart = today.atStartOfDay(),
            yesterdayStart = today.minusDays(1).atStartOfDay(),
            thisMondayStart = thisMonday.atStartOfDay(),
            lastMondayStart = thisMonday.minusDays(7).atStartOfDay(),
        )
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val songs = remember(events) { events.map { it.song }.distinctBy { it.id }.take(15) }
    if (songs.isEmpty()) return

    // id of the song that is playing now
    var playingId by remember { mutableStateOf(playerConnection?.player?.currentMediaItem?.mediaId) }
    DisposableEffect(playerConnection) {
        val player = playerConnection?.player
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                playingId = mediaItem?.mediaId
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }

    KingsSongsPanel(songs = songs, title = "Recents", playingKey = playingId, modifier = modifier)
}