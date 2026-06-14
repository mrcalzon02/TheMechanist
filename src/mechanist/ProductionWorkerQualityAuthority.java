package mechanist;

import java.util.List;

/** Resolves assigned recruit skill without pretending dormant automation operates manual Craft. */
final class ProductionWorkerQualityAuthority {
    record WorkerQuality(boolean assigned, boolean activeForRun, String workerName, int skill,
                         int potentialTier, String potentialQuality, List<String> lines) { }

    private ProductionWorkerQualityAuthority() { }

    static WorkerQuality evaluate(GamePanel game, BaseObject machine, boolean manual) {
        String assignedName = machine == null || machine.assignedWorker == null ? "" : machine.assignedWorker.trim();
        RecruitWorker worker = findWorker(game, assignedName);
        if (assignedName.isBlank()) {
            return new WorkerQuality(false, false, "unassigned", 0, QualityAuthorityApi.UNLIMITED_TIER, "open",
                    List.of("Worker quality: unassigned; manual Craft remains player-operated."));
        }
        if (worker == null) {
            return new WorkerQuality(true, false, assignedName, 0, QualityAuthorityApi.UNLIMITED_TIER, "unresolved",
                    List.of("Worker quality: " + assignedName + " is assigned, but no matching recruit skill record is available; no worker cap is applied."));
        }
        int tier = tierForSkill(worker.skill);
        String quality = QualityAuthorityApi.qualityName(tier);
        if (manual) {
            return new WorkerQuality(true, false, worker.name, worker.skill, tier, quality,
                    List.of("Worker quality: " + worker.name + " skill " + worker.skill + " could support " + quality + " queued work.",
                            "Manual boundary: this Craft action is player-operated, so the assigned worker does not cap or improve its result."));
        }
        return new WorkerQuality(true, true, worker.name, worker.skill, tier, quality,
                List.of("Worker quality cap: " + quality + " from " + worker.name + " skill " + worker.skill + "."));
    }

    static int tierForSkill(int skill) {
        if (skill <= 1) return 2;
        if (skill == 2) return 3;
        if (skill == 3) return 4;
        return 5;
    }

    private static RecruitWorker findWorker(GamePanel game, String name) {
        if (game == null || name == null || name.isBlank()) return null;
        for (RecruitWorker worker : game.factionRecruits) {
            if (worker != null && worker.name != null && worker.name.equalsIgnoreCase(name)) return worker;
        }
        return null;
    }
}
