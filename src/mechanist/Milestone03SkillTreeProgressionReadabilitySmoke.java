package mechanist;

import java.util.List;

/** Smoke for the first Phase 15 skill-tree branch/node readability contract. */
final class Milestone03SkillTreeProgressionReadabilitySmoke {
    public static void main(String[] args) {
        List<SkillTreeProgressionAuthority.SkillBranch> branches = SkillTreeProgressionAuthority.branches();
        List<SkillTreeProgressionAuthority.SkillNode> nodes = SkillTreeProgressionAuthority.allNodes();
        require(branches.size() >= 5, "expected multiple player-facing skill branches");
        require(nodes.size() >= 6, "expected capability nodes, not just branch headings");
        for (SkillTreeProgressionAuthority.SkillNode node : nodes) {
            require(branches.stream().anyMatch(branch -> branch.id().equals(node.branchId())),
                    "node should belong to a declared branch: " + node.name());
            require(node.xpCost() > 0, "node should have a positive XP cost: " + node.name());
            require(!node.capability().isBlank(), "node should name a capability: " + node.name());
            require(!node.visibleEffect().isBlank(), "node should name a visible effect: " + node.name());
            require(!node.statRequirement().isBlank(), "node should name stat requirement boundary: " + node.name());
            require(!node.statEffect().isBlank(), "node should name stat effect boundary: " + node.name());
            require(!node.exclusiveGroup().isBlank(), "node should name exclusivity boundary: " + node.name());
            require(!node.capabilityKey().isBlank(), "node should expose a capability key: " + node.name());
            require(!node.passiveBonus().isBlank(), "node should name passive hook boundary: " + node.name());
            require(!node.activeAbility().isBlank(), "node should name active hook boundary: " + node.name());
            require(node.knowledgeBoundary().toLowerCase(java.util.Locale.ROOT).contains("knowledge")
                    || node.knowledgeBoundary().toLowerCase(java.util.Locale.ROOT).contains("doctrine"),
                    "node should distinguish skill from knowledge: " + node.name());
        }
        requireContains(SkillTreeProgressionAuthority.summaryLines(), "XP buys durable capabilities",
                "XP capability summary");
        requireContains(SkillTreeProgressionAuthority.summaryLines(), "access-gate validation exist",
                "implementation boundary");
        requireContains(SkillTreeProgressionAuthority.infopediaLines(), "Knowledge Tree", "knowledge separation");
        requireContains(SkillTreeProgressionAuthority.infopediaLines(), "faction standing", "world access gates");
        requireContains(SkillTreeProgressionAuthority.infopediaLines(), "specialist conversations can offer Train",
                "in-person trainer route");
        requireContains(SkillTreeProgressionAuthority.infopediaLines(), "stat threshold", "stat gates");
        requireContains(SkillTreeProgressionAuthority.infopediaLines(), "mutually exclusive", "specialization exclusivity");
        requireContains(SkillTreeProgressionAuthority.infopediaLines(), "capability keys", "capability hooks");
        requireContains(SkillTreeProgressionAuthority.infopediaLines(), "skill_unlock <node id>", "spending route");
        requireContains(SkillTreeProgressionAuthority.infopediaLines(), "definition audit", "definition audit explanation");
        requireContains(SkillTreeProgressionAuthority.infopediaLines(), "Milestone03SkillTreeDefinitionAuditSmoke", "definition audit guard");
        require(nodes.stream().anyMatch(node -> !"none".equalsIgnoreCase(node.accessRequirement())),
                "expected at least one gated skill node");
        require(nodes.stream().anyMatch(node -> !"none".equalsIgnoreCase(node.statRequirement())
                        && !"none".equalsIgnoreCase(node.statEffect())),
                "expected at least one stat-gated skill node");
        require(nodes.stream().filter(node -> "trade-appraisal-specialization".equals(node.exclusiveGroup())).count() >= 2,
                "expected a mutually exclusive trade specialization pair");
        require(nodes.stream().anyMatch(node -> !"none".equalsIgnoreCase(node.passiveBonus())),
                "expected at least one passive skill hook");
        require(nodes.stream().anyMatch(node -> !"none".equalsIgnoreCase(node.activeAbility())),
                "expected at least one active skill hook");
        require(SkillTreeProgressionAuthority.auditLine().contains("knowledgeDistinct=true"),
                "audit line should expose skill/knowledge separation");
        require(SkillTreeProgressionAuthority.auditLine().contains("accessGates="),
                "audit line should expose access-gate contract");
        require(SkillTreeProgressionAuthority.auditLine().contains("statGates=true"),
                "audit line should expose stat-gate contract");
        require(SkillTreeProgressionAuthority.auditLine().contains("exclusiveGroups=true"),
                "audit line should expose specialization exclusivity contract");
        require(SkillTreeProgressionAuthority.auditLine().contains("capabilityHooks=true"),
                "audit line should expose capability hook contract");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03SkillTreeProgressionReadabilitySmoke() { }
}
