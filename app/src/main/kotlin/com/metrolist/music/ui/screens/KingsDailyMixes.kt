/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Josh Songs (in Explore):
 *  - Insta Trending: songs trending on reels, one playlist that changes once a month
 *  - Today's Best 5: five josh playlists, a new set every day
 *  - All Josh Mix: ALL josh playlists combined into one, new mix every day
 *  - All Josh playlists: each mix on its own (about 150 songs: trending + new + max 10 old)
 *  - Mashup Mix: ONE playlist of 30 mashups a day, like Shape of You x Until I Found You
 *  - BGM Music: 30 film BGMs a day from every language
 *  - All DJ Music: DJ songs from every language in one playlist
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.LocalNavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.utils.resize
import coil3.compose.AsyncImage
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
    val languageMix: Boolean = false,
    /** more searches (used for mashups, so there are enough songs) */
    val extra: List<String> = emptyList(),
    /** love / romance playlist (60 songs a day) */
    val love: Boolean = false,
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

/**
 * Language Mix playlists: MASHUPS of two songs in two languages in one track,
 * like "Let Me Down Slowly x Main Dhoondne Ko Zamaane Mein".
 * Only songs whose name shows a mashup ("x", "×", "vs", "mashup") are kept.
 */
private fun mix(title: String, tag: String, a: String, b: String) = JoshTheme(
    title = title,
    tag = tag,
    trending = "$a $b mashup",
    latest = "new $a $b mashup {year}",
    old = "old $a $b mashup",
    languageMix = true,
    extra = listOf("$b x $a mashup", "$a x $b mashup songs", "$a $b mashup lofi", "$b $a mashup reels"),
)

private val MixThemes = listOf(
    mix("Hindi × English", "Hindi English", "hindi", "english").let {
        it.copy(extra = it.extra + listOf("let me down slowly x main dhoondne ko zamaane mein", "english x hindi mashup reels"))
    },
    mix("Punjabi × English", "Punjabi English", "punjabi", "english"),
    mix("Kannada × English", "Kannada English", "kannada", "english"),
    mix("Tamil × English", "Tamil English", "tamil", "english"),
    mix("Telugu × English", "Telugu English", "telugu", "english"),
    mix("Malayalam × English", "Malayalam English", "malayalam", "english"),
    mix("Hindi × Punjabi", "Hindi Punjabi", "hindi", "punjabi"),
    mix("Bollywood × English", "Desi English", "bollywood", "english"),
)

/** True if the song name looks like a mashup of two songs (e.g. "Song A x Song B"). */
private fun SongItem.isMashup(): Boolean {
    val name = " " + title.lowercase() + " "
    return " x " in name || "×" in name || " vs " in name || " vs. " in name ||
        "mashup" in name || "mash up" in name || "mash-up" in name
}

/** All DJ Music: DJ songs from every language (one search per language). */
private val DjQueries = listOf(
    "hindi dj songs", "bollywood dj remix songs", "punjabi dj songs", "kannada dj songs", "tamil dj songs",
    "telugu dj songs", "malayalam dj songs", "marathi dj songs", "bengali dj songs", "gujarati dj songs",
    "english edm dj songs",
)

/** Every josh playlist (mashups have their own single playlist: Mashup Mix). */
private val AllThemes = JoshThemes

/** Love Songs: romantic songs only, one playlist per language, 60 songs a day. */
private fun loveTheme(title: String, lang: String, word: String = lang.lowercase()) = JoshTheme(
    title = title,
    tag = lang,
    trending = "$word romantic songs",
    latest = "new $word love songs {year}",
    old = "old $word romantic songs",
    extra = listOf("$word love songs hits", "$word romantic melody songs"),
    love = true,
)

private val LoveThemes = listOf(
    loveTheme("Hindi Love Songs", "Hindi"),
    loveTheme("Kannada Love Songs", "Kannada"),
    loveTheme("Tamil Love Songs", "Tamil"),
    loveTheme("Telugu Love Songs", "Telugu"),
    loveTheme("Malayalam Love Songs", "Malayalam"),
    loveTheme("Punjabi Love Songs", "Punjabi"),
    loveTheme("Marathi Love Songs", "Marathi"),
    loveTheme("English Love Songs", "English"),
)

