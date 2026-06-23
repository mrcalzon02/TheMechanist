package mechanist;

import java.util.List;

/** Resolves doctrine shared by another serviceable station in the claimed production room. */
final class ProductionFacilityKnowledgeAuthority {
    record FacilityKnowledge(boolean supplied, String providerName, List<String> lines) { }

    private ProductionFacilityKnowledgeAuthority() { }

    static FacilityKnowledge evaluate(GamePanel game, BaseObject selectedMachine, String requiredKnowledge) {
        String required = requiredKnowledge == null ? "" : requiredKnowledge.trim();
        if (required.isBlank()) return new FacilityKnowledge(false, "", List.of("Facility doctrine: no doctrine required."));
        if (game == null || selectedMachine == null || !game.baseClaimed || game.claimedRoomId < 0
                || !game.isInClaimedRoom(selectedMachine.x, selectedMachine.y)) {
            return new FacilityKnowledge(false, "", List.of("Facility doctrine: unavailable; no claimed production room governs the selected machine."));
        }

        int selectedRoom = game.world == null ? -1 : game.world.roomIdAt(selectedMachine.x, selectedMachine.y);
        for (BaseObject station : game.baseObjects) {
            if (station == null || station == selectedMachine || station.integrity <= 0
                    || !StaffingLaborBridgeAuthority.isMachineStation(station)) continue;
            boolean sameRoom = selectedRoom >= 0 && game.world != null
                    ? game.world.roomIdAt(station.x, station.y) == selectedRoom
                    : game.isInClaimedRoom(station.x, station.y);
            if (!sameRoom || station.machineKnowledge == null || !station.machineKnowledge.equalsIgnoreCase(required)) continue;
            String provider = station.name == null || station.name.isBlank() ? "serviceable room station" : station.name;
            return new FacilityKnowledge(true, provider, List.of(
                    "Facility doctrine: " + required + " supplied by " + provider + " in the claimed production room.",
                    "Facility knowledge rule: only another serviceable production station in the same claimed room can share doctrine."));
        }
        return new FacilityKnowledge(false, "", List.of(
                "Facility doctrine: no serviceable station in the claimed production room supplies " + required + "."));
    }
}
