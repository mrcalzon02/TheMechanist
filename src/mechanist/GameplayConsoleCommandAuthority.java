package mechanist;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Strict registry and implementation bridge for gameplay/debug/admin console commands. */
final class GameplayConsoleCommandAuthority {
    static final String VERSION = "gameplay-console-command-authority-0.9.10jt";
    private static final ArrayDeque<String> HISTORY = new ArrayDeque<>();
    private static final int HISTORY_MAX = 40;
    private static final Map<String, CommandSpec> SPECS = new LinkedHashMap<>();
    static {
        add("help", 1, "help [command]", "Displays command usage.");
        add("clear", 1, "clear", "Clears the developer console history buffer.");
        add("history", 1, "history", "Lists recently executed console commands.");
        add("quit", 1, "quit", "Safely closes the executable.");
        add("fps", 1, "fps", "Toggles the diagnostics/FPS overlay.");
        add("god", 3, "god", "Toggles local invulnerability flag.");
        add("noclip", 3, "noclip", "Toggles local collision bypass flag.");
        add("vision", 3, "vision", "Toggles full vision ignoring lighting.");
        add("knowledge_status", 3, "knowledge_status", "Reports knowledge credits and unlocked doctrine count.");
        add("knowledge_add_credits", 3, "knowledge_add_credits <amount>", "Adds debug knowledge credits to the local character.");
        add("knowledge_set_credits", 3, "knowledge_set_credits <amount>", "Sets debug knowledge credits on the local character.");
        add("knowledge_unlock", 3, "knowledge_unlock <knowledge name>", "Force-unlocks a knowledge node/doctrine by name.");
        add("knowledge_lock", 3, "knowledge_lock <knowledge name>", "Force-locks/removes a knowledge node/doctrine by name.");
        add("knowledge_list", 3, "knowledge_list [filter]", "Lists known doctrines or matching doctrine names.");
        add("skill_status", 1, "skill_status", "Reports XP, unlocked skill nodes, and available capability nodes.");
        add("skill_unlock", 1, "skill_unlock <node id>", "Spends XP to unlock a skill capability node when prerequisites are met.");
        add("construction_status", 1, "construction_status", "Reports active staged construction sites, work target, next required work, and dismantle target guidance.");
        add("construction_progress", 1, "construction_progress", "Reports the same staged construction progress, work target, and dismantle target packet as construction_status.");
        add("construction_work", 1, "construction_work [turns 1-20]", "Stages available materials or adds labor to the adjacent staged site named by construction_progress, spends productive work turns, accepts 1-20 turns, and points to the nearest staged site when none are adjacent.");
        add("construction_dismantle", 1, "construction_dismantle", "Dismantles the least-complete adjacent unfinished staged site, spends one turn when a site is removed, recovers staged materials, and points to the nearest staged site when none are adjacent.");
        add("production_status", 1, "production_status", "Reports live production queue status, selected-machine readiness, and recent completion guidance.");
        add("production_history", 1, "production_history [count 1-5]", "Lists recent completed production records from shared machine-operation history, including saved result readbacks when present.");
        add("give", 3, "give <item_id> <amount>", "Adds an item/resource to inventory.");
        add("heal", 3, "heal <amount>", "Restores body endurance by amount.");
        add("kill", 3, "kill", "Defeats the local character.");
        add("tp", 3, "tp <x> <y> [z]", "Teleports to coordinates through authority lane.");
        add("speed", 3, "speed <multiplier>", "Sets local movement speed multiplier flag.");
        add("spawn", 3, "spawn <npc_or_object>", "Audits a spawn request in front/look coordinates.");
        add("settime", 3, "settime <time>", "Sets in-game clock text/value where possible.");
        add("timescale", 3, "timescale <multiplier>", "Sets local time-scale diagnostic flag.");
        add("weather", 3, "weather <type>", "Sets weather diagnostic state.");
        add("gravity", 3, "gravity <value>", "Sets gravity diagnostic state.");
        add("clear_dropped", 2, "clear_dropped", "Deletes nearby loose-item clutter within five tiles.");
        add("clear_dropped_all", 3, "clear_dropped_all", "Deletes all loose-item clutter.");
        add("show_hitboxes", 2, "show_hitboxes", "Toggles hitbox overlay flag.");
        add("show_wireframe", 2, "show_wireframe", "Toggles wireframe overlay flag.");
        add("show_navmesh", 2, "show_navmesh", "Toggles AI navmesh overlay flag.");
        add("show_navplans", 2, "show_navplans", "Toggles AI planned-path overlay flag.");
        add("show_npc_sense", 2, "show_npc_sense", "Toggles NPC sense range overlay flag.");
        add("stat_memory", 1, "stat_memory", "Reports JVM memory usage.");
        add("stat_drawcalls", 1, "stat_drawcalls", "Reports Java2D/render pass diagnostics.");
        add("lod_bias", 2, "lod_bias <value>", "Sets LOD bias diagnostic value.");
        add("mute_layer", 2, "mute_layer <audio_bus>", "Audits an audio bus mute request.");
        add("toggle_lighting", 2, "toggle_lighting", "Cycles visual lighting off/on.");
        add("toggle_fog", 2, "toggle_fog", "Toggles fog diagnostic flag.");
        add("post_process", 2, "post_process <0|1>", "Enables/disables post-process style effects where available.");
        add("ai_freeze", 3, "ai_freeze", "Toggles NPC AI freeze diagnostic flag.");
        add("ai_show_targets", 2, "ai_show_targets", "Toggles AI target-line overlay flag.");
        add("quest_start", 3, "quest_start <quest_id>", "Audits quest start request.");
        add("quest_complete", 3, "quest_complete <quest_id>", "Audits quest complete request.");
        add("clear_save", 3, "clear_save", "Requires external confirmation; not executed from gameplay console.");
        add("bind", 2, "bind <key> <command>", "Records a key bind request for later input-map authority.");
        add("exec", 3, "exec <filename.cfg>", "Runs a batch command file only when explicitly implemented by server policy.");
        add("server_save", 3, "server_save", "Writes active state to disk.");
        add("server_restart", 3, "server_restart <delay_seconds>", "Audits safe restart request.");
        add("server_shutdown", 3, "server_shutdown", "Requests shutdown through server authority.");
        add("status", 1, "status", "Returns server/runtime health.");
        add("broadcast", 2, "broadcast <message>", "Adds a center-screen/broadcast alert locally.");
        add("whisper", 1, "whisper <player> <message>", "Audits private message request.");
        add("perm_check", 1, "perm_check <player>", "Displays known local permission tier.");
        add("perm_set", 3, "perm_set <player> <rank_id>", "Audits permanent permission change request.");
        add("op", 3, "op <player>", "Audits administrator promotion request.");
        add("deop", 3, "deop <player>", "Audits administrator removal request.");
        add("kick", 2, "kick <player> <reason>", "Audits player kick request.");
        add("ban", 3, "ban <player/IP> <duration> <reason>", "Audits player/IP ban request.");
        add("unban", 3, "unban <player/IP>", "Audits unban request.");
        add("mute", 2, "mute <player> <duration>", "Audits mute request.");
        add("unmute", 2, "unmute <player>", "Audits unmute request.");
        add("freeze", 2, "freeze <player>", "Audits player freeze request.");
        add("spectate", 2, "spectate <player>", "Audits spectate request.");
        add("kill_player", 2, "kill_player <player>", "Audits remote player defeat request.");
        add("ban_steam64", 3, "ban_steam64 <steam64_id> <duration> <reason>", "Audits Steam64 identity ban request.");
        add("ban_gog", 3, "ban_gog <gog_id> <duration> <reason>", "Audits GOG identity ban request.");
        add("ban_hwid", 3, "ban_hwid <player_name/HWID> <duration> <reason>", "Audits HWID ban request.");
        add("ban_identity", 3, "ban_identity <player_name>", "Audits multi-identifier ban request.");
        add("player_fingerprint", 2, "player_fingerprint <player_name>", "Shows local/fallback identifiers when known.");
        add("alt_check", 2, "alt_check <player_name>", "Audits alternate-account check request.");
        add("alias_history", 2, "alias_history <player_name/ID>", "Audits alias-history request.");
        add("restrict_new_accounts", 3, "restrict_new_accounts <days>", "Audits account-age restriction.");
        add("vpn_block", 3, "vpn_block <on/off>", "Audits VPN block toggle.");
        add("family_share_block", 3, "family_share_block <on/off>", "Audits family-share block toggle.");
        add("net_drop", 3, "net_drop <player>", "Audits simulated disconnect.");
        add("whitelist", 3, "whitelist <on/off>", "Audits whitelist toggle.");
        add("whitelist_add", 3, "whitelist_add <player>", "Audits whitelist addition.");
        add("slomo", 3, "slomo <multiplier>", "Sets slow-motion diagnostic value.");
        add("map", 3, "map <map_name>", "Audits map/session change request.");
        add("team_shuffle", 3, "team_shuffle", "Audits team balance request.");
    }

