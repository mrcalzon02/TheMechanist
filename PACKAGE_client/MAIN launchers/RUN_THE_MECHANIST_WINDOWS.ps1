param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GameArgs
)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RuntimeRoot = (Resolve-Path (Join-Path $ScriptDir '..')).Path
$JarPath = Join-Path $RuntimeRoot 'TheMechanist.jar'
$LogDir = Join-Path $env:LOCALAPPDATA 'TheMechanist\logs'
if ([string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
    $LogDir = Join-Path $RuntimeRoot 'logs'
}
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
$LogFile = Join-Path $LogDir 'launch-client.log'

function Write-LogLine([string]$Message) {
    Add-Content -Path $LogFile -Value $Message -Encoding UTF8
}

function Build-Classpath {
    $entries = [System.Collections.Generic.List[string]]::new()
    $entries.Add($JarPath) | Out-Null
    $lib = Join-Path $RuntimeRoot 'lib'
    if (Test-Path -LiteralPath $lib -PathType Container) {
        Get-ChildItem -LiteralPath $lib -Recurse -File -Filter '*.jar' -ErrorAction SilentlyContinue |
            Sort-Object FullName |
            ForEach-Object { $entries.Add($_.FullName) | Out-Null }
    }
    return [string]::Join([System.IO.Path]::PathSeparator, $entries)
}

Set-Content -Path $LogFile -Value '==================================================' -Encoding UTF8
Write-LogLine "The Mechanist segmented Windows client launcher started at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff')"
Write-LogLine "Script dir: $ScriptDir"
Write-LogLine "Runtime root: $RuntimeRoot"
Write-LogLine "Initial working directory: $((Get-Location).Path)"

if (-not (Test-Path -LiteralPath $JarPath -PathType Leaf)) {
    Write-Host "ERROR: TheMechanist.jar was not found at $JarPath"
    Write-LogLine "ERROR: TheMechanist.jar was not found at $JarPath"
    exit 2
}

$java = Get-Command java.exe -ErrorAction SilentlyContinue
if ($null -eq $java) {
    Write-Host 'ERROR: Java 17 or newer was not found on PATH.'
    Write-LogLine 'ERROR: Java was not found on PATH.'
    exit 17
}

# Runtime assets are package-relative. Force the process working directory to the
# package root before preflight or launch so sound, art, settings, profiles, and
# saves resolve identically from double-click launchers, terminals, and packaged
# shortcuts.
Set-Location -LiteralPath $RuntimeRoot
Write-LogLine "Runtime working directory: $((Get-Location).Path)"

$classPath = Build-Classpath
Write-LogLine "Classpath: $classPath"
Write-Host 'Running startup preflight...'
$preflight = & $java.Source -cp $classPath mechanist.WindowsLaunchHealthCheck 2>&1
$preflightExit = $LASTEXITCODE
foreach ($line in $preflight) { Write-LogLine ([string]$line) }
if ($preflightExit -ne 0) {
    Write-Host "ERROR: Startup preflight failed with code $preflightExit. See $LogFile"
    exit $preflightExit
}

Write-Host 'Launching The Mechanist client...'
$output = & $java.Source -Xms512m -Xmx4096m -cp $classPath mechanist.TheMechanist @GameArgs 2>&1
$exitCode = $LASTEXITCODE
foreach ($line in $output) { Write-LogLine ([string]$line) }
Write-LogLine "Process exited with code $exitCode at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff')"
if ($exitCode -ne 0) {
    Write-Host "ERROR: The Mechanist exited with code $exitCode. See $LogFile"
}
exit $exitCode
