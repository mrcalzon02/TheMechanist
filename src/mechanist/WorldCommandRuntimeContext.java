package mechanist;

/**
 * Narrow command-execution context for WorldCommandRequest.
 *
 * This is the first practical retarget away from direct GamePanel command
 * mutation.  The GamePanel adapter remains temporary; command records should
 * depend on this context surface rather than the panel monolith.
 */
interface WorldCommandRuntimeContext {
    boolean mounted();
    World world();
    boolean inBounds(int x, int y);
    boolean walkable(int x, int y);
    void movePlayer(int dx, int dy, String source);
    void waitOneTurn(String line);
    void settleAfterNoMove(String reason);
    void clearPendingMovement(String reason);
    void confirmInteraction();
    void confirmCombatTarget();
    void useSelectedInventoryItem();
    void unequipSelectedEquipmentSlot();
    void addImperialScript(int amount);
    void logEvent(String line);
    void advanceTurn(String line);
    void teleportPlayer(int x, int y, String reason);
    void spawnInventoryItem(String item, int count);
}

final class WorldCommandRuntimeContexts {
    private WorldCommandRuntimeContexts() {}

    static WorldCommandRuntimeContext fromGamePanel(GamePanel game) {
        return new GamePanelWorldCommandRuntimeContext(game);
    }
}

final class GamePanelWorldCommandRuntimeContext implements WorldCommandRuntimeContext {
    private final GamePanel game;

    GamePanelWorldCommandRuntimeContext(GamePanel game) {
        this.game = game;
    }

    public boolean mounted() { return game != null; }
    public World world() { return game == null ? null : game.world; }
    public boolean inBounds(int x, int y) { return game != null && game.world != null && game.world.inBounds(x, y); }
    public boolean walkable(int x, int y) { return game != null && game.world != null && game.world.walkable(x, y); }
    public void movePlayer(int dx, int dy, String source) { if (game != null) game.executePacedMovementBody(dx, dy, source); }
    public void waitOneTurn(String line) {
        if (game == null) return;
        game.clearPendingMovementInput("wait-command");
        game.advanceTurnBody(line);
        game.settlePlayerMotionAfterNoMoveTurn("wait-command");
    }
    public void settleAfterNoMove(String reason) { if (game != null) game.settlePlayerMotionAfterNoMoveTurn(reason); }
    public void clearPendingMovement(String reason) { if (game != null) game.clearPendingMovementInput(reason); }
    public void confirmInteraction() { if (game != null) game.confirmInteractionBody(); }
    public void confirmCombatTarget() { if (game != null) game.confirmCombatTargetBody(); }
    public void useSelectedInventoryItem() { if (game != null) game.useSelectedInventoryItemBody(); }
    public void unequipSelectedEquipmentSlot() { if (game != null) game.unequipSelectedEquipmentSlotBody(); }
    public void addImperialScript(int amount) { if (game != null) game.addImperialScript(amount); }
    public void logEvent(String line) { if (game != null) game.logEvent(line); }
    public void advanceTurn(String line) { if (game != null) game.advanceTurnBody(line); }
    public void teleportPlayer(int x, int y, String reason) {
        if (game == null) return;
        game.playerX = x;
        game.playerY = y;
        game.lookX = x;
        game.lookY = y;
        game.clearPendingMovementInput("admin-teleport");
        game.markLocalDirtyRegion(reason == null ? "admin teleport" : reason, x, y, Math.max(6, game.visionRange() + 2), true, true, true, false);
        game.updateSensoryModel("admin teleport");
    }
    public void spawnInventoryItem(String item, int count) {
        if (game == null || item == null || item.isBlank()) return;
        int safeCount = Math.max(1, Math.min(200, count));
        for (int i = 0; i < safeCount; i++) game.inventory.add(item);
    }
}
