package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/** Persisted NPC happiness, deprivation departure, and officer-led rival recruitment. */
final class NpcHappinessAuthority {
    static final int RECRUITABLE_HAPPINESS = 45;
    static final int SEVERE_HAPPINESS = 12;
    static final int DEPARTURE_MIN_DAYS = 7;
    static final int GUARANTEED_DEPARTURE_DAYS = 14;
    private static final int DAY_TURNS = GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY;

    record Assessment(int targetHappiness, int severeDeprivations, boolean foodAvailable,
                      boolean waterAvailable, boolean bedAvailable, boolean paidRecently,
                      boolean rankHousing, String summary) { }
    record TickResult(int evaluated, int changed, int departed, String summary) { }
    record RecruitmentOutcome(boolean success, int chancePct, String officerName,
                              String targetName, String message) { }

    private NpcHappinessAuthority() { }

    static TickResult tick(GamePanel game) {
        if (game == null || game.world == null) return new TickResult(0, 0, 0, "No NPC happiness world is loaded.");
        int evaluated = 0;
        int changed = 0;
        int departed = 0;
        ArrayList<String> departures = new ArrayList<>();
        for (NpcEntity npc : new ArrayList<>(game.world.npcs)) {
            if (!tracks(npc)) continue;
            if (npc.happinessLastEvaluatedWorldTurn >= 0L
                    && game.worldTurn - npc.happinessLastEvaluatedWorldTurn < GamePanel.TURNS_PER_HOUR) continue;
            evaluated++;
            if (FactionIdentityAuthority.aligned(npc.faction)) processPayroll(game.world, npc, game.worldTurn);
            else if (npc.lastPaidWorldTurn < 0L) npc.lastPaidWorldTurn = game.worldTurn;
            Assessment assessment = assess(game.world, npc, game.worldTurn);
            int before = npc.happiness;
            npc.happiness = moveToward(npc.happiness, assessment.targetHappiness(), 2);
            npc.happinessReason = assessment.summary();
            npc.happinessLastEvaluatedWorldTurn = game.worldTurn;
            if (npc.happiness != before) changed++;
            if (FactionIdentityAuthority.aligned(npc.faction)
                    && updateSevereDurationAndMaybeDepart(game.world, npc, assessment, game.worldTurn)) {
                departed++;
                departures.add(npc.name);
            }
        }
        String summary = "NPC happiness: evaluated " + evaluated + ", changed " + changed + ", left factions " + departed + ".";
        if (!departures.isEmpty()) summary += " Departures: " + String.join(", ", departures) + ".";
        return new TickResult(evaluated, changed, departed, summary);
    }

    static Assessment assess(World world, NpcEntity npc, long worldTurn) {
        if (world == null || npc == null) {
            return new Assessment(50, 0, true, true, true, true, true, "conditions unavailable");
        }
        int foodReserve = EssentialSupplyProvenanceAuthority.availableForFaction(world, npc.faction, "food", worldTurn);
        int waterReserve = EssentialSupplyProvenanceAuthority.availableForFaction(world, npc.faction, "water", worldTurn);
        boolean food = foodReserve != 0 && npc.hunger <= 14;
        boolean water = waterReserve != 0 && npc.thirst <= 14;
        boolean bed = hasFactionBed(world, npc.faction, npc.homeX, npc.homeY);
        long payAge = npc.lastPaidWorldTurn < 0L ? 0L : Math.max(0L, worldTurn - npc.lastPaidWorldTurn);
        boolean paidRecently = payAge <= 2L * DAY_TURNS;
        boolean longUnpaid = payAge >= 7L * DAY_TURNS;
        boolean rankHousing = rankHousingMet(world, npc, bed);
        int target = 55 + FactionCrecheAuthority.happinessBoostFor(world, npc.faction);
        target += food ? 5 : -25;
        target += water ? 5 : -30;
        target += bed ? 5 : -15;
        target += paidRecently ? 5 : (longUnpaid ? -25 : -10);
        target += rankHousing ? 3 : -10;
        int severe = (food ? 0 : 1) + (water ? 0 : 1) + (bed ? 0 : 1) + (longUnpaid ? 1 : 0);
        String summary = (food ? "food supplied" : "no reliable food") + ", "
                + (water ? "water supplied" : "no reliable water") + ", "
                + (bed ? "bed available" : "no bed") + ", "
                + (paidRecently ? "pay current" : (longUnpaid ? "pay overdue seven days" : "pay late")) + ", "
                + (rankHousing ? "housing fits rank" : "housing below rank");
        return new Assessment(clamp(target), severe, food, water, bed, paidRecently, rankHousing, summary);
    }

