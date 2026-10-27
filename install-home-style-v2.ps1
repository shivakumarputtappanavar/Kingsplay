# ============================================================================
# Kings Play: attractive Home
#   1. Quick picks as big square cards (like Recents)
#   2. YouTube song sections (e.g. Trending songs) as big cards too
#   3. Bigger album / playlist cards
#   4. Bold section titles with a small coloured accent
#   5. Glass shine + cover-colour glow on every Home card
#
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\install-home-style-v2.ps1
# ============================================================================

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dl = Join-Path $env:USERPROFILE "Downloads"

if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "Run this from C:\Projects\KingsPlay (the project folder)." -ForegroundColor Red
    exit 1
}
$file = Join-Path $dl "KingsHomeCards_v3.kt"
if (-not (Test-Path $file)) {
    Write-Host "KingsHomeCards_v3.kt is not in Downloads. Download it from the chat first." -ForegroundColor Red
    exit 1
}
$src = "app\src\main\kotlin\com\metrolist\music"
function Read-Text($p) { [IO.File]::ReadAllText((Resolve-Path $p)) }
function Write-Text($p, $t) { [IO.File]::WriteAllText((Resolve-Path $p), $t, $utf8) }

$dirty = git status --porcelain --untracked-files=no
if ($dirty) { git add -A | Out-Null; git commit -m "Before Home style" | Out-Null }
git tag -f before-home-style | Out-Null
Write-Host "Restore point saved: before-home-style" -ForegroundColor Green

$problems = @()

# Replace text only inside one part of a file (between two markers)
function SegPatch($path, $start, $end, $pairs, $doneMarker, $what) {
    $t = Read-Text $path
    if ($t.Contains($doneMarker)) { Write-Host "  $what - already there" -ForegroundColor Green; return }
    $s = $t.IndexOf($start)
    if ($s -lt 0) { $script:problems += $what; Write-Host "  $what - could not find the place" -ForegroundColor Yellow; return }
    $e = $t.IndexOf($end, $s + $start.Length)
    if ($e -lt 0) { $script:problems += $what; Write-Host "  $what - could not find the place" -ForegroundColor Yellow; return }
    $seg = $t.Substring($s, $e - $s)
    foreach ($p in $pairs) {
        $i = $seg.IndexOf($p[0])
        if ($i -lt 0) { $script:problems += $what; Write-Host "  $what - could not find the place" -ForegroundColor Yellow; return }
        $seg = $seg.Substring(0, $i) + $p[1] + $seg.Substring($i + $p[0].Length)
    }
    Write-Text $path ($t.Substring(0, $s) + $seg + $t.Substring($e))
    Write-Host "  $what - done" -ForegroundColor Green
}

function Patch($path, $pattern, $replacement, $doneMarker, $what) {
    $t = Read-Text $path
    if ($t.Contains($doneMarker)) { Write-Host "  $what - already there" -ForegroundColor Green; return }
    $rx = [regex]$pattern
    if ($rx.Matches($t).Count -ne 1) { $script:problems += $what; Write-Host "  $what - could not find the place" -ForegroundColor Yellow; return }
    Write-Text $path ($rx.Replace($t, $replacement, 1))
    Write-Host "  $what - done" -ForegroundColor Green
}

$C = "com.metrolist.music.ui.component."
$homeFile = "$src\ui\screens\HomeScreen.kt"

# 1. Quick picks -> big cards
SegPatch $homeFile 'item(key = "quick_picks_list")' 'item(key = ' @(
    @('rows = GridCells.Fixed(4),', 'rows = GridCells.Fixed(1),'),
    @('.height(ListItemHeight * 4),', ".height(${C}KingsQuickPickHeight),"),
    @('SongListItem(', "${C}KingsQuickPickCard("),
    @('.width(horizontalLazyGridItemWidth)', ".width(${C}KingsQuickPickWidth)")
) "KingsQuickPickCard(" "Quick picks as big cards"

# 2. YouTube song sections -> big cards
SegPatch $homeFile 'if (isSongsOnlySection) {' '// Render mixed content' @(
    @('rows = GridCells.Fixed(4),', 'rows = GridCells.Fixed(1),'),
    @('.height(ListItemHeight * 4),', ".height(${C}KingsQuickPickHeight),"),
    @('YouTubeListItem(', "${C}KingsYTSongCard("),
    @('.width(horizontalLazyGridItemWidth)', ".width(${C}KingsQuickPickWidth)")
) "KingsYTSongCard(" "Song sections as big cards"

# 2b. Cover-colour glow on album / playlist / new-release cards
SegPatch $homeFile 'val ytGridItem: @Composable (YTItem) -> Unit = { item ->' 'onClick = {' @(
    ,@('.combinedClickable(', ".then(${C}kingsCardGlow(item.thumbnail)).combinedClickable(")
) "kingsCardGlow(item.thumbnail)" "Glow on album cards"

# 3. Bigger album / playlist cards
Patch "$src\constants\Dimensions.kt" 'val GridThumbnailHeight = 128\.dp' 'val GridThumbnailHeight = 152.dp' "GridThumbnailHeight = 152.dp" "Bigger album cards"

# 4. Bold section titles with accent
$nav = "$src\ui\component\NavigationTitle.kt"
Patch $nav '(?m)^import androidx\.compose\.foundation\.BorderStroke(\r?)$' 'import androidx.compose.foundation.BorderStroke$1
import androidx.compose.foundation.background$1
import androidx.compose.foundation.layout.Box$1
import androidx.compose.foundation.layout.size$1
import androidx.compose.ui.draw.clip$1
import androidx.compose.ui.graphics.Brush$1' "import androidx.compose.ui.graphics.Brush" "Title imports"
Patch $nav 'fontWeight = FontWeight\.Bold,(\s*)color = MaterialTheme\.colorScheme\.primary,(\s*)overflow = TextOverflow\.Ellipsis,' 'fontWeight = FontWeight.Black,$1color = MaterialTheme.colorScheme.onSurface,$2overflow = TextOverflow.Ellipsis,' "fontWeight = FontWeight.Black," "Bold section titles"
Patch $nav '(?m)^([ \t]*)thumbnail\?\.invoke\(\)(\r?)$' '$1thumbnail?.invoke()$2
$1// Kings Play: small coloured accent before section titles$2
$1if (thumbnail == null) {$2
$1    Box($2
$1        Modifier$2
$1            .size(width = 4.dp, height = 22.dp)$2
$1            .clip(RoundedCornerShape(2.dp))$2
$1            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))$2
$1    )$2
$1}$2' "small coloured accent before section titles" "Title accent"

if ($problems.Count -gt 0) {
    git reset --hard before-home-style | Out-Null
    Write-Host ""
    Write-Host "Some parts could not be added, so nothing was changed. Send a screenshot of this to Claude." -ForegroundColor Red
    exit 1
}

Copy-Item $file "$src\ui\component\KingsHomeCards.kt" -Force
Write-Host "  Copied new card designs" -ForegroundColor Green
Write-Host ""
Write-Host "Done! Now build:  .\gradlew.bat assembleIzzyDebug  - then pull down on Home to refresh" -ForegroundColor Green
Write-Host "If anything goes wrong, go back with:  git reset --hard before-home-style" -ForegroundColor Gray
