package mechanist;

import java.util.*;

/**
 * Logistics delivery-intent authority.
 *
 * Creates bounded, player-triggered intent records from the selected-machine
 * logistics forecast. Records are visible and saveable without moving actors,
 * locking routes, transferring items, consuming inputs, or running production.
 */
final class LogisticsDeliveryIntentAuthority {
    static final String VERSION = "0.9.10e";
    static final int MAX_RECORDS = 24;
    static final int MAX_DISPLAY_LINES = 8;

    private LogisticsDeliveryIntentAuthority() {}

    static final class DeliveryIntentRecord {
        final int id;
        final int turn;
        final String stationName;
        final int stationX;
        final int stationY;
        final String jobLabel;
        final String workerLabel;
        final String sourceLabel;
        final String status;
        final int missingKinds;
        final int missingUnits;
        final ArrayList<String> tokenLines;

        DeliveryIntentRecord(int id, int turn, BaseObject station, String jobLabel, String workerLabel,
                             String sourceLabel, String status, int missingKinds, int missingUnits,
                             ArrayList<String> tokenLines) {
            this.id = Math.max(0, id);
            this.turn = Math.max(0, turn);
            this.stationName = station == null || station.name == null ? "none" : station.name;
            this.stationX = station == null ? -1 : station.x;
            this.stationY = station == null ? -1 : station.y;
            this.jobLabel = clean(jobLabel, "none");
            this.workerLabel = clean(workerLabel, "unassigned");
            this.sourceLabel = clean(sourceLabel, "unknown");
            this.status = clean(status, "forecast-only");
            this.missingKinds = Math.max(0, missingKinds);
            this.missingUnits = Math.max(0, missingUnits);
            this.tokenLines = tokenLines == null ? new ArrayList<>() : new ArrayList<>(tokenLines);
        }

        String compact() {
            return "Intent #" + id + " turn " + turn + " " + stationName + "@" + stationX + "," + stationY
                    + " job=" + jobLabel + " worker=" + workerLabel + " status=" + status
                    + " missing=" + missingKinds + "/" + missingUnits;
        }

        String encode() {
            return id + "\t" + turn + "\t" + esc(stationName) + "\t" + stationX + "\t" + stationY + "\t"
                    + esc(jobLabel) + "\t" + esc(workerLabel) + "\t" + esc(sourceLabel) + "\t"
                    + esc(status) + "\t" + missingKinds + "\t" + missingUnits + "\t" + esc(String.join(";;", tokenLines));
        }

