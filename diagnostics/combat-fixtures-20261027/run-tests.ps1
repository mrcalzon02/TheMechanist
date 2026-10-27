param([string]$JdkHome='C:\Program Files\Java\jdk-17')
$ErrorActionPreference='Stop'
$taskRoot=(Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$taskResults=@()
Push-Location (Join-Path $taskRoot 'PACKAGE_client')
try {
 foreach($taskClass in @('CombatFixtureExecutionSmoke','Milestone06StructuralScaleCombatSmoke','Milestone06VehicleRuntimeFoundationSmoke','Milestone06VehicleTransitSmoke','Milestone06VehicleAccessSmoke','Milestone06VehicleManifestSmoke','Milestone06VehicleFuelHistorySmoke','Milestone06VehicleMaintenanceSmoke','Milestone06VehicleOperationFeedbackSmoke','Milestone06VehicleLossSmoke','ActorCombatPresentationSmoke','ActorCombatExecutionSmoke','DeveloperFacilitySmoke','WeaponAudioSmoke','WeaponSoundVariationSmoke','SpatialAudioSmoke','Milestone02DoomHudFacingSmoke')) {
  $taskOut=if($taskClass -eq 'CombatFixtureExecutionSmoke'){$PSScriptRoot}else{Join-Path $PSScriptRoot 'supporting'}
  New-Item -ItemType Directory -Force $taskOut | Out-Null
  & (Join-Path $JdkHome 'bin\java.exe') '-Xmx5g' '-Djava.awt.headless=true' "-Dmechanist.storage.root=$PSScriptRoot\test-user" "-Dmechanist.client.storage.root=$PSScriptRoot\test-user\data" '-cp' (Join-Path $taskRoot 'target\TheMechanist-all.jar') "mechanist.$taskClass" $taskOut *> (Join-Path $PSScriptRoot "$taskClass.log")
  $taskResults+=@{test=$taskClass;exitCode=$LASTEXITCODE;log="$taskClass.log"}
  $taskResults | ConvertTo-Json | Set-Content (Join-Path $PSScriptRoot 'regressions.json')
  Write-Output "$taskClass exit=$LASTEXITCODE"
 }
 if($taskResults.Where({$_.exitCode -ne 0}).Count -gt 0){throw 'Fixture regressions failed; inspect regressions.json.'}
} finally {Pop-Location}
