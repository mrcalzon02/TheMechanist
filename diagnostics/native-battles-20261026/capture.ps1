param([string]$JdkHome='C:\Program Files\Java\jdk-17')
$ErrorActionPreference='Stop'
$taskRoot=(Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Push-Location (Join-Path $taskRoot 'PACKAGE_client')
try {
 & (Join-Path $JdkHome 'bin\java.exe') '-Xmx5g' '-Djava.awt.headless=true' "-Dmechanist.storage.root=$PSScriptRoot\capture-user" "-Dmechanist.client.storage.root=$PSScriptRoot\capture-user\data" '-cp' (Join-Path $taskRoot 'target\TheMechanist-all.jar') 'mechanist.NativeBattleCaptureProbe' $PSScriptRoot *> (Join-Path $PSScriptRoot 'capture.log')
 if($LASTEXITCODE -ne 0){throw 'Native capture failed; inspect capture.log.'}
} finally {Pop-Location}
