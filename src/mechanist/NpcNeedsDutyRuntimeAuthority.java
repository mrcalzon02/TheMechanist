package mechanist;

import java.awt.Point;
import java.util.Locale;

/**
 * Owns the live in-zone NPC ecology tick.
 *
 * NpcEntity already contains the small behavior routines. This authority binds
 * those routines to the turn-budget scheduler so local actors receive full
 * updates while distant actors are handed to AbstractDistantZoneSimulation.
 */
final class NpcNeedsDutyRuntimeAuthority {
    private NpcNeedsDutyRuntimeAuthority() {}

    static final class TickResult {
        int total;
        int selected;
        int moved;
        int needs;
        int duties;
        int passiveDesires;
        int hostilePressure;
        int farDeferred;
        int budgetLimited;
        int deferredBudget;
        String schedulerAudit = "scheduler not run";

        String summary() {
            return "npcRuntime selected=" + selected + "/" + total
                    + " moved=" + moved
                    + " needs=" + needs
                    + " duties=" + duties
                    + " passiveDesires=" + passiveDesires
                    + " hostilePressure=" + hostilePressure
                    + " farDeferred=" + farDeferred
                    + " budgetLimited=" + budgetLimited
                    + " deferredBudget=" + deferredBudget
                    + " | " + schedulerAudit;
        }
    }

    static TickResult tick(GamePanel game, boolean sleeping) {
        TickResult out = new TickResult();
        if (game == null || game.world == null || game.world.npcs == null || game.world.npcs.isEmpty()) {
            return out;
        }
        World world = game.world;
        boolean playerSneaking = game.selectedMovementModeIndex == GamePanel.MOTION_SNEAK;
        boolean dangerClose = dangerClose(world, game.playerX, game.playerY);
        NpcTurnBudgetScheduler.Selection selection = game.npcTurnBudgetScheduler.select(
                world.npcs, game.playerX, game.playerY, game.turn, playerSneaking, dangerClose);

        out.total = selection.total;
        out.selected = selection.selected.size();
        out.farDeferred = selection.farDeferred;
        out.budgetLimited = selection.budgetLimited;
        out.deferredBudget = selection.debtSuggested;
        out.hostilePressure = selection.hostileEmergency;
        int localBudget = dangerClose
                ? NpcTurnBudgetScheduler.HARD_LOCAL_BUDGET + NpcTurnBudgetScheduler.EMERGENCY_EXTRA_BUDGET
                : (playerSneaking ? NpcTurnBudgetScheduler.QUIET_LOCAL_BUDGET : NpcTurnBudgetScheduler.HARD_LOCAL_BUDGET);
        out.schedulerAudit = selection.auditLine(game.turn, game.playerX, game.playerY, localBudget);

        for (NpcEntity npc : selection.selected) {
            if (npc == null) continue;
            int beforeX = npc.x;
            int beforeY = npc.y;
            npc.tick(world, game.playerX, game.playerY, game.turn);
            if (isNeedState(npc)) out.needs++;
            if (isDutyState(npc)) out.duties++;
            if (npc.x != beforeX || npc.y != beforeY) {
                out.moved++;
                continue;
            }
            if (!sleeping && applyPassiveDesire(world, npc, game.turn, game.playerX, game.playerY)) {
                out.passiveDesires++;
                if (npc.x != beforeX || npc.y != beforeY) out.moved++;
            }
        }
        return out;
    }

