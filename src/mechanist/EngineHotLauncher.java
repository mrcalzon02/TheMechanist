package mechanist;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client-side in-memory hot launcher used after asset acquisition.
 * It never owns or closes the live network connection; it only purges local runtime
 * registries, validates the user mods directory, swaps in a fresh SecureModClassLoader,
 * and reports completion through the supplied persistent connection callback.
 */
final class EngineHotLauncher implements AutoCloseable {
    interface RuntimeRegistry extends Closeable {
        String registryName();
        int estimatedEntryCount();
        void purge() throws IOException;
        @Override default void close() throws IOException { purge(); }
    }

    interface PersistentConnection {
        String connectionId();
        boolean isOpen();
        void sendFrame(HandshakeNetworkFrame frame) throws IOException;
    }

    record HotRestartRequest(String sessionId, Path modsDirectory, SecureHandshakeStateMachine.ModManifestRecord manifest, PersistentConnection connection) {
        HotRestartRequest {
            if (sessionId == null || sessionId.isBlank()) sessionId = UUID.randomUUID().toString();
            modsDirectory = Objects.requireNonNull(modsDirectory, "modsDirectory").normalize().toAbsolutePath();
            Objects.requireNonNull(connection, "connection");
        }
    }

    sealed interface HotRestartResult permits HotRestartResult.Success, HotRestartResult.Failure {
        String sessionId();
        Instant completedAt();
        record Success(String sessionId, SecureModClassLoader classLoader, String mountedFingerprint, List<Path> mountedJars, List<String> purgeLog, Instant completedAt) implements HotRestartResult {
            public Success {
                Objects.requireNonNull(classLoader, "classLoader");
                mountedFingerprint = mountedFingerprint == null ? "" : mountedFingerprint;
                mountedJars = List.copyOf(Objects.requireNonNullElse(mountedJars, List.of()));
                purgeLog = List.copyOf(Objects.requireNonNullElse(purgeLog, List.of()));
                completedAt = completedAt == null ? Instant.now() : completedAt;
            }
        }
        record Failure(String sessionId, String reason, List<String> purgeLog, Instant completedAt) implements HotRestartResult {
            public Failure {
                reason = reason == null || reason.isBlank() ? "unknown hot restart failure" : reason;
                purgeLog = List.copyOf(Objects.requireNonNullElse(purgeLog, List.of()));
                completedAt = completedAt == null ? Instant.now() : completedAt;
            }
        }
    }

    private final ExecutorService worker;
    private final List<RuntimeRegistry> registries = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final AtomicReference<SecureModClassLoader> activeModClassLoader = new AtomicReference<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    EngineHotLauncher() {
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "mechanist-client-hot-restart");
            t.setDaemon(true);
            return t;
        };
        this.worker = Executors.newSingleThreadExecutor(factory);
    }

    void registerRegistry(RuntimeRegistry registry) {
        registries.add(Objects.requireNonNull(registry, "registry"));
    }

    SecureModClassLoader activeModClassLoader() { return activeModClassLoader.get(); }

    CompletableFuture<HotRestartResult> launchAsync(HotRestartRequest request) {
        Objects.requireNonNull(request, "request");
        if (closed.get()) return CompletableFuture.completedFuture(new HotRestartResult.Failure(request.sessionId(), "hot launcher is closed", List.of(), Instant.now()));
        return CompletableFuture.supplyAsync(() -> executeRestart(request), worker);
    }

    private HotRestartResult executeRestart(HotRestartRequest request) {
        List<String> purgeLog = new ArrayList<>();
        try {
            if (!request.connection().isOpen()) {
                return new HotRestartResult.Failure(request.sessionId(), "persistent network connection is already closed before hot restart", purgeLog, Instant.now());
            }
            request.connection().sendFrame(new HandshakeNetworkFrame.HotRestartStarted(request.sessionId(), Instant.now().toEpochMilli()));
            purgeLocalRuntimeRegistries(purgeLog);
            List<Path> jars = discoverModJars(request.modsDirectory());
            List<URL> urls = new ArrayList<>();
            StringBuilder fingerprintMaterial = new StringBuilder(request.manifest() == null ? "no-manifest" : request.manifest().stableFingerprint());
            for (Path jar : jars) {
                SecureModClassLoader.ModVerificationResult verification = SecureModClassLoader.verifyJar(jar);
                if (verification instanceof SecureModClassLoader.ModVerificationResult.Rejected rejected) {
                    return new HotRestartResult.Failure(request.sessionId(), "mod verification failed for " + jar.getFileName() + ": " + rejected.violations(), purgeLog, Instant.now());
                }
                if (verification instanceof SecureModClassLoader.ModVerificationResult.Accepted accepted) {
                    urls.add(jar.toUri().toURL());
                    fingerprintMaterial.append('|').append(jar.getFileName()).append('=').append(accepted.sha256()).append(':').append(accepted.totalBytes());
                }
            }
            SecureModClassLoader nextLoader = new SecureModClassLoader(urls.toArray(URL[]::new), EngineHotLauncher.class.getClassLoader());
            SecureModClassLoader previous = activeModClassLoader.getAndSet(nextLoader);
            if (previous != null) {
                try { previous.close(); } catch (IOException ex) { purgeLog.add("previous loader close warning: " + ex.getMessage()); }
            }
            System.gc();
            String mountedFingerprint = SecureHandshakeStateMachine.sha256Hex(fingerprintMaterial.toString().getBytes(StandardCharsets.UTF_8));
            request.connection().sendFrame(new HandshakeNetworkFrame.HotRestartComplete(request.sessionId(), mountedFingerprint, jars.stream().map(Path::getFileName).map(Path::toString).toList(), Instant.now().toEpochMilli()));
            return new HotRestartResult.Success(request.sessionId(), nextLoader, mountedFingerprint, jars, purgeLog, Instant.now());
        } catch (IOException | RuntimeException ex) {
            try {
                if (request.connection().isOpen()) request.connection().sendFrame(new HandshakeNetworkFrame.Rejected(request.sessionId(), "CLIENT_HOT_RESTART", ex.getMessage(), Instant.now().toEpochMilli()));
            } catch (IOException ignored) { }
            return new HotRestartResult.Failure(request.sessionId(), ex.getClass().getSimpleName() + ": " + ex.getMessage(), purgeLog, Instant.now());
        }
    }

    private void purgeLocalRuntimeRegistries(List<String> purgeLog) throws IOException {
        for (RuntimeRegistry registry : registries) {
            int before = Math.max(0, registry.estimatedEntryCount());
            registry.purge();
            purgeLog.add(registry.registryName() + " purged entries=" + before);
        }
    }

    static List<Path> discoverModJars(Path modsDirectory) throws IOException {
        Path root = Objects.requireNonNull(modsDirectory, "modsDirectory").normalize().toAbsolutePath();
        if (!Files.exists(root)) Files.createDirectories(root);
        if (!Files.isDirectory(root)) throw new IOException("mods path is not a directory: " + root);
        try (var stream = Files.list(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".jar"))
                    .map(p -> p.normalize().toAbsolutePath())
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    @Override public void close() {
        if (!closed.getAndSet(true)) {
            SecureModClassLoader loader = activeModClassLoader.getAndSet(null);
            if (loader != null) {
                try { loader.close(); } catch (IOException ignored) { }
            }
            worker.shutdownNow();
        }
    }
}
