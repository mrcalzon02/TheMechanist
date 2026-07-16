package mechanist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Authoritative runtime schema for the parked vehicle fixtures already placed
 * by world generation. Vehicle truth remains attached to MapObjectState so the
 * existing world/save pipeline persists it without a parallel registry.
 */
final class VehicleRuntimeAuthority {
    static final String SCHEMA_VERSION = "1";

    enum VehicleClass {
        UTILITY_BIKE(
                AssetIntegrationDisciplineAuthority.PARKED_UTILITY_BIKE,
                "Utility bike", "light transit", "LIGHT", 1, 1, 1,
                90, 3, "CIVILIAN",
                List.of("Sumpwheel Cooperative", "Aurel Cycle Works"),
                List.of("Utility Cycle Mk II", "Hab Courier")),
        CIVILIAN_CAR(
                AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                "Civilian car", "personal transport", "LIGHT", 4, 1, 3,
                180, 5, "CIVILIAN",
                List.of("Aurel Civil Motorworks", "Marrow Transit Works"),
                List.of("Hab-Runner Four", "Spine Compact")),
        CARGO_TRUCK(
                AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                "Cargo truck", "commercial freight", "MEDIUM", 3, 18, 7,
                240, 8, "COMMERCIAL",
                List.of("Guild Haulage Works", "Sump Ledger Motorworks"),
                List.of("Cargo-12 Hauler", "Longbed Utility")),
        ARMORED_CAR(
                AssetIntegrationDisciplineAuthority.PARKED_ARMORED_CAR,
                "Armored car", "security patrol", "HEAVY", 4, 4, 10,
                360, 12, "RESTRICTED",
                List.of("Wardens Motor Pool", "Bastion Security Works"),
                List.of("Patrol Bastion", "Ward Car Mk IV")),
        TANK(
                AssetIntegrationDisciplineAuthority.PARKED_TANK,
                "Armored crawler", "military armor", "VERY_HEAVY", 5, 2, 18,
                900, 24, "MILITARY",
                List.of("Munitorum Forge Line", "Cloister Red Foundry"),
                List.of("Crawler IX", "Siege Tractor Pattern"));

        final String type;
        final String label;
        final String role;
        final String durabilityTier;
        final int seats;
        final int cargoCapacity;
        final int crewRequired;
        final int purchasePrice;
        final int salvageBase;
        final String legalClass;
        final List<String> manufacturers;
        final List<String> models;

        VehicleClass(String type, String label, String role, String durabilityTier,
                     int seats, int cargoCapacity, int crewRequired,
                     int purchasePrice, int salvageBase, String legalClass,
                     List<String> manufacturers, List<String> models) {
            this.type = type;
            this.label = label;
            this.role = role;
            this.durabilityTier = durabilityTier;
            this.seats = seats;
            this.cargoCapacity = cargoCapacity;
            this.crewRequired = crewRequired;
            this.purchasePrice = purchasePrice;
            this.salvageBase = salvageBase;
            this.legalClass = legalClass;
            this.manufacturers = List.copyOf(manufacturers);
            this.models = List.copyOf(models);
        }
    }

    enum Component {
        FRAME("frame", "Frame / chassis"),
        POWERPLANT("powerplant", "Engine / powerplant"),
        DRIVE("drive", "Transmission / drive"),
        MOBILITY("mobility", "Wheels / tracks"),
        ARMOR("armor", "Armor / hull"),
        LIGHTS("lights", "Headlights / optics"),
        CARGO("cargo", "Cargo assembly"),
        CREW("crew", "Crew compartment");

        final String key;
        final String label;
        Component(String key, String label) {
            this.key = key;
            this.label = label;
        }
    }

    enum OwnerType {
        PLAYER, FACTION, PUBLIC, PRIVATE, ABANDONED, SALVAGE, UNASSIGNED
    }

    record Snapshot(VehicleClass vehicleClass, String manufacturer, String model,
                    String variant, String productionBatch, OwnerType ownerType,
                    String ownerName, Faction ownerFaction, String ownership,
                    String legalClass, String condition, String operationState,
                    int integrity, Map<Component, Integer> components,
                    List<String> formerOwners, List<String> history,
                    boolean forSale, int purchasePrice, int salvageYield,
                    String accessSummary) { }

    record Result(boolean success, boolean changed, String action,
                  String message, MapObjectState vehicle, int amount) {
        static Result blocked(String action, String message, MapObjectState vehicle) {
            return new Result(false, false, clean(action, "vehicle action"),
                    clean(message, "The vehicle action was blocked."), vehicle, 0);
        }
    }

