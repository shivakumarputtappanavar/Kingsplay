# Kings Play: add "Sound > Dolby Atmos" to main Settings
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-dolby.ps1

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
$file = Join-Path $env:USERPROFILE "Downloads\KingsSoundSettings.kt"
if (-not (Test-Path $file)) {
    Write-Host "KingsSoundSettings.kt is not in Downloads. Download it from the chat first." -ForegroundColor Red
    exit 1
}

$src = "app\src\main\kotlin\com\metrolist\music"
Copy-Item $file "$src\ui\screens\settings\KingsSoundSettings.kt" -Force
Write-Host "Copied KingsSoundSettings.kt" -ForegroundColor Green

$set = "$src\ui\screens\settings\SettingsScreen.kt"
$s = [IO.File]::ReadAllText((Resolve-Path $set))
if ($s.Contains("KingsSoundSettingsGroup()")) {
    Write-Host "Settings already has the Sound group." -ForegroundColor Green
} else {
    $nl = if ($s.Contains("`r`n")) { "`r`n" } else { "`n" }
    $old = "        KingsDisplaySettingsGroup()$nl"
    $count = ([regex]::Matches($s, [regex]::Escape($old))).Count
    if ($count -ne 1) {
        Write-Host "Could not find the Display group in Settings (found $count). Nothing was changed." -ForegroundColor Red
        Write-Host "Send a screenshot of this message to Claude." -ForegroundColor Yellow
        exit 1
    }
    $new = $old + "$nl        // Kings Play: Sound (Dolby Atmos)$nl        Spacer(modifier = Modifier.height(16.dp))$nl        KingsSoundSettingsGroup()$nl"
    $s = $s.Replace($old, $new)
    [IO.File]::WriteAllText((Resolve-Path $set), $s, $utf8)
    Write-Host "Added Sound > Dolby Atmos to main Settings (under Display)" -ForegroundColor Green
}

Write-Host ""
Write-Host "Done! Now build:  .\gradlew.bat assembleIzzyDebug" -ForegroundColor Green
