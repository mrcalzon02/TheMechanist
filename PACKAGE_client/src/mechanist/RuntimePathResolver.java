package mechanist;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

final class RuntimePathResolver {
    private RuntimePathResolver() {}

    static File resolveAssetFile(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) return new File("");
        String normalized = normalize(rawPath);
        for (Path root : candidateRoots()) {
            for (String candidate : expandedCandidates(normalized)) {
                File f = root.resolve(candidate).normalize().toFile();
                if (f.exists()) return f;
            }
        }
        return preferredClientRoot().resolve(stripPackagePrefix(normalized)).normalize().toFile();
    }

    static String resolveAssetPath(String rawPath) {
        return resolveAssetFile(rawPath).getPath();
    }

    static String resolveDirectoryPath(String... rawPaths) {
        if (rawPaths != null) {
            for (String raw : rawPaths) {
                if (raw == null || raw.isBlank()) continue;
                File f = resolveAssetFile(raw);
                if (f.isDirectory()) return f.getPath();
            }
            for (String raw : rawPaths) {
                if (raw == null || raw.isBlank()) continue;
                return resolveAssetFile(raw).getPath();
            }
        }
        return preferredClientRoot().toString();
    }

    static String workingDirectorySummary() {
        return "cwd=" + cwd() + "; jarDir=" + jarDirectory() + "; clientRoot=" + preferredClientRoot();
    }

    private static List<Path> candidateRoots() {
        LinkedHashSet<Path> roots = new LinkedHashSet<>();
        Path cwd = cwd();
        Path jar = jarDirectory();
        roots.add(cwd);
        roots.add(cwd.resolve("PACKAGE_client"));
        roots.add(cwd.resolve("client"));
        roots.add(cwd.resolve("packages/client"));
        roots.add(jar);
        roots.add(jar.resolve("PACKAGE_client"));
        roots.add(jar.resolve("client"));
        roots.add(jar.resolve("packages/client"));
        Path parent = jar.getParent();
        if (parent != null) {
            roots.add(parent);
            roots.add(parent.resolve("PACKAGE_client"));
            roots.add(parent.resolve("client"));
            roots.add(parent.resolve("packages/client"));
        }
        return new ArrayList<>(roots);
    }

    private static Path preferredClientRoot() {
        Path cwd = cwd();
        if (cwd.getFileName() != null && "PACKAGE_client".equalsIgnoreCase(cwd.getFileName().toString())) return cwd;
        if (cwd.resolve("PACKAGE_client").toFile().isDirectory()) return cwd.resolve("PACKAGE_client");
        Path jar = jarDirectory();
        if (jar.getFileName() != null && "PACKAGE_client".equalsIgnoreCase(jar.getFileName().toString())) return jar;
        if (jar.resolve("PACKAGE_client").toFile().isDirectory()) return jar.resolve("PACKAGE_client");
        Path parent = jar.getParent();
        if (parent != null && parent.resolve("PACKAGE_client").toFile().isDirectory()) return parent.resolve("PACKAGE_client");
        return cwd;
    }

    private static List<String> expandedCandidates(String normalized) {
        ArrayList<String> out = new ArrayList<>();
        out.add(normalized);
        String stripped = stripPackagePrefix(normalized);
        if (!stripped.equals(normalized)) out.add(stripped);
        if (stripped.startsWith("assets/")) {
            out.add("PACKAGE_client/" + stripped);
            out.add("client/" + stripped);
            out.add("packages/client/" + stripped);
        }
        return out;
    }

    private static String stripPackagePrefix(String normalized) {
        String s = normalize(normalized);
        if (s.startsWith("packages/client/")) return s.substring("packages/client/".length());
        if (s.startsWith("client/")) return s.substring("client/".length());
        if (s.startsWith("PACKAGE_client/")) return s.substring("PACKAGE_client/".length());
        return s;
    }

    private static String normalize(String path) {
        return path.replace('\\', '/').replaceAll("^/", "");
    }

    private static Path cwd() {
        return Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    }

    private static Path jarDirectory() {
        try {
            URI uri = RuntimePathResolver.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path p = Paths.get(uri).toAbsolutePath().normalize();
            if (p.toFile().isFile()) return p.getParent();
            return p;
        } catch (Throwable ignored) {
            return cwd();
        }
    }
}
