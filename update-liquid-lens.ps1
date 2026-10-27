# Kings Play: liquid glass shows as soon as you touch (playlists, search, settings, menus)
# Needs in Downloads: KingsLiquid.kt (newest version)
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\update-liquid-lens.ps1
$ErrorActionPreference = "Stop"
$dl = Join-Path $env:USERPROFILE "Downloads"
$target = "app\src\main\kotlin\com\metrolist\music\ui\component\KingsLiquid.kt"
if (-not (Test-Path "app\build.gradle.kts")) { Write-Host "Run this from C:\Projects\KingsPlay" -ForegroundColor Red; exit 1 }
if (-not (Test-Path $target)) { Write-Host "Liquid is not installed yet. Run install-liquid-playlists.ps1 and install-liquid-lens.ps1 first." -ForegroundColor Red; exit 1 }
$f = Get-ChildItem -Path $dl -Filter "KingsLiquid*.kt" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $f -or -not (Select-String -Path $f.FullName -Pattern "glass appears under the finger at once" -Quiet)) {
    Write-Host "The newest KingsLiquid.kt is not in Downloads. Delete old KingsLiquid files and download it again from the chat." -ForegroundColor Red
    exit 1
}
Copy-Item $f.FullName $target -Force
Write-Host "New KingsLiquid.kt copied ($($f.Name))" -ForegroundColor Green
Write-Host "Now build again:  .\gradlew.bat assembleIzzyDebug" -ForegroundColor Green
