package mechanist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

/** Creates faction work from live market pressure and applies its concrete delivery outcome. */
final class FactionMarketContractAuthority {
    record WorkResult(boolean success, String message, FactionContract contract) { }
    record CompletionResult(String summary) { }
    private record Candidate(String item, String targetId, String pressure, String source, String outcome,
                             int pressureBonus) { }

    private static final String ESSENTIAL = "MARKET:ESSENTIAL:";
    private static final String RAW = "MARKET:RAW:";
    private static final String SHIPMENT = "MARKET:SHIPMENT:";
    private static final String REINFORCEMENT = "MARKET:REINFORCEMENT:";
    private static final String EVENT = "MARKET:EVENT:";
    private static final String DRAUGHT = "MARKET:DRAUGHT:";
    private static final String FACTION = "MARKET:FACTION:";
    private static final String VEHICLE_REPAIR = "MARKET:VEHICLE_REPAIR:";
    private static final String VEHICLE_SALVAGE = "MARKET:VEHICLE_SALVAGE:";
    private static final String VEHICLE_FUEL = "MARKET:VEHICLE_FUEL:";

    private FactionMarketContractAuthority() {}

    static String representativeLine(GamePanel game, NpcEntity representative) {
        if (game == null || representative == null || !representative.isFactionRepresentative()) {
            return "Faction work: speak to a faction representative to request an order.";
        }
        FactionContract active = activeForRepresentative(game, representative);
        if (active != null) {
            return "Faction work active: deliver " + active.publicRequiredItem() + " for the current "
                    + active.displayType() + " before requesting another order.";
        }
        Candidate candidate = candidate(game, representative.faction);
        if (candidate == null) return ProductionContractAuthority.representativeLine(game, representative);
        return "Faction market work available: deliver " + candidate.item + " to address " + candidate.pressure
                + ". Source: " + candidate.source + ". Expected result: " + candidate.outcome + ".";
    }

    static WorkResult accept(GamePanel game, NpcEntity representative) {
        if (game == null || representative == null || !representative.isFactionRepresentative()) {
            return new WorkResult(false, "Faction work unavailable: speak to a faction representative.", null);
        }
        FactionContract active = activeForRepresentative(game, representative);
        if (active != null) return new WorkResult(false,
                "Faction work unavailable: this faction already has an active " + active.displayType() + ".", active);
        Candidate candidate = candidate(game, representative.faction);
        if (candidate == null) {
            ProductionContractAuthority.WorkResult fallback = ProductionContractAuthority.accept(game, representative);
            return new WorkResult(fallback.success(), fallback.message(), fallback.contract());
        }
        FactionContract contract = create(game, representative, candidate);
        game.factionContracts.add(contract);
        return new WorkResult(true, "Accepted " + contract.displayFactionName() + " market supply contract: deliver "
                + contract.publicRequiredItem() + " for " + contract.payout + " script and standing +"
                + contract.repReward + ". Pressure: " + candidate.pressure + ". Expected result: "
                + candidate.outcome + ".", contract);
    }

