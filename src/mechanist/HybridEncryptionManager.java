package mechanist;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * Native Java 17 hybrid E2EE manager for chat payloads.
 *
 * The central multiplayer server can relay SecureChatPacket instances without holding any
 * symmetric keys or private keys. Only the intended recipient can unwrap the temporary AES key.
 */
public final class HybridEncryptionManager {
    public static final String VERSION = "hybrid-encryption-manager-0.9.10hr";
    public static final int RSA_IDENTITY_BITS = 3072;
    public static final int AES_KEY_BITS = 256;
    public static final int AES_GCM_IV_BYTES = 12;
    public static final int AES_GCM_TAG_BITS = 128;
    public static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    public static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";

    private static final OAEPParameterSpec OAEP_SHA256 = new OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
    );
    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();

    private final SecureRandom secureRandom;

    public HybridEncryptionManager() {
        this(new SecureRandom());
    }

    public HybridEncryptionManager(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
    }

    public KeyPair generateIdentityKeyPair() throws SecureChatCryptoException {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(RSA_IDENTITY_BITS, secureRandom);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new SecureChatCryptoException("RSA key-pair generation is not available in this Java runtime.", e);
        }
    }

    public SecretKey generateTemporaryAesKey() throws SecureChatCryptoException {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(AES_KEY_BITS, secureRandom);
            return generator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new SecureChatCryptoException("AES key generation is not available in this Java runtime.", e);
        }
    }

    public SecureChatPacket encryptForRecipient(String senderIdentifier,
                                                String recipientIdentifier,
                                                PublicKey recipientPublicKey,
                                                String plaintext) throws SecureChatCryptoException {
        Objects.requireNonNull(plaintext, "plaintext");
        return encryptForRecipient(senderIdentifier, recipientIdentifier, recipientPublicKey, plaintext.getBytes(StandardCharsets.UTF_8));
    }

    public SecureChatPacket encryptForRecipient(String senderIdentifier,
                                                String recipientIdentifier,
                                                PublicKey recipientPublicKey,
                                                byte[] plaintext) throws SecureChatCryptoException {
        Objects.requireNonNull(recipientPublicKey, "recipientPublicKey");
        Objects.requireNonNull(plaintext, "plaintext");
        SecretKey aesKey = generateTemporaryAesKey();
        byte[] encryptedAesKey = encryptAesKeyForRecipient(aesKey, recipientPublicKey);
        byte[] iv = randomBytes(AES_GCM_IV_BYTES);
        String safeSender = SecureChatPacket.sanitizeIdentifier(senderIdentifier);
        String safeRecipient = SecureChatPacket.sanitizeIdentifier(recipientIdentifier);
        String keyId = publicKeyFingerprint(recipientPublicKey);
        String encryptedKeyB64 = SecureChatPacket.encodeBase64(encryptedAesKey);
        String ivB64 = SecureChatPacket.encodeBase64(iv);
        long createdAt = Instant.now().toEpochMilli();
        byte[] aad = SecureChatPacket.authenticatedMetadataBytes(safeSender, safeRecipient, keyId, encryptedKeyB64, ivB64, createdAt);
        byte[] ciphertext = encryptPayload(aesKey, iv, aad, plaintext).ciphertextWithTag();
        return new SecureChatPacket(safeSender, safeRecipient, keyId, encryptedKeyB64, ivB64,
                SecureChatPacket.encodeBase64(ciphertext), createdAt);
    }

    public String decryptTextFromRecipientPacket(SecureChatPacket packet,
                                                 PrivateKey recipientPrivateKey) throws SecureChatCryptoException {
        return new String(decryptFromRecipientPacket(packet, recipientPrivateKey), StandardCharsets.UTF_8);
    }

    public byte[] decryptFromRecipientPacket(SecureChatPacket packet,
                                             PrivateKey recipientPrivateKey) throws SecureChatCryptoException {
        Objects.requireNonNull(packet, "packet");
        SecretKey aesKey = decryptAesKeyFromRecipientEnvelope(packet.encryptedAesKeyBytes(), recipientPrivateKey);
        return decryptPayload(aesKey, packet.ivBytes(), packet.additionalAuthenticatedData(), packet.ciphertextPayloadBytes());
    }

    public byte[] encryptAesKeyForRecipient(SecretKey aesKey, PublicKey recipientPublicKey) throws SecureChatCryptoException {
        Objects.requireNonNull(aesKey, "aesKey");
        Objects.requireNonNull(recipientPublicKey, "recipientPublicKey");
        try {
            Cipher rsa = Cipher.getInstance(RSA_TRANSFORMATION);
            rsa.init(Cipher.ENCRYPT_MODE, recipientPublicKey, OAEP_SHA256, secureRandom);
            return rsa.doFinal(aesKey.getEncoded());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new SecureChatCryptoException("RSA-OAEP-SHA256 is not available in this Java runtime.", e);
        } catch (InvalidKeyException e) {
            throw new SecureChatCryptoException("Recipient public key cannot be used for RSA-OAEP encryption.", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new SecureChatCryptoException("RSA-OAEP-SHA256 parameters were rejected by the crypto provider.", e);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new SecureChatCryptoException("Temporary AES key encapsulation failed.", e);
        }
    }

    public SecretKey decryptAesKeyFromRecipientEnvelope(byte[] encryptedAesKey,
                                                        PrivateKey recipientPrivateKey) throws SecureChatCryptoException {
        Objects.requireNonNull(encryptedAesKey, "encryptedAesKey");
        Objects.requireNonNull(recipientPrivateKey, "recipientPrivateKey");
        try {
            Cipher rsa = Cipher.getInstance(RSA_TRANSFORMATION);
            rsa.init(Cipher.DECRYPT_MODE, recipientPrivateKey, OAEP_SHA256);
            byte[] rawKey = rsa.doFinal(encryptedAesKey);
            if (rawKey.length != AES_KEY_BITS / 8) {
                throw new SecureChatCryptoException("Unwrapped AES key had unexpected byte length: " + rawKey.length);
            }
            return new SecretKeySpec(rawKey, "AES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new SecureChatCryptoException("RSA-OAEP-SHA256 is not available in this Java runtime.", e);
        } catch (InvalidKeyException e) {
            throw new SecureChatCryptoException("Recipient private key cannot be used for RSA-OAEP decryption.", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new SecureChatCryptoException("RSA-OAEP-SHA256 parameters were rejected by the crypto provider.", e);
        } catch (IllegalBlockSizeException e) {
            throw new SecureChatCryptoException("Encrypted AES key envelope has an invalid RSA block size.", e);
        } catch (BadPaddingException e) {
            throw new SecureChatCryptoException("Encrypted AES key envelope could not be decrypted by this recipient.", e);
        }
    }

    public AesGcmSealedPayload encryptPayload(SecretKey aesKey,
                                              byte[] iv,
                                              byte[] additionalAuthenticatedData,
                                              byte[] plaintext) throws SecureChatCryptoException {
        Objects.requireNonNull(aesKey, "aesKey");
        Objects.requireNonNull(iv, "iv");
        Objects.requireNonNull(additionalAuthenticatedData, "additionalAuthenticatedData");
        Objects.requireNonNull(plaintext, "plaintext");
        validateIv(iv);
        try {
            Cipher aes = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(AES_GCM_TAG_BITS, iv);
            aes.init(Cipher.ENCRYPT_MODE, aesKey, spec, secureRandom);

            /*
             * AES-GCM is used instead of AES-CBC because GCM is an AEAD mode:
             * it encrypts the message and authenticates both ciphertext and metadata.
             * CBC by itself provides only confidentiality; without a separate MAC it is
             * malleable and can permit tampering/padding-oracle classes of failure.
             */
            aes.updateAAD(additionalAuthenticatedData);
            return new AesGcmSealedPayload(Arrays.copyOf(iv, iv.length), aes.doFinal(plaintext));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new SecureChatCryptoException("AES-GCM is not available in this Java runtime.", e);
        } catch (InvalidKeyException e) {
            throw new SecureChatCryptoException("AES key cannot be used for AES-GCM encryption.", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new SecureChatCryptoException("AES-GCM parameters were rejected by the crypto provider.", e);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new SecureChatCryptoException("AES-GCM encryption failed.", e);
        }
    }

    public byte[] decryptPayload(SecretKey aesKey,
                                 byte[] iv,
                                 byte[] additionalAuthenticatedData,
                                 byte[] ciphertextWithTag) throws SecureChatCryptoException {
        Objects.requireNonNull(aesKey, "aesKey");
        Objects.requireNonNull(iv, "iv");
        Objects.requireNonNull(additionalAuthenticatedData, "additionalAuthenticatedData");
        Objects.requireNonNull(ciphertextWithTag, "ciphertextWithTag");
        validateIv(iv);
        try {
            Cipher aes = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(AES_GCM_TAG_BITS, iv);
            aes.init(Cipher.DECRYPT_MODE, aesKey, spec);

            /*
             * GCM tag verification happens inside doFinal. If any relay-visible metadata,
             * IV, ciphertext byte, or authentication tag is modified, decryption throws
             * AEADBadTagException and no plaintext is accepted by the caller.
             */
            aes.updateAAD(additionalAuthenticatedData);
            return aes.doFinal(ciphertextWithTag);
        } catch (AEADBadTagException e) {
            throw new SecureChatCryptoException("AES-GCM authentication failed; packet was tampered with or keys do not match.", e);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new SecureChatCryptoException("AES-GCM is not available in this Java runtime.", e);
        } catch (InvalidKeyException e) {
            throw new SecureChatCryptoException("AES key cannot be used for AES-GCM decryption.", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new SecureChatCryptoException("AES-GCM parameters were rejected by the crypto provider.", e);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new SecureChatCryptoException("AES-GCM decryption failed.", e);
        }
    }

    public String publicKeyFingerprint(PublicKey publicKey) throws SecureChatCryptoException {
        Objects.requireNonNull(publicKey, "publicKey");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(digest, 18));
        } catch (NoSuchAlgorithmException e) {
            throw new SecureChatCryptoException("SHA-256 digest is not available in this Java runtime.", e);
        }
    }

    public String encodePublicKey(PublicKey publicKey) {
        Objects.requireNonNull(publicKey, "publicKey");
        return B64_ENCODER.encodeToString(publicKey.getEncoded());
    }

    public String encodePrivateKey(PrivateKey privateKey) {
        Objects.requireNonNull(privateKey, "privateKey");
        return B64_ENCODER.encodeToString(privateKey.getEncoded());
    }

    public PublicKey decodePublicKey(String x509Base64) throws SecureChatCryptoException {
        Objects.requireNonNull(x509Base64, "x509Base64");
        try {
            byte[] encoded = B64_DECODER.decode(x509Base64);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
        } catch (IllegalArgumentException e) {
            throw new SecureChatCryptoException("Public key text is not valid Base64.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new SecureChatCryptoException("RSA key factory is not available in this Java runtime.", e);
        } catch (InvalidKeySpecException e) {
            throw new SecureChatCryptoException("Public key text is not a valid X.509 RSA public key.", e);
        }
    }

    public PrivateKey decodePrivateKey(String pkcs8Base64) throws SecureChatCryptoException {
        Objects.requireNonNull(pkcs8Base64, "pkcs8Base64");
        try {
            byte[] encoded = B64_DECODER.decode(pkcs8Base64);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));
        } catch (IllegalArgumentException e) {
            throw new SecureChatCryptoException("Private key text is not valid Base64.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new SecureChatCryptoException("RSA key factory is not available in this Java runtime.", e);
        } catch (InvalidKeySpecException e) {
            throw new SecureChatCryptoException("Private key text is not a valid PKCS#8 RSA private key.", e);
        }
    }

    public byte[] randomBytes(int length) {
        if (length <= 0) throw new IllegalArgumentException("length must be positive");
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    private static void validateIv(byte[] iv) throws SecureChatCryptoException {
        if (iv.length != AES_GCM_IV_BYTES) {
            throw new SecureChatCryptoException("AES-GCM chat IV must be exactly " + AES_GCM_IV_BYTES + " bytes.");
        }
    }

    public static String auditSummary() {
        return VERSION + " rsaBits=" + RSA_IDENTITY_BITS + " aesBits=" + AES_KEY_BITS
                + " ivBytes=" + AES_GCM_IV_BYTES + " tagBits=" + AES_GCM_TAG_BITS
                + " rsa=" + RSA_TRANSFORMATION + " aes=" + AES_TRANSFORMATION;
    }

    public record AesGcmSealedPayload(byte[] iv, byte[] ciphertextWithTag) {
        public AesGcmSealedPayload {
            Objects.requireNonNull(iv, "iv");
            Objects.requireNonNull(ciphertextWithTag, "ciphertextWithTag");
            iv = Arrays.copyOf(iv, iv.length);
            ciphertextWithTag = Arrays.copyOf(ciphertextWithTag, ciphertextWithTag.length);
        }
        @Override public byte[] iv() { return Arrays.copyOf(iv, iv.length); }
        @Override public byte[] ciphertextWithTag() { return Arrays.copyOf(ciphertextWithTag, ciphertextWithTag.length); }
    }

    public static final class SecureChatCryptoException extends GeneralSecurityException {
        public SecureChatCryptoException(String message) { super(message); }
        public SecureChatCryptoException(String message, Throwable cause) { super(message, cause); }
    }
}
