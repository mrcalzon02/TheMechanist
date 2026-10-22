package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Derived balance-of-power comparison over authoritative faction vehicle
 * fixtures. The authority stores no parallel strategic registry: assault,
 * defense, route-control, confidence, deterrence, and committed route orders are
 * recalculated from the loaded physical fleet.
 */
final class FactionVehicleBalanceAuthority {
    enum Posture {
        DOMINANT("dominant"),
        ADVANTAGED("advantaged"),
        CONTESTED("contested"),
        DETERRED("deterred"),
        OUTMATCHED("outmatched");

        final String label;
        Posture(String label) { this.label = label; }
    }

    record Contest(Faction attacker, Faction defender,
                   int attackerPower, int defenderPower,
                   int attackerHeavy, int defenderHeavy,
                   int attackerAssault, int defenderDefense,
                   int attackerRouteCommitment,
                   int defenderRouteCommitment,
                   int routeControlDelta, int confidence,
                   int deterrence, int escalationThreshold,
                   Posture posture, String summary) {
        int commitment(int aggression, int ambition) {
            return clamp(Math.max(0, aggression) + Math.max(0, ambition),
                    0, 200);
        }

        boolean canEscalate(int aggression, int ambition) {
            return commitment(aggression, ambition) >= escalationThreshold;
        }
    }

    /**
     * A commander's estimate is deliberately not the actual fleet balance.
     * Stable inputs keep planning previews and execution in agreement without
     * consuming the global simulation RNG. A different plan or commander can
     * make a different assessment of the same physical forces.
     */
    record Assessment(Contest actual, int commandSkill, int uncertaintyPct,
                      int perceivedAttackerPower, int perceivedDefenderPower,
                      Posture perceivedPosture, int decisionThreshold,
                      int effectiveCommitment, boolean willEngage,
                      int victoryChance, int battleRoll) {
        boolean victorious() { return battleRoll < victoryChance; }
        String summary() {
            return "Commander assessment: skill " + commandSkill
                    + "/100, uncertainty up to " + uncertaintyPct
                    + "%; estimated strength " + perceivedAttackerPower
                    + " vs " + perceivedDefenderPower
                    + " (" + perceivedPosture.label + "); resolve "
                    + effectiveCommitment + "/" + decisionThreshold
                    + "; engagement chance " + victoryChance + "%.";
        }
    }

    static Assessment assess(GamePanel game, Faction attacker,
                             Faction defender, NpcFactionSite site,
                             FactionStrategicPlan plan) {
        Contest actual = compare(game, attacker, defender, site);
        int skill = plan == null ? 50 : plan.effectiveCommandSkill();
        int uncertainty = Math.max(4, 40 - skill * 36 / 100);
        long worldSeed = game == null || game.world == null ? 0L : game.world.seed;
        long identity = Objects.hash(
                plan == null ? "" : plan.id,
                plan == null ? "" : plan.leaderName,
                plan == null ? "" : plan.deputyName,
                plan == null ? "" : plan.scheme,
                plan == null ? 0 : plan.cadenceCycle,
                actual.attacker(), actual.defender());
        Random assessmentRolls = new Random(worldSeed ^ (identity * 0x9E3779B97F4A7C15L));
        // Commanders know their own forces better than opposing dispositions.
        int ownError = assessmentRolls.nextInt(uncertainty + 1)
                - uncertainty / 2;
        int enemyError = assessmentRolls.nextInt(uncertainty * 2 + 1)
                - uncertainty;
        int own = Math.max(0, (int) ((long) actual.attackerPower()
                * (100 + ownError) / 100));
        int enemy = Math.max(0, (int) ((long) actual.defenderPower()
                * (100 + enemyError) / 100));
        Posture perceived = posture(own, enemy);
        int threshold = switch (perceived) {
            case DOMINANT -> 25;
            case ADVANTAGED -> 40;
            case CONTESTED -> 60;
            case DETERRED -> 100;
            case OUTMATCHED -> 155;
        };
        int aggression = plan == null ? 35 : clamp(plan.aggression, 0, 100);
        int ambition = plan == null ? 35 : clamp(plan.ambition, 0, 100);
        int resolve = actual.commitment(aggression, ambition)
                + aggression / 8 + assessmentRolls.nextInt(21) - 10;
        boolean engage = resolve >= threshold;
        int total = Math.max(1, actual.attackerPower() + actual.defenderPower());
        int chance = actual.defenderPower() <= 0 ? 100
                : clamp(50 + (int) ((long) (actual.attackerPower()
                - actual.defenderPower()) * 65 / total)
                + (skill - 50) / 10 + Math.max(0, aggression - 40) / 10,
                5, 95);
        int roll = assessmentRolls.nextInt(100);
        return new Assessment(actual, skill, uncertainty, own, enemy, perceived,
                threshold, resolve, engage, chance, roll);
    }

    private FactionVehicleBalanceAuthority() { }

