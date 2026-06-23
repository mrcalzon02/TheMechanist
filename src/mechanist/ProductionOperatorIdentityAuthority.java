package mechanist;

import java.util.List;

/** Resolves the immediate manual-production operator for forecast and provenance. */
final class ProductionOperatorIdentityAuthority {
    record OperatorIdentity(String provenanceLabel, List<String> lines) { }

    private ProductionOperatorIdentityAuthority() { }

    static OperatorIdentity evaluate(GamePanel game) {
        String name = game == null || game.active == null ? "player operator" : provenanceLabel(game.active.name);
        return new OperatorIdentity(name, List.of(
                "Producing operator: " + name + " (immediate manual Craft).",
                "Operator boundary: assigned workers and supervisors are not credited unless a queued staffed-production owner executes the run."));
    }

    static String provenanceLabel(String worker) {
        return worker == null || worker.isBlank() ? "unknown manual operator" : worker.trim();
    }
}
