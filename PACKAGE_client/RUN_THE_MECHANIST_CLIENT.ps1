$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot
& java -cp "classes;." mechanist.TheMechanist
exit $LASTEXITCODE