    private static final List<String> VARIANTS = List.of(
            "Standard pattern", "Refurbished service pattern", "Faction field refit",
            "Cold-start package", "Heavy-duty suspension", "Economy service package");

    private VehicleRuntimeAuthority() { }

    static List<VehicleClass> catalog() {
        return List.of(VehicleClass.values());
    }

    static boolean isVehicle(MapObjectState object) {
        return object != null && RoadTransitFixtureAuthority.isVehicleType(object.type);
    }

    static VehicleClass vehicleClass(String type) {
        String canonical = AssetIntegrationDisciplineAuthority.canonicalType(type);
        for (VehicleClass value : VehicleClass.values()) {
            if (value.type.equals(canonical)) return value;
        }
        return VehicleClass.CIVILIAN_CAR;
    }

    static void initialize(World world, MapObjectState vehicle, Faction ownerFaction,
                           String ownership, String role, boolean forSale, Random random) {
        if (vehicle == null || !RoadTransitFixtureAuthority.isVehicleType(vehicle.type)) return;
        if (SCHEMA_VERSION.equals(value(vehicle, "vehicleSchema"))) {
            recomputeCondition(vehicle);
            return;
        }
        VehicleClass definition = vehicleClass(vehicle.type);
        Random r = random == null
                ? new Random(Objects.hash(world == null ? 0L : world.seed,
                vehicle.id, vehicle.x, vehicle.y, definition.name())) : random;
        Faction safeFaction = ownerFaction == null ? parseFaction(value(vehicle, "ownerFaction")) : ownerFaction;
        String safeOwnership = clean(ownership, value(vehicle, "ownership"));
        if (safeOwnership.isBlank()) {
            safeOwnership = vehicle.label != null
                    && vehicle.label.toLowerCase(Locale.ROOT).contains("abandoned")
                    ? "abandoned" : safeFaction != Faction.NONE ? "faction" : "private";
        }
        OwnerType ownerType = inferOwnerType(safeOwnership, safeFaction, forSale);
        String manufacturer = pick(definition.manufacturers, r);
        String model = pick(definition.models, r);
        String variant = pick(VARIANTS, r);
        String batch = "Batch " + (1000 + Math.floorMod(Objects.hash(
                world == null ? 0L : world.seed, vehicle.id, vehicle.x, vehicle.y), 9000));
        String facility = world == null || world.zoneType == null
                ? "unrecorded vehicle works" : world.zoneType.label + " vehicle works";

        set(vehicle, "vehicleSchema", SCHEMA_VERSION);
        set(vehicle, "vehicleClass", definition.name());
        set(vehicle, "manufacturer", manufacturer);
        set(vehicle, "model", model);
        set(vehicle, "variant", variant);
        set(vehicle, "productionBatch", batch);
        set(vehicle, "facilityOrigin", facility);
        set(vehicle, "componentSource", manufacturer + " / " + facility);
        set(vehicle, "durabilityTier", definition.durabilityTier);
        set(vehicle, "seats", Integer.toString(definition.seats));
        set(vehicle, "cargoCapacity", Integer.toString(definition.cargoCapacity));
        set(vehicle, "crewRequired", Integer.toString(definition.crewRequired));
        set(vehicle, "purchasePrice", Integer.toString(definition.purchasePrice));
        set(vehicle, "legalClass", definition.legalClass);
        set(vehicle, "ownerType", ownerType.name());
        set(vehicle, "ownerFaction", safeFaction.name());
        set(vehicle, "ownerName", defaultOwnerName(ownerType, safeFaction));
        set(vehicle, "ownership", safeOwnership.toLowerCase(Locale.ROOT));
        set(vehicle, "vehicleRole", clean(role, definition.role));
        set(vehicle, "forSale", Boolean.toString(forSale));
        set(vehicle, "operationState", "parked");
        set(vehicle, "formerOwners", "");
        set(vehicle, "damageHistory", "");
        set(vehicle, "repairHistory", "");
        set(vehicle, "captureHistory", "");
        set(vehicle, "salvageHistory", "");
        set(vehicle, "deploymentHistory", "");
        for (Component component : Component.values()) {
            set(vehicle, componentKey(component), "100");
        }
        append(vehicle, "deploymentHistory", "Generated at " + facility);
        append(vehicle, "vehicleHistory", "Registered " + manufacturer + " " + model
                + " / " + variant + " / " + batch);
        recomputeCondition(vehicle);
    }

    static void ensureInitialized(World world, MapObjectState vehicle) {
        if (vehicle == null || !isVehicle(vehicle)) return;
        initialize(world, vehicle, parseFaction(value(vehicle, "ownerFaction")),
                value(vehicle, "ownership"), value(vehicle, "vehicleRole"),
                bool(value(vehicle, "forSale")), null);
    }

