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
        System.out.println("BootStartupAudioSilenceSmoke PASS startup sound effects disabled; release identity, persistent remote sessions, hosted-session commands, immutable rosters, and closed world authority verified.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private BootStartupAudioSilenceSmoke() { }
}
