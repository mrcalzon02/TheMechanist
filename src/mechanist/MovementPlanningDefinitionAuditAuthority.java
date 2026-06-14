package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Player-readable definition and gap audit for exact movement planning. */
final class MovementPlanningDefinitionAuditAuthority {
    static List<String> infopediaLines(int controlsTab) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Movement planning definition audit");
        lines.add("Mode: Sneak, Walk, Run, or Sprint controls route range, color, fatigue, vision, and noise consequences.");
        lines.add("Ghost overlay: reachable range, route line, endpoint portrait, and endpoint cursor use the active movement-mode color; partial endpoints use a warning color.");
        lines.add("Placement inputs: Plan Exact Movement starts at the player; directional Run or Sprint starts a ranged plan; mouse movement previews a route; Approach selects the shortest reachable adjacent tile.");
        lines.add(ControlReferenceTextSubsystem.contextPromptLine("Movement planning", controlsTab,
                InputAction.CONFIRM, InputAction.CANCEL, "Move actions nudge the ghost; confirm commits the route."));
        lines.add(ControlReferenceTextSubsystem.contextPromptLine("Start movement planning", controlsTab,
                InputAction.PLAN_MOVE, InputAction.CANCEL, "Open the exact-movement ghost or return safely."));
        lines.add("Valid target rules: destination must be inside the current area, walkable, reachable within the active range, and unoccupied unless the actor-layer shove/squeeze resolver accepts it.");
        lines.add("Refusal outcomes: outside current area, path blocked, destination occupied, cannot reach from here, or partial route when the full destination exceeds one movement chain.");
        lines.add("Hazards: a route crossing recorded hazard tiles renders amber and reports tile count plus highest concern before confirmation. Warnings do not yet block or price movement by exposure.");
        lines.add("Quick movement parity: a Walk or Sneak step toward a recorded hazard opens a one-tile confirmation ghost instead of moving immediately.");
        lines.add("Interaction adjacency: Approach evaluates all four adjacent tiles and opens the same manual ghost for explicit confirmation; it never moves automatically.");
        lines.add("Controller status: Plan Exact Movement and directional action bindings are registered. End-to-end gamepad ghost nudging still requires live runtime verification.");
        lines.add("Overlay priority: quest guidance and the facing vision cone draw first; the movement range/path draws above them; Look, combat, construction cursors, and the player sprite draw afterward.");
        lines.add("Reset behavior: confirm, cancel, movement recovery, completed movement, save/load entry, successful load, and main-menu return clear manual and mouse routes, hazard flags, and ghost focus.");
        lines.add("Guard: Milestone02MovementPlanningDefinitionAuditSmoke checks required fields, truthful gaps, current prompts, and leak-free wording.");
        return lines;
    }

    static String summary() {
        return "Movement planning audit: mode, ghost, placement, bindings, target rules, refusals, hazard warnings, adjacency, overlay priority, and focus reset documented; gamepad live verification remains pending.";
    }

    private MovementPlanningDefinitionAuditAuthority() { }
}
