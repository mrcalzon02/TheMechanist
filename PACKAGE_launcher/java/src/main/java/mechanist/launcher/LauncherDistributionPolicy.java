package mechanist.launcher;

import java.nio.file.Files;

/** Honest package-source policy for the controlled limited-alpha launcher. */
final class LauncherDistributionPolicy {
    static final String LIMITED_ALPHA_SOURCE = "limited-alpha-bundled";

    static String[] selectableSources() {
        return new String[] {LIMITED_ALPHA_SOURCE};
    }

    static String normalizeSource(String value) {
        return LIMITED_ALPHA_SOURCE;
    }

    static boolean remoteAcquisitionEnabled() {
        return false;
    }

    static String sourceStatus(LauncherConfig config) {
        if (config == null) return "Launcher configuration unavailable.";
        boolean bundled = Files.isDirectory(config.manifestDir)
                && Files.isDirectory(config.clientPackageDir)
                && Files.isDirectory(config.serverPackageDir);
        if (bundled) {
            return "Bundled limited-alpha package set detected at " + config.installRoot;
        }
        if (Files.isDirectory(config.packageSeedRoot)) {
            return "Local verified package seed detected at " + config.packageSeedRoot;
        }
        return "No bundled package or local verified seed is present. Remote acquisition is disabled for this alpha.";
    }

    static String auditSummary(LauncherConfig config) {
        return "source=" + LIMITED_ALPHA_SOURCE
                + " remoteAcquisition=false"
                + " repositoryClone=false"
                + " localSeedSupported=true"
                + " bundledPayloadSupported=true"
                + " status=" + sourceStatus(config);
    }

    private LauncherDistributionPolicy() { }
}
