$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$panelPath = Join-Path $root 'src\mechanist\LegacyPanelContext.java'
$packageScriptPath = Join-Path $root 'scripts\PACKAGE_CLIENT_WINDOWS.ps1'
$bootSmokePath = Join-Path $root 'scripts\BOOT_SMOKE_PACKAGE_CLIENT_WINDOWS.ps1'
foreach ($p in @($panelPath, $packageScriptPath, $bootSmokePath)) { if (-not (Test-Path -LiteralPath $p -PathType Leaf)) { throw "Missing $p" } }

$panel = Get-Content -LiteralPath $panelPath -Raw
# The package copier places files under assets\compiled_assets\32px\..., so the resolver must keep the 32px folder in the relative path.
$panel = $panel.Replace('        p = p.replaceFirst("^" + java.util.regex.Pattern.quote(resolution + "px/") , "");' + "`r`n", '')
$panel = $panel.Replace('        p = p.replaceFirst("^" + java.util.regex.Pattern.quote(resolution + "px/") , "");' + "`n", '')
$panel = $panel.Replace('        p = p.replaceFirst("^" + java.util.regex.Pattern.quote(resolution + "px/") , "");', '')
Set-Content -LiteralPath $panelPath -Value $panel

$pkg = Get-Content -LiteralPath $packageScriptPath -Raw
$pkg = $pkg.Replace('-Dmechanist.assetTier=low_32 -cp', '-Dmechanist.assetTier=low_32 -Dmechanist.assetResolution=32 -cp')
$pkg = $pkg.Replace('-Dmechanist.assetResolution=32 -Dmechanist.assetResolution=32', '-Dmechanist.assetResolution=32')
Set-Content -LiteralPath $packageScriptPath -Value $pkg

$boot = Get-Content -LiteralPath $bootSmokePath -Raw
$boot = $boot.Replace('-Dmechanist.assetTier=low_32 -cp', '-Dmechanist.assetTier=low_32 -Dmechanist.assetResolution=32 -cp')
$boot = $boot.Replace("'-Dmechanist.assetTier=low_32', '-cp'", "'-Dmechanist.assetTier=low_32', '-Dmechanist.assetResolution=32', '-cp'")
$boot = $boot.Replace('-Dmechanist.assetResolution=32 -Dmechanist.assetResolution=32', '-Dmechanist.assetResolution=32')
$boot = $boot.Replace("'-Dmechanist.assetResolution=32', '-Dmechanist.assetResolution=32'", "'-Dmechanist.assetResolution=32'")
Set-Content -LiteralPath $bootSmokePath -Value $boot

Write-Host 'Applied package asset resolver path fix and launcher assetResolution flag.'
