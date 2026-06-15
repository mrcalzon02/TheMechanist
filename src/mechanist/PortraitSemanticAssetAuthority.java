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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Semantic authority for portrait/entity-art pools.
 *
 * <p>An authored partition index remains the preferred source of truth. When
 * that optional index is absent from a source checkout or packaged client, the
 * authority synthesizes equivalent partition records from active-registry
 * PORTRAIT metadata. Runtime selection therefore fails closed by asset type and
 * stable ID instead of silently returning an empty portrait system.</p>
 */
public final class PortraitSemanticAssetAuthority {
    public static final String VERSION = "0.9.10kf-runtime-portrait-families";
    public static final String DEFAULT_PARTITION_INDEX = "assets/indexes/semantic_portrait_entity_partitions.tsv";

    private static final List<String> PARTITION_INDEX_CANDIDATES = List.of(
            DEFAULT_PARTITION_INDEX,
            "PACKAGE_client/" + DEFAULT_PARTITION_INDEX,
            "client/" + DEFAULT_PARTITION_INDEX,
            "packages/client/" + DEFAULT_PARTITION_INDEX,
            "ROOT_tools/atlas_asset_pipeline/indexes/semantic_portrait_entity_partitions.tsv",
            "ROOT_tools/atlas_asset_pipeline/semantic_portrait_entity_partitions.tsv"
    );

