package mechanist.assets;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only in-memory lookup table for semantic asset metadata.
 *
 * Stage 1 keeps the registry file deliberately simple: tab-separated columns
 * under assets/indexes/. Later stages may add authoring tools or JSON export,
 * but the runtime lookup API should remain ID based.
 */
public final class AssetRegistry {
    public static final String DEFAULT_REGISTRY = "assets/indexes/semantic_asset_registry.tsv";

    private final Path projectRoot;
    private final Path registryFile;
    private final Map<String, AssetMetadata> byId;
    private final Map<AssetType, List<AssetMetadata>> byType;

    private AssetRegistry(Path projectRoot, Path registryFile, Map<String, AssetMetadata> entries) {
        this.projectRoot = projectRoot == null ? Paths.get("").toAbsolutePath().normalize() : projectRoot.toAbsolutePath().normalize();
        this.registryFile = registryFile == null ? this.projectRoot.resolve(DEFAULT_REGISTRY) : registryFile.toAbsolutePath().normalize();
        this.byId = Map.copyOf(entries);
        this.byType = groupByType(entries);
    }

    public static AssetRegistry empty() {
        return new AssetRegistry(Paths.get("").toAbsolutePath().normalize(), null, Map.of());
    }

    public static AssetRegistry loadDefault(Path projectRoot) throws IOException {
        Path root = projectRoot == null ? Paths.get("").toAbsolutePath().normalize() : projectRoot.toAbsolutePath().normalize();
        return loadFromTsv(root.resolve(DEFAULT_REGISTRY), root);
    }

