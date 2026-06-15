package mechanist;

import mechanist.assets.AssetManager;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Narrow runtime bridge between semantic portrait IDs and the established
 * ImageCache loaders.
 *
 * <p>Player portraits use the persistent portrait index against the ordered,
 * active-registry Humans8x8 pool, then retain the legacy sheet loader as a
 * compatibility fallback. NPC portraits deliberately remain under the existing
 * identity-aware ImageCache authority until their persistent identity and
 * restricted-family routing rules are migrated as a separately verified step.</p>
 */
final class PortraitSemanticRuntimeBridge {
    static final String VERSION = "portrait-semantic-runtime-bridge-0.3-player-safe";

    private PortraitSemanticRuntimeBridge() {}

    static BufferedImage playerPortrait(
            ImageCache media,
            int portraitSheet,
            int portraitIndex
    ) {
        if (media == null) return null;
        Optional<String> semanticId = PortraitSemanticIdentityResolver.playerAssetId(
                Path.of("."),
                AssetManager.registry(),
                portraitIndex
        );
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