    private static final Set<String> SEARCH_STOPWORDS = Set.of(
            "portrait", "portraits", "sheet", "pool", "entity", "entities",
            "class", "profile", "ordinary", "generic", "baseline", "human"
    );

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
        Path root = normalizedRoot(projectRoot);
        AssetRegistry registry = AssetRegistry.loadDefault(root);
        return loadDefault(root, registry);
    }

    public static String partitionSource(Path projectRoot) {
        Path root = normalizedRoot(projectRoot);
        Optional<Path> authored = firstExistingPartitionIndex(root);
        return authored.map(path -> root.relativize(path).toString().replace('\\', '/'))
                .orElse("active-registry synthesis");
    }

    private static List<PartitionRecord> loadDefault(Path root, AssetRegistry registry) throws IOException {
        Optional<Path> authored = firstExistingPartitionIndex(root);
        if (authored.isPresent()) return load(authored.orElseThrow());
        return synthesizeFromRegistry(registry == null ? AssetRegistry.loadDefault(root) : registry);
    }

    public static List<PartitionRecord> load(Path indexFile) throws IOException {
        if (indexFile == null || !Files.isRegularFile(indexFile)) return List.of();
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
                    throw new IOException("Invalid portrait partition row at line " + lineNo
                            + ": expected 9 tab-separated fields");
                }
                out.add(new PartitionRecord(
                        parts[0], parts[1], parts[2], parseBoolean(parts[3]), parseBoolean(parts[4]),
                        parseBoolean(parts[5]), parseBoolean(parts[6]), parts[7], parts[8]
                ));
            }
        }
        return ordered(out);
    }

    public static Optional<PartitionRecord> find(Path projectRoot, String assetId) {
        if (assetId == null || assetId.isBlank()) return Optional.empty();
        try {
            String target = normalizeAssetId(assetId);
            return loadDefault(projectRoot).stream()
                    .filter(record -> record.assetId().equals(target))
                    .findFirst();
        } catch (IOException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    public static List<PartitionRecord> byPartition(Path projectRoot, String partitionKey) throws IOException {
        String key = partitionKey == null ? "" : partitionKey.trim().toLowerCase(Locale.ROOT);
        return loadDefault(projectRoot).stream()
                .filter(record -> record.partitionKey().equalsIgnoreCase(key))
                .toList();
    }

    public static List<PartitionRecord> playerCreationPool(Path projectRoot) throws IOException {
        return loadDefault(projectRoot).stream()
                .filter(PartitionRecord::playerCreationAllowed)
                .toList();
    }

    public static List<PartitionRecord> npcPool(Path projectRoot) throws IOException {
        return loadDefault(projectRoot).stream()
                .filter(PartitionRecord::npcPoolAllowed)
                .toList();
    }

    public static List<PartitionRecord> nameLockedPool(Path projectRoot) throws IOException {
        return loadDefault(projectRoot).stream()
                .filter(PartitionRecord::nameLockedOnly)
                .toList();
    }

    public static List<PartitionRecord> activePlayerPool(Path projectRoot, AssetRegistry registry) {
        return activeRecords(projectRoot, registry).stream()
                .filter(PartitionRecord::playerCreationAllowed)
                .filter(record -> !record.nameLockedOnly())
                .filter(record -> !record.nonhumanOrRestricted())
                .toList();
    }

    public static List<PartitionRecord> activeNpcPool(Path projectRoot, AssetRegistry registry) {
        return activeRecords(projectRoot, registry).stream()
                .filter(PartitionRecord::npcPoolAllowed)
                .filter(record -> !record.nameLockedOnly())
                .filter(record -> !record.nonhumanOrRestricted())
                .toList();
    }

    public static Optional<String> runtimePlayerAssetId(
            Path projectRoot,
            AssetRegistry registry,
            String portraitSheet,
            int portraitIndex
    ) {
        List<PartitionRecord> ordinary = activePlayerPool(projectRoot, registry);
        List<PartitionRecord> matched = partitionMatches(ordinary, portraitSheet);
        List<PartitionRecord> pool = matched.isEmpty() ? ordinary : matched;
        String identityKey = normalizeSearchText(portraitSheet) + "|" + portraitIndex;
        return deterministicAsset(pool, identityKey);
    }

    public static Optional<String> runtimeNpcAssetId(
            Path projectRoot,
            AssetRegistry registry,
            String persistentIdentityKey
    ) {
        String identity = normalizeSearchText(persistentIdentityKey);
        if (identity.isBlank()) return Optional.empty();

        List<PartitionRecord> active = activeRecords(projectRoot, registry);
        List<PartitionRecord> lockedMatches = bestIdentityMatches(
                active.stream().filter(PartitionRecord::nameLockedOnly).toList(), identity);
        Optional<String> locked = deterministicAsset(lockedMatches, identity);
        if (locked.isPresent()) return locked;

        List<PartitionRecord> ordinary = active.stream()
                .filter(PartitionRecord::npcPoolAllowed)
                .filter(record -> !record.nameLockedOnly())
                .filter(record -> !record.nonhumanOrRestricted())
                .toList();
        List<PartitionRecord> matched = partitionMatches(ordinary, identity);
        return deterministicAsset(matched.isEmpty() ? ordinary : matched, identity);
    }

    public static PartitionAudit audit(Path projectRoot, AssetRegistry registry) throws IOException {
        Path root = normalizedRoot(projectRoot);
        AssetRegistry safeRegistry = registry == null ? AssetRegistry.loadDefault(root) : registry;
        List<PartitionRecord> records = loadDefault(root, safeRegistry);
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
            AssetMetadata asset = metadata.get();
            if (asset.type() != AssetType.PORTRAIT) {
                errors.add("Partition record " + record.assetId()
                        + " is not a PORTRAIT asset: " + asset.type());
            }
            if (!Objects.equals(asset.pathOrUri(), record.registryPath())) {
                errors.add("Partition record " + record.assetId()
                        + " path mismatch registry=" + asset.pathOrUri()
                        + " partition=" + record.registryPath());
            }
            if (record.nameLockedOnly()
                    && (record.playerCreationAllowed() || record.npcPoolAllowed())) {
                errors.add("Name-locked asset " + record.assetId()
                        + " leaked into ordinary player/NPC pools");
            }
            if (record.nonhumanOrRestricted() && record.playerCreationAllowed()) {
                errors.add("Restricted/nonhuman asset " + record.assetId()
                        + " marked player-creation allowed");
            }
        }

        if (records.isEmpty()) errors.add("Portrait/entity partition source is empty");
        if (player <= 0) errors.add("No portrait entries marked as player-creation allowed");
        if (npc <= 0) errors.add("No portrait entries marked as NPC-pool allowed");
        if (locked <= 0) errors.add("No name-locked portrait entries found");
        if (restricted <= 0) errors.add("No restricted/nonhuman portrait entries found");
        if (!partitions.containsKey("name_locked_profile")) errors.add("Missing name_locked_profile partition");
        if (!partitions.containsKey("administratum")) errors.add("Missing administratum/baseline human portrait partition");
        if (!partitions.containsKey("rogue_automata_servitors")) errors.add("Missing servitor/automata portrait partition");
        if (!partitions.containsKey("pets")) errors.add("Missing pets portrait partition");

        return new PartitionAudit(records.size(), partitions.size(), player, npc,
                locked, restricted, List.copyOf(errors));
    }

    public static List<String> infopediaLines(Path projectRoot, String assetId) {
        Optional<PartitionRecord> found = find(projectRoot, assetId);
        if (found.isEmpty()) return List.of();
        PartitionRecord record = found.get();
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Portrait/entity partition: " + record.partitionLabel());
        lines.add("Partition key: " + record.partitionKey());
        lines.add("Player creation pool: " + yesNo(record.playerCreationAllowed()));
        lines.add("Ordinary NPC pool: " + yesNo(record.npcPoolAllowed()));
        lines.add("Name-locked only: " + yesNo(record.nameLockedOnly()));
        lines.add("Non-human/restricted: " + yesNo(record.nonhumanOrRestricted()));
        lines.add("Partition source: " + partitionSource(projectRoot));
        if (!record.notes().isBlank()) lines.add("Partition notes: " + record.notes());
        return lines;
    }

    private static List<PartitionRecord> activeRecords(Path projectRoot, AssetRegistry registry) {
        try {
            Path root = normalizedRoot(projectRoot);
            AssetRegistry safeRegistry = registry == null ? AssetRegistry.loadDefault(root) : registry;
            return loadDefault(root, safeRegistry).stream()
                    .filter(record -> safeRegistry.find(record.assetId())
                            .filter(metadata -> metadata.type() == AssetType.PORTRAIT)
                            .isPresent())
                    .toList();
        } catch (IOException | RuntimeException ex) {
            return List.of();
        }
    }

    private static List<PartitionRecord> synthesizeFromRegistry(AssetRegistry registry) {
        if (registry == null) return List.of();
        ArrayList<PartitionRecord> out = new ArrayList<>();
        for (AssetMetadata asset : registry.byType(AssetType.PORTRAIT)) {
            String haystack = normalizeSearchText(asset.pathOrUri() + " "
                    + asset.name() + " " + asset.semanticDescription());
            String partition = synthesizedPartitionKey(haystack);
            boolean nameLocked = partition.equals("name_locked_profile");
            boolean restricted = synthesizedRestricted(partition, haystack);
            boolean playerAllowed = synthesizedPlayerAllowed(partition, haystack)
                    && !nameLocked && !restricted;
            boolean npcAllowed = !nameLocked;
            out.add(new PartitionRecord(
                    asset.id(), partition, partitionLabel(partition), playerAllowed,
                    npcAllowed, nameLocked, restricted, asset.pathOrUri(),
                    "Synthesized from active semantic registry metadata; authored portrait partition TSV unavailable."
            ));
        }
        return ordered(out);
    }

    private static String synthesizedPartitionKey(String haystack) {
        if (containsAny(haystack, "specialprofiles", "special profiles", "name locked", "name locked profile", "celebrity")) {
            return "name_locked_profile";
        }
        if (containsAny(haystack, "rogue automata", "servitor", "automata", "robot portrait", "machine person")) {
            return "rogue_automata_servitors";
        }
        if (containsAny(haystack, "farm beasts", "farm beast", "livestock", "hog portrait", "goat portrait", "fowl portrait")) {
            return "farm_beasts";
        }
        if (containsAny(haystack, "exotic pets", "swamp creatures", "sump creature", "eel portrait", "leech portrait")) {
            return "exotic_pets_swamp_creatures";
        }
        if (containsAny(haystack, "pets", "pet portrait", "kennel", "mastiff", "hound portrait", "cat portrait", "rat portrait")) {
            return "pets";
        }
        if (containsAny(haystack, "flesh cult", "flesh biotech", "organic cult")) return "flesh_cult";
        if (containsAny(haystack, "undyinglords", "undying lords")) return "undying_lords";
        if (containsAny(haystack, "servants butlers and chefs", "servants butlers chefs", "servant", "butler", "chef portrait", "household staff")) {
            return "servants_butlers_and_chefs";
        }
        if (containsAny(haystack, "schola children", "child portrait", "children portrait")) return "schola_children";
        if (containsAny(haystack, "sisters hospital", "sororitas", "hospital sisters")) return "sisters_hospital";
        if (containsAny(haystack, "hospital", "medical portrait")) return "hospital";
        if (containsAny(haystack, "enforcer arebites", "arbites", "enforcer portrait")) return "enforcer_arebites";
        if (containsAny(haystack, "pdf military", "imperial guard", "military portrait")) return "pdf_military";
        if (containsAny(haystack, "ecclesiarch", "ministorum", "priest portrait", "clerics", "clergy")) return "ecclesiarch";
        if (containsAny(haystack, "mechanicus", "tech priest", "magos portrait")) return "mechanicus";
        if (containsAny(haystack, "genestealer cult", "genestealer")) return "genestealer_cult";
        if (containsAny(haystack, "cultists", "cultist portrait")) return "cultists";
        if (containsAny(haystack, "heretics", "heretic portrait")) return "heretics";
        if (containsAny(haystack, "mutants", "mutant portrait")) return "mutants";
        if (containsAny(haystack, "gangers", "ganger portrait", "gang portrait")) return "gangers";
        if (containsAny(haystack, "nobles", "noble portrait", "noble house")) return "nobles";
        if (containsAny(haystack, "humans8x8", "baseline human", "base human", "player human",
                "human profiles", "administratum", "ordinary human", "clerks", "clerk portrait")) {
            return "administratum";
        }
        return "unclassified_humanoid";
    }

    private static boolean synthesizedPlayerAllowed(String partition, String haystack) {
        if (!partition.equals("administratum")) return false;
        return containsAny(haystack, "humans8x8", "baseline human", "base human", "player human",
                "human profiles", "ordinary human");
    }

    private static boolean synthesizedRestricted(String partition, String haystack) {
        return switch (partition) {
            case "rogue_automata_servitors", "pets", "farm_beasts",
                    "exotic_pets_swamp_creatures", "mutants", "genestealer_cult",
                    "flesh_cult", "undying_lords" -> true;
            default -> containsAny(haystack, "nonhuman", "non human", "restricted portrait", "beast portrait");
        };
    }

    private static String partitionLabel(String partition) {
        String safe = partition == null || partition.isBlank() ? "unclassified_humanoid" : partition;
        String[] words = safe.replace('_', ' ').split(" ");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    private static Optional<Path> firstExistingPartitionIndex(Path root) {
        for (String candidate : PARTITION_INDEX_CANDIDATES) {
            Path path = root.resolve(candidate).normalize();
            if (Files.isRegularFile(path)) return Optional.of(path);
        }
        return Optional.empty();
    }

    private static List<PartitionRecord> partitionMatches(List<PartitionRecord> records, String hint) {
        if (records == null || records.isEmpty()) return List.of();
        List<String> tokens = searchTokens(hint);
        if (tokens.isEmpty()) return List.of();
        return records.stream()
                .filter(record -> tokens.stream().anyMatch(token -> partitionHaystack(record).contains(token)))
                .toList();
    }

    private static List<PartitionRecord> bestIdentityMatches(List<PartitionRecord> records, String identityKey) {
        if (records == null || records.isEmpty()) return List.of();
        List<String> tokens = searchTokens(identityKey);
        if (tokens.isEmpty()) return List.of();
        int bestScore = 0;
        ArrayList<PartitionRecord> matches = new ArrayList<>();
        for (PartitionRecord record : records) {
            String haystack = partitionHaystack(record);
            int score = 0;
            for (String token : tokens) if (haystack.contains(token)) score += Math.max(1, token.length());
            if (score <= 0) continue;
            if (score > bestScore) {
                bestScore = score;
                matches.clear();
            }
            if (score == bestScore) matches.add(record);
        }
        return List.copyOf(matches);
    }

    private static Optional<String> deterministicAsset(List<PartitionRecord> records, String persistentIdentityKey) {
        if (records == null || records.isEmpty()) return Optional.empty();
        List<PartitionRecord> ordered = ordered(records);
        int index = Math.floorMod(Objects.hash(normalizeSearchText(persistentIdentityKey)), ordered.size());
        return Optional.of(ordered.get(index).assetId());
    }

    private static List<PartitionRecord> ordered(List<PartitionRecord> records) {
        return records.stream()
                .sorted(Comparator.comparing(PartitionRecord::partitionKey)
                        .thenComparing(PartitionRecord::assetId))
                .toList();
    }

    private static String partitionHaystack(PartitionRecord record) {
        return normalizeSearchText(record.partitionKey() + " " + record.partitionLabel() + " "
                + record.registryPath() + " " + record.notes());
    }

    private static List<String> searchTokens(String value) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String token : normalizeSearchText(value).split(" ")) {
            if (token.length() < 4 || SEARCH_STOPWORDS.contains(token)) continue;
            tokens.add(token);
        }
        return List.copyOf(tokens);
    }

    private static boolean containsAny(String text, String... alternatives) {
        String normalized = normalizeSearchText(text);
        for (String alternative : alternatives) {
            String needle = normalizeSearchText(alternative);
            if (!needle.isBlank() && normalized.contains(needle)) return true;
        }
        return false;
    }

    private static Path normalizedRoot(Path projectRoot) {
        return projectRoot == null
                ? Path.of(".").toAbsolutePath().normalize()
                : projectRoot.toAbsolutePath().normalize();
    }

    private static String normalizeSearchText(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean parseBoolean(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("true") || normalized.equals("yes")
                || normalized.equals("1") || normalized.equals("y");
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
