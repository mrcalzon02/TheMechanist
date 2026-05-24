package mechanist;

import java.util.*;

/**
 * 0.9.10h — Logistics Delivery Route Intent Display Bridge.
 *
 * Turns the latest delivery/source-token pair into a visible route-intent
 * estimate.  This is deliberately not pathfinding and not hauling: it records
 * a bounded, player-triggered display record using existing intent/source
 * authorities so hauling code has one route-contract surface to mature.
 */
final class LogisticsRouteIntentAuthority {
    static final String VERSION = "0.9.10ep";
    static final int MAX_RECORDS = 24;
    static final int MAX_ROUTE_LINES = 8;

    private LogisticsRouteIntentAuthority() {}

    static final class RouteIntentRecord {
        final int id;
        final int turn;
        final int intentId;
        final int sourceTokenId;
        final String stationName;
        final int fromX;
        final int fromY;
        final int toX;
        final int toY;
        final String sourceAnchor;
        final String routeClass;
        final String status;
        final int estimatedSteps;
        final ArrayList<String> routeLines;

        RouteIntentRecord(int id, int turn, int intentId, int sourceTokenId, String stationName,
                          int fromX, int fromY, int toX, int toY, String sourceAnchor,
                          String routeClass, String status, int estimatedSteps, ArrayList<String> routeLines) {
            this.id = Math.max(0, id);
            this.turn = Math.max(0, turn);
            this.intentId = Math.max(0, intentId);
            this.sourceTokenId = Math.max(0, sourceTokenId);
            this.stationName = clean(stationName, "unknown station");
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
            this.sourceAnchor = clean(sourceAnchor, "unknown-source");
            this.routeClass = clean(routeClass, "display-only");
            this.status = clean(status, "estimated");
            this.estimatedSteps = Math.max(0, estimatedSteps);
            this.routeLines = routeLines == null ? new ArrayList<>() : new ArrayList<>(routeLines);
        }

        String compact() {
            return "Route intent #" + id + " turn " + turn + " intent #" + intentId + " source #" + sourceTokenId
                    + " " + sourceAnchor + " -> " + stationName + "@" + toX + "," + toY
                    + " class=" + routeClass + " status=" + status + " estSteps=" + estimatedSteps;
        }

        String encode() {
            return id + "\t" + turn + "\t" + intentId + "\t" + sourceTokenId + "\t" + esc(stationName) + "\t"
                    + fromX + "\t" + fromY + "\t" + toX + "\t" + toY + "\t" + esc(sourceAnchor) + "\t"
                    + esc(routeClass) + "\t" + esc(status) + "\t" + estimatedSteps + "\t" + esc(String.join(";;", routeLines));
        }

