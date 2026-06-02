param(
    [string]$OutputDir = "PACKAGE_client",
    [int]$MaxErrors = 200,
    [switch]$CleanOutput,
    [switch]$CleanClasses,
    [switch]$SkipAssetCopy,
    [switch]$SkipSourceMirror,
    [switch]$BuildJar
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$rootFull = [System.IO.Path]::GetFullPath($root).TrimEnd([char[]]@([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar))
$rootPrefix = $rootFull + [System.IO.Path]::DirectorySeparatorChar
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$diagRoot = Join-Path $root 'diagnostics'
$runRoot = Join-Path $diagRoot "package_client_$stamp"
$buildRoot = Join-Path $root 'build\package_client'
$outRoot = if ([System.IO.Path]::IsPathRooted($OutputDir)) { $OutputDir } else { Join-Path $root $OutputDir }
$classes = Join-Path $outRoot 'classes'

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

function Copy-FilePreserveTree($sourceRoot, $destRoot, $fileInfo) {
    $sourceFull = [System.IO.Path]::GetFullPath([string]$fileInfo.FullName)
    $rootFullLocal = [System.IO.Path]::GetFullPath([string]$sourceRoot).TrimEnd([char[]]@([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar))
    $rootPrefixLocal = $rootFullLocal + [System.IO.Path]::DirectorySeparatorChar
    if ($sourceFull.StartsWith($rootPrefixLocal, [System.StringComparison]::OrdinalIgnoreCase)) {
        $relative = $sourceFull.Substring($rootPrefixLocal.Length)
    } else {
        $relative = [System.IO.Path]::GetFileName($sourceFull)
    }
    if ([System.IO.Path]::IsPathRooted($relative)) { $relative = [System.IO.Path]::GetFileName($relative) }
    $dest = Join-Path -Path $destRoot -ChildPath $relative
    $parent = Split-Path -Parent $dest
    if (-not [string]::IsNullOrWhiteSpace($parent)) { New-Item -ItemType Directory -Force -Path $parent | Out-Null }
    Copy-Item -LiteralPath $sourceFull -Destination $dest -Force -ErrorAction Stop
}

function Copy-MergeIfExists($source, $dest) {
    if (-not (Test-Path -LiteralPath $source)) { return }
    Write-Host "MERGE $source -> $dest"
    if (Test-Path -LiteralPath $source -PathType Container) {
        New-Item -ItemType Directory -Force -Path $dest | Out-Null
        Get-ChildItem -LiteralPath $source -Recurse -File -Force | ForEach-Object { Copy-FilePreserveTree $source $dest $_ }
    } else {
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $dest) | Out-Null
        Copy-Item -LiteralPath $source -Destination $dest -Force -ErrorAction Continue
    }
}

function Copy-IfExists($source, $dest) {
    if (Test-Path -LiteralPath $source) {
        if (Test-Path -LiteralPath $source -PathType Container) {
            Copy-MergeIfExists $source $dest
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

function Resolve-JavaHomeTool($toolName) {
    if (-not $env:JAVA_HOME) { return $null }
    foreach ($candidateName in @("$toolName.exe", "$toolName.cmd", $toolName)) {
        $candidate = Join-Path (Join-Path $env:JAVA_HOME 'bin') $candidateName
        if (Test-Path -LiteralPath $candidate -PathType Leaf) { return $candidate }
    }
    return $null
}

function New-JarWithDotNetZip($sourceDir, $manifestSource, $targetJar, $logPath) {
    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction Stop
        if (Test-Path -LiteralPath $targetJar) { Remove-Item -LiteralPath $targetJar -Force -ErrorAction Stop }
        $metaInf = Join-Path $sourceDir 'META-INF'
        New-Item -ItemType Directory -Force -Path $metaInf | Out-Null
        Copy-Item -LiteralPath $manifestSource -Destination (Join-Path $metaInf 'MANIFEST.MF') -Force -ErrorAction Stop
        [System.IO.Compression.ZipFile]::CreateFromDirectory($sourceDir, $targetJar, [System.IO.Compression.CompressionLevel]::Optimal, $false)
        "DOTNET ZIP JAR PASS: $targetJar" | Tee-Object -FilePath $logPath -Append
        return 0
    } catch {
        "DOTNET ZIP JAR FAIL: $($_.Exception.Message)" | Tee-Object -FilePath $logPath -Append
        return 1
    }
}

"Client Package Run: $stamp" | Set-Content -LiteralPath $summary
"Repository root: $rootFull" | Add-Content -LiteralPath $summary
"Package output: $outRoot" | Add-Content -LiteralPath $summary
"Package mode: unfolded exposed client directory" | Add-Content -LiteralPath $summary
"Classes output: $classes" | Add-Content -LiteralPath $summary
"Build root: $buildRoot" | Add-Content -LiteralPath $summary
"Max javac errors: $MaxErrors" | Add-Content -LiteralPath $summary
"CleanOutput: $CleanOutput" | Add-Content -LiteralPath $summary
"CleanClasses: $CleanClasses" | Add-Content -LiteralPath $summary
"BuildJar: $BuildJar" | Add-Content -LiteralPath $summary

Write-Section 'Tooling preflight'
$javacExe = Resolve-CommandPath 'javac'
$jarExe = Resolve-CommandPath 'jar'
if (-not $javacExe) { $javacExe = Resolve-JavaHomeTool 'javac' }
if (-not $jarExe) { $jarExe = Resolve-JavaHomeTool 'jar' }
if ((-not $jarExe) -and $javacExe) { $jarExe = Resolve-SiblingTool $javacExe 'jar' }

if (-not $javacExe) { 'ERROR: javac not found on PATH or JAVA_HOME.' | Tee-Object -FilePath $compileLog; Publish-LatestPackageAliases; exit 127 }
"javac command: $javacExe" | Add-Content -LiteralPath $summary
& $javacExe -version *>&1 | Tee-Object -FilePath $compileLog
if ($BuildJar) {
    if ($jarExe) {
        "jar command: $jarExe" | Add-Content -LiteralPath $summary
        & $jarExe --version *>&1 | Tee-Object -FilePath $packageLog
    } else {
        "jar command: unavailable; optional BuildJar will use .NET ZipFile fallback" | Add-Content -LiteralPath $summary
        "WARN: jar not found; optional BuildJar will use .NET ZipFile fallback." | Tee-Object -FilePath $packageLog
    }
} else {
    "jar command: not required; unfolded package is primary output" | Add-Content -LiteralPath $summary
    "INFO: unfolded PACKAGE_client mode; jar assembly skipped unless -BuildJar is supplied." | Tee-Object -FilePath $packageLog
}

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

Write-Section 'Merge package resources'
if (-not $SkipAssetCopy) {
    Copy-MergeIfExists (Join-Path $root 'assets') (Join-Path $outRoot 'assets')
    Copy-MergeIfExists (Join-Path $root 'settings') (Join-Path $outRoot 'settings')
    Copy-MergeIfExists (Join-Path $root 'client\locale') (Join-Path $outRoot 'client\locale')
    Copy-MergeIfExists (Join-Path $root 'locale') (Join-Path $outRoot 'locale')
}
if (-not $SkipSourceMirror) {
    Copy-MergeIfExists (Join-Path $root 'src') (Join-Path $outRoot 'src')
}

$runBat = Join-Path $outRoot 'RUN_THE_MECHANIST_CLIENT.bat'
@(
    '@echo off',
    'setlocal',
    'cd /d "%~dp0"',
    'java -cp "classes;." mechanist.TheMechanist',
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
    '& java -cp "classes;." mechanist.TheMechanist',
    'exit $LASTEXITCODE'
) | Set-Content -LiteralPath $runPs1

$pkgManifest = Join-Path $outRoot 'PACKAGE_MANIFEST.txt'
@(
    'The Mechanist unfolded client package',
    "Built: $stamp",
    'Primary layout: unfolded directory, not jar-first',
    'Classes: classes\mechanist\*.class',
    'Source mirror: src\',
    'Assets/settings/locale are merged in place when present',
    "Source files: $($sources.Count)",
    'Launch: RUN_THE_MECHANIST_CLIENT.bat or powershell -ExecutionPolicy Bypass -File RUN_THE_MECHANIST_CLIENT.ps1',
    'Manual launch: java -cp "classes;." mechanist.TheMechanist',
    'Note: PACKAGE_client is preserved by default. Use -CleanOutput only when intentionally rebuilding from empty.'
) | Set-Content -LiteralPath $pkgManifest

$layout = Join-Path $outRoot 'PACKAGE_LAYOUT.txt'
@(
    'PACKAGE_client is the exposed unfolded client directory.',
    '',
    'Expected important paths:',
    '  classes\                 Compiled Java classes, updated in place by PACKAGE_CLIENT_WINDOWS.ps1',
    '  src\                     Source mirror for inspection/debugging/exposed-client review',
    '  assets\                  Art/audio/data assets copied by merge, not destructive delete',
    '  settings\                Settings copied by merge when present',
    '  client\locale\ or locale\ Localization files copied by merge when present',
    '  RUN_THE_MECHANIST_CLIENT.bat',
    '  RUN_THE_MECHANIST_CLIENT.ps1',
    '',
    'The package script intentionally does not delete this directory by default.',
    'The jar artifact is optional and is not the primary package output.'
) | Set-Content -LiteralPath $layout

if ($BuildJar) {
    Write-Section 'Optional jar assembly'
    $jarPath = Join-Path $outRoot 'TheMechanist.jar'
    Write-Utf8NoBomLines $manifest @(
        'Manifest-Version: 1.0',
        'Main-Class: mechanist.TheMechanist',
        'Implementation-Title: The Mechanist',
        "Implementation-Version: $stamp",
        ''
    )
    if ($jarExe) {
        Push-Location $classes
        try {
            & $jarExe cfm $jarPath $manifest . *>&1 | Tee-Object -FilePath $packageLog -Append
            $jarExit = $LASTEXITCODE
        } finally {
            Pop-Location
        }
    } else {
        $jarExit = New-JarWithDotNetZip $classes $manifest $jarPath $packageLog
    }
    "JarExit: $jarExit" | Add-Content -LiteralPath $summary
    if ($jarExit -ne 0) {
        Write-Host "CLIENT PACKAGE FAIL: optional jar assembly failed with exit $jarExit. Logs: $runRoot"
        Publish-LatestPackageAliases
        exit $jarExit
    }
    $jarInfo = Get-Item -LiteralPath $jarPath
    "PackageJar: $jarPath" | Add-Content -LiteralPath $summary
    "PackageJarBytes: $($jarInfo.Length)" | Add-Content -LiteralPath $summary
} else {
    "JarExit: skipped" | Add-Content -LiteralPath $summary
}

$classCount = @(Get-ChildItem -LiteralPath $classes -Recurse -Filter '*.class' -ErrorAction SilentlyContinue).Count
"ClassFiles: $classCount" | Add-Content -LiteralPath $summary
"CLIENT PACKAGE PASS: unfolded client updated at $outRoot with $classCount class files" | Tee-Object -FilePath $packageLog -Append
Publish-LatestPackageAliases
exit 0