    static Snapshot inspect(World world, MapObjectState vehicle) {
        if (vehicle == null || !isVehicle(vehicle)) return null;
        ensureInitialized(world, vehicle);
        VehicleClass definition = vehicleClass(vehicle.type);
        LinkedHashMap<Component, Integer> components = new LinkedHashMap<>();
        for (Component component : Component.values()) {
            components.put(component, component(vehicle, component));
        }
        int integrity = integrity(vehicle);
        OwnerType ownerType = parseOwnerType(value(vehicle, "ownerType"));
        Faction ownerFaction = parseFaction(value(vehicle, "ownerFaction"));
        return new Snapshot(definition,
                value(vehicle, "manufacturer"), value(vehicle, "model"),
                value(vehicle, "variant"), value(vehicle, "productionBatch"),
                ownerType, value(vehicle, "ownerName"), ownerFaction,
                value(vehicle, "ownership"), value(vehicle, "legalClass"),
                value(vehicle, "condition"), value(vehicle, "operationState"),
                integrity, Map.copyOf(components), split(value(vehicle, "formerOwners")),
                split(value(vehicle, "vehicleHistory")), bool(value(vehicle, "forSale")),
                intValue(value(vehicle, "purchasePrice"), definition.purchasePrice),
                salvageYield(vehicle), accessSummary(ownerType, ownerFaction));
    }

    static String inspectionLine(World world, MapObjectState vehicle) {
        Snapshot snapshot = inspect(world, vehicle);
        if (snapshot == null) return "VEHICLE: no authoritative vehicle record is available.";
        StringBuilder components = new StringBuilder();
        for (Map.Entry<Component, Integer> entry : snapshot.components().entrySet()) {
            if (components.length() > 0) components.append(", ");
            components.append(entry.getKey().key).append(" ").append(entry.getValue()).append("%");
        }
        return "VEHICLE: " + clean(snapshot.manufacturer(), snapshot.vehicleClass().label)
                + " " + clean(snapshot.model(), snapshot.vehicleClass().label)
                + " / " + clean(snapshot.variant(), "standard pattern")
                + ". Class: " + snapshot.vehicleClass().label
                + "; condition " + clean(snapshot.condition(), "unknown")
                + " (integrity " + snapshot.integrity() + "%). Owner: "
                + clean(snapshot.ownerName(), snapshot.ownerType().name().toLowerCase(Locale.ROOT))
                + (snapshot.ownerFaction() == Faction.NONE ? "" : " / " + snapshot.ownerFaction().label)
                + "; status " + clean(snapshot.ownership(), "unassigned")
                + "; legal class " + clean(snapshot.legalClass(), "unlisted")
                + "; access " + snapshot.accessSummary()
                + ". Components: " + components + ".";
    }

    static Result interact(GamePanel game, MapObjectState vehicle) {
        if (game == null || game.world == null || vehicle == null || !isVehicle(vehicle)) {
            return Result.blocked("inspect vehicle", "No vehicle is available.", vehicle);
        }
        ensureInitialized(game.world, vehicle);
        Snapshot snapshot = inspect(game.world, vehicle);
        if (snapshot.ownerType() == OwnerType.ABANDONED
                && snapshot.condition() != null
                && !snapshot.condition().equals("wreck")
                && !snapshot.condition().equals("salvaged")) {
            return claimAbandoned(game, vehicle);
        }
        if ((snapshot.ownerType() == OwnerType.SALVAGE
                || "wreck".equals(snapshot.condition()))
                && !"salvaged".equals(snapshot.condition())) {
            return salvageForPlayer(game, vehicle);
        }
        return new Result(true, false, "inspect vehicle",
                inspectionLine(game.world, vehicle), vehicle, 0);
    }

