package mechanist;

import java.util.ArrayList;
import java.util.List;

/**
 * Derived balance-of-power comparison over authoritative faction vehicle
 * fixtures. The authority stores no parallel strategic registry: assault,
 * defense, route-control, confidence, and deterrence are recalculated from the
 * loaded fleets exposed by FactionVehicleDoctrineAuthority.
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
        int attackerPower = attackerFleet.totalPower()
                + attackerAssault * 3
                + attackerFleet.readyVehicles() * 6
                + attackerFleet.heavyVehicles() * 18;
        int defenderPower = defenderFleet.totalPower()
                + defenderDefense * 3
                + defenderFleet.readyVehicles() * 6
                + defenderFleet.heavyVehicles() * 20;
        int routeDelta = attackerFleet.power(
                FactionVehicleDoctrineAuthority.Dimension.ROUTE_CONTROL)
                - defenderFleet.power(
                FactionVehicleDoctrineAuthority.Dimension.ROUTE_CONTROL);

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
                + signed(routeDelta) + ".";
        return new Contest(attackerFamily, defenderFamily, attackerPower,
                defenderPower, attackerFleet.heavyVehicles(),
                defenderFleet.heavyVehicles(), attackerAssault,
                defenderDefense, routeDelta, confidence, deterrence,
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
        lines.add("Escalation requirement: combined leadership aggression and ambition "
                + contest.escalationThreshold() + " or greater for the current "
                + contest.posture().label + " posture.");
        return List.copyOf(lines);
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
