param(
    [string] $OutputDir = "dist/native/windows",
    [switch] $SkipTests
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")
$LauncherDir = Join-Path $RepoRoot "launcher\java"
$OutRoot = Join-Path $RepoRoot $OutputDir
$InstallerRoot = Join-Path $OutRoot "launcher-installer"
$LauncherResourceRoot = Join-Path $LauncherDir "src\main\resources"

function Step($Message) {
    Write-Host ""
    Write-Host "============================================================"
    Write-Host $Message
    Write-Host "============================================================"
}

function Need($Name) {
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if ($null -eq $cmd) { throw "Required command not found: $Name" }
    return $cmd.Source
}

Step "The Mechanist Launcher EXE Installer Build"
Write-Host "Repo:   $RepoRoot"
Write-Host "Output: $InstallerRoot"
Write-Host "Note: jpackage EXE/MSI generation on Windows requires the WiX Toolset on PATH."

$mvn = Need "mvn"
$jpackage = Need "jpackage"
$java = Need "java"
Write-Host "Maven:    $mvn"
Write-Host "Java:     $java"
Write-Host "jpackage: $jpackage"
& java -version

# WiX v3 generally exposes candle/light. WiX v4 may expose wix.exe. jpackage availability varies by JDK.
$wixCandle = Get-Command candle.exe -ErrorAction SilentlyContinue
$wixLight = Get-Command light.exe -ErrorAction SilentlyContinue
$wixExe = Get-Command wix.exe -ErrorAction SilentlyContinue
if ($null -eq $wixExe -and ($null -eq $wixCandle -or $null -eq $wixLight)) {
    throw "WiX Toolset not found. Install WiX and ensure wix.exe or candle.exe/light.exe is on PATH before building EXE/MSI installers. Use build_launcher_app_image.ps1 first for portable testing."
}

Step "Building launcher jar with Maven / Java 17"
Push-Location $LauncherDir
try {
    if ($SkipTests) { mvn -q -DskipTests package } else { mvn -q package }
    if ($LASTEXITCODE -ne 0) { throw "Launcher Maven build failed." }
} finally {
    Pop-Location
}

$LauncherJar = Join-Path $LauncherDir "target\mechanist-launcher-0.1.0.jar"
if (-not (Test-Path -LiteralPath $LauncherJar)) { throw "Launcher jar not found: $LauncherJar" }
$RequiredLauncherResources = @(
    "assets\app\icons\the-mechanist-256.png",
    "assets\app\icons\the-mechanist-128.png",
    "assets\app\icons\the-mechanist-64.png",
    "assets\sound\core\ambient_press_01.wav",
    "assets\sound\core\ambient_chime_01.wav",
    "assets\sound\core\ambient_alarm_far_01.wav"
)
foreach ($rel in $RequiredLauncherResources) {
    $path = Join-Path $LauncherResourceRoot $rel
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Launcher-owned resource missing: $path"
    }
}

Step "Scanning launcher jar for Java 17 classfile compatibility"
python (Join-Path $RepoRoot "tools\packaging\scan_java17_classfiles.py") $LauncherJar
if ($LASTEXITCODE -ne 0) { throw "Java 17 classfile scan failed." }

Step "Creating launcher EXE installer"
New-Item -ItemType Directory -Force -Path $InstallerRoot | Out-Null

$jpackageArgs = @(
    "--type", "exe",
    "--name", "TheMechanistLauncher",
    "--app-version", "0.1.0",
    "--vendor", "StellarCore",
    "--dest", $InstallerRoot,
    "--input", (Join-Path $LauncherDir "target"),
    "--main-jar", "mechanist-launcher-0.1.0.jar",
    "--main-class", "mechanist.launcher.MechanistLauncherApp",
    "--win-dir-chooser",
    "--win-menu",
    "--win-shortcut",
    "--java-options", "-Dfile.encoding=UTF-8"
)

$IconPath = Join-Path $RepoRoot "assets\app\icons\the-mechanist.ico"
if (Test-Path -LiteralPath $IconPath) {
    $jpackageArgs += @("--icon", $IconPath)
} else {
    Write-Host "Icon not found at $IconPath; building installer without custom icon."
}

& jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage EXE installer failed." }

Step "Launcher EXE installer complete"
Write-Host "Output: $InstallerRoot"
Write-Host "Install this on a test machine/account before publishing."
