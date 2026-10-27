# ============================================================================
# Kings Play: Josh Songs v6 (Explore only)
#   - Josh mixes are REMOVED from Home
#   - Explore gets one big "Josh Songs" card. Tap it -> Josh Songs page with:
#       Insta Trending (changes once a month)
#       All Josh Mix (all josh playlists in one)
#       Today's Best 5 (changes every day)
#       All Josh playlists (each one separate)
#   - No folk songs, no devotional songs, no repeats from yesterday
#
# Needs in Downloads:  KingsJosh.kt
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-josh-v6.ps1
# ============================================================================

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dl = Join-Path $env:USERPROFILE "Downloads"

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
$file = Join-Path $dl "KingsJosh.kt"
if (-not (Test-Path $file)) {
    Write-Host "KingsJosh.kt is not in Downloads. Download it from the chat first." -ForegroundColor Red
    exit 1
}
$src = "app\src\main\kotlin\com\metrolist\music"
$homeFile = "$src\ui\screens\HomeScreen.kt"
$explore = "$src\ui\screens\MoodAndGenresScreen.kt"
$navFile = "$src\ui\screens\NavigationBuilder.kt"
function Read-Text($p) { [IO.File]::ReadAllText((Resolve-Path $p)) }
function Write-Text($p, $t) { [IO.File]::WriteAllText((Resolve-Path $p), $t, $utf8) }

$dirty = git status --porcelain --untracked-files=no
if ($dirty) { git add -A | Out-Null; git commit -m "Before Josh v6" | Out-Null }
git tag -f before-josh-v6 | Out-Null
Write-Host "Restore point saved: before-josh-v6" -ForegroundColor Green

$problems = @()

# 1. Remove the josh mixes from Home
$t = Read-Text $homeFile
$rx = [regex]'(?m)^[ \t]*list\.add\(HomeSection\.KingsDailyMixes\)[ \t]*\r?\n'
if ($rx.Matches($t).Count -eq 0) {
    Write-Host "  Remove mixes from Home - already removed" -ForegroundColor Green
} else {
    Write-Text $homeFile ($rx.Replace($t, ''))
    Write-Host "  Remove mixes from Home - done" -ForegroundColor Green
}

# 2. Explore: one big "Josh Songs" card
$t = Read-Text $explore
if ($t.Contains("KingsJoshBanner()")) {
    Write-Host "  Josh Songs card in Explore - already there" -ForegroundColor Green
} else {
    $old = [regex]'KingsDailyMixesSection\([^\r\n]*\)'
    if ($t.Contains("kings_josh_songs") -and $old.Matches($t).Count -eq 1) {
        $t = $old.Replace($t, 'KingsJoshBanner()', 1)
        Write-Text $explore $t
        Write-Host "  Josh Songs card in Explore - done" -ForegroundColor Green
    } else {
        $rx = [regex]'(?m)^([ \t]*)item\(key = "explore_header"\) \{\s*ExploreHeader\(\)\s*\}'
        if ($rx.Matches($t).Count -ne 1) {
            $problems += "Explore"
            Write-Host "  Josh Songs card in Explore - could not find the place" -ForegroundColor Yellow
        } else {
            $t = $rx.Replace($t, {
                param($m)
                $i = $m.Groups[1].Value
                $m.Value + "`n`n" + $i + "// Kings Play: Josh Songs`n" + $i + 'item(key = "kings_josh_songs") {' + "`n" + $i + "    KingsJoshBanner()`n" + $i + "}"
            }, 1)
            Write-Text $explore $t
            Write-Host "  Josh Songs card in Explore - done" -ForegroundColor Green
        }
    }
}

# 3. New page: Josh Songs
$t = Read-Text $navFile
if ($t.Contains('composable("kings_josh")')) {
    Write-Host "  Josh Songs page - already there" -ForegroundColor Green
} else {
    $rx = [regex]'(?m)^([ \t]*)composable\("stats"\) \{'
    if ($rx.Matches($t).Count -ne 1) {
        $problems += "Page"
        Write-Host "  Josh Songs page - could not find the place" -ForegroundColor Yellow
    } else {
        $t = $rx.Replace($t, {
            param($m)
            $i = $m.Groups[1].Value
            $i + 'composable("kings_josh") {' + "`n" + $i + "    com.metrolist.music.ui.screens.KingsJoshScreen(navController)`n" + $i + "}`n`n" + $m.Value
        }, 1)
        Write-Text $navFile $t
        Write-Host "  Josh Songs page - done" -ForegroundColor Green
    }
}

if ($problems.Count -gt 0) {
    git reset --hard before-josh-v6 | Out-Null
    Write-Host ""
    Write-Host "Some parts could not be added, so nothing was changed. Send a screenshot of this to Claude." -ForegroundColor Red
    exit 1
}

# 4. New Josh file (replaces the old mixes file)
Copy-Item $file "$src\ui\screens\KingsDailyMixes.kt" -Force
Write-Host "  Copied Josh Songs file" -ForegroundColor Green
Write-Host ""
Write-Host "Done! Now build:  .\gradlew.bat assembleIzzyDebug" -ForegroundColor Green
Write-Host "If anything goes wrong, go back with:  git reset --hard before-josh-v6" -ForegroundColor Gray
