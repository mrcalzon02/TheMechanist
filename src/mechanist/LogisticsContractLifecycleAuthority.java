package mechanist;

import java.util.*;

/**
 * 0.9.10l — Logistics Contract Lifecycle Authority.
 *
 * Adds bounded expiry, cancellation, and staleness records for manual haul
 * contracts before any execution authority is allowed to move actors, reserve
 * hard item locks, transfer goods, or own production.  This remains a cheap,
 * player-facing state surface: it checks the latest contract chain on demand
 * only and stores a small recent-history buffer for save/load/debugging.
 */
final class LogisticsContractLifecycleAuthority {
    static final String VERSION = "0.9.10l";
    static final int MAX_RECORDS = 24;
    static final int CONTRACT_EXPIRY_TURNS = 240;
    static final int CONTRACT_STALE_TURNS = 80;
    static final int MAX_LINES = 10;

    private LogisticsContractLifecycleAuthority() {}

    static final class ContractLifecycleRecord {
        final int id;
        final int turn;
        final int contractId;
        final String action;
        final String status;
        final String stationName;
        final String workerLabel;
        final int ageTurns;
        final boolean stale;
        final boolean expired;
        final boolean cancelled;
        final ArrayList<String> lines;

        ContractLifecycleRecord(int id, int turn, int contractId, String action, String status,
                                String stationName, String workerLabel, int ageTurns,
                                boolean stale, boolean expired, boolean cancelled, ArrayList<String> lines) {
            this.id = Math.max(0, id);
            this.turn = Math.max(0, turn);
            this.contractId = Math.max(0, contractId);
            this.action = clean(action, "check");
            this.status = clean(status, "unknown");
            this.stationName = clean(stationName, "unknown station");
            this.workerLabel = clean(workerLabel, "unassigned");
            this.ageTurns = Math.max(0, ageTurns);
            this.stale = stale;
            this.expired = expired;
            this.cancelled = cancelled;
            this.lines = lines == null ? new ArrayList<>() : new ArrayList<>(lines);
        }

        String compact() {
            return "Contract lifecycle #" + id + " turn " + turn + " contract #" + contractId
                    + " action=" + action + " status=" + status + " age=" + ageTurns
                    + " stale=" + stale + " expired=" + expired + " cancelled=" + cancelled
                    + " station=" + stationName + " worker=" + workerLabel;
        }

        String encode() {
            return id + "\t" + turn + "\t" + contractId + "\t" + esc(action) + "\t" + esc(status) + "\t"
                    + esc(stationName) + "\t" + esc(workerLabel) + "\t" + ageTurns + "\t"
                    + stale + "\t" + expired + "\t" + cancelled + "\t" + esc(String.join(";;", lines));
        }

