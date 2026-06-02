$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot
& java -Dmechanist.assetRoot=. -Dmechanist.generatedAssetRoot=. -Dmechanist.assetTier=low_32 -cp "classes;." mechanist.TheMechanist
exit $LASTEXITCODE