    private static boolean dangerClose(World world, int playerX, int playerY) {
        if (world == null || world.npcs == null) return false;
        for (NpcEntity npc : world.npcs) {
            if (npc == null) continue;
            if (NpcTurnBudgetScheduler.looksDangerous(npc)
                    && NpcTurnBudgetScheduler.manhattan(npc.x, npc.y, playerX, playerY) <= NpcTurnBudgetScheduler.CRITICAL_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNeedState(NpcEntity npc) {
        if (npc == null) return false;
        String state = safe(npc.state);
        String need = safe(npc.needTargetKind);
        return "search needs".equals(state)
                || "seeking safety".equals(state)
                || "seeking entertainment".equals(state)
                || "seeking news".equals(state)
                || (!"none".equals(need) && !need.isBlank());
    }

    private static boolean isDutyState(NpcEntity npc) {
        if (npc == null) return false;
        String state = safe(npc.state);
        return "guard".equals(state)
                || "patrol".equals(state)
                || "inspect".equals(state)
                || "trade".equals(state)
                || "prayer".equals(state)
                || "pilgrim service".equals(state)
                || "contract desk".equals(state)
                || "sanctuary".equals(state)
                || "branch guard".equals(state)
                || "vault desk".equals(state);
    }

    private static boolean applyPassiveDesire(World world, NpcEntity npc, int turn, int blockX, int blockY) {
        if (world == null || npc == null || npc.isAnimalActor() || npc.isUntargetableAnchor()) return false;
        if (NpcTurnBudgetScheduler.looksDangerous(npc) || isDutyState(npc)) return false;
        if (!isUnassignedOrPassiveActor(npc)) return false;
        int cadence = Math.max(6, Math.max(1, npc.idleBias) * 5);
        if (turn <= 0 || turn % cadence != Math.floorMod(npc.numericId, cadence)) return false;

        String desire = passiveDesireFor(world, npc, turn);
        Point target = world.findNeedProvider(npc.x, npc.y, desire);
        if (target == null) return false;
        npc.needTarget = target;
        npc.needTargetKind = desire;
        int distance = NpcTurnBudgetScheduler.manhattan(npc.x, npc.y, target.x, target.y);
        if (distance <= 1) {
            consumePassiveDesire(npc, desire);
            return true;
        }
        npc.stepToward(world, target.x, target.y, blockX, blockY);
        if (npc.x != target.x || npc.y != target.y) npc.state = "Seeking " + title(desire);
        return true;
    }

    private static boolean isUnassignedOrPassiveActor(NpcEntity npc) {
        Faction faction = npc.faction == null ? Faction.NONE : npc.faction;
        if (faction == Faction.NONE || faction == Faction.SCAVENGER || faction == Faction.HIVER || faction == Faction.INN) return true;
        String role = safe(npc.role);
        return role.contains("civilian")
                || role.contains("resident")
                || role.contains("local")
                || role.contains("servant")
                || role.contains("worker")
                || role.contains("reporter")
                || role.contains("editor")
                || role.contains("broadcaster")
                || role.contains("trader");
    }

    private static String passiveDesireFor(World world, NpcEntity npc, int turn) {
        if (npc.sleepDebt > 12) return "sleep";
        if (npc.hunger > 8) return "food";
        if (npc.thirst > 8) return "water";
        if (nearDanger(world, npc)) return "safety";
        String role = safe(npc.role);
        if (npc.faction == Faction.INN || role.contains("reporter") || role.contains("editor") || role.contains("broadcaster")) return "news";
        int roll = Math.floorMod((npc.id == null ? 0 : npc.id.hashCode()) + turn / Math.max(1, npc.idleBias), 5);
        return switch (roll) {
            case 0 -> "news";
            case 1, 2 -> "entertainment";
            case 3 -> "safety";
            default -> "food";
        };
    }

    private static boolean nearDanger(World world, NpcEntity npc) {
        if (world == null || world.npcs == null || npc == null) return false;
        for (NpcEntity other : world.npcs) {
            if (other == null || other == npc) continue;
            if (NpcTurnBudgetScheduler.looksDangerous(other)
                    && NpcTurnBudgetScheduler.manhattan(npc.x, npc.y, other.x, other.y) <= 8) {
                return true;
            }
        }
        return false;
    }

    private static void consumePassiveDesire(NpcEntity npc, String desire) {
        if (npc == null) return;
        switch (safe(desire)) {
            case "food" -> npc.hunger = Math.max(0, npc.hunger - 3);
            case "water" -> npc.thirst = Math.max(0, npc.thirst - 3);
            case "sleep" -> npc.sleepDebt = Math.max(0, npc.sleepDebt - 3);
            case "safety" -> npc.state = "Reassured";
            case "entertainment" -> npc.state = "Passing Time";
            case "news" -> npc.state = "Reading News";
            default -> npc.state = "Idle";
        }
        if (npc.hunger < 9 && npc.thirst < 9 && npc.sleepDebt < 13) {
            npc.needTarget = null;
            npc.needTargetKind = "none";
        }
    }

    private static String title(String value) {
        String s = safe(value);
        if (s.isBlank()) return "Need";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
