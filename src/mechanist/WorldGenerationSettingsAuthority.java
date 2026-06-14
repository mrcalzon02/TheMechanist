package mechanist;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Stable lookup surface for world-setup selections and their resolved runtime modifiers. */
final class WorldGenerationSettingsAuthority {
    static final String VERSION = "world-generation-settings-authority-1";

    static final String NPC_DENSITY = "worldgen.npc_density";
    static final String ZONE_SIZE = "worldgen.zone_size";
    static final String ZONE_DENSITY = "worldgen.zone_density";
    static final String PRICE_DIFFICULTY = "worldgen.price_difficulty";
    static final String CRAFT_DIFFICULTY = "worldgen.craft_difficulty";
    static final String HOARDER_MODE = "worldgen.hoarder_mode";
    static final String SIMULATION_AGE = "worldgen.simulation_age";

    record Modifiers(
            String encodedSetup,
            int npcDensityIndex,
            int zoneSizeIndex,
            int zoneDensityIndex,
            int priceDifficultyIndex,
            int craftDifficultyIndex,
            int simulationAgeIndex,
            boolean unlimitedCarry,
            double npcDensityMultiplier,
            double zoneDensityMultiplier,
            double priceMultiplier,
            double craftMultiplier,
            int simulationBatches,
            int minWorldgenWeight,
            int maxWorldgenWeight,
            WorldGenerationScaleProfile scaleProfile) {

        double numeric(String optionId) {
            return switch (optionId == null ? "" : optionId) {
                case NPC_DENSITY -> npcDensityMultiplier;
                case ZONE_SIZE -> zoneSizeIndex;
                case ZONE_DENSITY -> zoneDensityMultiplier;
                case PRICE_DIFFICULTY -> priceMultiplier;
                case CRAFT_DIFFICULTY -> craftMultiplier;
                case HOARDER_MODE -> unlimitedCarry ? 1.0 : 0.0;
                case SIMULATION_AGE -> simulationBatches;
                default -> Double.NaN;
            };
        }

        String label(String optionId) {
            return switch (optionId == null ? "" : optionId) {
                case NPC_DENSITY -> WorldSetupSettings.NPC_DENSITY[npcDensityIndex];
                case ZONE_SIZE -> WorldSetupSettings.ZONE_SIZE[zoneSizeIndex];
                case ZONE_DENSITY -> WorldSetupSettings.ZONE_DENSITY[zoneDensityIndex];
                case PRICE_DIFFICULTY -> WorldSetupSettings.PRICE[priceDifficultyIndex];
                case CRAFT_DIFFICULTY -> WorldSetupSettings.CRAFT[craftDifficultyIndex];
                case HOARDER_MODE -> unlimitedCarry ? "Unlimited" : "Strength/Endurance limits";
                case SIMULATION_AGE -> WorldSetupSettings.AGE[simulationAgeIndex];
                default -> "unknown";
            };
        }

        String auditLine() {
            return "setup=" + encodedSetup
                    + " npc=" + label(NPC_DENSITY) + " x" + fmt(npcDensityMultiplier)
                    + " size=" + label(ZONE_SIZE) + " weight=" + minWorldgenWeight + "-" + maxWorldgenWeight
                    + " density=" + label(ZONE_DENSITY) + " x" + fmt(zoneDensityMultiplier)
                    + " prices=" + label(PRICE_DIFFICULTY) + " x" + fmt(priceMultiplier)
                    + " craft=" + label(CRAFT_DIFFICULTY) + " x" + fmt(craftMultiplier)
                    + " carry=" + label(HOARDER_MODE)
                    + " age=" + label(SIMULATION_AGE) + " batches=" + simulationBatches
                    + " profile=" + scaleProfile.id;
        }
    }