    static Result purchaseNearestForSale(GamePanel game, MapObjectState dealership) {
        if (game == null || game.world == null) {
            return Result.blocked("purchase vehicle", "No vehicle market world is loaded.", null);
        }
        MapObjectState vehicle = nearest(game.world, dealership, candidate -> {
            if (!isVehicle(candidate)) return false;
            ensureInitialized(game.world, candidate);
            VehicleClass definition = vehicleClass(candidate.type);
            return bool(value(candidate, "forSale"))
                    && !"MILITARY".equals(definition.legalClass)
                    && !"salvaged".equals(value(candidate, "condition"));
        });
        if (vehicle == null) {
            return Result.blocked("purchase vehicle",
                    "DEALERSHIP: no titled civilian or commercial vehicle is currently staged for sale.", null);
        }
        Snapshot before = inspect(game.world, vehicle);
        int price = before.purchasePrice();
        if (!game.spendImperialScript(price)) {
            return Result.blocked("purchase vehicle",
                    "DEALERSHIP: " + before.manufacturer() + " " + before.model()
                            + " costs " + price + " script; you do not have enough carried script.",
                    vehicle);
        }
        String oldOwner = ownerDisplay(before);
        appendFormerOwner(vehicle, oldOwner);
        set(vehicle, "ownerType", OwnerType.PLAYER.name());
        set(vehicle, "ownerName", playerName(game));
        set(vehicle, "ownerFaction", game.playerFaction().name());
        set(vehicle, "ownership", "player-owned");
        set(vehicle, "forSale", "false");
        append(vehicle, "captureHistory", "Purchased from " + oldOwner + " at turn " + game.turn);
        append(vehicle, "vehicleHistory", "Legal title transferred to " + playerName(game)
                + " for " + price + " script at turn " + game.turn);
        String title = "Vehicle title: " + before.manufacturer() + " " + before.model();
        if (!game.inventory.contains(title)) game.addInventoryItem(title, null);
        game.rebuildItemContainersFromLegacyLists();
        VehicleExpansionAttentionAuthority.Attention attention =
                VehicleExpansionAttentionAuthority.applyAcquisition(game, vehicle,
                        "dealership purchase");
        return new Result(true, true, "purchase vehicle",
                "DEALERSHIP PURCHASE: paid " + price + " script for "
                        + before.manufacturer() + " " + before.model()
                        + ". The staged vehicle now carries the player's legal title. "
                        + attention.summary(), vehicle, price);
    }

    static Result serviceNearestPlayerVehicle(GamePanel game, MapObjectState garage) {
        if (game == null || game.world == null) {
            return Result.blocked("service vehicle", "No vehicle-service world is loaded.", null);
        }
        MapObjectState vehicle = nearest(game.world, garage,
                candidate -> isVehicle(candidate) && playerOwns(game, candidate)
                        && !"salvaged".equals(value(candidate, "condition")));
        if (vehicle == null) {
            return Result.blocked("service vehicle",
                    "SERVICE GARAGE: no player-owned vehicle is staged in this zone.", null);
        }
        ensureInitialized(game.world, vehicle);
        Component worst = worstDamagedComponent(vehicle);
        if (worst == null) {
            return new Result(true, false, "service vehicle",
                    "SERVICE GARAGE: " + displayName(vehicle)
                            + " is already at full component integrity; no payment was taken.",
                    vehicle, 0);
        }
        boolean usedPart = removeInventoryLike(game, "Machine part");
        if (!usedPart && !game.spendImperialScript(8)) {
            return Result.blocked("service vehicle",
                    "SERVICE GARAGE: repairing " + worst.label
                            + " requires one Machine part or 8 script.", vehicle);
        }
        int before = component(vehicle, worst);
        int after = Math.min(100, before + 35);
        setComponent(vehicle, worst, after);
        append(vehicle, "repairHistory", worst.label + " " + before + "->" + after
                + " at turn " + game.turn + " / "
                + (usedPart ? "owner-supplied Machine part" : "garage service payment"));
        append(vehicle, "vehicleHistory", "Garage repair restored " + worst.label
                + " at turn " + game.turn);
        recomputeCondition(vehicle);
        if (usedPart) game.rebuildItemContainersFromLegacyLists();
        return new Result(true, true, "service vehicle",
                "SERVICE GARAGE: restored " + displayName(vehicle) + " "
                        + worst.label + " from " + before + "% to " + after + "% using "
                        + (usedPart ? "one Machine part" : "8 script") + ". Condition is now "
                        + value(vehicle, "condition") + ".", vehicle, after - before);
    }

