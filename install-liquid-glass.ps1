# Kings Play: install liquid glass (and Kings Play styles)
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-liquid-glass.ps1
#
# 1. Copies the Kings Play files from your Downloads folder into the project
# 2. Adds the Backdrop (liquid glass) library to app\build.gradle.kts
# 3. Connects the glass to the screens in MainActivity.kt (3 small changes)

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dl = Join-Path $env:USERPROFILE "Downloads"

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}

$src = "app\src\main\kotlin\com\metrolist\music"

# ---------- 1. Copy files ----------
$required = @(
    @("KP_KingsGlass.kt",          "$src\ui\theme\KingsGlass.kt"),
    @("KP_KingsStyle_v2.kt",       "$src\ui\theme\KingsStyle.kt"),
    @("KP_AppNavigation_glass.kt", "$src\ui\component\AppNavigation.kt")
)
$optional = @(
    @("KP_Theme.kt",       "$src\ui\theme\Theme.kt"),
    @("KP_ThemeScreen.kt", "$src\ui\screens\settings\ThemeScreen.kt")
)

foreach ($f in $required) {
    $from = Join-Path $dl $f[0]
    if (-not (Test-Path $from)) {
        Write-Host "Missing in Downloads: $($f[0])  -> download it from the chat first." -ForegroundColor Red
        exit 1
    }
}
foreach ($f in $required + $optional) {
    $from = Join-Path $dl $f[0]
    if (Test-Path $from) {
        Copy-Item $from $f[1] -Force
        Write-Host "Copied $($f[0])" -ForegroundColor Green
    } else {
        Write-Host "Skipped $($f[0]) (not in Downloads)" -ForegroundColor Yellow
    }
}

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

# ---------- 2. Library ----------
$gradle = "app\build.gradle.kts"
$g = Read-Text $gradle
if ($g.Contains("io.github.kyant0:backdrop")) {
    Write-Host "Liquid glass library already added." -ForegroundColor Green
} else {
    $nl = if ($g.Contains("`r`n")) { "`r`n" } else { "`n" }
    $g = Replace-Once $g "    implementation(libs.compose.ui)$nl" "    implementation(libs.compose.ui)$nl    implementation(`"io.github.kyant0:backdrop:1.0.6`") // Kings Play liquid glass$nl" "library line"
    Write-Text $gradle $g
    Write-Host "Added liquid glass library to app\build.gradle.kts" -ForegroundColor Green
}

# ---------- 3. MainActivity ----------
$main = "$src\MainActivity.kt"
$m = Read-Text $main
if ($m.Contains("rememberKingsBackdrop()")) {
    Write-Host "MainActivity already connected." -ForegroundColor Green
} else {
    $nl = if ($m.Contains("`r`n")) { "`r`n" } else { "`n" }

    $m = Replace-Once $m `
        "import com.metrolist.music.ui.theme.MetrolistTheme$nl" `
        ("import com.metrolist.music.ui.theme.MetrolistTheme$nl" +
         "import com.metrolist.music.ui.theme.LocalKingsBackdrop$nl" +
         "import com.metrolist.music.ui.theme.rememberKingsBackdrop$nl" +
         "import com.kyant.backdrop.backdrops.layerBackdrop$nl") `
        "imports"

    $m = Replace-Once $m `
        "                CompositionLocalProvider($nl                    LocalDatabase provides database,$nl" `
        ("                val kingsBackdrop = rememberKingsBackdrop()$nl" +
         "                CompositionLocalProvider($nl                    LocalDatabase provides database,$nl") `
        "backdrop creation"

    $m = Replace-Once $m `
        "                    LocalArtistNameAliases provides artistNameAliases,$nl" `
        ("                    LocalArtistNameAliases provides artistNameAliases,$nl" +
         "                    LocalKingsBackdrop provides kingsBackdrop,$nl") `
        "backdrop provider"

    $m = Replace-Once $m `
        "                                    modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),$nl" `
        "                                    modifier = Modifier.layerBackdrop(kingsBackdrop).nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),$nl" `
        "screen recording"

    Write-Text $main $m
    Write-Host "Connected liquid glass in MainActivity.kt" -ForegroundColor Green
}

Write-Host ""
Write-Host "Done! Now click 'Sync Now' (or the elephant icon), then press Run." -ForegroundColor Green