    static CompletionResult complete(GamePanel game, FactionContract contract, String deliveredItem) {
        if (game == null || game.world == null || contract == null || !"MARKET".equals(contract.type)) {
            return new CompletionResult("");
        }
        String target = safe(contract.targetEntityId);
        String result;
        if (target.startsWith(ESSENTIAL)) {
            EssentialSupplyReserveRecord reserve = essentialById(game.world, target.substring(ESSENTIAL.length()));
            if (reserve != null) {
                int before = reserve.remaining;
                reserve.remaining = Math.min(reserve.capacity, reserve.remaining + 1);
                reserve.route = append(reserve.route, "player contract delivery through " + contract.displayFactionName());
                result = "Essential reserve " + reserve.itemName + " improved from " + before + "/" + reserve.capacity
                        + " to " + reserve.remaining + "/" + reserve.capacity + ".";
            } else result = "The named essential reserve is no longer present; the faction accepted the replacement stock.";
        } else if (target.startsWith(RAW)) {
            RawMaterialSupplyReserveRecord reserve = rawById(game.world, target.substring(RAW.length()));
            if (reserve != null) {
                int before = reserve.remaining;
                reserve.remaining = Math.min(reserve.capacity, reserve.remaining + 1);
                reserve.route = append(reserve.route, "player contract delivery through " + contract.displayFactionName());
                result = "Material reserve " + reserve.itemName + " improved from " + before + "/" + reserve.capacity
                        + " to " + reserve.remaining + "/" + reserve.capacity + ".";
            } else result = "The named material reserve is no longer present; the faction accepted the substitute stock.";
        } else if (target.startsWith(SHIPMENT)) {
            ShipmentProvenanceRecord shipment = ShipmentProvenanceAuthority.byId(game.world, target.substring(SHIPMENT.length()));
            if (shipment != null) {
                String before = shipment.status;
                shipment.status = "DELIVERED";
                shipment.remaining = 0;
                shipment.eventModifier = append(shipment.eventModifier, "player replacement delivery fulfilled the missing cargo");
                result = "Shipment " + shipment.id + " changed from " + before
                        + " to delivered replacement stock; no duplicate shelf cargo was created.";
            } else result = "The named shipment record is no longer present; the faction accepted direct replacement stock.";
        } else if (target.startsWith(REINFORCEMENT)) {
            PersonnelReplacementRequest request = replacementById(game.world, target.substring(REINFORCEMENT.length()));
            if (request != null) {
                int before = request.dueTurn;
                int support = GamePanel.TURNS_PER_HOUR * 6;
                request.dueTurn = Math.max(game.turn, request.dueTurn - support);
                request.expiresTurn = Math.max(request.dueTurn + 1, request.expiresTurn - support);
                request.reason = append(request.reason, "player supply delivery accelerated reception");
                result = "Reinforcement reception advanced from turn " + before + " to " + request.dueTurn + ".";
            } else result = "The named reinforcement request has already resolved; the supplies entered faction reserves.";
        } else if (target.startsWith(EVENT)) {
            TopDownWorldEventRecord event = eventById(game.world, target.substring(EVENT.length()));
            int replenished = replenishMatchingEssential(game.world, contract.faction, deliveredItem);
            if (event != null) {
                event.consequenceSummary = append(event.consequenceSummary,
                        "Player delivery of " + deliveredItem + " supported the local response");
                result = event.title + " response recorded the delivery"
                        + (replenished > 0 ? " and restored one matching reserve unit." : ".");
            } else result = "The event has ended; the delivered stock entered local faction reserves.";
        } else if (target.startsWith(DRAUGHT)) {
            DraughtCustodyRecord custody = NobleLuxuryProvenanceAuthority.custodyById(
                    game.world, target.substring(DRAUGHT.length()));
            if (custody != null) {
                custody.eventStatus = append(custody.eventStatus, "custody papers renewed by faction contract");
                result = custody.itemName + " custody papers were renewed at " + custody.vaultLabel
                        + "; the protected draught remains not for sale.";
            } else result = "The named draught custody has moved; the permit entered the noble house archive.";
        } else if (target.startsWith(VEHICLE_REPAIR)) {
            result = "Motor-pool repair materials were accepted for the named persistent vehicle work order.";
        } else if (target.startsWith(VEHICLE_SALVAGE)) {
            result = "Motor-pool salvage authorization was accepted for the named persistent vehicle work order.";
        } else if (target.startsWith(VEHICLE_FUEL)) {
            result = "Motor-pool fuel or power supplies were accepted for the named persistent vehicle work order.";
        } else {
            result = "The delivered stock entered " + contract.displayFactionName() + " internal market supply.";
        }

        Faction normalized = FactionInventoryStockAuthority.normalizeFaction(contract.faction);
        int pressure = game.factionMarketPressure.getOrDefault(normalized, 0);
        game.factionMarketPressure.put(normalized, Math.max(0, pressure - 5));
        publishCompletionNews(game, contract, result);
        return new CompletionResult(result);
    }

