# Kings Play: install Premium player + Refresh rate (60 Hz / 120 Hz) in main Settings
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-premium.ps1

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dl = Join-Path $env:USERPROFILE "Downloads"

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
$src = "app\src\main\kotlin\com\metrolist\music"

# ---------- 1. Copy files ----------
$files = @(
    @("KP_KingsPremium.kt",         "$src\ui\player\KingsPremium.kt"),
    @("KP_KingsDisplaySettings.kt", "$src\ui\screens\settings\KingsDisplaySettings.kt"),
    @("KP_Player_v3.kt",            "$src\ui\player\Player.kt"),
    @("KP_KingsStyle_v5.kt",        "$src\ui\theme\KingsStyle.kt")
)
foreach ($f in $files) {
    if (-not (Test-Path (Join-Path $dl $f[0]))) {
        Write-Host "Missing in Downloads: $($f[0])  -> download it from the chat first." -ForegroundColor Red
        exit 1
    }
}
foreach ($f in $files) {
    Copy-Item (Join-Path $dl $f[0]) $f[1] -Force
    Write-Host "Copied $($f[0])" -ForegroundColor Green
}

function Read-Text($path) { return [IO.File]::ReadAllText((Resolve-Path $path)) }
function Write-Text($path, $text) { [IO.File]::WriteAllText((Resolve-Path $path), $text, $utf8) }
function Replace-Once([string]$text, [string]$old, [string]$new, [string]$what) {
    $count = ([regex]::Matches($text, [regex]::Escape($old))).Count
    if ($count -ne 1) {
        Write-Host "Could not find the place for: $what  (found $count). Nothing was changed in this file." -ForegroundColor Red
        Write-Host "Send a screenshot of this message to Claude." -ForegroundColor Red
        exit 1
    }
    return $text.Replace($old, $new)
}

# ---------- 2. Main Settings: Display group ----------
$set = "$src\ui\screens\settings\SettingsScreen.kt"
$s = Read-Text $set
if ($s.Contains("KingsDisplaySettingsGroup()")) {
    Write-Host "Settings already has the Display group." -ForegroundColor Green
} else {
    $nl = if ($s.Contains("`r`n")) { "`r`n" } else { "`n" }
    $old = "                    onClick = { navController.navigate(`"settings/appearance`") }$nl                )$nl            )$nl        )$nl"
    $new = $old + "$nl        // Kings Play: refresh rate (60 Hz / 120 Hz)$nl        Spacer(modifier = Modifier.height(16.dp))$nl        KingsDisplaySettingsGroup()$nl"
    $s = Replace-Once $s $old $new "Settings Display group"
    Write-Text $set $s
    Write-Host "Added Display (refresh rate) to main Settings" -ForegroundColor Green
}

# ---------- 3. MainActivity: real 120 Hz (fastest mode) ----------
$main = "$src\MainActivity.kt"
$m = Read-Text $main
if ($m.Contains("Kings Play: pick the fastest")) {
    Write-Host "MainActivity refresh rate already updated." -ForegroundColor Green
} else {
    $nl = if ($m.Contains("`r`n")) { "`r`n" } else { "`n" }
    $m = Replace-Once $m `
        "                    layoutParams.preferredDisplayModeId = 0$nl" `
        ("                    // Kings Play: pick the fastest mode at the current resolution (e.g. 120 Hz)$nl" +
         "                    val kpDisplay = window.windowManager.defaultDisplay$nl" +
         "                    val kpCurrent = kpDisplay.mode$nl" +
         "                    val kpFastest = kpDisplay.supportedModes$nl" +
         "                        .filter { it.physicalWidth == kpCurrent.physicalWidth && it.physicalHeight == kpCurrent.physicalHeight }$nl" +
         "                        .maxByOrNull { it.refreshRate }$nl" +
         "                    layoutParams.preferredDisplayModeId = kpFastest?.modeId ?: 0$nl") `
        "high refresh mode"
    $m = Replace-Once $m `
        "                    params.preferredRefreshRate = 0f$nl" `
        "                    params.preferredRefreshRate = window.windowManager.defaultDisplay.supportedRefreshRates.maxOrNull() ?: 0f$nl" `
        "high refresh rate (old Android)"
    Write-Text $main $m
    Write-Host "MainActivity now uses the phone's fastest refresh rate when 120 Hz is chosen" -ForegroundColor Green
}

Write-Host ""
Write-Host "Done! Now press Run." -ForegroundColor Green
