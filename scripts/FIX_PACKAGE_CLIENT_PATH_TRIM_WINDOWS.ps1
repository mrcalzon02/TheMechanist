$ErrorActionPreference = 'Stop'
$scriptPath = Join-Path $PSScriptRoot 'PACKAGE_CLIENT_WINDOWS.ps1'
if (-not (Test-Path -LiteralPath $scriptPath -PathType Leaf)) { throw "Missing $scriptPath" }

$text = Get-Content -LiteralPath $scriptPath -Raw

$bad1 = ".TrimEnd('\\', '/')"
$bad2 = ".TrimEnd('\\\\', '/')"
$good = ".TrimEnd([char[]]@([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar))"
$text = $text.Replace($bad1, $good).Replace($bad2, $good)

# Harden tree-copy path calculation so an unexpected rooted relative path cannot
# be joined into an invalid Windows path during PACKAGE_client source/resource merge.
$old = @'
function Copy-FilePreserveTree($sourceRoot, $destRoot, $fileInfo) {
    $sourceFull = [System.IO.Path]::GetFullPath($fileInfo.FullName)
    $rootFullLocal = [System.IO.Path]::GetFullPath($sourceRoot).TrimEnd([char[]]@([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)) + [System.IO.Path]::DirectorySeparatorChar
    $relative = if ($sourceFull.StartsWith($rootFullLocal, [System.StringComparison]::OrdinalIgnoreCase)) { $sourceFull.Substring($rootFullLocal.Length) } else { $fileInfo.Name }
    $dest = Join-Path $destRoot $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $dest) | Out-Null
    Copy-Item -LiteralPath $fileInfo.FullName -Destination $dest -Force -ErrorAction Continue
}
'@
$new = @'
function Copy-FilePreserveTree($sourceRoot, $destRoot, $fileInfo) {
    $sourceFull = [System.IO.Path]::GetFullPath([string]$fileInfo.FullName)
    $rootFullLocal = [System.IO.Path]::GetFullPath([string]$sourceRoot).TrimEnd([char[]]@([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar))
    $rootPrefixLocal = $rootFullLocal + [System.IO.Path]::DirectorySeparatorChar
    if ($sourceFull.StartsWith($rootPrefixLocal, [System.StringComparison]::OrdinalIgnoreCase)) {
        $relative = $sourceFull.Substring($rootPrefixLocal.Length)
    } else {
        $relative = [System.IO.Path]::GetFileName($sourceFull)
    }
    if ([System.IO.Path]::IsPathRooted($relative)) { $relative = [System.IO.Path]::GetFileName($relative) }
    $dest = Join-Path -Path $destRoot -ChildPath $relative
    $parent = Split-Path -Parent $dest
    if (-not [string]::IsNullOrWhiteSpace($parent)) { New-Item -ItemType Directory -Force -Path $parent | Out-Null }
    Copy-Item -LiteralPath $sourceFull -Destination $dest -Force -ErrorAction Stop
}
'@
if ($text.Contains($old)) {
    $text = $text.Replace($old, $new)
}

Set-Content -LiteralPath $scriptPath -Value $text
Write-Host 'Fixed PACKAGE_CLIENT_WINDOWS.ps1 path trimming and tree-copy path construction.'
