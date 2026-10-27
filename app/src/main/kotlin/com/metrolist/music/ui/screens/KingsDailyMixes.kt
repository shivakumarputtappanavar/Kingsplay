/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Josh Songs (in Explore):
 *  - Insta Trending: songs trending on reels, one playlist that changes once a month
 *  - Today's Best 5: five josh playlists, a new set every day
 *  - All Josh Mix: ALL josh playlists combined into one, new mix every day
 *  - All Josh playlists: each mix on its own (about 150 songs: trending + new + max 10 old)
 *  - Remix playlists: remixes of trending songs
 * Rules: no devotional songs, no folk songs, and songs from yesterday's mixes are not repeated today.
 * Songs come from YouTube Music searches (smart playlists, not a real AI model).
 */

package com.metrolist.music.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.LocalNavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

// ---------------------------------------------------------------- themes

/**
 * One josh playlist. Songs come from 3 searches:
 *  trending = most of the playlist, latest = new songs ({year} = this year),
 *  old = only up to 10 older songs.
 */
data class JoshTheme(
    val title: String,
    val tag: String,
    val trending: String,
    val latest: String,
    val old: String,
    val remix: Boolean = false,
)

/** Josh playlists (no folk, no devotional). */
private val JoshThemes = listOf(
    JoshTheme("Trending Hindi Josh", "Hindi", "trending hindi songs high energy", "new hindi songs {year}", "old hindi party songs"),
    JoshTheme("Bollywood Trending", "Hindi", "latest bollywood trending songs", "new bollywood movie songs {year}", "2000s bollywood hit songs"),
    JoshTheme("Bollywood Workout", "Hindi", "bollywood workout songs", "new bollywood gym songs {year}", "old bollywood energetic songs"),
    JoshTheme("Road Trip Josh", "Hindi", "road trip songs bollywood", "new hindi road trip songs {year}", "old hindi road trip songs"),
    JoshTheme("Trending Punjabi", "Punjabi", "trending punjabi songs", "new punjabi songs {year}", "old punjabi hit songs"),
    JoshTheme("Bhangra Blast", "Punjabi", "punjabi bhangra hit songs", "new bhangra songs {year}", "old bhangra songs"),
    JoshTheme("Desi Hip-Hop", "Hindi · Punjabi", "trending desi hip hop songs", "new desi hip hop {year}", "old desi hip hop songs"),
    JoshTheme("Kannada Josh", "Kannada", "kannada mass hit songs", "new kannada songs {year}", "old kannada hit songs"),
    JoshTheme("Sandalwood Mass", "Kannada", "kannada mass hero songs", "new kannada movie mass songs {year}", "old kannada mass songs"),
    JoshTheme("Kannada Party Hits", "Kannada", "kannada party dance songs", "new kannada dance songs {year}", "old kannada dance songs"),
    JoshTheme("Trending Tamil", "Tamil", "trending tamil songs", "new tamil songs {year}", "old tamil hit songs"),
    JoshTheme("Tamil Mass Beats", "Tamil", "tamil mass songs anirudh", "new tamil mass songs {year}", "old tamil mass songs"),
    JoshTheme("Trending Telugu", "Telugu", "trending telugu songs", "new telugu songs {year}", "old telugu hit songs"),
    JoshTheme("Telugu Mass Hits", "Telugu", "telugu mass dance songs", "new telugu mass songs {year}", "old telugu mass songs"),
    JoshTheme("Trending Malayalam", "Malayalam", "trending malayalam songs", "new malayalam songs {year}", "old malayalam hit songs"),
    JoshTheme("Trending Marathi", "Marathi", "trending marathi dj songs", "new marathi songs {year}", "old marathi hit songs"),
    JoshTheme("Party Anthems India", "All Indian", "trending indian party songs", "new party songs india {year}", "old bollywood party songs"),
    JoshTheme("Pump Up Anthems", "English", "pump up workout songs", "new workout songs {year}", "2010s pump up songs"),
    JoshTheme("Trending English Hits", "English", "trending english songs", "new english songs {year}", "2010s english hits"),
)

