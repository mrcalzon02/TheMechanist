package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side player action gate for long actions and progress snapshots. */
final class PlayerActionRegistry {
    static final String VERSION = "player-action-registry-0.9.10gm";

    record ActiveAction(String actionName, int ticksRemaining, int totalTicks, WorldCommandRequest completionCommand, String source) {
        ActiveAction {
            actionName = actionName == null || actionName.isBlank() ? "long action" : actionName.trim();
            totalTicks = Math.max(1, totalTicks);
            ticksRemaining = Math.max(0, Math.min(ticksRemaining, totalTicks));
            source = source == null || source.isBlank() ? "server-action-registry" : source.trim();
        }
        double progress() { return totalTicks <= 0 ? 1.0 : (double)(totalTicks - ticksRemaining) / (double)totalTicks; }
        int percent() { return Math.max(0, Math.min(100, (int)Math.round(progress() * 100.0))); }
        String progressBar() {
            int filled = Math.max(0, Math.min(10, (int)Math.round(progress() * 10.0)));
            return "#".repeat(filled) + "-".repeat(10 - filled);
        }
        String compact() { return actionName + " [" + progressBar() + "] " + percent() + "% (" + ticksRemaining + "/" + totalTicks + " ticks left)"; }
        String clockGlyph() {
            String[] clocks = {"🕛","🕐","🕑","🕒","🕓","🕔","🕕","🕖","🕗","🕘","🕙","🕚"};
            if (totalTicks <= 1) return "🕛";
            int remainingBucket = (int)Math.floor(((double)Math.max(0, ticksRemaining) / (double)Math.max(1, totalTicks)) * 11.0);
            return clocks[Math.max(0, Math.min(clocks.length - 1, remainingBucket))];
        }
        String countdownOverlayText() { return clockGlyph() + " " + Math.max(0, ticksRemaining); }
        String completionSummary() { return completionCommand == null ? "none" : completionCommand.auditName(); }
    }

    record TickSummary(int activeBefore, int activeAfter, List<WorldCommandRequest> completedCommands, List<String> progressLines) {
        String compact() { return "activeBefore=" + activeBefore + " activeAfter=" + activeAfter + " completed=" + completedCommands.size() + " progressLines=" + progressLines.size(); }
    }

    private final Map<String, ActiveAction> processingPlayers = new ConcurrentHashMap<>();

    void assignLongAction(String playerId, String actionName, int durationInTicks, WorldCommandRequest completionCommand, String source) {
        String id = cleanPlayerId(playerId);
        ActiveAction action = new ActiveAction(actionName, durationInTicks, durationInTicks, completionCommand, source);
        processingPlayers.put(id, action);
        DebugLog.audit("PLAYER_ACTION_GATE", "assign player=" + id + " action=" + action.compact() + " completion=" + action.completionSummary());
    }

    void assignLongAction(String playerId, String actionName, int durationInTicks, WorldCommandRequest completionCommand) {
        assignLongAction(playerId, actionName, durationInTicks, completionCommand, "server-action-registry");
    }

    boolean cancelAction(String playerId, String reason) {
        String id = cleanPlayerId(playerId);
        ActiveAction removed = processingPlayers.remove(id);
        if (removed != null) DebugLog.audit("PLAYER_ACTION_CANCEL", "player=" + id + " action=" + removed.compact() + " reason=" + cleanReason(reason));
        return removed != null;
    }

    boolean isGated(String playerId) {
        return processingPlayers.containsKey(cleanPlayerId(playerId));
    }

    ActiveAction activeAction(String playerId) {
        return processingPlayers.get(cleanPlayerId(playerId));
    }

    String activeActionLine(String playerId) {
        ActiveAction action = activeAction(playerId);
        return action == null ? "" : action.compact();
    }

    String activeActionCountdownOverlay(String playerId) {
        ActiveAction action = activeAction(playerId);
        return action == null ? "" : action.countdownOverlayText();
    }

    int activeActionTicksRemaining(String playerId) {
        ActiveAction action = activeAction(playerId);
        return action == null ? 0 : action.ticksRemaining();
    }

    TickSummary tickWorldActions(String worldId) {
        int before = processingPlayers.size();
        ArrayList<WorldCommandRequest> completed = new ArrayList<>();
        ArrayList<String> progress = new ArrayList<>();
        for (Map.Entry<String, ActiveAction> entry : processingPlayers.entrySet()) {
            String playerId = entry.getKey();
            ActiveAction action = entry.getValue();
            int next = action.ticksRemaining() - 1;
            if (next <= 0) {
                if (processingPlayers.remove(playerId, action)) {
                    if (action.completionCommand() != null) completed.add(action.completionCommand());
                    String done = action.actionName() + " complete";
                    progress.add(done);
                    DebugLog.audit("PLAYER_ACTION_COMPLETE", "world=" + cleanWorldId(worldId) + " player=" + playerId + " action=" + action.actionName() + " completion=" + action.completionSummary());
                }
            } else {
                ActiveAction updated = new ActiveAction(action.actionName(), next, action.totalTicks(), action.completionCommand(), action.source());
                if (processingPlayers.replace(playerId, action, updated)) {
                    progress.add(updated.compact());
                    DebugLog.audit("PLAYER_ACTION_PROGRESS", "world=" + cleanWorldId(worldId) + " player=" + playerId + " action=" + updated.compact());
                }
            }
        }
        return new TickSummary(before, processingPlayers.size(), List.copyOf(completed), List.copyOf(progress));
    }

    String statusLine(String playerId) {
        ActiveAction action = activeAction(playerId);
        return "authority=" + VERSION + " gatedPlayers=" + processingPlayers.size()
                + " local=" + (action == null ? "none" : action.compact())
                + " completion=" + (action == null ? "none" : action.completionSummary());
    }

    static String auditSummary() {
        return "authority=" + VERSION + " rule=long-actions-gate-player-input-and-report-progress completion=named-world-command-request-not-arbitrary-runnable";
    }

    private static String cleanPlayerId(String playerId) {
        String id = playerId == null ? "player" : playerId.trim();
        return id.isEmpty() ? "player" : id;
    }

    private static String cleanWorldId(String worldId) {
        String id = worldId == null ? "local-world" : worldId.trim();
        return id.isEmpty() ? "local-world" : id;
    }

    private static String cleanReason(String reason) {
        String r = reason == null ? "unspecified" : reason.trim();
        return r.isEmpty() ? "unspecified" : r.replace('\n', ' ');
    }
}
