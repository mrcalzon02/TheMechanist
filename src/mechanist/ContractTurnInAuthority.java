package mechanist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Owns atomic faction-contract proof validation, completion, and reward payout. */
final class ContractTurnInAuthority {
    record TurnInResult(boolean success, String message, FactionContract contract) { }

    private static final String VEHICLE_REPAIR_TARGET = "MARKET:VEHICLE_REPAIR:";
    private static final String VEHICLE_SALVAGE_TARGET = "MARKET:VEHICLE_SALVAGE:";
    private static final String VEHICLE_FUEL_TARGET = "MARKET:VEHICLE_FUEL:";

    private enum VehicleContractMode {
        REPAIR("repair", "damaged faction vehicle"),
        SALVAGE("salvage", "seized or wrecked faction vehicle"),
        REFUEL("refuel", "low-energy faction vehicle");

        final String action;
        final String targetDescription;

        VehicleContractMode(String action, String targetDescription) {
            this.action = action;
            this.targetDescription = targetDescription;
        }
    }

    private record VehicleContractResult(boolean applies, boolean changed,
                                         String summary) {
        static VehicleContractResult none() {
            return new VehicleContractResult(false, false, "");
        }
    }

    private ContractTurnInAuthority() { }

    static String representativeLine(GamePanel game, NpcEntity representative) {
        FactionContract contract = bestActiveContract(game, representative);
        if (contract == null) return "Contract turn-in: no active contract belongs to this representative's faction.";
        String problem = turnInProblem(game, representative, contract);
        String blueprintPreview = ConstructionBlueprintContractRewardAuthority.preview(contract);
        String vehiclePreview = vehicleContractPreview(game, contract);
        String consequencePreview = appendPreview(blueprintPreview, vehiclePreview);
        return problem == null
                ? "Contract turn-in ready: " + contract.displayType() + " for " + contract.payout
                        + " script and standing +" + contract.repReward + "."
                        + consequencePreview
                : "Contract turn-in blocked: " + problem + "."
                        + consequencePreview;
    }

    static TurnInResult turnInFirst(GamePanel game, NpcEntity representative) {
        FactionContract contract = bestActiveContract(game, representative);
        if (contract == null) return new TurnInResult(false,
                "No active contract belongs to this representative's faction.", null);
        String problem = turnInProblem(game, representative, contract);
        if (problem != null) return new TurnInResult(false, "Contract turn-in blocked: " + problem + ".", contract);

        int itemIndex = matchingItemIndex(game, contract);
        if (itemIndex < 0) return new TurnInResult(false,
                "Contract turn-in blocked: required proof is not carried.", contract);
        String deliveredItem = game.inventory.remove(itemIndex);
        game.takeProvenanceForItem(deliveredItem);
        game.rebuildItemContainersFromLegacyLists();
        FactionMarketContractAuthority.CompletionResult marketResult =
                FactionMarketContractAuthority.complete(game, contract, deliveredItem);
        ConstructionBlueprintContractRewardAuthority.RewardResult blueprintResult =
                ConstructionBlueprintContractRewardAuthority.apply(game, contract, deliveredItem);
        VehicleContractResult vehicleResult =
                applyVehicleContract(game, contract, deliveredItem);
        contract.completed = true;
        game.carriedScript = Math.max(0, game.carriedScript + Math.max(0, contract.payout));
        Faction faction = contract.faction == null ? Faction.NONE : contract.faction;
        int standing = game.factionStanding.getOrDefault(faction, 0);
        game.factionStanding.put(faction, standing + Math.max(0, contract.repReward));
        if (contract.skillXpReward > 0) {
            game.gainXp("Mechanics", contract.skillXpReward, "completed " + contract.displayType());
        }
        String message = "Contract completed for " + contract.displayFactionName() + ": paid "
                + contract.payout + " script and awarded standing +" + contract.repReward
                + (contract.skillXpReward > 0 ? " and skill XP +" + contract.skillXpReward : "") + "."
                + (marketResult.summary().isBlank() ? "" : " " + marketResult.summary())
                + (blueprintResult.summary().isBlank() ? "" : " " + blueprintResult.summary())
                + (vehicleResult.summary().isBlank() ? "" : " " + vehicleResult.summary());
        return new TurnInResult(true, message, contract);
    }

