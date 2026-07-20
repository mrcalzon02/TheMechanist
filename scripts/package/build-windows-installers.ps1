[CmdletBinding()]
param(
    [string]$DistributionRoot = $env:MECHANIST_DISTRIBUTION_ROOT,
    [string[]]$PackageTypes = @("app-image", "exe", "msi"),
    [string]$OutputDir,
    [string]$AppName = "The Mechanist",
    [string]$RemoteLauncherName = "The Mechanist Remote Lobby",
    [string]$Vendor = "The Mechanist Project",
    [bool]$PerUserInstall = $true,
    [switch]$RequireNativeInstallers,
    [string]$InstallDirName = "TheMechanist",
    [string]$UpgradeUuid = "b7c10db2-4a4b-4bb2-9c13-93b6fb4d70df"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $ProjectRoot "dist\installers\windows"
}

function Require-Command([string]$Name) {
    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $command) { throw "Required command '$Name' was not found on PATH." }
    return $command.Source
}

function Invoke-Checked {
    param(
        [string]$Command,
        [string[]]$Arguments
    )
    Write-Host ("+ " + $Command + " " + ($Arguments -join " "))
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code $LASTEXITCODE`: $Command"
    }
}

function Normalize-PackageType([string]$Type) {
    switch ($Type.Trim().ToLowerInvariant()) {
        "image" { return "app-image" }
        "appimage" { return "app-image" }
        "portable" { return "app-image" }
        "app-image" { return "app-image" }
        "exe" { return "exe" }
        "msi" { return "msi" }
        default { throw "Unsupported Windows package type '$Type'. Use app-image, exe, or msi." }
    }
}

function Get-RelativePathPortable([string]$BasePath, [string]$ChildPath) {
    $baseFull = [System.IO.Path]::GetFullPath($BasePath)
    $childFull = [System.IO.Path]::GetFullPath($ChildPath)
    if (-not $baseFull.EndsWith([System.IO.Path]::DirectorySeparatorChar)) {
        $baseFull += [System.IO.Path]::DirectorySeparatorChar
    }
    $baseUri = [System.Uri]::new($baseFull)
    $childUri = [System.Uri]::new($childFull)
    return [System.Uri]::UnescapeDataString(
        $baseUri.MakeRelativeUri($childUri).ToString()
    ).Replace("/", [System.IO.Path]::DirectorySeparatorChar)
}

$Python = Require-Command "python"
$JPackage = Require-Command "jpackage"
$Builder = Join-Path $ProjectRoot "ROOT_build\ci\build_runnable_distribution.py"
$Verifier = Join-Path $ProjectRoot "ROOT_build\ci\verify_runnable_distribution.py"
$Stager = Join-Path $ProjectRoot "ROOT_build\ci\stage_native_installer_payload.py"
$ImageVerifier = Join-Path $ProjectRoot "ROOT_build\ci\verify_native_installer_image.py"

foreach ($required in @($Builder, $Verifier, $Stager, $ImageVerifier)) {
    if (-not (Test-Path -LiteralPath $required -PathType Leaf)) {
        throw "Required release tool is missing: $required"
    }
}

if ([string]::IsNullOrWhiteSpace($DistributionRoot)) {
    Require-Command "mvn" | Out-Null
    Require-Command "java" | Out-Null
    $buildOutput = Join-Path $ProjectRoot "dist\releases-native"
    Invoke-Checked $Python @(
        $Builder,
        "--repo", $ProjectRoot,
        "--release-hardened",
        "--output", $buildOutput
    )
    $candidate = Get-ChildItem -LiteralPath $buildOutput -Directory -Filter "TheMechanist-*-windows-*" |
        Sort-Object Name |
        Select-Object -Last 1
    if (-not $candidate) {
        throw "The canonical Windows distribution builder produced no windows-x64 directory."
    }
    $DistributionRoot = $candidate.FullName
}

$DistributionRoot = (Resolve-Path -LiteralPath $DistributionRoot).Path
$ManifestPath = Join-Path $DistributionRoot "manifests\runtime-manifest.json"
if (-not (Test-Path -LiteralPath $ManifestPath -PathType Leaf)) {
    throw "Canonical runtime manifest is missing: $ManifestPath"
}
$Identity = Get-Content -LiteralPath $ManifestPath -Raw -Encoding UTF8 | ConvertFrom-Json
if ($Identity.platform -ne "windows-x64") {
    throw "Windows native packaging requires windows-x64, found '$($Identity.platform)'."
}
if ($Identity.releaseHardened -ne $true) {
    throw "Windows native packaging requires a release-hardened canonical distribution."
}
if ($Identity.remoteClientEntryPoint -ne "mechanist.RemoteClientMain") {
    throw "Canonical distribution does not declare the governed remote-client entry."
}
$Version = [string]$Identity.version
$versionMatch = [regex]::Match($Version, "\d+(?:\.\d+){0,2}")
$NativeVersion = if ($versionMatch.Success) { $versionMatch.Value } else { "0.0.0" }
$Commit = [string]$Identity.commit

Remove-Item -LiteralPath $OutputDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$PayloadDir = Join-Path $ProjectRoot "build\native-installer\windows\payload"
Invoke-Checked $Python @(
    $Verifier,
    $DistributionRoot,
    "--require-release-hardened",
    "--report", (Join-Path $OutputDir "source-verification-windows-x64.json")
)
Invoke-Checked $Python @(
    $Stager,
    $DistributionRoot,
    "--output", $PayloadDir,
    "--expected-platform", "windows-x64",
    "--report", (Join-Path $OutputDir "staging-windows-x64.json")
)

$RuntimeImage = Join-Path $DistributionRoot "runtime"
$RuntimeJava = Join-Path $RuntimeImage "bin\java.exe"
if (-not (Test-Path -LiteralPath $RuntimeJava -PathType Leaf)) {
    throw "Canonical Windows runtime image is incomplete: $RuntimeImage"
}

$IconPath = $null
foreach ($candidate in @(
    (Join-Path $ProjectRoot "PACKAGE_client\assets\app\icons\the-mechanist.ico"),
    (Join-Path $ProjectRoot "client\assets\app\icons\the-mechanist.ico")
)) {
    if (Test-Path -LiteralPath $candidate -PathType Leaf) {
        $IconPath = $candidate
        break
    }
}

$ResourceDir = $null
foreach ($candidate in @(
    (Join-Path $ProjectRoot "PACKAGE_installer\windows\resources"),
    (Join-Path $ProjectRoot "packaging\windows")
)) {
    if (Test-Path -LiteralPath $candidate -PathType Container) {
        $ResourceDir = $candidate
        break
    }
}

$LauncherConfigDir = Join-Path $ProjectRoot "build\native-installer\windows\launchers"
Remove-Item -LiteralPath $LauncherConfigDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $LauncherConfigDir | Out-Null
$RemoteLauncherConfig = Join-Path $LauncherConfigDir "remote-client.properties"
@(
    "main-jar=packages/client/TheMechanist.jar",
    "main-class=mechanist.RemoteClientMain",
    "app-version=$NativeVersion",
    "win-console=false"
) | Set-Content -LiteralPath $RemoteLauncherConfig -Encoding ascii

$NormalizedTypes = New-Object System.Collections.Generic.List[string]
foreach ($type in $PackageTypes) {
    $normalized = Normalize-PackageType $type
    if (-not $NormalizedTypes.Contains($normalized)) {
        $NormalizedTypes.Add($normalized) | Out-Null
    }
}
if ($NormalizedTypes.Count -eq 0) { $NormalizedTypes.Add("app-image") | Out-Null }

$WixRequired = $NormalizedTypes.Contains("exe") -or $NormalizedTypes.Contains("msi")
$WixAvailable = $true
foreach ($tool in @("candle.exe", "light.exe")) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        $WixAvailable = $false
        Write-Warning "WiX Toolset executable '$tool' was not found. jpackage EXE/MSI generation requires WiX 3.x."
    }
}
if ($WixRequired -and -not $WixAvailable -and $RequireNativeInstallers) {
    throw "WiX Toolset 3.x is required because -RequireNativeInstallers was set."
}

$CommonArgs = @(
    "--name", $AppName,
    "--app-version", $NativeVersion,
    "--vendor", $Vendor,
    "--description", "The Mechanist limited-alpha launcher and verified package runtime",
    "--runtime-image", $RuntimeImage,
    "--input", $PayloadDir,
    "--main-jar", "launcher/MechanistLauncher.jar",
    "--main-class", "mechanist.launcher.MechanistLauncherApp",
    "--add-launcher", "$RemoteLauncherName=$RemoteLauncherConfig"
)
if ($IconPath) { $CommonArgs += @("--icon", $IconPath) }
if ($ResourceDir) { $CommonArgs += @("--resource-dir", $ResourceDir) }

function Invoke-JPackage {
    param(
        [string]$Type,
        [string]$Destination
    )
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    $arguments = @("--type", $Type) + $CommonArgs + @("--dest", $Destination)
    if ($Type -in @("exe", "msi")) {
        $arguments += @(
            "--install-dir", $InstallDirName,
            "--win-menu",
            "--win-menu-group", "The Mechanist",
            "--win-shortcut",
            "--win-shortcut-prompt",
            "--win-dir-chooser",
            "--win-upgrade-uuid", $UpgradeUuid
        )
        if ($PerUserInstall) { $arguments += "--win-per-user-install" }
    }
    Invoke-Checked $JPackage $arguments
}

$AppImageDest = Join-Path $OutputDir "app-image"
$AppImageRoot = Join-Path $AppImageDest $AppName
$AppImageBuilt = $false

function Build-AppImage {
    Remove-Item -LiteralPath $AppImageDest -Recurse -Force -ErrorAction SilentlyContinue
    Invoke-JPackage "app-image" $AppImageDest
    if (-not (Test-Path -LiteralPath $AppImageRoot -PathType Container)) {
        throw "jpackage did not produce expected app image: $AppImageRoot"
    }
    $MainExecutable = Join-Path $AppImageRoot "$AppName.exe"
    $RemoteExecutable = Join-Path $AppImageRoot "$RemoteLauncherName.exe"
    if (-not (Test-Path -LiteralPath $MainExecutable -PathType Leaf)) {
        throw "Native main launcher is missing: $MainExecutable"
    }
    if (-not (Test-Path -LiteralPath $RemoteExecutable -PathType Leaf)) {
        throw "Native remote-lobby launcher is missing: $RemoteExecutable"
    }
    Invoke-Checked $Python @(
        $ImageVerifier,
        $AppImageRoot,
        "--expected-platform", "windows-x64",
        "--report", (Join-Path $OutputDir "native-image-verification-windows-x64.json")
    )
    $PortableZip = Join-Path $OutputDir "TheMechanist-$Version-windows-x64-native-app-image.zip"
    Remove-Item -LiteralPath $PortableZip -Force -ErrorAction SilentlyContinue
    Compress-Archive -LiteralPath $AppImageRoot -DestinationPath $PortableZip -CompressionLevel Optimal
    $script:AppImageBuilt = $true
}

foreach ($type in $NormalizedTypes) {
    switch ($type) {
        "app-image" { Build-AppImage }
        "exe" {
            if ($WixAvailable) { Invoke-JPackage "exe" $OutputDir }
            else { Write-Warning "EXE packaging skipped because WiX is unavailable." }
        }
        "msi" {
            if ($WixAvailable) { Invoke-JPackage "msi" $OutputDir }
            else { Write-Warning "MSI packaging skipped because WiX is unavailable." }
        }
    }
}
if (-not $AppImageBuilt) { Build-AppImage }

Get-ChildItem -LiteralPath $OutputDir -File -Recurse |
    Where-Object { $_.Name -ne "SHA256SUMS.txt" } |
    Sort-Object FullName |
    Get-FileHash -Algorithm SHA256 |
    ForEach-Object {
        $relative = (Get-RelativePathPortable $OutputDir $_.Path).Replace("\", "/")
        "$($_.Hash.ToLowerInvariant())  $relative"
    } |
    Set-Content -LiteralPath (Join-Path $OutputDir "SHA256SUMS.txt") -Encoding ascii

@"
The Mechanist limited-alpha Windows outputs
============================================

Version: $Version
Native package version: $NativeVersion
Commit: $Commit
Platform: windows-x64
Release hardened: true
Canonical source distribution: $DistributionRoot
Distribution model: installer -> thin launcher -> client -> server
Native launchers: $AppName.exe; $RemoteLauncherName.exe

Every native output in this directory was composed from the verified canonical
release staging tree. The native app image was reopened and checked against the
launcher manifest after jpackage completed. Mutable saves, profiles, settings,
logs, cache, mods, exports, and resume-token custody remain outside the installed
application payload.

Recommended validation order:
1. Verify SHA256SUMS.txt.
2. Extract the native app-image ZIP and start The Mechanist.exe.
3. Confirm launcher package verification succeeds.
4. Start The Mechanist Remote Lobby.exe and verify the lobby-only boundary.
5. Run a single-player save/resume test.
6. Run the independent host bind and two-client connection tests.
"@ | Set-Content -LiteralPath (Join-Path $OutputDir "WINDOWS_INSTALLERS_README.txt") -Encoding UTF8

Write-Host "Windows native package convergence complete: $OutputDir"