    static boolean isKnown(String root) { return SPECS.containsKey(cleanRoot(root)); }

    static String execute(GamePanel game, InternalServerSessionAuthority.CommandContext context, String root, String[] args) {
        String command = cleanRoot(root);
        if (command.isEmpty()) return "No command entered.";
        if (!SPECS.containsKey(command)) return "Unknown gameplay command: " + command;
        recordHistory(command + (args == null || args.length == 0 ? "" : " " + String.join(" ", args)));
        int rank = context == null || context.isAdmin() ? 3 : 1;
        CommandSpec spec = SPECS.get(command);
        if (rank < spec.rank()) return "Access denied: " + command + " requires rank " + spec.rank() + ".";
        args = args == null ? new String[0] : args;
        try {
            return switch (command) {
                case "help" -> help(args);
                case "history" -> history();
                case "clear" -> "Console history cleared: " + clearHistory() + ".";
                case "quit" -> { if (game != null) game.requestApplicationExit("console /quit"); yield "Quit requested."; }
                case "status" -> game == null ? "Game panel unavailable." : game.stateSummary();
                case "fps" -> { if (game != null) game.togglePerformanceDiagnostics(); yield "FPS/diagnostics overlay toggled."; }
                case "god" -> toggle(game, "god");
                case "noclip" -> toggle(game, "noclip");
                case "vision" -> toggle(game, "vision");
                case "knowledge_status" -> knowledgeStatus(game);
                case "knowledge_add_credits" -> knowledgeAddCredits(game, args);
                case "knowledge_set_credits" -> knowledgeSetCredits(game, args);
                case "knowledge_unlock" -> knowledgeUnlock(game, args);
                case "knowledge_lock" -> knowledgeLock(game, args);
                case "knowledge_list" -> knowledgeList(game, args);
                case "skill_status" -> skillStatus(game);
                case "skill_unlock" -> skillUnlock(game, args);
                case "construction_status", "construction_progress" -> constructionStatus(game, command, args);
                case "construction_work" -> constructionWork(game, args);
                case "construction_dismantle" -> constructionDismantle(game, args);
                case "production_status" -> productionStatus(game, args);
                case "production_history" -> productionHistory(game, args);
                case "show_hitboxes" -> toggle(game, "hitboxes");
                case "show_wireframe" -> toggle(game, "wireframe");
                case "show_navmesh" -> toggle(game, "navmesh");
                case "show_navplans" -> toggle(game, "navplans");
                case "show_npc_sense" -> toggle(game, "npc_sense");
                case "ai_freeze" -> toggle(game, "ai_freeze");
                case "ai_show_targets" -> toggle(game, "ai_targets");
                case "toggle_fog" -> toggle(game, "fog");
                case "give" -> give(game, args);
                case "heal" -> heal(game, args);
                case "kill" -> kill(game);
                case "tp" -> teleport(game, context, args);
                case "speed" -> setFloatFlag(game, "speed", args, 0.1f, 10f);
                case "timescale" -> setFloatFlag(game, "timescale", args, 0f, 20f);
                case "slomo" -> setFloatFlag(game, "slomo", args, 0.05f, 5f);
                case "gravity" -> setFloatFlag(game, "gravity", args, 0f, 10f);
                case "settime" -> setStringFlag(game, "time", args);
                case "weather" -> setStringFlag(game, "weather", args);
                case "spawn" -> audit(game, command, args, "spawn request recorded at current/look coordinates");
                case "clear_dropped" -> clearDropped(game, false);
                case "clear_dropped_all" -> clearDropped(game, true);
                case "stat_memory" -> statMemory();
                case "stat_drawcalls" -> game == null ? "Render diagnostics unavailable." : "Render diagnostics: " + game.renderScaling.auditSummary() + " diagnostics=" + game.performanceDiagnostics.auditSummary();
                case "lod_bias" -> setFloatFlag(game, "lod_bias", args, -4f, 4f);
                case "mute_layer" -> audit(game, command, args, "audio bus mute request recorded");
                case "toggle_lighting" -> toggleLighting(game);
                case "post_process" -> postProcess(game, args);
                case "quest_start", "quest_complete", "bind", "exec", "server_restart", "server_shutdown", "broadcast", "whisper", "perm_set", "op", "deop", "kick", "ban", "unban", "mute", "unmute", "freeze", "spectate", "kill_player", "ban_steam64", "ban_gog", "ban_hwid", "ban_identity", "alt_check", "alias_history", "restrict_new_accounts", "vpn_block", "family_share_block", "net_drop", "whitelist", "whitelist_add", "map", "team_shuffle" -> audit(game, command, args, "server/network command is registered; remote enforcement remains gated until multiplayer transport is opened");
                case "perm_check" -> "Permission: local session rank=" + rank + " admin=" + (context != null && context.isAdmin()) + ".";
                case "player_fingerprint" -> fingerprint(game, args);
                case "server_save" -> serverSave(game);
                case "clear_save" -> "clear_save is registered but fail-closed from console; use the profile/save UI for destructive reset.";
                default -> help(new String[]{command});
            };
        } catch (Throwable t) {
            DebugLog.error("GAMEPLAY_CONSOLE_COMMAND", "Command failed root=" + command + " args=" + Arrays.toString(args), t);
            return "Command failed safely: " + command + ".";
        }
    }

