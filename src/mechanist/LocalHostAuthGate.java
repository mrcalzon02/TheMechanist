package mechanist;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Direct-host identity verification gate used before any world or manifest data is disclosed. */
final class LocalHostAuthGate {
    enum AuthMode { PASSWORD, HOST_KEY }
    record AuthConfig(AuthMode mode, String secret, String createdAtIso) {
        AuthConfig { mode = mode == null ? AuthMode.HOST_KEY : mode; secret = cleanSecret(secret); createdAtIso = createdAtIso == null || createdAtIso.isBlank() ? Instant.now().toString() : createdAtIso; }
    }
    record AuthAttempt(String remoteSessionId, String suppliedSecret, String claimedPlayerName, String protocol, long receivedNanos) { }
    record AuthDecision(boolean accepted, String reason, String remoteSessionId, String mode) { }

    private static final char[] READABLE_KEY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final SecureRandom secureRandom = new SecureRandom();
    private final AtomicReference<AuthConfig> config = new AtomicReference<>(new AuthConfig(AuthMode.HOST_KEY, generateStaticKey(new SecureRandom()), Instant.now().toString()));

    AuthConfig currentConfig() { return config.get(); }

    AuthConfig usePassword(String password) {
        AuthConfig next = new AuthConfig(AuthMode.PASSWORD, password, Instant.now().toString());
        config.set(next);
        return next;
    }

    AuthConfig generateHostKey() {
        AuthConfig next = new AuthConfig(AuthMode.HOST_KEY, generateStaticKey(secureRandom), Instant.now().toString());
        config.set(next);
        return next;
    }

    AuthDecision validate(AuthAttempt attempt) {
        Objects.requireNonNull(attempt, "attempt");
        AuthConfig cfg = config.get();
        String supplied = cleanSecret(attempt.suppliedSecret());
        if (cfg.secret().isBlank()) return new AuthDecision(false, "host authentication secret is not configured", attempt.remoteSessionId(), cfg.mode().name());
        boolean accepted = constantTimeEquals(cfg.secret(), supplied);
        if (!accepted) return new AuthDecision(false, "identity verification failed before world data disclosure", attempt.remoteSessionId(), cfg.mode().name());
        return new AuthDecision(true, "identity verified", attempt.remoteSessionId(), cfg.mode().name());
    }

    AuthDecision validateIdentityVerificationFrame(String remoteSessionId, String frame) {
        if (frame == null || !frame.startsWith("AUTH|")) return new AuthDecision(false, "missing STATE 0 identity verification frame", remoteSessionId, config.get().mode().name());
        String[] parts = frame.split("\\|", 4);
        if (parts.length < 4) return new AuthDecision(false, "malformed auth frame", remoteSessionId, config.get().mode().name());
        return validate(new AuthAttempt(remoteSessionId, parts[2], parts[3], parts[1], System.nanoTime()));
    }

    String publicStatusLine() {
        AuthConfig cfg = config.get();
        return switch (cfg.mode()) {
            case PASSWORD -> "Password authentication enabled; secret is hidden.";
            case HOST_KEY -> "Host Key authentication enabled; key=" + cfg.secret();
        };
    }

    private static String generateStaticKey(SecureRandom random) {
        char[] key = new char[8];
        for (int i = 0; i < key.length; i++) key[i] = READABLE_KEY_ALPHABET[random.nextInt(READABLE_KEY_ALPHABET.length)];
        return new String(key);
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] aa = cleanSecret(a).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] bb = cleanSecret(b).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int diff = aa.length ^ bb.length;
        int max = Math.max(aa.length, bb.length);
        for (int i = 0; i < max; i++) {
            byte av = i < aa.length ? aa[i] : 0;
            byte bv = i < bb.length ? bb[i] : 0;
            diff |= av ^ bv;
        }
        return diff == 0;
    }

    private static String cleanSecret(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
}
