# ============================================================================
# Kings Play v17 fixes
#   - Liquid glass: moves up and down with your finger, list scrolls as normal,
#     no double text inside the glass, smoother (one animation per frame)
#   - Playlist pages: glow removed
#   - Josh: one-language playlists use YouTube Music's own playlists of that language
#   - Startup sound, intro animation and "Preview intro" removed
#   - Smoother scrolling (lighter row animation)
#
# Needs in Downloads:  KingsJosh.kt  KingsLiquid.kt  KingsPlaylistStyle.kt
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-v17.ps1
# ============================================================================

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dl = Join-Path $env:USERPROFILE "Downloads"

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
$src = "app\src\main\kotlin\com\metrolist\music"
function Newest($pattern) {
    Get-ChildItem -Path $dl -Filter $pattern -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
}
# file in Downloads, text that proves it is the new one, where it goes
$copies = @(
    @("KingsJosh*.kt",          "Josh v17",                 "$src\ui\screens\KingsDailyMixes.kt"),
    @("KingsLiquid*.kt",        "class LensState",          "$src\ui\component\KingsLiquid.kt"),
    @("KingsPlaylistStyle*.kt", "glow removed",             "$src\ui\component\KingsPlaylistStyle.kt")
)
$found = @()
foreach ($c in $copies) {
    $f = Newest $c[0]
    if (-not $f -or -not (Select-String -Path $f.FullName -Pattern $c[1] -SimpleMatch -Quiet)) {
        Write-Host "The new $($c[0].Replace('*','')) is not in Downloads. Delete the old ones and download it again from the chat." -ForegroundColor Red
        exit 1
    }
    if (-not (Test-Path $c[2])) {
        Write-Host "$($c[2]) is missing - run the earlier installers first (install-liquid-playlists.ps1, install-josh-v16.ps1)." -ForegroundColor Red
        exit 1
    }
    $found += ,@($f.FullName, $c[2], $f.Name)
}

function Read-Text($p) { [IO.File]::ReadAllText((Resolve-Path $p)) }
function Write-Text($p, $t) { [IO.File]::WriteAllText((Resolve-Path $p), $t, $utf8) }

$dirty = git status --porcelain --untracked-files=no
if ($dirty) { git add -A | Out-Null; git commit -m "Before v17 fixes" | Out-Null }
git tag -f before-v17 | Out-Null
Write-Host "Restore point saved: before-v17" -ForegroundColor Green

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

Patch "$src\utils\KingsAppIcons.kt" @'
fun onAppStart\(activity: ComponentActivity, savedInstanceState: Bundle\?\) \{
'@ @'
$0
        return // Kings Play: intro animation and startup sound removed
'@ 'intro animation and startup sound removed' 'Startup sound + intro animation: removed' 'one'

Patch "$src\utils\KingsAppIcons.kt" @'
fun playSound\(context: Context, icon: KingsAppIcon, force: Boolean = false\) \{
'@ @'
$0
        return // Kings Play: logo sounds removed
'@ 'logo sounds removed' 'Logo sounds: removed' 'one'

Patch "$src\ui\screens\settings\KingsDisplaySettings.kt" @'
(?s)\s*Material3SettingsItem\(\s*icon = painterResource\(R\.drawable\.[a-z_]+\),\s*title = \{ Text\("Startup sound"\) \}.*?\n\s*\),
'@ @'

'@ 'ZZZ-never' 'Settings: "Startup sound" removed' 'optional'

Patch "$src\ui\screens\settings\KingsDisplaySettings.kt" @'
(?s)\s*Material3SettingsItem\(\s*icon = painterResource\(R\.drawable\.[a-z_]+\),\s*title = \{ Text\("Intro animation"\) \}.*?\n\s*\),
'@ @'

'@ 'ZZZ-never' 'Settings: "Intro animation" removed' 'optional'

Patch "$src\ui\screens\settings\KingsDisplaySettings.kt" @'
(?s)\s*Material3SettingsItem\(\s*icon = painterResource\(R\.drawable\.[a-z_]+\),\s*title = \{ Text\("Preview intro"\) \}.*?\n\s*\),
'@ @'

'@ 'ZZZ-never' 'Settings: "Preview intro" removed' 'optional'

if ($problems.Count -gt 0) {
    git reset --hard before-v17 | Out-Null
    Write-Host ""
    Write-Host "Some parts could not be changed, so nothing was changed. Send a screenshot of this to Claude." -ForegroundColor Red
    exit 1
}

foreach ($f in $found) {
    Copy-Item $f[0] $f[1] -Force
    Write-Host "  Copied $($f[2])" -ForegroundColor Green
}
Write-Host ""
Write-Host "Done! Now build:  .\gradlew.bat assembleIzzyDebug" -ForegroundColor Green
Write-Host "Then install the NEW apk from: app\build\outputs\apk\izzy\debug" -ForegroundColor Green
Write-Host "If anything goes wrong, go back with:  git reset --hard before-v17" -ForegroundColor Gray
