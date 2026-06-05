package mechanist;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Validation pass for topology reservations before the room/faction generator
 * consumes them.
 *
 * This is deliberately conservative: it reports errors and warnings, but it does
 * not mutate the world.  Later generator passes can decide whether to fail hard,
 * wiggle a reservation, or downgrade a non-critical room placement.
 */
final class WorldTopologyPlacementValidator {
    private WorldTopologyPlacementValidator() {}

    enum Severity { ERROR, WARNING }

    record Issue(Severity severity, String code, String message) {
        boolean error() { return severity == Severity.ERROR; }
    }

    record ValidationReport(List<Issue> issues) {
        boolean hasErrors() {
            for (Issue issue : issues) if (issue.error()) return true;
            return false;
        }

        int errorCount() {
            int count = 0;
            for (Issue issue : issues) if (issue.error()) count++;
            return count;
        }

        int warningCount() { return Math.max(0, issues.size() - errorCount()); }

        String summary() {
            return "errors=" + errorCount() + " warnings=" + warningCount() + " issues=" + issues.size();
        }

        String detailLine() {
            if (issues.isEmpty()) return summary() + " clean";
            StringBuilder out = new StringBuilder(summary());
            int shown = 0;
            for (Issue issue : issues) {
                if (shown++ >= 4) {
                    out.append(" ...");
                    break;
                }
                out.append(" | ").append(issue.severity()).append(':').append(issue.code()).append(' ').append(issue.message());
            }
            return out.toString();
        }
    }

    static ValidationReport validate(WorldTopologyPreplacementPlan.Plan plan) {
        ArrayList<Issue> issues = new ArrayList<>();
        if (plan == null || plan.topology() == null) {
            issues.add(new Issue(Severity.ERROR, "missing-plan", "No topology preplacement plan exists."));
            return new ValidationReport(List.copyOf(issues));
        }
        int size = plan.topology().sectorSize().tiles;
        validateTopology(plan.topology(), issues);
        validateReservations(plan, size, issues);
        validateConnectivityMinimums(plan, issues);
        return new ValidationReport(List.copyOf(issues));
    }

    static void audit(WorldTopologyPreplacementPlan.Plan plan, String source) {
        ValidationReport report = validate(plan);
        DebugLog.audit("WORLD_TOPOLOGY_VALIDATE", "source=" + safe(source) + " " + report.detailLine());
    }

    private static void validateTopology(WorldTopologyContract.ZoneTransitionPlan topology, ArrayList<Issue> issues) {
        for (WorldTopologyContract.CardinalExit direction : WorldTopologyContract.CardinalExit.values()) {
            WorldTopologyContract.EdgeTransitionAnchor exit = topology.exit(direction);
            if (exit == null) {
                issues.add(new Issue(Severity.ERROR, "missing-cardinal-exit", "Missing " + direction + " transition anchor."));
                continue;
            }
            if (!exit.isDoubleDoorCenteredInRoad()) {
                issues.add(new Issue(Severity.ERROR, "door-not-centered", direction + " double door is not centered on its road."));
            }
            WorldTopologyContract.EdgeTransitionAnchor neighbor = exit.matchingNeighborEntrance();
            if (neighbor == null || neighbor.direction() != direction.opposite()) {
                issues.add(new Issue(Severity.ERROR, "bad-opposite-edge", direction + " exit does not resolve to the opposing neighbor entrance."));
            }
        }
    }

