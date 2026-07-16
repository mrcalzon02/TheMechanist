package mechanist;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Owns crèche happiness, long-form birth cohorts, maturation, and young-adult musters. */
final class FactionCrecheAuthority {
    static final int MAX_HAPPINESS_BOOST = 25;
    static final int CRECHES_FOR_MAX_HAPPINESS = 10;
    static final int MATURITY_YEARS = 16;
    static final int MAX_MUSTER_GROUP = 6;
    static final int MINIMUM_FLOOR_AREA = 24;

    record Status(int creches, int happinessBoost, int careProviders, int childCareCapacity,
                  int children, int matureYoungAdults,
                  int oldestAgeYears, long turnsUntilOldestMatures, String line) { }

    record MusterResult(boolean success, int recruited, String message, List<NpcEntity> personnel) { }

    record BuildingReadiness(boolean plannedCreche, boolean operating, int floorArea,
                             List<String> blockers, String line) { }

    private FactionCrecheAuthority() { }

    static int tick(World world, long worldTurn) {
        if (world == null) return 0;
        if (world.crecheCohorts == null) return 0;
        int created = acceptDueFactionBirths(world, worldTurn);
        long birthYear = Math.max(0L, worldTurn / AgeAndWorldTimeAuthority.TURNS_PER_YEAR);
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            BuildingReadiness readiness = buildingReadiness(world, ledger);
            if (!readiness.operating() || !FactionIdentityAuthority.aligned(ledger.faction)) continue;
            boolean exists = false;
            for (CrecheCohortRecord cohort : world.crecheCohorts) {
                if (cohort != null && ledger.id.equals(cohort.ledgerId) && cohort.birthYear == birthYear) {
                    exists = true;
                    break;
                }
            }
            if (exists) continue;
            int currentChildren = immatureChildren(world, ledger.id, worldTurn);
            int openCare = Math.max(0, childCareCapacity(ledger) - currentChildren);
            int count = Math.min(openCare, Math.max(1, ledger.careProviders * 3));
            if (count <= 0) continue;
            CrecheCohortRecord cohort = new CrecheCohortRecord();
            cohort.id = "creche." + Math.abs((ledger.id + ":" + birthYear).hashCode());
            cohort.faction = ledger.faction == null ? Faction.NONE : ledger.faction;
            cohort.ledgerId = ledger.id;
            cohort.roomId = ledger.roomId;
            cohort.birthWorldTurn = Math.max(1L, worldTurn);
            cohort.birthYear = birthYear;
            cohort.remaining = count;
            cohort.intakeMode = "abstract-abandoned-intake";
            cohort.parentName = "unrecorded or abandoned child intake";
            world.crecheCohorts.add(cohort);
            created++;
        }
        world.factionHappinessBoost.clear();
        java.util.EnumMap<Faction,Integer> crechesByFamily = new java.util.EnumMap<>(Faction.class);
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger == null || !FactionIdentityAuthority.aligned(ledger.faction)
                    || !buildingReadiness(world, ledger).operating()) continue;
            Faction family = FactionIdentityAuthority.strategicFamily(ledger.faction);
            crechesByFamily.put(family, crechesByFamily.getOrDefault(family, 0) + 1);
        }
        for (java.util.Map.Entry<Faction,Integer> entry : crechesByFamily.entrySet()) {
            world.factionHappinessBoost.put(entry.getKey(), happinessBoost(entry.getValue()));
        }
        return created;
    }

    static Status status(World world, Faction faction, long worldTurn) {
        Faction target = faction == null ? Faction.NONE : faction;
        if (world == null || target == Faction.NONE) {
            return new Status(0, 0, 0, 0, 0, 0, 0, 0,
                    "Crèche program unavailable: this representative has no faction population.");
        }
        tick(world, worldTurn);
        int creches = 0;
        int plannedCreches = 0;
        ArrayList<String> buildingBlockers = new ArrayList<>();
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (!FactionIdentityAuthority.sameFamily(ledger.faction, target)) continue;
            BuildingReadiness readiness = buildingReadiness(world, ledger);
            if (!readiness.plannedCreche()) continue;
            plannedCreches++;
            if (readiness.operating()) creches++;
            else buildingBlockers.add(ledger.roomName + ": " + String.join(", ", readiness.blockers()));
        }
        int children = 0;
        int mature = 0;
        int careProviders = 0;
        int careCapacity = 0;
        int oldest = 0;
        long soonest = Long.MAX_VALUE;
        for (CrecheCohortRecord cohort : world.crecheCohorts) {
            if (cohort == null || !FactionIdentityAuthority.sameFamily(cohort.faction, target) || cohort.remaining <= 0) continue;
            int age = cohort.ageYears(worldTurn);
            oldest = Math.max(oldest, age);
            if (age >= MATURITY_YEARS) mature += cohort.remaining;
            else {
                children += cohort.remaining;
                soonest = Math.min(soonest, cohort.maturityTurn() - worldTurn);
            }
        }
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (buildingReadiness(world, ledger).operating()
                    && FactionIdentityAuthority.sameFamily(ledger.faction, target)) {
                careProviders += Math.max(0, ledger.careProviders);
                careCapacity += childCareCapacity(ledger);
            }
        }
        int happiness = happinessBoost(creches);
        String line;
        if (creches == 0) {
            line = plannedCreches == 0
                    ? "Crèche program: no faction crèche is planned; happiness boost +0 and no generational cohort source."
                    : "Crèche program blocked: 0/" + plannedCreches + " planned crèches meet the operating baseline; happiness boost +0; missing "
                    + String.join("; ", buildingBlockers) + ".";
        } else if (mature > 0) {
            line = "Crèche program: " + creches + " crèche" + (creches == 1 ? "" : "s")
                    + " provide faction happiness +" + happiness + "; " + mature
                    + " mature young adult" + (mature == 1 ? " is" : "s are")
                    + " ready for a bulk cohort muster; " + children + " children remain in long-term care; "
                    + careProviders + " care provider" + (careProviders == 1 ? "" : "s")
                    + " support up to " + careCapacity + " children at four per bed.";
        } else {
            long turns = soonest == Long.MAX_VALUE ? (long) MATURITY_YEARS * AgeAndWorldTimeAuthority.TURNS_PER_YEAR : Math.max(0L, soonest);
            line = "Crèche program: " + creches + " crèche" + (creches == 1 ? "" : "s")
                    + " provide faction happiness +" + happiness + "; " + children
                    + " children are aging in world time; oldest cohort age " + oldest + "/" + MATURITY_YEARS
                    + " years, about " + turns + " turns until the first young-adult muster; "
                    + careProviders + " care provider" + (careProviders == 1 ? "" : "s")
                    + " support up to " + careCapacity + " children at four per bed.";
        }
        return new Status(creches, happiness, careProviders, careCapacity, children, mature, oldest,
                soonest == Long.MAX_VALUE ? 0L : Math.max(0L, soonest), line);
    }

    static String representativeLine(GamePanel game, NpcEntity representative) {
        if (game == null || representative == null || !representative.isFactionRepresentative()) {
            return "Crèche program unavailable: speak with a faction representative.";
        }
        return status(game.world, representative.faction, game.worldTurn).line();
    }

    static MusterResult muster(World world, Faction faction, long worldTurn, Random random) {
        Faction target = faction == null ? Faction.NONE : faction;
        if (world == null || target == Faction.NONE) {
            return new MusterResult(false, 0, "Crèche cohort muster unavailable: no faction population is selected.", List.of());
        }
        Status status = status(world, target, worldTurn);
        if (status.creches() <= 0) {
            return new MusterResult(false, 0, "Crèche cohort muster blocked: this faction has no operating crèche.", List.of());
        }
        if (status.matureYoungAdults() <= 0) {
            return new MusterResult(false, 0, "Crèche cohort muster is years away: oldest cohort age "
                    + status.oldestAgeYears() + "/" + MATURITY_YEARS + "; children cannot be recruited.", List.of());
        }
        int capacity = PersonnelPopulationApi.replacementCapacityForFaction(world, target);
        int living = PersonnelPopulationApi.countLivingFactionActors(world, target);
        int slots = Math.max(0, capacity - living);
        if (slots <= 0) {
            return new MusterResult(false, 0, "Crèche cohort muster blocked: staffed housing and faction-room capacity is full at "
                    + living + "/" + capacity + ".", List.of());
        }
        ArrayList<CrecheCohortRecord> cohorts = new ArrayList<>();
        for (CrecheCohortRecord cohort : world.crecheCohorts) {
            if (cohort != null && FactionIdentityAuthority.sameFamily(cohort.faction, target)
                    && cohort.remaining > 0 && cohort.ageYears(worldTurn) >= MATURITY_YEARS) {
                cohorts.add(cohort);
            }
        }
        cohorts.sort(Comparator.comparingLong(cohort -> cohort.birthWorldTurn));
        int targetCount = Math.min(Math.min(status.matureYoungAdults(), slots), MAX_MUSTER_GROUP);
        ArrayList<NpcEntity> recruited = new ArrayList<>();
        Random rng = random == null ? new Random(world.seed ^ worldTurn ^ target.ordinal()) : random;
        for (CrecheCohortRecord cohort : cohorts) {
            RoomPopulationLedger ledger = PersonnelPopulationApi.ledgerById(world, cohort.ledgerId);
            while (cohort.remaining > 0 && recruited.size() < targetCount) {
                Point point = spawnPoint(world, ledger, rng);
                if (point == null) break;
                NpcEntity npc = NpcEntity.create(target, world.zoneType, point.x, point.y, rng);
                npc.role = "Young Adult Recruit";
                npc.state = "Crèche Cohort Muster";
                npc.birthWorldTurn = cohort.birthWorldTurn;
                npc.ageYears = cohort.ageYears(worldTurn);
                npc.ageBand = AgeAndWorldTimeAuthority.bandForAge(npc.ageYears);
                npc.ensureRankIdentity(rng);
                attachProvenance(npc, world, ledger, cohort, worldTurn);
                NpcPortraitSelectionAuthority.assignForSpawn(npc, world);
                world.npcs.add(npc);
                cohort.remaining--;
                if (ledger != null) ledger.assigned++;
                recruited.add(npc);
            }
            if (recruited.size() >= targetCount) break;
        }
        if (recruited.isEmpty()) {
            return new MusterResult(false, 0, "Crèche cohort muster delayed: no open arrival point is available near the crèche.", List.of());
        }
        return new MusterResult(true, recruited.size(), "Mustered " + recruited.size() + " " + target.label
                + " young adult" + (recruited.size() == 1 ? "" : "s")
                + " from mature crèche cohorts; no child was recruited; staffed capacity is now "
                + (living + recruited.size()) + "/" + capacity + ".", List.copyOf(recruited));
    }

    static boolean isCrecheLedger(RoomPopulationLedger ledger) {
        if (ledger == null || ledger.capacity <= 0) return false;
        String text = (safe(ledger.sourceKind) + " " + safe(ledger.sourceLabel) + " " + safe(ledger.roomName)
                + " " + safe(ledger.facilityPurpose)).toLowerCase(Locale.ROOT);
        return text.contains("creche") || text.contains("crèche") || text.contains("daycare")
                || text.contains("nursery") || text.contains("rookery");
    }

    static BuildingReadiness buildingReadiness(World world, RoomPopulationLedger ledger) {
        if (!isCrecheLedger(ledger)) {
            return new BuildingReadiness(false, false, 0, List.of("room is not designated as a crèche"),
                    "Not a planned crèche.");
        }
        int floorArea = 0;
        if (world != null && ledger.roomId >= 0 && ledger.roomId < world.rooms.size()) {
            java.awt.Rectangle room = world.rooms.get(ledger.roomId);
            if (room != null) floorArea = Math.max(0, room.width) * Math.max(0, room.height);
        }
        ArrayList<String> blockers = new ArrayList<>();
        if (floorArea < MINIMUM_FLOOR_AREA) blockers.add("floor area " + floorArea + "/" + MINIMUM_FLOOR_AREA);
        if (ledger.careProviders < 1) blockers.add("care provider 0/1");
        if (ledger.crecheFoodStorageUnits < 1) blockers.add("food storage 0/1");
        if (ledger.crecheWaterStorageUnits < 1) blockers.add("water storage 0/1");
        if (ledger.crecheBedUnits < 1) blockers.add("child bed unit 0/1");
        if (ledger.crecheTeachingStations < 1) blockers.add("teaching station 0/1");
        boolean operating = blockers.isEmpty();
        String line = operating
                ? "Operating crèche: floor area " + floorArea + "; providers " + ledger.careProviders
                + "; food storage " + ledger.crecheFoodStorageUnits + "; water storage " + ledger.crecheWaterStorageUnits
                + "; child bed units " + ledger.crecheBedUnits + " at four children each; teaching stations "
                + ledger.crecheTeachingStations + "."
                : "Planned crèche blocked: " + String.join(", ", blockers) + ".";
        return new BuildingReadiness(true, operating, floorArea, List.copyOf(blockers), line);
    }

    static int happinessBoost(int creches) {
        int count = Math.max(0, creches);
        return Math.min(MAX_HAPPINESS_BOOST,
                (count * MAX_HAPPINESS_BOOST + CRECHES_FOR_MAX_HAPPINESS - 1) / CRECHES_FOR_MAX_HAPPINESS);
    }

    static int happinessBoostFor(World world, Faction faction) {
        if (world == null || world.factionHappinessBoost == null) return 0;
        return world.factionHappinessBoost.getOrDefault(FactionIdentityAuthority.strategicFamily(faction), 0);
    }

    static boolean registerPregnancy(NpcEntity parent, long dueWorldTurn) {
        if (parent == null || parent.isAnimalActor() || parent.faction == null || parent.faction == Faction.NONE) return false;
        parent.pregnancyDueWorldTurn = Math.max(1L, dueWorldTurn);
        return true;
    }

    static int childCareCapacity(RoomPopulationLedger ledger) {
        if (ledger == null) return 0;
        int providerCapacity = Math.max(0, ledger.careProviders) * 12;
        int denseBedCapacity = Math.max(0, ledger.crecheBedUnits) * 4;
        return Math.min(providerCapacity, denseBedCapacity);
    }

    private static int immatureChildren(World world, String ledgerId, long worldTurn) {
        if (world == null || ledgerId == null) return 0;
        int children = 0;
        for (CrecheCohortRecord cohort : world.crecheCohorts) {
            if (cohort != null && ledgerId.equals(cohort.ledgerId) && cohort.remaining > 0
                    && cohort.ageYears(worldTurn) < MATURITY_YEARS) children += cohort.remaining;
        }
        return children;
    }

    private static int acceptDueFactionBirths(World world, long worldTurn) {
        if (world == null || world.npcs == null) return 0;
        int accepted = 0;
        for (NpcEntity parent : world.npcs) {
            if (parent == null || parent.pregnancyDueWorldTurn <= 0L || parent.pregnancyDueWorldTurn > worldTurn
                    || parent.faction == null || parent.faction == Faction.NONE) continue;
            RoomPopulationLedger destination = null;
            int mostOpenCare = 0;
            for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
                if (!FactionIdentityAuthority.sameFamily(ledger.faction, parent.faction)) continue;
                if (!buildingReadiness(world, ledger).operating()) continue;
                int open = childCareCapacity(ledger) - immatureChildren(world, ledger.id, worldTurn);
                if (open > mostOpenCare) {
                    destination = ledger;
                    mostOpenCare = open;
                }
            }
            if (destination == null || mostOpenCare <= 0) continue;
            CrecheCohortRecord cohort = new CrecheCohortRecord();
            cohort.id = "creche.birth." + Math.abs((safe(parent.id) + ":" + parent.pregnancyDueWorldTurn).hashCode());
            cohort.faction = parent.faction;
            cohort.ledgerId = destination.id;
            cohort.roomId = destination.roomId;
            cohort.birthWorldTurn = Math.max(1L, worldTurn);
            cohort.birthYear = Math.max(0L, worldTurn / AgeAndWorldTimeAuthority.TURNS_PER_YEAR);
            cohort.remaining = 1;
            cohort.intakeMode = "faction-parent-birth";
            cohort.parentNpcId = safe(parent.id);
            cohort.parentName = safe(parent.name);
            world.crecheCohorts.add(cohort);
            parent.pregnancyDueWorldTurn = 0L;
            accepted++;
        }
        return accepted;
    }

    private static Point spawnPoint(World world, RoomPopulationLedger ledger, Random random) {
        if (world == null) return null;
        if (ledger != null && ledger.roomId >= 0 && ledger.roomId < world.rooms.size()) {
            for (int i = 0; i < 12; i++) {
                Point point = world.randomOpenPointInRoom(world.rooms.get(ledger.roomId));
                if (point != null && world.npcAt(point.x, point.y) == null) return point;
            }
        }
        for (int i = 0; i < 20; i++) {
            Point point = world.randomOpenPoint(random);
            if (point != null && world.npcAt(point.x, point.y) == null) return point;
        }
        return null;
    }

    private static void attachProvenance(NpcEntity npc, World world, RoomPopulationLedger ledger,
                                         CrecheCohortRecord cohort, long worldTurn) {
        PersonnelProvenanceRecord provenance = new PersonnelProvenanceRecord();
        provenance.originMode = "creche-raised";
        provenance.originZone = world.zoneType.label + " / " + world.zoneCoordText();
        provenance.originRoom = ledger == null ? "recorded faction crèche" : ledger.roomName + " #" + ledger.roomId;
        provenance.originSiteId = cohort.ledgerId;
        provenance.populationPool = "birth cohort " + cohort.birthYear + " / " + cohort.intakeMode;
        provenance.arrivalRoute = "mature crèche cohort muster";
        provenance.upbringing = "raised in faction crèche care for " + MATURITY_YEARS + " world years before recruitment";
        String birthOrigin = "faction-parent-birth".equals(cohort.intakeMode)
                ? "born to faction member " + cohort.parentName
                : "accepted through abstract abandoned or ward intake";
        provenance.backstory = "Born on world turn " + cohort.birthWorldTurn + ", " + birthOrigin + ", raised by "
                + (ledger == null ? "a recorded faction crèche" : ledger.sourceLabel)
                + ", reached age " + cohort.ageYears(worldTurn) + ", and entered faction service through a bulk young-adult cohort muster.";
        npc.provenance = provenance;
    }

    private static String safe(String text) {
        return text == null ? "" : text;
    }
}

