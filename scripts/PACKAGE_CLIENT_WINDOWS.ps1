param(
    [string]$OutputDir = "PACKAGE_client",
    [int]$MaxErrors = 200,
    [switch]$CleanOutput,
    [switch]$CleanClasses,
    [switch]$SkipAssetCopy,
    [switch]$IncludeSourceMirror,
    [switch]$BuildJar
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$rootFull = [System.IO.Path]::GetFullPath([string]$root).TrimEnd([char[]]@([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar))
$rootPrefix = $rootFull + [System.IO.Path]::DirectorySeparatorChar
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$diagRoot = Join-Path $root 'diagnostics'
$runRoot = Join-Path $diagRoot "package_client_$stamp"
$outRoot = if ([System.IO.Path]::IsPathRooted($OutputDir)) { $OutputDir } else { Join-Path $root $OutputDir }
$outRoot = [System.IO.Path]::GetFullPath([string]$outRoot)
$classes = Join-Path $outRoot 'classes'

New-Item -ItemType Directory -Force -Path $runRoot | Out-Null
$summary = Join-Path $runRoot 'SUMMARY.txt'
$compileLog = Join-Path $runRoot 'package_compile.log'
$packageLog = Join-Path $runRoot 'package.log'
$errors = Join-Path $runRoot 'package_compile_errors.tsv'
$sourceList = Join-Path $runRoot 'sources.txt'
$javacSourceArgs = Join-Path $runRoot 'javac_sources.args'
$assetInventory = Join-Path $runRoot 'asset_inventory.txt'
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

function Copy-FilePreserveTree($sourceRoot, $destRoot, $fileInfo) {
    $sourceFull = [System.IO.Path]::GetFullPath([string]$fileInfo.FullName)
    $rootFullLocal = [System.IO.Path]::GetFullPath([string]$sourceRoot).TrimEnd([char[]]@([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar))
    $rootPrefixLocal = $rootFullLocal + [System.IO.Path]::DirectorySeparatorChar
    if ($sourceFull.StartsWith($rootPrefixLocal, [System.StringComparison]::OrdinalIgnoreCase)) { $relative = $sourceFull.Substring($rootPrefixLocal.Length) } else { $relative = [System.IO.Path]::GetFileName($sourceFull) }
    if ([System.IO.Path]::IsPathRooted($relative)) { $relative = [System.IO.Path]::GetFileName($relative) }
    $dest = Join-Path -Path $destRoot -ChildPath $relative
    $parent = Split-Path -Parent $dest
    if (-not [string]::IsNullOrWhiteSpace($parent)) { New-Item -ItemType Directory -Force -Path $parent | Out-Null }
    Copy-Item -LiteralPath $sourceFull -Destination $dest -Force -ErrorAction Stop
}

function Copy-MergeIfExists($source, $dest, $label) {
    if (-not (Test-Path -LiteralPath $source -PathType Container)) {
        "SKIP missing $label source: $source" | Tee-Object -FilePath $packageLog -Append
        return 0
    }
    $sourceFull = [System.IO.Path]::GetFullPath([string]$source)
    $destFull = [System.IO.Path]::GetFullPath([string]$dest)
    $sourceNorm = $sourceFull.TrimEnd([char[]]@([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar))
    $destNorm = $destFull.TrimEnd([char[]]@([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar))
    if ($sourceNorm.Equals($destNorm, [System.StringComparison]::OrdinalIgnoreCase)) {
        $count = @(Get-ChildItem -LiteralPath $destFull -Recurse -File -Force -ErrorAction SilentlyContinue).Count
        "PRESERVE $label already in package: $destFull ($count file(s))" | Tee-Object -FilePath $packageLog -Append
        return $count
    }
    New-Item -ItemType Directory -Force -Path $destFull | Out-Null
    $files = @(Get-ChildItem -LiteralPath $sourceFull -Recurse -File -Force -ErrorAction SilentlyContinue)
    foreach ($file in $files) { Copy-FilePreserveTree $sourceFull $destFull $file }
    "MERGED $($files.Count) $label file(s): $sourceFull -> $destFull" | Tee-Object -FilePath $packageLog -Append
    return $files.Count
}

function Publish-LatestPackageAliases() {
    if (Test-Path -LiteralPath $summary) { Copy-Item -LiteralPath $summary -Destination (Join-Path $diagRoot 'LATEST_PACKAGE_SUMMARY.txt') -Force -ErrorAction Continue }
    if (Test-Path -LiteralPath $compileLog) { Copy-Item -LiteralPath $compileLog -Destination (Join-Path $diagRoot 'LATEST_PACKAGE_COMPILE_LOG.txt') -Force -ErrorAction Continue }
    if (Test-Path -LiteralPath $errors) { Copy-Item -LiteralPath $errors -Destination (Join-Path $diagRoot 'LATEST_PACKAGE_COMPILE_ERRORS.tsv') -Force -ErrorAction Continue }
    if (Test-Path -LiteralPath $packageLog) { Copy-Item -LiteralPath $packageLog -Destination (Join-Path $diagRoot 'LATEST_PACKAGE_LOG.txt') -Force -ErrorAction Continue }
    if (Test-Path -LiteralPath $assetInventory) { Copy-Item -LiteralPath $assetInventory -Destination (Join-Path $diagRoot 'LATEST_PACKAGE_ASSET_INVENTORY.txt') -Force -ErrorAction Continue }
}

function Resolve-CommandPath($name) {
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if (-not $cmd) { return $null }
    if ($cmd.Source) { return $cmd.Source }
    return $cmd.Name
}

function Resolve-JavaHomeTool($toolName) {
    if (-not $env:JAVA_HOME) { return $null }
    foreach ($candidateName in @("$toolName.exe", "$toolName.cmd", $toolName)) {
        $candidate = Join-Path (Join-Path $env:JAVA_HOME 'bin') $candidateName
        if (Test-Path -LiteralPath $candidate -PathType Leaf) { return $candidate }
    }
    return $null
}

"Client Package Run: $stamp" | Set-Content -LiteralPath $summary
"Repository root: $rootFull" | Add-Content -LiteralPath $summary
"Package output: $outRoot" | Add-Content -LiteralPath $summary
"Package mode: unfolded exposed client directory" | Add-Content -LiteralPath $summary
"Classes output: $classes" | Add-Content -LiteralPath $summary
"Max javac errors: $MaxErrors" | Add-Content -LiteralPath $summary
"CleanOutput: $CleanOutput" | Add-Content -LiteralPath $summary
"CleanClasses: $CleanClasses" | Add-Content -LiteralPath $summary
"IncludeSourceMirror: $IncludeSourceMirror" | Add-Content -LiteralPath $summary
"BuildJar: $BuildJar" | Add-Content -LiteralPath $summary

Write-Section 'Tooling preflight'
$javacExe = Resolve-CommandPath 'javac'
if (-not $javacExe) { $javacExe = Resolve-JavaHomeTool 'javac' }
if (-not $javacExe) { 'ERROR: javac not found on PATH or JAVA_HOME.' | Tee-Object -FilePath $compileLog; Publish-LatestPackageAliases; exit 127 }
"javac command: $javacExe" | Add-Content -LiteralPath $summary
& $javacExe -version *>&1 | Tee-Object -FilePath $compileLog
"INFO: unfolded PACKAGE_client mode; jar assembly skipped unless -BuildJar is supplied." | Tee-Object -FilePath $packageLog

Write-Section 'Prepare unfolded PACKAGE_client directory'
if ((Test-Path -LiteralPath $outRoot) -and $CleanOutput) {
    Write-Host "CLEAN OUTPUT $outRoot"
    Remove-Item -LiteralPath $outRoot -Recurse -Force -ErrorAction Continue
}
New-Item -ItemType Directory -Force -Path $outRoot | Out-Null
if ((Test-Path -LiteralPath $classes) -and $CleanClasses) {
    Write-Host "CLEAN CLASSES $classes"
    Remove-Item -LiteralPath $classes -Recurse -Force -ErrorAction Continue
}
New-Item -ItemType Directory -Force -Path $classes | Out-Null

$staleSrc = Join-Path $outRoot 'src'
if ((Test-Path -LiteralPath $staleSrc) -and (-not $IncludeSourceMirror)) {
    Write-Host "REMOVE STALE SOURCE MIRROR $staleSrc"
    Remove-Item -LiteralPath $staleSrc -Recurse -Force -ErrorAction Continue
}
if ((Test-Path -LiteralPath (Join-Path $outRoot 'TheMechanist.jar')) -and (-not $BuildJar)) {
    Remove-Item -LiteralPath (Join-Path $outRoot 'TheMechanist.jar') -Force -ErrorAction Continue
}

Write-Section 'Source inventory'
$sources = @(Get-ChildItem -LiteralPath (Join-Path $root 'src') -Recurse -Filter '*.java' | Sort-Object FullName | ForEach-Object { $_.FullName })
$sources | Set-Content -LiteralPath $sourceList
$javacArgLines = @($sources | ForEach-Object { Convert-ToJavacRelativePath $_ })
Write-Utf8NoBomLines $javacSourceArgs $javacArgLines
"Java source files: $($sources.Count)" | Add-Content -LiteralPath $summary
"Source list: $sourceList" | Add-Content -LiteralPath $summary

Write-Section 'Compile client classes into unfolded package'
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

Write-Section 'Merge package assets and resources'
$totalCopied = 0
if (-not $SkipAssetCopy) {
    $assetDest = Join-Path $outRoot 'assets'
    $assetSources = @(
        (Join-Path $root 'assets'),
        (Join-Path $root 'client\assets'),
        (Join-Path $root 'resources\assets'),
        (Join-Path $root 'src\main\resources\assets')
    )
    foreach ($source in $assetSources) { $totalCopied += Copy-MergeIfExists $source $assetDest 'assets' }
    $totalCopied += Copy-MergeIfExists (Join-Path $root 'settings') (Join-Path $outRoot 'settings') 'settings'
    $totalCopied += Copy-MergeIfExists (Join-Path $root 'client\locale') (Join-Path $outRoot 'client\locale') 'client-locale'
    $totalCopied += Copy-MergeIfExists (Join-Path $root 'locale') (Join-Path $outRoot 'locale') 'locale'
    $totalCopied += Copy-MergeIfExists (Join-Path $root 'PACKAGE_client\assets') (Join-Path $outRoot 'assets') 'existing-package-assets'
}
if ($IncludeSourceMirror) { $totalCopied += Copy-MergeIfExists (Join-Path $root 'src') (Join-Path $outRoot 'src') 'source-mirror' }

$assetRoot = Join-Path $outRoot 'assets'
if (Test-Path -LiteralPath $assetRoot -PathType Container) {
    Get-ChildItem -LiteralPath $assetRoot -Recurse -File -Force -ErrorAction SilentlyContinue |
        ForEach-Object { $_.FullName.Substring($assetRoot.Length).TrimStart([char[]]@([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)) } |
        Sort-Object | Set-Content -LiteralPath $assetInventory
} else {
    'NO PACKAGE assets DIRECTORY PRESENT' | Set-Content -LiteralPath $assetInventory
}
$packageAssetCount = @(Get-ChildItem -LiteralPath $assetRoot -Recurse -File -Force -ErrorAction SilentlyContinue).Count
"MergedResourceFiles: $totalCopied" | Add-Content -LiteralPath $summary
"PackageAssetFiles: $packageAssetCount" | Add-Content -LiteralPath $summary
"AssetInventory: $assetInventory" | Add-Content -LiteralPath $summary

$runBat = Join-Path $outRoot 'RUN_THE_MECHANIST_CLIENT.bat'
@(
    '@echo off',
    'setlocal',
    'cd /d "%~dp0"',
    'java -Dmechanist.assetRoot=. -Dmechanist.generatedAssetRoot=. -Dmechanist.assetTier=low_32 -cp "classes;." mechanist.TheMechanist',
    'set MECH_EXIT=%ERRORLEVEL%',
    'echo.',
    'echo The Mechanist client exited with code %MECH_EXIT%.',
    'pause',
    'exit /b %MECH_EXIT%'
) | Set-Content -LiteralPath $runBat

$runPs1 = Join-Path $outRoot 'RUN_THE_MECHANIST_CLIENT.ps1'
@(
    '$ErrorActionPreference = "Stop"',
    'Set-Location -LiteralPath $PSScriptRoot',
    '& java -Dmechanist.assetRoot=. -Dmechanist.generatedAssetRoot=. -Dmechanist.assetTier=low_32 -cp "classes;." mechanist.TheMechanist',
    'exit $LASTEXITCODE'
) | Set-Content -LiteralPath $runPs1

$pkgManifest = Join-Path $outRoot 'PACKAGE_MANIFEST.txt'
@(
    'The Mechanist unfolded client package',
    "Built: $stamp",
    'Primary layout: unfolded directory, not jar-first',
    'Classes: classes\mechanist\*.class',
    'Assets: assets\',
    "Package asset files: $packageAssetCount",
    "Source files compiled: $($sources.Count)",
    'Launch: RUN_THE_MECHANIST_CLIENT.bat or powershell -ExecutionPolicy Bypass -File RUN_THE_MECHANIST_CLIENT.ps1',
    'Manual launch: java -Dmechanist.assetRoot=. -Dmechanist.generatedAssetRoot=. -Dmechanist.assetTier=low_32 -cp "classes;." mechanist.TheMechanist',
    'Note: PACKAGE_client is preserved by default. The src folder is removed unless -IncludeSourceMirror is supplied.'
) | Set-Content -LiteralPath $pkgManifest

$layout = Join-Path $outRoot 'PACKAGE_LAYOUT.txt'
@(
    'PACKAGE_client is the exposed unfolded client directory.',
    '',
    'Expected important paths:',
    '  classes\                 Compiled Java classes',
    '  assets\                  Art/audio/data assets copied by merge, not destructive delete',
    '  settings\                Settings copied by merge when present',
    '  client\locale\ or locale\ Localization files copied by merge when present',
    '  RUN_THE_MECHANIST_CLIENT.bat',
    '  RUN_THE_MECHANIST_CLIENT.ps1',
    '',
    'The full source folder is not included by default.',
    'The jar artifact is optional and is not the primary package output.'
) | Set-Content -LiteralPath $layout

if ($BuildJar) {
    Write-Section 'Optional jar assembly'
    $jarExe = Resolve-CommandPath 'jar'
    if (-not $jarExe) { $jarExe = Resolve-JavaHomeTool 'jar' }
    if (-not $jarExe) { 'ERROR: jar not found for optional -BuildJar mode.' | Tee-Object -FilePath $packageLog -Append; Publish-LatestPackageAliases; exit 127 }
    $jarPath = Join-Path $outRoot 'TheMechanist.jar'
    Write-Utf8NoBomLines $manifest @('Manifest-Version: 1.0', 'Main-Class: mechanist.TheMechanist', 'Implementation-Title: The Mechanist', "Implementation-Version: $stamp", '')
    Push-Location $classes
    try { & $jarExe cfm $jarPath $manifest . *>&1 | Tee-Object -FilePath $packageLog -Append; $jarExit = $LASTEXITCODE } finally { Pop-Location }
    "JarExit: $jarExit" | Add-Content -LiteralPath $summary
    if ($jarExit -ne 0) { Publish-LatestPackageAliases; exit $jarExit }
} else { "JarExit: skipped" | Add-Content -LiteralPath $summary }

$classCount = @(Get-ChildItem -LiteralPath $classes -Recurse -Filter '*.class' -ErrorAction SilentlyContinue).Count
"ClassFiles: $classCount" | Add-Content -LiteralPath $summary
"CLIENT PACKAGE PASS: unfolded client updated at $outRoot with $classCount class files and $packageAssetCount package asset files" | Tee-Object -FilePath $packageLog -Append
Publish-LatestPackageAliases
exit 0

