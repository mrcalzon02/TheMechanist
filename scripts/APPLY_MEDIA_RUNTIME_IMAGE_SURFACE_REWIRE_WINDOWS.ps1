$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$panelPath = Join-Path $root 'src\mechanist\LegacyPanelContext.java'
if (-not (Test-Path -LiteralPath $panelPath -PathType Leaf)) { throw "Missing $panelPath" }

$text = Get-Content -LiteralPath $panelPath -Raw
$new = @'
final class LegacyImageSurface {
    private final MediaRuntimeFramework media = new MediaRuntimeFramework();
    private boolean loaded;
    private GameOptions lastOptions;

    void reloadArtQuality(GameOptions options) {
        loaded = false;
        lastOptions = options;
        clearMediaCaches();
        ensureLoaded(options);
    }

    BufferedImage get(String key) {
        if (key == null || key.isBlank()) return null;
        ensureLoaded(lastOptions);
        return media.cache.get(key);
    }

    BufferedImage getTile(char tile) {
        ensureLoaded(lastOptions);
        return null;
    }

    BufferedImage getNpcPortraitFor(Object npc) {
        ensureLoaded(lastOptions);
        return media.getNpcPortrait(npc == null ? 0 : Math.abs(npc.hashCode()));
    }

    java.util.ArrayList<String> loadIntroCrawlLines() {
        ensureLoaded(lastOptions);
        return media.loadIntroCrawlLines();
    }

    private void ensureLoaded(GameOptions options) {
        if (loaded) return;
        clearMediaCaches();
        media.load(options == null ? new GameOptions() : options);
        loaded = true;
    }

    private void clearMediaCaches() {
        media.cache.clear();
        media.bootFrames.clear();
        media.portraitSheets.clear();
        media.playerHumanPortraitCells.clear();
        media.npcPortraitCells.clear();
        media.nameLockedProfilePortraits.clear();
        media.npcPortraitRanges.clear();
        media.portraitProfiles.clear();
        media.semanticAssetImageCache.clear();
    }
}
'@
$pattern = '(?s)final class LegacyImageSurface \{.*?\}\s*\r?\n\r?\nfinal class LegacyFirstPersonRenderViewport'
if ($text -notmatch $pattern) { throw 'Could not locate LegacyImageSurface block.' }
$text = [regex]::Replace($text, $pattern, $new + "`r`n`r`nfinal class LegacyFirstPersonRenderViewport", 1)
Set-Content -LiteralPath $panelPath -Value $text
Write-Host 'Rewired LegacyImageSurface to delegate to MediaRuntimeFramework.'