    static String statusLine(World world, NpcEntity npc, long worldTurn) {
        if (npc == null) return "Happiness: unavailable.";
        Assessment assessment = assess(world, npc, worldTurn);
        int chance = vulnerabilityChance(world, npc, Faction.NONE, 0);
        return "Happiness: " + npc.happiness + "/100 (" + band(npc.happiness) + "); "
                + assessment.summary() + "; rival recruitment susceptibility " + chance + "%.";
    }

    static int vulnerabilityChance(World world, NpcEntity npc, Faction recruiter, int officerBonus) {
        if (!tracks(npc) || !FactionIdentityAuthority.aligned(npc.faction)
                || (FactionIdentityAuthority.aligned(recruiter) && FactionIdentityAuthority.sameFamily(npc.faction, recruiter))
                || npc.happiness > RECRUITABLE_HAPPINESS) return 0;
        if (npc.lastFactionChangeWorldTurn >= 0L && npc.happinessLastEvaluatedWorldTurn >= 0L
                && npc.happinessLastEvaluatedWorldTurn - npc.lastFactionChangeWorldTurn < 7L * DAY_TURNS) return 0;
        int relative = FactionIdentityAuthority.aligned(recruiter)
                ? Math.max(0, averageHappiness(world, recruiter) - npc.happiness) : 0;
        return Math.max(5, Math.min(90, (RECRUITABLE_HAPPINESS - npc.happiness) * 2 + relative / 2 + officerBonus));
    }

    static RecruitmentOutcome attemptSchemeRecruitment(GamePanel game, FactionStrategicPlan plan) {
        if (game == null || game.world == null || plan == null || !FactionIdentityAuthority.aligned(plan.faction)) {
            return new RecruitmentOutcome(false, 0, "no officer", "no target", "Recruitment scheme lacked a valid faction plan.");
        }
        NpcEntity officer = bestOfficer(game.world, plan.faction);
        String officerName = officer == null ? plan.leaderName : officer.name;
        int officerBonus = officer == null ? 6 + plan.ambition / 20 : 8 + Math.max(0, 6 - officer.factionRank) * 2;
        NpcEntity target = mostRecruitableRival(game.world, plan.faction, plan.schemeTargetFaction, officerBonus);
        if (target == null) {
            return new RecruitmentOutcome(false, 0, officerName, "no target",
                    officerName + " found no sufficiently unhappy rival member willing to hear an approach.");
        }
        int capacity = PersonnelPopulationApi.replacementCapacityForFaction(game.world, plan.faction);
        int living = PersonnelPopulationApi.countLivingFactionActors(game.world, plan.faction);
        if (living >= capacity) {
            return new RecruitmentOutcome(false, 0, officerName, target.name,
                    officerName + " could not promise " + target.name + " faction housing; destination capacity is " + living + "/" + capacity + ".");
        }
        int chance = vulnerabilityChance(game.world, target, plan.faction, officerBonus);
        int roll = 1 + game.rng.nextInt(100);
        if (roll > chance) {
            return new RecruitmentOutcome(false, chance, officerName, target.name,
                    officerName + " approached " + target.name + ", but the offer failed (roll " + roll + " vs " + chance + "%).");
        }
        Faction oldFaction = target.faction;
        releasePopulationAssignment(game.world, target, oldFaction);
        target.faction = plan.faction;
        target.state = "Recruited by Rival Faction";
        target.lastFactionChangeWorldTurn = game.worldTurn;
        target.lastPaidWorldTurn = game.worldTurn;
        target.happiness = Math.max(45, Math.min(75, averageHappiness(game.world, plan.faction)));
        target.severeUnhappinessSinceWorldTurn = -1L;
        assignPopulationSlot(game.world, target, plan.faction, game.rng);
        if (target.provenance != null) {
            target.provenance.backstory += " Recruited from " + oldFaction.label + " by " + officerName
                    + " at world turn " + game.worldTurn + " after sustained dissatisfaction.";
        }
        return new RecruitmentOutcome(true, chance, officerName, target.name,
                officerName + " recruited " + target.name + " from " + oldFaction.label + " into "
                        + plan.faction.label + " (roll " + roll + " vs " + chance + "%).");
    }

