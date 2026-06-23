param(
    [string]$TargetPath,
    [switch]$ForceHash,
    [switch]$LegacyFullHash
)

$ErrorActionPreference = "Stop"

$toolDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = [System.IO.Path]::GetFullPath((Join-Path $toolDir ".."))

if ([string]::IsNullOrWhiteSpace($TargetPath)) {
    $TargetPath = Join-Path $root "ROOT_docs\REPOSITORY_FILE_MANIFEST.tsv"
}

$targetFull = [System.IO.Path]::GetFullPath($TargetPath)
$rootFull = [System.IO.Path]::GetFullPath($root)
if (-not $targetFull.StartsWith($rootFull, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Manifest target must stay inside the repository: $TargetPath"
}

if (-not $LegacyFullHash) {
    $pythonScript = Join-Path $toolDir "update_repository_file_manifest_incremental.py"
    $relativeTarget = $targetFull.Substring($rootFull.Length).TrimStart([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
    $args = @($pythonScript, "--target", $relativeTarget)
    if ($ForceHash) {
        $args += "--force-hash"
    }
    python @args
    exit $LASTEXITCODE
}

function Convert-ToManifestText {
    param([string]$Value)
    if ($null -eq $Value) {
        return ""
    }
    return ($Value -replace "`t", " " -replace "`r", " " -replace "`n", " ")
}

function Get-RepositoryRelativePath {
    param([string]$FullName)
    $full = [System.IO.Path]::GetFullPath($FullName)
    $relative = $full.Substring($rootFull.Length).TrimStart([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
    return ($relative -replace "\\", "/")
}

function Get-FileKind {
    param([string]$RelativePath, [string]$Extension)
    $path = $RelativePath.ToLowerInvariant()
    $ext = $Extension.ToLowerInvariant()

    if ($path.StartsWith("root_src_assets/")) { return "protected_source_asset" }
    if ($path.StartsWith("package_client/assets/")) { return "client_runtime_asset" }
    if ($path.StartsWith("package_launcher/") -and $path.Contains("/resources/assets/")) { return "launcher_runtime_asset" }
    if ($path.StartsWith("root_docs/")) { return "documentation" }
    if ($path.StartsWith("root_tools/") -or $path.StartsWith("scripts/")) { return "tooling" }
    if ($path.StartsWith("src/")) { return "source_code" }
    if ($path.StartsWith("package_client/")) { return "client_package_file" }
    if ($path.StartsWith("package_launcher/")) { return "launcher_package_file" }
    if ($path.StartsWith("package_installer/")) { return "installer_package_file" }
    if (@(".png", ".jpg", ".jpeg", ".gif", ".bmp", ".ico", ".svg", ".webp").Contains($ext)) { return "image" }
    if (@(".mp3", ".wav", ".ogg", ".flac", ".m4a").Contains($ext)) { return "audio" }
    if (@(".jar", ".zip", ".exe", ".msi", ".dll", ".so", ".dylib", ".class").Contains($ext)) { return "binary" }
    if (@(".md", ".txt", ".csv", ".tsv", ".json", ".xml", ".properties", ".mf", ".conf", ".yml", ".yaml").Contains($ext)) { return "text_data" }
    return "other"
}

$rows = New-Object System.Collections.Generic.List[string]
$rows.Add((@("relative_path", "file_kind", "text_or_binary", "extension", "bytes", "modified_utc", "sha256") -join "`t"))

$files = Get-ChildItem -LiteralPath $rootFull -Recurse -File -Force |
    Where-Object {
        $_.FullName -notmatch "\\.git(\\|$)" -and
        ([System.IO.Path]::GetFullPath($_.FullName) -ne $targetFull)
    } |
    Sort-Object FullName

foreach ($file in $files) {
    $relative = Get-RepositoryRelativePath $file.FullName
    $extension = $file.Extension.TrimStart(".").ToLowerInvariant()
    $kind = Get-FileKind $relative $file.Extension

    $binaryExtensions = @(".png", ".jpg", ".jpeg", ".gif", ".bmp", ".ico", ".webp", ".mp3", ".wav", ".ogg", ".flac", ".m4a", ".jar", ".zip", ".exe", ".msi", ".dll", ".so", ".dylib", ".class")
    $textOrBinary = if ($binaryExtensions.Contains($file.Extension.ToLowerInvariant())) { "binary" } else { "text_or_unknown" }
    $hash = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    $modified = $file.LastWriteTimeUtc.ToString("yyyy-MM-ddTHH:mm:ssZ")

    $rows.Add((
        (Convert-ToManifestText $relative),
        (Convert-ToManifestText $kind),
        (Convert-ToManifestText $textOrBinary),
        (Convert-ToManifestText $extension),
        $file.Length,
        (Convert-ToManifestText $modified),
        (Convert-ToManifestText $hash)
    ) -join "`t")
}

$selfRelative = Get-RepositoryRelativePath $targetFull
$rows.Add((
    (Convert-ToManifestText $selfRelative),
    "generated_repository_manifest",
    "text_or_unknown",
    "tsv",
    "GENERATED",
    (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ"),
    "SELF_GENERATED"
) -join "`t")

$targetDir = Split-Path -Parent $targetFull
New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
Set-Content -LiteralPath $targetFull -Value $rows -Encoding UTF8

Write-Host ("Wrote {0} indexed file rows to {1}" -f ($rows.Count - 1), $targetFull)
