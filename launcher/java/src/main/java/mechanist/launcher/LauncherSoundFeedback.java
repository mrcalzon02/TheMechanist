package mechanist.launcher;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.File;
import java.nio.file.Path;

final class LauncherSoundFeedback {
    private final Path repoDir;
    private final Path launcherDir;

    LauncherSoundFeedback(LauncherConfig config) {
        this.repoDir = config.repoDir;
        this.launcherDir = config.launcherDir;
    }

    void button() { playFirst("assets/sound/wav/tp_gui_button_press_01.wav", "assets/sound/wav/machine_start.wav"); }
    void panel() { playFirst("assets/sound/wav/tp_gui_panel_open_01.wav", "assets/sound/wav/tp_gui_button_press_01.wav"); }
    void warning() { playFirst("assets/sound/core/ambient_alarm_far_01.wav", "assets/sound/wav/tp_gui_panel_open_01.wav"); }

    private void playFirst(String... relativePaths) {
        for (String rel : relativePaths) {
            File f = repoDir.resolve(rel).toFile();
            if (!f.exists()) f = launcherDir.resolve(rel).toFile();
            if (f.exists()) { play(f); return; }
        }
    }

    private void play(File file) {
        new Thread(() -> {
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), -12.0f)));
                }
                clip.addLineListener(ev -> { if (ev.getType() == javax.sound.sampled.LineEvent.Type.STOP) clip.close(); });
                clip.start();
            } catch (Throwable ignored) {
                // Launcher audio feedback must never block install/update/launch.
            }
        }, "launcher-sound-feedback").start();
    }
}
