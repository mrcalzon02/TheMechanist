package mechanist;

/** Persists the bounded player and faction repair/replacement provenance ledger. */
final class MachineRepairHistoryAuthority {
    private static final int MAX_HISTORY_CHARS = 900;

    private MachineRepairHistoryAuthority() { }

    static String recordLine(BaseObject machine, MachineRepairAuthority.RepairPreview preview, int turn, String actor) {
        if (machine == null || preview == null || !preview.available()) return "";
        return "field repair turn " + Math.max(0, turn)
                + ": " + safe(actor, "player")
                + " restored " + safe(machine.name, "machine")
                + " integrity " + preview.currentIntegrity() + "->" + preview.projectedIntegrity()
                + " using " + preview.partCost() + " machine part";
    }

    static String recordFactionLine(BaseObject machine, int integrityBefore,
                                    int integrityAfter, int partCost, int turn,
                                    String actor, boolean replacement) {
        if (machine == null) return "";
        String line = "faction " + (replacement ? "replacement" : "repair")
                + " turn " + Math.max(0, turn)
                + ": " + safe(actor, "assigned faction maintenance crew")
                + " restored " + safe(machine.name, "machine")
                + " integrity " + Math.max(0, integrityBefore) + "->"
                + Math.max(0, integrityAfter) + " using "
                + Math.max(0, partCost) + " prepaid maintenance stock";
        String prior = provenanceLabel(machine);
        String combined = prior.isBlank() ? line : prior + " || " + line;
        if (combined.length() <= MAX_HISTORY_CHARS) return combined;
        return combined.substring(combined.length() - MAX_HISTORY_CHARS);
    }

    static String provenanceLabel(BaseObject machine) {
        if (machine == null || machine.machineRepairHistory == null || machine.machineRepairHistory.isBlank()) return "";
        return machine.machineRepairHistory.trim();
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().replace('|', '/');
    }
}
