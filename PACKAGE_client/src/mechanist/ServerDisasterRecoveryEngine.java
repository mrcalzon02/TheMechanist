package mechanist;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** Global uncaught-exception panic handler and emergency save/dump engine for headless server deployments. */
final class ServerDisasterRecoveryEngine implements Thread.UncaughtExceptionHandler, AutoCloseable {
    interface NetworkPauseController { void pauseNetwork(String reason); }
    interface SessionSnapshotProvider { List<EmergencySessionSnapshot> activeSessions(); }

    private final Path dumpDirectory;
    private final CharacterSaveManager characterSaveManager;
    private final NetworkPauseController networkPauseController;
    private final SessionSnapshotProvider sessionProvider;
    private final Supplier<String> modFingerprintSupplier;
    private final Thread.UncaughtExceptionHandler previous;
    private final AtomicBoolean panicInProgress = new AtomicBoolean(false);
    private final CopyOnWriteArrayList<CrashDumpRecord> recentDumps = new CopyOnWriteArrayList<>();

    ServerDisasterRecoveryEngine(Path dumpDirectory,
                                 CharacterSaveManager characterSaveManager,
                                 NetworkPauseController networkPauseController,
                                 SessionSnapshotProvider sessionProvider,
                                 Supplier<String> modFingerprintSupplier) throws IOException {
        this.dumpDirectory = Objects.requireNonNull(dumpDirectory, "dumpDirectory").toAbsolutePath().normalize();
        Files.createDirectories(this.dumpDirectory);
        this.characterSaveManager = Objects.requireNonNull(characterSaveManager, "characterSaveManager");
        this.networkPauseController = networkPauseController == null ? reason -> DebugLog.warn("DISASTER_NETWORK_PAUSE", reason) : networkPauseController;
        this.sessionProvider = sessionProvider == null ? List::of : sessionProvider;
        this.modFingerprintSupplier = modFingerprintSupplier == null ? () -> "unknown" : modFingerprintSupplier;
        this.previous = Thread.getDefaultUncaughtExceptionHandler();
    }

    void install() {
        Thread.setDefaultUncaughtExceptionHandler(this);
        DebugLog.audit("DISASTER_RECOVERY", "installed dumpDir=" + dumpDirectory);
    }

    @Override public void uncaughtException(Thread thread, Throwable throwable) {
        handleCrash(thread, throwable);
        if (previous != null && previous != this) previous.uncaughtException(thread, throwable);
    }

    CrashDumpRecord handleCrash(Thread thread, Throwable throwable) {
        if (!panicInProgress.compareAndSet(false, true)) {
            return new CrashDumpRecord(Instant.now().toString(), thread == null ? "unknown" : thread.getName(), classify(thread), throwable == null ? "unknown" : throwable.getClass().getName(), "panic already in progress", List.of(), List.of(), modFingerprintSupplier.get());
        }
        try {
            networkPauseController.pauseNetwork("unhandled crash in " + (thread == null ? "unknown" : thread.getName()));
            ArrayList<EmergencySessionSnapshot> sessions = new ArrayList<>();
            try { sessions.addAll(sessionProvider.activeSessions()); } catch (RuntimeException ex) { DebugLog.error("DISASTER_SESSION_ENUM", "Could not enumerate active sessions.", ex); }
            ArrayList<String> saveResults = new ArrayList<>();
            for (EmergencySessionSnapshot session : sessions) {
                try {
                    characterSaveManager.atomicSaveSync(session.characterState());
                    saveResults.add(session.identityKey() + ":saved");
                } catch (IOException | RuntimeException ex) {
                    saveResults.add(session.identityKey() + ":save-failed:" + ex.getClass().getSimpleName());
                    DebugLog.error("DISASTER_CHARACTER_SAVE", "Emergency save failed for " + session.identityKey(), ex);
                }
            }
            CrashDumpRecord dump = new CrashDumpRecord(Instant.now().toString(), thread == null ? "unknown" : thread.getName(), classify(thread), throwable == null ? "unknown" : throwable.getClass().getName(), stackTrace(throwable), threadMap(), saveResults, modFingerprintSupplier.get());
            writeCrashDump(dump);
            recentDumps.add(dump);
            while (recentDumps.size() > 8) recentDumps.remove(0);
            return dump;
        } finally {
            panicInProgress.set(false);
        }
    }

    List<CrashDumpRecord> recentDumps() { return List.copyOf(recentDumps); }

