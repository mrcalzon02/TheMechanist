package mechanist;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Zero-dependency mod package pre-loader scanner.  This class deliberately avoids
 * Java SecurityManager because that facility is deprecated/removed from the
 * modern Java security model; the loader instead rejects dangerous bytecode
 * before it is ever given to a ClassLoader.
 */
final class ModPackageValidator {
    static final int MAX_ENTRY_BYTES = 8 * 1024 * 1024;
    static final int MAX_CLASS_BYTES = 2 * 1024 * 1024;
    static final int MAX_TOTAL_BYTES = 96 * 1024 * 1024;

    static final List<String> BANNED_DOTTED_REFERENCES = List.of(
            "java.lang.ProcessBuilder",
            "java.lang.Runtime",
            "java.lang.System.exit",
            "java.lang.reflect",
            "java.lang.invoke.MethodHandles",
            "java.io.File",
            "java.io.FileOutputStream",
            "java.io.FileWriter",
            "java.io.RandomAccessFile",
            "java.nio.file.Files",
            "java.nio.file.Path",
            "java.net.Socket",
            "java.net.ServerSocket",
            "java.net.DatagramSocket",
            "java.net.URLClassLoader",
            "sun.misc.Unsafe",
            "jdk.internal",
            "javax.crypto"
    );

    static final List<String> BANNED_TEXT_PATTERNS = List.of(
            "ProcessBuilder",
            "Runtime.getRuntime",
            "System.exit",
            "setAccessible",
            "defineClass",
            "Lookup.defineClass",
            "getDeclaredField",
            "getDeclaredMethod",
            "java/lang/reflect",
            "java/lang/ProcessBuilder",
            "java/lang/Runtime",
            "java/net/Socket",
            "java/net/ServerSocket",
            "java/io/FileOutputStream",
            "java/nio/file/Files",
            "sun/misc/Unsafe",
            "jdk/internal"
    );

    private final Executor backgroundExecutor;

    ModPackageValidator(Executor backgroundExecutor) {
        this.backgroundExecutor = Objects.requireNonNull(backgroundExecutor, "backgroundExecutor");
    }

    CompletableFuture<ValidationReport> validateAsync(Path jarFile) {
        Objects.requireNonNull(jarFile, "jarFile");
        return CompletableFuture.supplyAsync(() -> {
            try { return validate(jarFile); }
            catch (IOException ex) { return ValidationReport.rejected(jarFile, 0, 0, List.of("I/O failure while scanning mod package: " + ex.getMessage())); }
        }, backgroundExecutor);
    }

