package mechanist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Character progression branch.  It owns node membership, prerequisite checks,
 * available point accounting, and unlock transactions.  It deliberately contains
 * no Swing or render code.
 */
public final class KnowledgeTree {
    public enum NodeState { LOCKED, AVAILABLE, UNLOCKED }

    public record UnlockResult(boolean success, String message, KnowledgeNode node, int remainingPoints) {}

    private final String id;
    private final String displayName;
    private final LinkedHashMap<String, KnowledgeNode> nodes = new LinkedHashMap<>();
    private int availableKnowledgePoints;
    private Set<String> externalUnlockedNodeIds = new LinkedHashSet<>();

    public KnowledgeTree(String id, String displayName, int availableKnowledgePoints) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("KnowledgeTree id must not be blank.");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("KnowledgeTree displayName must not be blank.");
        this.id = id.trim();
        this.displayName = displayName.trim();
        this.availableKnowledgePoints = Math.max(0, availableKnowledgePoints);
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public int availableKnowledgePoints() { return availableKnowledgePoints; }

    public void setAvailableKnowledgePoints(int availableKnowledgePoints) {
        this.availableKnowledgePoints = Math.max(0, availableKnowledgePoints);
    }

    public void setExternalUnlockedNodeIds(Collection<String> unlockedIds) {
        this.externalUnlockedNodeIds = new LinkedHashSet<>();
        if (unlockedIds != null) {
            for (String id : unlockedIds) if (id != null && !id.isBlank()) this.externalUnlockedNodeIds.add(id.trim());
        }
    }

    public KnowledgeTree addNode(KnowledgeNode node) {
        Objects.requireNonNull(node, "node");
        if (nodes.containsKey(node.id())) throw new IllegalArgumentException("Duplicate knowledge node id: " + node.id());
        nodes.put(node.id(), node);
        return this;
    }

    public KnowledgeNode node(String nodeId) { return nodes.get(nodeId); }

    public List<KnowledgeNode> nodes() { return Collections.unmodifiableList(new ArrayList<>(nodes.values())); }

    public Map<String, KnowledgeNode> nodeMap() { return Collections.unmodifiableMap(nodes); }

    public boolean containsNode(String nodeId) { return nodes.containsKey(nodeId); }

    public boolean prerequisitesMet(String nodeId) {
        KnowledgeNode node = nodes.get(nodeId);
        return node != null && prerequisitesMet(node);
    }

    public boolean prerequisitesMet(KnowledgeNode node) {
        if (node == null) return false;
        for (String prerequisiteId : node.prerequisiteNodeIds()) {
            KnowledgeNode prerequisite = nodes.get(prerequisiteId);
            if (prerequisite != null) {
                if (!prerequisite.unlocked()) return false;
            } else if (!externalUnlockedNodeIds.contains(prerequisiteId)) {
                return false;
            }
        }
        return true;
    }

    public NodeState stateOf(String nodeId) {
        KnowledgeNode node = nodes.get(nodeId);
        if (node == null) return NodeState.LOCKED;
        return stateOf(node);
    }

    public NodeState stateOf(KnowledgeNode node) {
        if (node == null) return NodeState.LOCKED;
        if (node.unlocked()) return NodeState.UNLOCKED;
        return prerequisitesMet(node) && availableKnowledgePoints >= node.pointCost() ? NodeState.AVAILABLE : NodeState.LOCKED;
    }

    public boolean canUnlock(String nodeId) {
        KnowledgeNode node = nodes.get(nodeId);
        return node != null && !node.unlocked() && prerequisitesMet(node) && availableKnowledgePoints >= node.pointCost();
    }

    public UnlockResult unlockNode(String nodeId) {
        KnowledgeNode node = nodes.get(nodeId);
        if (node == null) return new UnlockResult(false, "Unknown knowledge node: " + nodeId + ".", null, availableKnowledgePoints);
        if (node.unlocked()) return new UnlockResult(false, node.name() + " is already known.", node, availableKnowledgePoints);
        if (!prerequisitesMet(node)) return new UnlockResult(false, "Prerequisites are not met for " + node.name() + ".", node, availableKnowledgePoints);
        if (availableKnowledgePoints < node.pointCost()) {
            return new UnlockResult(false, node.name() + " costs " + node.pointCost() + " knowledge credit(s); available: " + availableKnowledgePoints + ".", node, availableKnowledgePoints);
        }
        availableKnowledgePoints -= node.pointCost();
        node.setUnlocked(true);
        externalUnlockedNodeIds.add(node.id());
        return new UnlockResult(true, "Unlocked " + node.name() + ".", node, availableKnowledgePoints);
    }

    public int unlockedCount() {
        int count = 0;
        for (KnowledgeNode node : nodes.values()) if (node.unlocked()) count++;
        return count;
    }

    public int availableCount() {
        int count = 0;
        for (KnowledgeNode node : nodes.values()) if (stateOf(node) == NodeState.AVAILABLE) count++;
        return count;
    }
}