/** Remix playlists: remixes of trending songs. */
private val RemixThemes = listOf(
    JoshTheme("Trending Reels Remix", "Remix", "trending reels remix songs", "new reels remix songs {year}", "old songs remix", remix = true),
    JoshTheme("Bollywood Remix", "Hindi Remix", "bollywood remix songs trending", "new bollywood remix {year}", "old bollywood remix songs", remix = true),
    JoshTheme("Bollywood Party Mashup", "Hindi Remix", "bollywood party mashup", "new bollywood mashup {year}", "old bollywood mashup", remix = true),
    JoshTheme("Punjabi Remix", "Punjabi Remix", "punjabi remix songs trending", "new punjabi remix {year}", "old punjabi remix songs", remix = true),
    JoshTheme("Kannada DJ Remix", "Kannada Remix", "kannada dj remix songs", "new kannada dj remix {year}", "old kannada dj remix songs", remix = true),
    JoshTheme("Tamil Remix", "Tamil Remix", "tamil remix songs trending", "new tamil remix {year}", "old tamil remix songs", remix = true),
    JoshTheme("Telugu DJ Remix", "Telugu Remix", "telugu dj remix songs", "new telugu dj remix {year}", "old telugu dj remix songs", remix = true),
    JoshTheme("English Remix", "English Remix", "trending english remix songs", "new english remix songs {year}", "old english songs remix", remix = true),
)

/** Every playlist (josh + remix). */
private val AllThemes = JoshThemes + RemixThemes

/** Insta trending searches; one is used per month. */
private val InstaQueries = listOf(
    "instagram reels trending songs",
    "trending reels songs india",
    "viral reels songs hindi",
    "instagram viral songs",
    "trending reels songs punjabi hindi",
    "reels trending songs this month",
)

/**
 * Songs are skipped if the song name, singer or album has any of these words.
 * Devotional and folk, in English letters and in Indian scripts.
 */
private val BlockedWords = listOf(
    // devotional
    "devotional", "bhakti", "bhakthi", "bhakthigeethe", "bhakti geete", "bhakti sagar", "bhajan", "bhajana",
    "aarti", "aarthi", "aarathi", "arati", "mantra", "stotra", "stotram", "stuti", "chalisa", "suprabhatam",
    "kirtan", "keertan", "keerthane", "keerthana", "sankirtan", "namavali", "shloka", "sloka", "temple",
    "ayyappa", "god song", "gospel", "worship", "hymn", "jesus", "christian", "praise and",
    "jai shree", "jai shri", "jai mata", "mata rani", "maa durga", "durga", "navratri special",
    "hanuman", "ganesha", "ganapati", "ganpati", "sai baba", "murugan", "amman", "venkateswara",
    "mahadev", "bholenath", "bhole baba", "bol bam", "kanwar", "har har", "namah shivaya", "om namah",
    "hare krishna", "hare rama", "radhe radhe", "shyam baba", "khatu shyam", "gurbani", "shabad",
    "naat", "hamd", "islamic", "dasara pada", "dasa pada", "amritwani", "jaap", "paath", "satsang",
    "भजन", "आरती", "भक्ति", "भक्ती", "चालीसा", "मंत्र", "ಭಕ್ತಿ", "ಭಜನೆ", "ದೇವರ", "பக்தி", "భక్తి", "ഭക്തി",
    // folk
    "folk", "janapada", "jaanapada", "lok geet", "lokgeet", "lavani", "gondhal", "bihu", "baul",
    "garhwali", "kumaoni", "pahari", "koli geet", "dollu kunitha", "kolata", "lok sangeet",
    "लोकगीत", "लोक गीत", "ಜಾನಪದ",
)

/** Very long tracks (bhajans, jukeboxes) are skipped too. */
private const val MaxSongSeconds = 9 * 60

private fun isBlockedText(text: String): Boolean {
    val lower = text.lowercase()
    return BlockedWords.any { it in lower }
}

private fun SongItem.isJoshAllowed(): Boolean {
    if ((duration ?: 0) > MaxSongSeconds) return false
    return !isBlockedText(title + " " + artists.joinToString(" ") { it.name } + " " + (album?.name ?: ""))
}

/** Final check just before playing (also for playlists saved earlier on the phone). */
private fun MediaMetadata.isJoshAllowed(): Boolean {
    if (duration > MaxSongSeconds) return false
    return !isBlockedText(title + " " + artists.joinToString(" ") { it.name })
}

private val Palettes = listOf(
    listOf(Color(0xFFFF4D6D), Color(0xFF7A0A2B)),
    listOf(Color(0xFFFFB238), Color(0xFF8A2C02)),
    listOf(Color(0xFF7C4DFF), Color(0xFF1E0A5C)),
    listOf(Color(0xFF00C6A7), Color(0xFF053B44)),
    listOf(Color(0xFF3D8BFF), Color(0xFF0A1F5C)),
    listOf(Color(0xFFFF6FD8), Color(0xFF4A0A5C)),
    listOf(Color(0xFFFF7A3D), Color(0xFF5C1A0A)),
)
private val InstaColors = listOf(Color(0xFFFEDA75), Color(0xFFD62976), Color(0xFF4F5BD5))
private val AllJoshColors = listOf(Color(0xFFFFD23F), Color(0xFFFF4D6D), Color(0xFF7C4DFF))

