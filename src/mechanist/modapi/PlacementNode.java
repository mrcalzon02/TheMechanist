package mechanist.modapi;

public record PlacementNode(String id, int x, int y, String nodeType) {
    public PlacementNode {
        id = SimulationContext.cleanId(id);
        x = Math.max(0, x);
        y = Math.max(0, y);
        nodeType = SimulationContext.safe(nodeType).isEmpty() ? "generic" : SimulationContext.safe(nodeType);
    }
}
