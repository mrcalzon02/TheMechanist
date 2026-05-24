param(
    [ValidateSet('windows','linux','all')]
    [string] $Platform = 'all',
    [string] $Root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path,
    [switch] $Force
)

$ErrorActionPreference = 'Stop'
$Version = '3.4.1'
$Repository = 'https://repo1.maven.org/maven2'
$Modules = @('lwjgl', 'lwjgl-glfw', 'lwjgl-opengl', 'lwjgl-stb')
$PlatformClassifiers = @{ windows = 'natives-windows'; linux = 'natives-linux' }
$LibDir = Join-Path $Root 'lib\lwjgl'
New-Item -ItemType Directory -Force -Path $LibDir | Out-Null

function Test-ValidJar {
    param([string] $Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { return $false }
    if ((Get-Item -LiteralPath $Path).Length -lt 128) { return $false }
    $fs = [System.IO.File]::OpenRead($Path)
    try {
        $bytes = New-Object byte[] 4
        $read = $fs.Read($bytes, 0, 4)
        return ($read -eq 4 -and $bytes[0] -eq 0x50 -and $bytes[1] -eq 0x4B -and $bytes[2] -eq 0x03 -and $bytes[3] -eq 0x04)
    } finally { $fs.Dispose() }
}

function Get-LwjglArtifacts {
    param([string] $PlatformName)
    $artifacts = New-Object System.Collections.Generic.List[object]
    foreach ($module in $Modules) {
        $file = "$module-$Version.jar"
        $url = "$Repository/org/lwjgl/$module/$Version/$file"
        $artifacts.Add([pscustomobject]@{ File = $file; Url = $url }) | Out-Null
    }
    $platforms = if ($PlatformName -eq 'all') { @('windows','linux') } else { @($PlatformName) }
    foreach ($p in $platforms) {
        $classifier = $PlatformClassifiers[$p]
        foreach ($module in $Modules) {
            $file = "$module-$Version-$classifier.jar"
            $url = "$Repository/org/lwjgl/$module/$Version/$file"
            $artifacts.Add([pscustomobject]@{ File = $file; Url = $url }) | Out-Null
        }
    }
    return $artifacts
}

foreach ($artifact in (Get-LwjglArtifacts $Platform)) {
    $target = Join-Path $LibDir $artifact.File
    if (-not $Force -and (Test-ValidJar $target)) {
        Write-Host "present: $($artifact.File)"
        continue
    }
    Write-Host "fetch:   $($artifact.File)"
    $tmp = "$target.tmp"
    Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
    Invoke-WebRequest -Uri $artifact.Url -OutFile $tmp -UseBasicParsing
    if (-not (Test-ValidJar $tmp)) {
        Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
        throw "Downloaded file is not a valid jar: $($artifact.Url)"
    }
    Move-Item -LiteralPath $tmp -Destination $target -Force
}
Write-Host "LWJGL runtime ready: $LibDir"
