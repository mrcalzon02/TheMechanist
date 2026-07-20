package mechanist;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * Client-owned custody for independent-host resume tokens.
 *
 * Resume tokens must remain reusable on the client, so this store writes the
 * plaintext token only inside the user's mutable profile namespace. Files are
 * addressed by a SHA-256 key, written with required atomic replacement, and
 * restricted to the current OS user where supported. Tokens are never included
 * in diagnostics or status text.
 */
final class IndependentHostResumeTokenStore {
    static final String VERSION = "independent-host-resume-token-store-1";
    private static final String SCHEMA = "1";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Path root;

    IndependentHostResumeTokenStore(Path root) throws IOException {
        this.root = Objects.requireNonNull(root, "root")
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(this.root);
        setOwnerOnlyDirectoryPermissions(this.root);
    }

    synchronized Optional<Record> load(
            String serverKey,
            String profileIdentity
    ) throws IOException {
        String server = safeKey(serverKey, "server key");
        String profile = safeKey(profileIdentity, "profile identity");
        Path file = pathFor(server, profile);
        if (!Files.exists(file)) return Optional.empty();
        if (!Files.isRegularFile(file)) {
            throw new IOException(
                    "resume-token path is not a regular file: " + file);
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        if (!SCHEMA.equals(properties.getProperty("schema", "").trim())) {
            throw new IOException("unsupported resume-token store schema");
        }
        String storedServer = required(properties, "serverKey", 256);
        String storedProfile = required(properties, "profileIdentity", 256);
        if (!server.equals(storedServer) || !profile.equals(storedProfile)) {
            throw new IOException(
                    "resume-token record identity does not match its requested key");
        }
        String playerId = required(properties, "playerId", 64);
        if (!playerId.matches("remote-[a-f0-9]{20}")) {
            throw new IOException("resume-token record has an invalid player id");
        }
        String token = required(properties, "resumeToken", 64).toLowerCase();
        if (!token.matches("[a-f0-9]{64}")) {
            throw new IOException("resume-token record has an invalid token");
        }
        long generation = positiveLong(
                properties.getProperty("connectionGeneration"),
                "connection generation");
        long updated = nonNegativeLong(
                properties.getProperty("updatedAtMillis"),
                "updated time");
        setOwnerOnlyFilePermissions(file);
        return Optional.of(new Record(
                server,
                profile,
                playerId,
                token,
                generation,
                updated));
    }

    synchronized Record save(
            String serverKey,
            String profileIdentity,
            String playerId,
            String resumeToken,
            long connectionGeneration
    ) throws IOException {
        String server = safeKey(serverKey, "server key");
        String profile = safeKey(profileIdentity, "profile identity");
        String player = safePlayerId(playerId);
        String token = safeResumeToken(resumeToken);
        if (connectionGeneration < 1L) {
            throw new IllegalArgumentException(
                    "connection generation must be positive");
        }
        long now = System.currentTimeMillis();
        Properties properties = new Properties();
        properties.setProperty("schema", SCHEMA);
        properties.setProperty("serverKey", server);
        properties.setProperty("profileIdentity", profile);
        properties.setProperty("playerId", player);
        properties.setProperty("resumeToken", token);
        properties.setProperty(
                "connectionGeneration",
                Long.toString(connectionGeneration));
        properties.setProperty("updatedAtMillis", Long.toString(now));

        Files.createDirectories(root);
        setOwnerOnlyDirectoryPermissions(root);
        Path file = pathFor(server, profile);
        Path temporary = root.resolve(
                file.getFileName() + ".tmp-" + randomHex(8));
        try {
            try (FileChannel channel = FileChannel.open(
                         temporary,
                         StandardOpenOption.CREATE_NEW,
                         StandardOpenOption.WRITE);
                 OutputStream output = new BufferedOutputStream(
                         Channels.newOutputStream(channel))) {
                setOwnerOnlyFilePermissions(temporary);
                properties.store(
                        output,
                        "The Mechanist independent-host resume token; sensitive client credential");
                output.flush();
                channel.force(true);
            }
            Files.move(
                    temporary,
                    file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            setOwnerOnlyFilePermissions(file);
        } finally {
            Files.deleteIfExists(temporary);
        }
        return new Record(
                server,
                profile,
                player,
                token,
                connectionGeneration,
                now);
    }

    synchronized boolean delete(
            String serverKey,
            String profileIdentity
    ) throws IOException {
        return Files.deleteIfExists(pathFor(
                safeKey(serverKey, "server key"),
                safeKey(profileIdentity, "profile identity")));
    }

    Path root() {
        return root;
    }

    String statusLine() {
        return "authority=" + VERSION
                + " root=" + root
                + " storage=plaintext-token-owner-only"
                + " atomicMove=required"
                + " diagnosticsIncludeToken=false";
    }

    private Path pathFor(String serverKey, String profileIdentity) {
        String digest = sha256Hex(serverKey + "|" + profileIdentity);
        return root.resolve("host-" + digest + ".properties");
    }

    private static String required(
            Properties properties,
            String key,
            int maximumLength
    ) throws IOException {
        String value = Objects.requireNonNullElse(
                        properties.getProperty(key),
                        "")
                .trim();
        if (value.isBlank()
                || value.length() > maximumLength
                || value.indexOf('|') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0) {
            throw new IOException(
                    "resume-token record contains invalid " + key);
        }
        return value;
    }

    private static String safeKey(String value, String label) {
        String token = Objects.requireNonNullElse(value, "").trim();
        if (token.isBlank()
                || token.length() > 256
                || token.indexOf('|') >= 0
                || token.indexOf('\n') >= 0
                || token.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(label + " is invalid");
        }
        return token;
    }

    private static String safePlayerId(String value) {
        String token = Objects.requireNonNullElse(value, "").trim();
        if (!token.matches("remote-[a-f0-9]{20}")) {
            throw new IllegalArgumentException("player id is invalid");
        }
        return token;
    }

    private static String safeResumeToken(String value) {
        String token = Objects.requireNonNullElse(value, "")
                .trim()
                .toLowerCase();
        if (!token.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("resume token is invalid");
        }
        return token;
    }

    private static long positiveLong(String value, String label)
            throws IOException {
        long parsed = nonNegativeLong(value, label);
        if (parsed < 1L) {
            throw new IOException(label + " must be positive");
        }
        return parsed;
    }

    private static long nonNegativeLong(String value, String label)
            throws IOException {
        try {
            long parsed = Long.parseLong(
                    Objects.requireNonNullElse(value, "").trim());
            if (parsed < 0L) throw new NumberFormatException("negative");
            return parsed;
        } catch (NumberFormatException failure) {
            throw new IOException(
                    "resume-token record contains invalid " + label,
                    failure);
        }
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception failure) {
            throw new IllegalStateException("SHA-256 is unavailable", failure);
        }
    }

    private static String randomHex(int bytes) {
        byte[] data = new byte[Math.max(8, Math.min(32, bytes))];
        RANDOM.nextBytes(data);
        return HexFormat.of().formatHex(data);
    }

    private static void setOwnerOnlyDirectoryPermissions(Path path) {
        try {
            Files.setPosixFilePermissions(
                    path,
                    EnumSet.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE));
        } catch (IOException | UnsupportedOperationException ignored) {
            // Windows and some mounted filesystems do not expose POSIX permissions.
        }
    }

    private static void setOwnerOnlyFilePermissions(Path path) {
        try {
            Files.setPosixFilePermissions(
                    path,
                    EnumSet.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE));
        } catch (IOException | UnsupportedOperationException ignored) {
            // Windows and some mounted filesystems do not expose POSIX permissions.
        }
    }

    record Record(
            String serverKey,
            String profileIdentity,
            String playerId,
            String resumeToken,
            long connectionGeneration,
            long updatedAtMillis
    ) { }
}
