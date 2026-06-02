package mechanist;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** Client-side remote-entity interpolation buffer that renders authoritative snapshots slightly in the past. */
final class EntityInterpolationBuffer {
    private final long interpolationDelayNanos;
    private final int maxSnapshotsPerEntity;
    private final Map<Long, EntityTimeline> timelines = new ConcurrentHashMap<>();

    EntityInterpolationBuffer() { this(Duration.ofMillis(100), 16); }

    EntityInterpolationBuffer(Duration interpolationDelay, int maxSnapshotsPerEntity) {
        this.interpolationDelayNanos = Math.max(10_000_000L, Objects.requireNonNullElse(interpolationDelay, Duration.ofMillis(100)).toNanos());
        this.maxSnapshotsPerEntity = Math.max(2, Math.min(128, maxSnapshotsPerEntity));
    }

    void accept(EntityRenderSnapshot snapshot) {
        if (snapshot == null) return;
        timelines.computeIfAbsent(snapshot.entityId(), id -> new EntityTimeline(maxSnapshotsPerEntity)).add(snapshot);
    }

    RenderedEntityPose sample(long entityId, long nowNanos) {
        EntityTimeline timeline = timelines.get(entityId);
        if (timeline == null) return null;
        return timeline.sample(nowNanos - interpolationDelayNanos);
    }

    void remove(long entityId) { timelines.remove(entityId); }
    int entityCount() { return timelines.size(); }

    private static final class EntityTimeline {
        private final int max;
        private final ArrayDeque<EntityRenderSnapshot> snapshots = new ArrayDeque<>();
        private final ReentrantLock lock = new ReentrantLock();

        EntityTimeline(int max) { this.max = max; }

        void add(EntityRenderSnapshot snapshot) {
            lock.lock();
            try {
                if (snapshots.isEmpty() || snapshots.peekLast().serverTimeNanos() <= snapshot.serverTimeNanos()) {
                    snapshots.addLast(snapshot);
                } else {
                    ArrayDeque<EntityRenderSnapshot> rebuilt = new ArrayDeque<>();
                    boolean inserted = false;
                    for (EntityRenderSnapshot s : snapshots) {
                        if (!inserted && snapshot.serverTimeNanos() < s.serverTimeNanos()) { rebuilt.add(snapshot); inserted = true; }
                        rebuilt.add(s);
                    }
                    if (!inserted) rebuilt.add(snapshot);
                    snapshots.clear(); snapshots.addAll(rebuilt);
                }
                while (snapshots.size() > max) snapshots.removeFirst();
            } finally { lock.unlock(); }
        }

        RenderedEntityPose sample(long renderTimeNanos) {
            lock.lock();
            try {
                if (snapshots.isEmpty()) return null;
                EntityRenderSnapshot first = snapshots.peekFirst();
                EntityRenderSnapshot last = snapshots.peekLast();
                if (snapshots.size() == 1 || renderTimeNanos <= first.serverTimeNanos()) return first.pose().asRendered(0.0d, "clamped-first");
                if (renderTimeNanos >= last.serverTimeNanos()) return last.pose().asRendered(1.0d, "clamped-last");
                EntityRenderSnapshot a = first;
                for (EntityRenderSnapshot b : snapshots) {
                    if (b.serverTimeNanos() >= renderTimeNanos) {
                        double denom = Math.max(1.0d, b.serverTimeNanos() - a.serverTimeNanos());
                        double t = clamp01((renderTimeNanos - a.serverTimeNanos()) / denom);
                        return EntityPose.lerp(a.pose(), b.pose(), t).asRendered(t, "interpolated");
                    }
                    a = b;
                }
                return last.pose().asRendered(1.0d, "fallthrough-last");
            } finally { lock.unlock(); }
        }
    }

    static double clamp01(double v) { return Math.max(0.0d, Math.min(1.0d, v)); }
}

record EntityRenderSnapshot(long entityId, long serverTimeNanos, EntityPose pose) {
    EntityRenderSnapshot { if (serverTimeNanos <= 0) serverTimeNanos = System.nanoTime(); pose = Objects.requireNonNull(pose, "pose"); }
}

record EntityPose(double x, double y, double z, Quaternion rotation) {
    EntityPose { rotation = rotation == null ? Quaternion.identity() : rotation.normalized(); }
    static EntityPose lerp(EntityPose a, EntityPose b, double t) {
        double u = EntityInterpolationBuffer.clamp01(t);
        return new EntityPose(lerp(a.x, b.x, u), lerp(a.y, b.y, u), lerp(a.z, b.z, u), Quaternion.slerp(a.rotation, b.rotation, u));
    }
    RenderedEntityPose asRendered(double alpha, String source) { return new RenderedEntityPose(x, y, z, rotation, alpha, source); }
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
}

record Quaternion(double w, double x, double y, double z) {
    static Quaternion identity() { return new Quaternion(1, 0, 0, 0); }
    Quaternion normalized() {
        double len = Math.sqrt(w*w + x*x + y*y + z*z);
        if (len <= 0.0000001d) return identity();
        return new Quaternion(w/len, x/len, y/len, z/len);
    }
    static Quaternion fromYawDegrees(double yaw) {
        double half = Math.toRadians(yaw) * 0.5d;
        return new Quaternion(Math.cos(half), 0, 0, Math.sin(half));
    }
    static Quaternion slerp(Quaternion a, Quaternion b, double t) {
        double u = EntityInterpolationBuffer.clamp01(t);
        Quaternion qa = a == null ? identity() : a.normalized();
        Quaternion qb = b == null ? identity() : b.normalized();
        double dot = qa.w*qb.w + qa.x*qb.x + qa.y*qb.y + qa.z*qb.z;
        if (dot < 0.0d) { qb = new Quaternion(-qb.w, -qb.x, -qb.y, -qb.z); dot = -dot; }
        if (dot > 0.9995d) return new Quaternion(lerp(qa.w, qb.w, u), lerp(qa.x, qb.x, u), lerp(qa.y, qb.y, u), lerp(qa.z, qb.z, u)).normalized();
        double theta0 = Math.acos(Math.max(-1.0d, Math.min(1.0d, dot)));
        double theta = theta0 * u;
        double sinTheta = Math.sin(theta);
        double sinTheta0 = Math.sin(theta0);
        double s0 = Math.cos(theta) - dot * sinTheta / sinTheta0;
        double s1 = sinTheta / sinTheta0;
        return new Quaternion((s0 * qa.w) + (s1 * qb.w), (s0 * qa.x) + (s1 * qb.x), (s0 * qa.y) + (s1 * qb.y), (s0 * qa.z) + (s1 * qb.z)).normalized();
    }
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
}

record RenderedEntityPose(double x, double y, double z, Quaternion rotation, double alpha, String source) { }
