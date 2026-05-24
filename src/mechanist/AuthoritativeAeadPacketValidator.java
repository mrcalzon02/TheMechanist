package mechanist;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Server-authoritative AES-GCM packet validator with metadata-bound AAD and replay rejection. */
final class AuthoritativeAeadPacketValidator {
    static final int GCM_TAG_BITS = 128;
    static final int GCM_IV_BYTES = 12;
    static final long MAX_TIMESTAMP_SKEW_NANOS = 30_000_000_000L;

    record SecurePacketFrame(String sessionToken, long packetId, long timestampNanos, String ivBase64, String ciphertextBase64) {
        SecurePacketFrame {
            if (sessionToken == null || sessionToken.isBlank()) throw new IllegalArgumentException("sessionToken is required");
            if (packetId < 0) throw new IllegalArgumentException("packetId must be non-negative");
            if (timestampNanos <= 0) throw new IllegalArgumentException("timestampNanos must be positive");
            Objects.requireNonNull(ivBase64, "ivBase64");
            Objects.requireNonNull(ciphertextBase64, "ciphertextBase64");
        }

        byte[] aad() {
            ByteBuffer buffer = ByteBuffer.allocate(sessionToken.getBytes(StandardCharsets.UTF_8).length + Long.BYTES + Long.BYTES + 2);
            buffer.put(sessionToken.getBytes(StandardCharsets.UTF_8));
            buffer.put((byte)'|');
            buffer.putLong(packetId);
            buffer.put((byte)'|');
            buffer.putLong(timestampNanos);
            return buffer.array();
        }
    }

    static final class PacketSecurityException extends Exception {
        PacketSecurityException(String message) { super(message); }
        PacketSecurityException(String message, Throwable cause) { super(message, cause); }
    }

    private final Map<String, AtomicLong> highestAcceptedPacketId = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    SecurePacketFrame encryptForSession(String sessionToken, long packetId, SecretKey key, byte[] plaintext) throws PacketSecurityException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(plaintext, "plaintext");
        byte[] iv = new byte[GCM_IV_BYTES];
        secureRandom.nextBytes(iv);
        long timestamp = System.nanoTime();
        SecurePacketFrame aadFrame = new SecurePacketFrame(sessionToken, packetId, timestamp, Base64.getEncoder().encodeToString(iv), "");
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(aadFrame.aad());
            byte[] ciphertext = cipher.doFinal(plaintext);
            return new SecurePacketFrame(sessionToken, packetId, timestamp, aadFrame.ivBase64(), Base64.getEncoder().encodeToString(ciphertext));
        } catch (GeneralSecurityException ex) {
            throw new PacketSecurityException("AES-GCM packet encryption failed", ex);
        }
    }

    byte[] decryptAndValidate(SecurePacketFrame frame, SecretKey key) throws PacketSecurityException {
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(key, "key");
        rejectReplayBeforeDecrypt(frame);
        try {
            byte[] iv = Base64.getDecoder().decode(frame.ivBase64());
            if (iv.length != GCM_IV_BYTES) throw new PacketSecurityException("Invalid AES-GCM IV length: " + iv.length);
            byte[] ciphertext = Base64.getDecoder().decode(frame.ciphertextBase64());
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(frame.aad());
            byte[] plaintext = cipher.doFinal(ciphertext);
            highestAcceptedPacketId.computeIfAbsent(frame.sessionToken(), ignored -> new AtomicLong(-1L)).set(frame.packetId());
            return plaintext;
        } catch (IllegalArgumentException ex) {
            throw new PacketSecurityException("Malformed packet encoding", ex);
        } catch (AEADBadTagException ex) {
            throw new PacketSecurityException("AES-GCM authentication tag rejected packet metadata or ciphertext", ex);
        } catch (GeneralSecurityException ex) {
            throw new PacketSecurityException("AES-GCM packet validation failed", ex);
        }
    }

    SecretKey generateSessionKey() {
        byte[] key = new byte[32];
        secureRandom.nextBytes(key);
        return new SecretKeySpec(key, "AES");
    }

    private void rejectReplayBeforeDecrypt(SecurePacketFrame frame) throws PacketSecurityException {
        AtomicLong last = highestAcceptedPacketId.computeIfAbsent(frame.sessionToken(), ignored -> new AtomicLong(-1L));
        long lastValue = last.get();
        if (frame.packetId() <= lastValue) {
            throw new PacketSecurityException("Replay or out-of-order packet rejected for " + frame.sessionToken() + ": packetId=" + frame.packetId() + " highest=" + lastValue);
        }
        long now = System.nanoTime();
        if (Math.abs(now - frame.timestampNanos()) > MAX_TIMESTAMP_SKEW_NANOS) {
            throw new PacketSecurityException("Packet timestamp outside accepted skew window at " + Instant.now());
        }
    }
}
