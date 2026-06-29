package mechanist;

import java.util.Arrays;
import java.util.Locale;

/** Server-authoritative admin command dispatcher for the internal local server. */
final class AdminCommandDispatcher {
    static final String VERSION = "admin-command-dispatcher-0.9.10hp";

    interface MutationSink {
        AuthoritativeWorldSnapshot mutate(GamePanel game, String reason, Runnable body);
        AuthoritativeWorldSnapshot submitCommand(GamePanel game, WorldCommandRequest command);
        String statusLine();
    }

    private final WorldTurnManager turnManager;
    private final PlayerActionRegistry actionRegistry;
    private final MutationSink mutationSink;

    AdminCommandDispatcher(WorldTurnManager turnManager, PlayerActionRegistry actionRegistry, MutationSink mutationSink) {
        this.turnManager = turnManager;
        this.actionRegistry = actionRegistry;
        this.mutationSink = mutationSink;
    }

    String executeCommand(GamePanel game, InternalServerSessionAuthority.CommandContext context, ConsoleCommandRequest request) {
        String raw = request == null ? "" : request.rawInput().trim();
        if (raw.isEmpty()) return "No server command entered.";
        String[] parts = raw.split("\\s+");
        String root = parts[0].toLowerCase(Locale.ROOT);
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        if ("/server_status".equals(root)) return serverStatus(game, context);
        if ("/action_status".equals(root)) return actionStatus(context);
        if (context == null || !context.isAdmin()) {
            DebugLog.warn("ADMIN_COMMAND_REJECTED", "reason=no-admin-rights command=" + root + " request=" + (request == null ? "none" : request.auditName()));
            return "Access denied: authoritative host privileges are required.";
        }
        return switch (root) {
            case "/worldmode" -> handleWorldMode(context, args);
            case "/setrate" -> handleSetRate(context, args);
            case "/add_money" -> handleAddMoney(game, context, args);
            case "/advance_turn" -> handleAdvanceTurn(game, context, args);
            case "/spawn_item" -> handleSpawnItem(game, context, args);
            case "/teleport" -> handleTeleport(game, context, args);
            case "/long_action" -> handleLongAction(context, args);
            case "/long_move" -> handleLongMove(context, args);
            case "/clear_action" -> handleClearAction(context);
            case "/save_catalog", "/persistence_catalog" -> handleSaveCatalog(game);
            case "/save_itemized", "/persistence_itemized" -> handleSaveItemized(game);
            case "/save_architecture", "/persistence_architecture" -> handleSaveArchitecture(game);
            case "/faction_continuity", "/player_faction" -> handleFactionContinuity(game);
            case "/faction_autonomy" -> handleFactionAutonomy(game);
            case "/faction_tick", "/faction_autonomous_tick" -> handleFactionTick(game, args);
            case "/player_assignments" -> handlePlayerAssignments(game);
            case "/rank_parity", "/personnel_parity" -> handleRankParity(game);
            case "/ui_framework", "/management_ui" -> handleUiFramework();
            case "/qol_defaults", "/gameplay_defaults" -> handleGameplayDefaults(game);
            case "/blueprint_editor", "/construction_editor" -> handleBlueprintEditor(game);
            case "/construction_validation", "/blueprint_validation" -> handleConstructionValidation();
            case "/construction_progress", "/staged_construction" -> handleConstructionProgress(game);
            case "/project_evaluation", "/project_replan" -> handleProjectEvaluation();
            case "/gap_analysis" -> handleGapAnalysis();
            case "/replanned_goals" -> handleReplannedGoals();
            case "/order_of_operations", "/project_order" -> handleOrderOfOperations();
            case "/hidden_dependency" -> handleHiddenDependency();
            case "/forcekick" -> handleForceKick(args);
            default -> GameplayConsoleCommandAuthority.isKnown(root)
                    ? GameplayConsoleCommandAuthority.execute(game, context, root, args)
                    : "Unknown authoritative instruction: " + root;
        };
    }

    private String handleWorldMode(InternalServerSessionAuthority.CommandContext context, String[] args) {
        if (args.length < 1) return "Usage: /worldmode <strict|continuous>";
        WorldTurnManager.PlayMode mode = switch (args[0].toLowerCase(Locale.ROOT)) {
            case "strict" -> WorldTurnManager.PlayMode.STRICT_TURN_BASED;
            case "continuous", "slow", "slow_continuous", "always", "always_ticking" -> WorldTurnManager.PlayMode.SLOW_CONTINUOUS;
            default -> null;
        };
        if (mode == null) return "Invalid mode. Use strict or continuous.";
        turnManager.setWorldMode(context.currentWorldId(), mode);
        return "World mode set to " + mode + " for " + context.currentWorldId() + ".";
    }

