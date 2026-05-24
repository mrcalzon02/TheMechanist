package mechanist;

import java.util.*;

/**
 * 0.9.10c — Staffing Role / Skill Validation Bridge.
 *
 * Assignment-time validation only.  This authority lets manual staffing reject
 * obviously bad station/worker matches and explain soft mismatches without
 * starting autonomous labor, pathing, production ownership, or global scans.
 */
final class StaffingRoleSkillValidationAuthority {
    static final String VERSION = "0.9.10c";

    private StaffingRoleSkillValidationAuthority() {}

    static final class ValidationResult {
        final boolean allowed;
        final boolean ideal;
        final String summary;
        final ArrayList<String> notes;

        ValidationResult(boolean allowed, boolean ideal, String summary, ArrayList<String> notes) {
            this.allowed = allowed;
            this.ideal = ideal;
            this.summary = summary == null ? "" : summary;
            this.notes = notes == null ? new ArrayList<>() : notes;
        }
    }

    static int minimumSkill(StaffingLaborBridgeAuthority.LaborRole role) {
        if (role == null) return 1;
        switch (role) {
            case TECHNICIAN: return 3;
            case OPERATOR: return 2;
            case LOADER: return 2;
            case GUARD: return 1;
            case MAINTENANCE:
            default: return 1;
        }
    }

    static String preferredDuty(StaffingLaborBridgeAuthority.LaborRole role) {
        if (role == null) return "labor";
        switch (role) {
            case GUARD: return "security";
            case LOADER: return "security";
            case TECHNICIAN: return "labor";
            case OPERATOR: return "labor";
            case MAINTENANCE:
            default: return "labor";
        }
    }

    static ValidationResult validate(GamePanel g, BaseObject station, RecruitWorker worker) {
        ArrayList<String> notes = new ArrayList<>();
        if (g == null) return new ValidationResult(false, false, "No active game panel.", notes);
        if (station == null) return new ValidationResult(false, false, "No station selected.", notes);
        if (worker == null) return new ValidationResult(false, false, "No worker selected.", notes);
        if (!(StaffingLaborBridgeAuthority.isMachineStation(station) || StaffingLaborBridgeAuthority.isDefenseStation(station))) {
            return new ValidationResult(false, false, station.name + " is not a staffable machine or defensive station.", notes);
        }
        if (worker.name == null || worker.name.isBlank()) {
            return new ValidationResult(false, false, "Selected worker has no valid name.", notes);
        }

        StaffingLaborBridgeAuthority.LaborRole role = StaffingLaborBridgeAuthority.primaryRole(station);
        int min = minimumSkill(role);
        int skill = Math.max(0, worker.skill);
        String duty = worker.duty == null || worker.duty.isBlank() ? "labor" : worker.duty;
        String preferred = preferredDuty(role);
        boolean skillOk = skill >= min;
        boolean dutyOk = preferred.equalsIgnoreCase(duty) || role == StaffingLaborBridgeAuthority.LaborRole.MAINTENANCE;

        if (!skillOk) {
            notes.add("skill " + skill + " below required " + min + " for " + role.label + ".");
        } else {
            notes.add("skill " + skill + " meets required " + min + " for " + role.label + ".");
        }
        if (!dutyOk) {
            notes.add("duty mismatch: " + duty + " assigned to " + role.label + "; preferred duty is " + preferred + ".");
        } else {
            notes.add("duty match: " + duty + " fits " + role.label + ".");
        }

        if (StaffingLaborBridgeAuthority.isDefenseStation(station)) {
            PassiveDefenseProfile p = PassiveDefenseEffectsAuthority.profile(station.symbol);
            if (p != null && p.family.contains("turret")) notes.add("turret station remains live-fire dormant; staffing is readiness metadata only.");
            else notes.add("passive defense staffing affects inspection/readiness language only in this phase.");
        } else {
            notes.add("machine staffing does not own production outcomes in this phase.");
        }

        if (!skillOk) {
            return new ValidationResult(false, false,
                    "Validation failed: " + worker.name + " lacks required skill for " + station.name + " (" + role.label + ").", notes);
        }
        boolean ideal = dutyOk;
        String summary = ideal
                ? "Validation passed: " + worker.name + " is a clean " + role.label + " fit for " + station.name + "."
                : "Validation passed with warning: " + worker.name + " can staff " + station.name + " but duty does not match preferred " + preferred + ".";
        return new ValidationResult(true, ideal, summary, notes);
    }

    static String compactLine(GamePanel g, BaseObject station, RecruitWorker worker) {
        ValidationResult r = validate(g, station, worker);
        String status = r.allowed ? (r.ideal ? "ideal" : "allowed-warning") : "blocked";
        return "Staff validation " + VERSION + ": " + status + " — " + r.summary;
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Staffing Role / Skill Validation " + VERSION + " — evaluates assignment-time role and skill fit.");
        if (g == null) { out.add("  No active game panel."); return out; }
        BaseObject station = g.selectedStaffingStation();
        RecruitWorker worker = g.selectedStaffingWorker();
        ValidationResult r = validate(g, station, worker);
        out.add("  " + r.summary);
        int shown = 0;
        for (String n : r.notes) {
            out.add("  - " + n);
            if (++shown >= 5) break;
        }
        out.add("  Efficiency: evaluated only for selected station/worker assignment or inspection.");
        return out;
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Staffing Role / Skill Validation " + VERSION);
        out.add("Purpose: prevent nonsensical manual staffing assignments before logistics, shifts, or autonomous labor exist.");
        out.add("Hard rule: workers below the minimum role skill cannot be assigned to that station.");
        out.add("Soft rule: duty mismatches are allowed with warnings so early testing is not over-constrained.");
        out.add("Role minimums: guard 1, maintenance 1, operator 2, loader 2, technician 3.");
        out.add("Efficiency: validation runs only when assigning or inspecting the selected station/worker pair; it performs no map scan, pathfinding, production tick, or global labor scheduling.");
        return out;
    }
}