        static RouteIntentRecord decode(String raw) {
            if (raw == null || raw.isBlank()) return null;
            String[] a = raw.split("\\t", -1);
            if (a.length < 14) return null;
            try {
                ArrayList<String> lines = new ArrayList<>();
                String joined = unesc(a[13]);
                if (!joined.isBlank()) for (String s : joined.split(";;", -1)) if (!s.isBlank()) lines.add(s);
                return new RouteIntentRecord(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]),
                        unesc(a[4]), Integer.parseInt(a[5]), Integer.parseInt(a[6]), Integer.parseInt(a[7]), Integer.parseInt(a[8]),
                        unesc(a[9]), unesc(a[10]), unesc(a[11]), Integer.parseInt(a[12]), lines);
            } catch (Exception ignored) { return null; }
        }
    }

    static String createRouteIntent(GamePanel g) {
        if (g == null) return "ROUTE INTENT FAILED: no active game panel.";
        LogisticsDeliveryIntentAuthority.DeliveryIntentRecord intent = g.logisticsDeliveryIntentHistory.peekFirst();
        if (intent == null) return "ROUTE INTENT FAILED: record a logistics INTENT first.";
        LogisticsSourceReservationAuthority.SourceReservationRecord source = g.logisticsSourceReservationHistory.peekFirst();
        if (source == null) return "ROUTE INTENT FAILED: record a SOURCE TOKEN first.";
        if (source.intentId != intent.id) return "ROUTE INTENT FAILED: latest source token belongs to intent #" + source.intentId + ", but latest intent is #" + intent.id + ". Refresh SOURCE TOKEN.";

        Anchor anchor = chooseAnchor(g, source);
        int toX = intent.stationX;
        int toY = intent.stationY;
        int steps = (anchor.valid && toX >= 0 && toY >= 0) ? Math.abs(anchor.x - toX) + Math.abs(anchor.y - toY) : 0;
        String status;
        if (!anchor.valid) status = "no-visible-source-anchor";
        else if (toX < 0 || toY < 0) status = "no-station-coordinate";
        else if (steps == 0) status = "same-tile-or-unknown";
        else status = "display-estimate-ready";
        String routeClass = anchor.routeClass;
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Anchor=" + anchor.label + " from=" + anchor.x + "," + anchor.y + " to=" + toX + "," + toY + " manhattan=" + steps);
        int shown = 0;
        for (String s : source.sourceLines) {
            if (shown >= MAX_ROUTE_LINES - 1) break;
            if (s == null || s.isBlank()) continue;
            lines.add("source " + s);
            shown++;
        }
        if (source.sourceLines.size() > shown) lines.add("bounded_display_hidden=" + (source.sourceLines.size() - shown));
        lines.addAll(EconomicTopologyPreviewConsumerAuthority.routeIntentLines(g, anchor.x, anchor.y, toX, toY));

        int id = Math.max(1, g.nextLogisticsRouteIntentSeq++);
        RouteIntentRecord rec = new RouteIntentRecord(id, g.turn, intent.id, source.id, intent.stationName,
                anchor.x, anchor.y, toX, toY, anchor.label, routeClass, status, steps, lines);
        remember(g, rec);
        return "LOGISTICS ROUTE INTENT DISPLAYED: " + rec.compact() + ". Display only: no pathfinding, route lock, hauling, transfer, or production.";
    }

    private static Anchor chooseAnchor(GamePanel g, LogisticsSourceReservationAuthority.SourceReservationRecord source) {
        if (source != null && source.sourceLines != null) {
            for (String line : source.sourceLines) {
                if (line == null) continue;
                if (line.contains(GamePanel.CONTAINER_BASE_STORAGE)) return new Anchor(true, g.baseX, g.baseY, GamePanel.CONTAINER_BASE_STORAGE, "base-storage-to-machine");
                if (line.contains(GamePanel.CONTAINER_PLAYER_INVENTORY)) return new Anchor(true, g.playerX, g.playerY, GamePanel.CONTAINER_PLAYER_INVENTORY, "carried-to-machine");
            }
        }
        if (g.baseClaimed && g.baseX >= 0 && g.baseY >= 0) return new Anchor(true, g.baseX, g.baseY, "base-anchor-fallback", "fallback-base-to-machine");
        return new Anchor(false, -1, -1, "no-visible-source-anchor", "unroutable-display");
    }

    private static final class Anchor {
        final boolean valid; final int x; final int y; final String label; final String routeClass;
        Anchor(boolean valid, int x, int y, String label, String routeClass) { this.valid = valid; this.x = x; this.y = y; this.label = label; this.routeClass = routeClass; }
    }

    static void remember(GamePanel g, RouteIntentRecord rec) {
        if (g == null || rec == null) return;
        g.logisticsRouteIntentHistory.addFirst(rec);
        while (g.logisticsRouteIntentHistory.size() > MAX_RECORDS) g.logisticsRouteIntentHistory.removeLast();
    }

    static ArrayList<String> encodeHistory(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        if (g == null) return out;
        for (RouteIntentRecord rec : g.logisticsRouteIntentHistory) if (rec != null) out.add(rec.encode());
        return out;
    }

    static void restoreHistory(GamePanel g, java.util.List<String> encoded) {
        if (g == null) return;
        g.logisticsRouteIntentHistory.clear();
        int maxId = 0;
        if (encoded != null) {
            for (String raw : encoded) {
                RouteIntentRecord rec = RouteIntentRecord.decode(raw);
                if (rec != null) {
                    g.logisticsRouteIntentHistory.add(rec);
                    maxId = Math.max(maxId, rec.id);
                    if (g.logisticsRouteIntentHistory.size() >= MAX_RECORDS) break;
                }
            }
        }
        g.nextLogisticsRouteIntentSeq = Math.max(g.nextLogisticsRouteIntentSeq, maxId + 1);
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics Route Intent Authority " + VERSION + " — records selected route intent and handling anchors for manual hauling.");
        if (g == null) { out.add("  No active game panel."); return out; }
        out.add("  Stored route intents: " + g.logisticsRouteIntentHistory.size() + "/" + MAX_RECORDS + ". Next id: " + g.nextLogisticsRouteIntentSeq + ".");
        RouteIntentRecord latest = g.logisticsRouteIntentHistory.peekFirst();
        if (latest == null) out.add("  No route intent displayed yet. Use INTENT, SOURCE TOKEN, then ROUTE INTENT.");
        else {
            out.add("  Latest: " + latest.compact());
            int shown = 0;
            for (String s : latest.routeLines) {
                if (shown >= 5) break;
                out.add("  route " + s);
                shown++;
            }
            if (latest.routeLines.size() > shown) out.add("  ... " + (latest.routeLines.size() - shown) + " route line(s) hidden by bounded display.");
        }
        out.add("  Efficiency: latest-intent/source-token only, Manhattan estimate only, bounded history, and no duplicated source or forecast logic.");
        return out;
    }

    static String compactSummary(GamePanel g) {
        if (g == null) return "Logistics route intent: no active panel.";
        RouteIntentRecord rec = g.logisticsRouteIntentHistory.peekFirst();
        return rec == null ? "Logistics route intent: none displayed yet." : "Logistics route intent: latest " + rec.compact();
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics Route Intent Authority " + VERSION);
        out.add("Purpose: give hauling a single visible route-intent display contract after delivery intent and source tokens exist.");
        out.add("Current behavior: selected/latest intent only, source-token only, estimated anchor-to-station display only.");
        out.add("Player feedback: reports source anchor, destination machine, route class, status, estimated Manhattan steps, and cached local topology context when available.");
        out.add("Hard boundary: no pathfinding, route reservation, actor movement, item transfer, item lock, production consumption, or autonomous hauling.");
        out.add("Efficiency: no global route scan, no turn-loop route worker, no map flood fill; this reuses delivery/source authorities and the cached local topology surface.");
        return out;
    }

    private static String clean(String s, String fallback) { return s == null || s.isBlank() ? fallback : s.replace('\t',' ').replace('\n',' '); }
    private static String esc(String s) { return clean(s, "").replace("%", "%25").replace("\t", "%09").replace("\n", "%0A").replace(";", "%3B"); }
    private static String unesc(String s) { return s == null ? "" : s.replace("%3B", ";").replace("%0A", "\n").replace("%09", "\t").replace("%25", "%"); }
}
