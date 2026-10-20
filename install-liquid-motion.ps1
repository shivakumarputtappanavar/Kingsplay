# ============================================================================
# Kings Play: Liquid motion (iPhone style)
#   - Bottom bar: a liquid glass bubble that slides between tabs, stretches,
#     bends what is behind it, and can be dragged with your finger
#   - Every switch in Settings becomes a liquid glass switch
#   - Uses the "Liquid glass" switch in Settings (ON by default)
#   - 120 Hz: full effect with rainbow glass edges / 60 Hz: lighter version
#
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-liquid-motion.ps1
# ============================================================================

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dl = Join-Path $env:USERPROFILE "Downloads"

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
foreach ($f in @("KP_AppNavigation_liquid.kt", "KingsLiquidSwitch.kt")) {
    if (-not (Test-Path (Join-Path $dl $f))) {
        Write-Host "Missing in Downloads: $f  -> download it from the chat first." -ForegroundColor Red
        exit 1
    }
}

$src = "app\src\main\kotlin\com\metrolist\music"
$nav = "$src\ui\component\AppNavigation.kt"
if (-not (Select-String -Path $nav -Pattern "GlassNavigationBar" -Quiet)) {
    Write-Host "The liquid glass bar (earlier step) is not installed. Nothing was changed." -ForegroundColor Red
    Write-Host "Send a screenshot of this message to Claude." -ForegroundColor Yellow
    exit 1
}

# Restore point
$dirty = git status --porcelain --untracked-files=no
if ($dirty) {
    git add -A | Out-Null
    git commit -m "Before liquid motion" | Out-Null
}
git tag -f before-liquid-motion | Out-Null
Write-Host "Restore point saved: before-liquid-motion" -ForegroundColor Green

# 1. Files
Copy-Item (Join-Path $dl "KP_AppNavigation_liquid.kt") $nav -Force
Copy-Item (Join-Path $dl "KingsLiquidSwitch.kt") "$src\ui\component\KingsLiquidSwitch.kt" -Force
Write-Host "Installed liquid bottom bar + liquid switch" -ForegroundColor Green

# 2. Every Switch in the app -> liquid switch
$count = 0
Get-ChildItem -Path "app\src\main\kotlin" -Recurse -Filter *.kt | ForEach-Object {
    if ($_.Name -eq "KingsLiquidSwitch.kt") { return }
    $text = [IO.File]::ReadAllText($_.FullName)
    $new = [regex]::Replace($text, '(?m)^import androidx\.compose\.material3\.Switch(\r?)$',
        'import com.metrolist.music.ui.component.KingsLiquidSwitch as Switch$1')
    if ($new -ne $text) {
        [IO.File]::WriteAllText($_.FullName, $new, $utf8)
        $count++
    }
}
Write-Host "Liquid switches in $count screens" -ForegroundColor Green

Write-Host ""
Write-Host "Done! Now build:  .\gradlew.bat assembleIzzyDebug" -ForegroundColor Green
Write-Host "If anything goes wrong, go back with:  git reset --hard before-liquid-motion" -ForegroundColor Gray
