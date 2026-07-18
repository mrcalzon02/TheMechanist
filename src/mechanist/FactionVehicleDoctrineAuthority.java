package mechanist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Derived faction doctrine and strategic fleet value over authoritative vehicle,
 * manifest, fuel, and motor-pool state. No parallel fleet registry is stored:
 * power is recalculated from the physical vehicle fixtures in the loaded world.
 */
final class FactionVehicleDoctrineAuthority {
    enum Dimension {
        TRANSPORT("transport"),
        PATROL("patrol"),
        CARGO("cargo"),
        ASSAULT("assault"),
        DEFENSE("defense"),
        INTIMIDATION("intimidation"),
        EVACUATION("evacuation"),
        LOGISTICS("logistics"),
        PRODUCTION_SUPPORT("production support"),
        CONVOY("convoy"),
        ROUTE_CONTROL("route control"),
        STRATEGIC_PROJECTION("strategic projection"),
        PRESTIGE("prestige"),
        SMUGGLING("smuggling"),
        SALVAGE("salvage");

        final String label;
        Dimension(String label) { this.label = label; }
    }

    enum Operation { SEIZURE, REPAIR, SALVAGE }

    record Profile(Faction family, String label,
                   Map<Dimension, Integer> priorities) {
        int priority(Dimension dimension) {
            return priorities.getOrDefault(dimension, 0);
        }
    }

    record VehicleAssessment(MapObjectState vehicle,
                             VehicleRuntimeAuthority.VehicleClass vehicleClass,
                             String displayName, Faction evaluatedFor,
                             int readiness, int doctrineFit,
                             int strategicValue, int salvagePriority,
                             boolean captured, boolean salvageRecommended,
                             Map<Dimension, Integer> contribution,
                             List<String> reasons) { }

    record FleetSnapshot(Faction family, String doctrine,
                         int vehicleCount, int readyVehicles,
                         int heavyVehicles, int totalPower,
                         Map<Dimension, Integer> power,
                         List<VehicleAssessment> vehicles,
                         String summary) {
        int power(Dimension dimension) {
            return power.getOrDefault(dimension, 0);
        }
    }

    private FactionVehicleDoctrineAuthority() { }

