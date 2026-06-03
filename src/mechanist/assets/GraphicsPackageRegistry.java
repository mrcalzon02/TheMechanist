package mechanist.assets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Discovers installed client graphics packages.
 *
 * Packages are intentionally discovered from real package locations rather than loose
 * manifest references. A package may be an unzipped folder or a compressed .zip bundle.
 */
public final class GraphicsPackageRegistry {
    public static final String DEFAULT_PACKAGE_ID = "default_32";
    public static final String PACKAGE_ROOT_PROPERTY = "mechanist.graphics.packagesRoot";
    public static final Path DEFAULT_PACKAGE_ROOT = Path.of("assets", "graphics", "packages");

    private GraphicsPackageRegistry() {}

    public static Path packageRoot() {
        var configured = System.getProperty(PACKAGE_ROOT_PROPERTY, "").trim();
        return configured.isEmpty() ? DEFAULT_PACKAGE_ROOT : Path.of(configured);
    }

    public static List<GraphicsPackage> discoverInstalledPackages() {
        var root = packageRoot();
        var packages = new ArrayList<GraphicsPackage>();
        if (Files.isDirectory(root)) {
            try (var stream = Files.list(root)) {
                stream.sorted().forEach(path -> discover(path, packages));
            } catch (IOException ignored) {
                // Fall back below to the mandatory default package descriptor.
            }
        }
        ensureDefaultPackage(packages, root);
        packages.sort(Comparator.comparingInt(GraphicsPackage::nominalSizePx).thenComparing(GraphicsPackage::id));
        return List.copyOf(packages);
    }

    public static GraphicsPackage selectedOrDefault(String selectedId) {
        var packages = discoverInstalledPackages();
        if (selectedId != null && !selectedId.isBlank()) {
            for (var pack : packages) {
                if (pack.id().equals(selectedId)) {
                    return pack;
                }
            }
        }
        for (var pack : packages) {
            if (pack.id().equals(DEFAULT_PACKAGE_ID)) {
                return pack;
            }
        }
        return packages.get(0);
    }

    private static void discover(Path path, List<GraphicsPackage> packages) {
        var name = path.getFileName().toString();
        if (Files.isDirectory(path)) {
            packages.add(new GraphicsPackage(idFromFolder(name), displayNameFrom(name), sizeFromName(name), path, false, true));
            return;
        }
        if (Files.isRegularFile(path) && name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            var base = name.substring(0, name.length() - 4);
            packages.add(new GraphicsPackage(idFromFolder(base), displayNameFrom(base), sizeFromName(base), path, true, false));
        }
    }

    private static void ensureDefaultPackage(List<GraphicsPackage> packages, Path root) {
        for (var pack : packages) {
            if (pack.id().equals(DEFAULT_PACKAGE_ID)) {
                return;
            }
        }
        packages.add(new GraphicsPackage(DEFAULT_PACKAGE_ID, "Default 32px", 32, root.resolve(DEFAULT_PACKAGE_ID), false, Files.isDirectory(root.resolve(DEFAULT_PACKAGE_ID))));
    }

    private static String idFromFolder(String name) {
        var normalized = name.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return normalized.isEmpty() ? DEFAULT_PACKAGE_ID : normalized;
    }

    private static String displayNameFrom(String name) {
        var normalized = name.replace('_', ' ').replace('-', ' ').trim();
        if (normalized.isEmpty()) {
            return "Default 32px";
        }
        var words = normalized.split("\\s+");
        var result = new StringBuilder();
        for (var word : words) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private static int sizeFromName(String name) {
        var lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("256")) return 256;
        if (lower.contains("128")) return 128;
        if (lower.contains("64")) return 64;
        if (lower.contains("32")) return 32;
        return 32;
    }
}
