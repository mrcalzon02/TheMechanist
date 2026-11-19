package mechanist;

import java.util.*;

/** Handles road/transit fixture inspection and authoritative parked-vehicle actions. */
final class RoadTransitFixtureInteractionAuthority {
    static final String VERSION = "0.9.10ai";

    private RoadTransitFixtureInteractionAuthority() {}

    static boolean tryInteract(GamePanel g, int tx, int ty) {
        if (g == null || g.world == null) return false;
        MapObjectState m = g.world.mapObjectAt(tx, ty);
        if (m == null || !RoadTransitFixtureAuthority.isRoadTransitType(m.type)) return false;
        long distance = Math.abs((long) g.playerX - tx)
                + Math.abs((long) g.playerY - ty);
        if (distance > 1L) {
            g.logEvent("Move adjacent to the road fixture before interacting.");
            g.repaint();
            return true;
        }
        String line;
        boolean accepted = true;
        boolean changed = false;
        if (RoadTransitFixtureAuthority.isVehicleType(m.type)) {
            if (VehicleRuntimeAuthority.playerOwns(g, m)
                    && g.manualMovementPlanActive
                    && g.manualMovementPlanPath != null
                    && !g.manualMovementPlanPath.isEmpty()) {
                VehicleTransitAuthority.TransitResult result =
                        VehicleTransitAuthority.executeManualPlan(g, m);
                line = result.message();
                accepted = result.success();
                changed = result.changed();
            } else {
                VehicleRuntimeAuthority.Result result = VehicleRuntimeAuthority.interact(g, m);
                line = result.message();
                accepted = result.success();
                changed = result.changed();
                if (result.success() && !result.changed()
                        && VehicleRuntimeAuthority.playerOwns(g, m)) {
                    line += " Create a manual movement plan to a road or parking destination, then interact again to preview and commit a constrained vehicle route.";
                }
            }
        } else if (VehicleEconomyFrontageAuthority.isCommerceType(m.type)) {
            VehicleEconomyFrontageAuthority.InteractionResult result =
                    VehicleEconomyFrontageAuthority.interactionResult(g, m);
            line = result.message();
            accepted = result.success();
            changed = result.changed();
        } else {
            line = RoadTransitFixtureAuthority.inspectionLine(m);
        }
        g.logEvent(line);
        // Refusals and read-only inspections may report information, but cannot
        // consume time, earn repeatable XP, rotate stock, or start cooldowns.
        if (!accepted || !changed) {
            g.repaint();
            return true;
        }
        g.gainXp("Navigation", 1, "handled road/transit fixture");
        m.vendCount++;
        m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, 24);
        g.sounds.playDistantCue(FixtureInteractionRegistry.soundFor(m.type, "ambient_door_servo"), 4, g.options);
        g.advanceTurn(RoadTransitFixtureAuthority.actionVerb(m.type));
        g.repaint();
        return true;
    }
}
