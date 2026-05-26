[CmdletBinding()]
param(
    [string]$AppName = "The Mechanist Launcher",
    [string]$AppVersion = "0.9.10ix",
    [string]$Vendor = "The Mechanist Project",
    [string[]]$PackageTypes = @("app-image", "exe", "msi"),
    [bool]$PerUserInstall = $true,
    [switch]$UseExistingJar,
    [switch]$RequireNativeInstallers,
    [string]$InstallDirName = "TheMechanistLauncher",
    [string]$UpgradeUuid = "b7c10db2-4a4b-4bb2-9c13-93b6fb4d70df"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$TargetDir = Join-Path $ProjectRoot "target"
$DistDir = Join-Path $ProjectRoot "dist\installers\windows"
$RuntimeDir = Join-Path $ProjectRoot "build\jlink\runtime-launcher-windows"
$InputDir = Join-Path $ProjectRoot "build\package-input\windows-launcher"
$AppImageDest = Join-Path $DistDir "app-image"
$IconPath = Join-Path $ProjectRoot "assets\app\icons\the-mechanist.ico"
$ModuleFile = Join-Path $ProjectRoot "packaging\jlink\client-modules.txt"
$WindowsReadme = Join-Path $DistDir "WINDOWS_INSTALLERS_README.txt"
$DependencyStageDir = Join-Path $TargetDir "package-runtime-deps"
$ThinLauncherManifestDir = Join-Path $InputDir "manifests"
$PackageCacheDir = Join-Path $InputDir "packages"
$SupportLibDir = Join-Path $PackageCacheDir "support\lib"
$ClientPackageDir = Join-Path $PackageCacheDir "client"
$ServerPackageDir = Join-Path $PackageCacheDir "server"
$LauncherPackageDir = Join-Path $PackageCacheDir "launcher"
$LauncherProfileSourceDir = Join-Path $ProjectRoot "launcher\profile-packages"
$LwjglVersion = "3.4.1"
$RequiredLwjglFiles = @(
    "lwjgl-$LwjglVersion.jar",
    "lwjgl-glfw-$LwjglVersion.jar",
    "lwjgl-opengl-$LwjglVersion.jar",
    "lwjgl-stb-$LwjglVersion.jar",
    "lwjgl-$LwjglVersion-natives-windows.jar",
    "lwjgl-glfw-$LwjglVersion-natives-windows.jar",
    "lwjgl-opengl-$LwjglVersion-natives-windows.jar",
    "lwjgl-stb-$LwjglVersion-natives-windows.jar"
)

function Require-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) { throw "Required command '$Name' was not found on PATH." }
}
function Test-Command([string]$Name) { return [bool](Get-Command $Name -ErrorAction SilentlyContinue) }
function Normalize-PackageType([string]$Type) {
    $t = $Type.Trim().ToLowerInvariant()
    switch ($t) {
        "image" { return "app-image" }
        "appimage" { return "app-image" }
        "portable" { return "app-image" }
        "app-image" { return "app-image" }
        "exe" { return "exe" }
        "msi" { return "msi" }
        default { throw "Unsupported Windows package type '$Type'. Use app-image, exe, or msi." }
    }
}
function Escape-Readme([string]$Value) { if ([string]::IsNullOrWhiteSpace($Value)) { return "(not produced)" }; return $Value }
function Get-RelativePathPortable([string]$BasePath, [string]$ChildPath) {
    $baseFull = [System.IO.Path]::GetFullPath($BasePath)
    $childFull = [System.IO.Path]::GetFullPath($ChildPath)
    if (-not $baseFull.EndsWith([System.IO.Path]::DirectorySeparatorChar)) { $baseFull += [System.IO.Path]::DirectorySeparatorChar }
    $baseUri = New-Object System.Uri($baseFull)
    $childUri = New-Object System.Uri($childFull)
    return [System.Uri]::UnescapeDataString($baseUri.MakeRelativeUri($childUri).ToString()).Replace("/", [System.IO.Path]::DirectorySeparatorChar)
}
function Test-ValidJar([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { return $false }
    if ((Get-Item -LiteralPath $Path).Length -lt 128) { return $false }
    $fs = [System.IO.File]::OpenRead($Path)
    try {
        $bytes = New-Object byte[] 4
        $read = $fs.Read($bytes, 0, 4)
        return ($read -eq 4 -and $bytes[0] -eq 0x50 -and $bytes[1] -eq 0x4B -and $bytes[2] -eq 0x03 -and $bytes[3] -eq 0x04)
    } finally { if ($null -ne $fs) { $fs.Dispose() } }
}
function Get-Sha256([string]$Path) { return (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLowerInvariant() }

function Stage-RuntimeDependencies {
    Require-Command mvn
    Remove-Item -LiteralPath $DependencyStageDir -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $DependencyStageDir, $SupportLibDir | Out-Null
    mvn -B -DincludeScope=runtime -Dmdep.copyPom=false "-DoutputDirectory=$DependencyStageDir" dependency:copy-dependencies
    Copy-Item -LiteralPath (Join-Path $DependencyStageDir "*") -Destination $SupportLibDir -Recurse -Force
    $missing = New-Object System.Collections.Generic.List[string]
    foreach ($required in $RequiredLwjglFiles) {
        $hits = @(Get-ChildItem -LiteralPath $SupportLibDir -Recurse -File -Filter $required -ErrorAction SilentlyContinue)
        $valid = $false
        foreach ($hit in $hits) { if (Test-ValidJar $hit.FullName) { $valid = $true; break } }
        if (-not $valid) { $missing.Add($required) | Out-Null }
    }
    if ($missing.Count -gt 0) { throw "Packaged Windows LWJGL runtime is incomplete. Missing or invalid: $($missing -join ', ')" }
    Write-Host "Launcher-managed Windows support libraries staged in $SupportLibDir"
}

function Stage-LauncherProfilePackages {
    if (-not (Test-Path -LiteralPath $LauncherProfileSourceDir -PathType Container)) {
        throw "Missing launcher profile package source directory: $LauncherProfileSourceDir"
    }
    $dest = Join-Path $LauncherPackageDir "profile-packages"
    Remove-Item -LiteralPath $dest -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $LauncherPackageDir | Out-Null
    Copy-Item -LiteralPath $LauncherProfileSourceDir -Destination $dest -Recurse -Force
    Write-Host "Launcher profile packages staged in $dest"
}

function Write-ThinLauncherManifest {
    param([string]$ClientJar, [string]$ServerJar)
    New-Item -ItemType Directory -Force -Path $ThinLauncherManifestDir | Out-Null
    $clientName = Split-Path -Leaf $ClientJar
    $serverName = Split-Path -Leaf $ServerJar
    $supportEntries = @(Get-ChildItem -LiteralPath $SupportLibDir -Recurse -File -Filter '*.jar' | Sort-Object FullName | ForEach-Object {
        $relative = (Get-RelativePathPortable $PackageCacheDir $_.FullName).Replace("\", "/")
        "      {`"path`": `"$relative`", `"sha256`": `"$(Get-Sha256 $_.FullName)`", `"size`": $($_.Length)}"
    })
    $supportJson = [string]::Join(",`n", $supportEntries)
    @"
{
  "schema": 1,
  "distribution_model": "installer-thin-launcher-client-server",
  "version": "$AppVersion",
  "platform": "windows-x64",
  "launcher": {
    "role": "installed-orchestrator",
    "main_class": "mechanist.launcher.ThinLauncherMain",
    "profile_packages": "packages/launcher/profile-packages",
    "owns": ["wrapper-detection", "fallback-profile-generation", "manifest-verification", "package-acquisition", "update", "rollback", "launch"]
  },
  "client": {
    "path": "packages/client/$clientName",
    "sha256": "$(Get-Sha256 $ClientJar)",
    "size": $((Get-Item -LiteralPath $ClientJar).Length),
    "main_class": "mechanist.TheMechanist"
  },
  "server": {
    "path": "packages/server/$serverName",
    "sha256": "$(Get-Sha256 $ServerJar)",
    "size": $((Get-Item -LiteralPath $ServerJar).Length),
    "main_class": "mechanist.MechanistServerMain"
  },
  "launcher_profile": {
    "fallback_human_portraits": "launcher-human-8x8-v1",
    "celebrity_portraits": "launcher-celebrity-portraits-v1",
    "celebrity_name_detection": "launcher-celebrity-name-detection-v1",
    "wrapper_detection": ["steam", "gog", "none"]
  },
  "support_libraries": [
$supportJson
  ]
}
"@ | Set-Content -LiteralPath (Join-Path $ThinLauncherManifestDir "windows-runtime-manifest.json") -Encoding utf8
}

Require-Command java; Require-Command javac; Require-Command jlink; Require-Command jpackage; Require-Command mvn
if (-not $env:JAVA_HOME) { throw "JAVA_HOME is not set. Set JAVA_HOME to a Java 17 JDK before packaging." }
$Jmods = Join-Path $env:JAVA_HOME "jmods"
if (-not (Test-Path -LiteralPath $Jmods)) { throw "JAVA_HOME does not point to a full JDK with jmods: $env:JAVA_HOME" }
if (-not (Test-Path -LiteralPath $ModuleFile)) { throw "Missing jlink module list: $ModuleFile" }
$ClientModules = (Get-Content -LiteralPath $ModuleFile -Raw).Replace("`r", "").Replace("`n", "").Replace(" ", "")
if ([string]::IsNullOrWhiteSpace($ClientModules)) { throw "Client module list was empty: $ModuleFile" }

$normalizedTypes = New-Object System.Collections.Generic.List[string]
foreach ($type in $PackageTypes) { $normal = Normalize-PackageType $type; if (-not $normalizedTypes.Contains($normal)) { $normalizedTypes.Add($normal) | Out-Null } }
$WixRequired = $normalizedTypes.Contains("exe") -or $normalizedTypes.Contains("msi")
$WixAvailable = $true
foreach ($tool in @("candle.exe", "light.exe")) { if (-not (Test-Command $tool)) { $WixAvailable = $false; Write-Warning "WiX Toolset executable '$tool' was not found on PATH. jpackage EXE/MSI generation requires WiX 3.x on Windows." } }
if ($WixRequired -and -not $WixAvailable -and $RequireNativeInstallers) { throw "WiX Toolset 3.x is required for EXE/MSI generation and -RequireNativeInstallers was set. Install WiX and retry." }

Set-Location $ProjectRoot
Remove-Item -LiteralPath $DistDir, $RuntimeDir, $InputDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $DistDir, $InputDir, $AppImageDest, $ClientPackageDir, $ServerPackageDir, $SupportLibDir, $LauncherPackageDir | Out-Null
if (-not $UseExistingJar) { & (Join-Path $ProjectRoot "scripts\security\generate-sensitive-strings.ps1"); mvn -B -DskipTests package }

$ClientJarSource = Join-Path $TargetDir "TheMechanist-all.jar"
$ServerJarSource = Join-Path $TargetDir "TheMechanistServer-all.jar"
$FallbackClientJar = Join-Path $ProjectRoot "TheMechanist.jar"
$FallbackServerJar = Join-Path $ProjectRoot "TheMechanistServer.jar"
if (-not (Test-Path -LiteralPath $ClientJarSource) -and $UseExistingJar -and (Test-Path -LiteralPath $FallbackClientJar)) { $ClientJarSource = $FallbackClientJar }
if (-not (Test-Path -LiteralPath $ServerJarSource) -and $UseExistingJar -and (Test-Path -LiteralPath $FallbackServerJar)) { $ServerJarSource = $FallbackServerJar }
if (-not (Test-Path -LiteralPath $ClientJarSource)) { throw "No packageable client jar was found. Expected $ClientJarSource, or use -UseExistingJar with $FallbackClientJar present." }
if (-not (Test-Path -LiteralPath $ServerJarSource)) { throw "No packageable server jar was found. Expected $ServerJarSource, or use -UseExistingJar with $FallbackServerJar present." }

$ClientJar = Join-Path $ClientPackageDir "TheMechanist.jar"
$ServerJar = Join-Path $ServerPackageDir "TheMechanistServer.jar"
Copy-Item -LiteralPath $ClientJarSource -Destination $ClientJar -Force
Copy-Item -LiteralPath $ServerJarSource -Destination $ServerJar -Force
Stage-RuntimeDependencies
Stage-LauncherProfilePackages
Write-ThinLauncherManifest -ClientJar $ClientJar -ServerJar $ServerJar
if (Test-Path -LiteralPath $IconPath) { Copy-Item -LiteralPath $IconPath -Destination (Join-Path $InputDir "the-mechanist.ico") -Force } else { Write-Warning "Windows icon was not found at $IconPath. Installer will be produced without a custom icon." }

& jlink --module-path $Jmods --add-modules $ClientModules --output $RuntimeDir --strip-debug --no-man-pages --no-header-files --strip-native-commands --compress=2

function Invoke-JPackage([string]$PackageType, [string]$Destination) {
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    $args = @("--type", $PackageType, "--name", $AppName, "--app-version", $AppVersion, "--vendor", $Vendor, "--description", "The Mechanist thin launcher and package orchestrator", "--runtime-image", $RuntimeDir, "--input", $InputDir, "--main-jar", "packages/client/TheMechanist.jar", "--main-class", "mechanist.launcher.ThinLauncherMain", "--dest", $Destination, "--resource-dir", (Join-Path $ProjectRoot "packaging\windows"))
    if (Test-Path -LiteralPath $IconPath) { $args += @("--icon", $IconPath) }
    if ($PackageType -eq "exe" -or $PackageType -eq "msi") {
        $args += @("--install-dir", $InstallDirName, "--win-menu", "--win-menu-group", "The Mechanist", "--win-shortcut", "--win-shortcut-prompt", "--win-dir-chooser", "--win-upgrade-uuid", $UpgradeUuid)
        if ($PerUserInstall) { $args += @("--win-per-user-install") }
    }
    Write-Host "Running jpackage --type $PackageType ..."
    & jpackage @args
}

$Produced = New-Object System.Collections.Generic.List[string]
foreach ($type in $normalizedTypes) {
    if ($type -eq "app-image") {
        Invoke-JPackage "app-image" $AppImageDest
        $imageRoot = Join-Path $AppImageDest $AppName
        if (-not (Test-Path -LiteralPath $imageRoot)) { throw "jpackage app-image did not produce the expected application image folder: $imageRoot" }
        $portableZip = Join-Path $DistDir ("TheMechanist_launcher_windows_portable_{0}.zip" -f $AppVersion)
        Remove-Item -LiteralPath $portableZip -Force -ErrorAction SilentlyContinue
        Compress-Archive -LiteralPath $imageRoot -DestinationPath $portableZip -Force
        $Produced.Add($portableZip) | Out-Null
        $exePath = Join-Path $imageRoot ("{0}.exe" -f $AppName)
        if (Test-Path -LiteralPath $exePath) { $Produced.Add($exePath) | Out-Null }
    } elseif ($type -eq "exe" -or $type -eq "msi") {
        if (-not $WixAvailable) { Write-Warning "Skipping $type installer because WiX Toolset 3.x was not found. Portable app-image output remains usable for testing."; continue }
        Invoke-JPackage $type $DistDir
        Get-ChildItem -LiteralPath $DistDir -File -Filter "*.$type" | ForEach-Object { $Produced.Add($_.FullName) | Out-Null }
    }
}

Get-ChildItem -LiteralPath $DistDir -File -Recurse | Where-Object { $_.Name -ne "SHA256SUMS.txt" } | Sort-Object FullName | Get-FileHash -Algorithm SHA256 | ForEach-Object { $relative = (Get-RelativePathPortable $DistDir $_.Path).Replace("\", "/"); "$($_.Hash.ToLowerInvariant())  $relative" } | Set-Content -LiteralPath (Join-Path $DistDir "SHA256SUMS.txt") -Encoding ascii
$portableLine = Escape-Readme ((Get-ChildItem -LiteralPath $DistDir -File -Filter "TheMechanist_launcher_windows_portable_*.zip" -ErrorAction SilentlyContinue | Select-Object -First 1).FullName)
$exeLine = Escape-Readme ((Get-ChildItem -LiteralPath $DistDir -File -Filter "*.exe" -ErrorAction SilentlyContinue | Select-Object -First 1).FullName)
$msiLine = Escape-Readme ((Get-ChildItem -LiteralPath $DistDir -File -Filter "*.msi" -ErrorAction SilentlyContinue | Select-Object -First 1).FullName)
@"
The Mechanist Windows installer outputs
======================================

Version: $AppVersion
Distribution model: installer -> thin launcher -> client -> server
Launcher entrypoint: mechanist.launcher.ThinLauncherMain
Package identity manifest: manifests/windows-runtime-manifest.json
Launcher-managed packages: packages/client, packages/server, packages/support/lib, packages/launcher/profile-packages
Wrapper detection: Steam/GOG/none, evaluated by thin launcher before client start
Fallback profile generation: launcher-owned, hash-based
Launcher portrait/name packages: human 8x8, celebrity portrait manifest, celebrity name detection manifest
LWJGL/support libraries: staged into packages/support/lib at package-build time
Game-launch dependency downloads: forbidden

Outputs:
- Portable launcher app-image zip: $portableLine
- EXE installer: $exeLine
- MSI installer: $msiLine
"@ | Set-Content -LiteralPath $WindowsReadme -Encoding utf8
