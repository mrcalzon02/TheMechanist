package mechanist;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Optional Netty resource-safety seam; compiles without Netty and activates by reflection if present. */
final class NettyResourceSafetyBridge {
    private static final AtomicBoolean configured = new AtomicBoolean(false);

    static boolean configureAdvancedLeakDetectionIfPresent() {
        if (!configured.compareAndSet(false, true)) return true;
        try {
            Class<?> detector = Class.forName("io.netty.util.ResourceLeakDetector");
            Class<?> level = Class.forName("io.netty.util.ResourceLeakDetector$Level");
            Object advanced = Enum.valueOf((Class<Enum>)level.asSubclass(Enum.class), "ADVANCED");
            Method setLevel = detector.getMethod("setLevel", level);
            setLevel.invoke(null, advanced);
            DebugLog.audit("NETTY_LEAK_DETECTOR", "ResourceLeakDetector.Level.ADVANCED configured via reflection.");
            return true;
        } catch (ClassNotFoundException ex) {
            DebugLog.audit("NETTY_LEAK_DETECTOR", "Netty not present; native payload close tracking active only.");
            return false;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            DebugLog.error("NETTY_LEAK_DETECTOR", "Could not configure Netty leak detector by reflection.", ex);
            return false;
        }
    }

    static void releaseIfNettyReferenceCounted(Object msg) {
        if (msg == null) return;
        try {
            Class<?> refUtil = Class.forName("io.netty.util.ReferenceCountUtil");
            Method release = refUtil.getMethod("release", Object.class);
            release.invoke(null, msg);
        } catch (ClassNotFoundException ignored) {
            if (msg instanceof SafePayload payload) payload.close();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            DebugLog.error("NETTY_RELEASE", "ReferenceCountUtil.release failed for " + msg.getClass().getName(), ex);
        }
    }

    static final class SafePayload implements AutoCloseable {
        private ByteBuffer buffer;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        SafePayload(ByteBuffer buffer) { this.buffer = Objects.requireNonNull(buffer, "buffer"); }
        ByteBuffer buffer() {
            if (closed.get()) throw new IllegalStateException("payload already closed");
            return buffer;
        }
        @Override public void close() {
            if (closed.compareAndSet(false, true)) buffer = ByteBuffer.allocate(0);
        }
    }

    private NettyResourceSafetyBridge() { }
}
