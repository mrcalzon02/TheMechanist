package mechanist.launcher;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

final class LauncherSoundFeedback {
    private final Path clientPackageDir;
    private final Path launcherDir;

    LauncherSoundFeedback(LauncherConfig config) {
        this.clientPackageDir = config.clientPackageDir;
        this.launcherDir = config.launcherDir;
    }

    void button() { playFirst("assets/sound/core/ambient_press_01.wav"); }
    void panel() { playFirst("assets/sound/core/ambient_chime_01.wav", "assets/sound/core/ambient_press_01.wav"); }
    void warning() { playFirst("assets/sound/core/ambient_alarm_far_01.wav", "assets/sound/core/ambient_chime_01.wav"); }

    private void playFirst(String... relativePaths) {
        for (String rel : relativePaths) {
            if (playResource("/" + rel)) return;
            File launcherFile = launcherDir.resolve(rel).toFile();
            if (launcherFile.exists()) { play(launcherFile); return; }
            File clientFile = clientPackageDir.resolve(rel).toFile();
            if (clientFile.exists()) { play(clientFile); return; }
        }
    }

    private boolean playResource(String resourcePath) {
        if (LauncherSoundFeedback.class.getResource(resourcePath) == null) return false;
        new Thread(() -> {
            try (InputStream raw = LauncherSoundFeedback.class.getResourceAsStream(resourcePath);
                 BufferedInputStream buffered = raw == null ? null : new BufferedInputStream(raw);
                 AudioInputStream ais = buffered == null ? null : AudioSystem.getAudioInputStream(buffered)) {
                if (ais != null) play(ais);
            } catch (Throwable ignored) {
                // Launcher audio feedback must never block install/update/launch.
            }
        }, "launcher-sound-feedback").start();
        return true;
    }

    private void play(File file) {
        new Thread(() -> {
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
                play(ais);
            } catch (Throwable ignored) {
                // Launcher audio feedback must never block install/update/launch.
            }
        }, "launcher-sound-feedback").start();
    }

    private void play(AudioInputStream ais) throws Exception {
        Clip clip = AudioSystem.getClip();
        clip.open(ais);
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), -12.0f)));
        }
        clip.addLineListener(ev -> { if (ev.getType() == javax.sound.sampled.LineEvent.Type.STOP) clip.close(); });
        clip.start();
    }
}
