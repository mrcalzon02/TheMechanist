package mechanist;

/** Smoke for population origin changing category demand and allocated vendor stock. */
final class Milestone04PopulationOriginDemandSmoke {
    public static void main(String[] args) {
        PopulationMarketPressureAuthority.Profile barracks = profile("barracks duty roster",
                "Guard Barracks", Faction.IMPERIAL_GUARD);
        PopulationMarketPressureAuthority.Profile labor = profile("contract labor roster",
                "Industrial Worker Dormitory", Faction.MECHANIST_COLLEGIA);
        PopulationMarketPressureAuthority.Profile noble = profile("household servant ledger",
                "Noble Household", Faction.NOBLE);
        PopulationMarketPressureAuthority.Profile displaced = profile("displaced population roster",
                "Relief Shelter", Faction.HIVER);
        PopulationMarketPressureAuthority.Profile ordinary = profile("local hab work roster",
                "Hab Apartment", Faction.HIVER);

        require(barracks.pressureFor("Stub cartridge box", "ammo").demandUnits()
                        > labor.pressureFor("Stub cartridge box", "ammo").demandUnits(),
                "barracks population should create more ammunition demand than equal-sized labor housing");
        require(labor.pressureFor("Tool bundle", "tool").demandUnits()
                        > ordinary.pressureFor("Tool bundle", "tool").demandUnits(),
                "contract labor should create more tool demand than equal-sized ordinary housing");
        require(noble.pressureFor("Noble preserved delicacy", "food/luxury").demandUnits() >= 40,
                "noble households should create explicit luxury demand");
        require(displaced.pressureFor("Bandage roll", "medical").demandUnits()
                        > ordinary.pressureFor("Bandage roll", "medical").demandUnits(),
                "displaced population should create more medical demand than equal-sized ordinary housing");

        TraderSession barracksTrader = traderFor("barracks duty roster", "Guard Barracks", Faction.IMPERIAL_GUARD);
        TraderSession laborTrader = traderFor("contract labor roster", "Industrial Worker Dormitory", Faction.MECHANIST_COLLEGIA);
        TraderSession nobleTrader = traderFor("household servant ledger", "Noble Household", Faction.NOBLE);
        require(hasOffer(barracksTrader, "Stub cartridge box"),
                "barracks demand should allocate ammunition fallback stock");
        require(hasOffer(laborTrader, "Tool bundle"),
                "contract-labor demand should allocate tool fallback stock");
        require(hasOffer(nobleTrader, "Noble preserved delicacy"),
                "noble-household demand should allocate luxury fallback stock");
        requireContains(barracksTrader.populationPressure.contextLines().toString(),
                "security and custody rosters", "barracks demand identity");
        requireContains(laborTrader.populationPressure.contextLines().toString(),
                "industrial and transport labor", "labor demand identity");
        requireContains(nobleTrader.populationPressure.contextLines().toString(),
                "noble households", "noble demand identity");

        require("custody population ledger".equals(PersonnelPopulationApi.sourceKindFor(
                        "precinct detention holding cell block", ZoneType.ARBITES_PRECINCT_EDGE, Faction.CIVIC_WARDENS)),
                "custody rooms should receive a custody population source kind");
        require("pilgrim lodging roster".equals(PersonnelPopulationApi.sourceKindFor(
                        "pilgrim shrine hostel", ZoneType.NEUTRAL_CIVILIAN_FLOOR, Faction.HIVER)),
                "pilgrim rooms should receive a pilgrim population source kind");
        require("displaced population roster".equals(PersonnelPopulationApi.sourceKindFor(
                        "disaster refugee relief shelter", ZoneType.NEUTRAL_CIVILIAN_FLOOR, Faction.HIVER)),
                "relief rooms should receive a displaced population source kind");
    }

    private static PopulationMarketPressureAuthority.Profile profile(String sourceKind, String room,
                                                                      Faction faction) {
        TraderSession trader = trader();
        return PopulationMarketPressureAuthority.apply(trader, world(sourceKind, room, faction), faction, 22);
    }

    private static TraderSession traderFor(String sourceKind, String room, Faction faction) {
        TraderSession trader = trader();
        PopulationMarketPressureAuthority.apply(trader, world(sourceKind, room, faction), faction, 22);
        return trader;
    }

    private static World world(String sourceKind, String room, Faction faction) {
        World world = new World(91004L + sourceKind.hashCode(), 30, 30);
        RoomPopulationLedger ledger = new RoomPopulationLedger();
        ledger.id = "pop.origin.test";
        ledger.roomId = 0;
        ledger.roomName = room;
        ledger.sourceKind = sourceKind;
        ledger.sourceLabel = faction.label + " " + sourceKind;
        ledger.faction = faction;
        ledger.capacity = 40;
        ledger.available = 30;
        ledger.assigned = 10;
        world.roomPopulationLedgers.add(ledger);
        return world;
    }

    private static TraderSession trader() {
        TraderSession trader = new TraderSession();
        trader.name = "Origin Demand Counter";
        trader.archetype = "local trader";
        return trader;
    }

    private static boolean hasOffer(TraderSession trader, String wanted) {
        for (TradeOffer offer : trader.offers) {
            if (offer != null && ItemQuality.namesMatch(offer.name, wanted)) return true;
        }
        return false;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private Milestone04PopulationOriginDemandSmoke() { }
}
