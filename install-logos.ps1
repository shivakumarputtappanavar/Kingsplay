# Kings Play: 3 app logos (switch in Settings) + startup sounds
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-logos.ps1

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dl = Join-Path $env:USERPROFILE "Downloads"
if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
$src = "app\src\main\kotlin\com\metrolist\music"

foreach ($n in @("KP_Logos.zip", "KP_KingsAppIcons.kt", "KP_KingsDisplaySettings_v2.kt")) {
    if (-not (Test-Path (Join-Path $dl $n))) {
        Write-Host "Missing in Downloads: $n  -> download it from the chat first." -ForegroundColor Red
        exit 1
    }
}

# ---------- 1. Pictures, icons and sounds ----------
Expand-Archive -Path (Join-Path $dl "KP_Logos.zip") -DestinationPath "app\src\main\res" -Force
Write-Host "Installed logos, icons and sounds" -ForegroundColor Green

# ---------- 2. Code ----------
Copy-Item (Join-Path $dl "KP_KingsAppIcons.kt") "$src\utils\KingsAppIcons.kt" -Force
Copy-Item (Join-Path $dl "KP_KingsDisplaySettings_v2.kt") "$src\ui\screens\settings\KingsDisplaySettings.kt" -Force
Write-Host "Copied code files" -ForegroundColor Green

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

# ---------- 3. AndroidManifest: two extra launcher icons ----------
$man = "app\src\main\AndroidManifest.xml"
$x = Read-Text $man
if ($x.Contains(".KingsIconRoyal")) {
    Write-Host "Manifest already has the extra icons." -ForegroundColor Green
} else {
    $nl = if ($x.Contains("`r`n")) { "`r`n" } else { "`n" }
    function New-KpAlias($name, $icon) {
        return ("        <activity-alias$nl" +
                "            android:name=`".$name`"$nl" +
                "            android:enabled=`"false`"$nl" +
                "            android:exported=`"true`"$nl" +
                "            android:icon=`"@mipmap/$icon`"$nl" +
                "            android:label=`"@string/app_name`"$nl" +
                "            android:roundIcon=`"@mipmap/$icon`"$nl" +
                "            android:targetActivity=`".MainActivity`">$nl" +
                "            <intent-filter>$nl" +
                "                <action android:name=`"android.intent.action.MAIN`" />$nl" +
                "                <category android:name=`"android.intent.category.LAUNCHER`" />$nl" +
                "            </intent-filter>$nl" +
                "        </activity-alias>$nl$nl")
    }
    $anchor = "        <activity$nl            android:name=`".discord.DiscordOAuthActivity`""
    $block = "        <!-- Kings Play: extra app icons (switched in Settings > App icon & sound) -->$nl" +
             (New-KpAlias "KingsIconRoyal" "kp_icon_royal") + (New-KpAlias "KingsIconChess" "kp_icon_chess")
    $x = Replace-Once $x $anchor ($block + $anchor) "manifest icons"
    Write-Text $man $x
    Write-Host "Added Royal and Chess icons to AndroidManifest.xml" -ForegroundColor Green
}

# ---------- 4. Settings: App icon & sound at the end ----------
$set = "$src\ui\screens\settings\SettingsScreen.kt"
$s = Read-Text $set
if ($s.Contains("KingsAppIconGroup()")) {
    Write-Host "Settings already has App icon & sound." -ForegroundColor Green
} else {
    $nl = if ($s.Contains("`r`n")) { "`r`n" } else { "`n" }
    $old = "            ReleaseNotesCard()$nl        }$nl"
    $new = $old + "$nl        // Kings Play: app icon + startup sound$nl        Spacer(modifier = Modifier.height(16.dp))$nl        KingsAppIconGroup()$nl"
    $s = Replace-Once $s $old $new "Settings App icon group"
    Write-Text $set $s
    Write-Host "Added App icon & sound to the end of Settings" -ForegroundColor Green
}

# ---------- 5. MainActivity: play the sound when the app opens ----------
$main = "$src\MainActivity.kt"
$m = Read-Text $main
if ($m.Contains("KingsAppIcons.onAppStart")) {
    Write-Host "MainActivity already plays the startup sound." -ForegroundColor Green
} else {
    $nl = if ($m.Contains("`r`n")) { "`r`n" } else { "`n" }
    $m = Replace-Once $m "        super.onCreate(savedInstanceState)$nl" ("        super.onCreate(savedInstanceState)$nl" + "        // Kings Play: startup sound$nl" + "        com.metrolist.music.utils.KingsAppIcons.onAppStart(this, savedInstanceState)$nl") "startup sound"
    Write-Text $main $m
    Write-Host "MainActivity now plays the startup sound" -ForegroundColor Green
}

Write-Host ""
Write-Host "Done! Press Run. If the old icon still shows, uninstall the app once and run again." -ForegroundColor Green
