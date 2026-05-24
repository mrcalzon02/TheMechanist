package mechanist;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves user-owned storage outside the application install directory.
 * Installers may place binaries under Program Files or /opt; mutable worlds,
 * profiles, live mods, exports, and archived mods belong in the user's data root.
 */
final class GameStorageManager {
    static final String VERSION = "game-storage-manager-0.9.10im";
    static final String APPLICATION_DIRECTORY_NAME = "TheMechanist";
    static final String OVERRIDE_ENV = "THE_MECHANIST_HOME";
    static final String OVERRIDE_PROPERTY = "mechanist.storage.root";

    enum HostOperatingSystem { WINDOWS, LINUX, MACOS, OTHER }

    record DirectorySnapshot(
            HostOperatingSystem operatingSystem,
            Path root,
            Path saves,
            Path profiles,
            Path export,
            Path mods,
            Path modsArchived,
            Instant capturedAt
    ) {
        DirectorySnapshot {
            Objects.requireNonNull(operatingSystem, "operatingSystem");
            root = requireAbsolute(root, "root");
            saves = requireAbsolute(saves, "saves");
            profiles = requireAbsolute(profiles, "profiles");
            export = requireAbsolute(export, "export");
            mods = requireAbsolute(mods, "mods");
            modsArchived = requireAbsolute(modsArchived, "modsArchived");
            capturedAt = capturedAt == null ? Instant.now() : capturedAt;
        }

        String auditLine() {
            return "storage=" + VERSION + " os=" + operatingSystem
                    + " root=" + root
                    + " saves=" + saves
                    + " profiles=" + profiles
                    + " export=" + export
                    + " mods=" + mods
                    + " modsArchived=" + modsArchived;
        }
    }

    private static final class Holder {
        private static final GameStorageManager INSTANCE = create();

        private static GameStorageManager create() {
            try {
                return new GameStorageManager(resolveUserRoot());
            } catch (IOException ex) {
                throw new IllegalStateException("Unable to initialize The Mechanist user storage directory", ex);
            }
        }
    }

    private final HostOperatingSystem operatingSystem;
    private final Path rootDir;
    private final Path savesDir;
    private final Path profilesDir;
    private final Path exportDir;
    private final Path modsDir;
    private final Path modsArchivedDir;

    static GameStorageManager get() {
        return Holder.INSTANCE;
    }

