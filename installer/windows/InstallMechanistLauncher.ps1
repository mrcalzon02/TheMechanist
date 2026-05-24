param(
    [string] $InstallRoot,
    [string] $Branch = 'main',
    [switch] $NoDesktopShortcut,
    [switch] $NoStartMenuShortcut,
    [switch] $NoInitialUpdate
)

$ErrorActionPreference = 'Stop'
$AppName = 'The Mechanist'

function Write-Step {
    param([string] $Message)
    Write-Host ''
    Write-Host '============================================================'
    Write-Host $Message
    Write-Host '============================================================'
}

function Require-Command {
    param([string] $Name)
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if ($null -eq $cmd) {
        throw "Required command not found: $Name. Install Git for Windows and ensure it is available from PowerShell."
    }
}

function Resolve-InstallRoot {
    param([string] $InputRoot)
    if (-not [string]::IsNullOrWhiteSpace($InputRoot)) { return $InputRoot }
    if (-not [string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) { return (Join-Path $env:LOCALAPPDATA 'TheMechanist') }
    return (Join-Path $HOME 'TheMechanist')
}

function Create-Shortcut {
    param([string] $ShortcutPath, [string] $TargetPath, [string] $WorkingDirectory, [string] $Description)
    $shell = New-Object -ComObject WScript.Shell
    $shortcut = $shell.CreateShortcut($ShortcutPath)
    $shortcut.TargetPath = $TargetPath
    $shortcut.WorkingDirectory = $WorkingDirectory
    $shortcut.Description = $Description
    $shortcut.Save()
}

Write-Step "$AppName Launcher Installer"
Require-Command git

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir '..\..')
$sourceLauncherDir = Join-Path $repoRoot 'launcher\windows'
if (-not (Test-Path -LiteralPath $sourceLauncherDir -PathType Container)) {
    throw "Could not locate launcher source directory: $sourceLauncherDir"
}

$root = Resolve-InstallRoot $InstallRoot
$launcherDir = Join-Path $root 'launcher'
$repoDir = Join-Path $root 'repo'
Write-Host "Install root: $root"
Write-Host "Launcher:     $launcherDir"
Write-Host "Game repo:    $repoDir"
Write-Host "Branch:       $Branch"

Write-Step 'Installing launcher files'
New-Item -ItemType Directory -Force -Path $launcherDir | Out-Null
Copy-Item -LiteralPath (Join-Path $sourceLauncherDir 'MechanistLauncher.ps1') -Destination (Join-Path $launcherDir 'MechanistLauncher.ps1') -Force
Copy-Item -LiteralPath (Join-Path $sourceLauncherDir 'RUN_MECHANIST_LAUNCHER.bat') -Destination (Join-Path $launcherDir 'RUN_MECHANIST_LAUNCHER.bat') -Force

$configPath = Join-Path $launcherDir 'launcher-config.properties'
$configText = @(
    'repo=https://github.com/mrcalzon02/TheMechanist.git',
    ('branch=' + $Branch),
    ('repoDir=' + $repoDir),
    'createdBy=The Mechanist Phase O installer'
)
Set-Content -Path $configPath -Value $configText -Encoding UTF8

$launcherBatPath = Join-Path $launcherDir 'RUN_MECHANIST_LAUNCHER.bat'

if (-not $NoDesktopShortcut) {
    $desktop = [Environment]::GetFolderPath('Desktop')
    if (-not [string]::IsNullOrWhiteSpace($desktop)) {
        $shortcutPath = Join-Path $desktop 'The Mechanist Launcher.lnk'
        Create-Shortcut $shortcutPath $launcherBatPath $launcherDir 'Update and launch The Mechanist'
        Write-Host "Desktop shortcut: $shortcutPath"
    }
}

if (-not $NoStartMenuShortcut) {
    $programs = [Environment]::GetFolderPath('Programs')
    if (-not [string]::IsNullOrWhiteSpace($programs)) {
        $menuDir = Join-Path $programs 'The Mechanist'
        New-Item -ItemType Directory -Force -Path $menuDir | Out-Null
        $shortcutPath = Join-Path $menuDir 'The Mechanist Launcher.lnk'
        Create-Shortcut $shortcutPath $launcherBatPath $launcherDir 'Update and launch The Mechanist'
        Write-Host "Start Menu shortcut: $shortcutPath"
    }
}

if (-not $NoInitialUpdate) {
    Write-Step 'Performing first update/clone without launching'
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $launcherDir 'MechanistLauncher.ps1') -InstallDir $repoDir -Branch $Branch -NoLaunch
}

Write-Step 'Installer complete'
Write-Host 'Use the desktop/start-menu shortcut or run:'
Write-Host "  $launcherBatPath"
