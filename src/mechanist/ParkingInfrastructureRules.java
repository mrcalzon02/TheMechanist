package mechanist;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Faction-aware vehicle lot and parking infrastructure rules.
 *
 * This is a generator contract, not a renderer.  Road/room generation should ask
 * this class what parking infrastructure a faction or residential block needs,
 * then reserve the resulting footprint through ZonePlacementValidator before
 * ordinary rooms expand into the area.
 */
final class ParkingInfrastructureRules {
    enum ParkingUse {
        RESIDENT_PERSONAL,
        FACTION_MOTOR_POOL,
        SECURITY_RESPONSE,
        INDUSTRIAL_LOADING,
        LIGHT_COMMERCIAL_LOADING,
        NOBLE_GARAGE,
        SCAVENGER_LOT,
        GANG_HIDEOUT_LOT,
        CIVIC_SERVICE_LOT,
        NONE
    }

    enum LotType {
        CURBSIDE_SPACES,
        SMALL_SURFACE_LOT,
        ENCLOSED_GARAGE,
        SECURED_MOTOR_POOL,
        LOADING_YARD,
        NOBLE_PRIVATE_GARAGE,
        SCRAP_VEHICLE_YARD,
        UNDERHAB_PERSONAL_LOT
    }

    enum AccessControl {
        PUBLIC,
        RESIDENT_ONLY,
        FACTION_SECURED,
        NOBLE_SECURED,
        GANG_CONTROLLED,
        SERVICE_ACCESS
    }

    static final class ParkingProfile {
        final ParkingUse use;
        final LotType lotType;
        final AccessControl accessControl;
        final int minimumSpaces;
        final int serviceVehicleSpaces;
        final int personalVehicleSpaces;
        final boolean requiresRoadFrontage;
        final boolean requiresLoadingDock;
        final boolean mayUseStreetParking;
        final String label;

        ParkingProfile(ParkingUse use, LotType lotType, AccessControl accessControl,
                       int minimumSpaces, int serviceVehicleSpaces, int personalVehicleSpaces,
                       boolean requiresRoadFrontage, boolean requiresLoadingDock, boolean mayUseStreetParking,
                       String label) {
            this.use = use == null ? ParkingUse.NONE : use;
            this.lotType = lotType == null ? LotType.CURBSIDE_SPACES : lotType;
            this.accessControl = accessControl == null ? AccessControl.PUBLIC : accessControl;
            this.minimumSpaces = Math.max(0, minimumSpaces);
            this.serviceVehicleSpaces = Math.max(0, serviceVehicleSpaces);
            this.personalVehicleSpaces = Math.max(0, personalVehicleSpaces);
            this.requiresRoadFrontage = requiresRoadFrontage;
            this.requiresLoadingDock = requiresLoadingDock;
            this.mayUseStreetParking = mayUseStreetParking;
            this.label = label == null ? this.use.name() : label;
        }

        int totalSpaces() {
            return Math.max(minimumSpaces, serviceVehicleSpaces + personalVehicleSpaces);
        }
    }

    static final class ParkingLotPlan {
        final ParkingProfile profile;
        final Rectangle footprint;
        final ArrayList<RoadInfrastructureTileRules.SpecialPurpose> requiredSpecialTiles;
        final String reservationLabel;

        ParkingLotPlan(ParkingProfile profile, Rectangle footprint,
                       List<RoadInfrastructureTileRules.SpecialPurpose> requiredSpecialTiles,
                       String reservationLabel) {
            this.profile = profile;
            this.footprint = footprint == null ? new Rectangle() : new Rectangle(footprint);
            this.requiredSpecialTiles = new ArrayList<>(requiredSpecialTiles == null ? List.of() : requiredSpecialTiles);
            this.reservationLabel = reservationLabel == null ? "parking" : reservationLabel;
        }

        List<RoadInfrastructureTileRules.SpecialPurpose> specialTiles() {
            return Collections.unmodifiableList(requiredSpecialTiles);
        }

        void reserveWith(ZonePlacementValidator validator) {
            if (validator == null || footprint.width <= 0 || footprint.height <= 0) return;
            validator.reserve(footprint, ZonePlacementValidator.ReservationKind.FACTION_RESERVED_ROOM, reservationLabel);
        }
    }

    private ParkingInfrastructureRules() {}

