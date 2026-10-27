# ============================================================================
# Kings Play v1.4 - new Library, Local Music, shrinking bottom bar
#   1. New Library: listening cards, Downloads + Local Music rows,
#      Playlists (New / Import Spotify / Liked / yours), Artists, More
#   2. Local Music: plays the songs saved on your phone (works offline)
#   3. Bottom bar shrinks into circles while scrolling, comes back on scroll up
#
# Needs v1.3 and the Spotify import installed first.
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-library-v14.ps1
# ============================================================================

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dl = Join-Path $env:USERPROFILE "Downloads"

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
$zip = Join-Path $dl "KP_v14_library.zip"
if (-not (Test-Path $zip)) {
    Write-Host "KP_v14_library.zip is not in Downloads. Download it from the chat first." -ForegroundColor Red
    exit 1
}

$src = "app\src\main\kotlin\com\metrolist\music"
function Read-Text($p) { [IO.File]::ReadAllText((Resolve-Path $p)) }
function Write-Text($p, $t) { [IO.File]::WriteAllText((Resolve-Path $p), $t, $utf8) }

# --- Earlier steps must be installed
$need = @(
    @("$src\ui\component\AppNavigation.kt", "KingsGlassSearchButton", "v1.3 (install-v13-full.ps1)"),
    @("$src\ui\component\KingsSpotifyImport.kt", "KingsSpotifyImportHost", "Spotify import (install-spotify.ps1)")
)
foreach ($n in $need) {
    if (-not (Test-Path $n[0]) -or -not (Select-String -Path $n[0] -Pattern $n[1] -SimpleMatch -Quiet)) {
        Write-Host "Please install $($n[2]) first. Nothing was changed." -ForegroundColor Red
        exit 1
    }
}

# --- Restore point
$dirty = git status --porcelain --untracked-files=no
if ($dirty) { git add -A | Out-Null; git commit -m "Before v1.4 library" | Out-Null }
git tag -f before-v1.4 | Out-Null
Write-Host "Restore point saved: before-v1.4" -ForegroundColor Green

Expand-Archive -Path $zip -DestinationPath . -Force
Write-Host "Copied new Library, Local Music and bottom bar files" -ForegroundColor Green

$problems = @()
function Patch($path, $pattern, $replacement, $doneMarker, $what) {
    $t = Read-Text $path
    if ($t.Contains($doneMarker)) { Write-Host "  $what - already there" -ForegroundColor Green; return }
    $rx = [regex]$pattern
    if ($rx.Matches($t).Count -ne 1) { $script:problems += $what; Write-Host "  $what - could not find the place" -ForegroundColor Yellow; return }
    $t = $rx.Replace($t, $replacement, 1)
    Write-Text $path $t
    Write-Host "  $what - done" -ForegroundColor Green
}

# 1. Library main view -> new overview
Patch "$src\ui\screens\library\LibraryScreen.kt" `
    'LibraryFilter\.LIBRARY -> LibraryMixScreen\((?:[^()]|\([^()]*\))*\)' `
    'LibraryFilter.LIBRARY -> KingsLibraryOverview(navController = navController, onShowAll = { filterType = it })' `
    "KingsLibraryOverview(" "New Library screen"

# 2. Local Music page
Patch "$src\ui\screens\NavigationBuilder.kt" `
    '(?m)^([ \t]*)composable\("stats"\) \{' `
    '$1composable("kings_local_music") {
$1    com.metrolist.music.ui.screens.KingsLocalMusicScreen(navController)
$1}
$1composable("stats") {' `
    "kings_local_music" "Local Music page"

# 3. Player: play phone files directly
Patch "$src\playback\MusicService.kt" `
    '(?m)^([ \t]*)val mediaId = dataSpec\.key \?: error\("No media id"\)(\r?)$' `
    '$1val mediaId = dataSpec.key ?: error("No media id")$2
$1// Kings Play: songs saved on the phone play straight from the file$2
$1if (mediaId.startsWith("local:")) {$2
$1    return@Factory dataSpec.withUri(android.net.Uri.parse(mediaId.removePrefix("local:")))$2
$1}$2' `
    'mediaId.startsWith("local:")' "Local file playback"

# 4. Permission to read music files
Patch "app\src\main\AndroidManifest.xml" `
    '(?m)^([ \t]*)<uses-permission android:name="android\.permission\.INTERNET" />(\r?)$' `
    '$1<uses-permission android:name="android.permission.INTERNET" />$2
$1<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />$2
$1<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />$2' `
    "android.permission.READ_MEDIA_AUDIO" "Music file permission"

# 5. Bottom bar knows when you scroll
Patch "$src\MainActivity.kt" `
    'Modifier\.layerBackdrop\(kingsBackdrop\)\.nestedScroll\(topAppBarScrollBehavior\.nestedScrollConnection\)' `
    'Modifier.layerBackdrop(kingsBackdrop).nestedScroll(topAppBarScrollBehavior.nestedScrollConnection).nestedScroll(com.metrolist.music.ui.component.KingsBarScroll)' `
    "KingsBarScroll" "Shrinking bottom bar"

# 6. Library shows its own big title (hide the small one at the top)
Patch "$src\MainActivity.kt" `
    'if \(currentTitleRes == R\.string\.home\) \{' `
    'if (currentTitleRes == R.string.filter_library) {
                                                // Kings Play: the Library screen shows its own big title
                                            } else if (currentTitleRes == R.string.home) {' `
    "currentTitleRes == R.string.filter_library" "Library title"

Write-Host ""
if ($problems.Count -gt 0) {
    Write-Host "These parts could not be added:" -ForegroundColor Yellow
    $problems | ForEach-Object { Write-Host "   $_" -ForegroundColor Yellow }
    Write-Host "Send a screenshot of this to Claude." -ForegroundColor Yellow
    Write-Host ""
}
Write-Host "Done! Now build:  .\gradlew.bat assembleIzzyDebug" -ForegroundColor Green
Write-Host "If anything goes wrong, go back with:  git reset --hard before-v1.4" -ForegroundColor Gray
