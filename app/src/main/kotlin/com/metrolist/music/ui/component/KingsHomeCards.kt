/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Big square song card for Home "Quick picks" (like the new Library style).
 * Takes the same options as the normal song row, so it can replace it directly.
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.R
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import com.metrolist.music.ui.theme.kingsCoverGlow
import com.metrolist.music.ui.theme.rememberCoverGlowColor
import com.metrolist.music.db.entities.Song
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem

/** Size of one Quick picks card (artwork + two lines of text). */
val KingsQuickPickWidth = 162.dp
val KingsQuickPickHeight = 222.dp

@Composable
fun KingsQuickPickCard(
    song: Song,
    modifier: Modifier = Modifier,
    showInLibraryIcon: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isSwipeable: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier.padding(horizontal = 6.dp)) {
        Box(
            modifier = Modifier
                .kingsCoverGlow(rememberCoverGlowColor(song.song.thumbnailUrl), 14.dp)
                .size(150.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .then(
                    if (isActive) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                    else Modifier
                ),
        ) {
            AsyncImage(
                model = song.song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(150.dp),
            )
            // Kings Play: glass shine and edge
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.22f),
                            0.35f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.18f),
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp)),
            )
            if (isActive) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            if (song.song.liked) {
                Icon(
                    painter = painterResource(R.drawable.favorite),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(16.dp),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    song.song.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song.artists.joinToString(", ") { it.name },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(modifier = Modifier.size(width = 32.dp, height = 40.dp)) {
                trailingContent()
            }
        }
    }
}

/** Big square card for YouTube Music songs on Home (same look as Quick picks). */
@Composable
fun KingsYTSongCard(
    item: YTItem,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isSwipeable: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier.padding(horizontal = 6.dp)) {
        Box(
            modifier = Modifier
                .kingsCoverGlow(rememberCoverGlowColor(item.thumbnail), 14.dp)
                .size(150.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .then(
                    if (isActive) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                    else Modifier
                ),
        ) {
            AsyncImage(
                model = item.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(150.dp),
            )
            // Kings Play: glass shine and edge
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.22f),
                            0.35f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.18f),
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp)),
            )
            if (isActive) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    (item as? SongItem)?.artists?.joinToString(", ") { it.name }.orEmpty(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(modifier = Modifier.size(width = 32.dp, height = 40.dp)) {
                trailingContent()
            }
        }
    }
}

/** Soft glow in the cover's colour behind any Home card (albums, playlists, new releases). */
@Composable
fun kingsCardGlow(url: String?, corner: Dp = 16.dp): Modifier =
    Modifier.kingsCoverGlow(rememberCoverGlowColor(url), corner, strength = 0.8f)
