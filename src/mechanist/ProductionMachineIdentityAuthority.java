package mechanist;

import java.util.List;

/** Resolves the exact producing station identity shared by forecast and provenance. */
final class ProductionMachineIdentityAuthority {
    record MachineIdentity(String provenanceLabel, List<String> lines) { }

    private ProductionMachineIdentityAuthority() { }

    static MachineIdentity evaluate(BaseObject machine) {
        if (machine == null) {
            return new MachineIdentity("manual workbench",
                    List.of("Producing machine identity: manual workbench; no placed station is attached to this run."));
        }
        String name = machine.name == null || machine.name.isBlank() ? "unnamed production station" : machine.name;
        String role = MachineTierAuthority.isMachineOrFacilitySymbol(machine.symbol)
                ? "production station " + machine.symbol
                : "work object " + machine.symbol;
        String label = name + " / " + role + " at " + machine.x + "," + machine.y;
        return new MachineIdentity(label, List.of("Producing machine identity: " + label + "."));
    }
}
