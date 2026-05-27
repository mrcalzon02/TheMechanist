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
        out.append("body{font-family:Arial,sans-serif;margin:0;background:#111;color:#eee;}\n");
        out.append(".toolbar{position:sticky;top:0;z-index:50;background:#181818;padding:16px;border-bottom:1px solid #333;display:flex;gap:12px;align-items:center;}\n");
        out.append("button{background:#2b6cb0;color:#fff;border:none;border-radius:6px;padding:10px 14px;font-weight:bold;cursor:pointer;}\n");
        out.append("button:hover{background:#3d7fd1;}\n");
        out.append(".meta{color:#aaa;}\n");
        out.append(".page{padding:24px;}\n");
        out.append(".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(180px,1fr));gap:16px;}\n");
        out.append(".card{background:#1b1b1b;border:2px solid #333;border-radius:8px;padding:10px;overflow:hidden;cursor:pointer;transition:border-color .15s ease,transform .15s ease;}\n");
        out.append(".card:hover{border-color:#666;transform:translateY(-1px);}\n");
        out.append(".card.selected{border-color:#ff5757;box-shadow:0 0 0 2px rgba(255,87,87,.25);}\n");
        out.append(".card img{display:block;width:100%;height:auto;image-rendering:auto;background:#222;border-radius:4px;}\n");
        out.append(".name{font-size:12px;line-height:1.35;margin-top:8px;overflow-wrap:anywhere;color:#ddd;}\n");
        out.append(".check{margin-top:8px;display:flex;align-items:center;gap:8px;font-size:12px;color:#bbb;}\n");
        out.append("textarea{width:100%;height:180px;margin-top:18px;background:#101010;color:#f0f0f0;border:1px solid #333;padding:12px;border-radius:8px;font-family:monospace;}\n");
        out.append("</style>\n</head>\n<body>\n");
        out.append("<div class=\"toolbar\">\n");
        out.append("<button onclick=\"exportSelected()\">Export Target List</button>\n");
        out.append("<div class=\"meta\">Generated at ").append(escape(Instant.now().toString())).append(" | Images: ").append(imagePaths.size()).append(" | Click tiles to flag possible infringement.</div>\n");
        out.append("</div>\n");
        out.append("<div class=\"page\">\n");
        out.append("<h1>The Mechanist Asset Visual Audit</h1>\n");
        out.append("<div class=\"grid\">\n");

        int index = 0;
        for (String path : imagePaths) {
            String src = "../assets/" + path;
            String id = "asset_" + index++;
            out.append("<div class=\"card\" data-path=\"").append(escape(path)).append("\" onclick=\"toggleCard(this)\">\n");
            out.append("<img src=\"").append(escape(src)).append("\" alt=\"").append(escape(path)).append("\">\n");
            out.append("<div class=\"name\">").append(escape(path)).append("</div>\n");
            out.append("<label class=\"check\"><input type=\"checkbox\" id=\"").append(id).append("\" onclick=\"event.stopPropagation();toggleCheckbox(this);\"> Flag for review</label>\n");
            out.append("</div>\n");
        }

        out.append("</div>\n");
        out.append("<textarea id=\"exportBox\" placeholder=\"Selected asset paths will appear here after export.\"></textarea>\n");
        out.append("</div>\n");

        out.append("<script>\n");
        out.append("function toggleCard(card){const box=card.querySelector('input[type=checkbox]');box.checked=!box.checked;updateCard(card,box.checked);}\n");
        out.append("function toggleCheckbox(box){updateCard(box.closest('.card'),box.checked);}\n");
        out.append("function updateCard(card,selected){if(selected){card.classList.add('selected');}else{card.classList.remove('selected');}}\n");
        out.append("function exportSelected(){const selected=[...document.querySelectorAll('.card.selected')].map(c=>c.dataset.path);const output=selected.join('\\n');document.getElementById('exportBox').value=output;if(output.length>0&&navigator.clipboard){navigator.clipboard.writeText(output).catch(()=>{});}}\n");
        out.append("</script>\n");

        out.append("</body>\n</html>\n");
        return out.toString();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