    static String turnInProblem(GamePanel game, NpcEntity representative, FactionContract contract) {
        if (game == null || contract == null || contract.completed) return "no active contract is selected";
        if (representative == null || !representative.isFactionRepresentative()) {
            return "speak to a faction representative";
        }
        Faction representativeFaction = representative.faction == null ? Faction.NONE : representative.faction;
        Faction contractFaction = contract.faction == null ? Faction.NONE : contract.faction;
        if (!sameContractFaction(game, representativeFaction, contractFaction)) {
            return "this representative serves another faction";
        }
        String deliveryProblem = deliveryProblem(game, contract);
        if (deliveryProblem != null) return deliveryProblem;
        String vehicleProblem = vehicleContractProblem(game, contract);
        if (vehicleProblem != null) return vehicleProblem;
        String proof = ContractObjectiveReadabilityAuthority.proofProblem(contract,
                game.unlockedSkillNodes, game.unlockedKnowledges);
        return proof.isBlank() ? null : proof;
    }

    private static FactionContract bestActiveContract(GamePanel game, NpcEntity representative) {
        if (game == null || representative == null || !representative.isFactionRepresentative()) return null;
        Faction representativeFaction = representative.faction == null ? Faction.NONE : representative.faction;
        FactionContract first = null;
        FactionContract carried = null;
        for (FactionContract contract : game.factionContracts) {
            if (contract == null || contract.completed) continue;
            Faction contractFaction = contract.faction == null ? Faction.NONE : contract.faction;
            if (!sameContractFaction(game, representativeFaction, contractFaction)) continue;
            if (first == null) first = contract;
            if (hasNamedItem(game.inventory, contract.requiredTurnInItem) && carried == null) {
                carried = contract;
            }
            if (turnInProblem(game, representative, contract) == null) return contract;
        }
        return carried == null ? first : carried;
    }

    private static int matchingItemIndex(GamePanel game, FactionContract contract) {
        if (game == null || contract == null || contract.requiredTurnInItem == null
                || contract.requiredTurnInItem.isBlank()) return -1;
        for (int i = 0; i < game.inventory.size(); i++) {
            String item = game.inventory.get(i);
            if (!ItemQuality.namesMatch(item, contract.requiredTurnInItem)) continue;
            if (!contract.requiresProductionProof() || qualifiesProductionItem(game, contract, item)) return i;
        }
        return -1;
    }

    private static String deliveryProblem(GamePanel game, FactionContract contract) {
        if (!hasNamedItem(game.inventory, contract.requiredTurnInItem)) {
            return "carry " + contract.publicRequiredItem() + " before turn-in";
        }
        if (!contract.requiresProductionProof()) return null;

        boolean qualityReady = false;
        boolean productionReady = false;
        for (String item : game.inventory) {
            if (!ItemQuality.namesMatch(item, contract.requiredTurnInItem)) continue;
            if (ItemQuality.tierIndex(item) < contract.minimumQualityTier) continue;
            qualityReady = true;
            ItemProvenanceRecord provenance = game.peekProvenanceForItem(item);
            if (!hasProductionRecord(provenance, contract.minimumQualityTier)) continue;
            productionReady = true;
            if (passedInspection(provenance)) return null;
        }
        if (!qualityReady) return "delivery requires " + contract.minimumQualityName() + " quality or better";
        if (!productionReady) return "delivery requires a recorded produced item at the required quality";
        return "delivery requires a batch that passed inspection";
    }

