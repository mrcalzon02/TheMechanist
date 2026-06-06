param(
    [string]$Version = "local",
    [switch]$IncludeAssets
)

$ErrorActionPreference = "Stop"

$toolDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = [System.IO.Path]::GetFullPath((Join-Path $toolDir "..\.."))
$buildRoot = Join-Path $root "build\local-package-seed"
$classesRoot = Join-Path $root "build\local-package-seed-classes"
$clientClasses = Join-Path $classesRoot "client"
$launcherClasses = Join-Path $classesRoot "launcher"
$manifestDir = Join-Path $buildRoot "manifests"
$clientDir = Join-Path $buildRoot "packages\client"
$serverDir = Join-Path $buildRoot "packages\server"
$supportDir = Join-Path $buildRoot "packages\support\lib"

function Reset-Directory {
    param([string]$Path)
    $full = [System.IO.Path]::GetFullPath($Path)
    if (-not $full.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to reset outside repository: $Path"
    }
    if (Test-Path -LiteralPath $full) { Remove-Item -LiteralPath $full -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $full | Out-Null
}

function Write-ArgFile {
    param([string]$Path, [System.IO.FileInfo[]]$Sources)
    $lines = $Sources | ForEach-Object { '"' + ($_.FullName -replace '\\','/') + '"' }
    Set-Content -LiteralPath $Path -Value $lines -Encoding ASCII
}

function Get-Sha256 {
    param([string]$Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Get-Platform {
    $os = [System.Runtime.InteropServices.RuntimeInformation]::OSDescription.ToLowerInvariant()
    $arch = [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture.ToString().ToLowerInvariant()
    $cpu = if ($arch -match "64|x64|x86_64") { "x64" } else { $arch -replace "[^a-z0-9]+", "" }
    if ($os.Contains("windows")) { return "windows-$cpu" }
    if ($os.Contains("linux")) { return "linux-$cpu" }
    if ($os.Contains("darwin") -or $os.Contains("mac")) { return "macos-$cpu" }
    return (($os -replace "[^a-z0-9]+", "") + "-" + $cpu)
}

function New-JarFromDirectory {
    param([string]$JarPath, [string]$ClassDir, [string]$MainClass)
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    if (Test-Path -LiteralPath $JarPath) { Remove-Item -LiteralPath $JarPath -Force }
    $archive = [System.IO.Compression.ZipFile]::Open($JarPath, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        $manifest = $archive.CreateEntry("META-INF/MANIFEST.MF")
        $writer = New-Object System.IO.StreamWriter($manifest.Open(), [System.Text.Encoding]::UTF8)
        try {
            $writer.WriteLine("Manifest-Version: 1.0")
            $writer.WriteLine("Main-Class: $MainClass")
            $writer.WriteLine("")
        } finally {
            $writer.Dispose()
        }

        $base = [System.IO.Path]::GetFullPath($ClassDir)
        foreach ($file in Get-ChildItem -LiteralPath $base -Recurse -File) {
            $relative = [System.IO.Path]::GetFullPath($file.FullName).Substring($base.Length).TrimStart('\','/') -replace '\\','/'
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $file.FullName, $relative) | Out-Null
        }
    } finally {
        $archive.Dispose()
    }
}

Reset-Directory $buildRoot
Reset-Directory $classesRoot
New-Item -ItemType Directory -Force -Path $clientClasses,$launcherClasses,$manifestDir,$clientDir,$serverDir,$supportDir | Out-Null

$clientArgFile = Join-Path $classesRoot "client-sources.arg"
$launcherArgFile = Join-Path $classesRoot "launcher-sources.arg"
Write-ArgFile $clientArgFile @(Get-ChildItem -Recurse -File (Join-Path $root "src") -Filter *.java)
Write-ArgFile $launcherArgFile @(Get-ChildItem -Recurse -File (Join-Path $root "PACKAGE_launcher\java\src\main\java") -Filter *.java)

javac -encoding UTF-8 --release 17 -d $clientClasses "@$clientArgFile"
javac -encoding UTF-8 --release 17 -d $launcherClasses "@$launcherArgFile"

$clientJar = Join-Path $clientDir "TheMechanist.jar"
$serverJar = Join-Path $serverDir "TheMechanistServer.jar"
$launcherJar = Join-Path $buildRoot "packages\launcher\MechanistLauncher.jar"
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $launcherJar) | Out-Null

New-JarFromDirectory $clientJar $clientClasses "mechanist.TheMechanist"
New-JarFromDirectory $serverJar $clientClasses "mechanist.MechanistServerMain"
New-JarFromDirectory $launcherJar $launcherClasses "mechanist.launcher.MechanistLauncherApp"

$supportMarker = Join-Path $supportDir "support-package-marker.jar"
New-JarFromDirectory $supportMarker $launcherClasses "mechanist.launcher.MechanistLauncherApp"

if ($IncludeAssets) {
    $assetSource = Join-Path $root "PACKAGE_client\assets"
    $assetTarget = Join-Path $clientDir "assets"
    if (Test-Path -LiteralPath $assetSource) { Copy-Item -LiteralPath $assetSource -Destination $assetTarget -Recurse -Force }
}

$platform = Get-Platform
$manifestPath = Join-Path $manifestDir "$platform-runtime-manifest.json"
$clientSize = (Get-Item -LiteralPath $clientJar).Length
$serverSize = (Get-Item -LiteralPath $serverJar).Length
$supportSize = (Get-Item -LiteralPath $supportMarker).Length
$json = @"
{
  "schema": 2,
  "distribution_model": "installer-thin-launcher-client-server",
  "version": "$Version",
  "platform": "$platform",
  "client": { "path": "packages/client/TheMechanist.jar", "sha256": "$(Get-Sha256 $clientJar)", "size": $clientSize },
  "server": { "path": "packages/server/TheMechanistServer.jar", "sha256": "$(Get-Sha256 $serverJar)", "size": $serverSize },
  "support_libraries": [
    { "path": "packages/support/lib/support-package-marker.jar", "sha256": "$(Get-Sha256 $supportMarker)", "size": $supportSize }
  ]
}
"@
Set-Content -LiteralPath $manifestPath -Value $json -Encoding UTF8

& (Join-Path $root "ROOT_tools\build\verify_java17_classfiles.ps1") $clientJar $serverJar $launcherJar $supportMarker

Write-Host "Staged local package seed at $buildRoot"
Write-Host "Runtime manifest: $manifestPath"