    public static AssetRegistry loadFromTsv(Path registryFile, Path projectRoot) throws IOException {
        Objects.requireNonNull(registryFile, "registryFile cannot be null");
        Path root = projectRoot == null ? registryFile.toAbsolutePath().normalize().getParent() : projectRoot.toAbsolutePath().normalize();
        Map<String, AssetMetadata> entries = new LinkedHashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(registryFile, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank() || line.stripLeading().startsWith("#")) {
                    continue;
                }
                if (lineNo == 1 && line.toLowerCase(Locale.ROOT).startsWith("id\t")) {
                    continue;
                }
                String[] parts = line.split("\t", 5);
                if (parts.length != 5) {
                    throw new IOException("Invalid semantic asset registry row at line " + lineNo + ": expected 5 tab-separated fields");
                }
                AssetMetadata metadata;
                try {
                    metadata = new AssetMetadata(
                            parts[0],
                            parts[2],
                            parts[3],
                            AssetType.fromToken(parts[1]),
                            parts[4]
                    );
                } catch (RuntimeException ex) {
                    throw new IOException("Invalid semantic asset registry row at line " + lineNo + ": " + ex.getMessage(), ex);
                }
                AssetMetadata duplicate = entries.putIfAbsent(metadata.id(), metadata);
                if (duplicate != null) {
                    throw new IOException("Duplicate semantic asset ID " + metadata.id()
                            + " at line " + lineNo
                            + "; first path=" + duplicate.pathOrUri()
                            + "; duplicate path=" + metadata.pathOrUri());
                }
            }
        }

        return new AssetRegistry(root, registryFile, entries);
    }

    public Path projectRoot() {
        return projectRoot;
    }

    public Path registryFile() {
        return registryFile;
    }

    public Optional<AssetMetadata> find(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(assetId.trim().toUpperCase(Locale.ROOT)));
    }

    public AssetMetadata require(String assetId) {
        return find(assetId).orElseThrow(() -> new NoSuchElementException("Unknown semantic asset ID: " + assetId));
    }

    public List<AssetMetadata> all() {
        return byId.values().stream()
                .sorted(Comparator.comparing(AssetMetadata::id))
                .toList();
    }

    public List<AssetMetadata> byType(AssetType type) {
        return byType.getOrDefault(type, List.of());
    }

    public List<AssetMetadata> search(String filter) {
        return all().stream().filter(asset -> asset.matchesFilter(filter)).toList();
    }

    public int size() {
        return byId.size();
    }

    public Path resolvePath(AssetMetadata metadata) {
        Objects.requireNonNull(metadata, "metadata cannot be null");
        String value = metadata.pathOrUri();
        if (value.startsWith("classpath:")) {
            throw new IllegalArgumentException("Classpath assets do not have a filesystem Path: " + value);
        }
        if (looksLikeUri(value)) {
            try {
                URI uri = URI.create(value);
                if ("file".equalsIgnoreCase(uri.getScheme())) {
                    return Paths.get(uri).toAbsolutePath().normalize();
                }
            } catch (RuntimeException ignored) {
                // Fall through and treat it as a plain path for better diagnostics.
            }
        }
        try {
            Path p = Paths.get(value);
            if (!p.isAbsolute()) {
                p = projectRoot.resolve(p);
            }
            return p.toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            throw new IllegalArgumentException("Invalid path for semantic asset " + metadata.id() + ": " + value, ex);
        }
    }

    public RegistryAudit audit(boolean requirePathExistence) {
        int duplicateIds = 0; // loadFromTsv fails before construction if duplicates are present.
        int unknownTypes = 0;
        int blankDescriptions = 0;
        int missingPaths = 0;
        List<String> lines = new ArrayList<>();
        for (AssetMetadata metadata : all()) {
            if (metadata.type() == AssetType.UNKNOWN) {
                unknownTypes++;
                lines.add(metadata.id() + " has UNKNOWN type");
            }
            if (metadata.semanticDescription().isBlank()) {
                blankDescriptions++;
                lines.add(metadata.id() + " has blank description");
            }
            if (requirePathExistence && !metadata.pathOrUri().startsWith("classpath:")) {
                Path path = resolvePath(metadata);
                if (!Files.exists(path)) {
                    missingPaths++;
                    lines.add(metadata.id() + " missing path " + path);
                }
            }
        }
        return new RegistryAudit(size(), byType.keySet().stream().filter(t -> !byType(t).isEmpty()).count(), duplicateIds, unknownTypes, blankDescriptions, missingPaths, List.copyOf(lines));
    }

    private static Map<AssetType, List<AssetMetadata>> groupByType(Map<String, AssetMetadata> entries) {
        EnumMap<AssetType, List<AssetMetadata>> grouped = new EnumMap<>(AssetType.class);
        for (AssetType type : AssetType.values()) {
            grouped.put(type, new ArrayList<>());
        }
        for (AssetMetadata metadata : entries.values()) {
            grouped.computeIfAbsent(metadata.type(), ignored -> new ArrayList<>()).add(metadata);
        }
        EnumMap<AssetType, List<AssetMetadata>> frozen = new EnumMap<>(AssetType.class);
        for (Map.Entry<AssetType, List<AssetMetadata>> entry : grouped.entrySet()) {
            frozen.put(entry.getKey(), entry.getValue().stream()
                    .sorted(Comparator.comparing(AssetMetadata::name, String.CASE_INSENSITIVE_ORDER).thenComparing(AssetMetadata::id))
                    .toList());
        }
        return Collections.unmodifiableMap(frozen);
    }

    private static boolean looksLikeUri(String value) {
        int colon = value.indexOf(':');
        if (colon <= 1) {
            return false; // Avoid treating Windows drive letters as URI schemes.
        }
        for (int i = 0; i < colon; i++) {
            char c = value.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '+' && c != '-' && c != '.') {
                return false;
            }
        }
        return true;
    }

    public record RegistryAudit(
            int entryCount,
            long categoryCount,
            int duplicateIdCount,
            int unknownTypeCount,
            int blankDescriptionCount,
            int missingPathCount,
            List<String> lines
    ) {
        public boolean passed() {
            return duplicateIdCount == 0 && unknownTypeCount == 0 && blankDescriptionCount == 0 && missingPathCount == 0;
        }
    }
}
