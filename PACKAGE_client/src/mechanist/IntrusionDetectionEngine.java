package mechanist;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runtime self-protection layer for mod execution spaces.  Java 17 cannot use
 * StackWalker to inspect arbitrary threads from the outside; StackWalker walks
 * the calling thread only.  This engine therefore samples stacks at instrumented
 * mod execution/class-loading boundaries, while background jobs verify class
 * resource drift and rolling exception frequency.
 */
final class IntrusionDetectionEngine implements AutoCloseable {
    private static final Set<String> FORBIDDEN_STACK_CLASS_FRAGMENTS = Set.of(
            "java.lang.reflect.",
            "java.lang.invoke.MethodHandles",
            "java.lang.ProcessBuilder",
            "java.lang.Runtime",
            "java.lang.System"
    );
    private static final Set<String> FORBIDDEN_STACK_METHOD_FRAGMENTS = Set.of(
            "setAccessible",
            "invoke",
            "defineClass",
            "getDeclaredField",
            "getDeclaredMethod",
            "loadLibrary",
            "exit"
    );

    private final AdminSecurityLogger logger;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService atomicLogExecutor;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentMap<String, MonitoredSandbox> sandboxes = new ConcurrentHashMap<>();
    private final SecurityExceptionWatchdog exceptionWatchdog;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    IntrusionDetectionEngine(AdminSecurityLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
        ThreadFactory schedulerFactory = r -> {
            Thread t = new Thread(r, "mechanist-intrusion-detector");
            t.setDaemon(true);
            return t;
        };
        ThreadFactory logFactory = r -> {
            Thread t = new Thread(r, "mechanist-intrusion-log-writer");
            t.setDaemon(true);
            return t;
        };
        this.scheduler = Executors.newSingleThreadScheduledExecutor(schedulerFactory);
        this.atomicLogExecutor = Executors.newSingleThreadExecutor(logFactory);
        this.exceptionWatchdog = new SecurityExceptionWatchdog(this::panic);
        scheduleNextDriftCheck();
    }

    void registerSandbox(String sandboxId, SecureModClassLoader loader, ThreadGroup threadGroup, LockdownTarget target) {
        Objects.requireNonNull(sandboxId, "sandboxId");
        Objects.requireNonNull(loader, "loader");
        sandboxes.put(sandboxId, new MonitoredSandbox(sandboxId, loader, threadGroup, target == null ? LockdownTarget.noop() : target, Instant.now()));
    }

    void unregisterSandbox(String sandboxId) { if (sandboxId != null) sandboxes.remove(sandboxId); }

    void runGuarded(String sandboxId, Runnable action) {
        Objects.requireNonNull(action, "action");
        observeCurrentThread(sandboxId, "before-mod-action");
        try {
            action.run();
            observeCurrentThread(sandboxId, "after-mod-action");
        } catch (SecurityException | NullPointerException | ArrayIndexOutOfBoundsException ex) {
            recordException(sandboxId, ex);
            throw ex;
        } catch (RuntimeException ex) {
            recordException(sandboxId, ex);
            throw ex;
        }
    }

    void observeCurrentThread(String sandboxId, String sampleReason) {
        MonitoredSandbox sandbox = sandboxes.get(sandboxId);
        ClassLoader expectedLoader = sandbox == null ? null : sandbox.loader();
        List<StackFrameSnapshot> frames = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(stream -> stream.limit(96).map(frame -> new StackFrameSnapshot(
                        frame.getDeclaringClass().getName(),
                        frame.getMethodName(),
                        String.valueOf(frame.getLineNumber()),
                        loaderId(frame.getDeclaringClass().getClassLoader()))).toList());
        boolean modFrameSeen = false;
        boolean forbiddenFrameSeen = false;
        for (StackFrameSnapshot frame : frames) {
            if (expectedLoader != null && frame.loaderId().equals(loaderId(expectedLoader))) modFrameSeen = true;
            for (String forbidden : FORBIDDEN_STACK_CLASS_FRAGMENTS) {
                if (frame.className().startsWith(forbidden) || frame.className().contains(forbidden)) forbiddenFrameSeen = true;
            }
            for (String forbiddenMethod : FORBIDDEN_STACK_METHOD_FRAGMENTS) {
                if (frame.methodName().equals(forbiddenMethod) || frame.methodName().contains(forbiddenMethod)) forbiddenFrameSeen = true;
            }
        }
        if (modFrameSeen && forbiddenFrameSeen) {
            panic(new IntrusionEvent(sandboxId, IntrusionType.REFLECTION_OR_PACKAGE_EVASION, sampleReason, frames, Map.of("thread", Thread.currentThread().getName()), Instant.now()));
        }
    }

