package mechanist.input;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Locale;
import java.util.Objects;

/**
 * Stable serializable token for a physical input assignment.
 *
 * The token is deliberately small and library-neutral so the UI can accept
 * keyboard, mouse, and controller events without tying the options menu to a
 * specific gamepad backend.
 */
public record InputToken(InputDevice device, String code, String displayName) {
    public InputToken {
        Objects.requireNonNull(device, "device");
        code = Objects.requireNonNull(code, "code").trim();
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        if (code.isEmpty()) {
            throw new IllegalArgumentException("Input token code cannot be blank.");
        }
        if (displayName.isEmpty()) {
            displayName = code;
        }
    }

    public static InputToken keyboard(int keyCode, int modifiersEx) {
        var modifierText = InputEvent.getModifiersExText(modifiersEx).trim();
        var keyText = KeyEvent.getKeyText(keyCode);
        var code = modifiersEx == 0 ? "KEY_" + keyCode : "KEY_" + keyCode + "_MOD_" + modifiersEx;
        var display = modifierText.isEmpty() ? keyText : modifierText + "+" + keyText;
        return new InputToken(InputDevice.KEYBOARD, code, display);
    }

    public static InputToken mouseButton(int button) {
        var display = switch (button) {
            case MouseEvent.BUTTON1 -> "Mouse Left";
            case MouseEvent.BUTTON2 -> "Mouse Middle";
            case MouseEvent.BUTTON3 -> "Mouse Right";
            default -> "Mouse Button " + button;
        };
        return new InputToken(InputDevice.MOUSE, "MOUSE_BUTTON_" + button, display);
    }

    public static InputToken controllerButton(int buttonIndex) {
        return new InputToken(InputDevice.GENERIC_CONTROLLER, "PAD_BUTTON_" + buttonIndex, "Button " + buttonIndex);
    }

    public static InputToken controllerHat(String direction) {
        var normalized = normalize(direction);
        return new InputToken(InputDevice.GENERIC_CONTROLLER, "PAD_HAT_" + normalized, "D-Pad " + pretty(normalized));
    }

    public static InputToken controllerAxis(String axisName, int direction) {
        var axis = normalize(axisName);
        var sign = direction < 0 ? "NEG" : "POS";
        var displaySign = direction < 0 ? "-" : "+";
        return new InputToken(InputDevice.GENERIC_CONTROLLER, "PAD_AXIS_" + axis + "_" + sign, pretty(axis) + " " + displaySign);
    }

    public String storageValue() {
        return device.name() + ":" + code + ":" + displayName;
    }

    public static InputToken fromStorageValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Input token storage value cannot be blank.");
        }
        var parts = value.split(":", 3);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Malformed input token: " + value);
        }
        var device = InputDevice.valueOf(parts[0]);
        var display = parts.length == 3 ? parts[2] : parts[1];
        return new InputToken(device, parts[1], display);
    }

    private static String normalize(String value) {
        return Objects.requireNonNull(value, "value")
                .trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    private static String pretty(String value) {
        var lower = value.replace('_', ' ').toLowerCase(Locale.ROOT);
        var words = lower.split(" ");
        var result = new StringBuilder();
        for (var word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
