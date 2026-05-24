package mechanist;

import java.util.*;

/**
 * 0.9.10a — Staffing / Labor Bridge.
 *
 * Observational staffing authority only.  It centralizes staffing summaries for
 * machines and defensive stations without assigning jobs, moving actors, or
 * owning production outcomes.  All methods are bounded and event/UI driven so
 * the bridge avoids turn-loop/global-scan overhead.
 */
final class StaffingLaborBridgeAuthority {
    static final String VERSION = "0.9.10a";
    static final int MAX_DISPLAY_LINES = 10;

    private StaffingLaborBridgeAuthority() {}

    enum LaborRole {
        OPERATOR("operator"),
        LOADER("loader"),
        GUARD("guard"),
        TECHNICIAN("technician"),
        MAINTENANCE("maintenance");

        final String label;
        LaborRole(String label) { this.label = label; }
    }

    static boolean isMachineStation(BaseObject obj) {
        if (obj == null) return false;
        switch (obj.symbol) {
            case 'w': case 'e': case 'f': case 'l': case 'x': case 'L':
                return true;
            default:
                return false;
        }
    }

    static boolean isDefenseStation(BaseObject obj) {
        return obj != null && PassiveDefenseEffectsAuthority.isPassiveDefense(obj.symbol);
    }

    static LaborRole primaryRole(BaseObject obj) {
        if (obj == null) return LaborRole.MAINTENANCE;
        if (isMachineStation(obj)) return LaborRole.OPERATOR;
        PassiveDefenseProfile p = PassiveDefenseEffectsAuthority.profile(obj.symbol);
        if (p != null) {
            if (p.family.contains("turret")) return LaborRole.LOADER;
            if (p.family.contains("sensor") || p.family.contains("coordination")) return LaborRole.TECHNICIAN;
            return LaborRole.GUARD;
        }
        return LaborRole.MAINTENANCE;
    }

    static String readinessLine(GamePanel g, BaseObject obj) {
        if (obj == null) return "No station selected.";
        LaborRole role = primaryRole(obj);
        String worker = obj.assignedWorker == null || obj.assignedWorker.isBlank() ? "unassigned" : obj.assignedWorker;
        String assignment = worker.equals("unassigned") || worker.equals("unmanned") ? "understaffed" : "staffed";
        if (isMachineStation(obj)) {
            String recipe = obj.assignedRecipe == null || obj.assignedRecipe.isBlank() ? "none" : obj.assignedRecipe;
            String readiness = "idle";
            if (!"none".equals(recipe)) {
                if (ControlledProductionJobAuthority.isGeneratedAssignment(recipe)) {
                    FactionRecipeVariant v = ControlledProductionJobAuthority.findVariantByAssignmentKey(recipe);
                    String problem = v == null ? "invalid generated assignment" : ControlledProductionJobAuthority.operationProblem(g, obj, v, false);
                    readiness = problem == null ? "ready" : "blocked: " + problem;
                } else {
                    CraftingRecipe r = CraftingRecipe.byName(recipe);
                    String problem = r == null ? "invalid recipe" : r.blockingProblemForMachine(g, obj);
                    readiness = problem == null ? "ready" : "blocked: " + problem;
                }
            }
            return obj.name + " staffing=" + assignment + " role=" + role.label + " worker=" + worker
                    + " recipe=" + recipe + " machine=" + readiness
                    + " queue=" + Math.max(0, obj.productionQueueRemaining) + "/" + Math.max(1, obj.productionQueueTarget);
        }
        if (isDefenseStation(obj)) {
            PassiveDefenseProfile p = PassiveDefenseEffectsAuthority.profile(obj.symbol);
            String family = p == null ? "defense" : p.family;
            return obj.name + " staffing=" + assignment + " role=" + role.label + " worker=" + worker
                    + " defense=" + family + " passive=" + PassiveDefenseEffectsAuthority.raidResistance(obj)
                    + " cover=" + PassiveDefenseEffectsAuthority.coverPenalty(obj)
                    + " live-fire=dormant";
        }
        return obj.name + " staffing=" + assignment + " role=" + role.label + " worker=" + worker;
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Staffing / Labor Bridge " + VERSION + " — summarizes staff readiness against assigned machines.");
        if (g == null) { lines.add("No active game panel."); return lines; }
        lines.add("Recruits " + g.factionRecruits.size() + "/" + g.recruitCapacity()
                + " | available labor " + g.availableRecruitLabor()
                + " | security staff " + g.securityStaffCount()
                + " | assigned stations " + assignedStationCount(g) + ".");
        int shown = 0;
        for (BaseObject obj : g.baseObjects) {
            if (!(isMachineStation(obj) || isDefenseStation(obj))) continue;
            lines.add("  " + readinessLine(g, obj));
            shown++;
            if (shown >= MAX_DISPLAY_LINES) break;
        }
        int total = stationCount(g);
        if (total == 0) lines.add("  No machine or defense stations currently require staffing summaries.");
        else if (total > shown) lines.add("  ... " + (total - shown) + " additional station(s) hidden by bounded display.");
        return lines;
    }

    static String compactSummary(GamePanel g) {
        if (g == null) return "staffingLaborBridge " + VERSION + " unavailable";
        return "staffingLaborBridge " + VERSION
                + " recruits=" + g.factionRecruits.size() + "/" + g.recruitCapacity()
                + " stations=" + stationCount(g)
                + " assigned=" + assignedStationCount(g)
                + " idle=" + Math.max(0, stationCount(g) - assignedStationCount(g))
                + " mode=observational/no-global-scan";
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Staffing / Labor Bridge " + VERSION);
        lines.add("Purpose: centralize machine and defense staffing summaries before autonomous labor exists.");
        lines.add("Current rule: staffing reads machine readiness, queue history, and passive defense status, but does not own production outcomes.");
        lines.add("Efficiency rule: no global labor simulation, no render-loop staffing scan, no autonomous assignment loop.");
        lines.add("Labor roles: operator, loader, guard, technician, maintenance.");
        return lines;
    }

    static int stationCount(GamePanel g) {
        if (g == null) return 0;
        int n = 0;
        for (BaseObject obj : g.baseObjects) if (isMachineStation(obj) || isDefenseStation(obj)) n++;
        return n;
    }

    static int assignedStationCount(GamePanel g) {
        if (g == null) return 0;
        int n = 0;
        for (BaseObject obj : g.baseObjects) {
            if (!(isMachineStation(obj) || isDefenseStation(obj))) continue;
            if (obj.assignedWorker != null && !obj.assignedWorker.isBlank() && !"unmanned".equalsIgnoreCase(obj.assignedWorker)) n++;
        }
        return n;
    }
}
