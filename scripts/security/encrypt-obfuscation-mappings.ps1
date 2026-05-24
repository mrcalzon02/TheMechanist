[CmdletBinding()]
param(
    [string]$KeyFile = $env:MECHANIST_MAPPING_KEY_FILE
)
$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$BuildDir = Join-Path $ProjectRoot "build\tools\obfuscation"
$ToolSrc = Join-Path $ProjectRoot "tools\obfuscation\MappingEncryptionTool.java"
if (-not $KeyFile) { $KeyFile = Join-Path $ProjectRoot "build\secure-local\mapping.key" }
$SecureMaps = Join-Path $ProjectRoot "dist\secure-maps"
New-Item -ItemType Directory -Force -Path $BuildDir, $SecureMaps | Out-Null
& javac --release 17 -d $BuildDir $ToolSrc
if ($LASTEXITCODE -ne 0) { throw "javac failed while compiling MappingEncryptionTool" }
foreach ($targetName in @("client", "server")) {
    $Raw = Join-Path $ProjectRoot "target\proguard\$targetName\mapping.raw.txt"
    $Encrypted = Join-Path $SecureMaps "$targetName-mapping.txt"
    if (-not (Test-Path -LiteralPath $Raw) -or ((Get-Item -LiteralPath $Raw).Length -le 0)) {
        throw "Expected ProGuard raw mapping was not found or is empty: $Raw"
    }
    & java -cp $BuildDir MappingEncryptionTool --encrypt --in $Raw --out $Encrypted --key-file $KeyFile --delete-input
    if ($LASTEXITCODE -ne 0) { throw "MappingEncryptionTool failed for $targetName" }
    Write-Host "Encrypted ProGuard mapping: $Encrypted"
}
