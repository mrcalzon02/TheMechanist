package mechanist;

import mechanist.input.InputDevice;
import mechanist.input.InputToken;
import mechanist.input.KeyBind;
import mechanist.input.KeyBindingManager;

/**
 * Fourth controller schema fallback. Interprets standard SDL/Jamepad face, d-pad,
 * and stick inputs into abstract actions through the current controller profile.
 */
final class GenericControllerSchema {
    static final String VERSION = "0.9.10ka";
    private static final float DIGITAL_AXIS_THRESHOLD = 0.55f;
    private final ControllerTapHoldTracker southFaceTracker = new ControllerTapHoldTracker();

    void apply(GamepadControllerSnapshot pad, InputRegistry registry) {
        apply(pad, registry, System.currentTimeMillis());
    }

    void apply(GamepadControllerSnapshot pad, InputRegistry registry, long nowMs) {
        if (pad == null || !pad.connected) {
            registry.clearSource(InputSource.GAMEPAD);
            southFaceTracker.clear();
            return;
        }

        KeyBindingManager manager = KeyBindingManager.getInstance();
        KeyBindingManager.ControllerTuningProfile tuning = manager.getControllerTuningProfile();
        double leftX = manager.applyControllerAxisTuning("left_stick_x", pad.leftX);
        double leftY = manager.applyControllerAxisTuning("left_stick_y", pad.leftY);
        double rightX = manager.applyControllerAxisTuning("right_stick_x", pad.rightX);
        double rightY = manager.applyControllerAxisTuning("right_stick_y", pad.rightY);
        ControllerTapHoldTracker.TapHoldRead southFaceRead = southFaceTracker.update(pad.a, nowMs, tuning);

        for (InputAction action : InputAction.values()) {
            registry.setDigital(InputSource.GAMEPAD, action, boundActionActive(action, pad, manager, leftX, leftY, rightX, rightY, southFaceRead));
            registry.setAnalog(InputSource.GAMEPAD, action, 0.0f);
        }

        boolean up = registry.isActiveFromSource(InputAction.MOVE_UP, InputSource.GAMEPAD)
                || defaultDpadCompanion(InputAction.MOVE_UP, "PAD_HAT_UP", leftY < -DIGITAL_AXIS_THRESHOLD, manager);
        boolean down = registry.isActiveFromSource(InputAction.MOVE_DOWN, InputSource.GAMEPAD)
                || defaultDpadCompanion(InputAction.MOVE_DOWN, "PAD_HAT_DOWN", leftY > DIGITAL_AXIS_THRESHOLD, manager);
        boolean left = registry.isActiveFromSource(InputAction.MOVE_LEFT, InputSource.GAMEPAD)
                || defaultDpadCompanion(InputAction.MOVE_LEFT, "PAD_HAT_LEFT", leftX < -DIGITAL_AXIS_THRESHOLD, manager);
        boolean right = registry.isActiveFromSource(InputAction.MOVE_RIGHT, InputSource.GAMEPAD)
                || defaultDpadCompanion(InputAction.MOVE_RIGHT, "PAD_HAT_RIGHT", leftX > DIGITAL_AXIS_THRESHOLD, manager);

        registry.setDigital(InputSource.GAMEPAD, InputAction.MOVE_UP, up);
        registry.setDigital(InputSource.GAMEPAD, InputAction.MOVE_DOWN, down);
        registry.setDigital(InputSource.GAMEPAD, InputAction.MOVE_LEFT, left);
        registry.setDigital(InputSource.GAMEPAD, InputAction.MOVE_RIGHT, right);
        registry.setAnalog(InputSource.GAMEPAD, InputAction.MOVE_LEFT, left ? (float)Math.abs(leftX) : 0.0f);
        registry.setAnalog(InputSource.GAMEPAD, InputAction.MOVE_RIGHT, right ? (float)Math.abs(leftX) : 0.0f);
        registry.setAnalog(InputSource.GAMEPAD, InputAction.MOVE_UP, up ? (float)Math.abs(leftY) : 0.0f);
        registry.setAnalog(InputSource.GAMEPAD, InputAction.MOVE_DOWN, down ? (float)Math.abs(leftY) : 0.0f);
    }

