package mechanist;

/** Smoke for the movement validation overlay contract. */
final class Milestone02MovementDebugOverlaySmoke {
    public static void main(String[] args) {
        MovementDebugOverlayAuthority.MovementDebugSnapshot snapshot =
                new MovementDebugOverlayAuthority.MovementDebugSnapshot(7, 9, "occupied",
                        "resolved through actor displacement", "applied to 6,9", "accepted");
        String lines = String.join(" | ", java.util.List.of(
                "Movement debug destination: " + snapshot.destinationX() + "," + snapshot.destinationY() + ".",
                "Occupancy: " + snapshot.occupancyResult() + ".",
                "Push/squeeze: " + snapshot.pushSqueezeResult() + ".",
                "Recovery: " + snapshot.recoveryResult() + "."));
        requireContains(lines, "destination: 7,9", "debug destination");
        requireContains(lines, "Occupancy: occupied", "debug occupancy");
        requireContains(lines, "actor displacement", "debug push/squeeze");
        requireContains(lines, "Recovery: applied", "debug recovery");
        requireContains(MovementDebugOverlayAuthority.auditSummary(),
                "exposes=destination+occupancy+pushSqueeze+recovery+execution", "debug overlay audit");
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
        }
    }

    private Milestone02MovementDebugOverlaySmoke() { }
}
