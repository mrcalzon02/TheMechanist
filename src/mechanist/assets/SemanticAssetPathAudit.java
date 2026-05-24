package mechanist.assets;

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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stage 7 migration audit for fragile graphical path references.
 *
 * <p>The semantic asset migration is staged: legacy render bridges still exist,
 * but new gameplay/UI code should not introduce fresh raw image paths. This
 * audit scans source files, classifies references against an explicit allow-list
 * and a generated legacy baseline, and reports any new unbaselined direct image
 * path usage as a regression.</p>
 */
public final class SemanticAssetPathAudit {
    public static final String DEFAULT_ALLOWLIST = "assets/indexes/semantic_asset_direct_path_allowlist.tsv";
    public static final String DEFAULT_BASELINE = "assets/indexes/semantic_asset_direct_path_baseline.tsv";

    private static final Pattern STRING_LITERAL = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern IMAGE_EXT = Pattern.compile("(?i).*(\\.png|\\.jpg|\\.jpeg|\\.gif|\\.webp|\\.bmp|\\.ico|\\.svg)(?:$|[?#].*)");
    private static final List<String> IMAGE_API_TOKENS = List.of(
            "ImageIO.read(",
            "new ImageIcon(",
            "Toolkit.getDefaultToolkit().getImage(",
            ".getImage(",
            "getResourceAsStream(",
            "getResource("
    );

    private SemanticAssetPathAudit() {}

    public static AuditResult runDefault(Path projectRoot) throws IOException {
        Path root = normalizeRoot(projectRoot);
        return run(root,
                root.resolve("src"),
                root.resolve(DEFAULT_ALLOWLIST),
                root.resolve(DEFAULT_BASELINE));
    }

    public static AuditResult run(Path projectRoot, Path sourceRoot, Path allowlistFile, Path baselineFile) throws IOException {
        Path root = normalizeRoot(projectRoot);
        List<AllowRule> allowRules = loadAllowRules(root, allowlistFile);
        Set<String> baseline = loadBaseline(baselineFile);
        List<AuditFinding> findings = scanSource(root, sourceRoot);

        int approved = 0;
        int baselinedLegacy = 0;
        int unbaselined = 0;
        List<AuditFinding> unbaselinedFindings = new ArrayList<>();
        Map<String, Integer> countsByClassification = new LinkedHashMap<>();

        for (AuditFinding finding : findings) {
            String classification = classify(finding, allowRules, baseline);
            countsByClassification.merge(classification, 1, Integer::sum);
            if (classification.startsWith("APPROVED")) {
                approved++;
            } else if (classification.equals("BASELINED_LEGACY")) {
                baselinedLegacy++;
            } else if (classification.equals("UNBASELINED_RUNTIME_REFERENCE")) {
                unbaselined++;
                unbaselinedFindings.add(finding);
            }
        }
        return new AuditResult(findings.size(), approved, baselinedLegacy, unbaselined, countsByClassification, List.copyOf(unbaselinedFindings));
    }

