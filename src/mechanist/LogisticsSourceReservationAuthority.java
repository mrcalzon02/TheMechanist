package mechanist;

import java.util.*;

/**
 * Records a bounded, player-triggered source-token snapshot from the latest
 * logistics delivery intent and its visible storage candidates. The token is a
 * visible execution prerequisite, not an item lock or autonomous hauling order.
 */
final class LogisticsSourceReservationAuthority {
    static final String VERSION = "0.9.10g";
    static final int MAX_RECORDS = 24;
    static final int MAX_ITEM_LINES = 8;

    private LogisticsSourceReservationAuthority() {}

    static final class SourceReservationRecord {
        final int id;
        final int turn;
        final int intentId;
        final String stationName;
        final String jobLabel;
        final String status;
        final ArrayList<String> sourceLines;

        SourceReservationRecord(int id, int turn, int intentId, String stationName, String jobLabel, String status, ArrayList<String> sourceLines) {
            this.id = Math.max(0, id);
            this.turn = Math.max(0, turn);
            this.intentId = Math.max(0, intentId);
            this.stationName = clean(stationName, "unknown station");
            this.jobLabel = clean(jobLabel, "unknown job");
            this.status = clean(status, "snapshot-only");
            this.sourceLines = sourceLines == null ? new ArrayList<>() : new ArrayList<>(sourceLines);
        }

        String compact() {
            return "Source token #" + id + " turn " + turn + " intent #" + intentId + " " + stationName + " job=" + jobLabel + " status=" + status + " lines=" + sourceLines.size();
        }

        String encode() {
            return id + "\t" + turn + "\t" + intentId + "\t" + esc(stationName) + "\t" + esc(jobLabel) + "\t" + esc(status) + "\t" + esc(String.join(";;", sourceLines));
        }

