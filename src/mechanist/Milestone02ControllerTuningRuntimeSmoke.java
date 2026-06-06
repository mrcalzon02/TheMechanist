package mechanist;

import mechanist.input.InputDevice;
import mechanist.input.InputToken;
import mechanist.input.KeyBindingManager;

/** Smoke for applying saved controller tuning and bindings through the runtime controller schema. */
final class Milestone02ControllerTuningRuntimeSmoke {
    public static void main(String[] args) {
        KeyBindingManager manager = KeyBindingManager.getInstance();
        manager.resetAllToDefaults();

        InputRegistry registry = new InputRegistry();
        GenericControllerSchema schema = new GenericControllerSchema();

        GamepadControllerSnapshot defaultPad = GamepadControllerSnapshot.of("smoke pad",
                true, false, false, false,
                false, false, false, false,
                false, false, false, false,
                0.80f, 0.0f, 0.0f, 0.0f);
        schema.apply(defaultPad, registry);
        require(registry.isActiveFromSource(InputAction.MOVE_RIGHT, InputSource.GAMEPAD), "default left stick should companion D-pad movement");
        require(!registry.isActiveFromSource(InputAction.MOVE_LEFT, InputSource.GAMEPAD), "default right movement should not trigger left");

        manager.updateControllerTuningProfile(new KeyBindingManager.ControllerTuningProfile(0.35, 1.0, true, false, 500, 200));
        schema.apply(defaultPad, registry);
        require(registry.isActiveFromSource(InputAction.MOVE_LEFT, InputSource.GAMEPAD), "inverted x axis should trigger left");
        require(!registry.isActiveFromSource(InputAction.MOVE_RIGHT, InputSource.GAMEPAD), "inverted x axis should no longer trigger right");

        GamepadControllerSnapshot smallMotionPad = GamepadControllerSnapshot.of("smoke pad",
                false, false, false, false,
                false, false, false, false,
                false, false, false, false,
                0.20f, 0.0f, 0.0f, 0.0f);
        schema.apply(smallMotionPad, registry);
        require(!registry.isActiveFromSource(InputAction.MOVE_LEFT, InputSource.GAMEPAD), "deadzone should suppress small inverted x motion");
        require(!registry.isActiveFromSource(InputAction.MOVE_RIGHT, InputSource.GAMEPAD), "deadzone should suppress small x motion");

        manager.rebind(InputDevice.GENERIC_CONTROLLER, "inventory", InputToken.controllerButton(5), KeyBindingManager.DuplicatePolicy.SWAP);
        GamepadControllerSnapshot reboundPad = GamepadControllerSnapshot.of("smoke pad",
                false, false, false, false,
                false, false, false, false,
                false, false, false, true,
                0.0f, 0.0f, 0.0f, 0.0f);
        schema.apply(reboundPad, registry);
        require(registry.isActiveFromSource(InputAction.INVENTORY, InputSource.GAMEPAD), "runtime schema should honor rebound controller button");

        GamepadControllerSnapshot disconnected = GamepadControllerSnapshot.disconnected();
        schema.apply(disconnected, registry);
        require(!registry.isActiveFromSource(InputAction.INVENTORY, InputSource.GAMEPAD), "disconnect should clear controller source");
        requireContains(GenericControllerSchema.auditSummary(), "tuning=deadzone+sensitivity+axisInversion", "generic controller audit summary");

        manager.resetAllToDefaults();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
        }
    }

    private Milestone02ControllerTuningRuntimeSmoke() { }
}
