param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GameArgs
)

$ErrorActionPreference = 'Stop'
$AppName = 'The Mechanist Server'
$JarName = 'TheMechanistServer.jar'
$LogName = 'launch-server.log'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location -LiteralPath $Root

$local = $env:LOCALAPPDATA
if ([string]::IsNullOrWhiteSpace($local)) {
    $LogDir = Join-Path $Root 'logs'
} else {
    $LogDir = Join-Path $local 'TheMechanist\logs'
}
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
$LogFile = Join-Path $LogDir $LogName

function Write-LogLine {
    param([AllowNull()][string] $Message)
    if ($null -eq $Message) { $Message = '' }
    Add-Content -Path $LogFile -Value $Message -Encoding UTF8
}

function Convert-JavaMajorVersion {
    param([string] $VersionText)
    if ([string]::IsNullOrWhiteSpace($VersionText)) { return -1 }
    $clean = $VersionText.Trim().Trim('"')
    if ($clean.StartsWith('1.')) {
        $parts = $clean.Split('.')
        if ($parts.Length -ge 2) {
            $major = 0
            if ([int]::TryParse(($parts[1] -replace '[^0-9].*$', ''), [ref]$major)) { return $major }
        }
        return -1
    }
    $head = ($clean -replace '[^0-9].*$', '')
    $major2 = 0
    if ([int]::TryParse($head, [ref]$major2)) { return $major2 }
    return -1
}

function Invoke-JavaVersionProbe {
    param([string] $JavaExe)
    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = $JavaExe
    $psi.Arguments = '-version'
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $process = $null
    try {
        $process = [System.Diagnostics.Process]::Start($psi)
        $stdout = $process.StandardOutput.ReadToEnd()
        $stderr = $process.StandardError.ReadToEnd()
        if (-not $process.WaitForExit(10000)) {
            try { $process.Kill() } catch { }
            return [pscustomobject]@{
                ExitCode = -10000
                Output = 'java -version timed out after 10 seconds'
            }
        }
        return [pscustomobject]@{
            ExitCode = $process.ExitCode
            Output = (($stdout + [Environment]::NewLine + $stderr).Trim())
        }
    } catch {
        return [pscustomobject]@{
            ExitCode = -9999
            Output = $_.Exception.Message
        }
    } finally {
        if ($null -ne $process) { $process.Dispose() }
    }
}

function Get-JavaInfo {
    param([string] $JavaExe)
    if ([string]::IsNullOrWhiteSpace($JavaExe)) { return $null }
    if (-not (Test-Path -LiteralPath $JavaExe -PathType Leaf)) { return $null }
    $resolved = (Resolve-Path -LiteralPath $JavaExe).Path
    $probe = Invoke-JavaVersionProbe $resolved
    $version = $null
    foreach ($line in ($probe.Output -split "`r?`n")) {
        $text = [string]$line
        if ($text -match 'java version "(.+?)"') {
            $version = $Matches[1]
            break
        }
        if ($text -match 'openjdk version "(.+?)"') {
            $version = $Matches[1]
            break
        }
        if ($text -match '^\s*java\.version\s*=\s*(.+?)\s*$') {
            $version = $Matches[1]
            break
        }
    }
    $major = Convert-JavaMajorVersion $version
    [pscustomobject]@{
        Path = $resolved
        Version = if ($version) { $version } else { 'unknown' }
        Major = $major
        ExitCode = $probe.ExitCode
        Output = $probe.Output
    }
}

function Add-CandidatePath {
    param(
        [System.Collections.Generic.List[string]] $List,
        [string] $PathText
    )
    if ([string]::IsNullOrWhiteSpace($PathText)) { return }
    if (-not (Test-Path -LiteralPath $PathText -PathType Leaf)) { return }
    $resolved = (Resolve-Path -LiteralPath $PathText).Path
    foreach ($existing in $List) {
        if ([string]::Equals($existing, $resolved, [System.StringComparison]::OrdinalIgnoreCase)) { return }
    }
    $List.Add($resolved) | Out-Null
}

function Add-GlobCandidatePaths {
    param(
        [System.Collections.Generic.List[string]] $List,
        [string[]] $Patterns
    )
    foreach ($pattern in $Patterns) {
        if ([string]::IsNullOrWhiteSpace($pattern)) { continue }
        try {
            foreach ($hit in Get-ChildItem -Path $pattern -ErrorAction SilentlyContinue) {
                Add-CandidatePath $List $hit.FullName
            }
        } catch {
            # Directory probing is best-effort; failure here should not stop launch discovery.
        }
    }
}

