package mechanist;

import java.util.*;

/**
 * 0.9.10f — Logistics Storage Source Candidate Display.
 *
 * Reads the latest bounded logistics delivery intent and displays where its
 * forecasted inputs appear to exist. This is informational only: it does not
 * reserve storage, move actors, transfer items, lock routes, or consume inputs.
 */
final class LogisticsStorageCandidateAuthority {
    static final String VERSION = "0.9.10f";
    static final int MAX_ITEM_LINES = 8;
    static final int MAX_SOURCE_CONTAINERS = 2;

    private LogisticsStorageCandidateAuthority() {}

    static final class CandidateLine {
        final String item;
        final int need;
        final int baseStorage;
        final int carried;

        CandidateLine(String item, int need, int baseStorage, int carried) {
            this.item = item == null || item.isBlank() ? "unknown" : item;
            this.need = Math.max(0, need);
            this.baseStorage = Math.max(0, baseStorage);
            this.carried = Math.max(0, carried);
        }

        int total() { return baseStorage + carried; }
        int missing() { return Math.max(0, need - total()); }
        boolean ready() { return missing() == 0; }

        String display() {
            String state = ready() ? "ready" : "missing " + missing();
            return item + " need=" + need + " base=" + baseStorage + " carried=" + carried + " total=" + total() + " " + state;
        }
    }

    static ArrayList<CandidateLine> latestCandidateLines(GamePanel g) {
        ArrayList<CandidateLine> out = new ArrayList<>();
        if (g == null || g.logisticsDeliveryIntentHistory.isEmpty()) return out;
        LogisticsDeliveryIntentAuthority.DeliveryIntentRecord rec = g.logisticsDeliveryIntentHistory.peekFirst();
        if (rec == null || rec.tokenLines == null) return out;
        int shown = 0;
        for (String raw : rec.tokenLines) {
            if (shown >= MAX_ITEM_LINES) break;
            ParsedToken t = parseToken(raw);
            if (t == null || t.item.isBlank()) continue;
            out.add(new CandidateLine(t.item, t.need, countInContainer(g, GamePanel.CONTAINER_BASE_STORAGE, t.item), countInContainer(g, GamePanel.CONTAINER_PLAYER_INVENTORY, t.item)));
            shown++;
        }
        return out;
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics Storage Source Candidate Authority " + VERSION + " — lists candidate source containers for selected station needs.");
        if (g == null) { out.add("  No active game panel."); return out; }
        LogisticsDeliveryIntentAuthority.DeliveryIntentRecord rec = g.logisticsDeliveryIntentHistory.peekFirst();
        if (rec == null) {
            out.add("  No delivery intent exists yet. Use INTENT on a selected machine before source candidates can be displayed.");
            out.add("  Efficiency: idle path does no storage scan.");
            return out;
        }
        out.add("  Latest intent: #" + rec.id + " " + rec.stationName + " job=" + rec.jobLabel + " status=" + rec.status + ".");
        ArrayList<CandidateLine> lines = latestCandidateLines(g);
        if (lines.isEmpty()) out.add("  No input tokens on latest intent; no source candidates required.");
        int ready = 0;
        for (CandidateLine l : lines) {
            if (l.ready()) ready++;
            out.add("  source " + l.display());
        }
        if (rec.tokenLines != null && rec.tokenLines.size() > lines.size()) out.add("  ... " + (rec.tokenLines.size() - lines.size()) + " token line(s) hidden or non-input by bounded display.");
        out.add("  Candidate readiness: " + ready + "/" + lines.size() + " input kind(s) have visible sources in base storage/carried inventory.");
        out.add("  Efficiency: checks only latest intent tokens against the two production input containers; no global container sweep and no pathfinding.");
        return out;
    }

    static String compactSummary(GamePanel g) {
        if (g == null) return "Logistics sources: no active panel.";
        LogisticsDeliveryIntentAuthority.DeliveryIntentRecord rec = g.logisticsDeliveryIntentHistory.peekFirst();
        if (rec == null) return "Logistics sources: no intent recorded yet.";
        ArrayList<CandidateLine> lines = latestCandidateLines(g);
        int ready = 0;
        int missingUnits = 0;
        for (CandidateLine l : lines) { if (l.ready()) ready++; missingUnits += l.missing(); }
        return "Logistics sources: intent #" + rec.id + " candidates " + ready + "/" + lines.size() + " ready, missing units " + missingUnits + ".";
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics Storage Source Candidate Authority " + VERSION);
        out.add("Purpose: show where a delivery intent's required inputs currently appear before any hauling AI is allowed to act.");
        out.add("Current behavior: reads the newest logistics intent token list, then checks base storage and carried inventory as candidate sources.");
        out.add("Player feedback: each input line reports need, base storage count, carried count, total availability, and missing amount.");
        out.add("Hard boundary: does not reserve an item instance, lock a route, move an actor, transfer goods, consume inputs, or trigger production.");
        out.add("Efficiency: bounded to the latest intent, up to " + MAX_ITEM_LINES + " token lines, and the two existing production input containers.");
        return out;
    }

    private static int countInContainer(GamePanel g, String cid, String item) {
        if (g == null || cid == null || item == null || item.isBlank()) return 0;
        ContainerRecord c = g.itemContainers.get(cid);
        if (c != null) {
            int n = 0;
            for (String id : c.itemInstanceIds) {
                ItemInstance inst = g.itemInstances.get(id);
                if (inst != null && ItemQuality.namesMatch(inst.displayName, item)) n++;
            }
            return n;
        }
        ArrayList<String> legacy = GamePanel.CONTAINER_BASE_STORAGE.equals(cid) ? g.baseStorage : GamePanel.CONTAINER_PLAYER_INVENTORY.equals(cid) ? g.inventory : null;
        int n = 0;
        if (legacy != null) for (String raw : legacy) if (ItemQuality.namesMatch(raw, item)) n++;
        return n;
    }

    private static final class ParsedToken {
        final String item; final int need;
        ParsedToken(String item, int need) { this.item = item == null ? "" : item.trim(); this.need = Math.max(0, need); }
    }

    private static ParsedToken parseToken(String raw) {
        if (raw == null || raw.isBlank() || raw.startsWith("bounded_display_hidden=")) return null;
        String item = raw;
        int colon = raw.indexOf(':');
        if (colon >= 0) item = raw.substring(0, colon);
        int need = intAfter(raw, "need=");
        if (need < 0) need = intAfter(raw, "need ");
        if (need < 0) need = 0;
        return new ParsedToken(item, need);
    }

    private static int intAfter(String raw, String key) {
        int p = raw.indexOf(key);
        if (p < 0) return -1;
        p += key.length();
        while (p < raw.length() && raw.charAt(p) == ' ') p++;
        int e = p;
        while (e < raw.length() && Character.isDigit(raw.charAt(e))) e++;
        if (e == p) return -1;
        try { return Integer.parseInt(raw.substring(p, e)); } catch (Exception ignored) { return -1; }
    }
}
