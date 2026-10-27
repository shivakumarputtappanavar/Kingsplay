# Kings Play: Library cards fix
#  - Card text no longer overlaps
#  - Tap a card to see what you listened to (songs / artists / albums)
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-library-fix.ps1

$ErrorActionPreference = "Stop"
if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
$file = Join-Path $env:USERPROFILE "Downloads\KingsLibrary_v2.kt"
if (-not (Test-Path $file)) {
    Write-Host "KingsLibrary_v2.kt is not in Downloads. Download it from the chat first." -ForegroundColor Red
    exit 1
}
$dirty = git status --porcelain --untracked-files=no
if ($dirty) { git add -A | Out-Null; git commit -m "Before library cards fix" | Out-Null }
git tag -f before-library-fix | Out-Null
Copy-Item $file "app\src\main\kotlin\com\metrolist\music\ui\screens\library\KingsLibrary.kt" -Force
Write-Host "Library cards fixed" -ForegroundColor Green
Write-Host "Done! Now build:  .\gradlew.bat assembleIzzyDebug" -ForegroundColor Green
Write-Host "If anything goes wrong, go back with:  git reset --hard before-library-fix" -ForegroundColor Gray