function Find-Java17OrNewer {
    $candidates = [System.Collections.Generic.List[string]]::new()
    Add-CandidatePath $candidates (Join-Path $Root 'runtime\bin\java.exe')
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        Add-CandidatePath $candidates (Join-Path $env:JAVA_HOME 'bin\java.exe')
    }

    $programRoots = @($env:ProgramFiles, ${env:ProgramFiles(x86)}) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    foreach ($base in $programRoots) {
        Add-GlobCandidatePaths $candidates @(
            (Join-Path $base 'Eclipse Adoptium\jdk-*\bin\java.exe'),
            (Join-Path $base 'Eclipse Adoptium\jre-*\bin\java.exe'),
            (Join-Path $base 'Java\jdk-*\bin\java.exe'),
            (Join-Path $base 'Java\jre-*\bin\java.exe'),
            (Join-Path $base 'Microsoft\jdk-*\bin\java.exe'),
            (Join-Path $base 'Amazon Corretto\jdk*\bin\java.exe'),
            (Join-Path $base 'BellSoft\LibericaJDK-*\bin\java.exe'),
            (Join-Path $base 'BellSoft\LibericaJRE-*\bin\java.exe'),
            (Join-Path $base 'Zulu\zulu-*\bin\java.exe'),
            (Join-Path $base 'ojdkbuild\java-*\bin\java.exe')
        )
    }

    try {
        foreach ($cmd in Get-Command java.exe -All -ErrorAction SilentlyContinue) {
            Add-CandidatePath $candidates $cmd.Source
        }
    } catch {
        # PATH probing is best-effort.
    }

    $seen = @()
    foreach ($candidate in $candidates) {
        $info = Get-JavaInfo $candidate
        if ($null -eq $info) { continue }
        $seen += $info
        if ($info.Major -ge 17) {
            return [pscustomobject]@{ Selected = $info; Seen = $seen }
        }
    }
    return [pscustomobject]@{ Selected = $null; Seen = $seen }
}

Set-Content -Path $LogFile -Value '==================================================' -Encoding UTF8
Write-LogLine "$AppName Windows launcher started at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff')"
Write-LogLine "Working directory: $Root"
Write-LogLine "Launcher path: $($MyInvocation.MyCommand.Path)"
Write-LogLine ''
Write-Host "Starting $AppName..."
Write-Host "Launch log: $LogFile"
Write-Host ''

$jar = Join-Path $Root $JarName
if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) {
    Write-LogLine "ERROR: $JarName was not found next to this launcher."
    Write-Host "ERROR: $JarName was not found next to this launcher."
    Write-Host 'The game zip must be fully extracted before running. Do not run this batch from inside the compressed zip preview.'
    Write-Host "Expected location: $jar"
    exit 2
}

$result = Find-Java17OrNewer
Write-LogLine 'Java candidates checked:'
if ($result.Seen.Count -eq 0) {
    Write-LogLine '  (none found)'
} else {
    foreach ($candidate in $result.Seen) {
        Write-LogLine "  $($candidate.Path) :: version=$($candidate.Version) :: major=$($candidate.Major) :: exit=$($candidate.ExitCode)"
        if ($candidate.Major -lt 17) {
            $firstLine = (($candidate.Output -split "`r?`n") | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -First 1)
            if (-not [string]::IsNullOrWhiteSpace($firstLine)) {
                Write-LogLine "    probe: $firstLine"
            }
        }
    }
}

if ($null -eq $result.Selected) {
    Write-LogLine ''
    Write-LogLine 'ERROR: Java 17 or newer was not found. Java 8 cannot run this game.'
    Write-Host 'ERROR: Java 17 or newer was not found.'
    Write-Host 'The launcher checked installed Java runtimes but did not find a readable Java 17+ runtime.'
    Write-Host 'Install a 64-bit Java 17+ runtime, then run this launcher again.'
    Write-Host 'Recommended: Eclipse Temurin / Adoptium Java 17 or newer for Windows x64.'
    Write-Host 'Details were written to:'
    Write-Host "  $LogFile"
    exit 17
}

$javaExe = $result.Selected.Path
Write-LogLine ''
Write-LogLine "Using Java: $javaExe"
Write-LogLine "Detected Java version: $($result.Selected.Version)"
Write-Host "Using Java: $javaExe"
Write-Host "Detected Java version: $($result.Selected.Version)"
Write-Host ''

Write-Host 'Running startup preflight...'
$preflightOutput = & $javaExe -cp $jar mechanist.WindowsLaunchHealthCheck 2>&1
$preflightExit = $LASTEXITCODE
foreach ($line in $preflightOutput) { Write-LogLine ([string]$line) }
if ($preflightExit -ne 0) {
    Write-LogLine "ERROR: Startup preflight failed with code $preflightExit."
    Write-Host "ERROR: Startup preflight failed with code $preflightExit."
    Write-Host "See log: $LogFile"
    exit $preflightExit
}

Write-Host "Preflight OK. Launching $AppName..."
Write-LogLine ''
Write-LogLine 'Launching jar...'
$processOutput = & $javaExe -Xms512m -Xmx4096m -jar $jar @GameArgs 2>&1
$processExit = $LASTEXITCODE
foreach ($line in $processOutput) { Write-LogLine ([string]$line) }
Write-LogLine ''
Write-LogLine "Process exited with code $processExit at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff')"

if ($processExit -ne 0) {
    Write-Host "ERROR: $AppName exited with code $processExit."
    Write-Host "See log: $LogFile"
    exit $processExit
}
exit 0
