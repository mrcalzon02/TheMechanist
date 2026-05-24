package mechanist;

import java.util.*;

/**
 * Shared machine-operation queue authority.
 *
 * Operations can be registered, queued, assigned, ticked, blocked, failed,
 * cancelled, completed, and audited without creating bespoke medicae/lab/forge
 * mini-systems. Inventory, staffing, power, fuel, noise, hazard, UI panels,
 * provenance, and save/load serialization bind through this queue vocabulary.
 */
final class MachineOperationQueue {
    static final String VERSION = "0.9.08x";

    enum State {
        QUEUED,
        PENDING,
        ACTIVE,
        PAUSED,
        INTERRUPTED,
        BLOCKED,
        FAILED,
        COMPLETED,
        ABANDONED
    }

    static final class OperationProfile {
        final String id;
        final String promotionId;
        final OperationTargetRegistry.OperationLane lane;
        final OperationTargetRegistry.WorkerNeed workerNeed;
        final String inputFamily;
        final String outputFamily;
        final int nominalTurns;
        final int powerCost;
        final int fuelCost;
        final int noiseCost;
        final int hazardRisk;
        final boolean playerFactionParity;
        final String notes;

        OperationProfile(OperationTargetRegistry.OperationQueueTarget target) {
            this.id = target.id;
            this.promotionId = target.promotionId;
            this.lane = target.lane;
            this.workerNeed = target.workerNeed;
            this.inputFamily = target.inputFamily;
            this.outputFamily = target.outputFamily;
            this.nominalTurns = Math.max(1, target.nominalTurns);
            this.powerCost = Math.max(0, target.powerCost);
            this.fuelCost = Math.max(0, target.fuelCost);
            this.noiseCost = Math.max(0, target.noiseCost);
            this.hazardRisk = Math.max(0, target.hazardRisk);
            this.playerFactionParity = target.playerFactionParity;
            this.notes = target.notes == null ? "" : target.notes;
        }

        String compactLine() {
            return id + " lane=" + lane + " worker=" + workerNeed + " duration=" + nominalTurns
                    + " costs[p=" + powerCost + ",f=" + fuelCost + ",n=" + noiseCost + ",h=" + hazardRisk + "]"
                    + " input=" + inputFamily + " output=" + outputFamily;
        }
    }

    static final class OperationRecord {
        final long operationId;
        final OperationProfile profile;
        final String actorId;
        final String ownerId;
        final String targetId;
        final int queuedTurn;
        int startedTurn = -1;
        int completedTurn = -1;
        int progressTurns = 0;
        State state = State.QUEUED;
        String status = "queued";

        OperationRecord(long operationId, OperationProfile profile, String actorId, String ownerId, String targetId, int queuedTurn) {
            this.operationId = operationId;
            this.profile = profile;
            this.actorId = clean(actorId, "unassigned_actor");
            this.ownerId = clean(ownerId, "unowned");
            this.targetId = clean(targetId, profile == null ? "unknown_target" : profile.promotionId);
            this.queuedTurn = Math.max(0, queuedTurn);
        }

        boolean terminal() {
            return state == State.FAILED || state == State.COMPLETED || state == State.ABANDONED;
        }

        int remainingTurns() {
            return Math.max(0, profile.nominalTurns - progressTurns);
        }

        String auditLine() {
            return "op#" + operationId + " " + profile.id + " state=" + state + " progress=" + progressTurns + "/" + profile.nominalTurns
                    + " actor=" + actorId + " owner=" + ownerId + " target=" + targetId + " status=" + status;
        }

        String saveLine() {
            return operationId + "|" + safe(profile.id) + "|" + safe(actorId) + "|" + safe(ownerId) + "|" + safe(targetId)
                    + "|" + queuedTurn + "|" + startedTurn + "|" + completedTurn + "|" + progressTurns
                    + "|" + state.name() + "|" + safe(status);
        }
    }

