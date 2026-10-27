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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.queues.ListQueue
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

private enum class KingsStatKind { MINUTES, SONG, ARTIST, ALBUM }

private data class KingsStatCard(
    val kind: KingsStatKind,
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
    var openCard by remember { mutableStateOf<KingsStatKind?>(null) }
    val playerConnection = LocalPlayerConnection.current
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
        add(KingsStatCard(KingsStatKind.MINUTES, "%,d".format(minutes), "MINUTES LISTENED", "$plays plays", listOf(Color(0xFF7A1235), Color(0xFF2B0614))))
        topSongs.firstOrNull()?.let {
            add(KingsStatCard(KingsStatKind.SONG, it.title, "TOP SONG", "${it.artistName ?: ""} · ${it.songCountListened} plays", listOf(Color(0xFF3B2A7A), Color(0xFF120A33))))
        }
        topArtists.firstOrNull()?.let {
            val mins = (it.timeListened ?: 0) / 60000
            add(KingsStatCard(KingsStatKind.ARTIST, it.artist.name, "TOP ARTIST", "$mins min · ${it.songCount} songs", listOf(Color(0xFF0F5C6B), Color(0xFF062A33))))
        }
        topAlbums.firstOrNull()?.let {
            add(KingsStatCard(KingsStatKind.ALBUM, it.album.title, "TOP ALBUM", it.artists.joinToString(", ") { a -> a.name }, listOf(Color(0xFF7A4A0F), Color(0xFF2E1A04))))
        }
    }

    openCard?.let { kind ->
        KingsStatSheet(
            kind = kind,
            onDismiss = { openCard = null },
            songs = topSongs.map { s ->
                KingsStatRow(
                    title = s.title,
                    subtitle = listOfNotNull(
                        s.artistName,
                        "${s.songCountListened} plays",
                        "${(s.timeListened ?: 0L) / 60000L} min",
                    ).joinToString(" · "),
                    image = s.thumbnailUrl,
                    round = false,
                )
            }.let { rows -> if (kind == KingsStatKind.MINUTES) rows else rows },
            artists = topArtists.map { a ->
                KingsStatRow(a.artist.name, "${(a.timeListened ?: 0) / 60000} min · ${a.songCount} songs", a.artist.thumbnailUrl, round = true)
            },
            albums = topAlbums.map { al ->
                KingsStatRow(al.album.title, al.artists.joinToString(", ") { it.name }, al.album.thumbnailUrl, round = false)
            },
            onSongClick = { index ->
                val items = topSongs.map { s ->
                    MediaMetadata(
                        id = s.id,
                        title = s.title,
                        artists = listOfNotNull(s.artistName).map { MediaMetadata.Artist(id = null, name = it) },
                        duration = -1,
                        thumbnailUrl = s.thumbnailUrl,
                    ).toMediaItem()
                }
                playerConnection?.playQueue(ListQueue(title = "Your top songs", items = items, startIndex = index))
            },
            onArtistClick = { index -> topArtists.getOrNull(index)?.let { navController.navigate("artist/${it.id}"); openCard = null } },
            onAlbumClick = { index -> topAlbums.getOrNull(index)?.let { navController.navigate("album/${it.id}"); openCard = null } },
        )
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
                StatCardView(cards[page], name, since) { openCard = cards[page].kind }
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
private fun StatCardView(card: KingsStatCard, name: String, since: String, onClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 176.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(card.colors))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Column {
            Text(
                "YOUR LISTENING EXPERIENCE",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                card.big,
                color = Color.White,
                fontSize = if (card.big.length > 12) 22.sp else 32.sp,
                lineHeight = if (card.big.length > 12) 26.sp else 36.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                card.label,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(card.footer, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                Text("MEMBER SINCE", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, letterSpacing = 1.sp, maxLines = 1)
                Text(since, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

private data class KingsStatRow(val title: String, val subtitle: String, val image: String?, val round: Boolean)

/** Opens when a listening card is tapped: shows what is behind that card. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KingsStatSheet(
    kind: KingsStatKind,
    onDismiss: () -> Unit,
    songs: List<KingsStatRow>,
    artists: List<KingsStatRow>,
    albums: List<KingsStatRow>,
    onSongClick: (Int) -> Unit,
    onArtistClick: (Int) -> Unit,
    onAlbumClick: (Int) -> Unit,
) {
    val (title, rows, onRowClick) = when (kind) {
        KingsStatKind.MINUTES -> Triple("Everything you listened to", songs, onSongClick)
        KingsStatKind.SONG -> Triple("Your top songs", songs, onSongClick)
        KingsStatKind.ARTIST -> Triple("Your top artists", artists, onArtistClick)
        KingsStatKind.ALBUM -> Triple("Your top albums", albums, onAlbumClick)
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 20.dp, bottom = 10.dp),
        )
        if (rows.isEmpty()) {
            Text(
                "Play some music and it will show here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
        }
        LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
            itemsIndexed(rows) { index, row ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRowClick(index) }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text(
                        "${index + 1}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(28.dp),
                    )
                    AsyncImage(
                        model = row.image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(if (row.round) CircleShape else RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(row.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            row.subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
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
