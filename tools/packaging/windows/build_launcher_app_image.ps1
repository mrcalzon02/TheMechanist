param(
    [string] $OutputDir = "dist/native/windows",
    [switch] $SkipTests
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")
$LauncherDir = Join-Path $RepoRoot "launcher\java"
$OutRoot = Join-Path $RepoRoot $OutputDir
$AppImageRoot = Join-Path $OutRoot "app-image"

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

Step "The Mechanist Launcher App-Image Build"
Write-Host "Repo:     $RepoRoot"
Write-Host "Launcher: $LauncherDir"
Write-Host "Output:   $AppImageRoot"

$mvn = Need "mvn"
$jpackage = Need "jpackage"
$java = Need "java"
Write-Host "Maven:    $mvn"
Write-Host "Java:     $java"
Write-Host "jpackage: $jpackage"
& java -version

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

Step "Scanning launcher jar for Java 17 classfile compatibility"
python (Join-Path $RepoRoot "tools\packaging\scan_java17_classfiles.py") $LauncherJar
if ($LASTEXITCODE -ne 0) { throw "Java 17 classfile scan failed." }

Step "Creating launcher app-image"
New-Item -ItemType Directory -Force -Path $OutRoot | Out-Null
if (Test-Path -LiteralPath $AppImageRoot) { Remove-Item -LiteralPath $AppImageRoot -Recurse -Force }

$jpackageArgs = @(
    "--type", "app-image",
    "--name", "TheMechanistLauncher",
    "--app-version", "0.1.0",
    "--vendor", "StellarCore",
    "--dest", $AppImageRoot,
    "--input", (Join-Path $LauncherDir "target"),
    "--main-jar", "mechanist-launcher-0.1.0.jar",
    "--main-class", "mechanist.launcher.MechanistLauncherApp",
    "--java-options", "-Dfile.encoding=UTF-8"
)

& jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage app-image failed." }

Step "Launcher app-image complete"
Write-Host "Output: $AppImageRoot"
Write-Host "Run the generated EXE inside the app-image before testing installer packaging."
