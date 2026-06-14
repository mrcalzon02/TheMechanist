package mechanist;

import java.util.Map;

/** Verifies every world-generation option resolves to a stable modifier and registered consumer hook. */
final class WorldGenerationOptionModifierSmoke {
    public static void main(String[] args) {
        WorldSetupSettings low = WorldSetupSettings.standard();
        low.npcDensity = 0;
        low.zoneSize = 0;
        low.zoneDensity = 0;
        low.priceDifficulty = 0;
        low.craftDifficulty = 0;
        low.simulationAge = 0;

        WorldSetupSettings high = WorldSetupSettings.standard();
        high.npcDensity = 3;
        high.zoneSize = 3;
        high.zoneDensity = 3;
        high.priceDifficulty = 3;
        high.craftDifficulty = 3;
        high.simulationAge = 3;
        high.hoarderMode = true;

        WorldGenerationSettingsAuthority.Modifiers lo = WorldGenerationSettingsAuthority.resolve(low);
        WorldGenerationSettingsAuthority.Modifiers hi = WorldGenerationSettingsAuthority.resolve(high);
        require(hi.npcDensityMultiplier() > lo.npcDensityMultiplier(), "NPC density modifier ordering");
        require(hi.minWorldgenWeight() > lo.minWorldgenWeight(), "zone size weight ordering");
        require(hi.scaleProfile().minWidth > lo.scaleProfile().minWidth, "zone size dimension ordering");
        require(hi.zoneDensityMultiplier() > lo.zoneDensityMultiplier(), "zone density modifier ordering");
        require(hi.priceMultiplier() > lo.priceMultiplier(), "price modifier ordering");
        require(hi.craftMultiplier() > lo.craftMultiplier(), "craft modifier ordering");
        require(hi.simulationBatches() > lo.simulationBatches(), "simulation age modifier ordering");
        require(hi.unlimitedCarry(), "hoarder mode modifier");
        require(WorldGenerationSettingsAuthority.adjustedBuyPrice(100, 0, 0, hi)
                        > WorldGenerationSettingsAuthority.adjustedBuyPrice(100, 0, 0, lo),
                "price difficulty calculation");
        require(WorldGenerationSettingsAuthority.adjustedSellPrice(100, 0, hi)
                        < WorldGenerationSettingsAuthority.adjustedSellPrice(100, 0, lo),
                "sell price difficulty calculation");
        require(WorldGenerationSettingsAuthority.adjustedCraftCost(10, hi)
                        > WorldGenerationSettingsAuthority.adjustedCraftCost(10, lo),
                "craft difficulty calculation");
        require(WorldGenerationSettingsAuthority.playerCarryCapacity(0, hi) > 100_000_000, "hoarder carry calculation");
        require(WorldGenerationSettingsAuthority.playerCarryCapacity(10, lo) == 42, "bounded carry calculation");

        Map<String, java.util.List<String>> pointers = WorldGenerationSettingsAuthority.consumerPointers();
        for (String id : java.util.List.of(
                WorldGenerationSettingsAuthority.NPC_DENSITY,
                WorldGenerationSettingsAuthority.ZONE_SIZE,
                WorldGenerationSettingsAuthority.ZONE_DENSITY,
                WorldGenerationSettingsAuthority.PRICE_DIFFICULTY,
                WorldGenerationSettingsAuthority.CRAFT_DIFFICULTY,
                WorldGenerationSettingsAuthority.HOARDER_MODE,
                WorldGenerationSettingsAuthority.SIMULATION_AGE)) {
            require(!Double.isNaN(hi.numeric(id)), "numeric lookup for " + id);
            require(pointers.containsKey(id) && !pointers.get(id).isEmpty(), "consumer pointers for " + id);
        }

        World world = new World(91L, 20, 20);
        world.configureGenerationSettings(high);
        require(WorldGenerationSettingsAuthority.forWorld(world).encodedSetup().equals(high.encode()), "world-owned modifier profile");
        require(WorldGenerationSettingsAuthority.forWorld(world).zoneSizeIndex() == 3, "world modifier lookup");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private WorldGenerationOptionModifierSmoke() { }
}