    static HostOperatingSystem detectOperatingSystem() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return HostOperatingSystem.WINDOWS;
        if (os.contains("linux")) return HostOperatingSystem.LINUX;
        if (os.contains("mac") || os.contains("darwin")) return HostOperatingSystem.MACOS;
        return HostOperatingSystem.OTHER;
    }

    private GameStorageManager(Path rootDir) throws IOException {
        this.operatingSystem = detectOperatingSystem();
        this.rootDir = normalizeAbsolute(rootDir);
        this.savesDir = this.rootDir.resolve("saves").normalize().toAbsolutePath();
        this.profilesDir = this.savesDir.resolve("data").resolve("profiles").normalize().toAbsolutePath();
        this.exportDir = this.rootDir.resolve("export").normalize().toAbsolutePath();
        this.modsDir = this.rootDir.resolve("mods").normalize().toAbsolutePath();
        this.modsArchivedDir = this.rootDir.resolve("modsarchived").normalize().toAbsolutePath();
        ensureLayout();
    }

    Path rootDir() { return rootDir; }
    Path savesDir() { return savesDir; }
    Path profilesDir() { return profilesDir; }
    Path exportDir() { return exportDir; }
    Path modsDir() { return modsDir; }
    Path modsArchivedDir() { return modsArchivedDir; }

    DirectorySnapshot snapshot() {
        return new DirectorySnapshot(operatingSystem, rootDir, savesDir, profilesDir, exportDir, modsDir, modsArchivedDir, Instant.now());
    }

    String auditSummary() { return snapshot().auditLine(); }

    /** Move a live mod into the archived-mod directory without permitting path traversal. */
    Path archiveMod(String modFileName) throws IOException {
        String clean = safeFileName(modFileName, "modFileName");
        Path source = resolveInside(modsDir, clean);
        Path target = resolveInside(modsArchivedDir, clean);
        if (!Files.exists(source)) throw new IOException("Live mod file does not exist: " + source);
        Files.createDirectories(target.getParent());
        return atomicMove(source, target);
    }

    /** Write an exported mod ZIP atomically into the export directory. */
    Path exportModArchive(String zipFileName, byte[] zipData) throws IOException {
        Objects.requireNonNull(zipData, "zipData");
        String clean = safeFileName(zipFileName, "zipFileName");
        if (!clean.toLowerCase(Locale.ROOT).endsWith(".zip")) clean = clean + ".zip";
        Path target = resolveInside(exportDir, clean);
        Path tmp = resolveInside(exportDir, clean + ".tmp");
        writeAtomic(tmp, target, zipData);
        return target;
    }

    Path resolveSavePath(String first, String... more) throws IOException {
        return resolveInside(savesDir, combine(first, more));
    }

    Path resolveProfilePath(String fileName) throws IOException {
        return resolveInside(profilesDir, safeFileName(fileName, "fileName"));
    }

    Path resolveModPath(String fileName) throws IOException {
        return resolveInside(modsDir, safeFileName(fileName, "fileName"));
    }

    private void ensureLayout() throws IOException {
        createReadableUserDirectory(rootDir);
        createReadableUserDirectory(savesDir);
        createReadableUserDirectory(profilesDir);
        createReadableUserDirectory(exportDir);
        createReadableUserDirectory(modsDir);
        createReadableUserDirectory(modsArchivedDir);
        verifyInside(rootDir, savesDir, "saves");
        verifyInside(rootDir, profilesDir, "profiles");
        verifyInside(rootDir, exportDir, "export");
        verifyInside(rootDir, modsDir, "mods");
        verifyInside(rootDir, modsArchivedDir, "modsarchived");
    }

    private static Path resolveUserRoot() {
        String propertyOverride = System.getProperty(OVERRIDE_PROPERTY);
        if (propertyOverride != null && !propertyOverride.isBlank()) return Paths.get(propertyOverride.trim());
        String envOverride = System.getenv(OVERRIDE_ENV);
        if (envOverride != null && !envOverride.isBlank()) return Paths.get(envOverride.trim());
        HostOperatingSystem os = detectOperatingSystem();
        return switch (os) {
            case WINDOWS -> windowsDocumentsRoot().resolve(APPLICATION_DIRECTORY_NAME);
            case LINUX -> linuxDataRoot().resolve(APPLICATION_DIRECTORY_NAME);
            case MACOS -> userHome().resolve("Library").resolve("Application Support").resolve(APPLICATION_DIRECTORY_NAME);
            case OTHER -> userHome().resolve(APPLICATION_DIRECTORY_NAME);
        };
    }

    private static Path windowsDocumentsRoot() {
        String userProfile = System.getenv("USERPROFILE");
        Path home = (userProfile == null || userProfile.isBlank()) ? userHome() : Paths.get(userProfile);
        Path documents = home.resolve("Documents");
        if (Files.isDirectory(documents)) return documents;
        Path localized = userHome().resolve("Documents");
        if (Files.isDirectory(localized)) return localized;
        return home.resolve("Documents");
    }

    private static Path linuxDataRoot() {
        String xdg = System.getenv("XDG_DATA_HOME");
        if (xdg != null && !xdg.isBlank()) return Paths.get(xdg.trim());
        return userHome().resolve(".local").resolve("share");
    }

    private static Path userHome() {
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) throw new IllegalStateException("user.home is not available");
        return Paths.get(home);
    }

    private static void createReadableUserDirectory(Path dir) throws IOException {
        Path parent = dir.getParent();
        FileAttribute<?>[] attributes = supportsPosix(parent == null ? dir : parent)
                ? new FileAttribute<?>[] { PosixFilePermissions.asFileAttribute(Set.of(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE)) }
                : new FileAttribute<?>[0];
        Files.createDirectories(dir, attributes);
        if (supportsPosix(dir)) {
            Files.setPosixFilePermissions(dir, Set.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE));
        }
    }

    private static boolean supportsPosix(Path path) {
        try {
            Path target = path == null ? Paths.get(".") : path;
            FileStore store = Files.exists(target) ? Files.getFileStore(target) : Files.getFileStore(target.toAbsolutePath().getParent() == null ? Paths.get(".") : target.toAbsolutePath().getParent());
            return store.supportsFileAttributeView("posix");
        } catch (IOException | SecurityException ex) {
            return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
        }
    }

    private static Path resolveInside(Path root, String relativeFileNameOrPath) throws IOException {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(relativeFileNameOrPath, "relativeFileNameOrPath");
        Path candidate;
        try {
            candidate = root.resolve(relativeFileNameOrPath).normalize().toAbsolutePath();
        } catch (InvalidPathException ex) {
            throw new IOException("Invalid path segment: " + relativeFileNameOrPath, ex);
        }
        verifyInside(root, candidate, relativeFileNameOrPath);
        return candidate;
    }

    private static void verifyInside(Path root, Path candidate, String label) throws IOException {
        Path cleanRoot = normalizeAbsolute(root);
        Path cleanCandidate = normalizeAbsolute(candidate);
        if (!cleanCandidate.startsWith(cleanRoot)) {
            throw new IOException("Refusing path traversal outside " + cleanRoot + " for " + label + ": " + cleanCandidate);
        }
    }

    private static Path normalizeAbsolute(Path path) {
        Objects.requireNonNull(path, "path");
        return path.normalize().toAbsolutePath();
    }

    private static Path requireAbsolute(Path path, String name) {
        Objects.requireNonNull(path, name);
        Path clean = normalizeAbsolute(path);
        if (!clean.isAbsolute()) throw new IllegalArgumentException(name + " must be absolute");
        return clean;
    }

    private static String safeFileName(String value, String field) throws IOException {
        if (value == null || value.isBlank()) throw new IOException(field + " is required");
        String trimmed = value.trim();
        if (trimmed.contains("/") || trimmed.contains("\\")) throw new IOException(field + " must be a plain file name, not a path: " + value);
        Path parsed;
        try { parsed = Paths.get(trimmed); }
        catch (InvalidPathException ex) { throw new IOException("Invalid " + field + ": " + value, ex); }
        if (!parsed.getFileName().toString().equals(trimmed)) throw new IOException(field + " must not contain path traversal: " + value);
        if (trimmed.equals(".") || trimmed.equals("..") || trimmed.contains("..")) throw new IOException(field + " must not contain traversal segments: " + value);
        return trimmed.replaceAll("[^A-Za-z0-9._+() -]", "_");
    }

    private static String combine(String first, String... more) throws IOException {
        if (first == null || first.isBlank()) throw new IOException("path segment is required");
        Path p = Paths.get(first);
        if (p.isAbsolute()) throw new IOException("absolute child paths are forbidden: " + first);
        if (more != null) {
            for (String segment : more) {
                if (segment == null || segment.isBlank()) throw new IOException("blank path segment is forbidden");
                Path next = Paths.get(segment);
                if (next.isAbsolute()) throw new IOException("absolute child paths are forbidden: " + segment);
                p = p.resolve(next);
            }
        }
        String normalized = p.normalize().toString();
        if (normalized.startsWith("..") || normalized.contains(".." + java.io.File.separator)) throw new IOException("path traversal is forbidden: " + normalized);
        return normalized;
    }

    private static void writeAtomic(Path tmp, Path target, byte[] data) throws IOException {
        Files.createDirectories(target.getParent());
        try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            channel.write(ByteBuffer.wrap(data));
            channel.force(true);
        }
        atomicMove(tmp, target);
    }

    private static Path atomicMove(Path source, Path target) throws IOException {
        try {
            return Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            return Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static String utf8Preview(Path path, int maxBytes) throws IOException {
        int limit = Math.max(1, Math.min(maxBytes, 8192));
        byte[] all = Files.readAllBytes(path);
        int len = Math.min(limit, all.length);
        return new String(all, 0, len, StandardCharsets.UTF_8);
    }
}
