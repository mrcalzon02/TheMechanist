package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Generator-facing contract for faction and resident vehicle parking.
 *
 * Parking is infrastructure, not decorative room clutter: lots reserve their own
 * footprint, reserve the road connector, and expose exact parking-space anchors
 * for later vehicle/entity placement.
 */
final class FactionParkingInfrastructureRules {
    enum LotSize {
        SMALL("Small Vehicle Lot", 4, 2, 2),
        MEDIUM("Medium Vehicle Lot", 8, 4, 2),
        LARGE("Large Vehicle Lot", 16, 4, 4);

        final String label;
        final int maxVehicles;
        final int columns;
        final int rows;

        LotSize(String label, int maxVehicles, int columns, int rows) {
            this.label = label;
            this.maxVehicles = maxVehicles;
            this.columns = columns;
            this.rows = rows;
        }

        static LotSize forCapacity(int vehicles) {
            if (vehicles <= SMALL.maxVehicles) return SMALL;
            if (vehicles <= MEDIUM.maxVehicles) return MEDIUM;
            return LARGE;
        }
    }

    enum LotAccess {
        FACTION_SECURED,
        RESIDENT_PERSONAL,
        PUBLIC_NEUTRAL,
        INDUSTRIAL_SERVICE
    }

    enum ConnectorSide {
        NORTH,
        SOUTH,
        EAST,
        WEST
    }

    static final class ParkingSpace {
        final int index;
        final Rectangle bounds;
        final Point vehicleAnchor;
        final boolean residentEligible;

        ParkingSpace(int index, Rectangle bounds, boolean residentEligible) {
            this.index = Math.max(0, index);
            this.bounds = bounds == null ? new Rectangle() : new Rectangle(bounds);
            this.vehicleAnchor = new Point(this.bounds.x + this.bounds.width / 2, this.bounds.y + this.bounds.height / 2);
            this.residentEligible = residentEligible;
        }
    }

    static final class ParkingLotPlan {
        final String factionId;
        final LotSize size;
        final LotAccess access;
        final Rectangle bounds;
        final Rectangle roadConnector;
        final ConnectorSide connectorSide;
        final ArrayList<ParkingSpace> spaces;
        final boolean usesLoadingDock;
        final String auditLabel;

        ParkingLotPlan(String factionId, LotSize size, LotAccess access, Rectangle bounds, Rectangle roadConnector,
                       ConnectorSide connectorSide, ArrayList<ParkingSpace> spaces, boolean usesLoadingDock, String auditLabel) {
            this.factionId = sanitizeFactionId(factionId);
            this.size = size == null ? LotSize.SMALL : size;
            this.access = access == null ? LotAccess.FACTION_SECURED : access;
            this.bounds = bounds == null ? new Rectangle() : new Rectangle(bounds);
            this.roadConnector = roadConnector == null ? new Rectangle() : new Rectangle(roadConnector);
            this.connectorSide = connectorSide == null ? ConnectorSide.SOUTH : connectorSide;
            this.spaces = spaces == null ? new ArrayList<>() : new ArrayList<>(spaces);
            this.usesLoadingDock = usesLoadingDock;
            this.auditLabel = auditLabel == null ? "" : auditLabel;
        }

        List<ParkingSpace> spaces() { return Collections.unmodifiableList(spaces); }
        int capacity() { return size.maxVehicles; }
        boolean allowsResidentPersonalVehicles() { return access == LotAccess.RESIDENT_PERSONAL || access == LotAccess.PUBLIC_NEUTRAL; }
    }

    private static final int SPACE_W = 5;
    private static final int SPACE_H = 7;
    private static final int DRIVE_LANE = 4;
    private static final int PAD = 2;

    private FactionParkingInfrastructureRules() {}

    static ParkingLotPlan createLot(String factionId, LotSize size, LotAccess access, int x, int y, ConnectorSide connectorSide) {
        LotSize safeSize = size == null ? LotSize.SMALL : size;
        ConnectorSide side = connectorSide == null ? ConnectorSide.SOUTH : connectorSide;
        int lotW = safeSize.columns * SPACE_W + PAD * 2;
        int lotH = safeSize.rows * SPACE_H + DRIVE_LANE + PAD * 2;
        Rectangle bounds = new Rectangle(x, y, lotW, lotH);
        Rectangle connector = connectorFor(bounds, side);
        ArrayList<ParkingSpace> spaces = new ArrayList<>();
        int index = 0;
        for (int row = 0; row < safeSize.rows; row++) {
            for (int col = 0; col < safeSize.columns; col++) {
                if (index >= safeSize.maxVehicles) break;
                int sx = bounds.x + PAD + col * SPACE_W;
                int sy = bounds.y + PAD + row * SPACE_H;
                spaces.add(new ParkingSpace(index, new Rectangle(sx, sy, SPACE_W - 1, SPACE_H - 1), access == LotAccess.RESIDENT_PERSONAL || access == LotAccess.PUBLIC_NEUTRAL));
                index++;
            }
        }
        boolean loadingDock = access == LotAccess.INDUSTRIAL_SERVICE;
        String label = sanitizeFactionId(factionId) + " " + safeSize.label + " " + access;
        return new ParkingLotPlan(factionId, safeSize, access, bounds, connector, side, spaces, loadingDock, label);
    }