    static Result claimAbandoned(GamePanel game, MapObjectState vehicle) {
        if (game == null || game.world == null || vehicle == null || !isVehicle(vehicle)) {
            return Result.blocked("claim vehicle", "No abandoned vehicle is available.", vehicle);
        }
        ensureInitialized(game.world, vehicle);
        Snapshot before = inspect(game.world, vehicle);
        if (before.ownerType() != OwnerType.ABANDONED) {
            return Result.blocked("claim vehicle", "This vehicle is not abandoned property.", vehicle);
        }
        if ("MILITARY".equals(before.legalClass()) || "RESTRICTED".equals(before.legalClass())) {
            return Result.blocked("claim vehicle",
                    "This abandoned " + before.vehicleClass().label
                            + " remains restricted property; inspection does not create a legal claim.",
                    vehicle);
        }
        appendFormerOwner(vehicle, ownerDisplay(before));
        set(vehicle, "ownerType", OwnerType.PLAYER.name());
        set(vehicle, "ownerName", playerName(game));
        set(vehicle, "ownerFaction", game.playerFaction().name());
        set(vehicle, "ownership", "salvage-claim");
        set(vehicle, "forSale", "false");
        append(vehicle, "captureHistory", "Abandonment claim by " + playerName(game)
                + " at turn " + game.turn);
        append(vehicle, "vehicleHistory", "Player abandonment claim recorded at turn "
                + game.turn);
        VehicleExpansionAttentionAuthority.Attention attention =
                VehicleExpansionAttentionAuthority.applyAcquisition(game, vehicle,
                        "abandonment claim");
        return new Result(true, true, "claim vehicle",
                "VEHICLE CLAIM: " + displayName(vehicle)
                        + " is now recorded as player-held salvage property. "
                        + attention.summary(), vehicle, 0);
    }

    static Result applyDamage(MapObjectState vehicle, Component component, int amount,
                              int turn, String source) {
        if (vehicle == null || !isVehicle(vehicle) || component == null || amount <= 0) {
            return Result.blocked("damage vehicle", "No valid vehicle damage was supplied.", vehicle);
        }
        int before = component(vehicle, component);
        int after = Math.max(0, before - amount);
        setComponent(vehicle, component, after);
        append(vehicle, "damageHistory", component.label + " " + before + "->" + after
                + " at turn " + Math.max(0, turn) + " / " + clean(source, "unrecorded damage"));
        append(vehicle, "vehicleHistory", "Damage affected " + component.label
                + " at turn " + Math.max(0, turn));
        recomputeCondition(vehicle);
        return new Result(true, before != after, "damage vehicle",
                displayName(vehicle) + " " + component.label + " changed from "
                        + before + "% to " + after + "%; condition "
                        + value(vehicle, "condition") + ".", vehicle, before - after);
    }

    static Result repairForFaction(MapObjectState vehicle, int amount, int turn,
                                   Faction faction, String source) {
        if (vehicle == null || !isVehicle(vehicle) || amount <= 0) {
            return Result.blocked("repair faction vehicle", "No repairable vehicle was supplied.", vehicle);
        }
        Component worst = worstDamagedComponent(vehicle);
        if (worst == null) {
            return new Result(true, false, "repair faction vehicle",
                    displayName(vehicle) + " already has full component integrity.", vehicle, 0);
        }
        int before = component(vehicle, worst);
        int after = Math.min(100, before + amount);
        setComponent(vehicle, worst, after);
        append(vehicle, "repairHistory", worst.label + " " + before + "->" + after
                + " at turn " + Math.max(0, turn) + " / "
                + factionLabel(faction) + " / " + clean(source, "faction repair"));
        recomputeCondition(vehicle);
        return new Result(true, before != after, "repair faction vehicle",
                factionLabel(faction) + " repaired " + displayName(vehicle) + " "
                        + worst.label + " from " + before + "% to " + after + "%.",
                vehicle, after - before);
    }

    static Result transferToFaction(MapObjectState vehicle, Faction faction, int turn,
                                    String reason) {
        if (vehicle == null || !isVehicle(vehicle) || faction == null || faction == Faction.NONE) {
            return Result.blocked("seize vehicle", "No valid faction vehicle transfer was supplied.", vehicle);
        }
        OwnerType beforeType = parseOwnerType(value(vehicle, "ownerType"));
        Faction beforeFaction = parseFaction(value(vehicle, "ownerFaction"));
        if (beforeType == OwnerType.FACTION
                && FactionIdentityAuthority.sameFamily(beforeFaction, faction)) {
            return new Result(true, false, "seize vehicle",
                    displayName(vehicle) + " is already controlled by " + faction.label + ".",
                    vehicle, 0);
        }
        String prior = clean(value(vehicle, "ownerName"), ownerDisplay(inspect(null, vehicle)));
        appendFormerOwner(vehicle, prior);
        set(vehicle, "ownerType", OwnerType.FACTION.name());
        set(vehicle, "ownerFaction", faction.name());
        set(vehicle, "ownerName", faction.label);
        set(vehicle, "ownership", "seized");
        set(vehicle, "forSale", "false");
        append(vehicle, "captureHistory", prior + " -> " + faction.label
                + " at turn " + Math.max(0, turn) + " / "
                + clean(reason, "faction seizure"));
        append(vehicle, "vehicleHistory", "Custody transferred to " + faction.label
                + " at turn " + Math.max(0, turn));
        return new Result(true, true, "seize vehicle",
                faction.label + " seized " + displayName(vehicle) + " from " + prior + ".",
                vehicle, 0);
    }

