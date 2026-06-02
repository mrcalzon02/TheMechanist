package mechanist;

/** Economy initialization ownership shell for staged world-generation extraction. */
final class ZoneEconomyInitializationManager {
    private ZoneEconomyInitializationManager() {}

    static WorldEconomyInitializationAuthority.Result initialize(World world) {
        if (world == null) return new WorldEconomyInitializationAuthority.Result(0, 0, 0, 0, 0, "no world supplied");
        return WorldEconomyInitializationAuthority.apply(world, world.r);
    }
}
