# ============================================================================
# Kings Play: liquid that follows your finger
#   Playlists (all kinds) and search (results, suggestions, library search):
#     touch the list and a glass drop flows with your finger while you move or scroll
#   Settings and 3-dot menus: the bubble follows your finger; hold, then slide to choose
#   Also fixes the "Unresolved reference 'dp'" build error in SearchScreen.kt
#
# Run install-liquid-playlists.ps1 first (already done).
# Needs in Downloads:  KingsLiquid.kt (new version)
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-liquid-follow.ps1
# ============================================================================

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dl = Join-Path $env:USERPROFILE "Downloads"

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
$src = "app\src\main\kotlin\com\metrolist\music"
if (-not (Test-Path "$src\ui\component\KingsLiquid.kt")) {
    Write-Host "Run install-liquid-playlists.ps1 first." -ForegroundColor Red
    exit 1
}
$liquidFile = Get-ChildItem -Path $dl -Filter "KingsLiquid*.kt" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $liquidFile) {
    Write-Host "KingsLiquid.kt is not in Downloads. Download it from the chat first." -ForegroundColor Red
    exit 1
}
if (-not (Select-String -Path $liquidFile.FullName -Pattern "kingsLiquidFollow" -Quiet)) {
    Write-Host "KingsLiquid.kt in Downloads is an OLD version. Delete it and download it again from the chat." -ForegroundColor Red
    exit 1
}
Write-Host "Using: $($liquidFile.Name)" -ForegroundColor Cyan

function Read-Text($p) { [IO.File]::ReadAllText((Resolve-Path $p)) }
function Write-Text($p, $t) { [IO.File]::WriteAllText((Resolve-Path $p), $t, $utf8) }

$dirty = git status --porcelain --untracked-files=no
if ($dirty) { git add -A | Out-Null; git commit -m "Before liquid follow" | Out-Null }
git tag -f before-liquid-follow | Out-Null
Write-Host "Restore point saved: before-liquid-follow" -ForegroundColor Green

$problems = @()

function Patch($path, $pattern, $replacement, $doneMarker, $what, $mode) {
    $t = Read-Text $path
    if ($t.Contains($doneMarker)) { Write-Host "  $what - already there" -ForegroundColor Green; return }
    $rx = [regex]$pattern
    $n = $rx.Matches($t).Count
    $optional = ($mode -eq "optional" -or $mode -eq "optall")
    if ($optional -and $n -eq 0) { Write-Host "  $what - skipped" -ForegroundColor Gray; return }
    if (($mode -eq "one" -and $n -ne 1) -or ($mode -ne "one" -and $n -lt 1)) {
        $script:problems += $what
        Write-Host "  $what - could not find the place" -ForegroundColor Yellow
        return
    }
    if ($mode -eq "all" -or $mode -eq "optall") { $t = $rx.Replace($t, $replacement) } else { $t = $rx.Replace($t, $replacement, 1) }
    Write-Text $path $t
    Write-Host "  $what - done" -ForegroundColor Green
}

Patch "$src\ui\screens\playlist\OnlinePlaylistScreen.kt" @'
(?m)^(package com\.metrolist\.music\.ui\.screens\.playlist)(\r?)$
'@ @'
$1$2

import com.metrolist.music.ui.component.kingsLiquidFollow$2
'@ 'import com.metrolist.music.ui.component.kingsLiquidFollow' 'Online playlist: import' 'one'