// ---------------------------------------------------------------- one language only

/** Words and scripts that show a song's language. */
private val LanguageWords = mapOf(
    "Hindi" to listOf("hindi", "bollywood"),
    "Punjabi" to listOf("punjabi", "bhangra"),
    "Kannada" to listOf("kannada", "sandalwood"),
    "Tamil" to listOf("tamil", "kollywood"),
    "Telugu" to listOf("telugu", "tollywood"),
    "Malayalam" to listOf("malayalam", "mollywood"),
    "Marathi" to listOf("marathi"),
    "English" to listOf("english"),
    "Bengali" to listOf("bengali", "bangla"),
    "Gujarati" to listOf("gujarati"),
    "Bhojpuri" to listOf("bhojpuri"),
)

/** Unicode script ranges of each language (Hindi and Marathi share Devanagari). */
private val LanguageScripts = mapOf(
    "Hindi" to 0x0900..0x097F,
    "Marathi" to 0x0900..0x097F,
    "Punjabi" to 0x0A00..0x0A7F,
    "Gujarati" to 0x0A80..0x0AFF,
    "Bengali" to 0x0980..0x09FF,
    "Tamil" to 0x0B80..0x0BFF,
    "Telugu" to 0x0C00..0x0C7F,
    "Kannada" to 0x0C80..0x0CFF,
    "Malayalam" to 0x0D00..0x0D7F,
)

/** Singers / composers who work almost only in one language (exact names, to catch songs whose name shows no language). */
private val LanguageArtists = mapOf(
    "Punjabi" to setOf("diljit dosanjh", "sidhu moose wala", "karan aujla", "ap dhillon", "shubh", "ammy virk", "jassie gill", "parmish verma", "gurnam bhullar", "satinder sartaaj"),
    "Tamil" to setOf("anirudh ravichander", "sid sriram", "yuvan shankar raja", "harris jayaraj", "g. v. prakash kumar", "hiphop tamizha", "d. imman"),
    "Telugu" to setOf("devi sri prasad", "thaman s", "s. thaman", "mangli", "rahul sipligunj", "m. m. keeravaani"),
    "Kannada" to setOf("arjun janya", "v. harikrishna", "vijay prakash", "ravi basrur", "chandan shetty", "ajaneesh loknath", "charan raj"),
    "Malayalam" to setOf("sushin shyam", "vineeth sreenivasan", "dabzee", "hesham abdul wahab", "shaan rahman"),
    "Hindi" to setOf("arijit singh", "jubin nautiyal", "vishal mishra", "tanishk bagchi", "sachet tandon", "darshan raval", "pritam", "mithoon", "sachin-jigar", "vishal-shekhar", "atif aslam", "shilpa rao", "amitabh bhattacharya", "irshad kamil", "kumaar", "javed ali", "mohit chauhan", "kk"),
    "English" to setOf("taylor swift", "ed sheeran", "justin bieber", "the weeknd", "dua lipa", "bruno mars", "billie eilish", "imagine dragons"),
)

/** True if the song looks like it is in [lang] (and not in another language). */
private fun SongItem.isInLanguage(lang: String, checkSingers: Boolean = true): Boolean {
    val artistNames = artists.map { it.name.lowercase().trim() }
    val artistText = artistNames.joinToString(" ")
    val text = (title + " " + (album?.name ?: "") + " " + artistText).lowercase()
    // another language written in the name or album -> not this playlist
    for ((other, words) in LanguageWords) {
        if (other == lang) continue
        if (lang == "Hindi" && other == "English") continue // "(Hindi Version)" etc. never says English
        if (words.any { it in text }) return false
    }
    // another Indian script in the name -> not this playlist
    val own = LanguageScripts[lang]
    for (ch in title) {
        val code = ch.code
        if (code < 0x0900 || code > 0x0D7F) continue
        if (own == null || code !in own) return false
    }
    // a well-known singer of another language -> not this playlist
    if (!checkSingers) return true
    for ((other, names) in LanguageArtists) {
        if (other == lang) continue
        if (artistNames.any { it in names }) {
            // unless the song clearly says it is in this language
            val mine = LanguageWords[lang].orEmpty()
            if (mine.none { it in text }) return false
        }
    }
    return true
}

