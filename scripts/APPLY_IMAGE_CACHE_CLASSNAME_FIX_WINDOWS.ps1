$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$panelPath = Join-Path $root 'src\mechanist\LegacyPanelContext.java'
$mediaScriptPath = Join-Path $root 'scripts\APPLY_MEDIA_RUNTIME_IMAGE_SURFACE_REWIRE_WINDOWS.ps1'
foreach ($p in @($panelPath, $mediaScriptPath)) { if (-not (Test-Path -LiteralPath $p -PathType Leaf)) { throw "Missing $p" } }

$panel = Get-Content -LiteralPath $panelPath -Raw
$panel = $panel.Replace('private final MediaRuntimeFramework media = new MediaRuntimeFramework();', 'private final ImageCache media = new ImageCache();')
Set-Content -LiteralPath $panelPath -Value $panel

$script = Get-Content -LiteralPath $mediaScriptPath -Raw
$script = $script.Replace('private final MediaRuntimeFramework media = new MediaRuntimeFramework();', 'private final ImageCache media = new ImageCache();')
Set-Content -LiteralPath $mediaScriptPath -Value $script

Write-Host 'Replaced MediaRuntimeFramework reference with ImageCache in LegacyImageSurface rewire.'
