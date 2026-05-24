package mechanist;

import java.util.*;

/**
 * 0.9.10j — Logistics Manual Haul Order Contract Bridge.
 *
 * Converts the latest delivery intent/source token/route intent/haul preview
 * chain into a bounded, saveable manual haul-order contract record.  This is
 * deliberately still record-only: it does not pathfind, move actors, lock
 * items, transfer goods, or consume production inputs.
 */
final class LogisticsManualHaulContractAuthority {
    static final String VERSION = "0.9.10j";
    static final int MAX_RECORDS = 24;
    static final int MAX_WARNINGS = 8;

    private LogisticsManualHaulContractAuthority() {}

    static final class ManualHaulContractRecord {
        final int id;
        final int turn;
        final int intentId;
        final int sourceTokenId;
        final int routeIntentId;
        final int previewId;
        final String stationName;
        final String workerLabel;
        final String contractStatus;
        final int readinessScore;
        final int fromX;
        final int fromY;
        final int toX;
        final int toY;
        final int estimatedSteps;
        final ArrayList<String> warnings;

        ManualHaulContractRecord(int id, int turn, int intentId, int sourceTokenId, int routeIntentId, int previewId,
                                 String stationName, String workerLabel, String contractStatus, int readinessScore,
                                 int fromX, int fromY, int toX, int toY, int estimatedSteps, ArrayList<String> warnings) {
            this.id = Math.max(0, id);
            this.turn = Math.max(0, turn);
            this.intentId = Math.max(0, intentId);
            this.sourceTokenId = Math.max(0, sourceTokenId);
            this.routeIntentId = Math.max(0, routeIntentId);
            this.previewId = Math.max(0, previewId);
            this.stationName = clean(stationName, "unknown station");
            this.workerLabel = clean(workerLabel, "unassigned");
            this.contractStatus = clean(contractStatus, "record-only");
            this.readinessScore = Math.max(0, Math.min(100, readinessScore));
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
            this.estimatedSteps = Math.max(0, estimatedSteps);
            this.warnings = warnings == null ? new ArrayList<>() : new ArrayList<>(warnings);
        }

        String compact() {
            return "Haul contract #" + id + " turn " + turn + " intent #" + intentId
                    + " source #" + sourceTokenId + " route #" + routeIntentId + " preview #" + previewId
                    + " " + stationName + " worker=" + workerLabel + " readiness=" + readinessScore
                    + "% status=" + contractStatus + " " + fromX + "," + fromY + " -> " + toX + "," + toY
                    + " estSteps=" + estimatedSteps;
        }

        String encode() {
            return id + "\t" + turn + "\t" + intentId + "\t" + sourceTokenId + "\t" + routeIntentId + "\t" + previewId + "\t"
                    + esc(stationName) + "\t" + esc(workerLabel) + "\t" + esc(contractStatus) + "\t" + readinessScore + "\t"
                    + fromX + "\t" + fromY + "\t" + toX + "\t" + toY + "\t" + estimatedSteps + "\t" + esc(String.join(";;", warnings));
        }

