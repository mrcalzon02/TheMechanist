package mechanist;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/** Writes security events into a protected administrative log folder using simple JSON. */
final class AdminSecurityLogger {
    private final SecurityPathGuard guard;

    AdminSecurityLogger(Path adminLogRoot) throws IOException { this.guard = new SecurityPathGuard(adminLogRoot); }

    Path writeJsonEvent(String prefix, String sessionId, String json) throws IOException {
        String safePrefix = sanitize(prefix == null || prefix.isBlank() ? "event" : prefix);
        String safeSession = sanitize(sessionId == null || sessionId.isBlank() ? "unknown" : sessionId);
        Path file = guard.resolveInside(safePrefix + "-" + safeSession + "-" + System.currentTimeMillis() + ".json");
        Files.writeString(file, Objects.requireNonNullElse(json, "{}"), StandardCharsets.UTF_8);
        return file;
    }

    static String quote(String value) {
        if (value == null) return "null";
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 32) out.append(String.format("\\u%04x", (int)c)); else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    static String eventEnvelope(String type, String sessionId, String bodyJson) {
        return "{\n"
                + "  \"type\": " + quote(type) + ",\n"
                + "  \"sessionId\": " + quote(sessionId) + ",\n"
                + "  \"createdAt\": " + quote(Instant.now().toString()) + ",\n"
                + "  \"body\": " + (bodyJson == null || bodyJson.isBlank() ? "{}" : bodyJson) + "\n"
                + "}\n";
    }

    private static String sanitize(String input) { return input.replaceAll("[^A-Za-z0-9._-]", "_"); }
}
