package mechanist;

import java.util.List;

/** Smoke for Phase 18 room-stamp construction parity audit coverage. */
final class Milestone03RoomConstructionParityAuditSmoke {
    public static void main(String[] args) {
        List<RoomConstructionParityAuthority.RoomParityEntry> rooms = RoomConstructionParityAuthority.roomEntries();
        List<String> audit = RoomConstructionParityAuthority.definitionAuditLines();

        require(rooms.size() >= 120, "expected broad room profile parity sample");
        requireContains(audit, "owner=RoomConstructionParityAuthority", "room parity owner");
        requireContains(audit, "roomOwner=RoomProfile", "room owner");
        requireContains(audit, "blueprintOwner=BuildRecipe", "blueprint owner");
        requireContains(audit, "acquisitionOwner=BlueprintAcquisitionPathAuthority", "acquisition owner");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-ID boundary");
        requireContains(audit, "roomProfiles=" + rooms.size(), "room count");
        requireContains(audit, "factionRooms=", "faction room count");
        requireContains(audit, "playerAcquirableRooms=", "player acquisition count");
        requireContains(audit, "nonAcquirableRooms=", "non-acquirable count");
        requireContains(audit, "documentedExceptions=", "exception count");
        requireContains(audit, "mappedBlueprints=", "mapped blueprint count");
        requireContains(audit, "playerBlueprints=" + BuildRecipe.allBuildRecipes().size(), "player blueprint count");
        requireContains(audit, "faction usable", "faction-use wording");
        requireContains(audit, "non-acquirable civic/transition/utility exception", "non-acquirable boundary");
        requireContains(audit, "does not place rooms, unlock blueprints, mutate ownership", "future owner boundary");
        requireContains(audit, "Milestone03RoomConstructionParityAuditSmoke", "guard reference");

        RoomConstructionParityAuthority.RoomParityEntry precinct = requireRoom(rooms, "Civic Wardens Precinct Lobby");
        requireContains(precinct.playerAcquisitionStatus(), "non-acquirable exception", "precinct lobby exception");
        requireContains(precinct.exceptionNote(), "documented exception", "precinct exception note");

        RoomConstructionParityAuthority.RoomParityEntry component = requireRoom(rooms, "Component Warehouse");
        requireContains(component.playerAcquisitionStatus(), "restricted player path", "component warehouse acquisition");
        requireContains(component.matchingBlueprint(), "Storage Crate", "component warehouse blueprint");
        requireContains(component.factionUseStatus(), "Mechanist Collegia", "component warehouse faction use");

        RoomConstructionParityAuthority.RoomParityEntry closet = requireFirstContaining(rooms, "utility closet");
        requireContains(closet.playerAcquisitionStatus(), "non-acquirable exception", "closet acquisition exception");
        requireContains(closet.exceptionNote(), "documented exception", "closet exception note");

        requireContains(RoomConstructionParityAuthority.factionUseStatusFor(BuildRecipe.storage()), "faction usable", "storage faction use");
        requireContains(RoomConstructionParityAuthority.factionUseStatusFor(BuildRecipe.securitySensorMast()), "Civic Wardens", "sensor faction use");
        requireContains(RoomConstructionParityAuthority.factionUseStatusFor(BuildRecipe.clinicStall()), "faction usable", "clinic faction use");

        for (RoomConstructionParityAuthority.RoomParityEntry entry : rooms) {
            requireNotBlank(entry.roomName(), "room name");
            requireNotBlank(entry.sourceZone(), "source zone");
            requireNotBlank(entry.owningFaction(), "owning faction");
            requireNotBlank(entry.playerAcquisitionStatus(), "player acquisition status");
            requireNotBlank(entry.factionUseStatus(), "faction use status");
            requireNotBlank(entry.matchingBlueprint(), "matching blueprint");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Room construction parity audit leaked implementation text: " + line);
            }
        }
    }

    private static RoomConstructionParityAuthority.RoomParityEntry requireRoom(
            List<RoomConstructionParityAuthority.RoomParityEntry> rooms, String name) {
        for (RoomConstructionParityAuthority.RoomParityEntry entry : rooms) {
            if (entry != null && entry.roomName().equalsIgnoreCase(name)) return entry;
        }
        throw new AssertionError("Missing room parity entry for " + name);
    }

    private static RoomConstructionParityAuthority.RoomParityEntry requireFirstContaining(
            List<RoomConstructionParityAuthority.RoomParityEntry> rooms, String text) {
        String needle = text.toLowerCase(java.util.Locale.ROOT);
        for (RoomConstructionParityAuthority.RoomParityEntry entry : rooms) {
            if (entry != null && entry.roomName().toLowerCase(java.util.Locale.ROOT).contains(needle)) return entry;
        }
        throw new AssertionError("Missing room parity entry containing " + text);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireNotBlank(String value, String label) {
        require(value != null && !value.isBlank(), "expected nonblank " + label);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03RoomConstructionParityAuditSmoke() { }
}
