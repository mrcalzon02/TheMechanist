package mechanist.assets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Lean resolver for semantic asset references.
 *
 * Client code asks for a semantic reference plus a preferred visual tier and gets
 * a filesystem path suitable for image loading. Server/headless code uses the
 * same registry validation without loading graphical textures into memory.
 */
public final class SemanticAssetResolver {
    private final AssetRegistry registry;
    private final GeneratedAssetRuntime generatedRuntime;
    private final boolean graphicalLoadingAllowed;

    private SemanticAssetResolver(AssetRegistry registry, GeneratedAssetRuntime generatedRuntime, boolean graphicalLoadingAllowed) {
        this.registry = registry == null ? AssetRegistry.empty() : registry;
        this.generatedRuntime = generatedRuntime == null ? GeneratedAssetRuntime.loadDefault(this.registry.projectRoot()) : generatedRuntime;
        this.graphicalLoadingAllowed = graphicalLoadingAllowed;
    }

    public static SemanticAssetResolver forClient(Path packageRoot) {
        Path root = packageRoot == null ? Path.of(System.getProperty("mechanist.assetRoot", ".")) : packageRoot;
        try {
            AssetRegistry registry = AssetRegistry.loadDefault(root);
            return new SemanticAssetResolver(registry, GeneratedAssetRuntime.loadDefault(registry.projectRoot()), true);
        } catch (Exception ignored) {
            return new SemanticAssetResolver(AssetRegistry.empty(), GeneratedAssetRuntime.loadDefault(root), true);
        }
    }

    public static SemanticAssetResolver forServer(Path packageRoot) {
        Path root = packageRoot == null ? Path.of(System.getProperty("mechanist.assetRoot", ".")) : packageRoot;
        try {
            AssetRegistry registry = AssetRegistry.loadDefault(root);
            return new SemanticAssetResolver(registry, GeneratedAssetRuntime.loadDefault(registry.projectRoot()), false);
        } catch (Exception ignored) {
            return new SemanticAssetResolver(AssetRegistry.empty(), GeneratedAssetRuntime.loadDefault(root), false);
        }
    }

    public static SemanticAssetResolver fromInstalledRuntime(boolean graphicalLoadingAllowed) {
        Path root = Path.of(System.getProperty("mechanist.assetRoot", ".")).toAbsolutePath().normalize();
        return graphicalLoadingAllowed ? forClient(root) : forServer(root);
    }

    public boolean graphicalLoadingAllowed() {
        return graphicalLoadingAllowed;
    }

    public int registrySize() {
        return registry.size();
    }

    public boolean isKnown(String rawReference) {
        try {
            return registry.find(SemanticAssetReference.of(rawReference).id()).isPresent();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public AssetMetadata requireMetadata(String rawReference) {
        SemanticAssetReference reference = SemanticAssetReference.of(rawReference);
        return registry.find(reference.id()).orElseThrow(() -> new NoSuchElementException("Unknown semantic asset reference: " + reference.id()));
    }

    public Optional<AssetMetadata> metadata(String rawReference) {
        try {
            return registry.find(SemanticAssetReference.of(rawReference).id());
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public Optional<Path> resolveExistingPath(String rawReference, AssetQualityTier preferredTier) {
        Optional<AssetMetadata> metadata = metadata(rawReference);
        if (metadata.isEmpty()) return Optional.empty();
        Path path = resolvePath(metadata.get(), preferredTier == null ? generatedRuntime.defaultTier() : preferredTier);
        return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
    }

    public Path requireExistingPath(String rawReference, AssetQualityTier preferredTier) {
        return resolveExistingPath(rawReference, preferredTier)
                .orElseThrow(() -> new NoSuchElementException("Semantic asset has no installed payload for reference: " + rawReference));
    }

    public boolean validateReferenceForServer(String rawReference) {
        return metadata(rawReference).isPresent();
    }

    public Path resolvePath(AssetMetadata metadata, AssetQualityTier preferredTier) {
        if (metadata == null) throw new IllegalArgumentException("metadata cannot be null");
        String ref = metadata.pathOrUri();
        if (GeneratedAssetRuntime.isGeneratedPath(ref)) {
            AssetQualityTier tier = preferredTier == null ? generatedRuntime.defaultTier() : preferredTier;
            String tierPath = generatedRuntime.rewriteTier(ref, tier);
            Optional<Path> tierResolved = generatedRuntime.resolveExistingGeneratedPath(tierPath);
            if (tierResolved.isPresent()) return tierResolved.orElseThrow();
            return generatedRuntime.resolvePath(tierPath);
        }
        return registry.resolvePath(metadata);
    }

    public String auditSummary() {
        return "semanticAssetResolver{registrySize=" + registry.size()
                + ", graphicalLoadingAllowed=" + graphicalLoadingAllowed
                + ", defaultTier=" + generatedRuntime.defaultTier().directoryName()
                + ", runtimeManifestPresent=" + generatedRuntime.runtimeManifestPresent()
                + ", tierManifestPresent=" + generatedRuntime.tierManifestPresent()
                + ", manifestAssetCount=" + generatedRuntime.manifestAssetCount()
                + '}';
    }

    public static AssetQualityTier tierFromClientRequest(String requestedTier) {
        if (requestedTier == null || requestedTier.isBlank()) return AssetQualityTier.LOW_32;
        String normalized = requestedTier.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return AssetQualityTier.fromToken(normalized);
    }
}
