/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState as rememberMaterialBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalMenuState = compositionLocalOf { MenuState() }

@Stable
class MenuState(
    isVisible: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    var isVisible by mutableStateOf(isVisible)
    var content by mutableStateOf(content)

    fun show(content: @Composable ColumnScope.() -> Unit) {
        isVisible = true
        this.content = content
    }

    fun dismiss() {
        isVisible = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedBottomSheet(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberMaterialBottomSheetState(initialValue = SheetValue.Hidden),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.modalWindowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetDefaults.properties,
    content: @Composable ColumnScope.() -> Unit,
) {
    var lastContent by remember { mutableStateOf(content) }

    LaunchedEffect(content) {
        if (isVisible) {
            lastContent = content
        }
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    if (!sheetState.isVisible && !isVisible) {
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor,
        dragHandle = dragHandle,
        contentWindowInsets = contentWindowInsets,
        properties = properties,
        content = lastContent,
    )
}

/**
 * 3-dot menu sheet. By default it uses the dark glass look.
 * Pass glass = false to get the old solid look back.
 */
@Composable
fun BottomSheetMenu(
    modifier: Modifier = Modifier,
    state: MenuState,
    background: Color = MaterialTheme.colorScheme.surface,
    glass: Boolean = true,
) {
    if (glass) {
        KingsGlassBottomSheetMenu(modifier = modifier, state = state)
    } else {
        ClassicBottomSheetMenu(modifier = modifier, state = state, background = background)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KingsGlassBottomSheetMenu(
    modifier: Modifier,
    state: MenuState,
) {
    val focusManager = LocalFocusManager.current
    val sheetState = rememberMaterialBottomSheetState(initialValue = SheetValue.Hidden)

    // Dark colours for everything inside the menu, so text and icons stay readable
    // in both light and dark app themes.
    val baseScheme = MaterialTheme.colorScheme
    val glassScheme =
        remember(baseScheme) {
            baseScheme.copy(
                surface = Color(0xFF16161A),
                onSurface = KingsGlassText,
                surfaceVariant = Color(0x24FFFFFF),
                onSurfaceVariant = Color(0xCCFFFFFF),
            )
        }

    AnimatedBottomSheet(
        isVisible = state.isVisible,
        onDismissRequest = {
            focusManager.clearFocus()
            state.isVisible = false
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color(0xE6101014),
        contentColor = KingsGlassText,
        scrimColor = Color.Black.copy(alpha = 0.40f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.35f))
            )
        },
        modifier = modifier.fillMaxHeight()
    ) {
        MaterialTheme(
            colorScheme = glassScheme,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
        ) {
            CompositionLocalProvider(LocalKingsGlass provides true) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x1AFFFFFF), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 14.dp)
                ) {
                    state.content(this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassicBottomSheetMenu(
    modifier: Modifier,
    state: MenuState,
    background: Color,
) {
    val focusManager = LocalFocusManager.current
    val sheetState = rememberMaterialBottomSheetState(initialValue = SheetValue.Hidden)

    AnimatedBottomSheet(
        isVisible = state.isVisible,
        onDismissRequest = {
            focusManager.clearFocus()
            state.isVisible = false
        },
        sheetState = sheetState,
        containerColor = background,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        },
        modifier = modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            state.content(this)
        }
    }
}
