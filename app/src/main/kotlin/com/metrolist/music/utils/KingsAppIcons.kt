/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * App icon choice (3 Kings Play logos), the sound and the animated intro when the app opens.
 */

package com.metrolist.music.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metrolist.music.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

val KingsAppIconKey = stringPreferencesKey("kingsAppIcon")
val KingsStartupSoundKey = booleanPreferencesKey("kingsStartupSound")
val KingsIntroKey = booleanPreferencesKey("kingsIntro")

/** Which logo the animated intro should show right now (null = no intro on screen). */
object KingsIntroState {
    var icon by mutableStateOf<KingsAppIcon?>(null)
}

enum class KingsAppIcon(
    val label: String,
    val aliasClass: String,
    @DrawableRes val preview: Int,
    @RawRes val sound: Int,
) {
    DEFAULT("Kings Play", "com.metrolist.music.MainActivityAlias", R.drawable.kp_logo1_preview, R.raw.kp_sound_default),
    ROYAL("Royal Dragon", "com.metrolist.music.KingsIconRoyal", R.drawable.kp_logo2_preview, R.raw.kp_sound_royal),
    CHESS("King's Move", "com.metrolist.music.KingsIconChess", R.drawable.kp_logo3_preview, R.raw.kp_sound_chess),
    ;

    companion object {
        fun from(name: String?): KingsAppIcon = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

object KingsAppIcons {

    /** Shows the chosen icon on the home screen and hides the others. */
    fun apply(context: Context, icon: KingsAppIcon) {
        val pm = context.packageManager
        val others = KingsAppIcon.entries.filter { it != icon }.map { it.aliasClass } +
            "com.metrolist.music.MainActivityStatic"
        runCatching {
            // Turn the new one on first, so there is always an icon on the home screen
            pm.setComponentEnabledSetting(
                ComponentName(context, icon.aliasClass),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            others.forEach {
                pm.setComponentEnabledSetting(
                    ComponentName(context, it),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }

    /**
     * Plays the icon's sound.
     * [force] = true for the "Preview" button in Settings.
     * On app open it is skipped when music is already playing or the phone is on silent/vibrate.
     */
    fun playSound(context: Context, icon: KingsAppIcon, force: Boolean = false) {
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (!force && (am.isMusicActive || am.ringerMode != AudioManager.RINGER_MODE_NORMAL)) return
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val player = MediaPlayer.create(context, icon.sound, attributes, am.generateAudioSessionId())
                ?: return
            player.setOnCompletionListener { it.release() }
            player.setOnErrorListener { mp, _, _ -> mp.release(); true }
            player.start()
        }
    }

    /**
     * Called once from MainActivity.onCreate on a fresh app open:
     * starts the animated intro and the logo's sound.
     * Skipped when music is already playing (for example when opening from the notification).
     */
    fun onAppStart(activity: ComponentActivity, savedInstanceState: Bundle?) {
        if (savedInstanceState != null) return // screen rotation etc. — not a fresh open
        val appContext = activity.applicationContext
        val musicPlaying = runCatching {
            (appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager).isMusicActive
        }.getOrDefault(false)
        if (musicPlaying) return
        // Read settings right away so the intro is ready for the very first frame
        val prefs = runCatching { runBlocking { appContext.dataStore.data.first() } }.getOrNull()
        val icon = KingsAppIcon.from(prefs?.get(KingsAppIconKey))
        if (prefs?.get(KingsIntroKey) != false) KingsIntroState.icon = icon
        if (prefs?.get(KingsStartupSoundKey) != false) playSound(appContext, icon)
    }
}
