# Kings Play: animated intro + new startup sounds
# Needs install-logos.ps1 to have been run first.
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-intro.ps1

$ErrorActionPreference = "Stop"
$dl = Join-Path $env:USERPROFILE "Downloads"
if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
if (-not ((Get-Content "app\src\main\AndroidManifest.xml" -Raw).Contains(".KingsIconRoyal"))) {
    Write-Host "Please run install-logos.ps1 first (the logos), then run this again." -ForegroundColor Red
    exit 1
}
$src = "app\src\main\kotlin\com\metrolist\music"
$files = @(
    @("KP_KingsAppIcons_v2.kt",         "$src\utils\KingsAppIcons.kt"),
    @("KP_KingsIntro.kt",               "$src\ui\theme\KingsIntro.kt"),
    @("KP_Theme_v2.kt",                 "$src\ui\theme\Theme.kt"),
    @("KP_KingsDisplaySettings_v3.kt",  "$src\ui\screens\settings\KingsDisplaySettings.kt")
)
foreach ($n in @("KP_Sounds_v2.zip") + ($files | ForEach-Object { $_[0] })) {
    if (-not (Test-Path (Join-Path $dl $n))) {
        Write-Host "Missing in Downloads: $n  -> download it from the chat first." -ForegroundColor Red
        exit 1
    }
}
Expand-Archive -Path (Join-Path $dl "KP_Sounds_v2.zip") -DestinationPath "app\src\main\res" -Force
Write-Host "Installed the new startup sounds" -ForegroundColor Green
foreach ($f in $files) {
    Copy-Item (Join-Path $dl $f[0]) $f[1] -Force
    Write-Host "Copied $($f[0])" -ForegroundColor Green
}
Write-Host ""
Write-Host "Done! Press Run, then close the app fully and open it again to see the intro." -ForegroundColor Green