    private static Candidate candidate(GamePanel game, Faction rawFaction) {
        if (game == null || game.world == null) return null;
        World world = game.world;
        Faction faction = rawFaction == null ? Faction.NONE : rawFaction;
        for (TopDownWorldEventRecord event : world.topDownWorldEvents) {
            if (!active(event, game.worldTurn) || !appliesTo(event.targetFaction, faction)) continue;
            Candidate candidate = eventCandidate(event);
            if (candidate != null) return candidate;
        }
        for (ShipmentProvenanceRecord shipment : world.shipmentRecords) {
            if (shipment == null || !sameFaction(shipment.destinationFaction, faction)
                    || !("DELAYED".equals(shipment.status) || "INTERCEPTED".equals(shipment.status)
                    || "SCHEDULED".equals(shipment.status))) continue;
            String item = catalogItem(shipment.cargoItem, "Construction supplies");
            return new Candidate(item, SHIPMENT + shipment.id,
                    shipment.status.toLowerCase(Locale.ROOT) + " incoming shipment cargo",
                    shipment.supplier + " via " + shipment.arrivalNode,
                    "replace the missing cargo without creating duplicate delivered stock", 70);
        }
        for (PersonnelReplacementRequest request : world.replacementQueue) {
            if (request == null || !sameFaction(request.faction, faction) || request.expiresTurn < game.turn) continue;
            return new Candidate("Emergency rations", REINFORCEMENT + safe(request.deadNpcId),
                    "reinforcement reception for " + safe(request.deadName), request.source,
                    "advance the supported arrival window by up to six hours", 55);
        }
        for (EssentialSupplyReserveRecord reserve : world.essentialSupplyReserves) {
            if (reserve != null && sameFaction(reserve.faction, faction) && reserve.remaining <= 0) {
                return new Candidate(catalogItem(reserve.itemName, "Emergency rations"), ESSENTIAL + reserve.id,
                        reserve.category + " reserve depletion", reserve.sourceLabel,
                        "restore one exact unit to the finite reserve", 50);
            }
        }
        for (RawMaterialSupplyReserveRecord reserve : world.rawMaterialSupplyReserves) {
            if (reserve != null && sameFaction(reserve.faction, faction) && reserve.remaining <= 0) {
                return new Candidate(catalogItem(reserve.itemName, "Construction supplies"), RAW + reserve.id,
                        reserve.itemName + " material depletion", reserve.sourceLabel,
                        "restore one exact unit to the finite material reserve", 45);
            }
        }
        Candidate vehicle = vehicleCandidate(game, faction);
        if (vehicle != null) return vehicle;
        Faction normalized = FactionInventoryStockAuthority.normalizeFaction(faction);
        int pressure = game.factionMarketPressure.getOrDefault(normalized, 0);
        if (pressure > 0) return new Candidate("Emergency rations", FACTION + faction.name(),
                "recorded market pressure " + pressure, faction.label + " internal demand ledger",
                "reduce faction market pressure and add the delivery to internal supply", 30);
        Candidate draught = draughtCustodyCandidate(world, faction);
        if (draught != null) return draught;
        return identityCandidate(normalized, faction);
    }

