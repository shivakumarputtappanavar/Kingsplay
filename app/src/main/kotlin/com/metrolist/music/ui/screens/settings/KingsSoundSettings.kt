/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Main Settings: "Sound" group with a Dolby Atmos item.
 * Dolby Atmos is part of the phone (for example Samsung), not of Kings Play.
 * This item checks if the phone has it and opens the phone's own sound effects
 * screen, where the user can turn Dolby Atmos on. It then works for Kings Play.
 */

package com.metrolist.music.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.metrolist.music.R
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem

/** True when the phone has a Dolby audio effect built in. */
private fun phoneHasDolby(): Boolean = runCatching {
    AudioEffect.queryEffects()?.any { d ->
        d.implementor.contains("Dolby", ignoreCase = true) || d.name.contains("Dolby", ignoreCase = true)
    } == true
}.getOrDefault(false)

private fun Context.tryStart(intent: Intent): Boolean =
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess

/**
 * Opens the phone's sound effects screen (where Dolby Atmos lives).
 * Returns what was opened, so we can show the right tip.
 */
private fun openPhoneSoundEffects(context: Context): String {
    // 1. The phone's own sound effects panel (Samsung: Sound quality and effects)
    val panel = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
        .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
    val handler = runCatching {
        context.packageManager.queryIntentActivities(panel, 0)
            .firstOrNull { it.activityInfo.packageName != context.packageName }
    }.getOrNull()
    if (handler != null) {
        panel.setClassName(handler.activityInfo.packageName, handler.activityInfo.name)
        if (context.tryStart(panel)) return "panel"
    }
    // 2. The phone's Sound settings
    if (context.tryStart(Intent(Settings.ACTION_SOUND_SETTINGS))) return "sound"
    // 3. Main settings
    if (context.tryStart(Intent(Settings.ACTION_SETTINGS))) return "settings"
    return "none"
}

@Composable
fun KingsSoundSettingsGroup() {
    val context = LocalContext.current
    val hasDolby = remember { phoneHasDolby() }
    val isSamsung = remember { Build.MANUFACTURER.equals("samsung", ignoreCase = true) }

    Material3SettingsGroup(
        title = "Sound",
        items = listOf(
            Material3SettingsItem(
                icon = painterResource(R.drawable.graphic_eq),
                title = { Text("Dolby Atmos") },
                description = {
                    Text(
                        if (hasDolby) {
                            "Your phone has Dolby Atmos. Tap to turn it on for richer, 3D sound in Kings Play"
                        } else {
                            "Tap to open your phone's sound effects (Dolby Atmos, if your phone has it)"
                        }
                    )
                },
                onClick = {
                    val tip = when (openPhoneSoundEffects(context)) {
                        "panel" -> "Turn on Dolby Atmos here"
                        "sound", "settings" -> if (isSamsung) {
                            "Go to Sound quality and effects → Dolby Atmos → On"
                        } else {
                            "Look for Sound effects or Dolby Atmos"
                        }
                        else -> "Your phone has no sound effects screen"
                    }
                    Toast.makeText(context, tip, Toast.LENGTH_LONG).show()
                },
            )
        )
    )
}