    private String handleSetRate(InternalServerSessionAuthority.CommandContext context, String[] args) {
        if (args.length < 1) return "Usage: /setrate <milliseconds>";
        try {
            long rateMs = Long.parseLong(args[0]);
            turnManager.setTickRate(context.currentWorldId(), rateMs);
            return "Continuous tick rate set to " + turnManager.tickRateMs(context.currentWorldId()) + "ms.";
        } catch (NumberFormatException nfe) {
            return "Tick rate must be an integer millisecond count.";
        }
    }

    private String handleAddMoney(GamePanel game, InternalServerSessionAuthority.CommandContext context, String[] args) {
        if (args.length < 1) return "Usage: /add_money <amount>";
        try {
            int amount = Math.max(0, Math.min(1_000_000, Integer.parseInt(args[0])));
            if (amount <= 0) return "Amount must be positive.";
            AuthoritativeWorldSnapshot snapshot = mutationSink.submitCommand(game, new AdminAddMoneyCommand(context.playerId(), amount));
            return "Credited " + amount + " Imperial Script. " + compact(snapshot);
        } catch (NumberFormatException nfe) {
            return "Amount must be an integer.";
        }
    }

    private String handleAdvanceTurn(GamePanel game, InternalServerSessionAuthority.CommandContext context, String[] args) {
        int count = 1;
        if (args.length >= 1) {
            try { count = Integer.parseInt(args[0]); }
            catch (NumberFormatException nfe) { return "Turn count must be an integer."; }
        }
        int safeCount = Math.max(1, Math.min(200, count));
        AuthoritativeWorldSnapshot snapshot = mutationSink.submitCommand(game, new AdminAdvanceTurnCommand(context.playerId(), safeCount));
        return "Advanced " + safeCount + " turn(s). " + compact(snapshot);
    }

    private String handleSpawnItem(GamePanel game, InternalServerSessionAuthority.CommandContext context, String[] args) {
        if (args.length < 1) return "Usage: /spawn_item <item name> [count]";
        int count = 1;
        String[] nameParts = args;
        String last = args[args.length - 1];
        if (args.length > 1) {
            try {
                count = Integer.parseInt(last);
                nameParts = Arrays.copyOf(args, args.length - 1);
            } catch (NumberFormatException ignored) {
                count = 1;
            }
        }
        count = Math.max(1, Math.min(200, count));
        String name = String.join(" ", nameParts).trim();
        if (name.isBlank()) return "Usage: /spawn_item <item name> [count]";
        AuthoritativeWorldSnapshot snapshot = mutationSink.submitCommand(game, new AdminSpawnItemCommand(context.playerId(), name, count));
        return "Issued " + count + " x " + name + ". " + compact(snapshot);
    }