    private static boolean boundActionActive(InputAction action, GamepadControllerSnapshot pad, KeyBindingManager manager,
                                             double leftX, double leftY, double rightX, double rightY,
                                             ControllerTapHoldTracker.TapHoldRead southFaceRead) {
        if (action == InputAction.INTERACT && southFaceRead != null && southFaceRead.holdActive()) return true;
        String commandId = ControlReferenceTextSubsystem.commandIdFor(action);
        if (commandId == null || commandId.isBlank()) return false;
        return manager.getBinding(InputDevice.GENERIC_CONTROLLER, commandId)
                .map(KeyBind::token)
                .map(token -> tokenActive(action, token, pad, leftX, leftY, rightX, rightY, southFaceRead))
                .orElse(false);
    }

    private static boolean tokenActive(InputAction action, InputToken token, GamepadControllerSnapshot pad,
                                       double leftX, double leftY, double rightX, double rightY,
                                       ControllerTapHoldTracker.TapHoldRead southFaceRead) {
        if (token == null || token.device() != InputDevice.GENERIC_CONTROLLER) return false;
        String code = token.code();
        if (code == null) return false;
        if ("PAD_BUTTON_0".equals(code)) return southFaceActionActive(action, southFaceRead);
        return switch (code) {
            case "PAD_BUTTON_1" -> pad.b;
            case "PAD_BUTTON_2" -> pad.x;
            case "PAD_BUTTON_3" -> pad.y;
            case "PAD_BUTTON_4" -> pad.leftBumper;
            case "PAD_BUTTON_5" -> pad.rightBumper;
            case "PAD_BUTTON_6" -> pad.back;
            case "PAD_BUTTON_7" -> pad.start;
            case "PAD_HAT_UP" -> pad.dpadUp;
            case "PAD_HAT_DOWN" -> pad.dpadDown;
            case "PAD_HAT_LEFT" -> pad.dpadLeft;
            case "PAD_HAT_RIGHT" -> pad.dpadRight;
            default -> axisTokenActive(code, leftX, leftY, rightX, rightY);
        };
    }

    private static boolean southFaceActionActive(InputAction action, ControllerTapHoldTracker.TapHoldRead read) {
        if (read == null) return false;
        return switch (action) {
            case INTERACT -> read.holdActive();
            case CONFIRM, EXAMINE -> read.tapReleased();
            default -> read.pressed();
        };
    }

    private static boolean axisTokenActive(String code, double leftX, double leftY, double rightX, double rightY) {
        if (!code.startsWith("PAD_AXIS_")) return false;
        String body = code.substring("PAD_AXIS_".length());
        boolean positive = body.endsWith("_POS");
        boolean negative = body.endsWith("_NEG");
        if (!positive && !negative) return false;
        String axisName = body.substring(0, body.length() - 4);
        double value = axisValue(axisName, leftX, leftY, rightX, rightY);
        return positive ? value > DIGITAL_AXIS_THRESHOLD : value < -DIGITAL_AXIS_THRESHOLD;
    }

    private static double axisValue(String axisName, double leftX, double leftY, double rightX, double rightY) {
        return switch (axisName) {
            case "LEFTX", "LEFT_X", "LEFT_STICK_X" -> leftX;
            case "LEFTY", "LEFT_Y", "LEFT_STICK_Y" -> leftY;
            case "RIGHTX", "RIGHT_X", "RIGHT_STICK_X" -> rightX;
            case "RIGHTY", "RIGHT_Y", "RIGHT_STICK_Y" -> rightY;
            default -> 0.0;
        };
    }

    private static boolean defaultDpadCompanion(InputAction action, String dpadCode, boolean stickActive, KeyBindingManager manager) {
        if (!stickActive) return false;
        String commandId = ControlReferenceTextSubsystem.commandIdFor(action);
        if (commandId == null || commandId.isBlank()) return false;
        return manager.getBinding(InputDevice.GENERIC_CONTROLLER, commandId)
                .map(KeyBind::token)
                .map(InputToken::code)
                .map(dpadCode::equals)
                .orElse(false);
    }

    static String auditSummary() {
        return "genericControllerSchema version=" + VERSION
                + " digitalAxisThreshold=" + DIGITAL_AXIS_THRESHOLD
                + " maps=currentGenericControllerBindings+dpadCompanionStick"
                + " tuning=deadzone+sensitivity+axisInversion+tapHold fallback=generic"
                + " tapHold=" + ControllerTapHoldTracker.auditSummary();
    }
}
