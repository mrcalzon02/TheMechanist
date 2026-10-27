param([string]$JdkHome='C:\Program Files\Java\jdk-17')
$ErrorActionPreference='Stop'
$taskRoot=(Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$taskJar=Join-Path $taskRoot 'target\TheMechanist-all.jar'
$taskResults=@()
Push-Location (Join-Path $taskRoot 'PACKAGE_client')
try {
 foreach($taskClass in @('SpatialAudioSmoke','WeaponAudioSmoke','ActorCombatPresentationSmoke','CyberneticLimbExecutionSmoke','MedicalTreatmentBoundarySmoke','CombatSupportExecutionSmoke','ActorCombatExecutionSmoke','DeveloperFacilitySmoke','NpcContinuityDeterminismSmoke','Milestone02DoomHudFacingSmoke','LightingFalloffStreetLightSmoke','Milestone02RenderAssetBindingDoctrineSmoke','Milestone02MedicalTreatmentReadabilitySmoke','Milestone02BodyConditionReadabilitySmoke','Milestone06StructuralScaleCombatSmoke','Milestone04MedicalSupplyProvenanceSmoke')) {
  $taskArguments=@('-Xmx5g','-Djava.awt.headless=true',"-Dmechanist.storage.root=$PSScriptRoot\test-user", "-Dmechanist.client.storage.root=$PSScriptRoot\test-user\data",'-cp',$taskJar,"mechanist.$taskClass",$PSScriptRoot)
  & (Join-Path $JdkHome 'bin\java.exe') @taskArguments *> (Join-Path $PSScriptRoot "$taskClass.log")
  $taskExit=$LASTEXITCODE;$taskResults+=@{test=$taskClass;exitCode=$taskExit;log="$taskClass.log"}
  $taskResults | ConvertTo-Json | Set-Content (Join-Path $PSScriptRoot 'regressions.json')
  Write-Output "$taskClass exit=$taskExit"
 }
 if($taskResults.Where({$_.exitCode -ne 0}).Count -gt 0) {throw 'Focused checks failed; inspect regressions.json.'}
} finally {Pop-Location}
