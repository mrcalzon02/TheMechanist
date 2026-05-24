package mechanist;

import java.util.*;

/**
 * 0.9.10i — Logistics Route Readiness / Manual Haul Preview Bridge.
 *
 * Reads the latest route-intent record and converts it into bounded player
 * feedback: whether a manual haul order would currently have enough visible
 * information to be issued later. This is deliberately not pathfinding, not
 * actor dispatch, not item movement, and not production ownership.
 */
final class LogisticsRouteReadinessPreviewAuthority {
    static final String VERSION = "0.9.10ep";
    static final int MAX_RECORDS = 24;
    static final int MAX_WARNINGS = 8;

    private LogisticsRouteReadinessPreviewAuthority() {}

    static final class ManualHaulPreviewRecord {
        final int id;
        final int turn;
        final int routeIntentId;
        final String stationName;
        final int fromX;
        final int fromY;
        final int toX;
        final int toY;
        final int estimatedSteps;
        final int readinessScore;
        final String status;
        final ArrayList<String> warnings;

        ManualHaulPreviewRecord(int id, int turn, int routeIntentId, String stationName,
                                int fromX, int fromY, int toX, int toY, int estimatedSteps,
                                int readinessScore, String status, ArrayList<String> warnings) {
            this.id = Math.max(0, id);
            this.turn = Math.max(0, turn);
            this.routeIntentId = Math.max(0, routeIntentId);
            this.stationName = clean(stationName, "unknown station");
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
            this.estimatedSteps = Math.max(0, estimatedSteps);
            this.readinessScore = Math.max(0, Math.min(100, readinessScore));
            this.status = clean(status, "preview-only");
            this.warnings = warnings == null ? new ArrayList<>() : new ArrayList<>(warnings);
        }

        String compact() {
            return "Manual haul preview #" + id + " turn " + turn + " route #" + routeIntentId
                    + " " + fromX + "," + fromY + " -> " + stationName + "@" + toX + "," + toY
                    + " estSteps=" + estimatedSteps + " readiness=" + readinessScore + "% status=" + status;
        }

        String encode() {
            return id + "\t" + turn + "\t" + routeIntentId + "\t" + esc(stationName) + "\t"
                    + fromX + "\t" + fromY + "\t" + toX + "\t" + toY + "\t" + estimatedSteps + "\t"
                    + readinessScore + "\t" + esc(status) + "\t" + esc(String.join(";;", warnings));
        }

