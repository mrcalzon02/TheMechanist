package mechanist;

import java.util.List;

/** Bounded player repair workflow for owned production machines. */
final class MachineRepairAuthority {
    static final int SERVICEABLE_INTEGRITY = 3;
    static final int PART_COST = 1;
    static final int RESTORE_AMOUNT = 2;

    record RepairPreview(boolean available, int partCost, int currentIntegrity, int projectedIntegrity, String summary) { }

    private MachineRepairAuthority() { }

    static RepairPreview preview(BaseObject machine, int availableParts) {
        if (machine == null) return new RepairPreview(false, PART_COST, 0, 0, "Repair unavailable: no owned machine selected.");
        int current = Math.max(0, machine.integrity);
        if (current >= SERVICEABLE_INTEGRITY) {
            return new RepairPreview(false, 0, current, current, "Repair unnecessary: machine condition is already serviceable.");
        }
        int projected = Math.min(SERVICEABLE_INTEGRITY, current + RESTORE_AMOUNT);
        if (availableParts < PART_COST) {
            return new RepairPreview(false, PART_COST, current, projected,
                    "Repair unavailable: need 1 machine part; available " + Math.max(0, availableParts) + ".");
        }
        return new RepairPreview(true, PART_COST, current, projected,
                "Repair ready: spend 1 machine part and 1 turn; integrity " + current + " -> " + projected + ".");
    }

    static List<String> detailLines(BaseObject machine, int availableParts) {
        RepairPreview preview = preview(machine, availableParts);
        return List.of(preview.summary(),
                "Field repair restores at most " + RESTORE_AMOUNT + " integrity and stops at serviceable integrity "
                        + SERVICEABLE_INTEGRITY + "; it does not rebuild original maximum integrity.");
    }
}
