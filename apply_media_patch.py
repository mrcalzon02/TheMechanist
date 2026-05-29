import os
import re

file_path = "src/mechanist/MediaRuntimeFramework.java"

if not os.path.exists(file_path):
    print(f"[-] Error: Could not find {file_path}")
    exit(1)

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add Explicit Locale Import just in case
if "import java.util.Locale;" not in content:
    content = content.replace("import java.util.*;", "import java.util.*;\nimport java.util.Locale;")

# 2. Patch the load method to resolve the root path and add telemetry
old_load_pattern = r"(void load\(String rootPath, int qualityIndex\) \{.*?ByGlyph\.clear\(\);.*?byAlias\.clear\(\);.*?bySemantic\.clear\(\);.*?)(File root = new File\(rootPath\);)(.*?)(loadedQualityFolder = qualityFolderFor\(qualityIndex\);.*?String cellRoot = qualityCellRoot\(rootPath, loadedQualityFolder\);)"

# Target the specific string replacements cleanly
if "ArtPackManager.resolveInstalledArtRoot" not in content:
    content = content.replace(
        "File root = new File(rootPath);\n        if (!root.exists()) return;",
        'String resolvedRootPath = ArtPackManager.resolveInstalledArtRoot(rootPath);\n        File root = new File(resolvedRootPath);\n        if (!root.exists()) {\n            DebugLog.warn("TILE_ART", "Tile art root does not exist. requestedRoot=" + rootPath + " resolvedRoot=" + resolvedRootPath + " runtime=" + RuntimePathResolver.workingDirectorySummary());\n            return;\n        }'
    )
    content = content.replace(
        "String cellRoot = qualityCellRoot(rootPath, loadedQualityFolder);",
        'String cellRoot = qualityCellRoot(resolvedRootPath, loadedQualityFolder);\n        int cellPngCount = ArtPackManager.countPngFiles(Paths.get(cellRoot));\n        DebugLog.audit("TILE_ART", "tile root requested=" + rootPath + " resolved=" + resolvedRootPath + " quality=" + loadedQualityFolder + " cellRoot=" + cellRoot + " cellRootExists=" + Files.isDirectory(Paths.get(cellRoot)) + " pngCount=" + cellPngCount);'
    )
    content = content.replace(
        "loadSemanticIcons(rootPath, loadedQualityFolder);",
        "loadSemanticIcons(resolvedRootPath, loadedQualityFolder);"
    )

# 3. Inject RuntimePathResolver inside resolveRuntimeCellFile
if "RuntimePathResolver.resolveAssetFile(p);" not in content:
    content = content.replace(
        "String p = path.replace('\\\\', '/');",
        "String p = path.replace('\\\\', '/');\n        File resolved = RuntimePathResolver.resolveAssetFile(p);\n        if (resolved.exists()) return resolved;"
    )

# 4. Inject ArtPackManager helper methods if they don't exist
if "static String resolveInstalledArtRoot" not in content:
    art_pack_helpers = """class ArtPackManager {
    static String resolveInstalledArtRoot(String requestedRoot) {
        ArrayList<Path> candidates = new ArrayList<>();
        if (requestedRoot != null && !requestedRoot.isBlank()) {
            candidates.add(Paths.get(requestedRoot));
            File runtimeResolved = RuntimePathResolver.resolveAssetFile(requestedRoot);
            if (runtimeResolved != null) candidates.add(runtimeResolved.toPath());
        }
        candidates.add(RuntimePathResolver.resolveAssetFile("assets/a/r").toPath());
        candidates.add(RuntimePathResolver.resolveAssetFile("PACKAGE_client/assets/a/r").toPath());
        candidates.add(RuntimePathResolver.resolveAssetFile("client/assets/a/r").toPath());
        candidates.add(Paths.get("assets/a/r"));

        for (Path candidate : candidates) {
            if (candidate == null) continue;
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(normalized.resolve("tiles").resolve("quality").resolve("low_32").resolve("cells"))) {
                return normalized.toString();
            }
        }
        return requestedRoot;
    }

    static int countPngFiles(Path root) {
        if (root == null || !Files.isDirectory(root)) return 0;
        try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
            return (int) stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName() != null && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .count();
        } catch (Throwable ignored) {
            return 0;
        }
    }"""
    content = content.replace("class ArtPackManager {", art_pack_helpers)

# 5. Redirect remaining ArtPackManager methods to use the resolver
content = content.replace(
    "String fallbackRoot = resolveBundledFallbackRoot(bundledFallbackRoot);",
    "String fallbackRoot = resolveInstalledArtRoot(resolveBundledFallbackRoot(bundledFallbackRoot));"
)
content = content.replace(
    "Path local = Paths.get(bundledRoot, \"tiles\", \"quality\", wanted);",
    "String resolvedBundledRoot = resolveInstalledArtRoot(bundledRoot);\n        Path local = Paths.get(resolvedBundledRoot, \"tiles\", \"quality\", wanted);"
)
content = content.replace(
    "Path bundledLow = Paths.get(bundledRoot, \"tiles\", \"quality\", \"low_32\");",
    "Path bundledLow = Paths.get(resolvedBundledRoot, \"tiles\", \"quality\", \"low_32\");"
)
content = content.replace(
    "return Paths.get(bundledRoot, \"tiles\", \"cells\").toString();",
    "String resolvedBundledRoot = resolveInstalledArtRoot(bundledRoot);\n        return Paths.get(resolvedBundledRoot, \"tiles\", \"cells\").toString();"
)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("[+] Patch applied successfully to MediaRuntimeFramework.java!")