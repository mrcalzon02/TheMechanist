param([string]$JdkHome='C:\Program Files\Java\jdk-17')
$ErrorActionPreference='Stop'
$taskRoot=(Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$taskJar=Join-Path $taskRoot 'target\TheMechanist-all.jar'
Push-Location (Join-Path $taskRoot 'PACKAGE_client')
try {
    foreach($taskClass in @('CombatSupportExecutionSmoke','CombatSupportScenarioProbe')) {
        $taskArguments=@('-Xmx5g','-Djava.awt.headless=true',"-Dmechanist.storage.root=$PSScriptRoot\rerun-user","-Dmechanist.client.storage.root=$PSScriptRoot\rerun-user\data",'-cp',$taskJar,"mechanist.$taskClass",$PSScriptRoot)
        & (Join-Path $JdkHome 'bin\java.exe') @taskArguments *> (Join-Path $PSScriptRoot "$taskClass-rerun.log")
        if($LASTEXITCODE -ne 0) {throw "$taskClass failed; inspect its log."}
    }
} finally {Pop-Location}
