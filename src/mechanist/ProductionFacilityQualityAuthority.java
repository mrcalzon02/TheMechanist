package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Resolves the quality ceiling contributed by the claimed production room. */
final class ProductionFacilityQualityAuthority {
    record FacilityQuality(boolean active, int tier, String quality, int serviceableStations, List<String> lines) { }

    private ProductionFacilityQualityAuthority() { }

    static FacilityQuality evaluate(GamePanel game, BaseObject machine) {
        if (game == null || machine == null || !game.baseClaimed || game.claimedRoomId < 0) {
            return new FacilityQuality(false, QualityAuthorityApi.UNLIMITED_TIER, "open", 0,
                    List.of("Facility quality: open; no claimed production room is governing this manual Craft action."));
        }
        if (!game.isInClaimedRoom(machine.x, machine.y)) {
            return new FacilityQuality(false, QualityAuthorityApi.UNLIMITED_TIER, "open", 0,
                    List.of("Facility quality: open; the selected machine is outside the claimed production room."));
        }

        int machineRoom = game.world == null ? -1 : game.world.roomIdAt(machine.x, machine.y);
        int serviceable = 0;
        for (BaseObject object : game.baseObjects) {
            if (object == null || !StaffingLaborBridgeAuthority.isMachineStation(object) || object.integrity <= 0) continue;
            boolean sameRoom = machineRoom >= 0 && game.world != null
                    ? game.world.roomIdAt(object.x, object.y) == machineRoom
                    : game.isInClaimedRoom(object.x, object.y);
            if (sameRoom) serviceable++;
        }

        int tier = tierForStations(serviceable);
        String quality = QualityAuthorityApi.qualityName(tier);
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Facility quality cap: " + quality + " from " + serviceable + " serviceable production station" + (serviceable == 1 ? "" : "s") + " in the claimed room.");
        lines.add("Facility rule: broken stations do not support the room ceiling; additional serviceable stations can raise it through Masterwork.");
        return new FacilityQuality(true, tier, quality, serviceable, List.copyOf(lines));
    }

    static int tierForStations(int stations) {
        if (stations <= 1) return 2;
        if (stations <= 3) return 3;
        if (stations <= 5) return 4;
        return 5;
    }
}
