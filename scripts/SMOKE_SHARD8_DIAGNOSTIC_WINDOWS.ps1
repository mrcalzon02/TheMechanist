param(
    [switch]$SkipRun,
    [switch]$VerboseJava
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$rootFull = [System.IO.Path]::GetFullPath($root).TrimEnd('\', '/')
$rootPrefix = $rootFull + [System.IO.Path]::DirectorySeparatorChar
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$diagRoot = Join-Path $root 'diagnostics'
$runRoot = Join-Path $diagRoot "shard8_smoke_$stamp"
New-Item -ItemType Directory -Force -Path $runRoot | Out-Null

$summary = Join-Path $runRoot 'SUMMARY.txt'
$compileLog = Join-Path $runRoot 'compile.log'
$javacLog = Join-Path $runRoot 'javac_filelist.log'
$sourceList = Join-Path $runRoot 'sources.txt'
$javacSourceArgs = Join-Path $runRoot 'javac_sources.args'
$envLog = Join-Path $runRoot 'environment.log'
$gitLog = Join-Path $runRoot 'git_state.log'
New-Item -ItemType File -Force -Path $compileLog | Out-Null

function Write-Section($name) {
    $line = "==== $name ===="
    Add-Content -LiteralPath $summary -Value "`r`n$line"
    Write-Host $line
}

function Run-Captured($name, $scriptBlock, $logPath) {
    Write-Section $name
    Add-Content -LiteralPath $summary -Value "Log: $logPath"
    try {
        & $scriptBlock *>&1 | Tee-Object -FilePath $logPath
        $code = if ($LASTEXITCODE -ne $null) { $LASTEXITCODE } else { 0 }
        Add-Content -LiteralPath $summary -Value "ExitCode: $code"
        return $code
    } catch {
        $_ | Out-String | Tee-Object -FilePath $logPath
        Add-Content -LiteralPath $summary -Value "Exception: $($_.Exception.Message)"
        return 999
    }
}

function Convert-ToJavacRelativePath($sourcePath) {
    $full = [System.IO.Path]::GetFullPath([string]$sourcePath)
    if ($full.StartsWith($rootPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        $relative = $full.Substring($rootPrefix.Length)
    } else {
        $relative = $full
    }
    $relative = $relative -replace '\\', '/'
    return '"' + ($relative -replace '"', '\"') + '"'
}

function Write-Utf8NoBomLines($path, $lines) {
    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines($path, [string[]]$lines, $encoding)
}

"Shard 8 Smoke Diagnostic Run: $stamp" | Set-Content -LiteralPath $summary
"Repository root: $rootFull" | Add-Content -LiteralPath $summary
"Run folder: $runRoot" | Add-Content -LiteralPath $summary

Run-Captured 'Environment' {
    Write-Host "PowerShell: $($PSVersionTable.PSVersion)"
    Write-Host "OS: $([System.Environment]::OSVersion.VersionString)"
    Write-Host "ProcessArch: $([System.Runtime.InteropServices.RuntimeInformation]::ProcessArchitecture)"
    Write-Host "CurrentDir: $(Get-Location)"
    java -version
    if (Get-Command javac -ErrorAction SilentlyContinue) { javac -version }
    if (Get-Command mvn -ErrorAction SilentlyContinue) { mvn -version }
    if (Get-Command git -ErrorAction SilentlyContinue) { git --version }
} $envLog | Out-Null

Run-Captured 'Git state' {
    if (Get-Command git -ErrorAction SilentlyContinue) {
        git -C $root rev-parse --show-toplevel
        git -C $root rev-parse HEAD
        git -C $root status --short
    } else {
        Write-Host 'git not available'
    }
} $gitLog | Out-Null

Write-Section 'Source inventory'
$sources = @(Get-ChildItem -LiteralPath (Join-Path $root 'src') -Recurse -Filter '*.java' | Sort-Object FullName | ForEach-Object { $_.FullName })
$sources | Set-Content -LiteralPath $sourceList
$javacArgLines = @($sources | ForEach-Object { Convert-ToJavacRelativePath $_ })
Write-Utf8NoBomLines $javacSourceArgs $javacArgLines
$count = $sources.Count
"Java source files: $count" | Add-Content -LiteralPath $summary
"Source list: $sourceList" | Add-Content -LiteralPath $summary
"Javac response file: $javacSourceArgs" | Add-Content -LiteralPath $summary

if ($SkipRun) {
    Write-Section 'Skipped compile by request'
    exit 0
}

$compileExit = 999
$pomPath = Join-Path $root 'pom.xml'
$hasPom = Test-Path -LiteralPath $pomPath -PathType Leaf
$hasMaven = [bool](Get-Command mvn -ErrorAction SilentlyContinue)
$hasJavac = [bool](Get-Command javac -ErrorAction SilentlyContinue)
if ($hasPom -and $hasMaven) {
    $compileExit = Run-Captured 'Maven compile smoke' { mvn -f $pomPath -DskipTests compile } $compileLog
} elseif ($hasJavac) {
    $classes = Join-Path $runRoot 'classes'
    New-Item -ItemType Directory -Force -Path $classes | Out-Null
    $tempArgFile = Join-Path ([System.IO.Path]::GetTempPath()) "mechanist_javac_sources_$stamp.args"
    Write-Utf8NoBomLines $tempArgFile $javacArgLines
    $responseArg = '@' + $tempArgFile
    $javacArgs = @('-encoding', 'UTF-8', '-d', $classes, $responseArg)
    if ($VerboseJava) { $javacArgs = @('-verbose') + $javacArgs }
    $compileExit = Run-Captured 'Javac compile smoke' {
        Push-Location $root
        try {
            Write-Host ('Working directory: ' + (Get-Location))
            Write-Host ('javac ' + ($javacArgs -join ' '))
            Write-Host ('Temp response file: ' + $tempArgFile)
            Write-Host ('First five javac source args:')
            $javacArgLines | Select-Object -First 5 | ForEach-Object { Write-Host $_ }
            & javac @javacArgs
        } finally {
            Pop-Location
            if (Test-Path -LiteralPath $tempArgFile) { Remove-Item -LiteralPath $tempArgFile -Force }
        }
    } $compileLog
} else {
    Write-Section 'Compile unavailable'
    'Neither Maven nor javac is available on PATH.' | Tee-Object -FilePath $compileLog
    $compileExit = 127
}

Write-Section 'Compile error extraction'
$errors = Join-Path $runRoot 'compile_errors.tsv'
& (Join-Path $PSScriptRoot 'EXTRACT_SMOKE_COMPILE_ERRORS_WINDOWS.ps1') -LogPath $compileLog -OutputPath $errors | Tee-Object -FilePath $javacLog
"Error table: $errors" | Add-Content -LiteralPath $summary
"Final compile exit: $compileExit" | Add-Content -LiteralPath $summary

if ($compileExit -eq 0) {
    Write-Host "SMOKE PASS: compile succeeded. Logs: $runRoot"
} else {
    Write-Host "SMOKE FAIL: compile failed with exit $compileExit. Logs: $runRoot"
}

exit $compileExit