    static boolean isRecruitmentPlan(FactionStrategicPlan plan) {
        if (plan == null) return false;
        String text = (safe(plan.immediateGoal) + " " + safe(plan.scheme)).toLowerCase(Locale.ROOT);
        return text.contains("recruit a rival") || text.contains("disaffected rival");
    }

    private static void processPayroll(World world, NpcEntity npc, long worldTurn) {
        if (npc.lastPaidWorldTurn < 0L) {
            npc.lastPaidWorldTurn = worldTurn;
            return;
        }
        if (worldTurn - npc.lastPaidWorldTurn < DAY_TURNS) return;
        DeferredFactionLedgerRecord ledger = DeferredOutOfSectorSimulationAuthority.ledgerFor(world, npc.faction);
        int wealth = ledger == null ? 55 : ledger.wealth;
        int chance = Math.max(35, Math.min(95, 70 + (wealth - 50) / 2));
        long day = worldTurn / DAY_TURNS;
        int roll = Math.floorMod(Objects.hash(world.seed, npc.id, day, "payroll"), 100);
        if (roll < chance) npc.lastPaidWorldTurn = worldTurn;
    }

    private static boolean updateSevereDurationAndMaybeDepart(World world, NpcEntity npc,
                                                               Assessment assessment, long worldTurn) {
        if (npc.happiness <= SEVERE_HAPPINESS && assessment.severeDeprivations() >= 3) {
            if (npc.severeUnhappinessSinceWorldTurn < 0L) npc.severeUnhappinessSinceWorldTurn = worldTurn;
        } else if (npc.happiness > 20 || assessment.severeDeprivations() < 3) {
            npc.severeUnhappinessSinceWorldTurn = -1L;
            return false;
        }
        if (npc.severeUnhappinessSinceWorldTurn < 0L) return false;
        long severeDays = (worldTurn - npc.severeUnhappinessSinceWorldTurn) / DAY_TURNS;
        if (severeDays < DEPARTURE_MIN_DAYS) return false;
        int chance = Math.min(85, 35 + (SEVERE_HAPPINESS - npc.happiness) * 4);
        int roll = 1 + Math.floorMod(Objects.hash(world.seed, npc.id, severeDays, "leave-faction"), 100);
        if (severeDays < GUARANTEED_DEPARTURE_DAYS && roll > chance) return false;
        Faction oldFaction = npc.faction;
        releasePopulationAssignment(world, npc, oldFaction);
        npc.faction = Faction.NONE;
        npc.factionRank = 8;
        npc.factionRankTitle = "Unaffiliated Resident";
        npc.factionRankScope = "general populace";
        npc.state = "Left Faction";
        npc.lastFactionChangeWorldTurn = worldTurn;
        npc.severeUnhappinessSinceWorldTurn = -1L;
        if (npc.provenance != null) npc.provenance.backstory += " Left " + oldFaction.label
                + " for the general populace at world turn " + worldTurn + " after prolonged severe deprivation.";
        return true;
    }

    private static boolean hasFactionBed(World world, Faction faction, int homeX, int homeY) {
        int homeRoom = world.roomIdAt(homeX, homeY);
        if (ownedHousingRoom(world, homeRoom, faction)) return true;
        for (int roomId = 0; roomId < world.rooms.size(); roomId++) {
            if (ownedHousingRoom(world, roomId, faction)) return true;
        }
        return false;
    }

    private static boolean rankHousingMet(World world, NpcEntity npc, boolean bed) {
        if (!bed) return false;
        if (npc.factionRank > 4) return true;
        for (int roomId = 0; roomId < world.rooms.size(); roomId++) {
            if (!FactionIdentityAuthority.sameFamily(world.roomFaction(roomId), npc.faction)) continue;
            String text = roomText(world, roomId);
            if (npc.factionRank <= 2) {
                if (contains(text, "command", "officer", "private", "noble", "residence", "apartment", "quarters")) return true;
            } else if (contains(text, "barracks", "billet", "dorm", "apartment", "quarters", "hab", "residence")) return true;
        }
        return npc.factionRank > 2;
    }

