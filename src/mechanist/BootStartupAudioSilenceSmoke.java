package mechanist;

import java.nio.file.Files;
import java.nio.file.Path;

/** Guards the package/smoke-test boot path against startup sound effects and release authority regressions. */
final class BootStartupAudioSilenceSmoke {
    public static void main(String[] args) throws Exception {
        GameOptions defaults = new GameOptions();
        require(!defaults.bootSound, "boot sound defaults must stay disabled");

        SoundManager manager = new SoundManager();
        manager.load();
        require(!manager.sounds.containsKey("boot"), "boot sound asset must not be registered");
        require(!manager.generatedCueKeys.contains("boot"), "boot sound must not fall back to generated cue synthesis");
        require(manager.startupCueSuppressed("boot"), "boot cue key must be suppressed by the audio runtime");
        require(manager.generatedCueBytes("boot", 80, new javax.sound.sampled.AudioFormat(44100f, 16, 1, true, false)).length == 0,
                "boot key must not produce generated startup tone bytes");

        Path legacyPanel = Path.of("src", "mechanist", "LegacyPanelContext.java");
        if (Files.exists(legacyPanel)) {
            String source = Files.readString(legacyPanel);
            require(!source.contains("sounds.play(\"boot\""), "GamePanel constructor must not play boot sound effects");
        }

        ReleaseBuildIdentitySmoke.main(args);
        verifyUserOptionsPersistence();
        System.out.println("BootStartupAudioSilenceSmoke PASS startup sound effects disabled; release identity, persistent remote sessions, hosted-session commands, immutable rosters, and closed world authority verified.");
    }

    private static void verifyUserOptionsPersistence() throws Exception {
        Path sandbox = Files.createTempDirectory("Mechanist options persistence with spaces ");
        String previous = System.getProperty(GameStorageManager.OVERRIDE_PROPERTY);
        try {
            Path userRoot = sandbox.resolve("user data");
            System.setProperty(GameStorageManager.OVERRIDE_PROPERTY, userRoot.toString());
            Path install = sandbox.resolve("installed application");
            Path client = Files.createDirectories(install.resolve("packages/client"));
            Path oldOptions = install.resolve("settings/options.properties");
            Path packageOptions = client.resolve("settings/options.properties");
            Path target = userRoot.resolve("settings/options.properties");
            require(GameOptions.settingsPath().equals(target), "options must live under user data");
            require(GameStorageManager.optionsReadPath(target, client).equals(target), "fresh install must use user path");
            Files.createDirectories(oldOptions.getParent());
            Files.writeString(oldOptions, "musicVolume=11\n");
            require(GameStorageManager.optionsReadPath(target, client).equals(oldOptions),
                    "older installed settings should be readable without rewriting them");
            Files.createDirectories(packageOptions.getParent());
            Files.writeString(packageOptions, "musicVolume=22\n");
            require(GameStorageManager.optionsReadPath(target, client).equals(packageOptions),
                    "package-relative legacy settings should take precedence over older root settings");

            Files.createDirectories(target.getParent());
            Files.writeString(target, "mechanist.musicRoot=custom music\nmechanist.musicManifest=custom.tsv\n");
            GameOptions options = new GameOptions();
            options.musicVolume = 37;
            options.soundEnabled = false;
            options.save();
            require(GameOptions.settingsReadPath().equals(target), "user settings must override legacy files");
            require(GameStorageManager.optionsReadPath(target, client).equals(target),
                    "legacy settings must never shadow the new user file");
            GameOptions loaded = GameOptions.load();
            require(loaded.musicVolume == 37 && !loaded.soundEnabled, "user options did not survive reload");
            SoundManager sounds = new SoundManager();
            require("custom music".equals(sounds.readOption("mechanist.musicRoot"))
                            && "custom.tsv".equals(sounds.readOption("mechanist.musicManifest")),
                    "audio runtime lost optional path overrides on save");
            loaded.musicVolume = 69;
            loaded.save();
            require(GameOptions.load().musicVolume == 69, "replacement options did not survive reload");
            require(Files.readString(oldOptions).equals("musicVolume=11\n")
                            && Files.readString(packageOptions).equals("musicVolume=22\n"),
                    "options save modified legacy installation files");
            try (var files = Files.list(target.getParent())) {
                require(files.count() == 1, "options save left temporary files behind");
            }
        } finally {
            if (previous == null) System.clearProperty(GameStorageManager.OVERRIDE_PROPERTY);
            else System.setProperty(GameStorageManager.OVERRIDE_PROPERTY, previous);
            try (var files = Files.walk(sandbox)) {
                for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private BootStartupAudioSilenceSmoke() { }
}
