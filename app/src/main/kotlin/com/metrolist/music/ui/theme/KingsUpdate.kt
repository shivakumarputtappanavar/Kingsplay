/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * "New version available" — checks Kings Play's own GitHub Releases
 * (github.com/ShivakumarPuttappanavar/Kingsplay), downloads the new APK inside the app
 * (with progress) and opens Android's installer. No browser needed.
 *
 * To publish an update:
 *   1. Change KingsPlayVersion below (for example "1.2")
 *   2. Build the APK
 *   3. On GitHub, create a new release with tag v1.2 and attach the APK
 * Everyone on an older version then sees the message.
 */

package com.metrolist.music.ui.theme

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.metrolist.music.utils.KingsIntroState
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** The version of THIS build. Change it before every new release. */
const val KingsPlayVersion = "1.2"

private const val KingsReleasesApi =
    "https://api.github.com/repos/ShivakumarPuttappanavar/Kingsplay/releases/latest"
const val KingsReleasesPage = "https://github.com/ShivakumarPuttappanavar/Kingsplay/releases"

/** Settings: check for updates when the app opens (on by default). */
val KingsAutoUpdateCheckKey = booleanPreferencesKey("kingsAutoUpdateCheck")

/** A version the user chose to skip. */
private val KingsSkippedVersionKey = stringPreferencesKey("kingsSkippedVersion")

data class KingsRelease(
    val version: String,
    val notes: String,
    val downloadUrl: String,
    /** True when downloadUrl is the APK itself (so it can be installed inside the app). */
    val isApk: Boolean = false,
)

/** Shared state: the update to show (null = nothing). */
object KingsUpdateState {
    var available by mutableStateOf<KingsRelease?>(null)
    var checkedThisLaunch = false

    /** -1 = not downloading, 0..1 = downloading, 2 = downloaded and ready to install */
    var progress by mutableFloatStateOf(-1f)
    var error by mutableStateOf<String?>(null)
    var downloadedFile: File? = null
}

/** "1.10" > "1.9", "v1.2" == "1.2". */
fun isNewerVersion(remote: String, local: String = KingsPlayVersion): Boolean {
    fun parts(v: String) = v.trim().removePrefix("v").removePrefix("V")
        .split('.', '-', ' ').mapNotNull { it.toIntOrNull() }
    val a = parts(remote)
    val b = parts(local)
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

/** Asks GitHub for the latest Kings Play release. Null if offline or no release. */
suspend fun fetchLatestKingsRelease(): KingsRelease? = withContext(Dispatchers.IO) {
    runCatching {
        val conn = (URL(KingsReleasesApi).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "KingsPlay/$KingsPlayVersion")
        }
        try {
            if (conn.responseCode != 200) return@runCatching null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val tag = json.optString("tag_name").ifBlank { return@runCatching null }
            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                        apkUrl = a.optString("browser_download_url")
                        break
                    }
                }
            }
            KingsRelease(
                version = tag.removePrefix("v").removePrefix("V"),
                notes = json.optString("body").trim(),
                downloadUrl = apkUrl ?: json.optString("html_url").ifBlank { KingsReleasesPage },
                isApk = apkUrl != null,
            )
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}

fun openKingsDownload(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        Toast.makeText(context, "Open $KingsReleasesPage in your browser", Toast.LENGTH_LONG).show()
    }
}

private fun updateFile(context: Context, version: String): File =
    File(File(context.cacheDir, "updates").apply { mkdirs() }, "KingsPlay-v$version.apk")

/** Downloads the APK into the app's own storage, reporting progress 0..1. */
private suspend fun downloadApk(context: Context, release: KingsRelease): File = withContext(Dispatchers.IO) {
    // Remove older downloads
    File(context.cacheDir, "updates").listFiles()?.forEach { it.delete() }
    val target = updateFile(context, release.version)
    val part = File(target.path + ".part")
    // GitHub sends the file from another https address; Android follows that automatically
    val conn = (URL(release.downloadUrl).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15000
        readTimeout = 30000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "KingsPlay/$KingsPlayVersion")
    }
    if (conn.responseCode != 200) {
        val code = conn.responseCode
        conn.disconnect()
        throw IllegalStateException("Server answered $code")
    }
    try {
        val total = conn.contentLengthLong
        conn.inputStream.use { input ->
            part.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var done = 0L
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    output.write(buffer, 0, n)
                    done += n
                    if (total > 0) {
                        val p = (done.toFloat() / total).coerceIn(0f, 0.999f)
                        withContext(Dispatchers.Main) { KingsUpdateState.progress = p }
                    }
                }
            }
        }
    } finally {
        conn.disconnect()
    }
    // Basic check: a real APK is a zip file ("PK") and not tiny
    val ok = part.length() > 1_000_000 && part.inputStream().use { s ->
        val head = ByteArray(2)
        s.read(head) == 2 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()
    }
    if (!ok) {
        part.delete()
        throw IllegalStateException("The download was not a valid APK")
    }
    if (target.exists()) target.delete()
    part.renameTo(target)
    target
}

