param(
    [string] $OutputDir = "dist/native/windows",
    [string] $Channel = "dev",
    [switch] $BuildInstaller,
    [switch] $SkipTests
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")
$OutRoot = Join-Path $RepoRoot $OutputDir

function Step($Message) {
    Write-Host ""
    Write-Host "============================================================"
    Write-Host $Message
    Write-Host "============================================================"
}

function Invoke-StepScript($ScriptPath, [string[]] $ExtraArgs = @()) {
    if (-not (Test-Path -LiteralPath $ScriptPath)) { throw "Required packaging script missing: $ScriptPath" }
    $args = @("-ExecutionPolicy", "Bypass", "-File", $ScriptPath, "-OutputDir", $OutputDir)
    if ($SkipTests) { $args += "-SkipTests" }
    $args += $ExtraArgs
    & powershell @args
    if ($LASTEXITCODE -ne 0) { throw "Packaging step failed: $ScriptPath" }
}

Step "The Mechanist Verified Native Windows Packaging Orchestrator"
Write-Host "Repo:      $RepoRoot"
Write-Host "Output:    $OutRoot"
Write-Host "Channel:   $Channel"
Write-Host "Installer: $BuildInstaller"
Write-Host "SkipTests: $SkipTests"

Step "Tool diagnostics"
foreach ($name in @("java", "mvn", "jpackage", "python", "git")) {
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if ($null -eq $cmd) { throw "Required command not found: $name" }
    Write-Host ("{0,-10} {1}" -f $name, $cmd.Source)
}
& java -version

Step "Building launcher app-image"
Invoke-StepScript (Join-Path $RepoRoot "tools\packaging\windows\build_launcher_app_image.ps1")

Step "Building game app-image"
Invoke-StepScript (Join-Path $RepoRoot "tools\packaging\windows\build_game_app_image.ps1")

Step "Building server app-image"
Invoke-StepScript (Join-Path $RepoRoot "tools\packaging\windows\build_server_app_image.ps1")

if ($BuildInstaller) {
    Step "Building launcher EXE installer"
    Invoke-StepScript (Join-Path $RepoRoot "tools\packaging\windows\build_launcher_installer_exe.ps1")
} else {
    Write-Host "Skipping launcher EXE installer. Use -BuildInstaller after app-images have been smoke-tested."
}

Step "Generating release manifest"
$manifest = Join-Path $OutRoot "release-manifest.json"
$manifestArgs = @(
    (Join-Path $RepoRoot "tools\packaging\generate_release_manifest.py"),
    "--repo-root", $RepoRoot,
    "--output", $manifest,
    "--channel", $Channel,
    "--artifact", (Join-Path $OutRoot "app-image"),
    "--artifact", (Join-Path $OutRoot "game-app-image"),
    "--artifact", (Join-Path $OutRoot "server-app-image")
)
if ($BuildInstaller) { $manifestArgs += @("--artifact", (Join-Path $OutRoot "launcher-installer")) }
python @manifestArgs
if ($LASTEXITCODE -ne 0) { throw "Release manifest generation failed." }

Step "Packaging orchestration complete"
Write-Host "Output:   $OutRoot"
Write-Host "Manifest: $manifest"
Write-Host "Next: smoke-test launcher, game, and server app-images before publishing installer artifacts."
