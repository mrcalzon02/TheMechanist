package mechanist;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Unified server-authoritative identity for Steam64 or fallback direct-connect credentials. */
record PlayerIdentity(PlayerIdentity.Kind kind, String value) {
    enum Kind { STEAM64, FALLBACK_UUID }
    private static final Pattern STEAM64 = Pattern.compile("^[0-9]{17}$");
    private static final Pattern UUID_TEXT = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    PlayerIdentity {
        Objects.requireNonNull(kind, "kind");
        if (value == null || value.isBlank()) throw new IllegalArgumentException("identity value is required");
        value = switch (kind) {
            case STEAM64 -> validateSteam64(value);
            case FALLBACK_UUID -> validateFallback(value);
        };
    }

    static PlayerIdentity steam64(String steamId) { return new PlayerIdentity(Kind.STEAM64, steamId); }

    static PlayerIdentity fallbackFromCredential(String accountCredential) {
        if (accountCredential == null || accountCredential.isBlank()) throw new IllegalArgumentException("fallback credential is required");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(("MechanistFallbackIdentity:v1|" + accountCredential).getBytes(StandardCharsets.UTF_8));
            UUID uuid = UUID.nameUUIDFromBytes(hash);
            return new PlayerIdentity(Kind.FALLBACK_UUID, uuid.toString());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    String storageKey() { return kind.name().toLowerCase(Locale.ROOT) + "-" + value; }

    private static String validateSteam64(String value) {
        String trimmed = value.trim();
        if (!STEAM64.matcher(trimmed).matches()) throw new IllegalArgumentException("Steam64 ID must be a 17-digit unsigned identity string");
        return trimmed;
    }

    private static String validateFallback(String value) {
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        if (!UUID_TEXT.matcher(trimmed).matches()) throw new IllegalArgumentException("Fallback identity must be a canonical UUID string");
        return trimmed;
    }
}
