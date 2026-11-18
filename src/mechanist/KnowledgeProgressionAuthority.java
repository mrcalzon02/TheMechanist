package mechanist;

/** Earned-XP conversion and live-state knowledge purchases shared by UI and tests. */
final class KnowledgeProgressionAuthority {
    static final int XP_PER_CREDIT = 25;

    private KnowledgeProgressionAuthority() { }

    static void awardXp(GamePanel game, int amount) {
        if (game == null || amount <= 0) return;
        game.xp = (int)Math.min(Integer.MAX_VALUE, (long)Math.max(0, game.xp) + amount);
        long progress = Math.max(0, Math.min(XP_PER_CREDIT - 1, game.knowledgeXpProgress)) + (long)amount;
        int earned = (int)(progress / XP_PER_CREDIT);
        game.knowledgeXpProgress = (int)(progress % XP_PER_CREDIT);
        game.knowledgeCredits = (int)Math.min(Integer.MAX_VALUE, (long)Math.max(0, game.knowledgeCredits) + earned);
        if (earned > 0) game.logEvent("Earned " + earned + " knowledge credit(s) from experience. Available: " + game.knowledgeCredits + ".");
    }

    static KnowledgeTree.UnlockResult unlock(GamePanel game, String branchId, String nodeId) {
        if (game == null) return new KnowledgeTree.UnlockResult(false, "No active character.", null, 0);
        // Rebuild from authoritative character state; never spend a stale menu's balance.
        KnowledgeTree tree = KnowledgeBranchDefinitions.createBranches(game.knowledgeCredits, game.unlockedKnowledges).get(branchId);
        if (tree == null) return new KnowledgeTree.UnlockResult(false, "Unknown knowledge branch.", null, game.knowledgeCredits);
        KnowledgeTree.UnlockResult result = tree.unlockNode(nodeId);
        if (result.success()) {
            game.knowledgeCredits = result.remainingPoints();
            game.unlockedKnowledges.add(result.node().id());
        }
        game.logEvent(result.message());
        return result;
    }
}