    static Profile profile(Faction faction) {
        Faction family = FactionIdentityAuthority.strategicFamily(faction);
        EnumMap<Dimension, Integer> priorities = baseline();
        String label = switch (family) {
            case MECHANIST_COLLEGIA -> {
                weights(priorities,
                        Dimension.LOGISTICS, 10,
                        Dimension.PRODUCTION_SUPPORT, 10,
                        Dimension.CARGO, 9,
                        Dimension.CONVOY, 8,
                        Dimension.ROUTE_CONTROL, 7,
                        Dimension.TRANSPORT, 6,
                        Dimension.STRATEGIC_PROJECTION, 6,
                        Dimension.SALVAGE, 5);
                yield "industrial logistics and protected technical assets";
            }
            case IMPERIAL_GUARD -> {
                weights(priorities,
                        Dimension.ASSAULT, 10,
                        Dimension.DEFENSE, 10,
                        Dimension.STRATEGIC_PROJECTION, 10,
                        Dimension.INTIMIDATION, 9,
                        Dimension.ROUTE_CONTROL, 9,
                        Dimension.CONVOY, 7,
                        Dimension.TRANSPORT, 6);
                yield "armored deployment and territorial deterrence";
            }
            case CIVIC_WARDENS -> {
                weights(priorities,
                        Dimension.PATROL, 10,
                        Dimension.ROUTE_CONTROL, 9,
                        Dimension.DEFENSE, 8,
                        Dimension.INTIMIDATION, 7,
                        Dimension.TRANSPORT, 6,
                        Dimension.CONVOY, 5);
                yield "patrol, checkpoint control, and public-order response";
            }
            case NOBLE -> {
                weights(priorities,
                        Dimension.PRESTIGE, 10,
                        Dimension.TRANSPORT, 8,
                        Dimension.DEFENSE, 7,
                        Dimension.INTIMIDATION, 7,
                        Dimension.EVACUATION, 6,
                        Dimension.STRATEGIC_PROJECTION, 6);
                yield "prestige transport, protected movement, and visible leverage";
            }
            case BANDIT -> {
                weights(priorities,
                        Dimension.SMUGGLING, 10,
                        Dimension.INTIMIDATION, 9,
                        Dimension.ASSAULT, 8,
                        Dimension.ROUTE_CONTROL, 8,
                        Dimension.CARGO, 6,
                        Dimension.TRANSPORT, 6,
                        Dimension.SALVAGE, 6);
                yield "rapid raiding, smuggling, intimidation, and route predation";
            }
            case SCAVENGER -> {
                weights(priorities,
                        Dimension.SALVAGE, 10,
                        Dimension.CARGO, 8,
                        Dimension.LOGISTICS, 8,
                        Dimension.TRANSPORT, 7,
                        Dimension.PRODUCTION_SUPPORT, 6,
                        Dimension.SMUGGLING, 5);
                yield "salvage recovery, improvised hauling, and practical mobility";
            }
            case HIVER -> {
                weights(priorities,
                        Dimension.TRANSPORT, 9,
                        Dimension.EVACUATION, 9,
                        Dimension.CARGO, 8,
                        Dimension.LOGISTICS, 7,
                        Dimension.CONVOY, 5,
                        Dimension.ROUTE_CONTROL, 5);
                yield "workforce transport, evacuation, and neighborhood supply";
            }
            case CIVIC_LEDGER_OFFICE -> {
                weights(priorities,
                        Dimension.TRANSPORT, 8,
                        Dimension.LOGISTICS, 7,
                        Dimension.ROUTE_CONTROL, 7,
                        Dimension.CARGO, 6,
                        Dimension.CONVOY, 5,
                        Dimension.PRESTIGE, 4);
                yield "administrative transport and regulated route continuity";
            }
            case INN -> {
                weights(priorities,
                        Dimension.TRANSPORT, 9,
                        Dimension.ROUTE_CONTROL, 7,
                        Dimension.CARGO, 6,
                        Dimension.CONVOY, 5,
                        Dimension.EVACUATION, 5,
                        Dimension.PRESTIGE, 4);
                yield "reporting mobility, courier access, and route reach";
            }
            case MINISTORUM, SORORITAS -> {
                weights(priorities,
                        Dimension.DEFENSE, 9,
                        Dimension.CONVOY, 8,
                        Dimension.TRANSPORT, 7,
                        Dimension.EVACUATION, 7,
                        Dimension.INTIMIDATION, 7,
                        Dimension.PRESTIGE, 5);
                yield "protected procession, escort, evacuation, and defensive presence";
            }
            case CULTIST, HERETIC -> {
                weights(priorities,
                        Dimension.SMUGGLING, 9,
                        Dimension.INTIMIDATION, 9,
                        Dimension.ASSAULT, 8,
                        Dimension.ROUTE_CONTROL, 7,
                        Dimension.TRANSPORT, 5,
                        Dimension.SALVAGE, 5);
                yield "covert movement, intimidation, and violent projection";
            }
            case MUTANT -> {
                weights(priorities,
                        Dimension.SALVAGE, 10,
                        Dimension.TRANSPORT, 7,
                        Dimension.CARGO, 6,
                        Dimension.INTIMIDATION, 6,
                        Dimension.SMUGGLING, 5);
                yield "improvised movement and salvage survival";
            }
            case ROGUE_MACHINE -> {
                weights(priorities,
                        Dimension.ASSAULT, 10,
                        Dimension.DEFENSE, 10,
                        Dimension.STRATEGIC_PROJECTION, 10,
                        Dimension.ROUTE_CONTROL, 9,
                        Dimension.LOGISTICS, 8,
                        Dimension.INTIMIDATION, 8);
                yield "mechanized assault, defense, and autonomous projection";
            }
            case NONE -> {
                weights(priorities,
                        Dimension.TRANSPORT, 6,
                        Dimension.CARGO, 6,
                        Dimension.SALVAGE, 6,
                        Dimension.LOGISTICS, 5);
                yield "unassigned practical mobility";
            }
            default -> {
                weights(priorities,
                        Dimension.TRANSPORT, 7,
                        Dimension.CARGO, 6,
                        Dimension.PATROL, 6,
                        Dimension.DEFENSE, 5,
                        Dimension.LOGISTICS, 5);
                yield "general transport and local security";
            }
        };
        return new Profile(family, label, Map.copyOf(priorities));
    }

