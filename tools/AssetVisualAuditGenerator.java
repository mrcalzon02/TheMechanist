import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a local HTML visual audit sheet for high-native renamed asset packs.
 *
 * Run from the repository root after regenerating docs/ASSET_MANIFEST.md:
 *   javac tools/AssetVisualAuditGenerator.java
 *   java -cp tools AssetVisualAuditGenerator
 */
public final class AssetVisualAuditGenerator {
    private static final Path MANIFEST = Paths.get("docs", "ASSET_MANIFEST.md");
    private static final Path OUTPUT = Paths.get("docs", "ASSET_VISUAL_SHEET.html");
    private static final Pattern BACKTICK_PATH = Pattern.compile("`([^`]+)`");

    private static final List<String> CLEAN_GROUPS = List.of(
            "Enforcer_Assets",
            "Automata_Drones",
            "Subversive_Cult",
            "subversive-cult-walls",
            "subversive_cult_floors",
            "Forge_Engineers"
    );

    private AssetVisualAuditGenerator() { }

    public static void main(String[] args) throws IOException {
        Path manifest = args.length >= 1 ? Paths.get(args[0]) : MANIFEST;
        Path output = args.length >= 2 ? Paths.get(args[1]) : OUTPUT;

        if (!Files.isRegularFile(manifest)) throw new IOException("Missing manifest: " + manifest.toAbsolutePath());

        List<String> imagePaths = collectImagePaths(manifest);
        String html = renderHtml(imagePaths);

        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(output, html, StandardCharsets.UTF_8);

        System.out.println("Wrote visual audit sheet: " + output.toAbsolutePath());
        System.out.println("Images included: " + imagePaths.size());
    }

    private static List<String> collectImagePaths(Path manifest) throws IOException {
        Set<String> paths = new LinkedHashSet<>();
        for (String line : Files.readAllLines(manifest, StandardCharsets.UTF_8)) {
            Matcher matcher = BACKTICK_PATH.matcher(line);
            while (matcher.find()) {
                String path = matcher.group(1).replace('\\', '/');
                if (isIncludedImage(path)) paths.add(path);
            }
        }
        List<String> sorted = new ArrayList<>(paths);
        sorted.sort(Comparator.naturalOrder());
        return sorted;
    }

    private static boolean isIncludedImage(String path) {
        if (!path.endsWith(".png")) return false;
        if (!path.startsWith("graphics/generated/high_native/")) return false;
        for (String group : CLEAN_GROUPS) {
            if (path.contains("/" + group + "/")) return true;
        }
        return false;
    }

    private static String renderHtml(List<String> imagePaths) {
        StringBuilder out = new StringBuilder();
        out.append("<!doctype html>\n<html lang=\"en\">\n<head>\n");
        out.append("<meta charset=\"utf-8\">\n");
        out.append("<title>The Mechanist Asset Visual Audit</title>\n");
        out.append("<style>\n");
        out.append("body{font-family:Arial,sans-serif;margin:24px;background:#111;color:#eee;}\n");
        out.append("h1{margin-bottom:4px;} .meta{color:#aaa;margin-bottom:20px;}\n");
        out.append(".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(180px,1fr));gap:16px;}\n");
        out.append(".card{background:#1b1b1b;border:1px solid #333;border-radius:8px;padding:10px;overflow:hidden;}\n");
        out.append(".card img{display:block;width:100%;height:auto;image-rendering:auto;background:#222;border-radius:4px;}\n");
        out.append(".name{font-size:12px;line-height:1.35;margin-top:8px;overflow-wrap:anywhere;color:#ddd;}\n");
        out.append("</style>\n</head>\n<body>\n");
        out.append("<h1>The Mechanist Asset Visual Audit</h1>\n");
        out.append("<div class=\"meta\">Generated at ").append(escape(Instant.now().toString())).append("; images: ").append(imagePaths.size()).append("</div>\n");
        out.append("<div class=\"grid\">\n");
        for (String path : imagePaths) {
            String src = "../assets/" + path;
            out.append("<div class=\"card\"><img src=\"").append(escape(src)).append("\" alt=\"").append(escape(path)).append("\"><div class=\"name\">").append(escape(path)).append("</div></div>\n");
        }
        out.append("</div>\n</body>\n</html>\n");
        return out.toString();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
