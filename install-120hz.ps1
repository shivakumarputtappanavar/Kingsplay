# ============================================================================
# Kings Play: very smooth 120 Hz
#   1. The app is built like a finished (release) app: Android runs it much faster
#      (same app, same name, updates over the old one, friends' apps keep working)
#   2. Always 120 Hz while Kings Play is open (no drop to 60 Hz when you stop touching)
#
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-120hz.ps1
# ============================================================================

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
$src = "app\src\main\kotlin\com\metrolist\music"
function Read-Text($p) { [IO.File]::ReadAllText((Resolve-Path $p)) }
function Write-Text($p, $t) { [IO.File]::WriteAllText((Resolve-Path $p), $t, $utf8) }

$dirty = git status --porcelain --untracked-files=no
if ($dirty) { git add -A | Out-Null; git commit -m "Before 120 Hz" | Out-Null }
git tag -f before-120hz | Out-Null
Write-Host "Restore point saved: before-120hz" -ForegroundColor Green

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

Patch "app\build.gradle.kts" @'
(?m)^([ \t]*)isDebuggable = true(\r?)$
'@ @'
$1isDebuggable = false // Kings Play: runs as fast as a finished (release) app$2
$1isMinifyEnabled = true$2
$1isShrinkResources = true$2
$1proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")$2
'@ 'runs as fast as a finished (release) app' 'Fast app (not a slow debug app)' 'one'

Patch "$src\ui\theme\KingsPerformance.kt" @'
window\.kingsApplyRate\(fast = true\)(\s*)delay\(KingsFastHoldMillis\)(\s*)window\.kingsApplyRate\(fast = false\)
'@ @'
window.kingsApplyRate(fast = true) // Kings Play: always 120 Hz while the app is open
'@ 'always 120 Hz while the app is open' 'Always 120 Hz (no drop to 60 Hz)' 'one'

if ($problems.Count -gt 0) {
    git reset --hard before-120hz | Out-Null
    Write-Host ""
    Write-Host "Some parts could not be changed, so nothing was changed. Send a screenshot of this to Claude." -ForegroundColor Red
    exit 1
}
Write-Host ""
Write-Host "Done! Now build:  .\gradlew.bat clean assembleIzzyDebug   (takes a few minutes longer now)" -ForegroundColor Green
Write-Host "Then install the NEW apk from: app\build\outputs\apk\izzy\debug" -ForegroundColor Green
Write-Host "If anything goes wrong, go back with:  git reset --hard before-120hz" -ForegroundColor Gray
