package mechanist;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Asset-pack and tile-art path authority.
 *
 * This class owns package-root/path/quality-tier resolution only.
 *
 * It must not:
 * - cache BufferedImage instances,
 * - bind glyphs,
 * - know about TileImageRegistry,
 * - know about renderer paint logic.
 */
final class ArtPackManager {
    static final String DEFAULT_QUALITY_FOLDER = "low_32";

    private static final String[] QUALITY_FOLDERS = {
            "low_32",
            "standard_64",
            "intermediate_128",
            "high_native"
    };

    private ArtPackManager() {
    }

    static String prepareAndResolveRoot(String packDirName, String cacheDirName, String bundledFallbackRoot) {
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();

        addCandidate(candidates, resolveBundledFallbackRoot(bundledFallbackRoot));
        addCandidate(candidates, bundledFallbackRoot);
        addCandidate(candidates, "assets/a/r");
        addCandidate(candidates, "PACKAGE_client/assets/a/r");
        addCandidate(candidates, "client/assets/a/r");
        addCandidate(candidates, "packages/client/assets/a/r");

        Path cacheArtRoot = findArtRoot(Paths.get(nullToBlank(cacheDirName)));
        if (cacheArtRoot != null) candidates.add(cacheArtRoot);

        Path packArtRoot = findArtRoot(Paths.get(nullToBlank(packDirName)));
        if (packArtRoot != null) candidates.add(packArtRoot);

        for (Path candidate : expandRuntimeCandidates(candidates)) {
            if (isUsableArtRoot(candidate)) {
                String root = candidate.toAbsolutePath().normalize().toString();
                DebugLog.audit("ART_PACK", "Using art root=" + root);
                return root;
            }
        }

        String fallbackRoot = resolveInstalledArtRoot(resolveBundledFallbackRoot(bundledFallbackRoot));
        DebugLog.warn("ART_PACK", "No verified art root found; returning fallback root=" + fallbackRoot
                + " runtime=" + RuntimePathResolver.workingDirectorySummary());
        return fallbackRoot;
    }

    static String resolveBundledFallbackRoot(String bundledFallbackRoot) {
        if (bundledFallbackRoot == null || bundledFallbackRoot.isBlank()) {
            return RuntimePathResolver.resolveAssetFile("assets/a/r").getPath();
        }

        ArrayList<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get(bundledFallbackRoot));
        candidates.add(RuntimePathResolver.resolveAssetFile(bundledFallbackRoot).toPath());

        String normalized = normalizeSlashes(bundledFallbackRoot);
        if (normalized.startsWith("packages/client/")) {
            String stripped = normalized.substring("packages/client/".length());
            candidates.add(RuntimePathResolver.resolveAssetFile(stripped).toPath());
        }
        if (normalized.startsWith("client/")) {
            String stripped = normalized.substring("client/".length());
            candidates.add(RuntimePathResolver.resolveAssetFile(stripped).toPath());
        }
        if (normalized.startsWith("PACKAGE_client/")) {
            String stripped = normalized.substring("PACKAGE_client/".length());
            candidates.add(RuntimePathResolver.resolveAssetFile(stripped).toPath());
        }

        candidates.add(RuntimePathResolver.resolveAssetFile("assets/a/r").toPath());
        candidates.add(RuntimePathResolver.resolveAssetFile("PACKAGE_client/assets/a/r").toPath());
        candidates.add(RuntimePathResolver.resolveAssetFile("client/assets/a/r").toPath());
        candidates.add(RuntimePathResolver.resolveAssetFile("packages/client/assets/a/r").toPath());

        for (Path candidate : candidates) {
            if (isUsableArtRoot(candidate)) {
                return candidate.toAbsolutePath().normalize().toString();
            }
        }

