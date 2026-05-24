[CmdletBinding()]
param(
    [string]$AppName = "The Mechanist",
    [string]$AppVersion = "0.9.10it",
    [string]$Vendor = "The Mechanist Project",
    [bool]$PerUserInstall = $true,
    [switch]$UseExistingJar,
    [switch]$RequireNativeInstallers
)

$script = Join-Path $PSScriptRoot "build-windows-installers.ps1"
$args = @(
    "-AppName", $AppName,
    "-AppVersion", $AppVersion,
    "-Vendor", $Vendor,
    "-PackageTypes", "msi",
    "-PerUserInstall", ([string]$PerUserInstall)
)
if ($UseExistingJar) { $args += "-UseExistingJar" }
if ($RequireNativeInstallers) { $args += "-RequireNativeInstallers" }
& $script @args
