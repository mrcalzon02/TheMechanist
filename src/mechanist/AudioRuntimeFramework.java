package mechanist;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

class SoundManager {
    final Map<String, File> sounds = new HashMap<>();
    final Set<String> generatedCueKeys = new HashSet<>();
    final DynamicMusicManager music = new DynamicMusicManager();
    private final Object managedVoiceLock = new Object();
    private Clip introCrawlNarrationClip = null;
    private int introCrawlNarrationToken = 0;
    private boolean missingMusicPackAudited = false;
    void load() {
        put("button", "assets/sound/wav/tp_gui_button_press_01.wav");
        put("panelOpen", "assets/sound/wav/tp_gui_panel_open_01.wav");
        put("panelClose", "assets/sound/wav/tp_gui_panel_close_01.wav");
        put("portrait", "assets/sound/wav/tp_gui_portrait_select_01.wav");
        put("tab", "assets/sound/wav/tp_gui_tab_change_01.wav");
        put("type", "assets/sound/wav/typing_sounds.wav");
        put("intro_crawl_narration", "assets/sound/voice/new_world_intro_crawl_narration.wav");
        put("weapon_bash", "assets/sound/effects/bash.wav");
        put("weapon_flame", "assets/sound/effects/flame.wav");
        put("weapon_las", "assets/sound/effects/las.wav");
        put("weapon_lightning", "assets/sound/effects/lightning_crackle.wav");
        put("weapon_plasma", "assets/sound/effects/plas.wav");
        put("weapon_reload", "assets/sound/effects/reload.wav");
        put("weapon_shot", "assets/sound/effects/shot.wav");
        put("weapon_slice", "assets/sound/effects/slice.wav");
        put("weapon_thundering", "assets/sound/effects/thundering.wav");
        put("footstep_metal", "assets/sound/core/footstep_metal_01.wav");
        put("footstep_grate", "assets/sound/core/footstep_grate_01.wav");
        put("footstep_sludge", "assets/sound/core/footstep_sludge_01.wav");
        put("footstep_debris", "assets/sound/core/footstep_debris_01.wav");
        put("ambient_machine", "assets/sound/core/ambient_machine_01.wav");
        put("ambient_radio", "assets/sound/core/ambient_radio_01.wav");
        put("ambient_tv", "assets/sound/core/ambient_tv_01.wav");
        put("ambient_spark", "assets/sound/core/ambient_spark_01.wav");
        put("ambient_pipe", "assets/sound/core/ambient_pipe_01.wav");
        put("ambient_vent", "assets/sound/core/ambient_vent_01.wav");
        put("ambient_press", "assets/sound/core/ambient_press_01.wav");
        put("ambient_servo", "assets/sound/core/ambient_servo_01.wav");
        put("ambient_drip", "assets/sound/core/ambient_drip_01.wav");
        put("ambient_sludge", "assets/sound/core/ambient_sludge_01.wav");
        put("ambient_chime", "assets/sound/core/ambient_chime_01.wav");
        put("ambient_alarm_far", "assets/sound/core/ambient_alarm_far_01.wav");
        put("ambient_door_servo", "assets/sound/core/ambient_door_servo_01.wav");
        File musicRoot = resolveMusicRoot();
        File musicManifest = resolveMusicManifest(musicRoot);
        music.load(musicRoot, musicManifest);
        DebugLog.audit("AUDIO", "Loaded sound handles=" + sounds.size() + "; generatedCueFallbacks=" + generatedCueKeys.size() + "; musicRoot=" + musicRoot.getPath() + "; musicManifest=" + (musicManifest == null ? "<none>" : musicManifest.getPath()) + "; dynamic music playlists=" + music.playlistSummary() + "; OGG originals preserved in assets/sound if present.");
    }
    void put(String key, String path) {
        File f = runtimeAssetFile(path);
        if (f.exists()) sounds.put(key, f);
        else generatedCueKeys.add(key);
    }
    File runtimeAssetFile(String path) {
        if (path != null && path.replace('\\', '/').startsWith("assets/")) {
            File packagedClientOwned = new File("packages/client", path);
            if (packagedClientOwned.exists()) return packagedClientOwned;
            File packageClientOwned = new File("PACKAGE_client", path);
            if (packageClientOwned.exists()) return packageClientOwned;
            File clientOwned = new File("client", path);
            if (clientOwned.exists()) return clientOwned;
        }
        File f = new File(path);
        if (f.exists()) return f;
        return f;
    }
    File resolveMusicRoot() {
        String configured = readOption("mechanist.musicRoot");
        if (configured != null && !configured.isBlank()) {
            File f = new File(configured.trim());
            if (f.exists()) return f;
            DebugLog.warn("MUSIC", "Configured musicRoot missing; falling back. path=" + f.getPath());
        }
        return new File(AudioPackManager.prepareAndResolveMusicRoot("packages/client/assets/audiopacks", "cache/audiopacks", "packages/client/assets/music/wav"));
    }
    File resolveMusicManifest(File musicRoot) {
        String configured = readOption("mechanist.musicManifest");
        if (configured != null && !configured.isBlank()) {
            File f = new File(configured.trim());
            if (f.exists()) return f;
            DebugLog.warn("MUSIC", "Configured musicManifest missing; trying default manifest. path=" + f.getPath());
        }
        if (musicRoot != null) {
            File parent = musicRoot.getParentFile();
            if (parent != null) {
                File f = new File(parent, "music_manifest.tsv");
                if (f.exists()) return f;
            }
        }
        File f = runtimeAssetFile("assets/music/music_manifest.tsv");
        return f.exists() ? f : null;
    }
    String readOption(String key) {
        File f = new File("settings/options.properties");
        if (!f.exists()) return null;
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(f)) { p.load(in); return p.getProperty(key); }
        catch (Throwable t) { DebugLog.warn("AUDIO", "Could not read options while resolving audio path: " + t.getMessage()); return null; }
    }
    boolean startupCueSuppressed(String key) {
        return key != null && "boot".equalsIgnoreCase(key.trim());
    }
    void play(String key, GameOptions opt) {
        if (startupCueSuppressed(key)) return;
        if (opt == null || !opt.soundEnabled || opt.volume <= 0) return;
        File f = sounds.get(key); if (f == null) { playGeneratedCue(key, opt, ("type".equals(key) || "portrait".equals(key)) ? opt.conversationVolume : opt.sfxVolume); return; }
        new Thread(() -> {
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(f)) {
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                setClipGain(clip, ("type".equals(key) || "portrait".equals(key)) ? opt.conversationVolume : opt.sfxVolume);
                clip.addLineListener(ev -> { if (ev.getType() == LineEvent.Type.STOP) clip.close(); });
                clip.start();
            } catch (Throwable t) { DebugLog.error("AUDIO_PLAY", "Failed to play " + key + " from " + f, t); }
        }, "sound-" + key).start();
    }

    void playIntroCrawlNarration(GameOptions opt) {
        if (opt == null || !opt.soundEnabled || !opt.conversationSound || opt.conversationVolume <= 0) return;
        File f = sounds.get("intro_crawl_narration");
        if (f == null) { DebugLog.warn("AUDIO", "Intro crawl narration is unavailable; continuing with silent crawl."); return; }
        final int token;
        synchronized (managedVoiceLock) {
            introCrawlNarrationToken++;
            token = introCrawlNarrationToken;
            closeIntroCrawlNarrationLocked();
        }
        new Thread(() -> {
            Clip clip = null;
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(f)) {
                clip = AudioSystem.getClip();
                clip.open(ais);
                setClipGain(clip, Math.min(opt.conversationVolume, 55));
                synchronized (managedVoiceLock) {
                    if (token != introCrawlNarrationToken) { try { clip.close(); } catch(Throwable ignored){} return; }
                    introCrawlNarrationClip = clip;
                }
                final Clip managedClip = clip;
                clip.addLineListener(ev -> {
                    if (ev.getType() == LineEvent.Type.STOP) {
                        synchronized (managedVoiceLock) { if (introCrawlNarrationClip == managedClip) introCrawlNarrationClip = null; }
                        try { managedClip.close(); } catch(Throwable ignored){}
                    }
                });
                clip.start();
                DebugLog.audit("INTRO_CRAWL_AUDIO", "Started narration file=" + f.getName() + " durationMs=" + (clip.getMicrosecondLength() / 1000L) + " volume=" + Math.min(opt.conversationVolume, 55));
            } catch (Throwable t) {
                if (clip != null) { try { clip.close(); } catch(Throwable ignored){} }
                DebugLog.error("INTRO_CRAWL_AUDIO", "Failed to play intro crawl narration from " + f, t);
            }
        }, "intro-crawl-narration").start();
    }

    void stopIntroCrawlNarration(String reason) {
        synchronized (managedVoiceLock) {
            introCrawlNarrationToken++;
            closeIntroCrawlNarrationLocked();
        }
        DebugLog.audit("INTRO_CRAWL_AUDIO", "Stopped narration: " + reason);
    }

    private void closeIntroCrawlNarrationLocked() {
        Clip clip = introCrawlNarrationClip;
        introCrawlNarrationClip = null;
        if (clip != null) {
            try { clip.stop(); } catch(Throwable ignored){}
            try { clip.close(); } catch(Throwable ignored){}
        }
    }
    void playFootstep(String surface, int distance, GameOptions opt) {
        String s = surface == null ? "metal" : surface.toLowerCase(Locale.ROOT);
        String key = s.contains("sludge") ? "footstep_sludge" : (s.contains("grate") ? "footstep_grate" : (s.contains("debris") ? "footstep_debris" : "footstep_metal"));
        playDistanceScaled(key, distance, opt, true);
    }
    void playDistantCue(String key, int distance, GameOptions opt) { playDistanceScaled(key, distance, opt, false); }
    void playDistanceScaled(String key, int distance, GameOptions opt, boolean quiet) {
        if (startupCueSuppressed(key)) return;
        if (opt == null || !opt.soundEnabled || opt.volume <= 0) return;
        int base = quiet ? Math.min(opt.sfxVolume, 55) : Math.min(opt.sfxVolume, 42);
        int vol = Math.max(4, base - Math.max(0, distance) * (quiet ? 5 : 2));
        File f = sounds.get(key); if (f == null) { playGeneratedCue(key, opt, vol); return; }
        playWithVolume(key, f, vol);
    }
    void playWithVolume(String key, File f, int volumePercent) {
        if (startupCueSuppressed(key)) return;
        new Thread(() -> {
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(f)) {
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                setClipGain(clip, volumePercent);
                clip.addLineListener(ev -> { if (ev.getType() == LineEvent.Type.STOP) clip.close(); });
                clip.start();
            } catch (Throwable t) { DebugLog.error("AUDIO_PLAY", "Failed to play scaled " + key + " from " + f, t); }
        }, "sound-" + key).start();
    }
    void playWeaponEffect(String weapon, boolean ranged, GameOptions opt) {
        play(weaponEffectKey(weapon, ranged), opt);
    }
    void playReloadEffect(GameOptions opt) { play("weapon_reload", opt); }
    void playGeneratedCue(String key, GameOptions opt, int volumePercent) {
        if (startupCueSuppressed(key)) return;
        if (opt == null || !opt.soundEnabled || volumePercent <= 0) return;
        String safeKey = key == null || key.isBlank() ? "cue" : key.replaceAll("[^A-Za-z0-9_-]+", "_");
        new Thread(() -> {
            AudioFormat format = new AudioFormat(44100f, 16, 1, true, false);
            SourceDataLine line = null;
            try {
                byte[] data = generatedCueBytes(key, volumePercent, format);
                line = AudioSystem.getSourceDataLine(format);
                line.open(format);
                line.start();
                line.write(data, 0, data.length);
                line.drain();
            } catch (Throwable t) {
                DebugLog.warn("AUDIO_PLAY", "Generated cue unavailable for " + safeKey + ": " + t.getMessage());
            } finally {
                if (line != null) {
                    try { line.stop(); } catch (Throwable ignored) {}
                    try { line.close(); } catch (Throwable ignored) {}
                }
            }
        }, "generated-sound-" + safeKey).start();
    }

    byte[] generatedCueBytes(String key, int volumePercent, AudioFormat format) {
        if (startupCueSuppressed(key)) return new byte[0];
        String k = key == null ? "" : key.toLowerCase(Locale.ROOT);
        int sampleRate = Math.max(8000, (int)format.getSampleRate());
        int ms = k.contains("ambient") ? 300 : (k.contains("weapon") ? 180 : (k.contains("type") ? 55 : 95));
        int samples = Math.max(1, sampleRate * ms / 1000);
        byte[] data = new byte[samples * 2];
        double freq = generatedCueFrequency(k);
        double pct = Math.max(0.01, Math.min(1.0, volumePercent / 100.0));
        double gain = 0.16 * pct;
        for (int i = 0; i < samples; i++) {
            double t = i / (double) sampleRate;
            double attack = Math.min(1.0, i / Math.max(1.0, sampleRate * 0.012));
            double release = Math.min(1.0, (samples - i) / Math.max(1.0, sampleRate * 0.045));
            double env = attack * release;
            double wave = Math.sin(2.0 * Math.PI * freq * t);
            if (k.contains("button") || k.contains("tab")) wave = 0.65 * wave + 0.35 * Math.sin(2.0 * Math.PI * (freq * 1.5) * t);
            if (k.contains("weapon") || k.contains("ambient")) wave = 0.55 * wave + 0.45 * Math.sin(2.0 * Math.PI * (freq * 0.5) * t);
            short value = (short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (int)(wave * env * gain * Short.MAX_VALUE)));
            data[i * 2] = (byte)(value & 0xFF);
            data[i * 2 + 1] = (byte)((value >>> 8) & 0xFF);
        }
        return data;
    }

    void setClipGain(Clip clip, int volumePercent) {
        try {
            if (clip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
                float pct = Math.max(0.01f, Math.min(1.0f, volumePercent / 100f));
                float db = (float)(20.0 * Math.log10(pct));
                db = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db));
                gain.setValue(db);
            }
        } catch (Throwable ignored) {}
    }

    double generatedCueFrequency(String key) {
        if (key == null) return 440.0;
        if (key.contains("panelopen")) return 330.0;
        if (key.contains("panelclose")) return 196.0;
        if (key.contains("tab")) return 520.0;
        if (key.contains("type")) return 760.0;
        if (key.contains("ambient")) return 120.0;
        if (key.contains("weapon_las") || key.contains("lightning")) return 880.0;
        if (key.contains("weapon")) return 160.0;
        return 440.0;
    }

    String weaponEffectKey(String weapon, boolean ranged) {
        String w = weapon == null ? "" : weapon.toLowerCase(Locale.ROOT);
        if (w.contains("flamer") || w.contains("flame") || w.contains("melta") || w.contains("inferno") || w.contains("promethium")) return "weapon_flame";
        if (w.contains("plasma")) return "weapon_plasma";
        if (w.contains("las") || w.contains("laser")) return "weapon_las";
        if (w.contains("arc") || w.contains("shock") || w.contains("lightning") || w.contains("electr") || w.contains("voltaic")) return "weapon_lightning";
        if (w.contains("thunder") || w.contains("heavy bolter") || w.contains("storm bolter") || w.contains("autocannon")) return "weapon_thundering";
        if (w.contains("chain") || w.contains("saw") || w.contains("sword") || w.contains("knife") || w.contains("blade") || w.contains("axe") || w.contains("dagger") || w.contains("claw") || w.contains("talon") || w.contains("cutter")) return "weapon_slice";
        if (ranged || w.contains("pistol") || w.contains("gun") || w.contains("rifle") || w.contains("bolter") || w.contains("stub") || w.contains("shot") || w.contains("revolver") || w.contains("stubber") || w.contains("webber") || w.contains("needle")) return "weapon_shot";
        return "weapon_bash";
    }
    void requestMusic(String key, GameOptions opt) {
        if (music.playlistSummary().isBlank()) {
            if (!missingMusicPackAudited) {
                DebugLog.audit("MUSIC", "No dynamic music playlist is packaged; music requests remain silent until assets/music/wav or an audiopack is installed.");
                missingMusicPackAudited = true;
            }
            return;
        }
        music.request(key, opt);
    }
    void setMusicVolume(GameOptions opt) { music.setVolume(opt); }
    void stopMusic() { music.stopAll("manual stop"); }
}



