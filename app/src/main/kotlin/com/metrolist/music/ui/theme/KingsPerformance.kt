/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Smooth + battery friendly display:
 *  - 60 Hz mode : steady 60 Hz all the time (saves the most battery)
 *  - 120 Hz mode: "Smart" — the phone's fastest rate while you touch / scroll
 *                 and when the app opens, then back to 60 Hz after 3 seconds of no touch.
 * Also a slow clock for decorative animations so they don't redraw the screen 120 times a second.
 */

package com.metrolist.music.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import com.metrolist.music.constants.EnableHighRefreshRateKey
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlin.math.abs

/** How long the screen stays at full speed after the last touch. */
private const val KingsFastHoldMillis = 3000L

private val kingsTouches = MutableSharedFlow<Unit>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
)

private fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** Switches the window between the fastest rate and 60 Hz (only when it really changes). */
@Suppress("DEPRECATION")
private fun Window.kingsApplyRate(fast: Boolean) {
    runCatching {
        val display = windowManager.defaultDisplay
        val params = attributes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val current = display.mode
            val sameSize = display.supportedModes.filter {
                it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight
            }
            val target = if (fast) {
                sameSize.maxByOrNull { it.refreshRate }
            } else {
                sameSize.minByOrNull { abs(it.refreshRate - 60f) }
            }
            val id = target?.modeId ?: 0
            if (params.preferredDisplayModeId == id) return
            params.preferredDisplayModeId = id
        } else {
            val rate = if (fast) (display.supportedRefreshRates.maxOrNull() ?: 0f) else 60f
            if (params.preferredRefreshRate == rate) return
            params.preferredRefreshRate = rate
        }
        attributes = params
    }
}

/** Watches every touch in the app (without blocking it) for the Smart 120 Hz mode. */
fun Modifier.kingsTouchWatcher(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial)
            kingsTouches.tryEmit(Unit)
        }
    }
}

/** Put once at the top of the app. Controls the refresh rate. */
@Composable
fun KingsSmartRefresh() {
    val highRefresh by rememberPreference(EnableHighRefreshRateKey, defaultValue = true)
    val view = LocalView.current
    LaunchedEffect(highRefresh) {
        val window = view.context.findActivity()?.window ?: return@LaunchedEffect
        if (!highRefresh) {
            window.kingsApplyRate(fast = false)
            return@LaunchedEffect
        }
        // Fast when the app opens and while touching; 60 Hz when idle
        kingsTouches.onStart { emit(Unit) }.collectLatest {
            window.kingsApplyRate(fast = true)
            delay(KingsFastHoldMillis)
            window.kingsApplyRate(fast = false)
        }
    }
}

/**
 * A slow clock (seconds) for background/decorative animations.
 * Updates [fps] times a second instead of every screen refresh, and pauses when the app is hidden.
 */
@Composable
fun rememberKingsSlowClock(fps: Int = 30): State<Float> {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(fps) {
        val start = System.nanoTime()
        val step = 1000L / fps.coerceIn(1, 60)
        while (true) {
            delay(step)
            withFrameNanos { now -> seconds.floatValue = (now - start) / 1_000_000_000f }
        }
    }
    return seconds
}
