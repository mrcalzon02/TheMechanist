package mechanist;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;

/**
 * Owns restart/runtime JVM profile configuration without requiring external libraries.
 * The active process can report the selected profile and compile child-process flags;
 * changing JVM flags still requires an explicit restart through the launcher/runtime path.
 */
final class JvmRuntimeProfileAuthority {
    static final String VERSION = "0.9.10im";

    enum ExecutionMode { CLIENT_GRAPHICS, SERVER_HEADLESS }
    enum RuntimeTarget { LAUNCHER_MENU, CLIENT_GRAPHICS, CLIENT_THIN, SINGLE_PLAYER_COMBINED, SINGLE_PLAYER_COMBINED_HEAVY, SERVER_HEADLESS }
    enum Java2dPipelineProfile { AUTO, OS_ACCELERATED, SOFTWARE_SAFE }
    enum GarbageCollectorProfile {
        Z_GC("-XX:+UseZGC", "ZGC smooth client"),
        G1_GC("-XX:+UseG1GC", "G1 balanced"),
        PARALLEL_GC("-XX:+UseParallelGC", "Parallel server throughput");
        final String flag;
        final String label;
        GarbageCollectorProfile(String flag, String label) { this.flag = flag; this.label = label; }
    }

    static final class RuntimeConfig {
        ExecutionMode mode = ExecutionMode.CLIENT_GRAPHICS;
        RuntimeTarget runtimeTarget = RuntimeTarget.CLIENT_GRAPHICS;
        Java2dPipelineProfile pipeline = Java2dPipelineProfile.OS_ACCELERATED;
        int initialRamMb = 1024;
        int maxRamMb = 4096;
        GarbageCollectorProfile gc = GarbageCollectorProfile.Z_GC;
        boolean stringDeduplication = true;
        boolean transparentAcceleration = true;
        boolean forceVolatileVram = true;
        boolean disableVectorAntialiasing = false;
        String manualOverrides = "";

        void applyLauncherProfile() {
            runtimeTarget = RuntimeTarget.LAUNCHER_MENU;
            mode = ExecutionMode.CLIENT_GRAPHICS;
            initialRamMb = 512;
            maxRamMb = 2048;
            gc = GarbageCollectorProfile.G1_GC;
            stringDeduplication = true;
            pipeline = Java2dPipelineProfile.AUTO;
            transparentAcceleration = true;
            forceVolatileVram = true;
            disableVectorAntialiasing = false;
        }

        void applyClientGraphicsProfile() {
            runtimeTarget = RuntimeTarget.CLIENT_GRAPHICS;
            mode = ExecutionMode.CLIENT_GRAPHICS;
            initialRamMb = 1024;
            maxRamMb = 4096;
            gc = GarbageCollectorProfile.Z_GC;
            stringDeduplication = true;
            pipeline = Java2dPipelineProfile.OS_ACCELERATED;
            transparentAcceleration = true;
            forceVolatileVram = true;
            disableVectorAntialiasing = false;
        }

        void applyThinClientProfile() {
            runtimeTarget = RuntimeTarget.CLIENT_THIN;
            mode = ExecutionMode.CLIENT_GRAPHICS;
            initialRamMb = 512;
            maxRamMb = 1536;
            gc = GarbageCollectorProfile.G1_GC;
            stringDeduplication = true;
            pipeline = Java2dPipelineProfile.AUTO;
            transparentAcceleration = true;
            forceVolatileVram = true;
            disableVectorAntialiasing = false;
        }

        void applySinglePlayerCombinedProfile() {
            runtimeTarget = RuntimeTarget.SINGLE_PLAYER_COMBINED;
            mode = ExecutionMode.CLIENT_GRAPHICS;
            initialRamMb = 2048;
            maxRamMb = 6144;
            gc = GarbageCollectorProfile.Z_GC;
            stringDeduplication = true;
            pipeline = Java2dPipelineProfile.OS_ACCELERATED;
            transparentAcceleration = true;
            forceVolatileVram = true;
            disableVectorAntialiasing = false;
        }