    static Modifiers resolve(WorldSetupSettings settings) {
        WorldSetupSettings use = settings == null ? WorldSetupSettings.standard() : settings.copy();
        int npc = clamp(use.npcDensity);
        int size = clamp(use.zoneSize);
        int density = clamp(use.zoneDensity);
        int price = clamp(use.priceDifficulty);
        int craft = clamp(use.craftDifficulty);
        int age = clamp(use.simulationAge);
        WorldGenerationScaleProfile profile = use.scaleProfile(WorldGenerationApi.CURRENT_MINIMUM_SCALE);
        return new Modifiers(
                use.encode(), npc, size, density, price, craft, age, use.hoarderMode,
                use.npcDensityMultiplier(), use.zoneDensityMultiplier(), use.priceMultiplier(), use.craftMultiplier(),
                use.simulationBatches(), WorldGenerationApi.minWorldgenWeightForZoneSize(size),
                WorldGenerationApi.maxWorldgenWeightForZoneSize(size), profile);
    }

    static Modifiers forWorld(World world) {
        return world == null ? active() : resolve(world.generationSettings());
    }

    static Modifiers forGame(GamePanel game) {
        if (game != null && game.world != null) return forWorld(game.world);
        return resolve(game == null ? null : game.worldSetup);
    }

    static Modifiers active() {
        return resolve(WorldGenerationApi.settings());
    }

    static Map<String, List<String>> consumerPointers() {
        LinkedHashMap<String, List<String>> pointers = new LinkedHashMap<>();
        pointers.put(NPC_DENSITY, List.of("World.populate", "World.seedNpcPopulation"));
        pointers.put(ZONE_SIZE, List.of("WorldGenerationApi.zoneSliceSize", "World.roadFirstRoomTarget", "RoadGridIntegrationAuthority.applySpinesAt"));
        pointers.put(ZONE_DENSITY, List.of("WorldSetupSettings.scaleProfile", "WorldGenerationApi.targetRoomCount"));
        pointers.put(PRICE_DIFFICULTY, List.of("TraderSession.buyPrice", "TraderSession.sellPrice"));
        pointers.put(CRAFT_DIFFICULTY, List.of("CraftingRecipe.effectiveSuppliesCost", "CraftingRecipe.effectiveMachinePartsCost"));
        pointers.put(HOARDER_MODE, List.of("GamePanel.carryCapacity"));
        pointers.put(SIMULATION_AGE, List.of("WorldAtlas history initialization batches"));
        return pointers;
    }

    static String pointerSummary(String optionId) {
        List<String> pointers = consumerPointers().get(optionId);
        return optionId + " -> " + (pointers == null ? "no registered consumers" : String.join(", ", pointers));
    }

    static int adjustedBuyPrice(int basePrice, int markupPercent, int discountPercent, Modifiers modifiers) {
        Modifiers use = modifiers == null ? active() : modifiers;
        double market = Math.max(0.01, (100 + markupPercent - discountPercent) / 100.0);
        return Math.max(1, (int)Math.ceil(Math.max(1, basePrice) * market * use.priceMultiplier()));
    }

    static int adjustedSellPrice(int basePrice, int markupPercent, Modifiers modifiers) {
        Modifiers use = modifiers == null ? active() : modifiers;
        double market = Math.max(0.01, (100 - markupPercent / 2) / 100.0);
        return Math.max(1, (int)Math.floor(Math.max(1, basePrice) * market / Math.max(0.75, use.priceMultiplier())));
    }

    static int adjustedCraftCost(int baseCost, Modifiers modifiers) {
        Modifiers use = modifiers == null ? active() : modifiers;
        return Math.max(0, (int)Math.ceil(Math.max(0, baseCost) * use.craftMultiplier()));
    }

    static int playerCarryCapacity(int supplies, Modifiers modifiers) {
        Modifiers use = modifiers == null ? active() : modifiers;
        return use.unlimitedCarry() ? Integer.MAX_VALUE / 4 : 40 + Math.max(0, supplies / 5);
    }

    private static int clamp(int value) { return Math.max(0, Math.min(3, value)); }
    private static String fmt(double value) { return String.format(Locale.US, "%.2f", value); }

    private WorldGenerationSettingsAuthority() { }
}
