package mechanist.launcher;

import java.nio.file.Files;
import java.nio.file.Path;

/** Bounded smoke for offline installed version and source-commit readback. */
final class LauncherBuildIdentitySmoke {
    public static void main(String[] args) throws Exception {
        String commit = "b8758b49e7d04a1cb86c01ff1df8e7b0f3b41c08";
        Path install = Files.createTempDirectory("mechanist-launcher-identity-");
        String previous = System.getProperty("mechanist.launcher.installRoot");
        try {
            System.setProperty("mechanist.launcher.installRoot", install.toString());
            Path dir = Files.createDirectories(install.resolve("manifests"));
            Path manifest = dir.resolve("runtime-manifest.json");
            Files.writeString(manifest, "{\"version\":\"0.9.54\",\"commit\":\"" + commit
                    + "\",\"platform\":\"windows-x64\",\"javaRelease\":17,\"releaseHardened\":true}");
            LauncherBuildIdentity.Identity identity = LauncherBuildIdentity.read(LauncherConfig.defaults());
            require("0.9.54".equals(identity.version()), "version is missing from installed manifest");
            require(commit.equals(identity.commit()), "full source commit was lost");
            require("v0.9.54 / b8758b49e7d0".equals(identity.compactLabel()),
                    "launcher compact status must distinguish releases");
            require(identity.compactLine().contains(commit), "runtime diagnostics must show full SHA");

            Files.writeString(manifest, "{\"version\":\"0.9.54\",\"commit\":\"unknown\"}");
            LauncherBuildIdentity.Identity unknown = LauncherBuildIdentity.read(LauncherConfig.defaults());
            require(unknown.compactLabel().contains("commit unknown"),
                    "missing or invalid SHA must not masquerade as a release fingerprint");
            Files.deleteIfExists(manifest);
            requireNotInstalled(LauncherConfig.defaults());
        } finally {
            if (previous == null) System.clearProperty("mechanist.launcher.installRoot");
            else System.setProperty("mechanist.launcher.installRoot", previous);
            Files.deleteIfExists(install.resolve("manifests/runtime-manifest.json"));
            Files.deleteIfExists(install.resolve("manifests"));
            Files.deleteIfExists(install);
        }
        System.out.println("LauncherBuildIdentitySmoke PASS");
    }

    private static void requireNotInstalled(LauncherConfig config) throws Exception {
        try {
            LauncherBuildIdentity.read(config);
            throw new AssertionError("missing manifest must not report an installed release");
        } catch (java.io.IOException expected) {
            // No installed runtime, not a silently accepted fake version.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