    static VehicleAssessment assess(GamePanel game, MapObjectState vehicle,
                                    NpcFactionSite site) {
        Faction faction = site == null ? Faction.NONE : site.faction;
        if (faction == Faction.NONE && vehicle != null) {
            VehicleRuntimeAuthority.Snapshot snapshot =
                    VehicleRuntimeAuthority.inspect(
                            game == null ? null : game.world, vehicle);
            if (snapshot != null) faction = snapshot.ownerFaction();
        }
        return assess(game, vehicle, site, faction);
    }

    static VehicleAssessment assess(GamePanel game, MapObjectState vehicle,
                                    NpcFactionSite site, Faction evaluatedFor) {
        World world = game == null ? null : game.world;
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return new VehicleAssessment(vehicle,
                    VehicleRuntimeAuthority.VehicleClass.CIVILIAN_CAR,
                    "missing vehicle", Faction.NONE, 0, 0, 0, 0,
                    false, false, Map.of(), List.of("no physical vehicle"));
        }
        VehicleRuntimeAuthority.ensureInitialized(world, vehicle);
        VehicleFuelAuthority.ensureInitialized(world, vehicle);
        VehicleRuntimeAuthority.Snapshot runtime =
                VehicleRuntimeAuthority.inspect(world, vehicle);
        VehicleManifestAuthority.Snapshot manifest =
                VehicleManifestAuthority.inspect(world, vehicle);
        VehicleFuelAuthority.Snapshot fuel =
                VehicleFuelAuthority.inspect(world, vehicle);
        Faction family = FactionIdentityAuthority.strategicFamily(evaluatedFor);
        if (family == Faction.NONE) {
            family = FactionIdentityAuthority.strategicFamily(
                    runtime.ownerFaction());
        }
        Profile profile = profile(family);
        EnumMap<Dimension, Integer> contribution =
                baseContribution(runtime.vehicleClass());
        applyRoleHints(contribution, value(vehicle, "vehicleRole"));
        applyRoleHints(contribution, value(vehicle, "motorPoolRole"));

        int readiness = readiness(runtime, manifest, fuel, vehicle);
        int weighted = 0;
        int possible = 0;
        for (Map.Entry<Dimension, Integer> entry : contribution.entrySet()) {
            weighted += entry.getValue() * profile.priority(entry.getKey());
            possible += entry.getValue() * 10;
        }
        int doctrineFit = possible == 0 ? 0
                : clamp(weighted * 100 / possible, 0, 100);
        int baseValue = baseValue(runtime.vehicleClass());
        int poolBonus = value(vehicle, "motorPoolSiteKey").isBlank() ? 0 : 8;
        int strategicValue = Math.max(0,
                baseValue * readiness / 100 + doctrineFit / 3 + poolBonus);
        boolean captured = VehicleRuntimeAuthority.seized(vehicle)
                && runtime.ownerType()
                == VehicleRuntimeAuthority.OwnerType.FACTION;
        boolean catastrophic = "wreck".equalsIgnoreCase(runtime.condition())
                || runtime.integrity() < 25;
        boolean poorFit = doctrineFit < 32
                && strategicValue < Math.max(18, baseValue / 2);
        boolean salvageRecommended = captured
                && !"salvaged".equalsIgnoreCase(runtime.condition())
                && (catastrophic || readiness < 25 || poorFit);
        int salvagePriority = Math.max(0,
                VehicleRuntimeAuthority.salvageYield(vehicle) * 8
                        + 100 - readiness
                        + Math.max(0, 45 - doctrineFit)
                        - strategicValue / 2);

