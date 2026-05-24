package mechanist;

/**
 * Server-side command request envelope for player/admin actions that still resolve
 * through legacy GamePanel methods while the authoritative runtime is being split out.
 */
sealed interface WorldCommandRequest permits MovePlayerCommand, WaitCommand, ConfirmInteractionCommand, ConfirmCombatCommand, UseInventoryCommand, UnequipEquipmentCommand, ChangeZoneCommand, AdminAddMoneyCommand, AdminAdvanceTurnCommand, AdminTeleportCommand, AdminSpawnItemCommand {
    String playerId();
    String reason();
    void apply(GamePanel game);

    default boolean requiresUngatedPlayer() { return true; }
    default boolean requiresAdminAuthority() { return false; }

    default String auditName() {
        return getClass().getSimpleName() + " player=" + playerId() + " reason=" + reason();
    }

    default String rejectionReason(GamePanel game) {
        if (playerId() == null || playerId().isBlank()) return "command missing player id";
        if (game == null) return "no game panel bound to authoritative command";
        return "";
    }
}

record MovePlayerCommand(String playerId, int dx, int dy, String source) implements WorldCommandRequest {
    public String reason() { return "command move " + dx + "," + dy + " source=" + (source == null ? "input" : source); }
    public String rejectionReason(GamePanel game) {
        String base = WorldCommandRequest.super.rejectionReason(game);
        if (!base.isBlank()) return base;
        if (dx < -1 || dx > 1 || dy < -1 || dy > 1 || (dx == 0 && dy == 0)) return "invalid movement vector";
        return "";
    }
    public void apply(GamePanel game) { if (game != null) game.executePacedMovementBody(dx, dy, source == null ? "server-command" : source); }
}

record WaitCommand(String playerId) implements WorldCommandRequest {
    public String reason() { return "command wait"; }
    public void apply(GamePanel game) { if (game != null) { game.clearPendingMovementInput("wait-command"); game.advanceTurnBody("waits. The underhive does not become kinder."); game.settlePlayerMotionAfterNoMoveTurn("wait-command"); } }
}

record ConfirmInteractionCommand(String playerId, int x, int y) implements WorldCommandRequest {
    public String reason() { return "command interact " + x + "," + y; }
    public String rejectionReason(GamePanel game) {
        String base = WorldCommandRequest.super.rejectionReason(game);
        if (!base.isBlank()) return base;
        if (game.world == null || !game.world.inBounds(x, y)) return "invalid interaction target";
        return "";
    }
    public void apply(GamePanel game) { if (game != null) game.confirmInteractionBody(); }
}

record ConfirmCombatCommand(String playerId, int x, int y) implements WorldCommandRequest {
    public String reason() { return "command combat " + x + "," + y; }
    public String rejectionReason(GamePanel game) {
        String base = WorldCommandRequest.super.rejectionReason(game);
        if (!base.isBlank()) return base;
        if (game.world == null || !game.world.inBounds(x, y)) return "invalid combat target";
        return "";
    }
    public void apply(GamePanel game) { if (game != null) game.confirmCombatTargetBody(); }
}

record UseInventoryCommand(String playerId, String itemName) implements WorldCommandRequest {
    public String reason() { return "command use-inventory " + (itemName == null ? "selected" : itemName); }
    public void apply(GamePanel game) { if (game != null) game.useSelectedInventoryItemBody(); }
}

record UnequipEquipmentCommand(String playerId, int slotIndex) implements WorldCommandRequest {
    public String reason() { return "command unequip slot=" + slotIndex; }
    public String rejectionReason(GamePanel game) {
        String base = WorldCommandRequest.super.rejectionReason(game);
        if (!base.isBlank()) return base;
        if (slotIndex < 0) return "invalid equipment slot";
        return "";
    }
    public void apply(GamePanel game) { if (game != null) game.unequipSelectedEquipmentSlotBody(); }
}

record ChangeZoneCommand(String playerId, String transitionName) implements WorldCommandRequest {
    public String reason() { return "command change-zone " + (transitionName == null ? "selected" : transitionName); }
    public void apply(GamePanel game) { if (game != null) game.confirmInteractionBody(); }
}

