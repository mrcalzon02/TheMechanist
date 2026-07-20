package mechanist.launcher;

import java.nio.file.Files;

/** Headless packaged smoke for the limited-alpha thin launcher. */
public final class LauncherBundledPackageVerificationSmoke {
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
        PackageInstallService.PackageIdentity identity = packages.verifyInstalledPackages();
        require(identity.ready(), "bundled package verification failed: " + identity.summary());
        require(identity.manifest() != null && Files.isRegularFile(identity.manifest()),
                "verified launcher manifest is missing");
        require(identity.manifest().getFileName().toString().equals("launcher-runtime-manifest.json"),
                "launcher selected the wrong manifest: " + identity.manifest());
        require(identity.client() != null && Files.isRegularFile(identity.client().absolutePath(config.installRoot)),
                "verified client package is missing");
        require(identity.server() != null && Files.isRegularFile(identity.server().absolutePath(config.installRoot)),
                "verified server package is missing");
        require(identity.supportLibraries() != null && !identity.supportLibraries().isEmpty(),
                "verified support-library set is empty");
        require(packages.gameLauncherPresent(),
                "launcher readiness probe disagrees with package identity verification");
        require(!LauncherDistributionPolicy.sourceStatus(config).contains("Remote acquisition is disabled"),
                "bundled distribution was not detected at the configured install root");

        LauncherBuildIdentity.Identity build = LauncherBuildIdentity.read(config);
        require(build.canonical(), "portable distribution must expose the canonical runtime manifest");
        require(!"unknown".equals(build.version()), "packaged version identity is unknown");
        require(!"unknown".equals(build.commit()), "packaged source commit identity is unknown");
        require(!"unknown".equals(build.platform()), "packaged platform identity is unknown");
        require(build.javaRelease() == 17, "packaged Java release identity is not 17");
        require("installer-thin-launcher-client-server".equals(build.distributionModel()),
                "packaged distribution model is not governed");

        System.out.println("LauncherBundledPackageVerificationSmoke PASS "
                + LauncherDistributionPolicy.auditSummary(config)
                + " build={" + build.compactLine() + "}"
                + " manifest=" + identity.manifest()
                + " supportLibraries=" + identity.supportLibraries().size());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private LauncherBundledPackageVerificationSmoke() { }
}
