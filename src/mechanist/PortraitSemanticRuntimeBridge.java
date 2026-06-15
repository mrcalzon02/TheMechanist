package mechanist;

import mechanist.assets.AssetManager;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * Narrow runtime bridge between the semantic portrait partition authority and
 * the existing ImageCache portrait loaders.
 *
 * <p>This class does not own portrait data. It tries a registry-backed semantic
 * portrait first when the actor has a safe persistent identity contract, then
 * delegates to the established ImageCache behavior. Restricted/nonhuman actors
 * and explicit name-locked profiles remain under their existing specialized
 * loaders until their semantic partitions receive dedicated runtime rules.</p>
 */
final class PortraitSemanticRuntimeBridge {
    static final String VERSION = "portrait-semantic-runtime-bridge-0.2";

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

        // Existing specialized loaders remain authoritative for identities that
        // ordinary-human semantic pools must never absorb.
        if (hasText(npc.nameLockedProfileKey)
                || npc.isAnimalActor()
                || restrictedFaction(npc.faction)) {
            return media.getNpcPortraitFor(npc);
        }

        String identityKey = persistentNpcIdentity(npc);
        Optional<String> semanticId = PortraitSemanticAssetAuthority.runtimeNpcAssetId(
                Path.of("."),
                AssetManager.registry(),
                identityKey
        );
        if (semanticId.isPresent()) {
            BufferedImage semantic = media.getSemanticAssetImage(semanticId.get());
            if (semantic != null) return semantic;
        }
        return media.getNpcPortraitFor(npc);
    }

    static String persistentNpcIdentity(NpcEntity npc) {
        if (npc == null) return "";
        String faction = npc.faction == null ? Faction.NONE.name() : npc.faction.name();
        return normalize(
                safe(npc.name) + "|"
                        + safe(npc.role) + "|"
                        + faction + "|"
                        + safe(npc.creatureKind) + "|"
                        + safe(npc.animalProfileId) + "|portrait-index-"
                        + npc.portraitIndex
        );
    }

    static boolean semanticNpcEligible(NpcEntity npc) {
        return npc != null
                && !hasText(npc.nameLockedProfileKey)
                && !npc.isAnimalActor()
                && !restrictedFaction(npc.faction)
                && !persistentNpcIdentity(npc).isBlank();
    }

    private static boolean restrictedFaction(Faction faction) {
        if (faction == null) return false;
        String name = faction.name().toLowerCase(Locale.ROOT);
        return faction == Faction.MUTANT
                || faction == Faction.ROGUE_MACHINE
                || name.contains("xenos")
                || name.contains("daemon")
                || name.contains("beast");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("[^a-z0-9| ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
