package mechanist;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Immutable world-location record captured when a player leaves active simulation. */
record PlayerWorldStateRecord(
        String identityKey,
        String worldId,
        double x,
        double y,
        double z,
        double orientationDegrees,
        List<String> inventory,
        int health,
        Instant capturedAt
) {
    PlayerWorldStateRecord {
        if (identityKey == null || identityKey.isBlank()) throw new IllegalArgumentException("identityKey is required");
        worldId = worldId == null || worldId.isBlank() ? "server-world" : worldId;
        orientationDegrees = ((orientationDegrees % 360.0) + 360.0) % 360.0;
        inventory = List.copyOf(Objects.requireNonNullElse(inventory, List.of()));
        health = Math.max(0, Math.min(100, health));
        capturedAt = capturedAt == null ? Instant.now() : capturedAt;
    }
}
