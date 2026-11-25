package mechanist;

import java.nio.file.Files;
import java.nio.file.Path;

/** Exercises mutable startup/settings/profile operations from an installed payload. */
final class InstalledUserStorageSmoke {
    public static void main(String[] args) throws Exception {
        String configured = System.getProperty(GameStorageManager.OVERRIDE_PROPERTY, "").trim();
        require(!configured.isEmpty(), "an explicit isolated user root is required");
        Path root = Path.of(configured).toAbsolutePath().normalize();
        require(GameStorageManager.get().rootDir().equals(root), "wrong selected storage root");
        DebugLog.init("installed-storage-smoke");
        try {
            UserProfileAuthority.Profile detected = UserProfileAuthority.detect();
            EulaConsentAuthority.accept(detected);
            require(EulaConsentAuthority.hasConsent(), "consent did not survive disk readback");
            var runtime = JvmRuntimeProfileAuthority.load();
            runtime.maxRamMb = 1024;
            JvmRuntimeProfileAuthority.save(runtime);
            require(JvmRuntimeProfileAuthority.load().maxRamMb == 1024, "runtime settings did not persist");
            GameOptions options = GameOptions.load();
            options.musicVolume = 69;
            options.save();
            require(GameOptions.load().musicVolume == 69, "game options did not persist");
            var local = FallbackProfileManagementAuthority.loadOrCreate(detected);
            local.profileName = "Installed Storage Tester";
            local.legalAdultCertified = true; // Owned CI profile, never the user's real profile.
            FallbackProfileManagementAuthority.save(local);
            Path seed = GameStorageManager.get().profilesDir().resolve("profile.seed");
            String identity = Files.readString(seed);
            Path archive = GameStorageManager.get().exportDir().resolve("installed-storage-profile.zip");
            FallbackProfileManagementAuthority.exportMigrationZip(local, archive.toFile());
            local.profileName = "Changed after export";
            FallbackProfileManagementAuthority.save(local);
            var imported = FallbackProfileManagementAuthority.importMigrationZip(archive.toFile());
            require("Installed Storage Tester".equals(imported.profileName), "profile migration did not restore metadata");
            require(identity.equals(Files.readString(seed)), "profile migration changed the identity seed");
            for (String relative : new String[] {"logs/current.log", "settings/legal.properties",
                    "settings/jvm_runtime.properties", "settings/options.properties",
                    "saves/data/profiles/active_profile.properties", "saves/data/profiles/profile.seed"}) {
                require(Files.isRegularFile(root.resolve(relative)), "user-owned file missing: " + relative);
            }
        } finally {
            DebugLog.shutdown("installed storage smoke complete");
        }
        System.out.println("InstalledUserStorageSmoke PASS userLogs=true userSettings=true profileMigration=true stableIdentity=true");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
