package mechanist;

import java.util.List;

/** Interprets the legacy absolute machine-integrity value for production. */
final class MachineConditionProductionAuthority {
    record Condition(String label, boolean operational, int defectRiskAdd, String guidance) { }

    private MachineConditionProductionAuthority() { }

    static Condition evaluate(BaseObject machine) {
        if (machine == null) return new Condition("unavailable", false, 0, "Build or select the required machine.");
        if (machine.integrity <= 0) return new Condition("broken", false, 0, "Repair the machine before production.");
        if (machine.integrity == 1) return new Condition("critical", true, 12, "Production remains possible, but defects are much more likely.");
        if (machine.integrity == 2) return new Condition("worn", true, 6, "Production remains possible, but defects are more likely.");
        return new Condition("serviceable", true, 0, "No defect surcharge from machine condition.");
    }

    static List<String> forecastLines(BaseObject machine) {
        Condition condition = evaluate(machine);
        return List.of(
                "Machine condition: " + condition.label + "; " + condition.guidance,
                condition.defectRiskAdd == 0
                        ? "Machine-condition defect adjustment: none."
                        : "Machine-condition defect adjustment: +" + condition.defectRiskAdd + " percentage points.");
    }
}
