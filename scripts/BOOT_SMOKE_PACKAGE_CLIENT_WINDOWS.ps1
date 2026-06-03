param(
    [string]$PackageDir = "PACKAGE_client",
    [int]$Seconds = 8
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$pkg = if ([System.IO.Path]::IsPathRooted($PackageDir)) { $PackageDir } else { Join-Path $root $PackageDir }
$diagRoot = Join-Path $root 'diagnostics'
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$runRoot = Join-Path $diagRoot "boot_smoke_client_$stamp"
New-Item -ItemType Directory -Force -Path $runRoot | Out-Null

$summary = Join-Path $runRoot 'BOOT_SMOKE_SUMMARY.txt'
$stdout = Join-Path $runRoot 'boot_stdout.txt'
$stderr = Join-Path $runRoot 'boot_stderr.txt'
$commandLine = Join-Path $runRoot 'boot_command.txt'

function Publish-LatestBootAliases() {
    if (Test-Path -LiteralPath $summary) { Copy-Item -LiteralPath $summary -Destination (Join-Path $diagRoot 'LATEST_BOOT_SMOKE_SUMMARY.txt') -Force -ErrorAction Continue }
    if (Test-Path -LiteralPath $stdout) { Copy-Item -LiteralPath $stdout -Destination (Join-Path $diagRoot 'LATEST_BOOT_SMOKE_STDOUT.txt') -Force -ErrorAction Continue }
    if (Test-Path -LiteralPath $stderr) { Copy-Item -LiteralPath $stderr -Destination (Join-Path $diagRoot 'LATEST_BOOT_SMOKE_STDERR.txt') -Force -ErrorAction Continue }
    if (Test-Path -LiteralPath $commandLine) { Copy-Item -LiteralPath $commandLine -Destination (Join-Path $diagRoot 'LATEST_BOOT_SMOKE_COMMAND.txt') -Force -ErrorAction Continue }
}

function Write-SummaryStatus([string] $Message) {
    Write-Host $Message
    Add-Content -LiteralPath $summary -Value $Message
}

"Boot Smoke Run: $stamp" | Set-Content -LiteralPath $summary -Encoding UTF8
"PackageDir: $pkg" | Add-Content -LiteralPath $summary
"TimeoutSeconds: $Seconds" | Add-Content -LiteralPath $summary

if (-not (Test-Path -LiteralPath $pkg -PathType Container)) {
    Write-SummaryStatus "BOOT_SMOKE_FAIL: package directory missing"
    Publish-LatestBootAliases
    exit 2
}

$classes = Join-Path $pkg 'classes'
if (-not (Test-Path -LiteralPath $classes -PathType Container)) {
    Write-SummaryStatus "BOOT_SMOKE_FAIL: classes directory missing"
    Publish-LatestBootAliases
    exit 3
}

$mainClass = Join-Path $classes 'mechanist\TheMechanist.class'
if (-not (Test-Path -LiteralPath $mainClass -PathType Leaf)) {
    Write-SummaryStatus "BOOT_SMOKE_FAIL: mechanist\TheMechanist.class missing"
    Publish-LatestBootAliases
    exit 4
}

$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    Write-SummaryStatus "BOOT_SMOKE_FAIL: java not found on PATH"
    Publish-LatestBootAliases
    exit 5
}

$javaPath = if ($java.Source) { $java.Source } else { $java.Name }
$argLine = '-Dmechanist.assetRoot=. -Dmechanist.generatedAssetRoot=. -Dmechanist.assetTier=low_32 -Dmechanist.assetResolution=32 -cp "classes;." mechanist.TheMechanist'
"$javaPath $argLine" | Set-Content -LiteralPath $commandLine
"Java: $javaPath" | Add-Content -LiteralPath $summary
"Command: $argLine" | Add-Content -LiteralPath $summary

try {
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = 'cmd.exe'
    $escapedJavaPath = '"' + ($javaPath -replace '"', '\"') + '"'
    $escapedStdout = '"' + ($stdout -replace '"', '\"') + '"'
    $escapedStderr = '"' + ($stderr -replace '"', '\"') + '"'
    $startInfo.Arguments = '/d /s /c "' + $escapedJavaPath + ' -Dmechanist.assetRoot=. -Dmechanist.generatedAssetRoot=. -Dmechanist.assetTier=low_32 -Dmechanist.assetResolution=32 -cp "classes;." mechanist.TheMechanist > ' + $escapedStdout + ' 2> ' + $escapedStderr + '"'
    $startInfo.WorkingDirectory = $pkg
    $startInfo.RedirectStandardOutput = $false
    $startInfo.RedirectStandardError = $false
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $proc = New-Object System.Diagnostics.Process
    $proc.StartInfo = $startInfo
    [void] $proc.Start()
    "ProcessId: $($proc.Id)" | Add-Content -LiteralPath $summary
    $exited = $proc.WaitForExit([Math]::Max(1, $Seconds) * 1000)
    if ($exited) {
        "Exited: true" | Add-Content -LiteralPath $summary
        "ExitCode: $($proc.ExitCode)" | Add-Content -LiteralPath $summary
        if ($proc.ExitCode -eq 0) {
            Write-SummaryStatus "BOOT_SMOKE_PASS: client exited cleanly before timeout"
            Publish-LatestBootAliases
            exit 0
        }
        Write-SummaryStatus "BOOT_SMOKE_FAIL: client exited before timeout with nonzero code $($proc.ExitCode)"
        Publish-LatestBootAliases
        exit $proc.ExitCode
    } else {
        "Exited: false" | Add-Content -LiteralPath $summary
        "Interpretation: process survived timeout; GUI boot likely reached or is awaiting user interaction" | Add-Content -LiteralPath $summary
        try {
            Stop-Process -Id $proc.Id -Force -ErrorAction Continue
            [void] $proc.WaitForExit(3000)
        } catch {}
        Write-SummaryStatus "BOOT_SMOKE_PASS_TIMEOUT: client stayed alive for $Seconds second(s); killed after smoke window"
        Publish-LatestBootAliases
        exit 0
    }
} catch {
    Write-SummaryStatus "BOOT_SMOKE_FAIL: $($_.Exception.Message)"
    Publish-LatestBootAliases
    exit 10
}


