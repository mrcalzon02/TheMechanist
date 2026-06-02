package mechanist.modapi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Modder-facing faction profile used by diplomacy, economy, and culture hooks. */
public final class FactionProfile {
    private final String id;
    private String displayName;
    private AlignmentVector alignment;
    private final LinkedHashMap<String, Integer> economicResources = new LinkedHashMap<>();
    private final LinkedHashMap<String, Integer> aggressionMatrix = new LinkedHashMap<>();
    private final ArrayList<String> culturalTraits = new ArrayList<>();
    private final LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();

    public FactionProfile(String id, String displayName, AlignmentVector alignment) {
        this.id = SimulationContext.cleanId(id);
        this.displayName = SimulationContext.safe(displayName).isEmpty() ? this.id : SimulationContext.safe(displayName);
        this.alignment = Objects.requireNonNull(alignment, "alignment");
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public AlignmentVector alignment() { return alignment; }
    public Map<String, Integer> economicResources() { return Map.copyOf(economicResources); }
    public Map<String, Integer> aggressionMatrix() { return Map.copyOf(aggressionMatrix); }
    public List<String> culturalTraits() { return List.copyOf(culturalTraits); }
    public Map<String, Object> attributes() { return Map.copyOf(attributes); }

    public void setAlignment(AlignmentVector alignment) { this.alignment = Objects.requireNonNull(alignment, "alignment"); }
    public void setEconomicResource(String resourceId, int amount) { economicResources.put(SimulationContext.cleanId(resourceId), Math.max(0, amount)); }
    public void setAggressionToward(String targetFactionId, int aggression) { aggressionMatrix.put(SimulationContext.cleanId(targetFactionId), Math.max(0, Math.min(100, aggression))); }
    public int aggressionToward(String targetFactionId) { return aggressionMatrix.getOrDefault(SimulationContext.cleanId(targetFactionId), 0); }

    public void addCulturalTrait(String trait) {
        String clean = SimulationContext.safe(trait);
        if (!clean.isEmpty() && !culturalTraits.contains(clean)) culturalTraits.add(clean);
    }

    public void setAttribute(String key, Object value) {
        String clean = SimulationContext.cleanId(key);
        if (value == null) attributes.remove(clean); else attributes.put(clean, value);
    }
}
