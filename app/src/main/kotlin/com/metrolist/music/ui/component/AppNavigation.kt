/**
 * Metrolist Project (C) 2026
 * Kings Play changes: liquid glass bottom bar with a sliding liquid bubble
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.metrolist.music.ui.theme.LocalKingsBackdrop
import com.metrolist.music.ui.theme.kingsGlass
import com.metrolist.music.ui.theme.rememberLiquidGlassEnabled
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.metrolist.music.ui.screens.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.metrolist.music.constants.EnableHighRefreshRateKey
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (navigationItems.any { it.route == screenRoute } &&
        currentRoute.startsWith("$screenRoute/")) return true

    // Fix: match the route template, not the resolved route
    if (screenRoute == "search_input" &&
        (currentRoute.startsWith("search/") || currentRoute == "search/{query}")) return true

    return false
}

@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
) {
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    NavigationRail(
        modifier = modifier,
        containerColor = containerColor
    ) {
        Spacer(modifier = Modifier.weight(1f))

        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
            val interactionSource = remember { MutableInteractionSource() }

            // Long press detection using InteractionSource
            if (isSearchItem || isHomeHoldItem) {
                LaunchedEffect(interactionSource) {
                    var isLongClick = false
                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                isLongClick = false
                                delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                                isLongClick = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isHomeHoldItem) onHomeLongHold.invoke() else onSearchLongClick?.invoke()
                            }
                            is PressInteraction.Release -> {
                                if (!isLongClick) {
                                    onItemClick(screen, currentIsSelected)
                                }
                            }
                            is PressInteraction.Cancel -> {
                                isLongClick = false
                            }
                        }
                    }
                }
            }

            NavigationRailItem(
                selected = isSelected,
                onClick = {
                    if (!isSearchItem && !isHomeHoldItem) {
                        onItemClick(screen, currentIsSelected)
                    }
                    // Long presses are handled via InteractionSource
                },
                interactionSource = interactionSource,
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(screen.titleId)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MetrolistNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
) {
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    NavigationBar(
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor
    ) {
        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
            val interactionSource = remember { MutableInteractionSource() }

            // Long press detection using InteractionSource
            if (isSearchItem || isHomeHoldItem) {
                LaunchedEffect(interactionSource) {
                    var isLongClick = false
                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                isLongClick = false
                                delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                                isLongClick = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isHomeHoldItem) onHomeLongHold.invoke() else onSearchLongClick?.invoke()
                            }
                            is PressInteraction.Release -> {
                                if (!isLongClick) {
                                    onItemClick(screen, currentIsSelected)
                                }
                            }
                            is PressInteraction.Cancel -> {
                                isLongClick = false
                            }
                        }
                    }
                }
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSearchItem && !isHomeHoldItem) {
                        onItemClick(screen, currentIsSelected)
                    }
                    // Long presses are handled via InteractionSource
                },
                interactionSource = interactionSource,
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(screen.titleId)
                    )
                },
                label = if (!slimNav) {
                    {
                        Text(
                            text = stringResource(screen.titleId),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else null
            )
        }
    }
}

@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
) {
    val backdrop = LocalKingsBackdrop.current
    if (rememberLiquidGlassEnabled() && backdrop != null) {
        GlassNavigationBar(
            backdrop = backdrop,
            navigationItems = navigationItems,
            currentRoute = currentRoute,
            onItemClick = onItemClick,
            modifier = modifier,
            pureBlack = pureBlack,
            slimNav = slimNav,
            onSearchLongClick = onSearchLongClick,
            onHomeLongHold = onHomeLongHold,
        )
    } else {
        MetrolistNavigationBar(
            navigationItems = navigationItems,
            currentRoute = currentRoute,
            onItemClick = onItemClick,
            modifier = modifier,
            pureBlack = pureBlack,
            slimNav = slimNav,
            onSearchLongClick = onSearchLongClick,
            onHomeLongHold = onHomeLongHold,
        )
    }
}

// Kings Play: floating liquid glass bar with a sliding liquid bubble.
// Tap a tab: the bubble lifts, slides like liquid (stretching with speed), bends what is
// behind it, then settles. Press and drag across the bar: the bubble follows your finger.
@Composable
private fun GlassNavigationBar(
    backdrop: LayerBackdrop,
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier,
    pureBlack: Boolean,
    slimNav: Boolean,
    onSearchLongClick: (() -> Unit)?,
    onHomeLongHold: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val pillShape = RoundedCornerShape(33.dp)
    val bubbleShape = RoundedCornerShape(50)
    val pillHeight = if (slimNav) 58.dp else 66.dp
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // 120 Hz: full effect with rainbow glass edges. 60 Hz: lighter, same feel.
    val fullFx = rememberPreference(EnableHighRefreshRateKey, defaultValue = true).value

    // Search gets its own round glass button next to the bar (like iPhone)
    val searchItem = navigationItems.firstOrNull { it == Screens.Search }
    val tabItems = navigationItems.filter { it != Screens.Search }
    val count = tabItems.size.coerceAtLeast(1)
    val selectedIndex = tabItems.indexOfFirst { isRouteSelected(currentRoute, it.route, navigationItems) }
    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentItems by rememberUpdatedState(tabItems)
    val currentOnItemClick by rememberUpdatedState(onItemClick)

    val position = remember { Animatable(selectedIndex.coerceAtLeast(0).toFloat()) }
    val lift = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    var dragPos by remember { mutableFloatStateOf(0f) }
    var barWidthPx by remember { mutableIntStateOf(0) }
    val sidePadPx = with(density) { 6.dp.toPx() }
    val itemWidthPx = if (barWidthPx > 0) (barWidthPx - 2 * sidePadPx) / count else 0f

    // Liquid slide when the selected tab changes
    LaunchedEffect(selectedIndex) {
        if (selectedIndex < 0 || dragging) return@LaunchedEffect
        if (abs(position.value - selectedIndex) < 0.01f && lift.value < 0.01f) return@LaunchedEffect
        coroutineScope {
            launch { lift.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 900f)) }
            position.animateTo(selectedIndex.toFloat(), spring(dampingRatio = 0.72f, stiffness = 330f))
        }
        lift.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 420f))
    }

    val pos = if (dragging) dragPos else position.value
    val l = lift.value
    val stretch = if (dragging) 0.06f * l else (abs(position.velocity) * 0.035f).coerceAtMost(0.28f)
    val restFill = if (dark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.55f)
    val activeIndex = if (dragging || l > 0.01f) pos.roundToInt() else selectedIndex

    Box(modifier = modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(start = 14.dp, end = 14.dp, top = 6.dp)
            .fillMaxWidth()
            .height(pillHeight),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .onSizeChanged { barWidthPx = it.width }
        ) {
            // The glass bar
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .kingsGlass(
                        backdrop = backdrop,
                        shape = pillShape,
                        surface = if (pureBlack) Color.Black else colors.surfaceContainer,
                        tint = colors.primary,
                        dark = dark,
                    )
            )

            // The liquid bubble
            if ((selectedIndex >= 0 || dragging) && itemWidthPx > 0f) {
                val pillHeightPx = with(density) { pillHeight.toPx() }
                val restW = itemWidthPx - with(density) { 4.dp.toPx() }
                val restH = pillHeightPx - with(density) { 10.dp.toPx() }
                val w = restW * (1f + 0.16f * l + stretch)
                val h = restH * (1f + 0.24f * l - 0.35f * stretch)
                val cx = sidePadPx + (pos + 0.5f) * itemWidthPx
                val bubbleBase = Modifier
                    .offset { IntOffset((cx - w / 2f).roundToInt(), ((pillHeightPx - h) / 2f).roundToInt()) }
                    .size(with(density) { w.toDp() }, with(density) { h.toDp() })
                val bubble = if (l > 0.01f) {
                    // Moving: real liquid glass that bends what is behind it
                    bubbleBase
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { bubbleShape },
                            effects = {
                                vibrancy()
                                blur(4f.dp.toPx())
                                lens(
                                    refractionHeight = (8f + 8f * l).dp.toPx(),
                                    refractionAmount = (14f + 22f * l).dp.toPx(),
                                    chromaticAberration = fullFx,
                                )
                            },
                            onDrawSurface = {
                                drawRect(restFill.copy(alpha = restFill.alpha * (1f - 0.7f * l)))
                            },
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = (if (dark) 0.12f else 0.4f) + 0.45f * l),
                            shape = bubbleShape,
                        )
                } else {
                    // Resting: a soft pill (cheap to draw, saves battery)
                    bubbleBase
                        .clip(bubbleShape)
                        .background(restFill)
                }
                Box(modifier = bubble)
            }

            // Tabs (tap, or press and drag across them)
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 6.dp)
                    .pointerInput(count, itemWidthPx) {
                        if (itemWidthPx <= 0f) return@pointerInput
                        var lastTick = -1
                        fun finish() {
                            val target = dragPos.roundToInt().coerceIn(0, count - 1)
                            scope.launch {
                                position.snapTo(dragPos)
                                dragging = false
                                val items = currentItems
                                if (target < items.size) {
                                    val alreadySelected = target == currentSelected
                                    currentOnItemClick(items[target], alreadySelected)
                                    if (alreadySelected) {
                                        position.animateTo(target.toFloat(), spring(dampingRatio = 0.72f, stiffness = 380f))
                                        lift.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 420f))
                                    }
                                }
                            }
                        }
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                dragPos = (offset.x / itemWidthPx - 0.5f).coerceIn(0f, count - 1f)
                                lastTick = dragPos.roundToInt()
                                dragging = true
                                scope.launch { lift.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 700f)) }
                            },
                            onHorizontalDrag = { change, dx ->
                                change.consume()
                                dragPos = (dragPos + dx / itemWidthPx).coerceIn(0f, count - 1f)
                                val tick = dragPos.roundToInt()
                                if (tick != lastTick) {
                                    lastTick = tick
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onDragEnd = { finish() },
                            onDragCancel = { finish() },
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabItems.forEachIndexed { index, screen ->
                    val isSelected = index == activeIndex
                    val currentIsSelected by rememberUpdatedState(index == selectedIndex)
                    val iconRes = remember(isSelected, screen) {
                        if (isSelected) screen.iconIdActive else screen.iconIdInactive
                    }

                    val isSearchItem = screen == Screens.Search && onSearchLongClick != null
                    val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
                    val interactionSource = remember { MutableInteractionSource() }

                    // Long press detection using InteractionSource
                    if (isSearchItem || isHomeHoldItem) {
                        LaunchedEffect(interactionSource) {
                            var isLongClick = false
                            interactionSource.interactions.collectLatest { interaction ->
                                when (interaction) {
                                    is PressInteraction.Press -> {
                                        isLongClick = false
                                        delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                                        isLongClick = true
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (isHomeHoldItem) onHomeLongHold.invoke() else onSearchLongClick?.invoke()
                                    }
                                    is PressInteraction.Release -> {
                                        if (!isLongClick) {
                                            onItemClick(screen, currentIsSelected)
                                        }
                                    }
                                    is PressInteraction.Cancel -> {
                                        isLongClick = false
                                    }
                                }
                            }
                        }
                    }

                    val itemColor by animateColorAsState(
                        targetValue = if (isSelected) colors.onSurface else colors.onSurface.copy(alpha = 0.72f),
                        label = "kpGlassItemColor"
                    )
                    // Icons under the moving bubble grow, like a magnifying drop of water
                    val near = (1f - abs(index - pos)).coerceIn(0f, 1f)
                    val magnify = 1f + 0.22f * l * near

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {
                                    if (!isSearchItem && !isHomeHoldItem) {
                                        onItemClick(screen, currentIsSelected)
                                    }
                                    // Long presses are handled via InteractionSource
                                }
                            )
                            .graphicsLayer {
                                scaleX = magnify
                                scaleY = magnify
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = stringResource(screen.titleId),
                            tint = itemColor,
                            modifier = Modifier.size(24.dp)
                        )
                        if (!slimNav) {
                            Text(
                                text = stringResource(screen.titleId),
                                color = itemColor,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Round glass Search button
        if (searchItem != null) {
            KingsGlassSearchButton(
                backdrop = backdrop,
                screen = searchItem,
                selected = isRouteSelected(currentRoute, searchItem.route, navigationItems),
                size = pillHeight,
                dark = dark,
                pureBlack = pureBlack,
                onClick = { alreadySelected -> onItemClick(searchItem, alreadySelected) },
                onLongClick = onSearchLongClick,
            )
        }
      }
    }
}

@Composable
private fun KingsGlassSearchButton(
    backdrop: LayerBackdrop,
    screen: Screens,
    selected: Boolean,
    size: androidx.compose.ui.unit.Dp,
    dark: Boolean,
    pureBlack: Boolean,
    onClick: (Boolean) -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val currentSelected by rememberUpdatedState(selected)
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "kpSearchScale",
    )

    // Tap = open search, long press = Metrolist's long-press action
    LaunchedEffect(interactionSource, onLongClick) {
        var isLongClick = false
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isLongClick = false
                    if (onLongClick != null) {
                        delay(viewConfiguration.longPressTimeoutMillis)
                        isLongClick = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                }
                is PressInteraction.Release -> {
                    if (!isLongClick) onClick(currentSelected)
                }
                is PressInteraction.Cancel -> isLongClick = false
            }
        }
    }

    val iconRes = if (selected) screen.iconIdActive else screen.iconIdInactive
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .kingsGlass(
                backdrop = backdrop,
                shape = CircleShape,
                surface = if (pureBlack) Color.Black else colors.surfaceContainer,
                tint = colors.primary,
                dark = dark,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(size - 10.dp)
                    .clip(CircleShape)
                    .background(if (dark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.55f))
            )
        }
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = stringResource(screen.titleId),
            tint = if (selected) colors.onSurface else colors.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.size(26.dp),
        )
    }
}
