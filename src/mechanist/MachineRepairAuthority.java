package mechanist;

import java.util.List;

/** Bounded player repair workflow for owned production machines. */
final class MachineRepairAuthority {
    static final int SERVICEABLE_INTEGRITY = 3;
    static final int PART_COST = 1;
    static final int RESTORE_AMOUNT = 2;
    static final int TRAINED_RESTORE_AMOUNT = 3;

    record RepairPreview(boolean available, int partCost, int currentIntegrity, int projectedIntegrity, String summary) { }

    private MachineRepairAuthority() { }

    static RepairPreview preview(BaseObject machine, int availableParts) {
        return preview(null, machine, availableParts);
    }

    static RepairPreview preview(GamePanel game, BaseObject machine, int availableParts) {
        if (machine == null) return new RepairPreview(false, PART_COST, 0, 0, "Repair unavailable: no owned machine selected.");
        if (FactionPhysicalConstructionAuthority.isFactionManaged(machine)) {
            int current = Math.max(0, machine.integrity);
            return new RepairPreview(false, 0, current, current,
                    "Repair unavailable: this faction facility retains its assigned maintenance roster.");
        }
        int current = Math.max(0, machine.integrity);
        if (current >= SERVICEABLE_INTEGRITY) {
            return new RepairPreview(false, 0, current, current, "Repair unnecessary: machine condition is already serviceable.");
        }
        boolean trained = game != null && SkillTreeProgressionAuthority.hasCapability(
                game.unlockedSkillNodes, "fab-repair-forge-tutoring");
        int restore = trained ? TRAINED_RESTORE_AMOUNT : RESTORE_AMOUNT;
        int projected = Math.min(SERVICEABLE_INTEGRITY, current + restore);
        if (availableParts < PART_COST) {
            return new RepairPreview(false, PART_COST, current, projected,
                    "Repair unavailable: need 1 machine part; available " + Math.max(0, availableParts) + ".");
        }
        return new RepairPreview(true, PART_COST, current, projected,
                "Repair ready: spend 1 machine part and 1 turn; integrity " + current + " -> " + projected
                        + (trained ? " with Forge-Tutored Repair." : "."));
    }

    static List<String> detailLines(BaseObject machine, int availableParts) {
        return detailLines(null, machine, availableParts);
    }

    static List<String> detailLines(GamePanel game, BaseObject machine, int availableParts) {
        RepairPreview preview = preview(game, machine, availableParts);
        boolean trained = game != null && SkillTreeProgressionAuthority.hasCapability(
                game.unlockedSkillNodes, "fab-repair-forge-tutoring");
        return List.of(preview.summary(),
                "Field repair restores at most " + (trained ? TRAINED_RESTORE_AMOUNT : RESTORE_AMOUNT)
                        + " integrity and stops at serviceable integrity "
                        + SERVICEABLE_INTEGRITY + "; it does not rebuild original maximum integrity.");
    }
}
