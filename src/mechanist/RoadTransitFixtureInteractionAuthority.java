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
        String line;
        if (RoadTransitFixtureAuthority.isVehicleType(m.type)) {
            line = VehicleRuntimeAuthority.interact(g, m).message();
        } else if (VehicleEconomyFrontageAuthority.isCommerceType(m.type)) {
            line = VehicleEconomyFrontageAuthority.interaction(g, m);
        } else {
            line = RoadTransitFixtureAuthority.inspectionLine(m);
        }
        g.logEvent(line);
        g.gainXp("Navigation", 1, "handled road/transit fixture");
        m.vendCount++;
        m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, 24);
        g.sounds.playDistantCue(FixtureInteractionRegistry.soundFor(m.type, "ambient_door_servo"), 4, g.options);
        g.advanceTurn(RoadTransitFixtureAuthority.actionVerb(m.type));
        g.repaint();
        return true;
    }
}
