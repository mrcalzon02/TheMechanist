package mechanist;

import java.util.List;

/** Smoke for exact, uncertain, hidden, and unsafe quest guidance behavior. */
final class Milestone02QuestObjectiveGuidanceSmoke {
    public static void main(String[] args) {
        QuestObjectiveGuidanceAuthority.ObjectiveGuidance exact = objective("Recover the ledger", QuestObjectiveGuidanceAuthority.GuidanceKind.EXACT, 8, 4, true, true);
        QuestObjectiveGuidanceAuthority.GuidanceReadout exactReadout = QuestObjectiveGuidanceAuthority.describe(exact, 3, 4, 0L);
        require(exactReadout.showTargetMarker(), "visible exact objective should show its marker");
        require(exactReadout.pulsing(), "exact marker should use the slow pulse phase");
        requireContains(exactReadout.summary(), "east", "exact objective direction");

        QuestObjectiveGuidanceAuthority.GuidanceReadout rumored = QuestObjectiveGuidanceAuthority.describe(
                objective("Find the missing factor", QuestObjectiveGuidanceAuthority.GuidanceKind.RUMORED, 20, 20, true, true), 3, 4, 0L);
        require(!rumored.showTargetMarker(), "rumored objective must not expose an exact marker");
        require(!rumored.showDirection(), "rumored objective must not expose an exact direction");
        requireContains(rumored.summary(), "rumored location", "rumored objective wording");

        QuestObjectiveGuidanceAuthority.GuidanceReadout hidden = QuestObjectiveGuidanceAuthority.describe(
                objective("Identify the saboteur", QuestObjectiveGuidanceAuthority.GuidanceKind.HIDDEN, 7, 7, true, true), 3, 4, 0L);
        requireContains(hidden.summary(), "more evidence", "hidden objective evidence wording");

        List<QuestObjectiveGuidanceAuthority.ObjectiveGuidance> ordered = QuestObjectiveGuidanceAuthority.orderedActive(List.of(
                exact,
                objective("Unsafe machine room", QuestObjectiveGuidanceAuthority.GuidanceKind.UNSAFE, 6, 4, true, true)), 3, 4);
        require(ordered.get(0).kind() == QuestObjectiveGuidanceAuthority.GuidanceKind.UNSAFE, "unsafe guidance should be surfaced first");
        requireContains(QuestObjectiveGuidanceAuthority.auditSummary(), "ownsQuestProgression=false", "guidance boundary audit");
    }

    private static QuestObjectiveGuidanceAuthority.ObjectiveGuidance objective(String label, QuestObjectiveGuidanceAuthority.GuidanceKind kind,
                                                                                int x, int y, boolean currentSlice, boolean visible) {
        return new QuestObjectiveGuidanceAuthority.ObjectiveGuidance(label, kind, x, y, currentSlice, visible, "");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private Milestone02QuestObjectiveGuidanceSmoke() { }
}
