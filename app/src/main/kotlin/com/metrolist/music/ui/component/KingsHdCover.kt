/**
 * Kings Play - sharp album covers for the player.
 *  1) asks YouTube Music for the big version of the cover picture
 *  2) looks the song up in Apple's public artwork service (covers up to 3000x3000) and uses that
 *     only when the title AND the artist match; otherwise the normal cover stays.
 * Answers are remembered while the app is open, so each song is looked up once.
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private val kgSizeWH = Regex("=w\\d+-h\\d+")
private val kgSizeS = Regex("=s\\d+")
private val kgQuality = Regex("-l\\d+")

/** Bigger version of a YouTube Music cover link. */
fun kingsHdCover(url: String?, size: Int = 1600): String? {
    if (url.isNullOrBlank()) return url
    return when {
        kgSizeWH.containsMatchIn(url) ->
            url.replace(kgSizeWH, "=w$size-h$size").replace(kgQuality, "-l100")
        kgSizeS.containsMatchIn(url) && (url.contains("googleusercontent") || url.contains("ggpht")) ->
            url.replace(kgSizeS, "=s$size")
        else -> url
    }
}

private val kgAppleCache = ConcurrentHashMap<String, String>()

/** Sharp cover from Apple's public artwork service, or null when there is no sure match. */
suspend fun kingsAppleCover(title: String?, artist: String?): String? {
    if (title.isNullOrBlank() || artist.isNullOrBlank()) return null
    val key = title.lowercase() + "|" + artist.lowercase()
    kgAppleCache[key]?.let { return it.ifEmpty { null } }
    val found = withContext(Dispatchers.IO) { runCatching { kgLookup(title, artist) }.getOrNull() }
    kgAppleCache[key] = found.orEmpty()
    return found
}

private fun kgNorm(s: String): String =
    s.lowercase().replace(Regex("\\(.*?\\)|\\[.*?\\]"), " ").replace(Regex("[^\\p{L}\\p{N}]+"), "")

private fun kgLookup(title: String, artist: String): String? {
    val term = java.net.URLEncoder.encode("$title $artist".trim(), "UTF-8")
    val url = java.net.URL("https://itunes.apple.com/search?term=$term&media=music&entity=song&limit=8&country=IN")
    val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
        connectTimeout = 4000
        readTimeout = 4000
        requestMethod = "GET"
    }
    try {
        if (conn.responseCode != 200) return null
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val results = org.json.JSONObject(body).optJSONArray("results") ?: return null
        val wantedTitle = kgNorm(title)
        val wantedArtists = artist.split(",", "&", ";", " and ", " feat").map { kgNorm(it) }.filter { it.length >= 3 }
        if (wantedTitle.length < 3 || wantedArtists.isEmpty()) return null
        for (i in 0 until results.length()) {
            val r = results.optJSONObject(i) ?: continue
            val trackTitle = kgNorm(r.optString("trackName"))
            val trackArtist = kgNorm(r.optString("artistName"))
            val art = r.optString("artworkUrl100")
            val titleOk = trackTitle.length >= 3 &&
                (trackTitle == wantedTitle || trackTitle.contains(wantedTitle) || wantedTitle.contains(trackTitle))
            val artistOk = trackArtist.length >= 3 &&
                wantedArtists.any { trackArtist.contains(it) || it.contains(trackArtist) }
            if (titleOk && artistOk && art.isNotBlank()) {
                return art.replace(Regex("\\d+x\\d+bb"), "3000x3000bb")
            }
        }
        return null
    } finally {
        conn.disconnect()
    }
}

@Composable
private fun KgCoverLayer(data: Any?, scale: ContentScale, tiny: Boolean = false) {
    val context = LocalContext.current
    val request = remember(data, tiny) {
        val b = ImageRequest.Builder(context).data(data)
        if (tiny) b.size(48, 48)
        b.build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = scale,
        modifier = Modifier.fillMaxSize(),
    )
}

/** The whole cover (never cropped) on a soft colour fill, sharpest version on top. */
@Composable
fun KingsCoverLayers(
    artworkUri: String?,
    title: String?,
    artist: String?,
    modifier: Modifier = Modifier,
) {
    var apple by remember(title, artist) { mutableStateOf<String?>(null) }
    LaunchedEffect(title, artist) { apple = kingsAppleCover(title, artist) }
    val hd = kingsHdCover(artworkUri)

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        // soft colour fill from the same cover, so the whole picture shows with no bars
        KgCoverLayer(artworkUri, ContentScale.Crop, tiny = true)
        // the cover as it was: shows at once and stays if nothing sharper exists
        KgCoverLayer(artworkUri, ContentScale.Fit)
        if (hd != null && hd != artworkUri) KgCoverLayer(hd, ContentScale.Fit)
        // the sharpest one, only when title and artist match for sure
        apple?.let { KgCoverLayer(it, ContentScale.Fit) }
    }
}