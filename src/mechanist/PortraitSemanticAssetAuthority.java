package mechanist;

import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetRegistry;
import mechanist.assets.AssetType;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Stage 8 bridge between portrait/entity-art pools and the Semantic Asset Registry.
 *
 * <p>This authority does not replace the existing portrait loaders yet. It makes the
 * partition rules auditable through data: each portrait asset has an ID, registry
 * row, partition key, and explicit allowed pool flags. Runtime loaders may keep
 * their existing image slices until Stage 9+ retires the old fallbacks.</p>
 */
public final class PortraitSemanticAssetAuthority {
    public static final String VERSION = "0.9.10ka-stage8-portrait-entity-partitions";
    public static final String DEFAULT_PARTITION_INDEX = "assets/indexes/semantic_portrait_entity_partitions.tsv";

    private PortraitSemanticAssetAuthority() {}

    public record PartitionRecord(
            String assetId,
            String partitionKey,
            String partitionLabel,
            boolean playerCreationAllowed,
            boolean npcPoolAllowed,
            boolean nameLockedOnly,
            boolean nonhumanOrRestricted,
            String registryPath,
            String notes
    ) {
        public PartitionRecord {
            assetId = normalizeAssetId(assetId);
            partitionKey = requireText(partitionKey, "partition key");
            partitionLabel = requireText(partitionLabel, "partition label");
            registryPath = requireText(registryPath, "registry path");
            notes = notes == null ? "" : notes.trim();
        }

        public String compactLine() {
            return assetId + " — " + partitionLabel + " [" + partitionKey + "]"
                    + " player=" + playerCreationAllowed
                    + " npc=" + npcPoolAllowed
                    + " nameLocked=" + nameLockedOnly
                    + " restricted=" + nonhumanOrRestricted;
        }
    }

    public record PartitionAudit(
            int recordCount,
            int partitionCount,
            int playerPoolCount,
            int npcPoolCount,
            int nameLockedCount,
            int nonhumanOrRestrictedCount,
            List<String> errors
    ) {
        public boolean passed() { return errors.isEmpty(); }

        public String summaryLine() {
            return "records=" + recordCount
                    + " partitions=" + partitionCount
                    + " playerPool=" + playerPoolCount
                    + " npcPool=" + npcPoolCount
                    + " nameLocked=" + nameLockedCount
                    + " nonhumanOrRestricted=" + nonhumanOrRestrictedCount
                    + " errors=" + errors.size();
        }
    }

    public static List<PartitionRecord> loadDefault(Path projectRoot) throws IOException {
        Path root = projectRoot == null ? Path.of(".").toAbsolutePath().normalize() : projectRoot.toAbsolutePath().normalize();
        return load(root.resolve(DEFAULT_PARTITION_INDEX));
    }

