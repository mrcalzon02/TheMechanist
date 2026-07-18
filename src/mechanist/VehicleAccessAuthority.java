package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Explicit access decisions over the authoritative vehicle ownership ledger.
 * Each permission is evaluated independently so cargo, repair, passenger,
 * operation, refueling, deployment, and seizure rights cannot be conflated.
 */
final class VehicleAccessAuthority {
    enum Permission {
        OPERATION("operate or drive"),
        PASSENGER("ride as a passenger"),
        CARGO("open or transfer cargo"),
        REPAIR("repair or replace components"),
        REFUEL("refuel or recharge"),
        DEPLOYMENT("command or deploy"),
        SEIZURE("seize or confiscate");

        final String label;
        Permission(String label) { this.label = label; }
    }

    record Decision(Permission permission, boolean allowed, String authority,
                    String requirement, String consequence) {
        String summary() {
            return (allowed ? "Access available: " : "Access blocked: ")
                    + permission.label + " — " + requirement + ".";
        }
    }

    private VehicleAccessAuthority() { }

    static Decision evaluate(GamePanel game, MapObjectState vehicle,
                             Permission permission) {
        Permission requested = permission == null ? Permission.OPERATION : permission;
        if (game == null || game.world == null || vehicle == null
                || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return blocked(requested, "no loaded vehicle",
                    "a physical vehicle in the loaded zone is required",
                    "no vehicle state changes");
        }
        VehicleRuntimeAuthority.ensureInitialized(game.world, vehicle);
        VehicleRuntimeAuthority.Snapshot snapshot =
                VehicleRuntimeAuthority.inspect(game.world, vehicle);
        if (snapshot == null) {
            return blocked(requested, "missing vehicle record",
                    "the vehicle must have a readable ownership record",
                    "inspection remains available");
        }

        boolean playerOwned = snapshot.ownerType()
                == VehicleRuntimeAuthority.OwnerType.PLAYER;
        boolean member = snapshot.ownerType()
                == VehicleRuntimeAuthority.OwnerType.FACTION
                && snapshot.ownerFaction() != null
                && snapshot.ownerFaction() != Faction.NONE
                && FactionIdentityAuthority.sameFamily(
                snapshot.ownerFaction(), game.playerFaction());
        boolean publicService = snapshot.ownerType()
                == VehicleRuntimeAuthority.OwnerType.PUBLIC;
        boolean abandoned = snapshot.ownerType()
                == VehicleRuntimeAuthority.OwnerType.ABANDONED;
        boolean salvage = snapshot.ownerType()
                == VehicleRuntimeAuthority.OwnerType.SALVAGE;
        boolean privateProperty = snapshot.ownerType()
                == VehicleRuntimeAuthority.OwnerType.PRIVATE;
        boolean questBound = "true".equals(value(vehicle, "questObjective"))
                || snapshot.ownership().toLowerCase(Locale.ROOT).contains("quest");
        boolean seized = snapshot.ownership().toLowerCase(Locale.ROOT)
                .contains("seized");
        boolean military = "MILITARY".equals(snapshot.legalClass());
        boolean restricted = military || "RESTRICTED".equals(snapshot.legalClass());
        boolean title = matchingVehicleTitle(game, snapshot);
        boolean driverPermit = credential(game, "Driver permit")
                || credential(game, "Vehicle operator permit");
        boolean transitPass = credential(game, "Transit pass")
                || credential(game, "Public transit pass");
        boolean cargoPermit = credential(game, "Cargo access permit")
                || credential(game, "Freight manifest");
        boolean mechanicPermit = credential(game, "Mechanic permit")
                || credential(game, "Vehicle service authorization");
        boolean refuelPermit = credential(game, "Fuel authorization")
                || credential(game, "Charging authorization");
        boolean commandPermit = credential(game, "Motor pool command")
                || credential(game, "Vehicle deployment order");
        boolean seizureWarrant = credential(game, "Vehicle seizure warrant")
                || credential(game, "Confiscation order");
        boolean operational = operational(vehicle);

        if (playerOwned) {
            if (requested == Permission.OPERATION && !operational) {
                return blocked(requested, "owner access",
                        "repair the disabled powerplant, drive, mobility, or frame assembly first",
                        "ownership remains intact");
            }
            if (requested == Permission.DEPLOYMENT && restricted
                    && !title && !commandPermit) {
                return blocked(requested, "restricted owner deployment",
                        "carry this vehicle's title or a motor-pool deployment order",
                        "ordinary inspection, repair, and local custody remain available");
            }
            return allowed(requested, "recorded owner",
                    requested == Permission.SEIZURE
                            ? "the owner may transfer or surrender lawful custody"
                            : "the player ownership record authorizes this action",
                    "the action remains recorded in vehicle provenance");
        }

        if (member) {
            if (requested == Permission.OPERATION && !operational) {
                return blocked(requested, "faction motor-pool access",
                        "restore critical vehicle components before operation",
                        "faction custody remains unchanged");
            }
            if (requested == Permission.CARGO && questBound && !cargoPermit) {
                return blocked(requested, "quest-bound faction cargo",
                        "carry the assigned freight manifest or cargo access permit",
                        "the sealed objective cargo remains attached");
            }
            if (requested == Permission.DEPLOYMENT && restricted
                    && !commandPermit) {
                return blocked(requested, "restricted faction deployment",
                        "carry a motor-pool command or vehicle deployment order",
                        "repair, refueling, and ordinary motor-pool access remain available");
            }
            if (requested == Permission.SEIZURE && !seizureWarrant) {
                return blocked(requested, "faction confiscation authority",
                        "carry a vehicle seizure warrant or confiscation order",
                        "the current custody record remains authoritative");
            }
            return allowed(requested, "faction membership",
                    "the player belongs to the controlling faction family",
                    "use remains faction-authorized and provenance-traceable");
        }

        if (publicService) {
            return switch (requested) {
                case PASSENGER -> transitPass
                        ? allowed(requested, "public transit credential",
                        "the carried transit pass authorizes boarding",
                        "fares and route rules still apply")
                        : blocked(requested, "public transit service",
                        "carry a public transit pass",
                        "the vehicle remains public-service property");
                case OPERATION -> driverPermit
                        ? allowed(requested, "licensed public-service driver",
                        "the carried driver permit authorizes operation",
                        "route and service duties remain recorded")
                        : blocked(requested, "public-service operation",
                        "carry a driver or vehicle-operator permit",
                        "passenger access may still be available");
                case REPAIR -> mechanicPermit
                        ? allowed(requested, "public maintenance authorization",
                        "the carried mechanic permit authorizes service work",
                        "parts and labor costs still apply")
                        : blocked(requested, "public maintenance authorization",
                        "carry a mechanic permit or vehicle-service authorization",
                        "the vehicle remains available to authorized staff");
                case REFUEL -> refuelPermit
                        ? allowed(requested, "public fuel authorization",
                        "the carried fuel or charging authorization permits service",
                        "fuel or power stock is still consumed")
                        : blocked(requested, "public fuel authorization",
                        "carry fuel or charging authorization",
                        "the public vehicle remains locked to service staff");
                case CARGO, DEPLOYMENT, SEIZURE -> blocked(requested,
                        "public-service restricted function",
                        "obtain the relevant agency cargo, command, or confiscation authority",
                        "public custody remains unchanged");
            };
        }

        if (abandoned) {
            return switch (requested) {
                case REPAIR -> allowed(requested, "abandoned-property safety work",
                        "stabilization and inspection may occur before a lawful claim",
                        "repair does not itself transfer ownership");
                case SEIZURE -> !restricted
                        ? allowed(requested, "civilian abandonment claim",
                        "the vehicle may be claimed through the existing abandonment action",
                        "former ownership remains in provenance")
                        : blocked(requested, "restricted abandoned property",
                        "obtain formal confiscation authority for restricted or military property",
                        "inspection and hazard control remain available");
                case OPERATION, PASSENGER, CARGO, REFUEL, DEPLOYMENT ->
                        blocked(requested, "unclaimed abandoned property",
                                "establish a lawful salvage or abandonment claim first",
                                "repair and inspection do not grant operation rights");
            };
        }

        if (salvage) {
            return switch (requested) {
                case REPAIR -> mechanicPermit
                        ? allowed(requested, "authorized salvage reconstruction",
                        "the mechanic permit authorizes stabilization before a new title",
                        "ordinary maintenance cannot restore a fully dismantled hulk")
                        : blocked(requested, "salvage reconstruction",
                        "carry a mechanic permit and establish a reconstruction project",
                        "stripping through the salvage authority remains available");
                case CARGO -> allowed(requested, "salvage access",
                        "loose recoverable material may be inspected or stripped",
                        "salvage history and one-time yields remain authoritative");
                case SEIZURE -> allowed(requested, "salvage claim",
                        "the hulk may enter a lawful salvage-custody transaction",
                        "a new operational title is still required");
                case OPERATION, PASSENGER, REFUEL, DEPLOYMENT ->
                        blocked(requested, "non-operational salvage hulk",
                                "reconstruct and retitle the vehicle before use",
                                "the physical hulk remains available for salvage");
            };
        }

        if (privateProperty || seized) {
            if (requested == Permission.SEIZURE && seizureWarrant) {
                return allowed(requested, "formal confiscation authority",
                        "the carried warrant authorizes custody transfer",
                        "former owner and seizure reason remain recorded");
            }
            if (requested == Permission.REPAIR && mechanicPermit) {
                return allowed(requested, "contract maintenance authority",
                        "the carried mechanic authorization permits service without title transfer",
                        "repair does not grant operation or cargo rights");
            }
            return blocked(requested, "private or seized property",
                    "obtain owner authorization, title transfer, or the specific legal warrant",
                    "the current custody and cargo record remain unchanged");
        }

        return blocked(requested, "unassigned vehicle custody",
                "establish ownership or a specific service authorization",
                "inspection remains available without granting use");
    }

