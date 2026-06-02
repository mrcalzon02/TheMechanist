package mechanist;

/**
 * Server-side command request envelope for player/admin actions.
 *
 * Commands now target WorldCommandRuntimeContext first.  The legacy GamePanel
 * overload remains only as an adapter seam while the authoritative runtime is
 * being split out.
 */
sealed interface WorldCommandRequest permits MovePlayerCommand, WaitCommand, ConfirmInteractionCommand, ConfirmCombatCommand, UseInventoryCommand, UnequipEquipmentCommand, ChangeZoneCommand, AdminAddMoneyCommand, AdminAdvanceTurnCommand, AdminTeleportCommand, AdminSpawnItemCommand {
    String playerId();
    String reason();
    void apply(WorldCommandRuntimeContext runtime);

    default void apply(GamePanel game) { apply(WorldCommandRuntimeContexts.fromGamePanel(game)); }

    default boolean requiresUngatedPlayer() { return true; }
    default boolean requiresAdminAuthority() { return false; }

    default String auditName() {
        return getClass().getSimpleName() + " player=" + playerId() + " reason=" + reason();
    }

    default String rejectionReason(WorldCommandRuntimeContext runtime) {
        if (playerId() == null || playerId().isBlank()) return "command missing player id";
        if (runtime == null || !runtime.mounted()) return "no world command runtime bound to authoritative command";
        return "";
    }

    default String rejectionReason(GamePanel game) { return rejectionReason(WorldCommandRuntimeContexts.fromGamePanel(game)); }
}

record MovePlayerCommand(String playerId, int dx, int dy, String source) implements WorldCommandRequest {
    public String reason() { return "command move " + dx + "," + dy + " source=" + (source == null ? "input" : source); }
    public String rejectionReason(WorldCommandRuntimeContext runtime) {
        String base = WorldCommandRequest.super.rejectionReason(runtime);
        if (!base.isBlank()) return base;
        if (dx < -1 || dx > 1 || dy < -1 || dy > 1 || (dx == 0 && dy == 0)) return "invalid movement vector";
        return "";
    }
    public void apply(WorldCommandRuntimeContext runtime) { if (runtime != null) runtime.movePlayer(dx, dy, source == null ? "server-command" : source); }
}

record WaitCommand(String playerId) implements WorldCommandRequest {
    public String reason() { return "command wait"; }
    public void apply(WorldCommandRuntimeContext runtime) { if (runtime != null) runtime.waitOneTurn("waits. The underhive does not become kinder."); }
}

record ConfirmInteractionCommand(String playerId, int x, int y) implements WorldCommandRequest {
    public String reason() { return "command interact " + x + "," + y; }
    public String rejectionReason(WorldCommandRuntimeContext runtime) {
        String base = WorldCommandRequest.super.rejectionReason(runtime);
        if (!base.isBlank()) return base;
        if (!runtime.inBounds(x, y)) return "invalid interaction target";
        return "";
    }
    public void apply(WorldCommandRuntimeContext runtime) { if (runtime != null) runtime.confirmInteraction(); }
}

record ConfirmCombatCommand(String playerId, int x, int y) implements WorldCommandRequest {
    public String reason() { return "command combat " + x + "," + y; }
    public String rejectionReason(WorldCommandRuntimeContext runtime) {
        String base = WorldCommandRequest.super.rejectionReason(runtime);
        if (!base.isBlank()) return base;
        if (!runtime.inBounds(x, y)) return "invalid combat target";
        return "";
    }
    public void apply(WorldCommandRuntimeContext runtime) { if (runtime != null) runtime.confirmCombatTarget(); }
}

record UseInventoryCommand(String playerId, String itemName) implements WorldCommandRequest {
    public String reason() { return "command use-inventory " + (itemName == null ? "selected" : itemName); }
    public void apply(WorldCommandRuntimeContext runtime) { if (runtime != null) runtime.useSelectedInventoryItem(); }
}

