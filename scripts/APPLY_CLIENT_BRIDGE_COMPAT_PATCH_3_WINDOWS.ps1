$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$panelPath = Join-Path $root 'src\mechanist\LegacyPanelContext.java'
if (-not (Test-Path -LiteralPath $panelPath -PathType Leaf)) { throw "Missing $panelPath" }

$panel = Get-Content -LiteralPath $panelPath -Raw

# The desktop launcher constructs GamePanel with RuntimeProfile, while the bridge
# already has a no-arg/JVM-config constructor path.  Preserve the launcher shape
# without forcing RuntimeProfile into the JVM config field.
if ($panel -notmatch 'GamePanel\(RuntimeProfile runtimeProfile\)') {
    $needle = @'
    GamePanel() {}
    GamePanel(JvmRuntimeProfileAuthority.RuntimeConfig runtimeProfile) {
        if (runtimeProfile != null) this.jvmRuntimeProfile = runtimeProfile;
    }
'@
    $replacement = @'
    GamePanel() {}
    GamePanel(RuntimeProfile runtimeProfile) {
        if (runtimeProfile != null) logEvent("Runtime profile attached: " + runtimeProfile.compactLine());
    }
    GamePanel(JvmRuntimeProfileAuthority.RuntimeConfig runtimeProfile) {
        if (runtimeProfile != null) this.jvmRuntimeProfile = runtimeProfile;
    }
'@
    if ($panel.Contains($needle)) {
        $panel = $panel.Replace($needle, $replacement)
    } else {
        $anchor = '    void runGuarded(String tag, String reason, Runnable body)'
        $idx = $panel.IndexOf($anchor, [System.StringComparison]::Ordinal)
        if ($idx -lt 0) { throw "Could not find constructor insertion anchor." }
        $panel = $panel.Substring(0, $idx) + $replacement + "`r`n" + $panel.Substring($idx)
    }
}

# WorldAtlas does not expose currentZoneType() in the current shard.  The bridge
# still records a stable visited-zone instance key and only records a type when
# the current World exposes one through its public field shape in later builds.
$panel = $panel.Replace('        if (atlas != null && atlas.currentZoneType() != null) visitedZoneTypes.add(atlas.currentZoneType());' + "`r`n", '')
$panel = $panel.Replace('        if (atlas != null && atlas.currentZoneType() != null) visitedZoneTypes.add(atlas.currentZoneType());' + "`n", '')
$panel = $panel.Replace('        if (atlas != null && atlas.currentZoneType() != null) visitedZoneTypes.add(atlas.currentZoneType());', '')

Set-Content -LiteralPath $panelPath -Value $panel
Write-Host 'Applied client bridge compatibility patch 3.'
