package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Converts loosely parsed mod metadata into sealed, immutable, data-only payloads. */
final class ModDataMappingVerifier {
    ModContentPayload map(String type, Map<String, ?> raw) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(raw, "raw");
        return switch (type.trim().toLowerCase()) {
            case "sector" -> new ModContentPayload.SectorPayload(string(raw, "id"), integer(raw, "x", 0), integer(raw, "y", 0), integer(raw, "z", 0), stringList(raw, "hazards"), string(raw, "controllingFaction", "neutral"));
            case "room" -> new ModContentPayload.RoomPayload(string(raw, "id"), integer(raw, "width", 1), integer(raw, "height", 1), bool(raw, "oxygenSealed", false), integer(raw, "securityRating", 0), stringList(raw, "placementNodes"));
            case "faction" -> new ModContentPayload.FactionPayload(string(raw, "id"), string(raw, "alignment", "neutral"), integerMap(raw, "resources"), integer(raw, "aggression", 0), stringList(raw, "culturalTraits"));
            case "item" -> new ModContentPayload.ItemPayload(string(raw, "id"), integer(raw, "techTier", 0), decimal(raw, "massKg", 0), integer(raw, "durabilityMax", 1), stringList(raw, "components"));
            case "knowledge" -> new ModContentPayload.KnowledgePayload(string(raw, "id"), stringList(raw, "prerequisites"), stringList(raw, "unlockBlueprints"));
            case "lore", "infopedia" -> new ModContentPayload.LorePayload(string(raw, "id"), string(raw, "title", "Untitled"), string(raw, "body", ""), stringList(raw, "tags"), stringList(raw, "crossLinks"));
            default -> throw new IllegalArgumentException("unknown mod payload type: " + type);
        };
    }

    void verifyEngineAccepts(ModContentPayload payload) {
        Objects.requireNonNull(payload, "payload");
        if (payload instanceof ModContentPayload.SectorPayload p) {
            if (p.hazards().size() > 64) throw new IllegalArgumentException("sector payload has too many hazards");
        } else if (payload instanceof ModContentPayload.RoomPayload p) {
            long tiles = (long) p.width() * (long) p.height();
            if (tiles > 65_536L) throw new IllegalArgumentException("room payload exceeds maximum tile area");
        } else if (payload instanceof ModContentPayload.FactionPayload p) {
            if (p.resources().size() > 128) throw new IllegalArgumentException("faction payload has too many resource channels");
        } else if (payload instanceof ModContentPayload.ItemPayload p) {
            if (p.components().size() > 128) throw new IllegalArgumentException("item payload has too many components");
        } else if (payload instanceof ModContentPayload.KnowledgePayload p) {
            if (p.prerequisites().size() > 64 || p.unlockBlueprints().size() > 128) throw new IllegalArgumentException("knowledge payload exceeds dependency bounds");
        } else if (payload instanceof ModContentPayload.LorePayload p) {
            if (p.tags().size() > 64 || p.crossLinks().size() > 128) throw new IllegalArgumentException("lore payload exceeds taxonomy bounds");
        } else {
            throw new IllegalArgumentException("unsupported payload implementation: " + payload.getClass().getName());
        }
    }

    private static String string(Map<String, ?> raw, String key) { return string(raw, key, null); }
    private static String string(Map<String, ?> raw, String key, String fallback) {
        Object value = raw.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
    private static int integer(Map<String, ?> raw, String key, int fallback) {
        Object value = raw.get(key);
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s && !s.isBlank()) return Integer.parseInt(s.trim());
        return fallback;
    }
    private static double decimal(Map<String, ?> raw, String key, double fallback) {
        Object value = raw.get(key);
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s && !s.isBlank()) return Double.parseDouble(s.trim());
        return fallback;
    }
    private static boolean bool(Map<String, ?> raw, String key, boolean fallback) {
        Object value = raw.get(key);
        if (value instanceof Boolean b) return b;
        if (value instanceof String s && !s.isBlank()) return Boolean.parseBoolean(s.trim());
        return fallback;
    }
    private static List<String> stringList(Map<String, ?> raw, String key) {
        Object value = raw.get(key);
        if (value == null) return List.of();
        if (value instanceof Iterable<?> iterable) {
            List<String> out = new ArrayList<>();
            for (Object item : iterable) if (item != null) out.add(String.valueOf(item));
            return List.copyOf(out);
        }
        return List.of(String.valueOf(value));
    }
    private static Map<String, Integer> integerMap(Map<String, ?> raw, String key) {
        Object value = raw.get(key);
        if (!(value instanceof Map<?, ?> map)) return Map.of();
        java.util.LinkedHashMap<String, Integer> out = new java.util.LinkedHashMap<>();
        for (var e : map.entrySet()) {
            if (e.getKey() != null) out.put(String.valueOf(e.getKey()), e.getValue() instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(e.getValue())));
        }
        return Map.copyOf(out);
    }
}
