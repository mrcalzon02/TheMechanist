package mechanist;

import mechanist.input.KeyBindingManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Player-facing audit surface for input action and rebinding readiness.
 *
 * This does not perform live rebinding. It exposes the owned action definitions,
 * prompt source, recovery requirements, overlap notes, and current-profile
 * fallback text so controls can be inspected through Infopedia and smokes.
 */
final class InputRebindingAuditAuthority {
    static final String VERSION = "0.9.10kc-input-audit";

    private InputRebindingAuditAuthority() { }

    record ActionAudit(InputAction action, String label, String context, String keyboard, String controller,
                       boolean required, boolean rebindable, String conflictNote, String recoveryNote) {
        String playerFacingLine() {
            StringBuilder out = new StringBuilder(label)
                    .append(" | Context: ").append(context)
                    .append(" | Default: ").append(keyboard)
                    .append(" | Controller: ").append(controller)
                    .append(" | Current profile: default mapping");
            if (required) out.append(" | Required recovery action");
            out.append(" | ").append(rebindable ? "Rebindable with conflict review" : "Locked for recovery");
            if (!conflictNote.isBlank()) out.append(" | Conflict: ").append(conflictNote);
            if (!recoveryNote.isBlank()) out.append(" | Recovery: ").append(recoveryNote);
            return PlayerFacingText.sanitize(out.toString());
        }
    }

    static String playerFacingSummary() {
        int required = 0;
        for (InputAction action : InputAction.values()) if (ControlReferenceTextSubsystem.requiredInputAction(action)) required++;
        return "Input audit covers " + InputAction.values().length + " actions, " + required
                + " recovery-critical actions, keyboard defaults, controller prompts, conflict notes, controller tuning, glyph fallback, and safe fallback behavior.";
    }

    static String controllerTuningSummary() {
        return PlayerFacingText.sanitize(KeyBindingManager.getInstance().getControllerTuningProfile().playerFacingSummary());
    }

    static List<ActionAudit> auditRows(int controlsTab) {
        ArrayList<ActionAudit> rows = new ArrayList<>();
        for (InputAction action : InputAction.values()) {
            boolean required = ControlReferenceTextSubsystem.requiredInputAction(action);
            rows.add(new ActionAudit(
                    action,
                    ControlReferenceTextSubsystem.inputActionLabel(action),
                    ControlReferenceTextSubsystem.inputActionContext(action),
                    ControlReferenceTextSubsystem.keyboardPromptFor(action),
                    ControlReferenceTextSubsystem.controllerPromptFor(controlsTab, action),
                    required,
                    !required || action == InputAction.MOVE_UP || action == InputAction.MOVE_DOWN || action == InputAction.MOVE_LEFT || action == InputAction.MOVE_RIGHT,
                    conflictNote(action),
                    recoveryNote(action, required)
            ));
        }
        return List.copyOf(rows);
    }

    static List<String> playerFacingAuditLines(int controlsTab) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(playerFacingSummary());
        lines.add("Profile source: current in-game controls tab with keyboard fallback; saved custom-profile export and import are validated before replacing the working profile.");
        lines.add(controllerTuningSummary());
        lines.add("Controller tuning behavior: deadzone, axis sensitivity, horizontal/vertical inversion, and tap/hold thresholds are saved with the profile and rejected if malformed.");
        lines.add("Controller prompt behavior: readable controller button names are shown now, with explicit text fallback until packaged glyph art exists.");
        lines.add("Reset behavior: Reset to defaults and keep keyboard fallback available before accepting any saved custom profile.");
        lines.add("Last-good behavior: profile imports keep the previous usable controls until bindings and controller tuning pass required-action checks.");
        lines.add("Conflict rule: warn when two actions compete inside the same context; allow harmless overlaps only when the owning panel decides which action is active.");
        for (ActionAudit row : auditRows(controlsTab)) lines.add(row.playerFacingLine());
        return lines;
    }

    static List<String> infopediaLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(playerFacingSummary());
        lines.add("Audit source: InputRebindingAuditAuthority composes rows from shared action labels and prompt definitions.");
        lines.add("Keyboard and mouse defaults remain visible even in controller views so the player has a recovery path.");
        lines.add("Live recovery: the keybinding manager keeps default bindings, stores the previous working profile before changes, can restore last-good bindings, and can reset one tab or all input families.");
        lines.add(controllerTuningSummary());
        lines.add("Implemented controller tuning: saved profiles now include deadzone, sensitivity, horizontal/vertical inversion, and tap/hold thresholds with validation before replacing the working profile.");
        lines.add("Implemented controller connection notices: controller connect, reconnect, and disconnect transitions keep keyboard and mouse fallback visible.");
        lines.add("Implemented controller prompt fallback: readable text prompts remain active while packaged controller glyph art is unavailable.");
        lines.add("Current limitation: per-controller glyph artwork selection still needs asset packaging and renderer integration.");
        lines.addAll(ControllerGlyphPromptAuthority.auditLines(4).subList(0, 2));
        List<String> audit = playerFacingAuditLines(4);
        lines.addAll(audit.subList(1, Math.min(10, audit.size())));
        lines.add("Guard: Milestone02InputRebindingAuditSmoke checks action coverage, required recovery actions, conflict notes, and leak-free Infopedia detail.");
        lines.add("Guard: Milestone02InputConflictRecoverySmoke checks duplicate conflicts, last-good restore, required-action preservation, and reset-to-default behavior.");
        lines.add("Guard: Milestone02InputProfilePersistenceSmoke checks saved profile export, import, prompt refresh, controller tuning, and corrupt-profile rejection.");
        lines.add("Guard: Milestone02ControllerGlyphPromptSmoke checks controller text fallback, keyboard/mouse recovery wording, and glyph-readiness audit text.");
        return lines;
    }

    private static String conflictNote(InputAction action) {
        switch (action) {
            case MOVE_UP: case MOVE_DOWN: case MOVE_LEFT: case MOVE_RIGHT:
                return "shares directional controls across movement, targeting, ghost nudging, and list navigation.";
            case CONFIRM:
                return "overlaps with menu choice and planning confirmation; panel ownership decides intent.";
            case CANCEL: case PAUSE:
                return "must stay reachable because it backs out of menus and unsafe modes.";
            case INTERACT: case EXAMINE:
                return "shares a familiar use button; Look owns Examine while the game surface owns Interact.";
            case ZOOM_IN: case ZOOM_OUT:
                return "may overlap mouse wheel or bumper prompts in map and viewport contexts.";
            case ATTACK: case RELOAD:
                return "combat-only overlap is acceptable outside ordinary menu navigation.";
            default:
                return "no dangerous default overlap registered.";
        }
    }

    private static String recoveryNote(InputAction action, boolean required) {
        if (action == InputAction.CANCEL) return "Escape or controller back must always leave the current mode.";
        if (action == InputAction.PAUSE) return "Menu access must keep reset-to-default reachable.";
        if (action == InputAction.CONFIRM) return "Confirm cannot be accepted as unbound unless another safe menu choose action exists.";
        if (required) return "Do not allow this action to become unrecoverably unbound.";
        return "May be cleared only when the menu explains the lost feature and keeps Cancel available.";
    }
}
