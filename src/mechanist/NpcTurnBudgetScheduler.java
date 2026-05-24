package mechanist;

import java.util.*;

/**
 * 0.9.07y NPC turn-budget scheduler.
 *
 * This class is deliberately small and package-local.  It does not move actors
 * by itself; it only chooses which NPCs are allowed to spend full local thinking
 * work during the current player/world turn.  GamePanel remains the authority
 * that applies combat, movement, sound, and dirty-region side effects.
 */
class NpcTurnBudgetScheduler {
    static final int ACTIVE_RADIUS = 28;
    static final int CRITICAL_RADIUS = 10;
    static final int HARD_LOCAL_BUDGET = 48;
    static final int QUIET_LOCAL_BUDGET = 24;
    static final int EMERGENCY_EXTRA_BUDGET = 12;
    static final int FAR_DEFERRED_DEBT_DIVISOR = 16;

    private int rotatingCursor = 0;
    private long lastAuditTurn = -9999L;

    static final class Selection {
        final ArrayList<NpcEntity> selected = new ArrayList<>();
        int hostileEmergency;
        int nearbyPriority;
        int rotatingLocal;
        int farDeferred;
        int budgetLimited;
        int nullEntries;
        int total;
        int cursorBefore;
        int cursorAfter;
        int debtSuggested;

        String auditLine(int turn, int playerX, int playerY, int budget) {
            return "turn=" + turn + " selected=" + selected.size() + " budget=" + budget +
                " hostileEmergency=" + hostileEmergency + " nearbyPriority=" + nearbyPriority +
                " rotatingLocal=" + rotatingLocal + " farDeferred=" + farDeferred +
                " budgetLimited=" + budgetLimited + " total=" + total +
                " cursor=" + cursorBefore + "->" + cursorAfter +
                " debtSuggested=" + debtSuggested + " player=" + playerX + "," + playerY;
        }
    }

    Selection select(List<NpcEntity> npcs, int playerX, int playerY, int turn, boolean playerSneaking, boolean dangerClose) {
        Selection out = new Selection();
        if (npcs == null || npcs.isEmpty()) return out;
        out.total = npcs.size();
        int n = npcs.size();
        if (rotatingCursor < 0 || rotatingCursor >= n) rotatingCursor = Math.floorMod(rotatingCursor, n);
        out.cursorBefore = rotatingCursor;

        int budget = dangerClose ? HARD_LOCAL_BUDGET + EMERGENCY_EXTRA_BUDGET : (playerSneaking ? QUIET_LOCAL_BUDGET : HARD_LOCAL_BUDGET);
        HashSet<NpcEntity> already = new HashSet<>();

        // Emergency lane: very close hostile/danger actors should not be delayed by the fairness cursor.
        for (NpcEntity npc : npcs) {
            if (npc == null) { out.nullEntries++; continue; }
            int dist = manhattan(npc.x, npc.y, playerX, playerY);
            if (dist <= CRITICAL_RADIUS && looksDangerous(npc) && out.selected.size() < budget) {
                out.selected.add(npc);
                already.add(npc);
                out.hostileEmergency++;
            }
        }

        // Priority local lane: nearby actors receive service before distant room-noise actors.
        for (NpcEntity npc : npcs) {
            if (npc == null || already.contains(npc)) continue;
            int dist = manhattan(npc.x, npc.y, playerX, playerY);
            if (dist <= CRITICAL_RADIUS && out.selected.size() < budget) {
                out.selected.add(npc);
                already.add(npc);
                out.nearbyPriority++;
            }
        }

        // Rotating local lane: fair bounded work through the active-zone bubble.
        int inspected = 0;
        int acceptedThisLane = 0;
        while (inspected < n && out.selected.size() < budget) {
            int idx = (rotatingCursor + inspected) % n;
            NpcEntity npc = npcs.get(idx);
            inspected++;
            if (npc == null || already.contains(npc)) continue;
            int dist = manhattan(npc.x, npc.y, playerX, playerY);
            if (dist > ACTIVE_RADIUS) { out.farDeferred++; continue; }
            out.selected.add(npc);
            already.add(npc);
            out.rotatingLocal++;
            acceptedThisLane++;
        }
        rotatingCursor = (rotatingCursor + Math.max(1, inspected)) % n;
        out.cursorAfter = rotatingCursor;

        // Count the rest as skipped/deferred for profiling.  This is intentionally cheap.
        for (NpcEntity npc : npcs) {
            if (npc == null || already.contains(npc)) continue;
            int dist = manhattan(npc.x, npc.y, playerX, playerY);
            if (dist > ACTIVE_RADIUS) out.farDeferred++; else out.budgetLimited++;
        }
        out.debtSuggested = Math.max(0, out.farDeferred / FAR_DEFERRED_DEBT_DIVISOR) + Math.max(0, out.budgetLimited / 8);
        return out;
    }

    boolean shouldAudit(int turn) {
        if (turn - lastAuditTurn >= 10) { lastAuditTurn = turn; return true; }
        return false;
    }

    static int manhattan(int ax, int ay, int bx, int by) {
        return Math.abs(ax - bx) + Math.abs(ay - by);
    }

    static boolean looksDangerous(NpcEntity npc) {
        if (npc == null || npc.isUntargetableAnchor()) return false;
        if ("Hostile".equalsIgnoreCase(npc.state)) return true;
        Faction f = npc.faction;
        return f == Faction.MUTANT || f == Faction.CULTIST || f == Faction.HERETIC || f == Faction.ROGUE_MACHINE || f == Faction.BANDIT || (f != null && f.name().startsWith("GANGER"));
    }
}
