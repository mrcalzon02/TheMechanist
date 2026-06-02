package mechanist;

/**
 * Fourth controller schema fallback. Interprets standard SDL/Jamepad face, d-pad,
 * and stick inputs into abstract actions with a deadzone.
 */
final class GenericControllerSchema {
    static final String VERSION = "0.9.10et";
    static final float DEADZONE = 0.15f;

    void apply(GamepadControllerSnapshot pad, InputRegistry registry) {
        if (pad == null || !pad.connected) {
            registry.clearSource(InputSource.GAMEPAD);
            return;
        }
        boolean up = pad.dpadUp || pad.leftY < -DEADZONE;
        boolean down = pad.dpadDown || pad.leftY > DEADZONE;
        boolean left = pad.dpadLeft || pad.leftX < -DEADZONE;
        boolean right = pad.dpadRight || pad.leftX > DEADZONE;
        registry.setDigital(InputSource.GAMEPAD, InputAction.MOVE_UP, up);
        registry.setDigital(InputSource.GAMEPAD, InputAction.MOVE_DOWN, down);
        registry.setDigital(InputSource.GAMEPAD, InputAction.MOVE_LEFT, left);
        registry.setDigital(InputSource.GAMEPAD, InputAction.MOVE_RIGHT, right);
        registry.setDigital(InputSource.GAMEPAD, InputAction.CONFIRM, pad.a);
        registry.setDigital(InputSource.GAMEPAD, InputAction.CANCEL, pad.b);
        registry.setDigital(InputSource.GAMEPAD, InputAction.INVENTORY, pad.x);
        registry.setDigital(InputSource.GAMEPAD, InputAction.CHARACTER, pad.y);
        registry.setDigital(InputSource.GAMEPAD, InputAction.PAUSE, pad.start);
        registry.setDigital(InputSource.GAMEPAD, InputAction.LOOK, pad.back);
        registry.setDigital(InputSource.GAMEPAD, InputAction.EXAMINE, pad.a);
        registry.setDigital(InputSource.GAMEPAD, InputAction.INTERACT, pad.a);
        registry.setDigital(InputSource.GAMEPAD, InputAction.PLAN_MOVE, pad.rightY > 0.75f);
        registry.setDigital(InputSource.GAMEPAD, InputAction.ZOOM_OUT, pad.leftBumper);
        registry.setDigital(InputSource.GAMEPAD, InputAction.ZOOM_IN, pad.rightBumper);
        registry.setDigital(InputSource.GAMEPAD, InputAction.ATTACK, pad.rightY < -0.75f);
        registry.setAnalog(InputSource.GAMEPAD, InputAction.MOVE_LEFT, left ? Math.abs(pad.leftX) : 0.0f);
        registry.setAnalog(InputSource.GAMEPAD, InputAction.MOVE_RIGHT, right ? Math.abs(pad.leftX) : 0.0f);
        registry.setAnalog(InputSource.GAMEPAD, InputAction.MOVE_UP, up ? Math.abs(pad.leftY) : 0.0f);
        registry.setAnalog(InputSource.GAMEPAD, InputAction.MOVE_DOWN, down ? Math.abs(pad.leftY) : 0.0f);
    }

    static String auditSummary() {
        return "genericControllerSchema version=" + VERSION + " deadzone=" + DEADZONE + " maps=faceButtons+dpad+leftStick+rightStick fallback=generic";
    }
}
