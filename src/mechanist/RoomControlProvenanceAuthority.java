package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Player-facing bridge from generated room origin to construction-plan parity and acquisition sources. */
final class RoomControlProvenanceAuthority {
    private RoomControlProvenanceAuthority() { }

    static List<String> inspectionLines(GamePanel game, int roomId) {
        ArrayList<String> lines = new ArrayList<>();
        if (game == null || game.world == null || roomId < 0 || roomId >= game.world.rooms.size()) return lines;
        RoomProfile profile = game.world.roomProfile(roomId);
        if (profile == null) return lines;

        RoomConstructionParityAuthority.RoomParityEntry parity =
                RoomConstructionParityAuthority.liveEntry(profile, game.world.zoneType);
        Faction origin = FactionIdentityAuthority.strategicFamily(profile.faction);
        String zone = game.world.zoneType == null ? "this zone" : game.world.zoneType.label;
        lines.add(origin == Faction.NONE
                ? "Room origin: local independent construction for " + zone + "."
                : "Room origin: built to " + origin.label + " standards for " + zone + ".");

        String status = sentence(parity.playerAcquisitionStatus());
        if ("unmapped".equalsIgnoreCase(parity.matchingBlueprint())) {
            boolean exception = parity.playerAcquisitionStatus().toLowerCase(java.util.Locale.ROOT).contains("non-acquirable");
            lines.add((exception ? "Construction plan: no ordinary blueprint is offered. "
                    : "Construction plan: no exact blueprint is listed. ") + status);
        } else {
            lines.add("Construction plan: " + parity.matchingBlueprint() + ". " + status);
        }
        lines.addAll(RoomBlueprintVendorGuidanceAuthority.forRoom(game, roomId).lines());
        return lines;
    }

    private static String sentence(String value) {
        String text = value == null ? "Unknown acquisition path" : value.trim();
        if (text.isBlank()) text = "Unknown acquisition path";
        text = Character.toUpperCase(text.charAt(0)) + text.substring(1);
        char last = text.charAt(text.length() - 1);
        return last == '.' || last == '!' || last == '?' ? text : text + ".";
    }
}