    static ParkingProfile profileForFaction(Faction faction, boolean residentialContext) {
        if (faction == null || faction == Faction.NONE) {
            return residentialContext
                    ? new ParkingProfile(ParkingUse.RESIDENT_PERSONAL, LotType.CURBSIDE_SPACES, AccessControl.PUBLIC, 4, 0, 4, true, false, true, "neutral resident parking")
                    : new ParkingProfile(ParkingUse.NONE, LotType.CURBSIDE_SPACES, AccessControl.PUBLIC, 0, 0, 0, false, false, false, "no parking");
        }
        switch (faction) {
            case NOBLE, NOBLE_HOUSE_VARN, NOBLE_HOUSE_KASTOR, NOBLE_HOUSE_MORVAIN, NOBLE_HOUSE_CYRA, NOBLE_HOUSE_DRAKE, NOBLE_HOUSE_TOLL, NOBLE_HOUSE_OSSUARY:
                return new ParkingProfile(ParkingUse.NOBLE_GARAGE, LotType.NOBLE_PRIVATE_GARAGE, AccessControl.NOBLE_SECURED, 10, 2, 8, true, false, false, "noble private garage");
            case MECHANICUS, MECHANIST_COLLEGIA, MECHANICUS_CLOISTER_RED, MECHANICUS_CLOISTER_RUST, MECHANICUS_CLOISTER_VOID:
                return new ParkingProfile(ParkingUse.FACTION_MOTOR_POOL, LotType.SECURED_MOTOR_POOL, AccessControl.FACTION_SECURED, 8, 6, 2, true, true, false, "forge motor pool");
            case ARBITES, CIVIC_WARDENS, IMPERIAL_GUARD:
                return new ParkingProfile(ParkingUse.SECURITY_RESPONSE, LotType.SECURED_MOTOR_POOL, AccessControl.FACTION_SECURED, 12, 10, 2, true, false, false, "security response motor pool");
            case ADMINISTRATUM, CIVIC_LEDGER_OFFICE, INN, MINISTORUM, SORORITAS:
                return new ParkingProfile(ParkingUse.CIVIC_SERVICE_LOT, LotType.SMALL_SURFACE_LOT, AccessControl.SERVICE_ACCESS, 6, 2, 4, true, false, true, "civic service parking");
            case HIVER, HIVER_BLOCK_AUREL, HIVER_BLOCK_MARROW, HIVER_BLOCK_SUMPLEDGER:
                return new ParkingProfile(ParkingUse.RESIDENT_PERSONAL, residentialContext ? LotType.UNDERHAB_PERSONAL_LOT : LotType.CURBSIDE_SPACES, AccessControl.RESIDENT_ONLY, 6, 0, 6, true, false, true, "resident personal parking");
            case BANDIT, GANGER_IRON_RATS, GANGER_BLACK_SUMP, GANGER_CANDLE_JACKS, GANGER_RED_GRIN, GANGER_CHAIN_SAINTS, GANGER_ASH_MARKET, GANGER_WIRE_WOLVES, GANGER_DROWNED_9TH:
                return new ParkingProfile(ParkingUse.GANG_HIDEOUT_LOT, LotType.SCRAP_VEHICLE_YARD, AccessControl.GANG_CONTROLLED, 5, 1, 4, true, false, false, "gang vehicle yard");
            case SCAVENGER, MUTANT:
                return new ParkingProfile(ParkingUse.SCAVENGER_LOT, LotType.SCRAP_VEHICLE_YARD, AccessControl.PUBLIC, 3, 1, 2, true, false, false, "scrap vehicle lot");
            default:
                return residentialContext
                        ? new ParkingProfile(ParkingUse.RESIDENT_PERSONAL, LotType.CURBSIDE_SPACES, AccessControl.RESIDENT_ONLY, 4, 0, 4, true, false, true, "resident parking")
                        : new ParkingProfile(ParkingUse.CIVIC_SERVICE_LOT, LotType.SMALL_SURFACE_LOT, AccessControl.PUBLIC, 3, 1, 2, true, false, true, "general parking");
        }
    }

    static ParkingLotPlan planLot(ParkingProfile profile, Rectangle roadFrontage, int preferredDepthTiles) {
        ParkingProfile safe = profile == null ? profileForFaction(Faction.NONE, true) : profile;
        Rectangle base = roadFrontage == null ? new Rectangle(0, 0, 18, 12) : new Rectangle(roadFrontage);
        int spaces = Math.max(1, safe.totalSpaces());
        int width = Math.max(base.width, Math.min(64, 6 + spaces * 3));
        int height = Math.max(8, preferredDepthTiles <= 0 ? 12 : preferredDepthTiles);
        Rectangle footprint = new Rectangle(base.x, base.y, width, height);
        ArrayList<RoadInfrastructureTileRules.SpecialPurpose> specials = new ArrayList<>();
        if (safe.requiresLoadingDock) specials.add(RoadInfrastructureTileRules.SpecialPurpose.INDUSTRIAL_LOADING_DOCK);
        if (safe.use == ParkingUse.LIGHT_COMMERCIAL_LOADING) specials.add(RoadInfrastructureTileRules.SpecialPurpose.LIGHT_COMMERCIAL_LOADING_DOCK);
        if (safe.personalVehicleSpaces > 0) specials.add(RoadInfrastructureTileRules.SpecialPurpose.PARKING_SPACE);
        if (safe.lotType == LotType.NOBLE_PRIVATE_GARAGE) specials.add(RoadInfrastructureTileRules.SpecialPurpose.SPECIAL_PARKING);
        return new ParkingLotPlan(safe, footprint, specials, safe.label);
    }

    static boolean shouldPlaceInsideZoneInterior(ParkingProfile profile) {
        if (profile == null) return true;
        return profile.accessControl != AccessControl.PUBLIC || profile.lotType == LotType.NOBLE_PRIVATE_GARAGE || profile.lotType == LotType.SECURED_MOTOR_POOL;
    }
}
