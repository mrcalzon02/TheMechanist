package mechanist;

import mechanist.assets.AssetRegistry;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** End-to-end smoke for Phase 18 live editor snapshots and searchable item market references. */
final class Milestone04EconomyWorldEditorInfopediaSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            seedRuntimeWorld(game);
            verifyEditorSchemasAndLiveSnapshots(game);
            verifyToolsMenuLayout(game);
            verifyInGameRefreshControl(game);
            verifyItemInfopedia();
            System.out.println("Milestone 04 economy/world editor and Infopedia smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static void seedRuntimeWorld(GamePanel game) {
        World world = new World(18001L, 32, 32);
        world.floor = 5;
        game.world = world;
        game.turn = 321;
        game.worldTurn = 321L;

        EssentialSupplyReserveRecord reserve = new EssentialSupplyReserveRecord();
        reserve.id = "essential.editor.smoke";
        reserve.faction = Faction.HIVER;
        reserve.remaining = 3;
        reserve.capacity = 6;
        reserve.sourceLabel = "Hab kitchen";
        world.essentialSupplyReserves.add(reserve);

        ShipmentProvenanceRecord shipment = new ShipmentProvenanceRecord();
        shipment.id = "shipment.editor.smoke";
        shipment.destinationFaction = Faction.HIVER;
        shipment.cargoItem = "Fertilizer";
        shipment.cargoManifest = "Fertilizer, Chemical reagent bottle";
        shipment.status = "DELAYED";
        shipment.earliestArrivalWorldTurn = 360L;
        shipment.latestArrivalWorldTurn = 390L;
        world.shipmentRecords.add(shipment);

        RoomPopulationLedger population = new RoomPopulationLedger();
        population.id = "population.editor.smoke";
        population.roomName = "Hab Roster";
        population.faction = Faction.HIVER;
        population.capacity = 12;
        population.available = 2;
        population.assigned = 10;
        world.roomPopulationLedgers.add(population);

        NpcEntity npc = new NpcEntity();
        npc.id = "npc-editor-smoke";
        npc.name = "Hest Editor";
        npc.faction = Faction.HIVER;
        npc.role = "Provisioner";
        npc.hp = 10;
        npc.happiness = 37;
        npc.happinessReason = "pay and food are overdue";
        npc.provenance = new PersonnelProvenanceRecord();
        npc.provenance.originMode = "faction birth";
        npc.provenance.originRoom = "Hab Roster";
        world.npcs.add(npc);

        PersonnelReplacementRequest replacement = new PersonnelReplacementRequest();
        replacement.deadNpcId = "fallen-editor-smoke";
        replacement.deadName = "Fallen Worker";
        replacement.faction = Faction.HIVER;
        replacement.sourceMode = FactionReinforcementAuthority.SourceMethod.TRAIN_IMPORT.id;
        replacement.source = "Sector Exchange Cargo Gate";
        replacement.requestedTurn = 300;
        replacement.dueTurn = 360;
        replacement.expiresTurn = 408;
        world.replacementQueue.add(replacement);

        MapObjectState importNode = new MapObjectState();
        importNode.id = "IMPORT-NODE-EDITOR-SMOKE";
        importNode.type = FactionImportNodeGenerationAuthority.TYPE;
        importNode.label = "Hiver Sector Exchange Cargo Gate";
        importNode.glyph = 'q';
        importNode.stockState = "kind=sector-exchange;faction=HIVER;roomId=1;status=open";
        world.mapObjects.add(importNode);

        DeferredFactionLedgerRecord distant = new DeferredFactionLedgerRecord();
        distant.id = "distant-network.editor.smoke";
        distant.faction = Faction.HIVER;
        distant.strength = 63;
        distant.lastChancePct = 58;
        distant.lastRoll = 41;
        distant.factorSummary = "supplier reliability and route safety exceed shipment pressure";
        world.deferredFactionLedgers.add(distant);

        TopDownWorldEventRecord event = TopDownWorldEventAuthority.scheduleCurated(world, "EXPORT_BAN", 300L);
        event.status = "ACTIVE";
        game.factionMarketPressure.put(Faction.HIVER, 12);
    }

    private static void verifyEditorSchemasAndLiveSnapshots(GamePanel game) {
        List<String> editors = List.of(
                SimulationToolSuiteRegistry.POPULATION_EDITOR,
                SimulationToolSuiteRegistry.ECONOMY_EDITOR,
                SimulationToolSuiteRegistry.REINFORCEMENT_EDITOR,
                SimulationToolSuiteRegistry.DEFERRED_NETWORK_EDITOR,
                SimulationToolSuiteRegistry.WORLD_EVENT_EDITOR);
        SimulationEditorRepository repository = new SimulationEditorRepository();
        for (String editor : editors) {
            require(SimulationEditorRepository.EDITORS.contains(editor), "missing dedicated editor " + editor);
            require(!SimulationToolSuiteRegistry.defaultPropertiesFor(editor).isEmpty(), "missing schema " + editor);
            require(!SimulationToolSuiteRegistry.seedEntitiesFor(editor).isEmpty(), "missing authoring template " + editor);
            SimulationRuntimeEditorBridgeAuthority.RefreshResult result =
                    SimulationRuntimeEditorBridgeAuthority.refresh(game, repository, editor);
            require(result.supported() && result.records() > 0, "live refresh failed for " + editor + ": " + result);
            require(runtimeCount(repository, editor) == result.records(), "runtime record count mismatch for " + editor);
        }
        SimulationRuntimeEditorBridgeAuthority.RefreshResult faction = SimulationRuntimeEditorBridgeAuthority.refresh(
                game, repository, SimulationToolSuiteRegistry.FACTION_EDITOR);
        require(faction.records() >= Faction.visibleFactions().length, "faction runtime records should cover visible factions");

        SimulationEditorRepository.EditableEntity essential = runtimeByClass(repository,
                SimulationToolSuiteRegistry.ECONOMY_EDITOR, EssentialSupplyReserveRecord.class.getSimpleName());
        require(essential != null && Integer.valueOf(3).equals(essential.properties().get("remaining")),
                "essential reserve fields should be visible in the economy editor");
        SimulationEditorRepository.EntityRef ref = new SimulationEditorRepository.EntityRef(
                SimulationToolSuiteRegistry.ECONOMY_EDITOR, essential.id());
        repository.setProperty(ref, "remaining", 0);
        require(game.world.essentialSupplyReserves.get(0).remaining == 3,
                "editing a snapshot must not mutate the active world reserve");

        int before = runtimeCount(repository, SimulationToolSuiteRegistry.ECONOMY_EDITOR);
        SimulationRuntimeEditorBridgeAuthority.refresh(game, repository, SimulationToolSuiteRegistry.ECONOMY_EDITOR);
        require(runtimeCount(repository, SimulationToolSuiteRegistry.ECONOMY_EDITOR) == before,
                "refresh should replace old runtime rows instead of duplicating them");
        require(runtimeByClass(repository, SimulationToolSuiteRegistry.REINFORCEMENT_EDITOR,
                "ReinforcementSourcePolicy") != null, "reinforcement editor should expose source prices and timers");
        require(runtimeByClass(repository, SimulationToolSuiteRegistry.WORLD_EVENT_EDITOR,
                TopDownWorldEventRecord.class.getSimpleName()) != null, "world-event editor should expose active event effects");
    }

    private static void verifyInGameRefreshControl(GamePanel game) {
        game.openInGameEditor(SimulationToolSuiteRegistry.ECONOMY_EDITOR);
        require(game.screen == GamePanel.Screen.EDITOR, "economy editor should open inside the primary game window");
        require(game.inGameEditorStatus.contains("Refreshed ")
                        && game.inGameEditorStatus.contains("economy editor records")
                        && game.inGameEditorStatus.contains("live-world")
                        && game.inGameEditorStatus.contains("catalog/definition"),
                "opening should distinguish live world and catalog-definition records");
        game.setSize(1280, 820);
        BufferedImage image = new BufferedImage(1280, 820, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        game.paintComponent(graphics);
        graphics.dispose();
        require(game.buttons.stream().anyMatch(button -> button != null && "Refresh Live World".equals(button.label)),
                "runtime-backed editor should provide a refresh command");
    }

    private static void verifyToolsMenuLayout(GamePanel game) {
        game.setSize(900, 600);
        game.openToolsMenu();
        BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        game.paintComponent(graphics);
        graphics.dispose();

        Set<String> labels = new HashSet<>();
        for (ButtonBox button : game.buttons) {
            require(button != null && button.r != null, "tools menu should only expose concrete buttons");
            require(button.r.x >= 0 && button.r.y >= 0
                            && button.r.x + button.r.width <= 900 && button.r.y + button.r.height <= 600,
                    "tools button should stay inside the 900x600 viewport: " + button.label + " " + button.r);
            require(labels.add(button.label), "tools menu should not duplicate button " + button.label);
        }
        for (String editor : List.of(
                SimulationToolSuiteRegistry.POPULATION_EDITOR,
                SimulationToolSuiteRegistry.ECONOMY_EDITOR,
                SimulationToolSuiteRegistry.REINFORCEMENT_EDITOR,
                SimulationToolSuiteRegistry.DEFERRED_NETWORK_EDITOR,
                SimulationToolSuiteRegistry.WORLD_EVENT_EDITOR,
                SimulationToolSuiteRegistry.MOD_PACKAGING_EDITOR)) {
            require(labels.contains(editor), "tools menu should expose " + editor);
        }
        for (int i = 0; i < game.buttons.size(); i++) {
            Rectangle left = game.buttons.get(i).r;
            for (int j = i + 1; j < game.buttons.size(); j++) {
                Rectangle overlap = left.intersection(game.buttons.get(j).r);
                require(overlap.isEmpty(), "tools buttons should not overlap: "
                        + game.buttons.get(i).label + " and " + game.buttons.get(j).label);
            }
        }
    }

    private static void verifyItemInfopedia() {
        String draughtRow = itemRow("Black Sun Draught");
        List<String> draught = SemanticAssetInfopediaAuthority.detailLines(AssetRegistry.empty(), draughtRow, null, "");
        requireContains(draught, "noble-only protected custody", "draught access reference");
        requireContains(draught, "secured estate vault", "draught availability reference");
        requireContains(draught, "Common sources and facilities", "draught source reference");

        String rationRow = itemRow("Emergency rations");
        List<String> ration = SemanticAssetInfopediaAuthority.detailLines(AssetRegistry.empty(), rationRow, null, "");
        requireContains(ration, "Common sellers: provisioners", "ration seller reference");
        requireContains(ration, "finite local reserves", "ration availability reference");
        requireContains(ration, "train or outside-sector shipment", "ration provenance alternatives");
    }

    private static String itemRow(String item) {
        for (String row : SemanticAssetInfopediaAuthority.entries(AssetRegistry.empty(), null, item)) {
            if (row.startsWith("ITEM - " + item + " [")) return row;
        }
        throw new AssertionError("missing searchable item Infopedia row for " + item);
    }

    private static int runtimeCount(SimulationEditorRepository repository, String editor) {
        int count = 0;
        for (SimulationEditorRepository.EditableEntity entity : repository.entities(editor))
            if (Boolean.TRUE.equals(entity.properties().get("runtimeSnapshot"))) count++;
        return count;
    }

    private static SimulationEditorRepository.EditableEntity runtimeByClass(SimulationEditorRepository repository,
                                                                              String editor, String recordClass) {
        for (SimulationEditorRepository.EditableEntity entity : repository.entities(editor)) {
            if (Boolean.TRUE.equals(entity.properties().get("runtimeSnapshot"))
                    && recordClass.equals(entity.properties().get("recordClass"))) return entity;
        }
        return null;
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        require(lines.toString().contains(expected), label + " missing '" + expected + "': " + lines);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone04EconomyWorldEditorInfopediaSmoke() { }
}
