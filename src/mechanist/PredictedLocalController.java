package mechanist;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/** Client-side immediate movement prediction plus authoritative reconciliation. */
final class PredictedLocalController {
    static final double DEFAULT_DESYNC_THRESHOLD = 0.125d;
    private final ArrayDeque<PredictionFrame> pendingInputs = new ArrayDeque<>(256);
    private final ReentrantLock lock = new ReentrantLock();
    private final double maxSpeedUnitsPerSecond;
    private LocalPlayerKinematicState state;
    private long nextInputTick = 0;

    PredictedLocalController(LocalPlayerKinematicState initialState, double maxSpeedUnitsPerSecond) {
        this.state = Objects.requireNonNullElse(initialState, LocalPlayerKinematicState.origin());
        this.maxSpeedUnitsPerSecond = Math.max(0.1d, Math.min(64.0d, maxSpeedUnitsPerSecond));
    }

    PredictionFrame applyInput(MovementInput input) {
        Objects.requireNonNull(input, "input");
        lock.lock();
        try {
            MovementInput stamped = input.withTick(nextInputTick++);
            LocalPlayerKinematicState before = state;
            state = integrate(state, stamped, maxSpeedUnitsPerSecond);
            PredictionFrame frame = new PredictionFrame(stamped.inputTickId(), stamped, before, state);
            pendingInputs.addLast(frame);
            while (pendingInputs.size() > 512) pendingInputs.removeFirst();
            return frame;
        } finally { lock.unlock(); }
    }

    ReconciliationResult reconcile(AuthoritativePlayerCorrection correction) { return reconcile(correction, DEFAULT_DESYNC_THRESHOLD); }

    ReconciliationResult reconcile(AuthoritativePlayerCorrection correction, double threshold) {
        Objects.requireNonNull(correction, "correction");
        lock.lock();
        try {
            PredictionFrame matched = null;
            while (!pendingInputs.isEmpty()) {
                PredictionFrame frame = pendingInputs.peekFirst();
                if (frame.inputTickId() < correction.inputTickId()) {
                    pendingInputs.removeFirst();
                } else if (frame.inputTickId() == correction.inputTickId()) {
                    matched = pendingInputs.removeFirst();
                    break;
                } else {
                    break;
                }
            }
            LocalPlayerKinematicState authoritative = new LocalPlayerKinematicState(correction.x(), correction.y(), correction.z(), correction.vx(), correction.vy(), correction.vz(), correction.yaw());
            double error = matched == null ? distance(state, authoritative) : distance(matched.after(), authoritative);
            if (error <= Math.max(0.001d, threshold)) {
                return new ReconciliationResult(false, error, state, List.copyOf(pendingInputs), "prediction within threshold");
            }
            ArrayList<PredictionFrame> resimulated = new ArrayList<>(pendingInputs.size());
            LocalPlayerKinematicState corrected = authoritative;
            ArrayDeque<PredictionFrame> rebuilt = new ArrayDeque<>(pendingInputs.size());
            for (PredictionFrame frame : pendingInputs) {
                LocalPlayerKinematicState before = corrected;
                corrected = integrate(corrected, frame.input(), maxSpeedUnitsPerSecond);
                PredictionFrame replayed = new PredictionFrame(frame.inputTickId(), frame.input(), before, corrected);
                rebuilt.addLast(replayed);
                resimulated.add(replayed);
            }
            pendingInputs.clear();
            pendingInputs.addAll(rebuilt);
            state = corrected;
            return new ReconciliationResult(true, error, state, List.copyOf(resimulated), "server correction applied and pending inputs resimulated");
        } finally { lock.unlock(); }
    }

    LocalPlayerKinematicState currentState() {
        lock.lock();
        try { return state; } finally { lock.unlock(); }
    }

    int pendingInputCount() {
        lock.lock();
        try { return pendingInputs.size(); } finally { lock.unlock(); }
    }

    private static LocalPlayerKinematicState integrate(LocalPlayerKinematicState s, MovementInput input, double maxSpeed) {
        double dt = Math.max(0.001d, Math.min(0.25d, input.deltaSeconds()));
        double ix = clamp(input.moveX(), -1, 1);
        double iy = clamp(input.moveY(), -1, 1);
        double len = Math.sqrt(ix * ix + iy * iy);
        if (len > 1.0d) { ix /= len; iy /= len; }
        double speed = maxSpeed * clamp(input.speedMultiplier(), 0, 2.5d);
        double vx = ix * speed;
        double vy = iy * speed;
        double vz = s.vz();
        double x = s.x() + vx * dt;
        double y = s.y() + vy * dt;
        double z = s.z() + vz * dt;
        double yaw = input.aimYawDegrees();
        return new LocalPlayerKinematicState(x, y, z, vx, vy, vz, yaw);
    }

    private static double distance(LocalPlayerKinematicState a, LocalPlayerKinematicState b) {
        double dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}

record MovementInput(long inputTickId, double moveX, double moveY, double speedMultiplier, double aimYawDegrees, double deltaSeconds, long clientNanos) {
    MovementInput {
        deltaSeconds = Math.max(0.001d, Math.min(0.25d, deltaSeconds));
        speedMultiplier = Math.max(0.0d, Math.min(2.5d, speedMultiplier));
        clientNanos = clientNanos <= 0 ? System.nanoTime() : clientNanos;
    }
    MovementInput withTick(long tick) { return new MovementInput(tick, moveX, moveY, speedMultiplier, aimYawDegrees, deltaSeconds, clientNanos); }
}

record PredictionFrame(long inputTickId, MovementInput input, LocalPlayerKinematicState before, LocalPlayerKinematicState after) { }
record LocalPlayerKinematicState(double x, double y, double z, double vx, double vy, double vz, double yaw) { static LocalPlayerKinematicState origin() { return new LocalPlayerKinematicState(0, 0, 0, 0, 0, 0, 0); } }
record AuthoritativePlayerCorrection(long inputTickId, double x, double y, double z, double vx, double vy, double vz, double yaw, long serverNanos) { }
record ReconciliationResult(boolean corrected, double errorMagnitude, LocalPlayerKinematicState finalState, List<PredictionFrame> resimulatedFrames, String reason) { }
