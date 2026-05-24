package mechanist;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Objects;

/** HMAC-SHA256 save/config signing for local tamper detection. */
final class SecureLocalSaveValidationManager {
    static final String SIGNATURE_PREFIX = "\n--MECHANIST-HMAC-SHA256:";
    static final String SIGNATURE_SUFFIX = "--\n";

    record ValidationResult(boolean valid, String payload, String reason) { }

    private final byte[] localSeed;

    SecureLocalSaveValidationManager() { this(localizedObscuredSeed()); }

    SecureLocalSaveValidationManager(byte[] localSeed) {
        this.localSeed = Objects.requireNonNull(localSeed, "localSeed").clone();
        if (this.localSeed.length < 16) throw new IllegalArgumentException("localSeed must be at least 16 bytes");
    }

    void writeSigned(Path file, String payload) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(payload, "payload");
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);
        String signature = sign(payload);
        Files.writeString(file, payload + SIGNATURE_PREFIX + signature + SIGNATURE_SUFFIX, StandardCharsets.UTF_8);
    }

    ValidationResult readAndValidate(Path file) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        int idx = content.lastIndexOf(SIGNATURE_PREFIX);
        if (idx < 0 || !content.endsWith(SIGNATURE_SUFFIX)) return new ValidationResult(false, "", "signature footer missing");
        String payload = content.substring(0, idx);
        String encodedSignature = content.substring(idx + SIGNATURE_PREFIX.length(), content.length() - SIGNATURE_SUFFIX.length());
        String expected = sign(payload);
        boolean ok = MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), encodedSignature.getBytes(StandardCharsets.UTF_8));
        return new ValidationResult(ok, ok ? payload : "", ok ? "valid" : "HMAC mismatch; file was modified or signed by another local seed");
    }

    String sign(String payload) throws IOException {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(localSeed, "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IOException("HMAC-SHA256 signing failed", ex);
        }
    }

    static byte[] localizedObscuredSeed() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String material = System.getProperty("user.name", "unknown") + "|"
                    + System.getProperty("user.home", "unknown") + "|"
                    + System.getProperty("os.name", "unknown") + "|"
                    + System.getProperty("java.vendor", "unknown") + "|MechanistLocalSaveSeed:v1";
            return digest.digest(material.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
