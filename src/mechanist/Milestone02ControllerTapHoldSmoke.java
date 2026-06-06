package mechanist;

import mechanist.input.KeyBindingManager;

/** Smoke for south-face controller tap/hold interpretation over the shared controller profile. */
final class Milestone02ControllerTapHoldSmoke {
    public static void main(String[] args) {
        KeyBindingManager manager = KeyBindingManager.getInstance();
        manager.resetAllToDefaults();
        manager.updateControllerTuningProfile(new KeyBindingManager.ControllerTuningProfile(0.25, 1.0, false, false, 500, 200));

        InputRegistry registry = new InputRegistry();
        GenericControllerSchema schema = new GenericControllerSchema();

        GamepadControllerSnapshot pressed = GamepadControllerSnapshot.of("tap smoke",
                true, false, false, false,
                false, false, false, false,
                false, false, false, false,
                0.0f, 0.0f, 0.0f, 0.0f);
        GamepadControllerSnapshot released = GamepadControllerSnapshot.of("tap smoke",
                false, false, false, false,
                false, false, false, false,
                false, false, false, false,
                0.0f, 0.0f, 0.0f, 0.0f);

        schema.apply(pressed, registry, 1_000L);
        require(!registry.isActiveFromSource(InputAction.CONFIRM, InputSource.GAMEPAD), "tap should not confirm before release");
        require(!registry.isActiveFromSource(InputAction.INTERACT, InputSource.GAMEPAD), "tap should not interact before hold threshold");
        schema.apply(released, registry, 1_140L);
        require(registry.isActiveFromSource(InputAction.CONFIRM, InputSource.GAMEPAD), "short release should produce confirm tap");
        require(registry.isActiveFromSource(InputAction.EXAMINE, InputSource.GAMEPAD), "short release should produce examine tap where panel owns it");
        require(!registry.isActiveFromSource(InputAction.INTERACT, InputSource.GAMEPAD), "short release should not produce hold interact");
        schema.apply(released, registry, 1_160L);
        require(!registry.isActiveFromSource(InputAction.CONFIRM, InputSource.GAMEPAD), "tap pulse should clear on next released frame");

        schema.apply(pressed, registry, 2_000L);
        schema.apply(pressed, registry, 2_550L);
        require(registry.isActiveFromSource(InputAction.INTERACT, InputSource.GAMEPAD), "held south face should produce interact after threshold");
        require(!registry.isActiveFromSource(InputAction.CONFIRM, InputSource.GAMEPAD), "held south face should not produce confirm tap while held");
        schema.apply(released, registry, 2_620L);
        require(!registry.isActiveFromSource(InputAction.CONFIRM, InputSource.GAMEPAD), "hold release should not become a tap confirm");

        ControllerTapHoldTracker tracker = new ControllerTapHoldTracker();
        ControllerTapHoldTracker.TapHoldRead read = tracker.update(true, 3_000L, manager.getControllerTuningProfile());
        require(read.pressed(), "tracker should report pressed state");
        read = tracker.update(false, 3_120L, manager.getControllerTuningProfile());
        require(read.tapReleased(), "tracker should report tap release inside tap threshold");
        requireContains(ControllerTapHoldTracker.auditSummary(), "tap-release+hold-active", "tap hold audit summary");
        requireContains(GenericControllerSchema.auditSummary(), "tapHold", "generic controller audit summary");

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

    private Milestone02ControllerTapHoldSmoke() { }
}