    public static List<PartitionRecord> load(Path indexFile) throws IOException {
        if (indexFile == null || !Files.isRegularFile(indexFile)) {
            return List.of();
        }
        List<PartitionRecord> out = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(indexFile, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank() || line.stripLeading().startsWith("#")) continue;
                if (lineNo == 1 && line.toLowerCase(Locale.ROOT).startsWith("asset_id\t")) continue;
                String[] parts = line.split("\t", 9);
                if (parts.length != 9) {
                    throw new IOException("Invalid portrait partition row at line " + lineNo + ": expected 9 tab-separated fields");
                }
                out.add(new PartitionRecord(
                        parts[0], parts[1], parts[2], parseBoolean(parts[3]), parseBoolean(parts[4]),
                        parseBoolean(parts[5]), parseBoolean(parts[6]), parts[7], parts[8]
                ));
            }
        }
        return out.stream()
                .sorted(Comparator.comparing(PartitionRecord::partitionKey).thenComparing(PartitionRecord::assetId))
                .toList();
    }

    public static Optional<PartitionRecord> find(Path projectRoot, String assetId) {
        if (assetId == null || assetId.isBlank()) return Optional.empty();
        try {
            String target = normalizeAssetId(assetId);
            return loadDefault(projectRoot).stream().filter(r -> r.assetId().equals(target)).findFirst();
        } catch (IOException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    public static List<PartitionRecord> byPartition(Path projectRoot, String partitionKey) throws IOException {
        String key = partitionKey == null ? "" : partitionKey.trim().toLowerCase(Locale.ROOT);
        return loadDefault(projectRoot).stream()
                .filter(r -> r.partitionKey().equalsIgnoreCase(key))
                .toList();
    }

    public static List<PartitionRecord> playerCreationPool(Path projectRoot) throws IOException {
        return loadDefault(projectRoot).stream().filter(PartitionRecord::playerCreationAllowed).toList();
    }

    public static List<PartitionRecord> npcPool(Path projectRoot) throws IOException {
        return loadDefault(projectRoot).stream().filter(PartitionRecord::npcPoolAllowed).toList();
    }

    public static List<PartitionRecord> nameLockedPool(Path projectRoot) throws IOException {
        return loadDefault(projectRoot).stream().filter(PartitionRecord::nameLockedOnly).toList();
    }

    public static PartitionAudit audit(Path projectRoot, AssetRegistry registry) throws IOException {
        Path root = projectRoot == null ? Path.of(".").toAbsolutePath().normalize() : projectRoot.toAbsolutePath().normalize();
        AssetRegistry safeRegistry = registry == null ? AssetRegistry.loadDefault(root) : registry;
        List<PartitionRecord> records = loadDefault(root);
        ArrayList<String> errors = new ArrayList<>();
        Map<String, PartitionRecord> byId = new LinkedHashMap<>();
        Map<String, Integer> partitions = new LinkedHashMap<>();
        int player = 0;
        int npc = 0;
        int locked = 0;
        int restricted = 0;

        for (PartitionRecord record : records) {
            PartitionRecord prior = byId.putIfAbsent(record.assetId(), record);
            if (prior != null) errors.add("Duplicate portrait partition record for " + record.assetId());
            partitions.merge(record.partitionKey(), 1, Integer::sum);
            if (record.playerCreationAllowed()) player++;
            if (record.npcPoolAllowed()) npc++;
            if (record.nameLockedOnly()) locked++;
            if (record.nonhumanOrRestricted()) restricted++;

            Optional<AssetMetadata> metadata = safeRegistry.find(record.assetId());
            if (metadata.isEmpty()) {
                errors.add("Partition record points at unknown registry asset " + record.assetId());
                continue;
            }
            AssetMetadata m = metadata.get();
            if (m.type() != AssetType.PORTRAIT) {
                errors.add("Partition record " + record.assetId() + " is not a PORTRAIT asset: " + m.type());
            }
            if (!Objects.equals(m.pathOrUri(), record.registryPath())) {
                errors.add("Partition record " + record.assetId() + " path mismatch registry=" + m.pathOrUri() + " partition=" + record.registryPath());
            }
            if (record.nameLockedOnly() && (record.playerCreationAllowed() || record.npcPoolAllowed())) {
                errors.add("Name-locked asset " + record.assetId() + " leaked into ordinary player/NPC pools");
            }
            if (record.nonhumanOrRestricted() && record.playerCreationAllowed()) {
                errors.add("Restricted/nonhuman asset " + record.assetId() + " marked player-creation allowed");
            }
        }
        if (records.isEmpty()) errors.add("Portrait/entity partition index is empty");
        if (player <= 0) errors.add("No portrait entries marked as player-creation allowed");
        if (npc <= 0) errors.add("No portrait entries marked as NPC-pool allowed");
        if (locked <= 0) errors.add("No name-locked portrait entries found");
        if (restricted <= 0) errors.add("No restricted/nonhuman portrait entries found");
        if (!partitions.containsKey("name_locked_profile")) errors.add("Missing name_locked_profile partition");
        if (!partitions.containsKey("administratum")) errors.add("Missing administratum/baseline human portrait partition");
        if (!partitions.containsKey("rogue_automata_servitors")) errors.add("Missing servitor/automata portrait partition");
        if (!partitions.containsKey("pets")) errors.add("Missing pets portrait partition");

        return new PartitionAudit(records.size(), partitions.size(), player, npc, locked, restricted, List.copyOf(errors));
    }

    public static List<String> infopediaLines(Path projectRoot, String assetId) {
        Optional<PartitionRecord> found = find(projectRoot, assetId);
        if (found.isEmpty()) return List.of();
        PartitionRecord r = found.get();
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Portrait/entity partition: " + r.partitionLabel());
        lines.add("Partition key: " + r.partitionKey());
        lines.add("Player creation pool: " + yesNo(r.playerCreationAllowed()));
        lines.add("Ordinary NPC pool: " + yesNo(r.npcPoolAllowed()));
        lines.add("Name-locked only: " + yesNo(r.nameLockedOnly()));
        lines.add("Non-human/restricted: " + yesNo(r.nonhumanOrRestricted()));
        if (!r.notes().isBlank()) lines.add("Partition notes: " + r.notes());
        return lines;
    }

    private static boolean parseBoolean(String value) {
        String v = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return v.equals("true") || v.equals("yes") || v.equals("1") || v.equals("y");
    }

    private static String normalizeAssetId(String raw) {
        String id = requireText(raw, "asset ID").toUpperCase(Locale.ROOT);
        if (id.length() != 8) throw new IllegalArgumentException("asset ID must be exactly 8 characters: " + id);
        return id;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("blank " + label);
        return value.trim();
    }

    private static String yesNo(boolean value) { return value ? "YES" : "NO"; }
}