    private void writeCrashDump(CrashDumpRecord dump) {
        Path finalPath = dumpDirectory.resolve("crash_dump.json");
        Path timestamped = dumpDirectory.resolve("crash_dump_" + dump.createdAtIso().replace(':', '-') + ".json");
        Path tmp = dumpDirectory.resolve("crash_dump.tmp");
        byte[] bytes = dump.toJson().getBytes(StandardCharsets.UTF_8);
        try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            channel.write(ByteBuffer.wrap(bytes));
            channel.force(true);
        } catch (IOException ex) {
            DebugLog.error("DISASTER_DUMP_WRITE", "Could not write crash dump tmp file.", ex);
            return;
        }
        try {
            Files.move(tmp, finalPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            try { Files.move(tmp, finalPath, StandardCopyOption.REPLACE_EXISTING); }
            catch (IOException second) { DebugLog.error("DISASTER_DUMP_MOVE", "Could not publish crash dump.", second); }
        }
        try { Files.copy(finalPath, timestamped, StandardCopyOption.REPLACE_EXISTING); }
        catch (IOException ex) { DebugLog.warn("DISASTER_DUMP_COPY", "Could not write timestamped dump: " + ex.getMessage()); }
    }

    private static String classify(Thread thread) {
        if (thread == null) return "unknown";
        Thread.State state = thread.getState();
        return switch (state) {
            case NEW -> "new";
            case RUNNABLE -> "runnable";
            case BLOCKED -> "blocked";
            case WAITING -> "waiting";
            case TIMED_WAITING -> "timed_waiting";
            case TERMINATED -> "terminated";
        };
    }

    private static String stackTrace(Throwable throwable) {
        if (throwable == null) return "";
        try {
            StringWriter sw = new StringWriter(4096);
            throwable.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        } catch (RuntimeException ex) {
            return throwable.getClass().getName() + ": " + throwable.getMessage();
        }
    }

    private static List<String> threadMap() {
        ArrayList<String> out = new ArrayList<>();
        for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
            Thread t = e.getKey();
            out.add(t.getName() + "|" + t.getState() + "|daemon=" + t.isDaemon() + "|priority=" + t.getPriority());
            if (out.size() >= 256) break;
        }
        return List.copyOf(out);
    }

    @Override public void close() {
        if (Thread.getDefaultUncaughtExceptionHandler() == this) Thread.setDefaultUncaughtExceptionHandler(previous);
    }
}

record EmergencySessionSnapshot(String identityKey, CharacterStateRecord characterState) {
    EmergencySessionSnapshot { if (identityKey == null || identityKey.isBlank()) throw new IllegalArgumentException("identityKey required"); characterState = Objects.requireNonNull(characterState, "characterState"); }
}

record CrashDumpRecord(String createdAtIso, String threadName, String threadState, String throwableClass, String stackTrace, List<String> threadOwnershipMap, List<String> emergencySaveResults, String activeModManifestFingerprint) {
    CrashDumpRecord {
        threadOwnershipMap = List.copyOf(Objects.requireNonNullElse(threadOwnershipMap, List.of()));
        emergencySaveResults = List.copyOf(Objects.requireNonNullElse(emergencySaveResults, List.of()));
        activeModManifestFingerprint = activeModManifestFingerprint == null ? "unknown" : activeModManifestFingerprint;
    }
    String toJson() {
        return "{\n"
                + "  \"createdAtIso\": " + AdminSecurityLogger.quote(createdAtIso) + ",\n"
                + "  \"threadName\": " + AdminSecurityLogger.quote(threadName) + ",\n"
                + "  \"threadState\": " + AdminSecurityLogger.quote(threadState) + ",\n"
                + "  \"throwableClass\": " + AdminSecurityLogger.quote(throwableClass) + ",\n"
                + "  \"activeModManifestFingerprint\": " + AdminSecurityLogger.quote(activeModManifestFingerprint) + ",\n"
                + "  \"emergencySaveResults\": " + array(emergencySaveResults) + ",\n"
                + "  \"threadOwnershipMap\": " + array(threadOwnershipMap) + ",\n"
                + "  \"stackTrace\": " + AdminSecurityLogger.quote(stackTrace) + "\n"
                + "}\n";
    }
    private static String array(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) { if (i > 0) sb.append(','); sb.append(AdminSecurityLogger.quote(values.get(i))); }
        return sb.append(']').toString();
    }
}
