/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Main Settings: "Display" group with a 60 Hz / 120 Hz refresh rate choice.
 * Uses Metrolist's existing high-refresh-rate setting, so both places stay in sync.
 */

package com.metrolist.music.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.view.WindowManager
import com.metrolist.music.R
import com.metrolist.music.constants.EnableHighRefreshRateKey
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.utils.rememberPreference
import kotlin.math.roundToInt

@Composable
fun KingsDisplaySettingsGroup() {
    val context = LocalContext.current
    val (highRefresh, onHighRefreshChange) = rememberPreference(EnableHighRefreshRateKey, defaultValue = true)

    // Fastest refresh rate this phone supports (for example 90, 120 or 144)
    val maxHz = remember {
        runCatching {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            }
            display?.supportedModes?.maxOfOrNull { it.refreshRate }?.roundToInt() ?: 60
        }.getOrDefault(60)
    }
    val supportsHigh = maxHz > 61

    Material3SettingsGroup(
        title = "Display",
        items = listOf(
            Material3SettingsItem(
                icon = painterResource(R.drawable.speed),
                title = { Text("Refresh rate") },
                description = {
                    Text(
                        when {
                            !supportsHigh -> "This phone runs at 60 Hz"
                            highRefresh -> "Extra smooth at $maxHz Hz (uses a bit more battery)"
                            else -> "Smooth 60 Hz, saves battery"
                        }
                    )
                },
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HzOption(
                            text = "60 Hz",
                            selected = !highRefresh || !supportsHigh,
                            onClick = { onHighRefreshChange(false) },
                        )
                        if (supportsHigh) {
                            HzOption(
                                text = "$maxHz Hz",
                                selected = highRefresh,
                                onClick = { onHighRefreshChange(true) },
                            )
                        }
                    }
                },
            )
        )
    )
}

@Composable
private fun HzOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.primary.copy(alpha = 0.22f) else colors.surfaceContainerHighest)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.primary else colors.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) colors.primary else colors.onSurface,
        )
    }
}
