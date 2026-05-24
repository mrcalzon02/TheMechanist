package mechanist;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Data-only mod payloads that the engine may interpret without granting low-level hooks. */
sealed interface ModContentPayload permits ModContentPayload.SectorPayload, ModContentPayload.RoomPayload, ModContentPayload.FactionPayload, ModContentPayload.ItemPayload, ModContentPayload.KnowledgePayload, ModContentPayload.LorePayload {
    String id();

    record SectorPayload(String id, int x, int y, int z, List<String> hazards, String controllingFaction) implements ModContentPayload {
        public SectorPayload {
            id = requireId(id);
            hazards = List.copyOf(Objects.requireNonNullElse(hazards, List.of()));
            controllingFaction = clean(controllingFaction, "neutral");
        }
    }

    record RoomPayload(String id, int width, int height, boolean oxygenSealed, int securityRating, List<String> placementNodes) implements ModContentPayload {
        public RoomPayload {
            id = requireId(id);
            if (width <= 0 || width > 256 || height <= 0 || height > 256) throw new IllegalArgumentException("room dimensions must be between 1 and 256");
            securityRating = Math.max(0, Math.min(10, securityRating));
            placementNodes = List.copyOf(Objects.requireNonNullElse(placementNodes, List.of()));
        }
    }

    record FactionPayload(String id, String alignment, Map<String, Integer> resources, int aggression, List<String> culturalTraits) implements ModContentPayload {
        public FactionPayload {
            id = requireId(id);
            alignment = clean(alignment, "neutral");
            resources = Map.copyOf(Objects.requireNonNullElse(resources, Map.of()));
            aggression = Math.max(0, Math.min(100, aggression));
            culturalTraits = List.copyOf(Objects.requireNonNullElse(culturalTraits, List.of()));
        }
    }

    record ItemPayload(String id, int techTier, double massKg, int durabilityMax, List<String> components) implements ModContentPayload {
        public ItemPayload {
            id = requireId(id);
            techTier = Math.max(0, Math.min(12, techTier));
            if (!Double.isFinite(massKg) || massKg < 0.0 || massKg > 1_000_000.0) throw new IllegalArgumentException("massKg outside safe bounds");
            durabilityMax = Math.max(1, Math.min(1_000_000, durabilityMax));
            components = List.copyOf(Objects.requireNonNullElse(components, List.of()));
        }
    }

    record KnowledgePayload(String id, List<String> prerequisites, List<String> unlockBlueprints) implements ModContentPayload {
        public KnowledgePayload {
            id = requireId(id);
            prerequisites = List.copyOf(Objects.requireNonNullElse(prerequisites, List.of()));
            unlockBlueprints = List.copyOf(Objects.requireNonNullElse(unlockBlueprints, List.of()));
        }
    }

    record LorePayload(String id, String title, String body, List<String> tags, List<String> crossLinks) implements ModContentPayload {
        public LorePayload {
            id = requireId(id);
            title = clean(title, id);
            body = Objects.requireNonNullElse(body, "");
            if (body.length() > 100_000) throw new IllegalArgumentException("lore body exceeds safe editor limit");
            tags = List.copyOf(Objects.requireNonNullElse(tags, List.of()));
            crossLinks = List.copyOf(Objects.requireNonNullElse(crossLinks, List.of()));
        }
    }

    static String requireId(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("payload id is required");
        String clean = id.trim();
        if (!clean.matches("[A-Za-z0-9._:-]{3,96}")) throw new IllegalArgumentException("payload id contains illegal characters: " + id);
        return clean;
    }

    static String clean(String value, String fallback) {
        String cleaned = value == null || value.isBlank() ? fallback : value.trim();
        if (cleaned.length() > 256) throw new IllegalArgumentException("text field exceeds 256 characters");
        return cleaned;
    }
}
