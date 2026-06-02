package mechanist;

import java.util.*;

/**
 * 0.9.10k — Logistics Manual Haul Fulfillment Preflight.
 *
 * Performs a narrow, player-triggered readiness check against the latest manual
 * haul contract.  It deliberately does not move actors, lock items, reserve
 * routes, transfer goods, consume inputs, or let logistics own production.  The
 * goal is player feedback and API consolidation: one shared place answers
 * "could this contract be fulfilled if execution existed?".
 */
final class LogisticsHaulFulfillmentPreflightAuthority {
    static final String VERSION = "0.9.10k";
    static final int MAX_RECORDS = 24;
    static final int MAX_LINES = 10;

    private LogisticsHaulFulfillmentPreflightAuthority() {}

    static final class FulfillmentPreflightRecord {
        final int id;
        final int turn;
        final int contractId;
        final String stationName;
        final String workerLabel;
        final String status;
        final int readinessScore;
        final int fromX, fromY, toX, toY, estimatedSteps;
        final ArrayList<String> lines;

        FulfillmentPreflightRecord(int id, int turn, int contractId, String stationName, String workerLabel,
                                   String status, int readinessScore, int fromX, int fromY, int toX, int toY,
                                   int estimatedSteps, ArrayList<String> lines) {
            this.id = Math.max(0, id);
            this.turn = Math.max(0, turn);
            this.contractId = Math.max(0, contractId);
            this.stationName = clean(stationName, "unknown station");
            this.workerLabel = clean(workerLabel, "unassigned");
            this.status = clean(status, "preflight-record");
            this.readinessScore = Math.max(0, Math.min(100, readinessScore));
            this.fromX = fromX; this.fromY = fromY; this.toX = toX; this.toY = toY;
            this.estimatedSteps = Math.max(0, estimatedSteps);
            this.lines = lines == null ? new ArrayList<>() : new ArrayList<>(lines);
        }

        String compact() {
            return "Haul preflight #" + id + " turn " + turn + " contract #" + contractId + " "
                    + stationName + " worker=" + workerLabel + " readiness=" + readinessScore + "% status=" + status
                    + " " + fromX + "," + fromY + " -> " + toX + "," + toY + " estSteps=" + estimatedSteps;
        }

        String encode() {
            return id + "\t" + turn + "\t" + contractId + "\t" + esc(stationName) + "\t" + esc(workerLabel) + "\t"
                    + esc(status) + "\t" + readinessScore + "\t" + fromX + "\t" + fromY + "\t" + toX + "\t" + toY + "\t"
                    + estimatedSteps + "\t" + esc(String.join(";;", lines));
        }

