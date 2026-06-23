package mechanist;

import java.util.List;

/** Resolves the producing room and facility identity for forecast and provenance. */
final class ProductionLocationAuthority {
    record ProductionLocation(String roomLabel, String facilityLabel, List<String> lines) { }

    private ProductionLocationAuthority() { }

    static ProductionLocation evaluate(GamePanel game, BaseObject machine) {
        if (machine == null) {
            return new ProductionLocation("manual workspace", "unassigned workspace",
                    List.of("Production location: manual workspace / unassigned workspace."));
        }
        int roomId = game == null || game.world == null ? -1 : game.world.roomIdAt(machine.x, machine.y);
        String room = roomLabel(game, roomId);
        boolean claimed = game != null && game.baseClaimed && game.claimedRoomId >= 0
                && game.isInClaimedRoom(machine.x, machine.y);
        String facility = claimed ? game.baseDisplayName() : "unclaimed world workspace";
        return new ProductionLocation(room, facility, List.of(
                "Production location: " + room + " / " + facility + ".",
                claimed ? "Location provenance: this run is attached to the claimed production facility."
                        : "Location provenance: this run is not attached to a claimed production facility."));
    }

    private static String roomLabel(GamePanel game, int roomId) {
        if (game != null && game.world != null && roomId >= 0) {
            RoomProfile profile = game.world.roomProfile(roomId);
            if (profile != null && profile.name != null && !profile.name.isBlank()) {
                return profile.name + " (room " + roomId + ")";
            }
            return "room " + roomId;
        }
        if (game != null && game.baseClaimed && game.claimedRoomId >= 0) {
            return "claimed production room " + game.claimedRoomId;
        }
        return "unmapped workspace";
    }
}