    private final LinkedHashMap<String, OperationProfile> profiles = new LinkedHashMap<>();
    private final ArrayDeque<OperationRecord> pending = new ArrayDeque<>();
    private final ArrayList<OperationRecord> active = new ArrayList<>();
    private final ArrayList<OperationRecord> history = new ArrayList<>();
    private static final int MAX_HISTORY = 40;
    private long nextOperationId = 1L;
    private int lastProcessedTurn = -1;
    private long totalTicks = 0L;
    private long totalWork = 0L;
    private long externalCompletions = 0L;
    private int lastTickWork = 0;

    MachineOperationQueue() {
        registerOperationTargets();
    }

    private void registerOperationTargets() {
        for (OperationTargetRegistry.OperationQueueTarget target : OperationTargetRegistry.TARGETS) {
            register(new OperationProfile(target));
        }
    }

    void register(OperationProfile profile) {
        if (profile == null || profile.id == null || profile.id.isBlank()) return;
        profiles.put(profile.id, profile);
    }

    Collection<OperationProfile> profiles() {
        return Collections.unmodifiableCollection(profiles.values());
    }

    OperationProfile profile(String operationType) {
        return profiles.get(operationType);
    }

    OperationRecord enqueue(String operationType, String actorId, String ownerId, String targetId, int currentTurn) {
        OperationProfile p = profile(operationType);
        if (p == null) return null;
        OperationRecord record = new OperationRecord(nextOperationId++, p, actorId, ownerId, targetId, currentTurn);
        pending.addLast(record);
        return record;
    }

    int tick(int currentTurn, int maxStarts, int maxActiveTicks) {
        if (currentTurn == lastProcessedTurn) return 0;
        lastProcessedTurn = currentTurn;
        totalTicks++;
        int work = 0;
        int starts = Math.max(0, maxStarts);
        while (starts > 0 && !pending.isEmpty()) {
            OperationRecord record = pending.removeFirst();
            record.state = State.ACTIVE;
            record.startedTurn = currentTurn;
            record.status = "active through shared machine-operation queue";
            active.add(record);
            starts--;
            work++;
        }
        int ticks = Math.max(0, maxActiveTicks);
        Iterator<OperationRecord> it = active.iterator();
        while (it.hasNext() && ticks > 0) {
            OperationRecord record = it.next();
            if (record.terminal()) {
                it.remove();
                remember(record);
                continue;
            }
            if (record.state == State.ACTIVE) {
                record.progressTurns++;
                record.status = "progressing; remaining " + record.remainingTurns() + " turn(s)";
                work++;
                ticks--;
                if (record.progressTurns >= record.profile.nominalTurns) {
                    record.state = State.COMPLETED;
                    record.completedTurn = currentTurn;
                    record.status = "completed output family: " + record.profile.outputFamily;
                    it.remove();
                    remember(record);
                }
            }
        }
        lastTickWork = work;
        totalWork += work;
        return work;
    }



    OperationRecord recordExternalCompletion(String operationType, String actorId, String ownerId, String targetId, int currentTurn, int durationTurns, String status) {
        OperationProfile p = profile(operationType);
        if (p == null) return null;
        OperationRecord record = new OperationRecord(nextOperationId++, p, actorId, ownerId, targetId, currentTurn);
        record.state = State.COMPLETED;
        record.startedTurn = Math.max(0, currentTurn - Math.max(0, durationTurns));
        record.completedTurn = Math.max(0, currentTurn);
        record.progressTurns = Math.max(1, Math.min(p.nominalTurns, Math.max(1, durationTurns)));
        record.status = clean(status, "external production completion recorded; legacy production remains outcome authority");
        remember(record);
        externalCompletions++;
        totalWork++;
        lastTickWork = Math.max(lastTickWork, 1);
        return record;
    }

    boolean cancel(long operationId, String reason) {
        for (Iterator<OperationRecord> it = pending.iterator(); it.hasNext();) {
            OperationRecord record = it.next();
            if (record.operationId == operationId) {
                record.state = State.ABANDONED;
                record.status = clean(reason, "cancelled before start");
                it.remove();
                remember(record);
                return true;
            }
        }
        for (Iterator<OperationRecord> it = active.iterator(); it.hasNext();) {
            OperationRecord record = it.next();
            if (record.operationId == operationId) {
                record.state = State.ABANDONED;
                record.status = clean(reason, "cancelled while active");
                it.remove();
                remember(record);
                return true;
            }
        }
        return false;
    }

