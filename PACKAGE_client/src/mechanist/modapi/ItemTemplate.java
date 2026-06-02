package mechanist.modapi;

import java.util.LinkedHashMap;
import java.util.Map;

/** Modder-facing item template/runtime state used by inventory and item-consumption hooks. */
public final class ItemTemplate {
    private final String id;
    private String displayName;
    private int techTier;
    private double massKg;
    private int fuelCharges;
    private int durability;
    private final LinkedHashMap<String, Integer> components = new LinkedHashMap<>();
    private final LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();

    public ItemTemplate(String id, String displayName, int techTier, double massKg, int fuelCharges, int durability) {
        this.id = SimulationContext.cleanId(id);
        this.displayName = SimulationContext.safe(displayName).isEmpty() ? this.id : SimulationContext.safe(displayName);
        this.techTier = Math.max(0, Math.min(99, techTier));
        this.massKg = Math.max(0.0, Math.min(1_000_000.0, massKg));
        this.fuelCharges = Math.max(0, fuelCharges);
        this.durability = Math.max(0, Math.min(100, durability));
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public int techTier() { return techTier; }
    public double massKg() { return massKg; }
    public int fuelCharges() { return fuelCharges; }
    public int durability() { return durability; }
    public Map<String, Integer> components() { return Map.copyOf(components); }
    public Map<String, Object> attributes() { return Map.copyOf(attributes); }

    public void setTechTier(int techTier) { this.techTier = Math.max(0, Math.min(99, techTier)); }
    public void setMassKg(double massKg) { this.massKg = Math.max(0.0, Math.min(1_000_000.0, massKg)); }
    public void setFuelCharges(int fuelCharges) { this.fuelCharges = Math.max(0, fuelCharges); }
    public void setDurability(int durability) { this.durability = Math.max(0, Math.min(100, durability)); }
    public void addComponent(String itemId, int count) { components.put(SimulationContext.cleanId(itemId), Math.max(1, count)); }
    public boolean consumeCharge() { if (fuelCharges <= 0) return false; fuelCharges--; return true; }
    public void damage(int amount) { setDurability(durability - Math.max(0, amount)); }

    public void setAttribute(String key, Object value) {
        String clean = SimulationContext.cleanId(key);
        if (value == null) attributes.remove(clean); else attributes.put(clean, value);
    }
}
