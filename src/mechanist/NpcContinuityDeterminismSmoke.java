package mechanist;

import java.util.ArrayList;
import java.util.Random;

/** Deterministic active-state, faction assignment, replacement and life-memory regression. */
public final class NpcContinuityDeterminismSmoke {
    private NpcContinuityDeterminismSmoke() { }

    public static void main(String[] args) {
        World world = new World(917431L, 32, 32);
        world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        RoomPopulationLedger old = ledger("civic", Faction.CIVIC_WARDENS);
        RoomPopulationLedger newer = ledger("hiver", Faction.HIVER);
        world.roomPopulationLedgers.add(old);
        world.roomPopulationLedgers.add(newer);
        NpcEntity actor = NpcEntity.create(Faction.HIVER, world.zoneType, 5, 5, new Random(17));
        actor.id = "CONTINUITY-FACTION-1";
        actor.name = "Sergeant Rook";
        actor.role = "Patrol Guard";
        actor.provenance = new PersonnelProvenanceRecord();
        actor.provenance.originSiteId = old.id;
        actor.currentAssignmentLedgerId = newer.id;
        old.assigned = 1;
        newer.assigned = 1;

        NpcLifeMemoryAuthority.remember(actor, 100, "allegiance", "Joined Hiver from the Wardens");
        NpcLifeMemoryAuthority.remember(actor, 100, "allegiance", "Joined Hiver from the Wardens");
        require(actor.lifeMemories.size() == 1, "Duplicate memory on same turn");
        actor.state = "Idle";
        NpcLifeMemoryAuthority.observeState(actor, 201, "Patrol");
        actor.state = "Search Needs";
        NpcLifeMemoryAuthority.observeState(actor, 202, "Idle");
        actor.state = "Guard";
        NpcLifeMemoryAuthority.observeState(actor, 203, "Idle");
        actor.state = "Patrol";
        NpcLifeMemoryAuthority.observeState(actor, 204, "Guard");
        require(actor.lifeMemories.size() == 3, "Routine duty transitions were not throttled");
        for (int i = 0; i < 30; i++) NpcLifeMemoryAuthority.remember(actor, 300 + i, "event", "Witnessed event " + i);
        require(actor.lifeMemories.size() == NpcLifeMemoryAuthority.MAX_EVENTS,
                "NPC memory budget not bounded");
        actor.idleBias = 5;
        actor.routineOffset = 19;
        actor.lastTickTurn = 270;
        actor.hp = 7;
        actor.needTargetKind = "water";
        actor.needTarget = new java.awt.Point(11, 12);
        actor.facingDx = 0;
        actor.facingDy = -1;
        actor.crowdShuffleCooldown = 4;
        actor.suspiciousBarkCooldown = 3;
        String originalMemory = NpcLifeMemoryAuthority.save(actor);
        NpcEntity restored = NpcEntity.parseLine(actor.saveLine(), world);
        require(restored != null && newer.id.equals(restored.currentAssignmentLedgerId)
                        && originalMemory.equals(NpcLifeMemoryAuthority.save(restored)),
                "Current faction roster or NPC episodic memory lost on save/load");
        require(restored.idleBias == actor.idleBias && restored.routineOffset == actor.routineOffset
                        && restored.lastTickTurn == actor.lastTickTurn && restored.hp == actor.hp
                        && restored.needTarget != null && restored.needTarget.equals(actor.needTarget)
                        && "water".equals(restored.needTargetKind)
                        && restored.facingDx == 0 && restored.facingDy == -1
                        && restored.crowdShuffleCooldown == 4 && restored.suspiciousBarkCooldown == 3,
                "NPC health, state/cadence, need target or movement cooldown changed after save/load");
        NpcEntity originalPet = new NpcEntity();
        originalPet.id = "PET-CONTINUITY-1";
        originalPet.x = 8; originalPet.y = 8;
        NpcEntity duplicatePet = new NpcEntity();
        duplicatePet.id = originalPet.id;
        duplicatePet.x = 8; duplicatePet.y = 8;
        for (int x = 0; x < world.w; x++) for (int y = 0; y < world.h; y++) world.tiles[x][y] = '.';
        originalPet.wanderNear(world, 8, 8, 2, -1, -1, 411);
        duplicatePet.wanderNear(world, 8, 8, 2, -1, -1, 411);
        require(originalPet.x == duplicatePet.x && originalPet.y == duplicatePet.y,
                "Identical world/actor/turn produced divergent companion movement");
        // Room-intake selection must use the request's RNG rather than the
        // mutable world-generation random stream.
        World firstWorld = roomWorld(8173L);
        World secondWorld = roomWorld(8173L);
        for (int i = 0; i < 500; i++) secondWorld.r.nextInt();
        PersonnelReplacementRequest fixedRequest = new PersonnelReplacementRequest();
        fixedRequest.sourceRoomId = 0;
        fixedRequest.sourceLedgerId = "replacement-room";
        fixedRequest.faction = Faction.HIVER;
        java.awt.Point firstPoint = PersonnelPopulationApi.spawnPointForReplacement(
                firstWorld, fixedRequest, new Random(19L));
        java.awt.Point secondPoint = PersonnelPopulationApi.spawnPointForReplacement(
                secondWorld, fixedRequest, new Random(19L));
        require(firstPoint != null && firstPoint.equals(secondPoint),
                "Intake placement changed after irrelevant world RNG draws");
        // Legacy NPC records lack the new cadence and memory fields; parsing
        // them must still work without fabricating current-assignment history.
        String[] existing = actor.saveLine().split("\\|", -1);
        NpcEntity legacyActor = NpcEntity.parseLine(String.join("|",
                java.util.Arrays.copyOf(existing, 41)), world);
        require(legacyActor != null && legacyActor.lifeMemories.isEmpty()
                        && legacyActor.currentAssignmentLedgerId.isEmpty(),
                "Historical 41-field NPC save stopped loading");
        PersonnelPopulationApi.recordDeath(world, restored);
        require(newer.assigned == 0 && newer.dead == 1 && old.assigned == 1 && old.dead == 0,
                "Death debited historical origin instead of current faction assignment");

        // Roster selection cannot consume unrelated faction ledgers or accept
        // a roster whose reserve is empty despite unused nominal capacity.
        World membership = new World(72419L, 22, 22);
        RoomPopulationLedger exhausted = ledger("a-hiver-exhausted", Faction.HIVER);
        exhausted.available = 0;
        RoomPopulationLedger active = ledger("b-hiver-available", Faction.HIVER);
        RoomPopulationLedger unrelated = ledger("c-wardens-available", Faction.CIVIC_WARDENS);
        membership.roomPopulationLedgers.add(exhausted);
        membership.roomPopulationLedgers.add(unrelated);
        membership.roomPopulationLedgers.add(active);
        NpcEntity applicant = NpcEntity.create(Faction.CIVIC_WARDENS, membership.zoneType,
                9, 9, new Random(24));
        applicant.id = "APPLICANT-1";
        RoomPopulationLedger selected = NpcHappinessAuthority.availableRecruitmentLedger(
                membership, Faction.HIVER, applicant);
        require(selected == active, "Recruitment selected an exhausted or rival roster");
        membership.roomPopulationLedgers.clear();
        membership.roomPopulationLedgers.add(active);
        membership.roomPopulationLedgers.add(unrelated);
        membership.roomPopulationLedgers.add(exhausted);
        require(NpcHappinessAuthority.availableRecruitmentLedger(
                membership, Faction.HIVER, applicant) == active,
                "Destination roster changed with insertion order");
        active.available = 0;
        require(NpcHappinessAuthority.availableRecruitmentLedger(
                membership, Faction.HIVER, applicant) == null,
                "Empty reserve ledger incorrectly accepted a faction transfer");
        active.available = 2;
        active.assigned = active.capacity;
        require(NpcHappinessAuthority.availableRecruitmentLedger(
                membership, Faction.HIVER, applicant) == null,
                "Fully assigned roster incorrectly accepted recruitment");

        // Remembered deprivation must actually affect recruitment behavior, be
        // restored from save, expire on world time, and never blame a new faction.
        long recruitmentTurn = 20L * GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY;
        applicant.role = "Laborer";
        applicant.ageYears = 30;
        applicant.happiness = 30;
        applicant.lastFactionChangeWorldTurn = -1L;
        applicant.happinessLastEvaluatedWorldTurn = recruitmentTurn;
        int untreatedChance = NpcHappinessAuthority.vulnerabilityChance(
                membership, applicant, Faction.HIVER, 0, recruitmentTurn);
        require(untreatedChance > 0 && untreatedChance <= 78,
                "Recruitment baseline must leave room for actual memory consequences");
        NpcLifeMemoryAuthority.remember(applicant, recruitmentTurn - 1L,
                "deprivation", "State changed to Search Needs");
        int influencedChance = NpcHappinessAuthority.vulnerabilityChance(
                membership, applicant, Faction.HIVER, 0, recruitmentTurn);
        require(influencedChance == untreatedChance + 12,
                "Recently experienced deprivation must change the rival recruitment probability");
        NpcEntity recalled = NpcEntity.parseLine(applicant.saveLine(), membership);
        require(recalled != null && NpcHappinessAuthority.vulnerabilityChance(
                membership, recalled, Faction.HIVER, 0, recruitmentTurn) == influencedChance,
                "Saved personal deprivation must influence the same decision after reload");
        Random irrelevant = new Random(618273);
        int originalRoll = NpcHappinessAuthority.deterministicRecruitmentRoll(
                membership, applicant, Faction.HIVER, recruitmentTurn);
        for (int i = 0; i < 500; i++) irrelevant.nextInt();
        require(originalRoll == NpcHappinessAuthority.deterministicRecruitmentRoll(
                membership, recalled, Faction.HIVER, recruitmentTurn),
                "Recruitment outcome depended on unrelated random draws or save/load");
        long later = recruitmentTurn + 4L * GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY;
        require(NpcHappinessAuthority.vulnerabilityChance(
                membership, recalled, Faction.HIVER, 0, later) == untreatedChance,
                "Old deprivation memories must stop affecting recruitment decisions");
        recalled.faction = Faction.HIVER;
        require(NpcLifeMemoryAuthority.recruitmentPressure(recalled, recruitmentTurn) == 0,
                "Changing faction must not make the new faction responsible for old deprivation");

        NpcEntity casualty = NpcEntity.create(Faction.CIVIC_WARDENS, world.zoneType, 9, 9, new Random(56));
        casualty.id = "CASUALTY-STABLE-A";
        casualty.name = "Corporal Flint";
        casualty.role = "Guard";
        casualty.state = "Patrol";
        casualty.provenance = new PersonnelProvenanceRecord();
        casualty.provenance.originSiteId = old.id;
        casualty.currentAssignmentLedgerId = old.id;
        PersonnelReplacementRequest first = PersonnelProvenanceApi.recordDeathAndScheduleReplacement(
                world, casualty, 350, new Random(1), "test casualty");
        world.replacementQueue.clear();
        PersonnelReplacementRequest repeat = PersonnelProvenanceApi.recordDeathAndScheduleReplacement(
                world, casualty, 350, new Random(9999), "test casualty");
        require(first.dueTurn == repeat.dueTurn
                        && first.sourceMode.equals(repeat.sourceMode)
                        && first.sourceLedgerId.equals(repeat.sourceLedgerId),
                "Replacement timing/source changed with unrelated random stream");
        require("Guard".equals(repeat.formerRole) && "Patrol".equals(repeat.formerState),
                "Original duty identity was not recorded in reinforcement manifest");
        PersonnelReplacementRequest parsed = PersonnelReplacementRequest.parse(repeat.saveLine());
        require(parsed != null && parsed.dueTurn == repeat.dueTurn
                        && parsed.formerRole.equals(repeat.formerRole)
                        && parsed.formerState.equals(repeat.formerState),
                "Reinforcement manifest lost identity or deterministic lifecycle fields");
        // Old 15-field request records still load and acquire no invented role.
        String[] fields = repeat.saveLine().split("\\|", -1);
        PersonnelReplacementRequest legacy = PersonnelReplacementRequest.parse(
                String.join("|", java.util.Arrays.copyOf(fields, 15)));
        require(legacy != null && legacy.formerRole.isEmpty(),
                "Legacy reinforcement manifests should not invent a former role");
        System.out.println("NpcContinuityDeterminismSmoke PASS memory=" + restored.lifeMemories.size()
                + " replacementDue=" + first.dueTurn);
    }

    private static World roomWorld(long seed) {
        World world = new World(seed, 20, 20);
        for (int x = 0; x < world.w; x++) for (int y = 0; y < world.h; y++) {
            world.tiles[x][y] = '#';
            world.roomIds[x][y] = -1;
        }
        java.awt.Rectangle room = new java.awt.Rectangle(3, 3, 12, 12);
        world.rooms.add(room);
        for (int x = 4; x < 14; x++) for (int y = 4; y < 14; y++) {
            world.tiles[x][y] = '.';
            world.roomIds[x][y] = 0;
        }
        RoomPopulationLedger ledger = ledger("replacement-room", Faction.HIVER);
        ledger.roomId = 0;
        world.roomPopulationLedgers.add(ledger);
        return world;
    }

    private static RoomPopulationLedger ledger(String id, Faction faction) {
        RoomPopulationLedger l = new RoomPopulationLedger();
        l.id = id;
        l.faction = faction;
        l.roomName = id + " barracks";
        l.sourceLabel = id + " roster";
        l.sourceKind = "local roster";
        l.capacity = 6;
        l.available = 5;
        return l;
    }

    private static void require(boolean truth, String detail) {
        if (!truth) throw new AssertionError(detail);
    }
}
