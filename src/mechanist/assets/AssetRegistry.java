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
        Path registry = root.resolve(DEFAULT_REGISTRY);
        if (Files.isRegularFile(registry)) {
            return loadFromTsv(registry, root);
        }
        Optional<Path> compiledIndex = firstExistingCompiledIndex(root);
        if (compiledIndex.isPresent()) {
            return loadFromCompiledContentIndex(compiledIndex.orElseThrow(), root);
        }
        return loadFromTsv(registry, root);
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

    public static AssetRegistry loadFromCompiledContentIndex(Path indexFile, Path projectRoot) throws IOException {
        Objects.requireNonNull(indexFile, "indexFile cannot be null");
        Path root = projectRoot == null ? indexFile.toAbsolutePath().normalize().getParent() : projectRoot.toAbsolutePath().normalize();
        Path index = indexFile.toAbsolutePath().normalize();
        Path assetRoot = compiledAssetRootFor(index);
        Map<String, AssetMetadata> entries = new LinkedHashMap<>();
        EnumMap<AssetType, Integer> counters = new EnumMap<>(AssetType.class);

        try (BufferedReader reader = Files.newBufferedReader(index, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) {
                return new AssetRegistry(root, index, entries);
            }
            Map<String, Integer> columns = columns(header);
            String line;
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank() || line.stripLeading().startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\t", -1);
                String sourceId = value(parts, columns, "asset_id");
                String relativePath = value(parts, columns, "path");
                if (sourceId.isBlank() || relativePath.isBlank()) {
                    continue;
                }
                String category = value(parts, columns, "category");
                String sourceGroup = value(parts, columns, "source_group");
                String sourceAtlas = value(parts, columns, "source_atlas");
                String contentType = value(parts, columns, "content_type");
                String tags = value(parts, columns, "content_tags");
                String description = value(parts, columns, "description");
                AssetType type = compiledAssetType(category, sourceGroup, sourceAtlas, contentType, tags);
                String id = syntheticId(type, counters);
                String path = metadataPath(root, assetRoot.resolve(relativePath.replace('\\', '/')).normalize());
                String name = compiledAssetName(sourceAtlas, sourceId, value(parts, columns, "row"), value(parts, columns, "col"));
                String semantic = compiledAssetDescription(description, category, sourceGroup, sourceAtlas, tags);
                try {
                    entries.put(id, new AssetMetadata(id, path, name, type, semantic));
                } catch (RuntimeException ex) {
                    throw new IOException("Invalid compiled asset index row at line " + lineNo + ": " + ex.getMessage(), ex);
                }
            }
        }

        return new AssetRegistry(root, index, entries);
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

    private static Optional<Path> firstExistingCompiledIndex(Path root) {
        List<String> candidates = List.of(
                "assets/indexes/asset_content_index_32px.tsv",
                "assets/indexes/asset_content_index_256px.tsv",
                "PACKAGE_client/assets/indexes/asset_content_index_32px.tsv",
                "PACKAGE_client/assets/indexes/asset_content_index_256px.tsv",
                "ROOT_tools/atlas_asset_pipeline/compiled_assets/asset_content_index_32px.tsv",
                "ROOT_tools/atlas_asset_pipeline/compiled_assets/asset_content_index_256px.tsv"
        );
        for (String candidate : candidates) {
            Path path = root.resolve(candidate).normalize();
            if (Files.isRegularFile(path)) {
                return Optional.of(path);
            }
        }
        return Optional.empty();
    }

    private static Path compiledAssetRootFor(Path indexFile) {
        Path parent = indexFile.getParent();
        if (parent != null && parent.getFileName() != null && "indexes".equalsIgnoreCase(parent.getFileName().toString())) {
            Path assets = parent.getParent();
            if (assets != null) {
                return assets.resolve("compiled_assets").normalize();
            }
        }
        return parent == null ? Paths.get("").toAbsolutePath().normalize() : parent.normalize();
    }

    private static Map<String, Integer> columns(String header) {
        LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
        String[] names = header == null ? new String[0] : header.split("\t", -1);
        for (int i = 0; i < names.length; i++) {
            out.put(names[i].trim().toLowerCase(Locale.ROOT), i);
        }
        return out;
    }

    private static String value(String[] parts, Map<String, Integer> columns, String column) {
        Integer index = columns.get(column);
        if (index == null || index < 0 || index >= parts.length) {
            return "";
        }
        return parts[index] == null ? "" : parts[index].trim();
    }

    private static String metadataPath(Path root, Path assetPath) {
        try {
            return root.relativize(assetPath).toString().replace('\\', '/');
        } catch (IllegalArgumentException ex) {
            return assetPath.toString().replace('\\', '/');
        }
    }

    private static AssetType compiledAssetType(String category, String sourceGroup, String sourceAtlas, String contentType, String tags) {
        String text = (category + " " + sourceGroup + " " + sourceAtlas + " " + contentType + " " + tags).toLowerCase(Locale.ROOT);
        if (containsAny(text, "portrait", "protrait", "profile", "human", "cultist", "ganger", "noble", "servitor", "clerk", "cleric")) return AssetType.PORTRAIT;
        if (containsAny(text, "corpse", "decay", "dead")) return AssetType.CORPSE_DECAY;
        if (containsAny(text, "weapon", "weapons", "ammo", "firearm", "blade")) return AssetType.WEAPON_ICON;
        if (containsAny(text, "armor", "armors", "clothing", "helmet")) return AssetType.ARMOR_ICON;
        if (containsAny(text, "system", "ui", "interface", "rondel", "knowledge", "skill", "icon")) return AssetType.UI_ICON;
        if (containsAny(text, "road", "street", "vehicle_path")) return AssetType.ROAD_TILE;
        if (containsAny(text, "sidewalk", "pavement")) return AssetType.SIDEWALK_TILE;
        if (containsAny(text, "corridor", "walkway")) return AssetType.CORRIDOR_TILE;
        if (containsAny(text, "floor", "floors", "ground", "void")) return AssetType.FLOOR_TILE;
        if (containsAny(text, "wall", "walls", "bulkhead")) return AssetType.WALL_TILE;
        if (containsAny(text, "machine", "machinery", "vehicle", "automotive", "vending", "emergency_machines")) return AssetType.MACHINE;
        if (containsAny(text, "door", "defense", "fixture", "counter", "table", "station")) return AssetType.FIXTURE;
        if (containsAny(text, "item", "items", "implant", "drug", "narcotic", "reagent", "goods", "loot", "relic", "journal", "paper")) return AssetType.ITEM_ICON;
        if (containsAny(text, "object", "objects", "decor", "furniture", "prop")) return AssetType.OBJECT;
        return AssetType.OBJECT;
    }

    private static boolean containsAny(String text, String... needles) {
        if (text == null || text.isBlank()) return false;
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && text.contains(needle)) return true;
        }
        return false;
    }

    private static String syntheticId(AssetType type, EnumMap<AssetType, Integer> counters) {
        AssetType safe = type == null ? AssetType.UNKNOWN : type;
        int next = counters.getOrDefault(safe, 0) + 1;
        counters.put(safe, next);
        return prefixFor(safe) + "-" + String.format(Locale.ROOT, "%04d", next);
    }

    private static String prefixFor(AssetType type) {
        return switch (type == null ? AssetType.UNKNOWN : type) {
            case PORTRAIT -> "POR";
            case WALL_TILE -> "WAL";
            case FLOOR_TILE -> "FLO";
            case ROAD_TILE -> "ROA";
            case SIDEWALK_TILE -> "SID";
            case CORRIDOR_TILE -> "COR";
            case OBJECT -> "OBJ";
            case MACHINE -> "MAC";
            case FIXTURE -> "FIX";
            case ITEM_ICON -> "ITE";
            case WEAPON_ICON -> "WEA";
            case ARMOR_ICON -> "ARM";
            case UI_ICON -> "UIX";
            case CORPSE_DECAY -> "DEC";
            case INTERNAL -> "INT";
            case UNKNOWN -> "UNK";
        };
    }

    private static String compiledAssetName(String sourceAtlas, String sourceId, String row, String col) {
        String base = !sourceAtlas.isBlank() ? sourceAtlas : sourceId;
        base = base == null || base.isBlank() ? "Compiled asset" : base.replace('_', ' ').replace('-', ' ').trim();
        if (!row.isBlank() && !col.isBlank()) {
            return base + " r" + row + "c" + col;
        }
        return base;
    }

    private static String compiledAssetDescription(String description, String category, String sourceGroup, String sourceAtlas, String tags) {
        if (description != null && !description.isBlank()) {
            return description.trim();
        }
        StringBuilder out = new StringBuilder();
        out.append("Compiled ").append(category == null || category.isBlank() ? "asset" : category.trim()).append(" asset");
        if (sourceGroup != null && !sourceGroup.isBlank()) out.append(" from ").append(sourceGroup.trim());
        if (sourceAtlas != null && !sourceAtlas.isBlank()) out.append(" / ").append(sourceAtlas.trim());
        if (tags != null && !tags.isBlank()) out.append(". Tags: ").append(tags.trim());
        return out.toString();
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
