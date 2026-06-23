package mechanist;

import java.util.List;

/** Smoke for the Phase 18 production knowledge-source audit surface. */
final class Milestone03ProductionKnowledgeSourceAuditSmoke {
    public static void main(String[] args) {
        List<String> audit = ProductionKnowledgeSourceAuthority.definitionAuditLines();
        requireContains(audit, "owner=ProductionKnowledgeSourceAuthority", "knowledge source owner");
        requireContains(audit, "sources=player knowledge+selected machine doctrine+claimed facility doctrine", "source list");
        requireContains(audit, "effectiveKnowledgeUnion=true", "effective knowledge union");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-ID boundary");
        requireContains(audit, "Teach Machine requires the player to know the doctrine", "teach-machine boundary");
        requireContains(audit, "owner=ProductionFacilityKnowledgeAuthority", "facility knowledge owner");
        requireContains(audit, "another serviceable production station in the same claimed production room", "facility provider rule");
        requireContains(audit, "brokenStations=false", "broken station boundary");
        requireContains(audit, "unclaimedWorkspace=false", "unclaimed workspace boundary");
        requireContains(audit, "doctrine tier comes from the effective knowledge set", "knowledge quality contribution");
        requireContains(audit, "does not mutate recipes, grant knowledge, consume inputs, start production", "mutation boundary");
        requireContains(audit, "Milestone03ProductionKnowledgeSourceAuditSmoke", "guard reference");

        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.unlockedKnowledges.clear();
        game.unlockedKnowledges.add("Common Tools Patterns");
        game.baseObjects.clear();
        game.baseClaimed = true;
        game.claimedRoomId = 1;
        game.baseX = 10;
        game.baseY = 10;

        BaseObject selected = station("Selected Forge", 'f', 10, 10, 5);
        selected.machineKnowledge = "Serviceable Tools Patterns";
        BaseObject archive = station("Pattern Archive", 'l', 11, 10, 5);
        archive.machineKnowledge = "Fine Tools Patterns";
        game.baseObjects.add(selected);
        game.baseObjects.add(archive);

        ProductionKnowledgeSourceAuthority.KnowledgeSource player = ProductionKnowledgeSourceAuthority.evaluate(
                game, selected, "Common Tools Patterns");
        require(player.available() && player.playerSupplied(), "player knowledge should satisfy common doctrine");
        requireContains(player.lines(), "player", "player source label");

        ProductionKnowledgeSourceAuthority.KnowledgeSource machine = ProductionKnowledgeSourceAuthority.evaluate(
                game, selected, "Serviceable Tools Patterns");
        require(machine.available() && machine.machineSupplied(), "selected machine should supply installed doctrine");
        require(machine.effectiveKnowledge().contains("Serviceable Tools Patterns"),
                "machine doctrine should enter effective knowledge set");

        ProductionKnowledgeSourceAuthority.KnowledgeSource facility = ProductionKnowledgeSourceAuthority.evaluate(
                game, selected, "Fine Tools Patterns");
        require(facility.available() && facility.facilitySupplied(), "claimed facility should supply doctrine");
        require("Pattern Archive".equals(facility.facilityProvider()), "facility provider should be named");
        require(ProductionQualityTraceAuthority.evaluate(facility.effectiveKnowledge(), "Fine Tools Patterns",
                "Fine").doctrineTier() == 4, "facility doctrine should contribute Fine knowledge quality");

        archive.integrity = 0;
        ProductionKnowledgeSourceAuthority.KnowledgeSource broken = ProductionKnowledgeSourceAuthority.evaluate(
                game, selected, "Fine Tools Patterns");
        require(!broken.available(), "broken facility provider must not supply doctrine");
        if (game.timer != null) game.timer.stop();
    }

    private static BaseObject station(String name, char symbol, int x, int y, int integrity) {
        BaseObject station = new BaseObject(name, symbol, x, y, 0, 0);
        station.integrity = integrity;
        return station;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03ProductionKnowledgeSourceAuditSmoke() { }
}
