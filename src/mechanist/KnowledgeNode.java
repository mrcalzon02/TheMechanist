package mechanist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Single character-owned knowledge/progression unlock node.
 * Rendering concerns live in TreeCanvasPanel; this class only owns node identity,
 * cost, prerequisite references, persisted unlock state, and tree coordinates.
 */
public final class KnowledgeNode {
    private final String id;
    private final String name;
    private final String shortDescription;
    private final String longDescription;
    private final int pointCost;
    private boolean unlocked;
    private final List<String> prerequisiteNodeIds;
    private final double x;
    private final double y;

    public KnowledgeNode(String id,
                         String name,
                         String shortDescription,
                         String longDescription,
                         int pointCost,
                         boolean unlocked,
                         List<String> prerequisiteNodeIds,
                         double x,
                         double y) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.shortDescription = shortDescription == null ? "" : shortDescription.trim();
        this.longDescription = longDescription == null ? "" : longDescription.trim();
        this.pointCost = Math.max(0, pointCost);
        this.unlocked = unlocked;
        this.prerequisiteNodeIds = Collections.unmodifiableList(new ArrayList<>(prerequisiteNodeIds == null ? List.of() : prerequisiteNodeIds));
        this.x = x;
        this.y = y;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("KnowledgeNode " + field + " must not be blank.");
        return value.trim();
    }

    public String id() { return id; }
    public String name() { return name; }
    public String shortDescription() { return shortDescription; }
    public String longDescription() { return longDescription; }
    public int pointCost() { return pointCost; }
    public boolean unlocked() { return unlocked; }
    public List<String> prerequisiteNodeIds() { return prerequisiteNodeIds; }
    public double x() { return x; }
    public double y() { return y; }

    void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public String costLabel() { return pointCost == 1 ? "1 knowledge credit" : pointCost + " knowledge credits"; }

    @Override public boolean equals(Object other) {
        return other instanceof KnowledgeNode node && id.equals(node.id);
    }

    @Override public int hashCode() { return Objects.hash(id); }

    @Override public String toString() {
        return name + " [" + id + "] cost=" + pointCost + " unlocked=" + unlocked;
    }
}