        static ContractLifecycleRecord decode(String raw) {
            if (raw == null || raw.isBlank()) return null;
            String[] a = raw.split("\\t", -1);
            if (a.length < 12) return null;
            try {
                ArrayList<String> lines = new ArrayList<>();
                String joined = unesc(a[11]);
                if (!joined.isBlank()) for (String s : joined.split(";;", -1)) if (!s.isBlank()) lines.add(s);
                return new ContractLifecycleRecord(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]),
                        unesc(a[3]), unesc(a[4]), unesc(a[5]), unesc(a[6]), Integer.parseInt(a[7]),
                        Boolean.parseBoolean(a[8]), Boolean.parseBoolean(a[9]), Boolean.parseBoolean(a[10]), lines);
            } catch (Exception ignored) { return null; }
        }
    }

    static String checkLatestContract(GamePanel g) {
        if (g == null) return "CONTRACT LIFECYCLE FAILED: no active game panel.";
        LogisticsManualHaulContractAuthority.ManualHaulContractRecord contract = g.logisticsHaulContractHistory.peekFirst();
        if (contract == null) {
            ContractLifecycleRecord rec = newRecord(g, 0, "check", "no-contract", "unknown station", "unassigned", 0, false, false, false,
                    list("BLOCK: no manual haul contract exists; create HAUL CONTRACT before lifecycle checks."));
            remember(g, rec);
            return "CONTRACT LIFECYCLE RECORDED: " + rec.compact() + ". No execution or transfer performed.";
        }
        return recordLifecycle(g, contract, "check", null);
    }

    static String cancelLatestContract(GamePanel g, String reason) {
        if (g == null) return "CONTRACT CANCEL FAILED: no active game panel.";
        LogisticsManualHaulContractAuthority.ManualHaulContractRecord contract = g.logisticsHaulContractHistory.peekFirst();
        if (contract == null) {
            ContractLifecycleRecord rec = newRecord(g, 0, "cancel", "no-contract", "unknown station", "unassigned", 0, false, false, false,
                    list("BLOCK: no manual haul contract exists to cancel."));
            remember(g, rec);
            return "CONTRACT CANCEL RECORDED: " + rec.compact() + ".";
        }
        return recordLifecycle(g, contract, "cancel", clean(reason, "manual cancellation"));
    }

    private static String recordLifecycle(GamePanel g, LogisticsManualHaulContractAuthority.ManualHaulContractRecord c, String action, String reason) {
        int age = Math.max(0, g.turn - c.turn);
        boolean expired = age > CONTRACT_EXPIRY_TURNS;
        boolean stale = age > CONTRACT_STALE_TURNS || latestChainDiffers(g, c);
        boolean cancelled = "cancel".equals(action);
        ArrayList<String> lines = new ArrayList<>();
        lines.add("OK: contract #" + c.id + " was recorded on turn " + c.turn + " and is " + age + " turn(s) old.");
        if (cancelled) lines.add("CANCELLED: " + clean(reason, "manual cancellation") + ". No locks or transferred items need rollback because execution has not begun.");
        if (expired) lines.add("EXPIRED: contract age exceeds " + CONTRACT_EXPIRY_TURNS + " turns; recreate the logistics chain before execution.");
        else if (stale) lines.add("STALE: contract age/chain differs from the latest logistics records; refresh SOURCE TOKEN, ROUTE INTENT, HAUL PREVIEW, and HAUL CONTRACT before execution.");
        else lines.add("OK: contract is fresh enough for manual execution.");
        appendChainLines(g, c, lines);
        if (c.warnings != null) for (String w : c.warnings) if (w != null && !w.isBlank()) lines.add("INHERITED: " + w);
        lines = cap(lines);
        String status = cancelled ? "cancelled-record" : (expired ? "expired-record" : (stale ? "stale-record" : "active-record"));
        ContractLifecycleRecord rec = newRecord(g, c.id, action, status, c.stationName, c.workerLabel, age, stale, expired, cancelled, lines);
        remember(g, rec);
        return "CONTRACT LIFECYCLE RECORDED: " + rec.compact() + ". "
                + "Lifecycle only: no pathfinding, hauling, actor movement, item lock, item transfer, or production.";
    }

    private static boolean latestChainDiffers(GamePanel g, LogisticsManualHaulContractAuthority.ManualHaulContractRecord c) {
        LogisticsDeliveryIntentAuthority.DeliveryIntentRecord intent = g.logisticsDeliveryIntentHistory.peekFirst();
        LogisticsSourceReservationAuthority.SourceReservationRecord source = g.logisticsSourceReservationHistory.peekFirst();
        LogisticsRouteIntentAuthority.RouteIntentRecord route = g.logisticsRouteIntentHistory.peekFirst();
        LogisticsRouteReadinessPreviewAuthority.ManualHaulPreviewRecord preview = g.logisticsRoutePreviewHistory.peekFirst();
        if (intent != null && intent.id != c.intentId) return true;
        if (source != null && source.id != c.sourceTokenId) return true;
        if (route != null && route.id != c.routeIntentId) return true;
        if (preview != null && preview.id != c.previewId) return true;
        return false;
    }

    private static void appendChainLines(GamePanel g, LogisticsManualHaulContractAuthority.ManualHaulContractRecord c, ArrayList<String> lines) {
        LogisticsDeliveryIntentAuthority.DeliveryIntentRecord intent = g.logisticsDeliveryIntentHistory.peekFirst();
        LogisticsSourceReservationAuthority.SourceReservationRecord source = g.logisticsSourceReservationHistory.peekFirst();
        LogisticsRouteIntentAuthority.RouteIntentRecord route = g.logisticsRouteIntentHistory.peekFirst();
        LogisticsRouteReadinessPreviewAuthority.ManualHaulPreviewRecord preview = g.logisticsRoutePreviewHistory.peekFirst();
        lines.add(chainLine("intent", c.intentId, intent == null ? 0 : intent.id));
        lines.add(chainLine("source token", c.sourceTokenId, source == null ? 0 : source.id));
        lines.add(chainLine("route intent", c.routeIntentId, route == null ? 0 : route.id));
        lines.add(chainLine("haul preview", c.previewId, preview == null ? 0 : preview.id));
    }

    private static String chainLine(String label, int contractId, int latestId) {
        if (latestId <= 0) return "WARN: no latest " + label + " record exists for comparison.";
        if (latestId == contractId) return "OK: latest " + label + " #" + latestId + " matches the contract.";
        return "WARN: latest " + label + " #" + latestId + " differs from contract " + label + " #" + contractId + ".";
    }

    private static ContractLifecycleRecord newRecord(GamePanel g, int contractId, String action, String status, String stationName, String workerLabel,
                                                     int age, boolean stale, boolean expired, boolean cancelled, ArrayList<String> lines) {
        int id = Math.max(1, g.nextLogisticsContractLifecycleSeq++);
        return new ContractLifecycleRecord(id, g.turn, contractId, action, status, stationName, workerLabel, age, stale, expired, cancelled, cap(lines));
    }

    static void remember(GamePanel g, ContractLifecycleRecord rec) {
        if (g == null || rec == null) return;
        g.logisticsContractLifecycleHistory.addFirst(rec);
        while (g.logisticsContractLifecycleHistory.size() > MAX_RECORDS) g.logisticsContractLifecycleHistory.removeLast();
    }

    static ArrayList<String> encodeHistory(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        if (g == null) return out;
        for (ContractLifecycleRecord rec : g.logisticsContractLifecycleHistory) if (rec != null) out.add(rec.encode());
        return out;
    }

    static void restoreHistory(GamePanel g, java.util.List<String> encoded) {
        if (g == null) return;
        g.logisticsContractLifecycleHistory.clear();
        int maxId = 0;
        if (encoded != null) {
            for (String raw : encoded) {
                ContractLifecycleRecord rec = ContractLifecycleRecord.decode(raw);
                if (rec != null) {
                    g.logisticsContractLifecycleHistory.add(rec);
                    maxId = Math.max(maxId, rec.id);
                    if (g.logisticsContractLifecycleHistory.size() >= MAX_RECORDS) break;
                }
            }
        }
        g.nextLogisticsContractLifecycleSeq = Math.max(g.nextLogisticsContractLifecycleSeq, maxId + 1);
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics Contract Lifecycle Authority " + VERSION + " — updates expiry, cancellation, and stale-link status for manual haul contracts.");
        if (g == null) { out.add("  No active game panel."); return out; }
        out.add("  Stored lifecycle records: " + g.logisticsContractLifecycleHistory.size() + "/" + MAX_RECORDS + ". Next id: " + g.nextLogisticsContractLifecycleSeq + ".");
        ContractLifecycleRecord latest = g.logisticsContractLifecycleHistory.peekFirst();
        if (latest == null) out.add("  No lifecycle check yet. Use HAUL CONTRACT, PREFLIGHT, then LIFECYCLE.");
        else {
            out.add("  Latest: " + latest.compact());
            int shown = 0;
            for (String s : latest.lines) {
                if (shown >= 6) break;
                out.add("  " + s);
                shown++;
            }
            if (latest.lines.size() > shown) out.add("  ... " + (latest.lines.size() - shown) + " lifecycle line(s) hidden by bounded display.");
        }
        return out;
    }

    private static ArrayList<String> cap(ArrayList<String> in) {
        if (in == null) return new ArrayList<>();
        if (in.size() <= MAX_LINES) return in;
        ArrayList<String> out = new ArrayList<>(in.subList(0, MAX_LINES));
        out.add("WARN: " + (in.size() - MAX_LINES) + " additional lifecycle line(s) hidden by bounded display.");
        return out;
    }

    private static ArrayList<String> list(String s) { ArrayList<String> a = new ArrayList<>(); a.add(s); return a; }
    private static String clean(String s, String fallback) { return s == null || s.isBlank() ? fallback : s.replace('\t',' ').replace('\n',' ').trim(); }
    private static String esc(String s) { return clean(s, "").replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n"); }
    private static String unesc(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder();
        boolean slash = false;
        for (int i=0;i<s.length();i++) {
            char c = s.charAt(i);
            if (slash) { if (c == 't') out.append('\t'); else if (c == 'n') out.append('\n'); else out.append(c); slash = false; }
            else if (c == '\\') slash = true;
            else out.append(c);
        }
        if (slash) out.append('\\');
        return out.toString();
    }
}
