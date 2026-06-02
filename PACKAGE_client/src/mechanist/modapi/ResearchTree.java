package mechanist.modapi;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Modder-facing progression graph used by Knowledge Editor and research callbacks. */
public final class ResearchTree {
    private final String id;
    private final LinkedHashMap<String, ResearchNode> nodes = new LinkedHashMap<>();

    public ResearchTree(String id) { this.id = SimulationContext.cleanId(id); }
    public String id() { return id; }
    public Map<String, ResearchNode> nodes() { return Map.copyOf(nodes); }
    public Optional<ResearchNode> node(String nodeId) { return Optional.ofNullable(nodes.get(SimulationContext.cleanId(nodeId))); }
    public void addNode(ResearchNode node) { nodes.put(node.id(), node); }

    public boolean canUnlock(String nodeId) {
        ResearchNode node = nodes.get(SimulationContext.cleanId(nodeId));
        if (node == null) return false;
        for (String prerequisite : node.prerequisites()) {
            ResearchNode prior = nodes.get(prerequisite);
            if (prior == null || !prior.unlocked()) return false;
        }
        return true;
    }

    public List<String> unlock(String nodeId) {
        String safeId = SimulationContext.cleanId(nodeId);
        ResearchNode node = nodes.get(safeId);
        if (node == null || !canUnlock(safeId)) return List.of();
        ResearchNode unlocked = node.asUnlocked();
        nodes.put(safeId, unlocked);
        return unlocked.unlockedBlueprints();
    }
}
