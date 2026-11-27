/**
 * Kings Play - "Daily mix" card for Home: today's songs made from your listening
 * (the same list as "Your daily discover"). Play it, or save it as a playlist.
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

@Composable
fun KingsDailyMixCard(
    covers: List<String?>,
    count: Int,
    onPlay: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var saved by remember { mutableStateOf(false) }
    val ink = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Daily mix",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(ink.copy(alpha = 0.08f))
                .border(0.8.dp, ink.copy(alpha = 0.2f), shape)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    KgMixCover(covers.getOrNull(0))
                    KgMixCover(covers.getOrNull(1))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    KgMixCover(covers.getOrNull(2))
                    KgMixCover(covers.getOrNull(3))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Made from your listening",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "$count songs, new every day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onPlay) { Text("Play") }
                    OutlinedButton(
                        enabled = !saved,
                        onClick = {
                            saved = true
                            onSave()
                        },
                    ) {
                        Text(if (saved) "Saved" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun KgMixCover(url: String?) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(10.dp)),
    )
}