package mechanist;

import java.util.ArrayList;
import java.util.Random;

/** Smoke for local workforce-gated faction production and finite trader exports. */
final class Milestone04FactionSiteWorkforceMarketSmoke {
    public static void main(String[] args) {
        World world = new World(92004L, 30, 30);
        world.sectorX = 1;
        world.sectorY = 1;
        world.zoneX = 2;
        world.zoneY = 2;
        world.floor = 4;
        RoomPopulationLedger ledger = new RoomPopulationLedger();
        ledger.id = "pop.forge.workforce";
        ledger.roomId = 0;
        ledger.roomName = "Forge Worker Dormitory";
        ledger.faction = Faction.MECHANIST_COLLEGIA;
        ledger.sourceKind = "contract labor roster";
        ledger.capacity = 8;
        ledger.available = 8;
        ledger.assigned = 0;
        world.roomPopulationLedgers.add(ledger);

        NpcFactionSite site = NpcFactionSite.create("Local Forge Site", Faction.MECHANIST_COLLEGIA,
                "forge workshop", 1, 1, 2, 2, 4, "Machine part", "Tool bundle", "Scrap-Forging Doctrine");
        site.stock = 2;
        FactionSiteWorkforceAuthority.Status unstaffed = FactionSiteWorkforceAuthority.sync(site, world);
        require(unstaffed.locallyBacked() && unstaffed.effectiveWorkers() == 0 && site.workers == 0,
                "matching unassigned local rosters should pause the site workforce");
        require(!site.produceHour(GamePanel.TURNS_PER_HOUR, new Random(1)),
                "zero-worker faction site must not produce stock");
        require(site.exportSample(new Random(1)).isEmpty() && site.stock == 2,
                "unstaffed faction site must not export or consume stock");

        TraderSession unavailable = new TraderSession();
        unavailable.name = "Unstaffed Forge Counter";
        unavailable.sourceWorkforceSummary = unstaffed.playerLine();
        TraderTradeActionAuthority.attachNpcSiteStock(unavailable, site, new Random(2), world, 10);
        requireContains(unavailable.supplyChainSummary, "site production is unstaffed",
                "unstaffed shelf reason");
        requireContains(TradeReadabilityAuthority.marketContext(unavailable,
                        Faction.MECHANIST_COLLEGIA.label, 0).toString(),
                "production and site exports are paused", "trade workforce readback");

        ledger.assigned = 3;
        ledger.available = 5;
        FactionSiteWorkforceAuthority.Status staffed = FactionSiteWorkforceAuthority.sync(site, world);
        require(staffed.effectiveWorkers() == 3 && site.workers == 3,
                "assigned local roster should become the site's effective workforce");
        site.stock = 0;
        require(site.produceHour(GamePanel.TURNS_PER_HOUR, new Random(3)) && site.stock == 2,
                "staffed site should produce a bounded hourly stock batch");

        ArrayList<String> first = site.exportSample(new Random(4));
        ArrayList<String> second = site.exportSample(new Random(5));
        ArrayList<String> depleted = site.exportSample(new Random(6));
        require(first.size() == 1 && second.size() == 1 && depleted.isEmpty() && site.stock == 0,
                "site exports should consume finite stock and stop when depleted");

        site.stock = 1;
        TraderSession stocked = new TraderSession();
        stocked.name = "Staffed Forge Counter";
        stocked.sourceWorkforceSummary = staffed.playerLine();
        TraderTradeActionAuthority.attachNpcSiteStock(stocked, site, new Random(7), world, 12);
        require(stocked.offers.size() == 1 && stocked.offers.get(0).provenance != null,
                "staffed site export should reach the shelf with provenance");
        require(site.stock == 0, "shelf export should decrement site stock exactly once");

        TraderSession empty = new TraderSession();
        empty.name = "Depleted Forge Counter";
        empty.sourceWorkforceSummary = staffed.playerLine();
        TraderTradeActionAuthority.attachNpcSiteStock(empty, site, new Random(8), world, 13);
        require(empty.offers.isEmpty(), "depleted site should contribute no shelf item");
        requireContains(empty.supplyChainSummary, "site export stock is depleted",
                "depleted shelf reason");

        NpcFactionSite parsed = NpcFactionSite.parse(site.saveLine());
        require(parsed != null && parsed.workers == 3 && parsed.stock == 0,
                "synced workforce and depleted stock should survive site persistence");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private Milestone04FactionSiteWorkforceMarketSmoke() { }
}