    static Result salvageForFaction(MapObjectState vehicle, Faction faction, int turn,
                                    String reason) {
        if (vehicle == null || !isVehicle(vehicle)) {
            return Result.blocked("salvage faction vehicle", "No vehicle is available to salvage.", vehicle);
        }
        if ("salvaged".equals(value(vehicle, "condition"))) {
            return Result.blocked("salvage faction vehicle",
                    displayName(vehicle) + " has already been stripped.", vehicle);
        }
        int yield = salvageYield(vehicle);
        markSalvaged(vehicle, turn, factionLabel(faction), reason);
        return new Result(true, true, "salvage faction vehicle",
                factionLabel(faction) + " stripped " + displayName(vehicle)
                        + " into " + yield + " faction stock unit(s).",
                vehicle, yield);
    }

    static Result salvageForPlayer(GamePanel game, MapObjectState vehicle) {
        if (game == null || game.world == null || vehicle == null || !isVehicle(vehicle)) {
            return Result.blocked("salvage vehicle", "No vehicle is available to salvage.", vehicle);
        }
        ensureInitialized(game.world, vehicle);
        Snapshot snapshot = inspect(game.world, vehicle);
        boolean allowed = snapshot.ownerType() == OwnerType.SALVAGE
                || snapshot.ownerType() == OwnerType.ABANDONED
                || snapshot.ownerType() == OwnerType.PLAYER
                || "wreck".equals(snapshot.condition());
        if (!allowed) {
            return Result.blocked("salvage vehicle",
                    "This vehicle remains controlled property and cannot be stripped into player storage.",
                    vehicle);
        }
        if ("salvaged".equals(snapshot.condition())) {
            return Result.blocked("salvage vehicle",
                    displayName(vehicle) + " has already been stripped.", vehicle);
        }
        int yield = salvageYield(vehicle);
        for (int i = 0; i < yield; i++) {
            game.addInventoryItem("Machine part", ItemProvenanceRecord.found(
                    "Machine part", game.world, game.turn,
                    "salvaged from " + displayName(vehicle)));
        }
        markSalvaged(vehicle, game.turn, playerName(game), "player salvage");
        game.rebuildItemContainersFromLegacyLists();
        VehicleExpansionAttentionAuthority.Attention attention =
                VehicleExpansionAttentionAuthority.applySalvage(game, vehicle, yield);
        return new Result(true, true, "salvage vehicle",
                "VEHICLE SALVAGE: stripped " + displayName(vehicle) + " into "
                        + yield + " Machine part(s). " + attention.summary(), vehicle, yield);
    }

    static boolean playerOwns(GamePanel game, MapObjectState vehicle) {
        if (game == null || vehicle == null || !isVehicle(vehicle)) return false;
        ensureInitialized(game.world, vehicle);
        return parseOwnerType(value(vehicle, "ownerType")) == OwnerType.PLAYER;
    }

    static boolean factionOwns(MapObjectState vehicle, Faction faction) {
        if (vehicle == null || !isVehicle(vehicle) || faction == null) return false;
        return parseOwnerType(value(vehicle, "ownerType")) == OwnerType.FACTION
                && FactionIdentityAuthority.sameFamily(
                parseFaction(value(vehicle, "ownerFaction")), faction);
    }

    static boolean damaged(MapObjectState vehicle) {
        return vehicle != null && isVehicle(vehicle) && integrity(vehicle) < 100
                && !"salvaged".equals(value(vehicle, "condition"));
    }

    static boolean seized(MapObjectState vehicle) {
        return vehicle != null && isVehicle(vehicle)
                && "seized".equalsIgnoreCase(value(vehicle, "ownership"));
    }

    static int integrity(MapObjectState vehicle) {
        if (vehicle == null) return 0;
        int total = 0;
        int count = 0;
        for (Component component : Component.values()) {
            total += component(vehicle, component);
            count++;
        }
        return count == 0 ? 0 : Math.max(0, Math.min(100, total / count));
    }

    static int salvageYield(MapObjectState vehicle) {
        VehicleClass definition = vehicleClass(vehicle == null ? null : vehicle.type);
        int integrity = integrity(vehicle);
        return Math.max(1, definition.salvageBase
                + Math.max(0, integrity - 20) / 20);
    }

