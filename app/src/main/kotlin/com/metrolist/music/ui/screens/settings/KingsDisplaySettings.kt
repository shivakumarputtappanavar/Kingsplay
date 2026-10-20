/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Main Settings: "Display" group (60 Hz / 120 Hz) and "App icon & sound" group.
 * Uses Metrolist's existing high-refresh-rate setting, so both places stay in sync.
 */

package com.metrolist.music.ui.screens.settings

import android.os.Build
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metrolist.music.ui.theme.DefaultKingsThought
import com.metrolist.music.ui.theme.KingsThoughtKey
import com.metrolist.music.ui.theme.KingsThoughtMaxLength
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Switch
import androidx.compose.ui.layout.ContentScale
import com.metrolist.music.utils.KingsAppIcon
import com.metrolist.music.utils.KingsAppIconKey
import com.metrolist.music.utils.KingsAppIcons
import com.metrolist.music.utils.KingsStartupSoundKey
import com.metrolist.music.utils.KingsIntroKey
import com.metrolist.music.utils.KingsIntroState
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
                            highRefresh -> "Smart $maxHz Hz: ultra smooth while you touch and scroll, 60 Hz when idle to save battery"
                            else -> "Steady 60 Hz: smooth and saves the most battery"
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

// ============================================================================
// Kings Play: App icon (3 logos) + startup sound — shown at the end of Settings
// ============================================================================

@Composable
fun KingsAppIconGroup() {
    val context = LocalContext.current
    val (iconName, onIconNameChange) = rememberPreference(KingsAppIconKey, KingsAppIcon.DEFAULT.name)
    val (soundOn, onSoundOnChange) = rememberPreference(KingsStartupSoundKey, true)
    val (introOn, onIntroOnChange) = rememberPreference(KingsIntroKey, true)
    val (thought, onThoughtChange) = rememberPreference(KingsThoughtKey, DefaultKingsThought)
    var editThought by remember { mutableStateOf(false) }

    if (editThought) {
        ThoughtDialog(
            current = thought.ifBlank { DefaultKingsThought },
            onSave = {
                onThoughtChange(it.trim().ifBlank { DefaultKingsThought })
                editThought = false
            },
            onReset = {
                onThoughtChange(DefaultKingsThought)
                editThought = false
            },
            onDismiss = { editThought = false },
        )
    }
    val selected = KingsAppIcon.from(iconName)

    Material3SettingsGroup(
        title = "Personalize Kings Play",
        items = listOf(
            Material3SettingsItem(
                icon = painterResource(R.drawable.edit),
                title = { Text("Home thought") },
                description = { Text("\"${thought.ifBlank { DefaultKingsThought }}\" (shown animated at the top of Home and in the intro)") },
                onClick = { editThought = true },
            ),
            Material3SettingsItem(
                title = { Text("App icon") },
                description = {
                    Column {
                        Text("Choose your Kings Play logo. The home screen may take a few seconds to update.")
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            KingsAppIcon.entries.forEach { icon ->
                                IconChoice(
                                    icon = icon,
                                    selected = icon == selected,
                                    onClick = {
                                        if (icon != selected) {
                                            onIconNameChange(icon.name)
                                            KingsAppIcons.apply(context, icon)
                                            if (soundOn) KingsAppIcons.playSound(context, icon, force = true)
                                            Toast.makeText(context, "${icon.label} icon set", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                },
            ),
            Material3SettingsItem(
                icon = painterResource(R.drawable.volume_up),
                title = { Text("Startup sound") },
                description = { Text("Plays the logo's sound when Kings Play opens (not on silent, not while music plays)") },
                trailingContent = {
                    Switch(checked = soundOn, onCheckedChange = onSoundOnChange)
                },
            ),
            Material3SettingsItem(
                icon = painterResource(R.drawable.palette),
                title = { Text("Intro animation") },
                description = { Text("Animated logo and name when Kings Play opens (tap to skip)") },
                trailingContent = {
                    Switch(checked = introOn, onCheckedChange = onIntroOnChange)
                },
            ),
            Material3SettingsItem(
                icon = painterResource(R.drawable.play),
                title = { Text("Preview intro") },
                description = { Text("See and hear the ${selected.label} intro") },
                onClick = {
                    KingsIntroState.icon = selected
                    KingsAppIcons.playSound(context, selected, force = true)
                },
            ),
        )
    )
}

@Composable
private fun IconChoice(
    icon: KingsAppIcon,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Image(
            painter = painterResource(icon.preview),
            contentDescription = icon.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) colors.primary else colors.outlineVariant,
                    shape = RoundedCornerShape(18.dp),
                ),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = icon.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) colors.primary else colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun ThoughtDialog(
    current: String,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Home thought") },
        text = {
            Column {
                Text(
                    "Shown animated at the top of Home and in the intro.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { if (it.length <= KingsThoughtMaxLength) value = it },
                    singleLine = true,
                    placeholder = { Text(DefaultKingsThought) },
                    supportingText = { Text("${value.length} / $KingsThoughtMaxLength") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onReset) { Text("Reset to default") }
        },
    )
}
