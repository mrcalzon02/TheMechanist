[CmdletBinding()]
param(
    [string]$AppName = "The Mechanist",
    [string]$AppVersion = "0.9.10ix",
    [string]$Vendor = "The Mechanist Project",
    [string[]]$PackageTypes = @("app-image", "exe", "msi"),
    [bool]$PerUserInstall = $true,
    [switch]$UseExistingJar,
    [switch]$RequireNativeInstallers,
    [string]$InstallDirName = "TheMechanist",
    [string]$UpgradeUuid = "b7c10db2-4a4b-4bb2-9c13-93b6fb4d70df"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$TargetDir = Join-Path $ProjectRoot "target"
$DistDir = Join-Path $ProjectRoot "dist\installers\windows"
$RuntimeDir = Join-Path $ProjectRoot "build\jlink\runtime-client-windows"
$InputDir = Join-Path $ProjectRoot "build\package-input\windows"
$AppImageDest = Join-Path $DistDir "app-image"
$IconPath = Join-Path $ProjectRoot "assets\app\icons\the-mechanist.ico"
$ModuleFile = Join-Path $ProjectRoot "packaging\jlink\client-modules.txt"
$WindowsReadme = Join-Path $DistDir "WINDOWS_INSTALLERS_README.txt"

function Require-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' was not found on PATH."
    }
}

