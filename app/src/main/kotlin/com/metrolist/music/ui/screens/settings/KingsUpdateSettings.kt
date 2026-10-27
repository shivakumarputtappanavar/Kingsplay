/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Main Settings: "Updates" group — check for a new Kings Play version.
 */

package com.metrolist.music.ui.screens.settings

import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.metrolist.music.R
import com.metrolist.music.ui.component.KingsLiquidSwitch
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.theme.KingsAutoUpdateCheckKey
import com.metrolist.music.ui.theme.KingsPlayVersion
import com.metrolist.music.ui.theme.KingsUpdateState
import com.metrolist.music.ui.theme.fetchLatestKingsRelease
import com.metrolist.music.ui.theme.isNewerVersion
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.launch

@Composable
fun KingsUpdateSettingsGroup() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (autoCheck, onAutoCheckChange) = rememberPreference(KingsAutoUpdateCheckKey, true)
    var checking by remember { mutableStateOf(false) }

    Material3SettingsGroup(
        title = "Updates",
        items = listOf(
            Material3SettingsItem(
                icon = painterResource(R.drawable.update),
                title = { Text("Check for updates") },
                description = {
                    Text(if (checking) "Checking…" else "You have Kings Play v$KingsPlayVersion")
                },
                onClick = {
                    if (!checking) {
                    checking = true
                    scope.launch {
                        val release = fetchLatestKingsRelease()
                        checking = false
                        when {
                            release == null ->
                                Toast.makeText(context, "Could not check. Are you online?", Toast.LENGTH_LONG).show()
                            isNewerVersion(release.version) ->
                                KingsUpdateState.available = release
                            else ->
                                Toast.makeText(context, "You have the latest version 👑", Toast.LENGTH_SHORT).show()
                        }
                    }
                    }
                },
            ),
            Material3SettingsItem(
                icon = painterResource(R.drawable.notification),
                title = { Text("Tell me about new versions") },
                description = { Text("Check when Kings Play opens") },
                trailingContent = {
                    KingsLiquidSwitch(checked = autoCheck, onCheckedChange = onAutoCheckChange)
                },
                onClick = { onAutoCheckChange(!autoCheck) },
            ),
        )
    )
}
