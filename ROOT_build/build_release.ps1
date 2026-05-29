param(
    [string] $Version = 'dev',
    [switch] $SkipJarAudit,
    [switch] $NoZip
)

$ErrorActionPreference = 'Stop'
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$ReleaseRoot = Join-Path $RepoRoot 'ROOT_RELEASE'
$Stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$SafeVersion = ($Version -replace '[^A-Za-z0-9._-]+', '_')
$ReleaseStage = Join-Path $ReleaseRoot "TheMechanist-$SafeVersion-$Stamp"
$ClientStage = Join-Path $ReleaseStage 'client'
$ServerStage = Join-Path $ReleaseStage 'server'
$ManifestPath = Join-Path $ReleaseStage 'release_manifest.json'

function Write-Step([string] $Message) {
    Write-Host "`n=== $Message ==="
}

function Copy-Tree([string] $Source, [string] $Destination) {
    if (-not (Test-Path -LiteralPath $Source -PathType Container)) {
        throw "Missing source package directory: $Source"
    }
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Destination) | Out-Null
    if (Test-Path -LiteralPath $Destination) {
        Remove-Item -LiteralPath $Destination -Recurse -Force
    }
    Copy-Item -LiteralPath $Source -Destination $Destination -Recurse -Force
}

function Assert-File([string] $Path, [string] $Label) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Missing required $Label file: $Path"
    }
}

function Assert-DirectoryPayload([string] $Path, [string] $Pattern, [string] $Label) {
    if (-not (Test-Path -LiteralPath $Path -PathType Container)) {
        throw "Missing required $Label directory: $Path"
    }
    $count = @(Get-ChildItem -LiteralPath $Path -Recurse -File -Filter $Pattern -ErrorAction SilentlyContinue).Count
    if ($count -lt 1) {
        throw "Required $Label directory contains no $Pattern payloads: $Path"
    }
    return $count
}

function Get-Sha256([string] $Path) {
    if (Test-Path -LiteralPath $Path -PathType Leaf) {
        return (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLowerInvariant()
    }
    return ''
}

Write-Step 'Preparing release stage'
New-Item -ItemType Directory -Force -Path $ReleaseRoot | Out-Null
if (Test-Path -LiteralPath $ReleaseStage) {
    Remove-Item -LiteralPath $ReleaseStage -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $ReleaseStage | Out-Null

Write-Step 'Building distinct runtime artifacts'
$buildArtifacts = Join-Path $PSScriptRoot 'build_runtime_artifacts.ps1'
if (-not (Test-Path -LiteralPath $buildArtifacts -PathType Leaf)) {
    throw "Missing runtime artifact builder: $buildArtifacts"
}
$artifactArgs = @('-ExecutionPolicy', 'Bypass', '-File', $buildArtifacts)
if ($SkipJarAudit) { $artifactArgs += '-SkipAudit' }
& powershell.exe -NoLogo -NoProfile @artifactArgs
if ($LASTEXITCODE -ne 0) { throw "Runtime artifact build failed with exit code $LASTEXITCODE" }

Write-Step 'Verifying client package authority'
$ClientPackage = Join-Path $RepoRoot 'PACKAGE_client'
$ServerPackage = Join-Path $RepoRoot 'PACKAGE_server'
$ClientJar = Join-Path $ClientPackage 'TheMechanist.jar'
$ServerJar = Join-Path $ServerPackage 'TheMechanistServer.jar'
Assert-File $ClientJar 'client jar'
Assert-File $ServerJar 'server jar'
Assert-File (Join-Path $ClientPackage 'assets\a\r\source\Title\TITEL.png') 'client title art'
Assert-File (Join-Path $ClientPackage 'assets\a\r\source\Background\Backdrop.png') 'client backdrop art'
$tileCount = Assert-DirectoryPayload (Join-Path $ClientPackage 'assets\a\r\tiles\quality\low_32\cells') '*.png' 'client low_32 tile art'
$soundCount = Assert-DirectoryPayload (Join-Path $ClientPackage 'assets\sound') '*.wav' 'client sound'

$clientJarInfo = Get-Item -LiteralPath $ClientJar
$serverJarInfo = Get-Item -LiteralPath $ServerJar
if ($clientJarInfo.Length -eq $serverJarInfo.Length) {
    throw 'Client and server jars still have identical byte size. Refusing release stage.'
}
$clientHash = Get-Sha256 $ClientJar
$serverHash = Get-Sha256 $ServerJar
if ($clientHash -eq $serverHash) {
    throw 'Client and server jars have identical SHA-256. Refusing release stage.'
}

Write-Step 'Copying self-contained client and server packages'
Copy-Tree $ClientPackage $ClientStage
Copy-Tree $ServerPackage $ServerStage

Write-Step 'Writing release manifest'
$manifest = [ordered]@{
    schema = 'mechanist.release_manifest.v1'
    generated_utc = (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ')
    version = $Version
    release_stage = $ReleaseStage
    client = [ordered]@{
        package = 'client'
        jar = 'client/TheMechanist.jar'
        jar_size_bytes = $clientJarInfo.Length
        jar_sha256 = $clientHash
        tile_png_count = $tileCount
        sound_wav_count = $soundCount
        launcher = 'client/MAIN launchers/RUN_THE_MECHANIST_WINDOWS.cmd'
    }
    server = [ordered]@{
        package = 'server'
        jar = 'server/TheMechanistServer.jar'
        jar_size_bytes = $serverJarInfo.Length
        jar_sha256 = $serverHash
        launcher = 'server/MAIN launchers/RUN_THE_MECHANIST_SERVER_WINDOWS.cmd'
    }
}
$manifest | ConvertTo-Json -Depth 8 | Set-Content -Path $ManifestPath -Encoding UTF8

if (-not $NoZip) {
    Write-Step 'Creating release zip archives'
    $clientZip = Join-Path $ReleaseRoot "TheMechanist-client-$SafeVersion-$Stamp.zip"
    $serverZip = Join-Path $ReleaseRoot "TheMechanist-server-$SafeVersion-$Stamp.zip"
    if (Test-Path -LiteralPath $clientZip) { Remove-Item -LiteralPath $clientZip -Force }
    if (Test-Path -LiteralPath $serverZip) { Remove-Item -LiteralPath $serverZip -Force }
    Compress-Archive -LiteralPath $ClientStage -DestinationPath $clientZip -Force
    Compress-Archive -LiteralPath $ServerStage -DestinationPath $serverZip -Force
    Write-Host "Client zip: $clientZip"
    Write-Host "Server zip: $serverZip"
}

Write-Step 'Release package staging complete'
Write-Host "Stage: $ReleaseStage"
Write-Host "Manifest: $ManifestPath"
