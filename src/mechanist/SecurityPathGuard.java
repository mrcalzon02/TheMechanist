package mechanist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** Restricts server file access to a canonical root and detects unsafe runtime ownership. */
final class SecurityPathGuard {
    private final Path root;

    SecurityPathGuard(Path root) throws IOException {
        Objects.requireNonNull(root, "root");
        Files.createDirectories(root);
        this.root = root.toRealPath().normalize();
    }

    Path root() { return root; }

    Path resolveInside(String relative) throws IOException {
        if (relative == null || relative.isBlank()) throw new IOException("Empty relative path denied");
        String clean = relative.replace('\0', '_').replace('\\', '/');
        Path resolved = root.resolve(clean).normalize();
        if (!resolved.startsWith(root)) throw new IOException("Directory traversal denied: " + relative);
        Path parent = resolved.getParent();
        if (parent != null) Files.createDirectories(parent);
        return resolved;
    }

    static RuntimePrivilegeReport inspectRuntimePrivilege() {
        String os = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);
        String user = System.getProperty("user.name", "unknown");
        boolean linux = os.contains("linux");
        boolean root = linux && "root".equals(user);
        String action = root
                ? "Server is running as root. Java cannot safely drop POSIX privileges without an external launcher; start the service as a non-privileged user after binding policy is configured."
                : "Server runtime user is non-root or privilege status is not Linux-root.";
        return new RuntimePrivilegeReport(os, user, root, action);
    }

    record RuntimePrivilegeReport(String osName, String userName, boolean linuxRoot, String recommendation) { }
}
