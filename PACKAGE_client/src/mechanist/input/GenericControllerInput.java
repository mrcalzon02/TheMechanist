package mechanist.input;

import java.util.Objects;

/**
 * Library-neutral gamepad event packet consumed by the remapping UI.
 *
 * A Jamepad/JInput/SDL bridge can translate raw hardware events into this record
 * without coupling the Swing options menu to that specific backend.
 */
public record GenericControllerInput(Type type, int buttonIndex, String axisName, int direction, String hatDirection) {
    public enum Type { BUTTON, AXIS, HAT }

    public GenericControllerInput {
        Objects.requireNonNull(type, "type");
    }

    public static GenericControllerInput button(int buttonIndex) {
        if (buttonIndex < 0) {
            throw new IllegalArgumentException("buttonIndex must be >= 0");
        }
        return new GenericControllerInput(Type.BUTTON, buttonIndex, "", 0, "");
    }

    public static GenericControllerInput axis(String axisName, int direction) {
        if (direction == 0) {
            throw new IllegalArgumentException("direction must be non-zero");
        }
        return new GenericControllerInput(Type.AXIS, -1, Objects.requireNonNull(axisName, "axisName"), direction, "");
    }

    public static GenericControllerInput hat(String direction) {
        return new GenericControllerInput(Type.HAT, -1, "", 0, Objects.requireNonNull(direction, "direction"));
    }

    public InputToken toToken() {
        return switch (type) {
            case BUTTON -> InputToken.controllerButton(buttonIndex);
            case AXIS -> InputToken.controllerAxis(axisName, direction);
            case HAT -> InputToken.controllerHat(hatDirection);
        };
    }
}