    private static void validateReservations(WorldTopologyPreplacementPlan.Plan plan, int size, ArrayList<Issue> issues) {
        List<WorldTopologyPreplacementPlan.Reservation> reservations = plan.reservations();
        for (WorldTopologyPreplacementPlan.Reservation reservation : reservations) {
            WorldTopologyPreplacementPlan.TileRect r = reservation.bounds();
            if (r == null) {
                issues.add(new Issue(Severity.ERROR, "missing-bounds", reservation.id() + " has no bounds."));
                continue;
            }
            if (r.width() <= 0 || r.height() <= 0) {
                issues.add(new Issue(Severity.ERROR, "invalid-bounds", reservation.id() + " has non-positive bounds " + r.width() + "x" + r.height() + "."));
            }
            if (r.x() < 0 || r.y() < 0 || r.right() >= size || r.bottom() >= size) {
                issues.add(new Issue(Severity.ERROR, "out-of-bounds", reservation.id() + " lies outside the fixed sector square."));
            }
        }
        for (int i = 0; i < reservations.size(); i++) {
            for (int j = i + 1; j < reservations.size(); j++) {
                WorldTopologyPreplacementPlan.Reservation a = reservations.get(i);
                WorldTopologyPreplacementPlan.Reservation b = reservations.get(j);
                if (!overlap(a.bounds(), b.bounds())) continue;
                if (allowedOverlap(a.kind(), b.kind())) continue;
                issues.add(new Issue(Severity.WARNING, "reservation-overlap", a.id() + " overlaps " + b.id() + "."));
            }
        }
    }

    private static void validateConnectivityMinimums(WorldTopologyPreplacementPlan.Plan plan, ArrayList<Issue> issues) {
        EnumMap<WorldTopologyPreplacementPlan.ReservationKind, Integer> counts = new EnumMap<>(WorldTopologyPreplacementPlan.ReservationKind.class);
        for (WorldTopologyPreplacementPlan.Reservation reservation : plan.reservations()) {
            counts.merge(reservation.kind(), 1, Integer::sum);
        }
        requireAtLeast(counts, WorldTopologyPreplacementPlan.ReservationKind.CENTRAL_PLAZA, 1, issues);
        requireAtLeast(counts, WorldTopologyPreplacementPlan.ReservationKind.CARDINAL_ROAD, 4, issues);
        requireAtLeast(counts, WorldTopologyPreplacementPlan.ReservationKind.EDGE_DOUBLE_DOOR, 4, issues);
        requireAtLeast(counts, WorldTopologyPreplacementPlan.ReservationKind.EDGE_TRANSITION_ROOM, 4, issues);
        requireAtLeast(counts, WorldTopologyPreplacementPlan.ReservationKind.ELEVATOR_TRANSITION_ROOM, 1, issues);
        requireAtLeast(counts, WorldTopologyPreplacementPlan.ReservationKind.STAIRWELL_TRANSITION_ROOM, 2, issues);
        requireAtLeast(counts, WorldTopologyPreplacementPlan.ReservationKind.MANHOLE_TRANSITION_ROOM, 1, issues);
        requireAtLeast(counts, WorldTopologyPreplacementPlan.ReservationKind.DRAIN_OUTFLOW_ROOM, 1, issues);
    }

    private static void requireAtLeast(EnumMap<WorldTopologyPreplacementPlan.ReservationKind, Integer> counts, WorldTopologyPreplacementPlan.ReservationKind kind, int needed, ArrayList<Issue> issues) {
        int found = counts.getOrDefault(kind, 0);
        if (found < needed) issues.add(new Issue(Severity.ERROR, "missing-" + kind.name().toLowerCase(), "Expected at least " + needed + " " + kind + " reservations, found " + found + "."));
    }

    private static boolean overlap(WorldTopologyPreplacementPlan.TileRect a, WorldTopologyPreplacementPlan.TileRect b) {
        if (a == null || b == null) return false;
        return a.x() <= b.right() && a.right() >= b.x() && a.y() <= b.bottom() && a.bottom() >= b.y();
    }

    private static boolean allowedOverlap(WorldTopologyPreplacementPlan.ReservationKind a, WorldTopologyPreplacementPlan.ReservationKind b) {
        if (a == WorldTopologyPreplacementPlan.ReservationKind.CARDINAL_ROAD || b == WorldTopologyPreplacementPlan.ReservationKind.CARDINAL_ROAD) return true;
        if (a == WorldTopologyPreplacementPlan.ReservationKind.EDGE_DOUBLE_DOOR || b == WorldTopologyPreplacementPlan.ReservationKind.EDGE_DOUBLE_DOOR) return true;
        return false;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unspecified" : value.replace('\n', ' ').trim();
    }
}