    static String auditSummary() { return "authority=" + VERSION + " commands=" + SPECS.size() + " ranks=1-player,2-moderator,3-admin destructive=fail-closed-unless-wired"; }
    static String help(String[] args) {
        if (args != null && args.length > 0) {
            CommandSpec s = SPECS.get(cleanRoot(args[0]));
            return s == null ? "No usage for " + cleanRoot(args[0]) + "." : s.usage() + " — rank " + s.rank() + " — " + s.description();
        }
        return "Commands registered: " + String.join(", ", SPECS.keySet()) + ". Use help <command>.";
    }
    private static void add(String name, int rank, String usage, String description) { SPECS.put(cleanRoot(name), new CommandSpec(cleanRoot(name), rank, usage, description)); }
    private static String cleanRoot(String s) { return (s == null ? "" : s.trim().toLowerCase(Locale.ROOT).replaceFirst("^/+", "")); }
    private static void recordHistory(String line) { if (line == null || line.isBlank()) return; HISTORY.addLast(Instant.now() + " " + ChatRuntimeAuthority.ChatSecurity.sanitizeLogLine(line)); while (HISTORY.size() > HISTORY_MAX) HISTORY.removeFirst(); }
    private static int clearHistory() { int n = HISTORY.size(); HISTORY.clear(); return n; }
    private static String history() { return HISTORY.isEmpty() ? "No console history." : String.join(" | ", HISTORY); }
    private static String toggle(GamePanel game, String key) { if (game == null) return "Game unavailable."; boolean on = game.toggleConsoleFlag(key); return key + " " + (on ? "enabled" : "disabled") + "."; }
    private static String give(GamePanel game, String[] args) { if (game == null || game.inventory == null) return "Inventory unavailable."; if (args.length < 1) return "Usage: give <item_id> <amount>"; int n = parseInt(args, args.length - 1, 1); String[] names = args; if (args.length > 1 && isInt(args[args.length-1])) names = Arrays.copyOf(args, args.length-1); String item = ChatRuntimeAuthority.ChatSecurity.sanitizeChatText(String.join(" ", names)).trim(); n = Math.max(1, Math.min(200, n)); for (int i=0;i<n;i++) game.inventory.add(item); game.logEvent("CONSOLE: issued " + n + " x " + item + "."); return "Issued " + n + " x " + item + "."; }
    private static String knowledgeStatus(GamePanel game) { if (game == null) return "Game unavailable."; return "Knowledge: credits=" + game.knowledgeCredits + " unlocked=" + game.unlockedKnowledges.size() + " known=" + String.join(", ", game.unlockedKnowledges) + "."; }
    private static String knowledgeAddCredits(GamePanel game, String[] args) { if (game == null) return "Game unavailable."; if (args.length < 1) return "Usage: knowledge_add_credits <amount>"; int amount = parseInt(args, 0, 0); if (amount == 0) return "Amount must be a non-zero integer."; int old = game.knowledgeCredits; game.knowledgeCredits = Math.max(0, Math.min(1_000_000, old + amount)); refreshKnowledgeDebugState(game, "knowledge_add_credits " + amount); return "Knowledge credits " + old + "->" + game.knowledgeCredits + "."; }
    private static String knowledgeSetCredits(GamePanel game, String[] args) { if (game == null) return "Game unavailable."; if (args.length < 1) return "Usage: knowledge_set_credits <amount>"; int old = game.knowledgeCredits; game.knowledgeCredits = Math.max(0, Math.min(1_000_000, parseInt(args, 0, old))); refreshKnowledgeDebugState(game, "knowledge_set_credits"); return "Knowledge credits " + old + "->" + game.knowledgeCredits + "."; }
    private static String knowledgeUnlock(GamePanel game, String[] args) { if (game == null) return "Game unavailable."; String name = canonicalKnowledgeName(args); if (name.isBlank()) return "Usage: knowledge_unlock <knowledge name>"; boolean added = game.unlockedKnowledges.add(name); refreshKnowledgeDebugState(game, "knowledge_unlock " + name); return added ? "Knowledge unlocked: " + name + "." : "Knowledge already unlocked: " + name + "."; }
    private static String knowledgeLock(GamePanel game, String[] args) { if (game == null) return "Game unavailable."; String name = canonicalKnowledgeName(args); if (name.isBlank()) return "Usage: knowledge_lock <knowledge name>"; boolean removed = game.unlockedKnowledges.remove(name); refreshKnowledgeDebugState(game, "knowledge_lock " + name); return removed ? "Knowledge locked: " + name + "." : "Knowledge was not unlocked: " + name + "."; }
    private static String knowledgeList(GamePanel game, String[] args) { if (game == null) return "Game unavailable."; String filter = args == null || args.length == 0 ? "" : ChatRuntimeAuthority.ChatSecurity.sanitizeChatText(String.join(" ", args)).trim().toLowerCase(Locale.ROOT); java.util.ArrayList<String> lines = new java.util.ArrayList<>(); if (filter.isBlank()) { lines.add("Unlocked(" + game.unlockedKnowledges.size() + "): " + (game.unlockedKnowledges.isEmpty() ? "none" : String.join(", ", game.unlockedKnowledges))); lines.add("Credits: " + game.knowledgeCredits); } else { for (String name : KnowledgeDef.all().keySet()) if (name.toLowerCase(Locale.ROOT).contains(filter)) lines.add(name + (game.unlockedKnowledges.contains(name) ? " [unlocked]" : " [locked]")); if (lines.isEmpty()) lines.add("No matching knowledge definitions for filter: " + filter); } return String.join(" | ", lines); }
    private static String skillStatus(GamePanel game) { if (game == null) return "Game unavailable."; return String.join(" | ", SkillTreeProgressionAuthority.statusLines(game.xp, game.unlockedSkillNodes, SkillTreeProgressionAuthority.SkillAccessContext.fromGame(game), game.active == null ? java.util.Map.of() : game.active.stats)); }
    private static String skillUnlock(GamePanel game, String[] args) {
        if (game == null) return "Game unavailable.";
        if (args == null || args.length == 0) return "Usage: skill_unlock <node id>";
        String raw = ChatRuntimeAuthority.ChatSecurity.sanitizeChatText(String.join(" ", args)).trim();
        return game.unlockSkillNode(raw);
    }
    private static String constructionStatus(GamePanel game, String command, String[] args) {
        if (game == null) return "Construction status unavailable until a run is active.";
        if (args != null && args.length > 0) return "Usage: " + command;
        return ProgressiveConstructionAuthority.statusPacket(game);
    }
    private static String constructionWork(GamePanel game, String[] args) {
        if (game == null || game.baseObjects == null) return "Construction work unavailable until a run is active.";
        if (args != null && args.length > 1) return "Usage: construction_work [turns 1-20]";
        if (args != null && args.length == 1 && !isInt(args[0])) return "Construction work turns must be an integer.";
        int requestedTurns = parseInt(args, 0, 1);
        int turns = Math.max(1, Math.min(20, requestedTurns));
        String turnNotice = constructionWorkTurnNotice(args, requestedTurns, turns);
        BaseObject site = ProgressiveConstructionAuthority.workCommandTarget(game);
        if (site == null) return turnNotice + ProgressiveConstructionAuthority.workReachFailureLine(game);
        String beforeName = constructionEventName(site, "construction site");
        int laborBefore = Math.max(0, site.constructionLaborDone);
        boolean wasUnderConstruction = site.underConstruction;
        int inserted = ProgressiveConstructionAuthority.contribute(game, site, turns, true);
        int laborAdded = Math.max(0, site.constructionLaborDone - laborBefore);
        boolean completed = wasUnderConstruction && !site.underConstruction;
        int turnCost = constructionWorkTurnCost(game, turns, inserted, laborAdded);
        String result;
        if (completed) {
            result = ProgressiveConstructionAuthority.contributionResultLine(site, inserted, true);
        } else if (inserted <= 0 && laborAdded <= 0) {
            result = "Construction blocked. " + ProgressiveConstructionAuthority.siteStatusLine(game, site);
        } else {
            java.util.ArrayList<String> changes = new java.util.ArrayList<>();
            if (inserted > 0) changes.add("staged " + inserted + " material unit(s)");
            if (laborAdded > 0) changes.add("added " + laborAdded + " labor");
            result = "Construction work " + String.join(" and ", changes) + ". " + ProgressiveConstructionAuthority.siteStatusLine(game, site);
        }
        result = appendConstructionTimeSpent(turnNotice + result, turnCost);
        game.logEvent(result);
        advanceConstructionWorkTurns(game, site, beforeName, turnCost, completed);
        DebugLog.audit("GAMEPLAY_CONSTRUCTION_WORK", "turns=" + turns + " spent=" + turnCost + " inserted=" + inserted + " laborAdded=" + laborAdded + " completed=" + completed + " site=" + site.x + "," + site.y);
        game.repaint();
        return result;
    }
    private static String constructionWorkTurnNotice(String[] args, int requestedTurns, int turns) {
        if (args == null || args.length == 0 || requestedTurns == turns) return "";
        return "Construction work turns adjusted to " + turns + " (allowed 1-20). ";
    }
    private static int constructionWorkTurnCost(GamePanel game, int requestedTurns, int inserted, int laborAdded) {
        if (inserted <= 0 && laborAdded <= 0) return 0;
        if (laborAdded <= 0) return 1;
        int multiplier = Math.max(1, ProgressiveConstructionAuthority.toolLaborMultiplier(game));
        int productiveTurns = (laborAdded + multiplier - 1) / multiplier;
        return Math.max(1, Math.min(Math.max(1, requestedTurns), productiveTurns));
    }
    private static void advanceConstructionWorkTurns(GamePanel game, BaseObject site, String beforeName, int turnCost, boolean completed) {
        if (game == null || turnCost <= 0) return;
        String completedName = constructionEventName(site, "construction");
        String workingName = (beforeName == null || beforeName.isBlank()) ? completedName : beforeName;
        for (int i = 0; i < turnCost; i++) {
            boolean finalTurn = completed && i == turnCost - 1;
            game.advanceTurn(finalTurn ? "finishes " + completedName + "." : "works on " + workingName + ".");
        }
    }
    private static String appendConstructionTimeSpent(String result, int turnCost) {
        if (turnCost <= 0) return result;
        return result + " Construction time spent: " + turnCost + " " + (turnCost == 1 ? "turn" : "turns") + ".";
    }
    private static String constructionEventName(BaseObject site, String fallback) {
        if (site == null || site.name == null || site.name.isBlank()) return fallback;
        return site.name.trim();
    }
    private static String constructionDismantle(GamePanel game, String[] args) {
        if (game == null || game.baseObjects == null) return "Construction dismantle unavailable until a run is active.";
        if (args != null && args.length > 0) return "Usage: construction_dismantle";
        BaseObject site = ProgressiveConstructionAuthority.dismantleCommandTarget(game);
        if (site == null) return ProgressiveConstructionAuthority.dismantleReachFailureLine(game);
        String beforeName = constructionEventName(site, "construction site");
        ProgressiveConstructionAuthority.DismantleResult result = ProgressiveConstructionAuthority.dismantle(game, site);
        int turnCost = result.removed() ? 1 : 0;
        String summary = appendConstructionTimeSpent(result.summary(), turnCost);
        game.logEvent(summary);
        if (turnCost > 0) game.advanceTurn("dismantles " + beforeName + ".");
        DebugLog.audit("GAMEPLAY_CONSTRUCTION_DISMANTLE", "removed=" + result.removed()
                + " spent=" + turnCost
                + " supplies=" + result.recoveredSupplies()
                + " parts=" + result.recoveredMachineParts()
                + " named=" + result.recoveredNamedItems()
                + " site=" + site.x + "," + site.y);
        game.repaint();
        return summary;
    }
    private static String productionStatus(GamePanel game, String[] args) {
        if (game == null) return "Production status unavailable until a run is active.";
        if (args != null && args.length > 0) return "Usage: production_status";
        return String.join(" | ", MachineOperationStatusBridge.statusLines(game));
    }
    private static String productionHistory(GamePanel game, String[] args) {
        if (game == null || game.machineOperationQueue == null) return "Production history unavailable until a run is active.";
        if (args != null && args.length > 1) return "Usage: production_history [count 1-5]";
        if (args != null && args.length == 1 && !isInt(args[0])) return "Production history count must be an integer.";
        int limit = Math.max(1, Math.min(5, parseInt(args, 0, 3)));
        return String.join(" | ", MachineOperationStatusBridge.historyLines(game, limit));
    }
    private static String canonicalKnowledgeName(String[] args) { if (args == null || args.length == 0) return ""; String raw = ChatRuntimeAuthority.ChatSecurity.sanitizeChatText(String.join(" ", args)).trim(); if (raw.isBlank()) return ""; String normalized = raw.toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ').replaceAll("\\s+", " "); for (String name : KnowledgeDef.all().keySet()) { String n = name.toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ').replaceAll("\\s+", " "); if (n.equals(normalized)) return name; } return raw; }
    private static void refreshKnowledgeDebugState(GamePanel game, String reason) { if (game == null) return; game.logEvent("CONSOLE: " + reason + " -> knowledge credits=" + game.knowledgeCredits + " unlocked=" + game.unlockedKnowledges.size() + "."); DebugLog.audit("KNOWLEDGE_DEBUG_COMMAND", reason + " credits=" + game.knowledgeCredits + " unlocked=" + game.unlockedKnowledges.size() + " state=" + game.stateSummary()); if (game.screen == GamePanel.Screen.KNOWLEDGE) game.selectedKnowledgeNodeId = null; game.repaint(); }
    private static String heal(GamePanel game, String[] args) { if (game == null) return "Game unavailable."; int amount = Math.max(1, Math.min(200, parseInt(args, 0, 10))); game.healWorstBodyPart(amount); return "Restored body endurance by " + amount + " to the most damaged body part."; }
    private static String kill(GamePanel game) { if (game == null) return "Game unavailable."; game.triggerPlayerDeath("console kill", "server-authoritative console", "self-command", "self"); return "Local character defeated."; }
    private static String teleport(GamePanel game, InternalServerSessionAuthority.CommandContext context, String[] args) { if (game == null || args.length < 2) return "Usage: tp <x> <y> [z]"; int x = parseInt(args,0,game.playerX), y = parseInt(args,1,game.playerY); if (game.world == null || !game.world.inBounds(x,y)) return "Invalid target."; return game.singlePlayerSectorBridge.submitCommand(game, new AdminTeleportCommand(context == null ? SinglePlayerSectorRuntimeBridge.LOCAL_PLAYER_ID : context.playerId(), x, y)).compact(); }
    private static String setFloatFlag(GamePanel game, String key, String[] args, float min, float max) { if (game == null) return "Game unavailable."; if (args.length < 1) return "Usage: " + key + " <value>"; float value = Math.max(min, Math.min(max, parseFloat(args[0], 1f))); game.setConsoleNumericFlag(key, value); return key + " set to " + value + "."; }
    private static String setStringFlag(GamePanel game, String key, String[] args) { if (game == null) return "Game unavailable."; if (args.length < 1) return "Usage: " + key + " <value>"; String value = ChatRuntimeAuthority.ChatSecurity.sanitizeChatText(String.join(" ", args)); game.setConsoleStringFlag(key, value); return key + " set to " + value + "."; }
    private static String audit(GamePanel game, String command, String[] args, String note) { String line = command + " args=" + Arrays.toString(args) + " note=" + note; if (game != null) game.logEvent("CONSOLE: " + line); DebugLog.audit("GAMEPLAY_CONSOLE_COMMAND", line); return note + "."; }
    private static String clearDropped(GamePanel game, boolean all) { if (game == null || game.world == null) return "World unavailable."; int removed = 0; return "Loose dropped-item cleanup is registered, but this world build has no unified loose-item ground ledger to clear yet."; }
    private static String statMemory() { Runtime rt = Runtime.getRuntime(); long used=(rt.totalMemory()-rt.freeMemory())/1024/1024, total=rt.totalMemory()/1024/1024, max=rt.maxMemory()/1024/1024; return "Memory: used=" + used + "MB total=" + total + "MB max=" + max + "MB."; }
    private static String toggleLighting(GamePanel game) { if (game == null) return "Game unavailable."; game.options.lightingFxIndex = game.options.lightingFxIndex == 0 ? 2 : 0; game.options.save(); return "Lighting effects now " + game.options.lightingFxLabel() + "."; }
    private static String postProcess(GamePanel game, String[] args) { if (game == null || args.length < 1) return "Usage: post_process <0|1>"; game.options.lightingFxIndex = "0".equals(args[0]) ? 0 : 2; game.options.save(); return "Post-process style render effects now " + game.options.lightingFxLabel() + "."; }
    private static String fingerprint(GamePanel game, String[] args) { UserProfileAuthority.Profile p = game == null ? UserProfileAuthority.detect() : game.userProfile; return "Fingerprint: provider=" + p.provider + " display=" + p.displayName + " id=" + p.shortId() + " localOnly=true."; }
    private static String serverSave(GamePanel game) { if (game == null) return "Game unavailable."; try { game.writeSaveFile(1, true); return "Server/local save requested."; } catch (Exception e) { return "Save failed: " + e.getMessage(); } }
    private static int parseInt(String[] a, int i, int d) { try { return i>=0 && i<a.length ? Integer.parseInt(a[i]) : d; } catch(Exception e){ return d; } }
    private static float parseFloat(String s, float d) { try { return Float.parseFloat(s); } catch(Exception e){ return d; } }
    private static boolean isInt(String s) { try { Integer.parseInt(s); return true; } catch(Exception e){ return false; } }
    record CommandSpec(String name, int rank, String usage, String description) {}
    private GameplayConsoleCommandAuthority() {}
}
