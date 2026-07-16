package mechanist;

import java.util.Random;

/** End-to-end smoke for market-pressure contracts, linked outcomes, event news, and local trade exceptions. */
final class Milestone04FactionMarketContractNewsSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        testDepletedReserveContractAndTurnIn();
        testShipmentAndReinforcementPressure();
        testFactionIdentityOffers();
        testProtectedDraughtCustodyContract();
        testWorldEventContractNewsAndOffMapBoundary();
        System.out.println("Milestone 04 faction market contract and news smoke passed.");
    }

    private static void testDepletedReserveContractAndTurnIn() {
        GamePanel game = game(17201L, ZoneType.HAB_STACK);
        try {
            NpcEntity representative = representative("Block Supply Delegate", Faction.HIVER);
            EssentialSupplyReserveRecord food = new EssentialSupplyReserveRecord();
            food.id = "essential.contract.food";
            food.itemName = "Emergency rations";
            food.category = "food";
            food.faction = Faction.HIVER;
            food.sourceLabel = "Block pantry reserve";
            food.capacity = 3;
            food.remaining = 0;
            game.world.essentialSupplyReserves.add(food);
            game.factionMarketPressure.put(Faction.HIVER, 12);

            requireContains(FactionMarketContractAuthority.representativeLine(game, representative),
                    "food reserve depletion", "representative pressure preview");
            FactionMarketContractAuthority.WorkResult accepted =
                    FactionMarketContractAuthority.accept(game, representative);
            require(accepted.success() && "MARKET".equals(accepted.contract().type),
                    "depleted reserve should create a live market contract");
            require("Emergency rations".equals(accepted.contract().requiredTurnInItem),
                    "contract should request the exact depleted essential item");
            requireContains(accepted.contract().displayType(), "market supply", "market contract display type");
            FactionContract restored = FactionContract.parse(accepted.contract().saveLine());
            require(restored != null && restored.targetEntityId.equals(accepted.contract().targetEntityId)
                            && "MARKET".equals(restored.type),
                    "market pressure target should survive contract persistence");

            game.inventory.add("Emergency rations");
            int scriptBefore = game.carriedScript;
            ContractTurnInAuthority.TurnInResult turnIn = ContractTurnInAuthority.turnInFirst(game, representative);
            require(turnIn.success() && food.remaining == 1, "turn-in should restore one exact reserve unit");
            require(game.factionMarketPressure.getOrDefault(Faction.HIVER, 0) == 7,
                    "completion should reduce recorded faction market pressure by five");
            require(game.carriedScript == scriptBefore + accepted.contract().payout,
                    "market contract should pay through the normal atomic turn-in flow");
            requireContains(turnIn.message(), "improved from 0/3 to 1/3", "linked reserve completion result");
            require(game.playerNewsEvents.stream().anyMatch(news -> news != null && news.category.contains("market service")),
                    "completed market work should enter the public news ledger");
        } finally { game.shutdownRuntime(); }
    }

    private static void testShipmentAndReinforcementPressure() {
        GamePanel shipmentGame = game(17202L, ZoneType.IMPERIAL_GUARD_BILLET);
        try {
            NpcEntity representative = representative("Guard Logistics Delegate", Faction.IMPERIAL_GUARD);
            ShipmentProvenanceRecord shipment = new ShipmentProvenanceRecord();
            shipment.id = "shipment.contract.intercepted";
            shipment.destinationFaction = Faction.IMPERIAL_GUARD;
            shipment.status = "INTERCEPTED";
            shipment.cargoItem = "Construction supplies";
            shipment.cargoManifest = "one replacement construction crate";
            shipment.supplier = "Outer works convoy";
            shipment.arrivalNode = "Guard freight intake";
            shipment.quantity = 1;
            shipment.remaining = 0;
            shipmentGame.world.shipmentRecords.add(shipment);

            FactionMarketContractAuthority.WorkResult accepted =
                    FactionMarketContractAuthority.accept(shipmentGame, representative);
            require(accepted.success() && accepted.contract().targetEntityId.contains(shipment.id),
                    "intercepted faction shipment should become the first market-work pressure");
            shipmentGame.inventory.add("Construction supplies");
            ContractTurnInAuthority.TurnInResult turnIn =
                    ContractTurnInAuthority.turnInFirst(shipmentGame, representative);
            require(turnIn.success() && "DELIVERED".equals(shipment.status) && shipment.remaining == 0,
                    "replacement delivery should close the shipment without generating duplicate shelf stock: success="
                            + turnIn.success() + " status=" + shipment.status + " remaining=" + shipment.remaining
                            + " target=" + accepted.contract().targetEntityId + " message=" + turnIn.message());
            requireContains(shipment.eventModifier, "player replacement delivery", "shipment outcome trace");
        } finally { shipmentGame.shutdownRuntime(); }

        GamePanel reinforcementGame = game(17203L, ZoneType.HAB_STACK);
        try {
            NpcEntity representative = representative("Block Muster Delegate", Faction.HIVER);
            PersonnelReplacementRequest request = new PersonnelReplacementRequest();
            request.deadNpcId = "fallen-worker-contract";
            request.deadName = "Fallen Worker";
            request.faction = Faction.HIVER;
            request.source = "rail reinforcement train";
            request.requestedTurn = 0;
            request.dueTurn = 1200;
            request.expiresTurn = 1800;
            reinforcementGame.world.replacementQueue.add(request);
            FactionMarketContractAuthority.WorkResult accepted =
                    FactionMarketContractAuthority.accept(reinforcementGame, representative);
            require(accepted.success() && accepted.contract().targetEntityId.contains(request.deadNpcId),
                    "pending reinforcement should create a support-supply contract");
            reinforcementGame.inventory.add("Emergency rations");
            require(ContractTurnInAuthority.turnInFirst(reinforcementGame, representative).success(),
                    "reinforcement support contract should turn in through the representative");
            require(request.dueTurn == 600 && request.expiresTurn == 1200,
                    "support delivery should advance both arrival and expiry windows by six hours");
        } finally { reinforcementGame.shutdownRuntime(); }
    }

    private static void testFactionIdentityOffers() {
        GamePanel gang = game(17204L, ZoneType.GANGER_TURF);
        try {
            NpcEntity representative = representative("Chem Crew Broker", Faction.GANGER_BLACK_SUMP);
            String line = FactionMarketContractAuthority.representativeLine(gang, representative);
            requireContains(line, "Street Stimm", "illicit identity contract item");
            requireContains(line, "internal use, sale income, leverage, and territorial trade",
                    "illicit narcotics economic purpose");
        } finally { gang.shutdownRuntime(); }

        GamePanel noble = game(17205L, ZoneType.SECTOR_GOVERNORS_MANSION);
        try {
            String line = FactionMarketContractAuthority.representativeLine(noble,
                    representative("House Varn Estate Delegate", Faction.NOBLE_HOUSE_VARN));
            requireContains(line, "Pearl Obscura", "noble luxury contract item");
            requireContains(line, "hospitality, gifting, favors, and private consumption",
                    "noble luxury economic purpose");
        } finally { noble.shutdownRuntime(); }
    }

    private static void testProtectedDraughtCustodyContract() {
        GamePanel game = game(17207L, ZoneType.SECTOR_GOVERNORS_MANSION);
        try {
            DraughtCustodyRecord custody = new DraughtCustodyRecord();
            custody.id = "draught.contract.black-sun";
            custody.itemName = "Black Sun Draught";
            custody.ownerFaction = Faction.NOBLE_HOUSE_VARN;
            custody.houseOwner = "House Varn";
            custody.vaultLabel = "House Varn Sealed Estate Vault";
            custody.heldQuantity = 1;
            custody.releasedForSale = false;
            game.world.draughtCustodyRecords.add(custody);
            NpcEntity representative = representative("House Varn Custody Delegate", Faction.NOBLE_HOUSE_VARN);

            String line = FactionMarketContractAuthority.representativeLine(game, representative);
            requireContains(line, "Noble Commerce Permit", "draught custody support item");
            requireContains(line, "protected custody for Black Sun Draught", "named draught custody pressure");
            requireContains(line, "without releasing the draught for sale", "protected sale boundary");

            FactionMarketContractAuthority.WorkResult accepted =
                    FactionMarketContractAuthority.accept(game, representative);
            require(accepted.success() && accepted.contract().targetEntityId.contains(custody.id),
                    "protected draught custody should create a linked political-support contract");
            game.inventory.add("Noble Commerce Permit");
            ContractTurnInAuthority.TurnInResult unqualified =
                    ContractTurnInAuthority.turnInFirst(game, representative);
            require(!unqualified.success() && game.inventory.contains("Noble Commerce Permit")
                            && custody.heldQuantity == 1 && !custody.releasedForSale,
                    "unqualified custody turn-in must preserve the permit and protected draught");
            game.unlockedSkillNodes.add("trade-batch-appraisal");
            game.unlockedSkillNodes.add("trade-guilder-certification");
            game.unlockedSkillNodes.add("investigation-trace-reading");
            game.unlockedKnowledges.add("Contract Negotiation");
            ContractTurnInAuthority.TurnInResult turnIn = ContractTurnInAuthority.turnInFirst(game, representative);
            require(turnIn.success(), "draught custody permit should use the normal atomic turn-in flow");
            require(custody.heldQuantity == 1 && !custody.releasedForSale,
                    "custody support must not consume or release the protected draught");
            requireContains(custody.eventStatus, "custody papers renewed", "custody completion trace");
            requireContains(turnIn.message(), "remains not for sale", "custody completion boundary");
        } finally { game.shutdownRuntime(); }
    }

    private static void testWorldEventContractNewsAndOffMapBoundary() {
        GamePanel game = game(17206L, ZoneType.NEUTRAL_RAIL_DEPOT);
        try {
            TopDownWorldEventRecord ban = TopDownWorldEventAuthority.scheduleCurated(game.world, "EXPORT_BAN", 0L);
            TopDownWorldEventAuthority.TickResult tick = TopDownWorldEventAuthority.tick(game.world, game, 0L);
            require(tick.activated() == 1 && "ACTIVE".equals(ban.status), "export ban should activate for the smoke");
            PlayerNewsEvent eventNews = game.playerNewsEvents.stream()
                    .filter(news -> news != null && news.category.contains("world-event active"))
                    .findFirst().orElse(null);
            require(eventNews != null && eventNews.publicDay == 0,
                    "active event should enter the public news ledger on the activation day");
            requireContains(eventNews.detail, "Off-map sales and export settlement are suspended",
                    "confirmed event restriction news");
            requireContains(eventNews.detail, "Local and internal faction commerce remains open",
                    "confirmed event exception news");
            requireContains(ImperialNewsNetworkApi.generateIssue(game, 0, new Random(9)),
                    "Sector event bulletin", "newspaper event exposure");
            requireContains(ImperialNewsNetworkApi.playerBroadcastLineFor(game, 0, new Random(10)),
                    "sector event notice", "broadcast event exposure");

            NpcEntity representative = representative("Rail Market Delegate", Faction.HIVER);
            requireContains(FactionMarketContractAuthority.representativeLine(game, representative),
                    "external-sector export ban", "event-driven representative work");
            String offMapBlock = FactionMarketAccessAuthority.offMapSaleBlock(game.world, 0L);
            requireContains(offMapBlock, "closes off-map sale settlement", "off-map sale closure");

            TraderSession local = new TraderSession();
            local.marketFaction = Faction.HIVER;
            local.marketCategory = "provisions";
            TradeOffer food = new TradeOffer("Emergency rations", "food", 4, "local faction stock");
            FactionMarketAccessAuthority.Decision localDecision = FactionMarketAccessAuthority.evaluate(local, food,
                    new FactionMarketAccessAuthority.AccessContext(Faction.NONE, 0, 0, 0,
                            java.util.Set.of(), java.util.Set.of(), java.util.List.of(), game.world, 0L));
            require(localDecision.allowed(), "export ban should preserve local faction-vendor purchases");
            requireContains(localDecision.eventNotice(), "local/internal vendor sale remains exempt",
                    "local-vendor event exception");

            TopDownWorldEventAuthority.tick(game.world, game, ban.endWorldTurn);
            require(FactionMarketAccessAuthority.offMapSaleBlock(game.world, ban.endWorldTurn).isBlank(),
                    "off-map settlement block should clear on event recovery");
            require(game.playerNewsEvents.stream().anyMatch(news -> news != null
                            && news.category.contains("world-event recovered")),
                    "event recovery should publish a recovery notice");
        } finally { game.shutdownRuntime(); }
    }

    private static GamePanel game(long seed, ZoneType zone) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.seed = seed;
        game.world = new World(seed, 40, 40);
        game.world.zoneType = zone;
        game.turn = 0;
        game.worldTurn = 0L;
        game.carriedScript = 0;
        return game;
    }

    private static NpcEntity representative(String name, Faction faction) {
        NpcEntity npc = new NpcEntity();
        npc.name = name;
        npc.role = "Faction Representative";
        npc.state = "Contract Desk";
        npc.faction = faction;
        return npc;
    }

    private static void requireContains(String actual, String expected, String label) {
        require(actual != null && actual.contains(expected), label + " missing '" + expected + "': " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone04FactionMarketContractNewsSmoke() {}
}
