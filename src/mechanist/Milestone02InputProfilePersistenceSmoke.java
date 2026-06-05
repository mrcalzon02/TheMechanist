package mechanist;

import mechanist.input.InputDevice;
import mechanist.input.InputToken;
import mechanist.input.KeyBindingManager;

import java.awt.event.KeyEvent;
import java.util.Properties;

/** Smoke for saved keybinding profile export, import, validation, prompt refresh, and controller tuning. */
final class Milestone02InputProfilePersistenceSmoke {
    public static void main(String[] args) {
        KeyBindingManager manager = KeyBindingManager.getInstance();
        manager.resetAllToDefaults();

        InputToken customKeyboardInventory = InputToken.keyboard(KeyEvent.VK_U, 0);
        InputToken customControllerInventory = InputToken.controllerButton(8);
        require(manager.rebind(InputDevice.KEYBOARD, "inventory", customKeyboardInventory, KeyBindingManager.DuplicatePolicy.REJECT).accepted(), "keyboard inventory rebind");
        require(manager.rebind(InputDevice.GENERIC_CONTROLLER, "inventory", customControllerInventory, KeyBindingManager.DuplicatePolicy.REJECT).accepted(), "controller inventory rebind");

        KeyBindingManager.ControllerTuningProfile tuned = new KeyBindingManager.ControllerTuningProfile(0.35, 1.5, false, true, 600, 220);
        require(manager.updateControllerTuningProfile(tuned).accepted(), "controller tuning update should be accepted");
        require(manager.applyControllerAxisTuning("right_stick_y", 0.30) == 0.0, "deadzone should suppress small axis motion");
        require(manager.applyControllerAxisTuning("right_stick_y", 0.70) < 0.0, "vertical inversion should reverse tuned y axis");

        Properties saved = manager.exportProfileProperties();
        require(saved.getProperty("binding.KEYBOARD.inventory").contains("KEYBOARD:KEY_85:U"), "keyboard inventory should be serialized");
        require(saved.getProperty("binding.GENERIC_CONTROLLER.inventory").contains("GENERIC_CONTROLLER:PAD_BUTTON_8:Button 8"), "controller inventory should be serialized");
        requireEquals("0.35", saved.getProperty("controller.GENERIC_CONTROLLER.deadzone"), "deadzone should be serialized");
        requireEquals("1.5", saved.getProperty("controller.GENERIC_CONTROLLER.sensitivity"), "sensitivity should be serialized");
        requireEquals("true", saved.getProperty("controller.GENERIC_CONTROLLER.invert_y"), "vertical inversion should be serialized");
        requireEquals("600", saved.getProperty("controller.GENERIC_CONTROLLER.hold_ms"), "hold threshold should be serialized");
        requireEquals("220", saved.getProperty("controller.GENERIC_CONTROLLER.tap_ms"), "tap threshold should be serialized");

        manager.resetAllToDefaults();
        requireContains(ControlReferenceTextSubsystem.keyboardPromptFor(InputAction.INVENTORY), "Keyboard: I", "default keyboard prompt after reset");
        requireContains(ControlReferenceTextSubsystem.genericPromptFor(InputAction.INVENTORY), "Generic: Button 3", "default controller prompt after reset");
        require(Math.abs(manager.getControllerTuningProfile().deadzone() - 0.25) < 0.0001, "default deadzone after reset");

        KeyBindingManager.RebindResult imported = manager.importProfileProperties(saved);
        require(imported.accepted(), "saved profile import should be accepted");
        requireContains(ControlReferenceTextSubsystem.keyboardPromptFor(InputAction.INVENTORY), "Keyboard: U", "keyboard prompt after import");
        requireContains(ControlReferenceTextSubsystem.genericPromptFor(InputAction.INVENTORY), "Generic: Button 8", "controller prompt after import");
        requireContains(InputRebindingAuditAuthority.controllerTuningSummary(), "deadzone 35%", "controller tuning audit after import");
        require(manager.applyControllerAxisTuning("left_stick_x", 0.80) > 0.0, "non-inverted x axis should remain positive");

        Properties malformed = manager.exportProfileProperties();
        malformed.setProperty("binding.KEYBOARD.confirm", "not-a-token");
        KeyBindingManager.RebindResult malformedResult = manager.importProfileProperties(malformed);
        require(!malformedResult.accepted(), "malformed saved token should be rejected");
        requireContains(malformedResult.message(), "malformed", "malformed rejection message");
        requireContains(ControlReferenceTextSubsystem.keyboardPromptFor(InputAction.INVENTORY), "Keyboard: U", "malformed import should preserve current profile");

        Properties wrongDevice = manager.exportProfileProperties();
        wrongDevice.setProperty("binding.KEYBOARD.cancel", InputToken.controllerButton(9).storageValue());
        KeyBindingManager.RebindResult wrongDeviceResult = manager.importProfileProperties(wrongDevice);
        require(!wrongDeviceResult.accepted(), "wrong-device saved token should be rejected");
        requireContains(wrongDeviceResult.message(), "not Keyboard", "wrong-device rejection message");
        require(manager.requiredCommandBound(InputDevice.KEYBOARD, "cancel"), "cancel should remain bound after rejected import");

        Properties badTuning = manager.exportProfileProperties();
        badTuning.setProperty("controller.GENERIC_CONTROLLER.deadzone", "1.5");
        KeyBindingManager.RebindResult badTuningResult = manager.importProfileProperties(badTuning);
        require(!badTuningResult.accepted(), "out-of-range controller tuning should be rejected");
        requireContains(badTuningResult.message(), "controller tuning", "bad tuning rejection message");
        requireContains(InputRebindingAuditAuthority.controllerTuningSummary(), "deadzone 35%", "bad tuning import should preserve current tuning");

        Properties badThresholds = manager.exportProfileProperties();
        badThresholds.setProperty("controller.GENERIC_CONTROLLER.tap_ms", "800");
        KeyBindingManager.RebindResult badThresholdResult = manager.importProfileProperties(badThresholds);
        require(!badThresholdResult.accepted(), "invalid tap/hold thresholds should be rejected");
        requireContains(badThresholdResult.message(), "controller tuning", "bad threshold rejection message");

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

    private static void requireEquals(String expected, String actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + label + " to equal '" + expected + "' but was '" + actual + "'");
        }
    }

    private Milestone02InputProfilePersistenceSmoke() { }
}