    private static boolean qualifiesProductionItem(GamePanel game, FactionContract contract, String item) {
        if (ItemQuality.tierIndex(item) < contract.minimumQualityTier) return false;
        ItemProvenanceRecord provenance = game.peekProvenanceForItem(item);
        return hasProductionRecord(provenance, contract.minimumQualityTier) && passedInspection(provenance);
    }

    private static boolean hasProductionRecord(ItemProvenanceRecord provenance, int minimumQualityTier) {
        if (provenance == null || provenance.productionMode == null || provenance.productionMode.isBlank()
                || provenance.outputQuality == null || provenance.outputQuality.isBlank()) return false;
        return ItemQuality.tierIndex(provenance.outputQuality + " item") >= minimumQualityTier;
    }

    private static boolean passedInspection(ItemProvenanceRecord provenance) {
        return provenance != null && "passed inspection".equalsIgnoreCase(provenance.defectState);
    }

    private static String vehicleContractProblem(GamePanel game, FactionContract contract) {
        VehicleContractMode mode = vehicleContractMode(contractText(contract, ""));
        if (mode == null) return null;
        Faction faction = contract.faction == null ? Faction.NONE : contract.faction;
        if (faction == Faction.NONE) {
            return "vehicle work requires an identified issuing faction";
        }
        if (selectVehicleContractTarget(game, contract, mode) == null) {
            return "no eligible local " + mode.targetDescription
                    + " remains linked to this contract";
        }
        return null;
    }

    private static String vehicleContractPreview(GamePanel game, FactionContract contract) {
        VehicleContractMode mode = vehicleContractMode(contractText(contract, ""));
        if (mode == null) return "";
        MapObjectState vehicle = selectVehicleContractTarget(game, contract, mode);
        if (vehicle == null) {
            return "Vehicle outcome: " + mode.action
                    + " is pending because no eligible local target is available.";
        }
        return "Vehicle outcome: completion will " + mode.action + " "
                + vehicleDisplayName(game, vehicle) + " through its persistent vehicle record.";
    }

    private static VehicleContractResult applyVehicleContract(
            GamePanel game, FactionContract contract, String deliveredItem) {
        String text = contractText(contract, deliveredItem);
        VehicleContractMode mode = vehicleContractMode(text);
        if (mode == null) return VehicleContractResult.none();
        MapObjectState vehicle = selectVehicleContractTarget(game, contract, mode);
        if (vehicle == null) {
            return new VehicleContractResult(true, false,
                    "Vehicle contract outcome was not applied because the eligible local target disappeared before completion.");
        }
        Faction faction = contract.faction == null ? Faction.NONE : contract.faction;
        if (mode == VehicleContractMode.REFUEL) {
            VehicleFuelAuthority.Result fuel = VehicleFuelAuthority.refuelForFaction(
                    game.world, vehicle, 16, game.turn, faction,
                    "contract " + safe(contract.id));
            if (!fuel.success()) {
                return new VehicleContractResult(true, false,
                        "Vehicle contract outcome was blocked: " + fuel.message());
            }
            return new VehicleContractResult(true, fuel.changed(),
                    "Vehicle contract outcome: " + fuel.message());
        }

        VehicleRuntimeAuthority.Result result;
        boolean recoveredLoss = false;
        if (mode == VehicleContractMode.REPAIR) {
            result = VehicleRuntimeAuthority.repairForFaction(vehicle, 35,
                    game.turn, faction, "contract " + safe(contract.id));
            if (result.success()) {
                recoveredLoss = VehicleMaintenanceAuthority.resolveLossRecovery(
                        vehicle, game.turn, "faction contract " + safe(contract.id));
            }
        } else {
            result = VehicleRuntimeAuthority.salvageForFaction(vehicle, faction,
                    game.turn, "contract " + safe(contract.id));
        }
        if (!result.success()) {
            return new VehicleContractResult(true, false,
                    "Vehicle contract outcome was blocked: " + result.message());
        }
        return new VehicleContractResult(true, result.changed() || recoveredLoss,
                "Vehicle contract outcome: " + result.message()
                        + (recoveredLoss
                        ? " Critical systems returned to service and active loss hazards were cleared."
                        : ""));
    }

