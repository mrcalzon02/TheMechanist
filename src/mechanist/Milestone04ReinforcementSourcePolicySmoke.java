package mechanist;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Smoke for infrastructure, timer, and script tradeoffs among reinforcement sources. */
final class Milestone04ReinforcementSourcePolicySmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.world = worldWithSources();
            game.turn = 80;
            game.worldTurn = 80;
            NpcEntity representative = NpcEntity.factionRepresentative(Faction.CIVIC_WARDENS, 4, 4, new Random(1));
            game.world.npcs.add(representative);
            game.activeInteractionNpc = representative;
            game.panelMode = GamePanel.PanelMode.DIALOGUE;
            game.screen = GamePanel.Screen.PANEL;
            game.setSize(1280, 720);

            PersonnelReplacementRequest request = request(game.world, game.turn, "Officer Train", 2001);
            require("train-import".equals(request.sourceMode) && request.scriptCost == 0,
                    "rail infrastructure should default the manifest to free train reinforcement");
            require(request.dueTurn >= game.turn + 150 && request.dueTurn <= game.turn + 250,
                    "train reinforcement should use the slow 150-250 turn timer");
            require(request.expiresTurn == request.dueTurn + GamePanel.TURNS_PER_HOUR * 8,
                    "train reinforcement should retain its longer intake window");
            requireContains(request.sourcePrerequisite, "rail intake", "train prerequisite");

            render(game);
            require(hasButton(game, "Receive Reinforcements") && hasButton(game, "Change Source"),
                    "representative should expose source choice beside reception: " + buttonLabels(game));
            requireContains(FactionReinforcementAuthority.representativeLine(game, representative),
                    "Reinforcement train / free", "free train readback");

            int trainDue = request.dueTurn;
            int sourceTurn = game.turn;
            game.cycleReinforcementSourceWithActiveNpc();
            require(game.turn == sourceTurn + 1 && "barracks-muster".equals(request.sourceMode),
                    "first source change should select the available barracks muster and spend one turn");
            require(request.scriptCost == 6 && request.dueTurn < trainDue,
                    "barracks muster should trade a modest fee for a shorter timer");
            require(request.dueTurn >= sourceTurn + 35 && request.dueTurn <= sourceTurn + 65,
                    "barracks muster should use the 35-65 turn timer");
            requireContains(request.sourcePrerequisite, "barracks/duty", "barracks prerequisite");

            int barracksDue = request.dueTurn;
            int paidSourceTurn = game.turn;
            game.cycleReinforcementSourceWithActiveNpc();
            require(game.turn == paidSourceTurn + 1 && "paid-local".equals(request.sourceMode),
                    "second source change should select paid local recruitment");
            require(request.scriptCost == 24 && request.dueTurn > barracksDue,
                    "paid recruitment should cost more and be slower than an equipped barracks muster");
            require(request.dueTurn >= paidSourceTurn + 65 && request.dueTurn <= paidSourceTurn + 110,
                    "paid local recruitment should use the 65-110 turn timer");

            PersonnelReplacementRequest persisted = PersonnelReplacementRequest.parse(request.saveLine());
            require(persisted != null && "paid-local".equals(persisted.sourceMode)
                            && persisted.scriptCost == 24
                            && persisted.sourcePrerequisite.equals(request.sourcePrerequisite),
                    "selected source, price, and prerequisite should persist with the manifest");

            request.dueTurn = game.turn;
            request.expiresTurn = game.turn + 100;
            game.carriedScript = 23;
            int blockedTurn = game.turn;
            int blockedPopulation = PersonnelPopulationApi.countLivingFactionActors(game.world, Faction.CIVIC_WARDENS);
            game.receiveReinforcementsWithActiveNpc();
            require(game.turn == blockedTurn && game.carriedScript == 23 && game.world.replacementQueue.contains(request),
                    "insufficient recruitment funds should preserve time, funds, and manifest");
            require(PersonnelPopulationApi.countLivingFactionActors(game.world, Faction.CIVIC_WARDENS) == blockedPopulation,
                    "insufficient funds must not materialize personnel");
            requireContains(lastEvent(game), "costs 24 script", "paid recruitment blocker");

            game.carriedScript = 24;
            game.receiveReinforcementsWithActiveNpc();
            require(game.carriedScript == 0 && game.turn == blockedTurn + 1 && !game.world.replacementQueue.contains(request),
                    "successful paid recruitment should charge exactly once and spend one turn");
            requireContains(lastEventContaining(game, "Received"), "paid 24 script", "paid arrival receipt");

            PersonnelReplacementRequest train = request(game.world, game.turn, "Officer Free", 2002);
            train.dueTurn = game.turn;
            train.expiresTurn = game.turn + 100;
            require("train-import".equals(train.sourceMode) && train.scriptCost == 0,
                    "new manifest should again prefer the available train route");
            int freeTurn = game.turn;
            game.receiveReinforcementsWithActiveNpc();
            require(game.carriedScript == 0 && game.turn == freeTurn + 1 && !game.world.replacementQueue.contains(train),
                    "train reinforcement should arrive without recruitment payment");
            requireContains(lastEventContaining(game, "Received"), "free intake", "free train receipt");

            World localOnly = new World(84200L, 24, 24);
            localOnly.npcs.clear();
            localOnly.replacementQueue.clear();
            localOnly.roomPopulationLedgers.clear();
            localOnly.roomPopulationLedgers.add(ledger("pop.local", "Hab Work Roster", "local hab work roster", 5));
            PersonnelReplacementRequest local = request(localOnly, 10, "Officer Local", 2003);
            require("paid-local".equals(local.sourceMode) && local.scriptCost == 24,
                    "a faction without rail or barracks infrastructure should fall back to paid recruitment");
            FactionReinforcementAuthority.SourceChangeResult unavailable = FactionReinforcementAuthority.cycleSource(
                    localOnly, Faction.CIVIC_WARDENS, 10, new Random(4));
            require(!unavailable.success() && unavailable.changed() == 0,
                    "missing buildings should prevent selecting unsupported train or barracks sources");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static World worldWithSources() {
        World world = new World(84199L, 32, 16);
        world.zoneType = ZoneType.ARBITES_PRECINCT_EDGE;
        world.npcs.clear();
        world.mapObjects.clear();
        world.hazardWarnings.clear();
        world.replacementQueue.clear();
        world.roomPopulationLedgers.clear();
        world.rooms.clear();
        world.roomProfiles.clear();
        world.roomFactions.clear();
        world.roomSpecials.clear();
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) {
                world.tiles[x][y] = '#';
                world.roomIds[x][y] = -1;
            }
        }

        addSourceRoom(world, new Rectangle(2, 2, 6, 4),
                new RoomProfile("Wardens Rail Intake",
                        "a platform-side intake room with rail manifests and arrival processing",
                        30, Faction.CIVIC_WARDENS, new String[]{"rail docket"}, new char[]{'q'}));
        RoomProfile barracks = new RoomProfile("Civic Wardens Duty Barracks",
                "a guarded barracks with four duty berths, an equipment issue post, and a muster anchor",
                45, Faction.CIVIC_WARDENS,
                new String[]{"Flak vest", "Shock maul"}, new char[]{'c', 'q', 'A'});
        barracks.declaredPurposeId = ExplicitRoomTypeRequirementAuthority.BARRACKS_ID;
        addSourceRoom(world, new Rectangle(10, 2, 6, 4), barracks);
        addSourceRoom(world, new Rectangle(18, 2, 6, 4),
                new RoomProfile("Civic Hab Roster Room",
                        "an ordinary staffed hab work roster and local recruitment desk",
                        30, Faction.CIVIC_WARDENS, new String[]{"work chit"}, new char[]{'q'}));

        world.roomPopulationLedgers.add(ledger(
                "pop.rail", 0, "Wardens Rail Intake", "rail intake roster", 6));
        RoomPopulationLedger barracksLedger = ledger(
                "pop.barracks", 1, "Civic Wardens Duty Barracks", "barracks duty roster", 6);
        barracksLedger.declaredRoomPurposeId = ExplicitRoomTypeRequirementAuthority.BARRACKS_ID;
        barracksLedger.facilityPurpose = "guard readiness and bounded reserve muster";
        world.roomPopulationLedgers.add(barracksLedger);
        world.roomPopulationLedgers.add(ledger(
                "pop.local", 2, "Civic Hab Roster", "local hab work roster", 6));
        require(ExplicitRoomTypeRequirementAuthority.installBarracksFixtures(world, 1) == 3,
                "source-policy setup should install exact physical barracks fixtures");
        require(ExplicitRoomTypeRequirementAuthority.ensureGeneratedBarracksDutyStaff(
                        world, new Random(84201L)) == 1,
                "source-policy setup should assign one living barracks duty guard");
        require(ExplicitRoomTypeRequirementAuthority.assessRoom(world, 1).operating(),
                "source-policy setup should expose one operating physical barracks");
        return world;
    }

    private static void addSourceRoom(World world, Rectangle room, RoomProfile profile) {
        int roomId = world.rooms.size();
        world.rooms.add(room);
        world.roomProfiles.add(profile);
        world.roomFactions.add(Faction.CIVIC_WARDENS);
        world.roomSpecials.add(Boolean.FALSE);
        for (int x = room.x; x < room.x + room.width; x++) {
            for (int y = room.y; y < room.y + room.height; y++) {
                boolean boundary = x == room.x || y == room.y
                        || x == room.x + room.width - 1
                        || y == room.y + room.height - 1;
                world.tiles[x][y] = boundary ? '#' : '.';
                world.roomIds[x][y] = roomId;
            }
        }
        world.tiles[room.x][room.y + 1] = 'D';
    }

    private static RoomPopulationLedger ledger(String id, String room, String kind, int capacity) {
        return ledger(id, 0, room, kind, capacity);
    }

    private static RoomPopulationLedger ledger(
            String id, int roomId, String room, String kind, int capacity) {
        RoomPopulationLedger ledger = new RoomPopulationLedger();
        ledger.id = id;
        ledger.roomId = roomId;
        ledger.roomName = room;
        ledger.faction = Faction.CIVIC_WARDENS;
        ledger.sourceKind = kind;
        ledger.sourceLabel = "Civic Wardens " + kind;
        ledger.capacity = capacity;
        ledger.available = capacity;
        return ledger;
    }

    private static PersonnelReplacementRequest request(World world, int turn, String name, int id) {
        NpcEntity dead = NpcEntity.create(Faction.CIVIC_WARDENS, world.zoneType, 5, 5, new Random(id));
        dead.id = "SOURCE-CASUALTY-" + id;
        dead.name = name;
        dead.provenance = new PersonnelProvenanceRecord();
        dead.provenance.originSiteId = world.roomPopulationLedgers.get(0).id;
        return PersonnelProvenanceApi.recordDeathAndScheduleReplacement(
                world, dead, turn, new Random(id), "source-policy casualty");
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

    private static String lastEventContaining(GamePanel game, String expected) {
        for (int i = game.eventLog.size() - 1; i >= 0; i--) {
            String line = game.eventLog.get(i);
            if (line != null && line.contains(expected)) return line;
        }
        return "";
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private Milestone04ReinforcementSourcePolicySmoke() { }
}