        static DeliveryIntentRecord decode(String raw) {
            if (raw == null || raw.isBlank()) return null;
            String[] a = raw.split("\\t", -1);
            if (a.length < 12) return null;
            try {
                BaseObject stub = new BaseObject(unesc(a[2]), '?', Integer.parseInt(a[3]), Integer.parseInt(a[4]), 0, 0);
                ArrayList<String> lines = new ArrayList<>();
                String joined = unesc(a[11]);
                if (!joined.isBlank()) for (String s : joined.split(";;", -1)) if (!s.isBlank()) lines.add(s);
                return new DeliveryIntentRecord(Integer.parseInt(a[0]), Integer.parseInt(a[1]), stub,
                        unesc(a[5]), unesc(a[6]), unesc(a[7]), unesc(a[8]),
                        Integer.parseInt(a[9]), Integer.parseInt(a[10]), lines);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    static String createIntent(GamePanel g, BaseObject station) {
        if (g == null) return "LOGISTICS INTENT FAILED: no active game panel.";
        LogisticsReservationForecastAuthority.ReservationForecast f = LogisticsReservationForecastAuthority.forecast(g, station);
        ArrayList<String> tokens = new ArrayList<>();
        int shown = 0;
        for (LogisticsReservationForecastAuthority.ReservationLine l : f.lines) {
            if (shown >= MAX_DISPLAY_LINES) break;
            tokens.add(l.item + ": need=" + l.need + " have=" + l.have + " missing=" + l.missing);
            shown++;
        }
        if (f.lines.size() > shown) tokens.add("bounded_display_hidden=" + (f.lines.size() - shown));
        String worker = station == null || station.assignedWorker == null || station.assignedWorker.isBlank() ? "unassigned" : station.assignedWorker;
        String status;
        if (!f.validStation) status = "invalid-station";
        else if (!f.hasJob) status = "no-job";
        else if (!f.assignedWorker) status = "needs-worker";
        else if (!f.allInputsReady) status = "missing-inputs";
        else status = "ready-intent";
        int id = Math.max(1, g.nextLogisticsIntentSeq++);
        DeliveryIntentRecord rec = new DeliveryIntentRecord(id, g.turn, station, f.jobLabel, worker, f.sourceLabel,
                status, f.missingKinds(), f.totalMissingUnits(), tokens);
        remember(g, rec);
        String report = "LOGISTICS DELIVERY INTENT RECORDED: " + rec.compact() + ". No goods moved; no actors routed; no production consumed.";
        return report;
    }

    static void remember(GamePanel g, DeliveryIntentRecord rec) {
        if (g == null || rec == null) return;
        g.logisticsDeliveryIntentHistory.addFirst(rec);
        while (g.logisticsDeliveryIntentHistory.size() > MAX_RECORDS) g.logisticsDeliveryIntentHistory.removeLast();
    }

    static ArrayList<String> encodeHistory(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        if (g == null) return out;
        for (DeliveryIntentRecord rec : g.logisticsDeliveryIntentHistory) if (rec != null) out.add(rec.encode());
        return out;
    }

    static void restoreHistory(GamePanel g, java.util.List<String> encoded) {
        if (g == null) return;
        g.logisticsDeliveryIntentHistory.clear();
        int maxId = 0;
        if (encoded != null) {
            for (String raw : encoded) {
                DeliveryIntentRecord rec = DeliveryIntentRecord.decode(raw);
                if (rec != null) {
                    g.logisticsDeliveryIntentHistory.add(rec);
                    maxId = Math.max(maxId, rec.id);
                    if (g.logisticsDeliveryIntentHistory.size() >= MAX_RECORDS) break;
                }
            }
        }
        g.nextLogisticsIntentSeq = Math.max(g.nextLogisticsIntentSeq, maxId + 1);
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics Delivery Intent Authority " + VERSION + " — records player-triggered delivery intent tokens for selected station supply.");
        if (g == null) { out.add("  No active game panel."); return out; }
        out.add("  Stored intents: " + g.logisticsDeliveryIntentHistory.size() + "/" + MAX_RECORDS + ". Next id: " + g.nextLogisticsIntentSeq + ".");
        int shown = 0;
        for (DeliveryIntentRecord rec : g.logisticsDeliveryIntentHistory) {
            if (shown >= 5) break;
            out.add("  " + rec.compact());
            shown++;
        }
        if (g.logisticsDeliveryIntentHistory.size() > shown) out.add("  ... " + (g.logisticsDeliveryIntentHistory.size() - shown) + " older intent(s) hidden by bounded display.");
        out.add("  Efficiency: no global storage sweep, no pathfinding, no route locks; records reuse the selected-machine forecast authority.");
        return out;
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics Delivery Intent Authority " + VERSION);
        out.add("Purpose: create one common token format for hauling/storage readiness records.");
        out.add("Current behavior: the player records a selected-machine delivery intent from the existing logistics forecast.");
        out.add("Player feedback: each intent explains station, job, worker, readiness, missing input kinds, and missing units.");
        out.add("Hard boundary: no item transfer, route lock, actor movement, production consumption, or autonomous hauling.");
        out.add("Efficiency: bounded history, selected-machine only, no per-turn logistics processing, and no duplicated logistics authority.");
        return out;
    }

    static String compactSummary(GamePanel g) {
        if (g == null) return "Logistics intent: no active panel.";
        DeliveryIntentRecord rec = g.logisticsDeliveryIntentHistory.peekFirst();
        return rec == null ? "Logistics intent: no delivery intent recorded yet." : "Logistics intent: latest " + rec.compact();
    }

    private static String clean(String s, String fallback) { return s == null || s.isBlank() ? fallback : s.replace('\t',' ').replace('\n',' '); }
    private static String esc(String s) { return clean(s, "").replace("%", "%25").replace("\t", "%09").replace("\n", "%0A").replace(";", "%3B"); }
    private static String unesc(String s) { return s == null ? "" : s.replace("%3B", ";").replace("%0A", "\n").replace("%09", "\t").replace("%25", "%"); }
}
