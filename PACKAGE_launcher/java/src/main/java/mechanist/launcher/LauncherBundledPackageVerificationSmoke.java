package mechanist.launcher;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Headless packaged smoke for the limited-alpha thin launcher. */
public final class LauncherBundledPackageVerificationSmoke {
    private static final String REMOTE_MAIN = "mechanist.RemoteClientMain";

    public static void main(String[] args) throws Exception {
        LauncherConfig config = LauncherConfig.defaults();
        require(!LauncherDistributionPolicy.remoteAcquisitionEnabled(),
                "limited-alpha launcher must not advertise unfinished remote acquisition");
        require(LauncherDistributionPolicy.selectableSources().length == 1,
                "limited-alpha launcher must expose exactly one real package source");
        require(LauncherDistributionPolicy.LIMITED_ALPHA_SOURCE.equals(
                        LauncherDistributionPolicy.normalizeSource("testing")),
                "inactive channel names must normalize to the bundled alpha source");

        PackageInstallService packages = new PackageInstallService(config);
        PackageInstallService.PackageIdentity identity =
                packages.verifyInstalledPackages();
        require(identity.ready(),
                "bundled package verification failed: " + identity.summary());
        require(identity.manifest() != null
                        && Files.isRegularFile(identity.manifest()),
                "verified launcher manifest is missing");
        require(identity.manifest().getFileName().toString()
                        .equals("launcher-runtime-manifest.json"),
                "launcher selected the wrong manifest: " + identity.manifest());
        require(identity.client() != null
                        && Files.isRegularFile(identity.client()
                        .absolutePath(config.installRoot)),
                "verified client package is missing");
        require(identity.server() != null
                        && Files.isRegularFile(identity.server()
                        .absolutePath(config.installRoot)),
                "verified server package is missing");
        require(identity.supportLibraries() != null
                        && !identity.supportLibraries().isEmpty(),
                "verified support-library set is empty");
        require(packages.gameLauncherPresent(),
                "launcher readiness probe disagrees with package identity verification");
        require(!LauncherDistributionPolicy.sourceStatus(config)
                        .contains("Remote acquisition is disabled"),
                "bundled distribution was not detected at the configured install root");

        String manifestText = Files.readString(
                identity.manifest(),
                StandardCharsets.UTF_8);
        require(manifestText.contains(
                        "\"remote_main_class\": \"" + REMOTE_MAIN + "\""),
                "launcher manifest does not declare the governed remote-client entry");
        Path remoteLauncher = config.installRoot.resolve(
                windows()
                        ? "Run-Remote-Client.cmd"
                        : "run-remote-client.sh");
        require(Files.isRegularFile(remoteLauncher),
                "portable remote-client launch script is missing: "
                        + remoteLauncher);
        String remoteLauncherText = Files.readString(
                remoteLauncher,
                StandardCharsets.UTF_8);
        require(remoteLauncherText.contains(REMOTE_MAIN),
                "portable remote-client launcher does not invoke " + REMOTE_MAIN);
        require(remoteLauncherText.contains("TheMechanist.jar")
                        && remoteLauncherText.toLowerCase(Locale.ROOT)
                        .contains("runtime"),
                "portable remote-client launcher is disconnected from the verified runtime");

        LauncherBuildIdentity.Identity build = LauncherBuildIdentity.read(config);
        require(build.canonical(),
                "portable distribution must expose the canonical runtime manifest");
        require(!"unknown".equals(build.version()),
                "packaged version identity is unknown");
        require(!"unknown".equals(build.commit()),
                "packaged source commit identity is unknown");
        require(!"unknown".equals(build.platform()),
                "packaged platform identity is unknown");
        require(build.javaRelease() == 17,
                "packaged Java release identity is not 17");
        require("installer-thin-launcher-client-server".equals(
                        build.distributionModel()),
                "packaged distribution model is not governed");

        System.out.println("LauncherBundledPackageVerificationSmoke PASS "
                + LauncherDistributionPolicy.auditSummary(config)
                + " build={" + build.compactLine() + "}"
                + " manifest=" + identity.manifest()
                + " remoteMain=" + REMOTE_MAIN
                + " remoteLauncher=" + remoteLauncher
                + " supportLibraries=" + identity.supportLibraries().size());
    }

    private static boolean windows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private LauncherBundledPackageVerificationSmoke() { }
}
