package mechanist;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Fixed-ring input telemetry scanner for AFK/macro enforcement. */
final class PlayerBehaviorMonitor {
    enum InputKind { MOVE, FIRE, INVENTORY, INTERACT, CAMERA, UI, CHAT, IDLE }
    enum Classification { NORMAL, HELD_INPUT_MACRO, PERFECT_TIMING_MACRO }

    record InputTelemetry(long timestampMillis, InputKind kind, int keyMask, int xAxis, int yAxis, boolean combatRelevant) {
        String signature() { return kind + ":" + keyMask + ":" + xAxis + ":" + yAxis + ":" + combatRelevant; }
        boolean genuineInteraction() { return kind != InputKind.IDLE && (keyMask != 0 || xAxis != 0 || yAxis != 0 || combatRelevant); }
    }

    record DetectionResult(Classification classification, String reason, int samplesObserved, long spanMillis) {
        static DetectionResult normal(int samples, long span) { return new DetectionResult(Classification.NORMAL, "normal", samples, span); }
        boolean suspicious() { return classification != Classification.NORMAL; }
    }

    interface DisconnectAction { void disconnect(String sessionId, String reason); }

    private final String sessionId;
    private final InputTelemetry[] ring;
    private final long heldInputThresholdMillis;
    private final long perfectTimingThresholdMillis;
    private final AdminSecurityLogger logger;
    private final DisconnectAction disconnectAction;
    private final AtomicBoolean disconnected = new AtomicBoolean(false);
    private int writeIndex;
    private int size;

    PlayerBehaviorMonitor(String sessionId, int capacity, long heldInputThresholdMillis, long perfectTimingThresholdMillis, AdminSecurityLogger logger, DisconnectAction disconnectAction) {
        if (capacity < 16) throw new IllegalArgumentException("capacity must be at least 16");
        this.sessionId = sessionId == null || sessionId.isBlank() ? "unknown" : sessionId;
        this.ring = new InputTelemetry[capacity];
        this.heldInputThresholdMillis = Math.max(1_000L, heldInputThresholdMillis);
        this.perfectTimingThresholdMillis = Math.max(1_000L, perfectTimingThresholdMillis);
        this.logger = Objects.requireNonNull(logger, "logger");
        this.disconnectAction = Objects.requireNonNull(disconnectAction, "disconnectAction");
    }

    synchronized DetectionResult record(InputTelemetry telemetry) {
        Objects.requireNonNull(telemetry, "telemetry");
        ring[writeIndex] = telemetry;
        writeIndex = (writeIndex + 1) % ring.length;
        if (size < ring.length) size++;
        DetectionResult result = analyze();
        if (result.suspicious() && disconnected.compareAndSet(false, true)) {
            try {
                logger.writeJsonEvent("macro", sessionId, AdminSecurityLogger.eventEnvelope("SuspiciousAutomationActivity", sessionId, timelineJson(result)));
            } catch (IOException ex) {
                DebugLog.error("ANTI_MACRO_LOG", "Could not write macro review log for " + sessionId, ex);
            }
            disconnectAction.disconnect(sessionId, ObfuscatedStringTable.text(ObfuscatedStringTable.Key.SUSPICIOUS_AUTOMATION_ACTIVITY) + ": " + result.reason());
        }
        return result;
    }

    synchronized DetectionResult analyze() {
        if (size < 8) return DetectionResult.normal(size, 0L);
        InputTelemetry[] ordered = ordered();
        long span = ordered[ordered.length - 1].timestampMillis() - ordered[0].timestampMillis();
        DetectionResult held = detectHeldInput(ordered, span);
        if (held.suspicious()) return held;
        DetectionResult timing = detectPerfectTiming(ordered, span);
        if (timing.suspicious()) return timing;
        return DetectionResult.normal(size, Math.max(0L, span));
    }

    private DetectionResult detectHeldInput(InputTelemetry[] ordered, long span) {
        String first = null;
        long firstTime = 0L;
        int count = 0;
        for (InputTelemetry t : ordered) {
            if (!t.genuineInteraction()) continue;
            String sig = t.signature();
            if (first == null) {
                first = sig;
                firstTime = t.timestampMillis();
                count = 1;
            } else if (first.equals(sig)) {
                count++;
                long held = t.timestampMillis() - firstTime;
                if (held >= heldInputThresholdMillis && count >= 30) {
                    return new DetectionResult(Classification.HELD_INPUT_MACRO, "identical input signature held for " + held + " ms", count, held);
                }
            } else {
                first = sig;
                firstTime = t.timestampMillis();
                count = 1;
            }
        }
        return DetectionResult.normal(size, Math.max(0L, span));
    }

    private DetectionResult detectPerfectTiming(InputTelemetry[] ordered, long span) {
        long previousDelta = -1L;
        int identicalDeltas = 0;
        InputTelemetry previous = null;
        for (InputTelemetry t : ordered) {
            if (!t.genuineInteraction()) continue;
            if (previous != null) {
                long delta = t.timestampMillis() - previous.timestampMillis();
                if (delta > 0 && delta == previousDelta) identicalDeltas++; else identicalDeltas = 0;
                previousDelta = delta;
                if (identicalDeltas >= 30 && span >= perfectTimingThresholdMillis) {
                    return new DetectionResult(Classification.PERFECT_TIMING_MACRO, "perfectly repeated input interval of " + delta + " ms", identicalDeltas + 2, span);
                }
            }
            previous = t;
        }
        return DetectionResult.normal(size, Math.max(0L, span));
    }

    private InputTelemetry[] ordered() {
        InputTelemetry[] out = new InputTelemetry[size];
        int start = size == ring.length ? writeIndex : 0;
        for (int i = 0; i < size; i++) out[i] = ring[(start + i) % ring.length];
        return out;
    }

    private String timelineJson(DetectionResult result) {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"classification\": ").append(AdminSecurityLogger.quote(result.classification().name())).append(",\n");
        json.append("  \"reason\": ").append(AdminSecurityLogger.quote(result.reason())).append(",\n");
        json.append("  \"createdAt\": ").append(AdminSecurityLogger.quote(Instant.now().toString())).append(",\n");
        json.append("  \"timeline\": [\n");
        InputTelemetry[] ordered = ordered();
        for (int i = 0; i < ordered.length; i++) {
            InputTelemetry t = ordered[i];
            json.append("    {\"t\":").append(t.timestampMillis())
                    .append(",\"kind\":").append(AdminSecurityLogger.quote(t.kind().name()))
                    .append(",\"keyMask\":").append(t.keyMask())
                    .append(",\"x\":").append(t.xAxis())
                    .append(",\"y\":").append(t.yAxis())
                    .append(",\"combat\":").append(t.combatRelevant()).append("}");
            if (i + 1 < ordered.length) json.append(',');
            json.append('\n');
        }
        json.append("  ]\n}");
        return json.toString();
    }

    @Override public String toString() { return "PlayerBehaviorMonitor{" + sessionId + ", size=" + size + ", ring=" + Arrays.toString(ordered()) + '}'; }
}
