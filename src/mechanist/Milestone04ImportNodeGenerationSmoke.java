package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/** End-to-end smoke for physical import rooms, cargo routing, and personnel arrival. */
final class Milestone04ImportNodeGenerationSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        testSectorExchangeCargoAndPersonnel();
        testZoneSpecificNodeFamilies();
        System.out.println("Milestone 04 import node generation smoke passed.");
    }

    private static void testSectorExchangeCargoAndPersonnel() {
        World world = world(16201L, ZoneType.NEUTRAL_RAIL_DEPOT, Faction.HIVER);
        world.floor = 5;
        world.zoneX = 2;
        world.zoneY = 2;
        world.zoneName = "Sector Exchange";

        FactionImportNodeGenerationAuthority.Result planned =
                FactionImportNodeGenerationAuthority.promoteAndPlan(world, new Random(1));
        require(planned.roomsPromoted() == 1 && "sector-exchange".equals(planned.kind()),
                "the early floor-5 zone-2,2 rail target should become a data-driven sector exchange");
        FactionImportNodeGenerationAuthority.Result placed =
                FactionImportNodeGenerationAuthority.placePhysicalNodes(world, new Random(2));
        require(placed.nodesPlaced() == 1, "promoted import room should receive one physical marker");
        MapObjectState node = FactionImportNodeGenerationAuthority.primaryNode(world, Faction.HIVER);
        require(node != null && "sector-exchange".equals(MapObjectState.stockValue(node.stockState, "kind")),
                "sector exchange should expose its exact persistent node kind");
        int roomId = Integer.parseInt(MapObjectState.stockValue(node.stockState, "roomId"));
        require(world.roomIds[node.x][node.y] == roomId, "physical node should stand inside its promoted room");
        requireContains(world.roomProfiles.get(roomId).featureText, "import intake roster", "room population support");
        require(FactionImportNodeGenerationAuthority.promoteAndPlan(world, new Random(3)).roomsPromoted() == 0
                        && FactionImportNodeGenerationAuthority.placePhysicalNodes(world, new Random(4)).nodesPlaced() == 0,
                "reapplying import generation must not duplicate rooms or markers");

        PersonnelPopulationApi.ensureLedgers(world, new Random(5));
        RoomPopulationLedger ledger = ledgerForRoom(world, roomId);
        require(ledger != null && ledger.capacity == 7 && ledger.sourceKind.contains("rail"),
                "import room should provide a capacity-limited rail intake roster");

        TraderSession trader = new TraderSession();
        trader.name = "Hiver Industrial Receiving Counter";
        trader.archetype = "import freight trader";
        trader.zoneLabel = world.zoneType.label;
        TradeOffer metal = offer("Refined metal stock");
        metal.provenance = ItemProvenanceRecord.of(metal.name, Faction.NONE, "outside-sector freight cooperative",
                world, 20, "sealed industrial freight", "outside-sector freight train -> rail intake");
        metal.provenance.producingFacility = "Outer Belt Refinery";
        trader.offers.add(metal);
        ShipmentProvenanceAuthority.apply(trader, world, Faction.HIVER, 100L, 100);
        ShipmentProvenanceRecord shipment = ShipmentProvenanceAuthority.shipmentForCargo(
                world, Faction.HIVER, "Refined metal stock");
        require(shipment != null && node.label.equals(shipment.arrivalNode) && shipment.route.contains(node.label),
                "external cargo should bind its manifest and route to the exact physical node");

        NpcEntity dead = NpcEntity.create(Faction.HIVER, world.zoneType, 5, 5, new Random(6));
        dead.id = "IMPORT-NODE-CASUALTY";
        dead.name = "Exchange Loader";
        dead.provenance = new PersonnelProvenanceRecord();
        dead.provenance.originSiteId = ledger.id;
        PersonnelReplacementRequest request = PersonnelProvenanceApi.recordDeathAndScheduleReplacement(
                world, dead, 100, new Random(7), "freight handling casualty");
        require(request != null && "train-import".equals(request.sourceMode) && request.scriptCost == 0,
                "generated import infrastructure should enable the free slow train source");
        requireContains(request.source, node.label, "reinforcement node binding");
        request.dueTurn = 100;
        request.expiresTurn = 200;
        FactionReinforcementAuthority.ReceptionResult reception = FactionReinforcementAuthority.receive(
                world, Faction.HIVER, 100, new Random(8));
        require(reception.success() && reception.arrived() == 1 && reception.scriptCost() == 0,
                "mature train manifest should receive one free replacement");
        NpcEntity arrival = reception.personnel().get(0);
        require(Math.max(Math.abs(arrival.x - node.x), Math.abs(arrival.y - node.y)) <= 1,
                "train replacement should materialize adjacent to the physical import node");
        require(arrival.provenance != null && arrival.provenance.arrivalRoute.contains(node.label),
                "imported person's provenance should retain the exact arrival node");

        List<String> inspection = FactionImportNodeGenerationAuthority.inspectionLines(world, node, 100L);
        requireContains(String.join(" ", inspection), "Cargo traffic:", "cargo status readback");
        requireContains(String.join(" ", inspection), "Personnel traffic:", "personnel status readback");
        testPlayerReadback(world, node);

        Properties saved = new Properties();
        Persistence.writeWorldState(world, saved);
        World restored = world(16201L, ZoneType.NEUTRAL_RAIL_DEPOT, Faction.HIVER);
        restored.floor = 5;
        restored.zoneX = 2;
        restored.zoneY = 2;
        restored.zoneName = "Sector Exchange";
        Persistence.readWorldState(restored, saved);
        MapObjectState restoredNode = FactionImportNodeGenerationAuthority.primaryNode(restored, Faction.HIVER);
        ShipmentProvenanceRecord restoredShipment = ShipmentProvenanceAuthority.shipmentForCargo(
                restored, Faction.HIVER, "Refined metal stock");
        require(restoredNode != null && node.id.equals(restoredNode.id) && node.label.equals(restoredNode.label),
                "physical import marker identity should survive save/load");
        require(restoredShipment != null && restoredNode.label.equals(restoredShipment.arrivalNode),
                "cargo-to-node binding should survive save/load");
    }

    private static void testZoneSpecificNodeFamilies() {
        requireKind(world(16202L, ZoneType.SECTOR_GOVERNORS_MANSION, Faction.NOBLE_HOUSE_VARN),
                "noble-private-import", "noble territory");
        requireKind(world(16203L, ZoneType.GANGER_TURF, Faction.GANGER_ASH_MARKET),
                "smuggling-entry", "gang territory");
        World sewer = world(16204L, ZoneType.SEWER_CONDUIT, Faction.SCAVENGER);
        sewer.sewerLayer = true;
        requireKind(sewer, "sewer-freight-hoist", "sewer territory");
        requireKind(world(16205L, ZoneType.IMPERIAL_GUARD_BILLET, Faction.IMPERIAL_GUARD),
                "customs-checkpoint", "controlled military territory");
        World upper = world(16206L, ZoneType.HAB_STACK, Faction.HIVER);
        upper.floor = 9;
        requireKind(upper, "air-void-cargo-dock", "upper-floor territory");
    }

    private static void testPlayerReadback(World world, MapObjectState node) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.world = world;
            Point approach = FactionImportNodeGenerationAuthority.arrivalPoint(world, Faction.HIVER);
            require(approach != null, "node should have a reachable adjacent interaction point");
            game.playerX = approach.x;
            game.playerY = approach.y;
            game.lookX = node.x;
            game.lookY = node.y;
            game.turn = 100;
            game.worldTurn = 100;
            int before = game.turn;
            game.confirmInteraction();
            require(game.turn == before + 1, "checking the import manifest should spend one interaction turn");
            requireContains(String.join(" ", game.eventLog), "Cargo traffic:", "Interact cargo readback");
            requireContains(String.join(" ", game.eventLog), "Personnel traffic:", "Interact personnel readback");
            String look = String.join(" ", ProgressiveLookAuthority.tileStackAt(game, node.x, node.y, 2));
            requireContains(look, "Arrival node:", "progressive Look import readback");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static void requireKind(World world, String expected, String label) {
        FactionImportNodeGenerationAuthority.Result result =
                FactionImportNodeGenerationAuthority.promoteAndPlan(world, new Random(world.seed));
        FactionImportNodeGenerationAuthority.placePhysicalNodes(world, new Random(world.seed + 1));
        MapObjectState node = FactionImportNodeGenerationAuthority.primaryNode(world, Faction.NONE);
        require(result.roomsPromoted() == 1 && node != null
                        && expected.equals(MapObjectState.stockValue(node.stockState, "kind")),
                label + " should generate " + expected + ": " + result.summary());
    }

    private static World world(long seed, ZoneType zone, Faction faction) {
        World world = new World(seed, 88, 72);
        world.zoneType = zone;
        world.zoneName = zone.label + " Import Test";
        for (int x = 0; x < world.w; x++) for (int y = 0; y < world.h; y++) world.tiles[x][y] = '#';
        addRoom(world, "Central Plaza", "neutral transit plaza", Faction.NONE);
        addRoom(world, "Cargo Warehouse", "a controlled freight and material store", faction);
        addRoom(world, "Service Receiving Room", "a lift-adjacent loading room", faction);
        addRoom(world, "Worker Dormitory", "staff sleeping and duty space", faction);
        return world;
    }

    private static int addRoom(World world, String name, String description, Faction faction) {
        int index = world.rooms.size();
        int slot = Math.max(0, index - 1);
        Rectangle room = new Rectangle(4 + (slot % 4) * 19, 4 + (slot / 4) * 15, 14, 10);
        world.carve(room);
        world.rooms.add(room);
        world.roomProfiles.set(index, new RoomProfile(name, description, 60, faction,
                new String[]{"Trade chit"}, new char[]{'Q'}));
        world.roomFactions.set(index, faction);
        world.roomSpecials.set(index, Boolean.FALSE);
        for (int x = room.x + 1; x < room.x + room.width - 1; x++) {
            for (int y = room.y + 1; y < room.y + room.height - 1; y++) world.tiles[x][y] = '.';
        }
        return index;
    }

    private static RoomPopulationLedger ledgerForRoom(World world, int roomId) {
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger != null && ledger.roomId == roomId) return ledger;
        }
        return null;
    }

    private static TradeOffer offer(String item) {
        ItemDef definition = ItemCatalog.get(item);
        return new TradeOffer(item, definition.category, definition.basePrice, "outside-sector imported freight");
    }

    private static void requireContains(String actual, String expected, String label) {
        require(actual != null && actual.contains(expected), label + " missing '" + expected + "': " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone04ImportNodeGenerationSmoke() { }
}
