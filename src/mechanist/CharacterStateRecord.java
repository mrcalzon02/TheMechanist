package mechanist;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("character position must contain finite coordinates");
        }
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
        if (json == null || !json.strip().startsWith("{")
                || !json.strip().endsWith("}")) {
            throw new IllegalArgumentException("invalid canonical character JSON envelope");
        }
        Map<String, String> values = SimpleJson.object(json);
        // These fields have always been written by toJson. Missing or damaged
        // position/health/identity data must never silently reset on reconnect.
        String identity = requiredSavedText(values, "identityKey");
        String name = requiredSavedText(values, "characterName");
        double x = requiredSavedCoordinate(values, "x");
        double y = requiredSavedCoordinate(values, "y");
        double z = requiredSavedCoordinate(values, "z");
        String zone = requiredSavedText(values, "zoneId");
        int health = requiredSavedHealth(values);
        Instant updated = requiredSavedTimestamp(values);
        return new CharacterStateRecord(
                identity, name, x, y, z, zone, health,
                parseStringList(values.get("selectedSkills"), List.of()),
                parseStringList(values.get("startingItems"), List.of("ration-pack", "work-clothes")),
                parseIntegerMap(values.get("factionReputation"), Map.of("civic-authority", 0)),
                updated);
    }

    private static String requiredSavedText(Map<String, String> values, String field) {
        String value = values.get(field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing or blank saved character " + field);
        }
        return value;
    }

    private static double requiredSavedCoordinate(Map<String, String> values, String field) {
        String raw = requiredSavedText(values, field);
        try {
            double coordinate = Double.parseDouble(raw);
            if (Double.isFinite(coordinate)) return coordinate;
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("invalid saved character " + field, invalid);
        }
        throw new IllegalArgumentException("non-finite saved character " + field);
    }

    private static int requiredSavedHealth(Map<String, String> values) {
        String raw = requiredSavedText(values, "health");
        try {
            int health = Integer.parseInt(raw);
            if (health >= 0 && health <= 100) return health;
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("invalid saved character health", invalid);
        }
        throw new IllegalArgumentException("out-of-range saved character health");
    }

    private static Instant requiredSavedTimestamp(Map<String, String> values) {
        String raw = requiredSavedText(values, "updatedAt");
        try {
            return Instant.parse(raw);
        } catch (java.time.format.DateTimeParseException invalid) {
            throw new IllegalArgumentException("invalid saved character updatedAt", invalid);
        }
    }

    private static List<String> parseStringList(String raw, List<String> fallback) {
        if (raw == null) return fallback;
        NestedFieldReader reader = new NestedFieldReader(raw);
        List<String> values = new ArrayList<>();
        reader.expect('[');
        if (!reader.consume(']')) {
            do { values.add(reader.readString()); }
            while (reader.consume(','));
            reader.expect(']');
        }
        reader.expectEnd();
        return List.copyOf(values);
    }

    private static Map<String, Integer> parseIntegerMap(
            String raw, Map<String, Integer> fallback
    ) {
        if (raw == null) return fallback;
        NestedFieldReader reader = new NestedFieldReader(raw);
        Map<String, Integer> values = new LinkedHashMap<>();
        reader.expect('{');
        if (!reader.consume('}')) {
            do {
                String key = reader.readString();
                reader.expect(':');
                if (values.putIfAbsent(key, reader.readInteger()) != null) {
                    throw new IllegalArgumentException("duplicate faction reputation key");
                }
            } while (reader.consume(','));
            reader.expect('}');
        }
        reader.expectEnd();
        return Map.copyOf(values);
    }

    /** Strictly reads the nested fields owned by this record; malformed saved state must not reset progress. */
    private static final class NestedFieldReader {
        private final String input;
        private int index;

        NestedFieldReader(String input) { this.input = input; }

        private void skipWhitespace() {
            while (index < input.length()
                    && Character.isWhitespace(input.charAt(index))) index++;
        }

        boolean consume(char token) {
            skipWhitespace();
            if (index < input.length() && input.charAt(index) == token) {
                index++;
                return true;
            }
            return false;
        }

        void expect(char token) {
            if (!consume(token)) {
                throw new IllegalArgumentException(
                        "invalid character field JSON: expected " + token);
            }
        }

        void expectEnd() {
            skipWhitespace();
            if (index != input.length()) {
                throw new IllegalArgumentException("trailing character field JSON");
            }
        }

        String readString() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (index < input.length()) {
                char value = input.charAt(index++);
                if (value == '"') return out.toString();
                if (value == '\\') {
                    if (index >= input.length()) {
                        throw new IllegalArgumentException("truncated JSON string escape");
                    }
                    char escape = input.charAt(index++);
                    switch (escape) {
                        case '"', '\\', '/' -> out.append(escape);
                        case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case 'u' -> {
                            if (index + 4 > input.length()) {
                                throw new IllegalArgumentException("truncated JSON unicode escape");
                            }
                            try {
                                out.append((char) Integer.parseInt(
                                        input.substring(index, index + 4), 16));
                            } catch (NumberFormatException invalid) {
                                throw new IllegalArgumentException(
                                        "invalid JSON unicode escape", invalid);
                            }
                            index += 4;
                        }
                        default -> throw new IllegalArgumentException(
                                "invalid JSON string escape");
                    }
                } else {
                    if (value < 0x20) {
                        throw new IllegalArgumentException(
                                "unescaped JSON control character");
                    }
                    out.append(value);
                }
            }
            throw new IllegalArgumentException("unterminated JSON string");
        }

        int readInteger() {
            skipWhitespace();
            int start = index;
            if (index < input.length() && input.charAt(index) == '-') index++;
            if (index >= input.length()) {
                throw new IllegalArgumentException(
                        "missing faction reputation integer");
            }
            if (input.charAt(index) == '0') index++;
            else {
                if (input.charAt(index) < '1' || input.charAt(index) > '9') {
                    throw new IllegalArgumentException(
                            "invalid faction reputation integer");
                }
                while (index < input.length()
                        && input.charAt(index) >= '0'
                        && input.charAt(index) <= '9') index++;
            }
            try {
                return Integer.parseInt(input.substring(start, index));
            } catch (NumberFormatException invalid) {
                throw new IllegalArgumentException(
                        "out-of-range faction reputation integer", invalid);
            }
        }
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