    static void reserveLot(ZonePlacementValidator validator, ParkingLotPlan lot) {
        if (validator == null || lot == null) return;
        validator.reserve(lot.bounds, ZonePlacementValidator.ReservationKind.FACTION_RESERVED_ROOM, lot.auditLabel);
        validator.reserve(lot.roadConnector, ZonePlacementValidator.ReservationKind.CORRIDOR, lot.auditLabel + " road connector");
        for (ParkingSpace space : lot.spaces) {
            validator.reserve(space.bounds, ZonePlacementValidator.ReservationKind.FACTION_RESERVED_ROOM, lot.auditLabel + " space " + space.index);
        }
    }

    static LotSize recommendedLotSize(String factionId, int expectedVehicles, int wealthScore, int securityScore) {
        int demand = Math.max(0, expectedVehicles);
        if (wealthScore >= 75) demand += 4;
        if (securityScore >= 70) demand += 4;
        if (factionId != null) {
            String f = factionId.toLowerCase(Locale.ROOT);
            if (f.contains("arbite") || f.contains("pdf") || f.contains("military") || f.contains("noble")) demand += 4;
            if (f.contains("worker") || f.contains("scav") || f.contains("outcast")) demand -= 2;
        }
        return LotSize.forCapacity(Math.max(1, demand));
    }

    static LotAccess recommendedAccess(String factionId, boolean residentialDistrict, boolean industrialFacility, int securityScore) {
        if (industrialFacility) return LotAccess.INDUSTRIAL_SERVICE;
        if (residentialDistrict && securityScore < 55) return LotAccess.RESIDENT_PERSONAL;
        if (securityScore >= 65) return LotAccess.FACTION_SECURED;
        String f = factionId == null ? "" : factionId.toLowerCase(Locale.ROOT);
        if (f.contains("noble") || f.contains("arbite") || f.contains("pdf") || f.contains("guild")) return LotAccess.FACTION_SECURED;
        return residentialDistrict ? LotAccess.RESIDENT_PERSONAL : LotAccess.PUBLIC_NEUTRAL;
    }

    static RoadInfrastructureTileRules.SpecialPurpose preferredEntranceTile(ParkingLotPlan lot) {
        if (lot == null) return RoadInfrastructureTileRules.SpecialPurpose.PARKING_SPACE;
        if (lot.usesLoadingDock && lot.access == LotAccess.INDUSTRIAL_SERVICE) return RoadInfrastructureTileRules.SpecialPurpose.INDUSTRIAL_LOADING_DOCK;
        if (lot.access == LotAccess.INDUSTRIAL_SERVICE) return RoadInfrastructureTileRules.SpecialPurpose.LIGHT_COMMERCIAL_LOADING_DOCK;
        if (lot.access == LotAccess.FACTION_SECURED) return RoadInfrastructureTileRules.SpecialPurpose.TOLL_GATE;
        return RoadInfrastructureTileRules.SpecialPurpose.PARKING_SPACE;
    }

    static ParkingSpace chooseResidentSpace(ParkingLotPlan lot, Random rng) {
        if (lot == null || !lot.allowsResidentPersonalVehicles()) return null;
        ArrayList<ParkingSpace> candidates = new ArrayList<>();
        for (ParkingSpace space : lot.spaces) if (space.residentEligible) candidates.add(space);
        if (candidates.isEmpty()) return null;
        Random safeRng = rng == null ? new Random(0L) : rng;
        return candidates.get(safeRng.nextInt(candidates.size()));
    }

    private static Rectangle connectorFor(Rectangle bounds, ConnectorSide side) {
        int cx = bounds.x + bounds.width / 2;
        int cy = bounds.y + bounds.height / 2;
        int w = 4;
        return switch (side) {
            case NORTH -> new Rectangle(cx - w / 2, bounds.y - DRIVE_LANE, w, DRIVE_LANE + 1);
            case SOUTH -> new Rectangle(cx - w / 2, bounds.y + bounds.height - 1, w, DRIVE_LANE + 1);
            case EAST -> new Rectangle(bounds.x + bounds.width - 1, cy - w / 2, DRIVE_LANE + 1, w);
            case WEST -> new Rectangle(bounds.x - DRIVE_LANE, cy - w / 2, DRIVE_LANE + 1, w);
        };
    }

    private static String sanitizeFactionId(String factionId) {
        return factionId == null || factionId.isBlank() ? "neutral" : factionId.trim().replace('\n', ' ');
    }
}
