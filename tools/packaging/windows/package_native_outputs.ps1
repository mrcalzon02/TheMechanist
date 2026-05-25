param(
    [string] $NativeRoot = "dist/native/windows",
    [string] $OutputDir = "dist/native/windows/downloads",
    [string] $Version = "dev"
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")
$NativePath = Join-Path $RepoRoot $NativeRoot
$DownloadPath = Join-Path $RepoRoot $OutputDir

function Step($Message) {
    Write-Host ""
    Write-Host "============================================================"
    Write-Host $Message
    Write-Host "============================================================"
}

function Add-ZipIfPresent($Name, $Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        Write-Host "Skipping missing artifact source: $Path"
        return
    }
    $safeVersion = $Version -replace '[^A-Za-z0-9._-]', '_'
    $zip = Join-Path $DownloadPath ("TheMechanist_{0}_{1}.zip" -f $Name, $safeVersion)
    if (Test-Path -LiteralPath $zip) { Remove-Item -LiteralPath $zip -Force }
    Compress-Archive -LiteralPath $Path -DestinationPath $zip -Force
    Write-Host "Created: $zip"
}

Step "The Mechanist Download Package Assembly"
Write-Host "Repo:       $RepoRoot"
Write-Host "NativeRoot: $NativePath"
Write-Host "OutputDir:  $DownloadPath"
Write-Host "Version:    $Version"

New-Item -ItemType Directory -Force -Path $DownloadPath | Out-Null

Add-ZipIfPresent "LauncherAppImage" (Join-Path $NativePath "app-image")
Add-ZipIfPresent "GameAppImage" (Join-Path $NativePath "game-app-image")
Add-ZipIfPresent "ServerAppImage" (Join-Path $NativePath "server-app-image")
Add-ZipIfPresent "LauncherInstaller" (Join-Path $NativePath "launcher-installer")

$manifest = Join-Path $NativePath "release-manifest.json"
if (Test-Path -LiteralPath $manifest) {
    Copy-Item -LiteralPath $manifest -Destination (Join-Path $DownloadPath "release-manifest.json") -Force
}

Step "Download package assembly complete"
Get-ChildItem -LiteralPath $DownloadPath -File | Format-Table Name, Length, LastWriteTime -AutoSize
