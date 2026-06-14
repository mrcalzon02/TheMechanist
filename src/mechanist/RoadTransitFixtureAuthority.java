package mechanist;

import java.util.*;

/**
 * Road/transit fixture authority.
 *
 * Owns passive road-adjacent fixtures, parking markers, taxi booths, and parked
 * vehicle records. Ownership and commerce metadata are live; movement, fuel,
 * storage, collision, recall, and combat remain outside this authority.
 */
final class RoadTransitFixtureAuthority {
    static final String VERSION = "0.9.10ai";

    static final class VehicleProfile {
        final String type;
        final String label;
        final String stock;
        VehicleProfile(String type, String label, String stock) {
            this.type = type; this.label = label; this.stock = stock;
        }
    }

    private static final List<VehicleProfile> VEHICLE_PROFILES = List.of(
            new VehicleProfile(AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR, "Parked civilian car", "entity_car;health=foundation;armor=light;seats=4;access=passive-inspection"),
            new VehicleProfile(AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK, "Parked cargo truck", "entity_truck;health=foundation;armor=medium;seats=3;cargo=sealed;access=passive-inspection"),
            new VehicleProfile(AssetIntegrationDisciplineAuthority.PARKED_UTILITY_BIKE, "Parked utility bike", "entity_bike;health=foundation;armor=light;seats=1;access=passive-inspection"),
            new VehicleProfile(AssetIntegrationDisciplineAuthority.PARKED_ARMORED_CAR, "Armored patrol car", "entity_armored_car;health=foundation;armor=heavy;seats=4;access=passive-inspection"),
            new VehicleProfile(AssetIntegrationDisciplineAuthority.PARKED_TANK, "Parked armored crawler", "entity_tank;health=foundation;armor=very-heavy;seats=5;access=passive-inspection")
    );

    private RoadTransitFixtureAuthority() {}

    static boolean isRoadTransitType(String type) {
        String t = AssetIntegrationDisciplineAuthority.canonicalType(type);
        return AssetIntegrationDisciplineAuthority.ROAD_ALCOVE_FIXTURE.equals(t) ||
                AssetIntegrationDisciplineAuthority.PARK_OPEN_SPACE.equals(t) ||
                AssetIntegrationDisciplineAuthority.TAXI_BOOTH.equals(t) ||
                AssetIntegrationDisciplineAuthority.PARKING_LOT_MARKER.equals(t) ||
                AssetIntegrationDisciplineAuthority.ROAD_VEHICLE_STAGING_MARKER.equals(t) ||
                VehicleEconomyFrontageAuthority.isCommerceType(t) ||
                isVehicleType(t);
    }

    static boolean isVehicleType(String type) {
        String t = AssetIntegrationDisciplineAuthority.canonicalType(type);
        return AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR.equals(t) ||
                AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK.equals(t) ||
                AssetIntegrationDisciplineAuthority.PARKED_UTILITY_BIKE.equals(t) ||
                AssetIntegrationDisciplineAuthority.PARKED_ARMORED_CAR.equals(t) ||
                AssetIntegrationDisciplineAuthority.PARKED_TANK.equals(t);
    }

    static String artKeyForType(String type) {
        String t = AssetIntegrationDisciplineAuthority.canonicalType(type);
        if (AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR.equals(t)) return "entity_car";
        if (AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK.equals(t)) return "entity_truck";
        if (AssetIntegrationDisciplineAuthority.PARKED_UTILITY_BIKE.equals(t)) return "entity_bike";
        if (AssetIntegrationDisciplineAuthority.PARKED_ARMORED_CAR.equals(t)) return "entity_armored_car";
        if (AssetIntegrationDisciplineAuthority.PARKED_TANK.equals(t)) return "entity_tank";
        if (AssetIntegrationDisciplineAuthority.PARKING_LOT_MARKER.equals(t)) return "tile_road_intersection";
        if (AssetIntegrationDisciplineAuthority.ROAD_ALCOVE_FIXTURE.equals(t)) return "feature_public_service_counter";
        if (AssetIntegrationDisciplineAuthority.PARK_OPEN_SPACE.equals(t)) return "feature_park_planter";
        if (AssetIntegrationDisciplineAuthority.TAXI_BOOTH.equals(t)) return "feature_public_info_column";
        if (AssetIntegrationDisciplineAuthority.ROAD_VEHICLE_STAGING_MARKER.equals(t)) return "entity_car";
        if (VehicleEconomyFrontageAuthority.isCommerceType(t)) return "feature_public_service_counter";
        return null;
    }

    static VehicleProfile chooseVehicleProfile(Random r, ZoneType z) {
        if (r == null) r = new Random(0);
        int roll = Math.floorMod(r.nextInt(), 100);
        boolean military = z == ZoneType.IMPERIAL_GUARD_BILLET || z == ZoneType.ARBITES_PRECINCT_EDGE;
        if (military && roll < 30) return VEHICLE_PROFILES.get(3);
        if (military && roll < 38) return VEHICLE_PROFILES.get(4);
        if (roll < 34) return VEHICLE_PROFILES.get(0);
        if (roll < 62) return VEHICLE_PROFILES.get(1);
        if (roll < 84) return VEHICLE_PROFILES.get(2);
        return VEHICLE_PROFILES.get(3);
    }

