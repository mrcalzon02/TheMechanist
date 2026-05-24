[CmdletBinding()]
param()
$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$BuildDir = Join-Path $ProjectRoot "build\tools\obfuscation"
$GeneratorSrc = Join-Path $ProjectRoot "tools\obfuscation\SensitiveStringTableGenerator.java"
$InputFile = Join-Path $ProjectRoot "config\obfuscation\sensitive-strings.properties"
$OutputFile = Join-Path $ProjectRoot "src\mechanist\ObfuscatedStringTable.java"
New-Item -ItemType Directory -Force -Path $BuildDir, (Split-Path -Parent $OutputFile) | Out-Null
& javac --release 17 -d $BuildDir $GeneratorSrc
if ($LASTEXITCODE -ne 0) { throw "javac failed while compiling SensitiveStringTableGenerator" }
& java -cp $BuildDir SensitiveStringTableGenerator --input $InputFile --output $OutputFile --package mechanist --class ObfuscatedStringTable
if ($LASTEXITCODE -ne 0) { throw "SensitiveStringTableGenerator failed" }
Write-Host "Generated $OutputFile"
