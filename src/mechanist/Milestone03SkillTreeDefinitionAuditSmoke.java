package mechanist;

import java.util.List;

/** Smoke for the Phase 18 skill-tree definition audit surface. */
final class Milestone03SkillTreeDefinitionAuditSmoke {
    public static void main(String[] args) {
        List<String> audit = SkillTreeProgressionAuthority.definitionAuditLines();
        requireContains(audit, "owner=SkillTreeProgressionAuthority", "definition owner");
        requireContains(audit, "branches=5", "branch count");
        requireContains(audit, "nodes=", "node count");
        requireContains(audit, "xpCosts=true", "XP cost coverage");
        requireContains(audit, "dependencies=true", "dependency coverage");
        requireContains(audit, "accessGates=true", "access gate coverage");
        requireContains(audit, "statModifiers=true", "stat modifier coverage");
        requireContains(audit, "exclusiveGroups=1", "exclusive group count");
        requireContains(audit, "capabilityHooks=true", "capability hook coverage");
        requireContains(audit, "knowledgeDistinct=true", "knowledge boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "ordinary UI raw-ID boundary");

        requireContains(audit, "Skill branch audit: Trade and Appraisal", "trade branch");
        requireContains(audit, "Skill node audit: Certified Market Appraisal", "certified appraisal node");
        requireContains(audit, "xpCost=55", "certified appraisal XP cost");
        requireContains(audit, "prerequisite=Batch Appraisal", "readable prerequisite");
        requireContains(audit, "faction Mechanist Collegia standing 20", "readable faction gate");
        requireContains(audit, "exclusive=trade appraisal specialization", "readable specialization group");
        requireContains(audit, "capabilityHook=trade guilder certification", "readable capability hook");
        requireContains(audit, "Skill node audit: Master Workshop Practice", "workshop node");
        requireContains(audit, "access=facility forge fabrication stall", "readable facility gate");
        requireContains(audit, "statRequirement=Mechanics:8", "stat requirement");
        requireContains(audit, "statEffect=Mechanics:+1", "stat effect");
        requireContains(audit, "Skill node audit: Forge-Tutored Repair", "trainer-gated repair node");
        requireContains(audit, "access=trainer forge tutor", "readable trainer gate");
        require(SkillTreeProgressionAuthority.auditLine().contains("spendingUi=character-skills-tab+console-route"),
                "skill audit should expose shared Character-panel and console spending routes");
        requireContains(audit, "Milestone03SkillTreeDefinitionAuditSmoke", "guard reference");

        rejectRawNodeId(audit, "trade-guilder-certification");
        rejectRawNodeId(audit, "fab-repair-master-workshop");
        rejectRawNodeId(audit, "machine-pressure-discipline");
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectRawNodeId(List<String> lines, String forbidden) {
        for (String line : lines) {
            if (line != null && line.contains(forbidden)) {
                throw new AssertionError("Skill-tree definition audit leaked raw node id '" + forbidden + "': " + line);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone03SkillTreeDefinitionAuditSmoke() { }
}
