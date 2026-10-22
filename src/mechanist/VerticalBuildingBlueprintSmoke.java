package mechanist;

import java.util.List;

/** Geometry, grouping, identity and vertical-volume invariants before tile stamping. */
public final class VerticalBuildingBlueprintSmoke {
    private VerticalBuildingBlueprintSmoke() { }

    public static void main(String[] args) {
        List<VerticalBuildingBlueprintAuthority.Blueprint> buildings =
                VerticalBuildingBlueprintAuthority.forZone(77177L, 1, 2, 2, 3);
        List<VerticalBuildingBlueprintAuthority.Blueprint> restored =
                VerticalBuildingBlueprintAuthority.forZone(77177L, 1, 2, 2, 3);
        require(buildings.equals(restored), "building plans changed on deterministic reload");
        require(buildings.size() == 3, "multiple rooms must not be counted as multiple buildings");
        require(!buildings.get(0).buildingId().equals(buildings.get(1).buildingId()),
                "different apartment complexes share a building identity");
        require(!buildings.get(0).buildingId().equals(
                VerticalBuildingBlueprintAuthority.forZone(77178L, 1, 2, 2, 3).get(0).buildingId()),
                "different world seeds share a building identity");
        require(!buildings.get(0).buildingId().equals(
                VerticalBuildingBlueprintAuthority.forZone(77177L, 2, 2, 2, 3).get(0).buildingId()),
                "different sectors share a building identity");

        var apartments = buildings.get(0);
        require(apartments.firstFloor() == 2 && apartments.lastFloor() == 4
                && apartments.floorCount() == 3 && apartments.roomCount() == 12,
                "three levels of independently usable apartments were not grouped as one building");
        require(apartments.plate(1) == null && apartments.plate(5) == null,
                "building incorrectly claims outside its floor span");
        require(apartments.connections().size() == 4,
                "each adjacent apartment floor needs both stair and lift connections");
        for (int floor = 2; floor <= 4; floor++) {
            var plate = apartments.plate(floor);
            require(plate != null && plate.roomCount() == 4,
                    "apartment level must retain four independently counted rooms");
            require(plate.space(VerticalBuildingBlueprintAuthority.Use.STAIRWELL).bounds()
                    .equals(apartments.plate(2).space(VerticalBuildingBlueprintAuthority.Use.STAIRWELL).bounds()),
                    "apartment stairs do not retain their shaft across floors");
        }

        var atrium = buildings.get(2);
        require(atrium.firstFloor() == 6 && atrium.lastFloor() == 8
                && atrium.roomCount() == 13, "atrium building rooms counted its void as usable floor");
        var lower = atrium.plate(6).space(VerticalBuildingBlueprintAuthority.Use.ATRIUM_FLOOR);
        require(lower != null && lower.use().hasWalkingSurface(), "atrium ground floor is not walkable");
        for (int floor = 7; floor <= 8; floor++) {
            var opening = atrium.plate(floor).space(VerticalBuildingBlueprintAuthority.Use.OPEN_TO_BELOW);
            require(opening != null && !opening.use().hasWalkingSurface()
                    && opening.bounds().equals(lower.bounds()),
                    "atrial air volume did not remain open and aligned across upper floors");
            require(atrium.plate(floor).space(VerticalBuildingBlueprintAuthority.Use.BALCONY) != null,
                    "upper atrium floors lack overlook galleries");
        }

        boolean malformedRejected = false;
        try {
            new VerticalBuildingBlueprintAuthority.Blueprint(
                    "invalid", VerticalBuildingBlueprintAuthority.Kind.ATRIUM_HALL,
                    1, 1, 1, 1, 5, 6, atrium.footprint(),
                    List.of(atrium.plates().get(1), atrium.plates().get(0)), atrium.connections());
        } catch (IllegalArgumentException expected) {
            malformedRejected = true;
        }
        require(malformedRejected, "unordered cross-floor footprint did not fail structural validation");
        System.out.println("VerticalBuildingBlueprintSmoke PASS buildings=" + buildings.size()
                + " apartmentRooms=" + apartments.roomCount()
                + " atriumRooms=" + atrium.roomCount()
                + " sameShaft=true voidsNonWalkable=true deterministic=true");
    }
    private static void require(boolean value, String detail) {
        if (!value) throw new AssertionError(detail);
    }
}
