package mechanist;

import java.util.Objects;

/** Issues one live faction production order and exposes its readable work state. */
final class ProductionContractAuthority {
    record WorkResult(boolean success, String message, FactionContract contract) { }

    private ProductionContractAuthority() { }

    static String representativeLine(GamePanel game, NpcEntity representative) {
        if (game == null || representative == null || !representative.isFactionRepresentative()) {
            return "Production work: speak to a faction representative to request an order.";
        }
        FactionContract active = activeForRepresentative(game, representative);
        if (active != null) {
            return active.requiresProductionProof()
                    ? "Production work active: deliver " + active.minimumQualityName() + " "
                            + active.publicRequiredItem() + " or better with a passed production record."
                    : "Faction work active: complete the current " + active.displayType() + " before requesting another order.";
        }
        int minimumTier = minimumTier(game, representative);
        return "Production work available: one " + ItemQuality.NAMES[minimumTier]
                + " Machine part or better, with recorded production and a passed batch inspection.";
    }

    static WorkResult accept(GamePanel game, NpcEntity representative) {
        if (game == null || representative == null || !representative.isFactionRepresentative()) {
            return new WorkResult(false, "Production work unavailable: speak to a faction representative.", null);
        }
        FactionContract active = activeForRepresentative(game, representative);
        if (active != null) {
            return new WorkResult(false,
                    "Production work unavailable: this faction already has an active " + active.displayType() + ".", active);
        }
        FactionContract contract = create(game, representative);
        game.factionContracts.add(contract);
        return new WorkResult(true,
                "Accepted " + contract.displayFactionName() + " production order: deliver "
                        + contract.minimumQualityName() + " " + contract.publicRequiredItem()
                        + " or better for " + contract.payout + " script, standing +" + contract.repReward
                        + ", and skill XP +" + contract.skillXpReward + ".",
                contract);
    }

    private static FactionContract activeForRepresentative(GamePanel game, NpcEntity representative) {
        Faction representativeFaction = representative.faction == null ? Faction.NONE : representative.faction;
        for (FactionContract contract : game.factionContracts) {
            if (contract == null || contract.completed) continue;
            Faction contractFaction = contract.faction == null ? Faction.NONE : contract.faction;
            if (ContractTurnInAuthority.sameContractFaction(game, representativeFaction, contractFaction)) return contract;
        }
        return null;
    }

    private static FactionContract create(GamePanel game, NpcEntity representative) {
        Faction faction = representative.faction == null ? Faction.NONE : representative.faction;
        int standing = game.factionStanding.getOrDefault(faction, 0);
        int tier = minimumTier(game, representative);
        FactionContract contract = new FactionContract();
        contract.id = "P-" + Integer.toUnsignedString(Objects.hash(faction, representative.name,
                game.turn, game.factionContracts.size()), 36).toUpperCase();
        contract.type = "PRODUCTION";
        contract.faction = faction;
        contract.targetZoneKey = currentZoneKey(game);
        contract.targetEntityId = "";
        contract.targetName = ItemQuality.NAMES[tier] + " faction-standard machine part";
        contract.requiredTurnInItem = "Machine part";
        contract.minimumQualityTier = tier;
        contract.skillXpReward = 4 + Math.max(0, tier - 3) * 2;
        contract.payout = 150 + Math.max(0, tier - 2) * 35 + Math.max(0, standing) * 5;
        contract.repReward = 2 + Math.max(0, standing / 10);
        contract.spawned = true;
        contract.description = "Produce one Machine part at " + ItemQuality.NAMES[tier]
                + " quality or better. It must have a recorded production origin and a batch that passed inspection,"
                + " then return it to a matching faction representative.";
        return contract;
    }

    private static int minimumTier(GamePanel game, NpcEntity representative) {
        Faction faction = representative == null || representative.faction == null ? Faction.NONE : representative.faction;
        int standing = game == null ? 0 : game.factionStanding.getOrDefault(faction, 0);
        return standing >= 8 ? 4 : 3;
    }

    private static String currentZoneKey(GamePanel game) {
        if (game == null || game.atlas == null) return "";
        WorldAtlas atlas = game.atlas;
        return atlas.sectorX + "," + atlas.sectorY + "," + atlas.zoneX + "," + atlas.zoneY + ","
                + atlas.floor + "," + atlas.sewer;
    }
}