    static List<String> inspectionLines(GamePanel game, MapObjectState vehicle) {
        ArrayList<String> lines = new ArrayList<>();
        for (Permission permission : Permission.values()) {
            Decision decision = evaluate(game, vehicle, permission);
            lines.add(permission.label + ": "
                    + (decision.allowed() ? "allowed" : "blocked")
                    + " — " + decision.requirement() + ".");
        }
        return List.copyOf(lines);
    }

    private static boolean operational(MapObjectState vehicle) {
        String operation = value(vehicle, "operationState")
                .toLowerCase(Locale.ROOT);
        String condition = value(vehicle, "condition")
                .toLowerCase(Locale.ROOT);
        return !operation.equals("disabled") && !operation.equals("dismantled")
                && !condition.equals("wreck") && !condition.equals("salvaged");
    }

    private static boolean matchingVehicleTitle(
            GamePanel game, VehicleRuntimeAuthority.Snapshot snapshot) {
        if (game == null || snapshot == null) return false;
        String identity = (snapshot.manufacturer() + " " + snapshot.model())
                .trim().toLowerCase(Locale.ROOT);
        if (identity.isBlank()) return false;
        for (String carried : game.inventory) {
            if (carried == null) continue;
            String low = carried.toLowerCase(Locale.ROOT);
            if (low.contains("vehicle title") && low.contains(identity)) return true;
        }
        return false;
    }

    private static boolean credential(GamePanel game, String expected) {
        if (game == null || expected == null || expected.isBlank()) return false;
        String needle = expected.toLowerCase(Locale.ROOT);
        for (String carried : game.inventory) {
            if (carried != null
                    && carried.toLowerCase(Locale.ROOT).contains(needle)) return true;
        }
        return false;
    }

    private static Decision allowed(Permission permission, String authority,
                                    String requirement, String consequence) {
        return new Decision(permission, true, authority, requirement, consequence);
    }

    private static Decision blocked(Permission permission, String authority,
                                    String requirement, String consequence) {
        return new Decision(permission, false, authority, requirement, consequence);
    }

    private static String value(MapObjectState vehicle, String key) {
        return vehicle == null ? ""
                : MapObjectState.stockValue(vehicle.stockState, key);
    }
}