private fun paletteFor(i: Int) = Palettes[i % Palettes.size]

/** Today's Best 5: changes every day. */
fun todaysBestJosh(): List<JoshTheme> =
    AllThemes.shuffled(Random(LocalDate.now().toEpochDay())).take(5)

// ---------------------------------------------------------------- no-repeat memory

private fun prefs(context: Context) = context.getSharedPreferences("kings_josh", Context.MODE_PRIVATE)

private fun yesterdaysSongs(context: Context): Set<String> {
    val key = "day_" + (LocalDate.now().toEpochDay() - 1)
    return prefs(context).getStringSet(key, emptySet()).orEmpty()
}

private fun rememberToday(context: Context, ids: List<String>) {
    val today = LocalDate.now().toEpochDay()
    val p = prefs(context)
    val key = "day_$today"
    val all = p.getStringSet(key, emptySet()).orEmpty() + ids
    val editor = p.edit().putStringSet(key, all)
    // keep only today and yesterday
    p.all.keys.filter { it.startsWith("day_") && it != key && it != "day_${today - 1}" }.forEach { editor.remove(it) }
    editor.apply()
}

// ---------------------------------------------------------------- song loading

private suspend fun searchSongs(query: String, pages: Int): List<SongItem> {
    val first = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull() ?: return emptyList()
    val songs = first.items.filterIsInstance<SongItem>().toMutableList()
    var next = first.continuation
    var page = 1
    while (next != null && page < pages) {
        val more = YouTube.searchContinuation(next).getOrNull() ?: break
        songs += more.items.filterIsInstance<SongItem>()
        next = more.continuation
        page++
    }
    return songs
}

/**
 * Puts songs that were NOT in yesterday's mixes first. Only if there are not enough,
 * yesterday's songs fill the rest, so every playlist is full.
 */
private fun pickFresh(songs: List<SongItem>, skip: Set<String>, count: Int): List<SongItem> {
    val fresh = songs.filter { it.id !in skip }
    return (fresh + songs.filter { it.id in skip }).take(count)
}

/** How many songs a playlist has: about 140 trending + new, plus up to 10 old ones. */
private const val MainSongs = 142
private const val OldSongs = 10

private fun String.withYear() = replace("{year}", LocalDate.now().year.toString())

/** One josh playlist: trending + new songs, max 10 old songs, no songs from yesterday first. */
private suspend fun loadTheme(context: Context, theme: JoshTheme): List<MediaMetadata> = withContext(Dispatchers.IO) {
    val skip = yesterdaysSongs(context)
    val (trending, latest, old) = coroutineScope {
        listOf(
            async { runCatching { searchSongs(theme.trending, pages = 6) }.getOrDefault(emptyList()) },
            async { runCatching { searchSongs(theme.latest.withYear(), pages = 6) }.getOrDefault(emptyList()) },
            async { runCatching { searchSongs(theme.old, pages = 1) }.getOrDefault(emptyList()) },
        ).awaitAll()
    }
    // trending and new songs, taken in turns
    val mainFound = mutableListOf<SongItem>()
    for (i in 0 until maxOf(trending.size, latest.size)) {
        trending.getOrNull(i)?.let { mainFound += it }
        latest.getOrNull(i)?.let { mainFound += it }
    }
    val main = pickFresh(mainFound.filter { it.isJoshAllowed() }.distinctBy { it.id }, skip, MainSongs)
    val mainIds = main.map { it.id }.toSet()
    val oldOnes = pickFresh(
        old.filter { it.isJoshAllowed() && it.id !in mainIds }.distinctBy { it.id }.shuffled(),
        skip,
        OldSongs,
    )
    val songs = (main + oldOnes).shuffled()
    rememberToday(context, songs.map { it.id })
    songs.map { it.toMetadata() }
}

/**
 * All Josh Mix: ALL josh playlists combined into one big playlist.
 * Made once a day and kept the same all day; tomorrow a new mix is made
 * (new order, and songs from yesterday are skipped).
 */
