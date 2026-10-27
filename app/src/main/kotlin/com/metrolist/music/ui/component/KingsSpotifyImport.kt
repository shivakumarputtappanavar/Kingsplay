/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Import a Spotify playlist (or album) by pasting its link.
 * Reads the song names and artists from Spotify's public "embed" page
 * (the same way BitChord does — BitChord is GPL-3.0, github.com/kushagrasinghx/BitChord),
 * then Kings Play's existing playlist import matches each song on YouTube Music.
 *
 * Limits: public playlists only, first 100 songs (that is all the embed page shows).
 * This is unofficial: it can stop working if Spotify changes that page.
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.ui.menu.AddToPlaylistDialogOnline
import com.metrolist.music.ui.menu.LoadingScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Opens the "paste Spotify link" box. */
object KingsSpotifyImportState {
    var showDialog by mutableStateOf(false)
}

data class KingsSpotifyTrack(val title: String, val artist: String)

private const val BrowserAgent =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"

private val SpotifyIdRegex = Regex("""(playlist|album)[/:]([A-Za-z0-9]{22})""")

private fun httpGet(url: String): Pair<String, String> {
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 12000
        readTimeout = 15000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", BrowserAgent)
        setRequestProperty("Accept-Language", "en")
    }
    try {
        if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        return conn.url.toString() to body
    } finally {
        conn.disconnect()
    }
}

/** "playlist"/"album" and the id, from any Spotify link (also spotify.link short links). */
private fun findSpotifyId(link: String): Pair<String, String>? {
    SpotifyIdRegex.find(link)?.let { return it.groupValues[1] to it.groupValues[2] }
    val trimmed = link.trim()
    if (trimmed.contains("spotify.link") || trimmed.contains("spoti.fi")) {
        val url = if (trimmed.startsWith("http")) trimmed else "https://$trimmed"
        val finalUrl = runCatching { httpGet(url).first }.getOrNull() ?: return null
        SpotifyIdRegex.find(finalUrl)?.let { return it.groupValues[1] to it.groupValues[2] }
    }
    return null
}

/** Reads a public Spotify playlist/album: its name and up to 100 songs. */
suspend fun fetchSpotifyTracks(link: String): Pair<String, List<KingsSpotifyTrack>> = withContext(Dispatchers.IO) {
    val (type, id) = findSpotifyId(link) ?: error("NOT_A_LINK")
    val html = httpGet("https://open.spotify.com/embed/$type/$id").second

    val json = Regex(
        """<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""",
        RegexOption.DOT_MATCHES_ALL,
    ).find(html)?.groupValues?.get(1) ?: error("NOT_PUBLIC")

    val props = JSONObject(json).optJSONObject("props")?.optJSONObject("pageProps")
    val entity = props?.optJSONObject("state")?.optJSONObject("data")?.optJSONObject("entity")
        ?: props?.optJSONObject("entity")
        ?: error("NOT_PUBLIC")

    val name = entity.optString("name").ifBlank { entity.optString("title") }
        .ifBlank { "Spotify playlist" }
    val list: JSONArray = entity.optJSONArray("trackList")
        ?: entity.optJSONArray("tracks")
        ?: JSONArray()

    val tracks = buildList {
        for (i in 0 until list.length()) {
            val t = list.optJSONObject(i) ?: continue
            val title = t.optString("title").ifBlank { t.optString("name") }.trim()
            var artist = t.optString("subtitle").trim()
            if (artist.isBlank()) {
                val artists = t.optJSONArray("artists")
                if (artists != null) {
                    artist = (0 until artists.length())
                        .mapNotNull { artists.optJSONObject(it)?.optString("name") }
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                }
            }
            if (title.isNotBlank()) add(KingsSpotifyTrack(title, artist))
        }
    }
    if (tracks.isEmpty()) error("NO_SONGS")
    name to tracks
}

/** Small button for the Library header: opens the Spotify import box. */
@Composable
fun KingsSpotifyImportButton() {
    IconButton(
        onClick = { KingsSpotifyImportState.showDialog = true },
        modifier = Modifier.padding(start = 8.dp).size(40.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.playlist_add),
            contentDescription = "Import from Spotify",
        )
    }
}

/**
 * Put once in a Library screen (not inside a list item).
 * Shows the link box, then Kings Play's normal "add to playlist" matching with progress.
 */
@Composable
fun KingsSpotifyImportHost() {
    val importedSongs = remember { mutableStateListOf<Song>() }
    var playlistTitle by remember { mutableStateOf("") }
    var showChoosePlaylist by remember { mutableStateOf(false) }
    var isProgressStarted by remember { mutableStateOf(false) }
    var progressPercentage by remember { mutableIntStateOf(0) }
    var currentSong by remember { mutableStateOf("") }

    if (KingsSpotifyImportState.showDialog) {
        KingsSpotifyLinkDialog(
            onDismiss = { KingsSpotifyImportState.showDialog = false },
            onLoaded = { title, tracks ->
                importedSongs.clear()
                importedSongs.addAll(
                    tracks.map { track ->
                        Song(
                            song = SongEntity(id = "", title = track.title),
                            artists = track.artist
                                .split(",", "&")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .map { ArtistEntity(id = "", name = it) },
                        )
                    }
                )
                playlistTitle = title
                KingsSpotifyImportState.showDialog = false
                showChoosePlaylist = true
            },
        )
    }

    AddToPlaylistDialogOnline(
        isVisible = showChoosePlaylist,
        allowSyncing = false,
        initialTextFieldValue = playlistTitle,
        songs = importedSongs,
        onDismiss = { showChoosePlaylist = false },
        onProgressStart = { isProgressStarted = it },
        onPercentageChange = { progressPercentage = it },
        onSongChange = { currentSong = it },
    )

    LoadingScreen(
        isVisible = isProgressStarted,
        value = progressPercentage,
        songTitle = currentSong,
    )
}

@Composable
private fun KingsSpotifyLinkDialog(
    onDismiss: () -> Unit,
    onLoaded: (String, List<KingsSpotifyTrack>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("Import from Spotify") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "In Spotify, open a playlist → Share → Copy link, then paste it here.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it; error = null },
                    singleLine = true,
                    enabled = !loading,
                    placeholder = { Text("open.spotify.com/playlist/…") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Public playlists and albums, up to 100 songs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = link.isNotBlank() && !loading,
                onClick = {
                    loading = true
                    error = null
                    scope.launch {
                        try {
                            val (title, tracks) = fetchSpotifyTracks(link)
                            loading = false
                            onLoaded(title, tracks)
                        } catch (e: Exception) {
                            loading = false
                            error = when (e.message) {
                                "NOT_A_LINK" -> "That doesn't look like a Spotify playlist or album link."
                                "NOT_PUBLIC" -> "Couldn't read this playlist. Make sure it is public."
                                "NO_SONGS" -> "No songs found in this playlist."
                                else -> "Couldn't reach Spotify. Check your internet and try again."
                            }
                        }
                    }
                },
            ) { Text(if (loading) "Reading…" else "Import") }
        },
        dismissButton = {
            TextButton(enabled = !loading, onClick = onDismiss) { Text("Cancel") }
        },
    )
}
