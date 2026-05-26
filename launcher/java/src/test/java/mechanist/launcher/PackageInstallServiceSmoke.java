package mechanist.launcher;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

final class PackageInstallServiceSmoke {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("mechanist-package-smoke");
        Path seed = Files.createTempDirectory("mechanist-package-seed");
        writePackage(seed, "v1-client", "v1-server", "v1-support", currentPlatform(), 2);

        System.setProperty("mechanist.launcher.installRoot", root.toString());
        System.setProperty("mechanist.launcher.userDataRoot", root.resolve("test-user-data").toString());
        System.setProperty("mechanist.launcher.roamingConfigRoot", root.resolve("test-config").toString());
        System.setProperty("mechanist.launcher.localStateRoot", root.resolve("test-state").toString());
        System.setProperty("mechanist.launcher.packageSeedRoot", seed.toString());
        LauncherConfig config = LauncherConfig.defaults();
        PackageInstallService service = new PackageInstallService(config);
        service.installOrUpdate("main");
        PackageInstallService.PackageIdentity installed = service.verifyInstalledPackages();
        if (!installed.ready()) throw new AssertionError("Expected package seed install to verify: " + installed.summary());

        writePackage(seed, "v2-client", "v2-server", "v2-support", currentPlatform(), 2);
        service.installOrUpdate("main");
        PackageInstallService.PackageIdentity updated = service.verifyInstalledPackages();
        if (!updated.ready()) throw new AssertionError("Expected package seed update to verify: " + updated.summary());

        Path supportJar = root.resolve("packages/support/lib/support.jar");
        Files.writeString(supportJar, "tampered-support-package", StandardCharsets.UTF_8);
        PackageInstallService.PackageIdentity bad = service.verifyInstalledPackages();
        if (bad.ready()) throw new AssertionError("Expected tampered support library to fail verification.");
        if (!bad.summary().contains("support library")) throw new AssertionError("Expected support-library failure, got: " + bad.summary());

        Files.deleteIfExists(seed.resolve("manifests/windows-runtime-manifest.json"));
        service.repair("main");
        PackageInstallService.PackageIdentity repaired = service.verifyInstalledPackages();
        if (!repaired.ready()) throw new AssertionError("Expected rollback repair to verify: " + repaired.summary());

        writePackage(seed, "v3-client", "v3-server", "v3-support", "wrong-platform", 2);
        try {
            service.installOrUpdate("main");
            throw new AssertionError("Expected wrong-platform seed to fail.");
        } catch (java.io.IOException expected) {
            if (!expected.getMessage().contains("platform mismatch")) throw expected;
        }

        writePackage(seed, "v3-client", "v3-server", "v3-support", currentPlatform(), 999);
        try {
            service.installOrUpdate("main");
            throw new AssertionError("Expected unsupported schema seed to fail.");
        } catch (java.io.IOException expected) {
            if (!expected.getMessage().contains("Unsupported runtime manifest schema")) throw expected;
        }
    }

    private static void writePackage(Path root, String clientText, String serverText, String supportText, String platform, int schema) throws Exception {
        Path manifests = root.resolve("manifests");
        Path client = root.resolve("packages/client");
        Path server = root.resolve("packages/server");
        Path support = root.resolve("packages/support/lib");
        Files.createDirectories(manifests);
        Files.createDirectories(client);
        Files.createDirectories(server);
        Files.createDirectories(support);

        Path clientJar = client.resolve("TheMechanist.jar");
        Path serverJar = server.resolve("TheMechanistServer.jar");
        Path supportJar = support.resolve("support.jar");
        Files.writeString(clientJar, clientText, StandardCharsets.UTF_8);
        Files.writeString(serverJar, serverText, StandardCharsets.UTF_8);
        Files.writeString(supportJar, supportText, StandardCharsets.UTF_8);

        Files.writeString(manifests.resolve("windows-runtime-manifest.json"), """
                {
                  "schema": %d,
                  "distribution_model": "installer-thin-launcher-client-server",
                  "version": "smoke",
                  "platform": "%s",
                  "client": { "path": "packages/client/TheMechanist.jar", "sha256": "%s", "size": %d },
                  "server": { "path": "packages/server/TheMechanistServer.jar", "sha256": "%s", "size": %d },
                  "support_libraries": [
                    {"path": "packages/support/lib/support.jar", "sha256": "%s", "size": %d}
                  ]
                }
                """.formatted(schema, platform, sha256(clientJar), Files.size(clientJar), sha256(serverJar), Files.size(serverJar), sha256(supportJar), Files.size(supportJar)), StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(path))).toLowerCase();
    }

    private static String currentPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();
        String cpu = (arch.contains("64") || arch.contains("amd64") || arch.contains("x86_64")) ? "x64" : arch.replaceAll("[^a-z0-9]+", "");
        if (os.contains("win")) return "windows-" + cpu;
        if (os.contains("linux")) return "linux-" + cpu;
        if (os.contains("mac") || os.contains("darwin")) return "macos-" + cpu;
        return os.replaceAll("[^a-z0-9]+", "") + "-" + cpu;
    }
}
