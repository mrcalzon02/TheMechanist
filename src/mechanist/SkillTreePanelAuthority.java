package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds player-facing Character-panel skill-tree rows and unlock previews. */
final class SkillTreePanelAuthority {
    private SkillTreePanelAuthority() { }

    static List<SkillTreeProgressionAuthority.SkillBranch> branches() {
        return SkillTreeProgressionAuthority.branches();
    }

    static List<SkillTreeProgressionAuthority.SkillNode> nodesForBranch(
            SkillTreeProgressionAuthority.SkillBranch branch) {
        return branch == null ? List.of() : branch.nodes();
    }

    static String branchLabel(GamePanel game, SkillTreeProgressionAuthority.SkillBranch branch) {
        if (branch == null) return "No branch";
        int unlocked = 0;
        for (SkillTreeProgressionAuthority.SkillNode node : branch.nodes()) {
            if (game != null && game.unlockedSkillNodes.contains(node.id())) unlocked++;
        }
        return branch.name() + "  " + unlocked + "/" + branch.nodes().size();
    }

    static String nodeLabel(GamePanel game, SkillTreeProgressionAuthority.SkillNode node) {
        if (node == null) return "No skill node";
        return node.name() + "  " + node.xpCost() + " XP  " + state(game, node);
    }

    static ArrayList<String> detailLines(GamePanel game, SkillTreeProgressionAuthority.SkillBranch branch,
                                         SkillTreeProgressionAuthority.SkillNode node) {
        ArrayList<String> lines = new ArrayList<>();
        if (branch == null || node == null) {
            lines.add("No skill node is selected.");
            return lines;
        }
        lines.add("Branch: " + branch.name() + ". " + branch.worldUse());
        lines.add("XP: " + Math.max(0, game == null ? 0 : game.xp)
                + " available; " + node.xpCost() + " required.");
        lines.add("Status: " + state(game, node) + ". " + previewMessage(game, node));
        lines.add("Prerequisite: " + humanizeNone(node.prerequisite()) + ".");
        lines.add("Access: " + accessLabel(node.accessRequirement()) + ".");
        if (game != null && game.activeSkillTrainerLabel != null && !game.activeSkillTrainerLabel.isBlank()) {
            lines.add("Trainer present: " + game.activeSkillTrainerLabel + ".");
        }
        lines.add("Stat requirement: " + humanizeNone(node.statRequirement())
                + "; unlock effect: " + humanizeNone(node.statEffect()) + ".");
        lines.add("Capability: " + node.capability());
        lines.add("Visible effect: " + node.visibleEffect());
        lines.add("Knowledge boundary: " + node.knowledgeBoundary());
        if (!none(node.exclusiveGroup())) {
            lines.add("Specialization: choosing this locks the other "
                    + humanize(node.exclusiveGroup()) + " option.");
        }
        return lines;
    }

    static String previewMessage(GamePanel game, SkillTreeProgressionAuthority.SkillNode node) {
        if (node == null) return "Select a skill node.";
        if (game != null && game.unlockedSkillNodes.contains(node.id())) return "This capability is already learned.";
        SkillTreeProgressionAuthority.SpendResult preview = preview(game, node);
        return preview.success() ? "Ready to unlock." : humanize(preview.message());
    }

    private static String state(GamePanel game, SkillTreeProgressionAuthority.SkillNode node) {
        if (node == null) return "Unavailable";
        if (game != null && game.unlockedSkillNodes.contains(node.id())) return "Unlocked";
        return preview(game, node).success() ? "Available" : "Locked";
    }

    private static SkillTreeProgressionAuthority.SpendResult preview(
            GamePanel game, SkillTreeProgressionAuthority.SkillNode node) {
        if (game == null) return SkillTreeProgressionAuthority.spendXp(null, 0, node == null ? "" : node.name());
        Map<String, Integer> stats = game.active == null ? Map.of() : game.active.stats;
        return SkillTreeProgressionAuthority.spendXp(game.unlockedSkillNodes, game.xp, node.name(),
                SkillTreeProgressionAuthority.SkillAccessContext.fromGame(game), stats);
    }

    private static String accessLabel(String access) {
        if (none(access)) return "none";
        String[] parts = access.split(":", 3);
        String kind = parts[0].toLowerCase(Locale.ROOT);
        String target = parts.length > 1 ? humanize(parts[1]) : "world access";
        if ("faction".equals(kind)) {
            String standing = parts.length > 2 ? " standing " + parts[2] : " standing";
            return target + standing;
        }
        return humanize(kind) + " " + target;
    }

    private static String humanizeNone(String value) {
        return none(value) ? "none" : humanize(value);
    }

    private static String humanize(String value) {
        return value == null ? "" : value.replace('-', ' ').replace('_', ' ');
    }

    private static boolean none(String value) {
        return value == null || value.isBlank() || "none".equalsIgnoreCase(value);
    }
}