class CrecheCohortRecord {
    String id = "creche.unassigned";
    Faction faction = Faction.NONE;
    String ledgerId = "";
    int roomId = -1;
    long birthWorldTurn;
    long birthYear;
    int remaining;
    String intakeMode = "abstract-abandoned-intake";
    String parentNpcId = "";
    String parentName = "unrecorded or abandoned child intake";

    int ageYears(long worldTurn) {
        return AgeAndWorldTimeAuthority.ageAtWorldTurn(birthWorldTurn, worldTurn);
    }

    long maturityTurn() {
        return birthWorldTurn + (long) FactionCrecheAuthority.MATURITY_YEARS * AgeAndWorldTimeAuthority.TURNS_PER_YEAR;
    }

    String saveLine() {
        return enc(id) + "|" + (faction == null ? Faction.NONE.name() : faction.name()) + "|" + enc(ledgerId)
                + "|" + roomId + "|" + birthWorldTurn + "|" + birthYear + "|" + remaining
                + "|" + enc(intakeMode) + "|" + enc(parentNpcId) + "|" + enc(parentName);
    }

    static CrecheCohortRecord parse(String text) {
        try {
            String[] fields = text.split("\\|", 10);
            if (fields.length < 7) return null;
            CrecheCohortRecord cohort = new CrecheCohortRecord();
            cohort.id = dec(fields[0]);
            cohort.faction = Faction.valueOf(fields[1]);
            cohort.ledgerId = dec(fields[2]);
            cohort.roomId = Integer.parseInt(fields[3]);
            cohort.birthWorldTurn = Long.parseLong(fields[4]);
            cohort.birthYear = Long.parseLong(fields[5]);
            cohort.remaining = Math.max(0, Integer.parseInt(fields[6]));
            if (fields.length >= 8 && !dec(fields[7]).isBlank()) cohort.intakeMode = dec(fields[7]);
            if (fields.length >= 9) cohort.parentNpcId = dec(fields[8]);
            if (fields.length >= 10 && !dec(fields[9]).isBlank()) cohort.parentName = dec(fields[9]);
            return cohort;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String enc(String text) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                (text == null ? "" : text).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String dec(String text) {
        try {
            return new String(java.util.Base64.getUrlDecoder().decode(text), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }
}
