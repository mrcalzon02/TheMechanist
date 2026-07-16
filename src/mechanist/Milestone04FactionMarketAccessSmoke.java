package mechanist;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** End-to-end smoke for faction vendor identity, legality, readable access, and transaction blocking. */
final class Milestone04FactionMarketAccessSmoke {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        testFactionVendorIdentityAndNarcoticPreference();
        testStandingPermitMembershipAndSuspicionGates();
        testNobleContrabandAndDisputedGoods();
        testOrdinaryAgriculturalStock();
        testWorldEventRestrictionsAndLocalException();
        testBlockedPurchaseMutatesNothing();
        System.out.println("Milestone 04 faction market access smoke passed.");
    }

    private static void testFactionVendorIdentityAndNarcoticPreference() {
        NpcEntity dealer = trader("Sump Chem Factor", "Black-Market Trader", Faction.GANGER_BLACK_SUMP);
        TraderSession blackMarket = TraderTradeActionAuthority.createSessionForNpc(dealer, ZoneType.GANGER_TURF, new Random(17101L));
        require(blackMarket.marketFaction == Faction.GANGER_BLACK_SUMP, "trader session should retain exact faction identity");
        require("black-market".equals(blackMarket.marketCategory), "specialist vendor should retain category identity");
        requireOffer(blackMarket, "Street Stimm");
        requireOffer(blackMarket, "Grin Powder");
        requireOffer(blackMarket, "Night Milk");

        TradeOffer streetStimm = offer(blackMarket, "Street Stimm");
        FactionMarketAccessAuthority.Decision illicit = FactionMarketAccessAuthority.evaluate(blackMarket, streetStimm,
                context(null, Faction.NONE, 0, 0, 0, Set.of(), Set.of(), List.of()));
        require(illicit.allowed(), "neutral buyers should be able to use a visible black-market channel");
        requireContains(illicit.legalClass(), "black-market narcotic", "illicit narcotic classification");
        requireContains(illicit.consequence(), "chem economy", "illicit faction economic reason");
        require("[RISK] ".equals(illicit.rowTag()), "illicit goods should be visibly risk-tagged in offer rows");
    }

    private static void testStandingPermitMembershipAndSuspicionGates() {
        TraderSession armory = session(Faction.IMPERIAL_GUARD, "armory", "Concord Quartermaster", "Armory Trader");
        TradeOffer rifle = new TradeOffer("Light Rifle", "weapon/ranged", 36, "regulated military issue rifle");

        FactionMarketAccessAuthority.Decision neutral = FactionMarketAccessAuthority.evaluate(armory, rifle,
                context(null, Faction.NONE, 0, 0, 0, Set.of(), Set.of(), List.of()));
        require(!neutral.allowed() && "[LOCKED] ".equals(neutral.rowTag()), "neutral military issue should be visibly locked");
        requireContains(neutral.requirement(), "standing 10", "military standing requirement");

        FactionMarketAccessAuthority.Decision favorable = FactionMarketAccessAuthority.evaluate(armory, rifle,
                context(null, Faction.NONE, 10, 0, 0, Set.of(), Set.of(), List.of()));
        require(favorable.allowed(), "favorable standing should authorize controlled military issue");

        FactionMarketAccessAuthority.Decision member = FactionMarketAccessAuthority.evaluate(armory, rifle,
                context(null, Faction.IMPERIAL_GUARD, 0, 0, 0, Set.of(), Set.of(), List.of()));
        require(member.allowed(), "same-faction membership should authorize controlled issue");

        FactionMarketAccessAuthority.Decision permit = FactionMarketAccessAuthority.evaluate(armory, rifle,
                context(null, Faction.NONE, 0, 0, 0, Set.of(), Set.of("civic-access-permit-based-access"), List.of()));
        require(permit.allowed(), "permit-based access knowledge should authorize controlled issue");

        FactionMarketAccessAuthority.Decision inspected = FactionMarketAccessAuthority.evaluate(armory, rifle,
                context(null, Faction.NONE, 20, 0, 70, Set.of(), Set.of(), List.of()));
        require(!inspected.allowed(), "high suspicion should place non-members under an inspection hold");
        requireContains(inspected.requirement(), "suspicion 70", "suspicion gate readback");

        List<String> preview = TradeReadabilityAuthority.offerPreview(armory, rifle, 100, 0, 20, neutral);
        requireContains(preview.toString(), "Access blocked", "selected-offer access denial");
        requireContains(preview.toString(), "no transfer", "blocked inventory result");
    }

    private static void testNobleContrabandAndDisputedGoods() {
        TraderSession noble = session(Faction.NOBLE_HOUSE_VARN, "luxury", "House Varn Broker", "Luxury Broker Trader");
        TradeOffer obscura = new TradeOffer("Pearl Obscura", "chem/noble-luxury", 34, "private noble dream narcotic");
        FactionMarketAccessAuthority.Decision uninvited = FactionMarketAccessAuthority.evaluate(noble, obscura,
                context(null, Faction.NONE, 0, 0, 0, Set.of(), Set.of(), List.of()));
        require(!uninvited.allowed(), "uninvited neutral buyers should not receive private noble narcotics");
        requireContains(uninvited.legalClass(), "noble private medicine", "noble narcotic class");
        FactionMarketAccessAuthority.Decision patron = FactionMarketAccessAuthority.evaluate(noble, obscura,
                context(null, Faction.NONE, 0, 0, 0, Set.of(), Set.of(), List.of("Noble invitation")));
        require(patron.allowed(), "a carried noble invitation should authorize private broker access");

        TradeOffer draught = new TradeOffer("Black Sun Draught", "chem/rare-campaign", 850, "exceptional custody release");
        draught.draughtCustodyId = "draught.smoke";
        require(!FactionMarketAccessAuthority.evaluate(noble, draught,
                context(null, Faction.NONE, 10, 0, 0, Set.of(), Set.of(), List.of())).allowed(),
                "ordinary favorable standing must not make protected draughts ordinary stock");
        require(FactionMarketAccessAuthority.evaluate(noble, draught,
                context(null, Faction.NONE, 25, 0, 0, Set.of(), Set.of(), List.of())).allowed(),
                "trusted exceptional transfer should remain possible after explicit release");

        TraderSession legal = session(Faction.CIVIC_WARDENS, "provisions", "Warden Counter", "Provision Trader");
        TradeOffer witchsalt = new TradeOffer("Witchsalt", "chem/cult-warp", 28, "forbidden ritual contraband");
        FactionMarketAccessAuthority.Decision rejected = FactionMarketAccessAuthority.evaluate(legal, witchsalt,
                context(null, Faction.NONE, 20, 0, 0, Set.of(), Set.of(), List.of()));
        require(!rejected.allowed(), "legal vendor must refuse forbidden contraband despite standing");
        requireContains(rejected.requirement(), "black-market counter", "contraband alternative channel");

        TradeOffer counterfeit = new TradeOffer("Bandage roll", "medical", 4, "questionable clinic stock");
        counterfeit.provenance = new ItemProvenanceRecord();
        counterfeit.provenance.batchIssueTags = "counterfeit batch";
        FactionMarketAccessAuthority.Decision disclosed = FactionMarketAccessAuthority.evaluate(legal, counterfeit,
                context(null, Faction.NONE, 0, 0, 0, Set.of(), Set.of(), List.of()));
        require(disclosed.allowed(), "disputed goods can remain offered when the risk is disclosed");
        requireContains(disclosed.legalClass(), "counterfeit", "counterfeit classification");
        requireContains(disclosed.consequence(), "recorded risk", "counterfeit consequence");
    }

    private static void testOrdinaryAgriculturalStock() {
        TraderSession provisions = session(Faction.HIVER, "provisions", "Sump Provisioner", "Provision Trader");
        TradeOffer fertilizer = new TradeOffer("Fertilizer", "agriculture/input", 3,
                "sewer-processed growth feedstock lifted from the layer below");
        FactionMarketAccessAuthority.Decision decision = FactionMarketAccessAuthority.evaluate(provisions, fertilizer,
                context(null, Faction.NONE, 0, 0, 0, Set.of(), Set.of(), List.of()));
        require(decision.allowed(), "ordinary agricultural stock must not be mistaken for cult contraband");
        requireContains(decision.legalClass(), "legal open market", "agricultural stock classification");
    }

    private static void testWorldEventRestrictionsAndLocalException() {
        World quarantineWorld = new World(17102L, 32, 32);
        TopDownWorldEventRecord quarantine = TopDownWorldEventAuthority.scheduleCurated(quarantineWorld, "QUARANTINE", 0L);
        quarantine.status = "ACTIVE";
        TraderSession dispensary = session(Faction.CIVIC_WARDENS, "medical", "Warden Dispensary", "Medical Trader");
        TradeOffer narcotic = new TradeOffer("Street Stimm", "chem/ganger-combat", 14, "controlled medicine resale");
        FactionMarketAccessAuthority.Decision suspended = FactionMarketAccessAuthority.evaluate(dispensary, narcotic,
                context(quarantineWorld, Faction.NONE, 20, 0, 0, Set.of(), Set.of(), List.of()));
        require(!suspended.allowed(), "active quarantine should suspend legal controlled-medicine resale");
        requireContains(suspended.legalClass(), "event-restricted", "quarantine market class");
        requireContains(suspended.requirement(), "active quarantine", "quarantine denial reason");

        World banWorld = new World(17103L, 32, 32);
        TopDownWorldEventRecord exportBan = TopDownWorldEventAuthority.scheduleCurated(banWorld, "EXPORT_BAN", 0L);
        exportBan.status = "ACTIVE";
        TraderSession provisions = session(Faction.HIVER, "provisions", "Block Provisioner", "Provision Trader");
        TradeOffer ration = new TradeOffer("Emergency rations", "food", 4, "local faction stock");
        FactionMarketAccessAuthority.Decision localSale = FactionMarketAccessAuthority.evaluate(provisions, ration,
                context(banWorld, Faction.NONE, 0, 0, 0, Set.of(), Set.of(), List.of()));
        require(localSale.allowed(), "off-map export ban must not close local faction commerce");
        requireContains(localSale.eventNotice(), "local/internal vendor sale remains exempt", "local commerce event exception");
        requireContains(FactionMarketAccessAuthority.marketNotice(banWorld, 0L), "Local and internal faction commerce remains open",
                "market-wide event notice");
    }

    private static void testBlockedPurchaseMutatesNothing() throws Exception {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            TraderSession armory = session(Faction.IMPERIAL_GUARD, "armory", "Concord Quartermaster", "Armory Trader");
            armory.offers.add(new TradeOffer("Light Rifle", "weapon/ranged", 36, "regulated military issue rifle"));
            game.activeTraderSession = armory;
            game.selectedTradeOfferIndex = 0;
            game.carriedScript = 100;
            game.inventory.clear();
            game.turn = 7;
            int scriptBefore = game.carriedScript;
            int inventoryBefore = game.inventory.size();
            int turnBefore = game.turn;
            Method buy = GamePanel.class.getDeclaredMethod("buySelectedTradeOffer");
            buy.setAccessible(true);
            buy.invoke(game);
            require(game.carriedScript == scriptBefore, "blocked market access must not consume script");
            require(game.inventory.size() == inventoryBefore, "blocked market access must not add inventory");
            require(game.turn == turnBefore, "blocked market access must not advance time");
            requireContains(game.eventLog.toString(), "standing 10", "runtime purchase denial reason");
        } finally {
            if (game.timer != null) game.timer.stop();
        }
    }

    private static FactionMarketAccessAuthority.AccessContext context(World world, Faction playerFaction, int standing,
                                                                       int heat, int suspicion, Set<String> skills,
                                                                       Set<String> knowledges, List<String> items) {
        return new FactionMarketAccessAuthority.AccessContext(playerFaction, standing, heat, suspicion,
                skills, knowledges, items, world, 0L);
    }

    private static TraderSession session(Faction faction, String category, String name, String archetype) {
        TraderSession session = new TraderSession();
        session.marketFaction = faction;
        session.marketCategory = category;
        session.name = name;
        session.archetype = archetype;
        session.zoneLabel = "Smoke market";
        return session;
    }

    private static NpcEntity trader(String name, String role, Faction faction) {
        NpcEntity npc = new NpcEntity();
        npc.name = name;
        npc.role = role;
        npc.state = "Trade";
        npc.faction = faction;
        return npc;
    }

    private static TradeOffer offer(TraderSession session, String item) {
        for (TradeOffer offer : session.offers) if (offer != null && ItemQuality.namesMatch(offer.name, item)) return offer;
        throw new AssertionError("missing offer " + item);
    }

    private static void requireOffer(TraderSession session, String item) {
        require(session.offers.stream().anyMatch(offer -> offer != null && ItemQuality.namesMatch(offer.name, item)),
                "vendor missing " + item + ": " + session.offers.stream().map(offer -> offer.name).toList());
    }

    private static void requireContains(String actual, String expected, String label) {
        require(actual != null && actual.contains(expected), label + " missing '" + expected + "': " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone04FactionMarketAccessSmoke() {}
}
