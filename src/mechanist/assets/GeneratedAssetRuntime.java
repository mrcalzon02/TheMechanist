package mechanist.assets;

import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runtime bridge for the generated source-first art tree introduced during the
 * 0.9.10kc graphics rebase.
 *
 * The semantic registry may now point at assets/graphics/generated/<tier>/...
 * while the game keeps choosing the safest available tier at runtime.  The
 * bridge deliberately falls back to the exact registry path if a preferred tier
 * is absent, which lets the project zip carry only low_32 until the full art
 * payload is installed beside it. External payload roots can be mounted with
 * -Dmechanist.generatedAssetRoot=/path/to/payload, including either canonical
 * assets/graphics/generated trees or onboarding exports/<tier> trees.
 */
public final class GeneratedAssetRuntime {
    public static final String RUNTIME_MANIFEST = "assets/indexes/runtime_asset_manifest.json";
    public static final String TIER_MANIFEST = "assets/indexes/tier_path_manifest.json";
    public static final String GENERATED_PAYLOAD_ROOT_PROPERTY = "mechanist.generatedAssetRoot";
    public static final String LEGACY_PAYLOAD_ROOT_PROPERTY = "mechanist.assetPayloadRoot";

    private static final Pattern GENERATED_PATH = Pattern.compile("^assets/graphics/generated/([^/]+)/(.+)$");
    private static final Pattern DEFAULT_TIER = Pattern.compile("\\\"default_tier\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern PREFERRED_ORDER = Pattern.compile("\\\"preferred_runtime_tier_order\\\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern QUOTED = Pattern.compile("\\\"([^\\\"]+)\\\"");
    private static final Pattern ASSET_COUNT = Pattern.compile("\\\"asset_count\\\"\\s*:\\s*(\\d+)");

    private final Path projectRoot;
    private final List<Path> generatedPayloadRoots;
    private final boolean runtimeManifestPresent;
    private final boolean tierManifestPresent;
    private final int manifestAssetCount;
    private final AssetQualityTier defaultTier;
    private final List<AssetQualityTier> preferredRuntimeOrder;

    private GeneratedAssetRuntime(
            Path projectRoot,
            List<Path> generatedPayloadRoots,
            boolean runtimeManifestPresent,
            boolean tierManifestPresent,
            int manifestAssetCount,
            AssetQualityTier defaultTier,
            List<AssetQualityTier> preferredRuntimeOrder
    ) {
        this.projectRoot = projectRoot == null ? Paths.get("").toAbsolutePath().normalize() : projectRoot.toAbsolutePath().normalize();
        this.generatedPayloadRoots = List.copyOf(generatedPayloadRoots == null || generatedPayloadRoots.isEmpty()
                ? List.of(this.projectRoot)
                : generatedPayloadRoots);
        this.runtimeManifestPresent = runtimeManifestPresent;
        this.tierManifestPresent = tierManifestPresent;
        this.manifestAssetCount = Math.max(0, manifestAssetCount);
        this.defaultTier = defaultTier == null ? AssetQualityTier.LOW_32 : defaultTier;
        this.preferredRuntimeOrder = List.copyOf(preferredRuntimeOrder == null || preferredRuntimeOrder.isEmpty()
                ? List.of(AssetQualityTier.LOW_32, AssetQualityTier.STANDARD_64, AssetQualityTier.INTERMEDIATE_128, AssetQualityTier.HIGH_NATIVE)
                : preferredRuntimeOrder);
    }