        static ManualHaulContractRecord decode(String raw) {
            if (raw == null || raw.isBlank()) return null;
            String[] a = raw.split("\\t", -1);
            if (a.length < 16) return null;
            try {
                ArrayList<String> warns = new ArrayList<>();
                String joined = unesc(a[15]);
                if (!joined.isBlank()) for (String s : joined.split(";;", -1)) if (!s.isBlank()) warns.add(s);
                return new ManualHaulContractRecord(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]),
                        Integer.parseInt(a[3]), Integer.parseInt(a[4]), Integer.parseInt(a[5]), unesc(a[6]), unesc(a[7]),
                        unesc(a[8]), Integer.parseInt(a[9]), Integer.parseInt(a[10]), Integer.parseInt(a[11]),
                        Integer.parseInt(a[12]), Integer.parseInt(a[13]), Integer.parseInt(a[14]), warns);
            } catch (Exception ignored) { return null; }
        }
    }

    static String createManualHaulContract(GamePanel g) {
        if (g == null) return "HAUL CONTRACT FAILED: no active game panel.";
        LogisticsDeliveryIntentAuthority.DeliveryIntentRecord intent = g.logisticsDeliveryIntentHistory.peekFirst();
        LogisticsSourceReservationAuthority.SourceReservationRecord source = g.logisticsSourceReservationHistory.peekFirst();
        LogisticsRouteIntentAuthority.RouteIntentRecord route = g.logisticsRouteIntentHistory.peekFirst();
        LogisticsRouteReadinessPreviewAuthority.ManualHaulPreviewRecord preview = g.logisticsRoutePreviewHistory.peekFirst();
        ArrayList<String> warnings = new ArrayList<>();

        if (intent == null) warnings.add("BLOCK: no delivery intent exists; use INTENT first.");
        if (source == null) warnings.add("BLOCK: no source token exists; use SOURCE TOKEN first.");
        if (route == null) warnings.add("BLOCK: no route intent exists; use ROUTE INTENT first.");
        if (preview == null) warnings.add("BLOCK: no haul preview exists; use HAUL PREVIEW first.");

        if (intent != null && source != null && source.intentId != intent.id) warnings.add("WARN: latest source token targets intent #" + source.intentId + " but latest intent is #" + intent.id + ".");
        if (route != null && intent != null && route.intentId != intent.id) warnings.add("WARN: latest route targets intent #" + route.intentId + " but latest intent is #" + intent.id + ".");
        if (route != null && source != null && route.sourceTokenId != source.id) warnings.add("WARN: latest route uses source token #" + route.sourceTokenId + " but latest token is #" + source.id + ".");
        if (preview != null && route != null && preview.routeIntentId != route.id) warnings.add("WARN: latest preview targets route #" + preview.routeIntentId + " but latest route is #" + route.id + ".");
        if (preview != null) for (String w : preview.warnings) if (w != null && !w.isBlank()) warnings.add(w);

        BaseObject station = intent == null ? null : findBaseObjectAt(g, intent.stationX, intent.stationY, intent.stationName);
        String worker = station != null && station.assignedWorker != null && !station.assignedWorker.isBlank()
                ? station.assignedWorker : (intent == null ? "unassigned" : intent.workerLabel);
        if (worker == null || worker.isBlank() || worker.equals("unassigned")) warnings.add("WARN: no worker is currently assigned; contract can be recorded but not fulfilled by labor yet.");

        warnings = cap(warnings);
        int critical = 0;
        for (String w : warnings) if (w != null && w.startsWith("BLOCK:")) critical++;
        int readiness = preview == null ? 0 : preview.readinessScore;
        readiness = Math.max(0, readiness - critical * 25 - Math.max(0, warnings.size() - critical) * 5);
        String status = critical > 0 ? "blocked-contract-record" : (warnings.isEmpty() ? "ready-contract-record" : "warning-contract-record");
        int id = Math.max(1, g.nextLogisticsHaulContractSeq++);
        ManualHaulContractRecord rec = new ManualHaulContractRecord(id, g.turn,
                intent == null ? 0 : intent.id,
                source == null ? 0 : source.id,
                route == null ? 0 : route.id,
                preview == null ? 0 : preview.id,
                intent == null ? "unknown station" : intent.stationName,
                worker, status, readiness,
                route == null ? -1 : route.fromX, route == null ? -1 : route.fromY,
                route == null ? -1 : route.toX, route == null ? -1 : route.toY,
                route == null ? 0 : route.estimatedSteps, warnings);
        remember(g, rec);
        return "MANUAL HAUL CONTRACT RECORDED: " + rec.compact() + ". "
                + (warnings.isEmpty() ? "No warnings." : warnings.size() + " warning(s)/blocker(s) retained for inspection.")
                + " Contract only: no pathfinding, actor dispatch, item lock, item transfer, or production.";
    }

    private static BaseObject findBaseObjectAt(GamePanel g, int x, int y, String name) {
        if (g == null) return null;
        for (BaseObject b : g.baseObjects) {
            if (b == null) continue;
            if (b.x == x && b.y == y) return b;
            if (name != null && !name.isBlank() && name.equals(b.name)) return b;
        }
        return null;
    }

    private static ArrayList<String> cap(ArrayList<String> in) {
        if (in == null) return new ArrayList<>();
        if (in.size() <= MAX_WARNINGS) return in;
        ArrayList<String> out = new ArrayList<>(in.subList(0, MAX_WARNINGS));
        out.add("WARN: " + (in.size() - MAX_WARNINGS) + " additional warning(s) hidden by bounded display.");
        return out;
    }

    static void remember(GamePanel g, ManualHaulContractRecord rec) {
        if (g == null || rec == null) return;
        g.logisticsHaulContractHistory.addFirst(rec);
        while (g.logisticsHaulContractHistory.size() > MAX_RECORDS) g.logisticsHaulContractHistory.removeLast();
    }

    static ArrayList<String> encodeHistory(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        if (g == null) return out;
        for (ManualHaulContractRecord rec : g.logisticsHaulContractHistory) if (rec != null) out.add(rec.encode());
        return out;
    }

    static void restoreHistory(GamePanel g, java.util.List<String> encoded) {
        if (g == null) return;
        g.logisticsHaulContractHistory.clear();
        int maxId = 0;
        if (encoded != null) {
            for (String raw : encoded) {
                ManualHaulContractRecord rec = ManualHaulContractRecord.decode(raw);
                if (rec != null) {
                    g.logisticsHaulContractHistory.add(rec);
                    maxId = Math.max(maxId, rec.id);
                    if (g.logisticsHaulContractHistory.size() >= MAX_RECORDS) break;
                }
            }
        }
        g.nextLogisticsHaulContractSeq = Math.max(g.nextLogisticsHaulContractSeq, maxId + 1);
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics Manual Haul Contract Authority " + VERSION + " — records a saveable manual haul contract from current logistics intent, source, route, and preview data.");
        if (g == null) { out.add("  No active game panel."); return out; }
        out.add("  Stored haul contracts: " + g.logisticsHaulContractHistory.size() + "/" + MAX_RECORDS + ". Next id: " + g.nextLogisticsHaulContractSeq + ".");
        ManualHaulContractRecord latest = g.logisticsHaulContractHistory.peekFirst();
        if (latest == null) out.add("  No haul contract yet. Use INTENT, SOURCE TOKEN, ROUTE INTENT, HAUL PREVIEW, then HAUL CONTRACT.");
        else {
            out.add("  Latest: " + latest.compact());
            if (latest.warnings.isEmpty()) out.add("  warnings none");
            else for (String w : latest.warnings) out.add("  " + w);
        }
        out.add("  Efficiency: reuses existing intent/source/route/preview authorities, selected/latest record only, bounded history, and no duplicate routing or storage scan.");
        return out;
    }

    static String compactSummary(GamePanel g) {
        if (g == null) return "Logistics haul contract: no active panel.";
        ManualHaulContractRecord rec = g.logisticsHaulContractHistory.peekFirst();
        return rec == null ? "Logistics haul contract: none recorded yet." : "Logistics haul contract: latest " + rec.compact();
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics Manual Haul Contract Authority " + VERSION);
        out.add("Purpose: formalize a manual haul order as a saveable contract after delivery intent, source token, route intent, and haul preview exist.");
        out.add("Current behavior: records one bounded latest-chain contract with warnings/blockers and readiness, for player inspection and reserved execution rules.");
        out.add("Player feedback: reports station, worker, source token, route intent, preview id, readiness, estimated steps, and blocker/warning text.");
        out.add("Hard boundary: no pathfinding, actor dispatch, item lock, item transfer, automatic hauling, or production consumption.");
        out.add("Efficiency: selected/latest chain only, bounded history, no global scans, and no duplicate source/route authority.");
        return out;
    }

    private static String clean(String s, String fallback) { return s == null || s.isBlank() ? fallback : s.replace('\t',' ').replace('\n',' '); }
    private static String esc(String s) { return clean(s, "").replace("%", "%25").replace("\t", "%09").replace("\n", "%0A").replace(";", "%3B"); }
    private static String unesc(String s) { return s == null ? "" : s.replace("%3B", ";").replace("%0A", "\n").replace("%09", "\t").replace("%25", "%"); }
}
