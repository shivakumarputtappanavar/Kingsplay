/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Home order: a fixed order chosen by Kings Play, which people can change in
 * Settings > Home order. Also two new Home sections: Recents (big cards) and Listen together.
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalNavController
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.KingsQuickPickCard
import com.metrolist.music.ui.component.KingsQuickPickWidth
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.utils.rememberPreference
import java.time.DayOfWeek
import java.time.LocalDate

/** Saved order (keys separated by commas). Empty = Kings Play's default order. */
val KingsHomeOrderKey = stringPreferencesKey("kingsHomeOrder")

enum class KingsHomeSlot(val key: String, val label: String) {
    RECENTS("recents", "Recents"),
    ALBUMS_SINGLES("albums_singles", "Albums & singles"),
    QUICK_PICKS("quick_picks", "Quick picks"),
    NEW_RELEASES("new_releases", "New releases"),
    TRENDING("trending", "Trending songs for you"),
    MIXES("mixes", "Mixed playlists"),
    DAILY_MIXES("daily_mixes", "Kings Mixes for today"),
    LISTEN_TOGETHER("listen_together", "Listen together"),
    KEEP_LISTENING("keep_listening", "Keep listening"),
    FORGOTTEN("forgotten", "Forgotten favorites"),
    DAILY_DISCOVER("daily_discover", "Daily discover"),
    ACCOUNT_PLAYLISTS("account_playlists", "Your playlists"),
    COMMUNITY("community", "From the community"),
    SIMILAR("similar", "Similar to what you like"),
    OTHER_YOUTUBE("other_youtube", "Other sections"),
    MOODS("moods", "Moods & genres"),
    SPEED_DIAL("speed_dial", "Speed dial"),
}

fun kingsHomeOrder(saved: String): List<KingsHomeSlot> {
    val fromSaved = saved.split(',').mapNotNull { k -> KingsHomeSlot.entries.firstOrNull { it.key == k.trim() } }.distinct()
    return fromSaved + KingsHomeSlot.entries.filter { it !in fromSaved }
}

private fun slotOf(section: HomeSection, titles: List<String>): KingsHomeSlot = when (section) {
    HomeSection.KingsRecents -> KingsHomeSlot.RECENTS
    HomeSection.KingsListenTogether -> KingsHomeSlot.LISTEN_TOGETHER
    HomeSection.KingsDailyMixes -> KingsHomeSlot.DAILY_MIXES
    HomeSection.SpeedDial -> KingsHomeSlot.SPEED_DIAL
    HomeSection.QuickPicks -> KingsHomeSlot.QUICK_PICKS
    HomeSection.KeepListening -> KingsHomeSlot.KEEP_LISTENING
    HomeSection.ForgottenFavorites -> KingsHomeSlot.FORGOTTEN
    HomeSection.DailyDiscover -> KingsHomeSlot.DAILY_DISCOVER
    HomeSection.AccountPlaylists -> KingsHomeSlot.ACCOUNT_PLAYLISTS
    HomeSection.FromTheCommunity -> KingsHomeSlot.COMMUNITY
    HomeSection.MoodAndGenres -> KingsHomeSlot.MOODS
    is HomeSection.SimilarRecommendation -> KingsHomeSlot.SIMILAR
    is HomeSection.HomePageSection -> {
        // YouTube Music's own Home sections, recognised by their title
        val t = titles.getOrNull(section.index).orEmpty().lowercase()
        when {
            "release" in t || "new music" in t -> KingsHomeSlot.NEW_RELEASES
            "album" in t || "single" in t -> KingsHomeSlot.ALBUMS_SINGLES
            "trending" in t || "chart" in t || "top songs" in t || "hits" in t -> KingsHomeSlot.TRENDING
            "mix" in t -> KingsHomeSlot.MIXES
            "listen again" in t || "recent" in t -> KingsHomeSlot.KEEP_LISTENING
            else -> KingsHomeSlot.OTHER_YOUTUBE
        }
    }
    else -> KingsHomeSlot.OTHER_YOUTUBE
}

