param(
    [string] $OutputDir = "dist/native/windows",
    [switch] $SkipTests
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")
$OutRoot = Join-Path $RepoRoot $OutputDir
$AppImageRoot = Join-Path $OutRoot "server-app-image"

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

Step "The Mechanist Headless Server App-Image Build"
Write-Host "Repo:   $RepoRoot"
Write-Host "Output: $AppImageRoot"

$mvn = Need "mvn"
$jpackage = Need "jpackage"
$java = Need "java"
Write-Host "Maven:    $mvn"
Write-Host "Java:     $java"
Write-Host "jpackage: $jpackage"
& java -version

Step "Building server jar with Maven / Java 17"
Push-Location $RepoRoot
try {
    if ($SkipTests) { mvn -q -DskipTests package } else { mvn -q package }
    if ($LASTEXITCODE -ne 0) { throw "Game Maven build failed." }
} finally {
    Pop-Location
}

$ServerJar = Join-Path $RepoRoot "target\TheMechanistServer-obfuscated.jar"
if (-not (Test-Path -LiteralPath $ServerJar)) {
    $ServerJar = Join-Path $RepoRoot "target\TheMechanistServer-all.jar"
}
if (-not (Test-Path -LiteralPath $ServerJar)) { throw "Server jar not found after build." }

Step "Scanning server jar for Java 17 classfile compatibility"
python (Join-Path $RepoRoot "tools\packaging\scan_java17_classfiles.py") $ServerJar
if ($LASTEXITCODE -ne 0) { throw "Java 17 classfile scan failed." }

Step "Preparing jpackage input"
$InputDir = Join-Path $OutRoot "server-input"
New-Item -ItemType Directory -Force -Path $InputDir | Out-Null
Copy-Item -LiteralPath $ServerJar -Destination (Join-Path $InputDir "TheMechanistServer.jar") -Force

Step "Creating server app-image"
New-Item -ItemType Directory -Force -Path $OutRoot | Out-Null
if (Test-Path -LiteralPath $AppImageRoot) { Remove-Item -LiteralPath $AppImageRoot -Recurse -Force }

$jpackageArgs = @(
    "--type", "app-image",
    "--name", "TheMechanistServer",
    "--app-version", "0.9.10",
    "--vendor", "StellarCore",
    "--dest", $AppImageRoot,
    "--input", $InputDir,
    "--main-jar", "TheMechanistServer.jar",
    "--main-class", "mechanist.MechanistServerMain",
    "--java-options", "-Dfile.encoding=UTF-8"
)

& jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage server app-image failed." }

Step "Server app-image complete"
Write-Host "Output: $AppImageRoot"
Write-Host "This is the internal/headless server executable image, not a public multiplayer claim."
