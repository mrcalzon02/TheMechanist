package mechanist.modapi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Modder-facing sector model used by the Sector Editor and runtime sector hooks. */
public final class SectorInstance {
    private final String id;
    private String displayName;
    private SectorCoordinates coordinates;
    private final ArrayList<String> environmentalHazards = new ArrayList<>();
    private final LinkedHashMap<String, Integer> factionControl = new LinkedHashMap<>();
    private final LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
    private NavigationVector navigationVector = new NavigationVector(0.0, 0.0, 0.0);

    public SectorInstance(String id, String displayName, SectorCoordinates coordinates) {
        this.id = SimulationContext.cleanId(id);
        this.displayName = SimulationContext.safe(displayName).isEmpty() ? this.id : SimulationContext.safe(displayName);
        this.coordinates = Objects.requireNonNull(coordinates, "coordinates");
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public SectorCoordinates coordinates() { return coordinates; }
    public List<String> environmentalHazards() { return List.copyOf(environmentalHazards); }
    public Map<String, Integer> factionControl() { return Map.copyOf(factionControl); }
    public Map<String, Object> attributes() { return Map.copyOf(attributes); }
    public NavigationVector navigationVector() { return navigationVector; }

    public void setDisplayName(String displayName) {
        String clean = SimulationContext.safe(displayName);
        if (!clean.isEmpty()) this.displayName = clean;
    }

    public void setCoordinates(SectorCoordinates coordinates) { this.coordinates = Objects.requireNonNull(coordinates, "coordinates"); }

    public void addEnvironmentalHazard(String hazard) {
        String clean = SimulationContext.safe(hazard);
        if (!clean.isEmpty() && !environmentalHazards.contains(clean)) environmentalHazards.add(clean);
    }

    public void setFactionControl(String factionId, int controlPercent) {
        factionControl.put(SimulationContext.cleanId(factionId), Math.max(0, Math.min(100, controlPercent)));
    }

    public void setAttribute(String key, Object value) {
        String clean = SimulationContext.cleanId(key);
        if (value == null) attributes.remove(clean); else attributes.put(clean, value);
    }

    public void setNavigationVector(NavigationVector vector) { this.navigationVector = Objects.requireNonNull(vector, "vector"); }
}