    static ValidationReport validate(Path jarFile) throws IOException {
        Objects.requireNonNull(jarFile, "jarFile");
        if (!Files.exists(jarFile)) return ValidationReport.rejected(jarFile, 0, 0, List.of("mod package does not exist"));
        if (!Files.isRegularFile(jarFile)) return ValidationReport.rejected(jarFile, 0, 0, List.of("mod package is not a regular file"));
        List<String> violations = new ArrayList<>();
        int classCount = 0;
        long totalBytes = 0;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(jarFile))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null || name.isBlank()) {
                    violations.add("blank zip entry name");
                    continue;
                }
                String normalized = name.replace('\\', '/');
                if (normalized.startsWith("/") || normalized.contains("../") || normalized.equals("..") || normalized.contains("\0")) {
                    violations.add("unsafe zip entry path: " + name);
                    continue;
                }
                if (entry.isDirectory()) continue;
                byte[] bytes = readEntryBounded(zip, MAX_ENTRY_BYTES, normalized);
                totalBytes += bytes.length;
                if (totalBytes > MAX_TOTAL_BYTES) {
                    violations.add("mod package exceeds maximum scan size of " + MAX_TOTAL_BYTES + " bytes");
                    break;
                }
                if (normalized.endsWith(".class")) {
                    classCount++;
                    if (bytes.length > MAX_CLASS_BYTES) violations.add(normalized + ": class exceeds maximum size of " + MAX_CLASS_BYTES + " bytes");
                    violations.addAll(scanClassFile(normalized, bytes));
                } else if (normalized.endsWith(".properties") || normalized.endsWith(".json") || normalized.endsWith(".txt") || normalized.endsWith(".cfg")) {
                    violations.addAll(scanText(normalized, bytes));
                }
            }
        }
        if (classCount == 0) violations.add("mod package contains no compiled class files");
        if (!violations.isEmpty()) return ValidationReport.rejected(jarFile, classCount, totalBytes, List.copyOf(violations));
        return new ValidationReport(jarFile, true, classCount, totalBytes, sha256Hex(Files.readAllBytes(jarFile)), List.of(), Instant.now());
    }

    static List<String> scanClassFile(String entryName, byte[] classBytes) {
        List<String> violations = new ArrayList<>();
        if (classBytes.length < 10) return List.of(entryName + ": invalid class file length");
        violations.addAll(scanRawBinary(entryName, classBytes));
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(classBytes))) {
            int magic = in.readInt();
            if (magic != 0xCAFEBABE) return List.of(entryName + ": invalid class magic");
            in.readUnsignedShort();
            in.readUnsignedShort();
            int cpCount = in.readUnsignedShort();
            String[] utf8 = new String[cpCount];
            int[] classNameIndexes = new int[cpCount];
            for (int i = 1; i < cpCount; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1 -> utf8[i] = in.readUTF();
                    case 3, 4 -> skipFully(in, 4, entryName);
                    case 5, 6 -> { skipFully(in, 8, entryName); i++; }
                    case 7 -> classNameIndexes[i] = in.readUnsignedShort();
                    case 8, 16, 19, 20 -> skipFully(in, 2, entryName);
                    case 9, 10, 11, 12, 18 -> skipFully(in, 4, entryName);
                    case 15 -> skipFully(in, 3, entryName);
                    case 17 -> skipFully(in, 4, entryName);
                    default -> throw new IOException(entryName + ": unsupported constant-pool tag " + tag);
                }
            }
            for (int i = 1; i < cpCount; i++) {
                String value = utf8[i];
                if (value != null) violations.addAll(validateConstant(entryName, value));
                int nameIndex = classNameIndexes[i];
                if (nameIndex > 0 && nameIndex < utf8.length && utf8[nameIndex] != null) {
                    violations.addAll(validateConstant(entryName, utf8[nameIndex]));
                }
            }
        } catch (IOException ex) {
            violations.add(entryName + ": class parse failure: " + ex.getMessage());
        }
        return violations;
    }

    private static List<String> scanText(String entryName, byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();
        for (String banned : BANNED_TEXT_PATTERNS) {
            if (text.contains(banned)) violations.add(entryName + ": banned text pattern " + banned);
        }
        return violations;
    }

    private static List<String> scanRawBinary(String entryName, byte[] bytes) {
        String latin = new String(bytes, StandardCharsets.ISO_8859_1);
        List<String> violations = new ArrayList<>();
        for (String banned : BANNED_TEXT_PATTERNS) {
            if (latin.contains(banned)) violations.add(entryName + ": banned binary signature " + banned);
        }
        return violations;
    }

    private static List<String> validateConstant(String entryName, String constant) {
        String dotted = constant.replace('/', '.');
        String normalized = dotted.replace('$', '.');
        List<String> violations = new ArrayList<>();
        for (String banned : BANNED_DOTTED_REFERENCES) {
            if (normalized.equals(banned) || normalized.startsWith(banned + ".") || normalized.contains(banned)) {
                violations.add(entryName + ": banned constant reference " + normalized);
            }
        }
        if (normalized.toLowerCase(Locale.ROOT).contains("setaccessible")) violations.add(entryName + ": reflective accessibility mutation constant " + normalized);
        return violations;
    }

    private static byte[] readEntryBounded(ZipInputStream zip, int limit, String entryName) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(8192, limit));
        byte[] buffer = new byte[8192];
        int read;
        int total = 0;
        while ((read = zip.read(buffer)) >= 0) {
            total += read;
            if (total > limit) throw new IOException(entryName + " exceeds maximum entry size of " + limit + " bytes");
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static void skipFully(DataInputStream in, int count, String entryName) throws IOException {
        int skipped = in.skipBytes(count);
        if (skipped != count) throw new IOException(entryName + ": truncated class constant pool");
    }

    static String sha256Hex(byte[] data) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("SHA-256 unavailable", ex);
        }
    }

    record ValidationReport(Path packagePath, boolean accepted, int classCount, long totalBytes, String sha256, List<String> violations, Instant scannedAt) {
        ValidationReport {
            Objects.requireNonNull(packagePath, "packagePath");
            sha256 = sha256 == null ? "" : sha256;
            violations = List.copyOf(Objects.requireNonNullElse(violations, List.of()));
            scannedAt = scannedAt == null ? Instant.now() : scannedAt;
        }
        static ValidationReport rejected(Path path, int classCount, long totalBytes, List<String> violations) {
            return new ValidationReport(path, false, classCount, totalBytes, "", violations, Instant.now());
        }
        String toAdminJson() {
            StringBuilder sb = new StringBuilder("{\n");
            sb.append("  \"packagePath\": ").append(AdminSecurityLogger.quote(packagePath.toString())).append(",\n");
            sb.append("  \"accepted\": ").append(accepted).append(",\n");
            sb.append("  \"classCount\": ").append(classCount).append(",\n");
            sb.append("  \"totalBytes\": ").append(totalBytes).append(",\n");
            sb.append("  \"sha256\": ").append(AdminSecurityLogger.quote(sha256)).append(",\n");
            sb.append("  \"scannedAt\": ").append(AdminSecurityLogger.quote(scannedAt.toString())).append(",\n");
            sb.append("  \"violations\": [");
            for (int i = 0; i < violations.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(AdminSecurityLogger.quote(violations.get(i)));
            }
            return sb.append("]\n}").toString();
        }
    }
}
