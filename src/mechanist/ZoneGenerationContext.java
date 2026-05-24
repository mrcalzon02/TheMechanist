package mechanist;

/**
 * Phase 3 spatial-generation scaffolding.
 *
 * This context is intentionally metadata-only for the first implementation pass.
 * It does not replace room, road, corridor, plaza, or parking placement yet.
 *
 * Purpose:
 * - classify zone spatial family,
 * - derive roaded/roadless behavior,
 * - define central-anchor intent,
 * - define edge-band policy,
 * - expose validation profile,
 * - preserve a minimum edge safety band for hive exterior wall systems.
 */
public final class ZoneGenerationContext {
    public static final int MIN_EDGE_SAFETY_BAND_TILES = 5;
    public static final double FEATURE_ACCESS_AREA_MULTIPLIER = 1.5;

    public enum ZoneFamily {
        ROADED_CIVIC,
        ROADED_NOBLE,
        ROADED_PDF_MILITARY,
        ROADED_MARKET_ADMIN,
        ROADLESS_SEWER,
        ROADLESS_UTILITY,
        ROADLESS_MAINTENANCE,
        ROADLESS_INDUSTRIAL_INTERIOR,
        ROADLESS_TUNNEL_WARREN,
        HYBRID_UNDERHIVE,
        HYBRID_RUINED_CIVIC,
        HYBRID_INDUSTRIAL_SURFACE,
        EDGE_EXTERIOR_WALL,
        SPECIAL_STORY_ZONE
    }

    public enum RoadMode {
        ROADS_ENABLED,
        ROADS_DISABLED,
        HYBRID_LIMITED_ROADS,
        SERVICE_ONLY,
        EXTERIOR_EDGE_ONLY
    }

    public enum AnchorType {
        CIVIC_PLAZA,
        NOBLE_COURT,
        MILITARY_PARADE_GROUND,
        MARKET_SQUARE,
        SEWER_JUNCTION_CHAMBER,
        UTILITY_NEXUS,
        PUMP_HALL,
        MAINTENANCE_HUB,
        UNDERHIVE_GATHERING_PIT,
        INDUSTRIAL_SERVICE_YARD,
        EXTERIOR_WALL_ACCESS_NODE,
        STORY_ANCHOR
    }

    public enum EdgeBandPolicy {
        STANDARD_PERIMETER_RESERVED,
        EXTERIOR_WALL_HEAVY,
        SEWER_UTILITY_EDGE,
        NOBLE_SECURED_EDGE,
        MILITARY_HARDENED_EDGE,
        UNDERHIVE_BREACHED_EDGE,
        EDGE_STORY_OVERRIDE,
        NO_EDGE_OVERRIDE_ALLOWED
    }

    public enum ValidationProfile {
        CIVIC_SURFACE_VALIDATION,
        NOBLE_SURFACE_VALIDATION,
        MILITARY_SURFACE_VALIDATION,
        ROADLESS_SEWER_VALIDATION,
        ROADLESS_UTILITY_VALIDATION,
        HYBRID_UNDERHIVE_VALIDATION,
        INDUSTRIAL_INTERIOR_VALIDATION,
        EXTERIOR_EDGE_VALIDATION,
        SPECIAL_STORY_VALIDATION
    }



    public enum RoomAdjacencyMode {
        CORRIDOR_LINKED,
        DIRECT_SHARED_WALL_ALLOWED,
        DIRECT_SHARED_WALL_WITH_DOOR_REQUIRED,
        DIRECT_SHARED_WALL_RESTRICTED,
        SPECIAL_OVERRIDE
    }

    public enum ConnectionValidationMode {
        ORTHOGONAL_ONLY,
        REQUIRE_TRAVERSABLE_RECEIVER,
        REQUIRE_CLEARANCE_BOTH_SIDES,
        DISALLOW_CORNER_CONTACT_ONLY,
        REQUIRE_PATHABLE_LINK,
        ALLOW_EXPLICIT_SPECIAL_OVERRIDE
    }

