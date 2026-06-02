package mechanist;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable server-side character state; clients may request actions, never author this record. */
record CharacterStateRecord(
        String identityKey,
        String characterName,
        double x,
        double y,
        double z,
        String zoneId,
        int health,
        List<String> selectedSkills,
        List<String> startingItems,
        Map<String, Integer> factionReputation,
        Instant updatedAt
) {
    CharacterStateRecord {
        if (identityKey == null || identityKey.isBlank()) throw new IllegalArgumentException("identityKey is required");
        characterName = characterName == null || characterName.isBlank() ? "Unnamed Citizen" : characterName.trim();
        zoneId = zoneId == null || zoneId.isBlank() ? "origin-zone" : zoneId.trim();
        health = Math.max(0, Math.min(100, health));
        selectedSkills = List.copyOf(Objects.requireNonNullElse(selectedSkills, List.of()));
        startingItems = List.copyOf(Objects.requireNonNullElse(startingItems, List.of("ration-pack", "work-clothes")));
        factionReputation = Map.copyOf(Objects.requireNonNullElse(factionReputation, Map.of()));
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    static CharacterStateRecord fresh(PlayerIdentity identity, String name) {
        return new CharacterStateRecord(identity.storageKey(), name, 0, 0, 0, "origin-zone", 100, List.of(), List.of("ration-pack", "work-clothes"), Map.of("civic-authority", 0), Instant.now());
    }

    String toJson() {
        return "{\n"
                + "  \"identityKey\": " + AdminSecurityLogger.quote(identityKey) + ",\n"
                + "  \"characterName\": " + AdminSecurityLogger.quote(characterName) + ",\n"
                + "  \"x\": " + x + ",\n"
                + "  \"y\": " + y + ",\n"
                + "  \"z\": " + z + ",\n"
                + "  \"zoneId\": " + AdminSecurityLogger.quote(zoneId) + ",\n"
                + "  \"health\": " + health + ",\n"
                + "  \"selectedSkills\": " + stringArray(selectedSkills) + ",\n"
                + "  \"startingItems\": " + stringArray(startingItems) + ",\n"
                + "  \"factionReputation\": " + intMap(factionReputation) + ",\n"
                + "  \"updatedAt\": " + AdminSecurityLogger.quote(updatedAt.toString()) + "\n"
                + "}";
    }

    static CharacterStateRecord fromJson(String json) {
        Map<String, String> values = SimpleJson.object(json);
        String identity = values.getOrDefault("identityKey", "unknown");
        String name = values.getOrDefault("characterName", "Unnamed Citizen");
        double x = SimpleJson.doubleValue(values.get("x"), 0);
        double y = SimpleJson.doubleValue(values.get("y"), 0);
        double z = SimpleJson.doubleValue(values.get("z"), 0);
        String zone = values.getOrDefault("zoneId", "origin-zone");
        int health = SimpleJson.intValue(values.get("health"), 100);
        Instant updated = SimpleJson.instantValue(values.get("updatedAt"), Instant.now());
        return new CharacterStateRecord(identity, name, x, y, z, zone, health, List.of(), List.of("ration-pack", "work-clothes"), Map.of("civic-authority", 0), updated);
    }

    private static String stringArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(AdminSecurityLogger.quote(values.get(i)));
        }
        return sb.append(']').toString();
    }

    private static String intMap(Map<String, Integer> values) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (var e : values.entrySet()) {
            if (i++ > 0) sb.append(',');
            sb.append(AdminSecurityLogger.quote(e.getKey())).append(':').append(e.getValue() == null ? 0 : e.getValue());
        }
        return sb.append('}').toString();
    }
}
