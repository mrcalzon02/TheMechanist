package mechanist;

import java.util.ArrayList;

/**
 * Stable subsystem for keyboard/controller reference text.
 */
final class ControlReferenceTextSubsystem {
    private ControlReferenceTextSubsystem() {
    }

    static ArrayList<String> controlsReferenceLines(GamePanel panel) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(controlProfileTitle(panel.controlsTab));
        lines.add("Action names are shared across keyboard and controller paths; context decides whether a prompt moves, examines, interacts, confirms, or backs out.");
        lines.add("Required recovery actions: Move, Confirm, Cancel/Back, Pause/Menu, and a keyboard/mouse fallback.");
        for (InputAction action : InputAction.values()) {
            lines.add(inputActionLabel(action) + " | " + inputActionContext(action) + " | "
                    + keyboardPromptFor(action) + " | " + controllerPromptFor(panel.controlsTab, action)
                    + (requiredInputAction(action) ? " | required" : ""));
        }
        lines.add("Context overlap: E examines while the Look panel is open and interacts from the game surface; A/Cross confirms in menus and examines/interacts only where that panel owns the prompt.");
        lines.add("Safe fallback: Escape/B/Circle backs out, and keyboard movement remains available if a controller is missing or reconnecting.");
        if (panel.rebindingTarget != null && !panel.rebindingTarget.isEmpty()) lines.add(panel.rebindingTarget);
        return lines;
    }

    static String controlProfileTitle(int controlsTab) {
        switch (controlsTab) {
            case 1: return "XBOX CONTROL REFERENCE";
            case 2: return "PLAYSTATION CONTROL REFERENCE";
            case 3: return "STEAM DECK CONTROL REFERENCE";
            case 4: return "GENERIC CONTROLLER REFERENCE";
            default: return "KEYBOARD AND MOUSE CONTROL REFERENCE";
        }
    }

    static boolean requiredInputAction(InputAction action) {
        return action == InputAction.MOVE_UP || action == InputAction.MOVE_DOWN || action == InputAction.MOVE_LEFT
                || action == InputAction.MOVE_RIGHT || action == InputAction.CONFIRM || action == InputAction.CANCEL
                || action == InputAction.PAUSE;
    }

    static String inputActionLabel(InputAction action) {
        switch (action) {
            case MOVE_UP: return "Move / nudge up";
            case MOVE_DOWN: return "Move / nudge down";
            case MOVE_LEFT: return "Move / nudge left";
            case MOVE_RIGHT: return "Move / nudge right";
            case CONFIRM: return "Confirm / choose";
            case CANCEL: return "Cancel / back";
            case WAIT: return "Wait one turn";
            case INTERACT: return "Interact";
            case EXAMINE: return "Examine visible target";
            case INVENTORY: return "Inventory";
            case CHARACTER: return "Character dossier";
            case BUILD: return "Build and base tools";
            case SENSES: return "Senses";
            case PLAN_MOVE: return "Plan exact movement";
            case PAUSE: return "Pause / menu";
            case LOOK: return "Look";
            case MAP: return "Map";
            case ZOOM_IN: return "Zoom in";
            case ZOOM_OUT: return "Zoom out";
            case ATTACK: return "Combat targeting";
            case RELOAD: return "Reload";
            default: return action.name();
        }
    }

    static String inputActionContext(InputAction action) {
        switch (action) {
            case MOVE_UP: case MOVE_DOWN: case MOVE_LEFT: case MOVE_RIGHT: return "movement, targeting, lists";
            case CONFIRM: return "menus, targeting, planning";
            case CANCEL: return "global recovery";
            case WAIT: return "game surface";
            case INTERACT: return "game surface and interact panel";
            case EXAMINE: return "Look panel";
            case INVENTORY: case CHARACTER: case BUILD: case SENSES: case LOOK: case MAP: return "game surface";
            case PLAN_MOVE: return "game surface movement planning";
            case PAUSE: return "global menu";
            case ZOOM_IN: case ZOOM_OUT: return "map and viewport";
            case ATTACK: case RELOAD: return "combat";
            default: return "general";
        }
    }

    static String keyboardPromptFor(InputAction action) {
        switch (action) {
            case MOVE_UP: return "Keyboard: W / Up";
            case MOVE_DOWN: return "Keyboard: S / Down";
            case MOVE_LEFT: return "Keyboard: A / Left";
            case MOVE_RIGHT: return "Keyboard: D / Right";
            case CONFIRM: return "Keyboard: Enter / Space";
            case CANCEL: return "Keyboard: Escape";
            case WAIT: return "Keyboard: . / Numpad 5";
            case INTERACT: return "Keyboard: E";
            case EXAMINE: return "Keyboard: E while looking";
            case INVENTORY: return "Keyboard: I";
            case CHARACTER: return "Keyboard: C";
            case BUILD: return "Keyboard: B";
            case SENSES: return "Keyboard: G";
            case PLAN_MOVE: return "Keyboard: P";
            case PAUSE: return "Keyboard: Escape";
            case LOOK: return "Keyboard: L";
            case MAP: return "Keyboard: M";
            case ZOOM_IN: return "Keyboard: Home / wheel up";
            case ZOOM_OUT: return "Keyboard: End / wheel down";
            case ATTACK: return "Keyboard: F";
            case RELOAD: return "Keyboard: X";
            default: return "Keyboard: unassigned";
        }
    }

    static String controllerPromptFor(int controlsTab, InputAction action) {
        switch (controlsTab) {
            case 1: return xboxPromptFor(action);
            case 2: return playstationPromptFor(action);
            case 3: return steamPromptFor(action);
            case 4: return genericPromptFor(action);
            default: return "Controller: see controller tabs";
        }
    }

    static String xboxPromptFor(InputAction action) {
        switch (action) {
            case MOVE_UP: case MOVE_DOWN: case MOVE_LEFT: case MOVE_RIGHT: return "Xbox: left stick / D-pad";
            case CONFIRM: case INTERACT: case EXAMINE: return "Xbox: A";
            case CANCEL: return "Xbox: B";
            case INVENTORY: return "Xbox: X";
            case CHARACTER: return "Xbox: Y";
            case PAUSE: return "Xbox: Menu";
            case LOOK: return "Xbox: View";
            case ZOOM_IN: return "Xbox: RB";
            case ZOOM_OUT: return "Xbox: LB";
            case ATTACK: return "Xbox: right stick up";
            case PLAN_MOVE: return "Xbox: right stick down";
            default: return "Xbox: keyboard fallback";
        }
    }

    static String playstationPromptFor(InputAction action) {
        switch (action) {
            case MOVE_UP: case MOVE_DOWN: case MOVE_LEFT: case MOVE_RIGHT: return "PlayStation: left stick / D-pad";
            case CONFIRM: case INTERACT: case EXAMINE: return "PlayStation: Cross";
            case CANCEL: return "PlayStation: Circle";
            case INVENTORY: return "PlayStation: Square";
            case CHARACTER: return "PlayStation: Triangle";
            case PAUSE: return "PlayStation: Options";
            case LOOK: return "PlayStation: Share/Create";
            case ZOOM_IN: return "PlayStation: R1";
            case ZOOM_OUT: return "PlayStation: L1";
            case ATTACK: return "PlayStation: right stick up";
            case PLAN_MOVE: return "PlayStation: right stick down";
            default: return "PlayStation: keyboard fallback";
        }
    }

    static String steamPromptFor(InputAction action) {
        switch (action) {
            case MOVE_UP: case MOVE_DOWN: case MOVE_LEFT: case MOVE_RIGHT: return "Steam: stick / D-pad";
            case CONFIRM: case INTERACT: case EXAMINE: return "Steam: A";
            case CANCEL: return "Steam: B";
            case INVENTORY: return "Steam: X";
            case CHARACTER: return "Steam: Y";
            case PAUSE: return "Steam: Menu";
            case LOOK: return "Steam: View";
            case ZOOM_IN: return "Steam: R1";
            case ZOOM_OUT: return "Steam: L1";
            case ATTACK: return "Steam: right stick up";
            case PLAN_MOVE: return "Steam: right stick down";
            default: return "Steam: keyboard fallback";
        }
    }

    static String genericPromptFor(InputAction action) {
        switch (action) {
            case MOVE_UP: case MOVE_DOWN: case MOVE_LEFT: case MOVE_RIGHT: return "Generic: left stick / D-pad";
            case CONFIRM: case INTERACT: case EXAMINE: return "Generic: south face";
            case CANCEL: return "Generic: east face";
            case INVENTORY: return "Generic: west face";
            case CHARACTER: return "Generic: north face";
            case PAUSE: return "Generic: Start";
            case LOOK: return "Generic: Back/Select";
            case ZOOM_IN: return "Generic: right bumper";
            case ZOOM_OUT: return "Generic: left bumper";
            case ATTACK: return "Generic: right stick up";
            case PLAN_MOVE: return "Generic: right stick down";
            default: return "Generic: keyboard fallback";
        }
    }
}
