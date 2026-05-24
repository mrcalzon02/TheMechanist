package mechanist;

import java.util.*;

/**
 * 0.9.10d — Logistics / Hauling Reservation Scaffold.
 *
 * Intent and status only.  This authority forecasts what a staffed machine would
 * need from base storage/player-carried production inputs before any real hauling,
 * actor movement, route reservation, or delivery ownership is enabled.
 */
final class LogisticsReservationForecastAuthority {
    static final String VERSION = "0.9.10d";
    static final int MAX_DISPLAY_LINES = 8;

    private LogisticsReservationForecastAuthority() {}

    static final class ReservationLine {
        final String item;
        final int need;
        final int have;
        final int missing;
        ReservationLine(String item, int need, int have) {
            this.item = item == null ? "unknown" : item;
            this.need = Math.max(0, need);
            this.have = Math.max(0, have);
            this.missing = Math.max(0, this.need - this.have);
        }
        String display() {
            return item + " have " + have + "/" + need + (missing > 0 ? " missing " + missing : " ready");
        }
    }

    static final class ReservationForecast {
        final BaseObject station;
        final String jobLabel;
        final String sourceLabel;
        final ArrayList<ReservationLine> lines = new ArrayList<>();
        final ArrayList<String> notes = new ArrayList<>();
        boolean validStation;
        boolean hasJob;
        boolean assignedWorker;
        boolean allInputsReady;

        ReservationForecast(BaseObject station, String jobLabel, String sourceLabel) {
            this.station = station;
            this.jobLabel = jobLabel == null ? "none" : jobLabel;
            this.sourceLabel = sourceLabel == null ? "unknown" : sourceLabel;
        }

        int missingKinds() {
            int n = 0;
            for (ReservationLine l : lines) if (l.missing > 0) n++;
            return n;
        }

        int totalMissingUnits() {
            int n = 0;
            for (ReservationLine l : lines) n += l.missing;
            return n;
        }

        String compact() {
            String stationName = station == null ? "none" : station.name;
            return "Logistics reservation forecast " + VERSION + ": station=" + stationName
                    + " job=" + jobLabel
                    + " source=" + sourceLabel
                    + " worker=" + (assignedWorker ? "assigned" : "unassigned")
                    + " inputs=" + (allInputsReady ? "ready" : ("missing " + missingKinds() + " kind(s)/" + totalMissingUnits() + " unit(s)"))
                    + " mode=intent-only/no-hauling.";
        }
    }

