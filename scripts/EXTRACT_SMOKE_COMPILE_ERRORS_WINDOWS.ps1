param(
    [Parameter(Mandatory=$true)][string]$LogPath,
    [Parameter(Mandatory=$true)][string]$OutputPath
)

$ErrorActionPreference = 'Stop'
$rows = New-Object System.Collections.Generic.List[string]
$rows.Add("file`tline`tcolumn`tkind`tmessage")

if (-not (Test-Path -LiteralPath $LogPath -PathType Leaf)) {
    $rows.Add("`t`t`tmissing-log`t$LogPath")
    $rows | Set-Content -LiteralPath $OutputPath -Encoding UTF8
    Write-Host "No log found: $LogPath"
    exit 1
}

$pattern = '^(?<file>.*?\.java):(?<line>\d+):(?:(?<col>\d+):)?\s*(?<kind>error|warning):\s*(?<msg>.*)$'
$lines = Get-Content -LiteralPath $LogPath
for ($i = 0; $i -lt $lines.Count; $i++) {
    $line = $lines[$i]
    $m = [regex]::Match($line, $pattern)
    if ($m.Success) {
        $file = $m.Groups['file'].Value
        $ln = $m.Groups['line'].Value
        $col = $m.Groups['col'].Value
        $kind = $m.Groups['kind'].Value
        $msg = $m.Groups['msg'].Value -replace "`t", ' ' 
        $rows.Add("$file`t$ln`t$col`t$kind`t$msg")
    }
}

if ($rows.Count -eq 1) {
    foreach ($line in $lines) {
        if ($line -match 'BUILD FAILURE|Compilation failure|cannot find symbol|symbol:|location:|error:') {
            $msg = $line -replace "`t", ' '
            $rows.Add("`t`t`tdiagnostic`t$msg")
        }
    }
}

$rows | Set-Content -LiteralPath $OutputPath -Encoding UTF8
Write-Host "Extracted $($rows.Count - 1) compile diagnostics to $OutputPath"
exit 0
