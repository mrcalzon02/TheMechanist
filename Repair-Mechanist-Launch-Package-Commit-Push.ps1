#requires -Version 5.1
<#
Repair-Mechanist-Launch-Package-Commit-Push.ps1

One-command repair for:
    Error: Could not find or load main class mechanist.TheMechanist
    Caused by: java.lang.ClassNotFoundException: mechanist.TheMechanist

This script does not assume mechanist.TheMechanist exists.
It builds the current source, discovers the real Java main class, packages the jar,
writes launcher scripts that use the discovered entrypoint, commits, and pushes.

Run from anywhere inside the repo:

    powershell -ExecutionPolicy Bypass -File .\Repair-Mechanist-Launch-Package-Commit-Push.ps1

Optional:
    powershell -ExecutionPolicy Bypass -File .\Repair-Mechanist-Launch-Package-Commit-Push.ps1 -NoPush
    powershell -ExecutionPolicy Bypass -File .\Repair-Mechanist-Launch-Package-Commit-Push.ps1 -MainClass mechanist.ActualMainClass
#>

[CmdletBinding()]
param(
    [string]$CommitMessage = "Repair packaged client launch entrypoint",
    [string]$JarOutputPath = "PACKAGE_client/build/the-mechanist.jar",
    [string]$MainClass = "",
    [switch]$NoPush
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Fail([string]$Message) {
    Write-Host ""
    Write-Host "FAILED: $Message" -ForegroundColor Red
    exit 1
}

function Run-Checked([string]$Exe, [string[]]$ArgsList) {
    Write-Host ""
    Write-Host "> $Exe $($ArgsList -join ' ')" -ForegroundColor Cyan
    & $Exe @ArgsList
    if ($LASTEXITCODE -ne 0) {
        Fail "$Exe exited with code $LASTEXITCODE"
    }
}

function Get-CommandOrFail([string]$Name, [string]$InstallHint) {
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $cmd) {
        Fail "$Name was not found. $InstallHint"
    }
    return $cmd
}

function Get-PackageNameFromJava([string]$Text) {
    $m = [regex]::Match($Text, '(?m)^\s*package\s+([A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*)\s*;')
    if ($m.Success) { return $m.Groups[1].Value }
    return ""
}

function Get-PublicClassNameFromJava([string]$Text, [string]$FallbackStem) {
    $m = [regex]::Match($Text, '(?m)\bpublic\s+(?:final\s+|abstract\s+)?(?:class|record|enum)\s+([A-Za-z_$][\w$]*)\b')
    if ($m.Success) { return $m.Groups[1].Value }
    $m = [regex]::Match($Text, '(?m)\b(?:class|record|enum)\s+([A-Za-z_$][\w$]*)\b')
    if ($m.Success) { return $m.Groups[1].Value }
    return $FallbackStem
}

function Find-MainClassesInSource() {
    $results = New-Object System.Collections.Generic.List[string]
    $roots = @("src", "PACKAGE_client", "launcher", "PACKAGE_launcher")
    foreach ($root in $roots) {
        if (-not (Test-Path $root)) { continue }
        Get-ChildItem $root -Recurse -File -Filter "*.java" | ForEach-Object {
            $text = Get-Content $_.FullName -Raw
            if ($text -match 'public\s+static\s+void\s+main\s*\(\s*String(?:\s*\[\s*\]|\.\.\.)\s+[A-Za-z_$][\w$]*\s*\)') {
                $pkg = Get-PackageNameFromJava $text
                $cls = Get-PublicClassNameFromJava $text $_.BaseName
                if ([string]::IsNullOrWhiteSpace($pkg)) {
                    $results.Add($cls)
                } else {
                    $results.Add("$pkg.$cls")
                }
            }
        }
    }
    return @($results | Select-Object -Unique)
}

function Find-MainClassFromPom() {
    if (-not (Test-Path "pom.xml")) { return "" }
    $pom = Get-Content "pom.xml" -Raw
    $patterns = @(
        '<mainClass>\s*([^<\s]+)\s*</mainClass>',
        '<main.class>\s*([^<\s]+)\s*</main.class>',
        '<start-class>\s*([^<\s]+)\s*</start-class>'
    )
    foreach ($p in $patterns) {
        $m = [regex]::Match($pom, $p)
        if ($m.Success) { return $m.Groups[1].Value.Trim() }
    }
    return ""
}

