package mechanist.modapi;

import java.util.List;

public record ResearchNode(String id, String displayName, List<String> prerequisites, List<String> unlockedBlueprints, boolean unlocked) {
    public ResearchNode {
        id = SimulationContext.cleanId(id);
        displayName = SimulationContext.safe(displayName).isEmpty() ? id : SimulationContext.safe(displayName);
        prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
        unlockedBlueprints = unlockedBlueprints == null ? List.of() : List.copyOf(unlockedBlueprints);
    }
    public ResearchNode asUnlocked() { return new ResearchNode(id, displayName, prerequisites, unlockedBlueprints, true); }
}