    public static GeneratedAssetRuntime loadDefault(Path projectRoot) {
        Path root = projectRoot == null ? Paths.get("").toAbsolutePath().normalize() : projectRoot.toAbsolutePath().normalize();
        List<Path> payloadRoots = discoverPayloadRoots(root);
        Optional<Path> runtimeManifest = firstRegularFile(payloadRoots, RUNTIME_MANIFEST, "manifests/runtime_asset_manifest.json");
        Optional<Path> tierManifest = firstRegularFile(payloadRoots, TIER_MANIFEST, "manifests/tier_path_manifest.json");
        boolean runtimePresent = runtimeManifest.isPresent();
        boolean tierPresent = tierManifest.isPresent();
        int assetCount = runtimePresent ? readAssetCount(runtimeManifest.orElseThrow()) : 0;
        AssetQualityTier defaultTier = AssetQualityTier.LOW_32;
        List<AssetQualityTier> order = new ArrayList<>(List.of(AssetQualityTier.LOW_32, AssetQualityTier.STANDARD_64, AssetQualityTier.INTERMEDIATE_128, AssetQualityTier.HIGH_NATIVE));
        if (tierPresent) {
            String text = readTextQuietly(tierManifest.orElseThrow());
            Matcher d = DEFAULT_TIER.matcher(text);
            if (d.find()) defaultTier = AssetQualityTier.fromToken(d.group(1));
            Matcher list = PREFERRED_ORDER.matcher(text);
            if (list.find()) {
                ArrayList<AssetQualityTier> parsed = new ArrayList<>();
                Matcher q = QUOTED.matcher(list.group(1));
                while (q.find()) parsed.add(AssetQualityTier.fromToken(q.group(1)));
                if (!parsed.isEmpty()) order = parsed;
            }
        }
        return new GeneratedAssetRuntime(root, payloadRoots, runtimePresent, tierPresent, assetCount, defaultTier, order);
    }

    public static boolean isGeneratedPath(String value) {
        if (value == null) return false;
        return GENERATED_PATH.matcher(value.trim().replace('\\', '/')).matches();
    }

    public Path projectRoot() {
        return projectRoot;
    }

    /**
     * Roots searched for generated payload files. The first entries may be
     * external art-pack mounts, while the project root remains the final
     * fallback so the bundled low_32 payload keeps working.
     */
    public List<Path> generatedPayloadRoots() {
        return generatedPayloadRoots;
    }

    public boolean runtimeManifestPresent() {
        return runtimeManifestPresent;
    }

    public boolean tierManifestPresent() {
        return tierManifestPresent;
    }

    public int manifestAssetCount() {
        return manifestAssetCount;
    }

    public AssetQualityTier defaultTier() {
        return defaultTier;
    }

    public List<AssetQualityTier> preferredRuntimeOrder() {
        return preferredRuntimeOrder;
    }

    /**
     * Resolve a registry path. Generated paths are tier-adjusted and existence
     * checked; non-generated paths are simply resolved under the project root.
     */
    public Path resolvePath(String pathOrUri) {
        String normalized = normalizeRelativePath(pathOrUri);
        if (!isGeneratedPath(normalized)) {
            return resolvePlainPath(normalized);
        }
        for (AssetQualityTier tier : candidateTiers(normalized)) {
            Optional<Path> candidate = resolveExistingGeneratedPathForTier(normalized, tier);
            if (candidate.isPresent()) {
                return candidate.orElseThrow();
            }
        }
        return projectRoot.resolve(normalized).normalize();
    }

    public Optional<Path> resolveExistingGeneratedPath(String pathOrUri) {
        if (!isGeneratedPath(pathOrUri)) return Optional.empty();
        Path resolved = resolvePath(pathOrUri);
        return Files.isRegularFile(resolved) ? Optional.of(resolved) : Optional.empty();
    }

    public String rewriteTier(String pathOrUri, AssetQualityTier tier) {
        String normalized = normalizeRelativePath(pathOrUri);
        Matcher m = GENERATED_PATH.matcher(normalized);
        if (!m.matches()) return normalized;
        AssetQualityTier safeTier = tier == null ? defaultTier : tier;
        return GeneratedAssetPathResolver.GENERATED_ROOT + "/" + safeTier.directoryName() + "/" + m.group(2);
    }