Patch "$src\ui\screens\playlist\OnlinePlaylistScreen.kt" @'
(LazyColumn\(\s*state = \w+,)(\s*)(contentPadding = )
'@ @'
$1$2modifier = Modifier.kingsLiquidFollow(),$2$3
'@ 'Modifier.kingsLiquidFollow()' 'Online playlist: liquid follows your finger' 'one'

Patch "$src\ui\screens\playlist\LocalPlaylistScreen.kt" @'
(?m)^(package com\.metrolist\.music\.ui\.screens\.playlist)(\r?)$
'@ @'
$1$2

import com.metrolist.music.ui.component.kingsLiquidFollow$2
'@ 'import com.metrolist.music.ui.component.kingsLiquidFollow' 'Local playlist: import' 'one'

Patch "$src\ui\screens\playlist\LocalPlaylistScreen.kt" @'
(LazyColumn\(\s*state = \w+,)(\s*)(contentPadding = )
'@ @'
$1$2modifier = Modifier.kingsLiquidFollow(),$2$3
'@ 'Modifier.kingsLiquidFollow()' 'Local playlist: liquid follows your finger' 'one'

Patch "$src\ui\screens\playlist\AutoPlaylistScreen.kt" @'
(?m)^(package com\.metrolist\.music\.ui\.screens\.playlist)(\r?)$
'@ @'
$1$2

import com.metrolist.music.ui.component.kingsLiquidFollow$2
'@ 'import com.metrolist.music.ui.component.kingsLiquidFollow' 'Auto playlist: import' 'one'

Patch "$src\ui\screens\playlist\AutoPlaylistScreen.kt" @'
(LazyColumn\(\s*state = \w+,)(\s*)(contentPadding = )
'@ @'
$1$2modifier = Modifier.kingsLiquidFollow(),$2$3
'@ 'Modifier.kingsLiquidFollow()' 'Auto playlist: liquid follows your finger' 'one'

Patch "$src\ui\screens\playlist\TopPlaylistScreen.kt" @'
(?m)^(package com\.metrolist\.music\.ui\.screens\.playlist)(\r?)$
'@ @'
$1$2

import com.metrolist.music.ui.component.kingsLiquidFollow$2
'@ 'import com.metrolist.music.ui.component.kingsLiquidFollow' 'Top playlist: import' 'one'

Patch "$src\ui\screens\playlist\TopPlaylistScreen.kt" @'
(LazyColumn\(\s*state = \w+,)(\s*)(contentPadding = )
'@ @'
$1$2modifier = Modifier.kingsLiquidFollow(),$2$3
'@ 'Modifier.kingsLiquidFollow()' 'Top playlist: liquid follows your finger' 'one'

Patch "$src\ui\screens\playlist\CachePlaylistScreen.kt" @'
(?m)^(package com\.metrolist\.music\.ui\.screens\.playlist)(\r?)$
'@ @'
$1$2

import com.metrolist.music.ui.component.kingsLiquidFollow$2
'@ 'import com.metrolist.music.ui.component.kingsLiquidFollow' 'Cache playlist: import' 'one'

Patch "$src\ui\screens\playlist\CachePlaylistScreen.kt" @'
(LazyColumn\(\s*state = \w+,)(\s*)(contentPadding = )
'@ @'
$1$2modifier = Modifier.kingsLiquidFollow(),$2$3
'@ 'Modifier.kingsLiquidFollow()' 'Cache playlist: liquid follows your finger' 'one'

Patch "$src\ui\screens\search\LocalSearchScreen.kt" @'
(?m)^(package com\.metrolist\.music\.ui\.screens\.search)(\r?)$
'@ @'
$1$2

import com.metrolist.music.ui.component.kingsLiquidFollow$2
'@ 'import com.metrolist.music.ui.component.kingsLiquidFollow' 'Library search: import' 'one'

Patch "$src\ui\screens\search\LocalSearchScreen.kt" @'
(LazyColumn\(\s*state = lazyListState,\s*modifier = Modifier\.weight\(1f\))
'@ @'
$1.kingsLiquidFollow()
'@ '.kingsLiquidFollow()' 'Library search: liquid follows your finger' 'one'

Patch "$src\ui\screens\search\OnlineSearchResult.kt" @'
(?m)^(package com\.metrolist\.music\.ui\.screens\.search)(\r?)$
'@ @'
$1$2

import com.metrolist.music.ui.component.kingsLiquidFollow$2
'@ 'import com.metrolist.music.ui.component.kingsLiquidFollow' 'Search results: import' 'one'

Patch "$src\ui\screens\search\OnlineSearchResult.kt" @'
(LazyColumn\(\s*state = lazyListState,\s*modifier = Modifier\.fillMaxWidth\(\))
'@ @'
$1.kingsLiquidFollow()
'@ '.kingsLiquidFollow()' 'Search results: liquid follows your finger' 'one'

Patch "$src\ui\screens\search\OnlineSearchScreen.kt" @'
(?m)^(package com\.metrolist\.music\.ui\.screens\.search)(\r?)$
'@ @'
$1$2

import com.metrolist.music.ui.component.kingsLiquidFollow$2
'@ 'import com.metrolist.music.ui.component.kingsLiquidFollow' 'Search suggestions: import' 'one'

Patch "$src\ui\screens\search\OnlineSearchScreen.kt" @'
(Modifier(\s*)\.fillMaxSize\(\))(\s*)\.background\(if \(pureBlack\) Color\.Black else MaterialTheme\.colorScheme\.background\)
'@ @'
$1$3.kingsLiquidFollow()$3.background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
'@ '.kingsLiquidFollow()' 'Search suggestions: liquid follows your finger' 'optional'

Patch "$src\ui\screens\search\SearchScreen.kt" @'
(?m)^(import com\.metrolist\.music\.ui\.component\.kingsLiquidPress)(\r?)$
'@ @'
$1$2
import androidx.compose.ui.unit.dp$2
'@ 'import androidx.compose.ui.unit.dp' 'Search bar: build fix' 'optional'

if ($problems.Count -gt 0) {
    git reset --hard before-liquid-follow | Out-Null
    Write-Host ""
    Write-Host "Some parts could not be added, so nothing was changed. Send a screenshot of this to Claude." -ForegroundColor Red
    exit 1
}

Copy-Item $liquidFile.FullName "$src\ui\component\KingsLiquid.kt" -Force
Write-Host "  Copied KingsLiquid.kt" -ForegroundColor Green
Write-Host ""
Write-Host "Done! Now build:  .\gradlew.bat assembleIzzyDebug" -ForegroundColor Green
Write-Host "Then install the NEW apk from: app\build\outputs\apk\izzy\debug" -ForegroundColor Green
Write-Host "If anything goes wrong, go back with:  git reset --hard before-liquid-follow" -ForegroundColor Gray
