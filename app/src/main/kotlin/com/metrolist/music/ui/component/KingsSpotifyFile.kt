/**
 * Kings Play - import Spotify playlists from the file you download from Spotify
 * (Account > Privacy > Download your data): the ZIP, Playlist1.json, YourLibrary.json,
 * or a CSV made with the Exportify tool. Up to 10,000 songs per playlist.
 * The songs then go through the same "add to playlist" matching as the link import.
 */

package com.metrolist.music.ui.component

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.util.zip.ZipInputStream

/** Most songs taken from one playlist. */
private const val KG_SPOTIFY_MAX = 10_000

private class KgSpotifyList(val name: String, val tracks: List<KingsSpotifyTrack>)

/** Button for the Spotify import box: choose the file, pick a playlist, then it goes on as a normal import. */
@Composable
internal fun KingsSpotifyFileButton(
    enabled: Boolean,
    onLoaded: (String, List<KingsSpotifyTrack>) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var found by remember { mutableStateOf<List<KgSpotifyList>?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            busy = true
            message = null
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { readSpotifyFile(context, uri) }.getOrNull()
                }
                busy = false
                if (result.isNullOrEmpty()) {
                    message = "No Spotify playlists found in that file."
                } else {
                    found = result
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
            enabled = enabled && !busy,
            onClick = { launcher.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (busy) "Reading file..." else "Choose Spotify data file")
        }
        Text(
            "From Spotify: Account > Privacy > Download your data. Pick the ZIP, Playlist1.json " +
                "or a CSV from Exportify. Up to 10,000 songs per playlist. " +
                "Keep the app open while it matches the songs.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        message?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }

    found?.let { lists ->
        AlertDialog(
            onDismissRequest = { found = null },
            title = { Text("Choose what to import") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(lists) { list ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    found = null
                                    onLoaded(list.name, list.tracks.take(KG_SPOTIFY_MAX))
                                }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(
                                text = list.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val count = list.tracks.size
                            Text(
                                text = if (count > KG_SPOTIFY_MAX) "$count songs (first 10,000 only)" else "$count songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { found = null }) { Text("Cancel") } },
        )
    }
}

/** Keeps the screen on while a big import is matching songs. */
@Composable
internal fun KingsKeepScreenOn(on: Boolean) {
    val view = LocalView.current
    DisposableEffect(on) {
        view.keepScreenOn = on
        onDispose { view.keepScreenOn = false }
    }
}

private fun displayName(context: Context, uri: Uri): String {
    var name = "Spotify playlist"
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (col >= 0 && cursor.moveToFirst()) name = cursor.getString(col) ?: name
        }
    }
    return name
}

private fun readSpotifyFile(context: Context, uri: Uri): List<KgSpotifyList> {
    val name = displayName(context, uri)
    val lists = mutableListOf<KgSpotifyList>()
    context.contentResolver.openInputStream(uri)?.use { raw ->
        val input = BufferedInputStream(raw)
        input.mark(4)
        val b0 = input.read()
        val b1 = input.read()
        input.reset()
        if (b0 == 0x50 && b1 == 0x4B) {
            // ZIP from "Download your data"
            val playlistName = Regex("(?i)playlist\\d*\\.json")
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val file = entry.name.substringAfterLast('/')
                    if (!entry.isDirectory && (playlistName.matches(file) || file.equals("YourLibrary.json", ignoreCase = true))) {
                        lists += parseSpotifyJson(String(zip.readBytes(), Charsets.UTF_8))
                    }
                    entry = zip.nextEntry
                }
            }
        } else {
            val text = String(input.readBytes(), Charsets.UTF_8).removePrefix("\uFEFF")
            if (text.trimStart().startsWith("{")) {
                lists += parseSpotifyJson(text)
            } else {
                lists += parseSpotifyCsv(text, name)
            }
        }
    }
    return lists
}

private fun parseSpotifyJson(text: String): List<KgSpotifyList> {
    val out = mutableListOf<KgSpotifyList>()
    val root = JSONObject(text)

    // Playlist1.json
    root.optJSONArray("playlists")?.let { playlists ->
        for (i in 0 until playlists.length()) {
            val playlist = playlists.optJSONObject(i) ?: continue
            val title = playlist.optString("name").ifBlank { "Spotify playlist ${i + 1}" }
            val items = playlist.optJSONArray("items") ?: continue
            val tracks = mutableListOf<KingsSpotifyTrack>()
            for (j in 0 until items.length()) {
                val track = items.optJSONObject(j)?.optJSONObject("track") ?: continue
                val song = track.optString("trackName")
                val artist = track.optString("artistName")
                if (song.isNotBlank()) tracks.add(KingsSpotifyTrack(song, artist.replace(';', ',')))
            }
            if (tracks.isNotEmpty()) out.add(KgSpotifyList(title, tracks))
        }
    }

    // YourLibrary.json (liked songs)
    root.optJSONArray("tracks")?.let { liked ->
        val tracks = mutableListOf<KingsSpotifyTrack>()
        for (j in 0 until liked.length()) {
            val o = liked.optJSONObject(j) ?: continue
            val song = o.optString("track")
            val artist = o.optString("artist")
            if (song.isNotBlank()) tracks.add(KingsSpotifyTrack(song, artist.replace(';', ',')))
        }
        if (tracks.isNotEmpty()) out.add(KgSpotifyList("Liked songs", tracks))
    }
    return out
}

private fun parseSpotifyCsv(text: String, fileName: String): List<KgSpotifyList> {
    val rows = csvRows(text)
    if (rows.size < 2) return emptyList()
    val header = rows[0].map { it.trim().lowercase() }
    val titleCol = header.indexOfFirst { it == "track name" || it == "track" || it == "title" || it == "name" }
    val artistCol = header.indexOfFirst { it.startsWith("artist") }
    if (titleCol < 0) return emptyList()
    val tracks = mutableListOf<KingsSpotifyTrack>()
    for (r in 1 until rows.size) {
        val row = rows[r]
        val song = row.getOrNull(titleCol).orEmpty().trim()
        val artist = if (artistCol >= 0) row.getOrNull(artistCol).orEmpty().trim() else ""
        if (song.isNotBlank()) tracks.add(KingsSpotifyTrack(song, artist.replace(';', ',')))
    }
    if (tracks.isEmpty()) return emptyList()
    return listOf(KgSpotifyList(fileName.substringBeforeLast('.').ifBlank { "Spotify playlist" }, tracks))
}

/** Small CSV reader: handles quotes, commas and line breaks inside quotes. */
private fun csvRows(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (inQuotes) {
            if (c == '"') {
                if (i + 1 < text.length && text[i + 1] == '"') {
                    field.append('"')
                    i++
                } else {
                    inQuotes = false
                }
            } else {
                field.append(c)
            }
        } else {
            when (c) {
                '"' -> inQuotes = true
                ',' -> {
                    row.add(field.toString())
                    field.setLength(0)
                }
                '\r' -> {}
                '\n' -> {
                    row.add(field.toString())
                    field.setLength(0)
                    if (row.any { it.isNotBlank() }) rows.add(row)
                    row = mutableListOf()
                }
                else -> field.append(c)
            }
        }
        i++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) {
        row.add(field.toString())
        if (row.any { it.isNotBlank() }) rows.add(row)
    }
    return rows
}