/**
 * Puts the Home sections in the fixed Kings Play order:
 * Recents, Albums & singles, Quick picks, New releases, Trending songs for you,
 * Mixed playlists, Listen together, Keep listening, Forgotten favorites, then the rest.
 * Speed dial is not shown.
 */
@Suppress("UNUSED_PARAMETER")
fun kingsSortHomeSections(list: List<HomeSection>, titles: List<String>, saved: String): List<HomeSection> {
    val order = KingsHomeSlot.entries
    return list
        .filter { it != HomeSection.SpeedDial }
        .sortedBy { order.indexOf(slotOf(it, titles)) }
}

// ---------------------------------------------------------------- Recents (big cards)

@Composable
fun KingsRecentsSection() {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val today = remember { LocalDate.now() }
    val thisMonday = remember { today.with(DayOfWeek.MONDAY) }
    val events by remember {
        database.historyEvents(
            tomorrowStart = today.plusDays(1).atStartOfDay(),
            todayStart = today.atStartOfDay(),
            yesterdayStart = today.minusDays(1).atStartOfDay(),
            thisMondayStart = thisMonday.atStartOfDay(),
            lastMondayStart = thisMonday.minusDays(7).atStartOfDay(),
        )
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val songs = remember(events) { events.map { it.song }.distinctBy { it.id }.take(15) }
    if (songs.isEmpty()) return

    Column {
        Text(
            "Recents",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 10.dp),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
            itemsIndexed(songs, key = { _, s -> "kings_recent_${s.id}" }) { index, song ->
                KingsQuickPickCard(
                    song = song,
                    modifier = Modifier
                        .size(width = KingsQuickPickWidth, height = 222.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = "Recents",
                                    items = songs.map { it.toMediaItem() },
                                    startIndex = index,
                                )
                            )
                        },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Listen together card

@Composable
fun KingsListenTogetherCard() {
    val navController = LocalNavController.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f))
                )
            )
            .clickable { navController.navigate("listen_together") }
            .padding(18.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
        ) {
            Icon(painterResource(R.drawable.group_filled), contentDescription = null, tint = Color.White)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text("Listen together", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text("Play the same song with friends, in real time", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        }
        Icon(painterResource(R.drawable.navigate_next), contentDescription = null, tint = Color.White)
    }
}

// ---------------------------------------------------------------- Settings > Home order

@Composable
fun KingsHomeOrderSettingsGroup() {
    val (saved, onSavedChange) = rememberPreference(KingsHomeOrderKey, "")
    var showDialog by remember { mutableStateOf(false) }

    Material3SettingsGroup(
        title = "Home",
        items = listOf(
            Material3SettingsItem(
                icon = painterResource(R.drawable.home_outlined),
                title = { Text("Home order") },
                description = { Text(if (saved.isBlank()) "Kings Play order" else "Your own order") },
                onClick = { showDialog = true },
            )
        )
    )

    if (showDialog) {
        var order by remember { mutableStateOf(kingsHomeOrder(saved)) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Home order") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 460.dp)) {
                    itemsIndexed(order, key = { _, s -> s.key }) { index, slot ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        ) {
                            Text(
                                "${index + 1}.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(slot.label, modifier = Modifier.weight(1f))
                            IconButton(
                                enabled = index > 0,
                                onClick = {
                                    order = order.toMutableList().apply { add(index - 1, removeAt(index)) }
                                },
                            ) { Icon(painterResource(R.drawable.arrow_upward), contentDescription = "Move up") }
                            IconButton(
                                enabled = index < order.lastIndex,
                                onClick = {
                                    order = order.toMutableList().apply { add(index + 1, removeAt(index)) }
                                },
                            ) { Icon(painterResource(R.drawable.arrow_downward), contentDescription = "Move down") }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSavedChange(order.joinToString(",") { it.key })
                    showDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        onSavedChange("")
                        showDialog = false
                    }) { Text("Reset") }
                    TextButton(onClick = { showDialog = false }) { Text("Cancel") }
                }
            },
        )
    }
}
