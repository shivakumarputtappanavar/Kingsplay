# ============================================================================
# Kings Play: liquid touch + new look for EVERY playlist page
#   Playlist pages (online, your own, liked, downloaded, top, cached):
#     big rounded glowing cover, bigger title, glass buttons, glowing Play,
#     header parallax while scrolling
#   YouTube playlists: Like | Play | Shuffle, menu moved to the top bar
#   Every song row and card in the app: liquid touch + soft slide-in while scrolling
#   Search bar: liquid touch
#   3-dot menus and settings: liquid touch + liquid bubble (hold, then slide)
#
# Needs in Downloads:  KingsPlaylistStyle.kt  and  KingsLiquid.kt
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-liquid-playlists.ps1
# ============================================================================

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dl = Join-Path $env:USERPROFILE "Downloads"

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
function Newest($pattern) {
    Get-ChildItem -Path $dl -Filter $pattern -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
}
$styleFile = Newest "KingsPlaylistStyle*.kt"
$liquidFile = Newest "KingsLiquid*.kt"
if (-not $styleFile -or -not $liquidFile) {
    Write-Host "KingsPlaylistStyle.kt and KingsLiquid.kt must both be in Downloads. Download them from the chat first." -ForegroundColor Red
    exit 1
}
if (-not (Select-String -Path $styleFile.FullName -Pattern "kingsCoverRing" -Quiet)) {
    Write-Host "KingsPlaylistStyle.kt in Downloads is an OLD version. Delete it and download it again from the chat." -ForegroundColor Red
    exit 1
}
Write-Host "Using: $($styleFile.Name) and $($liquidFile.Name)" -ForegroundColor Cyan

$src = "app\src\main\kotlin\com\metrolist\music"
function Read-Text($p) { [IO.File]::ReadAllText((Resolve-Path $p)) }
function Write-Text($p, $t) { [IO.File]::WriteAllText((Resolve-Path $p), $t, $utf8) }

$dirty = git status --porcelain --untracked-files=no
if ($dirty) { git add -A | Out-Null; git commit -m "Before liquid playlists" | Out-Null }
git tag -f before-liquid-playlists | Out-Null
Write-Host "Restore point saved: before-liquid-playlists" -ForegroundColor Green

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

Patch "$src\ui\screens\playlist\OnlinePlaylistScreen.kt" @'
\.animateItem\(\)\.kingsRowAppear\(\),
'@ @'
.animateItem(),
'@ 'ZZZ-never' 'Online rows: use the new global slide-in' 'optall'

Patch "$src\ui\screens\playlist\LocalPlaylistScreen.kt" @'
(?m)^(package com\.metrolist\.music\.ui\.screens\.playlist)(\r?)$
'@ @'
$1$2

import com.metrolist.music.ui.component.kingsCoverRing$2
import com.metrolist.music.ui.component.kingsHeaderParallax$2
import com.metrolist.music.ui.component.kingsPlayGlow$2
'@ 'import com.metrolist.music.ui.component.kingsCoverRing' 'Local: new style imports' 'one'

