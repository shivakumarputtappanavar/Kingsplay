/**
 * Kings Play - inside every playlist: a "+ Add songs" button (search YouTube Music and add)
 * and a "Recommended" row made from the playlist's own songs.
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalDatabase
import com.metrolist.music.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun KingsPlaylistExtras(
    playlistId: String,
    seedIds: List<String>,
    existingIds: Set<String>,
    modifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var recommended by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var added by remember { mutableStateOf<Set<String>>(emptySet()) }

    // recommended songs: related songs of a few random songs from this playlist
    LaunchedEffect(playlistId, seedIds.isNotEmpty()) {
        if (seedIds.isEmpty()) return@LaunchedEffect
        recommended = withContext(Dispatchers.IO) {
            val found = mutableListOf<SongItem>()
            seedIds.shuffled().take(3).forEach { id ->
                val endpoint = YouTube.next(WatchEndpoint(videoId = id)).getOrNull()?.relatedEndpoint
                if (endpoint != null) {
                    YouTube.related(endpoint).onSuccess { page -> found.addAll(page.songs) }
                }
            }
            found.distinctBy { it.id }.take(24)
        }
    }

    // adds one song to this playlist (same calls the playlist import uses)
    val addSong: (SongItem) -> Unit = { song ->
        if (song.id !in added) {
            added = added + song.id
            scope.launch(Dispatchers.IO) {
                database.insert(song.toMediaMetadata())
                val target = database.playlistBlocking(playlistId)
                if (target != null) {
                    database.addSongsToPlaylist(target, listOf(song.id to null))
                }
            }
        }
    }

    val shown = recommended.filter { it.id !in existingIds && it.id !in added }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Button(
            onClick = { showSearch = true },
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text("+  Add songs")
        }

        if (shown.isNotEmpty()) {
            Text(
                text = "Recommended",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(shown, key = { "kg_rec_${it.id}" }) { song ->
                    Column(modifier = Modifier.width(132.dp)) {
                        Box {
                            AsyncImage(
                                model = song.thumbnail,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(132.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable { addSong(song) },
                            ) {
                                Text("+", color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp)
                            }
                        }
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Text(
                            text = song.artists.joinToString(", ") { it.name },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    if (showSearch) {
        var query by remember { mutableStateOf("") }
        var results by remember { mutableStateOf<List<SongItem>>(emptyList()) }
        var loading by remember { mutableStateOf(false) }
        var searched by remember { mutableStateOf(false) }

        val runSearch: () -> Unit = {
            if (query.isNotBlank() && !loading) {
                loading = true
                scope.launch {
                    results = withContext(Dispatchers.IO) {
                        YouTube.search(query.trim(), YouTube.SearchFilter.FILTER_SONG)
                            .getOrNull()?.items.orEmpty().filterIsInstance<SongItem>()
                    }
                    loading = false
                    searched = true
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showSearch = false },
            title = { Text("Add songs") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text("Search a song or artist") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = runSearch, enabled = query.isNotBlank() && !loading) {
                        Text("Search")
                    }
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(26.dp))
                    if (searched && !loading && results.isEmpty()) {
                        Text("No songs found.", style = MaterialTheme.typography.bodySmall)
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                        items(results, key = { "kg_search_${it.id}" }) { song ->
                            val isAdded = song.id in added || song.id in existingIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAdded) { addSong(song) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AsyncImage(
                                    model = song.thumbnail,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        song.artists.joinToString(", ") { it.name },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = if (isAdded) "Added" else "+",
                                    fontSize = if (isAdded) 12.sp else 22.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSearch = false }) { Text("Done") } },
        )
    }
}