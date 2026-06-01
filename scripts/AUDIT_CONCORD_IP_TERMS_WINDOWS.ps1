param(
    [string]$OutputName = ''
)

$ErrorActionPreference = 'Continue'
$AuditVersion = '2026-05-31-v4-powershell5-safe'
Write-Host "Concord IP audit script $AuditVersion"

$root = Split-Path -Parent $PSScriptRoot
$rootFull = [System.IO.Path]::GetFullPath($root)
$backslash = [string][char]92
$slash = [string][char]47
while ($rootFull.EndsWith($backslash) -or $rootFull.EndsWith($slash)) {
    $rootFull = $rootFull.Substring(0, $rootFull.Length - 1)
}
$rootPrefix = $rootFull + [System.IO.Path]::DirectorySeparatorChar
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$diagRoot = Join-Path $root 'diagnostics'
if ([string]::IsNullOrWhiteSpace($OutputName)) { $OutputName = "concord_ip_audit_$stamp.tsv" }
$outputPath = Join-Path $diagRoot $OutputName
New-Item -ItemType Directory -Force -Path $diagRoot | Out-Null

$terms = @(
    'Warhammer', '40K', '40k', 'Imperium', 'Imperial', 'Adeptus', 'Mechanicus',
    'Tech Priest', 'Tech-Priest', 'Magos', 'Omnissiah', 'Arbites', 'Administratum',
    'Ecclesiarchy', 'Ministorum', 'Inquisition', 'Inquisitor', 'Ordo', 'Exterminatus',
    'Rosarius', 'Warrant of Trade', 'Space Marine', 'Astartes', 'Servitor', 'Cogitator',
    'Hive City', 'Medicae', 'Planetary Defense Force', 'Astra Militarum', 'Imperial Guard',
    'Guilliman', 'Baneblade', 'Leman Russ', 'Chimera', 'Basilisk', 'Sentinel', 'Hydra',
    'arbites', 'mechanicus', 'ecclesiarchy', 'administratum', 'inquisitor', 'cogitator',
    'servitor', 'medicae', 'civic Wardens'
)

function Convert-ToRepoRelativePath($path) {
    $full = [System.IO.Path]::GetFullPath([string]$path)
    if ($full.StartsWith($script:rootPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        $relative = $full.Substring($script:rootPrefix.Length)
    } else {
        $relative = $full
    }
    return ($relative -replace '\\', '/')
}

function Classify-Hit($path, $line) {
    if ($path -match '\.java$') {
        if ($line -match '^\s*(public\s+)?(final\s+)?class\s+') { return 'java-class' }
        if ($line -match '^\s*(public|private|protected|static|final|void|int|boolean|String|char|double|float|long|short|byte)\b') { return 'java-code' }
        if ($line -match '"') { return 'java-string-or-code' }
        return 'java-other'
    }
    if ($path -match '\.(md|txt)$') { return 'documentation' }
    if ($path -match '\.(json|cfg|properties|yml|yaml|xml)$') { return 'data-or-config' }
    return 'other'
}

$rows = New-Object System.Collections.Generic.List[string]
$rows.Add("term`tfile`tline`tcategory`ttext")
$files = Get-ChildItem -LiteralPath $root -Recurse -File | Where-Object {
    $_.FullName -notmatch '\\.git\\' -and
    $_.FullName -notmatch '\\diagnostics\\' -and
    $_.Extension -in @('.java', '.md', '.txt', '.json', '.cfg', '.properties', '.yml', '.yaml', '.xml', '.bat', '.ps1')
}

foreach ($file in $files) {
    $relative = Convert-ToRepoRelativePath $file.FullName
    $lines = @(Get-Content -LiteralPath $file.FullName -ErrorAction SilentlyContinue)
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = [string]$lines[$i]
        foreach ($term in $terms) {
            if ($line.IndexOf([string]$term, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
                $category = Classify-Hit $relative $line
                $clean = ($line.Trim() -replace "`t", ' ')
                $rows.Add("$term`t$relative`t$($i + 1)`t$category`t$clean")
            }
        }
    }
}

$encoding = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllLines($outputPath, [string[]]$rows, $encoding)
Write-Host "Wrote $($rows.Count - 1) suspect term hits to $outputPath"
exit 0
