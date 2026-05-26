param(
    [string] $InstallDir,
    [string] $Branch = 'main',
    [switch] $NoUpdate,
    [switch] $NoLaunch,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GameArgs
)

$ErrorActionPreference = 'Stop'

Write-Host ''
Write-Host '============================================================'
Write-Host 'The Mechanist legacy Git launcher is retired'
Write-Host '============================================================'
Write-Host ''
Write-Host 'Gate 2 requires installer -> thin launcher -> manifest-verified client/server packages.'
Write-Host 'This script no longer clones or updates the full development repository.'
Write-Host ''
Write-Host 'Use the native packaging pipeline to build the manifest launcher:'
Write-Host '  powershell -ExecutionPolicy Bypass -File .\scripts\package\build-windows-installers.ps1'
Write-Host ''
Write-Host 'Then run the produced launcher app-image or installer.'
exit 2
