package mechanist;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

/** Smoke for capped crèche happiness, child demand, world-time maturation, and bulk musters. */
final class Milestone04CrecheGenerationMaturitySmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        World world = crecheWorld();
        require(world.roomPopulationLedgers.get(0).roomId != world.roomPopulationLedgers.get(1).roomId,
                "the two operating creches should occupy distinct physical rooms");
        int created = FactionCrecheAuthority.tick(world, 0L);
        require(created == 2 && world.crecheCohorts.size() == 2,
                "each operating crèche should create one aggregate newborn cohort for the world year");
        FactionCrecheAuthority.BuildingReadiness operating = FactionCrecheAuthority.buildingReadiness(
                world, world.roomPopulationLedgers.get(0));
        require(operating.operating() && operating.floorArea() >= FactionCrecheAuthority.MINIMUM_FLOOR_AREA,
                "a full-size room with care, food, water, beds, and teaching should count as an operating crèche");
        World missingFoodWorld = crecheWorld();
        RoomPopulationLedger missingFood = missingFoodWorld.roomPopulationLedgers.get(0);
        MapObjectState removedFood = removeCrecheFixture(missingFoodWorld, missingFood.roomId,
                ExplicitRoomTypeRequirementAuthority.Capability.SECURE_FOOD_STORAGE);
        FactionCrecheAuthority.BuildingReadiness missingFoodReadiness =
                FactionCrecheAuthority.buildingReadiness(missingFoodWorld, missingFood);
        require(removedFood != null && missingFood.crecheFoodStorageUnits == 1
                        && !missingFoodReadiness.operating()
                        && missingFoodReadiness.blockers().contains("secure food storage 0/1"),
                "removing live secure-food fixture evidence should block the building despite a stale legacy counter");
        World undersized = crecheWorld();
        Rectangle originalRoom = undersized.rooms.get(0);
        undersized.rooms.set(0, new Rectangle(originalRoom.x, originalRoom.y, 4, 4));
        require(!FactionCrecheAuthority.buildingReadiness(undersized, undersized.roomPopulationLedgers.get(0)).operating(),
                "a four-by-four room should not satisfy the explicit six-by-six creche minimum");
        require(FactionCrecheAuthority.happinessBoost(1) == 3
                        && FactionCrecheAuthority.happinessBoost(2) == 5
                        && FactionCrecheAuthority.happinessBoost(10) == 25
                        && FactionCrecheAuthority.happinessBoost(20) == 25,
                "crèche happiness should scale to a +25 cap at ten buildings");
        require(world.factionHappinessBoost.getOrDefault(Faction.CIVIC_WARDENS, 0) == 5,
                "two faction crèches should write a +5 happiness boost to world state");

        FactionCrecheAuthority.Status newborn = FactionCrecheAuthority.status(
                world, Faction.CIVIC_WARDENS, 0L);
        require(newborn.children() == 6 && newborn.matureYoungAdults() == 0,
                "newborn cohorts should remain aggregate children rather than recruitable NPCs");
        requireContains(newborn.line(), "happiness +5", "scaled happiness readback");
        requireContains(newborn.line(), "age 0/16 years", "long-form maturation readback");
        require(newborn.careProviders() == 2 && newborn.childCareCapacity() == 24,
                "one care provider per crèche should support twelve children at dense four-per-bed occupancy");
        requireContains(newborn.line(), "support up to 24 children at four per bed", "dense care capacity readback");

        World ordinary = ordinaryWorld();
        TraderSession crecheTrader = new TraderSession();
        TraderSession ordinaryTrader = new TraderSession();
        PopulationMarketPressureAuthority.Profile crechePressure = PopulationMarketPressureAuthority.apply(
                crecheTrader, world, Faction.CIVIC_WARDENS, 0L, 0);
        PopulationMarketPressureAuthority.Profile ordinaryPressure = PopulationMarketPressureAuthority.apply(
                ordinaryTrader, ordinary, Faction.CIVIC_WARDENS, 0L, 0);
        require(crechePressure.pressureFor("Emergency rations", "food").demandUnits()
                        > ordinaryPressure.pressureFor("Emergency rations", "food").demandUnits(),
                "equal-capacity crèches with growing cohorts should demand more food than ordinary hab rosters");
        requireContains(crechePressure.contextLines().toString(),
                "growing crèche cohorts add 6 children", "child food-pressure driver");

        CrecheCohortRecord demandCohort = new CrecheCohortRecord();
        demandCohort.id = "demand-only";
        demandCohort.faction = Faction.CIVIC_WARDENS;
        demandCohort.birthWorldTurn = 1L;
        demandCohort.birthYear = 0L;
        demandCohort.remaining = 4;
        World cohortOnly = new World(84301L, 24, 24);
        cohortOnly.roomPopulationLedgers.clear();
        cohortOnly.crecheCohorts.clear();
        cohortOnly.crecheCohorts.add(demandCohort);
        int immatureFood = PopulationMarketPressureAuthority.apply(new TraderSession(), cohortOnly,
                Faction.CIVIC_WARDENS, demandCohort.maturityTurn() - 1L, 0)
                .pressureFor("Emergency rations", "food").demandUnits();
        int matureFood = PopulationMarketPressureAuthority.apply(new TraderSession(), cohortOnly,
                Faction.CIVIC_WARDENS, demandCohort.maturityTurn(), 0)
                .pressureFor("Emergency rations", "food").demandUnits();
        require(immatureFood > matureFood,
                "a cohort should stop adding child growth-food demand once it reaches young adulthood");

        Properties saved = new Properties();
        Persistence.writeWorldState(world, saved);
        World restored = new World(84302L, 24, 24);
        Persistence.readWorldState(restored, saved);
        require(restored.crecheCohorts.size() == 2
                        && restored.crecheCohorts.get(0).remaining == 3
                        && restored.factionHappinessBoost.getOrDefault(Faction.CIVIC_WARDENS, 0) == 5,
                "cohort birth time, remaining children, and faction happiness should survive world save/load");

        NpcEntity parent = NpcEntity.create(Faction.CIVIC_WARDENS, world.zoneType, 7, 7, new Random(31));
        parent.id = "PARENT-31";
        parent.name = "Mara Vale";
        world.npcs.add(parent);
        require(FactionCrecheAuthority.registerPregnancy(parent, 100L),
                "faction member should accept a recorded pregnancy due turn");
        NpcEntity persistedParent = NpcEntity.parseLine(parent.saveLine(), world);
        require(persistedParent != null && persistedParent.pregnancyDueWorldTurn == 100L,
                "pregnancy due turn should survive NPC persistence");
        require(FactionCrecheAuthority.tick(world, 100L) == 1 && parent.pregnancyDueWorldTurn == 0L,
                "a due faction birth should enter an operating crèche and clear the completed pregnancy");
        CrecheCohortRecord parentCohort = world.crecheCohorts.stream()
                .filter(cohort -> cohort != null && "faction-parent-birth".equals(cohort.intakeMode))
                .findFirst().orElseThrow(() -> new AssertionError("parent birth should create a distinct cohort"));
        require("PARENT-31".equals(parentCohort.parentNpcId) && "Mara Vale".equals(parentCohort.parentName),
                "parent-origin cohort should preserve the faction member identity");
        CrecheCohortRecord parsedParentCohort = CrecheCohortRecord.parse(parentCohort.saveLine());
        require(parsedParentCohort != null && "faction-parent-birth".equals(parsedParentCohort.intakeMode)
                        && "Mara Vale".equals(parsedParentCohort.parentName),
                "parent birth origin should persist with the cohort");

        World fullCare = crecheWorld();
        while (fullCare.roomPopulationLedgers.size() > 1) fullCare.roomPopulationLedgers.remove(fullCare.roomPopulationLedgers.size() - 1);
        fullCare.crecheCohorts.clear();
        CrecheCohortRecord full = new CrecheCohortRecord();
        full.id = "full-care";
        full.faction = Faction.CIVIC_WARDENS;
        full.ledgerId = fullCare.roomPopulationLedgers.get(0).id;
        full.roomId = 0;
        full.birthWorldTurn = 1L;
        full.birthYear = 0L;
        full.remaining = 12;
        fullCare.crecheCohorts.add(full);
        NpcEntity waitingParent = NpcEntity.create(Faction.CIVIC_WARDENS, fullCare.zoneType, 8, 8, new Random(32));
        waitingParent.id = "WAITING-PARENT";
        fullCare.npcs.add(waitingParent);
        FactionCrecheAuthority.registerPregnancy(waitingParent, 10L);
        FactionCrecheAuthority.tick(fullCare, 10L);
        require(waitingParent.pregnancyDueWorldTurn == 10L
                        && fullCare.crecheCohorts.stream().noneMatch(cohort -> "WAITING-PARENT".equals(cohort.parentNpcId)),
                "a full care and bed allocation should leave the due birth waiting instead of deleting or inventing a child");

        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.world = world;
            game.turn = 100;
            game.worldTurn = 100L;
            NpcEntity representative = NpcEntity.factionRepresentative(Faction.CIVIC_WARDENS, 4, 4, new Random(2));
            game.world.npcs.add(representative);
            game.activeInteractionNpc = representative;
            game.panelMode = GamePanel.PanelMode.DIALOGUE;
            game.screen = GamePanel.Screen.PANEL;
            game.setSize(1280, 720);
            render(game);
            require(hasButton(game, "Muster Cohort"),
                    "representative dialogue should expose mature cohort recruitment: " + buttonLabels(game));

            int minorTurn = game.turn;
            int minorPopulation = PersonnelPopulationApi.countLivingFactionActors(
                    game.world, Faction.CIVIC_WARDENS);
            game.musterCrecheCohortWithActiveNpc();
            require(game.turn == minorTurn && PersonnelPopulationApi.countLivingFactionActors(
                            game.world, Faction.CIVIC_WARDENS) == minorPopulation,
                    "attempting to recruit children should preserve time and materialize nobody");
            requireContains(lastEvent(game), "years away", "minor recruitment refusal");

            long maturityTurn = world.crecheCohorts.get(0).maturityTurn();
            game.worldTurn = maturityTurn;
            FactionCrecheAuthority.Status mature = FactionCrecheAuthority.status(
                    world, Faction.CIVIC_WARDENS, game.worldTurn);
            require(mature.matureYoungAdults() == 6,
                    "the original two cohorts should become recruitable only after sixteen world years");
            int beforeMusterTurn = game.turn;
            game.musterCrecheCohortWithActiveNpc();
            require(game.turn == beforeMusterTurn + 1,
                    "successful bulk cohort muster should spend one turn");
            ArrayList<NpcEntity> recruits = new ArrayList<>();
            for (NpcEntity npc : world.npcs) if (npc != null && "Young Adult Recruit".equals(npc.role)) recruits.add(npc);
            require(recruits.size() == 6,
                    "one cohort action should materialize the bounded six-person bulk group");
            for (NpcEntity recruit : recruits) {
                require(recruit.ageYears >= 16 && "young adult".equals(recruit.ageBand),
                        "only mature young adults should enter the recruited group");
                require("Young Adult Recruit".equals(recruit.role)
                                && recruit.provenance != null
                                && "creche-raised".equals(recruit.provenance.originMode),
                        "cohort recruits should retain readable crèche upbringing provenance");
            }
            FactionCrecheAuthority.Status after = FactionCrecheAuthority.status(
                    world, Faction.CIVIC_WARDENS, game.worldTurn);
            require(after.matureYoungAdults() == 0,
                    "bulk muster should consume the six recruited members from mature cohorts");
            require(world.factionHappinessBoost.getOrDefault(Faction.CIVIC_WARDENS, 0) == 5,
                    "recruiting a mature cohort should not remove the crèche building happiness benefit");

            game.worldTurn = parentCohort.maturityTurn();
            int beforeParentMuster = world.npcs.size();
            game.musterCrecheCohortWithActiveNpc();
            require(world.npcs.size() == beforeParentMuster + 1,
                    "the faction-parent child should become recruitable only after its own sixteen-year maturity turn");
            NpcEntity parentRecruit = world.npcs.get(world.npcs.size() - 1);
            requireContains(parentRecruit.provenance.backstory, "born to faction member Mara Vale",
                    "parent birth provenance on mature recruit");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static World crecheWorld() {
        World world = new World(84300L, 32, 32);
        int northRoom = addRoom(world, new Rectangle(2, 2, 6, 6), "Civic Faction Creche",
                ExplicitRoomTypeRequirementAuthority.CRECHE_ID);
        int southRoom = addRoom(world, new Rectangle(12, 2, 6, 6), "Civic Faction Nursery",
                ExplicitRoomTypeRequirementAuthority.CRECHE_ID);
        world.npcs.clear();
        world.roomPopulationLedgers.clear();
        world.crecheCohorts.clear();
        world.roomPopulationLedgers.add(ledger("pop.creche.a", northRoom, "North Crèche",
                "creche population ledger", ExplicitRoomTypeRequirementAuthority.CRECHE_ID));
        world.roomPopulationLedgers.add(ledger("pop.creche.b", southRoom, "South Nursery",
                "nursery population ledger", ExplicitRoomTypeRequirementAuthority.CRECHE_ID));
        ExplicitRoomTypeRequirementAuthority.installCrecheFixtures(world, northRoom);
        ExplicitRoomTypeRequirementAuthority.installCrecheFixtures(world, southRoom);
        ExplicitRoomTypeRequirementAuthority.ensureGeneratedCareProviders(world, new Random(84300L));
        return world;
    }

    private static World ordinaryWorld() {
        World world = new World(84310L, 32, 32);
        int northRoom = addRoom(world, new Rectangle(2, 2, 6, 6), "North Ordinary Hab Room", "");
        int southRoom = addRoom(world, new Rectangle(12, 2, 6, 6), "South Ordinary Hab Room", "");
        world.npcs.clear();
        world.roomPopulationLedgers.clear();
        world.crecheCohorts.clear();
        world.roomPopulationLedgers.add(ledger("pop.hab.a", northRoom, "North Hab",
                "local hab work roster", ""));
        world.roomPopulationLedgers.add(ledger("pop.hab.b", southRoom, "South Hab",
                "local hab work roster", ""));
        return world;
    }

    private static int addRoom(World world, Rectangle room, String name, String declaredPurposeId) {
        int roomId = world.rooms.size();
        world.rooms.add(room);
        RoomProfile profile = RoomProfile.themedRoom(name, name, 20, Faction.CIVIC_WARDENS,
                new String[]{"Emergency rations"}, new char[]{'b'});
        profile.declaredPurposeId = declaredPurposeId == null ? "" : declaredPurposeId;
        world.roomProfiles.add(profile);
        world.roomFactions.add(Faction.CIVIC_WARDENS);
        world.roomSpecials.add(Boolean.FALSE);
        for (int x = room.x; x < room.x + room.width; x++) {
            for (int y = room.y; y < room.y + room.height; y++) {
                boolean boundary = x == room.x || y == room.y
                        || x == room.x + room.width - 1 || y == room.y + room.height - 1;
                world.tiles[x][y] = boundary ? '#' : '.';
                world.roomIds[x][y] = roomId;
            }
        }
        world.tiles[room.x + room.width / 2][room.y] = 'D';
        return roomId;
    }

    private static RoomPopulationLedger ledger(String id, int roomId, String room, String kind,
                                                String declaredPurposeId) {
        RoomPopulationLedger ledger = new RoomPopulationLedger();
        ledger.id = id;
        ledger.roomId = roomId;
        ledger.roomName = room;
        ledger.sourceKind = kind;
        ledger.sourceLabel = "Civic Wardens " + kind;
        ledger.faction = Faction.CIVIC_WARDENS;
        ledger.capacity = 8;
        ledger.available = 8;
        ledger.declaredRoomPurposeId = declaredPurposeId == null ? "" : declaredPurposeId;
        return ledger;
    }

    private static MapObjectState removeCrecheFixture(World world, int roomId,
                                                       ExplicitRoomTypeRequirementAuthority.Capability capability) {
        for (int i = 0; i < world.mapObjects.size(); i++) {
            MapObjectState object = world.mapObjects.get(i);
            if (object == null || world.roomIdAt(object.x, object.y) != roomId
                    || !ExplicitRoomTypeRequirementAuthority.CRECHE_ID.equals(
                    MapObjectState.stockValue(object.stockState, "roomPurpose"))
                    || !capability.name().equals(MapObjectState.stockValue(object.stockState, "capability"))) {
                continue;
            }
            world.mapObjects.remove(i);
            char underlying = MapObjectState.underlyingTileFromStock(object.stockState);
            world.tiles[object.x][object.y] = underlying == 0 ? '.' : underlying;
            return object;
        }
        return null;
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

    private Milestone04CrecheGenerationMaturitySmoke() { }
}
