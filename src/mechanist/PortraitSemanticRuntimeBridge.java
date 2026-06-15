package mechanist;

import mechanist.assets.AssetManager;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Optional;

final class PortraitSemanticRuntimeBridge {
    static final String VERSION = "portrait-semantic-runtime-bridge-0.4";

    private PortraitSemanticRuntimeBridge() {}

    static BufferedImage playerPortrait(LegacyImageSurface images, Candidate candidate) {
        if (images == null) return null;
        if (candidate == null) return images.getPlayerPortrait(null);
        Optional<String> semanticId = PortraitSemanticIdentityResolver.playerAssetId(
                Path.of("."), AssetManager.registry(), candidate.portraitIndex);
        if (semanticId.isPresent()) {
            BufferedImage semantic = images.getSemanticAssetImage(semanticId.get());
            if (semantic != null) return semantic;
        }
        return images.getPlayerPortrait(candidate);
    }

    static BufferedImage playerPortrait(ImageCache media, int portraitSheet, int portraitIndex) {
        if (media == null) return null;
        Optional<String> semanticId = PortraitSemanticIdentityResolver.playerAssetId(
                Path.of("."), AssetManager.registry(), portraitIndex);
        if (semanticId.isPresent()) {
            BufferedImage semantic = media.getSemanticAssetImage(semanticId.get());
            if (semantic != null) return semantic;
        }
        return media.getPortrait(portraitSheet, portraitIndex);
    }

    static BufferedImage npcPortrait(ImageCache media, NpcEntity npc) {
        if (media == null) return null;
        if (npc == null) return media.getNpcPortrait(0);
        return media.getNpcPortraitFor(npc);
    }
}
