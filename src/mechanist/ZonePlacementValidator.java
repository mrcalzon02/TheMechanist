package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central placement-reservation validator for world generation.
 *
 * Rooms must not be placed over roads, corridors, plazas, edge transitions, or
 * pre-placed vertical transition rooms.  The generator should reserve those
 * areas first, then ask this validator before placing ordinary rooms or faction
 * expansion footprints.
 */
final class ZonePlacementValidator {
    enum ReservationKind {
        ROAD,
        CORRIDOR,
        CENTRAL_PLAZA,
        CARDINAL_TRANSITION,
        VERTICAL_TRANSITION_ROOM,
        DOOR_BUFFER,
        FACTION_RESERVED_ROOM,
        ROOM
    }

    static final class Reservation {
        final Rectangle bounds;
        final ReservationKind kind;
        final String label;

        Reservation(Rectangle bounds, ReservationKind kind, String label) {
            this.bounds = bounds == null ? new Rectangle() : new Rectangle(bounds);
            this.kind = kind == null ? ReservationKind.ROOM : kind;
            this.label = label == null ? "" : label;
        }

        boolean blocksRoomPlacement() {
            return kind != ReservationKind.ROOM;
        }
    }

    static final class PlacementReport {
        final boolean valid;
        final String reason;
        final Reservation blocker;

        private PlacementReport(boolean valid, String reason, Reservation blocker) {
            this.valid = valid;
            this.reason = reason == null ? "" : reason;
            this.blocker = blocker;
        }

        static PlacementReport ok() { return new PlacementReport(true, "ok", null); }
        static PlacementReport blocked(String reason, Reservation blocker) { return new PlacementReport(false, reason, blocker); }
    }

    private final int width;
    private final int height;
    private final ArrayList<Reservation> reservations = new ArrayList<>();

    ZonePlacementValidator(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    void reserve(Rectangle bounds, ReservationKind kind, String label) {
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) return;
        Rectangle clipped = bounds.intersection(new Rectangle(0, 0, width, height));
        if (clipped.width <= 0 || clipped.height <= 0) return;
        reservations.add(new Reservation(clipped, kind, label));
    }

    void reservePoint(int x, int y, int radius, ReservationKind kind, String label) {
        int r = Math.max(0, radius);
        reserve(new Rectangle(x - r, y - r, r * 2 + 1, r * 2 + 1), kind, label);
    }

    PlacementReport validateRoom(Rectangle candidate) {
        if (candidate == null || candidate.width <= 0 || candidate.height <= 0) return PlacementReport.blocked("empty room footprint", null);
        if (candidate.x < 1 || candidate.y < 1 || candidate.x + candidate.width >= width - 1 || candidate.y + candidate.height >= height - 1) {
            return PlacementReport.blocked("room footprint is too close to the sector edge; prefer interior placement", null);
        }
        for (Reservation reservation : reservations) {
            if (!reservation.blocksRoomPlacement()) continue;
            if (reservation.bounds.intersects(candidate)) {
                return PlacementReport.blocked("room overlaps reserved " + reservation.kind + " area " + reservation.label, reservation);
            }
        }
        return PlacementReport.ok();
    }

    int interiorPreferenceScore(Rectangle candidate, Point plaza) {
        if (candidate == null) return Integer.MIN_VALUE;
        int cx = candidate.x + candidate.width / 2;
        int cy = candidate.y + candidate.height / 2;
        int edgeDistance = Math.min(Math.min(cx, width - 1 - cx), Math.min(cy, height - 1 - cy));
        int score = edgeDistance * 8;
        if (plaza != null) {
            int manhattan = Math.abs(cx - plaza.x) + Math.abs(cy - plaza.y);
            score += Math.max(0, Math.min(width, height) - manhattan);
        }
        for (Reservation reservation : reservations) {
            if (!reservation.blocksRoomPlacement()) continue;
            if (expanded(reservation.bounds, 3).intersects(candidate)) score -= 250;
        }
        return score;
    }

    List<Reservation> reservations() {
        return Collections.unmodifiableList(reservations);
    }

    private Rectangle expanded(Rectangle source, int amount) {
        int a = Math.max(0, amount);
        return new Rectangle(source.x - a, source.y - a, source.width + a * 2, source.height + a * 2);
    }
}
