package mechanist;

public class LayerH {
    public LayerH() {}

    static String controlProfileTitle(GamePanel panel) {
        switch (panel.controlsTab) {
            case 1: return "XBOX CONTROL REFERENCE";
            case 2: return "PLAYSTATION CONTROL REFERENCE";
            case 3: return "STEAM DECK CONTROL REFERENCE";
            case 4: return "GENERIC CONTROLLER REFERENCE";
            default: return "KEYBOARD AND MOUSE CONTROL REFERENCE";
        }
    }

    static boolean requiredMovementInput(InputAction action) {
        return action == InputAction.MOVE_UP || action == InputAction.MOVE_DOWN || action == InputAction.MOVE_LEFT || action == InputAction.MOVE_RIGHT;
    }

    static boolean requiredNavigationInput(InputAction action) {
        return action == InputAction.CONFIRM || action == InputAction.CANCEL || action == InputAction.PAUSE;
    }

    static boolean requiredInputAction(InputAction action) {
        return requiredMovementInput(action) || requiredNavigationInput(action);
    }

    static String movementPromptText(InputAction action) {
        if (action == InputAction.MOVE_UP) return "W / Up";
        if (action == InputAction.MOVE_DOWN) return "S / Down";
        if (action == InputAction.MOVE_LEFT) return "A / Left";
        if (action == InputAction.MOVE_RIGHT) return "D / Right";
        return null;
    }

    static String navigationPromptText(InputAction action) {
        if (action == InputAction.CONFIRM) return "Enter / Space";
        if (action == InputAction.CANCEL) return "Escape";
        if (action == InputAction.PAUSE) return "Escape";
        return null;
    }

    static String keyboardPromptFor(InputAction action) {
        return ControlReferenceTextSubsystem.keyboardPromptFor(action);
    }
}
