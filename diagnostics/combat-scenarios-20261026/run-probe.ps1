param([string]$JdkHome = 'C:\Program Files\Java\jdk-17')
$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$taskClasses = Join-Path $PSScriptRoot 'classes'
$taskJar = Join-Path $taskRoot 'target\TheMechanist-all.jar'
New-Item -ItemType Directory -Force -Path $taskClasses | Out-Null
& (Join-Path $JdkHome 'bin\javac.exe') --release 17 -cp $taskJar -d $taskClasses (Join-Path $taskRoot 'src\mechanist\CombatScenarioBaselineProbe.java')
if ($LASTEXITCODE -ne 0) { throw 'Probe compilation failed.' }
Push-Location (Join-Path $taskRoot 'PACKAGE_client')
try {
    & (Join-Path $JdkHome 'bin\java.exe') -Xmx5g '-Djava.awt.headless=true' "-Dmechanist.storage.root=$PSScriptRoot\user" "-Dmechanist.client.storage.root=$PSScriptRoot\user\data" -cp "$taskClasses;$taskJar" mechanist.CombatScenarioBaselineProbe $PSScriptRoot *> (Join-Path $PSScriptRoot 'probe.log')
    if ($LASTEXITCODE -ne 0) { throw 'Probe failed; inspect probe.log.' }
} finally { Pop-Location }