        void applySinglePlayerCombinedHeavyProfile() {
            runtimeTarget = RuntimeTarget.SINGLE_PLAYER_COMBINED_HEAVY;
            mode = ExecutionMode.CLIENT_GRAPHICS;
            initialRamMb = 4096;
            maxRamMb = 12288;
            gc = GarbageCollectorProfile.G1_GC;
            stringDeduplication = true;
            pipeline = Java2dPipelineProfile.OS_ACCELERATED;
            transparentAcceleration = true;
            forceVolatileVram = true;
            disableVectorAntialiasing = false;
        }

        void applyServerProfile() {
            runtimeTarget = RuntimeTarget.SERVER_HEADLESS;
            mode = ExecutionMode.SERVER_HEADLESS;
            initialRamMb = 2048;
            maxRamMb = 8192;
            gc = GarbageCollectorProfile.PARALLEL_GC;
            stringDeduplication = true;
            pipeline = Java2dPipelineProfile.SOFTWARE_SAFE;
            transparentAcceleration = false;
            forceVolatileVram = false;
            disableVectorAntialiasing = true;
        }

        String targetLabel() {
            switch (runtimeTarget) {
                case LAUNCHER_MENU: return "Launcher/Main Menu";
                case CLIENT_THIN: return "Thin Network Client";
                case SINGLE_PLAYER_COMBINED: return "Single Player Combined";
                case SINGLE_PLAYER_COMBINED_HEAVY: return "Single Player Combined Heavy";
                case SERVER_HEADLESS: return "Headless Server";
                default: return "Client Graphics";
            }
        }

        String pipelineLabel() {
            switch (pipeline) {
                case OS_ACCELERATED: return "OS accelerated Java2D";
                case SOFTWARE_SAFE: return "Software safe Java2D";
                default: return "Automatic Java2D";
            }
        }

        RuntimeConfig copy() {
            RuntimeConfig c = new RuntimeConfig();
            c.mode = mode; c.runtimeTarget = runtimeTarget; c.pipeline = pipeline;
            c.initialRamMb = initialRamMb; c.maxRamMb = maxRamMb; c.gc = gc;
            c.stringDeduplication = stringDeduplication; c.transparentAcceleration = transparentAcceleration;
            c.forceVolatileVram = forceVolatileVram; c.disableVectorAntialiasing = disableVectorAntialiasing;
            c.manualOverrides = manualOverrides;
            return c;
        }

        java.util.List<String> buildJvmArgs() { return buildJvmArgsFor(osName()); }

        java.util.List<String> buildJvmArgsFor(String osName) {
            ArrayList<String> args = new ArrayList<>();
            args.add("-Xms" + Math.max(256, initialRamMb) + "M");
            args.add("-Xmx" + Math.max(Math.max(256, initialRamMb), maxRamMb) + "M");
            args.add(gc.flag);
            if (stringDeduplication && (gc == GarbageCollectorProfile.Z_GC || gc == GarbageCollectorProfile.G1_GC)) args.add("-XX:+UseStringDeduplication");
            if (mode == ExecutionMode.SERVER_HEADLESS || runtimeTarget == RuntimeTarget.SERVER_HEADLESS) args.add("-Djava.awt.headless=true");
            args.addAll(java2dArgsFor(this, osName));
            if (manualOverrides != null && !manualOverrides.isBlank()) {
                for (String s : manualOverrides.trim().split("\\s+")) if (!s.isBlank()) args.add(s.trim());
            }
            return args;
        }
    }

    private static final Path PATH = Paths.get("settings", "jvm_runtime.properties");