    public final ZoneFamily zoneFamily;
    public final RoadMode roadMode;
    public final AnchorType anchorType;
    public final EdgeBandPolicy edgeBandPolicy;
    public final ValidationProfile validationProfile;
    public final ConnectionValidationMode connectionValidationMode;
    public final RoomAdjacencyMode roomAdjacencyMode;
    public final int mapWidth;
    public final int mapHeight;
    public final int edgeBandTiles;
    public final Rect interiorGenerationBounds;

    private ZoneGenerationContext(
            ZoneFamily zoneFamily,
            RoadMode roadMode,
            AnchorType anchorType,
            EdgeBandPolicy edgeBandPolicy,
            ValidationProfile validationProfile,
            ConnectionValidationMode connectionValidationMode,
            RoomAdjacencyMode roomAdjacencyMode,
            int mapWidth,
            int mapHeight,
            int edgeBandTiles,
            Rect interiorGenerationBounds) {
        this.zoneFamily = zoneFamily;
        this.roadMode = roadMode;
        this.anchorType = anchorType;
        this.edgeBandPolicy = edgeBandPolicy;
        this.validationProfile = validationProfile;
        this.connectionValidationMode = connectionValidationMode;
        this.roomAdjacencyMode = roomAdjacencyMode;
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.edgeBandTiles = Math.max(MIN_EDGE_SAFETY_BAND_TILES, edgeBandTiles);
        this.interiorGenerationBounds = interiorGenerationBounds;
    }

    public static ZoneGenerationContext create(ZoneType zoneType, int mapWidth, int mapHeight) {
        ZoneFamily family = deriveZoneFamily(zoneType);
        int edgeBand = MIN_EDGE_SAFETY_BAND_TILES;
        return new ZoneGenerationContext(
                family,
                deriveRoadMode(family),
                deriveAnchorType(family),
                deriveEdgeBandPolicy(family),
                deriveValidationProfile(family),
                deriveConnectionValidationMode(family),
                deriveRoomAdjacencyMode(family),
                mapWidth,
                mapHeight,
                edgeBand,
                deriveInteriorBounds(mapWidth, mapHeight, edgeBand));
    }

    public static ZoneFamily deriveZoneFamily(ZoneType zoneType) {
        if (zoneType == null) {
            return ZoneFamily.ROADLESS_TUNNEL_WARREN;
        }

        String name = zoneType.name().toLowerCase();

        if (name.contains("sewer")) return ZoneFamily.ROADLESS_SEWER;
        if (name.contains("utility")) return ZoneFamily.ROADLESS_UTILITY;
        if (name.contains("maintenance")) return ZoneFamily.ROADLESS_MAINTENANCE;
        if (name.contains("noble")) return ZoneFamily.ROADED_NOBLE;
        if (name.contains("pdf") || name.contains("military") || name.contains("barracks")) return ZoneFamily.ROADED_PDF_MILITARY;
        if (name.contains("market") || name.contains("admin") || name.contains("civic")) return ZoneFamily.ROADED_MARKET_ADMIN;
        if (name.contains("underhive")) return ZoneFamily.HYBRID_UNDERHIVE;
        if (name.contains("industrial") || name.contains("factory") || name.contains("manufactorum")) return ZoneFamily.HYBRID_INDUSTRIAL_SURFACE;
        if (name.contains("exterior") || name.contains("wall")) return ZoneFamily.EDGE_EXTERIOR_WALL;
        if (name.contains("story") || name.contains("special")) return ZoneFamily.SPECIAL_STORY_ZONE;

        return ZoneFamily.ROADED_CIVIC;
    }