    private String handleTeleport(GamePanel game, InternalServerSessionAuthority.CommandContext context, String[] args) {
        if (args.length < 2) return "Usage: /teleport <x> <y>";
        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            AuthoritativeWorldSnapshot snapshot = mutationSink.submitCommand(game, new AdminTeleportCommand(context.playerId(), x, y));
            return "Teleport request resolved. " + compact(snapshot);
        } catch (NumberFormatException nfe) {
            return "Teleport coordinates must be integers.";
        }
    }

    private String handleLongAction(InternalServerSessionAuthority.CommandContext context, String[] args) {
        if (args.length < 1) return "Usage: /long_action <ticks> [action name]";
        try {
            int ticks = Math.max(1, Math.min(200, Integer.parseInt(args[0])));
            String name = args.length <= 1 ? "server-gated action" : String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            actionRegistry.assignLongAction(context.playerId(), name, ticks, null, "admin /long_action");
            return "Queued long action: " + actionRegistry.activeActionLine(context.playerId()) + ".";
        } catch (NumberFormatException nfe) {
            return "Long action ticks must be an integer.";
        }
    }

    private String handleLongMove(InternalServerSessionAuthority.CommandContext context, String[] args) {
        if (args.length < 3) return "Usage: /long_move <ticks> <dx> <dy>";
        try {
            int ticks = Math.max(1, Math.min(200, Integer.parseInt(args[0])));
            int dx = Math.max(-1, Math.min(1, Integer.parseInt(args[1])));
            int dy = Math.max(-1, Math.min(1, Integer.parseInt(args[2])));
            if (dx == 0 && dy == 0) return "Long move requires a non-zero direction.";
            WorldCommandRequest completion = new MovePlayerCommand(context.playerId(), dx, dy, "long-action-completion");
            actionRegistry.assignLongAction(context.playerId(), "committed movement " + dx + "," + dy, ticks, completion, "admin /long_move");
            return "Queued long move: " + actionRegistry.activeActionLine(context.playerId()) + ".";
        } catch (NumberFormatException nfe) {
            return "Long move ticks and direction values must be integers.";
        }
    }

    private String handleClearAction(InternalServerSessionAuthority.CommandContext context) {
        boolean removed = actionRegistry.cancelAction(context.playerId(), "admin /clear_action");
        return removed ? "Cleared the active server-gated action." : "No server-gated action is active.";
    }

    private String actionStatus(InternalServerSessionAuthority.CommandContext context) {
        String player = context == null ? SinglePlayerSectorRuntimeBridge.LOCAL_PLAYER_ID : context.playerId();
        return "Action status: " + actionRegistry.statusLine(player);
    }

    private String handleForceKick(String[] args) {
        if (args.length < 1) return "Usage: /forcekick <player_id>";
        DebugLog.audit("ADMIN_FORCEKICK_LOCAL", "target=" + args[0] + " result=no-remote-session-mounted");
        return "No remote session ejected; only the local single-player session is mounted.";
    }

    private String handleSaveCatalog(GamePanel game) {
        if (game == null || game.world == null || game.atlas == null) return "Save catalog unavailable until a run is active.";
        String line = SaveEfficiencyAuthority.compactRuntimeCatalog(game);
        DebugLog.audit("ADMIN_SAVE_CATALOG", line);
        return "Save catalog: " + line;
    }

    private String handleSaveItemized(GamePanel game) {
        if (game == null || game.world == null || game.atlas == null) return "Save itemization unavailable until a run is active.";
        String line = SaveEfficiencyAuthority.itemizedRuntimeCatalog(game);
        DebugLog.audit("ADMIN_SAVE_ITEMIZED", line.replace('\n', ' '));
        return line;
    }

    private String handleSaveArchitecture(GamePanel game) {
        if (game == null || game.world == null || game.atlas == null) return "Save architecture unavailable until a run is active.";
        String line = SaveEfficiencyAuthority.architectureSummary(game);
        DebugLog.audit("ADMIN_SAVE_ARCHITECTURE", line.replace('\n', ' '));
        return line;
    }

    private String handleFactionContinuity(GamePanel game) {
        if (game == null || game.world == null || game.atlas == null) return "Faction continuity unavailable until a run is active.";
        String line = PlayerFactionWorldAuthority.managementSummary(game);
        DebugLog.audit("ADMIN_FACTION_CONTINUITY", line);
        return line;
    }

    private String handleFactionAutonomy(GamePanel game) {
        if (game == null || game.world == null || game.atlas == null) return "Faction autonomy unavailable until a run is active.";
        String line = PlayerFactionAutonomyAuthority.summary(game);
        DebugLog.audit("ADMIN_FACTION_AUTONOMY", line);
        return line;
    }

    private String handleFactionTick(GamePanel game, String[] args) {
        if (game == null || game.world == null || game.atlas == null) return "Faction autonomous tick unavailable until a run is active.";
        int turns = 1;
        if (args.length >= 1) {
            try { turns = Integer.parseInt(args[0]); }
            catch (NumberFormatException nfe) { return "Faction tick turn count must be an integer."; }
        }
        String line = PlayerFactionAutonomousTickAuthority.previewTick(game, turns);
        DebugLog.audit("ADMIN_FACTION_AUTONOMOUS_TICK", line);
        return line;
    }

    private String handlePlayerAssignments(GamePanel game) {
        if (game == null || game.world == null || game.atlas == null) return "Player assignments unavailable until a run is active.";
        String line = PlayerFactionAutonomyAuthority.assignmentSummary(game);
        DebugLog.audit("ADMIN_PLAYER_ASSIGNMENTS", line);
        return line;
    }

    private String handleRankParity(GamePanel game) {
        if (game == null || game.world == null || game.atlas == null) return "Rank parity unavailable until a run is active.";
        String line = PlayerNpcCommandParityAuthority.assignmentParitySummary(game);
        DebugLog.audit("ADMIN_RANK_PARITY", line.replace('\n', ' '));
        return line;
    }

    private String handleUiFramework() {
        String line = UniversalManagementWindowAuthority.itemizedSummary();
        DebugLog.audit("ADMIN_UI_FRAMEWORK", line.replace('\n', ' '));
        return line;
    }

    private String handleGameplayDefaults(GamePanel game) {
        GameOptions options = game == null ? null : game.options;
        String line = GameplayQualityOfLifeAuthority.auditSummary(options);
        DebugLog.audit("ADMIN_GAMEPLAY_QOL", line);
        return line;
    }

    private String handleBlueprintEditor(GamePanel game) {
        GameOptions options = game == null ? null : game.options;
        String line = BlueprintConstructionAuthority.auditSummary() + " | " + String.join(" | ", BlueprintConstructionAuthority.optionLines(options));
        DebugLog.audit("ADMIN_BLUEPRINT_EDITOR", line);
        return line;
    }

    private String handleConstructionValidation() {
        BlueprintConstructionAuthority.RoomBlueprint bp = BlueprintConstructionAuthority.hollowBox("admin-test-room", "Admin Test Room", 5, 4, true);
        java.util.ArrayList<BlueprintConstructionAuthority.TargetTile> target = new java.util.ArrayList<>();
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 5; x++) {
                boolean occupied = x == 2 && y == 2;
                target.add(new BlueprintConstructionAuthority.TargetTile(10 + x, 20 + y, occupied, false, false, false, true, occupied ? "existing-crate" : "clear-floor"));
            }
        }
        BlueprintConstructionAuthority.ValidationResult result = BlueprintConstructionAuthority.preflight(bp, 10, 20, target, java.util.Map.of("Construction supplies", 99, "Scrap plate", 99, "Rivet set", 99, "Bearing set", 1));
        String line = result.summary() + " issues=" + result.issues();
        DebugLog.audit("ADMIN_CONSTRUCTION_VALIDATION", line);
        return line;
    }

    private String handleConstructionProgress(GamePanel game) {
        String line = ProgressiveConstructionAuthority.statusPacket(game);
        DebugLog.audit("ADMIN_CONSTRUCTION_PROGRESS", line);
        return line;
    }

    private String handleProjectEvaluation() {
        String line = ProjectReevaluationAuthority.fullReport();
        DebugLog.audit("ADMIN_PROJECT_EVALUATION", line.replace('\n', ' '));
        return line;
    }

    private String handleGapAnalysis() {
        String line = ProjectReevaluationAuthority.gapAnalysis();
        DebugLog.audit("ADMIN_GAP_ANALYSIS", line.replace('\n', ' '));
        return line;
    }

    private String handleReplannedGoals() {
        String line = ProjectReevaluationAuthority.rewrittenGoals();
        DebugLog.audit("ADMIN_REPLANNED_GOALS", line.replace('\n', ' '));
        return line;
    }

    private String handleOrderOfOperations() {
        String line = ProjectReevaluationAuthority.orderOfOperations();
        DebugLog.audit("ADMIN_ORDER_OF_OPERATIONS", line.replace('\n', ' '));
        return line;
    }

    private String handleHiddenDependency() {
        String line = ProjectReevaluationAuthority.hiddenDependency();
        DebugLog.audit("ADMIN_HIDDEN_DEPENDENCY", line);
        return line;
    }

    private String serverStatus(GamePanel game, InternalServerSessionAuthority.CommandContext context) {
        String worldId = context == null ? "local-world" : context.currentWorldId();
        return "Server status: "
                + (mutationSink == null ? "mutationSink=none" : mutationSink.statusLine())
                + " | " + (turnManager == null ? "turnManager=none" : turnManager.statusLine(worldId))
                + " | " + (actionRegistry == null ? "actionRegistry=none" : actionRegistry.statusLine(context == null ? SinglePlayerSectorRuntimeBridge.LOCAL_PLAYER_ID : context.playerId()))
                + " | adminDispatcher=" + VERSION;
    }

    private static String compact(AuthoritativeWorldSnapshot snapshot) {
        return snapshot == null ? "No authoritative snapshot was published." : snapshot.compact();
    }

    static String auditSummary() {
        return "authority=" + VERSION + " commands=/server_status,/action_status,/worldmode,/setrate,/add_money,/advance_turn,/spawn_item,/teleport,/long_action,/long_move,/clear_action,/save_catalog,/save_itemized,/save_architecture,/faction_continuity,/faction_autonomy,/faction_tick,/player_assignments,/rank_parity,/personnel_parity,/ui_framework,/management_ui,/qol_defaults,/gameplay_defaults,/project_evaluation,/gap_analysis,/replanned_goals,/order_of_operations,/hidden_dependency,/blueprint_editor,/construction_validation,/construction_progress,/forcekick + " + GameplayConsoleCommandAuthority.auditSummary() + " admin=internal-server-owner console=ConsoleCommandRequest mutations=named-WorldCommandRequest";
    }
}
