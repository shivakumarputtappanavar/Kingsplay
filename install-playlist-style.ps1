# ============================================================================
# Kings Play: new look for playlist pages
#   - big rounded cover with a glow in the cover's colour
#   - cover colour washes the top of the page and fades while you scroll
#   - header slides slower and fades out while scrolling (parallax)
#   - songs slide in softly while scrolling, playing song highlighted
#   - Like | Play | Shuffle buttons (Shuffle opposite Like)
#   - 3-dot menu moved to the top bar, Shuffle removed from it
#
# Needs in Downloads:  KingsPlaylistStyle.kt
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-playlist-style.ps1
# ============================================================================

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dl = Join-Path $env:USERPROFILE "Downloads"

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
$newest = Get-ChildItem -Path $dl -Filter "KingsPlaylistStyle*.kt" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $newest) {
    Write-Host "KingsPlaylistStyle.kt is not in Downloads. Download it from the chat first." -ForegroundColor Red
    exit 1
}
$file = $newest.FullName
Write-Host "Using: $($newest.Name)" -ForegroundColor Cyan

$src = "app\src\main\kotlin\com\metrolist\music"
function Read-Text($p) { [IO.File]::ReadAllText((Resolve-Path $p)) }
function Write-Text($p, $t) { [IO.File]::WriteAllText((Resolve-Path $p), $t, $utf8) }

$dirty = git status --porcelain --untracked-files=no
if ($dirty) { git add -A | Out-Null; git commit -m "Before playlist style" | Out-Null }
git tag -f before-playlist-style | Out-Null
Write-Host "Restore point saved: before-playlist-style" -ForegroundColor Green

$problems = @()

function Patch($path, $pattern, $replacement, $doneMarker, $what, $mode) {
    $t = Read-Text $path
    if ($t.Contains($doneMarker)) { Write-Host "  $what - already there" -ForegroundColor Green; return }
    $rx = [regex]$pattern
    $n = $rx.Matches($t).Count
    if ($mode -eq "optional" -and $n -eq 0) { Write-Host "  $what - skipped" -ForegroundColor Gray; return }
    if (($mode -eq "one" -and $n -ne 1) -or ($mode -ne "one" -and $n -lt 1)) {
        $script:problems += $what
        Write-Host "  $what - could not find the place" -ForegroundColor Yellow
        return
    }
    if ($mode -eq "all") { $t = $rx.Replace($t, $replacement) } else { $t = $rx.Replace($t, $replacement, 1) }
    Write-Text $path $t
    Write-Host "  $what - done" -ForegroundColor Green
}

function RemoveBlock($path, $pattern, $goneMarker, $what) {
    $t = Read-Text $path
    if (-not $t.Contains($goneMarker)) { Write-Host "  $what - already done" -ForegroundColor Green; return }
    $rx = [regex]$pattern
    if ($rx.Matches($t).Count -ne 1) {
        $script:problems += $what
        Write-Host "  $what - could not find the place" -ForegroundColor Yellow
        return
    }
    Write-Text $path ($rx.Replace($t, "", 1))
    Write-Host "  $what - done" -ForegroundColor Green
}

Patch "$src\ui\screens\playlist\OnlinePlaylistScreen.kt" @'
(?m)^(package com\.metrolist\.music\.ui\.screens\.playlist)(\r?)$
'@ @'
$1$2

import com.metrolist.music.ui.component.kingsHeaderParallax$2
import com.metrolist.music.ui.component.kingsPlayGlow$2
import com.metrolist.music.ui.component.kingsRowAppear$2
import com.metrolist.music.ui.component.kingsSongRow$2
'@ 'import com.metrolist.music.ui.component.kingsSongRow' 'New style imports' 'one'

