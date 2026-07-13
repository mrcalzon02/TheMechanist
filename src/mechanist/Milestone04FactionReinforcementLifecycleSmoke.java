package mechanist;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

/** Smoke for casualty-backed, timed, capacity-limited faction reinforcement arrivals. */
final class Milestone04FactionReinforcementLifecycleSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.world = new World(84056L, 32, 32);
            game.world.npcs.clear();
            game.world.replacementQueue.clear();
            game.world.roomPopulationLedgers.clear();
            RoomPopulationLedger ledger = ledger();
            game.world.roomPopulationLedgers.add(ledger);
            game.turn = 40;
            game.worldTurn = 40;

            NpcEntity representative = NpcEntity.factionRepresentative(Faction.CIVIC_WARDENS, 4, 4, new Random(1));
            game.world.npcs.add(representative);
            game.activeInteractionNpc = representative;
            game.panelMode = GamePanel.PanelMode.DIALOGUE;
            game.screen = GamePanel.Screen.PANEL;
            game.setSize(1280, 720);

            NpcEntity casualty = casualty(game.world, ledger, "Patrol Officer Vale", 5, 5, 1001);
            ledger.assigned = 1;
            game.world.npcs.add(casualty);
            casualty.hp = 0;
            FactionReinforcementAuthority.TickResult captured = FactionReinforcementAuthority.tick(game.world, game.turn, new Random(2));
            require(captured.casualties() == 1 && !game.world.npcs.contains(casualty),
                    "dead faction actor should be captured and removed exactly once");
            require(ledger.assigned == 0 && ledger.dead == 1,
                    "casualty should leave its staffed slot and increment recorded losses");
            require(game.world.replacementQueue.size() == 1, "casualty should open one replacement manifest");
            PersonnelReplacementRequest request = game.world.replacementQueue.get(0);
            require(request.requestedTurn == game.turn && request.dueTurn > game.turn
                            && request.expiresTurn > request.dueTurn,
                    "manifest should carry durable request, availability, and expiry turns");

            render(game);
            require(hasButton(game, "Receive Reinforcements") && hasButton(game, "Change Source"),
                    "representative dialogue should render reinforcement receive and source controls: " + buttonLabels(game));
            requireContains(FactionReinforcementAuthority.representativeLine(game, representative),
                    "Reinforcements inbound", "inbound representative readback");

            int earlyTurn = game.turn;
            int earlyQueue = game.world.replacementQueue.size();
            game.receiveReinforcementsWithActiveNpc();
            require(game.turn == earlyTurn && game.world.replacementQueue.size() == earlyQueue,
                    "early reception should preserve time and the manifest");
            requireContains(lastEvent(game), "still inbound", "early reception feedback");

            Properties saved = new Properties();
            Persistence.writeCore(game, saved);
            String encoded = saved.getProperty("world.personnelReplacements", "");
            require(!encoded.isBlank(), "open reinforcement manifest should enter world persistence");
            PersonnelReplacementRequest parsed = PersonnelReplacementRequest.parse(request.saveLine());
            require(parsed != null && parsed.requestedTurn == request.requestedTurn
                            && parsed.dueTurn == request.dueTurn && parsed.expiresTurn == request.expiresTurn,
                    "reinforcement lifecycle turns should survive request persistence");
            String[] currentFields = request.saveLine().split("\\|", -1);
            PersonnelReplacementRequest legacy = PersonnelReplacementRequest.parse(
                    String.join("|", java.util.Arrays.copyOf(currentFields, 10)));
            require(legacy != null && legacy.expiresTurn > legacy.dueTurn,
                    "legacy replacement records should receive a backward-compatible arrival window");
            World restoredWorld = new World(84057L, 32, 32);
            Persistence.readWorldState(restoredWorld, saved);
            require(restoredWorld.replacementQueue.size() == 1
                            && restoredWorld.replacementQueue.get(0).expiresTurn == request.expiresTurn,
                    "open reinforcement manifest should survive world save/load");

            addReadyRequest(game.world, ledger, "Patrol Officer Kest", game.turn, 1002);
            addReadyRequest(game.world, ledger, "Patrol Officer Rook", game.turn, 1003);
            for (PersonnelReplacementRequest queued : game.world.replacementQueue) queued.dueTurn = game.turn;
            int beforeArrival = PersonnelPopulationApi.countLivingFactionActors(game.world, Faction.CIVIC_WARDENS);
            int availableBefore = ledger.available;
            game.receiveReinforcementsWithActiveNpc();
            int afterArrival = PersonnelPopulationApi.countLivingFactionActors(game.world, Faction.CIVIC_WARDENS);
            require(afterArrival == beforeArrival + 3 && game.world.replacementQueue.isEmpty(),
                    "one representative action should receive the ready group in a bounded batch");
            require(ledger.assigned == 3 && ledger.available == availableBefore - 3,
                    "bulk arrival should consume one reserve slot per person");
            require(game.turn == earlyTurn + 1, "successful reception should spend one turn");
            for (NpcEntity npc : game.world.npcs) {
                if (npc == representative) continue;
                require(npc.provenance != null && "replacement-arrival".equals(npc.provenance.originMode),
                        "arrived personnel should retain replacement provenance");
                requireContains(npc.provenance.backstory, "replace Patrol Officer", "arrival casualty provenance");
            }

            PersonnelReplacementRequest blocked = addReadyRequest(game.world, ledger, "Patrol Officer Full", game.turn, 1004);
            NpcEntity filler = NpcEntity.create(Faction.CIVIC_WARDENS, game.world.zoneType, 8, 8, new Random(8));
            game.world.npcs.add(filler);
            int blockedTurn = game.turn;
            FactionReinforcementAuthority.ReceptionResult full = FactionReinforcementAuthority.receive(
                    game.world, Faction.CIVIC_WARDENS, game.turn, new Random(9));
            require(!full.success() && game.world.replacementQueue.contains(blocked),
                    "full capacity should leave a ready manifest waiting");
            requireContains(full.message(), "capacity is full", "capacity blocker");
            require(game.turn == blockedTurn, "authority-level blocked reception must not spend time");

            blocked.expiresTurn = game.turn + 1;
            FactionReinforcementAuthority.TickResult expired = FactionReinforcementAuthority.tick(
                    game.world, game.turn + 2, new Random(10));
            require(expired.expired() == 1 && !game.world.replacementQueue.contains(blocked),
                    "unreceived manifest should expire after its arrival window");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static RoomPopulationLedger ledger() {
        RoomPopulationLedger ledger = new RoomPopulationLedger();
        ledger.id = "pop.wardens.rail-intake";
        ledger.roomId = 0;
        ledger.roomName = "Wardens Rail Intake Barracks";
        ledger.faction = Faction.CIVIC_WARDENS;
        ledger.sourceKind = "rail intake roster";
        ledger.sourceLabel = "Civic Wardens rail intake roster";
        ledger.capacity = 4;
        ledger.available = 3;
        return ledger;
    }

    private static NpcEntity casualty(World world, RoomPopulationLedger ledger, String name, int x, int y, int id) {
        NpcEntity npc = NpcEntity.create(Faction.CIVIC_WARDENS, world.zoneType, x, y, new Random(id));
        npc.id = "CASUALTY-" + id;
        npc.name = name;
        npc.provenance = new PersonnelProvenanceRecord();
        npc.provenance.originSiteId = ledger.id;
        npc.provenance.originRoom = ledger.roomName;
        npc.provenance.populationPool = ledger.sourceLabel;
        return npc;
    }

    private static PersonnelReplacementRequest addReadyRequest(World world, RoomPopulationLedger ledger,
                                                                 String name, int turn, int id) {
        NpcEntity dead = casualty(world, ledger, name, 6 + id % 3, 6 + id % 4, id);
        PersonnelReplacementRequest request = PersonnelProvenanceApi.recordDeathAndScheduleReplacement(
                world, dead, turn, new Random(id), "recorded faction casualty");
        request.dueTurn = turn;
        request.expiresTurn = turn + FactionReinforcementAuthority.ARRIVAL_WINDOW_TURNS;
        return request;
    }

    private static void render(GamePanel game) {
        java.awt.image.BufferedImage canvas = new java.awt.image.BufferedImage(1280, 720,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = canvas.createGraphics();
        game.paintComponent(graphics);
        graphics.dispose();
    }

    private static boolean hasButton(GamePanel game, String label) {
        for (ButtonBox button : game.buttons) if (button != null && label.equals(button.label)) return true;
        return false;
    }

    private static String buttonLabels(GamePanel game) {
        ArrayList<String> labels = new ArrayList<>();
        for (ButtonBox button : game.buttons) if (button != null) labels.add(button.label);
        return labels.toString();
    }

    private static String lastEvent(GamePanel game) {
        return game.eventLog.isEmpty() ? "" : game.eventLog.get(game.eventLog.size() - 1);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private Milestone04FactionReinforcementLifecycleSmoke() { }
}