    public static RoadMode deriveRoadMode(ZoneFamily family) {
        switch (family) {
            case ROADED_CIVIC:
            case ROADED_NOBLE:
            case ROADED_PDF_MILITARY:
            case ROADED_MARKET_ADMIN:
                return RoadMode.ROADS_ENABLED;
            case ROADLESS_SEWER:
            case ROADLESS_UTILITY:
            case ROADLESS_MAINTENANCE:
            case ROADLESS_INDUSTRIAL_INTERIOR:
            case ROADLESS_TUNNEL_WARREN:
                return RoadMode.ROADS_DISABLED;
            case HYBRID_UNDERHIVE:
            case HYBRID_RUINED_CIVIC:
            case HYBRID_INDUSTRIAL_SURFACE:
                return RoadMode.HYBRID_LIMITED_ROADS;
            case EDGE_EXTERIOR_WALL:
                return RoadMode.EXTERIOR_EDGE_ONLY;
            case SPECIAL_STORY_ZONE:
            default:
                return RoadMode.SERVICE_ONLY;
        }
    }

    public static AnchorType deriveAnchorType(ZoneFamily family) {
        switch (family) {
            case ROADED_NOBLE:
                return AnchorType.NOBLE_COURT;
            case ROADED_PDF_MILITARY:
                return AnchorType.MILITARY_PARADE_GROUND;
            case ROADED_MARKET_ADMIN:
                return AnchorType.MARKET_SQUARE;
            case ROADLESS_SEWER:
                return AnchorType.SEWER_JUNCTION_CHAMBER;
            case ROADLESS_UTILITY:
                return AnchorType.UTILITY_NEXUS;
            case ROADLESS_MAINTENANCE:
                return AnchorType.MAINTENANCE_HUB;
            case ROADLESS_INDUSTRIAL_INTERIOR:
            case HYBRID_INDUSTRIAL_SURFACE:
                return AnchorType.INDUSTRIAL_SERVICE_YARD;
            case HYBRID_UNDERHIVE:
            case ROADLESS_TUNNEL_WARREN:
                return AnchorType.UNDERHIVE_GATHERING_PIT;
            case EDGE_EXTERIOR_WALL:
                return AnchorType.EXTERIOR_WALL_ACCESS_NODE;
            case SPECIAL_STORY_ZONE:
                return AnchorType.STORY_ANCHOR;
            case ROADED_CIVIC:
            case HYBRID_RUINED_CIVIC:
            default:
                return AnchorType.CIVIC_PLAZA;
        }
    }

    public static EdgeBandPolicy deriveEdgeBandPolicy(ZoneFamily family) {
        switch (family) {
            case EDGE_EXTERIOR_WALL:
                return EdgeBandPolicy.EXTERIOR_WALL_HEAVY;
            case ROADLESS_SEWER:
            case ROADLESS_UTILITY:
            case ROADLESS_MAINTENANCE:
                return EdgeBandPolicy.SEWER_UTILITY_EDGE;
            case ROADED_NOBLE:
                return EdgeBandPolicy.NOBLE_SECURED_EDGE;
            case ROADED_PDF_MILITARY:
                return EdgeBandPolicy.MILITARY_HARDENED_EDGE;
            case HYBRID_UNDERHIVE:
                return EdgeBandPolicy.UNDERHIVE_BREACHED_EDGE;
            case SPECIAL_STORY_ZONE:
                return EdgeBandPolicy.EDGE_STORY_OVERRIDE;
            default:
                return EdgeBandPolicy.STANDARD_PERIMETER_RESERVED;
        }
    }

    public static ValidationProfile deriveValidationProfile(ZoneFamily family) {
        switch (family) {
            case ROADED_NOBLE:
                return ValidationProfile.NOBLE_SURFACE_VALIDATION;
            case ROADED_PDF_MILITARY:
                return ValidationProfile.MILITARY_SURFACE_VALIDATION;
            case ROADLESS_SEWER:
                return ValidationProfile.ROADLESS_SEWER_VALIDATION;
            case ROADLESS_UTILITY:
            case ROADLESS_MAINTENANCE:
                return ValidationProfile.ROADLESS_UTILITY_VALIDATION;
            case HYBRID_UNDERHIVE:
            case ROADLESS_TUNNEL_WARREN:
                return ValidationProfile.HYBRID_UNDERHIVE_VALIDATION;
            case ROADLESS_INDUSTRIAL_INTERIOR:
            case HYBRID_INDUSTRIAL_SURFACE:
                return ValidationProfile.INDUSTRIAL_INTERIOR_VALIDATION;
            case EDGE_EXTERIOR_WALL:
                return ValidationProfile.EXTERIOR_EDGE_VALIDATION;
            case SPECIAL_STORY_ZONE:
                return ValidationProfile.SPECIAL_STORY_VALIDATION;
            default:
                return ValidationProfile.CIVIC_SURFACE_VALIDATION;
        }
    }