record UnequipEquipmentCommand(String playerId, int slotIndex) implements WorldCommandRequest {
    public String reason() { return "command unequip slot=" + slotIndex; }
    public String rejectionReason(WorldCommandRuntimeContext runtime) {
        String base = WorldCommandRequest.super.rejectionReason(runtime);
        if (!base.isBlank()) return base;
        if (slotIndex < 0) return "invalid equipment slot";
        return "";
    }
    public void apply(WorldCommandRuntimeContext runtime) { if (runtime != null) runtime.unequipSelectedEquipmentSlot(); }
}

record ChangeZoneCommand(String playerId, String transitionName) implements WorldCommandRequest {
    public String reason() { return "command change-zone " + (transitionName == null ? "selected" : transitionName); }
    public void apply(WorldCommandRuntimeContext runtime) { if (runtime != null) runtime.confirmInteraction(); }
}

record AdminAddMoneyCommand(String playerId, int amount) implements WorldCommandRequest {
    public String reason() { return "admin-command add-money amount=" + amount; }
    public boolean requiresAdminAuthority() { return true; }
    public boolean requiresUngatedPlayer() { return false; }
    public String rejectionReason(WorldCommandRuntimeContext runtime) {
        String base = WorldCommandRequest.super.rejectionReason(runtime);
        if (!base.isBlank()) return base;
        if (amount <= 0) return "invalid script amount";
        return "";
    }
    public void apply(WorldCommandRuntimeContext runtime) {
        if (runtime == null) return;
        runtime.addImperialScript(Math.max(0, amount));
        runtime.logEvent("ADMIN: credited " + amount + " Concord Script through the local server authority.");
    }
}

record AdminAdvanceTurnCommand(String playerId, int count) implements WorldCommandRequest {
    public String reason() { return "admin-command advance-turn count=" + count; }
    public boolean requiresAdminAuthority() { return true; }
    public boolean requiresUngatedPlayer() { return false; }
    public String rejectionReason(WorldCommandRuntimeContext runtime) {
        String base = WorldCommandRequest.super.rejectionReason(runtime);
        if (!base.isBlank()) return base;
        if (count <= 0) return "invalid turn count";
        return "";
    }
    public void apply(WorldCommandRuntimeContext runtime) {
        if (runtime == null) return;
        int safeCount = Math.max(1, Math.min(200, count));
        for (int i = 0; i < safeCount; i++) runtime.advanceTurn(i == 0 ? "is advanced by server authority." : "continues server-authority time advancement.");
    }
}

record AdminTeleportCommand(String playerId, int x, int y) implements WorldCommandRequest {
    public String reason() { return "admin-command teleport " + x + "," + y; }
    public boolean requiresAdminAuthority() { return true; }
    public boolean requiresUngatedPlayer() { return false; }
    public String rejectionReason(WorldCommandRuntimeContext runtime) {
        String base = WorldCommandRequest.super.rejectionReason(runtime);
        if (!base.isBlank()) return base;
        if (!runtime.inBounds(x, y)) return "invalid teleport target";
        if (!runtime.walkable(x, y)) return "teleport target is not walkable";
        return "";
    }
    public void apply(WorldCommandRuntimeContext runtime) {
        if (runtime == null) return;
        runtime.teleportPlayer(x, y, "admin teleport");
        runtime.logEvent("ADMIN: relocated through the local server authority to " + x + "," + y + ".");
    }
}

record AdminSpawnItemCommand(String playerId, String itemName, int count) implements WorldCommandRequest {
    public String reason() { return "admin-command spawn-item " + cleanItem(itemName) + " x" + count; }
    public boolean requiresAdminAuthority() { return true; }
    public boolean requiresUngatedPlayer() { return false; }
    public String rejectionReason(WorldCommandRuntimeContext runtime) {
        String base = WorldCommandRequest.super.rejectionReason(runtime);
        if (!base.isBlank()) return base;
        if (cleanItem(itemName).isBlank()) return "missing item name";
        if (count <= 0) return "invalid item count";
        return "";
    }
    public void apply(WorldCommandRuntimeContext runtime) {
        if (runtime == null) return;
        int safeCount = Math.max(1, Math.min(200, count));
        String item = cleanItem(itemName);
        runtime.spawnInventoryItem(item, safeCount);
        runtime.logEvent("ADMIN: issued " + safeCount + " x " + item + " through the local server authority.");
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