    private static MapObjectState selectVehicleContractTarget(
            GamePanel game, FactionContract contract, VehicleContractMode mode) {
        if (game == null || game.world == null || game.world.mapObjects == null
                || contract == null || mode == null) return null;
        Faction faction = contract.faction == null ? Faction.NONE : contract.faction;
        String explicitTargetId = explicitVehicleTargetId(contract, mode);
        String text = contractText(contract, "");
        ArrayList<MapObjectState> candidates = new ArrayList<>();
        for (MapObjectState object : game.world.mapObjects) {
            if (!VehicleRuntimeAuthority.isVehicle(object)) continue;
            VehicleRuntimeAuthority.ensureInitialized(game.world, object);
            VehicleRuntimeAuthority.Snapshot snapshot =
                    VehicleRuntimeAuthority.inspect(game.world, object);
            if (!eligibleVehicleContractTarget(game, object, snapshot, faction, mode)) continue;
            if (!explicitTargetId.isBlank()) {
                if (explicitTargetId.equals(safe(object.id))) return object;
                continue;
            }
            candidates.add(object);
        }
        if (!explicitTargetId.isBlank()) return null;
        candidates.sort(Comparator
                .comparingInt((MapObjectState object) -> vehicleContractScore(
                        game, object, text, mode)).reversed()
                .thenComparing(object -> vehicleDisplayName(game, object))
                .thenComparing(object -> safe(object.id)));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static boolean eligibleVehicleContractTarget(
            GamePanel game, MapObjectState object,
            VehicleRuntimeAuthority.Snapshot snapshot,
            Faction faction, VehicleContractMode mode) {
        if (object == null || snapshot == null || faction == null
                || faction == Faction.NONE
                || "salvaged".equals(snapshot.condition())
                || !VehicleRuntimeAuthority.factionOwns(object, faction)) return false;
        if (mode == VehicleContractMode.REPAIR) {
            return VehicleRuntimeAuthority.damaged(object);
        }
        if (mode == VehicleContractMode.REFUEL) {
            VehicleFuelAuthority.Snapshot fuel = VehicleFuelAuthority.inspect(
                    game == null ? null : game.world, object);
            return fuel.capacity() > 0 && fuel.current() < fuel.capacity();
        }
        return VehicleRuntimeAuthority.seized(object)
                || "wreck".equals(snapshot.condition());
    }

    private static String explicitVehicleTargetId(
            FactionContract contract, VehicleContractMode mode) {
        if (contract == null || mode == null) return "";
        String target = safe(contract.targetEntityId);
        String prefix = switch (mode) {
            case REPAIR -> VEHICLE_REPAIR_TARGET;
            case SALVAGE -> VEHICLE_SALVAGE_TARGET;
            case REFUEL -> VEHICLE_FUEL_TARGET;
        };
        return target.startsWith(prefix) ? target.substring(prefix.length()) : "";
    }

    private static int vehicleContractScore(GamePanel game, MapObjectState vehicle,
                                            String text, VehicleContractMode mode) {
        VehicleRuntimeAuthority.Snapshot snapshot = VehicleRuntimeAuthority.inspect(
                game == null ? null : game.world, vehicle);
        if (snapshot == null) return Integer.MIN_VALUE;
        String low = safe(text).toLowerCase(Locale.ROOT);
        int score = 0;
        score += textMatchScore(low, snapshot.manufacturer(), 90);
        score += textMatchScore(low, snapshot.model(), 120);
        score += textMatchScore(low, snapshot.variant(), 30);
        score += textMatchScore(low, snapshot.vehicleClass().label, 75);
        score += textMatchScore(low, snapshot.vehicleClass().role, 45);
        score += textMatchScore(low, snapshot.ownerName(), 25);
        String id = safe(vehicle.id).toLowerCase(Locale.ROOT);
        if (!id.isBlank() && low.contains(id)) score += 200;
        if (mode == VehicleContractMode.REFUEL) {
            VehicleFuelAuthority.Snapshot fuel = VehicleFuelAuthority.inspect(
                    game == null ? null : game.world, vehicle);
            score += Math.max(0, fuel.capacity() - fuel.current()) * 4;
        } else if (VehicleRuntimeAuthority.damaged(vehicle)) {
            score += Math.max(0, 100 - snapshot.integrity());
        }
        return score;
    }

    private static int textMatchScore(String text, String value, int exactWeight) {
        String candidate = safe(value).toLowerCase(Locale.ROOT);
        if (candidate.isBlank()) return 0;
        int score = text.contains(candidate) ? exactWeight : 0;
        for (String token : candidate.split("[^a-z0-9]+")) {
            if (token.length() >= 4 && text.contains(token)) score += 8;
        }
        return score;
    }

    private static VehicleContractMode vehicleContractMode(String text) {
        String low = safe(text).toLowerCase(Locale.ROOT);
        boolean vehicle = contains(low, "vehicle", "motor pool", "motor-pool",
                "convoy", "cargo truck", "armored car", "civilian car",
                "utility bike", "tank", "crawler", "hauler");
        if (!vehicle) return null;
        if (low.contains(VEHICLE_FUEL_TARGET.toLowerCase(Locale.ROOT))
                || contains(low, "refuel", "fuel support", "fuel reserve",
                "power support", "power reserve", "energy support")) {
            return VehicleContractMode.REFUEL;
        }
        if (contains(low, "salvage", "strip", "wreck", "route clearing",
                "clear the route", "scrap")) {
            return VehicleContractMode.SALVAGE;
        }
        if (contains(low, "repair", "service", "restore", "overhaul",
                "machine part", "engine", "powerplant", "transmission",
                "wheel", "track", "headlight", "sensor", "armor patch")) {
            return VehicleContractMode.REPAIR;
        }
        return null;
    }

    private static String contractText(FactionContract contract,
                                       String deliveredItem) {
        if (contract == null) return safe(deliveredItem);
        return (safe(contract.type) + " " + safe(contract.targetName) + " "
                + safe(contract.targetEntityId) + " "
                + safe(contract.requiredTurnInItem) + " "
                + safe(contract.description) + " " + safe(deliveredItem))
                .toLowerCase(Locale.ROOT);
    }

    private static String vehicleDisplayName(GamePanel game,
                                             MapObjectState vehicle) {
        VehicleRuntimeAuthority.Snapshot snapshot = VehicleRuntimeAuthority.inspect(
                game == null ? null : game.world, vehicle);
        if (snapshot == null) return "the selected vehicle";
        return (clean(snapshot.manufacturer(), "") + " "
                + clean(snapshot.model(), snapshot.vehicleClass().label)).trim();
    }

    private static String appendPreview(String first, String second) {
        StringBuilder out = new StringBuilder();
        if (first != null && !first.isBlank()) out.append(' ').append(first);
        if (second != null && !second.isBlank()) out.append(' ').append(second);
        return out.toString();
    }

    private static boolean contains(String text, String... terms) {
        String low = safe(text).toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (term != null && !term.isBlank()
                    && low.contains(term.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static String clean(String value, String fallback) {
        String text = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return text.isBlank() ? fallback : text;
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/');
    }

    private static boolean hasNamedItem(List<String> items, String wanted) {
        if (items == null || wanted == null || wanted.isBlank()) return false;
        for (String item : items) if (ItemQuality.namesMatch(item, wanted)) return true;
        return false;
    }

    static boolean sameContractFaction(GamePanel game, Faction a, Faction b) {
        if (game != null && game.sameFactionFamily(a, b)) return true;
        return (a == Faction.ARBITES || a == Faction.CIVIC_WARDENS)
                && (b == Faction.ARBITES || b == Faction.CIVIC_WARDENS);
    }
}
