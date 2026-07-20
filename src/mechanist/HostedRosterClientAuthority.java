package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Canonical client-side authority for assembling and validating hosted-roster
 * control frames.
 *
 * It accepts only complete, connected-only, monotonically versioned lobby
 * rosters. It never interprets relay payloads or grants world/gameplay
 * authority.
 */
final class HostedRosterClientAuthority {
    static final String VERSION = "hosted-roster-client-authority-1";
    static final int MAX_VISIBLE_PLAYERS = 64;

    private Snapshot latest;
    private Assembly assembly;
    private long acceptedSnapshots;
    private long idempotentSnapshots;
    private long rejectedFrames;
    private String lastEvent = "no hosted roster received";

    static boolean isRosterControlLine(String line) {
        return line != null && (line.startsWith("MECH|HOSTED_ROSTER_BEGIN|")
                || line.startsWith("MECH|HOSTED_ROSTER_ENTRY|")
                || line.startsWith("MECH|HOSTED_ROSTER_END|"));
    }

    synchronized Optional<Snapshot> accept(String line) {
        try {
            String[] fields = split(line);
            return switch (fields[1]) {
                case "HOSTED_ROSTER_BEGIN" -> acceptBegin(fields);
                case "HOSTED_ROSTER_ENTRY" -> acceptEntry(fields);
                case "HOSTED_ROSTER_END" -> acceptEnd(fields);
                default -> reject("line is not a hosted-roster control frame");
            };
        } catch (RuntimeException failure) {
            assembly = null;
            rejectedFrames++;
            lastEvent = "rejected: " + safeReason(failure.getMessage());
            throw failure;
        }
    }

    synchronized Snapshot latest() {
        return latest;
    }

    synchronized boolean assembling() {
        return assembly != null;
    }

    synchronized void resetForWorldChange() {
        latest = null;
        assembly = null;
        lastEvent = "reset for explicit world change";
    }

    synchronized String statusLine() {
        return "authority=" + VERSION
                + " latestVersion=" + (latest == null ? 0 : latest.version())
                + " world=" + (latest == null ? "none" : latest.worldId())
                + " visiblePlayers=" + (latest == null ? 0 : latest.entries().size())
                + " assembling=" + (assembly != null)
                + " accepted=" + acceptedSnapshots
                + " idempotent=" + idempotentSnapshots
                + " rejected=" + rejectedFrames
                + " lastEvent=" + lastEvent
                + " rosterVisibility=connected-only"
                + " worldAuthority=false";
    }

    private Optional<Snapshot> acceptBegin(String[] fields) {
        requireCount(fields, 7, "HOSTED_ROSTER_BEGIN");
        if (assembly != null) {
            throw new IllegalStateException(
                    "nested hosted-roster begin frame is not allowed");
        }
        long version = nonNegativeLong(fields[2], "roster version");
        String worldId = token(fields[3], "world id", 256);
        int total = boundedInt(fields[4], "visible roster count", 0, MAX_VISIBLE_PLAYERS);
        int active = boundedInt(fields[5], "active roster count", 0, MAX_VISIBLE_PLAYERS);
        boolean worldAuthority = strictBoolean(fields[6], "world-authority flag");
        if (worldAuthority) {
            throw new SecurityException(
                    "hosted roster attempted to claim remote world authority");
        }
        if (total != active) {
            throw new SecurityException(
                    "hosted roster exposed sessions outside the living lobby");
        }
        if (latest != null) {
            if (!latest.worldId().equals(worldId)) {
                throw new IllegalStateException(
                        "hosted roster world changed without an explicit client reset");
            }
            if (version < latest.version()) {
                throw new IllegalStateException(
                        "hosted roster version moved backward");
            }
        }
        assembly = new Assembly(version, worldId, total);
        lastEvent = "assembling roster version " + version;
        return Optional.empty();
    }

    private Optional<Snapshot> acceptEntry(String[] fields) {
        requireCount(fields, 10, "HOSTED_ROSTER_ENTRY");
        Assembly use = requireAssembly();
        if (use.entries.size() >= use.expectedEntries) {
            throw new IllegalStateException(
                    "hosted roster contains more entries than declared");
        }
        String playerId = token(fields[2], "player id", 64);
        if (!playerId.matches("remote-[a-f0-9]{20}")) {
            throw new IllegalArgumentException(
                    "hosted roster player id is invalid");
        }
        boolean connected = strictBoolean(fields[3], "connected flag");
        if (!connected) {
            throw new SecurityException(
                    "hosted roster serialized an offline persisted identity");
        }
        long generation = positiveLong(fields[4], "connection generation");
        boolean ready = strictBoolean(fields[5], "ready flag");
        String presence = enumToken(
                fields[6], "presence", List.of("available", "away", "busy"));
        String chatState = enumToken(
                fields[7], "chat state", List.of("idle", "typing"));
        long acceptedHostedCommands = nonNegativeLong(
                fields[8], "accepted hosted-command count");
        long lastSeenMillis = nonNegativeLong(fields[9], "last-seen time");
        if (!use.entries.isEmpty()) {
            String previous = use.entries.get(use.entries.size() - 1).playerId();
            if (previous.compareTo(playerId) >= 0) {
                throw new IllegalStateException(
                        "hosted roster player ids are duplicated or out of order");
            }
        }
        use.entries.add(new Entry(
                playerId,
                generation,
                ready,
                presence,
                chatState,
                acceptedHostedCommands,
                lastSeenMillis));
        return Optional.empty();
    }