        File resolved = RuntimePathResolver.resolveAssetFile(bundledFallbackRoot);
        return resolved.getPath();
    }

    static String resolveInstalledArtRoot(String requestedRoot) {
        ArrayList<Path> candidates = new ArrayList<>();

        if (requestedRoot != null && !requestedRoot.isBlank()) {
            candidates.add(Paths.get(requestedRoot));
            candidates.add(RuntimePathResolver.resolveAssetFile(requestedRoot).toPath());
        }

        candidates.add(RuntimePathResolver.resolveAssetFile("assets/a/r").toPath());
        candidates.add(RuntimePathResolver.resolveAssetFile("PACKAGE_client/assets/a/r").toPath());
        candidates.add(RuntimePathResolver.resolveAssetFile("client/assets/a/r").toPath());
        candidates.add(RuntimePathResolver.resolveAssetFile("packages/client/assets/a/r").toPath());
        candidates.add(Paths.get("assets/a/r"));
        candidates.add(Paths.get("PACKAGE_client/assets/a/r"));
        candidates.add(Paths.get("client/assets/a/r"));
        candidates.add(Paths.get("packages/client/assets/a/r"));

        for (Path candidate : candidates) {
            if (isUsableArtRoot(candidate)) {
                return candidate.toAbsolutePath().normalize().toString();
            }
        }

        return requestedRoot == null ? RuntimePathResolver.resolveAssetFile("assets/a/r").getPath() : requestedRoot;
    }

    static Path resolveQualityRoot(String bundledRoot, String folder) {
        String wanted = normalizeQualityFolder(folder);
        String resolvedBundledRoot = resolveInstalledArtRoot(bundledRoot);

        Path local = Paths.get(resolvedBundledRoot, "tiles", "quality", wanted);
        if (Files.isDirectory(local.resolve("cells"))) {
            return local;
        }

        Path external = findQualityRoot(Paths.get("cache", "artpacks"), wanted);
        if (external != null) {
            return external;
        }

        Path bundledLow = Paths.get(resolvedBundledRoot, "tiles", "quality", DEFAULT_QUALITY_FOLDER);
        if (Files.isDirectory(bundledLow.resolve("cells"))) {
            if (!DEFAULT_QUALITY_FOLDER.equals(wanted)) {
                DebugLog.warn("ART_PACK", "Requested art tier '" + wanted
                        + "' is not installed; falling back to bundled " + DEFAULT_QUALITY_FOLDER + ".");
            }
            return bundledLow;
        }

        DebugLog.warn("ART_PACK", "No usable art quality root found. requestedTier=" + wanted
                + " artRoot=" + resolvedBundledRoot
                + " expectedCells=" + local.resolve("cells")
                + " runtime=" + RuntimePathResolver.workingDirectorySummary());
        return local;
    }

    static String resolveQualityCellsRoot(String bundledRoot, String folder) {
        Path qualityRoot = resolveQualityRoot(bundledRoot, folder);
        Path cells = qualityRoot.resolve("cells");
        if (Files.isDirectory(cells)) {
            return cells.toString();
        }

        String resolvedBundledRoot = resolveInstalledArtRoot(bundledRoot);
        Path legacyCells = Paths.get(resolvedBundledRoot, "tiles", "cells");
        return legacyCells.toString();
    }

    static boolean isSupportedQualityFolder(String folder) {
        String normalized = normalizeQualityFolder(folder);
        for (String tier : QUALITY_FOLDERS) {
            if (tier.equals(normalized)) return true;
        }
        return false;
    }

    static String normalizeQualityFolder(String folder) {
        if (folder == null || folder.isBlank()) return DEFAULT_QUALITY_FOLDER;

        String f = folder.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return switch (f) {
            case "low", "low32", "32", "default_32" -> "low_32";
            case "standard", "standard64", "64", "default_64" -> "standard_64";
            case "intermediate", "intermediate128", "128", "default_128" -> "intermediate_128";
            case "native", "high", "highnative", "high_native", "source_native" -> "high_native";
            default -> f;
        };
    }

    static int countPngFiles(Path root) {
        if (root == null || !Files.isDirectory(root)) return 0;

        try (Stream<Path> stream = Files.walk(root)) {
            return (int) stream
                    .filter(Files::isRegularFile)
                    .filter(ArtPackManager::isPng)
                    .count();
        } catch (IOException ex) {
            DebugLog.warn("ART_PACK", "Failed to count PNG files under " + root + ": " + ex.getMessage());
            return 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    static boolean hasQualityCells(String artRoot, String folder) {
        if (artRoot == null || artRoot.isBlank()) return false;
        Path root = Paths.get(resolveInstalledArtRoot(artRoot));
        Path cells = root.resolve("tiles").resolve("quality").resolve(normalizeQualityFolder(folder)).resolve("cells");
        return Files.isDirectory(cells);
    }

    static String auditQualityRoots(String artRoot) {
        String resolved = resolveInstalledArtRoot(artRoot);
        StringBuilder sb = new StringBuilder();
        sb.append("ArtPackManager{root=").append(resolved);

        for (String tier : QUALITY_FOLDERS) {
            Path cells = Paths.get(resolved, "tiles", "quality", tier, "cells");
            sb.append(", ")
                    .append(tier)
                    .append("=")
                    .append(Files.isDirectory(cells))
                    .append(":pngs=")
                    .append(countPngFiles(cells));
        }

        sb.append('}');
        return sb.toString();
    }

    private static Path findArtRoot(Path searchRoot) {
        if (searchRoot == null) return null;

        Path resolvedSearchRoot = RuntimePathResolver.resolveAssetFile(searchRoot.toString()).toPath();
        if (isUsableArtRoot(resolvedSearchRoot)) return resolvedSearchRoot;

        if (!Files.isDirectory(resolvedSearchRoot)) return null;

        try (Stream<Path> stream = Files.walk(resolvedSearchRoot, 5)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(ArtPackManager::isUsableArtRoot)
                    .findFirst()
                    .orElse(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Path findQualityRoot(Path searchRoot, String wantedFolder) {
        if (searchRoot == null) return null;
        String wanted = normalizeQualityFolder(wantedFolder);

        Path resolvedSearchRoot = RuntimePathResolver.resolveAssetFile(searchRoot.toString()).toPath();
        if (!Files.isDirectory(resolvedSearchRoot)) return null;

        try (Stream<Path> stream = Files.walk(resolvedSearchRoot, 7)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(path -> wanted.equalsIgnoreCase(path.getFileName() == null ? "" : path.getFileName().toString()))
                    .filter(path -> Files.isDirectory(path.resolve("cells")))
                    .findFirst()
                    .orElse(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Set<Path> expandRuntimeCandidates(Set<Path> incoming) {
        LinkedHashSet<Path> out = new LinkedHashSet<>();

        for (Path candidate : incoming) {
            if (candidate == null) continue;

            out.add(candidate);
            out.add(candidate.toAbsolutePath().normalize());

            String raw = normalizeSlashes(candidate.toString());
            if (!raw.isBlank()) {
                out.add(RuntimePathResolver.resolveAssetFile(raw).toPath());

                if (raw.startsWith("assets/")) {
                    out.add(RuntimePathResolver.resolveAssetFile("PACKAGE_client/" + raw).toPath());
                    out.add(RuntimePathResolver.resolveAssetFile("client/" + raw).toPath());
                    out.add(RuntimePathResolver.resolveAssetFile("packages/client/" + raw).toPath());
                }

                if (raw.startsWith("packages/client/")) {
                    out.add(RuntimePathResolver.resolveAssetFile(raw.substring("packages/client/".length())).toPath());
                }

                if (raw.startsWith("PACKAGE_client/")) {
                    out.add(RuntimePathResolver.resolveAssetFile(raw.substring("PACKAGE_client/".length())).toPath());
                }

                if (raw.startsWith("client/")) {
                    out.add(RuntimePathResolver.resolveAssetFile(raw.substring("client/".length())).toPath());
                }
            }
        }

        return out;
    }

    private static void addCandidate(Set<Path> candidates, String path) {
        if (path == null || path.isBlank()) return;
        candidates.add(Paths.get(path));
    }

    private static boolean isUsableArtRoot(Path root) {
        if (root == null) return false;

        Path normalized = root.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized)) return false;

        Path lowCells = normalized
                .resolve("tiles")
                .resolve("quality")
                .resolve(DEFAULT_QUALITY_FOLDER)
                .resolve("cells");

        if (Files.isDirectory(lowCells)) return true;

        for (String tier : QUALITY_FOLDERS) {
            Path tierCells = normalized
                    .resolve("tiles")
                    .resolve("quality")
                    .resolve(tier)
                    .resolve("cells");
            if (Files.isDirectory(tierCells)) return true;
        }

        return Files.isDirectory(normalized.resolve("tiles").resolve("cells"));
    }

    private static boolean isPng(Path path) {
        if (path == null || path.getFileName() == null) return false;
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png");
    }

    private static String normalizeSlashes(String raw) {
        if (raw == null) return "";
        return raw.replace('\\', '/').replaceAll("^/+", "");
    }

    private static String nullToBlank(String raw) {
        return Objects.toString(raw, "");
    }
}