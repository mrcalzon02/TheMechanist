package mechanist;

/**
 * Road-infrastructure atlas semantics for deterministic road tile selection.
 *
 * Atlas convention is 1-based in design notes, zero-based in code:
 * columns are themes/variants; rows are road purpose/orientation.
 */
final class RoadInfrastructureTileRules {
    enum RoadRow {
        NORTH_SOUTH(1),
        EAST_WEST(2),
        RIGHT_HAND_TURN(3),
        INTERSECTION(4),
        SIDEWALK_OR_SPECIAL(5);

        final int oneBasedRow;
        RoadRow(int oneBasedRow) { this.oneBasedRow = oneBasedRow; }
        int zeroBasedRow() { return oneBasedRow - 1; }
    }

    enum SpecialPurpose {
        ROAD,
        SIDEWALK,
        STREET_LIGHT_SIDEWALK,
        TOLL_GATE,
        PARKING_SPACE,
        SPECIAL_PARKING,
        SEWER_MANHOLE,
        LIGHT_COMMERCIAL_LOADING_DOCK,
        INDUSTRIAL_LOADING_DOCK,
        ROAD_BLOCKADE_OR_GATEWAY,
        UNKNOWN
    }

    static final class TileChoice {
        final int themeColumnOneBased;
        final RoadRow row;
        final SpecialPurpose purpose;
        final String reason;

        TileChoice(int themeColumnOneBased, RoadRow row, SpecialPurpose purpose, String reason) {
            this.themeColumnOneBased = Math.max(1, themeColumnOneBased);
            this.row = row == null ? RoadRow.NORTH_SOUTH : row;
            this.purpose = purpose == null ? SpecialPurpose.ROAD : purpose;
            this.reason = reason == null ? "" : reason;
        }

        int atlasColumnZeroBased() { return themeColumnOneBased - 1; }
        int atlasRowZeroBased() { return row.zeroBasedRow(); }
        String atlasKey() { return "road_infrastructure[c" + themeColumnOneBased + ",r" + row.oneBasedRow + "]"; }
    }

    static final class Neighborhood {
        final boolean northRoad;
        final boolean southRoad;
        final boolean eastRoad;
        final boolean westRoad;
        final boolean northSidewalk;
        final boolean southSidewalk;
        final boolean eastSidewalk;
        final boolean westSidewalk;
        final boolean northDoor;
        final boolean southDoor;
        final boolean eastDoor;
        final boolean westDoor;

        Neighborhood(boolean northRoad, boolean southRoad, boolean eastRoad, boolean westRoad,
                     boolean northSidewalk, boolean southSidewalk, boolean eastSidewalk, boolean westSidewalk,
                     boolean northDoor, boolean southDoor, boolean eastDoor, boolean westDoor) {
            this.northRoad = northRoad;
            this.southRoad = southRoad;
            this.eastRoad = eastRoad;
            this.westRoad = westRoad;
            this.northSidewalk = northSidewalk;
            this.southSidewalk = southSidewalk;
            this.eastSidewalk = eastSidewalk;
            this.westSidewalk = westSidewalk;
            this.northDoor = northDoor;
            this.southDoor = southDoor;
            this.eastDoor = eastDoor;
            this.westDoor = westDoor;
        }

        boolean n() { return northRoad || northDoor; }
        boolean s() { return southRoad || southDoor; }
        boolean e() { return eastRoad || eastDoor; }
        boolean w() { return westRoad || westDoor; }
        int connectionCount() { return (n() ? 1 : 0) + (s() ? 1 : 0) + (e() ? 1 : 0) + (w() ? 1 : 0); }
        boolean hasSidewalk() { return northSidewalk || southSidewalk || eastSidewalk || westSidewalk; }
    }

    private RoadInfrastructureTileRules() {}

