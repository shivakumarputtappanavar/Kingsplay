# Kings Play: animated "Home thought" (default: ONE WORLD. ONE BEAT), editable in Settings
# Needs install-logos.ps1 and install-intro.ps1 to have been run first.
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-thought.ps1

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dl = Join-Path $env:USERPROFILE "Downloads"
if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
$src = "app\src\main\kotlin\com\metrolist\music"
if (-not ((Get-Content "$src\utils\KingsAppIcons.kt" -Raw -ErrorAction SilentlyContinue) -match "KingsIntroState")) {
    Write-Host "Please run install-logos.ps1 and install-intro.ps1 first, then run this again." -ForegroundColor Red
    exit 1
}
$files = @(
    @("KP_KingsThought.kt",            "$src\ui\theme\KingsThought.kt"),
    @("KP_KingsIntro_v2.kt",           "$src\ui\theme\KingsIntro.kt"),
    @("KP_KingsDisplaySettings_v4.kt", "$src\ui\screens\settings\KingsDisplaySettings.kt")
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

# MainActivity: Home title -> animated thought
$main = "$src\MainActivity.kt"
$m = [IO.File]::ReadAllText((Resolve-Path $main))
if ($m.Contains("KingsHomeThought()")) {
    Write-Host "MainActivity already shows the Home thought." -ForegroundColor Green
} else {
    $nl = if ($m.Contains("`r`n")) { "`r`n" } else { "`n" }
    $old = "                                        title = {$nl" +
           "                                            Text($nl" +
           "                                                text = currentTitleRes?.let { stringResource(it) } ?: `"`",$nl" +
           "                                                style = MaterialTheme.typography.titleLarge,$nl" +
           "                                            )$nl" +
           "                                        },$nl"
    $new = "                                        title = {$nl" +
           "                                            if (currentTitleRes == R.string.home) {$nl" +
           "                                                // Kings Play: animated thought instead of `"Home`"$nl" +
           "                                                com.metrolist.music.ui.theme.KingsHomeThought()$nl" +
           "                                            } else {$nl" +
           "                                            Text($nl" +
           "                                                text = currentTitleRes?.let { stringResource(it) } ?: `"`",$nl" +
           "                                                style = MaterialTheme.typography.titleLarge,$nl" +
           "                                            )$nl" +
           "                                            }$nl" +
           "                                        },$nl"
    $count = ([regex]::Matches($m, [regex]::Escape($old))).Count
    if ($count -ne 1) {
        Write-Host "Could not find the Home title (found $count). Nothing was changed in MainActivity. Send a screenshot to Claude." -ForegroundColor Red
        exit 1
    }
    $m = $m.Replace($old, $new)
    [IO.File]::WriteAllText((Resolve-Path $main), $m, $utf8)
    Write-Host "Home title now shows the animated thought" -ForegroundColor Green
}
Write-Host ""
Write-Host "Done! Press Run." -ForegroundColor Green