record AdminAddMoneyCommand(String playerId, int amount) implements WorldCommandRequest {
    public String reason() { return "admin-command add-money amount=" + amount; }
    public boolean requiresAdminAuthority() { return true; }
    public boolean requiresUngatedPlayer() { return false; }
    public String rejectionReason(GamePanel game) {
        String base = WorldCommandRequest.super.rejectionReason(game);
        if (!base.isBlank()) return base;
        if (amount <= 0) return "invalid script amount";
        return "";
    }
    public void apply(GamePanel game) {
        if (game == null) return;
        game.addImperialScript(Math.max(0, amount));
        game.logEvent("ADMIN: credited " + amount + " Imperial Script through the local server authority.");
    }
}

record AdminAdvanceTurnCommand(String playerId, int count) implements WorldCommandRequest {
    public String reason() { return "admin-command advance-turn count=" + count; }
    public boolean requiresAdminAuthority() { return true; }
    public boolean requiresUngatedPlayer() { return false; }
    public String rejectionReason(GamePanel game) {
        String base = WorldCommandRequest.super.rejectionReason(game);
        if (!base.isBlank()) return base;
        if (count <= 0) return "invalid turn count";
        return "";
    }
    public void apply(GamePanel game) {
        if (game == null) return;
        int safeCount = Math.max(1, Math.min(200, count));
        for (int i = 0; i < safeCount; i++) game.advanceTurnBody(i == 0 ? "is advanced by server authority." : "continues server-authority time advancement.");
    }
}

record AdminTeleportCommand(String playerId, int x, int y) implements WorldCommandRequest {
    public String reason() { return "admin-command teleport " + x + "," + y; }
    public boolean requiresAdminAuthority() { return true; }
    public boolean requiresUngatedPlayer() { return false; }
    public String rejectionReason(GamePanel game) {
        String base = WorldCommandRequest.super.rejectionReason(game);
        if (!base.isBlank()) return base;
        if (game.world == null || !game.world.inBounds(x, y)) return "invalid teleport target";
        if (!game.world.walkable(x, y)) return "teleport target is not walkable";
        return "";
    }
    public void apply(GamePanel game) {
        if (game == null) return;
        game.playerX = x;
        game.playerY = y;
        game.lookX = x;
        game.lookY = y;
        game.clearPendingMovementInput("admin-teleport");
        game.markLocalDirtyRegion("admin teleport", x, y, Math.max(6, game.visionRange() + 2), true, true, true, false);
        game.updateSensoryModel("admin teleport");
        game.logEvent("ADMIN: relocated through the local server authority to " + x + "," + y + ".");
    }
}

record AdminSpawnItemCommand(String playerId, String itemName, int count) implements WorldCommandRequest {
    public String reason() { return "admin-command spawn-item " + cleanItem(itemName) + " x" + count; }
    public boolean requiresAdminAuthority() { return true; }
    public boolean requiresUngatedPlayer() { return false; }
    public String rejectionReason(GamePanel game) {
        String base = WorldCommandRequest.super.rejectionReason(game);
        if (!base.isBlank()) return base;
        if (cleanItem(itemName).isBlank()) return "missing item name";
        if (count <= 0) return "invalid item count";
        return "";
    }
    public void apply(GamePanel game) {
        if (game == null) return;
        int safeCount = Math.max(1, Math.min(200, count));
        String item = cleanItem(itemName);
        for (int i = 0; i < safeCount; i++) game.inventory.add(item);
        game.logEvent("ADMIN: issued " + safeCount + " x " + item + " through the local server authority.");
    }
    private static String cleanItem(String s) { return s == null ? "" : s.trim().replace('\n', ' '); }
}

record ConsoleCommandRequest(String playerId, String rawInput) {
    ConsoleCommandRequest {
        playerId = playerId == null || playerId.isBlank() ? SinglePlayerSectorRuntimeBridge.LOCAL_PLAYER_ID : playerId.trim();
        rawInput = ChatRuntimeAuthority.ChatSecurity.sanitizeChatText(rawInput == null ? "" : rawInput).trim();
    }
    String auditName() { return "ConsoleCommandRequest player=" + playerId + " raw=" + ChatRuntimeAuthority.ChatSecurity.sanitizeChatText(rawInput); }
}
