package mechanist;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class InteractionApproachAuthority {
    record ApproachPlan(boolean available, int x, int y, List<Point> path, String message) { }

    private InteractionApproachAuthority() {}

    static ApproachPlan plan(GamePanel game, int targetX, int targetY, String targetLabel) {
        String label = targetLabel == null || targetLabel.isBlank() ? "target" : targetLabel;
        if (game == null || game.world == null) return new ApproachPlan(false, -1, -1, List.of(), "Approach unavailable: no world is loaded.");
        int[][] offsets = {{0,-1},{1,0},{0,1},{-1,0}};
        ArrayList<ApproachPlan> candidates = new ArrayList<>();
        for (int[] offset : offsets) {
            int x = targetX + offset[0];
            int y = targetY + offset[1];
            if (!game.world.inBounds(x, y)) continue;
            if (x == game.playerX && y == game.playerY) {
                candidates.add(new ApproachPlan(true, x, y, List.of(), "Already adjacent to " + label + "."));
                continue;
            }
            if (!MovementPlanningAuthority.canEnter(game, x, y)) continue;
            ArrayList<Point> path = MovementPlanningAuthority.buildPathTo(game, x, y,
                    MovementPlanningAuthority.rangeForMode(game.selectedMovementModeIndex));
            if (path.isEmpty() || !MovementPlanningAuthority.pathReaches(path, x, y)) continue;
            candidates.add(new ApproachPlan(true, x, y, List.copyOf(path),
                    "Approach plan selected for " + label + " at " + x + "," + y + "."));
        }
        ApproachPlan selected = selectShortest(candidates);
        return selected != null ? selected :
                new ApproachPlan(false, -1, -1, List.of(), "No reachable adjacent interaction tile for " + label
                        + " within the current movement range.");
    }

    static ApproachPlan selectShortest(List<ApproachPlan> candidates) {
        if (candidates == null) return null;
        return candidates.stream().filter(ApproachPlan::available)
                .min(Comparator.comparingInt(p -> p.path().size())).orElse(null);
    }
}