Patch "$src\ui\screens\playlist\OnlinePlaylistScreen.kt" @'
(?m)^([ \t]*)Box\(Modifier\.fillMaxSize\(\)\) \{(\r?)\n([ \t]*)LazyColumn\(
'@ @'
$1Box(Modifier.fillMaxSize()) {$2
$3com.metrolist.music.ui.component.KingsPlaylistBackdrop(playlist?.thumbnail, lazyListState)$2
$3LazyColumn(
'@ 'KingsPlaylistBackdrop(' 'Cover colour at the top of the page' 'one'

Patch "$src\ui\screens\playlist\OnlinePlaylistScreen.kt" @'
isPodcastPlaylist = isPodcastPlaylist,(\s*)modifier = Modifier\.animateItem\(\),
'@ @'
isPodcastPlaylist = isPodcastPlaylist,$1modifier = Modifier.animateItem().kingsHeaderParallax(lazyListState),
'@ 'kingsHeaderParallax(lazyListState)' 'Header parallax scrolling' 'one'

Patch "$src\ui\screens\playlist\OnlinePlaylistScreen.kt" @'
isSelected = inSelectMode && songItem\.id in selection,(\s*)modifier =(\s*)Modifier(\s*)\.combinedClickable\(
'@ @'
isSelected = inSelectMode && songItem.id in selection,$1modifier =$2Modifier$3.kingsSongRow(mediaMetadata?.id == songItem.id)$3.combinedClickable(
'@ 'kingsSongRow(mediaMetadata' 'Rounded song rows' 'one'

Patch "$src\ui\screens\playlist\OnlinePlaylistScreen.kt" @'
\)\.animateItem\(\),(\s*)trailingContent = \{
'@ @'
).animateItem().kingsRowAppear(),$1trailingContent = {
'@ 'kingsRowAppear()' 'Songs slide in while scrolling' 'one'

Patch "$src\ui\screens\playlist\OnlinePlaylistScreen.kt" @'
(?s)(onClick = \{ isSearching = true \},\s*\)\s*\{\s*Icon\(\s*painter = painterResource\(R\.drawable\.search\),\s*contentDescription = null,\s*\)\s*\})
'@ @'
$1
                    // Kings Play: playlist menu moved here (Shuffle is now next to Play)
                    playlist?.let { pl ->
                        IconButton(onClick = { menuState.show { YouTubePlaylistMenu(playlist = pl, songs = songs, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) } }) {
                            Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                        }
                    }
'@ 'playlist menu moved here' 'Menu (3 dots) moved to the top bar' 'one'

Patch "$src\ui\screens\playlist\OnlinePlaylistScreen.kt" @'
(?s)Surface\(\s*modifier =\s*Modifier\s*\.size\(240\.dp\).*?\n([ \t]*)Spacer\(modifier = Modifier\.height\(20\.dp\)\)
'@ @'
com.metrolist.music.ui.component.KingsPlaylistCover(playlist.thumbnail)

$1Spacer(modifier = Modifier.height(28.dp))
'@ 'KingsPlaylistCover(' 'Big rounded glowing cover' 'one'

Patch "$src\ui\screens\playlist\OnlinePlaylistScreen.kt" @'
style = MaterialTheme\.typography\.headlineMedium,(\s*)fontWeight = FontWeight\.Bold,
'@ @'
style = MaterialTheme.typography.headlineLarge, // Kings title$1fontWeight = FontWeight.Black,
'@ '// Kings title' 'Bigger bold title' 'one'

Patch "$src\ui\screens\playlist\OnlinePlaylistScreen.kt" @'
(?s)Text\(\s*text = metadataText,.*?modifier = Modifier\.padding\(horizontal = 32\.dp\),\s*\)
'@ @'
com.metrolist.music.ui.component.KingsMetaPill(metadataText)
'@ 'KingsMetaPill(' 'Song count in a glass pill' 'one'

Patch "$src\ui\screens\playlist\OnlinePlaylistScreen.kt" @'
(?s)// Menu Button - Smaller secondary button\s*Surface\(\s*onClick = \{\s*menuState\.show \{\s*YouTubePlaylistMenu\(.*?\}\s*\},(\s*)shape = CircleShape,
'@ @'
// Kings Play: Shuffle button (opposite the Like button)
            Surface(
                onClick = {
                    if (!isListenTogetherGuest) {
                        com.metrolist.music.ui.component.kingsShufflePlay(playerConnection, playlist.shuffleEndpoint, playlist.title, songs)
                    }
                },$1shape = CircleShape,
'@ 'kingsShufflePlay(' 'Shuffle button next to Play' 'one'

Patch "$src\ui\screens\playlist\OnlinePlaylistScreen.kt" @'
painter = painterResource\(R\.drawable\.more_vert\),(\s*)contentDescription = null,(\s*)modifier = Modifier\.size\(24\.dp\),
'@ @'
painter = painterResource(R.drawable.shuffle),$1contentDescription = "Shuffle",$2modifier = Modifier.size(24.dp),
'@ 'contentDescription = "Shuffle"' 'Shuffle icon' 'one'

Patch "$src\ui\screens\playlist\OnlinePlaylistScreen.kt" @'
color = MaterialTheme\.colorScheme\.surfaceVariant,(\s*)modifier = Modifier\.size\(48\.dp\),
'@ @'
color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), // Kings glass$1modifier = Modifier.size(56.dp),
'@ '// Kings glass' 'Glass Like and Shuffle buttons' 'all'

Patch "$src\ui\screens\playlist\OnlinePlaylistScreen.kt" @'
color = MaterialTheme\.colorScheme\.primary,(\s*)shape = CircleShape,(\s*)modifier = Modifier\.size\(72\.dp\),
'@ @'
color = MaterialTheme.colorScheme.primary,$1shape = CircleShape,$2modifier = Modifier.size(78.dp).kingsPlayGlow(MaterialTheme.colorScheme.primary),
'@ 'kingsPlayGlow(MaterialTheme' 'Glowing Play button' 'one'

Patch "$src\ui\screens\playlist\OnlinePlaylistScreen.kt" @'
horizontalArrangement = Arrangement\.spacedBy\(16\.dp, Alignment\.CenterHorizontally\),
'@ @'
horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally),
'@ 'spacedBy(30.dp, Alignment.CenterHorizontally)' 'More space between buttons' 'optional'

RemoveBlock "$src\ui\menu\YouTubePlaylistMenu.kt" @'
(?s)\r?\n[ \t]*playlist\.shuffleEndpoint\?\.let \{ shuffleEndpoint ->\s*add\(\s*NewAction\(.*?YouTubeQueue\(shuffleEndpoint\)\)\s*onDismiss\(\)\s*\},\s*\),\s*\)\s*\}
'@ 'YouTubeQueue(shuffleEndpoint)' 'Shuffle removed from the 3-dot menu'

if ($problems.Count -gt 0) {
    git reset --hard before-playlist-style | Out-Null
    Write-Host ""
    Write-Host "Some parts could not be added, so nothing was changed. Send a screenshot of this to Claude." -ForegroundColor Red
    exit 1
}

Copy-Item $file "$src\ui\component\KingsPlaylistStyle.kt" -Force
Write-Host "  Copied playlist style file" -ForegroundColor Green
Write-Host ""
Write-Host "Done! Now build:  .\gradlew.bat assembleIzzyDebug" -ForegroundColor Green
Write-Host "Then install the NEW apk from: app\build\outputs\apk\izzy\debug" -ForegroundColor Green
Write-Host "If anything goes wrong, go back with:  git reset --hard before-playlist-style" -ForegroundColor Gray
