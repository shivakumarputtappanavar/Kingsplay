# ============================================================================
# Kings Play: Kings Mixes (Home) + Josh Songs (Explore)
#   8 energetic playlists every day, in many languages, plus 1-2 from your
#   top artists. Shown in the middle of Home (after Mixed playlists).
#
# Needs the Home order update (install-home-v14b.ps1) first.
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-josh-all.ps1
# ============================================================================

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dl = Join-Path $env:USERPROFILE "Downloads"

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
foreach ($f in @("KingsDailyMixes_v5.kt", "KingsHomeOrder_v3.kt")) {
    if (-not (Test-Path (Join-Path $dl $f))) {
        Write-Host "Missing in Downloads: $f  -> download it from the chat first." -ForegroundColor Red
        exit 1
    }
}
$src = "app\src\main\kotlin\com\metrolist\music"
$homeFile = "$src\ui\screens\HomeScreen.kt"
if (-not (Select-String -Path $homeFile -Pattern "KingsListenTogether" -SimpleMatch -Quiet)) {
    Write-Host "Please install the Home order update (install-home-v14b.ps1) first. Nothing was changed." -ForegroundColor Red
    exit 1
}
function Read-Text($p) { [IO.File]::ReadAllText((Resolve-Path $p)) }
function Write-Text($p, $t) { [IO.File]::WriteAllText((Resolve-Path $p), $t, $utf8) }

$dirty = git status --porcelain --untracked-files=no
if ($dirty) { git add -A | Out-Null; git commit -m "Before Kings Mixes" | Out-Null }
git tag -f before-mixes | Out-Null
Write-Host "Restore point saved: before-mixes" -ForegroundColor Green

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

Patch $homeFile '(?m)^([ \t]*)data object KingsListenTogether : HomeSection\("kings_listen_together", 15\)(\r?)$' '$1data object KingsListenTogether : HomeSection("kings_listen_together", 15)$2

$1data object KingsDailyMixes : HomeSection("kings_daily_mixes", 16)$2' 'data object KingsDailyMixes' "New Home section"
Patch $homeFile '(?m)^([ \t]*)list\.add\(HomeSection\.KingsListenTogether\)(\r?)$' '$1list.add(HomeSection.KingsListenTogether)$2
$1list.add(HomeSection.KingsDailyMixes)$2' 'list.add(HomeSection.KingsDailyMixes)' "Kings Mixes on Home"
Patch $homeFile '(?m)^([ \t]*)HomeSection\.KingsListenTogether -> \{(\r?)$' '$1HomeSection.KingsDailyMixes -> {$2
$1    item(key = "kings_daily_mixes") { KingsDailyMixesSection() }$2
$1}$2
$1HomeSection.KingsListenTogether -> {$2' 'KingsDailyMixesSection()' "Show Kings Mixes"

if ($problems.Count -gt 0) {
    git reset --hard before-mixes | Out-Null
    Write-Host ""
    Write-Host "Could not add Kings Mixes, so nothing was changed. Send a screenshot of this to Claude." -ForegroundColor Red
    exit 1
}

$explore = "$src\ui\screens\MoodAndGenresScreen.kt"
$t = Read-Text $explore
if ($t.Contains("kings_josh_songs")) {
    Write-Host "  Josh Songs in Explore - already there" -ForegroundColor Green
} else {
    $rx = [regex]'(?m)^([ \t]*)item\(key = "explore_header"\) \{\s*ExploreHeader\(\)\s*\}'
    if ($rx.Matches($t).Count -ne 1) {
        git reset --hard before-mixes | Out-Null
        Write-Host "Could not find the top of Explore. Nothing was changed. Send a screenshot to Claude." -ForegroundColor Red
        exit 1
    }
    $t = $rx.Replace($t, {
        param($m)
        $i = $m.Groups[1].Value
        $m.Value + "`n`n" + $i + "// Kings Play: daily josh mixes`n" + $i + 'item(key = "kings_josh_songs") {' + "`n" + $i + '    KingsDailyMixesSection(title = "Josh Songs", subtitle = "Trending josh in every language - new every day")' + "`n" + $i + "}"
    }, 1)
    Write-Text $explore $t
    Write-Host "  Josh Songs in Explore - done" -ForegroundColor Green
}

Copy-Item (Join-Path $dl "KingsDailyMixes_v5.kt") "$src\ui\screens\KingsDailyMixes.kt" -Force
Copy-Item (Join-Path $dl "KingsHomeOrder_v3.kt") "$src\ui\screens\KingsHomeOrder.kt" -Force
Write-Host "  Copied Kings Mixes files" -ForegroundColor Green

Write-Host ""
Write-Host "Done! Now build:  .\gradlew.bat assembleIzzyDebug  - then pull down on Home to refresh" -ForegroundColor Green
Write-Host "If anything goes wrong, go back with:  git reset --hard before-mixes" -ForegroundColor Gray