    public List<AssetQualityTier> candidateTiers(String pathOrUri) {
        String normalized = normalizeRelativePath(pathOrUri);
        LinkedHashSet<AssetQualityTier> out = new LinkedHashSet<>();
        String explicit = System.getProperty("mechanist.assetTier", System.getProperty("mechanist.graphicsTier", ""));
        if (explicit != null && !explicit.isBlank()) out.add(AssetQualityTier.fromToken(explicit));
        out.add(defaultTier);
        Matcher m = GENERATED_PATH.matcher(normalized);
        if (m.matches()) out.add(AssetQualityTier.fromToken(m.group(1)));
        out.addAll(preferredRuntimeOrder);
        out.add(AssetQualityTier.LOW_32);
        out.add(AssetQualityTier.STANDARD_64);
        out.add(AssetQualityTier.INTERMEDIATE_128);
        out.add(AssetQualityTier.HIGH_NATIVE);
        return List.copyOf(out);
    }


    private Optional<Path> resolveExistingGeneratedPathForTier(String normalizedGeneratedPath, AssetQualityTier tier) {
        String canonical = rewriteTier(normalizedGeneratedPath, tier);
        Matcher m = GENERATED_PATH.matcher(canonical);
        if (!m.matches()) return Optional.empty();
        String tierName = m.group(1);
        String relativeAfterTier = m.group(2);
        List<String> candidateRelativePaths = List.of(
                canonical,
                "exports/" + tierName + "/" + relativeAfterTier,
                tierName + "/" + relativeAfterTier
        );
        for (Path root : generatedPayloadRoots) {
            for (String relative : candidateRelativePaths) {
                Path candidate = root.resolve(relative).normalize();
                if (Files.isRegularFile(candidate)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    private static List<Path> discoverPayloadRoots(Path projectRoot) {
        LinkedHashSet<Path> roots = new LinkedHashSet<>();
        addPayloadRootsFromProperty(roots, System.getProperty(GENERATED_PAYLOAD_ROOT_PROPERTY));
        addPayloadRootsFromProperty(roots, System.getProperty(LEGACY_PAYLOAD_ROOT_PROPERTY));
        Path root = projectRoot == null ? Paths.get("").toAbsolutePath().normalize() : projectRoot.toAbsolutePath().normalize();
        roots.add(root);
        return List.copyOf(roots);
    }

    private static void addPayloadRootsFromProperty(LinkedHashSet<Path> out, String raw) {
        if (raw == null || raw.isBlank()) return;
        String[] parts = raw.contains(";") ? raw.split(";") : raw.split(Pattern.quote(File.pathSeparator));
        for (String part : parts) {
            if (part == null || part.isBlank()) continue;
            try {
                out.add(Paths.get(part.trim()).toAbsolutePath().normalize());
            } catch (InvalidPathException ignored) {
                // A bad optional payload root must not prevent the bundled low_32
                // project art from loading. The validator smoke reports bad roots.
            }
        }
    }

    private static Optional<Path> firstRegularFile(List<Path> roots, String... relativePaths) {
        for (Path root : roots) {
            for (String relative : relativePaths) {
                Path candidate = root.resolve(relative).normalize();
                if (Files.isRegularFile(candidate)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    private Path resolvePlainPath(String value) {
        Objects.requireNonNull(value, "path value cannot be null");
        try {
            Path p = Paths.get(value);
            if (!p.isAbsolute()) p = projectRoot.resolve(p);
            return p.toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            throw new IllegalArgumentException("Invalid asset path: " + value, ex);
        }
    }

    private static String normalizeRelativePath(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Asset path cannot be blank");
        return value.trim().replace('\\', '/');
    }

    private static int readAssetCount(Path manifest) {
        String text = readTextQuietly(manifest);
        Matcher m = ASSET_COUNT.matcher(text);
        if (!m.find()) return 0;
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String readTextQuietly(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return "";
        }
    }
}