    static VehicleProfile plazaStagingProfile(int index) {
        int i = Math.floorMod(index, 4);
        if (i == 0) return new VehicleProfile(AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR, "Parked service cart", "entity_car;health=foundation;armor=light;seats=2;access=passive-plaza-inspection");
        if (i == 1) return new VehicleProfile(AssetIntegrationDisciplineAuthority.PARKED_UTILITY_BIKE, "Abandoned utility bike", "entity_bike;health=foundation;armor=light;seats=1;access=passive-plaza-inspection");
        if (i == 2) return new VehicleProfile(AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK, "Cargo hauler bay", "entity_truck;health=foundation;armor=medium;seats=3;cargo=sealed;access=passive-plaza-inspection");
        return new VehicleProfile(AssetIntegrationDisciplineAuthority.PARKED_ARMORED_CAR, "Armored checkpoint vehicle", "entity_armored_car;health=foundation;armor=heavy;seats=4;access=passive-plaza-inspection");
    }

    static String labelForType(String type, ZoneType z, Random r) {
        String t = AssetIntegrationDisciplineAuthority.canonicalType(type);
        if (AssetIntegrationDisciplineAuthority.ROAD_ALCOVE_FIXTURE.equals(t)) return pick(r, "Roadside alcove", "Lay-by alcove", "Service-niche alcove");
        if (AssetIntegrationDisciplineAuthority.PARK_OPEN_SPACE.equals(t)) return pick(r, "Roadside park", "Civic breathing space", "Pocket park");
        if (AssetIntegrationDisciplineAuthority.TAXI_BOOTH.equals(t)) return pick(r, "Transit toll booth", "Taxi-call kiosk", "Road-service call booth");
        if (AssetIntegrationDisciplineAuthority.PARKING_LOT_MARKER.equals(t)) return pick(r, "Roadside parking lot", "Vehicle set-down bay", "Marked parking bay");
        if (AssetIntegrationDisciplineAuthority.ROAD_VEHICLE_STAGING_MARKER.equals(t)) return pick(r, "Parked service vehicle", "Roadside vehicle marker", "Vehicle staging point");
        for (VehicleProfile p : VEHICLE_PROFILES) if (p.type.equals(t)) return p.label;
        return "Road transit fixture";
    }

    static String stockForType(String type, char under) {
        String t = AssetIntegrationDisciplineAuthority.canonicalType(type);
        String base;
        if (AssetIntegrationDisciplineAuthority.ROAD_ALCOVE_FIXTURE.equals(t)) base = "road-adjacent;frontage-fixture;access=passive-inspection";
        else if (AssetIntegrationDisciplineAuthority.PARK_OPEN_SPACE.equals(t)) base = "park-open-space;open-space-room;access=passive-inspection";
        else if (AssetIntegrationDisciplineAuthority.TAXI_BOOTH.equals(t)) base = "taxi-booth;paid-zone-transport;access=passive-service-surface";
        else if (AssetIntegrationDisciplineAuthority.PARKING_LOT_MARKER.equals(t)) base = "parking-lot;vehicle-setdown;access=passive-inspection";
        else if (AssetIntegrationDisciplineAuthority.ROAD_VEHICLE_STAGING_MARKER.equals(t)) base = "road-grid;vehicle-staging-marker;access=passive-inspection";
        else if (AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR.equals(t)) base = VEHICLE_PROFILES.get(0).stock;
        else if (AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK.equals(t)) base = VEHICLE_PROFILES.get(1).stock;
        else if (AssetIntegrationDisciplineAuthority.PARKED_UTILITY_BIKE.equals(t)) base = VEHICLE_PROFILES.get(2).stock;
        else if (AssetIntegrationDisciplineAuthority.PARKED_ARMORED_CAR.equals(t)) base = VEHICLE_PROFILES.get(3).stock;
        else if (AssetIntegrationDisciplineAuthority.PARKED_TANK.equals(t)) base = VEHICLE_PROFILES.get(4).stock;
        else base = "road-transit;access=passive-inspection";
        return base + ";semantic=" + safe(artKeyForType(t)) + ";under=" + (int)under;
    }

    static MapObjectState newFixture(World w, int x, int y, String type, char glyph, Random r, String idPrefix) {
        MapObjectState m = new MapObjectState();
        m.x = x; m.y = y; m.glyph = glyph;
        m.type = AssetIntegrationDisciplineAuthority.canonicalType(type);
        ZoneType z = w == null ? null : w.zoneType;
        m.label = labelForType(m.type, z, r) + " / " + (z == null ? "Unknown zone" : z.label);
        char under = (w == null || !w.inBounds(x,y)) ? '.' : w.tiles[x][y];
        m.stockState = stockForType(m.type, under);
        m.cooldownUntilTurn = 0; m.vendCount = 0;
        m.id = (idPrefix == null || idPrefix.isBlank() ? "ROAD-TRANSIT" : idPrefix) + "-" + Math.abs(Objects.hash(m.type, w == null ? "world" : w.locationKey(), x, y, m.label));
        return m;
    }

