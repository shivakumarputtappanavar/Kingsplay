# ============================================================================
# Kings Play: Import from Spotify by link (Library)
# Paste a public Spotify playlist/album link -> songs are matched on YouTube Music
# and saved as a Kings Play playlist (up to 100 songs).
#
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-spotify.ps1
# ============================================================================

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dl = Join-Path $env:USERPROFILE "Downloads"

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
$file = Join-Path $dl "KingsSpotifyImport.kt"
if (-not (Test-Path $file)) {
    Write-Host "KingsSpotifyImport.kt is not in Downloads. Download it from the chat first." -ForegroundColor Red
    exit 1
}
$src = "app\src\main\kotlin\com\metrolist\music"
function Read-Text($p) { [IO.File]::ReadAllText((Resolve-Path $p)) }
function Write-Text($p, $t) { [IO.File]::WriteAllText((Resolve-Path $p), $t, $utf8) }

# Restore point
$dirty = git status --porcelain --untracked-files=no
if ($dirty) { git add -A | Out-Null; git commit -m "Before Spotify import" | Out-Null }
git tag -f before-spotify | Out-Null
Write-Host "Restore point saved: before-spotify" -ForegroundColor Green

Copy-Item $file "$src\ui\component\KingsSpotifyImport.kt" -Force
Write-Host "Copied KingsSpotifyImport.kt" -ForegroundColor Green

$screens = @("$src\ui\screens\library\LibraryMixScreen.kt", "$src\ui\screens\library\LibraryPlaylistsScreen.kt")
$done = 0
foreach ($p in $screens) {
    $t = Read-Text $p
    if ($t.Contains("KingsSpotifyImportHost()")) { $done++; continue }
    $rxBtn = [regex]'(?m)^([ \t]*)IconButton\(\s*onClick = \{ isSearchActive = true \},'
    $rxHost = [regex]'(?m)^([ \t]*)var showCreatePlaylistDialog by rememberSaveable \{ mutableStateOf\(false\) \}(\r?)$'
    if ($rxBtn.Matches($t).Count -ne 1 -or $rxHost.Matches($t).Count -ne 1) {
        Write-Host "Could not find the place in $(Split-Path $p -Leaf) - skipped" -ForegroundColor Yellow
        continue
    }
    $t = $rxBtn.Replace($t, {
        param($m)
        $m.Groups[1].Value + "com.metrolist.music.ui.component.KingsSpotifyImportButton()`n" + $m.Value
    }, 1)
    $t = $rxHost.Replace($t, {
        param($m)
        $m.Value + "`n" + $m.Groups[1].Value + "// Kings Play: Import from Spotify by link" + $m.Groups[2].Value + "`n" + $m.Groups[1].Value + "com.metrolist.music.ui.component.KingsSpotifyImportHost()" + $m.Groups[2].Value
    }, 1)
    Write-Text $p $t
    $done++
    Write-Host "Added Spotify import to $(Split-Path $p -Leaf)" -ForegroundColor Green
}

Write-Host ""
if ($done -eq 0) {
    Write-Host "Could not add the button. Undo with: git reset --hard before-spotify, and send a screenshot to Claude." -ForegroundColor Red
    exit 1
}
Write-Host "Done! Now build:  .\gradlew.bat assembleIzzyDebug" -ForegroundColor Green
Write-Host "If anything goes wrong, go back with:  git reset --hard before-spotify" -ForegroundColor Gray