    private Optional<Snapshot> acceptEnd(String[] fields) {
        requireCount(fields, 3, "HOSTED_ROSTER_END");
        Assembly use = requireAssembly();
        long endVersion = nonNegativeLong(fields[2], "roster end version");
        if (endVersion != use.version) {
            throw new IllegalStateException(
                    "hosted roster begin/end versions do not match");
        }
        if (use.entries.size() != use.expectedEntries) {
            throw new IllegalStateException(
                    "hosted roster ended before its declared entry count");
        }
        Snapshot completed = new Snapshot(
                use.version,
                use.worldId,
                List.copyOf(use.entries),
                false);
        assembly = null;
        if (latest != null && completed.version() == latest.version()) {
            if (!completed.equals(latest)) {
                throw new IllegalStateException(
                        "same-version hosted roster changed content");
            }
            idempotentSnapshots++;
            lastEvent = "accepted idempotent roster version " + completed.version();
            return Optional.of(latest);
        }
        latest = completed;
        acceptedSnapshots++;
        lastEvent = "accepted roster version " + completed.version();
        return Optional.of(completed);
    }

    private Assembly requireAssembly() {
        if (assembly == null) {
            throw new IllegalStateException(
                    "hosted roster entry/end arrived without a begin frame");
        }
        return assembly;
    }

    private Optional<Snapshot> reject(String reason) {
        throw new IllegalArgumentException(reason);
    }

    private static String[] split(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException(
                    "hosted roster control frame is blank");
        }
        if (line.length() > 4096) {
            throw new IllegalArgumentException(
                    "hosted roster control frame exceeds 4096 characters");
        }
        String[] fields = line.split("\\|", -1);
        if (fields.length < 2 || !"MECH".equals(fields[0])) {
            throw new IllegalArgumentException(
                    "hosted roster frame has an invalid protocol prefix");
        }
        return fields;
    }

    private static void requireCount(
            String[] fields,
            int expected,
            String command
    ) {
        if (fields.length != expected) {
            throw new IllegalArgumentException(
                    command + " has an invalid field count");
        }
    }

    private static String token(
            String value,
            String label,
            int maximumLength
    ) {
        String token = Objects.requireNonNullElse(value, "").trim();
        if (token.isBlank() || token.length() > maximumLength
                || token.indexOf('|') >= 0
                || token.indexOf('\n') >= 0
                || token.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(label + " is invalid");
        }
        return token;
    }

    private static String enumToken(
            String value,
            String label,
            List<String> allowed
    ) {
        String token = token(value, label, 32).toLowerCase(Locale.ROOT);
        if (!allowed.contains(token)) {
            throw new IllegalArgumentException(
                    label + " must be one of " + allowed);
        }
        return token;
    }

    private static boolean strictBoolean(String value, String label) {
        String token = token(value, label, 5).toLowerCase(Locale.ROOT);
        if (!"true".equals(token) && !"false".equals(token)) {
            throw new IllegalArgumentException(label + " must be true or false");
        }
        return Boolean.parseBoolean(token);
    }

    private static int boundedInt(
            String value,
            String label,
            int minimum,
            int maximum
    ) {
        long parsed = nonNegativeLong(value, label);
        if (parsed < minimum || parsed > maximum) {
            throw new IllegalArgumentException(
                    label + " must be between " + minimum + " and " + maximum);
        }
        return (int) parsed;
    }

    private static long positiveLong(String value, String label) {
        long parsed = nonNegativeLong(value, label);
        if (parsed < 1L) {
            throw new IllegalArgumentException(label + " must be positive");
        }
        return parsed;
    }

    private static long nonNegativeLong(String value, String label) {
        String token = token(value, label, 32);
        try {
            long parsed = Long.parseLong(token);
            if (parsed < 0L) throw new NumberFormatException("negative");
            return parsed;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    label + " must be a non-negative integer");
        }
    }

    private static String safeReason(String value) {
        String reason = Objects.requireNonNullElse(value, "invalid roster")
                .replace('|', '/')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
        return reason.isBlank()
                ? "invalid roster"
                : reason.substring(0, Math.min(160, reason.length()));
    }

    record Snapshot(
            long version,
            String worldId,
            List<Entry> entries,
            boolean worldAuthority
    ) {
        Snapshot {
            entries = List.copyOf(Objects.requireNonNullElse(entries, List.of()));
            if (worldAuthority) {
                throw new SecurityException(
                        "client roster snapshot cannot own world authority");
            }
        }

        int visiblePlayers() {
            return entries.size();
        }

        Entry entryFor(String playerId) {
            for (Entry entry : entries) {
                if (entry.playerId().equals(playerId)) return entry;
            }
            return null;
        }
    }

    record Entry(
            String playerId,
            long connectionGeneration,
            boolean ready,
            String presence,
            String chatState,
            long acceptedHostedCommands,
            long lastSeenMillis
    ) { }

    private static final class Assembly {
        final long version;
        final String worldId;
        final int expectedEntries;
        final ArrayList<Entry> entries = new ArrayList<>();

        Assembly(long version, String worldId, int expectedEntries) {
            this.version = version;
            this.worldId = worldId;
            this.expectedEntries = expectedEntries;
        }
    }
}
