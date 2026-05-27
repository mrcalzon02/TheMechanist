import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local utility for renaming generated asset files after docs/ASSET_MANIFEST.md has been produced.
 *
 * Run from the repository root:
 *   javac tools/AssetRenamer.java
 *   java -cp tools AssetRenamer
 *
 * Optional dry run:
 *   java -cp tools AssetRenamer --dry-run
 */
public final class AssetRenamer {
    private static final Path ASSET_ROOT = Paths.get("assets");
    private static final Path MANIFEST = Paths.get("docs", "ASSET_MANIFEST.md");
    private static final Pattern BACKTICK_PATH = Pattern.compile("`([^`]+)`");

    private AssetRenamer() { }

    public static void main(String[] args) throws IOException {
        boolean dryRun = args.length > 0 && "--dry-run".equalsIgnoreCase(args[0]);
        Path assetRoot = ASSET_ROOT.toAbsolutePath().normalize();
        Path manifest = MANIFEST.toAbsolutePath().normalize();

        if (!Files.isDirectory(assetRoot)) throw new IOException("Missing asset root: " + assetRoot);
        if (!Files.isRegularFile(manifest)) throw new IOException("Missing manifest: " + manifest);

        Set<String> manifestPaths = readManifestPaths(manifest);
        int moved = 0;

        for (String relativePath : manifestPaths) {
            String renamedPath = renamePath(relativePath);
            if (renamedPath.equals(relativePath)) continue;

            Path source = resolveInsideAssets(assetRoot, relativePath);
            Path target = resolveInsideAssets(assetRoot, renamedPath);

            if (!Files.isRegularFile(source)) continue;
            if (Files.exists(target)) throw new IOException("Target already exists: " + target);

            System.out.println(source + " -> " + target);
            if (!dryRun) {
                Path parent = target.getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
            moved++;
        }

        System.out.println((dryRun ? "Planned" : "Completed") + " asset renames: " + moved);
        System.out.println("Regenerate docs/ASSET_MANIFEST.md after running this tool.");
    }

    private static Set<String> readManifestPaths(Path manifest) throws IOException {
        Set<String> paths = new LinkedHashSet<>();
        for (String line : Files.readAllLines(manifest, StandardCharsets.UTF_8)) {
            Matcher matcher = BACKTICK_PATH.matcher(line);
            while (matcher.find()) {
                String path = matcher.group(1);
                if (path.contains("Adeptus_Arbites_assets") || path.contains("Automata_servitors")) {
                    paths.add(path);
                }
            }
        }
        return paths;
    }

    private static String renamePath(String path) {
        return path
                .replace("Adeptus_Arbites_assets", "Enforcer_Assets")
                .replace("Automata_servitors", "Automata_Drones");
    }

    private static Path resolveInsideAssets(Path assetRoot, String manifestPath) throws IOException {
        String relative = manifestPath.replace('\\', '/');
        if (relative.startsWith("assets/")) relative = relative.substring("assets/".length());
        if (relative.startsWith("/") || relative.contains("..")) {
            throw new IOException("Unsafe manifest path: " + manifestPath);
        }
        Path resolved = assetRoot.resolve(relative).normalize();
        if (!resolved.startsWith(assetRoot)) throw new IOException("Path escapes asset root: " + manifestPath);
        return resolved;
    }
}
