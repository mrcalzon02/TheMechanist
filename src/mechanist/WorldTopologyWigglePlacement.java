package mechanist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bounded placement adjustment around topology anchors.
 *
 * Required topology anchors should not be randomized away.  When a generator pass
 * discovers a local collision, it may ask this helper for the nearest legal
 * rectangle within a small deterministic radius.  If no placement is available,
 * the generator should report a topology validation error rather than silently
 * creating disconnected transitions.
 */
final class WorldTopologyWigglePlacement {
    static final int DEFAULT_WIGGLE_RADIUS = 12;

    private WorldTopologyWigglePlacement() {}

    record WiggleResult(
            boolean placed,
            WorldTopologyPreplacementPlan.TileRect original,
            WorldTopologyPreplacementPlan.TileRect adjusted,
            int dx,
            int dy,
            String reason
    ) {
        static WiggleResult unchanged(WorldTopologyPreplacementPlan.TileRect rect) {
            return new WiggleResult(true, rect, rect, 0, 0, "original placement accepted");
        }

        static WiggleResult failed(WorldTopologyPreplacementPlan.TileRect rect, String reason) {
            return new WiggleResult(false, rect, rect, 0, 0, reason == null ? "no legal wiggle placement" : reason);
        }
    }

    interface Occupancy {
        boolean blocked(WorldTopologyPreplacementPlan.TileRect rect);
    }

    static WiggleResult placeNearest(WorldTopologyPreplacementPlan.TileRect desired, int sectorSize, Occupancy occupancy) {
        return placeNearest(desired, sectorSize, DEFAULT_WIGGLE_RADIUS, occupancy);
    }

    static WiggleResult placeNearest(WorldTopologyPreplacementPlan.TileRect desired, int sectorSize, int radius, Occupancy occupancy) {
        if (desired == null) return WiggleResult.failed(null, "desired rectangle missing");
        WorldTopologyPreplacementPlan.TileRect clamped = desired.clampInside(sectorSize);
        if (isLegal(clamped, sectorSize, occupancy)) return WiggleResult.unchanged(clamped);

        ArrayList<int[]> offsets = new ArrayList<>();
        int r = Math.max(0, radius);
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                if (dx == 0 && dy == 0) continue;
                offsets.add(new int[]{dx, dy, Math.abs(dx) + Math.abs(dy), dx * dx + dy * dy});
            }
        }
        offsets.sort(Comparator.<int[]>comparingInt(a -> a[2]).thenComparingInt(a -> a[3]).thenComparingInt(a -> a[1]).thenComparingInt(a -> a[0]));
        for (int[] offset : offsets) {
            WorldTopologyPreplacementPlan.TileRect candidate = new WorldTopologyPreplacementPlan.TileRect(clamped.x() + offset[0], clamped.y() + offset[1], clamped.width(), clamped.height()).clampInside(sectorSize);
            if (isLegal(candidate, sectorSize, occupancy)) return new WiggleResult(true, clamped, candidate, candidate.x() - clamped.x(), candidate.y() - clamped.y(), "nearest legal wiggle placement");
        }
        return WiggleResult.failed(clamped, "no legal placement inside radius " + r);
    }

    static Occupancy fromReservations(List<WorldTopologyPreplacementPlan.Reservation> reservations, WorldTopologyPreplacementPlan.Reservation ignore) {
        List<WorldTopologyPreplacementPlan.Reservation> safe = reservations == null ? List.of() : List.copyOf(reservations);
        return rect -> {
            if (rect == null) return true;
            for (WorldTopologyPreplacementPlan.Reservation reservation : safe) {
                if (reservation == null || reservation == ignore) continue;
                if (reservation.bounds() == null) continue;
                if (allowedReservationOverlap(ignore, reservation)) continue;
                if (overlap(rect, reservation.bounds())) return true;
            }
            return false;
        };
    }

    static boolean isLegal(WorldTopologyPreplacementPlan.TileRect rect, int sectorSize, Occupancy occupancy) {
        if (rect == null || rect.width() <= 0 || rect.height() <= 0) return false;
        if (rect.x() < 0 || rect.y() < 0 || rect.right() >= sectorSize || rect.bottom() >= sectorSize) return false;
        return occupancy == null || !occupancy.blocked(rect);
    }

    static boolean overlap(WorldTopologyPreplacementPlan.TileRect a, WorldTopologyPreplacementPlan.TileRect b) {
        if (a == null || b == null) return false;
        return a.x() <= b.right() && a.right() >= b.x() && a.y() <= b.bottom() && a.bottom() >= b.y();
    }

    static boolean allowedReservationOverlap(WorldTopologyPreplacementPlan.Reservation a, WorldTopologyPreplacementPlan.Reservation b) {
        if (a == null || b == null) return false;
        return allowedKindOverlap(a.kind(), b.kind());
    }

    static boolean allowedKindOverlap(WorldTopologyPreplacementPlan.ReservationKind a, WorldTopologyPreplacementPlan.ReservationKind b) {
        if (a == WorldTopologyPreplacementPlan.ReservationKind.CARDINAL_ROAD || b == WorldTopologyPreplacementPlan.ReservationKind.CARDINAL_ROAD) return true;
        if (a == WorldTopologyPreplacementPlan.ReservationKind.EDGE_DOUBLE_DOOR || b == WorldTopologyPreplacementPlan.ReservationKind.EDGE_DOUBLE_DOOR) return true;
        return false;
    }

    static void audit(WiggleResult result, String source) {
        if (result == null) {
            DebugLog.warn("WORLD_TOPOLOGY_WIGGLE", "source=" + safe(source) + " result missing");
            return;
        }
        String line = "source=" + safe(source)
                + " placed=" + result.placed()
                + " dx=" + result.dx()
                + " dy=" + result.dy()
                + " reason=" + result.reason();
        if (result.placed()) DebugLog.audit("WORLD_TOPOLOGY_WIGGLE", line);
        else DebugLog.warn("WORLD_TOPOLOGY_WIGGLE", line);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unspecified" : value.replace('\n', ' ').trim();
    }
}