        static SourceReservationRecord decode(String raw) {
            if (raw == null || raw.isBlank()) return null;
            String[] a = raw.split("\\t", -1);
            if (a.length < 7) return null;
            try {
                ArrayList<String> lines = new ArrayList<>();
                String joined = unesc(a[6]);
                if (!joined.isBlank()) for (String s : joined.split(";;", -1)) if (!s.isBlank()) lines.add(s);
                return new SourceReservationRecord(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]), unesc(a[3]), unesc(a[4]), unesc(a[5]), lines);
            } catch (Exception ignored) { return null; }
        }
    }

    static String createSourceReservation(GamePanel g) {
        if (g == null) return "SOURCE RESERVATION FAILED: no active game panel.";
        LogisticsDeliveryIntentAuthority.DeliveryIntentRecord intent = g.logisticsDeliveryIntentHistory.peekFirst();
        if (intent == null) return "SOURCE RESERVATION FAILED: record a logistics INTENT first.";

        ArrayList<LogisticsStorageCandidateAuthority.CandidateLine> candidates = LogisticsStorageCandidateAuthority.latestCandidateLines(g);
        ArrayList<String> lines = new ArrayList<>();
        int ready = 0;
        int missing = 0;
        int shown = 0;
        for (LogisticsStorageCandidateAuthority.CandidateLine c : candidates) {
            if (shown >= MAX_ITEM_LINES) break;
            if (c == null) continue;
            if (c.ready()) ready++;
            missing += c.missing();
            String preferred;
            if (c.baseStorage >= c.need && c.need > 0) preferred = GamePanel.CONTAINER_BASE_STORAGE;
            else if (c.carried >= c.need && c.need > 0) preferred = GamePanel.CONTAINER_PLAYER_INVENTORY;
            else if (c.baseStorage > 0) preferred = GamePanel.CONTAINER_BASE_STORAGE + "+partial";
            else if (c.carried > 0) preferred = GamePanel.CONTAINER_PLAYER_INVENTORY + "+partial";
            else preferred = "no-visible-source";
            lines.add(c.item + ": need=" + c.need + " preferred=" + preferred + " base=" + c.baseStorage + " carried=" + c.carried + " missing=" + c.missing());
            shown++;
        }
        if (candidates.size() > shown) lines.add("bounded_display_hidden=" + (candidates.size() - shown));
        String status;
        if (candidates.isEmpty()) status = "no-inputs-required";
        else if (missing == 0) status = "source-ready";
        else if (ready > 0) status = "partial-source";
        else status = "source-missing";

        int id = Math.max(1, g.nextLogisticsSourceReservationSeq++);
        SourceReservationRecord rec = new SourceReservationRecord(id, g.turn, intent.id, intent.stationName, intent.jobLabel, status, lines);
        remember(g, rec);
        return "LOGISTICS SOURCE TOKEN RECORDED: " + rec.compact() + ". Missing units=" + missing + ". Snapshot only: no item lock, route lock, actor movement, transfer, or production.";
    }

    static void remember(GamePanel g, SourceReservationRecord rec) {
        if (g == null || rec == null) return;
        g.logisticsSourceReservationHistory.addFirst(rec);
        while (g.logisticsSourceReservationHistory.size() > MAX_RECORDS) g.logisticsSourceReservationHistory.removeLast();
    }

    static ArrayList<String> encodeHistory(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        if (g == null) return out;
        for (SourceReservationRecord rec : g.logisticsSourceReservationHistory) if (rec != null) out.add(rec.encode());
        return out;
    }

    static void restoreHistory(GamePanel g, java.util.List<String> encoded) {
        if (g == null) return;
        g.logisticsSourceReservationHistory.clear();
        int maxId = 0;
        if (encoded != null) {
            for (String raw : encoded) {
                SourceReservationRecord rec = SourceReservationRecord.decode(raw);
                if (rec != null) {
                    g.logisticsSourceReservationHistory.add(rec);
                    maxId = Math.max(maxId, rec.id);
                    if (g.logisticsSourceReservationHistory.size() >= MAX_RECORDS) break;
                }
            }
        }
        g.nextLogisticsSourceReservationSeq = Math.max(g.nextLogisticsSourceReservationSeq, maxId + 1);
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics Source Reservation Authority " + VERSION + " — records source-token snapshots for selected station needs.");
        if (g == null) { out.add("  No active game panel."); return out; }
        out.add("  Stored source tokens: " + g.logisticsSourceReservationHistory.size() + "/" + MAX_RECORDS + ". Next id: " + g.nextLogisticsSourceReservationSeq + ".");
        SourceReservationRecord latest = g.logisticsSourceReservationHistory.peekFirst();
        if (latest == null) {
            out.add("  No source token recorded yet. Use INTENT, review source candidates, then SOURCE TOKEN.");
        } else {
            out.add("  Latest: " + latest.compact());
            int shown = 0;
            for (String s : latest.sourceLines) {
                if (shown >= 5) break;
                out.add("  token " + s);
                shown++;
            }
            if (latest.sourceLines.size() > shown) out.add("  ... " + (latest.sourceLines.size() - shown) + " token line(s) hidden by bounded display.");
        }
        out.add("  Efficiency: selected/latest intent only, bounded token history, no global storage sweep, no pathfinding, and no duplicated input forecast authority.");
        return out;
    }

    static String compactSummary(GamePanel g) {
        if (g == null) return "Logistics source token: no active panel.";
        SourceReservationRecord rec = g.logisticsSourceReservationHistory.peekFirst();
        return rec == null ? "Logistics source token: none recorded yet." : "Logistics source token: latest " + rec.compact();
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics Source Reservation Authority " + VERSION);
        out.add("Purpose: give hauling/storage systems one shared source-token format after delivery intent and source-candidate display are proven stable.");
        out.add("Current behavior: records a bounded snapshot of preferred source containers for the latest logistics intent.");
        out.add("Player feedback: reports source-ready, partial-source, source-missing, or no-inputs-required with per-item base/carried availability.");
        out.add("Hard boundary: no item-instance lock, route reservation, actor movement, item transfer, production consumption, or autonomous hauling.");
        out.add("Efficiency: latest-intent only, up to " + MAX_ITEM_LINES + " input lines, bounded history, and reuse of the storage-candidate authority.");
        return out;
    }

    private static String clean(String s, String fallback) { return s == null || s.isBlank() ? fallback : s.replace('\t',' ').replace('\n',' '); }
    private static String esc(String s) { return clean(s, "").replace("%", "%25").replace("\t", "%09").replace("\n", "%0A").replace(";", "%3B"); }
    private static String unesc(String s) { return s == null ? "" : s.replace("%3B", ";").replace("%0A", "\n").replace("%09", "\t").replace("%25", "%"); }
}
