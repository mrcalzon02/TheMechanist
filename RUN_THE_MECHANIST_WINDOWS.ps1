param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GameArgs
)

$ErrorActionPreference = 'Stop'
$AppName = 'The Mechanist'
$JarName = 'TheMechanist.jar'
$LogName = 'launch-client.log'
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


function Build-MechanistClasspath {
    param([string] $RootPath, [string] $JarPath)
    $entries = [System.Collections.Generic.List[string]]::new()
    $entries.Add($JarPath) | Out-Null
    $libRoot = Join-Path $RootPath 'lib'
    if (Test-Path -LiteralPath $libRoot -PathType Container) {
        try {
            Get-ChildItem -LiteralPath $libRoot -Recurse -File -Filter '*.jar' -ErrorAction SilentlyContinue |
                Sort-Object FullName |
                ForEach-Object { $entries.Add($_.FullName) | Out-Null }
        } catch {
            Write-LogLine "WARNING: Could not scan lib directory for dependency jars: $($_.Exception.Message)"
        }
    }
    return [string]::Join([System.IO.Path]::PathSeparator, $entries)
}

function Write-DependencyClasspathReport {
    param([string] $RootPath)
    $libRoot = Join-Path $RootPath 'lib'
    if (-not (Test-Path -LiteralPath $libRoot -PathType Container)) {
        Write-LogLine 'Runtime dependency lib directory: missing'
        return
    }
    $jars = @(Get-ChildItem -LiteralPath $libRoot -Recurse -File -Filter '*.jar' -ErrorAction SilentlyContinue | Sort-Object FullName)
    if ($jars.Count -eq 0) {
        Write-LogLine 'Runtime dependency jars: none found under lib/'
        return
    }
    Write-LogLine 'Runtime dependency jars:'
    foreach ($jarFile in $jars) {
        $relative = $jarFile.FullName.Substring($RootPath.Length).TrimStart('\','/')
        Write-LogLine "  $relative :: $($jarFile.Length) bytes"
    }
}


$LwjglVersion = '3.4.1'
$LwjglRepository = 'https://repo1.maven.org/maven2'
$LwjglModules = @('lwjgl', 'lwjgl-glfw', 'lwjgl-opengl', 'lwjgl-stb')

function Test-MechanistValidJar {
    param([string] $Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { return $false }
    if ((Get-Item -LiteralPath $Path).Length -lt 128) { return $false }
    $fs = [System.IO.File]::OpenRead($Path)
    try {
        $bytes = New-Object byte[] 4
        $read = $fs.Read($bytes, 0, 4)
        return ($read -eq 4 -and $bytes[0] -eq 0x50 -and $bytes[1] -eq 0x4B -and $bytes[2] -eq 0x03 -and $bytes[3] -eq 0x04)
    } catch {
        return $false
    } finally {
        if ($null -ne $fs) { $fs.Dispose() }
    }
}

function Get-MechanistLwjglArtifacts {
    $artifacts = [System.Collections.Generic.List[object]]::new()
    foreach ($module in $LwjglModules) {
        $file = "$module-$LwjglVersion.jar"
        $url = "$LwjglRepository/org/lwjgl/$module/$LwjglVersion/$file"
        $artifacts.Add([pscustomobject]@{ File = $file; Url = $url }) | Out-Null
    }
    foreach ($module in $LwjglModules) {
        $file = "$module-$LwjglVersion-natives-windows.jar"
        $url = "$LwjglRepository/org/lwjgl/$module/$LwjglVersion/$file"
        $artifacts.Add([pscustomobject]@{ File = $file; Url = $url }) | Out-Null
    }
    return $artifacts
}

function Ensure-MechanistLwjglRuntime {
    param([string] $RootPath)
    if ([string]::Equals($env:MECHANIST_DISABLE_LWJGL_BOOTSTRAP, 'true', [System.StringComparison]::OrdinalIgnoreCase)) {
        Write-LogLine 'LWJGL bootstrap disabled by MECHANIST_DISABLE_LWJGL_BOOTSTRAP=true.'
        return
    }
    $libDir = Join-Path $RootPath 'lib\lwjgl'
    New-Item -ItemType Directory -Force -Path $libDir | Out-Null
    Write-LogLine "LWJGL bootstrap: version=$LwjglVersion platform=windows target=$libDir"
    foreach ($artifact in (Get-MechanistLwjglArtifacts)) {
        $target = Join-Path $libDir $artifact.File
        if (Test-MechanistValidJar $target) {
            Write-LogLine "LWJGL present: lib/lwjgl/$($artifact.File)"
            continue
        }
        Write-Host "Installing optional LWJGL runtime: $($artifact.File)"
        Write-LogLine "LWJGL download: $($artifact.Url)"
        $tmp = "$target.tmp"
        Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
        try {
            Invoke-WebRequest -Uri $artifact.Url -OutFile $tmp -UseBasicParsing
            if (-not (Test-MechanistValidJar $tmp)) {
                throw "Downloaded file is not a valid jar."
            }
            Move-Item -LiteralPath $tmp -Destination $target -Force
            Write-LogLine "LWJGL installed: lib/lwjgl/$($artifact.File)"
        } catch {
            Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
            Write-LogLine "ERROR: LWJGL bootstrap failed for $($artifact.File): $($_.Exception.Message)"
            Write-Host "ERROR: Could not install optional LWJGL runtime jar: $($artifact.File)"
            Write-Host 'The launcher could not install optional rendering runtime libraries. Connect to the internet or pre-populate lib\lwjgl, then run again.'
            Write-Host "See log: $LogFile"
            exit 23
        }
    }
    Write-LogLine 'LWJGL bootstrap complete.'
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

Ensure-MechanistLwjglRuntime $Root
$classPath = Build-MechanistClasspath $Root $jar
Write-DependencyClasspathReport $Root
Write-LogLine "Classpath: $classPath"
Write-Host 'Running startup preflight...'
$requireLwjglJvmArg = '-Dmechanist.requireLwjgl=true'
$preflightArgs = @($requireLwjglJvmArg, '-cp', $classPath, 'mechanist.WindowsLaunchHealthCheck')
Write-LogLine ('Preflight Java args: ' + ($preflightArgs -join ' | '))
$preflightOutput = & $javaExe @preflightArgs 2>&1
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
Write-LogLine 'Launching client through explicit classpath so bundled libraries in lib/ are visible...'
$clientArgs = @($requireLwjglJvmArg, '-Xms512m', '-Xmx4096m', '-cp', $classPath, 'mechanist.TheMechanist')
if ($null -ne $GameArgs -and $GameArgs.Count -gt 0) { $clientArgs += $GameArgs }
Write-LogLine ('Client Java args: ' + ($clientArgs -join ' | '))
$processOutput = & $javaExe @clientArgs 2>&1
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
