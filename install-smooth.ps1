# ============================================================================
# Kings Play: Smooth + fast + battery saving update
#
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-smooth.ps1
#
#  - 60 Hz : steady 60 Hz, saves the most battery
#  - 120 Hz: Smart - ultra smooth while touching/scrolling, 60 Hz when idle
#  - No more never-ending animations (play button glow, Home thought shine)
#  - Liquid glow updates 30x a second instead of 120x
#  - Lighter liquid glass, shorter intro, startup sound loads in background
# ============================================================================

$ErrorActionPreference = "Stop"

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}

$zip = Join-Path $env:USERPROFILE "Downloads\KP_Smooth.zip"
if (-not (Test-Path $zip)) {
    Write-Host "KP_Smooth.zip is not in Downloads. Download it from the chat first." -ForegroundColor Red
    exit 1
}

# Make sure the earlier Kings Play parts are installed (these files get replaced)
$src = "app\src\main\kotlin\com\metrolist\music"
$need = @(
    @("$src\ui\theme\Theme.kt",                       "KingsIntroHost"),
    @("$src\ui\theme\KingsStyle.kt",                  "KingsPlayerStyle"),
    @("$src\ui\theme\KingsGlass.kt",                  "kingsGlass"),
    @("$src\ui\theme\KingsThought.kt",                "KingsHomeThought"),
    @("$src\ui\theme\KingsIntro.kt",                  "KingsIntroScreen"),
    @("$src\ui\player\KingsPremium.kt",               "kingsPlayButtonFx"),
    @("$src\utils\KingsAppIcons.kt",                  "KingsIntroState"),
    @("$src\ui\screens\settings\KingsDisplaySettings.kt", "KingsThoughtDialog|ThoughtDialog|Home thought")
)
foreach ($n in $need) {
    if (-not (Test-Path $n[0]) -or -not (Select-String -Path $n[0] -Pattern $n[1] -Quiet)) {
        Write-Host "An earlier Kings Play step is missing: $($n[0])" -ForegroundColor Red
        Write-Host "Nothing was changed. Send a screenshot of this message to Claude." -ForegroundColor Yellow
        exit 1
    }
}

# Save a restore point first
$dirty = git status --porcelain --untracked-files=no
if ($dirty) {
    git add -A | Out-Null
    git commit -m "Before smooth update" | Out-Null
}
git tag -f before-smooth | Out-Null
Write-Host "Restore point saved: before-smooth" -ForegroundColor Green

Expand-Archive -Path $zip -DestinationPath . -Force
Write-Host "Installed 9 smooth/battery files" -ForegroundColor Green

Write-Host ""
Write-Host "Done! Now build:  .\gradlew.bat assembleIzzyDebug" -ForegroundColor Green
Write-Host "If anything goes wrong, go back with:  git reset --hard before-smooth" -ForegroundColor Gray
