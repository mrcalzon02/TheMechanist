package mechanist;

/** Smoke for claimed-room facility quality and central production capping. */
final class Milestone03ProductionFacilityQualitySmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.baseObjects.clear();
        game.baseClaimed = true;
        game.claimedRoomId = 1;
        game.baseX = 10;
        game.baseY = 10;

        BaseObject machine = station("Masterwork Forge", 'f', 10, 10, "Masterwork", 5);
        game.baseObjects.add(machine);
        ProductionFacilityQualityAuthority.FacilityQuality basic = ProductionFacilityQualityAuthority.evaluate(game, machine);
        require(basic.active() && basic.tier() == 2, "one serviceable station should make a Common facility");

        game.baseObjects.add(station("Bench", 'w', 11, 10, "Common", 5));
        require(ProductionFacilityQualityAuthority.evaluate(game, machine).tier() == 3,
                "two serviceable stations should make a Serviceable facility");
        game.baseObjects.add(station("Lab", 'l', 12, 10, "Common", 5));
        game.baseObjects.add(station("Condenser", 'e', 14, 10, "Common", 0));
        ProductionFacilityQualityAuthority.FacilityQuality developed = ProductionFacilityQualityAuthority.evaluate(game, machine);
        require(developed.tier() == 3, "broken stations must not raise facility quality");
        game.baseObjects.get(3).integrity = 5;
        require(ProductionFacilityQualityAuthority.evaluate(game, machine).tier() == 4,
                "four serviceable stations should make a Fine facility");

        game.baseClaimed = false;
        require(!ProductionFacilityQualityAuthority.evaluate(game, machine).active(),
                "an unclaimed work area should leave facility quality open");
        if (game.timer != null) game.timer.stop();
    }

    private static BaseObject station(String name, char symbol, int x, int y, String quality, int integrity) {
        BaseObject object = new BaseObject(name, symbol, x, y, 0, 0);
        object.qualityName = quality;
        object.integrity = integrity;
        return object;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone03ProductionFacilityQualitySmoke() { }
}
