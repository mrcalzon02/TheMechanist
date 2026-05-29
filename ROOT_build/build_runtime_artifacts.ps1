param(
    [switch] $SkipAudit
)

$ErrorActionPreference = 'Stop'
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$BuildRoot = Join-Path $RepoRoot 'ROOT_BUILD\runtime_artifacts'
$ClientClasses = Join-Path $BuildRoot 'client_classes'
$ServerClasses = Join-Path $BuildRoot 'server_classes'
$ClientPackage = Join-Path $RepoRoot 'PACKAGE_client'
$ServerPackage = Join-Path $RepoRoot 'PACKAGE_server'
$ClientJar = Join-Path $ClientPackage 'TheMechanist.jar'
$ServerJar = Join-Path $ServerPackage 'TheMechanistServer.jar'
$ClientSourcesFile = Join-Path $BuildRoot 'client_sources.txt'
$ServerSourcesFile = Join-Path $BuildRoot 'server_sources.txt'

function Write-Step([string] $Message) {
    Write-Host "`n=== $Message ==="
}

function Require-Command([string] $Name) {
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if ($null -eq $cmd) {
        throw "Required command '$Name' was not found on PATH. Java 17+ JDK is required."
    }
    return $cmd.Source
}

function Reset-Dir([string] $Path) {
    if (Test-Path -LiteralPath $Path) {
        Remove-Item -LiteralPath $Path -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $Path | Out-Null
}

Write-Step 'Preparing runtime artifact build directories'
$javac = Require-Command 'javac.exe'
$jar = Require-Command 'jar.exe'
Reset-Dir $ClientClasses
Reset-Dir $ServerClasses
New-Item -ItemType Directory -Force -Path $ClientPackage | Out-Null
New-Item -ItemType Directory -Force -Path $ServerPackage | Out-Null

Write-Step 'Collecting client source files'
$clientSources = Get-ChildItem -LiteralPath (Join-Path $RepoRoot 'src') -Recurse -File -Filter '*.java' |
    Sort-Object FullName
if ($clientSources.Count -lt 1) {
    throw 'No Java sources found for client build.'
}
$clientSources | ForEach-Object { $_.FullName } | Set-Content -Path $ClientSourcesFile -Encoding UTF8
Write-Host "Client source files: $($clientSources.Count)"

Write-Step 'Compiling client classes'
& $javac -encoding UTF-8 -d $ClientClasses "@$ClientSourcesFile"
if ($LASTEXITCODE -ne 0) { throw "Client javac failed with exit code $LASTEXITCODE" }

Write-Step 'Writing client jar'
if (Test-Path -LiteralPath $ClientJar) {
    Copy-Item -LiteralPath $ClientJar -Destination ($ClientJar + '.bak') -Force
}
& $jar cfe $ClientJar mechanist.TheMechanist -C $ClientClasses .
if ($LASTEXITCODE -ne 0) { throw "Client jar failed with exit code $LASTEXITCODE" }

Write-Step 'Collecting server source files'
$serverSourcePaths = @(
    (Join-Path $RepoRoot 'src\mechanist\RuntimePathResolver.java'),
    (Join-Path $RepoRoot 'src\mechanist\TheMechanistServer.java')
)
foreach ($path in $serverSourcePaths) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required server source missing: $path"
    }
}
$serverSourcePaths | Set-Content -Path $ServerSourcesFile -Encoding UTF8
Write-Host "Server source files: $($serverSourcePaths.Count)"

Write-Step 'Compiling server classes'
& $javac -encoding UTF-8 -d $ServerClasses "@$ServerSourcesFile"
if ($LASTEXITCODE -ne 0) { throw "Server javac failed with exit code $LASTEXITCODE" }

Write-Step 'Writing server jar'
if (Test-Path -LiteralPath $ServerJar) {
    Copy-Item -LiteralPath $ServerJar -Destination ($ServerJar + '.bak') -Force
}
& $jar cfe $ServerJar mechanist.TheMechanistServer -C $ServerClasses .
if ($LASTEXITCODE -ne 0) { throw "Server jar failed with exit code $LASTEXITCODE" }

Write-Step 'Artifact summary'
$clientInfo = Get-Item -LiteralPath $ClientJar
$serverInfo = Get-Item -LiteralPath $ServerJar
Write-Host "Client jar: $($clientInfo.FullName) size=$($clientInfo.Length)"
Write-Host "Server jar: $($serverInfo.FullName) size=$($serverInfo.Length)"
if ($clientInfo.Length -eq $serverInfo.Length) {
    throw 'Client and server jars have identical size after distinct build; refusing artifact split.'
}

if (-not $SkipAudit) {
    Write-Step 'Auditing runtime jars'
    $auditor = Join-Path $RepoRoot 'ROOT_tools\jar_artifact_auditor.py'
    if (Test-Path -LiteralPath $auditor -PathType Leaf) {
        $python = Get-Command python -ErrorAction SilentlyContinue
        if ($null -eq $python) { $python = Get-Command py -ErrorAction SilentlyContinue }
        if ($null -ne $python) {
            & $python.Source $auditor
            if ($LASTEXITCODE -ne 0) {
                throw "Jar artifact auditor failed with exit code $LASTEXITCODE"
            }
        } else {
            Write-Warning 'Python not found; skipping jar_artifact_auditor.py.'
        }
    }
}

Write-Step 'Runtime artifact build complete'