    private static boolean ownedHousingRoom(World world, int roomId, Faction faction) {
        if (roomId < 0 || roomId >= world.rooms.size()
                || !FactionIdentityAuthority.sameFamily(world.roomFaction(roomId), faction)) return false;
        String text = roomText(world, roomId);
        if (contains(text, "bed", "cot", "sleep", "dorm", "barracks", "billet", "apartment", "quarters", "hab", "residence", "creche", "nursery")) return true;
        java.awt.Rectangle room = world.roomRect(roomId);
        if (room == null) return false;
        for (int x = room.x; x < room.x + room.width; x++) {
            for (int y = room.y; y < room.y + room.height; y++) {
                if (world.inBounds(x, y) && world.tiles[x][y] == 'c') return true;
            }
        }
        return false;
    }

    private static String roomText(World world, int roomId) {
        RoomProfile room = world.roomProfile(roomId);
        return (safe(room.name) + " " + safe(room.descriptor) + " " + safe(room.featureText)).toLowerCase(Locale.ROOT);
    }

    private static NpcEntity bestOfficer(World world, Faction faction) {
        NpcEntity best = null;
        for (NpcEntity npc : world.npcs) {
            if (!tracks(npc) || !FactionIdentityAuthority.sameFamily(npc.faction, faction) || !isOfficer(npc)) continue;
            if (best == null || npc.factionRank < best.factionRank) best = npc;
        }
        return best;
    }

    private static boolean isOfficer(NpcEntity npc) {
        if (npc == null) return false;
        String role = safe(npc.role).toLowerCase(Locale.ROOT);
        return npc.factionRank <= 4 || contains(role, "officer", "leader", "manager", "supervisor", "marshal", "deputy", "representative");
    }

    private static NpcEntity mostRecruitableRival(World world, Faction recruiter, Faction targetFaction, int officerBonus) {
        NpcEntity best = null;
        int bestChance = 0;
        for (NpcEntity npc : world.npcs) {
            if (!tracks(npc) || !FactionIdentityAuthority.aligned(npc.faction)
                    || FactionIdentityAuthority.sameFamily(npc.faction, recruiter)) continue;
            if (FactionIdentityAuthority.aligned(targetFaction)
                    && !FactionIdentityAuthority.sameFamily(npc.faction, targetFaction)) continue;
            int chance = vulnerabilityChance(world, npc, recruiter, officerBonus);
            if (chance > bestChance) {
                best = npc;
                bestChance = chance;
            }
        }
        return best;
    }

    private static int averageHappiness(World world, Faction faction) {
        if (world == null) return 50;
        int total = 0;
        int count = 0;
        for (NpcEntity npc : world.npcs) {
            if (tracks(npc) && FactionIdentityAuthority.sameFamily(npc.faction, faction)) {
                total += npc.happiness;
                count++;
            }
        }
        return count == 0 ? 60 : total / count;
    }

    private static void releasePopulationAssignment(World world, NpcEntity npc, Faction oldFaction) {
        if (npc.provenance == null) return;
        RoomPopulationLedger ledger = PersonnelPopulationApi.ledgerById(world, npc.provenance.originSiteId);
        if (ledger != null && FactionIdentityAuthority.sameFamily(ledger.faction, oldFaction)) {
            ledger.assigned = Math.max(0, ledger.assigned - 1);
        }
    }

    private static void assignPopulationSlot(World world, NpcEntity npc, Faction faction, Random random) {
        RoomPopulationLedger ledger = PersonnelPopulationApi.drawLedgerFor(world, -1, faction, random, false);
        if (ledger == null || !FactionIdentityAuthority.sameFamily(ledger.faction, faction)) return;
        ledger.assigned++;
        ledger.available = Math.max(0, ledger.available - 1);
        if (npc.provenance != null) npc.provenance.backstory += " Current faction assignment uses " + ledger.roomName + ".";
    }

    private static boolean tracks(NpcEntity npc) {
        return npc != null && npc.hp > 0 && !npc.isAnimalActor() && !npc.isUntargetableAnchor() && npc.ageYears >= 16;
    }

    private static int moveToward(int current, int target, int step) {
        if (current == target) return current;
        return current + Integer.signum(target - current) * Math.min(step, Math.abs(target - current));
    }

    private static String band(int happiness) {
        if (happiness >= 80) return "content";
        if (happiness >= 60) return "settled";
        if (happiness >= 46) return "uneasy";
        if (happiness >= 21) return "unhappy";
        if (happiness >= 13) return "desperate";
        return "near departure";
    }

    private static int clamp(int value) { return Math.max(0, Math.min(100, value)); }
    private static String safe(String value) { return value == null ? "" : value; }
    private static boolean contains(String text, String... needles) {
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
    }
}