/** The one language of a playlist, or null for mixed playlists. */
private fun JoshTheme.onlyLanguage(): String? =
    if (languageMix) null else tag.takeIf { it in LanguageWords.keys }

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
private val LoveColors = listOf(
    listOf(Color(0xFFFF4F8B), Color(0xFF6A0F2E)),
    listOf(Color(0xFFFF7A9C), Color(0xFF5C1438)),
    listOf(Color(0xFFE040FB), Color(0xFF3B0A4F)),
    listOf(Color(0xFFFF6E6E), Color(0xFF5C0A1A)),
)
private val DjColors = listOf(Color(0xFF00E5FF), Color(0xFF2962FF), Color(0xFF6A00F4))
private val MashupColors = listOf(Color(0xFFFF8A00), Color(0xFFE91E63), Color(0xFF3F1D6E))
private val BgmColors = listOf(Color(0xFFFFC107), Color(0xFFB71C1C), Color(0xFF1A0A0A))

private fun paletteFor(i: Int) = Palettes[i % Palettes.size]

/** Main language of a playlist: "Hindi", "Hindi English", "Hindi · Punjabi" -> "Hindi" (Bollywood = Hindi). */
private fun JoshTheme.language(): String = tag.substringBefore("·").trim().substringBefore(" ")

/** Today's Best 5: changes every day, and all 5 are in DIFFERENT languages (only one Hindi/Bollywood). */
fun todaysBestJosh(): List<JoshTheme> =
    AllThemes.shuffled(Random(LocalDate.now().toEpochDay())).distinctBy { it.language() }.take(5)

// ---------------------------------------------------------------- no-repeat memory

private fun prefs(context: Context) = context.getSharedPreferences("kings_josh", Context.MODE_PRIVATE)

/** Saved playlists (song lists) live in their own file so the small settings stay fast. */
private fun cachePrefs(context: Context) = context.getSharedPreferences("kings_josh_cache", Context.MODE_PRIVATE)

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
    // keep only today and yesterday (and remove old saved playlists from earlier versions)
    p.all.keys.filter { it != key && it != "day_${today - 1}" }.forEach { editor.remove(it) }
    editor.apply()
}

// ---------------------------------------------------------------- song loading