        static FulfillmentPreflightRecord decode(String raw) {
            if (raw == null || raw.isBlank()) return null;
            String[] a = raw.split("\\t", -1);
            if (a.length < 13) return null;
            try {
                ArrayList<String> l = new ArrayList<>();
                String joined = unesc(a[12]);
                if (!joined.isBlank()) for (String s : joined.split(";;", -1)) if (!s.isBlank()) l.add(s);
                return new FulfillmentPreflightRecord(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]),
                        unesc(a[3]), unesc(a[4]), unesc(a[5]), Integer.parseInt(a[6]), Integer.parseInt(a[7]),
                        Integer.parseInt(a[8]), Integer.parseInt(a[9]), Integer.parseInt(a[10]), Integer.parseInt(a[11]), l);
            } catch (Exception ignored) { return null; }
        }
    }

    static String createPreflight(GamePanel g) {
        if (g == null) return "HAUL PREFLIGHT FAILED: no active game panel.";
        LogisticsManualHaulContractAuthority.ManualHaulContractRecord contract = g.logisticsHaulContractHistory.peekFirst();
        if (contract == null) return "HAUL PREFLIGHT FAILED: record a HAUL CONTRACT first.";

        ArrayList<String> lines = new ArrayList<>();
        int blockers = 0;
        int warnings = 0;

        // Reuse the existing chain instead of duplicating logistics authority.
        LogisticsDeliveryIntentAuthority.DeliveryIntentRecord intent = g.logisticsDeliveryIntentHistory.peekFirst();
        LogisticsSourceReservationAuthority.SourceReservationRecord source = g.logisticsSourceReservationHistory.peekFirst();
        LogisticsRouteIntentAuthority.RouteIntentRecord route = g.logisticsRouteIntentHistory.peekFirst();
        LogisticsRouteReadinessPreviewAuthority.ManualHaulPreviewRecord preview = g.logisticsRoutePreviewHistory.peekFirst();

        CheckResult chain = checkChain(contract, intent, source, route, preview); blockers += chain.blockers; warnings += chain.warnings; lines.addAll(chain.lines);
        CheckResult geometry = checkGeometry(g, contract); blockers += geometry.blockers; warnings += geometry.warnings; lines.addAll(geometry.lines);
        CheckResult worker = checkWorker(g, contract); blockers += worker.blockers; warnings += worker.warnings; lines.addAll(worker.lines);
        CheckResult sourceReady = checkSourceVisibility(source); blockers += sourceReady.blockers; warnings += sourceReady.warnings; lines.addAll(sourceReady.lines);

        if (contract.warnings != null) {
            for (String w : contract.warnings) {
                if (w == null || w.isBlank()) continue;
                if (w.startsWith("BLOCK:")) { blockers++; lines.add("BLOCK inherited from contract: " + w.substring(6).trim()); }
                else { warnings++; lines.add("WARN inherited from contract: " + w.replaceFirst("^WARN:\\s*", "")); }
            }
        }

        lines = cap(lines);
        int readiness = Math.max(0, Math.min(100, contract.readinessScore - blockers * 25 - warnings * 5));
        String status = blockers > 0 ? "blocked-preflight" : (warnings > 0 ? "warning-preflight" : "ready-preflight");
        int id = Math.max(1, g.nextLogisticsHaulPreflightSeq++);
        FulfillmentPreflightRecord rec = new FulfillmentPreflightRecord(id, g.turn, contract.id, contract.stationName,
                contract.workerLabel, status, readiness, contract.fromX, contract.fromY, contract.toX, contract.toY,
                contract.estimatedSteps, lines);
        remember(g, rec);
        return "MANUAL HAUL PREFLIGHT RECORDED: " + rec.compact() + ". "
                + blockers + " blocker(s), " + warnings + " warning(s). Preflight only: no pathfinding, actor movement, item lock, item transfer, hauling, or production.";
    }

    private static CheckResult checkChain(LogisticsManualHaulContractAuthority.ManualHaulContractRecord c,
                                          LogisticsDeliveryIntentAuthority.DeliveryIntentRecord intent,
                                          LogisticsSourceReservationAuthority.SourceReservationRecord source,
                                          LogisticsRouteIntentAuthority.RouteIntentRecord route,
                                          LogisticsRouteReadinessPreviewAuthority.ManualHaulPreviewRecord preview) {
        CheckResult r = new CheckResult();
        if (intent == null) r.block("No current delivery intent is available for this preflight.");
        else if (intent.id != c.intentId) r.warn("Latest delivery intent #" + intent.id + " differs from contract intent #" + c.intentId + "; refresh the contract chain for freshest feedback.");
        else r.ok("Delivery intent link is current (#" + intent.id + ").");
        if (source == null) r.block("No current source token is available.");
        else if (source.id != c.sourceTokenId) r.warn("Latest source token #" + source.id + " differs from contract source #" + c.sourceTokenId + ".");
        else r.ok("Source token link is current (#" + source.id + ").");
        if (route == null) r.block("No current route intent is available.");
        else if (route.id != c.routeIntentId) r.warn("Latest route intent #" + route.id + " differs from contract route #" + c.routeIntentId + ".");
        else r.ok("Route intent link is current (#" + route.id + ").");
        if (preview == null) r.block("No current haul preview is available.");
        else if (preview.id != c.previewId) r.warn("Latest haul preview #" + preview.id + " differs from contract preview #" + c.previewId + ".");
        else r.ok("Haul preview link is current (#" + preview.id + ").");
        return r;
    }

    private static CheckResult checkGeometry(GamePanel g, LogisticsManualHaulContractAuthority.ManualHaulContractRecord c) {
        CheckResult r = new CheckResult();
        if (g.world == null) { r.block("No loaded world exists for fulfillment preflight."); return r; }
        if (!g.world.inBounds(c.fromX, c.fromY)) r.block("Source anchor is out of current-zone bounds.");
        else if (!g.world.walkableAdjacentOrSame(c.fromX, c.fromY)) r.block("Source anchor has no walkable handling tile.");
        else r.ok("Source anchor has a walkable handling tile.");
        if (!g.world.inBounds(c.toX, c.toY)) r.block("Destination is out of current-zone bounds.");
        else if (!g.world.walkableAdjacentOrSame(c.toX, c.toY)) r.block("Destination has no walkable handling tile.");
        else r.ok("Destination has a walkable handling tile.");
        if (c.estimatedSteps <= 0) r.warn("Estimated route length is zero/unknown; route intent may need refreshing.");
        else if (c.estimatedSteps > 80) r.warn("Estimated route length is long for manual hauling (" + c.estimatedSteps + " steps).");
        else r.ok("Estimated route length is within ordinary manual-haul bounds.");
        return r;
    }

    private static CheckResult checkWorker(GamePanel g, LogisticsManualHaulContractAuthority.ManualHaulContractRecord c) {
        CheckResult r = new CheckResult();
        if (c.workerLabel == null || c.workerLabel.isBlank() || c.workerLabel.equals("unassigned")) {
            r.warn("No worker is assigned; fulfillment requires player/manual handling or a labor assignment.");
            return r;
        }
        RecruitWorker worker = null;
        for (RecruitWorker rw : g.factionRecruits) if (rw != null && c.workerLabel.equals(rw.name)) { worker = rw; break; }
        if (worker == null) r.warn("Assigned worker label is not currently present in the recruit roster: " + c.workerLabel + ".");
        else r.ok("Assigned worker exists in recruit roster: " + worker.name + " skill=" + worker.skill + " duty=" + worker.duty + ".");
        return r;
    }

    private static CheckResult checkSourceVisibility(LogisticsSourceReservationAuthority.SourceReservationRecord source) {
        CheckResult r = new CheckResult();
        if (source == null) { r.block("No source-token line can be inspected."); return r; }
        String status = source.status == null ? "" : source.status;
        if (status.contains("source-missing")) r.block("Source token reports missing visible inputs.");
        else if (status.contains("partial-source")) r.warn("Source token reports only partial visible inputs.");
        else r.ok("Source token reports status: " + status + ".");
        int shown = 0;
        for (String s : source.sourceLines) {
            if (shown >= 3) break;
            if (s != null && !s.isBlank()) { r.ok("source snapshot " + s); shown++; }
        }
        return r;
    }

    private static final class CheckResult {
        int blockers = 0, warnings = 0;
        final ArrayList<String> lines = new ArrayList<>();
        void block(String s) { blockers++; lines.add("BLOCK: " + s); }
        void warn(String s) { warnings++; lines.add("WARN: " + s); }
        void ok(String s) { lines.add("OK: " + s); }
    }

    private static ArrayList<String> cap(ArrayList<String> in) {
        if (in == null) return new ArrayList<>();
        if (in.size() <= MAX_LINES) return in;
        ArrayList<String> out = new ArrayList<>(in.subList(0, MAX_LINES));
        out.add("WARN: " + (in.size() - MAX_LINES) + " additional preflight line(s) hidden by bounded display.");
        return out;
    }

    static void remember(GamePanel g, FulfillmentPreflightRecord rec) {
        if (g == null || rec == null) return;
        g.logisticsHaulPreflightHistory.addFirst(rec);
        while (g.logisticsHaulPreflightHistory.size() > MAX_RECORDS) g.logisticsHaulPreflightHistory.removeLast();
    }

    static ArrayList<String> encodeHistory(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        if (g == null) return out;
        for (FulfillmentPreflightRecord rec : g.logisticsHaulPreflightHistory) if (rec != null) out.add(rec.encode());
        return out;
    }

    static void restoreHistory(GamePanel g, java.util.List<String> encoded) {
        if (g == null) return;
        g.logisticsHaulPreflightHistory.clear();
        int maxId = 0;
        if (encoded != null) {
            for (String raw : encoded) {
                FulfillmentPreflightRecord rec = FulfillmentPreflightRecord.decode(raw);
                if (rec != null) {
                    g.logisticsHaulPreflightHistory.add(rec);
                    maxId = Math.max(maxId, rec.id);
                    if (g.logisticsHaulPreflightHistory.size() >= MAX_RECORDS) break;
                }
            }
        }
        g.nextLogisticsHaulPreflightSeq = Math.max(g.nextLogisticsHaulPreflightSeq, maxId + 1);
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics Haul Fulfillment Preflight Authority " + VERSION + " — checks selected contract readiness before execution.");
        if (g == null) { out.add("  No active game panel."); return out; }
        out.add("  Stored preflights: " + g.logisticsHaulPreflightHistory.size() + "/" + MAX_RECORDS + ". Next id: " + g.nextLogisticsHaulPreflightSeq + ".");
        FulfillmentPreflightRecord latest = g.logisticsHaulPreflightHistory.peekFirst();
        if (latest == null) out.add("  No fulfillment preflight yet. Use HAUL CONTRACT, then PREFLIGHT.");
        else {
            out.add("  Latest: " + latest.compact());
            int shown = 0;
            for (String s : latest.lines) {
                if (shown >= 6) break;
                out.add("  " + s);
                shown++;
            }
            if (latest.lines.size() > shown) out.add("  ... " + (latest.lines.size() - shown) + " preflight line(s) hidden by bounded display.");
        }
        out.add("  Efficiency: latest contract only, adjacency/source/worker checks only, bounded history, and reuse of existing logistics records.");
        return out;
    }

    static String compactSummary(GamePanel g) {
        if (g == null) return "Logistics haul preflight: no active panel.";
        FulfillmentPreflightRecord rec = g.logisticsHaulPreflightHistory.peekFirst();
        return rec == null ? "Logistics haul preflight: none recorded yet." : "Logistics haul preflight: latest " + rec.compact();
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics Haul Fulfillment Preflight Authority " + VERSION);
        out.add("Purpose: answer whether the latest manual haul contract is plausibly fulfillable before actual hauling execution exists.");
        out.add("Current behavior: checks latest contract links, source status, walkable handling-adjacent tiles, worker presence, and inherited contract blockers.");
        out.add("Player feedback: reports blocker/warning/OK lines with readiness score and coordinates so failed logistics is explainable rather than silent.");
        out.add("Hard boundary: no pathfinding, actor dispatch, item locks, route locks, item transfer, automatic hauling, or production ownership.");
        out.add("Efficiency: one latest-contract check per player action, bounded history, no global scan, and no duplicate storage/route authority.");
        return out;
    }

    private static String clean(String s, String fallback) { return s == null || s.isBlank() ? fallback : s.replace('\t',' ').replace('\n',' '); }
    private static String esc(String s) { return clean(s, "").replace("%", "%25").replace("\t", "%09").replace("\n", "%0A").replace(";", "%3B"); }
    private static String unesc(String s) { return s == null ? "" : s.replace("%3B", ";").replace("%0A", "\n").replace("%09", "\t").replace("%25", "%"); }
}
