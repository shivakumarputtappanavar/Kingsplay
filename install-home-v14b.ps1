# ============================================================================
# Kings Play v1.4 - Home order
#   Fixed order: Recents (big cards), Albums & singles, Quick picks,
#   New releases, Trending songs for you, Mixed playlists, Listen together,
#   Keep listening, Forgotten favorites, then the rest. No Speed dial.
#
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-home-v14b.ps1
# ============================================================================

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dl = Join-Path $env:USERPROFILE "Downloads"

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
$zip = Join-Path $dl "KP_v14_home2.zip"
if (-not (Test-Path $zip)) {
    Write-Host "KP_v14_home2.zip is not in Downloads. Download it from the chat first." -ForegroundColor Red
    exit 1
}
$src = "app\src\main\kotlin\com\metrolist\music"
function Read-Text($p) { [IO.File]::ReadAllText((Resolve-Path $p)) }
function Write-Text($p, $t) { [IO.File]::WriteAllText((Resolve-Path $p), $t, $utf8) }

$dirty = git status --porcelain --untracked-files=no
if ($dirty) { git add -A | Out-Null; git commit -m "Before Home order" | Out-Null }
git tag -f before-home-order | Out-Null
Write-Host "Restore point saved: before-home-order" -ForegroundColor Green

Expand-Archive -Path $zip -DestinationPath . -Force
Write-Host "Copied Home order files" -ForegroundColor Green

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

$homeFile = "$src\ui\screens\HomeScreen.kt"
Patch $homeFile '(?m)^([ \t]*)data object MoodAndGenres : HomeSection\("mood_and_genres", 5\)(\r?)$' '$1data object MoodAndGenres : HomeSection("mood_and_genres", 5)$2

$1// Kings Play: new Home sections$2
$1data object KingsRecents : HomeSection("kings_recents", 95)$2

$1data object KingsListenTogether : HomeSection("kings_listen_together", 15)$2' 'KingsRecents : HomeSection' "New Home sections"
Patch $homeFile '(?m)^([ \t]*)val \(randomizeHomeOrder\) = rememberPreference\(RandomizeHomeOrderKey, true\)(\r?)$' '$1val (randomizeHomeOrder) = rememberPreference(RandomizeHomeOrderKey, true)$2
$1val kingsHomeOrder = rememberPreference(KingsHomeOrderKey, "").value$2' 'val kingsHomeOrder =' "Home order setting"
Patch $homeFile 'remember\(\s*randomizeHomeOrder,' 'remember(
            kingsHomeOrder,
            randomizeHomeOrder,' '            kingsHomeOrder,' "Home order refresh"
Patch $homeFile '(?m)^([ \t]*)val chipActive = selectedChip != null(\r?)$' '$1val chipActive = selectedChip != null$2
$1if (!chipActive) {$2
$1    list.add(HomeSection.KingsRecents)$2
$1    list.add(HomeSection.KingsListenTogether)$2
$1}$2' 'list.add(HomeSection.KingsRecents)' "Recents + Listen together"
Patch $homeFile 'if \(randomizeHomeOrder\) \{(\s*)list\.sortedByDescending \{ section ->' 'if (true) {
                // Kings Play: fixed Home order (change it in Settings > Home order)
                kingsSortHomeSections(list, homePage?.sections?.map { it.title } ?: emptyList(), kingsHomeOrder)
            } else if (randomizeHomeOrder) {$1list.sortedByDescending { section ->' 'kingsSortHomeSections(' "Fixed Home order"
Patch $homeFile '(?m)^([ \t]*)HomeSection\.FromTheCommunity -> \{(\r?)$' '$1HomeSection.KingsRecents -> {$2
$1    item(key = "kings_recents") { KingsRecentsSection() }$2
$1}$2
$1HomeSection.KingsListenTogether -> {$2
$1    item(key = "kings_listen_together") { KingsListenTogetherCard() }$2
$1}$2
$1HomeSection.FromTheCommunity -> {$2' 'KingsRecentsSection()' "Show Recents + Listen together"
Patch $homeFile 'val rows =\s*if \(columns >= 6\) \{\s*1\s*\} else if \(columns >= 4\) \{\s*2\s*\} else \{\s*3\s*\}' 'val rows = 1 // Kings Play: speed dial in one row' 'speed dial in one row' "Speed dial in one row"

# No Home order item in Settings (remove it if an earlier version added it)
$set = "$src\ui\screens\settings\SettingsScreen.kt"
$s = Read-Text $set
$rx = [regex]'\r?\n[ \t]*// Kings Play: Home order\r?\n[ \t]*Spacer\(modifier = Modifier\.height\(16\.dp\)\)\r?\n[ \t]*com\.metrolist\.music\.ui\.screens\.KingsHomeOrderSettingsGroup\(\)'
if ($rx.Matches($s).Count -eq 1) {
    $s = $rx.Replace($s, '', 1)
    Write-Text $set $s
    Write-Host "  Removed Home order from Settings" -ForegroundColor Green
}

Write-Host ""
if ($problems.Count -gt 0) {
    Write-Host "These parts could not be added:" -ForegroundColor Yellow
    $problems | ForEach-Object { Write-Host "   $_" -ForegroundColor Yellow }
    Write-Host "Undo with: git reset --hard before-home-order, and send a screenshot to Claude." -ForegroundColor Yellow
    exit 1
}
Write-Host "Done! Now build:  .\gradlew.bat assembleIzzyDebug" -ForegroundColor Green
Write-Host "If anything goes wrong, go back with:  git reset --hard before-home-order" -ForegroundColor Gray
