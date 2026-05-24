param(
    [Parameter(Mandatory=$true, ValueFromRemainingArguments=$true)]
    [string[]]$Paths,
    [int]$MaxMajor = 61
)

Add-Type -AssemblyName System.IO.Compression.FileSystem
$ClassMagic = [byte[]](0xCA,0xFE,0xBA,0xBE)
$Scanned = 0
$Highest = 0
$Offenders = New-Object System.Collections.Generic.List[string]

function Get-ClassMajorFromBytes([byte[]]$Bytes, [string]$Label) {
    if ($Bytes.Length -lt 8) {
        Write-Warning "$Label is too short to be a classfile"
        return $null
    }
    for ($i = 0; $i -lt 4; $i++) {
        if ($Bytes[$i] -ne $ClassMagic[$i]) {
            Write-Warning "$Label is missing classfile magic"
            return $null
        }
    }
    return ($Bytes[6] * 256) + $Bytes[7]
}

function Scan-ClassFile([string]$FilePath, [string]$Label) {
    $bytes = [System.IO.File]::ReadAllBytes($FilePath)
    $major = Get-ClassMajorFromBytes $bytes $Label
    if ($null -ne $major) {
        $script:Scanned++
        if ($major -gt $script:Highest) { $script:Highest = $major }
        if ($major -gt $MaxMajor) { $script:Offenders.Add("major ${major}: ${Label}") }
    }
}

foreach ($raw in $Paths) {
    if (-not (Test-Path -LiteralPath $raw)) {
        Write-Error "Missing path $raw"
        exit 2
    }
    $item = Get-Item -LiteralPath $raw
    if ($item.PSIsContainer) {
        Get-ChildItem -LiteralPath $item.FullName -Filter *.class -Recurse | ForEach-Object {
            Scan-ClassFile $_.FullName ("{0}:{1}" -f $item.FullName, $_.FullName.Substring($item.FullName.Length).TrimStart('\','/'))
        }
    } elseif ($item.Extension -ieq ".class") {
        Scan-ClassFile $item.FullName $item.FullName
    } elseif ($item.Extension -ieq ".jar" -or $item.Extension -ieq ".zip") {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($item.FullName)
        try {
            foreach ($entry in $zip.Entries) {
                if ($entry.FullName.EndsWith(".class")) {
                    $stream = $entry.Open()
                    try {
                        $ms = New-Object System.IO.MemoryStream
                        $stream.CopyTo($ms)
                        $major = Get-ClassMajorFromBytes $ms.ToArray() ("{0}!/{1}" -f $item.FullName, $entry.FullName)
                        if ($null -ne $major) {
                            $Scanned++
                            if ($major -gt $Highest) { $Highest = $major }
                            if ($major -gt $MaxMajor) { $Offenders.Add(("major {0}: {1}!/{2}" -f $major, $item.FullName, $entry.FullName)) }
                        }
                    } finally {
                        $stream.Dispose()
                    }
                }
            }
        } finally {
            $zip.Dispose()
        }
    } else {
        Write-Warning "Skipping unsupported path $($item.FullName)"
    }
}

Write-Host "Scanned $Scanned classfile(s). Highest major version: $Highest. Allowed maximum: $MaxMajor."
if ($Offenders.Count -gt 0) {
    Write-Error "Java 17 classfile gate failed. Offending classes:`n$($Offenders -join "`n")"
    exit 1
}
Write-Host "Java 17 classfile gate passed."
exit 0