    static MapObjectState newVehicleFixture(World w, int x, int y, VehicleProfile p, Random r, String idPrefix) {
        MapObjectState m = newFixture(w, x, y, p == null ? AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR : p.type, 'N', r, idPrefix);
        if (p != null) {
            m.label = p.label + " / " + (w == null || w.zoneType == null ? "Unknown zone" : w.zoneType.label);
            char under = (w == null || !w.inBounds(x,y)) ? '.' : w.tiles[x][y];
            m.stockState = p.stock + ";semantic=" + safe(artKeyForType(p.type)) + ";under=" + (int)under;
        }
        return m;
    }

    static String inspectionLine(MapObjectState m) {
        String t = AssetIntegrationDisciplineAuthority.canonicalType(m == null ? null : m.type);
        if (AssetIntegrationDisciplineAuthority.ROAD_ALCOVE_FIXTURE.equals(t)) return "ROAD ALCOVE: a small lay-by cut into the frontage. It is useful for foot traffic, shelter, and service-attachment metadata, and it is inspected as a civic fixture here.";
        if (AssetIntegrationDisciplineAuthority.PARK_OPEN_SPACE.equals(t)) return "ROADSIDE PARK: a small public breathing space with a planter and enough open paving to mark civic intent in a place that otherwise smells like industrial Fuelgel and damp concrete.";
        if (AssetIntegrationDisciplineAuthority.TAXI_BOOTH.equals(t)) return "TRANSIT BOOTH: a taxi-call and toll surface. It exposes route/service metadata here; boarding, fare execution, recall, and traffic are not exposed by this inspected fixture.";
        if (AssetIntegrationDisciplineAuthority.PARKING_LOT_MARKER.equals(t)) return "PARKING MARKER: a vehicle set-down bay with readable access metadata and no live storage, fueling, ownership, or driving behavior attached.";
        if (AssetIntegrationDisciplineAuthority.ROAD_VEHICLE_STAGING_MARKER.equals(t)) return "VEHICLE STAGING POINT: a passive roadside vehicle marker used to keep the street readable without spawning a movable vehicle entity.";
        if (isVehicleType(t)) return vehicleInspectionLine(m, t);
        if (VehicleEconomyFrontageAuthority.isCommerceType(t)) return "VEHICLE SERVICE FRONTAGE: " + (m == null ? "service metadata unavailable" : m.label) + ".";
        return "ROAD TRANSIT FIXTURE: passive transit metadata surface.";
    }

    static String vehicleInspectionLine(MapObjectState m, String type) {
        String stock = m == null || m.stockState == null ? "" : m.stockState;
        String armor = value(stock, "armor");
        String seats = value(stock, "seats");
        String cargo = value(stock, "cargo");
        String label = m == null || m.label == null ? labelForType(type, null, null) : m.label.split(" / ")[0];
        String owner = value(stock, "ownerFaction");
        String ownership = value(stock, "ownership");
        String role = value(stock, "vehicleRole");
        return "PARKED VEHICLE: " + label + ". Profile: armor=" + empty(armor, "unlisted") + ", seats=" + empty(seats, "unlisted") +
                (cargo.isBlank() ? "" : ", cargo=" + cargo) + ", owner=" + empty(owner, "unassigned") +
                ", ownership=" + empty(ownership, "unassigned") + ", role=" + empty(role, "general") +
                ". Ownership hooks are live; driving remains reserved for the vehicle runtime.";
    }

    static String actionVerb(String type) {
        String t = AssetIntegrationDisciplineAuthority.canonicalType(type);
        if (AssetIntegrationDisciplineAuthority.TAXI_BOOTH.equals(t)) return "checks transit booth routing metadata.";
        if (isVehicleType(t) || AssetIntegrationDisciplineAuthority.ROAD_VEHICLE_STAGING_MARKER.equals(t)) return "inspects a parked vehicle.";
        if (AssetIntegrationDisciplineAuthority.PARKING_LOT_MARKER.equals(t)) return "inspects a vehicle set-down bay.";
        return "inspects a road-adjacent civic fixture.";
    }

    private static String value(String stock, String key) {
        if (stock == null || key == null) return "";
        for (String part : stock.split(";")) {
            int i = part.indexOf('=');
            if (i > 0 && part.substring(0, i).equals(key)) return part.substring(i + 1);
        }
        return "";
    }

    private static String empty(String s, String fallback) { return s == null || s.isBlank() ? fallback : s; }
    private static String safe(String s) { return s == null ? "" : s; }
    private static String pick(Random r, String... vals) {
        if (vals == null || vals.length == 0) return "Road transit fixture";
        if (r == null) return vals[0];
        return vals[Math.floorMod(r.nextInt(), vals.length)];
    }
}
