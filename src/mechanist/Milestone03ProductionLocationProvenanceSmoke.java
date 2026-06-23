package mechanist;

import java.util.List;

/** Smoke for producing-room and claimed-facility provenance. */
final class Milestone03ProductionLocationProvenanceSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.baseClaimed = true;
        game.claimedRoomId = 7;
        game.baseX = 10;
        game.baseY = 10;
        BaseObject machine = new BaseObject("Test Forge", 'f', 10, 10, 0, 0);

        ProductionLocationAuthority.ProductionLocation location = ProductionLocationAuthority.evaluate(game, machine);
        require(location.roomLabel().contains("claimed production room 7"), "claimed room identity should be retained");
        require("Claimed room 7".equals(location.facilityLabel()), "claimed facility label should be retained");
        requireContains(location.lines(), "attached to the claimed production facility", "claimed location explanation");

        ProductionRecipe recipe = ProductionRecipe.create("Test Tool", Faction.HIVER, "Common",
                "Common Tool Patterns", "Test Forge");
        ItemProvenanceRecord made = ItemProvenanceRecord.produced(recipe, machine, game.world, 8,
                "Test Operator", null, null, null, null, null, location);
        requireContains(made.qualityContextLines(), "Producing room: claimed production room 7", "room provenance");
        requireContains(made.qualityContextLines(), "Producing facility: Claimed room 7", "facility provenance");
        ItemProvenanceRecord decoded = ItemProvenanceRecord.decode(made.encode());
        require(decoded != null && made.producingRoom.equals(decoded.producingRoom),
                "producing room should survive save encoding");
        require(made.producingFacility.equals(decoded.producingFacility),
                "producing facility should survive save encoding");
        ItemProvenanceRecord transferred = ItemProvenanceRecord.transferred(decoded, made.itemName, game.world, 9, "moved to storage");
        require(decoded.producingRoom.equals(transferred.producingRoom), "producing room should survive transfer");
        require(decoded.producingFacility.equals(transferred.producingFacility), "producing facility should survive transfer");

        game.baseClaimed = false;
        require("unclaimed world workspace".equals(ProductionLocationAuthority.evaluate(game, machine).facilityLabel()),
                "unclaimed workspace must not claim facility provenance");
        if (game.timer != null) game.timer.stop();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03ProductionLocationProvenanceSmoke() { }
}