    public static List<AuditFinding> scanSource(Path projectRoot, Path sourceRoot) throws IOException {
        Path root = normalizeRoot(projectRoot);
        Path src = sourceRoot == null ? root.resolve("src") : sourceRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(src)) {
            return List.of();
        }
        List<AuditFinding> findings = new ArrayList<>();
        try (var stream = Files.walk(src)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
            for (Path file : files) {
                scanFile(root, file, findings);
            }
        }
        return findings.stream()
                .sorted(Comparator.comparing(AuditFinding::signatureKey))
                .toList();
    }

    public static List<String> baselineLines(Path projectRoot, Path sourceRoot) throws IOException {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (AuditFinding finding : scanSource(projectRoot, sourceRoot)) {
            keys.add(finding.signatureKey());
        }
        return keys.stream().sorted().toList();
    }

    public static void writeBaseline(Path projectRoot, Path sourceRoot, Path baselineFile) throws IOException {
        List<String> keys = baselineLines(projectRoot, sourceRoot);
        List<String> lines = new ArrayList<>();
        lines.add("# Stage 7 generated baseline of current direct graphical path references.");
        lines.add("# New gameplay/UI image path calls should not be added here unless deliberately accepted as migration debt.");
        lines.add("# signatureKey");
        lines.addAll(keys);
        Files.createDirectories(baselineFile.toAbsolutePath().normalize().getParent());
        Files.write(baselineFile, lines, StandardCharsets.UTF_8);
    }

    private static void scanFile(Path root, Path file, List<AuditFinding> findings) throws IOException {
        Path relative = root.relativize(file.toAbsolutePath().normalize());
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNumber = i + 1;
            Matcher matcher = STRING_LITERAL.matcher(line);
            while (matcher.find()) {
                String literal = unescapeJavaString(matcher.group(1));
                if (isGraphicalLiteral(literal)) {
                    String kind = literal.toLowerCase(Locale.ROOT).contains("assets/") ? "IMAGE_ASSET_LITERAL" : "IMAGE_LITERAL";
                    findings.add(new AuditFinding(relative.toString().replace('\\', '/'), lineNumber, kind, normalizeEvidence(literal)));
                }
            }
            for (String token : IMAGE_API_TOKENS) {
                if (line.contains(token)) {
                    findings.add(new AuditFinding(relative.toString().replace('\\', '/'), lineNumber, "IMAGE_API_CALL", normalizeEvidence(line)));
                    break;
                }
            }
        }
    }

    private static boolean isGraphicalLiteral(String literal) {
        if (literal == null || literal.isBlank()) return false;
        String lower = literal.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".mp3")) return false;
        return IMAGE_EXT.matcher(literal).matches();
    }

    private static String classify(AuditFinding finding, List<AllowRule> allowRules, Set<String> baseline) {
        for (AllowRule rule : allowRules) {
            if (rule.matches(finding.relativePath())) {
                return "APPROVED:" + rule.reason();
            }
        }
        if (baseline.contains(finding.signatureKey())) {
            return "BASELINED_LEGACY";
        }
        return "UNBASELINED_RUNTIME_REFERENCE";
    }

    private static List<AllowRule> loadAllowRules(Path root, Path allowlistFile) throws IOException {
        if (allowlistFile == null || !Files.isRegularFile(allowlistFile)) {
            return List.of();
        }
        List<AllowRule> rules = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(allowlistFile, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank() || line.stripLeading().startsWith("#")) continue;
                String[] parts = line.split("\t", 2);
                if (parts.length < 2) {
                    throw new IOException("Invalid direct-path allowlist row at line " + lineNo + ": " + line);
                }
                String pattern = parts[0].trim().replace('\\', '/');
                String reason = parts[1].trim();
                if (pattern.isBlank() || reason.isBlank()) {
                    throw new IOException("Blank direct-path allowlist field at line " + lineNo);
                }
                rules.add(new AllowRule(pattern, reason));
            }
        }
        return List.copyOf(rules);
    }

    private static Set<String> loadBaseline(Path baselineFile) throws IOException {
        if (baselineFile == null || !Files.isRegularFile(baselineFile)) {
            return Set.of();
        }
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(baselineFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.stripLeading().startsWith("#")) continue;
                keys.add(line.strip());
            }
        }
        return Set.copyOf(keys);
    }

    private static Path normalizeRoot(Path projectRoot) {
        return (projectRoot == null ? Path.of(".") : projectRoot).toAbsolutePath().normalize();
    }

    private static String normalizeEvidence(String evidence) {
        if (evidence == null) return "";
        return evidence.trim().replaceAll("\\s+", " ");
    }

    private static String unescapeJavaString(String raw) {
        Objects.requireNonNull(raw, "raw");
        StringBuilder out = new StringBuilder(raw.length());
        boolean escaping = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (!escaping) {
                if (c == '\\') {
                    escaping = true;
                } else {
                    out.append(c);
                }
                continue;
            }
            switch (c) {
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case '\'' -> out.append('\'');
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                default -> out.append(c);
            }
            escaping = false;
        }
        if (escaping) out.append('\\');
        return out.toString();
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        if (args.length > 1 && "--write-baseline".equals(args[1])) {
            writeBaseline(root, root.resolve("src"), root.resolve(DEFAULT_BASELINE));
            System.out.println("Wrote semantic direct-path baseline to " + root.resolve(DEFAULT_BASELINE));
            return;
        }
        AuditResult result = runDefault(root);
        System.out.println(result.summaryLine());
        if (!result.unbaselinedFindings().isEmpty()) {
            for (AuditFinding finding : result.unbaselinedFindings().stream().limit(20).toList()) {
                System.out.println("UNBASELINED " + finding.signatureKey());
            }
        }
    }

    private record AllowRule(String pathPattern, String reason) {
        boolean matches(String relativePath) {
            String normalized = relativePath.replace('\\', '/');
            if (pathPattern.endsWith("/**")) {
                String prefix = pathPattern.substring(0, pathPattern.length() - 3);
                return normalized.startsWith(prefix);
            }
            return normalized.equals(pathPattern) || normalized.startsWith(pathPattern + "/");
        }
    }

    public record AuditFinding(String relativePath, int lineNumber, String kind, String evidence) {
        public AuditFinding {
            relativePath = Objects.requireNonNull(relativePath, "relativePath").replace('\\', '/');
            kind = Objects.requireNonNull(kind, "kind");
            evidence = Objects.requireNonNull(evidence, "evidence");
        }
        public String signatureKey() {
            return relativePath + "\t" + kind + "\t" + evidence;
        }
    }

    public record AuditResult(
            int findingCount,
            int approvedCount,
            int baselinedLegacyCount,
            int unbaselinedCount,
            Map<String, Integer> countsByClassification,
            List<AuditFinding> unbaselinedFindings
    ) {
        public boolean passed() {
            return unbaselinedCount == 0;
        }
        public String summaryLine() {
            return "SemanticAssetPathAudit findings=" + findingCount
                    + " approved=" + approvedCount
                    + " baselinedLegacy=" + baselinedLegacyCount
                    + " unbaselined=" + unbaselinedCount
                    + " classifications=" + countsByClassification;
        }
    }
}
