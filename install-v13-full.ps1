# ============================================================================
# Kings Play v1.3 - the 6 next-update features in one go
#   1. Round glass Search button next to the bottom bar
#   2. Now-playing glow (song cover colors) behind the player and mini player
#   3. Heart burst when you like a song
#   4. Glass search bar
#   5. Lyrics: bolder, and sung words light up in your Kings theme color
#   6. No back arrow on the Settings tab
#   7. Updates download and install inside the app (no browser)
#
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-v13-full.ps1
# ============================================================================

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dl = Join-Path $env:USERPROFILE "Downloads"

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
$zip = Join-Path $dl "KP_v13_full.zip"
if (-not (Test-Path $zip)) {
    Write-Host "KP_v13_full.zip is not in Downloads. Download it from the chat first." -ForegroundColor Red
    exit 1
}

$src = "app\src\main\kotlin\com\metrolist\music"
function Read-Text($p) { [IO.File]::ReadAllText((Resolve-Path $p)) }
function Write-Text($p, $t) { [IO.File]::WriteAllText((Resolve-Path $p), $t, $utf8) }

# --- Check the earlier Kings Play steps are installed (these files get replaced)
$need = @(
    @("$src\ui\player\Player.kt",            "kingsPlayerBackdrop"),
    @("$src\ui\player\MiniPlayer.kt",        "kingsGlass"),
    @("$src\ui\component\AppNavigation.kt",  "sliding liquid bubble"),
    @("$src\ui\theme\KingsStyle.kt",         "KingsStyleKey")
)
foreach ($n in $need) {
    if (-not (Test-Path $n[0]) -or -not (Select-String -Path $n[0] -Pattern $n[1] -SimpleMatch -Quiet)) {
        Write-Host "An earlier Kings Play step is missing: $($n[0])" -ForegroundColor Red
        Write-Host "Nothing was changed. Send a screenshot of this message to Claude." -ForegroundColor Yellow
        exit 1
    }
}

# --- Restore point
$dirty = git status --porcelain --untracked-files=no
if ($dirty) { git add -A | Out-Null; git commit -m "Before v1.3 update" | Out-Null }
git tag -f before-v1.3 | Out-Null
Write-Host "Restore point saved: before-v1.3" -ForegroundColor Green

# --- 1, 2, 3: new and updated files
Expand-Archive -Path $zip -DestinationPath . -Force
Write-Host "[1] Glass Search button        - installed" -ForegroundColor Green
Write-Host "[2] Now-playing glow            - installed" -ForegroundColor Green
Write-Host "[3] Heart burst                 - installed" -ForegroundColor Green

$skipped = @()

# --- 4: Glass search bar
$search = "$src\ui\screens\search\SearchScreen.kt"
$s = Read-Text $search
if ($s.Contains("kingsSearchCapsule()")) {
    Write-Host "[4] Glass search bar            - already there" -ForegroundColor Green
} else {
    $rx = [regex]'(title\s*=\s*\{\s*Row\(\s*modifier\s*=\s*Modifier\.fillMaxWidth\(\))'
    if ($rx.Matches($s).Count -eq 1) {
        $s = $rx.Replace($s, '$1.kingsSearchCapsule()', 1)
        $s = ([regex]'(?m)^import ').Replace($s, "import com.metrolist.music.ui.theme.kingsSearchCapsule`nimport ", 1)
        Write-Text $search $s
        Write-Host "[4] Glass search bar            - installed" -ForegroundColor Green
    } else { $skipped += "4 Glass search bar" }
}

# --- 5a: Lyrics color (Kings theme)
$exp = "$src\ui\component\ExperimentalLyrics.kt"
$e = Read-Text $exp
if ($e.Contains("kingsLyricsAccent(")) {
    Write-Host "[5] Lyrics theme color          - already there" -ForegroundColor Green
} else {
    $rx = [regex]'val expressiveAccent = when \(playerBackground\) \{(\s*PlayerBackgroundStyle\.DEFAULT -> MaterialTheme\.colorScheme\.primary\s*PlayerBackgroundStyle\.BLUR, PlayerBackgroundStyle\.GRADIENT -> Color\.White\s*)\}'
    if ($rx.Matches($e).Count -eq 1) {
        $e = $rx.Replace($e, 'val expressiveAccent = com.metrolist.music.ui.theme.kingsLyricsAccent(when (playerBackground) {$1})', 1)
        Write-Text $exp $e
        Write-Host "[5] Lyrics theme color          - installed" -ForegroundColor Green
    } else { $skipped += "5 Lyrics theme color" }
}

# --- 5b: Lyrics extra bold
$line = "$src\ui\component\LyricsLine.kt"
$l = Read-Text $line
$rx = [regex]'(fontSize = if \(item\.isBackground\) \(lyricsTextSize \* 0\.7f\)\.sp else lyricsTextSize\.sp,\s*fontWeight = )FontWeight\.Bold,'
if ($rx.Matches($l).Count -eq 1) {
    $l = $rx.Replace($l, '$1FontWeight.Black,', 1)
    Write-Text $line $l
    Write-Host "[5] Lyrics extra bold           - installed" -ForegroundColor Green
} elseif ($l.Contains("FontWeight.Black,")) {
    Write-Host "[5] Lyrics extra bold           - already there" -ForegroundColor Green
} else { $skipped += "5 Lyrics extra bold" }

