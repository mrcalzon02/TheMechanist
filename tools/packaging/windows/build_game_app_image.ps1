param(
    [string] $OutputDir = "dist/native/windows",
    [switch] $SkipTests
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")
$OutRoot = Join-Path $RepoRoot $OutputDir
$AppImageRoot = Join-Path $OutRoot "game-app-image"

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

Step "The Mechanist Game App-Image Build"
Write-Host "Repo:   $RepoRoot"
Write-Host "Output: $AppImageRoot"

$mvn = Need "mvn"
$jpackage = Need "jpackage"
$java = Need "java"
Write-Host "Maven:    $mvn"
Write-Host "Java:     $java"
Write-Host "jpackage: $jpackage"
& java -version

Step "Building game jars with Maven / Java 17"
Push-Location $RepoRoot
try {
    if ($SkipTests) { mvn -q -DskipTests package } else { mvn -q package }
    if ($LASTEXITCODE -ne 0) { throw "Game Maven build failed." }
} finally {
    Pop-Location
}

$ClientJar = Join-Path $RepoRoot "target\TheMechanist-obfuscated.jar"
$ServerJar = Join-Path $RepoRoot "target\TheMechanistServer-obfuscated.jar"
if (-not (Test-Path -LiteralPath $ClientJar)) {
    $ClientJar = Join-Path $RepoRoot "target\TheMechanist-all.jar"
}
if (-not (Test-Path -LiteralPath $ServerJar)) {
    $ServerJar = Join-Path $RepoRoot "target\TheMechanistServer-all.jar"
}
if (-not (Test-Path -LiteralPath $ClientJar)) { throw "Client jar not found after build." }
if (-not (Test-Path -LiteralPath $ServerJar)) { throw "Server jar not found after build." }

Step "Scanning game jars for Java 17 classfile compatibility"
python (Join-Path $RepoRoot "tools\packaging\scan_java17_classfiles.py") $ClientJar $ServerJar
if ($LASTEXITCODE -ne 0) { throw "Java 17 classfile scan failed." }

Step "Preparing jpackage input"
$InputDir = Join-Path $OutRoot "game-input"
New-Item -ItemType Directory -Force -Path $InputDir | Out-Null
Copy-Item -LiteralPath $ClientJar -Destination (Join-Path $InputDir "TheMechanist.jar") -Force
Copy-Item -LiteralPath $ServerJar -Destination (Join-Path $InputDir "TheMechanistServer.jar") -Force

Step "Creating game app-image"
New-Item -ItemType Directory -Force -Path $OutRoot | Out-Null
if (Test-Path -LiteralPath $AppImageRoot) { Remove-Item -LiteralPath $AppImageRoot -Recurse -Force }

$jpackageArgs = @(
    "--type", "app-image",
    "--name", "TheMechanist",
    "--app-version", "0.9.10",
    "--vendor", "StellarCore",
    "--dest", $AppImageRoot,
    "--input", $InputDir,
    "--main-jar", "TheMechanist.jar",
    "--main-class", "mechanist.TheMechanist",
    "--java-options", "-Dfile.encoding=UTF-8"
)

& jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage game app-image failed." }

Step "Game app-image complete"
Write-Host "Output: $AppImageRoot"
Write-Host "Server jar copied alongside jpackage input for later server executable packaging."
