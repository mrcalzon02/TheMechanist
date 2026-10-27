package mechanist;

/**
 * Narrow command-execution context for WorldCommandRequest.
 *
 * This is the first practical retarget away from direct GamePanel command
 * mutation.  The GamePanel adapter remains temporary; command records should
 * depend on this context surface rather than the panel monolith.
 */
interface WorldCommandRuntimeContext {
    default String cyberneticRejection(String action,String root,String item,String patientId) {return "Cybernetic procedures are unavailable in this runtime.";}
    default void cyberneticAction(String action,String root,String item,String patientId) {throw new UnsupportedOperationException("Cybernetic runtime unavailable");}
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
    default void confirmCombatTarget(int x,int y) { confirmCombatTarget(); }
    void useSelectedInventoryItem();
    void unequipSelectedEquipmentSlot();
    void addImperialScript(int amount);
    void logEvent(String line);
    void advanceTurn(String line);
    void teleportPlayer(int x, int y, String reason);
    void spawnInventoryItem(String item, int count);
    default String developerFacilityRejection(String action, java.util.List<String> args) { return "Developer facility is unavailable in this runtime."; }
    default void developerFacilityAction(String action, java.util.List<String> args) { throw new UnsupportedOperationException("Developer facility runtime is unavailable"); }
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

    public String cyberneticRejection(String action,String root,String item,String patientId) {NpcEntity patient=cyberneticPatient(patientId);if(patientId!=null && !patientId.isEmpty() && patient==null) return "The selected patient must be alive and within two tiles.";return CyberneticLimbAuthority.rejection(game,patient,action,root,item);}
    public void cyberneticAction(String action,String root,String item,String patientId) {if(cyberneticRejection(action,root,item,patientId).isEmpty()) {NpcEntity patient=cyberneticPatient(patientId);if(CyberneticLimbAuthority.execute(game,patient,action,root,item) && patient!=null) game.advanceTurn("Treats adjacent patient.");}}
    private NpcEntity cyberneticPatient(String id) {
        if(game==null || game.world==null || id==null || id.isEmpty()) return null;
        for(NpcEntity n:game.world.npcs) if(id.equals(n.id) && n.hp>0 && Math.abs(n.x-game.playerX)+Math.abs(n.y-game.playerY)<=2) return n;
        return null;
    }
    public boolean mounted() { return game != null; }
    public String developerFacilityRejection(String action, java.util.List<String> args) { return DeveloperFacilityAuthority.reject(game,action,args); }
    public void developerFacilityAction(String action, java.util.List<String> args) { DeveloperFacilityAuthority.execute(game,action,args); }
    public World world() { return game == null ? null : game.world; }
    public boolean inBounds(int x, int y) { return game != null && game.world != null && game.world.inBounds(x, y); }
    public boolean walkable(int x, int y) { return game != null && game.world != null && game.world.walkable(x, y); }
    public void movePlayer(int dx, int dy, String source) { if (game != null) MovementExecutionAuthority.executeStep(game, dx, dy, source, true); }
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
    public void confirmCombatTarget(int x,int y) { if(game!=null) {game.combatX=x;game.combatY=y;game.confirmCombatTargetBody();} }
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