    private static void markSalvaged(MapObjectState vehicle, int turn,
                                     String actor, String reason) {
        set(vehicle, "condition", "salvaged");
        set(vehicle, "operationState", "dismantled");
        set(vehicle, "forSale", "false");
        set(vehicle, "ownerType", OwnerType.SALVAGE.name());
        set(vehicle, "ownership", "stripped-hulk");
        for (Component component : Component.values()) setComponent(vehicle, component, 0);
        append(vehicle, "salvageHistory", clean(actor, "unknown salvager")
                + " at turn " + Math.max(0, turn) + " / "
                + clean(reason, "vehicle salvage"));
        append(vehicle, "vehicleHistory", "Vehicle dismantled at turn "
                + Math.max(0, turn));
        vehicle.glyph = 'o';
        vehicle.label = "Stripped " + displayName(vehicle) + " hulk";
    }

    private static void recomputeCondition(MapObjectState vehicle) {
        if (vehicle == null) return;
        if ("dismantled".equals(value(vehicle, "operationState"))) {
            set(vehicle, "condition", "salvaged");
            return;
        }
        int frame = component(vehicle, Component.FRAME);
        int power = component(vehicle, Component.POWERPLANT);
        int drive = component(vehicle, Component.DRIVE);
        int mobility = component(vehicle, Component.MOBILITY);
        int average = integrity(vehicle);
        String condition;
        String operation = value(vehicle, "operationState");
        if (frame <= 0) {
            condition = "wreck";
            operation = "disabled";
        } else if (power <= 0 || drive <= 0 || mobility <= 0) {
            condition = "disabled";
            operation = "disabled";
        } else if (average < 55) {
            condition = "damaged";
        } else if (average < 85) {
            condition = "worn";
        } else {
            condition = "serviceable";
        }
        set(vehicle, "condition", condition);
        set(vehicle, "operationState", clean(operation, "parked"));
    }

    private static Component worstDamagedComponent(MapObjectState vehicle) {
        Component worst = null;
        int value = 101;
        for (Component component : Component.values()) {
            int current = component(vehicle, component);
            if (current < value) {
                value = current;
                worst = component;
            }
        }
        return value >= 100 ? null : worst;
    }

    private static int component(MapObjectState vehicle, Component component) {
        return intValue(value(vehicle, componentKey(component)), 100);
    }

    private static void setComponent(MapObjectState vehicle, Component component, int value) {
        set(vehicle, componentKey(component), Integer.toString(
                Math.max(0, Math.min(100, value))));
    }

    private static String componentKey(Component component) {
        return "vehicleComponent" + component.name().charAt(0)
                + component.name().substring(1).toLowerCase(Locale.ROOT);
    }

    private interface VehiclePredicate {
        boolean test(MapObjectState vehicle);
    }

