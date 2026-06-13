package mechanist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Player-facing guidance model for active quest objectives without owning quest progression. */
final class QuestObjectiveGuidanceAuthority {
    static final String VERSION = "0.9.10kk";

    enum GuidanceKind { EXACT, APPROXIMATE, RUMORED, HIDDEN, UNSAFE, NEAREST_TRANSITION }

    record ObjectiveGuidance(String label, GuidanceKind kind, int targetX, int targetY,
                             boolean currentSlice, boolean targetVisible, String detail) {
        ObjectiveGuidance {
            label = PlayerFacingText.sanitize(label == null || label.isBlank() ? "Active objective" : label);
            kind = kind == null ? GuidanceKind.HIDDEN : kind;
            detail = PlayerFacingText.sanitize(detail);
        }
    }

    record GuidanceReadout(boolean showTargetMarker, boolean showDirection, boolean pulsing,
                           int targetX, int targetY, String summary) { }

    private QuestObjectiveGuidanceAuthority() { }

    static GuidanceReadout describe(ObjectiveGuidance objective, int playerX, int playerY, long nowMillis) {
        if (objective == null) return new GuidanceReadout(false, false, false, playerX, playerY, "No active objective guidance.");
        boolean exact = objective.kind() == GuidanceKind.EXACT || objective.kind() == GuidanceKind.UNSAFE;
        boolean marker = exact && objective.currentSlice() && objective.targetVisible();
        boolean direction = objective.kind() != GuidanceKind.HIDDEN && objective.kind() != GuidanceKind.RUMORED;
        boolean pulse = marker && ((nowMillis / 700L) & 1L) == 0L;
        String summary = switch (objective.kind()) {
            case EXACT -> objective.label() + ": exact target " + directionLabel(playerX, playerY, objective.targetX(), objective.targetY()) + ".";
            case APPROXIMATE -> objective.label() + ": search the indicated area; the exact target is uncertain.";
            case RUMORED -> objective.label() + ": rumored location only; no exact marker is available.";
            case HIDDEN -> objective.label() + ": the target remains hidden until more evidence is found.";
            case UNSAFE -> objective.label() + ": known target " + directionLabel(playerX, playerY, objective.targetX(), objective.targetY()) + "; approach is unsafe.";
            case NEAREST_TRANSITION -> objective.label() + ": follow the nearest known transition toward the objective zone.";
        };
        if (objective.detail() != null && !objective.detail().isBlank()) summary += " " + objective.detail();
        return new GuidanceReadout(marker, direction, pulse, objective.targetX(), objective.targetY(), PlayerFacingText.sanitize(summary));
    }

    static List<ObjectiveGuidance> orderedActive(List<ObjectiveGuidance> objectives, int playerX, int playerY) {
        ArrayList<ObjectiveGuidance> ordered = new ArrayList<>();
        if (objectives != null) for (ObjectiveGuidance objective : objectives) if (objective != null) ordered.add(objective);
        ordered.sort(Comparator
                .comparingInt((ObjectiveGuidance objective) -> priority(objective.kind()))
                .thenComparingLong(objective -> distanceSquared(playerX, playerY, objective.targetX(), objective.targetY()))
                .thenComparing(ObjectiveGuidance::label));
        return List.copyOf(ordered);
    }

    static String directionLabel(int fromX, int fromY, int toX, int toY) {
        int dx = Integer.compare(toX, fromX);
        int dy = Integer.compare(toY, fromY);
        if (dx == 0 && dy == 0) return "at your position";
        String vertical = dy < 0 ? "north" : dy > 0 ? "south" : "";
        String horizontal = dx < 0 ? "west" : dx > 0 ? "east" : "";
        return vertical + horizontal;
    }

    static String auditSummary() {
        return "questObjectiveGuidanceAuthority version=" + VERSION
                + " modes=exact+approximate+rumored+hidden+unsafe+nearestTransition"
                + " visuals=direction+slowPulse visibleTargetsOnly=true ownsQuestProgression=false";
    }

    private static int priority(GuidanceKind kind) {
        return switch (kind) {
            case UNSAFE -> 0;
            case EXACT -> 1;
            case NEAREST_TRANSITION -> 2;
            case APPROXIMATE -> 3;
            case RUMORED -> 4;
            case HIDDEN -> 5;
        };
    }

    private static long distanceSquared(int x1, int y1, int x2, int y2) {
        long dx = x2 - x1;
        long dy = y2 - y1;
        return dx * dx + dy * dy;
    }
}