    void recordException(String sandboxId, Throwable throwable) {
        exceptionWatchdog.record(sandboxId, throwable);
    }

    private void scheduleNextDriftCheck() {
        if (closed.get()) return;
        long delayMillis = 2_000L + random.nextInt(4_000);
        scheduler.schedule(() -> {
            try { verifyClassResourceDrift(); }
            catch (RuntimeException ex) { DebugLog.error("INTRUSION_DRIFT", "Drift verification loop failed.", ex); }
            finally { scheduleNextDriftCheck(); }
        }, delayMillis, TimeUnit.MILLISECONDS);
    }

    private void verifyClassResourceDrift() {
        for (MonitoredSandbox sandbox : sandboxes.values()) {
            for (var entry : sandbox.loader().approvedClassHashes().entrySet()) {
                String className = entry.getKey();
                String expected = entry.getValue();
                String resource = className.replace('.', '/') + ".class";
                try (InputStream in = sandbox.loader().getResourceAsStream(resource)) {
                    if (in == null) {
                        panic(new IntrusionEvent(sandbox.id(), IntrusionType.CODE_RESOURCE_MISSING, "approved class resource disappeared", List.of(), Map.of("className", className), Instant.now()));
                        continue;
                    }
                    String actual = ModPackageValidator.sha256Hex(in.readAllBytes());
                    if (!actual.equalsIgnoreCase(expected)) {
                        panic(new IntrusionEvent(sandbox.id(), IntrusionType.CODE_MEMORY_DRIFT, "approved class resource hash changed", List.of(), Map.of("className", className, "expected", expected, "actual", actual), Instant.now()));
                    }
                } catch (IOException ex) {
                    panic(new IntrusionEvent(sandbox.id(), IntrusionType.CODE_RESOURCE_READ_FAILURE, ex.getMessage(), List.of(), Map.of("className", className), Instant.now()));
                }
            }
        }
    }

    private void panic(IntrusionEvent event) {
        if (event == null) return;
        MonitoredSandbox sandbox = sandboxes.get(event.sandboxId());
        if (sandbox != null) {
            try { sandbox.target().freezePlayerSession(event.reason()); } catch (RuntimeException ex) { DebugLog.error("PANIC_FREEZE", "Could not freeze session for " + event.sandboxId(), ex); }
            try { sandbox.target().closeNetworkSession(event.reason()); } catch (RuntimeException ex) { DebugLog.error("PANIC_NETWORK", "Could not close session for " + event.sandboxId(), ex); }
            try { sandbox.target().quarantineMod(event.reason()); } catch (RuntimeException ex) { DebugLog.error("PANIC_QUARANTINE", "Could not quarantine mod for " + event.sandboxId(), ex); }
            ThreadGroup group = sandbox.threadGroup();
            if (group != null) {
                try { group.interrupt(); } catch (SecurityException ex) { DebugLog.error("PANIC_INTERRUPT", "Could not interrupt mod thread group for " + event.sandboxId(), ex); }
            }
        }
        atomicLogExecutor.execute(() -> {
            try { logger.writeJsonEvent("intrusion", event.sandboxId(), event.toJson()); }
            catch (IOException ex) { DebugLog.error("PANIC_LOG", "Could not write intrusion log for " + event.sandboxId(), ex); }
        });
    }

