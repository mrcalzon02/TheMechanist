package mechanist;

/** @deprecated Use ControlReferenceOptionsRuntime. */
@Deprecated
public class LayerH {
    public LayerH() {}

    static String controlProfileTitle(GamePanel panel) { return ControlReferenceOptionsRuntime.controlProfileTitle(panel); }
    static boolean requiredMovementInput(InputAction action) { return ControlReferenceOptionsRuntime.requiredMovementInput(action); }
    static boolean requiredNavigationInput(InputAction action) { return ControlReferenceOptionsRuntime.requiredNavigationInput(action); }
    static boolean requiredInputAction(InputAction action) { return ControlReferenceOptionsRuntime.requiredInputAction(action); }
    static String movementPromptText(InputAction action) { return ControlReferenceOptionsRuntime.movementPromptText(action); }
    static String navigationPromptText(InputAction action) { return ControlReferenceOptionsRuntime.navigationPromptText(action); }
    static String keyboardPromptFor(InputAction action) { return ControlReferenceOptionsRuntime.keyboardPromptFor(action); }
}
