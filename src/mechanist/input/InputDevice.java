package mechanist.input;

/**
 * Physical input family used by the keybinding remapper.
 */
public enum InputDevice {
    KEYBOARD("Keyboard"),
    MOUSE("Mouse"),
    GENERIC_CONTROLLER("Generic Controller");

    private final String displayName;

    InputDevice(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