        static ManualHaulPreviewRecord decode(String raw) {
            if (raw == null || raw.isBlank()) return null;
            String[] a = raw.split("\\t", -1);
            if (a.length < 12) return null;
            try {
                ArrayList<String> warns = new ArrayList<>();
                String joined = unesc(a[11]);
                if (!joined.isBlank()) for (String s : joined.split(";;", -1)) if (!s.isBlank()) warns.add(s);
                return new ManualHaulPreviewRecord(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]),
                        unesc(a[3]), Integer.parseInt(a[4]), Integer.parseInt(a[5]), Integer.parseInt(a[6]), Integer.parseInt(a[7]),
                        Integer.parseInt(a[8]), Integer.parseInt(a[9]), unesc(a[10]), warns);
            } catch (Exception ignored) { return null; }
        }
    }

    static String createManualHaulPreview(GamePanel g) {
        if (g == null) return "HAUL PREVIEW FAILED: no active game panel.";
        LogisticsRouteIntentAuthority.RouteIntentRecord route = g.logisticsRouteIntentHistory.peekFirst();
        if (route == null) return "HAUL PREVIEW FAILED: display a ROUTE INTENT first.";

        ArrayList<String> warnings = evaluateWarnings(g, route);
        int critical = 0;
        for (String w : warnings) if (w != null && w.startsWith("BLOCK:")) critical++;
        int readiness = Math.max(0, 100 - critical * 35 - Math.max(0, warnings.size() - critical) * 10);
        String status = critical > 0 ? "blocked-preview" : (warnings.isEmpty() ? "ready-preview" : "warning-preview");
        int id = Math.max(1, g.nextLogisticsRoutePreviewSeq++);
        ManualHaulPreviewRecord rec = new ManualHaulPreviewRecord(id, g.turn, route.id, route.stationName,
                route.fromX, route.fromY, route.toX, route.toY, route.estimatedSteps, readiness, status, warnings);
        remember(g, rec);
        return "MANUAL HAUL ORDER PREVIEW: " + rec.compact() + ". "
                + (warnings.isEmpty() ? "No warnings." : warnings.size() + " warning(s); inspect LOGISTICS status for detail.")
                + " Preview only: no pathfinding, actor dispatch, item lock, transfer, or production.";
    }

    private static ArrayList<String> evaluateWarnings(GamePanel g, LogisticsRouteIntentAuthority.RouteIntentRecord r) {
        ArrayList<String> out = new ArrayList<>();
        if (g.world == null) { out.add("BLOCK: no loaded world for route preview."); return cap(out); }
        if (!g.world.inBounds(r.fromX, r.fromY)) out.add("BLOCK: source anchor is outside current zone bounds.");
        if (!g.world.inBounds(r.toX, r.toY)) out.add("BLOCK: destination machine coordinate is outside current zone bounds.");
        if (g.world.inBounds(r.fromX, r.fromY) && !g.world.walkableAdjacentOrSame(r.fromX, r.fromY)) out.add("BLOCK: source anchor has no walkable adjacent handling tile.");
        if (g.world.inBounds(r.toX, r.toY) && !g.world.walkableAdjacentOrSame(r.toX, r.toY)) out.add("BLOCK: destination has no walkable adjacent handling tile.");
        if (r.estimatedSteps <= 0) out.add("WARN: route estimate is zero or unknown; source and target may overlap or coordinates may be incomplete.");
        if (r.estimatedSteps > 80) out.add("WARN: route estimate is long for manual hauling; hauling should prefer a closer storage anchor.");
        if (!baseObjectExistsAt(g, r.toX, r.toY)) out.add("WARN: destination machine/station is not visible in current base-object registry at that coordinate.");
        out.addAll(EconomicTopologyPreviewConsumerAuthority.routeReadinessWarnings(g, r));
        LogisticsSourceReservationAuthority.SourceReservationRecord src = g.logisticsSourceReservationHistory.peekFirst();
        if (src == null) out.add("BLOCK: no current source token remains available.");
        else if (src.id != r.sourceTokenId) out.add("WARN: latest source token #" + src.id + " differs from route source #" + r.sourceTokenId + "; refresh route intent for freshest feedback.");
        LogisticsDeliveryIntentAuthority.DeliveryIntentRecord intent = g.logisticsDeliveryIntentHistory.peekFirst();
        if (intent == null) out.add("BLOCK: no current delivery intent remains available.");
        else if (intent.id != r.intentId) out.add("WARN: latest delivery intent #" + intent.id + " differs from route intent #" + r.intentId + "; refresh route intent for freshest feedback.");
        return cap(out);
    }

    private static boolean baseObjectExistsAt(GamePanel g, int x, int y) {
        if (g == null) return false;
        for (BaseObject b : g.baseObjects) if (b != null && b.x == x && b.y == y) return true;
        return false;
    }

    private static ArrayList<String> cap(ArrayList<String> in) {
        if (in == null || in.size() <= MAX_WARNINGS) return in == null ? new ArrayList<>() : in;
        ArrayList<String> out = new ArrayList<>(in.subList(0, MAX_WARNINGS));
        out.add("WARN: " + (in.size() - MAX_WARNINGS) + " additional warning(s) hidden by bounded display.");
        return out;
    }

    static void remember(GamePanel g, ManualHaulPreviewRecord rec) {
        if (g == null || rec == null) return;
        g.logisticsRoutePreviewHistory.addFirst(rec);
        while (g.logisticsRoutePreviewHistory.size() > MAX_RECORDS) g.logisticsRoutePreviewHistory.removeLast();
    }

    static ArrayList<String> encodeHistory(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        if (g == null) return out;
        for (ManualHaulPreviewRecord rec : g.logisticsRoutePreviewHistory) if (rec != null) out.add(rec.encode());
        return out;
    }

    static void restoreHistory(GamePanel g, java.util.List<String> encoded) {
        if (g == null) return;
        g.logisticsRoutePreviewHistory.clear();
        int maxId = 0;
        if (encoded != null) {
            for (String raw : encoded) {
                ManualHaulPreviewRecord rec = ManualHaulPreviewRecord.decode(raw);
                if (rec != null) {
                    g.logisticsRoutePreviewHistory.add(rec);
                    maxId = Math.max(maxId, rec.id);
                    if (g.logisticsRoutePreviewHistory.size() >= MAX_RECORDS) break;
                }
            }
        }
        g.nextLogisticsRoutePreviewSeq = Math.max(g.nextLogisticsRoutePreviewSeq, maxId + 1);
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics Route Readiness / Manual Haul Preview Authority " + VERSION + " — reports path and readiness warnings for selected manual haul orders.");
        if (g == null) { out.add("  No active game panel."); return out; }
        out.add("  Stored haul previews: " + g.logisticsRoutePreviewHistory.size() + "/" + MAX_RECORDS + ". Next id: " + g.nextLogisticsRoutePreviewSeq + ".");
        ManualHaulPreviewRecord latest = g.logisticsRoutePreviewHistory.peekFirst();
        if (latest == null) out.add("  No haul preview yet. Use INTENT, SOURCE TOKEN, ROUTE INTENT, then HAUL PREVIEW.");
        else {
            out.add("  Latest: " + latest.compact());
            if (latest.warnings.isEmpty()) out.add("  warnings none");
            else for (String w : latest.warnings) out.add("  " + w);
        }
        out.add("  Efficiency: selected/latest route only, adjacency checks only, bounded warning list, and no duplicated logistics forecast/source authority.");
        return out;
    }

    static String compactSummary(GamePanel g) {
        if (g == null) return "Manual haul preview: no active panel.";
        ManualHaulPreviewRecord rec = g.logisticsRoutePreviewHistory.peekFirst();
        return rec == null ? "Manual haul preview: none recorded yet." : "Manual haul preview: latest " + rec.compact();
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics Route Readiness / Manual Haul Preview Authority " + VERSION);
        out.add("Purpose: give the player a clear, low-cost answer about whether the latest displayed route is ready for a manual haul order.");
        out.add("Current behavior: checks only the latest route intent and reports missing anchors, stale tokens, unreachable handling-adjacent tiles, long-route warnings, and cached topology-fit cautions.");
        out.add("Hard boundary: no pathfinding, actor dispatch, item locks, item transfer, automatic hauling, or production ownership.");
        out.add("Efficiency: no global storage scan, no route flood fill, no turn-loop worker; this reuses existing delivery intent, source token, route intent, and cached local topology authorities.");
        return out;
    }

    private static String clean(String s, String fallback) { return s == null || s.isBlank() ? fallback : s.replace('\t',' ').replace('\n',' '); }
    private static String esc(String s) { return clean(s, "").replace("%", "%25").replace("\t", "%09").replace("\n", "%0A").replace(";", "%3B"); }
    private static String unesc(String s) { return s == null ? "" : s.replace("%3B", ";").replace("%0A", "\n").replace("%09", "\t").replace("%25", "%"); }
}
