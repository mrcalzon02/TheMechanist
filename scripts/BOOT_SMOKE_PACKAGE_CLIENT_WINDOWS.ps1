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
    Copy-Item -LiteralPath $summary -Destination (Join-Path $diagRoot 'LATEST_BOOT_SMOKE_SUMMARY.txt') -Force -ErrorAction Continue
    Copy-Item -LiteralPath $stdout -Destination (Join-Path $diagRoot 'LATEST_BOOT_SMOKE_STDOUT.txt') -Force -ErrorAction Continue
    Copy-Item -LiteralPath $stderr -Destination (Join-Path $diagRoot 'LATEST_BOOT_SMOKE_STDERR.txt') -Force -ErrorAction Continue
    Copy-Item -LiteralPath $commandLine -Destination (Join-Path $diagRoot 'LATEST_BOOT_SMOKE_COMMAND.txt') -Force -ErrorAction Continue
}

"Boot Smoke Run: $stamp" | Set-Content -LiteralPath $summary
"PackageDir: $pkg" | Add-Content -LiteralPath $summary
"TimeoutSeconds: $Seconds" | Add-Content -LiteralPath $summary

if (-not (Test-Path -LiteralPath $pkg -PathType Container)) {
    "BOOT_SMOKE_FAIL: package directory missing" | Tee-Object -FilePath $summary -Append
    Publish-LatestBootAliases
    exit 2
}

$classes = Join-Path $pkg 'classes'
if (-not (Test-Path -LiteralPath $classes -PathType Container)) {
    "BOOT_SMOKE_FAIL: classes directory missing" | Tee-Object -FilePath $summary -Append
    Publish-LatestBootAliases
    exit 3
}

$mainClass = Join-Path $classes 'mechanist\TheMechanist.class'
if (-not (Test-Path -LiteralPath $mainClass -PathType Leaf)) {
    "BOOT_SMOKE_FAIL: mechanist\TheMechanist.class missing" | Tee-Object -FilePath $summary -Append
    Publish-LatestBootAliases
    exit 4
}

$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    "BOOT_SMOKE_FAIL: java not found on PATH" | Tee-Object -FilePath $summary -Append
    Publish-LatestBootAliases
    exit 5
}

$javaPath = if ($java.Source) { $java.Source } else { $java.Name }
$argLine = '-Dmechanist.assetRoot=. -Dmechanist.generatedAssetRoot=. -Dmechanist.assetTier=low_32 -Dmechanist.assetResolution=32 -cp "classes;." mechanist.TheMechanist'
"$javaPath $argLine" | Set-Content -LiteralPath $commandLine
"Java: $javaPath" | Add-Content -LiteralPath $summary
"Command: $argLine" | Add-Content -LiteralPath $summary

try {
    $proc = Start-Process -FilePath $javaPath -ArgumentList @('-Dmechanist.assetRoot=.', '-Dmechanist.generatedAssetRoot=.', '-Dmechanist.assetTier=low_32', '-Dmechanist.assetResolution=32', '-cp', 'classes;.', 'mechanist.TheMechanist') -WorkingDirectory $pkg -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru -WindowStyle Normal
    "ProcessId: $($proc.Id)" | Add-Content -LiteralPath $summary
    $exited = $proc.WaitForExit([Math]::Max(1, $Seconds) * 1000)
    if ($exited) {
        "Exited: true" | Add-Content -LiteralPath $summary
        "ExitCode: $($proc.ExitCode)" | Add-Content -LiteralPath $summary
        if ($proc.ExitCode -eq 0) {
            "BOOT_SMOKE_PASS: client exited cleanly before timeout" | Tee-Object -FilePath $summary -Append
            Publish-LatestBootAliases
            exit 0
        }
        "BOOT_SMOKE_FAIL: client exited before timeout with nonzero code $($proc.ExitCode)" | Tee-Object -FilePath $summary -Append
        Publish-LatestBootAliases
        exit $proc.ExitCode
    } else {
        "Exited: false" | Add-Content -LiteralPath $summary
        "Interpretation: process survived timeout; GUI boot likely reached or is awaiting user interaction" | Add-Content -LiteralPath $summary
        try { Stop-Process -Id $proc.Id -Force -ErrorAction Continue } catch {}
        "BOOT_SMOKE_PASS_TIMEOUT: client stayed alive for $Seconds second(s); killed after smoke window" | Tee-Object -FilePath $summary -Append
        Publish-LatestBootAliases
        exit 0
    }
} catch {
    "BOOT_SMOKE_FAIL: $($_.Exception.Message)" | Tee-Object -FilePath $summary -Append
    Publish-LatestBootAliases
    exit 10
}


