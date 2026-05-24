package mechanist.modapi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Modder-facing room archetype/runtime node used by Room Editor hooks. */
public final class RoomNode {
    private final String id;
    private String displayName;
    private RoomDimensions dimensions;
    private boolean oxygenSealed;
    private int securityTerminalCount;
    private double oxygenPercent = 21.0;
    private final ArrayList<PlacementNode> placementNodes = new ArrayList<>();
    private final LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();

    public RoomNode(String id, String displayName, RoomDimensions dimensions, boolean oxygenSealed, int securityTerminalCount) {
        this.id = SimulationContext.cleanId(id);
        this.displayName = SimulationContext.safe(displayName).isEmpty() ? this.id : SimulationContext.safe(displayName);
        this.dimensions = Objects.requireNonNull(dimensions, "dimensions");
        this.oxygenSealed = oxygenSealed;
        this.securityTerminalCount = Math.max(0, Math.min(64, securityTerminalCount));
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public RoomDimensions dimensions() { return dimensions; }
    public boolean oxygenSealed() { return oxygenSealed; }
    public int securityTerminalCount() { return securityTerminalCount; }
    public double oxygenPercent() { return oxygenPercent; }
    public List<PlacementNode> placementNodes() { return List.copyOf(placementNodes); }
    public Map<String, Object> attributes() { return Map.copyOf(attributes); }

    public void setDisplayName(String displayName) {
        String clean = SimulationContext.safe(displayName);
        if (!clean.isEmpty()) this.displayName = clean;
    }

    public void setDimensions(RoomDimensions dimensions) { this.dimensions = Objects.requireNonNull(dimensions, "dimensions"); }
    public void setOxygenSealed(boolean oxygenSealed) { this.oxygenSealed = oxygenSealed; }
    public void setSecurityTerminalCount(int securityTerminalCount) { this.securityTerminalCount = Math.max(0, Math.min(64, securityTerminalCount)); }
    public void setOxygenPercent(double oxygenPercent) { this.oxygenPercent = Math.max(0.0, Math.min(100.0, oxygenPercent)); }

    public void addPlacementNode(PlacementNode node) {
        PlacementNode safe = Objects.requireNonNull(node, "node");
        for (PlacementNode existing : placementNodes) if (existing.id().equals(safe.id())) return;
        placementNodes.add(safe);
    }

    public void setAttribute(String key, Object value) {
        String clean = SimulationContext.cleanId(key);
        if (value == null) attributes.remove(clean); else attributes.put(clean, value);
    }
}