    private static Candidate vehicleCandidate(GamePanel game, Faction faction) {
        if (game == null || game.world == null || game.world.mapObjects == null
                || faction == null || faction == Faction.NONE) return null;
        ArrayList<MapObjectState> salvage = new ArrayList<>();
        ArrayList<MapObjectState> criticalRepair = new ArrayList<>();
        ArrayList<MapObjectState> fuel = new ArrayList<>();
        ArrayList<MapObjectState> repair = new ArrayList<>();
        for (MapObjectState object : game.world.mapObjects) {
            if (!VehicleRuntimeAuthority.isVehicle(object)) continue;
            VehicleRuntimeAuthority.ensureInitialized(game.world, object);
            if (!VehicleRuntimeAuthority.factionOwns(object, faction)) continue;
            VehicleRuntimeAuthority.Snapshot snapshot =
                    VehicleRuntimeAuthority.inspect(game.world, object);
            if (snapshot == null || "salvaged".equals(snapshot.condition())) continue;
            FactionVehicleDoctrineAuthority.VehicleAssessment assessment =
                    FactionVehicleDoctrineAuthority.assess(game, object, null, faction);
            if (assessment.salvageRecommended()) {
                salvage.add(object);
                continue;
            }
            boolean damaged = VehicleRuntimeAuthority.damaged(object);
            if (damaged && criticalRepair(snapshot)) {
                criticalRepair.add(object);
                continue;
            }
            VehicleFuelAuthority.Snapshot energy =
                    VehicleFuelAuthority.inspect(game.world, object);
            if (energy.capacity() > 0
                    && energy.current() < Math.max(1, energy.capacity() / 4)) {
                fuel.add(object);
            } else if (damaged) {
                repair.add(object);
            }
        }
        salvage.sort(Comparator
                .comparingInt((MapObjectState object) ->
                        FactionVehicleDoctrineAuthority.assess(
                                game, object, null, faction).salvagePriority())
                .reversed()
                .thenComparing(object -> vehicleDisplayName(game, object))
                .thenComparing(object -> safe(object.id)));
        if (!salvage.isEmpty()) {
            MapObjectState target = salvage.get(0);
            FactionVehicleDoctrineAuthority.VehicleAssessment assessment =
                    FactionVehicleDoctrineAuthority.assess(
                            game, target, null, faction);
            String name = vehicleDisplayName(game, target);
            return new Candidate(catalogItem("Tool bundle", "Machine part"),
                    VEHICLE_SALVAGE + safe(target.id),
                    "seized vehicle salvage backlog for " + name,
                    faction.label + " local motor pool / readiness "
                            + assessment.readiness() + "% / doctrine fit "
                            + assessment.doctrineFit() + "%",
                    "salvage the seized vehicle wreck " + name
                            + " through its persistent vehicle record",
                    75 + Math.min(40, assessment.salvagePriority() / 4));
        }
        criticalRepair.sort(Comparator
                .comparingInt((MapObjectState object) ->
                        FactionVehicleDoctrineAuthority.assess(
                                game, object, null, faction).readiness())
                .thenComparingInt(object -> VehicleRuntimeAuthority.inspect(
                        game.world, object).integrity())
                .thenComparing(object -> vehicleDisplayName(game, object))
                .thenComparing(object -> safe(object.id)));
        if (!criticalRepair.isEmpty()) {
            return repairCandidate(game, faction, criticalRepair.get(0), true);
        }
        fuel.sort(Comparator
                .comparingInt((MapObjectState object) -> {
                    VehicleFuelAuthority.Snapshot energy =
                            VehicleFuelAuthority.inspect(game.world, object);
                    return energy.capacity() - energy.current();
                }).reversed()
                .thenComparing(object -> vehicleDisplayName(game, object))
                .thenComparing(object -> safe(object.id)));
        if (!fuel.isEmpty()) {
            MapObjectState target = fuel.get(0);
            VehicleFuelAuthority.Snapshot energy =
                    VehicleFuelAuthority.inspect(game.world, target);
            String name = vehicleDisplayName(game, target);
            return new Candidate(catalogItem("Fuel canister", "Machine part"),
                    VEHICLE_FUEL + safe(target.id),
                    "empty or low vehicle fuel or power reserve for " + name,
                    faction.label + " local motor pool / " + energy.current()
                            + "/" + energy.capacity() + " "
                            + energy.energyType() + " unit(s)",
                    "refuel the low-energy vehicle " + name
                            + " through its persistent fuel or power ledger",
                    65 + Math.min(60, energy.capacity() - energy.current()));
        }
        repair.sort(Comparator
                .comparingInt((MapObjectState object) ->
                        VehicleRuntimeAuthority.inspect(
                                game.world, object).integrity())
                .thenComparing(object -> vehicleDisplayName(game, object))
                .thenComparing(object -> safe(object.id)));
        return repair.isEmpty() ? null
                : repairCandidate(game, faction, repair.get(0), false);
    }

    private static Candidate repairCandidate(GamePanel game, Faction faction,
                                             MapObjectState target,
                                             boolean critical) {
        VehicleRuntimeAuthority.Snapshot snapshot =
                VehicleRuntimeAuthority.inspect(game.world, target);
        String name = vehicleDisplayName(game, target);
        int deficit = Math.max(0, 100 - snapshot.integrity());
        return new Candidate(catalogItem("Machine part", "Construction supplies"),
                VEHICLE_REPAIR + safe(target.id),
                (critical ? "critical damaged vehicle repair backlog for "
                        : "damaged vehicle repair backlog for ") + name,
                faction.label + " local motor pool / integrity "
                        + snapshot.integrity() + "% / condition "
                        + safe(snapshot.condition()),
                "repair the damaged vehicle " + name
                        + " through its persistent component record",
                (critical ? 75 : 55) + Math.min(60, deficit));
    }

