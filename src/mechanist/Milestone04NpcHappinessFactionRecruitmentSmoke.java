package mechanist;

import java.awt.Rectangle;
import java.util.Random;

/** End-to-end smoke for family ownership, population pressure, happiness, departure, and recruitment. */
final class Milestone04NpcHappinessFactionRecruitmentSmoke {
    private static final int DAY_TURNS = GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY;

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        testFactionFamilyAndCrecheOwnership();
        testPopulationSupplyConsumption();
        testHappinessPersistenceAndDeparture();
        testOfficerSchemeCadence();
        testOfficerRecruitmentScheme();
        System.out.println("Milestone 04 NPC happiness and faction recruitment smoke passed.");
    }

    private static void testFactionFamilyAndCrecheOwnership() {
        World world = world(17101L);
        int neutralRoom = addRoom(world, "Neutral Nursery", "public nursery with beds and teaching station",
                Faction.NONE, 4, 4);
        int aurelRoom = addRoom(world, "Aurel Creche", "faction creche with beds and teaching station",
                Faction.HIVER_BLOCK_AUREL, 23, 4);
        world.roomPopulationLedgers.add(creche("creche.neutral", neutralRoom, Faction.NONE));
        world.roomPopulationLedgers.add(creche("creche.aurel", aurelRoom, Faction.HIVER_BLOCK_AUREL));

        DeferredFactionLedgerRecord family = new DeferredFactionLedgerRecord();
        family.id = "distant.hiver";
        family.faction = Faction.HIVER;
        DeferredFactionLedgerRecord duplicate = new DeferredFactionLedgerRecord();
        duplicate.id = "distant.aurel";
        duplicate.faction = Faction.HIVER_BLOCK_AUREL;
        duplicate.lastResolutionWorldTurn = 10L;
        duplicate.recentEvents.add("Aurel family event");
        world.deferredFactionLedgers.add(family);
        world.deferredFactionLedgers.add(duplicate);
        DeferredOutOfSectorSimulationAuthority.ensureLedgers(world, null, 20L);
        require(world.deferredFactionLedgers.size() == 1,
                "related block factions should retain one distant strategic-family ledger");
        require(DeferredOutOfSectorSimulationAuthority.ledgerFor(world, Faction.HIVER_BLOCK_AUREL) != null,
                "block factions should resolve their shared distant ledger");

        FactionCrecheAuthority.tick(world, 1L);
        require(FactionCrecheAuthority.happinessBoostFor(world, Faction.HIVER) == 3,
                "one owned creche should grant the first 3-point family happiness step");
        require(FactionCrecheAuthority.happinessBoostFor(world, Faction.HIVER_BLOCK_AUREL) == 3,
                "owned block creche happiness should be visible to its strategic family");
        require(!world.factionHappinessBoost.containsKey(Faction.CIVIC_WARDENS),
                "neutral creches must not grant every unrelated faction free happiness");
        require(world.crecheCohorts.stream().noneMatch(c -> c != null && c.faction == Faction.NONE),
                "neutral creches must not generate faction child cohorts");
    }

    private static void testPopulationSupplyConsumption() {
        World world = world(17102L);
        RoomPopulationLedger population = new RoomPopulationLedger();
        population.id = "population.hiver";
        population.roomName = "Hiver Hab";
        population.faction = Faction.HIVER_BLOCK_AUREL;
        population.capacity = 12;
        population.assigned = 8;
        world.roomPopulationLedgers.add(population);
        CrecheCohortRecord children = new CrecheCohortRecord();
        children.id = "children.hiver";
        children.faction = Faction.HIVER;
        children.ledgerId = population.id;
        children.birthWorldTurn = 1L;
        children.remaining = 12;
        world.crecheCohorts.add(children);
        EssentialSupplyReserveRecord food = reserve("food", Faction.HIVER, 10);
        EssentialSupplyReserveRecord water = reserve("water", Faction.HIVER_BLOCK_AUREL, 10);
        world.essentialSupplyReserves.add(food);
        world.essentialSupplyReserves.add(water);

        EssentialSupplyProvenanceAuthority.PopulationConsumption use =
                EssentialSupplyProvenanceAuthority.tickPopulationConsumption(world, DAY_TURNS);
        require(use.unitsConsumed() == 3 && food.remaining == 8 && water.remaining == 9,
                "12 residents plus 12 children should consume two food units and one water unit per day");
        require(food.remaining < water.remaining,
                "growing children should exert higher food pressure than water pressure");
        EssentialSupplyReserveRecord restored = EssentialSupplyReserveRecord.parse(food.saveLine());
        require(restored != null && restored.lastPopulationConsumptionWorldTurn == DAY_TURNS
                        && restored.remaining == food.remaining,
                "population-consumption timing and remaining reserve should survive save parsing");
    }

    private static void testHappinessPersistenceAndDeparture() {
        World world = world(17103L);
        NpcEntity worker = NpcEntity.create(Faction.HIVER, world.zoneType, 8, 8, new Random(3));
        worker.id = "HAPPINESS-DEPARTURE-WORKER";
        worker.name = "Unpaid Hab Worker";
        worker.role = "Laborer";
        worker.ageYears = 30;
        worker.hunger = 20;
        worker.thirst = 20;
        worker.happiness = 0;
        worker.lastPaidWorldTurn = 0L;
        worker.happinessLastEvaluatedWorldTurn = 0L;
        worker.severeUnhappinessSinceWorldTurn = 1L;
        worker.lastFactionChangeWorldTurn = -1L;
        worker.happinessReason = "no food, no water, no pay, no bed";
        world.npcs.add(worker);
        NpcEntity recentlyDeprived = NpcEntity.create(Faction.HIVER, world.zoneType, 9, 8, new Random(31));
        recentlyDeprived.id = "RECENTLY-DEPRIVED-WORKER";
        recentlyDeprived.name = "Recently Deprived Worker";
        recentlyDeprived.role = "Laborer";
        recentlyDeprived.ageYears = 28;
        recentlyDeprived.hunger = 20;
        recentlyDeprived.thirst = 20;
        recentlyDeprived.happiness = 0;
        recentlyDeprived.lastPaidWorldTurn = 0L;
        recentlyDeprived.happinessLastEvaluatedWorldTurn = 0L;
        recentlyDeprived.severeUnhappinessSinceWorldTurn = 8L * DAY_TURNS + 2L;
        world.npcs.add(recentlyDeprived);

        NpcEntity restored = NpcEntity.parseLine(worker.saveLine(), world);
        require(restored != null && restored.happiness == 0 && restored.severeUnhappinessSinceWorldTurn == 1L
                        && restored.lastPaidWorldTurn == 0L && restored.happinessReason.contains("no food"),
                "NPC happiness, deprivation duration, pay recency, and reason should survive save parsing");
        requireContains(NpcHappinessAuthority.statusLine(world, worker, 8L * DAY_TURNS),
                "Happiness: 0/100", "player happiness readback");

        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.world = world;
            game.worldTurn = 14L * DAY_TURNS + 2L;
            game.turn = (int) Math.min(Integer.MAX_VALUE, game.worldTurn);
            NpcHappinessAuthority.TickResult tick = NpcHappinessAuthority.tick(game);
            require(tick.departed() == 1 && worker.faction == Faction.NONE,
                    "an NPC under at least three severe deprivations for fourteen days should leave for the general populace");
            require(recentlyDeprived.faction == Faction.HIVER,
                    "severe deprivation lasting less than seven days must not trigger faction departure");
            require("Unaffiliated Resident".equals(worker.factionRankTitle)
                            && "Left Faction".equals(worker.state),
                    "departure should retain the NPC as an unaffiliated resident instead of deleting them");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static void testOfficerRecruitmentScheme() {
        World world = world(17104L);
        int habRoom = addRoom(world, "Hiver Barracks", "barracks dormitory with cots and quarters",
                Faction.HIVER, 4, 4);
        RoomPopulationLedger destination = new RoomPopulationLedger();
        destination.id = "population.hiver.recruitment";
        destination.roomId = habRoom;
        destination.roomName = "Hiver Barracks";
        destination.faction = Faction.HIVER;
        destination.capacity = 6;
        destination.available = 5;
        world.roomPopulationLedgers.add(destination);

        NpcEntity officer = NpcEntity.create(Faction.HIVER, world.zoneType, 8, 8, new Random(4));
        officer.id = "HIVER-RECRUITING-OFFICER";
        officer.name = "Officer Mara Vale";
        officer.role = "Faction Officer";
        officer.factionRank = 3;
        officer.ageYears = 34;
        officer.happiness = 80;
        world.npcs.add(officer);
        NpcEntity rival = NpcEntity.create(Faction.CIVIC_WARDENS, world.zoneType, 10, 8, new Random(5));
        rival.id = "UNHAPPY-WARDEN";
        rival.name = "Warden Pell";
        rival.role = "Watch Member";
        rival.ageYears = 29;
        rival.happiness = 30;
        rival.lastFactionChangeWorldTurn = -1L;
        rival.happinessLastEvaluatedWorldTurn = 30L * DAY_TURNS;
        world.npcs.add(rival);

        int happierDestinationChance = NpcHappinessAuthority.vulnerabilityChance(world, rival, Faction.HIVER, 12);
        officer.happiness = 10;
        int unhappyDestinationChance = NpcHappinessAuthority.vulnerabilityChance(world, rival, Faction.HIVER, 12);
        require(happierDestinationChance > unhappyDestinationChance,
                "relative destination happiness should materially increase rival recruitability");
        officer.happiness = 80;

        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.world = world;
            game.worldTurn = 30L * DAY_TURNS;
            game.turn = 30 * DAY_TURNS;
            game.rng = new Random(2L);
            FactionStrategicPlan plan = FactionStrategicPlan.create(Faction.HIVER, new Random(6), game.turn);
            plan.immediateGoal = "recruit a rival faction member";
            plan.scheme = "recruit a disaffected rival member";
            plan.schemeTargetFaction = Faction.CIVIC_WARDENS;
            int beforeAssigned = destination.assigned;
            FactionStrategySimulationApi.resolveExecution(game, plan);
            require(plan.success == 1 && plan.failure == 0,
                    "officer-led recruitment plan should resolve through the faction strategy lifecycle");
            require(rival.faction == Faction.HIVER && rival.lastFactionChangeWorldTurn == game.worldTurn,
                    "successful scheme should transfer the unhappy rival's tracked faction membership");
            require(destination.assigned == beforeAssigned + 1,
                    "successful recruitment should consume destination population capacity");
            requireContains(plan.lastOutcome, officer.name, "recruiting officer outcome");
            requireContains(plan.lastOutcome, rival.name, "recruited NPC outcome");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static void testOfficerSchemeCadence() {
        int turn = 1000;
        FactionStrategicPlan quick = cadencePlan("PLAN-QUICK", "Officer Quick", 80);
        FactionStrategicPlan deliberate = cadencePlan("PLAN-DELIBERATE", "Officer Deliberate", 125);
        quick.chooseNewImmediateGoal(new Random(77L), turn);
        deliberate.chooseNewImmediateGoal(new Random(77L), turn);
        require(quick.phaseUntilTurn != deliberate.phaseUntilTurn,
                "officers given identical planning rolls should receive different personal planning deadlines");
        require(quick.cadenceCycle == 1 && deliberate.cadenceCycle == 1
                        && quick.lastPhaseJitterHours >= -4 && quick.lastPhaseJitterHours <= 4
                        && deliberate.lastPhaseJitterHours >= -4 && deliberate.lastPhaseJitterHours <= 4,
                "planning should retain a bounded cycle-specific jitter and cadence sequence");
        requireContains(quick.shortLine(), "cadence 80%", "quick officer cadence readback");
        requireContains(deliberate.shortLine(), "cycle jitter", "officer jitter readback");

        quick.phase = "EXECUTION";
        deliberate.phase = "EXECUTION";
        quick.advancePhase(new Random(88L), 5000);
        deliberate.advancePhase(new Random(88L), 5000);
        require("COOLDOWN".equals(quick.phase) && "COOLDOWN".equals(deliberate.phase)
                        && quick.phaseUntilTurn != deliberate.phaseUntilTurn,
                "personal cadence should stagger cooldown after schemes resolve on the same turn");
        requireContains(quick.history.get(quick.history.size() - 1), "Cooling down for",
                "cooldown duration history");

        FactionStrategicPlan restored = FactionStrategicPlan.parse(deliberate.saveLine());
        require(restored != null && restored.officerCadencePct == deliberate.officerCadencePct
                        && restored.cadenceCycle == deliberate.cadenceCycle
                        && restored.lastPhaseDurationTurns == deliberate.lastPhaseDurationTurns
                        && restored.lastPhaseJitterHours == deliberate.lastPhaseJitterHours,
                "officer pace, cycle, phase duration, and jitter should survive save parsing");
        int generatedCadence = FactionStrategicPlan.cadenceFor(
                Faction.CIVIC_WARDENS, "Officer Generated", "Deputy Generated");
        require(generatedCadence >= 80 && generatedCadence <= 125,
                "generated officer cadence should stay inside the 80-125 percent design band");
    }

    private static FactionStrategicPlan cadencePlan(String id, String leader, int cadencePct) {
        FactionStrategicPlan plan = new FactionStrategicPlan();
        plan.id = id;
        plan.faction = Faction.HIVER;
        plan.leaderName = leader;
        plan.deputyName = leader + " Deputy";
        plan.officerCadencePct = cadencePct;
        return plan;
    }

    private static EssentialSupplyReserveRecord reserve(String category, Faction faction, int stock) {
        EssentialSupplyReserveRecord reserve = new EssentialSupplyReserveRecord();
        reserve.id = "essential." + category + ".smoke";
        reserve.category = category;
        reserve.itemName = "food".equals(category) ? "Emergency rations" : "Clean water";
        reserve.faction = faction;
        reserve.capacity = stock;
        reserve.remaining = stock;
        reserve.restockIntervalTurns = 30 * DAY_TURNS;
        reserve.nextRestockWorldTurn = 30L * DAY_TURNS;
        reserve.lastPopulationConsumptionWorldTurn = 0L;
        return reserve;
    }

    private static RoomPopulationLedger creche(String id, int roomId, Faction faction) {
        RoomPopulationLedger ledger = new RoomPopulationLedger();
        ledger.id = id;
        ledger.roomId = roomId;
        ledger.roomName = id;
        ledger.faction = faction;
        ledger.sourceKind = "creche population room";
        ledger.capacity = 12;
        ledger.careProviders = 1;
        ledger.crecheFoodStorageUnits = 1;
        ledger.crecheWaterStorageUnits = 1;
        ledger.crecheBedUnits = 3;
        ledger.crecheTeachingStations = 1;
        return ledger;
    }

    private static World world(long seed) {
        World world = new World(seed, 78, 58);
        world.zoneType = ZoneType.HAB_STACK;
        world.zoneName = "Happiness Smoke Hab";
        world.floor = 4;
        for (int x = 0; x < world.w; x++) for (int y = 0; y < world.h; y++) world.tiles[x][y] = '#';
        return world;
    }

    private static int addRoom(World world, String name, String description, Faction faction, int x, int y) {
        int index = world.rooms.size();
        Rectangle room = new Rectangle(x, y, 14, 10);
        world.carve(room);
        world.rooms.add(room);
        world.roomProfiles.set(index, new RoomProfile(name, description, 60, faction,
                new String[]{"Trade chit"}, new char[]{'c'}));
        world.roomFactions.set(index, faction);
        world.roomSpecials.set(index, Boolean.FALSE);
        for (int px = room.x + 1; px < room.x + room.width - 1; px++) {
            for (int py = room.y + 1; py < room.y + room.height - 1; py++) world.tiles[px][py] = '.';
        }
        world.tiles[room.x + 2][room.y + 2] = 'c';
        return index;
    }

    private static void requireContains(String actual, String expected, String label) {
        require(actual != null && actual.contains(expected), label + " missing '" + expected + "': " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone04NpcHappinessFactionRecruitmentSmoke() { }
}
