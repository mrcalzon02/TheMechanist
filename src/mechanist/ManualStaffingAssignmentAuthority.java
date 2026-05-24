package mechanist;

import java.util.*;

/**
 * 0.9.10b/0.9.10c — Manual Staffing Assignment + Role Validation Bridge.
 *
 * Assignment authority only.  It lets the player bind a recruited worker name to
 * a machine or defensive station so later queue/logistics systems can query a
 * single staffing field.  It does not move actors, run production, pathfind,
 * schedule shifts, or own combat outcomes.
 */
final class ManualStaffingAssignmentAuthority {
    static final String VERSION = "0.9.10c";
    static final int MAX_DISPLAY_LINES = 8;

    private ManualStaffingAssignmentAuthority() {}

    static String assign(GamePanel g, BaseObject station, RecruitWorker worker) {
        if (g == null) return "Staffing assignment failed: no active game panel.";
        if (station == null) return "Staffing assignment failed: no staffable station selected.";
        if (worker == null) return "Staffing assignment failed: no recruited worker selected.";
        if (!(StaffingLaborBridgeAuthority.isMachineStation(station) || StaffingLaborBridgeAuthority.isDefenseStation(station))) {
            return "Staffing assignment failed: " + station.name + " is not a staffable machine or defensive station.";
        }
        if (worker.name == null || worker.name.isBlank()) return "Staffing assignment failed: selected worker has no valid name.";
        StaffingRoleSkillValidationAuthority.ValidationResult validation = StaffingRoleSkillValidationAuthority.validate(g, station, worker);
        if (!validation.allowed) return "Staffing assignment failed: " + validation.summary;
        clearWorkerFromOtherStations(g, worker.name, station);
        station.assignedWorker = worker.name;
        String warning = validation.ideal ? "" : " WARNING: duty mismatch accepted for now.";
        return "STAFFING: " + worker.name + " assigned to " + station.name + " as "
                + StaffingLaborBridgeAuthority.primaryRole(station).label + ". " + validation.summary + warning
                + " Production/combat automation remains dormant.";
    }

    static String unassign(GamePanel g, BaseObject station) {
        if (g == null) return "Staffing clear failed: no active game panel.";
        if (station == null) return "Staffing clear failed: no staffable station selected.";
        String prior = station.assignedWorker == null || station.assignedWorker.isBlank() ? "unassigned" : station.assignedWorker;
        station.assignedWorker = "";
        return "STAFFING: cleared " + prior + " from " + station.name + ".";
    }

    static void clearWorkerFromOtherStations(GamePanel g, String workerName, BaseObject keep) {
        if (g == null || workerName == null || workerName.isBlank()) return;
        for (BaseObject obj : g.baseObjects) {
            if (obj == null || obj == keep) continue;
            if (workerName.equals(obj.assignedWorker)) obj.assignedWorker = "";
        }
    }

    static String selectionLine(GamePanel g, BaseObject station, RecruitWorker worker) {
        String stationText = station == null ? "none" : station.name + " at " + station.x + "," + station.y + " role=" + StaffingLaborBridgeAuthority.primaryRole(station).label;
        String workerText = worker == null ? "none" : worker.name + " duty=" + worker.duty + " skill=" + worker.skill + " loyalty=" + worker.loyalty;
        String validation = (g == null || station == null || worker == null) ? "validation=pending" : StaffingRoleSkillValidationAuthority.compactLine(g, station, worker);
        return "Manual staffing selection: station=" + stationText + " | worker=" + workerText + " | " + validation + ".";
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Manual Staffing Assignment Bridge " + VERSION + " — records manual worker assignment to selected machines.");
        if (g == null) { out.add("  No active game panel."); return out; }
        BaseObject station = g.selectedStaffingStation();
        RecruitWorker worker = g.selectedStaffingWorker();
        out.add("  " + selectionLine(g, station, worker));
        int assigned = 0, shown = 0;
        for (BaseObject obj : g.staffableStations()) {
            String aw = obj.assignedWorker == null || obj.assignedWorker.isBlank() ? "unassigned" : obj.assignedWorker;
            if (!"unassigned".equals(aw)) assigned++;
            if (shown < MAX_DISPLAY_LINES) {
                out.add("  station " + obj.name + " -> " + aw + " | " + StaffingLaborBridgeAuthority.primaryRole(obj).label);
                shown++;
            }
        }
        int total = g.staffableStations().size();
        out.add("  assigned stations " + assigned + "/" + total + "; assignment data persists through BaseObject assignedWorker saves.");
        if (total > shown) out.add("  ... " + (total - shown) + " additional staffable station(s) hidden by bounded display.");
        return out;
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Manual Staffing Assignment Authority " + VERSION);
        out.add("Purpose: manually bind recruited workers to selected machines or defensive stations.");
        out.add("Boundary: assignments update station metadata and inspection/readiness displays only; they do not move NPCs, run production, fire turrets, or lock logistics routes.");
        out.add("Efficiency: no global scan, no pathing, no turn-loop scheduler; the bridge runs only when the player changes or inspects assignments.");
        out.add("Persistence: station assignedWorker fields are saved with base objects, so no bulky save structure is required.");
        out.add("Validation: assignments are role/skill checked before station metadata changes.");
        return out;
    }
}