Patch "$src\ui\screens\playlist\LocalPlaylistScreen.kt" @'
(?s)(item\(key = "playlist_header"\) \{\s*LocalPlaylistHeader\(.*?modifier = Modifier\.animateItem\(\))
'@ @'
$1.kingsHeaderParallax(lazyListState)
'@ 'kingsHeaderParallax(lazyListState)' 'Local: header parallax scrolling' 'one'

Patch "$src\ui\screens\playlist\LocalPlaylistScreen.kt" @'
\.size\(240\.dp\)(\s*)\.shadow\(\s*elevation = \d+\.dp,\s*shape = RoundedCornerShape\(3\.dp\),(?:\s*spotColor = [^\n]*)?\s*\),(\s*)shape = RoundedCornerShape\(3\.dp\)
'@ @'
.size(256.dp)$1.kingsCoverRing(MaterialTheme.colorScheme.primary),$2shape = RoundedCornerShape(28.dp)
'@ 'kingsCoverRing(MaterialTheme' 'Local: big rounded glowing cover' 'all'

Patch "$src\ui\screens\playlist\LocalPlaylistScreen.kt" @'
\.align\(alignment\)(\s*)\.size\(120\.dp\)
'@ @'
.align(alignment)$1.fillMaxSize(0.5f)
'@ '.fillMaxSize(0.5f)' 'Local: 4-cover grid fits the bigger cover' 'optional'

Patch "$src\ui\screens\playlist\LocalPlaylistScreen.kt" @'
style = MaterialTheme\.typography\.headlineMedium,(\s*)fontWeight = FontWeight\.Bold,
'@ @'
style = MaterialTheme.typography.headlineLarge, // Kings title$1fontWeight = FontWeight.Black,
'@ '// Kings title' 'Local: bigger bold title' 'all'

Patch "$src\ui\screens\playlist\LocalPlaylistScreen.kt" @'
color = MaterialTheme\.colorScheme\.surfaceVariant,(\s*)modifier = Modifier\.size\(48\.dp\)
'@ @'
color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), // Kings glass$1modifier = Modifier.size(56.dp)
'@ '// Kings glass' 'Local: glass Shuffle and menu buttons' 'all'

Patch "$src\ui\screens\playlist\LocalPlaylistScreen.kt" @'
color = MaterialTheme\.colorScheme\.primary,(\s*)shape = ((?:androidx\.compose\.foundation\.shape\.)?CircleShape),(\s*)modifier = Modifier\.size\(72\.dp\)
'@ @'
color = MaterialTheme.colorScheme.primary,$1shape = $2,$3modifier = Modifier.size(78.dp).kingsPlayGlow(MaterialTheme.colorScheme.primary)
'@ 'kingsPlayGlow(MaterialTheme' 'Local: glowing Play button' 'all'

Patch "$src\ui\screens\playlist\LocalPlaylistScreen.kt" @'
horizontalArrangement = Arrangement\.spacedBy\(16\.dp, Alignment\.CenterHorizontally\),
'@ @'
horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally),
'@ 'spacedBy(30.dp, Alignment.CenterHorizontally)' 'Local: more space between buttons' 'optional'

Patch "$src\ui\screens\playlist\AutoPlaylistScreen.kt" @'
(?m)^(package com\.metrolist\.music\.ui\.screens\.playlist)(\r?)$
'@ @'
$1$2

import com.metrolist.music.ui.component.kingsCoverRing$2
import com.metrolist.music.ui.component.kingsHeaderParallax$2
import com.metrolist.music.ui.component.kingsPlayGlow$2
'@ 'import com.metrolist.music.ui.component.kingsCoverRing' 'Auto: new style imports' 'one'

Patch "$src\ui\screens\playlist\AutoPlaylistScreen.kt" @'
(?s)(item\(key = "playlist_header"\) \{\s*AutoPlaylistHeader\(.*?modifier = Modifier\.animateItem\(\))
'@ @'
$1.kingsHeaderParallax(state)
'@ 'kingsHeaderParallax(state)' 'Auto: header parallax scrolling' 'one'

Patch "$src\ui\screens\playlist\AutoPlaylistScreen.kt" @'
\.size\(240\.dp\)(\s*)\.shadow\(\s*elevation = \d+\.dp,\s*shape = RoundedCornerShape\(3\.dp\),(?:\s*spotColor = [^\n]*)?\s*\),(\s*)shape = RoundedCornerShape\(3\.dp\)
'@ @'
.size(256.dp)$1.kingsCoverRing(MaterialTheme.colorScheme.primary),$2shape = RoundedCornerShape(28.dp)
'@ 'kingsCoverRing(MaterialTheme' 'Auto: big rounded glowing cover' 'all'

Patch "$src\ui\screens\playlist\AutoPlaylistScreen.kt" @'
\.align\(alignment\)(\s*)\.size\(120\.dp\)
'@ @'
.align(alignment)$1.fillMaxSize(0.5f)
'@ '.fillMaxSize(0.5f)' 'Auto: 4-cover grid fits the bigger cover' 'optional'

Patch "$src\ui\screens\playlist\AutoPlaylistScreen.kt" @'
style = MaterialTheme\.typography\.headlineMedium,(\s*)fontWeight = FontWeight\.Bold,
'@ @'
style = MaterialTheme.typography.headlineLarge, // Kings title$1fontWeight = FontWeight.Black,
'@ '// Kings title' 'Auto: bigger bold title' 'all'

Patch "$src\ui\screens\playlist\AutoPlaylistScreen.kt" @'
color = MaterialTheme\.colorScheme\.surfaceVariant,(\s*)modifier = Modifier\.size\(48\.dp\)
'@ @'
color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), // Kings glass$1modifier = Modifier.size(56.dp)
'@ '// Kings glass' 'Auto: glass Shuffle and menu buttons' 'all'

Patch "$src\ui\screens\playlist\AutoPlaylistScreen.kt" @'
color = MaterialTheme\.colorScheme\.primary,(\s*)shape = ((?:androidx\.compose\.foundation\.shape\.)?CircleShape),(\s*)modifier = Modifier\.size\(72\.dp\)
'@ @'
color = MaterialTheme.colorScheme.primary,$1shape = $2,$3modifier = Modifier.size(78.dp).kingsPlayGlow(MaterialTheme.colorScheme.primary)
'@ 'kingsPlayGlow(MaterialTheme' 'Auto: glowing Play button' 'all'

Patch "$src\ui\screens\playlist\AutoPlaylistScreen.kt" @'
horizontalArrangement = Arrangement\.spacedBy\(16\.dp, Alignment\.CenterHorizontally\),
'@ @'
horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally),
'@ 'spacedBy(30.dp, Alignment.CenterHorizontally)' 'Auto: more space between buttons' 'optional'

Patch "$src\ui\screens\playlist\TopPlaylistScreen.kt" @'
(?m)^(package com\.metrolist\.music\.ui\.screens\.playlist)(\r?)$
'@ @'
$1$2

import com.metrolist.music.ui.component.kingsCoverRing$2
import com.metrolist.music.ui.component.kingsHeaderParallax$2
import com.metrolist.music.ui.component.kingsPlayGlow$2
'@ 'import com.metrolist.music.ui.component.kingsCoverRing' 'Top: new style imports' 'one'

Patch "$src\ui\screens\playlist\TopPlaylistScreen.kt" @'
(?s)(item\(key = "playlist_header"\) \{\s*TopPlaylistHeader\(.*?modifier = Modifier\.animateItem\(\))
'@ @'
$1.kingsHeaderParallax(state)
'@ 'kingsHeaderParallax(state)' 'Top: header parallax scrolling' 'one'

Patch "$src\ui\screens\playlist\TopPlaylistScreen.kt" @'
\.size\(240\.dp\)(\s*)\.shadow\(\s*elevation = \d+\.dp,\s*shape = RoundedCornerShape\(3\.dp\),(?:\s*spotColor = [^\n]*)?\s*\),(\s*)shape = RoundedCornerShape\(3\.dp\)
'@ @'
.size(256.dp)$1.kingsCoverRing(MaterialTheme.colorScheme.primary),$2shape = RoundedCornerShape(28.dp)
'@ 'kingsCoverRing(MaterialTheme' 'Top: big rounded glowing cover' 'all'

Patch "$src\ui\screens\playlist\TopPlaylistScreen.kt" @'
\.align\(alignment\)(\s*)\.size\(120\.dp\)
'@ @'
.align(alignment)$1.fillMaxSize(0.5f)
'@ '.fillMaxSize(0.5f)' 'Top: 4-cover grid fits the bigger cover' 'optional'

Patch "$src\ui\screens\playlist\TopPlaylistScreen.kt" @'
style = MaterialTheme\.typography\.headlineMedium,(\s*)fontWeight = FontWeight\.Bold,
'@ @'
style = MaterialTheme.typography.headlineLarge, // Kings title$1fontWeight = FontWeight.Black,
'@ '// Kings title' 'Top: bigger bold title' 'all'

Patch "$src\ui\screens\playlist\TopPlaylistScreen.kt" @'
color = MaterialTheme\.colorScheme\.surfaceVariant,(\s*)modifier = Modifier\.size\(48\.dp\)
'@ @'
color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), // Kings glass$1modifier = Modifier.size(56.dp)
'@ '// Kings glass' 'Top: glass Shuffle and menu buttons' 'all'

Patch "$src\ui\screens\playlist\TopPlaylistScreen.kt" @'
color = MaterialTheme\.colorScheme\.primary,(\s*)shape = ((?:androidx\.compose\.foundation\.shape\.)?CircleShape),(\s*)modifier = Modifier\.size\(72\.dp\)
'@ @'
color = MaterialTheme.colorScheme.primary,$1shape = $2,$3modifier = Modifier.size(78.dp).kingsPlayGlow(MaterialTheme.colorScheme.primary)
'@ 'kingsPlayGlow(MaterialTheme' 'Top: glowing Play button' 'all'

Patch "$src\ui\screens\playlist\TopPlaylistScreen.kt" @'
horizontalArrangement = Arrangement\.spacedBy\(16\.dp, Alignment\.CenterHorizontally\),
'@ @'
horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally),
'@ 'spacedBy(30.dp, Alignment.CenterHorizontally)' 'Top: more space between buttons' 'optional'

Patch "$src\ui\screens\playlist\CachePlaylistScreen.kt" @'
(?m)^(package com\.metrolist\.music\.ui\.screens\.playlist)(\r?)$
'@ @'
$1$2

import com.metrolist.music.ui.component.kingsCoverRing$2
import com.metrolist.music.ui.component.kingsHeaderParallax$2
import com.metrolist.music.ui.component.kingsPlayGlow$2
'@ 'import com.metrolist.music.ui.component.kingsCoverRing' 'Cache: new style imports' 'one'

Patch "$src\ui\screens\playlist\CachePlaylistScreen.kt" @'
(?s)(item\(key = "playlist_header"\) \{\s*CachePlaylistHeader\(.*?modifier = Modifier\.animateItem\(\))
'@ @'
$1.kingsHeaderParallax(lazyListState)
'@ 'kingsHeaderParallax(lazyListState)' 'Cache: header parallax scrolling' 'one'

Patch "$src\ui\screens\playlist\CachePlaylistScreen.kt" @'
\.size\(240\.dp\)(\s*)\.shadow\(\s*elevation = \d+\.dp,\s*shape = RoundedCornerShape\(3\.dp\),(?:\s*spotColor = [^\n]*)?\s*\),(\s*)shape = RoundedCornerShape\(3\.dp\)
'@ @'
.size(256.dp)$1.kingsCoverRing(MaterialTheme.colorScheme.primary),$2shape = RoundedCornerShape(28.dp)
'@ 'kingsCoverRing(MaterialTheme' 'Cache: big rounded glowing cover' 'all'

Patch "$src\ui\screens\playlist\CachePlaylistScreen.kt" @'
\.align\(alignment\)(\s*)\.size\(120\.dp\)
'@ @'
.align(alignment)$1.fillMaxSize(0.5f)
'@ '.fillMaxSize(0.5f)' 'Cache: 4-cover grid fits the bigger cover' 'optional'

Patch "$src\ui\screens\playlist\CachePlaylistScreen.kt" @'
style = MaterialTheme\.typography\.headlineMedium,(\s*)fontWeight = FontWeight\.Bold,
'@ @'
style = MaterialTheme.typography.headlineLarge, // Kings title$1fontWeight = FontWeight.Black,
'@ '// Kings title' 'Cache: bigger bold title' 'all'

Patch "$src\ui\screens\playlist\CachePlaylistScreen.kt" @'
color = MaterialTheme\.colorScheme\.surfaceVariant,(\s*)modifier = Modifier\.size\(48\.dp\)
'@ @'
color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), // Kings glass$1modifier = Modifier.size(56.dp)
'@ '// Kings glass' 'Cache: glass Shuffle and menu buttons' 'all'

Patch "$src\ui\screens\playlist\CachePlaylistScreen.kt" @'
color = MaterialTheme\.colorScheme\.primary,(\s*)shape = ((?:androidx\.compose\.foundation\.shape\.)?CircleShape),(\s*)modifier = Modifier\.size\(72\.dp\)
'@ @'
color = MaterialTheme.colorScheme.primary,$1shape = $2,$3modifier = Modifier.size(78.dp).kingsPlayGlow(MaterialTheme.colorScheme.primary)
'@ 'kingsPlayGlow(MaterialTheme' 'Cache: glowing Play button' 'all'

Patch "$src\ui\screens\playlist\CachePlaylistScreen.kt" @'
horizontalArrangement = Arrangement\.spacedBy\(16\.dp, Alignment\.CenterHorizontally\),
'@ @'
horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally),
'@ 'spacedBy(30.dp, Alignment.CenterHorizontally)' 'Cache: more space between buttons' 'optional'

Patch "$src\ui\component\Items.kt" @'
modifier // (playing highlight|inactive selected|default)
'@ @'
modifier.kingsLiquidPress(8.dp).kingsRowAppear() // $1
'@ 'kingsLiquidPress(8.dp).kingsRowAppear()' 'All song rows: liquid touch + slide-in' 'all'

Patch "$src\ui\component\Items.kt" @'
modifier(\s*)\.padding\(12\.dp\)
'@ @'
modifier$1.kingsLiquidPress(20.dp)$1.padding(12.dp)
'@ 'kingsLiquidPress(20.dp)' 'All playlist / album cards: liquid touch' 'all'

Patch "$src\ui\screens\search\SearchScreen.kt" @'
(?m)^(package com\.metrolist\.music\.ui\.screens\.search)(\r?)$
'@ @'
$1$2

import com.metrolist.music.ui.component.kingsLiquidPress$2
'@ 'import com.metrolist.music.ui.component.kingsLiquidPress' 'Search: import' 'one'

Patch "$src\ui\screens\search\SearchScreen.kt" @'
\.kingsSearchCapsule\(\)
'@ @'
.kingsSearchCapsule().kingsLiquidPress(28.dp)
'@ 'kingsLiquidPress(28.dp)' 'Search bar: liquid touch' 'optional'

Patch "$src\ui\component\Menu.kt" @'
fun Material3MenuGroup\(\s*items: List<Material3MenuItemData>\s*\)\s*\{
'@ @'
$0
    val kingsBubble = rememberKingsBubble()
'@ 'val kingsBubble = rememberKingsBubble()' '3-dot menus: bubble' 'one'

Patch "$src\ui\component\Menu.kt" @'
Column\(\s*modifier = Modifier\.fillMaxWidth\(\),(\s*)verticalArrangement = Arrangement\.spacedBy\(4\.dp\)(\s*)\)\s*\{(\s*)items\.forEachIndexed \{ index, item ->
'@ @'
Column(
        modifier = Modifier.fillMaxWidth().kingsBubbleHost(kingsBubble, 24.dp),$1verticalArrangement = Arrangement.spacedBy(4.dp)$2) {$3items.forEachIndexed { index, item ->
'@ 'kingsBubbleHost(kingsBubble, 24.dp)' '3-dot menus: bubble area' 'one'

Patch "$src\ui\component\Menu.kt" @'
Card\(\s*modifier = Modifier(\s*)\.fillMaxWidth\(\)(\s*)\.animateContentSize\(\),(\s*)shape = shape,
'@ @'
Card(
                modifier = Modifier$1.fillMaxWidth()$2.animateContentSize()$2.kingsLiquidPress(24.dp)$2.kingsBubbleItem(kingsBubble, index, enabled = item.onClick != null) { item.onClick?.invoke() },$3shape = shape,
'@ 'kingsBubbleItem(kingsBubble, index' '3-dot menus: liquid items' 'one'

Patch "$src\ui\component\Material3SettingsGroup.kt" @'
fun Material3SettingsGroup\([^{]*?\)\s*\{
'@ @'
$0
    val kingsBubble = rememberKingsBubble()
'@ 'val kingsBubble = rememberKingsBubble()' 'Settings: bubble' 'one'

Patch "$src\ui\component\Material3SettingsGroup.kt" @'
Column\(\s*modifier = Modifier\.fillMaxWidth\(\),(\s*)verticalArrangement = Arrangement\.spacedBy\(4\.dp\)(\s*)\)\s*\{(\s*)items\.forEachIndexed \{ index, item ->
'@ @'
Column(
        modifier = Modifier.fillMaxWidth().kingsBubbleHost(kingsBubble, 24.dp),$1verticalArrangement = Arrangement.spacedBy(4.dp)$2) {$3items.forEachIndexed { index, item ->
'@ 'kingsBubbleHost(kingsBubble, 24.dp)' 'Settings: bubble area' 'one'

Patch "$src\ui\component\Material3SettingsGroup.kt" @'
Card\(\s*modifier = Modifier(\s*)\.fillMaxWidth\(\)(\s*)\.animateContentSize\(\),(\s*)shape = shape,
'@ @'
Card(
                modifier = Modifier$1.fillMaxWidth()$2.animateContentSize()$2.kingsLiquidPress(24.dp)$2.kingsBubbleItem(kingsBubble, index, enabled = item.enabled && item.onClick != null) { item.onClick?.invoke() },$3shape = shape,
'@ 'kingsBubbleItem(kingsBubble, index' 'Settings: liquid items' 'one'

Patch "$src\ui\component\NewMenuComponents.kt" @'
enabled: Boolean = true,(\s*)backgroundColor: Color = MaterialTheme\.colorScheme\.surfaceVariant,
'@ @'
enabled: Boolean = true,$1bubble: KingsBubbleState? = null,$1backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
'@ 'bubble: KingsBubbleState? = null' 'Menu buttons: bubble support' 'one'

Patch "$src\ui\component\NewMenuComponents.kt" @'
modifier(\s*)\.clickable\(enabled = enabled\) \{ performAction = true \},
'@ @'
modifier$1.kingsLiquidPress(16.dp)$1.kingsBubbleItem(bubble, text, enabled) { performAction = true }$1.clickable(enabled = enabled) { performAction = true },
'@ 'kingsBubbleItem(bubble, text' 'Menu buttons: liquid touch' 'one'

Patch "$src\ui\component\NewMenuComponents.kt" @'
(\s*)val rows = actions\.chunked\(columns\)
'@ @'
$1val kingsBubble = rememberKingsBubble()$1val rows = actions.chunked(columns)
'@ 'rememberKingsBubble()' 'Menu buttons: bubble' 'one'

Patch "$src\ui\component\NewMenuComponents.kt" @'
Column\(\s*modifier = modifier,(\s*)verticalArrangement = Arrangement\.spacedBy\(12\.dp\),
'@ @'
Column(
        modifier = modifier.kingsBubbleHost(kingsBubble, 16.dp),$1verticalArrangement = Arrangement.spacedBy(12.dp),
'@ 'kingsBubbleHost(kingsBubble, 16.dp)' 'Menu buttons: bubble area' 'one'

Patch "$src\ui\component\NewMenuComponents.kt" @'
modifier = Modifier\.weight\(1f\),(\s*)enabled = action\.enabled,
'@ @'
modifier = Modifier.weight(1f),$1enabled = action.enabled,$1bubble = kingsBubble,
'@ 'bubble = kingsBubble,' 'Menu buttons: connect bubble' 'one'

RemoveBlock "$src\ui\menu\YouTubePlaylistMenu.kt" @'
(?s)\r?\n[ \t]*playlist\.shuffleEndpoint\?\.let \{ shuffleEndpoint ->\s*add\(\s*NewAction\(.*?YouTubeQueue\(shuffleEndpoint\)\)\s*onDismiss\(\)\s*\},\s*\),\s*\)\s*\}
'@ 'YouTubeQueue(shuffleEndpoint)' 'Shuffle removed from the 3-dot menu'

if ($problems.Count -gt 0) {
    git reset --hard before-liquid-playlists | Out-Null
    Write-Host ""
    Write-Host "Some parts could not be added, so nothing was changed. Send a screenshot of this to Claude." -ForegroundColor Red
    exit 1
}

Copy-Item $styleFile.FullName "$src\ui\component\KingsPlaylistStyle.kt" -Force
Copy-Item $liquidFile.FullName "$src\ui\component\KingsLiquid.kt" -Force
Write-Host "  Copied KingsPlaylistStyle.kt and KingsLiquid.kt" -ForegroundColor Green
Write-Host ""
Write-Host "Done! Now build:  .\gradlew.bat assembleIzzyDebug" -ForegroundColor Green
Write-Host "Then install the NEW apk from: app\build\outputs\apk\izzy\debug" -ForegroundColor Green
Write-Host "If anything goes wrong, go back with:  git reset --hard before-liquid-playlists" -ForegroundColor Gray
