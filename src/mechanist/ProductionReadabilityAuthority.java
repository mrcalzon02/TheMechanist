package mechanist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ProductionReadabilityAuthority {
    private ProductionReadabilityAuthority() {}

    static List<String> detailLines(GamePanel game, CraftingRecipe recipe) {
        if (recipe == null) return List.of("No crafting recipe selected.");
        BaseObject machine = game == null ? null : recipe.requiredMachine(game);
        LinkedHashMap<String, Integer> availableItems = new LinkedHashMap<>();
        for (String item : recipe.itemInputs.keySet()) {
            availableItems.put(item, game == null ? 0 : game.countCraftInput(item));
        }
        String blocker = game == null ? "live crafting state is unavailable." : recipe.blockingProblemForMachine(game, machine);
        ProductionMaterialQualityAuthority.MaterialQuality materials = ProductionMaterialQualityAuthority.evaluate(game, recipe);
        ProductionKnowledgeSourceAuthority.KnowledgeSource knowledge = ProductionKnowledgeSourceAuthority.evaluate(
                game, machine, recipe.requiredKnowledge);
        int materialTier = materials.active() && materials.complete() ? materials.limitingTier() : -1;
        ProductionFacilityQualityAuthority.FacilityQuality facility = ProductionFacilityQualityAuthority.evaluate(game, machine);
        int facilityTier = facility.active() ? facility.tier() : -1;
        ProductionToolQualityAuthority.ToolQuality tool = ProductionToolQualityAuthority.evaluate(game);
        int toolTier = tool.active() ? tool.tier() : -1;
        ProductionOperatorSkillAuthority.OperatorSkill operator = ProductionOperatorSkillAuthority.evaluate(game, recipe.xpSkill);
        String quality = ProductionQualityTraceAuthority.evaluate(
                knowledge.effectiveKnowledge(),
                recipe.requiredKnowledge, machine == null ? "Common" : machine.qualityName, materialTier, facilityTier, toolTier,
                operator.qualityTier()).outputQuality();
        int turns = ControlledProductionJobAuthority.manualTurnCost(game, machine, recipe);
        int fatigue = ControlledProductionJobAuthority.manualFatigueCost(game, machine, recipe);
        ProductionFatiguePressureAuthority.FatiguePressure pressure = ProductionFatiguePressureAuthority.evaluate(game, fatigue);
        ArrayList<String> lines = new ArrayList<>(preview(recipe, machine, game == null ? 0 : game.supplies,
                game == null ? 0 : game.machineParts, availableItems, quality, turns, fatigue, blocker, operator));
        int pending = game == null || game.machineOperationQueue == null ? 0 : game.machineOperationQueue.pendingCount();
        int active = game == null || game.machineOperationQueue == null ? 0 : game.machineOperationQueue.activeCount();
        int history = game == null || game.machineOperationQueue == null ? 0 : game.machineOperationQueue.historyCount();
        lines.addAll(ProductionQualityTraceAuthority.evaluate(
                knowledge.effectiveKnowledge(),
                recipe.requiredKnowledge, machine == null ? "Common" : machine.qualityName, materialTier, facilityTier, toolTier,
                operator.qualityTier()).lines());
        lines.addAll(materials.lines());
        lines.addAll(facility.lines());
        lines.addAll(tool.lines());
        lines.addAll(pressure.lines());
        lines.addAll(ProductionLocationAuthority.evaluate(game, machine).lines());
        lines.addAll(ProductionMachineIdentityAuthority.evaluate(machine).lines());
        lines.addAll(ProductionOperatorIdentityAuthority.evaluate(game).lines());
        lines.addAll(knowledge.lines());
        lines.addAll(skillCapabilityContext(game));
        lines.addAll(ProductionWorkerQualityAuthority.evaluate(game, machine, true).lines());
        lines.addAll(machineContext(machine, pending, active, history));
        return lines;
    }

    static List<String> preview(CraftingRecipe recipe, BaseObject machine, int supplies, int machineParts,
                                Map<String, Integer> availableItems, String quality, int turns,
                                int fatigueCost, String blocker) {
        return preview(recipe, machine, supplies, machineParts, availableItems, quality, turns, fatigueCost, blocker,
                ProductionOperatorSkillAuthority.evaluate(null, recipe == null ? null : recipe.xpSkill));
    }

    static List<String> preview(CraftingRecipe recipe, BaseObject machine, int supplies, int machineParts,
                                Map<String, Integer> availableItems, String quality, int turns,
                                int fatigueCost, String blocker, ProductionOperatorSkillAuthority.OperatorSkill operator) {
        ArrayList<String> lines = new ArrayList<>();
        if (recipe == null) {
            lines.add("No crafting recipe selected.");
            return lines;
        }
        String resolvedQuality = quality == null || quality.isBlank() ? "Common" : quality;
        ProductionRecipe production = ProductionRecipe.create(recipe.outputBaseItem, recipe.faction,
                resolvedQuality, recipe.requiredKnowledge, recipe.machineName());

        lines.add(blocker == null ? "Status: READY for immediate manual crafting."
                : "Status: BLOCKED - " + blocker);
        lines.add("Output: " + Math.max(1, recipe.outputCount) + "x " + production.outputItemName() + ".");
        lines.add("Destination: carried inventory; this Craft action does not create a queued machine job.");
        lines.add(machine == null
                ? "Machine: " + recipe.machineName() + " is not available."
                : "Machine: " + machine.name + " / " + safe(machine.qualityName, "Common")
                        + " / integrity " + machine.integrity + " -> " + Math.max(0, machine.integrity - Math.max(0, recipe.machineWear)) + ".");
        lines.add("Operation: player-operated, " + Math.max(0, turns) + " turn(s), fatigue +"
                + Math.max(0, fatigueCost) + ", machine wear " + Math.max(0, recipe.machineWear) + ".");
        lines.add("Reward: " + Math.max(0, recipe.xpGain) + " " + safe(recipe.xpSkill, "Mechanics") + " XP.");
        lines.add("Supplies: " + Math.max(0, supplies) + "/" + recipe.effectiveSuppliesCost()
                + "; machine parts: " + Math.max(0, machineParts) + "/" + recipe.effectiveMachinePartsCost() + ".");
        for (Map.Entry<String, Integer> input : recipe.itemInputs.entrySet()) {
            int have = availableItems == null ? 0 : Math.max(0, availableItems.getOrDefault(input.getKey(), 0));
            lines.add("Input: " + input.getKey() + " " + have + "/" + Math.max(0, input.getValue()) + ".");
        }
        lines.add("Knowledge: " + safe(recipe.requiredKnowledge, "none") + ". Faction pattern: "
                + FactionManufacturingProfile.forFaction(recipe.faction).label + ".");
        lines.addAll(ProductionFactionMutationAuthority.evaluate(production).lines());
        lines.addAll(MachineConditionProductionAuthority.forecastLines(machine));
        if (operator != null) lines.addAll(operator.lines());
        lines.add("Outcome estimate: value about " + production.estimatedValue() + " script; usable charges about "
                + production.outputCharges() + "; defect risk about " + production.estimatedDefectPercent(machine,
                operator == null ? 0 : operator.defectRiskAdjust()) + "% before live fatigue pressure.");
        lines.add("Batch rule: manual Craft rolls one inspection disposition for the whole output batch and records it in item provenance.");
        lines.add("Defect consequence: a flagged batch receives a 40% ordinary-trader resale penalty; item statistics remain unchanged.");
        lines.add("Purpose: " + safe(recipe.description, "No description recorded."));
        return lines;
    }

    static List<String> machineContext(BaseObject machine, int pendingOperations, int activeOperations, int historyRecords) {
        ArrayList<String> lines = new ArrayList<>();
        if (machine == null) {
            lines.add("Machine staffing: unavailable until the required machine is built.");
            lines.add("Separate machine queue: no selected machine; shared operations pending " + Math.max(0, pendingOperations)
                    + ", active " + Math.max(0, activeOperations) + ", recorded " + Math.max(0, historyRecords) + ".");
            return lines;
        }
        String worker = safe(machine.assignedWorker, "unassigned");
        String assignedRecipe = safe(machine.assignedRecipe, "none");
        lines.add("Machine staffing: " + worker + "; manual Craft remains player-operated and does not require an assigned worker.");
        lines.add("Machine queue record: assigned recipe " + assignedRecipe + "; remaining "
                + Math.max(0, machine.productionQueueRemaining) + "/" + Math.max(1, machine.productionQueueTarget) + ".");
        lines.add("Shared operation queue: pending " + Math.max(0, pendingOperations) + ", active "
                + Math.max(0, activeOperations) + ", recorded " + Math.max(0, historyRecords) + ".");
        lines.add("Utility boundary: manual Craft currently checks machine presence, knowledge, supplies, parts, and named inputs; no separate power or fuel gate is enforced here.");
        lines.add("Routing boundary: manual output enters carried inventory; queued-machine output routing is not controlled by this Craft action.");
        return lines;
    }

    static List<String> skillCapabilityContext(GamePanel game) {
        ArrayList<String> lines = new ArrayList<>();
        if (game == null || game.unlockedSkillNodes == null || game.unlockedSkillNodes.isEmpty()) {
            lines.add("Skill capability hooks: no unlocked skill nodes are contributing production preview context.");
            return lines;
        }
        List<String> passive = SkillTreeProgressionAuthority.passiveBonusLines(game.unlockedSkillNodes);
        List<String> active = SkillTreeProgressionAuthority.activeAbilityLines(game.unlockedSkillNodes);
        if (passive.isEmpty() && active.isEmpty()) {
            lines.add("Skill capability hooks: unlocked nodes have no production preview hook yet.");
            return lines;
        }
        lines.add("Skill capability hooks: preview context only; production execution math is unchanged until a consuming authority applies the hook.");
        for (String line : passive) lines.add("Skill passive hook: " + line + ".");
        for (String line : active) lines.add("Skill active ability: " + line + ".");
        return lines;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
