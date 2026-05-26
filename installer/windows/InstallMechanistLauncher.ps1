param(
    [string] $InstallRoot,
    [string] $Branch = 'main',
    [switch] $NoDesktopShortcut,
    [switch] $NoStartMenuShortcut,
    [switch] $NoInitialUpdate
)

$ErrorActionPreference = 'Stop'

Write-Host ''
Write-Host '============================================================'
Write-Host 'The Mechanist legacy Git launcher installer is retired'
Write-Host '============================================================'
Write-Host ''
Write-Host 'Gate 2 requires a native installer/app-image that carries the thin launcher,'
Write-Host 'runtime manifests, and manifest-verified package seeds. This legacy installer'
Write-Host 'no longer installs a Git clone updater or performs an initial repository clone.'
Write-Host ''
Write-Host 'Build the current Windows launcher package with:'
Write-Host '  powershell -ExecutionPolicy Bypass -File .\scripts\package\build-windows-installers.ps1'
Write-Host ''
Write-Host 'Then test the portable app-image before EXE/MSI installer testing.'
exit 2
