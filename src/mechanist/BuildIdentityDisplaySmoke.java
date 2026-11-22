package mechanist;

import java.nio.file.Files;
import java.nio.file.Path;

/** Fast, offline client identity smoke: only the matching installed build gets a SHA. */
final class BuildIdentityDisplaySmoke {
    public static void main(String[] args) throws Exception {
        String commit = "b8758b49e7d04a1cb86c01ff1df8e7b0f3b41c08";
        String manifest = "{\"version\":\"0.9.54\",\"commit\":\"" + commit + "\"}";
        require(commit.equals(BuildIdentityAuthority.parseManifestCommit(manifest, "0.9.54")),
                "matching manifest must provide exact commit");
        require(BuildIdentityAuthority.parseManifestCommit(manifest, "0.9.53").isEmpty(),
                "stale manifest must not be presented as the current binary");
        require(BuildIdentityAuthority.parseManifestCommit(
                "{\"version\":\"0.9.54\",\"commit\":\"invalid\"}", "0.9.54").isEmpty(),
                "malformed manifest SHA must be rejected");
        require(BuildIdentityAuthority.parseManifestCommit("{}", "0.9.54").isEmpty(),
                "missing identity must not invent a commit");

        Path root = Files.createTempDirectory("mechanist-client-release-identity-");
        String previousRoot = System.getProperty("mechanist.launcher.appHome");
        String previousVersion = System.getProperty("mechanist.version");
        try {
            Path dir = Files.createDirectories(root.resolve("manifests"));
            Files.writeString(dir.resolve("runtime-manifest.json"), manifest);
            require(commit.equals(BuildIdentityAuthority.readInstalledCommit(root, "0.9.54")),
                    "installed manifest did not match");
            require(BuildIdentityAuthority.readInstalledCommit(root, "0.9.53").isEmpty(),
                    "different runtime version must not inherit stale SHA");
            System.setProperty("mechanist.version", "0.9.54");
            System.setProperty("mechanist.launcher.appHome", root.toString());
            require("0.9.54".equals(BuildIdentityAuthority.version()), "explicit version not applied");
            require(commit.equals(BuildIdentityAuthority.commit()), "installed source SHA not exposed");
            require(BuildIdentityAuthority.clientWindowTitle().contains(commit.substring(0, 12)),
                    "game window title must distinguish builds");
            Files.delete(dir.resolve("runtime-manifest.json"));
            require(BuildIdentityAuthority.readInstalledCommit(root, "0.9.54").isEmpty(),
                    "removed manifest must not retain stale disk identity");
        } finally {
            if (previousRoot == null) System.clearProperty("mechanist.launcher.appHome");
            else System.setProperty("mechanist.launcher.appHome", previousRoot);
            if (previousVersion == null) System.clearProperty("mechanist.version");
            else System.setProperty("mechanist.version", previousVersion);
            Files.deleteIfExists(root.resolve("manifests/runtime-manifest.json"));
            Files.deleteIfExists(root.resolve("manifests"));
            Files.deleteIfExists(root);
        }
        System.out.println("BuildIdentityDisplaySmoke PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