private suspend fun loadAllJosh(context: Context): List<MediaMetadata> = withContext(Dispatchers.IO) {
    val day = LocalDate.now().toEpochDay()
    val key = "alljosh_v2_$day"
    loadSaved(context, key)?.let { return@withContext it }

    val skip = yesterdaysSongs(context)
    val random = Random(day)
    val lists = coroutineScope {
        AllThemes.map { theme ->
            async { runCatching { searchSongs(theme.trending, pages = 1) }.getOrDefault(emptyList()).shuffled(random) }
        }.awaitAll()
    }.shuffled(random)
    // take songs in turns from each playlist so the mix has every flavour
    val mixed = mutableListOf<SongItem>()
    val max = lists.maxOfOrNull { it.size } ?: 0
    for (i in 0 until max) lists.forEach { list -> list.getOrNull(i)?.let { mixed += it } }
    val songs = pickFresh(mixed.filter { it.isJoshAllowed() }.distinctBy { it.id }, skip, 220)
    if (songs.isNotEmpty()) {
        rememberToday(context, songs.map { it.id })
        save(context, key, "alljosh_", songs)
    }
    songs.map { it.toMetadata() }
}

/** Reads a saved playlist (song list) from the phone. */
private fun loadSaved(context: Context, key: String): List<MediaMetadata>? {
    val saved = prefs(context).getString(key, null) ?: return null
    val arr = runCatching { JSONArray(saved) }.getOrNull() ?: return null
    val list = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        MediaMetadata(
            id = o.getString("id"),
            title = o.getString("title"),
            artists = listOf(MediaMetadata.Artist(id = null, name = o.optString("artist"))),
            duration = o.optInt("duration", -1),
            thumbnailUrl = o.optString("thumb").ifBlank { null },
        )
    }
    return list.ifEmpty { null }
}

/** Saves a playlist on the phone and removes old ones with the same prefix. */
private fun save(context: Context, key: String, prefix: String, songs: List<SongItem>) {
    val arr = JSONArray()
    songs.forEach { s ->
        arr.put(
            JSONObject()
                .put("id", s.id)
                .put("title", s.title)
                .put("artist", s.artists.joinToString(", ") { it.name })
                .put("duration", s.duration ?: -1)
                .put("thumb", s.thumbnail)
        )
    }
    val p = prefs(context)
    val editor = p.edit()
    p.all.keys.filter { it.startsWith(prefix) && it != key }.forEach { editor.remove(it) }
    editor.putString(key, arr.toString()).apply()
}

/** Insta Trending: fetched once per month and kept the same all month. */
private suspend fun loadInstaMonth(context: Context): List<MediaMetadata> = withContext(Dispatchers.IO) {
    val month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))
    val key = "insta_v2_$month"
    loadSaved(context, key)?.let { return@withContext it }
    val monthIndex = LocalDate.now().let { it.year * 12 + it.monthValue }
    val query = InstaQueries[monthIndex % InstaQueries.size]
    val songs = searchSongs(query, pages = 7)
        .filter { it.isJoshAllowed() }
        .distinctBy { it.id }
        .take(130)
    if (songs.isNotEmpty()) save(context, key, "insta_", songs)
    songs.map { it.toMetadata() }
}

private fun SongItem.toMetadata() = MediaMetadata(
    id = id,
    title = title,
    artists = artists.map { MediaMetadata.Artist(id = it.id, name = it.name) },
    duration = duration ?: -1,
    thumbnailUrl = thumbnail,
)

// ---------------------------------------------------------------- Explore banner

/** Old Home section name, kept so older Home code still builds. Josh mixes now live in Explore only. */
@Composable
fun KingsDailyMixesSection(title: String = "", subtitle: String = "") {
}

