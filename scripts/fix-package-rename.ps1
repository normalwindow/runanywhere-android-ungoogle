$ErrorActionPreference = 'Stop'

$replacements = [ordered]@{
  'xyz.normalwindow.runanywherePortableEmbeddingSmokeTest' = 'xyz.normalwindow.runanywhere.PortableEmbeddingSmokeTest'
  'xyz.normalwindow.runanywherePortableNvidiaE2ETest' = 'xyz.normalwindow.runanywhere.PortableNvidiaE2ETest'
  'xyz.normalwindow.runanywhereRunAnywhereApplication' = 'xyz.normalwindow.runanywhere.RunAnywhereApplication'
  'xyz.normalwindow.runanywhereNpuLoadOnlyTest' = 'xyz.normalwindow.runanywhere.NpuLoadOnlyTest'
  'xyz.normalwindow.runanywhereBuildConfig' = 'xyz.normalwindow.runanywhere.BuildConfig'
  'xyz.normalwindow.runanywhereMainActivity' = 'xyz.normalwindow.runanywhere.MainActivity'
  'xyz.normalwindow.runanywheredownload' = 'xyz.normalwindow.runanywhere.download'
  'xyz.normalwindow.runanywhereutil' = 'xyz.normalwindow.runanywhere.util'
  'xyz.normalwindow.runanywheretools' = 'xyz.normalwindow.runanywhere.tools'
  'xyz.normalwindow.runanywherestate' = 'xyz.normalwindow.runanywhere.state'
  'xyz.normalwindow.runanywheresecure' = 'xyz.normalwindow.runanywhere.secure'
  'xyz.normalwindow.runanywhereaction' = 'xyz.normalwindow.runanywhere.action'
  'xyz.normalwindow.runanywheredata' = 'xyz.normalwindow.runanywhere.data'
  'xyz.normalwindow.runanywhereui' = 'xyz.normalwindow.runanywhere.ui'
  'xyz.normalwindow.runanywhereR' = 'xyz.normalwindow.runanywhere.R'
}

$roots = @('app\src\main', 'app\src\test', 'app\src\androidTest', 'app\src\debug', 'docs', 'scripts')
$files = foreach ($root in $roots) {
  if (Test-Path $root) {
    Get-ChildItem -Path $root -Recurse -File | Where-Object {
      $_.FullName -notmatch '\\build\\' -and $_.Extension -match '\.(kt|kts|xml|pro|md|properties|sh|java)$'
    }
  }
}

$changed = 0
foreach ($file in $files) {
  $original = [System.IO.File]::ReadAllText($file.FullName)
  $updated = $original
  foreach ($pair in $replacements.GetEnumerator()) {
    $updated = $updated.Replace($pair.Key, $pair.Value)
  }
  if ($updated -ne $original) {
    [System.IO.File]::WriteAllText($file.FullName, $updated)
    $changed++
  }
}

Write-Output "Updated $changed files"

$moves = @(
  @{ From = 'app\src\main\java\com\runanywhere\runanywhereai'; To = 'app\src\main\java\xyz\normalwindow\runanywhere' },
  @{ From = 'app\src\test\java\com\runanywhere\runanywhereai'; To = 'app\src\test\java\xyz\normalwindow\runanywhere' },
  @{ From = 'app\src\androidTest\java\com\runanywhere\runanywhereai'; To = 'app\src\androidTest\java\xyz\normalwindow\runanywhere' }
)

foreach ($move in $moves) {
  if (-not (Test-Path $move.From)) { continue }
  $destParent = Split-Path $move.To -Parent
  New-Item -ItemType Directory -Force -Path $destParent | Out-Null
  if (Test-Path $move.To) {
    throw "Destination already exists: $($move.To)"
  }
  Move-Item -Path $move.From -Destination $move.To
  Write-Output "Moved $($move.From) -> $($move.To)"
}

foreach ($stale in @(
  'app\src\main\java\com',
  'app\src\test\java\com',
  'app\src\androidTest\java\com'
)) {
  if ((Test-Path $stale) -and -not (Get-ChildItem -Path $stale -Recurse -File -ErrorAction SilentlyContinue)) {
    Remove-Item -Path $stale -Recurse -Force
    Write-Output "Removed empty $stale"
  }
}

$leftovers = Get-ChildItem -Path app,scripts,docs -Recurse -File |
  Where-Object { $_.FullName -notmatch '\\build\\' } |
  Select-String -Pattern 'xyz\.normalwindow\.runanywhere[A-Za-z0-9_]+' -AllMatches |
  ForEach-Object { $_.Matches.Value } |
  Sort-Object -Unique

if ($leftovers) {
  Write-Output 'LEFTOVER MANGLED IDENTIFIERS:'
  $leftovers
} else {
  Write-Output 'No leftover mangled identifiers'
}