    static RuntimeConfig load() {
        RuntimeConfig c = new RuntimeConfig();
        if (!Files.exists(PATH)) return c;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(PATH)) {
            p.load(in);
            c.mode = parseEnum(ExecutionMode.class, p.getProperty("mode"), c.mode);
            c.runtimeTarget = parseEnum(RuntimeTarget.class, p.getProperty("runtimeTarget"), c.runtimeTarget);
            c.pipeline = parseEnum(Java2dPipelineProfile.class, p.getProperty("pipeline"), c.pipeline);
            c.initialRamMb = parseInt(p.getProperty("initialRamMb"), c.initialRamMb, 256, 262144);
            c.maxRamMb = parseInt(p.getProperty("maxRamMb"), c.maxRamMb, 256, 262144);
            c.gc = parseEnum(GarbageCollectorProfile.class, p.getProperty("gc"), c.gc);
            c.stringDeduplication = Boolean.parseBoolean(p.getProperty("stringDeduplication", String.valueOf(c.stringDeduplication)));
            c.transparentAcceleration = Boolean.parseBoolean(p.getProperty("transparentAcceleration", String.valueOf(c.transparentAcceleration)));
            c.forceVolatileVram = Boolean.parseBoolean(p.getProperty("forceVolatileVram", String.valueOf(c.forceVolatileVram)));
            c.disableVectorAntialiasing = Boolean.parseBoolean(p.getProperty("disableVectorAntialiasing", String.valueOf(c.disableVectorAntialiasing)));
            c.manualOverrides = p.getProperty("manualOverrides", "");
        } catch (Throwable t) {
            DebugLog.warn("JVM_RUNTIME_PROFILE", "Could not load settings/jvm_runtime.properties; using defaults.");
        }
        return c;
    }

    static void save(RuntimeConfig c) {
        if (c == null) return;
        try {
            Files.createDirectories(PATH.getParent());
            Properties p = new Properties();
            p.setProperty("mode", c.mode.name());
            p.setProperty("runtimeTarget", c.runtimeTarget.name());
            p.setProperty("pipeline", c.pipeline.name());
            p.setProperty("initialRamMb", String.valueOf(c.initialRamMb));
            p.setProperty("maxRamMb", String.valueOf(c.maxRamMb));
            p.setProperty("gc", c.gc.name());
            p.setProperty("stringDeduplication", String.valueOf(c.stringDeduplication));
            p.setProperty("transparentAcceleration", String.valueOf(c.transparentAcceleration));
            p.setProperty("forceVolatileVram", String.valueOf(c.forceVolatileVram));
            p.setProperty("disableVectorAntialiasing", String.valueOf(c.disableVectorAntialiasing));
            p.setProperty("manualOverrides", c.manualOverrides == null ? "" : c.manualOverrides.replace('\n', ' '));
            try (OutputStream out = Files.newOutputStream(PATH)) { p.store(out, "The Mechanist JVM runtime profile"); }
        } catch (Throwable t) {
            DebugLog.error("JVM_RUNTIME_PROFILE_SAVE", "Failed to save JVM runtime profile.", t);
        }
    }

    static String auditSummary(RuntimeConfig c) {
        RuntimeConfig r = c == null ? load() : c;
        return "jvmRuntimeProfile version=" + VERSION + " target=" + r.targetLabel() + " mode=" + r.mode + " xmsMb=" + r.initialRamMb + " xmxMb=" + r.maxRamMb
                + " gc=" + r.gc.label + " pipeline=" + r.pipelineLabel() + " stringDedup=" + r.stringDeduplication
                + " java2dArgs=" + java2dArgsFor(r, osName()) + " config=" + PATH
                + " activeArgs=" + ManagementFactory.getRuntimeMXBean().getInputArguments();
    }

    static java.util.List<String> infopediaLines(RuntimeConfig c) {
        RuntimeConfig r = c == null ? load() : c;
        ArrayList<String> lines = new ArrayList<>();
        lines.add("JVM runtime profile authority " + VERSION + ".");
        lines.add("Profile file: " + PATH + ".");
        lines.add("RESTART REQUIRED: heap, garbage collector, headless mode, and Java2D pipeline flags only fully apply after restart.");
        lines.add("Profile target: " + r.targetLabel() + "; execution mode: " + r.mode + "; selected GC: " + r.gc.label + ".");
        lines.add("Selected memory: -Xms" + r.initialRamMb + "M / -Xmx" + r.maxRamMb + "M.");
        lines.add("Java2D pipeline: " + r.pipelineLabel() + "; OS-specific flags: " + java2dArgsFor(r, osName()) + ".");
        lines.add("Launcher/client/server are treated as related runtime profiles. Thin client uses lower memory, single-player combined profiles reserve more heap because the local client and local host/server lane share one process, and headless server disables rendering commands.");
        lines.add("Compiled restart args: " + String.join(" ", r.buildJvmArgs()));
        lines.add("Active JVM args are reported separately because changing heap/GC requires restart.");
        return lines;
    }

    static RuntimeConfig effectiveServerProfile(RuntimeConfig base) {
        RuntimeConfig c = base == null ? new RuntimeConfig() : base.copy();
        c.applyServerProfile();
        return c;
    }

    static RuntimeConfig cycleTarget(RuntimeConfig c) {
        RuntimeConfig r = c == null ? new RuntimeConfig() : c;
        switch (r.runtimeTarget) {
            case CLIENT_GRAPHICS: r.applyThinClientProfile(); break;
            case CLIENT_THIN: r.applySinglePlayerCombinedProfile(); break;
            case SINGLE_PLAYER_COMBINED: r.applySinglePlayerCombinedHeavyProfile(); break;
            case SINGLE_PLAYER_COMBINED_HEAVY: r.applyLauncherProfile(); break;
            case LAUNCHER_MENU: r.applyServerProfile(); break;
            default: r.applyClientGraphicsProfile(); break;
        }
        return r;
    }

    static RuntimeConfig cycleGc(RuntimeConfig c) {
        RuntimeConfig r = c == null ? new RuntimeConfig() : c;
        GarbageCollectorProfile[] vals = GarbageCollectorProfile.values();
        r.gc = vals[(r.gc.ordinal() + 1) % vals.length];
        return r;
    }

    static RuntimeConfig cyclePipeline(RuntimeConfig c) {
        RuntimeConfig r = c == null ? new RuntimeConfig() : c;
        Java2dPipelineProfile[] vals = Java2dPipelineProfile.values();
        r.pipeline = vals[(r.pipeline.ordinal() + 1) % vals.length];
        return r;
    }

    static RuntimeConfig changeMemory(RuntimeConfig c, int deltaMb) {
        RuntimeConfig r = c == null ? new RuntimeConfig() : c;
        r.maxRamMb = Math.max(512, Math.min(32768, r.maxRamMb + deltaMb));
        r.initialRamMb = Math.max(256, Math.min(r.maxRamMb, Math.max(256, r.maxRamMb / 2)));
        return r;
    }

    static void configureEarlyJava2dProperties(RuntimeConfig c) {
        RuntimeConfig r = c == null ? load() : c;
        for (String arg : java2dArgsFor(r, osName())) {
            if (!arg.startsWith("-D")) continue;
            int eq = arg.indexOf('=');
            if (eq > 2) System.setProperty(arg.substring(2, eq), arg.substring(eq + 1));
        }
    }

    static java.util.List<String> java2dArgsFor(RuntimeConfig c, String osName) {
        ArrayList<String> args = new ArrayList<>();
        RuntimeConfig r = c == null ? new RuntimeConfig() : c;
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (r.mode == ExecutionMode.SERVER_HEADLESS || r.runtimeTarget == RuntimeTarget.SERVER_HEADLESS) {
            return args;
        }
        if (r.pipeline == Java2dPipelineProfile.SOFTWARE_SAFE) {
            args.add("-Dsun.java2d.opengl=false");
            args.add("-Dsun.java2d.d3d=false");
        } else if (r.pipeline == Java2dPipelineProfile.OS_ACCELERATED) {
            if (os.contains("win")) {
                args.add("-Dsun.java2d.d3d=true");
                args.add("-Dsun.java2d.ddforcevram=" + r.forceVolatileVram);
            } else if (os.contains("mac") || os.contains("darwin")) {
                args.add("-Dsun.java2d.metal=true");
                args.add("-Dsun.java2d.opengl=true");
            } else if (os.contains("linux") || os.contains("nix") || os.contains("nux")) {
                args.add("-Dsun.java2d.opengl=true");
            }
        }
        if (r.transparentAcceleration) args.add("-Dsun.java2d.transaccel=true");
        if (r.disableVectorAntialiasing) args.add("-Dsun.java2d.noaa=true");
        return args;
    }

    static String osName() { return System.getProperty("os.name", ""); }

    private static int parseInt(String s, int fallback, int min, int max) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(String.valueOf(s).trim()))); }
        catch (Throwable t) { return fallback; }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String s, E fallback) {
        try { return Enum.valueOf(type, String.valueOf(s).trim()); }
        catch (Throwable t) { return fallback; }
    }
}
