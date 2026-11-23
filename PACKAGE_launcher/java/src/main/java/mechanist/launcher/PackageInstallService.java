package mechanist.launcher;

import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class PackageInstallService {
    private static final Pattern CLIENT_BLOCK = Pattern.compile("\"client\"\\s*:\\s*\\{([^}]*)}", Pattern.DOTALL);
    private static final Pattern SERVER_BLOCK = Pattern.compile("\"server\"\\s*:\\s*\\{([^}]*)}", Pattern.DOTALL);
    private static final Pattern SUPPORT_BLOCK = Pattern.compile("\"support_libraries\"\\s*:\\s*\\[([^]]*)]", Pattern.DOTALL);
    private static final Pattern OBJECT_BLOCK = Pattern.compile("\\{([^}]*)}");
    private static final Pattern SCHEMA_FIELD = Pattern.compile("\"schema\"\\s*:\\s*(\\d+)");
    private static final Pattern DISTRIBUTION_MODEL_FIELD = Pattern.compile("\"distribution_model\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern VERSION_FIELD = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PLATFORM_FIELD = Pattern.compile("\"platform\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATH_FIELD = Pattern.compile("\"path\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SHA_FIELD = Pattern.compile("\"sha256\"\\s*:\\s*\"([a-fA-F0-9]{64})\"");
    private static final Pattern SIZE_FIELD = Pattern.compile("\"size\"\\s*:\\s*(\\d+)");

    private final LauncherConfig config;

    PackageInstallService(LauncherConfig config) {
        this.config = config;
    }

    void ensureDirectories() throws IOException {
        Files.createDirectories(config.saveDir);
        Files.createDirectories(config.settingsDir);
        Files.createDirectories(config.logsDir);
        Files.createDirectories(config.cacheDir);
    }

    void installOrUpdate(String channel) throws IOException {
        ensureDirectories();
        if (Files.isDirectory(config.packageSeedRoot)) {
            acquireFromSeed(config.packageSeedRoot, channel);
            return;
        }
        PackageIdentity identity = verifyInstalledPackages();
        if (identity.ready()) return;
        throw new IOException("Package acquisition is manifest-controlled and no verified package set is installed for channel '"
                + channel + "'. Place a verified package seed at " + config.packageSeedRoot + " or run the future remote acquisition route; the launcher will not clone the full development repository.");
    }

    void repair(String channel) throws IOException {
        ensureDirectories();
        PackageIdentity identity = verifyInstalledPackages();
        if (identity.ready()) return;
        Path rollback = latestRollback();
        if (rollback != null) {
            restoreRollback(rollback);
            PackageIdentity restored = verifyInstalledPackages();
            if (restored.ready()) return;
            throw new IOException("Rollback restored from " + rollback + " but package verification still failed: " + restored.summary());
        }
        if (Files.isDirectory(config.packageSeedRoot)) {
            acquireFromSeed(config.packageSeedRoot, channel);
            return;
        }
        throw new IOException("Repair could not find a verified rollback or package seed for channel '"
                + channel + "'. Destructive repository reset is intentionally disabled.");
    }

    boolean gameLauncherPresent() {
        try {
            return verifyInstalledPackages().ready();
        } catch (IOException ex) {
            return false;
        }
    }

    PackageIdentity verifyInstalledPackages() throws IOException {
        Path manifest = findManifest();
        if (manifest == null) {
            return PackageIdentity.missing("No runtime manifest was found under " + config.manifestDir);
        }
        String text = Files.readString(manifest, StandardCharsets.UTF_8);
        ManifestMetadata metadata = parseMetadata(text);
        Artifact client = parseArtifact(text, CLIENT_BLOCK, "client");
        Artifact server = parseArtifact(text, SERVER_BLOCK, "server");
        List<Artifact> supportLibraries = parseSupportArtifacts(text);
        ArrayList<String> problems = new ArrayList<>();
        verifyMetadata(metadata, problems);
        verifyArtifact(manifest, client, problems);
        verifyArtifact(manifest, server, problems);
        for (Artifact support : supportLibraries) {
            verifyArtifact(manifest, support, problems);
        }
        if (!supportLibraries.isEmpty() && !Files.isDirectory(config.supportLibraryDir)) {
            problems.add("Support library directory is missing: " + config.supportLibraryDir);
        }
        verifyStartupAssets(config.installRoot, problems);
        return new PackageIdentity(manifest, metadata, client, server, supportLibraries, List.copyOf(problems));
    }

    /**
     * The startup portrait chooser must never be reported "ready" when its
     * authored 64-image package is absent. Verify client portraits and the
     * bundled audio manifest too; the JAR-only checks previously missed them.
     */
    private static void verifyStartupAssets(Path root, ArrayList<String> problems) {
        Path launcher = root.resolve("profile-packages/human-8x8/assets");
        Path client = root.resolve("packages/client/assets/graphics/packages/default_32/Protraits");
        for (int row = 1; row <= 8; row++) {
            for (int column = 1; column <= 8; column++) {
                String filename = String.format(java.util.Locale.ROOT,
                        "Humans8x8_r%02dc%02d_32px.png", row, column);
                verifyPortrait(launcher.resolve(filename), problems);
                verifyPortrait(client.resolve(filename), problems);
            }
        }
        Path music = root.resolve("packages/client/assets/music");
        Path manifest = music.resolve("music_manifest.tsv");
        if (!Files.isRegularFile(manifest)) {
            problems.add("Client music manifest is missing: " + manifest);
        } else {
            try {
                int tracks = 0;
                for (String line : Files.readAllLines(manifest, StandardCharsets.UTF_8)) {
                    if (line.isBlank() || line.stripLeading().startsWith("#")) continue;
                    String[] fields = line.split("\t");
                    if (fields.length < 2 || fields[0].isBlank()) {
                        problems.add("Invalid client music manifest row: " + line);
                        continue;
                    }
                    String filename = fields[1].trim();
                    if (!filename.endsWith(".wav") || filename.startsWith(".")
                            || filename.contains("/") || filename.contains("\\")) {
                        problems.add("Unsafe client music filename: " + filename);
                        continue;
                    }
                    Path track = music.resolve("wav").resolve(filename);
                    if (!Files.isRegularFile(track)) {
                        problems.add("Client music track is missing: " + filename);
                    }
                    tracks++;
                }
                if (tracks == 0) problems.add("Client music manifest contains no tracks");
            } catch (IOException exception) {
                problems.add("Client music manifest unreadable: " + exception.getMessage());
            }
        }
        Path core = root.resolve("packages/client/assets/sound/core/footstep_metal_01.wav");
        if (!Files.isRegularFile(core)) {
            problems.add("Client core audio is missing: " + core);
        }
    }

    private static void verifyPortrait(Path portrait, ArrayList<String> problems) {
        if (!Files.isRegularFile(portrait)) {
            problems.add("Required character portrait is missing: " + portrait);
            return;
        }
        try {
            BufferedImage image = ImageIO.read(portrait.toFile());
            if (image == null || image.getWidth() != 32 || image.getHeight() != 32) {
                problems.add("Character portrait is not a usable 32x32 image: " + portrait);
            }
        } catch (IOException exception) {
            problems.add("Character portrait cannot be decoded: " + portrait);
        }
    }

    private void copyStartupAssetsFromSeed(Path seedRoot, Path rollback) throws IOException {
        for (String relative : List.of(
                "profile-packages/human-8x8",
                "packages/client/assets/graphics/packages/default_32/Protraits",
                "packages/client/assets/music",
                "packages/client/assets/sound/core")) {
            Path source = seedRoot.resolve(relative).toAbsolutePath().normalize();
            if (!source.startsWith(seedRoot.toAbsolutePath().normalize()) || !Files.isDirectory(source)) {
                throw new IOException("Startup package seed path missing or unsafe: " + relative);
            }
            try (Stream<Path> walk = Files.walk(source)) {
                for (Path file : walk.filter(Files::isRegularFile).toList()) {
                    if (Files.isSymbolicLink(file)) throw new IOException("Linked startup seed asset: " + file);
                    Path destination = config.installRoot.resolve(relative)
                            .resolve(source.relativize(file)).normalize();
                    if (!destination.toAbsolutePath().normalize()
                            .startsWith(config.installRoot.toAbsolutePath().normalize())) {
                        throw new IOException("Startup seed asset escapes install root: " + file);
                    }
                    backupPath(destination, rollback);
                    Files.createDirectories(destination.getParent());
                    Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    private void acquireFromSeed(Path seedRoot, String channel) throws IOException {
        Path seedManifest = findManifest(seedRoot.resolve("manifests"));
        if (seedManifest == null) seedManifest = findManifest(seedRoot);
        if (seedManifest == null) throw new IOException("Package seed for channel '" + channel + "' has no runtime manifest: " + seedRoot);

        String text = Files.readString(seedManifest, StandardCharsets.UTF_8);
        ManifestMetadata metadata = parseMetadata(text);
        Artifact client = parseArtifact(text, CLIENT_BLOCK, "client");
        Artifact server = parseArtifact(text, SERVER_BLOCK, "server");
        List<Artifact> support = parseSupportArtifacts(text);
        ArrayList<String> problems = new ArrayList<>();
        verifyMetadata(metadata, problems);
        verifySeedArtifact(seedRoot, seedManifest, client, problems);
        verifySeedArtifact(seedRoot, seedManifest, server, problems);
        for (Artifact artifact : support) verifySeedArtifact(seedRoot, seedManifest, artifact, problems);
        verifyStartupAssets(seedRoot, problems);
        if (!problems.isEmpty()) throw new IOException("Package seed failed verification: " + String.join("; ", problems));

        Path rollback = createRollbackDirectory();
        copyArtifactFromSeed(seedRoot, client, rollback);
        copyArtifactFromSeed(seedRoot, server, rollback);
        for (Artifact artifact : support) copyArtifactFromSeed(seedRoot, artifact, rollback);
        copyStartupAssetsFromSeed(seedRoot, rollback);
        Files.createDirectories(config.manifestDir);
        backupPath(config.manifestDir.resolve(seedManifest.getFileName()), rollback);
        Files.copy(seedManifest, config.manifestDir.resolve(seedManifest.getFileName()), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

        PackageIdentity installed = verifyInstalledPackages();
        if (!installed.ready()) {
            restoreRollback(rollback);
            throw new IOException("Installed package seed failed verification and previous files were restored: " + installed.summary());
        }
    }

    Path clientJar() throws IOException {
        PackageIdentity identity = verifyInstalledPackages();
        if (!identity.ready()) throw new IOException(identity.summary());
        return identity.client().absolutePath(config.installRoot);
    }

    List<Path> supportJars() throws IOException {
        if (!Files.isDirectory(config.supportLibraryDir)) return List.of();
        try (Stream<Path> stream = Files.walk(config.supportLibraryDir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".jar"))
                    .sorted()
                    .toList();
        }
    }

    private static List<Artifact> parseSupportArtifacts(String text) throws IOException {
        Matcher supportBlock = SUPPORT_BLOCK.matcher(text);
        if (!supportBlock.find()) return List.of();
        ArrayList<Artifact> artifacts = new ArrayList<>();
        Matcher object = OBJECT_BLOCK.matcher(supportBlock.group(1));
        int index = 0;
        while (object.find()) {
            String body = object.group(1);
            String path = field(body, PATH_FIELD, "support_libraries[" + index + "].path");
            String sha = field(body, SHA_FIELD, "support_libraries[" + index + "].sha256");
            long size = Long.parseLong(field(body, SIZE_FIELD, "support_libraries[" + index + "].size"));
            artifacts.add(new Artifact("support library " + index, path, sha.toLowerCase(), size));
            index++;
        }
        return List.copyOf(artifacts);
    }

    private Path findManifest() throws IOException {
        return findManifest(config.manifestDir);
    }

    private static Path findManifest(Path manifestDir) throws IOException {
        if (!Files.isDirectory(manifestDir)) return null;
        try (Stream<Path> stream = Files.list(manifestDir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith("-runtime-manifest.json"))
                    .sorted()
                    .findFirst()
                    .orElse(null);
        }
    }

    private void verifySeedArtifact(Path seedRoot, Path manifest, Artifact artifact, ArrayList<String> problems) {
        verifyArtifact(seedRoot, manifest, artifact, problems);
    }

    private static Artifact parseArtifact(String text, Pattern blockPattern, String label) throws IOException {
        Matcher block = blockPattern.matcher(text);
        if (!block.find()) throw new IOException("Runtime manifest is missing " + label + " package block.");
        String body = block.group(1);
        String path = field(body, PATH_FIELD, label + ".path");
        String sha = field(body, SHA_FIELD, label + ".sha256");
        long size = Long.parseLong(field(body, SIZE_FIELD, label + ".size"));
        return new Artifact(label, path, sha.toLowerCase(), size);
    }

    private static ManifestMetadata parseMetadata(String text) throws IOException {
        int schema = Integer.parseInt(field(text, SCHEMA_FIELD, "schema"));
        String distributionModel = field(text, DISTRIBUTION_MODEL_FIELD, "distribution_model");
        String version = field(text, VERSION_FIELD, "version");
        String platform = field(text, PLATFORM_FIELD, "platform");
        return new ManifestMetadata(schema, distributionModel, version, platform);
    }

    private static void verifyMetadata(ManifestMetadata metadata, ArrayList<String> problems) {
        if (metadata.schema != 2) {
            problems.add("Unsupported runtime manifest schema: " + metadata.schema);
        }
        if (!"installer-thin-launcher-client-server".equals(metadata.distributionModel)) {
            problems.add("Unsupported distribution model: " + metadata.distributionModel);
        }
        String expectedPlatform = currentPlatform();
        if (!expectedPlatform.equals(metadata.platform)) {
            problems.add("Runtime manifest platform mismatch: expected " + expectedPlatform + ", found " + metadata.platform);
        }
    }

    private static String currentPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();
        String cpu = (arch.contains("64") || arch.contains("amd64") || arch.contains("x86_64")) ? "x64" : arch.replaceAll("[^a-z0-9]+", "");
        if (os.contains("win")) return "windows-" + cpu;
        if (os.contains("linux")) return "linux-" + cpu;
        if (os.contains("mac") || os.contains("darwin")) return "macos-" + cpu;
        return os.replaceAll("[^a-z0-9]+", "") + "-" + cpu;
    }

    private static String field(String body, Pattern pattern, String name) throws IOException {
        Matcher m = pattern.matcher(body);
        if (!m.find()) throw new IOException("Runtime manifest is missing " + name + ".");
        return m.group(1);
    }

    private void verifyArtifact(Path manifest, Artifact artifact, ArrayList<String> problems) {
        verifyArtifact(config.installRoot, manifest, artifact, problems);
    }

    private void verifyArtifact(Path root, Path manifest, Artifact artifact, ArrayList<String> problems) {
        Path path = artifact.absolutePath(root);
        try {
            Path normalizedRoot = root.toAbsolutePath().normalize();
            Path normalizedPath = path.toAbsolutePath().normalize();
            if (!normalizedPath.startsWith(normalizedRoot)) {
                problems.add(artifact.label + " path escapes install root: " + artifact.relativePath);
                return;
            }
            if (!Files.isRegularFile(normalizedPath)) {
                problems.add(artifact.label + " package is missing: " + artifact.relativePath);
                return;
            }
            long size = Files.size(normalizedPath);
            if (size != artifact.size) {
                problems.add(artifact.label + " package size mismatch in " + manifest.getFileName() + ": expected " + artifact.size + ", found " + size);
            }
            String sha = sha256(normalizedPath);
            if (!sha.equals(artifact.sha256)) {
                problems.add(artifact.label + " package hash mismatch: " + artifact.relativePath);
            }
        } catch (IOException ex) {
            problems.add(artifact.label + " package verification failed: " + ex.getMessage());
        }
    }

    private void copyArtifactFromSeed(Path seedRoot, Artifact artifact, Path rollback) throws IOException {
        Path source = artifact.absolutePath(seedRoot);
        Path target = artifact.absolutePath(config.installRoot);
        backupPath(target, rollback);
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private Path createRollbackDirectory() throws IOException {
        Path rollback = rollbackRoot().resolve(Instant.now().toString().replace(':', '-'));
        Files.createDirectories(rollback);
        return rollback;
    }

    private Path rollbackRoot() {
        return config.cacheDir.resolve("package-rollback");
    }

    private void backupPath(Path target, Path rollback) throws IOException {
        if (!Files.isRegularFile(target)) return;
        Path relative = config.installRoot.toAbsolutePath().normalize().relativize(target.toAbsolutePath().normalize());
        Path backup = rollback.resolve(relative);
        Path parent = backup.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private Path latestRollback() throws IOException {
        Path root = rollbackRoot();
        if (!Files.isDirectory(root)) return null;
        try (Stream<Path> stream = Files.list(root)) {
            return stream.filter(Files::isDirectory)
                    .max(Comparator.comparing(p -> p.getFileName().toString()))
                    .orElse(null);
        }
    }

    private void restoreRollback(Path rollback) throws IOException {
        if (!Files.isDirectory(rollback)) return;
        try (Stream<Path> stream = Files.walk(rollback)) {
            List<Path> files = stream.filter(Files::isRegularFile).toList();
            for (Path source : files) {
                Path relative = rollback.relativize(source);
                Path target = config.installRoot.resolve(relative).normalize();
                if (!target.toAbsolutePath().normalize().startsWith(config.installRoot.toAbsolutePath().normalize())) {
                    throw new IOException("Rollback path escapes install root: " + relative);
                }
                Path parent = target.getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            }
        }
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] data = Files.readAllBytes(file);
            return HexFormat.of().formatHex(digest.digest(data)).toLowerCase();
        } catch (Exception ex) {
            throw new IOException("Could not hash " + file, ex);
        }
    }

    record Artifact(String label, String relativePath, String sha256, long size) {
        Path absolutePath(Path installRoot) {
            return installRoot.resolve(relativePath.replace('/', java.io.File.separatorChar)).normalize();
        }
    }

    record ManifestMetadata(int schema, String distributionModel, String version, String platform) {}

    record PackageIdentity(Path manifest, ManifestMetadata metadata, Artifact client, Artifact server, List<Artifact> supportLibraries, List<String> problems) {
        boolean ready() {
            return manifest != null && problems.isEmpty();
        }

        String summary() {
            if (ready()) return "Package manifest verified: " + manifest;
            if (problems.isEmpty()) return "Package manifest is not ready.";
            return String.join("; ", problems);
        }

        static PackageIdentity missing(String problem) {
            return new PackageIdentity(null, null, null, null, List.of(), List.of(problem));
        }
    }
}
