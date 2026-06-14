package mechanist;

import java.util.List;

final class Milestone02MovementPlanningDefinitionAuditSmoke {
    public static void main(String[] args) {
        List<String> lines = MovementPlanningDefinitionAuditAuthority.infopediaLines(4);
        requireContains(lines, "Sneak, Walk, Run, or Sprint", "movement modes");
        requireContains(lines, "reachable range, route line, endpoint portrait", "ghost overlay");
        requireContains(lines, "Generic: right stick down", "current controller start binding");
        requireContains(lines, "outside current area", "invalid target outcomes");
        requireContains(lines, "renders amber and reports tile count plus highest concern", "hazard warning behavior");
        requireContains(lines, "do not yet block or price movement", "hazard limitation");
        requireContains(lines, "Walk or Sneak step toward a recorded hazard opens a one-tile confirmation ghost", "quick movement hazard parity");
        requireContains(lines, "never moves automatically", "approach confirmation boundary");
        requireContains(lines, "gamepad ghost nudging still requires live runtime verification", "controller verification gap");
        requireContains(lines, "quest guidance and the facing vision cone draw first", "overlay priority");
        requireContains(lines, "save/load entry, successful load, and main-menu return clear", "focus reset coverage");
        for (String line : lines) {
            if (PlayerFacingText.containsLikelyLeak(line)) throw new AssertionError("Movement planning audit leaked implementation text: " + line);
        }
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone02MovementPlanningDefinitionAuditSmoke() { }
}