    static Contest compare(GamePanel game, Faction attacker,
                           Faction defender, NpcFactionSite attackerSite) {
        Faction attackerFamily = FactionIdentityAuthority.strategicFamily(
                attacker);
        Faction defenderFamily = FactionIdentityAuthority.strategicFamily(
                defender);
        FactionVehicleDoctrineAuthority.FleetSnapshot attackerFleet =
                FactionVehicleDoctrineAuthority.fleet(game, attackerFamily,
                        attackerSite);
        FactionVehicleDoctrineAuthority.FleetSnapshot defenderFleet =
                FactionVehicleDoctrineAuthority.fleet(game, defenderFamily,
                        null);

        int attackerAssault = attackerFleet.power(
                FactionVehicleDoctrineAuthority.Dimension.ASSAULT)
                + attackerFleet.power(
                FactionVehicleDoctrineAuthority.Dimension.STRATEGIC_PROJECTION)
                + attackerFleet.power(
                FactionVehicleDoctrineAuthority.Dimension.ROUTE_CONTROL) / 2;
        int defenderDefense = defenderFleet.power(
                FactionVehicleDoctrineAuthority.Dimension.DEFENSE)
                + defenderFleet.power(
                FactionVehicleDoctrineAuthority.Dimension.STRATEGIC_PROJECTION)
                + defenderFleet.power(
                FactionVehicleDoctrineAuthority.Dimension.ROUTE_CONTROL) / 2;
        int attackerRouteCommitment = normalizedRouteCommitment(
                game, attackerFamily);
        int defenderRouteCommitment = normalizedRouteCommitment(
                game, defenderFamily);
        int attackerPower = attackerFleet.totalPower()
                + attackerAssault * 3
                + attackerFleet.readyVehicles() * 6
                + attackerFleet.heavyVehicles() * 18
                + attackerRouteCommitment * 2;
        int defenderPower = defenderFleet.totalPower()
                + defenderDefense * 3
                + defenderFleet.readyVehicles() * 6
                + defenderFleet.heavyVehicles() * 20
                + defenderRouteCommitment * 2;
        int routeDelta = attackerFleet.power(
                FactionVehicleDoctrineAuthority.Dimension.ROUTE_CONTROL)
                + attackerRouteCommitment
                - defenderFleet.power(
                FactionVehicleDoctrineAuthority.Dimension.ROUTE_CONTROL)
                - defenderRouteCommitment;

        Posture posture = posture(attackerPower, defenderPower);
        int total = Math.max(25, attackerPower + defenderPower);
        int confidence = clamp(50
                + (attackerPower - defenderPower) * 50 / total
                + attackerFleet.heavyVehicles() * 3
                - defenderFleet.heavyVehicles() * 2, 0, 100);
        int deterrence = clamp(50
                + (defenderPower - attackerPower) * 50 / total
                + defenderFleet.heavyVehicles() * 5
                - attackerFleet.heavyVehicles() * 3, 0, 100);
        int threshold = switch (posture) {
            case DOMINANT -> 25;
            case ADVANTAGED -> 45;
            case CONTESTED -> 75;
            case DETERRED -> 115;
            case OUTMATCHED -> 150;
        };
        String summary = faction(attackerFamily) + " is " + posture.label
                + " against " + faction(defenderFamily)
                + ": vehicle power " + attackerPower + " vs "
                + defenderPower + ", confidence " + confidence
                + "%, deterrence " + deterrence + "%, route-control balance "
                + signed(routeDelta) + ", committed route strength "
                + attackerRouteCommitment + " vs "
                + defenderRouteCommitment + ".";
        return new Contest(attackerFamily, defenderFamily, attackerPower,
                defenderPower, attackerFleet.heavyVehicles(),
                defenderFleet.heavyVehicles(), attackerAssault,
                defenderDefense, attackerRouteCommitment,
                defenderRouteCommitment, routeDelta, confidence, deterrence,
                threshold, posture, summary);
    }

    static List<String> inspectionLines(GamePanel game, Faction attacker,
                                        Faction defender,
                                        NpcFactionSite attackerSite) {
        Contest contest = compare(game, attacker, defender, attackerSite);
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Vehicle balance: " + contest.summary());
        lines.add("Heavy assets: " + faction(contest.attacker()) + " "
                + contest.attackerHeavy() + "; "
                + faction(contest.defender()) + " "
                + contest.defenderHeavy() + ".");
        lines.add("Route commitments: " + faction(contest.attacker()) + " "
                + contest.attackerRouteCommitment() + "; "
                + faction(contest.defender()) + " "
                + contest.defenderRouteCommitment() + ".");
        lines.add("Escalation requirement: combined leadership aggression and ambition "
                + contest.escalationThreshold() + " or greater for the current "
                + contest.posture().label + " posture.");
        return List.copyOf(lines);
    }

    private static int normalizedRouteCommitment(GamePanel game,
                                                 Faction faction) {
        return Math.max(0,
                FactionVehicleRouteControlAuthority.committedStrength(
                        game, faction) / 5);
    }

    private static Posture posture(int attackerPower, int defenderPower) {
        if (defenderPower <= 0) {
            return attackerPower > 0 ? Posture.DOMINANT : Posture.CONTESTED;
        }
        int ratio = attackerPower * 100 / Math.max(1, defenderPower);
        if (ratio >= 175) return Posture.DOMINANT;
        if (ratio >= 120) return Posture.ADVANTAGED;
        if (ratio >= 80) return Posture.CONTESTED;
        if (ratio >= 50) return Posture.DETERRED;
        return Posture.OUTMATCHED;
    }

    private static String faction(Faction faction) {
        return faction == null || faction == Faction.NONE
                ? "Unaligned" : faction.label;
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