    private static boolean criticalRepair(
            VehicleRuntimeAuthority.Snapshot snapshot) {
        if (snapshot == null) return false;
        String condition = safe(snapshot.condition()).toLowerCase(Locale.ROOT);
        String operation = safe(snapshot.operationState()).toLowerCase(Locale.ROOT);
        return condition.equals("wreck") || condition.equals("disabled")
                || condition.equals("damaged") || operation.equals("disabled");
    }

    private static String vehicleDisplayName(GamePanel game,
                                             MapObjectState vehicle) {
        VehicleRuntimeAuthority.Snapshot snapshot =
                VehicleRuntimeAuthority.inspect(
                        game == null ? null : game.world, vehicle);
        if (snapshot == null) return "vehicle";
        String manufacturer = safe(snapshot.manufacturer()).trim();
        String model = safe(snapshot.model()).trim();
        String fallback = snapshot.vehicleClass() == null
                ? "vehicle" : snapshot.vehicleClass().label;
        String combined = (manufacturer + " " + model).trim();
        return combined.isBlank() ? fallback : combined;
    }

    private static Candidate draughtCustodyCandidate(World world, Faction faction) {
        if (FactionInventoryStockAuthority.normalizeFaction(faction) != Faction.NOBLE) return null;
        for (DraughtCustodyRecord custody : world.draughtCustodyRecords) {
            if (custody == null || custody.heldQuantity <= 0 || custody.releasedForSale
                    || !sameFaction(custody.ownerFaction, faction)) continue;
            return new Candidate("Noble Commerce Permit", DRAUGHT + custody.id,
                    "protected custody for " + custody.itemName,
                    custody.houseOwner + " at " + custody.vaultLabel,
                    "renew the custody papers without releasing the draught for sale", 85);
        }
        return null;
    }

    private static Candidate eventCandidate(TopDownWorldEventRecord event) {
        String item = switch (event.eventType) {
            case "RELIEF_SHIPMENT", "TITHING_DECREE" -> "Emergency rations";
            case "INFRASTRUCTURE_REPAIR", "TRAIN_OUTAGE", "SUPPLY_SHOCK" -> "Construction supplies";
            case "EXPORT_BAN" -> "Trade chit";
            case "QUARANTINE" -> "Medkit";
            default -> null;
        };
        if (item == null) return null;
        return new Candidate(item, EVENT + event.id, event.title.toLowerCase(Locale.ROOT),
                event.newsExposure, "support the local response while preserving the event's stated limits", 80);
    }

    private static Candidate identityCandidate(Faction normalized, Faction exact) {
        return switch (normalized) {
            case BANDIT, CULTIST, HERETIC -> new Candidate("Street Stimm", FACTION + exact.name(),
                    "illicit narcotics demand", exact.label + " chem kitchens and dealer network",
                    "support internal use, sale income, leverage, and territorial trade", 35);
            case NOBLE -> new Candidate("Pearl Obscura", FACTION + exact.name(),
                    "estate luxury and private-medicine demand", exact.label + " broker network",
                    "support hospitality, gifting, favors, and private consumption", 45);
            default -> null;
        };
    }

    private static FactionContract create(GamePanel game, NpcEntity representative, Candidate candidate) {
        Faction faction = representative.faction == null ? Faction.NONE : representative.faction;
        int standing = game.factionStanding.getOrDefault(faction, 0);
        FactionContract contract = new FactionContract();
        contract.id = "M-" + Integer.toUnsignedString(Objects.hash(faction, representative.name, game.turn,
                candidate.targetId, game.factionContracts.size()), 36).toUpperCase(Locale.ROOT);
        contract.type = "MARKET";
        contract.faction = faction;
        contract.targetZoneKey = currentZoneKey(game);
        contract.targetEntityId = candidate.targetId;
        contract.targetName = candidate.pressure;
        contract.requiredTurnInItem = candidate.item;
        contract.minimumQualityTier = -1;
        contract.skillXpReward = 0;
        contract.payout = Math.max(60, ItemCatalog.priceFor(candidate.item) * 5 + candidate.pressureBonus
                + Math.max(0, standing) * 3);
        contract.repReward = 2 + Math.max(0, standing / 12);
        contract.spawned = true;
        contract.description = "Deliver one " + candidate.item + " to answer " + candidate.pressure
                + ". Recorded source: " + candidate.source + ". Completion will " + candidate.outcome + ".";
        return contract;
    }

