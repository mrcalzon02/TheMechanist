package mechanist;

import mechanist.ProGuardMapParser.ProGuardMapping;
import mechanist.StackTraceDeobfuscator.DeobfuscationResult;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

/** Administrative engine for loading release mappings and de-obfuscating player crash logs. */
public final class CrashDeobfuscatorEngine implements AutoCloseable {
    private static final String ENCRYPTED_MAPPING_MAGIC = "MECHANIST-MAPPING-V1";
    private static final int GCM_TAG_BITS = 128;
    private final ExecutorService executor;
    private final ProGuardMapParser parser = new ProGuardMapParser();
    private final StackTraceDeobfuscator deobfuscator = new StackTraceDeobfuscator();
    private final Map<String, ProGuardMapping> mappingsByVersion = new ConcurrentHashMap<>();

    public CrashDeobfuscatorEngine() {
        this(Executors.newFixedThreadPool(Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors())), runnable -> {
            Thread t = new Thread(runnable, "mechanist-crash-deobfuscator");
            t.setDaemon(true);
            return t;
        }));
    }

    public CrashDeobfuscatorEngine(ExecutorService executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public CompletableFuture<ProGuardMapParser.MappingMetrics> loadMappingAsync(String buildVersion, Path mappingFile) {
        return loadMappingAsync(buildVersion, mappingFile, Optional.empty());
    }

    public CompletableFuture<ProGuardMapParser.MappingMetrics> loadMappingAsync(String buildVersion, Path mappingFile, Optional<Path> optionalKeyFile) {
        String version = sanitizeVersion(buildVersion);
        Objects.requireNonNull(mappingFile, "mappingFile");
        Optional<Path> keyFile = optionalKeyFile == null ? Optional.empty() : optionalKeyFile;
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path source = mappingFile.toAbsolutePath().normalize();
                ProGuardMapping mapping;
                if (isEncryptedMapping(source)) {
                    if (keyFile.isEmpty()) throw new IOException("Encrypted mapping selected without a key file: " + source);
                    String plain = decryptMappingPayload(source, keyFile.get().toAbsolutePath().normalize());
                    mapping = parser.parseText(plain, source.getFileName().toString());
                } else {
                    mapping = parser.parse(source);
                }
                mappingsByVersion.put(version, mapping);
                return mapping.metrics();
            } catch (IOException | GeneralSecurityException ex) {
                throw new CrashDeobfuscationException("Unable to load mapping for version " + version + " from " + mappingFile, ex);
            }
        }, executor);
    }

    public CompletableFuture<DeobfuscationResult> deobfuscateAsync(String buildVersion, String rawTrace) {
        String version = sanitizeVersion(buildVersion);
        return CompletableFuture.supplyAsync(() -> {
            ProGuardMapping mapping = mappingsByVersion.get(version);
            if (mapping == null) {
                String trace = rawTrace == null ? "" : rawTrace;
                String rebuilt = "[MappingMissingAnomalie build=" + version + " reason=no-mapping-loaded]" + System.lineSeparator() + trace;
                return new DeobfuscationResult(version, rebuilt, 0, 0, 1);
            }
            return deobfuscator.deobfuscate(rawTrace, version, mapping);
        }, executor);
    }

    public boolean hasMapping(String buildVersion) {
        return mappingsByVersion.containsKey(sanitizeVersion(buildVersion));
    }

    public void clearMapping(String buildVersion) {
        mappingsByVersion.remove(sanitizeVersion(buildVersion));
    }

    public void clearAllMappings() {
        mappingsByVersion.clear();
    }

    @Override public void close() {
        executor.shutdownNow();
    }

    private static String sanitizeVersion(String buildVersion) {
        if (buildVersion == null || buildVersion.isBlank()) return "manual-local";
        String cleaned = buildVersion.trim();
        if (cleaned.length() > 128) cleaned = cleaned.substring(0, 128);
        return cleaned;
    }

    private static boolean isEncryptedMapping(Path source) throws IOException {
        if (!Files.isRegularFile(source)) return false;
        try (var reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            String first = reader.readLine();
            return ENCRYPTED_MAPPING_MAGIC.equals(first);
        }
    }

    private static String decryptMappingPayload(Path source, Path keyFile) throws IOException, GeneralSecurityException {
        if (!Files.isRegularFile(keyFile)) throw new IOException("Mapping key file is missing: " + keyFile);
        String keyText = Files.readString(keyFile, StandardCharsets.US_ASCII).trim();
        byte[] keyBytes = Base64.getDecoder().decode(keyText);
        if (keyBytes.length != 32) throw new IOException("Mapping key must decode to exactly 32 bytes: " + keyFile);
        String payload = Files.readString(source, StandardCharsets.UTF_8);
        String iv64 = readField(payload, "iv");
        String ciphertext64 = readField(payload, "ciphertext");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, Base64.getDecoder().decode(iv64)));
        cipher.updateAAD(ENCRYPTED_MAPPING_MAGIC.getBytes(StandardCharsets.US_ASCII));
        byte[] compressed = cipher.doFinal(Base64.getDecoder().decode(ciphertext64));
        return new String(gunzip(compressed), StandardCharsets.UTF_8);
    }

    private static String readField(String payload, String key) throws IOException {
        String prefix = key + "=";
        for (String line : payload.split("\\R")) {
            if (line.startsWith(prefix)) return line.substring(prefix.length()).trim();
        }
        throw new IOException("Encrypted mapping payload is missing field: " + key);
    }

    private static byte[] gunzip(byte[] bytes) throws IOException {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(bytes)); ByteArrayOutputStream out = new ByteArrayOutputStream(bytes.length * 2)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read > 0) out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    public static final class CrashDeobfuscationException extends RuntimeException {
        public CrashDeobfuscationException(String message, Throwable cause) { super(message, cause); }
    }
}
