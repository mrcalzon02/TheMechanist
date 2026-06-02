package mechanist;

final class PlayerFacingInspectionTextSmoke {
    public static void main(String[] args) {
        String tile = PlayerFacingInspectionText.tile(
                "mechanist.runtime.ZoneTile",
                "registryKey=underhive.floor path=/srv/mechanist/worlds/debug.map"
        );

        if (tile.contains("mechanist.runtime.ZoneTile")) {
            throw new AssertionError("Inspection tile leaked runtime class name: " + tile);
        }
        if (tile.contains("/srv/mechanist/worlds/debug.map")) {
            throw new AssertionError("Inspection tile leaked filesystem path: " + tile);
        }
        if (!tile.contains("catalog")) {
            throw new AssertionError("Inspection tile did not remap registry wording: " + tile);
        }

        String actor = PlayerFacingInspectionText.actor(
                "Citizen",
                "targetZoneKey=1,2,3,4,5,false uuid=123e4567-e89b-12d3-a456-426614174000"
        );

        if (actor.contains("1,2,3,4,5,false") || actor.contains("123e4567")) {
            throw new AssertionError("Inspection actor leaked raw identifiers: " + actor);
        }

        String fallback = PlayerFacingInspectionText.fixture("", "");
        if (!fallback.equals("The fixture appears inactive.")) {
            throw new AssertionError("Fixture fallback wording changed unexpectedly: " + fallback);
        }
    }
}
