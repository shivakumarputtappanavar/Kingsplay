# Kings Play rename script
# Run from the project folder (C:\Projects\KingsPlay):
#   powershell -ExecutionPolicy Bypass -File .\rename-to-kingsplay.ps1
#
# Changes the app ID, the app name, and every "Metrolist" text shown
# inside the app (all languages) to "Kings Play".
# Keeps credit lines and Metrolist-specific links/services unchanged.

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}

# Lines that must stay "Metrolist" (credits, Metrolist links/services)
$keep = @(
    'wrapped_special_thanks',
    'github_releases_url',
    'discord_information',
    'discord_information_warning',
    'enable_discord_rpc',
    'kmp_upgrade_title',
    'kmp_upgrade_warning',
    'update_available_message'
)

# 1. App ID and app name
$gradle = "app\build.gradle.kts"
$t = [IO.File]::ReadAllText($gradle)
$t = $t.Replace('val baseApplicationId = "com.metrolist.music"', 'val baseApplicationId = "com.kingsplay.music"')
$t = $t.Replace('appNameOverride ?: "Metrolist")', 'appNameOverride ?: "Kings Play")')
$t = $t.Replace('"app_name", "Metrolist Debug")', '"app_name", "Kings Play")')
[IO.File]::WriteAllText((Resolve-Path $gradle), $t, $utf8)
Write-Host "App ID and name updated in app\build.gradle.kts" -ForegroundColor Green

# 2. Text inside the app, all languages
$count = 0
Get-ChildItem "app\src\main\res" -Directory -Filter "values*" | ForEach-Object {
    Get-ChildItem $_.FullName -Filter "*.xml" | ForEach-Object {
        $file = $_.FullName
        $lines = [IO.File]::ReadAllLines($file)
        $changed = $false
        for ($i = 0; $i -lt $lines.Length; $i++) {
            $line = $lines[$i]
            if ($line -match '<string name="([^"]+)"') {
                $name = $Matches[1]
                if (($keep -notcontains $name) -and ($line -cmatch 'Metrolist|METROLIST')) {
                    $lines[$i] = $line.Replace('Metrolist', 'Kings Play').Replace('METROLIST', 'KINGS PLAY')
                    $changed = $true
                    $count++
                }
            }
        }
        if ($changed) { [IO.File]::WriteAllLines($file, $lines, $utf8) }
    }
}

Write-Host "Done. Changed $count text lines to Kings Play." -ForegroundColor Green
