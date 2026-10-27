/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * "Kings Mixes for today": 8 high-energy ("josh") playlists in many languages,
 * a new set every day, plus 1–2 made from your own top artists.
 * Each mix is filled with matching songs from YouTube Music when you tap it.
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.queues.ListQueue
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

data class KingsMix(
    val title: String,
    val tag: String,
    val query: String,
    val colors: List<Color>,
)

private val MixPalettes = listOf(
    listOf(Color(0xFFFF4D6D), Color(0xFF7A0A2B)),
    listOf(Color(0xFFFFB238), Color(0xFF8A2C02)),
    listOf(Color(0xFF7C4DFF), Color(0xFF1E0A5C)),
    listOf(Color(0xFF00C6A7), Color(0xFF053B44)),
    listOf(Color(0xFF3D8BFF), Color(0xFF0A1F5C)),
    listOf(Color(0xFFFF6FD8), Color(0xFF4A0A5C)),
    listOf(Color(0xFFB6FF3D), Color(0xFF1F4A05)),
    listOf(Color(0xFFFF7A3D), Color(0xFF5C1A0A)),
)

/** Kannada mixes: one of these is on Home every day (a different one each day). */
private val KannadaThemes = listOf(
    "Kannada Josh" to ("Kannada" to "kannada mass hit songs"),
    "Sandalwood Mass" to ("Kannada" to "kannada mass bgm hero songs"),
    "Kannada Party Hits" to ("Kannada" to "kannada party dance songs"),
    "Kannada Workout" to ("Kannada" to "kannada energetic workout songs"),
    "Kannada Folk Beats" to ("Kannada" to "kannada janapada dj songs"),
)

/** Insta trending: songs going viral on reels. One of these is on Home every day. */
private val InstaThemes = listOf(
    "Insta Trending" to ("Viral on reels" to "instagram reels trending songs"),
    "Viral Reels Hindi" to ("Viral on reels" to "hindi viral reels songs trending"),
    "Reels Trending India" to ("Viral on reels" to "trending reels songs india"),
    "Viral Reels Punjabi" to ("Viral on reels" to "punjabi trending reels songs"),
    "Kannada Reels Trending" to ("Viral on reels" to "kannada trending reels songs"),
)

/** Trending josh mixes in Indian languages (most of the daily mixes come from here). */
private val MixThemes = listOf(
    "Trending Hindi Josh" to ("Hindi" to "trending hindi songs high energy"),
    "Bollywood Trending" to ("Hindi" to "latest bollywood trending songs"),
    "Trending Punjabi" to ("Punjabi" to "trending punjabi songs"),
    "Bhangra Blast" to ("Punjabi" to "punjabi bhangra hit songs"),
    "Trending Tamil" to ("Tamil" to "trending tamil songs"),
    "Tamil Mass Beats" to ("Tamil" to "tamil mass kuthu songs"),
    "Trending Telugu" to ("Telugu" to "trending telugu songs"),
    "Telugu Mass Hits" to ("Telugu" to "telugu mass dance songs"),
    "Trending Malayalam" to ("Malayalam" to "trending malayalam songs"),
    "Trending Marathi" to ("Marathi" to "trending marathi dj songs"),
    "Trending Bengali" to ("Bengali" to "trending bengali songs"),
    "Desi Hip-Hop Trending" to ("Hindi · Punjabi" to "trending desi hip hop songs"),
    "Bollywood Workout" to ("Hindi" to "bollywood workout songs"),
    "Road Trip Josh" to ("Hindi" to "road trip songs bollywood"),
    "Party Anthems India" to ("All Indian" to "trending indian party songs"),
)

/** International mixes: at most one of these per day. */
private val WorldThemes = listOf(
    "Trending English Hits" to ("English" to "trending english songs"),
    "Pump Up Anthems" to ("English" to "pump up workout songs"),
    "EDM Festival" to ("English" to "edm festival anthems"),
    "Phonk Drive" to ("Phonk" to "phonk drift songs"),
    "K-Pop Hype" to ("Korean" to "kpop hype songs"),
    "Latin Fuego" to ("Spanish" to "reggaeton party hits"),
)

