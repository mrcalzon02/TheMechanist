param(
    [string]$OutputRoot = "build\local-package-seed",
    [string]$Version = "local-dev",
    [switch]$IncludeAssets
)

$ErrorActionPreference = "Stop"

function Resolve-InWorkspace([string]$Path) {
    $root = [System.IO.Path]::GetFullPath((Get-Location).Path)
    $full = [System.IO.Path]::GetFullPath((Join-Path $root $Path))
    if (-not $full.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Path escapes workspace: $Path"
    }
    return $full
}

function Need([string]$Name) {
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if ($null -eq $cmd) { throw "Required tool was not found on PATH: $Name" }
    return $cmd.Source
}

function Current-Platform {
    $os = [System.Runtime.InteropServices.RuntimeInformation]::OSDescription.ToLowerInvariant()
    $arch = [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture.ToString().ToLowerInvariant()
    $cpu = if ($arch.Contains("64") -or $arch -eq "x64" -or $arch -eq "x86_64") { "x64" } else { ($arch -replace "[^a-z0-9]+", "") }
    if ($os.Contains("windows")) { return "windows-$cpu" }
    if ($os.Contains("linux")) { return "linux-$cpu" }
    if ($os.Contains("darwin") -or $os.Contains("mac")) { return "macos-$cpu" }
    return (($os -replace "[^a-z0-9]+", "") + "-$cpu")
}

function Get-RelativePathPortable([string]$Base, [string]$Path) {
    $baseUri = [Uri](([System.IO.Path]::GetFullPath($Base).TrimEnd('\','/')) + [System.IO.Path]::DirectorySeparatorChar)
    $pathUri = [Uri][System.IO.Path]::GetFullPath($Path)
    return [Uri]::UnescapeDataString($baseUri.MakeRelativeUri($pathUri).ToString()).Replace("/", [System.IO.Path]::DirectorySeparatorChar)
}

function Get-Sha256([string]$Path) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $stream = [System.IO.File]::OpenRead($Path)
        try {
            return ([System.BitConverter]::ToString($sha.ComputeHash($stream))).Replace("-", "").ToLowerInvariant()
        } finally {
            $stream.Dispose()
        }
    } finally {
        $sha.Dispose()
    }
}

function Write-JarManifest([string]$Path, [string]$MainClass, [string]$Title) {
    @(
        "Manifest-Version: 1.0"
        "Main-Class: $MainClass"
        "Implementation-Title: $Title"
        "Implementation-Version: $Version"
        ""
    ) | Set-Content -LiteralPath $Path -Encoding ASCII
}

function Add-ZipEntryFromText($Zip, [string]$EntryName, [string]$Text) {
    $entry = $Zip.CreateEntry($EntryName.Replace("\", "/"))
    $stream = $entry.Open()
    try {
        $bytes = [System.Text.Encoding]::ASCII.GetBytes($Text)
        $stream.Write($bytes, 0, $bytes.Length)
    } finally {
        $stream.Dispose()
    }
}

function Add-ZipEntryFromFile($Zip, [string]$EntryName, [string]$FilePath) {
    $entry = $Zip.CreateEntry($EntryName.Replace("\", "/"))
    $out = $entry.Open()
    try {
        $in = [System.IO.File]::OpenRead($FilePath)
        try {
            $in.CopyTo($out)
        } finally {
            $in.Dispose()
        }
    } finally {
        $out.Dispose()
    }
}

function New-JarFromDirectory([string]$JarPath, [string]$SourceDir, [string]$ManifestPath) {
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    if (Test-Path -LiteralPath $JarPath) {
        Remove-Item -LiteralPath $JarPath -Force
    }
    $zip = [System.IO.Compression.ZipFile]::Open($JarPath, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        Add-ZipEntryFromText $zip "META-INF/MANIFEST.MF" ([System.IO.File]::ReadAllText($ManifestPath))
        Get-ChildItem -LiteralPath $SourceDir -Recurse -File | Sort-Object FullName | ForEach-Object {
            $relative = Get-RelativePathPortable $SourceDir $_.FullName
            if ($relative.Replace("\", "/") -ieq "META-INF/MANIFEST.MF") { return }
            Add-ZipEntryFromFile $zip $relative $_.FullName
        }
    } finally {
        $zip.Dispose()
    }
}

$java = Need "java"
$javac = Need "javac"

$repoRoot = [System.IO.Path]::GetFullPath((Get-Location).Path)
$outRoot = Resolve-InWorkspace $OutputRoot
$workRoot = Resolve-InWorkspace "build\gate4-local-package"
$classes = Join-Path $workRoot "classes"
$launcherClasses = Join-Path $workRoot "launcher-classes"
$manifestWork = Join-Path $workRoot "manifests"

foreach ($target in @($outRoot, $workRoot)) {
    $resolved = [System.IO.Path]::GetFullPath($target)
    if (-not $resolved.StartsWith($repoRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to clear path outside workspace: $target"
    }
    if (Test-Path -LiteralPath $target) {
        Remove-Item -LiteralPath $target -Recurse -Force
    }
}

New-Item -ItemType Directory -Force -Path $classes, $launcherClasses, $manifestWork | Out-Null

$libJars = @(Get-ChildItem -LiteralPath (Join-Path $repoRoot "lib") -File -Filter *.jar -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName })
$clientCp = if ($libJars.Count -gt 0) { [string]::Join([System.IO.Path]::PathSeparator, $libJars) } else { "" }
$sourceFiles = @(Get-ChildItem -LiteralPath (Join-Path $repoRoot "src") -Recurse -File -Filter *.java | ForEach-Object { $_.FullName })
if ($sourceFiles.Count -eq 0) { throw "No client/server Java sources found." }

Write-Host "Compiling client/server sources with javac --release 17..."
if ($clientCp.Length -gt 0) {
    & $javac --release 17 -cp $clientCp -d $classes $sourceFiles
} else {
    & $javac --release 17 -d $classes $sourceFiles
}
if ($LASTEXITCODE -ne 0) { throw "Client/server compilation failed." }

$launcherSources = @(Get-ChildItem -LiteralPath (Join-Path $repoRoot "launcher\java\src\main\java") -Recurse -File -Filter *.java | ForEach-Object { $_.FullName })
if ($launcherSources.Count -eq 0) { throw "No launcher Java sources found." }

Write-Host "Compiling launcher sources with javac --release 17..."
& $javac --release 17 -d $launcherClasses $launcherSources
if ($LASTEXITCODE -ne 0) { throw "Launcher compilation failed." }

$launcherResources = Join-Path $repoRoot "launcher\java\src\main\resources"
if (Test-Path -LiteralPath $launcherResources) {
    Get-ChildItem -LiteralPath $launcherResources -Force | ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination $launcherClasses -Recurse -Force
    }
}

$clientDir = Join-Path $outRoot "packages\client"
$serverDir = Join-Path $outRoot "packages\server"
$launcherDir = Join-Path $outRoot "packages\launcher"
$supportDir = Join-Path $outRoot "packages\support\lib"
$manifestDir = Join-Path $outRoot "manifests"
New-Item -ItemType Directory -Force -Path $clientDir, $serverDir, $launcherDir, $supportDir, $manifestDir | Out-Null

$clientJar = Join-Path $clientDir "TheMechanist.jar"
$serverJar = Join-Path $serverDir "TheMechanistServer.jar"
$launcherJar = Join-Path $launcherDir "MechanistLauncher.jar"
$clientManifest = Join-Path $manifestWork "client.mf"
$serverManifest = Join-Path $manifestWork "server.mf"
$launcherManifest = Join-Path $manifestWork "launcher.mf"

Write-JarManifest $clientManifest "mechanist.TheMechanist" "The Mechanist Client"
Write-JarManifest $serverManifest "mechanist.MechanistServerMain" "The Mechanist Server"
Write-JarManifest $launcherManifest "mechanist.launcher.MechanistLauncherApp" "The Mechanist Launcher"

New-JarFromDirectory $clientJar $classes $clientManifest
New-JarFromDirectory $serverJar $classes $serverManifest
New-JarFromDirectory $launcherJar $launcherClasses $launcherManifest

foreach ($lib in $libJars) {
    Copy-Item -LiteralPath $lib -Destination (Join-Path $supportDir ([System.IO.Path]::GetFileName($lib))) -Force
}

if ($IncludeAssets) {
    $assetSource = Join-Path $repoRoot "client\assets"
    if (Test-Path -LiteralPath $assetSource) {
        Copy-Item -LiteralPath $assetSource -Destination $clientDir -Recurse -Force
    }
}

$platform = Current-Platform
$supportEntries = @(Get-ChildItem -LiteralPath $supportDir -File -Filter *.jar -ErrorAction SilentlyContinue | Sort-Object Name | ForEach-Object {
    $relative = (Get-RelativePathPortable $outRoot $_.FullName).Replace("\", "/")
    '    {"path": "' + $relative + '", "sha256": "' + (Get-Sha256 $_.FullName) + '", "size": ' + $_.Length + '}'
})
$supportJson = if ($supportEntries.Count -gt 0) { [string]::Join(",`n", $supportEntries) } else { "" }

$manifestPath = Join-Path $manifestDir "$platform-runtime-manifest.json"
$clientRel = "packages/client/TheMechanist.jar"
$serverRel = "packages/server/TheMechanistServer.jar"
$launcherRel = "packages/launcher/MechanistLauncher.jar"

@"
{
  "schema": 2,
  "distribution_model": "installer-thin-launcher-client-server",
  "version": "$Version",
  "platform": "$platform",
  "launcher": { "path": "$launcherRel", "sha256": "$(Get-Sha256 $launcherJar)", "size": $((Get-Item -LiteralPath $launcherJar).Length), "main_class": "mechanist.launcher.MechanistLauncherApp" },
  "client": { "path": "$clientRel", "sha256": "$(Get-Sha256 $clientJar)", "size": $((Get-Item -LiteralPath $clientJar).Length), "main_class": "mechanist.TheMechanist", "launcher_main_class": "mechanist.launcher.ThinLauncherMain" },
  "client_assets": { "root": "packages/client/assets", "staged": $($IncludeAssets.IsPresent.ToString().ToLowerInvariant()) },
  "server": { "path": "$serverRel", "sha256": "$(Get-Sha256 $serverJar)", "size": $((Get-Item -LiteralPath $serverJar).Length), "main_class": "mechanist.MechanistServerMain" },
  "support_libraries": [
$supportJson
  ]
}
"@ | Set-Content -LiteralPath $manifestPath -Encoding UTF8

Write-Host "Scanning staged jars for Java 17 classfile compatibility..."
& (Join-Path $repoRoot "tools\build\verify_java17_classfiles.ps1") -Paths @($clientJar, $serverJar, $launcherJar)
if ($LASTEXITCODE -ne 0) { throw "Java 17 classfile scan failed." }

Write-Host "Local package seed staged:"
Write-Host "  $outRoot"
Write-Host "Manifest:"
Write-Host "  $manifestPath"
