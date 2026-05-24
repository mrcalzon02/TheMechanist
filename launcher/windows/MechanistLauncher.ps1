param(
    [string] $InstallDir,
    [string] $Branch = 'main',
    [switch] $NoUpdate,
    [switch] $NoLaunch,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GameArgs
)

$ErrorActionPreference = 'Stop'
$RepoUrl = 'https://github.com/mrcalzon02/TheMechanist.git'
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
    return $cmd.Source
}

function Resolve-InstallDir {
    param([string] $InputDir)
    if (-not [string]::IsNullOrWhiteSpace($InputDir)) { return $InputDir }
    $local = $env:LOCALAPPDATA
    if ([string]::IsNullOrWhiteSpace($local)) {
        return (Join-Path $HOME 'TheMechanist\repo')
    }
    return (Join-Path $local 'TheMechanist\repo')
}

function Ensure-Repo {
    param([string] $TargetDir, [string] $TargetBranch)
    $parent = Split-Path -Parent $TargetDir
    New-Item -ItemType Directory -Force -Path $parent | Out-Null

    if (-not (Test-Path -LiteralPath $TargetDir -PathType Container)) {
        Write-Step "Cloning $AppName into $TargetDir"
        git clone --branch $TargetBranch $RepoUrl $TargetDir
        return
    }

    if (-not (Test-Path -LiteralPath (Join-Path $TargetDir '.git') -PathType Container)) {
        throw "Install directory exists but is not a Git repository: $TargetDir"
    }

    Push-Location $TargetDir
    try {
        $remote = (git remote get-url origin) 2>$null
        if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($remote)) {
            throw 'Could not read Git remote origin.'
        }
        if ($remote.Trim() -ne $RepoUrl) {
            Write-Host "WARNING: origin is $remote"
            Write-Host "Expected: $RepoUrl"
        }

        if (-not $NoUpdate) {
            Write-Step "Updating $AppName from GitHub branch $TargetBranch"
            git fetch origin $TargetBranch
            if ($LASTEXITCODE -ne 0) { throw 'git fetch failed.' }

            git checkout $TargetBranch
            if ($LASTEXITCODE -ne 0) { throw "git checkout $TargetBranch failed." }

            git pull --ff-only origin $TargetBranch
            if ($LASTEXITCODE -ne 0) {
                Write-Host ''
                Write-Host 'Fast-forward update failed. This usually means local files were modified inside the repo.'
                Write-Host 'Move personal saves/logs/settings outside the repository, or resolve local Git changes manually.'
                throw 'git pull --ff-only failed.'
            }
        } else {
            Write-Step 'Skipping update by request.'
        }
    } finally {
        Pop-Location
    }
}

function Launch-Game {
    param([string] $TargetDir, [string[]] $ArgsForGame)
    $launcherBat = Join-Path $TargetDir 'RUN_THE_MECHANIST_WINDOWS.bat'
    $launcherPs1 = Join-Path $TargetDir 'RUN_THE_MECHANIST_WINDOWS.ps1'

    if (Test-Path -LiteralPath $launcherBat -PathType Leaf) {
        Write-Step "Launching $AppName"
        Push-Location $TargetDir
        try {
            if ($null -ne $ArgsForGame -and $ArgsForGame.Count -gt 0) {
                & $launcherBat @ArgsForGame
            } else {
                & $launcherBat
            }
        } finally {
            Pop-Location
        }
        return
    }

    if (Test-Path -LiteralPath $launcherPs1 -PathType Leaf) {
        Write-Step "Launching $AppName"
        Push-Location $TargetDir
        try {
            if ($null -ne $ArgsForGame -and $ArgsForGame.Count -gt 0) {
                & powershell.exe -ExecutionPolicy Bypass -File $launcherPs1 @ArgsForGame
            } else {
                & powershell.exe -ExecutionPolicy Bypass -File $launcherPs1
            }
        } finally {
            Pop-Location
        }
        return
    }

    throw 'Could not find the Windows game launcher scripts in the installed repository.'
}

Write-Step "$AppName GitHub Launcher"
Write-Host "Repository: $RepoUrl"
Write-Host "Branch:     $Branch"

Require-Command git | Out-Null
$resolvedInstallDir = Resolve-InstallDir $InstallDir
Write-Host "Install:    $resolvedInstallDir"

Ensure-Repo $resolvedInstallDir $Branch

if ($NoLaunch) {
    Write-Step 'Update complete. Launch skipped by request.'
    exit 0
}

Launch-Game $resolvedInstallDir $GameArgs
