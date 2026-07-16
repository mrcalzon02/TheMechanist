package mechanist;

import java.util.List;

/** Owns atomic faction-contract proof validation, completion, and reward payout. */
final class ContractTurnInAuthority {
    record TurnInResult(boolean success, String message, FactionContract contract) { }

    private ContractTurnInAuthority() { }

    static String representativeLine(GamePanel game, NpcEntity representative) {
        FactionContract contract = bestActiveContract(game, representative);
        if (contract == null) return "Contract turn-in: no active contract belongs to this representative's faction.";
        String problem = turnInProblem(game, representative, contract);
        return problem == null
                ? "Contract turn-in ready: " + contract.displayType() + " for " + contract.payout
                        + " script and standing +" + contract.repReward + "."
                : "Contract turn-in blocked: " + problem + ".";
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
                + (marketResult.summary().isBlank() ? "" : " " + marketResult.summary());
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