    void block(long operationId, String reason) {
        OperationRecord record = findLive(operationId);
        if (record == null || record.terminal()) return;
        record.state = State.BLOCKED;
        record.status = clean(reason, "blocked by missing requirement");
    }

    void resume(long operationId, String reason) {
        OperationRecord record = findLive(operationId);
        if (record == null || record.terminal()) return;
        record.state = State.ACTIVE;
        record.status = clean(reason, "resumed");
    }

    OperationRecord findLive(long operationId) {
        for (OperationRecord r : pending) if (r.operationId == operationId) return r;
        for (OperationRecord r : active) if (r.operationId == operationId) return r;
        return null;
    }

    List<OperationRecord> liveOperations() {
        ArrayList<OperationRecord> out = new ArrayList<>();
        out.addAll(pending);
        out.addAll(active);
        return Collections.unmodifiableList(out);
    }

    List<OperationRecord> recentHistory() {
        return Collections.unmodifiableList(history);
    }

    List<String> encodeRecentHistory() {
        ArrayList<String> out = new ArrayList<>();
        for (OperationRecord r : history) if (r != null && r.profile != null) out.add(r.saveLine());
        return out;
    }

    void restoreRecentHistory(List<String> encodedLines) {
        history.clear();
        if (encodedLines == null) return;
        long maxId = 0L;
        for (String line : encodedLines) {
            OperationRecord r = parseHistoryLine(line);
            if (r == null) continue;
            history.add(r);
            maxId = Math.max(maxId, r.operationId);
            while (history.size() > MAX_HISTORY) history.remove(0);
        }
        if (maxId >= nextOperationId) nextOperationId = maxId + 1L;
    }

    private OperationRecord parseHistoryLine(String line) {
        if (line == null || line.isBlank()) return null;
        String[] a = line.split("\\|", 11);
        if (a.length < 11) return null;
        try {
            OperationProfile p = profile(a[1]);
            if (p == null) return null;
            OperationRecord r = new OperationRecord(Long.parseLong(a[0]), p, a[2], a[3], a[4], Integer.parseInt(a[5]));
            r.startedTurn = Integer.parseInt(a[6]);
            r.completedTurn = Integer.parseInt(a[7]);
            r.progressTurns = Integer.parseInt(a[8]);
            r.state = State.valueOf(a[9]);
            r.status = clean(a[10], "restored queue history record");
            return r;
        } catch (Exception ignored) { return null; }
    }

    ArrayList<String> recentHistoryLines(int limit) {
        ArrayList<String> out = new ArrayList<>();
        int cap = Math.max(0, limit);
        int start = Math.max(0, history.size() - cap);
        for (int i = start; i < history.size(); i++) out.add(history.get(i).auditLine());
        return out;
    }

    int pendingCount() { return pending.size(); }
    int activeCount() { return active.size(); }
    int historyCount() { return history.size(); }

    String auditSummary() {
        EnumMap<State, Integer> byState = new EnumMap<>(State.class);
        for (OperationRecord r : pending) byState.put(r.state, byState.getOrDefault(r.state, 0) + 1);
        for (OperationRecord r : active) byState.put(r.state, byState.getOrDefault(r.state, 0) + 1);
        for (OperationRecord r : history) byState.put(r.state, byState.getOrDefault(r.state, 0) + 1);
        return "machineOperationQueue version=" + VERSION + " profiles=" + profiles.size()
                + " pending=" + pending.size() + " active=" + active.size() + " history=" + history.size()
                + " lastWork=" + lastTickWork + " totalTicks=" + totalTicks + " totalWork=" + totalWork + " externalCompletions=" + externalCompletions
                + " states=" + byState;
    }

    private void remember(OperationRecord record) {
        history.add(record);
        while (history.size() > MAX_HISTORY) history.remove(0);
    }

    private static String clean(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text;
    }

    private static String safe(String text) {
        return clean(text, "").replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }
}
