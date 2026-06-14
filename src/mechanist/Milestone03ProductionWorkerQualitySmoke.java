package mechanist;

/** Smoke for assigned-worker quality and the manual Craft ownership boundary. */
final class Milestone03ProductionWorkerQualitySmoke {
    public static void main(String[] args) {
        require(ProductionWorkerQualityAuthority.tierForSkill(1) == 2, "skill one should support Common");
        require(ProductionWorkerQualityAuthority.tierForSkill(2) == 3, "skill two should support Serviceable");
        require(ProductionWorkerQualityAuthority.tierForSkill(3) == 4, "skill three should support Fine");
        require(ProductionWorkerQualityAuthority.tierForSkill(4) == 5, "skill four should support Masterwork");

        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        RecruitWorker worker = new RecruitWorker("Hest Var", "forge worker", Faction.HIVER, 3, 4);
        game.factionRecruits.add(worker);
        BaseObject machine = new BaseObject("Test Forge", 'f', 0, 0, 0, 0);
        machine.assignedWorker = worker.name;

        ProductionWorkerQualityAuthority.WorkerQuality manual = ProductionWorkerQualityAuthority.evaluate(game, machine, true);
        require(manual.assigned(), "assigned worker should resolve");
        require(!manual.activeForRun(), "assigned worker must not affect manual Craft");
        require("Fine".equals(manual.potentialQuality()), "skill three potential quality");
        require(manual.lines().stream().anyMatch(line -> line.contains("does not cap or improve")), "manual boundary wording");

        ProductionWorkerQualityAuthority.WorkerQuality queued = ProductionWorkerQualityAuthority.evaluate(game, machine, false);
        require(queued.activeForRun(), "worker should be active for a future staffed run");
        require(queued.lines().stream().anyMatch(line -> line.contains("Worker quality cap: Fine")), "queued cap wording");
        if (game.timer != null) game.timer.stop();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone03ProductionWorkerQualitySmoke() { }
}