    static ReservationForecast forecast(GamePanel g, BaseObject station) {
        ReservationForecast f = new ReservationForecast(station, "none", "no assignment");
        if (g == null) { f.notes.add("No active game panel."); return f; }
        if (station == null) { f.notes.add("No machine selected for logistics reservation forecast."); return f; }
        f.validStation = StaffingLaborBridgeAuthority.isMachineStation(station);
        f.assignedWorker = station.assignedWorker != null && !station.assignedWorker.isBlank() && !"unmanned".equalsIgnoreCase(station.assignedWorker);
        if (!f.validStation) { f.notes.add(station.name + " is not a machine logistics station."); return f; }
        String assigned = station.assignedRecipe == null ? "" : station.assignedRecipe.trim();
        if (assigned.isEmpty()) {
            f.notes.add("No assigned machine recipe/job. Reservation forecast remains idle.");
            f.allInputsReady = true;
            return f;
        }
        f.hasJob = true;

        if (ControlledProductionJobAuthority.isGeneratedAssignment(assigned)) {
            FactionRecipeVariant v = ControlledProductionJobAuthority.findVariantByAssignmentKey(assigned);
            f = new ReservationForecast(station, v == null ? assigned : v.outputName, "generated/faction variant");
            f.validStation = true;
            f.hasJob = v != null;
            f.assignedWorker = station.assignedWorker != null && !station.assignedWorker.isBlank() && !"unmanned".equalsIgnoreCase(station.assignedWorker);
            if (v == null) {
                f.notes.add("Assigned generated job no longer resolves; no reservation made.");
                return f;
            }
            for (Map.Entry<String,Integer> e : v.itemInputs.entrySet()) f.lines.add(new ReservationLine(e.getKey(), e.getValue(), g.countProductionInput(e.getKey())));
            f.notes.add("Generated job equipment requirement: " + v.equipmentSummary() + ". Equipment is checked elsewhere; this forecast records consumable-input intent only.");
            String problem = ControlledProductionJobAuthority.assignmentProblem(g, station, v);
            if (problem != null) f.notes.add("Assignment blocker: " + problem);
        } else {
            CraftingRecipe r = CraftingRecipe.byName(assigned);
            f = new ReservationForecast(station, r == null ? assigned : r.name, "known crafting recipe");
            f.validStation = true;
            f.hasJob = r != null;
            f.assignedWorker = station.assignedWorker != null && !station.assignedWorker.isBlank() && !"unmanned".equalsIgnoreCase(station.assignedWorker);
            if (r == null) {
                f.notes.add("Assigned recipe no longer resolves; no reservation made.");
                return f;
            }
            if (r.effectiveSuppliesCost() > 0) f.lines.add(new ReservationLine("supplies", r.effectiveSuppliesCost(), g.supplies));
            if (r.effectiveMachinePartsCost() > 0) f.lines.add(new ReservationLine("machine parts", r.effectiveMachinePartsCost(), g.machineParts));
            for (Map.Entry<String,Integer> e : r.itemInputs.entrySet()) f.lines.add(new ReservationLine(e.getKey(), e.getValue(), g.countCraftInput(e.getKey())));
            String problem = r.blockingProblemForMachine(g, station);
            if (problem != null) f.notes.add("Recipe blocker: " + problem);
        }
        f.allInputsReady = f.missingKinds() == 0;
        if (!f.assignedWorker) f.notes.add("No worker assigned. Reservation remains a forecast only; no hauling or crew execution is started.");
        else f.notes.add("Assigned worker present. Current logistics status records intent only and does not move workers or goods.");
        if (f.lines.isEmpty()) f.notes.add("No consumable inputs required; route reservation is idle/ready.");
        return f;
    }

    static String reserveIntent(GamePanel g, BaseObject station) {
        ReservationForecast f = forecast(g, station);
        return "LOGISTICS RESERVATION FORECAST: " + f.compact();
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics / Hauling Reservation Forecast " + VERSION + " — shows station, worker, and source readiness for manual hauling.");
        if (g == null) { out.add("  No active game panel."); return out; }
        BaseObject station = g.selectedWorkerMachine();
        ReservationForecast f = forecast(g, station);
        out.add("  " + f.compact());
        int shown = 0;
        for (ReservationLine l : f.lines) {
            if (shown >= MAX_DISPLAY_LINES) break;
            out.add("  input " + l.display());
            shown++;
        }
        if (f.lines.size() > shown) out.add("  ... " + (f.lines.size() - shown) + " additional input line(s) hidden by bounded display.");
        int noteShown = 0;
        for (String n : f.notes) {
            if (noteShown >= 4) break;
            out.add("  note: " + n);
            noteShown++;
        }
        out.add("  Efficiency: evaluates selected machine only, using existing input counts; no global hauling scan or pathfinding.");
        return out;
    }

    static String compactSummary(GamePanel g) {
        BaseObject station = g == null ? null : g.selectedWorkerMachine();
        return forecast(g, station).compact();
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Logistics / Hauling Reservation Forecast " + VERSION);
        out.add("Purpose: establish a single forecast authority for hauling/storage readiness before actors or routes move goods.");
        out.add("Current behavior: selected machine + assigned recipe/job + available inputs are read into an intent report.");
        out.add("Hard boundary: the forecast does not move workers, lock map routes, transfer items, consume inputs, or run production.");
        out.add("Efficiency: selected-machine only; no global labor scan, no per-turn hauling loop, no pathfinding, no broad storage sweep beyond existing input count helpers.");
        return out;
    }
}
