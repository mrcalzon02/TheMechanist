package mechanist;

import java.util.*;

/**
 * Executes one player-selected manual haul contract through the existing logistics
 * chain.  Scope is intentionally narrow: the latest contract only, current-zone
 * handling checks only, item registry transfers only, and no autonomous hauling loop.
 */
final class LogisticsManualHaulExecutionAuthority {
    static final String VERSION = "0.9.10q";
    static final int MAX_RECORDS = 24;
    static final int MAX_LINES = 12;
    static final int MAX_UNITS_PER_EXECUTION = 12;

    private LogisticsManualHaulExecutionAuthority() {}

    static final class ManualHaulExecutionRecord {
        final int id;
        final int turn;
        final int contractId;
        final int preflightId;
        final String stationName;
        final String workerLabel;
        final String status;
        final String targetContainerId;
        final int pickedUp;
        final int delivered;
        final int turnsSpent;
        final int fatigueCost;
        final ArrayList<String> lines;

        ManualHaulExecutionRecord(int id, int turn, int contractId, int preflightId, String stationName,
                                  String workerLabel, String status, String targetContainerId, int pickedUp,
                                  int delivered, int turnsSpent, int fatigueCost, ArrayList<String> lines) {
            this.id = Math.max(0, id);
            this.turn = Math.max(0, turn);
            this.contractId = Math.max(0, contractId);
            this.preflightId = Math.max(0, preflightId);
            this.stationName = clean(stationName, "unknown station");
            this.workerLabel = clean(workerLabel, "player/manual");
            this.status = clean(status, "execution-record");
            this.targetContainerId = clean(targetContainerId, "unknown.container");
            this.pickedUp = Math.max(0, pickedUp);
            this.delivered = Math.max(0, delivered);
            this.turnsSpent = Math.max(0, turnsSpent);
            this.fatigueCost = Math.max(0, fatigueCost);
            this.lines = lines == null ? new ArrayList<>() : new ArrayList<>(lines);
        }

        String compact() {
            return "Manual haul execution #" + id + " turn " + turn + " contract #" + contractId
                    + " preflight #" + preflightId + " " + stationName + " worker=" + workerLabel
                    + " status=" + status + " picked=" + pickedUp + " delivered=" + delivered
                    + " turns=" + turnsSpent + " fatigue+" + fatigueCost + " target=" + targetContainerId;
        }

        String encode() {
            return id + "\t" + turn + "\t" + contractId + "\t" + preflightId + "\t" + esc(stationName) + "\t"
                    + esc(workerLabel) + "\t" + esc(status) + "\t" + esc(targetContainerId) + "\t" + pickedUp + "\t"
                    + delivered + "\t" + turnsSpent + "\t" + fatigueCost + "\t" + esc(String.join(";;", lines));
        }

