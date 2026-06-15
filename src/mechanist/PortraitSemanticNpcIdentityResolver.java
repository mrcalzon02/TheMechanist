package mechanist;

import mechanist.assets.AssetRegistry;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * Converts persistent NPC identity context into one exact semantic portrait partition.
 * The mapping mirrors the established legacy image-cache routing while keeping
 * restricted and name-locked permissions explicit for the partition resolver.
 */
final class PortraitSemanticNpcIdentityResolver {
    static final String VERSION = "portrait-semantic-npc-identity-0.2";

    record Context(
            String nameLockedProfileKey,
            String creatureKind,
            String animalProfileId,
            String role,
            String name,
            String factionKey,
            int portraitIndex,
            boolean animalActor,
            boolean childActor
    ) {}

    record Selection(
            String partitionKey,
            String stableIdentity,
            boolean allowRestricted,
            boolean allowNameLocked
    ) {}

    private PortraitSemanticNpcIdentityResolver() {}

    static Optional<String> assetId(
            Path projectRoot,
            AssetRegistry registry,
            NpcEntity npc
    ) {
        return selectionFor(npc).flatMap(selection ->
                PortraitSemanticPartitionResolver.assetId(
                        projectRoot,
                        registry,
                        selection.partitionKey(),
                        selection.stableIdentity(),
                        selection.allowRestricted(),
                        selection.allowNameLocked()));
    }

    static Optional<Selection> selectionFor(NpcEntity npc) {
        if (npc == null) return Optional.empty();
        String faction = npc.faction == null ? "none" : npc.faction.name();
        return selectionFor(new Context(
                npc.nameLockedProfileKey,
                npc.creatureKind,
                npc.animalProfileId,
                npc.role,
                npc.name,
                faction,
                npc.portraitIndex,
                npc.isAnimalActor(),
                npc.isChildActor()));
    }

    static Optional<Selection> selectionFor(Context context) {
        if (context == null) return Optional.empty();

        String lockedKey = safe(context.nameLockedProfileKey());
        String identity = stableIdentity(context);
        if (!lockedKey.isBlank()) {
            return Optional.of(new Selection(
                    "name_locked_profile",
                    "name-locked|" + normalize(lockedKey),
                    false,
                    true));
        }

        String roleText = normalize(safe(context.creatureKind()) + " "
                + safe(context.animalProfileId()) + " "
                + safe(context.role()) + " "
                + safe(context.name()));
        String faction = PortraitSemanticPartitionResolver.canonicalPartitionKey(context.factionKey());

        if (containsAny(roleText, "servant", "chef", "butler", "household",
                "laundry", "retainer", "pantry")) {
            return ordinary("servants_butlers_and_chefs", identity);
        }
        if (containsAny(roleText, "medicae", "hospital", "clinic")) {
            return ordinary(faction.equals("sororitas") ? "sisters_hospital" : "hospital", identity);
        }

        if (context.animalActor()) {
            if (containsAny(roleText, "farm", "hog", "goat", "fowl", "grub")) {
                return restricted("farm_beasts", identity);
            }
            if (containsAny(roleText, "sump", "sewer", "swamp", "eel", "leech",
                    "corpse feeder", "fungus")) {
                return restricted("exotic_pets_swamp_creatures", identity);
            }
            if (containsAny(roleText, "kennel", "mastiff", "hound", "guard", "pet",
                    "cat", "rat", "lizard", "moth", "glowfish")) {
                return restricted("pets", identity);
            }
            return restricted(selectByPortraitIndex(
                    context.portraitIndex(),
                    "pets",
                    "exotic_pets_swamp_creatures",
                    "farm_beasts"), identity);
        }

        if (context.childActor()) {
            return ordinary("schola_children", identity);
        }

        if (faction.equals("administratum") || faction.equals("inn")) {
            return ordinary("administratum", identity);
        }
        if (faction.equals("arbites")) {
            return ordinary("enforcer_arebites", identity);
        }
        if (faction.equals("imperial_guard")) {
            return ordinary("pdf_military", identity);
        }
        if (faction.equals("ministorum")) {
            return ordinary("ecclesiarch", identity);
        }
        if (faction.equals("sororitas")) {
            return ordinary("sisters_hospital", identity);
        }
        if (faction.equals("mechanicus") || faction.startsWith("mechanicus")) {
            return ordinary("mechanicus", identity);
        }
        if (faction.equals("rogue_machine")) {
            return restricted("rogue_automata_servitors", identity);
        }
        if (faction.equals("mutant")) {
            return restricted("mutants", identity);
        }
        if (faction.equals("cultist")) {
            return ordinary("cultists", identity);
        }
        if (faction.equals("heretic")) {
            return ordinary("heretics", identity);
        }
        if (faction.startsWith("ganger") || faction.equals("bandit")) {
            return ordinary("gangers", identity);
        }
        if (faction.startsWith("noble") || faction.equals("noble")) {
            return ordinary("nobles", identity);
        }
        if (faction.startsWith("hiver") || faction.equals("hiver")
                || faction.equals("scavenger") || faction.equals("none")) {
            return ordinary(selectByPortraitIndex(
                    context.portraitIndex(),
                    "administratum",
                    "gangers",
                    "servants_butlers_and_chefs"), identity);
        }

        return ordinary(selectByPortraitIndex(
                context.portraitIndex(),
                "administratum",
                "nobles",
                "gangers"), identity);
    }

    private static Optional<Selection> ordinary(String partitionKey, String identity) {
        return Optional.of(new Selection(partitionKey, identity, false, false));
    }

    private static Optional<Selection> restricted(String partitionKey, String identity) {
        return Optional.of(new Selection(partitionKey, identity, true, false));
    }

    private static String selectByPortraitIndex(int portraitIndex, String... keys) {
        if (keys == null || keys.length == 0) return "administratum";
        int selector = portraitIndex / 7;
        return keys[Math.floorMod(selector, keys.length)];
    }

    private static String stableIdentity(Context context) {
        return normalize(safe(context.name()) + "|"
                + safe(context.role()) + "|"
                + safe(context.creatureKind()) + "|"
                + safe(context.animalProfileId()) + "|"
                + safe(context.factionKey()) + "|"
                + context.portraitIndex());
    }

    private static boolean containsAny(String haystack, String... needles) {
        if (haystack == null || haystack.isBlank() || needles == null) return false;
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && haystack.contains(normalize(needle))) {
                return true;
            }
        }
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalize(String value) {
        return safe(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
