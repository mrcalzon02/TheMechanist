$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot
$javaw = if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME "bin\javaw.exe"))) { Join-Path $env:JAVA_HOME "bin\javaw.exe" } else { "javaw.exe" }
$argsList = @("-Dmechanist.assetRoot=.", "-Dmechanist.generatedAssetRoot=.", "-Dmechanist.assetTier=low_32", "-Dmechanist.assetResolution=32", "-cp", "classes;.", "mechanist.TheMechanist")
Start-Process -FilePath $javaw -ArgumentList $argsList -WorkingDirectory $PSScriptRoot -WindowStyle Hidden
exit 0