    public static RoomAdjacencyMode deriveRoomAdjacencyMode(ZoneFamily family) {
        switch (family) {
            case ROADED_NOBLE:
            case ROADED_PDF_MILITARY:
                return RoomAdjacencyMode.DIRECT_SHARED_WALL_RESTRICTED;
            case ROADLESS_SEWER:
            case ROADLESS_UTILITY:
            case ROADLESS_MAINTENANCE:
                return RoomAdjacencyMode.CORRIDOR_LINKED;
            case SPECIAL_STORY_ZONE:
                return RoomAdjacencyMode.SPECIAL_OVERRIDE;
            default:
                return RoomAdjacencyMode.DIRECT_SHARED_WALL_WITH_DOOR_REQUIRED;
        }
    }

    public static ConnectionValidationMode deriveConnectionValidationMode(ZoneFamily family) {
        return ConnectionValidationMode.ORTHOGONAL_ONLY;
    }

    public static Rect deriveInteriorBounds(int mapWidth, int mapHeight, int edgeBandTiles) {
        int band = Math.max(MIN_EDGE_SAFETY_BAND_TILES, edgeBandTiles);
        int x = band;
        int y = band;
        int width = Math.max(0, mapWidth - band * 2);
        int height = Math.max(0, mapHeight - band * 2);
        return new Rect(x, y, width, height);
    }


    public boolean hasUsableInteriorBounds() {
        return interiorGenerationBounds != null
                && interiorGenerationBounds.width > 0
                && interiorGenerationBounds.height > 0;
    }


    /**
     * Returns true only when an ordinary non-edge-authorized placement rectangle is
     * fully inside the protected interior generation bounds.
     */
    public boolean allowsOrdinaryPlacement(Rect candidate) {
        return candidate != null
                && hasUsableInteriorBounds()
                && interiorGenerationBounds.contains(candidate);
    }


    public boolean allowsDirectSharedWallRoomAdjacency() {
        return roomAdjacencyMode == RoomAdjacencyMode.DIRECT_SHARED_WALL_ALLOWED
                || roomAdjacencyMode == RoomAdjacencyMode.DIRECT_SHARED_WALL_WITH_DOOR_REQUIRED
                || roomAdjacencyMode == RoomAdjacencyMode.SPECIAL_OVERRIDE;
    }

    public String toDebugString() {
        return "ZoneGenerationContext{" +
                "zoneFamily=" + zoneFamily +
                ", roadMode=" + roadMode +
                ", anchorType=" + anchorType +
                ", edgeBandPolicy=" + edgeBandPolicy +
                ", validationProfile=" + validationProfile +
                ", mapWidth=" + mapWidth +
                ", mapHeight=" + mapHeight +
                ", edgeBandTiles=" + edgeBandTiles +
                ", interiorGenerationBounds=" + interiorGenerationBounds +
                '}';
    }


    public static Rect rect(int x, int y, int width, int height) {
        return new Rect(x, y, width, height);
    }

    public static final class Rect {
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        public Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }


        public boolean contains(Rect other) {
            if (other == null) return false;
            return other.x >= this.x
                    && other.y >= this.y
                    && other.x + other.width <= this.x + this.width
                    && other.y + other.height <= this.y + this.height;
        }

        public boolean intersects(Rect other) {
            if (other == null) return false;
            return this.x < other.x + other.width
                    && this.x + this.width > other.x
                    && this.y < other.y + other.height
                    && this.y + this.height > other.y;
        }

        @Override
        public String toString() {
            return "Rect{" +
                    "x=" + x +
                    ", y=" + y +
                    ", width=" + width +
                    ", height=" + height +
                    '}';
        }
    }
}
