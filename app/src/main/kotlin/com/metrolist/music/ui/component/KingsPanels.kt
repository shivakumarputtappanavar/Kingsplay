/**
 * Kings Play - helpers that turn Home section data into cards for the carousel panel.
 */

package com.metrolist.music.ui.component

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.db.entities.Song

/** Cards from YouTube Music items: songs play, everything else opens (album / artist / playlist). */
fun kingsYtCards(
    items: List<YTItem>,
    onSong: (SongItem) -> Unit,
    onOther: (YTItem) -> Unit,
): List<KgCard> =
    items.map { item ->
        val subtitle = when (item) {
            is SongItem -> item.artists.joinToString(", ") { it.name }
            is AlbumItem -> item.artists?.joinToString(", ") { it.name }.orEmpty()
            is PlaylistItem -> item.author?.name.orEmpty()
            else -> ""
        }
        KgCard(
            key = item.id,
            thumbnail = item.thumbnail,
            title = item.title,
            subtitle = subtitle,
            onClick = { if (item is SongItem) onSong(item) else onOther(item) },
        )
    }

/** Cards from songs in your library; tapping plays the list from that song. */
fun kingsSongCards(
    songs: List<Song>,
    onPlay: (List<Song>, Int) -> Unit,
): List<KgCard> =
    songs.mapIndexed { index, song ->
        KgCard(
            key = song.id,
            thumbnail = song.song.thumbnailUrl,
            title = song.song.title,
            subtitle = song.artists.joinToString(", ") { it.name },
            onClick = { onPlay(songs, index) },
        )
    }