/** Big "Josh Songs" card at the top of Explore. Opens the Josh Songs page. */
@Composable
fun KingsJoshBanner() {
    val navController = LocalNavController.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(AllJoshColors))
            .glowSpots()
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .clickable { navController.navigate("kings_josh") }
            .padding(18.dp),
    ) {
        Column(Modifier.align(Alignment.CenterStart)) {
            Text("JOSH SONGS", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("Full josh playlists", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Insta trending · Today's best 5 · All josh mix", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        }
        PlayDot(Modifier.align(Alignment.CenterEnd), AllJoshColors.last(), loading = false)
    }
}

// ---------------------------------------------------------------- Josh Songs page

@Composable
fun KingsJoshScreen(navController: NavController) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf<String?>(null) }
    val best5 = remember { todaysBestJosh() }
    val monthName = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM")) }

    fun play(id: String, title: String, loader: suspend () -> List<MediaMetadata>) {
        if (loading != null) return
        loading = id
        scope.launch {
            // final safety check + a new random order on every tap
            val songs = runCatching { loader() }.getOrDefault(emptyList())
                .filter { it.isJoshAllowed() }
                .shuffled()
            loading = null
            if (songs.isEmpty()) {
                Toast.makeText(context, "Couldn't load this playlist. Check your internet.", Toast.LENGTH_SHORT).show()
            } else {
                playerConnection?.playQueue(ListQueue(title = title, items = songs.map { it.toMediaItem() }))
            }
        }
    }

    LazyColumn(
        contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues(),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "header") {
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                    Text("Josh Songs", fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
                Text(
                    "Josh v8 · ~150 songs each · shuffle on every tap · no devotional or folk",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 18.dp, bottom = 8.dp),
                )
            }
        }

        item(key = "insta") {
            WideCard(
                tag = "VIRAL ON REELS · $monthName",
                title = "Insta Trending",
                subtitle = "Songs trending on reels · changes once a month",
                colors = InstaColors,
                loading = loading == "insta",
            ) { play("insta", "Insta Trending · $monthName") { loadInstaMonth(context) } }
        }

        item(key = "best_title") { SectionTitle("Today's Best 5", "Changes every day") }
        item(key = "best") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(best5.size) { i ->
                    val theme = best5[i]
                    ThemeCard(theme, paletteFor(i), loading == "best_${theme.title}", width = 160.dp, rank = i + 1) {
                        play("best_${theme.title}", theme.title) { loadTheme(context, theme) }
                    }
                }
            }
        }

        item(key = "all_title") { SectionTitle("All Josh playlists", "All in one, or each mix on its own") }
        item(key = "all") {
            WideCard(
                tag = "ALL ${AllThemes.size} PLAYLISTS IN ONE",
                title = "All Josh Mix",
                subtitle = "Every josh + remix playlist combined · new mix every day",
                colors = AllJoshColors,
                loading = loading == "all",
            ) { play("all", "All Josh Mix") { loadAllJosh(context) } }
        }
        listOf("josh" to JoshThemes, "remix" to RemixThemes).forEach { (group, themes) ->
            if (group == "remix") {
                item(key = "remix_title") { SectionTitle("Remix playlists", "Remixes of trending songs") }
            }
            themes.chunked(2).forEachIndexed { rowIndex, row ->
                item(key = "${group}_row_$rowIndex") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        row.forEachIndexed { j, theme ->
                            Box(Modifier.weight(1f)) {
                                val color = paletteFor(rowIndex * 2 + j + if (group == "remix") 1 else 3)
                                ThemeCard(theme, color, loading == "t_${theme.title}", width = null) {
                                    play("t_${theme.title}", theme.title) { loadTheme(context, theme) }
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item(key = "end") { Spacer(Modifier.height(24.dp)) }
    }
}

// ---------------------------------------------------------------- pieces

private fun Modifier.glowSpots(): Modifier = drawBehind {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
            center = Offset(size.width * 0.85f, size.height * 0.1f),
            radius = size.minDimension,
        ),
        radius = size.minDimension,
        center = Offset(size.width * 0.85f, size.height * 0.1f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent),
            center = Offset(size.width * 0.1f, size.height),
            radius = size.minDimension * 1.2f,
        ),
        radius = size.minDimension * 1.2f,
        center = Offset(size.width * 0.1f, size.height),
    )
}

@Composable
private fun PlayDot(modifier: Modifier, tint: Color, loading: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(42.dp).clip(CircleShape).background(Color.White),
    ) {
        if (loading) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = tint)
        } else {
            Icon(painterResource(R.drawable.play), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(start = 16.dp, top = 18.dp, bottom = 10.dp)) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WideCard(
    tag: String,
    title: String,
    subtitle: String,
    colors: List<Color>,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(colors))
            .glowSpots()
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Column(Modifier.align(Alignment.CenterStart).padding(end = 56.dp)) {
            Text(tag, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, maxLines = 2)
        }
        PlayDot(Modifier.align(Alignment.CenterEnd), colors.last(), loading)
    }
}

@Composable
private fun ThemeCard(
    theme: JoshTheme,
    colors: List<Color>,
    loading: Boolean,
    width: Dp?,
    rank: Int? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .then(if (width != null) Modifier.size(width = width, height = 190.dp) else Modifier.fillMaxWidth().height(170.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(colors))
            .glowSpots()
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(
            (if (rank != null) "#$rank  " else "") + theme.tag.uppercase(),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopStart),
        )
        Text(
            theme.title,
            color = Color.White,
            fontSize = 21.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Black,
            maxLines = 3,
            modifier = Modifier.align(Alignment.BottomStart).padding(end = 46.dp),
        )
        PlayDot(Modifier.align(Alignment.BottomEnd), colors.last(), loading)
    }
}
