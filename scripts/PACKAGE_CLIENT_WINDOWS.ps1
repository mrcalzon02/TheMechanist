param(
    [string]$OutputDir = "PACKAGE_client",
    [int]$MaxErrors = 200,
    [switch]$NoClean,
    [switch]$SkipAssetCopy
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$rootFull = [System.IO.Path]::GetFullPath($root).TrimEnd('\\', '/')
$rootPrefix = $rootFull + [System.IO.Path]::DirectorySeparatorChar
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$diagRoot = Join-Path $root 'diagnostics'
$runRoot = Join-Path $diagRoot "package_client_$stamp"
$buildRoot = Join-Path $root 'build\package_client'
$classes = Join-Path $buildRoot 'classes'
$outRoot = if ([System.IO.Path]::IsPathRooted($OutputDir)) { $OutputDir } else { Join-Path $root $OutputDir }

New-Item -ItemType Directory -Force -Path $runRoot | Out-Null
$summary = Join-Path $runRoot 'SUMMARY.txt'
$compileLog = Join-Path $runRoot 'package_compile.log'
$packageLog = Join-Path $runRoot 'package.log'
$errors = Join-Path $runRoot 'package_compile_errors.tsv'
$sourceList = Join-Path $runRoot 'sources.txt'
$javacSourceArgs = Join-Path $runRoot 'javac_sources.args'
$manifest = Join-Path $runRoot 'MANIFEST.MF'

function Write-Section($name) {
    $line = "==== $name ===="
    Add-Content -LiteralPath $summary -Value "`r`n$line"
    Write-Host $line
}

function Convert-ToJavacRelativePath($sourcePath) {
    $full = [System.IO.Path]::GetFullPath([string]$sourcePath)
    if ($full.StartsWith($rootPrefix, [System.StringComparison]::OrdinalIgnoreCase)) { $relative = $full.Substring($rootPrefix.Length) } else { $relative = $full }
    $relative = $relative -replace '\\', '/'
    return '"' + ($relative -replace '"', '\"') + '"'
}

function Write-Utf8NoBomLines($path, $lines) {
    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines($path, [string[]]$lines, $encoding)
}

function Copy-IfExists($source, $dest) {
    if (Test-Path -LiteralPath $source) {
        Write-Host "COPY $source -> $dest"
        if (Test-Path -LiteralPath $source -PathType Container) {
            if (Test-Path -LiteralPath $dest) { Remove-Item -LiteralPath $dest -Recurse -Force -ErrorAction Continue }
            Copy-Item -LiteralPath $source -Destination $dest -Recurse -Force -ErrorAction Continue
        } else {
            New-Item -ItemType Directory -Force -Path (Split-Path -Parent $dest) | Out-Null
            Copy-Item -LiteralPath $source -Destination $dest -Force -ErrorAction Continue
        }
    }
}

function Publish-LatestPackageAliases() {
    Copy-IfExists $summary (Join-Path $diagRoot 'LATEST_PACKAGE_SUMMARY.txt')
    Copy-IfExists $compileLog (Join-Path $diagRoot 'LATEST_PACKAGE_COMPILE_LOG.txt')
    Copy-IfExists $errors (Join-Path $diagRoot 'LATEST_PACKAGE_COMPILE_ERRORS.tsv')
    Copy-IfExists $packageLog (Join-Path $diagRoot 'LATEST_PACKAGE_LOG.txt')
}

function Resolve-CommandPath($name) {
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if (-not $cmd) { return $null }
    if ($cmd.Source) { return $cmd.Source }
    return $cmd.Name
}

function Resolve-SiblingTool($knownToolPath, $siblingName) {
    if (-not $knownToolPath) { return $null }
    $knownToolDir = Split-Path -Parent $knownToolPath
    if (-not $knownToolDir) { return $null }
    foreach ($candidateName in @("$siblingName.exe", "$siblingName.cmd", $siblingName)) {
        $candidate = Join-Path $knownToolDir $candidateName
        if (Test-Path -LiteralPath $candidate -PathType Leaf) { return $candidate }
    }
    return $null
}

"Client Package Run: $stamp" | Set-Content -LiteralPath $summary
"Repository root: $rootFull" | Add-Content -LiteralPath $summary
"Package output: $outRoot" | Add-Content -LiteralPath $summary
"Build root: $buildRoot" | Add-Content -LiteralPath $summary
"Max javac errors: $MaxErrors" | Add-Content -LiteralPath $summary

Write-Section 'Tooling preflight'
$javacExe = Resolve-CommandPath 'javac'
$jarExe = Resolve-CommandPath 'jar'
if ((-not $jarExe) -and $javacExe) { $jarExe = Resolve-SiblingTool $javacExe 'jar' }

if (-not $javacExe) { 'ERROR: javac not found on PATH.' | Tee-Object -FilePath $compileLog; Publish-LatestPackageAliases; exit 127 }
if (-not $jarExe) {
    $msg = "ERROR: jar not found on PATH or beside javac. javac resolved to: $javacExe"
    $msg | Tee-Object -FilePath $packageLog
    Publish-LatestPackageAliases
    exit 127
}
"javac command: $javacExe" | Add-Content -LiteralPath $summary
"jar command: $jarExe" | Add-Content -LiteralPath $summary
& $javacExe -version *>&1 | Tee-Object -FilePath $compileLog
& $jarExe --version *>&1 | Tee-Object -FilePath $packageLog

Write-Section 'Source inventory'
$sources = @(Get-ChildItem -LiteralPath (Join-Path $root 'src') -Recurse -Filter '*.java' | Sort-Object FullName | ForEach-Object { $_.FullName })
$sources | Set-Content -LiteralPath $sourceList
$javacArgLines = @($sources | ForEach-Object { Convert-ToJavacRelativePath $_ })
Write-Utf8NoBomLines $javacSourceArgs $javacArgLines
"Java source files: $($sources.Count)" | Add-Content -LiteralPath $summary
"Source list: $sourceList" | Add-Content -LiteralPath $summary

Write-Section 'Compile client classes'
if (Test-Path -LiteralPath $buildRoot) { Remove-Item -LiteralPath $buildRoot -Recurse -Force -ErrorAction Continue }
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$tempArgFile = Join-Path ([System.IO.Path]::GetTempPath()) "mechanist_package_javac_sources_$stamp.args"
Write-Utf8NoBomLines $tempArgFile $javacArgLines
$responseArg = '@' + $tempArgFile
$javacArgs = @('-Xmaxerrs', ([string][Math]::Max(100, $MaxErrors)), '-encoding', 'UTF-8', '-d', $classes, $responseArg)
Push-Location $root
try {
    Write-Host ('Working directory: ' + (Get-Location)) | Tee-Object -FilePath $compileLog -Append
    Write-Host ($javacExe + ' ' + ($javacArgs -join ' ')) | Tee-Object -FilePath $compileLog -Append
    & $javacExe @javacArgs *>&1 | Tee-Object -FilePath $compileLog -Append
    $compileExit = $LASTEXITCODE
} finally {
    Pop-Location
    if (Test-Path -LiteralPath $tempArgFile) { Remove-Item -LiteralPath $tempArgFile -Force -ErrorAction SilentlyContinue }
}
"CompileExit: $compileExit" | Add-Content -LiteralPath $summary

$extractor = Join-Path $PSScriptRoot 'EXTRACT_SMOKE_COMPILE_ERRORS_WINDOWS.ps1'
if (Test-Path -LiteralPath $extractor -PathType Leaf) {
    & $extractor -LogPath $compileLog -OutputPath $errors | Tee-Object -FilePath (Join-Path $runRoot 'error_extraction.log')
} else {
    "source`tline`tcolumn`tseverity`tmessage" | Set-Content -LiteralPath $errors
}
if ($compileExit -ne 0) {
    Write-Host "CLIENT PACKAGE FAIL: compile failed with exit $compileExit. Logs: $runRoot"
    Publish-LatestPackageAliases
    exit $compileExit
}

Write-Section 'Assemble PACKAGE_client'
if ((Test-Path -LiteralPath $outRoot) -and (-not $NoClean)) { Remove-Item -LiteralPath $outRoot -Recurse -Force -ErrorAction Continue }
New-Item -ItemType Directory -Force -Path $outRoot | Out-Null
$jarPath = Join-Path $outRoot 'TheMechanist.jar'
@(
    'Manifest-Version: 1.0',
    'Main-Class: mechanist.TheMechanist',
    'Implementation-Title: The Mechanist',
    "Implementation-Version: $stamp",
    ''
) | Set-Content -LiteralPath $manifest
Push-Location $classes
try {
    & $jarExe cfm $jarPath $manifest . *>&1 | Tee-Object -FilePath $packageLog -Append
    $jarExit = $LASTEXITCODE
} finally {
    Pop-Location
}
"JarExit: $jarExit" | Add-Content -LiteralPath $summary
if ($jarExit -ne 0) {
    Write-Host "CLIENT PACKAGE FAIL: jar failed with exit $jarExit. Logs: $runRoot"
    Publish-LatestPackageAliases
    exit $jarExit
}

if (-not $SkipAssetCopy) {
    Copy-IfExists (Join-Path $root 'assets') (Join-Path $outRoot 'assets')
    Copy-IfExists (Join-Path $root 'settings') (Join-Path $outRoot 'settings')
    Copy-IfExists (Join-Path $root 'client\locale') (Join-Path $outRoot 'client\locale')
    Copy-IfExists (Join-Path $root 'locale') (Join-Path $outRoot 'locale')
}

$runBat = Join-Path $outRoot 'RUN_THE_MECHANIST_CLIENT.bat'
@(
    '@echo off',
    'cd /d "%~dp0"',
    'java -jar TheMechanist.jar',
    'pause'
) | Set-Content -LiteralPath $runBat

$pkgManifest = Join-Path $outRoot 'PACKAGE_MANIFEST.txt'
@(
    'The Mechanist client package',
    "Built: $stamp",
    "Jar: TheMechanist.jar",
    "Main-Class: mechanist.TheMechanist",
    "Source files: $($sources.Count)",
    'Launch: RUN_THE_MECHANIST_CLIENT.bat or java -jar TheMechanist.jar'
) | Set-Content -LiteralPath $pkgManifest

$jarInfo = Get-Item -LiteralPath $jarPath
"PackageJar: $jarPath" | Add-Content -LiteralPath $summary
"PackageJarBytes: $($jarInfo.Length)" | Add-Content -LiteralPath $summary
"CLIENT PACKAGE PASS: $jarPath ($($jarInfo.Length) bytes)" | Tee-Object -FilePath $packageLog -Append
Publish-LatestPackageAliases
exit 0
