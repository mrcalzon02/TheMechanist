package mechanist;

/** Formats the narrow repair-history note owned by the current machine repair flow. */
final class MachineRepairHistoryAuthority {
    private MachineRepairHistoryAuthority() { }

    static String recordLine(BaseObject machine, MachineRepairAuthority.RepairPreview preview, int turn, String actor) {
        if (machine == null || preview == null || !preview.available()) return "";
        return "field repair turn " + Math.max(0, turn)
                + ": " + safe(actor, "player")
                + " restored " + safe(machine.name, "machine")
                + " integrity " + preview.currentIntegrity() + "->" + preview.projectedIntegrity()
                + " using " + preview.partCost() + " machine part";
    }

    static String provenanceLabel(BaseObject machine) {
        if (machine == null || machine.machineRepairHistory == null || machine.machineRepairHistory.isBlank()) return "";
        return machine.machineRepairHistory.trim();
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().replace('|', '/');
    }
}
