package mechanist;

import java.util.*;

/**
 * 0.9.07z Abstract distant-zone simulation foundation.
 *
 * This is a conservative ledger layer.  It does not replace authoritative
 * in-zone tile/entity simulation; it records and advances cheap summaries for
 * zones outside the active player bubble so the game can imply a living arcology
 * without requiring every NPC, machine, room, and faction to think every turn.
 */
class AbstractDistantZoneSimulation {
    static final int ABSTRACT_DISTANCE_THRESHOLD = NpcTurnBudgetScheduler.ACTIVE_RADIUS + 8;
    static final int MAX_LEDGER_EVENTS = 8;
    static final int MAX_WORK_UNITS_PER_CALL = 64;

    final LinkedHashMap<String, ZoneLedger> ledgers = new LinkedHashMap<>();
    long abstractTicksProcessed = 0L;
    long abstractWorkUnitsProcessed = 0L;
    long lastAuditTurn = -9999L;
    String lastSummary = "No abstract distant-zone ledger work has run yet.";

    static final class ZoneLedger {
        final String zoneKey;
        String zoneLabel;
        int lastTurn;
        int abstractTicks;
        int factionPressure;
        int productionDrift;
        int patrolDrift;
        int unrest;
        int estimatedPopulation;
        int estimatedRooms;
        final EnumMap<Faction,Integer> factionPresence = new EnumMap<>(Faction.class);
        final ArrayDeque<String> recentEvents = new ArrayDeque<>();

        ZoneLedger(String zoneKey) { this.zoneKey = zoneKey; }

        String compact() {
            return zoneKey + " label=" + zoneLabel + " ticks=" + abstractTicks + " pop=" + estimatedPopulation + " rooms=" + estimatedRooms +
                " pressure=" + factionPressure + " production=" + productionDrift + " patrol=" + patrolDrift + " unrest=" + unrest + " events=" + recentEvents.size();
        }
    }

    static final class TickResult {
        int requestedBudget;
        int spentWork;
        int deferredPaid;
        int npcsAbstracted;
        int roomsSummarized;
        int ledgersTouched;
        String summary = "abstract simulation not run";
    }

    TickResult spend(World world, int playerX, int playerY, int turn, int requestedBudget) {
        TickResult out = new TickResult();
        out.requestedBudget = Math.max(0, requestedBudget);
        if (world == null || out.requestedBudget <= 0) { out.summary = lastSummary; return out; }
        int budget = Math.min(MAX_WORK_UNITS_PER_CALL, out.requestedBudget);
        ZoneLedger ledger = ledgerFor(world);
        ledger.lastTurn = turn;
        ledger.estimatedRooms = world.rooms == null ? 0 : world.rooms.size();
        ledger.estimatedPopulation = world.npcs == null ? 0 : world.npcs.size();
        ledger.factionPresence.clear();
        if (world.roomFactions != null) {
            for (Faction f : world.roomFactions) if (f != null) ledger.factionPresence.put(f, ledger.factionPresence.getOrDefault(f, 0) + 1);
        }
        out.roomsSummarized = ledger.estimatedRooms;

        if (world.npcs != null) {
            for (NpcEntity npc : world.npcs) {
                if (npc == null) continue;
                int dist = Math.abs(npc.x - playerX) + Math.abs(npc.y - playerY);
                if (dist < ABSTRACT_DISTANCE_THRESHOLD) continue;
                out.npcsAbstracted++;
                if (budget-- <= 0) break;
                out.spentWork++;
                abstractNpcDrift(ledger, npc, turn);
            }
        }

        int roomBudget = Math.min(budget, 8);
        for (int i = 0; i < roomBudget && i < ledger.estimatedRooms; i++) {
            out.spentWork++;
            ledger.productionDrift += 1 + Math.floorMod(Objects.hash(ledger.zoneKey, turn, i), 3);
            ledger.patrolDrift += Math.floorMod(Objects.hash(turn, ledger.zoneKey, i, "patrol"), 2);
        }

        ledger.abstractTicks++;
        abstractTicksProcessed++;
        abstractWorkUnitsProcessed += out.spentWork;
        out.deferredPaid = out.spentWork;
        out.ledgersTouched = 1;
        out.summary = ledger.compact();
        lastSummary = out.summary;
        return out;
    }

    ZoneLedger ledgerFor(World world) {
        String key = zoneKey(world);
        ZoneLedger ledger = ledgers.get(key);
        if (ledger == null) {
            ledger = new ZoneLedger(key);
            ledgers.put(key, ledger);
        }
        ledger.zoneLabel = world == null ? "unknown" : world.zoneType.label + " " + world.layerText();
        return ledger;
    }

    static String zoneKey(World world) {
        if (world == null) return "world:none";
        return "sector=" + world.sectorX + "," + world.sectorY + ";zone=" + world.zoneX + "," + world.zoneY + ";floor=" + world.floor + ";sewer=" + world.sewerLayer;
    }

    void abstractNpcDrift(ZoneLedger ledger, NpcEntity npc, int turn) {
        if (ledger == null || npc == null) return;
        Faction f = npc.faction == null ? Faction.NONE : npc.faction;
        ledger.factionPresence.put(f, ledger.factionPresence.getOrDefault(f, 0) + 1);
        if (NpcTurnBudgetScheduler.looksDangerous(npc)) ledger.factionPressure++;
        else ledger.patrolDrift++;
        if (turn % 17 == 0 && ledger.recentEvents.size() < MAX_LEDGER_EVENTS) addEvent(ledger, "distant " + f.label + " movement abstracted near " + npc.x + "," + npc.y);
        if (turn % 23 == 0) ledger.unrest += NpcTurnBudgetScheduler.looksDangerous(npc) ? 2 : 1;
    }

    void addEvent(ZoneLedger ledger, String event) {
        if (ledger == null || event == null || event.isBlank()) return;
        while (ledger.recentEvents.size() >= MAX_LEDGER_EVENTS) ledger.recentEvents.removeFirst();
        ledger.recentEvents.addLast(event);
    }

    boolean shouldAudit(int turn) {
        if (turn - lastAuditTurn >= 25) { lastAuditTurn = turn; return true; }
        return false;
    }

    String globalSummary() {
        return "ledgers=" + ledgers.size() + " abstractTicks=" + abstractTicksProcessed + " workUnits=" + abstractWorkUnitsProcessed + " last=" + lastSummary;
    }
}
