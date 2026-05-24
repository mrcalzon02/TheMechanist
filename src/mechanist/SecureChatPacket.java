package mechanist;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * Network-safe encrypted chat packet for blind relay by a central server.
 *
 * The relay server needs only sender/recipient routing metadata and opaque Base64 fields.
 * It never receives the recipient private key and cannot decrypt ciphertextPayloadBase64.
 */
public record SecureChatPacket(
        String senderIdentifier,
        String recipientIdentifier,
        String keyId,
        String encryptedAesKeyBase64,
        String ivBase64,
        String ciphertextPayloadBase64,
        long createdAtEpochMillis
) {
    static final String AAD_VERSION = "MECHANIST_CHAT_E2EE_V1";
    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();
    private static final int AES_GCM_IV_BYTES = 12;

    public SecureChatPacket {
        senderIdentifier = sanitizeIdentifier(senderIdentifier);
        recipientIdentifier = sanitizeIdentifier(recipientIdentifier);
        keyId = sanitizeIdentifier(keyId);
        encryptedAesKeyBase64 = requireBase64("encryptedAesKeyBase64", encryptedAesKeyBase64, 1);
        ivBase64 = requireBase64("ivBase64", ivBase64, AES_GCM_IV_BYTES);
        ciphertextPayloadBase64 = requireBase64("ciphertextPayloadBase64", ciphertextPayloadBase64, 16);
        if (createdAtEpochMillis <= 0L) {
            throw new IllegalArgumentException("createdAtEpochMillis must be a positive epoch-millisecond value.");
        }
    }

    public static SecureChatPacket fromWireString(String wire) {
        Objects.requireNonNull(wire, "wire");
        String[] parts = wire.split("\\|", -1);
        if (parts.length != 7) {
            throw new IllegalArgumentException("Secure chat packet wire string must contain 7 pipe-delimited fields.");
        }
        long created;
        try {
            created = Long.parseLong(parts[6]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Secure chat packet timestamp is not a valid long.", e);
        }
        return new SecureChatPacket(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], created);
    }

    public static String encodeBase64(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        return B64_ENCODER.encodeToString(bytes);
    }

    public String toWireString() {
        return senderIdentifier + "|" + recipientIdentifier + "|" + keyId + "|" + encryptedAesKeyBase64
                + "|" + ivBase64 + "|" + ciphertextPayloadBase64 + "|" + createdAtEpochMillis;
    }

    public String safeRelayLogLine() {
        return "e2ee sender=" + senderIdentifier
                + " recipient=" + recipientIdentifier
                + " keyId=" + keyId
                + " encryptedKeyBytes=" + encryptedAesKeyBytes().length
                + " ivBytes=" + ivBytes().length
                + " ciphertextBytes=" + ciphertextPayloadBytes().length
                + " createdAt=" + createdAtEpochMillis;
    }

    public byte[] encryptedAesKeyBytes() {
        return decodeBase64("encryptedAesKeyBase64", encryptedAesKeyBase64);
    }

    public byte[] ivBytes() {
        return decodeBase64("ivBase64", ivBase64);
    }

    public byte[] ciphertextPayloadBytes() {
        return decodeBase64("ciphertextPayloadBase64", ciphertextPayloadBase64);
    }

    public byte[] additionalAuthenticatedData() {
        return authenticatedMetadataBytes(senderIdentifier, recipientIdentifier, keyId, encryptedAesKeyBase64, ivBase64, createdAtEpochMillis);
    }

    public static byte[] authenticatedMetadataBytes(String senderIdentifier,
                                                    String recipientIdentifier,
                                                    String keyId,
                                                    String encryptedAesKeyBase64,
                                                    String ivBase64,
                                                    long createdAtEpochMillis) {
        String metadata = AAD_VERSION + '\u001F'
                + sanitizeIdentifier(senderIdentifier) + '\u001F'
                + sanitizeIdentifier(recipientIdentifier) + '\u001F'
                + sanitizeIdentifier(keyId) + '\u001F'
                + Objects.requireNonNull(encryptedAesKeyBase64, "encryptedAesKeyBase64") + '\u001F'
                + Objects.requireNonNull(ivBase64, "ivBase64") + '\u001F'
                + createdAtEpochMillis;
        return metadata.getBytes(StandardCharsets.UTF_8);
    }

    static String sanitizeIdentifier(String input) {
        if (input == null) return "UNKNOWN";
        String cleaned = input.replaceAll("[^A-Za-z0-9_.:@/-]", "_");
        if (cleaned.isBlank()) return "UNKNOWN";
        return cleaned.length() > 96 ? cleaned.substring(0, 96) : cleaned;
    }

    private static String requireBase64(String field, String value, int minimumDecodedBytes) {
        Objects.requireNonNull(value, field);
        byte[] decoded = decodeBase64(field, value);
        if (decoded.length < minimumDecodedBytes) {
            throw new IllegalArgumentException(field + " decoded length is too short: " + decoded.length);
        }
        if ("ivBase64".equals(field) && decoded.length != AES_GCM_IV_BYTES) {
            throw new IllegalArgumentException("AES-GCM IV must be exactly 12 bytes for this chat protocol.");
        }
        return value;
    }

    private static byte[] decodeBase64(String field, String value) {
        try {
            return B64_DECODER.decode(Objects.requireNonNull(value, field));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(field + " is not valid Base64.", e);
        }
    }
}