    private static String loaderId(ClassLoader loader) { return loader == null ? "bootstrap" : loader.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(loader)); }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        scheduler.shutdownNow();
        atomicLogExecutor.shutdown();
    }

    enum IntrusionType { REFLECTION_OR_PACKAGE_EVASION, CODE_MEMORY_DRIFT, CODE_RESOURCE_MISSING, CODE_RESOURCE_READ_FAILURE, EXCEPTION_FUZZING }

    record StackFrameSnapshot(String className, String methodName, String lineNumber, String loaderId) {
        String toJson() {
            return "{\"class\":" + AdminSecurityLogger.quote(className)
                    + ",\"method\":" + AdminSecurityLogger.quote(methodName)
                    + ",\"line\":" + AdminSecurityLogger.quote(lineNumber)
                    + ",\"loader\":" + AdminSecurityLogger.quote(loaderId) + "}";
        }
    }

    record IntrusionEvent(String sandboxId, IntrusionType type, String reason, List<StackFrameSnapshot> frames, Map<String, String> state, Instant createdAt) {
        IntrusionEvent {
            sandboxId = sandboxId == null || sandboxId.isBlank() ? "unknown" : sandboxId;
            type = type == null ? IntrusionType.REFLECTION_OR_PACKAGE_EVASION : type;
            reason = reason == null ? "unspecified" : reason;
            frames = List.copyOf(Objects.requireNonNullElse(frames, List.of()));
            state = Map.copyOf(Objects.requireNonNullElse(state, Map.of()));
            createdAt = createdAt == null ? Instant.now() : createdAt;
        }
        String toJson() {
            StringBuilder sb = new StringBuilder("{\n");
            sb.append("  \"type\": ").append(AdminSecurityLogger.quote(type.name())).append(",\n");
            sb.append("  \"sandboxId\": ").append(AdminSecurityLogger.quote(sandboxId)).append(",\n");
            sb.append("  \"reason\": ").append(AdminSecurityLogger.quote(reason)).append(",\n");
            sb.append("  \"createdAt\": ").append(AdminSecurityLogger.quote(createdAt.toString())).append(",\n");
            sb.append("  \"state\": {");
            int s = 0;
            for (var e : state.entrySet()) {
                if (s++ > 0) sb.append(',');
                sb.append(AdminSecurityLogger.quote(e.getKey())).append(':').append(AdminSecurityLogger.quote(e.getValue()));
            }
            sb.append("},\n  \"frames\": [");
            for (int i = 0; i < frames.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append('\n').append("    ").append(frames.get(i).toJson());
            }
            if (!frames.isEmpty()) sb.append('\n');
            return sb.append("  ]\n}\n").toString();
        }
    }

    interface LockdownTarget {
        void freezePlayerSession(String reason);
        void closeNetworkSession(String reason);
        void quarantineMod(String reason);
        static LockdownTarget noop() {
            return new LockdownTarget() {
                @Override public void freezePlayerSession(String reason) { DebugLog.warn("PANIC_NOOP", "freeze session requested: " + reason); }
                @Override public void closeNetworkSession(String reason) { DebugLog.warn("PANIC_NOOP", "close network requested: " + reason); }
                @Override public void quarantineMod(String reason) { DebugLog.warn("PANIC_NOOP", "quarantine requested: " + reason); }
            };
        }
    }

    record MonitoredSandbox(String id, SecureModClassLoader loader, ThreadGroup threadGroup, LockdownTarget target, Instant registeredAt) { }

    private final class SecurityExceptionWatchdog {
        private static final long WINDOW_MILLIS = 2_000L;
        private static final int SECURITY_FAULT_LIMIT = 5;
        private static final int TOTAL_FAULT_LIMIT = 50;
        private final ConcurrentMap<String, ArrayDeque<ExceptionSample>> samples = new ConcurrentHashMap<>();
        private final java.util.function.Consumer<IntrusionEvent> breachConsumer;

        SecurityExceptionWatchdog(java.util.function.Consumer<IntrusionEvent> breachConsumer) { this.breachConsumer = breachConsumer; }

        void record(String sandboxId, Throwable throwable) {
            String id = sandboxId == null || sandboxId.isBlank() ? "unknown" : sandboxId;
            long now = System.currentTimeMillis();
            ArrayDeque<ExceptionSample> deque = samples.computeIfAbsent(id, ignored -> new ArrayDeque<>());
            IntrusionEvent breach = null;
            synchronized (deque) {
                deque.addLast(new ExceptionSample(now, throwable.getClass().getName(), throwable.getMessage()));
                while (!deque.isEmpty() && now - deque.peekFirst().timestampMillis() > WINDOW_MILLIS) deque.removeFirst();
                long securityFaults = deque.stream().filter(ExceptionSample::securityRelevant).count();
                if (securityFaults > SECURITY_FAULT_LIMIT || deque.size() > TOTAL_FAULT_LIMIT) {
                    List<StackFrameSnapshot> frames = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                            .walk(stream -> stream.limit(64).map(frame -> new StackFrameSnapshot(frame.getDeclaringClass().getName(), frame.getMethodName(), String.valueOf(frame.getLineNumber()), loaderId(frame.getDeclaringClass().getClassLoader()))).toList());
                    breach = new IntrusionEvent(id, IntrusionType.EXCEPTION_FUZZING, "exception storm: securityFaults=" + securityFaults + " total=" + deque.size(), frames, Map.of("lastException", throwable.getClass().getName()), Instant.now());
                    deque.clear();
                }
            }
            if (breach != null) breachConsumer.accept(breach);
        }
    }

    record ExceptionSample(long timestampMillis, String className, String message) {
        boolean securityRelevant() {
            return className.endsWith("SecurityException") || className.endsWith("NullPointerException") || className.endsWith("ArrayIndexOutOfBoundsException");
        }
    }
}
