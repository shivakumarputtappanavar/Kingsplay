# Kings Play: fix "Unresolved reference 'dp'" in SearchScreen.kt
# Run from C:\Projects\KingsPlay :
#   powershell -ExecutionPolicy Bypass -File .\fix-search-dp.ps1
$ErrorActionPreference = "Stop"
$f = "app\src\main\kotlin\com\metrolist\music\ui\screens\search\SearchScreen.kt"
if (-not (Test-Path $f)) { Write-Host "Run this from C:\Projects\KingsPlay" -ForegroundColor Red; exit 1 }
$t = [IO.File]::ReadAllText((Resolve-Path $f))
if ($t -match '(?m)^import androidx\.compose\.ui\.unit\.dp\r?$') {
    Write-Host "dp import - already there" -ForegroundColor Green
} else {
    $t = $t.Replace("import com.metrolist.music.ui.component.kingsLiquidPress", "import com.metrolist.music.ui.component.kingsLiquidPress`nimport androidx.compose.ui.unit.dp")
    [IO.File]::WriteAllText((Resolve-Path $f), $t, (New-Object System.Text.UTF8Encoding($false)))
    Write-Host "dp import - added" -ForegroundColor Green
}
Write-Host "Now build again:  .\gradlew.bat assembleIzzyDebug" -ForegroundColor Green
