# Kings Play: your own About page + remove Changelog
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-about.ps1

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dl = Join-Path $env:USERPROFILE "Downloads"
if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
$src = "app\src\main\kotlin\com\metrolist\music"
$from = Join-Path $dl "KP_AboutScreen_v2.kt"
if (-not (Test-Path $from)) {
    Write-Host "Missing in Downloads: KP_AboutScreen_v2.kt -> download it from the chat first." -ForegroundColor Red
    exit 1
}
Copy-Item $from "$src\ui\screens\settings\AboutScreen.kt" -Force
Write-Host "Installed your About page" -ForegroundColor Green

function Read-Text($path) { return [IO.File]::ReadAllText((Resolve-Path $path)) }
function Write-Text($path, $text) { [IO.File]::WriteAllText((Resolve-Path $path), $text, $utf8) }

# Settings: remove the Changelog item
$set = "$src\ui\screens\settings\SettingsScreen.kt"
$s = Read-Text $set
$nl = if ($s.Contains("`r`n")) { "`r`n" } else { "`n" }
$old = "                val showChangelog = com.metrolist.music.LocalChangelogState.current$nl" +
       "                add($nl" +
       "                    Material3SettingsItem($nl" +
       "                        icon = painterResource(R.drawable.newspaper),$nl" +
       "                        title = { Text(stringResource(R.string.changelog)) },$nl" +
       "                        onClick = { showChangelog.value = true }$nl" +
       "                    )$nl" +
       "                )$nl"
if ($s.Contains($old)) {
    $s = $s.Replace($old, "                // Kings Play: Changelog removed$nl")
    Write-Text $set $s
    Write-Host "Removed Changelog from Settings" -ForegroundColor Green
} elseif ($s.Contains("Kings Play: Changelog removed")) {
    Write-Host "Changelog was already removed." -ForegroundColor Green
} else {
    Write-Host "Could not find the Changelog item in Settings. Send a screenshot to Claude." -ForegroundColor Yellow
}

# MainActivity: stop the automatic changelog popup after updates
$main = "$src\MainActivity.kt"
$m = Read-Text $main
$nl = if ($m.Contains("`r`n")) { "`r`n" } else { "`n" }
$old = "                    if (lastSeenVersion != currentVersion) {$nl                        showChangelog.value = true$nl"
$new = "                    if (lastSeenVersion != currentVersion) {$nl                        // Kings Play: changelog popup turned off$nl"
if ($m.Contains($old)) {
    $m = $m.Replace($old, $new)
    Write-Text $main $m
    Write-Host "Turned off the automatic changelog popup" -ForegroundColor Green
} elseif ($m.Contains("Kings Play: changelog popup turned off")) {
    Write-Host "Changelog popup was already off." -ForegroundColor Green
} else {
    Write-Host "Could not find the changelog popup in MainActivity. Send a screenshot to Claude." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Done! Press Run, then open Settings > About." -ForegroundColor Green