        static ManualHaulExecutionRecord decode(String raw) {
            if (raw == null || raw.isBlank()) return null;
            String[] a = raw.split("\\t", -1);
            if (a.length < 13) return null;
            try {
                ArrayList<String> l = new ArrayList<>();
                String joined = unesc(a[12]);
                if (!joined.isBlank()) for (String s : joined.split(";;", -1)) if (!s.isBlank()) l.add(s);
                return new ManualHaulExecutionRecord(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]),
                        Integer.parseInt(a[3]), unesc(a[4]), unesc(a[5]), unesc(a[6]), unesc(a[7]), Integer.parseInt(a[8]),
                        Integer.parseInt(a[9]), Integer.parseInt(a[10]), Integer.parseInt(a[11]), l);
            } catch (Exception ignored) { return null; }
        }
    }

    static String executeLatestContract(GamePanel g) {
        if (g == null) return "MANUAL HAUL EXECUTION FAILED: no active game panel.";
        LogisticsManualHaulContractAuthority.ManualHaulContractRecord contract = g.logisticsHaulContractHistory.peekFirst();
        if (contract == null) return "MANUAL HAUL EXECUTION FAILED: record a HAUL CONTRACT first.";
        LogisticsHaulFulfillmentPreflightAuthority.FulfillmentPreflightRecord preflight = g.logisticsHaulPreflightHistory.peekFirst();
        LogisticsDeliveryIntentAuthority.DeliveryIntentRecord intent = g.logisticsDeliveryIntentHistory.peekFirst();
        LogisticsSourceReservationAuthority.SourceReservationRecord source = g.logisticsSourceReservationHistory.peekFirst();
        LogisticsRouteIntentAuthority.RouteIntentRecord route = g.logisticsRouteIntentHistory.peekFirst();

        ArrayList<String> lines = new ArrayList<>();
        ArrayList<String> blockers = new ArrayList<>();
        if (preflight == null) blockers.add("Run PREFLIGHT before executing a manual haul contract.");
        else if (preflight.contractId != contract.id) blockers.add("Latest preflight #" + preflight.id + " belongs to contract #" + preflight.contractId + ", not contract #" + contract.id + ".");
        else if (preflight.status != null && preflight.status.startsWith("blocked")) blockers.add("Latest preflight is blocked: " + preflight.status + ".");
        if (intent == null || intent.id != contract.intentId) blockers.add("Delivery intent link is not current for contract #" + contract.id + ".");
        if (source == null || source.id != contract.sourceTokenId) blockers.add("Source token link is not current for contract #" + contract.id + ".");
        if (route == null || route.id != contract.routeIntentId) blockers.add("Route intent link is not current for contract #" + contract.id + ".");
        if (contract.contractStatus != null && contract.contractStatus.startsWith("blocked")) blockers.add("Contract status is blocked: " + contract.contractStatus + ".");
        if (contract.warnings != null) for (String w : contract.warnings) if (w != null && w.startsWith("BLOCK:")) blockers.add(w.substring(6).trim());
        LogisticsContractLifecycleAuthority.ContractLifecycleRecord lifecycle = g.logisticsContractLifecycleHistory.peekFirst();
        if (lifecycle != null && lifecycle.contractId == contract.id && lifecycle.cancelled) blockers.add("Latest lifecycle record cancelled this contract.");
        if (lifecycle != null && lifecycle.contractId == contract.id && lifecycle.expired) blockers.add("Latest lifecycle record expired this contract.");
        if (g.world == null) blockers.add("No loaded world exists for manual haul execution.");
        else {
            if (!g.world.inBounds(contract.fromX, contract.fromY)) blockers.add("Source anchor is out of bounds.");
            if (!g.world.inBounds(contract.toX, contract.toY)) blockers.add("Destination station is out of bounds.");
            if (g.world.inBounds(contract.fromX, contract.fromY) && !g.world.walkableAdjacentOrSame(contract.fromX, contract.fromY)) blockers.add("Source anchor has no walkable handling tile.");
            if (g.world.inBounds(contract.toX, contract.toY) && !g.world.walkableAdjacentOrSame(contract.toX, contract.toY)) blockers.add("Destination station has no walkable handling tile.");
        }

        ArrayList<SourceNeed> needs = parseNeeds(source);
        int requestedUnits = 0;
        for (SourceNeed n : needs) requestedUnits += n.need;
        if (requestedUnits > MAX_UNITS_PER_EXECUTION) blockers.add("Manual haul contract asks for " + requestedUnits + " units; bounded execution limit is " + MAX_UNITS_PER_EXECUTION + ".");
        for (SourceNeed n : needs) {
            if (n.missing > 0) blockers.add(n.item + " is still short by " + n.missing + " unit(s) in the source token.");
            int visible = countIn(g, GamePanel.CONTAINER_BASE_STORAGE, n.item) + countIn(g, GamePanel.CONTAINER_PLAYER_INVENTORY, n.item);
            if (visible < n.need) blockers.add(n.item + " now has only " + visible + "/" + n.need + " visible unit(s) in base/player input containers.");
        }

        if (!blockers.isEmpty()) {
            lines.addAll(prefix("BLOCK: ", blockers));
            ManualHaulExecutionRecord rec = newRecord(g, contract, preflight, "blocked-execution", targetContainerId(g, contract), 0, 0, 0, 0, lines);
            remember(g, rec);
            return "MANUAL HAUL EXECUTION BLOCKED: " + rec.compact() + ". " + blockers.size() + " blocker(s) recorded; no items moved.";
        }

        String targetId = targetContainerId(g, contract);
        String targetLabel = contract.stationName + " input buffer";
        g.ensureContainer(targetId, targetLabel);
        int picked = 0;
        int delivered = 0;
        for (SourceNeed n : needs) {
            for (int i = 0; i < n.need; i++) {
                String from = chooseSourceContainer(g, n);
                if (from == null) {
                    lines.add("WARN: " + n.item + " disappeared before delivery; execution stopped after " + delivered + " delivered unit(s).");
                    break;
                }
                ArrayList<String> fromLegacy = legacyList(g, from);
                if (fromLegacy == null) {
                    lines.add("WARN: unsupported source container " + from + " for " + n.item + "; execution stopped.");
                    break;
                }
                if (GamePanel.CONTAINER_BASE_STORAGE.equals(from)) {
                    ItemInstance carried = g.transferContainerItemByName(GamePanel.CONTAINER_BASE_STORAGE, g.baseStorage, n.item,
                            GamePanel.CONTAINER_PLAYER_INVENTORY, "Player carried inventory", g.inventory,
                            "manual haul pickup for contract #" + contract.id + " to " + contract.stationName);
                    if (carried == null) { lines.add("WARN: pickup failed for " + n.item + " from base storage."); break; }
                    picked++;
                    ItemInstance landed = g.transferContainerItemByName(GamePanel.CONTAINER_PLAYER_INVENTORY, g.inventory, n.item,
                            targetId, targetLabel, null,
                            "manual haul delivery for contract #" + contract.id + " to " + contract.stationName);
                    if (landed == null) { lines.add("WARN: delivery failed after pickup for " + n.item + ". Item remains carried."); break; }
                    delivered++;
                    lines.add("OK: delivered " + landed.displayName + " from base storage via carried inventory to " + targetLabel + ".");
                } else {
                    ItemInstance landed = g.transferContainerItemByName(GamePanel.CONTAINER_PLAYER_INVENTORY, g.inventory, n.item,
                            targetId, targetLabel, null,
                            "manual haul delivery for contract #" + contract.id + " to " + contract.stationName);
                    if (landed == null) { lines.add("WARN: carried delivery failed for " + n.item + "."); break; }
                    picked++;
                    delivered++;
                    lines.add("OK: delivered carried " + landed.displayName + " to " + targetLabel + ".");
                }
            }
        }

        int turns = delivered == 0 ? 0 : Math.max(1, Math.min(12, 1 + Math.max(0, contract.estimatedSteps) / 12 + delivered / 3));
        int fatigue = delivered == 0 ? 0 : Math.max(1, Math.min(4, 1 + delivered / 4));
        if (delivered > 0) {
            g.fatigue = Math.min(20, g.fatigue + fatigue);
            for (int i = 0; i < turns; i++) g.advanceTurn("executes a selected manual haul contract.");
            g.verifyItemOperationalParity("manual haul execution");
        }
        String status = delivered == requestedUnits ? "executed" : (delivered > 0 ? "partial-execution" : "no-transfer");
        ManualHaulExecutionRecord rec = newRecord(g, contract, preflight, status, targetId, picked, delivered, turns, fatigue, lines);
        remember(g, rec);
        return "MANUAL HAUL EXECUTION RECORDED: " + rec.compact() + ". "
                + (delivered == requestedUnits ? "Selected contract fulfilled into the station input buffer." : "Partial/empty execution; inspect logistics status.");
    }

    private static ManualHaulExecutionRecord newRecord(GamePanel g, LogisticsManualHaulContractAuthority.ManualHaulContractRecord c,
                                                        LogisticsHaulFulfillmentPreflightAuthority.FulfillmentPreflightRecord p,
                                                        String status, String targetId, int picked, int delivered, int turns, int fatigue,
                                                        ArrayList<String> lines) {
        int id = Math.max(1, g.nextLogisticsHaulExecutionSeq++);
        return new ManualHaulExecutionRecord(id, g.turn, c == null ? 0 : c.id, p == null ? 0 : p.id,
                c == null ? "unknown station" : c.stationName, c == null ? "player/manual" : c.workerLabel,
                status, targetId, picked, delivered, turns, fatigue, cap(lines));
    }

    private static String targetContainerId(GamePanel g, LogisticsManualHaulContractAuthority.ManualHaulContractRecord c) {
        String name = c == null ? "unknown" : c.stationName;
        int x = c == null ? -1 : c.toX;
        int y = c == null ? -1 : c.toY;
        return GamePanel.CONTAINER_MACHINE_INPUT_PREFIX + ContainerIdentityApi.safeToken(name) + "." + x + "_" + y;
    }

    private static ArrayList<SourceNeed> parseNeeds(LogisticsSourceReservationAuthority.SourceReservationRecord source) {
        ArrayList<SourceNeed> out = new ArrayList<>();
        if (source == null || source.sourceLines == null) return out;
        for (String raw : source.sourceLines) {
            if (raw == null || raw.isBlank() || raw.startsWith("bounded_display_hidden=")) continue;
            int colon = raw.indexOf(':');
            String item = colon >= 0 ? raw.substring(0, colon).trim() : raw.trim();
            if (item.isBlank()) continue;
            int need = intAfter(raw, "need=");
            int base = intAfter(raw, "base=");
            int carried = intAfter(raw, "carried=");
            int missing = intAfter(raw, "missing=");
            String preferred = stringAfter(raw, "preferred=");
            if (need <= 0) continue;
            out.add(new SourceNeed(item, need, Math.max(0, base), Math.max(0, carried), Math.max(0, missing), preferred));
            if (out.size() >= LogisticsSourceReservationAuthority.MAX_ITEM_LINES) break;
        }
        return out;
    }

    private static String chooseSourceContainer(GamePanel g, SourceNeed n) {
        String p = n.preferred == null ? "" : n.preferred;
        if (p.startsWith(GamePanel.CONTAINER_BASE_STORAGE) && countIn(g, GamePanel.CONTAINER_BASE_STORAGE, n.item) > 0) return GamePanel.CONTAINER_BASE_STORAGE;
        if (p.startsWith(GamePanel.CONTAINER_PLAYER_INVENTORY) && countIn(g, GamePanel.CONTAINER_PLAYER_INVENTORY, n.item) > 0) return GamePanel.CONTAINER_PLAYER_INVENTORY;
        if (countIn(g, GamePanel.CONTAINER_BASE_STORAGE, n.item) > 0) return GamePanel.CONTAINER_BASE_STORAGE;
        if (countIn(g, GamePanel.CONTAINER_PLAYER_INVENTORY, n.item) > 0) return GamePanel.CONTAINER_PLAYER_INVENTORY;
        return null;
    }

    private static int countIn(GamePanel g, String cid, String item) {
        if (g == null || cid == null || item == null) return 0;
        ContainerRecord c = g.itemContainers.get(cid);
        if (c != null) {
            int n = 0;
            for (String id : c.itemInstanceIds) {
                ItemInstance inst = g.itemInstances.get(id);
                if (inst != null && ItemQuality.namesMatch(inst.displayName, item)) n++;
            }
            return n;
        }
        ArrayList<String> legacy = legacyList(g, cid);
        int n = 0;
        if (legacy != null) for (String s : legacy) if (ItemQuality.namesMatch(s, item)) n++;
        return n;
    }

    private static ArrayList<String> legacyList(GamePanel g, String cid) {
        if (GamePanel.CONTAINER_BASE_STORAGE.equals(cid)) return g.baseStorage;
        if (GamePanel.CONTAINER_PLAYER_INVENTORY.equals(cid)) return g.inventory;
        return null;
    }

    private static final class SourceNeed {
        final String item; final int need; final int base; final int carried; final int missing; final String preferred;
        SourceNeed(String item, int need, int base, int carried, int missing, String preferred) {
            this.item = clean(item, "unknown item"); this.need = Math.max(0, need); this.base = Math.max(0, base);
            this.carried = Math.max(0, carried); this.missing = Math.max(0, missing); this.preferred = clean(preferred, "no-visible-source");
        }
    }

    static void remember(GamePanel g, ManualHaulExecutionRecord rec) {
        if (g == null || rec == null) return;
        g.logisticsHaulExecutionHistory.addFirst(rec);
        while (g.logisticsHaulExecutionHistory.size() > MAX_RECORDS) g.logisticsHaulExecutionHistory.removeLast();
    }

    static ArrayList<String> encodeHistory(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        if (g == null) return out;
        for (ManualHaulExecutionRecord rec : g.logisticsHaulExecutionHistory) if (rec != null) out.add(rec.encode());
        return out;
    }

    static void restoreHistory(GamePanel g, java.util.List<String> encoded) {
        if (g == null) return;
        g.logisticsHaulExecutionHistory.clear();
        int maxId = 0;
        if (encoded != null) {
            for (String raw : encoded) {
                ManualHaulExecutionRecord rec = ManualHaulExecutionRecord.decode(raw);
                if (rec != null) {
                    g.logisticsHaulExecutionHistory.add(rec);
                    maxId = Math.max(maxId, rec.id);
                    if (g.logisticsHaulExecutionHistory.size() >= MAX_RECORDS) break;
                }
            }
        }
        g.nextLogisticsHaulExecutionSeq = Math.max(g.nextLogisticsHaulExecutionSeq, maxId + 1);
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics Manual Haul Execution Authority " + VERSION + " — executes selected, preflighted manual haul contracts into station input buffers.");
        if (g == null) { out.add("  No active game panel."); return out; }
        out.add("  Stored executions: " + g.logisticsHaulExecutionHistory.size() + "/" + MAX_RECORDS + ". Next id: " + g.nextLogisticsHaulExecutionSeq + ".");
        ManualHaulExecutionRecord latest = g.logisticsHaulExecutionHistory.peekFirst();
        if (latest == null) out.add("  No execution recorded yet. Use HAUL CONTRACT, PREFLIGHT, then EXEC HAUL.");
        else {
            out.add("  Latest: " + latest.compact());
            int shown = 0;
            for (String s : latest.lines) {
                if (shown >= 5) break;
                out.add("  " + s);
                shown++;
            }
            if (latest.lines.size() > shown) out.add("  ... " + (latest.lines.size() - shown) + " execution line(s) hidden by bounded display.");
        }
        out.add("  Efficiency: latest contract only, max " + MAX_UNITS_PER_EXECUTION + " units, base/player sources only, station input-buffer target only.");
        return out;
    }

    static String compactSummary(GamePanel g) {
        if (g == null) return "Logistics manual haul execution: no active panel.";
        ManualHaulExecutionRecord rec = g.logisticsHaulExecutionHistory.peekFirst();
        return rec == null ? "Logistics manual haul execution: none recorded yet." : "Logistics manual haul execution: latest " + rec.compact();
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics Manual Haul Execution Authority " + VERSION);
        out.add("Purpose: execute one selected manual haul contract through item-instance transfers without enabling global hauling AI.");
        out.add("Current behavior: validates latest contract/preflight/source chain, moves required base/player input items through carried inventory, and delivers them into a station input buffer.");
        out.add("Player feedback: records picked/delivered counts, target buffer, time cost, fatigue cost, blockers, and transfer lines.");
        out.add("Hard boundary: no autonomous labor economy, no global pathfinding, no production ownership, and no item teleportation.");
        out.add("Efficiency: latest-contract only, bounded units, existing container/provenance transfer authority, and saveable recent execution history.");
        return out;
    }

    private static ArrayList<String> cap(ArrayList<String> in) {
        if (in == null) return new ArrayList<>();
        if (in.size() <= MAX_LINES) return in;
        ArrayList<String> out = new ArrayList<>(in.subList(0, MAX_LINES));
        out.add("WARN: " + (in.size() - MAX_LINES) + " additional execution line(s) hidden by bounded display.");
        return out;
    }
    private static ArrayList<String> prefix(String p, ArrayList<String> lines) { ArrayList<String> out = new ArrayList<>(); if (lines != null) for (String s : lines) out.add(p + s); return out; }
    private static int intAfter(String raw, String key) {
        int p = raw == null ? -1 : raw.indexOf(key); if (p < 0) return 0; p += key.length();
        while (p < raw.length() && raw.charAt(p) == ' ') p++;
        int e = p; while (e < raw.length() && Character.isDigit(raw.charAt(e))) e++;
        if (e == p) return 0;
        try { return Integer.parseInt(raw.substring(p, e)); } catch (Exception ignored) { return 0; }
    }
    private static String stringAfter(String raw, String key) {
        int p = raw == null ? -1 : raw.indexOf(key); if (p < 0) return ""; p += key.length();
        int e = p; while (e < raw.length() && !Character.isWhitespace(raw.charAt(e))) e++;
        return raw.substring(p, e).trim();
    }
    private static String clean(String s, String fallback) { return s == null || s.isBlank() ? fallback : s.replace('\t',' ').replace('\n',' '); }
    private static String esc(String s) { return clean(s, "").replace("%", "%25").replace("\t", "%09").replace("\n", "%0A").replace(";", "%3B"); }
    private static String unesc(String s) { return s == null ? "" : s.replace("%3B", ";").replace("%0A", "\n").replace("%09", "\t").replace("%25", "%"); }
}
