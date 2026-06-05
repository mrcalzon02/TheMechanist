param(
    [Parameter(Mandatory=$true, ValueFromRemainingArguments=$true)]
    [string[]]$Paths
)

$ErrorActionPreference = "Stop"

function Read-U2 {
    param([byte[]]$Bytes, [int]$Offset)
    return (($Bytes[$Offset] -shl 8) -bor $Bytes[$Offset + 1])
}

function Test-ClassBytes {
    param([byte[]]$Bytes, [string]$Name)
    if ($Bytes.Length -lt 8) { throw "Class file too short: $Name" }
    if ($Bytes[0] -ne 0xCA -or $Bytes[1] -ne 0xFE -or $Bytes[2] -ne 0xBA -or $Bytes[3] -ne 0xBE) {
        throw "Invalid class magic: $Name"
    }
    $major = Read-U2 $Bytes 6
    return [pscustomobject]@{ Name = $Name; Major = $major }
}

function Get-ClassEntriesFromJar {
    param([string]$JarPath)
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        foreach ($entry in $archive.Entries) {
            if (-not $entry.FullName.EndsWith(".class", [System.StringComparison]::OrdinalIgnoreCase)) { continue }
            $stream = $entry.Open()
            try {
                $memory = New-Object System.IO.MemoryStream
                $stream.CopyTo($memory)
                Test-ClassBytes $memory.ToArray() "$JarPath!$($entry.FullName)"
            } finally {
                $stream.Dispose()
            }
        }
    } finally {
        $archive.Dispose()
    }
}

$checked = 0
$highest = 0
$failures = New-Object System.Collections.Generic.List[string]

foreach ($path in $Paths) {
    $resolved = Resolve-Path -LiteralPath $path -ErrorAction Stop
    foreach ($item in $resolved) {
        $file = Get-Item -LiteralPath $item.Path
        $results = @()
        if ($file.PSIsContainer) {
            foreach ($classFile in Get-ChildItem -LiteralPath $file.FullName -Recurse -File -Filter *.class) {
                $results += Test-ClassBytes ([System.IO.File]::ReadAllBytes($classFile.FullName)) $classFile.FullName
            }
        } elseif ($file.Extension -ieq ".jar" -or $file.Extension -ieq ".zip") {
            $results += Get-ClassEntriesFromJar $file.FullName
        } elseif ($file.Extension -ieq ".class") {
            $results += Test-ClassBytes ([System.IO.File]::ReadAllBytes($file.FullName)) $file.FullName
        }

        foreach ($result in $results) {
            $checked++
            if ($result.Major -gt $highest) { $highest = $result.Major }
            if ($result.Major -gt 61) { $failures.Add("$($result.Name) major=$($result.Major)") }
        }
    }
}

if ($failures.Count -gt 0) {
    Write-Error ("Java 17 classfile gate failed:`n" + ($failures -join "`n"))
}

Write-Host ("Java 17 classfile scan passed for {0} classfiles; highest major version {1}." -f $checked, $highest)
