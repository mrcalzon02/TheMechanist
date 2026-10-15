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
        String originalMemory = NpcLifeMemoryAuthority.save(actor);
        NpcEntity restored = NpcEntity.parseLine(actor.saveLine(), world);
        require(restored != null && newer.id.equals(restored.currentAssignmentLedgerId)
                        && originalMemory.equals(NpcLifeMemoryAuthority.save(restored)),
                "Current faction roster or NPC episodic memory lost on save/load");
        PersonnelPopulationApi.recordDeath(world, restored);
        require(newer.assigned == 0 && newer.dead == 1 && old.assigned == 1 && old.dead == 0,
                "Death debited historical origin instead of current faction assignment");

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
