package mechanist;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** Resolves user-owned mutable client storage without writing into the installation. */
final class ClientMutableStorageAuthority {
    static final String VERSION = "client-mutable-storage-1";
    private static final String OVERRIDE_PROPERTY = "mechanist.client.storage.root";

    private ClientMutableStorageAuthority() { }

    static Path root() {
        String override = System.getProperty(OVERRIDE_PROPERTY, "").trim();
        if (!override.isBlank()) {
            return normalized(Path.of(override));
        }

        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            String localAppData = environment("LOCALAPPDATA");
            if (!localAppData.isBlank()) {
                return normalized(Path.of(localAppData, "TheMechanist"));
            }
            String appData = environment("APPDATA");
            if (!appData.isBlank()) {
                return normalized(Path.of(appData, "TheMechanist"));
            }
        }

        String xdgData = environment("XDG_DATA_HOME");
        if (!xdgData.isBlank()) {
            return normalized(Path.of(xdgData, "the-mechanist"));
        }

        String home = System.getProperty("user.home", "").trim();
        if (home.isBlank()) {
            throw new IllegalStateException(
                    "user.home is unavailable and no client storage override was provided");
        }
        return normalized(Path.of(home, ".local", "share", "the-mechanist"));
    }

    static Path remoteClientRoot() {
        return root().resolve("remote-client").normalize();
    }

    static Path resumeTokenRoot() {
        return remoteClientRoot().resolve("resume-tokens").normalize();
    }

    static Path remoteClientSettingsFile() {
        return remoteClientRoot().resolve("connection.properties").normalize();
    }

    static String auditSummary() {
        return "authority=" + VERSION
                + " root=" + root()
                + " installMutation=false"
                + " remoteTokenRoot=" + resumeTokenRoot()
                + " tokenContentInAudit=false";
    }

    private static Path normalized(Path path) {
        Path normalized = Objects.requireNonNull(path, "path")
                .toAbsolutePath()
                .normalize();
        if (normalized.getNameCount() < 1) {
            throw new IllegalArgumentException("client storage root is invalid");
        }
        return normalized;
    }

    private static String environment(String name) {
        return Objects.requireNonNullElse(System.getenv(name), "").trim();
    }
}