private suspend fun searchSongs(
    query: String,
    pages: Int,
    filter: YouTube.SearchFilter = YouTube.SearchFilter.FILTER_SONG,
): List<SongItem> {
    val first = YouTube.search(query, filter).getOrNull() ?: return emptyList()
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

// ---------------------------------------------------------------- one language: YouTube Music's own playlists

/** True if a playlist's name says it is in [lang] (YouTube Music names them "Kannada Hits", "Bollywood Workout" ...). */
private fun PlaylistItem.isInLanguage(lang: String): Boolean {
    val name = title.lowercase()
    if (lang == "English") {
        return LanguageWords.filterKeys { it != "English" }.values.flatten().none { it in name }
    }
    val others = LanguageWords.filterKeys { it != lang && it != "English" }.values.flatten()
    return LanguageWords[lang].orEmpty().any { it in name } && others.none { it in name }
}

/**
 * Songs from YouTube Music's own playlists of one language (for example "Kannada Mass Hits").
 * These are made per language by YouTube Music, so other languages almost never get in.
 * A different set of playlists is used every day.
 */
private suspend fun curatedSongs(queries: List<String>, lang: String, random: Random, maxPlaylists: Int): List<SongItem> = coroutineScope {
    val found = queries.map { q ->
        async {
            runCatching {
                YouTube.search(q, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST).getOrNull()?.items?.filterIsInstance<PlaylistItem>()
            }.getOrNull().orEmpty()
        }
    }.awaitAll()
    // best results first: the top playlist of every search, then the second ...
    val playlists = mutableListOf<PlaylistItem>()
    for (i in 0 until (found.maxOfOrNull { it.size } ?: 0)) found.forEach { list -> list.getOrNull(i)?.let { playlists += it } }
    val chosen = playlists
        .distinctBy { it.id }
        .filter { it.isInLanguage(lang) }
        .take(maxPlaylists * 2)
        .shuffled(random)
        .take(maxPlaylists)
    chosen.map { p ->
        async {
            runCatching {
                val page = YouTube.playlist(p.id).getOrNull() ?: return@runCatching emptyList<SongItem>()
                val more = page.songsContinuation?.let { YouTube.playlistContinuation(it).getOrNull()?.songs }.orEmpty()
                page.songs + more
            }.getOrDefault(emptyList())
        }
    }.awaitAll().flatten()
}

/** A one-language playlist made from YouTube Music's own playlists of that language (null if too few songs). */
private suspend fun curatedTheme(theme: JoshTheme, lang: String, random: Random, skip: Set<String>): List<SongItem>? {
    val mainCount = if (theme.love) 52 else MainSongs
    val oldCount = if (theme.love) 8 else OldSongs
    val fits = { s: SongItem -> s.isJoshAllowed() && s.isInLanguage(lang, checkSingers = false) }
    val mainQueries = listOf(theme.trending, theme.latest.withYear()) + theme.extra
    val main = curatedSongs(mainQueries, lang, random, maxPlaylists = 4)
        .filter(fits)
        .distinctBy { it.id }
        .distinctBy { it.title.lowercase().filter { c -> c.isLetterOrDigit() } }
        .shuffled(random)
    if (main.size < 30) return null
    val mainPicked = pickFresh(main, skip, mainCount)
    val ids = mainPicked.map { it.id }.toSet()
    val old = curatedSongs(listOf(theme.old), lang, random, maxPlaylists = 2)
        .filter { fits(it) && it.id !in ids }
        .distinctBy { it.id }
        .shuffled(random)
    return mainPicked + pickFresh(old, skip, oldCount)
}

/** One josh playlist: trending + new songs, max 10 old songs, no songs from yesterday first. */
/** Extra search words; every day each playlist uses different ones, so it finds different songs. */
private val MashupDayWords = listOf("", "lofi", "slowed reverb", "reels", "chill", "viral", "love", "party")

private val LoveDayWords = listOf("", "hits", "melody", "best", "top", "superhit", "latest", "duet", "heart touching", "evergreen")

private val DayWords = listOf("", "hits", "dance", "party", "viral", "best", "top", "superhit", "dj", "blockbuster", "mass", "chartbusters")

private suspend fun loadTheme(context: Context, theme: JoshTheme): List<MediaMetadata> = withContext(Dispatchers.IO) {
    // The playlist is made once a day and saved: same songs all day (new order on every tap), new songs tomorrow.
    val day = LocalDate.now().toEpochDay()
    val prefix = "theme5_" + theme.title.hashCode() + "_"
    val key = prefix + day
    loadSaved(context, key)?.let { return@withContext it }

    val random = Random(day * 31 + theme.title.hashCode())
    val words = DayWords.shuffled(random)
    val skip = yesterdaysSongs(context)

    // one-language playlists: YouTube Music's own playlists of that language first
    theme.onlyLanguage()?.let { lang ->
        val curated = runCatching { curatedTheme(theme, lang, random, skip) }.getOrNull()
        if (curated != null) {
            val songs = curated.shuffled(random)
            rememberToday(context, songs.map { it.id })
            save(context, key, prefix, songs)
            return@withContext songs.map { it.toMetadata() }
        }
    }
    // mashups: day words that fit mashups
    val loveWords = LoveDayWords.shuffled(random)
    val dayWord = { i: Int ->
        when {
            theme.languageMix -> MashupDayWords[((day + i) % MashupDayWords.size).toInt()]
            theme.love -> loveWords[i % loveWords.size]
            else -> words[i % words.size]
        }
    }
    val mainQueries = listOf(theme.trending, theme.latest.withYear()) + theme.extra
    val (mainLists, old) = coroutineScope {
        val main = mainQueries.flatMapIndexed { i, q ->
            val query = (q + " " + dayWord(i)).trim()
            if (theme.languageMix) {
                // most mashups are uploaded as videos, so search songs AND videos
                listOf(
                    async { runCatching { searchSongs(query, pages = 2) }.getOrDefault(emptyList()) },
                    async { runCatching { searchSongs(query, pages = 3, filter = YouTube.SearchFilter.FILTER_VIDEO) }.getOrDefault(emptyList()) },
                )
            } else {
                listOf(async { runCatching { searchSongs(query, pages = if (theme.love) 3 else 6) }.getOrDefault(emptyList()) })
            }
        }
        val oldSearch = async { runCatching { searchSongs((theme.old + " " + dayWord(9)).trim(), pages = 1) }.getOrDefault(emptyList()) }
        main.awaitAll() to oldSearch.await()
    }
    // songs from every search, taken in turns
    val mainFound = mutableListOf<SongItem>()
    val most = mainLists.maxOfOrNull { it.size } ?: 0
    for (i in 0 until most) mainLists.forEach { list -> list.getOrNull(i)?.let { mainFound += it } }
    val lang = theme.onlyLanguage()
    val fits = { s: SongItem ->
        s.isJoshAllowed() && (!theme.languageMix || s.isMashup()) && (lang == null || s.isInLanguage(lang))
    }
    val main = pickFresh(
        mainFound.filter(fits).distinctBy { it.id }.distinctBy { it.title.lowercase().filter { c -> c.isLetterOrDigit() } },
        skip,
        if (theme.love) 52 else MainSongs, // love: 52 + 8 older = 60 songs
    )
    val mainIds = main.map { it.id }.toSet()
    val oldOnes = pickFresh(
        old.filter { fits(it) && it.id !in mainIds }.distinctBy { it.id }.shuffled(),
        skip,
        if (theme.love) 8 else OldSongs,
    )
    val songs = (main + oldOnes).shuffled(random)
    if (songs.isNotEmpty()) {
        rememberToday(context, songs.map { it.id })
        save(context, key, prefix, songs)
    }
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
            val word = DayWords[((day + theme.title.length) % DayWords.size).toInt()]
            async { runCatching { searchSongs((theme.trending + " " + word).trim(), pages = 1) }.getOrDefault(emptyList()).shuffled(random) }
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

/**
 * One special playlist made from many searches (Mashup Mix, BGM Music).
 * Made once a day and kept the same all day; new songs tomorrow (yesterday's songs are skipped first).
 */
private suspend fun loadSpecialMix(
    context: Context,
    keyPrefix: String,
    always: List<String>,
    pool: List<String>,
    perDay: Int,
    count: Int,
    keep: (SongItem) -> Boolean,
): List<MediaMetadata> = withContext(Dispatchers.IO) {
    val day = LocalDate.now().toEpochDay()
    val key = keyPrefix + day
    loadSaved(context, key)?.let { return@withContext it }

    val skip = yesterdaysSongs(context)
    val random = Random(day * 7 + keyPrefix.hashCode())
    // a different set of searches every day
    val queries = (always + pool.shuffled(random).take(perDay)).distinct()
    val lists = coroutineScope {
        queries.map { q ->
            listOf(
                async { runCatching { searchSongs(q, pages = 1) }.getOrDefault(emptyList()) },
                async { runCatching { searchSongs(q, pages = 2, filter = YouTube.SearchFilter.FILTER_VIDEO) }.getOrDefault(emptyList()) },
            )
        }.flatten().awaitAll()
    }
    // the top results of every search, taken in turns (most trending first)
    val mixed = mutableListOf<SongItem>()
    val max = lists.maxOfOrNull { it.size } ?: 0
    for (i in 0 until max) lists.forEach { list -> list.getOrNull(i)?.let { mixed += it } }
    val found = mixed
        .filter { it.isJoshAllowed() && keep(it) }
        .distinctBy { it.id }
        .distinctBy { it.title.lowercase().filter { c -> c.isLetterOrDigit() } }
    val songs = pickFresh(found, skip, count).shuffled(random)
    if (songs.isNotEmpty()) {
        rememberToday(context, songs.map { it.id })
        save(context, key, keyPrefix, songs)
    }
    songs.map { it.toMetadata() }
}

/**
 * Mashup Mix: ONE playlist of 30 mashups a day, like "Shape of You x Until I Found You"
 * or "Let Me Down Slowly x Main Dhoondne Ko Zamaane Mein". Only songs whose name shows a
 * mashup ("x", "×", "vs", "mashup") are kept.
 */
private val MashupAlways = listOf(
    "shape of you x until i found you mashup",
    "let me down slowly x main dhoondne ko zamaane mein",
    "trending mashup songs",
)
private val MashupPool = listOf(
    "english songs mashup", "english mashup trending", "perfect x until i found you mashup",
    "night changes x perfect mashup", "die with a smile x mashup", "english x hindi mashup",
    "hindi english mashup", "punjabi english mashup", "bollywood mashup trending",
    "kannada english mashup", "tamil english mashup", "telugu english mashup",
    "malayalam english mashup", "hindi punjabi mashup", "mashup reels trending",
    "lofi mashup english", "love mashup english", "party mashup english",
)

private suspend fun loadMashupMix(context: Context) = loadSpecialMix(
    context, "mashup2_", MashupAlways, MashupPool, perDay = 10, count = 30,
) { it.isMashup() }

/**
 * BGM Music: background music (BGM) of films from every language, 30 a day.
 * Only songs whose name says BGM / theme / background music are kept.
 */
private val BgmAlways = listOf("trending bgm")
private val BgmPool = listOf(
    "kannada mass bgm", "kannada movie bgm", "tamil mass bgm", "tamil movie bgm anirudh",
    "telugu mass bgm", "telugu movie bgm", "malayalam mass bgm", "malayalam movie bgm",
    "hindi movie bgm", "bollywood bgm", "punjabi bgm", "marathi movie bgm",
    "south indian mass bgm", "hero entry bgm", "hollywood movie theme music", "english movie bgm",
)

private fun SongItem.isBgm(): Boolean {
    val name = title.lowercase()
    return "bgm" in name || "theme" in name || "background" in name || "score" in name
}

private suspend fun loadBgmMix(context: Context) = loadSpecialMix(
    context, "bgm_", BgmAlways, BgmPool, perDay = 10, count = 30,
) { it.isBgm() }

/**
 * All DJ Music: DJ songs from every language in one playlist.
 * Made once a day and kept the same all day; new DJ songs tomorrow.
 */
private suspend fun loadAllDj(context: Context): List<MediaMetadata> = withContext(Dispatchers.IO) {
    val day = LocalDate.now().toEpochDay()
    val key = "alldj_$day"
    loadSaved(context, key)?.let { return@withContext it }

    val skip = yesterdaysSongs(context)
    val random = Random(day + 99)
    val lists = coroutineScope {
        DjQueries.mapIndexed { i, query ->
            val word = DayWords[((day + i) % DayWords.size).toInt()]
            async { runCatching { searchSongs((query + " " + word).trim(), pages = 2) }.getOrDefault(emptyList()).shuffled(random) }
        }.awaitAll()
    }.shuffled(random)
    // one song from each language in turns
    val mixed = mutableListOf<SongItem>()
    val max = lists.maxOfOrNull { it.size } ?: 0
    for (i in 0 until max) lists.forEach { list -> list.getOrNull(i)?.let { mixed += it } }
    val songs = pickFresh(mixed.filter { it.isJoshAllowed() }.distinctBy { it.id }, skip, 200)
    if (songs.isNotEmpty()) {
        rememberToday(context, songs.map { it.id })
        save(context, key, "alldj_", songs)
    }
    songs.map { it.toMetadata() }
}

/** Reads a saved playlist (song list) from the phone. */
private fun loadSaved(context: Context, key: String): List<MediaMetadata>? {
    val saved = cachePrefs(context).getString(key, null) ?: return null
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
    val p = cachePrefs(context)
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

// ---------------------------------------------------------------- cover images

/**
 * Cover pictures for a card: album covers of real songs.
 * If today's playlist is already made, its own song covers are used.
 * Otherwise one quick search is done; the result is saved for the day.
 */
@Composable
private fun rememberCovers(id: String, query: String, savedKey: String? = null): List<String> {
    val context = LocalContext.current
    val day = LocalDate.now().toEpochDay()
    val prefix = "cover_" + id.hashCode() + "_"
    val key = prefix + day
    var covers by remember(key) { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(key) {
        covers = withContext(Dispatchers.IO) {
            val fromPlaylist = savedKey?.let { runCatching { loadSaved(context, it) }.getOrNull() }
                ?.mapNotNull { it.thumbnailUrl }?.distinct()?.take(4)
            if (!fromPlaylist.isNullOrEmpty()) return@withContext fromPlaylist
            val saved = cachePrefs(context).getString(key, null)?.split("\n")?.filter { it.isNotBlank() }
            if (!saved.isNullOrEmpty()) return@withContext saved
            val found = runCatching { searchSongs(query, pages = 1) }.getOrDefault(emptyList())
                .filter { it.isJoshAllowed() }
                .map { it.thumbnail }
                .distinct()
                .take(4)
            if (found.isNotEmpty()) {
                val p = cachePrefs(context)
                val editor = p.edit()
                p.all.keys.filter { it.startsWith(prefix) && it != key }.forEach { editor.remove(it) }
                editor.putString(key, found.joinToString("\n")).apply()
            }
            found
        }
    }
    return covers
}

private fun themeSavedKey(theme: JoshTheme) = "theme5_" + theme.title.hashCode() + "_" + LocalDate.now().toEpochDay()

/** One cover picture filling the space. */
@Composable
private fun Cover(url: String, modifier: Modifier) {
    AsyncImage(
        model = url.resize(544, 544),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

/** Up to 3 covers side by side (for wide cards), with the card colour fading over them from the left. */
@Composable
private fun WideCovers(covers: List<String>, colors: List<Color>) {
    if (covers.isEmpty()) return
    Row(Modifier.fillMaxSize()) {
        covers.take(3).forEach { url -> Cover(url, Modifier.weight(1f).fillMaxSize()) }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    0f to colors.first().copy(alpha = 0.97f),
                    0.45f to colors.first().copy(alpha = 0.80f),
                    1f to colors.last().copy(alpha = 0.35f),
                )
            )
    )
}

// ---------------------------------------------------------------- Explore banner

/** Old Home section name, kept so older Home code still builds. Josh mixes now live in Explore only. */
@Composable
fun KingsDailyMixesSection(title: String = "", subtitle: String = "") {
}

/** Big "Josh Songs" card at the top of Explore. Opens the Josh Songs page. */
@Composable
fun KingsJoshBanner() {
    val navController = LocalNavController.current
    val covers = rememberCovers("banner", "trending songs india high energy")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(AllJoshColors))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .clickable { navController.navigate("kings_josh") },
    ) {
        WideCovers(covers, AllJoshColors)
        Box(Modifier.fillMaxSize().glowSpots().padding(18.dp)) {
        Column(Modifier.align(Alignment.CenterStart)) {
            Text("JOSH SONGS", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("Full josh playlists", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Insta trending · Best 5 · All josh mix · DJ music", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        }
        PlayDot(Modifier.align(Alignment.CenterEnd), AllJoshColors.last(), loading = false)
        }
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
                Spacer(Modifier.height(8.dp)) // Kings Play: version line removed (Josh v18)
            }
        }

        item(key = "insta") {
            WideCard(
                tag = "VIRAL ON REELS · $monthName",
                title = "Insta Trending",
                subtitle = "Songs trending on reels · changes once a month",
                colors = InstaColors,
                loading = loading == "insta",
                covers = rememberCovers("insta", "instagram reels trending songs", "insta_v2_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))),
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
                subtitle = "Every josh playlist combined · new mix every day",
                colors = AllJoshColors,
                loading = loading == "all",
                covers = rememberCovers("alljosh", "trending indian party songs", "alljosh_v2_" + LocalDate.now().toEpochDay()),
            ) { play("all", "All Josh Mix") { loadAllJosh(context) } }
        }
        item(key = "dj") {
            WideCard(
                tag = "DJ SONGS FROM EVERY LANGUAGE",
                title = "All DJ Music",
                subtitle = "Hindi, Punjabi, Kannada, Tamil, Telugu & more · new every day",
                colors = DjColors,
                loading = loading == "dj",
                covers = rememberCovers("alldj", "hindi dj songs", "alldj_" + LocalDate.now().toEpochDay()),
            ) { play("dj", "All DJ Music") { loadAllDj(context) } }
        }
        item(key = "mashup") {
            WideCard(
                tag = "MASHUPS FROM EVERY LANGUAGE",
                title = "Mashup Mix",
                subtitle = "30 mashups like Shape of You × Until I Found You · new every day",
                colors = MashupColors,
                loading = loading == "mashup",
                covers = rememberCovers("mashupmix", "english x hindi mashup", "mashup2_" + LocalDate.now().toEpochDay()),
            ) { play("mashup", "Mashup Mix") { loadMashupMix(context) } }
        }
        item(key = "bgm") {
            WideCard(
                tag = "BGM FROM EVERY LANGUAGE",
                title = "BGM Music",
                subtitle = "30 film BGMs - Kannada, Tamil, Telugu, Hindi & more · new every day",
                colors = BgmColors,
                loading = loading == "bgm",
                covers = rememberCovers("bgmmix", "mass bgm", "bgm_" + LocalDate.now().toEpochDay()),
            ) { play("bgm", "BGM Music") { loadBgmMix(context) } }
        }
        listOf("josh" to JoshThemes, "love" to LoveThemes).forEach { (group, themes) ->
            if (group == "love") {
                item(key = "love_title") { SectionTitle("Love Songs", "Only romantic songs · 60 a day · new every day") }
            }
            themes.chunked(2).forEachIndexed { rowIndex, row ->
                item(key = "${group}_row_$rowIndex") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        row.forEachIndexed { j, theme ->
                            Box(Modifier.weight(1f)) {
                                val color = if (group == "love") LoveColors[(rowIndex * 2 + j) % LoveColors.size] else paletteFor(rowIndex * 2 + j + 3)
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
    covers: List<String> = emptyList(),
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(colors))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
    ) {
        WideCovers(covers, colors)
        Box(Modifier.fillMaxSize().glowSpots().padding(18.dp)) {
            Column(Modifier.align(Alignment.CenterStart).padding(end = 56.dp)) {
                Text(tag, color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, maxLines = 2)
            }
            PlayDot(Modifier.align(Alignment.CenterEnd), colors.last(), loading)
        }
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
    val covers = rememberCovers(theme.title, theme.trending, themeSavedKey(theme))
    Box(
        modifier = Modifier
            .then(if (width != null) Modifier.size(width = width, height = 190.dp) else Modifier.fillMaxWidth().height(170.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(colors))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
    ) {
        // the playlist's cover picture, with its colour fading in from the bottom so the name is easy to read
        covers.firstOrNull()?.let { url ->
            Cover(url, Modifier.fillMaxSize())
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to colors.first().copy(alpha = 0.35f),
                            0.45f to colors.last().copy(alpha = 0.45f),
                            1f to colors.last().copy(alpha = 0.97f),
                        )
                    )
            )
        }
        Box(Modifier.fillMaxSize().padding(14.dp)) {
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
}