/**
 * Opens Android's installer for the downloaded APK.
 * The first time, Android asks to allow Kings Play to install updates.
 */
fun installKingsUpdate(context: Context, file: File): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        Toast.makeText(context, "Turn on \"Allow from this source\", then come back and tap Install", Toast.LENGTH_LONG).show()
        return false
    }
    return runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    }.getOrElse {
        Toast.makeText(context, "Could not open the installer", Toast.LENGTH_LONG).show()
        false
    }
}

/** Put once at the top of the app (after the intro). Checks once per app start. */
@Composable
fun KingsUpdateHost() {
    val context = LocalContext.current
    val autoCheck by rememberPreference(KingsAutoUpdateCheckKey, true)

    LaunchedEffect(autoCheck) {
        if (!autoCheck || KingsUpdateState.checkedThisLaunch) return@LaunchedEffect
        KingsUpdateState.checkedThisLaunch = true
        val release = fetchLatestKingsRelease() ?: return@LaunchedEffect
        if (!isNewerVersion(release.version)) return@LaunchedEffect
        val skipped = runCatching { context.dataStore.data.first()[KingsSkippedVersionKey] }.getOrNull()
        if (skipped == release.version) return@LaunchedEffect
        KingsUpdateState.available = release
    }

    // Wait until the intro has finished
    val release = KingsUpdateState.available ?: return
    if (KingsIntroState.icon != null) return

    var skipping by androidx.compose.runtime.remember { mutableStateOf(false) }
    LaunchedEffect(skipping) {
        if (skipping) {
            runCatching { context.dataStore.edit { it[KingsSkippedVersionKey] = release.version } }
            KingsUpdateState.available = null
        }
    }

    val scope = rememberCoroutineScope()
    val progress = KingsUpdateState.progress
    val downloading = progress in 0f..1f
    val ready = progress >= 2f

    fun startDownload() {
        if (!release.isApk) {
            // No APK attached to the release: open the GitHub page instead
            openKingsDownload(context, release.downloadUrl)
            KingsUpdateState.available = null
            return
        }
        KingsUpdateState.error = null
        KingsUpdateState.progress = 0f
        scope.launch {
            try {
                val file = downloadApk(context, release)
                KingsUpdateState.downloadedFile = file
                KingsUpdateState.progress = 2f
                installKingsUpdate(context, file)
            } catch (e: Exception) {
                KingsUpdateState.progress = -1f
                KingsUpdateState.error = "Download failed. Check your internet and try again."
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!downloading) KingsUpdateState.available = null },
        title = { Text("New version available 👑", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Kings Play v${release.version} is ready (you have v$KingsPlayVersion).",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (release.notes.isNotBlank()) {
                    Text(release.notes, style = MaterialTheme.typography.bodyMedium)
                }
                when {
                    downloading -> {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "Downloading… ${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    ready -> Text(
                        "Downloaded. Tap Install. Your songs and settings stay.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> Text(
                        "Tap Update: Kings Play downloads it and opens the installer. Your songs and settings stay.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                KingsUpdateState.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            when {
                downloading -> Button(onClick = {}, enabled = false) { Text("Downloading…") }
                ready -> Button(onClick = {
                    val file = KingsUpdateState.downloadedFile
                    if (file != null && file.exists()) installKingsUpdate(context, file) else startDownload()
                }) { Text("Install") }
                else -> Button(onClick = { startDownload() }) { Text("Update") }
            }
        },
        dismissButton = {
            if (!downloading) {
                Column {
                    TextButton(onClick = { KingsUpdateState.available = null }) { Text("Later") }
                    if (!ready) {
                        TextButton(onClick = { skipping = true }) { Text("Skip this version") }
                    }
                }
            }
        },
    )
}