class DynamicMusicManager {
    static final String SILENCE = "SILENCE";
    final Map<String, ArrayList<File>> playlists = new HashMap<>();
    final Random rng = new Random();
    Clip currentClip = null;
    String currentKey = SILENCE;
    String requestedKey = SILENCE;
    File currentFile = null;
    boolean transitioning = false;

    void load(File root) { load(root, null); }

    void load(File root, File manifest) {
        playlists.clear();
        if (root == null || !root.exists()) { DebugLog.warn("MUSIC", "Music directory missing: " + root); return; }
        int manifestRows = loadManifest(root, manifest);
        File[] files = root.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".wav"));
        if (files == null) return;
        Arrays.sort(files, Comparator.comparing(File::getName));
        if (manifestRows <= 0) for (File f : files) register(f);
        DebugLog.audit("MUSIC", "Loaded dynamic music playlists: " + playlistSummary() + "; manifestRows=" + manifestRows + "; looseFileFallback=" + (manifestRows <= 0));
    }

    int loadManifest(File root, File manifest) {
        File m = manifest;
        if (m == null && root != null && root.getParentFile() != null) m = new File(root.getParentFile(), "music_manifest.tsv");
        if (m == null || !m.exists()) return 0;
        int loaded = 0;
        try {
            List<String> lines = Files.readAllLines(m.toPath());
            for (String line : lines) {
                String trimmed = line == null ? "" : line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] parts = line.split("\\t");
                if (parts.length < 2) continue;
                String key = parts[0].trim();
                String rel = parts[1].trim();
                if (key.isEmpty() || rel.isEmpty()) continue;
                File f = root.toPath().resolve(rel).normalize().toFile();
                if (!f.exists()) {
                    DebugLog.warn("MUSIC", "Manifest music file missing key=" + key + " file=" + rel);
                    continue;
                }
                add(key, f);
                loaded++;
            }
            DebugLog.audit("MUSIC", "Loaded music manifest rows=" + loaded + " manifest=" + m.getPath());
        } catch (Throwable t) {
            DebugLog.error("MUSIC", "Failed to load music manifest " + m, t);
            return 0;
        }
        return loaded;
    }

    void register(File f) {
        String n = f.getName().toLowerCase(Locale.ROOT);
        if (n.contains("mainmenu")) add("MAIN_MENU", f);
        else if (n.contains("characterselect")) add("CHARACTER_SELECT", f);
        else if (n.contains("estate_battle") || n.contains("palace_of_gold") || n.contains("palace of gold")) add("ESTATE_BATTLE", f);
        else if (n.contains("estate_palace") || n.contains("rust crown") || n.contains("rust_crown")) add("ESTATE_PALACE", f);
        else if (n.contains("combat")) add("COMBAT", f);
        else if (n.contains("gang")) add("GANG_TURF", f);
        else if (n.contains("publicorder") || n.contains("public_order") || n.contains("ferrocrete")) add("PUBLIC_ORDER", f);
        else if (n.contains("triage")) add("TRIAGE", f);
        else if (n.contains("proscribed") || n.contains("mechanical_moon") || n.contains("mechanical moon")) add("PROSCRIBED_CELL", f);
        else if (n.contains("engine") || n.contains("machinery")) add("ENGINE_HOUSE", f);
        else if (n.contains("outcast") || n.contains("sump_dark_choir") || n.contains("sump dark choir")) add("OUTCAST", f);
        else if (n.contains("train") || n.contains("false_seal") || n.contains("false seal")) add("TRAIN", f);
        else if (n.contains("charter") || n.contains("ration_line") || n.contains("ration line")) add("CHARTER_GUARD", f);
        else if (n.contains("records") || n.contains("seal_of_ash") || n.contains("seal of ash")) add("CIVIC_RECORDS", f);
        else if (n.contains("estate") || n.contains("velvet")) add("ESTATE", f);
        else add("LOW_HABITATION", f);
    }

    void add(String key, File f) {
        if (key == null || key.isBlank() || f == null) return;
        ArrayList<File> list = playlists.computeIfAbsent(key.trim(), k -> new ArrayList<>());
        if (!list.contains(f)) list.add(f);
    }

    String playlistSummary() {
        ArrayList<String> keys = new ArrayList<>(playlists.keySet());
        Collections.sort(keys);
        ArrayList<String> parts = new ArrayList<>();
        for (String k : keys) parts.add(k + "=" + playlists.get(k).size());
        return String.join(",", parts);
    }

    synchronized void request(String key, GameOptions opt) {
        requestedKey = (key == null || key.isBlank()) ? SILENCE : key;
        if (opt == null || !opt.musicEnabled || opt.musicVolume <= 0 || SILENCE.equals(requestedKey)) {
            if (currentClip != null || !SILENCE.equals(currentKey)) stopAll("disabled or silence requested");
            return;
        }
        if (requestedKey.equals(currentKey) && currentClip != null && currentClip.isOpen()) { setVolume(opt); return; }
        if (transitioning) return;
        transitioning = true;
        String target = requestedKey;
        int vol = opt.musicVolume;
        new Thread(() -> transitionTo(target, vol), "music-transition-" + target).start();
    }

    File choose(String key) {
        ArrayList<File> list = playlists.get(key);
        if ((list == null || list.isEmpty()) && !"LOW_HABITATION".equals(key)) list = playlists.get("LOW_HABITATION");
        if ((list == null || list.isEmpty()) && !"MAIN_MENU".equals(key)) list = playlists.get("MAIN_MENU");
        if (list == null || list.isEmpty()) return null;
        return list.get(rng.nextInt(list.size()));
    }

    void transitionTo(String key, int volume) {
        File f;
        synchronized (this) { f = choose(key); }
        if (f == null) { DebugLog.warn("MUSIC", "No track available for key=" + key + " playlists=" + playlistSummary()); synchronized(this){ transitioning=false; currentKey=SILENCE; } return; }
        Clip old;
        synchronized (this) { old = currentClip; currentClip = null; currentKey = key; currentFile = f; }
        if (old != null) fadeClose(old, 2200);
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(f)) {
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            setGain(clip, 0.001f);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
            synchronized (this) { currentClip = clip; currentKey = key; currentFile = f; transitioning = false; }
            DebugLog.audit("MUSIC", "Now playing key=" + key + " file=" + f.getName());
            fadeTo(clip, Math.max(0.001f, Math.min(1f, volume/100f)), 2600);
        } catch (Throwable t) {
            DebugLog.error("MUSIC_PLAY", "Failed to play dynamic music key=" + key + " file=" + f, t);
            synchronized (this) { transitioning = false; currentKey = SILENCE; currentClip = null; }
        }
    }

    synchronized void setVolume(GameOptions opt) {
        if (opt == null || currentClip == null || !currentClip.isOpen()) return;
        setGain(currentClip, Math.max(0.001f, Math.min(1f, opt.musicVolume/100f)));
    }

    synchronized void stopAll(String reason) {
        requestedKey = SILENCE;
        currentKey = SILENCE;
        Clip old = currentClip;
        currentClip = null;
        currentFile = null;
        if (old != null) fadeClose(old, 1200);
        DebugLog.audit("MUSIC", "Stopped dynamic music: " + reason);
    }

    void fadeClose(Clip clip, int ms) {
        new Thread(() -> { fadeTo(clip, 0.001f, ms); try { clip.stop(); clip.close(); } catch(Throwable ignored){} }, "music-fade-close").start();
    }

    void fadeTo(Clip clip, float targetPct, int ms) {
        if (clip == null) return;
        int steps = Math.max(4, ms / 80);
        float start = currentPctGuess(clip);
        for (int i=1; i<=steps; i++) {
            float t = i / (float)steps;
            float pct = start + (targetPct - start) * t;
            setGain(clip, pct);
            try { Thread.sleep(Math.max(20, ms / steps)); } catch (InterruptedException ignored) { return; }
        }
        setGain(clip, targetPct);
    }

    float currentPctGuess(Clip clip) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
                return (float)Math.pow(10.0, gain.getValue()/20.0);
            }
        } catch(Throwable ignored) {}
        return 1.0f;
    }

    void setGain(Clip clip, float pct) {
        try {
            if (clip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
                float clamped = Math.max(0.001f, Math.min(1.0f, pct));
                float db = (float)(20.0 * Math.log10(clamped));
                db = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db));
                gain.setValue(db);
            }
        } catch(Throwable ignored) {}
    }
}
