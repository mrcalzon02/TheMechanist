param(
    [switch]$SkipCompile,
    [switch]$VerboseJava,
    [switch]$AllowArchivedShardCopies,
    [int]$CommandTimeoutSeconds = 900
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$rootFull = [System.IO.Path]::GetFullPath($root).TrimEnd('\', '/')
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$diagRoot = Join-Path $root 'diagnostics'
$runRoot = Join-Path $diagRoot "function_ops_smoke_$stamp"
New-Item -ItemType Directory -Force -Path $runRoot | Out-Null

$summary = Join-Path $runRoot 'SUMMARY.txt'
$opsLog = Join-Path $runRoot 'operations.log'
$gateTable = Join-Path $runRoot 'operation_gates.tsv'
$mermaidEvalCopy = Join-Path $runRoot 'CODE_MERMAID_EVALUATION.tsv'
$functionSummaryCopy = Join-Path $runRoot 'FUNCTION_MAP_SUMMARY.md'
$compileRunLog = Join-Path $runRoot 'compile_smoke_wrapper.log'

$gateRows = New-Object System.Collections.Generic.List[string]
$gateRows.Add("severity`tgate`tstatus`tmessage") | Out-Null

function Write-Section($name) {
    $line = "==== $name ===="
    Add-Content -LiteralPath $summary -Value "`r`n$line"
    Add-Content -LiteralPath $opsLog -Value "`r`n$line"
    Write-Host $line
}

function Add-Gate($severity, $gate, $status, $message) {
    $safeMessage = ([string]$message) -replace "`t", ' ' -replace "`r?`n", ' '
    $row = "$severity`t$gate`t$status`t$safeMessage"
    $gateRows.Add($row) | Out-Null
    Add-Content -LiteralPath $summary -Value "$severity`t$gate`t$status`t$safeMessage"
    if ($severity -eq 'ERROR') { Write-Host "ERROR [$gate] $safeMessage" -ForegroundColor Red }
    elseif ($severity -eq 'WARN') { Write-Host "WARN  [$gate] $safeMessage" -ForegroundColor Yellow }
    else { Write-Host "OK    [$gate] $safeMessage" -ForegroundColor Green }
}

function Run-ProcessCaptured($name, $exe, [string[]]$argList, $logPath, [int]$timeoutSeconds) {
    Write-Section $name
    Add-Content -LiteralPath $summary -Value "Log: $logPath"
    Add-Content -LiteralPath $summary -Value "TimeoutSeconds: $timeoutSeconds"
    $stdout = "$logPath.stdout.tmp"
    $stderr = "$logPath.stderr.tmp"
    Remove-Item -LiteralPath $stdout, $stderr -Force -ErrorAction SilentlyContinue
    try {
        $p = Start-Process -FilePath $exe -ArgumentList $argList -NoNewWindow -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
        $finished = $p.WaitForExit([Math]::Max(1, $timeoutSeconds) * 1000)
        if (-not $finished) {
            try { $p.Kill($true) } catch { try { $p.Kill() } catch {} }
            "TIMEOUT after $timeoutSeconds seconds: $exe $($argList -join ' ')" | Tee-Object -FilePath $logPath
            if (Test-Path -LiteralPath $stdout) { Get-Content -LiteralPath $stdout -ErrorAction SilentlyContinue | Add-Content -LiteralPath $logPath }
            if (Test-Path -LiteralPath $stderr) { Get-Content -LiteralPath $stderr -ErrorAction SilentlyContinue | Add-Content -LiteralPath $logPath }
            Add-Content -LiteralPath $summary -Value "ExitCode: 124"
            return 124
        }
        if (Test-Path -LiteralPath $stdout) { Get-Content -LiteralPath $stdout -ErrorAction SilentlyContinue | Tee-Object -FilePath $logPath }
        if (Test-Path -LiteralPath $stderr) { Get-Content -LiteralPath $stderr -ErrorAction SilentlyContinue | Tee-Object -FilePath $logPath -Append }
        $code = $p.ExitCode
        Add-Content -LiteralPath $summary -Value "ExitCode: $code"
        return $code
    } catch {
        $_ | Out-String | Tee-Object -FilePath $logPath
        Add-Content -LiteralPath $summary -Value "Exception: $($_.Exception.Message)"
        return 999
    } finally {
        Remove-Item -LiteralPath $stdout, $stderr -Force -ErrorAction SilentlyContinue
    }
}

"Function Map Operations Smoke: $stamp" | Set-Content -LiteralPath $summary
"Repository root: $rootFull" | Add-Content -LiteralPath $summary
"Run folder: $runRoot" | Add-Content -LiteralPath $summary
"Command timeout seconds: $CommandTimeoutSeconds" | Add-Content -LiteralPath $summary
"Function Map Operations Smoke: $stamp" | Set-Content -LiteralPath $opsLog

Write-Section 'Tooling preflight'
$hasPython = [bool](Get-Command py -ErrorAction SilentlyContinue)
$hasGit = [bool](Get-Command git -ErrorAction SilentlyContinue)
$hasJava = [bool](Get-Command java -ErrorAction SilentlyContinue)
$hasJavac = [bool](Get-Command javac -ErrorAction SilentlyContinue)
if ($hasPython) { Add-Gate 'INFO' 'python_launcher' 'present' 'py launcher found' } else { Add-Gate 'ERROR' 'python_launcher' 'missing' 'py launcher is required for map builders' }
if ($hasGit) { Add-Gate 'INFO' 'git' 'present' 'git found' } else { Add-Gate 'WARN' 'git' 'missing' 'git not found; commit-state reporting unavailable' }
if ($hasJava) { Add-Gate 'INFO' 'java' 'present' 'java found' } else { Add-Gate 'WARN' 'java' 'missing' 'java not found on PATH' }
if ($hasJavac) { Add-Gate 'INFO' 'javac' 'present' 'javac found' } else { Add-Gate 'WARN' 'javac' 'missing' 'javac not found; Maven may still compile if available' }

Write-Section 'Retired shard and GamePanel gates'
$gamePanel = Join-Path $root 'src\mechanist\GamePanel.java'
if (Test-Path -LiteralPath $gamePanel -PathType Leaf) {
    $gpItem = Get-Item -LiteralPath $gamePanel
    if ($gpItem.Length -eq 0) {
        Add-Gate 'WARN' 'gamepanel_shell' 'empty_tracked_shell' 'GamePanel.java exists but is empty; local git rm is still expected.'
    } else {
        Add-Gate 'ERROR' 'gamepanel_shell' 'active_content' "GamePanel.java still has $($gpItem.Length) bytes; active monolith content remains."
    }
} else {
    Add-Gate 'INFO' 'gamepanel_shell' 'removed' 'GamePanel.java is absent from active source tree.'
}

$activeShards = @(Get-ChildItem -LiteralPath (Join-Path $root 'src\mechanist') -Filter 'gamepanel-shard*.txt' -File -ErrorAction SilentlyContinue)
if ($activeShards.Count -gt 0) {
    Add-Gate 'ERROR' 'active_shard_files' 'present' ("Active shard text files remain in src/mechanist: " + (($activeShards | ForEach-Object { $_.Name }) -join ', '))
} else {
    Add-Gate 'INFO' 'active_shard_files' 'absent' 'No active gamepanel-shard*.txt files under src/mechanist.'
}

$archiveRoots = @(
    (Join-Path $root 'ROOT_docs\shardmining\generated_subsystems\_backups'),
    (Join-Path $root 'ROOT_docs\shardmining\generated_subsystems\_retired_shards'),
    (Join-Path $root 'ROOT_DOCS\shardmining\generated_subsystems\_backups'),
    (Join-Path $root 'ROOT_DOCS\shardmining\generated_subsystems\_retired_shards')
)
$archiveHits = @($archiveRoots | Where-Object { Test-Path -LiteralPath $_ })
if ($archiveHits.Count -gt 0) {
    $severity = if ($AllowArchivedShardCopies) { 'WARN' } else { 'ERROR' }
    Add-Gate $severity 'archived_shard_copies' 'present' ("Archive backup/retired shard directories still exist: " + ($archiveHits -join '; '))
} else {
    Add-Gate 'INFO' 'archived_shard_copies' 'absent' 'No generated_subsystems _backups/_retired_shards directories found.'
}

Write-Section 'Regenerate function and Mermaid maps'
$functionMapExit = 999
if ($hasPython) {
    $functionMapExit = Run-ProcessCaptured 'Function map builder' 'py' @('-3', (Join-Path $root 'scripts\BUILD_FUNCTION_MAP.py'), '--apply') (Join-Path $runRoot 'function_map_builder.log') $CommandTimeoutSeconds
    if ($functionMapExit -eq 0) { Add-Gate 'INFO' 'function_map_builder' 'pass' 'Function map regenerated.' } else { Add-Gate 'ERROR' 'function_map_builder' 'fail' "Exit code $functionMapExit" }

    $mermaidExit = Run-ProcessCaptured 'Mermaid code map builder' 'py' @('-3', (Join-Path $root 'scripts\BUILD_MERMAID_CODE_MAP.py'), '--apply') (Join-Path $runRoot 'mermaid_code_map_builder.log') $CommandTimeoutSeconds
    if ($mermaidExit -eq 0) { Add-Gate 'INFO' 'mermaid_code_map_builder' 'pass' 'Mermaid code position map regenerated.' } else { Add-Gate 'ERROR' 'mermaid_code_map_builder' 'fail' "Exit code $mermaidExit" }
} else {
    Add-Gate 'ERROR' 'map_builders' 'skipped' 'py launcher missing; function/Mermaid maps were not regenerated.'
}

Write-Section 'Mermaid evaluation gate'
$evalCandidates = @(
    (Join-Path $root 'ROOT_docs\functionmap\generated\CODE_MERMAID_EVALUATION.tsv'),
    (Join-Path $root 'ROOT_DOCS\functionmap\generated\CODE_MERMAID_EVALUATION.tsv')
)
$evalPath = $evalCandidates | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
if ($evalPath) {
    Copy-Item -LiteralPath $evalPath -Destination $mermaidEvalCopy -Force
    $rows = @(Import-Csv -LiteralPath $evalPath -Delimiter "`t")
    $errors = @($rows | Where-Object { $_.severity -eq 'ERROR' })
    $warnings = @($rows | Where-Object { $_.severity -eq 'WARN' })
    if ($errors.Count -gt 0) {
        Add-Gate 'ERROR' 'mermaid_position_errors' 'fail' "$($errors.Count) unpositioned/error module rows in CODE_MERMAID_EVALUATION.tsv"
    } else {
        Add-Gate 'INFO' 'mermaid_position_errors' 'pass' 'No ERROR rows in CODE_MERMAID_EVALUATION.tsv.'
    }
    if ($warnings.Count -gt 0) {
        Add-Gate 'WARN' 'mermaid_position_warnings' 'review' "$($warnings.Count) WARN rows require ownership review."
    } else {
        Add-Gate 'INFO' 'mermaid_position_warnings' 'pass' 'No WARN rows in CODE_MERMAID_EVALUATION.tsv.'
    }
} else {
    Add-Gate 'ERROR' 'mermaid_evaluation_file' 'missing' 'CODE_MERMAID_EVALUATION.tsv was not found after builder run.'
}

$summaryCandidates = @(
    (Join-Path $root 'ROOT_docs\functionmap\generated\SUMMARY.md'),
    (Join-Path $root 'ROOT_DOCS\functionmap\generated\SUMMARY.md')
)
$functionSummary = $summaryCandidates | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
if ($functionSummary) { Copy-Item -LiteralPath $functionSummary -Destination $functionSummaryCopy -Force }

Write-Section 'Compile smoke'
$compileExit = 0
if ($SkipCompile) {
    Add-Gate 'WARN' 'compile_smoke' 'skipped' 'Compile smoke skipped by request.'
} else {
    $compileScript = Join-Path $root 'scripts\SMOKE_SHARD8_DIAGNOSTIC_WINDOWS.ps1'
    if (Test-Path -LiteralPath $compileScript -PathType Leaf) {
        $args = @('-ExecutionPolicy', 'Bypass', '-File', $compileScript)
        if ($VerboseJava) { $args += '-VerboseJava' }
        $compileExit = Run-ProcessCaptured 'Existing javac/Maven compile smoke' 'powershell' $args $compileRunLog $CommandTimeoutSeconds
        if ($compileExit -eq 0) { Add-Gate 'INFO' 'compile_smoke' 'pass' 'Existing compile smoke completed successfully.' } else { Add-Gate 'ERROR' 'compile_smoke' 'fail' "Compile smoke failed with exit code $compileExit." }
    } else {
        Add-Gate 'ERROR' 'compile_smoke_script' 'missing' 'scripts/SMOKE_SHARD8_DIAGNOSTIC_WINDOWS.ps1 not found.'
        $compileExit = 998
    }
}

Write-Section 'Final gate summary'
$gateRows | Set-Content -LiteralPath $gateTable
$errorRows = @($gateRows | Where-Object { $_ -like 'ERROR*' })
$warnRows = @($gateRows | Where-Object { $_ -like 'WARN*' })
"Gate table: $gateTable" | Add-Content -LiteralPath $summary
"Errors: $($errorRows.Count)" | Add-Content -LiteralPath $summary
"Warnings: $($warnRows.Count)" | Add-Content -LiteralPath $summary
"CompileExit: $compileExit" | Add-Content -LiteralPath $summary

if ($errorRows.Count -gt 0) {
    Write-Host "FUNCTION OPS SMOKE FAIL: $($errorRows.Count) error gates. Logs: $runRoot" -ForegroundColor Red
    exit 1
}

Write-Host "FUNCTION OPS SMOKE PASS with $($warnRows.Count) warnings. Logs: $runRoot" -ForegroundColor Green
exit 0
