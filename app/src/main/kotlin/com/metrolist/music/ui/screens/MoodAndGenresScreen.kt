/**
 * Metrolist Project (C) 2026
 * Kings Play changes: redesigned Explore page (big picture cards)
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.pages.MoodAndGenres
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.viewmodels.MoodAndGenresViewModel

@Composable
fun MoodAndGenresScreen(
    navController: NavController,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val localConfiguration = LocalConfiguration.current
    val itemsPerRow = if (localConfiguration.orientation == ORIENTATION_LANDSCAPE) 3 else 2

    val moodAndGenresList by viewModel.moodAndGenres.collectAsStateWithLifecycle()
    val covers by viewModel.covers.collectAsStateWithLifecycle()

    // No top bar on this tab: start just below the status bar, keep the player-aware bottom space
    val layoutDirection = LocalLayoutDirection.current
    val baseInsets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val contentPadding = PaddingValues(
        start = baseInsets.calculateStartPadding(layoutDirection),
        end = baseInsets.calculateEndPadding(layoutDirection),
        top = statusTop + 12.dp,
        bottom = baseInsets.calculateBottomPadding() + 16.dp,
    )

    val openItem: (MoodAndGenres.Item) -> Unit = {
        navController.navigate("youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item(key = "explore_header") {
            ExploreHeader()
        }

        if (moodAndGenresList == null) {
            item(key = "mood_and_genres_shimmer") {
                ShimmerHost(
                    modifier = Modifier.animateItem()
                ) {
                    repeat(8) {
                        ListItemPlaceHolder()
                    }
                }
            }
        }

        moodAndGenresList?.forEachIndexed { sectionIndex, section ->
            item(key = "mood_and_genres_title_$sectionIndex") {
                SectionTitle(section.title)
            }

            // The very first card is a big "featured" card across the full width
            val items = section.items
            val featured = if (sectionIndex == 0) items.firstOrNull() else null
            val gridItems = if (featured != null) items.drop(1) else items

            if (featured != null) {
                item(key = "mood_and_genres_featured") {
                    LaunchedEffect(featured) { viewModel.loadCover(featured) }
                    ExploreCard(
                        item = featured,
                        coverUrl = covers[MoodAndGenresViewModel.coverKey(featured)],
                        height = 160.dp,
                        featured = true,
                        onClick = { openItem(featured) },
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .fillMaxWidth(),
                    )
                }
            }

            gridItems.chunked(itemsPerRow).forEachIndexed { rowIndex, row ->
                item(key = "mood_and_genres_row_${sectionIndex}_$rowIndex") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                    ) {
                        row.forEach { item ->
                            LaunchedEffect(item) { viewModel.loadCover(item) }
                            ExploreCard(
                                item = item,
                                coverUrl = covers[MoodAndGenresViewModel.coverKey(item)],
                                height = 112.dp,
                                featured = false,
                                onClick = { openItem(item) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(6.dp),
                            )
                        }
                        repeat(itemsPerRow - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreHeader() {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)) {
        Text(
            text = "Explore",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = colors.onBackground,
        )
        Text(
            text = "A sound for every mood and moment",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        // Small accent line in the app's colors
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.horizontalGradient(listOf(colors.primary, colors.secondary, colors.tertiary)))
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun ExploreCard(
    item: MoodAndGenres.Item,
    coverUrl: String?,
    height: Dp,
    featured: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stripe = Color(item.stripeColor)
    val shape = RoundedCornerShape(if (featured) 28.dp else 22.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "exploreCardPress")

    Box(
        modifier = modifier
            .height(height)
            .scale(pressScale)
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(stripe, stripe.copy(alpha = 0.55f), Color.Black))
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        // Cover picture from the first playlist of this mood/genre
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Color wash: strong on the left (for the text), see-through on the right (to show the picture)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to stripe.copy(alpha = 0.95f),
                        0.45f to stripe.copy(alpha = 0.70f),
                        1.0f to stripe.copy(alpha = 0.15f),
                    )
                )
        )
        // Soft dark fade at the bottom for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.5f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.35f),
                    )
                )
        )

        Text(
            text = item.title,
            style = TextStyle(
                shadow = Shadow(color = Color.Black.copy(alpha = 0.45f), blurRadius = 8f),
            ),
            color = Color.White,
            fontSize = if (featured) 28.sp else 18.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = if (featured) 32.sp else 21.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(if (featured) Alignment.BottomStart else Alignment.TopStart)
                .padding(if (featured) 20.dp else 14.dp)
                .fillMaxWidth(if (featured) 0.7f else 0.85f),
        )
    }
}

// ----- Small mood buttons used on the Home screen (unchanged from Metrolist) -----

@Composable
fun MoodAndGenresButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier =
        modifier
            .height(MoodAndGenresButtonHeight)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

val MoodAndGenresButtonHeight = 48.dp
