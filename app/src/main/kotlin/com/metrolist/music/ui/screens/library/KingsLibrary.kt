/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The new Library main view:
 *  - big "Library" title
 *  - swipeable "Your listening experience" cards (minutes, top song, top artist, top album)
 *  - Downloads and Local Music as two separate rows
 *  - Playlists (New playlist, Import Spotify, Liked songs, your playlists), Artists
 *  - Songs / Albums / Podcasts rows to reach the other lists
 */

package com.metrolist.music.ui.screens.library

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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.LibraryFilter
import com.metrolist.music.ui.component.CreatePlaylistDialog
import com.metrolist.music.ui.component.KingsSpotifyImportHost
import com.metrolist.music.ui.component.KingsSpotifyImportState
import com.metrolist.music.ui.component.LibraryArtistGridItem
import com.metrolist.music.ui.component.LibraryPlaylistGridItem
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LibraryArtistsViewModel
import com.metrolist.music.viewmodels.LibraryPlaylistsViewModel
import com.metrolist.music.viewmodels.StatsViewModel
import java.time.format.DateTimeFormatter

private data class KingsStatCard(
    val big: String,
    val label: String,
    val footer: String,
    val colors: List<Color>,
)

@Composable
fun KingsLibraryOverview(
    navController: NavController,
    onShowAll: (LibraryFilter) -> Unit,
) {
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()

    val playlistsViewModel: LibraryPlaylistsViewModel = hiltViewModel()
    val artistsViewModel: LibraryArtistsViewModel = hiltViewModel()
    val statsViewModel: StatsViewModel = hiltViewModel()

    val playlists by playlistsViewModel.allPlaylists.collectAsStateWithLifecycle()
    val artists by artistsViewModel.allArtists.collectAsStateWithLifecycle()
    val topSongs by statsViewModel.mostPlayedSongsStats.collectAsStateWithLifecycle()
    val topArtists by statsViewModel.mostPlayedArtists.collectAsStateWithLifecycle()
    val topAlbums by statsViewModel.mostPlayedAlbums.collectAsStateWithLifecycle()
    val firstEvent by statsViewModel.firstEvent.collectAsStateWithLifecycle()
    val downloaded by database.downloadedSongsByCreateDateAsc().collectAsStateWithLifecycle(initialValue = emptyList())
    val accountName by rememberPreference(AccountNameKey, "")

    var showCreatePlaylist by rememberSaveable { mutableStateOf(false) }
    if (showCreatePlaylist) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylist = false },
            onPlaylistCreated = { playlistId ->
                showCreatePlaylist = false
                navController.navigate("local_playlist/$playlistId")
            },
        )
    }
    KingsSpotifyImportHost()

    // ---- listening cards
    val name = accountName.ifBlank { "Kings Play listener" }.uppercase()
    val since = firstEvent?.event?.timestamp?.format(DateTimeFormatter.ofPattern("MM/yy")) ?: "--/--"
    val minutes = topSongs.sumOf { it.timeListened ?: 0L } / 60000L
    val plays = topSongs.sumOf { it.songCountListened }
    val cards = buildList {
        add(KingsStatCard("%,d".format(minutes), "MINUTES LISTENED", "$plays plays", listOf(Color(0xFF7A1235), Color(0xFF2B0614))))
        topSongs.firstOrNull()?.let {
            add(KingsStatCard(it.title, "TOP SONG", "${it.artistName ?: ""} · ${it.songCountListened} plays", listOf(Color(0xFF3B2A7A), Color(0xFF120A33))))
        }
        topArtists.firstOrNull()?.let {
            val mins = (it.timeListened ?: 0) / 60000
            add(KingsStatCard(it.artist.name, "TOP ARTIST", "$mins min · ${it.songCount} songs", listOf(Color(0xFF0F5C6B), Color(0xFF062A33))))
        }
        topAlbums.firstOrNull()?.let {
            add(KingsStatCard(it.album.title, "TOP ALBUM", it.artists.joinToString(", ") { a -> a.name }, listOf(Color(0xFF7A4A0F), Color(0xFF2E1A04))))
        }
    }

    LazyColumn(
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "kp_title") {
            Text(
                text = "Library",
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 18.dp, top = 8.dp, bottom = 12.dp),
            )
        }

        item(key = "kp_cards") {
            val pagerState = rememberPagerState { cards.size }
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(start = 18.dp, end = 60.dp),
                pageSpacing = 12.dp,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                StatCardView(cards[page], name, since)
            }
            if (cards.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) {
                    repeat(cards.size) { i ->
                        val on = pagerState.currentPage == i
                        Box(
                            Modifier
                                .height(6.dp)
                                .width(if (on) 18.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
                        )
                    }
                }
            }
        }

        item(key = "kp_rows") {
            RowCard(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp),
                rows = listOf(
                    Triple(R.drawable.download, "Downloads", "${downloaded.size} songs · saved for offline") to {
                        navController.navigate("auto_playlist/downloaded")
                    },
                    Triple(R.drawable.library_music, "Local Music", "Songs saved on this phone") to {
                        navController.navigate("kings_local_music")
                    },
                ),
            )
        }

        item(key = "kp_playlists_title") { SectionTitle("Playlists") { onShowAll(LibraryFilter.PLAYLISTS) } }
        item(key = "kp_playlists") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "new") {
                    ActionTile(R.drawable.add, "New playlist", "Create your own") { showCreatePlaylist = true }
                }
                item(key = "spotify") {
                    ActionTile(R.drawable.playlist_add, "Import Spotify", "From a playlist link") {
                        KingsSpotifyImportState.showDialog = true
                    }
                }
                item(key = "liked") {
                    ActionTile(R.drawable.favorite, "Liked songs", "Your favourites", highlight = true) {
                        navController.navigate("auto_playlist/liked")
                    }
                }
                items(playlists.take(20), key = { it.id }) { playlist ->
                    LibraryPlaylistGridItem(
                        menuState = menuState,
                        coroutineScope = coroutineScope,
                        playlist = playlist,
                        modifier = Modifier.width(140.dp),
                    )
                }
            }
        }

        if (artists.isNotEmpty()) {
            item(key = "kp_artists_title") { SectionTitle("Artists") { onShowAll(LibraryFilter.ARTISTS) } }
            item(key = "kp_artists") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(artists.take(20), key = { it.id }) { artist ->
                        LibraryArtistGridItem(
                            menuState = menuState,
                            coroutineScope = coroutineScope,
                            artist = artist,
                            modifier = Modifier.width(130.dp),
                        )
                    }
                }
            }
        }

        item(key = "kp_more_title") { SectionTitle("More", onShowAll = null) }
        item(key = "kp_more") {
            RowCard(
                modifier = Modifier.padding(horizontal = 18.dp),
                rows = listOf(
                    Triple(R.drawable.music_note, "Songs", "Everything in your library") to { onShowAll(LibraryFilter.SONGS) },
                    Triple(R.drawable.album, "Albums", "Saved albums") to { onShowAll(LibraryFilter.ALBUMS) },
                    Triple(R.drawable.mic, "Podcasts", "Shows and episodes") to { onShowAll(LibraryFilter.PODCASTS) },
                ),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCardView(card: KingsStatCard, name: String, since: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(card.colors))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
            .padding(18.dp),
    ) {
        Column {
            Text("YOUR LISTENING EXPERIENCE", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                card.big,
                color = Color.White,
                fontSize = if (card.big.length > 12) 22.sp else 32.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(card.label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(card.footer, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("MEMBER SINCE", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, letterSpacing = 1.sp)
                Text(since, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RowCard(
    modifier: Modifier = Modifier,
    rows: List<Pair<Triple<Int, String, String>, () -> Unit>>,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        rows.forEachIndexed { index, (info, onClick) ->
            val (icon, title, subtitle) = info
            if (index > 0) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                ) {
                    Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    painterResource(R.drawable.navigate_next),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, onShowAll: (() -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 12.dp),
    ) {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        if (onShowAll != null) {
            Text(
                "Show all",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onShowAll).padding(6.dp),
            )
        }
    }
}

@Composable
private fun ActionTile(icon: Int, title: String, subtitle: String, highlight: Boolean = false, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (highlight) {
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
                    } else {
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHigh))
                    }
                ),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(if (highlight) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)),
            ) {
                Icon(
                    painterResource(icon),
                    contentDescription = null,
                    tint = if (highlight) Color.White else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
        Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