function Test-Command([string]$Name) {
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

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

function Escape-Readme([string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) { return "(not produced)" }
    return $Value
}

function Get-RelativePathPortable([string]$BasePath, [string]$ChildPath) {
    $baseFull = [System.IO.Path]::GetFullPath($BasePath)
    $childFull = [System.IO.Path]::GetFullPath($ChildPath)
    if (-not $baseFull.EndsWith([System.IO.Path]::DirectorySeparatorChar)) {
        $baseFull += [System.IO.Path]::DirectorySeparatorChar
    }
    $baseUri = New-Object System.Uri($baseFull)
    $childUri = New-Object System.Uri($childFull)
    $relativeUri = $baseUri.MakeRelativeUri($childUri)
    return [System.Uri]::UnescapeDataString($relativeUri.ToString()).Replace("/", [System.IO.Path]::DirectorySeparatorChar)
}

Require-Command java
Require-Command javac
Require-Command jlink
Require-Command jpackage

if (-not $env:JAVA_HOME) {
    throw "JAVA_HOME is not set. Set JAVA_HOME to a Java 17 JDK before packaging."
}
$Jmods = Join-Path $env:JAVA_HOME "jmods"
if (-not (Test-Path -LiteralPath $Jmods)) {
    throw "JAVA_HOME does not point to a full JDK with jmods: $env:JAVA_HOME"
}
if (-not (Test-Path -LiteralPath $ModuleFile)) {
    throw "Missing jlink module list: $ModuleFile"
}
$ClientModules = (Get-Content -LiteralPath $ModuleFile -Raw).Replace("`r", "").Replace("`n", "").Replace(" ", "")
if ([string]::IsNullOrWhiteSpace($ClientModules)) {
    throw "Client module list was empty: $ModuleFile"
}

$normalizedTypes = New-Object System.Collections.Generic.List[string]
foreach ($type in $PackageTypes) {
    $normal = Normalize-PackageType $type
    if (-not $normalizedTypes.Contains($normal)) {
        $normalizedTypes.Add($normal) | Out-Null
    }
}

$WixRequired = $normalizedTypes.Contains("exe") -or $normalizedTypes.Contains("msi")
$WixTools = @("candle.exe", "light.exe")
$WixAvailable = $true
foreach ($tool in $WixTools) {
    if (-not (Test-Command $tool)) {
        $WixAvailable = $false
        Write-Warning "WiX Toolset executable '$tool' was not found on PATH. jpackage EXE/MSI generation requires WiX 3.x on Windows."
    }
}
if ($WixRequired -and -not $WixAvailable -and $RequireNativeInstallers) {
    throw "WiX Toolset 3.x is required for EXE/MSI generation and -RequireNativeInstallers was set. Install WiX and retry."
}

Set-Location $ProjectRoot
Remove-Item -LiteralPath $DistDir, $RuntimeDir, $InputDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $DistDir, $InputDir, $AppImageDest | Out-Null

if (-not $UseExistingJar) {
    Require-Command mvn
    & (Join-Path $ProjectRoot "scripts\security\generate-sensitive-strings.ps1")
    mvn -B -DskipTests package
    & (Join-Path $ProjectRoot "scripts\security\encrypt-obfuscation-mappings.ps1")
}

$ObfuscatedJar = Join-Path $TargetDir "TheMechanist-obfuscated.jar"
$FallbackRootJar = Join-Path $ProjectRoot "TheMechanist.jar"
$JarToPackage = $null
$PackagedJarName = "TheMechanist-obfuscated.jar"

if (Test-Path -LiteralPath $ObfuscatedJar) {
    $obfInfo = Get-Item -LiteralPath $ObfuscatedJar
    if ($obfInfo.Length -gt 0) { $JarToPackage = $ObfuscatedJar }
}
if ($null -eq $JarToPackage -and $UseExistingJar -and (Test-Path -LiteralPath $FallbackRootJar)) {
    $rootInfo = Get-Item -LiteralPath $FallbackRootJar
    if ($rootInfo.Length -gt 0) {
        $JarToPackage = $FallbackRootJar
        $PackagedJarName = "TheMechanist.jar"
        Write-Warning "Using root development jar for installer testing because -UseExistingJar was set: $FallbackRootJar"
    }
}
if ($null -eq $JarToPackage) {
    throw "No packageable client jar was found. Expected $ObfuscatedJar, or use -UseExistingJar with $FallbackRootJar present."
}

Copy-Item -LiteralPath $JarToPackage -Destination (Join-Path $InputDir $PackagedJarName) -Force
$LibDir = Join-Path $ProjectRoot "lib"
if (Test-Path -LiteralPath $LibDir -PathType Container) {
    Copy-Item -LiteralPath $LibDir -Destination (Join-Path $InputDir "lib") -Recurse -Force
}
if (Test-Path -LiteralPath $IconPath) {
    Copy-Item -LiteralPath $IconPath -Destination (Join-Path $InputDir "the-mechanist.ico") -Force
} else {
    Write-Warning "Windows icon was not found at $IconPath. Installer will be produced without a custom icon."
}

& jlink `
  --module-path $Jmods `
  --add-modules $ClientModules `
  --output $RuntimeDir `
  --strip-debug `
  --no-man-pages `
  --no-header-files `
  --strip-native-commands `
  --compress=2

function Invoke-JPackage([string]$PackageType, [string]$Destination) {
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    $args = @(
        "--type", $PackageType,
        "--name", $AppName,
        "--app-version", $AppVersion,
        "--vendor", $Vendor,
        "--description", "The Mechanist Java 17 desktop simulation client",
        "--runtime-image", $RuntimeDir,
        "--input", $InputDir,
        "--main-jar", $PackagedJarName,
        "--main-class", "mechanist.TheMechanist",
        "--dest", $Destination,
        "--resource-dir", (Join-Path $ProjectRoot "packaging\windows")
    )
    if (Test-Path -LiteralPath $IconPath) {
        $args += @("--icon", $IconPath)
    }
    if ($PackageType -eq "exe" -or $PackageType -eq "msi") {
        $args += @(
            "--install-dir", $InstallDirName,
            "--win-menu",
            "--win-menu-group", "The Mechanist",
            "--win-shortcut",
            "--win-shortcut-prompt",
            "--win-dir-chooser",
            "--win-upgrade-uuid", $UpgradeUuid
        )
        if ($PerUserInstall) {
            $args += @("--win-per-user-install")
        }
    }
    Write-Host "Running jpackage --type $PackageType ..."
    & jpackage @args
}

$Produced = New-Object System.Collections.Generic.List[string]
foreach ($type in $normalizedTypes) {
    if ($type -eq "app-image") {
        Invoke-JPackage "app-image" $AppImageDest
        $imageRoot = Join-Path $AppImageDest $AppName
        if (-not (Test-Path -LiteralPath $imageRoot)) {
            throw "jpackage app-image did not produce the expected application image folder: $imageRoot"
        }
        $portableZip = Join-Path $DistDir ("TheMechanist_windows_portable_{0}.zip" -f $AppVersion)
        Remove-Item -LiteralPath $portableZip -Force -ErrorAction SilentlyContinue
        Compress-Archive -LiteralPath $imageRoot -DestinationPath $portableZip -Force
        $Produced.Add($portableZip) | Out-Null
        $exePath = Join-Path $imageRoot ("{0}.exe" -f $AppName)
        if (Test-Path -LiteralPath $exePath) {
            $Produced.Add($exePath) | Out-Null
        }
    } elseif ($type -eq "exe" -or $type -eq "msi") {
        if (-not $WixAvailable) {
            Write-Warning "Skipping $type installer because WiX Toolset 3.x was not found. Portable app-image output remains usable for testing."
            continue
        }
        Invoke-JPackage $type $DistDir
        Get-ChildItem -LiteralPath $DistDir -File -Filter "*.$type" | ForEach-Object { $Produced.Add($_.FullName) | Out-Null }
    }
}

Get-ChildItem -LiteralPath $DistDir -File -Recurse | Where-Object { $_.Name -ne "SHA256SUMS.txt" } | Sort-Object FullName | Get-FileHash -Algorithm SHA256 | ForEach-Object {
    $relative = (Get-RelativePathPortable $DistDir $_.Path).Replace("\", "/")
    "$($_.Hash.ToLowerInvariant())  $relative"
} | Set-Content -LiteralPath (Join-Path $DistDir "SHA256SUMS.txt") -Encoding ascii

$portableLine = Escape-Readme ((Get-ChildItem -LiteralPath $DistDir -File -Filter "TheMechanist_windows_portable_*.zip" -ErrorAction SilentlyContinue | Select-Object -First 1).FullName)
$exeLine = Escape-Readme ((Get-ChildItem -LiteralPath $DistDir -File -Filter "*.exe" -ErrorAction SilentlyContinue | Select-Object -First 1).FullName)
$msiLine = Escape-Readme ((Get-ChildItem -LiteralPath $DistDir -File -Filter "*.msi" -ErrorAction SilentlyContinue | Select-Object -First 1).FullName)
@"
The Mechanist Windows installer outputs
======================================

Version: $AppVersion
Packaged jar: $PackagedJarName
Per-user install requested: $PerUserInstall
Install directory name: $InstallDirName
Desktop shortcut requested: yes
Start Menu group requested: The Mechanist
Directory chooser requested: yes
Shortcut prompt requested: yes
Icon path: $IconPath

Outputs:
- Portable app-image zip: $portableLine
- EXE installer: $exeLine
- MSI installer: $msiLine

Recommended Windows testing order:
1. Unzip the portable app-image zip and run "The Mechanist\The Mechanist.exe".
2. Run the EXE installer and confirm that the wizard asks for install location and shortcut preference.
3. Verify a desktop shortcut named "The Mechanist" appears when shortcuts are accepted.
4. Verify Start Menu > The Mechanist > The Mechanist launches the same app.
5. Test the MSI after the EXE path, because MSI behavior varies more depending on enterprise policy and Windows Installer state.

If nothing appears after double-clicking a script:
- Open PowerShell in the project root instead.
- Run: powershell -ExecutionPolicy Bypass -File .\scripts\package\build-windows-installers.ps1 -UseExistingJar
- Watch the console output for missing JAVA_HOME, Maven, jpackage, or WiX diagnostics.
"@ | Set-Content -LiteralPath $WindowsReadme -Encoding utf8

Write-Host "Windows packaging outputs written to $DistDir"
Write-Host "Portable app-image and native installer checksums written to SHA256SUMS.txt"