    private static MapObjectState nearest(World world, MapObjectState origin,
                                          VehiclePredicate predicate) {
        if (world == null || world.mapObjects == null) return null;
        int ox = origin == null ? 0 : origin.x;
        int oy = origin == null ? 0 : origin.y;
        ArrayList<MapObjectState> candidates = new ArrayList<>();
        for (MapObjectState object : world.mapObjects) {
            if (object != null && predicate.test(object)) candidates.add(object);
        }
        candidates.sort(Comparator
                .comparingInt((MapObjectState object) ->
                        Math.abs(object.x - ox) + Math.abs(object.y - oy))
                .thenComparing(object -> clean(object.id, object.label)));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static boolean removeInventoryLike(GamePanel game, String item) {
        if (game == null || item == null) return false;
        for (int i = 0; i < game.inventory.size(); i++) {
            if (ItemQuality.namesMatch(game.inventory.get(i), item)) {
                game.inventory.remove(i);
                return true;
            }
        }
        return false;
    }

    private static void appendFormerOwner(MapObjectState vehicle, String owner) {
        String cleanOwner = clean(owner, "unrecorded owner");
        List<String> owners = split(value(vehicle, "formerOwners"));
        if (!owners.contains(cleanOwner)) append(vehicle, "formerOwners", cleanOwner);
    }

    private static void append(MapObjectState vehicle, String key, String entry) {
        if (vehicle == null || key == null || key.isBlank()) return;
        ArrayList<String> values = new ArrayList<>(split(value(vehicle, key)));
        String cleanEntry = clean(entry, "");
        if (cleanEntry.isBlank()) return;
        values.add(cleanEntry);
        while (values.size() > 8) values.remove(0);
        set(vehicle, key, String.join("~", values));
    }

    private static List<String> split(String value) {
        ArrayList<String> out = new ArrayList<>();
        if (value == null || value.isBlank()) return List.of();
        for (String part : value.split("~")) {
            String clean = clean(part, "");
            if (!clean.isBlank()) out.add(clean);
        }
        return List.copyOf(out);
    }

    private static String accessSummary(OwnerType ownerType, Faction ownerFaction) {
        return switch (ownerType) {
            case PLAYER -> "owner operation, cargo, repair, and deployment";
            case FACTION -> factionLabel(ownerFaction)
                    + " operation, repair, cargo, and command authorization";
            case PUBLIC -> "public-service staff and route authorization";
            case PRIVATE -> "registered owner authorization";
            case ABANDONED -> "civilian abandonment claim where legal";
            case SALVAGE -> "salvage access only";
            case UNASSIGNED -> "inspection only until ownership is established";
        };
    }

    private static OwnerType inferOwnerType(String ownership, Faction faction,
                                            boolean forSale) {
        String low = clean(ownership, "").toLowerCase(Locale.ROOT);
        if (low.contains("abandon")) return OwnerType.ABANDONED;
        if (low.contains("salvage") || low.contains("hulk") || low.contains("wreck")) return OwnerType.SALVAGE;
        if (low.contains("public")) return OwnerType.PUBLIC;
        if (low.contains("player")) return OwnerType.PLAYER;
        if (low.contains("faction") || low.contains("motor-pool") || low.contains("seized")) return OwnerType.FACTION;
        if (forSale || low.contains("private") || low.contains("commercial")) return OwnerType.PRIVATE;
        if (faction != null && faction != Faction.NONE) return OwnerType.FACTION;
        return OwnerType.UNASSIGNED;
    }

    private static OwnerType parseOwnerType(String value) {
        try {
            return OwnerType.valueOf(clean(value, "UNASSIGNED").toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return OwnerType.UNASSIGNED;
        }
    }

    private static Faction parseFaction(String value) {
        try {
            return Faction.valueOf(clean(value, Faction.NONE.name()));
        } catch (Exception ignored) {
            return Faction.NONE;
        }
    }

    private static String defaultOwnerName(OwnerType type, Faction faction) {
        return switch (type) {
            case FACTION -> factionLabel(faction);
            case PUBLIC -> "Civic public service";
            case PRIVATE -> "Registered private owner";
            case ABANDONED -> "Abandoned property record";
            case SALVAGE -> "Salvage claim record";
            case PLAYER -> "Player";
            case UNASSIGNED -> "Unassigned custody";
        };
    }

    private static String ownerDisplay(Snapshot snapshot) {
        if (snapshot == null) return "unrecorded owner";
        String owner = clean(snapshot.ownerName(), snapshot.ownerType().name().toLowerCase(Locale.ROOT));
        if (snapshot.ownerFaction() != null && snapshot.ownerFaction() != Faction.NONE
                && !owner.contains(snapshot.ownerFaction().label)) {
            owner += " / " + snapshot.ownerFaction().label;
        }
        return owner;
    }

    private static String displayName(MapObjectState vehicle) {
        if (vehicle == null) return "vehicle";
        String manufacturer = value(vehicle, "manufacturer");
        String model = value(vehicle, "model");
        if (!manufacturer.isBlank() || !model.isBlank()) {
            return (manufacturer + " " + model).trim();
        }
        String label = clean(vehicle.label, vehicleClass(vehicle.type).label);
        int slash = label.indexOf(" / ");
        return slash < 0 ? label : label.substring(0, slash);
    }

    private static String playerName(GamePanel game) {
        return game != null && game.active != null
                ? clean(game.active.name, "Player") : "Player";
    }

    private static String factionLabel(Faction faction) {
        return faction == null || faction == Faction.NONE
                ? "Unaligned" : faction.label;
    }

    private static String value(MapObjectState vehicle, String key) {
        return vehicle == null ? "" : MapObjectState.stockValue(vehicle.stockState, key);
    }

    private static void set(MapObjectState vehicle, String key, String value) {
        if (vehicle == null) return;
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                key, cleanToken(value));
    }

    private static boolean bool(String value) {
        return Boolean.parseBoolean(clean(value, "false"));
    }

    private static int intValue(String value, int fallback) {
        try {
            return Integer.parseInt(clean(value, Integer.toString(fallback)));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String pick(List<String> values, Random random) {
        if (values == null || values.isEmpty()) return "Unrecorded";
        if (random == null) return values.get(0);
        return values.get(Math.floorMod(random.nextInt(), values.size()));
    }

    private static String cleanToken(String value) {
        return clean(value, "").replace(';', ',').replace('~', '/').replace('|', '/');
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return cleaned.isBlank() ? (fallback == null ? "" : fallback) : cleaned;
    }
}