    private static FactionContract activeForRepresentative(GamePanel game, NpcEntity representative) {
        if (game == null || representative == null) return null;
        Faction faction = representative.faction == null ? Faction.NONE : representative.faction;
        for (FactionContract contract : game.factionContracts) if (contract != null && !contract.completed
                && ContractTurnInAuthority.sameContractFaction(game, faction, contract.faction)) return contract;
        return null;
    }

    private static void publishCompletionNews(GamePanel game, FactionContract contract, String result) {
        int day = game.currentInnDay();
        String zone = game.world == null ? "unknown zone" : game.world.zoneType.label;
        PlayerNewsEvent news = PlayerNewsEvent.create(game.turn, day, "market service",
                contract.displayFactionName() + " supply delivery", contract.faction, zone, result, 3, game.seed);
        news.publicDay = day;
        game.playerNewsEvents.add(news);
        game.lastPlayerNewsReport = "Market service notice: " + contract.publicRequiredItem() + ". " + result;
    }

    private static int replenishMatchingEssential(World world, Faction faction, String item) {
        for (EssentialSupplyReserveRecord reserve : world.essentialSupplyReserves) {
            if (reserve == null || !sameFaction(reserve.faction, faction) || !ItemQuality.namesMatch(reserve.itemName, item)
                    || reserve.remaining >= reserve.capacity) continue;
            reserve.remaining++;
            return 1;
        }
        return 0;
    }

    private static EssentialSupplyReserveRecord essentialById(World world, String id) {
        for (EssentialSupplyReserveRecord reserve : world.essentialSupplyReserves)
            if (reserve != null && id.equals(reserve.id)) return reserve;
        return null;
    }

    private static RawMaterialSupplyReserveRecord rawById(World world, String id) {
        for (RawMaterialSupplyReserveRecord reserve : world.rawMaterialSupplyReserves)
            if (reserve != null && id.equals(reserve.id)) return reserve;
        return null;
    }

    private static PersonnelReplacementRequest replacementById(World world, String id) {
        for (PersonnelReplacementRequest request : world.replacementQueue)
            if (request != null && id.equals(safe(request.deadNpcId))) return request;
        return null;
    }

    private static TopDownWorldEventRecord eventById(World world, String id) {
        for (TopDownWorldEventRecord event : world.topDownWorldEvents)
            if (event != null && id.equals(event.id)) return event;
        return null;
    }

    private static boolean active(TopDownWorldEventRecord event, long turn) {
        return event != null && "ACTIVE".equals(event.status) && turn >= event.startWorldTurn && turn < event.endWorldTurn;
    }

    private static boolean appliesTo(Faction target, Faction faction) {
        return target == null || target == Faction.NONE || sameFaction(target, faction);
    }

    private static boolean sameFaction(Faction a, Faction b) {
        if (a == null || b == null) return false;
        return FactionInventoryStockAuthority.normalizeFaction(a) == FactionInventoryStockAuthority.normalizeFaction(b);
    }

    private static String catalogItem(String candidate, String fallback) {
        return candidate != null && ItemCatalog.get(candidate) != null ? candidate : fallback;
    }

    private static String currentZoneKey(GamePanel game) {
        if (game == null || game.atlas == null) return "";
        WorldAtlas atlas = game.atlas;
        return atlas.sectorX + "," + atlas.sectorY + "," + atlas.zoneX + "," + atlas.zoneY + ","
                + atlas.floor + "," + atlas.sewer;
    }

    private static String append(String existing, String addition) {
        if (addition == null || addition.isBlank()) return safe(existing);
        return safe(existing).isBlank() ? addition : safe(existing) + "; " + addition;
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