        ArrayList<String> reasons = new ArrayList<>();
        reasons.add("readiness " + readiness + "%");
        reasons.add("doctrine fit " + doctrineFit + "% for "
                + profile.label());
        if (!value(vehicle, "motorPoolSiteName").isBlank()) {
            reasons.add("assigned to " + value(vehicle, "motorPoolSiteName"));
        }
        if (catastrophic) reasons.add("catastrophic condition");
        if (salvageRecommended) {
            reasons.add("salvage is preferred over continued fleet investment");
        } else if (captured) {
            reasons.add("captured asset is worth retaining for faction use");
        }
        return new VehicleAssessment(vehicle, runtime.vehicleClass(),
                displayName(runtime), family, readiness, doctrineFit,
                strategicValue, salvagePriority, captured,
                salvageRecommended, Map.copyOf(contribution),
                List.copyOf(reasons));
    }

    static FleetSnapshot fleet(GamePanel game, Faction faction,
                               NpcFactionSite site) {
        Faction family = FactionIdentityAuthority.strategicFamily(faction);
        Profile profile = profile(family);
        EnumMap<Dimension, Integer> power = new EnumMap<>(Dimension.class);
        ArrayList<VehicleAssessment> vehicles = new ArrayList<>();
        int ready = 0;
        int heavy = 0;
        int total = 0;
        if (game != null && game.world != null
                && game.world.mapObjects != null) {
            for (MapObjectState vehicle : game.world.mapObjects) {
                if (!VehicleRuntimeAuthority.isVehicle(vehicle)
                        || !VehicleRuntimeAuthority.factionOwns(vehicle, family)
                        || "salvaged".equalsIgnoreCase(
                        value(vehicle, "condition"))) continue;
                VehicleAssessment assessment = assess(game, vehicle, site,
                        family);
                vehicles.add(assessment);
                if (assessment.readiness() >= 75) ready++;
                if (assessment.vehicleClass()
                        == VehicleRuntimeAuthority.VehicleClass.ARMORED_CAR
                        || assessment.vehicleClass()
                        == VehicleRuntimeAuthority.VehicleClass.TANK) heavy++;
                total += assessment.strategicValue();
                for (Map.Entry<Dimension, Integer> entry
                        : assessment.contribution().entrySet()) {
                    int scaled = entry.getValue()
                            * assessment.readiness() / 100;
                    scaled = scaled
                            * (10 + profile.priority(entry.getKey())) / 10;
                    power.merge(entry.getKey(), scaled, Integer::sum);
                }
            }
        }
        vehicles.sort(Comparator
                .comparingInt(VehicleAssessment::strategicValue).reversed()
                .thenComparing(VehicleAssessment::displayName));
        String summary = family.label + " fleet: " + vehicles.size()
                + " active vehicle(s), " + ready + " ready, " + heavy
                + " heavy, strategic power " + total + ". Doctrine: "
                + profile.label() + ".";
        return new FleetSnapshot(family, profile.label(), vehicles.size(),
                ready, heavy, total, Map.copyOf(power),
                List.copyOf(vehicles), summary);
    }

    static boolean shouldSalvageCaptured(GamePanel game,
                                         MapObjectState vehicle,
                                         NpcFactionSite site) {
        return assess(game, vehicle, site).salvageRecommended();
    }

    static int operationPriority(GamePanel game, MapObjectState vehicle,
                                 NpcFactionSite site, Operation operation) {
        VehicleAssessment assessment = assess(game, vehicle, site);
        VehicleRuntimeAuthority.Snapshot runtime =
                VehicleRuntimeAuthority.inspect(
                        game == null ? null : game.world, vehicle);
        int deficit = runtime == null ? 100
                : Math.max(0, 100 - runtime.integrity());
        Operation safe = operation == null ? Operation.SEIZURE : operation;
        return switch (safe) {
            case SEIZURE -> assessment.strategicValue() * 3
                    + assessment.doctrineFit() * 2
                    + assessment.readiness();
            case REPAIR -> assessment.strategicValue() * 3
                    + deficit * 2 + assessment.doctrineFit();
            case SALVAGE -> assessment.salvagePriority() * 3
                    + (assessment.salvageRecommended() ? 150 : 0)
                    - assessment.strategicValue();
        };
    }

    static List<String> inspectionLines(GamePanel game,
                                        MapObjectState vehicle,
                                        NpcFactionSite site) {
        VehicleAssessment assessment = assess(game, vehicle, site);
        Profile profile = profile(assessment.evaluatedFor());
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Faction vehicle doctrine: " + profile.label() + ".");
        lines.add("Fleet assessment: " + assessment.displayName()
                + " has readiness " + assessment.readiness()
                + "%, doctrine fit " + assessment.doctrineFit()
                + "%, and strategic value "
                + assessment.strategicValue() + ".");
        ArrayList<Map.Entry<Dimension, Integer>> strongest =
                new ArrayList<>(assessment.contribution().entrySet());
        strongest.sort(Comparator
                .comparingInt((Map.Entry<Dimension, Integer> entry)
                        -> entry.getValue()).reversed()
                .thenComparing(entry -> entry.getKey().label));
        StringBuilder roles = new StringBuilder();
        int shown = 0;
        for (Map.Entry<Dimension, Integer> entry : strongest) {
            if (entry.getValue() <= 0 || shown >= 3) continue;
            if (roles.length() > 0) roles.append(", ");
            roles.append(entry.getKey().label);
            shown++;
        }
        if (roles.length() > 0) {
            lines.add("Strongest vehicle roles: " + roles + ".");
        }
        if (assessment.captured()) {
            lines.add(assessment.salvageRecommended()
                    ? "Captured-asset decision: salvage is recommended because repair and retention offer poor doctrine value."
                    : "Captured-asset decision: retain this vehicle for faction use rather than automatically salvaging it.");
        }
        return List.copyOf(lines);
    }

    private static int readiness(VehicleRuntimeAuthority.Snapshot runtime,
                                 VehicleManifestAuthority.Snapshot manifest,
                                 VehicleFuelAuthority.Snapshot fuel,
                                 MapObjectState vehicle) {
        if (runtime == null
                || "salvaged".equalsIgnoreCase(runtime.condition())) return 0;
        String operation = clean(runtime.operationState(), "parked")
                .toLowerCase(Locale.ROOT);
        if ("wreck".equalsIgnoreCase(runtime.condition())) return 10;
        if (operation.equals("disabled") || operation.equals("dismantled")) {
            return 10;
        }
        int readiness = clamp(runtime.integrity(), 0, 100);
        if (VehicleRuntimeAuthority.damaged(vehicle)) {
            readiness = Math.min(readiness, 65);
        }
        int requiredCrew = Math.max(1,
                runtime.vehicleClass().crewRequired);
        if (manifest.driver().isBlank()) readiness = Math.min(readiness, 65);
        if (manifest.assignedCrew() < requiredCrew) {
            readiness = Math.min(readiness, 70);
        }
        if (fuel.capacity() > 0) {
            int fuelPercent = fuel.current() * 100 / fuel.capacity();
            if (fuelPercent <= 0) readiness = Math.min(readiness, 25);
            else if (fuelPercent < 25) readiness = Math.min(readiness, 55);
        }
        return clamp(readiness, 0, 100);
    }

    private static EnumMap<Dimension, Integer> baseline() {
        EnumMap<Dimension, Integer> map = new EnumMap<>(Dimension.class);
        for (Dimension dimension : Dimension.values()) map.put(dimension, 2);
        return map;
    }

    private static EnumMap<Dimension, Integer> baseContribution(
            VehicleRuntimeAuthority.VehicleClass vehicleClass) {
        EnumMap<Dimension, Integer> map = new EnumMap<>(Dimension.class);
        switch (vehicleClass) {
            case UTILITY_BIKE -> weights(map,
                    Dimension.TRANSPORT, 5,
                    Dimension.PATROL, 4,
                    Dimension.SMUGGLING, 6,
                    Dimension.ROUTE_CONTROL, 3,
                    Dimension.EVACUATION, 2);
            case CIVILIAN_CAR -> weights(map,
                    Dimension.TRANSPORT, 8,
                    Dimension.PATROL, 3,
                    Dimension.EVACUATION, 6,
                    Dimension.PRESTIGE, 6,
                    Dimension.SMUGGLING, 3,
                    Dimension.CONVOY, 2);
            case CARGO_TRUCK -> weights(map,
                    Dimension.CARGO, 10,
                    Dimension.LOGISTICS, 10,
                    Dimension.PRODUCTION_SUPPORT, 9,
                    Dimension.CONVOY, 8,
                    Dimension.ROUTE_CONTROL, 5,
                    Dimension.TRANSPORT, 4,
                    Dimension.SMUGGLING, 4);
            case ARMORED_CAR -> weights(map,
                    Dimension.PATROL, 10,
                    Dimension.ASSAULT, 7,
                    Dimension.DEFENSE, 9,
                    Dimension.INTIMIDATION, 9,
                    Dimension.ROUTE_CONTROL, 8,
                    Dimension.CONVOY, 7,
                    Dimension.STRATEGIC_PROJECTION, 6,
                    Dimension.TRANSPORT, 4);
            case TANK -> weights(map,
                    Dimension.ASSAULT, 14,
                    Dimension.DEFENSE, 14,
                    Dimension.INTIMIDATION, 14,
                    Dimension.ROUTE_CONTROL, 9,
                    Dimension.STRATEGIC_PROJECTION, 15,
                    Dimension.CONVOY, 4);
        }
        return map;
    }

    private static void applyRoleHints(EnumMap<Dimension, Integer> map,
                                       String role) {
        String text = clean(role, "").toLowerCase(Locale.ROOT);
        if (text.isBlank()) return;
        if (contains(text, "cargo", "freight", "haul")) {
            add(map, Dimension.CARGO, 3);
            add(map, Dimension.LOGISTICS, 3);
            add(map, Dimension.PRODUCTION_SUPPORT, 2);
        }
        if (contains(text, "patrol", "security", "warden")) {
            add(map, Dimension.PATROL, 3);
            add(map, Dimension.ROUTE_CONTROL, 2);
            add(map, Dimension.DEFENSE, 2);
        }
        if (contains(text, "assault", "military", "armor")) {
            add(map, Dimension.ASSAULT, 3);
            add(map, Dimension.DEFENSE, 2);
            add(map, Dimension.STRATEGIC_PROJECTION, 3);
        }
        if (contains(text, "convoy", "escort")) {
            add(map, Dimension.CONVOY, 3);
            add(map, Dimension.DEFENSE, 2);
            add(map, Dimension.ROUTE_CONTROL, 2);
        }
        if (contains(text, "staff", "passenger", "transit")) {
            add(map, Dimension.TRANSPORT, 3);
            add(map, Dimension.EVACUATION, 2);
        }
        if (contains(text, "smuggl", "covert", "black-market")) {
            add(map, Dimension.SMUGGLING, 4);
        }
        if (contains(text, "salvage", "recovery")) {
            add(map, Dimension.SALVAGE, 4);
        }
        if (contains(text, "prestige", "noble")) {
            add(map, Dimension.PRESTIGE, 4);
        }
    }

    private static void weights(EnumMap<Dimension, Integer> map,
                                Object... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            Dimension dimension = (Dimension)pairs[i];
            int amount = (Integer)pairs[i + 1];
            map.put(dimension, clamp(amount, 0, 15));
        }
    }

    private static void add(EnumMap<Dimension, Integer> map,
                            Dimension dimension, int amount) {
        map.merge(dimension, Math.max(0, amount), Integer::sum);
    }

    private static int baseValue(
            VehicleRuntimeAuthority.VehicleClass vehicleClass) {
        return switch (vehicleClass) {
            case UTILITY_BIKE -> 8;
            case CIVILIAN_CAR -> 14;
            case CARGO_TRUCK -> 32;
            case ARMORED_CAR -> 58;
            case TANK -> 105;
        };
    }

    private static boolean contains(String text, String... terms) {
        for (String term : terms) if (text.contains(term)) return true;
        return false;
    }

    private static String displayName(
            VehicleRuntimeAuthority.Snapshot snapshot) {
        return (clean(snapshot.manufacturer(), "") + " "
                + clean(snapshot.model(), snapshot.vehicleClass().label)).trim();
    }

    private static String value(MapObjectState vehicle, String key) {
        return vehicle == null ? ""
                : MapObjectState.stockValue(vehicle.stockState, key);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String clean(String value, String fallback) {
        String text = value == null ? ""
                : value.trim().replaceAll("\\s+", " ");
        return text.isBlank() ? (fallback == null ? "" : fallback) : text;
    }
}