    static TileChoice chooseRoadTile(Neighborhood n, int themeColumnOneBased) {
        if (n == null) return new TileChoice(themeColumnOneBased, RoadRow.NORTH_SOUTH, SpecialPurpose.ROAD, "default isolated road");
        int count = n.connectionCount();
        if (count > 2) return new TileChoice(themeColumnOneBased, RoadRow.INTERSECTION, SpecialPurpose.ROAD, "three-or-four-way road connection");
        if (n.hasSidewalk()) return sidewalkAwareRoadTile(n, themeColumnOneBased);
        if ((n.n() && n.s()) || count <= 1) return new TileChoice(themeColumnOneBased, RoadRow.NORTH_SOUTH, SpecialPurpose.ROAD, "north/south road or isolated vertical default");
        if (n.e() && n.w()) return new TileChoice(themeColumnOneBased, RoadRow.EAST_WEST, SpecialPurpose.ROAD, "east/west road");
        return new TileChoice(themeColumnOneBased, RoadRow.RIGHT_HAND_TURN, SpecialPurpose.ROAD, "road bend or non-door terminal");
    }

    static TileChoice sidewalkAwareRoadTile(Neighborhood n, int themeColumnOneBased) {
        boolean sidewalkNorthSouth = n.northSidewalk || n.southSidewalk;
        boolean sidewalkEastWest = n.eastSidewalk || n.westSidewalk;
        if (sidewalkNorthSouth && !sidewalkEastWest) return new TileChoice(themeColumnOneBased, RoadRow.EAST_WEST, SpecialPurpose.ROAD, "road perpendicular to north/south sidewalk edge");
        if (sidewalkEastWest && !sidewalkNorthSouth) return new TileChoice(themeColumnOneBased, RoadRow.NORTH_SOUTH, SpecialPurpose.ROAD, "road perpendicular to east/west sidewalk edge");
        return chooseRoadTile(new Neighborhood(n.northRoad, n.southRoad, n.eastRoad, n.westRoad, false, false, false, false, n.northDoor, n.southDoor, n.eastDoor, n.westDoor), themeColumnOneBased);
    }

    static SpecialPurpose specialPurpose(int columnOneBased, int rowOneBased) {
        if (rowOneBased == 5 && (columnOneBased == 1 || columnOneBased == 2)) return SpecialPurpose.STREET_LIGHT_SIDEWALK;
        if (rowOneBased == 4 && columnOneBased >= 1 && columnOneBased <= 3) return SpecialPurpose.TOLL_GATE;
        if (rowOneBased == 2 && (columnOneBased == 3 || columnOneBased == 4)) return SpecialPurpose.PARKING_SPACE;
        if (rowOneBased == 2 && columnOneBased == 5) return SpecialPurpose.SPECIAL_PARKING;
        if (rowOneBased == 5 && columnOneBased == 3) return SpecialPurpose.SEWER_MANHOLE;
        if (rowOneBased == 5 && columnOneBased == 5) return SpecialPurpose.INDUSTRIAL_LOADING_DOCK;
        if (rowOneBased == 2 && columnOneBased == 2) return SpecialPurpose.LIGHT_COMMERCIAL_LOADING_DOCK;
        if (rowOneBased == 5) return SpecialPurpose.SIDEWALK;
        if (rowOneBased >= 1 && rowOneBased <= 4) return SpecialPurpose.ROAD;
        return SpecialPurpose.UNKNOWN;
    }

    static boolean doorsCountAsRoads() { return true; }
    static boolean intersectionRequiresMoreThanTwoRoads() { return true; }
    static boolean rightTurnAllowedForNonDoorTerminal() { return true; }
    static boolean isTransitionRoadPurpose(SpecialPurpose purpose) { return purpose == SpecialPurpose.ROAD || purpose == SpecialPurpose.TOLL_GATE || purpose == SpecialPurpose.SEWER_MANHOLE || purpose == SpecialPurpose.LIGHT_COMMERCIAL_LOADING_DOCK || purpose == SpecialPurpose.INDUSTRIAL_LOADING_DOCK; }
}
