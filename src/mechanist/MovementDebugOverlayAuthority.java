package mechanist;

import java.util.List;

/** Compact runtime movement trace used by the pause/session validation surface. */
final class MovementDebugOverlayAuthority {
    static final String VERSION = "0.9.10kj";

    record MovementDebugSnapshot(int destinationX, int destinationY, String occupancyResult,
                                 String pushSqueezeResult, String recoveryResult, String executionResult) {
        static MovementDebugSnapshot idle() {
            return new MovementDebugSnapshot(0, 0, "not evaluated", "not evaluated", "not requested", "no movement attempt recorded");
        }
    }

    private MovementDebugOverlayAuthority() { }

    static void recordExecution(GamePanel game, int destinationX, int destinationY, boolean occupied,
                                boolean pushSqueezeUsed, boolean success, String result) {
        if (game == null) return;
        String recovery = game.movementDebugSnapshot == null ? "not requested" : game.movementDebugSnapshot.recoveryResult();
        game.movementDebugSnapshot = new MovementDebugSnapshot(destinationX, destinationY,
                occupied ? "occupied" : "open",
                pushSqueezeUsed ? "resolved through actor displacement" : (occupied ? "resolver did not displace an actor" : "not required"),
                recovery,
                (success ? "accepted: " : "rejected: ") + PlayerFacingText.sanitize(result));
    }

    static void recordRecovery(GamePanel game, MovementPlanningAuthority.MovementRecoveryApplicationResult result) {
        if (game == null || result == null) return;
        MovementDebugSnapshot prior = game.movementDebugSnapshot == null ? MovementDebugSnapshot.idle() : game.movementDebugSnapshot;
        game.movementDebugSnapshot = new MovementDebugSnapshot(prior.destinationX(), prior.destinationY(),
                prior.occupancyResult(), prior.pushSqueezeResult(),
                (result.applied() ? "applied to " + result.toX() + "," + result.toY() : "unchanged") + ": " + result.summary(),
                prior.executionResult());
    }

    static List<String> overlayLines(GamePanel game) {
        MovementDebugSnapshot snapshot = game == null || game.movementDebugSnapshot == null
                ? MovementDebugSnapshot.idle() : game.movementDebugSnapshot;
        return List.of(
                "Movement debug destination: " + snapshot.destinationX() + "," + snapshot.destinationY() + ".",
                "Occupancy: " + snapshot.occupancyResult() + ".",
                "Push/squeeze: " + snapshot.pushSqueezeResult() + ".",
                "Recovery: " + snapshot.recoveryResult() + ".",
                "Execution: " + snapshot.executionResult() + ".");
    }

    static String auditSummary() {
        return "movementDebugOverlayAuthority version=" + VERSION
                + " exposes=destination+occupancy+pushSqueeze+recovery+execution surface=pauseSession";
    }
}