function Get-MainClassFromJarManifest([string]$JarPath) {
    $tmp = Join-Path $env:TEMP ("mechanist_manifest_" + [guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Force $tmp | Out-Null
    try {
        Push-Location $tmp
        & jar xf $JarPath META-INF/MANIFEST.MF 2>$null
        Pop-Location
        $mf = Join-Path $tmp "META-INF/MANIFEST.MF"
        if (Test-Path $mf) {
            $text = Get-Content $mf -Raw
            $m = [regex]::Match($text, '(?m)^Main-Class:\s*([^\r\n]+)')
            if ($m.Success) { return $m.Groups[1].Value.Trim() }
        }
        return ""
    } finally {
        if ((Get-Location).Path -eq $tmp) { Pop-Location }
        Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
    }
}

function Jar-ContainsClass([string]$JarPath, [string]$ClassName) {
    $entry = ($ClassName -replace '\.', '/') + ".class"
    $list = & jar tf $JarPath
    return @($list) -contains $entry
}

function Write-LauncherScripts([string]$MainClassValue, [string]$JarPathRelative) {
    New-Item -ItemType Directory -Force "PACKAGE_client" | Out-Null
    New-Item -ItemType Directory -Force "PACKAGE_client/build" | Out-Null

    $cmd = @"
@echo off
setlocal
cd /d "%~dp0"
if exist "build\the-mechanist.jar" (
    java -cp "build\the-mechanist.jar" $MainClassValue
) else (
    echo Missing build\the-mechanist.jar
    exit /b 1
)
set EXITCODE=%ERRORLEVEL%
if not "%EXITCODE%"=="0" (
    echo.
    echo The Mechanist client exited with code %EXITCODE%.
    pause
)
exit /b %EXITCODE%
"@
    Set-Content -Path "PACKAGE_client/run-the-mechanist.cmd" -Value $cmd -Encoding ASCII

    $ps1 = @"
`$ErrorActionPreference = "Stop"
Set-Location `$PSScriptRoot
`$jar = Join-Path `$PSScriptRoot "build/the-mechanist.jar"
if (-not (Test-Path `$jar)) {
    throw "Missing packaged jar: `$jar"
}
& java -cp `$jar $MainClassValue
exit `$LASTEXITCODE
"@
    Set-Content -Path "PACKAGE_client/run-the-mechanist.ps1" -Value $ps1 -Encoding UTF8

    $entrypoint = @"
mechanist.client.mainClass=$MainClassValue
mechanist.client.jar=$JarPathRelative
mechanist.client.cmd=PACKAGE_client/run-the-mechanist.cmd
mechanist.client.ps1=PACKAGE_client/run-the-mechanist.ps1
"@
    Set-Content -Path "PACKAGE_client/build/client-entrypoint.properties" -Value $entrypoint -Encoding ASCII
}

Get-CommandOrFail "git" "Install Git and make sure it is on PATH." | Out-Null
Get-CommandOrFail "mvn" "Install Maven and make sure it is on PATH." | Out-Null
Get-CommandOrFail "jar" "Install a JDK, not just a JRE, and make sure its bin folder is on PATH." | Out-Null
Get-CommandOrFail "java" "Install Java 17 and make sure it is on PATH." | Out-Null

$repoRoot = (& git rev-parse --show-toplevel 2>$null)
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($repoRoot)) {
    Fail "This script must be run from inside the The Mechanist Git repository."
}
$repoRoot = $repoRoot.Trim()
Set-Location $repoRoot

$branch = (& git rev-parse --abbrev-ref HEAD).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($branch) -or $branch -eq "HEAD") {
    Fail "Could not determine a normal checked-out Git branch. Checkout a branch before running."
}

if (-not (Test-Path "pom.xml")) {
    Fail "No pom.xml found at repo root: $repoRoot"
}

Write-Host ""
Write-Host "Repository: $repoRoot" -ForegroundColor Green
Write-Host "Branch:     $branch" -ForegroundColor Green

Run-Checked "mvn" @("-B", "-DskipTests", "package")

$jarCandidates = Get-ChildItem "target" -File -Filter "*.jar" |
    Where-Object {
        $_.Name -notlike "*sources.jar" -and
        $_.Name -notlike "*javadoc.jar" -and
        $_.Name -notlike "original-*.jar"
    } |
    Sort-Object Length -Descending

if (-not $jarCandidates -or $jarCandidates.Count -lt 1) {
    Fail "No usable jar found in target/. Maven may not be producing a packaged game jar."
}

$sourceJar = $jarCandidates[0]
$outPath = Join-Path $repoRoot $JarOutputPath
$outDir = Split-Path -Parent $outPath
New-Item -ItemType Directory -Force $outDir | Out-Null
Copy-Item -Path $sourceJar.FullName -Destination $outPath -Force

Write-Host ""
Write-Host "Copied built jar:" -ForegroundColor Green
Write-Host "  from $($sourceJar.FullName)"
Write-Host "  to   $JarOutputPath"

$detectedMain = ""
if (-not [string]::IsNullOrWhiteSpace($MainClass)) {
    $detectedMain = $MainClass.Trim()
    Write-Host "Using supplied main class: $detectedMain" -ForegroundColor Yellow
}

if ([string]::IsNullOrWhiteSpace($detectedMain)) {
    $detectedMain = Get-MainClassFromJarManifest $outPath
    if (-not [string]::IsNullOrWhiteSpace($detectedMain)) {
        Write-Host "Detected Main-Class from jar manifest: $detectedMain" -ForegroundColor Green
    }
}

if ([string]::IsNullOrWhiteSpace($detectedMain)) {
    $pomMain = Find-MainClassFromPom
    if (-not [string]::IsNullOrWhiteSpace($pomMain)) {
        $detectedMain = $pomMain
        Write-Host "Detected main class from pom.xml: $detectedMain" -ForegroundColor Green
    }
}

$sourceMains = Find-MainClassesInSource
if ([string]::IsNullOrWhiteSpace($detectedMain)) {
    if ($sourceMains.Count -eq 1) {
        $detectedMain = $sourceMains[0]
        Write-Host "Detected only source main class: $detectedMain" -ForegroundColor Green
    } elseif ($sourceMains.Count -gt 1) {
        $preferred = @(
            $sourceMains | Where-Object { $_ -match 'mechanist\.(TheMechanist|Main|Game|Client|Launcher)$' } | Select-Object -First 1
        )
        if ($preferred.Count -gt 0) {
            $detectedMain = $preferred[0]
            Write-Host "Detected preferred source main class: $detectedMain" -ForegroundColor Green
        } else {
            Write-Host "Found multiple source main classes:" -ForegroundColor Yellow
            $sourceMains | ForEach-Object { Write-Host "  $_" }
            Fail "Multiple main classes found. Re-run with -MainClass one.of.TheseClasses"
        }
    }
}

if ([string]::IsNullOrWhiteSpace($detectedMain)) {
    Fail "Could not determine a Java main class. Add -MainClass package.ClassName."
}

if (-not (Jar-ContainsClass $outPath $detectedMain)) {
    Write-Host ""
    Write-Host "The selected main class is not present in the packaged jar: $detectedMain" -ForegroundColor Red
    Write-Host "Source mains found:" -ForegroundColor Yellow
    if ($sourceMains.Count -gt 0) {
        $sourceMains | ForEach-Object { Write-Host "  $_" }
    } else {
        Write-Host "  <none>"
    }

    $jarClasses = & jar tf $outPath | Where-Object { $_ -like "*.class" -and $_ -notlike "*`$*" } | Select-Object -First 80
    Write-Host ""
    Write-Host "First classes inside jar:" -ForegroundColor Yellow
    $jarClasses | ForEach-Object { Write-Host "  $_" }

    Fail "Packaged jar does not contain selected main class. Maven source/include configuration is wrong, or the launcher target is stale."
}

Write-Host ""
Write-Host "Confirmed packaged jar contains main class: $detectedMain" -ForegroundColor Green

Write-LauncherScripts -MainClassValue $detectedMain -JarPathRelative $JarOutputPath

$jarHash = (Get-FileHash -Algorithm SHA256 $outPath).Hash.ToLowerInvariant()
$sourceHead = (& git rev-parse HEAD).Trim()
$timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")

$manifestPath = Join-Path $outDir "the-mechanist-build-manifest.txt"
$manifest = @"
The Mechanist packaged jar build manifest
========================================
Built UTC:       $timestamp
Git branch:      $branch
Git source HEAD: $sourceHead
Source jar:      $($sourceJar.FullName)
Output jar:      $JarOutputPath
Main class:      $detectedMain
SHA-256:         $jarHash

Launch commands:
- PACKAGE_client/run-the-mechanist.cmd
- PACKAGE_client/run-the-mechanist.ps1

This jar and launch entrypoint were produced from the current checked-out source by:
Repair-Mechanist-Launch-Package-Commit-Push.ps1
"@
Set-Content -Path $manifestPath -Value $manifest -Encoding UTF8

$hashPath = "$outPath.sha256"
Set-Content -Path $hashPath -Value "$jarHash  $(Split-Path -Leaf $outPath)" -Encoding ASCII

New-Item -ItemType Directory -Force ".github/workflows" | Out-Null
$workflow = @"
name: Build Mechanist Jar

on:
  push:
  pull_request:
  workflow_dispatch:

jobs:
  build-mechanist-jar:
    name: Compile current source into Mechanist jar
    runs-on: ubuntu-latest

    steps:
      - name: Check out source
        uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: maven

      - name: Compile and package source
        run: mvn -B -DskipTests package

      - name: Upload target jars
        uses: actions/upload-artifact@v4
        with:
          name: target-jars
          path: target/*.jar
          if-no-files-found: error
"@
Set-Content -Path ".github/workflows/build-mechanist-jar.yml" -Value $workflow -Encoding UTF8

Write-Host ""
Write-Host "Wrote repaired package launch files:" -ForegroundColor Green
Write-Host "  PACKAGE_client/run-the-mechanist.cmd"
Write-Host "  PACKAGE_client/run-the-mechanist.ps1"
Write-Host "  PACKAGE_client/build/client-entrypoint.properties"
Write-Host "  PACKAGE_client/build/the-mechanist-build-manifest.txt"
Write-Host "  PACKAGE_client/build/the-mechanist.jar.sha256"

Run-Checked "git" @("add", ".github/workflows/build-mechanist-jar.yml")
Run-Checked "git" @("add", "-f", $JarOutputPath)
Run-Checked "git" @("add", "-f", "$JarOutputPath.sha256")
Run-Checked "git" @("add", "-f", "PACKAGE_client/build/client-entrypoint.properties")
Run-Checked "git" @("add", "-f", "PACKAGE_client/build/the-mechanist-build-manifest.txt")
Run-Checked "git" @("add", "PACKAGE_client/run-the-mechanist.cmd")
Run-Checked "git" @("add", "PACKAGE_client/run-the-mechanist.ps1")

& git diff --cached --quiet
$hasStagedChanges = ($LASTEXITCODE -ne 0)

if (-not $hasStagedChanges) {
    Write-Host ""
    Write-Host "No staged changes to commit. Package and launch files already match repository state." -ForegroundColor Yellow
    exit 0
}

Run-Checked "git" @("commit", "-m", $CommitMessage)

if ($NoPush) {
    Write-Host ""
    Write-Host "Committed locally. Push skipped because -NoPush was supplied." -ForegroundColor Yellow
    exit 0
}

$originUrl = (& git remote get-url origin 2>$null)
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($originUrl)) {
    Fail "No git remote named origin is configured. Commit exists locally; push manually after adding a remote."
}

$upstream = (& git rev-parse --abbrev-ref --symbolic-full-name "@{u}" 2>$null)
if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($upstream)) {
    Run-Checked "git" @("push")
} else {
    Run-Checked "git" @("push", "-u", "origin", $branch)
}

Write-Host ""
Write-Host "Complete: built jar, repaired launch entrypoint, committed, and pushed." -ForegroundColor Green