/** Today's mixes: up to 2 from your top artists + themed ones, 8 in total. Same all day, new tomorrow. */
fun todaysKingsMixes(topArtists: List<String>): List<KingsMix> {
    val day = LocalDate.now().toEpochDay()
    val random = Random(day)
    val personal = topArtists.filter { it.isNotBlank() }.take(2).mapIndexed { i, artist ->
        if (i == 0) {
            KingsMix("$artist & more", "Made for you", "$artist top songs", MixPalettes[(day % MixPalettes.size).toInt()])
        } else {
            KingsMix("$artist Radio", "Made for you", "$artist mix", MixPalettes[((day + 3) % MixPalettes.size).toInt()])
        }
    }
    val kannada = KannadaThemes[(day % KannadaThemes.size).toInt()].let { (title, info) ->
        KingsMix(title, info.first, info.second, listOf(Color(0xFFFFD23F), Color(0xFFC1121F)))
    }
    val insta = InstaThemes[(day % InstaThemes.size).toInt()].let { (title, info) ->
        KingsMix(title, info.first, info.second, listOf(Color(0xFFFEDA75), Color(0xFFD62976), Color(0xFF4F5BD5)))
    }
    val world = WorldThemes[(day % WorldThemes.size).toInt()].let { (title, info) ->
        KingsMix(title, info.first, info.second, MixPalettes[((day + 5) % MixPalettes.size).toInt()])
    }
    val themed = MixThemes.shuffled(random).take(5 - personal.size).mapIndexed { i, (title, info) ->
        KingsMix(title, info.first, info.second, MixPalettes[(i + day.toInt()) % MixPalettes.size])
    }
    return listOf(insta) + personal + kannada + themed + world
}

/** Finds songs for a mix on YouTube Music (about 40 songs, shuffled). */
private suspend fun songsForMix(mix: KingsMix): List<SongItem> {
    val first = YouTube.search(mix.query, YouTube.SearchFilter.FILTER_SONG).getOrNull() ?: return emptyList()
    val songs = first.items.filterIsInstance<SongItem>().toMutableList()
    first.continuation?.let { more ->
        YouTube.searchContinuation(more).getOrNull()?.items?.filterIsInstance<SongItem>()?.let { songs += it }
    }
    return songs.distinctBy { it.id }.shuffled()
}

@Composable
fun KingsDailyMixesSection(
    title: String = "Kings Mixes for today",
    subtitle: String = "Full josh in every language · new mixes every day",
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val scope = rememberCoroutineScope()

    val since = remember { LocalDateTime.now().minusDays(30) }
    val topArtists by remember {
        database.mostPlayedArtists(since, limit = 5, toTimeStamp = LocalDateTime.now())
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val mixes = remember(topArtists.map { it.id }) { todaysKingsMixes(topArtists.map { it.artist.name }) }
    var loadingTitle by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)) {
        Text(
            title,
            fontSize = 21.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 16.dp),
        )
        Text(
            subtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 12.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(mixes, key = { it.title }) { mix ->
                val loading = loadingTitle == mix.title
                Box(
                    modifier = Modifier
                        .size(width = 160.dp, height = 200.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(mix.colors))
                        .drawBehind {
                            // soft light spots for a glowing look
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                                    center = Offset(size.width * 0.85f, size.height * 0.1f),
                                    radius = size.width * 0.8f,
                                ),
                                radius = size.width * 0.8f,
                                center = Offset(size.width * 0.85f, size.height * 0.1f),
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent),
                                    center = Offset(size.width * 0.1f, size.height),
                                    radius = size.width,
                                ),
                                radius = size.width,
                                center = Offset(size.width * 0.1f, size.height),
                            )
                        }
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
                        .clickable(enabled = loadingTitle == null) {
                            loadingTitle = mix.title
                            scope.launch {
                                val songs = runCatching { songsForMix(mix) }.getOrDefault(emptyList())
                                loadingTitle = null
                                if (songs.isEmpty()) {
                                    Toast.makeText(context, "Couldn't load this mix. Check your internet.", Toast.LENGTH_SHORT).show()
                                } else {
                                    playerConnection.playQueue(
                                        ListQueue(title = mix.title, items = songs.map { it.toMediaItem() })
                                    )
                                }
                            }
                        }
                        .padding(14.dp),
                ) {
                    Text(
                        mix.tag.uppercase(),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopStart),
                    )
                    Text(
                        mix.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 3,
                        modifier = Modifier.align(Alignment.BottomStart).padding(end = 40.dp),
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    ) {
                        if (loading) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = mix.colors.last())
                        } else {
                            Icon(
                                painterResource(R.drawable.play),
                                contentDescription = "Play ${mix.title}",
                                tint = mix.colors.last(),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