# --- 6: No back arrow on the Settings tab
$set = "$src\ui\screens\settings\SettingsScreen.kt"
$t = Read-Text $set
$rx = [regex]'navigationIcon = \{\s*IconButton\(\s*onClick = navController::navigateUp,\s*onLongClick = navController::backToMain\s*\)\s*\{\s*Icon\(\s*painterResource\(R\.drawable\.arrow_back\),\s*contentDescription = null\s*\)\s*\}\s*\}'
if ($rx.Matches($t).Count -eq 1) {
    $t = $rx.Replace($t, 'navigationIcon = {}', 1)
    Write-Text $set $t
    Write-Host "[6] No back arrow on Settings   - installed" -ForegroundColor Green
} elseif ($t.Contains("navigationIcon = {}")) {
    Write-Host "[6] No back arrow on Settings   - already there" -ForegroundColor Green
} else { $skipped += "6 Settings back arrow" }

# --- 7: Update message + in-app update install
# 7a. Show the "New version available" message after the intro
$theme = "$src\ui\theme\Theme.kt"
$th = Read-Text $theme
if ($th.Contains("KingsUpdateHost()")) {
    Write-Host "[7] Update message              - already there" -ForegroundColor Green
} else {
    $rx = [regex]'(?m)^([ \t]*)KingsIntroHost\(\)(\r?)$'
    if ($rx.Matches($th).Count -eq 1) {
        $th = $rx.Replace($th, '$1KingsIntroHost()$2' + "`n" + '$1KingsUpdateHost()$2', 1)
        Write-Text $theme $th
        Write-Host "[7] Update message              - installed" -ForegroundColor Green
    } else { $skipped += "7 Update message (Theme.kt)" }
}
# 7b. Settings > Updates
$set2 = "$src\ui\screens\settings\SettingsScreen.kt"
$s2 = Read-Text $set2
if ($s2.Contains("KingsUpdateSettingsGroup()")) {
    Write-Host "[7] Settings > Updates          - already there" -ForegroundColor Green
} else {
    $nl = if ($s2.Contains("`r`n")) { "`r`n" } else { "`n" }
    $anchor = "        KingsSoundSettingsGroup()$nl"
    if (-not $s2.Contains($anchor)) { $anchor = "        KingsDisplaySettingsGroup()$nl" }
    if (([regex]::Matches($s2, [regex]::Escape($anchor))).Count -eq 1) {
        $s2 = $s2.Replace($anchor, $anchor + "$nl        // Kings Play: Updates$nl        Spacer(modifier = Modifier.height(16.dp))$nl        KingsUpdateSettingsGroup()$nl")
        Write-Text $set2 $s2
        Write-Host "[7] Settings > Updates          - installed" -ForegroundColor Green
    } else { $skipped += "7 Settings > Updates" }
}
# 7c. About page shows the same version
$about = "$src\ui\screens\settings\AboutScreen.kt"
if (Test-Path $about) {
    $a = Read-Text $about
    $rx2 = [regex]'private const val KINGS_PLAY_VERSION = "[^"]*"'
    if ($rx2.Matches($a).Count -eq 1) {
        $a = $rx2.Replace($a, 'private const val KINGS_PLAY_VERSION = com.metrolist.music.ui.theme.KingsPlayVersion', 1)
        Write-Text $about $a
    }
}
# 7d. Permission to install updates
$man = "app\src\main\AndroidManifest.xml"
$m = Read-Text $man
if ($m.Contains("android.permission.REQUEST_INSTALL_PACKAGES")) {
    Write-Host "[7] In-app update install      - already there" -ForegroundColor Green
} else {
    $rx = [regex]'(?m)^([ \t]*)<uses-permission android:name="android\.permission\.INTERNET" />(\r?)$'
    if ($rx.Matches($m).Count -eq 1) {
        $m = $rx.Replace($m, '$1<uses-permission android:name="android.permission.INTERNET" />$2' + "`n" + '$1<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />$2', 1)
        Write-Text $man $m
        Write-Host "[7] In-app update install      - installed" -ForegroundColor Green
    } else { $skipped += "7 In-app update install (permission)" }
}
Write-Host "Version is now 1.3" -ForegroundColor Green

Write-Host ""
if ($skipped.Count -gt 0) {
    Write-Host "These could not be added (your file looks different):" -ForegroundColor Yellow
    $skipped | ForEach-Object { Write-Host "   $_" -ForegroundColor Yellow }
    Write-Host "Everything else is installed. Send a screenshot of this to Claude for the missing ones." -ForegroundColor Yellow
    Write-Host ""
}
Write-Host "Done! Now build:  .\gradlew.bat assembleIzzyDebug" -ForegroundColor Green
Write-Host "If anything goes wrong, go back with:  git reset --hard before-v1.3" -ForegroundColor Gray
